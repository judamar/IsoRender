package com.judamar.isorender.render;

import net.minecraft.client.Minecraft;

/**
 * T-022 (CP-24, CP-27): "/isorender stress [n]". n renders consecutivos de la region a 512 px, UNO POR FRAME (el
 * llamador invoca paso() desde onRender), para no bloquear el juego. Cada render construye y libera su geometria y su FBO,
 * asi que la memoria debe quedar estable. Retiene la guardia de RenderPrueba hasta terminar o fallar. Solo hilo de render.
 */
public final class Estres {
    private static final int RES = 512;

    private final int[] reg;
    private final ResumenEstres resumen;
    private final int erroresGlInicio;
    private int i;
    private boolean cerrado;

    private Estres(int[] reg, int n) {
        this.reg = reg.clone();
        this.resumen = new ResumenEstres(n);
        this.erroresGlInicio = RenderPrueba.erroresGl();
    }

    /** Comprueba el argumento sin iniciar nada; lanza IllegalArgumentException en inglés. */
    public static void validarArgumento(String argN) { ResumenEstres.parsearN(argN); }

    /** Valida n (1-50, vacio = 30) y toma la guardia; devuelve null (con aviso en el chat) si no se puede iniciar. */
    public static Estres iniciar(Minecraft mc, int[] reg, String argN) {
        int n = ResumenEstres.parsearN(argN); // lanza IllegalArgumentException en inglés
        if (!RenderPrueba.guardia().entrar()) {
            decir(mc, Chat.OCUPADO);
            return null;
        }
        Estres e = new Estres(reg, n);
        System.gc(); // referencia estable para comparar "antes" y "despues"
        e.resumen.usadoAntes = EstimacionMemoria.usado();
        decir(mc, "Stress test: " + n + " renders at " + RES + " px, one per frame. The game may lag a little; do not start another render until it finishes.");
        return e;
    }

    private static void decir(Minecraft mc, String s) {
        Chat.decir(mc, s);
    }

    /** Ejecuta un render. Devuelve true cuando terminó (bien, con error o abortado) y la guardia ya se liberó. */
    public boolean paso(Minecraft mc) {
        if (cerrado) return true;
        if (mc.world == null || mc.player == null) { cerrar(); return true; } // salio del mundo: abortar en silencio
        long t0 = System.nanoTime();
        boolean error = false;
        try {
            double rot = 360.0 * i / resumen.solicitados; // reparte rotaciones: tambien ejercita el orden de translucidas
            RenderPrueba.renderSilencioso(mc, reg, RES, ParametrosCamara.crear(rot, ParametrosCamara.POR_DEFECTO.elevacion,
                    ParametrosCamara.POR_DEFECTO.zoom, 0, 0));
        } catch (Throwable e) {
            error = true;
            Fallos.informar(mc, "rendering during the stress test", e);
        }
        resumen.registrar(System.nanoTime() - t0, error);
        i++;
        if (i == 1 && !error) { System.gc(); resumen.usadoTrasPrimero = EstimacionMemoria.usado(); } // RNF-009: base = tras la 1.ª iteración
        if (error || i >= resumen.solicitados) {
            try {
                System.gc();
                resumen.usadoDespues = EstimacionMemoria.usado();
                resumen.erroresGl = RenderPrueba.erroresGl() - erroresGlInicio;
                if (error) decir(mc, "Stress test stopped by an error in render " + i + ".");
                for (String l : resumen.lineas()) decir(mc, l);
            } finally { cerrar(); }
            return true;
        }
        return false;
    }

    /** Libera la guardia. Idempotente; llamar siempre al abandonar un estres (p. ej. tras una excepcion inesperada). */
    public void cerrar() {
        if (cerrado) return;
        cerrado = true;
        RenderPrueba.guardia().salir();
    }
}
