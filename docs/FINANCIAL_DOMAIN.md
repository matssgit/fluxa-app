# Domínio financeiro

As regras que o Fluxa aplica, como estão no código (`android/app/src/main/java/com/fluxa/personal/domain/rules`). Todos os exemplos usam valores fictícios.

## Princípios

1. **Dinheiro é inteiro.** Todo valor é um `Money` em centavos (`Long`). Somas e subtrações usam aritmética exata. Totais maiores, como somas de muitas contas, são acumulados em `BigInteger`. Se não couberem em um `Long`, o resultado é marcado como estouro e não é exibido errado.
2. **Datas financeiras são dias.** Uma transação acontece em um `LocalDate`, no fuso do aparelho, e não em um instante UTC. Datas de criação e edição são instantes, usados só para auditoria e ordenação.
3. **Fatos são gravados e saldos são calculados.** Saldo de conta, saldo disponível, limite usado do cartão, valor reservado e Limite seguro nunca ficam armazenados. São recalculados a partir dos lançamentos, das parcelas e dos movimentos.
4. **Nada é contado duas vezes.** Cada saída de dinheiro aparece uma única vez no saldo e nas análises.
5. **Registros não somem.** Contas, categorias, cartões e metas são arquivados. Lançamentos e compras são cancelados, e o histórico permanece.

## Contas e saldo

`saldo da conta = saldo inicial + entradas − saídas`, considerando apenas lançamentos não cancelados com data **igual ou posterior** à data de abertura da conta.

`saldo disponível = Σ (saldo − reservado)` das contas **ativas** e **incluídas no disponível**.

**Exemplo**

| Conta | Saldo | Reservado | No disponível? |
| --- | ---: | ---: | --- |
| Conta Principal | R$ 4.000,00 | R$ 1.000,00 | sim |
| Carteira | R$ 150,00 | — | sim |
| Poupança | R$ 8.000,00 | — | não |

Saldo total: R$ 12.150,00. Reservado: R$ 1.000,00. **Disponível: R$ 3.150,00** (3.000 + 150).

## Receitas e despesas

- O valor é sempre positivo. O tipo (entrada ou saída) define o sinal.
- A data não pode ser futura (`FUTURE_DATE`).
- A conta e a categoria precisam existir e estar ativas, e a categoria precisa ser do mesmo tipo do lançamento.
- Lançamentos criados por pagamento de parcela ou de assinatura só mudam pela operação de origem (`MANAGED_BY_INSTALLMENT`, `MANAGED_BY_SUBSCRIPTION`).
- Cancelar marca `cancelledAt`. O lançamento sai do saldo e das análises, mas continua no banco.

## Cartões, compras e parcelas

### Limite

`limite disponível = limite total − Σ parcelas pendentes do cartão`

Uma compra só é aceita se `valor total ≤ limite disponível`. O limite total não pode ser reduzido abaixo do comprometido.

### Primeira parcela

Se a compra foi feita **antes** do dia de fechamento do mês, entra nesse fechamento. Se foi feita no dia do fechamento ou depois, entra no fechamento do mês seguinte. A primeira parcela vence no primeiro vencimento **posterior** a esse fechamento.

**Exemplo**: cartão que fecha no dia 3 e vence no dia 10.
- Compra em 2 de março → fechamento de 3 de março → vencimento em **10 de março**.
- Compra em 3 de março → fechamento de 3 de abril → vencimento em **10 de abril**.

Dias de cobrança acima do tamanho do mês, como 31, caem no último dia do mês.

### Divisão em parcelas

`valor base = total ÷ n` (divisão inteira, em centavos). O resto vai **um centavo por parcela, nas últimas**.

**Exemplo**: R$ 100,00 em 3 vezes → R$ 33,33 · R$ 33,33 · R$ 33,34. A soma é exatamente R$ 100,00.

### Pagamento e desfazer

- Pagar uma parcela cria **uma** despesa na conta escolhida, com a categoria da compra, e marca a parcela como paga. As duas coisas acontecem na mesma transação de banco.
- Um índice único (`financial_transactions.installmentId`) garante no máximo uma despesa por parcela.
- A data do pagamento não pode ser futura nem anterior à abertura da conta.
- Pagar de novo uma parcela já paga devolve o pagamento existente, sem criar outro.
- Desfazer apaga a despesa vinculada e devolve a parcela para pendente.

### Cancelar compra

Permitido enquanto **nenhuma** parcela tiver sido paga (`HAS_PAID_INSTALLMENT`). As parcelas pendentes são canceladas e o limite é liberado.

### Quando a compra vira despesa

A compra **não** reduz o saldo de nenhuma conta no dia da compra. Ela compromete o limite do cartão. O dinheiro sai da conta, e a despesa entra nas análises, **quando a parcela é paga**. Assim o mesmo gasto não é contado duas vezes.

## Assinaturas

