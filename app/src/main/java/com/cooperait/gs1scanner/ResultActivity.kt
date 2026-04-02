package com.cooperait.gs1scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cooperait.gs1scanner.parser.GS1Parser

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val rawContent = intent.getStringExtra("raw_content") ?: ""
        val result = GS1Parser.parse(rawContent)

        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Scanned content
        val tvScannedContent = findViewById<TextView>(R.id.tvScannedContent)
        tvScannedContent.text = rawContent.removePrefix("]d2").replace(Regex("[\\x00-\\x1F\\x7F]"), "")
        tvScannedContent.setOnLongClickListener {
            copyToClipboard("Código Scanneado", rawContent)
            true
        }

        // Fields
        val fieldsContainer = findViewById<LinearLayout>(R.id.fieldsContainer)

        for (field in result.fields) {
            val fieldView = LayoutInflater.from(this)
                .inflate(R.layout.item_field, fieldsContainer, false)

            val tvLabel = fieldView.findViewById<TextView>(R.id.tvFieldLabel)
            val tvValue = fieldView.findViewById<TextView>(R.id.tvFieldValue)
            val tvAI = fieldView.findViewById<TextView>(R.id.tvFieldAI)
            val tvError = fieldView.findViewById<TextView>(R.id.tvFieldError)
            val tvCheckDigit = fieldView.findViewById<TextView>(R.id.tvCheckDigitStatus)
            val fieldBorder = fieldView.findViewById<View>(R.id.fieldBorder)

            tvLabel.text = field.label
            tvValue.text = field.displayValue
            tvAI.text = field.ai

            if (field.isValid) {
                fieldBorder.setBackgroundResource(R.drawable.field_border)
            } else {
                fieldBorder.setBackgroundResource(R.drawable.field_border_error)
            }

            // Check digit status for GTIN
            if (field.ai == "01") {
                tvCheckDigit.visibility = View.VISIBLE
                if (field.isValid) {
                    tvCheckDigit.text = "Dígito verificador correto!"
                    tvCheckDigit.setTextColor(ContextCompat.getColor(this, R.color.success_green))
                } else {
                    tvCheckDigit.text = field.errorMessage
                    tvCheckDigit.setTextColor(ContextCompat.getColor(this, R.color.error_red))
                }
            }

            // Error message for non-GTIN fields
            if (!field.isValid && field.ai != "01") {
                tvError.visibility = View.VISIBLE
                tvError.text = field.errorMessage
            }

            fieldView.setOnLongClickListener {
                copyToClipboard(field.label, field.rawValue)
                true
            }

            fieldsContainer.addView(fieldView)
        }

        // Validation status
        val tvStatus = findViewById<TextView>(R.id.tvValidationStatus)
        val ivStatus = findViewById<ImageView>(R.id.ivStatusIcon)

        if (result.isValid) {
            tvStatus.text = "Verificação dos dados do código de barras realizada com sucesso!"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            ivStatus.setImageResource(R.drawable.ic_check_circle)
        } else {
            val errorText = buildString {
                append("A verificação dos dados do código de barras falhou.")
                for (error in result.errors) {
                    append("\n• $error")
                }
            }
            tvStatus.text = errorText
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            ivStatus.setImageResource(R.drawable.ic_error_circle)
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copiado!", Toast.LENGTH_SHORT).show()
    }
}
