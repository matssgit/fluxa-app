# Configuração recomendada do repositório

Para aplicar manualmente em `github.com/matssgit/fluxa-app` → **Settings**. Nada disso foi alterado remotamente.

## About (engrenagem ao lado de "About", na página inicial do repositório)

- **Description:** `Controle financeiro pessoal para Android, gratuito e offline. Kotlin, Jetpack Compose e Room.`
- **Website:** `https://matheusantanadev.vercel.app/`
  - Alternativa, quando a release existir: `https://github.com/matssgit/fluxa-app/releases/latest`
- **Topics:** `android`, `kotlin`, `jetpack-compose`, `room`, `offline-first`, `personal-finance`, `finance-app`, `material3`, `privacy`, `open-source`
- Marcar **Releases**. Desmarcar **Packages** e **Deployments**, se não forem usados.

## Social preview

Settings → General → **Social preview** → enviar `marketing/launch/github/social-preview-1280x640.png`.

## Features (Settings → General → Features)

| Recurso | Recomendação | Motivo |
| --- | --- | --- |
| Issues | **Ativar** | Bugs e sugestões, com os modelos em `.github/ISSUE_TEMPLATE` |
| Discussions | **Opcional** | Útil para dúvidas de uso e ideias abertas. Se ativar, crie as categorias "Dúvidas" e "Ideias" e acrescente o link em `.github/ISSUE_TEMPLATE/config.yml` |
| Wiki | Desativar | A documentação fica em `docs/` |
| Projects | Desativar | Opcional |
| Sponsorships | Desativar | O projeto não aceita contribuições financeiras |

## Segurança (Settings → Code security)

- **Private vulnerability reporting:** ativar. O `SECURITY.md` e o `config.yml` dos modelos de issue apontam para ele.
- **Dependabot alerts:** ativar.
- **Secret scanning** e **push protection:** ativar, se disponíveis para o repositório.

## Labels (Issues → Labels)

Criar: `bug`, `dados`, `segurança`, `sugestão`, `documentação`, `aguardando informação`, `não planejado`. O uso de cada uma está em `docs/ROADMAP.md`.

## Branch principal

- Proteger `main` (Settings → Branches): exigir que a verificação **Android** (workflow `android.yml`) passe antes do merge e impedir *force-push*.

## Releases

- Tag `v1.0.2` no commit publicado.
- Título: `Fluxa 1.0.2`.
- Corpo: `docs/releases/1.0.2.md`, sem o comentário inicial e com o SHA-256 preenchido.
- Assets:
  - `Fluxa-1.0.2.apk`
  - `Fluxa-1.0.2.apk.sha256`
  - `THIRD_PARTY_NOTICES.md`
- Marcar **Set as the latest release**. Não marcar *pre-release*.

## Passo a passo da publicação

Execute só depois do aval no S23. Todos os comandos rodam na sua máquina, com as suas credenciais do GitHub.

### 1. Ajuste final do README

Remova o bloco "Ainda não há versão pública publicada" do `README.md` e o equivalente do `README.en.md`. Faça o commit na branch `claude/fluxa-open-source-launch`.

### 2. Integrar ao `main` e enviar

O `main` remoto (`a361913`) é ancestral da branch, então a integração é um avanço direto, sem reescrever histórico:

```powershell
cd F:\fluxa-app-worktrees\claude-launch
git fetch origin
git merge-base --is-ancestor origin/main HEAD; if ($?) { "ok: avanço direto" }
git push origin HEAD:main
```

Se o `git merge-base` não imprimir "ok", **pare**: o `main` mudou, e o caminho certo é um pull request.

### 3. Configurações

Aplique as seções acima: About, Social preview, Features, Segurança, Labels e proteção do `main`. Ative a proteção só **depois** do push do passo 2.

### 4. Tag e release

Pela interface: **Releases → Draft a new release**.
- Tag: `v1.0.2`, no commit enviado.
- Título: `Fluxa 1.0.2`.
- Descrição: o conteúdo de `docs/releases/1.0.2.md` a partir de `# Fluxa 1.0.2`.
- Assets: arrastar os três arquivos de `android/artifacts/release/assets-da-release/`.
- Marcar **Set as the latest release** → **Publish release**.

Ou, com o GitHub CLI autenticado:

```powershell
cd F:\fluxa-app-worktrees\claude-launch
git tag -a v1.0.2 -m "Fluxa 1.0.2"
git push origin v1.0.2
gh release create v1.0.2 --title "Fluxa 1.0.2" --notes-file docs/releases/1.0.2.md --latest `
  android/artifacts/release/assets-da-release/Fluxa-1.0.2.apk `
  android/artifacts/release/assets-da-release/Fluxa-1.0.2.apk.sha256 `
  android/artifacts/release/assets-da-release/THIRD_PARTY_NOTICES.md
```

O `--notes-file` publica também o comentário HTML do topo. Ele fica invisível na página, mas, se preferir, remova-o antes.

### 5. Conferência depois de publicar

1. Baixe o APK pela página da release, em outra máquina ou no celular.
2. Confira o SHA-256: `6729bdbfc4bcff5c88838e31621fad416b9d6643520d8fce4772f1f5d70fcec8`.
3. Abra `https://github.com/matssgit/fluxa-app/releases/latest` e os links de `marketing/launch/LINKS.md`.
