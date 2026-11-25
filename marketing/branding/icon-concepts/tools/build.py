"""Gera todos os arquivos das quatro propostas. Uso: python tools/build.py (Chrome instalado; sem dependências Python)."""
import html
import io
import json
import os
import subprocess
import sys
import tempfile
from math import copysign, cos, sin, pi
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).parent))
import symbols as S

OUT = Path(__file__).resolve().parent.parent
CHROME = r"C:/Program Files/Google/Chrome/Application/chrome.exe"

# Cores reais da v1.0.1 (FluxaColors.kt). Nada fora desta lista.
PETROL, PETROL_DEEP, PETROL_ACTION = "#14414D", "#123A45", "#2F6E80"
CREAM, PAPER, SURFACE, SUNKEN = "#FFFDF7", "#F6F2EA", "#FFFDF9", "#F0EBE1"
SAGE, TERRA = "#E6EAE2", "#B9684F"
INK, INK2, INK3 = "#1E2A2E", "#505A5C", "#5F6667"
NIGHT, NIGHT_CARD, MIST = "#0E171B", "#162227", "#A3C4CB"

META = {
    "a": dict(
        slug="monograma-fluido", name="Monograma fluido", letter="A", radius=42,
        line="Duas correntes sobem juntas e se abrem em F.",
        text="Entradas e saídas nascem da mesma haste e se abrem em dois braços ondulados. A fenda entre elas é o espaço "
             "negativo que dá ritmo ao F. Lê-se como letra de longe e como movimento de perto.",
        strong="É a inicial do nome e já conta a história do produto. Silhueta própria, com ponta orgânica.",
        risk="A fenda é o detalhe mais fino: some abaixo de 20 px e o F continua legível sem ela.",
        scores=(8, 9, 8, 9, 9, 9, 9),
    ),
    "b": dict(
        slug="fluxo-abstrato", name="Fluxo abstrato", letter="B", radius=36.5,
        line="Duas correntes viram uma; a folha no meio é o que fica.",
        text="Um disco atravessado por duas correntes que entram separadas e se encontram em uma só, mais larga. "
             "Entre elas sobra uma folha que se afina: o saldo. Não depende de letra nem de idioma.",
        strong="Massa cheia e redonda: ocupa bem qualquer máscara e funciona como avatar.",
        risk="Disco com ondas é um território comum (água, energia, saúde); sozinho, não diz “Fluxa”.",
        scores=(6, 8, 8, 7, 9, 8, 7),
    ),
    "c": dict(
        slug="monograma-geometrico", name="Monograma geométrico", letter="C", radius=42,
        line="Um módulo, uma junta, um raio. O F surge do encaixe.",
        text="Três peças com a mesma largura e o mesmo quarto de círculo, separadas por uma junta constante. Cada peça é "
             "a “folha” dos cartões do app lida como arquitetura. O F aparece pelo encaixe, não pelo contorno.",
        strong="A construção mais precisa: nítida em qualquer tamanho e simples de reproduzir.",
        risk="É a mais racional das quatro; fala menos de fluxo e mais de ordem.",
        scores=(8, 9, 8, 8, 10, 9, 9),
    ),
    "d": dict(
        slug="simbolo-organico", name="Símbolo orgânico", letter="D", radius=39.5,
        line="A folha dos cartões, em camadas que crescem de um canto.",
        text="A forma assimétrica dos cartões de destaque do Fluxa, repetida em camadas a partir do mesmo canto: "
             "reserva, saldo e horizonte. É a assinatura visual do aplicativo condensada em um sinal.",
        strong="A maior ligação com a interface: quem vê o ícone reconhece o cartão de saldo.",
        risk="Abstrata: precisa do nome ao lado no começo. O contorno lembra molduras quando muito pequeno.",
        scores=(7, 7, 9, 10, 8, 8, 8),
    ),
}
CRITERIA = ["Originalidade", "Tamanhos pequenos", "Equilíbrio visual", "Fidelidade ao Fluxa", "Fundos claros e escuros", "Legibilidade no launcher", "Potencial de marca"]


def symbol_paths(k):
    return S.fitted(S.RAW[k](), META[k]["radius"])


def paths_svg(k):
    return "".join(f'<path fill-rule="evenodd" d="{d}"/>' for d in symbol_paths(k))


