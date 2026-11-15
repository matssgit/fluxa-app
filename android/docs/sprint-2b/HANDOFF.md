# Sprint 2-B — Contas e Categorias (frontend)

Base: `codex/fluxa-android-sprint-2` @ `d62582a`. Usa apenas `AccountRepository`, `CategoryRepository`, `Money` e os modelos de domínio existentes. Schema Room, DAOs, repositórios e regras do Codex não foram alterados.

## Onde fica cada coisa

| Caminho | Conteúdo |
|---|---|
| `ui/feature/accounts/` | `AccountsViewModel`, `AccountFormViewModel`, `AccountsScreen`, `AccountFormScreen`, rotas |
| `ui/feature/categories/` | `CategoriesViewModel`, `CategoryFormViewModel`, `CategoriesScreen`, `CategoryFormScreen`, rotas |
| `ui/feature/catalog/CatalogVisuals.kt` | Ícones/cores de categoria e visual dos tipos de conta |
| `ui/common/MoneyInput.kt` | Entrada BRL → `Money` (sem Float/Double) |
| `ui/common/FeatureScaffolding.kt` | Tela secundária com ação fixa, estados de carregamento/erro/vazio, Switch, confirmação |
| `ui/FluxaNavigation.kt` | Rotas novas; barra flutuante só nas quatro abas |

## Navegação

- Abas preservadas: Início · Caixa · + · Cartões · Mais.
- **Caixa** ganha um cartão “Suas contas” (contagem de ativas) que abre `accounts`. O resto da aba continua reservado às movimentações.
- **Mais → Categorias** abre `categories`. Os demais itens seguem “Em breve”.
- Rotas: `accounts`, `accounts/new`, `accounts/{id}/edit`, `categories`, `categories/new?kind=`, `categories/{id}/edit`.
- Fora das abas a barra flutuante some e o botão Salvar fica fixo no rodapé, acima do teclado e da barra de gestos.

## Decisões de UI sobre o contrato

- **Saldo**: só o saldo inicial é mostrado, rotulado como tal, com a data de referência. Nenhum saldo atual é calculado.
- **Valor negativo**: o campo recebe o valor absoluto e o sinal vem do controle “Saldo negativo” (o teclado numérico nem sempre tem “-”). `MoneyInput` aceita `0`, `0,00`, `1.234,56`, `1234,5`; rejeita mais de 2 decimais (`Money.fromReais` com `UNNECESSARY`), agrupamento inválido, sinal no texto e valores acima de 13 dígitos inteiros.
- **Validação de nome**: vem do `init` de `AccountDetails`/`CategoryDetails` (domínio). Nomes repetidos continuam permitidos.
- **Ícone e cor de categoria**: `icon` guarda uma chave (`cart`, `home`, `food`, `transport`, `health`, `work`, `gift`, `education`, `utilities`, `subscription`, `savings`, `tag`); `color` guarda `#RRGGBB` da paleta Fluxa. Valores desconhecidos caem no padrão (etiqueta, sálvia). A cor é decorativa: o ícone é desenhado em `textPrimary` sobre a cor a 22%, e a categoria sempre tem nome.
- **Arquivamento**: conta e categoria arquivam com confirmação no sheet padrão. Conta **não** tem reativação (o contrato não oferece). Categoria reativa na lista (seção Arquivadas) e no formulário.
- **Toques repetidos**: salvar/arquivar/reativar ignoram novos toques enquanto a operação está em curso.

## Testes

- JVM: `MoneyInputTest`, `AccountsViewModelsTest`, `CategoriesViewModelsTest`. Os ViewModels rodam sobre os **repositórios reais** com DAOs em memória (`FakeDaos.kt`) que reproduzem filtros, ordenação e transições condicionais do Room.
- Instrumentados: `AccountsAndCategoriesFlowTest` (CRUD de conta, erro de valor, CRUD/arquivar/reativar categoria, fechar e reabrir a atividade).
- `VisualReviewCapture` só roda sob demanda. Com o app e o APK de teste instalados:

```
adb shell am instrument -w -e class com.fluxa.personal.ui.VisualReviewCapture -e fluxaCapture 390-light com.fluxa.personal.test/androidx.test.runner.AndroidJUnitRunner
```

Ele limpa o banco, popula dados de exemplo e salva PNGs em `/sdcard/Android/data/com.fluxa.personal/files/captures`. Largura, tema e fonte são definidos antes via `adb shell wm density`, `cmd uimode night` e `settings put system font_scale`.

## Pendências para as próximas sprints

1. Conta arquivada não pode ser reativada (contrato). Se o produto quiser, o Codex precisa expor `AccountRepository.reactivate`.
2. O estado dos formulários não sobrevive à morte do processo no meio da edição (os dados salvos sobrevivem). Mover os campos para `SavedStateHandle` se for requisito.
3. Mudança de tipo de categoria já referenciada: quando lançamentos existirem, decidir se o formulário bloqueia a troca (README do Codex já sinaliza).
4. Ocultar valores ainda vale só para a sessão (`LocalPreferences` sem escrita).
5. Tablet: conteúdo limitado a 640dp e centralizado; `NavigationRail` não foi implementado.
