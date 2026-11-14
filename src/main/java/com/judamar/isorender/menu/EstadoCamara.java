package com.judamar.isorender.menu;

import com.judamar.isorender.render.ParametrosCamara;

/** T-024: cámara que se edita en el menú. Vive en el mod, no en la pantalla, para conservarse al cerrar y reabrir. */
public final class EstadoCamara {
    private ParametrosCamara actual = ParametrosCamara.POR_DEFECTO;

    public ParametrosCamara actual() { return actual; }

    /** Cambia un ajuste (limitado al rango) y devuelve el valor realmente aplicado. */
    public double fijar(Parametro p, double valor) {
        actual = p.con(actual, valor);
        return p.valor(actual);
    }
}
