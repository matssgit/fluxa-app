Eu faria uma mudança importante antes de qualquer nova feature.

O roadmap atual ainda está muito orientado por **telas** ("Dashboard", "Cartões"...). Conforme o projeto cresce, isso começa a atrapalhar porque mistura implementação com produto.

Eu transformaria esse documento em um **roadmap de módulos**, igual empresas maiores fazem (Notion, Jira, Linear, etc.).

Assim o Gemini sempre sabe exatamente o contexto antes de gerar código.

---

# FRONTEND_ROADMAP.md

**Finance App Beta V2**
**Frontend Roadmap**
**Versão: 2.0**

---

# OBJETIVO

Este documento controla exclusivamente a evolução do Frontend.

O Frontend é responsável apenas por:

* Interface
* Experiência do usuário (UX)
* Acessibilidade
* Navegação
* Consumo das APIs

### Regras Imutáveis

* O Frontend nunca realiza cálculos financeiros.
* Toda regra de negócio pertence ao Backend.
* Toda tela deve consumir dados através de React Query.
* Nenhuma informação financeira poderá ser recalculada localmente.
* Toda comunicação deve ocorrer através dos serviços da pasta `/services`.
* Todo estado global deve ser mínimo.
* Componentes devem ser reutilizáveis.
* Atomic Hooks são obrigatórios.

---

# STATUS GERAL

| Módulo          | Status         |
| --------------- | -------------- |
| Dashboard       | 🚧 Em evolução |
| Fluxo de Caixa  | ✅ Estável      |
| Cartões         | ✅ Estável      |
| Assinaturas     | ✅ Estável      |
| Navegação       | 🚧 Em evolução |
| Contas          | 🚧 Em evolução |
| Configurações   | ⏳ Planejado    |
| Pesquisa Global | ⏳ Planejado    |
| Wallets         | ⏳ Planejado    |
| Métricas        | ⏳ Planejado    |
| Aparência       | ⏳ Planejado    |
| UX              | 🚧 Em evolução |

---

# MÓDULO — Dashboard

Status: 🚧 Em evolução

## Implementado

* [x] Cards de Entradas
* [x] Cards de Saídas
* [x] Saldo Atual
* [x] Projeção do mês
* [x] Timeline híbrida
* [x] Lista de pendências
* [x] Atualização automática via React Query
* [x] Defensive Rendering
* [x] Contrato único consumindo DashboardResponse

## Pendências

* [ ] Skeleton completo
* [ ] Responsividade mobile
* [ ] Cards recolhíveis
* [ ] Atalhos rápidos
* [ ] Melhor organização visual

---

# MÓDULO — Fluxo de Caixa

Status: ✅ Funcional

## Implementado

* [x] Entradas
* [x] Saídas
* [x] Histórico
* [x] Contas
* [x] Categorias
* [x] Timeline integrada
* [x] Pendências

## Melhorias Futuras

* [ ] Histórico infinito
* [ ] Agrupar movimentações por dia
* [ ] Ícones por categoria
* [ ] Ordenações personalizadas

---

# MÓDULO — Cartões

Status: ✅ Estável

## Implementado

* [x] Cadastro
* [x] Edição
* [x] Exclusão protegida
* [x] Personalização de cores
* [x] Barra dinâmica de limite
* [x] Compras
* [x] Parcelamento
* [x] Cancelamento
* [x] Pagamento
* [x] Toasts
* [x] Atualização automática
* [x] Modais integrados

## Melhorias Futuras

* [ ] Melhor visualização das compras
* [ ] Indicadores gráficos de utilização
* [ ] Histórico filtrável

---

# MÓDULO — Assinaturas

Status: ✅ Funcional

Objetivo

Representar cobranças recorrentes que não são compras.

## Implementado

* [x] Cadastro
* [x] Categoria
* [x] Conta
* [x] Cartão
* [x] Frequência
* [x] Valor
* [x] Próximo vencimento
* [x] Ativar
* [x] Pausar
* [x] Pagamento
* [x] Histórico
* [x] Timeline
* [x] Pendências
* [x] Participação na Projeção

