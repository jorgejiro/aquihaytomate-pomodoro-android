# Novedades para Google Play — ¡Aquí hay tomate!

Textos de **«Novedades»** («What's new») listos para copiar y pegar en Play Console al crear la
release: *Producción → Crear nueva versión → Notas de la versión*, una pestaña por idioma
(`es-ES` y `en-US`).

- **Límite de Google Play: 500 caracteres por idioma.** Cada bloque de abajo indica los que ocupa.
- La ficha permanente (nombre, descripciones, capturas, privacidad, cuestionarios) está en
  [`play-store-publication-texts.md`](play-store-publication-texts.md). Este archivo es solo el
  texto que cambia en cada publicación.
- Las viñetas salen del `CHANGELOG.md` y de los `string-array` `changelog_*`, pero **no son el mismo
  texto**: aquí se escribe para alguien que aún no tiene la versión, así que se omite lo interno y se
  añade el recordatorio de que la app sigue sin cuentas ni seguimiento.
- Al publicar una versión nueva, añade su bloque arriba y deja los anteriores como historial.

---

## 1.3.1 (versionCode 7) — 2026-08-18

Casi todo son correcciones, así que las viñetas nombran **el síntoma que el usuario vio**, no la causa:
nadie de fuera sabe qué es una corrutina cancelada, pero sí se acuerda de que el pomodoro acabó en
silencio. El sonido va primero porque es el que rompía la promesa de la app —un temporizador que no avisa
no es un temporizador— y arrastra dos cosas más en la misma viñeta: el volumen de alarma, que explica por
qué alguien podía no oír nada teniéndola alta, y el cambio de sonido por defecto, que es lo que va a notar
de entrada quien nunca haya tocado ese ajuste. De ahí la frase sobre el cuenco: lo primero que se pregunta
quien echa de menos el de antes es si sigue estando.

### es-ES (457 caracteres)

```text
Novedades de la versión 1.3.1

• El aviso del final del pomodoro vuelve a sonar, y lo hace con el volumen de alarma. Ahora es «Digital»: más seco y más fuerte. El cuenco tibetano sigue en la lista.
• El ciclo empieza de cero cada día: ya no te recibe en el pomodoro 3 de 4 de otro día.
• Reiniciar desde la notificación ya no la hace desaparecer: se queda con el tiempo completo y el botón de empezar.

Sin cuentas, sin nube, sin anuncios y sin seguimiento.
```

### en-US (422 caracteres)

```text
What's new in 1.3.1

• The end-of-pomodoro alert sounds again, and it follows the alarm volume. It is «Digital» now: shorter and louder. The singing bowl is still in the list.
• The cycle starts from zero each day: no more opening on pomodoro 3 of 4 from another day.
• Resetting from the notification no longer makes it vanish: it stays, with the full time and a start button.

No accounts, no cloud, no ads, no tracking.
```

### Formato con etiquetas de idioma

```xml
<es-ES>
Novedades de la versión 1.3.1

• El aviso del final del pomodoro vuelve a sonar, y lo hace con el volumen de alarma. Ahora es «Digital»: más seco y más fuerte. El cuenco tibetano sigue en la lista.
• El ciclo empieza de cero cada día: ya no te recibe en el pomodoro 3 de 4 de otro día.
• Reiniciar desde la notificación ya no la hace desaparecer: se queda con el tiempo completo y el botón de empezar.

Sin cuentas, sin nube, sin anuncios y sin seguimiento.
</es-ES>
<en-US>
What's new in 1.3.1

• The end-of-pomodoro alert sounds again, and it follows the alarm volume. It is «Digital» now: shorter and louder. The singing bowl is still in the list.
• The cycle starts from zero each day: no more opening on pomodoro 3 of 4 from another day.
• Resetting from the notification no longer makes it vanish: it stays, with the full time and a start button.

No accounts, no cloud, no ads, no tracking.
</en-US>
```

---

## 1.3.0 (versionCode 6) — 2026-08-10

**Este bloque suma la 1.3.0 y la 1.2.0**, porque las dos llegan juntas al usuario: la 1.2.0 no se
publicó por separado. Dentro de la app las dos versiones se siguen leyendo por separado, cada una con su
`string-array` — la pantalla Novedades es un historial y esto no.

### es-ES (465 caracteres)

```text
Novedades de la versión 1.3.0

• El icono es ahora un reloj que marca las cinco y cinco: las cinco son los 25 minutos de un pomodoro y la una, los 5 del descanso.
• El sonido de fin se puede repetir hasta diez veces, con un ajuste aparte para el pomodoro y para el descanso.
• La pantalla se mantiene encendida mientras el móvil carga.
• Corregido: el temporizador se salía de la pantalla con el móvil girado.

Sin cuentas, sin nube, sin anuncios y sin seguimiento.
```

### en-US (420 caracteres)

```text
What's new in 1.3.0

• The icon is a clock now, reading five past five: five o'clock is the 25 minutes of a pomodoro and one o'clock the 5 of a break.
• The end sound can repeat up to ten times, with a separate setting for the pomodoro and for the break.
• The screen stays awake while your phone is charging.
• Fixed: the timer ran off the screen with the phone on its side.

No accounts, no cloud, no ads, no tracking.
```

### Formato con etiquetas de idioma

```xml
<es-ES>
Novedades de la versión 1.3.0

• El icono es ahora un reloj que marca las cinco y cinco: las cinco son los 25 minutos de un pomodoro y la una, los 5 del descanso.
• El sonido de fin se puede repetir hasta diez veces, con un ajuste aparte para el pomodoro y para el descanso.
• La pantalla se mantiene encendida mientras el móvil carga.
• Corregido: el temporizador se salía de la pantalla con el móvil girado.

Sin cuentas, sin nube, sin anuncios y sin seguimiento.
</es-ES>
<en-US>
What's new in 1.3.0

• The icon is a clock now, reading five past five: five o'clock is the 25 minutes of a pomodoro and one o'clock the 5 of a break.
• The end sound can repeat up to ten times, with a separate setting for the pomodoro and for the break.
• The screen stays awake while your phone is charging.
• Fixed: the timer ran off the screen with the phone on its side.

No accounts, no cloud, no ads, no tracking.
</en-US>
```

---

## 1.2.0 (versionCode 5) — 2026-08-10 · no publicada por separado

Redactado en su día y **absorbido por el bloque de la 1.3.0**, que es el que se envía. Se conserva como
registro.

### es-ES (425 caracteres)

```text
La pantalla se mantiene encendida mientras el móvil carga, así puedes ver la cuenta atrás sin tocar nada. Se puede poner en «nunca» o «siempre» en Ajustes.

Corregido: el temporizador se salía de la pantalla con el móvil girado y «Saltar» quedaba fuera. El icono ya no aparece dentro de un círculo negro sobre fondos claros.

Además: «Reiniciar» y «Saltar» se ven en blanco, y hay una opción nueva para escribirme por correo.
```

### en-US

```text
The screen stays awake while your phone is charging, so you can watch the countdown without touching anything. You can set it to never or always in Settings.

Fixed: the timer ran off the screen with the phone on its side and «Skip» was out of reach. The app icon no longer sits inside a black circle on light wallpapers.

Also: «Reset» and «Skip» are white now, and there is a new entry to email me.
```

---

## 1.0.0 (versionCode 2) — 2026-07-28

### es-ES

```text
Primera versión. Temporizador Pomodoro con widget de una sola casilla, estadísticas, avisos configurables y tema oscuro. Sin anuncios ni seguimiento.
```

### en-US

```text
First release. Pomodoro timer with a one-cell home screen widget, statistics, configurable alerts and a dark theme. No ads, no tracking.
```
