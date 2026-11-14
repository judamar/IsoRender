package com.judamar.isorender.render;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

/**
 * Vista de solo lectura de la region: delega en el mundo dentro de la caja y devuelve aire fuera.
 * Asi se dibujan las caras del borde exterior y funcionan el culling interior y el AO.
 * [SUPUESTO] Las conexiones de vallas/paneles hacia fuera de la region se cortan.
 */
final class VistaRegion implements IBlockAccess {
    /** Luz fija maxima (cielo 15, bloque 15): mismo PNG de dia y de noche. */
    private static final int LUZ_MAX = 15 << 20 | 15 << 4;
    private final World w;
    private final int[] reg;

    VistaRegion(World w, int[] reg) { this.w = w; this.reg = reg; }

    private boolean dentro(BlockPos p) { return GeometriaRegion.contiene(reg, p.getX(), p.getY(), p.getZ()); }

    @Nullable @Override public TileEntity getTileEntity(BlockPos p) { return dentro(p) ? w.getTileEntity(p) : null; }
    @Override public int getCombinedLight(BlockPos p, int min) { return LUZ_MAX; }
    @Override public IBlockState getBlockState(BlockPos p) {
        return dentro(p) ? w.getBlockState(p) : Blocks.AIR.getDefaultState();
    }
    @Override public boolean isAirBlock(BlockPos p) { return getBlockState(p).getMaterial() == net.minecraft.block.material.Material.AIR; }
    @Override public Biome getBiome(BlockPos p) { return w.getBiome(p); }
    @Override public int getStrongPower(BlockPos p, EnumFacing d) { return 0; }
    @Override public WorldType getWorldType() { return w.getWorldType(); }
}
