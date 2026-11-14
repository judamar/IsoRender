# Documentación técnica de IsoRender (T-039, RNF-006)

Para quien mantiene o amplía el mod. Complementa a `docs/ERS.md` (qué), `docs/PLAN.md` (cómo y cuándo), `docs/ENTORNO.md` (toolchain y compilación), `docs/LITELOADER-API.md` (API de LiteLoader verificada), `docs/LICENCIAS.md` y `docs/CASOS-PRUEBA.md`. Los usuarios finales deben leer `docs/MANUAL.md`.

Todo lo de este documento está contrastado con el código de `src/main/java/com/judamar/isorender/` (versión 0.1.0).

## 1. Visión general

Mod de cliente para Minecraft 1.12.2 cargado con LiteLoader (sin Forge), Java 8, mappings MCP `snapshot_20170624`, OpenGL de función fija con LWJGL 2. Dada una región (dos puntos) genera un PNG RGBA cuadrado con fondo transparente. El motor dibuja la región en un framebuffer propio (FBO), lo lee por franjas y lo reduce con supermuestreo. Una vista previa en el menú reutiliza el mismo motor con otro FBO.

Reglas que condicionan todo el diseño (de `CLAUDE.md`): el mundo es de solo lectura; un fallo de selección, render o menú no cierra el juego (se informa en el chat y se restaura el estado gráfico); textos al usuario en español; lógica pura separada de la que toca Minecraft para poder probarla con JUnit.

## 2. Arquitectura por paquetes

Raíz: `com.judamar.isorender`.

| Paquete | Responsabilidad | Clases clave |
|---|---|---|
| (raíz) | Punto de entrada y orquestación | `LiteModIsoRender` |
| `seleccion` | Estado de la selección, análisis de comandos y validación de la región | `Seleccion`, `Subcomando`, `ValidacionRegion` (pura), `ValidacionMundo` (usa `World`) |
| `configuracion` | Valores de configuración, lectura/escritura y acceso global | `Configuracion` (pura), `ArchivoConfiguracion` (E/S), `Ajustes` |
| `menu` | Pantalla del menú y estado de cámara que edita | `PantallaMenu`, `Deslizador`, `EstadoCamara`, `Parametro`, `PresetCamara`, `TextoSeleccion` |
| `render` | Motor de render, vista previa, PNG, mensajes y robustez | ver §2.1 |

### 2.1 Clases del paquete `render`

| Grupo | Clases | Papel |
|---|---|---|
| Cámara | `ParametrosCamara` (pura), `CamaraGL` | Rangos, encuadre automático y distancia de perspectiva; aplicación de la proyección y el modelview |
| Geometría | `GeometriaCacheada`, `VistaRegion`, `GeometriaRegion` (pura), `OrdenTranslucido` (pura), `ResumenGeometria` (pura) | Lectura del mundo una sola vez, display lists por capa, orden de translúcidas |
| Entidades | `EntidadesBloque`, `EntidadesDecorativas`, `ResumenEntidades` (pura) | Entidades de bloque y marcos/pinturas/armor stands |
| Salida de imagen | `FramebufferRender`, `ConversorImagen` (pura), `ResolucionSalida` (pura), `NombreArchivo` (pura) | FBO, lectura por franjas, reducción a ARGB y PNG, nombres únicos |
| Orquestación | `RenderPrueba`, `Estres`, `ResumenEstres` (pura), `ArgumentoItem` (pura) | Flujo de un render completo, render de ítem, pruebas de estrés |
| Vista previa | `VistaPrevia`, `VistaPreviaCalculo` (pura), `FondoVista` | FBO propio, estados, modo simplificado |
| Robustez | `EstadoGrafico`, `Guardia` (pura), `Fallos` (pura salvo `informar`), `EstimacionMemoria` (pura), `Chat` | Captura/restauración de estado GL, exclusión mutua, errores, memoria, salida al chat |
| Mundo | `ContornoSeleccion` | Caja de la selección en el mundo |

«Pura» = sin clases de Minecraft ni OpenGL; son las que cubren las pruebas unitarias (§9).

### 2.2 Dependencias entre paquetes

`LiteModIsoRender` conoce a todos. `configuracion.Configuracion` depende de tipos de `render` (`ParametrosCamara`, `FondoVista`, `ResolucionSalida`) y de `seleccion.ValidacionRegion` (la constante `MAX`). `render.VistaPrevia` usa `menu.TextoSeleccion`, `seleccion.*` y `configuracion.Ajustes`. Hay dependencias en ambos sentidos (`configuracion` → `render` y `render` → `configuracion`), así que los paquetes no son capas estrictas: tenlo en cuenta al mover clases.

## 3. Ciclo de vida

Todo corre en el hilo de cliente de Minecraft (que es el de render). Los `volatile` de `LiteModIsoRender` son defensivos.

