# Estado del proyecto — dónde se quedó y por qué

> Documento de reentrada. Si vuelves a esta app dentro de seis meses —tú o un agente—, **lee esto antes que
> nada**: `CLAUDE.md` dice cómo se trabaja aquí, `docs/design-spec.md` cómo se ve, y esto **por qué está
> como está** y con qué trampas de plataforma nos hemos peleado ya.
>
> Última revisión: 2026-07-28 · Versión publicada: **1.0.0 (versionCode 2)**

---

## 1. Dónde está la app

Funcionalidad de la v1.0 completa y revisada en un **Samsung Galaxy S25** real (temporizador, widget,
notificación y aviso al reloj). Firmada con la upload key de Bebe Agua y lista para subir a Google Play.

| Superficie | Estado |
|---|---|
| Pantalla del temporizador | Terminada. Tres acciones, tomate tocable, «a continuación» |
| Estadísticas | Terminada, con las cuatro gráficas en Canvas |
| Ajustes | Terminada, con los dos auto-inicios separados |
| Onboarding | Cuatro páginas: qué es · duraciones · tu ciclo · widget y permisos |
| Widget 1×1 | Terminado y probado en el escritorio; **el bug que impedía colocarlo está corregido** |
| Notificación ongoing | Cuerpo propio con cifra grande y tres controles sin desplegar |
| Aviso de fin de intervalo | Llega al Garmin con dos acciones, verificado por el autor |

**Lo que no está verificado en dispositivo**, y por eso la recomendación de subir primero a pruebas
internas: los escenarios de §10 de `CLAUDE.md` que exigen tiempo o condiciones raras — pantalla apagada un
slot entero, swipe desde recientes, reinicio a mitad de pomodoro, batería restringida, y el widget en Pixel
Launcher y One UI además de Nova.

---

## 2. Los acuerdos de producto que no se deducen del código

Estas decisiones las tomó el autor por escrito. **No las cambies sin preguntar**, aunque parezcan
arbitrarias:

1. **El widget es el producto.** Una sola casilla. Todo lo demás del proyecto existe para sostener eso.
2. **Solo tema oscuro.** Con una excepción justificada: el cuerpo de la notificación, cuyo fondo lo pinta
   SystemUI, tiene colores claros en `values/colors.xml`.
3. **Los controles son texto, sin cajas.** El glifo del control primario se dibuja, no se escribe.
4. **Auto-iniciar el descanso viene activado; el pomodoro siguiente, no.** La asimetría es intencional: un
   descanso se alarga a propósito. `TimerSettingsTest` la fija.
5. **Terminología**: «Enfoque» y «Descanso» / «Descanso largo» en la app; «Pomodoro» aparece solo en el
   título del aviso de fin. El autor sopesó llamar «Pomodoro» a la fase de enfoque y quedó **pendiente de
   decidir**; si se hace, afecta a pantalla, widget, notificación, estadísticas y `design-spec`.
6. **La notificación persistente no llega al reloj; el aviso de fin, sí.** Se renuncia a pausar desde la
   muñeca a cambio de que la muñeca solo vibre cuando hay algo que decidir.
7. **La notificación vuelve si se descarta** con un intervalo en marcha. Es deliberadamente insistente y
   está documentado dónde retirarlo si Play lo señalara (ADR 010).
8. **`1.0.0` se publicó tras revisión en dispositivo real.** Las 0.9.0 y 0.9.1 fueron internas. El
   `versionCode` sube de uno en uno **en cada subida a Play**, aunque el `versionName` solo cambie el patch.

---

## 3. Trampas de plataforma ya pagadas

Cada una de estas costó una iteración de depuración. Están en los ADR, pero aquí juntas para no repetirlas:

