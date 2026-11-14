package com.judamar.isorender.render;

/** T-021 (RF-013) y T-044 (RF-034): resultado de una pasada de entidades de bloque o decorativas (lógica pura, sin Minecraft). */
final class ResumenEntidades {
    static final ResumenEntidades NINGUNA = new ResumenEntidades(0, 0);

    final int dibujadas;
    /** Entidades cuyo renderer lanzó excepción: se omitieron (CA-013.2). */
    final int fallidas;

    /** Nombre del tipo de entidad en singular y plural, para los avisos. */
    private final String singular, plural;

    /** Entidades de bloque (T-021). */
    ResumenEntidades(int dibujadas, int fallidas) { this(dibujadas, fallidas, "entidad de bloque", "entidades de bloque"); }

    /** Textos propios, p. ej. las entidades decorativas (T-044). */
    ResumenEntidades(int dibujadas, int fallidas, String singular, String plural) {
        this.dibujadas = dibujadas; this.fallidas = fallidas; this.singular = singular; this.plural = plural;
    }

    /** Marcos, pinturas y armor stands (RF-034). */
    static ResumenEntidades decorativas(int dibujadas, int fallidas) {
        return new ResumenEntidades(dibujadas, fallidas, "entidad decorativa", "entidades decorativas");
    }

    /** Texto para el chat con las dibujadas, o null si no había ninguna. */
    String avisoDibujadas() {
        if (dibujadas == 0) return null;
        return dibujadas == 1 ? "1 " + singular + " dibujada" : dibujadas + " " + plural + " dibujadas";
    }

    /** Aviso del chat para las omitidas (CA-013.2), o null si no falló ninguna. */
    String avisoFallidas() {
        if (fallidas == 0) return null;
        return fallidas == 1 ? "1 " + singular + " no se pudo dibujar y se omitió"
                : fallidas + " " + plural + " no se pudieron dibujar y se omitieron";
    }
}