```
 arranque
   LiteLoader instancia LiteModIsoRender (clase LiteMod*, descubierta por nombre)
   init(configPath): registra la KeyBinding «Abrir menú IsoRender» (K, categoría IsoRender),
                      carga liteconfig/common/isorender.properties, siembra EstadoCamara y Ajustes

 cada fotograma
   onTick (Tickable)            saludo al entrar a un mundo; lee la tecla K; abre PantallaMenu
        │                       si (tecla o abrirMenu) y no hay otra pantalla
   [chat] OutboundChatFilter    onSendChatMessage: Subcomando.parsear; marca flags/slots y devuelve false
        │                       (cancela el envío al servidor); pos1/pos2/clear/uso se atienden aquí mismo
   onRender (RenderListener)    ANTES de dibujar el fotograma: ejecuta el trabajo diferido, en este orden:
        │                         1. renderMenu  → renderDesdeMenu
        │                         2. estres en curso → un paso por fotograma
        │                         3. pendiente (prueba, prueba-multi, estres, item)
   [mundo, GUI del juego]
   onPostRender (PostRenderListener)  con la cámara del mundo ya puesta: ContornoSeleccion.dibujar
```

Por qué el trabajo se difiere a `onRender`:

- El filtro de chat se ejecuta dentro de `EntityPlayerSP.sendChatMessage`, no en el bucle de dibujo; el render offscreen necesita un contexto GL en un punto controlado del fotograma. Además `GuiChat` cierra la pantalla después de enviar, por eso `/isorender menu` activa una bandera que `onTick` atiende en el tick siguiente (así `mc.currentScreen == null`).
- Hay un solo hueco `pendiente` y la `Guardia` de `RenderPrueba`: un segundo comando se rechaza con `Chat.OCUPADO` en lugar de pisar al primero. `/isorender render` usa su propia bandera (`renderMenu`) y comprueba el mismo conjunto de ocupación.
- `Estres` retiene la guardia entre fotogramas y hace un render por fotograma para no congelar el juego.

Comandos: LiteLoader no ofrece comandos de cliente (`ServerCommandProvider` es de servidor y no existe `ClientCommandHandler`); `OutboundChatFilter` es el mecanismo válido y por eso no hay autocompletado (ver `LITELOADER-API.md` §4). El análisis es sensible a mayúsculas: solo `/isorender` en minúsculas.

Menú: `PantallaMenu` es una `GuiScreen` normal abierta con `displayGuiScreen`; no pausa el juego. `EstadoCamara` vive en el mod (no en la pantalla) para conservar cámara, fondo, resolución y corte Y al cerrar y reabrir. `onGuiClosed` llama a `guardarConfiguracion` (que compara con `ultimoGuardado` y no escribe si no hay cambios).

## 4. Flujo de render de un PNG

```
 [Renderizar]  /  /isorender render
        │
        ▼
 PantallaMenu.actionPerformed ─ displayGuiScreen(null) ─► onGuiClosed ─► guardarConfiguracion
        │ alRenderizar.run()  ⇒  renderMenu = true
        ▼                                                          (siguiente fotograma)
 LiteModIsoRender.onRender ─► renderDesdeMenu
        │   ValidacionMundo.validar(puntos, ≤ tamanoMaximo, chunks cargados, no solo aire)
        │   RenderPrueba.exportar(regionConCorte(región, corteY), resolución, cámara)
        ▼
 RenderPrueba.render ── GUARDIA.entrar() (si ocupada: aviso y fin)
        │
        ├─(a) CONSTRUCCIÓN  construirControlado
        │        EstimacionMemoria.comprobarGeometria (un System.gc() y reintento antes de rechazar)
        │        GeometriaCacheada.construir
        │          · EstadoGrafico.capturarAjustes + forzar AO suave y hojas «fancy»
        │          · VistaRegion (aire fuera de la caja, luz máxima) → renderBlock por bloque
        │            a 4 BufferBuilder (SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT)
        │          · translúcidas: se copian las caras crudas (getRawBuffer)
        │          · EntidadesBloque.recoger + EntidadesDecorativas.recoger (solo lectura)
        │          · 3 capas opacas → 3 display lists (la translúcida se compila al dibujar)
        │
        ├─(b) DIBUJO  aImagen(res, escena)
        │        factor = factorSupermuestreo(res, GPU) → factorSeguro (heap y VRAM NVIDIA)
        │        EstadoGrafico.capturarCompleto  ← estado REAL previo (glGet*)
        │        FramebufferRender(res·factor); enlazarYLimpiar (alfa 0, reenlaza tras el clear)
        │        dibujarEscena
        │          CamaraGL.aplicar (ortográfica o frustum + encuadre automático)
        │          pasada 1  opacas: blend (SRC_ALPHA,0 ; 1,0)  → color premultiplicado
        │          pasada 1b entidades de bloque, luego decorativas (con su estado aislado)
        │          pasada 2  translúcidas: ordenadas atrás→delante, blend premultiplicado, depthMask(false)
        │        leerFranja × N (franjas de 128·factor filas) → ConversorImagen.reducirFranja
        │        finally: fbo.close()  ·  previo.restaurar()  ·  glGetError → aviso si hubo
        │
        ├─ avisos de bloques/entidades omitidos
        ├─ NombreArchivo.base + unico  →  ConversorImagen.guardarPng (crea carpeta; borra PNG parcial si falla)
        └─ Chat.archivo (ruta clicable)
        finally: geo.close() (borra las display lists) · GUARDIA.salir()
        (cualquier Throwable ⇒ Fallos.informar: traza a consola, mensaje corto al chat)
```

La vista previa sigue el mismo camino a partir de `dibujarEscena`, con su propio FBO y sin pasar por `ConversorImagen` (§5.9).

## 5. Puntos de render

Cada punto está comentado en el código; aquí el porqué.

