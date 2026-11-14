package com.judamar.isorender.render;

/**
 * RF-010, RF-017, RF-018, RN-007: parámetros inmutables de la cámara ortográfica y encuadre automático.
 * Lógica pura (sin Minecraft). Rotación en grados, elevación en grados, zoom en %, desplazamiento en
 * fracción del semi-ancho (-1 a 1 = ±100 %) sobre los ejes de pantalla (derecha, arriba).
 */
public final class ParametrosCamara {
    public static final double ROT_MIN = 0, ROT_MAX = 359, ELEV_MIN = 5, ELEV_MAX = 90,
            ZOOM_MIN = 25, ZOOM_MAX = 400, DESP_MAX = 1;
    /** Margen del encuadre automático (CA-010.1): 5 % del semi-ancho a cada lado. */
    public static final double MARGEN = 0.05;

    /** Campo de visión vertical de la vista en perspectiva, en grados. [SUPUESTO] pendiente de confirmar. */
    public static final double FOV_GRADOS = 30;

    public static final ParametrosCamara POR_DEFECTO = new ParametrosCamara(45, 35.264, 100, 0, 0, false);

    public final double rotacion, elevacion, zoom, despX, despY;
    /** false = isométrica (ortográfica); true = perspectiva. */
    public final boolean perspectiva;

    private ParametrosCamara(double rot, double elev, double zoom, double dx, double dy, boolean persp) {
        rotacion = rot; elevacion = elev; this.zoom = zoom; despX = dx; despY = dy; perspectiva = persp;
    }

    /** Copia con la vista cambiada (isométrica/perspectiva). */
    public ParametrosCamara conPerspectiva(boolean persp) {
        return new ParametrosCamara(rotacion, elevacion, zoom, despX, despY, persp);
    }

    /** Rechaza valores fuera de rango (comando); el mensaje en español incluye el rango. */
    public static ParametrosCamara crear(double rot, double elev, double zoom, double dx, double dy) {
        return crear(rot, elev, zoom, dx, dy, false);
    }

    public static ParametrosCamara crear(double rot, double elev, double zoom, double dx, double dy, boolean persp) {
        comprobar("Rotación", rot, ROT_MIN, ROT_MAX, "°");
        comprobar("Elevación", elev, ELEV_MIN, ELEV_MAX, "°");
        comprobar("Zoom", zoom, ZOOM_MIN, ZOOM_MAX, " %");
        comprobar("Desplazamiento X", dx * 100, -DESP_MAX * 100, DESP_MAX * 100, " %");
        comprobar("Desplazamiento Y", dy * 100, -DESP_MAX * 100, DESP_MAX * 100, " %");
        return new ParametrosCamara(rot, elev, zoom, dx, dy, persp);
    }

    /** Ajusta al rango más cercano (controles del menú, CA-017.2 / CA-018.2). */
    public static ParametrosCamara limitado(double rot, double elev, double zoom, double dx, double dy) {
        return limitado(rot, elev, zoom, dx, dy, false);
    }

