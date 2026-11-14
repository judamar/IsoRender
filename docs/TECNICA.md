# IsoRender technical documentation (T-039, RNF-006)

For people who maintain or extend the mod. It complements `docs/ERS.md` (what), `docs/PLAN.md` (how and when), `docs/ENTORNO.md` (toolchain and build), `docs/LITELOADER-API.md` (verified LiteLoader API), `docs/LICENCIAS.md` and `docs/CASOS-PRUEBA.md`. End users should read `docs/MANUAL.md`. The internal project documents (ERS, PLAN, ENTORNO, LITELOADER-API, LICENCIAS, CASOS-PRUEBA) and the code comments are in Spanish; everything the mod shows to the player is in English.

Everything in this document has been checked against the code in `src/main/java/com/judamar/isorender/` (version 1.0.0). Class and method names are Spanish identifiers; the command names, configuration keys and file names the user sees are English. Mapping for the command enum: `Subcomando.PRUEBA` = `test`, `PRUEBA_MULTI` = `test-multi`, `ESTRES` = `stress`.

## 1. Overview

A client-side mod for Minecraft 1.12.2 loaded with LiteLoader (no Forge), Java 8, MCP mappings `snapshot_20170624`, fixed-function OpenGL with LWJGL 2. Given a region (two points) it produces a square RGBA PNG with a transparent background. The engine draws the region into its own framebuffer object (FBO), reads it back in strips and downsamples it with supersampling. A preview in the menu reuses the same engine with another FBO.

Rules that shape the whole design (from `CLAUDE.md`): the world is read-only; a selection, render or menu failure does not close the game (it is reported in the chat and the graphics state is restored); user-facing text is in English; pure logic is kept apart from the logic that touches Minecraft so that it can be tested with JUnit.

## 2. Architecture by package

Root: `com.judamar.isorender`.

| Package | Responsibility | Key classes |
|---|---|---|
| (root) | Entry point and orchestration | `LiteModIsoRender` |
| `seleccion` | Selection state, command parsing and region validation | `Seleccion`, `Subcomando`, `ValidacionRegion` (pure), `ValidacionMundo` (uses `World`) |
| `configuracion` | Configuration values, read/write and global access | `Configuracion` (pure), `ArchivoConfiguracion` (I/O), `Ajustes` |
| `menu` | Menu screen and the camera state it edits | `PantallaMenu`, `Deslizador`, `EstadoCamara`, `Parametro`, `PresetCamara`, `TextoSeleccion` |
| `render` | Render engine, preview, PNG, messages and robustness | see §2.1 |

### 2.1 Classes of the `render` package

| Group | Classes | Role |
|---|---|---|
| Camera | `ParametrosCamara` (pure), `CamaraGL` | Ranges, automatic framing and perspective distance; applying the projection and modelview |
| Geometry | `GeometriaCacheada`, `VistaRegion`, `GeometriaRegion` (pure), `OrdenTranslucido` (pure), `ResumenGeometria` (pure) | Reading the world once, per-layer display lists, translucent ordering |
| Entities | `EntidadesBloque`, `EntidadesDecorativas`, `ResumenEntidades` (pure) | Block entities and item frames / paintings / armor stands |
| Image output | `FramebufferRender`, `ConversorImagen` (pure), `ResolucionSalida` (pure), `NombreArchivo` (pure) | FBO, strip readback, reduction to ARGB and PNG, unique names |
| Orchestration | `RenderPrueba`, `Estres`, `ResumenEstres` (pure), `ArgumentoItem` (pure) | Flow of a full render, item render, stress tests |
| Preview | `VistaPrevia`, `VistaPreviaCalculo` (pure), `FondoVista` | Own FBO, states, simplified mode |
| Robustness | `EstadoGrafico`, `Guardia` (pure), `Fallos` (pure except `informar`), `EstimacionMemoria` (pure), `Chat` | GL state capture/restore, mutual exclusion, errors, memory, chat output |
| World | `ContornoSeleccion` | Selection box in the world |

"Pure" = no Minecraft or OpenGL classes; these are the ones covered by the unit tests (§9).

### 2.2 Dependencies between packages

`LiteModIsoRender` knows all of them. `configuracion.Configuracion` depends on `render` types (`ParametrosCamara`, `FondoVista`, `ResolucionSalida`) and on `seleccion.ValidacionRegion` (the `MAX` constant). `render.VistaPrevia` uses `menu.TextoSeleccion`, `seleccion.*` and `configuracion.Ajustes`. There are dependencies in both directions (`configuracion` → `render` and `render` → `configuracion`), so the packages are not strict layers: keep that in mind when moving classes.

## 3. Lifecycle

Everything runs on Minecraft's client thread (which is also the render thread). The `volatile` fields of `LiteModIsoRender` are defensive.

