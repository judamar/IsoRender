package com.judamar.isorender.render;

import java.io.IOException;
import net.minecraft.client.Minecraft;

/**
 * T-022 (RNF-005): cualquier fallo de seleccion, render o menu se informa en el chat con un mensaje corto en español
 * (nunca un stack trace) y el detalle va a la consola. El juego no se cierra.
 */
public final class Fallos {
    private Fallos() { }

    static final int MAX = 160;

    /** Mensaje corto para el chat. Logica pura. */
    public static String mensaje(Throwable e, String contexto) {
        String m = e.getMessage();
        if (e instanceof OutOfMemoryError) return "Memoria insuficiente para " + contexto + "; usa una región más pequeña o menos resolución.";
        if (e instanceof IOException) return "No se pudo guardar el archivo (" + corto(m) + "). Comprueba permisos y espacio en disco.";
        // Las excepciones de validacion propias llevan ya un mensaje en español.
        if ((e instanceof IllegalArgumentException || e instanceof IllegalStateException) && m != null && !m.trim().isEmpty()) return corto(m);
        return "Error inesperado al " + contexto + " (" + e.getClass().getSimpleName() + "). Detalles en la consola del juego.";
    }

    private static String corto(String m) {
        String t = m == null ? "sin detalle" : m.replaceAll("\\s+", " ").trim();
        return t.length() > MAX ? t.substring(0, MAX - 1) + "…" : t;
    }

    /** Nombre del codigo de error de OpenGL (para avisos legibles). */
    public static String nombreErrorGl(int e) {
        switch (e) {
            case 0x0500: return "GL_INVALID_ENUM";
            case 0x0501: return "GL_INVALID_VALUE";
            case 0x0502: return "GL_INVALID_OPERATION";
            case 0x0503: return "GL_STACK_OVERFLOW";
            case 0x0504: return "GL_STACK_UNDERFLOW";
            case 0x0505: return "GL_OUT_OF_MEMORY";
            case 0x0506: return "GL_INVALID_FRAMEBUFFER_OPERATION";
            default: return "0x" + Integer.toHexString(e);
        }
    }

    /** Traza completa a la consola/log y mensaje corto al chat. Nunca lanza. */
    public static void informar(Minecraft mc, String contexto, Throwable e) {
        try { e.printStackTrace(); } catch (Throwable ignorado) { }
        Chat.decir(mc, mensaje(e, contexto));
    }
}
