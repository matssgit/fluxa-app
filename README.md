# 💰 Finance App Beta

Uma aplicação full stack para gerenciamento de finanças pessoais, permitindo registrar receitas e despesas, visualizar transações e acompanhar o saldo financeiro de forma simples e intuitiva.

## 🚀 Sobre o Projeto

O **Finance App Beta** foi desenvolvido como um projeto de estudos para consolidar conhecimentos em desenvolvimento Full Stack utilizando tecnologias modernas do ecossistema JavaScript/TypeScript.

A aplicação permite:

* ✅ Registrar receitas
* ✅ Registrar despesas
* ✅ Listar transações
* ✅ Calcular saldo total
* ✅ Visualizar entradas e saídas
* ✅ Persistência de dados com banco SQLite
* ✅ Validação de dados
* ✅ API REST com TypeScript

---

## 📸 Preview

> Em breve: adicionar screenshots ou GIF da aplicação em funcionamento.

---

## 🛠️ Tecnologias Utilizadas

### Frontend

* React
* TypeScript
* Vite
* Tailwind CSS
* React Hooks
* Axios
* Zod

### Backend

* Node.js
* Fastify
* TypeScript
* Knex.js
* SQLite
* Zod
* Vitest

---

## 📂 Estrutura do Projeto

```bash
finance-app-beta/
├── frontend/
├── backend/
└── render.yaml
```

### Frontend

Responsável pela interface do usuário.

Principais módulos:

```bash
src/
├── components/
├── hooks/
├── pages/
├── services/
├── schemas/
├── types/
└── utils/
```

### Backend

Responsável pelas regras de negócio e persistência de dados.

Principais módulos:

```bash
src/
├── routes/
├── middlewares/
├── env/
├── test/
└── database/
```

---

## ⚙️ Como Executar Localmente

### Clone o repositório

```bash
git clone https://github.com/seu-usuario/finance-app-beta.git
```

```bash
cd finance-app-beta
```

---

## Backend

Entre na pasta:

```bash
cd backend
```

Instale as dependências:

```bash
npm install
```

Configure as variáveis de ambiente:

```bash
cp .env.example .env
```

Execute as migrations:

```bash
npm run knex migrate:latest
```

Inicie o servidor:

```bash
npm run dev
```

Servidor disponível em:

```bash
http://localhost:3333
```

---

## Frontend

Entre na pasta:

```bash
cd frontend
```

Instale as dependências:

```bash
npm install
```

Inicie o projeto:

```bash
npm run dev
```

Aplicação disponível em:

```bash
http://localhost:5173
```

---

## 🧪 Testes

Para executar os testes do backend:

```bash
npm test
```

ou

```bash
npm run test
```

---

## 📚 Aprendizados

Durante o desenvolvimento deste projeto foram praticados conceitos como:

* Arquitetura Full Stack
* Consumo de APIs REST
* Gerenciamento de estado com React Hooks
* Validação de dados com Zod
* Persistência de dados com SQLite
* Migrations com Knex
* Tipagem estática com TypeScript
* Testes automatizados
* Organização de código em camadas

---

## 🔮 Próximas Funcionalidades

* [ ] Autenticação de usuários
* [ ] Dashboard com gráficos
* [ ] Filtros por período
* [ ] Categorias personalizadas
* [ ] Exportação de relatórios
* [ ] Banco de dados PostgreSQL
* [ ] Deploy em produção

---

## 👨🏾‍💻 Autor

**Matheus Santana**

Desenvolvedor Full Stack em formação, focado em React, Node.js e TypeScript.

LinkedIn: linkedin.com/in/matheussantanadev

GitHub: github.com/matssgit

---

## 📄 Licença

Este projeto foi desenvolvido para fins de estudo e portfólio.
