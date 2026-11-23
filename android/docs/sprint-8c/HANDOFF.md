# Sprint 8-C — Backup e restauração (frontend)

Base: `codex/fluxa-android-sprint-8b-backup-encryption` @ `44d528f`. Não houve alteração no container, AES-GCM, PBKDF2 (600.000 iterações), manifest, formato `.fluxa`, serviço de restauração nem Room. A UI consome só `FluxaApplication.backupDocuments` (contratos SAF da 8-B) e a regra de senha do backend (`BackupCrypto.validatePassword`).

## Tela

O caminho é **Mais → Backup e restauração** (rota `backup`). O Início não tem nenhuma ação de backup ou restauração.

A tela inicial (`BackupScreen`, estado `Home`) mostra:
- o card **Protegido por senha**: "Seus backups são protegidos por senha e podem ser restaurados sem internet." e "O Fluxa não armazena a senha do backup.";
- duas ações num card no estilo de Configurações, Assinaturas e Metas:
  - **Fazer backup**: "Salve uma cópia protegida dos seus dados financeiros.";
  - **Restaurar backup**: "Substitua os dados atuais usando um arquivo de backup do Fluxa.";
- a pílula "Arquivo .fluxa" em cada ação;
- a nota de que não há backup automático.

O texto não usa linguagem de "cibersegurança" nem promete "100% seguro".

## Export

O fluxo de exportação passa por quatro passos:
1. Formulário com **Senha** e **Confirmar senha**, mais o aviso crítico sempre visível (sem tooltip): "Guarde essa senha com cuidado. O Fluxa não armazena nem consegue recuperar a senha do backup. Sem ela, o arquivo não poderá ser restaurado."
2. **Escolher onde salvar** valida a senha. A igualdade entre os campos é checada na UI. O mínimo vem da regra do backend (`backupPasswordError` → `BackupCrypto.validatePassword`): 8 pontos de código Unicode, sem exigir maiúscula, número ou símbolo.
3. O seletor do Android (`ACTION_CREATE_DOCUMENT`) abre com o nome sugerido `Fluxa-backup-AAAA-MM-DD.fluxa`.
4. O app mostra **Protegendo seu backup…** (indeterminado, sem porcentagem). No sucesso, mostra "Backup criado com sucesso.", o nome do arquivo informado pelo provedor e a data/hora de `manifest.createdAt`. A senha nunca é exibida de novo.

Erros da exportação:
- **Cancelar o seletor** não é erro: volta ao formulário com os campos preenchidos.
- **Falha** mostra "Não foi possível criar o backup. Tente novamente.", limpa os campos e tenta excluir o documento recém-criado e incompleto (`DocumentsContract.deleteDocument`).
- **`TOO_LARGE`** tem mensagem própria.

## Password UX

O campo de senha (`PasswordField`) tem:
- ícone de cadeado;
- botão mostrar/ocultar com descrição "Mostrar senha" / "Ocultar senha" (alvo de 48dp);
- `KeyboardType.Password`, sem autocorreção nem sugestões.

O autofill não é pedido: nenhum `contentType` é declarado, e o Compose 1.6 não participa do autofill sem ele.

O texto digitado fica num `remember` comum, nunca num `rememberSaveable`. Os campos são limpos quando o fluxo muda de tentativa: sucesso, erro definitivo, cancelamento ou saída da tela.

Enviar (botão ou ação do teclado) fecha o teclado. Assim o formulário inteiro e a mensagem de erro ficam visíveis até em 320dp com fonte 1,3. O bloco do erro é trazido para a área visível (`BringIntoViewRequester`) e anunciado como região viva.

## SAF

O seletor fica em `BackupPicker`; a implementação de produção é `SystemBackupPicker`, sobre a Activity Result API:
- `CreateDocument(application/vnd.fluxa.backup)` para exportar e para o backup preventivo;
- `OpenDocument` com os tipos `application/vnd.fluxa.backup` e `application/octet-stream` para importar.

Não há permissão de armazenamento amplo nem `MANAGE_EXTERNAL_STORAGE`, e nenhum caminho absoluto: só a URI devolvida pelo seletor, convertida em `DocumentRef`, um valor opaco para o ViewModel.

**MIME e extensão.** Validado no emulador com o DocumentsUI real:
- a criação com o MIME próprio preserva o nome `Fluxa-backup-2026-11-19.fluxa`, cujo conteúdo começa com `FLUXAENC`;
- na abertura, o provedor local classifica a extensão desconhecida como `application/octet-stream`. Por isso esse tipo está no filtro, como fallback compatível; só com o MIME próprio o arquivo não apareceria.

