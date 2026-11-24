# Sprint 11-A — Analytics e projeção financeira

## Base

Base exata: 8b5edccabdc6613c8bdacbf5ddb5cf6941bc6d77, origem codex/fluxa-android-rc1-physical-device.
Branch: codex/fluxa-android-sprint-11a-analytics.
Worktree: F:/fluxa-app-android-sprint-11a.
O commit de entrega é o que contém este documento. Checkout principal e suas alterações preservados. Sem push, merge ou deploy.

## Analytics architecture

FinancialAnalyticsRepository → FinancialAnalyticsDao + leitura transacional compartilhada do SafeToSpend → FinancialAnalyticsCalculator (Kotlin puro).
FinancialAnalyticsSnapshot contém asOfDate, monthSummary, categorySpending, sixMonthEvolution, projection, remainingMonthProjection, subscriptionAnalytics e insights; topCategories é um helper take(5).
Nenhuma tabela, coluna, migration, dado derivado persistido ou mudança de formato .fluxa. Room permanece v7.
Sem UI, gráficos, mudanças de navegação, SDK/dependências, crypto, security ou comandos financeiros.
A única alteração no SafeToSpendRepository é tornar sua leitura read(today,horizonDays) internal para reutilização; o corpo e o algoritmo permanecem iguais.

## Monthly summary

MonthlyFinancialSummary: month, incomeRealized, expenseRealized, netResult, transactionCount e savingsRate.
Somente financial_transactions não canceladas no intervalo local [primeiro dia do mês, primeiro dia do mês seguinte).
O ledger existente só persiste movimentos realizados; seu comando rejeita datas futuras. Saldo inicial não é receita.
Não somar Purchase, Installment, SubscriptionOccurrence, reserva ou liberação ao ledger novamente. Pagamentos de parcelas/assinaturas já geram a Transaction gerenciada; undo a remove conforme os contratos atuais.
Histórico inclui contas arquivadas/excluídas do disponível: excluir saldo de projeção não apaga uma movimentação realizada.
netResult mantém sinal. savingsRate é razão decimal net/income com DECIMAL128 (34 algarismos significativos, HALF_EVEN); 0.25 representa25%, -0.5 representa-50%. Income zero → null, nunca0 arbitrário.

## Categories

CategorySpending: categoryId estável, categoryName atual, amount Money e percentage inteiro0..100.
SQL agrega apenas despesas realizadas. JOIN por ID mantém categorias arquivadas no histórico. Não há “Outros” inventado; sem despesas retorna lista vazia.
Ordenação: maior valor, empate pelo UUID textual. Distribuição de100 pontos pelo maior resto, empate pelo UUID, total exato100. Categorias pequenas podem ter0%.
topCategories retorna as cinco primeiras sem renormalizar percentuais: o top5 não precisa somar100.
A categoria da despesa de parcela/assinatura é a da Transaction realmente gerada pelo domínio, sem reclassificar nem contar compra/ocorrência outra vez.
Nome é o cadastro atual, não um snapshot histórico de nome inexistente na persistência.

## Evolution

CashFlowMonth: month, income, expense, net.
Seis meses contínuos e em ordem cronológica, do selecionado menos5 até o selecionado, inclusive. Meses vazios0/0/0.
Limites SQL são epochDay de LocalDate calculados a partir de YearMonth; sem strftime/UTC. Datas de criação/edição Instant não definem competência.
APIs usam YearMonth; valores extremos sem seis meses completos representáveis ou sem limite final em LocalDate são rejeitados na validação dos limites.

## Projection

FinancialProjection: availableNow, obligationsTotal, confirmedIncomeTotal, projectedEndingBalance, lowestProjectedBalance, lowestBalanceDate, horizonStart e horizonEndExclusive.
O mês selecionado controla resumo/categorias/evolução. A projeção, o orçamento de assinaturas e asOfDate são sempre relativos à data financeira atual injetada, inclusive quando o mês selecionado é histórico. Não simular um saldo histórico.
Default: [today,today+30dias), isto é30 datas financeiras; parâmetro horizonDays positivo. Semântica idêntica à do SafeToSpend.
remainingMonthProjection usa [today,primeiro dia do próximo mês), incluindo fevereiro bissexto, pelo mesmo pipeline. Reutiliza o resultado quando os horizontes coincidem.

Saldo final = disponível atual + receitas confirmadas do horizonte − obrigações do horizonte, com aritmética intermediária BigInteger e conversão exata para Long.
O domínio persistido atual não possui um comando de receita futura confirmada: confirmedIncomeTotal é0 na integração atual. Não transformar receita histórica ou promessa em receita projetada. O contrato compartilhado já aceita eventos CONFIRMED_INCOME; os testes puros cobrem renda posterior/no mesmo dia, sem inventar nova persistência.
Valores finais e mínimos podem ser negativos. A menor data é a primeira em que o mínimo estrito ocorre; sem queda, é today.

## SafeToSpend relationship

