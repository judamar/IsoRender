package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class ResumenEntidadesTest {
    @Test public void sinEntidadesNoHayAvisos() {
        assertNull(ResumenEntidades.NINGUNA.avisoDibujadas());
        assertNull(ResumenEntidades.NINGUNA.avisoFallidas());
    }
    @Test public void avisosSingularYPlural() {
        assertEquals("1 block entity drawn", new ResumenEntidades(1, 0).avisoDibujadas());
        assertEquals("3 block entities drawn", new ResumenEntidades(3, 0).avisoDibujadas());
        assertEquals("1 block entity could not be drawn and was skipped", new ResumenEntidades(2, 1).avisoFallidas());
        assertEquals("2 block entities could not be drawn and were skipped", new ResumenEntidades(0, 2).avisoFallidas());
    }
    @Test public void regionSoloConEntidadesNoEstaVacia() {
        assertTrue(new ResumenGeometria(0, 0, 0).vacio());
        assertFalse(new ResumenGeometria(0, 0, 1).vacio()); // p. ej. solo un cofre
        assertFalse(new ResumenGeometria(5, 0, 0).vacio());
    }

    @Test public void decorativasSinEntidadesNoHayAvisos() {
        assertNull(ResumenEntidades.decorativas(0, 0).avisoDibujadas());
        assertNull(ResumenEntidades.decorativas(0, 0).avisoFallidas());
    }
    @Test public void decorativasSingularYPlural() {
        assertEquals("1 decorative entity drawn", ResumenEntidades.decorativas(1, 0).avisoDibujadas());
        assertEquals("4 decorative entities drawn", ResumenEntidades.decorativas(4, 0).avisoDibujadas());
        assertEquals("1 decorative entity could not be drawn and was skipped", ResumenEntidades.decorativas(2, 1).avisoFallidas());
        assertEquals("3 decorative entities could not be drawn and were skipped", ResumenEntidades.decorativas(0, 3).avisoFallidas());
    }
    @Test public void constructorAntiguoConservaTextoDeBloque() {
        assertTrue(new ResumenEntidades(2, 1).avisoDibujadas().contains("block entities"));
        assertTrue(new ResumenEntidades(1, 1).avisoFallidas().contains("block entity"));
    }
}
