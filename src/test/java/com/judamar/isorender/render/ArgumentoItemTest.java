package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class ArgumentoItemTest {
    @Test public void vacioEsMano() {
        assertNull(ArgumentoItem.parsear("").id);
        assertNull(ArgumentoItem.parsear("  ").id);
        assertEquals(0, ArgumentoItem.parsear("").meta);
    }
    @Test public void soloId() {
        ArgumentoItem a = ArgumentoItem.parsear("stone");
        assertEquals("stone", a.id);
        assertEquals(0, a.meta);
    }
    @Test public void idYMeta() {
        ArgumentoItem a = ArgumentoItem.parsear("minecraft:wool 14");
        assertEquals("minecraft:wool", a.id);
        assertEquals(14, a.meta);
        assertEquals(32767, ArgumentoItem.parsear("stone 32767").meta);
    }
    @Test public void metaInvalida() {
        for (String s : new String[] {"stone -1", "stone 40000", "stone x"}) {
            try { ArgumentoItem.parsear(s); fail(s); } catch (IllegalArgumentException e) { assertFalse(e.getMessage().isEmpty()); }
        }
    }
    @Test public void demasiadosArgumentos() {
        try { ArgumentoItem.parsear("stone 1 2"); fail(); } catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("Too many")); }
    }
}
