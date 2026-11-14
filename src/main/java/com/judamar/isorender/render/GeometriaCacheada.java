package com.judamar.isorender.render;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/**
 * T-018 (RNF-002, RNF-003, RNF-009): geometria de una region construida UNA vez (lectura del mundo + BufferBuilder
 * por capa) y subida a display lists, una por capa; dibujarla con otra camara no vuelve a leer el mundo.
 *
 * Por que display lists y no VBO: es el camino que vanilla 1.12 usa para chunks sin VBO (ListedRenderChunk): se
 * compila el mismo WorldVertexBufferUploader.draw(...) que ya usamos, asi se conservan sin codigo extra los
 * punteros de vertice, color, UV y lightmap de DefaultVertexFormats.BLOCK, y el driver copia los datos a su
 * memoria; no hay buffers GL que enlazar/desenlazar ni estado de clientes que restaurar. Un VBO por capa exigiria
 * configurar a mano cada atributo y dejar GL_ARRAY_BUFFER limpio. Si T-025 mide que no basta, se cambia aqui.
 *
 * T-020: la capa TRANSLUCENT va aparte: se guardan sus caras crudas y su display list se recompila ordenada de atras
 * hacia delante (OrdenTranslucido) solo cuando cambian rotacion o elevacion; zoom y desplazamiento no alteran el orden.
 * Es lo barato aqui: ordenar N caras (N en miles) y volver a compilar una lista, sin releer el mundo.
 *
 * Solo hilo de render. Usar con try/finally y close().
 */
public final class GeometriaCacheada implements AutoCloseable {
    private final int[] region;
    private final ResumenGeometria resumen;
    private final int base; // primera display list; capa k = base + k
    private boolean cerrada;
    // Capa translucida: caras crudas (null = no hay) y el BufferBuilder reutilizado para recompilar la lista ordenada.
    private int[] datosT;
    private BufferBuilder bufT;
    private double rotT = Double.NaN, elevT = Double.NaN; // camara con la que se compilo la lista translucida

    private GeometriaCacheada(int[] region, ResumenGeometria resumen, int base) {
        this.region = region.clone(); this.resumen = resumen; this.base = base;
    }

    /**
     * Lee la region (solo lectura) y compila las listas. Lanza IllegalStateException (mensaje en espanol) si no hay
     * nada que dibujar. Fuerza AO suave y hojas con capa determinista mientras construye; los restaura siempre.
     */
    public static GeometriaCacheada construir(Minecraft mc, int[] reg) {
        World w = mc.world;
        if (w == null || mc.player == null) throw new IllegalStateException("no hay mundo cargado");
        BlockPos origen = new BlockPos(reg[0], reg[1], reg[2]);
        VistaRegion vista = new VistaRegion(w, reg); // aire fuera de la region (caras de borde visibles)
        int aoPrevio = mc.gameSettings.ambientOcclusion;
        boolean[] hojasPrevias = {hoja(Blocks.LEAVES), hoja(Blocks.LEAVES2)};
        int base = -1;
        boolean ok = false;
        try {
            mc.gameSettings.ambientOcclusion = 2;
            ((BlockLeaves) Blocks.LEAVES).setGraphicsLevel(true); ((BlockLeaves) Blocks.LEAVES2).setGraphicsLevel(true);
            int ini = GeometriaRegion.bytesIniciales(reg);
            BufferBuilder[] capas = new BufferBuilder[GeometriaRegion.ORDEN_CAPAS.length];
            for (int i = 0; i < capas.length; i++) {
                capas[i] = new BufferBuilder(i == GeometriaRegion.CAPA_TRANSLUCIDA ? Math.max(1 << 16, ini / 8) : ini);
                capas[i].begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            }
            int dibujados = 0, fallidos = 0;
            // getAllInBoxMutable reutiliza un solo BlockPos: sin objetos por bloque.
            for (BlockPos.MutableBlockPos p : BlockPos.getAllInBoxMutable(origen, new BlockPos(reg[3], reg[4], reg[5]))) {
                if (!w.isBlockLoaded(p)) continue;
                IBlockState s = w.getBlockState(p);
                if (s.getMaterial() == Material.AIR) continue;
                int k = GeometriaRegion.indiceCapa(s.getBlock().getBlockLayer().name());
                if (k < 0) continue; // capa desconocida (mod): se omite
                try {
                    // renderBlock llama a getActualState (orientacion, conexiones, mitades) con la vista.
                    if (mc.getBlockRendererDispatcher().renderBlock(s, p, vista, capas[k])) dibujados++;
                } catch (Throwable e) { fallidos++; } // bloque de mod u otro fallo: se omite (RN-010)
            }
            // Caras translucidas crudas (antes de cerrar el buffer) para reordenarlas por camara.
            BufferBuilder bt = capas[GeometriaRegion.CAPA_TRANSLUCIDA];
            int[] crudoT = bt.getVertexCount() > 0 ? bt.getVertexState().getRawBuffer() : null;
            for (BufferBuilder b : capas) b.finishDrawing();
            ResumenGeometria res = new ResumenGeometria(dibujados, fallidos);
            if (res.vacio()) throw new IllegalStateException(res.motivoVacio());

            // Subida: una display list por capa que reproduce el buffer (draw tambien lo resetea).
            base = GLAllocation.generateDisplayLists(capas.length);
            for (int k = 0; k < GeometriaRegion.CAPA_TRANSLUCIDA; k++) { // las opacas; la translucida se compila al dibujar
                GlStateManager.glNewList(base + k, GL11.GL_COMPILE);
                try { new WorldVertexBufferUploader().draw(capas[k]); } finally { GlStateManager.glEndList(); }
            }
            GeometriaCacheada g = new GeometriaCacheada(reg, res, base);
            g.datosT = crudoT; g.bufT = crudoT == null ? null : bt;
            ok = true;
            return g;
        } finally {
            if (!ok && base >= 0) GLAllocation.deleteDisplayLists(base, GeometriaRegion.ORDEN_CAPAS.length);
            mc.gameSettings.ambientOcclusion = aoPrevio;
            ((BlockLeaves) Blocks.LEAVES).setGraphicsLevel(hojasPrevias[0]); ((BlockLeaves) Blocks.LEAVES2).setGraphicsLevel(hojasPrevias[1]);
        }
    }

