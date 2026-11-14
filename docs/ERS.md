# ERS — Mod de renders isométricos de estructuras (Minecraft 1.12.2, LiteLoader)

Versión v0.4 (adaptada para ejecución con Claude Code). Basado en la ERS v0.3. Los marcadores SUPUESTO e IMPLÍCITO indican contenido no pedido explícitamente por el solicitante.

# 1. Información general

| **Campo** | **Valor** |
|---|---|
| Proyecto | Mod de renders isométricos de estructuras para Minecraft 1.12.2 (LiteLoader) |
| Cliente / solicitante | @juancho (solicitante) |
| Fecha | 13 de noviembre de 2026 |
| Versión | v0.3 (borrador) |
| Autor | Analista de software (Claude) |
| Fuente analizada | Brief_para_analista_mod_de_renders_isométricos_1.12.2 (LiteLoader).md y mensajes del solicitante del 13/11/2026 (reenfoque a estructuras con menú interactivo; LiteLoader obligatorio) |

**Convención de marcas:** el texto sombreado en amarillo e itálica, como *\[SUPUESTO\]* o *\[IMPLÍCITO\]*, señala contenido que **no** viene de lo que dijo el solicitante y debe ser validado.

**Qué cambió en la v0.3:** el solicitante confirmó que **LiteLoader es obligatorio**; Forge queda descartado como alternativa y se cierra la pregunta P-02.

**Qué cambió respecto de la v0.1:** el solicitante aclaró que el mod es para renderizar **estructuras**: se selecciona el punto 1 y el punto 2 y el mod crea el render de esa región, con un **menú interactivo** para ajustar cámara, ángulo, etc. El brief original hablaba de ítems y bloques individuales; eso pasa a ser secundario (RF-030).

# 2. Resumen ejecutivo

**Problema:** no existe una versión de isometric-renders para Minecraft 1.12.2 (el original solo cubre 1.18 y 1.19 y no se puede portar), y se necesita renderizar construcciones completas, no solo bloques sueltos.

**Objetivo:** un mod nuevo que permita seleccionar dos puntos del mundo y generar un PNG isométrico, con fondo transparente, de la estructura que contiene esa región, ajustando la cámara desde un menú con vista previa.

**Usuarios:** jugadores y creadores de contenido. **Alcance:** selección de región, render de estructura, menú interactivo, exportación a PNG y configuración. Quedan fuera WebSocket, atlas, entidades y otros loaders.

**Resultado esperado:** un único .litemod para LiteLoader 1.12.2. Se identificaron 31 requerimientos funcionales, 14 no funcionales y 10 reglas de negocio.

# 3. Alcance

## 3.1 Dentro del alcance (versión 1)

- Selección de una región con punto 1 y punto 2, con validación y contorno visible.

- Render isométrico de todos los bloques de la región, con su estado real, transparencias y entidades de bloque.

- Menú interactivo con vista previa para ajustar rotación, elevación, zoom, desplazamiento y resolución.

- Exportación a PNG con fondo transparente y resolución configurable, y resultado informado en el chat.

- Archivo de configuración y empaquetado como .litemod para LiteLoader 1.12.2.

## 3.2 Fuera del alcance

- Exportación por WebSocket, render externo y atlas de ítems (existen en el mod original).

- Entidades, mobs y jugadores dentro de la región.

- Otras versiones de Minecraft y otros loaders.

- Modificar el mundo (el mod es de solo lectura).

- *\[SUPUESTO\]* Regiones mayores de 64 × 64 × 64 bloques y zonas con chunks no cargados.

- *\[SUPUESTO\]* Bloques de otros mods con garantía de funcionamiento (solo vanilla).

- *\[SUPUESTO\]* Fondo opaco, imágenes no cuadradas, animaciones y GIF.

- *\[SUPUESTO\]* Render en lote (RF-031).

## 3.3 Opcionales y fase 2

- Render de ítem en la mano o de ítem y bloque por ID (RF-030), del brief original; se estima aparte.

- Corte por altura de la estructura (RF-015).

# 4. Stakeholders y roles

| **Rol** | **Descripción** | **Necesidades principales** |
|---|---|---|
| Solicitante (@juancho) | Quien pide el mod y valida la entrega. | Definir alcance y licencia; responder preguntas abiertas; aprobar la UAT. |
| Usuario final | Jugador o creador de contenido que genera las imágenes de estructuras. | Seleccionar una región, ajustar la cámara con ayuda visual y exportar sin que el juego falle. |
| Desarrollador / mantenedor | Una persona que construye y mantiene el mod. | Entorno reproducible, requisitos claros y código mantenible. |
| Analista | Redacta la ERS y el plan; no desarrolla. | Respuestas del solicitante para cerrar preguntas bloqueantes. |
| Autor del mod original (tercero) | gliscowo, autor de isometric-renders. | Que se respete su licencia si se usa algo suyo. |

# 5. Glosario

| **Término** | **Definición** |
|---|---|
| ERS | Especificación de Requerimientos de Software (este documento). |
| Estructura | Conjunto de bloques construido en el mundo (casa, torre, granja) contenido en una región. |
| Punto 1 / punto 2 | Dos bloques que el usuario marca; son esquinas opuestas de la región. |
| Región | Caja alineada a los ejes que contiene los puntos 1 y 2, inclusive. |
| Encuadre automático | Ajuste de la cámara para que toda la estructura quepa en la imagen con un margen. |
| Vista previa | Imagen en vivo dentro del menú que muestra cómo quedará el render. |
| Menú interactivo | Pantalla propia del mod con controles (deslizadores, botones, campos) para configurar el render. |
| Chunk | Sección del mundo de 16 × 16 bloques; el cliente solo conoce los chunks cargados. |
| LiteLoader | Cargador de mods ligero para Minecraft; admite mods .litemod sin necesitar Forge. |
| .litemod / litemod.json | Archivo de distribución de un mod de LiteLoader y su descriptor de metadatos. |
| Forge | Otro cargador de mods; descartado como alternativa porque el solicitante exige LiteLoader. |
| MCP | Mod Coder Pack: mappings que dan nombres legibles al código de Minecraft 1.12.2. |
| LWJGL 2 / OpenGL de función fija | Biblioteca y modo de dibujo de Minecraft 1.12.2, sin shaders modernos. |
| Framebuffer | Superficie de dibujo fuera de pantalla donde se renderiza la imagen a alta resolución. |
| Proyección ortográfica / vista isométrica | Cámara sin perspectiva; en la vista isométrica se ven tres caras de un cubo. *\[SUPUESTO\]* Por defecto: elevación 35,264° y rotación 45°. |
| Entidad de bloque | Bloque con lógica y render propios (cofre, cama, cartel). |
| Halo | Contorno de color indeseado en el borde de una imagen con transparencia. |
| MoSCoW | Priorización: Must, Should, Could, Won't. |
| UAT | Pruebas de aceptación del usuario. |

# 6. Contexto y supuestos

## 6.1 Supuestos

Del brief (declarados por el solicitante):

- El desarrollo lo ejecuta Claude Code (agente de IA) bajo supervisión del solicitante, quien prueba el mod en el juego.

- Se usa Java 8 y una instalación funcional de LiteLoader 1.12.2.

- No se reutiliza código del original.

Añadidos por el analista:

- LiteLoader es obligatorio (confirmado por el solicitante el 13/11/2026); Forge queda descartado.

- *\[SUPUESTO\]* El mod es solo de cliente y funciona con los chunks cargados, en un jugador o en servidor.

- *\[SUPUESTO\]* Los puntos se fijan con comandos /isorender pos1 y pos2 sobre el bloque que el jugador apunta (P-08).

- *\[SUPUESTO\]* Tamaño máximo de región 64 × 64 × 64; rango de resolución 512–4096 px; imagen cuadrada.

- *\[SUPUESTO\]* Rangos y valores por defecto de cámara en RN-007.

