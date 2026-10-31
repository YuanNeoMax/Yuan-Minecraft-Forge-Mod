package com.yuan.timerewind;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class YuanTimeRewindServerState {
    private static final AtomicBoolean REWINDING = new AtomicBoolean(false);
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> DEATH_RETRIES = new ConcurrentHashMap<>();
    private static volatile long playbackUntilMillis = 0L;
    private static volatile UUID activePlayerUuid;
    private static volatile boolean freezeOthers;
    private static volatile int activeCameraMode = 0;
    private static volatile Vec3 activeTargetPosition = Vec3.ZERO;
    private static volatile Map<ResourceKey<Level>, List<YuanTimeRewindRestorer.RetreatTarget>> activeRetreatTargets = Map.of();

    private YuanTimeRewindServerState() {}

    public static boolean isRewinding() {
        return REWINDING.get();
    }

    public static boolean tryAcquire() {
        return REWINDING.compareAndSet(false, true);
    }

    public static void release() {
        REWINDING.set(false);
    }

    public static void startPlayback(UUID playerUuid, boolean freezeOtherPlayers, int cameraMode) {
        activePlayerUuid = playerUuid;
        freezeOthers = freezeOtherPlayers;
        activeCameraMode = cameraMode;
        activeTargetPosition = Vec3.ZERO;
        activeRetreatTargets = Map.of();
    }

    public static void endPlayback() {
        activePlayerUuid = null;
        freezeOthers = false;
        activeCameraMode = 0;
        activeTargetPosition = Vec3.ZERO;
        activeRetreatTargets = Map.of();
    }

    public static void setActiveRetreatTargets(
            Map<ResourceKey<Level>, List<YuanTimeRewindRestorer.RetreatTarget>> targets) {
        activeRetreatTargets = targets == null ? Map.of() : targets;
    }

    public static Map<ResourceKey<Level>, List<YuanTimeRewindRestorer.RetreatTarget>> getActiveRetreatTargets() {
        return activeRetreatTargets;
    }

    public static UUID getActivePlayerUuid() {
        return activePlayerUuid;
    }

    /**
     * True while the ACTIVE player is inside their own playback with the trigger NOT frozen.
     *
     * <p>THE 1-HEALTH WINDOW. A death rewind sets the trigger to 1 health and then plays back for
     * seconds; the default camera mode does not freeze them, so those seconds are exactly when a
     * second hit kills for real -- with the rewind lock held and the death handler returning
     * silently. The renderer/ai path applies this as a damage veto, which is cheaper and narrower
     * than granting invulnerable tags that would have to be un-tagged afterwards.
     */
    public static boolean isTriggerUnprotected(UUID playerUuid) {
        if (!isRewinding() || playerUuid == null) return false;
        return playerUuid.equals(activePlayerUuid) && activeCameraMode != 1;
    }

    public static boolean shouldFreezePlayer(Player player) {
        if (!isRewinding() || player == null || activePlayerUuid == null) return false;
        boolean trigger = player.getUUID().equals(activePlayerUuid);
        if (trigger) return activeCameraMode == 1;
        return freezeOthers;
    }

    public static boolean shouldFreezeEntity(Entity entity) {
        if (!isRewinding()) return false;
        if (entity instanceof Player player) return shouldFreezePlayer(player);
        return true;
    }

    public static void setActiveTargetPosition(Vec3 pos) {
        activeTargetPosition = pos == null ? Vec3.ZERO : pos;
    }

    public static Vec3 getActiveTargetPosition() {
        return activeTargetPosition;
    }

    public static void setPlaybackUntilMillis(long untilMillis) {
        playbackUntilMillis = Math.max(0L, untilMillis);
    }

    public static long getPlaybackUntilMillis() {
        return playbackUntilMillis;
    }

    public static boolean isPlaybackActive() {
        return System.currentTimeMillis() < playbackUntilMillis;
    }

    public static void clearPlayback() {
        playbackUntilMillis = 0L;
    }

    public static boolean cooldownReady(UUID playerUuid, int ticks) {
        if (playerUuid == null || ticks <= 0) return true;
        Long until = COOLDOWNS.get(playerUuid);
        return until == null || System.currentTimeMillis() >= until;
    }

    public static void startCooldown(UUID playerUuid, int ticks) {
        if (playerUuid == null) return;
        COOLDOWNS.put(playerUuid,
                System.currentTimeMillis() + Math.max(0L, ticks * 50L));
    }

    public static long cooldownRemainingMillis(UUID playerUuid) {
        if (playerUuid == null) return 0L;
        Long until = COOLDOWNS.get(playerUuid);
        return until == null ? 0L : Math.max(0L, until - System.currentTimeMillis());
    }

    public static void setDeathRetry(UUID playerUuid, int value) {
        if (playerUuid == null) return;
        DEATH_RETRIES.put(playerUuid, Math.max(0, value));
    }

    public static int getDeathRetry(UUID playerUuid) {
        if (playerUuid == null) return 0;
        return DEATH_RETRIES.getOrDefault(playerUuid, 0);
    }

    public static void resetDeathRetry(UUID playerUuid) {
        if (playerUuid != null) {
            DEATH_RETRIES.remove(playerUuid);
        }
    }

    public static void resetAll() {
        COOLDOWNS.clear();
        DEATH_RETRIES.clear();
        playbackUntilMillis = 0L;
        endPlayback();
        release();
    }
}
