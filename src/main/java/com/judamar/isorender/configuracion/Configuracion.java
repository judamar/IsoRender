package com.judamar.isorender.configuracion;

import com.judamar.isorender.render.FondoVista;
import com.judamar.isorender.render.ParametrosCamara;
import com.judamar.isorender.render.ResolucionSalida;
import com.judamar.isorender.seleccion.ValidacionRegion;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * T-029 (RF-028, RF-029): valores inmutables del archivo de configuración y su lectura/escritura (formato
 * java.util.Properties). Lógica pura, sin Minecraft. Un valor inválido se sustituye por el de fábrica y se avisa.
 */
public final class Configuracion {
    public static final Configuracion POR_DEFECTO = new Configuracion("", ResolucionSalida.POR_DEFECTO,
            ParametrosCamara.POR_DEFECTO, FondoVista.CHECKERBOARD, ValidacionRegion.MAX);

    /** "" = carpeta por defecto (isorender dentro del directorio del juego). */
    public final String carpetaSalida;
    public final int resolucion;
    public final ParametrosCamara camara;
    public final FondoVista fondo;
    /** 1..ValidacionRegion.MAX: más de 64 queda fuera del alcance v1. */
    public final int tamanoMaximo;

    public Configuracion(String carpetaSalida, int resolucion, ParametrosCamara camara, FondoVista fondo, int tamanoMaximo) {
        this.carpetaSalida = carpetaSalida; this.resolucion = resolucion; this.camara = camara;
        this.fondo = fondo; this.tamanoMaximo = tamanoMaximo;
    }

