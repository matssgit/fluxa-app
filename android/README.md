# Fluxa Personal Android

Projeto Android nativo independente, com um módulo Gradle `app`. O Sprint 0 contém os destinos Dashboard, Caixa, Cartões e Mais, com telas de verificação da navegação.

## Arquitetura

- `ui` contém o tema Material 3, a navegação e as telas Compose. ViewModels serão introduzidos nos fluxos que precisarem de estado de apresentação.
- `domain/model` e `domain/rules` contêm tipos e regras Kotlin independentes de Android, Compose e Room. Casos de uso entram quando houver orquestração de operações.
- `data/database` concentra entidades, DAOs, mapeamento e migrations. `data/repository` implementará acesso a dados e transações compostas. `data/backup` receberá a exportação e restauração portátil.
- `core/date` contém relógio com zona injetável e regras de dia de vencimento. `core/money` contém o tipo monetário em BRL. `core/preferences` mantém um DataStore de processo. `core/security` é reservado à segurança local.
- Fluxos com regras seguem Compose → ViewModel → caso de uso → repositório → Room. Leituras simples podem expor Flow do repositório diretamente ao ViewModel. Pacotes futuros serão criados quando tiverem código. A composição de dependências é manual em `FluxaApplication`.

## Suporte Android

`minSdk 30`, para `java.time` nativo e suporte à futura combinação de biometria forte e credencial do aparelho. `compileSdk 34` e `targetSdk 34` correspondem à plataforma estável instalada neste ambiente. O build usa JDK 17 e Gradle Wrapper 8.9. Configure `JAVA_HOME` e `ANDROID_HOME` ou abra esta pasta no Android Studio com SDK 34 e Build Tools 34.0.0.

## Banco e preferências

Room abre `fluxa.db` no armazenamento privado, com foreign keys habilitadas. O schema v7 acrescenta metas e movimentos de reserva. O builder de produção registra toda a cadeia de migrations de v1 a v7. Consulte o [handoff da Sprint 6-A](docs/sprint-6a/HANDOFF.md) para os contratos de reserva e disponibilidade. Todos os builds exigem um caminho de migration explícito.

O plugin Room exporta os schemas em `app/schemas`. Os testes instrumentados usam os schemas exportados para validar as migrations, conferir dados preexistentes e reabrir o banco com o builder de produção. Ao introduzir uma versão, gere e versione o JSON antes de empacotar os testes instrumentados.

DataStore lê tema e ocultação de valores, com padrão do sistema e valores visíveis. Auto-lock e proteção de captura passarão pelo mesmo armazenamento quando implementados.

## Offline e backup

O app funciona localmente e não declara permissão INTERNET. O primeiro build pode precisar de acesso aos repositórios de dependências. O manifesto desativa Auto Backup. Regras XML excluem banco, preferências e demais domínios privados tanto no backup legado como em cloud backup e transferência de dispositivo em Android 12+. O formato portátil será controlado pelo Fluxa.

## Núcleo financeiro

- `Money` é uma value class sobre `Long` em centavos de BRL. Saldos e resultados de cálculo podem ser assinados. Soma e subtração verificam overflow. Conversão de reais e cálculo percentual exigem um `RoundingMode` explícito. Para entrada que precisa ter precisão de centavos, use `UNNECESSARY`. O desconto arredonda o desconto uma vez e o subtrai do total.
- `BillingDay` aceita dias de 1 a 31. Em mês curto, usa o último dia e mantém o dia pretendido para recalcular o mês seguinte. O relógio existente calcula `LocalDate` no fuso fornecido.
- `InstallmentSplitter` recebe o primeiro mês de vencimento. A numeração começa em 1, cada parcela tem pelo menos um centavo e cada centavo do resto aumenta uma das últimas parcelas em um centavo. Com isso, a soma fecha e a diferença máxima fica em um centavo. O calendário de crédito determina o primeiro mês de vencimento.
- `OccurrenceCalendar` gera previsões únicas, mensais e anuais desde a âncora. O começo do intervalo é inclusivo e o fim é exclusivo. A identidade inclui origem e data prevista. Fornecer os IDs já efetivados exclui essas previsões mesmo quando a baixa ocorreu em outra data. Consultas que se sobrepõem retornam o mesmo ID para a mesma ocorrência. A unicidade da efetivação fica para a transação persistida.

## Contratos de domínio reservados para os próximos sprints

- `Transaction.transferGroupId?` liga as duas pontas da transferência. Elas afetam o saldo das contas e aparecem no ledger. Ficam fora de receitas, despesas, savings rate e analytics de receita/despesa. Categoria é dispensável.
- `availableCash = eligibleAccountBalance - goalReserves`. O Limite Seguro usa esse saldo sem repetir a dedução. A UI pode mostrar saldo elegível menos reservas, ou apresentar apenas o saldo disponível líquido. Receitas e obrigações usadas na explicação devem terminar no dia de menor folga.

