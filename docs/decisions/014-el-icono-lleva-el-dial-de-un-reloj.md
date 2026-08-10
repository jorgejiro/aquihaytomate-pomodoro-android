# 014 · El icono lleva el dial de un reloj, y marca las 5:05

**Fecha**: 2026-08-10 · **Estado**: aceptada · **Versión**: 1.3

## Contexto

El icono de la 1.2.0 arregló un problema y creó otro.

Hasta la 1.1.0 el fondo del adaptativo era una placa casi negra con el tomate dibujado pequeño en el
primer plano. Sobre cualquier fondo de pantalla que no fuera negro eso se leía como una pegatina negra,
porque el launcher **siempre** pinta la capa de fondo y **siempre** recorta las dos con su máscara. La
salida fue hacer que la placa *fuera* la fruta: el degradado rojo llena los 108 dp y la máscara da la
forma redonda.

Lo que se perdió por el camino es el contorno. **La silueta del tomate ya no la dibujaba el icono, la
dibujaba el recorte**: no había nada trazando un borde, porque el rojo llegaba hasta el filo. Puesto al
lado de [Tomato](https://github.com/nsh07/Tomato) en el mismo escritorio, la diferencia no era el fondo
blanco de ellos, era que ellos dibujan el contorno de la fruta y nosotros no.

Y aparte de eso, un problema independiente: el icono no decía en ningún momento que la app fuera un
temporizador. Un círculo rojo con tres hojas es un tomate, no un pomodoro.

Se revisaron tres iconos del género en Play. El que resolvió la duda fue el de
[indieappslab](https://play.google.com/store/apps/details?id=io.github.indieappslab.pomodoro), por una
razón que no es estética: **su contorno no es una silueta de fruta, es el bisel de un reloj**. Cierra la
forma *por dentro*, así que funciona sobre cualquier fondo de pantalla sin necesitar placa.

## Decisión

**El primer plano del icono es un dial de reloj pintado sobre la fruta**, y el fondo sigue siendo el
degradado rojo a sangre de la 1.2.0.

- Marcas de cinco en cinco a radio 31,5: las de 3, 6 y 9 más largas y gruesas.
- **Dos agujas, a las 5:05.** La corta y gruesa marca las cinco, que en la escala de minutos son los
  **25 de un bloque de enfoque**; la larga y fina marca la una, que son los **5 del descanso corto**. El
  icono lleva escritos los dos valores por defecto de la app en la única notación que un reloj sabe leer.
- Las agujas son **cápsulas**: grosor constante —2,6 y 4,2— y puntas redondeadas, con los largos en la
  proporción clásica de un reloj, 25,5 y 18,5. Más el **remache**, una redonda limpia de radio 3,6.
- El cáliz se queda arriba, donde le toca, pero **más pequeño** que en la 1.2.0.
- Sin aro y sin número: nada de cajas alrededor de nada, que es la regla del design-spec.

Las agujas se dibujaron primero como polígonos que se estrechan hacia la punta, que es lo que da una
aguja «de reloj de pulsera». El efecto lateral es que las dos **nacen anchas en el eje** y, con el
remache encima, el centro sale abultado sin que ese bulto signifique nada. Con grosor constante el
centro queda limpio y el remache se puede quedar. Se compararon cinco variantes de aguja y de remache en
[`F-variantes-manecilla.png`](../propuestas-icono/F-variantes-manecilla.png); la hora se alargó de 15,5 a
18,5 tras verlas en fila, porque a 15,5 se leía como un muñón.

Cuatro detalles que solo aparecen al dibujarlo, y que están en los comentarios de los drawables porque no
se deducen del resultado:

1. **No hay marca a las 12.** Caería justo debajo del tallo, que tiene 3 dp de ancho y arranca en y=23,
   así que sería tinta invisible. **El tallo es el doce**: el rabillo de la fruta indexa el dial.
2. **Las dos agujas tienen que contrastar mucho** en largo y grosor. A las 5:05 están a 120°, y con
   pesos parecidos los dos brazos forman una uve simétrica: el icono se lee como una flecha, no como un
   reloj.
3. **En el monocromo, cáliz y tallo son un solo contorno.** El monocromo cala el reloj con la regla
   `nonZero`, y como agujeros separados el cáliz y el tallo se solapan entre y=26,4 y 28,4: dos agujeros
   superpuestos suman winding −2, que no es cero, así que la intersección se vuelve a rellenar y al cáliz
   le sale una verruga. El tallo se injerta en el vértice central superior del cáliz.
4. **En el monocromo las agujas arrancan en radio 3,75, no en el centro**, para que no se crucen entre
   ellas y no repitan el mismo problema en medio del icono. Ahí no hay remache dibujado: **el blanco que
   queda entre las dos *es* el remache**. Y el sentido de recorrido de cada contorno se **calcula**
   comparando su área con signo contra la del cuerpo, no se elige a ojo: invertir la *normal* de una
   cápsula no invierte su winding, la refleja, y así es como las agujas salieron rellenas en vez de
   caladas en el primer intento.

## Alternativas descartadas

Se renderizaron siete direcciones completas con las máscaras reales —círculo y squircle—, a 216 y a
48 px y sobre fondo de pantalla claro y oscuro. Están en
[`docs/propuestas-icono/`](../propuestas-icono/README.md) con el generador, que se conserva para poder
reabrir la discusión sin volver a dibujar.

- **Bisel de reloj** (aro de hueso alrededor del dial). Es la que se lee antes como temporizador y la
  que mejor devuelve el borde, pero el aro es una caja alrededor de algo y mete bastante hueso en el
  escritorio. Quedó como segunda opción.
- **Arco de 25 minutos** en vez de agujas. La más sobria en grande y la más débil en pequeño: a 48 px se
  lee como un icono de recarga. En el escritorio manda el tamaño pequeño.
- **Tomate visto desde arriba**, con el cáliz en el centro haciendo de eje. La idea más propia de todas,
  pero el centro se empasta —aguja sobre cáliz sobre remache— y en monocromo el verde desaparece y con
  él la mitad del hallazgo.
- **El tomate que se vacía**, el elemento de la pantalla principal. Para que el hueco vacío se vea
  oscuro hace falta fondo oscuro, así que **vuelve la placa** y con ella el problema de la 1.1.0.
- **El 25 escrito** en Space Grotesk. La más legible de las siete sin discusión, pero casa el icono con
  un ajuste que el usuario puede cambiar en Ajustes.
- **Placa de hueso**, la vía de Tomato: la única con el contorno completo de la fruta, a cambio de meter
  en el escritorio el único blanco de todo el producto. Y no resuelve el problema de fondo, solo lo
  cambia de lado: **una placa no salva de los dos extremos, elige de qué lado fallar.**
- **Temporizador de cocina mecánico**, al estilo del icono de
  [zetabitapps](https://play.google.com/store/apps/details?id=com.zetabitapps.pomodoro): cáliz granate,
  la junta de la tapa en perspectiva con sus muescas y el triángulo sobre el 25. Se dibujó en tres
  variantes y se descartó en revisión: la banda de muescas se convierte en una raya de puntos a 48 px y
  los tres números en tres manchas.

## Consecuencias

- El icono dice lo que hace la app, y la fruta se lee como un objeto con borde sin necesidad de placa.
- **Se mantiene lo que ganó la 1.2.0**: no hay ninguna superficie oscura que pueda leerse como pegatina.
  Lo que no se recupera es el contorno *exterior*; las marcas pegadas al filo lo insinúan, no lo trazan.
  Si en dispositivo real eso sigue molestando, la vía es el aro de la propuesta A al límite de la zona
  segura, tan fino que sea un filo y no un bisel.
- El monocromo pasa de una silueta llena a una silueta con el reloj calado. Sigue siendo **un solo
  path**, como pide el design-spec.
- **El icono de notificación y el del splash no cambian.** El de notificación mide 24 dp y las marcas de
  1,5 dp no sobreviven a ese tamaño ni a la máscara alfa; el del splash enlaza con la pantalla
  Temporizador, que sigue siendo el tomate de líquido, no con el icono. Son siluetas de la fruta y
  siguen siendo correctas.
- **El widget tampoco cambia.** Tiene su propio lenguaje —nivel de líquido más el glifo de la acción que
  hará el toque— y el dial no le aporta nada: ahí la cifra ya está escrita.
- Hay que volver a subir a mano el icono de 512 a la ficha de Play. `generar-assets.py` ya lo produce
  actualizado.
