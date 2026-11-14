package com.judamar.isorender.render;

import com.judamar.isorender.seleccion.Seleccion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.opengl.GL11;

/** T-014 (RF-006): caja de lineas de la seleccion. Solo dibuja; no toca el mundo. */
public final class ContornoSeleccion {
    private boolean desactivado;

    /** Llamar desde PostRenderListener.onPostRender (hilo de render, camara de mundo ya configurada). */
    public void dibujar(Minecraft mc, Seleccion sel, float partialTicks) {
        if (desactivado || mc.world == null) return;
        int[] p1 = sel.punto1(), p2 = sel.punto2();
        if (p1 == null && p2 == null) return;
        if (p1 == null) p1 = p2;
        if (p2 == null) p2 = p1;
        Entity cam = mc.getRenderViewEntity();
        if (cam == null) return;
        boolean tocado = false;
        try {
            // Compensar la camara: posicion interpolada del observador
            double cx = cam.lastTickPosX + (cam.posX - cam.lastTickPosX) * partialTicks;
            double cy = cam.lastTickPosY + (cam.posY - cam.lastTickPosY) * partialTicks;
            double cz = cam.lastTickPosZ + (cam.posZ - cam.lastTickPosZ) * partialTicks;
            double e = 0.002; // margen para evitar z-fighting con las caras de los bloques
            double x0 = Math.min(p1[0], p2[0]) - e - cx, x1 = Math.max(p1[0], p2[0]) + 1 + e - cx;
            double y0 = Math.min(p1[1], p2[1]) - e - cy, y1 = Math.max(p1[1], p2[1]) + 1 + e - cy;
            double z0 = Math.min(p1[2], p2[2]) - e - cz, z1 = Math.max(p1[2], p2[2]) + 1 + e - cz;

            // No se usa glPushAttrib/glPopAttrib: GlStateManager cachea el estado y un pop crudo lo desincroniza
            // (dejaba el mundo en negro). Se cambia solo via GlStateManager y se restaura a mano en finally.
            tocado = true;
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GL11.glLineWidth(2f);
            GlStateManager.depthMask(false);
            // Dos pasadas: visible tras bloques (tenue) y encima (nitida), para ver la caja desde dentro y detras de terreno
            GlStateManager.disableDepth();
            caja(x0, y0, z0, x1, y1, z1, 1f, 0.8f, 0f, 0.25f);
            GlStateManager.enableDepth();
            caja(x0, y0, z0, x1, y1, z1, 1f, 0.8f, 0f, 1f);
        } catch (Throwable t) {
            desactivado = true;
            t.printStackTrace();
            if (mc.player != null) mc.player.sendMessage(new TextComponentString(
                "[IsoRender] Error al dibujar el contorno; se desactiva hasta reiniciar el juego: " + t));
        } finally {
            if (tocado) {
                // Estado vanilla tras el render del mundo
                GlStateManager.depthMask(true);
                GlStateManager.enableDepth();
                GlStateManager.disableBlend();
                GlStateManager.enableCull();
                GlStateManager.enableTexture2D();
                GlStateManager.enableLighting();
                GlStateManager.color(1f, 1f, 1f, 1f);
                GL11.glLineWidth(1f);
            }
        }
    }

    private static void caja(double x0, double y0, double z0, double x1, double y1, double z1,
                             float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(GL11.GL_LINES);
        double[] xs = {x0, x1}, ys = {y0, y1}, zs = {z0, z1};
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
            line(xs[0], ys[i], zs[j], xs[1], ys[i], zs[j]); // aristas en X
            line(xs[i], ys[0], zs[j], xs[i], ys[1], zs[j]); // aristas en Y
            line(xs[i], ys[j], zs[0], xs[i], ys[j], zs[1]); // aristas en Z
        }
        GL11.glEnd();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void line(double ax, double ay, double az, double bx, double by, double bz) {
        GL11.glVertex3d(ax, ay, az);
        GL11.glVertex3d(bx, by, bz);
    }
}
