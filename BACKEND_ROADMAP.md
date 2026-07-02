---

# BACKEND_ROADMAP.md

**Finance App Beta V2**
**Backend Roadmap**
**Versão: 2.0**

---

# OBJETIVO

Este documento controla exclusivamente a evolução do Backend.

O Backend é a única fonte da verdade para:

- regras financeiras;
- cálculos monetários;
- projeções;
- integridade dos dados;
- segurança;
- consistência transacional.

Todo cálculo financeiro pertence exclusivamente ao Backend.

---

# REGRAS IMUTÁVEIS

## Arquitetura

- Todo cálculo financeiro deve ocorrer exclusivamente no Backend.
- O Frontend nunca recalcula valores monetários.
- Toda operação crítica deve ser transacional (ACID).
- Toda consulta deve respeitar isolamento por `user_id`.
- Toda regra deve possuir validação antes da persistência.
- Nenhuma regra financeira poderá depender da interface.

---

## Separação de Domínios

- `transactions` representa apenas dinheiro real.
- Compras de cartão nunca entram em `transactions`.
- Compras parceladas vivem exclusivamente em `credit_purchases`.
- Parcelas vivem exclusivamente em `installments`.
- O pagamento de parcelas gera uma nova `transaction`.
- `account_id` nunca pertence às parcelas.
- `card_id` nunca pertence às transações.
- Assinaturas não bloqueiam limite de cartão antecipadamente.
- Wallets nunca representam despesas, apenas movimentações internas.
- Faturas (Invoices) pertencem exclusivamente à V2.

---

# STATUS GERAL

| Módulo          | Status         |
| --------------- | -------------- |
| Autenticação    | ✅ Estável     |
| Contas          | ✅ Estável     |
| Categorias      | 🚧 Em evolução |
| Fluxo de Caixa  | ✅ Estável     |
| Dashboard       | ✅ Estável     |
| Crédito         | ✅ Estável     |
| Assinaturas     | ✅ Estável     |
| Wallets         | ⏳ Planejado   |
| Pesquisa Global | ⏳ Planejado   |
| Métricas        | ⏳ Planejado   |
| Relatórios      | ⏳ Planejado   |
| Performance     | ⏳ Planejado   |
| Testes          | 🚧 Em evolução |

---

# MÓDULO — Autenticação

Status: ✅ Estável

## Implementado

- [x] JWT
- [x] Cadastro
- [x] Login
- [x] Middleware
- [x] Proteção de rotas
- [x] Isolamento por usuário
- [x] Remoção do Session ID legado
- [x] Tipagem do usuário autenticado

## Melhorias Futuras

- [ ] Refresh Token
- [ ] Recuperação de senha
- [ ] Confirmação por e-mail
- [ ] Auditoria de login

---

# MÓDULO — Contas

Status: ✅ Estável

## Implementado

- [x] Migration
- [x] CRUD
- [x] Validações
- [x] Integração Dashboard

## Melhorias Futuras

- [ ] Contas arquivadas
- [ ] Ordenação personalizada

---

# MÓDULO — Categorias

Status: 🚧 Em evolução

## Implementado

- [x] Migration
- [x] CRUD
- [x] Cor
- [x] Ícone
- [x] Categorias padrão do sistema

## Pendências

- [ ] Seed automático na criação do usuário

---

# MÓDULO — Fluxo de Caixa

Status: ✅ Estável

## Implementado

- [x] CRUD
- [x] Entradas
- [x] Saídas
- [x] Status
- [x] Datas previstas
- [x] Datas efetivas
- [x] Integração Dashboard

## Melhorias Futuras

- [ ] Exclusão em lote
- [ ] Paginação
- [ ] Filtros avançados

---

# MÓDULO — Dashboard

Status: ✅ Estável

## Implementado

- [x] Saldo atual
- [x] Entradas
- [x] Saídas
- [x] Timeline híbrida
- [x] Pendências
- [x] DashboardResponse
- [x] Defensive Coding
- [x] Projeção do mês
- [x] Inteligência preditiva

## Melhorias Futuras