```
 startup
   LiteLoader instantiates LiteModIsoRender (a LiteMod* class, discovered by name)
   init(configPath): registers the KeyBinding "Open IsoRender menu" (K, category IsoRender),
                      loads liteconfig/common/isorender.properties, seeds EstadoCamara and Ajustes

 every frame
   onTick (Tickable)            greeting on entering a world; reads the K key; opens PantallaMenu
        │                       if (key or abrirMenu) and no other screen is open
   [chat] OutboundChatFilter    onSendChatMessage: Subcomando.parsear; sets flags/slots and returns false
        │                       (cancels sending to the server); pos1/pos2/clear/usage are handled right here
   onRender (RenderListener)    BEFORE the frame is drawn: runs the deferred work, in this order:
        │                         1. renderMenu  → renderDesdeMenu
        │                         2. stress in progress → one step per frame
        │                         3. pending (test, test-multi, stress, item)
   [world, game GUI]
   onPostRender (PostRenderListener)  with the world camera already set: ContornoSeleccion.dibujar
```

Why the work is deferred to `onRender`:

- The chat filter runs inside `EntityPlayerSP.sendChatMessage`, not in the draw loop; the offscreen render needs a GL context at a controlled point of the frame. Also `GuiChat` closes the screen after sending, so `/isorender menu` sets a flag that `onTick` handles on the next tick (so `mc.currentScreen == null`).
- There is a single `pendiente` slot and the `Guardia` of `RenderPrueba`: a second command is rejected with `Chat.OCUPADO` ("A render is already running or pending; wait for it to finish.") instead of overwriting the first one. `/isorender render` uses its own flag (`renderMenu`) and checks the same busy set.
- `Estres` keeps the guard between frames and does one render per frame so the game does not freeze.

Commands: LiteLoader offers no client commands (`ServerCommandProvider` is server-side and there is no `ClientCommandHandler`); `OutboundChatFilter` is the valid mechanism and that is why there is no Tab completion (see `LITELOADER-API.md` §4). Parsing is case-sensitive: only lowercase `/isorender`.

Menu: `PantallaMenu` is a normal `GuiScreen` opened with `displayGuiScreen`; it does not pause the game. `EstadoCamara` lives in the mod (not in the screen) so that camera, background, resolution and Y cut survive closing and reopening. `onGuiClosed` calls `guardarConfiguracion` (which compares with `ultimoGuardado` and does not write if nothing changed).

## 4. Render flow of a PNG

```
 [Render]  /  /isorender render
        │
        ▼
 PantallaMenu.actionPerformed ─ displayGuiScreen(null) ─► onGuiClosed ─► guardarConfiguracion
        │ alRenderizar.run()  ⇒  renderMenu = true
        ▼                                                          (next frame)
 LiteModIsoRender.onRender ─► renderDesdeMenu
        │   ValidacionMundo.validar(points, ≤ maxSize, chunks loaded, not only air)
        │   RenderPrueba.exportar(regionConCorte(region, cutY), resolution, camera)
        ▼
 RenderPrueba.render ── GUARDIA.entrar() (if busy: warning and end)
        │
        ├─(a) BUILD  construirControlado
        │        EstimacionMemoria.comprobarGeometria (one System.gc() and retry before rejecting)
        │        GeometriaCacheada.construir
        │          · EstadoGrafico.capturarAjustes + force smooth AO and "fancy" leaves
        │          · VistaRegion (air outside the box, max light) → renderBlock per block
        │            into 4 BufferBuilders (SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT)
        │          · translucent: the raw faces are copied (getRawBuffer)
        │          · EntidadesBloque.recoger + EntidadesDecorativas.recoger (read-only)
        │          · 3 opaque layers → 3 display lists (the translucent one is compiled when drawing)
        │
        ├─(b) DRAW  aImagen(res, scene)
        │        factor = factorSupermuestreo(res, GPU) → factorSeguro (heap and NVIDIA VRAM)
        │        EstadoGrafico.capturarCompleto  ← REAL previous state (glGet*)
        │        FramebufferRender(res·factor); enlazarYLimpiar (alpha 0, re-binds after the clear)
        │        dibujarEscena
        │          CamaraGL.aplicar (orthographic or frustum + automatic framing)
        │          pass 1  opaque: blend (SRC_ALPHA,0 ; 1,0)  → premultiplied color
        │          pass 1b block entities, then decorative ones (each with its own isolated state)
        │          pass 2  translucent: sorted back→front, premultiplied blend, depthMask(false)
        │        leerFranja × N (strips of 128·factor rows) → ConversorImagen.reducirFranja
        │        finally: fbo.close()  ·  previo.restaurar()  ·  glGetError → warning if any
        │
        ├─ warnings about skipped blocks/entities
        ├─ NombreArchivo.base + unico  →  ConversorImagen.guardarPng (creates the folder; deletes a partial PNG on failure)
        └─ Chat.archivo (clickable path)
        finally: geo.close() (deletes the display lists) · GUARDIA.salir()
        (any Throwable ⇒ Fallos.informar: stack trace to the console, short message to the chat)
```

