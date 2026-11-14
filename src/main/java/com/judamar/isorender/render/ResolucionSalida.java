package com.judamar.isorender.render;

/** RF-025 / RN-006: lado de la imagen cuadrada de salida, entre 512 y 4096 px. Lógica pura. */
public final class ResolucionSalida {
    public static final int MIN = 512, MAX = 4096, POR_DEFECTO = 512;

    private ResolucionSalida() { }

    /** Devuelve null si el lado es válido; si no, el mensaje de error en español. */
    public static String validar(int lado) {
        return lado >= MIN && lado <= MAX ? null
                : "Resolución fuera de rango: " + lado + " px. Debe estar entre " + MIN + " y " + MAX + " px.";
    }

    /** T-019: factor de supermuestreo (render interno a lado*factor y reduccion por promedio): 2 si cabe en la GPU, si no 1. */
    public static int factorSupermuestreo(int lado, int maxGpu) {
        return lado * 2 <= maxGpu ? 2 : 1;
    }

    /** Texto vacío o null = por defecto. Lanza IllegalArgumentException con mensaje en español si no es válido. */
    public static int parsear(String texto) {
        if (texto == null || texto.trim().isEmpty()) return POR_DEFECTO;
        int lado;
        try {
            lado = Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Resolución no válida: «" + texto.trim() + "». Usa un número entero entre "
                    + MIN + " y " + MAX + " px.");
        }
        String err = validar(lado);
        if (err != null) throw new IllegalArgumentException(err);
        return lado;
    }
}
