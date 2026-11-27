# Roadmap e manutenção

O Fluxa é mantido por uma pessoa, conforme o tempo disponível, o uso real e o feedback recebido. **Não há datas.** Nada nesta lista é uma promessa.

## Versão atual

**1.0.2**: todas as funcionalidades descritas em [FEATURES.md](FEATURES.md), com a nova marca.

## Manutenção contínua

- Correção de bugs relatados nas issues.
- Compatibilidade com novas versões do Android.
- Revisão periódica das dependências, sempre com a suíte completa e o teste de atualização.
- Documentação mantida em dia com o código.

## Necessário para a distribuição pública

- Isolar a chave de distribuição num keystore exclusivo, com cópias de segurança (veja [RELEASE_PROCESS.md](RELEASE_PROCESS.md#chave-de-distribuição)).
- Validação da 1.0.2 no aparelho físico de referência.

## Em consideração

Possibilidades que surgem das limitações atuais. Podem ou não ser feitas, dependendo do feedback e de manterem o app simples e offline:

- Transferência entre contas como operação própria.
- Lançamentos futuros e receitas previstas no Limite seguro.
- Exportação dos dados para planilha (CSV).
- Lembretes de cobranças.
- `targetSdk` mais recente e validação em aparelhos com páginas de memória de 16 KB.
- Rotação para uma chave de assinatura nova, quando o `minSdk` chegar à API 33.
- Tela de licenças de terceiros dentro do app.
- Avaliação de uma publicação na Google Play, que tem requisitos próprios de `targetSdk`, assinatura e política.

Sugestões novas entram por [issues](https://github.com/matssgit/fluxa-app/issues/new/choose).

## Fora de escopo

- Servidor, conta, sincronização em nuvem ou qualquer acesso à internet.
- Conexão com bancos.
- Anúncios, rastreamento ou monetização.

## Como as issues são tratadas

| Rótulo | Uso |
| --- | --- |
| `bug` | Algo não funciona como documentado |
| `dados` | Risco de perda ou corrupção de dados. **Prioridade máxima** |
| `segurança` | Relatos que chegaram pelo canal privado e já foram corrigidos |
| `sugestão` | Ideias de melhoria |
| `documentação` | Textos, exemplos e traduções |
| `aguardando informação` | Falta algo para reproduzir |
| `não planejado` | Fora da proposta do app, com explicação |

Prioridade: perda de dados > segurança > cálculo financeiro errado > falha que impede o uso > problemas visuais > sugestões.