The preview follows the same path from `dibujarEscena` on, with its own FBO and without going through `ConversorImagen` (§5.9).

## 5. Render points

Each point is commented in the code; here is the why.

### 5.1 `FramebufferRender`
A square FBO of `lado` px with depth, on top of `net.minecraft.client.shader.Framebuffer`. The constructor validates the GPU limit (`min(GL_MAX_TEXTURE_SIZE, GL_MAX_RENDERBUFFER_SIZE)`), clears foreign GL errors, creates the FBO, checks `glGetError` and `glCheckFramebufferStatus`, and if anything fails it calls `close()` before rethrowing (no half-built resources). It throws `IllegalStateException` with an English message (`Your graphics card does not support N px (maximum M px). Choose a lower resolution.`, `OpenGL error (0x…) while creating the N px framebuffer; possibly out of video memory.`, `Incomplete framebuffer (0x…) at N px; possibly out of video memory.`). `vramLibre()` uses `NVX_gpu_memory_info` (NVIDIA only; -1 = unknown). Readback is `glReadPixels` in strips so that `lado*lado*4` bytes are never reserved at once. `close()` deletes the FBO and re-binds `mc.getFramebuffer()`.

`enlazarYLimpiar()` re-binds the FBO after `framebufferClear()` (see §7, lesson 2) and first sets `colorMask` and `depthMask` and disables the scissor, because `glClear` honors them.

### 5.2 `CamaraGL` and `ParametrosCamara` (camera and framing)
`CamaraGL.aplicar` loads the projection and the modelview. Common modelview: `rotate(elevation, X) · rotate(rotation, Y) · translate(-region center)`.

- **Orthographic (isometric):** `halfWidth = baseHalfWidth · 100 / zoom`, with
  `baseHalfWidth = max over the 8 corners of max(|xp|, |yp|) / (1 − MARGEN)`, where `xp`, `yp` are each corner's screen coordinates after rotation and tilt and `MARGEN = 0.05`. The window is `ortho(cx−r, cx+r, cy−r, cy+r, −256, 256)` with `cx = −offX·r`, `cy = −offY·r` (the offset moves the structure, so the window moves the other way). Fixed ±256 planes: the maximum diagonal of 64³ (≈111) fits.
- **Perspective:** fixed vertical FOV `FOV_GRADOS = 30` (`t = tan(15°)`). The camera moves back `d` from the center, with
  `d = max( max over corners of [ c + max(|xp|,|yp|) / (t·(1−MARGEN)) ] , 0.5·diagonal + 1 )`,
  where `c = y·sin(e) + zp·cos(e)` is how close the corner is to the camera. The first bound makes the 8 corners fit with a margin; the second keeps the camera outside the sphere that encloses the region (no corner ends up behind it). Planes: `near = 0.9·(d − cMax)`, `far = 1.1·(d − cMin) + 1`. Zoom and offset do not move the camera: they scale and shift the frustum window on the near plane (`h = near·t·100/zoom`; `glFrustum(cx−h, cx+h, cy−h, cy+h, near, far)`, `cx = −offX·h`), and `translate(0,0,−d)`.

`ParametrosCamara` is immutable; `crear` validates (throws with a message that includes the range) and `limitado` clamps (menu controls). Ranges: rotation 0–359, elevation 5–90, zoom 25–400, offset ±1 (±100%).

### 5.3 `GeometriaCacheada`, `GeometriaRegion`, `VistaRegion`
The region is read **once**: `BlockRendererDispatcher.renderBlock(state, pos, view, layerBuffer)` for each block (with `getAllInBoxMutable`, a single reused `BlockPos`). `renderBlock` calls `getActualState`, so orientation, connections and halves come out right. Each block is in a try/catch: a mod block that fails is counted (`fallidos`) and skipped (RN-010). Unknown layers (from mods) are skipped.

`VistaRegion` implements `IBlockAccess` over the world: inside the box it delegates, outside it returns air (so edge faces are drawn and interior culling and ambient occlusion work), `getCombinedLight` always returns maximum light (same PNG by day and by night) and `getBiome` delegates (real tint). `getTileEntity` outside the box returns null. [SUPUESTO] fence and pane connections toward the outside of the region are cut.

While building, `EstadoGrafico.forzarAjustesGeometria()` sets `ambientOcclusion = 2` and leaves to "fancy" (deterministic layer); `restaurar()` always gives the player's settings back.

**Per-layer display lists** (SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT): each list replays `WorldVertexBufferUploader.draw(buffer)`. They were chosen over VBOs because it is the vanilla 1.12 non-VBO path (`ListedRenderChunk`): they keep the vertex, color, UV and lightmap pointers of `DefaultVertexFormats.BLOCK` with no extra code, the driver copies the data into its own memory, and there are no buffers or client GL state to bind or clean up. If a future measurement demands it, the change is local to this class. `close()` deletes the lists and is idempotent; `construir` deletes them if it fails halfway.

