#!/usr/bin/env python3
"""
Renderiza las propuestas de icono de la 1.3 para poder compararlas antes de tocar los drawables.

    python3 docs/propuestas-icono/generar-propuestas.py

Cada propuesta se dibuja en el lienzo de 108 unidades del icono adaptativo y se saca:

    · recortada con la máscara **círculo** (Pixel Launcher) a 216 px
    · recortada con la máscara **squircle** (One UI, Nova) a 216 px
    · a 48 px, que es el tamaño al que se ve de verdad en el escritorio
    · sobre fondo de pantalla **oscuro y claro**, que es donde se cayó el icono de la 1.1

Se dibuja todo a 8× y se reduce con Lanczos, igual que `docs/store-assets/generar-assets.py`: sin un
rasterizador de verdad, el supermuestreo es lo que da los bordes limpios.

Esto es un banco de pruebas, no el asset final. Cuando una propuesta se elija, sus formas se pasan a
`res/drawable/ic_launcher_*.xml` y a `generar-assets.py`, y este fichero se queda como registro de lo
que se descartó.
"""

import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SALIDA = os.path.dirname(os.path.abspath(__file__))
FUENTES = os.path.join(RAIZ, "app", "src", "main", "res", "font")

# ── Paleta, de ui/theme/Color.kt y del icono actual ─────────────────────────────────────────────────
ROJO_ALTO = (0xFF, 0x52, 0x40)      # arranque del degradado del icono actual
ROJO_BAJO = (0xB4, 0x24, 0x1A)      # final del degradado del icono actual
TOMATE_FILL = (0xE2, 0x3B, 0x2B)
TOMATE_DEEP = (0x7A, 0x20, 0x18)
TOMATE_GHOST = (0x1C, 0x0D, 0x0B)
HUESO = (0xF5, 0xF2, 0xEF)
VERDE = (0x57, 0x94, 0x3E)
VERDE_OSCURO = (0x37, 0x62, 0x2A)
NEGRO = (0, 0, 0)
CASI_NEGRO = (0x10, 0x0A, 0x0A)

LIENZO = 108          # unidades del icono adaptativo
VENTANA = 72          # lo que el launcher deja ver
SEGURA = 66           # zona que ninguna máscara recorta
ESCALA = 8            # supermuestreo
PX = LIENZO * ESCALA  # lado del lienzo de trabajo


# ── Utilidades de dibujo ────────────────────────────────────────────────────────────────────────────
def u(v):
    """Unidades del icono → píxeles del lienzo de trabajo."""
    return v * ESCALA


def lienzo_nuevo(color=(0, 0, 0, 0)):
    return Image.new("RGBA", (PX, PX), color)


def degradado_rojo():
    """El degradado del icono actual: #FF5240 → #B4241A sobre el eje (18,14)→(94,98)."""
    ini, fin = np.array([u(18), u(14)]), np.array([u(94), u(98)])
    d = fin - ini
    ys, xs = np.mgrid[0:PX, 0:PX]
    t = ((xs - ini[0]) * d[0] + (ys - ini[1]) * d[1]) / float(d @ d)
    t = np.clip(t, 0.0, 1.0)[..., None]
    a = np.array(ROJO_ALTO, dtype=float)
    b = np.array(ROJO_BAJO, dtype=float)
    rgb = (a + (b - a) * t).astype(np.uint8)
    alfa = np.full((PX, PX, 1), 255, dtype=np.uint8)
    return Image.fromarray(np.concatenate([rgb, alfa], axis=2), "RGBA")


def disco(img, cx, cy, r, color):
    ImageDraw.Draw(img).ellipse([u(cx - r), u(cy - r), u(cx + r), u(cy + r)], fill=color)


def anillo(img, cx, cy, r, grosor, color):
    """Anillo centrado en el radio [r]: el trazo va de r-grosor/2 a r+grosor/2."""
    ImageDraw.Draw(img).ellipse(
        [u(cx - r), u(cy - r), u(cx + r), u(cy + r)],
        outline=color,
        width=int(round(u(grosor))),
    )


