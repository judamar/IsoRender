package com.judamar.isorender.render;

/**
 * T-022 (RNF-014, RNF-009): estimaciones de memoria ANTES de renderizar, para rechazar con mensaje o bajar el
 * supermuestreo en vez de lanzar OutOfMemoryError. Las cifras son cotas conservadoras, no mediciones. Logica pura.
 */
final class EstimacionMemoria {
    private EstimacionMemoria() { }

    static final long MB = 1L << 20;
    /** Se exige un 25 % de holgura sobre la estimacion. */
    static final double MARGEN = 1.25;

    /** Heap + memoria directa para construir la geometria: BufferBuilders de 4 capas (ver GeometriaCacheada) x2 por crecimiento y copias. */
    static long geometria(long volumen) {
        long ini = Math.min(volumen * 64L, 64L * MB);
        return (3 * ini + Math.max(1L << 16, ini / 8)) * 2;
    }

    /** Heap + memoria directa del dibujo: BufferedImage + int[] ARGB + copia transitoria del PNG (12 B/px) y el buffer de franja de lectura. */
    static long dibujo(int res, int factor) {
        long lado = (long) res * factor;
        return 12L * res * res + lado * (128L * factor) * 4;
    }

    /** Memoria de video del FBO interno: color RGBA8 + profundidad de 32 bits. */
    static long vram(int res, int factor) {
        long lado = (long) res * factor;
        return lado * lado * 8;
    }

    /** Heap libre real: maximo - usado (el usado incluye basura aun no recogida). */
    static long libre(long max, long total, long libreDeTotal) { return max - (total - libreDeTotal); }

    static long libre() {
        Runtime r = Runtime.getRuntime();
        return libre(r.maxMemory(), r.totalMemory(), r.freeMemory());
    }

    static long usado() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    /**
     * Mayor factor de supermuestreo (de {@code deseado} hasta 1) que cabe en memoria; 0 si ni con 1.
     * {@code vramLibre} < 0 significa "desconocida" (no se limita por VRAM).
     */
    static int factor(int res, int deseado, long libreHeap, long vramLibre) {
        for (int f = deseado; f >= 1; f--) {
            boolean heap = dibujo(res, f) * MARGEN <= libreHeap;
            boolean video = vramLibre < 0 || vram(res, f) * MARGEN <= vramLibre;
            if (heap && video) return f;
        }
        return 0;
    }

    /** Null si la geometria cabe; si no, el mensaje en inglés. */
    static String comprobarGeometria(long volumen, long libreHeap) {
        long need = geometria(volumen);
        return need * MARGEN <= libreHeap ? null : mensaje(need, libreHeap);
    }

    static String mensaje(long necesario, long libre) {
        return "Not enough memory: about " + (long) Math.ceil(necesario * MARGEN / MB) + " MB are needed and only "
                + Math.max(0, libre / MB) + " MB are free. Use a smaller region or a lower resolution.";
    }
}
