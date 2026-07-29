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

## [Sin publicar]

### Añadido

- **Un sonido para acabar el pomodoro y otro para acabar el descanso**, configurables por separado. Por
  defecto, cuenco tibetano al terminar de trabajar y campana al terminar el descanso: acabar un pomodoro es
  una buena noticia y volver al tajo es una orden, así que no tienen por qué sonar igual. El cuenco es el
  más grave de los cuatro y decae solo; la campana es más brillante y suena 2,6 s, que es lo que hace que
  se oiga. Quien ya tenía un sonido elegido lo conserva en los dos extremos hasta que toque uno de los dos
  ajustes. Ver `docs/decisions/012-*`.
- **El selector de sonido suena al tocarlo** y se queda abierto para poder comparar, con un «Hecho» para
  cerrar. Antes cambiaba el sonido y se cerraba sin dejar oír nada, con lo que elegir entre cinco nombres
  era adivinar. Suena por el mismo camino que la alerta real, así que respeta el silencio y el volumen de
  alarma igual que ella.

## [1.0.1] — 2026-07-29 (versionCode 3)

Dos fallos visuales que salieron al preparar las capturas para la ficha de Play, ninguno de ellos en el
motor del temporizador. El `versionCode` sube a 3 porque el 2 ya se subió a Play con la 1.0.0.

### Corregido

- **El widget llevaba un contorno gris.** La placa negra tenía un filete de 1 dp al 14 % de blanco, puesto
  para que no se perdiera sobre un wallpaper oscuro. En el escritorio se leía como un borde sucio alrededor
  del tomate y no hacía falta: lo que tiene que verse es el tomate, que está siempre porque es el
  contenido. Sobre un wallpaper claro la placa negra ya se recorta sola.
- **El mapa del mes se comía la leyenda en pantallas anchas.** La rejilla se dibuja repartiendo el ancho
  entre las siete columnas, pero el hueco reservado para ella se calculaba con una celda fija de 40 dp: en
  un móvil de 411 dp se derramaba 39 dp sobre la leyenda y en una tablet de 800 dp, 317 dp, tapando también
  las rachas y el sparkline. En un móvil estrecho la diferencia era de tres dp, y por eso no se había
  visto. Ahora la altura sale del ancho real, con la misma aritmética que el dibujo, y `MonthHeatmapTest`
  lo fija.
- **La barra de estado desaparecía con el tema del sistema en claro.** `enableEdgeToEdge()` sin argumentos
  decide el color de los iconos según el tema **del sistema**, así que en un móvil con tema claro los
  pintaba oscuros: sobre el negro de la app no se veían ni la hora ni la batería. La app solo tiene tema
  oscuro, de modo que ahora los pide claros siempre.

## [1.0.0] — 2026-07-28 (versionCode 2)

Primera versión pública. Las 0.9.0 y 0.9.1 fueron versiones internas que nunca salieron del móvil del
autor; su contenido está incluido aquí.

### Añadido

- Temporizador Pomodoro con ciclos completos: enfoque, descanso corto y descanso largo cada N
  pomodoros. Todas las duraciones y el tamaño del ciclo son configurables.
- **Widget de escritorio de 1×1**: un toque inicia o pausa, un doble toque rápido reinicia. Lleva dibujado
  el glifo de lo que hará el siguiente toque, y al terminar un intervalo muestra el siguiente listo con su
  color y su duración. Es la funcionalidad diferencial del proyecto. Ver
  `docs/decisions/001-widget-con-remoteviews-y-chronometer.md`.
- **Notificación del temporizador con el tiempo restante en grande** y los tres controles —pausar,
  reiniciar y saltar— visibles sin desplegarla. La cifra va en rojo durante un pomodoro y en ámbar durante
  un descanso. Vuelve si se descarta por error con un intervalo en marcha. Ver
  `docs/decisions/009-*` y `010-*`.
- **Aviso al terminar cada intervalo** con sonido seleccionable y vibración de duración configurable
  (5 s por defecto), respetando el modo silencio y No molestar. Ver `docs/decisions/004-*`.
- **El aviso llega al reloj emparejado** con dos acciones: empezar el siguiente intervalo o descartar.
  Descartarlo desde la muñeca corta la vibración del móvil. Ver `docs/decisions/011-*`.
- Estadísticas e historial: pomodoros completados y tiempo enfocado por día, semana y mes, racha de
  días, mapa de calor mensual y evolución de los últimos 30 días. Todas las gráficas dibujadas a
  mano con Compose Canvas, ver `docs/decisions/005-*`.
- Auto-inicio en dos ajustes independientes: **auto-iniciar el descanso** al terminar un pomodoro —
  **activado por defecto** — y **auto-iniciar el pomodoro** al terminar un descanso, desactivado.
- Onboarding de cuatro páginas en el primer arranque, con las duraciones, la forma del ciclo y los dos
  permisos que el temporizador necesita. Ver `docs/decisions/008-*`.
- Pantalla «Novedades» accesible desde Ajustes → Acerca de.
- Interfaz en español e inglés, con selector de idioma en Ajustes. Solo tema oscuro.
- Sin cuentas, sin nube, sin anuncios y sin seguimiento: todo queda en el dispositivo.

### Interno

- Motor del temporizador híbrido en tres capas: estado persistido en DataStore como única fuente de
  verdad, `ForegroundService` de tipo `specialUse` como mecanismo primario y `AlarmManager` como red
  de seguridad. Ver `docs/decisions/002-*` y `006-*`.
- 192 tests unitarios y 43 instrumentados. R8 verificado sobre el artefacto de release.

---

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
  reiniciar y saltar— sin necesidad de desplegarla. La cifra va en rojo durante un pomodoro y en ámbar
  durante un descanso, porque el nombre de la fase no cabe sin cortarse.
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
