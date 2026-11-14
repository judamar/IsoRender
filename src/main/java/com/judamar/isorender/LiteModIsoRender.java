package com.judamar.isorender;

import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.LiteMod;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;

/** Clase de entrada. T-009: prototipo que saluda en el chat al entrar a un mundo. */
public class LiteModIsoRender implements LiteMod, Tickable {
    private boolean saludado;

    @Override public String getName() { return "IsoRender"; }
    @Override public String getVersion() { return "0.1.0"; }
    @Override public void init(File configPath) { }
    @Override public void upgradeSettings(String version, File configPath, File oldConfigPath) { }

    /** Una vez por mundo: escribe en el chat local (no se envía al servidor). */
    @Override
    public void onTick(Minecraft mc, float partialTicks, boolean inGame, boolean clock) {
        if (!inGame || mc.player == null) { saludado = false; return; }
        if (!saludado) {
            saludado = true;
            mc.player.sendMessage(new TextComponentString("[IsoRender] Mod cargado (v" + getVersion() + ")."));
        }
    }
}
