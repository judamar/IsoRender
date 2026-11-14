package com.judamar.isorender.render;

import com.judamar.isorender.configuracion.Ajustes;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * T-011 (prototipo) / T-018 / T-019 / T-022: render isometrico a FBO propio y PNG con alfa. Dos fases separadas:
 * CONSTRUCCION de geometria (GeometriaCacheada, lee el mundo una vez) y DIBUJO (camara + FBO + lectura de pixeles,
 * repetible con otra camara sin releer el mundo). Solo lectura del mundo.
 * T-022: el estado GL se captura y restaura con EstadoGrafico; todo fallo se informa en el chat sin cerrar el juego.
 */
public final class RenderPrueba {
    private RenderPrueba() { }

    /** T-022: un solo render a la vez (test, test-multi y stress comparten la guardia; el estres la retiene entre frames). */
    private static final Guardia GUARDIA = new Guardia();
    /** Errores de OpenGL detectados al terminar cada dibujo (acumulado; el estres lo lee). Solo hilo de render. */
    private static int erroresGl;

    static Guardia guardia() { return GUARDIA; }
    static int erroresGl() { return erroresGl; }
    /** T-043 (RF-015): region a exportar con el corte por altura (Y maxima absoluta e inclusiva); sin corte o fuera de la region, la misma. */
    public static int[] regionConCorte(int[] reg, Integer corteY) { return GeometriaRegion.recortar(reg, corteY); }

    /** True si hay un render (o un estres) en curso. */
    public static boolean ocupado() { return GUARDIA.ocupada(); }

    /** Argumento de "/isorender test-multi [n]": entero 2-8; vacío = 4. Lanza IllegalArgumentException en inglés. */
    public static int parsearN(String arg) { return GeometriaRegion.parsearN(arg); }

    private static void chat(Minecraft mc, String s) { Chat.decir(mc, s); }

    private static void avisosGeometria(Minecraft mc, GeometriaCacheada geo) {
        if (geo.fallidos() > 0) chat(mc, geo.fallidos() + (geo.fallidos() == 1 ? " block could not be drawn and was skipped" : " blocks could not be drawn and were skipped"));
    }

    /** T-021 y T-044: cuántas entidades (de bloque o decorativas) se dibujaron y aviso (CA-013.2) de las que se omitieron. */
    private static void avisosEntidades(Minecraft mc, ResumenEntidades e) {
        if (e.avisoDibujadas() != null) chat(mc, e.avisoDibujadas());
        if (e.avisoFallidas() != null) chat(mc, "Warning: " + e.avisoFallidas());
    }

    private static void guardar(BufferedImage img, File out) throws IOException { ConversorImagen.guardarPng(img, out); }

    /**
     * T-022 (RNF-014): lee la geometria solo si la memoria alcanza (con un GC previo si no); si no, rechaza con mensaje
     * en inglés en vez de arriesgar un OutOfMemoryError. Si aun asi falta memoria, el OOM lo informa el llamador.
     */
    public static GeometriaCacheada construirControlado(Minecraft mc, int[] reg) {
        long vol = GeometriaRegion.volumen(reg);
        String m = EstimacionMemoria.comprobarGeometria(vol, EstimacionMemoria.libre());
        if (m != null) {
            System.gc(); // el "usado" incluye basura por recoger: se mide de nuevo antes de rechazar
            m = EstimacionMemoria.comprobarGeometria(vol, EstimacionMemoria.libre());
            if (m != null) throw new IllegalStateException(m);
        }
        return GeometriaCacheada.construir(mc, reg);
    }

    /** T-028/T-030: carpeta de exportacion segun la configuracion (vacia = isorender dentro del directorio del juego). */
    static File carpetaSalida(Minecraft mc) { return Ajustes.resolverCarpeta(Ajustes.actual().carpetaSalida, mc.mcDataDir); }

