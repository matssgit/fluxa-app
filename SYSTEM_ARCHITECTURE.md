---
# ARCHITECTURE.md

Este documento representa a especificação oficial da arquitetura do sistema.
Toda implementação deverá respeitar as decisões aqui documentadas.
Em caso de conflito entre código e documentação, a documentação deverá ser revisada antes que qualquer alteração estrutural seja realizada.
---

## 1. Visão Geral

O Finance App Beta não é um aplicativo bancário. Ele é um sistema de gestão financeira pessoal e familiar.
Seu objetivo é refletir a realidade financeira informada pelo usuário, auxiliando na organização, planejamento e tomada de decisões, mantendo histórico completo das movimentações e preservando a integridade dos dados financeiros.

A regra de ouro da arquitetura é: **O sistema não reescreve o passado.** Aplica-se rastreabilidade absoluta, onde exclusões físicas são evitadas em favor de cancelamentos lógicos, e a arquitetura garante que gastos no crédito só afetem o fluxo de caixa quando uma obrigação for efetivamente liquidada.

## 2. Não Objetivos

Este projeto não pretende:

- Substituir aplicativos bancários;
- Impedir financeiramente que o usuário realize compras;
- Sincronizar automaticamente com bancos (na V1);
- Tomar decisões financeiras pelo usuário.

Seu papel é representar fielmente a realidade financeira informada pelo usuário.

## 3. Princípios da Arquitetura

Todo desenvolvimento do sistema deve respeitar os princípios abaixo:

- **Backend como fonte única da verdade.**
- **Separação absoluta entre Fluxo de Caixa e Crédito.**
- **Toda operação financeira deve preservar histórico.**
- **Nenhuma informação financeira relevante deve ser perdida.**
- **Toda operação crítica deve ser transacional (ACID).**
- **O banco de dados é responsável pela integridade relacional.**
- **O frontend jamais deve recalcular regras financeiras.**
- **Toda funcionalidade nova deve ser compatível com futuras aplicações Mobile.**

## 4. Princípios de Evolução

Novas funcionalidades devem ser adicionadas preservando a arquitetura existente.
Ao evoluir o sistema:

- Evitar duplicação de regras de negócio;
- Manter o backend como fonte única da verdade;
- Preservar compatibilidade entre Web e Mobile;
- Priorizar mudanças incrementais ao invés de reestruturações completas;
- Preservar histórico financeiro sempre que possível.

## 5. Portabilidade

A API foi projetada para ser independente da interface.
Toda regra financeira reside exclusivamente no backend.
Qualquer cliente (Web, React Native ou outra plataforma) deve consumir exatamente a mesma API e obter os mesmos resultados. Nenhuma regra financeira poderá existir exclusivamente em uma interface.

## 6. Escopo Inicial

A fundação do sistema contempla exclusivamente:

- Autenticação
- Contas
- Categorias
- Fluxo de caixa
- Cartões
- Compras parceladas
- Pagamento de parcelas
- Dashboard

Funcionalidades como recorrências, metas financeiras, investimentos, faturas consolidadas e relatórios avançados pertencem a camadas de evolução futuras, mas a arquitetura base deve suportar sua futura incorporação.

## 7. Stack Tecnológica

- **Frontend:** React, TypeScript, Vite, Tailwind CSS, TanStack Query (React Query), Axios, Lucide Icons.
- **Backend:** Node.js, Fastify, TypeScript, Zod (validação), Knex.js (Query Builder).
- **Banco de Dados:** PostgreSQL (Relacional).

## 8. Infraestrutura

A infraestrutura foi padronizada utilizando Docker.

**Objetivos:**

- Ambiente reproduzível.
- Isolamento entre serviços.
- Facilidade de implantação.
- Compatibilidade entre ambientes.

**Serviços atuais:**

- PostgreSQL

**Planejamento futuro:**

- Backend
- Frontend
- Reverse Proxy
- Deploy automatizado

## 9. Estrutura do Banco de Dados

A arquitetura utiliza banco de dados relacional (PostgreSQL) com esquema estrito.

