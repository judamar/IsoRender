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
        assertEquals(-1, GeometriaRegion.indiceCapa("TRANSLUCENT"));
    }
    @Test public void tamanoInicialAcotado() {
        assertEquals(300 * 64, GeometriaRegion.bytesIniciales(r));
        assertEquals(64 << 20, GeometriaRegion.bytesIniciales(new int[]{0, 0, 0, 999, 999, 999}));
    }
}
