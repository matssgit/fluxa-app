# Sprint 11-B — Análises, projeção e insights (interface da v1)

- Base: `6468182455c36b8fa0b8da263902e6622f75ac7b` (`codex/fluxa-android-sprint-11a-analytics`).
- Branch: `claude/fluxa-android-sprint-11b-analytics-ui`. Worktree: `F:/fluxa-app-worktrees/claude-sprint-11b`.
- O commit de entrega é o que contém este documento. Sem push, merge ou deploy. Checkout principal intocado.
- Produção alterada só em `ui/` e `strings.xml`. Backend, Room v7, SafeToSpend, cartões, assinaturas, metas, crypto e segurança sem diff. Nenhuma dependência nova.

## Analytics screen

`ui/feature/analytics/`: `AnalyticsViewModel` (escolhe o mês, coleta o snapshot, prepara a apresentação) e `AnalyticsScreens` (Compose). Rota `analytics`, aberta por **Mais → Análises** ("Em breve" removido) e **Início → Ver análises**. As quatro abas não mudaram.

Estados: Loading, Content (inclusive parcial e vazio) e Error (overflow, conflito, falha de leitura). Sem botão de atualizar: o Flow do repositório e `financialDays()` (o mesmo sinal das outras telas) recriam a observação na virada do dia.

Ordem adotada: mês → resumo → insights → categorias → 6 meses → próximos 30 dias → assinaturas. Insights sobem para junto do resumo porque falam do mês escolhido; projeção e assinaturas falam de hoje e dizem isso ("A partir de hoje, …").

Seletor de mês: volta sem limite, nunca passa do mês atual. `null` acompanha o mês corrente.

## Dashboard integration

O cartão do mês já existia; ganhou só o botão **Ver análises** e o rótulo "Despesas" (era "Saídas"), igual às Análises. O resumo agora acompanha a virada do mês (antes o mês ficava fixo até recriar o ViewModel e divergiria das Análises).

## Month summary

Entradas, Despesas, Resultado com sinal e cor, quantidade de movimentações e a nota "Compras no cartão contam quando a parcela é paga". Resultado negativo aparece com "−". Mês sem movimentação: frase própria, sem zeros.

## Savings rate

Razão do contrato, uma casa decimal. Sem receita: "Sem receita realizada no período" (nunca 0%). Valor mínimo não vira zero: "<0,1%" / ">−0,1%".

## Categories

Barras horizontais (não donut) com o top 5 do contrato, cor e ícone do cadastro. Percentuais do total, sem renormalizar; fatia pequena aparece como "<1%". Além das cinco: "Mais N categorias", **sem valor "Outras" inventado**.

## Evolution

Barras agrupadas em Canvas (sem biblioteca), animação única lida só na fase de desenho, e lista mês a mês com resultado. Meses vazios valem R$ 0,00.

Cores: o par receita/despesa usado em textos se confunde em deuteranopia (ΔE 5,4 no validador). O gráfico usa petróleo × terracota da própria paleta (ΔE 20,8 no claro, 14,4 no escuro), com posição fixa, legenda e lista. O validador ainda marca esses tons como pouco saturados/fora da faixa de luminosidade; é a identidade do Fluxa e foi aceito.

## Projection

Disponível agora, obrigações conhecidas, saldo projetado (com data), menor saldo projetado (com data) e, como contexto, saldo no fim do mês. Receita confirmada só aparece se for maior que zero. Todos os valores vêm prontos; o teste JVM usa sentinelas que não fecham entre si para provar que a UI não recalcula. Nota educativa e atalho para o Limite seguro, que continua sendo outro conceito.

## Subscriptions

Ativas, equivalente mensal e custo anual projetado, com a explicação de que o equivalente não é uma cobrança mensal.

## Insights

Fatos tipados do contrato (no máximo 4), texto em português, sem tom julgador, score ou conselho. No mês corrente usa "até agora"/"neste mês"; comparação com o mês anterior só existe em meses encerrados (regra da 11-A).

## Empty states

Banco vazio: convite, sem gráficos. Receita sem despesa, despesa sem receita, sem assinaturas, sem obrigações e 6 meses vazios têm frases próprias sem esconder o resto.

## Privacy

Com "Ocultar valores": valores viram "••••••" / "Valor oculto", inclusive na descrição acessível de cada mês e na taxa de economia.
- **Gráfico de 6 meses fica oculto**: a altura relativa denunciaria o sinal do resultado, que o app já esconde no Início.
- Insights que revelam sinal ou variação somem; ficam maior categoria e ausência de despesas (sem valor).
- Cores de sinal ficam neutras.
- Percentuais de categoria continuam visíveis, como o percentual das metas.