### 5.1 `FramebufferRender`
FBO cuadrado de `lado` px con profundidad, sobre `net.minecraft.client.shader.Framebuffer`. El constructor valida el límite de la GPU (`min(GL_MAX_TEXTURE_SIZE, GL_MAX_RENDERBUFFER_SIZE)`), limpia errores GL ajenos, crea el FBO, comprueba `glGetError` y `glCheckFramebufferStatus`, y si algo falla hace `close()` antes de relanzar (no quedan recursos a medias). Lanza `IllegalStateException` con mensaje en español. `vramLibre()` usa `NVX_gpu_memory_info` (solo NVIDIA; -1 = desconocida). La lectura es `glReadPixels` por franjas para no reservar de golpe `lado*lado*4` bytes. `close()` borra el FBO y reenlaza `mc.getFramebuffer()`.

`enlazarYLimpiar()` reenlaza el FBO después de `framebufferClear()` (ver §7, lección 2) y antes fija `colorMask`, `depthMask` y desactiva el scissor, porque `glClear` los respeta.

### 5.2 `CamaraGL` y `ParametrosCamara` (cámara y encuadre)
`CamaraGL.aplicar` carga proyección e `modelview`. Modelview común: `rotate(elevación, X) · rotate(rotación, Y) · translate(-centro de la región)`.

- **Ortográfica (isométrica):** `semiAncho = semiAnchoBase · 100 / zoom`, con
  `semiAnchoBase = max sobre las 8 esquinas de max(|xp|, |yp|) / (1 − MARGEN)`, donde `xp`, `yp` son las coordenadas de pantalla de cada esquina tras rotación e inclinación y `MARGEN = 0,05`. La ventana es `ortho(cx−r, cx+r, cy−r, cy+r, −256, 256)` con `cx = −despX·r`, `cy = −despY·r` (el desplazamiento mueve la estructura, así que la ventana se mueve en sentido contrario). Plano ±256 fijo: la diagonal máxima de 64³ (≈111) cabe.
- **Perspectiva:** FOV vertical fijo `FOV_GRADOS = 30` (`t = tan(15°)`). La cámara se aleja `d` del centro, con
  `d = max( max sobre esquinas de [ c + max(|xp|,|yp|) / (t·(1−MARGEN)) ] , 0,5·diagonal + 1 )`,
  donde `c = y·sen(e) + zp·cos(e)` es la cercanía de la esquina a la cámara. La primera cota hace que las 8 esquinas quepan con margen; la segunda deja la cámara fuera de la esfera que envuelve la región (ninguna esquina queda detrás). Planos: `near = 0,9·(d − cMax)`, `far = 1,1·(d − cMin) + 1`. El zoom y el desplazamiento no mueven la cámara: escalan y desplazan la ventana del frustum en el plano near (`h = near·t·100/zoom`; `glFrustum(cx−h, cx+h, cy−h, cy+h, near, far)`, `cx = −despX·h`), y `translate(0,0,−d)`.

`ParametrosCamara` es inmutable; `crear` valida (lanza con mensaje y rango) y `limitado` recorta (controles del menú). Rangos: rotación 0–359, elevación 5–90, zoom 25–400, desplazamiento ±1 (±100 %).

### 5.3 `GeometriaCacheada`, `GeometriaRegion`, `VistaRegion`
La región se lee **una vez**: `BlockRendererDispatcher.renderBlock(estado, pos, vista, bufferDeCapa)` por cada bloque (con `getAllInBoxMutable`, un solo `BlockPos` reutilizado). `renderBlock` llama a `getActualState`, así que orientación, conexiones y mitades salen correctas. Cada bloque va en try/catch: un bloque de mod que falla se cuenta (`fallidos`) y se omite (RN-010). Las capas desconocidas (de mods) se omiten.

`VistaRegion` implementa `IBlockAccess` sobre el mundo: dentro de la caja delega, fuera devuelve aire (así se dibujan las caras del borde y funcionan el culling interior y la oclusión ambiental), `getCombinedLight` devuelve siempre luz máxima (mismo PNG de día y de noche) y `getBiome` delega (tinte real). `getTileEntity` fuera de la caja devuelve null. [SUPUESTO] las conexiones de vallas y paneles hacia fuera de la región quedan cortadas.

Mientras construye, `EstadoGrafico.forzarAjustesGeometria()` fija `ambientOcclusion = 2` y las hojas en modo «fancy» (capa determinista); `restaurar()` devuelve los ajustes del jugador siempre.

**Display lists por capa** (SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT): cada lista reproduce `WorldVertexBufferUploader.draw(buffer)`. Se eligieron frente a VBO porque es el camino de vanilla 1.12 sin VBO (`ListedRenderChunk`): conservan sin código extra los punteros de vértice, color, UV y lightmap de `DefaultVertexFormats.BLOCK`, el driver copia los datos a su memoria y no hay buffers ni estado de cliente GL que enlazar o limpiar. Si una medición futura lo exige, el cambio es local a esta clase. `close()` borra las listas y es idempotente; `construir` las borra si falla a medias.

`GeometriaRegion` aloja la lógica pura: orden de capas, `contiene`, `volumen`, `bytesIniciales` (acotado a 64 MB por capa), `recortar` (corte Y) y `parsearN`.

