# Sprint 8-B — Backup criptografado

Base `13c19bb51c58e30f094c40bd00c231b4aa0709e6`. Os contratos de `FluxaApplication.backups` e `backupDocuments` agora exigem senha.

## Modelo de segurança

O arquivo portátil contém um envelope binário autenticado e o ZIP lógico v1 da Sprint 8-A cifrado por completo. O manifest financeiro, incluindo contagens, versão do app e schema, permanece dentro do ciphertext. Senha e chave não são gravadas no arquivo, no Room ou no DataStore.

A senha do backup não é armazenada e não pode ser recuperada pelo Fluxa.

O SHA-256 interno continua validando os bytes de `data.json` depois da autenticação e da abertura do ZIP. A autenticação do arquivo externo é fornecida pelo AES-GCM.

## Container v1

Inteiros multibyte usam big-endian. O cabeçalho tem 52 bytes. Todos os bytes do cabeçalho são passados ao GCM como AAD.

| Offset | Tamanho | Conteúdo |
| --- | --- | --- |
| 0 | 8 | ASCII `FLUXAENC` |
| 8 | 1 | containerVersion = 1 |
| 9 | 1 | cipher = 1, AES-256-GCM |
| 10 | 1 | KDF = 1, PBKDF2-HMAC-SHA256 |
| 11 | 1 | flags = 0 |
| 12 | 4 | iterações |
| 16 | 1 | saltLength = 16 |
| 17 | 1 | nonceLength = 12 |
| 18 | 1 | tagLength = 16 |
| 19 | 1 | reservado = 0 |
| 20 | 4 | tamanho do ciphertext, incluindo tag |
| 24 | 16 | salt aleatório |
| 40 | 12 | nonce aleatório |
| 52 | variável | ciphertext seguido da tag de 16 bytes |

A versão do envelope é independente de `formatVersion=1` e `databaseSchemaVersion=7` dentro do ZIP. O campo legado `encryption=NONE` do manifest descreve o ZIP interno v1, que permanece intacto e é transportado dentro do envelope AES-GCM.

O parser recusa parâmetros desconhecidos e tamanhos inválidos antes da KDF e da alocação do payload. A entrada precisa terminar exatamente após a tag. O limite de 100.000 registros e 32 MiB de JSON é preservado. O ZIP lógico admite 32 MiB mais 64 KiB; o envelope acrescenta 68 bytes.

## KDF e AES-GCM

A exportação usa sempre `PBKDF2WithHmacSHA256`, 600.000 iterações e chave de 256 bits. A importação v1 aceita de 600.000 a 1.200.000 iterações, limitando o custo de arquivos manipulados. Os parâmetros permanecem fixos por exportação, independentemente do aparelho.

Salt de 16 bytes e nonce de 12 bytes são gerados por `SecureRandom` em cada exportação, incluindo o backup preventivo. A tag tem 128 bits. O cabeçalho também é autenticado, inclusive iterações, salt, nonce e comprimento.

