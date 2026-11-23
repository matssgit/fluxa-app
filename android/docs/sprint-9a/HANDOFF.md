# Sprint 9-A — Segurança local

Base exata: `3b5b6470268d18a1dc91f472bc60b7ba5d603825`.
Branch: `codex/fluxa-android-sprint-9a-local-security`.
Worktree: `F:/fluxa-app-android-sprint-9a`.

## Security model

Bloqueio local da interface com autenticação delegada ao Android. Não há PIN/senha próprios do Fluxa, credencial armazenada, login, recovery, cloud ou nova criptografia do Room. Dinheiro, datas, entidades, migrations, schemas Room v7, payload de backup v1 e criptografia de backup permanecem fora do escopo das alterações.

`LocalSecurityManager` contém a política sem dependências de Android, Compose ou relógio financeiro. Monitor curto serializa transições de lifecycle/callback; Mutex serializa escritas suspensas de configurações, sem segurar o monitor durante I/O.

## App lock state

`StateFlow<AppLockState>` contém status, configurações, capability, solicitação de autenticação e erro tipado.

- `INITIALIZING`: ainda não foi lida a política; conteúdo financeiro proibido.
- `DISABLED`: política desabilitada; conteúdo permitido.
- `LOCKED`: política habilitada e sem sessão autenticada; conteúdo proibido.
- `UNLOCKED`: política habilitada e sessão autenticada neste processo.
- `INITIALIZATION_FAILED`: leitura falhou; conteúdo proibido, com retry explícito.

A decisão é `canShowContent`; o frontend não deve presumir que ausência de configurações significa lock desabilitado. Erro de leitura não troca a política para false. Timeout persistido desconhecido usa IMMEDIATELY.

## Biometric capability

`AuthenticationCapabilities` é o port testável. `AndroidAuthenticationCapabilities` concentra a consulta a `BiometricManager.canAuthenticate`. Estados: AVAILABLE, NOT_CONFIGURED, UNAVAILABLE. Erros inesperados do adaptador resultam em indisponibilidade, sem liberar conteúdo bloqueado.

Sem bloqueio de tela utilizável, a UI deve orientar o usuário a configurá-lo no Android. Capability é reavaliada no retorno ao foreground, ao solicitar autenticação e antes de concluir uma alteração autenticada.

## Device credential

AndroidX Biometric 1.1.0 com `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`, compatível com minSdk 30. Não há dependência exclusiva de biometria. Credencial do Android sozinha permite autenticação. USE_BIOMETRIC é uma permissão normal; não há permissão ampla de armazenamento.

MainActivity é FragmentActivity, exigida pelo BiometricPrompt estável. Fragment 1.6.2 é explícito: a dependência transitiva 1.2.5 do Biometric rejeitou os request codes do Activity Result/SAF em teste real. Gradle, AGP, Kotlin, Compose, SDKs e as demais versões diretas não foram alterados.

