#!/usr/bin/env python3
"""
Una tanda de capturas de la app: mismas escenas, un dispositivo y un idioma.

    python3 tanda.py <directorio> <es|en>

Las escenas van en el orden que pidió Jorge: el pomodoro en marcha primero, luego el descanso, luego los
ajustes, luego el historial, y el widget después. La del widget no está aquí porque hay que arrastrarlo
en el launcher; la hace `widget.py`.

Todo se navega buscando el texto en pantalla, nunca por coordenadas fijas: el mismo script tiene que
valer para el teléfono y para las dos tablets, que tienen resoluciones distintas.
"""
import os
import subprocess
import sys
import time

import estado
import ui

PKG = "com.jjrapps.aquihaytomate"

TEXTOS = {
    "es": {"temporizador": "TEMPORIZADOR", "estadisticas": "ESTADÍSTICAS", "ajustes": "AJUSTES",
           "idioma": "Idioma", "eleccion": "Español",
           "pausar": "PAUSAR", "descanso": "DESCANSO", "duraciones": "DURACIONES",
           "hoy": "HOY", "racha": "mejor racha", "fin": "¡TIEMPO!", "descartar": "Descartar"},
    # «fin» es el texto completo a propósito: en inglés, «TIME» es subcadena de «TIMER», el nombre de la
    # pestaña, así que esperar por «TIME» se cumple sin que la pantalla de fin haya llegado.
    "en": {"temporizador": "TIMER", "estadisticas": "STATISTICS", "ajustes": "SETTINGS",
           "idioma": "Language", "eleccion": "English",
           "pausar": "PAUSE", "descanso": "BREAK", "duraciones": "DURATIONS",
           "hoy": "TODAY", "racha": "best streak", "fin": "TIME'S UP!", "descartar": "Dismiss"},
}


def adb(*args):
    return ui.adb(*args)


def abrir_app():
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    time.sleep(2.5)


def reiniciar_app():
    adb("shell", "am", "force-stop", PKG)
    time.sleep(1)
    abrir_app()


def alto_pantalla():
    tamano = adb("shell", "wm", "size")
    return int(tamano.strip().split("x")[-1])


def deslizar_arriba(veces=3):
    """Sube al principio de una lista larga, para que la captura no salga a media pantalla."""
    alto = alto_pantalla()
    for _ in range(veces):
        adb("shell", "input", "swipe", "540", str(int(alto * 0.35)), "540", str(int(alto * 0.85)), "250")
        time.sleep(0.6)


def deslizar_abajo(veces=1):
    alto = alto_pantalla()
    for _ in range(veces):
        adb("shell", "input", "swipe", "540", str(int(alto * 0.80)), "540", str(int(alto * 0.30)), "250")
        time.sleep(0.6)


def poner_idioma(idioma):
    """Por la pantalla de Ajustes, que es el camino real: la app impone su ajuste al arrancar."""
    t = TEXTOS[idioma]
    otro = TEXTOS["en" if idioma == "es" else "es"]
    abrir_app()
    for etiqueta in (t["ajustes"], otro["ajustes"]):
        if ui.buscar(etiqueta):
            ui.tocar(etiqueta)
            break
    time.sleep(1.5)
    for _ in range(6):                      # el idioma está al final de una lista larga
        if ui.buscar(t["idioma"]) or ui.buscar(otro["idioma"]):
            break
        deslizar_abajo()
    etiqueta = t["idioma"] if ui.buscar(t["idioma"]) else otro["idioma"]
    ui.tocar(etiqueta)
    time.sleep(1.5)
    ui.tocar(t["eleccion"])
    time.sleep(2.5)
    print(f"  idioma → {idioma}")


def esperar(texto, limite=20, exacto=False):
    """
    Espera a que el texto esté en pantalla antes de capturar.

    Con un `sleep` fijo salían capturas en negro: tras un `force-stop` la pantalla del temporizador tarda
    en componer y el cronómetro aún no está. Esperar por contenido es además lo único que aguanta el
    cambio de dispositivo, porque una tablet no tarda lo mismo que un teléfono.

    **Y esperar por una subcadena no es esperar.** La escena del descanso aguardaba «DESCANSO», que ya
    está en la pantalla de enfoque dentro de «SIGUIENTE: DESCANSO · 5 MIN»: la espera se cumplía sola y la
    captura salía negra en la tablet de 10, que es la que más tarda en componer. Con [exacto] solo cuenta
    el nodo cuyo texto es ese y nada más — la etiqueta de fase—. Es la misma trampa que ya obligó a usar
    `exacto` con «SIGUIENTE» en el onboarding.
    """
    fin = time.time() + limite
    while time.time() < fin:
        if ui.buscar(texto, exacto=exacto):
            time.sleep(0.4)          # un pelín más: que acabe de asentarse el primer fotograma
            return True
        time.sleep(0.7)
    raise SystemExit(f"no apareció en pantalla: {texto!r}")


def escena(nombre, destino, esperando=None, reposo=0.0, exacto=False):
    if esperando:
        esperar(esperando, exacto=exacto)
    if reposo:
        time.sleep(reposo)
    ruta = os.path.join(destino, nombre + ".png")
    ui.captura(ruta)
    print(f"  {nombre}.png")


def tanda(destino, idioma):
    t = TEXTOS[idioma]
    os.makedirs(destino, exist_ok=True)
    poner_idioma(idioma)

    # 1 · El pomodoro en marcha. Es la pantalla principal y va primero.
    estado.escribir("enfoque")
    abrir_app()
    escena("01-pomodoro-en-marcha", destino, esperando=t["pausar"])

    # 2 · El descanso, con su color ámbar y el cáliz.
    estado.escribir("descanso")
    abrir_app()
    escena("02-descanso", destino, esperando=t["descanso"], exacto=True)

    # 3 · Ajustes, desde arriba: duraciones y comportamiento.
    ui.tocar(t["ajustes"])
    time.sleep(1.5)
    deslizar_arriba()
    escena("03-ajustes", destino, esperando=t["duraciones"])

    # 4 · El historial. En el teléfono ya entra ahí el mapa del mes completo, y en una tablet entra todo,
    # así que una segunda captura con el listado desplazado no aportaba nada: sale la misma pantalla.
    ui.tocar(t["estadisticas"])
    time.sleep(2)
    deslizar_arriba()
    escena("04-historial", destino, esperando=t["hoy"])

    # 5 · Fin de intervalo: el tomate vacío, «¡Tiempo!» y la invitación a seguir.
    #
    # Al reconciliar, la app lanza su aviso y la notificación asoma un par de segundos por arriba. Salía en
    # unas capturas y no en otras según lo que tardara el volcado de la pantalla, así que se espera a que
    # se retire: entre una captura bonita a veces y una captura igual siempre, la segunda.
    ui.tocar(t["temporizador"])
    time.sleep(1)
    estado.escribir("fin")
    abrir_app()
    escena("05-fin-del-intervalo", destino, esperando=t["fin"], reposo=6, exacto=True)

    # Volver al temporizador, que es donde conviene dejar la app.
    ui.tocar(t["temporizador"])
    time.sleep(1)


if __name__ == "__main__":
    tanda(sys.argv[1], sys.argv[2])
