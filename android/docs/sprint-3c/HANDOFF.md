# Sprint 3-C — Consultas financeiras

Os contratos estão em `FluxaApplication.transactions` (`TransactionRepository`). Para consumir no frontend, adicione os métodos necessários ao adaptador `TransactionSource` da Sprint 3-B e delegue ao repositório. As consultas do backend emitem mudanças no Room por `Flow`.

## Resumo por período

`observePeriodSummary(startInclusive: LocalDate, endExclusive: LocalDate): Flow<PeriodSummary>` retorna `income`, `expenses` e `net`. Cada campo é `CalculatedBalance.Exact(Money)` ou `CalculatedBalance.Overflow`. Entradas e saídas usualmente são magnitudes positivas e o líquido é entradas menos saídas. Um intervalo vazio retorna zero em todos os campos. Um intervalo invertido é rejeitado.

O resumo soma lançamentos efetivados de todas as contas pela data financeira. Cancelados, saldos iniciais e previsões ficam fora. Um lançamento anterior à abertura de uma conta pertence ao resumo se cair no período. A regra da data de abertura vale para o saldo da conta.

A agregação usa `SUM` inteiro no SQLite. Se o SQLite atingir overflow, uma segunda consulta soma partes inteiras menores e permite recuperar o total exato antes de convertê-lo para `Money`. Cada campo sinaliza seu próprio overflow. O líquido pode caber em centavos mesmo com entradas ou saídas fora do limite. Se a própria agregação por partes estourar, o resumo sinaliza overflow em todos os campos. O Flow continua observando mudanças nesses casos.

## Histórico e recentes

- `TransactionHistoryFilter` combina `accountId`, `categoryId`, `fromInclusive`, `untilExclusive`, `includeCancelled` e `direction`. Use `null` para todas as direções ou `INCOME`/`EXPENSE`. Cancelados continuam visíveis por padrão.
- `observeHistoryPage(filter, TransactionHistoryPage(limit, offset = 0))` aplica `LIMIT/OFFSET` no SQL. Use `limit > 0` e `offset >= 0`. Ao trocar filtros, reinicie no primeiro offset. Páginas observadas separadamente podem mudar após inserção, edição ou cancelamento. Ao carregar incrementalmente, atualize as páginas a partir do offset zero após mudanças.
- `observeHistory(filter)` continua compatível e não limita a quantidade. Prefira a consulta paginada em telas que podem exibir períodos grandes.
- `observeRecentTransactions(limit)` retorna até os últimos N lançamentos efetivados, sem limite de período e sem cancelados. Para o Início, passe `5`.
- Histórico e recentes usam a mesma ordem, `occurredOn DESC, id ASC`. Os filtros operam em `LocalDate` com início inclusivo e fim exclusivo. Novembro de 2026 é `[2026-11-01, 2026-12-01)`.

## Exemplo para ViewModel

```kotlin
val month = YearMonth.of(2026, 11)
val start = month.atDay(1)
val end = month.plusMonths(1).atDay(1)

val summary = transactions.observePeriodSummary(start, end)
val available = transactions.observeAvailableBalance()
val recent = transactions.observeRecentTransactions(5)

val expenses = transactions.observeHistoryPage(
    filter = TransactionHistoryFilter(
        accountId = selectedAccountId,
        direction = TransactionDirection.EXPENSE,
        fromInclusive = start,
        untilExclusive = end,
    ),
    page = TransactionHistoryPage(limit = 50),
)
```

A Sprint 3-B continua com os consumidores atuais. O próximo trabalho de UI pode migrar os filtros do Caixa para Room, adotar páginas e trocar o recorte em memória de recentes por `observeRecentTransactions(5)`. Use o resumo para mostrar entradas, saídas e líquido e trate `Overflow` como estado de exibição.

## Schema e índices

Room continua na versão 3. A análise com `EXPLAIN QUERY PLAN` sobre o schema exportado mostrou o índice existente de `occurredOn` atendendo o intervalo do resumo e do histórico e a varredura ordenada dos recentes. SQLite usa ordenação auxiliar para empates por UUID e agrupamento por direção. Os índices existentes de conta/data e categoria/data foram mantidos. Não há migration nova.
