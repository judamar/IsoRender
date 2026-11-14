package com.judamar.isorender;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SmokeTest {
    @Test public void nombre() { assertEquals("IsoRender", new LiteModIsoRender().getName()); }
}
