package com.judamar.isorender.menu;

import java.io.IOException;
import com.judamar.isorender.render.Fallos;
import com.judamar.isorender.render.VistaPrevia;
import com.judamar.isorender.render.VistaPreviaCalculo;
import com.judamar.isorender.seleccion.Seleccion;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

/**
 * T-023 (RF-016) y T-024 (RF-017, RF-018, RF-023): menú con un deslizador y un campo numérico por ajuste de cámara.
 * La vista previa (T-025) está en VistaPrevia; el botón Renderizar activo (T-026) se añaden sobre esta pantalla.
 * No pausa el juego y no modifica el mundo.
 */
public class PantallaMenu extends GuiScreen {
    private static final int ID_RENDERIZAR = 100, ID_CERRAR = 101, ID_FONDO = 102;
    private static final int X = 20, ANCHO_DESLIZADOR = 150, ANCHO_CAMPO = 50, FILA = 26, Y0 = 44;
    private final Seleccion seleccion;
    private final EstadoCamara camara;
    private final Deslizador[] deslizadores = new Deslizador[Parametro.values().length];
    private final GuiTextField[] campos = new GuiTextField[Parametro.values().length];
    private final VistaPrevia vista = new VistaPrevia(); // T-025: se crea aquí, no en initGui (initGui se repite al redimensionar)
    private String error;

    public PantallaMenu(Seleccion seleccion, EstadoCamara camara) { this.seleccion = seleccion; this.camara = camara; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        for (Parametro p : Parametro.values()) {
            int i = p.ordinal(), y = Y0 + i * FILA;
            double v = p.valor(camara.actual());
            deslizadores[i] = new Deslizador(i, X, y, ANCHO_DESLIZADOR, p, v, this::alDeslizar);
            buttonList.add(deslizadores[i]);
            GuiTextField c = new GuiTextField(i, fontRenderer, X + ANCHO_DESLIZADOR + 6, y, ANCHO_CAMPO, 20);
            c.setMaxStringLength(8);
            c.setText(Parametro.formato(v));
            campos[i] = c;
        }
        buttonList.add(new GuiButton(ID_FONDO, X, Y0 + Parametro.values().length * FILA, 110, 20, "Fondo: " + camara.fondo().etiqueta));
        int ancho = 150, x = width / 2 - ancho / 2;
        // CA-016.3: sin región completa, Renderizar queda deshabilitado (la acción real llega en T-026)
        GuiButton renderizar = new GuiButton(ID_RENDERIZAR, x, height - 52, ancho, 20, "Renderizar");
        renderizar.enabled = false;
        buttonList.add(renderizar);
        buttonList.add(new GuiButton(ID_CERRAR, x, height - 28, ancho, 20, "Cerrar"));
    }

    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); vista.close(); }

    private void alDeslizar(Parametro p, double valor) {
        error = null;
        campos[p.ordinal()].setText(Parametro.formato(camara.fijar(p, valor)));
    }

    /** Confirma lo escrito en un campo (CA-023.1); si no es un número conserva el valor anterior e indica el error (CA-023.2). */
    private void confirmar(Parametro p) {
        int i = p.ordinal();
        try {
            double v = camara.fijar(p, p.leer(campos[i].getText()));
            error = null;
            deslizadores[i].poner(v);
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        campos[i].setText(Parametro.formato(p.valor(camara.actual())));
    }

    @Override
    public void updateScreen() { for (GuiTextField c : campos) c.updateCursorCounter(); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            drawDefaultBackground();
            drawCenteredString(fontRenderer, "IsoRender", width / 2, 12, 0xFFFFFF);
            boolean completa = seleccion.completa();
            drawCenteredString(fontRenderer, TextoSeleccion.describir(seleccion), width / 2, 28, completa ? 0x55FF55 : 0xFFAA00);
            super.drawScreen(mouseX, mouseY, partialTicks);
            for (GuiTextField c : campos) c.drawTextBox();
            if (error != null) { // bajo el botón de fondo, ajustado a su ancho para no invadir Renderizar/Cerrar
                int ye = Y0 + Parametro.values().length * FILA + 24;
                for (String l : fontRenderer.listFormattedStringToWidth(error, 112)) { drawString(fontRenderer, l, X, ye, 0xFF5555); ye += 9; }
            }
            // Vista previa (T-025): recuadro a la derecha de los controles; la escala de GUI fija el tamaño real del FBO.
            int[] r = VistaPreviaCalculo.recuadro(width, height);
            if (r == null) drawCenteredString(fontRenderer, "Ventana demasiado pequeña para la vista previa", width / 2, height - 72, 0xFFAA00);
            else vista.dibujar(mc, seleccion, camara.actual(), camara.fondo(), r[0], r[1], r[2], new ScaledResolution(mc).getScaleFactor());
        } catch (Throwable e) { // RNF-005: un fallo al dibujar el menu lo cierra y avisa; no cierra el juego
            Fallos.informar(mc, "dibujar el menú", e);
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        boolean[] tenia = new boolean[campos.length];
        for (int i = 0; i < campos.length; i++) tenia[i] = campos[i].isFocused();
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (int i = 0; i < campos.length; i++) {
            campos[i].mouseClicked(mouseX, mouseY, mouseButton);
            if (tenia[i] && !campos[i].isFocused()) confirmar(Parametro.values()[i]); // al salir del campo se confirma
        }
    }

    @Override
    protected void keyTyped(char c, int tecla) throws IOException {
        for (int i = 0; i < campos.length; i++) {
            if (!campos[i].isFocused()) continue;
            if (tecla == Keyboard.KEY_RETURN || tecla == Keyboard.KEY_NUMPADENTER) confirmar(Parametro.values()[i]);
            else if (tecla == Keyboard.KEY_ESCAPE) { campos[i].setFocused(false); confirmar(Parametro.values()[i]); }
            else campos[i].textboxKeyTyped(c, tecla);
            return;
        }
        super.keyTyped(c, tecla); // Esc cierra el menú
    }

    @Override
    protected void actionPerformed(GuiButton boton) throws IOException {
        if (boton.id == ID_CERRAR) mc.displayGuiScreen(null);
        else if (boton.id == ID_FONDO) boton.displayString = "Fondo: " + camara.siguienteFondo().etiqueta;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
