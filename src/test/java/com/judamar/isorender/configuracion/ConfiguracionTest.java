package com.judamar.isorender.configuracion;

import static org.junit.Assert.*;

import com.judamar.isorender.render.FondoVista;
import com.judamar.isorender.render.ParametrosCamara;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ConfiguracionTest {
    private final List<String> avisos = new ArrayList<>();

    private Configuracion leer(String texto) { return Configuracion.leer(new StringReader(texto), avisos); }

    @Test public void validosModificados() {
        Configuracion c = leer("outputFolder=salidas/x\nresolution=2048\nrotation=90\nelevation=20.5\nzoom=150\n"
                + "offsetX=-10\noffsetY=25\nview=persp\nbackground=green\nmaxSize=32\n");
        assertTrue(avisos.isEmpty());
        assertEquals("salidas/x", c.carpetaSalida);
        assertEquals(2048, c.resolucion);
        assertEquals(ParametrosCamara.crear(90, 20.5, 150, -0.10, 0.25, true), c.camara);
        assertEquals(FondoVista.GREEN, c.fondo);
        assertEquals(32, c.tamanoMaximo);
    }

    @Test public void vacioEsPorDefectoSinAvisos() {
        assertEquals(Configuracion.POR_DEFECTO, leer(""));
        assertTrue(avisos.isEmpty());
    }

    @Test public void resolucionInvalidaAislada() {
        Configuracion c = leer("resolution=99999\nzoom=200\nbackground=BLACK\n");
        assertEquals(Configuracion.POR_DEFECTO.resolucion, c.resolucion);
        assertEquals(200, c.camara.zoom, 0);
        assertEquals(FondoVista.BLACK, c.fondo);
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("resolution"));
    }

    @Test public void resolucionNoNumerica() {
        assertEquals(Configuracion.POR_DEFECTO, leer("resolution=grande"));
        assertEquals(1, avisos.size());
    }

    @Test public void camaraFueraDeRango() {
        Configuracion c = leer("rotation=400\nelevation=2\nzoom=1000\noffsetX=150\noffsetY=abc\nresolution=1024");
        assertEquals(ParametrosCamara.POR_DEFECTO, c.camara);
        assertEquals(1024, c.resolucion);
        assertEquals(5, avisos.size());
    }

    @Test public void camaraValidaConUnaMala() {
        Configuracion c = leer("rotation=10\nzoom=0");
        assertEquals(10, c.camara.rotacion, 0);
        assertEquals(ParametrosCamara.POR_DEFECTO.zoom, c.camara.zoom, 0);
        assertEquals(1, avisos.size());
    }

    @Test public void vistaInvalida() {
        assertFalse(leer("view=3d").camara.perspectiva);
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("view"));
    }

    @Test public void fondoInvalido() {
        assertEquals(FondoVista.CHECKERBOARD, leer("background=PINK").fondo);
        assertEquals(1, avisos.size());
    }

    @Test public void tamanoMaximoFueraDeRango() {
        assertEquals(64, leer("maxSize=0").tamanoMaximo);
        assertEquals(64, leer("maxSize=100").tamanoMaximo);
        assertEquals(2, avisos.size());
        assertEquals(1, leer("maxSize=1").tamanoMaximo);
        assertEquals(64, leer("maxSize=64").tamanoMaximo);
    }

    @Test public void comaDecimal() {
        Configuracion c = leer("elevation=35,5\noffsetX=2,5");
        assertTrue(avisos.isEmpty());
        assertEquals(35.5, c.camara.elevacion, 0);
        assertEquals(0.025, c.camara.despX, 1e-12);
    }

    @Test public void corrupto() {
        assertEquals(Configuracion.POR_DEFECTO, leer("zoom=200\nclave=\\uZZ"));
        assertEquals(1, avisos.size());
    }

    @Test public void idaYVuelta() throws Exception {
        Configuracion c = new Configuracion("C:\\mis renders\\ü=x", 4096,
                ParametrosCamara.crear(123, 47.3, 250, 0.12, -0.07, true), FondoVista.GRAY, 40);
        StringWriter w = new StringWriter();
        c.escribir(w);
        assertTrue(w.toString().contains("elevation=47.3"));
        assertEquals(c, leer(w.toString()));
        assertTrue(avisos.isEmpty());
    }

    @Test public void idaYVueltaDefecto() throws Exception {
        StringWriter w = new StringWriter();
        Configuracion.POR_DEFECTO.escribir(w);
        assertEquals(Configuracion.POR_DEFECTO, leer(w.toString()));
        assertTrue(avisos.isEmpty());
    }
}
