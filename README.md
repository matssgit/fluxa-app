# 💰 Finance App - Arquitetura Oficial do Projeto

## Objetivo do Projeto

Construir um aplicativo financeiro moderno para uso pessoal e familiar inspirado em Mobills, Organizze e similares.

O sistema deverá permitir:

- Controle de receitas e despesas
- Gestão de contas financeiras
- Controle de cartões de crédito
- Compras parceladas
- Lançamentos futuros
- Central de pendências
- Recorrências
- Dashboard financeiro
- Backup e restauração

O projeto também servirá como portfólio Full Stack.

Stack principal:

### Backend

- Node.js
- Fastify
- TypeScript
- Knex
- MySQL
- JWT
- Zod

### Frontend

- React
- TypeScript
- React Query
- React Hook Form
- Zod
- TailwindCSS

---

# PRINCÍPIO ARQUITETURAL

O sistema é dividido em dois domínios completamente separados.

## Domínio 1 - Fluxo de Caixa

Responsável pelo dinheiro real.

Exemplos:

- Salário
- PIX
- Dinheiro
- Conta Corrente
- Poupança
- Investimentos
- Pagamento de contas
- Transferências

Tudo que altera saldo de contas pertence ao domínio de fluxo de caixa.

---

## Domínio 2 - Cartões de Crédito

Responsável por compromissos futuros.

Exemplos:

- Compras parceladas
- Compras à vista no crédito
- Controle de limite
- Parcelas futuras

Uma compra no cartão NÃO altera imediatamente o saldo da conta.

Ela gera compromissos futuros.

---

# MODELAGEM DE DADOS

## users

Responsável pelos usuários do sistema.

Campos:

```text
id
name
email
password_hash
created_at
```

Relacionamentos:

```text
1 usuário
├─ N contas
├─ N categorias
├─ N transações
├─ N cartões
└─ N compras de cartão
```

---

# accounts

Representa contas financeiras.

Exemplos:

- Carteira
- Nubank
- Inter
- Santander
- Poupança

Campos:

```text
id
user_id
name
type
created_at
```

Tipos:

```text
wallet
checking
savings
investment
```

---

# categories

Categorias utilizadas em transações e compras.

Campos:

```text
id
user_id
name
type
color
icon
is_default
created_at
```

Tipos:

```text
income
expense
```

Exemplo:

```text
Mercado
🛒
#16a34a
```

---

# transactions

Representa movimentações reais de dinheiro.

Esta é a tabela mais importante do sistema.

Campos:

```text
id
user_id
account_id
category_id

title
description
observation

amount

status

expected_date
completed_date

created_at
```

Status:

```text
pending
completed
ignored
```

Exemplos:

```text
Salário
PIX recebido
Conta de água
Aluguel
Pagamento de fatura
```

IMPORTANTE:

transactions NÃO possui card_id.

Cartões são tratados em outro domínio.

---

# cards

Representa cartões de crédito.

Campos:

```text
id
user_id

name
brand

credit_limit

created_at
```

Exemplos:

```text
Mercado Pago
Nubank
Inter
```

---

# credit_purchases

Compra principal realizada no cartão.

Campos:

```text
id

user_id
card_id
category_id

title
store

total_amount
total_installments

purchase_date

observation

created_at
```

Exemplo:

```text
Título:
iPhone 17 Pro

Loja:
Apple

Valor:
7200

Parcelas:
24
```

Esta tabela representa a compra mãe.

---

# installments

Parcelas geradas automaticamente.

Campos:

```text
id

purchase_id

installment_number
total_installments

amount

expected_date

status
```

Status:

```text
pending
paid
```

Exemplo:

```text
1/24
2/24
3/24
...
24/24
```

A representação visual 1/24 é montada pelo frontend.

Não armazenar strings como:

```text
"1/24"
```

---

# RECURRENCES (VERSÃO FUTURA)

Tabela reservada para recorrências.

Não implementar agora.

Campos previstos:

```text
id
user_id
account_id
category_id

title
amount

frequency

start_date
end_date

active
```

Exemplos:

```text
Netflix
Academia
Internet
Aluguel
```

---

# REGRAS DE NEGÓCIO

