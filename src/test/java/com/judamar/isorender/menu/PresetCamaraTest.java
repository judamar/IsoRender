package com.judamar.isorender.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.judamar.isorender.render.ParametrosCamara;
import org.junit.Test;

public class PresetCamaraTest {
    private static final ParametrosCamara D = ParametrosCamara.POR_DEFECTO;

    @Test public void valores() { // CA-022.2
        assertEquals(45, PresetCamara.ISOMETRICA.aplicar(D).rotacion, 0);
        assertEquals(35.264, PresetCamara.ISOMETRICA.aplicar(D).elevacion, 0);
        assertEquals(0, PresetCamara.FRONTAL.aplicar(D).rotacion, 0);
        assertEquals(5, PresetCamara.FRONTAL.aplicar(D).elevacion, 0);
        assertEquals(0, PresetCamara.SUPERIOR.aplicar(D).rotacion, 0);
        assertEquals(90, PresetCamara.SUPERIOR.aplicar(D).elevacion, 0);
    }
    @Test public void conservanZoomDesplazamientoYVista() {
        EstadoCamara e = new EstadoCamara();
        e.fijar(Parametro.ZOOM, 200);
        e.fijar(Parametro.DESP_X, 30);
        e.fijar(Parametro.DESP_Y, -20);
        e.alternarPerspectiva();
        for (PresetCamara p : PresetCamara.values()) {
            e.aplicar(p);
            assertEquals(p.rotacion, e.actual().rotacion, 0);
            assertEquals(p.elevacion, e.actual().elevacion, 0);
            assertEquals(200, e.actual().zoom, 0);
            assertEquals(0.3, e.actual().despX, 1e-12);
            assertEquals(-0.2, e.actual().despY, 1e-12);
            assertTrue(e.actual().perspectiva);
        }
    }
}
