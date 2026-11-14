package com.judamar.isorender.menu;

import static org.junit.Assert.assertEquals;

import com.judamar.isorender.configuracion.Configuracion;
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

    @Test public void sembrarYAConfiguracion() { // T-030
        Configuracion base = new Configuracion("salidas", 1024, ParametrosCamara.crear(10, 20, 150, 0.1, -0.2, true), FondoVista.GRIS, 32);
        EstadoCamara e = new EstadoCamara();
        e.sembrar(base);
        assertEquals(base.camara, e.actual());
        assertEquals(1024, e.resolucion());
        assertEquals(FondoVista.GRIS, e.fondo());
        e.fijar(Parametro.ZOOM, 300);
        Configuracion c = e.aConfiguracion(base);
        assertEquals(300, c.camara.zoom, 0);
        assertEquals("salidas", c.carpetaSalida);
        assertEquals(32, c.tamanoMaximo);
        e.restablecer(); // sigue siendo el valor de fábrica, no la configuración
        assertEquals(ParametrosCamara.POR_DEFECTO, e.actual());
    }
}