# Letreiro próprio, desenhado em contornos (não depende de fonte instalada). Altura de caixa alta 70.
def wordmark():
    F = "M0 -70H43V-57.5H13.5V-41.5H38V-29.5H13.5V0H0Z"
    l = "M50 -72H63V0H50Z"
    u = "M72 -50H85V-20.5A9.2 9.2 0 0 0 103.400 -20.5V-50H116.400V0H104.200V-5.2C100.600 -0.900 96 1.200 90.600 1.200C79 1.200 72 -6.800 72 -19Z"
    x = "M121 -50H136.200L145 -36.6L153.800 -50H168.600L152.300 -25.500L169.600 0H154.400L144.600 -14.800L134.800 0H120L137.300 -25.300Z"
    a = ("M212.500 -50H225.500V0H212.500ZM198.200 -51.200C209.500 -51.200 218 -40.500 218 -25C218 -9.500 209.500 1.200 198.200 1.200C184.500 1.200 174 -10.300 174 -25C174 -39.700 184.500 -51.200 198.200 -51.200Z"
         "M199.800 -39.200C192.300 -39.200 187.200 -33.200 187.200 -25C187.200 -16.800 192.300 -10.800 199.800 -10.800C207.200 -10.800 212.500 -16.800 212.500 -25C212.500 -33.200 207.200 -39.200 199.800 -39.200Z")
    # a haste e o bojo do "a" são peças separadas para a contraforma não ser recortada na sobreposição
    stem, bowl = a[: a.index("ZM") + 1], a[a.index("ZM") + 1:]
    return [F, l, u, x, stem, bowl]


def wordmark_svg():
    return "".join(f'<path fill-rule="evenodd" d="{d}"/>' for d in wordmark())


WORD_W = 225.5


def svg_symbol(k, color):
    m = META[k]
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="-50 -50 100 100" width="512" height="512" role="img" aria-label="Fluxa, {m["name"].lower()}">'
            f'<title>Fluxa · Proposta {m["letter"]} · {m["name"]}</title><g id="simbolo" fill="{color}">{paths_svg(k)}</g></svg>\n')


def svg_icon(k, bg, fg):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="-50 -50 100 100" width="1024" height="1024">'
            f'<rect id="fundo" x="-50" y="-50" width="100" height="100" fill="{bg}"/><g id="simbolo" fill="{fg}">{paths_svg(k)}</g></svg>\n')


def svg_adaptive(k, fg):
    """Camada de 108 dp: 100 u do ícone visível (72 dp) ficam no centro."""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="-75 -75 150 150" width="1080" height="1080"><g id="foreground" fill="{fg}">{paths_svg(k)}</g></svg>\n')


def logo_box(k):
    """Símbolo com altura óptica 84, centrado na caixa alta (70); respiro fixo até o letreiro."""
    pts = [p for d in symbol_paths(k) for p in S.sample(d)]
    x0, x1 = min(p[0] for p in pts), max(p[0] for p in pts)
    y0, y1 = min(p[1] for p in pts), max(p[1] for p in pts)
    sc = 84 / (y1 - y0)
    word_x = 10 + (x1 - x0) * sc + 24
    return dict(s=sc, x=10 - x0 * sc, y=52 - (y0 + y1) / 2 * sc, word=word_x, w=word_x + WORD_W + 10)


def logo_inner(k, fg, word=None):
    b = logo_box(k)
    return (f'<g id="simbolo" fill="{fg}" transform="translate({b["x"]:.3f} {b["y"]:.3f}) scale({b["s"]:.4f})">{paths_svg(k)}</g>'
            f'<g id="letreiro" fill="{word or fg}" transform="translate({b["word"]:.3f} 87)">{wordmark_svg()}</g>')


def svg_logo(k, fg, word=None):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {logo_box(k)['w']:.2f} 104" width="{logo_box(k)['w'] * 4:.0f}" height="416"><title>Fluxa</title>{logo_inner(k, fg, word)}</svg>\n'


# Máscaras (quadro de 100 u) ------------------------------------------------------------------------------------
def superellipse(n, r=50, steps=160):
    p = []
    for i in range(steps):
        t = 2 * pi * i / steps
        c, s = cos(t), sin(t)
        p.append(f"{copysign(abs(c) ** (2 / n), c) * r:.2f} {copysign(abs(s) ** (2 / n), s) * r:.2f}")
    return "M" + "L".join(p) + "Z"


MASKS = {
    "circulo": ("Circular", '<circle r="50"/>'),
    "arredondada": ("Arredondada", '<rect x="-50" y="-50" width="100" height="100" rx="24"/>'),
    "superelipse": ("Superelipse (One UI)", f'<path d="{superellipse(4.6)}"/>'),
}


def masked(k, mask, bg, fg, size, uid, guide=False):
    g = ('<circle r="45.833" fill="none" stroke="#B9684F" stroke-width=".7" stroke-dasharray="2 1.6"/>' if guide else "")
    return (f'<svg width="{size}" height="{size}" viewBox="-50 -50 100 100"><defs><clipPath id="{uid}">{MASKS[mask][1]}</clipPath></defs>'
            f'<g clip-path="url(#{uid})"><rect x="-50" y="-50" width="100" height="100" fill="{bg}"/><g fill="{fg}">{paths_svg(k)}</g></g>{g}</svg>')


