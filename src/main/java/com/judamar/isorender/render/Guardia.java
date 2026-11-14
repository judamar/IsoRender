package com.judamar.isorender.render;

import java.util.concurrent.atomic.AtomicBoolean;

/** T-022: guardia no bloqueante para que dos renders no se ejecuten a la vez ni se solapen. Logica pura. */
public final class Guardia {
    private final AtomicBoolean ocupada = new AtomicBoolean();

    /** True si se pudo entrar; false si ya hay alguien dentro (el llamador debe rechazar y NO llamar a salir). */
    public boolean entrar() { return ocupada.compareAndSet(false, true); }

    public void salir() { ocupada.set(false); }

    public boolean ocupada() { return ocupada.get(); }
}
