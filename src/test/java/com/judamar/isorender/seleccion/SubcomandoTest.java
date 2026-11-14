package com.judamar.isorender.seleccion;

import static org.junit.Assert.*;
import org.junit.Test;

public class SubcomandoTest {
    @Test public void estres() {
        assertEquals(Subcomando.ESTRES, Subcomando.parsear("/isorender estres"));
        assertEquals(Subcomando.ESTRES, Subcomando.parsear("/isorender estres 20"));
        assertEquals("20", Subcomando.argumento("/isorender estres 20"));
        assertEquals("", Subcomando.argumento("/isorender estres"));
    }
    @Test public void otrosSiguenIgual() {
        assertNull(Subcomando.parsear("/otro"));
        assertEquals(Subcomando.USO, Subcomando.parsear("/isorender pos1 x"));
        assertEquals(Subcomando.PRUEBA_MULTI, Subcomando.parsear("/isorender prueba-multi 3"));
        assertEquals(Subcomando.USO, Subcomando.parsear("/isorender nada"));
    }
}
