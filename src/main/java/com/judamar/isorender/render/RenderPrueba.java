package com.judamar.isorender.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.block.BlockLeaves;
import net.minecraft.init.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/** T-011 (prototipo): render isometrico 8x8x8 a FBO propio y PNG con alfa. Solo lectura del mundo. */
public final class RenderPrueba {
    private RenderPrueba() { }

    /** Estado actual de la hoja (fancy o no); leavesFancy es protegido y no tiene getter. */
    private static boolean hoja(net.minecraft.block.Block b) { return !b.isOpaqueCube(b.getDefaultState()); }

    private static void chat(Minecraft mc, String s) {
        if (mc.player != null) mc.player.sendMessage(new TextComponentString("[IsoRender] " + s));
    }

    /** Debe llamarse desde el hilo de render (RenderListener.onRender). Nunca lanza. */
    public static void ejecutar(Minecraft mc, int[] reg, int res, ParametrosCamara cam) {
        long t0 = System.currentTimeMillis();
        FramebufferRender fbo = null;
        boolean mats = false, lm = false, hojasForzadas = false;
        int aoPrevio = -1;
        boolean[] hojasPrevias = null;
        try {
            World w = mc.world;
            if (w == null || mc.player == null) throw new IllegalStateException("no hay mundo cargado");
            BlockPos origen = new BlockPos(reg[0], reg[1], reg[2]);
            int nx = reg[3] - reg[0] + 1, ny = reg[4] - reg[1] + 1, nz = reg[5] - reg[2] + 1;

            // (a) Geometria por capa. Vista de solo lectura: aire fuera de la region (caras de borde visibles).
            // Ajustes del jugador forzados durante el render (AO suave y hojas con capa determinista); se restauran en finally.
            VistaRegion vista = new VistaRegion(w, reg);
            aoPrevio = mc.gameSettings.ambientOcclusion; mc.gameSettings.ambientOcclusion = 2;
            hojasPrevias = new boolean[]{hoja(Blocks.LEAVES), hoja(Blocks.LEAVES2)};
            ((BlockLeaves) Blocks.LEAVES).setGraphicsLevel(true); ((BlockLeaves) Blocks.LEAVES2).setGraphicsLevel(true);
            hojasForzadas = true;
            int ini = GeometriaRegion.bytesIniciales(reg);
            BufferBuilder[] capas = new BufferBuilder[GeometriaRegion.ORDEN_CAPAS.length];
            for (int i = 0; i < capas.length; i++) { capas[i] = new BufferBuilder(ini); capas[i].begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK); }
            int dibujados = 0, translucidos = 0, fallidos = 0;
            // getAllInBoxMutable reutiliza un solo BlockPos: sin objetos por bloque.
            for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(origen, new BlockPos(reg[3], reg[4], reg[5]))) {
                if (!w.isBlockLoaded(p)) continue;
                IBlockState s = w.getBlockState(p);
                if (s.getMaterial() == Material.AIR) continue;
                int k = GeometriaRegion.indiceCapa(s.getBlock().getBlockLayer().name());
                if (k < 0) { translucidos++; continue; } // TRANSLUCENT: lo dibuja T-020
                try {
                    // renderBlock llama a getActualState (orientacion, conexiones, mitades) con la vista.
                    if (mc.getBlockRendererDispatcher().renderBlock(s, p, vista, capas[k])) dibujados++;
                } catch (Throwable e) { fallidos++; } // bloque de mod u otro fallo: se omite (RN-010)
            }
            for (BufferBuilder b : capas) b.finishDrawing();
            if (dibujados == 0) throw new IllegalStateException("la región solo contiene aire"
                    + (translucidos > 0 ? " o bloques translúcidos (aún no soportados)" : ""));
            if (translucidos > 0) chat(mc, translucidos + " bloques translúcidos omitidos");
            if (fallidos > 0) chat(mc, fallidos + " bloques no se pudieron dibujar y se omitieron");

            // (b) Render fuera de pantalla: aislar estado GL.
            fbo = new FramebufferRender(res); // valida rango y límites de GPU; lanza con mensaje en español
            // Sin glPushAttrib/glPopAttrib: desincroniza la caché de GlStateManager. Se restaura a mano en finally.
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.pushMatrix(); mats = true;

            fbo.enlazarYLimpiar(); // viewport res x res; limpia con alfa 0 y profundidad

            // Cámara ortográfica parametrizable con encuadre automático (T-016).
            CamaraGL.aplicar(cam, nx, ny, nz, origen.getX(), origen.getY(), origen.getZ());

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
            mc.entityRenderer.disableLightmap(); lm = true;
            // Orden: SOLID, CUTOUT_MIPPED, CUTOUT; alphaFunc GREATER 0.1 recorta cristal, hierba alta, antorchas.
            for (BufferBuilder b : capas) new WorldVertexBufferUploader().draw(b); // dibuja y resetea el buffer

            // Lectura de pixeles del FBO (RGBA) y volcado a PNG con alfa (se invierte el eje Y).
            ByteBuffer px = fbo.leerPixeles();
            int RES = res;
            BufferedImage img = new BufferedImage(RES, RES, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < RES; y++) {
                for (int x = 0; x < RES; x++) {
                    int i = ((RES - 1 - y) * RES + x) * 4;
                    img.setRGB(x, y, (px.get(i + 3) & 255) << 24 | (px.get(i) & 255) << 16
                            | (px.get(i + 1) & 255) << 8 | (px.get(i + 2) & 255));
                }
            }
            File dir = new File(mc.mcDataDir, "isorender-test");
            dir.mkdirs();
            File out = new File(dir, "prueba-" + RES + "-" + System.currentTimeMillis() + ".png");
            ImageIO.write(img, "png", out);
            chat(mc, "Prueba lista " + RES + "x" + RES + " px (" + dibujados + " bloques) en " + out.getPath()
                    + " [" + (System.currentTimeMillis() - t0) + " ms]");
        } catch (OutOfMemoryError e) {
            chat(mc, "Memoria insuficiente para dibujar la región; usa una región más pequeña o menos resolución.");
        } catch (Throwable e) {
            chat(mc, "Error en la prueba de render: " + e);
            e.printStackTrace();
        } finally {
            // Restaurar siempre: matrices, atributos GL, framebuffer principal y su viewport.
            try {
                if (aoPrevio >= 0) mc.gameSettings.ambientOcclusion = aoPrevio;
                if (hojasForzadas) { ((BlockLeaves) Blocks.LEAVES).setGraphicsLevel(hojasPrevias[0]); ((BlockLeaves) Blocks.LEAVES2).setGraphicsLevel(hojasPrevias[1]); }
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
