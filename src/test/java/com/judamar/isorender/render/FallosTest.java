package com.judamar.isorender.render;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;

public class FallosTest {
    @Test public void memoria() {
        String m = Fallos.mensaje(new OutOfMemoryError("Java heap space"), "renderizar");
        assertTrue(m.startsWith("Memoria insuficiente"));
        assertFalse(m.contains("Java heap space"));
    }
    @Test public void archivo() {
        assertTrue(Fallos.mensaje(new IOException("Permission denied"), "renderizar").contains("No se pudo guardar"));
    }
    @Test public void validacionPropiaSeMuestraTal() {
        assertEquals("Resolución fuera de rango", Fallos.mensaje(new IllegalArgumentException("Resolución fuera de rango"), "x"));
        assertEquals("sin FBO", Fallos.mensaje(new IllegalStateException("sin FBO"), "x"));
    }
    @Test public void inesperadoNoVuelcaTraza() {
        String m = Fallos.mensaje(new NullPointerException(), "abrir el menú");
        assertTrue(m.contains("abrir el menú") && m.contains("NullPointerException") && m.contains("consola"));
        assertFalse(m.contains("\n") || m.contains(" at "));
    }
    @Test public void mensajeLargoOMultilineaSeAcorta() {
        StringBuilder sb = new StringBuilder("linea1\nlinea2");
        for (int i = 0; i < 400; i++) sb.append('x');
        String m = Fallos.mensaje(new IllegalStateException(sb.toString()), "x");
        assertTrue(m.length() <= Fallos.MAX);
        assertFalse(m.contains("\n"));
        assertTrue(Fallos.mensaje(new IllegalStateException(), "x").startsWith("Error inesperado")); // sin mensaje
    }
    @Test public void nombresGl() {
        assertEquals("GL_OUT_OF_MEMORY", Fallos.nombreErrorGl(0x0505));
        assertEquals("0x9999", Fallos.nombreErrorGl(0x9999));
    }
}
