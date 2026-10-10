package com.yuan.domain.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuan.domain.YuanDomainClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Draws the star-abyss floor for every active domain.
 *
 * <p>Runs at {@code AFTER_PARTICLES}, where the pose stack is camera-relative -- the same stage and
 * convention the project's other world-space effects use.
 *
 * <p>The floor is drawn per holder. The network packet carries only the holder's UUID, radius,
 * toggle and on/off state, so the position is resolved from the local player list each frame. That
 * keeps the floor glued to a moving player without a position stream, and it means an unloaded
 * holder simply is not drawn rather than being drawn at a stale spot.
 */
@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class YuanDomainRender {

    /** Ticks the open animation takes to reach full strength. */
    private static final float OPEN_TICKS = 11.0f;   // 0.55 s at 20 tps

    /** Ticks the close animation takes to reach zero. */
    private static final float CLOSE_TICKS = 8.0f;   // 0.4 s

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("YuanDomainRender");

    /** Per-holder open/close progress, 0..1. */
    private static final Map<UUID, Float> STRENGTH = new HashMap<>();

    /** Wall-clock of the previous frame, for converting frame deltas into ticks. */
    private static long lastFrameNanos = -1L;

    /** Guards the one-shot "shader missing" warning. */
    private static boolean warnedShaderMissing;

    private YuanDomainRender() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        if (YuanDomainClient.isEmpty()) {
            traceOnce("render event runs, but no domain is active on this client");
            return;
        }
        if (!YuanDomainShaders.ready()) {
            if (!warnedShaderMissing) {
                warnedShaderMissing = true;
                LOGGER.warn("domain is active but the starfield shader is not registered; "
                        + "the floor will not render. Look for a domain_starfield compile error above.");
            }
            return;
        }

        advanceAnimations();

        float seconds = YuanDomainShaders.advanceClock();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (Map.Entry<UUID, YuanDomainClient.Entry> entry : YuanDomainClient.active().entrySet()) {
            UUID holder = entry.getKey();
            float radius = entry.getValue().radius();
            float strength = STRENGTH.getOrDefault(holder, 0.0f);

            if (strength <= 0.001f) {
                traceOnce("skipped: open animation has not advanced yet (strength=" + strength + ")");
                continue;
            }
            // The toggle lives on the holder's sword, so its state arrives in the packet rather than
            // being read from a local config -- this client cannot read another player's sword.
            if (!entry.getValue().starfield()) {
                traceOnce("skipped: the holder has the star-field toggle switched off");
                continue;
            }

            Player player = findHolder(mc, level, holder);
            if (player == null) {
                traceOnce("skipped: holder " + holder + " is not among the loaded players");
                continue;
            }

            double cx = player.getX();
            double cz = player.getZ();

            YuanDomainShaders.update(seconds, radius, strength);
            // Do NOT call apply() here. The uniforms are pushed by the shader's own onApply
            // callback, which RenderType runs from setupRenderState while the program is bound.
            // Applying early uploads them to whatever program happens to be bound at the time and
            // clears CCUniform's dirty flag, after which the real apply() sees unchanged values and
            // skips the upload -- leaving the shader on its json defaults (domainIntensity = 0,
            // which discards every fragment).

            VertexConsumer consumer = buffers.getBuffer(YuanDomainRenderTypes.STARFIELD);
            int quads = YuanDomainFloorMesh.build(pose, consumer,
                    camera.x, camera.y, camera.z, cx, cz, radius);
            buffers.endBatch(YuanDomainRenderTypes.STARFIELD);

            traceDrew(quads, radius, strength, camera, cx, cz);
        }
    }

    /**
     * Finds a holder among the players this client has loaded.
     *
     * <p>Walks the local player list rather than {@code level.getPlayerByUUID}: that method is
     * defined on {@code Level} in terms of the server's player list, which a client does not have.
     * Walking the local list is what the rest of the project does and it is always safe.
     */
    private static Player findHolder(Minecraft mc, ClientLevel level, UUID holder) {
        LocalPlayer self = mc.player;
        if (self != null && holder.equals(self.getUUID())) {
            return self;
        }
        for (Player player : level.players()) {
            if (holder.equals(player.getUUID())) {
                return player;
            }
        }
        return null;
    }

    /**
     * Advances each holder's open/close strength.
     *
     * <p>Driven by real frame time rather than a tick event: the floor is a render-side effect and
     * must keep animating while the world is frozen. A time stop pauses ticks, and a floor that froze
     * mid-open would look broken.
     */
    private static void advanceAnimations() {
        long now = System.nanoTime();
        if (lastFrameNanos < 0L) {
            lastFrameNanos = now;
            return;
        }
        float deltaTicks = (now - lastFrameNanos) / 50_000_000.0f;   // 50 ms per tick
        lastFrameNanos = now;
        // Clamp so a stall cannot skip the whole animation.
        deltaTicks = Math.min(deltaTicks, 10.0f);

        for (UUID holder : YuanDomainClient.active().keySet()) {
            float current = STRENGTH.getOrDefault(holder, 0.0f);
            STRENGTH.put(holder, Math.min(1.0f, current + deltaTicks / OPEN_TICKS));
        }

        // Holders that are no longer active fade out, then are dropped.
        java.util.Iterator<Map.Entry<UUID, Float>> it = STRENGTH.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Float> e = it.next();
            if (YuanDomainClient.isActive(e.getKey())) {
                continue;
            }
            float next = e.getValue() - deltaTicks / CLOSE_TICKS;
            if (next <= 0.0f) {
                it.remove();
            } else {
                e.setValue(next);
            }
        }
    }

    /** Clears animation state; called on logout so a new session starts closed. */
    public static void reset() {
        STRENGTH.clear();
        lastFrameNanos = -1L;
        warnedShaderMissing = false;
        YuanDomainShaders.resetClock();
    }

    // ---------------------------------------------------------------- tracing
    //
    // The floor failing to appear leaves no trace of its own: every branch above returns quietly, and
    // a GLSL error surfaces at runtime rather than at build time. These report each distinct
    // condition once, so a single run distinguishes "the packet never arrived" from "it drew and is
    // hidden" from "the shader is missing". Remove once the floor is confirmed rendering.

    private static final java.util.Set<String> TRACED = new java.util.HashSet<>();

    private static long lastDrawTraceAt;

    private static void traceOnce(String note) {
        if (TRACED.add(note)) {
            LOGGER.info("domain trace: {}", note);
        }
    }

    /** Reports a successful draw at most once a second, so the log stays readable. */
    private static void traceDrew(int quads, float radius, float strength, Vec3 camera,
                                  double cx, double cz) {
        long now = System.currentTimeMillis();
        if (now - lastDrawTraceAt < 1000L) {
            return;
        }
        lastDrawTraceAt = now;
        LOGGER.info("domain trace: drew floor quads={} radius={} strength={} cam={},{},{} centre={},{}",
                quads, radius, round(strength),
                round(camera.x), round(camera.y), round(camera.z), round(cx), round(cz));
    }

    private static int round(double value) {
        return (int) Math.round(value);
    }
}
