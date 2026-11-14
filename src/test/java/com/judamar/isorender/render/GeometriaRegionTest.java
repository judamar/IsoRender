package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class GeometriaRegionTest {
    private final int[] r = {10, 5, -3, 19, 10, 1};
    @Test public void contieneBordes() {
        assertTrue(GeometriaRegion.contiene(r, 10, 5, -3));
        assertTrue(GeometriaRegion.contiene(r, 19, 10, 1));
        assertFalse(GeometriaRegion.contiene(r, 20, 7, 0));
        assertFalse(GeometriaRegion.contiene(r, 9, 7, 0));
        assertFalse(GeometriaRegion.contiene(r, 12, 4, 0));
        assertFalse(GeometriaRegion.contiene(r, 12, 7, 2));
    }
    @Test public void volumen() {
        assertEquals(10 * 6 * 5, GeometriaRegion.volumen(r));
        assertEquals(64L * 64 * 64, GeometriaRegion.volumen(new int[]{0, 0, 0, 63, 63, 63}));
    }
    @Test public void ordenDeCapas() {
        assertEquals(0, GeometriaRegion.indiceCapa("SOLID"));
        assertEquals(1, GeometriaRegion.indiceCapa("CUTOUT_MIPPED"));
        assertEquals(2, GeometriaRegion.indiceCapa("CUTOUT"));
        assertEquals(3, GeometriaRegion.indiceCapa("TRANSLUCENT"));
        assertEquals(GeometriaRegion.CAPA_TRANSLUCIDA, GeometriaRegion.ORDEN_CAPAS.length - 1);
        assertEquals(-1, GeometriaRegion.indiceCapa("INVENTADA"));
    }
    @Test public void tamanoInicialAcotado() {
        assertEquals(300 * 64, GeometriaRegion.bytesIniciales(r));
        assertEquals(64 << 20, GeometriaRegion.bytesIniciales(new int[]{0, 0, 0, 999, 999, 999}));
    }

    @Test public void claveDeCache() {
        assertTrue(GeometriaRegion.misma(r, r.clone()));
        assertFalse(GeometriaRegion.misma(r, new int[]{10, 5, -3, 19, 10, 2}));
        assertFalse(GeometriaRegion.misma(r, null));
        assertFalse(GeometriaRegion.misma(null, null));
    }
    @Test public void cantidadMulti() {
        assertEquals(4, GeometriaRegion.parsearN(""));
        assertEquals(4, GeometriaRegion.parsearN(null));
        assertEquals(2, GeometriaRegion.parsearN(" 2 "));
        assertEquals(8, GeometriaRegion.parsearN("8"));
        for (String mal : new String[]{"1", "9", "-3", "abc", "2.5"}) {
            try { GeometriaRegion.parsearN(mal); fail(mal); } catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("2")); }
        }
    }
    @Test public void contadores() {
        ResumenGeometria v = new ResumenGeometria(0, 0);
        assertTrue(v.vacio());
        assertEquals("la región solo contiene aire", v.motivoVacio());
        ResumenGeometria g = new ResumenGeometria(10, 1);
        assertFalse(g.vacio());
        assertEquals(10, g.bloques); assertEquals(1, g.fallidos);
    }
}
