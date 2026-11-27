## O que muda

<!-- Uma ou duas frases. Se resolve uma issue: "Resolve #123". -->

## Por quê

## Como foi testado

- [ ] `./gradlew testDebugUnitTest lintDebug`
- [ ] Testes instrumentados afetados (quais?)
- [ ] Testado em emulador ou aparelho (qual Android?)

## Checklist

- [ ] Valores em `Money` (centavos), sem `Double`/`Float`.
- [ ] Regras novas ou corrigidas têm teste.
- [ ] Mudança no banco tem migration, schema exportado e teste de migration (ou não se aplica).
- [ ] Mudança no formato de backup tem nova versão e compatibilidade (ou não se aplica).
- [ ] Sem acesso à internet ou dependência de rede.
- [ ] Documentação e CHANGELOG atualizados, se o comportamento mudou.
- [ ] Nenhum dado real, senha, chave ou backup no diff, nas capturas ou nos testes.
