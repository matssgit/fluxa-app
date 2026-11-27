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

**Estado atual**: as versões 1.0.0 a 1.0.2 foram assinadas com um certificado local de QA (`CN=Android Debug`). A estratégia para a distribuição pública está em avaliação e será documentada aqui antes da primeira release pública.

Regras que valem para qualquer chave:
- O arquivo da chave e as senhas **nunca** entram no repositório. `.gitignore` bloqueia `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `keystore.properties` e `signing.properties`.
- A chave precisa de **cópia de segurança** fora do computador de build. Perder a chave impede atualizar o app para quem já o tem instalado.
- O build release no repositório sai sem assinatura. Assinar é um passo separado e manual.

## Se algo der errado

- **APK publicado com defeito**: não apague a release; marque-a como *pre-release* ou edite as notas com o aviso. Publique uma correção com `versionCode` **maior**. O Android não permite voltar para um código menor sem desinstalar.
- **Quem precisa voltar à versão anterior**: só é possível com backup → desinstalar → instalar a anterior → restaurar. O formato de backup v1 é aceito por todas as versões atuais.
- **Migration com defeito**: nunca publique uma migration destrutiva como correção. Corrija com uma migration nova que preserve os dados.

## Notas da release

Modelo em [`docs/releases/`](releases/), um arquivo por versão.
