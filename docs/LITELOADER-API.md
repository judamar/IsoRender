# API de LiteLoader 1.12.2 (T-008)

Fuente: `liteloader-1.12.2-SNAPSHOT-sources.jar` (paquete `com.mumfrey.liteloader`). Los sources vienen **sin desofuscar** (`field_71441_e`, `func_151464_g`...): en el código del mod se usan los nombres MCP (`mc.world`, `binding.getKeyDescription()`). Todo lo de abajo está verificado en esos sources; lo marcado [PRUEBA] solo se confirma en juego (T-009..T-011).

## 1. Descriptor `litemod.json` (raíz del .litemod)
Claves leídas por `LoadableMod.getMetaValue(...)` / `LiteLoaderMods` / `LiteLoaderEnumerator`:
`name` (id interno), `displayName`, `version`, `mcversion`, `revision`, `author`, `description`, `url`, `injectAt`, `tweakClass`, `classTransformerClasses`, `mixinConfigs`, `dependsOn`, `requiredAPIs`. El existente (`src/main/resources/litemod.json`) usa name, displayName, version, mcversion, author, description, revision: suficiente; no hace falta más.
La clase de entrada se **descubre por nombre**: `LiteLoaderCoreAPI.getModClassPrefix()` devuelve `"LiteMod"`, así que la clase debe llamarse `LiteMod*` (p. ej. `LiteModIsoRender`) y estar en el jar. El descriptor no declara la clase.

## 2. Entrada y ciclo de vida
`LiteMod extends Exposable, Listener`:
- `String getName()` (de `api.Listener`), `String getVersion()`.
- `void init(File configPath)`: una vez al cargar.
- `void upgradeSettings(String version, File configPath, File oldConfigPath)`: al cambiar la versión/revisión; puede quedar vacío.
Las capacidades se activan **implementando interfaces** (LiteLoader las registra por `InterfaceProvider`); no hay registro manual.
Extras: `InitCompleteListener.onInitCompleted(Minecraft, LiteLoader)` (todo listo, mejor sitio para registrar tecla), `ShutdownListener.onShutDown()`, `Configurable.getConfigPanelClass()`.
Rutas: `LiteLoader.getConfigFolder()`, `LiteLoader.getCommonConfigFolder()`, `LiteLoader.getInstance().registerExposable(Exposable, String)` (JSON de config con anotaciones `@Expose`).

## 3. Tecla
No hay `Keybind` propio: se usa `net.minecraft.client.settings.KeyBinding` + `LiteLoader.getInput().registerKeyBinding(binding)` (`util.Input`/`InputManager`; lo añade a Controles y persiste la tecla). Lectura: `binding.isPressed()` / `isKeyDown()` dentro de `Tickable.onTick`. Ej.: `new KeyBinding("Abrir menú IsoRender", Keyboard.KEY_K, "IsoRender")`. `unRegisterKeyBinding` existe.

## 4. Comandos de cliente
- `ServerCommandProvider.provideCommands(ServerCommandManager)` es para servidor/integrado: **no sirve** como comando de cliente.
- No existe `ClientCommandHandler` (es de Forge).
- **Mecanismo válido: `OutboundChatFilter.onSendChatMessage(String message)`** (`boolean`: `false` = cancela el envío). Verificado: `MixinEntityPlayerSP` inyecta en `sendChatMessage(String)` en HEAD y llama a `LiteLoaderEventBrokerClient.onSendChatMessage(CallbackInfo, String)`, que hace `e.cancel()` si algún filtro devuelve `false`. Recibe también las líneas que empiezan por `/`, por lo que `/isorender pos1` se intercepta en cliente y no llega al servidor. Parsear con `message.startsWith("/isorender")`.
- Alternativa solo-lectura: `OutboundChatListener.onSendChatMessage(CPacketChatMessage, String)` (no cancela). Entrantes: `ChatFilter.onChat(ITextComponent, String, ReturnValue<ITextComponent>)` / `ChatListener.onChat(...)` (no necesarios).
- Contra: no hay autocompletado con Tab [PRUEBA]; el mensaje no queda en el historial si se cancela (aceptable).
- Responder al usuario: `mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(...))`.

## 5. Pantalla propia
Sin API de LiteLoader: `Minecraft.getMinecraft().displayGuiScreen(new GuiScreen subclase)` desde el hilo de cliente (en `onTick`; desde el filtro de chat conviene diferir al siguiente tick, porque el chat se cierra tras enviar [PRUEBA]). `onRenderGui(GuiScreen)` de `RenderListener` sirve para saber la pantalla actual.