- Frequência mensal ou anual, a partir de uma data-âncora, com dia de cobrança.
- **Termos** (`subscription_terms`) guardam valor, categoria e origem com a data a partir da qual valem. Uma mudança afeta só as cobranças a partir dessa data.
- Cada cobrança é identificada por `(assinatura, data)`. A **resolução** (`subscription_resolutions`) registra se ela foi paga, cobrada no cartão ou pulada.
- **Pagar em conta** cria uma despesa vinculada à cobrança. Um índice único em `(subscriptionId, occurrenceDate)` impede duplicidade.
- **Cobrar no cartão** cria uma compra à vista no cartão, sujeita ao limite (`INSUFFICIENT_CARD_LIMIT`). A despesa só aparece quando essa parcela for paga.
- **Desfazer** remove o resultado e reabre a cobrança. Se a parcela da cobrança no cartão já foi paga, é preciso desfazer esse pagamento antes (`CARD_CHARGE_ALREADY_PAID`).
- Assinaturas pausadas ou canceladas não geram cobranças novas.
- Uma cobrança passada e não resolvida aparece como **vencida**.

**Custo nas análises**: `mensal equivalente = Σ mensais + arredonda(Σ anuais ÷ 12)` e `anual = Σ mensais × 12 + Σ anuais`, somente das assinaturas ativas.

## Metas e reservas

- A meta pertence a uma conta. **Reservar** registra um movimento `ALLOCATE` e **liberar** registra um movimento `RELEASE`.
- `reservado = Σ reservas − Σ liberações`, que nunca fica negativo.
- Não é possível reservar mais do que falta para o alvo (`ALLOCATION_EXCEEDS_TARGET`) nem mais do que o disponível (`INSUFFICIENT_AVAILABLE_FUNDS`).
- Não é possível liberar mais do que está reservado.
- A meta pode ser **concluída** quando o reservado é igual ao alvo, e **arquivada** só com reservado zero.
- Depois do primeiro movimento, a conta da meta não muda. Isso é garantido também por um *trigger* no banco.

**Exemplo**: Conta Principal com saldo de R$ 4.000,00 e meta "Viagem" de R$ 5.000,00 com R$ 1.000,00 reservados. O saldo da conta continua R$ 4.000,00, o disponível dela é R$ 3.000,00 e faltam R$ 4.000,00 para a meta.

## Limite seguro

Responde: **quanto posso gastar hoje sem que o disponível fique negativo em nenhum dia dos próximos 30 dias?**

**Entradas**
- Disponível agora (com as reservas descontadas).
- Compromissos com vencimento em `[hoje, hoje + 30)`:
  - parcelas pendentes de cartão;
  - cobranças de assinatura previstas e não resolvidas. As cobranças de contas fora do disponível são ignoradas, porque não saem dele.
- Compromissos já vencidos e não pagos entram **como se vencessem hoje**.

**Cálculo**
1. Remove duplicidades pela identidade de cada compromisso. Se o mesmo compromisso aparecer com valores diferentes, o resultado fica indisponível (`CONFLICTING_EVENTS`) em vez de arriscar um número errado.
2. Ordena por data.
3. Percorre os compromissos subtraindo cada um do saldo projetado e guarda o **menor saldo** e o dia em que ele ocorre (o **dia de menor folga**).
4. `Limite seguro = max(0, menor saldo projetado)`.

**Exemplo**: disponível de R$ 2.000,00 hoje.

| Dia | Compromisso | Saldo projetado |
| --- | --- | ---: |
| hoje | — | R$ 2.000,00 |
| dia 5 | Parcela do notebook | R$ 1.640,00 |
| dia 10 | Academia | R$ 1.520,00 |
| dia 22 | Parcela da geladeira | R$ 1.120,00 |

Limite seguro: **R$ 1.120,00**. Menor folga: dia 22.

Ele **não** prevê salário, gastos do dia a dia nem nada que não esteja registrado. É um limite em relação ao que você já combinou, e não uma previsão do futuro.

## Análises

- **Resumo do mês**: soma das entradas e saídas realizadas (lançamentos não cancelados) com data no mês. `resultado = entradas − saídas` e `taxa de economia = resultado ÷ entradas`, quando há entradas.
- **Por categoria**: saídas do mês agrupadas. Os percentuais são inteiros que somam **exatamente 100**: os pontos que sobram vão para as categorias com maior resto, com desempate estável.
- **Últimos 6 meses**: o mesmo resumo para o mês escolhido e os cinco anteriores.
- **Projeção**: reaproveita o Limite seguro, sem um segundo algoritmo. `saldo final = disponível + receitas confirmadas − compromissos`, e o menor saldo é o do Limite seguro.
- **Observações** (no máximo quatro): mês negativo, despesas maiores que as do mês anterior (só entre meses completos), maior categoria, resultado positivo e mês sem despesas.

Como compras no cartão entram pelo pagamento da parcela, um mês com uma compra grande no cartão e nenhuma parcela paga não mostra essa despesa.