    /** Lee la configuración; lo inválido toma el valor por defecto de su clave y añade un aviso a {@code avisos}. Nunca lanza. */
    public static Configuracion leer(Reader r, List<String> avisos) {
        Properties p = new Properties();
        try {
            p.load(r);
        } catch (IOException | IllegalArgumentException e) { // IAE: escape \\u mal formado
            avisos.add("Configuration file unreadable or corrupt (" + e.getMessage() + "); using default values.");
            return POR_DEFECTO;
        }
        Configuracion d = POR_DEFECTO;
        ParametrosCamara dc = d.camara;
        String carpeta = p.getProperty("outputFolder", d.carpetaSalida).trim();

        int resolucion = d.resolucion;
        String s = texto(p, "resolution");
        if (s != null) {
            try {
                resolucion = Integer.parseInt(s);
                String err = ResolucionSalida.validar(resolucion);
                if (err != null) throw new IllegalArgumentException(err);
            } catch (IllegalArgumentException e) { // NumberFormatException es IAE
                resolucion = d.resolucion;
                avisos.add(aviso("resolution", s, e instanceof NumberFormatException ? "not an integer" : e.getMessage()));
            }
        }

        // Cada ajuste de cámara se valida solo (con el resto por defecto) para que uno malo no tire a los demás.
        double rot = numero(p, "rotation", dc.rotacion, avisos), elev = numero(p, "elevation", dc.elevacion, avisos),
                zoom = numero(p, "zoom", dc.zoom, avisos), dx = numero(p, "offsetX", dc.despX * 100, avisos) / 100,
                dy = numero(p, "offsetY", dc.despY * 100, avisos) / 100;
        boolean persp = dc.perspectiva;
        s = texto(p, "view");
        if (s != null) {
            if (s.equalsIgnoreCase("persp")) persp = true;
            else if (s.equalsIgnoreCase("iso")) persp = false;
            else avisos.add(aviso("view", s, "must be iso or persp"));
        }
        ParametrosCamara cam = ParametrosCamara.crear(rot, elev, zoom, dx, dy, persp);

        FondoVista fondo = d.fondo;
        s = texto(p, "background");
        if (s != null) {
            try { fondo = FondoVista.valueOf(s.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { avisos.add(aviso("background", s, "valid values: " + java.util.Arrays.toString(FondoVista.values()))); }
        }

        int max = d.tamanoMaximo;
        s = texto(p, "maxSize");
        if (s != null) {
            try {
                max = Integer.parseInt(s);
                if (max < 1 || max > ValidacionRegion.MAX) throw new IllegalArgumentException("must be between 1 and " + ValidacionRegion.MAX);
            } catch (IllegalArgumentException e) {
                max = d.tamanoMaximo;
                avisos.add(aviso("maxSize", s, e instanceof NumberFormatException ? "not an integer" : e.getMessage()));
            }
        }
        return new Configuracion(carpeta, resolucion, cam, fondo, max);
    }

    /** Escribe el archivo (decimales con punto; desplazamientos en %, como en el menú). */
    public void escribir(Writer w) throws IOException {
        Properties p = new Properties();
        p.setProperty("outputFolder", carpetaSalida);
        p.setProperty("resolution", String.valueOf(resolucion));
        p.setProperty("rotation", num(camara.rotacion));
        p.setProperty("elevation", num(camara.elevacion));
        p.setProperty("zoom", num(camara.zoom));
        p.setProperty("offsetX", num(Math.rint(camara.despX * 1e8) / 1e6));
        p.setProperty("offsetY", num(Math.rint(camara.despY * 1e8) / 1e6));
        p.setProperty("view", camara.perspectiva ? "persp" : "iso");
        p.setProperty("background", fondo.name());
        p.setProperty("maxSize", String.valueOf(tamanoMaximo));
        p.store(w, "IsoRender configuration\n"
                + "outputFolder: empty = the game's isorender folder; relative path = relative to the game directory.\n"
                + "resolution: 512-4096. rotation 0-359, elevation 5-90, zoom 25-400, offsetX/Y -100 to 100 (in %).\n"
                + "view: iso or persp. background: CHECKERBOARD, WHITE, GRAY, BLACK or GREEN. maxSize: 1-" + ValidacionRegion.MAX + " blocks per axis.\n"
                + "An invalid value is ignored and the default is used (see the log).");
    }

    private static String num(double v) { return String.valueOf(v); } // punto decimal siempre

    private static String texto(Properties p, String clave) {
        String s = p.getProperty(clave);
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    /** Lee un decimal (punto o coma). Devuelve el defecto si falta o no es un número; el rango lo valida {@link #comprobar}. */
    private static double numero(Properties p, String clave, double def, List<String> avisos) {
        String s = texto(p, clave);
        if (s == null) return def;
        try {
            double v = Double.parseDouble(s.replace(',', '.'));
            String err = comprobar(clave, v);
            if (err == null) return v;
            avisos.add(aviso(clave, s, err));
        } catch (NumberFormatException e) {
            avisos.add(aviso(clave, s, "not a number"));
        }
        return def;
    }

    /** Valida un ajuste de cámara aislado con ParametrosCamara.crear; null si es válido. */
    private static String comprobar(String clave, double v) {
        ParametrosCamara d = ParametrosCamara.POR_DEFECTO;
        double r = d.rotacion, e = d.elevacion, z = d.zoom, x = 0, y = 0;
        switch (clave) {
            case "rotation": r = v; break;
            case "elevation": e = v; break;
            case "zoom": z = v; break;
            case "offsetX": x = v / 100; break;
            default: y = v / 100;
        }
        try { ParametrosCamara.crear(r, e, z, x, y); return null; }
        catch (IllegalArgumentException ex) { return ex.getMessage(); }
    }

    private static String aviso(String clave, String valor, String causa) {
        return "Configuration: \"" + clave + "=" + valor + "\" is not valid (" + causa + "); using the default value.";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Configuracion)) return false;
        Configuracion c = (Configuracion) o;
        return carpetaSalida.equals(c.carpetaSalida) && resolucion == c.resolucion && camara.equals(c.camara)
                && fondo == c.fondo && tamanoMaximo == c.tamanoMaximo;
    }

    @Override public int hashCode() { return Objects.hash(carpetaSalida, resolucion, camara, fondo, tamanoMaximo); }
}