_uid = 0


def uid():
    global _uid
    _uid += 1
    return f"m{_uid}"


def icon(k, size, bg=PETROL, fg=CREAM, mask="superelipse", guide=False):
    return masked(k, mask, bg, fg, size, uid(), guide)


# Launcher -------------------------------------------------------------------------------------------------------
GLYPHS = {
    "Telefone": '<path d="M-14 -18C-18 -14 -17 -4 -8 6C2 17 13 20 18 15L12 6L5 8C0 5 -5 0 -7 -5L-5 -12Z"/>',
    "Mensagens": '<path d="M-19 -15H19V10H-4L-13 19V10H-19Z"/>',
    "Câmera": '<path fill-rule="evenodd" d="M-21 -11H-11L-7 -17H7L11 -11H21V16H-21ZM0 -7A9 9 0 1 0 0 11A9 9 0 1 0 0 -7Z"/>',
    "Galeria": '<path d="M-19 15L-6 -6L3 6L8 0L19 15Z"/><circle cx="10" cy="-11" r="4.500"/>',
    "Agenda": '<path fill-rule="evenodd" d="M-18 -15H18V18H-18ZM-13 -5V13H13V-5Z"/>',
    "Notas": '<path d="M-14 -19H6L14 -11V19H-14Z"/>',
    "Relógio": '<path fill-rule="evenodd" d="M0 -20A20 20 0 1 0 0 20A20 20 0 1 0 0 -20ZM-2 -13H3V-1L11 4L8 8L-2 2Z"/>',
    "Mapas": '<path fill-rule="evenodd" d="M0 -20C10 -20 15 -13 15 -6C15 4 4 14 0 20C-4 14 -15 4 -15 -6C-15 -13 -10 -20 0 -20ZM0 -11A5.500 5.500 0 1 0 0 0A5.500 5.500 0 1 0 0 -11Z"/>',
    "Música": '<path d="M-8 -16L16 -20V10A6 6 0 1 1 10 4V-10L-3 -8V14A6 6 0 1 1 -8 8Z"/>',
    "Arquivos": '<path d="M-19 -14H-5L0 -8H19V16H-19Z"/>',
    "Clima": '<circle r="11"/><path d="M-2 -21H2V-15H-2ZM-2 15H2V21H-2ZM-21 -2H-15V2H-21ZM15 -2H21V2H15Z"/>',
    "Contatos": '<circle cy="-8" r="9"/><path d="M-17 18C-17 5 17 5 17 18Z"/>',
}


def generic(name, size, dark):
    bg, fg = ("#E9E4DA", "#6E7577") if not dark else ("#2A3338", "#AEB6B8")
    return (f'<svg width="{size}" height="{size}" viewBox="-50 -50 100 100"><path d="{superellipse(4.6)}" fill="{bg}"/><g fill="{fg}">{GLYPHS[name]}</g></svg>')


def launcher(k, bg, fg, dark, w=360, h=780):
    ink = "#FFFFFF" if dark else INK
    wall = (f"radial-gradient(120% 70% at 80% 8%, #22424B 0%, #16252B 45%, {NIGHT} 100%)" if dark
            else f"radial-gradient(120% 70% at 20% 5%, {CREAM} 0%, {PAPER} 45%, #E3DED3 100%)")
    size = 60
    names = ["Câmera", "Galeria", "Agenda", "Relógio", "Notas", "FLUXA", "Mapas", "Música", "Arquivos", "Clima"]
    cells = ""
    for n in names:
        ic = icon(k, size, bg, fg) if n == "FLUXA" else generic(n, size, dark)
        cells += f'<div class="cell">{ic}<span>{"Fluxa" if n == "FLUXA" else n}</span></div>'
    dock = "".join(f'<div class="cell">{generic(n, size, dark)}</div>' for n in ("Telefone", "Mensagens", "Contatos"))
    dock += f'<div class="cell">{icon(k, size, bg, fg)}</div>'
    return (f'<div class="phone" style="width:{w}px;height:{h}px;background:{wall};color:{ink}">'
            f'<div class="bar"><b>9:41</b><i></i><span>▮▮▮ &nbsp;84%</span></div>'
            f'<div class="clock"><strong>9:41</strong><em>ter., 24 de novembro</em></div>'
            f'<div class="grid">{cells}</div><div class="dock" style="background:{"rgba(255,255,255,.07)" if dark else "rgba(255,255,255,.45)"}">{dock}</div><div class="home" style="background:{ink}"></div></div>')


