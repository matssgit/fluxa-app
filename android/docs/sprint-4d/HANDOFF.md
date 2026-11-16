# Sprint 4-D — Pagamento de parcelas, estorno e Caixa (frontend)

Base: `codex/fluxa-android-sprint-4c` @ `97fd797`. Nenhuma alteração em Room v5, migrations, domínio, DAOs, repositórios, comandos ou constraints.

## Payment UI

- A linha da parcela (detalhe da compra e "Próximas parcelas" do cartão) abre o **detalhe da parcela** (`cards/{card}/purchases/{purchase}/installments/{installment}`). Parcela cancelada fica só leitura na lista.
- **Pendente**: botão “Pagar parcela” → `FluxaBottomSheet` com resumo fixo (compra, `Parcela X/Y`, valor, vencimento). O valor não é editável e não há campo de categoria.
  - **Conta**: só contas ativas (`AccountRepository.observeAll(includeArchived = false)`), com nome, tipo e saldo atual vindo de `TransactionRepository.observeBalances()` (via `BalanceAmount`/`FluxaAmount`). Saldo negativo não bloqueia; o texto avisa que o saldo pode ficar negativo. Conta única é escolhida automaticamente.
  - **Data**: hoje por padrão; o seletor bloqueia datas futuras (`maxDate = hoje`). Pagamento antecipado ou atrasado é aceito. O backend continua sendo a fonte de verdade (data futura e anterior à abertura são mapeadas).
  - **Continuar** → resumo de confirmação (“Pagamento pela conta: …”, “Data: …”) → **Confirmar pagamento**.
  - Sucesso fecha o sheet e mostra feedback. Parcela, limite, saldo, Caixa, Início e recentes atualizam pelos Flows, sem atualização manual.
  - `AlreadyPaid` = sucesso idempotente (“Esta parcela já estava paga. Nada foi duplicado.”). Prevalece a primeira baixa, lida de `observeInstallmentPayment`.
- **Paga**: pílula “Paga” e o cartão “Pagamento” com **Pago em**, **Conta** e **Valor pago**, separados do **Vencimento** original no resumo.

## Undo UI

- Parcela paga mostra “Desfazer pagamento” (destrutivo, secundário, ícone de restaurar) com confirmação: “A saída será removida do Caixa, a parcela voltará a ficar pendente e o valor voltará a comprometer o limite do cartão.”
- Usa apenas `undoInstallmentPayment(installmentId, expectedTransactionId)`, com o UUID da baixa lida de `observeInstallmentPayment`.
- `INSUFFICIENT_LIMIT_TO_UNDO` → “Não é possível desfazer este pagamento porque o limite liberado já foi utilizado por outras compras.” A UI não compensa nem altera compras.

## ViewModels

| ViewModel | Responsabilidade |
|---|---|
| `InstallmentDetailViewModel` | Leitura (compra, parcela, cartão, baixa + conta) e estorno. `CommandStatus` Idle/Submitting/Success/Error. |
| `InstallmentPaymentViewModel` | Formulário de baixa: contas+saldos, data, etapas `Form`/`Confirm`, `CommandStatus`, mapeamento de erros. |
| `PurchaseDetailViewModel` | Inalterado: `canCancel` continua exigindo nenhuma parcela `PAID` (estado real). |
| `TransactionFormViewModel` | Detecta `installmentId != null` e entra em modo leitura gerenciado. |

Os dois novos ficam no back stack da rota da parcela, separados do `CardDetailViewModel`. Toques duplos são ignorados enquanto `Submitting`. A integridade continua no backend.

## Caixa

- A saída gerada aparece uma única vez no ledger existente (Caixa, Início/Recentes, resumo do mês, saldos). A linha ganha só a marca discreta “Cartão” (pílula + `stateDescription`), sem repetir dados.
- Abrir essa saída mostra **modo leitura**: descrição, valor, conta (mesmo se arquivada depois), data, categoria e origem “Parcela de cartão de crédito”, mais a orientação para desfazer pela parcela. Não há Salvar nem Cancelar lançamento. Se algum caminho ainda chamar editar/cancelar, `MANAGED_BY_INSTALLMENT` mostra essa orientação.
- A parcela `PAID` não cria segunda saída: obrigação (crédito) e ledger (Caixa) continuam separados.

## Navegação entre Transaction e Purchase

O contrato público só expõe `FinancialTransaction.installmentId`. Não há consulta pública `installmentId → compra/cartão` (`CreditDao.findInstallment` é interno ao repositório). Para não varrer cartões, compras e parcelas na UI, **“Ver compra/parcela” não foi implementado**. O modo leitura explica onde desfazer.
Contrato sugerido: `observeInstallmentOrigin(installmentId): Flow<InstallmentOrigin?>` com `cardId`, `purchaseId`, `number`.

## Erros

