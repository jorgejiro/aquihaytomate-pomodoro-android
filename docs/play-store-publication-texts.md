# Publicación en Google Play — ¡Aquí hay tomate!

> Textos y checklist para la ficha de Google Play. Cuenta de desarrollador: la misma que Bebe Agua.
>
> Estado: **v1.0.0 (versionCode 2), lista para subir** · Última revisión: 2026-07-28
>
> El autor ha revisado la app en su Galaxy S25 —temporizador, widget, notificación y aviso al Garmin— y da
> el visto bueno para publicar. Las 0.9.0 y 0.9.1 fueron versiones internas que nunca se subieron.

---

## 1. Checklist para publicar

> Estado a 2026-07-28: firma, bundle y textos listos, y la app revisada en un Galaxy S25 real. Lo que
> **sigue sin probarse en dispositivo** son los escenarios de resiliencia de §10 que exigen tiempo o
> condiciones raras —pantalla apagada un slot entero, reinicio a mitad, batería restringida, otros
> launchers— y por eso la recomendación es **subir primero a pruebas internas** y comprobarlos sobre la
> build de Play, que lleva R8. Ver `docs/f9-verificacion-en-emulador.md`.

### Antes de la primera subida

1. **Enviar la declaración de foreground service `specialUse`** (§14). Es revisión manual y puede
   tardar o rebotar. **Hazlo en la primera subida a pruebas internas, no en la de producción**, para
   que el rechazo, si llega, no bloquee el lanzamiento.
2. ✅ **Firma configurada.** Se reutiliza la **misma upload key que Bebe Agua** — misma cuenta de Play,
   mismo prefijo `com.jjrapps` — porque una clave de subida puede firmar varias apps y así hay un solo
   secreto que custodiar en vez de dos:

   | | |
   |---|---|
   | Keystore | `/Users/jorge/dev/bebe-agua-android/bebeagua-release.jks` |
   | Alias | `bebeagua` |
   | Contraseñas | en `bebeagua-release.jks.pwd.txt`, junto al keystore, **fuera de todo repo** |
   | Config de este proyecto | `keystore.properties` en la raíz, con **ruta absoluta** al keystore |
   | Certificado | válido hasta 2051-04-29 |

   `keystore.properties` **no se versiona** y `.gitignore` ya cubre `keystore.properties`, `*.jks`,
   `*.pwd.txt`, `*.aab` y `/app/release/`. Si algún día mueves el keystore, la ruta absoluta de este
   fichero es lo único que hay que tocar; conviene entonces llevarlo a un sitio neutral —
   `~/keys/jjrapps-upload.jks` — y apuntar los dos proyectos ahí.
3. **Activar Play App Signing** al crear la app en Play Console (viene activado por defecto). Con eso
   la clave de firma real la custodia Google y esta upload key es reemplazable si se pierde.
5. **Rellenar la ficha con lo que ya está hecho.** Todo lo gráfico y todos los textos están en el repo,
   así que este paso es copiar y subir:

   | Campo de Play Console | De dónde sale |
   |---|---|
   | Icono de la aplicación | `docs/store-assets/icono-play-512.png` (§9) |
   | Gráfico de funciones | `docs/store-assets/grafico-de-funciones-1024x500.png` (§9) |
   | Capturas de teléfono y de tablet | los seis juegos de `docs/store-assets/capturas/` (§8): teléfono, tablet de 7" y de 10", en ES y EN |
   | Nombre, descripción corta y completa | §2, §3 y §4, en ES y EN |
   | Política de privacidad | la URL de `docs/web/aqui-hay-tomate.html` una vez subida (§11) |
   | Data safety | §10 |
   | Declaración de permisos | §14 |

### Antes de cada publicación

5. Pasar la **checklist de resiliencia** de `CLAUDE.md` §10 en Android 12, 14 y 16.
6. Pasar la **checklist del widget** en Nova Launcher, Pixel Launcher y One UI.
7. Probar manualmente los ocho escenarios: onboarding · permiso de notificaciones · permiso de
   alarmas exactas denegado · ciclo completo de 4 pomodoros · acciones de la notificación · widget
   con toque simple y doble · cambio de idioma en caliente · estadísticas con datos y vacías.
