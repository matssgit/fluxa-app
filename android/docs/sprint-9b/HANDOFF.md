# Sprint 9-B — Segurança local (frontend)

Base: `codex/fluxa-android-sprint-9a-local-security` @ `8c5eb5e`. A UI só consome o contrato da 9-A (`app.security.state`, `requestAuthentication` via host, `changeTimeout`, `changeScreenProtection`). Não foram alterados `LocalSecurityManager`, regras de timeout, regras de capability, o contrato AndroidX Biometric (`BIOMETRIC_STRONG | DEVICE_CREDENTIAL`) nem a política de backup.

Única mudança no pacote `security`: o `AndroidAppAuthenticator` monta o `PromptInfo` por propósito, mudando só os textos. Autenticadores, callbacks e consumo da solicitação continuam iguais.

## Security screen

O caminho é **Mais → Segurança** (rota `security`). O Início não tem controles de segurança. A tela (`SecurityScreen` + `SecuritySettingsViewModel`) segue o padrão de Configurações, Assinaturas e Metas: `FeatureScreen`, cards e `SwitchRow`.

- Introdução: "O Fluxa usa a autenticação do seu aparelho para proteger o acesso ao aplicativo." A tela não promete "100% seguro".
- **Bloquear o Fluxa**: toggle "Exigir autenticação para abrir o app", com "Use a biometria ou o bloqueio de tela do seu aparelho."
- **Bloqueio automático**: lista de opções, sem slider. São elas Imediatamente, Após 1 minuto, Após 5 minutos e Após 15 minutos. Mostra a escolha atual e fica desabilitada, com a nota "Disponível quando…", enquanto o bloqueio estiver desligado. A preferência salva é mantida ao desligar o bloqueio.
- **Proteger conteúdo da tela**: toggle "Bloquear capturas de tela", com "Quando ativado, o Android impede capturas de tela do Fluxa e pode ocultar o conteúdo na tela de aplicativos recentes." O "pode" é proposital, pela variação entre fabricantes.
- Nota final: bloqueio do app, proteção de tela e ocultar valores são controles independentes.

Capability:
- `AVAILABLE`: o toggle funciona normalmente.
- `NOT_CONFIGURED`: toggle desabilitado, com "Configure uma biometria ou bloqueio de tela no Android para proteger o Fluxa." e o botão "Abrir configurações do Android" (`ACTION_SECURITY_SETTINGS`).
- `UNAVAILABLE`: toggle desabilitado, com "Este aparelho não oferece uma forma compatível de autenticação."

A capability é reavaliada a cada retorno à tela (`LifecycleResumeEffect`). Verificado no AVD: sem PIN aparece a orientação; ao configurar o PIN e voltar, o toggle é liberado.

## Root gate

Estrutura: `MainActivity → LocalSecurityGate → (Initializing | Locked | AppContent)`.
- `AppContent` (FluxaApp, navegação, ViewModels e consultas) só compõe quando `canShowContent` é verdadeiro.
- Bloqueado ou inicializando, não existe nó financeiro nem na composição nem na árvore semântica.
- A cobertura nativa da 9-A continua valendo para o intervalo entre a mudança síncrona da política e o commit da composição. Agora ela usa a cor de fundo do tema (creme ou petróleo), em vez de branco fixo.

O conteúdo fica dentro de um `SaveableStateHolder`. Ao bloquear, a composição sai, mas o estado salvável (back stack do NavController e campos `rememberSaveable`) fica guardado em memória e volta no desbloqueio.

## Lock screen

A barreira tem fundo creme ou petróleo conforme o tema, formas orgânicas sutis nos cantos (brandTonal, baixa opacidade) e o nome "Fluxa". Abaixo vêm "Seus dados estão protegidos.", o botão **Desbloquear** e a ajuda "Use sua biometria ou o bloqueio de tela do aparelho." Não há cadeado grande nem blur sobre o Dashboard.

O tema segue a preferência explícita do app (LIGHT/DARK/SYSTEM, lida do DataStore, que não guarda dado financeiro). Até a primeira leitura, usa o tema do sistema. Se o tema mudar com o app desbloqueado, a próxima barreira já usa o novo.

## Initializing

Enquanto a política é lida, aparece só o nome "Fluxa" sobre o fundo do tema. Um indicador discreto de 20dp surge apenas se a leitura passar de 600 ms.

