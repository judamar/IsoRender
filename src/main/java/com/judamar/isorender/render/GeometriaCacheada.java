package com.judamar.isorender.render;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.Collections;
import java.util.List;
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
 * hacia delante (OrdenTranslucido) solo cuando cambian rotacion, elevacion o modo (isometrica/perspectiva); zoom y desplazamiento no alteran el orden.
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
    // T-021: entidades de bloque (cofres, camas, carteles...) recogidas al construir; se dibujan directo, sin display list.
    private List<TileEntity> entidades = Collections.emptyList();
    private ResumenEntidades ultimasEntidades = ResumenEntidades.NINGUNA;
    // T-044: marcos, pinturas y armor stands recogidos al construir (solo lectura); se dibujan con el RenderManager, sin display list.
    private List<Entity> decorativas = Collections.emptyList();
    private ResumenEntidades ultimasDecorativas = ResumenEntidades.NINGUNA;
    private double rotT = Double.NaN, elevT = Double.NaN; // camara con la que se compilo la lista translucida
    private boolean perspT;

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
        EstadoGrafico previo = EstadoGrafico.capturarAjustes(mc); // AO y hojas del jugador; se restauran siempre
        int base = -1;
        boolean ok = false;
        try {
            previo.forzarAjustesGeometria();
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
            List<TileEntity> tes = EntidadesBloque.recoger(w, reg); // solo lectura de los chunks cargados
            List<Entity> deco = EntidadesDecorativas.recoger(w, reg); // marcos, pinturas, armor stands (solo lectura)
            // Las decorativas cuentan como "entidades": una region con solo un armor stand no esta vacia.
            ResumenGeometria res = new ResumenGeometria(dibujados, fallidos, tes.size() + deco.size());
            if (res.vacio()) throw new IllegalStateException(res.motivoVacio());

            // Subida: una display list por capa que reproduce el buffer (draw tambien lo resetea).
            base = GLAllocation.generateDisplayLists(capas.length);
            for (int k = 0; k < GeometriaRegion.CAPA_TRANSLUCIDA; k++) { // las opacas; la translucida se compila al dibujar
                GlStateManager.glNewList(base + k, GL11.GL_COMPILE);
                try { new WorldVertexBufferUploader().draw(capas[k]); } finally { GlStateManager.glEndList(); }
            }
            GeometriaCacheada g = new GeometriaCacheada(reg, res, base);
            g.entidades = tes; g.decorativas = deco;
            g.datosT = crudoT; g.bufT = crudoT == null ? null : bt;
            ok = true;
            return g;
        } finally {
            if (!ok && base >= 0) GLAllocation.deleteDisplayLists(base, GeometriaRegion.ORDEN_CAPAS.length);
            previo.restaurar();
        }
    }

    /** True si esta cache sigue valida para esa region (misma caja); si no, hay que reconstruir. No detecta cambios de bloques. */
    public boolean valida(int[] otraRegion) { return !cerrada && GeometriaRegion.misma(region, otraRegion); }

    public int bloques() { return resumen.bloques; }
    public int fallidos() { return resumen.fallidos; }
    /** Entidades de bloque y decorativas candidatas. */
    public int entidadesBloque() { return resumen.entidades; }

    /**
     * T-021: dibuja las entidades de bloque con su renderer especial. Va DESPUES de dibujarOpacas y ANTES de las translucidas,
     * con la camara activa. Cada entidad que falla se omite y se cuenta en el resultado (CA-013.2).
     */
    ResumenEntidades dibujarEntidades(Minecraft mc, int[] reg) {
        comprobarAbierta();
        ultimasEntidades = EntidadesBloque.dibujar(mc, entidades, reg);
        return ultimasEntidades;
    }

    /** Resultado de la última pasada de entidades de bloque (para avisar en el chat tras dibujar). */
    ResumenEntidades ultimasEntidades() { return ultimasEntidades; }

    /** T-044: dibuja marcos, pinturas y armor stands; mismo punto de la escena que las entidades de bloque. */
    ResumenEntidades dibujarDecorativas(Minecraft mc, int[] reg) {
        comprobarAbierta();
        ultimasDecorativas = EntidadesDecorativas.dibujar(mc, decorativas, reg);
        return ultimasDecorativas;
    }

    /** Resultado de la última pasada de entidades decorativas (para avisar en el chat). */
    ResumenEntidades ultimasDecorativas() { return ultimasDecorativas; }

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
    public void dibujarTranslucidas(double rotacion, double elevacion) { dibujarTranslucidas(rotacion, elevacion, true); }

    /** Cámara ortográfica con esa rotación y elevación (ver la sobrecarga con ParametrosCamara). */
    public void dibujarTranslucidas(double rotacion, double elevacion, boolean reordenar) {
        dibujarTranslucidas(ParametrosCamara.crear(rotacion, elevacion, 100, 0, 0), reordenar);
    }

    /**
     * T-025: con reordenar=false reutiliza la lista ya compilada (vista previa simplificada); solo compila si nunca se compiló.
     * El orden depende de rotación, elevación y modo (en perspectiva, de la distancia al ojo); zoom y desplazamiento no lo cambian.
     */
    public void dibujarTranslucidas(ParametrosCamara cam, boolean reordenar) {
        comprobarAbierta();
        if (datosT == null) return;
        boolean nunca = Double.isNaN(rotT);
        if (nunca || (reordenar && (cam.rotacion != rotT || cam.elevacion != elevT || cam.perspectiva != perspT))) {
            // Reordena las caras y vuelve a compilar la lista (GL_COMPILE sobre un nombre ya usado la reemplaza).
            int iv = DefaultVertexFormats.BLOCK.getIntegerSize();
            int[] orden;
            if (cam.perspectiva) {
                // Ojo en coordenadas de mundo (las de los vertices): centro de la región + D * dirección hacia la cámara.
                int nx = region[3] - region[0] + 1, ny = region[4] - region[1] + 1, nz = region[5] - region[2] + 1;
                double d = cam.distanciaPerspectiva(nx, ny, nz), r = Math.toRadians(cam.rotacion), e = Math.toRadians(cam.elevacion);
                orden = OrdenTranslucido.ordenar(datosT, iv,
                        region[0] + nx / 2.0 - d * Math.sin(r) * Math.cos(e),
                        region[1] + ny / 2.0 + d * Math.sin(e),
                        region[2] + nz / 2.0 + d * Math.cos(r) * Math.cos(e));
            } else orden = OrdenTranslucido.ordenar(datosT, iv, cam.rotacion, cam.elevacion);
            bufT.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            bufT.addVertexData(orden);
            bufT.finishDrawing();
            GlStateManager.glNewList(base + GeometriaRegion.CAPA_TRANSLUCIDA, GL11.GL_COMPILE);
            try { new WorldVertexBufferUploader().draw(bufT); } finally { GlStateManager.glEndList(); }
            rotT = cam.rotacion; elevT = cam.elevacion; perspT = cam.perspectiva;
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
        datosT = null; bufT = null; entidades = Collections.emptyList(); decorativas = Collections.emptyList();
        GLAllocation.deleteDisplayLists(base, GeometriaRegion.ORDEN_CAPAS.length);
    }
}
