# F9 — Verificación en emulador

Fecha: 2026-07-28 · Emulador: `Medium_Phone` AVD, API 37, imagen con Google Play (sin root).

Lo que se pudo comprobar sin dispositivo físico, y lo que **sigue pendiente**. Este documento no
sustituye la checklist de §10 de `CLAUDE.md`: la cierra en parte y deja el resto anotado con el motivo.

---

## 1. R8 — un bug real encontrado y corregido

**El único hallazgo grave de la fase.** El build de release compilaba y arrancaba sin errores, y aun así
estaba corrupto.

R8 renombraba las constantes de los enums del dominio:

```
com.jjrapps.aquihaytomate.domain.model.SlotType -> yn1:
    SlotType FOCUS       -> e
    SlotType SHORT_BREAK -> f
    SlotType LONG_BREAK  -> g
```

`SlotType` y `TimerStatus` se persisten **por nombre**, en `focus_session.slot_type` y en el DataStore
del temporizador. En release, por tanto, se estaba guardando `"e"` en la base de datos.

Nada falla a simple vista, porque una build es consistente consigo misma. Rompe en la **siguiente**
versión: R8 reparte las letras según cómo quede el grafo de clases, `"e"` pasa a significar otra cosa y
todo el historial del usuario queda mal interpretado — `fromName("e")` devuelve `FOCUS` por defecto.

La regla que había era la receta habitual, y es insuficiente:

```proguard
# Mantiene los dos métodos, pero deja que R8 renombre las constantes.
-keepclassmembers enum … { values(); valueOf(); }
```

Corregido en `app/proguard-rules.pro` añadiendo `<fields>`, más reglas para las clases de Room y para el
provider del widget, que el launcher instancia por nombre.

**Verificado en el artefacto, no solo en el mapping:**

```
$ strings classes.dex | grep -x FOCUS
FOCUS          ← presentes los siete nombres: FOCUS, SHORT_BREAK, LONG_BREAK,
               ← IDLE, RUNNING, PAUSED, RINGING
```

| Comprobación | Resultado |
|---|---|
| `assembleRelease` con `isMinifyEnabled` y `isShrinkResources` | correcto |
| Tamaño del APK release | **3,7 MB** (debug: 36 MB) |
| Nombres de enum en `classes.dex` | los 7 intactos |
| Arranque de la app minificada | sin `FATAL` ni `AndroidRuntime`; Hilt y Room sobreviven |

> El APK release se firmó **con la clave de debug y solo dentro del scratchpad** para poder instalarlo.
> El proyecto sigue sin `keystore.properties`, y el `assembleRelease` del repo produce un APK sin firmar.

---

## 2. Tests instrumentados

`connectedDebugAndroidTest`: **22 tests, todos en verde**. Cubren el DAO —incluida la idempotencia del
índice `UNIQUE(session_id, slot_index)`—, el schema exportado de Room y las guardas del manifest.

---

## 3. Checklist de resiliencia (§10)

| # | Caso | Estado |
|---|---|---|
| 1 | Pantalla apagada 25 min → suena en el segundo correcto | ⏳ **pendiente**: requiere esperar el slot completo en un dispositivo real |
| 2 | Swipe desde recientes con el timer corriendo | ⚠️ **no probado de verdad** — ver abajo |
| 3 | Reinicio a mitad de slot | ⏳ **pendiente** |
| 4 | Cambiar la hora +1 h a mitad | ⛔ **imposible en este AVD**: la imagen con Google Play no admite `adb root` ni `cmd time_detector`. Cubierto por tests unitarios |
| 5 | `SCHEDULE_EXACT_ALARM` denegado | ✅ **verificado** |
| 6 | Sin permiso de notificaciones | ⏳ pendiente |
| 7 | «Restringir uso de batería» | ⏳ pendiente |

### Lo que sí quedó verificado

Con el temporizador en marcha (`dumpsys`):

```
ServiceRecord{… .timer.PomodoroTimerService}
  isForeground=true foregroundId=1 types=0x40000000        ← FOREGROUND_SERVICE_TYPE_SPECIAL_USE
  foregroundNoti=Notification(channel=timer_running
      flags=ONGOING_EVENT|ONLY_ALERT_ONCE|FOREGROUND_SERVICE|SILENT
      color=0xffe23b2b category=stopwatch actions=2)

ELAPSED_WAKEUP #12: tag=*walarm*:com.jjrapps.aquihaytomate.SLOT_DEADLINE
  origWhen=+24m55s
```

- El servicio corre con **el único tipo permitido**, `specialUse`, y su notificación es muda, ongoing y
  de categoría cronómetro, con las dos acciones.
- La alarma de respaldo se arma con `ELAPSED_REALTIME_WAKEUP` al mismo deadline.
- El estado persistido guarda `RUNNING` y `FOCUS` **por su nombre completo** — la comprobación en vivo de
  que la corrección de R8 funciona.

**Caso 5, degradación por permiso denegado.** La alarma aparece como
`window=+18m44s maxWhenElapsed=+43m40s`: es **inexacta**, porque `SCHEDULE_EXACT_ALARM` viene denegado por
defecto. Es exactamente el nivel 2 de degradación del ADR 002 y el comportamiento que se buscaba: el FGS
sigue siendo el mecanismo primario y el slot se cumple igual.

### Caso 2, con honestidad

`adb shell am kill` **no llegó a matar el proceso**: el pid siguió siendo el mismo, porque un servicio en
foreground lo protege del kill. Así que lo que se comprobó es que el proceso resiste, no que el estado se
repare tras morir.

Al volver a la app, el temporizador seguía coherente (23:34, `PAUSE`, `reset` visible) y el servicio vivo,
pero eso no prueba `reconcile()`. **El caso sigue pendiente** y necesita un swipe real desde recientes en
un dispositivo, que es lo que en la mayoría de OEM sí mata el proceso.

---

## 4. Lo que falta para cerrar F9

- **Checklist de §10 en dispositivo real**, en Android 12, 14 y 16. Los casos 1, 2, 3, 6 y 7 no son
  automatizables desde aquí; el 4 necesita una imagen sin Google Play o un dispositivo.
- **Checklist del widget** en Nova, Pixel Launcher y One UI.
- **Consumo de batería** con Battery Historian (ADR 002 estima ~0,3 % por pomodoro de 25 min).
- **Prueba en un OEM agresivo** (Xiaomi, Samsung, Oppo).
- **`keystore.properties` y la subida** a pruebas internas, con la declaración de FGS `specialUse` que ya
  está redactada en `docs/play-store-publication-texts.md` §14.
