# Reddit post — ¡Aquí hay tomate!

Ready to paste. Reddit's Markdown editor does not render HTML, so everything below is plain Markdown.
Attach 2–3 screenshots as an image gallery (widget on the home screen first) from
`docs/store-assets/capturas/en/telefono/` when the subreddit allows image posts.

---

## English version

### Title (pick one)

- I couldn't find a Pomodoro app with a 1×1 widget, so I built one (free, no ads, open source)
- I made a Pomodoro timer whose home screen widget fits in a single cell — free, offline and open source
- Every Pomodoro widget I tried took 2×2 cells. Mine takes one. [Free, no ads, open source]

### Body

Hi everyone!

I use the Pomodoro technique every day, and for a long time I kept jumping between Pomodoro apps on the
Play Store. None of them covered what I actually needed:

- **A tiny widget.** Every app I tried had a home screen widget of 2×2 cells at minimum, and some of them
  much bigger. I just wanted to start, pause and reset the timer from the home screen without giving up a
  quarter of it.
- **A timer that never fails.** Several apps went off late, or not at all, when the screen was off or
  after I swiped the app away from recents. A timer that goes off late is no timer at all.
- **A calm design.** Most apps today follow a generic look with huge, filled buttons. I wanted something
  dark and minimal that stays out of the way.
- **No accounts, no ads, no tracking.** A timer has no reason to know who I am.

So I built my own: **¡Aquí hay tomate!** (the name is a Spanish pun: *pomodoro* is Italian for
*tomato*).

**What it does**

- 🍅 **1×1 home screen widget.** One tap starts, pauses or resumes. A double tap resets. It shows the
  remaining time, and the tomato drains like liquid as time runs out: red for focus, amber for breaks. The
  countdown ticks inside the launcher, so the widget costs no battery.
- ⏱️ **Goes off on the right second**, with the screen off, after you kill the app from recents and after
  a reboot. It runs a foreground service with an `AlarmManager` backstop, so it doesn't depend on the app
  staying open. Changing the system clock doesn't make the timer jump either.
- 🔁 **Full cycles.** Focus, short break, and a long break every N Pomodoros. Every duration is
  configurable, and you can choose to start the break, the next Pomodoro, or both automatically.
- 🔔 **Different alerts for each end.** Finishing a Pomodoro and finishing a break sound different, so you
  know which one ended without looking. There are five sounds, each can repeat 1–10 times, and the
  vibration length is set in seconds. The alert uses the alarm volume and respects silent mode and Do Not
  Disturb. It also reaches your paired smartwatch, with an action to start the next slot.
- 📊 **Statistics.** Pomodoros completed, time focused, weekly bars against a daily goal, a monthly
  heatmap, and your current and best streaks.
- 🌙 **Other details.** A dark theme on a pure black background, an option to keep the screen on while
  charging, and the app in English and Spanish.
- 🔒 **100% offline.** The app doesn't even request the Internet permission: no accounts, no cloud, no ads,
  no analytics. All your data stays on your phone.

It's **free, with no ads and no in-app purchases**, and it needs Android 12 or newer.

👉 **Google Play:** https://play.google.com/store/apps/details?id=com.jjrapps.aquihaytomate

**It's open source.** All the code is on GitHub under the MIT license. It's written in Kotlin with
Jetpack Compose, and the widget uses classic `RemoteViews` with a native `Chronometer`. If you're curious
how it works, or want to learn from it or contribute, the repo documents the architecture decisions:

👉 **GitHub:** https://github.com/jorgejiro/aquihaytomate-pomodoro-android

**Missing a feature?** I built this to scratch my own itch, but I'd love it to be useful to more people.
If you try it and miss something, email me at **jjrmobileapps@gmail.com** and I'll gladly look at your
suggestion. Feedback in the comments is very welcome too, including the harsh kind.

Thanks for reading! 🍅

---

## Versión en español

### Título (elige uno)

- No encontraba ninguna app de Pomodoro con widget de 1×1, así que me hice una (gratis, sin anuncios y de código abierto)
- He creado un temporizador Pomodoro cuyo widget ocupa una sola casilla del escritorio: gratis, sin conexión y open source

