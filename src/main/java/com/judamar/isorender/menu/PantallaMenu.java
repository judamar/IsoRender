package com.judamar.isorender.menu;

import java.io.IOException;
import com.judamar.isorender.render.Fallos;
import com.judamar.isorender.render.ResolucionSalida;
import com.judamar.isorender.render.VistaPrevia;
import com.judamar.isorender.render.VistaPreviaCalculo;
import com.judamar.isorender.seleccion.Seleccion;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

/**
 * T-023 (RF-016), T-024 (RF-017, RF-018, RF-023) y T-026 (RF-020, RF-021, RF-022): menú con un deslizador y un campo numérico
 * por ajuste de cámara, selector de resolución, presets, restablecer y botón Renderizar. La vista previa está en VistaPrevia.
 * No pausa el juego y no modifica el mundo.
 */
public class PantallaMenu extends GuiScreen {
    private static final int ID_RENDERIZAR = 100, ID_CERRAR = 101, ID_FONDO = 102, ID_VISTA = 103, ID_RESOLUCION = 104,
            ID_PRESET = 110 /* + ordinal del preset */, ID_RESTABLECER = 120;
    private static final int ANCHO_VISTA = 100; // botón «Vista» junto a «Fondo» (X+110+6 .. 236 < VistaPreviaCalculo.X_MIN)
    private static final int X = 20, ANCHO_DESLIZADOR = 150, ANCHO_CAMPO = 50, ANCHO_PRESET = 64, ANCHO_ERROR = 140;
    private static final int N = Parametro.values().length; // el campo de resolución es campos[N]
    private final Seleccion seleccion;
    private final EstadoCamara camara;
    private final Runnable alRenderizar;
    private final Deslizador[] deslizadores = new Deslizador[N];
    private final GuiTextField[] campos = new GuiTextField[N + 1];
    private final VistaPrevia vista = new VistaPrevia(); // T-025: se crea aquí, no en initGui (initGui se repite al redimensionar)
    private GuiButton botonVista, botonResolucion, botonRenderizar;
    private String error;

    /** alRenderizar: lo llama el botón Renderizar tras cerrar el menú; el mod hace el render en el frame siguiente. */
    public PantallaMenu(Seleccion seleccion, EstadoCamara camara, Runnable alRenderizar) {
        this.seleccion = seleccion; this.camara = camara; this.alRenderizar = alRenderizar;
    }

    /** Filas de 22 px desde y = 40. */
    private static int fila(int i) { return 40 + 22 * i; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        for (Parametro p : Parametro.values()) {
            int i = p.ordinal();
            deslizadores[i] = new Deslizador(i, X, fila(i), ANCHO_DESLIZADOR, p, p.valor(camara.actual()), this::alDeslizar);
            buttonList.add(deslizadores[i]);
            campos[i] = nuevoCampo(i, fila(i));
        }
        buttonList.add(new GuiButton(ID_FONDO, X, fila(5), 110, 20, "Fondo: " + camara.fondo().etiqueta));
        buttonList.add(botonVista = new GuiButton(ID_VISTA, X + 110 + 6, fila(5), ANCHO_VISTA, 20, ""));
        buttonList.add(botonResolucion = new GuiButton(ID_RESOLUCION, X, fila(6), ANCHO_DESLIZADOR, 20, ""));
        campos[N] = nuevoCampo(N, fila(6));
        for (PresetCamara pr : PresetCamara.values())
            buttonList.add(new GuiButton(ID_PRESET + pr.ordinal(), X + pr.ordinal() * (ANCHO_PRESET + 4), fila(7), ANCHO_PRESET, 20, pr.etiqueta));
        buttonList.add(new GuiButton(ID_RESTABLECER, X, fila(8), 70, 20, "Restablecer"));
        // Renderizar y Cerrar bajo la vista previa, centrados en la zona derecha (x >= VistaPreviaCalculo.X_MIN)
        int libre = width - 10 - 240, w = Math.min(150, libre), x = 240 + (libre - w) / 2;
        buttonList.add(botonRenderizar = new GuiButton(ID_RENDERIZAR, x, height - 52, w, 20, "Renderizar"));
        buttonList.add(new GuiButton(ID_CERRAR, x, height - 28, w, 20, "Cerrar"));
        botonRenderizar.enabled = seleccion.completa(); // CA-021.2
        refrescarControles();
    }

    private GuiTextField nuevoCampo(int id, int y) {
        GuiTextField c = new GuiTextField(id, fontRenderer, X + ANCHO_DESLIZADOR + 6, y, ANCHO_CAMPO, 20);
        c.setMaxStringLength(8);
        return c;
    }