- *\[SUPUESTO\]* Comando /isorender, tecla K y textos en español.

- *\[SUPUESTO\]* RF-027 sube a Must; el render de ítem individual (RF-030) baja a Could.

- *\[SUPUESTO\]* Ejecutor: Claude Code, con el solicitante como supervisor y probador en juego. Las horas del plan son equivalentes humanos de referencia, no tiempo de Claude Code (P-06).

## 6.2 Restricciones

| **Tipo** | **Restricción** |
|---|---|
| Técnica | Solo Minecraft 1.12.2 con LiteLoader; OpenGL de función fija (LWJGL 2); mappings MCP; Java 8; toolchain esperada Gradle + ForgeGradle. |
| Entrega | Un solo .litemod, con versión y licencia declaradas. |
| Legal | Respetar la licencia del mod original si se usa algo suyo. |
| Tiempo / presupuesto | No indicados (P-06). |
| Marca | No aplica. |

## 6.3 Dependencias externas

- LiteLoader 1.12.2 funcional y mappings MCP.

- JDK 8, Gradle y un cliente de pruebas de Minecraft 1.12.2.

- Mundos de prueba con estructuras de referencia (pequeña, mediana, grande, con cristal y cofres).

- Respuestas del solicitante a las preguntas abiertas (sección 13).

# 7. Requerimientos funcionales

Prioridades: 19 Must, 9 Should, 2 Could y 1 Won't. Resumen:

| **ID** | **Título** | **Prioridad** | **Épica** |
|---|---|---|---|
| RF-001 | Carga del mod en LiteLoader | Must | EP-1 |
| RF-002 | Distribución en un único archivo .litemod | Must | EP-1 |
| RF-003 | Definir el punto 1 | Must | EP-2 |
| RF-004 | Definir el punto 2 | Must | EP-2 |
| RF-005 | Región entre los dos puntos | Must | EP-2 |
| RF-006 | Contorno de la selección en el mundo | Should | EP-2 |
| RF-007 | Validación de la región antes del render | Must | EP-2 |
| RF-008 | Limpiar la selección | Should | EP-2 |
| RF-009 | Render isométrico de la región | Must | EP-3 |
| RF-010 | Encuadre automático | Must | EP-3 |
| RF-011 | Fondo transparente | Must | EP-3 |
| RF-012 | Bloques transparentes y tintados | Should | EP-3 |
| RF-013 | Bloques con entidad de bloque | Should | EP-3 |
| RF-014 | Estado real de cada bloque | Must | EP-3 |
| RF-015 | Corte por altura | Could | EP-3 |
| RF-016 | Abrir el menú | Must | EP-4 |
| RF-017 | Ajuste de rotación y elevación | Must | EP-4 |
| RF-018 | Ajuste de zoom y desplazamiento | Must | EP-4 |
| RF-019 | Vista previa en vivo | Must | EP-4 |
| RF-020 | Selector de resolución en el menú | Must | EP-4 |
| RF-021 | Botón Renderizar | Must | EP-4 |
| RF-022 | Restablecer y presets de cámara | Should | EP-4 |
| RF-023 | Entrada numérica de valores exactos | Should | EP-4 |
| RF-024 | Exportación a PNG en carpeta definida | Must | EP-5 |
| RF-025 | Resolución de salida configurable | Must | EP-5 |
| RF-026 | Nombre de archivo único | Should | EP-5 |
| RF-027 | Resultado del render en el chat | Must | EP-5 |
| RF-028 | Lectura del archivo de configuración | Should | EP-6 |
| RF-029 | Guardado de configuración y último ajuste | Should | EP-6 |
| RF-030 | Render de ítem o bloque individual | Could | EP-7 |
| RF-031 | Render en lote | Won't | EP-7 |
| RF-032 | Fondo de la vista previa | Could | EP-4 |
| RF-033 | Vista isométrica o en perspectiva | Should | EP-4 |

## 7.1 EP-1 Carga y distribución

### RF-001: Carga del mod en LiteLoader

**Descripción:** El sistema deberá cargarse como mod de LiteLoader sobre Minecraft 1.12.2 al iniciar el cliente.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «El mod se carga en LiteLoader sobre Minecraft 1.12.2»

**Precondiciones:** Instalación funcional de LiteLoader 1.12.2.

**Criterios de aceptación:**

- **CA-001.1 (caso feliz):** **Dado** un cliente 1.12.2 con LiteLoader y el .litemod en la carpeta de mods, **cuando** se inicia el juego, **entonces** el mod figura como cargado y el juego llega al menú principal sin errores atribuibles al mod.

- **CA-001.2 (validación/error):** **Dado** un cliente de una versión de Minecraft distinta de 1.12.2, **cuando** se inicia con el .litemod instalado, **entonces** el juego no se cierra por causa del mod y el log indica la incompatibilidad.

- **CA-001.3 (caso límite):** **Dado** otros mods de LiteLoader instalados, **cuando** se inicia el juego, **entonces** el mod carga y no depende de los demás.

**Notas / dudas:** LiteLoader confirmado como obligatorio por el solicitante (13/11/2026); Forge descartado.

### RF-002: Distribución en un único archivo .litemod

**Descripción:** El sistema deberá entregarse como un único archivo .litemod autocontenido.

**Rol(es):** Usuario final, Desarrollador \| **Prioridad:** Must

**Origen:** «un solo archivo .litemod»

**Precondiciones:** Build de release disponible.

**Criterios de aceptación:**

- **CA-002.1 (caso feliz):** **Dado** el proceso de empaquetado ejecutado, **cuando** se genera la entrega, **entonces** existe un solo .litemod que, copiado a la carpeta de mods, habilita todas las funciones.

- **CA-002.2 (validación/error):** **Dado** un paquete sin descriptor de metadatos (litemod.json), **cuando** se valida la entrega, **entonces** la validación falla y el paquete no se marca como release.

**Notas / dudas:** Versión y licencia: ver RNF-007.

## 7.2 EP-2 Selección de región

### RF-003: Definir el punto 1

**Descripción:** El sistema deberá permitir fijar el punto 1 de la selección en un bloque del mundo mediante comando.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «Selecciono punto 1»

**Precondiciones:** Jugador dentro de un mundo. *\[SUPUESTO\]* El bloque es el que el jugador apunta; si no apunta a ninguno, el bloque donde está de pie.

**Criterios de aceptación:**

- **CA-003.1 (caso feliz):** **Dado** un jugador apuntando a un bloque, **cuando** ejecuta /isorender pos1, **entonces** el punto 1 queda fijado en las coordenadas de ese bloque y el chat las muestra.

- **CA-003.2 (validación/error):** **Dado** un jugador que no apunta a ningún bloque, **cuando** ejecuta /isorender pos1, **entonces** el punto 1 se fija en el bloque donde está de pie y el chat lo informa.

- **CA-003.3 (caso límite):** **Dado** un punto 1 ya definido, **cuando** ejecuta de nuevo el comando sobre otro bloque, **entonces** el nuevo punto reemplaza al anterior.

**Notas / dudas:** Pregunta P-08 (mecanismo de selección). *\[SUPUESTO\]* También se ofrece una tecla (RF-016).

### RF-004: Definir el punto 2

**Descripción:** El sistema deberá permitir fijar el punto 2 de la selección en un bloque del mundo mediante comando.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «punto 2»

**Precondiciones:** Igual que RF-003.

**Criterios de aceptación:**

- **CA-004.1 (caso feliz):** **Dado** un jugador apuntando a un bloque, **cuando** ejecuta /isorender pos2, **entonces** el punto 2 queda fijado en las coordenadas de ese bloque y el chat las muestra.

- **CA-004.2 (caso límite):** **Dado** el punto 2 igual al punto 1, **cuando** se fija, **entonces** se acepta y la región es de 1 × 1 × 1 bloques.

