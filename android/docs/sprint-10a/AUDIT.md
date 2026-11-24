# Auditoria inicial — Sprint 10-A

Base confirmada: a79fa8bd09b8c400c3cc7a4ee036b50da0918476. Checkout principal com alterações preexistentes preservado. Auditoria antes das mudanças de produto:

| Prioridade | Evidência real | Ação de RC |
| --- | --- | --- |
| P1 | Manifest sem icon/roundIcon; lint MissingApplicationIcon. app_name ainda Fluxa Personal. | Ícone adaptativo mínimo derivado da identidade existente do Sidebar (setas, Pine900/Cream50); nome Fluxa. |
| P1 | Theme.Material.Light.NoActionBar e fallback assíncrono SYSTEM antes do tema persistido. | SplashScreen e fundo de startup seguro; aguardar somente política/preferências e composição antes de mostrar conteúdo. |
| P1 | FluxaNavigation.amountsOverride é session-only; o olho não grava DataStore. | Toggle atômico persistido, sem override; primeira composição recebe preferências já conhecidas. |
| P1 | SecurityGateTestHook e BackupPickerTestHook em src/main; catálogo Preview também na release. | Isolar hooks/catálogo em debug e conferir o APK release. |
| P2 | versionName=0.1.0; release sem configuração explícita; .gitignore sem proteção para keystores/backups reais. | 1.0.0, versionCode inicial 1, release não debug e sem minificação/assinatura de distribuição; regras locais restritas. |
| P2 | cleartext implícito; manifest release mesclado contém biometria, inicializador privado, Room service privado e ProfileInstallReceiver protegido por DUMP. Sem INTERNET. | cleartext=false explícito; manter componentes AndroidX necessários, documentando proteção. |
| Distribuição / nativo (inspeção no fechamento) | libdatastore_shared_counter.so tem ZIP/LOAD alinhados16KB; RELRO cobre o LOAD correspondente e o próximo LOAD gravável está fora da página protegida. AVD usa4KB. | Não há incompatibilidade nativa confirmada estaticamente; execução16KB e nova auditoria do APK/AAB continuam obrigatórias antes de distribuição. |
| Distribuição | minSdk30 / compile e target34; só SDK34 instalado; AGP8.5.2/Gradle8.9. Play 2026 requer target36. | Preservar stack desta RC local; migração API36/tooling/Android16 é bloqueio de distribuição posterior, não fingir elegibilidade Play. |

TODO/FIXME/HACK e logs financeiros/segredos em produção: nenhum encontrado na busca inicial. API deprecated USE_FINGERPRINT vem do Biometric estável; não remover permission/dependência transitiva por suposição. Fragment1.6.2 continua necessário para Activity Result/SAF.

Mais → Análises e Configurações continuam entradas inativas existentes, com “Em breve”; não habilitar funcionalidades pós-v1. Troca de tema será testada pela preferência existente. FLAG_SECURE já é idempotente na 9-B; manter.

Room v7 e schemas históricos preservados. O caso saldo 5000 / reserva1500 / disponível e seguro3500 já existe em SafeToSpendPersistenceTest e entrará na regressão completa. Testes de cada migration existem; acrescentar verificação da cadeia v1→v7 sem criar migration.

Baseline executado antes das alterações: JVM/lint/debug/release/androidTest aprovados; lint com zero erros e só MissingApplicationIcon.
