# Sprint 7-B — Limite seguro (frontend)

Base: `codex/fluxa-android-sprint-7a` @ `be953ae`. Nenhuma alteração em `SafeToSpendCalculator`, Room v7, migrations, domínio, repositórios, deduplicação ou regras temporais. A UI só exibe `observeSafeToSpend(horizonDays = 30)`.

Na UI o nome é sempre **Limite seguro** (nunca "SafeToSpend"). Texto de apoio: "Quanto você pode gastar sem comprometer suas obrigações conhecidas nos próximos 30 dias."

## Dashboard

- Card `SafeToSpendCard` logo abaixo de "Saldo disponível". A ordem fica: disponível → limite seguro → mês atual → recentes.
- O valor usa o tamanho `headline`, menor que o saldo principal (`amountHero`). O card é claro (`FluxaCard`) e não compete com o card escuro do disponível.
- O card mostra:
  - sobrelinha "LIMITE SEGURO" com ícone;
  - o valor;
  - uma linha de situação;
  - "Menor folga em 9 dez. 2026" ou "Menor folga: hoje";
  - o botão "Entender cálculo".
- Atualiza sozinho por Flow. Não há botão de atualizar, gauge, termômetro nem classificação "confortável/atenção".
- "Disponível" e "Limite seguro" ficam separados: o card do disponível não mudou, e o detalhe explica a diferença entre eles.

## Limite seguro

`safeAmount` é exibido como veio. A cor terracota com ícone e texto aparece só quando o limite é zero (zero por obrigações ou disponível negativo). Não há limiar arbitrário.

## Detail

Rota `safe-to-spend` (tela `FeatureScreen`, sobrelinha "PRÓXIMOS 30 DIAS"). Ela tem:
- **Destaque** com o limite e o texto de apoio.
- **Linha de situação**, só quando não é o caso "reduzido", que o texto de apoio já explica.
- **Como chegamos a esse valor**, com estas linhas:
  - Disponível agora: `availableNow`, com a legenda "Dinheiro livre hoje, depois das reservas de metas.";
  - Comprometido: `availableNow − safeAmount`, só para exibir e só quando o disponível ≥ 0 e o limite < disponível;
  - Limite seguro;
  - Menor folga: data ou "Hoje";
  - Saldo projetado: `lowestProjectedBalance`;
  - Próximos 30 dias: de `horizonStart` até `horizonEndExclusive − 1`.
- **Obrigações consideradas**: o breakdown.
- **Notas**:
  - "Valores reservados em metas já foram descontados antes deste cálculo.";
  - o limite do cartão não é dinheiro disponível;
  - "O cálculo considera apenas informações registradas no Fluxa…".

## Breakdown

- A lista é exatamente `result.breakdown`, na ordem do backend (inclusive no mesmo dia). Ela termina na menor folga: a UI não acrescenta obrigações posteriores e não reordena nada.
- Cada linha mostra:
  - a data efetiva, ou "Hoje";
  - o nome;
  - a fonte: "Assinatura", "Assinatura · Cartão", "Parcela · Cartão" ou "Receita confirmada" (nunca o enum);
  - "− R$ X", ou "+ R$ X" para receita confirmada.
- Os nomes vêm de `CashEventLabels`, buscados **só pelos IDs estruturais** do evento:
  - assinatura: `observeOccurrenceDetail(id, occurrenceDate)` → nome;
  - parcela: compra + parcela → "Notebook 1/10".
- Esses rótulos não somam, não filtram e não deduplicam. Se faltar o rótulo, a linha mostra o tipo ("Assinatura", "Parcela de cartão").
- Obrigação vencida: o backend a aplica hoje. A linha mostra "Hoje" e a nota "Vencida em 16 nov. 2026, conta hoje", com a data original vinda da chave da ocorrência.
- Tocar numa assinatura abre a ocorrência; tocar numa parcela abre a parcela.

## Zero

Disponível ≥ 0 e `safeAmount = 0`: "Suas obrigações conhecidas já utilizam todo o valor disponível dentro deste período." (alerta com ícone).

Sem obrigações (`safeAmount = availableNow`): "Nenhuma obrigação conhecida reduz seu limite neste período."

## Negative availability

`availableNow < 0`: o limite mostra R$ 0,00 (vem do backend) com "Seu saldo disponível já está negativo.". O detalhe exibe o disponível negativo em terracota e não mostra a linha "Comprometido".

## Privacy

- Todos os valores passam por `FluxaAmount`: "ocultar valores" mostra "••••••" e anuncia "Valor oculto".
- Descrições de acessibilidade, `stateDescription`, notas e rótulos levam só datas e nomes, nunca valores.
- Teste instrumentado: com valores ocultos, nem o card, nem o destaque, nem as linhas do breakdown contêm "R$".

## Day change

