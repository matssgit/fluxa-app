"""Símbolo definitivo do Fluxa (proposta A refinada). Quadro 100×100 centrado em 0."""
import sys
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "icon-concepts" / "tools"))
from symbols import pt, bez, fitted, sample, transform  # noqa: E402

# Refino sobre a proposta: hastes com a mesma largura (11,4), fenda de 5,4 (era 4,6) para resistir a tamanhos pequenos,
# as duas ondas com a mesma família de curva e as pontas na mesma direção.
W, GAP = 11.4, 5.4
X0 = -25.0
X1 = X0 + W          # borda direita da corrente externa
X2 = X1 + GAP        # borda esquerda da corrente interna
X3 = X2 + W
FOOT = 37.0


def raw():
    outer = (
        "M" + pt(X0, FOOT) + "L" + pt(X0, -9)
        + bez((X0, -26), (-13.5, -37.5), (5.5, -37.5))
        + bez((15.5, -37.5), (21, -33.2), (33, -37.5))
        + bez((31.2, -26.4), (23.6, -20.6), (12.6, -20.6))
        + bez((-4, -20.6), (X1, -9.5), (X1, 14))
        + "L" + pt(X1, FOOT) + "Z"
    )
    inner = (
        "M" + pt(X2, FOOT) + "L" + pt(X2, 14.5)
        + bez((X2, 1.6), (-1.2, -6.6), (10.4, -6.6))
        + bez((17, -6.6), (20.4, -3.7), (28, -6.6))
        + bez((26.8, 3.2), (20.8, 8.6), (12.2, 8.6))
        + bez((6.4, 8.6), (X3, 12.2), (X3, 18.2))
        + "L" + pt(X3, FOOT) + "Z"
    )
    return [outer, inner]


# As hastes pesam à esquerda: +1,2 u centra a massa, não só o contorno.
OPTICAL_X = 1.2


def mark(radius=42.0):
    return [transform(d, 1, OPTICAL_X, 0) for d in fitted(raw(), radius)]


# No launcher real o símbolo a 30,8 dp encostava visualmente na máscara (36 dp); 27,5 dp iguala o respiro dos vizinhos.
ANDROID_RADIUS_DP = 27.5


def android():
    """Mesmo desenho em viewport 108 (100 u = 72 dp visíveis)."""
    paths = mark()
    r = max((x * x + y * y) ** .5 for d in paths for x, y in sample(d))
    k = ANDROID_RADIUS_DP / r
    return [transform(d, k, 54 / k, 54 / k) for d in paths]