### 5.4 `OrdenTranslucido`
Las caras translúcidas se guardan crudas y se recompilan ordenadas de atrás hacia delante solo cuando cambian rotación, elevación o modo (isométrica/perspectiva); zoom y desplazamiento no alteran el orden. En ortográfica la profundidad exacta es la coordenada z del espacio de vista (`y·sen(e) + (−x·sen(r) + z·cos(r))·cos(e)`), no la distancia a un punto como hace `sortVertexData` de vanilla (que solo acierta con cámara en perspectiva). En perspectiva se ordena por distancia al ojo, calculado en coordenadas de mundo. Se ordena por el centroide de la cara (vale para caras de bloque) con TimSort (estable en empates).

### 5.5 `EntidadesBloque`
En 1.12 cofres, camas, carteles y cabezas no tienen modelo estático (o solo parcial): vanilla usa `TileEntitySpecialRenderer`. `recoger` recorre el mapa de entidades de los chunks **ya cargados** (`getLoadedChunk`, que no carga ni crea nada; `World.getTileEntity` podría crear la entidad que falte) y excluye renderers globales (baliza, bloque de estructura), spawner (su renderer crea una entidad cacheada) y pistón en movimiento. `dibujar` no usa display list (los renderers usan estado GL y animación): se llama a `TileEntityRendererDispatcher.render` (versión de 7 argumentos, sin comprobar distancia al jugador) con la cámara activa. Detalles:

- `cameraHitResult = null` para que ningún renderer dibuje el rótulo del nombre; `partialTicks = 0`.
- Se traslada al origen de la región y se pasan coordenadas relativas (con coordenadas de mundo grandes el `float` perdería precisión).
- Cada entidad va en try/catch; tras cada una `restaurarEstado` iguala la profundidad de la pila de matrices, apaga la luz de objeto y devuelve el estado que espera la pasada de sólidos. Los campos públicos del dispatcher se guardan y restauran.
- Se dibujan tras los sólidos y antes de las translúcidas.

### 5.6 `EntidadesDecorativas`
Solo marcos de objetos, pinturas y armor stands (RN-001/RF-034), comprobados con `instanceof` explícitos para que una entidad de mod que herede de `EntityHanging` no entre. `recoger` usa `getEntitiesWithinAABB` (solo lectura) y filtra por el bloque de anclaje (`getHangingPosition`) o el bloque base (armor stand). Se dibujan con `RenderManager.doRenderEntity` (partialTicks 0, sin hitbox), con `pointedEntity = null` y sombras apagadas (se restauran). El resto del aislamiento es el de `EntidadesBloque`.

Limitación: una región solo con un armor stand y sin bloques no llega aquí, porque `ValidacionMundo` busca bloques que no sean aire y la rechaza antes. El `ResumenGeometria.vacio()` sí cuenta las decorativas.

### 5.7 Supermuestreo, `ConversorImagen` y alfa premultiplicado
`aImagen` elige `factor = 2` si `res·2 ≤ máximo de la GPU`, si no 1 (con aviso). `factorSeguro` baja el factor si el heap o la VRAM (si se conoce) no alcanzan, con un `System.gc()` y nueva medición antes de degradar; si ni con factor 1 cabe, lanza con mensaje. Si la construcción del FBO o la memoria fallan con factor 2, se repite a 1 y se avisa. La escena es un `Runnable` que puede ejecutarse dos veces: no debe tener efectos fuera de OpenGL.

El FBO guarda color **premultiplicado** (`rgb·a`, `a`):

- Opacas: `blend (SRC_ALPHA, ZERO ; ONE, ZERO)` (el color queda multiplicado por su alfa, el alfa se copia).
- Translúcidas: `blend (SRC_ALPHA, ONE_MINUS_SRC_ALPHA ; ONE, ONE_MINUS_SRC_ALPHA)`: el alfa acumulado es `src + dst·(1−src)`, correcto incluso sobre fondo transparente (RF-011).

`reducirFranja` suma por bloque `factor×factor` el alfa y el rgb premultiplicado, y des-premultiplica: `canal = Σ(c·a) / Σa`, alfa = media. Así el color del borde es el de la estructura, nunca mezclado con el rgb del fondo transparente (sin halos), y alfa 0 produce exactamente `0x00000000`. La imagen sale con el eje Y invertido respecto a GL. Las franjas son de `128·factor` filas (múltiplo del factor).

### 5.8 `RenderPrueba` (orquestación)
Es el nexo: guardia, construcción controlada, dibujo, nombres de archivo y mensajes. Funciones: `exportar` (PNG real en `carpetaSalida`), `ejecutar` y `ejecutarMulti` (pruebas, en `isorender-test`), `exportarItem` / `escenaItem` (ítem como en el inventario: proyección con Y invertida y `scale(1,−1,1)` replicando `RenderItem`, luz de GUI, ignora la cámara del menú), `renderSilencioso` (estrés) y `dibujarEscena` (compartida con la vista previa). El nombre de la clase es histórico (prototipo de T-011).

### 5.9 `VistaPrevia` y `VistaPreviaCalculo`
Máquina de estados `SIN_REGION → PREPARANDO → LISTA` (y `ERROR`):