    /** Estado actual de la hoja (fancy o no); leavesFancy es protegido y no tiene getter. */
    private static boolean hoja(net.minecraft.block.Block b) { return !b.isOpaqueCube(b.getDefaultState()); }

    /** True si esta cache sigue valida para esa region (misma caja); si no, hay que reconstruir. No detecta cambios de bloques. */
    public boolean valida(int[] otraRegion) { return !cerrada && GeometriaRegion.misma(region, otraRegion); }

    public int bloques() { return resumen.bloques; }
    public int fallidos() { return resumen.fallidos; }

    /** Dibuja las capas opacas y recortadas (SOLID, CUTOUT_MIPPED, CUTOUT). El llamador fija camara, textura y estado GL. */
    public void dibujarOpacas() {
        comprobarAbierta();
        for (int k = 0; k < GeometriaRegion.CAPA_TRANSLUCIDA; k++) GlStateManager.callList(base + k); // sin releer el mundo
    }

    /**
     * Dibuja la capa translucida, de atras hacia delante para esa camara (rotacion y elevacion en grados; las mismas
     * de CamaraGL). Debe ir DESPUES de dibujarOpacas, con blend activo y depthMask(false) (lo fija el llamador).
     * Recompila la lista solo si la direccion de vista cambio desde la ultima vez.
     */
    public void dibujarTranslucidas(double rotacion, double elevacion) {
        comprobarAbierta();
        if (datosT == null) return;
        if (rotacion != rotT || elevacion != elevT) {
            // Reordena las caras y vuelve a compilar la lista (GL_COMPILE sobre un nombre ya usado la reemplaza).
            int[] orden = OrdenTranslucido.ordenar(datosT, DefaultVertexFormats.BLOCK.getIntegerSize(), rotacion, elevacion);
            bufT.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            bufT.addVertexData(orden);
            bufT.finishDrawing();
            GlStateManager.glNewList(base + GeometriaRegion.CAPA_TRANSLUCIDA, GL11.GL_COMPILE);
            try { new WorldVertexBufferUploader().draw(bufT); } finally { GlStateManager.glEndList(); }
            rotT = rotacion; elevT = elevacion;
        }
        GlStateManager.callList(base + GeometriaRegion.CAPA_TRANSLUCIDA);
    }

    private void comprobarAbierta() {
        if (cerrada) throw new IllegalStateException("la geometría ya fue liberada");
    }

    /** Libera las display lists. Idempotente. */
    @Override
    public void close() {
        if (cerrada) return;
        cerrada = true;
        datosT = null; bufT = null;
        GLAllocation.deleteDisplayLists(base, GeometriaRegion.ORDEN_CAPAS.length);
    }
}
