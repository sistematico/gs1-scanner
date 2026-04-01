# GS1 DataMatrix Scanner

Aplicativo Android para leitura e validação de códigos DataMatrix no padrão GS1.

## Funcionalidades

- Leitura de códigos DataMatrix via câmera (CameraX + ML Kit)
- Parsing automático dos Application Identifiers (AI):
  - **AI 01** — GTIN (14 dígitos) com validação Mod10 GS1
  - **AI 11** — Data de Produção (AAMMDD)
  - **AI 17** — Data de Validade (AAMMDD)
  - **AI 10** — Número do Lote (variável, até 20 caracteres)
- Identificação automática do prefixo GS1 (Brasil, Indonesia, México, etc.)
- Descarte automático do prefixo `]d2` (identificador GS1)

## Regras de Negócio

- Dígito verificador Mod10 do GTIN (exibe o correto em caso de erro)
- Datas válidas no formato AAMMDD
- Data de Validade >= Data de Produção
- Charset do lote: `A-Z a-z 0-9` e símbolos permitidos, máximo 20 caracteres
- AIs esperados: 01, 11, 17, 10 — AIs não reconhecidos geram erro

## Instalação do APK

1. Transfira o arquivo `GS1Scanner-v1.0.0-debug.apk` para o celular
2. No celular, ative **Instalar de fontes desconhecidas** nas configurações
3. Toque no arquivo APK para instalar
4. Ao abrir o app, conceda permissão de câmera

**Requisitos:** Android 8.0 (API 26) ou superior.

## Build

### Pré-requisitos

- JDK 17
- Android SDK com:
  - Build Tools 34.0.0
  - Platform Android 34 (API 34)

### Compilar

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=/opt/android-sdk

# Debug APK
./gradlew assembleDebug

# Release APK (não assinado)
./gradlew assembleRelease
```

O APK será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Estrutura do Projeto

```
app/src/main/java/com/cooperait/gs1scanner/
├── MainActivity.kt          # Tela de câmera/scanner
├── ResultActivity.kt        # Tela de resultados
├── parser/
│   └── GS1Parser.kt         # Lógica de parsing e validação GS1
└── scanner/
    └── BarcodeAnalyzer.kt    # Integração com ML Kit Barcode
```

## Versionamento

- **v1.0.0** — Versão inicial com leitura e validação de DataMatrix GS1