- Valida al abrir, cada 20 fotogramas y cuando cambia la región completa (tamaño máximo, chunks, solo aire).
- La construcción de la geometría se difiere un fotograma para poder pintar «Preparando vista previa…». Usa `RenderPrueba.construirControlado` (misma comprobación de memoria).
- El FBO propio se repinta solo si cambia la cámara, el tamaño o queda un reordenado de translúcidas pendiente. Se muestra como un quad con `blendFunc(ONE, ONE_MINUS_SRC_ALPHA)` (premultiplicado) sobre un fondo (tablero de ajedrez dibujado en un solo lote, o color liso). V invertida porque el FBO tiene el origen abajo.
- **Modo simplificado:** volumen > 32.768 bloques (tras el corte Y). Lado del FBO limitado a 256 px (512 en normal; mínimo 64; se calcula como recuadro × escala de GUI), no dibuja entidades de bloque ni decorativas, y reordena las translúcidas como mucho cada 300 ms.
- En error no reintenta cada fotograma: solo si cambian región o cámara. Los fallos quedan en el recuadro; el menú no se cierra.
- `PantallaMenu` crea la `VistaPrevia` en el constructor (no en `initGui`, que se repite al redimensionar) y la cierra en `onGuiClosed`.

### 5.10 `EstadoGrafico`
Único punto que captura y restaura el estado gráfico que toca el mod, al estado **previo real** (leído con `glGet*`/`glIsEnabled`), no a un valor «vanilla» supuesto. Tres alcances:

| Alcance | Qué guarda | Uso |
|---|---|---|
| `capturarCompleto` | banderas, funciones, texturas por unidad, framebuffer, viewport, scissor, matrices (y profundidad de pilas), alineación de lectura, AO y hojas, `glGetError` | Render offscreen y repintado de la vista previa |
| `capturarLigero` | solo banderas y unidades de textura | Dibujo dentro del mundo o la GUI (contorno, quad de la vista previa) |
| `capturarAjustes` | solo AO y hojas del jugador | Construcción de geometría |

`restaurar()` nunca lanza; en el alcance completo devuelve el primer error GL pendiente (y vacía la cola) para que el llamador avise. Restaura mediante `GlStateManager` para mantener coherente su caché; lo que `GlStateManager` no cachea (framebuffer, viewport, matrices, scissor, ancho de línea, alineación) va directo a GL. Iguala la profundidad de las pilas de matrices (un renderer que falló a medias puede dejar `push` sin `pop`), y escribe el color también directo con `glColor4f` porque tras dibujar con arrays de vértices el color actual de GL es indefinido.

### 5.11 `Guardia`, `Fallos`, `EstimacionMemoria`, `Chat`
- `Guardia`: `AtomicBoolean` con `entrar/salir`; un solo render o estrés a la vez.
- `Fallos.mensaje`: traduce `OutOfMemoryError`, `IOException`, `IllegalArgumentException`/`IllegalStateException` con texto en español y cualquier otra cosa a «Error inesperado…». `informar` imprime la traza en consola y manda el mensaje corto al chat; nunca lanza.
- `EstimacionMemoria`: cotas conservadoras (no mediciones) con margen del 25 %: geometría `≈ (3·ini + max(64 KB, ini/8))·2` con `ini = min(volumen·64, 64 MB)`; dibujo `12·res² + lado·128·factor·4`; VRAM `lado²·8`. El heap libre es `máximo − usado`.
- `Chat`: único punto de salida al chat, con el prefijo `[IsoRender] `. `archivo` añade la ruta una sola vez, subrayada y con `ClickEvent OPEN_FILE`. Nunca lanza.

### 5.12 `ContornoSeleccion`
Dibuja las 12 aristas de la caja de selección en `onPostRender`, con cámara del mundo ya puesta, compensando la posición interpolada del observador. Dos pasadas: una tenue sin test de profundidad (visible tras el terreno) y otra nítida con profundidad. Margen de 0,002 para evitar z-fighting. Usa `capturarLigero`. Si lanza, se desactiva hasta reiniciar y avisa una vez.

## 6. Decisiones de diseño

| Decisión | Motivo |
|---|---|
| No usar `glPushAttrib/glPopAttrib` | Desincronizan la caché de `GlStateManager` (dejaron el mundo en negro, T-014). Se captura el estado real y se restaura por `GlStateManager` (`EstadoGrafico`). |
| Reenlazar el FBO tras `framebufferClear()` | En 1.12.2 termina con `unbindFramebuffer()`; sin reenlazar se dibujaba y leía en el framebuffer principal (T-018). |
| Display lists en vez de VBO | Ver §5.3: reutilizan exactamente el camino de vanilla y no dejan estado GL pendiente. |
| Geometría construida una vez, dibujo repetible | La vista previa y el render comparten `GeometriaCacheada`; mover la cámara no relee el mundo. Es la base de RNF-003. |
| Encuadre automático por las 8 esquinas | Garantiza que cualquier rotación/elevación cabe con margen del 5 % (CA-010.1). En perspectiva se añade la cota de la esfera para no dejar esquinas tras la cámara. |
| Luz plena y AO forzado | El PNG es reproducible (no depende de la hora ni del lightmap) y se parece al aspecto del juego. Se apaga el lightmap con `entityRenderer.disableLightmap()`. |
| Alfa premultiplicado de extremo a extremo | Evita halos de color en los bordes (RNF-004) y da alfa acumulado correcto en translúcidas. |
| Supermuestreo 2× en lugar de MSAA | El antialiasing de bordes funciona sin MSAA y con alfa; coste: 4× píxeles y VRAM. |
| Solo lectura del mundo | RNF-013: `getBlockState`, `getLoadedChunk`, `getEntitiesWithinAABB`; nunca `setBlockState`, `getTileEntity` (podría crear) ni movimiento de entidades. Los ítems de la mano se copian (`stack.copy()`). |
| Comandos por `OutboundChatFilter` | Único mecanismo válido en LiteLoader; trae como coste la ausencia de Tab. |
| `GuiSlider` propio (`Deslizador`) | `GuiSlider` es de Forge, que no se usa. |
| Configuración en `.properties` con UTF-8 estricto | Sin dependencias; un archivo con bytes inválidos se detecta (decoder estricto) en vez de leerse mal. |
| Un render a la vez (`Guardia`) | Evita solapar FBO, display lists y estado GL. |
| `Subcomando` en `seleccion` | Análisis puro de la línea de comandos, probado sin Minecraft; también reconoce los comandos de prueba. |

