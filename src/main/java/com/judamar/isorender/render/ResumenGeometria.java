package com.judamar.isorender.render;

/** Contadores de una construcción de geometría (lógica pura, sin Minecraft). */
final class ResumenGeometria {
    /** Bloques que generaron geometría. */
    final int bloques;
    /** Bloques TRANSLUCENT omitidos (los dibuja T-020). */
    final int translucidos;
    /** Bloques cuyo dibujo lanzó excepción y se omitieron (RN-010). */
    final int fallidos;

    ResumenGeometria(int bloques, int translucidos, int fallidos) {
        this.bloques = bloques; this.translucidos = translucidos; this.fallidos = fallidos;
    }

    boolean vacio() { return bloques == 0; }

    /** Motivo en español de que no haya nada que dibujar. */
    String motivoVacio() {
        return "la región solo contiene aire" + (translucidos > 0 ? " o bloques translúcidos (aún no soportados)" : "");
    }
}
