# Sprint 5-A — Assinaturas e ocorrências

Base `ab66f9d`. O frontend recebe `SubscriptionRepository` por `FluxaApplication.subscriptions`.

## Cadastro e calendário

`create(details, frequency, anchorDate, billingDay)` cria uma assinatura `ACTIVE`. `SubscriptionDetails` contém nome, valor positivo em `Money`, categoria ativa de despesa e exatamente uma `SubscriptionSource.Account(id)` ou `Card(id)` ativa. `update(id, details)` altera esses dados, com vigência a partir do dia seguinte para uma assinatura ativa. As datas já vencidas e a data de hoje preservam seus termos. Frequência e âncora ficam fixas nesta sprint.

A identidade é `SubscriptionOccurrenceKey(subscriptionId, occurrenceDate)`. O calendário mensal preserva o dia pretendido e ajusta meses curtos. O anual preserva mês e dia da âncora. Em 29/02, cai em 28/02 nos anos comuns e volta a 29/02 nos bissextos. Uma assinatura anual de R$ 600 produz uma ocorrência de R$ 600 por ano. O pagamento integral entra no caixa na data realizada. Nenhum valor equivalente mensal participa da agenda ou do ledger.

## Resolver ocorrência

`paySubscriptionOccurrence(subscriptionId, occurrenceDate, paidOn)` resolve conforme os termos **da ocorrência**. Não recebe valor, categoria ou fonte da UI. A data da ocorrência e a data da baixa podem diferir. Ambas devem ser iguais ou anteriores ao dia financeiro atual. Na conta, a data também precisa ser igual ou posterior à abertura. A validação usa a data local injetável do `FinancialCalendar`.

- **Conta.** Cria uma única saída efetivada com data `paidOn` e a chave da ocorrência. O saldo pode ficar negativo. Caixa, recentes, resumo e saldos leem pelo ledger existente. Edição e cancelamento genéricos retornam `MANAGED_BY_SUBSCRIPTION`; um estorno próprio fica para decisão posterior.
- **Cartão.** Cria uma compra com uma parcela, usando `paidOn` como data de compra e as regras de fechamento e limite existentes. O valor entra no ledger quando a parcela for paga. Cancele a compra pelo contrato existente quando aplicável. A ocorrência permanece historicamente `CHARGED` e não pode ser gerada outra vez.
- **Skip.** `skipSubscriptionOccurrence(subscriptionId, occurrenceDate)` marca uma ocorrência válida como `SKIPPED`. Ela sai das próximas e vencidas e continua visível no histórico. Skip pode ser aplicado a uma data futura válida ou a um atraso anterior à suspensão. Não cria movimento financeiro.

As três resoluções retornam `SubscriptionResult.Saved(occurrence)` ou `Rejected(error)`. O resultado traz termos históricos e, para `PAID`/`CHARGED`, o UUID da transação ou compra. O contrato admite um único desfecho por identidade, inclusive entre conta e cartão. Falha de limite ou de persistência não consome a ocorrência.

## Pausa, retomada e cancelamento

`pause(id)` interrompe a geração a partir do dia financeiro atual. `resume(id)` retoma a partir do dia seguinte e não recupera as datas perdidas durante a pausa. Atrasos anteriores permanecem consultáveis. Durante pausa ou após cancelamento, a baixa financeira é rejeitada; é possível pular ocorrências antigas ainda válidas. `cancel(id)` interrompe a geração a partir de hoje e é terminal. As resoluções anteriores prevalecem sobre pausas, cancelamentos e revisões posteriores.

## Leituras

- `observeSubscriptions()` e `observeSubscription(id)` retornam o cadastro atual, estado e fonte configurada.
- `observeNextOccurrence(id)` busca a primeira pendente a partir de hoje, incluindo hoje. `observeUpcomingOccurrences(7)` ou `(30)` usa a janela `[hoje, hoje + dias)`.
- `observeOverdueOccurrences()` inclui pendências anteriores a hoje sem restringir ao mês atual. `observeOccurrences(fromInclusive, untilExclusive)` traz pendentes, vencidas e resolvidas dentro da janela. `observeOccurrence(id, date)` consulta a identidade individual.
- Os Flows reagem a alterações do Room. Para cruzar meia-noite com uma tela aberta, recrie a observação ao atualizar o dia financeiro.
- `credit.observeInstallmentOrigin(installmentId)` retorna IDs de cartão, compra e parcela com um join por PK. Use para navegação a partir de uma baixa no Caixa.

```kotlin
val upcoming = subscriptions.observeUpcomingOccurrences(7)
val overdue = subscriptions.observeOverdueOccurrences()
val next = subscriptions.observeNextOccurrence(subscriptionId)
val result = subscriptions.paySubscriptionOccurrence(subscriptionId, occurrenceDate, paidOn)
```

## Erros e Room v6

- `NOT_FOUND`, `PAUSED`, `CANCELLED` indicam o estado do cadastro.
- `INVALID_DETAILS`, `INVALID_ANCHOR`, `INVALID_CATEGORY`, `INVALID_SOURCE` indicam campos ou referências inválidos. Categoria ou fonte arquivada impede uma nova baixa de assinatura.
- `INVALID_OCCURRENCE`, `FUTURE_DATE`, `BEFORE_ACCOUNT_OPENING` indicam datas inválidas.
- `ALREADY_RESOLVED` ou `SKIPPED` pedem releitura da ocorrência existente.
- `INSUFFICIENT_CARD_LIMIT` e `INVALID_CARD_SCHEDULE` mantêm a ocorrência em aberto.

A migration v5 → v6 é aditiva. `subscriptions` guarda o cadastro, `subscription_terms` guarda revisões com data de vigência e `subscription_resolutions` guarda somente desfechos, com snapshot de nome, fonte, categoria e valor. As tabelas financeiras recebem vínculo anulável e índice composto único. O registro de desfecho tem PK composta compartilhada pelos canais. Gatilhos protegem a fonte exclusiva, o vínculo contra alterações e a disputa entre as tabelas de compra e ledger. A gravação do desfecho e do efeito financeiro é atômica. Nenhuma ocorrência pendente é gravada antecipadamente.

UI, cobrança automática, notificações e estorno de desfechos de assinatura ficam para outras etapas.
