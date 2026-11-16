# Sprint 4-B — Cartões, compras e parcelas (frontend)

Base: `codex/fluxa-android-sprint-4a` @ `fb03e5f`. Nenhuma alteração em Room v4, migrations, domínio, DAOs, repositórios, `CreditBillingCalendar` ou `InstallmentSplitter`.

## Cartões

- **Aba Cartões** (`cards`): lista de cartões ativos com o componente `CreditCardVisual`; arquivados atrás de “Mostrar arquivados”. Botão “Novo cartão”.
- **CreditCardVisual**: superfície escura (verde petróleo, forma folha), dois anéis discretos na cor escolhida, sem marca/logotipo/nome de banco. Mostra nome, *Limite disponível*, *Comprometido*, *Limite total*, *Fecha dia X* e *Vence dia Y*. Todos os valores via `FluxaAmount` (respeita “ocultar valores”).
- **Cadastro/edição** (`cards/new`, `cards/{card}/edit`): nome, limite total (`MoneyInput`, sem Float/Double; 0,00 aceito), dia de fechamento, dia de vencimento (1–31) e cor opcional (paleta Fluxa). Ajuda: “Compras feitas no dia do fechamento já entram no próximo ciclo.” A UI não calcula vencimentos.
- **Arquivar**: no fim do formulário de edição, com confirmação (“Este cartão deixará de aparecer para novas compras, mas o histórico será preservado.”).

## Compras

- **Nova compra**: pelo detalhe do cartão (cartão pré-selecionado) ou pelo botão central “+” (“Nova despesa / Nova receita / Nova compra”). Campos: cartão (ativos), valor total, nº de parcelas, categoria (ativas de despesa via `CategoryRepository.observeAll(kind = EXPENSE)`), título, estabelecimento, data, observação.
- **Sem preview**: o contrato 4-A não expõe preview de parcelas nem de vencimento. A UI mostra apenas “N parcelas” e o aviso de que valores e vencimentos são calculados pelo cartão ao salvar; em seguida abre o **detalhe da compra** com o cronograma persistido.
- **Erros tipados → mensagens** (no campo e também acima do botão Salvar, porque o campo pode estar fora da tela):

| `CreditError` | Mensagem |
|---|---|
| `INSUFFICIENT_LIMIT` | Limite disponível insuficiente para esta compra. |
| `CARD_ARCHIVED` | Este cartão não aceita novas compras. |
| `CATEGORY_NOT_FOUND` / `CATEGORY_ARCHIVED` / `CATEGORY_NOT_EXPENSE` | Escolha uma categoria de despesa válida. |
| `INVALID_INSTALLMENT_COUNT` | O número de parcelas não é compatível com o valor da compra. |
| `INVALID_TITLE` / `INVALID_AMOUNT` | Informe um título. / Informe um valor maior que zero. |
| `INVALID_SCHEDULE_DATE` | Data fora do intervalo aceito para o calendário do cartão. |
| `LIMIT_BELOW_COMMITTED` (edição) | O novo limite é menor que o valor já comprometido. |
| `HAS_PAID_INSTALLMENT` / `PURCHASE_CANCELLED` | Mensagens próprias no detalhe. |
| `DUPLICATE_PURCHASE_ID` | Tratado como recuperação: se a compra com o mesmo UUID existe, o envio é dado como concluído. |

- **Envio**: UUID gerado no primeiro envio e reutilizado em novas tentativas; toques repetidos ignorados.

## Parcelas

- Detalhe da compra: lista `n/N`, valor, “Vence dd/mm/aaaa” e estado (Pendente/Paga/Cancelada com ícone + texto).
- Detalhe do cartão: “Próximas parcelas” reúne as parcelas **pendentes já persistidas** das compras do cartão e as ordena por vencimento (apresentação; mostra até 12).
- Pagamento de parcela **não** foi implementado.

## Cancelamento

