# Fluxa QA Checklist - Release Final (Validação Concluída)

Todas as suítes de testes listadas abaixo foram executadas e validadas com sucesso.

## 1. Integridade de Dados & Ciclo de Vida

- [x] Reset do ambiente (Migrações + Seeds limpas)
- [x] Cadastro de Contas, Cartões e Categorias
- [x] Cadastro de Receitas
- [x] Cadastro de Despesas
- [x] Cadastro de Compra Parcelada
- [x] Cadastro de Assinatura
- [x] Transferência entre carteiras

## 2. Super Omnibox & Pesquisa

- [x] Pesquisa por Título (e.g., "Netflix")
- [x] Pesquisa por Merchant (e.g., "Mercado")
- [x] Pesquisa por Notas (e.g., "aniversário")
- [x] Pesquisa por Termos Cruzados (e.g., "Nubank compras")
- [x] Pesquisa por Valor (via filtros avançados)
- [x] Pesquisa por Categoria/Conta (via filtros avançados)

## 3. Experiência do Usuário (Matrioska & Contexto)

- [x] Abertura do Painel de Contexto (Matrioska)
- [x] Validação de Parcelas (3/12, progresso visual)
- [x] Validação de Assinaturas (próxima cobrança)
- [x] Botão "Dar Baixa" (Status Pendente -> Concluído)
- [x] Fechamento fluido do painel (Overlay/Close)

## 4. Filtros & Paginação

- [x] Filtros Rápidos (Hoje, Semana, Mês)
- [x] Filtros Avançados (Drawer responsivo)
- [x] Limpeza de filtros (Botão "Limpar" na Toolbar)
- [x] Infinite Scroll (Paginação infinita funcionando)
- [x] Persistência de filtros ao navegar

## 5. Mobile & Responsividade

- [x] Modal de detalhes centralizado (Safe Area)
- [x] Drawer de filtros sem quebrar layout
- [x] Barra de navegação não sobreposta
- [x] Touch targets (botões clicáveis com facilidade)
- [x] Scroll interno no mobile quando há excesso de dados

## 6. Estados de Tela

- [x] Skeleton loading (transições suaves)
- [x] Empty states (quando busca retorna vazio)
- [x] Feedback visual de "Carregando mais"
