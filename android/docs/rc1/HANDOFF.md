# Fluxa Personal 1.0.0 — RC1 para teste físico no Galaxy S23

## Base

Base funcional exata: `b7600d42608ef19377a8752e446f82e80d51f7b5`, branch de origem `claude/fluxa-android-sprint-10b-final-polish`.
Status da 10-B: READY FOR PHYSICAL DEVICE RC; [handoff anterior](../sprint-10b/HANDOFF.md).
Branch de entrega: `codex/fluxa-android-rc1-physical-device`.
Worktree: `F:/fluxa-app-android-rc1`. O commit de entrega é o que contém este documento; consultar git log -1.
Checkout principal e alterações pendentes preservados. Sem push, merge, deploy, AAB ou preparação de Play Store.

**Nenhuma mudança em app/src, UI, domínio financeiro, Room v7/schemas/migrations, crypto, LocalSecurityManager, dependências, versão, SDK ou tooling.** As mudanças desta tarefa são documentação, exclusão de artefatos gerados e script de empacotamento local.

## APK

- Arquivo: **Fluxa-1.0.0-RC1-s23.apk**
- Caminho: `F:/fluxa-app-android-rc1/android/artifacts/rc1/Fluxa-1.0.0-RC1-s23.apk`
- Tamanho: **23469205 bytes** (aproximadamente 22.38 MiB).
- A pasta artifacts/rc1 é ignorada pelo Git. A APK precisa ser copiada deste worktree; não acompanha um clone Git.
- Não foi criado build com seeders nem pacote de testes. A APK é a variante release original assinada em cópia.

## SHA-256

```text
93c42ada035c567614295d0da94dbe3b367c540b003bdb81465aafe6cb9deb7c
```

SHA-256 do release unsigned de origem: `5750c53f9c1093e9d8dfd6d0345a395022e9af17ce3eaf245c82a1b05c821918`.
O unsigned não foi alterado ao assinar a cópia.
A base.apk instalada no AVD foi extraída e seu SHA-256 comparado: **idêntico à RC1 exportada**.

```powershell
Get-FileHash -Algorithm SHA256 'F:\fluxa-app-android-rc1\android\artifacts\rc1\Fluxa-1.0.0-RC1-s23.apk'
```

SHA256SUMS.txt e APK-METADATA.json ficam junto da APK.

## Signing

Reutilizado o padrão local da Sprint 10-A: certificado Android debug **já existente** em `%USERPROFILE%/.android/debug.keystore`, alias androiddebugkey, credenciais públicas padrão Android debug.
Nenhuma chave criada; nenhuma assinatura definitiva ou chave de produção; keystore privado não copiado nem incluído na APK/Git.
Certificado: `C=US, O=Android, CN=Android Debug`.
Fingerprint SHA-256 público: `aa912c9e0650533c9ebe5a86c0fb780aa7af03cbc9a39eba5c9090aec1098aa9`.
apksigner 34.0.0 verificou **APK Signature Scheme v3**; v1/v2/v3.1/v4 desativados/não utilizados no artefato gerado. minSdk30 é compatível com v3.
A assinatura de QA permite instalação local; não é identidade de distribuição nem deve ser usada para publicar o aplicativo.

Reprodução, com JDK17 e o SDK já instalados:
```powershell
$env:JAVA_HOME='CAMINHO_DO_JDK17'
$env:ANDROID_HOME='CAMINHO_DO_ANDROID_SDK'
.\gradlew.bat --offline --no-daemon :app:testDebugUnitTest :app:assembleRelease :app:lintRelease
.\scripts\package-rc1-qa.ps1
```
O script exige unsigned e debug.keystore existentes, não cria chave, não muda signingConfig e não atualiza SDK. Caminhos internos de XML são resolvidos pela tabela da APK, pois o AAPT otimiza nomes de recursos.
Gerar novamente exige reconferir hash/certificado/tamanho; não presumir que outro ambiente produza bytes idênticos.

## Package

