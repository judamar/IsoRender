package com.judamar.isorender.render;

/** T-022: acumulador y texto del resultado de "/isorender estres". Logica pura (sin Minecraft). */
final class ResumenEstres {
    static final int MIN = 1, MAX = 50, DEFECTO = 30;

    final int solicitados;
    int hechos, errores, erroresGl;
    long nanos;
    long usadoAntes, usadoTrasPrimero, usadoDespues;

    ResumenEstres(int solicitados) { this.solicitados = solicitados; }

    /** Vacio = 30; fuera de 1-50 o no numerico lanza con mensaje en español. */
    static int parsearN(String arg) {
        String t = arg == null ? "" : arg.trim();
        if (t.isEmpty()) return DEFECTO;
        int n;
        try { n = Integer.parseInt(t); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Cantidad no válida: «" + t + "». Usa un entero entre " + MIN + " y " + MAX + "."); }
        if (n < MIN || n > MAX) throw new IllegalArgumentException("Cantidad fuera de rango: " + n + ". Debe estar entre " + MIN + " y " + MAX + ".");
        return n;
    }

    void registrar(long duracionNanos, boolean error) {
        if (error) { errores++; return; }
        hechos++; nanos += duracionNanos;
    }

    double mediaMs() { return hechos == 0 ? 0 : nanos / 1e6 / hechos; }

    private static long mb(long b) { return Math.round(b / (double) EstimacionMemoria.MB); }

    /** Variacion porcentual de la memoria final respecto a la medida tras la primera iteracion (RNF-009). */
    double variacionPct() { return usadoTrasPrimero <= 0 ? 0 : 100.0 * (usadoDespues - usadoTrasPrimero) / usadoTrasPrimero; }

    String[] lineas() {
        return new String[] {
            String.format("Estrés: %d de %d renders de 512 px completados, %d con error. Tiempo medio: %.1f ms por render.",
                    hechos, solicitados, errores, mediaMs()),
            String.format("Memoria JVM usada: antes %d MB, tras el 1.º render %d MB, al final %d MB (%+.1f %% frente al 1.º; tras recolectar basura).",
                    mb(usadoAntes), mb(usadoTrasPrimero), mb(usadoDespues), variacionPct()),
            "Errores de OpenGL durante el estrés: " + erroresGl + "."
        };
    }
}
