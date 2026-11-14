package com.judamar.isorender;

import com.mumfrey.liteloader.LiteMod;
import java.io.File;

/** Clase de entrada vacia (T-004/T-006). El mod es de solo lectura. */
public class LiteModIsoRender implements LiteMod {
    @Override public String getName() { return "IsoRender"; }
    @Override public String getVersion() { return "0.1.0"; }
    @Override public void init(File configPath) { }
    @Override public void upgradeSettings(String version, File configPath, File oldConfigPath) { }
}