A seleção de 600.000 iterações segue a referência atual de PBKDF2-HMAC-SHA256 da OWASP e foi medida no emulador disponível. [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). A implementação usa as APIs JCA nativas. [Android GCMParameterSpec](https://developer.android.com/reference/javax/crypto/spec/GCMParameterSpec).

## Senhas e memória

O mínimo é de oito pontos de código Unicode. Espaços, acentos, símbolos e passphrases longas são aceitos, sem regras de composição adicionais. A representação é UTF-8, sem normalização nem trim. UTF-16 malformado é rejeitado. Um vetor independente gerado por PBKDF2/AES-GCM do .NET usa acentos, espaços e emoji e é aberto tanto no JVM quanto no Android.

Os contratos de serviço e SAF consomem o `CharArray` recebido e o preenchem com zero ao terminar, inclusive em erro ou cancelamento. A UI deve fornecer uma cópia nova a cada operação e limpar o seu buffer original ao encerrar o fluxo. O token preparado contém o snapshot necessário para confirmação, com a senha já descartada.

`PBEKeySpec.clearPassword()`, limpeza da chave codificada e limpeza de buffers de ZIP, JSON e plaintext são usadas quando esses buffers estão sob controle da aplicação. Objetos internos dos provedores JCA, strings e DTOs no heap gerenciado têm ciclo de vida do runtime. Essas medidas reduzem retenção e não constituem garantia de apagamento forense da memória.

A API JCA usa buffers diretos para a mensagem completa, enquanto a leitura e a escrita dos streams usam chunks de 64 KiB. Isso evita a acumulação e as cópias adicionais no heap realizadas pelo caminho incremental do Conscrypt. A decifragem mantém o plaintext em um buffer privado e só o entrega ao codec após a tag ser validada. Buffers de falhas são limpos. O ZIP decifrado entra no codec por referência, evitando outra cópia completa. Não há arquivos temporários plaintext. [Conscrypt, caminho ByteBuffer](https://github.com/google/conscrypt/blob/2.5.2/common/src/main/java/org/conscrypt/OpenSSLAeadCipher.java).

## Exportação e restauração

`export(password, output)` captura o snapshot consistente da 8-A, valida o ZIP interno, cifra e faz flush no destino, em IO. O retorno continua sendo `BackupResult.Success(manifest)` ou `Failure(error)`.

`validate(password, input)` reconhece o envelope, autentica, decifra e executa toda a validação financeira da 8-A. Uma falha de autenticação ocorre antes de qualquer acesso de restauração ao Room ou DataStore.

`restore(prepared, preventivePassword, emergencyOutput)` pede uma senha nova para cifrar o backup preventivo. A UI pode reutilizar a mesma senha fornecendo outra cópia, ou permitir uma senha diferente. O serviço não retém a senha usada para abrir o arquivo importado.

A gravação e o fechamento do preventivo terminam antes da substituição. O controle de alterações concorrentes, o commit Room único e a aplicação secundária de preferências seguem o contrato da 8-A. Falha de DataStore após o commit financeiro retorna sucesso com `settingsApplied=false`.

## Política de backups de desenvolvimento

O fluxo de produção aceita somente o envelope criptografado v1. ZIPs plaintext da Sprint 8-A retornam `CRYPTO_UNSUPPORTED`. O codec lógico continua sendo utilizado internamente e em testes. As exclusões de Android Auto Backup e device transfer foram preservadas.

## Erros para a UI

| Erro | Significado |
| --- | --- |
| `PASSWORD_REQUIRED` | Buffer vazio. |
| `PASSWORD_TOO_SHORT` | Menos de oito pontos de código. |
| `PASSWORD_INVALID_ENCODING` | Entrada Unicode malformada. |
| `INVALID_PASSWORD` | Falha de autenticação GCM. |
| `CRYPTO_UNSUPPORTED` | Envelope, versão, algoritmo ou custo KDF sem suporte. |
| `CORRUPTED` | Cabeçalho, comprimentos, truncamento ou dados excedentes inválidos. |
| `DECRYPTION_FAILED`, `ENCRYPTION_FAILED` | Outra falha do provedor criptográfico. |

Os erros da validação lógica e da restauração continuam disponíveis. Não há diagnóstico capaz de distinguir senha errada de alteração de um ciphertext/tag/salt/nonce autenticado. A mensagem sugerida para `INVALID_PASSWORD` é “Não foi possível abrir o backup. Verifique a senha ou o arquivo.” Testes usam a mesma categoria de erro para esses casos.

## SAF e exemplos

O MIME `application/vnd.fluxa.backup` e a extensão `.fluxa` permanecem. Os adapters recebem URIs e trabalham em IO. A UI futura é responsável pelo seletor SAF, confirmação de substituição e orientação sobre perda da senha.

```kotlin
val exportResult = app.backupDocuments.export(passwordForExport, destinationUri)
// passwordForExport foi consumido e limpo.
val checked = app.backupDocuments.validate(passwordForImport, sourceUri)
if (checked is BackupResult.Success) {
    // Confirmar substituição e obter uma nova cópia da senha para o preventivo.
    val restored = app.backupDocuments.restore(checked.value, passwordForPreventive, emergencyUri)
}
```

## Performance e testes

Os benchmarks registram somente tamanho sintético e duração. Medem KDF separadamente e os tempos totais de cifragem/decifragem do container, que incluem uma KDF cada. Os cenários são 1 KiB e o máximo de ZIP admitido.

A suíte JVM cobre cabeçalho, parâmetros hostis, senha mínima e Unicode, vetor interoperável, aleatoriedade de salt/nonce, autenticação, truncamento e limite de tamanho. Os testes lógicos da 8-A continuam cobrindo JSON, centavos, datas, UUIDs e referências.

Os instrumentados exercitam o round-trip criptografado completo com milhares de lançamentos e centenas de parcelas, backup preventivo criptografado, senha errada, corrupção, preservação do banco e das preferências, rollback, reabertura e igualdade do SafeToSpend.

O primeiro teste de tamanho máximo revelou um pico de heap no caminho incremental do Conscrypt. As chamadas JCA foram ajustadas para buffers diretos. O benchmark passou a usar um arquivo privado contendo apenas ciphertext, removido ao terminar. A validação isolada após a correção passou no limite de 33.619.968 bytes.

Medição de referência no emulador exclusivo API 34 x86_64, com heap de 192 MiB:

| ZIP sintético | KDF | Cifrar, incluindo KDF | Decifrar, incluindo KDF |
| --- | --- | --- | --- |
| 1.024 bytes | 4.964 ms | 3.907 ms | 3.697 ms |
| 33.619.968 bytes | 3.472 ms | 3.376 ms | 3.498 ms |

São medições aproximadas de uma execução, sujeitas a aquecimento do runtime e variação do host. O custo da KDF domina a operação. Esse tempo é aceitável para uma operação ocasional de backup em background, com indicação de trabalho em andamento pela futura UI. A configuração fixa permanece em 600.000 iterações. A medição em aparelho físico fica para a validação de release.

Validação final no emulador exclusivo offline:

- 220 testes JVM aprovados.
- 119 testes instrumentados aprovados e 6 rotinas opcionais ignoradas.
- Round-trip criptografado com 3.000 lançamentos de volume e centenas de parcelas, reabertura e SafeToSpend idêntico.
- Vetor independente .NET aberto no JVM e no Android.
- Tamanho máximo do container validado após a correção de memória.
- Builds debug e release aprovados.
- Lint com zero erros e um aviso preexistente de ícone ausente.
- Fixture sintética presente apenas nos recursos de teste, ausente do APK release.
- Sem mudanças no schema Room, regras financeiras ou exclusões de backup automático.
