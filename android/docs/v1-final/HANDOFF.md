# FLUXA PERSONAL v1.0.0 — Final de uso pessoal

Preparação em 24/11/2026. **PRODUCT FEATURE FREEZE — ACTIVE**.
Esta é a APK final da v1.0.0 para uso pessoal no Samsung Galaxy S23. Validações desta entrega feitas em AVD; não constituem teste físico do S23.

## Base

`49fe7b207103eceea126eb7ec39570db00b922c1`, ponta da branch RC2, no worktree isolado `F:/fluxa-app-android-v1-final`, branch `codex/fluxa-android-v1-final`.
Checkout principal `F:/fluxa-app` e suas alterações pendentes preservados. Nenhum push, merge em main ou deploy.

## Integration

Cherry-pick limpo, com `-x`, do patch original do Claude; sem conflitos ou recriação manual.
Commit aplicado: `c5927bd90c168048489baa052c5af2d43c9fa354`.
Patch-id estável idêntico entre origem e aplicação: `ac74bb7d680362b191017e317a6fdb73b054e0f3`.

Histórico preservado:
- Sprint 11-A: `6468182455c36b8fa0b8da263902e6622f75ac7b`, ancestral.
- Sprint 11-B: `27c7c86206a5f8e296074d2e887e408ab634d833`, ancestral.
- RC2 packaging: `49fe7b207103eceea126eb7ec39570db00b922c1`, ancestral.
- RC2.1: patch equivalente de `9f483b23f55f6121b254f39cf8b49f3abf87735e`, aplicado por cherry-pick.

Todo `app/src/main` equivale exatamente ao patch RC2.1 original. Domínio, infraestrutura, Room/schema/migrations, cálculos financeiros, backup, segurança, dependências e navegação inferior sem alterações adicionais. Nenhum polish novo.
Após integração, somente versionCode, preparação/auditoria de artefatos, testes de upgrade e documentação final.

## RC2 commit

`49fe7b207103eceea126eb7ec39570db00b922c1`.

## RC2.1 commit

Original `9f483b23f55f6121b254f39cf8b49f3abf87735e`; aplicado `c5927bd90c168048489baa052c5af2d43c9fa354`.
O SHA original não é ancestral porque foi usado cherry-pick; o patch equivalente foi comprovado.

## Version

Package `com.fluxa.personal`; label Fluxa.
versionName 1.0.0; versionCode **3**, para atualizar a RC2 code 2.
minSdk 30, targetSdk/compileSdk 34 preservados. Não depurável.
Room permanece v7, nenhuma migration nova ou destrutiva.

## APK

`F:/fluxa-app-android-v1-final/android/artifacts/final/Fluxa-1.0.0-FINAL-s23.apk`.
Filename `Fluxa-1.0.0-FINAL-s23.apk`; 23.674.005 bytes; **22,577291 MiB**.
Artefato, relatórios e fixtures locais em `artifacts/final/`, ignorado pelo Git; nenhuma APK/banco/backup/chave privada versionada.

## SHA-256

`6148827d0df56a0bcf1054a99892b73264e2cb6d03cd937c28e84ca32e4530a0`.

O APK instalado foi extraído via `pm path` e apresentou o mesmo SHA-256 da entrega.
Unsigned original preservado: `a7e4d1000abf178deacf1e0f6241ceb90938f3124909f7d388604372041742c3`.

## Signing

Mesmo certificado Android debug/QA já existente, sem chave nova ou assinatura definitiva.
DN `C=US, O=Android, CN=Android Debug`.
Fingerprint SHA-256: `aa912c9e0650533c9ebe5a86c0fb780aa7af03cbc9a39eba5c9090aec1098aa9`.
Comparado diretamente com a APK RC2 real: idêntico. `apksigner 34.0.0 verify --verbose --print-certs` válido, um assinante, esquema v3.