    /** Prueba (/isorender test): PNG en isorender-test con el nombre de siempre. Debe llamarse desde el hilo de render. Nunca lanza. */
    public static void ejecutar(Minecraft mc, int[] reg, int res, ParametrosCamara cam) {
        File dir = new File(mc.mcDataDir, "isorender-test");
        render(mc, reg, res, cam, "Test render saved", "rendering the test",
                () -> new File(dir, "test-" + res + "-" + System.currentTimeMillis() + ".png"));
    }

    /** T-028 (RF-024/RF-026): exportacion real a carpetaSalida con nombre unico estructura_<fecha>_<coords>.png. */
    public static void exportar(Minecraft mc, int[] reg, int res, ParametrosCamara cam) {
        File dir = carpetaSalida(mc);
        render(mc, reg, res, cam, "Render saved", "exporting the render", () -> new File(dir,
                NombreArchivo.unico(NombreArchivo.base(LocalDateTime.now(), reg), n -> new File(dir, n).exists())));
    }

    /**
     * T-042 (RF-030): PNG de un item (o bloque en su forma de item) tal como se ve en el inventario, a la resolucion dada.
     * Misma guardia, FBO, supermuestreo y restauracion de estado que la region. Debe llamarse desde el hilo de render. Nunca lanza.
     */
    public static void exportarItem(Minecraft mc, ItemStack stack, int res) {
        if (!GUARDIA.entrar()) { chat(mc, Chat.OCUPADO); return; }
        try {
            long t0 = System.currentTimeMillis();
            BufferedImage img = aImagen(mc, res, s -> chat(mc, s), () -> escenaItem(mc, stack));
            File dir = carpetaSalida(mc);
            String base = NombreArchivo.baseItem(LocalDateTime.now(), String.valueOf(Item.REGISTRY.getNameForObject(stack.getItem())), stack.getMetadata());
            File out = new File(dir, NombreArchivo.unico(base, n -> new File(dir, n).exists()));
            guardar(img, out);
            Chat.archivo(mc, "Item render saved (" + res + "x" + res + " px): ", out, " [" + (System.currentTimeMillis() - t0) + " ms]");
        } catch (Throwable e) {
            Fallos.informar(mc, "rendering the item", e);
        } finally {
            GUARDIA.salir();
        }
    }