def punto_reloj(cx, cy, r, minuto):
    """Punto del dial de 60 minutos: el 0 arriba y el sentido horario, como un reloj."""
    a = math.radians(minuto * 6.0)
    return cx + r * math.sin(a), cy - r * math.cos(a)


def marca(img, cx, cy, minuto, r0, r1, grosor, color):
    x0, y0 = punto_reloj(cx, cy, r0, minuto)
    x1, y1 = punto_reloj(cx, cy, r1, minuto)
    ImageDraw.Draw(img).line(
        [u(x0), u(y0), u(x1), u(y1)], fill=color, width=int(round(u(grosor)))
    )


def arco(img, cx, cy, r, grosor, minuto_ini, minuto_fin, color, extremos_redondos=True):
    """Arco sobre el dial de minutos. PIL no sabe de caps, así que los extremos se tapan a mano."""
    ImageDraw.Draw(img).arc(
        [u(cx - r), u(cy - r), u(cx + r), u(cy + r)],
        start=minuto_ini * 6.0 - 90,
        end=minuto_fin * 6.0 - 90,
        fill=color,
        width=int(round(u(grosor))),
    )
    if extremos_redondos:
        for m in (minuto_ini, minuto_fin):
            x, y = punto_reloj(cx, cy, r, m)
            disco(img, x, y, grosor / 2.0, color)


def manecilla(img, cx, cy, minuto, largo, ancho, color, cola=0.0):
    """
    Manecilla de un solo brazo: base ancha en el eje, punta fina.

    Se dibuja como un polígono en vez de una línea gruesa porque una línea de grosor constante en un
    icono a 48 px parece una cerilla; el estrechamiento es lo que la hace leer como aguja.
    """
    a = math.radians(minuto * 6.0)
    dx, dy = math.sin(a), -math.cos(a)
    nx, ny = -dy, dx  # normal
    px_, py_ = cx + dx * largo, cy + dy * largo                  # punta
    bx, by = cx - dx * cola, cy - dy * cola                      # cola tras el eje
    ImageDraw.Draw(img).polygon(
        [
            (u(px_ + nx * ancho * 0.18), u(py_ + ny * ancho * 0.18)),
            (u(bx + nx * ancho * 0.5), u(by + ny * ancho * 0.5)),
            (u(bx - nx * ancho * 0.5), u(by - ny * ancho * 0.5)),
            (u(px_ - nx * ancho * 0.18), u(py_ - ny * ancho * 0.18)),
        ],
        fill=color,
    )


def bezier2(p0, p1, p2, pasos=18):
    return [
        (
            (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0],
            (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1],
        )
        for t in (i / pasos for i in range(pasos + 1))
    ]


def hoja(img, cx, cy, angulo, largo, ancho, color):
    """
    Una hoja lanceolada saliendo de (cx,cy) hacia [angulo]: dos cuadráticas y una punta.

    La primera versión de esto eran puntas de estrella rectas y el resultado no leía como cáliz, leía
    como hélice. Las hojas de un tomate son almendradas, y ese perfil es lo que hace que el verde se
    reconozca como fruta y no como un adorno geométrico.
    """
    cos_a, sin_a = math.cos(angulo), math.sin(angulo)

    def sitio(px, py):
        return (cx + px * cos_a - py * sin_a, cy + px * sin_a + py * cos_a)

    base, punta = (0.0, 0.0), (largo, 0.0)
    ctrl = (largo * 0.34, ancho)
    perfil = bezier2(base, ctrl, punta) + bezier2(punta, (ctrl[0], -ancho), base)[1:]
    ImageDraw.Draw(img).polygon([(u(x), u(y)) for x, y in (sitio(*p) for p in perfil)], fill=color)