[package-final-qa.ps1](../../scripts/package-final-qa.ps1) assina uma cópia sem modificar o unsigned, exige esse fingerprint e verifica package/versão/release. Keystore não empacotado nem commitado.

## Upgrade validation

AVD exclusivo `fluxa_v1_final`, `emulator-5584`, API 34 Google APIs x86_64. Sem compartilhar com Claude.
Modo avião, Wi-Fi e dados desligados; `Active default network: none`.

Instalada a APK RC2 original `F:/fluxa-app-android-rc2/android/artifacts/rc2/Fluxa-1.0.0-RC2-s23.apk`, SHA-256 `5fbeed0969b9fabe8548d75dc7250f0e53752a6df7afa0dd0abd90873015fbdb`.
Criado dataset representativo por repositórios reais na RC2, configurados DARK, hideAmounts=true, App Lock imediato e proteção de captura. Análises abertas antes de atualizar.

`adb -s emulator-5584 install -r artifacts/final/Fluxa-1.0.0-FINAL-s23.apk`: Success.
**Sem desinstalar, limpar dados ou trocar a APK alvo por debug durante o protocolo.**
Code 2 → 3; firstInstallTime `2026-11-24 19:03:41` preservado; lastUpdateTime `19:04:19`, no relógio do AVD.

[FinalUpgradeTest.kt](../../app/src/androidTest/java/com/fluxa/personal/rc/FinalUpgradeTest.kt) reside exclusivamente no APK de testes separado, assinado com certificado compatível. Não injeta hooks na produção.
Fases explícitas `-e finalPhase seed|upgrade|restore`; sem fase, os testes são pulados. Seed exige code 2 e banco sem contas; verificações exigem code 3.

Reprodução em AVD novo/exclusivo, com PIN descartável configurado e no mesmo dia financeiro:
```powershell
# Defina $adb, $serial, $rc2 e $final.
& $adb -s $serial install $rc2
& $adb -s $serial install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb -s $serial shell am instrument -w -r -e finalPhase seed -e class com.fluxa.personal.rc.FinalUpgradeSeedTest com.fluxa.personal.test/androidx.test.runner.AndroidJUnitRunner
# Abrir e autenticar no app RC2; visitar Mais → Análises.
& $adb -s $serial install -r $final
& $adb -s $serial shell am instrument -w -r -e finalPhase upgrade -e class 'com.fluxa.personal.rc.FinalUpgradeVerifyTest#verifyExactFinalAndOldDataAnalytics' com.fluxa.personal.test/androidx.test.runner.AndroidJUnitRunner
# Após export/restore real pela UI e SAF:
& $adb -s $serial shell am instrument -w -r -e finalPhase restore -e class 'com.fluxa.personal.rc.FinalUpgradeVerifyTest#verifyAfterRealSafRestore' com.fluxa.personal.test/androidx.test.runner.AndroidJUnitRunner
```
Não executar tarefas Gradle de instalação ou suítes que limpam fixtures entre seed e verificação do upgrade.

## Data preservation

Preservados todos os registros de contas/categorias/transações/cartões/compras/parcelas/pagamentos/assinaturas/metas/movimentos de reserva e vínculos.
Comparação de todas as tabelas persistidas (linhas ordenadas canonicamente), schema v7, preferências portáveis e locais de segurança, resultado completo de SafeToSpend e **snapshot completo de Analytics**, idênticos após upgrade.

Hash canônico do conteúdo persistido, igual na RC2, na FINAL e após restore:
`ba911b97873153920cab83d1c30dc92a6f0c8ba8836aac90717040afaa440297`.
Não confundir com hash físico de SQLite/WAL ou da APK.

## Analytics

Sobre dados da RC2: mês com entradas R$ 6.200,00, despesas R$ 650,00, resultado +R$ 5.550,00; quatro movimentações; categoria Mercado 100%; seis meses com outubro R$ 2.000,00/R$ 200,00; disponível R$ 10.950,00; obrigações no horizonte R$ 120,00; projetado R$ 10.830,00; duas assinaturas, equivalente mensal R$ 60,00 e anual R$ 720,00.
Snapshot completo preservado e tela reaberta após upgrade e restore.

