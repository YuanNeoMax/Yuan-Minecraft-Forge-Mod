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
    }
}
