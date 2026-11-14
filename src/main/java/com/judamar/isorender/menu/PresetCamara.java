package com.judamar.isorender.menu;

import com.judamar.isorender.render.ParametrosCamara;

/** T-026 (RF-022): presets de cámara. Cambian solo rotación y elevación; zoom, desplazamiento y vista se conservan. */
public enum PresetCamara {
    ISOMETRICA("Isometric", 45, 35.264),
    /** [SUPUESTO] «frontal» = elevación mínima permitida (RN-007), no 0°. */
    FRONTAL("Front", 0, ParametrosCamara.ELEV_MIN),
    SUPERIOR("Top", 0, 90);

    public final String etiqueta;
    public final double rotacion, elevacion;

    PresetCamara(String etiqueta, double rotacion, double elevacion) {
        this.etiqueta = etiqueta; this.rotacion = rotacion; this.elevacion = elevacion;
    }

    /** Copia de p con la rotación y elevación del preset. */
    public ParametrosCamara aplicar(ParametrosCamara p) {
        return ParametrosCamara.limitado(rotacion, elevacion, p.zoom, p.despX, p.despY, p.perspectiva);
    }
}
