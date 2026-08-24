#!/usr/bin/env python3
"""
Juego completo de capturas para la ficha de Play, en el dispositivo conectado.

    python3 capturar.py <formato> [--sin-widget]

`<formato>` es una de las claves de `PANTALLAS` —`telefono`, `tablet-7-pulgadas`, `tablet-10-pulgadas`—.
Las capturas van a `capturas/<idioma>/<formato>/`, **el idioma primero**, que es como Play pide los
recursos: la ficha se rellena idioma a idioma, así que lo que interesa tener junto en una carpeta son los
tres formatos de un mismo idioma.

Seis escenas por idioma, en el orden que pidió Jorge —el pomodoro primero, luego el descanso, la
configuración, el historial y el widget— en español y en inglés:

    01-pomodoro-en-marcha   05-historial-del-mes
    02-descanso             06-widget-en-el-escritorio
    03-ajustes
    04-historial

Tres cosas se preparan a propósito y no son cosmética:

- **El historial se siembra** con 45 días de sesiones (`siembra/`), porque con una instalación nueva la
  pantalla de estadísticas sale vacía y no se puede enseñar en una tienda.
- **El estado del temporizador se escribe a mano** (`estado.py`) para que el tomate salga a media asta con
  una cifra creíble sin esperar ocho minutos por escena y dispositivo.
- **El widget se coloca en la segunda página del escritorio.** En la primera, el launcher pinta «At a
  glance» con la fecha, que es texto del sistema y saldría en inglés en la ficha española; la segunda
  página no lo lleva y además queda el escritorio limpio, que es lo que se pidió.
"""
import os
import subprocess
import sys
import time

import estado
import tanda
import ui

# Las capturas se agrupan por idioma y dentro por formato, no al revés: subir la ficha de Play es
# rellenar un idioma cada vez.
CAPTURAS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "capturas")

PKG = "com.jjrapps.aquihaytomate"
LAUNCHER = "com.google.android.apps.nexuslauncher"

# La otra app del autor, que también trae widget. Si está instalada aparece en la bandeja junto a la
# nuestra y el arrastre puede agarrar su vista previa: en un pase salió el widget de Bebe Agua en la
# captura del teléfono. Se desinstala antes de colocar nada.
PKG_RIVAL = "com.jjrapps.bebeagua"


def adb(*args):
    return ui.adb(*args)


def densidad():
    return int(adb("shell", "wm", "density").strip().split(":")[-1])


def dp(valor):
    return int(valor * densidad() / 160)


def ancho_pantalla():
    return int(adb("shell", "wm", "size").strip().split(":")[-1].split("x")[0])


def alto_pantalla():
    return int(adb("shell", "wm", "size").strip().split(":")[-1].split("x")[1])


# Resolución y densidad que Play exige de cada formato. **No salen del AVD**:
# los tres `config.ini` dicen 2560×1600 @ 320, y los valores de verdad se fijan con `wm size` y
# `wm density`, que persisten en el emulador… pero no siempre los dos. En un pase la tablet de 7" había
# conservado la resolución y perdido la densidad, y a 320 dpi se queda en 540 dp de ancho en vez de 600,
# que es el umbral con el que Android decide que algo es una tablet: las capturas habrían salido con el
# layout de un teléfono grande sin que nada fallara. Se fijan aquí para no depender de eso.
PANTALLAS = {
    "telefono": (1080, 2400, 420),
    "tablet-7-pulgadas": (1080, 1920, 288),
    "tablet-10-pulgadas": (1440, 2560, 288),
}


def destino_de(idioma, formato):
    """`capturas/<idioma>/<formato>`: el idioma primero, como los recursos de la ficha de Play."""
    return os.path.join(CAPTURAS, idioma, formato)


