#!/usr/bin/env python3
"""
Genera los dos assets gráficos que pide Google Play y que no son capturas de pantalla:

    icono-play-512.png                  512 × 512   icono de la ficha
    grafico-de-funciones-1024x500.png  1024 × 500   «feature graphic»

    python3 docs/store-assets/generar-assets.py

Está aquí, versionado, por una razón concreta: **los dos assets se derivan del código de la app**, no
son un diseño paralelo hecho a ojo en un editor. El icono reproduce los `pathData` de
`res/drawable/ic_launcher_*.xml` y el gráfico de funciones usa la misma onda que
`domain/render/TomatoGeometry.kt` y los colores de `ui/theme/Color.kt`. Si el icono o la paleta
cambian, se actualiza aquí y se vuelve a ejecutar, en vez de repintarlos a mano y que divergan.

Sin dependencias más allá de Pillow. Todo se dibuja con supermuestreo y se reduce con Lanczos, que es
lo que da los bordes limpios sin tener un rasterizador de verdad.
"""

import math
import os
import re
from PIL import Image, ImageDraw, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SALIDA = os.path.join(RAIZ, "docs", "store-assets")
FUENTES = os.path.join(RAIZ, "app", "src", "main", "res", "font")

# ── Paleta, copiada de ui/theme/Color.kt ────────────────────────────────────────────────────────────
NEGRO = (0, 0, 0)
TOMATE_BRIGHT = (0xFF, 0x44, 0x33)
TOMATE_FILL = (0xE2, 0x3B, 0x2B)
TOMATE_DEEP = (0x7A, 0x20, 0x18)
TOMATE_GHOST = (0x1C, 0x0D, 0x0B)
TEXTO_PRIMARIO = (0xF5, 0xF2, 0xEF)
TEXTO_SECUNDARIO = (0xA8, 0xA0, 0x9B)
TEXTO_SOBRE_LIQUIDO = (0, 0, 0)
BRILLO_SUPERFICIE = (0xFF, 0xFF, 0xFF, 0x99)

SUPERMUESTREO = 4


# ── Utilidades ──────────────────────────────────────────────────────────────────────────────────────
def fuente(archivo, tamano, peso):
    """Carga una de las fuentes variables de la app y fija el eje de peso."""
    f = ImageFont.truetype(os.path.join(FUENTES, archivo), tamano)
    try:
        f.set_variation_by_axes([peso])
    except Exception:
        pass  # Sin soporte de fuentes variables se usa el peso por defecto: no vale la pena abortar.
    return f


def grotesk(tamano, peso=500):
    return fuente("space_grotesk_variable.ttf", tamano, peso)


def inter(tamano, peso=600):
    # El primer eje de Inter es el tamaño óptico, así que hay que darle los dos valores.
    f = ImageFont.truetype(os.path.join(FUENTES, "inter_variable.ttf"), tamano)
    try:
        f.set_variation_by_axes([min(32, max(14, tamano / 4)), peso])
    except Exception:
        pass
    return f


def bezier_cubica(p0, p1, p2, p3, pasos=24):
    """Aplana una cúbica en puntos. Los `pathData` del icono son solo M, L, C y Z absolutos."""
    puntos = []
    for i in range(1, pasos + 1):
        t = i / pasos
        u = 1 - t
        x = u * u * u * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t * t * t * p3[0]
        y = u * u * u * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t * t * t * p3[1]
        puntos.append((x, y))
    return puntos