CSS = f"""
*{{box-sizing:border-box;margin:0}} body{{font-family:"Segoe UI",Roboto,system-ui,sans-serif;background:{PAPER};color:{INK};-webkit-font-smoothing:antialiased}}
.phone{{position:relative;border-radius:44px;overflow:hidden;border:7px solid #11181b;box-shadow:0 22px 50px -28px rgba(42,33,24,.55);flex:none}}
.bar{{display:flex;justify-content:space-between;align-items:center;padding:14px 24px 0;font-size:12px}} .bar i{{width:11px;height:11px;border-radius:50%;background:#0b0f11}}
.clock{{text-align:center;padding:64px 0 60px}} .clock strong{{display:block;font-size:66px;font-weight:300;letter-spacing:-2px;line-height:1}} .clock em{{font-style:normal;font-size:14px;opacity:.75}}
.grid{{display:grid;grid-template-columns:repeat(4,1fr);gap:26px 0;padding:0 14px}} .cell{{display:flex;flex-direction:column;align-items:center;gap:7px;font-size:12px;font-weight:500}}
.dock{{position:absolute;left:16px;right:16px;bottom:30px;border-radius:30px;padding:14px 4px;display:grid;grid-template-columns:repeat(4,1fr)}} .home{{position:absolute;left:50%;bottom:9px;width:108px;height:4px;margin-left:-54px;border-radius:9px;opacity:.75}}
"""


def shot(body, css, out, w, h, scale=1, transparent=False):
    doc = f'<!doctype html><meta charset="utf-8"><style>{css}</style><body>{body}</body>'
    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as t:
        p = Path(t) / "p.html"
        p.write_text(doc, encoding="utf-8")
        cmd = [CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--user-data-dir={t}/prof", f"--force-device-scale-factor={scale}",
               f"--window-size={w},{h}", f"--screenshot={out}"]
        if transparent:
            cmd.append("--default-background-color=00000000")
        cmd.append(p.as_uri())
        subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def svg_to_png(svg, out, w, h, transparent=False):
    body = svg.replace("<svg ", f'<svg style="display:block;width:{w}px;height:{h}px" ', 1)
    shot(body, "*{margin:0}html,body{background:transparent;overflow:hidden}", out, w, h, 1, transparent)


# Prancha --------------------------------------------------------------------------------------------------------
def board_row(k):
    m = META[k]
    total = sum(m["scores"])
    small = "".join(f'<div class="sz">{icon(k, s)}<span>{s}</span></div>' for s in (64, 48, 32, 24, 16))
    masks = "".join(f'<div class="sz">{icon(k, 74, mask=x, guide=True)}<span>{MASKS[x][0]}</span></div>' for x in MASKS)
    sym = lambda size, fg: f'<svg width="{size}" height="{size}" viewBox="-50 -50 100 100" fill="{fg}">{paths_svg(k)}</svg>'
    logo = lambda fg, w: f'<svg width="{w}" viewBox="0 0 {logo_box(k)["w"]:.2f} 104">{logo_inner(k, fg)}</svg>'
    return f"""
<section>
 <header><b>{m['letter']}</b><div><h2>{m['name']}</h2><p>{m['line']}</p></div></header>
 <div class="row">
  <div class="hero">{sym(384, PETROL)}</div>
  <div class="col">
    <div class="pair"><div>{icon(k, 168)}<label>Petróleo</label></div><div>{icon(k, 168, SURFACE, PETROL)}<label>Creme</label></div></div>
    <div class="pair mono"><div class="w">{sym(104, '#000')}<label>Mono preto</label></div><div class="k">{sym(104, '#fff')}<label>Mono branco</label></div></div>
  </div>
  <div class="col logos"><div class="lg">{logo(PETROL, 360)}</div><div class="lg dk">{logo(CREAM, 360)}</div></div>
  <div class="col tests"><h4>Tamanhos (px)</h4><div class="line">{small}</div><h4>Máscaras e safe zone de 66 dp</h4><div class="line">{masks}</div>
     <p class="txt">{m['text']}</p><p class="txt"><b>Força.</b> {m['strong']}</p><p class="txt"><b>Atenção.</b> {m['risk']}</p></div>
  <div class="ph">{launcher(k, PETROL, CREAM, False)}</div>
 </div>
</section>"""