**Características implementadas:**

- Foreign Keys restritivas.
- Utilização de UUID (v4) para chaves primárias e estrangeiras.
- Constraints rigorosas (ex: `CHECK`).
- Transações ACID para operações de múltiplas tabelas.

## 10. Migrations

Toda alteração estrutural no banco deverá ocorrer exclusivamente através de migrations versionadas.

- É proibido alterar manualmente a estrutura do banco em produção.
- Cada migration deve ser reversível sempre que possível.

## 11. API

A API segue arquitetura REST.
As rotas devem:

- Utilizar autenticação JWT;
- Validar payloads com Zod;
- Retornar códigos HTTP coerentes;
- Evitar efeitos colaterais inesperados;
- Manter compatibilidade retroativa sempre que possível.

## 12. Regras Imutáveis de Domínio

1. `transactions` representa apenas dinheiro real.
2. Compras de cartão jamais entram em `transactions`.
3. Compras parceladas vivem exclusivamente em `credit_purchases` e `installments`.
4. O pagamento de uma parcela gera movimentação de saída (despesa) em `transactions`.
5. `account_id` nunca deve existir em `installments`.
6. `card_id` nunca deve existir em `transactions`.
7. Todo registro financeiro deve, obrigatoriamente, estar vinculado a um `user_id` (Multi-tenant).

## 13. Fluxos Financeiros

### Fluxo de Caixa

Representado pela tabela `transactions` e `accounts`. Saldo de uma conta é a soma algébrica de suas transações (Entradas positivas, Saídas negativas). O saldo nunca é uma coluna estática; é sempre um valor derivado das transações concluídas.

### Crédito

Regido por `cards`, `credit_purchases` e `installments`. Uma compra de crédito gera _N_ parcelas. O motor de parcelamento calcula vencimentos somando meses à data da compra e ajusta eventuais dízimas/centavos na primeira parcela.

### Limites

A tabela `cards` possui desmembramento estratégico:

- `total_limit`: Valor máximo concedido pelo emissor.
- `available_limit`: Limite de uso em tempo real. Subtraído na compra, restaurado no pagamento ou cancelamento de faturas.

### Cancelamentos

Compras de crédito utilizam **Soft Delete** (`status = 'cancelled'`). Ao cancelar uma compra:

- Transação ACID isola parcelas pagas das pendentes.
- Apenas parcelas pendentes mudam para `cancelled`.
- O limite das parcelas pendentes é estornado para o cartão.
- O histórico de parcelas já pagas permanece intacto.

### Exclusões

Exclusão física (`DELETE`) é permitida apenas em entidades-raiz que não possuam vínculos financeiros ativos. Um cartão não pode ser apagado se possuir faturas `pending`.

## 14. Segurança

- Proteção de rotas via JWT (Bearer Token).
- Hashes de senha via Bcrypt.
- Identificação do usuário interceptada em Middleware e injetada no contexto da requisição. Nenhuma rota confia em `user_id` enviado pelo payload.

## 15. Design System

O projeto segue uma identidade visual única.

**Princípios:**

- Minimalismo;
- Consistência;
- Foco na informação;
- Baixa carga cognitiva;
- Componentes reutilizáveis;
- Design responsivo;
- Adaptação futura ao React Native.

A identidade visual é inspirada nas interfaces modernas da Apple, utilizando transparências sutis, bordas suaves, animações discretas e tipografia SF Pro Display quando disponível.

## 16. Convenções de Código

Todo código deve seguir:

- SOLID (quando aplicável).
- DRY (Don't Repeat Yourself).
- KISS (Keep It Simple, Stupid).
- Clean Code (Nomes descritivos, responsabilidade única).
- TypeScript Strict (Proibido uso de `any` explícito).

## 17. Visão de Longo Prazo

Este projeto foi concebido para evoluir continuamente. A arquitetura deverá suportar aplicação Web, aplicação Mobile, API Pública, sincronização em nuvem, múltiplos dispositivos, notificações, recorrências automáticas e dashboards avançados — sem necessidade de reescrever o núcleo financeiro.