    public static ParametrosCamara limitado(double rot, double elev, double zoom, double dx, double dy, boolean persp) {
        return new ParametrosCamara(Math.max(ROT_MIN, Math.min(ROT_MAX, rot)), Math.max(ELEV_MIN, Math.min(ELEV_MAX, elev)),
                Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom)), Math.max(-DESP_MAX, Math.min(DESP_MAX, dx)),
                Math.max(-DESP_MAX, Math.min(DESP_MAX, dy)), persp);
    }

    /** Argumentos de "/isorender prueba": [rotación] [elevación] [zoom %] [iso|persp]; vacíos = por defecto. */
    public static ParametrosCamara parsear(String[] t) {
        double[] v = {POR_DEFECTO.rotacion, POR_DEFECTO.elevacion, POR_DEFECTO.zoom};
        String[] nom = {"Rotación", "Elevación", "Zoom"};
        for (int i = 0; i < 3 && i < t.length; i++) {
            try { v[i] = Double.parseDouble(t[i].replace(',', '.')); }
            catch (NumberFormatException e) { throw new IllegalArgumentException(nom[i] + " no válida: «" + t[i] + "». Usa un número."); }
        }
        boolean persp = false;
        if (t.length > 3) {
            String s = t[3].trim().toLowerCase();
            if (s.equals("persp")) persp = true;
            else if (!s.equals("iso")) throw new IllegalArgumentException("Vista no válida: «" + t[3] + "». Usa iso o persp.");
        }
        return crear(v[0], v[1], v[2], 0, 0, persp);
    }

    /** Igualdad por valor (T-025: la vista previa repinta solo cuando la cámara cambia). */
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParametrosCamara)) return false;
        ParametrosCamara c = (ParametrosCamara) o;
        return Double.compare(rotacion, c.rotacion) == 0 && Double.compare(elevacion, c.elevacion) == 0
                && Double.compare(zoom, c.zoom) == 0 && Double.compare(despX, c.despX) == 0 && Double.compare(despY, c.despY) == 0
                && perspectiva == c.perspectiva;
    }

    @Override public int hashCode() { return java.util.Objects.hash(rotacion, elevacion, zoom, despX, despY, perspectiva); }

    private static void comprobar(String n, double v, double min, double max, String u) {
        if (!(v >= min && v <= max)) // también rechaza NaN
            throw new IllegalArgumentException(n + " fuera de rango: " + v + u + ". Debe estar entre " + min + u + " y " + max + u + ".");
    }

    /**
     * Semi-ancho ortográfico (en bloques) que contiene siempre las 8 esquinas de la caja nx*ny*nz centrada
     * en el origen, con rotación Y y luego elevación X (misma cadena que el modelview), más margen. Sin zoom.
     */
    public double semiAnchoBase(int nx, int ny, int nz) {
        double r = Math.toRadians(rotacion), e = Math.toRadians(elevacion);
        double cr = Math.cos(r), sr = Math.sin(r), ce = Math.cos(e), se = Math.sin(e), m = 0;
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? -nx / 2.0 : nx / 2.0, y = (i & 2) == 0 ? -ny / 2.0 : ny / 2.0,
                    z = (i & 4) == 0 ? -nz / 2.0 : nz / 2.0;
            double xp = x * cr + z * sr, zp = -x * sr + z * cr; // rotación sobre Y
            double yp = y * ce - zp * se;                       // elevación sobre X
            m = Math.max(m, Math.max(Math.abs(xp), Math.abs(yp)));
        }
        return m / (1 - MARGEN);
    }

    /**
     * Por esquina de la caja nx*ny*nz centrada en el origen: {xp, yp, c}. xp e yp son los ejes de pantalla (misma cadena que
     * semiAnchoBase) y c = y*sen(e) + zp*cos(e) es la cercanía a la cámara (igual que OrdenTranslucido.cercania).
     */
    private double[][] esquinasVista(int nx, int ny, int nz) {
        double r = Math.toRadians(rotacion), e = Math.toRadians(elevacion);
        double cr = Math.cos(r), sr = Math.sin(r), ce = Math.cos(e), se = Math.sin(e);
        double[][] q = new double[8][];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? -nx / 2.0 : nx / 2.0, y = (i & 2) == 0 ? -ny / 2.0 : ny / 2.0,
                    z = (i & 4) == 0 ? -nz / 2.0 : nz / 2.0;
            double xp = x * cr + z * sr, zp = -x * sr + z * cr;
            q[i] = new double[] {xp, y * ce - zp * se, y * se + zp * ce};
        }
        return q;
    }

    /**
     * Distancia de la cámara en perspectiva al centro de la región (bloques, zoom 100 %): la menor con que las 8 esquinas
     * caben con el margen, y siempre fuera de la esfera que envuelve la región (así ninguna esquina queda detrás de la cámara).
     */
    public double distanciaPerspectiva(int nx, int ny, int nz) {
        double t = Math.tan(Math.toRadians(FOV_GRADOS / 2)), d = 0;
        for (double[] q : esquinasVista(nx, ny, nz))
            d = Math.max(d, q[2] + Math.max(Math.abs(q[0]), Math.abs(q[1])) / (t * (1 - MARGEN)));
        return Math.max(d, 0.5 * Math.sqrt((double) nx * nx + (double) ny * ny + (double) nz * nz) + 1);
    }

    /** {near, far} del frustum: envuelven la región con holgura (near > 0 porque la cámara queda fuera de la esfera). */
    public double[] planosPerspectiva(int nx, int ny, int nz) {
        double d = distanciaPerspectiva(nx, ny, nz), cMax = -Double.MAX_VALUE, cMin = Double.MAX_VALUE;
        for (double[] q : esquinasVista(nx, ny, nz)) { cMax = Math.max(cMax, q[2]); cMin = Math.min(cMin, q[2]); }
        return new double[] {0.9 * (d - cMax), 1.1 * (d - cMin) + 1};
    }

    /** Semi-ancho final: el zoom lo divide (zoom 200 % = mitad de semi-ancho). */
    public double semiAncho(int nx, int ny, int nz) { return semiAnchoBase(nx, ny, nz) * 100 / zoom; }
}
