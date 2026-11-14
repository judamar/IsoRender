package com.judamar.isorender.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/** T-015/T-019 (RF-025, RNF-009): FBO cuadrado de lado N con profundidad. Usar con try/finally y close(). Solo hilo de render. */
public final class FramebufferRender implements AutoCloseable {
    private final int lado;
    private Framebuffer fbo;

    /** Lado máximo que la GPU admite como textura y como renderbuffer (profundidad). */
    public static int maximoGpu() {
        return Math.min(GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE), GL11.glGetInteger(GL30.GL_MAX_RENDERBUFFER_SIZE));
    }

    /** VRAM libre en bytes si el driver la expone (extension NVX de NVIDIA); -1 si no se puede saber. */
    public static long vramLibre() {
        try {
            if (org.lwjgl.opengl.GLContext.getCapabilities().GL_NVX_gpu_memory_info)
                return GL11.glGetInteger(org.lwjgl.opengl.NVXGpuMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX) * 1024L;
        } catch (Throwable e) { /* sin dato: no se limita por VRAM */ }
        return -1;
    }

    /**
     * El lado es el interno (puede ser el doble del de salida por el supermuestreo, T-019); el rango de salida lo valida
     * el llamador. Lanza IllegalStateException con mensaje en inglés; no deja recursos a medias.
     */
    public FramebufferRender(int lado) {
        this.lado = lado;
        int max = maximoGpu();
        if (lado > max) throw new IllegalStateException("Your graphics card does not support " + lado
                + " px (maximum " + max + " px). Choose a lower resolution.");
        while (GL11.glGetError() != GL11.GL_NO_ERROR) { } // limpiar errores ajenos antes de medir los nuestros
        try {
            fbo = new Framebuffer(lado, lado, true);
            int e = GL11.glGetError();
            if (e != GL11.GL_NO_ERROR) throw new IllegalStateException("OpenGL error (0x" + Integer.toHexString(e)
                    + ") while creating the " + lado + " px framebuffer; possibly out of video memory.");
            fbo.bindFramebuffer(false);
            int st = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (st != GL30.GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("Incomplete framebuffer (0x"
                    + Integer.toHexString(st) + ") at " + lado + " px; possibly out of video memory.");
        } catch (Throwable ex) { // incluye OutOfMemoryError: no dejar el FBO a medias
            close();
            throw ex;
        }
    }

    public int lado() { return lado; }

    /** Enlaza el FBO (fija el viewport a lado x lado) y lo limpia a transparente (alfa 0) con profundidad. */
    public void enlazarYLimpiar() {
        fbo.bindFramebuffer(true);
        // glClear respeta colorMask, depthMask y scissor: el estado heredado del juego puede impedir que el
        // clear borre el color (fondo opaco y restos de renders anteriores en memoria de VRAM reutilizada).
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.depthMask(true);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        fbo.setFramebufferColor(0, 0, 0, 0);
        fbo.framebufferClear();
        // framebufferClear() de MC 1.12.2 termina con unbindFramebuffer(): hay que reenlazar o se dibuja
        // (y se lee) en el framebuffer principal de la ventana. bindFramebuffer(true) tambien fija el viewport.
        fbo.bindFramebuffer(true);
    }

    /** T-025: enlaza la textura de color del FBO para dibujarla como imagen (vista previa del menú). */
    public void enlazarTextura() { fbo.bindFramebufferTexture(); }

    /**
     * Lee {@code filas} filas RGBA (origen abajo-izquierda) desde la fila y0 del FBO enlazado. Por franjas para no
     * reservar de golpe lado*lado*4 bytes (a 8192 px serían 256 MB). El buffer debe caber en lado*filas*4.
     */
    public void leerFranja(ByteBuffer destino, int y0, int filas) {
        destino.clear();
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, y0, lado, filas, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, destino);
    }

    /** Libera el FBO y devuelve el framebuffer principal con su viewport. Idempotente. */
    @Override
    public void close() {
        if (fbo != null) { fbo.deleteFramebuffer(); fbo = null; }
        Minecraft mc = Minecraft.getMinecraft();
        mc.getFramebuffer().bindFramebuffer(true);
    }
}
