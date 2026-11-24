# RC1 — Validação física no Samsung Galaxy S23

**Estado deste documento: preparado, não executado no S23.** Os resultados do AVD constam no HANDOFF e não substituem estes testes.
Usar a APK Fluxa-1.0.0-RC1-s23.apk e conferir o SHA-256 no [HANDOFF](HANDOFF.md). Durante a RC, somente corrigir bugs encontrados; não adicionar features.

## Registro da sessão

- Responsável / data / duração:
- Modelo e variante do S23 (sem IMEI/serial):
- Android / One UI / patch de segurança:
- APK / SHA-256:
- Resolução efetiva / densidade / tamanho de exibição:
- Modo de suavidade da tela e frequência (incluindo 120 Hz, conforme configuração):
- Teclado / idioma:
- Tema efetivo / fonte normal ou ampliada:
- Biometria cadastrada / credencial de fallback (não registrar PIN):
- Modo avião / Wi-Fi / dados:
- Resultado por item: PASSOU / FALHOU / NÃO EXECUTADO, evidência e ID do bug.

## Instalação e partida fria

- [ ] Preferir primeira instalação limpa, sem importar dados de builds antigas não relacionadas.
- [ ] Conferir assinatura/checksum e instalar a APK conforme HANDOFF, sem Android Studio no telefone.
- [ ] Caso já exista com.fluxa.personal, parar e verificar a origem/assinatura. Não desinstalar automaticamente: a remoção apaga dados locais. Fazer backup verificado antes de uma remoção deliberada.
- [ ] Ícone adaptativo/temático no launcher One UI, nome Fluxa e abertura pelo ícone.
- [ ] Splash, primeiro cold start, abertura após force-stop e ausência de flash claro no tema escuro.
- [ ] Nenhuma solicitação de contatos, localização, câmera, microfone ou armazenamento amplo. A autenticação do dispositivo e a escolha explícita de documentos pelo SAF são esperadas.
- [ ] Abrir e usar em modo avião, Wi-Fi/dados desligados.

## Visual, tela e teclado

- [ ] Registrar resolução/densidade reais; não presumir equivalência ao AVD. Testar a configuração usual da tela e 120 Hz quando habilitado.
- [ ] Claro e escuro: com a preferência padrão SYSTEM, alternar o tema do Android. Se um backup trouxer tema explícito, registrar o tema efetivo. Não exigir a entrada Configurações “Em breve”.
- [ ] Fonte normal e ampliada do Android, registrando a configuração usada.
- [ ] Início, Caixa, Cartões, compra/parcelas, Assinaturas, Metas, Limite Seguro, Backup e Segurança sem clipping.
- [ ] Sheets opacos, alça, scrim, botões e fechar acessíveis; navegação inferior e barras do sistema corretas.
- [ ] Navegação por gestos e, se utilizada, barra de três botões; voltar em formulário, detalhe e sheet.
- [ ] Teclado Samsung e/ou Gboard efetivamente configurado: receita/despesa, compra, meta e senha de backup.
- [ ] adjustResize, campos/botões não cobertos, rolagem e ação IME. Conferir sheets de pagamento/reserva com teclado aberto.
- [ ] Quatro abas e botão central abrem os destinos esperados, sem navegação duplicada.

## Biometria física e App Lock

- [ ] Ativar “Exigir autenticação para abrir o app”: autenticação física real antes de persistir a ativação.
- [ ] Testar impressão digital cadastrada e credencial do dispositivo como fallback, conforme disponibilidade do S23.
- [ ] Cancelar ao ativar mantém a configuração anterior.
- [ ] Com App Lock ativo: force-stop → abrir → barreira → prompt nativo → autenticar → conteúdo.
- [ ] Cancelar o prompt mantém o bloqueio; tocar Desbloquear permite nova autenticação. Sem loop de prompt nem flash financeiro.
- [ ] Desativar App Lock exige nova autenticação; cancelar não o desativa.
- [ ] Observar erro/indisponibilidade de biometria e fallback sem bloquear permanentemente o acesso.
- [ ] Voltar não permite contornar a barreira.

## Auto-lock e process death

- [ ] Imediatamente: autenticar → Home → voltar → nova autenticação.
- [ ] Após 1 minuto: voltar antes do limite mantém sessão; sair de novo e voltar após o limite bloqueia.
- [ ] Não é necessário repetir esperas reais de 5 e 15 minutos; registrar ao menos a política não imediata acima.
- [ ] Rotação/recriação durante uso não deve agir como background real.
- [ ] Abrir uma tela interna, habilitar bloqueio, fazer force-stop pelo Android ou ADB e reabrir: dados persistidos, novo bloqueio e nenhum flash financeiro.
- [ ] Não exigir restauração de senha transitória nem back stack após force-stop.

## Recent Apps e screenshots no One UI

Registrar também os estados de App Lock e Ocultar valores para distinguir os três controles.
- [ ] Proteção de tela OFF: screenshot normal e comportamento do card em Recent Apps.
- [ ] Proteção de tela ON: tentar screenshot pela combinação física do S23 e registrar mensagem/resultado.
- [ ] Com proteção ON, observar exatamente Recent Apps: card vazio, placeholder, conteúdo oculto ou outro comportamento. Não presumir igual ao AVD.
- [ ] Desligar proteção restaura captura sem flag residual; conferir após reabertura/rotação.
- [ ] Alternar proteção não muda App Lock ou Ocultar valores.