RC2.1 integrada: mês sem movimentos não exibe seção de categorias; seletor futuro permanece desabilitado e usa textMuted sem transparência de 45%; projeção tem blocos lado a lado quando cabem, notas menores e espaçamento do patch original.
Mais contém seis destinos funcionais, termina em Segurança, sem Configurações/Em breve ou divisor final sobrando. Capturas locais da APK final foram inspecionadas. Navegação inferior preservada.

## Financial smoke

Jornada pela UI: receita → despesa → cartão/compra → pagamento de parcela → assinatura/pagamento → meta/reserva → Limite seguro → Análises.
Mesmo histórico, sem refresh: saldo R$ 2.850,00, reservado R$ 400,00, disponível R$ 2.450,00; Análises entradas R$ 1.500,00, despesas R$ 650,00, resultado +R$ 850,00. Sem alterar regras ou duplicar realizado.
Resultados instrumentados registrados em Tests.

## Security

Na APK final release: autenticação por PIN nativo, Home/retorno, cancelamento mantendo barreira sem conteúdo financeiro, desbloqueio e desativação autenticada.
FLAG_SECURE presente na janela MainActivity com proteção ON e ausente após OFF. Controle independente da ocultação de valores.
Sem bypass, hook mutável ou logs sensíveis no release. O buffer crash não retornou entradas na verificação final; rede sem conexão padrão. Biometria física e Recent Apps One UI dependem do aparelho.

## Privacy

Ocultação herdada da RC2 conferida no Início, Análises e Limite seguro, incluindo textos/descritivos acessíveis. Gráfico oculto e insights sensíveis suprimidos conforme contrato.
Testes automatizados conferem ausência de dinheiro nas semânticas de Analytics.

## Backup

Export e restore reais pelo SAF nativo da APK final, App Lock imediato habilitado, senha exclusivamente sintética.
Export e preventivo locais: `Fluxa-backup-2026-11-24.fluxa` e `Fluxa-antes-da-restauracao-2026-11-24.fluxa`, 2.173 bytes cada, somente no AVD.
Confirmação, escolha do preventivo, “Backup restaurado” e retorno ao Início concluídos.
Banco canônico, SafeToSpend e snapshot completo de Analytics iguais ao baseline RC2; DARK/hideAmounts preservados. Segurança local não importada do backup.

## Audit

[audit-final-apk.py](../../scripts/audit-final-apk.py): 561 entradas, 16.021 definições reais DEX. Sem bancos, .fluxa, screenshots, docs/handoffs/logs, chaves privadas, dados/fixtures/test assets, hooks, classes de testes ou tooling/previews.
LocalSecurityManager e BiometricPrompt presentes; assets somente baseline.prof/profm.