**Notas / dudas:** Atómico respecto a RF-003.

### RF-005: Región entre los dos puntos

**Descripción:** El sistema deberá definir la región como la caja alineada a los ejes que contiene ambos puntos (inclusive), sin importar el orden, e informar sus dimensiones.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «Selecciono punto 1 y punto 2»

**Precondiciones:** Puntos 1 y 2 definidos.

**Criterios de aceptación:**

- **CA-005.1 (caso feliz):** **Dado** punto 1 en (0, 64, 0) y punto 2 en (9, 69, 4), **cuando** ambos están definidos, **entonces** la región es de 10 × 6 × 5 bloques y el chat informa las dimensiones.

- **CA-005.2 (caso límite):** **Dado** el punto 2 con coordenadas menores que el punto 1, **cuando** ambos están definidos, **entonces** la región resultante es la misma que con el orden inverso.

### RF-006: Contorno de la selección en el mundo

**Descripción:** El sistema deberá dibujar en el mundo el contorno de la región seleccionada.

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Precondiciones:** Al menos un punto definido.

**Criterios de aceptación:**

- **CA-006.1 (caso feliz):** **Dado** ambos puntos definidos, **cuando** el jugador mira la zona, **entonces** se ve el contorno de la caja de selección.

- **CA-006.2 (caso límite):** **Dado** la selección limpiada, **cuando** el jugador mira la zona, **entonces** el contorno ya no se muestra.

**Notas / dudas:** *\[IMPLÍCITO\]* Sin retroalimentación visual es difícil saber qué se va a renderizar.

### RF-007: Validación de la región antes del render

**Descripción:** El sistema deberá validar, antes de renderizar, que ambos puntos existan, que la región no supere el tamaño máximo y que sus chunks estén cargados.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Precondiciones:** Ninguna.

**Criterios de aceptación:**

- **CA-007.1 (caso feliz):** **Dado** una región válida de 20 × 20 × 20 con chunks cargados, **cuando** se solicita el render, **entonces** la validación pasa y comienza el render.

- **CA-007.2 (validación/error):** **Dado** solo el punto 1 definido, **cuando** se solicita el render, **entonces** el chat informa que falta el punto 2 y no se genera archivo.

- **CA-007.3 (validación/error):** **Dado** una región mayor que el máximo (64 × 64 × 64), **cuando** se solicita el render, **entonces** el chat informa el máximo permitido y no se renderiza.

- **CA-007.4 (caso límite):** **Dado** una región con chunks fuera del radio de carga, **cuando** se solicita el render, **entonces** el chat informa que hay zonas no cargadas y no se genera archivo.

**Notas / dudas:** *\[IMPLÍCITO\]* Un mod de cliente solo ve los chunks cargados (RN-004). Pregunta P-09.

### RF-008: Limpiar la selección

**Descripción:** El sistema deberá permitir borrar ambos puntos de la selección.

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Precondiciones:** Ninguna.

**Criterios de aceptación:**

- **CA-008.1 (caso feliz):** **Dado** una selección definida, **cuando** ejecuta /isorender clear, **entonces** ambos puntos se borran y el chat lo confirma.

- **CA-008.2 (caso límite):** **Dado** ninguna selección definida, **cuando** ejecuta /isorender clear, **entonces** el chat informa que no había selección, sin error.

## 7.3 EP-3 Render de la estructura

### RF-009: Render isométrico de la región

**Descripción:** El sistema deberá generar una imagen isométrica de todos los bloques contenidos en la región seleccionada.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «crea el render de eso»

**Precondiciones:** Región válida (RF-007).

**Criterios de aceptación:**

- **CA-009.1 (caso feliz):** **Dado** una región de 10 × 6 × 5 con una construcción, **cuando** se solicita el render, **entonces** el PNG muestra todos los bloques de la región vistos desde el ángulo configurado.

- **CA-009.2 (validación/error):** **Dado** una región que contiene solo aire, **cuando** se solicita el render, **entonces** el chat informa que no hay bloques y no se genera archivo.

- **CA-009.3 (caso límite):** **Dado** bloques justo fuera de la región, junto al borde, **cuando** se solicita el render, **entonces** esos bloques no aparecen en la imagen y los del borde interior sí.

**Notas / dudas:** Las caras entre bloques adyacentes no se ven; las del borde de la región sí *\[SUPUESTO\]*.

### RF-010: Encuadre automático

**Descripción:** El sistema deberá encuadrar la estructura completa, centrada y con margen, para cualquier rotación, elevación y zoom base.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Precondiciones:** Región válida.

**Criterios de aceptación:**

- **CA-010.1 (caso feliz):** **Dado** cualquier región válida y cualquier rotación y elevación permitidas, **cuando** se renderiza, **entonces** la estructura completa queda dentro de la imagen con un margen de *\[SUPUESTO\]* 5 %, sin recortes.

- **CA-010.2 (caso límite):** **Dado** una región muy alargada de 64 × 1 × 1, **cuando** se renderiza, **entonces** la estructura cabe completa en la imagen.

**Notas / dudas:** *\[IMPLÍCITO\]* Sin esto el usuario tendría que ajustar el zoom a mano en cada render.

### RF-011: Fondo transparente

**Descripción:** El sistema deberá producir imágenes cuyo fondo (fuera de la silueta de la estructura) tenga alfa igual a 0.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «con fondo transparente» (brief)

**Precondiciones:** Captura exitosa.

**Criterios de aceptación:**

- **CA-011.1 (caso feliz):** **Dado** una imagen generada, **cuando** se inspeccionan los píxeles fuera de la silueta, **entonces** todos tienen alfa = 0.

- **CA-011.2 (caso límite):** **Dado** bloques translúcidos (cristal), **cuando** se inspecciona la imagen, **entonces** esas zonas tienen alfa intermedio, no opaco ni transparente por completo.

**Notas / dudas:** Halos: RNF-004.

### RF-012: Bloques transparentes y tintados

**Descripción:** El sistema deberá renderizar correctamente bloques con transparencia o color por bioma (cristal, hojas, agua, hierba).

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Precondiciones:** Región válida.

**Criterios de aceptación:**

- **CA-012.1 (caso feliz):** **Dado** una región con cristal y hojas, **cuando** se renderiza, **entonces** los bloques se ven con su transparencia y los de atrás se distinguen.

- **CA-012.2 (caso límite):** **Dado** hierba y hojas en un bioma con tinte, **cuando** se renderiza, **entonces** el color corresponde al bioma del mundo.

**Notas / dudas:** Riesgo R-04 (orden de dibujo).

### RF-013: Bloques con entidad de bloque

**Descripción:** El sistema deberá renderizar los bloques con entidad de bloque de la región (cofres, camas, carteles).

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Precondiciones:** Región válida.

**Criterios de aceptación:**

- **CA-013.1 (caso feliz):** **Dado** una región con un cofre y una cama, **cuando** se renderiza, **entonces** ambos aparecen con su forma y textura.

- **CA-013.2 (validación/error):** **Dado** una entidad de bloque que no puede renderizarse, **cuando** se renderiza, **entonces** esa entidad se omite, el chat avisa y el resto del render se completa.

**Notas / dudas:** Conflicto CF-02: el brief dejaba estos bloques fuera de la v1, pero las estructuras suelen incluirlos.

### RF-014: Estado real de cada bloque

**Descripción:** El sistema deberá dibujar cada bloque con su estado real del mundo (orientación, mitades, puertas abiertas, conexiones).

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Precondiciones:** Región válida.

**Criterios de aceptación:**

- **CA-014.1 (caso feliz):** **Dado** una escalera orientada al este y una valla conectada a otra, **cuando** se renderiza, **entonces** ambas se ven con esa orientación y conexión.

- **CA-014.2 (caso límite):** **Dado** una puerta de dos bloques, **cuando** se renderiza, **entonces** ambas mitades se ven con el estado correcto.

