# Sprint 8-A — Backup e restauração

Base `fbf9bc4900a0868f276914703403eb2a3a311199`. Os pontos de composição são `FluxaApplication.backups` para streams e `backupDocuments` para URIs do SAF.

## Formato .fluxa v1

Extensão `.fluxa`, MIME `application/vnd.fluxa.backup`. O arquivo é um ZIP com exatamente duas entradas, `manifest.json` e `data.json`. Entradas duplicadas, nomes adicionais, diretórios, ZIP incompleto, JSON inválido, propriedades duplicadas e UTF-8 inválido são rejeitados. O leitor limita o arquivo e o conteúdo expandido, sem extrair caminhos para o sistema de arquivos.

O manifest contém `formatVersion=1`, `createdAt`, `appVersion`, `databaseSchemaVersion=7`, `currency=BRL`, `encryption=NONE`, `dataSha256` e `recordCounts`. O checksum SHA-256 cobre os bytes UTF-8 exatos de `data.json`. Ele detecta corrupção acidental. Proteção contra adulteração exige futura autenticação criptográfica.

O contrato externo é congelado em `BackupFormatV1`, com DTOs `BackupDataV1`, `BackupRowV1`, `BackupManifestV1` e `PortableSettingsV1`. O mapper de persistência converte as colunas do Room. A versão do formato e a versão do banco são campos separados. A v1 atual aceita schema 7; uma versão futura do app deverá migrar explicitamente os DTOs antigos antes de adaptar o novo banco.

## Dados incluídos

`data.json` contém `tables` e `settings`. Todas as propriedades de cada registro são obrigatórias, com `null` explícito quando permitido.

As doze tabelas transportadas são `local_schema_marker`, `accounts`, `categories`, `credit_cards`, `subscriptions`, `credit_purchases`, `credit_installments`, `financial_transactions`, `subscription_terms`, `subscription_resolutions`, `goals` e `goal_movements`. Isso inclui arquivados, cancelados, snapshots históricos e ocorrências reabertas.

Valores monetários são números inteiros em centavos, validados na faixa de `Long`. Datas financeiras são strings ISO de `LocalDate`, inclusive as colunas que o Room guarda em dias desde epoch. Instantes usam o formato canônico UTC de `Instant`. UUIDs são preservados exatamente.

As preferências portáteis atuais são `theme` e `hideAmounts`. O contrato enumera apenas esses campos. Chaves de dispositivo, credenciais e configurações futuras de biometria deverão ser tratadas separadamente pela próxima sprint de segurança.

## Exportação

`export(output)` devolve `BackupResult.Success(manifest)` ou `Failure(error)`. Captura todos os registros dentro de uma transação Room. Lê as preferências separadamente, pois elas não fazem parte da transação financeira. Serialização, validação, compressão e escrita são executadas em IO depois da captura. O ZIP produzido passa pelo mesmo leitor/validador usado para importação antes de ser enviado ao destino.

O snapshot é em memória. A v1 limita a 100.000 registros, manifest de 64 KiB e payload de 32 MiB. O tamanho do arquivo de entrada também é limitado a 32 MiB mais 64 KiB. A exportação de bases maiores retorna `TOO_LARGE`.

## Validação

`validate(input)` lê, valida e devolve um `PreparedFluxaBackup`. A UI pode exibir o manifest para a confirmação da restauração.

A validação confere versão, moeda, modo de criptografia, checksum, contagens, campos, tipos, IDs, unicidade, datas, enums e referências com índices em memória. Confere também a cadeia de parcelas e baixas, soma das parcelas, categoria das despesas, fonte exclusiva de assinatura, vínculos dos desfechos, reserva não negativa, target e arquivamento de metas.

Essa etapa precede toda substituição. Um arquivo rejeitado conserva o banco atual.

## Restauração REPLACE e backup preventivo

A restauração substitui os dados atuais do Fluxa.

`restore(prepared, emergencyOutput)` exige um destino gravável para o backup preventivo. Primeiro captura o estado atual, gera e valida o arquivo preventivo, escreve, faz flush e fecha esse stream. Falha de escrita ou fechamento interrompe a operação antes da substituição.

