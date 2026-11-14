package com.judamar.isorender.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.EntityHanging;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/**
 * T-044 (RF-034): unicas entidades que se dibujan: marcos de objetos, pinturas y armor stands. Se recogen SOLO LEYENDO al
 * construir y se dibujan con el RenderManager de vanilla en cada llamada (sin display list), como las entidades de bloque.
 * Mobs, jugadores, objetos en el suelo, correas y entidades de mods quedan fuera (RN-001).
 */
final class EntidadesDecorativas {
    private EntidadesDecorativas() { }

    /**
     * Decorativas dentro de la region. Solo lectura: getEntitiesWithinAABB consulta los chunks ya cargados; no se crea
     * ni se mueve nada. Entra la entidad cuyo bloque de anclaje (marco, pintura) o bloque base (armor stand) esta dentro.
     */
    static List<Entity> recoger(World w, int[] reg) {
        AxisAlignedBB caja = new AxisAlignedBB(reg[0], reg[1], reg[2], reg[3] + 1, reg[4] + 1, reg[5] + 1);
        // instanceof explicitos (no la jerarquia): una entidad de mod que herede de EntityHanging no entra.
        List<Entity> candidatas = w.getEntitiesWithinAABB(Entity.class, caja,
                e -> !e.isDead && (e instanceof EntityItemFrame || e instanceof EntityPainting || e instanceof EntityArmorStand));
        List<Entity> res = new ArrayList<>();
        for (Entity e : candidatas) {
            BlockPos p = e instanceof EntityHanging ? ((EntityHanging) e).getHangingPosition()
                    : new BlockPos(MathHelper.floor(e.posX), MathHelper.floor(e.posY), MathHelper.floor(e.posZ));
            if (GeometriaRegion.contiene(reg, p.getX(), p.getY(), p.getZ())) res.add(e);
        }
        return res;
    }

    /**
     * Dibuja con la camara YA activa (modelview = camara, en coordenadas de mundo, igual que EntidadesBloque). Cada entidad
     * va aislada: una excepcion la omite y se cuenta (CA-034.2). Restaura pointedEntity y las sombras del RenderManager.
     */
    static ResumenEntidades dibujar(Minecraft mc, List<Entity> lista, int[] reg) {
        if (lista.isEmpty()) return ResumenEntidades.NINGUNA;
        RenderManager rm = mc.getRenderManager();
        Entity apuntadaPrev = rm.pointedEntity;
        boolean sombraPrev = rm.isRenderShadow();
        int ok = 0, fallidas = 0;
        try {
            rm.pointedEntity = null; // sin rotulo del nombre del objeto del marco
            rm.setRenderShadow(false); // sin sombras en el suelo
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            int prof = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
            for (Entity e : lista) {
                try {
                    if (e.isDead) continue;
                    RenderHelper.enableStandardItemLighting(); // luz de objeto con la camara activa; lightmap apagado: luz plena
                    GlStateManager.enableCull();
                    GlStateManager.pushMatrix();
                    // Como EntidadesBloque: trasladar al origen de la region y pasar coordenadas relativas (precision float).
                    GlStateManager.translate(reg[0], reg[1], reg[2]);
                    // doRenderEntity (en vanilla 1.12 renderEntity no existe con esta firma): partialTicks 0, sin hitbox.
                    rm.doRenderEntity(e, e.posX - reg[0], e.posY - reg[1], e.posZ - reg[2], e.rotationYaw, 0F, false);
                    ok++;
                } catch (Throwable t) {
                    fallidas++; // entidad de mod u otro fallo: se omite
                    try { Tessellator.getInstance().getBuffer().finishDrawing(); } catch (IllegalStateException ignorado) { }
                } finally {
                    EntidadesBloque.restaurarEstado(mc, prof);
                }
            }
        } catch (Throwable t) {
            fallidas += lista.size() - ok - fallidas;
            t.printStackTrace();
        } finally {
            rm.pointedEntity = apuntadaPrev;
            rm.setRenderShadow(sombraPrev);
            try { mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE); } catch (Throwable t) { t.printStackTrace(); }
        }
        return ResumenEntidades.decorativas(ok, fallidas);
    }
}
