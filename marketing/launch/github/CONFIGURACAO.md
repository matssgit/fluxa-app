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
