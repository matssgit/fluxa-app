# Sprint 10-B — Revisão visual final e preparação da RC1

- Base: `e0f024e2c39b2a836261a6e16350f402566cfdd9` (`codex/fluxa-android-sprint-10a-release-hardening`).
- Branch: `claude/fluxa-android-sprint-10b-final-polish`. Worktree: `F:/fluxa-app-worktrees/claude-sprint-10b`.
- O commit de entrega é o que contém este documento. Sem push, merge ou deploy. Checkout principal intocado.
- Ambiente: AVD exclusivo API 34, `emulator-5580`. Nenhum aparelho físico disponível.
- Não mudaram: Room v7, migrations, crypto/KDF, formato `.fluxa`, `LocalSecurityManager`, cálculo do limite seguro, regras de cartões, assinaturas e metas. O diff de produção fica em `ui/` e `strings.xml`.

## Visual audit

`RcVisualCapture` (androidTest, só com `-e fluxaCapture <sufixo>`) percorre o app inteiro com dados sintéticos e fotografa cada tela. Foi executado **antes** das mudanças (390 claro, 390 escuro, 320 fonte 1,3, valores ocultos, banco vazio) e **depois** (os mesmos mais 320 fonte 1,0). Todos os defeitos abaixo saíram dessas imagens ou da leitura do TalkBack.

Padrão encontrado: a identidade estava coerente; os problemas se concentravam em (1) linhas "rótulo + valor" que esmagavam o texto em 320 dp/fonte 1,3, (2) navegação translúcida demais, (3) sheets sem cobrir as barras do sistema, (4) datas em três formatos, (5) ícones com significado errado, (6) ações reversíveis com aparência destrutiva.

## Dashboard

- O cartão principal explica por que *Saldo total − Reservado* pode diferir do disponível (contas fora do disponível). Antes o texto dava a entender uma subtração que não fechava.
- O alerta "reservado acima do saldo" só aparece quando há reserva; sem reserva, o texto é "O saldo disponível está negativo."
- Limite seguro: ícone neutro (carteira em vez de um "check", que parecia nota) e carregamento compacto, sem salto de layout.
- "Ver no Caixa" alinhado ao título. Estado vazio com botão de largura total.

## Caixa

- Overline, como nas outras abas (o título não muda mais de altura entre abas).
- Carrossel de contas e filtros sangram até a borda; cartões com a mesma altura. Com fonte grande, Reservado/Disponível empilham em vez de quebrar "Reserv/ado".
- "Movimentações" e a contagem deixam de brigar por espaço.
- Estado vazio: duas ações empilhadas ("Cadastrar conta", "Gerenciar contas").

## Cards

- Linha de parcela refeita: número, título e valor na primeira linha; vencimento e situação na segunda. Antes, em 320/1,3, "Pendente" saía uma letra por linha.
- Selo do número com largura mínima (1/10 e 10/10 alinham). Botões do cartão empilham quando não cabem.
- Resumo da compra com hierarquia única.

## Subscriptions

- Criar fica fixo embaixo, como em Contas e Categorias. Ícones próprios de pausar, retomar e pular.
- Pausar e retomar usam confirmação não destrutiva; cancelar, pular e desfazer continuam destrutivas.
- No histórico da própria assinatura a data vira o título, em vez de repetir o nome em cada linha.
- "Fonte" virou "Forma de pagamento"; "ocorrência" virou "cobrança".

## Goals

- Criar fixo embaixo. "Concluir meta" só fica habilitada quando o objetivo foi atingido (a regra já era essa; o botão parecia ativo) e a confirmação é não destrutiva.
- Ícone de liberar (−) em vez de "restaurar". "Faltam", "Data-alvo", "Saldo atual".
- Feedback com valores ocultos tinha erro de concordância ("O valor foram reservados"); agora é uma frase própria, sem valor.

## SafeToSpend

- Sinal colado ao valor (−R$ 120,00), igual às listas. Dica do breakdown menos técnica. Cálculo intocado.

## Backup

- Ações principais com o mesmo tamanho das demais telas. Campo de senha com a mesma altura dos outros campos (era 76 dp, agora 56 dp). Fluxo e textos de segurança intocados.

## Security

- Ícone de relógio no bloqueio automático; respiro entre rótulo e apoio dos switches; "Abrir configurações" sem corte. Nenhuma lógica alterada.

## Forms

