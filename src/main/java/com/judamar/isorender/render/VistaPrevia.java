package com.judamar.isorender.render;

import com.judamar.isorender.menu.TextoSeleccion;
import com.judamar.isorender.seleccion.Seleccion;
import com.judamar.isorender.seleccion.ValidacionMundo;
import com.judamar.isorender.seleccion.ValidacionRegion;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.opengl.GL11;

/**
 * T-025 (RF-019, RNF-003): vista previa en vivo del menú. La geometría se construye una vez por región y se dibuja en un
 * FBO propio solo cuando la cámara cambia; el FBO se muestra como un quad sobre un tablero que revela la transparencia.
 * Solo lectura del mundo. Solo hilo de render. Cualquier fallo queda en estado ERROR (texto en el recuadro), sin cerrar el menú.
 */
public final class VistaPrevia implements AutoCloseable {
    enum Estado { SIN_REGION, PREPARANDO, LISTA, ERROR }

    private static final int CELDA = 8, REVALIDAR_CADA = 20;

    private Estado estado = Estado.SIN_REGION;
    private String motivo = "";
    private GeometriaCacheada geo;
    private FramebufferRender fbo;
    private boolean simplificado, pendiente, sucio, ordenPendiente, avisadoGl;
    private ParametrosCamara camPintada, camError;
    private int[] regValidada, regError;
    private String motivoInvalido;
    private long ultimoReorden;
    private int frame;

    public void dibujar(Minecraft mc, Seleccion sel, ParametrosCamara cam, int x, int y, int lado, int escala) {
        tablero(x, y, lado);
        try {
            actualizar(mc, sel, cam, lado, escala);
        } catch (Throwable e) { // RNF-005: la vista previa falla sola; el menú sigue abierto
            fallar(e, sel.region(), cam);
        }
        if (estado != Estado.LISTA) { texto(mc.fontRenderer, motivo, x, y, lado); return; }
        try {
            quad(mc, x, y, lado);
        } catch (Throwable e) {
            fallar(e, sel.region(), cam);
            return;
        }
        if (simplificado) {
            String s = "Vista simplificada (región grande)";
            mc.fontRenderer.drawString(s, x + (lado - mc.fontRenderer.getStringWidth(s)) / 2, y + lado + 2, 0xAAAAAA);
        }
    }

    /** Valida la región, construye la geometría (diferida un frame) y repinta el FBO si hace falta. Fija estado y motivo. */
    private void actualizar(Minecraft mc, Seleccion sel, ParametrosCamara cam, int lado, int escala) {
        int[] reg = sel.region();
        // (a) Validación al abrir, cada ~20 frames y cuando cambia la región.
        if (frame++ % REVALIDAR_CADA == 0 || !Arrays.equals(reg, regValidada)) {
            motivoInvalido = !sel.completa() ? TextoSeleccion.describir(sel) : ValidacionMundo.validar(sel, mc.world, ValidacionRegion.MAX);
            regValidada = reg;
        }
        if (motivoInvalido != null) {
            liberar(); estado = Estado.SIN_REGION; motivo = motivoInvalido; return;
        }
        if (estado == Estado.SIN_REGION) preparar();
        if (estado == Estado.ERROR) { // sin reintento por frame: solo si cambian región o cámara
            if (Arrays.equals(reg, regError) && cam.equals(camError)) return;
            preparar();
        }
        if (geo != null && !geo.valida(reg)) preparar();
        if (estado == Estado.PREPARANDO) {
            if (geo == null && RenderPrueba.ocupado()) { motivo = "Hay un render en curso; la vista previa espera."; return; }
            if (pendiente) { pendiente = false; motivo = "Preparando vista previa…"; return; } // un frame solo con el texto
            geo = RenderPrueba.construirControlado(mc, reg); // incluye la comprobación de memoria (RNF-014)
            simplificado = VistaPreviaCalculo.simplificado(GeometriaRegion.volumen(reg));
            sucio = true; ordenPendiente = false;
            estado = Estado.LISTA;
        }
        // (c) Repintar el FBO solo si cambió la cámara, el tamaño o quedó el orden de translúcidas pendiente.
        int lf = VistaPreviaCalculo.ladoFbo(lado, escala, simplificado);
        long ahora = System.currentTimeMillis();
        boolean reordenar = VistaPreviaCalculo.reordenar(simplificado, ahora, ultimoReorden);
        if (sucio || !cam.equals(camPintada) || (ordenPendiente && reordenar) || fbo == null || fbo.lado() != lf) {
            repintar(mc, reg, cam, lf, reordenar);
            camPintada = cam; sucio = false;
            if (reordenar) ultimoReorden = ahora;
            ordenPendiente = !reordenar;
        }
    }