## 7. Lecciones y trampas (gotchas)

1. **Estado GL desincronizado (T-014).** `glPushAttrib/glPopAttrib` crudos descuadran la caché de `GlStateManager`: la siguiente llamada de vanilla «cree» un estado que ya no es el real y el mundo se veía en negro. Regla: capturar con `glGet*` y restaurar con `GlStateManager` (`EstadoGrafico`).
2. **FBO desenlazado por `framebufferClear()` (T-018, T-011).** El prototipo dibujó sobre el framebuffer de la ventana (render solapado, fondo opaco). Hay que reenlazar tras el clear. Además `glClear` respeta `colorMask`, `depthMask` y el scissor heredados del juego; `enlazarYLimpiar` los fija antes.
3. **Color actual tras arrays de vértices.** Es indefinido; `EstadoGrafico` escribe `glColor4f` directo además de `GlStateManager.color`.
4. **Texturas y lightmap.** Tras dibujar entidades hay que volver a enlazar el atlas de bloques y apagar el lightmap (unidad 1); `restaurarEstado` y los `finally` lo hacen.
5. **Renderers a medias.** Un renderer que falla puede dejar pilas de matrices sin `pop` o el `Tessellator` en medio de un `draw`: se iguala la pila y se llama a `finishDrawing()` capturando `IllegalStateException`.
6. **Orden de translúcidas en ortográfica.** No sirve ordenar por distancia al ojo (vanilla): hay que ordenar por profundidad del eje de vista.
7. **Entidades de bloque:** no usar `World.getTileEntity` (puede crear); no comprobar distancia al jugador (usar la sobrecarga de 7 argumentos de `render`); pasar `cameraHitResult = null` para ocultar rótulos.
8. **Mappings.** Los sources de LiteLoader vienen sin desofuscar (SRG): en el código se usan los nombres MCP (`mc.world`, `getKeyDescription()`).
9. **`isPressed()` se consume siempre** en `onTick` aunque no se abra el menú, para no acumular pulsaciones.
10. **`ParametrosCamara.crear` vs `limitado`:** comandos y configuración rechazan o avisan; el menú recorta. No mezclarlos.
11. **Ítem en la mano:** `RenderItem.renderItem` con el atlas en `setBlurMipmap(false, false)` para pixeles nítidos; se restaura con `restoreLastBlurMipmap` en el `finally`.

## 8. Configuración y persistencia

- Archivo: `<directorio del juego>/liteconfig/common/isorender.properties` (el `configPath` que LiteLoader pasa a `init`). Formato `java.util.Properties`, UTF-8 estricto.
- `Configuracion` (inmutable): `carpetaSalida`, `resolucion`, `camara` (`ParametrosCamara`), `fondo`, `tamanoMaximo`. `leer` valida cada clave por separado: lo inválido toma el valor de fábrica y añade un aviso; `p.load` fallido (escape `\u` malformado) devuelve todos los valores por defecto. Los desplazamientos se guardan en % (la cámara los guarda como fracción).
- `ArchivoConfiguracion.cargar` crea el archivo si no existe; `guardar` escribe en `.tmp` y renombra (si el renombrado falla, escribe directo) y crea el directorio. Ambos nunca lanzan: los fallos van al log (`LOG::warn`).
- `Ajustes.actual()` es la configuración vigente global (`volatile`); `Ajustes.resolverCarpeta` resuelve `carpetaSalida` contra `mc.mcDataDir` (vacío → `isorender`).
- El menú persiste cámara, vista, resolución y fondo al cerrarse (`EstadoCamara.aConfiguracion`); conserva `carpetaSalida` y `tamanoMaximo`. El corte Y no se persiste.
- Claves, rangos y valores por defecto: ver `docs/MANUAL.md` §9 (la lista autorizada es `Configuracion.leer/escribir`).

## 9. Empaquetado, construcción y pruebas