- Chips de tipo de conta com o mesmo espaçamento dos outros formulários. Datas editáveis seguem em dd/mm/aaaa; **datas de leitura usam um único formato** ("6 dez. 2026").
- Botões quebram em até duas linhas com fonte ampliada em vez de cortar.

## Sheets

- Contêiner creme opaco. Sheet e scrim cobrem a janela inteira (antes sobrava uma faixa clara nas barras do sistema). No escuro sumiu a faixa tonal abaixo da alça.
- Altura limitada à área útil. Conferido com o teclado aberto em 320×640/fonte 1,3: campo, ações e fechar permanecem visíveis.

## Dark mode

Petróleo, sage e terracota mantidos; sem branco puro nem preto chapado novo. Revisado em 390 dp: Início, Caixa, cartão, compra, assinatura, metas, limite, backup, segurança, sheets e bloqueio.

## Compact screens

`isNarrowForText()` mede a largura dividida pela escala da fonte (≤ 300 por padrão) e empilha as linhas críticas. 320×640 com fonte 1,0 e 1,3 revisados. Sem overflow horizontal nem rótulo quebrado letra a letra nas telas fotografadas.

## Accessibility

- Estado e data deixaram de ser anunciados duas vezes. A data é lida por extenso.
- Sinais "+"/"−" decorativos ficaram fora da árvore de acessibilidade (a linha já diz Entrada/Saída).
- Estado continua com ícone + texto, nunca só cor. Alvos ≥ 48 dp preservados.

## TalkBack

Passe manual com o TalkBack real do AVD: toque por evento de kernel (deslizar e toque duplo) e leitura das frases enviadas ao sintetizador. Percorridos: Início, abas, Novo lançamento, formulário de despesa, Cartões e detalhe, Assinaturas e detalhe, Metas, detalhe e sheet de reserva, Limite seguro, Segurança, Backup, Mais, e Início com valores ocultos.

| Antes | Depois |
| --- | --- |
| "Menor folga: hoje. Menor folga: hoje" | "Menor folga: hoje" |
| "Positivo. Resultado do mês. Positivo. +R$ 850,00" | "Resultado do mês. Positivo. +R$ 850,00" |
| "Saída. −. Feira. …" | "Saída. Feira. …" |
| "24 de dezembro de 2026. 24 dez. 2026" | "24 de dezembro de 2026" |
| "Situação: Paga. Paga" | "Situação: Paga" |
| "Reservado, 24 de novembro… Reservado. 24 nov. 2026. +. R$ 400,00" | "Reservado. 24 de novembro de 2026. R$ 400,00" |

Observações não corrigidas (baixo impacto): em cartões clicáveis, um bloco interno ("Próxima cobrança", "Faltam") é uma parada de foco própria; a pílula de "Situação" no detalhe idem; ao abrir um sheet o TalkBack anuncia "Pop-Up Window" (texto do sistema, AVD em inglês). Voz e gestos reais: pendente em aparelho físico.

## Privacy

- "Ocultar valores" percorrido com TalkBack: todo valor é anunciado como "Valor oculto" e o sinal do resultado do mês também some.
- Capturas com valores ocultos revisadas nas telas de leitura e sheets: nenhum valor monetário visível. Feedback de reserva não inclui valor.
- Por desenho, percentual da meta continua visível e o formulário de **edição** mostra o valor que o usuário está editando.

## Navigation

Quatro abas + "+" central e voltar sem mudança de rota. A barra ficou praticamente opaca e o conteúdo esmaece por baixo dela. Seções que abrem tela cheia seguem sem barra.

## Visual fixes (antes/depois)

- [Parcelas 320/1,3](screens/antes-depois-parcelas-320-f13.jpg)
- [Caixa 320/1,3](screens/antes-depois-caixa-320-f13.jpg)
- [Navegação](screens/antes-depois-navegacao.jpg)
- [Sheet](screens/antes-depois-sheet.jpg)
- [Ações da meta](screens/antes-depois-metas-acoes.jpg)

Ícone e splash: revisados a partir da 10-A; sem defeito que justificasse redesenho. Animações: nenhuma adicionada.

## Screenshots

Em [screens/](screens/): `claro-*` (390 dp), `escuro-*` (390 dp), `compacto-*` (320 dp, fonte 1,3). Dados sintéticos.

## Tests

