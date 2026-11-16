# Sprint 5-C — Fechamento de assinaturas

Base `4fd798c`. Os contratos estão em `FluxaApplication.subscriptions`. A UI da Sprint 5-B continua disponível. A integração dos novos comandos e leituras fica para a próxima entrega de frontend.

## Agenda por assinatura

`observeOccurrences(subscriptionId, fromInclusive, untilExclusive, limit)` consulta o cadastro, termos e ocorrências gravadas de uma assinatura pelo ID. O calendário deriva as demais ocorrências no intervalo `[fromInclusive, untilExclusive)`. A lista inclui abertas, vencidas, pagas, cobradas e puladas em ordem decrescente de data. O limite é opcional e aplicado depois da ordenação. Use uma janela adequada à tela.

`observeOccurrenceDetail(id, date)` combina a mesma ocorrência com o efeito financeiro em uma leitura transacional do Room. A resolução de conta expõe `transactionId` e `occurredOn`. A de cartão expõe `purchaseId`, `installmentId` e `occurredOn`, que corresponde à data da compra. Nome, valor, categoria e fonte vêm do snapshot da ocorrência. Uma ocorrência aberta ou pulada tem `resolution == null`. Uma referência inconsistente também pode resultar em resolução ausente, mesmo com estado `PAID` ou `CHARGED`.

## Estorno

Use `undoSubscriptionOccurrence(id, date, expectedResultId)` com o ID financeiro apresentado ao usuário na confirmação. O ID protege um pagamento ou uma cobrança nova de uma solicitação antiga.

- **Conta.** O comando confere a identidade, conta, categoria, valor e tipo da despesa efetivada. Remove só o lançamento gerado por essa assinatura e recompõe o Caixa e o saldo. Edição e cancelamento genéricos da despesa continuam retornando `MANAGED_BY_SUBSCRIPTION`.
- **Cartão.** O comando exige uma compra vinculada com uma única parcela pendente e sem baixa financeira. Reutiliza a política de cancelamento de compras e remove a parcela e a compra geradas, liberando o limite derivado. O cancelamento genérico de uma compra ativa de assinatura retorna `CreditError.MANAGED_BY_SUBSCRIPTION`. Uma compra cancelada pelo fluxo antigo, com a parcela também cancelada e sem baixa, pode ser reconciliada pelo mesmo estorno.
- **Parcela paga.** Desfaça primeiro o pagamento da parcela usando `credit.undoInstallmentPayment`. Depois, estorne a cobrança da assinatura. O estorno da assinatura nunca desfaz a baixa da parcela em cascata.
- **Fonte arquivada.** Conta e cartão arquivados permitem estorno histórico. Uma nova baixa continua sujeita às validações normais de fonte e categoria ativas.
- **Assinatura pausada ou cancelada.** A ocorrência estornada fica aberta com os termos históricos. O status do cadastro e o calendário futuro permanecem. Pagamentos seguem as restrições normais de pausa e cancelamento.

O sucesso retorna `SubscriptionUndoResult.Reopened(occurrence)`. A data original permanece, com estado `PENDING` ou `OVERDUE` calculado na leitura. Ao pagar ou cobrar novamente, o repositório usa os termos históricos e gera um novo ID financeiro. A compra, a parcela ou o lançamento revertido são removidos. O histórico de tentativas de estorno não é armazenado.

## Erros

| Erro | Comportamento |
| --- | --- |
| `OCCURRENCE_NOT_FOUND` | Atualize o detalhe. A identidade não está presente. |
| `NOT_RESOLVED` | A ocorrência já está aberta. |
| `SKIPPED` | A ocorrência pulada não admite estorno financeiro. |
| `CARD_CHARGE_ALREADY_PAID` | Direcione ao estorno de pagamento da parcela. |
| `CONFLICT` | O ID mudou ou os vínculos financeiros são inconsistentes. Recarregue antes de confirmar outro estorno. |

## Room e concorrência

O schema permanece v6. A linha da ocorrência passa a guardar só o snapshot aberto, com `status = PENDING` e `resultId = null`. O estado vencido continua sendo derivado. A remoção do efeito financeiro e a reabertura usam a mesma transação Room. Uma nova resolução substitui o snapshot em outra transação e conserva a unicidade entre conta, cartão e skip. Uma falha mantém o snapshot aberto. O contrato de meia-noite permanece no frontend da Sprint 5-B.

```kotlin
val history = subscriptions.observeOccurrences(subscriptionId, firstDay, endExclusive, limit = 24)
val detail = subscriptions.observeOccurrenceDetail(subscriptionId, occurrenceDate)
val undo = subscriptions.undoSubscriptionOccurrence(subscriptionId, occurrenceDate, confirmedResultId)
```
