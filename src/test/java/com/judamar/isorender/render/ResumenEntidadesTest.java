package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class ResumenEntidadesTest {
    @Test public void sinEntidadesNoHayAvisos() {
        assertNull(ResumenEntidades.NINGUNA.avisoDibujadas());
        assertNull(ResumenEntidades.NINGUNA.avisoFallidas());
    }
    @Test public void avisosSingularYPlural() {
        assertEquals("1 entidad de bloque dibujada", new ResumenEntidades(1, 0).avisoDibujadas());
        assertEquals("3 entidades de bloque dibujadas", new ResumenEntidades(3, 0).avisoDibujadas());
        assertEquals("1 entidad de bloque no se pudo dibujar y se omitió", new ResumenEntidades(2, 1).avisoFallidas());
        assertEquals("2 entidades de bloque no se pudieron dibujar y se omitieron", new ResumenEntidades(0, 2).avisoFallidas());
    }
    @Test public void regionSoloConEntidadesNoEstaVacia() {
        assertTrue(new ResumenGeometria(0, 0, 0).vacio());
        assertFalse(new ResumenGeometria(0, 0, 1).vacio()); // p. ej. solo un cofre
        assertFalse(new ResumenGeometria(5, 0, 0).vacio());
    }
}
