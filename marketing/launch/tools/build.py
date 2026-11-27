"""Materiais de lançamento do Fluxa: banner do README, social preview, carrossel do LinkedIn (PNG + PDF) e capa do portfólio.

Uso: python marketing/launch/tools/build.py [banner|social|carrossel|portfolio]
Requer só o Chrome instalado. As telas vêm de marketing/launch/screenshots (capturas reais, dados fictícios).
"""
import importlib.util
import subprocess
import sys
import tempfile
from pathlib import Path

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
LAUNCH = HERE.parent
REPO = LAUNCH.parents[1]
FINAL = REPO / "marketing" / "branding" / "final" / "tools"
CONCEPTS = REPO / "marketing" / "branding" / "icon-concepts" / "tools"
sys.path[:0] = [str(CONCEPTS), str(FINAL)]
import mark as M  # noqa: E402

_spec = importlib.util.spec_from_file_location("concepts", CONCEPTS / "build.py")
C = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(C)

CHROME = C.CHROME
SHOTS = LAUNCH / "screenshots"

# Paleta real da v1.0.1 (FluxaColors.kt).
PETROL, DEEP, CREAM, PAPER, SURFACE = "#14414D", "#123A45", "#FFFDF7", "#F6F2EA", "#FFFDF9"
SAGE, NIGHT, NIGHT_CARD, MIST, TERRA = "#E6EAE2", "#0E171B", "#162227", "#A3C4CB", "#B9684F"
INK, INK2, ON_PETROL = "#1E2A2E", "#505A5C", "#D5E1E3"

# Disponibilidade: só mencionar código aberto depois de a licença e a publicação serem aprovadas.
OPEN_SOURCE = True

PATHS = M.mark()
_pts = [p for d in PATHS for p in C.S.sample(d)]
X0, X1 = min(p[0] for p in _pts), max(p[0] for p in _pts)
Y0, Y1 = min(p[1] for p in _pts), max(p[1] for p in _pts)


def symbol(fg, h, style=""):
    w = h * (X1 - X0) / (Y1 - Y0)
    body = "".join(f'<path d="{d}"/>' for d in PATHS)
    return (f'<svg viewBox="{X0:.3f} {Y0:.3f} {X1 - X0:.3f} {Y1 - Y0:.3f}" width="{w:.1f}" height="{h}" style="{style}" '
            f'fill="{fg}" aria-hidden="true">{body}</svg>')


def logo(fg, w, style=""):
    sc = 84 / (Y1 - Y0)
    wx = 10 + (X1 - X0) * sc + 24
    vw = wx + C.WORD_W + 10
    sym = "".join(f'<path d="{d}"/>' for d in PATHS)
    return (f'<svg viewBox="0 0 {vw:.2f} 104" width="{w}" height="{w * 104 / vw:.1f}" style="display:block;{style}" fill="{fg}" role="img" aria-label="Fluxa">'
            f'<g transform="translate({10 - X0 * sc:.3f} {52 - (Y0 + Y1) / 2 * sc:.3f}) scale({sc:.4f})">{sym}</g>'
            f'<g transform="translate({wx:.3f} 87)">{C.wordmark_svg()}</g></svg>')


def phone(shot, w, tilt="", z=0):
    """Aparelho Android genérico (sem marca), com a captura real na tela."""
    h = w * 2340 / 1080
    pad = w * 0.035
    r = w * 0.13
    cam = w * 0.034
    img = (SHOTS / f"{shot}.png").as_uri()
    t = f"transform:{tilt};" if tilt else ""
    return (f'<div class="dev" style="width:{w + 2 * pad:.0f}px;height:{h + 2 * pad:.0f}px;border-radius:{r:.0f}px;padding:{pad:.1f}px;{t}z-index:{z}">'
            f'<div class="scr" style="border-radius:{r - pad * 0.9:.0f}px"><img src="{img}">'
            f'<i class="cam" style="width:{cam:.1f}px;height:{cam:.1f}px;top:{w * 0.028:.1f}px"></i></div></div>')


def at(x, y, inner, extra=""):
    return f'<div class="a" style="left:{x}px;top:{y}px;{extra}">{inner}</div>'