Referências oficiais: [BiometricPrompt e credencial](https://developer.android.com/identity/sign-in/biometric-auth), [Biometric estável](https://developer.android.com/jetpack/androidx/releases/biometric), [Fragment](https://developer.android.com/jetpack/androidx/releases/fragment#1.6.2).

## Session

LOCKED/UNLOCKED, solicitações, IDs, timestamps e leases existem apenas em memória. Nenhum desses valores vai para DataStore, Room, SavedStateHandle ou backup.

`AuthenticationRequest` é consumida uma única vez e tem propósito: UNLOCK, ENABLE_LOCK ou DISABLE_LOCK. Callback antigo, repetido, cancelado ou expirado não autentica uma nova solicitação.

Habilitar exige capability disponível e novo sucesso do sistema antes de persistir true. Desabilitar exige sessão acessível e nova autenticação; uma sessão já desbloqueada não vale como prova para desligar a proteção. Alterar timeout/proteção de tela exige conteúdo acessível e app em foreground.

Cancelamento mantém o gate fechado quando se tenta desbloquear. Rejeição biométrica comum mantém a mesma solicitação e deixa o prompt gerir retries. Erro terminal encerra a solicitação; o usuário pode tentar novamente. Não há abertura automática do Dashboard ou loop de reapresentação de prompt.

## Auto-lock

Defaults: appLockEnabled=false, autoLockTimeout=IMMEDIATELY, protectScreenContent=false. Nada ativa o bloqueio sem autenticação e consentimento.

Políticas de background: IMMEDIATELY, AFTER_1_MINUTE, AFTER_5_MINUTES e AFTER_15_MINUTES. No retorno, `elapsed >= timeout` bloqueia. Imediato bloqueia já no STOP real, exceto fluxo externo explicitamente suspenso. Eventos duplicados não reiniciam o timestamp. Não há timeout de inatividade durante uso contínuo em foreground.

## Monotonic time

`ElapsedRealtimeClock` permite testes determinísticos; o adaptador usa `SystemClock.elapsedRealtime()`, incluindo suspensão do dispositivo. Não usa LocalDate, java.time.Clock ou horário de parede. Uma duração negativa é recusada de forma conservadora.

## Lifecycle e proteção de conteúdo

ProcessLifecycleOwner acompanha o processo. Como seu STOP é atrasado para coalescer recriações, MainActivity também marca STOP real quando `isChangingConfigurations=false`. O manager torna eventos duplicados idempotentes. ON_START avalia a política de forma síncrona antes do desenho. Pausa simples e rotação não contam como background real.

`LocalSecurityGate` nem compõe FluxaApp/navegação/consultas de Dashboard antes da permissão. A leitura inicial começa na Application; não precisa abrir Room ou executar KDF. Uma cobertura nativa opaca protege o intervalo entre mudança síncrona da política e commit da composição. Nesse intervalo, os descendentes de acessibilidade também ficam ocultos. A barreira possui somente apresentação funcional mínima; não é o desenho final da 9-B.

Referência: [ProcessLifecycleOwner](https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner).

## Process death

Novo manager começa INITIALIZING; ao ler enabled=true, começa LOCKED. Nunca restaura sessão, prova de autenticação ou lease. Recriar Activity por configuração no mesmo processo preserva a sessão e reanexa o callback do prompt; não é process death.

Callback de Activity Result de outro processo não cria uma isenção de segurança no processo novo. Senhas de backup continuam seguindo o contrato em memória da 8-C/8-D.

## External activities

`beginExternalActivity()` emite lease somente com conteúdo acessível e foreground. `finishExternalActivity(lease)` consome o retorno/cancelamento; callbacks repetidos não geram nova isenção. `abandonExternalActivity` limpa o lease ao descartar o dono. Nenhum pause genérico cria lease.

Há teto técnico de cinco minutos para SAF/prompt, separado do timeout configurado. Callback perdido não suspende auto-lock permanentemente; ao retornar com lease expirado, exige autenticação. Terminar o fluxo pouco antes de expirar não renova seu prazo. Esse teto conservador é uma decisão de infraestrutura, documentada para a 9-B.

## SAF interaction

SystemBackupPicker marca a suspensão antes de lançar CreateDocument/OpenDocument e a consome no callback, inclusive cancelamento. Falha de launch também libera a suspensão. `TrustedExternalActivity` é ViewModel do fluxo de backup: preserva o lease em memória durante rotação, sem SavedStateHandle. Fluxos de exportação, validação, restore e cleanup da 8-D permanecem os mesmos.

Prompt tem suspensão própria vinculada à solicitação, para não bloquear entre Activity de credencial e callback. A ordem callback → foreground e foreground → callback está coberta nos testes.

## Backup policy

As três preferências de segurança são específicas do aparelho e ficam fora do backup portátil. `LocalPreferences.read/apply` continuam transportando somente theme/hideAmounts. Restaurar tema/privacidade mantém a política de segurança atual do aparelho; não ativa App Lock no aparelho de destino.

allowBackup=false e exclusões de cloud/device transfer existentes cobrem todo o DataStore e permanecem iguais. Nenhum payload/schema novo foi criado. Não há transporte de biometric state, sessão ou timestamps.

## Screen protection setting

protectScreenContent é persistida atomicamente com as demais preferências, global e independente de esconder valores e de appLockEnabled.

FLAG_SECURE ainda não é aplicado nesta sprint. A 9-B deve observá-la globalmente na Activity e validar screenshots e Recent Apps em dispositivos reais. Não decidir por tela.

## UI contract

A UI observa `app.security.state`, incluindo settings/capability/status/authentication/error/isUpdatingSettings. Durante a escrita de uma alteração autenticada, o manager recusa novas solicitações de prompt e leases; a UI pode desabilitar os controles enquanto isUpdatingSettings=true. Usa `canShowContent` para o gate e `needsAuthentication` para a apresentação bloqueada. Não acessa o storage port para habilitar/desabilitar a proteção.

`LocalAuthenticationLauncher.current.launch(purpose)` encaminha UNLOCK, ENABLE_LOCK ou DISABLE_LOCK ao host da Activity. Retorna se a solicitação foi aceita; o resultado efetivo vem no StateFlow. O frontend não instancia BiometricPrompt nem interpreta códigos de hardware.

```kotlin
val launcher = LocalAuthenticationLauncher.current
// Toggle ON: apenas solicitar; não persistir true na UI.
launcher.launch(AuthenticationPurpose.ENABLE_LOCK)
// Toggle OFF: exige nova prova do Android.
launcher.launch(AuthenticationPurpose.DISABLE_LOCK)
// Dentro de sessão acessível, a partir de ViewModel/coroutine:
app.security.changeTimeout(AutoLockTimeout.AFTER_5_MINUTES)
app.security.changeScreenProtection(true)
```

Erros públicos: NOT_CONFIGURED, UNAVAILABLE, CANCELLED, AUTHENTICATION_ERROR, SETTINGS_UNAVAILABLE. A barreira mínima possui retry de leitura/capability; a apresentação final das mensagens pertence à 9-B. A apresentação funcional usa o tema do sistema; a 9-B deve aplicar também a preferência explícita de tema na tela bloqueada, sem antecipar a composição financeira.

## Tests

Validação final em 23/11/2026, somente no AVD exclusivo `fluxa_sprint9a` (API 34, serial emulator-5614, 1080×2336, densidade 443):

- Baseline anterior à implementação: 252 testes JVM aprovados.
- Entrega: 276 testes JVM aprovados, incluindo 24 novos casos determinísticos de política/sessão. Zero falhas, erros ou ignorados.
- Suíte instrumentada completa: 147 casos no XML, 139 aprovados, oito ignorados sob demanda e zero falhas/erros. O console do driver duplica os ignorados e mostra 155; usar o XML.
- Nove casos novos de Activity aprovados, sem ignorados: startup com dados financeiros semeados sem conteúdo sensível na semântica, sucesso/cancelamento de autenticação, STOP/START real, rotação, disabled, SAF real e restauração cifrada preservando a política do aparelho.
- Credencial real do Android: sucesso e cancelamento pelo AndroidX Biometric no AVD com PIN do dispositivo e sem biometria cadastrada. A credencial é fornecida apenas como argumento de fixture ao APK androidTest; não existe PIN no APK de produção.
- Novo processo: protocolo externo seed → force-stop → verify, duas fases aprovadas também offline, com os mesmos APKs finais. Política persistida, sessão descartada, gate obrigatório antes do Dashboard.
- Regressão de backup/restauração 8-C/8-D, cleanup, senha incorreta/adulteração, round-trip, preferências, SafeToSpend, Room/migrations e fluxos existentes: aprovada.
- Builds debug, release (sem assinatura de distribuição) e androidTest aprovados. Lint: zero erros e o único aviso preexistente MissingApplicationIcon.
- Gradle --offline e AVD sem rede: modo avião=1, mobile_data=0, wifi_on=0 e `Active default network: none` confirmados antes e depois dos testes.
- Buffer de crashes vazio.

Os oito ignorados na suíte única são seis capturas/seeds visuais e dois protocolos externos de force-stop. O protocolo novo de segurança foi executado separadamente em seed/verify; o protocolo antigo opcional de assinaturas continua sob demanda.

Comando final, usando variável de fixture somente para o PIN do emulador exclusivo:

```powershell
.\gradlew.bat --offline --no-daemon --continue :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:lint :app:testDebugUnitTest :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.deviceCredential=$env:FLUXA_9A_DEVICE_CREDENTIAL"
```

Resultado: BUILD SUCCESSFUL em 10m 41s. O argumento de fixture é necessário para executar o caso de credencial real; sem ele, esse caso se declara sob demanda.

O Gradle remove os APKs ao final da instrumentação. Para o protocolo externo, instalar novamente os APKs debug/androidTest aprovados e executar:

```text
adb -s emulator-5614 shell am instrument -w -r -e class com.fluxa.personal.security.LocalSecurityProcessDeathTest -e localSecurityPhase seed com.fluxa.personal.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5614 shell am force-stop com.fluxa.personal
adb -s emulator-5614 shell am instrument -w -r -e class com.fluxa.personal.security.LocalSecurityProcessDeathTest -e localSecurityPhase verify com.fluxa.personal.test/androidx.test.runner.AndroidJUnitRunner
```

Cada fase terminou com OK (1 test). A fase seed exercita o callback controlado do manager; o prompt real é coberto pelos testes de Activity. A fase verify encerra a fixture com a política desabilitada.

Problemas resolvidos na validação: Fragment transitivo incompatível com Activity Result/SAF (corrigido pela dependência explícita 1.6.2); primeiro Back no diálogo de credencial pode fechar apenas o teclado (automação distingue esse passo do cancelamento); a SIM do primeiro boot ativou dados móveis após a configuração inicial, detectado por dumpsys e corrigido na fixture com modo avião. Toda a suíte foi repetida sem rede, não apenas a autenticação.

Logs locais ignorados pelo Git: `.gradle/security-validation-offline.log`, `.gradle/security-process-death/seed-offline.log`, `.gradle/security-process-death/verify-offline.log`; relatórios em `app/build/test-results/testDebugUnitTest/`, `app/build/outputs/androidTest-results/connected/debug/` e `app/build/reports/lint-results-debug.html`.

## Remaining frontend work

Sprint 9-B: desenhar tela final de lock e configurações; exibir timeout/capability/erros; usar o host autenticado para toggles; aplicar política global de FLAG_SECURE e validar Recent Apps/screenshots. Validar biometria física em hardware real. Não confundir rotação com novo processo, nem ocultação de valores com App Lock.
