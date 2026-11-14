package com.judamar.isorender.seleccion;

/** Parseo puro de la línea "/isorender [sub]". */
public enum Subcomando {
    POS1, POS2, CLEAR, MENU, PRUEBA, PRUEBA_MULTI, ESTRES, USO;

    /** Devuelve null si la línea no es de /isorender; USO si el subcomando no existe o sobran argumentos. */
    public static Subcomando parsear(String linea) {
        String m = linea.trim();
        if (!m.equals("/isorender") && !m.startsWith("/isorender ")) return null;
        String[] t = m.substring("/isorender".length()).trim().split("\\s+", 2);
        if (t.length > 1 && !t[0].equals("prueba") && !t[0].equals("prueba-multi") && !t[0].equals("estres")) return USO; // solo las pruebas admiten argumentos
        switch (t[0]) {
            case "pos1": return POS1;
            case "pos2": return POS2;
            case "clear": return CLEAR;
            case "menu": return MENU;
            case "prueba": return PRUEBA;
            case "prueba-multi": return PRUEBA_MULTI;
            case "estres": return ESTRES;
            default: return USO;
        }
    }

    /** Argumento opcional tras el subcomando ("/isorender prueba 2048" -> "2048"); "" si no hay. */
    public static String argumento(String linea) {
        String[] t = linea.trim().split("\\s+", 3);
        return t.length > 2 ? t[2] : "";
    }
}
