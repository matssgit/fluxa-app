# Sprint 10-A — Release hardening

## Base

Base exata: `a79fa8bd09b8c400c3cc7a4ee036b50da0918476`, origem `claude/fluxa-android-sprint-9b-local-security-ui`.
Branch: `codex/fluxa-android-sprint-10a-release-hardening`.
Worktree: `F:/fluxa-app-android-sprint-10a`.
O checkout principal e suas alterações preexistentes foram preservados. Nenhum push, merge ou deploy.
O commit de entrega é o commit que contém este documento; consultar `git log -1` nessa branch.

## Audit

A lista priorizada foi registrada antes das mudanças em [AUDIT.md](AUDIT.md). Handoffs 8-D, 9-A e 9-B e contratos atuais revisados.
Defeitos corrigidos: ícone ausente; label antigo; janela clara anterior ao Compose; preferência do olho apenas em memória; hooks mutáveis e catálogo de previews incluídos na release.
Sem novas regras financeiras, telas financeiras ou refatoração ampla. A busca não identificou TODO/FIXME/HACK nem logging sensível de produção. As entradas inativas Análises/Configurações de Mais permanecem com “Em breve”; habilitá-las é pós-v1.

## App identity

Label exibido: **Fluxa**. ApplicationId/namespace técnico `com.fluxa.personal` preservado.
Versão inicial local: **1.0.0**, versionCode **1**, mantido porque não houve distribuição anterior nesta base. Toda publicação futura exige código crescente.

## Icon

