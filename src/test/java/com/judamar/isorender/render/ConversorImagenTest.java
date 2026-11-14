package com.judamar.isorender.render;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import org.junit.Test;

public class ConversorImagenTest {
    /** Buffer RGBA GL (fila 0 = abajo) de lado x lado, con pixeles dados en orden fila a fila: {r,g,b,a}. */
    private static ByteBuffer buf(int[][] px) {
        ByteBuffer b = ByteBuffer.allocate(px.length * 4);
        for (int[] p : px) for (int c : p) b.put((byte) c);
        b.flip();
        return b;
    }

    private static int[] reducir(int[][] px, int lado, int factor) {
        int[] out = new int[(lado / factor) * (lado / factor)];
        ConversorImagen.reducirFranja(buf(px), lado, 0, lado, factor, out);
        return out;
    }

    private static int mezclar(int argb, int fondo) { // composicion sobre fondo gris uniforme, canal rojo
        int a = argb >>> 24, r = argb >> 16 & 255;
        return (r * a + fondo * (255 - a) + 127) / 255;
    }

    @Test public void opacoFactor1() {
        assertEquals(0xFF336699, reducir(new int[][]{{0x33, 0x66, 0x99, 255}}, 1, 1)[0]);
    }

    @Test public void transparenteNormalizaRgb() {
        // RGB basura con alfa 0 no debe sobrevivir.
        assertEquals(0, reducir(new int[][]{{200, 10, 90, 0}}, 1, 1)[0]);
    }

    @Test public void semitransparenteSeConserva() {
        assertEquals(0x80FF0000, reducir(new int[][]{{255, 0, 0, 128}}, 1, 1)[0]);
    }

    @Test public void promedioPremultiplicadoSinHalo() {
        // 1 pixel rojo opaco + 3 transparentes con RGB blanco (basura): el borde debe ser rojo puro con alfa 64.
        int[] r = reducir(new int[][]{{255, 0, 0, 255}, {255, 255, 255, 0}, {255, 255, 255, 0}, {255, 255, 255, 0}}, 2, 2);
        assertEquals(1, r.length);
        assertEquals(64, r[0] >>> 24);
        assertEquals(0xFF0000, r[0] & 0xFFFFFF);
    }

    @Test public void bordeSinFranjaSobreBlancoYNegro() {
        // Mismo caso con el fondo transparente negro: el color tampoco cambia, asi que sobre blanco y negro
        // el pixel es exactamente alfa*rojo + (1-alfa)*fondo (sin canal verde/azul ajeno).
        for (int rgbFondo : new int[]{0, 255}) {
            int[] r = reducir(new int[][]{{255, 0, 0, 255}, {rgbFondo, rgbFondo, rgbFondo, 0},
                    {rgbFondo, rgbFondo, rgbFondo, 0}, {rgbFondo, rgbFondo, rgbFondo, 0}}, 2, 2);
            assertEquals(0xFF0000, r[0] & 0xFFFFFF);
            assertEquals((255 * 64 + 255 * 191 + 127) / 255, mezclar(r[0], 255)); // sobre blanco
            assertEquals((255 * 64 + 127) / 255, mezclar(r[0], 0));              // sobre negro
        }
    }

    @Test public void mediaPonderadaPorAlfa() {
        // Rojo opaco + azul con alfa 85: el color pondera por alfa (255:85), no 50/50.
        int[] r = reducir(new int[][]{{255, 0, 0, 255}, {0, 0, 255, 85}, {0, 0, 0, 0}, {0, 0, 0, 0}}, 2, 2);
        assertEquals((255 + 85 + 2) / 4, r[0] >>> 24);
        assertEquals(191, r[0] >> 16 & 255); // 255*255/340
        assertEquals(64, r[0] & 255);        // 85*255/340
    }

    @Test public void invierteEjeY() {
        // GL: fila 0 = abajo. Pixel (x=0,y=0) rojo, (0,1) verde -> en la imagen el verde queda arriba.
        int[] r = reducir(new int[][]{{255, 0, 0, 255}, {0, 0, 0, 0}, {0, 255, 0, 255}, {0, 0, 0, 0}}, 2, 1);
        assertEquals(0xFF00FF00, r[0]);          // fila superior, x=0
        assertEquals(0xFFFF0000, r[2]);          // fila inferior, x=0
    }

    @Test public void franjasEquivalenALecturaCompleta() {
        // 4x4 con factor 2, leido en 2 franjas de 2 filas: mismo resultado que de una vez.
        int[][] px = new int[16][];
        for (int i = 0; i < 16; i++) px[i] = new int[]{i * 16, 255 - i * 16, i, i % 3 == 0 ? 0 : 255};
        int[] completo = reducir(px, 4, 2);
        int[] porFranjas = new int[4];
        for (int y0 = 0; y0 < 4; y0 += 2) {
            int[][] parte = java.util.Arrays.copyOfRange(px, y0 * 4, (y0 + 2) * 4);
            ConversorImagen.reducirFranja(buf(parte), 4, y0, 2, 2, porFranjas);
        }
        assertArrayEquals(completo, porFranjas);
    }

    @Test public void factorSupermuestreo() {
        assertEquals(2, ResolucionSalida.factorSupermuestreo(2048, 16384));
        assertEquals(2, ResolucionSalida.factorSupermuestreo(4096, 8192));
        assertEquals(1, ResolucionSalida.factorSupermuestreo(4096, 4096));
        assertEquals(1, ResolucionSalida.factorSupermuestreo(2049, 4096));
    }
}