    /** Pinta la escena en el FBO propio entre captura y restauración del estado GL del juego. */
    private void repintar(Minecraft mc, int[] reg, ParametrosCamara cam, int lf, boolean reordenar) {
        EstadoGrafico previo = EstadoGrafico.capturarCompleto(mc);
        try {
            if (fbo == null || fbo.lado() != lf) { cerrarFbo(); fbo = new FramebufferRender(lf); }
            fbo.enlazarYLimpiar();
            // El modo simplificado omite entidades de bloque y limita el reordenado de translúcidas.
            RenderPrueba.dibujarEscena(mc, geo, reg, cam, !simplificado, reordenar);
        } finally {
            // restaurar() devuelve framebuffer y viewport previos (no hace falta reenlazar a mano) y limpia glGetError.
            int e = previo.restaurar();
            if (e != 0 && !avisadoGl) {
                avisadoGl = true;
                if (mc.player != null) mc.player.sendMessage(new TextComponentString("[IsoRender] Aviso: OpenGL informó un error ("
                        + Fallos.nombreErrorGl(e) + ") en la vista previa; ya se limpió."));
            }
        }
    }

    /** Dibuja la textura del FBO (color premultiplicado) sobre el tablero; V invertida porque el FBO tiene el origen abajo. */
    private void quad(Minecraft mc, int x, int y, int lado) {
        EstadoGrafico previo = EstadoGrafico.capturarLigero(mc);
        try {
            GlStateManager.enableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1, 1, 1, 1);
            fbo.enlazarTextura();
            Tessellator t = Tessellator.getInstance();
            BufferBuilder b = t.getBuffer();
            b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            b.pos(x, y + lado, 0).tex(0, 0).endVertex();
            b.pos(x + lado, y + lado, 0).tex(1, 0).endVertex();
            b.pos(x + lado, y, 0).tex(1, 1).endVertex();
            b.pos(x, y, 0).tex(0, 1).endVertex();
            t.draw();
        } finally {
            previo.restaurar();
        }
    }

    /** Tablero de ajedrez: fondo claro con las celdas oscuras en un solo lote (miles de drawRect serían lentos). */
    private static void tablero(int x, int y, int lado) {
        Gui.drawRect(x, y, x + lado, y + lado, 0xFFCCCCCC);
        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(0.6F, 0.6F, 0.6F, 1);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        for (int j = 0; j * CELDA < lado; j++)
            for (int i = (j & 1); i * CELDA < lado; i += 2) {
                int x0 = x + i * CELDA, y0 = y + j * CELDA, x1 = Math.min(x0 + CELDA, x + lado), y1 = Math.min(y0 + CELDA, y + lado);
                b.pos(x0, y1, 0).endVertex(); b.pos(x1, y1, 0).endVertex(); b.pos(x1, y0, 0).endVertex(); b.pos(x0, y0, 0).endVertex();
            }
        t.draw();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableTexture2D();
    }

    /** Texto centrado con saltos de línea dentro del recuadro, sobre un fondo oscuro translúcido para que se lea. */
    private static void texto(FontRenderer f, String s, int x, int y, int lado) {
        List<String> lineas = f.listFormattedStringToWidth(s, lado - 12);
        int alto = lineas.size() * f.FONT_HEIGHT, y0 = y + (lado - alto) / 2;
        Gui.drawRect(x + 2, y0 - 4, x + lado - 2, y0 + alto + 4, 0xAA000000);
        for (int i = 0; i < lineas.size(); i++)
            f.drawString(lineas.get(i), x + (lado - f.getStringWidth(lineas.get(i))) / 2, y0 + i * f.FONT_HEIGHT, 0xFFFFFF);
    }

    private void preparar() { liberarGeo(); estado = Estado.PREPARANDO; pendiente = true; motivo = "Preparando vista previa…"; }

    private void fallar(Throwable e, int[] reg, ParametrosCamara cam) {
        try { e.printStackTrace(); } catch (Throwable ignorado) { }
        liberar();
        estado = Estado.ERROR; motivo = Fallos.mensaje(e, "la vista previa"); regError = reg; camError = cam;
    }

    private void liberarGeo() { if (geo != null) { geo.close(); geo = null; } camPintada = null; }
    private void cerrarFbo() { if (fbo != null) { fbo.close(); fbo = null; } }
    private void liberar() { liberarGeo(); cerrarFbo(); }

    /** Libera geometría y FBO. Idempotente. */
    @Override public void close() { liberar(); }
}
