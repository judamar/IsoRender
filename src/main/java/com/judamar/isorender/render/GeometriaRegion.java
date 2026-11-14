package com.judamar.isorender.render;

/** Logica pura de la region (sin clases de Minecraft, para poder probarla con JUnit). */
final class GeometriaRegion {
    private GeometriaRegion() { }

    /** Capas dibujadas, en orden. TRANSLUCENT queda fuera (T-020). */
    static final String[] ORDEN_CAPAS = {"SOLID", "CUTOUT_MIPPED", "CUTOUT"};

    /** Indice de dibujo de la capa, o -1 si no se dibuja (translucida). */
    static int indiceCapa(String capa) {
        for (int i = 0; i < ORDEN_CAPAS.length; i++) if (ORDEN_CAPAS[i].equals(capa)) return i;
        return -1;
    }

    /** reg = {x1,y1,z1,x2,y2,z2} (min..max inclusive). */
    static boolean contiene(int[] reg, int x, int y, int z) {
        return x >= reg[0] && x <= reg[3] && y >= reg[1] && y <= reg[4] && z >= reg[2] && z <= reg[5];
    }

    static long volumen(int[] reg) {
        return (long) (reg[3] - reg[0] + 1) * (reg[4] - reg[1] + 1) * (reg[5] - reg[2] + 1);
    }

    /** Tamano inicial (bytes) del BufferBuilder de una capa: estimacion acotada a 64 MB. */
    static int bytesIniciales(int[] reg) {
        return (int) Math.min(volumen(reg) * 64L, 64L << 20);
    }
}