def surco(img, cx, cy, minuto, r0, r1, desvio, grosor, color):
    """
    Un surco del tomate: del centro al borde, curvado.

    Rectos parecían radios de bicicleta. Los gajos de un tomate salen del cáliz abriéndose, así que la
    curva es la que hace que el disco rojo se lea como fruta.
    """
    p0 = punto_reloj(cx, cy, r0, minuto)
    p2 = punto_reloj(cx, cy, r1, minuto + desvio)
    p1 = punto_reloj(cx, cy, (r0 + r1) / 2, minuto + desvio * 0.15)
    ImageDraw.Draw(img).line(
        [(u(x), u(y)) for x, y in bezier2(p0, p1, p2, pasos=20)],
        fill=color,
        width=int(round(u(grosor))),
        joint="curve",
    )


def caliz_desde_arriba(img, cx, cy, r, hojas, color, color_centro, giro=0.0, ancho=0.34):
    """
    El cáliz de un tomate mirado desde arriba: [hojas] hojas radiales y el rabillo cortado en medio.

    Es la pieza que permite que la fruta y una esfera de reloj sean la misma forma, porque el cáliz
    cae exactamente donde va el eje de las agujas.
    """
    for i in range(hojas):
        a = giro + i * 2 * math.pi / hojas - math.pi / 2
        hoja(img, cx, cy, a, r, r * ancho, color)
    disco(img, cx, cy, r * 0.23, color_centro)


def caliz_tres_hojas(img, cx, cy, ancho, alto, color, color_tallo, alto_tallo=12.0):
    """El cáliz del icono actual: tres hojas planas en horizontal, con el rabillo saliendo arriba."""
    d = ImageDraw.Draw(img)
    d.rectangle(
        [u(cx - 1.7), u(cy - alto_tallo), u(cx + 1.7), u(cy + 2)], fill=color_tallo
    )
    d.polygon(
        [
            (u(cx), u(cy)),
            (u(cx + ancho * 0.54), u(cy - alto * 0.62)),
            (u(cx + ancho), u(cy)),
            (u(cx + ancho * 0.47), u(cy + alto)),
            (u(cx), u(cy + alto * 1.14)),
            (u(cx - ancho * 0.47), u(cy + alto)),
            (u(cx - ancho), u(cy)),
            (u(cx - ancho * 0.54), u(cy - alto * 0.62)),
        ],
        fill=color,
    )


def brillo(img, cx=36, cy=45):
    """El brillo especular del icono actual, blanco al 20 %, desplazable."""
    capa = lienzo_nuevo()
    dx, dy = cx - 36, cy - 45
    pts = [
        (36, 45), (40.5, 39), (48, 36), (54, 37.5),
        (48, 40.5), (42, 45), (39, 52.5), (36.75, 51.75),
    ]
    ImageDraw.Draw(capa).polygon(
        [(u(x + dx), u(y + dy)) for x, y in pts], fill=(255, 255, 255, 51)
    )
    return Image.alpha_composite(img, capa)


def superficie_liquido(img, cx, cy, r, fraccion, color):
    """
    El tomate relleno hasta [fraccion] con la superficie ondulada, como en la pantalla Temporizador.

    Reproduce la forma de `domain/render/TomatoGeometry.kt` a grandes rasgos: dos periodos de seno con
    amplitud proporcional al radio. No importa la fase exacta; importa que el icono enseñe el gesto
    que hace la app.
    """
    mascara = Image.new("L", (PX, PX), 0)
    md = ImageDraw.Draw(mascara)
    md.ellipse([u(cx - r), u(cy - r), u(cx + r), u(cy + r)], fill=255)

    nivel = cy + r - 2 * r * fraccion
    amplitud = r * 0.055
    onda = [(u(cx - r), u(nivel))]
    pasos = 96
    for i in range(pasos + 1):
        x = cx - r + 2 * r * i / pasos
        y = nivel + amplitud * math.sin(2 * math.pi * 2 * i / pasos)
        onda.append((u(x), u(y)))
    onda += [(u(cx + r), u(cy + r + 2)), (u(cx - r), u(cy + r + 2))]

    corte = Image.new("L", (PX, PX), 0)
    ImageDraw.Draw(corte).polygon(onda, fill=255)
    mascara = Image.fromarray(
        np.minimum(np.array(mascara), np.array(corte))
    )

    tinta = Image.new("RGBA", (PX, PX), color + (255,))
    return Image.composite(tinta, img, mascara)


