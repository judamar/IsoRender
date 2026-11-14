package com.judamar.isorender.configuracion;

import java.io.File;

/** T-030: configuración vigente del mod, accesible desde menú, render y selección sin pasarla por parámetro. */
public final class Ajustes {
    private static volatile Configuracion actual = Configuracion.POR_DEFECTO;

    private Ajustes() { }

    public static Configuracion actual() { return actual; }

    public static void actual(Configuracion c) { actual = c; }

    /** Carpeta de exportación: vacía = «isorender» en {@code base}; relativa = respecto a {@code base}; absoluta tal cual. */
    public static File resolverCarpeta(String carpeta, File base) {
        if (carpeta == null || carpeta.trim().isEmpty()) return new File(base, "isorender");
        File f = new File(carpeta.trim());
        return f.isAbsolute() ? f : new File(base, carpeta.trim());
    }
}
