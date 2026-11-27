# Processo de release

Como uma versão do Fluxa sai do código e chega ao GitHub Releases. Nenhuma senha, chave ou credencial faz parte deste repositório.

## Versionamento

[Versionamento Semântico](https://semver.org/lang/pt-BR/), em `android/app/build.gradle.kts`:

| Campo | Regra |
| --- | --- |
| `versionName` | `MAJOR.MINOR.PATCH`. PATCH para correções e ajustes visuais, MINOR para funcionalidades, MAJOR para mudanças que exijam ação de quem usa |
| `versionCode` | Inteiro que **sempre aumenta** em 1 a cada APK distribuído. O Android recusa atualizar para um código menor ou igual |

Histórico: 1.0.0 = 3, 1.0.1 = 4, 1.0.2 = 5.

## Checklist

1. **Código**
   - [ ] `versionName` e `versionCode` atualizados.
   - [ ] [CHANGELOG.md](../CHANGELOG.md) com a nova versão.
   - [ ] Se o banco mudou: migration, schema exportado e teste de migration.
2. **Verificação**
   - [ ] `./gradlew testDebugUnitTest lintDebug lintRelease assembleRelease assembleDebugAndroidTest`
   - [ ] Suíte instrumentada completa, offline, nos perfis de [TESTING.md](TESTING.md).
   - [ ] **Teste de atualização** a partir do APK público anterior, sem desinstalar, comparando os dados.
   - [ ] Backup feito na versão anterior e restaurado na nova.
3. **Empacotamento**
   - [ ] Assinar uma **cópia** de `app-release-unsigned.apk` com a chave de distribuição (veja abaixo).
   - [ ] `apksigner verify --print-certs`: conferir que o certificado é o esperado.
   - [ ] `aapt dump badging`: conferir pacote, versão e que o app não é depurável.
   - [ ] Auditoria do APK (`android/scripts/audit-*.py`): sem bancos, backups, chaves, classes de teste ou segredos.
   - [ ] Calcular o SHA-256 do APK.
4. **Aparelho físico**
   - [ ] Atualizar por cima da versão anterior no aparelho de referência e conferir a abertura, o bloqueio, um lançamento e o backup.
5. **Publicação**
   - [ ] Tag `vX.Y.Z` no commit da versão.
   - [ ] Release no GitHub com o APK como *asset*, o SHA-256 e as notas da versão.
   - [ ] Conferir o download e o checksum a partir da página publicada.

O APK **nunca** é versionado no Git. Ele existe só como *asset* da release.

## Nome do arquivo

`Fluxa-X.Y.Z.apk`, com um arquivo `Fluxa-X.Y.Z.apk.sha256` ao lado, contendo a soma no formato do `sha256sum`.

## Assinatura

O Android só atualiza um app se o APK novo for assinado pela **mesma chave**, ou por uma chave que a anterior autorizou por **rotação de chave** (APK Signature Scheme v3). Trocar de chave sem rotação obriga a desinstalar, e desinstalar apaga os dados.

### Chave de distribuição

As versões 1.0.0 a 1.0.2 foram assinadas com uma chave RSA 2048 criada no ambiente de desenvolvimento (certificado `CN=Android Debug`, SHA-256 `aa912c9e…c1098aa9`, válido até 2056). Quem já tem o Fluxa instalado tem essa chave registrada no aparelho.

**Estratégia adotada:** essa mesma chave passa a ser a chave de distribuição, **isolada** num keystore exclusivo do Fluxa, com senha forte e cópias de segurança offline. O script [`scripts/signing/isolate-distribution-key.ps1`](../android/scripts/signing/isolate-distribution-key.ps1) faz a cópia. O keystore padrão de debug do SDK é aposentado, e o SDK gera outro só para desenvolvimento.

Por que não uma chave nova com rotação:
- O Android só aceita uma chave nova sem desinstalar o app por meio de **rotação** (APK Signature Scheme v3, a partir do Android 9).
- A documentação oficial diz que a rotação **não é recomendada no Android 12 (API 31) e anteriores**. Ela lista problemas de plataforma corrigidos só no Android 11, 12 e 13. O Fluxa suporta a partir do Android 11 (API 30).
- O `apksigner` (v3.1, a partir do Android 13) aplica a chave rotacionada só a partir da API 33 por padrão. Aparelhos com Android 11 e 12 continuam verificando a **chave original**, que precisaria assinar todas as releases do mesmo jeito.
- Na prática, uma chave nova hoje não aposentaria a original. Ela só adicionaria uma segunda chave para proteger.

A rotação pode ser reavaliada quando o `minSdk` chegar à API 33. Fontes: [apksigner](https://developer.android.com/tools/apksigner), [APK Signature Scheme v3](https://source.android.com/docs/security/features/apksigning/v3) e [v3.1](https://source.android.com/docs/security/features/apksigning/v3-1).

### Guarda da chave

- Keystore PKCS#12 fora do repositório e fora da pasta `.android` do SDK.
- Senha de pelo menos 16 caracteres, gerada e guardada num gerenciador de senhas.
- **Duas cópias offline** do keystore, em lugares físicos diferentes, e um teste de restauração depois de copiar.
- **Recuperação**, por exemplo se a máquina for perdida: instalar JDK 17 e Android SDK, restaurar o keystore a partir de uma cópia, definir `FLUXA_KS` e `FLUXA_KS_PASS` e rodar `scripts/sign-release.ps1`.
- Se o keystore **e** todas as cópias se perderem, não há como atualizar o app instalado. A saída é o plano de contingência abaixo.

### Assinar uma release

```powershell
cd android
./gradlew :app:assembleRelease
$env:FLUXA_KS = "<caminho do keystore de distribuição>"
$env:FLUXA_KS_PASS = "<lido do gerenciador de senhas, nunca digitado em arquivo>"
.\scripts\sign-release.ps1 -Version 1.0.2 -VersionCode 5
Remove-Item Env:FLUXA_KS_PASS
```

O script assina uma cópia do APK, confere o certificado esperado, o pacote, a versão e o estado não depurável, roda a auditoria de conteúdo e gera `Fluxa-<versão>.apk` e `Fluxa-<versão>.apk.sha256` em `android/artifacts/release/`, pasta ignorada pelo Git. A senha nunca aparece na linha de comando nem em relatórios.

### Regras

- O arquivo da chave e as senhas **nunca** entram no repositório. `.gitignore` bloqueia `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `keystore.properties` e `signing.properties`.
- O build release no repositório sai sem assinatura. Assinar é um passo separado e manual.
- Nenhum APK de debug deve ser instalado em aparelhos de usuários.

### Plano de contingência: troca de chave sem rotação

Só se a chave atual se perder ou for comprometida. O Android recusa a atualização e é preciso reinstalar:

1. Na versão instalada: **Mais → Backup e restauração → Fazer backup**, com senha. Guarde o arquivo fora do celular.
2. Confira que o arquivo `.fluxa` existe e anote a senha.
3. Desinstale o Fluxa. **Isso apaga os dados do aparelho.**
4. Instale a versão nova, assinada com a chave nova.
5. **Mais → Backup e restauração → Restaurar backup**, com o arquivo e a senha.
6. Reative o bloqueio do app e a proteção de tela em **Mais → Segurança**. Essas configurações não vão no backup.

As notas dessa release precisariam avisar isso em destaque, antes do link de download.

## Se algo der errado

- **APK publicado com defeito**: não apague a release; marque-a como *pre-release* ou edite as notas com o aviso. Publique uma correção com `versionCode` **maior**. O Android não permite voltar para um código menor sem desinstalar.
- **Quem precisa voltar à versão anterior**: só é possível com backup → desinstalar → instalar a anterior → restaurar. O formato de backup v1 é aceito por todas as versões atuais.
- **Migration com defeito**: nunca publique uma migration destrutiva como correção. Corrija com uma migration nova que preserve os dados.

## Notas da release

Modelo em [`docs/releases/`](releases/), um arquivo por versão.
