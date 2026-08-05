# BACKEND_ROADMAP.md

**Fluxa API**
**Backend Roadmap**
**Versão: 3.0 — Final Release (Testes de Autenticação e Segurança Concluídos)**

---

## 1. OBJETIVO

Este documento controla e consolida a evolução e as regras transacionais do Backend.

O Backend é a única fonte da verdade para:

- Regras de negócio e cálculos monetários.
- Projeções e agregação de KPIs.
- Algoritmos de pontuação e BI.
- Integridade relacional e transações ACID.
- Segurança e isolamento multiusuário.

---

## 2. REGRAS IMUTÁVEIS

### Arquitetura & Transações

- Todo cálculo financeiro ocorre exclusivamente no Backend.
- Toda operação crítica (parcelamentos, aportes em metas, pagamento de faturas) é envolvida em transação ACID via Knex (`knex.transaction`).
- Toda consulta SQL é estritamente isolada por `user_id` extraído do token JWT (`request.user.sub`).
- Blindagem de tipos em tempo de execução via schemas Zod e fallbacks estritos em consultas SQL para evitar retornos `undefined` no JSON.

### Separação de Domínios

- `transactions`: representa exclusivamente movimentações de liquidez real em contas bancárias.
- `credit_purchases` e `installments`: representam obrigações de crédito (nunca afetam o saldo bancário até o pagamento efetivo da parcela).
- `subscriptions`: compromissos fixos recorrentes (geram despesa no fluxo de caixa apenas no ato da baixa operacional).
- `wallets`: reservas e objetivos patrimoniais. Aportes transferem saldo de `accounts` para `wallets` via transação ACID.

---

## 3. STATUS GERAL (RELEASE FINAL)

| Módulo                                  | Status                        |
| :-------------------------------------- | :---------------------------- |
| **Autenticação & JWT**                  | ✅ Concluído / Estável        |
| **Contas Bancárias**                    | ✅ Concluído / Estável        |
| **Categorias**                          | ✅ Concluído / Estável        |
| **Fluxo de Caixa (Transactions)**       | ✅ Concluído / Estável        |
| **Dashboard & Projeções**               | ✅ Concluído / Estável        |
| **Crédito & Parcelamento**              | ✅ Concluído / Estável        |
| **Assinaturas Recorrentes**             | ✅ Concluído / Estável        |
| **Wallets (Objetivos Financeiros)**     | ✅ Concluído / Estável        |
| **Inteligência Financeira (Analytics)** | ✅ Concluído / Estável        |
| **Qualidade & ACID Tests (Fase 1)**     | ✅ Concluído (Infraestrutura) |
| **Segurança e Isolamento (Fase 2)**     | ✅ Concluído (Auditoria)      |

---

## 4. MÓDULOS DE DOMÍNIO (IMPLEMENTAÇÕES COMPLETAS)

### ✅ 1. Wallets (Objetivos Financeiros / Metas)

Status: ✅ Concluído

- [x] Tabela `wallets` criada no PostgreSQL com chaves UUID e status (`active`, `paused`, `completed`).
- [x] Rota transacional `/wallets/transfer` conectando `accounts` e `wallets` no mesmo bloco ACID.
- [x] Lógica de transição automática para `"completed"` ao atingir 100% da meta.
- [x] Proteção contra resgate superior ao saldo acumulado na reserva.

### ✅ 2. Inteligência Financeira (BI & Analytics)

Status: ✅ Concluído

- [x] Endpoint consolidado `/analytics/dashboard` entregando telemetria executiva em uma única requisição.
- [x] Algoritmo de cálculo de Saúde Financeira (Score 0 a 100) ponderando taxa de poupança, compromisso de renda e meses de liquidez.
- [x] Agregação no servidor de despesas por categoria e evolução de fluxo de caixa dos últimos 6 meses.
- [x] Gerador dinâmico de Insights comportamentais e Recomendações de otimização patrimonial com blindagem TypeScript.

### ✅ 3. Assinaturas & Crédito

Status: ✅ Concluído

- [x] Motor de parcelamento em _N_ vezes lidando com precisão de centavos na primeira parcela.
- [x] Soft Delete em compras de crédito com estorno automático de limite disponível no cartão.
- [x] Baixa operacional de assinaturas integrando diretamente na tabela de transações.

---

## 5. SPRINT 7.2 — FASE 2: SEGURANÇA E ISOLAMENTO MULTIUSUÁRIO (CONCLUÍDA)

Foco operacional garantido na estabilidade de autenticação, proteção contra IDOR e blindagem de dados cruzados finalizado com sucesso.

- [x] **Etapa 1:** Auditoria da geração de JWT, middlewares e mapeamento de extração de `user_id`.
- [x] **Etapa 2:** Mapeamento em matriz de todas as rotas da aplicação vs. Nível de Proteção.
- [x] **Etapa 3 & 4:** Testes de rejeição de tokens (ausentes, inválidos, expirados) executados em rotas protegidas.
- [x] **Etapa 5 & 6:** Testes implacáveis de isolamento implementados (Usuário A jamais manipula Account/Transaction/Wallet do Usuário B).
- [x] **Etapa 7:** Prevenção de spoofing de ownership validada no payload (`user_id`).
- [x] **Etapa 8:** Testes de relacionamentos proibidos concluídos (ex: Transaction de A vinculada a Category de B).
