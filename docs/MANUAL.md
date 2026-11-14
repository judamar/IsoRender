# Manual de usuario de IsoRender (T-038)

Versión del mod: 0.1.0 · Minecraft 1.12.2 · LiteLoader 1.12.2 · Licencia MIT

Requisitos cubiertos: RF-003 (selección), RF-016 (menú), RF-028 (configuración). Para quien programa el mod, ver `docs/TECNICA.md`.

## 1. Qué hace IsoRender

IsoRender convierte una construcción de tu mundo en una imagen PNG con fondo transparente. Marcas dos esquinas (punto 1 y punto 2), abres un menú donde ves una vista previa en vivo, ajustas la cámara y pulsas **Renderizar**. El mod guarda el PNG en una carpeta de tu juego y te dice la ruta en el chat.

- La vista por defecto es isométrica (rotación 45°, elevación 35,264°). También hay vista en perspectiva.
- La imagen es cuadrada, de 512 a 4096 píxeles de lado, con bordes suavizados y sin halos.
- Se dibujan bloques, bloques con entidad (cofres, camas, carteles, cabezas...) y marcos de objetos, pinturas y armor stands.
- El mod es solo de lectura: **nunca cambia bloques ni entidades de tu mundo**.
- Si algo falla (selección, render o menú), te avisa en el chat en español y el juego sigue abierto.

## 2. Requisitos e instalación

| Necesitas | Detalle |
|---|---|
| Minecraft | Java Edition 1.12.2 (cliente) |
| LiteLoader | LiteLoader 1.12.2 instalado en ese perfil. **No se usa Forge.** |
| Gráficos | GPU con OpenGL y framebuffers (cualquier equipo que ejecute 1.12.2). Para 4096 px con bordes suavizados hace falta una GPU con textura máxima de 8192 px (ver §11). |

Instalación:

1. Instala LiteLoader 1.12.2 en tu perfil de Minecraft 1.12.2 (sigue las instrucciones de LiteLoader).
2. Copia el archivo `mod-isorender-0.1.0-mc1.12.2.litemod` a la carpeta `mods` de tu juego (`<directorio del juego>/mods/`). No hay que descomprimirlo.
3. Inicia el juego con el perfil de LiteLoader. En el menú de mods de LiteLoader debe aparecer «IsoRender 0.1.0».
4. Entra a un mundo. En el chat verás `[IsoRender] Mod cargado (v0.1.0).` cada vez que entras a un mundo.

El mod solo funciona en el cliente: sirve en un mundo de un jugador y en servidores, porque los comandos `/isorender` se interceptan en tu cliente y no se envían al servidor.

## 3. Flujo rápido

1. Apunta a una esquina de la construcción y escribe `/isorender pos1`.
2. Apunta a la esquina opuesta y escribe `/isorender pos2`. El chat muestra el tamaño de la región.
3. Pulsa la tecla **K** (o escribe `/isorender menu`) para abrir el menú.
4. Ajusta la cámara mirando la vista previa.
5. Pulsa **Renderizar**. El menú se cierra y, en el siguiente fotograma, el mod genera el PNG y escribe la ruta en el chat (la ruta es clicable: abre la imagen).

Con una selección completa también puedes renderizar sin abrir el menú: `/isorender render` usa la cámara y la resolución que tenías en el menú.

## 4. Seleccionar la región

### 4.1 Fijar los puntos

| Comando | Qué hace |
|---|---|
| `/isorender pos1` | Fija el punto 1 en el bloque al que apuntas. |
| `/isorender pos2` | Fija el punto 2 de la misma forma. |
| `/isorender clear` | Borra ambos puntos. Responde «Selección borrada.» o «No había ninguna selección.» |

