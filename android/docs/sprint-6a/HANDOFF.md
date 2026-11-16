# Sprint 6-A — Metas, reservas e disponibilidade

Base `229331e`. Os contratos de metas estão em `FluxaApplication.goals`.

## Metas e movimentos

Cada meta pertence a uma conta. `GoalDetails` recebe nome, target positivo em `Money`, ID da conta e data opcional de planejamento. A troca de conta exige destino ativo e ausência de qualquer movimento histórico, inclusive após uma liberação total. A edição aceita aumentar o target e reduzir até o reservado. A conclusão registra um marco explícito e permanece no histórico mesmo após edição do target ou liberação.

`allocateToGoal(id, amount)` registra `ALLOCATE` positivo em meta ativa e conta ativa. O valor deve caber no restante do target e no saldo livre da conta depois das reservas de todas as suas metas. `releaseFromGoal(id, amount)` registra `RELEASE` positivo de até o valor reservado, inclusive em meta concluída ou conta arquivada. Os comandos registram a data financeira local via `FinancialCalendar` e o instante de auditoria via `Clock`.

`completeGoal(id)` exige que o valor reservado iguale o target. A conclusão mantém o dinheiro reservado e encerra novas alocações. `archiveGoal(id)` exige reserva zerada e preserva os movimentos. Arquivar a conta segue o contrato existente, preserva a meta e a reserva e exclui a conta da disponibilidade consolidada. Nenhum desses comandos gera lançamento no Caixa.

## Leituras reativas

- `observeGoals(includeArchived = false)` lista metas, incluindo concluídas. `observeGoal(id)` inclui conta, target, reserva, valor restante e progresso em pontos-base de 0 a 10.000. O cálculo arredonda para baixo.
- `observeMovements(id)` fornece o histórico por data financeira, instante e ID em ordem decrescente. A UI pode sinalizar `ALLOCATE` com mais e `RELEASE` com menos como classificação da reserva.
- `observeAccountReserves(id)` expõe saldo real, reserva e saldo disponível. O disponível pode ficar negativo por despesas ou alterações posteriores no saldo.
- `observeReserveBalances()` fornece a mesma visão por conta e os totais `totalBalance`, `totalReserved` e `availableBalance`. Os totais de saldo real e reserva incluem contas arquivadas para compor a visão de todas as contas registradas. O disponível consolida somente contas ativas com `includeInAvailable=true`; nele são deduzidas somente as reservas das próprias contas elegíveis. Use `CalculatedBalance.Exact` ou `Overflow` conforme os read models financeiros existentes.

O Dashboard existente segue usando seu contrato anterior. Para exibir disponibilidade líquida, use `goals.observeReserveBalances()`. O futuro SafeToSpend pode partir de `availableBalance`, que **já deduz as reservas elegíveis**. Aplicar outra dedução contaria a mesma reserva duas vezes.

## Comandos e erros

Os comandos retornam `GoalResult.Saved` com a meta ou movimento gravado, ou `GoalResult.Rejected(error)`.

| Erro | Condição |
| --- | --- |
| `NOT_FOUND`, `ARCHIVED`, `COMPLETED` | Identidade ausente ou estado incompatível com o comando. |
| `INVALID_NAME`, `INVALID_TARGET`, `INVALID_AMOUNT` | Nome vazio ou valor inválido. |
| `ACCOUNT_NOT_FOUND`, `ACCOUNT_ARCHIVED` | Conta de origem ou novo destino indisponível. |
| `ACCOUNT_CHANGE_NOT_ALLOWED` | A meta possui histórico de movimentos. |
| `TARGET_BELOW_RESERVED` | O novo target é inferior ao reservado. |
| `ALLOCATION_EXCEEDS_TARGET`, `INSUFFICIENT_AVAILABLE_FUNDS` | A alocação excede o restante da meta ou o disponível da conta. |
| `RELEASE_EXCEEDS_RESERVED` | A liberação excede a reserva atual. |
| `TARGET_NOT_REACHED`, `HAS_RESERVED_FUNDS` | Falta completar a reserva ou zerá-la antes de arquivar. |

## Room v7 e concorrência

A migration v6 → v7 adiciona `goals` e `goal_movements` com FKs restritivas. O saldo reservado é derivado de movimentos; a meta guarda os instantes de conclusão e arquivamento. O cálculo usa precisão inteira em centavos. A alocação lê o ledger e as reservas da conta e grava em uma única transação do Room. As leituras de disponibilidade reagem às quatro tabelas e usam um snapshot transacional para evitar combinar a reserva de uma revisão com o saldo de outra.

```kotlin
val created = goals.createGoal(GoalDetails("Viagem", Money(500_000), accountId))
val history = goals.observeMovements(goalId)
val summary = goals.observeGoal(goalId)
val allocation = goals.allocateToGoal(goalId, Money(150_000))
val balances = goals.observeReserveBalances()
```
