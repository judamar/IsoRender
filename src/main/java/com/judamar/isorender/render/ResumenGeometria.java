package com.judamar.isorender.render;

/** Contadores de una construcción de geometría (lógica pura, sin Minecraft). */
final class ResumenGeometria {
    /** Bloques que generaron geometría (opacos y translúcidos). */
    final int bloques;
    /** Bloques cuyo dibujo lanzó excepción y se omitieron (RN-010). */
    final int fallidos;

    ResumenGeometria(int bloques, int fallidos) { this.bloques = bloques; this.fallidos = fallidos; }

    boolean vacio() { return bloques == 0; }

    /** Motivo en español de que no haya nada que dibujar. */
    String motivoVacio() { return "la región solo contiene aire"; }
}