`SafeToSpendViewModel` recebe `days: Flow<LocalDate>` (padrão `financialDays()`, o mesmo das Assinaturas) e faz `days.flatMapLatest { observeSafeToSpend(30) }`, como recomenda o handoff 7-A. Na virada do dia a observação é recriada, e "Hoje" e "Menor folga: hoje" se movem. Teste JVM: `dayChangeResubscribesAndMovesToday`.

## ViewModel

`ui/feature/safe/SafeToSpendViewModel.kt`:
- `SafeToSpendDataSource` (recorte do repositório) e `CashEventLabels` (rótulos por ID), com as implementações de repositório.
- `SafeToSpendUiState` = `Loading` | `Unavailable(message)` | `Content`. Situações financeiras normais (zero, negativo) são `Content`, não erro. `Unavailable` cobre `OVERFLOW` e `CONFLICTING_EVENTS` com mensagens próprias, e falha de leitura com `error_load`.
- `SafeSituation` (Reduced / NoObligations / ZeroByObligations / NegativeAvailable) serve só para escolher textos.
- O `DashboardViewModel` não mudou. O card e o detalhe usam instâncias próprias de `SafeToSpendViewModel`.

## Accessibility

- Sobrelinha marcada como título.
- Alertas com ícone, texto e `liveRegion` (nunca só cor).
- Datas anunciadas por extenso ("9 de dezembro de 2026").
- Linhas do breakdown mescladas e clicáveis como botão, com alvos ≥ 64dp.

## Responsive

Matriz offline (modo avião, Wi-Fi e dados desligados): 320×640, 360, 390 escuro, 412, 360 com fonte 1,3 e 320 com fonte 1,3. O resultado está no relatório final da sprint.

## Visual review

Capturas em `screens/`:
- Início;
- detalhe;
- breakdown;
- zero;
- negativo;
- escuro;
- 320dp com fonte 1,3.

Defeitos encontrados e corrigidos antes do commit:
1. **Texto duplicado:** a frase de situação "reduzido" repetia o texto de apoio do destaque e ocupava 3 linhas. No card, ela virou o texto de apoio oficial; no detalhe, foi removida porque o destaque já explica.
2. **"Parcela Notebook 1/10":** repetia "Parcela", que já está na fonte. Agora é "Notebook 1/10" com "Parcela · Cartão".
3. **Horizonte em 320dp/1,3:** a data ao lado espremia "Próximos 30 dias" em uma coluna de uma letra. Agora os dois ficam empilhados.
4. **Nota de vencida em 320dp/1,3:** ficava espremida ao lado do valor. Agora ocupa a largura toda, abaixo da linha.
5. **Rótulos longos:** quebravam em 3 linhas e foram encurtados para "Comprometido" e "Saldo projetado".

`SafeToSpendVisualSeed` (androidTest) só roda com `-e fluxaSeed 1`, e `-e fluxaScenario positive|zero|negative` escolhe o cenário. A suíte normal o pula.

## Tests

- **JVM:** `SafeToSpendViewModelTest` (17). `FakeSafeToSpendSource` usa o `SafeToSpendCalculator` real do domínio ou um estado-sentinela verbatim. Cobre:
  - limite menor que o disponível, igual, zero e com disponível negativo;
  - indisponível (overflow/conflito) e horizonte de 30 dias;
  - **reservas:** real 5.000, reservado 1.500, disponível 3.500 → limite **3.500, nunca 2.000**;
  - **ordem temporal:** 2.000; amanhã −500; +5d −900; +10d +1.000 → **600** com menor folga em +5d, nunca 1.600, e o breakdown sem a receita posterior;
  - resultado-sentinela exibido verbatim e ordem do mesmo dia preservada;
  - rótulos por ID, rótulo ausente, vencida (data original), renomear sem mexer nos valores e rótulos sem valores;
  - atualização automática (nova obrigação e pagamento) e virada do dia.
- **Instrumentados:** `SafeToSpendFlowTest` (7), sobre o Room v7 real:
  - obrigação reduz o limite e pagar atualiza;
  - reservar e liberar meta;
  - zero por obrigações;
  - vencida aplicada hoje;
  - disponível negativo;
  - ocultar valores;
  - persistência entre aberturas.
- **Force-stop manual:** PID novo e início a frio, com o mesmo limite, a mesma menor folga e o mesmo breakdown.

## Pendências

1. Pagamento antecipado de ocorrência futura é rejeitado pelo contrato (`FUTURE_DATE`). Então o limite só "sobe de volta" quando a obrigação vence e é paga. Isso está correto, mas pode surpreender; vale avaliar no produto.
2. `ConfirmedIncome` ainda não tem fonte no domínio. A UI já exibe "+ R$ X · Receita confirmada" se o backend passar a enviar, sem nome (falta contrato de rótulo).
3. Margem de segurança configurável, seletor de horizonte, previsão, receitas inferidas e notificações continuam fora do escopo.

## Base recomendada

`claude/fluxa-android-sprint-7b-frontend`.
