#!/usr/bin/env python3
"""
Utilidad de automatización para las capturas de la tienda.

Localiza los elementos por texto o por descripción con `uiautomator dump` y luego toca su centro. Tocar
a ciegas con coordenadas falla la mitad de las veces —está anotado en docs/estado-del-proyecto.md— y
además se rompe en cuanto cambia la resolución, que es justo lo que hacemos aquí: teléfono y dos tablets.

    python3 ui.py cap <ruta.png>          captura la pantalla
    python3 ui.py dump                    volca el árbol de la UI (texto y descripciones)
    python3 ui.py tap <texto>             toca el elemento cuyo texto o descripción coincida
    python3 ui.py wait <texto> [segundos]  espera a que aparezca
"""
import os
import re
import subprocess
import sys
import time

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
SERIE = os.environ.get("SERIE_ADB")


def adb(*args, binario=False):
    orden = [ADB] + (["-s", SERIE] if SERIE else []) + list(args)
    r = subprocess.run(orden, capture_output=True)
    if binario:
        return r.stdout
    return r.stdout.decode("utf-8", "replace")


def captura(ruta):
    datos = adb("exec-out", "screencap", "-p", binario=True)
    if len(datos) < 1000:
        raise RuntimeError(f"captura vacía ({len(datos)} bytes)")
    with open(ruta, "wb") as f:
        f.write(datos)
    return len(datos)


def arbol():
    """El XML de la jerarquía. Se reintenta: uiautomator falla si la UI está animando."""
    for _ in range(4):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("shell", "cat", "/sdcard/ui.xml")
        if "<hierarchy" in xml:
            return xml
        time.sleep(1)
    raise RuntimeError("uiautomator no devolvió la jerarquía")


NODO = re.compile(r'<node[^>]*?>')


def nodos(xml):
    fuera = []
    for n in NODO.findall(xml):
        attr = dict(re.findall(r'(\w+(?:-\w+)?)="([^"]*)"', n))
        caja = attr.get("bounds", "")
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", caja)
        if not m:
            continue
        x1, y1, x2, y2 = map(int, m.groups())
        fuera.append({
            "texto": attr.get("text", ""),
            "desc": attr.get("content-desc", ""),
            "clase": attr.get("class", ""),
            "clicable": attr.get("clickable", "false"),
            "centro": ((x1 + x2) // 2, (y1 + y2) // 2),
            "caja": (x1, y1, x2, y2),
        })
    return fuera


def buscar(aguja, xml=None):
    xml = xml or arbol()
    aguja_b = aguja.lower()
    exactos, parciales = [], []
    for n in nodos(xml):
        for campo in (n["texto"], n["desc"]):
            if not campo:
                continue
            if campo.lower() == aguja_b:
                exactos.append(n)
            elif aguja_b in campo.lower():
                parciales.append(n)
    return (exactos + parciales)[:1]


def tocar(aguja):
    halladas = buscar(aguja)
    if not halladas:
        raise SystemExit(f"no encontrado: {aguja!r}")
    x, y = halladas[0]["centro"]
    adb("shell", "input", "tap", str(x), str(y))
    return x, y


if __name__ == "__main__":
    orden = sys.argv[1]
    if orden == "cap":
        print(captura(sys.argv[2]), "bytes")
    elif orden == "dump":
        filtro = sys.argv[2].lower() if len(sys.argv) > 2 else ""
        for n in nodos(arbol()):
            etiqueta = n["texto"] or n["desc"]
            if etiqueta and (not filtro or filtro in etiqueta.lower()):
                print(f'{etiqueta[:58]:60} {n["caja"]} clicable={n["clicable"]}')
    elif orden == "tap":
        print("tocado en", tocar(sys.argv[2]))
    elif orden == "wait":
        limite = float(sys.argv[3]) if len(sys.argv) > 3 else 15
        fin = time.time() + limite
        while time.time() < fin:
            if buscar(sys.argv[2]):
                print("visible"); raise SystemExit(0)
            time.sleep(1)
        raise SystemExit(f"no apareció: {sys.argv[2]!r}")
