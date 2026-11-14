package com.judamar.isorender.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.opengl.GL11;

/**
 * T-011 (prototipo) / T-018: render isometrico a FBO propio y PNG con alfa. Dos fases separadas:
 * CONSTRUCCION de geometria (GeometriaCacheada, lee el mundo una vez) y DIBUJO (camara + FBO + lectura de pixeles,
 * repetible con otra camara sin releer el mundo). Solo lectura del mundo.
 */
public final class RenderPrueba {
    private RenderPrueba() { }

    /** Argumento de "/isorender prueba-multi [n]": entero 2-8; vacío = 4. Lanza IllegalArgumentException en español. */
    public static int parsearN(String arg) { return GeometriaRegion.parsearN(arg); }

    private static void chat(Minecraft mc, String s) {
        if (mc.player != null) mc.player.sendMessage(new TextComponentString("[IsoRender] " + s));
    }

    private static void avisosGeometria(Minecraft mc, GeometriaCacheada geo) {
        if (geo.translucidosOmitidos() > 0) chat(mc, geo.translucidosOmitidos() + " bloques translúcidos omitidos");
        if (geo.fallidos() > 0) chat(mc, geo.fallidos() + " bloques no se pudieron dibujar y se omitieron");
    }

    private static void errorAlChat(Minecraft mc, Throwable e) {
        if (e instanceof OutOfMemoryError) chat(mc, "Memoria insuficiente para dibujar la región; usa una región más pequeña o menos resolución.");
        else { chat(mc, "Error en la prueba de render: " + e); e.printStackTrace(); }
    }

    private static void guardar(BufferedImage img, File out) throws java.io.IOException {
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
    }

    /** Debe llamarse desde el hilo de render (RenderListener.onRender). Nunca lanza. */
    public static void ejecutar(Minecraft mc, int[] reg, int res, ParametrosCamara cam) {
        long t0 = System.currentTimeMillis();
        // (a) Construccion: lee el mundo y sube la geometria; close() la libera siempre.
        try (GeometriaCacheada geo = GeometriaCacheada.construir(mc, reg)) {
            avisosGeometria(mc, geo);
            // (b) Dibujo con la camara dada.
            BufferedImage img = dibujar(mc, geo, reg, res, cam);
            File out = new File(new File(mc.mcDataDir, "isorender-test"), "prueba-" + res + "-" + System.currentTimeMillis() + ".png");
            guardar(img, out);
            chat(mc, "Prueba lista " + res + "x" + res + " px (" + geo.bloques() + " bloques) en " + out.getPath()
                    + " [" + (System.currentTimeMillis() - t0) + " ms]");
        } catch (Throwable e) {
            errorAlChat(mc, e);
        }
    }

    /**
     * T-018: construye la geometria UNA vez y genera n PNG de 512 px con rotaciones repartidas en 0..360.
     * Informa el tiempo de construccion y el tiempo medio de dibujo (FBO + dibujo + lectura de pixeles; sin PNG).
     */
    public static void ejecutarMulti(Minecraft mc, int[] reg, int n) {
        final int res = 512;
        try {
            long t0 = System.nanoTime();
            try (GeometriaCacheada geo = GeometriaCacheada.construir(mc, reg)) {
                long construccion = System.nanoTime() - t0;
                avisosGeometria(mc, geo);
                long totalDibujo = 0;
                File dir = new File(mc.mcDataDir, "isorender-test");
                long marca = System.currentTimeMillis();
                for (int i = 0; i < n; i++) {
                    double rot = 360.0 * i / n;
                    ParametrosCamara c = ParametrosCamara.crear(rot, ParametrosCamara.POR_DEFECTO.elevacion,
                            ParametrosCamara.POR_DEFECTO.zoom, 0, 0);
                    long t1 = System.nanoTime();
                    BufferedImage img = dibujar(mc, geo, reg, res, c); // reutiliza la geometria: no relee el mundo
                    totalDibujo += System.nanoTime() - t1;
                    guardar(img, new File(dir, "multi-" + marca + "-" + (i + 1) + "of" + n + "-rot" + Math.round(rot) + ".png"));
                }
                chat(mc, n + " imágenes de " + res + "x" + res + " px (" + geo.bloques() + " bloques) en " + dir.getPath());
                chat(mc, String.format("Construcción de geometría: %.1f ms (una vez). Dibujo medio por imagen: %.1f ms.",
                        construccion / 1e6, totalDibujo / 1e6 / n));
            }
        } catch (Throwable e) {
            errorAlChat(mc, e);
        }
    }

    /**
     * Dibuja la geometria cacheada con la camara dada en un FBO de res x res y devuelve la imagen ARGB.
     * Restaura el estado GL (matrices, framebuffer, banderas) al terminar o fallar.
     */
    private static BufferedImage dibujar(Minecraft mc, GeometriaCacheada geo, int[] reg, int res, ParametrosCamara cam) {
        FramebufferRender fbo = null;
        boolean mats = false;
        try {
            int nx = reg[3] - reg[0] + 1, ny = reg[4] - reg[1] + 1, nz = reg[5] - reg[2] + 1;
            // Render fuera de pantalla: aislar estado GL.
            fbo = new FramebufferRender(res); // valida rango y límites de GPU; lanza con mensaje en español
            // Sin glPushAttrib/glPopAttrib: desincroniza la caché de GlStateManager. Se restaura a mano en finally.
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.pushMatrix(); mats = true;

            fbo.enlazarYLimpiar(); // viewport res x res; limpia con alfa 0 y profundidad

            // Cámara ortográfica parametrizable con encuadre automático (T-016).
            CamaraGL.aplicar(cam, nx, ny, nz, reg[0], reg[1], reg[2]);

            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.disableCull();
            GlStateManager.disableLighting();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.enableTexture2D();
            GlStateManager.color(1, 1, 1, 1);
            mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE); // atlas de bloques
            // Lightmap del mundo apagado: la luz es plena y fija (mismo PNG de dia y de noche); el AO sigue en el color de vertice.
            mc.entityRenderer.disableLightmap();
            // Orden: SOLID, CUTOUT_MIPPED, CUTOUT; alphaFunc GREATER 0.1 recorta cristal, hierba alta, antorchas.
            geo.dibujar(); // reproduce las display lists (sin releer el mundo)

            // Lectura de pixeles del FBO (RGBA) y volcado a ARGB (se invierte el eje Y).
            ByteBuffer px = fbo.leerPixeles();
            BufferedImage img = new BufferedImage(res, res, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < res; y++) {
                for (int x = 0; x < res; x++) {
                    int i = ((res - 1 - y) * res + x) * 4;
                    img.setRGB(x, y, (px.get(i + 3) & 255) << 24 | (px.get(i) & 255) << 16
                            | (px.get(i + 1) & 255) << 8 | (px.get(i + 2) & 255));
                }
            }
            return img;
        } finally {
            // Restaurar siempre: matrices, atributos GL, framebuffer principal y su viewport.
            try {
                if (mats) {
                    GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.popMatrix();
                    GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.popMatrix();
                }
                if (fbo != null) fbo.close(); // libera el FBO y reenlaza el framebuffer principal + viewport
                else mc.getFramebuffer().bindFramebuffer(true);
                // Estado vanilla tras el render del mundo (igual que ContornoSeleccion)
                GlStateManager.depthMask(true);
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.enableDepth();
                GlStateManager.enableCull();
                GlStateManager.disableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
                GlStateManager.enableTexture2D();
                GlStateManager.enableLighting();
                GlStateManager.color(1f, 1f, 1f, 1f);
            } catch (Throwable e) { e.printStackTrace(); }
        }
    }
}
