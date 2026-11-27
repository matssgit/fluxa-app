# Fluxa: case para o portfólio

Conteúdo pronto para integrar ao portfólio. Links oficiais e verificados em [../LINKS.md](../LINKS.md). Prompt para o agente do portfólio em [PROMPT-INTEGRACAO.md](PROMPT-INTEGRACAO.md).

**Logo:** `marketing/branding/final/svg/logo-petroleo.svg` (fundo claro) e `logo-creme.svg` (fundo escuro) · **Banner:** `capa-1600x1000.png` · **Telas:** `../screenshots/`

## Card do projeto

**Fluxa**
Controle financeiro pessoal para Android, gratuito, offline e de código aberto. Kotlin, Jetpack Compose e Room, com 326 testes automatizados.

Imagem: `card-800x500.png` · Tags: Android · Kotlin · Jetpack Compose · Room · Offline-first

## Chamadas para ação

1. **Baixar o aplicativo** → `https://github.com/matssgit/fluxa-app/releases/latest`
2. **Ver o código-fonte** → `https://github.com/matssgit/fluxa-app`
3. **Conhecer o projeto** → página do case no portfólio (ou `https://github.com/matssgit/fluxa-app#readme`)

Links secundários:
- Documentação → `https://github.com/matssgit/fluxa-app/tree/main/docs`
- Sugestões → `https://github.com/matssgit/fluxa-app/issues/new?template=feature_request.yml`
- Histórico de versões → `https://github.com/matssgit/fluxa-app/releases`

---

## Página do case

**Capa:** `capa-1600x1000.png`

### Fluxa

Controle financeiro pessoal para Android. Gratuito, sem conta e sem internet.

**Papel:** produto, design e desenvolvimento · **Plataforma:** Android 11+ · **Status:** versão 1.0.2 publicada no GitHub Releases, de código aberto (MIT) e mantida publicamente

### O problema

Apps de finanças costumam pedir conta, enviar dados para um servidor ou viver de anúncios e assinatura. Planilhas resolvem o registro, mas não respondem com facilidade a pergunta que mais importa no dia a dia: **quanto posso gastar sem comprometer o que já está combinado?** Parcelas de cartão e assinaturas tornam essa conta difícil de fazer de cabeça.

### A proposta

Um app que fica inteiramente no celular, organiza o que entra, o que sai e o que já está comprometido, e calcula um **Limite seguro**: o valor que pode ser gasto hoje sem faltar dinheiro para as parcelas e assinaturas dos próximos 30 dias.

O Fluxa começou como aplicação web (React, Fastify e PostgreSQL). Para dados financeiros de uma pessoa, servidor e login traziam mais risco e manutenção do que valor. Então ele foi reconstruído como app Android nativo e offline.

### Funcionalidades

- Contas, receitas, despesas, categorias e histórico.
- Cartões de crédito, compras parceladas, pagamento de parcelas com desfazer.
- Assinaturas mensais e anuais, em conta ou cartão, com agenda.
- Metas com dinheiro reservado em uma conta.
- Limite seguro e projeção de 30 dias.
- Análises: resumo do mês, categorias, últimos seis meses e custo de assinaturas.
- Backup criptografado, bloqueio com biometria, proteção de tela, tema claro e escuro.

Telas: `../screenshots/` (claro e escuro).

### Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room/SQLite · DataStore · Coroutines e Flow · AndroidX Biometric · Gradle.

### Arquitetura

Telas Compose → ViewModels → fontes por funcionalidade → repositórios → regras de domínio puras + Room e DataStore. Injeção de dependências manual, sem frameworks. Nenhuma biblioteca de rede, analytics ou anúncio.

### Decisões técnicas

- **Dinheiro em centavos inteiros**, com aritmética exata e detecção de estouro.
- **Valores derivados**: saldo, limite usado, reservado e projeções são sempre recalculados a partir dos fatos, nunca armazenados.
- **Sem contagem dupla**: a compra no cartão vira despesa no pagamento da parcela; a reserva de uma meta sai do disponível, não do saldo.
- **Operações atômicas** com índices únicos e triggers como segunda barreira contra pagamentos duplicados e estados inválidos.
- **Migrations explícitas** da versão 1 à 7 e **backup versionado**, independente do banco.

### Desafios

- Calcular o primeiro vencimento de uma compra a partir do fechamento do cartão, inclusive em meses curtos.
- Projetar 30 dias de compromissos sem duplicar nada que apareça por dois caminhos, como uma assinatura cobrada no cartão.
- Garantir que restaurar um backup nunca deixe o app pela metade: validação completa antes, cópia preventiva e troca em uma única transação.
- Manter tudo legível e acessível em telas de 320 dp com fonte ampliada.

### Segurança e privacidade

Sem permissão de internet. Dados no armazenamento interno do app, fora do backup automático do Android. Backup com AES-256-GCM e chave derivada por PBKDF2 (600 mil iterações). Bloqueio pelo desbloqueio do próprio Android e proteção opcional contra capturas de tela.

Limitação declarada: o banco local não tem criptografia própria e conta com a proteção do Android.

### Qualidade

- 326 testes JVM.
- 200 casos instrumentados, executados offline e sem falhas em cinco combinações de tela e fonte.
- Testes de atualização entre versões, comparando o banco inteiro antes e depois.
- Lint sem avisos. Validação manual no Samsung Galaxy S23.

### Status

Em uso pelo autor. Quatro pessoas próximas receberam o app e demonstraram interesse em usar. Gratuito e de código aberto (licença MIT), com melhorias publicadas conforme o uso e o feedback.

### Feedback

Sugestões: `https://github.com/matssgit/fluxa-app/issues/new?template=feature_request.yml`
Bugs: `https://github.com/matssgit/fluxa-app/issues/new?template=bug_report.yml`
