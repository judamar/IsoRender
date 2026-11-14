package com.judamar.isorender;

import com.judamar.isorender.menu.PantallaPrueba;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.OutboundChatFilter;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

/** Clase de entrada. T-009: saludo en el chat. T-010: tecla K y /isorender menu abren una pantalla de prueba. */
public class LiteModIsoRender implements LiteMod, Tickable, OutboundChatFilter {
    private final KeyBinding teclaMenu = new KeyBinding("Abrir menú IsoRender", Keyboard.KEY_K, "IsoRender");
    private boolean saludado;
    /** El filtro de chat corre dentro de sendChatMessage: la pantalla se abre en el siguiente tick. */
    private volatile boolean abrirMenu;

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
        String m = message.trim();
        if (!m.equals("/isorender") && !m.startsWith("/isorender ")) return true;
        if (m.equals("/isorender menu")) {
            abrirMenu = true;
        } else {
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString("[IsoRender] Uso: /isorender menu"));
        }
        return false;
    }
}
