package com.judamar.isorender.render;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

/** T-027 (RF-027, RNF-011): único punto de salida al chat del jugador; todo con el prefijo [IsoRender] y en inglés. Nunca lanza. */
public final class Chat {
    public static final String PREFIJO = "[IsoRender] ";
    public static final String OCUPADO = "A render is already running or pending; wait for it to finish.";

    private Chat() { }

    public static void decir(Minecraft mc, String texto) {
        try {
            if (mc != null && mc.player != null) mc.player.sendMessage(new TextComponentString(PREFIJO + texto));
        } catch (Throwable ignorado) { }
    }

    /** Mensaje de éxito: la ruta va una sola vez, subrayada, y al hacer clic abre el PNG con el programa por defecto (CA-027.1). */
    public static void archivo(Minecraft mc, String antes, File archivo, String despues) {
        try {
            if (mc == null || mc.player == null) return;
            TextComponentString ruta = new TextComponentString(archivo.getAbsolutePath());
            ruta.getStyle().setUnderlined(true).setColor(TextFormatting.AQUA)
                    .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, archivo.getAbsolutePath()));
            TextComponentString m = new TextComponentString(PREFIJO + antes);
            m.appendSibling(ruta);
            m.appendText(despues);
            mc.player.sendMessage(m);
        } catch (Throwable ignorado) { }
    }
}