`GeometriaRegion` holds the pure logic: layer order, `contiene`, `volumen`, `bytesIniciales` (capped at 64 MB per layer), `recortar` (Y cut) and `parsearN`.

### 5.4 `OrdenTranslucido`
Translucent faces are stored raw and recompiled sorted back to front only when rotation, elevation or mode (isometric/perspective) change; zoom and offset do not alter the order. In orthographic mode the exact depth is the view-space z coordinate (`y·sin(e) + (−x·sin(r) + z·cos(r))·cos(e)`), not the distance to a point as vanilla's `sortVertexData` does (which is only right with a perspective camera). In perspective the order is by distance to the eye, computed in world coordinates. Faces are sorted by their centroid (fine for block faces) with TimSort (stable on ties).

### 5.5 `EntidadesBloque`
In 1.12 chests, beds, signs and heads have no static model (or only a partial one): vanilla uses a `TileEntitySpecialRenderer`. `recoger` walks the tile-entity map of the **already loaded** chunks (`getLoadedChunk`, which neither loads nor creates anything; `World.getTileEntity` could create a missing entity) and excludes global renderers (beacon, structure block), mob spawners (their renderer creates a cached entity) and moving pistons. `dibujar` does not use a display list (the renderers use GL state and animation): it calls `TileEntityRendererDispatcher.render` (the 7-argument version, with no distance-to-player check) with the active camera. Details:

- `cameraHitResult = null` so that no renderer draws the name label; `partialTicks = 0`.
- It translates to the region origin and passes relative coordinates (with large world coordinates the `float` would lose precision).
- Each entity is in a try/catch; after each one `restaurarEstado` equalizes the matrix stack depth, turns off item lighting and returns the state that the solid pass expects. The dispatcher's public fields are saved and restored.
- Lighting: before each entity vanilla's item lighting is enabled and `luzObjeto()` forces, through direct GL, `GL_COLOR_MATERIAL`, white color and ambient light 0.4. With `GlStateManager` alone, chests and beds came out far too dark (its cache did not reflect the real state and OpenGL's default material remained); confirmed in game.
- They are drawn after the solids and before the translucents.

### 5.6 `EntidadesDecorativas`
Only item frames, paintings and armor stands (RN-001/RF-034), checked with explicit `instanceof` so that a mod entity inheriting from `EntityHanging` does not get in. `recoger` uses `getEntitiesWithinAABB` (read-only) and filters by the anchor block (`getHangingPosition`) or the block at the armor stand's position. They are drawn with `RenderManager.doRenderEntity` (partialTicks 0, no hitbox), with `pointedEntity = null` and shadows off (restored afterwards). The rest of the isolation is the same as `EntidadesBloque`.

Limitation: a region with only an armor stand and no blocks never gets here, because `ValidacionMundo` looks for non-air blocks and rejects it first (`The region contains only air: there are no blocks to render.`). `ResumenGeometria.vacio()` does count the decorative entities.

### 5.7 Supersampling, `ConversorImagen` and premultiplied alpha
`aImagen` chooses `factor = 2` if `res·2 ≤ GPU maximum`, otherwise 1 (with a warning). `factorSeguro` lowers the factor if the heap or the VRAM (if known) is not enough, with a `System.gc()` and a new measurement before degrading; if even factor 1 does not fit, it throws with a message. If building the FBO or memory fails at factor 2, it is repeated at 1 and a warning is shown. The scene is a `Runnable` that can run twice: it must not have effects outside OpenGL.

The FBO stores **premultiplied** color (`rgb·a`, `a`):

- Opaque: `blend (SRC_ALPHA, ZERO ; ONE, ZERO)` (the color ends up multiplied by its alpha, alpha is copied).
- Translucent: `blend (SRC_ALPHA, ONE_MINUS_SRC_ALPHA ; ONE, ONE_MINUS_SRC_ALPHA)`: accumulated alpha is `src + dst·(1−src)`, correct even over a transparent background (RF-011).

`reducirFranja` sums, per `factor×factor` block, the alpha and the premultiplied rgb, and un-premultiplies: `channel = Σ(c·a) / Σa`, alpha = mean. This way the edge color is the structure's, never mixed with the rgb of the transparent background (no halos), and alpha 0 produces exactly `0x00000000`. The image comes out with the Y axis flipped relative to GL. Strips are `128·factor` rows (a multiple of the factor).

### 5.8 `RenderPrueba` (orchestration)
It is the hub: guard, controlled build, drawing, file names and messages. Functions: `exportar` (real PNG in the output folder), `ejecutar` and `ejecutarMulti` (the `test` and `test-multi` commands, writing to `isorender-test`), `exportarItem` / `escenaItem` (item as in the inventory: projection with inverted Y and `scale(1,−1,1)` replicating `RenderItem`, GUI lighting, ignores the menu camera), `renderSilencioso` (stress) and `dibujarEscena` (shared with the preview). The class name is historical (prototype of T-011).

