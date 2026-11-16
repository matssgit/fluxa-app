# Sprint 3-B — Lançamentos, Caixa e Saldos (frontend)

Base: `codex/fluxa-android-sprint-3a` @ `f0b17b1`. Nenhuma regra financeira, DAO, repositório ou schema Room (v3) foi alterado. Todo saldo exibido vem de `TransactionRepository.observeBalances()`.

## Telas

| Tela | Rota | Conteúdo |
|---|---|---|
| Início | `dashboard` (aba) | Saldo disponível (consolidado do repositório), nº de contas ativas e até 5 lançamentos efetivados dos últimos 30 dias |
| Caixa | `cash` (aba) | Saldo disponível, saldo atual por conta (tiles que também filtram), histórico agrupado por dia com filtros de período, direção e conta |
| Sheet do “+” | — | “Nova despesa” / “Nova receita” no `FluxaBottomSheet` padrão |
| Lançamento | `transactions/new?direction=` e `transactions/{id}` | Formulário de criação/edição; cancelamento com confirmação; cancelados abrem só para leitura |
| Contas | `accounts` | Agora mostra o **saldo atual** de cada conta (repositório) além do saldo inicial |

## ViewModels

- `TransactionFormViewModel` → `TransactionFormUiState`: direção, valor (texto), conta, categoria, data, descrição, listas de contas/categorias **ativas** (Flows), erros por campo, `isSaving`/`isCancelling`, `completion` (Created/Updated/Cancelled), modo somente leitura para cancelados.
- `CashViewModel` → `CashUiState`: `available`, `accounts` (com `CalculatedBalance`), `filters`, `days` (agrupados), `entryCount`, erro.
- `DashboardViewModel` → `DashboardUiState`: `available`, `activeAccounts`, `recent`.
- `AccountsViewModel` passou a receber `TransactionSource` para exibir saldos.
- `CategoryFormViewModel` trata `CategoryKindInUse` com mensagem própria.

`TransactionSource` é um recorte do `TransactionRepository` só para permitir testes JVM (o repositório usa `withTransaction` do Room). Em produção, `RepositoryTransactionSource` delega cada chamada ao repositório. Os testes JVM usam `FakeTransactionSource`, que aplica as próprias `TransactionRules` e `BalanceCalculator` do Codex.

## Como os contratos foram usados

- **Criação idempotente**: o UUID é gerado no primeiro envio e reutilizado nos reenvios. Em `DUPLICATE_ID` o formulário consulta o mesmo ID e conclui se o lançamento existe.
- **Valor**: `MoneyInput` → `Money` estritamente positivo; a direção dá o sinal. Sem Float/Double.
- **Data**: padrão `FinancialCalendar().today()` (mesmo fuso do backend); o seletor bloqueia datas futuras e `FUTURE_DATE` vira erro no campo.
- **Erros tipados** viram mensagens no campo (`INVALID_AMOUNT`, `FUTURE_DATE`, conta/categoria inativa, direção incompatível) ou banner (`NOT_FOUND`, `CANCELLED`). Se outro fluxo cancelou o item, o formulário relê o registro e vira somente leitura.
- **Vínculo arquivado em edição**: o item mostra o aviso e exige escolher conta/categoria ativa antes de salvar (contrato).
- **Overflow**: `CalculatedBalance.Overflow` é exibido como “Valor acima do limite exibível” com ícone, nunca como zero; respeita a ocultação de valores.
- **Cancelamento**: confirmação explícita dizendo que não é exclusão; o item continua no histórico com pílula “Cancelado” e valor riscado.

## Contratos pedidos ao Codex (não improvisados na UI)

1. **Totais do período** (entradas e saídas efetivadas): `observePeriodTotals(from, until): Flow<PeriodTotals>` com `CalculatedBalance` para cada direção. O Início não mostra esses totais até existir o contrato.
2. **Filtro por direção no histórico**: `TransactionHistoryFilter.direction`. Hoje o filtro Receita/Despesa é aplicado na UI sobre o resultado do período (já limitado).
3. **Paginação/limite do histórico**: `limit`/`offset` ou Paging 3. A UI só consulta períodos limitados (mês, mês anterior, 90 dias, ano) para não carregar o histórico inteiro; um ano com muitos lançamentos ainda carrega tudo daquele ano.
4. **Recentes**: `observeRecent(limit)` evitaria carregar 30 dias para exibir 5.

## Correção das falhas em 320dp

Reproduzidas no AVD exclusivo em 320×640dp (`wm size 640x1280`, `density 320`) sobre `f0b17b1` sem alterações: `invalidAmountShowsErrorAndDoesNotSave` e `createEditArchiveAndReactivateCategories`.

- **Causa no teste**: após digitar o nome, o teclado reduz a área da `LazyColumn`; `field-account-amount` e `icon-work` deixam de estar compostos e o teste interagia sem rolar. Correção: rolar até o nó (`performScrollToNode`) antes de interagir. Nenhuma asserção foi removida ou relaxada.
- **Causa no layout**: o rodapé fixo tinha Salvar + Arquivar (~140dp), deixando pouquíssimo conteúdo visível com teclado em 640dp de altura. Correção: só Salvar fica fixo; Arquivar/Reativar/Cancelar lançamento foram para o fim do conteúdo.
- Em 360dp, um toque por coordenada caía na barra flutuante sobre um item visível do Caixa. O conteúdo rola por trás da barra por design (com folga inferior), então os testes de lista acionam o clique semântico do item após rolar.

## Validação

- JVM: 69 (55 da base + 14 novos).
- Instrumentados no AVD exclusivo `claude_fluxa_2b` (`ANDROID_SERIAL=emulator-5580`): 24 testes + 2 de captura sob demanda, verdes em 320×640, 360, 390 (dark), 412dp e 360dp com fonte 1,3.
- Capturas em `docs/sprint-3b/screens/`.

## Pendências

- Contratos 1–4 acima.
- Preferência “ocultar valores” continua só na sessão (`LocalPreferences` sem escrita).
- Reativação de conta segue fora do contrato.
- Formulários não preservam o que foi digitado após morte do processo (os dados salvos persistem).
- Tablet: conteúdo limitado a 640dp; `NavigationRail` não implementado.
