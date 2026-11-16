# Sprint 5-D — Fechamento de assinaturas no frontend Android

## Estado recebido do Claude

O Claude Code iniciou a Sprint 5-D e preparou a branch `claude/fluxa-android-sprint-5d-frontend` no worktree `F:/fluxa-app-worktrees/claude-sprint-5d`. O Codex concluiu a sprint excepcionalmente devido à indisponibilidade temporária do Claude Code. O worktree estava limpo, sem diff, na base `3621143ca050ee9227e055827a36ebee9fa09da4`.

## Consulta específica adotada

O detalhe observa somente a assinatura aberta, com `observeOccurrences(subscriptionId, fromInclusive, untilExclusive)`. A janela existente vai da âncora (ou de hoje, quando a âncora é futura) até hoje mais um ano, inclusive. A consulta entrega as ocorrências em ordem decrescente. A tela usa as seções existentes de vencidas, próxima cobrança e histórico e mantém o limite visual do histórico.

## Occurrence detail

`observeOccurrenceDetail` entrega os termos da ocorrência e a resolução. Valor, fonte e identidade vêm dos termos da ocorrência. O nome exibido da fonte é o do cadastro referenciado pelo snapshot, que pode estar arquivado. A data da baixa ou lançamento vem de `occurredOn`. As ações financeiras exigem a resolução estruturada e uma ocorrência pulada fica em leitura.

## Undo conta

`Desfazer pagamento` explica a remoção da saída e confirma o `transactionId` recebido no detalhe. A única escrita é `undoSubscriptionOccurrence`. Caixa, saldo da conta e cobrança reaberta são atualizados pelos Flows existentes.

## Undo cartão

`Desfazer cobrança` confirma o `purchaseId` recebido no detalhe. O mesmo comando remove a compra e a parcela conforme o contrato da Sprint 5-C. O limite disponível e o valor comprometido vêm de `CreditCardSummary`.

## Paid installment

`CARD_CHARGE_ALREADY_PAID` explica a ordem dos estornos e mantém o atalho `Ver parcela`, disponível também no detalhe da cobrança. O usuário estorna a baixa no detalhe da parcela e volta para confirmar o estorno da assinatura.

## Navegação

`Ver no Caixa`, `Ver compra` e `Ver parcela` usam os IDs fornecidos pelo detalhe. Quando a leitura ou o comando reporta uma ocorrência inexistente, a tela volta para o destino anterior.

## Caixa

A origem de assinatura e `Ver assinatura` continuam no lançamento gerenciado, em modo leitura. Se uma saída carregada no detalhe desaparecer, a observação do lançamento faz o detalhe voltar à tela anterior.

## Cartão

As compras de assinatura mantêm o atalho para a origem. O botão genérico de cancelar compra fica restrito às compras que podem usar esse comando. Limites e parcelas refletem as emissões dos repositórios.

## ViewModels

A confirmação guarda o ID da resolução apresentada. O estado local protege imediatamente contra envio duplicado. Se outra resolução substituir a confirmada, o backend recusa o ID antigo e a tela informa o conflito enquanto exibe a resolução emitida pelo Flow. `NOT_RESOLVED` termina sem erro e `SKIPPED` mostra a mensagem de corrida. Após estorno, a agenda consome pendente ou vencida do repositório. A virada do dia usa a solução existente. Assinaturas pausadas e canceladas mantêm o status, e uma nova baixa segue as permissões normais.

## Acessibilidade

O botão usa semântica de ação destrutiva. O banner de erros usa região viva assertiva. O cancelamento da confirmação devolve o foco de teclado à ação quando ela continua disponível. Os botões de confirmação empilhados têm altura mínima de 48 dp.

## Responsividade

A interface mantém tema, cards, topbar de duas linhas, privacidade de valores e componentes existentes. Os atalhos de compra e parcela têm linhas próprias.

## Testes

- JVM: 151 testes aprovados. Os novos casos cobrem consulta por ID, ordem do histórico, confirmação, toque duplicado, estorno de conta, estorno de cartão, parcela paga, conflito e preservação dos estados pausado e cancelado.
- Emulador Android 14: os cinco novos fluxos instrumentados passaram na configuração padrão e em cada uma das seis configurações offline. A primeira execução completa teve um timeout no teste existente de cadastro, edição e arquivamento de conta, que passou isoladamente e na reexecução completa. A reexecução terminou com 82 aprovados, quatro casos sob demanda e zero falhas. O caso sob demanda de persistência passou nas três fases separadas.
- Matriz offline: 30 aprovações, com modo avião ativo, Wi-Fi e dados móveis desligados. Perfis em dp: 320×640, 360×800, 390×844 escuro, 412×915, 360×800 com fonte 1,3 e 320×640 com fonte 1,3.
- Persistência: as fases `settle`, `undo` e `verify` de `SubscriptionForceStopTest` passaram, executadas separadamente com `adb shell am force-stop com.fluxa.personal` entre fases. Cada fase reabre o app. A verificação final consulta saldo, Caixa, compra, limite e ocorrência no Room.
- Builds offline aprovados: `assembleDebug`, `assembleRelease` e `assembleDebugAndroidTest`.
- `lint`: zero erros, com o aviso preexistente `MissingApplicationIcon`.

## Pendências

A verificação de acessibilidade cobriu a implementação de semântica, anúncio de erro, foco de teclado ao dispensar a confirmação e alvos de toque definidos pelos componentes. Leitura manual com TalkBack não foi executada.
