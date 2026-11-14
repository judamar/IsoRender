package com.judamar.isorender.render;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

/**
 * T-019 (RF-011, RNF-004): convierte pixeles RGBA de glReadPixels (origen abajo-izquierda) en una imagen ARGB final,
 * reduciendo el supermuestreo factor x factor. Logica pura, sin Minecraft ni OpenGL.
 * La media se hace con alfa premultiplicado: el color del borde es el de la estructura, nunca mezclado con el
 * RGB del fondo transparente (sin halo claro ni oscuro). Pixeles con alfa 0 salen como 0x00000000.
 */
public final class ConversorImagen {
    private ConversorImagen() { }

    /**
     * Procesa una franja de {@code filas} filas GL (desde la fila {@code y0}, ambas multiplos de {@code factor}) de una
     * imagen interna de ladoInterno x ladoInterno y escribe los pixeles reducidos en {@code destino}
     * (lado x lado ARGB, fila 0 arriba, lado = ladoInterno / factor). El buffer se lee desde su posicion 0.
     */
    public static void reducirFranja(ByteBuffer px, int ladoInterno, int y0, int filas, int factor, int[] destino) {
        int lado = ladoInterno / factor, n = factor * factor;
        for (int j = 0; j < filas / factor; j++) {
            int filaDestino = (lado - 1) - (y0 / factor + j); // invierte el eje Y (GL: abajo-izquierda)
            for (int x = 0; x < lado; x++) {
                long a = 0, r = 0, g = 0, b = 0; // sumas con alfa premultiplicado
                for (int dy = 0; dy < factor; dy++) {
                    for (int dx = 0; dx < factor; dx++) {
                        int i = ((j * factor + dy) * ladoInterno + x * factor + dx) * 4;
                        int pa = px.get(i + 3) & 255;
                        a += pa;
                        r += (px.get(i) & 255) * pa;
                        g += (px.get(i + 1) & 255) * pa;
                        b += (px.get(i + 2) & 255) * pa;
                    }
                }
                destino[filaDestino * lado + x] = componer(a, r, g, b, n);
            }
        }
    }

    /** Sumas premultiplicadas de n muestras a un pixel ARGB. Alfa = media; color = sum(c*a)/sum(a) (des-premultiplicado). */
    static int componer(long a, long r, long g, long b, int n) {
        if (a == 0) return 0; // transparente total: RGB normalizado a 0
        int alfa = (int) ((a + n / 2) / n);
        return alfa << 24 | (int) ((r + a / 2) / a) << 16 | (int) ((g + a / 2) / a) << 8 | (int) ((b + a / 2) / a);
    }

    public static BufferedImage aImagen(int[] argb, int lado) {
        BufferedImage img = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, lado, lado, argb, 0, lado);
        return img;
    }
}
