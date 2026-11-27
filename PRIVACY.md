# Privacidade

Última revisão: versão 1.0.2.

## Resumo

O Fluxa não coleta, não envia e não vende dados. Não existe servidor do Fluxa. Tudo o que você registra fica no seu aparelho.

## Onde os dados ficam

- Contas, lançamentos, cartões, compras, assinaturas, metas e preferências ficam no **armazenamento interno do app**, no seu celular.
- Outros apps não têm acesso a esse armazenamento. O Android o protege com o isolamento entre apps e, nos aparelhos compatíveis, com a criptografia de armazenamento do sistema.
- **O Fluxa não aplica uma criptografia própria ao banco de dados local.** Quem tiver acesso ao armazenamento interno do app, como em um aparelho com root, consegue ler esses dados.

## O que o app não faz

- Não pede permissão de acesso à internet. Por isso, não consegue enviar dados para lugar nenhum.
- Não tem conta, login, analytics, anúncios, rastreamento ou relatórios de erro automáticos.
- Não acessa contatos, localização, câmera, microfone ou outros arquivos do aparelho.
- Não participa do backup automático do Android nem da transferência de dados entre aparelhos.

## Permissões

| Permissão | Para quê |
| --- | --- |
| `USE_BIOMETRIC`, `USE_FINGERPRINT` | Desbloquear o app com a biometria ou o bloqueio de tela do Android, se você ativar o bloqueio |
| Permissão interna do próprio app | Proteção técnica criada pela biblioteca AndroidX. Não dá acesso a nada fora do app |

## Autenticação

O bloqueio do Fluxa usa a autenticação do próprio Android. O app recebe apenas o resultado (autenticado ou não). Ele **não tem acesso** à sua biometria, ao seu PIN ou à sua senha.

## Backups

- Só são feitos quando você pede, e só são gravados no local que você escolher.
- São criptografados com uma senha definida por você. O Fluxa não guarda essa senha nem tem como recuperá-la.
- Depois de gravado, o arquivo é seu: onde ele fica, quem tem acesso e se ele vai para algum serviço de nuvem depende de você.

## Proteção de tela

A opção **Proteger conteúdo da tela** impede capturas e gravações de tela e esconde a prévia do app em Recentes. Ela não impede fotos da tela feitas com outro aparelho.

## Desinstalação

Desinstalar o Fluxa **apaga todos os dados do aparelho**. Isso não pode ser desfeito sem um backup.

## Issues e contato

Se você abrir uma issue pública no GitHub, o conteúdo fica visível para qualquer pessoa. Não inclua dados financeiros reais, senhas, backups ou informações pessoais.

## Mudanças

Se uma versão futura mudar algo descrito aqui, este documento e o [CHANGELOG](CHANGELOG.md) serão atualizados na mesma versão.
