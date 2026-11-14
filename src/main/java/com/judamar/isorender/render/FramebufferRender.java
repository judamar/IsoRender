package com.judamar.isorender.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/** T-015 (RF-025, RNF-009): FBO cuadrado de lado N con profundidad. Usar con try/finally y close(). Solo hilo de render. */
public final class FramebufferRender implements AutoCloseable {
    private final int lado;
    private Framebuffer fbo;

    /** Lanza IllegalArgumentException/IllegalStateException con mensaje en español; no deja recursos a medias. */
    public FramebufferRender(int lado) {
        String err = ResolucionSalida.validar(lado);
        if (err != null) throw new IllegalArgumentException(err);
        this.lado = lado;
        // El hardware debe soportar el tamaño como textura y como renderbuffer (profundidad).
        int maxTex = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        int maxRb = GL11.glGetInteger(GL30.GL_MAX_RENDERBUFFER_SIZE);
        int max = Math.min(maxTex, maxRb);
        if (lado > max) throw new IllegalStateException("Tu tarjeta gráfica no soporta " + lado
                + " px (máximo " + max + " px). Elige una resolución menor.");
        while (GL11.glGetError() != GL11.GL_NO_ERROR) { } // limpiar errores ajenos antes de medir los nuestros
        try {
            fbo = new Framebuffer(lado, lado, true);
            int e = GL11.glGetError();
            if (e != GL11.GL_NO_ERROR) throw new IllegalStateException("Error de OpenGL (0x" + Integer.toHexString(e)
                    + ") al crear el framebuffer de " + lado + " px; posible falta de memoria de vídeo.");
        } catch (RuntimeException ex) {
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

    /** Lee los píxeles RGBA (origen abajo-izquierda) del FBO enlazado. */
    public ByteBuffer leerPixeles() {
        ByteBuffer px = BufferUtils.createByteBuffer(lado * lado * 4);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, lado, lado, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px);
        return px;
    }

    /** Libera el FBO y devuelve el framebuffer principal con su viewport. Idempotente. */
    @Override
    public void close() {
        if (fbo != null) { fbo.deleteFramebuffer(); fbo = null; }
        Minecraft mc = Minecraft.getMinecraft();
        mc.getFramebuffer().bindFramebuffer(true);
    }
}
