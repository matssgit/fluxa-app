# Como contribuir

Obrigado pelo interesse no Fluxa. Ele é mantido por uma pessoa, então toda ajuda conta, inclusive um relato de bug bem escrito.

> [!WARNING]
> Issues são públicas. **Não publique dados financeiros reais, senhas, arquivos de backup ou informações pessoais.** Use valores fictícios e, se for enviar uma captura de tela, troque os valores reais ou ative a opção de ocultar valores (ícone de olho).

## Formas de contribuir

- **Relatar um bug**: abra uma issue com o modelo *Relato de bug*.
- **Sugerir uma melhoria**: abra uma issue com o modelo *Sugestão*. Explique o problema que você tem, não só a solução.
- **Contar como usa**: feedback de uso, mesmo sem um pedido específico, ajuda a priorizar.
- **Documentação**: correções de texto, exemplos e traduções.
- **Código**: veja abaixo.

## Antes de escrever código

Para qualquer coisa além de uma correção pequena, **abra uma issue antes** e combine a abordagem. Isso evita trabalho que não vai ser incorporado. Algumas coisas não fazem parte da proposta do app:

- acesso à internet, contas, sincronização com servidor, anúncios ou rastreamento;
- dependências grandes para resolver problemas pequenos;
- mudanças que alterem dados de quem já usa sem uma migration testada.

## Ambiente

Veja [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md). Resumo:

```bash
cd android
./gradlew testDebugUnitTest lintDebug
```

## Pull requests

- Um assunto por pull request.
- Siga o estilo do código existente: Kotlin idiomático, regras financeiras como funções puras em `domain/rules` e operações que mudam dados dentro de `withTransaction`.
- **Dinheiro sempre em `Money` (centavos)**. Nunca use `Double` ou `Float` para valores.
- Inclua testes para regras novas ou corrigidas. Bugs de regra financeira devem vir com um teste que falhava antes.
- Mudanças no banco exigem uma migration nova, o schema exportado e um teste de migration. Nunca use migração destrutiva.
- Mudanças no formato de backup exigem uma nova versão do formato e compatibilidade com a anterior.
- Textos da interface em português do Brasil.
- Rode `./gradlew testDebugUnitTest lintDebug` antes de enviar.

## Mensagens de commit

Curtas e no imperativo, de preferência no formato `tipo(escopo): descrição`. Por exemplo: `fix(credit): corrige vencimento em meses de 30 dias`.

## Conduta

Ao participar, você concorda com o [Código de Conduta](CODE_OF_CONDUCT.md).

## Licença das contribuições

O projeto usa a [licença MIT](LICENSE). Ao enviar uma contribuição, você concorda que ela seja distribuída sob a mesma licença e confirma que tem o direito de enviá-la. Não inclua código, imagens ou textos de terceiros sem licença compatível e sem indicar a origem.