def cifra(img, texto, cx, cy, tamano, color, peso=600):
    f = ImageFont.truetype(os.path.join(FUENTES, "space_grotesk_variable.ttf"), int(u(tamano)))
    try:
        f.set_variation_by_axes([peso])
    except Exception:
        pass
    d = ImageDraw.Draw(img)
    caja = d.textbbox((0, 0), texto, font=f)
    d.text(
        (u(cx) - (caja[0] + caja[2]) / 2, u(cy) - (caja[1] + caja[3]) / 2),
        texto,
        font=f,
        fill=color,
    )


# ── Las propuestas ──────────────────────────────────────────────────────────────────────────────────
# Cada una devuelve el lienzo de 108 unidades ya compuesto (fondo + primer plano juntos: aquí no hace
# falta separar capas, eso se hace al pasarlo a los drawables).
C = LIENZO / 2.0   # 54, el centro


def p0_actual():
    """Control: el icono de la 1.2.0, tal cual está publicado."""
    img = degradado_rojo()
    img = brillo(img)
    capa = lienzo_nuevo()
    caliz_tres_hojas(capa, C, 31.4, 19.5, 10.4, VERDE, VERDE_OSCURO, alto_tallo=7.4)
    return Image.alpha_composite(img, capa)


def p1_anterior():
    """
    Control: el de la 1.1.0, tomate rojo sobre placa oscura. Es el que Jorge echa de menos.

    La fruta iba casi tan grande como la ventana visible —el negro era un filo, no un marco—, así que
    aquí va con r=30 sobre 36. Con menos, la reconstrucción exagera la placa y hace parecer al icono
    viejo peor de lo que era.
    """
    img = lienzo_nuevo(CASI_NEGRO + (255,))
    cuerpo = degradado_rojo()
    mascara = Image.new("L", (PX, PX), 0)
    ImageDraw.Draw(mascara).ellipse(
        [u(C - 30), u(C - 27), u(C + 30), u(C + 31)], fill=255
    )
    img = Image.composite(cuerpo, img, mascara)
    img = brillo(img, cx=36, cy=44)
    capa = lienzo_nuevo()
    caliz_tres_hojas(capa, C, 28.5, 17.5, 9.0, VERDE, VERDE_OSCURO, alto_tallo=7.5)
    return Image.alpha_composite(img, capa)


def pA_bisel():
    """
    A · Bisel de reloj. El rojo sigue llenando el lienzo, y dentro va un dial de hueso con marcas y
    una sola aguja en el 25. El anillo es lo que devuelve el borde que el icono actual perdió: cierra
    la fruta por dentro, así que se lee redonda incluso cuando el fondo de pantalla también es rojizo.
    """
    img = degradado_rojo()
    img = brillo(img, cx=30, cy=40)
    capa = lienzo_nuevo()

    # Bisel fino y en hueso, no en blanco puro: el hueso es el color de texto de la app. Grueso, el
    # aro convertía el icono en un reloj de pared de juguete.
    anillo(capa, C, C, 31.0, 2.4, HUESO + (255,))
    for m in range(0, 60, 5):
        largo = 4.4 if m % 15 == 0 else 2.8
        grosor = 2.0 if m % 15 == 0 else 1.4
        marca(capa, C, C, m, 27.4, 27.4 - largo, grosor, HUESO + (235,))
    manecilla(capa, C, C, 25, 20.0, 4.0, HUESO + (255,), cola=3.0)
    disco(capa, C, C, 3.0, HUESO + (255,))

    caliz_tres_hojas(capa, C, 25.6, 15.0, 7.8, VERDE, VERDE_OSCURO, alto_tallo=4.0)
    return Image.alpha_composite(img, capa)


