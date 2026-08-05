# Fluxa - Sistema Financeiro Fullstack

## Design System Oficial

Versão: 2.0 (Arquitetura Feature-Sliced)

---

# 1. OBJETIVO

Este documento define a arquitetura visual oficial do Fluxa.
Todo componente, página, modal, botão, formulário, animação ou elemento visual deve respeitar rigorosamente este documento.
Este Design System possui prioridade superior ao código existente.
Caso exista conflito entre o código atual e este documento, o código deverá ser refatorado para obedecer às regras aqui definidas.

---

# 2. FILOSOFIA

O Fluxa NÃO deve parecer um ERP.
Também não deve parecer um aplicativo bancário tradicional.
O objetivo é transmitir:

- Clareza.
- Organização.
- Leveza.
- Elegância.
- Alto padrão.
- Sofisticação.

A inspiração visual vem de produtos como Apple, Linear, Notion, Stripe e Arc Browser.
Nunca utilizar elementos exagerados.
O design deve "respirar".
O lema principal é: Menos informação visual, mais organização.

---

# 3. PRINCÍPIOS VISUAIS E DE UX

## Regra 1: O Respiro

Espaço em branco é informação. Nunca tentar preencher todos os espaços vazios na tela.

## Regra 2: Hierarquia Imediata

Toda tela deve possuir hierarquia clara. O usuário deve identificar imediatamente:

- Onde está.
- O que é importante.
- O que pode fazer.

## Regra 3: Carga Cognitiva e Calma

A interface deve transmitir calma. É estritamente proibido o uso de:

- Excesso de cores.
- Excesso de sombras.
- Excesso de bordas.
- Excesso de animações.

## Regra 4: Ponto Focal Único

Cada tela deve possuir um único ponto focal. Nunca competir atenção entre vários elementos.

## Regra 5: Privacidade Nativamente Integrada

A privacidade não é um add-on, é uma funcionalidade _core_. Todos os valores financeiros, saldos de carteiras, limites de cartões e valores de faturas devem estar envelopados pelo componente `<PrivacyMask />`, respeitando o contexto global `PrivacyContext`.

---

# 4. PALETA OFICIAL (TOKENS E THEMES)

As cores nunca devem ser utilizadas em seus valores hexadecimais brutos diretamente nos componentes (ex: proibido o uso de `bg-slate-900` ou `text-purple-600`). Toda cor deve nascer dos arquivos oficiais `theme.css` e `tokens.css`.

## Cores Semânticas

- **Primary (`#13312A`):** Verde principal. Representa confiança.
- **Secondary (`#155446`):** Botões principais, links e ações positivas.
- **Accent (`#C69A72`):** Cor de destaque. Usar apenas para KPIs, ícones importantes e pequenos detalhes. Nunca utilizar em grandes áreas.

## Superfícies (Modo Claro & Escuro)

O `ThemeContext` orquestra a alternância entre os temas suportados nativamente:

- **Background (Ergonomia Pine & Sage):** Plano de fundo principal calibrado para alto contraste térmico e luminância, eliminando a fadiga visual.
  - Claro: `#B0C2B7` (Musgo Sálvia Executivo).
  - Escuro: `#0B1D19` (Esmeralda Profundo).
- **Surface (Chapas Metalizadas):** Utilizado para Cards, Inputs e Modais.
  - Claro: `#FFFFFF` (Branco Puro em Gradiente Linear Acetinado).

---

# 5. COMPONENTES E FEATURE-SLICED DESIGN

Para manter a organização, o Frontend adota separação estrita:

- **UI Reutilizável (`src/components/ui/`):** Elementos visuais puros e agnósticos (Botões, Modais, Inputs).
- **Domínios (`src/components/features/`):** Componentes atrelados a regras de negócio (Cartões, Transações, Assinaturas, Carteiras, Analytics).

## Padrões de Componentes UI

Todo componente de interface deve utilizar variantes estruturadas. Nunca criar botões únicos diretamente dentro de páginas.

- **Button:** `variant="primary"`, `variant="secondary"`, `variant="ghost"`, `variant="danger"`.
- **Card (Estética Neumórfica Metalizada):**
  - `variant="default"`: Gradiente linear acetinado, microborda chanfrada a laser de 1px e sombra 3D de alta profundidade para flutuação de 16px.
  - `variant="interactive"`: Efeito de gravidade tátil, com elevação física de -6px e borda da marca no _hover_.
- **Badge:** `success`, `warning`, `danger`, `neutral`.
- **Input:** `default`, `error`, `disabled`, `focused`.
- **Modal:** `small`, `medium`, `large`, `fullscreen`.

---

# 6. TIPOGRAFIA, ESPAÇAMENTO E SOMBRAS

## Tipografia

Uma única família tipográfica deve ser mantida, com hierarquia fixa (`H1`, `H2`, `H3`, `Body`, `Caption`, `Small`). Nunca alterar pesos arbitrariamente fora dessas diretrizes.

## Espaçamento e Border Radius

- **Espaçamento Oficial:** 4, 8, 12, 16, 24, 32, 48, 64. Nunca utilizar valores aleatórios.
- **Radius:** 6, 8, 12, 16. Nunca utilizar `rounded-full` sem necessidade explícita no layout.

## Sombras e Animações

- **Sombras:** Extremamente discretas e calibradas para profundidade física (`xs`, `sm`, `md`). A sombra `md` aplica um efeito de dispersão dupla com tintura verde suave para criar flutuação 3D em cards.
- **Animações:** Durações permitidas de 150ms, 200ms, 250ms. Jamais utilizar animações lentas.

---

# 7. ORIENTAÇÕES POR DOMÍNIO

- **Dashboard (Central de Comando):** O Dashboard deve mostrar a situação financeira de forma consolidada e permitir decisões rápidas. Nunca deve ser tratado como uma tela de CRUD e nunca deve esconder informações cruciais atrás de múltiplos cliques. Métricas detalhadas (Analytics) devem viver em telas próprias.
- **Cartões:** Tela responsável exclusivamente por faturas, limites, compras de crédito e parcelas.
- **Assinaturas:** Representam cobranças recorrentes e automáticas. Não devem ser confundidas com compras comuns ou parcelas de cartão.
- **Carteiras (Wallets) e Contas:** Separar visualmente o que compõe o saldo de fluxo de caixa diário (Contas) do que representa separações para objetivos e guardas de valor (Carteiras).
- **Performance:** Sempre utilizar `Skeleton` loadings para dados assíncronos. Nunca bloquear a tela inteira com _spinners_ longos.