### Cuerpo

¡Hola a todos!

Uso la técnica Pomodoro a diario, y durante mucho tiempo fui probando una app tras otra de Google Play.
Ninguna cubría lo que yo necesitaba:

- **Un widget pequeño.** Todas las que probé tenían un widget de 2×2 casillas como mínimo, y algunas mucho
  más grande. Yo solo quería iniciar, pausar y reiniciar el temporizador desde el escritorio sin
  sacrificar un cuarto de la pantalla.
- **Un temporizador que no falle nunca.** Varias sonaban tarde, o directamente no sonaban, con la pantalla
  apagada o después de cerrar la app desde recientes. Un temporizador que suena tarde no sirve.
- **Un diseño tranquilo.** La mayoría sigue un estilo genérico con botones enormes y rellenos. Yo quería
  algo oscuro y minimalista que no molestara.
- **Sin cuentas, sin anuncios y sin rastreo.** Un temporizador no tiene por qué saber quién soy.

Así que me hice la mía: **¡Aquí hay tomate!** (*pomodoro* significa *tomate* en italiano).

**Qué hace**

- 🍅 **Widget de 1×1.** Un toque inicia, pausa o reanuda; un doble toque reinicia. Muestra el tiempo que
  queda y el tomate se vacía como un líquido: rojo en el enfoque y ámbar en los descansos. La cuenta atrás
  avanza dentro del launcher, así que el widget no gasta batería.
- ⏱️ **Suena en el segundo exacto**, con la pantalla apagada, después de cerrar la app desde recientes y
  tras reiniciar el móvil. Funciona con un servicio en primer plano y una alarma de respaldo, así que no
  depende de que la app siga abierta. Cambiar la hora del sistema tampoco hace saltar el temporizador.
- 🔁 **Ciclos completos.** Enfoque, descanso corto y un descanso largo cada N pomodoros. Todas las
  duraciones se pueden configurar, y puedes hacer que el descanso, el siguiente pomodoro o los dos
  empiecen solos.
- 🔔 **Un aviso distinto para cada final.** El final del pomodoro y el del descanso suenan distinto, así
  que sabes cuál ha terminado sin mirar. Hay cinco sonidos, cada uno puede repetirse de 1 a 10 veces, y
  la vibración se configura en segundos. El aviso usa el volumen de alarma y respeta el modo silencio y
  No molestar. También llega al reloj emparejado, con una acción para empezar el siguiente intervalo.
- 📊 **Estadísticas.** Pomodoros completados, tiempo enfocado, barras semanales frente a un objetivo
  diario, un mapa de calor mensual y tu racha actual y la mejor.
- 🌙 **Otros detalles.** Tema oscuro sobre negro puro, opción de mantener la pantalla encendida mientras
  carga, y la app en español e inglés.
- 🔒 **Funciona 100 % sin conexión.** La app ni siquiera pide el permiso de Internet: no hay cuentas, ni
  nube, ni anuncios, ni analítica. Tus datos no salen del móvil.

Es **gratis, sin anuncios y sin compras integradas**, y necesita Android 12 o superior.

👉 **Google Play:** https://play.google.com/store/apps/details?id=com.jjrapps.aquihaytomate

**Es de código abierto.** Todo el código está en GitHub con licencia MIT. Está escrita en Kotlin con
Jetpack Compose, y el widget usa `RemoteViews` clásicos con un `Chronometer` nativo. Si te interesa ver
cómo funciona, aprender de él o contribuir, el repositorio documenta las decisiones de arquitectura:

👉 **GitHub:** https://github.com/jorgejiro/aquihaytomate-pomodoro-android

**¿Echas en falta alguna función?** La hice para cubrir mis propias necesidades, pero me encantaría que
le fuera útil a más gente. Si la pruebas y echas algo en falta, escríbeme a **jjrmobileapps@gmail.com** y
revisaré tu propuesta con mucho gusto. Los comentarios aquí también son bienvenidos, incluidas las
críticas.

¡Gracias por leer! 🍅
