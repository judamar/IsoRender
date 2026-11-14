package com.judamar.isorender.render;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

/**
 * T-028 (RF-024, RF-026): nombre del PNG exportado, estructura_&lt;fecha-hora&gt;_&lt;x1,y1,z1&gt;_&lt;x2,y2,z2&gt;.png
 * (ERS 11.2), unico dentro de la carpeta de salida. Logica pura, sin Minecraft.
 */
public final class NombreArchivo {
    private NombreArchivo() { }

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** reg = {minX,minY,minZ,maxX,maxY,maxZ}. Los negativos llevan «-»; las comas y el «-» son validos en cualquier sistema de archivos. */
    public static String base(LocalDateTime fechaHora, int[] reg) {
        return "estructura_" + FECHA.format(fechaHora) + "_" + reg[0] + "," + reg[1] + "," + reg[2]
                + "_" + reg[3] + "," + reg[4] + "," + reg[5] + ".png";
    }

    /** Si {@code base} ya existe añade _2, _3... antes de «.png» hasta encontrar un nombre libre (CA-026.2). */
    public static String unico(String base, Predicate<String> existe) {
        if (!existe.test(base)) return base;
        String raiz = base.substring(0, base.length() - ".png".length());
        for (int n = 2; ; n++) {
            String candidato = raiz + "_" + n + ".png";
            if (!existe.test(candidato)) return candidato;
        }
    }
}