8. Probar los avisos con silencio, modo vibración, DND total y DND prioritario.
9. Verde en:
   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lint test
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew connectedDebugAndroidTest
   ```
10. Generar el `.aab` firmado con la upload key:
   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew bundleRelease
   # app/build/outputs/bundle/release/aquihaytomate-<versionName>-vc<versionCode>-release.aab
   ```
   Comprobar que sale firmado, que es lo que un `assembleRelease` sin `keystore.properties` no avisa, y
   **que la huella es la de la upload key de esta app**:
   ```bash
   "$JAVA_HOME/bin/keytool" -printcert -jarfile app/build/outputs/bundle/release/aquihaytomate-*-release.aab
   # SHA1 esperado: 9E:D3:7C:F1:51:26:CB:D2:21:45:3B:9B:94:B5:4D:B0:E4:67:F3:36
   ```

   > **Cuidado con subir el bundle de Bebe Agua.** Pasó al publicar la 1.2.0: las dos apps viven en
   > carpetas hermanas de `~/dev`, su artefacto se llamaba igual —`app-release.aab`— y el diálogo de
   > subida recuerda la última carpeta. Play lo rechaza con «tu Android App Bundle está firmado con la
   > clave incorrecta», porque comparten cuenta y carpeta **pero no clave de firma**: Bebe Agua es
   > `A0:1B:59:6B…` y esta app es `9E:D3:7C:F1…` —aunque el keystore se llame `bebeagua-release.jks` y su
   > alias sea `bebeagua`, heredado de aquel proyecto—. Por eso el `archivesName` de `build.gradle.kts`
   > lleva ahora nombre y versión.
11. Subir a **Internal testing** primero. Instalar desde Play en un dispositivo real y repetir 5 y 6:
    la build de Play lleva R8 y puede romper cosas que el debug no.
12. Promover a producción. Revisar antes: ficha, países, categoría, data safety, content rating,
    declaración de permisos y precio.
13. Enviar a revisión.

---

## 2. Nombre de la app

| | |
|---|---|
| **En el dispositivo** | ¡Aquí hay tomate! |
| **En Google Play (ES)** | ¡Aquí hay tomate! - Pomodoro timer |
| **En Google Play (EN)** | ¡Aquí hay tomate! - Pomodoro timer |

El nombre se mantiene en español en ambos idiomas: es la marca.

---

## 3. Descripción corta (máx. 80 caracteres)

**ES**
> Temporizador Pomodoro con widget de una sola casilla. Oscuro, rápido y sin ruido.

*(78 caracteres)*

**EN**
> Pomodoro timer with a one-cell home screen widget. Dark, fast and quiet.

*(71 caracteres)*

---

## 4. Descripción completa

### ES

> **Un temporizador Pomodoro que cabe en una casilla.**
>
> Todas las apps de pomodoro traen un widget de 2×2 como mínimo, y te comen el escritorio.
> «¡Aquí hay tomate!» trae uno de **una sola casilla**: un toque para empezar o pausar, dos toques
> para reiniciar. Sin abrir la app, sin buscar nada.
>
> Trabaja 25 minutos, descansa 5, y cada cuatro pomodoros tómate un descanso largo. O configúralo
> como te dé la gana: todas las duraciones son tuyas.
>
> **Fiable de verdad.** Suena en el segundo correcto con la pantalla apagada, después de cerrar la
> app desde recientes y después de reiniciar el móvil. Un temporizador que llega tarde no sirve
> para nada.
>
> **Y bonito.** Fondo negro y un tomate rojo que se va vaciando conforme pasa el tiempo. Nada de
> botones enormes ni de interfaces de juguete.
>
> **Funciones principales**
>
> • Widget de escritorio de 1×1: iniciar, pausar y reiniciar sin abrir la app
> • Ciclos completos de enfoque, descanso corto y descanso largo
> • Todas las duraciones configurables, y también cuántos pomodoros por ciclo
> • Estadísticas: pomodoros completados, tiempo enfocado, racha de días, mapa mensual y tendencia
> • Aviso al terminar con sonido a elegir y vibración de la duración que tú decidas
> • Auto-iniciar el descanso, el pomodoro siguiente, o ninguno de los dos
> • Notificación con el descuento y botones de pausar, saltar y reiniciar
> • Respeta el modo silencio y No molestar
> • Español e inglés
>
> **Sin cuentas. Sin nube. Sin anuncios. Sin seguimiento.** Todos tus datos se quedan en el móvil.
>
> El nombre es un juego: *pomodoro* es «tomate» en italiano, que es de donde viene el nombre de la
> técnica. Y sí, también es un guiño a aquel anuncio de tomate frito.

### EN

