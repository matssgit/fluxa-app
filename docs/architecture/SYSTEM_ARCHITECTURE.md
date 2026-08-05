# SYSTEM_ARCHITECTURE.md

Este documento representa a especificação oficial da arquitetura do sistema.
Toda implementação deverá respeitar as decisões aqui documentadas.
Em caso de conflito entre código e documentação, a documentação deverá ser revisada antes que qualquer alteração estrutural seja realizada.

---

## 1. Visão Geral

O Fluxa não é apenas um aplicativo bancário, mas um sistema completo de gestão financeira pessoal. Seu objetivo é refletir a realidade financeira informada pelo usuário, auxiliando na organização, planejamento e tomada de decisões.

**Regra de Ouro da Arquitetura:** O sistema não reescreve o passado. Aplica-se rastreabilidade absoluta, onde exclusões físicas são restritas, cancelamentos lógicos são priorizados, e a arquitetura garante que gastos no crédito só afetem o fluxo de caixa quando uma fatura ou obrigação for efetivamente liquidada.

## 2. Princípios da Arquitetura

Todo o desenvolvimento deve respeitar rigorosamente as premissas:

- **Backend como Fonte Única da Verdade:** Regras financeiras e cálculos residem exclusivamente no lado do servidor. O frontend jamais recalcula regras financeiras essenciais.
- **Multi-tenant e Ownership Isolado:** O banco e os repositórios garantem que cada transação e entidade seja estritamente validada contra o `user_id` criptografado no token JWT. A API desconfia nativamente de IDs passados no payload.
- **Preservação de Histórico:** Nenhuma informação financeira relevante deve ser perdida.
- **Consistência ACID:** Toda operação crítica multi-tabela é executada dentro de transações de banco de dados (Transactions do Knex).

## 3. Escopo e Domínios Implementados

A fundação evoluiu progressivamente e atualmente sustenta uma gama complexa de domínios:

- **Autenticação:** Baseada em JWT (com infraestrutura pronta para evoluções futuras como 2FA).
- **Gestão de Contas e Fluxo de Caixa:** Lançamentos diretos de entradas e saídas.
- **Carteiras (Wallets):** Gestão de saldos separados e transferências entre fundos.
- **Cartões e Parcelamentos:** Motor de crédito que separa compras do caixa principal, gerando projeções de parcelas (`installments`).
- **Assinaturas (Subscriptions):** Motor de recorrência que projeta e lista as próximas datas de faturamento contínuo.
- **Analytics e Dashboard:** Consolidação visual, estado global de privacidade e tematização nativa.

## 4. Stack Tecnológica e Infraestrutura

A aplicação segue a divisão Cliente-Servidor (SPA + API RESTful):

- **Frontend:** React, TypeScript, Vite, Tailwind CSS, TanStack Query (React Query) / Axios API Client, Lucide Icons.
- **Backend:** Node.js, Fastify, Zod (validação), Knex.js (Query Builder).
- **Banco de Dados:** SQLite (para desenvolvimento local rápido) e PostgreSQL (para o ambiente de produção Relacional).
- **Infraestrutura:** Docker, configurações de deploy baseadas no `render.yaml`.
- **Qualidade e Testes:** Vitest para suítes de testes automatizados (Unitários e Integração).

## 5. Estrutura do Banco de Dados e Migrations

A integridade arquitetural depende fortemente de um modelo de banco relacional rigoroso:

- **Migrations:** Toda alteração estrutural no banco deverá ocorrer exclusivamente através de migrations versionadas do Knex (`backend/db/migrations`). É proibido alterar manualmente a estrutura do banco.
- **Restrições Relacionais:** Utilização intensiva de Foreign Keys restritivas (`ON DELETE RESTRICT`) e Constraints rigorosas para garantir a amarração dos dados.

## 6. Regras Imutáveis de Domínio (Fluxos Financeiros)

1. A entidade `transactions` (agora unificada no conceito de Eventos Financeiros) representa apenas dinheiro real.
2. Compras de cartão jamais entram diretamente no fluxo de transações à vista.
3. Compras parceladas vivem exclusivamente nas tabelas de crédito (`credit_purchases` e `installments`).
4. O pagamento de uma parcela de cartão gera movimentação de saída (despesa) no caixa.
5. Um `account_id` (Conta) nunca deve existir em parcelas de crédito (`installments`), pois a dívida é do cartão, não da conta até que seja paga.
6. Todo registro financeiro deve, obrigatoriamente, estar vinculado a um `user_id`.

## 7. Segurança e Proteção

- **JWT:** Proteção total de rotas via Bearer Token.
- **Middleware Contextual:** O middleware `check-auth` identifica o usuário criptograficamente e o injeta na requisição. Nenhuma rota confia no `user_id` enviado pelo frontend.
- **Criptografia:** Senhas armazenadas com Hashes seguros (Bcrypt).

## 8. Estratégia de Testes (Quality Assurance)

A estabilidade da arquitetura é matematicamente provada através de suítes de testes:

- **Testes de Concorrência (`concurrency.spec.ts`):** Proteção contra _race conditions_, garantindo que múltiplas chamadas assíncronas não burlem cálculos de saldo de carteiras ou limites de cartão.
- **Testes de Ownership (`ownership.spec.ts`):** Validação estrita onde requisições mockadas atestam que um Usuário "A" é incapaz de ler, editar ou deletar propriedades do Usuário "B".
- **Testes de Hardening (`hardening.spec.ts`):** Injeção de _payloads_ malformados e maliciosos para provar a invulnerabilidade dos esquemas Zod da camada de _Routes_.

## 9. Visão de Evolução

O Fluxa foi concebido para evoluir. A separação atual de domínios garante que a adição de novas funcionalidades (como a futura implantação da Autenticação de 2 Fatores - 2FA, metas e consolidação avançada de faturas) ocorra de forma iterativa, sem a necessidade de refatorações de quebra de compatibilidade em toda a árvore do projeto.