def pB_arco():
    """
    B · Arco de 25 minutos. Sin aguja y sin marcas: solo la pista y el arco que cubre los 25 primeros
    minutos, y el cáliz en el centro porque el tomate se mira desde arriba. Es la propuesta que sigue
    al pie de la letra la regla del design-spec —si dudas entre poner algo o no, no lo pongas— y la
    única que además dice *cuánto queda*, que es lo que hace la app.
    """
    img = degradado_rojo()
    img = brillo(img, cx=30, cy=40)
    capa = lienzo_nuevo()

    # Sin pista bajo el arco: el rojo de la fruta ya hace de pista, y una pista oscura de verdad se
    # veía como un cerco marrón sucio en la mitad que no cubre el arco.
    arco(capa, C, C, 30.0, 3.6, 0, 25, HUESO + (255,))
    caliz_desde_arriba(capa, C, C, 9.0, 5, VERDE + (255,), VERDE_OSCURO + (255,), ancho=0.28)
    return Image.alpha_composite(img, capa)


def pC_desde_arriba():
    """
    C · Tomate visto desde arriba. La fruta *es* la esfera: el cáliz cae en el centro, que es donde
    está el eje de un reloj, las marcas son los gajos y la única aguja apunta al 25. No hay bisel ni
    número; el icono no ilustra un tomate y un reloj, es la misma forma leída de dos maneras.
    """
    img = degradado_rojo()
    img = brillo(img, cx=28, cy=38)

    # Gajos: seis surcos apenas visibles saliendo del centro. Son lo que hace que el disco rojo se lea
    # como fruta cortada por arriba y no como una esfera de reloj cualquiera pintada de rojo.
    gajos = lienzo_nuevo()
    for i in range(6):
        surco(gajos, C, C, i * 10 + 2, 9.0, 33.0, 3.2, 1.8, (0, 0, 0, 34))
    img = Image.alpha_composite(img, gajos)

    capa = lienzo_nuevo()
    for m in range(0, 60, 5):
        largo = 5.0 if m % 15 == 0 else 3.2
        grosor = 2.2 if m % 15 == 0 else 1.5
        marca(capa, C, C, m, 31.0, 31.0 - largo, grosor, HUESO + (255,))
    # El cáliz va debajo de la aguja y con un remache encima: si no, la aguja parte el verde en dos y
    # a 48 px el centro se convierte en un borrón.
    caliz_desde_arriba(capa, C, C, 10.0, 5, VERDE + (255,), VERDE_OSCURO + (255,), ancho=0.28)
    manecilla(capa, C, C, 25, 22.5, 4.4, HUESO + (255,), cola=3.6)
    disco(capa, C, C, 2.2, HUESO + (255,))
    return Image.alpha_composite(img, capa)


def pD_se_vacia():
    """
    D · El tomate que se vacía. El icono es el elemento de la pantalla principal: el mismo círculo con
    el líquido a media altura y la superficie ondulando. Vuelve el fondo oscuro que Jorge echa de
    menos, pero la fruta llena la ventana visible y solo deja un filo, que hace de contorno en vez de
    placa.
    """
    img = lienzo_nuevo(NEGRO + (255,))
    capa_fruta = lienzo_nuevo()
    disco(capa_fruta, C, C, 35.5, TOMATE_GHOST + (255,))
    img = Image.alpha_composite(img, capa_fruta)
    img = superficie_liquido(img, C, C, 35.5, 0.62, TOMATE_FILL)

    capa = lienzo_nuevo()
    anillo(capa, C, C, 34.8, 1.6, TOMATE_DEEP + (255,))
    for m in range(0, 60, 5):
        marca(capa, C, C, m, 31.5, 28.8, 1.5, HUESO + (150,))
    # El cáliz queda en el hueco vacío, por encima de la superficie: el tomate se está bebiendo desde
    # arriba, que es literalmente lo que hace la pantalla Temporizador.
    caliz_tres_hojas(capa, C, 25.6, 13.5, 7.0, VERDE, VERDE_OSCURO, alto_tallo=4.0)
    return Image.alpha_composite(img, capa)


