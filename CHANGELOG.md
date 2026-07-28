# Changelog

Historial de versiones de ¡Aquí hay tomate! Este archivo es la fuente de verdad del changelog del
repositorio; el que se muestra dentro de la app vive en los `string-array` `changelog_*` de
`app/src/main/res/values/strings.xml` y `values-es/strings.xml`, indexados desde
`ui/changelog/ChangelogCatalog.kt`.

Al publicar una versión nueva hay que tocar los cuatro sitios: este archivo, los dos
`string-array` (EN y ES), el catálogo y el `versionCode`/`versionName` de `app/build.gradle.kts`.

**El `versionCode` sube de uno en uno en cada subida a Play, incluso si el `versionName` solo cambia de
patch.** Play rechaza un bundle cuyo `versionCode` no sea mayor que el de la última subida, y no se puede
reutilizar ni bajando la versión.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y las versiones
[Semantic Versioning](https://semver.org/lang/es/).

## [0.9.1] — 2026-07-28 (versionCode 2)

Primera tanda de correcciones salidas de usar la app en un dispositivo real.

### Corregido

- **El widget no se podía colocar en el escritorio**: el launcher respondía «la app no está instalada».
  `widget_pomodoro_info.xml` declaraba `android:configure="false"`, y ese atributo no es un booleano sino
  el nombre de la Activity de configuración, así que el sistema intentaba abrir una clase llamada `false`.
- Los glifos del widget eran los caracteres `▸` y `❚❚` en una fuente que no los contiene, y el sistema
  pintaba una mota ilegible. Ahora se dibujan.
- Al terminar un slot, el widget mostraba un círculo vacío con un signo de exclamación que se leía como un
  error. Ahora muestra el slot siguiente listo: su color, su duración y el `▶`.
- La notificación del temporizador no volvía si se descartaba por error, aunque el pomodoro siguiera
  corriendo. Desde Android 13 se puede descartar la de un servicio en primer plano.
- Descartar el aviso de fin de slot desde un reloj emparejado no cortaba la vibración, que podía seguir
  hasta 30 segundos.
- La barra de pestañas y el onboarding se metían debajo de la barra de estado.

### Cambiado

- **La notificación del temporizador enseña el tiempo restante en grande** y los tres controles —pausar,
  reiniciar y saltar— sin necesidad de desplegarla.
- El aviso de fin de slot **llega al reloj emparejado** con sus dos acciones: empezar el siguiente o
  descartar. La notificación del cronómetro en curso ya no se envía al reloj.
- La pantalla del temporizador ofrece las tres acciones, con `SALTAR` que antes solo estaba en la
  notificación, y tocar el tomate inicia o pausa.
- **Auto-iniciar el descanso viene activado de fábrica.**
- El onboarding gana una página para la forma del ciclo, y la de permisos dice con claridad cuáles están
  concedidos y cuáles faltan.

---

## [0.9.0] — 2026-07-28 (versionCode 1)

Primera versión, publicada como **0.9.0** para pruebas: la funcionalidad de la 1.0 está completa,
pero la checklist de resiliencia en dispositivo real (`CLAUDE.md` §10) todavía no está pasada. Las
correcciones que salgan de esas pruebas van en 0.9.1, 0.9.2…; la **1.0.0** se reserva para cuando la
checklist esté cerrada.

### Añadido

- Temporizador Pomodoro con ciclos completos: enfoque, descanso corto y descanso largo cada N
  pomodoros. Todas las duraciones y el tamaño del ciclo son configurables.
- **Widget de escritorio de 1×1**: un toque inicia o pausa, un doble toque rápido reinicia, y lleva
  dibujado el glifo de lo que hará el siguiente toque (`▶` / `❚❚`). Muestra
  el tiempo restante y distingue enfoque de descanso por color. Es la funcionalidad diferencial del
  proyecto. Ver `docs/decisions/001-widget-con-remoteviews-y-chronometer.md`.
- Estadísticas e historial: pomodoros completados y tiempo enfocado por día, semana y mes, racha de
  días, mapa de calor mensual y evolución de los últimos 30 días. Todas las gráficas dibujadas a
  mano con Compose Canvas, ver `docs/decisions/005-graficas-con-compose-canvas-sin-vico.md`.
- Aviso al terminar cada slot con sonido seleccionable y **vibración de duración configurable en
  segundos** (5 s por defecto), respetando el modo silencio y No molestar. Ver
  `docs/decisions/004-alerta-propia-en-vez-de-sonido-de-canal.md`.
- Notificación persistente con el descuento, el aviso de lo que viene a continuación y las mismas tres
  acciones que la pantalla: pausar o reanudar, reiniciar y saltar.
- Auto-inicio en dos ajustes independientes: **auto-iniciar el descanso** al terminar un pomodoro —
  **activado por defecto** — y **auto-iniciar el pomodoro** al terminar un descanso, desactivado. Las dos direcciones no son la misma decisión:
  el descanso conviene que arranque solo, y el pomodoro siguiente casi nunca, porque un descanso se
  alarga a propósito. En ambos casos el aviso suena y vibra igual.
- Onboarding de cuatro páginas en el primer arranque, con las duraciones, la forma del ciclo y los dos
  permisos que el temporizador necesita.
- Pantalla «Novedades» accesible desde Ajustes → Acerca de.
- Interfaz en español e inglés, con selector de idioma en Ajustes.

### Interno

- Motor del temporizador híbrido en tres capas: estado persistido en DataStore como única fuente de
  verdad, `ForegroundService` de tipo `specialUse` como mecanismo primario y `AlarmManager` como red
  de seguridad. Ver `docs/decisions/002-motor-del-temporizador-hibrido.md`.
- Solo se persisten los slots de enfoque, ver
  `docs/decisions/003-solo-se-persisten-los-slots-de-enfoque.md`.
- Tema propio 100 % oscuro, sin Material dinámico, con tipografías Inter y Space Grotesk
  empaquetadas.
