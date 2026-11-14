package com.judamar.isorender.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.judamar.isorender.render.ParametrosCamara;
import org.junit.Test;

public class ParametroTest {
    private static final ParametrosCamara D = ParametrosCamara.POR_DEFECTO;

    @Test public void valoresPorDefecto() {
        assertEquals(45, Parametro.ROTACION.valor(D), 0);
        assertEquals(35.264, Parametro.ELEVACION.valor(D), 0);
        assertEquals(100, Parametro.ZOOM.valor(D), 0);
        assertEquals(0, Parametro.DESP_X.valor(D), 0);
    }
    @Test public void limitaAlRango() { // CA-017.2, CA-018.2
        EstadoCamara e = new EstadoCamara();
        assertEquals(359, e.fijar(Parametro.ROTACION, 720), 0);
        assertEquals(5, e.fijar(Parametro.ELEVACION, -3), 0);
        assertEquals(400, e.fijar(Parametro.ZOOM, 9999), 0);
        assertEquals(25, e.fijar(Parametro.ZOOM, 1), 0);
        assertEquals(100, e.fijar(Parametro.DESP_X, 250), 0);
        assertEquals(-100, e.fijar(Parametro.DESP_Y, -250), 0);
    }
    @Test public void fijarConservaLosDemas() {
        EstadoCamara e = new EstadoCamara();
        e.fijar(Parametro.ROTACION, 30);
        assertEquals(30, e.actual().rotacion, 0);
        assertEquals(35.264, e.actual().elevacion, 0);
        e.fijar(Parametro.DESP_X, 50);
        assertEquals(0.5, e.actual().despX, 1e-12);
        assertEquals(30, e.actual().rotacion, 0);
    }
    @Test public void fraccionIda() {
        assertEquals(0, Parametro.ZOOM.fraccion(25), 1e-12);
        assertEquals(1, Parametro.ZOOM.fraccion(400), 1e-12);
        assertEquals(0.5, Parametro.DESP_X.fraccion(0), 1e-12);
        assertEquals(200, Parametro.DESP_X.deFraccion(2) + 100, 0); // fuera de rango: tope
        assertEquals(-100, Parametro.DESP_X.deFraccion(-1), 0);
        assertEquals(0, Parametro.ROTACION.deFraccion(0), 0);
        assertEquals(359, Parametro.ROTACION.deFraccion(1), 0);
    }
    @Test public void formato() {
        assertEquals("45", Parametro.formato(45));
        assertEquals("35,3", Parametro.formato(35.264));
        assertEquals("-12,5", Parametro.formato(-12.5));
        assertEquals("45°", Parametro.ROTACION.conUnidad(45));
    }
    @Test public void leerNumeros() { // CA-023.1
        assertEquals(30, Parametro.ROTACION.leer("30"), 0);
        assertEquals(12.5, Parametro.ZOOM.leer(" 12,5 "), 0);
        assertEquals(-4, Parametro.DESP_X.leer("-4"), 0);
    }
    @Test public void leerRechazaTexto() { // CA-023.2
        for (String s : new String[] {"abc", "", "NaN", "Infinity", "1e999x"}) {
            try { Parametro.ROTACION.leer(s); fail(s); }
            catch (IllegalArgumentException e) { assertEquals(true, e.getMessage().startsWith("Rotación no válido")); }
        }
    }
}
