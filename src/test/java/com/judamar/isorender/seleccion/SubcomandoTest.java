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
    @Test public void render() {
        assertEquals(Subcomando.RENDER, Subcomando.parsear("/isorender render"));
        assertEquals(Subcomando.USO, Subcomando.parsear("/isorender render 2048"));
    }
    @Test public void item() {
        assertEquals(Subcomando.ITEM, Subcomando.parsear("/isorender item"));
        assertEquals(Subcomando.ITEM, Subcomando.parsear("/isorender item stone 1"));
        assertEquals("stone 1", Subcomando.argumento("/isorender item stone 1"));
        assertEquals("", Subcomando.argumento("/isorender item"));
        assertEquals(Subcomando.USO, Subcomando.parsear("/isorender pos1 x"));
    }
}