### 5.9 `VistaPrevia` and `VistaPreviaCalculo`
State machine `SIN_REGION → PREPARANDO → LISTA` (and `ERROR`):

- It validates on opening, every 20 frames and whenever the complete region changes (maximum size, chunks, only air).
- Building the geometry is deferred by one frame so that "Preparing preview…" can be painted. It uses `RenderPrueba.construirControlado` (same memory check).
- The own FBO is repainted only if the camera, the size or a pending re-sort of translucents changes. It is shown as a quad with `blendFunc(ONE, ONE_MINUS_SRC_ALPHA)` (premultiplied) over a background (checkerboard drawn in a single batch, or a flat color). V is flipped because the FBO has its origin at the bottom.
- **Simplified mode:** volume > 32,768 blocks (after the Y cut). FBO side limited to 256 px (512 in normal mode; minimum 64; computed as box × GUI scale), it draws neither block nor decorative entities, and re-sorts translucents at most every 300 ms.
- On error it does not retry every frame: only if the region or the camera change. Failures are shown in the box; the menu does not close.
- `PantallaMenu` creates the `VistaPrevia` in the constructor (not in `initGui`, which repeats on resize) and closes it in `onGuiClosed`.

### 5.10 `EstadoGrafico`
The single point that captures and restores the graphics state the mod touches, back to the **real previous** state (read with `glGet*`/`glIsEnabled`), not to an assumed "vanilla" value. Three scopes:

| Scope | What it saves | Use |
|---|---|---|
| `capturarCompleto` | flags, functions, textures per unit, framebuffer, viewport, scissor, matrices (and stack depths), read alignment, AO and leaves, `glGetError` | Offscreen render and preview repaint |
| `capturarLigero` | only flags and texture units | Drawing inside the world or the GUI (outline, preview quad) |
| `capturarAjustes` | only the player's AO and leaves | Geometry building |

`restaurar()` never throws; in the full scope it returns the first pending GL error (and drains the queue) so the caller can warn. It restores through `GlStateManager` to keep its cache consistent; whatever `GlStateManager` does not cache (framebuffer, viewport, matrices, scissor, line width, alignment) goes straight to GL. It equalizes the matrix stack depths (a renderer that failed halfway can leave a `push` without a `pop`), and writes the color directly with `glColor4f` too, because after drawing with vertex arrays GL's current color is undefined.

### 5.11 `Guardia`, `Fallos`, `EstimacionMemoria`, `Chat`
- `Guardia`: an `AtomicBoolean` with `entrar/salir`; only one render or stress run at a time.
- `Fallos.mensaje`: translates `OutOfMemoryError` (`Not enough memory while <context>; use a smaller region or a lower resolution.`), `IOException` (`Could not save the file (<detail>). Check permissions and disk space.`), `IllegalArgumentException`/`IllegalStateException` (their own English message, shortened to 160 characters) and anything else into `Unexpected error while <context> (<ErrorType>). Details are in the game console.` `informar` prints the stack trace to the console and sends the short message to the chat; it never throws.
- `EstimacionMemoria`: conservative bounds (not measurements) with a 25% margin: geometry `≈ (3·ini + max(64 KB, ini/8))·2` with `ini = min(volume·64, 64 MB)`; drawing `12·res² + side·128·factor·4`; VRAM `side²·8`. Free heap is `max − used`. Message: `Not enough memory: about X MB are needed and only Y MB are free. Use a smaller region or a lower resolution.`
- `Chat`: the single chat output point, with the `[IsoRender] ` prefix. `archivo` appends the path once, underlined and with `ClickEvent OPEN_FILE`. It never throws.

### 5.12 `ContornoSeleccion`
Draws the 12 edges of the selection box in `onPostRender`, with the world camera already set, compensating for the viewer's interpolated position. Two passes: a faint one without depth test (visible behind terrain) and a sharp one with depth. A 0.002 margin avoids z-fighting. It uses `capturarLigero`. If it throws, it disables itself until restart and warns once.

## 6. Design decisions

