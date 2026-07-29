#!/usr/bin/env python3
"""
Construye una base de datos con historial para que Estadísticas no salga vacía en las capturas.

    python3 sembrar_historial.py [directorio-de-salida]   # por defecto, junto a este script

Con una instalación nueva la pantalla de estadísticas está en blanco y no se puede enseñar en una tienda.
Aquí se generan 45 días de sesiones con forma creíble: sin fines de semana casi nunca, algún día laborable
en blanco, algún pomodoro interrumpido —que la app también guarda, con `completed = 0`— y el día de hoy a
medias, que es lo normal a media tarde.

**Room valida la base al abrirla**: si `room_master_table` no lleva el `identityHash` del esquema
compilado, lanza `IllegalStateException` y la app no arranca. El hash se lee de
`app/schemas/…AppDatabase/1.json`, que es la fuente de verdad del esquema, así que al cambiar la base de
datos esto sigue cuadrando sin tocar nada.
"""
import json
import os
import random
import sqlite3
import sys
from datetime import date, datetime, timedelta, timezone

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(AQUI)))   # …/docs/store-assets/generar-capturas
ESQUEMA = os.path.join(RAIZ, "app", "schemas",
                       "com.jjrapps.aquihaytomate.data.local.db.AppDatabase", "1.json")
ZONA = "Europe/Madrid"
DESPLAZAMIENTO = timezone(timedelta(hours=2))     # verano en Madrid; solo afecta a las horas locales


def identity_hash():
    with open(ESQUEMA, encoding="utf-8") as f:
        return json.load(f)["database"]["identityHash"]


def sembrar(destino, hoy=None, dias=45, semilla=7):
    ruta = os.path.join(destino, "aquihaytomate.db")
    if os.path.exists(ruta):
        os.remove(ruta)
    hoy = hoy or date.today()

    con = sqlite3.connect(ruta)
    con.executescript("""
    CREATE TABLE IF NOT EXISTS `focus_session` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      `session_id` INTEGER NOT NULL, `slot_index` INTEGER NOT NULL, `slot_type` TEXT NOT NULL,
      `started_at_epoch_ms` INTEGER NOT NULL, `ended_at_epoch_ms` INTEGER NOT NULL,
      `planned_duration_ms` INTEGER NOT NULL, `actual_focus_ms` INTEGER NOT NULL,
      `completed` INTEGER NOT NULL, `timezone_id` TEXT NOT NULL, `local_date` TEXT NOT NULL);
    CREATE INDEX IF NOT EXISTS `index_focus_session_local_date` ON `focus_session` (`local_date`);
    CREATE INDEX IF NOT EXISTS `index_focus_session_started_at_epoch_ms`
      ON `focus_session` (`started_at_epoch_ms`);
    CREATE UNIQUE INDEX IF NOT EXISTS `index_focus_session_session_id_slot_index`
      ON `focus_session` (`session_id`, `slot_index`);
    CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT);
    CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT);
    """)
    con.execute("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                (identity_hash(),))
    con.execute("INSERT INTO android_metadata (locale) VALUES ('en_US')")
    con.execute("PRAGMA user_version = 1")

    random.seed(semilla)          # mismo historial en cada ejecución: las capturas son comparables
    filas = []
    for atras in range(dias - 1, -1, -1):
        dia = hoy - timedelta(days=atras)
        finde = dia.weekday() >= 5
        if finde and random.random() < 0.75:
            continue
        if random.random() < 0.1:
            continue
        objetivo = 5 if atras == 0 else (3 if finde else random.choice([5, 6, 7, 8, 8, 9, 10]))
        inicio = datetime(dia.year, dia.month, dia.day, 9, 10, tzinfo=DESPLAZAMIENTO)
        for i in range(objetivo):
            completo = random.random() > 0.12
            plan = 25 * 60_000
            real = plan if completo else random.randint(9, 21) * 60_000
            fin = inicio + timedelta(milliseconds=real)
            filas.append((
                int(inicio.timestamp() * 1000), i, "FOCUS",
                int(inicio.timestamp() * 1000), int(fin.timestamp() * 1000),
                plan, real, 1 if completo else 0, ZONA, dia.isoformat(),
            ))
            inicio = fin + timedelta(minutes=5 if (i + 1) % 4 else 15)

    con.executemany(
        "INSERT INTO focus_session (session_id, slot_index, slot_type, started_at_epoch_ms, "
        "ended_at_epoch_ms, planned_duration_ms, actual_focus_ms, completed, timezone_id, local_date) "
        "VALUES (?,?,?,?,?,?,?,?,?,?)", filas)
    con.commit()

    hechos = con.execute("SELECT COUNT(*) FROM focus_session WHERE completed = 1").fetchone()[0]
    jornadas = con.execute("SELECT COUNT(DISTINCT local_date) FROM focus_session").fetchone()[0]
    con.close()
    print(f"{ruta}: {len(filas)} slots en {jornadas} días, {hechos} completados")
    return ruta


if __name__ == "__main__":
    destino = sys.argv[1] if len(sys.argv) > 1 else AQUI
    os.makedirs(destino, exist_ok=True)
    sembrar(destino)
