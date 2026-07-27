# 001 — Widget 1×1 con RemoteViews y Chronometer

Fecha: 2026-07-27
Estado: aceptada

## Contexto

El widget de escritorio de 1×1 es la razón de existir del proyecto: todas las apps de pomodoro que
el usuario ha probado tienen widgets de 2×2 como mínimo y le ocupan demasiado escritorio.

A diferencia del widget de Bebe Agua, este **sí renderiza datos que cambian**: muestra el tiempo
restante del slot en curso, que se actualiza continuamente durante 25 minutos. Y tiene que soportar
dos gestos distintos sobre una sola superficie de 40 × 40 dp: iniciar/pausar y reiniciar.

En `docs/decisions/002-widget-de-escritorio-con-glance.md` de Bebe Agua se eligió Glance con el
argumento de no meter layouts XML en un proyecto 100 % Compose. Aquí hay que revisar esa decisión
porque las restricciones son distintas.

## Decisión

1. **`AppWidgetProvider` + `RemoteViews` con layout XML**, no Glance.

   El motivo determinante es que `RemoteViews` da acceso al widget `Chronometer` de Android:

   ```kotlin
   views.setChronometer(R.id.countdown, base, null, true)
   views.setChronometerCountDown(R.id.countdown, true)
   ```

   El `Chronometer` **tickea dentro del proceso del launcher**, sin despertar el nuestro. Con Glance
   habría que llamar a `updateAll()` cada minuto —o cada segundo si se quieren mostrar segundos—, y
   cada llamada implica recomposición, serialización e IPC. Son ~25 despertares del proceso por
   pomodoro frente a **cero**.

2. **El descuento lo pinta el `Chronometer`; el resto del widget se refresca solo en transiciones**
   (start, pause, resume, skip, complete). Un colector con scope de aplicación observa
   `TimerStateRepository.state` con `distinctUntilChangedBy { Triple(status, slotType, endAtEpochMs) }`
   y `debounce(250)`. Nunca se refresca por segundo.

3. **El tomate se rasteriza a un `Bitmap`** con `TomatoBitmapRenderer` y se pone con
   `setImageViewBitmap`. La geometría de la superficie del líquido sale de
   `domain/render/TomatoGeometry.kt`, una función pura sin dependencias de Compose ni de Android,
   **compartida con el `Canvas` de la app**. Es el mismo patrón que `resolveDefaultIntakeSize` en
   Bebe Agua: si app y widget calculasen la forma por separado, divergirían.

4. **Un toque = iniciar/pausar/reanudar. Doble toque rápido = reiniciar.**

   El toque simple se aplica **de inmediato**, sin esperar la ventana del doble toque: retrasar la
   acción 400 ms se percibe como lag. Si llega un segundo toque dentro de la ventana, se deshace el
   toggle y se ejecuta el reinicio.

5. **La ventana del doble toque se persiste, no se guarda en memoria.** `TapGate` escribe el
   timestamp del último toque en el DataStore del estado del temporizador. El proceso puede morir
   entre el primer toque y el segundo, y una variable de instancia se perdería.

6. **`updatePeriodMillis="0"`.** El mínimo que respeta el sistema son 30 minutos, que no sirve para
   nada aquí. El refresco lo controlamos nosotros.

7. **`previewLayout`** (targetSdk 36) con una copia del layout real y cifra fija, más `previewImage`
   como respaldo para hosts antiguos.

## Alternativas descartadas

- **Glance con `updateAll()` por minuto.** Coste de batería y de despertares del proceso
  desproporcionado para mostrar un número que el sistema sabe pintar solo. Además Glance arrastra
  `androidx.work:work-runtime`, que este proyecto no necesita para nada más.

- **Glance embebiendo un `Chronometer` con `AndroidRemoteViews`.** Técnicamente posible y se
  consideró en serio: daría el tick gratis manteniendo la API declarativa. Se descarta porque
  acabaría siendo un híbrido con las dos capas presentes —el layout XML del `Chronometer` existiría
  igual— más la dependencia de Glance y su WorkManager. Si vas a tener el XML, tenlo entero.

- **Pulsación larga para reiniciar.** Es lo que pide el instinto, pero **no funciona**: en cualquier
  launcher (Nova, Pixel Launcher, One UI) el long-press sobre un widget lo captura el propio
  launcher para arrastrarlo o abrir su menú. El evento nunca llega a la app. Es una limitación de
  plataforma, no una preferencia de diseño.

- **Tap que cicla iniciar → pausar → reiniciar.** Un solo gesto, sin ventanas temporales, pero
  reiniciar por accidente al tercer toque es demasiado fácil y destructivo.

- **Dos widgets 1×1 separados** (uno de play/pausa, otro de reset). Duplica el espacio ocupado, que
  es justo lo que el proyecto quiere evitar.

- **Retrasar el toque simple 400 ms** para poder distinguirlo del doble sin deshacer nada. Se
  descarta: 400 ms de latencia en un botón se nota y se percibe como que el widget «va lento».

## Consecuencias

- **Se rompe la regla de «no usar XML para UI» de `CLAUDE.md`**, y está aceptado explícitamente: la
  regla se aplica a la UI de la app, y el layout del widget es la excepción documentada (junto con
  el splash).

- El proyecto **no depende de Glance ni, por tanto, de WorkManager**. El APK es más pequeño que el
  de Bebe Agua en esa parte.

- `TomatoBitmapRenderer` tiene que producir un bitmap del tamaño correcto para la densidad del
  dispositivo y **respetar el límite de memoria de `RemoteViews`** (el `Bundle` de una transacción
  Binder ronda 1 MB; un bitmap de 40 dp a xxxhdpi son ~160 × 160 px ARGB, unos 100 KB: sobra
  margen, pero no se puede rasterizar «por si acaso» a un tamaño mayor).

- El estado `RINGING` es el **único** caso en que el widget se refresca por tiempo, para hacer
  parpadear el borde. Está acotado a 60 segundos y luego para.

- Hay que probar en **Nova Launcher** (el que usa el autor), Pixel Launcher y One UI: algunos
  launchers ignoran `targetCellWidth` y colocan el widget con el tamaño de `minWidth`.

- La ventana de 400 ms es una estimación. Se calibra en dispositivo real durante la fase F8; si
  produce falsos positivos o negativos, es el primer número que hay que tocar.