def fijar_pantalla(formato):
    """Aplica la resolución y la densidad que Play exige del formato que toca."""
    medidas = PANTALLAS.get(formato)
    if not medidas:
        print(f"aviso: '{formato}' no está en PANTALLAS, se deja la pantalla como esté")
        return
    ancho, alto, dpi = medidas
    adb("shell", "wm", "size", f"{ancho}x{alto}")
    adb("shell", "wm", "density", str(dpi))
    time.sleep(5)
    real = adb("shell", "wm", "size").strip().split(":")[-1].strip()
    print(f"pantalla: {formato} → {ancho}x{alto} @ {dpi} dpi "
          f"({round(ancho / (dpi / 160))} dp de ancho), efectiva {real}")


def preparar():
    """Permisos, historial sembrado, sin animaciones y onboarding pasado."""
    adb("shell", "pm", "grant", PKG, "android.permission.POST_NOTIFICATIONS")
    adb("shell", "appops", "set", PKG, "SCHEDULE_EXACT_ALARM", "allow")
    for clave in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
        adb("shell", "settings", "put", "global", clave, "0")

    semilla = os.path.join(os.path.dirname(os.path.abspath(__file__)), "siembra", "aquihaytomate.db")
    adb("shell", "am", "force-stop", PKG)
    adb("push", semilla, "/data/local/tmp/seed.db")
    adb("shell", f"run-as {PKG} mkdir -p databases")
    adb("shell", f"run-as {PKG} sh -c 'cat /data/local/tmp/seed.db > databases/aquihaytomate.db'")
    # **Y fuera el WAL y el shm de la sesión anterior.** Sustituir el fichero principal de una base SQLite
    # sin borrarlos deja a Room aplicando por encima un diario que no corresponde: en la tablet de 7" el
    # `.db` era el sembrado y el `-wal` de siete horas antes, y la pantalla de Estadísticas salió a cero
    # —con la captura hecha y subida, sin que nada fallara—.
    adb("shell", f"run-as {PKG} sh -c 'rm -f databases/aquihaytomate.db-wal "
                 f"databases/aquihaytomate.db-shm'")
    print("preparado: permisos, historial sembrado (sin WAL viejo), animaciones apagadas")

    tanda.abrir_app()
    for _ in range(6):                       # el onboarding, si aparece, son cuatro páginas
        # Exacto: «SIGUIENTE» es el botón del onboarding y también el arranque de la línea
        # «SIGUIENTE: ENFOQUE · 25 MIN» del temporizador, que es lo que se ve si no hay onboarding.
        if ui.buscar("GET STARTED", exacto=True) or ui.buscar("EMPEZAR", exacto=True):
            ui.tocar("GET STARTED") if ui.buscar("GET STARTED", exacto=True) else ui.tocar("EMPEZAR")
            time.sleep(2)
            break
        if ui.buscar("NEXT", exacto=True) or ui.buscar("SIGUIENTE", exacto=True):
            ui.tocar("NEXT") if ui.buscar("NEXT", exacto=True) else ui.tocar("SIGUIENTE")
            time.sleep(1)
        else:
            break


def limpiar_escritorio():
    """
    Escritorio virgen antes de colocar el widget.

    `pm clear` del launcher es la única forma de quitar lo que dejó un pase anterior: no hay orden de
    `adb` que borre un widget, así que sin esto se acumulan —en la tablet grande salieron dos tomates en
    la misma captura— y encima quedan los widgets que el launcher trae de fábrica, como el de Calendar
    con su «Sign in», que no tienen nada que hacer en la ficha.
    """
    adb("shell", "pm", "uninstall", PKG_RIVAL)
    adb("shell", "pm", "clear", LAUNCHER)
    time.sleep(3)
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(5)
    print("escritorio limpio")


