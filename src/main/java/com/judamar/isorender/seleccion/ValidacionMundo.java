package com.judamar.isorender.seleccion;

import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** RF-007 / RNF-014 (capa Minecraft): chunks cargados y región no vacía. Solo lectura del mundo. */
public final class ValidacionMundo {
    private ValidacionMundo() { }

    /**
     * Validación completa de la selección para /isorender render y /isorender prueba.
     * Devuelve null si es válida (y la región queda en r[0..5] de Seleccion.region()); si no, el mensaje para el chat.
     */
    public static String validar(Seleccion s, World w, int max) {
        String e = ValidacionRegion.validar(s, max);
        if (e != null) return e;
        int[] r = s.region();
        // Chunks: se recorre cada columna de chunk que cubre la región (esquinas de cada chunk de 16x16).
        for (int x = r[0] >> 4; x <= r[3] >> 4; x++)
            for (int z = r[2] >> 4; z <= r[5] >> 4; z++)
                if (!w.isBlockLoaded(new BlockPos(x << 4, r[1], z << 4)))
                    return "La región tiene zonas no cargadas; acércate para que los chunks se carguen.";
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = r[0]; x <= r[3]; x++)
            for (int y = r[1]; y <= r[4]; y++)
                for (int z = r[2]; z <= r[5]; z++)
                    if (w.getBlockState(p.setPos(x, y, z)).getMaterial() != Material.AIR) return null;
        return "La región solo contiene aire: no hay bloques que renderizar.";
    }
}
