# Backup e segurança

Como o Fluxa protege os dados, o que ele **não** protege e como funcionam o backup e a restauração. Código em [`backup/`](../android/app/src/main/java/com/fluxa/personal/backup) e [`security/`](../android/app/src/main/java/com/fluxa/personal/security).

## Resumo

| Onde está o dado | Proteção |
| --- | --- |
| Banco do app (Room/SQLite) | Isolamento entre apps e criptografia de armazenamento do Android. **O app não criptografa o banco.** |
| Preferências (DataStore) | Mesma proteção do banco |
| Arquivo de backup `.fluxa` | **AES-256-GCM** com chave derivada da sua senha (PBKDF2-HMAC-SHA256) |
| Tela do app | Bloqueio pela autenticação do Android e proteção opcional contra capturas |

## Permissões

O APK pede apenas:

- `USE_BIOMETRIC` e `USE_FINGERPRINT` (esta última incluída pela biblioteca AndroidX Biometric), para o bloqueio do app;
- `com.fluxa.personal.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, uma permissão interna de nível *signature* gerada pelo AndroidX.

**Não há permissão de internet**, localização, câmera, microfone, contatos nem acesso amplo ao armazenamento. Arquivos de backup são lidos e gravados só pelo seletor de arquivos do Android, no local que você escolher.

O manifesto também declara `android:allowBackup="false"`, regras que excluem todos os dados do backup em nuvem e da transferência entre aparelhos, e `usesCleartextTraffic="false"`. A release não é depurável. Os scripts em `android/scripts/audit-*.py` conferem isso em cada APK.

## Backup

### Exportar

1. Você escolhe uma senha de **pelo menos 8 caracteres**.
2. O app lê todas as tabelas numa única transação e monta um documento com um manifesto (versão do formato, versão do schema, versão do app, data) e os registros.
3. O documento é compactado e criptografado.
4. O arquivo `.fluxa` é gravado onde você escolher.

Conteúdo: todos os dados financeiros, o tema e a opção de ocultar valores.
Fora do backup: as **configurações de segurança** (bloqueio, bloqueio automático e proteção de tela). Elas pertencem ao aparelho e não são restauradas em outro.

O Fluxa **não guarda a senha** e não faz backup automático.

### Formato do arquivo

```
FLUXAENC | versão 1 | algoritmo | iterações | tamanhos | salt (16 bytes) | nonce (12 bytes) | dados cifrados + tag (16 bytes)
```

- Cabeçalho fixo de 52 bytes, **sem nenhum metadado financeiro**.
- Chave: PBKDF2-HMAC-SHA256 com **600.000 iterações**, salt aleatório e chave de 256 bits.
- Cifra: AES-GCM com nonce aleatório de 96 bits e tag de 128 bits. O cabeçalho entra como dado autenticado (AAD): qualquer alteração nele invalida o arquivo.
- O conteúdo decifrado só é usado **depois** de a autenticação do GCM passar.
- Limites de tamanho são conferidos antes de alocar memória. A versão do formato (1) é separada da versão do banco, para que o app possa evoluir sem quebrar backups antigos.

### Restaurar

1. Você escolhe o arquivo e digita a senha.
2. O app decifra, descompacta e **valida tudo antes de mudar qualquer coisa**: versão, campos, tipos, chaves, referências entre tabelas e regras como as categorias de cada lançamento.
3. O app pede onde salvar uma **cópia preventiva** dos dados atuais, com uma senha. Essa cópia é gravada e finalizada antes da troca.
4. Os dados atuais são substituídos pelos do backup em **uma única transação**. Se algo falhar, nada muda.

Mensagens de erro distinguem arquivo que não é do Fluxa, arquivo corrompido, senha errada, versão não suportada e arquivo grande demais.

### Responsabilidade do usuário

- **Sem a senha, o backup não pode ser aberto.** Não existe recuperação de senha.
- O arquivo é seu: guarde-o fora do celular (computador, pen drive ou um serviço de nuvem de sua escolha).
- **Desinstalar o Fluxa apaga todos os dados do aparelho.** Faça um backup antes de trocar de celular, reinstalar ou testar uma versão nova.

## Bloqueio do app

- Usa o `BiometricPrompt` do Android com **biometria forte** ou a **credencial do aparelho** (PIN, padrão ou senha). O Fluxa não tem senha própria e não vê sua biometria.
- Se o aparelho não tiver nenhum bloqueio configurado, o app explica e oferece abrir as configurações do Android.
- Ligar ou desligar o bloqueio exige autenticar.
- **Bloqueio automático** ao ir para segundo plano: imediatamente, ou após 1, 5 ou 15 minutos. O tempo é medido pelo relógio monotônico (`elapsedRealtime`), que não muda se o usuário alterar a hora do aparelho.
- Enquanto está bloqueado, o conteúdo financeiro não é composto na tela. Cancelar a autenticação mantém o bloqueio.
- Fluxos externos confiáveis, como o seletor de arquivos do backup, não disparam o bloqueio no meio da operação.

Vem **desligado** por padrão.

## Proteção de tela

Quando ligada, a janela recebe `FLAG_SECURE`: o Android bloqueia capturas e gravações de tela e mostra a prévia em Recentes em branco.

Ela não impede alguém de fotografar a tela com outro aparelho, nem protege contra um sistema comprometido. Vem **desligada** por padrão.

## Ocultar valores

O botão de olho troca os valores por `••••••` no Início, Caixa, Cartões, Análises e Limite seguro, inclusive nos textos lidos por leitores de tela. É uma proteção contra olhares por cima do ombro, não criptografia.

## O que o Fluxa não protege

- Um aparelho desbloqueado nas mãos de outra pessoa, com o bloqueio do app desligado.
- Um aparelho com root, malware ou sistema modificado.
- Um backup cuja senha seja fraca ou conhecida por outras pessoas.
- O banco local contra quem consiga ler o armazenamento interno do app, como em aparelhos com root. Ele não tem uma camada própria de criptografia.

Para relatar uma vulnerabilidade, veja [SECURITY.md](../SECURITY.md).
