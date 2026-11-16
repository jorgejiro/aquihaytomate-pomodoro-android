# 017 · El botón de pantalla encendida es un atajo al ajuste, no un segundo interruptor

## Contexto

El ADR 013 dejó la pantalla encendida en tres modos, con `while_charging` por defecto. Funciona para el
caso del autor —el móvil en el cargador del escritorio—, pero fuera del cargador mantener la pantalla
despierta obligaba a ir a Ajustes, cambiar a `always` y acordarse luego de volver. Es algo que se decide
en el momento: «hoy trabajo en el sofá y quiero ver la cuenta atrás». Una decisión así tiene que estar en
la pantalla donde se usa y tiene que verse cuando está puesta.

## Decisión

Un control de texto en la pantalla Temporizador, **en la fila de abajo, a la derecha de los puntos de
ciclo**, que **cambia el mismo ajuste** `keep_screen_on_mode`:

- **Activarlo** pone el modo en `always` y guarda el que había en `keep_screen_on_restore`.
- **Desactivarlo** devuelve ese modo guardado, o `DEFAULT` si no hay ninguno (como cuando se eligió
  `always` desde Ajustes). Quien estaba en `while_charging` sigue teniendo la pantalla encendida en el
  cargador, y a quien estaba en `never` no se le cuela la regla del cargador sin pedirla.
- **Está encendido siempre que el modo sea `always`**, se haya elegido aquí o en Ajustes.

La regla es una función pura, `KeepScreenOnMode.toggleAlways(restore)`, y la escritura se hace en una sola
edición de DataStore para que dos toques rápidos no se pisen.

Cómo se ve: apagado, un anillo y `PANTALLA` en `TextMuted`; encendido, el punto se rellena y el texto pasa a
`PANTALLA ENCENDIDA` en el color de la fase. El estado se lee por la forma, por el color y por las palabras,
nunca solo por el color.

## Alternativas descartadas

- **Un booleano aparte que se combina con el modo.** Serían dos interruptores para la misma pregunta, justo
  lo que el ADR 013 descartó. Con el modo en `always`, el botón apagado no apagaría nada.
- **Que el botón solo dure lo que dure la sesión.** Cerrar la app lo desactivaría sin avisar, y lo que el
  usuario ve en pantalla dejaría de coincidir con lo que dice Ajustes.
- **Que desactivarlo siempre lleve a `while_charging`.** A quien había elegido `never` le cambiaría el
  ajuste solo por probar el botón.
- **Ponerlo como tercer control junto a `REINICIAR` y `SALTAR`.** Esos controles actúan sobre el pomodoro y
  este es una preferencia de la pantalla. Su sitio es la fila de estado de abajo.

## Consecuencias

- La regla del ADR 013 no cambia: solo actúa en la pestaña Temporizador y no depende del estado del
  temporizador.
- Con el botón encendido la pantalla puede seguir así hasta gastar la batería. Lo ha pedido el usuario y
  lo tiene a la vista, en el color de la fase, en la misma pantalla en la que actúa.
- Una clave nueva en DataStore (`keep_screen_on_restore`). Sin migración: si falta, se usa `DEFAULT`.