> **A Pomodoro timer that fits in one cell.**
>
> Every Pomodoro app ships a 2×2 widget at minimum, and it eats your home screen. "¡Aquí hay
> tomate!" ships a **one-cell** widget: tap to start or pause, double-tap to reset. No app to open,
> nothing to hunt for.
>
> Work for 25 minutes, rest for 5, and take a long break every four Pomodoros. Or set it up however
> you like — every duration is yours.
>
> **Actually reliable.** It fires on the right second with the screen off, after you swipe the app
> away, and after a reboot. A timer that runs late is no timer at all.
>
> **And good-looking.** A black background and a red tomato that drains as time passes. No giant
> buttons, no toy interface.
>
> **Main features**
>
> • 1×1 home screen widget: start, pause and reset without opening the app
> • Full cycles of focus, short break and long break
> • Every duration configurable, plus how many Pomodoros per cycle
> • Statistics: completed Pomodoros, time focused, day streak, monthly map and trend
> • End-of-slot alert with a sound of your choice and vibration for as long as you want
> • Optional auto-start of the next slot
> • Notification with the countdown and pause, skip and reset buttons
> • Respects silent mode and Do Not Disturb
> • Spanish and English
>
> **No accounts. No cloud. No ads. No tracking.** All your data stays on your phone.
>
> The name is a Spanish pun: *pomodoro* is Italian for tomato, which is where the technique's name
> comes from. And yes, it also nods to an old tinned-tomato commercial.

---

## 5. Categoría y etiquetas

- **Categoría**: Productivity (Productividad).
- **Etiquetas sugeridas**: pomodoro, timer, focus, productivity, widget, study.
- **Sin categoría secundaria.**

---

## 6. Texto promocional / tagline

**ES** — Un pomodoro que cabe en una casilla del escritorio.
**EN** — A Pomodoro timer that fits in a single home screen cell.

---

## 7. Novedades de esta versión

**Los textos viven en [`play-release-notes.md`](play-release-notes.md)**, con el bloque de cada versión y
el formato con etiquetas `<es-ES>` / `<en-US>` listo para pegar en Play Console. Están ahí y no aquí
porque son lo único de la ficha que cambia en cada publicación, y tenerlos en dos sitios acaba en dos
versiones distintas del mismo texto. Misma separación que en Bebe Agua.

---

## 8. Capturas — **36 listas en el repo**

**El pomodoro va primero, y el widget después del historial.** Seis escenas, en este orden:

| # | Fichero | Qué muestra |
|---|---|---|
| 1 | `01-pomodoro-en-marcha.png` | Un pomodoro corriendo: tomate al 70 %, 17:30, los tres controles y «a continuación» |
| 2 | `02-descanso.png` | El descanso: el mismo tomate en ámbar, con la fase escrita debajo |
| 3 | `03-ajustes.png` | Configuración: duraciones, los dos auto-inicios, avisos y widget |
| 4 | `04-historial.png` | Estadísticas con 45 días sembrados: hoy, la semana y el mapa del mes |
| 5 | `05-fin-del-intervalo.png` | Fin de intervalo: el tomate vacío, «¡Tiempo!» y la invitación a seguir |
| 6 | `06-widget-en-el-escritorio.png` | El widget de 1×1 en un escritorio limpio, junto al dock para que se aprecie el tamaño |

Están en `docs/store-assets/capturas/`, en **tres formatos por dos idiomas**:

| Carpeta | Resolución | Aspecto | Requisito de Play |
|---|---|---|---|
| `telefono/{es,en}` | 1080 × 2400 | 9:20 | lados de 320 a 3840 px |
| `tablet-7-pulgadas/{es,en}` | 1080 × 1920 | **9:16** | 9:16 o 16:9, lados de 320 a 3840 px |
| `tablet-10-pulgadas/{es,en}` | 1440 × 2560 | **9:16** | 9:16 o 16:9, lados de 1080 a 7680 px |

Las de tablet cumplen el 9:16 exacto que Play valida. Las de teléfono van en la resolución nativa de un
móvil actual, que es 9:20: **si la consola pusiera problemas con esa proporción**, se regeneran en 9:16
cambiando la resolución del AVD y volviendo a ejecutar la herramienta.

Sin marcos de móvil, sin fondos de marketing y sin texto sobreimpreso: el fondo negro de la app ya destaca
sobre el blanco de la ficha. Si algún día se quiere el texto encima, va en Inter SemiBold blanco, abajo.

