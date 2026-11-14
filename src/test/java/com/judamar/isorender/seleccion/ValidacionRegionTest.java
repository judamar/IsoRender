package com.judamar.isorender.seleccion;

import static org.junit.Assert.*;
import org.junit.Test;

public class ValidacionRegionTest {
    private static Seleccion sel(int dx, int dy, int dz) {
        Seleccion s = new Seleccion();
        s.fijarPunto1(0, 0, 0); s.fijarPunto2(dx - 1, dy - 1, dz - 1);
        return s;
    }
    @Test public void ninguno() { assertTrue(ValidacionRegion.validar(new Seleccion(), 64).contains("No hay selección")); }
    @Test public void soloPunto1() {
        Seleccion s = new Seleccion(); s.fijarPunto1(1, 2, 3);
        assertTrue(ValidacionRegion.validar(s, 64).contains("Falta el punto 2"));
    }
    @Test public void soloPunto2() {
        Seleccion s = new Seleccion(); s.fijarPunto2(1, 2, 3);
        assertTrue(ValidacionRegion.validar(s, 64).contains("Falta el punto 1"));
    }
    @Test public void limite64Valido() { assertNull(ValidacionRegion.validar(sel(64, 64, 64), 64)); }
    @Test public void x65() { assertTrue(ValidacionRegion.validar(sel(65, 1, 1), 64).contains("64")); }
    @Test public void y65() { assertNotNull(ValidacionRegion.validar(sel(1, 65, 1), 64)); }
    @Test public void z65() { assertNotNull(ValidacionRegion.validar(sel(1, 1, 65), 64)); }
    @Test public void feliz20() { assertNull(ValidacionRegion.validar(sel(20, 20, 20), 64)); }
}
