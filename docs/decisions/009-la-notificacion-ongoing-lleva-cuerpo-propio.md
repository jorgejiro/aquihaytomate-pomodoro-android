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
- Pausado usa un `TextView` al mismo tamaño con la cifra congelada, porque un `Chronometer` no se puede
  detener en un valor arbitrario, y añade `· Pausado` a la línea de fase.
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
