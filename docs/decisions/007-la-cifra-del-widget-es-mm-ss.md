# 007 — La cifra del widget es `MM:SS`, y el estado se pinta con un solo `TextView` alternativo

Fecha: 2026-07-27
Estado: aceptada

Resuelve la **pregunta abierta 1** de §13 de `CLAUDE.md`.

## Contexto

§13 dejaba abierto qué muestra la cifra del widget de 40 dp, con esta propuesta:

> `MM` a secas cuando quedan ≥ 1 min, `SS` en rojo cuando queda menos.

Al implementar F8 esa propuesta resultó incompatible con el mecanismo elegido en el ADR 001, y hay que
elegir una de las dos cosas.

El `Chronometer` de `RemoteViews` es lo que hace que el descuento no cueste nada: tickea **dentro del
proceso del launcher** y nunca despierta el nuestro. Pero su API es cerrada:

- `setChronometerCountDown(true)` cuenta hacia atrás y pinta el tiempo restante.
- El formato se controla con `setFormat("%s")`, donde `%s` es **el tiempo completo** que el propio
  `Chronometer` compone (`MM:SS`, o `H:MM:SS` pasada la hora). No hay forma de decirle «solo los
  minutos».
- Tampoco se puede congelar en un valor arbitrario: un `Chronometer` corre o se para en su base.

Mostrar solo los minutos exigiría repintar el widget nosotros cada vez que cambia el minuto. Son ~25
`updateAppWidget()` por pomodoro, cada uno con serialización e IPC — exactamente el coste que el ADR 001
rechazó al descartar Glance, y por el mismo motivo.

## Decisión

**1. La cifra es `MM:SS`, pintada por el `Chronometer`.** Cero despertares del proceso durante el slot,
que es la propiedad por la que se eligió `RemoteViews`.

**2. El descuento y la cifra estática son dos vistas distintas** apiladas en el mismo `FrameLayout`, y
solo una está visible:

| Estado | Vista visible | Contenido |
|---|---|---|
| `RUNNING` | `Chronometer` | descuento `MM:SS`, ticado por SystemUI |
| `PAUSED` | `TextView` | el restante congelado, más el glifo `❚❚` |
| `IDLE` | `TextView` | el glifo `▸` en el acento de la fase |
| `RINGING` | `TextView` | `!`, con el borde alternando |

Es la consecuencia directa de que un `Chronometer` no se pueda parar donde uno quiera.

**3. El tamaño baja de 15 sp a 13 sp** y usa Space Grotesk con figuras tabulares. `24:58` son cinco
glifos en 40 dp menos 2 dp de padding; a 15 sp no caben con holgura, que es justo lo que §7.2 del
design-spec avisaba. **Queda por medir en dispositivo durante la checklist de F8** — es el primer número
que hay que tocar si se ve apretado.

## Alternativas descartadas

- **Solo minutos, repintando cada 60 s.** Cumpliría la propuesta original a cambio de ~25 despertares del
  proceso por pomodoro. Es la decisión que el ADR 001 ya tomó en contra.

- **Solo minutos con un `Chronometer` y `setFormat`.** No se puede: `%s` es el tiempo completo ya
  formateado. Se puede envolver en texto (`"%s restantes"`), no recortar.

- **Los segundos en rojo durante el último minuto.** Requiere un cambio de color a un instante concreto,
  es decir, un repintado programado a falta de 60 s. Es un solo despertar por slot y sería asumible, pero
  se descarta por ahora: sin el cambio de formato que lo motivaba, el color por sí solo no aporta
  información que el propio descuento no dé ya. Reconsiderable en v1.1.

- **Dos líneas, minutos grandes y segundos pequeños.** En 40 dp no hay sitio para dos líneas legibles
  sobre el tomate.

## Consecuencias

- El widget **nunca se refresca por tiempo mientras corre el slot**. El único caso acotado sigue siendo el
  parpadeo de `RINGING`, con tope de 60 s (`WidgetStateCollector.BLINK_BUDGET_MS`).

- Hay dos vistas de texto en el layout donde una habría bastado si el `Chronometer` fuese congelable. El
  coste es un `View` extra en el árbol de `RemoteViews`, inapreciable.

- El párrafo de la pregunta abierta 1 en §13 de `CLAUDE.md` y la nota de §7.2 del design-spec quedan
  resueltos por este ADR; ambos apuntan aquí.