## Melhorias Futuras

* [ ] Edição em lote
* [ ] Busca rápida
* [ ] Agrupamento por categoria

---

# MÓDULO — Navegação

Status: 🚧 Em evolução

Objetivo

Transformar o sistema em uma aplicação com navegação fluida.

## Implementado

* [x] Estrutura inicial
* [x] Navegação entre páginas

## Pendências

* [ ] Sidebar Desktop definitiva
* [ ] Bottom Navigation definitiva
* [ ] Melhor UX Mobile
* [ ] Organização visual
* [ ] Indicadores da página atual

---

# MÓDULO — Contas

Status: 🚧 Em evolução

Objetivo

Transformar "Contas" em um módulo independente.

## Implementado

* [x] CRUD
* [x] Integração Dashboard

## Pendências

* [ ] Tela própria
* [ ] Acesso via Sidebar
* [ ] Melhor gerenciamento

---

# MÓDULO — Configurações

Status: ⏳ Planejado

Objetivo

Centralizar preferências do usuário.

## Planejado

* [ ] Perfil
* [ ] Nome
* [ ] Foto
* [ ] Senha
* [ ] Preferências
* [ ] Backup
* [ ] Exportações
* [ ] Tema

---

# MÓDULO — Aparência

Status: ⏳ Planejado

## Planejado

* [ ] Tema Claro
* [ ] Tema Escuro
* [ ] Tema Sistema
* [ ] Persistência da preferência
* [ ] Transições suaves

---

# MÓDULO — Pesquisa Global

Status: ⏳ Planejado

Objetivo

Encontrar qualquer informação do sistema.

## Pesquisar por

* [ ] Transações
* [ ] Compras
* [ ] Parcelas
* [ ] Assinaturas
* [ ] Cartões
* [ ] Contas
* [ ] Categorias
* [ ] Datas
* [ ] Valores
* [ ] Texto livre

## Filtros

* [ ] Período
* [ ] Valor
* [ ] Status
* [ ] Categoria
* [ ] Conta
* [ ] Cartão

---

# MÓDULO — Wallets (Caixinhas)

Status: ⏳ Planejado

Objetivo

Guardar dinheiro para objetivos.

## Planejado

* [ ] Criar Wallet
* [ ] Definir meta
* [ ] Valor atual
* [ ] Valor objetivo
* [ ] Barra de progresso
* [ ] Transferências
* [ ] Histórico

---

# MÓDULO — Métricas

Status: ⏳ Planejado

Objetivo

Transformar dados financeiros em indicadores úteis.

## Planejado

* [ ] Gastos por categoria
* [ ] Gastos mensais
* [ ] Evolução patrimonial
* [ ] Evolução de receitas
* [ ] Evolução de despesas
* [ ] Comparativo mensal
* [ ] Tendências

---

# MÓDULO — UX

Status: 🚧 Em evolução

## Implementado

* [x] Toasts
* [x] Empty States
* [x] Personalização de cartões

## Pendências

* [ ] Skeleton Global
* [ ] Melhor responsividade
* [ ] Acessibilidade
* [ ] Navegação por teclado
* [ ] Estados de erro padronizados
* [ ] Estados vazios padronizados
* [ ] Animações consistentes

---

# MVP V1

## Obrigatório para finalizar

### Interface

* [ ] Tema Claro/Escuro
* [ ] Sidebar Desktop
* [ ] Bottom Navigation
* [ ] Tela de Contas
* [ ] Tela de Configurações

### Experiência

* [ ] Pesquisa Global
* [ ] Skeleton Global
* [ ] Responsividade completa
* [ ] Melhor UX Mobile

### Funcionalidades

* [x] Dashboard
* [x] Fluxo de Caixa
* [x] Cartões
* [x] Assinaturas
* [ ] Wallets
* [ ] Métricas Financeiras

