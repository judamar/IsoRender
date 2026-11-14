package com.judamar.isorender.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.opengl.GL11;

/**
 * T-021 (RF-013): bloques con entidad de bloque (cofres, camas, carteles, cabezas...). En 1.12 no tienen modelo
 * estatico (o solo parcial): vanilla los dibuja con TileEntitySpecialRenderer. Aqui se recogen SOLO LEYENDO al construir
 * y se dibujan en cada llamada (sin display list: los renderers usan estado GL y animacion), tras los solidos.
 */
final class EntidadesBloque {
    private EntidadesBloque() { }

    /**
     * Entidades de bloque con renderer especial dentro de la region. Solo lectura: recorre el mapa de entidades de los
     * chunks YA cargados (getLoadedChunk no carga ni crea nada; World.getTileEntity podria crear la entidad que falte).
     * Excluidos: renderers globales (baliza, estructura...), spawner (su renderer crea una entidad cacheada) y piston en movimiento.
     */
    static List<TileEntity> recoger(World w, int[] reg) {
        List<TileEntity> res = new ArrayList<>();
        TileEntityRendererDispatcher d = TileEntityRendererDispatcher.instance;
        for (int cx = reg[0] >> 4; cx <= reg[3] >> 4; cx++) {
            for (int cz = reg[2] >> 4; cz <= reg[5] >> 4; cz++) {
                Chunk c = w.getChunkProvider().getLoadedChunk(cx, cz);
                if (c == null) continue;
                for (TileEntity te : c.getTileEntityMap().values()) {
                    BlockPos p = te.getPos();
                    if (te.isInvalid() || !GeometriaRegion.contiene(reg, p.getX(), p.getY(), p.getZ())) continue;
                    TileEntitySpecialRenderer<TileEntity> r = d.<TileEntity>getRenderer(te);
                    if (r == null || r.isGlobalRenderer(te) || te instanceof TileEntityMobSpawner || te instanceof TileEntityPiston) continue;
                    res.add(te);
                }
            }
        }
        return res;
    }

    /**
     * Dibuja las entidades con la camara ortografica YA activa (modelview = camara). Cada una va aislada: una excepcion
     * omite esa entidad y se cuenta (CA-013.2); el estado GL se restaura tras cada una. Restaura los campos del dispatcher.
     */
    static ResumenEntidades dibujar(Minecraft mc, List<TileEntity> lista, int[] reg) {
        if (lista.isEmpty()) return ResumenEntidades.NINGUNA;
        TileEntityRendererDispatcher d = TileEntityRendererDispatcher.instance;
        // Campos publicos del dispatcher que prepare() modifica; se restauran al terminar (el FontRenderer privado ya es mc.fontRenderer).
        World wPrev = d.world; TextureManager tmPrev = d.renderEngine; Entity ePrev = d.entity;
        float yawPrev = d.entityYaw, pitchPrev = d.entityPitch; RayTraceResult hitPrev = d.cameraHitResult;
        double exPrev = d.entityX, eyPrev = d.entityY, ezPrev = d.entityZ;
        int ok = 0, fallidas = 0;
        try {
            // cameraHitResult = null: asi ningun renderer dibuja el rotulo con el nombre de la entidad. partialTicks = 0.
            d.prepare(mc.world, mc.getTextureManager(), mc.fontRenderer, mc.player, null, 0F);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            int prof = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
            for (TileEntity te : lista) {
                try {
                    if (te.isInvalid()) continue;
                    BlockPos p = te.getPos();
                    // Luz de objeto estandar con la camara activa (como vanilla en el mundo); lightmap apagado: luz plena y fija.
                    RenderHelper.enableStandardItemLighting();
                    GlStateManager.enableCull();
                    GlStateManager.pushMatrix();
                    // Los renderers reciben x,y,z y los suman a la matriz actual: trasladamos al origen de la region y pasamos
                    // coordenadas relativas a el (numeros pequenos; con coordenadas de mundo grandes el float perderia precision).
                    GlStateManager.translate(reg[0], reg[1], reg[2]);
                    // Version de 7 argumentos: no comprueba distancia al jugador ni toca la luz; destroyStage -1 (sin grietas); alfa 1.
                    d.render(te, p.getX() - reg[0], p.getY() - reg[1], p.getZ() - reg[2], 0F, -1, 1F);
                    ok++;
                } catch (Throwable e) {
                    fallidas++; // entidad de mod u otro fallo: se omite
                    try { Tessellator.getInstance().getBuffer().finishDrawing(); } catch (IllegalStateException ignorado) { } // si quedo a medias
                } finally {
                    restaurarEstado(mc, prof);
                }
            }
        } catch (Throwable e) {
            fallidas += lista.size() - ok - fallidas; // fallo general (p. ej. prepare): el resto se cuenta como omitido
            e.printStackTrace();
        } finally {
            d.world = wPrev; d.renderEngine = tmPrev; d.entity = ePrev; d.entityYaw = yawPrev; d.entityPitch = pitchPrev;
            d.cameraHitResult = hitPrev; d.entityX = exPrev; d.entityY = eyPrev; d.entityZ = ezPrev;
            try { mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE); } catch (Throwable e) { e.printStackTrace(); }
        }
        return new ResumenEntidades(ok, fallidas);
    }

    /** Deja el estado GL como lo espera la pasada de solidos (y la translucida que sigue), pase lo que pase en el renderer. */
    static void restaurarEstado(Minecraft mc, int profundidadMatrices) {
        try {
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            // Un renderer que fallo a medias puede haber dejado matrices sin sacar: se iguala la pila a la de antes.
            while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > profundidadMatrices) GlStateManager.popMatrix();
            RenderHelper.disableStandardItemLighting(); // apaga luces y lighting
            GlStateManager.shadeModel(GL11.GL_SMOOTH); // la luz de objeto la deja en FLAT
            GlStateManager.disableRescaleNormal();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.disableCull();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            // Mezcla de la pasada de opacas (FBO premultiplicado).
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ZERO,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.enableTexture2D();
            mc.entityRenderer.disableLightmap(); // unidad del lightmap apagada y unidad activa = la de textura
        } catch (Throwable e) { e.printStackTrace(); }
    }
}
