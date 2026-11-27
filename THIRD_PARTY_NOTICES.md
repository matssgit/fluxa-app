# Componentes de terceiros

O APK do Fluxa para Android inclui as bibliotecas abaixo. Todas são distribuídas sob a **Apache License 2.0**: <https://www.apache.org/licenses/LICENSE-2.0>.

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
