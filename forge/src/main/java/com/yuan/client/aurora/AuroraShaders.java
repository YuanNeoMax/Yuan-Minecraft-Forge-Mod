package com.yuan.client.aurora;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.yuan.client.cosmic.CCShaderInstance;
import com.yuan.client.cosmic.CCUniform;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Owns the Aurora backdrop-blur shader and its per-pass uniforms.
 *
 * <p>Uniforms are written from an {@code onApply} callback rather than before the draw: a shader's
 * uniforms are only meaningful once the program is applied, and writing earlier lets the apply
 * overwrite them with the json defaults. The callback runs on every apply, so the values in force
 * are always the ones for the pass being drawn -- several passes run per frame with different
 * {@code Dir} steps through the same shader.
 */
@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AuroraShaders {

    private static final Logger LOGGER = LoggerFactory.getLogger("AuroraShaders");

    public static CCShaderInstance blurShader;

    private static CCUniform uDir;
    private static CCUniform uTint;
    private static CCUniform uColorModulator;

    // Values the onApply callback pushes. Held in fields because apply runs once per pass and
    // every pass must see its own step and tint.
    private static float dirX;
    private static float dirY;
    private static final float[] TINT = new float[4];

    private AuroraShaders() {
    }

    /** The compiled shader, or null until shaders are registered. */
    public static CCShaderInstance getShader() {
        return blurShader;
    }

    /** True once the shader is registered and every uniform resolved. */
    public static boolean ready() {
        return blurShader != null && uDir != null && uTint != null && uColorModulator != null;
    }

    /** Records the step and tint the next apply will push. {@code (0, 0)} means identity copy. */
    public static void setPass(float stepX, float stepY, float r, float g, float b, float a) {
        dirX = stepX;
        dirY = stepY;
        TINT[0] = r;
        TINT[1] = g;
        TINT[2] = b;
        TINT[3] = a;
    }

    private static void pushUniforms() {
        if (uDir == null) {
            return;
        }
        uDir.set(dirX, dirY);
        uTint.set(TINT[0], TINT[1], TINT[2], TINT[3]);
        uColorModulator.set(1.0f, 1.0f, 1.0f, 1.0f);
    }

    // ---- the glass pass ------------------------------------------------------------------------

    public static CCShaderInstance glassShader;

    private static CCUniform uGlass;
    private static CCUniform uGlassTint;
    private static CCUniform uGlassSize;
    private static CCUniform uGlassTexel;

    // Values the glass apply callback pushes. One panel is drawn per call, so these describe the
    // quad currently being drawn and are re-set before each one.
    private static final float[] GLASS = new float[4];
    private static final float[] GLASS_TINT = new float[4];
    private static final float[] GLASS_SIZE = new float[2];
    private static final float[] GLASS_TEXEL = new float[2];

    /** The compiled glass shader, or null until shaders are registered. */
    public static CCShaderInstance getGlassShader() {
        return glassShader;
    }

    /**
     * True once the glass shader is registered and every uniform resolved.
     *
     * <p>Checked separately from {@link #ready()}: the two passes are independent, and a glass
     * failure must not disable the backdrop blur -- the window is perfectly usable with a blurred
     * background and no refraction, but not with neither.
     */
    public static boolean glassReady() {
        return glassShader != null && uGlass != null && uGlassTint != null
                && uGlassSize != null && uGlassTexel != null;
    }

    /**
     * Records the parameters for the next glass draw.
     *
     * @param radiusPx   the panel's corner radius, so the refraction follows its own shape
     * @param strengthPx how far the image is displaced at the rim, in pixels
     * @param rim        rim highlight strength
     * @param shellAlpha the glass tint's alpha: how much the surface darkens what it refracts
     */
    public static void setGlassPass(float radiusPx, float strengthPx, float rim, float shellAlpha,
                                    float quadW, float quadH, float screenW, float screenH) {
        GLASS[0] = radiusPx;
        GLASS[1] = strengthPx;
        GLASS[2] = rim;
        GLASS[3] = 0f;
        GLASS_TINT[0] = 1f;
        GLASS_TINT[1] = 1f;
        GLASS_TINT[2] = 1f;
        GLASS_TINT[3] = shellAlpha;
        GLASS_SIZE[0] = Math.max(1f, quadW);
        GLASS_SIZE[1] = Math.max(1f, quadH);
        GLASS_TEXEL[0] = screenW > 0f ? 1f / screenW : 0f;
        GLASS_TEXEL[1] = screenH > 0f ? 1f / screenH : 0f;
    }

    private static void pushGlassUniforms() {
        if (uGlass == null) {
            return;
        }
        uGlass.set(GLASS[0], GLASS[1], GLASS[2], GLASS[3]);
        uGlassTint.set(GLASS_TINT[0], GLASS_TINT[1], GLASS_TINT[2], GLASS_TINT[3]);
        uGlassSize.set(GLASS_SIZE[0], GLASS_SIZE[1]);
        uGlassTexel.set(GLASS_TEXEL[0], GLASS_TEXEL[1]);
        uColorModulator.set(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                CCShaderInstance.create(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath("yuan", "aurora_blur"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> {
                    blurShader = (CCShaderInstance) shader;
                    uDir = blurShader.getUniform("Dir");
                    uTint = blurShader.getUniform("Tint");
                    uColorModulator = blurShader.getUniform("ColorModulator");
                    blurShader.onApply(AuroraShaders::pushUniforms);

                    // Names are reported because a typo in a uniform name is not an error anywhere:
                    // the shader compiles, the lookup returns null, the value keeps its json default
                    // and the effect is simply wrong with nothing in the log.
                    LOGGER.info("aurora blur shader registered (uniforms resolved: "
                                    + "dir={} tint={} colorModulator={})",
                            uDir != null, uTint != null, uColorModulator != null);
                });

        // THE GLASS PASS. A second shader rather than a mode inside the first: the two do different
        // jobs (one smears the whole screen, the other refracts inside a rectangle) and share no
        // uniforms beyond the tint. Folding them together would mean branching in the fragment stage
        // and carrying the blur's ping-pong state through a pass that does not use it.
        event.registerShader(
                CCShaderInstance.create(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath("yuan", "aurora_glass"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> {
                    glassShader = (CCShaderInstance) shader;
                    uGlass = glassShader.getUniform("Glass");
                    uGlassTint = glassShader.getUniform("Tint");
                    uGlassSize = glassShader.getUniform("QuadSizePx");
                    uGlassTexel = glassShader.getUniform("ScreenTexelSize");
                    glassShader.onApply(AuroraShaders::pushGlassUniforms);

                    LOGGER.info("aurora glass shader registered (uniforms resolved: "
                                    + "glass={} tint={} size={} texel={})",
                            uGlass != null, uGlassTint != null, uGlassSize != null, uGlassTexel != null);
                });
    }
}
