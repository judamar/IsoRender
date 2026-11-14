package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class ResolucionSalidaTest {
    @Test public void limitesValidos() {
        assertNull(ResolucionSalida.validar(512));
        assertNull(ResolucionSalida.validar(4096));
        assertEquals(1024, ResolucionSalida.parsear(" 1024 "));
    }
    @Test public void defecto() {
        assertEquals(512, ResolucionSalida.parsear(null));
        assertEquals(512, ResolucionSalida.parsear(""));
    }
    @Test public void fueraDeRango() {
        for (int v : new int[]{100, 511, 4097, -5}) assertNotNull(ResolucionSalida.validar(v));
        try { ResolucionSalida.parsear("100"); fail(); }
        catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("512") && e.getMessage().contains("4096")); }
    }
    @Test(expected = IllegalArgumentException.class) public void noNumerico() { ResolucionSalida.parsear("abc"); }
    @Test public void siguienteRecorreElCiclo() {
        assertEquals(1024, ResolucionSalida.siguiente(512));
        assertEquals(2048, ResolucionSalida.siguiente(1024));
        assertEquals(4096, ResolucionSalida.siguiente(2048));
        assertEquals(512, ResolucionSalida.siguiente(4096));
        assertEquals(2048, ResolucionSalida.siguiente(1500));
        assertEquals(4096, ResolucionSalida.siguiente(3000));
        assertEquals(512, ResolucionSalida.siguiente(5000));
        assertEquals(512, ResolucionSalida.siguiente(100)); // por debajo del primero: el primero mayor es 512
    }
}
