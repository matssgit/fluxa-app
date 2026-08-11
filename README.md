# Fluxa

<div align="center">

<img src="https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB" alt="React" />
<img src="https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
<img src="https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white" alt="Node.js" />
<img src="https://img.shields.io/badge/Fastify-000000?style=for-the-badge&logo=fastify&logoColor=white" alt="Fastify" />
<img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />

<br />
<br />

**Aplicação Full Stack de gestão financeira pessoal e patrimonial.**

Controle seu fluxo de caixa, cartões, parcelamentos, assinaturas e metas financeiras em uma única plataforma.

<br />

<a href="COLOQUE-SEU-LINK-AQUI">
  <strong>🚀 Acessar aplicação</strong>
</a>

</div>

---

## 📌 Sobre o projeto

O **Fluxa** é uma aplicação Full Stack de gestão financeira pessoal desenvolvida do zero com foco em **engenharia de software, segurança, consistência de dados e experiência do usuário**.

A ideia surgiu da necessidade de ter uma ferramenta financeira simples e completa para o uso cotidiano, mas o projeto evoluiu para também funcionar como um laboratório prático de desenvolvimento de software.

Além do controle tradicional de receitas e despesas, o Fluxa trabalha com conceitos como:

* Contas bancárias e fluxo de caixa
* Cartões de crédito
* Compras parceladas
* Assinaturas recorrentes
* Carteiras financeiras
* Metas e organização patrimonial
* Autenticação em dois fatores
* Recuperação segura de acesso
* PWA e experiência mobile-first

---

## 🎥 Demonstração

### Dashboard

![Fluxa - Dashboard](./docs/assets/caixa.png)

### Autenticação

![Fluxa - Login](./docs/assets/login.png)

### Transações

![Fluxa - Transações](./docs/assets/baixa.png)

### Assinaturas

![Fluxa - Assinaturas](./docs/assets/assinaturas.png)

### Metas e Carteiras

![Fluxa - Metas](./docs/assets/metas.png)

---

## ⚙️ Principais funcionalidades

### 💰 Gestão financeira

* **Fluxo de caixa** para receitas e despesas
* **Múltiplas contas bancárias**
* **Categorias personalizadas**
* Controle de transações **pendentes e concluídas**
* **Cartões de crédito**
* Compras **parceladas**
* Projeção de **faturas futuras**
* **Assinaturas recorrentes**
* **Carteiras financeiras** para separar patrimônio
* **Metas financeiras**

### 🔐 Segurança

* Autenticação baseada em **JWT**
* Validação de payloads com **Zod**
* **Bcrypt** para proteção de credenciais
* **Rate limiting** contra abuso de endpoints
* Headers de segurança com **Helmet**
* **2FA via TOTP**
* **Recovery Codes**
* Recuperação de senha através de tokens seguros enviados por e-mail
* Isolamento de dados baseado na identidade autenticada do usuário

---

## 🧠 Engenharia de software

Uma das principais propostas do Fluxa é aplicar conceitos de engenharia de software em um projeto real, indo além de um simples CRUD.

### 💵 Precisão monetária

Valores financeiros são armazenados como **inteiros representando centavos**, evitando problemas de precisão relacionados ao uso de `floating point`.

O sistema também utiliza algoritmos determinísticos para distribuir valores de compras parceladas, garantindo que a soma das parcelas corresponda exatamente ao valor original.

### 🔄 Transações e consistência

Operações críticas são executadas dentro de **transações SQL**, garantindo atomicidade e consistência dos dados.

Isso é utilizado, por exemplo, em operações envolvendo:

* Limite de cartões
* Compras parceladas
* Aportes em carteiras
* Atualizações financeiras relacionadas

O objetivo é evitar inconsistências causadas por requisições concorrentes.

### 🔒 Isolamento de dados

O backend determina a propriedade dos recursos através da identidade autenticada presente no **JWT**.

IDs enviados pelo cliente não são utilizados como mecanismo de autorização.

Isso ajuda a prevenir vulnerabilidades de **BOLA/IDOR**, evitando que um usuário consiga acessar recursos pertencentes a outra conta simplesmente alterando um identificador na requisição.

### 💳 Separação entre caixa e crédito

O Fluxa diferencia o **dinheiro disponível** do **limite de crédito**.

Uma compra realizada no cartão não reduz diretamente o saldo da conta bancária. Ela gera uma obrigação futura que será refletida no fluxo de caixa somente quando a respectiva fatura for efetivamente paga.

### 🧪 Testes automatizados

A API possui uma suíte de testes utilizando **Vitest**, cobrindo principalmente:

* Autenticação
* Fluxos de segurança
* 2FA
* Integridade das operações financeiras
* Regras de parcelamento
* Cenários de erro

**31 testes automatizados** atualmente.

---

## 🎨 Interface e experiência

### 📱 Mobile-first

A interface foi desenvolvida para funcionar desde dispositivos móveis de aproximadamente **360px até telas desktop de 1920px**.