**No están hechas a mano**: las genera `docs/store-assets/generar-capturas/`, con su propio README. Se
rehacen con un par de órdenes cuando cambie una pantalla, y así no vuelve a colarse una captura en el
idioma equivocado. Lo que hace la herramienta, en corto: siembra 45 días de historial para que Estadísticas
no salga vacía, escribe el estado del temporizador para que el tomate salga a media asta sin esperar ocho
minutos por escena, y coloca el widget en la segunda página del escritorio, que es la que el launcher deja
sin «At a glance» —el único texto del sistema que se colaría en otro idioma—.

> La captura de Estadísticas **sale con datos sembrados, no reales**. Es representativa de lo que se ve tras
> un mes y medio de uso; si prefieres las tuyas, sustituye `04-historial.png` por una captura de tu S25.

---

## 9. Icono de la ficha y gráfico de funciones — **ya generados**

Los dos están hechos, con las medidas y el formato que Play valida al subirlos:

| Asset | Fichero | Formato |
|---|---|---|
| Icono de la app | `docs/store-assets/icono-play-512.png` | 512 × 512, PNG de 32 bits con el alfa opaco, 33 KiB (el máximo es 1 MB) |
| Gráfico de funciones | `docs/store-assets/grafico-de-funciones-1024x500.png` | 1024 × 500, PNG sin alfa, 60 KiB (el máximo es 15 MB) |

Los sube en Play Console → **Presencia en Play → Ficha de Play principal**, en «Icono de la aplicación»
y «Gráfico de funciones».

**No son un diseño paralelo: los genera el código de la app.**

```bash
python3 docs/store-assets/generar-assets.py
```

El script reproduce los `pathData` de `res/drawable/ic_launcher_*.xml` y la onda de
`domain/render/TomatoGeometry.kt`, con los colores de `ui/theme/Color.kt`, y al terminar comprueba
tamaño, modo y peso de los dos ficheros. Si cambia el icono o la paleta, se ejecuta otra vez en vez de
repintarlos a mano. Solo necesita Pillow.

Dos cosas que conviene saber si se toca:

- **El icono recorta la ventana visible de 72 dp** del lienzo de 108, no escala el lienzo entero. Un
  icono adaptativo solo muestra esos 72 dp centrales, así que escalando los 108 el tomate saldría un
  tercio más pequeño en la ficha que en el escritorio del móvil, y es en la lista de Play donde se
  comparan los dos.
- **El alfa se conserva pero opaco.** Play pide «PNG de 32 bits» y aplica su propia máscara redondeada:
  sin canal alfa unas herramientas se quejan, y con un píxel translúcido en una esquina se vería una
  muesca. El script falla si detecta transparencia.

La composición está especificada en `docs/design-spec.md` §9, incluida la única desviación —el nombre en
dos líneas, porque a 68 px no cabe en una— y por qué se resolvió así.

---

## 10. Data safety — respuestas

| Pregunta | Respuesta |
|---|---|
| ¿La app recoge o comparte datos de usuario? | **No** |
| ¿Los datos se transmiten cifrados? | No aplica: no se transmite nada |
| ¿Puede el usuario solicitar la eliminación de sus datos? | Sí: desinstalar la app borra todo |
| ¿Hay recogida de datos obligatoria? | No |
| ¿Se usan identificadores de publicidad? | No |
| ¿Hay SDK de terceros que recojan datos? | No |

La app no tiene red. No declara el permiso `INTERNET`.

---

## 11. Política de privacidad

**Ya está hecha y lista para subir**: `docs/web/aqui-hay-tomate.html`, un HTML autocontenido —sin
dependencias externas, sin fuentes remotas— con la misma estructura bilingüe que la de Bebe Agua en
`jorgejiro.es`, selector Español/English y enlace de vuelta al sitio.

1. Súbela a tu web como `aqui-hay-tomate.html`, junto a `bebe-agua.html`.
2. En Play Console → Contenido de la aplicación → Política de privacidad, pon la URL resultante:
   **`https://www.jorgejiro.es/aqui-hay-tomate.html`**.
3. Si algún día cambia cómo se tratan los datos, actualiza la página **y** la fecha de entrada en vigor que
   figura en las dos versiones.

Detalles que conviene no romper al editarla: enumera **los siete permisos exactos del manifest** y afirma
que la app no declara `INTERNET`, que es comprobable por cualquiera en la propia ficha de Play. Si en el
futuro se añadiera un permiso, hay que añadirlo aquí. Como en la de Bebe Agua, no aparece ningún correo: se
remite al que Play muestra en la ficha.

