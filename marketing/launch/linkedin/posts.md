# LinkedIn: textos de lançamento

Três versões. A **principal** (1) é a definitiva: pronta para copiar e publicar.

- Release publicada: https://github.com/matssgit/fluxa-app/releases/tag/v1.0.2
- Anexe `carrossel/fluxa-carrossel.pdf` como documento (9 páginas).
- A primeira frase aparece antes do "ver mais": ela precisa funcionar sozinha.
- As quatro pessoas **receberam o app e demonstraram interesse**. Não as descreva como usuários ativos.

---

## 1. Principal (recomendada)

Há alguns meses eu queria uma resposta simples: quanto posso gastar este mês sem me complicar com o que já está combinado?

Foi assim que nasceu o Fluxa. Primeiro como uma aplicação web, com servidor, banco de dados e login. Funcionava, mas para controlar as finanças de uma pessoa era estrutura demais: dados financeiros em um servidor, sessão para manter, dependência de internet.

Então reconstruí o Fluxa como um aplicativo Android nativo, que funciona totalmente offline. Não tem conta, não tem servidor e não tem nem permissão de acesso à internet. Os dados ficam no celular, e o backup é um arquivo criptografado com uma senha que só a pessoa conhece.

Hoje ele organiza contas, receitas e despesas, cartões com compras parceladas, assinaturas e metas. Com isso, calcula um "Limite seguro": quanto dá para gastar sem faltar dinheiro para as parcelas e assinaturas dos próximos 30 dias.

A parte mais difícil não foi a interface, foi a consistência. Uma compra no cartão não pode virar despesa duas vezes. Uma reserva para uma meta precisa sair do disponível sem sumir da conta. Um toque duplo não pode pagar a mesma parcela duas vezes. Restaurar um backup não pode deixar o app pela metade. Por isso tudo é calculado em centavos inteiros, as operações são atômicas e o app tem 326 testes automatizados de JVM, além dos testes instrumentados e de atualização entre versões.

Eu uso o Fluxa no dia a dia. Também mandei o app para quatro pessoas próximas, que demonstraram interesse em começar a usar. Agora ele está disponível para qualquer pessoa: gratuito, sem anúncios e com o código aberto sob a licença MIT.

Ele vai continuar recebendo melhorias aos poucos, conforme o uso e o que as pessoas sugerirem.

Se você usa Android e quer testar: https://github.com/matssgit/fluxa-app/releases/latest
O código e a documentação: https://github.com/matssgit/fluxa-app

Sugestões e relatos de problemas são muito bem-vindos, aqui nos comentários ou pelas issues do repositório.

#Android #Kotlin #JetpackCompose #OpenSource #FinançasPessoais

---

## 2. Técnica

Publiquei o Fluxa, um app Android de finanças pessoais que fiz do zero, roda 100% offline e agora tem o código aberto. Algumas decisões de engenharia que valem compartilhar:

**Arquitetura.** Kotlin, Jetpack Compose, Room e DataStore, sem servidor e sem permissão de internet. As regras financeiras são funções puras em Kotlin, separadas dos repositórios. Os repositórios abrem a transação no banco, chamam a regra e gravam o resultado. As telas observam `Flow`s que recalculam quando as tabelas mudam.

**Dinheiro.** Todo valor é um `Long` em centavos, com soma exata. Totais grandes são acumulados em `BigInteger`, e um estouro aparece como estouro, não como um número errado. Parcelas distribuem os centavos restantes nas últimas para que a soma feche exatamente.

**Consistência.** Saldo, limite usado do cartão, valor reservado e projeções nunca ficam armazenados; são derivados dos fatos. Índices únicos impedem duas despesas para a mesma parcela ou cobrança. Triggers no SQLite são uma segunda barreira contra estados inválidos.

**Limite seguro.** Percorre as parcelas e assinaturas dos próximos 30 dias em ordem e devolve o menor saldo projetado, não só o saldo final. Se o mesmo compromisso aparecer com dois valores, o cálculo prefere ficar indisponível a mostrar um número errado.

**Dados do usuário.** Migrations explícitas da versão 1 à 7, nunca destrutivas. O backup é um formato versionado, criptografado com AES-GCM e chave derivada por PBKDF2, e é totalmente validado antes de qualquer escrita. A restauração salva antes uma cópia preventiva e troca os dados em uma única transação.

**Testes.** 326 testes JVM, 200 casos instrumentados executados offline em cinco combinações de tela e fonte, e testes de atualização entre versões que comparam o banco inteiro antes e depois.

**Distribuição.** APK assinado, publicado só no GitHub Releases com SHA-256, e uma estratégia de assinatura escolhida para não quebrar a atualização de quem já tinha o app.

O Fluxa começou como aplicação web (React, Fastify e PostgreSQL) e virou Android nativo quando ficou claro que, para dados financeiros pessoais, menos infraestrutura era mais segurança.

Eu uso no dia a dia, e quatro pessoas próximas receberam o app e demonstraram interesse em usar. Ele é gratuito e o código está sob licença MIT.

APK: https://github.com/matssgit/fluxa-app/releases/latest
Código e documentação: https://github.com/matssgit/fluxa-app

Feedback técnico é bem-vindo, principalmente sobre o domínio financeiro e a estratégia de testes.

#Android #Kotlin #JetpackCompose #EngenhariaDeSoftware #OpenSource

---

## 3. Curta

Fiz um app de finanças pessoais para Android que funciona sem internet, sem conta e sem anúncios: o Fluxa.

Ele organiza contas, cartões, parcelas, assinaturas e metas, e mostra quanto dá para gastar sem comprometer o que vence nos próximos 30 dias. Os dados ficam no celular, com backup criptografado.

Comecei usando sozinho, mandei para algumas pessoas próximas e agora ele está disponível de graça, com o código aberto. Vou continuar melhorando conforme o uso.

Download: https://github.com/matssgit/fluxa-app/releases/latest
Código: https://github.com/matssgit/fluxa-app

Testou? Me conta o que achou.

#Android #Kotlin #OpenSource

---

## Comentário fixado (opcional)

Para quem for testar: o Fluxa precisa de Android 11 ou superior e não está na Google Play. A instalação é pelo APK, baixado só da página oficial de Releases do GitHub. Antes de trocar de celular ou desinstalar, faça um backup em Mais → Backup e restauração; desinstalar apaga os dados.
