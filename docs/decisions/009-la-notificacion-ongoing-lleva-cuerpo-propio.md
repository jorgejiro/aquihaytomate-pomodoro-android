# 009 · La notificación ongoing lleva cuerpo propio con `Chronometer`

Fecha: 2026-07-28 · Estado: aceptada

## Contexto

La notificación del temporizador usaba la plantilla estándar: `setWhen(endAtEpochMs)` más
`setUsesChronometer(true)` y `setChronometerCountDown(true)`, con el título `Enfoque · 2/4`. El descuento lo
dibujaba SystemUI en el hueco donde una notificación pone su hora.

Eso resolvía lo importante —el coste— y estropeaba la jerarquía:

- **La cifra sale a 11 sp, arriba a la derecha, y no hay forma de cambiarlo.** Ese hueco es del sistema:
  ni tamaño, ni posición, ni peso son nuestros.
- **El `2/4` se lleva todo el peso visual**, porque estaba pegado al título, que es el texto grande de
  cualquier notificación. Justo al revés de lo que interesa: el tiempo que queda es el dato, y en qué
  pomodoro del ciclo vas es contexto.

En palabras del autor: «no me gusta que el tiempo restante salga tan pequeño y que lo de 2/4 salga tan
grande».

Las dos salidas obvias tenían pegas serias:

1. **Minutos en el título** (`Enfoque · 24 min restantes`), que es lo que hace Tomato. El título es texto
   estático: para que cambie hay que republicar la notificación cada minuto. Son ~25 `notify()` por
   pomodoro y, con la pantalla apagada, otros tantos despertares del proceso con wakelock — exactamente el
   coste que el ADR 001 rechazó para el widget. Además la cifra avanzaría a saltos de minuto.
2. **Dejarlo como estaba** y conformarse. La notificación es la cara del temporizador cuando la app está
   cerrada, que es la mayor parte del tiempo.

## Decisión

La notificación ongoing lleva **cuerpo propio** (`RemoteViews`) con un `Chronometer` de 26 sp, envuelto en
`NotificationCompat.DecoratedCustomViewStyle()` para que el sistema siga poniendo el encabezado y la fila
de acciones.

- **El `Chronometer` sigue ticando dentro de SystemUI**, con base en el reloj monotónico, igual que en el
  widget. El coste sigue siendo de unos cuatro `notify()` por pomodoro y el proceso sigue durmiendo el slot
  entero de un solo `delay()`. Esta es la razón de elegir esta vía y no la 1.
- La fase y el ciclo bajan a 13 sp, en el color secundario, al lado de la cifra.
- **Los colores salen de `values/colors.xml` y `values-night/colors.xml`**, no de la paleta del tomate: la
  vista la infla SystemUI sobre *su* fondo, que es casi blanco en tema claro, así que una cifra en hueso
  sería invisible la mitad de las veces.

  Aquí hubo una sorpresa que conviene dejar escrita. Lo primero que se probó fue
  `?android:attr/textColorPrimary`, que es lo que recomienda la documentación de Android para vistas propias
  en notificaciones. **No funciona**: medido en API 37, resuelve a un color oscuro con el sistema en tema
  claro *y* en tema oscuro, incluso después de reiniciar SystemUI para descartar caché, con lo que la cifra
  quedaba gris sobre fondo casi negro. Los cualificadores de recursos sí funcionan, porque el valor lo elige
  la configuración (`uiMode`) y no el tema que resulte estar inflando la vista. Es, además, el mismo
  mecanismo por el que un widget puede tener recursos distintos en modo noche.

  Es también la única superficie de la app con esquema claro, y la excepción está justificada: el fondo de
  una notificación no es nuestro. La regla de «solo tema oscuro» sigue valiendo para todo lo demás.