## Dataset financeiro e Limite Seguro

Inserir dados fictícios e realistas pela interface; não usar seeders, credenciais bancárias ou dados pessoais.
- [ ] Criar conta, categoria de receita e de despesa; conferir a continuidade do histórico dos registros.
- [ ] Cenário de referência antes de outras obrigações: conta elegível com saldo R$ 5.000,00 e reserva em meta de R$ 1.500,00 → disponível R$ 3.500,00; sem obrigações conhecidas, limite seguro R$ 3.500,00.
- [ ] Criar receita e despesa, conferir saldos e histórico no Início/Caixa.
- [ ] Criar cartão, compra parcelada e pagar uma parcela; conferir compra, parcelas, pagamento e efeito no Caixa.
- [ ] Criar assinatura e resolver uma cobrança; conferir histórico.
- [ ] Criar meta, reservar e liberar valor; conferir saldo real, reservado e disponível, sem dupla dedução.
- [ ] Conferir reação do Limite Seguro a reservas e obrigações.
- [ ] Abrir detalhes/breakdown: datas, obrigações consideradas e explicação devem corresponder aos registros, não apenas ao número do cartão.
- [ ] Fechar/reabrir e conferir persistência; não mudar regras de cálculo durante esta RC.

## Backup, restore, Files e KDF

Usar senha de teste guardada separadamente; não incluí-la em relatórios.
- [ ] Exportar pelo seletor real do Samsung/Android para uma pasta local conhecida.
- [ ] Conferir extensão .fluxa, arquivo no Samsung My Files/Files e tamanho coerente. Não deve exibir dados financeiros em texto legível.
- [ ] Alterar os dados no Fluxa depois do export.
- [ ] Restaurar o arquivo anterior, informar senha, escolher destino do preventivo, confirmar e ir para Início.
- [ ] Conferir que o estado anterior voltou, com histórico/vínculos e preferências portáveis.
- [ ] Conferir preventivo criado; configurações locais de segurança não devem vir do backup.
- [ ] Senha errada, cancelamento do picker/confirmar e interrupção não podem substituir dados indevidamente.
- [ ] SAF não deve causar rebloqueio indevido durante o fluxo autorizado; sair de verdade depois segue a política configurada.
- [ ] Usar Samsung My Files e DocumentsUI quando acionado. Se Drive aparecer como provider, não é necessário testar cloud.
- [ ] Medir aproximadamente: export ____ s; abrir backup ____ s; restore ____ s.
- [ ] Registrar feedback, travamento, memória percebida e responsividade. Referência histórica do AVD: cerca de 2–5 s; não é resultado nem meta obrigatória do S23.
- [ ] Reabrir o app após restore e conferir Dashboard, Caixa e Limite Seguro.

## Privacidade e TalkBack em português

- [ ] Ocultar valores → fechar/force-stop → abrir: preferência permanece.
- [ ] TalkBack em português: percorrer Início, Caixa, Meta, Limite Seguro, Backup, Segurança e barreira bloqueada.
- [ ] Sem leitura duplicada, foco perdido, ação sem rótulo ou quantia financeira exposta nas telas de consulta/feedback com valores ocultos.
- [ ] “Valor oculto” anunciado; testar também valores negativos.
- [ ] Por contrato, percentual da meta permanece visível e campos de edição mostram o valor sendo editado; não confundir com vazamento nas telas de consulta.
- [ ] Sheet anuncia abertura, foco alcança fechar/confirmar e retorna coerentemente.
- [ ] Enquanto bloqueado, nenhum conteúdo financeiro alcançável por TalkBack.

## Offline e uso contínuo

- [ ] Modo avião com Wi-Fi/dados desligados: navegar, cadastrar, consultar, pagar, reservar, exportar/restaurar sem depender de rede.
- [ ] Usar por um período razoável, registrando duração e telas: sem crash, ANR, travamento, perda de estado ou recomposição/navegação estranha.
- [ ] Reiniciar o app e conferir novamente o estado financeiro e a proteção.

## Classificação e registro de bugs

**BLOCKER:** perda/corrupção de dados; backup não restaura; bypass de lock; crash recorrente; Limite Seguro incorreto. Impede aprovar RC.
**HIGH:** ação financeira importante impossível; autenticação impede acesso; sheet essencial inutilizável.
**MEDIUM:** layout quebrado ou fluxo confuso que permite contorno.
**LOW:** falha menor de texto, espaçamento ou ícone.
**COSMETIC:** detalhe estritamente visual sem prejuízo funcional.

Para cada problema:
- ID / título / classificação:
- APK e SHA-256:
- Modelo / Android / One UI / teclado / tema / fonte / tela / frequência:
- Estado de App Lock, timeout, proteção de tela, privacidade e rede:
- Passos para reproduzir:
- Resultado esperado / observado:
- Frequência e impacto; contorno, se houver:
- Evidências com dados de teste e logs revisados, sem PIN, senha de backup, backup real ou banco real:

## Decisão da RC física

- [ ] Todos os casos obrigatórios executados ou justificados.
- [ ] Nenhum BLOCKER/HIGH aberto sem decisão explícita.
- Resultado: APROVADA / REPROVADA / INCOMPLETA.
- Responsável / data / bugs pendentes:

Aprovação no S23 não habilita publicação Play: SDK/tooling/API 36, runtime 16 KB e assinatura definitiva são etapas posteriores. Não gerar AAB/publicação nesta tarefa.