## Fluxo de Caixa

Saldo de uma conta:

```text
Entradas completed
-
Saídas completed
```

Pendências não afetam saldo.

---

## Cartões

Ao criar:

```text
iPhone
7200
24x
```

Sistema:

1. cria credit_purchase

2. calcula:

7200 / 24 = 300

3. gera:

24 registros em installments

---

# CENTRAL DE PENDÊNCIAS

Objetivo:

Mostrar tudo que ainda não foi concluído.

Fontes:

### transactions

status = pending

### installments

status = pending

Ordenação:

```text
expected_date ASC
```

---

# DASHBOARD

Objetivo:

Mostrar visão consolidada.

Indicadores:

## Saldo Total

Somatório de todas as contas.

---

## Saldo por Conta

Exemplo:

```text
Nubank
R$ 2.500

Carteira
R$ 300

Inter
R$ 1.200
```

---

## Receitas do Mês

Somatório de entradas completed.

---

## Despesas do Mês

Somatório de saídas completed.

---

## Gastos por Categoria

Exemplo:

```text
Mercado
Transporte
Tecnologia
Moradia
```

---

# TRANSFERÊNCIAS (FASE FUTURA)

Exemplo:

```text
Carteira
→
Nubank
```

A transferência:

não gera receita

não gera despesa

apenas movimentação interna.

---

# BACKUP

Endpoint:

```http
GET /backup
```

Retorna:

```json
{
   "accounts": [],
   "categories": [],
   "transactions": [],
   "cards": [],
   "creditPurchases": [],
   "installments": []
}
```

---

# RESTORE

Endpoint:

```http
POST /restore
```

Recebe:

```json
{
  ...
}
```

Utilizar estratégia de UPSERT.

---

# ROADMAP OFICIAL

## Fase 1

Autenticação

- Users
- JWT
- Login
- Cadastro
- Proteção de rotas

STATUS: CONCLUÍDA

---

## Fase 2

Contas e Categorias

- Accounts
- Categories
- CRUDs

STATUS: CONCLUÍDA

---

## Fase 3

Fluxo de Caixa

- Transactions
- Summary
- Pendências

STATUS: CONCLUÍDA

---

## Fase 4

Cartões e Parcelamentos

Implementar:

- Cards
- Credit Purchases
- Installments

Objetivo:

Cadastrar uma compra e gerar parcelas automaticamente.

STATUS: EM DESENVOLVIMENTO

---

## Fase 5

Recorrências

- Netflix
- Academia
- Internet
- Aluguel

STATUS: FUTURO

---

## Fase 6

Dashboard Avançado

- Gráficos
- Indicadores
- Relatórios

STATUS: FUTURO

---

## Fase 7

Transferências

STATUS: FUTURO

---

## Fase 8

Backup e Restore

STATUS: FUTURO

---

# REGRA DE OURO DO PROJETO

Antes de implementar qualquer funcionalidade nova:

1. Verificar se ela pertence ao domínio de Fluxo de Caixa ou Cartões.
2. Não misturar transactions com compras de cartão.
3. Não criar atalhos que gerem dívida técnica.
4. Priorizar simplicidade do MVP.
5. Não implementar funcionalidades de V2 sem necessidade.
6. Sempre atualizar este documento quando houver mudanças arquiteturais.

Este documento é a fonte oficial da arquitetura do projeto.

