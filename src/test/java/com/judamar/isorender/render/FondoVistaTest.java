package com.judamar.isorender.render;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FondoVistaTest {
    @Test public void siguienteRecorreTodosYVuelveAlPrimero() {
        FondoVista f = FondoVista.TABLERO;
        for (int i = 0; i < FondoVista.values().length; i++) f = f.siguiente();
        assertEquals(FondoVista.TABLERO, f);
        assertEquals(FondoVista.BLANCO, FondoVista.TABLERO.siguiente());
    }
    @Test public void lisosSonOpacos() {
        for (FondoVista f : FondoVista.values()) if (f != FondoVista.TABLERO) assertEquals(0xFF, f.argb >>> 24);
    }
}
