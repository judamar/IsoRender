package com.judamar.isorender.menu;

import static org.junit.Assert.assertEquals;

import com.judamar.isorender.render.FondoVista;
import com.judamar.isorender.render.ParametrosCamara;
import org.junit.Test;

public class EstadoCamaraTest {
    @Test public void restablecerVuelveALosValoresPorDefecto() { // CA-022.1
        EstadoCamara e = new EstadoCamara();
        e.fijar(Parametro.ROTACION, 10);
        e.fijar(Parametro.ZOOM, 300);
        e.fijar(Parametro.DESP_X, 40);
        e.alternarPerspectiva();
        FondoVista fondo = e.siguienteFondo();
        e.fijarResolucion(2048);
        e.restablecer();
        assertEquals(ParametrosCamara.POR_DEFECTO, e.actual());
        assertEquals(fondo, e.fondo());   // no toca el fondo
        assertEquals(2048, e.resolucion()); // ni la resolución
    }
    @Test public void resolucion() { // RF-020
        EstadoCamara e = new EstadoCamara();
        assertEquals(512, e.resolucion());
        e.fijarResolucion(4096);
        assertEquals(4096, e.resolucion());
    }
    @Test public void resolucionFueraDeRangoSeRechazaYConserva() { // CA-020.2
        EstadoCamara e = new EstadoCamara();
        e.fijarResolucion(1024);
        for (int v : new int[] {511, 4097}) {
            try { e.fijarResolucion(v); org.junit.Assert.fail(); } catch (IllegalArgumentException ex) { }
        }
        assertEquals(1024, e.resolucion());
    }
}
