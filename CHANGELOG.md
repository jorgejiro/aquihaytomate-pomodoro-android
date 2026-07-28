# Changelog

Historial de versiones de ¡Aquí hay tomate! Este archivo es la fuente de verdad del changelog del
repositorio; el que se muestra dentro de la app vive en los `string-array` `changelog_*` de
`app/src/main/res/values/strings.xml` y `values-es/strings.xml`, indexados desde
`ui/changelog/ChangelogCatalog.kt`.

Al publicar una versión nueva hay que tocar los cuatro sitios: este archivo, los dos
`string-array` (EN y ES), el catálogo y el `versionCode`/`versionName` de `app/build.gradle.kts`.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y las versiones
[Semantic Versioning](https://semver.org/lang/es/).

## [No publicado] — 1.0.0 (versionCode 1)

Primera versión. En desarrollo; ver el roadmap por fases en `CLAUDE.md` §9.

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
- Auto-inicio en dos ajustes independientes: **auto-iniciar el descanso** al terminar un pomodoro y
  **auto-iniciar el pomodoro** al terminar un descanso. Las dos direcciones no son la misma decisión:
  el descanso conviene que arranque solo, y el pomodoro siguiente casi nunca, porque un descanso se
  alarga a propósito. En ambos casos el aviso suena y vibra igual.
- Onboarding en el primer arranque.
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
