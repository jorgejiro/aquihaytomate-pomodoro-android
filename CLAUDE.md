# CLAUDE.md — ¡Aquí hay tomate!

> Documento guía para Claude Code. Léelo antes de cualquier tarea no trivial. Si algo aquí entra en conflicto con la petición del usuario, pregunta antes de ejecutar.
>
> **Si vuelves al proyecto después de un tiempo, empieza por `docs/estado-del-proyecto.md`**: resume en qué punto está la app, los acuerdos de producto que no se deducen del código y las trampas de plataforma ya pagadas.

---

---

## 1. Visión del producto

**¡Aquí hay tomate!** es una app Android nativa de temporizador Pomodoro: bloques de trabajo enfocado alternados con descansos, con estadísticas de lo enfocado y un widget de escritorio de una sola casilla. Pensada para uso personal del autor y publicación en Google Play.

El nombre juega con el tomate (*pomodoro* en italiano, de donde viene el nombre de la técnica) y con el anuncio antiguo de la marca Orlando. No tiene nada que ver con el programa de televisión homónimo.

- **Nombre en la app**: ¡Aquí hay tomate!
- **Nombre en Google Play**: ¡Aquí hay tomate! - Pomodoro timer

### Principios de diseño

- **El widget es el producto.** Un widget de **1×1** para iniciar, pausar y reiniciar sin abrir la app. Es la razón de existir del proyecto: todas las apps de pomodoro del mercado tienen widgets de 2×2 como mínimo y ocupan demasiado escritorio.
- **Estética propia, no Material genérico.** Fondo negro, rojo tomate como color de acento, cero tarjetas, cero botones grandes con relleno. Los controles son texto. La app de referencia funcional ([Tomato](https://github.com/nsh07/Tomato)) se descartó justamente por su diseño Material 3 Expressive de botones enormes.
- **El temporizador nunca falla.** Suena en el segundo correcto con la pantalla apagada, tras matar la app desde recientes y tras reiniciar el móvil. Es la característica que separa un temporizador usable de un juguete.
- **Sin cuentas, sin nube, sin anuncios, sin tracking.** Todo local en el dispositivo.
- **Solo tema oscuro.** No hay tema claro ni conmutador. Es una decisión de producto, no una carencia.

### Referencia funcional

[Tomato](https://github.com/nsh07/Tomato) (`org.nsh07.pomodoro`). Nos interesa de él:

- Las estadísticas: minutos enfocados por día, gráficas semanales y mensuales.
- Los parámetros de temporizador configurables.

**No nos interesa**: su diseño Material 3 Expressive, sus botones grandes, ni su widget de 2×2.

---

## 2. Funcionalidad (v1.0, publicada)

### 2.1 Pantalla Temporizador (principal) ✅ Implementada en F3

- Un círculo grande relleno de rojo que **se va vaciando por abajo como un líquido**, con la superficie ondulando. Es el indicador de progreso del slot en curso.
- La cifra del tiempo restante centrada, **en negativo** donde el líquido la cubre (blanca sobre el hueco, negra sobre el rojo).
- Etiqueta de fase debajo: `ENFOQUE` / `DESCANSO` / `DESCANSO LARGO` / `¡TIEMPO!`.
- Control primario en texto, sin caja: `▸ INICIAR` / `❚❚ PAUSAR` / `▸ REANUDAR`. **Tocar el círculo hace lo mismo que el control primario**: es el objetivo táctil más grande de la pantalla.
- Controles secundarios `REINICIAR` y `SALTAR` en una fila debajo, con su hueco reservado para que el tomate no salte al aparecer. Son las tres acciones que tiene un pomodoro en marcha, y **saltar tiene que estar en la pantalla, no solo en la notificación**.
- Línea `SIGUIENTE: DESCANSO · 5 MIN`, de `SlotPlanner.upcomingSlot` — la misma función pura que alimenta la notificación ongoing.
- **En horizontal el bloque se parte en dos columnas** —tomate a la izquierda, controles a la derecha—, porque en vertical no cabía y `SALTAR` se salía de la pantalla. Ver `docs/design-spec.md` §5.1.
- Puntos de ciclo **pegados al borde inferior** (`● ● ○ ○   2/4`) indicando en qué pomodoro del ciclo vamos. No forman parte del grupo de controles.

### 2.2 Pantalla Estadísticas ✅ Implementada

- Cifras de hoy: pomodoros completados, tiempo enfocado, racha de días.
- Barras diarias de la semana, con línea de objetivo diario punteada.
- Mapa de calor mensual (7 columnas, 5 niveles de intensidad).
- Racha actual y mejor racha, con tira de los últimos 30 días.
- Sparkline de los últimos 30 días.

Todo dibujado con Compose `Canvas`. **Sin librería de gráficos** — ver `docs/decisions/005-graficas-con-compose-canvas-sin-vico.md`.

### 2.3 Pantalla Ajustes ✅ Implementada

| Ajuste | Por defecto | Rango |
|---|---|---|
| Duración de enfoque | **25 min** | 1–180 |
| Descanso corto | **5 min** | 1–60 |
| Descanso largo | **15 min** | 1–120 |
| Pomodoros por ciclo | **4** | 2–12 |
| **Auto-iniciar el descanso** (al terminar un pomodoro) | **activado** | — |
| **Auto-iniciar el pomodoro** (al terminar un descanso) | desactivado | — |
| **Sonido al terminar el pomodoro** | **Cuenco tibetano** | `silent`, `bell`, `bowl`, `digital`, `soft` |
| **Repetir ese sonido** (al terminar el pomodoro) | **1 vez** | 1–10 |
| **Sonido al terminar el descanso** | **Campana** | los mismos cinco |
| **Repetir ese sonido** (al terminar el descanso) | **1 vez** | 1–10 |
| **Duración de la vibración** | **5 s** | 0 (= desactivada) – 30 |
| **Mantener pantalla encendida** (en la pantalla Temporizador) | **Mientras carga** | Nunca / Mientras carga / Siempre |
| Objetivo diario de pomodoros | 8 | 1–24 |
| Fondo del widget | **Transparente** | Sólido / Transparente |
| Idioma | Auto | Auto / Español / English |

Más: estado de los permisos (notificaciones y alarmas exactas) con botón a los ajustes del sistema, refrescado en `onResume`; y **Acerca de** con **Enviar comentarios** —correo al autor con el nombre y la versión en el asunto; la dirección **no se imprime en la pantalla**, se ve en la app de correo al abrirse—, la versión instalada y acceso a **Novedades**.

### 2.4 Pantalla Onboarding ✅ Implementada

Cuatro páginas: qué es la técnica pomodoro · elige tus duraciones · **tu ciclo** · el widget y los dos permisos.

La tercera reúne los cuatro ajustes que deciden cómo se siente la app a lo largo de una mañana —pomodoros por ciclo, descanso largo y los dos auto-inicios— porque son los que conviene preguntar antes del primer pomodoro y no dejar enterrados en Ajustes. **Auto-iniciar el descanso viene activado**; el pomodoro siguiente, no.

En la tercera página los permisos son **filas con su estado escrito** (`Pendiente` en ámbar con `Necesario` debajo, o `Activado`), no botones planos, y **`EMPEZAR` está deshabilitado hasta que los dos estén concedidos**, con un `CONTINUAR SIN ELLOS` discreto como válvula de escape obligatoria. Ver `docs/decisions/008-el-onboarding-exige-los-dos-permisos.md`.

### 2.5 Widget de escritorio 1×1 ✅ Implementado

**La funcionalidad diferencial del proyecto.**

- Ocupa **una sola casilla** (`targetCellWidth/Height=1`, `resizeMode="none"`).
- Muestra los minutos restantes y el nivel de líquido como progreso. Distingue enfoque (rojo) de descanso (ámbar) por color.
- **Lleva dibujado el glifo de la acción que hará el toque**: `▶` parado o pausado, `❚❚` corriendo. Parado no muestra cifra, solo el `▶` a todo el tomate. Los glifos se pintan en el bitmap con `Canvas`, no se escriben como texto: Space Grotesk no tiene ni `▸` ni `❚`.
- **Al terminar un slot muestra el siguiente listo**: color de esa fase, su duración y el `▶`. Nunca un signo de alarma — en un escritorio se lee como un error. Qué muestra cada estado vive en la función pura `widget/WidgetReadout.kt`, con test.
- **Un toque** = iniciar / pausar / reanudar, según el estado.
- **Doble toque rápido** (ventana de ~400 ms) = reiniciar.
- **No usa pulsación larga**: en cualquier launcher (Nova incluido) el long-press sobre un widget lo intercepta el propio launcher para arrastrarlo y el evento nunca llega a la app. Es una limitación de plataforma, no una preferencia.
- Implementado con **RemoteViews clásico**, no con Glance. Ver `docs/decisions/001-widget-con-remoteviews-y-chronometer.md`.
- El descuento lo pinta un `Chronometer` de `RemoteViews`, que tickea en el proceso del launcher **sin despertar la app**.

### 2.6 Alertas de fin de slot ✅ Implementadas

- Sonido seleccionable de un catálogo de 4 más silencio, reproducido con `USAGE_ALARM` (usa el volumen de alarma, no el de multimedia). **Hay dos ajustes, uno por extremo del slot**: acabar un pomodoro suena suave —cuenco, el más grave y largo— y acabar el descanso suena más fuerte —campana, más brillante y sonando 2,6 s—, porque uno es una recompensa y el otro una orden. Lo decide el slot que **acaba**, en `TimerSettings.alertSoundFor`. El selector reproduce cada opción al tocarla y no se cierra, que es lo que hace el ajuste utilizable: los nombres no significan nada hasta oírlos. Ver `docs/decisions/012-*`. Los clips son **Opus mono en contenedor `.ogg`** —no Vorbis—, sintetizados para la app: mismo contenedor, soportado desde API 21 y comprime mejor en mono.
- **El sonido se puede repetir de 1 a 10 veces seguidas**, con **un ajuste por cada extremo del slot** igual que el sonido —`TimerSettings.alertRepeatsFor`—, y por defecto **una sola vez**, que es lo que hacía la app antes. Encadenado sin hueco: una repetición tiene que leerse como una alerta más larga, no como dos. **La cadena se cuenta, nunca se usa `isLooping`**: un bucle es ilimitado y lo único que lo pararía —este proceso— es justo lo que el sistema puede matar a mitad de alerta, así que sería la versión sonora del bug de la vibración infinita.
- **Vibración de duración configurable en segundos**, por defecto 5 s, en pulsos de 400 ms con huecos de 250 ms.
- Respeta modo silencio y No molestar.
- **La alerta la toca la app, no el canal de notificación.** Ver `docs/decisions/004-alerta-propia-en-vez-de-sonido-de-canal.md`.
- **La notificación de fin de slot está hecha para llegar al reloj emparejado** con sus dos acciones —empezar el siguiente y descartar—, y es la única que no va marcada como silenciosa. Ver `docs/decisions/011-*`.

### 2.7 Pantalla Novedades (changelog) ✅ Implementada

Igual que en Bebe Agua. **Cuatro sitios que hay que mantener sincronizados** al publicar una versión:

1. `CHANGELOG.md` en la raíz (fuente de verdad del repo).
2. Los `string-array` `changelog_<version>` en `values/strings.xml` y `values-es/strings.xml`.
3. `ui/changelog/ChangelogCatalog.kt`.
4. `versionCode` / `versionName` en `app/build.gradle.kts`.

El test unitario `ChangelogCatalogTest` falla si el `versionCode` compilado no tiene entrada en el catálogo; el instrumentado `ChangelogResourcesTest` falla si falta el array en ES o EN.

---

## 3. Stack técnico

**Vinculante** (no cambiar sin justificarlo):

| Capa | Decisión |
|---|---|
| Lenguaje | Kotlin **2.3.21** |
| UI | Jetpack Compose con BOM **`2026.04.01`** |
| Material | Material 3 **solo como base de componentes**; el tema es 100 % propio y solo oscuro |
| `minSdk` | 31 (Android 12) |
| `targetSdk` | 36 (Android 16) |
| `compileSdk` | 36 |
| Build | Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`) |
| AGP | **9.3.1** |
| KSP | **2.3.7** (para Hilt y Room; **no usar kapt**) |
| Arquitectura | MVVM + UDF, capas `ui` / `domain` / `data` |
| DI | Hilt **2.59.2** |
| Persistencia | **Room 2.7.1** para el historial de sesiones; **DataStore Preferences 1.1.4** para ajustes y para el estado del temporizador (dos ficheros separados) |
| Navegación | **Navigation Compose 2.9.0** |
| Motor del temporizador | **Híbrido**: estado persistido en DataStore + `ForegroundService` `specialUse` + `AlarmManager` de respaldo. Ver §6 y `docs/decisions/002-motor-del-temporizador-hibrido.md` |
| Widget | **RemoteViews + `Chronometer`**. **No usar Glance** (a diferencia de Bebe Agua). Ver `docs/decisions/001-...` |
| Gráficas | **Compose `Canvas` a mano**. No añadir Vico ni ninguna librería de gráficos |
| Notificaciones | `NotificationManagerCompat` + dos canales: `timer_running` (LOW) y `timer_alerts` (HIGH), **ambos mudos** |
| Concurrencia | Coroutines **1.10.2** + Flow |
| i18n | `strings.xml` (`values/` inglés base, `values-es/`); cambio en runtime con `AppCompatDelegate.setApplicationLocales` |
| Tipografía | **Inter** + **Space Grotesk** (OFL) empaquetadas en `res/font/` como **fuentes variables** (un fichero por familia, `FontVariation` elige el peso), con figuras tabulares |
| Tests | **JUnit 4** + MockK **1.13.9** + Turbine **1.2.0** + Compose UI Test + `room-testing` |
| Logs | `Timber 5.0.1` (solo en debug) |
| Backup | `android:allowBackup="false"` |

### Versiones de librerías clave

| Librería | Versión |
|---|---|
| Compose BOM | 2026.04.01 |
| Hilt | 2.59.2 |
| Room | 2.7.1 |
| DataStore | 1.1.4 |
| Navigation Compose | 2.9.0 |
| Hilt Navigation Compose | 1.2.0 |
| Coroutines | 1.10.2 |
| core-splashscreen | 1.0.1 |
| Timber | 5.0.1 |
| MockK | 1.13.9 |
| Turbine | 1.2.0 |

Gradle wrapper **9.5.0**, toolchain JDK **21**, `compileOptions` Java **11**.

### Permisos requeridos

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

> **No declarar `USE_EXACT_ALARM` jamás.** Google Play lo reserva a despertadores y calendarios, y nuestra arquitectura no lo necesita: la alarma exacta es la tercera capa de respaldo, no el mecanismo principal. `SCHEDULE_EXACT_ALARM` se pide en onboarding con `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` y **la app funciona sin él** — pero el onboarding lo pide como necesario en vez de como opcional, porque describir la arquitectura no es lo mismo que describir la experiencia; ver `docs/decisions/008-*`.

> **No declarar `USE_FULL_SCREEN_INTENT`.** Android 14 lo restringe a llamadas y alarmas de despertador, y Play lo revisa. Basta con `IMPORTANCE_HIGH` y heads-up.

> **El `<service>` lleva `foregroundServiceType="specialUse"`** más su `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="pomodoro_countdown_timer" />`. La justificación para Play Console está redactada en `docs/play-store-publication-texts.md` y **hay que enviarla en la primera subida a pruebas internas**, no en la de producción.

---

## 4. Estructura del proyecto

```
app/
  build.gradle.kts
  schemas/…AppDatabase/1.json          # schema Room exportado, versionado en git
  src/main/
    AndroidManifest.xml
    java/com/jjrapps/aquihaytomate/
      AquiHayTomateApplication.kt      # @HiltAndroidApp; Timber, canales, colector del widget
      MainActivity.kt                  # AppCompatActivity + setContent
      di/
        ClockModule.kt                 # provee java.time.Clock (relojes fijos en tests)
        CoroutineModule.kt             # CoroutineScope de aplicación + dispatchers
        DatabaseModule.kt              # Room + DAO
        DataStoreModule.kt             # dos DataStore, con qualifiers distintos
        Qualifiers.kt                  # @SettingsPreferences, @TimerStatePreferences, @ApplicationScope
        RepositoryModule.kt            # abstract class con todos los @Binds
      data/
        local/db/
          AppDatabase.kt               # v1, entidad única focus_session
          FocusSessionEntity.kt        # @ColumnInfo snake_case; UNIQUE(session_id, slot_index)
          FocusSessionDao.kt           # inserción idempotente + agregaciones diarias
          DayTotals.kt                 # POJO de proyección
          LocalDateConverter.kt        # java.time ↔ TEXT (minSdk 31, sin desugaring)
        local/datastore/
          SettingsDataSource.kt        # Flows por campo + snapshot; coerceIn en los setters
          TimerStateDataSource.kt      # Flow<TimerState> + write() atómico
        system/
          ChargingMonitorImpl.kt       # sticky BATTERY_CHANGED + POWER_CONNECTED/DISCONNECTED
        repository/
          SettingsRepositoryImpl.kt
          StatsRepositoryImpl.kt
          TimerStateRepositoryImpl.kt
      domain/
        model/
          TimerSettings.kt             # defaults + MIN/MAX en el companion object
          TimerState.kt                # snapshot completo del motor
          SlotType.kt                  # FOCUS / SHORT_BREAK / LONG_BREAK
          TimerStatus.kt               # IDLE / RUNNING / PAUSED / RINGING
          FocusSession.kt
          DayStats.kt
          PeriodStats.kt
          StreakInfo.kt
          AlertSound.kt                # enum con id estable + @RawRes + @StringRes
          KeepScreenOnMode.kt          # NEVER / WHILE_CHARGING / ALWAYS, con la regla pura
        repository/
          SettingsRepository.kt
          StatsRepository.kt
          TimerStateRepository.kt
          TimerAlarmScheduler.kt       # interfaz: arm(deadline) / cancel()
          TimerServiceController.kt    # interfaz: start() / stop()
          AlertPlayer.kt               # interfaz: play(sound, vibrationSeconds)
          ChargingMonitor.kt           # interfaz: Flow<Boolean> de enchufado
        render/
          TomatoGeometry.kt            # PURO: superficie del líquido, compartido app ↔ widget
        usecase/
          TimerMath.kt                 # PURO: tiempo restante, progreso, resistencia a cambios de reloj
          SlotPlanner.kt               # PURO: planNextSlot, ciclo de N pomodoros
          DayRollover.kt               # PURO: si un temporizador parado es de un día ya cerrado
          AlertPolicy.kt               # PURO: decide sonido/vibración según ringer + DND
          VibrationPatterns.kt         # PURO: waveform finito de N segundos
          StatsAggregation.kt          # PURO: días → semanas/meses con WeekFields
          StreakCalculator.kt          # PURO: racha actual y mejor racha
          StartTimerUseCase.kt · PauseTimerUseCase.kt · ResumeTimerUseCase.kt
          SkipSlotUseCase.kt · ResetTimerUseCase.kt · ToggleTimerUseCase.kt
          CompleteSlotUseCase.kt       # registra + alerta + planifica; IDEMPOTENTE
          ReconcileTimerUseCase.kt     # repara el estado tras muerte del proceso o reinicio
          StartFreshDayUseCase.kt      # barre el ciclo que quedó de un día anterior
          ObserveTimerStateUseCase.kt · ObserveSettingsUseCase.kt
          GetTodayStatsUseCase.kt · GetPeriodStatsUseCase.kt · GetStreakUseCase.kt
      timer/
        PomodoroTimerService.kt        # FGS specialUse: wakelock acotado + delay + notificación
        TimerServiceControllerImpl.kt  # start/stop con catch de ForegroundServiceStartNotAllowed
        TimerAlarmSchedulerImpl.kt     # ELAPSED_REALTIME_WAKEUP, exacta o fallback inexacta
        TimerAlarmReceiver.kt          # red de seguridad: goAsync + CompleteSlotUseCase
        TimerActionReceiver.kt         # acciones de notificación
        BootReceiver.kt                # BOOT / LOCKED_BOOT / MY_PACKAGE_REPLACED → reconcile
        TimerNotificationFactory.kt    # ongoing con chronometer + alerta de fin
      alert/
        AlertPlayerImpl.kt             # MediaPlayer USAGE_ALARM + VibratorManager
        SoundCatalog.kt                # AlertSound → R.raw; único sitio con los ids
      ui/
        theme/          Color.kt · Type.kt · Shape.kt · Theme.kt   (solo esquema oscuro)
        common/         LiquidTomato · LiquidCountdown · PhaseLabel · TextControl · CycleDots
                        TopTabBar · SectionLabel · SettingsRow · PhaseToggle · DurationSheet
                        StatCell · WeekBarChart · MonthHeatmap · StreakStrip · Sparkline
                        HairlineDivider · AlertBanner · PagerDots · TomateIcons
        timer/          TimerScreen.kt · TimerViewModel.kt · TimerUiState.kt
        stats/          StatsScreen.kt · StatsViewModel.kt · StatsUiState.kt
        settings/       SettingsScreen.kt · SettingsViewModel.kt · SettingsUiState.kt
        onboarding/     OnboardingScreen.kt · OnboardingViewModel.kt
        changelog/      ChangelogScreen.kt · ChangelogViewModel.kt · ChangelogUiState.kt
                        ChangelogCatalog.kt
        main/           MainViewModel.kt      # gate de onboarding + reconcile al abrir
        navigation/     NavGraph.kt · Screen.kt
      widget/
        PomodoroWidgetProvider.kt      # AppWidgetProvider clásico
        TomatoBitmapRenderer.kt        # rasteriza el tomate usando TomatoGeometry
        WidgetTapReceiver.kt           # resuelve toque simple vs doble
        TapGate.kt                     # ventana de doble toque, persistida
    res/
      font/            inter_*.ttf · space_grotesk_*.ttf
      raw/             bell.ogg · bowl.ogg · digital.ogg · soft.ogg · licenses_ofl.txt
      layout/          widget_tomate.xml · widget_tomate_preview.xml
      values/          strings.xml (inglés base) · colors.xml · themes.xml
      values-es/       strings.xml
      xml/             locale_config.xml · backup_rules.xml · data_extraction_rules.xml
                       widget_pomodoro_info.xml
      drawable/ · mipmap-*/
gradle/
  libs.versions.toml
docs/
  design-spec.md
  play-store-publication-texts.md
  decisions/001…005-*.md
```

---

## 5. Base de datos (Room)

**Versión actual de la base de datos: 1**

### Tabla `focus_session`

| Columna | Tipo | Notas |
|---|---|---|
| `id` | INTEGER PK autogen | |
| `session_id` | INTEGER NOT NULL | agrupa los slots de una tanda; es el epoch ms del inicio |
| `slot_index` | INTEGER NOT NULL | posición dentro de la tanda |
| `slot_type` | TEXT NOT NULL | siempre `FOCUS` en v1; la columna queda abierta a futuro |
| `started_at_epoch_ms` | INTEGER NOT NULL | UTC |
| `ended_at_epoch_ms` | INTEGER NOT NULL | UTC |
| `planned_duration_ms` | INTEGER NOT NULL | |
| `actual_focus_ms` | INTEGER NOT NULL | tiempo real, **excluye pausas** |
| `completed` | INTEGER NOT NULL | 0 / 1 |
| `timezone_id` | TEXT NOT NULL | p. ej. `Europe/Madrid` |
| `local_date` | TEXT NOT NULL | `YYYY-MM-DD` derivado |

Índices: `local_date`, `started_at_epoch_ms`, y **`UNIQUE(session_id, slot_index)`**.

> **El índice único es una pieza de arquitectura, no una optimización.** Es lo que hace idempotente la carrera entre el servicio y la alarma de respaldo: los dos caminos insertan con `OnConflictStrategy.IGNORE` y el segundo devuelve `-1L`. Sin banderas en memoria ni bloqueos. **No lo quites.**

### Qué se guarda y qué no

- **Solo se persisten slots de enfoque.** Los descansos no generan filas. Ver `docs/decisions/003-solo-se-persisten-los-slots-de-enfoque.md`.
- Enfoque completo → `completed = 1`.
- Enfoque saltado o reiniciado a mitad → fila con `completed = 0`, **solo si `activeElapsedMs ≥ 60 s`**. Por debajo se descarta: un start/stop accidental no debe ensuciar los datos.
- Por tanto: «minutos enfocados» suma `actual_focus_ms` de **todas** las filas; «pomodoros completados» cuenta solo las de `completed = 1`. El copy de Estadísticas tiene que reflejarlo.

### Agregaciones

El DAO solo hace la agregación **diaria** (`GROUP BY local_date`).

> **Semana y mes NO se agregan en SQL.** `strftime('%Y-%W')` de SQLite decide el primer día de la semana por su cuenta y no conoce `WeekFields.of(locale)`. Las agregaciones semanales y mensuales y el cálculo de racha son **funciones puras en Kotlin** (`StatsAggregation.kt`, `StreakCalculator.kt`) con `Clock` y `Locale` inyectados: testeables sin emulador y correctas en cambios de hora.

Al subir la versión del schema → escribir migration + test en `androidTest/`. `AppDatabaseMigrationTest` con `MigrationTestHelper` está montado desde la v1 precisamente para forzarlo.

---

## 6. El motor del temporizador (resumen técnico)

El estado del temporizador lo consumen cuatro sitios (UI, widget, notificación, servicio) y debe sobrevivir a que el sistema mate el proceso. La solución es **híbrida, en tres capas**:

```
CAPA 1 · VERDAD    DataStore `timer_state.preferences_pb`
                   endAtEpochMs + endAtElapsedRealtimeMs + status + slot + ciclo
                   → Flow<TimerState>. NADIE hace tick sobre esto.

CAPA 2 · MOTOR     PomodoroTimerService (FGS specialUse, stopWithTask=false)
                   Vive solo mientras status == RUNNING.
                   PARTIAL_WAKE_LOCK acotado + una corrutina delay(→ deadline).
                   Mecanismo PRIMARIO. Cero permisos de usuario.

CAPA 3 · RED       AlarmManager ELAPSED_REALTIME_WAKEUP al mismo deadline.
                   Exacta si hay permiso, inexacta si no.
                   + BootReceiver + reconcile() al abrir la app o tocar el widget.
```

### Reglas que no se pueden romper

- **Doble deadline.** Se guardan `endAtEpochMs`, `endAtElapsedRealtimeMs` y `bootEpochMs`. Se usa **siempre** el monotónico `elapsedRealtime` salvo que se detecte un reinicio, y **el reinicio se detecta porque el uptime retrocedió** (`endAtElapsedRealtimeMs - slotDurationMs > uptimeActual`), no porque `bootEpochMs` se descuadre. Un reinicio y un cambio de hora rompen los dos ese valor pero piden respuestas opuestas; confundirlos hace saltar el pomodoro justo lo que el usuario movió el reloj. **Cambiar la hora del sistema no debe hacer saltar el pomodoro.** Ver `docs/decisions/006-*`.
- **El tiempo activo se deriva, no se acumula.** `activeElapsedMs = slotDurationMs - remainingMs`. Como al reanudar el deadline se empuja exactamente lo que quedaba, esa resta *es* el tiempo que el reloj estuvo corriendo. Un acumulador guardado sería un segundo origen de verdad que reconciliar. Ver `docs/decisions/006-*`.
- **Un FGS no mantiene la CPU despierta.** Con la pantalla apagada, `delay()` se despierta tarde. El servicio adquiere un `PARTIAL_WAKE_LOCK` con `acquire(remainingMs + 5_000)` y lo libera en `finally`. **Nunca un wakelock sin timeout**: aparece en Play Vitals.
- **`startForeground()` en la primera línea de `onStartCommand`**, antes de leer nada de DataStore. Hay 5 s de margen antes de `ForegroundServiceDidNotStartInTimeException`.
- **`START_NOT_STICKY`.** Un `START_STICKY` reviviría el servicio con `intent == null` y sin contexto; preferimos reconciliar.
- **Todo `startForegroundService` va en `try/catch (ForegroundServiceStartNotAllowedException)`** con degradación a «solo estado + alarma». Las exenciones legítimas que sí tenemos: desde la Activity, desde una acción de notificación, desde el toque en el widget y desde una alarma exacta.
- **Al pausar se detiene el servicio.** No tiene sentido quemar wakelock y notificación foreground con el reloj parado; se publica una notificación normal con «Reanudar».
- **El ciclo pertenece a la jornada: al cambiar de día se reinicia.** Un temporizador **parado** que
  quedó de un día anterior se barre en `reconcile()` —ciclo a cero, siguiente slot de enfoque, tanda
  cerrada—, y un pomodoro que se quedó pausado se da por abandonado registrando su parcial. **Un
  temporizador corriendo no se toca nunca**, y la comparación de días solo rueda hacia adelante, para
  que atrasar el reloj del sistema no borre el ciclo en curso. Ver `docs/decisions/015-*`.
- **`reconcile()` nunca simula más de un slot vencido**, aunque el auto-inicio esté activo. Si el móvil estuvo apagado 8 horas, se registra el slot que venció, se pasa a `IDLE` y ya. Sin esta regla, abrir la app por la mañana insertaría 16 pomodoros falsos.
- **La notificación ongoing vuelve si el usuario la descarta**, mientras el temporizador esté corriendo o pausado: desde Android 13 `setOngoing` no impide el swipe, y quedarse sin notificación es quedarse sin cifra y sin controles. En `RINGING` e `IDLE` no vuelve. Ver `docs/decisions/010-*`.
- **La notificación ongoing no se repinta cada segundo.** Se publica una vez por transición (~4 `notify()` por pomodoro) y el descuento lo tickea un `Chronometer` dentro de **nuestro propio cuerpo de notificación** (`DecoratedCustomViewStyle`), en el proceso de SystemUI. Nada de `setProgress()` ni de minutos en el título, que obligaría a republicar cada minuto. Ver `docs/decisions/009-*`.
- **El widget se refresca desde un solo sitio**: un colector con scope de aplicación observa `TimerStateRepository.state` con `distinctUntilChangedBy { Triple(status, slotType, endAtEpochMs) }` + `debounce(250)`. **Nunca por segundo** — de eso se encarga el `Chronometer` del widget.

### Máquina de estados

`IDLE → RUNNING ⇄ PAUSED → RINGING → …`

- El contador de ciclo **solo avanza con enfoques completados al 100 %**. Saltar un enfoque no acerca al descanso largo.
- Saltar un enfoque lleva siempre a descanso corto, nunca a largo.
- `pomodorosPerCycle` y la duración del slot se **congelan en el snapshot del estado**: cambiar Ajustes no altera la tanda en curso.

---

## 7. Convenciones de código

- **Idioma del código y comentarios técnicos**: inglés. Strings de UI: recursos i18n.
- **Package raíz**: `com.jjrapps.aquihaytomate`.
- **Nomenclatura Compose**: `TimerScreen.kt`, `TimerViewModel.kt`, `TimerUiState.kt`. Una pantalla = tres ficheros.
- **Estado de pantalla**: `sealed interface XxxUiState` con `Loading` / `Success(data)` / `Error(message)`. Eventos de una vez vía `Channel<XxxEvent>(Channel.BUFFERED).receiveAsFlow()`.
- **Screens**: `fun XxxScreen(vm: XxxViewModel = hiltViewModel())` con `collectAsStateWithLifecycle()`, más un `private fun XxxContent(state, callbacks)` stateless debajo.
- **Inyección**: constructor injection siempre. La **única excepción** permitida es donde el framework instancia la clase: `BroadcastReceiver` y `Service` con `@AndroidEntryPoint` + `@Inject lateinit var`.
- **Receivers**: siempre con el esqueleto `goAsync()` + `CoroutineScope(SupervisorJob() + Dispatchers.IO)` + `try / catch { Timber.e } / finally { pendingResult.finish() }`.
- **Lógica compartida entre entrypoints → función pura top-level** en `domain/usecase/` o `domain/render/`. Es lo que impide que la app, el widget, el servicio y el receiver diverjan. `TimerMath`, `SlotPlanner`, `AlertPolicy`, `TomatoGeometry` existen por esto.
- **Toda lógica que dependa del tiempo recibe `java.time.Clock` inyectado**, nunca `System.currentTimeMillis()` directo. Para eso está `ClockModule`.
- **No usar `LiveData`** ni `RxJava`. Solo Flow/StateFlow.
- **No usar XML para UI de la app.** Todo Compose. Excepciones: el splash con `androidx.core.splashscreen` y el **layout del widget**, que por diseño es RemoteViews.
- **Previews**: cada Composable público con al menos un `@Preview`. Como solo hay tema oscuro, basta `@Preview(showBackground = true, backgroundColor = 0xFF000000)`.

---

## 8. Cómo trabajar en este repo (instrucciones para Claude Code)

### Antes de cada tarea

1. Lee este archivo entero. Si la tarea contradice algo aquí, pregunta antes de tirar adelante.
2. Mira el último commit y `git status` para no pisar cambios.
3. Si vas a tocar UI, lee `docs/design-spec.md` y `ui/theme/*` **antes** de inventarte estilos, colores o tamaños.
4. Si vas a tocar el motor del temporizador, relee §6 y `docs/decisions/002-*`.

### Al hacer cambios

- Los strings van en `strings.xml`. **Nunca hardcodees strings en Composables.**
- Si añades un string en `values/`, añade la traducción en `values-es/` en el mismo commit. `StringsParityTest` falla si no.
- Si subes `versionCode`/`versionName` → actualiza los cuatro sitios de §2.7.
- Si tocas el schema de Room → migration + test de migration, e incrementa la versión.
- Si tocas notificaciones, el servicio o las alarmas → **pasa la checklist de resiliencia** (§10) en Android 12, 14 y 16. La lógica de permisos y de background cambia mucho entre versiones.
- Si tocas el widget → pruébalo en **Nova Launcher** (el que usa Jorge), Pixel Launcher y One UI.
- No añadas dependencias sin justificarlo y sin actualizar `libs.versions.toml`.

### Al terminar una tarea

- Ejecuta y deja en verde:
  ```bash
  JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew lint test
  ```
- Commit con [Conventional Commits](https://www.conventionalcommits.org/) **en español y con scope**: `feat(widget): …`, `fix(timer): …`, `refactor(data): …`, `chore(version): …`.
- Si has tomado una decisión técnica no trivial, añade `docs/decisions/NNN-titulo.md` (ADR ligero, con las secciones Contexto / Decisión / Alternativas descartadas / Consecuencias).

### Lo que NO hacer (rojo)

- **No declarar `USE_EXACT_ALARM`** ni `USE_FULL_SCREEN_INTENT`.
- **No declarar ningún otro `foregroundServiceType`** que no sea `specialUse`.
- **No repintar la notificación ni el widget cada segundo.** Para eso están `setUsesChronometer` y el `Chronometer` de RemoteViews.
- **No usar `VibrationEffect` con `repeat >= 0`.** Un patrón repetitivo cuyo `cancel()` se pierda al morir el proceso deja el móvil vibrando indefinidamente. Siempre waveform finito.
- **No poner el estado del temporizador en memoria** (un `StateFlow` del servicio, un singleton, un `object`). La verdad está en DataStore, punto.
- **No quitar el índice `UNIQUE(session_id, slot_index)`.**
- No usar WebView, Cordova, Capacitor, React Native, Flutter ni KMP. Esto es Android nativo en Compose.
- No escribir lógica de negocio en Composables ni en `Activity`. Va en `ViewModel` o en `UseCase`.
- No usar `runBlocking` fuera de tests.
- No subir secretos, claves de Play Console ni `keystore` al repo.
- No añadir analítica, crash reporting con telemetría ni SDKs de marketing.
- No añadir un tema claro «porque es lo normal». Es una decisión de producto tomada.

---

## 9. Roadmap

**v1.0 — MVP ✅ Publicada** (`versionCode 2`, `versionName 1.0.0`)

> **Esquema de versionado.** Las `0.9.0` y `0.9.1` fueron versiones internas que nunca se subieron; la
> `1.0.0` sale tras la revisión del autor en un Galaxy S25 real. El `versionCode` sube de uno en uno en
> **cada** subida a Play, aunque solo cambie el patch: Play rechaza un bundle cuyo `versionCode` no supere
> el de la última subida.

- [x] **F0** Esqueleto: Gradle, Hilt, tema, navegación, i18n, changelog, CI. *Hito alcanzado: `lint test` verde con 9 tests unitarios y la app vacía.*
- [x] **F1** Núcleo puro: `TimerMath`, `SlotPlanner`, `StreakCalculator`, `StatsAggregation`, `VibrationPatterns`, `AlertPolicy`, `TomatoGeometry` + tests. *Sin nada de Android.*
- [x] **F2** Persistencia: Room + DAO + los dos DataStore + repositorios.
- [x] **F3** Motor sin servicio: use cases de comando, `TimerViewModel`, `TimerScreen`, `LiquidTomato`. *Hito alcanzado: pomodoro completo con la app abierta, `lint test` verde con 157 tests unitarios.*
- [x] **F4** Supervivencia: FGS, notificaciones, alarma de respaldo, los tres receivers. *Código completo; la checklist de resiliencia en dispositivo real sigue pendiente.*
- [x] **F5** Alertas: sonidos y vibración configurable. *Los cuatro clips son Opus mono sintetizados para la app, ~80 KB.*
- [x] **F6** Estadísticas: agregaciones y las cuatro gráficas Canvas.
- [x] **F7** Ajustes, onboarding e i18n completos, con Inter y Space Grotesk empaquetadas como fuentes variables.
- [x] **F8** Widget 1×1. *Código completo; probarlo en Nova, Pixel Launcher y One UI sigue pendiente.*
- [x] **F9** Endurecimiento y publicación: R8, batería, prueba en OEM agresivo, ficha de Play. *R8 verificado y corregido (renombraba las constantes de los enums persistidos); 28 tests instrumentados en verde; **firma configurada con la upload key de Bebe Agua y `.aab` de release firmado y verificado** (4,5 MiB, certificado hasta 2051); ficha, data safety, política y declaración de `specialUse` redactadas. **Falta lo que solo se puede hacer en dispositivo real**: la checklist §10 en Android 12/14/16, la del widget en Nova / Pixel Launcher / One UI, batería restringida y OEM agresivo. Después, subir a pruebas internas. Ver `docs/f9-verificacion-en-emulador.md` y `docs/play-store-publication-texts.md` §1.*

**v1.2.0 — pulido en dispositivo real** (`versionCode 5`, 2026-08-10)

La primera tanda escrita **mirando la app en un Pixel 10 con Android 17**, y de ahí sale casi todo: los dos
fallos gordos —el temporizador se salía de la pantalla en horizontal y el icono se veía como una pegatina
negra sobre un fondo de pantalla claro— no se ven en un emulador con el escritorio por defecto ni en el
móvil del autor, que usa wallpaper negro. Lo mismo la placa del widget.

- [x] Pantalla encendida en tres modos, **mientras carga por defecto**, sin depender de que el reloj corra. Ver `docs/decisions/013-*`.
- [x] Enviar comentarios al autor por correo, con nombre y versión en el asunto.
- [x] El temporizador en dos columnas en horizontal; el onboarding, con scroll.
- [x] Icono sin placa: el degradado rojo llena el lienzo y la máscara del launcher da la forma.
- [x] Widget sin placa negra por defecto; el cáliz del tomate, retirado.
- [x] `archivesName` con nombre y versión, tras subir a Play el bundle de Bebe Agua por llamarse los dos `app-release.aab`.
- [x] Pipeline de capturas arreglado: escritorio limpio, punto de soltado relativo e idioma explícito.

**v1.3 (eventual)**

- [ ] Etiquetas / proyectos por sesión, con estadísticas desglosadas.
- [ ] Quick Settings Tile para iniciar/pausar desde la persiana.
- [ ] Export/import de datos en JSON.
- [ ] Variante de widget 2×1 con botones separados.

---

## 10. Checklists de verificación manual

### Notificación con cuerpo propio (obligatoria tras el ADR 009)

El cuerpo de la notificación lo decora cada fabricante, así que hay que mirarlo en:

- Android 12, 14 y 16.
- **One UI** (el móvil de Jorge) además de Pixel Launcher.
- **Tema claro y tema oscuro** del sistema: la cifra sale de `values-night`, y es la única superficie de la app con esquema claro.
- Colapsada y expandida, y en la **pantalla de bloqueo**, donde algunos sistemas degradan a la plantilla estándar.

### Aviso en el reloj emparejado (Garmin, tras el ADR 011)

1. La app aparece habilitada en Garmin Connect → Notificaciones inteligentes.
2. **Con el temporizador corriendo, en el reloj no hay ninguna notificación**: la persistente va `localOnly`.
3. Al terminar un slot con el auto-inicio desactivado, el aviso llega al reloj.
4. En el reloj se ven **las dos acciones**: «Empezar descanso» / «Volver al tajo» y «Descartar».
5. Pulsar la primera arranca el slot siguiente **con el móvil bloqueado**.
6. Descartar desde el reloj **corta la vibración** en el móvil, incluso con la vibración a 30 s.
7. Con auto-inicio activado **también llega**, con el texto «… ya en marcha» y la acción de saltar.

### Resiliencia del temporizador (obligatoria en F4, en Android 12, 14 y 16)

1. Pantalla apagada 25 min → suena en el segundo correcto.
2. Swipe desde recientes con el timer corriendo → la alarma dispara y al abrir la app el estado es coherente.
3. Reinicio del dispositivo a mitad de slot → estado reconciliado; si venció, **un solo** pomodoro registrado.
4. Cambiar la hora del sistema +1 h a mitad → el restante no salta.
5. `SCHEDULE_EXACT_ALARM` denegado → el timer sigue sonando vía FGS; Ajustes avisa de la merma de fiabilidad.
6. Sin permiso de notificaciones → no crashea, el timer funciona.
7. «Restringir uso de batería» activado → degradación documentada, no crash.

### Widget (obligatoria en F8, en Nova / Pixel Launcher / One UI)

- Ocupa una sola casilla.
- Un toque inicia y pausa.
- Doble toque reinicia.
- Un toque lento **no** dispara el reinicio.
- El descuento avanza con la app cerrada.
- Legible sobre wallpaper claro y oscuro.
- Sobrevive a reiniciar el launcher.

---

## 11. Branding y assets

- **Nombre**: ¡Aquí hay tomate! · **Play**: ¡Aquí hay tomate! - Pomodoro timer
- **Package**: `com.jjrapps.aquihaytomate`
- **Paleta**: negro puro de fondo. Enfoque `#FF4433` (rojo tomate), descanso corto `#F2A03D` (ámbar), descanso largo `#FFD166` (dorado). Paleta completa en `docs/design-spec.md` y `ui/theme/Color.kt`.
- **Tipografía**: Inter (textos) + Space Grotesk (cifras), ambas OFL, empaquetadas.
- **Icono adaptativo**: el degradado rojo **llena el lienzo** y es el cuerpo del tomate —la máscara del launcher le da la forma—, con el rabillo y el brillo en el primer plano. Sin placa oscura: sobre un fondo de pantalla claro se leía como una pegatina negra. Variante monocroma obligatoria para los iconos temáticos de Android 13+. **Desde la 1.3 la fruta lleva además el dial de un reloj marcando las 5:05**: la aguja gruesa en las cinco son los 25 min de enfoque y la fina en la una son los 5 del descanso. Ver `docs/decisions/014-*` y las siete direcciones descartadas en `docs/propuestas-icono/`.
- **Capturas para Play** (5): **el widget en el escritorio primero** — es el argumento de venta —, luego Temporizador en enfoque, Temporizador en descanso, Estadísticas y Ajustes.
- **Feature graphic** 1024×500: tomate cortado por abajo sobre negro, con la cifra en negativo.

---

## 12. Relación con Bebe Agua

Este proyecto replica deliberadamente las convenciones de `/Users/jorge/dev/bebe-agua-android` (mismo autor, misma cuenta de Play, mismo prefijo `com.jjrapps`): estructura de paquetes, patrón de ViewModel, `ClockModule`, esqueleto de receivers, formato de `CHANGELOG.md`, ADRs, textos de Play.

**Diferencias deliberadas:**

| Aspecto | Bebe Agua | Aquí hay tomate | Motivo |
|---|---|---|---|
| Widget | Glance | **RemoteViews + Chronometer** | El widget muestra una cuenta atrás; el `Chronometer` tickea sin despertar la app |
| Background | Solo `AlarmManager` | **FGS + alarma de respaldo** | Un temporizador no puede depender de un permiso denegado por defecto |
| Tipografías | `FontFamily.SansSerif` (TODO pendiente) | **Inter + Space Grotesk empaquetadas** | El TODO de Bebe Agua nunca se cerró |
| `AGENTS.md` | Copia de `CLAUDE.md`, desincronizada | **Symlink a `CLAUDE.md`** | Para que no puedan divergir |
| Tema | Solo oscuro (aunque el roadmap dice dynamic color) | **Solo oscuro, explícito** | Decisión de producto documentada |
| CI | No hay | **GitHub Actions con `lint` + `test`** | Hueco que se corrige aquí |

---

## 13. Preguntas abiertas

Si te topas con una de estas, **pregunta a Jorge** antes de inventar una respuesta:

1. ~~¿La cifra del widget muestra solo minutos (`24`) o minutos y segundos (`24:58`)?~~ **Resuelta en `docs/decisions/007-*`: `MM:SS` a 13 sp.** Solo los minutos exigiría repintar cada 60 s, que es el coste que el ADR 001 rechazó. Queda medir el tamaño en dispositivo durante la checklist de F8.
2. ¿El doble toque del widget debe pedir confirmación al reiniciar una sesión larga ya avanzada, o reinicia sin más? *Propuesta: sin confirmación, pero el toque simple posterior dentro de 3 s deshace.*
3. ¿La ventana del doble toque son 400 ms fijos o se hace configurable en Ajustes? *Propuesta: 400 ms fijos en v1, se calibra en dispositivo real durante F8.*
4. ¿Se muestran los descansos en las estadísticas en algún momento futuro, o el histórico es solo de enfoque para siempre? La columna `slot_type` está preparada para ambas.
5. ¿Objetivo diario en pomodoros (8) o en minutos enfocados (200)? *Propuesta: pomodoros, que es la unidad mental de la técnica.*
