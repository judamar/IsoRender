package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class ResumenEstresTest {
    @Test public void parsearN() {
        assertEquals(30, ResumenEstres.parsearN(null));
        assertEquals(30, ResumenEstres.parsearN("  "));
        assertEquals(1, ResumenEstres.parsearN("1"));
        assertEquals(50, ResumenEstres.parsearN(" 50 "));
        for (String malo : new String[]{"0", "51", "-3", "abc", "2.5"}) {
            try { ResumenEstres.parsearN(malo); fail(malo); }
            catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("1") && e.getMessage().contains("50")); }
        }
    }

    @Test public void mediaYVariacion() {
        ResumenEstres r = new ResumenEstres(3);
        r.registrar(10_000_000L, false); r.registrar(30_000_000L, false); r.registrar(99_000_000L, true);
        assertEquals(2, r.hechos); assertEquals(1, r.errores);
        assertEquals(20.0, r.mediaMs(), 1e-9);
        r.usadoTrasPrimero = 100L << 20; r.usadoDespues = 103L << 20;
        assertEquals(3.0, r.variacionPct(), 1e-9);
        assertEquals(3, r.lineas().length);
        assertTrue(r.lineas()[0].contains("2 de 3"));
    }

    @Test public void sinDatosNoDivideEntreCero() {
        ResumenEstres r = new ResumenEstres(5);
        assertEquals(0.0, r.mediaMs(), 0);
        assertEquals(0.0, r.variacionPct(), 0);
    }
}
