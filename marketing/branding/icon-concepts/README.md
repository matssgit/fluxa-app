# Fluxa — propostas de ícone e marca (etapa 1)

Base: `d0d3d09` (v1.0.1). **Escolhida: A — Monograma fluido.** Versão final em [../final](../final/README.md). As quatro propostas ficam aqui como registro da etapa 1.

- `index.html` — comparação interativa (fundo, máscara, tema, safe zone). Abre sem servidor.
- `prancha-comparativa.png` — 4800 × 9120.
- `a-monograma-fluido/`, `b-fluxo-abstrato/`, `c-monograma-geometrico/`, `d-simbolo-organico/`
  - `svg/` símbolo (petróleo, creme, mono preto, mono branco), ícones, logotipos e camada adaptive de 108 dp. Só contornos preenchidos, sem máscara, traço ou fonte.
  - `png/` ícones 1024 × 1024 (petróleo, creme, mono), símbolo transparente, logotipos, launcher e máscaras.
- `tools/` — `symbols.py` (geometria) e `build.py` (gera tudo; usa só o Chrome instalado).

## Adaptive icon

| Proposta | Raio do símbolo | Safe zone |
| --- | --- | --- |
| A | 30,2 dp | 33 dp |
| B | 26,3 dp | 33 dp |
| C | 30,2 dp | 33 dp |
| D | 28,4 dp | 33 dp |

Círculo mínimo que contém cada símbolo, calculado a partir dos contornos. Máscaras circular, arredondada e superelipse conferidas visualmente.

As referências enviadas orientaram qualidades (fluidez, espaço negativo, corte preciso); nenhuma foi redesenhada. Não houve pesquisa de marca registrada. O letreiro "Fluxa" foi desenhado para esta entrega e é o mesmo nas quatro, para comparar só o símbolo.