## Accessibility

Passe com TalkBack real no AVD: 43 paradas em ordem lógica, cabeçalhos por seção, cada categoria lida como "Mercado. R$ 1.015,55. 49 por cento das despesas" e cada mês como uma frase. O gráfico é uma parada só, que aponta para a lista. A legenda, só visual, saiu da árvore depois desse passe.

## Responsive

`isNarrowForText()` empilha resumo, categorias, evolução, projeção e assinaturas em 320 dp/fonte 1,3. Sem scroll horizontal.

## Visual review

[screens/](screens/): `claro-*`, `escuro-*`, `compacto-*` (320/1,3), `oculto-*`, `vazio-*`, Início e Mais. Dados sintéticos. Fluidez não foi medida: o AVD renderiza por software.

## Tests

- **JVM**: 326 testes, zero falhas (14 novos em `AnalyticsViewModelTest`, 1 no Início).
- **Builds** offline: `assembleDebug`, `assembleRelease` (sem assinatura), `assembleDebugAndroidTest`.
- **Lint** debug e release: "No issues found". O dicionário inglês do lint acusava "anual" como erro de digitação; a string recebeu `tools:ignore="Typos"`. Foi a única mudança depois da matriz e não altera o app.
- **Matriz instrumentada offline** (modo avião, Wi-Fi e dados desligados, PIN descartável):

| Configuração | Total | Aprovados | Pulados | Falhas |
| --- | --- | --- | --- | --- |
| 320×640 (segunda execução) | 183 | 169 | 14 | 0 |
| 360 | 183 | 169 | 14 | 0 |
| 390, sistema escuro | 183 | 169 | 14 | 0 |
| 412 | 183 | 169 | 14 | 0 |
| 360, fonte 1,3 | 183 | 169 | 14 | 0 |
| 320×640, fonte 1,3 | 183 | 169 | 14 | 0 |

  A **primeira** execução de 320×640 teve 2 falhas: `FoundationTest` (esperava o tema padrão e achou LIGHT, deixado por uma execução avulsa anterior no mesmo app instalado) e `LocalSecurityActivityTest.credentialOnlyDevice…` (o prompt real de PIN não concluiu em 15 s). Repetido com o app desinstalado antes, passou inteiro. Nenhum teste ou tempo de espera foi alterado. A causa do tema é inferida; a do PIN não foi investigada além da repetição (o mesmo teste passou nos outros cinco perfis).
  Os 14 pulados são rotinas sob demanda; as que são protocolo rodaram à parte:
- **`AnalyticsFlowTest`** (8 casos, 7 na suíte): receita e despesas criadas pela interface → resumo, categorias, evolução e atalho do Início; compra contada uma vez e desfeita; assinatura resolvida e desfeita sem duplicar; reserva muda a projeção e não o realizado; navegação entre meses; banco vazio; privacidade (nenhum valor em texto, descrição ou estado de toda a tela).
- **Force-stop**: Análises seed/verify em processos diferentes 2/2; `RcProcessDeathTest` 8/8; segurança 2/2; assinaturas 3/3; timeout real 1/1; aparelho sem PIN 1/1.
- **Regressão financeira**: `RcFinancialSmokeTest` e os testes de analytics da 11-A passaram nos seis perfis.
- `RcVisualCapture` ganhou as seções das Análises e `-e fluxaOnly <prefixo>`.

O TalkBack exigiu `adb root` só no AVD descartável; revertido antes da matriz. PIN, display e rede restaurados ao final.

## Feature freeze

**PRODUCT FEATURE FREEZE — v1**

A partir deste commit só entram correções, compatibilidade e segurança. Qualquer ideia nova vai para **POST-V1**: Configurações, transferências, notificações, orçamentos, receita futura confirmada, sync, widgets, exportações, comparações de meses parciais, histórico de projeções, API 36/Play.

## RC2 physical checklist

- [ ] Tudo do checklist da RC1 (`docs/rc1/PHYSICAL_DEVICE_CHECKLIST.md`).
- [ ] Rolagem das Análises no S23: fluidez, animação única das barras.
- [ ] Gráfico e barras de categoria em OLED, claro e escuro, brilho baixo e alto.
- [ ] Análises com fonte do sistema 1,3 e 2,0.
- [ ] TalkBack em português na tela inteira e com valores ocultos.
- [ ] Virada de dia e de mês com o app aberto.
- [ ] Base real (restaurada de backup) com muitos lançamentos: tempo até o conteúdo.
- [ ] Pagar, desfazer, reservar e voltar às Análises sem atualizar.
- [ ] Voltar (gesto) a partir de Início → Ver análises → Ver Limite seguro.
