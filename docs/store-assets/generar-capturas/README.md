# Generar las capturas de la ficha de Play

Automatiza el juego completo de capturas: **seis escenas × dos idiomas × tres formatos = 36 imágenes**,
en `docs/store-assets/capturas/`.

Está aquí versionado porque estas capturas hay que rehacerlas cada vez que cambie una pantalla, y hacerlas
a mano son 36 secuencias de navegación con el riesgo de que se cuele una en el idioma equivocado —que es
exactamente lo que pasó con el primer juego—.

## Uso

Con un emulador arrancado y la app instalada:

```bash
python3 sembrar_historial.py siembra          # una vez: la base de datos con historial
python3 capturar.py <destino>                 # las doce capturas del dispositivo conectado
python3 recolocar.py <destino>                # opcional: centra el widget y rehace su captura
```

`capturar.py --sin-widget` salta la colocación del widget, para cuando ya está puesto en el escritorio.

Los tres emuladores usados, creados a mano en `~/.android/avd` (no hay `avdmanager` instalado), con las
resoluciones que Play exige:

| AVD | Resolución | Densidad | En dp | Aspecto |
|---|---|---|---|---|
| `Medium_Phone` | 1080 × 2400 | 420 | 411 × 914 | 9:20 |
| `Tablet7` | 1080 × 1920 | 288 | 600 × 1067 | **9:16** |
| `Tablet10` | 1440 × 2560 | 288 | 800 × 1422 | **9:16** |

Play pide **9:16 exacto** para las capturas de tablet, y lados de 320–3840 px en la de 7" y de
1080–7680 px en la de 10". Las densidades de 288 dpi no son casualidad: dejan la tablet pequeña en 600 dp
de ancho y la grande en 800 dp, que son los dos umbrales con los que Android decide que algo es una tablet.

## Lo que hace y por qué

- **`sembrar_historial.py`** — 45 días de sesiones en un `aquihaytomate.db` que se copia con `run-as`. Con
  una instalación nueva, Estadísticas sale vacía. Copia el `identityHash` del esquema exportado, sin el
  cual Room se niega a abrir la base.
- **`estado.py`** — escribe `timer_state.preferences_pb` a mano (protobuf de DataStore Preferences) para
  fijar la escena: enfoque al 70 % con 17:30, descanso al 67 % con 03:20, y el fin de intervalo. La
  alternativa era esperar de verdad ocho minutos por escena y dispositivo, más de una hora en total.
- **`ui.py`** — localiza los elementos por texto con `uiautomator dump` y toca su centro. Tocar por
  coordenadas falla la mitad de las veces y además no sobrevive al cambio de resolución.
- **`tanda.py`** — las cinco escenas de la app en un idioma. El idioma se pone por la pantalla de Ajustes,
  no con `cmd locale set-app-locales`: la app aplica su propio ajuste al arrancar y sobreescribe el del
  sistema.
- **`capturar.py`** — orquesta las dos tandas y coloca el widget arrastrándolo, que es la única forma: no
  hay orden de `adb` que fije un widget en el escritorio.

## Detalles que costaron una iteración cada uno

- **Esperar por contenido, nunca por tiempo.** Con un `sleep` fijo, las dos primeras capturas salían en
  negro: tras un `force-stop` la pantalla del temporizador tarda en componer.
- **`am force-stop` deja el widget en blanco**, con el marcador de carga del launcher. Hay que reabrir la
  app para que el colector republique sus `RemoteViews` antes de ir al escritorio.
- **El widget va en la segunda página del escritorio.** En la primera, el launcher pinta «At a glance» con
  la fecha en el idioma del sistema, que no se puede cambiar sin root —y la app de Ajustes del emulador se
  cierra al buscar idiomas—. En la segunda no aparece, y el escritorio queda limpio.
- **El long press necesita que el dedo se quede quieto de verdad.** `input swipe` con origen y destino
  iguales no arrastra: hay que usar `input motionevent` con esperas dentro del dispositivo.
- **La bandeja de widgets tiene dos formas.** En el teléfono la fila de la app se despliega en el sitio; en
  una tablet es un panel doble y la vista previa aparece a la derecha.
