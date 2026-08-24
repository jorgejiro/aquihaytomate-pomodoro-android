# 008 · El onboarding exige los dos permisos, aunque la app funcione sin ellos

Fecha: 2026-07-28 · Estado: aceptada

## Contexto

La tercera página del onboarding ofrecía los dos permisos que el temporizador usa —`POST_NOTIFICATIONS`
y `SCHEDULE_EXACT_ALARM`— como dos controles de texto sueltos, `Permitir avisos` y `Permitir alarmas
exactas`, con una línea debajo que decía «Opcional. El temporizador funciona sin ellas».

En la primera prueba en dispositivo real aparecieron dos problemas:

1. **Nada indicaba si el permiso estaba pendiente o ya concedido.** Los dos controles se veían idénticos
   antes y después de concederlos, así que al volver de los ajustes del sistema no había forma de saber si
   había servido de algo. Un control que no cambia al pulsarlo se lee como roto.
2. **«Opcional» invita a saltárselos.** Es técnicamente cierto —el ADR 002 coloca la alarma exacta como
   tercera capa de respaldo, y el servicio en primer plano es el mecanismo primario— pero describe la
   arquitectura, no la experiencia. Un usuario que se salta los dos permisos acaba con un temporizador que
   puede llegar tarde con la pantalla apagada y que no avisa cuando termina un slot, y no relacionará eso
   con la pantalla que despachó en el primer arranque.

La tensión de fondo: `SCHEDULE_EXACT_ALARM` **no se puede** volver obligatorio de verdad. Google Play
reserva `USE_EXACT_ALARM` a despertadores y calendarios (CLAUDE.md §3), el usuario puede denegar
`POST_NOTIFICATIONS` para siempre, y no hay API que fuerce ninguno de los dos.

## Decisión

El onboarding **presiona por los dos permisos y deja una salida**:

- Cada permiso es un `SettingsRow` con su estado escrito: `Pendiente` en `AlertAmber` con `Necesario` como
  sublabel, o `Activado` en `TomateBright` sin sublabel y sin chevron. Los mismos componentes y los mismos
  colores que la sección Sistema de Ajustes.
- `EMPEZAR` queda deshabilitado, en `TextGhost`, mientras alguno de los dos siga pendiente.
- Debajo, y solo mientras está deshabilitado, aparece `CONTINUAR SIN ELLOS` en `caption` `TextMuted`. Es
  la válvula de escape, y es obligatoria: sin ella un usuario que ha denegado el permiso de notificaciones
  para siempre no podría pasar de la tercera página del primer arranque nunca más.
- El estado se relee en cada `ON_RESUME` y también en la respuesta del diálogo de permiso, con
  `RefreshPermissionsOnResume`, el mismo composable que usa Ajustes.

En Ajustes **no cambia nada**: allí una alarma exacta denegada sigue describiéndose como una merma de
puntualidad y no como un fallo, que es lo que es.

## Alternativas descartadas

- **Dejarlo como «opcional» y arreglar solo el estado visual.** Resuelve el problema 1 y no el 2. La
  fiabilidad del temporizador es el principio de producto número tres de CLAUDE.md; pedirla en voz baja
  en la única pantalla donde el usuario está dispuesto a configurar cosas es tirar la oportunidad.
- **Bloquear el onboarding sin salida hasta conceder los dos.** Un usuario que deniega
  `POST_NOTIFICATIONS` en el diálogo del sistema no vuelve a verlo: quedaría encerrado en el onboarding
  con la única vía de los ajustes del sistema, y en algunos OEM ni eso. Es un cierre en falso.
- **Declarar `USE_EXACT_ALARM` y quitarse el problema.** Vetado por CLAUDE.md §3 y por la revisión de
  Play: la app no es un despertador ni un calendario.
- **Pedir los permisos justo antes de iniciar el primer pomodoro, no en el onboarding.** Es el patrón
  «in-context» y sería más fino, pero pone un diálogo del sistema entre el usuario y el primer `INICIAR`,
  que es justo el momento que tiene que ser inmediato. Queda anotado para una v1.x si el embudo del
  onboarding resulta ser malo.

## Consecuencias

- El primer arranque tiene un paso más de fricción a cambio de un temporizador que suena a su hora.
- La página 3 pasa a depender del estado de permisos, así que `OnboardingViewModel` recibe
  `TimerAlarmScheduler` y expone `OnboardingUiState`.
- `RefreshPermissionsOnResume` sube de `ui/settings/` a `ui/common/`: lo comparten dos pantallas y dos
  copias habrían divergido.
- Queda una regla nueva para la checklist de F9: **denegar los dos permisos y comprobar que
  `CONTINUAR SIN ELLOS` deja terminar el onboarding**, en Android 12 (donde `POST_NOTIFICATIONS` no
  existe y la fila va directa a los ajustes) y en Android 14+.

---

## Añadido el 2026-08-24 · la página nueva va antes, no después

El onboarding gana una **quinta página** —cuántas veces suena el aviso al terminar cada fase, ver el
añadido del ADR 012— y se coloca **cuarta, justo antes de los permisos**. Los permisos siguen cerrando el
onboarding y el gate sigue siendo `EMPEZAR`, sin un solo cambio.

Se probó al revés, con la página nueva la última, y se descartó por dos razones que solo se ven montado:

1. **Un gate con una página detrás no gatea.** Deshabilitar el control de avance en la página de los
   permisos no cuesta nada de saltarse cuando la siguiente está a un deslizamiento del dedo. Se puede
   tapar bloqueando el gesto del pager (`userScrollEnabled = false`), pero eso también quita el volver
   atrás, y se acaba defendiendo con dos mecanismos lo que aquí sostiene uno solo: **si los permisos son
   lo último, no hay nada detrás que alcanzar**.
2. **El control de terminar es el sitio natural del gate.** `EMPEZAR` en gris sobre las dos filas que
   dicen `Pendiente · Necesario` se lee como lo que es. En una página que habla de sonidos se lee como una
   avería.

Que la última pregunta del onboarding sea la de los permisos tiene además el efecto que buscaba este ADR:
la pantalla en la que el usuario decide si sigue adelante es la que le explica por qué hacen falta.
