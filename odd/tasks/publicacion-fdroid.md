# Publicación en F-Droid

## Objetivo

Dejar ¡Aquí hay tomate! lista para entrar en el catálogo principal de F-Droid: el repositorio cumple sus
requisitos y la receta para `fdroiddata` está escrita y validada. Replica lo hecho en Sleep Noise
(`/Users/jorge/dev/sleep-noise-android`, merge request fdroiddata !50449).

## Por qué

La app es MIT, no pide el permiso de Internet ni usa servicios de Google, y el público de F-Droid es
justo el que valora eso.

## Alcance y restricciones

- F-Droid compila desde el código y **firma con su clave**: una instalación de Play y una de F-Droid no
  se actualizan entre sí. Las builds reproducibles con la firma propia quedan fuera.
- El merge request a `gitlab.com/fdroid/fdroiddata` se abre con `glab`, que tiene sesión iniciada como
  `jorgejiro` con el token en el llavero del sistema (no en Sleep Noise, cuyo MR se abrió desde la web).
  **Cualquier operación remota (push a GitHub, fork, rama y MR en GitLab) espera la confirmación
  explícita de Jorge.**
- TDD: no aplica (cambios de build y metadatos). Checks: `./gradlew lint test`, `assembleRelease` sin
  `keystore.properties` y `fdroid lint` sobre la receta.
- Rama `feat/publicacion-fdroid`. Estrategia de entrega: `ask-on-risk`; previsión muy por debajo de
  400 líneas escritas (las imágenes son copias binarias).

## Tareas

- [x] T1 · Build: quitar `dependenciesInfo` del APK/AAB y el plugin `foojay-resolver` de
  `settings.gradle.kts`; `assembleRelease` compila sin `keystore.properties`. Ruta: delegada (un
  escritor para todo el lote, disparador de escritura: 2+ ficheros no triviales).
- [x] T2 · Metadatos fastlane `fastlane/metadata/android/{en-US,es-ES}`: título, descripciones,
  changelog del versionCode 8, icono, gráfico y capturas de teléfono. Ruta: delegada.
- [x] T3 · Receta `docs/fdroid/com.jjrapps.aquihaytomate.yml` validada con `fdroid lint`, guía
  `docs/fdroid/LEEME.md`, y regla del changelog de fastlane en `CLAUDE.md` §2.7 / `CHANGELOG.md`.
  Ruta: delegada.

## Progreso

- T1 · `235ee7a` build(fdroid): quita del APK el bloque de dependencias cifrado para Google.
  `28598aa` build(fdroid): quita el plugin foojay, que F-Droid rechaza.
  Verificado: `lint test` en verde en el repo real. Clon limpio de la rama en el scratchpad,
  `local.properties` copiado (solo `sdk.dir`), sin `keystore.properties`; `assembleRelease` produce
  `aquihaytomate-1.4.0-vc8-release-unsigned.apk`. `apksigner verify` confirma que no lleva firma
  (`Missing META-INF/MANIFEST.MF`), así que no hay bloque de firma que inspeccionar para
  `dependenciesInfo` — es coherente: ese bloque solo se añade al firmar, y lo que importa es que ya
  no se genera en el `.aab` que sí se firma para Play.

- T2 · `cfac7a2` docs(fdroid): metadatos fastlane en inglés y español. Título real de Play
  «¡Aquí hay tomate! - Pomodoro» (no «- Pomodoro timer», que es el que documenta
  `docs/play-store-publication-texts.md` y no cabía) en `en-US` y `es-ES`. Descripciones cortas y
  completas tomadas de `docs/play-store-publication-texts.md` §3/§4, sin las marcas `> ` de cita ni
  negritas Markdown, con dos viñetas ajustadas a las funciones actuales (dos sonidos de aviso, uno
  por extremo del slot, y vibración configurable) en vez de la viñeta simplificada del texto de
  Play — alineación menor, no una función inventada. Changelog del `versionCode` 8 en
  `changelogs/8.txt` tomado de `docs/play-release-notes.md` §1.4.0, sin la cabecera «Novedades de
  la versión / What's new», que ya pone F-Droid. Capturas de teléfono en orden
  widget → enfoque → descanso → fin de slot → estadísticas → ajustes (no se copian capturas de
  tablet: Sleep Noise tampoco las llevó). Recuentos de caracteres:
  - `es-ES/short_description.txt`: 80 (límite 80; se quitó el punto final del texto de Play, que
    medía 81).
  - `en-US/short_description.txt`: 72.
  - `es-ES/full_description.txt`: 1690.
  - `en-US/full_description.txt`: 1488.
  - `es-ES/changelogs/8.txt`: 457. `en-US/changelogs/8.txt`: 413. (límite 500 en ambos)

