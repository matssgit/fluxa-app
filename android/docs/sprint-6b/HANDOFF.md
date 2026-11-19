# Sprint 6-B — Metas, reservas e Dashboard (frontend)

Base: `codex/fluxa-android-sprint-6a` @ `f7a4a1f`. Nenhuma alteração em Room v7, migrations, domínio, repositórios ou constraints. Meta é **reserva dentro de uma conta**: não cria Transaction, não é conta separada nem transferência.

## Goal UI

- Acesso por **Mais → Metas** (`goals`). A navegação principal não mudou.
- Seções **Em andamento** e **Concluídas**; **Arquivadas** atrás de “Mostrar arquivadas”.
- Cada `GoalCard` (sobre `FluxaCard`) mostra:
  - nome (até 2 linhas) e percentual;
  - “R$ X reservados de R$ Y” (em `FlowRow`, quebra sem cortar valores);
  - barra de progresso;
  - “Faltam R$ Z” ou “Objetivo atingido”;
  - conta, data alvo e estado (pílula com ícone, para concluída e arquivada).
- Todos os números vêm de `observeGoals` (`GoalSummary`): reservado, restante e `progressBasisPoints`. O percentual exibido é `bp / 100`, inteiro e arredondado para baixo como o contrato. A barra usa a fração só para desenhar; nenhum valor monetário passa por `Float`/`Double`.

## Cadastro/Edição

- Campos: nome, valor objetivo (`MoneyInput`), conta (só ativas; conta única é escolhida automaticamente) e data alvo opcional (definir/remover).
- Explicação fixa: “O valor reservado continua nessa conta, mas deixa de contar como dinheiro disponível para gastar.”
- Edição do target abaixo do reservado mostra “O objetivo não pode ser menor que o valor já reservado.”
- Troca de conta com histórico mostra “A conta não pode ser trocada porque esta meta já tem movimentos de reserva.” Nenhuma transferência implícita.

## Allocate

- “Reservar dinheiro” abre `FluxaBottomSheet` com o campo de valor no topo e o resumo da conta: saldo real, reservado na conta, **disponível** (de `observeAccountReserves`, nada subtraído na UI) e restante da meta.
- Erros do contrato:
  - disponível insuficiente → “Esta conta não possui valor disponível suficiente para essa reserva.”;
  - acima da meta → “Essa reserva ultrapassa o valor objetivo da meta.”.
- Sucesso: “R$ X foram reservados para esta meta. O saldo real da conta não mudou.” O valor é o gravado pelo contrato e respeita “ocultar valores”.

## Release

- “Liberar reserva”: mesmo sheet, com “O valor voltará a ficar disponível para uso na conta.”
- Acima do reservado → “O valor é maior que o reservado nesta meta.”
- Feedback: “R$ X foram liberados. O valor voltou a ficar disponível na conta.” Nunca “receita”.

## Completion

- “Concluir meta” com confirmação: “Concluir a meta não gasta nem libera o valor reservado.”
- `TARGET_NOT_REACHED` vira uma mensagem clara; a dica “Disponível quando o valor reservado alcançar o objetivo” aparece enquanto falta valor.
- Concluída: reserva, histórico e conta preservados. Não oferece “Reservar”; “Liberar reserva” continua disponível.

## Archive

- “Arquivar meta” (destrutivo, com confirmação). `HAS_RESERVED_FUNDS` → “Libere todo o valor reservado antes de arquivar esta meta.”
- O erro aparece junto dos botões de ação, não no topo da tela (no topo, ficaria fora da tela; encontrado no teste instrumentado).

## History

`observeMovements`, do mais recente ao mais antigo: “Reservado + R$ 500” / “Liberado − R$ 100”, com a data. A legenda explica que os sinais indicam o movimento da **reserva**, não entradas ou saídas da conta.

## Dashboard

Usa `goals.observeReserveBalances()`. O card “Saldo disponível” mostra exatamente:
- **Disponível** = `availableBalance`, que **já desconta as reservas elegíveis**;
- **Saldo total** = `totalBalance`;
- **Reservado** = `totalReserved`, com a legenda “O disponível já desconta o valor reservado em metas.”

Sem reservas, o card fica como antes. **Não há nenhuma subtração na UI**. Testado com 5.000/1.500/3.500 (nunca 2.000) e com valores-sentinela de conta excluída exibidos verbatim. O Caixa usa o mesmo contrato para que “Disponível” não divirja entre as telas.

## Account summaries

No Caixa, cada card de conta com reserva mostra **Reservado** e **Disponível** dessa conta (`AccountReserveSummary`), abaixo do saldo atual. Não houve redesign da tela de contas. `GoalMovement` não entra no ledger: Caixa, recentes e resumo do mês continuam só com movimentos financeiros reais.

## Negative availability

Disponível negativo é exibido como veio (ex.: −R$ 400,00), em terracota, sempre com **ícone + texto**:
- detalhe da meta: “O valor reservado está acima do saldo atual desta conta.”;
- card de conta no Caixa: versão curta, “Reserva acima do saldo”;
- card consolidado: “O valor reservado está acima do saldo disponível das contas.”.

A reserva nunca é zerada nem escondida.

## ViewModels

