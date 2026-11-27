# Funcionalidades

O que o Fluxa 1.0.2 faz hoje, conferido no código (`android/app/src/main`). Este documento não lista ideias nem planos; para isso, veja o [roadmap](ROADMAP.md).

A interface está em português do Brasil e os valores são em reais (BRL).

## Navegação

Barra inferior com **Início**, **Caixa**, **Cartões** e **Mais**. No centro fica o botão **+**, que abre **Nova despesa**, **Nova receita** e **Nova compra**.

A aba **Mais** reúne Assinaturas, Metas, Análises, Categorias, Backup e restauração, Segurança e Aparência. As contas são gerenciadas pelo Caixa. O Início também tem atalhos para as Análises e para o detalhe do Limite seguro.

## Início

- **Saldo disponível**: a soma das contas ativas incluídas no disponível, menos o que está reservado em metas.
- **Saldo total** e **Reservado**, mostrados separadamente.
- **Limite seguro** dos próximos 30 dias, com o dia de menor folga e o acesso ao cálculo.
- Botão de olho para **ocultar valores**. A escolha fica salva.

## Contas

- Tipos: conta corrente, carteira e poupança.
- Cada conta tem nome, saldo inicial e data de abertura. Movimentos anteriores à data de abertura não entram no saldo.
- Uma conta pode ficar **fora do saldo disponível**, como uma poupança que você não quer contar como dinheiro livre.
- Contas são **arquivadas**, nunca apagadas, para preservar o histórico.

## Categorias

- Categorias próprias de **entrada** ou de **saída**, com cor e ícone.
- Uma despesa só aceita categoria de saída, e uma receita só aceita categoria de entrada.
- Categorias também são arquivadas, não apagadas.

## Receitas e despesas (Caixa)

- Lançamento com tipo, valor, conta, categoria, data e descrição opcional.
- **Não é possível lançar com data futura.** O Fluxa registra o que aconteceu. Compromissos futuros entram como parcelas ou assinaturas.
- Edição e **cancelamento** de lançamentos. Um lançamento cancelado deixa de contar no saldo e nas análises.
- Histórico com filtros de período (este mês, mês anterior, 90 dias e outros) e de tipo (tudo, entradas, saídas).
- Lançamentos gerados por pagamento de parcela ou de assinatura não podem ser editados diretamente. Para corrigir, desfaça o pagamento na origem.

## Cartões de crédito

- Cartão com nome, cor, **limite total**, **dia de fechamento** e **dia de vencimento**.
- **Limite disponível** = limite total menos as parcelas ainda não pagas. O limite total não pode ser reduzido abaixo do que já está comprometido.
- Cartões podem ser arquivados.

## Compras e parcelas

- Compra à vista ou parcelada, com título, valor total, número de parcelas, categoria de saída e data.
- A compra só é aceita se couber no limite disponível.
- A primeira parcela vence no vencimento seguinte ao fechamento em que a compra caiu. Os centavos que não dividem igualmente vão para as últimas parcelas.
- **Pagar parcela**: escolhendo a conta e a data, o app cria a despesa correspondente naquela conta.
- **Desfazer pagamento**: remove a despesa criada e devolve a parcela para "em aberto".
- **Cancelar compra**: permitido enquanto nenhuma parcela tiver sido paga. As parcelas em aberto são canceladas e o limite é liberado.

> Não existe registro de estorno parcial feito pela loja nem de crédito avulso na fatura. Também não há fatura fechada como objeto: o app acompanha parcelas, não faturas.

## Assinaturas

- Assinatura com nome, valor, categoria, frequência **mensal** ou **anual**, dia de cobrança e origem do pagamento: uma **conta** ou um **cartão**.
- **Agenda** com as próximas cobranças e as vencidas.
- Cada cobrança pode ser:
  - **paga**: em uma conta, vira uma despesa;
  - **cobrada no cartão**: vira uma compra à vista naquele cartão;
  - **pulada**.
