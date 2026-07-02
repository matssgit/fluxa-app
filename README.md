
```
finance-app-beta-v2
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
│  │     ├─ 20260625005711_optimize_database_indexes.ts
│  │     ├─ 20260625194144_alter_cards_limits.ts
│  │     ├─ 20260626004855_add_status_to_purchases.ts
│  │     ├─ 20260626011822_update_installments_status_check.ts
│  │     ├─ 20260626113806_add_color_to_cards.ts
│  │     ├─ 20260701142000_add_type_to_transactions.ts
│  │     ├─ 20260701190000_alter_transactions_subscription_id.ts
│  │     └─ 20260701204237_create_subscriptions.ts
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
│  │  ├─ 001_clear_data.ts
│  │  └─ seed-initial-transactions.ts
│  ├─ src
│  │  ├─ @types
│  │  │  ├─ credit.ts
│  │  │  ├─ dashboard.types.ts
│  │  │  ├─ fastify-jwt.d.ts
│  │  │  └─ knex.d.ts
│  │  ├─ app.ts
│  │  ├─ database.ts
│  │  ├─ env
│  │  │  └─ index.ts
│  │  ├─ middlewares
│  │  │  ├─ check-auth.ts
│  │  │  └─ check-session-id-exists.ts
│  │  ├─ routes
│  │  │  ├─ accounts.ts
│  │  │  ├─ categories.ts
│  │  │  ├─ credit.ts
│  │  │  ├─ dashboard.ts
│  │  │  ├─ subscriptions.ts
│  │  │  ├─ transactions.ts
│  │  │  └─ users.ts
│  │  ├─ server.ts
│  │  └─ tests
│  │     ├─ credit.spec.ts
│  │     ├─ setup.ts
│  │     ├─ transactions.spec.ts
│  │     ├─ validate-payment.ts
│  │     ├─ validate-phase1.ts
│  │     ├─ validate-phase2.ts
│  │     ├─ validate-phase3.ts
│  │     └─ validate-phase4.ts
│  ├─ tsconfig.json
│  └─ vitest.config.ts
├─ BACKEND_ROADMAP.md
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
│  │  │  ├─ CancelPurchaseModal.tsx
│  │  │  ├─ CardDetailsModal.tsx
│  │  │  ├─ CardItem.tsx
│  │  │  ├─ CategoryModal.tsx
│  │  │  ├─ CreateCardModal.tsx
│  │  │  ├─ CreatePurchaseModal.tsx
│  │  │  ├─ DeleteActionModal.tsx
│  │  │  ├─ EditCardModal.tsx
│  │  │  ├─ Header.tsx
│  │  │  ├─ InstallmentsList.tsx
│  │  │  ├─ NewTransactionModal.tsx
│  │  │  ├─ PayInstallmentModal.tsx
│  │  │  ├─ PaySubscriptionModal.tsx
│  │  │  ├─ PurchaseDetailsModal.tsx
│  │  │  ├─ PurchasesList.tsx
│  │  │  ├─ Sidebar.tsx
│  │  │  ├─ SummaryCard.tsx
│  │  │  ├─ TransactionForm.tsx
│  │  │  ├─ TransactionTable.tsx
│  │  │  └─ ui
│  │  │     ├─ CardsList.tsx
│  │  │     └─ Skeleton.tsx
│  │  ├─ contexts
│  │  │  ├─ AuthContext.tsx
│  │  │  └─ AuthProvider.tsx
│  │  ├─ hooks
│  │  │  ├─ useAccounts.ts
│  │  │  ├─ useAuth.ts
│  │  │  ├─ useCategories.ts
│  │  │  ├─ useCredit.ts
│  │  │  ├─ useDashBoard.ts
│  │  │  ├─ useSubscriptions.ts
│  │  │  └─ useTransactions.ts
│  │  ├─ index.css
│  │  ├─ layouts
│  │  │  └─ DefaultLayout.tsx
│  │  ├─ main.tsx
│  │  ├─ pages
│  │  │  ├─ Accounts
│  │  │  │  └─ index.tsx
│  │  │  ├─ CreditCards.tsx
│  │  │  ├─ Dashboard
│  │  │  │  └─ index.tsx
│  │  │  ├─ Login.tsx
│  │  │  ├─ Register.tsx
│  │  │  ├─ Settings.tsx
│  │  │  └─ Subscriptions
│  │  │     ├─ CreateSubscriptionModal.tsx
│  │  │     └─ index.tsx
│  │  ├─ schemas
│  │  │  └─ transactionSchema.ts
│  │  ├─ services
│  │  │  ├─ accounts.ts
│  │  │  ├─ auth.ts
│  │  │  ├─ categories.ts
│  │  │  ├─ subscriptions.ts
│  │  │  └─ transactions.ts
│  │  ├─ types
│  │  │  ├─ dashboard.ts
│  │  │  ├─ subscription.ts
│  │  │  └─ transaction.ts
│  │  └─ utils
│  │     ├─ cardColors.ts
│  │     ├─ currency.ts
│  │     └─ date.ts
│  ├─ tailwind.config.js
│  ├─ tsconfig.app.json
│  ├─ tsconfig.json
│  ├─ tsconfig.node.json
│  └─ vite.config.ts
├─ FRONTEND_ROADMAP.md
├─ PROJECT_ROADMAP.md
├─ render.yaml
└─ SYSTEM_ARCHITECTURE.md

```