# Desenvolvimento

Como clonar, compilar, rodar e testar o Fluxa para Android.

## Requisitos

| Ferramenta | Versão |
| --- | --- |
| JDK | 17 (o build usa `jvmToolchain(17)`) |
| Android SDK | plataforma **34** (`compileSdk`) e build-tools 34 |
| Gradle | 8.9, baixado pelo Wrapper |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24, com KSP 1.9.24-1.0.20 |
| Android Studio | Opcional. Qualquer versão compatível com AGP 8.5 |

O app roda no Android 11 (API 30) ou superior.

## Configurar o SDK

Defina a variável `ANDROID_HOME` ou crie `android/local.properties` (ignorado pelo Git):

```properties
sdk.dir=/caminho/para/Android/Sdk
```

Com o Android Studio, basta abrir a pasta `android/`: ele cria esse arquivo.

## Compilar

```bash
git clone https://github.com/matssgit/fluxa-app.git
cd fluxa-app/android

./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release-unsigned.apk
```

No Windows, troque `./gradlew` por `gradlew.bat`.

O APK debug pode ser instalado direto, com `adb install -r`. O build *release* sai **sem assinatura**: o repositório não contém nenhuma chave, e a assinatura de distribuição é feita fora dele (veja [RELEASE_PROCESS.md](RELEASE_PROCESS.md)).

> Debug e release usam o mesmo `applicationId` (`com.fluxa.personal`). Um APK debug não instala por cima de um APK oficial, porque as assinaturas são diferentes. Para desenvolver, use um emulador ou um aparelho sem o app oficial, ou faça um backup antes.

## Testar

```bash
./gradlew testDebugUnitTest          # testes JVM (rápidos, sem emulador)
./gradlew lintDebug lintRelease      # lint
./gradlew connectedDebugAndroidTest  # testes instrumentados
```

Alguns testes instrumentados de segurança precisam de um PIN configurado no emulador. Passe-o como argumento:

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.deviceCredential=1357
```

Use um PIN **descartável**, só para o emulador. Detalhes em [TESTING.md](TESTING.md).

## Onde mexer

| Para… | Veja |
| --- | --- |
| Mudar uma regra financeira | `domain/rules/` e o teste correspondente em `src/test/.../domain/rules` |
| Mudar uma tela | `ui/feature/<área>/*Screens.kt` e o ViewModel ao lado |
| Mudar cores, tipografia ou componentes | `ui/designsystem/` |
| Mudar o banco | entidade + DAO + **nova migration** + schema exportado + teste de migration |
| Mudar o backup | `backup/` (o formato v1 é congelado; mudanças exigem uma versão nova) |

Arquitetura completa: [ARCHITECTURE.md](ARCHITECTURE.md).

## Convenções

- Valores sempre em `Money` (centavos). Nunca `Double` ou `Float`.
- Datas financeiras em `LocalDate`. Use `FinancialCalendar` para obter "hoje" e permitir testes.
- Regras como funções puras que devolvem um resultado tipado (`Saved`/`Rejected`), sem lançar exceção para erro de negócio.
- Toda operação que altera mais de uma linha deve rodar dentro de `database.withTransaction`.
- Sem dependências de rede.
- Textos da interface em português do Brasil.

## Histórico de desenvolvimento

Cada etapa do app tem um registro em [`android/docs/`](../android/docs), com decisões, testes e capturas daquela entrega. Esses registros mencionam caminhos e emuladores da máquina do mantenedor e servem como histórico. Para o estado atual, use este guia.
