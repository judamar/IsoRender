package com.judamar.isorender.menu;

import com.judamar.isorender.render.ParametrosCamara;

/** T-024: un ajuste de cámara del menú (lógica pura). Desplazamiento en %, igual que en pantalla; ParametrosCamara lo guarda como fracción. */
public enum Parametro {
    ROTACION("Rotation", "°", ParametrosCamara.ROT_MIN, ParametrosCamara.ROT_MAX),
    ELEVACION("Elevation", "°", ParametrosCamara.ELEV_MIN, ParametrosCamara.ELEV_MAX),
    ZOOM("Zoom", "%", ParametrosCamara.ZOOM_MIN, ParametrosCamara.ZOOM_MAX),
    DESP_X("Offset X", "%", -ParametrosCamara.DESP_MAX * 100, ParametrosCamara.DESP_MAX * 100),
    DESP_Y("Offset Y", "%", -ParametrosCamara.DESP_MAX * 100, ParametrosCamara.DESP_MAX * 100);

    public final String etiqueta, unidad;
    public final double min, max;

    Parametro(String etiqueta, String unidad, double min, double max) {
        this.etiqueta = etiqueta; this.unidad = unidad; this.min = min; this.max = max;
    }

    public double valor(ParametrosCamara p) {
        switch (this) {
            case ROTACION: return p.rotacion;
            case ELEVACION: return p.elevacion;
            case ZOOM: return p.zoom;
            case DESP_X: return p.despX * 100;
            default: return p.despY * 100;
        }
    }

    /** Copia de p con este ajuste cambiado; el valor se limita al rango (CA-017.2, CA-018.2). */
    public ParametrosCamara con(ParametrosCamara p, double v) {
        double r = p.rotacion, e = p.elevacion, z = p.zoom, dx = p.despX, dy = p.despY;
        switch (this) {
            case ROTACION: r = v; break;
            case ELEVACION: e = v; break;
            case ZOOM: z = v; break;
            case DESP_X: dx = v / 100; break;
            default: dy = v / 100;
        }
        return ParametrosCamara.limitado(r, e, z, dx, dy, p.perspectiva);
    }

    /** Posición 0..1 de un valor dentro del rango (para el deslizador). */
    public double fraccion(double v) { return Math.max(0, Math.min(1, (v - min) / (max - min))); }

    /** Valor de una posición 0..1 del deslizador, redondeado a entero. */
    public double deFraccion(double f) { return Math.rint(min + Math.max(0, Math.min(1, f)) * (max - min)); }

    /** Texto con punto decimal y un decimal como máximo: «35.3». */
    public static String formato(double v) {
        double r = Math.rint(v * 10) / 10;
        return r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r);
    }

    public String conUnidad(double v) { return formato(v) + unidad; }

    /** Interpreta lo escrito en el campo numérico (CA-023.2): lanza IllegalArgumentException en inglés si no es un número. */
    public double leer(String texto) {
        try {
            double v = Double.parseDouble(texto.trim().replace(',', '.'));
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(etiqueta + " is not a valid number: \"" + texto.trim() + "\".");
        }
    }
}
