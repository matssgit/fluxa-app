# LinkedIn: textos de lançamento

Três versões. A **principal** é a recomendada. Os links entram só quando existirem; até lá, ficam marcados como `[link …]`.

Antes de publicar:
- A abertura da versão principal ("Há alguns meses eu queria…") é uma sugestão de narrativa. Ajuste-a ao que de fato motivou você.
- Se o código ainda não estiver público com licença aprovada, use as frases marcadas com **(sem código aberto)**.
- Anexe o PDF `carrossel/fluxa-carrossel.pdf` como documento.
- O LinkedIn corta o texto depois de cerca de 210 caracteres. A primeira frase precisa funcionar sozinha.

---

## 1. Principal (recomendada)

Há alguns meses eu queria uma resposta simples: quanto posso gastar este mês sem me complicar com o que já está combinado?

Foi assim que nasceu o Fluxa. Primeiro como uma aplicação web, com servidor, banco de dados e login. Funcionava, mas para controlar as finanças de uma pessoa era estrutura demais: dados financeiros em um servidor, sessão para manter, dependência de internet.

Então reconstruí o Fluxa como um aplicativo Android nativo, que funciona totalmente offline. Não tem conta, não tem servidor e não tem nem permissão de acesso à internet. Os dados ficam no celular, e o backup é um arquivo criptografado com uma senha que só a pessoa conhece.

Hoje ele organiza contas, receitas e despesas, cartões com compras parceladas, assinaturas e metas. Com isso, calcula um "Limite seguro": quanto dá para gastar sem faltar dinheiro para as parcelas e assinaturas dos próximos 30 dias.

A parte mais difícil não foi a interface, foi a consistência. Uma compra no cartão não pode virar despesa duas vezes. Uma reserva para uma meta precisa sair do disponível sem sumir da conta. Um toque duplo não pode pagar a mesma parcela duas vezes. Restaurar um backup não pode deixar o app pela metade. Por isso tudo é calculado em centavos inteiros, as operações são atômicas e o app tem 326 testes automatizados de JVM, além dos testes instrumentados e de atualização entre versões.

Eu uso o Fluxa no dia a dia. Também mandei o app para quatro pessoas próximas, que pediram para começar a usar. Agora decidi deixar o app gratuito e aberto para quem quiser testar.

Ele vai continuar recebendo melhorias aos poucos, conforme o uso e o que as pessoas sugerirem.

Se você usa Android e quer testar, o download está aqui: [link da release]
O código-fonte está no GitHub: [link do repositório] **(sem código aberto: trocar por "Em breve, o código-fonte também estará no GitHub.")**

Sugestões e relatos de problemas são muito bem-vindos, pelos comentários ou pelas issues do repositório.

#Android #Kotlin #JetpackCompose #DesenvolvimentoDeSoftware #FinançasPessoais

---

## 2. Técnica

Publiquei o Fluxa, um app Android de finanças pessoais que fiz do zero e que roda 100% offline. Algumas decisões de engenharia que valem compartilhar:

**Arquitetura.** Kotlin, Jetpack Compose, Room e DataStore, sem servidor e sem permissão de internet. As regras financeiras são funções puras em Kotlin, separadas dos repositórios. Os repositórios abrem a transação no banco, chamam a regra e gravam o resultado. As telas observam `Flow`s que recalculam quando as tabelas mudam.

**Dinheiro.** Todo valor é um `Long` em centavos, com soma exata. Totais grandes são acumulados em `BigInteger`, e um estouro aparece como estouro, não como um número errado. Parcelas dividem os centavos restantes nas últimas para que a soma feche exatamente.

**Consistência.** Saldo, limite usado do cartão, valor reservado e projeções nunca ficam armazenados; são derivados dos fatos. Índices únicos impedem duas despesas para a mesma parcela ou cobrança. Triggers no SQLite são uma segunda barreira contra estados inválidos.

**Limite seguro.** Percorre as parcelas e assinaturas dos próximos 30 dias em ordem e devolve o menor saldo projetado, não só o saldo final. Se o mesmo compromisso aparecer com dois valores, o cálculo prefere ficar indisponível a mostrar um número errado.

**Dados do usuário.** Migrations explícitas da versão 1 à 7, nunca destrutivas. O backup é um formato versionado, criptografado com AES-GCM e chave derivada por PBKDF2, e é totalmente validado antes de qualquer escrita. A restauração salva antes uma cópia preventiva e troca os dados em uma única transação.

**Testes.** 326 testes JVM, 200 casos instrumentados executados offline em cinco combinações de tela e fonte, e testes de atualização entre versões que comparam o banco inteiro antes e depois.

O Fluxa começou como aplicação web (React, Fastify e PostgreSQL) e virou Android nativo quando ficou claro que, para dados financeiros pessoais, menos infraestrutura era mais segurança.

Eu uso no dia a dia, e quatro pessoas próximas receberam o app e querem começar a usar. Agora ele está disponível de graça.

APK: [link da release]
Código: [link do repositório] **(sem código aberto: remover esta linha)**

Feedback técnico é bem-vindo, principalmente sobre o domínio financeiro e a estratégia de testes.

#Android #Kotlin #JetpackCompose #EngenhariaDeSoftware #OpenSource **(sem código aberto: trocar #OpenSource por #DesenvolvimentoMobile)**

---

## 3. Curta

Fiz um app de finanças pessoais para Android que funciona sem internet, sem conta e sem anúncios: o Fluxa.

Ele organiza contas, cartões, parcelas, assinaturas e metas, e mostra quanto dá para gastar sem comprometer o que vence nos próximos 30 dias. Os dados ficam no celular, com backup criptografado.

Comecei usando sozinho, mandei para algumas pessoas próximas e agora ele está disponível de graça. Ainda vou continuar melhorando.

Download: [link da release]
Código: [link do repositório] **(sem código aberto: remover esta linha)**

Testou? Me conta o que achou.

#Android #Kotlin #FinançasPessoais

---

## Comentário fixado (opcional)

Para quem for testar: o Fluxa precisa de Android 11 ou superior e ainda não está na Google Play, então a instalação é pelo APK. Antes de trocar de celular ou desinstalar, faça um backup em Mais → Backup e restauração; desinstalar apaga os dados.