## 6. Hooks de render (todos en el hilo de render)
| Interfaz | Método | Momento (verificado en `LiteLoaderEventBrokerClient`) |
|---|---|---|
| `RenderListener` | `onRender()` | antes de renderizar el frame |
| | `onRenderGui(GuiScreen)` | antes de la GUI actual |
| | `onSetupCameraTransform()` | tras configurar la cámara |
| `PreRenderListener` | `onRenderWorld(float partialTicks)` | antes del mundo |
| | `onSetupCameraTransform(float, int pass, long timeSlice)`, `onRenderSky`, `onRenderClouds`, `onRenderTerrain(float,int)` | fases del mundo |
| `PostRenderListener` | `onPostRenderEntities(float)`, `onPostRender(float)` | tras entidades / tras el mundo (la cámara se reconfigura con `setupCamera` antes de `onPostRender`: ahí se dibuja el contorno RF-006) |
| `HUDRenderListener` | `onPreRenderHUD(int,int)`, `onPostRenderHUD(int,int)` | HUD |
| `FrameBufferListener` | `onRenderFBO`/`postRenderFBO(Framebuffer)` | framebuffer principal |
| `ViewportListener` | `onViewportResized`, `onFullScreenToggled` | ventana |
| `ScreenshotListener` | captura F2 | no usar |
GL de función fija con LWJGL 2 directamente (`GL11`, `Framebuffer`, `GlStateManager`); LiteLoader no ofrece capa propia de dibujo.

## 7. Tick y mundo
- `Tickable.onTick(Minecraft, float partialTicks, boolean inGame, boolean clock)`: cada frame; `clock` = true una vez por tick de 20 Hz. `inGame` indica mundo cargado.
- `GameLoopListener.onRunGameLoop(Minecraft)`.
- `JoinGameListener.onJoinGame(INetHandler, SPacketJoinGame, ServerData, RealmsServer)`, `PreJoinGameListener`, `PostLoginListener`.
- Cambio de mundo: `api.WorldObserver.onWorldChanged(World)` (observador del loader, vía `LiteLoaderEventBrokerClient`; detecta cambio por hashCode del `WorldClient`). Alternativa simple: comparar `mc.world` en `onTick`.

## 8. Lectura de bloques
Vanilla, sin LiteLoader: `Minecraft.getMinecraft().world` (`WorldClient`) → `getBlockState(BlockPos)`, `isBlockLoaded(BlockPos)`, `getChunkProvider().isChunkGeneratedAt(x,z)`. Dibujo: `getBlockRendererDispatcher().renderBlock(...)`/`BlockModelRenderer`. Solo lectura: no llamar a `setBlockState`.

## 9. Interfaces útiles por tarea
| Tarea | Interfaces / clases |
|---|---|
| T-009 hola mundo | `LiteMod` (`LiteModIsoRender`), `InitCompleteListener`, `printChatMessage` |
| T-010 tecla/comando/pantalla | `Tickable`, `LiteLoader.getInput().registerKeyBinding`, `OutboundChatFilter`, `displayGuiScreen` |
| T-011 go/no-go | `PostRenderListener`/`RenderListener` + FBO propio + `WorldClient.getBlockState` |
| T-012/T-013 selección | `OutboundChatFilter`, `Tickable` (pos del jugador/mirada), `World.isBlockLoaded` |
| T-014 contorno | `PostRenderListener.onPostRender(float)` (cámara ya configurada) |
| T-015..T-019, T-025 render | `Framebuffer`, `GL11`, `BlockRendererDispatcher`; disparo desde `RenderListener.onRender()` o `onPostRender` |
| T-020+ menú/vista previa | `GuiScreen`, FBO + `Tickable`; `ViewportListener` si se reajusta |
| Config | `Exposable` + `registerExposable`, `LiteLoader.getConfigFolder()`, `upgradeSettings` |

## 10. Riesgos para el go/no-go de T-011
1. Estado GL: el loader no aísla; guardar/restaurar con `glPushAttrib/glPopAttrib` + `GlStateManager` y re-enlazar `mc.getFramebuffer()` (RNF/regla de «no cerrar el juego»).
2. Render offscreen (FBO) dentro de un hook cambia viewport/matrices: restaurar siempre (try/finally).
3. Elegir el hook de disparo: `onPostRender` ya tiene cámara de mundo; render a FBO fuera de la cadena normal podría exigir `onRender()` [PRUEBA].
4. Chunks no cargados o `RenderChunk`/`BlockRendererDispatcher` sin `BufferBuilder` en curso: dibujar con un `Tessellator` propio, no con los chunks del mundo.
5. Comandos: sin Tab-complete; depende de que el `Mixin` de `EntityPlayerSP` esté activo (viene con LiteLoader; [PRUEBA] en T-010).
6. Abrir `GuiScreen` desde el filtro de chat: diferir un tick.
7. Mappings: los sources de LiteLoader son SRG; no copiar firmas de ahí al compilar, usar MCP.
8. Si el aviso de `libfliteWrapper.so` o la SNAPSHOT cambia (repositorio mutable), fijar el jar usado (ver ENTORNO.md).
Veredicto previo: **sin bloqueadores técnicos de API**; la decisión go/no-go depende de la prueba visual en juego (T-011 con 👁).
