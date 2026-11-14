package com.judamar.isorender.render;

import java.util.Arrays;
import java.util.Comparator;

/**
 * T-020 (RF-012, R-04): orden de dibujo de las caras translucidas, de atras hacia delante segun la camara.
 * Logica pura (sin Minecraft ni OpenGL). Los datos son los enteros crudos de un BufferBuilder de GL_QUADS:
 * cada cara son 4 vertices de {@code intsVertice} enteros y los 3 primeros de cada vertice son x,y,z (float en bits).
 *
 * La camara es ortografica, asi que la profundidad exacta es la coordenada z del espacio de vision (eje de vista),
 * no la distancia a un punto como hace sortVertexData de vanilla (que solo acierta con camara en perspectiva).
 * Se ordena por el centroide de cada cara; vale para caras de bloque (1x1 como maximo).
 */
final class OrdenTranslucido {
    private OrdenTranslucido() { }

    /**
     * Profundidad de vision de un punto: mayor = mas cerca de la camara. Misma cadena que CamaraGL
     * (rotX(elevacion) * rotY(rotacion)); la traslacion al centro es constante y no cambia el orden.
     */
    static double cercania(double x, double y, double z, double rotacion, double elevacion) {
        double r = Math.toRadians(rotacion), e = Math.toRadians(elevacion);
        return y * Math.sin(e) + (-x * Math.sin(r) + z * Math.cos(r)) * Math.cos(e);
    }

    /** Devuelve una copia con las caras reordenadas de la mas lejana a la mas cercana (estable en empates). */
    static int[] ordenar(int[] datos, int intsVertice, double rotacion, double elevacion) {
        final int porCara = 4 * intsVertice, n = datos.length / porCara;
        final double[] cerca = new double[n];
        Integer[] idx = new Integer[n];
        for (int q = 0; q < n; q++) {
            double x = 0, y = 0, z = 0;
            for (int v = 0; v < 4; v++) {
                int o = q * porCara + v * intsVertice;
                x += Float.intBitsToFloat(datos[o]); y += Float.intBitsToFloat(datos[o + 1]); z += Float.intBitsToFloat(datos[o + 2]);
            }
            cerca[q] = cercania(x / 4, y / 4, z / 4, rotacion, elevacion);
            idx[q] = q;
        }
        Arrays.sort(idx, Comparator.comparingDouble(q -> cerca[q])); // TimSort: estable
        int[] res = new int[n * porCara];
        for (int i = 0; i < n; i++) System.arraycopy(datos, idx[i] * porCara, res, i * porCara, porCara);
        return res;
    }
}