### 📲 PWA

O Fluxa pode ser instalado como **Progressive Web App**, proporcionando uma experiência semelhante à de uma aplicação nativa.

### 🌲 Pine & Sage Design System

O projeto possui um design system próprio desenvolvido com **Tailwind CSS**, incluindo:

* Light Mode
* Dark Mode
* Componentes reutilizáveis
* Tipografia e espaçamento padronizados
* Alta legibilidade
* Interface responsiva

### 👁️ Modo privacidade

Um modo global permite ocultar rapidamente valores financeiros da interface através de **React Context**, útil para situações em que o usuário está utilizando o sistema em ambientes públicos.

### ⚡ Interface reativa

As operações assíncronas são gerenciadas com **TanStack Query**, permitindo atualizações rápidas da interface e gerenciamento eficiente de cache e estados de requisição.

---

## 🛠️ Stack tecnológica

### Frontend

| Tecnologia          | Utilização              |
| ------------------- | ----------------------- |
| **React**           | Interface               |
| **TypeScript**      | Tipagem estática        |
| **Vite**            | Build e desenvolvimento |
| **Tailwind CSS**    | Estilização             |
| **TanStack Query**  | Server state e cache    |
| **React Hook Form** | Formulários             |
| **Zod**             | Validação               |
| **Lucide React**    | Ícones                  |
| **PWA**             | Experiência instalável  |

### Backend

| Tecnologia             | Utilização                 |
| ---------------------- | -------------------------- |
| **Node.js**            | Runtime                    |
| **Fastify**            | Framework HTTP             |
| **TypeScript**         | Tipagem estática           |
| **PostgreSQL**         | Banco de produção          |
| **SQLite**             | Desenvolvimento local      |
| **Knex.js**            | Query builder e migrations |
| **Zod**                | Validação                  |
| **JWT**                | Autenticação               |
| **Bcrypt**             | Hash de senhas             |
| **Fastify Rate Limit** | Proteção contra abuso      |
| **Helmet**             | Headers de segurança       |
| **Vitest**             | Testes automatizados       |

### Infraestrutura

* **Vercel** — Frontend
* **Render** — Backend
* **Docker / Docker Compose** — Ambiente local
* **Brevo SMTP + Nodemailer** — E-mails transacionais

---

## 🏗️ Arquitetura

O Fluxa utiliza uma arquitetura **cliente-servidor**, mantendo frontend e backend desacoplados.

```text
┌──────────────────────┐
│       Frontend       │
│ React + TypeScript   │
│ Vite + Tailwind      │
└──────────┬───────────┘
           │ HTTP / JSON
           ▼
┌──────────────────────┐
│        Backend       │
│ Node.js + Fastify    │
│ TypeScript + Zod     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│      PostgreSQL      │
│      Produção        │
└──────────────────────┘
```

Para desenvolvimento local, o projeto utiliza **SQLite**, permitindo executar a aplicação sem depender de uma instalação local do PostgreSQL.

---

## 🚀 Como executar localmente

### Pré-requisitos

* Node.js **18+**
* npm

### 1. Clone o repositório

```bash
git clone https://github.com/matssgit/fluxa.git
cd fluxa
```

### 2. Configure o Backend

```bash
cd backend

npm install

cp .env.example .env
```

Abra o arquivo `.env` e configure as variáveis necessárias, principalmente:

```env
JWT_SECRET=seu_secret_seguro
```

Execute as migrations:

```bash
npm run knex -- migrate:latest
```

Inicie o servidor:

```bash
npm run dev
```

A API estará disponível em:

```text
http://localhost:3333
```

### 3. Configure o Frontend

Em outro terminal:

```bash
cd frontend

npm install
npm run dev
```

A aplicação estará disponível em:

```text
http://localhost:5173
```

---

## 📚 Documentação

O projeto possui documentação técnica complementar na pasta `docs/`.

* [🏛️ Arquitetura do Sistema](./docs/architecture/SYSTEM_ARCHITECTURE.md)
* [🎨 Design System](./docs/architecture/DESIGN_SYSTEM.md)
* [⚙️ Roadmap do Backend](./docs/development/BACKEND_ROADMAP.md)
* [💻 Roadmap do Frontend](./docs/development/FRONTEND_ROADMAP.md)

---

## 👨‍💻 Desenvolvedor

**Matheus Santana**

Desenvolvedor Full Stack com foco em **JavaScript, TypeScript, Node.js e React**.

O Fluxa representa a consolidação prática de conhecimentos em desenvolvimento Full Stack, arquitetura de software, APIs, bancos de dados, segurança, testes automatizados e desenvolvimento de interfaces.

<div align="left">

🐙 **GitHub:** [@matssgit](https://github.com/matssgit)

💼 **LinkedIn:** [Matheus Santana](https://linkedin.com/in/matheussantanadev)

</div>

---

<div align="center">

**Fluxa — Gestão financeira simples, segura e completa.**

</div>