Foreground vetorial mínimo derivado das setas ArrowLeftRight do Sidebar existente, com Cream50 sobre Pine900. Não introduz marca nova.
Launcher e roundIcon adaptativos, camada monocromática para ícones temáticos. Sem bitmap legado porque minSdk=30.
Teste renderiza circle, superellipse/squircle e rounded-square e verifica que todos os pixels principais cabem no círculo seguro de 66dp em uma prancheta de 108dp. Render Android incluído em [screens/icon-masks.png](screens/icon-masks.png). O launcher Pixel do AVD exibiu o ícone e o label Fluxa corretamente; o toque abriu MainActivity, sem crash. Evidência: [launcher real](screens/launcher.png). Não substitui revisão em launchers OEM.
Referência: [adaptive icons Android](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

## Splash

AndroidX core-splashscreen 1.0.1 e Theme.Fluxa.Starting. Fundo de marca escuro `#0B1E1A` inclusive se o sistema está claro; depois prevalece o tema salvo.
Splash aguarda somente leitura local de preferências/política. Sem timer ou atraso artificial.
Uma cortina nativa opaca continua protegendo a janela até o gate compor; o conteúdo financeiro só existe quando autorizado. FLAG_SECURE é sincronizado também antes da remoção da cortina.
Falha na leitura de preferências apresenta erro/retry localizado sem compor finanças com defaults de privacidade.

Durante a validação, segurar o splash até um SideEffect de composição deixou ActivityScenario.recreate aguardando idle. A inspeção das threads mostrou runner esperando waitForIdleSync e main ociosa. A implementação final separa leitura local (splash) de autorização/composição (cortina). Os testes focados de recriação e startup passaram após essa correção, sem aumentar timeouts.
Referência: [migração SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate).

## Dark startup

Cinco force-stops seguidos de cold start do APK release não depurável, em sistema LIGHT e preferência DARK, com valores ocultos: TotalTime **1370, 1401, 1290, 1253 e 1421 ms**; mediana **1370 ms**, intervalo **1253–1421 ms** neste AVD.
Capturas de tela gravadas por screenrecord, amostradas a 10fps: nenhum flash claro observado após o primeiro fundo escuro. A amostragem não prova todos os frames do compositor nem desempenho em hardware físico.
Evidências selecionadas: [splash](screens/cold-start-splash.png) e [conteúdo escuro/valores ocultos](screens/cold-start-dark.png).
Cinco aberturas adicionais de Activity com App Lock/DARK verificam fundo nativo escuro e ausência de semântica financeira. Não confundir essas aberturas com as cinco partidas de processo acima.

## Manifest

Manifest final mesclado auditado e contrato conferido no APK debug e no release não depurável:
- Label Fluxa; icon/roundIcon; theme próprio; cleartext=false.
- Sem INTERNET. USE_BIOMETRIC direto; USE_FINGERPRINT transitivo do AndroidX Biometric, preservado. AndroidX também declara DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, restrita à mesma assinatura.
- MainActivity exportada somente como MAIN/LAUNCHER. Sem deep links, launchMode/configChanges/orientação forçados; padrões standard/configuration recreation/unrestricted; adjustResize preservado.
- AndroidX Startup provider privado e Room invalidation service privado.
- ProfileInstallReceiver exportado pelo AndroidX, protegido por android.permission.DUMP; não expõe dados financeiros.
- Sem FileProvider: SAF continua resolvendo leitura/escrita por URI. Não foram acrescentados grants persistentes ou exposição de arquivos.
- allowBackup=false e regras explícitas de exclusão integral para cloud/device transfer, inclusive armazenamento device-protected.

## SDK

minSdk **30**, compileSdk/targetSdk **34**; somente API34 disponível no ambiente. Mantidos para RC técnica local.
**Esta RC não é elegível para publicação atual no Google Play.** Em 23/11/2026, novas versões exigem target API36. A solicitação permite documentar a mudança pós-RC se a atualização de tooling ampliar o risco.
Próxima etapa antes de distribuir: SDK36 + AGP compatível (mínimo oficial 8.9.1), wrapper/JDK/Kotlin/KSP/Compose compatíveis, validação Android16 (edge-to-edge/insets/comportamentos), dependências nativas e suporte 16KB, novo conjunto completo de builds/lint/migrations/offline e testes em API36/aparelho físico. Não basta trocar dois números no Gradle.
Fontes oficiais: [requisitos de target SDK](https://developer.android.com/google/play/requirements/target-sdk), [compatibilidade API/AGP](https://developer.android.com/build/releases/about-agp).

## Dependencies

Inventário preservado: AGP8.5.2, Gradle8.9, Kotlin1.9.24, KSP1.9.24-1.0.20, Compose compiler1.5.14/BOM2024.06.00, Activity1.9.0, Navigation2.7.7, Lifecycle2.7.0, Room2.6.1, DataStore1.1.1, Coroutines1.8.1, Gson2.10.1, Biometric1.1.0, Fragment1.6.2. Java17.
Obrigatória nesta sprint: core-splashscreen1.0.1 para corrigir startup, compatível com a stack atual. ui-tooling-preview ficou debug-only, junto do catálogo.
Fragment1.6.2 continua explicitamente pinado: Biometric1.1.0 selecionaria Fragment1.2.5, incompatível com requestCodes do Activity Result usado pelo SAF. Não retirar o pin sem regressão SAF/autenticação.
Auditoria nativa: quatro ABIs de libdatastore_shared_counter.so, sem compressão, offsets ZIP múltiplos de16384 e LOAD.p_align=16384. O final nominal GNU_RELRO não é múltiplo16KB, mas cobre todo seu LOAD e o próximo LOAD gravável está fora da página protegida. Por exemplo arm64: LOAD gravável0x51c0..0x5428 coberto por RELRO0x51c0..0x6000; próximo LOAD gravável só em0x9428 (além de0x8000, limite arredondado16KB). Um primeiro script que exigia igualdade exata dos finais produziu falso positivo; a inspeção dos segmentos completos corrigiu essa interpretação. **Não foi confirmada incompatibilidade nativa16KB nem se justifica upgrade automático com esse falso positivo.**
O AVD desta sprint tem PAGE_SIZE=4096. A compatibilidade real16KB ainda exige imagem/dispositivo apropriado, inspeção do APK/AAB final com ferramentas recentes e regressão, junto da etapa API36; alinhamento ZIP/ELF sozinho não certifica runtime. Nenhuma biblioteca foi reempacotada ou modificada manualmente. Referência: [critérios oficiais de páginas16KB e RELRO](https://developer.android.com/guide/practices/page-sizes).
Versões disponíveis mais novas permanecem opcionais nesta RC, sujeitas à compatibilidade para a etapa de distribuição; não houve upgrade em massa. Auditoria de releaseRuntimeClasspath não é certificação SCA nem prova de ausência de vulnerabilidades.
Referências: [AndroidX Core](https://developer.android.com/jetpack/androidx/releases/core), [Gson security](https://github.com/google/gson/security).

## Build config

Release explicitamente não depurável, sem minificação e sem shrinkResources: ativar R8 junto de Gson/Room perto da RC exigiria validação adicional. Sem configuração ou chave de assinatura de produção.
Artefato original `app-release-unsigned.apk` preservado. Para validar runtime de release, uma **cópia local de QA** foi assinada com o certificado Android debug já existente e passou nos dois testes de pacote, inclusive reflexão confirmando ausência dos hooks/catálogo. Essa cópia não serve para distribuição.
Builds debug/release/androidTest e lint: ver resultados finais na seção Tests.
.gitignore passou a excluir keystores, signing.properties e backups .fluxa reais. Nenhum APK/build/local.properties/segredo é versionado.
A fixture já rastreada `app/src/test/resources/backup/unicode-v1.fluxa` é um arquivo sintético de compatibilidade criptográfica e foi preservada.

## Logging

Nenhum Log/println de produção com valores, passwords, chaves, UUIDs ou detalhes de autenticação foi identificado. Os tempos de startup são logados somente por androidTest. Sem telemetria/crash analytics.

## Backup/security

Formato .fluxa/crypto/KDF/contratos de segurança não modificados. Regressão completa cobre round-trip criptografado, IDs, preferências portáveis, SafeToSpend, exclusão das preferências de segurança, arquivo preventivo, limpeza e falhas atômicas.
Ocultar valores agora grava DataStore atomicamente, inclusive sob concorrência; o root recebe as preferências reais antes de compor. Continua independente de App Lock/FLAG_SECURE.
Hooks mutáveis SecurityGateTestHook/BackupPickerTestHook somente em src/debug. Políticas de src/release usam autenticação automática e SystemBackupPicker sem overrides. Previews também só debug.
Regras do LocalSecurityManager e do AndroidX Biometric não alteradas. FLAG_SECURE continua idempotente, global e sincronizado com DataStore; rotação/recriação cobertas.
Force-stop real em oito rotas aprovado com processo novo: Dashboard, Caixa, Cartões, Assinaturas, Metas, SafeToSpend, Backup e App Lock. Dados/prefs preservados; novo processo com lock exige nova autenticação, e Back não permite bypass. O protocolo mata intencionalmente a instrumentação seed com a rota aberta; uma segunda instrumentação verifica o novo processo. Nenhuma senha transitória precisa ser recuperada. No protocolo, o callback do manager estabelece a sessão inicial do caso com lock; o auto-prompt debug fica desligado para inspecionar a barreira no processo novo. O PIN nativo real é verificado pelos casos de segurança da matriz, não por esse callback. A navegação reabre cada rota depois do reinício; esse protocolo não promete restauração automática do back stack após force-stop. O caso lock mata uma sessão desbloqueada com App Lock ativo, justamente para confirmar que ela não sobrevive no novo PID.

## Navigation

Quatro destinos e sheet central preservados. Suítes existentes exercitam forms, details, folhas, navegação, entidades arquivadas/removidas e Back em segurança/backup.
Troca de tema pela preferência existente mantém a rota Segurança, inclusive após recriação; não foi necessário refatorar NavController. Não foi criada tela de tema.
A restauração de backup mantém seu reset intencional do shell/VMs para evitar referências ao banco anterior.

## Persistence

Domínio financeiro, repositórios, crypto e schemas históricos intactos. Room permanece **v7**, sem nova migration ou fallback destrutivo.
Cenário crítico coberto pela regressão e pelo protocolo de process death: saldo real R$ 5.000,00 − reserva R$ 1.500,00 = disponível R$ 3.500,00; sem obrigações, seguro R$ 3.500,00. Sem dupla dedução.
Novo RcMigrationChainTest cria banco pelo schema v1, mantém marcador real e valida toda a cadeia até v7, com reabertura Room. Cobertura v6→v7 e migrations individuais preservada.

## Tests

**Validação final aprovada**:
- JVM: **293 testes, zero falhas/erros/skips**.
- assembleDebug, assembleRelease (unsigned), assembleDebugAndroidTest: aprovados offline.
- lintDebug e lintRelease: **No issues found**, zero erros e warnings nos relatórios finais offline. Avisos de versão disponível da auditoria online são informativos e não motivaram upgrade em massa.
- Suíte instrumentada completa: seis perfis abaixo, cada um154 aprovações/12 skips intencionais/zero falhas.
- Migrations individuais e v6→v7, nova cadeia v1→v7 e reabertura: aprovadas na suíte.
- Backup criptografado, IDs/settings/SafeToSpend, exclusão de segurança local, preventivo/cleanup/falhas: aprovados.
- Timeout real1min (50s fora sem lock, depois61s fora com lock): **1/1 aprovado**, sem skip.
- AVD sem PIN: capability indisponível/toggle desativado/orientação: **1/1 aprovado**, sem skip. PIN apenas do AVD restaurado depois.
- Protocolo SubscriptionForceStopTest existente: **settle, undo e verify aprovados**, com force-stop entre fases; liquidação/estorno e vínculos persistidos.
- Release não depurável: contrato de pacote/adaptive icon **2/2 aprovados** com cópia local de QA, certificado debug.
- Auditoria releaseRuntimeClasspath concluída offline, sem dependência não resolvida e sem ui-tooling/preview no runtime release.
Relatórios locais adicionais em `.gradle/post/`; JVM em `app/build/test-results/testDebugUnitTest/`. Arquivos de build, AVD e logs permanecem ignorados pelo Git.

Reprodução a partir de android, com JAVA_HOME/ANDROID_HOME e ANDROID_SERIAL apontando para o AVD exclusivo:
```powershell
.\gradlew.bat --offline --no-daemon testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lint lintRelease
.\gradlew.bat --offline --no-daemon :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.deviceCredential=PIN_DO_AVD_DESCARTAVEL"
```
Para o timeout real, selecionar SecurityUiFlowTest#oneMinutePolicyBeforeAndAfterTheLimit e passar `-Pandroid.testInstrumentationRunnerArguments.fluxaLongTimeout=1`. O caso sem bloqueio de tela requer remover o PIN somente do AVD descartável e selecionar SecurityUiFlowTest#capabilityWithoutScreenLockDisablesTheToggleWithGuidance.
Os 12 skips da matriz: dois protocolos de process death (9-A e RC), protocolo de force-stop de assinaturas, timeout real, caso sem PIN e sete rotinas de captura/seed visual. Os protocolos RC/assinaturas e os dois casos de capability/timeout foram executados separadamente; não se contam skips como aprovações.

Novos casos normais: pacote/manifest, adaptive icon/safe zone, preferência do olho atômica/persistida e rota/tema/recreate, startup DARK com lock (cinco aberturas), cadeia v1→v7.
Regressão focada do prompt nativo: três cenários × três repetições em instrumentações independentes, **9/9 aprovadas** após sincronizar SystemUI.
Novo protocolo RcProcessDeathTest é sob demanda; oito execuções externas aprovadas. Verificação de pacote release aprovada (2 testes).
A primeira execução 320×640 detectou uma corrida em LocalSecurityActivityTest.realDeviceCredentialPromptCanBeCancelledWithoutOpeningContent: a condição “janela diferente do app” aceitava IME/launcher antes de o prompt estar pronto. Logcat: prompt criado 01:19:08 UTC; montagem/troca do teclado até 01:19:10; o Back de 01:19:11 só ocultou IME e o Back do teardown às 01:19:21 finalmente cancelou o prompt. O teste expirou na linha189, sem crash nem exposição de conteúdo. Correção de teste: aguardar package com.android.systemui e acessibilidade ociosa (500ms dentro de limite5s) antes do Back, aplicada também ao helper equivalente da 9-B. Nenhum timeout anterior foi aumentado nem callback de autenticação simulado para esse cancelamento.
Uma seleção focada conjunta passou no cancelamento corrigido, mas falhou antes do auto-prompt: o manager ainda carregava CANCELLED do caso anterior, que por contrato impede reabertura automática. A validação focada passou a executar cada cenário em nova instrumentação/processo, sem alterar a regra de segurança nem limpar artificialmente seu erro por hook.
A mesma tentativa inicial terminou com mais quatro esperas por texto monetário (1 SafeToSpendFlowTest e 3 TransactionsFlowTest): hiddenAmountsMaskTheLimitAndItsDetails havia deixado a preferência agora persistente ativada. A fixture SafeToSpend passou a declarar valores visíveis no Before e limpar a preferência no After. Valores do domínio/banco estavam corretos; não se alterou cálculo nem tempo de espera para fazer o teste passar.
A primeira rodada360 falhou apenas em BackupRestoreFlowTest.cancellingTheConfirmationKeepsCurrentData, aguardando “Abrindo backup protegido…” (5s). O helper enviava clique por coordenada imediatamente após dar foco à senha, durante a abertura do IME; não houve crash. Esse ponto passou a usar o helper de clique semântico já existente, após waitForIdle, para exercitar OnClick sem depender da posição durante resize. Trata-se de uma correção da automação; não se alterou UI, KDF, persistência nem limite de espera. A hipótese de corrida de coordenadas é consistente com o ponto da falha; três repetições focadas passaram após o ajuste, preservando todas as asserções. A matriz final foi reiniciada para executar os seis perfis com os mesmos testes. Não se declara log de frames ociosos como prova de stall.
Capturas/seeds/protocolos de force-stop não são testes ordinários e exigem argumentos específicos.

## Offline

AVD exclusivo `fluxa_sprint10a`, API34 x86_64, serial emulator-5574. Sem compartilhar com Claude. Modo avião, Wi-Fi e dados desligados; Active default network:none antes/depois de cada perfil. Gradle --offline.
Matriz final completa, mesma versão de produto/testes em todos os perfis:

| Configuração | Total | Aprovados | Pulados | Falhas/erros |
| --- | --- | --- | --- | --- |
| 320×640 | 166 | 154 | 12 | 0 |
| 360 | 166 | 154 | 12 | 0 |
| 390, sistema escuro | 166 | 154 | 12 | 0 |
| 412 | 166 | 154 | 12 | 0 |
| 360, fonte1.3 | 166 | 154 | 12 | 0 |
| 320, fonte1.3 | 166 | 154 | 12 | 0 |

**996 casos listados na matriz: 924 aprovações e 72 skips intencionais**, não 996 testes únicos. Os seis buffers de crash ficaram vazios e as seis checagens finais de rede retornaram Active default network:none.
O relatório XML, log Gradle, crash buffer e snapshot de rede de cada perfil estão localmente em `.gradle/matrix/<perfil>/` (artefatos ignorados pelo Git). As tentativas iniciais320 e360 foram preservadas separadamente; não foram substituídas por uma afirmação de “primeira execução limpa”. A rodada320 aprovada anterior ao último ajuste de teste também foi preservada.

Densidade de 480 dpi (3 px/dp), respectivamente: 960×1920, 1080×2400, 1170×2532, 1236×2676, 1080×2400 e 960×1920 pixels. Fonte 1,0 nos quatro primeiros e 1,3 nos dois últimos; sistema NIGHT=yes apenas em 390.

O perfil 390 usa o sistema escuro; algumas fixtures preexistentes selecionam explicitamente LIGHT para testar aquele contrato. A cobertura de tema DARK persistido está nos testes/capturas específicos, não se afirma que toda fixture foi executada com conteúdo escuro. Para completar esse ponto, o protocolo das oito rotas foi repetido em **390dp, sistema NIGHT=yes, preferência DARK persistida e valores ocultos**: oito verificações aprovadas, PID novo em todas, sem crash e sem rede. Relatórios separados em `.gradle/process-rc-390-dark/`.

## RC checklist

- [x] Escopo de hardening, sem novas features/regras financeiras.
- [x] Identidade, adaptive icon, safe zone e label Fluxa.
- [x] Startup escuro e autorização antes de finanças; privacidade persistente.
- [x] Manifest sem rede, sem backup automático e sem componente sensível indevidamente exportado.
- [x] Release sem hooks mutáveis/previews e sem chave de produção.
- [x] Process death real em oito rotas e teste do APK release.
- [x] Regressão final JVM/build/lint de debug/release e seis perfis offline, sem falhas.
- [x] Timeout real, capability sem PIN e protocolo de assinaturas, executados separadamente.
- [ ] Revisão visual final do Claude e testes físicos abaixo.
- [ ] Atualização API36/tooling e nova validação antes de distribuição Play.
A RC é local; aprovação em AVD não substitui os itens pendentes de distribuição.

## Physical device checklist

Não havia aparelho físico disponível; nenhum resultado físico foi presumido.
- [ ] Biometria física (digital/rosto classe3), cancelamento/lockout e fallback credencial.
- [ ] Recent Apps e screenshot com FLAG_SECURE em aparelhos/fabricantes diferentes.
- [ ] Performance real do KDF e memória em exportação/restauração.
- [ ] Instalação/abertura do APK no aparelho e partida de processo.
- [ ] Backup/restore por Files real: permissões URI, preventivo, cleanup e interrupções.
- [ ] Lifecycle, rotação, fonte1.3 e comportamento OEM.

## Remaining visual work

Claude: revisão final do launcher em máscaras/ícones temáticos OEM, transição da splash escura para LIGHT e contraste/tipografia/rolagem em 320dp/fonte1.3. As capturas anexadas usam somente dados sintéticos.
A captura de Dashboard preserva a barra inferior translúcida já existente sobre o conteúdo rolável; revisar legibilidade/insets no polish, sem mudar regras ou ampliar escopo.
Mais → Análises/Configurações permanece pós-v1. Não liberar entradas apenas para parecer completo.