| Trampa | Síntoma | Regla |
|---|---|---|
| `android:configure` en el widget **no es un booleano** | El widget se lista pero al soltarlo Nova dice «la app no está instalada» | No declarar el atributo; `ManifestGuardTest` lo fija |
| `RemoteViews` solo infla una **lista blanca** de vistas | `Space` como espaciador → `BadForegroundServiceNotificationException`, la app «keeps stopping» | Espaciar con `TextView` vacío; `bothBodiesInflate` lo fija |
| Contenido propio colapsado limitado a **48 dp** con `targetSdk` ≥ 31 | La segunda fila de botones no se recorta: desaparece | Una fila; controles como iconos |
| `?android:attr/textColorPrimary` en vistas de notificación | Resuelve oscuro en tema claro **y** oscuro | Colores por cualificador (`values` / `values-night`) |
| Fuentes sin el glifo pedido | `▸` y `❚❚` en Space Grotesk salían como una mota | Dibujar los glifos con `Canvas` |
| `setSilent(true)` | No solo calla: marca como no alertante y **el reloj no la recibe** | Solo en las ongoing, nunca en el aviso de fin |
| Android 13 permite descartar la notificación de un FGS | El temporizador seguía sin nada en pantalla | `deleteIntent` que republica (ADR 010) |
| Descartar desde el reloj no pasa por la acción | La vibración seguía hasta 30 s | `deleteIntent` también en el aviso de fin |
| Las Live Updates de Android 16 (`setRequestPromotedOngoing`) | — | **Incompatibles con vistas propias**: o cifra grande, o promoción |
| R8 renombra las constantes de los enums | El release guardaba `"e"` en la base de datos | Reglas en `proguard-rules.pro`; verificado en el `mapping.txt` |
| Garmin **sí** reenvía las acciones de notificación | (suposición errónea al principio) | Se comprobó con TickTick; el copy debe valer sin nombre de app |
| Alto reservado con celda fija vs. dibujo proporcional al ancho | El mapa del mes se derramaba sobre la leyenda: +3 dp en un móvil de 360, +317 dp en una tablet de 800 | Derivar el alto del ancho real con la misma aritmética que el dibujo; `MonthHeatmapTest` |
| `enableEdgeToEdge()` sin argumentos | Con el tema del **sistema** en claro, los iconos de la barra de estado se pintan oscuros y desaparecen sobre el negro de la app | `SystemBarStyle.dark(...)` explícito: la app solo tiene tema oscuro |
| Play exige que la política **hable de conservación**, no que se deduzca | Rechazo de una actualización de Bebe Agua: «No se especifica una política de conservación de datos» | Sección propia que declara conservación cero del lado del desarrollador y control del usuario en el dispositivo |
| El emulador no deja cambiar el idioma del sistema | `setprop persist.sys.locale` lo bloquea SELinux y Ajustes se cierra al buscar idiomas | Para las capturas, el widget va en la segunda página del escritorio, que no lleva «At a glance» |

---

## 4. Cómo verificar cambios aquí

Lo que ha funcionado, por orden de coste:

1. **Funciones puras con test unitario.** `TimerMath`, `SlotPlanner`, `WidgetReadout`, `TimerSettings`. Si
   una regla de comportamiento se puede expresar sin Android, va aquí — es donde se detectan las
   regresiones en segundos.
2. **Tests instrumentados para lo que solo existe en un dispositivo**: el manifest
   (`ManifestGuardTest`), la forma de las notificaciones (`TimerNotificationFactoryTest`) y el bitmap del
   widget (`TomatoBitmapRendererTest`).
3. **El emulador, con `dumpsys` en vez de la vista.** `adb shell dumpsys notification --noredact` dice
   flags, acciones, `contentView` y textos exactos; es más fiable que mirar una captura. Para el estado del
   motor, `adb shell run-as com.jjrapps.aquihaytomate cat files/datastore/settings.preferences_pb | strings`.
4. **Capturas solo para juzgar diseño**, nunca para verificar lógica. El juego de la ficha de Play se
   regenera con `docs/store-assets/generar-capturas/` —36 imágenes, seis escenas por dos idiomas y tres
   formatos— y ahí están anotadas las trampas de automatizar el launcher. Los dos fallos visuales del
   apartado anterior salieron precisamente de mirar esas capturas en una tablet.

Aviso práctico: automatizar la UI con `adb shell input tap` a ciegas falla la mitad de las veces. Captura,
localiza y **luego** toca; y recuerda que el onboarding tiene cuatro páginas.

---

## 5. Lo siguiente, si se retoma

Por orden de valor aparente:

- **Cerrar §10 en dispositivo** sobre la build de Play y anotar los resultados en
  `docs/f9-verificacion-en-emulador.md`.
- **Quick Settings Tile** para iniciar y pausar desde la persiana. Es la superficie que falta y encaja con
  la filosofía del proyecto.
- **Etiquetas o proyectos por sesión**, con estadísticas desglosadas. La columna `slot_type` de
  `focus_session` ya está preparada para más de un tipo.
- **App Connect IQ para el Garmin**, si se quiere controlar el temporizador desde la esfera y no solo
  responder al aviso. Es un proyecto aparte en Monkey C.
- **Export/import en JSON**, que es lo que convierte el historial en algo que sobrevive a un cambio de
  móvil.

Y una deuda pequeña y concreta: `docs/f9-verificacion-en-emulador.md` describe el estado de la fase F9 y
quedó a medio camino entre bitácora y checklist. Cuando se cierre §10 conviene refundirlo con este
documento.