- **Bloque apuntado:** si tu mira está sobre un bloque (dentro del alcance normal de interacción), se usa ese bloque.
- **Sin apuntar a un bloque:** se usa el bloque sobre el que estás de pie (el que está justo debajo de tus pies) y el chat lo indica.
- Repetir `pos1` o `pos2` reemplaza el punto anterior.
- El orden no importa: la región es la caja que contiene ambos puntos, **inclusive**. Si los dos puntos son el mismo bloque, la región es de 1 × 1 × 1.
- Ejemplo: pos1 en (0, 64, 0) y pos2 en (9, 69, 4) dan una región de 10 × 6 × 5 bloques. El chat lo escribe como `Región: 10 × 6 × 5 bloques.`

### 4.2 Contorno

Mientras haya al menos un punto, el mod dibuja en el mundo un contorno amarillo con la caja de la selección. Se ve desde dentro y desde fuera, y también a través de los bloques (más tenue). Desaparece con `/isorender clear`.

### 4.3 Límites y validaciones

Antes de renderizar (con el botón, con `/isorender render` y también en la vista previa) el mod comprueba:

| Regla | Detalle |
|---|---|
| Hay dos puntos | Si falta alguno, te dice cuál. |
| Tamaño máximo | **64 × 64 × 64** bloques por eje (se puede bajar con `tamanoMaximo`, nunca subir de 64; ver §9). |
| Chunks cargados | Todos los chunks que toca la región deben estar cargados en tu cliente. Acércate si no lo están. |
| No solo aire | Debe haber al menos un bloque que no sea aire. |

Mod de cliente: solo ve lo que tu cliente tiene cargado. En un servidor, la distancia de renderizado limita qué zonas puedes capturar.

## 5. El menú

Se abre con **K** o `/isorender menu` (solo si no hay otra pantalla abierta: no se abre con el inventario o el chat abiertos). No pausa el juego. Se cierra con **Esc** o con el botón **Cerrar**; la tecla K no lo cierra.

Disposición de arriba abajo:

```
            IsoRender
   Región: 10 × 6 × 5 bloques            (verde si la selección está completa; naranja si falta algo)
 [ Rotación: 45° .......... ] [ 45 ]    +----------------------+
 [ Elevación: 35,3° ....... ] [35,3]    |                      |
 [ Zoom: 100 % ............ ] [ 100 ]   |     VISTA PREVIA     |
 [ Desp. X: 0 % ........... ] [  0  ]   |                      |
 [ Desp. Y: 0 % ........... ] [  0  ]   |                      |
 [ Fondo: Tablero ] [ Vista: Isométrica ]|                      |
 [ Resolución: 512 px ...... ] [ 512 ]  +----------------------+
 [Isométrica][Frontal][Superior]              [ Renderizar ]
 Corte Y (vacío = sin corte)   [     ]        [   Cerrar   ]
 Rango válido: 64..69
 [Restablecer]  (mensaje de error en rojo)
```

### 5.1 Cámara: rotación, elevación, zoom y desplazamiento

Cada ajuste tiene un deslizador (arrastra con el ratón; avanza en pasos enteros) y un campo numérico a la derecha (admite decimales).

| Ajuste | Rango | Por defecto | Qué hace |
|---|---|---|---|
| Rotación | 0 a 359° | 45° | Gira la cámara alrededor de la estructura. |
| Elevación | 5 a 90° | 35,264° | Altura de la cámara: 5° casi de frente, 90° desde arriba. |
| Zoom | 25 a 400 % | 100 % | Acerca o aleja. A 100 % la estructura completa cabe con un margen del 5 %. |
| Desp. X | -100 a 100 % | 0 % | Mueve la estructura a derecha (+) o izquierda (-) de la imagen. |
| Desp. Y | -100 a 100 % | 0 % | Mueve la estructura hacia arriba (+) o abajo (-). |

El desplazamiento se mide como porcentaje del semi-ancho de la imagen. La estructura se encuadra automáticamente (centrada y con margen) para cualquier rotación y elevación; el zoom y el desplazamiento se aplican encima.