- T3 · Receta `docs/fdroid/com.jjrapps.aquihaytomate.yml`, guía `docs/fdroid/LEEME.md`, y la regla
  del quinto sitio del changelog en `CLAUDE.md` §2.7 y `CHANGELOG.md`.
  - `Categories: [Timer]`. En `config/categories.yml` de `fdroiddata` **no existen** las categorías
    `Time` ni `Productivity` que sugería el encargo — se comprobó descargando ese fichero real de
    `gitlab.com/fdroid/fdroiddata` — así que se usó `Timer` («Interval timing, timekeeping,
    countdown»), la que describe la app con precisión. `Task` («to-do lists, task managers...») no
    encaja: la app no gestiona tareas.
  - `commit` apunta al sha completo de `28598aa` (segundo commit de T1, el que quita el plugin
    foojay), no al tag `v1.4.0`, igual que en Sleep Noise: el tag es anterior a los dos cambios de
    build que F-Droid exige.
  - Validación con `fdroidserver` 2.4.5 (`brew install fdroidserver`) en un directorio
    `fdroiddata`-símil del scratchpad (`metadata/com.jjrapps.aquihaytomate.yml` +
    `config/categories.yml` descargado de la rama `master` real de `fdroiddata`, con PNGs vacíos de
    relleno para los iconos de categoría que el propio `fdroid lint` necesita copiar). `fdroid lint`
    → 0 errores; un único aviso no bloqueante, `trailing spaces` en la línea `Changelog:`, que es el
    propio `fdroid rewritemeta` quien la introduce al partir esa URL en dos líneas (nuestro nombre de
    repo es más largo que el de Sleep Noise y supera el ancho que `rewritemeta` no envuelve). Se
    comprobó que `fdroid rewritemeta` dos veces seguidas es idempotente con ese aviso presente. El
    fichero final del repo es exactamente esa salida canónica de `rewritemeta`.
  - `docs/fdroid/LEEME.md` adaptado del de Sleep Noise: nombre de la app, `AutoName`, rama
    `com.jjrapps.aquihaytomate` del fork, commit `New app: ¡Aquí hay tomate!`, y la nota final sobre
    que nada en el build descarga herramientas por su cuenta (sin el bloque `sudo` de JDK, que Sleep
    Noise también retiró en su último ajuste).
  - `CLAUDE.md` §2.7: los «cuatro sitios» pasan a ser **cinco**, añadiendo
    `fastlane/metadata/android/{en-US,es-ES}/changelogs/<versionCode>.txt`. Misma actualización en
    `CLAUDE.md` §8 («cinco sitios de §2.7») y en la cabecera de `CHANGELOG.md`. Se comprobó con `rg`
    que no hay ninguna otra mención de «cuatro sitios» sobre este tema en el repo.

## Estado

Las tres tareas están completas y verificadas localmente. Falta lo que exige confirmación explícita
de Jorge y operación remota: empujar la rama `feat/publicacion-fdroid` a GitHub, hacer fork de
`fdroid/fdroiddata`, crear la rama `com.jjrapps.aquihaytomate` con la receta y abrir el merge request
con `glab`.

## Siguiente paso

Jorge confirma push de la rama a GitHub + MR en fdroiddata.
