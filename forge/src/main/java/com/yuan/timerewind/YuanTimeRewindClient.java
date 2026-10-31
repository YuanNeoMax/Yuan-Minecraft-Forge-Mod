package com.yuan.timerewind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class YuanTimeRewindClient {
    private static final double FLIGHT_SPEED = 0.30D;

    /**
     * WATCHDOG GRACE. The server sends an End packet when playback finishes; a client that never
     * receives it -- disconnect mid-play, a dimension change eating the packet, a mod eating the
     * channel -- would otherwise stay in the rewind state FOREVER: camera taken over, input locked,
     * the playback filter stuck on screen, until the player reconnects. After the playback duration
     * plus this margin, the client recovers on its own. {@link #onEnd} is idempotent, so a late End
     * packet arriving after a self-recovery changes nothing.
     */
    private static final long END_GRACE_MILLIS = 1500L;
    /**
     * The same watchdog for the INSTANT playback mode, where {@code durationMillis} is 0 and there is
     * no duration to measure against. The End packet for an instant rewind is sent within a tick or
     * two of the Start, so a much shorter cap still leaves the real packet ample time.
     */
    private static final long NO_DURATION_END_MILLIS = 5000L;

    private static boolean active;
    private static boolean freeCamera;
    private static boolean freezeOthers;
    private static boolean positionRewindActive;
    private static Vec3 cameraPos = Vec3.ZERO;
    private static Vec3 interpolationStart = Vec3.ZERO;
    private static Vec3 targetPosition = Vec3.ZERO;
    private static int activePlayerId = -1;
    private static long startMillis;
    private static long durationMillis;
    private static float cameraYaw;
    private static float cameraPitch;
    private static double lastMouseX;
    private static double lastMouseY;
    private static long lastFrameMillis = 0L;
    /** Entity retreat targets per dimension (client-driven animation), sent in the StartPacket. */
    private static Map<ResourceKey<Level>, List<YuanTimeRewindRestorer.RetreatTarget>> retreatTargets = Map.of();
    private static final Map<UUID, Vec3> retreatStartPos = new HashMap<>();
    private static final Map<UUID, Float> retreatStartYRot = new HashMap<>();
    private static final Map<UUID, Float> retreatStartXRot = new HashMap<>();

    private YuanTimeRewindClient() {}

    public static void onStart(int playerId, int cameraMode, boolean freeCamRestorePosition,
                               boolean positionRewind, int positionMode, float playbackSeconds,
                               boolean freezeOtherPlayers, double targetX, double targetY, double targetZ,
                               Map<ResourceKey<Level>, List<YuanTimeRewindRestorer.RetreatTarget>> targets) {
        Minecraft mc = Minecraft.getInstance();
        int localPlayerId = mc.player == null ? -1 : mc.player.getId();
        boolean triggerPlayer = playerId == localPlayerId;
        active = true;
        lastFrameMillis = 0L;
        activePlayerId = playerId;
        freeCamera = isFreeCameraEnabled(cameraMode, playerId, localPlayerId);
        freezeOthers = freezeOtherPlayers;
        positionRewindActive = triggerPlayer && positionRewind && positionMode == 1;
        startMillis = System.currentTimeMillis();
        durationMillis = Float.isFinite(playbackSeconds) && playbackSeconds > 0.0f
                ? (long) (playbackSeconds * 1000L)
                : 0L;
        if (mc.player != null) {
            interpolationStart = mc.player.position();
            targetPosition = new Vec3(targetX, targetY, targetZ);
        }
        if (freeCamera && mc.player != null) {
            cameraPos = mc.player.getEyePosition(1.0f);
            cameraYaw = mc.player.getYRot();
            cameraPitch = mc.player.getXRot();
            lastMouseX = mc.mouseHandler.xpos();
            lastMouseY = mc.mouseHandler.ypos();
        }
        // Client-driven retreat animation: remember the targets and the entities' current
        // position/rotation so the playback eases them back to their old spots.
        retreatTargets = targets == null ? Map.of() : targets;
        retreatStartPos.clear();
        retreatStartYRot.clear();
        retreatStartXRot.clear();
        int localDimTargets = 0;
        if (mc.level != null) {
            List<YuanTimeRewindRestorer.RetreatTarget> dimTargets = retreatTargets.get(mc.level.dimension());
            localDimTargets = dimTargets == null ? 0 : dimTargets.size();
            if (dimTargets != null) {
                for (Entity e : mc.level.entitiesForRendering()) {
                    if (e == null || e.isRemoved()) continue;
                    YuanTimeRewindRestorer.RetreatTarget t = findTarget(dimTargets, e.getUUID());
                    if (t == null) continue;
                    retreatStartPos.put(e.getUUID(), e.position());
                    retreatStartYRot.put(e.getUUID(), e.getYRot());
                    retreatStartXRot.put(e.getUUID(), e.getXRot());
                }
            }
        }
    }

    private static YuanTimeRewindRestorer.RetreatTarget findTarget(
            List<YuanTimeRewindRestorer.RetreatTarget> targets, UUID uuid) {
        for (YuanTimeRewindRestorer.RetreatTarget t : targets) {
            if (t.uuid().equals(uuid)) return t;
        }
        return null;
    }

    static boolean isFreeCameraEnabled(int cameraMode, int playerId, int localPlayerId) {
        return cameraMode == 1 && playerId == localPlayerId;
    }

    public static void onEnd() {
        if (positionRewindActive) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.setPos(targetPosition.x, targetPosition.y, targetPosition.z);
            }
        }
        active = false;
        freeCamera = false;
        freezeOthers = false;
        positionRewindActive = false;
        activePlayerId = -1;
        durationMillis = 0L;
        lastFrameMillis = 0L;
        retreatTargets = Map.of();
        retreatStartPos.clear();
        retreatStartYRot.clear();
        retreatStartXRot.clear();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isFreeCamera() {
        return active && freeCamera;
    }

    public static boolean shouldFreezeEntity(Entity entity) {
        if (!active) return false;
        if (entity == null) return true;
        if (entity instanceof Player) {
            boolean trigger = entity.getId() == activePlayerId;
            if (trigger) return freeCamera;
            return freezeOthers;
        }
        // During animated playback non-player entities are driven by the client-driven retreat
        // animation (animateRetreat), so they must not be treated as frozen for packet/render
        // purposes; their AI is still frozen server-side.
        return false;
    }

    public static boolean shouldFreezeCamera() {
        if (!active) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        return !freeCamera && shouldFreezeEntity(mc.player);
    }

    public static Vec3 cameraPos() {
        return cameraPos;
    }

    public static float cameraYaw() {
        return cameraYaw;
    }

    public static float cameraPitch() {
        return cameraPitch;
    }

    public static void moveCamera(Vec3 delta) {
        cameraPos = cameraPos.add(delta);
    }

    public static void rotateCamera(float yawDelta, float pitchDelta) {
        cameraYaw += yawDelta;
        cameraPitch = Math.max(-90.0f, Math.min(90.0f, cameraPitch + pitchDelta));
    }

    public static boolean isDone() {
        return active && System.currentTimeMillis() - startMillis >= durationMillis;
    }

    private static void tick() {
        if (!active) return;
        long now = System.currentTimeMillis();

        // THE WATCHDOG, checked before anything else the state would act on. `isDone()` existed for
        // this purpose and had no caller: with it unused, a lost End packet left the client in the
        // rewind state permanently -- the audit's finding A. The cap is the playback duration plus a
        // grace for the packet, or a fixed short cap in the instant mode where there is no duration.
        long elapsed = now - startMillis;
        long cap = durationMillis > 0L ? durationMillis + END_GRACE_MILLIS : NO_DURATION_END_MILLIS;
        if (elapsed >= cap) {
            onEnd();
            return;
        }

        double frameScale = lastFrameMillis == 0L
                ? 1.0D
                : Math.min(2.0D, (now - lastFrameMillis) / 50.0D);
        lastFrameMillis = now;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && positionRewindActive
                && activePlayerId == mc.player.getId()
                && durationMillis > 0L) {
            double progress = Math.min(1.0D,
                    (now - startMillis) / (double) durationMillis);
            Vec3 visual = interpolationStart.lerp(targetPosition, progress);
            mc.player.setPos(visual.x, visual.y, visual.z);
        }
        animateRetreat(mc, now);
        if (!freeCamera) return;
        if (mc.player == null || activePlayerId != mc.player.getId() || mc.screen != null) return;
        tickMovement(mc, frameScale);
        tickMouseLook(mc);
    }

    /**
     * Client-driven retreat animation: every frame, ease each targeted entity in the local
     * dimension back toward its rewind target (ease-out, arriving exactly at the target by the
     * end of playback). Completely independent of the vanilla position-packet/lerp pipeline.
     */
    private static void animateRetreat(Minecraft mc, long now) {
        if (mc.level == null || retreatTargets.isEmpty() || durationMillis <= 0L) return;
        List<YuanTimeRewindRestorer.RetreatTarget> dimTargets = retreatTargets.get(mc.level.dimension());
        if (dimTargets == null || dimTargets.isEmpty()) return;
        double progress = Math.min(1.0D, (now - startMillis) / (double) durationMillis);
        double eased = 1.0D - Math.pow(1.0D - progress, 2.0D);
        int matched = 0;
        String sample = "none";
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == null || e.isRemoved() || e instanceof Player) continue;
            YuanTimeRewindRestorer.RetreatTarget t = findTarget(dimTargets, e.getUUID());
            if (t == null) continue;
            matched++;
            Vec3 start = retreatStartPos.get(e.getUUID());
            if (start == null) {
                start = e.position();
                retreatStartPos.put(e.getUUID(), start);
            }
            Float sYaw = retreatStartYRot.get(e.getUUID());
            Float sPitch = retreatStartXRot.get(e.getUUID());
            float startYaw = sYaw == null ? e.getYRot() : sYaw;
            float startPitch = sPitch == null ? e.getXRot() : sPitch;
            double nx = start.x + (t.x() - start.x) * eased;
            double ny = start.y + (t.y() - start.y) * eased;
            double nz = start.z + (t.z() - start.z) * eased;
            float nyRot = startYaw + shortestAngleDiff(t.yRot(), startYaw) * (float) eased;
            float nxRot = startPitch + shortestAngleDiff(t.xRot(), startPitch) * (float) eased;
            if (sample.equals("none")) {
                sample = e.getType().getDescriptionId() + " old=(" + start.x + "," + start.y + "," + start.z
                        + ") new=(" + nx + "," + ny + "," + nz + ")";
            }
            // Advance the render interpolation base so the renderer glides smoothly.
            e.xo = e.getX();
            e.yo = e.getY();
            e.zo = e.getZ();
            e.xOld = e.getX();
            e.yOld = e.getY();
            e.zOld = e.getZ();
            e.yRotO = e.getYRot();
            e.xRotO = e.getXRot();
            e.setPos(nx, ny, nz);
            e.setYRot(nyRot);
            e.setXRot(nxRot);
        }
    }

    private static float shortestAngleDiff(float target, float current) {
        return ((target - current + 540.0f) % 360.0f) - 180.0f;
    }

    private static void tickMovement(Minecraft mc, double frameScale) {
        double forward = (mc.options.keyUp.isDown() ? 1.0D : 0.0D)
                - (mc.options.keyDown.isDown() ? 1.0D : 0.0D);
        double strafe = (mc.options.keyLeft.isDown() ? 1.0D : 0.0D)
                - (mc.options.keyRight.isDown() ? 1.0D : 0.0D);
        double vertical = (mc.options.keyJump.isDown() ? 1.0D : 0.0D)
                - (mc.options.keyShift.isDown() ? 1.0D : 0.0D);
        if (forward == 0.0D && strafe == 0.0D && vertical == 0.0D) {
            return;
        }

        double yawRad = Math.toRadians(cameraYaw);
        double speed = FLIGHT_SPEED * frameScale;
        Vec3 delta = new Vec3(0.0D, vertical * speed, 0.0D);
        if (forward != 0.0D || strafe != 0.0D) {
            Vec3 horizontal = new Vec3(
                    -Math.sin(yawRad) * forward + Math.cos(yawRad) * strafe,
                    0.0D,
                    Math.cos(yawRad) * forward + Math.sin(yawRad) * strafe);
            if (horizontal.lengthSqr() > 1.0E-8D) {
                delta = delta.add(horizontal.normalize().scale(speed));
            }
        }
        moveCamera(delta);
    }

    private static void tickMouseLook(Minecraft mc) {
        if (!mc.mouseHandler.isMouseGrabbed()) {
            lastMouseX = mc.mouseHandler.xpos();
            lastMouseY = mc.mouseHandler.ypos();
            return;
        }
        double dx = mc.mouseHandler.xpos() - lastMouseX;
        double dy = mc.mouseHandler.ypos() - lastMouseY;
        lastMouseX = mc.mouseHandler.xpos();
        lastMouseY = mc.mouseHandler.ypos();
        double sensitivity = mc.options.sensitivity().get();
        double scale = Math.pow(0.6D * sensitivity + 0.2D, 3.0D) * 8.0D;
        double verticalDelta = dy * scale * 0.15D;
        if (mc.options.invertYMouse().get()) {
            verticalDelta = -verticalDelta;
        }
        rotateCamera((float) (dx * scale * 0.15D), (float) verticalDelta);
    }

    @Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientTick {
        private ClientTick() {}

        /**
         * DROP EVERYTHING ON LOGOUT, including the camera takeover.
         *
         * <p>The watchdog covers a client left in the rewind state with no server talking to it, but
         * it needs {@code duration + grace} to fire -- and a player who disconnects and reconnects
         * FASTER than that re-enters a world where {@code freeCamera} is still true, so the camera
         * mixin keeps cancelling Camera.setup against the OLD session's coordinates until the
         * watchdog finally runs. Same pattern as YuanDomainClient: a session ends, its state ends
         * with it. onEnd is idempotent, so this is safe even when nothing was running.
         */
        @SubscribeEvent
        public static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            onEnd();
        }

        @SubscribeEvent
        public static void onRenderTick(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.START) {
                tick();
            }
        }
    }
}
