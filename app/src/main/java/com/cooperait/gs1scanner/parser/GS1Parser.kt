package com.cooperait.gs1scanner.parser

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GS1Field(
    val ai: String,
    val label: String,
    val rawValue: String,
    val displayValue: String,
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class GS1ParseResult(
    val rawContent: String,
    val fields: List<GS1Field>,
    val isValid: Boolean,
    val errors: List<String>
)

object GS1Parser {

    private const val GS_CHAR = '\u001D'
    private const val GS1_PREFIX = "]d2"

    // AI code -> (label, fixed length (-1 for variable), max length)
    private val AI_DEFINITIONS = mapOf(
        "01" to Triple("GTIN", 14, 14),
        "11" to Triple("Data de Produção", 6, 6),
        "17" to Triple("Data de Validade", 6, 6),
        "10" to Triple("Número do Lote", -1, 20)
    )

    fun parse(rawContent: String): GS1ParseResult {
        val errors = mutableListOf<String>()
        val fields = mutableListOf<GS1Field>()

        var data = rawContent
        if (data.startsWith(GS1_PREFIX)) {
            data = data.substring(GS1_PREFIX.length)
        }
        data = data.trimStart(GS_CHAR)

        var position = 0
        var productionDate: Date? = null
        var expirationDate: Date? = null

        while (position < data.length) {
            if (position + 2 > data.length) {
                errors.add("Dados incompletos na posição $position")
                break
            }

            val ai = data.substring(position, position + 2)
            position += 2

            val definition = AI_DEFINITIONS[ai]
            if (definition == null) {
                errors.add("AI não reconhecido '$ai' na posição ${position - 2}")
                break
            }

            val (_, fixedLength, maxLength) = definition

            val value: String
            if (fixedLength > 0) {
                if (position + fixedLength > data.length) {
                    errors.add("Dados insuficientes para AI $ai na posição $position")
                    break
                }
                value = data.substring(position, position + fixedLength)
                position += fixedLength
            } else {
                val gsIndex = data.indexOf(GS_CHAR, position)
                val endIndex = if (gsIndex >= 0) gsIndex else data.length
                value = data.substring(position, minOf(endIndex, position + maxLength))
                position = if (gsIndex >= 0) gsIndex + 1 else data.length
            }

            val field = when (ai) {
                "01" -> validateGTIN(ai, value)
                "11" -> {
                    val f = validateDate(ai, "Data de Produção", value)
                    if (f.isValid) productionDate = parseDate(value)
                    f
                }
                "17" -> {
                    val f = validateDate(ai, "Data de Validade", value)
                    if (f.isValid) expirationDate = parseDate(value)
                    f
                }
                "10" -> validateLotNumber(ai, value)
                else -> GS1Field(ai, definition.first, value, value, true)
            }

            fields.add(field)
            if (!field.isValid && field.errorMessage != null) {
                errors.add(field.errorMessage)
            }
        }

        // Business rule: Validade >= Produção
        if (productionDate != null && expirationDate != null) {
            if (expirationDate.before(productionDate)) {
                errors.add("Data de Validade não pode ser anterior à Data de Produção")
            }
        }

        return GS1ParseResult(
            rawContent = rawContent,
            fields = fields,
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    private fun validateGTIN(ai: String, value: String): GS1Field {
        if (!value.matches(Regex("\\d{14}"))) {
            return GS1Field(ai, "GTIN", value, value, false,
                "GTIN deve conter 14 dígitos numéricos")
        }

        val expectedCheckDigit = calculateMod10CheckDigit(value.substring(0, 13))
        val actualCheckDigit = value[13].digitToInt()
        val gs1Org = identifyGS1Prefix(value)
        val label = "GTIN ($gs1Org)"

        return if (expectedCheckDigit == actualCheckDigit) {
            GS1Field(ai, label, value, value, true)
        } else {
            GS1Field(ai, label, value, value, false,
                "Dígito verificador incorreto! O correto é $expectedCheckDigit")
        }
    }

    private fun calculateMod10CheckDigit(digits: String): Int {
        var sum = 0
        for (i in digits.indices) {
            val digit = digits[digits.length - 1 - i].digitToInt()
            val multiplier = if (i % 2 == 0) 3 else 1
            sum += digit * multiplier
        }
        return (10 - (sum % 10)) % 10
    }

    private fun identifyGS1Prefix(gtin: String): String {
        // For GTIN-14: position 0 is packaging indicator, positions 1-3 are GS1 prefix
        val prefix = gtin.substring(1, 4)
        return when (prefix) {
            "789", "790" -> "GS1 Brasil"
            "899" -> "GS1 Indonesia"
            "750" -> "GS1 México"
            "779" -> "GS1 Argentina"
            "770" -> "GS1 Colombia"
            "775" -> "GS1 Perú"
            "784" -> "GS1 Paraguay"
            "773" -> "GS1 Uruguay"
            "780" -> "GS1 Chile"
            else -> "GS1"
        }
    }

    private fun validateDate(ai: String, label: String, value: String): GS1Field {
        if (!value.matches(Regex("\\d{6}"))) {
            return GS1Field(ai, label, value, value, false,
                "$label deve conter 6 dígitos (AAMMDD)")
        }

        val month = value.substring(2, 4).toIntOrNull() ?: return GS1Field(
            ai, label, value, value, false, "$label: mês inválido")
        val day = value.substring(4, 6).toIntOrNull() ?: return GS1Field(
            ai, label, value, value, false, "$label: dia inválido")
        val year = value.substring(0, 2).toIntOrNull() ?: return GS1Field(
            ai, label, value, value, false, "$label: ano inválido")

        if (month < 1 || month > 12) {
            return GS1Field(ai, label, value, value, false,
                "$label: mês inválido ($month)")
        }
        if (day < 1 || day > 31) {
            return GS1Field(ai, label, value, value, false,
                "$label: dia inválido ($day)")
        }

        val fullYear = if (year >= 50) 1900 + year else 2000 + year
        val displayDate = formatDate(day, month, fullYear)

        return GS1Field(ai, label, value, displayDate, true)
    }

    private fun parseDate(value: String): Date? {
        return try {
            val sdf = SimpleDateFormat("yyMMdd", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDate(day: Int, month: Int, year: Int): String {
        val monthNames = arrayOf(
            "Jan.", "Fev.", "Mar.", "Abr.", "Mai.", "Jun.",
            "Jul.", "Ago.", "Set.", "Out.", "Nov.", "Dez."
        )
        return "$day ${monthNames[month - 1]} $year"
    }

    private fun validateLotNumber(ai: String, value: String): GS1Field {
        val label = "Número do Lote"
        if (value.isEmpty()) {
            return GS1Field(ai, label, value, value, false,
                "Número do Lote não pode ser vazio")
        }
        if (value.length > 20) {
            return GS1Field(ai, label, value, value, false,
                "Número do Lote excede 20 caracteres")
        }
        if (!value.matches(Regex("[A-Za-z0-9!\"#%&'()*+,\\-./:<;>=?_ ]+"))) {
            return GS1Field(ai, label, value, value, false,
                "Número do Lote contém caracteres inválidos")
        }
        return GS1Field(ai, label, value, value, true)
    }
}