Permissões: USE_BIOMETRIC, USE_FINGERPRINT e `com.fluxa.personal.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, interna de proteção signature. Sem INTERNET, localização, câmera, microfone, contatos ou armazenamento amplo.
Manifest: allowBackup=false, usesCleartextTraffic=false, não depurável; regras de backup/transferência extraídas da APK excluem todos os domínios.

Native libs: `libdatastore_shared_counter.so` em `arm64-v8a`, `armeabi-v7a`, `x86` e `x86_64`. Não alteradas nem validadas para 16 KB.

## Tests

- JVM completo: **326 aprovados**, zero falhas/erros/pulados.
- Offline: assembleDebug, assembleRelease, assembleDebugAndroidTest aprovados.
- Lint debug e release: **No issues found**.
- Upgrade real: seed RC2 1/1; verify FINAL 1/1; verify após SAF 1/1.
- APK final release em 320×640: 15 casos, **14 aprovados, 1 pulado por protocolo de force-stop, zero falhas**. Inclui jornada financeira até Análises, mudanças RC2.1, Mais, privacidade, abas/sheet e contrato de pacote/ícone.
- Matriz completa debug em **320×640** (960×1920 px, densidade 480, fonte 1,0): **191 casos, 171 aprovados, 20 pulados por protocolos sob demanda, zero falhas**, 550,219 s. Inclui Room, migrations, criptografia, CRUD/Flows, finanças, segurança e UI.
- Primeira seleção debug de 390/sistema escuro: duas falhas de harness identificadas. FoundationTest esperava SYSTEM depois que outro teste persistiu LIGHT; a jornada financeira tentou rolar até analytics-summary durante Loading. A primeira é dependência de ordem/fixture; a segunda foi corrigida **apenas em androidTest**, aguardando a tag do resumo antes de rolar. Nenhuma mudança na APK de produção.
- Revalidação debug **390×844**, sistema escuro (1170×2532 px, densidade 480, fonte 1,0), em lotes isolados: Foundation **3/3**; Analytics com tema efetivamente escuro **9 aprovados/1 pulado**; UI/segurança/backup **30 aprovados/2 pulados**. Total **45 casos, 42 aprovados, 3 pulados, zero falhas**. Asserções preservadas; apenas sincronização e isolamento de fixture corrigidos.
- APK final release em **390/sistema escuro**, instalação de teste limpa: **15 casos, 14 aprovados, 1 pulado, zero falhas** (78,921 s), com o harness final. Confirma navegação, sheet, jornada financeira, Analytics, Mais, privacidade e contrato do pacote.
- Após concluir upgrade, preservação e smokes da release, as suítes debug usam fixtures limpas. Não houve limpeza entre RC2 e FINAL no protocolo de upgrade.
- Avisos Kotlin preexistentes de !! redundante em testes; sem mudança em código para eliminá-los.
- Matriz debug usa fixtures e hooks exclusivos de debug; smokes release, autenticação e SAF reais foram executados separadamente na APK final sem hooks.

Comando offline executado: `gradlew.bat --offline testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lintDebug lintRelease`. Após corrigir a espera exclusiva de androidTest, `assembleDebugAndroidTest` e os cenários afetados foram reexecutados. Não houve alteração nem regeneração da APK final por essa correção.
Os relatórios iniciais com falhas de harness foram mantidos; não são regressões financeiras nem falhas ocultadas na entrega.

Evidências ignoradas em `artifacts/final/`: build.log, APK-METADATA.json, SHA256SUMS.txt, signature-verification.txt, apk-*.txt, apk-content-audit.json, upgrade-*.txt, preservation-evidence.txt, restore-verify.txt, install-before/after.txt, capturas sintéticas e relatórios instrumentados.

## Instalação no S23

Abrir **Fluxa-1.0.0-FINAL-s23.apk** e aceitar **Atualizar**, mantendo a RC2 instalada.
```powershell
adb install -r "F:/fluxa-app-android-v1-final/android/artifacts/final/Fluxa-1.0.0-FINAL-s23.apk"
```
Com vários dispositivos, selecionar explicitamente o S23 via `-s <serial>`.
O upgrade normal preserva os dados; backup manual prévio é opcional por segurança, sem obrigatoriedade para atualizar.
**Não desinstalar RC2 no S23.** Se Android recusar, parar, capturar o erro real e investigar assinatura/package/code. Não desinstalar como contorno.

## Distribution limitations

Esta é a versão final de uso pessoal da v1.0.0; ainda **não é build Play Store**.
Pendências exclusivas de distribuição: target/API atual para publicação, validação de páginas de 16 KB, signing definitivo/estratégia de atualização e AAB.
Não alteradas nesta tarefa e não bloqueiam o uso diário pretendido no S23. Nenhuma compatibilidade 16 KB foi afirmada.

## Feature freeze

**PRODUCT FEATURE FREEZE — ACTIVE**.
Nenhuma feature, regra financeira ou polish adicional implementado. Ideias novas permanecem POST-V1.
