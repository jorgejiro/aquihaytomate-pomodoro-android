# Especificación visual — ¡Aquí hay tomate!

> Documento de referencia para toda la UI. **Léelo antes de escribir cualquier Composable.** Si necesitas un color, un tamaño o un espaciado que no está aquí, no lo inventes: añádelo aquí primero.
>
> Estado: v1.0 · Última revisión: 2026-07-27

---

## 1. Estética general

Fondo negro, un círculo de líquido rojo que se vacía, y nada más. La app no tiene tarjetas, ni sombras, ni botones con relleno, ni iconografía decorativa. Los controles son texto en mayúsculas. La única forma con presencia es el tomate.

**Referencias que sí:** paneles de instrumentos, cronómetros de laboratorio, tipografía suiza sobre negro.
**Referencias que no:** Material 3 Expressive, botones píldora gigantes, ilustraciones, gamificación, emojis en la UI.

Tres reglas que resuelven el 90 % de las dudas:

1. **Si dudas entre poner una caja o no ponerla, no la pongas.** Un `Text` con área táctil de 48 dp es un botón perfectamente válido.
2. **El color solo lo lleva la información.** El rojo (o el ámbar) es el estado del temporizador y los datos de las gráficas. Todo lo demás es negro, hueso o gris.
3. **Un solo elemento por pantalla puede ser grande.** En Temporizador es el tomate; en Estadísticas, las cifras de hoy. Lo demás baja de escala.

---

## 2. Paleta

Solo esquema oscuro. No hay tema claro ni `dynamicColor`.

### 2.1 `ui/theme/Color.kt` — listo para pegar

```kotlin
package com.jjrapps.aquihaytomate.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Backgrounds ────────────────────────────────────────────────────────
val BackgroundVoid      = Color(0xFF000000)   // lienzo de la app, negro puro
val SurfaceRaised       = Color(0xFF0D0B0A)   // grupos de ajustes, bottom sheets
val SurfacePressed      = Color(0xFF1A1614)   // estado pulsado de filas y chips
val SurfaceSunken       = Color(0xFF120F0E)   // pista de las barras de las gráficas

// ─── Focus phase · rojo tomate ──────────────────────────────────────────
val TomateBright        = Color(0xFFFF4433)   // acento vivo: etiquetas, barra de hoy
val TomateFill          = Color(0xFFE23B2B)   // relleno del líquido
val TomateDeep          = Color(0xFF7A2018)   // contorno del círculo vacío
val TomateGhost         = Color(0xFF1C0D0B)   // hueco del tomate sin líquido

// ─── Short break phase · ámbar ──────────────────────────────────────────
val AmbarBright         = Color(0xFFF2A03D)
val AmbarFill           = Color(0xFFDB8C2C)
val AmbarDeep           = Color(0xFF6B4715)
val AmbarGhost          = Color(0xFF1A1208)

// ─── Long break phase · dorado ──────────────────────────────────────────
val DoradoBright        = Color(0xFFFFD166)
val DoradoFill          = Color(0xFFE8B849)
val DoradoDeep          = Color(0xFF6E5720)
val DoradoGhost         = Color(0xFF191509)

// ─── Text ───────────────────────────────────────────────────────────────
val TextPrimary         = Color(0xFFF5F2EF)   // hueso, no blanco puro
val TextSecondary       = Color(0xFFA8A09B)
val TextMuted           = Color(0xFF6E6763)
val TextGhost           = Color(0xFF3A3532)   // puntos pendientes, chevrons
val TextOnLiquid        = Color(0xFF000000)   // cifra en negativo sobre el líquido

// ─── Borders ────────────────────────────────────────────────────────────
val BorderHair          = Color(0xFF1E1A18)   // separadores de 1 dp
val BorderStrong        = Color(0xFF332D2A)   // borde de toggles y chips inactivos

// ─── Semantic ───────────────────────────────────────────────────────────
val AlertAmber          = Color(0xFFF2A03D)   // avisos de permisos y batería
val SurfaceHighlight    = Color(0x99FFFFFF)   // línea de brillo de la superficie
```

### 2.2 Colores por fase

Se agrupan en un `PhaseColors` que se resuelve una vez y se pasa a los componentes, en vez de hacer `when (slotType)` dentro de cada uno:

```kotlin
data class PhaseColors(
    val bright: Color, val fill: Color, val deep: Color, val ghost: Color,
)

fun phaseColorsOf(type: SlotType) = when (type) {
    SlotType.FOCUS       -> PhaseColors(TomateBright, TomateFill, TomateDeep, TomateGhost)
    SlotType.SHORT_BREAK -> PhaseColors(AmbarBright,  AmbarFill,  AmbarDeep,  AmbarGhost)
    SlotType.LONG_BREAK  -> PhaseColors(DoradoBright, DoradoFill, DoradoDeep, DoradoGhost)
}
```

### 2.3 Contraste (WCAG 2.1, sobre `BackgroundVoid` salvo indicación)

| Par | Ratio | Cumple |
|---|---|---|
| `TextPrimary` / negro | 18,4:1 | AAA |
| `TextSecondary` / negro | 8,7:1 | AAA |
| `TextMuted` / negro | 3,9:1 | AA solo para texto ≥ 18,66 sp o negrita ≥ 14 sp |
| `TomateBright` / negro | 5,4:1 | AA texto normal |
| `AmbarBright` / negro | 8,6:1 | AAA |
| `DoradoBright` / negro | 12,3:1 | AAA |
| `TextOnLiquid` / `TomateFill` | 5,1:1 | AA |
| `TextOnLiquid` / `AmbarFill` | 8,0:1 | AAA |
| `TextPrimary` / `TomateFill` | 3,6:1 | Solo se usa a 76 sp → AA large |
| `TextGhost` / negro | 1,9:1 | **Decorativo únicamente.** Nunca lleva información que no esté duplicada |

Reglas derivadas:

- **`TextMuted` nunca se usa por debajo de 11 sp para información esencial.** En etiquetas de eje de gráfica (11 sp) es aceptable porque el dato también está en el detalle al tocar.
- **`TextGhost` solo para puntos de ciclo pendientes y chevrons**, que son redundantes con el texto adyacente.

### 2.4 Redundancia no cromática

En la app, la fase **nunca se distingue solo por color**:

- La etiqueta de fase (`ENFOQUE` / `DESCANSO` / `DESCANSO LARGO`) siempre está presente, en palabras, bajo el tomate. Es la redundancia que cuenta.
- La línea `SIGUIENTE: …` nombra también la fase que viene.

**El cáliz de 3 hojas se retiró.** Se dibujaba en descanso, tangente al borde superior del tomate, y en pantalla no se leía como hojas sino como un recorte pegado encima del círculo: las tres hojas se solapaban —semianchura `0,17·w` con las bases a `0,30·w`— y se fusionaban en un bloque de base recta que además cruzaba el contorno, porque se pintaba fuera del `clipPath`. En el widget de 40 dp queda por tanto **solo el color** para distinguir la fase, que es una merma asumida: la app es donde se lee el estado y ahí la fase está escrita.

---

## 3. Tipografía

Dos familias, ambas SIL OFL 1.1, empaquetadas en `res/font/` con subsetting a Latin + Latin Extended-A (~230 KB en total).

| Uso | Familia | Fichero |
|---|---|---|
| Textos, etiquetas, ajustes | **Inter** | `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf` |
| Cifras, temporizador, estadísticas | **Space Grotesk** | `space_grotesk_medium.ttf`, `space_grotesk_bold.ttf` |

> **Todas las cifras llevan figuras tabulares.** Sin `tnum`, los dígitos tienen anchos distintos y el contador «baila» horizontalmente cada segundo, que es exactamente lo que hace que un temporizador se vea barato.
>
> ```kotlin
> val TabularNums = TextStyle(fontFeatureSettings = "tnum")
> ```

Se añade `res/raw/licenses_ofl.txt` con la licencia y una fila **Licencias** en Acerca de.

### 3.1 Escala

