#!/usr/bin/env python3
"""
Deja el widget en un sitio presentable del escritorio y vuelve a capturar la escena.

    python3 recolocar.py <formato>

`<formato>` es el mismo que espera `capturar.py`: la captura se rehace en `capturas/<idioma>/<formato>/`.

La posición en la que cae al arrastrarlo depende del launcher y de la rejilla, y en una tablet quedaba
pegado al borde superior. La captura del widget es la que más importa de todo el juego —«el widget es el
producto», §1 de CLAUDE.md—, así que merece quedar centrada.
"""
import os
import sys
import time

import capturar
import estado
import tanda
import ui


def alto_pantalla():
    return int(ui.adb("shell", "wm", "size").strip().split(":")[-1].split("x")[1])


def centrar():
    ui.adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(2)
    ancho, alto = capturar.ancho_pantalla(), alto_pantalla()
    destino_x, destino_y = ancho // 2, int(alto * 0.34)

    # El widget vive en la segunda página: hay que pasar de la primera, que es donde cae el HOME.
    puesto = ui.buscar("Pomodoro")
    for _ in range(3):
        if puesto:
            break
        ui.adb("shell", "input", "swipe", str(int(ancho * 0.8)), str(alto // 2),
               str(int(ancho * 0.2)), str(alto // 2), "300")
        time.sleep(1.5)
        puesto = ui.buscar("Pomodoro")
    if not puesto:
        raise SystemExit("no encuentro el widget en ninguna página del escritorio")
    px, py = puesto[0]["centro"]
    ui.adb("shell", f"input motionevent DOWN {px} {py}; sleep 1.2; "
                    f"input motionevent MOVE {px} {py - 10}; sleep 0.3; "
                    f"input motionevent MOVE {(px + destino_x) // 2} {(py + destino_y) // 2}; sleep 0.3; "
                    f"input motionevent MOVE {destino_x} {destino_y}; sleep 0.6; "
                    f"input motionevent UP {destino_x} {destino_y}")
    time.sleep(3)
    ui.adb("shell", "input", "tap", str(ancho // 2), str(int(alto * 0.8)))   # quitar el marco
    time.sleep(1)
    donde = ui.buscar("Pomodoro")
    print(f"widget en {donde[0]['caja'] if donde else '¿?'}")


if __name__ == "__main__":
    formato = os.path.basename(os.path.normpath(sys.argv[1]))
    centrar()
    for idioma in ("es", "en"):
        # El idioma es un ajuste persistido de la app, no del sistema, y quien lo dejó puesto fue la
        # última tanda que corrió aquí. Sin fijarlo, recolocar contra la carpeta «es» se queda esperando
        # un «PAUSAR» que no va a aparecer porque la app sigue en inglés.
        tanda.abrir_app()
        tanda.poner_idioma(idioma)
        capturar.capturar_widget(capturar.destino_de(idioma, formato), idioma)