Não há segundo calendário ou segundo algoritmo de ordenação/deduplicação. Analytics chama a mesma leitura do SafeToSpend para cada horizonte, com margem0, e adapta os resultados.
Obrigações vencidas são aplicadas hoje; obrigações precedem receitas no mesmo dia. Renda posterior não corrige um mínimo anterior.
Parcelas pendentes, cancelamentos, resoluções/reaberturas de assinaturas e elegibilidade de contas seguem exatamente o leitor existente.
availableNow já inclui o desconto de reservas elegíveis: não subtrair novamente. Caso obrigatório: real5000, reserva1500 → disponível/projeção inicial3500, seguro3500, nunca2000.
Não usar safeAmount como saldo final: ele é “quanto posso gastar”, com clamp próprio; projeção preserva o saldo mínimo assinado.
SafeToSpendCalculator não foi alterado.

## Subscriptions

SubscriptionAnalytics: activeCount, monthlyEquivalentCost e annualProjectedCost.
Somente status ACTIVE. Considera todas as configurações ativas, inclusive fontes fora do disponível; é orçamento contratado, enquanto projeção usa elegibilidade de caixa. Paused/cancelled não entram no custo futuro ativo, mas não apagam histórico realizado nem dívidas já existentes.
Valores são os detalhes configurados atuais de Subscription, como no cadastro/listagem; não reconstituem termos históricos do mês selecionado. Calendário de caixa continua usando os termos/resoluções efetivos no SafeToSpend.
Mensal: anual = valor×12. Anual: custo anual preservado. Somar os anuais primeiro, dividir por12 e arredondar uma única vez HALF_UP para centavos; adicionar os mensais.
monthlyEquivalentCost é aproximado. Não multiplicá-lo por12 para obter annualProjectedCost nem ratear uma saída anual no ledger/projeção.

## Insights

Fatos tipados, sem texto localizado, IA, score ou linguagem motivacional:
- NEGATIVE_MONTH: resultado assinado negativo.
- EXPENSES_INCREASED: apenas meses encerrados, anterior consecutivo com dados e despesa>0; atual com dados e despesa maior. Razão de crescimento DECIMAL128, sem quantizar um aumento pequeno para zero.
- TOP_CATEGORY: ID e valor da categoria líder.
- POSITIVE_SAVINGS: resultado positivo com receita positiva.
- NO_EXPENSES: mês com movimentações mas nenhuma despesa.

Ordem determinística conforme lista acima, máximo4 fatos. Sem movimentações: lista vazia. Nenhum crescimento com base zero ou comparação entre mês atual parcial e mês anterior completo. A restrição a meses encerrados é deliberada para evitar comparação enganosa; por isso o mês corrente não emite EXPENSES_INCREASED.
Frontend escreve e localiza o texto a partir dos fatos. O backend preserva o sinal de taxas muito pequenas: uma primeira versão com escala fixa6 poderia transformar uma perda de1centavo sobre receita muito alta em0; foi corrigida para DECIMAL128 e coberta em JVM. Arredondamento visual é responsabilidade da apresentação.

## Metrics intentionally excluded

financialHealthScore, liquidityMonths, HIGH_SUBSCRIPTION_COST (nenhum limiar aprovado), knownCommitmentRate (ambiguidade entre disponível, obrigações e SafeToSpend), previsão estatística, orçamento persistido, novos comandos de receita futura e comparações de meses parciais.
Não portar heurísticas antigas do Web.

## Queries

FinancialAnalyticsDao:
1. readMonths: CTE de seis intervalos epochDay, GROUP BY bucket/direction e COUNT; retorna até12 grupos.
2. readCategories: GROUP BY categoryId/name no mês, sem filtro de arquivamento.
3. readActiveSubscriptions: GROUP BY frequency, até2 grupos.

SUM monetário usa duas parcelas inteiras (radix2^32), recombinadas em BigInteger. Não usar TOTAL(), REAL, Float ou Double. Faixa excedida retorna FinancialAnalyticsState.Unavailable(OVERFLOW), sem wrap/clamp. Overflow do SUM SQLite também é explícito; outros erros de banco propagam, não são mascarados como overflow.
Conversão BigInteger → Long usa bitLength/toLong após checar faixa, compatível com API30. O primeiro lint identificou longValueExact como API31; corrigido sem mudar minSdk nem adicionar suppressions.

## Flow/reactivity

observeFinancialAnalytics(month: YearMonth? = null, horizonDays: Int = 30): Flow<FinancialAnalyticsState>.
null escolhe o mês financeiro atual por emissão.
Observa accounts, financial_transactions, categories, goals, goal_movements, credit_cards, credit_purchases, credit_installments, subscriptions, subscription_terms e subscription_resolutions.
Cada emissão lê todos os componentes dentro de uma única withTransaction; não combina snapshots independentes de pagamento e obrigação. distinctUntilChanged elimina saídas idênticas.
Ler/injetar FinancialCalendar na construção permite testes determinísticos. A data é recapturada a cada invalidação/coleta; como o contrato SafeToSpend existente, não há ticker de meia-noite. A futura UI deve recolher/atualizar ao retornar à tela ou ao mudar a data local.

