package com.judamar.isorender.seleccion;

/** RF-007 (parte pura, sin Minecraft): faltan puntos y tamaño máximo. Mensajes en español. */
public final class ValidacionRegion {
    /** [SUPUESTO P-09] región máxima 64 por eje. */
    public static final int MAX = 64;

    private ValidacionRegion() { }

    /** Devuelve null si la selección es válida; si no, el mensaje de error para el chat. */
    public static String validar(Seleccion s, int max) {
        if (!s.tienePunto1() && !s.tienePunto2()) return "No hay selección: fija los puntos con /isorender pos1 y /isorender pos2.";
        if (!s.tienePunto1()) return "Falta el punto 1: fíjalo con /isorender pos1.";
        if (!s.tienePunto2()) return "Falta el punto 2: fíjalo con /isorender pos2.";
        int[] d = s.dimensiones();
        if (d[0] > max || d[1] > max || d[2] > max)
            return "La región de " + d[0] + " × " + d[1] + " × " + d[2] + " supera el máximo permitido de "
                    + max + " × " + max + " × " + max + " bloques.";
        return null;
    }
}
