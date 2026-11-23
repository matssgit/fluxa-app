# Sprint 8-D — Hardening de backup

Base `0773061faf009fef950376161bab70edc3c8b27e`. A entrega fecha o cleanup de preventivos incompletos e a identificação de erros públicos do container.

## Preventivo e ordenação

`BackupDestination` representa o documento recém-escolhido e oferece `open()` e `delete()`. É um contrato de I/O sem dependência de Android. `PreventiveBackupOutput` é o único responsável por fechar o stream do preventivo.

A sequência é cifrar → escrever → flush → fechar → marcar `created=true` → iniciar o REPLACE atômico. Falha na abertura, escrita, flush ou fechamento mantém `created=false` e impede que a execução alcance a substituição no Room.

Um preventivo completo permanece disponível quando há falha posterior de persistência ou `RESTORE_CONFLICT`. O adapter SAF deixou de fechar o mesmo stream novamente depois do retorno do serviço.

## Cleanup SAF

A sobrecarga de `FluxaBackupService.restore` que recebe `BackupDestination` coordena a conclusão e o cleanup. `BackupDocuments.restore` cria esse destino usando a URI fornecida pelo seletor.

O cleanup tenta `ContentResolver.delete(uri, null, null)`. Para uma URI de documento reconhecida pela estrutura de `DocumentsContract.getDocumentId`, pode recorrer a `DocumentsContract.deleteDocument`. As chamadas usam somente a URI escolhida. Não há dependência de caminho absoluto.

Uma falha de cleanup conserva o erro original de gravação/restauração. O resultado informa que a exclusão falhou para a UI orientar o usuário. A tentativa é executada uma única vez por operação, inclusive no caminho de cancelamento por coroutine, em IO e contexto não cancelável. A conclusão do preventivo impede qualquer exclusão posterior por essa operação.

A sobrecarga legada de streams continua disponível. Como um stream isolado não oferece exclusão de documento, seu chamador deve cuidar do destino. Se houver falha antes da conclusão, essa sobrecarga informa `cleanup=FAILED`.

## Resultado público

`BackupResult.Failure` conserva `error` e acrescenta:

- `preventiveBackupCreated`, verdadeiro somente se escrita, flush e fechamento terminaram;
- `cleanup`, com valores `NOT_NEEDED`, `DELETED` e `FAILED`.

Em sucesso, `RestoreReceipt.preventiveBackupCreated=true`. O campo `settingsApplied` mantém seu contrato anterior. As etapas de senha, autenticação do arquivo importado, validação financeira, backup preventivo, REPLACE e preferências seguem a mesma sequência.

```kotlin
when (val result = app.backupDocuments.restore(prepared, passwordCopy, preventiveUri)) {
    is BackupResult.Success -> showRestored(result.value.settingsApplied)
    is BackupResult.Failure -> {
        showOriginalError(result.error)
        if (result.cleanup == BackupCleanup.FAILED) showIncompleteDocumentWarning()
        // preventiveBackupCreated=true permite orientar o usuário sobre o preventivo preservado.
    }
}
```

## Granularidade de formato

O reconhecimento é feito antes de derivar a chave.

| Erro | Condição |
| --- | --- |
| `NOT_FLUXA` | Arquivo vazio ou bytes que divergem do magic `FLUXAENC`. |
| `CORRUPTED` | Prefixo reconhecido truncado ou estrutura/comprimentos inválidos. |
| `CONTAINER_VERSION_UNSUPPORTED` | Magic correto e versão externa sem suporte. |
| `CRYPTO_UNSUPPORTED` | Cipher, KDF ou custo de derivação sem suporte. |
| `VERSION_UNSUPPORTED` | Versão lógica/schema do payload interno sem suporte, conforme o contrato anterior. |

Uma versão externa futura pode ser recusada assim que seu byte de versão é lido, sem presumir o tamanho do restante do cabeçalho futuro. Isso permite uma mensagem específica antes da KDF.

## Segurança e UI

`INVALID_PASSWORD` continua cobrindo senha incorreta e adulteração autenticada de salt, nonce, ciphertext ou tag. O protocolo, AES-256-GCM, PBKDF2-HMAC-SHA256, 600.000 iterações de exportação, limites importados, salt, nonce, payload v1 e Room v7 permanecem com os mesmos parâmetros.

O mapeamento existente do ViewModel recebeu mensagens para arquivo não reconhecido e configuração criptográfica sem suporte. A versão externa incompatível usa a mensagem de versão já existente. Quando `cleanup=FAILED`, o card de erro existente informa que os dados foram preservados e que o documento pode estar incompleto. O desenho da tela e a navegação seguem o contrato da Sprint 8-C.

## Testes

Os testes JVM cobrem o marcador de conclusão depois de flush/close, falhas por etapa, cleanup aceito/recusado/com exceção, retenção de arquivo completo e mensagens de UI. Os testes do header foram atualizados e a regressão de senha errada/tag adulterada foi preservada.

Os instrumentados injetam falhas em estado A ao tentar restaurar B, verificando banco e preferências intactos. Cobrem sucesso de A → B e falha posterior no commit, em ambos os casos preservando o preventivo completo de A. Um ContentProvider em memória, acessado por ContentResolver real, falha na abertura e verifica a URI exata enviada ao delete, com aceitação e recusa de exclusão. O seletor visual é dispensado nesse teste.

Validação final em 23/11/2026, com `BUILD SUCCESSFUL` em 6m 4s:

```text
.\gradlew.bat --offline --no-daemon --continue :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:lint :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

- JVM: 252 testes, zero falhas, erros ou ignorados.
- Instrumentados: 137 casos no XML, 130 aprovados, sete opcionais ignorados, zero falhas ou erros. O contador de console do driver duplica os ignorados e mostra 144; o total correto é o do XML.
- Os sete ignorados são capturas/seeds visuais sob demanda e o cenário externo de force-stop, também sob demanda.
- Debug, release (APK sem assinatura de distribuição) e androidTest: aprovados.
- Lint: zero erros e um aviso preexistente `MissingApplicationIcon`.
- Round-trip cifrado com volume, senha incorreta, adulteração, arquivos inválidos, preferências, SafeToSpend, migrations/reabertura e fluxos existentes: aprovados.
- AVD exclusivo `fluxa_sprint8d`, API 34, porta 5612, resolução 1080×2336 e densidade 443. Wi-Fi e dados móveis desativados, ambos confirmados como `0`. Buffer de crashes vazio.

A primeira tentativa foi interrompida por desconexão do ADB. A reconexão por TCP permitiu executar todos os casos, mas deixou o driver sem finalizar a coleta e oito testes de UI falharam por timeout/elementos de listas ausentes. Reiniciar somente o AVD, desativar as três escalas de animação e repetir os oito casos em processos individuais resultou em oito aprovações. A suíte completa final foi executada novamente com os dados dos APKs de teste limpos, pois o teste de defaults pressupõe preferências iniciais; essa rodada terminou normalmente e sem falhas. Não houve alteração nos testes ou regras de cartões, metas, parcelas, SafeToSpend e assinaturas. Os logs e relatórios locais ficam em `.gradle/validation-8d-final-clean.log` e `app/build/outputs/androidTest-results/connected/debug/`, ignorados pelo Git.

## Pendências

A exclusão continua sujeita às permissões e ao suporte do provider. Quando recusada, o resultado e a mensagem permitem identificar a possibilidade de documento incompleto, com o banco preservado.

A medição da KDF em aparelho físico continua pendente. Esta sprint usa o emulador disponível e mantém os parâmetros criptográficos aprovados.
