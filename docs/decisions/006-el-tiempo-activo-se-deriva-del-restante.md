# 006 — El tiempo activo se deriva del restante, y el reinicio se detecta por el uptime

Fecha: 2026-07-27
Estado: aceptada

## Contexto

Al implementar el núcleo del temporizador (F1–F3) aparecieron dos preguntas que §6 de `CLAUDE.md`
no resolvía, y una de ellas destapó un error en el algoritmo que ese documento describía.

**1. ¿Cómo se mide el tiempo realmente enfocado, excluyendo las pausas?** La columna
`actual_focus_ms` de `focus_session` es la base de «minutos enfocados», así que tiene que ser
exacta y tiene que sobrevivir a que el proceso muera a mitad de un pomodoro pausado.

**2. ¿Cómo se distingue un reinicio del dispositivo de un cambio manual de la hora?** `CLAUDE.md`
§6 y el ADR 002 decían: guardar `bootEpochMs = epoch - elapsedRealtime` y, cuando la diferencia
recalculada se salga de la tolerancia, «caer al epoch», porque eso «indica reinicio o cambio manual
de la hora».

Los dos sucesos rompen `epoch - elapsedRealtime`, pero **piden respuestas opuestas**:

| Suceso | Qué le pasa a `epoch` | Qué le pasa a `elapsedRealtime` | Referencia válida |
|---|---|---|---|
| Reinicio | sigue bien | **vuelve a ~0** | el epoch |
| Cambio de hora | **salta** | sigue bien | el `elapsedRealtime` |

Caer al epoch en ambos casos hace que cambiar la hora del sistema +1 h desplace el pomodoro esa
misma hora. Eso contradice de frente dos requisitos explícitos del propio documento: la regla
«**cambiar la hora del sistema no debe hacer saltar el pomodoro**» de §6, y el punto 4 de la
checklist de resiliencia de §10. Se detectó porque los tests de `TimerMath` escritos contra esos
requisitos fallaron.

## Decisión

**1. `activeElapsedMs = slotDurationMs - remainingMs`. No hay acumulador.**

Al reanudar, el deadline se empuja exactamente lo que quedaba (`endAt = now + remainingAtPause`).
Por construcción, entonces, `duración − restante` *es* el tiempo que el reloj estuvo corriendo, sin
contar las pausas. No hace falta guardar ni un contador acumulado ni el instante de la última
reanudación.

**2. El reinicio se detecta porque el uptime retrocedió, no porque `bootEpochMs` se descuadre.**

Mientras un slot corre, el deadline monotónico se escribió como `algúnUptime + restante` con
`restante ≤ slotDurationMs`. Por tanto:

```
endAtElapsedRealtimeMs − slotDurationMs ≤ uptimeActual
```

siempre, sin reinicio. Si la desigualdad se rompe, el contador volvió a empezar. Es una cota exacta,
no una heurística:

```kotlin
fun hasRebooted(state: TimerState, nowElapsedRealtimeMs: Long): Boolean {
    if (state.endAtElapsedRealtimeMs <= 0L) return true
    val earliestPossibleUptime = state.endAtElapsedRealtimeMs - state.slotDurationMs
    return nowElapsedRealtimeMs < earliestPossibleUptime - REBOOT_TOLERANCE_MS
}
```

Sin reinicio se usa **siempre** el deadline monotónico, así que mover el reloj del sistema en
cualquier dirección no altera el restante. `bootEpochMs` se sigue guardando en el estado: cuesta
ocho bytes y es el dato que uno quiere en un log cuando algo raro pasa en un dispositivo real.

## Alternativas descartadas

- **Acumular `activeElapsedMs` en el estado, sumando en cada pausa.** Es un segundo origen de verdad
  sobre el mismo hecho. Si el proceso muere entre el momento de pausar y el de escribir, el
  acumulador y los deadlines discrepan, y `reconcile()` no tiene forma de saber cuál de los dos
  mentía. La resta no puede desincronizarse porque no es un dato guardado.

- **Guardar `startedAtElapsedRealtimeMs` en el estado para detectar el reinicio.** Funciona igual de
  bien, pero es un campo más en la superficie de almacenamiento —y por tanto en el widget, en el
  servicio y en la notificación— para expresar algo que ya está implícito en el deadline y la
  duración.

- **Mantener la detección por `bootEpochMs` y aceptar el salto al cambiar la hora.** Rompe un
  requisito explícito de producto. Además el fallo es silencioso: un usuario que ajusta la hora en
  un viaje ve su pomodoro dispararse sin explicación.

- **Quedarse con el mínimo de los dos restantes.** Un cambio de hora hacia adelante haría que el
  epoch diese el mínimo y el pomodoro saltaría; hacia atrás, lo contrario. No sirve.

## Consecuencias

- Hay un caso residual sin cubrir: reiniciar el dispositivo cuando el slot arrancó en los primeros
  segundos de uptime *y* el nuevo uptime ya supera esa cota. La imprecisión es de segundos, no un
  salto, y el `reconcile()` al abrir la app cierra el slot igualmente. Es un compromiso mucho mejor
  que el que teníamos.

- **`CLAUDE.md` §6 y el ADR 002 quedan desactualizados en este punto.** El párrafo del «doble
  deadline» describe la detección vieja. Hay que corregirlo cuando se toque el documento; este ADR
  es la referencia válida mientras tanto.

- `TimerMath.hasRebooted` sustituye a la `isSameBoot` que se había escrito primero. El nombre importa:
  el predicado interesante es «¿se reinició?», no «¿coinciden los relojes?», precisamente porque son
  preguntas distintas y confundirlas es el error que nos costó los dos tests.

- Los dos requisitos quedan cubiertos por tests unitarios que no necesitan emulador
  (`TimerMathTest`, `TimerEngineTest`), y siguen siendo obligatorios en dispositivo en F4 según la
  checklist de §10.
