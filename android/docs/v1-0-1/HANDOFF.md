# Fluxa 1.0.1 — Tema e paleta

## Base

- Base: `35c7b5b8532e47f283f33c5f0a85689ef53ec29b` (`codex/fluxa-android-v1-final`).
- Branch: `claude/fluxa-android-v1-0-1-theme`. Worktree: `F:/fluxa-app-worktrees/claude-v101`.
- Dois commits locais: `169b913` (parcial) e o que contém este documento. Sem push, merge ou deploy. Checkout principal intocado.
- Domínio, Room v7, migrations, analytics, backup, segurança, regras e rotas de navegação sem diff.

## Auditoria

1. **Claro "não implementado":** o tema claro existia e a preferência era salva, mas nenhuma tela permitia mudá-la.
2. **Verde demais:** marca, seleção, receita, sucesso, bordas, sombras, textos e o fundo escuro saíam do mesmo verde-pinho (matiz ~166°).
3. **Web:** usa o mesmo verde (`#173F36`), chamado lá de "verde petróleo". A direção pedida (menos verde) foi resolvida levando a marca para um petróleo azulado e mantendo creme, sálvia e terracota, além dos anéis orgânicos do cartão principal da web.

## Tema e paleta

| Papel | Claro | Escuro |
| --- | --- | --- |
| Fundo | `#F6F2EA` creme | `#0E171B` petróleo quase preto |
| Cartão | `#FFFDF9` | `#162227` |
| Destaque (saldo, limite, meta) | `#123A45` | `#1B4A57` |
| Marca (texto, ícone, progresso) | `#14414D` | `#A3C4CB` |
| Ação principal, "+", switch | `#14414D` | `#2F6E80` |
| Navegação ativa | `#DCE7E9` | `#23424B` |
| Apoio (selos de ícone) | `#E6EAE2` sálvia | `#23393F` |
| Receita / positivo | `#2C6270` | `#8CBFC9` |
| Despesa / alerta | `#964831` | `#E29A80` |
| Bordas e sombras | neutras quentes | creme translúcido |

- Chips selecionados em petróleo cheio. Campos claros em creme limpo com borda, no lugar do cinza-esverdeado.
- Saldo, limite e meta ganharam as formas orgânicas discretas da web.
- Navegação: indicador petróleo, sem esverdeado; opacidade de 96%.
- Gráfico: entradas em petróleo e despesas em terracota. ΔE para daltonismo 20,4 (claro) e 20,7 (escuro). Textos de receita × despesa: 9,5 e 10,1 (antes ~5). O validador ainda marca esses tons fora da faixa de saturação/luminosidade; é a identidade e foi aceito. Sinal, rótulo e posição reforçam.
- Todos os pares de texto testados passam WCAG AA nos dois temas (teste JVM, ampliado com os tokens novos).
- Splash, cortina de bloqueio e fundo do ícone acompanham a paleta.
- "Petróleo" entrou como opção de cor de categoria e cartão. As cores já escolhidas não mudam; categorias verdes do usuário continuam verdes.

## Troca de tema

Mais → **Aparência**: Automático, Claro, Escuro. Grava só o tema (`LocalPreferences.setTheme`), sem tocar em ocultar valores ou segurança. É o único controle novo, pedido explicitamente. A aba Mais passou a respeitar a barra de status ao rolar, como as outras.

## APK

- `F:/fluxa-app-worktrees/claude-v101/android/artifacts/v1.0.1/Fluxa-1.0.1-s23.apk`, ignorado pelo Git.
- 23.690.389 bytes. SHA-256 `66ec2280f9c0aa69bdf54a09ac5d388c0922e215bfe1d64813ac6c40c6fd5bcd`.
- `com.fluxa.personal`, versionName 1.0.1, versionCode 4, release não depurável, minSdk 30, targetSdk 34.
- Certificado SHA-256 `aa912c9e0650533c9ebe5a86c0fb780aa7af03cbc9a39eba5c9090aec1098aa9`, o mesmo da v1.0.0 (exigido pelo script). É o certificado local de QA, não identidade de loja.
- `scripts/package-v1.0.1.ps1` e `scripts/audit-v1.0.1-apk.py`: sem arquivos proibidos, classes de teste ou segredos.

## Atualização v1.0.0 → v1.0.1

AVD exclusivo API 34, modo avião, com PIN descartável. Instalada a APK v1.0.0 original, criado o conjunto representativo (contas, lançamentos, cartão, parcelas, assinaturas, meta, reserva), tema DARK, valores ocultos, bloqueio imediato e proteção de captura. `adb install -r` sem desinstalar nem limpar:

- versionCode 3 → 4; `firstInstallTime` preservado; APK instalada com o mesmo SHA-256.
- Todas as tabelas, preferências, segurança, Limite seguro e Análises idênticos (`V101UpgradeTest`). O app abriu pedindo o PIN e com valores ocultos.
- Na interface da release: Claro → fechar → reabrir → Claro; Escuro → fechar → reabrir → Escuro. Nas duas verificações só o tema mudou em relação à v1.0.0.
- Buffer de crash vazio.

## Testes

- JVM 326/326. Debug, Release e AndroidTest compilados. Lint debug e release: "No issues found".
- Suíte instrumentada completa offline, 197 casos (173 aprovados, 24 sob demanda pulados):

| Perfil | Resultado final |
| --- | --- |
| 320×640 | 0 falhas (2ª execução) |
| 360 | 0 falhas |
| 390 escuro | 0 falhas |
| 412 | 0 falhas |
| 320, fonte 1,3 | 1 falha na 2ª execução, ver abaixo |

  - 1ª execução: em 320 e 320/1,3 falharam 1 e 2 casos do teste novo `AppearanceFlowTest`, cujo toque por coordenada caía na navegação flutuante. O teste passou a usar clique semântico, como os demais; o app não mudou.
  - 2ª execução de 320/1,3: falhou `LocalSecurityActivityTest.credentialOnlyDevice…` (prompt real de PIN do Android), que havia passado na 1ª. A classe foi repetida três vezes no mesmo perfil: 9/9 em todas. É uma oscilação já vista na 11-B; causa não investigada. Segurança não foi alterada neste patch.
- Na própria APK release: Aparência 3/3, contrato do pacote 2/2, jornada financeira 1/1, Análises 10/10.
- Force-stop: tema claro e escuro 2/2 cada, Análises 2/2, segurança 2/2.
- Visual: 360 claro e escuro (app inteiro); 320/1,3 claro (app inteiro); 390 claro com banco vazio; Mais em 320/1,3 claro e escuro e em 412.
- Não feito: passe novo de TalkBack (os chips são o mesmo componente já verificado; "Aparência" é cabeçalho), perfil 360/1,3 e aparelho físico.

## Capturas

[screens/](screens/), dados sintéticos.

## Instalação

Manter a v1.0.0 instalada e aceitar **Atualizar**:
```powershell
adb install -r "F:/fluxa-app-worktrees/claude-v101/android/artifacts/v1.0.1/Fluxa-1.0.1-s23.apk"
```

## Pendências

- Aval no S23: claro sob sol, escuro em OLED, Recentes, ícone no launcher da One UI.
- Seguem da v1.0.0: targetSdk 34 e certificado de QA não servem para o Google Play.