    /**
     * T-042: dibuja el item como vanilla lo dibuja en un hueco del inventario (RenderItem.renderItemModelIntoGUI/setupGuiTransform),
     * centrado en el origen y con ancho 1 unidad. La GUI tiene la Y hacia abajo (proyeccion invertida + scale(1,-1,1)); se replica
     * tal cual para que la luz de objeto de la GUI caiga igual que en el inventario. Ignora la camara del menu.
     */
    private static void escenaItem(Minecraft mc, ItemStack s) {
        double r = 0.5 / (1 - ParametrosCamara.MARGEN); // mismo margen que el encuadre de la region
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(-r, r, r, -r, -10, 10); // bottom > top: Y hacia abajo, como la GUI
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();

        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        GlStateManager.disableCull();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        // FBO premultiplicado: el alfa acumulado tambien se mezcla (ONE, ONE_MINUS_SRC_ALPHA) para que el borde translucido sea correcto.
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableRescaleNormal();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        ITextureObject atlas = mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        atlas.setBlurMipmap(false, false); // pixeles nitidos, como en el inventario; se restaura abajo
        mc.entityRenderer.disableLightmap();
        try {
            IBakedModel m = mc.getRenderItem().getItemModelWithOverrides(s, null, null);
            // La luz se fija ANTES de voltear la Y (como vanilla: luces con la matriz de la GUI, despues el hueco).
            RenderHelper.enableGUIStandardItemLighting();
            if (!m.isGui3d()) GlStateManager.disableLighting(); // item plano: sin sombreado
            GlStateManager.pushMatrix();
            GlStateManager.scale(1, -1, 1);
            m.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GUI);
            mc.getRenderItem().renderItem(s, m); // modelo normal, renderer especial (cofre, cabeza, estandarte, escudo) y brillo de encantamiento
            GlStateManager.popMatrix();
        } finally {
            try { Tessellator.getInstance().getBuffer().finishDrawing(); } catch (IllegalStateException ignorado) { } // si un renderer fallo a medias
            RenderHelper.disableStandardItemLighting();
            atlas.restoreLastBlurMipmap();
        }
    }

    /** Flujo comun: guardia, construccion, dibujo, avisos, guardado y mensaje. {@code destino} decide el File (tras dibujar). */
    private static void render(Minecraft mc, int[] reg, int res, ParametrosCamara cam, String exito, String contexto, Supplier<File> destino) {
        if (!GUARDIA.entrar()) { chat(mc, Chat.OCUPADO); return; }
        try {
            long t0 = System.currentTimeMillis();
            // (a) Construccion: lee el mundo y sube la geometria; close() la libera siempre (tambien si falla).
            try (GeometriaCacheada geo = construirControlado(mc, reg)) {
                avisosGeometria(mc, geo);
                // (b) Dibujo con la camara dada.
                BufferedImage img = dibujar(mc, geo, reg, res, cam, s -> chat(mc, s));
                avisosEntidades(mc, geo.ultimasEntidades()); avisosEntidades(mc, geo.ultimasDecorativas());
                File out = destino.get();
                guardar(img, out);
                Chat.archivo(mc, exito + " (" + res + "x" + res + " px, " + geo.bloques() + " blocks): ", out,
                        " [" + (System.currentTimeMillis() - t0) + " ms]");
            }
        } catch (Throwable e) {
            Fallos.informar(mc, contexto, e);
        } finally {
            GUARDIA.salir();
        }
    }

    /**
     * T-018: construye la geometria UNA vez y genera n PNG de 512 px con rotaciones repartidas en 0..360.
     * Informa el tiempo de construccion y el tiempo medio de dibujo (FBO + dibujo + lectura de pixeles; sin PNG).
     */
    public static void ejecutarMulti(Minecraft mc, int[] reg, int n) {
        if (!GUARDIA.entrar()) { chat(mc, Chat.OCUPADO); return; }
        final int res = 512;
        try {
            long t0 = System.nanoTime();
            try (GeometriaCacheada geo = construirControlado(mc, reg)) {
                long construccion = System.nanoTime() - t0;
                avisosGeometria(mc, geo);
                long totalDibujo = 0;
                File dir = new File(mc.mcDataDir, "isorender-test");
                long marca = System.currentTimeMillis();
                for (int i = 0; i < n; i++) {
                    double rot = 360.0 * i / n;
                    ParametrosCamara c = ParametrosCamara.crear(rot, ParametrosCamara.POR_DEFECTO.elevacion,
                            ParametrosCamara.POR_DEFECTO.zoom, 0, 0);
                    long t1 = System.nanoTime();
                    BufferedImage img = dibujar(mc, geo, reg, res, c, s -> chat(mc, s)); // reutiliza la geometria: no relee el mundo
                    totalDibujo += System.nanoTime() - t1;
                    guardar(img, new File(dir, "multi-" + marca + "-" + (i + 1) + "of" + n + "-rot" + Math.round(rot) + ".png"));
                }
                avisosEntidades(mc, geo.ultimasEntidades()); avisosEntidades(mc, geo.ultimasDecorativas());
                chat(mc, n + " images of " + res + "x" + res + " px (" + geo.bloques() + " blocks) saved to " + dir.getPath());
                chat(mc, String.format(Locale.ROOT, "Geometry build: %.1f ms (once). Average draw time per image: %.1f ms.",
                        construccion / 1e6, totalDibujo / 1e6 / n));
            }
        } catch (Throwable e) {
            Fallos.informar(mc, "rendering the test", e);
        } finally {
            GUARDIA.salir();
        }
    }

    /**
     * T-022: un render completo (construccion + dibujo, sin guardar PNG ni avisos al chat) para el estres. NO toma la
     * guardia: la retiene el llamador (Estres). Lanza si falla; los recursos se liberan igualmente.
     */
    static void renderSilencioso(Minecraft mc, int[] reg, int res, ParametrosCamara cam) {
        try (GeometriaCacheada geo = construirControlado(mc, reg)) {
            dibujar(mc, geo, reg, res, cam, s -> { });
        }
    }

    /** Region: dibuja la geometria cacheada con la camara dada (la escena es dibujarEscena con entidades). */
    private static BufferedImage dibujar(Minecraft mc, GeometriaCacheada geo, int[] reg, int res, ParametrosCamara cam,
                                         Consumer<String> aviso) {
        return aImagen(mc, res, aviso, () -> dibujarEscena(mc, geo, reg, cam, true, true));
    }

    /**
     * T-019/T-020/T-022/T-042: valida la resolucion y dibuja {@code escena} con supermuestreo x2 si la GPU y la memoria lo admiten
     * (antialiasing de bordes sin MSAA). Si el FBO interno no se puede crear o falta memoria, repite a factor 1 y avisa.
     * Devuelve siempre una imagen ARGB de exactamente res x res (RF-025). La escena se dibuja con el FBO ya enlazado y limpio
     * y puede ejecutarse dos veces (reintento): no debe tener efectos fuera de OpenGL.
     */
    private static BufferedImage aImagen(Minecraft mc, int res, Consumer<String> aviso, Runnable escena) {
        String err = ResolucionSalida.validar(res);
        if (err != null) throw new IllegalArgumentException(err);
        int factor = ResolucionSalida.factorSupermuestreo(res, FramebufferRender.maximoGpu());
        if (factor == 1) aviso.accept("Warning: your GPU does not support supersampling at " + res + " px; edges will not be smoothed.");
        int seguro = factorSeguro(res, factor);
        if (seguro < factor) aviso.accept("Warning: low memory for supersampling at " + res + " px; drawing without edge smoothing.");
        factor = seguro;
        try {
            return aImagen(mc, res, factor, aviso, escena);
        } catch (IllegalStateException | OutOfMemoryError e) {
            if (factor == 1) throw e;
            String causa = e instanceof OutOfMemoryError ? "out of memory" : e.getMessage();
            aviso.accept("Warning: not enough memory for supersampling (" + causa + "). Retrying without edge smoothing.");
            return aImagen(mc, res, 1, aviso, escena);
        }
    }

    /**
     * T-022 (RNF-014): mayor factor de supermuestreo que cabe en heap y, si el driver lo dice, en VRAM. Rechaza con
     * mensaje (IllegalStateException) si ni con factor 1 alcanza.
     */
    private static int factorSeguro(int res, int deseado) {
        int f = EstimacionMemoria.factor(res, deseado, EstimacionMemoria.libre(), FramebufferRender.vramLibre());
        if (f < deseado) { // medir de nuevo tras recolectar basura
            System.gc();
            f = EstimacionMemoria.factor(res, deseado, EstimacionMemoria.libre(), FramebufferRender.vramLibre());
        }
        if (f == 0) throw new IllegalStateException(EstimacionMemoria.mensaje(EstimacionMemoria.dibujo(res, 1), EstimacionMemoria.libre()));
        return f;
    }

    /**
     * Dibuja la escena (camara + opacas + entidades de bloque + translucidas) en el FBO YA enlazado y limpio. Compartido por
     * el PNG final y la vista previa del menu (T-025). El llamador captura y restaura el estado GL.
     * @param entidades dibujar entidades de bloque (la vista previa simplificada las omite)
     * @param reordenarT reordenar las translucidas para esta camara (false = reutiliza el orden anterior)
     */
    static void dibujarEscena(Minecraft mc, GeometriaCacheada geo, int[] reg, ParametrosCamara cam, boolean entidades, boolean reordenarT) {
        int nx = reg[3] - reg[0] + 1, ny = reg[4] - reg[1] + 1, nz = reg[5] - reg[2] + 1;
        // Cámara ortográfica parametrizable con encuadre automático (T-016). Sobrescribe proyeccion y modelview: EstadoGrafico las recarga.
        CamaraGL.aplicar(cam, nx, ny, nz, reg[0], reg[1], reg[2]);

        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE); // atlas de bloques
        // Lightmap del mundo apagado: la luz es plena y fija (mismo PNG de dia y de noche); el AO sigue en el color de vertice.
        mc.entityRenderer.disableLightmap();
        // Pasada 1, opacas: SOLID, CUTOUT_MIPPED, CUTOUT; alphaFunc GREATER 0.1 recorta hierba alta, antorchas.
        // El FBO guarda color PREMULTIPLICADO (rgb*a, a): sin mezcla (SRC_ALPHA, ZERO) para el color y alfa tal cual.
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ZERO,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        geo.dibujarOpacas(); // reproduce las display lists (sin releer el mundo)
        // Pasada 1b, entidades de bloque (T-021): cofres, camas, carteles, cabezas... con su TileEntitySpecialRenderer, tras los
        // solidos (que ocultan lo que quede detras) y antes de las translucidas. Cada una aislada con try/catch; restaura el estado GL.
        if (entidades) { geo.dibujarEntidades(mc, reg); geo.dibujarDecorativas(mc, reg); } // T-044: marcos, pinturas, armor stands
        // Pasada 2, translucidas (cristal tintado, agua, hielo...): ultima, de atras hacia delante para esta camara.
        // Test de profundidad ACTIVO (se ocultan tras los solidos) pero sin escribir profundidad (no se tapan entre si).
        // Mezcla sobre destino premultiplicado: rgb = src*a + dst*(1-a); alfa = src + dst*(1-src), asi el alfa
        // acumulado del PNG es correcto incluso sobre fondo transparente (RF-011, CA-011.2).
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        geo.dibujarTranslucidas(cam, reordenarT);
        GlStateManager.depthMask(true);
    }

    /**
     * Dibuja la escena en un FBO de (res*factor)^2, lo lee por franjas y lo reduce a res x res (el FBO es premultiplicado; ver ConversorImagen).
     * Restaura SIEMPRE el estado GL previo (EstadoGrafico) y libera el FBO, tambien si falla; avisa si queda un error GL.
     */
    private static BufferedImage aImagen(Minecraft mc, int res, int factor, Consumer<String> aviso, Runnable escena) {
        // Captura ANTES de tocar nada: framebuffer, viewport, matrices, banderas, unidades de textura...
        EstadoGrafico previo = EstadoGrafico.capturarCompleto(mc);
        FramebufferRender fbo = null;
        try {
            // Render fuera de pantalla: aislar estado GL.
            fbo = new FramebufferRender(res * factor); // lado interno; valida límites de GPU; lanza con mensaje en inglés

            fbo.enlazarYLimpiar(); // viewport res x res; limpia con alfa 0 y profundidad

            escena.run();

            // Lectura del FBO por franjas (RGBA, origen abajo-izquierda) y reduccion factor x factor con alfa
            // premultiplicado: bordes suavizados y sin halo; fondo exactamente 0x00000000 (RF-011, RNF-004).
            // Franjas de 128*factor filas: nunca se reserva lado*lado*4 bytes de golpe (RNF-014).
            int interno = res * factor, franja = 128 * factor; // multiplo del factor
            ByteBuffer buf = BufferUtils.createByteBuffer(interno * Math.min(franja, interno) * 4);
            int[] argb = new int[res * res];
            for (int y0 = 0; y0 < interno; y0 += franja) {
                int filas = Math.min(franja, interno - y0);
                fbo.leerFranja(buf, y0, filas);
                ConversorImagen.reducirFranja(buf, interno, y0, filas, factor, argb);
            }
            return ConversorImagen.aImagen(argb, res);
        } finally {
            if (fbo != null) fbo.close(); // libera el FBO (siempre, tambien con error) y reenlaza el framebuffer principal
            int glError = previo.restaurar(); // vuelve al estado PREVIO real y limpia glGetError
            if (glError != 0) {
                erroresGl++;
                aviso.accept("Warning: OpenGL reported an error (" + Fallos.nombreErrorGl(glError) + ") during the render; it has been cleared.");
            }
        }
    }
}
