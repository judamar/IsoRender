package com.judamar.isorender.render;

/** Logica pura de la region (sin clases de Minecraft, para poder probarla con JUnit). */
final class GeometriaRegion {
    private GeometriaRegion() { }

    /** Capas dibujadas, en orden: TRANSLUCENT va la ultima (T-020). */
    static final String[] ORDEN_CAPAS = {"SOLID", "CUTOUT_MIPPED", "CUTOUT", "TRANSLUCENT"};
    /** Indice de la capa translucida (las anteriores son opacas o recortadas). */
    static final int CAPA_TRANSLUCIDA = 3;

    /** Indice de dibujo de la capa, o -1 si no existe. */
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

    /** T-043 (RF-015): copia de reg con Y maxima = corteY si y1 <= corteY < y2; con null o corte fuera de la region devuelve reg tal cual (CA-015.2). Nunca modifica reg. */
    static int[] recortar(int[] reg, Integer corteY) {
        if (reg == null || corteY == null || corteY < reg[1] || corteY >= reg[4]) return reg;
        int[] r = reg.clone();
        r[4] = corteY;
        return r;
    }

    /** Clave de cache: dos regiones son la misma si tienen la misma caja (null nunca coincide). */
    static boolean misma(int[] a, int[] b) { return a != null && b != null && java.util.Arrays.equals(a, b); }

    static final int MULTI_MIN = 2, MULTI_MAX = 8, MULTI_DEFECTO = 4;

    /** Numero de imagenes de test-multi: vacio = 4; fuera de 2-8 o no numerico lanza con mensaje en ingles. */
    static int parsearN(String arg) {
        String t = arg == null ? "" : arg.trim();
        if (t.isEmpty()) return MULTI_DEFECTO;
        int n;
        try { n = Integer.parseInt(t); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid count: \"" + t + "\". Use an integer between " + MULTI_MIN + " and " + MULTI_MAX + "."); }
        if (n < MULTI_MIN || n > MULTI_MAX) throw new IllegalArgumentException("Count out of range: " + n + ". Must be between " + MULTI_MIN + " and " + MULTI_MAX + ".");
        return n;
    }
}
