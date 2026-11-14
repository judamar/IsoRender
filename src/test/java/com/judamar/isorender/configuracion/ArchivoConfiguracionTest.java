package com.judamar.isorender.configuracion;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ArchivoConfiguracionTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    private final List<String> log = new ArrayList<>();

    @Test public void ausenteCreaArchivoConDefectos() throws Exception {
        File f = new File(tmp.getRoot(), "isorender.properties");
        assertEquals(Configuracion.POR_DEFECTO, ArchivoConfiguracion.cargar(f, log::add));
        assertTrue(f.isFile());
        assertTrue(log.isEmpty());
        assertEquals(Configuracion.POR_DEFECTO, ArchivoConfiguracion.cargar(f, log::add)); // lo creado se lee igual
    }

    @Test public void validoModificado() throws Exception {
        File f = tmp.newFile("c.properties");
        Files.write(f.toPath(), "resolucion=1024\ncarpetaSalida=ñandú\ntamanoMaximo=16\n".getBytes(StandardCharsets.UTF_8));
        Configuracion c = ArchivoConfiguracion.cargar(f, log::add);
        assertEquals(1024, c.resolucion);
        assertEquals("ñandú", c.carpetaSalida);
        assertEquals(16, c.tamanoMaximo);
        assertTrue(log.isEmpty());
    }

    @Test public void valorInvalidoVaAlLog() throws Exception {
        File f = tmp.newFile("c.properties");
        Files.write(f.toPath(), "resolucion=99999\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(Configuracion.POR_DEFECTO, ArchivoConfiguracion.cargar(f, log::add));
        assertEquals(1, log.size());
        assertTrue(log.get(0).contains("resolucion"));
    }

    @Test public void bytesBasura() throws Exception {
        File f = tmp.newFile("c.properties");
        Files.write(f.toPath(), new byte[] {(byte) 0xFF, (byte) 0xFE, (byte) 0x80, 0x00, (byte) 0xC3, 0x28});
        assertEquals(Configuracion.POR_DEFECTO, ArchivoConfiguracion.cargar(f, log::add));
        assertEquals(1, log.size());
    }

    @Test public void escapeCorrupto() throws Exception {
        File f = tmp.newFile("c.properties");
        Files.write(f.toPath(), "zoom=200\nx=\\uZZ".getBytes(StandardCharsets.UTF_8));
        assertEquals(Configuracion.POR_DEFECTO, ArchivoConfiguracion.cargar(f, log::add));
        assertEquals(1, log.size());
    }

    @Test public void guardarCreaDirectorioFaltante() {
        File f = new File(tmp.getRoot(), "a/b/c.properties");
        Configuracion c = new Configuracion("x", 2048, Configuracion.POR_DEFECTO.camara, Configuracion.POR_DEFECTO.fondo, 10);
        ArchivoConfiguracion.guardar(f, c, log::add);
        assertTrue(log.isEmpty());
        assertEquals(c, ArchivoConfiguracion.cargar(f, log::add));
        assertFalse(new File(f.getPath() + ".tmp").exists());
    }

    @Test public void guardarSobrescribe() {
        File f = new File(tmp.getRoot(), "c.properties");
        ArchivoConfiguracion.guardar(f, Configuracion.POR_DEFECTO, log::add);
        Configuracion c = new Configuracion("", 4096, Configuracion.POR_DEFECTO.camara, Configuracion.POR_DEFECTO.fondo, 64);
        ArchivoConfiguracion.guardar(f, c, log::add);
        assertEquals(c, ArchivoConfiguracion.cargar(f, log::add));
    }

    @Test public void guardarEnDirectorioImposibleNoLanza() throws Exception {
        File archivoRegular = tmp.newFile("no-es-directorio");
        ArchivoConfiguracion.guardar(new File(archivoRegular, "c.properties"), Configuracion.POR_DEFECTO, log::add);
        assertEquals(1, log.size());
    }
}
