package com.judamar.isorender.render;

/** T-025 (RF-019, RNF-003): geometría y umbrales de la vista previa del menú. Lógica pura, sin clases de Minecraft. */
public final class VistaPreviaCalculo {
    private VistaPreviaCalculo() { }

    /** Zona libre: a la derecha de los controles (x < 240) y entre el título y los botones inferiores. */
    static final int X_MIN = 240, MARGEN_DER = 10, Y_MIN = 44, MARGEN_INF = 60, LADO_MIN = 64;
    /** Región grande: más de 32³ bloques. */
    static final long VOLUMEN_SIMPLIFICADO = 32L * 32 * 32;
    static final int TOPE_NORMAL = 512, TOPE_SIMPLIFICADO = 256;
    static final long REORDEN_MS = 300;

    /** Recuadro cuadrado centrado en la zona libre: {x, y, lado} en píxeles de GUI; null si el lado sería menor de 64. */
    public static int[] recuadro(int ancho, int alto) {
        int zw = ancho - MARGEN_DER - X_MIN, zh = alto - MARGEN_INF - Y_MIN;
        int lado = Math.min(zw, zh);
        if (lado < LADO_MIN) return null;
        return new int[] {X_MIN + (zw - lado) / 2, Y_MIN + (zh - lado) / 2, lado};
    }

    public static boolean simplificado(long volumen) { return volumen > VOLUMEN_SIMPLIFICADO; }

    /** Lado del FBO en píxeles reales: recuadro x escala de GUI, con tope según el modo y mínimo de 64. */
    public static int ladoFbo(int ladoRecuadro, int escala, boolean simplificado) {
        int tope = simplificado ? TOPE_SIMPLIFICADO : TOPE_NORMAL;
        return Math.max(LADO_MIN, Math.min(ladoRecuadro * escala, tope));
    }

    /** Normal: se reordenan las translúcidas siempre; simplificado: como mucho cada 300 ms. */
    public static boolean reordenar(boolean simplificado, long ahora, long ultimo) {
        return !simplificado || ahora - ultimo > REORDEN_MS;
    }
}
