package com.judamar.isorender.menu;

import com.judamar.isorender.configuracion.Configuracion;
import com.judamar.isorender.render.FondoVista;
import com.judamar.isorender.render.ParametrosCamara;
import com.judamar.isorender.render.ResolucionSalida;

/** T-024: cámara (y fondo de la vista previa) que se editan en el menú. Vive en el mod, no en la pantalla, para conservarse al cerrar y reabrir. */
public final class EstadoCamara {
    private ParametrosCamara actual = ParametrosCamara.POR_DEFECTO;

    private FondoVista fondo = FondoVista.TABLERO;

    private int resolucion = ResolucionSalida.POR_DEFECTO;

    /** T-030 (RF-028/RF-029): carga cámara, resolución y fondo desde la configuración leída al iniciar. */
    public void sembrar(Configuracion c) { actual = c.camara; resolucion = c.resolucion; fondo = c.fondo; }

    /** T-030 (RF-029): configuración con el estado actual; carpeta de salida y tamaño máximo se conservan de {@code base}. */
    public Configuracion aConfiguracion(Configuracion base) {
        return new Configuracion(base.carpetaSalida, resolucion, actual, fondo, base.tamanoMaximo);
    }

    public ParametrosCamara actual() { return actual; }

    public FondoVista fondo() { return fondo; }

    public int resolucion() { return resolucion; }

    /** T-026 (RF-020): lado de salida; lanza IllegalArgumentException si está fuera de 512-4096. */
    public void fijarResolucion(int lado) {
        String err = ResolucionSalida.validar(lado);
        if (err != null) throw new IllegalArgumentException(err);
        resolucion = lado;
    }

    /** T-026 (RF-022): cámara por defecto (rotación, elevación, zoom, desplazamiento y vista); no toca fondo ni resolución. */
    public void restablecer() { actual = ParametrosCamara.POR_DEFECTO; }

    public void aplicar(PresetCamara preset) { actual = preset.aplicar(actual); }

    /** Pasa al siguiente fondo de la vista previa y lo devuelve. */
    public FondoVista siguienteFondo() { return fondo = fondo.siguiente(); }

    /** Alterna entre vista isométrica y perspectiva; devuelve true si queda en perspectiva. */
    public boolean alternarPerspectiva() {
        actual = actual.conPerspectiva(!actual.perspectiva);
        return actual.perspectiva;
    }

    /** Cambia un ajuste (limitado al rango) y devuelve el valor realmente aplicado. */
    public double fijar(Parametro p, double valor) {
        actual = p.con(actual, valor);
        return p.valor(actual);
    }
}