**Notas / dudas:** *\[IMPLÍCITO\]* Diferencia entre renderizar «un bloque por ID» y renderizar la estructura real.

### RF-015: Corte por altura

**Descripción:** El sistema deberá permitir ocultar las capas superiores a una altura indicada para ver el interior.

**Rol(es):** Usuario final \| **Prioridad:** Could

**Origen:** *\[SUPUESTO\]* No indicado por el solicitante

**Precondiciones:** Región válida.

**Criterios de aceptación:**

- **CA-015.1 (caso feliz):** **Dado** una altura de corte dentro de la región, **cuando** se renderiza, **entonces** solo se dibujan los bloques hasta esa altura.

- **CA-015.2 (caso límite):** **Dado** una altura de corte fuera de la región, **cuando** se renderiza, **entonces** se ignora el corte y se dibuja todo.

**Notas / dudas:** *\[SUPUESTO\]* Idea del analista; no pedida. Se estima como opcional.

## 7.4 EP-4 Menú interactivo

### RF-016: Abrir el menú

**Descripción:** El sistema deberá abrir el menú interactivo mediante una tecla configurable y mediante comando.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «menú interactivo para cuadrar el render»

**Precondiciones:** Jugador dentro de un mundo.

**Criterios de aceptación:**

- **CA-016.1 (caso feliz):** **Dado** el jugador dentro de un mundo, **cuando** presiona la tecla (por defecto K *\[SUPUESTO\]*) o ejecuta /isorender menu, **entonces** se abre el menú.

- **CA-016.2 (validación/error):** **Dado** otra pantalla ya abierta, **cuando** presiona la tecla, **entonces** no se abre un segundo menú.

- **CA-016.3 (caso límite):** **Dado** sin región seleccionada, **cuando** abre el menú, **entonces** se abre y muestra que falta definir la selección, con Renderizar deshabilitado.

### RF-017: Ajuste de rotación y elevación

**Descripción:** El menú deberá permitir ajustar la rotación horizontal y la elevación de la cámara.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «cuadrar el render camara, ángulo»

**Precondiciones:** Menú abierto.

**Criterios de aceptación:**

- **CA-017.1 (caso feliz):** **Dado** el menú abierto, **cuando** el usuario mueve el control de rotación o de elevación, **entonces** el valor cambia y la vista previa lo refleja.

- **CA-017.2 (caso límite):** **Dado** los extremos de cada control, **cuando** el usuario los alcanza, **entonces** el valor se limita al rango permitido (rotación 0°–359°, elevación 5°–90° *\[SUPUESTO\]*).

### RF-018: Ajuste de zoom y desplazamiento

**Descripción:** El menú deberá permitir ajustar el zoom y el desplazamiento (encuadre) de la cámara.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «cuadrar el render camara»

**Precondiciones:** Menú abierto.

**Criterios de aceptación:**

- **CA-018.1 (caso feliz):** **Dado** el menú abierto, **cuando** el usuario cambia el zoom o el desplazamiento, **entonces** la vista previa se acerca, aleja o desplaza.

- **CA-018.2 (caso límite):** **Dado** un zoom y desplazamiento extremos, **cuando** el usuario los aplica, **entonces** el valor se limita al rango *\[SUPUESTO\]* 25 %–400 % y ±100 %.

**Notas / dudas:** Parte de la estructura puede salir del encuadre si el usuario así lo decide.

### RF-019: Vista previa en vivo

**Descripción:** El menú deberá mostrar una vista previa de la estructura que se actualice al cambiar cualquier ajuste de cámara.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «menú interactivo para cuadrar el render»

**Precondiciones:** Región válida.

**Criterios de aceptación:**

- **CA-019.1 (caso feliz):** **Dado** la región válida y el menú abierto, **cuando** el usuario cambia un ajuste, **entonces** la vista previa se actualiza sin cerrar el menú.

- **CA-019.2 (validación/error):** **Dado** una región inválida, **cuando** se abre el menú, **entonces** la vista previa muestra el motivo en lugar de la imagen.

- **CA-019.3 (caso límite):** **Dado** una región grande (64 × 64 × 64), **cuando** el usuario cambia un ajuste, **entonces** la vista previa se actualiza en modo simplificado y sin congelar el juego.

**Notas / dudas:** Ver RNF-003. Pregunta P-12 (tiempo real o botón «actualizar»).

### RF-020: Selector de resolución en el menú

**Descripción:** El menú deberá permitir elegir la resolución de salida dentro del rango permitido.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «resolución configurable» (brief)

**Precondiciones:** Menú abierto.

**Criterios de aceptación:**

- **CA-020.1 (caso feliz):** **Dado** el menú abierto, **cuando** el usuario elige 2048, **entonces** la siguiente exportación mide 2048 × 2048 px.

- **CA-020.2 (validación/error):** **Dado** una resolución escrita fuera del rango 512–4096, **cuando** el usuario confirma, **entonces** el menú rechaza el valor e indica el rango.

### RF-021: Botón Renderizar

**Descripción:** El menú deberá tener un botón que exporte el render con los ajustes actuales.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «crea el render de eso»

**Precondiciones:** Región válida.

**Criterios de aceptación:**

- **CA-021.1 (caso feliz):** **Dado** una región válida y ajustes definidos, **cuando** el usuario pulsa Renderizar, **entonces** se genera el PNG con esos ajustes y el chat informa la ruta.

- **CA-021.2 (validación/error):** **Dado** una región inválida, **cuando** el usuario intenta renderizar, **entonces** el botón está deshabilitado o el chat informa el motivo.

- **CA-021.3 (caso límite):** **Dado** un doble clic rápido, **cuando** el usuario pulsa dos veces, **entonces** se genera un solo render.

### RF-022: Restablecer y presets de cámara

**Descripción:** El menú deberá permitir volver a los valores por defecto y elegir presets de cámara (isométrica clásica, frontal, superior).

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** *\[SUPUESTO\]* No indicado por el solicitante

**Precondiciones:** Menú abierto.

**Criterios de aceptación:**

- **CA-022.1 (caso feliz):** **Dado** ajustes modificados, **cuando** el usuario pulsa Restablecer, **entonces** todos los controles vuelven a rotación 45°, elevación 35,264° y zoom 100 %.

- **CA-022.2 (caso feliz):** **Dado** el menú abierto, **cuando** el usuario elige el preset «superior», **entonces** la elevación pasa a 90° y la vista previa se actualiza.

**Notas / dudas:** *\[SUPUESTO\]* Presets no pedidos; idea del analista.

### RF-023: Entrada numérica de valores exactos

**Descripción:** El menú deberá permitir escribir valores numéricos exactos de rotación, elevación y zoom.

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** «cuadrar el render»

**Precondiciones:** Menú abierto.

**Criterios de aceptación:**

- **CA-023.1 (caso feliz):** **Dado** un campo de rotación, **cuando** el usuario escribe 30 y confirma, **entonces** el control marca 30° y la vista previa lo refleja.

- **CA-023.2 (validación/error):** **Dado** un texto no numérico, **cuando** el usuario confirma, **entonces** se conserva el valor anterior y se indica el error.

## 7.5 EP-5 Exportación

### RF-024: Exportación a PNG en carpeta definida

**Descripción:** El sistema deberá guardar el render como PNG en la carpeta de salida definida.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «Exportar la imagen a PNG» (brief)

**Precondiciones:** Carpeta de salida por defecto o configurada.

**Criterios de aceptación:**

- **CA-024.1 (caso feliz):** **Dado** una carpeta con permisos de escritura, **cuando** finaliza un render, **entonces** existe un .png válido en esa carpeta.

- **CA-024.2 (validación/error):** **Dado** una carpeta inexistente o sin permisos, **cuando** finaliza un render, **entonces** se intenta crearla; si no se puede, el chat informa el error y el juego continúa.