| Rol | Familia / peso | Tamaño | Tracking | Uso |
|---|---|---|---|---|
| `displayTimer` | Space Grotesk Medium | 76 sp | −2,0 | La cifra del temporizador |
| `displayStat` | Space Grotesk Bold | 34 sp | −0,8 | Cifras de «Hoy» y racha |
| `titleScreen` | Inter SemiBold | 28 sp | −0,4 | Títulos de onboarding |
| `phaseLabel` | Inter SemiBold | 13 sp | **+3,2** | `ENFOQUE`, `DESCANSO` |
| `controlLabelLarge` | Inter Medium | 19 sp | +1,8 | `PAUSAR`, `INICIAR`: el control primario del temporizador |
| `controlLabel` | Inter Medium | 15 sp | +1,4 | Controles secundarios: `REINICIAR`, `SIGUIENTE` del onboarding |
| `tabLabel` | Inter Medium | 12 sp | +1,2 | Pestañas superiores |
| `sectionLabel` | Inter Medium | 10 sp | +1,6 | Cabeceras de sección, en mayúsculas |
| `rowLabel` | Inter Regular | 15 sp | 0 | Etiqueta de fila de ajustes |
| `rowSublabel` | Inter Regular | 12 sp | 0 | Explicación bajo la etiqueta |
| `bodyDefault` | Inter Regular | 15 sp | 0 | Texto corrido, `lineHeight` 22 sp |
| `numberSmall` | Space Grotesk Medium | 13 sp | 0 | Valores de ajustes, ejes |
| `caption` | Inter Regular | 11 sp | +0,4 | Etiquetas bajo cifras |
| `widgetTime` | Space Grotesk Bold | 15 sp | −0,5 | La cifra del widget de 40 dp |

Las mayúsculas se aplican en el `TextStyle` con el string ya en su forma natural en `strings.xml`. **No escribas los strings en mayúsculas en el XML**: rompe la traducción y los lectores de pantalla.

---

## 4. Formas y espaciado

```kotlin
// ui/theme/Shape.kt
Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // barras de gráfica
    small      = RoundedCornerShape(8.dp),    // chips, celdas del heatmap
    medium     = RoundedCornerShape(12.dp),   // grupos de ajustes
    large      = RoundedCornerShape(18.dp),   // bottom sheets
    extraLarge = RoundedCornerShape(50),      // nada por ahora
)
```

- **Rejilla base: 4 dp.** Todo espaciado es múltiplo de 4.
- **Margen lateral de contenido: 20 dp** (32 dp en onboarding, que necesita más aire).
- **Área táctil mínima: 48 dp** de alto, aunque el texto mida 15 sp.
- **Separadores: 1 dp** en `BorderHair`, con 14 dp de sangrado izquierdo dentro de los grupos y a ancho completo entre secciones.

---

## 5. Layout de pantallas

Medidas de referencia: pantalla de 360 dp de ancho, área de contenido de **320 dp**.

### 5.0 Barra de pestañas (común a las tres pantallas)

- Alto **48 dp**, fondo `BackgroundVoid`, sin borde inferior.
- Tres etiquetas `tabLabel` repartidas con `Arrangement.SpaceEvenly`: `TEMPORIZADOR · ESTADÍSTICAS · AJUSTES`.
- Activa: `TextPrimary` más subrayado de **2 × 16 dp** en el `bright` de la fase actual, centrado, con `animateDpAsState(spring(dampingRatio = 0.85f))` en la posición X.
- Inactiva: `TextMuted`.
- **La barra lleva su propio `statusBarsPadding()`**, con el `background` aplicado *antes* del padding: el negro llega por detrás del reloj del sistema y las etiquetas quedan por debajo. `Scaffold` no lo inserta solo — solo las app bars de Material consumen ese inset, y esta deliberadamente no es una de ellas. Sin este padding las tres pestañas se solapan con la hora y los iconos de estado.

### 5.1 Temporizador (`TimerScreen`)

```
┌──────────────────────────────────────┐  status bar transparente
│  TEMPORIZADOR  ESTADÍSTICAS  AJUSTES │  48 dp
│  ────────────                        │
│                                      │  24 dp
│           ╭──────────────╮           │
│         ╭──────────────────╮         │
│        │░░░░░░░░░░░░░░░░░░░ │        │  268 dp Ø · hueco TomateGhost
│       │ ░░░░░░ 18:42 ░░░░░   │       │  76 sp
│        │∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿∿│        │  ← superficie ondulando
│         ╲████████████████████╱        │  relleno TomateFill
│           ╰──────────────╯           │  ← el círculo entero es área táctil
│                                      │  28 dp
│              E N F O Q U E           │  phaseLabel · TomateBright
│                                      │  ← aire elástico, peso 0,675 (mínimo 10 dp)
│             ❚❚  P A U S A R          │  controlLabelLarge · alto táctil 56 dp
│                                      │
│      R E I N I C I A R   S A L T A R │  controlLabel · TextPrimary · fila de 48 dp
│                                      │  ← el mismo aire, peso 0,675 → el grupo va centrado
│      SIGUIENTE: DESCANSO · 5 MIN     │  sectionLabel · TextMuted · hueco de 20 dp
│                                      │  16 dp
│          ● ● ○ ○      2/4            │  puntos 7 dp, gap 10 dp
└──────────────────────────────────────┘  32 dp de aire inferior
```

| Elemento | Valor |
|---|---|
| Diámetro del tomate | **268 dp**. En pantallas < 600 dp de alto: **224 dp**. En horizontal: `0,84 × alto`, acotado a 140–268 dp |
| Contorno (visible donde no hay líquido) | 2 dp en `phase.deep` |
| Hueco sin líquido | relleno `phase.ghost` |
| Contador | `displayTimer`, centrado con `offsetY = -2.dp` (compensa el descender vacío) |
| Línea de superficie | 1,5 dp en `SurfaceHighlight`, alpha 0,9 |
| Tomate → etiqueta de fase | 28 dp |
| Etiqueta → control primario | **elástico**: la mitad del aire sobrante, con un mínimo de 10 dp. En horizontal, 20 dp fijos |
| Control primario | `controlLabelLarge`, alto táctil **56 dp**, ancho `wrap`, ripple sin límites, **sin fondo** |
| Controles secundarios | `REINICIAR` y `SALTAR` en una fila, `controlLabel` en **`TextPrimary`**, **en mayúsculas igual que el primario**, gap 24 dp, en un hueco de 48 dp reservado siempre; entran con `fadeIn(150 ms)`. Van en el mismo tinte que el primario a propósito: en `TextMuted` se leían como deshabilitados, y en una pantalla sin cajas el color es la única señal de que algo se puede pulsar. La jerarquía la marcan el tamaño y el glifo |
| «Siguiente» | `sectionLabel` `TextMuted` vía `SectionLabel`, hueco de 20 dp reservado, 16 dp por encima de los puntos |
| Puntos de ciclo | 7 dp Ø, gap 10 dp. Completado: relleno `phase.bright`. Actual: anillo de 1,5 dp `phase.bright` + relleno al 25 %. Pendiente: anillo de 1 dp `TextGhost` |
| `2/4` | `numberSmall` `TextMuted`, 12 dp a la derecha del último punto |

**El reparto vertical no es un `Arrangement.Center` con espaciados fijos.** El aire libre se divide en tres `Spacer` con peso: **1 encima del tomate** y **0,675 a cada lado del grupo de controles**, con los puntos de ciclo pegados al borde inferior a 32 dp. Tres razones:

1. Con todo centrado en bloque, el tomate, la etiqueta, los dos controles y los puntos formaban una sola masa apelotonada en el centro con dos franjas negras enormes arriba y abajo. Los puntos de ciclo son un indicador de estado, no parte del grupo de controles, y su sitio es abajo.
2. Los dos pesos de abajo suman 1,35, que es el peso único que había antes bajo los controles, así que **el tomate queda exactamente donde estaba** y sigue ligeramente por encima del centro óptico, que es donde el ojo lo espera.
3. **Repartirlo a los dos lados del grupo de controles es lo que los centra.** Hasta la 1.3 los controles colgaban de la etiqueta de fase con 20 dp fijos y todo el sobrante se acumulaba debajo: en un móvil alto el grupo salía pegado al tomate con un agujero negro bajo él. El mínimo de 10 dp por lado —la mitad de esos 20 dp— es lo que mantiene el espaciado de antes en pantallas donde no sobra nada.

**El hueco de los controles secundarios se reserva esté o no visible.** Si no, `REINICIAR` empujaba el tomate 48 dp arriba y abajo cada vez que se pausaba o se reiniciaba. Lo mismo con la línea de «a continuación», que no existe en `RINGING`.

**Las tres acciones son el control primario, `REINICIAR` y `SALTAR`.** Son las tres cosas que se pueden hacer con un pomodoro en marcha, y hasta ahora la pantalla ofrecía dos: saltar solo se alcanzaba desde la notificación. Cuándo se ve cada secundario:

