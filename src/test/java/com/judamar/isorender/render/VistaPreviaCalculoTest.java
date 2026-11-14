package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class VistaPreviaCalculoTest {
    @Test public void recuadro427x240() {
        int[] r = VistaPreviaCalculo.recuadro(427, 240);
        assertArrayEquals(new int[] {260, 44, 136}, r); // zona 177 x 136: lado 136, centrado en x
    }

    @Test public void ventanaMinimaYDemasiadoPequena() {
        assertNotNull(VistaPreviaCalculo.recuadro(240 + 10 + 64, 44 + 60 + 64)); // justo 64
        assertNull(VistaPreviaCalculo.recuadro(240 + 10 + 63, 600));
        assertNull(VistaPreviaCalculo.recuadro(1000, 44 + 60 + 63));
        assertNull(VistaPreviaCalculo.recuadro(100, 100));
    }

    @Test public void sinSolapeConControlesNiBotones() {
        for (int w = 314; w <= 1000; w += 7)
            for (int h = 168; h <= 600; h += 7) {
                int[] r = VistaPreviaCalculo.recuadro(w, h);
                if (r == null) continue;
                assertTrue(r[0] >= 240);                    // controles y campos ocupan x < 240
                assertTrue(r[0] + r[2] <= w - 10);
                assertTrue(r[1] >= 44);                     // bajo el texto de selección
                assertTrue(r[1] + r[2] <= h - 60);          // botones desde h-52
                assertTrue(r[2] >= 64);
            }
    }

    @Test public void umbralSimplificado() {
        assertFalse(VistaPreviaCalculo.simplificado(32768));
        assertTrue(VistaPreviaCalculo.simplificado(32769));
        assertFalse(VistaPreviaCalculo.simplificado(32L * 32 * 32));
        assertTrue(VistaPreviaCalculo.simplificado(64L * 64 * 64));
        assertFalse(VistaPreviaCalculo.simplificado(1));
    }

    @Test public void ladoFboTopesYMinimo() {
        assertEquals(512, VistaPreviaCalculo.ladoFbo(400, 3, false));
        assertEquals(256, VistaPreviaCalculo.ladoFbo(400, 3, true));
        assertEquals(272, VistaPreviaCalculo.ladoFbo(136, 2, false));
        assertEquals(256, VistaPreviaCalculo.ladoFbo(136, 2, true));
        assertEquals(64, VistaPreviaCalculo.ladoFbo(20, 1, false));
        assertEquals(64, VistaPreviaCalculo.ladoFbo(10, 2, true));
    }

    @Test public void reordenar() {
        assertTrue(VistaPreviaCalculo.reordenar(false, 100, 100));
        assertTrue(VistaPreviaCalculo.reordenar(true, 1301, 1000));
        assertFalse(VistaPreviaCalculo.reordenar(true, 1300, 1000));
        assertFalse(VistaPreviaCalculo.reordenar(true, 1100, 1000));
    }
}