| Decision | Reason |
|---|---|
| Do not use `glPushAttrib/glPopAttrib` | They desynchronize `GlStateManager`'s cache (they left the world black, T-014). The real state is captured and restored through `GlStateManager` (`EstadoGrafico`). |
| Re-bind the FBO after `framebufferClear()` | In 1.12.2 it ends with `unbindFramebuffer()`; without re-binding, drawing and reading happened in the main framebuffer (T-018). |
| Display lists instead of VBOs | See §5.3: they reuse exactly vanilla's path and leave no pending GL state. |
| Geometry built once, drawing repeatable | Preview and render share `GeometriaCacheada`; moving the camera does not re-read the world. It is the basis of RNF-003. |
| Automatic framing by the 8 corners | Guarantees that any rotation/elevation fits with a 5% margin (CA-010.1). In perspective the sphere bound is added so no corner is left behind the camera. |
| Full light and forced AO | The PNG is reproducible (it does not depend on time of day or the lightmap) and resembles the in-game look. The lightmap is turned off with `entityRenderer.disableLightmap()`. |
| Premultiplied alpha end to end | Avoids color halos at the edges (RNF-004) and gives correct accumulated alpha on translucent blocks. |
| 2× supersampling instead of MSAA | Edge antialiasing works without MSAA and with alpha; cost: 4× pixels and VRAM. |
| Read-only world | RNF-013: `getBlockState`, `getLoadedChunk`, `getEntitiesWithinAABB`; never `setBlockState`, `getTileEntity` (could create) or entity movement. Held items are copied (`stack.copy()`). |
| Commands through `OutboundChatFilter` | The only valid mechanism in LiteLoader; the cost is no Tab completion. |
| Own slider (`Deslizador`) | `GuiSlider` belongs to Forge, which is not used. |
| Configuration in `.properties` with strict UTF-8 | No dependencies; a file with invalid bytes is detected (strict decoder) instead of being misread. |
| One render at a time (`Guardia`) | Avoids overlapping FBOs, display lists and GL state. |
| `Subcomando` in `seleccion` | Pure parsing of the command line, tested without Minecraft; it also recognizes the debug commands. |

## 7. Lessons and gotchas

1. **Desynchronized GL state (T-014).** Raw `glPushAttrib/glPopAttrib` throw off `GlStateManager`'s cache: vanilla's next call "believes" in a state that is no longer the real one and the world turned black. Rule: capture with `glGet*` and restore with `GlStateManager` (`EstadoGrafico`).
2. **FBO unbound by `framebufferClear()` (T-018, T-011).** The prototype drew onto the window's framebuffer (overlapping render, opaque background). You must re-bind after the clear. Also `glClear` honors the `colorMask`, `depthMask` and scissor inherited from the game; `enlazarYLimpiar` sets them first.
3. **Current color after vertex arrays.** It is undefined; `EstadoGrafico` writes `glColor4f` directly as well as `GlStateManager.color`.
4. **Textures and lightmap.** After drawing entities the block atlas must be re-bound and the lightmap (unit 1) turned off; `restaurarEstado` and the `finally` blocks do it.
5. **Half-failed renderers.** A renderer that fails can leave matrix stacks without a `pop` or the `Tessellator` in the middle of a `draw`: the stack is equalized and `finishDrawing()` is called, catching `IllegalStateException`.
6. **Translucent ordering in orthographic mode.** Sorting by distance to the eye (vanilla) does not work: you must sort by depth along the view axis.
7. **Block entities:** do not use `World.getTileEntity` (it can create); do not check distance to the player (use the 7-argument overload of `render`); pass `cameraHitResult = null` to hide labels.
8. **Mappings.** LiteLoader's sources are not deobfuscated (SRG): the code uses MCP names (`mc.world`, `getKeyDescription()`).
9. **`isPressed()` is always consumed** in `onTick` even if the menu is not opened, so presses do not pile up.
10. **`ParametrosCamara.crear` vs `limitado`:** commands and configuration reject or warn; the menu clamps. Do not mix them.
11. **Held item:** `RenderItem.renderItem` with the atlas in `setBlurMipmap(false, false)` for sharp pixels; restored with `restoreLastBlurMipmap` in the `finally`.

## 8. Configuration and persistence

- File: `<game directory>/liteconfig/common/isorender.properties` (the `configPath` LiteLoader passes to `init`). Format `java.util.Properties`, strict UTF-8.
- `Configuracion` (immutable) holds `carpetaSalida` (key `outputFolder`), `resolucion` (`resolution`), `camara` (`ParametrosCamara`: keys `rotation`, `elevation`, `zoom`, `offsetX`, `offsetY`, `view`), `fondo` (`background`) and `tamanoMaximo` (`maxSize`). `leer` validates each key separately: an invalid value takes the factory default and adds a warning (`Configuration: "<key>=<value>" is not valid (<cause>); using the default value.`); a failed `p.load` (malformed `\u` escape) returns all defaults. Offsets are stored in % (the camera keeps them as a fraction). `view` and `background` are read case-insensitively; decimals accept a comma.
- `ArchivoConfiguracion.cargar` creates the file if it does not exist; `guardar` writes to `.tmp` and renames (if the rename fails, it writes directly) and creates the directory. Both never throw: failures go to the log (`LOG::warn`, logger name `IsoRender`).
- `Ajustes.actual()` is the current global configuration (`volatile`); `Ajustes.resolverCarpeta` resolves `outputFolder` against `mc.mcDataDir` (empty → `isorender`).
- The menu persists camera, view, resolution and background when it closes (`EstadoCamara.aConfiguracion`); it keeps `outputFolder` and `maxSize`. The Y cut is not persisted.
- Keys, ranges and defaults: see `docs/MANUAL.md` §9 (the authoritative list is `Configuracion.leer/escribir`).
- Output names (`NombreArchivo`): `structure_<yyyyMMdd-HHmmss>_<x1,y1,z1>_<x2,y2,z2>.png` and `item_<yyyyMMdd-HHmmss>_<id>_<meta>.png`, made unique with `_2`, `_3`... (`unico`). The debug commands write `test-<size>-<ms>.png` and `multi-<stamp>-<i>of<n>-rot<deg>.png` to `<game>/isorender-test/`.