- **CA-024.3 (caso límite):** **Dado** coordenadas negativas en el nombre, **cuando** se genera el nombre, **entonces** es un nombre de archivo válido.

### RF-025: Resolución de salida configurable

**Descripción:** El sistema deberá exportar con la resolución elegida (lado de la imagen cuadrada) entre 512 y 4096 px.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «por ejemplo 512 a 4096 px» (brief)

**Precondiciones:** Ninguna.

**Criterios de aceptación:**

- **CA-025.1 (caso feliz):** **Dado** la resolución 1024, **cuando** se renderiza, **entonces** el PNG mide 1024 × 1024 px.

- **CA-025.2 (validación/error):** **Dado** una resolución fuera del rango, **cuando** se renderiza, **entonces** no se genera archivo y el chat informa el rango.

- **CA-025.3 (caso límite):** **Dado** una resolución no soportada por la GPU, **cuando** se renderiza, **entonces** el chat informa y el juego continúa.

**Notas / dudas:** *\[SUPUESTO\]* Rango del ejemplo del brief; imagen cuadrada (RN-006). Pregunta P-04.

### RF-026: Nombre de archivo único

**Descripción:** El sistema deberá nombrar cada archivo con la fecha y hora y las coordenadas de la región, sin sobrescribir exportaciones anteriores.

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Precondiciones:** Ninguna.

**Criterios de aceptación:**

- **CA-026.1 (caso feliz):** **Dado** dos renders de la misma región, **cuando** finalizan, **entonces** existen dos archivos distintos.

- **CA-026.2 (caso límite):** **Dado** dos renders en el mismo segundo, **cuando** finalizan, **entonces** ningún archivo se sobrescribe.

### RF-027: Resultado del render en el chat

**Descripción:** El sistema deberá mostrar en el chat la ruta del archivo o el motivo del error.

**Rol(es):** Usuario final \| **Prioridad:** Must

**Origen:** «Mostrar en el chat el resultado» (brief)

**Precondiciones:** Ninguna.

**Criterios de aceptación:**

- **CA-027.1 (caso feliz):** **Dado** un render exitoso, **cuando** finaliza, **entonces** el chat muestra una vez la ruta completa.

- **CA-027.2 (validación/error):** **Dado** un render fallido, **cuando** finaliza, **entonces** el chat muestra la causa y el juego sigue respondiendo.

**Notas / dudas:** *\[SUPUESTO\]* Sube de Media (brief) a Must: los criterios de error de RF-007, RF-009 y RF-021 dependen de él.

## 7.6 EP-6 Configuración

### RF-028: Lectura del archivo de configuración

**Descripción:** El sistema deberá leer al iniciar un archivo de configuración con carpeta de salida, resolución, cámara por defecto y tamaño máximo de región.

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** «Leer y guardar la configuración del mod» (brief)

**Precondiciones:** Ninguna.

**Criterios de aceptación:**

- **CA-028.1 (caso feliz):** **Dado** un archivo válido modificado, **cuando** se inicia el juego, **entonces** los renders usan esos valores.

- **CA-028.2 (validación/error):** **Dado** un archivo corrupto o con valores inválidos, **cuando** se inicia el juego, **entonces** se usan los valores por defecto y el log registra la causa.

- **CA-028.3 (caso límite):** **Dado** un archivo ausente, **cuando** se inicia el juego, **entonces** el mod funciona con valores por defecto.

### RF-029: Guardado de configuración y último ajuste

**Descripción:** El sistema deberá crear el archivo con valores por defecto si no existe y guardar en él el último ajuste de cámara y resolución usados en el menú.

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** «Leer y guardar la configuración del mod» (brief)

**Precondiciones:** Directorio con permisos de escritura.

**Criterios de aceptación:**

- **CA-029.1 (caso feliz):** **Dado** que el usuario cambió la cámara en el menú, **cuando** cierra el menú y reabre el juego más tarde, **entonces** el menú abre con el último ajuste.

- **CA-029.2 (validación/error):** **Dado** un directorio sin permisos de escritura, **cuando** se intenta guardar, **entonces** el juego continúa y el log registra el fallo.

**Notas / dudas:** *\[SUPUESTO\]* Recordar el último ajuste es idea del analista.

## 7.7 EP-7 Fuera de la v1 / opcionales

### RF-030: Render de ítem o bloque individual

**Descripción:** El sistema deberá renderizar un ítem en la mano, o un ítem o bloque indicado por ID y metadata.

**Rol(es):** Usuario final \| **Prioridad:** Could

**Origen:** «Render de un ítem (en mano o por ID)» (brief)

**Precondiciones:** Motor de render operativo.

**Criterios de aceptación:**

- **CA-030.1 (caso feliz):** **Dado** un ítem en la mano, **cuando** ejecuta el comando, **entonces** se genera su PNG isométrico.

- **CA-030.2 (validación/error):** **Dado** un ID inexistente, **cuando** ejecuta el comando, **entonces** el chat informa el error y no se genera archivo.

**Notas / dudas:** Conflicto CF-01: el brief original pedía esto; el nuevo enfoque lo hace secundario. Se estima aparte.

### RF-031: Render en lote

**Descripción:** El sistema deberá renderizar en lote una lista de ítems.

**Rol(es):** Usuario final \| **Prioridad:** Won't

**Origen:** «Renderizar en lote una lista de ítems» (brief)

**Precondiciones:** Fase 2.

**Criterios de aceptación:**

- **CA-031.1 (comportamiento en v1):** **Dado** el usuario pide un lote, **cuando** se ejecuta en la v1, **entonces** no existe la función.

**Notas / dudas:** *\[SUPUESTO\]* Pierde sentido con el enfoque en estructuras.

### RF-032: Fondo de la vista previa

**Descripción:** El menú deberá permitir elegir el fondo de la vista previa (tablero que revela la transparencia, blanco, gris, negro o verde croma).

**Rol(es):** Usuario final \| **Prioridad:** Could

**Origen:** Petición del solicitante durante la verificación de T-025 (2026-11-14); responde en parte a P-13.

**Criterios de aceptación:**

- **CA-032.1 (caso feliz):** **Dado** el menú abierto, **cuando** el usuario pulsa el botón de fondo, **entonces** la vista previa cambia al siguiente fondo y lo conserva al cerrar y reabrir el menú.

- **CA-032.2 (límite):** **Dado** cualquier fondo elegido, **cuando** se exporta el PNG, **entonces** el PNG sigue siendo transparente (el fondo solo afecta a la vista previa; confirmado por el solicitante).

### RF-033: Vista isométrica o en perspectiva

**Descripción:** El menú deberá permitir alternar entre la vista isométrica (proyección ortográfica, por defecto) y una vista en perspectiva, tanto en la vista previa como en el PNG exportado.

**Rol(es):** Usuario final \| **Prioridad:** Should

**Origen:** Petición del solicitante durante la verificación de T-025 (2026-11-14).

**Criterios de aceptación:**

- **CA-033.1 (caso feliz):** **Dado** el menú abierto, **cuando** el usuario alterna a «Perspectiva», **entonces** la vista previa muestra la estructura con perspectiva y al volver a «Isométrica» recupera la vista anterior.

- **CA-033.2 (límite):** **Dado** el modo perspectiva, **cuando** el usuario usa cualquier rotación, elevación, zoom o desplazamiento permitidos, **entonces** la estructura cabe en el encuadre automático (con el margen de RF-010) salvo lo que el usuario desplace o amplíe, y no se recorta por el plano cercano.

**Notas / dudas:** *\[SUPUESTO\]* Campo de visión fijo de 30°; el zoom actúa como ventana (la cámara no se acerca). *\[SUPUESTO\]* Ámbito: afecta también al PNG. Se mantiene el valor por defecto isométrico.

# 8. Requerimientos no funcionales

