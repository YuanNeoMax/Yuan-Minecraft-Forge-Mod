package com.yuan.space_slash;

import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.gameobject.emitter.data.EmissionSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.material.TextureMaterial;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.RandomConstant;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.Curve;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.curve.ECBCurves;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Cone;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleConfig;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays the space-slash streak at a hit point. The FX is built in code (Photon's editor exports
 * compressed-NBT {@code .fx} files, which would add an author→copy→rebuild loop for no gain), but an
 * authored {@code yuan:fx/space_slash_rain.fx} resource wins if one is ever shipped, so the editor
 * route stays open. Every call is guarded: Photon is a nested mod and a failure here must never take
 * down the base slash rendering, and failures log at most once per 30s.
 *
 * <p>Positioning and orientation deliberately mirror {@link YuanSpaceSlashEffect}, the effect the user
 * signed off on: centred on the hit point, spanning {@code -length/2..+length/2} along the swing
 * direction projected into the screen plane.
 */
public final class YuanPhotonSlashBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("YuanSpaceSlash");
    private static final ResourceLocation SLASH_RAIN_FX =
            ResourceLocation.fromNamespaceAndPath("yuan", "space_slash_rain");
    /** {@code FXHelper} resolves an fx location to {@code assets/<ns>/fx/<path>.fx}. */
    private static final ResourceLocation SLASH_RAIN_FX_RESOURCE =
            ResourceLocation.fromNamespaceAndPath("yuan", "fx/space_slash_rain.fx");
    /**
     * Our own streak sprite: U runs along the trail ribbon and V across it (verified in
     * {@code TrailParticle#getUVs} for uvMode Stretch), so it carries the cross-section only — a hot
     * white core with gaussian falloff, flat along the length.
     */
    private static final ResourceLocation STREAK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("yuan", "textures/effect/slash_streak.png");

    /** Temporary, per user request: one streak per normal attack until the look is signed off. */
    private static final int STREAKS_PER_HIT = 1;
    private static final int EMITTER_LIFE_TICKS = 30;
    /** Sweep time; matches the legacy blade's 0.26s fade so both read at the same speed. */
    private static final int STREAK_LIFE_TICKS = 5;
    /** Travel per particle is life * speed * 0.05 blocks, so speed = length / (life * 0.05). */
    private static final float SPEED_JITTER = 0.1F;
    private static final float HEAD_SIZE = 0.1F;
    /**
     * Measured off the reference frames: streaks run 20~60 length-to-full-width, so the ribbon's
     * half-width is ~2% of its length. (The legacy blade's 0.09 ratio is for a solid mesh, not this.)
     */
    private static final float WIDTH_RATIO = 0.02F;
    private static final float MIN_HALF_WIDTH = 0.04F;
    private static final float MAX_HALF_WIDTH = 0.16F;
    /** Small half-angle so repeated hits cross slightly instead of stacking perfectly. */
    private static final float STREAK_SPREAD_DEGREES = 8.0F;
    /** Below this the swing is nearly parallel to the view vector; legacy uses the same epsilon. */
    private static final float PARALLEL_EPSILON_SQR = 0.0064F;

    /** Null until we have looked for an authored fx once; Photon caches a miss as an exception per call. */
    private static Boolean authoredFxPresent;
    private static long lastFailureLogMillis;

    private YuanPhotonSlashBridge() {
    }

    public static void playRain(ClientLevel level, Entity target, Vec3 hit,
                                float yaw, float pitch, float length) {
        try {
            if (target == null || target.isRemoved()) {
                return;
            }
            FX fx = resolveFx(length);
            if (fx == null) {
                return;
            }
            // Anchored to the target, not to a block: BlockEffect#start puts the root at the block
            // center plus the offset, a constant half-block off the click, and kills the fx when the
            // block state changes.
            EntityEffect effect = new EntityEffect(fx, level, target, EntityEffect.AutoRotate.NONE);
            Vec3 anchor = target.position();
            effect.setOffset(new Vector3f(
                    (float) (hit.x - anchor.x),
                    (float) (hit.y - anchor.y),
                    (float) (hit.z - anchor.z)));
            effect.setRotation(slashRotation(yaw, pitch));
            // Required for the code-built fx: it has no location, and the dedup pass compares null==null.
            effect.setAllowMulti(true);
            // Decorative: when the target goes away, drop the streak instead of letting it linger.
            effect.setForcedDeath(true);
            effect.start();
        } catch (Throwable t) {
            long now = System.currentTimeMillis();
            if (now - lastFailureLogMillis > TimeUnit.SECONDS.toMillis(30)) {
                lastFailureLogMillis = now;
                LOGGER.warn("[YuanSpaceSlash] photon streak fx unavailable", t);
            }
        }
    }

    private static FX resolveFx(float length) {
        if (authoredFxPresent == null) {
            authoredFxPresent = !Minecraft.getInstance().getResourceManager()
                    .getResourceStack(SLASH_RAIN_FX_RESOURCE).isEmpty();
        }
        if (authoredFxPresent) {
            FX authored = FXHelper.getFX(SLASH_RAIN_FX);
            if (authored != null) {
                return authored;
            }
        }
        return buildStreak(length);
    }

    /**
     * Rotation taking the emitters' local +Y (the cone axis) onto the slash direction: the swing vector
     * projected into the screen plane, exactly like the legacy blade's long axis. Without the
     * projection a swing aimed at the target points almost straight away from the camera, and the
     * streak foreshortens into a dot that looks like it appeared in the wrong place.
     */
    private static Quaternionf slashRotation(float yaw, float pitch) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f right = YuanSpaceSlashEffect.normalizeOrElse(
                new Vector3f(look).cross(up, new Vector3f()), new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f attack = YuanSpaceSlashEffect.direction(yaw, pitch).toVector3f();
        Vector3f axis = YuanSpaceSlashEffect.screenProjection(attack, right, up);
        if (axis.lengthSquared() < PARALLEL_EPSILON_SQR) {
            float angle = (float) (Math.PI / 4.0D);
            axis = new Vector3f(right).mul((float) Math.cos(angle))
                    .add(new Vector3f(up).mul((float) Math.sin(angle)));
        }
        axis.normalize();
        return new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), axis);
    }

    /**
     * One streak per hit, built per call because its speed, start offset and width all derive from the
     * target-sized length; {@code FXRuntime} shallow-copies the emitter per play but keeps this config,
     * so a cached fx could not follow the target's size.
     */
    private static FX buildStreak(float length) {
        float halfLength = length * 0.5F;
        float speed = length / (STREAK_LIFE_TICKS * 0.05F);

        ParticleEmitter emitter = new ParticleEmitter();
        ParticleConfig config = emitter.config;

        config.setLooping(false);
        config.setDuration(EMITTER_LIFE_TICKS);
        config.setMaxParticles(STREAKS_PER_HIT);
        config.setStartLifetime(NumberFunction.constant(STREAK_LIFE_TICKS));
        config.setStartSpeed(new RandomConstant(
                speed * (1.0F - SPEED_JITTER), speed * (1.0F + SPEED_JITTER), true));
        config.setStartSize(new NumberFunction3(HEAD_SIZE, HEAD_SIZE, HEAD_SIZE));
        config.setStartColor(NumberFunction.color(-1));

        EmissionSetting.Burst burst = new EmissionSetting.Burst();
        burst.time = 0;
        burst.cycles = 1;
        burst.interval = 1;
        burst.probability = 1.0F;
        burst.setCount(NumberFunction.constant(STREAKS_PER_HIT));
        config.emission.setEmissionRate(NumberFunction.constant(0));
        config.emission.setEmissionMode(EmissionSetting.Mode.Exacting);
        config.emission.getBursts().add(burst);

        // The cone fans around local +Y, which the effect rotation points along the slash. Starting the
        // particles at -length/2 on that axis makes the ribbon sweep through the hit point, so the cut
        // is centred on it instead of growing out to one side.
        Cone fan = new Cone();
        fan.setAngle(STREAK_SPREAD_DEGREES);
        fan.setRadius(0.5F);
        fan.setRadiusThickness(1.0F);
        fan.setArc(360.0F);
        config.shape.setShape(fan);
        config.shape.setPosition(new NumberFunction3(
                NumberFunction.constant(0),
                NumberFunction.constant(-halfLength),
                NumberFunction.constant(0)));
        config.shape.setScale(new NumberFunction3(
                NumberFunction.constant(0.01F),
                NumberFunction.constant(0.01F),
                NumberFunction.constant(0.01F)));

        // The head particle is a plain soft dot; the streak is the trail ribbon it drags.
        config.trails.setEnable(true);
        config.trails.setDieWithParticles(true);
        config.trails.setSizeAffectsWidth(false);
        config.trails.setInheritParticleColor(false);
        // 0xFFFFF4F4 — warm white, so the streak does not read as flat grey.
        config.trails.setColorOverLifetime(NumberFunction.color(-2828));
        // Taper the ribbon itself to a point at both ends: Curve returns lower + (upper-lower)*curveY,
        // so the half-width rides on the curve's upper bound and the sprite can stay flat along U.
        float halfWidth = Mth.clamp(length * WIDTH_RATIO, MIN_HALF_WIDTH, MAX_HALF_WIDTH);
        Curve taper = new Curve(0.0F, 1.0F, 0.0F, halfWidth, halfWidth, "trail", "width");
        taper.setCurves(new ECBCurves(0.0F, 0.0F, 0.18F, 1.0F, 0.82F, 1.0F, 1.0F, 0.0F));
        config.trails.config.setWidthOverTrail(taper);
        config.trails.config.setColorOverTrail(NumberFunction.color(-1));
        config.trails.config.material.setMaterial(new TextureMaterial(STREAK_TEXTURE));
        // The reference's attack fx blooms the ribbon, not the head particle.
        config.trails.config.getRenderer().setBloomEffect(true);
        config.trails.config.getRenderer().setBloomColor(-1);

        FX fx = new FX();
        fx.getMainFX().objects().add(emitter);
        return fx;
    }
}