**Entrada numérica:** haz clic en un campo, escribe el valor (coma o punto decimal) y confirma con **Intro**, con **Esc** (solo cierra el campo, no el menú) o haciendo clic fuera. Un valor fuera de rango se ajusta al límite más cercano. Si lo escrito no es un número, el campo recupera el valor anterior y aparece un mensaje en rojo junto al botón Restablecer (por ejemplo «Rotación no válido: «abc». Escribe un número.»).

### 5.2 Fondo de la vista previa

El botón **Fondo** cicla entre Tablero, Blanco, Gris, Negro y Verde croma. El tablero de ajedrez deja ver qué zonas son transparentes. **Solo cambia la vista previa:** el PNG exportado siempre tiene fondo transparente. El fondo elegido se guarda en la configuración.

### 5.3 Vista Isométrica o Perspectiva

El botón **Vista** alterna entre «Isométrica» (proyección ortográfica, sin deformación por distancia; es la de por defecto) y «Perspectiva» (campo de visión fijo de 30°; las partes cercanas se ven mayores). Afecta a la vista previa y al PNG. En perspectiva el zoom y el desplazamiento mueven la ventana de la imagen, la cámara no se acerca. Si el texto no cabe en el botón se abrevia («Persp.» / «Isom.»).

### 5.4 Resolución

El botón **Resolución** cicla por 512, 1024, 2048 y 4096 px (desde un valor intermedio salta al siguiente mayor; desde 4096 vuelve a 512). El campo de al lado acepta cualquier entero entre **512 y 4096**. Fuera de rango muestra «Resolución fuera de rango: N px. Debe estar entre 512 y 4096 px.» y conserva el valor anterior. Un campo vacío equivale a 512. La imagen siempre es cuadrada (N × N).

### 5.5 Presets

Tres botones ponen rotación y elevación de golpe; no cambian el zoom, el desplazamiento ni la vista:

| Preset | Rotación | Elevación |
|---|---|---|
| Isométrica | 45° | 35,264° |
| Frontal | 0° | 5° (el mínimo permitido) |
| Superior | 0° | 90° |

### 5.6 Corte Y

Escribe una altura **Y absoluta** (la misma que ves en F3) y confirma. Se dibujan solo los bloques con Y menor o igual a ese valor: sirve para ver el interior quitando las capas de arriba. La vista previa y el PNG muestran el corte, y el encuadre se ajusta a lo que queda visible.

- Campo vacío = sin corte. Debajo de la etiqueta aparece «Rango válido: y1..y2» con las alturas de tu región.
- Un valor menor que y1 o igual o mayor que y2 se ignora y **se dibuja todo** (no hay error).
- Un valor que no sea un entero da un error en rojo y se conserva el anterior.
- El nombre del PNG usa la caja recortada (y2 = el corte).
- El corte **no se guarda** en la configuración y **Restablecer no lo borra**. Se mantiene mientras el juego esté abierto, también si cierras y reabres el menú.

### 5.7 Restablecer

Devuelve la cámara a los valores por defecto (rotación 45°, elevación 35,264°, zoom 100 %, desplazamiento 0 y vista Isométrica). **No** cambia el fondo, la resolución ni el corte Y.

### 5.8 Renderizar y Cerrar

- **Renderizar** (se pone gris si la selección no está completa): cierra el menú y genera el PNG con la cámara, la resolución y el corte actuales. Como el menú se cierra, no se puede pulsar dos veces. Si ya hay otro render en curso, el chat dice «Ya hay un render en curso o pendiente; espera a que termine.»
- **Cerrar:** cierra el menú. También lo hace Esc fuera de un campo de texto.

Al cerrar el menú (por cualquier vía) se guarda en `isorender.properties` tu último ajuste de cámara, vista, resolución y fondo, y el menú los recupera la próxima vez, incluso tras reiniciar el juego.

### 5.9 La vista previa

El recuadro de la derecha muestra la estructura en vivo y se actualiza al mover cualquier control. En lugar de la imagen puede mostrar un texto:

