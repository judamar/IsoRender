package com.judamar.isorender.seleccion;

/** RF-007 (parte pura, sin Minecraft): faltan puntos y tamaño máximo. Mensajes en inglés. */
public final class ValidacionRegion {
    /** [SUPUESTO P-09] región máxima 64 por eje. */
    public static final int MAX = 64;

    private ValidacionRegion() { }

    /** Devuelve null si la selección es válida; si no, el mensaje de error para el chat. */
    public static String validar(Seleccion s, int max) {
        if (!s.tienePunto1() && !s.tienePunto2()) return "No selection: set the points with /isorender pos1 and /isorender pos2.";
        if (!s.tienePunto1()) return "Point 1 is missing: set it with /isorender pos1.";
        if (!s.tienePunto2()) return "Point 2 is missing: set it with /isorender pos2.";
        int[] d = s.dimensiones();
        if (d[0] > max || d[1] > max || d[2] > max)
            return "The " + d[0] + " × " + d[1] + " × " + d[2] + " region exceeds the maximum allowed size of "
                    + max + " × " + max + " × " + max + " blocks.";
        return null;
    }
}
