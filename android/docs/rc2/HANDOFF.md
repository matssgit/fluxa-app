# FLUXA PERSONAL 1.0.0 — RC2 para Samsung Galaxy S23

Preparação e validação local em 24/11/2026. **Pronta para teste físico; ainda não testada no S23.**
**PRODUCT FEATURE FREEZE — ACTIVE**

## Base

- Base exata: `27c7c86206a5f8e296074d2e887e408ab634d833`, origem `claude/fluxa-android-sprint-11b-analytics-ui`.
- Descendente confirmado de `8b5edccabdc6613c8bdacbf5ddb5cf6941bc6d77`, entrega RC1; `git merge-base --is-ancestor` retornou 0.
- Branch: `codex/fluxa-android-rc2-physical-device`.
- Worktree: `F:/fluxa-app-android-rc2`. Criado limpo na base exata. Commit de entrega: o commit que contém este documento.
- Checkout principal `F:/fluxa-app` e suas alterações pendentes preservados. Sem push, merge ou deploy.
- Nenhuma diferença em `app/src/main`, `app/src/release`, schemas Room, migrations, domínio, dependências ou SDKs. Única alteração no aplicativo: versionCode 1 → 2. Restante: QA, testes e documentação.

## APK

| Campo | Valor |
| --- | --- |
| Caminho | `F:/fluxa-app-android-rc2/android/artifacts/rc2/Fluxa-1.0.0-RC2-s23.apk` |
| Filename | `Fluxa-1.0.0-RC2-s23.apk` |
| Bytes | 23.674.005 |
| MiB (2²⁰ bytes) | 22,577291 |
| SHA-256 | `5fbeed0969b9fabe8548d75dc7250f0e53752a6df7afa0dd0abd90873015fbdb` |
| Package / label | `com.fluxa.personal` / Fluxa |
| versionName / versionCode | 1.0.0 / 2 |
| minSdk / targetSdk / compileSdk | 30 / 34 / 34 |
| Variante | release, não depurável; R8/shrinking mantidos desativados conforme base |
| Unsigned SHA-256 | `0d3cc53e501c3d3d07b06b4f3d2870274db8ccb70b7ce285934d534d347bb0c0` |

APK, checksums, metadados, evidências e dados sintéticos em `artifacts/rc2/`, ignorado pelo Git. O APK instalado no AVD foi extraído de `pm path` e tem o mesmo SHA-256 da entrega.

## Signing

Mesmo certificado Android debug de QA já existente na RC1, sem criar/copiar chave ou configurar assinatura de produção.
DN: `C=US, O=Android, CN=Android Debug`.
Certificado SHA-256: `aa912c9e0650533c9ebe5a86c0fb780aa7af03cbc9a39eba5c9090aec1098aa9`.

`apksigner 34.0.0 verify --verbose --print-certs`: válido, um assinante, esquema v3. RC1 e RC2 comparadas diretamente, fingerprints idênticos. Compatibilidade também demonstrada pelo upgrade aceito pelo PackageManager. Certificado de QA não é identidade definitiva de distribuição.

O script [package-rc2-qa.ps1](../../scripts/package-rc2-qa.ps1) assina uma cópia, preserva o unsigned, verifica identidade/versão/release e exige o fingerprint da RC1. Não inclui chave na APK nem no Git.

## Version

versionName permanece 1.0.0. versionCode sobe para 2 para identificar a atualização sobre RC1 code 1 e evitar downgrade acidental. Nenhuma migration é necessária: ambas usam Room v7.

## Permissions e conteúdo

