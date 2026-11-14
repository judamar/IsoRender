package com.judamar.isorender.render;

import static org.junit.Assert.*;
import org.junit.Test;

public class OrdenTranslucidoTest {
    private static final int IV = 7; // enteros por vertice de DefaultVertexFormats.BLOCK

    /** Cara de 4 vertices con centroide (cx,cy,cz); el resto de enteros de cada vertice lleva la marca. */
    private static int[] cara(float cx, float cy, float cz, int marca) {
        int[] d = new int[4 * IV];
        for (int v = 0; v < 4; v++) {
            d[v * IV] = Float.floatToRawIntBits(cx + (v < 2 ? -0.5f : 0.5f));
            d[v * IV + 1] = Float.floatToRawIntBits(cy + (v % 2 == 0 ? -0.5f : 0.5f));
            d[v * IV + 2] = Float.floatToRawIntBits(cz);
            for (int k = 3; k < IV; k++) d[v * IV + k] = marca;
        }
        return d;
    }

    private static int[] unir(int[]... caras) {
        int[] r = new int[caras.length * 4 * IV];
        for (int i = 0; i < caras.length; i++) System.arraycopy(caras[i], 0, r, i * 4 * IV, 4 * IV);
        return r;
    }

    private static int marca(int[] d, int cara) { return d[cara * 4 * IV + 3]; }

    @Test public void rotacion0MiraDesdeMasZ() {
        // Con rotacion 0 y elevacion 0 la camara esta en +z: z mayor = mas cerca.
        assertTrue(OrdenTranslucido.cercania(0, 0, 5, 0, 0) > OrdenTranslucido.cercania(0, 0, -5, 0, 0));
        // Elevacion 90: se mira desde arriba, y mayor = mas cerca.
        assertTrue(OrdenTranslucido.cercania(0, 5, 0, 0, 90) > OrdenTranslucido.cercania(0, -5, 0, 0, 90));
        // Rotacion 90 (cadena de CamaraGL): x menor = mas cerca.
        assertTrue(OrdenTranslucido.cercania(-5, 0, 0, 90, 0) > OrdenTranslucido.cercania(5, 0, 0, 90, 0));
    }

    @Test public void ordenaDeLejosACerca() {
        int[] d = unir(cara(0, 0, 3, 1), cara(0, 0, -3, 2), cara(0, 0, 0, 3));
        int[] o = OrdenTranslucido.ordenar(d, IV, 0, 0);
        assertEquals(2, marca(o, 0)); // z = -3, la mas lejana primero
        assertEquals(3, marca(o, 1));
        assertEquals(1, marca(o, 2)); // z = 3, la mas cercana al final
        assertEquals(d.length, o.length);
    }

    @Test public void cambiaConLaRotacion() {
        int[] d = unir(cara(3, 0, 0, 1), cara(-3, 0, 0, 2));
        // rotacion 0: ninguna esta mas cerca por x; rotacion 90: x menor mas cerca; 270: x mayor mas cerca.
        assertEquals(1, marca(OrdenTranslucido.ordenar(d, IV, 90, 0), 0));
        assertEquals(2, marca(OrdenTranslucido.ordenar(d, IV, 270, 0), 0));
    }

    @Test public void empateConservaElOrdenOriginal() {
        int[] d = unir(cara(1, 0, 0, 1), cara(-1, 0, 0, 2), cara(0, 0, 0, 3));
        int[] o = OrdenTranslucido.ordenar(d, IV, 0, 0);
        assertEquals(1, marca(o, 0)); assertEquals(2, marca(o, 1)); assertEquals(3, marca(o, 2));
    }

    @Test public void vacioYNoModificaElOriginal() {
        assertEquals(0, OrdenTranslucido.ordenar(new int[0], IV, 45, 35).length);
        int[] d = unir(cara(0, 0, 3, 1), cara(0, 0, -3, 2));
        int[] copia = d.clone();
        OrdenTranslucido.ordenar(d, IV, 0, 0);
        assertArrayEquals(copia, d);
    }
}