## 9. Packaging, building and tests

- **Build and test:** see `docs/ENTORNO.md` (pinned versions, `./gradlew setupDecompWorkspace`, `./gradlew build`, installing in the test client and the known `ZipException` problem with a clean cache). It is not duplicated here.
- **Unit tests:** JUnit 4.12, about 150 tests in `src/test/java`, only for pure logic (selection, validations, camera, names, configuration, image conversion, memory, translucent ordering, menu parameters). One class: `./gradlew test --tests com.judamar.isorender.seleccion.SubcomandoTest`. Whatever touches OpenGL or the game is checked by hand with `docs/CASOS-PRUEBA.md`.
- **Package:** a single `.litemod` (`jar { extension = 'litemod'; from('LICENSE') }`) with the classes of `com.judamar.isorender`, `litemod.json` (manual, in `src/main/resources`) and `LICENSE` (MIT). No dependencies or third-party resources are packaged (see `docs/LICENCIAS.md`). Output: `build/libs/mod-isorender-1.0.0-mc1.12.2.litemod`.
- **`validarLitemod`** (hooked to `build`): fails if the package lacks `litemod.json` or `LICENSE`, if the version in `litemod.json` differs from the project's, or if `mcversion` is not `1.12.2`.
- **Version:** it lives in `build.gradle` (`version`), `litemod.json` and as a literal in `LiteModIsoRender.getVersion()`. `validarLitemod` only compares the first two: when bumping the version, change all three.
- **Entry class:** LiteLoader discovers it by name prefix (`LiteMod*`); the descriptor does not declare it.

## 10. How to add a feature

Follow `CLAUDE.md`: one plan task at a time, read the requirement and its Given/When/Then criteria, pure logic with a test, a commit with the task ID, and tasks marked with the eye icon wait for the requester's approval after delivering the exact list of what to check in the game.

| I want... | Where |
|---|---|
| A new subcommand | Add the value to `Subcomando` (and its case in `parsear`, with arguments only if applicable) and its test; handle it in the `switch` of `LiteModIsoRender.onSendChatMessage`; if it draws, defer it to `onRender` (new value of `pendiente`) and respect the `Guardia`; update the `Usage: ...` message and `docs/MANUAL.md` §6. |
| A new camera setting | Field in `ParametrosCamara` (`crear`, `limitado`, `equals`, `hashCode`), value in `Parametro`, row in `PantallaMenu` (mind the row height, see the comment on `fila`), key in `Configuracion.leer/escribir` and tests. |
| A special entity or block type | Block entities: `EntidadesBloque.recoger`. World entities: `EntidadesDecorativas.recoger` (with explicit `instanceof`). Keep it read-only, with try/catch isolation and `restaurarEstado`. |
| A new drawing step | In `RenderPrueba.dibujarEscena`, at the right place in the order (opaque → entities → translucent) and with the GL state set and restored inside the step. Any offscreen drawing must go through `EstadoGrafico`. |
| A configuration key | `Configuracion` (field, `leer`, `escribir`, `equals`, `hashCode`), the file's header comment, tests in `ConfiguracionTest` and `docs/MANUAL.md` §9. |
| A user message | `Chat.decir`, always in English; recoverable errors as `IllegalArgumentException`/`IllegalStateException` with an English message (`Fallos.mensaje` forwards them as they are). |
| Change the 64³ limit | It is a scope decision (ERS RN-003, RNF-014): do not change it silently. |

## 11. Active assumptions

Marked `[SUPUESTO]` in the ERS or in the code. Those marked "code" are commented in the source. The rule of not resolving them silently still applies.

