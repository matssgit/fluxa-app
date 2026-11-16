# Sprint 4-A — Cartões, compras e parcelas

Base `67c5ea3`. Os consumidores recebem `CreditRepository` por `FluxaApplication.credit`. As operações são locais. Contas de caixa e cartões mantêm contratos separados.

## Modelos

- `CreditCard` tem UUID, `CreditCardDetails` (nome, limite total em `Money`, dias de fechamento e vencimento em `BillingDay`, cor opcional), datas de criação/edição e `archivedAt`. Limite zero é permitido e bloqueia compromissos positivos novos.
- `PurchaseDraft` recebe cartão (`cardId`), categoria de despesa, título, total em `Money`, quantidade de parcelas, `purchaseDate: LocalDate`, estabelecimento e observações opcionais. `Purchase` acrescenta UUID, `firstDueDate`, datas de criação/edição e `cancelledAt`. O estado `ACTIVE` ou `CANCELLED` deriva de `cancelledAt`.
- `Installment` tem UUID, compra, número iniciado em 1, valor, vencimento e estado `PENDING`, `PAID` ou `CANCELLED`. O estado `PAID` está reservado para o fluxo de baixa posterior.

## Fechamento e primeiro vencimento

O `CreditBillingCalendar` calcula o próximo fechamento **estritamente posterior à compra**. Compra anterior ao fechamento do mês usa esse fechamento. Compra no dia de fechamento ou depois dele usa o fechamento do mês seguinte. Para o primeiro vencimento, o dia escolhido precisa ficar **depois** desse fechamento. Se cair no mesmo dia ou antes dele, o vencimento vai para o mês seguinte. Tanto o fechamento quanto o vencimento são limitados ao último dia do mês quando necessário.

Com fechamento 10 e vencimento 17, compras em 09/11 vencem primeiro em 17/11. Compras em 10/11 ou 11/11 vencem primeiro em 17/12. Se o vencimento nominal for 5 e o fechamento for 10, o vencimento da fatura daquele fechamento cai no mês seguinte. Se os dois dias coincidirem pelo ajuste de fevereiro, vale a mesma regra.

O Android usa o ciclo definido nesta sprint. Na web, a primeira parcela era sempre gerada no mês seguinte ao da compra. O `InstallmentSplitter` continua distribuindo os centavos entre as últimas parcelas, com soma exata e diferença máxima de um centavo. Cada parcela custa ao menos um centavo. O calendário preserva o dia pretendido ao voltar de um mês curto e grava cada data resolvida. Editar o cartão afeta apenas compras criadas após a edição.

## Limite e comandos

`CreditCardSummary` expõe `card`, `totalLimit`, `committedAmount` e `availableLimit`. O SQLite soma parcelas pendentes de compras ativas. O disponível é limite total menos comprometido. Nenhum desses dois valores calculados é armazenado. O valor da próxima fatura fica para o fluxo de fatura.

| Método suspenso | Contrato |
|---|---|
| `createCard(details)` | Cria e devolve o cartão em `CreditResult.Saved`. |
| `updateCard(id, details)` | Edita nome, cor, calendário e limite. Rejeita limite inferior ao comprometido; o limite exato é aceito. |
| `archiveCard(id)` | Devolve `true` na transição. Compra nova passa a ser rejeitada e o histórico segue acessível. |
| `createPurchase(id, draft)` | Valida cartão, categoria ativa de despesa, valor, parcelamento e limite dentro da transação que grava compra e parcelas. Reuse o UUID em uma nova tentativa da mesma operação. |
| `cancelPurchase(id)` | Mantém compra e parcelas no histórico. Cancela a compra e suas parcelas pendentes em uma transação. O limite derivado volta a ficar disponível. Uma parcela paga bloqueia todo o cancelamento. |

`CreditResult.Rejected` carrega um `CreditError`. Ausência, arquivamento, categoria incompatível, título/valor/quantidade inválidos, calendário fora da faixa, limite insuficiente e repetição de UUID são erros explícitos. A edição usa `LIMIT_BELOW_COMMITTED`; o cancelamento usa `PURCHASE_NOT_FOUND`, `PURCHASE_CANCELLED` ou `HAS_PAID_INSTALLMENT`. Erros inesperados do banco são propagados e a transação é revertida.

## Leituras reativas

- `observeCards(includeArchived = false)` lista resumos por nome e UUID. `observeCard(id)` inclui cartão arquivado e retorna `null` se ausente.
- `observePurchases(cardId, status = null)` lista compras pela data decrescente e UUID crescente. Use `ACTIVE` ou `CANCELLED` para filtrar.
- `observeInstallments(purchaseId)` lista parcelas por número crescente.

```kotlin
val cards = credit.observeCards()
val limits = credit.observeCard(cardId)
val activePurchases = credit.observePurchases(cardId, PurchaseStatus.ACTIVE)
val schedule = credit.observeInstallments(purchaseId)
val result = credit.createPurchase(submissionId, draft)
```

## Room v4

A migration v3 → v4 adiciona `credit_cards`, `credit_purchases` e `credit_installments`, preservando o ledger e o catálogo. O schema exportado mantém a cadeia desde a v1. As FKs usam `RESTRICT`, inclusive de parcela para compra, para proteger o histórico. O índice cartão/data atende o histórico; categoria atende sua FK e a proteção de tipo. `UNIQUE(purchaseId, number)` protege a numeração e atende as consultas por compra. Vencimento e estado ainda não possuem consultas globais que justifiquem índices próprios. O gatilho anterior também bloqueia a troca de tipo de categorias vinculadas a compras, incluindo canceladas.

## Próxima etapa

Usar os resumos de limite e os Flows de compras e parcelas nas telas. O formulário deve gerar um UUID por envio lógico, exibir rejeições tipadas e manter o calendário das parcelas já contratadas. Edição de compra, baixa e cancelamento parcial após pagamento, fatura e valor da próxima fatura ficam para suas respectivas decisões de produto.
