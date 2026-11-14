package com.judamar.isorender;

import com.judamar.isorender.configuracion.Ajustes;
import com.judamar.isorender.configuracion.ArchivoConfiguracion;
import com.judamar.isorender.configuracion.Configuracion;
import com.judamar.isorender.menu.PantallaMenu;
import com.judamar.isorender.seleccion.Seleccion;
import com.judamar.isorender.seleccion.Subcomando;
import com.judamar.isorender.seleccion.ValidacionMundo;
import com.mumfrey.liteloader.LiteMod;
import com.judamar.isorender.render.ArgumentoItem;
import com.judamar.isorender.render.Estres;
import com.judamar.isorender.render.Fallos;
import com.judamar.isorender.render.ParametrosCamara;
import com.judamar.isorender.render.RenderPrueba;
import com.judamar.isorender.render.ResolucionSalida;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.RenderListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import java.io.File;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

/** Clase de entrada. T-009: saludo en el chat. T-010/T-023: tecla K y /isorender menu abren el menú. */
public class LiteModIsoRender implements LiteMod, Tickable, OutboundChatFilter, RenderListener, PostRenderListener {
    private final com.judamar.isorender.render.ContornoSeleccion contorno = new com.judamar.isorender.render.ContornoSeleccion();
    private final KeyBinding teclaMenu = new KeyBinding("Abrir menú IsoRender", Keyboard.KEY_K, "IsoRender");
    private final Seleccion seleccion = new Seleccion();
    private final com.judamar.isorender.menu.EstadoCamara camara = new com.judamar.isorender.menu.EstadoCamara();
    private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger("IsoRender");
    private File archivoConfig;
    private Configuracion ultimoGuardado;
    private boolean saludado;
    /** El filtro de chat corre dentro de sendChatMessage: la pantalla se abre en el siguiente tick. */
    private volatile boolean abrirMenu;
    /** T-011/T-018/T-022: el render pedido (prueba, prueba-multi, estres) se ejecuta en el siguiente frame, dentro de onRender. Un solo hueco: no se solapan. */
    private volatile Subcomando pendiente;
    /** T-026: render pedido por el botón Renderizar o /isorender render; onRender lo procesa en el frame siguiente. */
    private volatile boolean renderMenu;
    private volatile String pruebaArg = "";
    /** T-022: estres en curso (un render por frame); null si no hay. */
    private Estres estres;

    @Override public String getName() { return "IsoRender"; }
    @Override public String getVersion() { return "0.1.0"; }
    @Override public void upgradeSettings(String version, File configPath, File oldConfigPath) { }

    @Override
    public void init(File configPath) {
        LiteLoader.getInput().registerKeyBinding(teclaMenu);
        try {
            archivoConfig = new File(configPath, "isorender.properties");
            LOG.info("Configuración en " + archivoConfig.getAbsolutePath());
            Configuracion c = ArchivoConfiguracion.cargar(archivoConfig, LOG::warn); // crea el archivo si falta; avisos al log, no al chat
            Ajustes.actual(c);
            camara.sembrar(c);
            ultimoGuardado = c;
        } catch (Throwable e) {
            LOG.warn("No se pudo cargar la configuración; se usan los valores por defecto.", e); // RNF-005
        }
    }

    /** T-030 (RF-029): al cerrar el menú guarda cámara, resolución y fondo actuales; sin cambios no escribe. Nunca lanza. */
    private void guardarConfiguracion() {
        if (archivoConfig == null) return;
        Configuracion c = camara.aConfiguracion(Ajustes.actual());
        if (c.equals(ultimoGuardado)) return;
        ArchivoConfiguracion.guardar(archivoConfig, c, LOG::warn);
        ultimoGuardado = c;
    }

    @Override
    public void onTick(Minecraft mc, float partialTicks, boolean inGame, boolean clock) {
        if (!inGame || mc.player == null) { saludado = false; return; }
        if (!saludado) {
            saludado = true;
            mc.player.sendMessage(new TextComponentString("[IsoRender] Mod cargado (v" + getVersion() + ")."));
        }
        // isPressed() se consume siempre para no acumular pulsaciones
        boolean tecla = teclaMenu.isPressed();
        boolean abrir = (tecla || abrirMenu) && mc.currentScreen == null;
        abrirMenu = false;
        if (abrir) {
            try { mc.displayGuiScreen(new PantallaMenu(seleccion, camara, this::pedirRenderMenu, this::guardarConfiguracion)); }
            catch (Throwable e) { Fallos.informar(mc, "abrir el menú", e); } // RNF-005: un fallo del menu no cierra el juego
        }
    }