BOARD_CSS = CSS + f"""
body{{width:2400px;padding:80px 80px 60px}} .top{{display:flex;justify-content:space-between;align-items:flex-end;padding-bottom:36px;border-bottom:1px solid rgba(58,47,36,.18)}}
h1{{font-size:54px;letter-spacing:-1.5px;font-weight:700;color:{PETROL}}} .top p{{font-size:21px;color:{INK2};margin-top:8px}} .sw{{display:flex;gap:14px}} .sw div{{font-size:14px;color:{INK2}}} .sw i{{display:block;width:104px;height:56px;border-radius:14px;margin-bottom:6px;border:1px solid rgba(58,47,36,.14)}}
section{{padding:44px 0 46px;border-bottom:1px solid rgba(58,47,36,.18)}} header{{display:flex;gap:20px;align-items:center;margin-bottom:24px}} header>b{{width:56px;height:56px;border-radius:24px 6px 24px 6px;background:{PETROL};color:{CREAM};display:grid;place-items:center;font-size:26px}} h2{{font-size:32px;letter-spacing:-.6px}} header p{{font-size:18px;color:{INK2}}}
.row{{display:flex;gap:28px;align-items:stretch;height:720px}} .hero{{width:400px;background:{SURFACE};border-radius:40px 10px 40px 10px;display:grid;place-items:center;border:1px solid rgba(58,47,36,.1)}}
.col{{display:flex;flex-direction:column;gap:24px}} .pair{{display:flex;gap:20px;flex:1}} .pair>div{{width:212px;background:{SUNKEN};border-radius:28px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px}} .pair label{{font-size:14px;color:{INK3}}} .mono .w{{background:#fff}} .mono .k{{background:#000}} .mono .k label{{color:#bbb}}
.logos{{width:470px}} .lg{{flex:1;background:{SURFACE};border-radius:28px;display:grid;place-items:center;border:1px solid rgba(58,47,36,.1)}} .lg.dk{{background:{NIGHT}}}
.tests{{flex:1;background:{SURFACE};border-radius:28px;padding:30px;gap:14px;border:1px solid rgba(58,47,36,.1)}} .tests h4{{font-size:15px;color:{INK3};font-weight:600}} .line{{display:flex;gap:22px;align-items:flex-end;margin-bottom:8px}} .sz{{display:flex;flex-direction:column;align-items:center;gap:8px;font-size:12px;color:{INK3};text-align:center}} .txt{{font-size:17px;line-height:1.45;color:{INK2}}} .txt b{{color:{PETROL}}}
.ph{{width:340px;display:flex;justify-content:center}} .ph .phone{{transform:scale(.905);transform-origin:top center}}
table{{width:100%;border-collapse:collapse;font-size:18px;margin-top:20px}} td,th{{padding:13px 16px;text-align:center;border-bottom:1px solid rgba(58,47,36,.12)}} td:first-child,th:first-child{{text-align:left}} th{{color:{INK3};font-weight:600;font-size:15px}} tr.tot td{{font-weight:700;color:{PETROL};font-size:21px;border:0}}
.foot{{margin-top:28px;font-size:16px;color:{INK3}}}
"""


def score_table():
    h = "<table><tr><th>Critério (0 a 10, avaliação de quem desenhou)</th>" + "".join(f"<th>{META[k]['letter']} · {META[k]['name']}</th>" for k in META) + "</tr>"
    for i, c in enumerate(CRITERIA):
        h += f"<tr><td>{c}</td>" + "".join(f"<td>{META[k]['scores'][i]}</td>" for k in META) + "</tr>"
    return h + '<tr class="tot"><td>Total</td>' + "".join(f"<td>{sum(META[k]['scores'])}/70</td>" for k in META) + "</tr></table>"


def board():
    sw = "".join(f'<div><i style="background:{c}"></i>{n}<br>{c}</div>' for c, n in ((PETROL, "Petróleo"), (CREAM, "Creme"), (PAPER, "Fundo claro"), (SAGE, "Sálvia"), (NIGHT, "Fundo escuro"), (TERRA, "Terracota")))
    return (f'<div class="top"><div><h1>Fluxa — quatro propostas de marca</h1><p>Cores reais da versão 1.0.1. Todos os símbolos são vetoriais e respeitam a safe zone de adaptive icons.</p></div><div class="sw">{sw}</div></div>'
            + "".join(board_row(k) for k in META) + f'<section style="border:0;padding-bottom:0"><header><div><h2>Avaliação comparada</h2></div></header>{score_table()}<p class="foot">Sem pesquisa de marca registrada: antes de divulgar como marca, verificar disponibilidade.</p></section>')


