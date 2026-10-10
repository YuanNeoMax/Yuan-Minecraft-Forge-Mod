package com.yuan.domain.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuan.domain.YuanDomainClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;

/**
 * Draws the star-abyss floor for every active domain.
 *
 * <p>Runs at {@code AFTER_PARTICLES}, where the pose stack is camera-relative -- the same stage and
 * convention the project's other world-space effects use.
 *
 * <p>The floor is drawn per holder. The network packet carries only the holder's UUID, radius and
 * on/off state, so the position is resolved from the local entity each frame. That keeps the floor
 * glued to a moving player without a position stream, and it is why an unloaded holder simply is not
 * drawn rather than being drawn at a stale spot.
 */
@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class YuanDomainRender {

    /** Ticks the open animation takes to reach full strength. */
    private static final float OPEN_TICKS = 11.0f;   // 0.55 s at 20 tps

    /** Ticks the close animation takes to reach zero. */
    private static final float CLOSE_TICKS = 8.0f;   // 0.4 s

    /** Per-holder open/close progress, 0..1. */
    private static final Map<UUID, Float> STRENGTH = new java.util.HashMap<>();

    /** Wall-clock of the previous frame, for converting frame deltas into ticks. */
    private static long lastFrameNanos = -1L;

    private YuanDomainRender() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (YuanDomainClient.isEmpty() || !YuanDomainShaders.ready()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        advanceAnimations(mc);

        float seconds = YuanDomainShaders.advanceClock();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (Map.Entry<UUID, YuanDomainClient.Entry> entry : YuanDomainClient.active().entrySet()) {
            UUID holder = entry.getKey();
            float radius = entry.getValue().radius();
            float strength = STRENGTH.getOrDefault(holder, 0.0f);
            if (strength <= 0.001f) {
                continue;
            }

            // The toggle lives on the holder's sword, so its state arrives in the packet rather than
            // being read from a local config -- this client cannot read another player's sword.
            if (!entry.getValue().starfield()) {
                continue;
            }

            Entity entity = level.getPlayerByUUID(holder);
            if (!(entity instanceof Player player)) {
                // Holder is not in view (other dimension, unloaded). Skip rather than guess.
                continue;
            }

            double cx = player.getX();
            double cz = player.getZ();

            YuanDomainShaders.update(seconds, radius, strength);
            // apply() runs the onApply callback, so uniforms are written for this draw.
            YuanDomainShaders.starfieldShader.apply();

            VertexConsumer consumer = buffers.getBuffer(YuanDomainRenderTypes.STARFIELD);
            YuanDomainFloorMesh.build(pose, consumer,
                    camera.x, camera.y, camera.z, cx, cz, radius);
            buffers.endBatch(YuanDomainRenderTypes.STARFIELD);
        }
    }

    /**
     * Advances each holder's open/close strength.
     *
     * <p>Driven by real frame time rather than a tick event: the floor is a render-side effect and
     * must keep animating while the world is frozen (time stop pauses ticks, and a floor that froze
     * mid-open would look broken).
     */
    private static void advanceAnimations(Minecraft mc) {
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
            float next = Math.min(1.0f, current + deltaTicks / OPEN_TICKS);
            STRENGTH.put(holder, next);
        }

        // Holders no longer active fade out, then are dropped.
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
        YuanDomainShaders.resetClock();
    }
}