def pE_cocina():
    """
    E · Dial de temporizador de cocina. El 25 escrito, en Space Grotesk, dentro del aro de marcas: la
    lectura es inmediata y ninguna otra propuesta dice el número. El riesgo es igual de inmediato —el
    icono se casa con un ajuste que el usuario puede cambiar— y a 48 px dos cifras son dos manchas.
    """
    img = degradado_rojo()
    img = brillo(img, cx=30, cy=38)
    capa = lienzo_nuevo()

    for m in range(0, 60, 5):
        grosor = 2.4 if m % 15 == 0 else 1.5
        largo = 4.4 if m % 15 == 0 else 3.0
        marca(capa, C, C, m, 30.5, 30.5 - largo, grosor, HUESO + (255,))
    cifra(capa, "25", C, C + 1.5, 27.0, HUESO + (255,), peso=600)
    caliz_tres_hojas(capa, C, 25.6, 13.5, 7.0, VERDE, VERDE_OSCURO, alto_tallo=4.0)
    return Image.alpha_composite(img, capa)


def pF_dial_desnudo():
    """
    F · Dial desnudo. Lo mismo que A pero sin bisel: las marcas y la aguja van pintadas directamente
    sobre la fruta, y el cáliz se queda donde le toca, arriba. Es la lectura más sobria de las seis y
    la que mejor cumple la regla del design-spec de no añadir cajas: no hay aro, no hay disco interior,
    no hay número. Solo la fruta y la aguja en el 25.
    """
    img = degradado_rojo()
    img = brillo(img, cx=30, cy=41)
    capa = lienzo_nuevo()

    for m in range(0, 60, 5):
        largo = 4.8 if m % 15 == 0 else 3.0
        grosor = 2.2 if m % 15 == 0 else 1.5
        marca(capa, C, C, m, 31.5, 31.5 - largo, grosor, HUESO + (255,))
    manecilla(capa, C, C, 25, 21.5, 4.4, HUESO + (255,), cola=3.4)
    disco(capa, C, C, 3.2, HUESO + (255,))
    caliz_tres_hojas(capa, C, 25.6, 14.5, 7.6, VERDE, VERDE_OSCURO, alto_tallo=4.0)
    return Image.alpha_composite(img, capa)


def pG_placa_hueso():
    """
    G · Placa de hueso. La vía de Tomato, que es la comparación que abrió el asunto: placa clara y la
    fruta entera dentro, con silueta cerrada por los cuatro lados. Es la única propuesta en la que se
    ve el *contorno* del tomate y no solo su interior.

    A cambio mete en el escritorio el único blanco de todo el producto, y sobre un fondo de pantalla
    claro la placa desaparece igual que desaparecía la negra sobre uno oscuro: una placa no salva de
    los dos extremos, solo elige de qué lado fallar.
    """
    img = lienzo_nuevo(HUESO + (255,))

    cuerpo = degradado_rojo()
    mascara = Image.new("L", (PX, PX), 0)
    ImageDraw.Draw(mascara).ellipse(
        [u(C - 29.0), u(C - 26.5), u(C + 29.0), u(C + 30.0)], fill=255
    )
    img = Image.composite(cuerpo, img, mascara)

    capa = lienzo_nuevo()
    for m in range(0, 60, 5):
        largo = 4.2 if m % 15 == 0 else 2.6
        grosor = 2.0 if m % 15 == 0 else 1.4
        marca(capa, C, C + 1.8, m, 25.0, 25.0 - largo, grosor, HUESO + (250,))
    manecilla(capa, C, C + 1.8, 25, 17.5, 3.8, HUESO + (255,), cola=3.0)
    disco(capa, C, C + 1.8, 2.8, HUESO + (255,))
    caliz_tres_hojas(capa, C, 27.5, 13.5, 7.0, VERDE, VERDE_OSCURO, alto_tallo=4.5)
    return Image.alpha_composite(img, capa)


