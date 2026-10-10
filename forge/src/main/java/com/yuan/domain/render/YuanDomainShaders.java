package com.yuan.domain.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.yuan.client.cosmic.CCShaderInstance;
import com.yuan.client.cosmic.CCUniform;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector4f;

import java.io.IOException;

/**
 * Owns the star-abyss floor shader and its uniforms.
 *
 * <p>Uniforms are written from an {@code onApply} callback rather than once per frame: a
 * {@code ShaderInstance}'s uniforms are only meaningful once it is applied, and writing them
 * earlier lets the apply overwrite them with the defaults from the shader json. The callback runs
 * on every apply, so the values are always current.
 *
 * <p>There is no centre uniform: the mesh carries each vertex's offset from the domain centre in
 * its UV0, so the fragment stage derives the radial distance from that. It avoids a second source
 * of truth for the centre and keeps precision usable far from the world origin.
 */
@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class YuanDomainShaders {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("YuanDomainShaders");

    public static CCShaderInstance starfieldShader;

    private static CCUniform uTime;
    private static CCUniform uRadius;
    private static CCUniform uFade;
    private static CCUniform uSeed;
    private static CCUniform uIntensity;
    private static CCUniform uColorBase;
    private static CCUniform uColorStar;
    private static CCUniform uColorCore;

    // Values the onApply callback pushes. Held in fields because apply can run more than once per
    // frame (several buffers, several passes) and every apply must write the same values.
    private static float time;
    private static float radius = 20.0f;
    private static float fade = 6.0f;
    private static float seed;
    private static float intensity;
    private static final Vector4f COLOR_BASE = new Vector4f(0.0f, 0.02f, 0.03f, 0.92f);
    private static final Vector4f COLOR_STAR = new Vector4f(0.75f, 0.85f, 1.0f, 1.0f);
    private static final Vector4f COLOR_CORE = new Vector4f(1.0f, 0.48f, 0.36f, 0.55f);

    private YuanDomainShaders() {
    }

    /** The compiled shader, for the render type's shader state. Null until shaders are registered. */
    public static CCShaderInstance getShader() {
        return starfieldShader;
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                CCShaderInstance.create(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath("yuan", "domain_starfield"),
                        DefaultVertexFormat.POSITION_COLOR_TEX),
                shader -> {
                    starfieldShader = (CCShaderInstance) shader;
                    uTime = starfieldShader.getUniform("time");
                    uRadius = starfieldShader.getUniform("domainRadius");
                    uFade = starfieldShader.getUniform("domainFade");
                    uSeed = starfieldShader.getUniform("domainSeed");
                    uIntensity = starfieldShader.getUniform("domainIntensity");
                    uColorBase = starfieldShader.getUniform("domainColorBase");
                    uColorStar = starfieldShader.getUniform("domainColorStar");
                    uColorCore = starfieldShader.getUniform("domainColorCore");
                    starfieldShader.onApply(YuanDomainShaders::pushUniforms);
                    // Confirms the shader compiled. A GLSL error here fails at runtime, not at
                    // build time, so this line is the difference between a silent no-show and a
                    // diagnosable one.
                    LOGGER.info("domain starfield shader registered");
                });
    }

    private static void pushUniforms() {
        if (uTime != null) uTime.set(time);
        if (uRadius != null) uRadius.set(radius);
        if (uFade != null) uFade.set(fade);
        if (uSeed != null) uSeed.set(seed);
        if (uIntensity != null) uIntensity.set(intensity);
        if (uColorBase != null) uColorBase.set(COLOR_BASE);
        if (uColorStar != null) uColorStar.set(COLOR_STAR);
        if (uColorCore != null) uColorCore.set(COLOR_CORE);
    }

    /**
     * Updates the per-frame values, before the floor is drawn.
     *
     * @param seconds  elapsed seconds from {@link #advanceClock()}, driving drift and twinkle
     * @param r        domain radius in blocks
     * @param strength 0..1 open/close fade; the floor is invisible at 0
     */
    public static void update(float seconds, float r, float strength) {
        time = seconds;
        radius = r;
        // The rim scales with the domain so a small domain is not swallowed by its own edge.
        fade = Math.max(2.0f, r * 0.3f);
        seed = 0.0f;
        intensity = strength;
    }

    /** True when the shader loaded and the floor can be drawn. */
    public static boolean ready() {
        return starfieldShader != null;
    }

    // Elapsed seconds, accumulated per frame. Deliberately NOT derived from an absolute clock:
    // a float holds ~7 significant digits, so epoch seconds (~1.7e9) would quantise to ~128-second
    // steps and the drift and twinkle would visibly stall. Accumulating from zero keeps full
    // resolution for the first hours of play.
    private static float elapsedSeconds;
    private static long lastFrameMillis = -1L;

    /**
     * Advances the shader clock and returns elapsed seconds since the first call.
     *
     * <p>Called once per frame. The step is clamped so a lag spike or a window that was paused
     * cannot make the field jump.
     */
    public static float advanceClock() {
        long now = net.minecraft.Util.getMillis();
        if (lastFrameMillis < 0L) {
            lastFrameMillis = now;
            return elapsedSeconds;
        }
        long delta = now - lastFrameMillis;
        lastFrameMillis = now;
        // 250 ms cap: beyond that the player was not watching, so the field should not lurch.
        elapsedSeconds += Math.min(delta, 250L) / 1000.0f;
        return elapsedSeconds;
    }

    /** Resets the clock; call on logout so a new session starts from a clean field. */
    public static void resetClock() {
        lastFrameMillis = -1L;
        elapsedSeconds = 0.0f;
    }
}