> **La sección «Conservación de datos» no es opcional: no la quites.** Google rechazó una actualización de
> Bebe Agua con este motivo —«No se especifica una política de conservación de datos. Indica tus prácticas
> de conservación de datos en la política o declara explícitamente en ella que no almacenas ni conservas
> datos de usuario»— y esta política tenía el mismo hueco: contaba que los datos se quedan en el dispositivo
> y cómo borrarlos, pero en ninguna parte decía **cuánto tiempo se conservan ni que el desarrollador no
> conserva nada**. No basta con que se deduzca de «no se recoge nada»: el revisor busca la palabra. La
> sección lo dice en dos frases, una por lado — conservación cero del lado del desarrollador, y en el
> dispositivo hasta que el usuario decida.

Texto base, por si hiciera falta reescribirla:

### EN

> **Privacy Policy — ¡Aquí hay tomate!**
>
> Effective date: *(fecha de publicación)*
>
> **Data stored on your device.** The app stores your timer settings and your Pomodoro history
> (start and end times, duration, whether the interval was completed) locally on your device. This
> data never leaves your phone.
>
> **Data collection.** The app collects no personal data. It has no analytics, no crash reporting
> with telemetry, no advertising identifiers and no third-party SDKs that collect data.
>
> **Network access.** The app does not request the INTERNET permission and cannot transmit anything.
>
> **Data sharing.** No data is shared with anyone, because no data leaves the device.
>
> **Permissions.** Notifications (to show the timer and alert you when an interval ends), exact
> alarms (optional, improves timer accuracy), vibration, foreground service and wake lock (to keep
> the countdown accurate while it runs), and receive boot completed (to restore a running timer
> after a reboot).
>
> **Deleting your data.** Uninstalling the app deletes all stored data permanently.
>
> **Contact.** *(correo de contacto)*

### ES

> **Política de privacidad — ¡Aquí hay tomate!**
>
> Fecha de entrada en vigor: *(fecha de publicación)*
>
> **Datos guardados en el dispositivo.** La app guarda tus ajustes del temporizador y tu historial
> de pomodoros (hora de inicio y fin, duración y si el intervalo se completó) localmente en tu
> dispositivo. Estos datos nunca salen de tu móvil.
>
> **Recogida de datos.** La app no recoge ningún dato personal. No tiene analítica, ni informes de
> fallos con telemetría, ni identificadores publicitarios, ni SDK de terceros que recojan datos.
>
> **Acceso a la red.** La app no declara el permiso INTERNET y no puede transmitir nada.
>
> **Compartición de datos.** No se comparte ningún dato con nadie, porque ningún dato sale del
> dispositivo.
>
> **Permisos.** Notificaciones (para mostrar el temporizador y avisarte al terminar un intervalo),
> alarmas exactas (opcional, mejora la precisión del temporizador), vibración, servicio en primer
> plano y bloqueo de suspensión (para que la cuenta atrás sea precisa mientras corre), y recepción
> de arranque completado (para restaurar un temporizador en marcha tras reiniciar).
>
> **Eliminación de datos.** Desinstalar la app borra todos los datos guardados de forma permanente.
>
> **Contacto.** *(correo de contacto)*

---

## 12. Content rating

Cuestionario de IARC, respuestas esperadas: sin violencia, sin contenido sexual, sin lenguaje
soez, sin sustancias, sin juego, sin compras integradas, sin contenido generado por usuarios, sin
compartición de ubicación, sin interacción entre usuarios. Resultado esperado: **PEGI 3 / Everyone**.

---

## 13. Público, anuncios y acceso

- **Público objetivo**: 13+ / general. No es una app infantil, no entra en Families.
- **Anuncios**: No.
- **Compras integradas**: No.
- **Acceso a la app**: todo el contenido es accesible sin login. No hay credenciales que dar al
  revisor.

---

## 14. Declaración de permisos

### `FOREGROUND_SERVICE_SPECIAL_USE` — texto para el formulario

Este es el que hay que enviar y mantener. El manifiesto declara además
`<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="pomodoro_countdown_timer" />`.

**Vídeo demostrativo, enviado con la 1.1.0**: <https://youtube.com/shorts/cQ_qm63iubo>