## Performance

Resumo/série/categorias não materializam o histórico em Kotlin: agregação SQL usa o índice existente occurredOn e agrupamentos limitados. Assinaturas de orçamento retornam no máximo dois grupos; top5 não consulta de novo.
Projeção reutiliza a leitura existente de reservas/saldos/calendário, que pode percorrer movimentos históricos de saldo e ocorrências vencidas. Esse custo herdado não foi reescrito em SQL com regras divergentes. Há até duas leituras SafeToSpend, uma por horizonte distinto. Bases grandes devem medir esse caminho compartilhado antes de otimização conjunta; não há cache ou métrica derivada persistida.
Flow deve ser coletado conforme lifecycle da futura tela, não iniciar coletores permanentes no Application. O acesso app.analytics é lazy.

## Tests

Validação aprovada na base isolada:
- Baseline antes da implementação: 293 testes JVM, zero falhas.
- Final: **311 testes JVM, zero falhas/erros/skips** (18 novos).
- Integração Analytics focada: **7/7 aprovados**.
- Suíte instrumentada completa final: **175 casos únicos, 162 aprovados, 13 skips sob demanda, zero falhas/erros**; os7 novos Analytics foram executados sem skip.
- A suíte completa anterior à melhoria DECIMAL128 também passou; builds/JVM e toda a suíte foram repetidos após essa melhoria. O contador de console188 inclui repetição de eventos de skip; o XML confirma175 casos.
- Incluídos na suíte: reserva3500, SafeToSpend, reatividade, pagamentos/undo, Room/migrations/reabertura, backup round-trip e fluxos existentes.
- AVD exclusivo fluxa_sprint11a, emulator-5578, API34: modo avião, Wi-Fi e dados desligados; Active default network:none ao final; crash buffer vazio. Encerrado após a validação. Não houve teste físico nesta sprint.
- assembleDebug, assembleRelease e assembleDebugAndroidTest: aprovados offline.
- lintDebug e lintRelease: **No issues found**, zero erros/warnings nos relatórios finais.
- Room v7 e schemas históricos sem diff; backup/crypto/security/commands/UI e SafeToSpendCalculator sem diff.
Os13 pulados são capturas/seeds visuais e protocolos externos/timeout/sem PIN herdados da base; não contam como aprovações e não são novos testes Analytics.

Logs locais ignorados em .gradle/11a: build-final.log, instrumented-final.log, analytics-integration.log, analytics-results.xml, crash-final.txt e network-final.txt. Resultados XML JVM em app/build/test-results/testDebugUnitTest; instrumentados em app/build/outputs/androidTest-results/connected/debug.

Reprodução em android, com JAVA_HOME/ANDROID_HOME configurados:
```powershell
.\gradlew.bat --offline --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:lint :app:lintRelease
$env:ANDROID_SERIAL='SERIAL_DO_AVD_EXCLUSIVO'
.\gradlew.bat --offline --no-daemon :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.deviceCredential=PIN_DESCARTAVEL_DO_AVD"
```

Testes puros: valores negativos, receita zero, taxa assinada/arredondamento, percentuais exatos/empates/muitas categorias/Long extremo, seis meses/vira de ano, projeção compartilhada (vencidos, renda posterior, mesmo dia, duplicidade, horizonte exclusivo e fevereiro29), orçamento mensal/anual e overflows, insights sem base zero e sem dados.
Integração Room: saldo inicial fora de receitas, ledger realizado/cancelado, categorias arquivadas/rename, datas locais com UTC no mês seguinte, pagamentos de parcelas, assinaturas por conta/cartão, undo, pausas/cancelamentos, Flow contínuo, reserva3500, conta excluída, horizontes, reabertura v7 e soma além de Long.
Um erro de assinatura de teste (retorno inferido de snapshot em runBlocking) foi corrigido explicitando Unit antes de executar a integração. Nenhum timeout foi aumentado.

## Frontend contract

Ponto de entrada: app.analytics.observeFinancialAnalytics().
Tratar Ready(snapshot) e Unavailable(error); não mostrar números parciais inventados no estado de overflow.
Money sempre em centavos BRL. Backend não mascara valores. Usar a preferência de privacidade e semântica acessível existentes também em gráficos, listas e fatos.
savingsRate/changeRate são razões decimais; percentage das categorias já está em pontos inteiros0..100.
Rotular projeções como caixa conhecido, não promessa/previsão estatística; confirmedIncome é0 até existir contrato de receita futura.
Distinguir período do realizado (monthSummary.month) e referência atual das projeções/orçamento (asOfDate/horizonStart).
Exibir monthlyEquivalentCost como equivalente aproximado de orçamento; annualProjectedCost é a referência anual exata.
Sem mudança na entrada Análises da navegação nesta sprint; implementação visual é responsabilidade da próxima entrega frontend.
