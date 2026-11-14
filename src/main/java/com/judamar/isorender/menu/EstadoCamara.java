package com.judamar.isorender.menu;

import com.judamar.isorender.render.FondoVista;
import com.judamar.isorender.render.ParametrosCamara;

/** T-024: cámara (y fondo de la vista previa) que se editan en el menú. Vive en el mod, no en la pantalla, para conservarse al cerrar y reabrir. */
public final class EstadoCamara {
    private ParametrosCamara actual = ParametrosCamara.POR_DEFECTO;

    private FondoVista fondo = FondoVista.TABLERO;

    public ParametrosCamara actual() { return actual; }

    public FondoVista fondo() { return fondo; }

    /** Pasa al siguiente fondo de la vista previa y lo devuelve. */
    public FondoVista siguienteFondo() { return fondo = fondo.siguiente(); }

    /** Cambia un ajuste (limitado al rango) y devuelve el valor realmente aplicado. */
    public double fijar(Parametro p, double valor) {
        actual = p.con(actual, valor);
        return p.valor(actual);
    }
}
