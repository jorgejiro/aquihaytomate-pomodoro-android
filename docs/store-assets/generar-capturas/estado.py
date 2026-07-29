#!/usr/bin/env python3
"""
Fija el estado del temporizador escribiendo `timer_state.preferences_pb` directamente.

Por qué en vez de usar la UI: para que el tomate salga a media asta con una cifra creíble haría falta
esperar de verdad siete u ocho minutos con el pomodoro corriendo, y hay que repetir la escena en tres
dispositivos y dos idiomas — más de una hora de espera. Escribiendo el estado, la escena es exacta y
reproducible: enfoque al 70 % con 17:30, descanso al 67 % con 03:20.

El fichero es un `PreferenceMap` de DataStore Preferences, protobuf sencillo:

    PreferenceMap { map<string, Value> preferences = 1; }
    Value { bool=1; float=2; int32=3; int64=4; string=5; StringSet=6; double=7; bytes=8; }

Las claves y sus tipos son el contrato de `TimerStateDataSource.Keys`, que el widget y el servicio ya
comparten; si allí cambian, esto deja de cuadrar y hay que actualizarlo.

    python3 estado.py enfoque|descanso
"""
import os
import struct
import subprocess
import sys

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
SERIE = os.environ.get("SERIE_ADB")
PKG = "com.jjrapps.aquihaytomate"


def adb(*args, binario=False):
    orden = [ADB] + (["-s", SERIE] if SERIE else []) + list(args)
    r = subprocess.run(orden, capture_output=True)
    return r.stdout if binario else r.stdout.decode("utf-8", "replace").strip()


# ── Codificación protobuf, lo justo ──────────────────────────────────────────────────────────────────
def varint(n):
    if n < 0:                       # int64 negativo: complemento a dos en 64 bits, 10 bytes de varint
        n += 1 << 64
    fuera = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        fuera.append(b | (0x80 if n else 0))
        if not n:
            return bytes(fuera)


def campo_bytes(numero, datos):
    return bytes([(numero << 3) | 2]) + varint(len(datos)) + datos


def campo_varint(numero, valor):
    return bytes([(numero << 3) | 0]) + varint(valor)


def valor_string(s):
    return campo_bytes(5, s.encode("utf-8"))


def valor_long(v):
    return campo_varint(4, v)


def valor_int(v):
    return campo_varint(3, v)


def preferencias(entradas):
    """`entradas` es una lista de (clave, bytes-de-Value) en orden."""
    fuera = b""
    for clave, valor in entradas:
        entrada = campo_bytes(1, clave.encode("utf-8")) + campo_bytes(2, valor)
        fuera += campo_bytes(1, entrada)
    return fuera


# ── Las dos escenas ─────────────────────────────────────────────────────────────────────────────────
def reloj_del_dispositivo():
    epoch_ms = int(adb("shell", "date", "+%s%3N"))
    uptime_ms = int(float(adb("shell", "cat", "/proc/uptime").split()[0]) * 1000)
    return epoch_ms, uptime_ms


ESCENAS = {
    # (slot, duración en minutos, restante en segundos, pomodoros ya completados del ciclo, estado)
    "enfoque":  ("FOCUS", 25, 17 * 60 + 30, 2, "RUNNING"),
    "descanso": ("SHORT_BREAK", 5, 3 * 60 + 20, 2, "RUNNING"),
    # Pomodoro recién terminado: el tomate vacío, «¡Tiempo!» y la invitación a empezar el descanso. Es la
    # escena que cuenta que la app avisa, y en una tablet alta evita repetir la de estadísticas.
    "fin":      ("FOCUS", 25, 0, 3, "RINGING"),
}


def escribir(escena):
    slot, minutos, restante_s, hechos, situacion = ESCENAS[escena]
    epoch_ms, uptime_ms = reloj_del_dispositivo()
    duracion_ms = minutos * 60_000
    restante_ms = restante_s * 1000
    transcurrido_ms = duracion_ms - restante_ms

    entradas = [
        ("status", valor_string(situacion)),
        ("slot_type", valor_string(slot)),
        ("session_id", valor_long(epoch_ms - transcurrido_ms)),
        ("slot_index", valor_int(hechos)),
        ("completed_focus_in_cycle", valor_int(hechos)),
        ("pomodoros_per_cycle", valor_int(4)),
        ("slot_duration_ms", valor_long(duracion_ms)),
        ("slot_started_at_epoch_ms", valor_long(epoch_ms - transcurrido_ms)),
        ("end_at_epoch_ms", valor_long(epoch_ms + restante_ms)),
        # El monotónico es el que manda para el descuento; el de época solo se usa para detectar
        # reinicios. `end_elapsed - duración` queda por debajo del uptime actual, así que TimerMath no
        # cree que el móvil se ha reiniciado y no salta el slot.
        ("end_at_elapsed_realtime_ms", valor_long(uptime_ms + restante_ms)),
        ("boot_epoch_ms", valor_long(epoch_ms - uptime_ms)),
        ("remaining_at_pause_ms", valor_long(0)),
    ]
    datos = preferencias(entradas)

    ruta_local = "/tmp/timer_state.preferences_pb"
    with open(ruta_local, "wb") as f:
        f.write(datos)

    # DataStore cachea el fichero en memoria mientras vive el proceso: hay que matar la app antes.
    adb("shell", "am", "force-stop", PKG)
    adb("push", ruta_local, "/data/local/tmp/ts.pb")
    adb("shell", f"run-as {PKG} mkdir -p files/datastore")
    adb("shell", f"run-as {PKG} sh -c 'cat /data/local/tmp/ts.pb > "
                 f"files/datastore/timer_state.preferences_pb'")
    print(f"{escena}: {situacion} {slot} {minutos} min, quedan "
          f"{restante_s // 60:02d}:{restante_s % 60:02d}, tomate al "
          f"{100 * restante_ms // duracion_ms} % ({len(datos)} bytes)")


if __name__ == "__main__":
    escribir(sys.argv[1])
