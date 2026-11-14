package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class EstimacionMemoriaTest {
    private static final long MB = EstimacionMemoria.MB;

    @Test public void libreCuentaLaBasuraComoUsada() {
        assertEquals(1000 * MB - 300 * MB, EstimacionMemoria.libre(1000 * MB, 500 * MB, 200 * MB));
    }

    @Test public void regionMaximaCabeEnDosGigas() { // RNF-014: 64^3 con 2 GB
        long vol = 64L * 64 * 64;
        assertNull(EstimacionMemoria.comprobarGeometria(vol, 1500 * MB));
        assertTrue(EstimacionMemoria.geometria(vol) < 128 * MB);
    }

    @Test public void geometriaRechazadaConMensajeEnEspanol() {
        String m = EstimacionMemoria.comprobarGeometria(64L * 64 * 64, 50 * MB);
        assertNotNull(m);
        assertTrue(m.startsWith("Memoria insuficiente") && m.contains("MB"));
    }

    @Test public void dibujoCrecePorResolucionYFactor() {
        assertTrue(EstimacionMemoria.dibujo(4096, 1) > EstimacionMemoria.dibujo(512, 1));
        assertTrue(EstimacionMemoria.dibujo(4096, 2) > EstimacionMemoria.dibujo(4096, 1));
        assertEquals(8192L * 8192 * 8, EstimacionMemoria.vram(4096, 2));
    }

    @Test public void factorBajaSiFaltaMemoria() {
        long con2 = (long) (EstimacionMemoria.dibujo(2048, 2) * EstimacionMemoria.MARGEN);
        long con1 = (long) (EstimacionMemoria.dibujo(2048, 1) * EstimacionMemoria.MARGEN) + 1;
        assertEquals(2, EstimacionMemoria.factor(2048, 2, con2 + 1, -1));
        assertEquals(1, EstimacionMemoria.factor(2048, 2, con2 - 1_000_000, -1));
        assertEquals(1, EstimacionMemoria.factor(2048, 2, con1 + 100, -1));
        assertEquals(0, EstimacionMemoria.factor(2048, 2, 1 * MB, -1));
        assertEquals(1, EstimacionMemoria.factor(512, 1, 500 * MB, -1));
    }

    @Test public void factorBajaPorVram() {
        long hp = 10_000 * MB;
        assertEquals(2, EstimacionMemoria.factor(4096, 2, hp, 2000 * MB));
        assertEquals(1, EstimacionMemoria.factor(4096, 2, hp, 400 * MB));
        assertEquals(0, EstimacionMemoria.factor(4096, 2, hp, 10 * MB));
        assertEquals(2, EstimacionMemoria.factor(4096, 2, hp, -1)); // VRAM desconocida: no limita
    }
}