def buscar_en_bandeja():
    """
    Deja la bandeja de widgets mostrando solo nuestra app.

    Tres esperas por contenido, y cada una está por un fallo distinto de la tablet de 10":

    1. **La bandeja tarda en pintarse.** Con los tres segundos fijos de antes, el pase moría en
       «no encontrado: 'Search'». Y ojo: en esa tablet «Search» solo existe como `content-desc`.
    2. **El campo de búsqueda tarda en coger el foco**, así que un `input text` inmediato se pierde en el
       vacío y la lista se queda sin filtrar. Se espera al botón «Back», que es lo que aparece cuando el
       buscador ya está abierto de verdad, antes de teclear.
    3. **El filtrado tarda** bastante más que el tecleo, sobre todo con el emulador cargado.

    Y si el buscador no cumple, queda la red de seguridad: recorrer la lista a mano. Es más lento pero no
    depende de que el foco caiga donde debe.
    """
    ui.tocar("Search", limite=25)
    if not ui.esperar("Back", limite=10):
        time.sleep(1.5)
    adb("shell", "input", "text", "tomate")
    if ui.esperar("¡Aquí hay tomate!", limite=25):
        return

    print("  el buscador no filtró; recorriendo la bandeja")
    adb("shell", "input", "keyevent", "KEYCODE_ESCAPE")
    time.sleep(1.5)
    ancho, alto = ancho_pantalla(), alto_pantalla()
    for _ in range(12):
        if ui.buscar("¡Aquí hay tomate!"):
            return
        adb("shell", "input", "swipe", str(ancho // 2), str(int(alto * 0.7)),
            str(ancho // 2), str(int(alto * 0.35)), "400")
        time.sleep(1.2)
    raise SystemExit("el widget no aparece en la bandeja, ni buscando ni recorriéndola")


def colocar_widget():
    """
    Añade el widget de 1×1 y lo deja en la segunda página del escritorio.

    Todo por gestos, porque no hay forma de fijar un widget por línea de órdenes. El arrastre se hace con
    `motionevent` y esperas dentro del propio dispositivo: un `input swipe` no mantiene el dedo quieto el
    tiempo que el launcher necesita para entender que es un long-press y no un toque.
    """
    ancho = ancho_pantalla()
    centro_x = ancho // 2

    limpiar_escritorio()
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(2)
    # El long press tiene que caer en hueco libre del escritorio, y dónde está el hueco depende de lo que
    # traiga preinstalado cada launcher: en la tablet grande la zona de arriba está ocupada.
    alto = int(adb("shell", "wm", "size").strip().split(":")[-1].split("x")[1])
    for fraccion in (0.65, 0.45, 0.8, 0.3):
        y = int(alto * fraccion)
        adb("shell", "input", "swipe", str(centro_x), str(y), str(centro_x), str(y), "900")
        time.sleep(2)
        if ui.buscar("Widgets"):
            break
        adb("shell", "input", "keyevent", "KEYCODE_ESCAPE")
        time.sleep(1)
    ui.tocar("Widgets")
    buscar_en_bandeja()

    # Abrir la ficha de la app. En el teléfono la fila se despliega en el sitio; en una tablet la bandeja
    # tiene dos paneles y la vista previa aparece en el de la derecha. Tocar la fila vale para los dos, y
    # si no basta se prueba el chevron del extremo derecho.
    titulo = ui.esperar("¡Aquí hay tomate!", limite=20)
    if not titulo:
        raise SystemExit("el widget no aparece en la bandeja")
    ui.tocar("¡Aquí hay tomate!")
    time.sleep(2.5)

    etiqueta = ui.esperar("1 × 1", limite=8) or ui.buscar("1 x 1")
    if not etiqueta:
        adb("shell", "input", "tap", str(titulo[0]["caja"][2] + dp(30)),
            str(titulo[0]["centro"][1]))
        etiqueta = ui.esperar("1 × 1", limite=10) or ui.buscar("1 x 1")
    if not etiqueta:
        raise SystemExit("no encuentro la ficha del widget en la bandeja")
    x = etiqueta[0]["centro"][0]
    y = etiqueta[0]["caja"][1] - dp(70)

    # Se suelta a un tercio de la altura y centrado, en fracciones de la pantalla y no en dp fijos: los
    # dp anteriores caían dentro del escritorio en el teléfono pero pegados al borde superior en la
    # tablet de 7", donde el widget salía cortado por la barra de estado.
    destino_x, destino_y = centro_x, int(alto * 0.33)
    adb("shell", f"input motionevent DOWN {x} {y}; sleep 1.2; "
                 f"input motionevent MOVE {x + 2} {y - 2}; sleep 0.3; "
                 f"input motionevent MOVE {x} {y - dp(30)}; sleep 0.2; "
                 f"input motionevent MOVE {int(x * 0.8)} {y - dp(60)}; sleep 0.2; "
                 f"input motionevent MOVE {destino_x} {destino_y}; sleep 0.5; "
                 f"input motionevent UP {destino_x} {destino_y}")
    time.sleep(3)
    adb("shell", "input", "tap", str(centro_x), str(dp(700)))     # soltar el marco de redimensión
    time.sleep(1)

    # A la segunda página, para dejar atrás el «At a glance» del launcher.
    puesto = ui.buscar("Pomodoro")
    if puesto:
        px, py = puesto[0]["centro"]
        adb("shell", f"input motionevent DOWN {px} {py}; sleep 1.2; "
                     f"input motionevent MOVE {px + dp(15)} {py}; sleep 0.3; "
                     f"input motionevent MOVE {int(ancho * 0.5)} {py}; sleep 0.3; "
                     f"input motionevent MOVE {ancho - 20} {py}; sleep 1.5; "
                     f"input motionevent MOVE {ancho - 10} {py}; sleep 1.5; "
                     f"input motionevent MOVE {centro_x} {py}; sleep 0.8; "
                     f"input motionevent UP {centro_x} {py}")
        time.sleep(3)
    print("widget colocado en la segunda página del escritorio")


def capturar_widget(destino, idioma):
    """
    El escritorio con el widget vivo. Hay que reabrir la app: `force-stop` deja el widget en blanco.

    Se espera el control primario **del idioma de esta tanda**. Antes se probaba `ui.buscar("PAUSAR")` y,
    si no estaba, se esperaba `"PAUSE"`: esa comprobación corre nada más abrir la app, así que si la
    pantalla todavía no había compuesto —una carrera que se pierde de vez en cuando— daba por hecho que
    la app estaba en inglés y se quedaba esperando un texto que no iba a llegar nunca.
    """
    estado.escribir("enfoque")
    tanda.abrir_app()
    tanda.esperar(tanda.TEXTOS[idioma]["pausar"])
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(3)

    # HOME deja la primera página, y el widget vive en la segunda. Recién arrancado el emulador esto no se
    # puede dar por supuesto: sin ir a buscarlo, la captura sale del escritorio vacío.
    ancho = ancho_pantalla()
    alto = int(adb("shell", "wm", "size").strip().split(":")[-1].split("x")[1])
    for _ in range(3):
        if ui.buscar("Pomodoro"):
            break
        adb("shell", "input", "swipe", str(int(ancho * 0.8)), str(alto // 2),
            str(int(ancho * 0.2)), str(alto // 2), "300")
        time.sleep(1.5)
    else:
        raise SystemExit("el widget no está en ninguna página del escritorio")

    ui.captura(os.path.join(destino, "06-widget-en-el-escritorio.png"))
    print("  06-widget-en-el-escritorio.png")


if __name__ == "__main__":
    # Se admite tanto el nombre del formato como una ruta que acabe en él, para no tener que recordar
    # cuál de los dos espera el script.
    formato = os.path.basename(os.path.normpath(sys.argv[1]))
    fijar_pantalla(formato)
    preparar()
    if "--sin-widget" not in sys.argv:
        colocar_widget()

    for idioma in ("es", "en"):
        destino = destino_de(idioma, formato)
        print(f"── {idioma} ──")
        tanda.tanda(destino, idioma)
        capturar_widget(destino, idioma)
