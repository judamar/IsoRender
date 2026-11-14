package com.judamar.isorender.seleccion;

import static org.junit.Assert.*;
import org.junit.Test;

public class SeleccionTest {
    @Test public void ca005_1_dimensiones() {
        Seleccion s = new Seleccion();
        s.fijarPunto1(0, 64, 0); s.fijarPunto2(9, 69, 4);
        assertArrayEquals(new int[] {10, 6, 5}, s.dimensiones());
    }
    @Test public void ca005_2_ordenInverso() {
        Seleccion a = new Seleccion(), b = new Seleccion();
        a.fijarPunto1(0, 64, 0); a.fijarPunto2(9, 69, 4);
        b.fijarPunto1(9, 69, 4); b.fijarPunto2(0, 64, 0);
        assertArrayEquals(a.region(), b.region());
        assertArrayEquals(new int[] {0, 64, 0, 9, 69, 4}, b.region());
    }
    @Test public void ca004_2_mismoPunto() {
        Seleccion s = new Seleccion();
        s.fijarPunto1(-3, 5, 7); s.fijarPunto2(-3, 5, 7);
        assertArrayEquals(new int[] {1, 1, 1}, s.dimensiones());
    }
    @Test public void incompletaSinRegion() {
        Seleccion s = new Seleccion();
        assertNull(s.region());
        s.fijarPunto1(1, 2, 3);
        assertTrue(s.tienePunto1()); assertFalse(s.completa()); assertNull(s.dimensiones());
    }
    @Test public void reemplazoDePuntos() {
        Seleccion s = new Seleccion();
        s.fijarPunto1(0, 0, 0); s.fijarPunto2(1, 1, 1); s.fijarPunto1(5, 5, 5);
        assertArrayEquals(new int[] {5, 5, 5}, s.punto1());
        assertArrayEquals(new int[] {1, 1, 1, 5, 5, 5}, s.region());
    }
    @Test public void ca008_clear() {
        Seleccion s = new Seleccion();
        s.fijarPunto1(0, 0, 0); s.fijarPunto2(1, 1, 1);
        assertTrue(s.limpiar());
        assertFalse(s.tienePunto1()); assertFalse(s.tienePunto2());
        assertFalse(s.limpiar()); // CA-008.2: sin selección, sin error
    }
    @Test public void parseo() {
        assertNull(Subcomando.parsear("/say hola"));
        assertNull(Subcomando.parsear("/isorenderx"));
        assertEquals(Subcomando.POS1, Subcomando.parsear(" /isorender pos1 "));
        assertEquals(Subcomando.POS2, Subcomando.parsear("/isorender pos2"));
        assertEquals(Subcomando.CLEAR, Subcomando.parsear("/isorender clear"));
        assertEquals(Subcomando.MENU, Subcomando.parsear("/isorender menu"));
        assertEquals(Subcomando.PRUEBA_MULTI, Subcomando.parsear("/isorender prueba-multi"));
        assertEquals(Subcomando.PRUEBA_MULTI, Subcomando.parsear("/isorender prueba-multi 6"));
        assertEquals("6", Subcomando.argumento("/isorender prueba-multi 6"));
        assertEquals(Subcomando.USO, Subcomando.parsear("/isorender menu 6"));
        assertEquals(Subcomando.USO, Subcomando.parsear("/isorender"));
        assertEquals(Subcomando.USO, Subcomando.parsear("/isorender foo"));
    }
}