```
finance-app-beta - v2
├─ backend
│  ├─ .env
│  ├─ .env.example
│  ├─ .env.test
│  ├─ .env.test.example
│  ├─ .eslintignore
│  ├─ .eslintrc.json
│  ├─ db
│  │  └─ migrations
│  │     ├─ 20260616175526_create-transactions.ts
│  │     ├─ 20260616185406_add-session-id-to-transactions.ts
│  │     ├─ 20260622233134_create_users.ts
│  │     ├─ 20260622233138_add_user_id_to_transactions.ts
│  │     ├─ 20260623163630_create_accounts.ts
│  │     ├─ 20260623163747_create_categories.ts
│  │     ├─ 20260623175309_add_account_and_category_to_transactions.ts
│  │     ├─ 20260623185742_apply_v1_architecture.ts
│  │     ├─ 20260624012148_add_is_default_to_categories.ts
│  │     ├─ 20260624012149_create_cards.ts
│  │     ├─ 20260624012150_create_credit_purchases.ts
│  │     ├─ 20260624012151_create_installments.ts
│  │     └─ 20260625005711_optimize_database_indexes.ts
│  ├─ dist
│  │  ├─ @types
│  │  │  └─ knex.d.cjs
│  │  ├─ app.cjs
│  │  ├─ database.cjs
│  │  ├─ env
│  │  │  └─ index.cjs
│  │  ├─ middlewares
│  │  │  └─ check-session-id-exists.cjs
│  │  ├─ routes
│  │  │  └─ transactions.cjs
│  │  ├─ server.cjs
│  │  └─ test
│  │     └─ transactions.spec.cjs
│  ├─ docker-compose.yml
│  ├─ knexfile.ts
│  ├─ package-lock.json
│  ├─ package.json
│  ├─ README_PROJECT.md
│  ├─ seeds
│  │  └─ seed-initial-transactions.ts
│  ├─ src
│  │  ├─ @types
│  │  │  ├─ credit.ts
│  │  │  ├─ fastify-jwt.d.ts
│  │  │  └─ knex.d.ts
│  │  ├─ app.ts
│  │  ├─ database.ts
│  │  ├─ env
│  │  │  └─ index.ts
│  │  ├─ middlewares
│  │  │  ├─ check-auto.ts
│  │  │  └─ check-session-id-exists.ts
│  │  ├─ routes
│  │  │  ├─ accounts.ts
│  │  │  ├─ categories.ts
│  │  │  ├─ credit.ts
│  │  │  ├─ transactions.ts
│  │  │  └─ users.ts
│  │  ├─ server.ts
│  │  └─ test
│  │     ├─ transactions.spec.ts
│  │     ├─ validate-payment.ts
│  │     ├─ validate-phase1.ts
│  │     ├─ validate-phase2.ts
│  │     ├─ validate-phase3.ts
│  │     └─ validate-phase4.ts
│  └─ tsconfig.json
├─ frontend
│  ├─ eslint.config.js
│  ├─ index.html
│  ├─ package-lock.json
│  ├─ package.json
│  ├─ postcss.config.js
│  ├─ src
│  │  ├─ api
│  │  │  └─ client.ts
│  │  ├─ App.tsx
│  │  ├─ components
│  │  │  ├─ AccountModal.tsx
│  │  │  ├─ CategoryModal.tsx
│  │  │  ├─ Header.tsx
│  │  │  ├─ NewTransactionModal.tsx
│  │  │  ├─ SummaryCard.tsx
│  │  │  ├─ TransactionForm.tsx
│  │  │  └─ TransactionTable.tsx
│  │  ├─ contexts
│  │  │  ├─ AuthContext.tsx
│  │  │  └─ AuthProvider.tsx
│  │  ├─ hooks
│  │  │  ├─ useAccounts.ts
│  │  │  ├─ useAuth.ts
│  │  │  ├─ useCategories.ts
│  │  │  ├─ useCredit.ts
│  │  │  └─ useTransactions.ts
│  │  ├─ index.css
│  │  ├─ main.tsx
│  │  ├─ pages
│  │  │  ├─ CreditCards.tsx
│  │  │  ├─ Dashboard
│  │  │  │  └─ index.tsx
│  │  │  ├─ Login.tsx
│  │  │  ├─ Register.tsx
│  │  │  └─ Settings.tsx
│  │  ├─ schemas
│  │  │  └─ transactionSchema.ts
│  │  ├─ services
│  │  │  ├─ accounts.ts
│  │  │  ├─ auth.ts
│  │  │  ├─ categories.ts
│  │  │  └─ transactions.ts
│  │  ├─ types
│  │  │  └─ transaction.ts
│  │  └─ utils
│  │     ├─ currency.ts
│  │     └─ date.ts
│  ├─ tailwind.config.js
│  ├─ tsconfig.app.json
│  ├─ tsconfig.json
│  ├─ tsconfig.node.json
│  └─ vite.config.ts
├─ PROJECT_ROADMAP.md
├─ README.md
└─ render.yaml

```