| Texto | Significado |
|---|---|
| «Falta definir la selección (usa /isorender pos1 y pos2)» o «Falta el punto N…» | La selección está incompleta. |
| Mensaje de validación | Región demasiado grande, chunks sin cargar o solo aire (ver §10). |
| «Preparando vista previa…» | Está leyendo la región (tarda un instante). |
| «Hay un render en curso; la vista previa espera.» | Hay un render real ejecutándose. |
| Mensaje de error | La vista previa falló (por ejemplo, falta de memoria); el menú sigue abierto. |
| «Ventana demasiado pequeña para la vista previa» | La ventana no deja sitio para el recuadro (el lado mínimo es de 64 px de interfaz). |

**Modo simplificado:** si la región tiene más de 32 × 32 × 32 bloques de volumen (más de 32.768 bloques, tras aplicar el corte Y), la vista previa baja su calidad para mantener fluidez: imagen interna de hasta 256 px (frente a 512), reordena las transparencias como mucho cada 0,3 s y **no dibuja entidades** (cofres, camas, marcos...). Lo indica el texto «Vista simplificada (región grande)» bajo el recuadro. El PNG final siempre se dibuja con calidad completa y con entidades.

La vista previa es una aproximación: el PNG final usa suavizado de bordes por supermuestreo.

## 6. Comandos

Todos empiezan por `/isorender` (en minúsculas) y los atiende tu cliente; no llegan al servidor. No hay autocompletado con Tab. Un subcomando desconocido, o argumentos de más, muestra la línea de uso.

| Comando | Qué hace |
|---|---|
| `/isorender pos1` / `pos2` | Fija el punto 1 / 2 (§4). |
| `/isorender clear` | Borra la selección. |
| `/isorender menu` | Abre el menú (igual que la tecla K). |
| `/isorender render` | Renderiza la región con la cámara, resolución y corte Y del menú. No admite argumentos: para cambiar la resolución usa el menú o la configuración. |
| `/isorender item [id] [meta]` | Renderiza un ítem o bloque suelto (§6.1). |
| `/isorender prueba [lado] [rotación] [elevación] [zoom] [vista]` | Comando de pruebas (§6.2). |
| `/isorender prueba-multi [n]` | Comando de pruebas (§6.2). |
| `/isorender estres [n]` | Comando de pruebas (§6.2). |

### 6.1 `/isorender item [id] [meta]`

No necesita selección ni menú. Dibuja el ítem tal como se ve en el inventario, con fondo transparente y la **resolución** del menú; ignora la rotación, elevación, zoom y vista del menú. No hay corte Y.

| Ejemplo | Resultado |
|---|---|
| `/isorender item` | El ítem de tu mano principal; si está vacía, el de la mano secundaria. Si ambas están vacías: «No tienes ningún ítem en la mano.» |
| `/isorender item minecraft:diamond_sword` | Ese ítem. El prefijo `minecraft:` es opcional (`stone` también vale). |
| `/isorender item minecraft:wool 14` | Lana roja: el segundo argumento es la metadata (entero de 0 a 32767). |
| `/isorender item minecraft:stained_glass 3` | Cristal tintado, translúcido. |

Errores: id inexistente («No existe el ítem o bloque «x».»), bloque sin forma de ítem como el agua («El bloque «water» no tiene forma de ítem y no se puede renderizar.»), metadata no válida o fuera de rango, o más de dos argumentos («Demasiados argumentos. Uso: /isorender item [id] [meta]»). En todos los casos no se crea archivo. El ítem de tu mano no se modifica (se trabaja sobre una copia).

### 6.2 Comandos de prueba

Son herramientas de desarrollo; usan la selección actual y no pasan por el menú. **Escriben en `<directorio del juego>/isorender-test/`, no en la carpeta de salida configurada.**

