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

### APK de Debug

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=/opt/android-sdk

./gradlew assembleDebug
```

Saída: `app/build/outputs/apk/debug/app-debug.apk`

O APK de debug já é assinado automaticamente com uma chave de desenvolvimento e pode ser instalado diretamente em dispositivos com depuração USB ativada.

---

### APK de Release

#### 1. Gerar um keystore (apenas na primeira vez)

```bash
keytool -genkeypair -v \
  -keystore keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias minha-chave
```

Guarde o arquivo `keystore.jks` e as senhas em local seguro. **Não versione o keystore no git.**

#### 2. Configurar as credenciais de assinatura

Crie ou edite o arquivo `local.properties` na raiz do projeto (já ignorado pelo `.gitignore`):

```properties
KEYSTORE_PATH=/caminho/absoluto/para/keystore.jks
KEYSTORE_PASSWORD=senha-do-keystore
KEY_ALIAS=minha-chave
KEY_PASSWORD=senha-da-chave
```

#### 3. Configurar a assinatura no `app/build.gradle.kts`

Adicione o bloco `signingConfigs` dentro de `android { }`:

```kotlin
import java.util.Properties

val localProps = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(localProps["KEYSTORE_PATH"] as String)
            storePassword = localProps["KEYSTORE_PASSWORD"] as String
            keyAlias = localProps["KEY_ALIAS"] as String
            keyPassword = localProps["KEY_PASSWORD"] as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

#### 4. Gerar o APK

```bash
./gradlew assembleRelease
```

Saída: `app/build/outputs/apk/release/app-release.apk`

> **Sem keystore configurado**, o comando ainda funciona e gera um APK não assinado em `app/build/outputs/apk/release/app-release-unsigned.apk`, mas ele **não pode ser instalado** em dispositivos sem assinatura manual posterior.

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
