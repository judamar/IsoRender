package com.judamar.isorender.seleccion;

/** Parseo puro de la línea "/isorender [sub]". */
public enum Subcomando {
    POS1, POS2, CLEAR, MENU, PRUEBA, USO;

    /** Devuelve null si la línea no es de /isorender; USO si el subcomando no existe o sobran argumentos. */
    public static Subcomando parsear(String linea) {
        String m = linea.trim();
        if (!m.equals("/isorender") && !m.startsWith("/isorender ")) return null;
        switch (m.substring("/isorender".length()).trim()) {
            case "pos1": return POS1;
            case "pos2": return POS2;
            case "clear": return CLEAR;
            case "menu": return MENU;
            case "prueba": return PRUEBA;
            default: return USO;
        }
    }
}