Cloud: não há integração própria. Provedores instalados aparecem naturalmente no seletor do Android.

## Restore

O fluxo de restauração tem cinco passos:
1. **Restaurar backup** abre o seletor (`ACTION_OPEN_DOCUMENT`). Cancelar volta ao início.
2. Com o arquivo escolhido, nada é restaurado ainda: a tela mostra "Arquivo selecionado: …" e pede a **Senha do backup**. O aviso "A senha não é armazenada…" e o botão **Continuar** completam a tela.
3. **Abrindo backup protegido…** chama `validate`, que faz leitura, autenticação, decifragem e validação completa. Uma senha curta é recusada antes da KDF.
4. **Confirmação** (ver abaixo).
5. Confirmado, o app abre o seletor "Salvar como" para o **backup preventivo** (`Fluxa-antes-da-restauracao-AAAA-MM-DD.fluxa`). Ele é cifrado com a mesma senha, a partir de uma cópia nova, como o handoff 8-B permite. Em seguida aparece **Restaurando seus dados…** (`restore`: preventivo + REPLACE + preferências) e, no fim, **Backup restaurado** com "Seus dados foram restaurados com sucesso." e **Ir para o Início**.

Cancelar o seletor do preventivo volta à confirmação, sem substituir nada.

## Confirmation

A confirmação usa o padrão destrutivo do DS: sheet com botão `Destructive`, botões empilhados, título como heading e conteúdo rolável em telas baixas. Ela traz:
- título **Substituir dados atuais?**;
- "A restauração substituirá os dados atuais do Fluxa pelos dados deste backup." e "Um backup preventivo será criado antes da substituição.", com a nota de que o usuário escolhe onde salvá-lo e que ele usa a mesma senha;
- botões **Restaurar backup** e **Cancelar**. Fechar o sheet também cancela e limpa a senha.

A v1 é sempre REPLACE: não existe "mesclar", "importar junto" nem "combinar".

## Errors

| Contrato | Onde aparece | Mensagem |
| --- | --- | --- |
| `INVALID_PASSWORD` | Formulário de senha, campo limpo | Não foi possível abrir o backup. Verifique a senha e tente novamente. |
| `PASSWORD_REQUIRED` / `PASSWORD_TOO_SHORT` / `PASSWORD_INVALID_ENCODING` | Erro no campo | Informe uma senha. / Use pelo menos 8 caracteres. / A senha contém caracteres que não podem ser usados. |
| `CORRUPTED`, `INVALID`, `INTEGRITY_FAILED`, `RELATION_INVALID`, `CRYPTO_UNSUPPORTED` | Erro definitivo | Este arquivo não pôde ser validado como um backup válido do Fluxa. |
| `VERSION_UNSUPPORTED` | Erro definitivo | Este backup foi criado por uma versão do Fluxa que este aplicativo ainda não consegue restaurar. |
| `TOO_LARGE` (importação) | Erro definitivo | Este arquivo é maior do que o limite de um backup do Fluxa. |
| `DECRYPTION_FAILED` | Erro definitivo | Não foi possível abrir o backup. Tente novamente. |
| `RESTORE_CONFLICT` | De volta à confirmação | Seus dados mudaram durante a restauração e nada foi substituído. Confirme novamente para tentar de novo. |
| Outros erros de `restore` | Erro definitivo | Não foi possível restaurar o backup. Seus dados atuais não foram alterados. |
| `EXPORT_FAILED` / `ENCRYPTION_FAILED` / `TOO_LARGE` (exportação) | Formulário de exportação | Não foi possível criar o backup. Tente novamente. / Seus dados excedem o tamanho máximo… |

A tela de erro definitivo oferece **Escolher outro arquivo** e **Voltar**. Nenhum caminho tenta restauração parcial.

A UI não afirma se o problema foi a senha ou uma alteração do arquivo. O GCM não distingue os dois casos, e o teste de adulteração confirma isso.

## State

`BackupRestoreViewModel` (`ui/feature/backup`) mantém um `StateFlow<BackupUiState>` com estes estados:
- `Home`;
- exportação: `ExportPassword` → `ChoosingDestination` → `Working(EXPORTING)` → `ExportSuccess`;
- restauração: `ChoosingFile` → `RestorePassword` → `Working(UNLOCKING)` → `RestoreConfirm` → `ChoosingPreventiveDestination` → `Working(RESTORING)` → `RestoreSuccess` | `RestoreError`.