    /** Intercepta /isorender en el cliente (no llega al servidor). Devuelve false para cancelar el envio. */
    @Override
    public boolean onSendChatMessage(String message) {
        Subcomando sub = Subcomando.parsear(message);
        if (sub == null) return true;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            switch (sub) {
                case MENU: abrirMenu = true; break;
                case RENDER: renderMenu = true; break;
                case PRUEBA: case PRUEBA_MULTI: case ESTRES: case ITEM:
                    // T-022: un render a la vez; el segundo comando se rechaza en vez de pisar al primero.
                    if (pendiente != null || estres != null || RenderPrueba.ocupado()) {
                        decir(mc, com.judamar.isorender.render.Chat.OCUPADO);
                    } else { pruebaArg = Subcomando.argumento(message); pendiente = sub; }
                    break;
                case POS1: case POS2: fijarPunto(mc, sub == Subcomando.POS1); break;
                case CLEAR:
                    decir(mc, seleccion.limpiar() ? "Selección borrada." : "No había ninguna selección.");
                    break;
                default: decir(mc, "Uso: /isorender pos1 | pos2 | clear | menu | render | prueba [lado 512-4096] [rotación 0-359] [elevación 5-90] [zoom 25-400] [vista iso|persp] | prueba-multi [n 2-8] | estres [n 1-50] | item [id] [meta]");
            }
        } catch (Throwable e) {
            Fallos.informar(mc, "procesar el comando", e); // RNF-005
        }
        return false;
    }

    private static void decir(Minecraft mc, String texto) {
        com.judamar.isorender.render.Chat.decir(mc, texto);
    }

    /** Fija pos1/pos2 en el bloque apuntado; sin bloque apuntado, en el bloque bajo los pies (CA-003.2). Solo lectura. */
    private void fijarPunto(Minecraft mc, boolean primero) {
        int x, y, z;
        String origen;
        RayTraceResult r = mc.objectMouseOver;
        if (r != null && r.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos b = r.getBlockPos();
            x = b.getX(); y = b.getY(); z = b.getZ(); origen = "bloque apuntado";
        } else {
            // [SUPUESTO] "donde está de pie" = bloque sobre el que se apoya el jugador
            x = MathHelper.floor(mc.player.posX); y = MathHelper.floor(mc.player.posY) - 1; z = MathHelper.floor(mc.player.posZ);
            origen = "no apuntas a ningún bloque; se usa el bloque donde estás de pie";
        }
        if (primero) seleccion.fijarPunto1(x, y, z); else seleccion.fijarPunto2(x, y, z);
        decir(mc, "Punto " + (primero ? 1 : 2) + " fijado en (" + x + ", " + y + ", " + z + ") [" + origen + "].");
        int[] d = seleccion.dimensiones();
        if (d != null) decir(mc, "Región: " + d[0] + " × " + d[1] + " × " + d[2] + " bloques.");
    }

    private void pedirRenderMenu() { renderMenu = true; }

    /** T-026 (RF-021): render de la región con la resolución y cámara actuales del menú; misma guardia y validación que las pruebas. */
    private void renderDesdeMenu(Minecraft mc) {
        try {
            if (mc.world == null || mc.player == null) return;
            if (pendiente != null || estres != null || RenderPrueba.ocupado()) {
                decir(mc, com.judamar.isorender.render.Chat.OCUPADO);
                return;
            }
            String error = ValidacionMundo.validar(seleccion, mc.world, Ajustes.actual().tamanoMaximo);
            if (error != null) { decir(mc, error); return; }
            // T-043 (RF-015): exporta la región recortada por el corte de altura del menú (el nombre de archivo usa la caja recortada)
            RenderPrueba.exportar(mc, RenderPrueba.regionConCorte(seleccion.region(), camara.corteY()), camara.resolucion(), camara.actual());
        } catch (Throwable e) {
            Fallos.informar(mc, "preparar el render", e); // RNF-005
        }
    }

    @Override
    public void onRender() {
        Minecraft mc = Minecraft.getMinecraft();
        if (renderMenu) { renderMenu = false; renderDesdeMenu(mc); return; }
        if (estres != null) { avanzarEstres(mc); return; }
        Subcomando sub = pendiente;
        if (sub == null) return;
        pendiente = null;
        try {
            if (mc.world == null || mc.player == null) return;
            if (sub == Subcomando.ITEM) { renderItem(mc); return; } // T-042: no usa region ni seleccion
            int lado = 0, n = 0;
            ParametrosCamara cam = null;
            // Argumentos invalidos: IllegalArgumentException con mensaje en español (CA-025.2: sin archivo)
            if (sub == Subcomando.PRUEBA_MULTI) n = RenderPrueba.parsearN(pruebaArg);
            else if (sub == Subcomando.PRUEBA) {
                String[] a = pruebaArg.trim().isEmpty() ? new String[0] : pruebaArg.trim().split("\\s+");
                lado = ResolucionSalida.parsear(a.length > 0 ? a[0] : "");
                cam = ParametrosCamara.parsear(a.length > 1 ? java.util.Arrays.copyOfRange(a, 1, a.length) : new String[0]);
            } else Estres.validarArgumento(pruebaArg);
            String error = ValidacionMundo.validar(seleccion, mc.world, Ajustes.actual().tamanoMaximo);
            if (error != null) { decir(mc, error); return; }
            if (sub == Subcomando.PRUEBA_MULTI) RenderPrueba.ejecutarMulti(mc, seleccion.region(), n);
            else if (sub == Subcomando.PRUEBA) RenderPrueba.ejecutar(mc, seleccion.region(), lado, cam);
            else estres = Estres.iniciar(mc, seleccion.region(), pruebaArg);
        } catch (Throwable e) {
            Fallos.informar(mc, "preparar el render", e); // RNF-005: nunca cierra el juego ni vuelca la traza al chat
        }
    }

    /** T-042 (RF-030): item de la mano (la secundaria si la principal esta vacia) o item/bloque por ID y metadata. Solo lectura. */
    private void renderItem(Minecraft mc) {
        ArgumentoItem a = ArgumentoItem.parsear(pruebaArg); // IllegalArgumentException en español: lo informa onRender, sin archivo
        ItemStack stack;
        if (a.id == null) {
            stack = mc.player.getHeldItemMainhand();
            if (stack.isEmpty()) stack = mc.player.getHeldItemOffhand(); // [SUPUESTO]
            if (stack.isEmpty()) { decir(mc, "No tienes ningún ítem en la mano."); return; }
            stack = stack.copy(); // nunca se toca el del inventario
        } else {
            Item item = Item.getByNameOrId(a.id);
            if (item == null || item == Items.AIR) {
                Block b = Block.getBlockFromName(a.id);
                decir(mc, b != null && b != Blocks.AIR ? "El bloque «" + a.id + "» no tiene forma de ítem y no se puede renderizar."
                        : "No existe el ítem o bloque «" + a.id + "».");
                return;
            }
            stack = new ItemStack(item, 1, a.meta);
        }
        RenderPrueba.exportarItem(mc, stack, camara.resolucion());
    }

    /** T-022: un paso del estres por frame; ante cualquier fallo inesperado se libera la guardia y se informa. */
    private void avanzarEstres(Minecraft mc) {
        try {
            if (estres.paso(mc)) estres = null;
        } catch (Throwable e) {
            Fallos.informar(mc, "ejecutar el estrés", e);
            estres.cerrar();
            estres = null;
        }
    }

    /** T-014: tras el mundo, con la camara ya configurada, se dibuja el contorno de la seleccion. */
    @Override public void onPostRenderEntities(float partialTicks) { }
    @Override public void onPostRender(float partialTicks) {
        contorno.dibujar(Minecraft.getMinecraft(), seleccion, partialTicks);
    }

    @Override public void onRenderGui(net.minecraft.client.gui.GuiScreen currentScreen) { }
    @Override public void onSetupCameraTransform() { }
}
