# Sprint 5-B — Assinaturas, ocorrências, pagamento e cobrança no cartão (frontend)

Base: `codex/fluxa-android-sprint-5a` @ `2f52b20`. Nenhuma alteração em Room v6, migrations, domínio, repositórios, calendário ou regras de concorrência. A UI não gera calendário, não calcula a próxima cobrança e não monta compras ou transações.

## Subscription UI

- Acesso por **Mais → Assinaturas** (`subscriptions`). Seções Ativas, Pausadas e Canceladas.
- Cada cartão de assinatura mostra:
  - nome (até 2 linhas), valor (`FluxaAmount`) e “Cobrança mensal/anual”;
  - **Próxima cobrança** (`observeNextOccurrence`) ou o motivo de não haver próxima (pausada/cancelada);
  - fonte (“Conta: …” / “Cartão: …”) e estado (pílula com ícone e texto).
- Um aviso de vencidas (`observeOverdueOccurrences`) leva à agenda.
- **Cadastro** (`subscriptions/new`):
  - nome, valor (`MoneyInput`), frequência **Mensal/Anual** e primeira cobrança (date picker);
  - dia de cobrança só no mensal (1–31, aviso de meses curtos). No anual o dia é o da âncora, que o contrato exige;
  - categoria de despesa ativa;
  - **Pagar com**: Conta OU Cartão. Só a lista do tipo escolhido aparece; o detalhe envia exatamente um `SubscriptionSource`.
- Anual traz o aviso “o valor inteiro é cobrado uma vez por ano”. **Nenhum equivalente mensal** é exibido, porque o backend não fornece.
- **Edição** (`subscriptions/{id}/edit`): nome, valor, categoria e fonte. Frequência, âncora e dia ficam bloqueados (contrato 5-A). Um aviso explica que valor e fonte valem a partir de amanhã e que hoje, vencidas e histórico mantêm os termos. Assinatura cancelada abre só leitura.
- **Detalhe** (`subscriptions/{id}`) mostra:
  - frequência, valor, estado, próxima cobrança, fonte, categoria e primeira cobrança;
  - **Em aberto** (vencidas desta assinatura) e **Histórico** (pagas, cobradas, puladas, com o snapshot gravado);
  - ações Ver agenda, Editar, Pausar/Retomar e Cancelar.

## Agenda

`subscriptions/agenda`, agrupada por data:
- **Vencidas** (`observeOverdueOccurrences`);
- **Próximos 30 dias** (`observeUpcomingOccurrences(30)`);
- **Resolvidas recentemente**: `observeOccurrences(hoje − 30, hoje + 31)`, só desfechos, mais recentes primeiro.

Cada linha mostra assinatura, valor, data, fonte e estado: Pendente, Vencida, Paga, Cobrada no cartão ou Pulada, sempre com texto e ícone.

## Occurrences

`subscriptions/{id}/occurrences/{data}` (`observeOccurrence`). Valor e fonte exibidos são os **da ocorrência** (termos vigentes na data).
- Em aberto e não futura, com assinatura ativa: “Pagar” (fonte conta) ou “Lançar no cartão” (fonte cartão).
- Futura: só “Pular cobrança”, com aviso da data a partir da qual pode ser paga.
- Pausada/cancelada: só pular, com aviso.
- Resolvida: só leitura, com atalho para o efeito real: “Ver no Caixa” (transação) ou “Ver compra no cartão” (compra, pelo `resultId` e o cartão da ocorrência).

## Account payment

Sheet com resumo (assinatura, data da cobrança, valor, conta) e **data do pagamento** (hoje por padrão, sem futuro). A conta é a da ocorrência e não é trocada na UI. Envia só `paySubscriptionOccurrence(id, data, paidOn)`. A saída aparece no Caixa, Início e saldos pelo ledger, uma única vez.

## Card charge