Relação com os estados conceituais pedidos:
- `Encrypting` e `Writing` correspondem a uma única chamada (`export`), representada por `EXPORTING`;
- `Reading`, `Unlocking` e `Validating` correspondem a `validate`, representada por `UNLOCKING`;
- `Preparing`, `Restoring` e `ApplyingSettings` correspondem a `restore`, representada por `RESTORING`.

O backend não expõe progresso intermediário. Por isso a UI não inventa passos nem porcentagens.

Duplo toque é bloqueado: submissões e resultados de seletor são ignorados fora do estado esperado. Enquanto há trabalho, o botão some, o voltar do topo fica desabilitado e o voltar do sistema é consumido.

A criptografia não roda na main thread: `BackupDocuments` e `FluxaBackupService` usam `Dispatchers.IO` (confirmado no código da 8-B), e o ViewModel só chama funções `suspend`.

## Password clearing

A UI entrega `CharArray` novos (`toCharArray()`) e o ViewModel assume a posse:
- zera os arrays recebidos ao sair de `submitExport` e `submitRestorePassword`;
- guarda só uma cópia (`heldPassword`) entre a validação e o fim da operação;
- entrega ao backend sempre uma cópia nova, que o backend consome e zera.

A cópia retida é zerada em sucesso, cancelamento (inclusive do seletor), erro definitivo, `cancel()`/voltar e `onCleared()`.

Nada vai para `SavedStateHandle`. Depois da morte do processo, o fluxo recomeça em `Home` e a senha precisa ser informada de novo; um resultado de seletor que chegue sem senha retida é ignorado.

`String`s do Compose são imutáveis e não podem ser zeradas: elas só vivem durante a edição do campo.

## Settings restore

O backend aplica tema e privacidade (`hideAmounts`) depois do commit financeiro. `RestoreReceipt(settingsApplied = false)` continua sendo sucesso, com o aviso "Seus dados foram restaurados. Algumas preferências do aplicativo não puderam ser aplicadas." (teste JVM).

O tema vem do DataStore por `collectAsState` na raiz (`FluxaApp`) e muda sem recriar a Activity e sem voltar ao Início no meio do fluxo. Testado com o tema DARK restaurado sobre LIGHT, sem crash.

## App refresh

**Ir para o Início** (e o voltar na tela de sucesso) incrementa `shellGeneration` em `FluxaApp` (`key(shellGeneration) { FluxaNavigation() }`). Isso recria o shell de navegação inteiro: NavController e back stack novos, ViewModels das telas novos e a ocultação de valores da sessão descartada. Nenhuma tela segura estado anterior, e a privacidade restaurada passa a valer, como no teste com `hideAmounts = true` restaurado, onde o Início mostra "••••••".

As telas já observam o Room por Flow; a recriação garante também os estados locais. A Activity não é recriada.

## Accessibility

- Campos de senha com rótulo visível; o botão mostrar/ocultar tem `contentDescription` e 48dp.
- Os estados de processamento (`backup-working`) são regiões vivas *polite*.
- Erros são anunciados: `ErrorBanner` é *assertive*, assim como o bloco do campo com erro e o card de erro definitivo.
- O título da confirmação destrutiva é heading. Botões têm ≥ 48dp; as linhas de ação, ≥ 72dp.
- Ícone e texto acompanham a cor (aviso, erro, sucesso).

Integração Android: o `MainActivity` agora declara `windowSoftInputMode="adjustResize"`. Sem isso, o sistema fazia *pan* da janela com o teclado aberto, em cima do `imePadding` dos formulários: o título subia sob a barra de status e o botão ficava atrás do teclado (verificado no emulador). Isso vale para todos os formulários do app e foi revalidado na matriz completa.

## Responsive

Revisão visual em 320×640, 360, 390 escuro, 412, 360 com fonte 1,3 e 320 com fonte 1,3. São 12 telas por configuração (`BackupVisualCapture`, só com `-e fluxaCapture`); a seleção está em `screens/`. Defeitos encontrados e corrigidos:
1. **Teclado:** pan da janela e botão escondido (ver Accessibility). Corrigido com `adjustResize`.
2. **Erro escondido:** a mensagem abaixo do campo ficava atrás do teclado em telas baixas, porque o campo focado rolava de volta. Agora enviar fecha o teclado e o bloco do erro é revelado e anunciado. No erro de senha, o banner fica acima do campo.
3. **Processamento:** o corpo do texto estava alinhado à esquerda sob um título centralizado. Agora está centralizado.
4. **Confirmação:** a data aparecia duplicada (card e sheet). Foi removida do sheet para caber em 320dp com fonte 1,3.
5. **Voltar:** o voltar do topo parecia ativo durante o processamento. Agora fica desabilitado (`FeatureScreen(backEnabled)`).