| Comando | Argumentos | Por defecto | Qué hace |
|---|---|---|---|
| `/isorender prueba` | `[lado 512-4096] [rotación 0-359] [elevación 5-90] [zoom 25-400] [vista iso\|persp]`, en ese orden | 512, 45, 35,264, 100, iso | Un PNG `prueba-<lado>-<milisegundos>.png`. Sin desplazamiento ni corte. |
| `/isorender prueba-multi` | `[n 2-8]` | 4 | `n` PNG de 512 px con la rotación repartida en 360° (`multi-<marca>-<i>of<n>-rot<grados>.png`). El chat informa el tiempo de construcción y el tiempo medio de dibujo. |
| `/isorender estres` | `[n 1-50]` | 30 | `n` renders de 512 px, uno por fotograma, sin guardar archivos. Al final informa tiempo medio, variación de memoria y errores de OpenGL. |

Ejemplos:

```
/isorender prueba                       (512 px, cámara por defecto)
/isorender prueba 2048 90 35 100 persp  (2048 px, rotación 90, elevación 35, zoom 100, perspectiva)
/isorender prueba 1024 45 35 150 iso
/isorender prueba-multi 6
/isorender estres 10
```

La vista es el cuarto argumento de la cámara: `/isorender prueba 1024 persp` no es válido (se interpretaría «persp» como rotación). Los argumentos erróneos dan un mensaje en español y no se crea archivo.

## 7. Dónde se guardan los PNG y cómo se llaman

Carpeta por defecto: **`<directorio del juego>/isorender/`** (con un launcher como Prism, la carpeta `minecraft` de la instancia). Se crea sola si no existe. Se cambia con `carpetaSalida` (ver §9).

| Tipo | Nombre | Ejemplo |
|---|---|---|
| Estructura | `estructura_<aaaammdd-hhmmss>_<x1,y1,z1>_<x2,y2,z2>.png` | `estructura_20261114-152831_-4,4,-4_-2,4,1.png` |
| Ítem | `item_<aaaammdd-hhmmss>_<id>_<meta>.png` (los caracteres raros del id, como `:`, pasan a `-`) | `item_20261114-120000_minecraft-diamond-sword_0.png` |

- Las coordenadas son la esquina mínima y la máxima de la región (con el corte Y aplicado, si lo hay); los negativos llevan «-».
- Nunca se sobrescribe: si el nombre ya existe (dos renders en el mismo segundo) se añade `_2`, `_3`...
- El PNG es RGBA de 32 bits, cuadrado, con alfa 0 fuera de la silueta y alfa parcial en cristal, agua y bordes suavizados.
- Si el archivo no se puede escribir, no queda un PNG a medias.

Al terminar, el chat muestra, por ejemplo: `[IsoRender] Render listo 1024x1024 px (1530 bloques) en /ruta/isorender/estructura_...png [2340 ms]`. La ruta aparece una sola vez, subrayada y clicable (abre la imagen con tu programa por defecto).

## 8. Qué se dibuja y qué no

| Se dibuja | No se dibuja |
|---|---|
| Todos los bloques de la región, con su estado real (orientación, puertas, vallas conectadas, escaleras, losas, mitades). | Mobs y animales. |
| Bloques translúcidos (cristal, cristal tintado, hielo, agua) mezclados correctamente, y tinte por bioma (hierba, hojas, agua). | Jugadores (incluido tú). |
| Entidades de bloque: cofres, camas, carteles, cabezas, estandartes y similares. | Ítems en el suelo, correas y otras entidades. |
| Marcos de objetos (con su ítem y rotación), pinturas y armor stands (con armadura y objetos en las manos). | Bloques fuera de la región: no aparecen aunque estén pegados al borde. |
| | Baliza, bloque de estructura, spawner y pistón en movimiento (entidades de bloque con tratamiento especial). |

Detalles que conviene saber:

- **Iluminación fija:** el render usa luz plena. El PNG sale igual de día que de noche o en una cueva. La oclusión ambiental suave (sombreado en las esquinas) está siempre activa, y las hojas se dibujan siempre con la calidad de gráficos detallada (Fancy) aunque tu juego esté en gráficos rápidos.
- Las caras del borde de la región se dibujan; las caras entre bloques contiguos no.
- Las conexiones de vallas y paneles hacia fuera de la región se cortan (el vecino exterior cuenta como aire).
- Un armor stand o un marco cuenta si su bloque de anclaje (marco, pintura) o su bloque base (armor stand) está dentro de la región.
- Si una entidad falla al dibujarse, se omite y el chat avisa («Aviso: N entidades de bloque no se pudieron dibujar y se omitieron»). Tras un render con entidades, el chat también cuenta cuántas se dibujaron («3 entidades decorativas dibujadas»).
- Los bloques de otros mods son de mejor esfuerzo: pueden faltar o verse mal (§11).

## 9. Configuración: `isorender.properties`

Se crea sola la primera vez que arrancas el juego, en:

**`<directorio del juego>/liteconfig/common/isorender.properties`**

Es un archivo de texto (formato `.properties`, UTF-8). Edítalo con el juego **cerrado**: el mod lo lee al arrancar y lo reescribe completo al cerrar el menú si cambiaste algo, así que los cambios hechos con el juego abierto se pierden. Los nombres de las claves distinguen mayúsculas y minúsculas.

| Clave | Valores | Por defecto | Ejemplo | Qué hace |
|---|---|---|---|---|
| `carpetaSalida` | ruta de carpeta | vacío | `carpetaSalida=renders` | Vacío = `<juego>/isorender`. Una ruta relativa se interpreta desde el directorio del juego; una absoluta se usa tal cual. |
| `resolucion` | entero 512 a 4096 | 512 | `resolucion=2048` | Lado de la imagen en píxeles. |
| `rotacion` | 0 a 359 | 45.0 | `rotacion=90.0` | Rotación de la cámara en grados. |
| `elevacion` | 5 a 90 | 35.264 | `elevacion=30.0` | Elevación en grados. |
| `zoom` | 25 a 400 | 100.0 | `zoom=150.0` | Zoom en %. |
| `desplazamientoX` | -100 a 100 | 0.0 | `desplazamientoX=10.0` | Desplazamiento horizontal en %. |
| `desplazamientoY` | -100 a 100 | 0.0 | `desplazamientoY=-5.0` | Desplazamiento vertical en %. |
| `vista` | `iso` o `persp` | `iso` | `vista=persp` | Isométrica o perspectiva. |
| `fondo` | `TABLERO`, `BLANCO`, `GRIS`, `NEGRO`, `VERDE` | `TABLERO` | `fondo=VERDE` | Fondo de la vista previa (no afecta al PNG). |
| `tamanoMaximo` | entero 1 a 64 | 64 | `tamanoMaximo=32` | Tamaño máximo de la región por eje. No se puede subir de 64. |

Ejemplo de archivo:

```
carpetaSalida=
resolucion=1024
rotacion=45.0
elevacion=35.264
zoom=100.0
desplazamientoX=0.0
desplazamientoY=0.0
vista=iso
fondo=TABLERO
tamanoMaximo=64
```

Reglas:

- Los decimales admiten punto o coma al leer; el mod siempre escribe con punto.
- Una clave ausente o con el valor en blanco usa el valor por defecto, sin aviso.
- **Valor inválido** (texto donde va un número, fuera de rango, `vista` que no sea `iso`/`persp`, `fondo` desconocido): solo esa clave vuelve a su valor por defecto y el resto se conserva. El motivo queda en el registro del juego (`logs/latest.log`, líneas «Configuración: «clave=valor» no es válido…»), no en el chat. Ojo: `resolucion=1024.0` no vale (debe ser un entero).
- **Archivo corrupto** (UTF-8 inválido, secuencia `\u` mal formada): se usan todos los valores por defecto y se anota en el registro.
- Archivo ausente: se crea con los valores por defecto. Si no se puede crear o guardar (por ejemplo, sin permisos), el juego continúa y el motivo queda en el registro.
- Las claves desconocidas se ignoran y desaparecen la próxima vez que el mod guarde.
- `carpetaSalida` y `tamanoMaximo` solo se cambian a mano; el menú guarda el resto (cámara, vista, resolución, fondo).