Sequência real observada em 5 cold starts no AVD: splash do sistema → janela vazia → "Fluxa" → prompt. Nenhum quadro com conteúdo financeiro. `INITIALIZATION_FAILED` mostra "Não foi possível ler a proteção local." e **Tentar novamente**.

## Authentication

O prompt é sempre o do Android (AndroidX `BiometricPrompt`); o Fluxa não desenha prompt próprio.
- Desbloqueio: "Desbloquear Fluxa" / "Confirme sua identidade para acessar seus dados financeiros."
- Configuração (ligar/desligar): "Confirmar segurança" / "Confirme sua identidade para alterar a proteção do Fluxa."

Validado com credencial real no AVD exclusivo (PIN configurado só como fixture do emulador; não há PIN no APK): ativar o bloqueio, desbloquear, cancelar e desbloquear pelo botão. A janela de credencial do sistema é protegida: o screencap dela sai vazio.

## Automatic prompt

Ao entrar em `LOCKED`, o gate pede a autenticação uma vez, só com o Activity em `RESUMED`, capability `AVAILABLE`, sem solicitação pendente e sem erro registrado (`AutoUnlockRequest`).

Cancelar ou falhar deixa `error` no manager, então não há reabertura automática nem loop. Rotação reaproveita a solicitação em andamento, que o host reanexa. O prompt de credencial causa STOP do Activity, mas, como a solicitação pendente é um fluxo confiável da 9-A, a volta não bloqueia de novo. Verificado manualmente e pelo teste `autoPromptIsRequestedOnceAndCancelDoesNotLoop`.

`SecurityGateTestHook.autoPrompt` existe só para os testes que conduzem o callback do manager; em produção é sempre `true`.

## Cancel/retry

- **Cancelar:** a barreira continua, com "Toque em Desbloquear quando quiser continuar.", sem tom de erro.
- **Erro real:** "Não foi possível confirmar sua identidade. Tente novamente.", sem códigos Android.
- O botão Desbloquear fica desabilitado enquanto há solicitação em curso, o que evita prompts simultâneos.

## Enable/disable

- **Ligar:** toggle → `launcher.launch(ENABLE_LOCK)` → prompt → no sucesso, o manager persiste `appLockEnabled=true`.
- **Desligar:** toggle → `DISABLE_LOCK` → nova autenticação → desliga.

O toggle sempre mostra o valor persistido, então cancelar mantém o estado anterior e não altera nada. No retorno aparece "Proteção ativada." ou "Proteção desativada." (`FeedbackBanner`), sem modal extra. Erro de autenticação e erro de gravação têm mensagens próprias.

## Auto-lock

A UI não calcula timeout: só reage ao `status` do manager.
- Teste real com elapsedRealtime, `oneMinutePolicyBeforeAndAfterTheLimit` (sob demanda, `-e fluxaLongTimeout 1`): 50 s fora → desbloqueado; 61 s fora → bloqueado.
- A fronteira exata `elapsed >= timeout` (4m59/5m00) está coberta pelos testes JVM determinísticos da 9-A.
- Em foreground contínuo não bloqueia: teste `continuousForegroundUseNeverShowsTheBarrier`.

## Foreground/background

Com "Imediatamente", Home → voltar mostra a barreira antes de qualquer conteúdo e abre o prompt. Verificado manualmente com o PIN real e pelo teste `immediatePolicyLocksOnRealBackgroundAndRotationDoesNot`.

Abrir Recentes só pausa o Activity (API 34): não é background real e não bloqueia. Rotação também não bloqueia; no AVD, `user_rotation` 1→0 recria o Activity sem prompt e na mesma tela.

## Process death

Com o bloqueio ligado, force-stop → abrir: novo PID, `LOCKED` e prompt obrigatório (5 execuções manuais). Sessão desbloqueada, solicitação e leases não são restaurados.

O protocolo seed → force-stop → verify da 9-A continua válido. A navegação depois de process death segue a restauração normal do Android.

## Navigation preservation

- **Detalhe da meta:** "Viagem" → Home → volta → PIN → mesmo detalhe da meta, sem cair no Início (manual).
- **Tela Segurança:** → background → bloqueio → desbloqueio → mesma tela (teste `unlockingReturnsToTheSameRoute`).

## SAF interaction

