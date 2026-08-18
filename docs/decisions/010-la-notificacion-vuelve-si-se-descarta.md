# 010 · La notificación del temporizador vuelve si se descarta

Fecha: 2026-07-28 · Estado: aceptada

## Contexto

`setOngoing(true)` dejó de significar «no se puede descartar». **Desde Android 13 el usuario puede quitar
por swipe la notificación de un servicio en primer plano**, y el servicio sobrevive: el pomodoro sigue
corriendo, el wakelock sigue tomado y la alarma sigue armada, pero en pantalla no queda nada que lo diga.

Para esta app eso es peor de lo que parece, por dos razones:

- **La notificación *es* el temporizador mientras la app está cerrada**, que es la mayor parte del tiempo.
  Sin ella no hay cifra, no hay fase y —desde el ADR 009— no hay controles.
- Un swipe en la cortinilla es un gesto que se hace sin mirar. Barrer una notificación de más es
  cotidiano; quedarse sin temporizador por ello, no.

Tomato hace que vuelva, y de ahí la petición: «si tienes un pomodoro o un descanso activo y lo cierras por
error, te vuelve a aparecer».

## Decisión

Las dos formas de la notificación ongoing llevan `setDeleteIntent(...)` apuntando a
`TimerActionReceiver.ACTION_ONGOING_DISMISSED`. El sistema dispara ese intent cuando el usuario la descarta,
y `RestoreOngoingNotificationUseCase` la vuelve a publicar.

**Solo mientras haya algo que informar**, y esta es la parte que evita convertirlo en spam:

| Estado | Al descartar |
|---|---|
| `RUNNING` | vuelve, con su cronómetro |
| `PAUSED` | vuelve, con la cifra congelada |
| `RINGING` | **no vuelve**: descartar la alerta es la forma de acusar recibo |
| `IDLE` | **no vuelve**: no hay slot del que informar |

`RestoreOngoingNotificationUseCase` decide leyendo el estado persistido, no recordando nada, así que la
decisión es la misma la reciba quien la reciba.

## Alternativas descartadas

- **No hacer nada y confiar en `FLAG_ONGOING_EVENT`.** Es lo que había, y es lo que Android 13 rompió a
  propósito.
- **Republicar desde un colector del estado.** Un descarte no cambia el estado del temporizador, así que
  el colector no se enteraría. Además, la republicación tiene que ocurrir *cuando* el usuario descarta,
  que es exactamente lo que un `deleteIntent` comunica.
- **Reiniciar el servicio en primer plano** (`serviceController.start()`) para que vuelva a llamar a
  `startForeground`. Funciona, y rehace el wakelock y la corrutina del countdown por una razón puramente
  cosmética. Publicar la notificación directamente es más barato y no toca el motor.

## Consecuencias

- **Es deliberadamente insistente, y hay que ser honesto sobre eso.** Android 13 le dio al usuario la
  posibilidad de descartar estas notificaciones y aquí se le devuelve una que él quitó. Se acepta porque es
  silenciosa, porque solo pasa mientras un slot está en marcha y porque la salida está a un toque: pausar o
  reiniciar el temporizador la retira de verdad, y el canal se puede silenciar. Si algún día Play lo señala,
  el sitio donde retirarlo es este `deleteIntent` y nada más.
- «Borrar todo» no la afecta: sigue teniendo `FLAG_NO_CLEAR`. Solo el swipe la descarta, y de ahí vuelve.
- Queda un caso que solo se ve en dispositivo real: si el usuario descarta la notificación **con la pantalla
  bloqueada** en algún OEM que la degrade a la plantilla estándar. El mecanismo es el mismo, pero conviene
  mirarlo cuando se pase la checklist §10.

---

## Añadido el 2026-08-18 · el temporizador parado también tiene notificación

Reiniciar desde la persiana dejaba la persiana **vacía**: `IDLE` limpiaba la notificación, y el
temporizador seguía ahí —en su slot, a cero de progreso— sin ninguna forma de volver a arrancarlo que no
fuera abrir la app. Ahora `IDLE` publica una tercera forma: el slot pendiente con su **duración completa**
y un ▸.

Lo que este ADR decide sigue en pie y es justo lo que la distingue de las otras dos:

- **No es `ongoing` y no vuelve al descartarla.** Con el reloj parado no hay ninguna cuenta corriendo a
  espaldas del usuario, así que un swipe es un «ahora no» legítimo. Las formas corriendo y pausada
  vuelven porque hay algo que sigue pasando; esta no.
- **Solo si hay una tanda empezada** (`sessionId != 0`). Una app recién instalada, o el día siguiente
  después del barrido del ADR 015, no saca de la nada una notificación ofreciendo un pomodoro que nadie
  ha pedido.
- **Los controles son los de la pantalla en ese estado**: ▸ INICIAR siempre, SALTAR solo si lo que espera
  es un descanso, y REINICIAR nunca — el slot ya está en su inicio. La regla vive en
  `TimerState.offersSkip`, que usan la pantalla y la notificación, para que no puedan divergir.
