# Componentes de terceiros

O código deste repositório é distribuído sob a [licença MIT](LICENSE), © 2026 Matheus Santana. Os itens abaixo **não** são cobertos por ela e mantêm as próprias licenças.

## No repositório

| Arquivo | Origem | Licença |
| --- | --- | --- |
| `android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/gradle-wrapper.jar` | Gradle Wrapper, © the original authors | Apache-2.0 (aviso mantido nos arquivos) |
| `frontend/public/*.png`, `frontend/public/favicon/*` | Ícones da versão web. **Origem não confirmada** | Não relicenciados. Excluídos da licença MIT até a origem ser confirmada |

## No APK

O APK do Fluxa para Android inclui as bibliotecas abaixo, sem modificações. Todas são distribuídas sob a **Apache License 2.0**: <https://www.apache.org/licenses/LICENSE-2.0>.

Lista gerada a partir de `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` na versão 1.0.2 (115 artefatos).

| Grupo | Projeto | Licença |
| --- | --- | --- |
| `androidx.*` (activity, annotation, appcompat, arch.core, autofill, biometric, collection, compose, concurrent, core, cursoradapter, customview, datastore, drawerlayout, emoji2, fragment, interpolator, lifecycle, loader, navigation, profileinstaller, room, savedstate, sqlite, startup, tracing, vectordrawable, versionedparcelable, viewpager) | Android Jetpack, © The Android Open Source Project | Apache-2.0 |
| `org.jetbrains.kotlin` | Kotlin Standard Library, © JetBrains s.r.o. | Apache-2.0 |
| `org.jetbrains.kotlinx` | kotlinx.coroutines, © JetBrains s.r.o. | Apache-2.0 |
| `org.jetbrains:annotations` | JetBrains Java Annotations | Apache-2.0 |
| `com.google.code.gson` | Gson, © Google | Apache-2.0 |
| `com.google.guava:listenablefuture` | Guava ListenableFuture, © Google | Apache-2.0 |
| `com.squareup.okio` | Okio, © Square, Inc. | Apache-2.0 |

## Recursos visuais

- **Ícones da interface**: desenhados para o Fluxa (`FluxaIcons.kt`). Não usam pacotes de ícones de terceiros.
- **Fontes**: o app usa a fonte padrão do sistema Android e não inclui arquivos de fonte.
- **Marca e ícone do app**: criados para o Fluxa (`marketing/branding`).
- **Materiais de divulgação**: gerados a partir de capturas do próprio app. Os títulos usam fontes do Windows (Sitka e Segoe UI) apenas na renderização das imagens. Nenhum arquivo de fonte é distribuído.

## Versão web (`frontend/`, `backend/`)

As dependências npm da versão web original estão nos respectivos `package-lock.json`. A grande maioria usa MIT, ISC, Apache-2.0 ou BSD. `lightningcss` (MPL-2.0) é usado só como ferramenta de build.
