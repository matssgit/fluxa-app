# Fluxa Personal Android

Projeto Android nativo independente, com um módulo Gradle `app`. O Sprint 0 contém os destinos Dashboard, Caixa, Cartões e Mais, com telas de verificação da navegação.

## Arquitetura

- `ui` contém o tema Material 3, a navegação e as telas Compose. ViewModels serão introduzidos nos fluxos que precisarem de estado de apresentação.
- `domain/model`, `domain/rules` e `domain/usecase` serão usados para modelos e regras Kotlin independentes de Android, Compose e Room.
- `data/database` concentra entidades, DAOs, mapeamento e migrations. `data/repository` implementará acesso a dados e transações compostas. `data/backup` receberá a exportação e restauração portátil.
- `core/date` contém calendário com relógio e zona injetáveis. `core/preferences` mantém um DataStore de processo. `core/money` e `core/security` são os destinos previstos para dinheiro e segurança local.
- Fluxos com regras seguem Compose → ViewModel → caso de uso → repositório → Room. Leituras simples podem expor Flow do repositório diretamente ao ViewModel. Pacotes futuros serão criados quando tiverem código. A composição de dependências é manual em `FluxaApplication`.

## Suporte Android

`minSdk 30`, para `java.time` nativo e suporte à futura combinação de biometria forte e credencial do aparelho. `compileSdk 34` e `targetSdk 34` correspondem à plataforma estável instalada neste ambiente. O build usa JDK 17 e Gradle Wrapper 8.9. Configure `JAVA_HOME` e `ANDROID_HOME` ou abra esta pasta no Android Studio com SDK 34 e Build Tools 34.0.0.

## Banco e preferências

Room abre `fluxa.db` no armazenamento privado, com foreign keys habilitadas e schema inicial 1. A tabela `local_schema_marker` existe para inicializar Room antes das entidades de produto. O plugin Room configura `room.schemaLocation` para exportar JSON em `app/schemas`. Versione o schema gerado. Os testes instrumentados usam esse diretório como assets para validar versões com `MigrationTestHelper`.

Durante a exploração, debug pode reconstruir a base quando faltar uma migration entre versões. Ao alterar a estrutura exploratória, incremente a versão ou limpe os dados de teste. Release exige migration para um banco de versão anterior. Ao congelar a versão de uso real, remova o fallback de debug e preserve um caminho de migration testado.

DataStore lê tema e ocultação de valores, com padrão do sistema e valores visíveis. Auto-lock e proteção de captura passarão pelo mesmo armazenamento quando implementados.

## Offline e backup

O app funciona localmente e não declara permissão INTERNET. O primeiro build pode precisar de acesso aos repositórios de dependências. O manifesto desativa Auto Backup. Regras XML excluem banco, preferências e demais domínios privados tanto no backup legado como em cloud backup e transferência de dispositivo em Android 12+. O formato portátil será controlado pelo Fluxa.

## Contratos de domínio reservados para os próximos sprints

- `Money` será uma value class sobre `Long` em centavos. O pacote previsto é `core.money`.
- `Transaction.transferGroupId?` liga as duas pontas da transferência. Elas afetam o saldo das contas e aparecem no ledger. Ficam fora de receitas, despesas, savings rate e analytics de receita/despesa. Categoria é dispensável.
- `availableCash = eligibleAccountBalance - goalReserves`. O Limite Seguro usa esse saldo sem repetir a dedução. A UI pode mostrar saldo elegível menos reservas, ou apresentar apenas o saldo disponível líquido. Receitas e obrigações usadas na explicação devem terminar no dia de menor folga.

## Validação

No Windows, execute a partir desta pasta:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:lint :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

O segundo comando exige aparelho ou emulador API 30+. Ele abre a atividade, verifica a navegação, inicializa Room e DataStore, verifica a configuração efetiva de foreign keys e exercita o schema exportado. O APK release padrão é gerado sem assinatura de distribuição.

O workflow Android do repositório compila os APKs, roda lint e testes JVM e verifica se o build altera o schema exportado. A execução dos testes instrumentados é feita com um dispositivo local.