def puntos_de_path(d, escala):
    """
    Convierte un `pathData` de VectorDrawable en un polígono, ya escalado.

    Solo M, L, C y Z absolutos, que es todo lo que usan los paths del icono. Se separa con regex y no
    partiendo por espacios porque en `M54,35` la orden va pegada al número.
    """
    fichas = re.findall(r"[MLCZz]|-?\d*\.?\d+", d)
    puntos, actual, i = [], (0.0, 0.0), 0
    orden = None
    while i < len(fichas):
        if fichas[i] in ("M", "L", "C", "Z", "z"):
            orden = fichas[i]
            i += 1
            if orden in ("Z", "z"):
                continue
        if orden is None:
            raise ValueError(f"pathData sin orden inicial: {d[:40]}")
        n = {"M": 2, "L": 2, "C": 6}[orden]
        valores = [float(v) for v in fichas[i:i + n]]
        i += n
        if orden in ("M", "L"):
            actual = (valores[0], valores[1])
            puntos.append(actual)
        else:
            p1, p2, p3 = valores[0:2], valores[2:4], valores[4:6]
            puntos.extend(bezier_cubica(actual, p1, p2, p3))
            actual = (p3[0], p3[1])
    return [(x * escala, y * escala) for x, y in puntos]


def degradado_lineal(tamano, inicio, fin, color_inicio, color_fin):
    """Degradado lineal proyectando cada píxel sobre el eje inicio→fin."""
    ancho, alto = tamano
    img = Image.new("RGB", tamano)
    px = img.load()
    dx, dy = fin[0] - inicio[0], fin[1] - inicio[1]
    largo2 = dx * dx + dy * dy or 1.0
    for y in range(alto):
        for x in range(ancho):
            t = ((x - inicio[0]) * dx + (y - inicio[1]) * dy) / largo2
            t = 0.0 if t < 0 else 1.0 if t > 1 else t
            px[x, y] = (
                int(color_inicio[0] + (color_fin[0] - color_inicio[0]) * t),
                int(color_inicio[1] + (color_fin[1] - color_inicio[1]) * t),
                int(color_inicio[2] + (color_fin[2] - color_inicio[2]) * t),
            )
    return img


def degradado_radial(tamano, centro, radio, color_centro, color_borde):
    ancho, alto = tamano
    img = Image.new("RGB", tamano)
    px = img.load()
    for y in range(alto):
        for x in range(ancho):
            d = math.hypot(x - centro[0], y - centro[1]) / radio
            t = 0.0 if d < 0 else 1.0 if d > 1 else d
            px[x, y] = (
                int(color_centro[0] + (color_borde[0] - color_centro[0]) * t),
                int(color_centro[1] + (color_borde[1] - color_centro[1]) * t),
                int(color_centro[2] + (color_borde[2] - color_centro[2]) * t),
            )
    return img


# ── La onda, traducida de domain/render/TomatoGeometry.kt ────────────────────────────────────────────
PERIODOS_FRENTE = 1.25
PERIODOS_FONDO = 1.7
FRACCION_AMPLITUD = 0.022


def puntos_superficie(ancho, alto, fraccion, fase, periodos, amplitud=None):
    base = alto * (1 - min(1.0, max(0.0, fraccion)))
    amplitud = alto * FRACCION_AMPLITUD if amplitud is None else amplitud
    # Amortiguada por la distancia al borde más cercano: es lo que hace que un tomate lleno se lea
    # lleno en vez de con un valle excavado arriba.
    amplitud = max(0.0, min(amplitud, min(base, alto - base)))
    puntos = []
    pasos = max(1, int(ancho / 2))
    for i in range(pasos + 1):
        x = ancho if i == pasos else min(i * 2, ancho)
        y = base + amplitud * math.sin(fase + 2 * math.pi * periodos * (x / ancho))
        puntos.append((x, min(alto, max(0.0, y))))
    return puntos


def mascara_liquido(tamano, fraccion, fase, periodos):
    """Máscara del área bajo la onda: la superficie, y luego se cierra por abajo."""
    ancho, alto = tamano
    m = Image.new("L", tamano, 0)
    if fraccion <= 0:
        return m
    puntos = puntos_superficie(ancho, alto, fraccion, fase, periodos)
    ImageDraw.Draw(m).polygon(puntos + [(ancho, alto), (0, alto)], fill=255)
    return m