PROPUESTAS = [
    ("0-actual", "Actual (1.2.0)", p0_actual),
    ("1-anterior", "Anterior (1.1.0)", p1_anterior),
    ("A-bisel", "A · Bisel de reloj", pA_bisel),
    ("B-arco", "B · Arco de 25 min", pB_arco),
    ("C-desde-arriba", "C · Visto desde arriba", pC_desde_arriba),
    ("D-se-vacia", "D · El tomate que se vacía", pD_se_vacia),
    ("E-cocina", "E · El 25 escrito", pE_cocina),
    ("F-dial-desnudo", "F · Dial desnudo", pF_dial_desnudo),
    ("G-placa-hueso", "G · Placa de hueso", pG_placa_hueso),
]


# ── Máscaras del launcher y presentación ────────────────────────────────────────────────────────────
def mascara_circulo(lado):
    m = Image.new("L", (lado * 4, lado * 4), 0)
    ImageDraw.Draw(m).ellipse([0, 0, lado * 4 - 1, lado * 4 - 1], fill=255)
    return m.resize((lado, lado), Image.LANCZOS)


def mascara_squircle(lado, n=4.0):
    """Squircle de One UI y Nova: superelipse |x|^n + |y|^n = 1. No es un rectángulo redondeado."""
    ejes = (np.arange(lado * 4) + 0.5) / (lado * 2) - 1.0
    xs, ys = np.meshgrid(ejes, ejes)
    dentro = (np.abs(xs) ** n + np.abs(ys) ** n) <= 1.0
    m = Image.fromarray((dentro * 255).astype(np.uint8), "L")
    return m.resize((lado, lado), Image.LANCZOS)


def recorta(img108, lado, squircle=False):
    """Recorta la ventana visible de 72 unidades y aplica la máscara. Así se ve en el escritorio."""
    v = int(round(u((LIENZO - VENTANA) / 2)))
    visible = img108.crop((v, v, PX - v, PX - v)).resize((lado, lado), Image.LANCZOS)
    m = mascara_squircle(lado) if squircle else mascara_circulo(lado)
    salida = Image.new("RGBA", (lado, lado), (0, 0, 0, 0))
    salida.paste(visible, (0, 0), m)
    return salida


def tablero(color, ancho, alto):
    """Fondo de pantalla de mentira: un degradado suave, que es lo peor para un icono con placa."""
    a, b = color
    t = np.linspace(0, 1, alto)[:, None, None]
    rgb = np.array(a) + (np.array(b) - np.array(a)) * t
    return Image.fromarray(
        np.repeat(rgb.astype(np.uint8), ancho, axis=1), "RGB"
    ).convert("RGBA")


FONDOS = [
    ("oscuro", ((10, 10, 12), (28, 26, 30))),
    ("claro", ((236, 232, 226), (208, 214, 220))),
]


