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

## [No publicado]

### Corregido

- **El ciclo de pomodoros se reinicia al cambiar de día.** Un móvil que se coge de vez en cuando abría la
  app diciendo `3/4` con un descanso esperando, días después del último pomodoro: el ciclo vive en el
  estado persistido y nada lo envejecía. Ahora, al abrir la app, tocar el widget o arrancar el
  dispositivo, un temporizador **parado** que quedó de un día anterior se barre —ciclo a cero, siguiente
  slot de enfoque y tanda cerrada—; un pomodoro que se quedó pausado se da por abandonado y su tiempo
  enfocado se apunta como parcial en el día en que se hizo; y un slot que venció mientras corría se
  registra como siempre, pero sin dejar su ciclo atrás. **Un temporizador en marcha no se toca nunca**, y
  atrasar el reloj del sistema no borra el ciclo en curso. Ver
  `docs/decisions/015-el-ciclo-se-reinicia-al-cambiar-de-dia.md`.

## [1.3.0] — 2026-08-10 (versionCode 6)

Sube el minor por las dos repeticiones de sonido, que son funcionalidad nueva, pero la versión la define
el icono: la app llevaba un tomate que no decía en ningún momento que fuera un temporizador.

**Las novedades que se envían a Play cubren también las de la 1.2.0**, porque van a llegar juntas al
usuario.

### Añadido

- **El sonido de fin de slot se puede repetir de 1 a 10 veces seguidas**, con **dos ajustes
  independientes**, uno para el pomodoro y otro para el descanso, y por defecto **una sola vez** —lo que
  la app hacía antes—. Un aviso que suena una vez es fácil de perder si te has ido a la cocina, y cuántas
  repeticiones hacen falta depende del sonido, de la casa y de la persona. Que sean dos y no uno es el
  mismo argumento que ya tenían los dos sonidos: levantarse del escritorio y volver a él no cuestan lo
  mismo. El selector suena al tocarlo, con el sonido de ese extremo, porque «3 veces» no significa nada
  hasta oírlo. Ver el apéndice de `docs/decisions/012-*`.

### Cambiado

- **El icono lleva el dial de un reloj y marca las 5:05.** La aguja corta y gruesa señala las cinco, que
  en la escala de minutos son los **25 de un bloque de enfoque**; la larga y fina señala la una, que son
  los **5 del descanso corto**. Al quitar la placa negra en la 1.2.0 el icono se quedó además sin
  contorno —la silueta la dibujaba el recorte del launcher, no el icono—, y dibujar el dial dentro
  arregla las dos cosas sin volver a meter una placa que se lea como pegatina. **No hay marca a las 12**:
  caería debajo del rabillo, que hace de doce. El monocromo pasa a llevar el reloj calado, todavía en un
  solo path. Se renderizaron siete direcciones antes de elegir; están en `docs/propuestas-icono/` con su
  generador. Ver `docs/decisions/014-*`.
- **Los controles `PAUSAR` / `REINICIAR` / `SALTAR` van centrados** en el hueco que queda entre el tomate
  y la línea de «siguiente», en vez de colgar de la etiqueta de fase con todo el aire acumulado debajo. El
  tomate no se mueve: el peso que había bajo los controles se parte en dos iguales, uno a cada lado.

### Interno

- **Las actions de la CI suben a los majors que corren en Node 24** —`checkout` v7, `setup-java` v5,
  `upload-artifact` v7 y `setup-gradle` v5—, porque GitHub tiene deprecado Node 20 y avisaba en cada
  ejecución. `setup-gradle` se queda en la v5 a propósito: desde la v6 el cacheo lo hace un componente
  cerrado cuyos términos de uso no son MIT.

## [1.2.0] — 2026-08-10 (versionCode 5)

Sube el minor porque hay funcionalidad nueva —los tres modos de pantalla encendida y el correo al
autor—, aunque el peso de la versión está en dos fallos que solo se ven en un dispositivo real: el
temporizador no cabía girado y el icono se leía como una pegatina negra.