| Control | Visible cuando | Por qué |
|---|---|---|
| `REINICIAR` | `status != IDLE` | En `IDLE` no hay progreso que tirar, y `ResetTimerUseCase` no haría nada |
| `SALTAR` | `status != IDLE` **o** el slot pendiente es un descanso | Saltarse un descanso que aún no ha empezado es algo que se quiere de verdad; saltarse el primer enfoque antes de empezarlo lleva a un descanso que nadie ha ganado |

**La línea «SIGUIENTE: DESCANSO · 5 MIN»** sale de `SlotPlanner.upcomingSlot(state, settings)`, la misma función pura que usa la notificación ongoing y la misma que ejecuta el motor cuando el slot termina de verdad. No se muestra en `RINGING`: allí el estado ya apunta al slot siguiente, así que la predicción hablaría del que viene *después*, y el control primario ya nombra el inmediato.

**Toda la superficie del tomate es el control primario**: `LiquidCountdown` acepta `onClick`, con recorte a `CircleShape` para que el ripple sea circular y con el `onClick` declarado también dentro de `clearAndSetSemantics` — ese bloque sustituye la semántica del subárbol, así que la acción del `clickable` no sobreviviría a él. Un objetivo de 268 dp de diámetro es lo más cómodo que hay en la pantalla; la etiqueta de texto se queda porque es lo que *nombra* la acción.

Funciona de 560 a 900 dp de alto sin recortes: a 560 dp los pesos se comprimen a cero y quedan los espaciados fijos con el tomate de 224 dp.

**En horizontal el bloque se parte en dos columnas.** Un móvil de lado deja unos 370 dp de alto y la columna vertical no cabe en ellos: hasta la 1.1.0, `SALTAR` y los puntos de ciclo quedaban **por debajo del borde inferior de la pantalla**, sin scroll ni forma de alcanzarlos. Cuando `maxWidth > maxHeight`:

- El tomate va solo en la mitad izquierda, centrado, y su diámetro se calcula sobre el alto disponible —`0,84 × maxHeight`, entre 140 dp y los 268 dp de vertical— en vez de ser un número fijo. Girar el móvil no lo hace más grande que en vertical.
- La mitad derecha apila etiqueta de fase, control primario, secundarios, «SIGUIENTE» y puntos de ciclo, centrada verticalmente y con espaciados fijos: no hay alto sobrante que repartir con pesos.
- Los puntos de ciclo dejan de estar pegados al borde inferior. En horizontal no hay un «abajo de la pantalla» lo bastante lejos para que se lean como un indicador aparte, así que acompañan a los controles.

El `@Preview` `TimerScreenLandscapePreview` (900 × 370 dp) existe para que la regresión no vuelva a pasar desapercibida.

**Estados del control primario:**

| Estado | Texto |
|---|---|
| `IDLE` | `▸ INICIAR` |
| `RUNNING` | `❚❚ PAUSAR` |
| `PAUSED` | `▸ REANUDAR` |
| `RINGING` (fin de enfoque) | `▸ EMPEZAR DESCANSO` |
| `RINGING` (fin de descanso) | `▸ VOLVER AL TAJO` |

En `RINGING` la etiqueta de fase se sustituye por **`¡TIEMPO!`** en `AlertAmber`, con `infiniteRepeatable` de alpha 1,0 ↔ 0,45 cada 900 ms. Con movimiento reducido, alpha fija a 1,0.

### 5.2 Estadísticas (`StatsScreen`)

```
┌──────────────────────────────────────┐
│  TEMPORIZADOR  ESTADÍSTICAS  AJUSTES │  48 dp
│                                      │  20 dp
│  HOY                                 │  sectionLabel
│   8          3 h 20 m       12       │  displayStat · 3 columnas iguales
│   pomodoros  enfocado       racha    │  caption · TextMuted
│                                      │  28 dp
│  ─────────────────────────────────   │  1 dp BorderHair
│  ESTA SEMANA            ‹    ›       │  20 dp arriba
│    ▄ █   ▃                       ┄┄┄ │  pista 96 dp · línea de objetivo
│  ▂ █ █ ▆ █ ▁ ▂                       │  barras 26 dp
│  L M X J V S D                       │  numberSmall 11 sp
│  ─────────────────────────────────   │
│  ABRIL 2026             ‹    ›       │
│  ▢ ▢ ▣ ▤ ▥ ▢ ▢                       │  celdas 40 dp, gap 6 dp, radio 8 dp
│  ▤ ▥ ▦ ▥ ▤ ▢ ▣                       │  5 niveles de intensidad
│  ▥ ▦ ▦ ▦ ▥ ▣ ▢                       │
│  ▣ ▤ ▥ ▦ ▢ ▢ ▢                       │
│  menos ▢▣▤▥▦ más            136 pom. │
│  ─────────────────────────────────   │
│  ÚLTIMOS 30 DÍAS                     │
│      ╱╲    ╱╲╱╲                      │  sparkline 1,5 dp
│  ╱╲╱  ╲╱╲╱     ╲╱╲                   │  relleno degradado 18 % → 0 %
└──────────────────────────────────────┘
```

| Elemento | Valor |
|---|---|
| Celdas «Hoy» | `Row` con 3 `Column(weight = 1f)`; cifra `displayStat` en `TomateBright`, etiqueta `caption` en `TextMuted`, gap 4 dp |
| Barras semanales | ancho **26 dp**, `SpaceBetween` en 320 dp, pista 96 dp en `SurfaceSunken`, esquinas superiores 4 dp |
| Barra de hoy | `TomateBright` en vez de `TomateFill` |
| Línea de objetivo | 1 dp punteada `dashPathEffect(floatArrayOf(4f, 4f))` en `BorderStrong`, con la cifra a la derecha en `caption` |
| Día bajo la barra | `numberSmall` 11 sp `TextMuted`; hoy en `TomateBright` |
| Celda del heatmap | **40 dp**, gap 6 dp (7 × 40 + 6 × 6 = 316 ≤ 320), radio 8 dp |
| Niveles del heatmap | 0 → `TomateGhost` · 1 → `TomateFill` @25 % · 2 → @45 % · 3 → @70 % · ≥4 → @100 % |
| Hoy en el heatmap | anillo de 1,5 dp `TextPrimary` |
| Racha | `displayStat` en `TomateBright` más tira de 30 segmentos de 3 × 8 dp con 2 dp de hueco |
| Sparkline | alto 56 dp, trazo 1,5 dp `TomateBright` `StrokeCap.Round`, relleno `verticalGradient(TomateFill@0.18f → Transparent)`, punto final de 4 dp |
| Flechas `‹ ›` | 28 dp, sin caja, glifo de 14 dp en `TextMuted`, ripple sin límites |

Al tocar una barra o una celda aparece bajo el gráfico una línea de detalle (`fecha · N pomodoros · H h M m`) en `caption`. **Sin tooltip flotante.**

**Regla común: ningún gráfico lleva ejes, ni rejilla, ni marco.** Solo las barras o celdas, la etiqueta del eje X y, cuando aporta, la línea de objetivo.

### 5.3 Ajustes (`SettingsScreen`)

Secciones con `sectionLabel` en `TextMuted` (20 dp arriba, 8 dp abajo) y un grupo con fondo `SurfaceRaised`, radio 12 dp, sin borde, con separadores internos de 1 dp `BorderHair` y 14 dp de sangrado.

Fila estándar: **52 dp** de alto, padding horizontal 16 dp. Etiqueta `rowLabel` en `TextPrimary`, sublabel opcional `rowSublabel` en `TextMuted`, valor a la derecha en `numberSmall` `TomateBright`, y chevron `›` de 14 dp en `TextGhost` a 8 dp. Pulsada: fondo `SurfacePressed`.