def lamina_propuesta(titulo, fabrica):
    """
    Una lámina por propuesta: círculo y squircle a 216 px, más la tira a 48 px, sobre los dos fondos.
    """
    img108 = fabrica()
    grande, chico = 216, 48
    margen, hueco = 28, 24
    ancho = margen * 2 + grande * 2 + hueco
    alto = margen * 2 + 34 + (grande + 18 + chico + 14) * 2 + hueco

    lamina = Image.new("RGBA", (ancho, alto), (24, 22, 22, 255))
    d = ImageDraw.Draw(lamina)
    f_tit = ImageFont.truetype(os.path.join(FUENTES, "inter_variable.ttf"), 21)
    f_pie = ImageFont.truetype(os.path.join(FUENTES, "inter_variable.ttf"), 13)
    d.text((margen, margen - 6), titulo, font=f_tit, fill=(245, 242, 239, 255))

    y = margen + 34
    for etiqueta, color in FONDOS:
        for i, squircle in enumerate((False, True)):
            x = margen + i * (grande + hueco)
            lamina.paste(tablero(color, grande, grande), (x, y))
            icono = recorta(img108, grande, squircle=squircle)
            lamina.paste(icono, (x, y), icono)
            d.text(
                (x, y + grande + 4),
                ("círculo · Pixel" if not squircle else "squircle · One UI") + f" · {etiqueta}",
                font=f_pie,
                fill=(168, 160, 155, 255),
            )
        # Tira a tamaño real, que es donde se decide si el icono funciona.
        yt = y + grande + 18 + 4
        tira_ancho = grande * 2 + hueco
        lamina.paste(tablero(color, tira_ancho, chico + 10), (margen, yt))
        for k in range(5):
            icono = recorta(img108, chico, squircle=(k % 2 == 1))
            lamina.paste(icono, (margen + 10 + k * (chico + 22), yt + 5), icono)
        d.text((margen, yt + chico + 12), f"48 px · {etiqueta}", font=f_pie, fill=(168, 160, 155, 255))
        y = yt + chico + 14 + hueco

    return lamina.convert("RGB")


def comparativa():
    """Todas las propuestas en una rejilla, que es como se van a comparar de verdad."""
    lado, chico = 168, 48
    margen, hueco = 26, 20
    cols = len(PROPUESTAS)
    ancho = margen * 2 + cols * lado + (cols - 1) * hueco
    alto = margen * 2 + 30 + (lado + 16 + chico + 12) * 2

    lamina = Image.new("RGBA", (ancho, alto), (24, 22, 22, 255))
    d = ImageDraw.Draw(lamina)
    f_tit = ImageFont.truetype(os.path.join(FUENTES, "inter_variable.ttf"), 22)
    f_pie = ImageFont.truetype(os.path.join(FUENTES, "inter_variable.ttf"), 13)
    d.text((margen, margen - 8), "Propuestas de icono · 1.3", font=f_tit, fill=(245, 242, 239, 255))

    renders = [(t, f()) for _, t, f in PROPUESTAS]
    y = margen + 30
    for etiqueta, color in FONDOS:
        for i, (titulo, img108) in enumerate(renders):
            x = margen + i * (lado + hueco)
            lamina.paste(tablero(color, lado, lado), (x, y))
            icono = recorta(img108, lado)
            lamina.paste(icono, (x, y), icono)
            if etiqueta == "oscuro":
                d.text((x, y - 18), titulo, font=f_pie, fill=(168, 160, 155, 255))
            # A tamaño real, debajo.
            lamina.paste(tablero(color, lado, chico + 10), (x, y + lado + 16))
            pequeno = recorta(img108, chico)
            lamina.paste(pequeno, (x + (lado - chico) // 2, y + lado + 21), pequeno)
        y += lado + 16 + chico + 12
    return lamina.convert("RGB")


def main():
    for slug, titulo, fabrica in PROPUESTAS:
        ruta = os.path.join(SALIDA, f"{slug}.png")
        lamina_propuesta(titulo, fabrica).save(ruta, "PNG")
        print(f"  {os.path.relpath(ruta, RAIZ)}")
    ruta = os.path.join(SALIDA, "comparativa.png")
    comparativa().save(ruta, "PNG")
    print(f"  {os.path.relpath(ruta, RAIZ)}")


if __name__ == "__main__":
    main()