El solicitante no asignó prioridad a los RNF: todas son *\[SUPUESTO\]*. Los umbrales numéricos marcados como *\[SUPUESTO\]* los propone el analista porque el brief solo dice «unos segundos».

### RNF-001: Minecraft 1.12.2 y LiteLoader

**Categoría:** Compatibilidad \| **Prioridad:** Must

**Descripción:** El mod deberá funcionar solo en Minecraft 1.12.2 con LiteLoader y en un cliente vanilla sin Forge.

**Origen:** «debe convivir con un cliente vanilla sin Forge» (brief)

**Criterio de aceptación:**

- **Dado** un cliente vanilla 1.12.2 con LiteLoader sin Forge, **cuando** se ejecutan los casos de selección, render y menú con bloques vanilla, **entonces** todos pasan sin errores en el log.

### RNF-002: Tiempo de render de la estructura

**Categoría:** Rendimiento \| **Prioridad:** Should

**Descripción:** Un render de una región de 32 × 32 × 32 a 2048 px deberá bloquear el juego como máximo *\[SUPUESTO\]* 10 segundos en el equipo de referencia *\[SUPUESTO\]* (GPU dedicada de 4 GB de VRAM, 16 GB de RAM).

**Origen:** «no debe congelar el juego más de unos segundos» (brief)

**Criterio de aceptación:**

- **Dado** el equipo de referencia y una región de 32 × 32 × 32, **cuando** se ejecutan 5 renders a 2048 px, **entonces** el promedio es ≤ 10 s y ninguno supera 15 s.

### RNF-003: Fluidez de la vista previa

**Categoría:** Rendimiento \| **Prioridad:** Should

**Descripción:** La vista previa deberá actualizarse a al menos *\[SUPUESTO\]* 15 cuadros por segundo en regiones de hasta 16 × 16 × 16, y a 5 en regiones de hasta 64 × 64 × 64.

**Origen:** «menú interactivo» (mensaje del solicitante)

**Criterio de aceptación:**

- **Dado** el equipo de referencia y el menú abierto, **cuando** el usuario mueve un control de cámara durante 10 segundos, **entonces** el promedio de cuadros por segundo cumple el umbral de cada tamaño.

### RNF-004: Bordes limpios y sin halos

**Categoría:** Calidad de imagen \| **Prioridad:** Should

**Descripción:** Las imágenes deberán tener bordes con antialiasing y sin halos de color en el borde transparente.

**Origen:** «bordes limpios (antialiasing)... sin halos» (brief)

**Criterio de aceptación:**

- **Dado** 3 estructuras de referencia a 2048 px, **cuando** se componen sobre fondo blanco y negro y se inspeccionan al 400 %, **entonces** no se ve franja de color ajena ni bordes dentados.

### RNF-005: Tolerancia a fallos

**Categoría:** Estabilidad \| **Prioridad:** Must

**Descripción:** Si falla una selección, un render o el menú, el juego no deberá cerrarse y deberá informar el error.

**Origen:** «si falla la captura, el juego no se cierra» (brief)

**Criterio de aceptación:**

- **Dado** fallos forzados (carpeta de solo lectura, región inválida, memoria insuficiente simulada, resolución no soportada), **cuando** se provocan 30 fallos consecutivos, **entonces** el juego responde en todos y el error se informa.

### RNF-006: Java 8, paquetes y comentarios

**Categoría:** Mantenibilidad \| **Prioridad:** Should

**Descripción:** El código deberá compilar con Java 8, organizarse en paquetes por responsabilidad (selección, render, menú, configuración) y comentar los puntos de render.

**Origen:** «código en Java 8, estructura por paquetes y comentarios» (brief)

**Criterio de aceptación:**

- **Dado** el repositorio, **cuando** se compila con JDK 8 y se revisa con lista de verificación, **entonces** compila, existen los paquetes y cada método de render tiene comentario.

### RNF-007: Versión y licencia declaradas

**Categoría:** Distribución \| **Prioridad:** Must

**Descripción:** El .litemod deberá declarar su versión y su licencia.

**Origen:** «con versión y licencia declaradas» (brief)

**Criterio de aceptación:**

- **Dado** el .litemod instalado, **cuando** se consulta la lista de mods y el contenido del paquete, **entonces** se muestran nombre y versión y el paquete incluye la licencia.

### RNF-008: Licencia del mod original

**Categoría:** Legal \| **Prioridad:** Must

**Descripción:** Si se usa código o recursos de isometric-renders, deberá cumplirse su licencia; si no se usa nada, deberá constar.

**Origen:** «respetar la licencia del mod original» (brief)

**Criterio de aceptación:**

- **Dado** el código final, **cuando** se realiza la revisión legal previa a la entrega, **entonces** el registro indica que no hay código ni recursos del original o que su licencia fue cumplida.

### RNF-009: Liberación de recursos gráficos

**Categoría:** Gestión de recursos \| **Prioridad:** Should

**Descripción:** El mod deberá liberar framebuffer, texturas y listas de dibujo tras cada render y al cerrar el menú *\[IMPLÍCITO\]*.

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Criterio de aceptación:**

- **Dado** el equipo de referencia, **cuando** se realizan 30 renders de 1024 px y se abre y cierra el menú 30 veces, **entonces** la memoria JVM y la VRAM no superan en más de *\[SUPUESTO\]* 5 % a las medidas tras la primera iteración.

### RNF-010: Estado gráfico restaurado

**Categoría:** Integridad del render \| **Prioridad:** Must

**Descripción:** Tras cada render y al cerrar el menú, el juego deberá renderizarse igual que antes (viewport, matrices, framebuffer) *\[IMPLÍCITO\]*.

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Criterio de aceptación:**

- **Dado** la pantalla de juego antes de abrir el menú, **cuando** termina un render y se cierra el menú, **entonces** el siguiente fotograma es visualmente idéntico, sin deformaciones ni pantalla negra.

### RNF-011: Idioma de la interfaz y los mensajes

**Categoría:** Idioma \| **Prioridad:** Could

**Descripción:** Los textos del menú y los mensajes del chat deberán estar en español *\[SUPUESTO\]*.

**Origen:** *\[SUPUESTO\]* No indicado por el solicitante

**Criterio de aceptación:**

- **Dado** el menú abierto y un render exitoso y uno fallido, **cuando** se leen los textos, **entonces** todos están en español.

### RNF-012: Menú legible y usable

**Categoría:** Usabilidad \| **Prioridad:** Should

**Descripción:** El menú deberá mostrar todos sus controles, con etiqueta y valor visibles y sin superposición, en ventanas desde 854 × 480 hasta 1920 × 1080 *\[SUPUESTO\]*.

**Origen:** «menú interactivo» (mensaje del solicitante)

**Criterio de aceptación:**

- **Dado** el juego en 854 × 480 y en 1920 × 1080, **cuando** se abre el menú, **entonces** todos los controles son visibles, etiquetados y utilizables.

### RNF-013: El mundo no se modifica

**Categoría:** Seguridad de datos \| **Prioridad:** Must

**Descripción:** El mod no deberá modificar bloques ni entidades del mundo al seleccionar, previsualizar o renderizar *\[IMPLÍCITO\]*.

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Criterio de aceptación:**

- **Dado** una región con 1000 bloques, **cuando** se realizan 20 selecciones y renders, **entonces** el recuento y estado de los bloques es idéntico antes y después.

### RNF-014: Límite de tamaño sin agotar memoria

**Categoría:** Memoria \| **Prioridad:** Should

**Descripción:** Una región de 64 × 64 × 64 deberá renderizarse sin OutOfMemoryError con la memoria por defecto del lanzador oficial (2 GB) *\[SUPUESTO\]*.

**Origen:** *\[IMPLÍCITO\]* Necesario para que lo pedido funcione

**Criterio de aceptación:**