### Añadido

- **La pantalla se mantiene encendida mientras el móvil carga**, en la pantalla Temporizador. El
  interruptor de sí/no pasa a tres modos —nunca, mientras carga, siempre— y **mientras carga es el nuevo
  valor por defecto**: enchufado en el escritorio, la pantalla no cuesta batería y un temporizador que hay
  que despertar para leer es un temporizador que se deja de mirar. Ya no depende de que el reloj esté
  corriendo, así que pausar deja de apagar la pantalla. Quien tenía el interruptor encendido pasa a
  «siempre», que es lo que tenía. Ver `docs/decisions/013-*`.
- **Enviar comentarios al autor** desde Acerca de: abre la app de correo con el destinatario puesto y el
  nombre y la versión de la app en el asunto, porque un informe sin versión no se puede atender. La
  dirección no se imprime en la pantalla de Ajustes.

### Cambiado

- **Los controles `REINICIAR` y `SALTAR` pasan a blanco.** En `TextMuted` se leían como deshabilitados, y
  en una pantalla sin cajas el color es la única señal de que algo se puede pulsar. La jerarquía la marcan
  ahora el tamaño y el glifo del control primario.
- **`A CONTINUACIÓN:` se queda en `SIGUIENTE:`.** A 10 sp y con tracking, esa etiqueta pesaba más que el
  dato que introduce.
- **El widget sale sin placa negra por defecto.** El fondo sólido era el valor por defecto y solo parecía
  correcto porque el escritorio del autor tiene un fondo de pantalla negro: sobre cualquier otro, el widget
  se lee como una tarjeta negra con un tomate dentro, que es justo lo que descarta el «cero tarjetas, cero
  botones con relleno» del proyecto. El tomate es un círculo relleno y opaco, así que se recorta solo sobre
  cualquier fondo. Quien prefiera la placa la tiene en Ajustes → Widget, y a quien ya la hubiera elegido a
  mano no se le cambia.
- **El tomate pierde el cáliz.** Las tres hojas que se dibujaban en los descansos se solapaban entre ellas
  y se fundían en un bloque de base recta pintado por encima del contorno, así que parecían un recorte
  pegado al círculo y no parte de él. La fase la sigue diciendo la etiqueta escrita debajo.

### Corregido

- **El temporizador se salía de la pantalla con el móvil girado.** En horizontal quedan unos 370 dp de
  alto y la columna necesita más, así que `SALTAR` y los puntos de ciclo caían por debajo del borde
  inferior, sin scroll ni forma de alcanzarlos. Girado, el bloque se parte en dos columnas —tomate a la
  izquierda, controles a la derecha— y el diámetro se calcula sobre el alto disponible. El onboarding
  sufría lo mismo, con `EMPEZAR` fuera de alcance: sus páginas quedan centradas cuando caben y con scroll
  cuando no, lo que cubre además las escalas de fuente grandes.
- **El icono de la app se veía como una pegatina negra** sobre cualquier fondo de pantalla que no fuera
  oscuro: el fondo del icono adaptativo era casi negro y el tomate se dibujaba pequeño dentro de la zona
  segura. Como el launcher siempre pinta la capa de fondo y siempre recorta con su propia máscara, ahora
  el degradado rojo **es** el cuerpo del tomate y llena el lienzo; en la capa delantera quedan el rabillo
  y el brillo.
- **El asunto del correo de comentarios llegaba vacío en Gmail**, que resuelve `ACTION_SENDTO` leyendo la
  URI `mailto:` y descarta `EXTRA_SUBJECT`. Ahora viaja en los dos sitios.

## [1.1.0] — 2026-07-29 (versionCode 4)

Sube el minor y no el patch porque hay funcionalidad nueva: los dos sonidos. El `versionCode` va a 4 porque
el 3 se quedó en la 1.0.1, que nunca llegó a subirse a Play.

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