- “Cancelar compra” só aparece quando a compra está ativa e não tem parcela paga (o backend continua decidindo). Confirmação: “As parcelas pendentes serão canceladas e o limite será liberado. O histórico será preservado.” Compra cancelada abre em modo leitura, com nota, valor riscado e pílula “Cancelada”.

## ViewModels

`CardsViewModel`, `CardFormViewModel`, `CardDetailViewModel`, `PurchaseFormViewModel`, `PurchaseDetailViewModel`. Todos sobre `CreditSource` (recorte do `CreditRepository` para testes JVM; `RepositoryCreditSource` só delega).

## Integração com repositories

| Contrato 4-A | Uso |
|---|---|
| `observeCards(includeArchived = true)` | Aba Cartões (ativos/arquivados) e seletor de cartão (`includeArchived = false`) |
| `observeCard(id)` | Detalhe, formulário de edição, detalhe da compra |
| `observePurchases(cardId)` | Detalhe do cartão; detalhe da compra localiza a compra por UUID (não há `observePurchase(id)`) |
| `observeInstallments(purchaseId)` | Cronograma em cada compra |
| `createCard` / `updateCard` / `archiveCard` / `createPurchase` / `cancelPurchase` | Formulários e ações |

Limite total, comprometido e disponível vêm de `CreditCardSummary`. Nada é somado na UI.

## Fluxos

Room → `CreditRepository` (Flow) → `CreditSource` → ViewModel (`StateFlow`) → Compose. Criar/cancelar compra atualiza cartão, limite e listas automaticamente.

## Erros

Ver tabela em **Compras**. Erros de backend aparecem no campo correspondente e, resumidos, acima do botão Salvar (`purchase-form-error`, `liveRegion` assertivo), porque em 320–360dp o campo pode estar fora da tela. Falhas de escrita (exceção) viram mensagem genérica e permitem nova tentativa com o mesmo UUID.

## Responsividade

- Matriz instrumentada offline (modo avião, Wi-Fi e dados desligados): 320×640, 360, 390 (escuro), 412dp e 360dp com fonte 1,3 — **45/45 em todas**.
- Rótulo “Cartões” cortado com fonte 1,3: a causa era o `clip(extraLarge)` de cada item da barra — o raio grande recortava as primeiras/últimas letras quando o rótulo ocupa toda a largura. Agora o item usa `shapes.medium`; além disso a ação central ocupa 64dp fixos e as margens laterais foram reduzidas. Fonte não foi reduzida. Ver `screens/07` e `screens/08`.

## Acessibilidade

- Alvos ≥ 48dp; estados (Ativa/Cancelada, Pendente/Paga/Cancelada) com texto + ícone, nunca só cor; compra cancelada também riscada.
- Botões destrutivos com ícone e rótulo explícitos e confirmação.
- Linhas de compra/parcela com `stateDescription`; erros anunciados.

## Testes

- JVM: 100/100 (12 novos em `CreditViewModelsTest`, com `FakeCreditSource` usando `CreditPurchaseRules`/`CreditLimit` do domínio).
- Instrumentados: 45/45 (6 novos em `CreditFlowTest`: criar cartão, compra à vista, parcelada pelo “+” com soma = total, limite insuficiente, cancelamento libera limite e preserva histórico, persistência ao reabrir).
- Morte de processo: `am force-stop` + início a frio (PID novo), cartão e compra preservados.
- Capturas em `screens/`.

## Pendências / contratos sugeridos ao Codex

1. Preview de parcelas e do primeiro vencimento (`previewPurchase(draft)`), para mostrar o cronograma antes de salvar.
2. `observePurchase(id)` para o detalhe da compra.
3. Consulta global de parcelas pendentes por vencimento (próximos compromissos), se o Início for exibi-los.
4. Pagamento de parcela, fatura e valor da próxima fatura (fora do escopo).

## Base recomendada

`claude/fluxa-android-sprint-4b-frontend`.