## Performance UX

Medido na UI, do toque até o resultado, no emulador exclusivo API 34 x86_64:

| Operação | Tempo |
| --- | --- |
| Exportar | 2,8 s |
| Abrir (validar) | 2,7 s |
| Restaurar (preventivo + REPLACE) | 2,4 a 2,7 s |

O feedback com texto aparece em menos de 5 s (verificado nos testes). A UI segue responsiva, porque o trabalho roda em IO e o indicador anima, e não aceita um segundo envio.

**Aparelho físico:** não havia nenhum conectado (`adb devices` só lista o emulador). A medição de KDF em hardware real continua pendente.

## Tests

**JVM** — `BackupRestoreViewModelTest` (25 testes; a suíte toda tem 245). O fake dos contratos zera as senhas como o backend real. Cobertura:
- exportação: senha curta pela regra do backend, vazia, 8 pontos de código com emoji/acentos/espaços (7 pontos com 8 chars é recusado), confirmação diferente, cancelamento do seletor, sucesso, falha (com descarte do arquivo), `TOO_LARGE` e duplo envio durante o processamento;
- restauração: cancelar o seletor, senha antes de qualquer validação, senha correta até a confirmação, mesma senha no preventivo, cancelar a confirmação, cancelar o seletor do preventivo, senha incorreta com nova tentativa, senha curta sem KDF, arquivo inválido (5 erros), versão incompatível, `settingsApplied = false`, falha de restore, `RESTORE_CONFLICT` e duplo toque em Continuar;
- senha: todos os arrays recebidos e entregues terminam zerados, e `onCleared` limpa a senha retida.

**Instrumentados** — `BackupRestoreFlowTest` (8 testes), sobre o `MainActivity` real com os contratos reais (KDF, AES-GCM, Room, DataStore). Só o seletor do sistema é trocado por `ScriptedPicker` (via `BackupPickerTestHook`, sempre `null` em produção), que devolve documentos `file://` de forma assíncrona. Cobertura:
- exportação cria arquivo `FLUXAENC` que valida com a senha e contém os dados atuais;
- senhas diferentes e mostrar/ocultar;
- restauração substitui os dados e cria o preventivo, que guarda o estado substituído e abre com a mesma senha;
- cancelar a confirmação mantém os dados;
- senha errada não altera banco nem preferências;
- arquivo não-Fluxa, truncado e adulterado não alteram o banco;
- preferências restauradas (DARK + ocultar valores) valem no Início recriado;
- estado restaurado lido por uma conexão Room nova.

**Manual no emulador:**
- seletor real (DocumentsUI): exportar, abrir com senha errada e depois a correta, confirmar, salvar o preventivo, Ir para o Início;
- force-stop depois do restore: PID 5538 → 6485, mesma tela do Início;
- force-stop depois de exportar pelo seletor real: PID 10332 → 10861, Início idêntico (a exportação não altera o banco).

**Matriz offline** (modo avião, Wi-Fi e dados desligados), suíte instrumentada completa: 134 testes, 0 falhas e 7 rotinas opcionais puladas em 320×640, 360, 390 escuro, 412, 360 com fonte 1,3 e 320 com fonte 1,3. Na primeira rodada em 360 com fonte 1,3, `SubscriptionFlowTest.skippingAnOverdueOccurrenceRemovesItFromOverdueAndKeepsHistory` falhou uma vez. Esse teste não usa teclado nem tela de backup. Isolado, passou 3 de 3, e a suíte completa nessa configuração passou depois (intermitente, preexistente).

## Pendências

1. **Medição da KDF em aparelho físico:** nenhum aparelho conectado nesta sprint.
2. **`CRYPTO_UNSUPPORTED`** cobre tanto "não é um arquivo do Fluxa" quanto "envelope de versão futura". A UI usa a mensagem de arquivo inválido. Para mostrar "versão não suportada" num envelope futuro, o backend precisaria separar os casos (exige mudança de contrato; não foi feita).
3. **Backup preventivo incompleto:** se a restauração falha ao escrever o preventivo, o documento criado pode ficar incompleto. A UI não o apaga, porque `RESTORE_FAILED` não diz se a falha foi antes ou depois da escrita completa.
4. Biometria, auto-lock, `FLAG_SECURE`, backup automático, nuvem, recuperação e armazenamento de senha seguem fora do escopo. A sprint de segurança local definirá a estratégia global de `FLAG_SECURE`.

## Base recomendada

`claude/fluxa-android-sprint-8c-backup-ui`.
