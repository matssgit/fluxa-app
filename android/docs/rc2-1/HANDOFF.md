# RC2.1 — Correções cosméticas finais

## Base

- Base: `27c7c86206a5f8e296074d2e887e408ab634d833` (`claude/fluxa-android-sprint-11b-analytics-ui`).
- Branch: `claude/fluxa-android-rc2-1-final-polish`. Worktree: `F:/fluxa-app-worktrees/claude-rc2-1`.
- O commit de entrega é o que contém este documento. Sem push, merge ou deploy. Checkout principal intocado.
- O commit de empacotamento da RC2 (`49fe7b207103eceea126eb7ec39570db00b922c1`) **não** está nesta árvore. Ele não muda `app/src/main`, e nenhum arquivo dele é alterado aqui.

**Atenção ao empacotar:** a RC2 instalada no S23 tem versionCode 2 e esta árvore tem versionCode 1. Um APK compilado daqui **não atualiza** a RC2 (o Android recusa como downgrade). Antes de gerar o APK definitivo, juntar `49fe7b2` e subir para versionCode 3, assinando com o mesmo certificado. Não mexi na versão porque está fora do escopo deste patch.

## Changes

Produção: `PlaceholderScreens.kt`, `AnalyticsScreens.kt`, `FluxaButton.kt`, `FluxaIcons.kt` e `strings.xml`. Domínio, Room, analytics, backup, segurança, SafeToSpend e navegação inferior sem diff.

## Removed placeholders

- Mais → **Configurações / "Em breve"**: linha, selo, textos e ícone sem uso removidos. Nada foi criado no lugar.
- Busca por "Em breve", "Coming soon" e equivalentes em código e textos: **nenhum outro item visível**. "Indisponível" (saldo, conta, forma de pagamento) é estado de dado, mantido. `placeholder_*` são nomes técnicos de strings e do arquivo.

## More screen

Seis destinos, todos funcionais. O divisor só existe entre linhas, então o card termina limpo em "Segurança". Em 320×640 (fonte 1,0) os seis cabem acima da navegação; com fonte 1,3 o card rola normalmente.

## Analytics empty state

Mês sem nenhuma movimentação: a seção "Gastos por categoria" some inteira, com o título. Mês com movimentações e sem despesas: a seção fica, com "Nenhuma despesa realizada neste mês." A mensagem de 6 meses não mudou.

## Month selector disabled state

A seta desabilitada usa o token `textMuted` em vez de 45% de opacidade. Contraste contra o fundo: claro 2,6 → 4,9; escuro 4,0 → 7,4. Continua distinta da habilitada (12,2 e 15,2) e continua não clicável. Só esta seta mudou; os demais botões desabilitados seguem iguais.

## Projection visual polish

- "Menor saldo projetado" e "Fim do mês" dividem uma linha quando cabem; empilham em 320 dp ou fonte grande.
- Um divisor a menos e mais respiro entre grupos (10 → 14 dp).
- As duas notas viraram texto de apoio: 11 sp, peso normal, coladas. Texto, posição e CTA iguais; nada escondido.

## Accessibility

TalkBack real no AVD: ordem do Mais e das Análises preservada; "Próximo mês. Button. disabled"; sem cabeçalho órfão no mês vazio; "Saldo projetado no fim do mês, 30 de novembro de 2026. R$ …" continua por extenso apesar do rótulo curto.

## Privacy

Comportamento intacto. `hiddenAmountsLeaveNoMoneyInTheScreenOrItsSemantics` passou nos três perfis e as capturas `oculto-*` foram revisadas.

## Tests

- JVM: 326, zero falhas.
- Suíte instrumentada completa, offline, primeira execução: 320×640, 320×640 com fonte 1,3 e 390 escuro, cada uma com 185 testes, 171 aprovados, 14 sob demanda pulados, 0 falhas.
- Novos: `monthWithoutMovementsDoesNotRepeatTheEmptyMessageOrLeaveAnOrphanHeading` e `moreListsOnlyWorkingDestinations`.
- `assembleDebug`, `assembleRelease` (sem assinatura) e `assembleDebugAndroidTest` aprovados. Lint debug e release: "No issues found".
- Não repetidos, por não haver mudança de lógica: perfis 360, 412 e 360/1,3, e os protocolos de force-stop.
- Não testado em aparelho físico: o contraste da seta e a densidade do card precisam do aval no S23.

## Screenshots

[screens/](screens/): `mais-*`, `analises-mes-vazio-*`, `analises-banco-vazio-*`, `analises-dados-*`, `projecao-*`, `oculto-*`. Viewport de 360 dp (equivalente ao S23) e 320 dp. Dados sintéticos.

## Feature freeze

**PRODUCT FEATURE FREEZE — ACTIVE.** Nenhuma funcionalidade adicionada. Ideias novas continuam em POST-V1.