Permissões presentes:
- `android.permission.USE_BIOMETRIC`
- `android.permission.USE_FINGERPRINT`
- `com.fluxa.personal.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (declarada com proteção signature).

Sem INTERNET ou nova permissão por Analytics; sem armazenamento amplo/contatos/localização/câmera/microfone.
Manifest final: não depurável, allowBackup=false, usesCleartextTraffic=false. Regras XML extraídas da APK excluem todos os domínios de backup automático e transferência; os nomes reais dos recursos compactados são resolvidos pela tabela de recursos.

[audit-rc2-apk.py](../../scripts/audit-rc2-apk.py) inspecionou 561 entradas e 16.021 definições reais de classes DEX:
- Sem bancos/backups .fluxa/screenshots/handoffs/keystores privados/logcats/test assets.
- Sem fixtures RC2, hooks mutáveis, classes de testes, seeders/capturas, previews ou tooling Compose.
- LocalSecurityManager e BiometricPrompt presentes.
- Nenhum literal conhecido da credencial sintética de QA ou chave privada.
- Assets somente baseline.prof/profm. Busca de logs em main/release sem Log/println/printStackTrace.

Bibliotecas nativas: `libdatastore_shared_counter.so` em arm64-v8a, armeabi-v7a, x86 e x86_64.
**16 KB não testado nem alterado nesta tarefa.**

## Upgrade RC1 → RC2

Emulador exclusivo `fluxa_rc2_upgrade`, serial `emulator-5582`, API 34 Google APIs x86_64, 1080×2400. Sem compartilhar com Claude. Modo avião, Wi-Fi/dados desligados e `Active default network: none`.

APK RC1 original instalada:
`F:/fluxa-app-android-rc1/android/artifacts/rc1/Fluxa-1.0.0-RC1-s23.apk`.
SHA-256: `93c42ada035c567614295d0da94dbe3b367c540b003bdb81465aafe6cb9deb7c`.

Sequência executada:
1. Instalar RC1 release original code 1 e APK de testes separada, ambas com certificado compatível.
2. Criar dados através dos repositórios reais, no processo instrumentado da RC1: duas contas, categorias, receitas/despesas no mês e no anterior, cartão, compra de duas parcelas com uma paga, duas assinaturas (mensal em conta paga e anual em cartão pendente), meta e reserva.
3. Persistir tema DARK, hideAmounts=true, App Lock habilitado, timeout imediato e proteção de captura habilitada.
4. Registrar snapshot canônico de todas as tabelas persistidas, preferências e resultado completo de SafeToSpend.
5. `adb -s emulator-5582 install -r artifacts/rc2/Fluxa-1.0.0-RC2-s23.apk`: Success, **sem desinstalar**.
6. Conferir Room v7, igualdade de todas as linhas, preferências e SafeToSpend; validar Análises sobre os dados antigos.

firstInstallTime permaneceu `2026-11-24 17:45:05`; lastUpdateTime mudou para `17:45:33` no relógio do AVD. Code 1 → 2.
Hash canônico (não é hash do arquivo SQLite/WAL):
`0ffeb73da04207199b1ef14e8e6de549f72a93a8fb62e8806cf6d8dd2e665c76`,
idêntico na RC1, depois do upgrade e depois do restore SAF.

Testes de protocolo em [Rc2UpgradeTest.kt](../../app/src/androidTest/java/com/fluxa/personal/rc/Rc2UpgradeTest.kt). **Somente androidTest**; não empacotados no release. Exigem fase explícita, recusam versão errada e seed em banco com contas existentes. A fase seed não referencia Analytics, inexistente na RC1.

Para repetir em um AVD novo e exclusivo, no mesmo dia financeiro, após compilar a APK de testes:
```powershell
# Defina $adb, $serial, $rc1 e $rc2; use apenas um AVD descartável.
& $adb -s $serial install $rc1
& $adb -s $serial install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb -s $serial shell am instrument -w -r -e rc2Phase seed -e class com.fluxa.personal.rc.Rc2UpgradeSeedTest com.fluxa.personal.test/androidx.test.runner.AndroidJUnitRunner
& $adb -s $serial install -r $rc2
& $adb -s $serial shell am instrument -w -r -e rc2Phase upgrade -e class 'com.fluxa.personal.rc.Rc2UpgradeVerifyTest#verifyExactRc2AndOldDataAnalytics' com.fluxa.personal.test/androidx.test.runner.AndroidJUnitRunner
# Após export/restore real pelo SAF:
& $adb -s $serial shell am instrument -w -r -e rc2Phase restore -e class 'com.fluxa.personal.rc.Rc2UpgradeVerifyTest#verifyAfterRealSafRestore' com.fluxa.personal.test/androidx.test.runner.AndroidJUnitRunner
```
Não usar `connectedDebugAndroidTest` entre seed e verify: tarefas Gradle de instalação podem substituir/remover o alvo. Não executar as suítes que limpam dados antes de concluir o protocolo. Os três corpos passaram (1 teste por fase); o seletor explícito foi acrescentado depois para não executar o protocolo em uma suíte comum.

## Analytics smoke

APK final release aberta por Mais → Análises após upgrade, autenticação e novamente após restore.
Dataset sintético de 24/11/2026:
- Mês: entradas R$ 6.200,00, despesas R$ 650,00, resultado +R$ 5.550,00, quatro movimentações, taxa 89,5%.
- Categoria RC2 Mercado: R$ 650,00 / 100%; insights de maior categoria e resultado positivo.
- Evolução de seis meses: outubro entradas R$ 2.000,00/despesas R$ 200,00; meses sem histórico zerados.
- Disponível R$ 10.950,00 após reserva; obrigações no horizonte R$ 120,00; projetado R$ 10.830,00. Limite do cartão e compras pendentes não são dinheiro.
- Duas assinaturas ativas, equivalente mensal R$ 60,00, custo anual R$ 720,00.
- Mensal/anual são métricas de orçamento, sem dupla contagem do pagamento.
- Navegação entre meses, vazio e reatividade de pagamentos/desfazer/reservas cobertos por AnalyticsFlowTest.

## Financial smoke

`RcFinancialSmokeTest` executado contra a **APK release final**, pela interface: receita → despesa → cartão/compra → pagar parcela → assinatura/pagamento → meta/reserva → Limite seguro → Análises, sem refresh.

Jornada: saldo R$ 2.850,00, reserva R$ 400,00, disponível R$ 2.450,00; Análises entradas R$ 1.500,00, despesas R$ 650,00, resultado +R$ 850,00, disponível R$ 2.450,00 e equivalente mensal R$ 50,00. O teste final foi ampliado apenas em androidTest para conferir essa chegada às Análises.

Quatro destinos inferiores e botão central exercitados pela jornada; sem alterações em componentes ou navegação.

## Privacy

Ocultar valores herdado da RC1: conferido no Início, Limite seguro e em todas as seções das Análises. Valores/descritivos acessíveis exibem Valor oculto, insights financeiros ficam ocultos, gráfico não revela valores/sinais. Percentuais de categoria continuam conforme contrato. Teste automatizado de ausência de dinheiro em textos/semântica das Análises passou na release.

## Security

PIN descartável somente no AVD, sem credenciais no relatório.
- App Lock preservado no upgrade; prompt nativo exigido ao abrir.
- Desativar e reativar pela UI exigiu autenticação.
- Ativar → Home → voltar: novo prompt. Cancelar manteve barreira “Seus dados estão protegidos.”, sem conteúdo financeiro.
- Desbloquear e autenticar: retorno às Análises funcionando.
- FLAG_SECURE: SECURE presente nas flags da janela específica MainActivity com proteção ON; ausente após OFF.
- Sem bypass de testes no release. Sem crashes no buffer crash durante os cenários.
- Biometria física, screenshot/Recent Apps do One UI e TalkBack em português continuam no checklist físico.

## Backup

Exportação criptografada e restore feitos pela UI da APK final e pelo **SAF nativo real**, com App Lock imediato habilitado.
Export `Fluxa-backup-2026-11-24.fluxa` (2.164 bytes) e preventivo `Fluxa-antes-da-restauracao-2026-11-24.fluxa` (2.163 bytes), ambos somente dados sintéticos no AVD. Validação por senha, confirmação, escolha do preventivo, “Backup restaurado” e “Ir para o Início” concluídos.

Depois do restore: mesmo hash canônico do banco, Room v7, SafeToSpend e resultados esperados de Analytics; DARK e valores ocultos preservados; Análises reabertas na UI. Segurança local não foi importada do backup.

## Builds, lint e instrumentação

Ferramentas mantidas: JDK 17, Gradle/AGP/Kotlin/Compose/dependências da base, SDK/build-tools 34.0.0, execução offline.
- `testDebugUnitTest`: **326 testes, zero falhas/erros/pulados**.
- `assembleRelease`: passou.
- `lintRelease`: **No issues found**.
- `assembleDebugAndroidTest`: passou; APK de testes separada.
- Protocolo upgrade: seed RC1 1/1, verify RC2 1/1, verify pós-SAF 1/1.
- Release: contrato de pacote/ícone + jornada financeira + AnalyticsFlowTest: 11 casos, **10 aprovados, 1 pulado por protocolo explícito de force-stop, zero falhas**.
- Jornada final ampliada até Análises: **1/1 aprovado**.
- Matriz completa 11-B não repetida, conforme escopo. Avisos Kotlin de !! redundante preexistentes em testes, sem mudança funcional.

Evidências locais ignoradas: APK-METADATA.json, SHA256SUMS.txt, signature-verification.txt, apk-*.txt, apk-content-audit.json, upgrade-seed-rc1.txt, upgrade-verify-rc2.txt, saf-restore-verify.txt, upgrade-preservation-evidence.txt, flag-secure-smoke.txt, release-financial-analytics-smoke.txt e release-full-journey-final.txt em `artifacts/rc2/`.

## Atualização no S23

Preferir abrir o arquivo e aceitar **Atualizar** sobre a RC1 existente. **Não desinstalar antes.** Conferir SHA-256 e certificado/origem da RC1 instalada.

```powershell
adb install -r "F:/fluxa-app-android-rc2/android/artifacts/rc2/Fluxa-1.0.0-RC2-s23.apk"
```

Se houver vários dispositivos, selecionar explicitamente o S23 com `adb -s <serial> install -r ...`.
Se Android rejeitar assinatura, **PARAR e investigar a origem/certificado**, preservando app e dados. Não desinstalar como contorno.
O sucesso no AVD comprova compatibilidade entre as duas APKs locais acima; não comprova a origem de toda instalação externa.

## Physical checklist

[PHYSICAL_DEVICE_CHECKLIST.md](PHYSICAL_DEVICE_CHECKLIST.md): upgrade preservando dados, Análises reais, fluidez/120 Hz, OLED claro/escuro, fontes, TalkBack em português, privacidade, biometria, FLAG_SECURE One UI, jornada financeira e backup/restore. Nenhum item foi marcado como executado no S23.

## Feature freeze e problemas

Nenhum BUG/SECURITY/COMPATIBILITY/COSMETIC bloqueador identificado nos cenários executados.
Registrar achados físicos como BUG / COMPATIBILITY / SECURITY / COSMETIC, com severidade BLOCKER / HIGH / MEDIUM / LOW / COSMETIC.
Perda de dados, cálculo incorreto, SafeToSpend divergente, backup quebrado, lock bypassável e crash recorrente bloqueiam a RC.

Ideias de novas configurações, notificações, budgets, transferências, sync/cloud/widgets/IA/novas métricas ou gráficos são **POST-V1**. Não implementadas.

## Distribution pending

- COMPATIBILITY: API 36/Play, pendência futura; compileSdk/targetSdk 34 preservados.
- COMPATIBILITY: suporte a páginas de 16 KB das bibliotecas nativas; não validado nesta RC.
- SECURITY: assinatura definitiva de distribuição e sua estratégia de atualização; nenhuma chave de produção criada.
- Validação física S23 pendente; fluidez/120 Hz/OLED/biometria não são comprovados por AVD com renderização em software.

Essas pendências de distribuição não foram tratadas como autorização para alterar a base nesta tarefa.
