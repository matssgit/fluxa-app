# Fluxa Android — Design System (Sprint 0)

Pacote: `com.fluxa.personal.ui.designsystem`. Catálogo visual: `designsystem/catalog/FluxaCatalogPreviews.kt` (abrir no Android Studio, modo Split/Design).

## Regras de uso

1. **Cores só por token**: `FluxaTheme.colors.*`. Nada de `Color(0x…)` em telas. Todos os pares texto/fundo passam AA (≥ 4.5:1) e são verificados em `FluxaDesignTokensTest`.
2. **Tipografia só pela escala**: `FluxaTheme.typography.*` (display, headline, title, titleSmall, body, bodyStrong, label, caption, overline, amountHero, amount). Mínimo 12sp. Sempre `sp`.
3. **Formas fechadas**: `small 12`, `medium 16`, `large 24`, `extraLarge 32`, `pill`, `sheet`, `leaf`. A forma **leaf** (folha) só em destaque: saldo, cartão em foco, meta em foco (`FluxaCard(featured = true)`).
4. **Profundidade**: `Depth0` (afundado), `Depth1` (cards), `Depth2` (hero, FAB), `Depth3` (flutuantes). Sombra suave + borda quase invisível + highlight no topo.
5. **Glass só em flutuantes**: navegação, controles flutuantes, chips ativos, FAB. Sheets usam superfície opaca (`surfaceRaised`) com o brilho do glass, porque a 86% o conteúdo de trás vazava sob o texto. `FluxaCard` recusa `tone = Glass`. Sem blur de fundo (custo fixo, sem animação).
6. **Todo valor financeiro passa por `FluxaAmount(cents = …)`**. Respeita `LocalAmountsHidden` e mostra `••••••`, anunciando "Valor oculto".
7. **Movimento** por `fluxaTween()` / `fluxaSpring()` / `Modifier.fluxaPressScale()`. Viram `snap()` quando o sistema remove animações.
8. **Acessibilidade**: alvo de toque ≥ 48dp, `contentDescription` obrigatório em `FluxaIconButton`, estados não dependem só de cor (check no chip, ícone e texto no erro, ícone sólido e rótulo em negrito na navegação).

## Primitives

| Componente | Equivalente M3 |
|---|---|
| `FluxaSurface(tone, shape, depth)` | `Surface` |
| `FluxaCard(tone, featured, onClick)` | `Card` / `ElevatedCard` |
| `FluxaButton(variant = Primary/Secondary/Glass/Destructive, size, loading)` | `Button` / `FilledTonalButton` |
| `FluxaIconButton(variant = Standard/Tonal/Glass)` | `IconButton` |
| `FluxaInput(label, placeholder, supportingText, errorText)` | `OutlinedTextField` |
| `FluxaChip(selected)` | `FilterChip` |
| `FluxaTopBar(title, overline, actions)` | `TopAppBar` |
| `FluxaNavigationBar(items, primaryAction)` | `NavigationBar` + `FloatingActionButton` |
| `FluxaBottomSheet` / `FluxaSheetLayout` | `ModalBottomSheet` |
| `FluxaSectionHeader` | — |
| `FluxaAmount` | — |

Ícones: `FluxaIcons` (próprios, grade 24, traço 1.75, variantes sólidas para estado ativo), sem `material-icons-extended`.
