# Sprint 4-C — Pagamento e estorno de parcelas

Base `36e1ffb`. O frontend recebe o repositório por `FluxaApplication.credit`.

## Pagamento

`suspend fun payInstallment(installmentId: UUID, accountId: UUID, paidOn: LocalDate): InstallmentPaymentResult`

A baixa grava `PENDING → PAID` e exatamente uma transação `EXPENSE` efetivada na mesma transação Room. O valor vem da parcela, a categoria vem da compra e a descrição é `Pagamento • <título> • <número>/<total>`. O vínculo é `FinancialTransaction.installmentId`. A data financeira `occurredOn` do ledger recebe `paidOn` e o vencimento da parcela permanece intacto. Pagamentos antecipados e atrasados são válidos. O relógio e o fuso financeiro vêm de `FinancialCalendar`.

`Paid(transaction)` confirma uma baixa nova. `AlreadyPaid(transactionId)` confirma que a parcela já tem baixa válida, inclusive em tentativa concorrente. Nessa resposta prevalecem os dados da primeira baixa. `Rejected(error)` indica uma rejeição de negócio.

A conta deve existir e estar ativa. O pagamento pode deixar o saldo negativo. Datas futuras e anteriores à abertura da conta são rejeitadas. O ledger preserva o recorte de abertura para saldos, portanto uma data anterior à abertura ficaria fora do saldo individual. Compras existentes podem ser pagas mesmo se o cartão ou a categoria original tiver sido arquivado. A categoria do histórico continua sendo a da compra.

## Estorno

`suspend fun undoInstallmentPayment(installmentId: UUID, expectedTransactionId: UUID): InstallmentPaymentResult`

Use o UUID da baixa que o usuário confirmou. Uma tentativa atrasada não pode estornar um pagamento posterior da mesma parcela. O estorno muda `PAID → PENDING` e apaga somente a transação gerada vinculada, na mesma transação Room. `Undone(transactionId)` confirma a remoção. A parcela e o vencimento permanecem. Um novo pagamento recebe outro UUID. Transações manuais continuam com cancelamento lógico.

O cartão precisa ter limite disponível para voltar a comprometer a parcela. Se o limite liberado tiver sido reutilizado ou reduzido e não comportar a parcela, o estorno é rejeitado sem remover a baixa. Cancelar compra que ainda tenha parcela paga continua retornando `CreditError.HAS_PAID_INSTALLMENT`. Para voltar a cancelar a compra, estorne as baixas primeiro.

## Estados e leituras

- `PENDING → PAID` por pagamento. `PAID → PENDING` pelo estorno. `PENDING → CANCELLED` pelo cancelamento da compra. Parcelas canceladas não podem ser pagas.
- `observeInstallmentPayment(installmentId): Flow<FinancialTransaction?>` busca pelo índice do vínculo. O resultado contém conta, data, valor, categoria e ID da transação. Uma parcela reaberta emite `null`. `observeInstallments(purchaseId)` continua fornecendo vencimento e estado.
- Caixa, recentes, resumo do período e saldos usam o ledger existente. Em uma timeline financeira, mostre a saída da transação uma vez. A parcela `PAID` serve ao histórico de crédito. Após o estorno ela volta ao conjunto de obrigações pendentes.
- `observeCard(id)` calcula limite da mesma forma. A baixa libera limite por deixar de ser pendente. O estorno recompromete o valor. O saldo de conta volta a ser calculado sem a saída excluída. O consolidado segue a regra vigente de conta ativa com `includeInAvailable`.
- `installmentId == null` identifica a transação manual nesta sprint. `TransactionRules.edit/cancel` devolve `MANAGED_BY_INSTALLMENT` para baixas geradas. O Caixa deve oferecer leitura e encaminhar o estorno ao fluxo da parcela. O formulário atual usa a mensagem genérica já existente para a nova rejeição.

## Erros

- `INSTALLMENT_NOT_FOUND`, `INSTALLMENT_CANCELLED`, `INSTALLMENT_NOT_PAID`, `PURCHASE_CANCELLED` — atualizar o estado exibido.
- `ACCOUNT_NOT_FOUND`, `ACCOUNT_ARCHIVED`, `PAYMENT_DATE_IN_FUTURE`, `PAYMENT_BEFORE_ACCOUNT_OPENING` — corrigir conta ou data.
- `PAYMENT_CHANGED` — reler o pagamento e pedir confirmação do novo estado.
- `INSUFFICIENT_LIMIT_TO_UNDO` — informar que o cartão não tem margem para voltar a comprometer a parcela.
- `INVALID_PAYMENT_LINK` — o estado e o vínculo persistidos divergem. Recarregue a leitura e preserve os registros existentes. Falhas inesperadas do SQLite são propagadas com rollback.

## Room v5

A migration aditiva adiciona `financial_transactions.installmentId` anulável com FK `RESTRICT` e índice único. Registros antigos mantêm `null`. O SQLite aceita múltiplos `NULL` nesse índice. A relação impede apagar uma parcela vinculada. A baixa, o estorno e o cancelamento de compra são serializados pela transação Room. A mudança de estado também tem predicado de estado de origem.

## Exemplo

```kotlin
val payment = credit.observeInstallmentPayment(installmentId)
val paid = credit.payInstallment(installmentId, selectedAccountId, selectedDate)
val undone = credit.undoInstallmentPayment(installmentId, confirmedPaymentId)
```

A etapa de frontend deve ligar os comandos às ações do detalhe de parcela, refletir os erros específicos e manter baixas geradas fora do fluxo de edição comum. Fatura, baixa em lote e estorno de múltiplas parcelas ficam para um contrato posterior.