    /** Relee todos los controles desde EstadoCamara (deslizadores, campos, resolución, vista) y limpia el error. */
    private void refrescarControles() {
        error = null;
        for (Parametro p : Parametro.values()) {
            double v = p.valor(camara.actual());
            deslizadores[p.ordinal()].poner(v);
            campos[p.ordinal()].setText(Parametro.formato(v));
        }
        botonVista.displayString = textoVista(camara.actual().perspectiva);
        botonResolucion.displayString = "Resolución: " + camara.resolucion() + " px";
        campos[N].setText(String.valueOf(camara.resolucion()));
    }

    /** «Vista: Perspectiva» si cabe en el botón (con margen de 4 px a cada lado); si no, la forma corta. */
    private String textoVista(boolean perspectiva) {
        String largo = "Vista: " + (perspectiva ? "Perspectiva" : "Isométrica");
        if (fontRenderer.getStringWidth(largo) <= ANCHO_VISTA - 8) return largo;
        return "Vista: " + (perspectiva ? "Persp." : "Isom.");
    }

    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); vista.close(); }

    private void alDeslizar(Parametro p, double valor) {
        error = null;
        campos[p.ordinal()].setText(Parametro.formato(camara.fijar(p, valor)));
    }

    /** Confirma lo escrito en el campo i (CA-023.1, CA-020.2); si no es válido conserva el valor anterior e indica el error. */
    private void confirmar(int i) {
        try {
            if (i < N) {
                Parametro p = Parametro.values()[i];
                deslizadores[i].poner(camara.fijar(p, p.leer(campos[i].getText())));
            } else camara.fijarResolucion(ResolucionSalida.parsear(campos[i].getText()));
            error = null;
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        if (i < N) campos[i].setText(Parametro.formato(Parametro.values()[i].valor(camara.actual())));
        else { campos[i].setText(String.valueOf(camara.resolucion())); botonResolucion.displayString = "Resolución: " + camara.resolucion() + " px"; }
    }

    @Override
    public void updateScreen() {
        for (GuiTextField c : campos) c.updateCursorCounter();
        botonRenderizar.enabled = seleccion.completa(); // CA-021.2
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        try {
            drawDefaultBackground();
            drawCenteredString(fontRenderer, "IsoRender", width / 2, 12, 0xFFFFFF);
            boolean completa = seleccion.completa();
            drawCenteredString(fontRenderer, TextoSeleccion.describir(seleccion), width / 2, 28, completa ? 0x55FF55 : 0xFFAA00);
            super.drawScreen(mouseX, mouseY, partialTicks);
            for (GuiTextField c : campos) c.drawTextBox();
            if (error != null) { // junto a Restablecer (fila 8), máx. 2 líneas ajustadas a 140 px para no invadir la vista previa
                int ye = fila(8) + 1, n = 0;
                for (String l : fontRenderer.listFormattedStringToWidth(error, ANCHO_ERROR)) {
                    if (n++ == 2) break;
                    drawString(fontRenderer, l, 96, ye, 0xFF5555); ye += 9;
                }
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
            if (tenia[i] && !campos[i].isFocused()) confirmar(i); // al salir del campo se confirma
        }
    }

    @Override
    protected void keyTyped(char c, int tecla) throws IOException {
        for (int i = 0; i < campos.length; i++) {
            if (!campos[i].isFocused()) continue;
            if (tecla == Keyboard.KEY_RETURN || tecla == Keyboard.KEY_NUMPADENTER) confirmar(i);
            else if (tecla == Keyboard.KEY_ESCAPE) { campos[i].setFocused(false); confirmar(i); }
            else campos[i].textboxKeyTyped(c, tecla);
            return;
        }
        super.keyTyped(c, tecla); // Esc cierra el menú
    }

    @Override
    protected void actionPerformed(GuiButton boton) throws IOException {
        if (boton.id == ID_CERRAR) mc.displayGuiScreen(null);
        else if (boton.id == ID_RENDERIZAR) {
            // CA-021.3: al cerrar el menú no hay segundo clic posible; el render lo hace el mod en el frame siguiente
            mc.displayGuiScreen(null);
            alRenderizar.run();
        }
        else if (boton.id == ID_FONDO) boton.displayString = "Fondo: " + camara.siguienteFondo().etiqueta;
        else if (boton.id == ID_VISTA) boton.displayString = textoVista(camara.alternarPerspectiva());
        else if (boton.id == ID_RESOLUCION) { camara.fijarResolucion(ResolucionSalida.siguiente(camara.resolucion())); refrescarControles(); }
        else if (boton.id == ID_RESTABLECER) { camara.restablecer(); refrescarControles(); }
        else if (boton.id >= ID_PRESET && boton.id < ID_PRESET + PresetCamara.values().length) {
            camara.aplicar(PresetCamara.values()[boton.id - ID_PRESET]);
            refrescarControles();
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
