# Integração Compose e Domain Core

A validação do shell cobre Início, Caixa, Cartões e Mais, além da abertura e do fechamento do sheet de Novo lançamento pelo botão central. O teste confirma que o destino anterior continua selecionado ao fechar o sheet.

## Revisão visual com o Claude

- Adicionar o ícone de launcher oficial, incluindo a variante adaptativa. O lint registra `MissingApplicationIcon`.
- Revisar os rótulos da barra flutuante e os insets do sheet com fonte ampliada, navegação por três botões e orientação horizontal. A validação desta integração usa um emulador de 320 × 640 dp em retrato.
- Verificar, com TalkBack no aparelho, a ordem de foco entre abas, botão central, título do sheet e opções de fechamento. Os testes automatizados verificam a interação Compose e os testes JVM verificam contraste dos tokens. A leitura por TalkBack precisa de revisão manual.
