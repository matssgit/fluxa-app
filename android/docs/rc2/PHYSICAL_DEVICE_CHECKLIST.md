# RC2 — Samsung Galaxy S23

**Preparado; ainda não executado em aparelho físico.** Resultados do emulador estão no [handoff](HANDOFF.md). Registrar PASSOU / FALHOU / NÃO EXECUTADO por item, evidência e ID do problema. Não anotar PIN, senha, IMEI ou dados financeiros pessoais no relatório.

## Sessão

Responsável/data: ___; variante S23: ___; Android/One UI/patch: ___; resolução/densidade/fonte: ___; idioma/teclado: ___; suavidade normal/120 Hz: ___; tema: ___; biometria/fallback disponíveis: ___; modo avião/Wi-Fi/dados: ___.
APK: Fluxa-1.0.0-RC2-s23.apk. Conferir SHA-256 no handoff.

## Atualização RC1 → RC2

- [ ] Antes, anotar saldos, histórico, contas/categorias, cartões/parcelas, assinaturas, metas/reservas, Limite seguro, tema, ocultação e opções de segurança da RC1. Guardar backup e senha separadamente.
- [ ] Conferir certificado compatível. Abrir a RC2 e instalar como **atualização sobre a RC1**, sem desinstalar. Via ADB: `adb install -r Fluxa-1.0.0-RC2-s23.apk`.
- [ ] Se Android rejeitar assinatura: **PARAR**, preservar a instalação/dados e registrar erro/origem. Não desinstalar para contornar.
- [ ] Confirmar preservação de todos os registros e preferências acima, inclusive vínculos e pagamentos. Nenhuma migration ou reset deve ser exigido.
- [ ] Abrir pelo ícone, partida fria/force-stop, sem crash; nome Fluxa, splash e ícone adaptativo/temático One UI.
- [ ] Funcionar offline; nenhuma permissão de rede, contatos, câmera, localização, microfone ou armazenamento amplo.

## Análises com os dados preservados

- [ ] Abrir por Mais → Análises e Início → Ver análises.
- [ ] Conferir entradas/despesas/resultado com alguns lançamentos do mês; abertura de conta não é receita e compra no cartão só entra no realizado quando a parcela é paga.
- [ ] Conferir categoria, valor, percentual e rótulos com despesas conhecidas. Top 5 não renormaliza os percentuais.
- [ ] Conferir seis meses, inclusive meses vazios quando há pouco histórico; legenda, lista e resultado com sinal.
- [ ] Trocar mês, voltar ao atual e conferir limite de avanço. Projeção/assinaturas continuam contextualizadas a partir de hoje.
- [ ] Comparar disponível/projeção com reservas e obrigações cadastradas; não confundir projeção com Limite seguro.
- [ ] Conferir assinaturas ativas/equivalente mensal/custo anual; anual distribuído é contexto de orçamento.
- [ ] Conferir insights e ausência de números inventados em meses vazios/sem receita.

## Fluidez, OLED e acessibilidade

- [ ] Abrir, rolar rápido, trocar mês, voltar e entrar novamente em suavidade normal/120 Hz; registrar stutter, lag, frames parados ou gráfico piscando. Não é necessário medir FPS.
- [ ] Claro/escuro, brilho baixo/alto: petróleo, terracota, contraste, barras, labels e background no OLED.
- [ ] Fonte normal, 1,3 e 2,0; nomes longos e scroll sem corte, sobreposição ou rolagem horizontal.
- [ ] TalkBack em português: tela inteira, ordem/cabeçalhos/categorias/meses/gráfico e botões compreensíveis.
- [ ] Ativar Ocultar valores e repetir TalkBack: nenhum dinheiro em texto, descrição, insight ou label; gráfico oculto, sem revelar proporções/sinal.
- [ ] Com valores ocultos, conferir também Início e Limite seguro. Reabrir confirma persistência.

## Jornada financeira e navegação

- [ ] Receita → despesa → compra/parcelas → pagar/desfazer parcela → assinatura/pagar/desfazer → meta/reservar/liberar → Limite seguro → Análises: valores reativos, sem atualizar manualmente ou duplicar despesas.
- [ ] Saldo, reservado, disponível e compromissos coerentes entre Início, Caixa, cartões, metas, Limite seguro e Análises.
- [ ] Quatro destinos inferiores e sheet do botão central; sheet opaco, fechamento/voltar e gestos One UI.
- [ ] Teclado Samsung/Gboard nos formulários/sheets/senha, botões acessíveis com IME aberto; conferir rotação e barras do sistema.
- [ ] Virada de dia/mês com app aberto; base maior restaurada: tempo até conteúdo e resposta ao scroll.

## Segurança física

- [ ] Ativar App Lock autenticando por biometria real/fallback do S23. Cancelar ativação não muda a opção.
- [ ] Home → voltar → autenticação → Análises acessível; cancelar mantém barreira sem dinheiro visível. Force-stop/reabrir e botão Voltar não contornam bloqueio.
- [ ] Desativação exige autenticação; cancelamento preserva proteção. Política imediata e após 1 minuto conforme opção.
- [ ] FLAG_SECURE ON/OFF: screenshot físico e card em Recent Apps; registrar comportamento real One UI. Desligar remove proteção sem flag residual.
- [ ] Privacidade, captura e App Lock são independentes.

## Backup real na RC2

- [ ] Criar dados de teste na RC2; exportar backup criptografado pelo seletor Samsung/Android; guardar senha fora deste relatório.
- [ ] Alterar um dado, restaurar export anterior com preventivo escolhido no SAF; conferir histórico/vínculos, tema/privacidade e banco anterior.
- [ ] Análises antes/depois semanticamente iguais: resumo, categorias, seis meses, projeção, assinaturas e insights.
- [ ] Segurança local permanece do aparelho, sem importação do backup. App Lock não interrompe fluxo SAF autorizado.
- [ ] Senha errada/cancelamento não substituem dados. Registrar tempos aproximados de export/validação/restore e eventual travamento.
- [ ] Reabrir e conferir Início, Caixa, Limite seguro e Análises.

## Registro e freeze

Problema: ID ___; classe BUG / COMPATIBILITY / SECURITY / COSMETIC; severidade BLOCKER / HIGH / MEDIUM / LOW / COSMETIC; passos ___; esperado/observado ___; evidência sanitizada ___.

Perda de dados, cálculos incorretos, divergência SafeToSpend, restore quebrado, bypass de lock e crash recorrente bloqueiam a RC.
Ideias novas são POST-V1. API 36, 16 KB e signing definitivo pertencem à distribuição futura.

**PRODUCT FEATURE FREEZE — ACTIVE**