## 10. Mensajes frecuentes y qué hacer

Todos llevan el prefijo `[IsoRender]`.

| Mensaje | Causa | Qué hacer |
|---|---|---|
| `No hay selección: fija los puntos con /isorender pos1 y /isorender pos2.` | No has fijado ningún punto. | Fija los dos puntos. |
| `Falta el punto 1: fíjalo con /isorender pos1.` (o punto 2) | Selección incompleta. En el menú, Renderizar está en gris. | Fija el punto que falta. |
| `La región de 70 × 10 × 10 supera el máximo permitido de 64 × 64 × 64 bloques.` | Algún eje supera el máximo. | Reduce la región o renderiza por partes. |
| `La región tiene zonas no cargadas; acércate para que los chunks se carguen.` | Parte de la región está fuera de la distancia de renderizado. | Acércate (o aumenta la distancia de renderizado) hasta que se cargue y repite. |
| `La región solo contiene aire: no hay bloques que renderizar.` | No hay ningún bloque. Un armor stand o un marco **sin ningún bloque** en la región también da este mensaje. | Incluye al menos un bloque en la región. |
| `Ya hay un render en curso o pendiente; espera a que termine.` | Pediste otro render antes de que acabe el anterior. | Espera unos segundos. |
| `Memoria insuficiente: se necesitan unos X MB y solo hay Y MB libres. Usa una región más pequeña o menos resolución.` | Poca memoria libre de Java antes de empezar. | Baja la resolución o la región; si es posible, dale más RAM a Java (parámetro `-Xmx` en tu launcher). |
| `Memoria insuficiente para renderizar...; usa una región más pequeña o menos resolución.` | Java se quedó sin memoria durante el render. | Igual que arriba. El juego no se cierra. |
| `Aviso: poca memoria para el supermuestreo a N px; se dibuja sin suavizado de bordes.` | Aviso, no error: el PNG se genera sin suavizado. | Más RAM o menos resolución si quieres bordes suaves. |
| `Aviso: tu GPU no admite supermuestreo a N px; los bordes saldrán sin suavizar.` | Tu GPU no admite el doble de resolución interna (p. ej. 4096 px exige textura de 8192). | Usa 2048 px o menos para bordes suaves. |
| `Tu tarjeta gráfica no soporta N px (máximo M px). Elige una resolución menor.` | La GPU no admite esa resolución. | Baja la resolución. |
| `Error de OpenGL (0x…) al crear el framebuffer…; posible falta de memoria de vídeo.` | Sin VRAM suficiente. | Baja la resolución. |
| `No se pudo guardar el archivo (…). Comprueba permisos y espacio en disco.` | Carpeta sin permisos, disco lleno o ruta imposible de crear. | Cambia `carpetaSalida` a una carpeta con permisos y comprueba el espacio. No queda archivo parcial. |
| `Error inesperado al … (TipoDeError). Detalles en la consola del juego.` | Fallo no previsto. | El juego sigue funcionando. Mira `logs/latest.log` y avisa a quien mantiene el mod. |
| `Uso: /isorender pos1 \| pos2 \| clear \| menu \| render \| prueba … \| item [id] [meta]` | Comando mal escrito o con argumentos de más. | Revisa §6. |
| `Aviso: OpenGL informó un error (…) durante el render; ya se limpió.` | OpenGL registró un error que el mod limpió. | Si se repite o ves fallos visuales, mira el registro. |

## 11. Limitaciones conocidas

