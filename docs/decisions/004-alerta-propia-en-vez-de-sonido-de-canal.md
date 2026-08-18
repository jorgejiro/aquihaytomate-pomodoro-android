# 004 — La alerta la toca la app, no el canal de notificación

Fecha: 2026-07-27
Estado: aceptada

## Contexto

Al terminar un slot hay que avisar al usuario con sonido y vibración. Los requisitos de producto
son:

- **Sonido seleccionable** de un catálogo (`bell`, `bowl`, `digital`, `soft`) más silencio.
- **Vibración de duración configurable en segundos**, por defecto 5 s, ajustable de 0 a 30.

La vía canónica de Android es delegar en el `NotificationChannel`: se le asigna un `Uri` de sonido y
un patrón de vibración, y el sistema se encarga.

El problema es que **un `NotificationChannel` es inmutable después de creado**. Ni el sonido ni el
patrón de vibración se pueden cambiar; el usuario solo puede modificarlos desde los ajustes del
sistema. Para cambiarlos desde la app hay que borrar el canal y crear otro con id distinto, lo que
en la práctica significa ids versionados del tipo `alerts_bell_5s_v1` y una lógica de limpieza de
canales huérfanos. Y aun así la vibración quedaría limitada a un patrón fijo por canal.

## Decisión

1. **Los dos canales de notificación son mudos.** Tanto `timer_running` (IMPORTANCE_LOW) como
   `timer_alerts` (IMPORTANCE_HIGH) se crean con `setSound(null, null)` y `enableVibration(false)`.

2. **`AlertPlayer` reproduce el sonido y dispara la vibración directamente**, en paralelo a publicar
   la notificación.

3. **Respetar el modo silencio y No molestar pasa a ser responsabilidad nuestra**, y se resuelve con
   una función pura testeable:

   ```kotlin
   // domain/usecase/AlertPolicy.kt
   fun decideAlert(
       ringerMode: Int, interruptionFilter: Int, dndAllowsAlarms: Boolean,
       alarmVolume: Int, soundSelected: Boolean, vibrationSeconds: Int,
   ): AlertDecision
   ```

   Comportamiento: modo silencio → nada. Modo vibración → solo vibra. DND total
   (`INTERRUPTION_FILTER_NONE`) → nada. DND prioritario → según el usuario permita alarmas, que es
   lo que el sistema ya hace con los despertadores. Volumen de alarma a 0 → solo vibra.

4. **Sonido con `MediaPlayer` y `USAGE_ALARM`:**

   ```kotlin
   AudioAttributes.Builder()
       .setUsage(AudioAttributes.USAGE_ALARM)
       .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
       .build()
   ```

   `USAGE_ALARM` hace que use el **volumen de alarma**, que es el que el usuario espera de un
   temporizador, y no el de multimedia.

   > **Corregido el 2026-08-18.** La implementación pasaba estos atributos en un `setAudioAttributes`
   > *posterior* a `MediaPlayer.create(context, resid)`, que llama a `prepare()` por dentro. La doc del
   > framework es explícita: «In order for the target audio attributes type to become effective, this
   > method must be called before `prepare()`». El setter se aceptaba, no cambiaba de estado y no hacía
   > nada, así que **el clip salía por el volumen de multimedia** — justo lo que este punto quería
   > evitar. Los atributos van ahora en la factoría,
   > `MediaPlayer.create(context, resid, attributes, sessionId)`. Se pide `requestAudioFocus(GAIN_TRANSIENT_MAY_DUCK)` antes y
   se abandona en `onCompletion`, para bajar la música del usuario en vez de pisarla. Watchdog de
   10 s que libera el `MediaPlayer` pase lo que pase.

5. **Vibración con waveform finito**, nunca repetitivo:

   ```kotlin
   private const val PULSE_MS = 400L
   private const val GAP_MS   = 250L
   fun buildWaveform(totalMs: Long): LongArray   // [0, 400, 250, 400, 250, …]
   ```

   `VibrationEffect.createWaveform(timings, amplitudes, repeat = -1)` con
   `VibrationAttributes.USAGE_ALARM`. `vibrationSeconds = 0` → no se llama a `vibrate()` en absoluto.

## Alternativas descartadas

- **Sonido y vibración por canal, con ids versionados.** Es la vía canónica pero degenera en
  `alerts_bell_v1`, `alerts_bowl_v1`, … con lógica de creación y borrado de canales cada vez que el
  usuario cambia un ajuste. Y la vibración de duración arbitraria seguiría sin poder hacerse: el
  patrón del canal es fijo.

- **Un solo canal con sonido por defecto y dejar que el usuario lo cambie desde los ajustes del
  sistema.** Cumpliría la letra de las buenas prácticas de Android, pero convierte «elegir sonido»
  en un viaje a los ajustes del sistema, y la vibración configurable en segundos sería imposible.

- **`SoundPool` en vez de `MediaPlayer`.** `SoundPool` gana en latencia y en reproducciones
  repetidas, que es justo lo que aquí no importa: se reproduce un clip cada 25 minutos. `MediaPlayer`
  evita mantener el pool precargado en memoria.

- **`USAGE_NOTIFICATION`** en vez de `USAGE_ALARM`. Usaría el volumen de notificaciones, que suele
  estar más bajo, y quedaría bloqueado por DND en configuraciones donde las alarmas sí pasan. Un
  temporizador que el usuario ha iniciado a propósito se comporta como una alarma.

- **Vibración continua de N segundos** (`vibrate(5000)`). Cinco segundos continuos son desagradables
  y muchos motores LRA los atenúan hasta hacerlos imperceptibles. Pulsos de 400 ms con huecos de
  250 ms se perciben mejor y suman la misma duración.

- **`VibrationEffect` con `repeat = 0` y un `cancel()` programado.** Si el proceso muere entre el
  `vibrate()` y el `cancel()`, **el móvil se queda vibrando indefinidamente**. Es un fallo
  catastrófico de cara al usuario y hay apps en Play que lo han sufrido. Waveform finito y explícito,
  siempre.

- **`USE_FULL_SCREEN_INTENT`** para que la alerta tome la pantalla con el móvil bloqueado. Android 14
  lo restringe a llamadas y alarmas de despertador, y Play lo revisa. Es desproporcionado para un
  pomodoro: basta con `IMPORTANCE_HIGH` y heads-up.

## Consecuencias

- **Asumimos la responsabilidad de respetar silencio y DND**, que normalmente delegas en el sistema.
  Se contiene poniendo toda la decisión en `AlertPolicy`, una función pura con un test que cubre la
  matriz completa de `ringerMode` × `interruptionFilter` × volumen × ajustes (12+ casos).

- **El usuario no puede silenciar la alerta desde los ajustes del canal en Android**, porque el canal
  ya es mudo: tiene que hacerlo desde los Ajustes de la app. Es un compromiso consciente. Los canales
  siguen existiendo y siendo controlables (importancia, badge, pantalla de bloqueo); lo único que no
  hacen es sonar.

- **La vibración se dispara desde el servicio si está vivo, y si no desde el receiver dentro del
  `goAsync()`.** Como `vibrate()` es asíncrona y la ejecuta el sistema, `pendingResult.finish()` no
  la corta.

- Hay que probar manualmente con silencio, modo vibración, DND total y DND prioritario. Está en la
  checklist de la fase F5.
