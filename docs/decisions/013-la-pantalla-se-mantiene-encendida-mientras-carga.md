# 013 · La pantalla se mantiene encendida mientras el móvil carga

## Contexto

Hasta la 1.1.0, «Mantener pantalla encendida» era un interruptor de sí/no, **desactivado por defecto**, y
además solo actuaba mientras el temporizador estaba en `RUNNING`.

Las dos cosas fallan en el uso real, que es el del autor: el móvil en el escritorio, en su cargador, toda
la mañana. Ahí quieres ver la cifra bajando sin tocar nada — es la razón de que la cuenta atrás sea de
76 sp y ocupe media pantalla. Y con el interruptor apagado, la pantalla se va a los 30 s; con el
interruptor encendido, se queda despierta también a las dos de la madrugada con el 8 % de batería, que
nadie pidió.

La condición `RUNNING` tenía un fallo aparte: al pausar, la pantalla se apagaba. Pausas un pomodoro para
contestar algo, y cuando vuelves la pantalla está negra y ya no sabes en qué punto estás. Lo mismo con
`RINGING`, que es exactamente el instante en el que hay algo que leer.

## Decisión

**Tres modos** en un solo ajuste (`keep_screen_on_mode`, string estable), con la regla en una función pura
en el modelo:

| Modo | Mantiene la pantalla encendida |
|---|---|
| `never` | nunca |
| `while_charging` | **por defecto**, mientras el móvil esté enchufado |
| `always` | siempre |

Y dos condiciones que valen para los tres:

- **Solo en la pantalla Temporizador.** La bandera se pone en la ventana desde `TimerScreen` con
  `view.keepScreenOn` y se limpia en `onDispose`, así que salir a Estadísticas o a Ajustes devuelve el
  apagado normal del sistema sin escribir una línea para ello.
- **No depende del estado del temporizador.** Parado, corriendo, pausado o sonando, si el usuario está
  mirando la pantalla del temporizador, la pantalla se queda.

«Enchufado» se lee de `EXTRA_PLUGGED` del sticky `ACTION_BATTERY_CHANGED` para el valor inicial, y a
partir de ahí solo quedan registrados `ACTION_POWER_CONNECTED` y `ACTION_POWER_DISCONNECTED`, y **solo
mientras el modo sea `while_charging`** y la pantalla esté delante.

## Alternativas descartadas

- **`BatteryManager.isCharging`.** Es la API que parece hecha para esto y es la respuesta equivocada. En
  One UI —el móvil del autor— la protección de la batería corta la carga al 80 % y desde ese momento el
  sistema informa de «no cargando» con el cable puesto. La pregunta que importa no es si entra corriente,
  es si la pantalla le está costando batería al usuario, y enchufado no le cuesta, esté al 80 % o al 100 %.
- **Dejar `ACTION_BATTERY_CHANGED` suscrito.** Da un broadcast cada vez que se mueve el nivel o la
  temperatura, varias veces por minuto, para responder a una pregunta que cambia dos veces al día.
- **Un `WakeLock`.** `SCREEN_BRIGHT_WAKE_LOCK` está obsoleto desde API 17 y, sobre todo, un wakelock que
  no se libera sobrevive a la pantalla que lo pidió. La bandera de ventana se cae con la ventana.
- **Mantener el booleano y añadir «solo si carga» como segundo interruptor.** Dos conmutadores para la
  misma pregunta, que hay que explicar cuál gana.
- **Seguir exigiendo `RUNNING`.** Es lo que causaba que pausar apagase la pantalla.

## Consecuencias

- Instalación nueva: la pantalla se mantiene encendida en el Temporizador con el móvil enchufado, sin
  configurar nada.
- Quien tenía el interruptor **encendido** pasa a `always`, que es literalmente lo que tenía. Quien lo
  tenía apagado pasa al nuevo valor por defecto, `while_charging`, y **no** a `never`: el interruptor viejo
  respondía a otra pregunta —«no apagues nunca la pantalla»— y decir que no a esa no es decir que no a
  esta. La clave booleana `keep_screen_on` queda retirada, solo se lee, y con otro nombre que la nueva
  porque leer un booleano guardado como texto revienta.
- `ChargingMonitor` entra como interfaz de dominio, así que la regla se prueba con un flow de mentira y
  `BatteryManager` no sale de la capa de datos.
- Con `always` sí es posible dejar la pantalla encendida hasta agotar la batería. Es una elección
  explícita del usuario, dicha con esas palabras en el selector.
- Queda por comprobar en dispositivo: que con la protección de batería de One UI cortando la carga la
  pantalla siga despierta, y que al desenchufar en modo `while_charging` la pantalla se apague sin salir
  de la app.
