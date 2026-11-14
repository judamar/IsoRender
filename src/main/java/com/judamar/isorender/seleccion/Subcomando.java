package com.judamar.isorender.seleccion;

/** Parseo puro de la línea "/isorender [sub]". */
public enum Subcomando {
    POS1, POS2, CLEAR, MENU, RENDER, PRUEBA, PRUEBA_MULTI, ESTRES, ITEM, USO;

    /** Devuelve null si la línea no es de /isorender; USO si el subcomando no existe o sobran argumentos. */
    public static Subcomando parsear(String linea) {
        String m = linea.trim();
        if (!m.equals("/isorender") && !m.startsWith("/isorender ")) return null;
        String[] t = m.substring("/isorender".length()).trim().split("\\s+", 2);
        if (t.length > 1 && !t[0].equals("test") && !t[0].equals("test-multi") && !t[0].equals("stress") && !t[0].equals("item")) return USO; // solo las pruebas y item admiten argumentos
        switch (t[0]) {
            case "pos1": return POS1;
            case "pos2": return POS2;
            case "clear": return CLEAR;
            case "menu": return MENU;
            case "render": return RENDER;
            case "test": return PRUEBA;
            case "test-multi": return PRUEBA_MULTI;
            case "stress": return ESTRES;
            case "item": return ITEM;
            default: return USO;
        }
    }

    /** Argumento opcional tras el subcomando ("/isorender test 2048" -> "2048"); "" si no hay. */
    public static String argumento(String linea) {
        String[] t = linea.trim().split("\\s+", 3);
        return t.length > 2 ? t[2] : "";
    }
}
