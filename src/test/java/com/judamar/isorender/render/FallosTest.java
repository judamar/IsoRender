package com.judamar.isorender.render;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.Test;

public class FallosTest {
    @Test public void memoria() {
        String m = Fallos.mensaje(new OutOfMemoryError("Java heap space"), "rendering");
        assertTrue(m.startsWith("Not enough memory"));
        assertFalse(m.contains("Java heap space"));
    }
    @Test public void archivo() {
        assertTrue(Fallos.mensaje(new IOException("Permission denied"), "rendering").contains("Could not save"));
    }
    @Test public void validacionPropiaSeMuestraTal() {
        assertEquals("Resolution out of range", Fallos.mensaje(new IllegalArgumentException("Resolution out of range"), "x"));
        assertEquals("sin FBO", Fallos.mensaje(new IllegalStateException("sin FBO"), "x"));
    }
    @Test public void inesperadoNoVuelcaTraza() {
        String m = Fallos.mensaje(new NullPointerException(), "opening the menu");
        assertTrue(m.contains("opening the menu") && m.contains("NullPointerException") && m.contains("console"));
        assertFalse(m.contains("\n") || m.contains(" at "));
    }
    @Test public void mensajeLargoOMultilineaSeAcorta() {
        StringBuilder sb = new StringBuilder("linea1\nlinea2");
        for (int i = 0; i < 400; i++) sb.append('x');
        String m = Fallos.mensaje(new IllegalStateException(sb.toString()), "x");
        assertTrue(m.length() <= Fallos.MAX);
        assertFalse(m.contains("\n"));
        assertTrue(Fallos.mensaje(new IllegalStateException(), "x").startsWith("Unexpected error")); // sin mensaje
    }
    @Test public void nombresGl() {
        assertEquals("GL_OUT_OF_MEMORY", Fallos.nombreErrorGl(0x0505));
        assertEquals("0x9999", Fallos.nombreErrorGl(0x9999));
    }
}
