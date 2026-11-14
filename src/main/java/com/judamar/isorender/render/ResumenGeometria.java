package com.judamar.isorender.render;

/** Contadores de una construcción de geometría (lógica pura, sin Minecraft). */
final class ResumenGeometria {
    /** Bloques que generaron geometría (opacos y translúcidos). */
    final int bloques;
    /** Bloques cuyo dibujo lanzó excepción y se omitieron (RN-010). */
    final int fallidos;
    /** Entidades de bloque candidatas dentro de la región (T-021); se dibujan aparte, con su renderer especial. */
    final int entidades;

    ResumenGeometria(int bloques, int fallidos, int entidades) {
        this.bloques = bloques; this.fallidos = fallidos; this.entidades = entidades;
    }

    /** Sin nada que dibujar: ni bloques con modelo ni entidades de bloque (una región con solo un cofre no está vacía). */
    boolean vacio() { return bloques == 0 && entidades == 0; }

    /** Motivo en inglés de que no haya nada que dibujar. */
    String motivoVacio() { return "The region contains only air."; }
}
