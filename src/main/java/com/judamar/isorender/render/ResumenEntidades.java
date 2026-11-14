package com.judamar.isorender.render;

/** T-021 (RF-013): resultado de una pasada de entidades de bloque (lógica pura, sin Minecraft). */
final class ResumenEntidades {
    static final ResumenEntidades NINGUNA = new ResumenEntidades(0, 0);

    final int dibujadas;
    /** Entidades cuyo renderer lanzó excepción: se omitieron (CA-013.2). */
    final int fallidas;

    ResumenEntidades(int dibujadas, int fallidas) { this.dibujadas = dibujadas; this.fallidas = fallidas; }

    /** Texto para el chat con las dibujadas, o null si no había ninguna. */
    String avisoDibujadas() {
        if (dibujadas == 0) return null;
        return dibujadas == 1 ? "1 entidad de bloque dibujada" : dibujadas + " entidades de bloque dibujadas";
    }

    /** Aviso del chat para las omitidas (CA-013.2), o null si no falló ninguna. */
    String avisoFallidas() {
        if (fallidas == 0) return null;
        return fallidas == 1 ? "1 entidad de bloque no se pudo dibujar y se omitió"
                : fallidas + " entidades de bloque no se pudieron dibujar y se omitieron";
    }
}
