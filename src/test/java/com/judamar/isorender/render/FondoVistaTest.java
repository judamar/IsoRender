package com.judamar.isorender.render;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FondoVistaTest {
    @Test public void siguienteRecorreTodosYVuelveAlPrimero() {
        FondoVista f = FondoVista.CHECKERBOARD;
        for (int i = 0; i < FondoVista.values().length; i++) f = f.siguiente();
        assertEquals(FondoVista.CHECKERBOARD, f);
        assertEquals(FondoVista.WHITE, FondoVista.CHECKERBOARD.siguiente());
    }
    @Test public void lisosSonOpacos() {
        for (FondoVista f : FondoVista.values()) if (f != FondoVista.CHECKERBOARD) assertEquals(0xFF, f.argb >>> 24);
    }
}
