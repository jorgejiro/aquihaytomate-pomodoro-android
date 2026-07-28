# 011 · La alerta de fin de slot está hecha para llegar al reloj

Fecha: 2026-07-28 · Estado: aceptada

## Contexto

El autor lleva un Garmin Forerunner 265s emparejado por Bluetooth y quiere que, al terminar un pomodoro o
un descanso —siempre que el auto-inicio esté desactivado—, le llegue el aviso a la muñeca con dos opciones:
**empezar el siguiente** y **descartar**.

Lo primero fue un error de diagnóstico que conviene dejar escrito para no repetirlo: se asumió que un
Garmin, al no ser Wear OS, solo reenvía título y texto y ofrece sus propias opciones nativas. **Es falso.**
Garmin Connect en Android reenvía las `Notification.Action` de la app y las presenta como lista en el
reloj; la prueba es TickTick, que muestra ahí sus propias «Complete» y «Snooze 15m».

Con eso claro, la petición no necesitaba nada nuevo: **la alerta ya tenía exactamente esas dos acciones**
—`Empezar descanso` / `Volver al tajo` y `Descartar`— desde la fase F4. El problema era que no llegaba, y
al mirarlo aparecieron dos cosas.

## Decisión

**1. La alerta no lleva `setSilent(true)`.** Estaba en el `base()` común a las dos notificaciones, puesto
por el ADR 004: el sonido y la vibración los toca `AlertPlayer`, no el canal. Pero ese flag hace más que
callar — marca la notificación como **no alertante**, y con ello:

- pierde el heads-up que `CLAUDE.md` §3 da por supuesto al renunciar a `USE_FULL_SCREEN_INTENT`;
- cae en la bandeja «silenciosas», que es la que los relojes emparejados tienden a no reenviar.

El canal `timer_alerts` sigue mudo por su cuenta (`setSound(null, null)`, `enableVibration(false)`), así
que quitar el flag **no produce ningún sonido del sistema** ni duplica la alerta. Verificado en el
emulador: la notificación se publica con `importance=4`, `category=alarm`, `actions=2` y `sound=null`, y
sus flags pasan de `ONLY_ALERT_ONCE|AUTO_CANCEL|SILENT` a `ONLY_ALERT_ONCE|AUTO_CANCEL`.

Las dos formas ongoing **sí conservan** `setSilent(true)`: se republican en cada transición y no deben
hacer ruido nunca.

**2. Descartar la alerta desde cualquier sitio para la alarma.** Al descartarla desde el reloj, Garmin
cancela la notificación en el móvil sin pasar por la acción `Descartar`, así que el sonido y la vibración
seguían hasta agotar su duración — **hasta 30 segundos** con el ajuste al máximo. Un `deleteIntent` a
`ACTION_DISMISS` cierra ese hueco. Es un fallo que existía también en el móvil, con «borrar todo».

Ese `deleteIntent` **no republica nada**, a diferencia del de las ongoing (ADR 010):
`RestoreOngoingNotificationUseCase` se niega a restaurar mientras el estado sea `RINGING`, que es
precisamente el estado en el que vive esta alerta.

**3. Las dos formas ongoing van `setLocalOnly(true)`: al reloj llega solo el aviso de fin.** Un
temporizador en marcha es una notificación permanente, y permanente en la lista de notificaciones de un
reloj es ruido — se acumula, hay que descartarla y vuelve en la transición siguiente. Es un intercambio
explícito y aceptado: **se renuncia a pausar desde la muñeca** y a cambio la muñeca solo vibra cuando hay
algo que decidir. Para el control rápido ya está el widget de una casilla, que es la razón de ser del
proyecto.

Las dos cosas van juntas en `asPhoneOnly()`, porque las dos ongoing comparten los dos motivos: silenciosas
por republicarse en cada transición, y locales por ser permanentes.

## Alternativas descartadas

- **Una app Connect IQ** en el reloj hablando con la app por el SDK de Garmin. Es la vía para controlar el
  temporizador desde la muñeca *sin* notificación —un widget con la cifra, por ejemplo—, y no hace falta
  para lo que se pedía. Queda para una v1.x si alguna vez se quiere el temporizador en la esfera.
- **Dejar que el cronómetro persistente llegue también al reloj**, para poder pausar desde la muñeca. Se
  descartó: un temporizador en marcha es una notificación permanente, y permanente en la lista de un reloj
  es ruido. Ver el punto 3 de la decisión.

**4. Con auto-inicio también se avisa.** El diseño original no publicaba alerta cuando el slot siguiente
arrancaba solo: se actualizaba la ongoing y ya. Visto en uso, eso significa que **el fin de un pomodoro no
llega al reloj**, que es justo cuando el móvil está en otra habitación. Ahora se publica igual, con el cuerpo
diciendo que el slot ya está en marcha, la acción de saltarlo en vez de empezarlo, y un `setTimeoutAfter` de
dos minutos para que no se acumule algo que nadie tiene que atender.

Se publica **después** de `syncTimerRuntime()` a propósito: ese paso, al pasar por `RUNNING`, llama a
`clearAlert()`, así que publicar antes sería publicar para nada.

**5. El copy se escribe para una pantalla de reloj.** Un Garmin muestra el título y el cuerpo, sin nombre de
app ni icono propio. `¡Tiempo!` sobre `Se acabó el descanso` no decía ni qué había acabado ni qué venía. El
título nombra lo completado con su posición en el ciclo —lo único que el estado ya no puede implicar, porque
ha avanzado— y el cuerpo nombra el slot siguiente con su duración:

| Situación | Título | Cuerpo |
|---|---|---|
| Fin de pomodoro, esperando | `Pomodoro 3 de 4 completado` | `Descanso de 5 min · toca para empezar` |
| Fin de pomodoro, encadenado | `Pomodoro 3 de 4 completado` | `Descanso de 5 min ya en marcha` |
| Fin de descanso | `Descanso terminado` | `Pomodoro de 25 min · toca para empezar` |

## Consecuencias

- Al terminar un slot, el sistema puede mostrar heads-up. Con un canal mudo no está garantizado en todas
  las versiones (en el emulador no apareció), pero cuando lo haga es el comportamiento que se quería.
- **Lo que no se puede verificar aquí es lo único que importaba**: que el Forerunner muestre las dos
  acciones. El emulador no tiene reloj emparejado. Queda en la checklist §10, junto con esto: que la app
  esté habilitada en Garmin Connect → Notificaciones inteligentes, y que pulsar «Empezar descanso» desde
  la muñeca arranque el slot con el móvil bloqueado — las acciones de notificación no piden desbloqueo,
  pero conviene comprobarlo.
- Con auto-inicio activado **también se avisa**, con el texto adaptado. Verificado en el emulador con la app
  en segundo plano, que es el caso que importa: se publica `id=2` con «Pomodoro 3 de 4 completado · Descanso
  de 5 min ya en marcha» mientras el descanso ya corre en la notificación `id=1`.
