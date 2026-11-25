# Fluxa 1.0.2 — Marca final (Monograma fluido)

## Base

- Base: `d0d3d09` (v1.0.1). Branch `claude/fluxa-brand-icon-concepts`. Worktree `F:/fluxa-app-worktrees/claude-brand`.
- Um commit local, com as quatro propostas (etapa 1) e a marca final (etapa 2). Sem push, merge ou deploy. Checkout principal intocado.
- Nenhuma funcionalidade nova. Kotlin de produção, domínio, Room v7, migrations, segurança, regras, rotas e telas sem diff. No app mudam só ícone, splash (pelo ícone) e versão.

## Símbolo

Proposta A refinada: hastes com a mesma largura (11,4), fenda de 5,4 (era 4,6), ondas da mesma família de curva e compensação óptica de +1,2 u. Geometria única em `marketing/branding/final/tools/mark.py`, de onde saem os SVG e os vetores Android.

## Android

- `ic_launcher_foreground.xml`: novo vetor `#FFFDF7`.
- `ic_launcher_monochrome.xml`: novo, silhueta própria para ícones temáticos (antes reaproveitava o foreground).
- Fundo: `@color/fluxa_icon_background` `#14414D`, sem mudança.
- Splash: usa o mesmo foreground sobre `#0E171B`, claro e escuro. `launch.xml` sem mudança.
- Safe zone: raio máximo de **27,5 dp** a partir do centro (limite 33 dp). A primeira versão, com 30,8 dp, estava dentro da safe zone mas parecia apertada ao lado dos vizinhos no launcher real e foi reduzida.

## APK

- `F:/fluxa-app-worktrees/claude-brand/android/artifacts/v1.0.2/Fluxa-1.0.2-s23.apk`, ignorado pelo Git.
- 23.690.445 bytes. SHA-256 `811cb8a149a42717eea0cb3af997ea06821d032a036bff03bb0a4a20239eeab5`.
- `com.fluxa.personal`, versionName 1.0.2, versionCode 5, release não depurável, minSdk 30, targetSdk 34.
- Certificado SHA-256 `aa912c9e0650533c9ebe5a86c0fb780aa7af03cbc9a39eba5c9090aec1098aa9`, o mesmo da v1.0.1 (exigido pelo script).
- `scripts/package-v1.0.2.ps1` e `scripts/audit-v1.0.2-apk.py`: sem arquivos proibidos, classes de teste ou segredos.
- Um APK anterior deste mesmo dia (`eda08891…`, símbolo de 30,8 dp) foi descartado e sobrescrito.

## Atualização v1.0.1 → v1.0.2

AVD exclusivo API 34 (`emulator-5580`), modo avião, PIN descartável. APK v1.0.1 original instalada, dados representativos, tema DARK, valores ocultos, bloqueio e proteção de captura. `adb install -r` sem desinstalar nem limpar:

- versionCode 4 → 5; `firstInstallTime` preservado; APK instalada com o mesmo SHA-256.
- Tabelas, preferências, segurança, Limite seguro e Análises idênticos (`V102UpgradeTest`).
- O app abriu pedindo o PIN e com valores ocultos. Nova leitura depois do uso: sem diferença.
- Buffer de crash vazio.

## Testes

- JVM 326/326. Debug, Release e AndroidTest compilados. Lint debug e release: "No issues found".
- Suíte instrumentada completa offline, 200 casos (173 aprovados, 27 sob demanda pulados), 0 falhas na primeira execução em 320×640, 360, 390 escuro, 412 e 320 com fonte 1,3.
- Na própria APK release: contrato do pacote 2/2, Aparência 3/3, jornada financeira 1/1, Análises 10/10, tema claro e escuro após force-stop 2/2 cada.
- O contrato do pacote falhou uma vez na release por `EACCES` ao gravar o PNG das máscaras: um arquivo órfão da queda anterior do emulador. As asserções da safe zone já tinham passado. Sem o arquivo: 2/2.
- Uma execução da matriz foi interrompida e refeita do zero; não conta nos resultados acima.

## Capturas

[screens/](screens/): launcher, ícone temático, splash e máscaras. `mascaras.png` mostra o quadro de 108 dp inteiro; no launcher o recorte é de 72 dp.

## Marca e materiais

[marketing/branding/final](../../../marketing/branding/final/README.md): símbolo, logotipo, ícones, favicon e materiais para GitHub, LinkedIn e portfólio. README, favicon e web do repositório não foram alterados.

## Instalação

Manter a v1.0.1 instalada e aceitar **Atualizar**:

```powershell
adb install -r "F:/fluxa-app-worktrees/claude-brand/android/artifacts/v1.0.2/Fluxa-1.0.2-s23.apk"
```

## Pendências

- Aval no S23: ícone na One UI (a superelipse daqui é simulada), ícone temático da One UI e splash. O launcher pode guardar o ícone antigo em cache até reiniciar.
- Nenhuma pesquisa de marca registrada.
- targetSdk 34 e certificado de QA não servem para o Google Play.
