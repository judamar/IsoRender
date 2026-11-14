package com.judamar.isorender.configuracion;

import static org.junit.Assert.assertEquals;

import java.io.File;
import org.junit.Test;

public class AjustesTest {
    private final File base = new File("juego");

    @Test public void vaciaEsPorDefecto() {
        assertEquals(new File(base, "isorender"), Ajustes.resolverCarpeta("", base));
        assertEquals(new File(base, "isorender"), Ajustes.resolverCarpeta("  ", base));
    }

    @Test public void relativaSeResuelveContraBase() {
        assertEquals(new File(base, "renders/mis"), Ajustes.resolverCarpeta("renders/mis", base));
    }

    @Test public void absolutaTalCual() {
        File abs = new File("/tmp/renders").getAbsoluteFile();
        assertEquals(abs, Ajustes.resolverCarpeta(abs.getPath(), base));
    }
}
