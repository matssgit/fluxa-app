# Sprint 3-D — Adoção dos read models financeiros

Base: `codex/fluxa-android-sprint-3c` @ `1314c1c`. Nenhuma alteração em Room, DAO, repositório, domínio ou migrations. Schema continua na v3.

## Integrações adotadas

| Contrato (Sprint 3-C) | Onde |
|---|---|
| `observePeriodSummary(start, end)` | Início → cartão “Mês atual” (entradas, saídas, resultado) |
| `observeRecentTransactions(5)` | Início → “Recentes” |
| `observeHistoryPage(filter, page)` | Caixa → histórico com período, conta e direção na consulta |
| `TransactionHistoryFilter.direction` | Caixa → filtro Tudo / Entradas / Saídas |
| `observeBalances()` (3-A) | Início e Caixa → saldo disponível e saldo por conta (inalterado) |

Os quatro métodos novos foram adicionados ao adaptador `TransactionSource`; `RepositoryTransactionSource` só delega ao repositório.

## Cálculos removidos do frontend

- Totais de entradas/saídas do mês: não existiam na UI (estavam bloqueados por falta de contrato); agora vêm prontos de `observePeriodSummary`, inclusive `net`.
- Recentes: removidos a janela de 30 dias + `take(5)` em memória.

## Filtros removidos do frontend

- Direção (`lista.filter { direction }`) no Caixa: substituído por `TransactionHistoryFilter.direction`.
- Nenhuma reordenação local: a ordem é a do repositório (`occurredOn DESC, id ASC`). O agrupamento por dia é só apresentação.

## Dashboard

- Período: `[primeiro dia do mês atual, primeiro dia do mês seguinte)`, a partir de `FinancialCalendar().today()` (mesmo fuso do backend, sem `Instant`/UTC).
- Entradas com “+”, saídas com “−” (as magnitudes chegam positivas; o sinal é apresentação), resultado com sinal e texto “Positivo / Negativo / Neutro”. Período vazio mostra R$ 0,00 nos três e “Neutro” — não é erro.
- `Overflow` em qualquer campo aparece como “Valor acima do limite exibível”.
- Com valores ocultos, os três valores ficam mascarados e o texto Positivo/Negativo não é exibido nem anunciado (revelaria o sinal).

## Caixa

- O ViewModel só monta `TransactionHistoryFilter(accountId, fromInclusive, untilExclusive, direction)`.
- Paginação simples: `PAGE_SIZE = 50`, sempre a partir do offset 0. “Carregar mais” aumenta o `limit`; trocar qualquer filtro volta à primeira página (orientação do handoff 3-C). Pede-se `limit + 1` apenas para saber se há próxima página.
- Contagem: “N movimentações”, ou “Mais de N movimentações” quando há próxima página (não existe contrato de contagem total).

## ViewModels

- `DashboardViewModel`: `combine` de contas, categorias, saldos, resumo e recentes; sem soma, sem recorte, sem ordenação.
- `CashViewModel`: `HistoryQuery(filters, pages)` → `observeHistoryPage`; sem filtro local de direção.

## Testes

- JVM: 83 (73 da base + 10 novos em `ReadModelsViewModelsTest`). O `FakeTransactionSource` reproduz a semântica SQL do Codex (filtros combináveis, `occurredOn DESC, id ASC`, LIMIT/OFFSET, resumo com `PeriodSummary.fromCents`) e tem espiões/sentinelas: os testes confirmam que o ViewModel pede o mês certo, passa direção/conta/período para a consulta e exibe exatamente o resumo/lista devolvidos, mesmo quando eles não batem com uma soma ou um filtro locais.
- Instrumentados: `ReadModelsFlowTest` (resumo e recentes atualizando sem recarregar, mês negativo, filtros combinados, reatividade Caixa + Início) além das suítes anteriores.

## Responsividade

Matriz executada **offline** (modo avião, sem Wi-Fi e sem dados móveis) no AVD exclusivo: 320×640, 360, 390 (dark), 412dp e 360dp com fonte 1,3. Layouts existentes preservados (cartões do DS, teclado, barra flutuante, barra de status, banner de feedback).

## Pendências

- Contrato de contagem total do histórico (para “N de M movimentações”).
- Preferência de ocultar valores ainda só na sessão; reativação de conta fora do contrato; tablet sem `NavigationRail`.

## Próxima base

`claude/fluxa-android-sprint-3d-frontend` — núcleo básico fechado (contas, categorias, receitas, despesas, Caixa, Início). Cartões ainda não iniciados.
