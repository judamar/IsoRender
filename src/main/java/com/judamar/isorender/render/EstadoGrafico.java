package com.judamar.isorender.render;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.init.Blocks;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/**
 * T-022 (RNF-010, RNF-005): UNICO punto que captura y restaura el estado grafico que toca el mod, al estado PREVIO REAL
 * (leido con las funciones glGet y glIsEnabled), no a un valor "vanilla" supuesto. No usa glPushAttrib/glPopAttrib: desincronizan la
 * cache de GlStateManager (dejaba el mundo en negro, ver T-014). Se restaura por GlStateManager para que su cache quede
 * coherente con OpenGL; lo que GlStateManager no cachea (framebuffer, viewport, matrices, scissor, linea, alineacion)
 * va directo a GL.
 *
 * Tres alcances: COMPLETO (render fuera de pantalla: todo, incluidos framebuffer, viewport, matrices y glGetError),
 * LIGERO (dibujo en el mundo, p. ej. el contorno: solo banderas y unidades de textura) y AJUSTES (solo los ajustes
 * del jugador que fuerza la construccion de geometria: oclusion ambiental y hojas). Solo hilo de render.
 */
public final class EstadoGrafico {
    private static final int UNIDADES = 2; // 0 = bloques/entidades, 1 = lightmap

    private final Minecraft mc;
    private final boolean gl, marco, ajustes;

    // Banderas y funciones
    private boolean blend, depth, depthMask, alpha, cull, lighting, luz0, luz1, colorMaterial, rescale;
    private int blendSrcRgb, blendDstRgb, blendSrcA, blendDstA, depthFunc, alphaFunc, shade;
    private float alphaRef, anchoLinea;
    private final FloatBuffer color = BufferUtils.createFloatBuffer(16);
    private final ByteBuffer mascara = BufferUtils.createByteBuffer(16);
    // Texturas por unidad + unidad activa
    private final boolean[] tex2d = new boolean[UNIDADES];
    private final int[] texId = new int[UNIDADES];
    private int unidadActiva;
    // Marco: framebuffer, viewport, scissor, matrices, alineacion de lectura
    private int framebuffer, packAlineacion, modoMatriz, profProy, profVista;
    private boolean scissor;
    private final int[] viewport = new int[4], cajaScissor = new int[4];
    private final FloatBuffer proy = BufferUtils.createFloatBuffer(16), vista = BufferUtils.createFloatBuffer(16);
    // Ajustes del jugador
    private int aoPrevio;
    private boolean[] hojasPrevias;

    private EstadoGrafico(Minecraft mc, boolean gl, boolean marco, boolean ajustes) {
        this.mc = mc; this.gl = gl; this.marco = marco; this.ajustes = ajustes;
    }

    public static EstadoGrafico capturarCompleto(Minecraft mc) { return new EstadoGrafico(mc, true, true, true).capturar(); }
    public static EstadoGrafico capturarLigero(Minecraft mc) { return new EstadoGrafico(mc, true, false, false).capturar(); }
    public static EstadoGrafico capturarAjustes(Minecraft mc) { return new EstadoGrafico(mc, false, false, true).capturar(); }

