````markdown
# Fluxa

<div align="center">
  <img src="https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB" alt="React" />
  <img src="https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white" alt="Node.js" />
  <img src="https://img.shields.io/badge/Fastify-000000?style=for-the-badge&logo=fastify&logoColor=white" alt="Fastify" />
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
</div>

<br>

<div align="center">
  <h3>🚀 <strong>Deploy:</strong> <a href="https://COLOQUE-SEU-LINK-AQUI.com">Acessar a Aplicação ao Vivo</a></h3>
</div>

<br>

**Fluxa** é uma aplicação Full Stack de gestão financeira pessoal e patrimonial desenvolvida do zero. O sistema vai muito além de um simples rastreador de despesas, unificando o controle de fluxo de caixa, cartões de crédito, compras parceladas, carteiras de metas e assinaturas recorrentes em uma única plataforma moderna e segura.

![Fluxa Demo](./docs/assets/demo.gif)
_(Substitua o caminho acima por um GIF do sistema em uso)_

---

## 🎯 Objetivo do Projeto

O Fluxa nasceu da necessidade de ter uma ferramenta financeira realmente simples, rápida e confiável para o controle das finanças pessoais. Ao longo do desenvolvimento, o projeto deixou de ser apenas um gerenciador financeiro e passou a servir também como laboratório para aplicação de boas práticas de arquitetura de software, autenticação, segurança, banco de dados e experiência do usuário (UX).

O projeto foi construído com dois propósitos principais:

- **Demonstrar capacidade técnica:** Construir uma aplicação robusta que vá muito além de um CRUD tradicional. O projeto explora arquitetura limpa, modelagem de domínio complexa, transações ACID, consistência de dados, testes automatizados e desenvolvimento de uma interface responsiva focada em atrito zero.
- **Resolver uma necessidade real:** Criar uma solução financeira diária sem anúncios, assinaturas obrigatórias ou funcionalidades desnecessárias, adaptada à realidade do controle financeiro.

---

## 🧠 Destaques Técnicos

O Fluxa se diferencia por resolver problemas críticos de engenharia financeira de forma estruturada:

- **Isolamento de Dados (Ownership):** O backend determina a propriedade dos dados exclusivamente através da identidade criptografada no token JWT (`user_id`). A API valida internamente o escopo de cada requisição, garantindo proteção contra ataques IDOR (Insecure Direct Object Reference).
- **Separação de Domínios (Caixa vs. Crédito):** A arquitetura blinda o dinheiro real (saldo) do dinheiro virtual (limite). Compras no crédito geram obrigações temporais, e o fluxo de caixa só sofre impacto no momento exato do pagamento da fatura/parcela.
- **Precisão Monetária (Centavos):** Os valores monetários são normalizados e persistidos em centavos inteiros no banco de dados, evitando problemas críticos de precisão com _floating points_. Algoritmos determinísticos garantem que o arredondamento de dízimas em parcelamentos bata exatamente com o valor original.
- **Controle de Concorrência (ACID):** Operações críticas, como a dedução do limite de um cartão ou aportes em carteiras, são envolvidas em transações SQL isoladas via Knex. Isso previne _race conditions_ em requisições simultâneas.
- **Validação _Fail-Fast_:** A biblioteca Zod valida e normaliza os payloads no momento em que entram na rota, rejeitando requisições malformadas antes que alcancem as camadas de regra de negócio.

---

## ✨ Funcionalidades

### 🔐 Segurança

- **Autenticação JWT:** Cadastro, Login e verificação segura de sessões.
- **Autenticação em Dois Fatores (2FA):** Camada adicional de segurança via aplicativo autenticador.
- **Recuperação de Acesso:** Fluxo completo de redefinição de senha com tokens de e-mail limitados por tempo.

### 💰 Fluxo de Caixa

- **Contas e Categorias:** Gestão de múltiplas contas bancárias e categorização personalizada (CRUD completo).
- **Transações:** Registro de movimentações com status dinâmico (Pendente / Concluído).

### 💳 Cartões e Crédito

- **Controle de Limites:** Acompanhamento automático do limite total e disponível.
- **Parcelamentos:** Motor de projeção de parcelas futuras que se ajustam organicamente ao calendário.

### 🔄 Assinaturas e 🎯 Carteiras

- **Assinaturas:** Acompanhamento de serviços recorrentes com aviso de data da próxima cobrança.
- **Wallets (Metas):** Organização de liquidez através de transferências diretas entre a conta corrente e carteiras de objetivo patrimonial.

### 📊 Dashboard e UX