CSS = f"""
*{{box-sizing:border-box;margin:0;padding:0}} html,body{{overflow:hidden}}
body{{font-family:"Segoe UI Variable Text","Segoe UI",system-ui,sans-serif;-webkit-font-smoothing:antialiased;color:{INK}}}
.c{{position:relative;overflow:hidden}} .a{{position:absolute}}
.serif{{font-family:"Sitka Heading","Sitka Display",Georgia,serif;font-weight:600;letter-spacing:-.015em}}
h1,h2{{font-weight:600;line-height:1.04}}
p{{line-height:1.42}}
.k{{font:600 18px/1 "Segoe UI Variable Text","Segoe UI";letter-spacing:.16em;text-transform:uppercase}}
.dev{{position:absolute;background:linear-gradient(150deg,#26313a,#0b1114 38%,#0b1114 70%,#1d272d);
  box-shadow:0 1px 0 1px rgba(255,255,255,.06) inset,0 30px 60px -28px rgba(8,20,24,.55),0 10px 24px -12px rgba(8,20,24,.35);transform-origin:50% 50%}}
.scr{{position:relative;overflow:hidden;width:100%;height:100%;background:#000}} .scr img{{display:block;width:100%}}
.cam{{position:absolute;left:50%;transform:translateX(-50%);border-radius:50%;background:#05080a;box-shadow:0 0 0 2px #10181c}}
.pill{{display:inline-block;padding:10px 20px;border-radius:999px;font-weight:600}}
.rule{{height:2px;width:64px;background:{TERRA};border-radius:2px}}
"""


def render(body, out, w, h, scale=1):
    out.parent.mkdir(parents=True, exist_ok=True)
    C.shot(body, CSS, out, w, h, scale)


def organic(color, size, x, y, opacity=1):
    """O símbolo, grande e recortado, como forma de fundo."""
    return at(x, y, symbol(color, size), f"opacity:{opacity}")


# Banner do README ------------------------------------------------------------------------------------------------
def banner(dark):
    bg, fg, sub, ghost = (NIGHT, CREAM, "#B7C6C9", "#132228") if dark else (PAPER, PETROL, INK2, "#ECE6DA")
    tilt = "perspective(1800px) rotateY(-16deg) rotateX(6deg) rotateZ(-3deg)"
    tilt2 = "perspective(1800px) rotateY(-16deg) rotateX(6deg) rotateZ(-3deg) translateZ(-40px)"
    a, b, c = ("escuro-inicio", "escuro-limite", "escuro-analises") if dark else ("claro-inicio", "claro-limite", "claro-analises")
    body = (organic(ghost, 900, 780, -160)
            + at(96, 150, logo(fg, 300))
            + at(98, 290, f'<p class="serif" style="font-size:44px;color:{fg};line-height:1.15">Suas finanças.<br>Seus dados.<br>Seu controle.</p>')
            + at(100, 488, f'<p style="font-size:21px;color:{sub}">Controle financeiro pessoal para Android · gratuito e offline</p>')
            + phone(c, 240, tilt2).replace('class="dev" style="', 'class="dev" style="left:1330px;top:150px;', 1)
            + phone(b, 260, tilt2).replace('class="dev" style="', 'class="dev" style="left:1045px;top:95px;', 1)
            + phone(a, 290, tilt).replace('class="dev" style="', 'class="dev" style="left:745px;top:40px;', 1))
    w, h = 1600, 640
    return f'<div class="c" style="width:{w}px;height:{h}px;background:{bg}">{body}</div>', w, h


def social():
    tilt = "perspective(1800px) rotateY(-14deg) rotateX(5deg) rotateZ(-3deg)"
    body = (organic(DEEP, 860, 600, -120)
            + at(84, 120, logo(CREAM, 280))
            + at(86, 262, f'<h1 class="serif" style="font-size:56px;color:{CREAM};line-height:1.1">Suas finanças.<br>Seus dados.<br>Seu controle.</h1>')
            + at(88, 486, f'<p style="font-size:22px;color:{ON_PETROL};width:470px">Controle financeiro pessoal para Android, gratuito e offline.</p>')
            + phone("escuro-analises", 250, tilt).replace('class="dev" style="', 'class="dev" style="left:960px;top:150px;', 1)
            + phone("claro-inicio", 270, tilt).replace('class="dev" style="', 'class="dev" style="left:690px;top:70px;', 1))
    return f'<div class="c" style="width:1280px;height:640px;background:{PETROL}">{body}</div>', 1280, 640