- ApplicationId: **com.fluxa.personal**.
- Label no artefato: **Fluxa**.
- debuggable: **false**, confirmado por aapt e flags do pacote instalado.
- MainActivity é a entrada MAIN/LAUNCHER.
- Providers/Room service privados. ProfileInstallReceiver AndroidX permanece protegido por android.permission.DUMP.
- Sem deep link ou FileProvider novo. Arquivos continuam via SAF.
- minify/shrinkResources continuam desativados, conforme a base.

## Version

versionName **1.0.0**; versionCode **1**. Preservados para esta RC local.

## SDK

minSdk **30**, compileSdk **34**, targetSdk **34**. Stack inalterada.
Esta RC é para sideload/validação física, não publicação Play. API36/tooling e compatibilidade de runtime16KB são etapas posteriores, conforme a 10-A e a instrução desta tarefa.

## Permissions

Lista completa solicitada pela APK final:
- android.permission.USE_BIOMETRIC.
- android.permission.USE_FINGERPRINT (transitiva do AndroidX).
- com.fluxa.personal.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, declarada com protectionLevel signature para receptores internos AndroidX.

**Ausentes:** INTERNET, permissões de armazenamento amplo, contatos, localização, câmera e microfone.
Não há permissão para instalar outros apps. “Instalar apps desconhecidos” é autorização do gerenciador/navegador usado pelo usuário, não do Fluxa.
usesCleartextTraffic=false.

## Backup e segurança no artefato

allowBackup=false. fullBackupContent e dataExtractionRules referenciam os XML efetivamente incluídos; inspecionados dentro da APK, com exclusões de banco/arquivos/preferences/root/external e domínios device-protected para cloud backup e device transfer.
A exclusão automática não altera o backup manual .fluxa.

Inspeção de **561 entradas ZIP e 15.883 definições de classes DEX**:
- LocalSecurityManager e AndroidX BiometricPrompt presentes.
- SecurityGateTestHook, BackupPickerTestHook, catálogo Preview, seeders/capturas/testes e ui-tooling/preview ausentes.
- Nenhum banco, backup .fluxa, keystore/chave privada, handoff, dump, logcat, screenshot privado ou arquivo de teste encontrado no pacote.
- Assets incluídos são somente baseline.prof/baseline.profm do otimizador, não dados de usuário.
- Sem logging sensível identificado em src/main; código de produção preservado.
- A senha sintética digitada no AVD não aparece na APK. Não foi introduzida constante de senha no app.

Relatórios locais ignorados: apk-content-audit.json, apk-entry-list.txt, apk-badging.txt, apk-permissions.txt, apk-manifest.txt, apk-backup-rules.txt, apk-extraction-rules.txt e signature-verification.txt.

## Native / 16 KB

Há código nativo transitivo do DataStore, não código novo desta tarefa:
- lib/arm64-v8a/libdatastore_shared_counter.so
- lib/armeabi-v7a/libdatastore_shared_counter.so
- lib/x86/libdatastore_shared_counter.so
- lib/x86_64/libdatastore_shared_counter.so

Nenhuma biblioteca alterada, reempacotada manualmente ou atualizada. A listagem não certifica runtime16KB. O smoke foi em API34/x86_64 com páginas4KB; execução16KB continua pendente.

## Build validation

Execução nova nesta base, offline:
- **JVM: 293 aprovados, zero falhas/erros/skips**.
- assembleRelease: aprovado.
- Empacotamento/assinatura RC1 e apksigner verify: aprovados.
- lintRelease: **No issues found**.
- Inspeção de metadata/permissões/conteúdo: aprovada.

Não se repetiu a matriz visual completa da 10-B: nenhum código funcional foi alterado. Resultados anteriores estão no handoff da base; não são apresentados como novos testes desta tarefa.

## Emulator install smoke

AVD exclusivo **fluxa_rc1_s23_qa**, serial emulator-5576, API34/x86_64, 360dp/fonte1.0; sem compartilhamento com Claude. Modo avião, Wi-Fi e dados desligados; Active default network:none.
Instalada **a APK final acima**, não debug.apk, não unsigned e sem instrumentação/seeders.