---

## Observação

Acho que essa versão é bem mais escalável. Ela não foi pensada apenas para terminar a V1, mas para continuar servindo quando o projeto chegar na V2 e V3. O Gemini também tende a se orientar melhor com uma organização por **módulos**, porque cada módulo deixa claro o objetivo, o que já existe e o que ainda falta, reduzindo a chance de ele "inventar" funcionalidades ou perder o contexto do projeto.


---

# FRONTEND_ROADMAP.md

**Finance App Beta V2**
**Frontend Roadmap**
**Versão: 2.0**

---

# OBJETIVO

Este documento controla exclusivamente a evolução do Frontend.

O Frontend é responsável apenas por:

* Interface
* Experiência do usuário (UX)
* Acessibilidade
* Navegação
* Consumo das APIs

### Regras Imutáveis

* O Frontend nunca realiza cálculos financeiros.
* Toda regra de negócio pertence ao Backend.
* Toda tela deve consumir dados através de React Query.
* Nenhuma informação financeira poderá ser recalculada localmente.
* Toda comunicação deve ocorrer através dos serviços da pasta `/services`.
* Todo estado global deve ser mínimo.
* Componentes devem ser reutilizáveis.
* Atomic Hooks são obrigatórios.

---

# STATUS GERAL

| Módulo          | Status         |
| --------------- | -------------- |
| Dashboard       | 🚧 Em evolução |
| Fluxo de Caixa  | ✅ Estável      |
| Cartões         | ✅ Estável      |
| Assinaturas     | ✅ Estável      |
| Navegação       | 🚧 Em evolução |
| Contas          | 🚧 Em evolução |
| Configurações   | ⏳ Planejado    |
| Pesquisa Global | ⏳ Planejado    |
| Wallets         | ⏳ Planejado    |
| Métricas        | ⏳ Planejado    |
| Aparência       | ⏳ Planejado    |
| UX              | 🚧 Em evolução |

---

# MÓDULO — Dashboard

Status: 🚧 Em evolução

## Implementado

* [x] Cards de Entradas
* [x] Cards de Saídas
* [x] Saldo Atual
* [x] Projeção do mês
* [x] Timeline híbrida
* [x] Lista de pendências
* [x] Atualização automática via React Query
* [x] Defensive Rendering
* [x] Contrato único consumindo DashboardResponse

## Pendências

* [ ] Skeleton completo
* [ ] Responsividade mobile
* [ ] Cards recolhíveis
* [ ] Atalhos rápidos
* [ ] Melhor organização visual

---

# MÓDULO — Fluxo de Caixa

Status: ✅ Funcional

## Implementado

* [x] Entradas
* [x] Saídas
* [x] Histórico
* [x] Contas
* [x] Categorias
* [x] Timeline integrada
* [x] Pendências

## Melhorias Futuras

* [ ] Histórico infinito
* [ ] Agrupar movimentações por dia
* [ ] Ícones por categoria
* [ ] Ordenações personalizadas

---

# MÓDULO — Cartões

Status: ✅ Estável

## Implementado

* [x] Cadastro
* [x] Edição
* [x] Exclusão protegida
* [x] Personalização de cores
* [x] Barra dinâmica de limite
* [x] Compras
* [x] Parcelamento
* [x] Cancelamento
* [x] Pagamento
* [x] Toasts
* [x] Atualização automática
* [x] Modais integrados

## Melhorias Futuras

* [ ] Melhor visualização das compras
* [ ] Indicadores gráficos de utilização
* [ ] Histórico filtrável

---

# MÓDULO — Assinaturas

Status: ✅ Funcional

Objetivo

Representar cobranças recorrentes que não são compras.

## Implementado

* [x] Cadastro
* [x] Categoria
* [x] Conta
* [x] Cartão
* [x] Frequência
* [x] Valor
* [x] Próximo vencimento
* [x] Ativar
* [x] Pausar
* [x] Pagamento
* [x] Histórico
* [x] Timeline
* [x] Pendências
* [x] Participação na Projeção

