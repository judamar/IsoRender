package com.judamar.isorender.render;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/** Aplica proyección ortográfica o en perspectiva + modelview de ParametrosCamara. El llamador guarda/restaura las matrices. */
public final class CamaraGL {
    private CamaraGL() { }

    /** Región de nx*ny*nz bloques con esquina mínima (ox,oy,oz). Deja matrixMode en MODELVIEW. */
    public static void aplicar(ParametrosCamara p, int nx, int ny, int nz, double ox, double oy, double oz) {
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        if (p.perspectiva) {
            // Perspectiva: la cámara se aleja D del centro; el zoom y el desplazamiento mueven la ventana del frustum
            // (en el plano near), no la cámara. Mismo sentido de desplazamiento que en la rama ortográfica.
            double d = p.distanciaPerspectiva(nx, ny, nz);
            double[] pl = p.planosPerspectiva(nx, ny, nz);
            double h = pl[0] * Math.tan(Math.toRadians(ParametrosCamara.FOV_GRADOS / 2)) * 100 / p.zoom;
            double cx = -p.despX * h, cy = -p.despY * h;
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GL11.glFrustum(cx - h, cx + h, cy - h, cy + h, pl[0], pl[1]); // glFrustum multiplica la identidad ya cargada
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.translate(0, 0, -d);
        } else {
            double r = p.semiAncho(nx, ny, nz);
            // El desplazamiento mueve la estructura en pantalla: la ventana se mueve en sentido contrario.
            double cx = -p.despX * r, cy = -p.despY * r;
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.ortho(cx - r, cx + r, cy - r, cy + r, -256, 256);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }
        // Modelview: rotX(elevación) * rotY(rotación) * traslación al centro de la región.
        GlStateManager.rotate((float) p.elevacion, 1, 0, 0);
        GlStateManager.rotate((float) p.rotacion, 0, 1, 0);
        GlStateManager.translate(-(ox + nx / 2.0), -(oy + ny / 2.0), -(oz + nz / 2.0));
    }
}
