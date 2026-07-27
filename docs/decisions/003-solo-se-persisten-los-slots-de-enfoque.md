# 003 — Solo se persisten los slots de enfoque

Fecha: 2026-07-27
Estado: aceptada

## Contexto

Hay que decidir qué se escribe en la tabla `focus_session` y qué no. Un ciclo pomodoro genera dos
tipos de slot: enfoque y descanso (corto o largo). Además, un slot puede terminar de tres formas
distintas: completarse, saltarse a mitad, o reiniciarse a mitad.

Las métricas que pide la v1.0 son: minutos enfocados por día/semana/mes, pomodoros completados y
racha de días. Ninguna necesita datos de descansos.

## Decisión

1. **Solo se persisten slots de `FOCUS`.** Los descansos no generan filas.

2. **La columna `slot_type` existe igualmente** y se rellena siempre con `FOCUS`. Es una columna de
   una letra de coste que deja la puerta abierta a registrar descansos en el futuro sin migración de
   esquema.

3. **Un enfoque completo** → fila con `completed = 1` y `actual_focus_ms` = tiempo real transcurrido
   **excluyendo las pausas**.

4. **Un enfoque saltado o reiniciado a mitad** → fila con `completed = 0`, **pero solo si
   `activeElapsedMs ≥ 60 s`** (`MIN_RECORDED_FOCUS_MS`). Por debajo se descarta sin más.

5. **Consecuencia semántica que hay que reflejar en el copy de Estadísticas:**
   - «Tiempo enfocado» suma `actual_focus_ms` de **todas** las filas, completas y parciales. El
     tiempo enfocado es tiempo enfocado, se haya llegado o no al final.
   - «Pomodoros completados» cuenta **solo** las filas con `completed = 1`.
   - La racha usa **solo** días con al menos un `completed = 1`.

## Alternativas descartadas

- **Persistir también los descansos.** No alimenta ninguna métrica de v1 y ensucia todas las
  agregaciones: cada `SUM(actual_focus_ms)` tendría que llevar un `WHERE slot_type = 'FOCUS'`, y
  antes o después alguien se olvidaría en una query y las estadísticas mentirían. Se puede añadir
  después sin migración gracias al punto 2.

- **Registrar solo enfoques completos y descartar los parciales.** Es la opción más simple y la más
  común en las apps del sector, pero pierde información real: veinte minutos de trabajo antes de que
  te interrumpan siguen siendo veinte minutos de trabajo. Ver esos minutos en las estadísticas es
  precisamente lo que hace útil el historial.

- **Registrar todos los parciales sin umbral mínimo.** Un start/stop accidental —y con un widget de
  un solo toque en el escritorio, van a pasar— generaría filas de 3 segundos. Sesenta segundos es un
  umbral que descarta el ruido sin descartar nada que el usuario reconozca como trabajo.

- **Un umbral proporcional** (por ejemplo, el 10 % de la duración planificada) en vez de 60 s fijos.
  Más elegante sobre el papel, pero con un enfoque de 25 minutos daría 2,5 minutos, y descartar dos
  minutos de trabajo real es peor que registrar un minuto de ruido.

- **Marcar los parciales con una columna `abandoned` en vez de `completed = 0`.** Es lo mismo con
  otro nombre y una columna más.

## Consecuencias

- La tabla es pequeña: un usuario intensivo con 12 pomodoros diarios genera ~4.400 filas al año. No
  hay que pensar en purgas ni en particionado.

- Todas las queries de agregación son directas, sin filtros por tipo de slot. Menos superficie para
  el error de olvidar un `WHERE`.

- **Si algún día se quieren estadísticas de descansos** («¿respetas los descansos?»), hay que:
  (a) empezar a escribir filas con `slot_type` distinto de `FOCUS`, (b) añadir el filtro a **todas**
  las queries existentes, y (c) escribir un test que verifique que ninguna agregación de enfoque
  cuenta descansos. El punto (b) es el trabajo real y por eso no se hace ahora «por si acaso».

- `MIN_RECORDED_FOCUS_MS = 60_000` vive en el `companion object` de `TimerSettings` y está cubierto
  por `CompleteSlotUseCaseTest` en los dos lados del umbral.
