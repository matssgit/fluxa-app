# Fluxa Web - Frontend Roadmap & UX Guidelines

**Versão:** 3.0 — Final Release (UX Validation Concluída)

---

## 1. VISÃO GERAL E OBJETIVOS

Este documento mapeia o estado final de desenvolvimento e as diretrizes de experiência do usuário (UX) da aplicação React (Single Page Application).

O Frontend do Fluxa possui responsabilidades estritas e bem delimitadas:

- **Interface e Design System:** Renderização fiel aos tokens de design (Pine & Sage).
- **Ergonomia e UX:** Garantir atrito zero na inserção de dados, com foco em usabilidade Mobile-First (desenhada para o polegar).
- **Gestão de Estado Assíncrono:** Consumo reativo de APIs através do TanStack Query (React Query) para manter a interface sincronizada com o servidor sem recarregamentos desnecessários.

## 2. REGRAS IMUTÁVEIS DE PRODUTO E UI

Para manter a consistência em toda a aplicação, o desenvolvimento front-end obedeceu rigorosamente às seguintes regras:

- **Componentização Global:** Todo componente visual que melhora a experiência do usuário foi globalizado em `src/components/ui/`. A criação de componentes isolados e duplicados dentro de páginas específicas é expressamente proibida.
- **Controle de Inputs Nativos:** O uso cru de `<select>` HTML e `<input type="date">` é proibido. Eles foram totalmente substituídos por modais interativos construídos no Design System (como `PickerModal` e `DatePickerModal`).
- **Reatividade e Cache:** Toda tela consome dados reativamente. As mutações atualizam o cache local do React Query instantaneamente, proporcionando uma sensação de tempo real.
- **Estabilidade de Layout:** A navegação não parece uma "troca de sistema". As larguras máximas (`max-w-7xl`) são rigorosamente travadas para evitar saltos de layout.

## 3. STATUS GERAL DOS DOMÍNIOS (PROJETO FINALIZADO)

Todos os domínios da aplicação atingiram estabilidade estrutural e a validação final de experiência do usuário foi concluída com sucesso.

| Módulo de Domínio               | Status Atual              |
| :------------------------------ | :------------------------ |
| **Dashboard Executivo**         | ✅ Concluído / Estável    |
| **Fluxo de Caixa (Transações)** | ✅ Concluído / Estável    |
| **Cartões & Parcelas**          | ✅ Concluído / Estável    |
| **Assinaturas Recorrentes**     | ✅ Concluído / Estável    |
| **Carteiras (Wallets / Metas)** | ✅ Concluído / Estável    |
| **Categorias (CRUD Completo)**  | ✅ Concluído / Estável    |
| **Navegação & Shell**           | ✅ Concluído / Estável    |
| **Arquitetura (Clean Code)**    | ✅ Concluído              |
| **Validação de Produto (UX)**   | ✅ Concluído (Sprint 7.1) |

## 4. ENTREGAS FINAIS E LAPIDAÇÃO DE UX (SPRINT 7.1 CONCLUÍDA)

Foco exclusivo em atrito zero, carga cognitiva mínima e experiência desenhada 100% para o polegar no Mobile finalizado com as seguintes entregas:

- [x] **Infraestrutura Global:** Propagação do `PickerModal` e `DatePickerModal` substituindo selects e calendários nativos em modais críticos (ex: `PaySubscriptionModal`).
- [x] **Sincronização Perfeita (React Query):** Fluxo de Caixa reage instantaneamente a ações transversais (ex: dar baixa numa assinatura atualiza o Caixa sem _refresh_).
- [x] **Cálculo de Data Inteligente:** Separação visual entre "Data de Competência" e "Data de Liquidação", com _parsing_ ISO imune a fusos horários (GMT-3).
- [x] **Lapidação de CRUDs:** Implementação completa de ações de Edição e Exclusão (Categorias) utilizando componentes nativos do Design System e _hover states_ limpos.
- [x] **Blindagem de Erros:** Prevenção de Tela Branca (White Screen) com tratamento rigoroso do ESLint e React Compiler.
- [x] **Pesquisa Textual (Caixa):** Caixa de busca de transações conectada efetivamente para vasculhar em tempo real (Filtros Avançados Mobile).
- [x] **Auditoria de Empty States:** Ícones, ilustrações e textos acolhedores revisados para garantir feedback visual humano em telas sem dados cadastrados.
- [x] **Microinterações e Feedback:** Suporte vibratório (Haptics Feedback) e _Toast notifications_ implementados para ações de deleção e pagamento.