# Carrossel ------------------------------------------------------------------------------------------------------
W, H = 1080, 1350
TOTAL = 9


def frame(n, bg, fg, inner, muted):
    head = (at(72, 64, logo(fg, 132))
            + at(W - 72 - 120, 70, f'<p class="k" style="color:{muted};text-align:right;width:120px">{n:02d} / {TOTAL:02d}</p>'))
    return f'<div class="c" style="width:{W}px;height:{H}px;background:{bg};color:{fg}">{inner}{head}</div>'


def title(kicker, text, color, kcolor, size=72):
    return (f'<p class="k" style="color:{kcolor}">{kicker}</p>'
            f'<h2 class="serif" style="font-size:{size}px;color:{color};margin-top:22px">{text}</h2>')


def bullets(items, color, dot, size=30):
    lis = "".join(f'<li style="display:flex;gap:16px;align-items:baseline;margin-top:14px"><i style="flex:none;width:9px;height:9px;border-radius:50%;background:{dot};transform:translateY(-3px)"></i><span>{t}</span></li>' for t in items)
    return f'<ul style="list-style:none;font-size:{size}px;line-height:1.35;color:{color}">{lis}</ul>'


def place(dev, x, y):
    return dev.replace('class="dev" style="', f'class="dev" style="left:{x}px;top:{y}px;', 1)


T_LEFT = "perspective(2000px) rotateY(12deg) rotateX(4deg) rotateZ(2deg)"
T_RIGHT = "perspective(2000px) rotateY(-12deg) rotateX(4deg) rotateZ(-2deg)"


