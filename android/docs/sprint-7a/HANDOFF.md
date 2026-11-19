# Sprint 7-A — SafeToSpend

A base é `3696a6ec885838d5228ad446c06065060ce2f2c3`. O ponto de composição é `FluxaApplication.safeToSpend`.

## Fórmula e precisão

`SafeToSpendCalculator.calculate(availableNow, events, today, horizonDays = 30, safetyMargin = Money(0))` é uma função pura Kotlin. O mínimo começa em `availableNow - safetyMargin` e acompanha obrigações e receitas confirmadas em ordem temporal. `safeAmount` é o maior valor entre zero e esse mínimo. Os eventos usam `Money` positivo. Acumulações intermediárias usam `BigInteger`, com resultado exato em centavos ou `SafeToSpendState.Unavailable(OVERFLOW)`.

O disponível já inclui a dedução das reservas elegíveis. O SafeToSpend o recebe do contrato de `GoalRepository` e aplica a margem uma única vez. A margem do repositório permanece zero.

Disponível negativo e ausência de obrigações são resultados válidos. O resultado conserva `availableNow` e `lowestProjectedBalance` negativos para permitir a explicação da situação financeira.

## Horizonte e ordem

A janela padrão tem 30 dias e segue `[today, horizonEndExclusive)`. A chamada admite outro número positivo de dias. Obrigações vencidas são aplicadas em hoje, independentemente de quão antiga seja a data original. Na mesma data, todas as obrigações antecedem receitas confirmadas. Empates usam identidade estável. Havendo duas folgas mínimas iguais, prevalece a primeira.

Receitas anteriores a hoje já pertencem ao caixa realizado e são excluídas da projeção. A função aceita eventos de receita futura explicitamente confirmada para permitir evolução posterior.

## Fontes de obrigação

O repositório atual projeta as seguintes fontes.

- Parcelas `PENDING` de compras ativas, com vencimento anterior ao fim exclusivo, inclusive vencidas. A compra total não é somada.
- Ocorrências abertas de assinatura, com seus termos históricos. Assinaturas anuais preservam o valor integral. Pausa, cancelamento, skip e reabertura seguem o calendário já existente.
- Assinaturas vinculadas a conta usam apenas contas ativas elegíveis, conforme `AccountReserveSummary.includedInAvailable`. A despesa prevista permanece ligada à sua conta histórica. Excluir essa conta da disponibilidade também exclui seus eventos da projeção consolidada.
- Assinaturas ainda abertas no cartão pesam na data da ocorrência. Essa é a política conservadora da v1, pois a data real da compra ainda é desconhecida.
- As parcelas de cartão são consideradas mesmo quando o cartão está arquivado. A dívida continua existente. O cartão ainda não possui conta de liquidação, então suas obrigações comprometem o disponível consolidado.

O ledger atual só persiste lançamentos efetivados ou cancelados e rejeita datas futuras. O enum compartilhado contém `PENDING`, mas esse estado não é um contrato persistido de lançamento. A Sprint 7-A utiliza o saldo realizado. Receitas futuras e lançamentos pendentes poderão ser incorporados quando seus contratos existirem. Nenhum histórico de salário é usado para prever receita.

## Unicidade e estornos

Quando uma ocorrência gera compra, ela fica `CHARGED` e sua obrigação passa a ser a parcela. Quando gera uma baixa em conta, fica `PAID` e seu valor já compõe o saldo realizado. Parcelas pagas e canceladas saem das obrigações. O estorno reabre a representação correspondente. Uma ocorrência pulada deixa de pesar.

O calculador remove cópias idênticas de uma identidade de evento. Versões diferentes da mesma identidade retornam `CONFLICTING_EVENTS`. A identidade de parcela é seu UUID; a identidade de assinatura contém o UUID e a data da ocorrência.

## Leitura e explicação

`observeSafeToSpend(horizonDays = 30)` retorna `Flow<SafeToSpendState>`.

`Ready.result` expõe:

- `safeAmount`, valor seguro para gastar agora;
- `availableNow` e `safetyMargin`, componentes da base;
- `lowestProjectedBalance` e `lowestSlackDate`, menor folga e primeira data;
- `horizonStart` e `horizonEndExclusive`;
- `obligationsTotal` e `confirmedIncomeTotal`, totais da janela completa, com vencidos;
- `breakdown`, eventos que contribuíram até o instante do mínimo.

O breakdown para antes de qualquer receita posterior ao mínimo, inclusive uma receita do mesmo dia. Os totais da janela podem incluir eventos posteriores; a UI deve usar o breakdown como justificativa do mínimo. Cada item contém data efetiva, tipo, valor e fonte estrutural para navegar a assinatura, compra ou parcela. A ocorrência preserva a data original dentro de sua chave, mesmo quando a obrigação vencida é aplicada hoje.

`Unavailable(OVERFLOW)` indica um resultado sem representação financeira exata no contrato. `Unavailable(CONFLICTING_EVENTS)` indica identidades conflitantes. Falhas de persistência são propagadas pelo Flow para o tratamento normal do aplicativo.

## Snapshot e custo de leitura

As fontes e a base são lidas na mesma transação Room. As invalidações observadas incluem contas, ledger, metas, movimentos de reserva, assinaturas, termos, desfechos, cartões, compras e parcelas. Assim, cada emissão reflete um estado consistente depois de pagamento, estorno, skip, reserva, liberação ou alteração do saldo.

A leitura compartilhada de disponibilidade agora agrega o ledger e as reservas em SQLite por conta, com partes inteiras de 32 bits recompostas em `BigInteger`. Retorna somente o estado atual por conta e reaproveita `ReserveBalanceCalculator`. O histórico de movimentos continua acessível pelos contratos próprios.

A projeção lê somente parcelas pendentes dentro do limite e os termos necessários ao calendário de assinaturas. Datas históricas são geradas em janelas de até 24 meses para localizar atrasos sem perdê-los. Uma anti-consulta indexada pela chave composta retorna apenas datas abertas; linhas históricas pagas, cobradas e puladas permanecem no banco. O trabalho de derivação de calendário cresce com a idade da assinatura, sem carregar o histórico financeiro resolvido em memória.

## Clock e meia-noite

O repositório recebe `Clock` e `FinancialCalendar`. A data financeira usa o fuso fornecido pelo calendário. A observação consulta o dia em cada emissão. Ao receber o sinal de mudança de dia já usado no frontend, recrie o Flow com `flatMapLatest`. O backend não instala um temporizador.

```kotlin
val safeState = app.safeToSpend.observeSafeToSpend()
val fortnight = app.safeToSpend.observeSafeToSpend(horizonDays = 14)
// No sinal de novo dia, cancele a observação anterior e assine novamente.
```

## Validação

A suíte JVM cobre ordenação temporal, reservas já deduzidas, receitas anteriores e posteriores, mesmo dia, atrasos, janela, empates, margem, identidades, resultado negativo e overflow.

Os testes Room cobrem reserva/liberação, conta excluída, despesa/cancelamento no ledger, assinatura em conta e cartão, parcela paga e estornada, cobrança estornada, skip, cancelamento da compra, pausa/cancelamento de assinatura, dívida anual antiga e reatividade. Um Clock fixo cruza a data UTC para validar a data financeira local.

O schema permanece Room v7. A projeção não cria tabelas nem eventos persistidos.
