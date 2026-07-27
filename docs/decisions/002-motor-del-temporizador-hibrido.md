# 002 — Motor del temporizador híbrido en tres capas

Fecha: 2026-07-27
Estado: aceptada

## Contexto

El estado del temporizador lo consumen cuatro entrypoints —la UI de la app, el widget de escritorio,
la notificación persistente y el propio servicio— y tiene que sobrevivir a que el sistema mate el
proceso, a que el usuario haga swipe desde recientes y a un reinicio del dispositivo.

Además debe **disparar en el segundo correcto** con la pantalla apagada. Un temporizador que se
retrasa dos minutos no es un temporizador.

El entorno de plataforma es hostil:

- `SCHEDULE_EXACT_ALARM` viene **denegado por defecto** desde Android 14 con `targetSdk ≥ 33`.
- No podemos declarar `USE_EXACT_ALARM`: Play lo reserva a despertadores y calendarios.
- En Doze, `setExactAndAllowWhileIdle` se limita a **una alarma cada 9 minutos** por app.
- Android 15 congela los procesos en caché.
- Android 12+ prohíbe arrancar un foreground service desde background salvo exenciones.
- Muchos OEM (Xiaomi, Samsung, Oppo) matan foreground services de apps de terceros.

## Decisión

Tres capas, con **el estado persistido como única fuente de verdad**:

```
CAPA 1 · VERDAD    DataStore `timer_state.preferences_pb`
                   endAtEpochMs + endAtElapsedRealtimeMs + bootEpochMs + status + slot + ciclo
                   → Flow<TimerState>. NADIE hace tick sobre esto.

CAPA 2 · MOTOR     PomodoroTimerService (FGS specialUse, stopWithTask=false)
                   Vive solo mientras status == RUNNING.
                   PARTIAL_WAKE_LOCK acotado + una corrutina delay(→ deadline).
                   Mecanismo PRIMARIO. Cero permisos de usuario.

CAPA 3 · RED       AlarmManager ELAPSED_REALTIME_WAKEUP al mismo deadline.
                   Exacta si hay permiso, inexacta (setAndAllowWhileIdle) si no.
                   + BootReceiver + reconcile() al abrir la app o tocar el widget.
```

Detalles que hacen que funcione:

1. **Doble deadline.** Se guardan `endAtEpochMs`, `endAtElapsedRealtimeMs` y `bootEpochMs`. Se usa
   el monotónico `elapsedRealtime` —inmune a cambios de reloj y que avanza en suspensión— salvo que
   `abs(bootEpochNow - bootEpochMs)` supere la tolerancia, lo que indica reinicio o cambio manual de
   la hora; entonces se cae al epoch. Cambiar la hora del sistema no hace saltar el pomodoro.

2. **Wakelock acotado.** Un FGS **no** mantiene la CPU despierta: el proceso vive, pero el
   dispositivo suspende y `delay()` se despierta tarde. El servicio adquiere un `PARTIAL_WAKE_LOCK`
   con `acquire(remainingMs + 5_000)` y lo libera en `finally`. El timeout es obligatorio: un
   wakelock atascado más de una hora aparece en Play Vitals.

3. **Idempotencia por base de datos, no por memoria.** El índice
   `UNIQUE(session_id, slot_index)` en `focus_session` hace que la carrera servicio↔alarma no pueda
   duplicar pomodoros: ambos caminos insertan con `OnConflictStrategy.IGNORE` y el segundo devuelve
   `-1L`. Sin banderas, sin locks.

4. **Regla anti-encadenado.** `reconcile()` **nunca simula más de un slot vencido**, aunque
   `autoStartNext` esté activo. Si el móvil estuvo apagado 8 horas, se registra el slot que venció,
   se pasa a `IDLE` y se acabó. Si venció hace más de 30 minutos ni siquiera se alerta.

5. **Arranque del FGS solo desde exenciones legítimas**: la Activity, una acción de notificación, un
   toque en el widget o una alarma exacta. Cuando caemos al fallback inexacto no hay exención, así
   que ese camino completa el slot desde el receiver con `goAsync()` y no arranca el servicio. Todo
   `startForegroundService` va en `try/catch (ForegroundServiceStartNotAllowedException)` con
   degradación a «solo estado + alarma».

6. **`START_NOT_STICKY`** y `startForeground()` en la primera línea de `onStartCommand`, antes de
   leer nada de DataStore (hay 5 segundos antes de `ForegroundServiceDidNotStartInTimeException`).