Depois inicia uma transação Room, compara novamente o estado atual com o snapshot preventivo e rejeita com `RESTORE_CONFLICT` se houve uma alteração financeira concorrente. A UI deve refazer a preparação do destino preventivo e confirmar novamente a operação nesse caso.

A substituição remove e insere todas as tabelas em ordem de dependência, preservando UUIDs e triggers. A mesma transação executa `foreign_key_check`. Uma falha durante a inserção desfaz também as exclusões, preservando o estado anterior. A operação usa o banco aberto e suas invalidações Room. Não troca o arquivo SQLite nem desabilita foreign keys.

O backup preventivo pode ser usado pelo próprio fluxo de importação para voltar ao estado anterior. A captura e a comparação utilizam todas as linhas, incluindo o histórico. A escrita lenta de ZIP permanece fora da transação.

## DataStore e resultado

Após o commit financeiro, o serviço aplica tema e privacidade juntos em um `DataStore.edit`. O resultado é `RestoreReceipt(settingsApplied=true)`. Se a aplicação das preferências falhar, o resultado financeiro continua sendo sucesso e o recibo indica `settingsApplied=false`. A UI deve informar que os dados foram restaurados e as preferências precisam ser ajustadas.

Room e DataStore conservam seus mecanismos de cancelamento normais. Uma interrupção do processo depois do commit financeiro pode deixar as preferências antigas, enquanto o banco já está completamente restaurado.

## Android automatic backup

O manifesto existente usa `allowBackup=false`, `fullBackupContent` e `dataExtractionRules`. As regras já excluem todos os domínios, incluindo database, file/DataStore, sharedpref e device-protected storage, tanto no cloud backup quanto no device-transfer. A Sprint 8-A preserva essas exclusões. O caminho de backup do usuário passa pelo SAF.

## Integração SAF

A UI deverá usar `ACTION_CREATE_DOCUMENT` para exportar e para escolher o destino preventivo, e `ACTION_OPEN_DOCUMENT` para importar. O adapter `BackupDocuments` trabalha com `ContentResolver` em IO e fecha os streams. O serviço de streams fecha o destino preventivo antes de gravar no banco; os demais streams ficam sob responsabilidade do chamador.

Use destinos distintos para o arquivo importado e para o preventivo. Um sucesso só deve ser mostrado depois que o adapter retornar, incluindo o fechamento do destino. Uma falha de exportação pode deixar um documento de saída incompleto, que o app deve oferecer para excluir ou sobrescrever. A v1 funciona offline com provedores locais e não exige permissões de armazenamento amplo.

```kotlin
val exported = app.backupDocuments.export(destinationUri)
val checked = app.backupDocuments.validate(sourceUri)
if (checked is BackupResult.Success) {
    // Confirmar com o usuário que os dados atuais serão substituídos.
    val restored = app.backupDocuments.restore(checked.value, emergencyUri)
}
```

## Erros

`BackupResult.Failure` contém `INVALID`, `CORRUPTED`, `VERSION_UNSUPPORTED`, `INTEGRITY_FAILED`, `RELATION_INVALID`, `EXPORT_FAILED`, `RESTORE_FAILED`, `RESTORE_CONFLICT` ou `TOO_LARGE`. O erro nunca inclui conteúdo financeiro nem detalhes de SQL no texto destinado à UI. Cancelamento de coroutine é propagado.

## Testes e limites da entrega

Os testes JVM exercitam os codecs, manifest, versão, centavos extremos, UUID, datas ISO, checksum, JSON e ZIP inválidos, duplicidades, FK ausente e limite de expansão.

Os instrumentados exercitam round-trip das doze tabelas com 3.000 lançamentos de volume e centenas de parcelas, IDs, estados históricos, backup preventivo, rollback após exclusão, concorrência durante a escrita, falha de DataStore, invalidação de Flow e reabertura. O mesmo Clock é usado para comparar o SafeToSpend antes e depois.

A proteção por senha fica para a evolução do envelope `encryption` e do formato. O arquivo da v1 contém dados financeiros legíveis. A UI de exportação, confirmação de substituição e apresentação de erros fica para a próxima entrega.