- **Dado** el juego con 2 GB de memoria, **cuando** se renderiza una región de 64 × 64 × 64, **entonces** el render termina o se rechaza con mensaje, sin cerrar el juego.

# 9. Reglas de negocio

| **ID** | **Regla** | **Origen** | **Requisitos** |
|---|---|---|---|
| RN-001 | Solo se renderizan bloques; no se renderizan mobs, jugadores, ítems en el suelo ni entidades similares *\[SUPUESTO sobre marcos y pinturas\]*. | «Entidades, mobs y jugadores» fuera (brief) | RF-009, RF-013 |
| RN-002 | La región es la caja alineada a los ejes que contiene los dos puntos, inclusive; el orden de los puntos no importa. | «Selecciono punto 1 y punto 2» | RF-005 |
| RN-003 | Tamaño máximo de región: 64 × 64 × 64 bloques *\[SUPUESTO\]*. | Implícito (rendimiento) | RF-007, RNF-014 |
| RN-004 | Solo se renderizan bloques de chunks cargados en el cliente. | Implícito (mod de cliente) | RF-007 |
| RN-005 | El fondo siempre es transparente; en la v1 no hay fondo opaco *\[SUPUESTO\]*. | «fondo transparente» (brief) | RF-011 |
| RN-006 | Resolución de 512 a 4096 px; imagen cuadrada *\[SUPUESTO\]*. | «por ejemplo 512 a 4096 px» (brief) | RF-020, RF-025 |
| RN-007 | Rangos de cámara *\[SUPUESTO\]*: rotación 0°–359°, elevación 5°–90°, zoom 25 %–400 %, desplazamiento ±100 %. Por defecto: rotación 45°, elevación 35,264°, zoom 100 %. | «cuadrar el render camara, ángulo» | RF-017, RF-018, RF-022 |
| RN-008 | El mod nunca modifica el mundo. | Implícito | RNF-013 |
| RN-009 | No se copia código ni recursos del mod original sin verificar y cumplir su licencia. | «respetar la licencia» (brief) | RNF-008 |
| RN-010 | Solo se garantizan bloques vanilla; los de otros mods son de mejor esfuerzo *\[SUPUESTO\]*. | Pregunta del brief sobre mods | RF-009, RF-030 |

# 10. Modelo de datos y reportes

El mod no usa base de datos. Las entidades de trabajo son:

| **Entidad** | **Atributos clave** | **Relaciones** |
|---|---|---|
| Selección | Punto 1 (x, y, z); punto 2 (x, y, z). | Define una Región. |
| Región | Esquinas mínima y máxima; dimensiones; bloques contenidos. | Es la entrada del render. |
| Ajustes de cámara | Rotación; elevación; zoom; desplazamiento; resolución; corte por altura (opcional). | Se editan en el menú y se guardan en la Configuración. |
| Configuración | Carpeta de salida; resolución; cámara por defecto; tamaño máximo; último ajuste. | Alimenta los Ajustes de cámara. |
| Resultado de render | Estado (éxito/error); ruta del archivo; mensaje. | Se muestra en el chat (RF-027). |
| Imagen PNG | Ancho = alto = resolución; canal alfa; nombre con fecha y coordenadas. | Resultado de un render exitoso. |

**Reportes / exportaciones:** el único producto es el PNG. *\[IMPLÍCITO\]* Los errores y advertencias se registran en el log del juego.

# 11. Integraciones e interfaces

## 11.1 Interfaces con el entorno

| **Sistema** | **Qué se espera** |
|---|---|
| Minecraft 1.12.2 (API de cliente) | Leer bloques y estados del mundo cargado, el bloque apuntado por el jugador y los renderizadores de bloques y entidades de bloque. |
| LiteLoader 1.12.2 | Cargar el mod, registrar tecla y comando, mostrar una pantalla propia y ejecutar código en el ciclo de render. |
| OpenGL / LWJGL 2 | Crear el framebuffer, dibujar con proyección ortográfica y leer los píxeles. |
| Sistema de archivos | Escribir PNG; leer y escribir la configuración. |

## 11.2 Interfaces de usuario y de salida

| **Interfaz** | **Especificación *\[SUPUESTO\]* salvo lo indicado** |
|---|---|
| Comandos | /isorender pos1 · /isorender pos2 · /isorender clear · /isorender menu · /isorender render \[resolución\] |
| Tecla | «K» abre el menú; reasignable en los controles de Minecraft. |
| Menú | Vista previa; deslizadores y campos de rotación, elevación, zoom y desplazamiento; selector de resolución; botones Renderizar, Restablecer y presets; estado de la selección (coordenadas y dimensiones). |
| Configuración | Archivo en el directorio de configuración de LiteLoader: carpetaSalida, resolución (1024), rotación (45), elevación (35,264), zoom (100 %), margen (5 %), tamañoMáximo (64). |
| Salida | PNG RGBA de 32 bits, cuadrado N × N, fondo con alfa 0. Nombre: estructura\_\<fecha-hora\>\_\<x1,y1,z1\>\_\<x2,y2,z2\>.png. |
| Mensajes | Texto en el chat con la ruta del archivo o el error, en español. |

# 12. Riesgos

| **ID** | **Riesgo** | **Prob.** | **Impacto** | **Mitigación** |
|---|---|---|---|---|
| R-01 | LiteLoader sin mantenimiento y con poca documentación, sobre todo para pantallas propias y render GL. | Alta | Alto | Prototipo mínimo al inicio; hito go/no-go; si falla, escalar al solicitante para ajustar el alcance (no se cambia de loader). |
| R-02 | Rendimiento y memoria con regiones grandes (hasta 262.144 bloques). | Alta | Alto | Límite de 64³; caché de geometría; vista previa simplificada en regiones grandes. |
| R-03 | La vista previa en vivo no es fluida. | Media | Medio | Reducir calidad en la vista previa; actualizar al soltar el control como alternativa (P-12). |
| R-04 | Orden de dibujo de transparencias (cristal, agua, hojas) produce artefactos. | Media | Medio | Dibujar opacos primero y translúcidos ordenados; pruebas con estructuras de referencia. |
| R-05 | Transparencia y antialiasing en el framebuffer producen halos. | Media | Medio | Prueba temprana con una estructura de referencia (T-019). |
| R-06 | Entidades de bloque (cofres, camas) difíciles de renderizar fuera de su contexto. | Alta | Medio | Prioridad Should; omitir con aviso si falla. |
| R-07 | Chunks no cargados dejan huecos en la región. | Alta | Medio | Validar antes del render y avisar (RF-007). |
| R-08 | El render altera el estado gráfico y deforma el juego. | Media | Alto | Guardar y restaurar el estado (RNF-010) y probarlo. |
| R-09 | Resoluciones altas (4096 px) superan el límite de la GPU. | Media | Medio | Consultar límites de GL y mostrar mensaje (RF-025). |
| R-10 | Toolchain antigua (ForgeGradle, JDK 8) difícil de montar. | Media | Medio | Documentar y fijar versiones desde el inicio. |
| R-11 | Dependencia de una sola persona. | Media | Alto | Documentación del entorno y del código; buffer de contingencia. |
| R-12 | Licencia del mod original. | Baja | Bajo | No copiar código; verificar la licencia si se desea. |

# 13. Preguntas abiertas y conflictos

## 13.1 Preguntas abiertas

