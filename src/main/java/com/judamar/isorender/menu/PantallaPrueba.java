package com.judamar.isorender.menu;

import java.io.IOException;
import net.minecraft.client.gui.GuiScreen;

/** Pantalla de prueba de T-010: se cierra con Esc o con clic; se reemplazara por el menu real (T-023). */
public class PantallaPrueba extends GuiScreen {
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "IsoRender - pantalla de prueba", width / 2, height / 2 - 10, 0xFFFFFF);
        drawCenteredString(fontRenderer, "Pulsa Esc o haz clic para cerrar", width / 2, height / 2 + 6, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
