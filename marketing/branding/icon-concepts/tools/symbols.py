"""Geometria dos quatro símbolos. Unidade: quadro 100×100 centrado em 0; tudo cabe num círculo de raio 40.

Cada símbolo é uma lista de `d` (um path por peça), preenchidos, sem máscara nem traço: o mesmo dado serve para SVG
editável e, depois, para VectorDrawable.
"""
from math import cos, sin, radians, hypot

R = 40.0


def f(v):
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def pt(x, y):
    return f"{f(x)} {f(y)}"


def rot(p, a):
    c, s = cos(radians(a)), sin(radians(a))
    return (p[0] * c - p[1] * s, p[0] * s + p[1] * c)


def on(angle, r=R):
    """Ponto no círculo; ângulo em graus, horário a partir da direita (y para baixo)."""
    return (r * cos(radians(angle)), r * sin(radians(angle)))


def arc(r, p, sweep=1, large=0):
    return f"A{f(r)} {f(r)} 0 {large} {sweep} {pt(*p)}"


def bez(a, b, p):
    return f"C{pt(*a)} {pt(*b)} {pt(*p)}"


# A — Monograma fluido -------------------------------------------------------------------------------------------
# Duas correntes sobem juntas na haste e se abrem em dois braços ondulados. A fenda que as separa é o espaço negativo.
def concept_a():
    k, ox, oy = 0.90, -2.6, 0.2

    def p(x, y):
        return ((x + ox) * k, (y + oy) * k)
    outer = (
        "M" + pt(*p(-25, 37)) + "L" + pt(*p(-25, -8))
        + bez(p(-25, -25.5), p(-13.5, -37), p(5, -37))
        + bez(p(15, -37), p(20, -33), p(32, -37))
        + bez(p(30, -26), p(22.5, -20), p(11.5, -20))
        + bez(p(-4.5, -20), p(-14.2, -9.5), p(-14.2, 15))
        + "L" + pt(*p(-14.2, 37)) + "Z"
    )
    inner = (
        "M" + pt(*p(-9.2, 37)) + "L" + pt(*p(-9.2, 15))
        + bez(p(-9.2, 1.5), p(-2, -7), p(9.5, -7))
        + bez(p(16, -7), p(19, -4), p(26, -7))
        + bez(p(24.5, 2.6), p(19, 8), p(10.5, 8))
        + bez(p(4, 8), p(2, 12), p(2, 18))
        + "L" + pt(*p(2, 37)) + "Z"
    )
    return [outer, inner]


# B — Fluxo abstrato ---------------------------------------------------------------------------------------------
# Duas correntes entram separadas e se unem em uma só. Entre elas, a folha que se afina: o que fica com você.
# Cada corrente tem a própria linha central e largura óptica constante (compensada pela inclinação).
def concept_b():
    from math import pi, sqrt
    tilt, tip, w = -26.0, 25.0, 3.3

    def y1(x):
        return -5.5 + 8.6 * sin(pi * (x + 4) / 44)

    def d(x):
        u = min(max((x + 48) / (tip + 48), 0), 1)
        return 27.0 * (1 - u * u * (3 - 2 * u))

    def y2(x):
        return y1(x) + d(x)

    def slope(fn, x, e=1e-3):
        return (fn(x + e) - fn(x - e)) / (2 * e)

    def wide(x):
        # a corrente unida ganha corpo depois do encontro
        u = min(max((x - 8) / 26, 0), 1)
        return w + 1.0 * u * u * (3 - 2 * u)

    def edge(fn, sign):
        return lambda x: fn(x) + sign * wide(x) * sqrt(1 + slope(fn, x) ** 2)

    up, lt, lb, dn = edge(y1, -1), edge(y1, 1), edge(y2, -1), edge(y2, 1)

    def at(fn):
        return lambda x: rot((x, fn(x)), tilt)

    def hit(fn, side):
        lo, hi = (-60.0, 0.0) if side < 0 else (0.0, 60.0)
        for _ in range(90):
            m = (lo + hi) / 2
            inside = hypot(m, fn(m)) < R
            if side < 0:
                lo, hi = (lo, m) if inside else (m, hi)
            else:
                lo, hi = (m, hi) if inside else (lo, m)
        return (lo + hi) / 2

    lo, hi = -10.0, tip
    for _ in range(90):  # ponta da folha: onde as margens internas se encontram
        m = (lo + hi) / 2
        lo, hi = (m, hi) if lb(m) > lt(m) else (lo, m)
    end = (lo + hi) / 2

    def curve(fn, xa, xb, n=7):
        g = at(fn)
        h, e = (xb - xa) / n, 1e-3
        out = ""
        for i in range(n):
            x0, x1 = xa + h * i, xa + h * (i + 1)
            a, b = g(x0), g(x1)
            d0 = rot((h / 3, slope(fn, x0, e) * h / 3), tilt)
            d1 = rot((h / 3, slope(fn, x1, e) * h / 3), tilt)
            out += bez((a[0] + d0[0], a[1] + d0[1]), (b[0] - d1[0], b[1] - d1[1]), b)
        return g(xa), out, g(xb)

    s, c, e = curve(up, hit(up, 1), hit(up, -1))
    top = "M" + pt(*e) + arc(R, s, 1, 0) + c + "Z"
    s, c, _ = curve(lt, hit(lt, -1), end)
    _, c2, _ = curve(lb, end, hit(lb, -1))
    leaf = "M" + pt(*s) + c + c2 + arc(R, s, 1, 0) + "Z"
    s, c, e = curve(dn, hit(dn, -1), hit(dn, 1))
    bottom = "M" + pt(*s) + c + arc(R, s, 1, 0) + "Z"
    return [top, leaf, bottom]