- **Construir y probar:** ver `docs/ENTORNO.md` (versiones fijadas, `./gradlew setupDecompWorkspace`, `./gradlew build`, instalación en el cliente de pruebas y el problema conocido de `ZipException` con la caché limpia). No se duplica aquí.
- **Pruebas unitarias:** JUnit 4.12, unas 150 pruebas en `src/test/java`, solo de lógica pura (selección, validaciones, cámara, nombres, configuración, conversión de imagen, memoria, orden de translúcidas, parámetros del menú). Una clase: `./gradlew test --tests com.judamar.isorender.seleccion.SubcomandoTest`. Lo que toca OpenGL o el juego se verifica a mano con `docs/CASOS-PRUEBA.md`.
- **Paquete:** un solo `.litemod` (`jar { extension = 'litemod'; from('LICENSE') }`), con las clases de `com.judamar.isorender`, `litemod.json` (manual, en `src/main/resources`) y `LICENSE` (MIT). No se empaquetan dependencias ni recursos de terceros (ver `docs/LICENCIAS.md`).
- **`validarLitemod`** (enganchada a `build`): falla si el paquete no contiene `litemod.json` y `LICENSE`, si la versión de `litemod.json` difiere de la del proyecto o si `mcversion` no es `1.12.2`.
- **Versión:** está en `build.gradle` (`version`), `litemod.json` y como literal en `LiteModIsoRender.getVersion()`. `validarLitemod` solo compara las dos primeras: al subir de versión hay que cambiar las tres.
- **Clase de entrada:** LiteLoader la descubre por prefijo de nombre (`LiteMod*`); el descriptor no la declara.

## 10. Cómo añadir una función

Sigue `CLAUDE.md`: una tarea del plan a la vez, leer el requisito y sus criterios Dado/Cuando/Entonces, lógica pura con prueba, commit con el ID de tarea, y las tareas con 👁 esperan la aprobación del solicitante tras entregar la lista exacta de qué comprobar en el juego.

| Quiero… | Dónde |
|---|---|
| Un subcomando nuevo | Añadir el valor a `Subcomando` (y su caso en `parsear`, con argumentos solo si procede) y su prueba; atenderlo en el `switch` de `LiteModIsoRender.onSendChatMessage`; si dibuja, diferirlo a `onRender` (nuevo valor de `pendiente`) y respetar la `Guardia`; actualizar el mensaje «Uso: …» y `docs/MANUAL.md` §6. |
| Un ajuste nuevo de cámara | Campo en `ParametrosCamara` (`crear`, `limitado`, `equals`, `hashCode`), valor en `Parametro`, fila en `PantallaMenu` (cuidado con la altura de filas, ver el comentario de `fila`), clave en `Configuracion.leer/escribir` y pruebas. |
| Un tipo de entidad o bloque especial | Entidades de bloque: `EntidadesBloque.recoger`. Entidades de mundo: `EntidadesDecorativas.recoger` (con `instanceof` explícito). Mantener solo lectura, aislamiento try/catch y `restaurarEstado`. |
| Un paso nuevo de dibujo | En `RenderPrueba.dibujarEscena`, en el sitio correcto del orden (opacas → entidades → translúcidas) y con el estado GL fijado y restaurado dentro del paso. Todo dibujo fuera de pantalla debe pasar por `EstadoGrafico`. |
| Una clave de configuración | `Configuracion` (campo, `leer`, `escribir`, `equals`, `hashCode`), comentario de cabecera del archivo, pruebas en `ConfiguracionTest` y `docs/MANUAL.md` §9. |
| Un mensaje al usuario | `Chat.decir`, siempre en español; los errores recuperables como `IllegalArgumentException`/`IllegalStateException` con texto en español (`Fallos.mensaje` los reenvía tal cual). |
| Cambiar el límite de 64³ | Es una decisión de alcance (ERS RN-003, RNF-014): no cambiar en silencio. |

## 11. Supuestos vigentes

Marcados `[SUPUESTO]` en el ERS o en el código. Los marcados «código» están comentados en el fuente. Sigue vigente la regla de no resolverlos en silencio.