# ── Icono de Play ───────────────────────────────────────────────────────────────────────────────────
# Los `pathData` de res/drawable/ic_launcher_foreground.xml, literalmente.
CUERPO = ("M54,35 C68.5,35 80,46.5 80,61 C80,75.5 68.5,87 54,87 "
          "C39.5,87 28,75.5 28,61 C28,46.5 39.5,35 54,35 Z")
BRILLO = "M42,48 C45,44 50,42 54,43 C50,45 46,48 44,53 C42.5,52.5 41.5,50.5 42,48 Z"
TALLO = "M52.5,25 L55.5,25 L55.5,36 L52.5,36 Z"
CALIZ = "M54,32 L62,29 L69,32 L61,39 L54,40 L47,39 L39,32 L46,29 Z"


def icono_play(lado=512):
    """
    Reproduce el icono adaptativo y recorta la ventana visible.

    El arte del `foreground` vive en la zona segura de 66 dp del lienzo de 108, pero el launcher solo
    muestra los 72 dp centrales. Si se escalara el lienzo entero a 512, el tomate saldría un 33 % más
    pequeño que en el escritorio del móvil; recortando [18,90] el icono de la ficha se ve **igual** que
    el instalado, que es lo que se compara al mirar la lista de Play.
    """
    escala = lado * SUPERMUESTREO / 72.0   # 72 unidades de la ventana visible → lado final
    lienzo = int(round(108 * escala))

    img = degradado_radial(
        (lienzo, lienzo),
        centro=(54 * escala, 52 * escala),
        radio=66 * escala,
        color_centro=(0x1A, 0x0C, 0x0A),
        color_borde=(0x0A, 0x09, 0x08),
    ).convert("RGBA")

    # Cuerpo: su propio degradado lineal, recortado por la silueta.
    cuerpo = Image.new("L", (lienzo, lienzo), 0)
    ImageDraw.Draw(cuerpo).polygon(puntos_de_path(CUERPO, escala), fill=255)
    relleno = degradado_lineal(
        (lienzo, lienzo),
        inicio=(34 * escala, 41 * escala),
        fin=(76 * escala, 83 * escala),
        color_inicio=(0xFF, 0x44, 0x33),
        color_fin=(0xC4, 0x2D, 0x1F),
    )
    img.paste(relleno, (0, 0), cuerpo)

    capa = Image.new("RGBA", (lienzo, lienzo), (0, 0, 0, 0))
    dibujo = ImageDraw.Draw(capa)
    dibujo.polygon(puntos_de_path(TALLO, escala), fill=(0x3F, 0x6B, 0x2E, 255))
    dibujo.polygon(puntos_de_path(CALIZ, escala), fill=(0x4A, 0x7A, 0x3B, 255))
    img = Image.alpha_composite(img, capa)

    brillo = Image.new("RGBA", (lienzo, lienzo), (0, 0, 0, 0))
    ImageDraw.Draw(brillo).polygon(puntos_de_path(BRILLO, escala), fill=(255, 255, 255, 56))
    img = Image.alpha_composite(img, brillo)

    ventana = int(round(18 * escala))
    img = img.crop((ventana, ventana, lienzo - ventana, lienzo - ventana))
    img = img.resize((lado, lado), Image.LANCZOS)
    # Play pide «PNG de 32 bits», es decir con canal alfa, pero **opaco**: aplica su propia máscara
    # redondeada, y un píxel translúcido en una esquina se vería como una muesca. Así que se conserva el
    # canal y se fuerza a 255, en vez de convertir a RGB.
    img.putalpha(255)
    return img


# ── Gráfico de funciones ────────────────────────────────────────────────────────────────────────────
def mascara_cifra(s, cifra, fuente_cifra):
    """La cifra centrada en un lienzo de lado [s], como máscara, más la caja real de su tinta."""
    texto = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(texto)
    caja = d.textbbox((0, 0), cifra, font=fuente_cifra)
    d.text(((s - (caja[2] - caja[0])) / 2 - caja[0], (s - (caja[3] - caja[1])) / 2 - caja[1]),
           cifra, font=fuente_cifra, fill=255)
    return texto, texto.getbbox()