```
┌──────────────────────────────────────┐
│  DURACIONES                          │
│ ┌──────────────────────────────────┐ │
│ │ Enfoque                 25 min › │ │  52 dp
│ │ Descanso corto           5 min › │ │
│ │ Descanso largo          15 min › │ │
│ │ Pomodoros por ciclo          4 › │ │
│ └──────────────────────────────────┘ │
│  COMPORTAMIENTO                      │
│ ┌──────────────────────────────────┐ │
│ │ Auto-iniciar el descanso  [● ]   │ │  toggle 40×22, thumb 16
│ │ Auto-iniciar el pomodoro  [ ○]   │ │  dos ajustes, no uno
│ │ Mantener pantalla encendida      │ │
│ │ Solo en el temporizador  Carga › │ │  tres modos, no un toggle
│ │ Objetivo diario              8 › │ │
│ └──────────────────────────────────┘ │
│  AVISOS                              │
│ ┌──────────────────────────────────┐ │
│ │ Sonido al terminar el pomodoro   │ │
│ │ Toca descansar        Digital  › │ │
│ │ Repetir ese sonido               │ │
│ │ Desde la cocina       1 vez    › │ │  1–10, encadenadas sin hueco
│ │ Sonido al terminar el descanso   │ │
│ │ Vuelta al tajo        Campana  › │ │
│ │ Repetir ese sonido               │ │
│ │ Sin poder ignorarlo   1 vez    › │ │  independiente del de arriba
│ │ Vibración                  5 s › │ │  0 s = desactivada
│ └──────────────────────────────────┘ │
│  WIDGET                              │
│ ┌──────────────────────────────────┐ │
│ │ Fondo             Transparente › │ │
│ └──────────────────────────────────┘ │
│  APARIENCIA                          │
│ ┌──────────────────────────────────┐ │
│ │ Animación del líquido     [ ●]   │ │
│ └──────────────────────────────────┘ │
│  SISTEMA                             │
│ ┌──────────────────────────────────┐ │
│ │ Idioma                   Auto  › │ │
│ │ Avisos                     OK  › │ │  AlertAmber si denegado
│ │ Alarmas exactas            OK  › │ │  AlertAmber si denegado
│ └──────────────────────────────────┘ │
│  ACERCA DE                           │
│ ┌──────────────────────────────────┐ │
│ │ Enviar comentarios             › │ │  sin sublabel: la dirección
│ │                                  │ │  solo se ve en la app de correo
│ │ Novedades                      › │ │
│ │ Licencias                      › │ │
│ │ Versión                 1.0.0 (1)│ │
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘
```

- **Toggle propio** (`PhaseToggle`): pista 40 × 22 dp radio 11 dp. Activo: `TomateFill` con thumb de 16 dp en `TextPrimary` a la derecha. Inactivo: `SurfacePressed` con borde 1 dp `BorderStrong` y thumb `TextGhost` a la izquierda. `spring(dampingRatio = 0.7f)`.
- **Selector de duración** (`DurationSheet`): `ModalBottomSheet` con fondo `SurfaceRaised`, radio superior 18 dp, chips de 1/5/10/15/20/25/30/45/50/60 min más «Otra…». Chip de 40 dp de alto, radio 8 dp, `numberSmall`; inactivo `SurfacePressed` + borde `BorderStrong`; activo `TomateFill` + `TextOnLiquid`.
- **No existe fila de tema claro/oscuro.** Es intencional.

### 5.4 Onboarding (`OnboardingScreen`)

Cinco páginas en `HorizontalPager`, sin barra de pestañas, sin botón «saltar»: qué es · tus duraciones · tu ciclo · cuántas veces suena · widget y permisos.

La tercera —**Tu ciclo**— lleva `pomodorosPerCycle` y el descanso largo como filas de chips (`2 · 3 · 4 · 6` y `10 · 15 · 20 · 30 min`) y los dos auto-inicios como `SettingsToggleRow` dentro de un `SettingsGroup`, con los mismos textos que Ajustes. Son los cuatro ajustes que deciden cómo se comporta la app a lo largo de una mañana, y por eso se preguntan antes del primer pomodoro. Los valores por defecto se ven ya marcados: ciclo de 4, descanso largo de 15 min, **auto-iniciar el descanso activado** y auto-iniciar el pomodoro desactivado.

```
Página 1 — Qué es          Página 2 — Tus duraciones   Página 5 — Widget y permisos
┌────────────────────┐     ┌────────────────────┐      ┌────────────────────┐
│      ╭──────╮      │     │   ENFOQUE          │      │   ┌────┐           │
│     │████████│     │120dp│   ┌──┬──┬──┬──┐    │      │   │ 25 │  widget   │
│      ╰──────╯      │     │   │20│25│30│45│    │chips │   └────┘  a escala │
│                    │     │   └──┴──┴──┴──┘    │48 dp │                    │
│  ¡Aquí hay tomate! │28sp │   DESCANSO         │      │  Ponlo en tu       │
│                    │     │   ┌──┬──┬──┬──┐    │      │  escritorio        │
│  25 minutos de     │body │   │ 3│ 5│10│15│    │      │  AJUSTA TU MÓVIL   │
│  trabajo, 5 de     │máx  │   └──┴──┴──┴──┘    │      │ ┌────────────────┐ │
│  descanso, y un    │260dp│                    │      │ │Avisos PENDIENTE│ │
│  widget de una     │     │   Lo puedes cambiar│cap.  │ │Necesario     › │ │
│  sola casilla.     │     │   luego en Ajustes │      │ │Alarmas ACTIVADO│ │
│                    │     │                    │      │ └────────────────┘ │
│   ● ○ ○ ○ ○        │     │   ○ ● ○ ○ ○        │      │   ○ ○ ○ ○ ●        │
│         SIGUIENTE →│     │         SIGUIENTE →│      │        EMPEZAR →   │
└────────────────────┘     └────────────────────┘      └────────────────────┘
```

La cuarta —**cuántas veces suena**— es un título, un cuerpo y dos filas de chips `1 · 2 · 3 · 4 veces`,
una por extremo del slot, con el 2 de fábrica ya marcado en las dos. El rango completo (1–10) se queda en
Ajustes: aquí lo que hay que transmitir es que la repetición existe, no agotarla. **Su cuerpo no sigue la
regla de las demás páginas**: en vez de `bodyDefault` centrado a 260 dp va a todo el ancho del contenido y
alineado a la izquierda, con 16 dp bajo el título y **48 dp hasta la primera cabecera**. Bajo un título que
parte en dos líneas y sobre dos rejillas de chips, tres líneas cortas centradas se leían apretadas y los
controles parecían parte del párrafo.

Va **antes** de los permisos, no después: un gate con una página detrás no gatea, porque la siguiente
queda a un deslizamiento. Ver `docs/decisions/008-*`.

```
Página 4 — Cuántas veces suena
┌────────────────────┐
│  ¿No te enteras    │28sp
│  del final de fase?│
│                    │
│  El aviso puede    │body
│  sonar varias veces│
│  seguidas…         │
│                    │
│  AL TERMINAR EL PO…│label
│  ┌──┬──┬──┬──┐     │chips
│  │1 │2 │3 │4 │     │48 dp
│  └──┴──┴──┴──┘     │
│  AL TERMINAR EL DE…│
│  ┌──┬──┬──┬──┐     │
│  │1 │2 │3 │4 │     │
│  └──┴──┴──┴──┘     │
│   ○ ○ ○ ● ○        │
│         SIGUIENTE →│
└────────────────────┘
```

- **Toda la pantalla lleva `safeDrawingPadding()`**: el onboarding no está dentro del `Scaffold` de las pestañas, así que nadie más le aplica los insets y sin él el contenido se metía bajo la status bar y bajo la barra de gestos.
- Padding lateral 32 dp. Título `titleScreen`. Cuerpo `bodyDefault` en `TextSecondary`, ancho máximo 260 dp, centrado.
- Indicador de página: 5 puntos de 6 dp, gap 8 dp; activo `TomateFill`, inactivo `TextGhost`. Abajo a la izquierda, 24 dp del borde.
- Botón solo texto, `controlLabel` en `TextPrimary`, abajo a la derecha, alto táctil 48 dp.
- **En la página 1 el tomate se drena de lleno a vacío en bucle de 6 s.** Es la demostración del concepto.
- Al terminar se marca `onboarding_done`; `MainViewModel` decide el destino inicial.

**Página 5: los permisos son filas de estado, no botones.** Cada uno es un `SettingsRow` dentro de un `SettingsGroup`, igual que en Ajustes, pero **con 64 dp de alto en vez de 52** y 14 dp entre la cabecera y el grupo: aquí las dos filas son todo el contenido de la página, y con las medidas de Ajustes —donde una fila es una de treinta— se leían apelotonadas. La cabecera dice `CONCEDE LOS PERMISOS NECESARIOS`, que es una instrucción y no una etiqueta de sección.

| Estado | Valor | Color | Sublabel | Chevron |
|---|---|---|---|---|
| Pendiente | `Pendiente` | `AlertAmber` | `Necesario` | sí, y la fila abre el diálogo o los ajustes del sistema |
| Concedido | `Activado` | `TomateBright` | — | no, y la fila deja de ser pulsable |

