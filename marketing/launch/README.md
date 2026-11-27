# Lançamento do Fluxa

Materiais de divulgação. Todas as telas são capturas reais do app, feitas em emulador com dados fictícios. Nenhum dado de pessoa real aparece aqui.

| Pasta | Conteúdo |
| --- | --- |
| `github/` | `banner-claro.png` e `banner-escuro.png` (topo do README) e `social-preview-1280x640.png` (Settings → Social preview do repositório) |
| `linkedin/` | `posts.md` (três versões do texto) e `carrossel/` (9 páginas 1080×1350 e `fluxa-carrossel.pdf`) |
| `portfolio/` | `case.md`, `capa-1600x1000.png` e `card-800x500.png` |
| `screenshots/` | 16 telas, nos temas claro e escuro, em 720 px de largura |
| `tools/build.py` | Gera tudo acima a partir das capturas e da marca |

A marca (símbolo, logotipo e ícones) fica em [`../branding/final`](../branding/final/README.md).

## Regenerar

```bash
python marketing/launch/tools/build.py                 # tudo
python marketing/launch/tools/build.py carrossel       # só o carrossel e o PDF
```

Requer o Google Chrome instalado (o script usa o modo headless para renderizar HTML/SVG em PNG e PDF). Tipografia: Sitka (títulos) e Segoe UI (texto), fontes do Windows usadas só para gerar as imagens.

`OPEN_SOURCE = False` no início do script controla a página 8 do carrossel. Mude para `True` só depois de a licença e a publicação do código serem aprovadas.

## Capturas

Geradas pelo teste instrumentado `RcVisualCapture` com `-e fluxaCapture <nome>`, em 360 dp, com a barra de status em *demo mode*. O conjunto de dados fictícios fica no próprio teste.
