# 💰 Finance App (Beta) — Sistema de Controle Financeiro

Uma aplicação web completa de controle financeiro pessoal, desenvolvida no modelo **Monorepo**. O projeto conta com uma API REST robusta seguindo regras de imutabilidade arquitetural e um frontend moderno e responsivo, totalmente integrados em ambientes de produção na nuvem.

🚀 **Link do Frontend (Vercel):** [https://finance-app-beta-ijfg.vercel.app](https://finance-app-beta-ijfg.vercel.app)  
⚙️ **Link do Backend (Render):** [https://finance-app-beta-2.onrender.com](https://finance-app-beta-2.onrender.com)

---

## 🏗️ Arquitetura e Decisões de Projeto

### 🛑 Regra de Ouro: Imutabilidade Financeira
Uma das principais decisões arquiteturais deste projeto foi a **não implementação de rotas de edição (`PUT`) ou exclusão (`DELETE`)** no ledger de transações. Em sistemas financeiros e contábeis reais, a auditabilidade é crucial. Se uma transação foi registrada incorretamente, a correção deve ser feita através de um **estorno** (uma nova transação inversa de crédito ou débito), garantindo o histórico real e a consistência da conta.

### 🔐 Autenticação Sem Estado (Stateful Cookies)
A aplicação gerencia sessões de forma transparente utilizando cookies identificadores exclusivos (`sessionId`) gerados via UUID no primeiro acesso (`POST /transactions`). 
- **Desafio de Produção Superado:** Como o frontend e o backend estão hospedados em domínios diferentes (Vercel e Render), a API foi configurada milimetricamente para lidar com políticas restritas de navegadores modernos, implementando `sameSite: "none"`, `secure: true` e comunicação Cross-Origin via Axios com `withCredentials: true`.

---

## 🛠️ Tecnologias Utilizadas

### **Backend (`/backend`)**
- **Node.js** & **TypeScript**
- **Fastify:** Framework focado em performance e baixo overhead.
- **Knex.js:** Query builder para construção e controle de versão do banco de dados (Migrations e Seeds).
- **PostgreSQL:** Banco de dados relacional robusto utilizado em produção.
- **Zod:** Validação estrita de esquemas e tipos em tempo de execução.
- **Tsup:** Compilação rápida de código TypeScript para CommonJS (`.cjs`).

### **Frontend (`/frontend`)**
- **React.js** & **TypeScript**
- **Vite:** Ferramenta de build ultra rápida.
- **Axios:** Cliente HTTP configurado com interceptors globais para tratamento elegante de erros (400, 401, 404, 500).
- **Tailwind CSS:** Estilização utilitária moderna e responsiva.

---

## 🛣️ Rotas da API (Backend)

| Método | Rota | Pré-requisito | Descrição |
| :--- | :--- | :--- | :--- |
| `POST` | `/transactions` | Nenhum | Cria uma transação (gera o cookie de sessão se for o primeiro acesso). |
| `GET` | `/transactions` | `sessionId` válido | Lista todas as transações pertencentes àquela sessão ativa. |
| `GET` | `/transactions/:id` | `sessionId` válido | Busca os detalhes de uma transação específica por UUID. |
| `GET` | `/transactions/summary` | `sessionId` válido | Retorna o resumo (saldo total acumulado) das transações da sessão. |

---

## ⚙️ Como Executar o Projeto Localmente

### Pré-requisitos
- Node.js (Versão v18 ou superior)
- Um banco de dados PostgreSQL (ou altere o `client` no `knexfile` para `sqlite3` se quiser testar localmente em arquivo).

### 1. Clonar o Repositório
```bash
git clone [https://github.com/matssgit/finance-app-beta.git](https://github.com/matssgit/finance-app-beta.git)
cd finance-app-beta