def pages():
    p = []
    # 1 — capa
    p.append(frame(1, PETROL, CREAM,
        organic(DEEP, 1500, 330, 120)
        + at(72, 250, f'<h1 class="serif" style="font-size:104px;color:{CREAM}">Conheça<br>o Fluxa.</h1>'
             f'<p style="font-size:31px;color:{ON_PETROL};margin-top:34px;width:430px">Controle financeiro pessoal, gratuito e offline.</p>'
             f'<div class="rule" style="margin-top:44px"></div>'
             f'<p style="font-size:21px;color:{ON_PETROL};margin-top:22px">Para Android</p>')
        + place(phone("claro-inicio", 420, "perspective(2200px) rotateY(-14deg) rotateX(5deg) rotateZ(-3deg)"), 580, 300),
        ON_PETROL))
    # 2 — organização
    p.append(frame(2, PAPER, INK,
        at(72, 190, title("Organização", "Cada real no<br>seu lugar.", PETROL, TERRA)
           + f'<div style="width:470px;margin-top:40px">' + bullets(["Contas corrente, carteira e poupança", "Receitas e despesas por categoria", "Histórico filtrável por período e tipo"], INK2, PETROL) + "</div>")
        + place(phone("escuro-novo-lancamento", 330, T_LEFT), 110, 820)
        + place(phone("claro-caixa", 360, T_RIGHT), 640, 170, ).replace("z-index:0", "z-index:2"),
        INK2))
    # 3 — cartões e compromissos
    p.append(frame(3, NIGHT, CREAM,
        at(72, 190, title("Cartões e compromissos", "Parcelas e<br>assinaturas<br>sob controle.", CREAM, MIST, 70))
        + at(72, 520, '<div style="width:470px">' + bullets(["Limite calculado pelo que já está comprometido", "Compras parceladas, com pagamento de cada parcela", "Assinaturas em conta ou cartão, com agenda"], "#C9D5D8", MIST) + "</div>")
        + place(phone("escuro-compra", 300, T_LEFT), 150, 1010)
        + place(phone("claro-cartao", 360, T_RIGHT), 610, 200),
        "#8FA3A8"))
    # 4 — planejamento
    p.append(frame(4, PAPER, INK,
        organic(SAGE, 1100, 520, 420)
        + at(72, 190, title("Planejamento", "Quanto dá para<br>gastar hoje?", PETROL, TERRA))
        + at(72, 470, f'<p style="font-size:29px;color:{INK2};width:470px">O <b style="color:{PETROL}">Limite seguro</b> mostra quanto pode ser gasto sem faltar dinheiro para as parcelas e assinaturas dos próximos 30 dias.</p>'
             f'<p style="font-size:29px;color:{INK2};width:470px;margin-top:26px">Metas reservam dinheiro em uma conta sem contar duas vezes.</p>'
             f'<p style="font-size:21px;color:#6F7779;width:450px;margin-top:36px">Os cálculos consideram apenas as informações e os compromissos registrados no app.</p>')
        + place(phone("claro-meta", 300, T_LEFT), 640, 1010)
        + place(phone("claro-limite", 340, T_RIGHT), 640, 190),
        INK2))
    # 5 — análises
    p.append(frame(5, PETROL, CREAM,
        at(72, 190, title("Análises", "Entenda para onde<br>o dinheiro vai.", CREAM, MIST))
        + at(72, 430, '<div style="width:900px">' + bullets(["Resumo do mês e gastos por categoria", "Últimos seis meses e projeção de 30 dias", "Custo das assinaturas e observações automáticas"], ON_PETROL, MIST) + "</div>")
        + place(phone("escuro-analises", 290, T_LEFT), 70, 700)
        + place(phone("claro-analises-meses", 300, ""), 395, 650).replace("z-index:0", "z-index:2")
        + place(phone("escuro-analises-projecao", 290, T_RIGHT), 720, 700),
        ON_PETROL))
    # 6 — privacidade
    p.append(frame(6, NIGHT, CREAM,
        at(72, 190, title("Privacidade e segurança", "Seus dados<br>ficam no<br>seu celular.", CREAM, MIST))
        + at(72, 540, '<div style="width:500px">' + bullets([
            "Sem internet, sem conta, sem servidor",
            "Desbloqueio com biometria ou com o bloqueio do Android",
            "Bloqueio automático e proteção contra capturas",
            "Backup criptografado com a sua senha"], "#C9D5D8", MIST) + "</div>")
        + place(phone("escuro-seguranca", 340, T_RIGHT), 640, 300),
        "#8FA3A8"))
    # 7 — engenharia
    cards = [
        ("Offline-first", "Kotlin, Jetpack Compose, Room e DataStore. Tudo é calculado no aparelho."),
        ("Dinheiro exato", "Valores em centavos inteiros. Estouros são detectados, nunca arredondados em silêncio."),
        ("Consistência", "Operações atômicas, índices únicos contra pagamentos duplicados e triggers no banco."),
        ("Testado", "326 testes JVM, 200 casos instrumentados e testes de atualização entre versões."),
    ]
    grid = "".join(f'<div style="background:{SURFACE};border:1px solid rgba(58,47,36,.1);border-radius:30px 8px 30px 8px;padding:34px 32px;height:262px">'
                   f'<p class="k" style="color:{TERRA}">{k}</p><p style="font-size:27px;line-height:1.35;color:{INK};margin-top:20px">{t}</p></div>' for k, t in cards)
    p.append(frame(7, PAPER, INK,
        at(72, 190, title("Engenharia", "Um app pequeno,<br>feito com cuidado.", PETROL, TERRA))
        + at(72, 470, f'<div style="width:936px;display:grid;grid-template-columns:1fr 1fr;gap:24px">{grid}</div>')
        + at(72, 1080, f'<p style="font-size:24px;color:{INK2};width:930px">Regras financeiras como funções puras, migrations explícitas da versão 1 à 7 e backup com formato versionado.</p>'),
        INK2))
    # 8 — disponibilidade
    avail = (["Gratuito, sem anúncios e sem conta", "Código aberto no GitHub", "Download do APK nas releases", "Aberto a sugestões e relatos de bugs"]
             if OPEN_SOURCE else
             ["Gratuito, sem anúncios e sem conta", "Download do APK pelo GitHub", "Aberto a sugestões e relatos de bugs", "Melhorias publicadas aos poucos, conforme o uso"])
    p.append(frame(8, PETROL, CREAM,
        organic(DEEP, 1300, 470, 260)
        + at(72, 190, title("Disponível", "Feito para ser<br>usado e melhorado.", CREAM, MIST))
        + at(72, 470, '<div style="width:560px">' + bullets(avail, ON_PETROL, MIST, 31) + "</div>")
        + place(phone("claro-assinaturas", 320, T_RIGHT), 650, 760),
        ON_PETROL))
    # 9 — encerramento
    p.append(frame(9, PAPER, INK,
        organic(SAGE, 1400, 380, 200)
        + at(72, 330, symbol(PETROL, 120))
        + at(72, 520, f'<h2 class="serif" style="font-size:66px;color:{PETROL}">Baixe, teste e me<br>conte o que achou.</h2>'
             f'<p style="font-size:26px;color:{INK2};margin-top:36px">Fluxa é um projeto independente de</p>'
             f'<p style="font-size:34px;color:{INK};font-weight:600;margin-top:6px">Matheus Santana</p>'
             f'<div class="rule" style="margin-top:40px"></div>'
             f'<p style="font-size:24px;color:{INK2};margin-top:28px;line-height:1.7">matheusantanadev.vercel.app<br>github.com/matssgit</p>'),
        INK2))
    return p