## Contas e categorias

Os ViewModels podem usar `FluxaApplication.accounts` e `FluxaApplication.categories`. Os repositórios expõem modelos de domínio e operações suspensas, sem expor entities ou DAOs à UI.

| Repositório | Leitura reativa | Escrita |
|---|---|---|
| `AccountRepository` | `observe(id: UUID): Flow<FinancialAccount?>` e `observeAll(includeArchived = false)` | `create(AccountDetails): UUID`, `update(id, details): Boolean` e `archive(id): Boolean` |
| `CategoryRepository` | `observe(id: UUID): Flow<Category?>` e `observeAll(includeArchived = false, kind = null)` | `create(CategoryDetails): UUID`, `update(id, details): Boolean`, `archive(id): Boolean` e `reactivate(id): Boolean` |

- A conta aceita `CHECKING`, `WALLET` e `SAVINGS`. `AccountDetails` inclui nome, tipo, saldo inicial `Money`, data de referência `LocalDate` e `includeInAvailable` (padrão `true`). O saldo inicial pode ser negativo. Cartões pertencem a outro fluxo.
- `CategoryDetails` inclui nome, `TransactionDirection.INCOME` ou `EXPENSE` e valores opcionais de ícone e cor. O destino visual desses valores será definido nas telas.
- Os IDs são UUIDs gerados na criação e preservados em edições, arquivamento e reativação. Os nomes precisam conter texto e podem repetir. Espaços no início e no fim são removidos ao salvar. As listas usam nome e ID para ordenar e excluem arquivados por padrão. A consulta por ID inclui registros arquivados.
- `archivedAt` nulo identifica um registro ativo. `update` devolve `false` quando o ID não existe. Arquivar ou reativar devolve `false` quando o ID não existe ou já está no estado desejado. A transição condicional e cada edição são atômicas. O arquivamento preserva a linha e seus dados para uso histórico.
- Os lançamentos referenciam ambos os UUIDs com FKs restritivas. Se houver lançamento ou compra vinculada, inclusive cancelada, a mudança de tipo da categoria lança `CategoryKindInUse`. Não há exclusão física nem reativação de conta no contrato atual.

## Lançamentos e saldos

Veja o [handoff da Sprint 3-A](docs/sprint-3a/HANDOFF.md) para o contrato de criação, histórico, cancelamento e saldos reativos.

## Crédito

Veja o [handoff da Sprint 4-A](docs/sprint-4a/HANDOFF.md) para os contratos de cartão, compras, parcelas, datas e limite derivado. O [handoff da Sprint 4-C](docs/sprint-4c/HANDOFF.md) detalha pagamento, estorno e o vínculo com o ledger.

## Validação

No Windows, execute a partir desta pasta:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:lint :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

O segundo comando exige aparelho ou emulador API 30+. Ele abre a atividade, verifica a navegação, inicializa Room e DataStore, verifica a configuração efetiva de foreign keys e exercita o schema exportado. O APK release padrão é gerado sem assinatura de distribuição.

O workflow Android do repositório compila os APKs, roda lint e testes JVM e verifica se o build altera o schema exportado. A execução dos testes instrumentados é feita com um dispositivo local.

## Plano de atualização para API 36

1. Usar uma branch dedicada e manter API 34 como referência. Instalar a plataforma e uma imagem de emulador API 36 no SDK local e configurar a plataforma na CI.
2. Testar como conjunto candidato Gradle 8.13, AGP 8.13.2, JDK 17 e Kotlin 2.3.21. Usar o plugin `org.jetbrains.kotlin.plugin.compose` na mesma versão do Kotlin e remover o pin antigo do compilador Compose. Selecionar KSP2 dentro da matriz de compatibilidade e atualizar runtime, compilador, plugin e testes Room em conjunto se necessário. Conferir as exigências dos AARs e atualizar Compose BOM e outras bibliotecas conforme o resultado. [AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes) · [Matriz Kotlin](https://kotlinlang.org/docs/gradle-configure-project.html) · [Compose Compiler](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler) · [KSP](https://kotlinlang.org/docs/ksp-overview.html) · [Room](https://developer.android.com/jetpack/androidx/releases/room)
3. Elevar primeiro `compileSdk` para 36, mantendo `targetSdk` 34. Rodar builds debug e release, lint, testes JVM, instrumentação e diff do schema Room. Verificar também o build da CI e manter `minSdk` 30.
4. Elevar `targetSdk` para 36 após validar a compilação. No emulador API 36, testar insets edge-to-edge, navegação de volta, inicialização offline e configuração de backup. Repetir a instrumentação no piso API 30 antes de adotar a atualização. [Mudanças de comportamento da API 36](https://developer.android.com/about/versions/16/behavior-changes-16)