- **`EMPEZAR` queda en `TextGhost` y deshabilitado hasta que los dos estén concedidos**, con `CONTINUAR SIN ELLOS` en `caption` `TextMuted` debajo, a la derecha. El hueco de esa salida (40 dp) se reserva en las cinco páginas para que el pager no salte al llegar a la última. Ver `docs/decisions/008-el-onboarding-exige-los-dos-permisos.md`.
- El diálogo de `POST_NOTIFICATIONS` se puede mostrar **una sola vez por instalación**: después de una negativa Android lo descarta en silencio, así que el segundo toque abre `ACTION_APP_NOTIFICATION_SETTINGS`. En API 31–32 no hay permiso que pedir y la fila va directa a los ajustes.
- El estado se relee en cada `ON_RESUME` con `RefreshPermissionsOnResume`, compartido con Ajustes, y también en la respuesta del diálogo: el diálogo del sistema no siempre pasa la Activity por `ON_PAUSE`, y sin eso la fila seguiría diciendo «Pendiente» sobre un permiso recién concedido.

---

## 6. La animación del líquido

Componente: `ui/common/LiquidTomato.kt`. Geometría en `domain/render/TomatoGeometry.kt`.

### 6.1 Geometría compartida con el widget

La forma del líquido vive en una **función pura sin dependencias de Compose ni de Android**, para que el `Canvas` de Compose y el `android.graphics.Canvas` que rasteriza el widget no puedan divergir:

```kotlin
/** Puntos de la superficie del líquido, de izquierda a derecha, en coordenadas de la caja. */
fun liquidSurfacePoints(
    width: Float, height: Float,
    fillFraction: Float,   // 1 = lleno, 0 = vacío
    phaseRad: Float,
    amplitudePx: Float,
    periods: Float,
    stepPx: Float,
): FloatArray
```

Testeable con JUnit puro: continuidad, amplitud acotada, y que `fillFraction = 1f` no deja hueco arriba ni `0f` deja líquido abajo.

### 6.2 Implementación en Compose

```kotlin
@Composable
fun LiquidTomato(
    fillFraction: Float,
    colors: PhaseColors,
    showCalyx: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "liquid")

    // Dos ondas con periodos primos entre sí: rompe la periodicidad y parece líquido
    val phaseA by transition.animateFloat(
        0f, TWO_PI,
        infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseA",
    )
    val phaseB by transition.animateFloat(
        0f, TWO_PI,
        infiniteRepeatable(tween(4700, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseB",
    )

    Spacer(
        modifier.drawBehind {              // ← la fase se LEE aquí dentro, no fuera
            clipPath(circlePath) {
                drawRect(colors.ghost)
                if (!reduced) drawWave(phaseB, ampB, periods = 1.7f, colors.fill.copy(alpha = .35f))
                drawWave(if (reduced) 0f else phaseA, ampA, periods = 1.25f, colors.fill)
                drawSurfaceLine(SurfaceHighlight)
            }
            if (fillFraction < 1f) drawCircle(colors.deep, style = Stroke(2.dp.toPx()))
            if (showCalyx) drawCalyx(colors.bright)
        }
    )
}
```

**Tres decisiones de rendimiento, por orden de importancia:**

1. **`Modifier.drawBehind` con la lectura de `phaseA`/`phaseB` DENTRO de la lambda de dibujo.** Así la animación invalida solo la fase de *draw*: no hay recomposición ni relayout por frame. Si la fase se lee fuera y se pasa como parámetro, se recompone 120 veces por segundo. Es el error clásico y aquí es la diferencia entre 0,3 ms y 4 ms por frame.
2. **`Path` construido con `lineTo` cada 4 dp**, no con béziers. A 268 dp en xxhdpi son ~804 px, o sea ~67 segmentos; el facetado es invisible. El `Path` se guarda en `remember` y se hace `rewind()` en cada draw, para no asignar en el bucle de frames.
3. **El nivel no se interpola.** En 25 min recorre 268 dp: un tick de 1 s lo mueve 0,18 dp. El ViewModel emite `remainingMs` a 1 Hz alineado al segundo (`delay(1000 - now % 1000)`) y el nivel se aplica directo. `animateFloatAsState(spring(dampingRatio = 0.75f, stiffness = StiffnessLow))` solo en transiciones discretas: reinicio y cambio de fase.

### 6.3 Los dígitos en negativo

Sin extraer el `Path` del texto. Dos `Text` superpuestos en el mismo `Box`, con el mismo estilo y por tanto el mismo layout, el de abajo recortado al líquido:

```kotlin
Box(contentAlignment = Alignment.Center) {
    Text(timeText, style = displayTimer, color = TextPrimary)
    Text(
        timeText, style = displayTimer, color = TextOnLiquid,
        modifier = Modifier.drawWithContent {
            clipPath(liquidPath) { this@drawWithContent.drawContent() }
        },
    )
}
```

Registro perfecto porque comparten layout. `liquidPath` se comparte con el `Canvas` de fondo vía el mismo `remember`, actualizado en el mismo frame.

### 6.4 App en segundo plano

**No hay que pausar nada a mano.** `rememberInfiniteTransition` se apoya en `withFrameNanos`; cuando la Activity se detiene, la ventana deja de producir frames y la animación se congela sola. Al volver reanuda desde donde estaba, sin saltar. En multiventana con la app visible pero sin foco sigue animando, que es lo correcto.

### 6.5 Movimiento reducido

> **`rememberInfiniteTransition` NO respeta `Settings.Global.ANIMATOR_DURATION_SCALE` automáticamente.** Eso solo afecta a `Animator` y a las animaciones de `View`. Hay que gestionarlo a mano.

```kotlin
val LocalReducedMotion = staticCompositionLocalOf { false }

// En MainActivity, con un ContentObserver sobre ANIMATOR_DURATION_SCALE:
val reduced =
    Settings.Global.getFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
    powerManager.isPowerSaveMode ||
    !settings.liquidAnimationEnabled
```

Se combinan tres fuentes: el ajuste del sistema, el modo de ahorro de energía (que algunos OEM no reflejan en el animator scale) y el toggle propio de Ajustes.

Con `reduced = true`:

- La onda desaparece: superficie plana. Sin `infiniteRepeatable`.
- El parpadeo de `¡TIEMPO!` pasa a alpha fija.
- El drenaje en bucle del onboarding se sustituye por una imagen estática al 55 %.
- **El nivel sigue bajando.** Es información, no decoración. Nunca se congela.

---

## 7. El widget 1×1

### 7.1 Placa

**Por defecto no hay placa: el widget va transparente y solo se ve el tomate.** El fondo sólido fue el valor por defecto hasta la 1.1.0 y parecía correcto por una razón que no vale para nadie más: el escritorio del autor tiene un fondo de pantalla negro, así que la placa era invisible. Sobre cualquier fondo más claro el widget se lee como una tarjeta negra con un tomate dentro, que es exactamente lo que descarta el «cero tarjetas, cero botones con relleno» del §1 de `CLAUDE.md`. El tomate es un círculo relleno y **opaco**, de modo que se recorta solo sobre lo que haya debajo sin necesidad de fondo.

La variante sólida sigue en Ajustes → Widget para quien la quiera: relleno negro opaco con `@android:dimen/system_app_widget_background_radius` —el radio lo decide el sistema, así encaja con el resto del escritorio— y **sin trazo**. Hubo un filete de 1 dp al 14 % de blanco para que la placa negra no se perdiera sobre un wallpaper oscuro; lo que hacía en realidad era rodear el widget de un contorno gris que se lee como suciedad.

### 7.2 Contenido a 40 × 40 dp

Tomate en miniatura de 34 dp con el nivel de líquido como progreso, la cifra encima en `widgetTime` (Space Grotesk Bold 15 sp) y **el glifo de la acción que hará el toque**.

> **Pregunta abierta (§13 de CLAUDE.md):** a 40 dp, `24:58` en 15 sp no cabe con holgura. Propuesta a validar en dispositivo: mostrar solo los minutos (`24`) mientras quedan ≥ 1 min, y los segundos en `phase.bright` durante el último minuto.

El descuento lo pinta un `Chronometer` de `RemoteViews` con `setChronometerCountDown(true)`, que tickea en el proceso del launcher **sin despertar la app**.