| **ID** | **Pregunta** | **Tipo** | **Afecta** | **Supuesto aplicado** |
|---|---|---|---|---|
| P-01 | ¿Para qué se usarán las imágenes (wiki, redes, portafolio, servidor propio)? | No bloqueante | RF-025, RNF-004 | Contenido y portafolio; resolución por defecto 1024 px. |
| P-02 | ¿LiteLoader es obligatorio o se acepta Forge 1.12.2? | **Resuelta** (13/11/2026) | RF-001, RF-002, RNF-001, RNF-007 | Confirmado: LiteLoader obligatorio; Forge descartado. |
| P-03 | ¿Solo bloques vanilla o también los de otros mods? | No bloqueante | RF-009, RN-010 | Solo vanilla garantizado. Ver CF-03. |
| P-04 | ¿Qué resolución máxima y qué ángulo isométrico por defecto? | No bloqueante | RF-017, RF-025 | 512–4096 px; elevación 35,264° y rotación 45°. |
| P-05 | ¿El mod será público? ¿Con qué licencia? | No bloqueante | RNF-007, RNF-008 | **Resuelta (2026-11-14, solicitante): licencia MIT** (archivo LICENSE en la raíz y dentro del .litemod). |
| P-06 | ¿Hay fecha límite, presupuesto o cantidad de personas? | **Bloqueante** (para comprometer el plan) | Plan de trabajo | Ejecuta Claude Code; sin fecha límite ni presupuesto; el ritmo lo marcan las verificaciones en juego del solicitante. |
| P-07 | ¿Idioma de los textos del menú y del chat? | No bloqueante | RNF-011 | Español. |
| P-08 | ¿Cómo se seleccionan los puntos: comandos, una varita tipo WorldEdit, clic sobre bloques o ambos? | No bloqueante | RF-003, RF-004 | Comandos sobre el bloque apuntado. |
| P-09 | ¿Qué tamaño máximo de estructura se espera y funciona solo con chunks cargados? | No bloqueante | RF-007, RN-003, RNF-002, RNF-014 | 64 × 64 × 64 y solo chunks cargados. |
| P-10 | ¿Se usará en un jugador, en servidores o en ambos? | No bloqueante | RF-003, RN-004 | Ambos, como mod de cliente. |
| P-11 | ¿Se deben incluir entidades dentro de la estructura (marcos, pinturas, armor stands)? | No bloqueante | RN-001 | No. |
| P-12 | ¿La vista previa debe ser en tiempo real o basta con un botón «Actualizar»? | No bloqueante | RF-019, RNF-003 | Tiempo real, con modo simplificado en regiones grandes. |
| P-13 | ¿Qué otros ajustes debe tener el menú además de cámara y ángulo (iluminación, margen, recorte de la imagen, fondo)? | No bloqueante | RF-017 a RF-023 | Rotación, elevación, zoom, desplazamiento y resolución. |
| P-14 | ¿Qué formato de imagen se prefiere: cuadrada o recortada a la silueta? | No bloqueante | RF-025, RN-006 | Cuadrada. |
| P-15 | ¿Se necesita el render de ítem o bloque individual del brief original? | No bloqueante | RF-030 | Opcional (Could). |
| P-16 | ¿Cuál es el equipo de referencia para medir el rendimiento? | No bloqueante | RNF-002, RNF-003 | GPU dedicada de 4 GB, 16 GB de RAM. |

## 13.2 Conflictos detectados

| **ID** | **Conflicto** | **Resolución provisional** |
|---|---|---|
| CF-01 | El brief pide render de ítems y bloques individuales; el solicitante ahora dice que el mod es para estructuras. | El foco de la v1 son las estructuras; ítem y bloque individual pasan a Could (RF-030). Confirmar en P-15. |
| CF-02 | El brief deja fuera los bloques con entidad de bloque; las estructuras suelen tener cofres, camas y carteles. | RF-013 como Should en la v1, con omisión avisada si falla. |
| CF-03 | El brief pide convivir con un cliente vanilla sin Forge y pregunta por mods; los bloques de otros mods suelen requerir Forge. *\[SUPUESTO\]* LiteLoader puede correr sobre Forge; no verificado. | Solo vanilla garantizado (RN-010), porque LiteLoader es obligatorio. Confirmar en P-03. |
| CF-04 | El brief sugiere prototipo «en la primera semana»; se supone una persona a tiempo parcial. | El prototipo queda al final del sprint 3 (ver Plan de Trabajo). |
| CF-05 | El brief da a «ajustar ángulo y escala» prioridad Media; el solicitante ahora pide un menú interactivo como parte central. | Los ajustes de cámara y el menú pasan a Must (RF-016 a RF-021). |

# 14. Matriz de trazabilidad

| **Fragmento del solicitante o del brief** | **Requisitos** | **Épica del plan** |
|---|---|---|
| «Selecciono punto 1 y punto 2» (mensaje) | RF-003, RF-004, RF-005, RF-007, RF-008 | E3 Selección de región |
| «crea el render de eso» (mensaje) | RF-009, RF-010, RF-014, RF-021 | E4 Motor, E5 Menú |
| «menú interactivo para cuadrar el render» (mensaje) | RF-016, RF-019, RF-020, RF-022 | E5 Menú interactivo |
| «camara, ángulo, etc» (mensaje) | RF-017, RF-018, RF-023, RF-033 | E4 Motor, E5 Menú |
| «un selector para elegir el fondo de la preview» (2026-11-14) | RF-032 | E5 Menú |
| «El mod se carga en LiteLoader» (RF-01) | RF-001 | E2 Prototipo |
| «un solo archivo .litemod» | RF-002, RNF-007 | E7 Empaquetado |
| «fondo transparente» (RF-04) | RF-011 | E4 Motor |
| «Exportar la imagen a PNG» (RF-04) | RF-024, RF-026 | E4 Motor, E5 |
| «resolución de salida» (RF-05) | RF-020, RF-025 | E4, E5 |
| «Mostrar en el chat el resultado» (RF-08) | RF-027 | E5 Menú y salida |
| «Leer y guardar la configuración» (RF-09) | RF-028, RF-029 | E6 Configuración |
| «Disparo por tecla y por comando» | RF-003, RF-004, RF-016 | E3, E5 |
| «Render de un ítem (en mano o por ID)» (RF-02, RF-03) | RF-030 | E10 Opcional |
| «Renderizar en lote» (RF-10) | RF-031 | Fuera (Won't) |
| «Bloques con entidad de bloque» (RF-11) | RF-013 | E4 Motor |
| Compatibilidad, rendimiento, calidad, estabilidad | RNF-001 a RNF-005 | E3, E4, E8 |
| Mantenibilidad, distribución, legal | RNF-006, RNF-007, RNF-008 | E1, E7, E9 |
| Implícitos: contorno, validación, encuadre, estado real, mundo intacto | RF-006, RF-007, RF-010, RF-014, RNF-013 | E3, E4 |
| Exportación por WebSocket, atlas, entidades | — | Fuera de alcance |

La trazabilidad entre requisitos y casos de prueba (CP-01 a CP-31) está en el Plan de Trabajo, sección 7.

# 15. Criterios globales y DoD

Todo entregable se considera terminado cuando cumple:

- Los casos de prueba de los requisitos Must pasan y no hay defectos críticos ni altos abiertos.

- El código está revisado, compila con JDK 8 y está en el repositorio con su etiqueta de versión.

- El .litemod se probó en un cliente vanilla 1.12.2 con LiteLoader limpio y con estructuras de referencia.

- La documentación (manual de usuario y técnica) está actualizada.

- Los supuestos *\[SUPUESTO\]* e *\[IMPLÍCITO\]* fueron validados por el solicitante.

- El solicitante aprobó la entrega en la UAT.

# Control de versiones

| **Versión** | **Fecha** | **Cambios** | **Autor** |
|---|---|---|---|
| v0.1 | 13/11/2026 | Borrador inicial a partir del brief (ítems y bloques individuales). | Analista de software (Claude) |
| v0.2 | 13/11/2026 | Reenfoque a estructuras: selección de punto 1 y punto 2, render de la región y menú interactivo de cámara. Se rehacen requisitos, plan y estimación. | Analista de software (Claude) |
| v0.3 | 13/11/2026 | LiteLoader confirmado como obligatorio (P-02 resuelta); se elimina el plan B con Forge. | Analista de software (Claude) |