def carousel():
    out = LAUNCH / "linkedin" / "carrossel"
    out.mkdir(parents=True, exist_ok=True)
    docs = []
    for i, body in enumerate(pages(), 1):
        render(body, out / f"pagina-{i:02d}.png", W, H)
        docs.append(body)
    # Fonte editável: um HTML com todas as páginas (abre no navegador) e o PDF gerado a partir dele.
    sheet = "".join(f'<section style="width:{W}px;height:{H}px;page-break-after:always;overflow:hidden">{b}</section>' for b in docs)
    html = f'<!doctype html><meta charset="utf-8"><title>Fluxa — carrossel</title><style>{CSS}@page{{size:{W}px {H}px;margin:0}}html,body{{overflow:visible}}</style><body>{sheet}</body>'
    with tempfile.TemporaryDirectory(ignore_cleanup_errors=True) as t:
        src = Path(t) / "c.html"
        src.write_text(html, encoding="utf-8")
        subprocess.run([CHROME, "--headless=new", "--disable-gpu", f"--user-data-dir={t}/prof", "--no-pdf-header-footer",
                        f"--print-to-pdf={out / 'fluxa-carrossel.pdf'}", src.as_uri()], check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def portfolio():
    tilt = "perspective(2000px) rotateY(-14deg) rotateX(5deg) rotateZ(-3deg)"
    body = (organic("#ECE6DA", 1250, 760, -160)
            + at(110, 170, logo(PETROL, 300))
            + at(112, 360, f'<h1 class="serif" style="font-size:74px;color:{PETROL};line-height:1.08">Suas finanças.<br>Seus dados.<br>Seu controle.</h1>')
            + at(114, 660, f'<p style="font-size:26px;color:{INK2};width:520px">Controle financeiro pessoal para Android, gratuito e offline.</p>')
            + at(114, 800, f'<p class="k" style="color:{TERRA}">Kotlin · Jetpack Compose · Room · offline-first</p>')
            + place(phone("escuro-analises", 265, tilt), 1315, 250)
            + place(phone("claro-limite", 280, tilt), 1020, 170)
            + place(phone("claro-inicio", 300, tilt), 705, 110))
    render(f'<div class="c" style="width:1600px;height:1000px;background:{PAPER}">{body}</div>', LAUNCH / "portfolio" / "capa-1600x1000.png", 1600, 1000)
    # Card (miniatura) do projeto
    body = (organic(DEEP, 760, 330, -60)
            + at(56, 64, logo(CREAM, 190))
            + at(58, 190, f'<p style="font-size:24px;color:{ON_PETROL};width:300px">Controle financeiro pessoal, gratuito e offline.</p>')
            + place(phone("claro-inicio", 230, tilt), 420, 80))
    render(f'<div class="c" style="width:800px;height:500px;background:{PETROL}">{body}</div>', LAUNCH / "portfolio" / "card-800x500.png", 800, 500)


def main():
    what = set(sys.argv[1:]) or {"banner", "social", "carrossel", "portfolio"}
    if "banner" in what:
        for dark, name in ((False, "banner-claro"), (True, "banner-escuro")):
            body, w, h = banner(dark)
            render(body, LAUNCH / "github" / f"{name}.png", w, h)
    if "social" in what:
        body, w, h = social()
        render(body, LAUNCH / "github" / "social-preview-1280x640.png", w, h)
    if "carrossel" in what:
        carousel()
    if "portfolio" in what:
        portfolio()


if __name__ == "__main__":
    main()
