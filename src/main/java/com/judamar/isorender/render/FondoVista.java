package com.judamar.isorender.render;

/** Fondo de la vista previa del menú (petición del solicitante). Solo afecta a la vista: el PNG exportado sigue siendo transparente. */
public enum FondoVista {
    CHECKERBOARD("Checkerboard", 0),
    WHITE("White", 0xFFFFFFFF),
    GRAY("Gray", 0xFF808080),
    BLACK("Black", 0xFF000000),
    GREEN("Chroma green", 0xFF00B140);

    public final String etiqueta;
    /** Color ARGB liso; 0 = tablero de ajedrez que revela la transparencia. */
    public final int argb;

    FondoVista(String etiqueta, int argb) { this.etiqueta = etiqueta; this.argb = argb; }

    public FondoVista siguiente() { return values()[(ordinal() + 1) % values().length]; }
}