- **JVM**: 293 testes, zero falhas/erros/skips.
- **Builds** (offline): `assembleDebug`, `assembleRelease` (unsigned), `assembleDebugAndroidTest` aprovados.
- **Lint**: `lintDebug` e `lintRelease` com "No issues found".
- **Matriz instrumentada offline** (modo avião, Wi-Fi e dados desligados, PIN descartável no AVD), limpa na primeira execução:

| Configuração | Total | Aprovados | Pulados | Falhas |
| --- | --- | --- | --- | --- |
| 320×640 | 168 | 155 | 13 | 0 |
| 360 | 168 | 155 | 13 | 0 |
| 390, sistema escuro | 168 | 155 | 13 | 0 |
| 412 | 168 | 155 | 13 | 0 |
| 360, fonte 1,3 | 168 | 155 | 13 | 0 |
| 320×640, fonte 1,3 | 168 | 155 | 13 | 0 |

  Os 13 pulados são rotinas sob demanda (12 da 10-A mais `RcVisualCapture`). As que são protocolo foram rodadas à parte:

- **Morte de processo** (`RcProcessDeathTest`, force-stop real): 8/8 rotas (Início, Caixa, Cartões, Assinaturas, Metas, Limite seguro, Backup, bloqueio).
- **Segurança**: `LocalSecurityProcessDeathTest` seed/verify 2/2; tempo real de 1 minuto 1/1; aparelho sem bloqueio de tela 1/1. Na matriz: gate sem semântica financeira, bloqueio imediato em background real, credencial do Android (PIN), FLAG_SECURE, SAF sem rebloqueio indevido, rotação.
- **Backup** (na matriz): exportar cifrado, restaurar com cópia preventiva, senha errada sem tocar no banco, arquivo corrompido, cancelamento, reabertura.
- **Assinaturas após force-stop**: settle/undo/verify 3/3.
- **Smoke financeiro** (`RcFinancialSmokeTest`, novo, roda na suíte): receita → despesa → cartão e compra em 2x → pagar parcela → criar e pagar assinatura → meta e reserva, tudo pela interface. A cada passo confere o disponível no Início e no Caixa e o limite seguro no Início, no detalhe e no contrato, com o esperado recalculado pelo teste a partir das parcelas e cobranças em aberto. Passou nas 6 configurações. Na primeira execução falhou por um seletor errado do próprio teste (aba "Mais" não tem `list-more`), corrigido antes da matriz.
- **Testes alterados**: `GoalFlowTest` e `SubscriptionFlowTest` mudaram uma linha cada (botão de criar saiu da lista e foi para a barra fixa).

A matriz 390 usa o sistema escuro, mas algumas fixtures fixam LIGHT; o conteúdo escuro foi revisado nas capturas dedicadas.
O TalkBack exigiu `adb root` e o log detalhado do TalkBack **somente no AVD descartável**; tudo foi revertido (dados do TalkBack limpos, adbd sem root, PIN removido, display e rede restaurados) antes da matriz ou ao final.

Reprodução (em `android/`):
```powershell
.\gradlew.bat --offline testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lintDebug lintRelease
.\gradlew.bat --offline :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.deviceCredential=PIN_DO_AVD"
```

## RC status

**READY FOR PHYSICAL DEVICE RC.** Sem bloqueador visual, de navegação, privacidade, segurança ou dados conhecido no AVD.

Pendências conhecidas que não impedem o teste em aparelho:
- Análises e Configurações seguem "Em breve" no Mais (pós-v1, decisão da 10-A).
- targetSdk 34: a RC **não** é publicável no Google Play sem a etapa API 36 descrita na 10-A.
- APK release sem assinatura de produção.
- Cartões do carrossel do Caixa têm a altura do mais alto; com uma conta em alerta os demais ficam com espaço vazio.
- Paradas de foco extras do TalkBack descritas acima.

## Physical device checklist

- [ ] Instalar, abrir, partida fria em claro e escuro.
- [ ] Biometria real (digital/rosto), cancelamento, bloqueio por tentativas e PIN de reserva.
- [ ] Bloqueio imediato e por tempo; app recentes com proteção de tela ligada e desligada.
- [ ] Exportar e restaurar pelo Files real (senha certa, senha errada, interrupção).
- [ ] Tempo do KDF em aparelho intermediário.
- [ ] TalkBack com voz em português; fonte do sistema 1,3 e tela pequena.
- [ ] Barra de navegação de 3 botões e por gestos: sheets até a borda e navegação flutuante.
- [ ] Teclado do fabricante nos sheets de reserva e pagamento.
- [ ] Ícone adaptativo e temático no launcher do fabricante.
