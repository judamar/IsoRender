package com.judamar.isorender.menu;

import com.judamar.isorender.seleccion.Seleccion;

/** T-023 (CA-016.3): texto de estado de la selección para el menú (lógica pura, sin clases de Minecraft). */
public final class TextoSeleccion {
    private TextoSeleccion() { }

    public static String describir(Seleccion sel) {
        if (sel.completa()) {
            int[] d = sel.dimensiones();
            return "Región: " + d[0] + " × " + d[1] + " × " + d[2] + " bloques";
        }
        if (!sel.tienePunto1() && !sel.tienePunto2()) return "Falta definir la selección (usa /isorender pos1 y pos2)";
        return "Falta el punto " + (sel.tienePunto1() ? 2 : 1) + " (usa /isorender pos" + (sel.tienePunto1() ? 2 : 1) + ")";
    }
}
