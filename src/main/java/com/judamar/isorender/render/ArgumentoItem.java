package com.judamar.isorender.render;

/** T-042 (RF-030): argumento de "/isorender item [id] [meta]". Logica pura. id == null significa "el item de la mano". */
public final class ArgumentoItem {
    public final String id;
    public final int meta;

    private ArgumentoItem(String id, int meta) { this.id = id; this.meta = meta; }

    /** Vacio = mano; "id" o "id meta" (meta entero 0..32767). Lanza IllegalArgumentException con mensaje en español. */
    public static ArgumentoItem parsear(String arg) {
        String t = arg == null ? "" : arg.trim();
        if (t.isEmpty()) return new ArgumentoItem(null, 0);
        String[] a = t.split("\\s+");
        if (a.length > 2) throw new IllegalArgumentException("Demasiados argumentos. Uso: /isorender item [id] [meta]");
        int meta = 0;
        if (a.length == 2) {
            try {
                meta = Integer.parseInt(a[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Metadata no válida: «" + a[1] + "». Usa un entero entre 0 y 32767.");
            }
            if (meta < 0 || meta > 32767) throw new IllegalArgumentException("Metadata fuera de rango: " + meta + ". Debe estar entre 0 y 32767.");
        }
        return new ArgumentoItem(a[0], meta);
    }
}