| # | Supuesto | Dónde |
|---|---|---|
| 1 | El mod es solo de cliente y trabaja con los chunks cargados, en un jugador o en servidor. | ERS §6.1; `ValidacionMundo` |
| 2 | Los puntos se fijan con `/isorender pos1`/`pos2` sobre el bloque apuntado (P-08). | ERS §6.1 |
| 3 | «Donde está de pie» = el bloque sobre el que se apoya el jugador (`floor(posY) − 1`). | código: `LiteModIsoRender.fijarPunto` |
| 4 | Región máxima 64 × 64 × 64 (P-09); resolución 512–4096 px; imagen cuadrada. | ERS RN-003, RN-006; código: `ValidacionRegion.MAX` |
| 5 | Rangos y valores por defecto de cámara (rotación 0–359 / 45°, elevación 5–90 / 35,264°, zoom 25–400 % / 100 %, desplazamiento ±100 %). | ERS RN-007; `ParametrosCamara` |
| 6 | Comando `/isorender`, tecla K y textos en español. | ERS §6.1, RNF-011 |
| 7 | Margen de encuadre del 5 %. | ERS CA-010.1; `ParametrosCamara.MARGEN` |
| 8 | Las caras del borde de la región se dibujan; las conexiones de vallas/paneles hacia fuera se cortan. | ERS RF-009; código: `VistaRegion` |
| 9 | Fondo siempre transparente (sin fondo opaco en la v1). | ERS RN-005 |
| 10 | Solo se garantizan bloques vanilla; los de otros mods, de mejor esfuerzo. LiteLoader sobre Forge no se verificó (CF-03). | ERS RN-010, CF-03 |
| 11 | Presets de cámara (idea del analista); «frontal» = elevación mínima permitida (5°), no 0°. | ERS RF-022; código: `PresetCamara` |
| 12 | Se recuerda el último ajuste (cámara, vista, resolución, fondo) al cerrar el menú. | ERS RF-029 |
| 13 | RF-027 (mensajes en el chat) sube a Must; el render de ítem individual (RF-030) baja a Could. | ERS §6.1, RF-027 |
| 14 | Corte por altura: solo Y máxima inclusiva; se ignora si está fuera de la región; el encuadre y el nombre usan la caja recortada; no se guarda y Restablecer no lo borra. | ERS RF-015 |
| 15 | Ítem suelto: si la mano principal está vacía se usa la secundaria; «bloque por ID» = su forma de ítem; ignora la cámara del menú y usa la resolución del menú. | ERS RF-030; código: `renderItem` |
| 16 | Perspectiva: FOV vertical fijo de 30°; el zoom actúa como ventana; afecta también al PNG; el valor por defecto sigue siendo isométrico. | ERS RF-033; código: `ParametrosCamara.FOV_GRADOS` |
| 17 | Un armor stand entra si su bloque base está dentro de la región; la vista previa simplificada omite entidades; el PNG siempre las dibuja. | ERS RF-034 |
| 18 | Umbrales de rendimiento y estabilidad: ≥15 fps (16³) y ≥5 fps (64³) en la vista previa; 32³ a 2048 px en ≤10 s de promedio (≤15 s máximo) en el equipo de referencia (GPU de 4 GB, 16 GB de RAM); memoria ±5 % tras 30 renders; 64³ sin `OutOfMemoryError` con 2 GB; menú usable de 854×480 a 1920×1080. | ERS RNF-002, RNF-003, RNF-009, RNF-012, RNF-014 |
| 19 | Render en lote (RF-031) fuera de la v1. | ERS RF-031 |
| 20 | Ejecutor: Claude Code, con el solicitante como supervisor y probador; las horas del plan son equivalentes humanos (P-06). | ERS §6.1; PLAN §1 |

Nota: el ERS §15 exige que el solicitante valide los supuestos; el solicitante aprobó las pruebas funcionales (T-034) pero el ERS no tiene un registro explícito de validación de cada supuesto.

### 11.1 Preguntas abiertas del ERS §13

Resueltas: P-02 (LiteLoader obligatorio), P-05 (licencia MIT), P-11 (entidades decorativas: sí, T-044) y P-15 (ítem suelto: sí, T-042). El resto sigue sin cierre formal en el ERS y el código aplica el supuesto indicado:

| ID | Pregunta | Supuesto aplicado |
|---|---|---|
| P-01 | Uso de las imágenes | Contenido y portafolio. El ERS dice resolución por defecto 1024 px; el código usa **512** |
| P-03 | ¿Solo bloques vanilla o también de mods? | Solo vanilla garantizado |
| P-04 | Resolución máxima y ángulo isométrico por defecto | 512–4096 px; 45° y 35,264° |
| P-06 | Fecha, presupuesto y equipo (bloqueante para el plan) | Ejecuta Claude Code; sin fecha ni presupuesto |
| P-07 | Idioma de menú y chat | Español |
| P-08 | Mecanismo de selección (comandos, varita, clic) | Comandos sobre el bloque apuntado |
| P-09 | Tamaño máximo y chunks cargados | 64³ y solo chunks cargados |
| P-10 | Un jugador, servidores o ambos | Ambos, como mod de cliente |
| P-12 | Vista previa en tiempo real o botón «Actualizar» | Tiempo real, con modo simplificado |
| P-13 | Otros ajustes del menú (iluminación, margen, recorte, fondo) | Rotación, elevación, zoom, desplazamiento, resolución (más fondo y vista, pedidos después) |
| P-14 | Imagen cuadrada o recortada a la silueta | Cuadrada |
| P-16 | Equipo de referencia para rendimiento | GPU de 4 GB, 16 GB de RAM |

`CLAUDE.md` indica como pendientes del solicitante P-06, P-08, P-09, P-12 y P-13 (antes de la épica E3, que ya se construyó con los supuestos). Los conflictos CF-01 a CF-05 tienen resolución provisional.

## 12. Notas de coherencia detectadas al redactar

Diferencias entre documentos y código (el código manda en este documento y en el manual):

- ERS §11.2 indica `/isorender render [resolución]`, resolución por defecto 1024 y una clave `margen` en la configuración. En el código `render` no admite argumentos (con argumentos cae en el mensaje de uso), la resolución por defecto es 512 (`ResolucionSalida.POR_DEFECTO`) y el margen es la constante `ParametrosCamara.MARGEN`, sin clave de configuración.
- ERS §11.2 sitúa el archivo «en el directorio de configuración de LiteLoader»: es `liteconfig/common/isorender.properties`.
- `RenderPrueba` tiene un comentario Javadoc huérfano («True si hay un render… en curso») justo antes de `regionConCorte`, que debería estar sobre `ocupado()`.
- La versión 0.1.0 está en tres sitios (ver §9) y `validarLitemod` solo comprueba dos.
- `build.gradle` fija `archivesBaseName = 'IsoRender'`, pero el artefacto documentado y presente en `build/libs` se llama `mod-isorender-0.1.0-mc1.12.2.litemod` (lo nombra el plugin de LiteLoader).
