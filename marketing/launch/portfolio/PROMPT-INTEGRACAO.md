# Prompt: integrar o Fluxa ao portfólio

Copie o bloco abaixo para um agente que esteja trabalhando **dentro do repositório do portfólio** (`https://matheusantanadev.vercel.app/`). Ele é independente: não pressupõe acesso a este repositório além dos links públicos.

---

```text
TAREFA: adicionar o Fluxa como um dos projetos principais do meu portfólio.

CONTEXTO
O Fluxa é um aplicativo Android de controle financeiro pessoal, gratuito, offline-first e de código aberto
(licença MIT). Começou como uma aplicação web (React, Fastify, PostgreSQL) e foi reconstruído como app nativo,
sem servidor, sem conta e sem permissão de internet. Versão pública atual: 1.0.2, distribuída pelo GitHub Releases.
É um produto real e mantido publicamente, não um projeto acadêmico.

RESTRIÇÕES
- Preserve a identidade visual, a navegação, a tipografia e os componentes existentes do portfólio.
  O Fluxa entra como um projeto entre os demais, com destaque, mas o portfólio NÃO vira uma landing page do Fluxa.
- Reaproveite o componente de card e o padrão de página de projeto que o portfólio já usa.
  Só crie componentes novos se não houver equivalente.
- Não faça deploy. Não publique. Deixe as mudanças prontas para minha revisão, num commit local ou num pull request,
  conforme o fluxo do repositório.
- Não invente números (downloads, usuários, avaliações). Use só os dados abaixo.

LINKS OFICIAIS (todos públicos e verificados)
- Baixar o aplicativo (botão principal): https://github.com/matssgit/fluxa-app/releases/latest
- Código-fonte: https://github.com/matssgit/fluxa-app
- Documentação: https://github.com/matssgit/fluxa-app/tree/main/docs
- Sugestões: https://github.com/matssgit/fluxa-app/issues/new?template=feature_request.yml
- Relatar bug: https://github.com/matssgit/fluxa-app/issues/new?template=bug_report.yml
- Histórico de versões: https://github.com/matssgit/fluxa-app/releases
- Licença: https://github.com/matssgit/fluxa-app/blob/main/LICENSE
O botão de download aponta para a PÁGINA da última release (não para o arquivo), para continuar válido nas próximas versões.

IMAGENS (baixe e salve nos assets do portfólio, otimizando para web: WebP ou AVIF com fallback, e tamanhos responsivos)
Base: https://raw.githubusercontent.com/matssgit/fluxa-app/main/
- Card (800×500): marketing/launch/portfolio/card-800x500.png
- Capa/banner (1600×1000): marketing/launch/portfolio/capa-1600x1000.png
- Logo para fundo claro: marketing/branding/final/svg/logo-petroleo.svg
- Logo para fundo escuro: marketing/branding/final/svg/logo-creme.svg
- Ícone do app: marketing/branding/final/png/icone-petroleo-1024.png
- Telas reais (720 px de largura, dados fictícios), use 4 a 6:
  marketing/launch/screenshots/claro-inicio.png
  marketing/launch/screenshots/claro-limite.png
  marketing/launch/screenshots/claro-compra.png
  marketing/launch/screenshots/escuro-analises.png
  marketing/launch/screenshots/escuro-assinaturas.png
  marketing/launch/screenshots/escuro-seguranca.png
Paleta do Fluxa (use só dentro do case, se combinar com o portfólio): petróleo #14414D, creme #F6F2EA, terracota #B9684F.

CONTEÚDO
Card:
  Título: Fluxa
  Texto: Controle financeiro pessoal para Android, gratuito, offline e de código aberto.
  Tags: Android · Kotlin · Jetpack Compose · Room · Offline-first
  CTAs: "Baixar aplicativo" (link de download) · "Ver código-fonte" · "Conhecer o projeto" (página do case)

Página do case (seções):
1. Apresentação: "Controle financeiro pessoal para Android. Gratuito, sem conta e sem internet."
   Papel: produto, design e desenvolvimento · Plataforma: Android 11+ · Status: versão 1.0.2 publicada, código aberto (MIT).
2. Problema: apps de finanças pedem conta, enviam dados a servidores ou vivem de anúncios; planilhas não respondem
   "quanto posso gastar sem comprometer o que já está combinado?", principalmente com parcelas e assinaturas.
3. Solução: app nativo que fica inteiro no celular e calcula o Limite Seguro — uma estimativa de quanto dá para gastar
   hoje considerando reservas e os compromissos registrados para os próximos 30 dias (não é garantia).
   Origem: começou como aplicação web; virou Android nativo porque, para dados financeiros pessoais,
   menos infraestrutura significa menos risco.
4. Funcionalidades: contas, receitas e despesas, categorias; cartões, compras parceladas e pagamento de parcelas;
   assinaturas com agenda; metas com reservas; Limite Seguro; análises e projeção de 30 dias;
   backup criptografado; bloqueio com biometria; tema claro e escuro.
5. Arquitetura e stack: Kotlin, Jetpack Compose, Material 3, Room/SQLite, DataStore, Coroutines/Flow,
   AndroidX Biometric. Telas → ViewModels → repositórios → regras de domínio em Kotlin puro.
   Sem bibliotecas de rede, analytics ou anúncios.
6. Decisões de engenharia: dinheiro em centavos inteiros; saldos e limites derivados dos fatos, nunca armazenados;
   operações atômicas e índices únicos contra pagamentos duplicados; migrations de banco versionadas;
   backup com formato versionado.
7. Segurança e privacidade: sem permissão de internet; dados no armazenamento interno do app; backup com
   AES-256-GCM e chave derivada da senha (PBKDF2). Limitação declarada: o banco local não tem criptografia própria.
8. Testes: 326 testes JVM e 200 casos instrumentados; testes de atualização entre versões; validação em aparelho físico.
9. Status e contribuição: gratuito, código aberto, aberto a sugestões e relatos de bugs; evolui conforme o uso,
   sem calendário fixo. Links de sugestões e bugs.

RESPONSIVIDADE
- Funcionar de 320 px a telas largas, sem rolagem horizontal.
- Telas do app em grade que vira carrossel ou coluna única no celular.
- CTAs empilhados no celular, com área de toque de pelo menos 44×44 px.

ACESSIBILIDADE
- Texto alternativo descritivo em todas as imagens (ex.: "Tela Início do Fluxa com saldo disponível e Limite Seguro").
- Contraste mínimo WCAG AA no texto e nos botões; foco visível; ordem de leitura lógica.
- Links externos com indicação (ícone ou texto) e rel="noopener noreferrer" quando abrirem em nova aba.
- Respeitar prefers-reduced-motion se houver animações.

SEO
- Título e meta description próprios na página do case
  (ex.: "Fluxa — app Android de finanças pessoais offline e open source | Matheus Santana").
- Open Graph e Twitter Card com a capa (1600×1000) ou o card.
- Dados estruturados (JSON-LD) do tipo SoftwareApplication: name Fluxa, operatingSystem Android 11+,
  applicationCategory FinanceApplication, offers price 0, license MIT, downloadUrl e codeRepository com os links acima.
- Incluir a página do case no sitemap, se o portfólio tiver um.

VALIDAÇÃO ANTES DE ENTREGAR
- Rodar o build e os testes/lint do portfólio.
- Abrir a página do case e a lista de projetos em largura de celular e de desktop e conferir visualmente.
- Testar todos os links (devem responder 200 ou redirecionar para 200), inclusive o de download.
- Confirmar que as imagens carregam e têm texto alternativo.
- Listar os arquivos alterados e criados.
```