# Página interativa ----------------------------------------------------------------------------------------------
def page():
    data = {k: dict(letter=m["letter"], name=m["name"], line=m["line"], text=m["text"], strong=m["strong"], risk=m["risk"], scores=m["scores"],
                    paths=symbol_paths(k), radius=m["radius"], logo=logo_box(k)) for k, m in META.items()}
    glyphs = {n: g for n, g in GLYPHS.items()}
    masks = {k: v[1] for k, v in MASKS.items()}
    css = CSS + f"""
:root{{--bg:{PAPER};--card:{SURFACE};--ink:{INK};--ink2:{INK2};--line:rgba(58,47,36,.14);--brand:{PETROL};--on:{CREAM}}}
:root.dark{{--bg:{NIGHT};--card:{NIGHT_CARD};--ink:#F5F0E6;--ink2:#C7CBCB;--line:rgba(240,233,220,.14);--brand:{MIST};--on:{NIGHT}}}
body{{background:var(--bg);color:var(--ink);padding:28px clamp(16px,4vw,56px) 60px;transition:background .2s}} main{{max-width:1240px;margin:0 auto}}
h1{{font-size:clamp(26px,4vw,40px);letter-spacing:-.8px}} .sub{{color:var(--ink2);margin:6px 0 22px}}
.ctl{{position:sticky;top:0;z-index:5;background:var(--bg);display:flex;flex-wrap:wrap;gap:18px;padding:14px 0;border-bottom:1px solid var(--line);margin-bottom:26px}}
fieldset{{border:0;padding:0;display:flex;gap:6px;align-items:center;flex-wrap:wrap}} legend{{font-size:12px;color:var(--ink2);float:left;margin-right:8px}}
button{{font:inherit;font-size:14px;padding:9px 15px;border-radius:99px;border:1px solid var(--line);background:var(--card);color:var(--ink);cursor:pointer}} button[aria-pressed=true]{{background:var(--brand);color:var(--on);border-color:transparent}} button:focus-visible{{outline:3px solid {TERRA};outline-offset:2px}}
.cards{{display:grid;grid-template-columns:repeat(auto-fit,minmax(250px,1fr));gap:18px}} .card{{background:var(--card);border:1px solid var(--line);border-radius:32px 8px 32px 8px;padding:22px;text-align:left;width:100%;display:block}} .card[aria-pressed=true]{{background:var(--card);color:var(--ink);border:2px solid var(--brand)}}
.card .v{{display:grid;place-items:center;height:190px}} .card h3{{font-size:18px;margin-top:8px}} .card p{{font-size:14px;color:var(--ink2);margin-top:4px;white-space:normal}}
.detail{{display:grid;grid-template-columns:minmax(0,1fr) 374px;gap:34px;margin-top:38px;align-items:start}} @media(max-width:900px){{.detail{{grid-template-columns:1fr}} .ph{{display:flex;justify-content:center}}}}
.panel{{background:var(--card);border:1px solid var(--line);border-radius:24px;padding:24px;margin-bottom:18px}} .panel h4{{font-size:13px;color:var(--ink2);font-weight:600;margin-bottom:14px}} .flex{{display:flex;flex-wrap:wrap;gap:22px;align-items:flex-end}} .flex figure{{display:flex;flex-direction:column;gap:8px;align-items:center;font-size:12px;color:var(--ink2)}}
.panel p{{line-height:1.55;color:var(--ink2);margin-bottom:8px}} .bars{{display:grid;grid-template-columns:auto 1fr auto;gap:8px 14px;align-items:center;font-size:14px}} .bars i{{display:block;height:8px;border-radius:9px;background:var(--brand)}} .bars span{{background:var(--line);border-radius:9px;display:block}}
.lg{{padding:26px;border-radius:18px;flex:1 1 260px;display:grid;place-items:center}} @media(max-width:420px){{.phone{{zoom:.88}}}}
"""
    js = """
const D=__DATA__,G=__GLYPHS__,M=__MASKS__,SQ=__SQ__,WORD=__WORD__,CRIT=__CRIT__,C=__COLORS__;
const st={k:'a',mask:'superelipse',icon:'petrol',theme:'light',guide:false};let n=0;
const P=k=>D[k].paths.map(d=>`<path fill-rule="evenodd" d="${d}"/>`).join('');
const pal=()=>({petrol:[C.petrol,C.cream],cream:[C.surface,C.petrol],mono:['#000','#fff']})[st.icon];
function ic(k,s,p=pal(),mask=st.mask,g=st.guide){const id='i'+(n++);return `<svg width="${s}" height="${s}" viewBox="-50 -50 100 100" aria-hidden="true"><defs><clipPath id="${id}">${M[mask]}</clipPath></defs><g clip-path="url(#${id})"><rect x="-50" y="-50" width="100" height="100" fill="${p[0]}"/><g fill="${p[1]}">${P(k)}</g></g>${g?'<circle r="45.833" fill="none" stroke="#B9684F" stroke-width=".7" stroke-dasharray="2 1.6"/>':''}</svg>`}
function gen(name,s,dark){const [b,f]=dark?['#2A3338','#AEB6B8']:['#E9E4DA','#6E7577'];return `<svg width="${s}" height="${s}" viewBox="-50 -50 100 100" aria-hidden="true"><path d="${SQ}" fill="${b}"/><g fill="${f}">${G[name]}</g></svg>`}
function phone(k){const dark=st.theme==='dark',ink=dark?'#fff':C.ink,wall=dark?`radial-gradient(120% 70% at 80% 8%,#22424B 0%,#16252B 45%,${C.night} 100%)`:`radial-gradient(120% 70% at 20% 5%,${C.cream} 0%,${C.paper} 45%,#E3DED3 100%)`;
const cell=(a,l)=>`<div class="cell">${a}${l?`<span>${l}</span>`:''}</div>`;const names=['Câmera','Galeria','Agenda','Relógio','Notas','F','Mapas','Música','Arquivos','Clima'];
return `<div class="phone" style="width:360px;height:780px;background:${wall};color:${ink}"><div class="bar"><b>9:41</b><i></i><span>84%</span></div><div class="clock"><strong>9:41</strong><em>ter., 24 de novembro</em></div><div class="grid">${names.map(x=>x==='F'?cell(ic(k,60,pal(),st.mask,false),'Fluxa'):cell(gen(x,60,dark),x)).join('')}</div><div class="dock" style="background:${dark?'rgba(255,255,255,.07)':'rgba(255,255,255,.45)'}">${['Telefone','Mensagens','Contatos'].map(x=>cell(gen(x,60,dark))).join('')}${cell(ic(k,60,pal(),st.mask,false))}</div><div class="home" style="background:${ink}"></div></div>`}
function logo(k,fg,w=300){const b=D[k].logo;return `<svg width="${w}" style="max-width:100%" viewBox="0 0 ${b.w} 104" role="img" aria-label="Fluxa"><g fill="${fg}" transform="translate(${b.x} ${b.y}) scale(${b.s})">${P(k)}</g><g fill="${fg}" transform="translate(${b.word} 87)">${WORD}</g></svg>`}
function draw(){document.documentElement.classList.toggle('dark',st.theme==='dark');
document.querySelectorAll('[data-set]').forEach(b=>{const [a,v]=b.dataset.set.split(':');b.setAttribute('aria-pressed',String(st[a])===v)});
document.getElementById('cards').innerHTML=Object.keys(D).map(k=>`<button class="card" aria-pressed="${st.k===k}" data-k="${k}"><div class="v">${ic(k,150)}</div><h3>${D[k].letter} · ${D[k].name}</h3><p>${D[k].line}</p></button>`).join('');
document.querySelectorAll('[data-k]').forEach(b=>b.onclick=()=>{st.k=b.dataset.k;draw();document.querySelector(`[data-k="${st.k}"]`).focus()});
const d=D[st.k],k=st.k,tot=d.scores.reduce((a,b)=>a+b);
document.getElementById('left').innerHTML=`<div class="panel"><h4>Logotipo</h4><div class="flex"><div class="lg" style="background:${C.surface}">${logo(k,C.petrol)}</div><div class="lg" style="background:${C.night}">${logo(k,C.cream)}</div></div></div>
<div class="panel"><h4>Tamanhos reduzidos (px)</h4><div class="flex">${[96,64,48,32,24,16].map(s=>`<figure>${ic(k,s)}<figcaption>${s}</figcaption></figure>`).join('')}</div></div>
<div class="panel"><h4>Máscaras e safe zone (66 dp)</h4><div class="flex">${Object.keys(M).map(m=>`<figure>${ic(k,104,pal(),m,true)}<figcaption>${({circulo:'Circular',arredondada:'Arredondada',superelipse:'Superelipse'})[m]}</figcaption></figure>`).join('')}</div></div>
<div class="panel"><h4>Monocromático</h4><div class="flex">${[['#fff','#000'],['#000','#fff'],[C.sage,C.petrol]].map(p=>`<figure>${ic(k,104,p)}</figure>`).join('')}</div></div>
<div class="panel"><h4>Conceito</h4><p>${d.text}</p><p><b>Força.</b> ${d.strong}</p><p><b>Atenção.</b> ${d.risk}</p></div>
<div class="panel"><h4>Avaliação de quem desenhou: ${tot}/70</h4><div class="bars">${CRIT.map((c,i)=>`<div>${c}</div><span><i style="width:${d.scores[i]*10}%"></i></span><b>${d.scores[i]}</b>`).join('')}</div></div>`;
document.getElementById('phone').innerHTML=phone(k)}
document.querySelectorAll('[data-set]').forEach(b=>b.onclick=()=>{const [a,v]=b.dataset.set.split(':');st[a]=v==='true'?true:v==='false'?false:v;draw()});draw();
"""
    rep = {"__DATA__": data, "__GLYPHS__": glyphs, "__MASKS__": masks, "__SQ__": superellipse(4.6), "__WORD__": wordmark_svg(), "__CRIT__": CRITERIA,
           "__COLORS__": dict(petrol=PETROL, cream=CREAM, surface=SURFACE, paper=PAPER, night=NIGHT, sage=SAGE, ink=INK)}
    for a, b in rep.items():
        js = js.replace(a, json.dumps(b, ensure_ascii=False))
    grp = lambda t, a, opts: f'<fieldset><legend>{t}</legend>' + "".join(f'<button data-set="{a}:{v}">{l}</button>' for v, l in opts) + "</fieldset>"
    ctl = (grp("Ícone", "icon", [("petrol", "Petróleo"), ("cream", "Creme"), ("mono", "Mono")]) + grp("Máscara", "mask", [("superelipse", "Superelipse"), ("circulo", "Círculo"), ("arredondada", "Arredondada")])
           + grp("Tema", "theme", [("light", "Claro"), ("dark", "Escuro")]) + grp("Safe zone", "guide", [("false", "Ocultar"), ("true", "Mostrar")]))
    return (f'<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Fluxa · Propostas de marca</title><style>{css}</style></head><body><main>'
            f'<h1>Fluxa · propostas de marca</h1><p class="sub">Escolha uma proposta e troque fundo, máscara e tema. Cores reais da versão 1.0.1.</p><div class="ctl">{ctl}</div><div class="cards" id="cards"></div>'
            f'<div class="detail"><div id="left"></div><div class="ph" id="phone"></div></div></main><script>{js}</script></body></html>')


