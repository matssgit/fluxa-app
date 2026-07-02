---

# PROJECT_ROADMAP.md

**Finance App Beta V2**
**Roadmap Geral do Projeto**
Versão: **2.0**

Este documento representa a visão macro do projeto.

Os detalhes técnicos encontram-se em:

- `SYSTEM_ARCHITECTURE.md`
- `BACKEND_ROADMAP.md`
- `FRONTEND_ROADMAP.md`

Nenhuma funcionalidade deve contrariar as regras definidas na arquitetura do sistema.

---

# VISÃO DO PRODUTO

O Finance App é um sistema de gestão financeira pessoal.

Seu objetivo é fornecer ao usuário uma visão clara da sua vida financeira através de quatro pilares principais:

- Controle do Fluxo de Caixa
- Gestão de Cartões de Crédito
- Planejamento Financeiro
- Inteligência Financeira

O Backend é responsável por toda regra de negócio.

O Frontend é responsável pela experiência do usuário.

---

# STATUS GERAL

| Fase                    | Status          |
| ----------------------- | --------------- |
| Autenticação            | ✅ Concluída    |
| Contas                  | ✅ Concluída    |
| Categorias              | 🚧 Em andamento |
| Fluxo de Caixa          | ✅ Concluído    |
| Dashboard               | ✅ Concluído    |
| Ecossistema de Crédito  | ✅ Concluído    |
| Polimento da Interface  | 🚧 Em andamento |
| Assinaturas             | ⏳ Planejado    |
| Wallets (Caixinhas)     | ⏳ Planejado    |
| Inteligência Financeira | ⏳ Planejado    |
| Pesquisa Global         | ⏳ Planejado    |
| Relatórios              | ⏳ Planejado    |

---

# MÓDULOS DO SISTEMA

## ✅ Autenticação

Concluído.

- Login
- Cadastro
- JWT
- Proteção de rotas
- Multiusuário

---

## ✅ Fluxo de Caixa

Concluído.

Permite:

- Entradas
- Saídas
- Histórico
- Categorias
- Contas

---

## ✅ Dashboard

Concluído.

A Central de Comando apresenta:

- Saldo Atual
- Entradas
- Saídas
- Projeção do mês
- Timeline financeira
- Pendências

---

## ✅ Cartões de Crédito

Concluído.

Permite:

- Criar cartões
- Personalizar cartões
- Editar cartões
- Excluir cartões
- Compras parceladas
- Parcelas
- Pagamento de parcelas
- Cancelamento de compras
- Atualização automática dos limites

---

## 🚧 Interface

Em evolução.

Já implementado:

- Toasts
- Skeletons
- Personalização dos cartões
- Navegação inicial
- Dashboard consolidado

Pendências:

- Pesquisa Global
- Melhorias de responsividade
- Tema Claro/Escuro
- Navegação definitiva
- Melhorias de acessibilidade

---

## ⏳ Assinaturas

Planejado.

Objetivo:

Permitir cobranças recorrentes.

Exemplos:

- Netflix
- Spotify
- Internet
- Academia
- Apple One

As assinaturas deverão:

- aparecer no histórico;
- participar da projeção financeira;
- gerar pendências automaticamente;
- respeitar categorias;
- permitir pausa e cancelamento.

---

## ⏳ Wallets (Caixinhas)

Planejado.

Objetivo:

Permitir criação de metas financeiras.

Exemplos:

- Reserva de emergência
- Viagem
- Notebook
- Moto
- Casa

---

## ⏳ Inteligência Financeira

Planejado.

Métricas previstas:

- Gastos por categoria
- Evolução mensal
- Comparativo de meses
- Evolução patrimonial
- Média diária
- Média semanal
- Média mensal
- Tendências

---

## ⏳ Pesquisa Global

Planejado.

Permitirá localizar rapidamente qualquer informação do sistema.

Filtros previstos:

- Texto
- Categoria
- Conta
- Cartão
- Compra
- Parcela
- Assinatura
- Data
- Período
- Valor
- Status

---

# ROADMAP DA V1

## ✅ Já entregue

- Sistema de autenticação
- Dashboard consolidado
- Fluxo de Caixa
- Cartões
- Parcelamento
- Projeção financeira
- Histórico híbrido
- Pendências
- Personalização dos cartões
- Toasts
- Navegação inicial

---

## 🚧 Em andamento

- Refinamento da navegação
- Pesquisa Global
- Responsividade
- Tema Claro/Escuro
- Skeletons completos
- Categorias padrão

---

## ⏳ Próximos módulos

1. Assinaturas Recorrentes
2. Wallets (Caixinhas)
3. Inteligência Financeira
4. Relatórios
5. Exportação de dados

---

# VERSÃO 1.0 (MVP)

Para considerar a V1 concluída, o sistema deverá possuir:

- [ ] Pesquisa Global
- [ ] Tema Claro/Escuro
- [ ] Categorias padrão
- [ ] Assinaturas recorrentes
- [ ] Wallets
- [ ] Responsividade completa
- [ ] Polimento final da interface
- [ ] Testes finais de validação

---

# VERSÃO 2.0

Itens reservados para a próxima grande versão:

- Fechamento de Faturas (Invoices)
- Múltiplos perfis financeiros
- Compartilhamento familiar
- Orçamentos mensais
- IA para análise financeira
- Metas inteligentes
- Alertas automáticos
- Integração bancária (Open Finance)

---
