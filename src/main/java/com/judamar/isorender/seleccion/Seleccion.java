package com.judamar.isorender.seleccion;

/** Estado de la selección (lógica pura, sin clases de Minecraft). Región = caja inclusive entre los dos puntos (RN-002). */
public final class Seleccion {
    private int[] p1, p2;

    public void fijarPunto1(int x, int y, int z) { p1 = new int[] {x, y, z}; }
    public void fijarPunto2(int x, int y, int z) { p2 = new int[] {x, y, z}; }

    /** Borra ambos puntos; devuelve false si no había ninguno. */
    public boolean limpiar() {
        boolean habia = p1 != null || p2 != null;
        p1 = null; p2 = null;
        return habia;
    }

    public boolean tienePunto1() { return p1 != null; }
    public boolean tienePunto2() { return p2 != null; }
    public boolean completa() { return p1 != null && p2 != null; }
    public int[] punto1() { return p1 == null ? null : p1.clone(); }
    public int[] punto2() { return p2 == null ? null : p2.clone(); }

    /** Región normalizada {minX,minY,minZ,maxX,maxY,maxZ}; null si falta un punto. */
    public int[] region() {
        if (!completa()) return null;
        int[] r = new int[6];
        for (int i = 0; i < 3; i++) { r[i] = Math.min(p1[i], p2[i]); r[i + 3] = Math.max(p1[i], p2[i]); }
        return r;
    }

    /** Dimensiones {ancho X, alto Y, largo Z} en bloques; null si falta un punto. */
    public int[] dimensiones() {
        int[] r = region();
        return r == null ? null : new int[] {r[3] - r[0] + 1, r[4] - r[1] + 1, r[5] - r[2] + 1};
    }
}
