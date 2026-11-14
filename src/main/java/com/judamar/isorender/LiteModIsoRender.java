package com.judamar.isorender;

import com.judamar.isorender.menu.PantallaPrueba;
import com.judamar.isorender.seleccion.Seleccion;
import com.judamar.isorender.seleccion.Subcomando;
import com.judamar.isorender.seleccion.ValidacionMundo;
import com.judamar.isorender.seleccion.ValidacionRegion;
import com.mumfrey.liteloader.LiteMod;
import com.judamar.isorender.render.RenderPrueba;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.RenderListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

/** Clase de entrada. T-009: saludo en el chat. T-010: tecla K y /isorender menu abren una pantalla de prueba. */
public class LiteModIsoRender implements LiteMod, Tickable, OutboundChatFilter, RenderListener, PostRenderListener {
    private final com.judamar.isorender.render.ContornoSeleccion contorno = new com.judamar.isorender.render.ContornoSeleccion();
    private final KeyBinding teclaMenu = new KeyBinding("Abrir menú IsoRender", Keyboard.KEY_K, "IsoRender");
    private final Seleccion seleccion = new Seleccion();
    private boolean saludado;
    /** El filtro de chat corre dentro de sendChatMessage: la pantalla se abre en el siguiente tick. */
    private volatile boolean abrirMenu;
    /** T-011: la prueba de render se ejecuta en el siguiente frame, dentro de onRender. */
    private volatile boolean pruebaPendiente;

    @Override public String getName() { return "IsoRender"; }
    @Override public String getVersion() { return "0.1.0"; }
    @Override public void upgradeSettings(String version, File configPath, File oldConfigPath) { }

    @Override
    public void init(File configPath) {
        LiteLoader.getInput().registerKeyBinding(teclaMenu);
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
        if ((tecla || abrirMenu) && mc.currentScreen == null) mc.displayGuiScreen(new PantallaPrueba());
        abrirMenu = false;
    }

    /** Intercepta /isorender en el cliente (no llega al servidor). Devuelve false para cancelar el envio. */
    @Override
    public boolean onSendChatMessage(String message) {
        Subcomando sub = Subcomando.parsear(message);
        if (sub == null) return true;
        Minecraft mc = Minecraft.getMinecraft();
        switch (sub) {
            case MENU: abrirMenu = true; break;
            case PRUEBA: pruebaPendiente = true; break;
            case POS1: case POS2: fijarPunto(mc, sub == Subcomando.POS1); break;
            case CLEAR:
                decir(mc, seleccion.limpiar() ? "Selección borrada." : "No había ninguna selección.");
                break;
            default: decir(mc, "Uso: /isorender pos1 | pos2 | clear | menu | prueba");
        }
        return false;
    }

    private static void decir(Minecraft mc, String texto) {
        mc.player.sendMessage(new TextComponentString("[IsoRender] " + texto));
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

    @Override
    public void onRender() {
        if (!pruebaPendiente) return;
        pruebaPendiente = false;
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (mc.world == null || mc.player == null) return;
            // Punto reutilizable para el futuro /isorender render: validar y luego renderizar la región.
            String error = ValidacionMundo.validar(seleccion, mc.world, ValidacionRegion.MAX);
            if (error != null) { decir(mc, error); return; }
            RenderPrueba.ejecutar(mc, seleccion.region());
        } catch (Throwable e) {
            decir(mc, "Error al validar la región: " + e);
            e.printStackTrace();
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