Com o bloqueio ligado e "Imediatamente":
- "Fazer backup" → seletor real (DocumentsUI) → voltar: o app continua desbloqueado no formulário.
- "Restaurar backup" → seletor → voltar: continua desbloqueado.
- Exportação real até o fim (Salvar) concluída.
- Depois dos fluxos, Home → voltar volta a bloquear e mostra um único prompt, sem loop.

O lease é o `TrustedExternalActivity` da 9-A; a UI não mudou isso.

## FLAG_SECURE

A `MainActivity` observa `settings.protectScreenContent` e aplica ou remove `FLAG_SECURE` na janela na hora, sem reiniciar. Também reaplica na recriação, por rotação. A decisão é global, nunca por tela. O padrão é `false`, conforme o contrato.

## Screenshots

No AVD API 34 (`emulator-5580`):
- **Proteção ligada:** a janela do Fluxa tem `fl=… SECURE …` e `adb exec-out screencap` devolve 0 bytes. `uiAutomation.takeScreenshot` também não captura. Por isso não há captura do estado "proteção ligada": o próprio FLAG_SECURE impede.
- **Proteção desligada:** o screencap volta a 230 KB, com o conteúdo normal, sem reiniciar o app.

## Recent Apps

No AVD API 34:
- **Proteção desligada:** o card mostra o conteúdo do Fluxa normalmente (`screens/10-recentes-protecao-desligada.png`).
- **Proteção ligada:** a captura da própria tela de Recentes sai vazia (0 bytes), porque o sistema bloqueia a captura inteira enquanto o card do Fluxa está visível. Por isso não foi possível documentar a imagem do card.

Isso reflete o AVD/API 34; outros fabricantes podem mostrar o card em branco, um placeholder ou nada. A UI usa "pode ocultar".

## Privacy independence

Ocultar valores (preferência e sessão), App Lock e FLAG_SECURE são três controles separados:
- ligar a proteção de tela não muda `appLockEnabled` nem `hideAmounts` (teste instrumentado e JVM);
- ligar ou desligar o bloqueio não muda o timeout nem a proteção de tela (JVM).

Combinações verificadas: lock desligado com privacidade (padrão do app, coberto nas suítes 6-B/7-B); lock ligado com privacidade desligada; lock ligado com FLAG_SECURE (manual).

## Accessibility

Barreira:
- `paneTitle` "Seus dados estão protegidos." anunciado ao aparecer, com o título marcado como heading;
- botão Desbloquear com pelo menos 48dp de altura e 200dp de largura;
- mensagens de cancelamento e erro como região viva *polite*;
- nenhum conteúdo financeiro na árvore semântica: o teste verifica tags e textos (saldo, conta, meta, "R$") enquanto bloqueado ou inicializando.

Configurações:
- toggles com rótulo e texto de apoio na mesma linha clicável;
- opções de timeout como `selectableGroup` + `Role.RadioButton`, com estado "Inativo" quando desabilitadas;
- orientação de capability como região viva;
- erros em `ErrorBanner`, que é *assertive*.

## Responsive

Capturas em 320×640, 360, 390 escuro, 412, 360 com fonte 1,3 e 320 com fonte 1,3 (`SecurityVisualCapture`, só com `-e fluxaCapture`; seleção em `screens/`). A barreira tem rolagem vertical e respeita os insets.

Defeitos encontrados e corrigidos:
1. O nome mostrado era "Fluxa Personal" (`app_name`), largo demais em 320dp; agora é "Fluxa" (`security_wordmark`).
2. O ícone de calendário no bloqueio automático foi trocado pelo ícone de relógio/reinício.

O fluxo não tem teclado.

## Offline

A suíte instrumentada completa rodou com modo avião, Wi-Fi e dados desligados, PIN no AVD e `-e deviceCredential` (para executar também os casos de credencial real da 9-A). Rodada final:

| Configuração | Total | Aprovados | Pulados | Falhas |
| --- | --- | --- | --- | --- |
| 320×640 | 160 | 149 | 11 | 0 |
| 360 | 160 | 148 → 149 na repetição | 11 | 1 → 0 |
| 390 escuro | 160 | 149 | 11 | 0 |
| 412 | 160 | 149 | 11 | 0 |
| 360, fonte 1,3 | 160 | 149 | 11 | 0 |
| 320, fonte 1,3 | 160 | 149 | 11 | 0 |

Os 11 pulados são rotinas sob demanda: capturas e seeds visuais, protocolos de force-stop, o teste de 1 minuto real e o caso "sem PIN", que só roda num AVD sem bloqueio de tela e foi executado à parte, sem PIN, e passou.

