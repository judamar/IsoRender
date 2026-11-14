package com.judamar.isorender.menu;

import java.io.IOException;
import com.judamar.isorender.render.Fallos;
import com.judamar.isorender.seleccion.Seleccion;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/**
 * T-023 (RF-016): pantalla base del menú. Los controles de cámara (T-024), la vista previa (T-025)
 * y el botón Renderizar activo (T-026) se añaden sobre esta pantalla. No pausa el juego y no modifica el mundo.
 */
public class PantallaMenu extends GuiScreen {
    private static final int ID_RENDERIZAR = 1, ID_CERRAR = 2;
    private final Seleccion seleccion;

    public PantallaMenu(Seleccion seleccion) { this.seleccion = seleccion; }

    @Override
    public void initGui() {
        int ancho = 150, x = width / 2 - ancho / 2;
        // CA-016.3: sin región completa, Renderizar queda deshabilitado (la acción real llega en T-026)
        GuiButton renderizar = new GuiButton(ID_RENDERIZAR, x, height - 52, ancho, 20, "Renderizar");
        renderizar.enabled = false;
        buttonList.add(renderizar);
        buttonList.add(new GuiButton(ID_CERRAR, x, height - 28, ancho, 20, "Cerrar"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            drawDefaultBackground();
            drawCenteredString(fontRenderer, "IsoRender", width / 2, 12, 0xFFFFFF);
            boolean completa = seleccion.completa();
            drawCenteredString(fontRenderer, TextoSeleccion.describir(seleccion), width / 2, 28, completa ? 0x55FF55 : 0xFFAA00);
            super.drawScreen(mouseX, mouseY, partialTicks);
        } catch (Throwable e) { // RNF-005: un fallo al dibujar el menu lo cierra y avisa; no cierra el juego
            Fallos.informar(mc, "dibujar el menú", e);
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void actionPerformed(GuiButton boton) throws IOException {
        if (boton.id == ID_CERRAR) mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