# C — Monograma geométrico ---------------------------------------------------------------------------------------
# Um módulo, uma junta e um único raio. Cada peça tem cantos vivos e um quarto de círculo: a "folha" dos cartões lida
# como arquitetura. O F aparece pelo encaixe, não pelo contorno.
def concept_c():
    u, g = 18.6, 4.0
    x0, y0, y1 = -25.0, -32.5, 32.5
    x1 = x0 + u
    stem = f"M{pt(x0, y1)}V{f(y0 + u)}A{f(u)} {f(u)} 0 0 1 {pt(x1, y0)}V{f(y1 - u)}A{f(u)} {f(u)} 0 0 1 {pt(x0, y1)}Z"
    xa = x1 + g
    top_r, mid_r = 28.0, 18.0
    top = f"M{pt(xa, y0)}H{f(top_r)}A{f(u)} {f(u)} 0 0 1 {pt(top_r - u, y0 + u)}H{f(xa)}Z"
    my = y0 + u + 8.4
    mid = f"M{pt(xa, my)}H{f(mid_r)}A{f(u)} {f(u)} 0 0 1 {pt(mid_r - u, my + u)}H{f(xa)}Z"
    return [stem, top, mid]


# D — Símbolo orgânico -------------------------------------------------------------------------------------------
# A forma "folha" dos cartões de destaque, em camadas que crescem a partir do mesmo canto: reserva, saldo, horizonte.
def concept_d():
    def leaf(c, h):
        cx, cy = c
        big, tight = h, h * 0.22
        x0, y0, x1, y1 = cx - h, cy - h, cx + h, cy + h
        return (f"M{pt(x0, y0 + big)}A{f(big)} {f(big)} 0 0 1 {pt(x0 + big, y0)}H{f(x1 - tight)}A{f(tight)} {f(tight)} 0 0 1 {pt(x1, y0 + tight)}"
                f"V{f(y1 - big)}A{f(big)} {f(big)} 0 0 1 {pt(x1 - big, y1)}H{f(x0 + tight)}A{f(tight)} {f(tight)} 0 0 1 {pt(x0, y1 - tight)}Z")
    return [leaf((0, 0), 31) + leaf((-3.4, 3.4), 21.2), leaf((-6.2, 6.2), 12.4)]


# Ajuste final ---------------------------------------------------------------------------------------------------
import re

_TOK = re.compile(r"([MLHVCAZ])|(-?\d*\.?\d+)")