**El glifo de acción se dibuja en el bitmap, no se escribe.** Era el carácter `▸` en un `TextView` con `fontFamily` Space Grotesk — una fuente que no lo tiene, así que el sistema hacía fallback y pintaba una mota oscura de 13 sp que se leía como suciedad sobre el tomate, no como un botón. Ahora son dos `Path` en el `Canvas` del `TomatoBitmapRenderer`, con la misma lógica que `ControlGlyph` en la app: no dependen de ninguna fuente y se pueden dimensionar.

| | Tamaño | Posición | Color |
|---|---|---|---|
| Glifo grande (`IDLE`) | 34 % del tomate | centrado, y **sin cifra**: un temporizador parado no tiene tiempo que informar | `TextPrimary` |
| Glifo pequeño (resto) | 15 % del tomate | centrado en x, a 0,83 de la altura, debajo de la cifra | `TextPrimary` |

`TextPrimary` y no el acento de fase ni negro: el glifo tiene que leerse tanto sobre el líquido como sobre el hueco. En acento sobre `TomateFill` el contraste es 1,1:1 y en negro desaparece en el hueco de un tomate casi vacío; el hueso da 3,6:1 sobre el relleno y 18:1 sobre el hueco, y a estos tamaños es un gráfico, no texto corrido. Hay test instrumentado a nivel de píxel (`TomatoBitmapRendererTest`).

### 7.3 Estados

```
 PARADO          ENFOQUE          ENFOQUE          DESCANSO        DESCANSO       TERMINADO
                 CORRIENDO        PAUSADO          CORRIENDO       PAUSADO
┌────────┐      ┌────────┐       ┌────────┐       ┌────────┐      ┌────────┐    ┌────────┐
│ ╭────╮ │      │ ╭────╮ │       │ ╭────╮ │       │ ╭────╮ │      │ ╭────╮ │    │ ╭────╮ │
│ │████│ │      │ │░░░░│ │       │ │░░░░│ │       │ │░░░░│ │      │ │░░░░│ │    │ │████│ │
│ │ ▶  │ │      │ │24:58│ │      │ │24:31│ │      │ │ 4:07│ │     │ │ 4:07│ │   │ │05:00│ │
│ │████│ │      │ │ ❚❚ │ │       │ │ ▶  │ │       │ │ ❚❚ │ │      │ │ ▶  │ │    │ │ ▶  │ │
│ ╰────╯ │      │ ╰────╯ │       │ ╰────╯ │       │ ╰────╯ │      │ ╰────╯ │    │ ╰────╯ │
└────────┘      └────────┘       └────────┘       └────────┘      └────────┘    └────────┘
 TomateFill      TomateFill       TomateFill       AmbarFill       AmbarFill     el color y
 lleno           + ❚❚ pequeño     @45 % + ▶        + ❚❚           @45 % + ▶     la duración
 + ▶ grande                                                                       del slot que
                                                                                  viene
```

**El glifo dice lo que hará el toque, no en qué estado está el temporizador.** Parado y pausado muestran `▶`, corriendo muestra `❚❚`. Es la misma regla que el control primario de la app, y es lo que convierte un cuadrado de 40 dp en un botón: la acción por defecto se ve sin pensar. El estado ya lo cuentan el nivel del líquido, el color de la fase y —en pausa— el líquido al 45 %, así que el glifo no tiene que repetirlo.

- **En `IDLE` no hay cifra.** Un temporizador parado no tiene tiempo que informar y el `▶` se queda con todo el tomate. Es también el estado en el que queda el widget tras un doble toque o tras reiniciar el móvil, que es cuando más falta hace que se entienda que hay que tocarlo.
- **En `RINGING` el widget muestra el slot que está a punto de empezar**: tomate lleno con el color de esa fase —rojo si viene enfoque, ámbar si viene descanso—, su duración como cifra y el `▶`. El estado ya apunta al slot siguiente, así que es literalmente lo que hará el toque.

  Antes dibujaba un tomate vacío con un `!`, y en un escritorio lleno de iconos eso se lee como «algo va mal», no como «tu descanso está listo». Se retiró con él el parpadeo del borde, que era **el único sitio donde el widget se refrescaba por tiempo** (dos `updateAppWidget()` alternos hasta 60 s): sin signo de alarma no hay nada que hacer parpadear, y el aviso de que el slot terminó lo dan el sonido, la vibración y la notificación.
- **Qué muestra cada estado es una función pura**, `widget/WidgetReadout.kt`, con test unitario. Estas reglas cambiaron tres veces en una tarde de feedback y cada error era invisible hasta ver el widget en un escritorio.

### 7.4 Configuración del proveedor

`res/xml/widget_pomodoro_info.xml`:

```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="40dp"
    android:minHeight="40dp"
    android:minResizeWidth="40dp"
    android:minResizeHeight="40dp"
    android:targetCellWidth="1"
    android:targetCellHeight="1"
    android:resizeMode="none"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen"
    android:description="@string/widget_description"
    android:previewLayout="@layout/widget_tomate_preview"
    android:previewImage="@drawable/widget_preview_tomate"
    android:initialLayout="@layout/widget_tomate" />
```

`updatePeriodMillis="0"` porque el mínimo del sistema son 30 minutos y no nos sirve de nada; refrescamos nosotros desde el colector de estado.

`previewLayout` es lo que usa targetSdk 36 en el selector del launcher; `previewImage` queda como respaldo para hosts antiguos.

---

## 8. Notificaciones

### 8.1 Ongoing (canal `timer_running`, IMPORTANCE_LOW)

- Icono pequeño `ic_notif_tomate`, `setColor(TomateFill)`.
- **Dos cuerpos propios** con `DecoratedCustomViewStyle`, para que la cifra sea lo más grande de la notificación y los controles estén desde que se abre la cortinilla:

  ```
  COLAPSADA · notification_timer_collapsed.xml · 48 dp de tope
  ┌──────────────────────────────────────┐
  │ 🍅  24:58  ENFOQUE    ❚❚   ↺   ▶│    │  24 sp · 12 sp · iconos 44 dp
  └──────────────────────────────────────┘

  EXPANDIDA · notification_timer.xml
  ┌──────────────────────────────────────┐
  │ 🍅 ¡Aquí hay tomate!                 │  encabezado del sistema
  │   24:58     ENFOQUE · 2/4            │  26 sp · 13 sp
  │  PAUSAR   │   REINICIAR   │  SALTAR  │  fila de acciones del sistema
  └──────────────────────────────────────┘
  ```

- **En la forma colapsada no hay texto de fase.** No cabe: con la cifra y los tres controles, en One UI se cortaba a `En…`, y ese estado ni siquiera dibuja el encabezado. La fase va en el `subText` —que se lee al expandir— y **en la forma colapsada la lleva el color de la cifra**: rojo en enfoque, ámbar en descanso, con los tres iconos del mismo color. No queda como único indicador: el `subText` y el `contentTitle` la llevan escrita, que es lo que lee un lector de pantalla.
- **Todas las vistas de los dos cuerpos tienen que estar en la lista blanca de `RemoteViews`.** Un `Space` usado de espaciador llegó a ejecución: `RemoteViews` lo rechaza con «Class not allowed to be inflated», la notificación del servicio en primer plano no se puede inflar y **el sistema mata la app** con `BadForegroundServiceNotificationException`. El espaciador es un `TextView` vacío con `weight`, y `TimerNotificationFactoryTest.bothBodiesInflate` infla los dos cuerpos para que no vuelva a pasar.
- **La fila de acciones del sistema solo se dibuja expandida**, así que la colapsada lleva tres iconos propios con los mismos `PendingIntent`. Son iconos y no etiquetas porque **el contenido propio de una notificación colapsada está limitado a 48 dp** con `targetSdk` ≥ 31 (antes 106 dp): no hay dos filas, y en una no caben la cifra y tres palabras. Por lo mismo, la colapsada muestra el nombre de la fase sin el `2/4`.
- Las acciones de `addAction` se mantienen aunque en colapsada no se usen: son las que ven Wear, el asistente y la pantalla de bloqueo.