def tomate_liquido(lado, fraccion, cifra, fuente_cifra):
    """
    El tomate de la pantalla principal: hueco, dos ondas, la línea de la superficie, el contorno de lo
    ya vaciado y la cifra en negativo —clara sobre el hueco, negra sobre el líquido—.

    La cifra se centra midiendo la tinta ya dibujada y no las métricas de la fuente: `textbbox` cuenta
    desde el ascender, y centrar por él deja los dígitos visiblemente altos dentro del círculo.
    """
    s = lado * SUPERMUESTREO
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))

    texto, _ = mascara_cifra(s, cifra, fuente_cifra)

    circulo = Image.new("L", (s, s), 0)
    ImageDraw.Draw(circulo).ellipse((0, 0, s - 1, s - 1), fill=255)

    img.paste(Image.new("RGB", (s, s), TOMATE_GHOST), (0, 0), circulo)

    # Onda de fondo al 35 %: dos periodos que no se dividen entre sí, para que no se lea como un bucle.
    fondo = mascara_liquido((s, s), fraccion, fase=2.1, periodos=PERIODOS_FONDO)
    fondo = Image.composite(fondo, Image.new("L", (s, s), 0), circulo).point(lambda v: int(v * 0.35))
    img.paste(Image.new("RGB", (s, s), TOMATE_FILL), (0, 0), fondo)

    frente = mascara_liquido((s, s), fraccion, fase=0.0, periodos=PERIODOS_FRENTE)
    frente = Image.composite(frente, Image.new("L", (s, s), 0), circulo)
    img.paste(Image.new("RGB", (s, s), TOMATE_FILL), (0, 0), frente)

    # Línea de brillo donde el líquido toca el aire.
    if 0 < fraccion < 1:
        linea = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        ImageDraw.Draw(linea).line(
            puntos_superficie(s, s, fraccion, 0.0, PERIODOS_FRENTE),
            fill=BRILLO_SUPERFICIE,
            width=max(1, int(s * 0.0055)),
        )
        img = Image.alpha_composite(img, Image.composite(
            linea, Image.new("RGBA", (s, s), (0, 0, 0, 0)), circulo))

    if fraccion < 1:
        grosor = max(2, int(s * 0.0075))
        ImageDraw.Draw(img).ellipse(
            (grosor / 2, grosor / 2, s - 1 - grosor / 2, s - 1 - grosor / 2),
            outline=TOMATE_DEEP + (255,), width=grosor,
        )

    # La cifra: una sola máscara pintada dos veces, para que las dos mitades encajen al píxel.
    img.paste(Image.new("RGB", (s, s), TEXTO_PRIMARIO), (0, 0),
              Image.composite(texto, Image.new("L", (s, s), 0), ImageChops_invert(frente)))
    img.paste(Image.new("RGB", (s, s), TEXTO_SOBRE_LIQUIDO), (0, 0),
              Image.composite(texto, Image.new("L", (s, s), 0), frente))

    return img.resize((lado, lado), Image.LANCZOS)


def ImageChops_invert(mascara):
    return mascara.point(lambda v: 255 - v)


def ancho_de(d, texto, f, tracking=0.0):
    if tracking == 0.0:
        caja = d.textbbox((0, 0), texto, font=f)
        return caja[2] - caja[0]
    return sum(d.textlength(c, font=f) + tracking for c in texto) - tracking