def parse(d):
    out, cmd, nums = [], None, []
    for c, n in _TOK.findall(d):
        if c:
            if cmd:
                out.append((cmd, nums))
            cmd, nums = c, []
        else:
            nums.append(float(n))
    out.append((cmd, nums))
    return out


def transform(d, k, dx, dy):
    """Escala uniforme + translação, mantendo comandos absolutos M L H V C A Z."""
    out = ""
    for c, n in parse(d):
        if c == "H":
            n = [(n[0] + dx) * k]
        elif c == "V":
            n = [(n[0] + dy) * k]
        elif c == "A":
            n = [n[0] * k, n[1] * k, n[2], n[3], n[4], (n[5] + dx) * k, (n[6] + dy) * k]
        else:
            n = [(v + (dx if i % 2 == 0 else dy)) * k for i, v in enumerate(n)]
        out += c + " ".join(f(v) for v in n)
    return out


def sample(d, n=24):
    """Pontos do contorno (arcos aproximados pelo círculo que contém os extremos e a flecha)."""
    from math import sqrt, atan2, pi
    pts, x, y, sx, sy = [], 0, 0, 0, 0
    for c, v in parse(d):
        if c == "M":
            x, y = v; sx, sy = x, y; pts.append((x, y))
        elif c == "L":
            x, y = v; pts.append((x, y))
        elif c == "H":
            x = v[0]; pts.append((x, y))
        elif c == "V":
            y = v[0]; pts.append((x, y))
        elif c == "C":
            for i in range(1, n + 1):
                t = i / n; u = 1 - t
                pts.append((u**3 * x + 3 * u * u * t * v[0] + 3 * u * t * t * v[2] + t**3 * v[4],
                            u**3 * y + 3 * u * u * t * v[1] + 3 * u * t * t * v[3] + t**3 * v[5]))
            x, y = v[4], v[5]
        elif c == "A":
            r, large, sw, ex, ey = v[0], int(v[3]), int(v[4]), v[5], v[6]
            mx, my = (x + ex) / 2, (y + ey) / 2
            hx, hy = (ex - x) / 2, (ey - y) / 2
            q = hypot(hx, hy)
            h = sqrt(max(r * r - q * q, 0))
            # escolhe o centro coerente com large/sweep testando os dois
            best = None
            for s2 in (1, -1):
                ccx, ccy = mx + s2 * h * -hy / q, my + s2 * h * hx / q
                a0, a1 = atan2(y - ccy, x - ccx), atan2(ey - ccy, ex - ccx)
                da = a1 - a0
                if sw and da < 0: da += 2 * pi
                if not sw and da > 0: da -= 2 * pi
                if (abs(da) > pi + 1e-6) == bool(large) or abs(abs(da) - pi) < 1e-6:
                    best = (ccx, ccy, a0, da); break
            ccx, ccy, a0, da = best
            for i in range(1, n + 1):
                t = a0 + da * i / n
                pts.append((ccx + r * cos(t), ccy + r * sin(t)))
            x, y = ex, ey
        elif c == "Z":
            x, y = sx, sy
    return pts


def enclosing(points):
    """Círculo mínimo (descida simples; precisão de centésimos basta para a safe zone)."""
    cx = (min(p[0] for p in points) + max(p[0] for p in points)) / 2
    cy = (min(p[1] for p in points) + max(p[1] for p in points)) / 2
    step = 8.0
    for _ in range(6000):
        far = max(points, key=lambda p: hypot(p[0] - cx, p[1] - cy))
        cx += (far[0] - cx) * step / 100
        cy += (far[1] - cy) * step / 100
        step *= 0.998
    return cx, cy, max(hypot(p[0] - cx, p[1] - cy) for p in points)


def fitted(paths, r=R):
    cx, cy, rad = enclosing([p for d in paths for p in sample(d)])
    return [transform(d, r / rad, -cx, -cy) for d in paths]


RAW = {"a": concept_a, "b": concept_b, "c": concept_c, "d": concept_d}
# Todos ocupam o mesmo círculo de raio 40: comparação justa e safe zone garantida (40 u = 28,8 dp de 33 dp).
CONCEPTS = {k: (lambda fn=fn: fitted(fn())) for k, fn in RAW.items()}
