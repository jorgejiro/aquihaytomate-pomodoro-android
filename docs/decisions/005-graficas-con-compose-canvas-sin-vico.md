# 005 — Gráficas con Compose Canvas, sin librería de gráficos

Fecha: 2026-07-27
Estado: aceptada

## Contexto

La pantalla de Estadísticas necesita cuatro visualizaciones: barras diarias de la semana, mapa de
calor mensual, tira de racha de 30 días y sparkline de 30 días.

La app de referencia funcional, [Tomato](https://github.com/nsh07/Tomato), usa
[Vico](https://github.com/patrykandpatrick/vico) para sus gráficas. Es la librería de charts más
madura del ecosistema Compose.

Al mismo tiempo, el proyecto tiene una estética muy concreta y muy poco convencional (`docs/design-spec.md`):
fondo negro, sin ejes, sin rejilla, sin marco, sin leyendas, sin tooltips flotantes, y un detalle
que aparece como una línea de texto bajo el gráfico al tocar.

## Decisión

**Todas las gráficas se dibujan a mano con Compose `Canvas` / `Modifier.drawBehind`**, en
componentes propios en `ui/common/`. No se añade Vico ni ninguna otra librería de gráficos.

Componentes: `WeekBarChart`, `MonthHeatmap`, `StreakStrip`, `Sparkline`.

Notas de implementación que forman parte de la decisión:

1. **`MonthHeatmap` es un solo `Canvas` con un bucle de 7 × N `drawRoundRect`**, no un `LazyGrid` ni
   35 Composables. Son como mucho 35 rectángulos: un Canvas es más barato, y además permite dibujar
   el anillo de «hoy» alrededor de una celda sin desalinear la rejilla. El toque se resuelve con
   `pointerInput { detectTapGestures { cellAt(it) } }` y aritmética.

2. **Ningún gráfico lleva ejes, rejilla ni marco.** Solo las barras o celdas, la etiqueta del eje X y,
   cuando aporta, la línea de objetivo punteada.

3. **Toda gráfica tiene que sobrevivir a una lista vacía.** Es la regresión clásica de los gráficos
   propios (división por cero al normalizar, `maxOf()` sobre lista vacía, `Path` sin puntos) y hay un
   test instrumentado, `StatsScreenTest`, que renderiza el estado vacío.

## Alternativas descartadas

- **Vico.** Es buena librería, pero está diseñada para gráficas convencionales: ejes, marcadores,
  leyendas, escalas. Aquí habría que desactivar casi todo lo que aporta y luego pelearse con su
  sistema de theming para que respete una paleta que no es Material. El resultado sería más código
  de configuración que el de dibujar 26 dp de rectángulo redondeado, con una dependencia externa
  encima, y con menos control sobre el pixel.

  Además, ni `StreakStrip` ni la interacción de «detalle como línea de texto bajo el gráfico» encajan
  en su modelo; habría que escribirlos a mano igualmente y acabaríamos con dos formas distintas de
  dibujar en la misma pantalla.

- **MPAndroidChart.** Es de Views, no de Compose, obliga a `AndroidView` y su estética es
  irremediablemente de otra época.

- **YCharts / Compose-Charts** y demás alternativas menores. Menos maduras que Vico y con el mismo
  problema de fondo: están pensadas para gráficas con ejes.

- **Dibujar con Composables en vez de `Canvas`** (una `Row` de `Box` con altura proporcional para las
  barras). Funciona para el gráfico semanal, pero no para el sparkline ni para el anillo de «hoy» del
  heatmap, y mezcla dos técnicas. Con `Canvas` todo sigue el mismo patrón.

## Consecuencias

- **Cero dependencias nuevas** para toda la pantalla de Estadísticas.

- El precedente ya existe en el repo del autor: `ui/common/ProgressRing.kt` de Bebe Agua es un
  `Canvas` a mano con `animateFloatAsState`, y funciona bien. Aquí se sigue el mismo patrón.

- **Coste real**: cuatro componentes de entre 40 y 90 líneas cada uno. Es menos de lo que parece
  porque no hay ejes que calcular ni escalas que renderizar.

- **Lo que se pierde**: si algún día se quiere una gráfica de verdad complicada (ejes con ticks
  automáticos, zoom, scroll horizontal sobre un año de datos), habrá que escribirla o reconsiderar
  esta decisión. Para el alcance de la v1.0 no aparece nada así.

- **La accesibilidad hay que ponerla a mano.** Un `Canvas` no tiene semántica: cada gráfico lleva su
  `contentDescription` con el resumen de los datos, y el detalle al tocar es texto real, no un
  tooltip dibujado.