def write(rel, text):
    p = OUT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")


def main():
    for k, m in META.items():
        base = f"{k}-{m['slug']}"
        d = f"{base}/"
        write(d + "svg/simbolo.svg", svg_symbol(k, PETROL))
        write(d + "svg/simbolo-creme.svg", svg_symbol(k, CREAM))
        write(d + "svg/simbolo-mono-preto.svg", svg_symbol(k, "#000000"))
        write(d + "svg/simbolo-mono-branco.svg", svg_symbol(k, "#FFFFFF"))
        write(d + "svg/icone-petroleo.svg", svg_icon(k, PETROL, CREAM))
        write(d + "svg/icone-creme.svg", svg_icon(k, SURFACE, PETROL))
        write(d + "svg/adaptive-foreground-108dp.svg", svg_adaptive(k, CREAM))
        write(d + "svg/logotipo-petroleo.svg", svg_logo(k, PETROL))
        write(d + "svg/logotipo-creme.svg", svg_logo(k, CREAM))
        (OUT / base / "png").mkdir(parents=True, exist_ok=True)
        png = lambda n: str(OUT / base / "png" / n)
        svg_to_png(svg_icon(k, PETROL, CREAM), png("icone-petroleo-1024.png"), 1024, 1024)
        svg_to_png(svg_icon(k, SURFACE, PETROL), png("icone-creme-1024.png"), 1024, 1024)
        svg_to_png(svg_icon(k, "#FFFFFF", "#000000"), png("icone-mono-preto-1024.png"), 1024, 1024)
        svg_to_png(svg_icon(k, "#000000", "#FFFFFF"), png("icone-mono-branco-1024.png"), 1024, 1024)
        svg_to_png(svg_symbol(k, PETROL), png("simbolo-transparente-1024.png"), 1024, 1024, True)
        svg_to_png(svg_logo(k, PETROL), png("logotipo-petroleo.png"), round(logo_box(k)["w"] * 4), 416, True)
        svg_to_png(svg_logo(k, CREAM), png("logotipo-creme.png"), round(logo_box(k)["w"] * 4), 416, True)
        wrap = CSS + "body{display:flex;gap:40px;padding:40px;background:#F6F2EA}"
        shot(launcher(k, PETROL, CREAM, False) + launcher(k, PETROL, CREAM, True) + launcher(k, SURFACE, PETROL, True), wrap, png("launcher-s23.png"), 1240, 860, 2)
        cells = "".join(f'<div>{icon(k, 320, bg, fg, mk, True)}<p>{MASKS[mk][0]}</p></div>' for bg, fg in ((PETROL, CREAM), (SURFACE, PETROL)) for mk in MASKS)
        shot(cells, "*{margin:0}body{font:18px 'Segoe UI',sans-serif;color:#505A5C;display:grid;grid-template-columns:repeat(3,320px);gap:30px 40px;padding:40px;background:#E6EAE2;text-align:center}p{padding-top:8px}", png("mascaras-safe-zone.png"), 1120, 800, 1)
    shot(board(), BOARD_CSS, str(OUT / "prancha-comparativa.png"), 2400, 4560, 2)
    write("index.html", page())


if __name__ == "__main__":
    main()
