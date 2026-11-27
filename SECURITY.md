# Política de segurança

## Versões cobertas

Apenas a versão mais recente publicada em [Releases](https://github.com/matssgit/fluxa-app/releases) recebe correções.

A versão web antiga em `frontend/` e `backend/` não é mantida ativamente.

## Como relatar uma vulnerabilidade

**Não abra uma issue pública** para problemas de segurança.

Use o recurso **Report a vulnerability** na aba **Security** deste repositório (relato privado de vulnerabilidades do GitHub). Só o mantenedor vê esse relato.

Inclua, se possível:

- a versão do Fluxa e do Android;
- o que é possível fazer e em que condições;
- os passos para reproduzir;
- uma prova de conceito com **dados fictícios**.

**Nunca envie** um backup real, dados financeiros reais ou senhas.

## O que esperar

O Fluxa é mantido por uma pessoa, no tempo disponível. Não há prazo garantido de resposta nem programa de recompensa. Relatos válidos serão confirmados, corrigidos em uma nova versão e creditados no [CHANGELOG](CHANGELOG.md), se você quiser.

## Escopo

Interessam especialmente:

- acesso a dados do app por outro app ou sem desbloqueio, com o bloqueio do app ligado;
- falhas na criptografia ou na validação dos arquivos de backup;
- formas de burlar o bloqueio do app ou a proteção de tela;
- qualquer caminho pelo qual dados saiam do aparelho.

Fora de escopo: aparelhos com root ou sistema modificado, acesso físico com o aparelho desbloqueado e o bloqueio do app desligado, e senhas de backup fracas escolhidas pelo usuário.

Limitações conhecidas estão em [docs/BACKUP_AND_SECURITY.md](docs/BACKUP_AND_SECURITY.md#o-que-o-fluxa-não-protege).