- **Los tres controles van dentro del cuerpo colapsado, como iconos.** La fila de acciones que dibuja el
  sistema a partir de `addAction` **solo existe en el estado expandido**, así que pausar costaba un toque de
  más: abrir la cortinilla no bastaba. Con los controles en nuestro cuerpo están ahí desde que se abre.

  Y son iconos, no etiquetas, por una restricción concreta: **para apps con `targetSdk` 31 o mayor, el
  contenido propio de una notificación colapsada está limitado a 48 dp** (antes eran 106 dp). En 48 dp no
  hay dos filas, y en una sola fila no caben la cifra a 24 sp y tres palabras. Por eso hay **dos cuerpos**:

  | Forma | Cuerpo | Controles |
  |---|---|---|
  | Colapsada (48 dp) | `notification_timer_collapsed.xml` | tres iconos propios: `⏸`/`▶`, `↺`, `▶\|` |
  | Expandida | `notification_timer.xml` | la fila del sistema, con etiquetas |

  Las acciones de `addAction` **se mantienen** aunque en colapsada no se usen: son las que ven Wear, el
  asistente y la pantalla de bloqueo, y los iconos disparan exactamente los mismos `PendingIntent`.
  La colapsada muestra solo el nombre de la fase, sin el `2/4`, que es lo primero que se recorta cuando
  compite con la cifra y los tres controles.
- Pausado usa un `TextView` al mismo tamaño con la cifra congelada, porque un `Chronometer` no se puede
  detener en un valor arbitrario, y añade `· Pausado` a la línea de fase de la forma expandida.
- `setContentTitle` se sigue rellenando aunque no se vea: es el fallback de las superficies que rechazan
  vistas propias y es lo que anuncia un lector de pantalla.
- **Se retira la línea «A continuación».** Ocupaba el renglón que ahora es la cifra, y lo que decía —que
  después de un pomodoro viene un descanso— ya lo sabe cualquiera que use la app. En la pantalla del
  temporizador se mantiene, donde no compite con nada.

## Alternativas descartadas

- **Minutos en el título, como Tomato.** Descrita arriba: ~25 republicaciones y despertares por pomodoro
  para empeorar la precisión de la cifra. Si algún día el cuerpo propio da problemas en algún OEM, este es
  el plan B.
- **Notificación totalmente custom** (`setCustomContentView` sin `DecoratedCustomViewStyle`). Da control
  absoluto y obliga a redibujar a mano el encabezado, el icono, el nombre de la app y las acciones, que es
  precisamente lo que cada fabricante cambia de sitio en cada versión.
- **`setRequestPromotedOngoing(true)`, las Live Updates de Android 16**, que es lo que usa Tomato y por lo
  que su notificación aparece desplegada y en el Now Bar de One UI. Se descartó porque **es incompatible con
  las vistas propias**: la documentación lo dice sin rodeos —«Must NOT have any `customContentView` set (no
  `RemoteViews`)»— y con ellas se iría la cifra grande, que era la petición original. Exige además el
  permiso `POST_PROMOTED_NOTIFICATIONS`, solo promociona en Android 16 y devolvería el tiempo al título con
  su repintado por minuto. Sigue siendo el plan B si el cuerpo propio da problemas en algún OEM: Google
  recomienda explícitamente evitar `RemoteViews` en notificaciones, y no sin razón.
- **`setProgress()` para el nivel del líquido.** Sigue prohibido, y por lo mismo de siempre: forzaría un
  repintado por segundo.

## Consecuencias

- **Un `Chronometer` no se puede formatear.** Muestra `MM:SS` mientras queda menos de una hora, que es todo
  el rango del temporizador, así que no hace falta; pero si alguna vez se permitieran slots de más de una
  hora, la cifra pasaría a `H:MM:SS` sola.
- **Hay una superficie nueva que revisar en cada Android y en cada OEM.** El cuerpo lo decora el sistema y
  One UI no lo hace igual que Pixel. Queda añadido a la checklist de §10: **mirar la notificación en
  Android 12, 14 y 16 y en One UI**, en tema claro y oscuro, colapsada y expandida, y en la pantalla de
  bloqueo, donde algunos sistemas degradan a la plantilla estándar.
- `TimerNotificationFactory` pasa a recibir `Clock` y `ElapsedRealtimeSource` para calcular la base del
  cronómetro, y `PomodoroTimerService` deja de leer Ajustes: solo lo hacía para la línea retirada.