Mesmo sheet, com “Lançar no cartão” e a data da compra. O backend cria a compra com uma parcela. A UI não cria nada. Comprometido, disponível e compra no cartão atualizam pelos Flows. Nada entra no Caixa até a parcela ser paga (Sprint 4-D). `INSUFFICIENT_CARD_LIMIT` → “Limite disponível insuficiente para lançar esta assinatura no cartão.”, e a ocorrência continua em aberto (estado do backend, sem mudança local).

## Skip

“Pular cobrança” com confirmação: “Esta ocorrência será ignorada e não aparecerá mais como pendente. O histórico será preservado.” Usa `skipSubscriptionOccurrence` e não cria movimento.

## Pause/resume

- **Pausar**: “Novas cobranças deixarão de ser geradas enquanto estiver pausada. O histórico e as cobranças já vencidas continuam disponíveis.”
- **Retomar**: “As cobranças voltam a ser geradas a partir de amanhã. As datas do período pausado não são recriadas.”
- Não há promessa de repor cobranças.

## Cancel

“Cancelar assinatura” (destrutivo) com confirmação: “A assinatura deixará de gerar novas cobranças. O histórico será preservado. Esta ação não pode ser desfeita.” Cancelada não oferece retomar nem editar.

## Navigation

- **Caixa**:
  - a linha da saída de assinatura ganha a marca “Assinatura” (ícone de repetição);
  - abrir mostra modo leitura “Pagamento de assinatura” com **Origem: Assinatura**, sem Salvar/Cancelar, e **Ver assinatura** (ID gravado no vínculo);
  - `MANAGED_BY_SUBSCRIPTION` mostra essa orientação.
- **Baixa de parcela no Caixa**: **Ver parcela** usa `observeInstallmentOrigin(installmentId)` (join por PK) e navega direto para a parcela. Sem varrer listas.
- **Compra gerada por assinatura**: o detalhe da compra mostra “Compra gerada por assinatura.” e **Ver assinatura**.

## ViewModels

| ViewModel | Responsabilidade |
|---|---|
| `SubscriptionsViewModel` | Lista por estado, próxima de cada uma, contagem de vencidas |
| `SubscriptionFormViewModel` | Cadastro/edição, validação de preenchimento, fonte exclusiva |
| `SubscriptionDetailViewModel` | Detalhe, em aberto, histórico, pausar/retomar/cancelar (`CommandStatus`) |
| `SubscriptionAgendaViewModel` | Vencidas, próximas 30 dias, resolvidas, agrupadas por data |
| `SubscriptionOccurrenceViewModel` | Pagar/lançar/pular, data da baixa, desfecho idempotente |

Todos sobre `SubscriptionDataSource` (recorte do `SubscriptionRepository` para testes JVM; `RepositorySubscriptionDataSource` só delega). Fluxo: Room → Repository Flow → ViewModel (`StateFlow`) → Compose. Pagar, cobrar, pular, pausar, retomar e cancelar atualizam a UI sem refresh manual.

## Meia-noite

Resolvido no frontend, sem polling pesado e sem mudar o backend. `financialDays()` (`ui/common/FinancialDay.kt`) emite o dia financeiro e dorme até a meia-noite seguinte, com teto de 1h para absorver mudanças manuais de relógio ou fuso. Só emite quando a data muda. Os ViewModels de lista, detalhe, agenda e ocorrência recriam as observações com `flatMapLatest`, e o repositório recalcula “hoje” no `map`, como recomenda o handoff 5-A. Coberto por teste JVM (pendente vira vencida ao trocar o dia).

## Errors

