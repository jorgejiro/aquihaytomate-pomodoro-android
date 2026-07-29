# 012 · Un sonido para acabar el pomodoro y otro para acabar el descanso

## Contexto

Hasta la 1.0.1 había **un solo** ajuste de sonido, `alert_sound`, con la campana por defecto. Ese sonido
—el que fuera— sonaba igual en los dos extremos de un slot: al terminar un pomodoro y al terminar un
descanso.

Los dos momentos no dicen lo mismo. Terminar un pomodoro es una buena noticia —te has ganado el descanso—
y el aviso puede permitirse ser agradable. Terminar el descanso es lo contrario: hay que sacarte de lo que
estuvieras haciendo y devolverte al trabajo, y ahí un tintineo suave es justo lo que no se oye.

Con un solo ajuste, quien quiere un sonido amable para el pomodoro se lleva el mismo sonido amable cuando
tiene que volver al tajo, y quien quiere que el segundo le espabile se come un pitido cada vez que acaba de
concentrarse. No hay elección que satisfaga las dos.

Además, el selector de sonidos **no dejaba escuchar nada**: al tocar un nombre se aplicaba y el panel se
cerraba. Cinco etiquetas —Silencio, Campana, Cuenco tibetano, Digital, Suave— que no significan nada hasta
que las oyes, así que en la práctica el ajuste no se podía usar salvo a base de terminar pomodoros de
prueba.

## Decisión

**Dos ajustes independientes**, `focus_alert_sound` y `break_alert_sound`, con valores por defecto
distintos, y un selector que suena al tocarlo.

Los cuatro clips, medidos:

| clip | centroide | duración | RMS | energía total | RMS > 2 kHz |
|---|---|---|---|---|---|
| `bowl` | 506 Hz | 2,90 s | −14,8 dB | 4229 | −37,6 dB |
| `soft` | 508 Hz | 1,40 s | −15,9 dB | 1578 | −42,9 dB |
| `bell` | 1112 Hz | 2,60 s | −15,3 dB | 3353 | −26,3 dB |
| `digital` | 2170 Hz | **0,72 s** | **−7,2 dB** | **6053** | **−9,9 dB** |

- **Fin de pomodoro → `bowl`** (cuenco tibetano). El más grave y el más largo: entra y decae solo, se lee
  como recompensa.
- **Fin de descanso → `bell`** (campana). Más brillante que el cuenco —1112 Hz de centroide contra 506, y
  11 dB más de energía por encima de 2 kHz— y suena 2,6 s.

**El primer valor por defecto fue `digital`, y se cambió tras escucharlo en un dispositivo real.** Sobre el
papel es el candidato obvio: el más agudo, 8 dB más de RMS que la campana y el que más energía total tiene
de los cuatro. Pero dura 0,72 s, y en el altavoz de un móvil eso es un blip que se acaba antes de que lo
registres si no estás mirando la pantalla. Destacar no es cuestión de nivel de pico, sino de insistir: la
campana suena tres veces y media más tiempo, y es la que se oye. La medida describe la señal, no lo que
llega al oído a través de un altavoz pequeño y a dos metros de distancia.

El sonido lo decide **el slot que acaba**, no el que empieza, en la función pura
`TimerSettings.alertSoundFor(finishedType)` — por el mismo motivo que existe `autoStartsInto`: que
`CompleteSlotUseCase` y cualquier otro camino que alerte no puedan discrepar. Importa especialmente con el
auto-inicio del descanso activado, donde en el mismo instante acaba un pomodoro y empieza un descanso: lo
que suena es el del pomodoro.

En el selector, los dos de sonido **se quedan abiertos** y reproducen la opción al tocarla, con un `HECHO`
explícito para cerrar. El resto de selectores —duraciones, ciclo, idioma— siguen cerrándose al elegir: 25
minutos no hay que compararlo con nada. La escucha usa el mismo `AlertPlayer` que la alerta real, con la
vibración a cero, así que respeta el silencio, el No molestar y el volumen de alarma igual que ella: si el
móvil está en silencio no se oye nada, que es exactamente lo que pasará cuando termine el pomodoro.

## Alternativas descartadas

- **Un sonido con un modificador para el descanso** (más volumen, repetido dos veces). Suena a parche, no
  se puede describir en una línea en Ajustes y no resuelve el caso de quien simplemente quiere otro timbre.
- **Un sonido por cada uno de los tres tipos de slot**, con el descanso largo aparte. Tres ajustes para una
  distinción que nadie pide: lo que cambia es «he terminado de trabajar» frente a «tengo que volver», y el
  descanso largo cae del mismo lado que el corto.
- **Un botón de altavoz junto a cada opción** para escucharla sin seleccionarla. Es un objetivo táctil más
  por fila y duplica los caminos; como cambiar de sonido es instantáneo y reversible con el toque
  siguiente, seleccionar *es* la forma natural de probar.
- **Sonidos nuevos** más marcados para cada extremo. Los cuatro clips ya cubren el rango de grave a agudo
  —hay un factor cuatro entre el cuenco y el digital—, y añadir audio engorda el APK sin necesidad.
- **`digital` para el fin del descanso**, que es lo que decían las medidas. Descartado por corto: ver
  arriba. Sigue estando en el catálogo para quien lo prefiera.

## Consecuencias

- Quien ya tenía un sonido elegido **lo conserva en los dos extremos**: ambas claves nuevas caen en la
  retirada `alert_sound` mientras no se toque ninguno de los dos ajustes. No hay migración, solo una
  lectura de respaldo, igual que con `auto_start_next` cuando se partió en dos.
- `AlertPolicy.decide(settings, …)` ya no puede resolver el sonido con los ajustes solos: ahora pide
  también el slot que acaba.
- La sección de Avisos en Ajustes pasa de una fila a dos, cada una con su subtítulo —«Toca descansar» y
  «Vuelta al tajo»— para que se entienda cuál es cuál sin abrirlas.
- Un pomodoro puede quedar en silencio y el descanso no, o al revés. La decisión de no hacer ruido sigue
  siendo del reproductor según la política; lo que cambia es qué sonido se le pide.