- **Inteligência Financeira:** Painel central com indicadores de saldo, despesas e distribuição por categorias atualizados em tempo real (React Query).
- **Modo Privacidade:** Censura rápida de valores financeiros sensíveis em tela.
- **Design Responsivo:** Layout _Mobile-First_ projetado para uso ergonômico, com suporte nativo a _Light_ e _Dark Mode_.

---

## 📸 Demonstração Visual

### Dashboard Executivo

![Dashboard](./docs/assets/dashboard.png)
_(Substitua o caminho acima por uma screenshot do seu Dashboard)_

### Gestão de Cartões e Parcelamentos

![Cartões](./docs/assets/cartoes.png)
_(Substitua o caminho acima por uma screenshot da tela de cartões)_

---

## 🛠️ Stack Tecnológica

A arquitetura Cliente-Servidor foi desenvolvida mantendo as frentes desacopladas, limpas e com responsabilidades bem definidas.

### Frontend

- **Core:** React, TypeScript, Vite.
- **Estilização:** Tailwind CSS (Design System Pine & Sage customizado).
- **Gestão de Estado & Cache:** TanStack Query (React Query).
- **Componentes & Ícones:** Modais nativos, Lucide React.

### Backend

- **Core:** Node.js, Fastify, TypeScript.
- **Banco de Dados:** PostgreSQL (Produção), SQLite (Ambiente Local/Dev).
- **Query Builder:** Knex.js.
- **Validação & Segurança:** Zod, JWT, Bcrypt.
- **Testes:** Vitest (Testes de concorrência, ownership e hardening).

### Infraestrutura

- **Deploy:** Vercel (Frontend) & Render/Railway (Backend).
- **Contêineres:** Docker & Docker Compose.

---

## 🚀 Como Executar Localmente (Quick Start)

O ambiente de desenvolvimento local foi configurado para ser executado de maneira simples utilizando o **SQLite** como banco de dados padrão para evitar a necessidade imediata de containers pesados no seu primeiro teste.

**Pré-requisitos:** Node.js (v18+) e NPM/Yarn instalados.

**1. Clone o repositório:**

```bash
git clone [https://github.com/matssgit/fluxa.git](https://github.com/matssgit/fluxa.git)
cd fluxa
```
````

**2. Configure e inicie o Backend:**

```bash
cd backend

# Instale as dependências
npm install

# Crie o arquivo de variáveis de ambiente
cp .env.example .env
# IMPORTANTE: Abra o arquivo .env e certifique-se de que a variável JWT_SECRET está preenchida com qualquer hash seguro.

# Execute as migrations para gerar a estrutura do banco SQLite local
npm run knex -- migrate:latest

# (Opcional) Popule o banco com dados/categorias base para testes
npm run knex -- seed:run

# Inicie o servidor da API (rodará em http://localhost:3333)
npm run dev

```

**3. Configure e inicie o Frontend:**
Em um novo terminal, retorne para a pasta raiz e acesse o frontend:

```bash
cd frontend

# Instale as dependências
npm install

# Inicie o servidor de desenvolvimento
npm run dev

```

Acesse `http://localhost:5173` no seu navegador para utilizar a aplicação.

**4. Executando os Testes Automatizados:**
Para atestar a garantia do isolamento de segurança e cálculos do sistema, acesse a pasta do backend e execute a suíte de testes:

```bash
cd backend
npm run test

```

---

## 📚 Documentação Técnica

O Fluxa conta com farta documentação técnica. Para compreender a fundo as decisões de design, _feature slicing_ e regras de negócio, consulte a pasta `docs/` do repositório:

- 🏛️ [Arquitetura do Sistema](https://www.google.com/search?q=./docs/architecture/SYSTEM_ARCHITECTURE.md)
- 🎨 [Design System](https://www.google.com/search?q=./docs/architecture/DESIGN_SYSTEM.md)
- ⚙️ [Roadmap do Backend](https://www.google.com/search?q=./docs/development/BACKEND_ROADMAP.md)
- 💻 [Roadmap do Frontend](https://www.google.com/search?q=./docs/development/FRONTEND_ROADMAP.md)

---

## 👨‍💻 Desenvolvido Por

O Fluxa coroa meu esforço de consolidação técnica em Engenharia de Software. Este projeto consolida a união entre uma interface front-end focada em uma ótima experiência para o usuário final e o desenvolvimento de um back-end sólido, seguro, testado e escalável.

**Matheus Santana**

Desenvolvedor Full Stack

- 🐙 **GitHub:** [@matssgit](https://github.com/matssgit)
- 💼 **LinkedIn:** [Matheus Santana](https://www.google.com/search?q=https://linkedin.com/in/matheussantanadev)

---