## Melhorias Futuras

* [ ] Edição em lote
* [ ] Busca rápida
* [ ] Agrupamento por categoria

---

# MÓDULO — Navegação

Status: 🚧 Em evolução

Objetivo

Transformar o sistema em uma aplicação com navegação fluida.

## Implementado

* [x] Estrutura inicial
* [x] Navegação entre páginas

## Pendências

* [ ] Sidebar Desktop definitiva
* [ ] Bottom Navigation definitiva
* [ ] Melhor UX Mobile
* [ ] Organização visual
* [ ] Indicadores da página atual

---

# MÓDULO — Contas

Status: 🚧 Em evolução

Objetivo

Transformar "Contas" em um módulo independente.

## Implementado

* [x] CRUD
* [x] Integração Dashboard

## Pendências

* [ ] Tela própria
* [ ] Acesso via Sidebar
* [ ] Melhor gerenciamento

---

# MÓDULO — Configurações

Status: ⏳ Planejado

Objetivo

Centralizar preferências do usuário.

## Planejado

* [ ] Perfil
* [ ] Nome
* [ ] Foto
* [ ] Senha
* [ ] Preferências
* [ ] Backup
* [ ] Exportações
* [ ] Tema

---

# MÓDULO — Aparência

Status: ⏳ Planejado

## Planejado

* [ ] Tema Claro
* [ ] Tema Escuro
* [ ] Tema Sistema
* [ ] Persistência da preferência
* [ ] Transições suaves

---

# MÓDULO — Pesquisa Global

Status: ⏳ Planejado

Objetivo

Encontrar qualquer informação do sistema.

## Pesquisar por

* [ ] Transações
* [ ] Compras
* [ ] Parcelas
* [ ] Assinaturas
* [ ] Cartões
* [ ] Contas
* [ ] Categorias
* [ ] Datas
* [ ] Valores
* [ ] Texto livre

## Filtros

* [ ] Período
* [ ] Valor
* [ ] Status
* [ ] Categoria
* [ ] Conta
* [ ] Cartão

---

# MÓDULO — Wallets (Caixinhas)

Status: ⏳ Planejado

Objetivo

Guardar dinheiro para objetivos.

## Planejado

* [ ] Criar Wallet
* [ ] Definir meta
* [ ] Valor atual
* [ ] Valor objetivo
* [ ] Barra de progresso
* [ ] Transferências
* [ ] Histórico

---

# MÓDULO — Métricas

Status: ⏳ Planejado

Objetivo

Transformar dados financeiros em indicadores úteis.

## Planejado

* [ ] Gastos por categoria
* [ ] Gastos mensais
* [ ] Evolução patrimonial
* [ ] Evolução de receitas
* [ ] Evolução de despesas
* [ ] Comparativo mensal
* [ ] Tendências

---

# MÓDULO — UX

Status: 🚧 Em evolução

## Implementado

* [x] Toasts
* [x] Empty States
* [x] Personalização de cartões

## Pendências

* [ ] Skeleton Global
* [ ] Melhor responsividade
* [ ] Acessibilidade
* [ ] Navegação por teclado
* [ ] Estados de erro padronizados
* [ ] Estados vazios padronizados
* [ ] Animações consistentes

---

# MVP V1

## Obrigatório para finalizar

### Interface

* [ ] Tema Claro/Escuro
* [ ] Sidebar Desktop
* [ ] Bottom Navigation
* [ ] Tela de Contas
* [ ] Tela de Configurações

### Experiência

* [ ] Pesquisa Global
* [ ] Skeleton Global
* [ ] Responsividade completa
* [ ] Melhor UX Mobile

### Funcionalidades

* [x] Dashboard
* [x] Fluxo de Caixa
* [x] Cartões
* [x] Assinaturas
* [ ] Wallets
* [ ] Métricas Financeiras

---

