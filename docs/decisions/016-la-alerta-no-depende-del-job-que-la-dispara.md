# 016 · La alerta no depende del job que la dispara

**Fecha**: 2026-08-18 · **Estado**: aceptada

## Contexto

Con los ajustes por defecto —auto-iniciar el descanso **activado**, auto-iniciar el pomodoro
desactivado— la app tenía un fallo con una asimetría muy característica: **al terminar el pomodoro no
sonaba nada, y al terminar el descanso sonaba con normalidad**. Los mismos ajustes, el mismo reproductor
y dos clips igual de válidos.

La asimetría es exactamente la del auto-inicio, y el mecanismo es este:

1. Quien cierra el slot es `PomodoroTimerService`, desde su corrutina `countdownJob`.
2. `CompleteSlotUseCase` cierra el slot y llama a `syncTimerRuntime()`.
3. Al terminar un **pomodoro**, el descanso encadena, así que el estado queda en `RUNNING` y `sync` va
   por su rama: arma la alarma y llama a `serviceController.start()`.
4. `onStartCommand` empieza por `countdownJob?.cancel()` — y ese es justo el job que está ejecutando el
   paso 3.
5. La corrutina, ya cancelada, muere en el siguiente punto de suspensión. La alerta iba después.

Al terminar un **descanso** no hay encadenado: el estado va a `RINGING`, no se arranca ningún servicio,
y la alerta llegaba a reproducirse. De ahí que el fallo pareciera cosa del sonido elegido.

Lo mismo se llevaba por delante la notificación de fin de slot del caso encadenado —la que el ADR 011
manda al reloj emparejado con «… ya en marcha»—, que se publica en esa misma línea.

Los tests no lo veían porque `FakeTimerStateRepository.current()` respondía desde memoria sin suspender:
sin suspensión no hay punto donde una corrutina cancelada se detenga, así que el fallo era invisible en
la JVM y solo existía en el dispositivo.

## Decisión

**Todo lo que va después de ganar el compare-and-set se ejecuta en `NonCancellable`**: la sincronización
del runtime, la notificación de fin y la alerta.

```kotlin
if (closedByUs) withContext(NonCancellable) {
    syncTimerRuntime()
    if (alertUser) { … notifier.showSlotFinished(…); alertPlayer.play(…) }
}
```

La regla que expresa: **una vez que el estado ha avanzado, avisar al usuario es una deuda**. El slot ya
está registrado y el reloj ya corre para el siguiente; quedarse a medias ahí es peor que cualquier
cancelación que pudiéramos estar honrando. La ventana es de milisegundos —`play()` arranca el
`MediaPlayer` y vuelve, y la cadena de repeticiones vive en el callback, no en la corrutina—, así que no
se retiene nada.

Y en los tests, **`FakeTimerStateRepository.current()` hace `yield()`**, porque DataStore lee de disco y
una lectura de disco es un punto de suspensión. Un fake que no suspende no es más rápido: es un fake que
miente sobre la única propiedad que estaba fallando.

## Alternativas descartadas

- **Reproducir la alerta antes de `syncTimerRuntime()`.** Quita el síntoma y deja el fondo: cualquier
  otra cancelación seguiría cortando la notificación, y el ADR 011 necesita que la alerta se publique
  *después* del `clearAlert()` que hace la rama `RUNNING` del sync.
- **Que el servicio no se cancele a sí mismo** (detectar en `onStartCommand` que ya corre el slot
  pedido). Es una mejora real, pero deja la alerta atada al ciclo de vida del servicio: `stopSelf`,
  `onDestroy` o una parada del sistema volverían a poder cortarla.
- **Lanzar la alerta en el scope de aplicación.** Funciona, pero convierte una llamada ordenada en un
  disparo suelto: se pierde el orden respecto al sync y aparece una carrera nueva con `stop()`.
- **Dejar el fake respondiendo desde memoria y probarlo solo en dispositivo.** Es lo que ya se hizo, y
  costó un fallo en producción que solo se notaba usando la app de verdad.

## Consecuencias

- El fin del pomodoro suena, y la notificación del caso encadenado se publica de verdad — con lo que el
  aviso al reloj emparejado con auto-inicio activado pasa a funcionar como decía el ADR 011.
- El `yield()` del fake hizo aflorar además un test del `TimerViewModel` que venía pasando por
  casualidad. Es la clase de deuda que un fake demasiado amable esconde.
- `NonCancellable` no protege de que el sistema mate el proceso. Es el límite conocido y aceptado del
  ADR 002: si el proceso muere a mitad de alerta, la alerta se corta.