def grafico_de_funciones(ancho=1024, alto=500):
    """
    El «feature graphic», tal como lo especifica docs/design-spec.md §9: fondo negro a sangre, tomate de
    560 px centrado en (300, 330) —cortado por el borde inferior—, relleno al 62 % con la superficie
    ondulada y `18:42` en negativo sobre el líquido; a la derecha, en x = 620, el nombre en Inter
    SemiBold 68 px, el subtítulo en Inter Regular y una barrita de 96 × 4 px en `TomateFill`.

    **Una desviación del spec, medida y deliberada**: el nombre va en dos líneas. En x = 620, con la zona
    segura de 40 px, quedan 364 px, y «¡Aquí hay tomate!» en Inter SemiBold 68 px ocupa 573. El §9 se
    escribió sin medirlo con la fuente real. De las tres formas de cuadrarlo —encoger el título, encoger
    el tomate o partir el nombre— partirlo es la que no toca ninguna de las cifras del spec, y el nombre
    ya se parte solo en dos mitades naturales. El subtítulo, por lo mismo, baja de 30 px a 26.
    """
    MARGEN_SEGURO = 40
    img = Image.new("RGB", (ancho, alto), NEGRO)
    d = ImageDraw.Draw(img)

    diametro, centro = 560, (300, 330)
    tomate = tomate_liquido(
        diametro,
        fraccion=0.62,
        cifra="18:42",
        fuente_cifra=grotesk(112 * SUPERMUESTREO, peso=500),
    )
    # Pillow recorta solo lo que sobresale, que es justamente el «cortado por abajo».
    img.paste(tomate, (centro[0] - diametro // 2, centro[1] - diametro // 2), tomate)

    x = 620
    disponible = ancho - MARGEN_SEGURO - x

    titulo = inter(68, peso=600)
    subtitulo = inter(26, peso=400)
    lineas = [
        (titulo, "¡Aquí hay", TEXTO_PRIMARIO, 150),
        (titulo, "tomate!", TEXTO_PRIMARIO, 225),
        (subtitulo, "Pomodoro de una sola casilla", TEXTO_SECUNDARIO, 318),
    ]

    for f, texto, color, y in lineas:
        caja = d.textbbox((0, 0), texto, font=f)
        if caja[2] - caja[0] > disponible:      # Que no se salga en silencio si cambia el copy.
            raise ValueError(f"«{texto}» ocupa {caja[2] - caja[0]} px y solo hay {disponible}")
        d.text((x, y - caja[1]), texto, font=f, fill=color)   # `y` es el borde superior de la tinta.

    d.rectangle((x, 378, x + 96, 378 + 4), fill=TOMATE_FILL)
    return img


def comprobar(ruta, tamano, modo, maximo_bytes):
    """Los requisitos que Play valida al subir el asset, comprobados aquí para no descubrirlos allí."""
    img = Image.open(ruta)
    bytes_ = os.path.getsize(ruta)
    fallos = []
    if img.size != tamano:
        fallos.append(f"mide {img.size}, se esperaba {tamano}")
    if img.mode != modo:
        fallos.append(f"modo {img.mode}, se esperaba {modo}")
    if bytes_ > maximo_bytes:
        fallos.append(f"pesa {bytes_ / 1024:.0f} KiB, el máximo es {maximo_bytes / 1024:.0f} KiB")
    if img.mode == "RGBA" and img.getchannel("A").getextrema() != (255, 255):
        fallos.append("tiene píxeles translúcidos, y Play aplica su propia máscara")
    estado = "✔" if not fallos else "✗ " + "; ".join(fallos)
    print(f"{os.path.relpath(ruta, RAIZ)}  {img.size[0]}×{img.size[1]}  {img.mode}  "
          f"{bytes_ / 1024:.0f} KiB  {estado}")
    return not fallos


if __name__ == "__main__":
    icono = os.path.join(SALIDA, "icono-play-512.png")
    icono_play().save(icono, "PNG")

    grafico = os.path.join(SALIDA, "grafico-de-funciones-1024x500.png")
    grafico_de_funciones().save(grafico, "PNG")

    bien = comprobar(icono, (512, 512), "RGBA", 1024 * 1024)
    bien = comprobar(grafico, (1024, 500), "RGB", 15 * 1024 * 1024) and bien
    raise SystemExit(0 if bien else 1)