- [ ] Cache
- [ ] Indicadores estatísticos
- [ ] Dashboard analítico

---

# MÓDULO — Crédito

Status: ✅ Estável

## Implementado

### Cartões

- [x] CRUD
- [x] Limite
- [x] Limite disponível

### Compras

- [x] Cadastro
- [x] Cancelamento
- [x] Soft Delete

### Parcelamento

- [x] Geração automática
- [x] Distribuição matemática
- [x] Rollback ACID
- [x] Baixa
- [x] Atualização automática do limite

## Pendências

- [ ] Testes completos 12x
- [ ] Testes completos 24x
- [ ] Testes de virada de ano

---

# MÓDULO — Assinaturas

Status: ✅ Estável

## Implementado

- [x] Migration
- [x] CRUD
- [x] Frequência
- [x] Pagamento
- [x] Integração Dashboard
- [x] Integração Timeline
- [x] Integração Pendências
- [x] Projeção financeira
- [x] Filtro inteligente

## Melhorias Futuras

- [ ] Scheduler automático
- [ ] Cobranças anuais
- [ ] Cobranças trimestrais
- [ ] Histórico completo

---

# MÓDULO — Wallets

Status: ⏳ Planejado

Objetivo

Gerenciar objetivos financeiros.

## Planejado

- [ ] Migration
- [ ] CRUD
- [ ] Transferências
- [ ] Histórico
- [ ] Metas
- [ ] Progresso
- [ ] Integração Dashboard

---

# MÓDULO — Pesquisa Global

Status: ⏳ Planejado

## Pesquisar

- [ ] Transactions
- [ ] Compras
- [ ] Parcelas
- [ ] Assinaturas
- [ ] Cartões
- [ ] Contas
- [ ] Categorias

## Filtros

- [ ] Período
- [ ] Valor
- [ ] Categoria
- [ ] Conta
- [ ] Cartão
- [ ] Status

---

# MÓDULO — Métricas

Status: ⏳ Planejado

Objetivo

Gerar indicadores financeiros.

## Planejado

- [ ] Gastos por categoria
- [ ] Evolução mensal
- [ ] Evolução patrimonial
- [ ] Fluxo de caixa
- [ ] Comparativos
- [ ] Tendências

---

# MÓDULO — Relatórios

Status: ⏳ Planejado

## Planejado

- [ ] PDF
- [ ] Excel
- [ ] CSV
- [ ] Backup JSON
- [ ] Importação

---

# MÓDULO — Performance

Status: ⏳ Planejado

## Planejado

- [ ] Índices SQL
- [ ] Paginação
- [ ] Cache
- [ ] Queries otimizadas
- [ ] Monitoramento
- [ ] Logs estruturados

---

# MÓDULO — Qualidade

Status: 🚧 Em evolução

## Testes

- [x] Vitest configurado
- [x] Banco isolado
- [x] Testes de Crédito
- [x] Testes ACID
- [x] Testes de Dashboard

## Pendências

- [ ] Cobertura ≥ 90%
- [ ] Testes de integração completos
- [ ] Testes E2E
- [ ] Testes de carga
- [ ] Testes de concorrência

---

# MVP V1

## Obrigatório para finalizar

### Domínio

- [ ] Seed automático de categorias
- [ ] Backend de Wallets
- [ ] Pesquisa Global

### Validação

- [ ] Testes completos de parcelamento (3x, 12x, 24x)
- [ ] Testes de virada de ano
- [ ] Cobertura mínima de testes ≥ 90%

### Infraestrutura

- [ ] Scheduler para Assinaturas (opcional, mas recomendado)
- [ ] Otimização de queries principais

---

## Minha única alteração em relação ao seu planejamento

Eu acrescentaria um módulo que normalmente só aparece quando o sistema já está mais maduro:

## **MÓDULO — Observabilidade**

Status: ⏳ Futuro

- [ ] Logs estruturados
- [ ] Auditoria de ações do usuário
- [ ] Rastreamento de erros
- [ ] Health Check
- [ ] Métricas de API
- [ ] Tempo médio de resposta
- [ ] Monitoramento de consultas lentas