| # | Assumption | Where |
|---|---|---|
| 1 | The mod is client-only and works with the loaded chunks, in single-player or on a server. | ERS §6.1; `ValidacionMundo` |
| 2 | Points are set with `/isorender pos1`/`pos2` on the aimed block (P-08). | ERS §6.1 |
| 3 | "Where they stand" = the block the player rests on (`floor(posY) − 1`). | code: `LiteModIsoRender.fijarPunto` |
| 4 | Maximum region 64 × 64 × 64 (P-09); resolution 512–4096 px; square image. | ERS RN-003, RN-006; code: `ValidacionRegion.MAX` |
| 5 | Camera ranges and defaults (rotation 0–359 / 45°, elevation 5–90 / 35.264°, zoom 25–400% / 100%, offset ±100%). | ERS RN-007; `ParametrosCamara` |
| 6 | Command `/isorender` and the K key. User-facing text in English (decided 2026-11-14 for a global release; the ERS RNF-011 and P-07 still say Spanish). | CLAUDE.md; ERS RNF-011 |
| 7 | Framing margin of 5%. | ERS CA-010.1; `ParametrosCamara.MARGEN` |
| 8 | Faces on the edge of the region are drawn; fence/pane connections toward the outside are cut. | ERS RF-009; code: `VistaRegion` |
| 9 | Background always transparent (no opaque background in v1). | ERS RN-005 |
| 10 | Only vanilla blocks are guaranteed; mod blocks are best-effort. LiteLoader on top of Forge was not verified (CF-03). | ERS RN-010, CF-03 |
| 11 | Camera presets (analyst's idea); "front" = the minimum allowed elevation (5°), not 0°. | ERS RF-022; code: `PresetCamara` |
| 12 | The last setting (camera, view, resolution, background) is remembered when the menu closes. | ERS RF-029 |
| 13 | RF-027 (chat messages) goes up to Must; the single-item render (RF-030) goes down to Could. | ERS §6.1, RF-027 |
| 14 | Height cut: inclusive maximum Y only; ignored if outside the region; framing and name use the cut box; not saved and Reset does not clear it. | ERS RF-015 |
| 15 | Single item: if the main hand is empty the off hand is used; "block by ID" = its item form; it ignores the menu camera and uses the menu resolution. | ERS RF-030; code: `renderItem` |
| 16 | Perspective: fixed 30° vertical FOV; zoom acts as a window; it also affects the PNG; the default remains isometric. | ERS RF-033; code: `ParametrosCamara.FOV_GRADOS` |
| 17 | An armor stand is included if the block at its position is inside the region; the simplified preview omits entities; the PNG always draws them. | ERS RF-034 |
| 18 | Performance and stability thresholds: ≥15 fps (16³) and ≥5 fps (64³) in the preview; 32³ at 2048 px in ≤10 s on average (≤15 s maximum) on the reference machine (4 GB GPU, 16 GB RAM); memory ±5% after 30 renders; 64³ without `OutOfMemoryError` with 2 GB; menu usable from 854×480 to 1920×1080. | ERS RNF-002, RNF-003, RNF-009, RNF-012, RNF-014 |
| 19 | Batch rendering (RF-031) is outside v1. | ERS RF-031 |
| 20 | Executor: Claude Code, with the requester as supervisor and tester; the plan's hours are human-equivalent (P-06). | ERS §6.1; PLAN §1 |

Note: ERS §15 requires the requester to validate the assumptions; the requester approved the functional tests (T-034) but the ERS has no explicit record of validation of each assumption.

### 11.1 Open questions of ERS §13

Resolved: P-02 (LiteLoader mandatory), P-05 (MIT license), P-11 (decorative entities: yes, T-044) and P-15 (single item: yes, T-042). The rest has no formal closure in the ERS and the code applies the assumption indicated:

| ID | Question | Assumption applied |
|---|---|---|
| P-01 | Use of the images | Content and portfolio; default resolution 512 px |
| P-03 | Only vanilla blocks or also mod blocks? | Only vanilla guaranteed |
| P-04 | Maximum resolution and default isometric angle | 512–4096 px; 45° and 35.264° |
| P-06 | Date, budget and team (blocking for the plan) | Claude Code executes; no date or budget |
| P-07 | Language of menu and chat | English (CLAUDE.md, 2026-11-14); the ERS still says Spanish |
| P-08 | Selection mechanism (commands, wand, click) | Commands on the aimed block |
| P-09 | Maximum size and loaded chunks | 64³ and loaded chunks only |
| P-10 | Single-player, servers or both | Both, as a client mod |
| P-12 | Real-time preview or "Update" button | Real time, with simplified mode |
| P-13 | Other menu settings (lighting, margin, crop, background) | Rotation, elevation, zoom, offset, resolution (plus background and view, requested later) |
| P-14 | Square image or cropped to the silhouette | Square |
| P-16 | Reference machine for performance | 4 GB GPU, 16 GB RAM |

`CLAUDE.md` lists P-06, P-08, P-09, P-12 and P-13 as pending from the requester (before epic E3, which was already built with the assumptions). Conflicts CF-01 to CF-05 have a provisional resolution.

## 12. Coherence notes found while writing

Differences between documents and code (the code wins in this document and in the manual):

- The ERS (Spanish) RNF-011 and P-07 still state that user-facing text is in Spanish; `CLAUDE.md` (2026-11-14) and the code use English.
- The version 1.0.0 lives in three places (see §9) and `validarLitemod` only checks two.
- `build.gradle` sets `archivesBaseName = 'IsoRender'`, but the documented artifact present in `build/libs` is called `mod-isorender-1.0.0-mc1.12.2.litemod` (the LiteLoader plugin names it).
- The "only air" check exists twice with different texts: `ValidacionMundo` says `The region contains only air: there are no blocks to render.` (no non-air block in the region) and `GeometriaCacheada` says `The region contains only air.` (blocks exist but nothing drawable came out of them). The second one is a safeguard in the geometry builder.
