"""Gera a marca final e os materiais. Uso: python tools/build.py (Chrome instalado; sem dependências Python)."""
import importlib.util
import sys
from pathlib import Path

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
CONCEPTS = HERE.parents[1] / "icon-concepts" / "tools"
sys.path.insert(0, str(CONCEPTS))
sys.path.insert(0, str(HERE))
import mark as M  # noqa: E402

_spec = importlib.util.spec_from_file_location("concepts", CONCEPTS / "build.py")
C = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(C)

OUT = HERE.parent
SCREENS = HERE.parents[3] / "android" / "docs" / "v1-0-1" / "screens"
PETROL, DEEP, CREAM, PAPER, SAGE, NIGHT, INK, INK2 = C.PETROL, C.PETROL_DEEP, C.CREAM, C.PAPER, C.SAGE, C.NIGHT, C.INK, C.INK2
ON_PETROL = "#D5E1E3"

# Símbolo em 100 u: o mesmo de tools/mark.py. No ícone, escala igual à do Android (27,5 dp de 36 dp visíveis).
PATHS = M.mark()
_pts = [p for d in PATHS for p in C.S.sample(d)]
X0, X1 = min(p[0] for p in _pts), max(p[0] for p in _pts)
Y0, Y1 = min(p[1] for p in _pts), max(p[1] for p in _pts)
ICON_K = (M.ANDROID_RADIUS_DP / 36 * 50) / max((x * x + y * y) ** 0.5 for x, y in _pts)
SVG = '<svg xmlns="http://www.w3.org/2000/svg" '


def g(fg, extra=""):
    return f'<g fill="{fg}"{extra}>' + "".join(f'<path d="{d}"/>' for d in PATHS) + "</g>"


def symbol(fg, pad=4):
    """Recorte justo ao símbolo, com margem fixa: para aplicar em qualquer fundo."""
    w, h = X1 - X0 + 2 * pad, Y1 - Y0 + 2 * pad
    return f'{SVG}viewBox="{X0 - pad:.3f} {Y0 - pad:.3f} {w:.3f} {h:.3f}"><title>Fluxa</title>{g(fg)}</svg>\n', w / h


def icon(bg, fg):
    return (f'{SVG}viewBox="-50 -50 100 100"><title>Fluxa</title><rect x="-50" y="-50" width="100" height="100" fill="{bg}"/>'
            + g(fg, f' transform="scale({ICON_K:.4f})"') + "</svg>\n")


def small(bg, fg):
    """Favicon: cantos arredondados e símbolo maior, porque não há máscara do sistema."""
    return (f'{SVG}viewBox="-50 -50 100 100"><title>Fluxa</title><rect x="-50" y="-50" width="100" height="100" rx="24" fill="{bg}"/>'
            + g(fg, ' transform="scale(.94)"') + "</svg>\n")


def logo(fg):
    sc = 84 / (Y1 - Y0)
    wx = 10 + (X1 - X0) * sc + 24
    w = wx + C.WORD_W + 10
    return (f'{SVG}viewBox="0 0 {w:.2f} 104"><title>Fluxa</title>'
            + g(fg, f' transform="translate({10 - X0 * sc:.3f} {52 - (Y0 + Y1) / 2 * sc:.3f}) scale({sc:.4f})"')
            + f'<g fill="{fg}" transform="translate({wx:.3f} 87)">{C.wordmark_svg()}</g></svg>\n'), w / 104


def write(rel, text):
    p = OUT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")


def png(rel, svg, w, h, transparent=False):
    p = OUT / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    C.svg_to_png(svg, p, w, h, transparent)


# Materiais ------------------------------------------------------------------------------------------------------
CSS = """*{box-sizing:border-box;margin:0}html,body{overflow:hidden}
body{font-family:"Segoe UI",Roboto,system-ui,sans-serif;-webkit-font-smoothing:antialiased}
.c{position:relative;overflow:hidden}.a{position:absolute}.logo{display:block}
h1{font-weight:600;letter-spacing:-.025em;line-height:1.08}p{line-height:1.45}
.ph{position:absolute;overflow:hidden;background:#0b1114;border:8px solid #0b1114}.ph img{display:block;width:100%}"""


def phone(name, x, y, w):
    """Captura real da 1.0.1 (dados sintéticos), sem a barra de status do emulador."""
    k, cut = w / 1080, 120
    h = (2340 - cut) * k
    bg = PAPER if name.startswith("claro") else NIGHT
    return (f'<div class="ph" style="left:{x}px;top:{y}px;width:{w + 16}px;height:{h + 16:.0f}px;border-radius:{w * .12:.0f}px">'
            f'<div style="overflow:hidden;border-radius:{w * .095:.0f}px;height:100%;background:{bg}">'
            f'<img src="{(SCREENS / (name + ".png")).as_uri()}" style="margin-top:-{cut * k:.1f}px"></div></div>')