| ViewModel | Responsabilidade |
|---|---|
| `GoalsViewModel` | Lista por estado (`observeGoals(includeArchived = true)`), alternar arquivadas |
| `GoalFormViewModel` | Cadastro/edição; validação de preenchimento; erros do contrato por campo |
| `GoalDetailViewModel` | Resumo, reservas da conta, histórico; concluir/arquivar (`CommandStatus`) |
| `GoalAllocationViewModel` | Reservar/liberar (modo), toque duplo bloqueado, valor gravado para o feedback |
| `DashboardViewModel` / `CashViewModel` | Novo parâmetro `reserves: Flow<ReserveBalanceSnapshot>?`; exibem os totais do contrato |

Todos sobre `GoalDataSource` (recorte do `GoalRepository` para testes JVM). Fluxo: Room → Repository Flow → ViewModel (`StateFlow`) → Compose. Reservar, liberar e lançamentos reais atualizam metas, Caixa e Dashboard sem refresh manual.

## Errors

| `GoalError` | Mensagem |
|---|---|
| `NOT_FOUND` / `ARCHIVED` / `COMPLETED` | Meta não encontrada / arquivada / já concluída (sem novas reservas) |
| `INVALID_NAME` / `INVALID_TARGET` / `INVALID_AMOUNT` | Preenchimento |
| `ACCOUNT_NOT_FOUND` / `ACCOUNT_ARCHIVED` | Esta conta não está disponível. Escolha uma conta ativa. |
| `ACCOUNT_CHANGE_NOT_ALLOWED` | A conta não pode ser trocada… |
| `TARGET_BELOW_RESERVED` | O objetivo não pode ser menor que o valor já reservado. |
| `ALLOCATION_EXCEEDS_TARGET` | Essa reserva ultrapassa o valor objetivo da meta. |
| `INSUFFICIENT_AVAILABLE_FUNDS` | Esta conta não possui valor disponível suficiente para essa reserva. |
| `RELEASE_EXCEEDS_RESERVED` | O valor é maior que o reservado nesta meta. |
| `TARGET_NOT_REACHED` | A meta só pode ser concluída quando o valor reservado alcançar o objetivo. |
| `HAS_RESERVED_FUNDS` | Libere todo o valor reservado antes de arquivar esta meta. |

## Accessibility

- Progresso anunciado em percentual (`progressBarRangeInfo` + “Progresso: 40 por cento”).
- Estados com ícone e texto; datas por extenso para o TalkBack.
- Movimentos lidos como “Reservado, 19 de novembro de 2026”; contas como opções de escolha única.
- Ações destrutivas com ícone e confirmação; erros e alerta de negativo em `liveRegion`; alvos ≥ 48dp.

## Responsiveness

Matriz offline (modo avião, Wi-Fi e dados desligados): 320×640, 360, 390 escuro, 412, 360 com fonte 1,3 e 320 com fonte 1,3.

## Visual review

Feita no emulador (claro, escuro, 320dp com fonte 1,3). Defeitos encontrados e corrigidos antes do commit:
1. **Sheet de reserva em 320dp/1,3:** “Voltar” ficava fora da tela e, com o teclado aberto, o campo de valor sumia. Agora o campo fica no topo, há uma área rolável que encolhe com o teclado e os botões ficam fixos (`screens/07`, `screens/08`).
2. **Rótulo longo:** “Reservado (todas as metas)” quebrava em 3 linhas no sheet e virou “Reservado na conta”.
3. **Alerta no card de conta:** o texto completo ficava pesado no card de 220dp; lá vai a versão curta.
4. **Erro de arquivar/concluir:** aparecia no topo, fora da tela; foi movido para junto das ações.

`GoalVisualSeed` (androidTest) popula dados de revisão só com `-e fluxaSeed 1`; a suíte normal o pula.

## Tests

- JVM: `GoalViewModelsTest` (16), com `FakeGoalDataSource` usando `GoalRules` e `ReserveBalanceCalculator` do domínio. Cobre:
  - lista ativa/concluída/arquivada e detalhe (progresso, reservado, restante, conta, reservas);
  - reservar com sucesso (sem ledger), disponível insuficiente e acima da meta;
  - liberar parcial, total e acima;
  - concluir rejeitado e com sucesso; arquivar com reserva e após liberar;
  - formulário e edição;
  - **Dashboard 5.000/1.500/3.500 sem dupla dedução**, conta excluída verbatim e disponível negativo no Caixa.
- Instrumentados: `GoalFlowTest` (5), cobrindo:
  - criar → reservar (progresso, reservado, disponível; saldo total igual) → Dashboard → liberar;
  - concluir/arquivar com rejeição;
  - erros acima da meta e acima do disponível;
  - disponível negativo após despesa real (reserva preservada, alerta);
  - persistência entre aberturas.
- Force-stop manual (PID novo, início a frio): metas, reservas, movimentos, progresso e Dashboard preservados.

## SafeToSpend preparation

Não implementado. Regra preservada: `ReserveBalanceSnapshot.availableBalance` já é líquido das reservas elegíveis, e a UI o exibe sem nova dedução. O futuro SafeToSpend deve partir desse valor e **nunca** subtrair `totalReserved` de novo.

## Pendências

1. Contas: detalhe completo de reservas por conta (hoje só no card do Caixa e no detalhe da meta).
2. Contribuição automática, transferência real, gasto direto da meta, notificações e SafeToSpend: fora do escopo.

## Base recomendada

`claude/fluxa-android-sprint-6b-frontend`.