    private EstadoGrafico capturar() {
        if (marco) while (GL11.glGetError() != GL11.GL_NO_ERROR) { } // descarta errores ajenos: asi medimos solo los nuestros
        if (ajustes) {
            aoPrevio = mc.gameSettings.ambientOcclusion;
            hojasPrevias = new boolean[] {hoja(Blocks.LEAVES), hoja(Blocks.LEAVES2)};
        }
        if (gl) {
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB); blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            blendSrcA = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA); blendDstA = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST); depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST); alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
            alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
            cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
            luz0 = GL11.glIsEnabled(GL11.GL_LIGHT0); luz1 = GL11.glIsEnabled(GL11.GL_LIGHT1);
            colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
            rescale = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
            shade = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
            anchoLinea = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
            color.clear(); GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
            mascara.clear(); GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, mascara);
            // Texturas: se recorre cada unidad por GlStateManager (mantiene su cache) y se vuelve a la activa.
            unidadActiva = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            for (int u = 0; u < UNIDADES; u++) {
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + u);
                tex2d[u] = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
                texId[u] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            GlStateManager.setActiveTexture(unidadActiva);
        }
        if (marco) {
            framebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            IntBuffer ib = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_VIEWPORT, ib); ib.get(viewport);
            scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            ib.clear(); GL11.glGetInteger(GL11.GL_SCISSOR_BOX, ib); ib.get(cajaScissor);
            packAlineacion = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
            modoMatriz = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            profProy = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH); profVista = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
            proy.clear(); GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, proy);
            vista.clear(); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, vista);
        }
        return this;
    }

    /** Estado actual de la hoja (fancy o no); leavesFancy es protegido y no tiene getter. */
    private static boolean hoja(Block b) { return !b.isOpaqueCube(b.getDefaultState()); }

    /** Fuerza AO suave y hojas con capa determinista para construir la geometria; restaurar() los devuelve. */
    public void forzarAjustesGeometria() {
        mc.gameSettings.ambientOcclusion = 2;
        ((BlockLeaves) Blocks.LEAVES).setGraphicsLevel(true); ((BlockLeaves) Blocks.LEAVES2).setGraphicsLevel(true);
    }

    /**
     * Devuelve todo lo capturado al estado previo. Nunca lanza. En alcance COMPLETO devuelve el primer error de OpenGL
     * pendiente (0 si no hay) y lo limpia; el llamador decide si avisar.
     */
    public int restaurar() {
        int error = 0;
        try {
            if (ajustes) {
                mc.gameSettings.ambientOcclusion = aoPrevio;
                ((BlockLeaves) Blocks.LEAVES).setGraphicsLevel(hojasPrevias[0]); ((BlockLeaves) Blocks.LEAVES2).setGraphicsLevel(hojasPrevias[1]);
            }
            if (marco) restaurarMarco();
            if (gl) restaurarBanderas();
            if (marco) {
                // Errores acumulados durante el render: se registran en consola y se limpian para no contaminar al juego.
                for (int e = GL11.glGetError(); e != GL11.GL_NO_ERROR; e = GL11.glGetError()) {
                    if (error == 0) error = e;
                    System.err.println("[IsoRender] Pending OpenGL error after the render: " + Fallos.nombreErrorGl(e));
                }
            }
        } catch (Throwable t) { t.printStackTrace(); }
        return error;
    }

    private void restaurarMarco() {
        OpenGlHelper.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GlStateManager.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        // Matrices: se iguala la profundidad de la pila (un renderer que fallo a medias puede dejar pushes sin pop) y se recarga el valor.
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        while (GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH) > profProy) GL11.glPopMatrix();
        GL11.glLoadMatrix(proy);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > profVista) GL11.glPopMatrix();
        GL11.glLoadMatrix(vista);
        GL11.glMatrixMode(modoMatriz);
        if (scissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(cajaScissor[0], cajaScissor[1], cajaScissor[2], cajaScissor[3]);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, packAlineacion);
    }

    private void restaurarBanderas() {
        GlStateManager.tryBlendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcA, blendDstA);
        if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
        GlStateManager.depthFunc(depthFunc);
        GlStateManager.depthMask(depthMask);
        if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
        GlStateManager.alphaFunc(alphaFunc, alphaRef);
        if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
        if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
        if (luz0) GlStateManager.enableLight(0); else GlStateManager.disableLight(0);
        if (luz1) GlStateManager.enableLight(1); else GlStateManager.disableLight(1);
        if (colorMaterial) GlStateManager.enableColorMaterial(); else GlStateManager.disableColorMaterial();
        if (rescale) GlStateManager.enableRescaleNormal(); else GlStateManager.disableRescaleNormal();
        GlStateManager.shadeModel(shade);
        GlStateManager.colorMask(mascara.get(0) != 0, mascara.get(1) != 0, mascara.get(2) != 0, mascara.get(3) != 0);
        GL11.glLineWidth(anchoLinea);
        for (int u = 0; u < UNIDADES; u++) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + u);
            if (tex2d[u]) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            GlStateManager.bindTexture(texId[u]);
        }
        GlStateManager.setActiveTexture(unidadActiva);
        // Color: GlStateManager.color se salta la llamada si su cache ya coincide, pero tras dibujar con arrays de vertices
        // el color actual de GL es indefinido: se escribe tambien directo.
        float r = color.get(0), g = color.get(1), b = color.get(2), a = color.get(3);
        GlStateManager.color(r, g, b, a);
        GL11.glColor4f(r, g, b, a);
    }
}
