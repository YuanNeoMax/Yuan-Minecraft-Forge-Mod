package com.yuan.domain.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.yuan.client.cosmic.CCShaderInstance;
import com.yuan.client.cosmic.CCUniform;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;

/**
 * Owns the star-abyss post-process shader and its uniforms.
 *
 * <p>Uniforms are written from an {@code onApply} callback rather than once per frame: a
 * {@code ShaderInstance}'s uniforms are only meaningful once it is applied, and writing them earlier
 * lets the apply overwrite them with the defaults from the shader json. The callback runs on every
 * apply, so the values are always current.
 *
 * <p>There is no radius uniform. The field covers the whole visible world, so a cut-off would only
 * be a smaller version of the effect; the one thing with a size of its own is the glow around the
 * caster, and that constant lives in the fragment stage.
 */
@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class YuanDomainShaders {

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("YuanDomainShaders");

    public static CCShaderInstance postShader;

    private static CCUniform uTime;
    private static CCUniform uSeed;
    private static CCUniform uIntensity;
    private static CCUniform uColorBase;
    private static CCUniform uColorStar;
    private static CCUniform uColorCore;
    private static CCUniform uInverseViewProjection;
    private static CCUniform uCenter;
    private static CCUniform uCameraWorldPos;

    // Values the onApply callback pushes. Held in fields because apply can run more than once per
    // frame (several buffers, several passes) and every apply must write the same values.
    private static float time;
    private static float seed;
    private static float intensity;
    private static final Matrix4f INVERSE_VIEW_PROJECTION = new Matrix4f();
    private static final Vector3f CENTER = new Vector3f();
    private static final Vector3f CAMERA_WORLD_POS = new Vector3f();
    private static final Vector4f COLOR_BASE = new Vector4f(0.0f, 0.02f, 0.03f, 0.92f);
    private static final Vector4f COLOR_STAR = new Vector4f(0.75f, 0.85f, 1.0f, 1.0f);
    private static final Vector4f COLOR_CORE = new Vector4f(1.0f, 0.48f, 0.36f, 0.55f);

    /** Accumulated seconds. A float cannot hold an absolute clock: see {@link #advanceClock}. */
    private static float elapsedSeconds;

    private static long lastFrameMillis = -1L;

    private YuanDomainShaders() {
    }

    /** The compiled shader, or null until shaders are registered. */
    public static CCShaderInstance getShader() {
        return postShader;
    }

    /** True once the shader has been registered and every uniform resolved. */
    public static boolean ready() {
        return postShader != null
                && uTime != null && uSeed != null && uIntensity != null
                && uColorBase != null && uColorStar != null && uColorCore != null
                && uInverseViewProjection != null && uCenter != null
                && uCameraWorldPos != null;
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                CCShaderInstance.create(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath("yuan", "domain_starfield_post"),
                        DefaultVertexFormat.POSITION_TEX),
                shader -> {
                    postShader = (CCShaderInstance) shader;
                    uTime = postShader.getUniform("time");
                    uSeed = postShader.getUniform("domainSeed");
                    uIntensity = postShader.getUniform("domainIntensity");
                    uColorBase = postShader.getUniform("domainColorBase");
                    uColorStar = postShader.getUniform("domainColorStar");
                    uColorCore = postShader.getUniform("domainColorCore");
                    uInverseViewProjection = postShader.getUniform("InverseViewProjection");
                    uCenter = postShader.getUniform("DomainCenter");
                    uCameraWorldPos = postShader.getUniform("CameraWorldPos");

                    postShader.onApply(YuanDomainShaders::pushUniforms);

                    // Names are reported on registration because a typo in a uniform name is not an
                    // error anywhere: the shader compiles, the lookup returns null, the value keeps
                    // its json default, and the effect is simply wrong with nothing in the log.
                    LOGGER.info("domain post shader registered (uniforms resolved: "
                                    + "time={} seed={} intensity={} base={} star={} core={} invViewProj={} center={} cameraPos={})",
                            uTime != null, uSeed != null, uIntensity != null,
                            uColorBase != null, uColorStar != null, uColorCore != null,
                            uInverseViewProjection != null, uCenter != null,
                            uCameraWorldPos != null);
                });
    }

    /**
     * Records the values the next apply will push.
     *
     * <p>Called once per holder per frame, immediately before that holder's draw. The callback reads
     * these fields when it runs, so the values in force are always the ones for the pass being drawn.
     */
    public static void update(float seconds, float strength, Matrix4f inverseViewProjection,
                              Vector3f center, Vector3f cameraWorldPos, float holderSeed) {
        time = seconds;
        intensity = strength;
        seed = holderSeed;
        INVERSE_VIEW_PROJECTION.set(inverseViewProjection);
        CENTER.set(center);
        CAMERA_WORLD_POS.set(cameraWorldPos);
    }

    private static void pushUniforms() {
        if (uTime == null) {
            return;
        }
        uTime.set(time);
        uSeed.set(seed);
        uIntensity.set(intensity);
        uColorBase.set(COLOR_BASE);
        uColorStar.set(COLOR_STAR);
        uColorCore.set(COLOR_CORE);
        uInverseViewProjection.glUniformMatrix4f(INVERSE_VIEW_PROJECTION);
        uCenter.set(CENTER);
        uCameraWorldPos.set(CAMERA_WORLD_POS);
    }

    /**
     * Advances the shader clock and returns elapsed seconds since the first call.
     *
     * <p>Accumulated rather than read from an absolute clock: an absolute time in seconds stops
     * being representable in a float after a few hours, and the field would visibly stall. The step
     * is clamped so a lag spike or a paused window cannot make the field jump.
     */
    public static float advanceClock() {
        long now = net.minecraft.Util.getMillis();
        if (lastFrameMillis < 0L) {
            lastFrameMillis = now;
            return elapsedSeconds;
        }
        long delta = now - lastFrameMillis;
        lastFrameMillis = now;
        elapsedSeconds += Math.min(delta, 250L) / 1000.0f;
        return elapsedSeconds;
    }

    /** Resets the clock; call on logout so a new session starts from a clean field. */
    public static void resetClock() {
        lastFrameMillis = -1L;
        elapsedSeconds = 0.0f;
    }
}
