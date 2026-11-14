package com.judamar.isorender.configuracion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** T-029 (RF-028, RF-029): E/S del archivo de configuración. Nunca lanza: los fallos van al log y se siguen usando los valores por defecto. */
public final class ArchivoConfiguracion {
    private ArchivoConfiguracion() { }

    /** Si el archivo no existe lo crea con los valores por defecto (RF-029); si existe lo lee en UTF-8 (CA-028.1/2). */
    public static Configuracion cargar(File archivo, Consumer<String> log) {
        try {
            if (!archivo.exists()) {
                guardar(archivo, Configuracion.POR_DEFECTO, log);
                return Configuracion.POR_DEFECTO;
            }
            List<String> avisos = new ArrayList<>();
            // newDecoder() rechaza bytes que no son UTF-8 válido (IOException) en vez de sustituirlos en silencio.
            try (Reader r = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8.newDecoder())) {
                Configuracion c = Configuracion.leer(r, avisos);
                avisos.forEach(log);
                return c;
            }
        } catch (Throwable e) {
            log.accept("Could not read " + archivo + " (" + e + "); using default values.");
            return Configuracion.POR_DEFECTO;
        }
    }

    /** Crea el directorio si falta y escribe vía archivo temporal + renombrado; si el renombrado falla, escribe directo (CA-029.2). */
    public static void guardar(File archivo, Configuracion c, Consumer<String> log) {
        try {
            File dir = archivo.getAbsoluteFile().getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory())
                throw new java.io.IOException("could not create directory " + dir);
            File tmp = new File(dir, archivo.getName() + ".tmp");
            escribir(tmp, c);
            try {
                Files.move(tmp.toPath(), archivo.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (java.io.IOException e) {
                escribir(archivo, c);
                tmp.delete();
            }
        } catch (Throwable e) {
            log.accept("Could not save the configuration to " + archivo + " (" + e + ").");
        }
    }

    private static void escribir(File f, Configuracion c) throws java.io.IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) { c.escribir(w); }
    }
}