def big(fg, size, x, y):
    s, ar = symbol(fg, 0)
    return s.replace("<svg ", f'<svg class="a" style="left:{x}px;top:{y}px;height:{size}px;width:{size * ar:.0f}px" ', 1)


def lg(fg, w):
    s, ar = logo(fg)
    return s.replace("<svg ", f'<svg class="logo" style="width:{w}px;height:{w / ar:.0f}px" ', 1)


LINE = "Seu dinheiro,<br>em fluxo claro."
SUB = "Contas, cartões, assinaturas e metas em um app Android que funciona sem internet."


def canvas(w, h, bg, fg, inner):
    return w, h, f'<div class="c" style="width:{w}px;height:{h}px;background:{bg};color:{fg}">{inner}</div>'


def at(x, y, w, inner):
    return f'<div class="a" style="left:{x}px;top:{y}px;width:{w}px">{inner}</div>'


def text(size, sub, color, subcolor, gap, sub_w):
    return (f'<h1 style="font-size:{size}px;color:{color}">{LINE}</h1>'
            f'<p style="font-size:{sub}px;margin-top:{gap}px;color:{subcolor};width:{sub_w}px">{SUB}</p>')


def materials():
    return {
        "github-social-1280x640": canvas(1280, 640, PETROL, CREAM,
            big(DEEP, 820, 560, -40) + at(80, 92, 250, lg(CREAM, 250))
            + at(80, 268, 560, text(62, 24, CREAM, ON_PETROL, 22, 500))
            + phone("claro-inicio", 712, 70, 250) + phone("escuro-analises", 984, 150, 250)),
        "linkedin-banner-1584x396": canvas(1584, 396, PETROL, CREAM,
            big(DEEP, 640, 60, -60) + at(520, 104, 560, lg(CREAM, 220)
            + '<h1 style="font-size:38px;margin-top:34px;white-space:nowrap">Seu dinheiro, em fluxo claro.</h1>'
            + f'<p style="font-size:20px;margin-top:10px;color:{ON_PETROL}">Finanças pessoais no Android, sem internet.</p>')
            + phone("claro-inicio", 1104, 56, 190) + phone("escuro-analises", 1330, 112, 190)),
        "linkedin-post-1200x1200": canvas(1200, 1200, PAPER, INK,
            big(SAGE, 1300, 420, 40) + at(84, 84, 270, lg(PETROL, 270))
            + at(84, 226, 900, text(72, 28, PETROL, INK2, 24, 640))
            + phone("claro-inicio", 250, 580, 340) + phone("escuro-analises", 660, 660, 340)),
        "portfolio-capa-1600x1000": canvas(1600, 1000, PAPER, INK,
            big(SAGE, 1180, 560, -40) + at(90, 150, 300, lg(PETROL, 300))
            + at(90, 380, 560, text(70, 26, PETROL, INK2, 28, 480))
            + phone("claro-meta", 676, 290, 258) + phone("claro-inicio", 970, 170, 258) + phone("escuro-analises", 1264, 290, 258)),
    }


def main():
    for name, fg in {"petroleo": PETROL, "creme": CREAM, "mono-preto": "#000000", "mono-branco": "#FFFFFF"}.items():
        s, ar = symbol(fg)
        write(f"svg/simbolo-{name}.svg", s)
        png(f"png/simbolo-{name}.png", s, round(1024 * ar), 1024, True)
        s, ar = logo(fg)
        write(f"svg/logo-{name}.svg", s)
        png(f"png/logo-{name}.png", s, round(400 * ar), 400, True)
    for name, (bg, fg) in {"petroleo": (PETROL, CREAM), "creme": (PAPER, PETROL), "noite": (NIGHT, CREAM), "mono": ("#FFFFFF", "#000000")}.items():
        write(f"svg/icone-{name}.svg", icon(bg, fg))
        png(f"png/icone-{name}-1024.png", icon(bg, fg), 1024, 1024)
    write("svg/favicon.svg", small(PETROL, CREAM))
    png("png/favicon-512.png", small(PETROL, CREAM), 512, 512, True)
    (OUT / "materiais").mkdir(exist_ok=True)
    for name, (w, h, body) in materials().items():
        C.shot(body, CSS, OUT / "materiais" / f"{name}.png", w, h)


if __name__ == "__main__":
    main()
