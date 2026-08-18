# 015 · El ciclo se reinicia al cambiar de día

**Fecha**: 2026-08-18 · **Estado**: aceptada

## Contexto

El contador de pomodoros del ciclo (`completedFocusInCycle`) vive en el estado persistido y **nada lo
envejecía**. El estado del temporizador está pensado para sobrevivir a que el sistema mate el proceso,
así que sobrevive también a que el usuario no toque la app en una semana.

El síntoma apareció en un Galaxy Z Fold 7 que no es el móvil de diario: al desbloquearlo y abrir la app
días después del último pomodoro, la pantalla decía `3/4` y ofrecía un descanso corto como slot
siguiente. Ni el ciclo ni el descanso tenían nada que ver con el día que el usuario estaba empezando.

Las estadísticas no sufrían el problema —se agregan por `local_date`, así que «hoy» es siempre hoy—, lo
que hacía la incoherencia más visible: cero pomodoros hoy y, a la vez, tres cuartos de ciclo hechos.

Hay una tensión real detrás: **un ciclo es una unidad de jornada, pero el estado del motor es una
unidad de proceso**. Nada en el motor sabía de días, y meterle esa noción a la ligera choca con dos
reglas ya tomadas: que un pomodoro corriendo no se toca nunca (ADR 002) y que un cambio de hora del
sistema no debe alterar el temporizador (ADR 006).

## Decisión

**Un temporizador parado que quedó de un día anterior se barre: ciclo a cero, siguiente slot de
enfoque y tanda cerrada.** La barrida ocurre dentro de `reconcile()`, que es justamente lo que llaman
todos los puntos de entrada que pueden ser lo primero que se toca por la mañana: abrir la app, tocar el
widget y el arranque del dispositivo.

Tres piezas:

1. **`TimerState.lastActivityEpochMs`**, sellado por *todas* las transiciones de `TimerTransitions`.
   Hacía falta una marca explícita: `slotStartedAtEpochMs` se pone a cero al reiniciar un slot y
   `sessionId` es de cuando empezó la tanda, que puede ser de anteayer aunque se trabajara anoche. Los
   estados escritos por versiones anteriores no la traen, así que `lastTouchedEpochMs` cae hacia atrás
   a esas dos marcas antiguas y, si tampoco hay, responde «no sé» y no se barre nada.
2. **`DayRollover`**, función pura con el `ZoneId` inyectado, como el resto de reglas de tiempo del
   motor. Compara días locales con `isBefore`, nunca con una desigualdad: **solo rueda hacia
   adelante**, de modo que atrasar el reloj del sistema no borra el ciclo en curso.
3. **`StartFreshDayUseCase`**, que hace la limpieza y se llama desde `reconcile()`.

Qué hace con cada estado parado:

| Estado | Qué pasa |
|---|---|
| `IDLE` con ciclo o con un descanso pendiente | se barre |
| `RINGING` sin atender desde otro día | se barre y se retira su notificación |
| `PAUSED` desde otro día | se da por abandonado: **se registra el parcial** si llegó al minuto, y se barre |
| `RUNNING` | **nunca se toca** |
| `IDLE` ya limpio | no se escribe nada, para no tocar el disco a diario sin motivo |

Además, un slot que estaba corriendo y **venció en un día anterior** se cierra como siempre —la fila se
apunta en el día en que se hizo— pero acto seguido se barre el ciclo que dejaba tras de sí. Sin eso, el
caso más probable del bug seguía vivo: el pomodoro que se quedó corriendo el martes abría el viernes con
`3/4` y un descanso esperando.

## Alternativas descartadas

- **Barrer por horas transcurridas (p. ej. «más de 8 h sin tocar»).** Más fácil de implementar y más
  difícil de explicar: el usuario razona en días, no en ventanas. Y trata igual las 21:00 → 05:00 de la
  misma jornada que las 09:00 → 17:00 del día siguiente.
- **Un `WorkManager` a medianoche.** Trabajo periódico, batería y un punto de fallo nuevo para algo que
  solo importa cuando alguien mira la app. Barrer perezosamente en `reconcile()` da el mismo resultado
  visible sin despertar el dispositivo.
- **Derivar el ciclo de las filas de `focus_session` de hoy.** Elimina el estado duplicado, pero rompe
  el ciclo que cruza la medianoche trabajando y obliga a consultar la base de datos desde el widget y
  el servicio, que hoy solo leen DataStore.
- **Dejar el pausado intacto.** Se valoró y se descartó con Jorge: un pomodoro pausado desde otro día
  está abandonado de hecho, y dejarlo ahí conserva justo el `3/4` que motivó todo. El precio es el caso
  raro de pausar a las 23:55 y volver a las 00:05, donde se pierde el slot a medias —el tiempo enfocado
  no, que se registra como parcial.
- **Comparar días con `!=` en vez de `isBefore`.** Habría hecho que atrasar el reloj del sistema borrase
  el ciclo, exactamente el tipo de daño que el ADR 006 evita en el descuento.

## Consecuencias

- La app abre siempre en `1/4` y en enfoque cuando el día anterior quedó a medias, que es lo que el
  usuario espera de una técnica organizada por jornadas.
- El histórico no pierde nada: los slots vencidos y los pausados abandonados se registran, y con la
  fecha del día en que se trabajaron.
- El estado persistido gana un campo. Es retrocompatible: la clave nueva no existe en los ficheros ya
  escritos y se lee como cero, con las marcas antiguas de reserva.
- `TimerTransitions.pause` y `resetSlot` pasan a recibir el reloj de pared. Es el precio de que la marca
  se selle en un solo sitio en vez de en cada caso de uso.
- Queda por decidir, si algún día hay etiquetas o proyectos, si la jornada debería empezar a una hora
  configurable en vez de a medianoche. Para quien trabaje de madrugada, medianoche parte la jornada por
  la mitad.
