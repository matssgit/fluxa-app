# PROJECT_ROADMAP.md

# Sistema Financeiro Familiar

## Roadmap Oficial de Arquitetura e Desenvolvimento

Versão: 1.1

Este documento é a fonte oficial da verdade do projeto.
Nenhuma funcionalidade deve ser implementada sem respeitar este roadmap e as regras imutáveis.

---

# REGRAS ARQUITETURAIS IMUTÁVEIS

1. `transactions` representa apenas dinheiro real.
2. Compras de cartão jamais entram em `transactions`.
3. Compras parceladas vivem exclusivamente em:
   - `credit_purchases`
   - `installments`
4. O pagamento de uma parcela gera movimentação de saída (despesa) em `transactions`.
5. `account_id` nunca deve existir em `installments`.
6. `card_id` nunca deve existir em `transactions`.
7. Nenhuma funcionalidade pode quebrar a separação estrita Caixa x Crédito.
8. Faturas (Invoices) e seu fechamento pertencem exclusivamente à V2.

---

# STATUS ATUAL

## Fase 1 - Autenticação

Status: ✅ CONCLUÍDO

### Checklist

- [x] JWT
- [x] Cadastro de usuário
- [x] Login
- [x] Middleware de autenticação
- [x] Migração gradual do session_id
- [x] Isolamento por user_id

---

## Fase 2 - Contas e Categorias

Status: ✅ CONCLUÍDO

### Accounts

- [x] Migration
- [x] CRUD Backend
- [x] Serviços Frontend
- [x] React Query
- [x] Modal de cadastro

### Categories

- [x] Migration
- [x] CRUD Backend
- [x] color
- [x] icon
- [x] is_default
- [ ] Categorias padrão do sistema

---

## Fase 3 - Fluxo de Caixa

Status: ✅ CONCLUÍDO

### Transactions

- [x] account_id
- [x] category_id
- [x] status
- [x] expected_date
- [x] completed_date

### Pendências

- [x] Listagem
- [x] Marcar como concluído

### Dashboard

- [x] Entradas
- [x] Saídas
- [x] Saldo

---

# FASE 4 - ECOSSISTEMA DE CRÉDITO

Status: ⏳ EM DESENVOLVIMENTO

Objetivo: Separar completamente Caixa real (transactions) de Crédito (cartões).

## Critério de Conclusão da Fase 4

A Fase 4 somente será considerada concluída quando:

- [ ] Criar cartão
- [ ] Listar cartões
- [ ] Criar compra parcelada
- [ ] Gerar parcelas automaticamente
- [ ] Listar parcelas
- [ ] Marcar parcela como paga
- [ ] Registrar saída em transactions
- [ ] Atualizar dashboard corretamente
- [ ] Testar parcelamento 3x
- [ ] Testar parcelamento 12x
- [ ] Testar parcelamento 24x
- [ ] Validar multiusuário

---

## 4.1 Migration Cards

Status: ✅ CONCLUÍDO

### Checklist

- [x] Migration criada
- [x] Migration executada
- [x] Tipagem Knex atualizada

---

## 4.2 Migration Credit Purchases

Status: ✅ CONCLUÍDO

### Regras

- category_id: ON DELETE RESTRICT

### Checklist

- [x] Migration criada
- [x] Migration executada
- [x] Tipagem atualizada

---

## 4.3 Migration Installments

Status: ✅ CONCLUÍDO

### Status

- pending
- paid

### Checklist

- [x] Migration criada
- [x] Migration executada
- [x] Tipagem atualizada

---

## 4.4 Backend Cards

Status: ⏳ EM DESENVOLVIMENTO

### Rotas

- GET /cards
- POST /cards

### Validação Obrigatória

- [x] Schema Zod
- [x] Controller
- [x] Validação JWT
- [ ] Teste Backend (Insomnia/Postman)
- [ ] Teste Persistência Banco

---

## 4.5 Backend Credit Purchases

Status: ⏳ EM DESENVOLVIMENTO

### Rotas

- GET /credit-purchases
- POST /credit-purchases

### Regras obrigatórias

- Transaction ACID
- Rollback automático
- Tratamento de centavos

### Validação Obrigatória

- [x] Controller
- [x] Service
- [x] Transaction DB
- [ ] Teste Backend (Insomnia/Postman)

---

## 4.6 Motor de Parcelamento

Status: ⏳ EM DESENVOLVIMENTO

### Checklist

- [x] Cálculo do vencimento
- [x] Geração de parcelas
- [x] Ajuste de centavos
- [ ] Teste com 3x
- [ ] Teste com 12x
- [ ] Teste com 24x

---

## 4.7 Backend Installments

Status: ⏳ PENDENTE

### Rotas

- GET /installments
- POST /installments/:id/pay

### Pay Payload

```json
{
   "account_id": "uuid"
}
```
