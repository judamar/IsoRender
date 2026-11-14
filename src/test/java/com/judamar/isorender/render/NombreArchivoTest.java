package com.judamar.isorender.render;

import static org.junit.Assert.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NombreArchivoTest {
    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 14, 9, 5, 7);

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test public void formatoExacto() {
        assertEquals("estructura_20261114-090507_10,64,20_25,80,35.png", NombreArchivo.base(T, new int[]{10, 64, 20, 25, 80, 35}));
    }

    @Test public void coordenadasNegativasSonValidas() {
        String n = NombreArchivo.base(T, new int[]{-10, -5, -20, 3, 0, -1});
        assertEquals("estructura_20261114-090507_-10,-5,-20_3,0,-1.png", n);
        for (char c : "/\\:*?\"<>|".toCharArray()) assertEquals("caracter invalido " + c, -1, n.indexOf(c));
    }

    @Test public void unicoSinColision() {
        assertEquals("a.png", NombreArchivo.unico("a.png", n -> false));
    }

    @Test public void colisionAñade2YLuego3() {
        Set<String> hay = new HashSet<>();
        hay.add("a.png");
        assertEquals("a_2.png", NombreArchivo.unico("a.png", hay::contains));
        hay.add("a_2.png");
        assertEquals("a_3.png", NombreArchivo.unico("a.png", hay::contains));
    }

    @Test public void predicadoRecibeNombresConExtension() {
        Set<String> vistos = new HashSet<>();
        NombreArchivo.unico("a.png", n -> { vistos.add(n); return n.equals("a.png"); });
        assertTrue(vistos.contains("a.png") && vistos.contains("a_2.png"));
        for (String n : vistos) assertTrue(n.endsWith(".png"));
    }

    @Test public void mismoSegundoNoSePisa() {
        int[] reg = {0, 0, 0, 1, 1, 1};
        Set<String> hay = new HashSet<>();
        String a = NombreArchivo.unico(NombreArchivo.base(T, reg), hay::contains);
        hay.add(a);
        String b = NombreArchivo.unico(NombreArchivo.base(T, reg), hay::contains);
        assertNotEquals(a, b);
    }

    @Test public void guardarCreaLaCarpetaYElPng() throws IOException {
        File out = new File(tmp.getRoot(), "a/b/x.png");
        ConversorImagen.guardarPng(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), out);
        assertTrue(out.isFile() && out.length() > 0);
    }

    @Test public void rutaImposibleLanzaIOExceptionSinArchivo() throws IOException {
        File archivo = tmp.newFile("regular"); // el "padre" es un archivo: no se puede crear la carpeta
        File out = new File(archivo, "sub/x.png");
        try {
            ConversorImagen.guardarPng(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), out);
            fail("debia lanzar IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("no se pudo crear la carpeta"));
        }
        assertFalse(out.exists());
    }
}
