package com.judamar.isorender.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/** T-024: deslizador propio (GuiSlider es de Forge, que no se usa). Arrastrar con el ratón cambia el valor del Parametro. */
final class Deslizador extends GuiButton {
    interface Oyente { void cambio(Parametro p, double valor); }

    private final Parametro parametro;
    private final Oyente oyente;
    private double fraccion;
    private boolean arrastrando;

    Deslizador(int id, int x, int y, int ancho, Parametro parametro, double valor, Oyente oyente) {
        super(id, x, y, ancho, 20, "");
        this.parametro = parametro; this.oyente = oyente;
        poner(valor);
    }

    /** Actualiza posición y texto sin avisar al oyente. */
    void poner(double valor) {
        fraccion = parametro.fraccion(valor);
        displayString = parametro.etiqueta + ": " + parametro.conUnidad(valor);
    }

    @Override protected int getHoverState(boolean mouseOver) { return 0; } // fondo oscuro de barra, sin resaltado de botón

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        if (arrastrando) {
            double f = (double) (mouseX - (x + 4)) / (width - 8);
            double v = parametro.deFraccion(f);
            poner(v);
            oyente.cambio(parametro, v);
        }
        mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
        GlStateManager.color(1f, 1f, 1f, 1f);
        int px = x + (int) (fraccion * (width - 8));
        drawTexturedModalRect(px, y, 0, 66, 4, 20);
        drawTexturedModalRect(px + 4, y, 196, 66, 4, 20);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!super.mousePressed(mc, mouseX, mouseY)) return false;
        arrastrando = true;
        return true;
    }

    @Override public void mouseReleased(int mouseX, int mouseY) { arrastrando = false; }
}