- Descuento: `Chronometer` con `setChronometerCountDown(true)` **dentro del cuerpo propio**, con base en el reloj monotónico. Sigue ticando en el proceso de SystemUI: **cero `notify()` por segundo**. Ver `docs/decisions/009-*`.
- La plantilla estándar daba la cifra al hueco del timestamp — 11 sp, arriba a la derecha, inmodificable — y todo el peso al título, donde estaba el `2/4`. Con el cuerpo propio se invierte: la cifra manda y la fase con el ciclo bajan a 13 sp.
- **Colores del cuerpo por cualificador** (`values/colors.xml` y `values-night/colors.xml`), no de la paleta del tomate ni de `?android:attr/textColorPrimary`, que resuelve oscuro en ambos temas. Es la única superficie de la app con esquema claro, porque su fondo lo pinta SystemUI.
- Pausado: `TextView` al mismo tamaño con la cifra congelada —un `Chronometer` no se puede detener en un valor arbitrario— y `· Pausado` al final de la línea de fase.
- **No hay línea de «Siguiente».** Ocupaba el renglón que ahora es la cifra y decía algo que el usuario ya sabe. En la pantalla del temporizador se mantiene, donde no compite con nada.
- `setContentTitle` se sigue rellenando aunque no se vea: es el fallback de las superficies que rechazan vistas propias y lo que lee un lector de pantalla.
- **Si el usuario la descarta por swipe con un slot en marcha, vuelve.** `setOngoing(true)` dejó de impedirlo en Android 13, y sin notificación no hay cifra ni controles. Un `deleteIntent` la republica mientras el estado sea `RUNNING` o `PAUSED`; en `RINGING` e `IDLE` no, porque ahí descartar es lo que el usuario quiere decir. Ver `docs/decisions/010-*`.
- Flags: `setOngoing`, `setSilent`, `setOnlyAlertOnce`, `CATEGORY_STOPWATCH`, `VISIBILITY_PUBLIC`, y **`setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)`** — sin esto Android 12+ retrasa la aparición hasta 10 s y el usuario cree que no ha arrancado.
- Acciones: **las mismas tres que la pantalla y en el mismo orden** — `Pausar`/`Reanudar` · `Reiniciar` · `Saltar`. Tres es el máximo que muestra una notificación, y que el juego sea idéntico en RUNNING y en PAUSED significa que **el botón bajo el dedo no se mueve** al pausar desde la persiana.
- **Nada de `setProgress()`**: obligaría a repintar constantemente. El progreso visual vive en la app y en el widget.

### 8.2 Fin de slot (canal `timer_alerts`, IMPORTANCE_HIGH)

`CATEGORY_ALARM`, `setAutoCancel(true)`. Título `¡Tiempo!`, texto `25 min de enfoque completados · 3 de 4`. Acciones **Empezar descanso** / **Volver al tajo** y **Descartar**.

- **Es la única que llega a un reloj emparejado**: las dos ongoing van `setLocalOnly(true)`, porque un cronómetro permanente en la lista de un reloj es ruido. Se renuncia así a pausar desde la muñeca, a cambio de que la muñeca solo avise cuando hay algo que decidir; para el control rápido está el widget.
- **Esta es la única notificación que NO lleva `setSilent(true)`.** El canal ya es mudo, así que el flag no aportaba silencio: lo que hacía era marcarla como no alertante, quitarle el heads-up y dejarla fuera de lo que un reloj emparejado reenvía. Las dos acciones se muestran en un Garmin como lista, que es como se controla el temporizador desde la muñeca. Ver `docs/decisions/011-*`.
- **Descartarla desde cualquier sitio para el sonido y la vibración**, vía `deleteIntent` a `ACTION_DISMISS`: al descartar desde el reloj, el sistema cancela la notificación sin pasar por la acción, y la vibración seguía hasta 30 s.

**Con auto-inicio también se publica**, con el cuerpo diciendo que el slot ya está en marcha, la acción de saltarlo en vez de empezarlo y `setTimeoutAfter` de 2 min para que no se acumule. Sin eso, el fin de un pomodoro no llegaba al reloj emparejado, que es justo cuando el móvil está lejos. Auto-iniciar significa no tener que tocar, no enterarse de nada.

El copy se escribe **para una pantalla de reloj**, que muestra título y cuerpo y nada más:

| Situación | Título | Cuerpo |
|---|---|---|
| Fin de pomodoro, esperando | `Pomodoro 3 de 4 completado` | `Descanso de 5 min · toca para empezar` |
| Fin de pomodoro, encadenado | `Pomodoro 3 de 4 completado` | `Descanso de 5 min ya en marcha` |
| Fin de descanso | `Descanso terminado` | `Pomodoro de 25 min · toca para empezar` |

> **Los dos canales son mudos** (`setSound(null, null)`, `enableVibration(false)`). El sonido y la vibración los toca `AlertPlayer`, porque un canal no permite cambiarlos después de creado y ambos son ajustes de primera línea. Ver ADR 004.

---

## 9. Iconografía y assets

| Asset | Fichero | Especificación |
|---|---|---|
| Icono adaptativo — fondo | `drawable/ic_launcher_background.xml` | 108 × 108 dp. **Es el cuerpo del tomate**: degradado lineal `#FF5240 → #B4241A` a 135° llenando el lienzo entero, para que la máscara del launcher sea la que da forma a la fruta |
| Icono adaptativo — primer plano | `drawable/ic_launcher_foreground.xml` | 108 × 108 dp, arte dentro de la zona segura de 66 dp: **el dial del reloj, las dos agujas, el rabillo, el cáliz de 3 hojas y el brillo** blanco al 20 % arriba a la izquierda, más una sombra al 18 % bajo el cáliz. El cuerpo va en la capa de fondo |

> **El fondo del icono es la fruta, no una placa.** Hasta la 1.1.0 el fondo era casi negro y el tomate se dibujaba pequeño dentro del primer plano, en la zona segura de 66 dp. Sobre cualquier fondo de pantalla que no fuera negro eso se leía como una pegatina negra con un tomate dentro, porque el launcher **siempre** pinta la capa de fondo y **siempre** recorta las dos con su propia máscara —círculo en Pixel, squircle en One UI—: no hay forma de publicar un icono con silueta libre. La única salida es que la placa sea el propio tomate. El cáliz lleva una sombra al 18 % porque verde y rojo son complementarios de luminancia parecida y sin ella las hojas se aplastan contra el cuerpo.

> **Desde la 1.3 la fruta es además un dial, y marca las 5:05.** Quitar la placa dejó el icono sin contorno —la silueta la dibujaba el recorte del launcher, no el icono— y además no decía en ningún momento que la app fuera un temporizador. Dibujar el dial *dentro* arregla las dos cosas de una vez: cierra la forma por dentro y se lee como reloj. La aguja **corta y gruesa marca las cinco, que son los 25 min de enfoque**; la **larga y fina marca la una, los 5 min del descanso corto**. Son cápsulas de grosor constante —2,6 y 4,2, largos 25,5 y 18,5— con remache de radio 3,6: dibujadas como polígonos que se estrechan, las dos nacían anchas en el eje y el centro salía abultado. **No hay marca a las 12**: caería debajo del tallo y sería tinta invisible, así que el rabillo hace de doce. Las siete direcciones que se descartaron están renderizadas en `docs/propuestas-icono/`; el razonamiento, en `docs/decisions/014-*`.
| Icono monocromo | `drawable/ic_launcher_monochrome.xml` | 108 × 108 dp, **un solo path blanco** con el reloj **calado** por winding `nonZero`. Obligatorio para iconos temáticos en Android 13+. Cuidado al tocarlo: dos agujeros que se solapen suman winding −2 y la intersección se vuelve a rellenar, así que cáliz y tallo van fundidos en un contorno y las agujas arrancan en radio 3,75 para no cruzarse — el blanco que queda entre ellas *es* el remache |
| `mipmap-anydpi/ic_launcher.xml` | | `<adaptive-icon>` con `<background>`, `<foreground>` y **`<monochrome>`** |
| Icono de notificación | `drawable/ic_notif_tomate.xml` | 24 × 24 dp, **máscara alfa pura** (el sistema lo tiñe de blanco). Silueta simplificada, sin agujeros finos |
| Splash | `drawable/ic_splash_tomate.xml` | Lienzo 288 × 288 dp con el arte en 192 × 192 dp centrado |
| Placa del widget | `drawable/widget_plate.xml` | Shape con `system_app_widget_background_radius` y relleno negro, **sin trazo** |
| Placa transparente | `drawable/widget_plate_transparent.xml` | Igual, sin relleno |
| Preview del widget | `layout/widget_tomate_preview.xml` + `drawable/widget_preview_tomate.xml` | Copia del layout con cifra fija `25`, y vector de respaldo |
| Fuentes | `font/inter_*.ttf`, `font/space_grotesk_*.ttf` | ~230 KB tras subsetting. SIL OFL 1.1 |
| Sonidos | `raw/bell.ogg`, `bowl.ogg`, `digital.ogg`, `soft.ogg` | OGG Vorbis mono 44,1 kHz, < 3 s, < 30 KB cada uno |
| Licencias | `raw/licenses_ofl.txt` | Texto de la OFL de ambas familias |
| Feature graphic | PNG sin alfa | **1024 × 500**, ver abajo. `docs/store-assets/grafico-de-funciones-1024x500.png` |
| Icono de Play | PNG 32 bits, alfa opaco | 512 × 512, render del adaptativo recortado a la ventana visible de 72 dp. `docs/store-assets/icono-play-512.png` |
| Capturas | PNG | Seis escenas por idioma en tres formatos: teléfono 1080 × 2400, tablet de 7" 1080 × 1920 y tablet de 10" 1440 × 2560 —las dos de tablet en 9:16 exacto, que es lo que Play valida—. **El pomodoro primero y el widget tras el historial.** En `docs/store-assets/capturas/<idioma>/<formato>/`, el idioma primero porque Play pide los recursos idioma a idioma. Las genera `docs/store-assets/generar-capturas/` |