A falha de 360 na rodada final (`InstallmentPaymentFlowTest.generatedExpenseOpensReadOnlyFromCash`, teste da 4-x) é ambiental. O logcat mostra a System UI do emulador cerca de 5,6 s sem quadros (`app_time_stats avg=5641ms`), contra uma espera de 5 s do teste logo ao abrir. Não havia prompt aberto nesse momento. A configuração inteira passou na repetição.

## Validation findings

1. **Defeito de produto corrigido:** `Window.setFlags` reenvia os atributos da janela mesmo sem mudança, o que provoca relayout e reavaliação do teclado. `applyScreenProtection` agora só altera a janela quando o estado de `FLAG_SECURE` desejado difere do atual.
2. **Instabilidade causada por testes novos, corrigida:** a primeira versão dos dois testes com prompt real (`enablingAndDisabling…` e `autoPrompt…`) resolvia a solicitação pelo callback do manager com a janela do sistema ainda aberta e depois só pressionava Back. A System UI do emulador ficava em estado ruim (`AuthController: Evicting client`, `Sync transaction timed out`, quadros de 4 a 15 s), e um teste qualquer, minutos depois, estourava a espera de 5 ou 15 s.
   - **Evidência:** base 9-A, 2 de 2 rodadas limpas; 9-B, cerca de 1 falha por rodada; 9-B sem esses dois testes, 2 de 2 limpas.
   - **Correção:** os testes passaram a seguir o fluxo real do Android: digitar o PIN, cancelar com Back e esperar a janela do sistema sumir. Depois disso, a matriz ficou como na base.
   - **Sonda descartada:** um teste temporário (não versionado) abrindo o app 30 vezes mediu a abertura igual na 9-B e na base (mediana de cerca de 0,6 s). O gate não deixa a inicialização mais lenta.
3. **Ambiente:** a pausa de renderização da System UI no emulador com swiftshader pode, raramente, estourar esperas de 5 s em testes antigos (item anterior).

## Tests

**JVM:** `SecuritySettingsViewModelTest` (17 testes), sobre o `LocalSecurityManager` real com armazenamento em memória; só o prompt é simulado. A suíte toda tem 293 testes. Cobre:
- capability e as três situações;
- ligar (só persiste depois do sucesso), cancelar ao ligar, desligar com nova autenticação, cancelar ao desligar;
- erro de autenticação, capability perdida antes do prompt, falha de gravação;
- timeout só com o bloqueio ativo, preferência mantida ao desligar;
- proteção de tela independente, falha de gravação;
- uma solicitação por vez, erro antigo não exibido, feedback.

**Instrumentados:** `SecurityUiFlowTest` (12), sobre o `MainActivity` e o manager reais. Cobre:
- cold start sem semântica financeira;
- desbloqueio e cancelamento;
- erro amigável;
- STOP real com "Imediatamente" e rotação;
- rota preservada;
- foreground contínuo;
- 1 minuto real (sob demanda);
- tela Segurança com timeout inativo;
- capability sem PIN (pulado com PIN);
- ligar e desligar com o prompt real;
- FLAG_SECURE ligado e desligado, inclusive após recriação;
- auto-prompt único sem loop.

Os 9 testes da 9-A passaram a desligar o auto-prompt, porque conduzem o callback do manager diretamente.

**Manual no AVD com PIN real:** ativação, cold start, cancelar e desbloquear, Home → voltar, detalhe de meta preservado, rotação, 5 force-stops, SAF de backup e restore, screenshot e Recentes com a proteção ligada e desligada, capability sem PIN e com PIN.

## Physical biometric pending

Não havia aparelho físico conectado. Biometria real (impressão digital ou rosto classe 3) e Recentes em fabricantes diferentes ficam para a validação de release. O emulador validou credencial do dispositivo (PIN), que o contrato aceita sozinha.

## Remaining release work

- Medir em hardware físico: biometria, KDF de backup (pendência da 8-B) e Recentes com FLAG_SECURE em 2 ou 3 fabricantes.
- Janela inicial: o tema do Activity (`Theme.Material.Light.NoActionBar`) mostra um quadro claro antes do Compose, também no tema escuro. Não há dado financeiro nesse quadro. Ajustar o tema de janela e o ícone do app (aviso `MissingApplicationIcon`, que já existia) é trabalho de release.
