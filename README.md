# Fluxa

Aplicação full stack de gestão financeira pessoal para acompanhar fluxo de caixa, contas, cartões, assinaturas e metas em uma visão consolidada, com segurança e uma experiência pensada para o uso cotidiano.

[Aplicação online](https://fluxa-core-app-five.vercel.app/) · [Portfólio](https://matheusantanadev.vercel.app/)

## Visão do produto

<p align="center">
  <img src="./docs/assets/2-Dashboard.png" alt="Dashboard do Fluxa" width="760" />
</p>

<table>
  <tr>
    <td width="50%"><img src="./docs/assets/3-Caixa%20Central.png" alt="Caixa Central" width="100%" /></td>
    <td width="50%"><img src="./docs/assets/4-Cart%C3%B5es.png" alt="Cartões de crédito" width="100%" /></td>
  </tr>
  <tr>
    <td align="center"><strong>Caixa Central</strong></td>
    <td align="center"><strong>Cartões</strong></td>
  </tr>
  <tr>
    <td width="50%"><img src="./docs/assets/5-Assinaturas.png" alt="Assinaturas" width="100%" /></td>
    <td width="50%"><img src="./docs/assets/6-Metas%20e%20Objetivos.png" alt="Metas e objetivos" width="100%" /></td>
  </tr>
  <tr>
    <td align="center"><strong>Assinaturas</strong></td>
    <td align="center"><strong>Metas</strong></td>
  </tr>
  <tr>
    <td width="50%"><img src="./docs/assets/7-Categorias.png" alt="Categorias" width="100%" /></td>
    <td width="50%"><img src="./docs/assets/8-Seguranca%20%202FA.png" alt="Segurança e autenticação em duas etapas" width="100%" /></td>
  </tr>
  <tr>
    <td align="center"><strong>Categorias</strong></td>
    <td align="center"><strong>Segurança e 2FA</strong></td>
  </tr>
  <tr>
    <td width="50%"><img src="./docs/assets/1-Login.png" alt="Login" width="100%" /></td>
    <td width="50%"><img src="./docs/assets/9-Cadastro%20%2B%20verificacao%20por%20e-mail.png" alt="Cadastro e verificação por e-mail" width="100%" /></td>
  </tr>
  <tr>
    <td align="center"><strong>Login</strong></td>
    <td align="center"><strong>Cadastro e verificação</strong></td>
  </tr>
</table>

## Sobre o projeto

O Fluxa começou como um projeto de estudo de CRUD financeiro e evoluiu para uma aplicação full stack mais completa. Hoje, o projeto combina consistência financeira, segurança de sessão, tratamento de concorrência e uma interface responsiva para organizar decisões financeiras sem fragmentar os dados em ferramentas diferentes.

A aplicação mantém separadas as responsabilidades de caixa, crédito, recorrência e objetivos financeiros, mas reúne esses domínios em uma leitura única no dashboard.

## Funcionalidades

### Finanças

- contas, receitas e despesas;
- histórico financeiro unificado no Caixa Central;
- categorias personalizadas;
- lançamentos pendentes e concluídos;
- dashboard consolidado com saldo, receitas, despesas e projeções.

### Cartões

- cadastro e acompanhamento de cartões;
- controle de limite total e disponível;
- compras à vista ou parceladas;
- vencimentos e pagamento de parcelas;
- cancelamento com recomposição consistente do limite.

### Assinaturas

- acompanhamento de gastos recorrentes;
- pagamentos associados à competência mensal;
- proteção contra pagamentos duplicados;
- visão de próximos vencimentos.

### Metas

- wallets para metas e objetivos;
- depósitos e retiradas;
- histórico de movimentações;
- acompanhamento visual do progresso.

### Segurança

- autenticação com JWT;
- confirmação de e-mail;
- recuperação de senha com token de uso único;
- revogação de sessões por versão do token;
- autenticação em duas etapas com TOTP;
- recovery codes;
- isolamento de dados entre usuários;
- limpeza do cache da aplicação ao encerrar ou invalidar uma sessão.

### Experiência

- interface responsiva;
- temas claro e escuro;
- modo de privacidade para ocultar valores;
- PWA instalável;
- navegação adaptada para mobile e desktop.

## Decisões técnicas

- **PostgreSQL como banco oficial:** desenvolvimento, testes e produção utilizam o mesmo mecanismo relacional.
- **Knex e migrations:** evolução do schema versionada e aplicada em ordem explícita.
- **Modelo monetário canônico:** novos lançamentos persistem o valor como magnitude não negativa e a direção em <code>type</code> (<code>entrada</code> ou <code>saida</code>).
- **Compatibilidade controlada:** registros legados sem tipo mantêm fallback isolado, sem influenciar novos dados.
- **Idempotência em assinaturas:** uma competência não pode receber o mesmo pagamento mais de uma vez.
- **Concorrência protegida:** operações críticas de cartões, parcelas, cancelamentos e wallets usam transações e locking.
- **Revogação de JWT:** <code>token_version</code> invalida sessões anteriores após eventos sensíveis.
- **Reset seguro:** tokens de redefinição são armazenados como hash SHA-256 e consumidos uma única vez.
- **Cache entre sessões:** o QueryClient e dados locais associados ao usuário são limpos na troca de sessão.
- **Validação de entrada:** contratos HTTP e filtros usam Zod.
- **Testes automatizados:** os fluxos financeiros e de autenticação são exercitados contra PostgreSQL dedicado a testes.

## Arquitetura

~~~text
Frontend
React + TypeScript + Vite + Tailwind CSS + TanStack Query
        │
        │ HTTPS / JSON
        ▼
Backend
Node.js + Fastify + TypeScript + Zod + Knex
        │
        ▼
PostgreSQL

Vercel ── frontend
Render ── backend
~~~

## Stack

| Camada | Tecnologias principais |
| --- | --- |
| Frontend | React, TypeScript, Vite, Tailwind CSS, TanStack Query |
| Backend | Node.js, Fastify, TypeScript, Zod, Knex |
| Banco | PostgreSQL |
| Autenticação | JWT, bcrypt, TOTP |
| Testes | Vitest e test runner nativo do Node.js |
| Infraestrutura | Vercel, Render, Docker Compose, Brevo SMTP |

## Testes

O projeto possui **65 testes automatizados passando**:

- **64 testes backend** com Vitest;
- **1 teste frontend** para a atualização consistente do histórico financeiro.

A cobertura inclui autenticação, 2FA, revogação de sessão, ownership, hardening, integridade financeira, concorrência, cartões, assinaturas, wallets e histórico do caixa.

Comandos principais:

~~~bash
# Backend
cd backend
npm test -- --run

# Frontend
cd frontend
npm test
npm run lint
npm run build
~~~

## PWA

O Fluxa pode ser instalado como Progressive Web App. O frontend gera manifest e service worker durante o build, inclui ícones dedicados e oferece uma experiência adequada para dispositivos móveis.

Dados financeiros autenticados não usam runtime caching no service worker, evitando que respostas privadas sejam reutilizadas indevidamente.

## Rodando localmente

### Requisitos

- Node.js 18 ou superior;
- npm;
- PostgreSQL 16 ou compatível;
- Docker e Docker Compose, caso prefira usar o banco disponibilizado pelo projeto.

### Instalação

~~~bash
git clone https://github.com/matssgit/fluxa-app.git
cd fluxa-app

cd backend
npm install

cd ../frontend
npm install
~~~

### Backend

1. Inicie uma instância PostgreSQL. O projeto inclui [docker-compose.yml](./backend/docker-compose.yml).
2. Crie <code>backend/.env</code> com as variáveis necessárias e uma <code>DATABASE_URL</code> apontando para o banco local.
3. Aplique as migrations e inicie a API:

~~~bash
cd backend
docker compose up -d
npm run migrate:latest
npm run dev
~~~

A API utiliza a porta <code>3333</code> por padrão.

### Frontend

Crie <code>frontend/.env.local</code> com <code>VITE_API_URL</code> apontando para a API local e execute:

~~~bash
cd frontend
npm run dev
~~~

O Vite utiliza a porta <code>5173</code> por padrão.

### Variáveis de ambiente

Backend:

~~~text
DATABASE_URL
DATABASE_CLIENT
JWT_SECRET
FRONTEND_URL
CORS_ORIGIN
SMTP_HOST
SMTP_PORT
SMTP_USER
SMTP_PASSWORD
EMAIL_FROM
DEMO_CLEANUP_ENABLED
DEMO_DATA_RETENTION_DAYS
~~~

Frontend:

~~~text
VITE_API_URL
~~~

Nenhum secret deve ser versionado. A limpeza automática de contas demo permanece desativada por padrão e deve ser habilitada somente de forma deliberada.

## Documentação

- [Arquitetura do sistema](./docs/architecture/SYSTEM_ARCHITECTURE.md)
- [Design system](./docs/architecture/DESIGN_SYSTEM.md)
- [Checklist de QA](./docs/development/QA_CHECKLIST.md)
- [Histórico de sprints](./docs/development/SPRINTS.md)

## Status

**Projeto ativo — versão estável de portfólio.**

Uma versão demo pública está disponível em [fluxa-core-app-five.vercel.app](https://fluxa-core-app-five.vercel.app/).

## Autor

**Matheus Santana**

- [Portfólio](https://matheusantanadev.vercel.app/)
- [GitHub](https://github.com/matssgit)
- [LinkedIn](https://linkedin.com/in/matheussantanadev)
