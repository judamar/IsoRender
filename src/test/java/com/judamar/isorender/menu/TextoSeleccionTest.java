package com.judamar.isorender.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.judamar.isorender.seleccion.Seleccion;
import org.junit.Test;

public class TextoSeleccionTest {
    @Test public void sinPuntos() {
        assertTrue(TextoSeleccion.describir(new Seleccion()).startsWith("Falta definir la selección"));
    }
    @Test public void faltaPunto2() {
        Seleccion s = new Seleccion(); s.fijarPunto1(0, 0, 0);
        assertTrue(TextoSeleccion.describir(s).contains("punto 2"));
    }
    @Test public void faltaPunto1() {
        Seleccion s = new Seleccion(); s.fijarPunto2(0, 0, 0);
        assertTrue(TextoSeleccion.describir(s).contains("punto 1"));
    }
    @Test public void completa() {
        Seleccion s = new Seleccion(); s.fijarPunto1(0, 0, 0); s.fijarPunto2(4, 2, 9);
        assertEquals("Región: 5 × 3 × 10 bloques", TextoSeleccion.describir(s));
    }
}
