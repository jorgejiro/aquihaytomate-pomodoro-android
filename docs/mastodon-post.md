# Mastodon thread — ¡Aquí hay tomate!

Ready to paste as a thread: publish post 1, then reply to it with post 2, and so on. Every post fits in Mastodon's default limit of 500 characters (links always count as 23, whatever their real length).

- **Hashtags** go in CamelCase so screen readers can read them word by word. Mastodon has no algorithm: hashtags are how people who don't follow you find the post.
- **Images**: attach to post 1 the widget on the home screen and the timer running, from `docs/store-assets/capturas/en/telefono/` (or `es/`). Always add the alt text given below: a lot of people on Mastodon filter out or boost less the posts without it.
- **Visibility**: post 1 as *Public*. The replies can be *Public* or *Quiet public*.

---

## English version

### Post 1/4

I couldn't find a Pomodoro app for Android whose widget didn't eat a quarter of my home screen. Every one I tried was 2×2 or bigger.

So I built my own: ¡Aquí hay tomate! 🍅

A 1×1 widget. One tap to start or pause, a double tap to reset, without opening the app.

Free, no ads, no accounts, no tracking, and open source.

https://play.google.com/store/apps/details?id=com.jjrapps.aquihaytomate

#Android #Pomodoro #Productivity #OpenSource

**Alt text, image 1:** An Android home screen with a small red tomato widget taking up a single cell, showing the minutes left in the current Pomodoro.

**Alt text, image 2:** The app's timer screen: a large red circle on a black background, half drained like liquid, with the remaining time in the middle and text controls below.

### Post 2/4

Why another Pomodoro app? Because none of them covered what I needed:

⏱️ Goes off on the right second, with the screen off, after swiping the app away and after a reboot
🔔 Different sounds for the end of a Pomodoro and the end of a break, so you know which one ended without looking
⌚ The alert reaches your smartwatch
📊 Stats: streaks, weekly bars and a monthly heatmap
🌙 Dark and minimal, no giant buttons

### Post 3/4

It's 100% offline: the app doesn't even request the Internet permission. Your data never leaves your phone.

And it's open source under the MIT license, written in Kotlin with Jetpack Compose. The repo documents every architecture decision, in case you're curious how the widget ticks without draining the battery:

https://github.com/jorgejiro/aquihaytomate-pomodoro-android

#Kotlin #JetpackCompose #AndroidDev #FOSS

### Post 4/4

I built it to scratch my own itch, but I'd love it to be useful to more people.

If you try it and miss a feature, write to me at jjrmobileapps@gmail.com and I'll gladly look at your request. Replies and boosts are very welcome too 🙏

(The name is a Spanish pun: *pomodoro* is Italian for tomato.)

---

## Versión en español

### Publicación 1/4

No encontraba ninguna app de Pomodoro para Android cuyo widget no se comiera un cuarto del escritorio. Todas las que probé ocupaban 2×2 o más.

Así que me hice la mía: ¡Aquí hay tomate! 🍅

Un widget de 1×1. Un toque para iniciar o pausar y un doble toque para reiniciar, sin abrir la app.

Gratis, sin anuncios, sin cuentas, sin rastreo y de código abierto.

https://play.google.com/store/apps/details?id=com.jjrapps.aquihaytomate

#Android #Pomodoro #Productividad #CódigoAbierto

**Texto alternativo, imagen 1:** El escritorio de un móvil Android con un pequeño widget en forma de tomate rojo que ocupa una sola casilla y muestra los minutos que quedan del pomodoro en curso.

**Texto alternativo, imagen 2:** La pantalla del temporizador: un gran círculo rojo sobre fondo negro, medio vacío como si fuera un líquido, con el tiempo restante en el centro y los controles en texto debajo.

### Publicación 2/4

¿Por qué otra app de Pomodoro? Porque ninguna cubría lo que yo necesitaba:

⏱️ Suena en el segundo exacto, con la pantalla apagada, tras cerrar la app desde recientes y tras reiniciar el móvil
🔔 Un sonido para el final del pomodoro y otro para el del descanso: sabes cuál ha terminado sin mirar
⌚ El aviso llega al reloj
📊 Estadísticas: rachas, barras semanales y un mapa de calor mensual
🌙 Oscura y minimalista, sin botones gigantes

### Publicación 3/4

Funciona 100 % sin conexión: la app ni siquiera pide el permiso de Internet. Tus datos no salen del móvil.

Y es de código abierto con licencia MIT, escrita en Kotlin con Jetpack Compose. El repositorio documenta cada decisión de arquitectura, por si te interesa cómo avanza la cuenta atrás del widget sin gastar batería:

https://github.com/jorgejiro/aquihaytomate-pomodoro-android

#Kotlin #JetpackCompose #DesarrolloAndroid #SoftwareLibre

### Publicación 4/4

La hice para cubrir mis propias necesidades, pero me encantaría que le fuera útil a más gente.

Si la pruebas y echas en falta alguna función, escríbeme a jjrmobileapps@gmail.com y revisaré tu propuesta con mucho gusto. Las respuestas y los impulsos también son muy bienvenidos 🙏

(*Pomodoro* significa *tomate* en italiano). 