# Sprint 3-A — Lançamentos e saldos

Base de frontend `578c954`. O ponto de entrada da próxima sprint é `FluxaApplication.transactions`, que fornece o `TransactionRepository`. Os modelos são Kotlin puro.

## Criar, editar e cancelar

| Método | Contrato |
|---|---|
| `create(id: UUID, draft: TransactionDraft)` | Gere o UUID quando iniciar o envio do formulário e reutilize-o para reenvios do mesmo lançamento. Se já existir uma linha com esse ID, inclusive cancelada, retorna `DUPLICATE_ID`. Consulte o ID para recuperar o resultado do primeiro envio. |
| `update(id: UUID, draft: TransactionDraft)` | Substitui os detalhes de um lançamento efetivado em uma transação. Preserva UUID e data de criação, atualiza a data de edição e recalcula os saldos quando valor, direção, data ou conta mudam. |
| `cancel(id: UUID)` | Grava a data de cancelamento e retira o efeito financeiro. A linha e seus vínculos permanecem no histórico. Cancelar novamente ou editar um cancelado retorna `CANCELLED`. |

Os três métodos retornam `TransactionResult.Saved(transaction)` ou `TransactionResult.Rejected(error)`. Erros de negócio são tipados. Falhas inesperadas do banco são propagadas.

`TransactionDraft` recebe direção (`INCOME` ou `EXPENSE`), IDs da conta e da categoria, `Money` com centavos estritamente positivos, `occurredOn: LocalDate` e descrição opcional. A direção fornece o sinal. A descrição é normalizada ao gravar. `FinancialTransaction` registra `createdAt`, `updatedAt` e `cancelledAt` em `Instant`. O estado é `COMPLETED` ou `CANCELLED`, inferido de `cancelledAt`.

| Erro | Tratamento sugerido no formulário |
|---|---|
| `INVALID_AMOUNT`, `FUTURE_DATE` | Mostrar erro no campo. A data pode ser hoje ou anterior, usando o dia no fuso financeiro do aparelho. |
| `ACCOUNT_NOT_FOUND`, `ACCOUNT_ARCHIVED` | Atualizar o seletor de contas ativas e pedir outra conta. |
| `CATEGORY_NOT_FOUND`, `CATEGORY_ARCHIVED`, `CATEGORY_DIRECTION_MISMATCH` | Atualizar a lista de categorias ativas filtrada pela direção. |
| `NOT_FOUND`, `CANCELLED` | Retornar ao histórico ou atualizar a tela de detalhes. |
| `DUPLICATE_ID` | Consultar o mesmo UUID. Um novo UUID representa outra operação. |

Criar e editar exigem conta e categoria finais ativas. Um item com vínculo arquivado pode ser movido para referências ativas ou cancelado mantendo os vínculos anteriores. O tipo de uma categoria já referenciada fica bloqueado, inclusive se só houver lançamentos cancelados. `CategoryRepository.update` lança `CategoryKindInUse` nesse caso. Nome, ícone e cor continuam editáveis.

## Histórico e saldos reativos

- `observe(id): Flow<FinancialTransaction?>` mantém a consulta por ID disponível após o cancelamento.
- `observeHistory(TransactionHistoryFilter(...))` filtra por conta, categoria e período. `fromInclusive` inclui o dia inicial e `untilExclusive` exclui o dia final. Cancelados aparecem por padrão e podem ser ocultados com `includeCancelled = false`. A ordem é data decrescente, com desempate pelo UUID.
- `observeBalances(): Flow<BalanceSnapshot>` emite o mapa de saldos por UUID e o saldo disponível a partir da mesma leitura. `observeAccountBalance(id)` emite `null` se o ID estiver ausente. `observeAvailableBalance()` emite apenas o consolidado.
- O saldo por conta soma ao saldo inicial os lançamentos efetivados a partir de `openingDate`, inclusive. Lançamentos anteriores continuam no histórico. A edição do saldo inicial ou de sua data recalcula o saldo automaticamente.
- O consolidado inclui as contas ativas com `includeInAvailable = true`. Contas arquivadas mantêm saldo individual e histórico. Nesta sprint o consolidado representa o caixa elegível, pois as reservas ainda são futuras.
- `CalculatedBalance.Exact` contém `Money`. `Overflow` sinaliza que o total exato ultrapassa o intervalo de `Long`; não deve ser exibido como zero. As somas usam acumulador inteiro exato e o Flow continua observável. O consolidado é calculado independentemente, podendo caber em `Money` mesmo quando um saldo de conta isolado excede o limite.

## Room v3

`financial_transactions` tem PK UUID, FKs obrigatórias para conta e categoria com `RESTRICT` e índices para conta/data, categoria/data e data. A data civil usa dias desde a época e o valor usa centavos inteiros. A migration v2 → v3 preserva as tabelas anteriores. O gatilho de tipo da categoria é instalado na migration e na criação de um banco novo. A validação de referências e a escrita ocorrem na mesma transação. Saldos são derivados da leitura e não são persistidos.

## Próxima integração

Ligar os atalhos do botão central aos formulários de receita e despesa, usar os filtros no Caixa e alimentar os valores de conta e consolidado pelos Flows. Tratar cancelamento, erro de tipo de categoria em uso e estado de overflow com mensagens próprias. Os contratos de cartões, parcelas, transferências e recorrências permanecem para etapas futuras.