| Erro | Onde | Mensagem |
|---|---|---|
| `ACCOUNT_NOT_FOUND` / `ACCOUNT_ARCHIVED` | Pagamento (volta para a conta) | Esta conta não está disponível. Escolha outra conta ativa. |
| `PAYMENT_DATE_IN_FUTURE` | Pagamento (data) | A data do pagamento não pode ser no futuro. |
| `PAYMENT_BEFORE_ACCOUNT_OPENING` | Pagamento (data) | A data do pagamento é anterior à abertura desta conta. |
| `INSTALLMENT_NOT_FOUND` | Ambos | Esta parcela não foi encontrada. |
| `INSTALLMENT_CANCELLED` | Ambos | Esta parcela foi cancelada e não pode ser paga. |
| `PURCHASE_CANCELLED` | Ambos | A compra desta parcela foi cancelada. |
| `PAYMENT_CHANGED` / `INVALID_PAYMENT_LINK` | Ambos | O pagamento desta parcela mudou. Confira os dados atualizados. |
| `INSTALLMENT_NOT_PAID` | Estorno | Este pagamento já foi desfeito. A parcela está pendente. |
| `INSUFFICIENT_LIMIT_TO_UNDO` | Estorno | (texto acima) |
| `MANAGED_BY_INSTALLMENT` | Caixa | Orientação para desfazer pela parcela. |
| Exceção inesperada | Ambos | Não foi possível registrar/desfazer… Tente novamente (permite repetir). |

Códigos internos nunca aparecem. Se a conta escolhida for arquivada com o sheet aberto, o formulário volta para a escolha da conta com erro.

## Responsividade

- Sheet de pagamento: área rolável proporcional à altura da tela, para o cabeçalho e o botão “Continuar” caberem em 320×640 com fonte 1,3. O saldo fica abaixo do nome da conta, para nomes longos não serem cortados.
- Confirmação do pagamento: “Confirmar pagamento” e “Voltar” empilhados em largura total. Lado a lado, o rótulo cortava em 360dp.
- `ConfirmArchiveSheet` (componente comum): botões empilhados por padrão. Lado a lado, “Manter compra”/“Cancelar compra” e “Manter pagamento”/“Desfazer pagamento” ficavam ilegíveis em 320dp/1,3. A opção `stacked = false` preserva o layout antigo. Isso afeta todas as confirmações destrutivas do app (arquivar conta/categoria/cartão, cancelar lançamento/compra, desfazer pagamento).
- Testes antigos ajustados para viewport 320dp/1,3 (combinação extra, fora da matriz pedida), sem enfraquecer asserções:
  - `CreditFlowTest.cancelReleasesLimitAndKeepsHistory` rola até a nota de cancelamento;
  - `AccountsAndCategoriesFlowTest.createEditArchiveAndReactivateCategories` usa clique semântico em “Reativar”.
  As duas falhas existiam na base 4-C intocada nessa combinação.

## Acessibilidade

- Estado anunciado (“Situação: Paga/Pendente/Cancelada”) com ícone e texto, nunca só cor.
- Linhas rótulo/valor mescladas (“Vencimento 17/12/2026”, “Pago em …”, “Conta …”).
- Contas como `RadioButton` em grupo; destrutivo com ícone e rótulo explícitos e confirmação; erros com `liveRegion` assertivo (`ErrorBanner`/`FieldError`). Alvos ≥ 48dp.

## Testes

- JVM: `InstallmentPaymentViewModelsTest` (16), com `FakeCreditSource` estendido usando `InstallmentPaymentRules`, `matchesPayment` e `CreditLimit` do domínio e gravando no `FakeTransactionSource`. Cobre:
  - estado inicial, conta/data, sucesso, `AlreadyPaid`, conta rejeitada, conta arquivada durante a confirmação, data anterior à abertura e falha inesperada;
  - estorno com sucesso, limite insuficiente e erro genérico;
  - detalhe PENDING/PAID/CANCELLED, compra paga sem cancelar e Caixa em modo leitura.
- Instrumentados: `InstallmentPaymentFlowTest` (6), cobrindo:
  - pagar pela UI: saldo, limite, Caixa e Início atualizados;
  - idempotência sem saída duplicada;
  - desfazer: pendente, saída some, saldo e limite voltam;
  - desfazer bloqueado por limite reutilizado;
  - lançamento gerado em modo leitura no Caixa;
  - pagar/desfazer persistem entre aberturas.

## Pendências

1. `observeInstallmentOrigin(installmentId)` para “Ver compra/parcela” a partir do Caixa.
2. Fatura, pagamento de fatura inteira, baixa em lote (fora do escopo).
3. Ao mudar o modo escuro com o app aberto, a navegação volta ao Início (comportamento anterior, fora desta sprint).

## Base recomendada

`claude/fluxa-android-sprint-4d-frontend`.
