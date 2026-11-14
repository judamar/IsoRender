package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class ParametrosCamaraTest {
    private static double[] proy(ParametrosCamara p, double x, double y, double z) {
        double r = Math.toRadians(p.rotacion), e = Math.toRadians(p.elevacion);
        double xp = x * Math.cos(r) + z * Math.sin(r), zp = -x * Math.sin(r) + z * Math.cos(r);
        return new double[] {xp, y * Math.cos(e) - zp * Math.sin(e)};
    }

    private static void contiene(ParametrosCamara p, int nx, int ny, int nz) {
        double s = p.semiAnchoBase(nx, ny, nz);
        for (int i = 0; i < 8; i++) {
            double[] q = proy(p, (i & 1) == 0 ? -nx / 2.0 : nx / 2.0, (i & 2) == 0 ? -ny / 2.0 : ny / 2.0, (i & 4) == 0 ? -nz / 2.0 : nz / 2.0);
            assertTrue(Math.abs(q[0]) <= s * (1 - ParametrosCamara.MARGEN) + 1e-9 && Math.abs(q[1]) <= s * (1 - ParametrosCamara.MARGEN) + 1e-9);
        }
    }

    @Test public void ca010_esquinasContenidas() {
        int[][] regs = {{64, 1, 1}, {1, 1, 64}, {1, 64, 1}, {64, 64, 64}, {10, 3, 40}};
        for (int[] g : regs)
            for (double rot = 0; rot <= 359; rot += 13)
                for (double el : new double[] {5, 35.264, 60, 90})
                    contiene(ParametrosCamara.crear(rot, el, 100, 0, 0), g[0], g[1], g[2]);
    }

    @Test public void margenReal() {
        // Cubo visto desde arriba (elev 90, rot 0): esquina a 0.5, semi-ancho = 0.5/0.95
        assertEquals(0.5 / 0.95, ParametrosCamara.crear(0, 90, 100, 0, 0).semiAnchoBase(1, 1, 1), 1e-9);
    }

    @Test public void zoomDivideSemiAncho() {
        ParametrosCamara p = ParametrosCamara.crear(45, 35.264, 200, 0, 0);
        assertEquals(ParametrosCamara.POR_DEFECTO.semiAncho(8, 8, 8) / 2, p.semiAncho(8, 8, 8), 1e-9);
    }

    @Test public void porDefecto() {
        ParametrosCamara p = ParametrosCamara.POR_DEFECTO;
        assertEquals(45, p.rotacion, 0); assertEquals(35.264, p.elevacion, 0);
        assertEquals(100, p.zoom, 0); assertEquals(0, p.despX, 0); assertEquals(0, p.despY, 0);
    }

    @Test public void limitesRechazan() {
        double[][] malos = {{-1, 45, 100, 0, 0}, {360, 45, 100, 0, 0}, {0, 4.9, 100, 0, 0}, {0, 90.1, 100, 0, 0},
                {0, 45, 24, 0, 0}, {0, 45, 401, 0, 0}, {0, 45, 100, 1.01, 0}, {0, 45, 100, 0, -1.01}, {Double.NaN, 45, 100, 0, 0}};
        for (double[] m : malos)
            try { ParametrosCamara.crear(m[0], m[1], m[2], m[3], m[4]); fail(); } catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("between")); }
        ParametrosCamara.crear(0, 5, 25, -1, 1); ParametrosCamara.crear(359, 90, 400, 1, -1);
    }

    @Test public void limitadoAjusta() {
        ParametrosCamara p = ParametrosCamara.limitado(500, 0, 1000, -5, 5);
        assertEquals(359, p.rotacion, 0); assertEquals(5, p.elevacion, 0); assertEquals(400, p.zoom, 0);
        assertEquals(-1, p.despX, 0); assertEquals(1, p.despY, 0);
    }

    @Test public void parsearArgumentos() {
        assertEquals(ParametrosCamara.POR_DEFECTO.rotacion, ParametrosCamara.parsear(new String[0]).rotacion, 0);
        ParametrosCamara p = ParametrosCamara.parsear(new String[] {"90", "30,5", "150"});
        assertEquals(90, p.rotacion, 0); assertEquals(30.5, p.elevacion, 0); assertEquals(150, p.zoom, 0);
        try { ParametrosCamara.parsear(new String[] {"x"}); fail(); } catch (IllegalArgumentException e) { }
        try { ParametrosCamara.parsear(new String[] {"45", "35", "999"}); fail(); } catch (IllegalArgumentException e) { }
    }

    @Test public void igualdadPorValor() {
        ParametrosCamara a = ParametrosCamara.crear(10, 20, 100, 0.5, 0), b = ParametrosCamara.crear(10, 20, 100, 0.5, 0);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, ParametrosCamara.crear(11, 20, 100, 0.5, 0));
        assertNotEquals(a, ParametrosCamara.crear(10, 20, 100, 0.5, 0.1));
    }

    private static final int[][] TAMANOS = {{1, 1, 1}, {64, 1, 64}, {1, 64, 1}, {64, 64, 64}, {10, 3, 40}};

    @Test public void perspectivaEncuadraLasEsquinas() {
        double t = Math.tan(Math.toRadians(ParametrosCamara.FOV_GRADOS / 2));
        for (int[] g : TAMANOS)
            for (double rot = 0; rot <= 359; rot += 15)
                for (double el = 5; el <= 90; el += 5) {
                    ParametrosCamara p = ParametrosCamara.crear(rot, el, 100, 0, 0, true);
                    double d = p.distanciaPerspectiva(g[0], g[1], g[2]), near = p.planosPerspectiva(g[0], g[1], g[2])[0];
                    double r = Math.toRadians(rot), e = Math.toRadians(el);
                    for (int i = 0; i < 8; i++) {
                        double x = (i & 1) == 0 ? -g[0] / 2.0 : g[0] / 2.0, y = (i & 2) == 0 ? -g[1] / 2.0 : g[1] / 2.0,
                                z = (i & 4) == 0 ? -g[2] / 2.0 : g[2] / 2.0;
                        double xp = x * Math.cos(r) + z * Math.sin(r), zp = -x * Math.sin(r) + z * Math.cos(r);
                        double yp = y * Math.cos(e) - zp * Math.sin(e), c = y * Math.sin(e) + zp * Math.cos(e);
                        double prof = d - c; // distancia delante de la camara
                        assertTrue(prof > near && near > 0);
                        assertTrue(Math.abs(xp) / (prof * t) <= 1 - ParametrosCamara.MARGEN + 1e-9);
                        assertTrue(Math.abs(yp) / (prof * t) <= 1 - ParametrosCamara.MARGEN + 1e-9);
                    }
                }
    }

    @Test public void perspectivaDistanciaYPlanos() {
        for (int[] g : TAMANOS)
            for (double rot : new double[] {0, 45, 200})
                for (double el : new double[] {5, 35.264, 90}) {
                    ParametrosCamara p = ParametrosCamara.crear(rot, el, 100, 0, 0, true);
                    double rad = 0.5 * Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
                    double[] pl = p.planosPerspectiva(g[0], g[1], g[2]);
                    assertTrue(p.distanciaPerspectiva(g[0], g[1], g[2]) > rad);
                    assertTrue(pl[1] > pl[0] && pl[0] > 0);
                }
    }

    @Test public void perspectivaEnIgualdadYConstructores() {
        ParametrosCamara a = ParametrosCamara.crear(10, 20, 100, 0, 0, true);
        assertNotEquals(a, ParametrosCamara.crear(10, 20, 100, 0, 0));
        assertNotEquals(a.hashCode(), ParametrosCamara.crear(10, 20, 100, 0, 0).hashCode());
        assertEquals(a, ParametrosCamara.crear(10, 20, 100, 0, 0).conPerspectiva(true));
        assertFalse(ParametrosCamara.POR_DEFECTO.perspectiva);
        assertFalse(ParametrosCamara.crear(10, 20, 100, 0, 0).perspectiva);
        assertFalse(ParametrosCamara.limitado(10, 20, 100, 0, 0).perspectiva);
        assertTrue(ParametrosCamara.limitado(10, 20, 100, 0, 0, true).perspectiva);
    }

    @Test public void parsearVista() {
        String[] base = {"45", "35", "100"};
        assertFalse(ParametrosCamara.parsear(base).perspectiva);
        assertTrue(ParametrosCamara.parsear(new String[] {"45", "35", "100", "persp"}).perspectiva);
        assertFalse(ParametrosCamara.parsear(new String[] {"45", "35", "100", "iso"}).perspectiva);
        try { ParametrosCamara.parsear(new String[] {"45", "35", "100", "otra"}); fail(); }
        catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("Invalid view")); }
    }
}