| Erro | Mensagem |
|---|---|
| `PAUSED` | Esta assinatura está pausada. |
| `CANCELLED` | Esta assinatura foi cancelada. |
| `INSUFFICIENT_CARD_LIMIT` | Limite disponível insuficiente para lançar esta assinatura no cartão. |
| `ALREADY_RESOLVED` / `SKIPPED` | Tratado como estado atualizado: “Esta cobrança já estava resolvida. Os dados foram atualizados.” |
| `FUTURE_DATE` / `BEFORE_ACCOUNT_OPENING` | Erro na data (sheet). |
| `INVALID_SOURCE` | A conta ou o cartão escolhido não está mais ativo. |
| `INVALID_CATEGORY` | Escolha uma categoria de despesa ativa. |
| `INVALID_ANCHOR` | O dia de cobrança anual precisa ser o mesmo dia da primeira cobrança. |
| `INVALID_DETAILS`, `INVALID_OCCURRENCE`, `INVALID_CARD_SCHEDULE`, `NOT_FOUND` | Mensagens próprias, sem códigos. |

## Accessibility

- Estados de assinatura e ocorrência anunciados (“Situação: …”) com ícone e texto.
- Frequência por extenso; fonte com prefixo “Conta:”/“Cartão:”.
- Datas exibidas como “17 dez. 2026” e anunciadas por extenso (“17 de dezembro de 2026”).
- Escolhas como `RadioButton` em grupo; ações destrutivas com ícone e confirmação; erros com `liveRegion` assertivo; alvos ≥ 48dp.

## Responsiveness

Matriz offline (modo avião, Wi-Fi e dados desligados): 320×640, 360, 390 escuro, 412, 360 com fonte 1,3 e, adicionalmente, 320 com fonte 1,3. Nomes longos com até 2 linhas, ações empilhadas em largura total e sheet de pagamento com botões empilhados. Ver `screens/`.

Ajustes feitos na verificação visual no emulador:
- “Nova assinatura” e “Agenda” ficam empilhados; lado a lado, o primeiro cortava em 360dp.
- `FluxaTopBar` (Design System): título com até **2 linhas** em vez de 1, porque nomes longos (“Música Premium Família”, “Pagamento de assinatura”) eram cortados e o nome completo não aparecia em outro lugar da tela. Afeta todos os títulos de telas secundárias.
- `SubscriptionVisualSeed` (androidTest) popula dados de revisão só com `-e fluxaSeed 1`; a suíte normal o ignora.

## Tests

- JVM: `SubscriptionViewModelsTest` (17), com `FakeSubscriptionDataSource` usando `SubscriptionCalendar` do domínio e gravando no `FakeTransactionSource`/`FakeCreditSource`. Cobre:
  - lista ativa/pausada/cancelada;
  - formulário mensal/conta, anual/cartão, inválidos e edição;
  - agenda pendente/vencida/paga/cobrada/pulada e meia-noite;
  - pagar, cobrar, limite insuficiente, pular, já resolvida, futura, pausa/retomada/cancelamento, intervalo pausado e bloqueio de pagamento pausado;
  - Caixa em modo leitura e origem de parcela.
  Os testes também pegaram dois defeitos reais de toque rápido (toque duplo e data recém-escolhida), corrigidos no `SubscriptionOccurrenceViewModel`.
- Instrumentados: `SubscriptionFlowTest` (7), cobrindo:
  - mensal em conta criada pela UI → pagar → Caixa (modo leitura e Ver assinatura);
  - anual R$ 600 com uma única ocorrência;
  - cartão → compra e limite;
  - limite insuficiente (continua pendente);
  - pular vencida (histórico preservado);
  - pausar/retomar (intervalo não gerado) e cancelar (terminal);
  - persistência entre aberturas.

## Pendências

1. Contrato de ocorrências **por assinatura** (`observeOccurrences(subscriptionId, …)`). Hoje o detalhe usa `observeOccurrences` geral e filtra por ID.
2. Equivalente mensal de anuais só se o backend fornecer.
3. Estorno de desfechos, cobrança automática, notificações e WorkManager (fora do escopo).
4. Mudar o modo escuro com o app aberto volta para o Início (comportamento anterior).

## Base recomendada

`claude/fluxa-android-sprint-5b-frontend`.