- **Solo cliente y solo lo cargado:** se capturan únicamente los chunks que tu cliente tiene cargados.
- **Regiones de hasta 64 × 64 × 64.** Más grande queda fuera de la versión 1.
- **Solo bloques vanilla garantizados.** Los de otros mods son de mejor esfuerzo; si un bloque falla se omite y se avisa («N bloques no se pudieron dibujar y se omitieron»).
- **Entidades:** solo marcos de objetos, pinturas y armor stands, más las entidades de bloque. No hay mobs, jugadores, ítems en el suelo ni correas.
- **Una región con únicamente un armor stand o un marco y ningún bloque se rechaza** como «solo contiene aire», porque la validación mira bloques, no entidades.
- **Imagen siempre cuadrada y fondo siempre transparente;** no hay fondo opaco, recorte a la silueta, animaciones ni GIF.
- **Sin autocompletado** de `/isorender` con Tab, y el comando va en minúsculas (`/ISORENDER` lo trata el servidor como comando desconocido).
- **Luz fija:** no refleja la hora del día ni la luz del mundo. Las conexiones de vallas y paneles con bloques fuera de la región se cortan.
- **Supermuestreo:** el suavizado de bordes usa un render interno del doble de lado; a 4096 px requiere una textura de 8192 px y unos 0,5 GB de VRAM. Si no cabe, se avisa y se dibuja sin suavizar.
- **Memoria de vídeo:** solo se comprueba antes del render con GPU NVIDIA (extensión NVX_gpu_memory_info). En otras GPU no se limita por VRAM y un fallo se detecta al crear el framebuffer.
- **Vista previa simplificada** en regiones de más de 32.768 bloques de volumen (sin entidades, menos calidad).
- **El corte Y no se guarda** y solo admite Y máxima (no hay Y mínima).
- **Una sola operación a la vez:** los renders, las pruebas y el estrés comparten un cerrojo; el segundo se rechaza con aviso.
- **Mientras se renderiza el juego se detiene un momento:** el render se hace en el hilo de dibujo (una región de 32³ a 2048 px debería tardar unos segundos).
- **Compatibilidad:** probado en cliente vanilla 1.12.2 con LiteLoader limpio. No se garantiza con otros mods ni con Forge.

## 12. Solución de problemas

| Síntoma | Qué probar |
|---|---|
| El mod no aparece / no sale el saludo | Comprueba que usas el perfil con LiteLoader 1.12.2 (no el vanilla), que el `.litemod` está en `mods/` y que la versión de Minecraft es 1.12.2. Mira `logs/latest.log` por «IsoRender». |
| `/isorender ...` se envía como mensaje o da «comando desconocido» | Escríbelo en minúsculas y con la barra inicial. Si no hay saludo, el mod no está cargado. |
| La tecla K no abre el menú | Puede estar ocupada por otra función: busca «Abrir menú IsoRender» (categoría IsoRender) en Opciones > Controles y reasígnala. No abre el menú si ya hay otra pantalla abierta (inventario, chat). Alternativa: `/isorender menu`. |
| Renderizar está en gris | La selección no está completa; el estado aparece arriba del menú. |
| La vista previa dice «zonas no cargadas» o «solo aire» | Ver §10. |
| La imagen sale con el fondo negro o blanco | Tu visor de imágenes no muestra la transparencia; abre el PNG en un editor o sobre otro fondo. El PNG tiene alfa 0 fuera de la silueta. |
| Faltan bloques o entidades | Mira el chat: avisa de lo omitido. Los bloques de mods son de mejor esfuerzo. La vista previa simplificada no dibuja entidades, pero el PNG sí. |
| No encuentro el PNG | La ruta sale en el chat (clicable). Por defecto: `<directorio del juego>/isorender/`. Los comandos de prueba escriben en `isorender-test/`. |
| El menú no recuerda mis ajustes | Se guardan al cerrar el menú. Si `isorender.properties` no se puede escribir, el juego sigue y el motivo queda en `logs/latest.log`. |
| La configuración se ignora | Mira `logs/latest.log` (líneas «Configuración:»). Cada valor inválido se sustituye por el por defecto; edita con el juego cerrado. |
| Pantalla negra o rara tras un render | No debería ocurrir: el mod restaura el estado gráfico. Si pasa, anota los pasos, `logs/latest.log` y avisa. |
| El juego va lento al renderizar 4096 px | Es normal un instante de pausa; usa 2048 o menos si tu equipo es modesto. |
