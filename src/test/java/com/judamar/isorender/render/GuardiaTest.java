package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class GuardiaTest {
    @Test public void noReentrante() {
        Guardia g = new Guardia();
        assertTrue(g.entrar());
        assertTrue(g.ocupada());
        assertFalse(g.entrar());   // segundo intento mientras hay un render: rechazado
        g.salir();
        assertFalse(g.ocupada());
        assertTrue(g.entrar());    // libre otra vez tras salir (p. ej. tras un fallo con finally)
    }
}
