# 💰 Finance API

![Node.js](https://img.shields.io/badge/Node.js-339933?style=for-the-badge\&logo=nodedotjs\&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge\&logo=typescript\&logoColor=white)
![Fastify](https://img.shields.io/badge/Fastify-000000?style=for-the-badge\&logo=fastify\&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-003B57?style=for-the-badge\&logo=sqlite\&logoColor=white)
![Knex](https://img.shields.io/badge/Knex-D26B38?style=for-the-badge)
![Vitest](https://img.shields.io/badge/Vitest-6E9F18?style=for-the-badge\&logo=vitest\&logoColor=white)

API REST desenvolvida com **Node.js**, **TypeScript** e **Fastify** para gerenciamento de transações financeiras.

A aplicação permite registrar receitas e despesas, listar movimentações, consultar transações específicas e obter o saldo consolidado da sessão do usuário.

Este projeto foi desenvolvido com foco em boas práticas de desenvolvimento backend, validação de dados, organização de rotas, persistência em banco de dados e testes automatizados.

---

# 🚀 Tecnologias Utilizadas

* Node.js
* TypeScript
* Fastify
* SQLite
* Knex.js
* Zod
* Vitest
* Supertest

---

# 📋 Funcionalidades

## Criar Transação

Permite cadastrar uma nova movimentação financeira.

Tipos disponíveis:

* `credit` → Receita
* `debit` → Despesa

### Exemplo

```json
{
  "title": "Salário",
  "amount": 5000,
  "type": "credit"
}
```

---

## Listar Transações

Retorna todas as transações pertencentes à sessão atual.

```http
GET /transactions
```

---

## Buscar Transação por ID

Retorna uma transação específica.

```http
GET /transactions/:id
```

---

## Obter Resumo Financeiro

Calcula automaticamente o saldo da sessão.

```http
GET /transactions/summary
```

### Exemplo de resposta

```json
{
  "summary": {
    "amount": 3000
  }
}
```

---

# 🔗 Endpoints

| Método | Endpoint                | Descrição               |
| ------ | ----------------------- | ----------------------- |
| POST   | `/transactions`         | Criar transação         |
| GET    | `/transactions`         | Listar transações       |
| GET    | `/transactions/:id`     | Buscar transação por ID |
| GET    | `/transactions/summary` | Obter saldo consolidado |

---

# 🍪 Controle de Sessão

A aplicação utiliza cookies para identificar cada usuário.

Quando a primeira transação é criada:

1. Um UUID é gerado.
2. O UUID é armazenado em um cookie chamado `sessionId`.
3. As transações passam a ser vinculadas a essa sessão.

Dessa forma:

* Cada usuário acessa apenas seus próprios registros.
* Não é necessário realizar login.
* As rotas protegidas utilizam middleware para validação da sessão.

---

# 🏗 Estrutura do Projeto

```text
src
│
├── env
│   └── index.ts
│
├── middlewares
│   └── check-session-id-exists.ts
│
├── routes
│   └── transactions.ts
│
├── test
│   └── transactions.spec.ts
│
├── app.ts
├── database.ts
└── server.ts
```

---

# 🗄 Banco de Dados

Tabela: `transactions`

| Campo      | Tipo      |
| ---------- | --------- |
| id         | UUID      |
| session_id | UUID      |
| title      | TEXT      |
| amount     | INTEGER   |
| created_at | TIMESTAMP |

---

# 🔒 Validação de Dados

A aplicação utiliza **Zod** para validar:

### Body da requisição

```json
{
  "title": "string",
  "amount": 1000,
  "type": "credit"
}
```

### Parâmetros de rota

```json
{
  "id": "uuid"
}
```

Isso garante maior previsibilidade e segurança durante o processamento das requisições.

---

# 🧪 Testes Automatizados

O projeto possui testes de integração utilizando:

* Vitest
* Supertest

### Cenários testados

* Criar transação
* Listar transações
* Buscar transação por ID
* Obter resumo financeiro

Durante os testes:

* O banco é recriado automaticamente.
* As migrations são executadas novamente.
* Cada teste é executado de forma isolada.

---

# ⚙️ Instalação

Clone o projeto:

```bash
git clone <url-do-repositorio>
```

Entre na pasta:

```bash
cd finance-api
```

Instale as dependências:

```bash
npm install
```

---

# 🔧 Configuração

Crie um arquivo `.env` na raiz do projeto:

```env
PORT=3333
NODE_ENV=development
DATABASE_URL=./db/app.db
```

---

# 🛠 Executando as Migrations

```bash
npm run knex migrate:latest
```

---

# ▶️ Executando o Projeto

Modo desenvolvimento:

```bash
npm run dev
```

A API ficará disponível em:

```text
http://localhost:3333
```

---

# ✅ Executando os Testes

```bash
npm run test
```

---

# 📚 Conceitos Aplicados

* Desenvolvimento de APIs REST
* TypeScript
* Fastify
* SQLite
* Query Builder com Knex
* Cookies e Sessões
* Middlewares
* Migrations
* Validação de Dados com Zod
* Testes Automatizados
* Arquitetura Backend

---

# 👨‍💻 Autor

**Matheus Santana**

Desenvolvedor Full Stack em formação, com foco em Node.js, TypeScript, React e desenvolvimento de aplicações web modernas.

📚 Análise e Desenvolvimento de Sistemas — Universidade Católica de Santos (UniSantos)

🔗 GitHub: https://github.com/matssgit
