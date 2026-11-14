package com.judamar.isorender.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** T-011 (prototipo): render isometrico 8x8x8 a FBO propio y PNG con alfa. Solo lectura del mundo. */
public final class RenderPrueba {
    private static final int RES = 512;
    private static final double ELEVACION = 35.264, ROTACION = 45;

    private RenderPrueba() { }

    private static void chat(Minecraft mc, String s) {
        if (mc.player != null) mc.player.sendMessage(new TextComponentString("[IsoRender] " + s));
    }

    /** Debe llamarse desde el hilo de render (RenderListener.onRender). Nunca lanza. */
    public static void ejecutar(Minecraft mc, int[] reg) {
        long t0 = System.currentTimeMillis();
        Framebuffer fbo = null;
        boolean attrib = false, mats = false;
        try {
            World w = mc.world;
            if (w == null || mc.player == null) throw new IllegalStateException("no hay mundo cargado");
            BlockPos origen = new BlockPos(reg[0], reg[1], reg[2]);
            int nx = reg[3] - reg[0] + 1, ny = reg[4] - reg[1] + 1, nz = reg[5] - reg[2] + 1;

            // (a) Lectura de bloques: solo getBlockState/isBlockLoaded, nunca setBlockState.
            BufferBuilder buf = new BufferBuilder(1 << 18);
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            int dibujados = 0;
            for (BlockPos p : BlockPos.getAllInBox(origen, new BlockPos(reg[3], reg[4], reg[5]))) {
                if (!w.isBlockLoaded(p)) continue;
                IBlockState s = w.getBlockState(p);
                if (s.getMaterial() == Material.AIR) continue;
                // Geometria del bloque en un BufferBuilder propio (no se tocan los chunks del mundo).
                if (mc.getBlockRendererDispatcher().renderBlock(s, p, w, buf)) dibujados++;
            }
            buf.finishDrawing();
            if (dibujados == 0) throw new IllegalStateException("la región solo contiene aire");

            // (b) Render fuera de pantalla: aislar estado GL.
            fbo = new Framebuffer(RES, RES, true);
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); attrib = true;
            GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.pushMatrix(); mats = true;

            fbo.bindFramebuffer(true); // tambien fija el viewport a RES x RES
            fbo.setFramebufferColor(0, 0, 0, 0); // fondo transparente
            fbo.framebufferClear();

            // Camara ortografica isometrica: rotX(elevacion) * rotY(rotacion) * traslacion al centro.
            double r = Math.sqrt(nx * nx + ny * ny + nz * nz) / 2 + 0.5; // semi-diagonal: cabe en cualquier rotación
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(-r, r, -r, r, -256, 256);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.rotate((float) ELEVACION, 1, 0, 0);
            GlStateManager.rotate((float) ROTACION, 0, 1, 0);
            GlStateManager.translate(-(origen.getX() + nx / 2.0), -(origen.getY() + ny / 2.0), -(origen.getZ() + nz / 2.0));

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
            new WorldVertexBufferUploader().draw(buf); // dibuja y resetea el buffer

            // Lectura de pixeles del FBO (RGBA) y volcado a PNG con alfa (se invierte el eje Y).
            ByteBuffer px = BufferUtils.createByteBuffer(RES * RES * 4);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, RES, RES, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px);
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
            File out = new File(dir, "prueba-" + System.currentTimeMillis() + ".png");
            ImageIO.write(img, "png", out);
            chat(mc, "Prueba lista (" + dibujados + " bloques) en " + out.getPath()
                    + " [" + (System.currentTimeMillis() - t0) + " ms]");
        } catch (Throwable e) {
            chat(mc, "Error en la prueba de render: " + e);
            e.printStackTrace();
        } finally {
            // Restaurar siempre: matrices, atributos GL, framebuffer principal y su viewport.
            try {
                if (mats) {
                    GlStateManager.matrixMode(GL11.GL_PROJECTION); GlStateManager.popMatrix();
                    GlStateManager.matrixMode(GL11.GL_MODELVIEW); GlStateManager.popMatrix();
                }
                if (attrib) GL11.glPopAttrib();
                if (fbo != null) fbo.deleteFramebuffer();
                mc.getFramebuffer().bindFramebuffer(true);
            } catch (Throwable e) { e.printStackTrace(); }
        }
    }
}
