#!/usr/bin/env python3
"""
Revisa el juego de capturas antes de subirlo a Play.

    python3 revisar.py [directorio-de-capturas]

Existe porque **una captura mala no da ningún error**: se genera, se guarda y se sube. Los dos fallos que
motivaron este fichero pasaron los dos por delante de una revisión a ojo sin que nadie los viera:

- una captura **completamente negra**, porque la escena esperaba un texto que ya estaba en la pantalla
  anterior y disparó antes de que la nueva compusiera;
- las estadísticas **a cero** en tablet, porque la siembra dejaba el `-wal` de la sesión anterior.

Comprueba cuatro cosas:

1. **Dimensiones exactas** por formato, que es lo que Play valida al subir.
2. **`es` y `en` no pueden ser idénticas.** Si lo son, una tanda se capturó en el idioma de la otra — el
   fallo del primer juego de capturas, y el motivo de que este pipeline exista.
3. **Ninguna escena a medio pintar.** Se compara cada captura con su gemela en el otro idioma en vez de
   contra un umbral fijo: la misma escena ocupa lo mismo en los dos idiomas, así que si una cae a la mitad
   de la otra, esa está incompleta. Un umbral fijo no sirve, porque en tablet el contenido ocupa
   proporcionalmente menos y una pantalla correcta baja del 2 % de tinta sin que le pase nada.
4. **Las seis escenas, en los dos idiomas y los tres formatos**: 36 ficheros, ni uno menos.

El juego vive en `capturas/<idioma>/<formato>/`, con el idioma primero: la ficha de Play se rellena idioma
a idioma, así que interesa tener juntos los tres formatos de un mismo idioma.
"""
import glob
import hashlib
import os
import sys
from fractions import Fraction

import numpy as np
from PIL import Image

FORMATOS = {
    "telefono": (1080, 2400),
    "tablet-7-pulgadas": (1080, 1920),
    "tablet-10-pulgadas": (1440, 2560),
}
IDIOMAS = ("es", "en")
ESCENAS = 6

# Por debajo de esta fracción de la gemela, la captura está a medio componer.
PROPORCION_MINIMA = 0.5


def tinta(imagen):
    """Fracción de píxeles que no son casi negros. La app es negra, así que esto mide el contenido."""
    a = np.array(imagen.convert("RGB"))
    return float((a.max(axis=2) > 40).mean())


def revisar(raiz):
    fallos, total = [], 0

    for formato, tam in FORMATOS.items():
        aspecto = Fraction(*tam).limit_denominator(100)
        print(f"\n{formato}  {tam[0]}×{tam[1]}  ({aspecto})")

        nombres = sorted(
            os.path.basename(p) for p in glob.glob(os.path.join(raiz, "es", formato, "*.png"))
        )
        if len(nombres) != ESCENAS:
            fallos.append(f"{formato}: {len(nombres)} escenas, se esperaban {ESCENAS}")

        for escena in nombres:
            medidas, firmas = {}, {}
            for idioma in IDIOMAS:
                ruta = os.path.join(raiz, idioma, formato, escena)
                if not os.path.exists(ruta):
                    fallos.append(f"{ruta}: no existe")
                    continue
                total += 1
                imagen = Image.open(ruta)
                if imagen.size != tam:
                    fallos.append(f"{ruta}: mide {imagen.size}, se esperaba {tam}")
                medidas[idioma] = tinta(imagen)
                firmas[idioma] = hashlib.sha1(
                    np.array(imagen.convert("RGB")).tobytes()
                ).hexdigest()

            if len(medidas) < 2:
                continue
            if firmas["es"] == firmas["en"]:
                fallos.append(f"{formato}/{escena}: es y en son idénticas — un idioma se coló")

            peor, mejor = min(medidas.values()), max(medidas.values())
            if mejor > 0 and peor < mejor * PROPORCION_MINIMA:
                fallos.append(
                    f"{formato}/{escena}: es {medidas['es']:.1%} frente a en {medidas['en']:.1%} — "
                    "una de las dos está a medio pintar"
                )
            print(f"   {escena:34} es {medidas['es']:5.1%}   en {medidas['en']:5.1%}")

    esperadas = len(FORMATOS) * len(IDIOMAS) * ESCENAS
    print(f"\n{total} capturas revisadas de {esperadas} esperadas")
    if fallos:
        print("FALLOS:")
        for f in fallos:
            print(f"  · {f}")
        return False
    print("todo correcto")
    return True


if __name__ == "__main__":
    raiz = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "capturas"
    )
    raise SystemExit(0 if revisar(raiz) else 1)