Aprovados:
1. Instalação limpa; label Fluxa; cold start até Dashboard vazio. am start -W registrou TotalTime1454ms neste AVD (não benchmark do S23).
2. Mais → Segurança → ativar App Lock com prompt Android/PIN real do AVD, sem callbacks simulados.
3. Proteção de tela ON adicionou FLAG_SECURE à janela do Fluxa; OFF removeu a flag.
4. Mais → Backup; exportação pelo DocumentsUI para Downloads criou .fluxa cifrado de775bytes; retorno sem rebloqueio indevido.
5. Arquivo aberto com a senha de QA; confirmação, preventivo via SAF e restore concluídos; retorno ao Início. Dataset vazio/sintético, não teste físico de dados financeiros reais.
6. Force-stop/reopen com PID diferente preservou o lock. Cancelar prompt deixou a barreira; Desbloquear e PIN real retornaram ao Dashboard.
7. APK instalada extraída e hash idêntico à exportada; crash buffer vazio; rede continuou desativada.

PIN/senha usados eram fixtures descartáveis do AVD, não credenciais do S23; não estão no app. Não houve aparelho físico testado.
Logs/relatórios ficam em artifacts/rc1, ignorados pelo Git. O AVD exclusivo foi encerrado após a validação.

## Install instructions

### ADB no computador

Conferir primeiro o SHA-256. Com platform-tools instalado e depuração USB autorizada no **S23 escolhido**, selecionar explicitamente seu serial:
```powershell
adb devices -l
adb -s SERIAL_DO_S23 install "F:\fluxa-app-android-rc1\android\artifacts\rc1\Fluxa-1.0.0-RC1-s23.apk"
```
Não exige Android Studio no telefone. Não usar o serial do AVD como se fosse o S23.

Preferir primeira instalação limpa. Se com.fluxa.personal já existir, parar e verificar a origem/assinatura. Não executar uninstall automaticamente: ele apaga banco, preferências e dados locais. Exportar e conferir um backup antes de eventual remoção deliberada. Não migrar dados de builds antigas não relacionadas.
Uma atualização com assinatura diferente falha; não contornar apagando dados sem decisão do dono.

### Instalação manual no S23

1. Conferir checksum no computador e copiar a APK por USB ou outro meio escolhido para uma pasta conhecida no aparelho.
2. Abrir pelo Samsung My Files/gerenciador de arquivos.
3. Se solicitado, autorizar **Instalar apps desconhecidos** somente para o gerenciador/navegador utilizado.
4. Instalar, abrir Fluxa e executar o checklist. Não é necessário conceder armazenamento amplo ao Fluxa.
5. Se instalação for bloqueada, registrar a mensagem e a configuração do aparelho; não alterar código/SDK nem remover instalação existente por tentativa.
6. Após instalar, a autorização de instalar apps desconhecidos pode ser revogada para o gerenciador, se desejado.

## Physical checklist

[PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md), específico para Galaxy S23/One UI, com campos de ambiente, evidências e classificação de bugs. Todos os itens físicos permanecem por executar.

## Known pending

- Instalação e uso real no S23: biometria digital/fallback, One UI/Recent Apps, screenshot físico, KDF, Files/My Files, teclado, TalkBack em português, fonte ampliada, resolução/densidade e120Hz.
- Runtime Android16/16KB, atualização API36/tooling e assinatura definitiva: posteriores ao RC físico; não realizados nesta tarefa.
- Pendências da 10-B: Análises/Configurações “Em breve”, altura comum no carrossel do Caixa e algumas paradas extras de foco TalkBack. Não são features a implementar nesta RC.
- Certificado debug é apenas QA; não distribuir como release de produção.

## How to report bugs

Usar o modelo e os níveis **BLOCKER / HIGH / MEDIUM / LOW / COSMETIC** do checklist. Incluir APK/SHA-256, Android/One UI, teclado, tema/fonte, estados de lock/privacidade/rede, passos, esperado/observado, frequência e impacto.
BLOCKER inclui perda/corrupção de dados, restore que falha, bypass de lock, crash recorrente ou Limite Seguro incorreto. Não aprovar RC com esses problemas.
Evidências somente com dados de teste; revisar logs antes de compartilhar. Nunca incluir PIN, senha de backup, banco/backup real ou chave privada.