**Splash** con `androidx.core.splashscreen`:

```xml
<style name="Theme.AquiHayTomate.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">#000000</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash_tomate</item>
    <item name="postSplashScreenTheme">@style/Theme.AquiHayTomate</item>
</style>
```

Sin `windowSplashScreenIconBackgroundColor`: el tomate flota directamente sobre negro y enlaza con la pantalla principal.

**Feature graphic 1024 × 500**: fondo `#000000` a sangre. Tomate de 560 px de diámetro centrado en `(300, 330)`, cortado por abajo, relleno al 62 % con la superficie ondulada. Sobre el líquido, en negativo, `18:42` en Space Grotesk Medium a 112 px. A la derecha, alineado a la izquierda en `x = 620`: **¡Aquí hay tomate!** en Inter SemiBold 68 px `#F5F2EF`, **en dos líneas**, con el borde superior de la tinta en `y = 150` y `y = 225`; en `y = 318` «Pomodoro de una sola casilla» en Inter Regular 26 px `#A8A09B`; en `y = 378` una barrita de 96 × 4 px en `TomateFill`. Sin capturas dentro, sin marcos de móvil, sin badges. Zona segura de 40 px en los bordes.

> Este párrafo se escribió primero con el nombre en una línea a 68 px, en `y = 205`, y el subtítulo a 30 px. **No cabe**: en `x = 620`, con la zona segura, quedan 364 px, y en Inter SemiBold 68 px el nombre ocupa 573 y el subtítulo a 30 px ocupa 412. De las tres formas de cuadrarlo —encoger el título, encoger el tomate o partir el nombre— se eligió partirlo, que es la única que no toca ninguna de las cifras de la composición y además cae en las dos mitades naturales del nombre. Medido, no estimado: lo comprueba el generador.

**Los dos assets no se dibujan a mano**: los genera `docs/store-assets/generar-assets.py`, que reproduce los `pathData` del icono y la onda de `TomatoGeometry` en vez de mantener un diseño paralelo que se desincronice. Si cambia el icono o la paleta, se actualiza el script y se vuelve a ejecutar.

---

## 10. Componentes reutilizables (`ui/common/`)

| Componente | Descripción |
|---|---|
| `LiquidTomato` | El círculo que se drena: hueco, dos ondas, línea de superficie y contorno. El corazón visual de la app |
| `LiquidCountdown` | `LiquidTomato` más los dos `Text` superpuestos que producen los dígitos en negativo. Con `onClick`, el círculo entero es el control primario |
| `PhaseLabel` | `ENFOQUE` / `DESCANSO` / `DESCANSO LARGO` / `¡TIEMPO!` con tracking amplio y color de fase |
| `TextControl` | Control sin caja: glifo + etiqueta **siempre en mayúsculas**, alto táctil configurable (48 dp por defecto), ripple sin límites. Base de `INICIAR`/`PAUSAR`/`REINICIAR`. El glifo se dibuja al 72 % del `fontSize` del estilo, así que crece con él |
| `RefreshPermissionsOnResume` | Relee el estado de los dos permisos en cada `ON_RESUME`. Lo comparten Ajustes y la página de permisos del onboarding |
| `CycleDots` | Fila de N puntos (completado / actual / pendiente) más el contador `2/4` |
| `TopTabBar` | Tres pestañas de texto con subrayado animado |
| `SectionLabel` | Cabecera de sección en `sectionLabel` `TextMuted` |
| `SettingsRow` | Fila de ajuste: etiqueta, sublabel opcional, valor y chevron |
| `SettingsToggleRow` | Variante con `PhaseToggle` en lugar de valor + chevron |
| `PhaseToggle` | Interruptor propio de 40 × 22 dp con thumb de 16 dp y muelle |
| `DurationChip` | Chip de duración activo / inactivo / «Otra…» |
| `DurationSheet` | `ModalBottomSheet` con la rejilla de chips y entrada manual |
| `StatCell` | Cifra `displayStat` más etiqueta `caption`, para las tres columnas de «Hoy» |
| `WeekBarChart` | Barras diarias con línea de objetivo y resalte de hoy |
| `MonthHeatmap` | Rejilla mensual de 7 columnas, 5 niveles, con detección de toque |
| `StreakStrip` | Número de racha más tira de 30 segmentos |
| `Sparkline` | Polilínea de 30 días con relleno degradado |
| `HairlineDivider` | Separador de 1 dp `BorderHair` con sangrado configurable |
| `AlertBanner` | Banner en `AlertAmber` para permisos denegados o batería restringida |
| `PagerDots` | Indicador de página del onboarding |
| `TomateIcons` | `object` con los `ImageVector`: `Play`, `Pause`, `Reset`, `Skip`, `ChevronLeft`, `ChevronRight`, `DotFilled`, `DotEmpty`, `Leaf` |

**Notas de implementación de las gráficas:**

- `MonthHeatmap` es **un solo `Canvas` con un bucle de 7 × N `drawRoundRect`**, no un `LazyGrid`. Son ≤ 35 rectángulos: un Canvas es más barato y permite dibujar el anillo de «hoy» sin desalinear la rejilla. El toque se resuelve con `pointerInput { detectTapGestures { cellAt(it) } }` y aritmética.
- Las barras de `WeekBarChart` entran con `animateFloatAsState` escalonado 40 ms por índice.
- **Todas las gráficas tienen que sobrevivir a una lista vacía.** Es la regresión clásica de los gráficos propios y hay test instrumentado para ello.

---

## 11. Animaciones — catálogo completo

| Qué | Cómo | Duración |
|---|---|---|
| Ondas del líquido | `rememberInfiniteTransition`, dos fases | 3200 ms y 4700 ms, `LinearEasing` |
| Nivel del líquido en marcha | directo, sin interpolación | — |
| Nivel en reinicio o cambio de fase | `spring(dampingRatio = 0.75f, stiffness = StiffnessLow)` | — |
| Subrayado de pestaña | `animateDpAsState(spring(dampingRatio = 0.85f))` | — |
| Thumb del toggle | `spring(dampingRatio = 0.7f)` | — |
| Aparición de «reiniciar» | `fadeIn` / `fadeOut` | 150 ms |
| Parpadeo de `¡TIEMPO!` | `infiniteRepeatable` alpha 1,0 ↔ 0,45 | 900 ms |
| Entrada de barras de la semana | `animateFloatAsState` escalonado por índice | 40 ms de desfase |

**Ninguna animación bloquea una acción del usuario.** Si el usuario toca `PAUSAR` mientras el nivel se está asentando, la pausa se aplica en el mismo frame.

---

## 12. Accesibilidad

- Todas las áreas táctiles ≥ 48 × 48 dp, aunque el texto sea pequeño.
- La cifra del temporizador lleva `contentDescription` legible («18 minutos y 42 segundos restantes»), no el texto crudo `18:42`.
- El tomate lleva `contentDescription` con la fase y el progreso; los `Text` que lo componen van con `clearAndSetSemantics {}` para no leerse dos veces.
- Los puntos de ciclo se anuncian como «pomodoro 2 de 4», no punto a punto.
- La fase nunca depende solo del color (§2.4).
- Se respeta el escalado de fuente del sistema hasta 200 %: el bloque del temporizador se centra y el diámetro baja a 224 dp cuando no cabe. **Ninguna medida en `sp` está fijada con `TextUnit.Unspecified` ni convertida a `dp`.**
