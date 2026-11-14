package com.judamar.isorender.render;

import java.util.Locale;

/** T-022: acumulador y texto del resultado de "/isorender stress". Logica pura (sin Minecraft). */
final class ResumenEstres {
    static final int MIN = 1, MAX = 50, DEFECTO = 30;

    final int solicitados;
    int hechos, errores, erroresGl;
    long nanos;
    long usadoAntes, usadoTrasPrimero, usadoDespues;

    ResumenEstres(int solicitados) { this.solicitados = solicitados; }

    /** Vacio = 30; fuera de 1-50 o no numerico lanza con mensaje en inglés. */
    static int parsearN(String arg) {
        String t = arg == null ? "" : arg.trim();
        if (t.isEmpty()) return DEFECTO;
        int n;
        try { n = Integer.parseInt(t); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid count: \"" + t + "\". Use an integer between " + MIN + " and " + MAX + "."); }
        if (n < MIN || n > MAX) throw new IllegalArgumentException("Count out of range: " + n + ". Must be between " + MIN + " and " + MAX + ".");
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
            String.format(Locale.ROOT, "Stress test: %d of %d renders at 512 px completed, %d with errors. Average time: %.1f ms per render.",
                    hechos, solicitados, errores, mediaMs()),
            String.format(Locale.ROOT, "JVM memory used: before %d MB, after the 1st render %d MB, at the end %d MB (%+.1f%% vs. the 1st; after garbage collection).",
                    mb(usadoAntes), mb(usadoTrasPrimero), mb(usadoDespues), variacionPct()),
            "OpenGL errors during the stress test: " + erroresGl + "."
        };
    }
}