El formulario exige un vídeo que muestre la funcionalidad, y **es el campo que más frena la revisión**.
Este ya está grabado y publicado: el pomodoro en marcha, la app en segundo plano, la notificación con la
cuenta atrás y sus controles. Reutilízalo en las siguientes subidas en vez de grabar otro, y **no lo borres
ni lo pases a privado**: Google puede volver a revisarlo, y un enlace muerto en una declaración enviada es
un rechazo. En YouTube tiene que estar como *no listado* o público — *privado* no vale, porque entonces el
revisor tampoco lo ve.

Respuestas del formulario, por si vuelve a pedirlas:

| Pregunta | Respuesta |
|---|---|
| ¿Usa la app permisos de servicio en primer plano? | **Sí** |
| Tipo | **Uso especial** (`specialUse`), subtipo `pomodoro_countdown_timer`. Ningún otro: no hay `camera`, `location`, `mediaPlayback` ni `dataSync` |
| Descripción de la funcionalidad | el texto **EN** de abajo |
| Por qué no vale una alternativa | el segundo párrafo del texto **EN**: ni `shortService` (3 min contra intervalos de hasta 3 h) ni WorkManager, que no da precisión al segundo ni notificación con controles |
| Vídeo | el enlace de arriba |

**EN**

> The app is a user-initiated Pomodoro countdown timer. When the user explicitly starts a focus or
> break interval, a foreground service keeps the countdown accurate and shows an ongoing,
> user-controllable notification (pause / skip / reset) until the interval ends or the user stops it.
>
> No other foreground service type applies: there is no media playback, no data sync, no location,
> no camera and no microphone, and `shortService` is capped at 3 minutes while intervals can last up
> to 3 hours.
>
> The service runs only while a timer is actually running. It is stopped as soon as the interval
> finishes, is paused, or is cancelled, and the app does no background work otherwise.

**ES** (por si lo piden en la comunicación de soporte)

> La app es un temporizador Pomodoro de cuenta atrás iniciado por el usuario. Cuando el usuario
> inicia explícitamente un intervalo de enfoque o de descanso, un servicio en primer plano mantiene
> la precisión de la cuenta atrás y muestra una notificación persistente controlable (pausar /
> saltar / reiniciar) hasta que el intervalo termina o el usuario lo detiene.
>
> Ningún otro tipo de servicio en primer plano encaja: no hay reproducción multimedia, ni
> sincronización de datos, ni ubicación, ni cámara, ni micrófono, y `shortService` está limitado a
> 3 minutos mientras que los intervalos pueden durar hasta 3 horas.
>
> El servicio solo corre mientras hay un temporizador en marcha. Se detiene en cuanto el intervalo
> termina, se pausa o se cancela, y la app no hace ningún otro trabajo en segundo plano.

### `SCHEDULE_EXACT_ALARM`

**EN**

> Used as a backstop so a running Pomodoro interval still ends on time if the operating system or
> the device manufacturer kills the app's process. The permission is optional: it is requested
> during onboarding, the app checks `canScheduleExactAlarms()` before scheduling, and falls back to
> an inexact alarm when it is not granted. The timer remains fully functional without it.
>
> `USE_EXACT_ALARM` is deliberately **not** declared.

### Resto de permisos

| Permiso | Justificación |
|---|---|
| `POST_NOTIFICATIONS` | Mostrar el temporizador en curso y el aviso de fin de intervalo |
| `FOREGROUND_SERVICE` | Requisito del servicio en primer plano descrito arriba |
| `RECEIVE_BOOT_COMPLETED` | Restaurar el estado de un temporizador en marcha tras reiniciar |
| `VIBRATE` | Aviso configurable al terminar un intervalo |
| `WAKE_LOCK` | Mantener la CPU despierta mientras corre la cuenta atrás con la pantalla apagada. Acotado con timeout a la duración restante + 5 s |

**No se declara**: `INTERNET`, `USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, ni ningún otro
`foregroundServiceType`.

---

## 15. Declaración corta para soporte o revisión

**ES**
> ¡Aquí hay tomate! es un temporizador Pomodoro local. No recoge datos, no accede a internet, no
> tiene anuncios ni compras. El servicio en primer plano solo corre mientras el usuario tiene un
> temporizador en marcha, iniciado por él, y muestra una notificación persistente con controles.

**EN**
> ¡Aquí hay tomate! is a local Pomodoro timer. It collects no data, has no internet access, no ads
> and no purchases. The foreground service runs only while the user has a timer running that they
> started themselves, and it shows an ongoing notification with controls.