- **Desfazer**: reabre uma cobrança paga ou cobrada. Se a parcela da cobrança no cartão já tiver sido paga, desfaça esse pagamento antes.
- Editar valores vale a partir da data da mudança. As cobranças antigas mantêm o valor que tinham.
- Assinaturas podem ser **pausadas**, **retomadas** e **canceladas**.

## Metas e reservas

- Meta com nome, valor-alvo, conta vinculada e data-alvo opcional.
- **Reservar** e **liberar** dinheiro. A reserva não pode passar do valor-alvo nem do disponível.
- Progresso, quanto falta e o saldo da conta: saldo atual, reservado e disponível.
- **Concluir** quando o reservado atingir o alvo. **Arquivar** quando não houver nada reservado.
- A conta de uma meta não pode ser trocada depois que ela já tem movimentos.

## Limite seguro

- Quanto pode ser gasto **hoje** sem que o saldo disponível fique negativo em algum dia dos **próximos 30 dias**.
- Considera as parcelas de cartão em aberto e as cobranças de assinatura previstas no período.
- Mostra o **dia de menor folga**, o saldo projetado e a lista de compromissos considerados, em ordem.
- Não considera gastos que ainda não foram registrados nem receitas futuras, porque o app não registra receitas futuras.

Cálculo detalhado: [FINANCIAL_DOMAIN.md](FINANCIAL_DOMAIN.md#limite-seguro).

## Análises

Em **Mais → Análises**, mês a mês (meses futuros ficam desabilitados):

- **Resumo do mês**: entradas, despesas, resultado, taxa de economia e número de movimentações.
- **Gastos por categoria**, com percentuais que somam exatamente 100%.
- **Últimos 6 meses**: entradas e despesas por mês.
- **Projeção de 30 dias**: disponível hoje, compromissos do período, saldo final e menor saldo.
- **Assinaturas**: quantidade ativa e custo mensal e anual equivalente.
- **Observações** automáticas, como mês negativo, maior categoria, despesas acima do mês anterior e resultado positivo.

Compras no cartão entram nas análises **quando a parcela é paga**, para não contar o mesmo gasto duas vezes.

## Backup e restauração

- **Fazer backup**: gera um arquivo `.fluxa` protegido por senha (mínimo de 8 caracteres) no local que você escolher pelo seletor de arquivos do Android.
- **Restaurar**: valida o arquivo e a senha antes de qualquer mudança e salva uma **cópia preventiva** dos dados atuais. Só então substitui os dados.
- O backup leva os dados financeiros, o tema e a opção de ocultar valores. As **configurações de segurança não são restauradas**, porque pertencem ao aparelho.
- **Não há backup automático nem na nuvem.** O Fluxa também desativa o backup automático do Android para não copiar o banco sem criptografia.

Detalhes: [BACKUP_AND_SECURITY.md](BACKUP_AND_SECURITY.md).

## Segurança

- **Bloquear o Fluxa** com a autenticação do Android: biometria forte ou o bloqueio de tela (PIN, padrão ou senha). Vem desligado.
- **Bloqueio automático** ao sair do app: imediatamente, após 1, 5 ou 15 minutos.
- **Proteger conteúdo da tela**: bloqueia capturas e gravações de tela e esconde a prévia em Recentes. Vem desligado.
- Ativar ou desativar o bloqueio exige autenticar.

## Aparência

**Automático** (segue o sistema), **Claro** ou **Escuro**, em **Mais → Aparência**.

## Limitações conhecidas

- Somente Android 11 ou superior. Não há versão para iOS, web atual ou desktop.
- Moeda única: real (BRL).
- Não há sincronização entre aparelhos. A troca de aparelho é feita por backup e restauração.
- Não há importação de extrato, OFX ou CSV, nem exportação para planilha.
- Não há transferência entre contas como operação própria.
- Não há lembretes ou notificações.
- O banco local não é criptografado pelo app. Ele depende da proteção do Android (veja [BACKUP_AND_SECURITY.md](BACKUP_AND_SECURITY.md)).