7. **Al pausar se detiene el servicio.** No tiene sentido quemar wakelock y notificación foreground
   con el reloj parado; se publica una notificación normal con «Reanudar», que se puede repostear
   sin restricciones.

8. **Tras reinicio no se relanza el servicio automáticamente.** `BootReceiver` reconcilia el estado
   y rearma la alarma; el servicio revive al abrir la app o pulsar la notificación. Android 15
   prohíbe arrancar desde `BOOT_COMPLETED` los tipos `dataSync`, `mediaPlayback`, `camera`,
   `microphone`, `phoneCall` y `mediaProjection` —`specialUse` sigue permitido—, pero relanzarlo
   solo por si acaso gasta batería sin ganar nada.

## Alternativas descartadas

- **Solo ForegroundService, con el estado en un `StateFlow` del servicio.** El estado se evapora con
  el proceso: swipe desde recientes (que en la mayoría de OEM mata el proceso pase lo que pase,
  `stopWithTask` o no), low-memory kill, reinicio, «Restringir uso de batería». Además el widget
  tendría que hacer `bindService` o quedarse en blanco cuando no hay temporizador, y la UI tendría
  una carrera en el arranque. **Un servicio no puede ser la verdad de algo que debe sobrevivirle.**

- **Solo estado persistido + `AlarmManager`, sin servicio.** Dos motivos fatales:
  (a) con `SCHEDULE_EXACT_ALARM` denegado por defecto en Android 14, la app está rota en la primera
  ejecución hasta que el usuario navegue a «Alarmas y recordatorios» en los ajustes del sistema;
  (b) en Doze, la cuota de una alarma exacta cada 9 minutos puede retrasar un descanso de 5 minutos
  hasta 4 minutos. Sin FGS, además, el receiver tiene ~10 s de `goAsync()` para reproducir sonido,
  vibrar 5 s, escribir en Room y reprogramar: es apurado y frágil con el proceso congelado.

- **`WorkManager`.** No garantiza puntualidad ni de lejos; está diseñado para trabajo diferible. Es
  exactamente lo contrario de lo que necesita un temporizador.

- **`CountDownTimer` en el ViewModel.** Muere con la Activity. Sirve, como mucho, para el tick de la
  UI, y para eso ya derivamos el restante del estado persistido.

- **`foregroundServiceType="shortService"`.** Está limitado a 3 minutos; nuestros slots llegan a 3
  horas.

- **Declarar `USE_EXACT_ALARM`** para tener alarmas exactas garantizadas sin pedir permiso. Play lo
  reserva a apps cuya función principal es un despertador o un calendario; un pomodoro es zona gris
  y hay historial de rechazos. Además con este diseño no hace falta.

## Consecuencias

- **Coste de batería**: unos 0,3 % por pomodoro de 25 minutos, casi todo del wakelock. Es la factura
  de un temporizador fiable y es la que pagan todas las apps del sector. Se mide con Battery
  Historian en la fase F9.

- **Hay que enviar la declaración de FGS `specialUse` a Play Console.** Es revisión manual y puede
  rebotar o tardar. El texto está en `docs/play-store-publication-texts.md` y se envía en la
  **primera subida a pruebas internas**, no en la de producción.

  Mitigación arquitectónica: el FGS está detrás de la interfaz `TimerServiceController`. Si Play
  rechazara el tipo, se publica sin servicio sustituyendo una implementación por
  `NoOpTimerServiceController`, sin tocar dominio ni UI. Esta reversibilidad es, por sí sola, una
  razón para el híbrido.

- **La app funciona en cuatro niveles de degradación**, todos aceptables:
  1. FGS + alarma exacta → precisión al segundo.
  2. FGS sin alarma exacta → precisión al segundo, sin red de seguridad.
  3. Sin FGS (matado por el OEM) + alarma exacta → precisión al segundo tras reconciliar.
  4. Sin FGS + alarma inexacta → puede retrasarse en Doze; Ajustes avisa.

- **Complejidad real añadida**: tres mecanismos que pueden dispararse a la vez. Se contiene con el
  índice único y con que `CompleteSlotUseCase` sea el **único** punto de cierre de slot, idempotente
  por construcción.

- La checklist de resiliencia (§10 de `CLAUDE.md`) es obligatoria en Android 12, 14 y 16 cada vez
  que se toca esta zona. No es opcional.
