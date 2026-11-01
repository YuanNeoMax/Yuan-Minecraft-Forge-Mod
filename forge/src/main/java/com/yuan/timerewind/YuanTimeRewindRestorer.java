package com.yuan.timerewind;

import com.yuan.item.YuanGodSwordConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreboardSaveData;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YuanTimeRewindRestorer {
    /**
     * How many entity glide targets one dimension may carry.
     *
     * <p>Chosen against the wire, not against a frame budget: 512 entries is ~23 KB per dimension,
     * which a SimpleChannel frame carries comfortably, while a thousand-entity farm would not. The
     * cost of the cap is only that entities beyond it stop animating and land immediately.
     */
    private static final int MAX_RETREAT_TARGETS_PER_LEVEL = 512;

    /**
     * The longest a playback may hold the world frozen, in seconds.
     *
     * <p>A hard ceiling rather than a tighter clamp on the config: the config is per-sword and
     * written by the player, while this is a property of what the SERVER can survive. Twenty
     * seconds is already far longer than any readable rewind animation; past that the trigger is
     * watching a stopped world and everyone else on it is simply waiting.
     */
    private static final float MAX_PLAYBACK_SECONDS = 20.0f;
    private static final Logger LOGGER = LoggerFactory.getLogger("YuanTimeRewind");
    private static final float SAFE_HEALTH = 10.0f;
    private static final int SAFE_FOOD_LEVEL = 6;
    private static final double MIN_SAFE_Y = -64.0;
    private static final double HOSTILE_CHECK_RADIUS = 16.0;

    private static int restoredBlockCount;
    private static int restoredEntityCount;
    private static int failedRestoreCount;

    /** A single entity's playback target position, sent to clients for the client-driven retreat animation. */
    public record RetreatTarget(UUID uuid, double x, double y, double z, float yRot, float xRot) {}

    private record LevelRestoreData(ServerLevel level, List<Object> after,
                                    List<Object> before, long targetTick) {}

    private YuanTimeRewindRestorer() {}

    private static void resetRestoreStats() {
        restoredBlockCount = 0;
        restoredEntityCount = 0;
        failedRestoreCount = 0;
    }

    private static void showRestoreStats(ServerPlayer player, YuanGodSwordConfig config) {
        if (player == null || isPlayerGone(player) || config == null || !config.rewindShowStats) return;
        String msg = "§b[回溯] §7还原方块 §f" + restoredBlockCount
                + " §7· 实体 §f" + restoredEntityCount;
        if (failedRestoreCount > 0) {
            msg += " §c· 失败 " + failedRestoreCount;
        }
        player.displayClientMessage(Component.literal(msg), true);
    }

    /**
     * Effective position rewind for a NON-death rewind is exactly the user's explicit
     * {@code rewindPositionRewind} toggle. The B free-camera mode ("freeCamRestorePosition")
     * no longer forces a teleport back to the snapshot position: the B-camera body is already
     * frozen at the trigger spot during playback and the camera reattaches there when it ends,
     * so teleporting to a 10-second-old snapshot position reads as a random "乱tp".
     */
    public static boolean effectivePositionRewind(YuanGodSwordConfig config) {
        return config != null && config.rewindPositionRewind;
    }

    public static YuanGodSwordConfig withEffectivePositionRewind(YuanGodSwordConfig source, boolean death) {
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.copyFrom(source);
        config.rewindPositionRewind = death || effectivePositionRewind(source);
        return config;
    }

    public static boolean restore(ServerLevel triggerLevel, YuanGodSwordConfig config,
                                  ServerPlayer player, boolean death, int retry) {
        if (!YuanTimeRewindServerState.tryAcquire()) {
            if (player != null && !isPlayerGone(player)) {
                player.displayClientMessage(
                        Component.literal("§c[回溯] 正在回溯中，请稍候"), true);
            }
            return false;
        }
        resetRestoreStats();
        if (player != null) {
            YuanTimeRewindServerState.startPlayback(player.getUUID(),
                    config.rewindFreezeOthers, config.rewindCameraMode);
        }
        boolean started = false;
        try {
            List<ServerLevel> levels = new ArrayList<>();
            if (config.rewindScopeMode == 0 && triggerLevel.getServer() != null) {
                for (ServerLevel level : triggerLevel.getServer().getAllLevels()) {
                    levels.add(level);
                }
            } else {
                levels.add(triggerLevel);
            }

            List<LevelRestoreData> restores = new ArrayList<>();
            YuanTimeRewindRecorder.PlayerSnapshot playerSnapshot = null;
            YuanTimeRewindRecorder.PlayerSnapshot nextPlayerSnapshot = null;
            long triggerTargetTick = 0L;
            for (ServerLevel level : levels) {
                YuanTimeRewindRecorder recorder = YuanTimeRewindEvents.recorder(level);
                if (recorder == null) continue;
                long targetTick = level.getGameTime() - config.rewindWindowSeconds * 20L;
                if (death) {
                    targetTick = level.getGameTime()
                            - config.rewindWindowSeconds * 20L * Math.max(1, retry);
                }
                if (death && config.rewindSafetyCheckpoint && level == triggerLevel) {
                    targetTick = findSafeTick(recorder, level, targetTick, config, player);
                }
                List<Object> after = recorder.since(targetTick);
                List<Object> before = historyAtOrBeforeFallback(recorder, targetTick);
                if (config.rewindScope == 1) {
                    after = filterSpatialHistory(level, after, config, player);
                    before = filterSpatialHistory(level, before, config, player);
                }
                if (level == triggerLevel) {
                    playerSnapshot = findPlayerSnapshot(player, before);
                    nextPlayerSnapshot = findNextPlayerSnapshot(player, after);
                    triggerTargetTick = targetTick;
                }
                restores.add(new LevelRestoreData(level, after, before, targetTick));
            }

            // THE CROSS-DIMENSION FALLBACK, and why it exists. Player snapshots are recorded PER
            // DIMENSION -- each recorder only sees the players of its own level. A player who walked
            // through a portal five seconds ago has no snapshot in the trigger dimension's history:
            // their death rewind used to return false here and the protection failed silently,
            // exactly where a portal trap wants it to. Their OLD dimension's recorder still has the
            // snapshot, so the search widens to every recorded level before giving up.
            if (playerSnapshot == null && player != null) {
                UUID playerId = player.getUUID();
                for (ServerLevel level : levels) {
                    YuanTimeRewindRecorder recorder = YuanTimeRewindEvents.recorder(level);
                    if (recorder == null) continue;
                    long fallbackTick = level.getGameTime()
                            - config.rewindWindowSeconds * 20L;
                    YuanTimeRewindRecorder.PlayerSnapshot candidate =
                            findPlayerSnapshot(player,
                                    recorder.history().atOrBefore(fallbackTick));
                    if (candidate != null) {
                        playerSnapshot = candidate;
                        break;
                    }
                }
                if (playerSnapshot == null && death) {
                    // Found nowhere in the whole retained history. Saying so beats failing silently:
                    // a protection that dies quietly is indistinguishable from not having it.
                    if (!isPlayerGone(player)) {
                        player.displayClientMessage(Component.literal(
                                "\u00a7c[回溯] 保留窗口内找不到你的状态快照，保护未生效"), true);
                    }
                }
            }
            if (restores.isEmpty()) return false;
            if (death && playerSnapshot == null) return false;
            if (player != null) {
                if (death) {
                    YuanTimeRewindServerState.setDeathRetry(player.getUUID(), Math.max(0, retry));
                }
                Vec3 target = playerSnapshot == null ? player.position() : snapshotPos(playerSnapshot.nbt());
                YuanTimeRewindServerState.setActiveTargetPosition(
                        target == null ? player.position() : target);
            }

            for (LevelRestoreData data : restores) {
                deductOtherPlayerItems(data.level(), config, player, data.before());
            }
            for (LevelRestoreData data : restores) {
                deductContainerDeltas(data.level(), config, player, data.after());
            }

            if (config.rewindPlaybackMode == 0) {
                restoreProgressive(restores, config, player, death, playerSnapshot,
                        nextPlayerSnapshot, triggerTargetTick);
                // Hand the interpolated entity targets to the clients so they can drive the
                // retreat animation locally (client-driven), independent of any packet/sync
                // channel quirks.
                YuanTimeRewindServerState.setActiveRetreatTargets(collectRetreatTargets());
            } else {
                for (LevelRestoreData data : restores) {
                    restoreInOrder(data.level(), config, player,
                            data.after(), data.before(), data.targetTick());
                }
                if (player != null && !isPlayerGone(player)) {
                    if (config.rewindPlayerState || death) {
                        restorePlayerState(player, playerSnapshot,
                                config.rewindPositionRewind || death);
                    }
                    if (config.rewindPositionRewind || death) {
                        restorePlayerPosition(player, playerSnapshot,
                                nextPlayerSnapshot, triggerTargetTick);
                    }
                }
                showRestoreStats(player, config);
                YuanTimeRewindServerState.setActiveRetreatTargets(Map.of());
            }
            // SERVER-SIDE CEILING, independent of what the sword's config asks for. During
                // progressive playback every entity and block tick in EVERY dimension is frozen, and
                // only the trigger can cancel -- so a configurable one-hour playback is a
                // "stop the server for an hour" switch that a single player holds. The config may
                // ask for less; it cannot ask for more.
                float playbackSeconds = Math.min(
                        config.rewindPlaybackSeconds, MAX_PLAYBACK_SECONDS);
                YuanTimeRewindServerState.setPlaybackUntilMillis(
                        System.currentTimeMillis() + (long) (playbackSeconds * 1000.0F));
            started = true;
            return true;
        } finally {
            if (!started) {
                // restoreProgressive already published the pending playback when something after it
                // threw; leaving it set would keep ServerPlayer/ServerLevel references alive and let
                // the next rewind replay a half-built restore.
                YuanTimeRewindEvents.clearPendingPlayback();
                YuanTimeRewindServerState.endPlayback();
                YuanTimeRewindServerState.release();
            }
        }
    }

    private static List<Object> filterSpatialHistory(ServerLevel level, List<Object> history,
                                                     YuanGodSwordConfig config, ServerPlayer player) {
        if (player == null) return history;
        Vec3 center = radiusCenterFor(level, level.dimension(), player.level().dimension(),
                player.getX(), player.getY(), player.getZ());
        double radius = radiusScaleFor(level.dimension(), player.level().dimension())
                * config.rewindRadius;
        double radiusSqr = radius * radius;
        List<Object> filtered = new ArrayList<>();
        for (Object event : history) {
            if (event instanceof YuanTimeRewindRecorder.BlockChange bc) {
                double x = bc.pos().getX() + 0.5D;
                double y = bc.pos().getY() + 0.5D;
                double z = bc.pos().getZ() + 0.5D;
                if (isWithinRadiusSqr(center.x, center.y, center.z,
                        x, y, z, radiusSqr)) {
                    filtered.add(event);
                }
            } else if (event instanceof YuanTimeRewindRecorder.EntitySnapshot es) {
                if (isWithinRadiusSqr(center.x, center.y, center.z,
                        es.x(), es.y(), es.z(), radiusSqr)) {
                    filtered.add(event);
                }
            } else {
                filtered.add(event);
            }
        }
        return filtered;
    }

    public static Vec3 radiusCenterFor(ResourceKey<Level> from, ResourceKey<Level> to,
                                       double x, double y, double z) {
        double cx = x;
        double cz = z;
        if (from == Level.NETHER && to == Level.OVERWORLD) {
            cx = x / 8.0D;
            cz = z / 8.0D;
        } else if (from == Level.OVERWORLD && to == Level.NETHER) {
            cx = x * 8.0D;
            cz = z * 8.0D;
        }
        return new Vec3(cx, y, cz);
    }

    /**
     * Cross-dimension radius center, refined to the actual portal pair in the
     * destination level when one exists near the naive 8x-scaled position.
     */
    public static Vec3 radiusCenterFor(ServerLevel toLevel, ResourceKey<Level> from, ResourceKey<Level> to,
                                       double x, double y, double z) {
        Vec3 scaled = radiusCenterFor(from, to, x, y, z);
        if (toLevel == null || to == from) return scaled;
        try {
            BlockPos search = new BlockPos((int) Math.floor(scaled.x),
                    (int) Math.floor(scaled.y), (int) Math.floor(scaled.z));
            Optional<BlockUtil.FoundRectangle> portal = new PortalForcer(toLevel)
                    .findPortalAround(search, true, toLevel.getWorldBorder());
            if (portal.isPresent()) {
                BlockPos corner = portal.get().minCorner;
                return new Vec3(corner.getX() + 0.5D, scaled.y, corner.getZ() + 0.5D);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("[YuanTimeRewind] portal lookup failed, using scaled center", e);
        }
        return scaled;
    }

    public static double radiusScaleFor(ResourceKey<Level> from, ResourceKey<Level> to) {
        if (from == Level.NETHER && to == Level.OVERWORLD) return 1.0D / 8.0D;
        if (from == Level.OVERWORLD && to == Level.NETHER) return 8.0D;
        return 1.0D;
    }

    static boolean isWithinRadiusSqr(double playerX, double playerY, double playerZ,
                                     double x, double y, double z, double radiusSqr) {
        double dx = x - playerX;
        double dy = y - playerY;
        double dz = z - playerZ;
        return dx * dx + dy * dy + dz * dz <= radiusSqr;
    }

    /**
     * The history at or before [targetTick], degrading to the whole history when that window is empty.
     *
     * <p>THE ORDER IS THE CONTRACT, and the old fallback broke it. Every reader here treats the list
     * as OLDEST FIRST: `findPlayerSnapshot` keeps overwriting `latest` and so takes the LAST match,
     * `applyEntities`' map overwrite takes the last snapshot per entity, and `findSafeTick` scans
     * forward looking for the first safe tick. The fallback used to reverse its result, so in the one
     * case it fired -- a retry whose target tick predates everything retained, or a rewind right
     * after a server restart -- every reader silently took the OLDEST snapshot instead: player
     * position, entity positions and world state each landed at a different, and wrong, moment, while
     * the blocks were restored correctly. The rewind looked half-applied and nothing explained why.
     *
     * <p>So the fallback no longer reverses. `atOrBefore` already returns oldest first; the degrading
     * path only widens the window, it does not reorder what it found.
     */
    private static List<Object> historyAtOrBeforeFallback(YuanTimeRewindRecorder recorder, long targetTick) {
        List<Object> before = recorder.history().atOrBefore(targetTick);
        if (before.isEmpty() && recorder.history().earliestTick() != Long.MAX_VALUE) {
            before = recorder.history().atOrBefore(Long.MAX_VALUE);
        }
        return before;
    }

    private static long findSafeTick(YuanTimeRewindRecorder recorder, ServerLevel level, long startTick,
                                     YuanGodSwordConfig config, ServerPlayer player) {
        List<YuanTimeRewindRecorder.PlayerSnapshot> snapshots = new ArrayList<>();
        List<Object> before = historyAtOrBeforeFallback(recorder, startTick);
        for (Object e : before) {
            if (e instanceof YuanTimeRewindRecorder.PlayerSnapshot ps
                    && ps.uuid().equals(player.getUUID())) {
                snapshots.add(ps);
            }
        }
        return findSafeTick(snapshots, startTick, level.getMinBuildHeight(), level, config, player);
    }

    private static long findSafeTick(List<YuanTimeRewindRecorder.PlayerSnapshot> snapshots, long startTick,
                                     int minBuildHeight, ServerLevel level,
                                     YuanGodSwordConfig config, ServerPlayer player) {
        if (snapshots.isEmpty()) return Math.max(0L, startTick);

        int scanCount = config.rewindDeathMaxRetries > 0
                ? Math.min(config.rewindDeathMaxRetries, snapshots.size())
                : snapshots.size();
        int firstScannedIndex = snapshots.size() - scanCount;
        for (int i = snapshots.size() - 1; i >= firstScannedIndex; i--) {
            YuanTimeRewindRecorder.PlayerSnapshot snapshot = snapshots.get(i);
            if (isSafe(snapshot.nbt(), minBuildHeight, level, config, player)) {
                return snapshot.tick();
            }
        }
        return snapshots.get(firstScannedIndex).tick();
    }

    public static boolean isSafe(CompoundTag playerTag) {
        if (playerTag == null || !playerTag.contains("Health") || !playerTag.contains("Fire")
                || !playerTag.contains("Air") || !playerTag.contains("foodLevel")) {
            return false;
        }
        if (playerTag.getFloat("Health") < SAFE_HEALTH) return false;
        if (playerTag.getInt("Fire") > 0) return false;
        if (playerTag.getInt("Air") <= 0) return false;
        if (playerTag.getInt("foodLevel") <= SAFE_FOOD_LEVEL) return false;
        Vec3 pos = snapshotPos(playerTag);
        return pos != null && pos.y > MIN_SAFE_Y;
    }

    private static boolean isSafe(CompoundTag playerTag, ServerLevel level,
                                  YuanGodSwordConfig config, ServerPlayer player) {
        return isSafe(playerTag, level.getMinBuildHeight(), level, config, player);
    }

    private static boolean isSafe(CompoundTag playerTag, int minBuildHeight,
                                  ServerLevel level,
                                  YuanGodSwordConfig config, ServerPlayer player) {
        if (!isSafe(playerTag)) return false;
        Vec3 pos = snapshotPos(playerTag);
        if (pos == null || pos.y < minBuildHeight) return false;
        if (isInLava(level, pos)) return false;
        if (!config.rewindHostileCheck) return true;

        AABB area = new AABB(pos.x - HOSTILE_CHECK_RADIUS, pos.y - HOSTILE_CHECK_RADIUS,
                pos.z - HOSTILE_CHECK_RADIUS, pos.x + HOSTILE_CHECK_RADIUS,
                pos.y + HOSTILE_CHECK_RADIUS, pos.z + HOSTILE_CHECK_RADIUS);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, Mob::isAlive)) {
            if (mob.isInvertedHealAndHarm() || mob.getTarget() == player) {
                return false;
            }
        }
        return true;
    }

    /** A death-rewind target standing in (or directly above) lava is not safe. */
    private static boolean isInLava(ServerLevel level, Vec3 pos) {
        if (level == null || pos == null) return false;
        BlockPos feet = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y),
                (int) Math.floor(pos.z));
        return level.getFluidState(feet).is(FluidTags.LAVA)
                || level.getFluidState(feet.below()).is(FluidTags.LAVA);
    }

    private static Vec3 snapshotPos(CompoundTag playerTag) {
        if (playerTag == null) return null;
        ListTag pos = playerTag.getList("Pos", Tag.TAG_DOUBLE);
        if (pos.size() != 3) return null;
        return new Vec3(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
    }

    private static float[] snapshotRotation(CompoundTag playerTag, float yaw, float pitch) {
        if (playerTag == null) return new float[]{yaw, pitch};
        ListTag rotation = playerTag.getList("Rotation", Tag.TAG_FLOAT);
        if (rotation.size() != 2) return new float[]{yaw, pitch};
        return new float[]{rotation.getFloat(0), rotation.getFloat(1)};
    }

    private static void restoreInstant(ServerLevel level, YuanGodSwordConfig config,
                                       ServerPlayer player, List<Object> after,
                                       List<Object> before, long targetTick) {
        restoreInOrder(level, config, player, after, before, targetTick);
    }

    private static void restoreProgressive(List<LevelRestoreData> restores, YuanGodSwordConfig config,
                                           ServerPlayer player, boolean death,
                                           YuanTimeRewindRecorder.PlayerSnapshot playerSnapshot,
                                           YuanTimeRewindRecorder.PlayerSnapshot nextPlayerSnapshot,
                                           long triggerTargetTick) {
        List<YuanTimeRewindEvents.LevelPending> levels = new ArrayList<>();
        for (LevelRestoreData data : restores) {
            levels.add(new YuanTimeRewindEvents.LevelPending(
                    data.level(), config, data.after(), data.before(), data.targetTick(), player));
        }
        YuanTimeRewindEvents.setPendingPlayback(
                new YuanTimeRewindEvents.PendingPlayback(
                        config, player, death, playerSnapshot, levels,
                        nextPlayerSnapshot, triggerTargetTick));
    }

    /**
     * Collects the interpolated entity targets (uuid -> position/rotation) for every level of the
     * pending progressive playback. Sent to clients so they can animate the retreat locally.
     */
    private static Map<ResourceKey<Level>, List<RetreatTarget>> collectRetreatTargets() {
        Map<ResourceKey<Level>, List<RetreatTarget>> out = new HashMap<>();
        YuanTimeRewindEvents.PendingPlayback pending = YuanTimeRewindEvents.pendingPlayback();
        if (pending == null) return out;
        for (YuanTimeRewindEvents.LevelPending lp : pending.levels) {
            List<RetreatTarget> list = new ArrayList<>();
            for (YuanTimeRewindRecorder.EntitySnapshot es : lp.entityTargets.values()) {
                // HARD CAP, and it is not an optimisation. Each target is ~46 bytes and this map is
                // broadcast to the clients of every affected dimension; a mob farm with a few
                // thousand entities would build a packet past the channel's frame limit and DISCONNECT
                // the bystanders who received it. The retreat animation is a flourish -- an entity
                // beyond the cap simply snaps to its restored position instead of gliding there.
                if (list.size() >= MAX_RETREAT_TARGETS_PER_LEVEL) break;
                list.add(new RetreatTarget(es.uuid(), es.x(), es.y(), es.z(), es.yRot(), es.xRot()));
            }
            out.put(lp.level.dimension(), list);
        }
        return out;
    }

    static boolean applyProgressiveBatch(YuanTimeRewindEvents.PendingPlayback pending, boolean allRemaining) {
        boolean allDone = true;
        for (YuanTimeRewindEvents.LevelPending levelPending : pending.levels) {
            int end = allRemaining
                    ? levelPending.blocks.size()
                    : Math.min(levelPending.blockIndex + levelPending.blocksPerTick,
                            levelPending.blocks.size());
            while (levelPending.blockIndex < end) {
                applyBlockChange(levelPending.level, pending.config,
                        (YuanTimeRewindRecorder.BlockChange) levelPending.blocks.get(levelPending.blockIndex++));
            }
            if (levelPending.blockIndex < levelPending.blocks.size()) {
                allDone = false;
            }
            // Entity retreat is driven on the client (see collectRetreatTargets / YuanTimeRewindClient);
            // the server keeps entities frozen during playback and snaps them exactly at finish.
            retreatTime(levelPending.level, pending.config, levelPending.targetDayTime);
        }
        return allRemaining && allDone;
    }

    /** During playback, pull the world clock back toward the exact target dayTime. */
    private static void retreatTime(ServerLevel level, YuanGodSwordConfig config, long targetDayTime) {
        if (!config.rewindTime || targetDayTime == 0L) return;
        long current = level.getDayTime();
        long delta = targetDayTime - current;
        if (delta == 0L) return;
        level.setDayTime(current + (long) (delta * 0.1D));
    }

    static void finishProgressive(YuanTimeRewindEvents.PendingPlayback pending) {
        for (YuanTimeRewindEvents.LevelPending levelPending : pending.levels) {
            // Finalize entity full state (snap to exact target position + NBT).
            for (YuanTimeRewindRecorder.EntitySnapshot es : levelPending.entityTargets.values()) {
                applyEntitySnapshot(levelPending.level, pending.config, es);
            }
            removeCreatedEntities(levelPending.level, pending.config, pending.player,
                    levelPending.before, levelPending.targetTick);
            applyWorldState(levelPending.level, pending.config, levelPending.before,
                    levelPending.targetTick);
            levelPending.level.getChunkSource().getLightEngine()
                    .checkBlock(levelPending.level.getSharedSpawnPos());
        }
        if (pending.player != null && !isPlayerGone(pending.player) && pending.playerSnapshot != null) {
            if (pending.config.rewindPlayerState || pending.death) {
                restorePlayerState(pending.player, pending.playerSnapshot,
                        pending.config.rewindPositionRewind || pending.death);
            }
            if (pending.config.rewindPositionRewind || pending.death) {
                restorePlayerPosition(pending.player, pending.playerSnapshot,
                        pending.nextPlayerSnapshot, pending.targetTick);
            }
        }
        if (pending.player != null && !isPlayerGone(pending.player)
                && pending.sword != null && !pending.sword.isEmpty()
                && YuanTimeRewindEvents.shouldRestoreSword(pending.config, pending.death)) {
            YuanTimeRewindEvents.ensureSwordHeld(pending.player, pending.sword);
        }
        showRestoreStats(pending.player, pending.config);
    }

    static boolean isEntitiesFirst(YuanGodSwordConfig config) {
        return config.rewindRestoreOrder == 1;
    }

    private static void restoreInOrder(ServerLevel level, YuanGodSwordConfig config,
                                       ServerPlayer player, List<Object> after,
                                       List<Object> before, long targetTick) {
        if (isEntitiesFirst(config)) {
            applyEntities(level, config, before, after, targetTick);
            removeCreatedEntities(level, config, player, before, targetTick);
            applyBlockChanges(level, config, after);
        } else {
            applyBlockChanges(level, config, after);
            applyEntities(level, config, before, after, targetTick);
            removeCreatedEntities(level, config, player, before, targetTick);
        }
        applyWorldState(level, config, before, targetTick);
        level.getChunkSource().getLightEngine().checkBlock(level.getSharedSpawnPos());
    }

    static boolean entityCategoryEnabled(YuanGodSwordConfig config, EntityType<?> type) {
        if (type == null) return config.rewindEntities;
        if (type == EntityType.ITEM) return config.rewindItems;
        if (type == EntityType.EXPERIENCE_ORB) return config.rewindExperience;
        return config.rewindEntities;
    }

    private static void applyBlockChanges(ServerLevel level, YuanGodSwordConfig config, List<Object> events) {
        if (!config.rewindBlocks) return;
        for (int i = events.size() - 1; i >= 0; i--) {
            Object e = events.get(i);
            if (e instanceof YuanTimeRewindRecorder.BlockChange bc) {
                applyBlockChange(level, config, bc);
            }
        }
    }

    /**
     * Restores one recorded block, then tells its neighbours.
     *
     * <p>FLAG 2 IS CORRECT FOR THE WRITE, and the neighbours must still be told. Flag 2 means
     * "send this to clients, do not cascade" -- and not cascading is what keeps a full-dimension
     * rewind from re-triggering every neighbour of every restored block, since they are all being
     * restored too. But it leaves the rest of the world not knowing: in a radius-limited or
     * filtered rewind only a subset of positions is touched, so a restored support block never
     * tells the sand above it to fall, a door half never tells the other half, and a rail never
     * reconnects. The rewind would be locally correct and globally broken.
     *
     * <p>So after the write, the restored position flags its own neighbours for a shape update.
     */
    private static void applyBlockChange(ServerLevel level, YuanGodSwordConfig config,
                                         YuanTimeRewindRecorder.BlockChange bc) {
        try {
            restoredBlockCount++;
            level.setBlock(bc.pos(), bc.oldState(), 2);
            // See the method note: flag 2 deliberately does not cascade, so the neighbours have to
            // be told explicitly or a partial rewind leaves the world inconsistent around it.
            level.blockUpdated(bc.pos(), level.getBlockState(bc.pos()).getBlock());
            if (config.rewindBlockEntities && bc.oldBlockEntity() != null) {
                BlockEntity be = level.getBlockEntity(bc.pos());
                if (be != null) {
                    be.load(bc.oldBlockEntity());
                    be.setChanged();
                }
            }
        } catch (RuntimeException e) {
            failedRestoreCount++;
            LOGGER.warn("[YuanTimeRewind] failed to restore block at " + bc.pos(), e);
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static float lerpAngle(float a, float b, float t) {
        float diff = ((b - a + 540.0f) % 360.0f) - 180.0f;
        return a + diff * (float) t;
    }

    /**
     * Estimates the entity's exact state at the target tick by interpolating
     * between the snapshot at/before target and the first snapshot after it.
     * Falls back to the before snapshot when no after snapshot exists.
     */
    static YuanTimeRewindRecorder.EntitySnapshot interpolatedSnapshot(
            YuanTimeRewindRecorder.EntitySnapshot prev, YuanTimeRewindRecorder.EntitySnapshot next,
            long targetTick) {
        if (next == null || next.tick() <= prev.tick()) return prev;
        double t = (double) (targetTick - prev.tick()) / (double) (next.tick() - prev.tick());
        t = Math.max(0.0D, Math.min(1.0D, t));
        return new YuanTimeRewindRecorder.EntitySnapshot(prev.tick(), prev.uuid(), prev.type(),
                lerp(prev.x(), next.x(), t), lerp(prev.y(), next.y(), t), lerp(prev.z(), next.z(), t),
                lerpAngle(prev.yRot(), next.yRot(), (float) t), lerpAngle(prev.xRot(), next.xRot(), (float) t),
                lerp(prev.dx(), next.dx(), t), lerp(prev.dy(), next.dy(), t), lerp(prev.dz(), next.dz(), t),
                (float) lerp(prev.health(), next.health(), t), prev.nbt());
    }

    private static void applyEntities(ServerLevel level, YuanGodSwordConfig config,
                                      List<Object> before, List<Object> after, long targetTick) {
        if (!config.rewindEntities && !config.rewindItems && !config.rewindExperience) return;
        Map<UUID, YuanTimeRewindRecorder.EntitySnapshot> snapshots = new HashMap<>();
        for (Object e : before) {
            if (e instanceof YuanTimeRewindRecorder.EntitySnapshot es) {
                snapshots.put(es.uuid(), es);
            }
        }
        Map<UUID, YuanTimeRewindRecorder.EntitySnapshot> nextSnapshots = new HashMap<>();
        if (after != null) {
            for (Object e : after) {
                if (e instanceof YuanTimeRewindRecorder.EntitySnapshot es) {
                    nextSnapshots.putIfAbsent(es.uuid(), es);
                }
            }
        }
        for (YuanTimeRewindRecorder.EntitySnapshot es : snapshots.values()) {
            applyEntitySnapshot(level, config,
                    interpolatedSnapshot(es, nextSnapshots.get(es.uuid()), targetTick));
        }
    }

    private static void applyEntitySnapshot(ServerLevel level, YuanGodSwordConfig config,
                                            YuanTimeRewindRecorder.EntitySnapshot es) {
        if (!entityCategoryEnabled(config, es.type())) return;
        try {
            restoredEntityCount++;
            Entity entity = level.getEntity(es.uuid());
            if (entity != null) {
                entity.teleportTo(es.x(), es.y(), es.z());
                entity.setYRot(es.yRot());
                entity.setXRot(es.xRot());
                if (entity instanceof LivingEntity le) le.setHealth(es.health());
                // The snapshot NBT carries the raw Pos/Motion/Rotation from its capture tick;
                // strip them so the entity lands exactly on the (interpolated) target instead of
                // snapping back to the raw snapshot position at the end of playback.
                CompoundTag nbt = es.nbt() == null ? null : es.nbt().copy();
                if (nbt != null) {
                    nbt.remove("Pos");
                    nbt.remove("Motion");
                    nbt.remove("Rotation");
                    entity.load(nbt);
                }
                // No explicit packet needed here: the client-driven animation has already placed
                // the entity at this target, and ServerEntity.sendChanges syncs any residue.
            } else if (es.type() != null) {
                Entity resurrected = es.type().create(level);
                if (resurrected != null) {
                    resurrected.load(es.nbt());
                    level.addFreshEntity(resurrected);
                }
            }
        } catch (RuntimeException e) {
            failedRestoreCount++;
            LOGGER.warn("[YuanTimeRewind] failed to restore entity " + es.uuid(), e);
        }
    }

    static boolean hasReliableEntityBaseline(List<Object> before, long targetTick) {
        for (Object e : before) {
            if (e instanceof YuanTimeRewindRecorder.EntitySnapshot es
                    && es.tick() <= targetTick) {
                return true;
            }
        }
        return false;
    }

    private static void removeCreatedEntities(ServerLevel level, YuanGodSwordConfig config,
                                              ServerPlayer player, List<Object> before,
                                              long targetTick) {
        if (!config.rewindEntities && !config.rewindItems && !config.rewindExperience) return;
        if (!hasReliableEntityBaseline(before, targetTick)) return;
        YuanTimeRewindRecorder recorder = YuanTimeRewindEvents.recorder(level);
        // Without a recorder there is no evidence of when anything appeared, and deletion is the
        // irreversible half of a rewind, so skip removal rather than guess.
        if (recorder == null) return;
        boolean radiusMode = config.rewindScope == 1;
        if (radiusMode && player == null) return;
        double radius = radiusMode
                ? radiusScaleFor(level.dimension(), player.level().dimension()) * config.rewindRadius
                : 0.0D;
        double radiusSqr = radius * radius;
        Vec3 center = radiusMode ? radiusCenterFor(level, level.dimension(), player.level().dimension(),
                player.getX(), player.getY(), player.getZ()) : null;
        List<Entity> toDiscard = new ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (entity == null || entity.isRemoved() || entity instanceof Player) continue;
            if (!entityCategoryEnabled(config, entity.getType())) continue;
            if (radiusMode && !isWithinRadiusSqr(center.x, center.y, center.z,
                    entity.getX(), entity.getY(), entity.getZ(), radiusSqr)) {
                continue;
            }
            if (isProvablyCreatedAfter(recorder.entityFirstSeenTick(entity.getUUID()), targetTick)) {
                toDiscard.add(entity);
            }
        }
        for (Entity entity : toDiscard) {
            entity.discard();
        }
    }

    /**
     * An entity may only be discarded when the recorder positively saw it appear after the target
     * tick. Absence from the retained history is not such evidence: entities whose NBT never
     * changes (armor stands, paintings, idle livestock) stop being snapshotted once their single
     * entry ages out of the pruning window, and used to be deleted as "newly created" by that.
     */
    public static boolean isProvablyCreatedAfter(Long firstSeenTick, long targetTick) {
        return firstSeenTick == null || firstSeenTick > targetTick;
    }

    private static void applyWorldState(ServerLevel level, YuanGodSwordConfig config,
                                        List<Object> before, long targetTick) {
        YuanTimeRewindRecorder.WorldSnapshot latest = null;
        for (Object e : before) {
            if (e instanceof YuanTimeRewindRecorder.WorldSnapshot ws) latest = ws;
        }
        if (latest == null) return;
        if (config.rewindTime) {
            // World time advances exactly 1 per tick, so the dayTime at the target
            // tick can be computed exactly from the nearest snapshot.
            long targetTime = latest.dayTime() + Math.max(0L, targetTick - latest.tick());
            level.setDayTime(targetTime);
        }
        if (config.rewindWeather) {
            level.setWeatherParameters(latest.clearWeatherTime(), latest.rainTime(),
                    latest.raining(), latest.thundering());
        }
        if (config.rewindWorldBorder) {
            level.getWorldBorder().setCenter(latest.borderX(), latest.borderZ());
            level.getWorldBorder().setSize(latest.borderSize());
        }
        if (config.rewindScoreboard && latest.scoreboard() != null) {
            new ScoreboardSaveData(level.getScoreboard()).load(latest.scoreboard());
        }
        if (config.rewindRaids && latest.raids() != null) {
            restoreRaids(level, latest.raids());
        }
    }

    private static void restoreRaids(ServerLevel level, CompoundTag tag) {
        try {
            Raids loaded = Raids.load(level, tag);
            Raids current = level.getRaids();
            // Production runtime uses SRG field names (f_...); official names only exist in dev.
            Map<Integer, Raid> loadedMap =
                    ObfuscationReflectionHelper.getPrivateValue(Raids.class, loaded, "f_37951_");
            Map<Integer, Raid> currentMap =
                    ObfuscationReflectionHelper.getPrivateValue(Raids.class, current, "f_37951_");
            if (loadedMap != null && currentMap != null) {
                currentMap.clear();
                currentMap.putAll(loadedMap);
            }
            Integer nextId =
                    ObfuscationReflectionHelper.getPrivateValue(Raids.class, loaded, "f_37954_");
            Integer tick = ObfuscationReflectionHelper.getPrivateValue(Raids.class, loaded, "f_37953_");
            if (nextId != null) {
                ObfuscationReflectionHelper.setPrivateValue(Raids.class, current, nextId, "f_37954_");
            }
            if (tick != null) {
                ObfuscationReflectionHelper.setPrivateValue(Raids.class, current, tick, "f_37953_");
            }
            current.setDirty();
        } catch (RuntimeException e) {
            LOGGER.warn("[YuanTimeRewind] failed to restore raids save data", e);
        }
    }

    private static void deductOtherPlayerItems(ServerLevel level, YuanGodSwordConfig config,
                                               ServerPlayer trigger, List<Object> before) {
        if (!(config.rewindItems && config.rewindOtherItemDeduct)) return;
        Map<UUID, YuanTimeRewindRecorder.EntitySnapshot> items = new HashMap<>();
        for (Object e : before) {
            if (e instanceof YuanTimeRewindRecorder.EntitySnapshot es
                    && es.type() == EntityType.ITEM) {
                items.put(es.uuid(), es);
            }
        }
        for (YuanTimeRewindRecorder.EntitySnapshot es : items.values()) {
            int beforeCount = itemCount(es);
            if (beforeCount <= 0) continue;
            Entity currentEntity = level.getEntity(es.uuid());
            int currentCount = currentEntity instanceof ItemEntity item
                    ? item.getItem().getCount()
                    : 0;
            int delta = Math.max(0, beforeCount - currentCount);
            if (delta == 0) continue;
            ItemStack stack = itemStack(es);
            int remaining = Math.min(stack.getCount(), delta);
            if (remaining <= 0) continue;
            List<ServerPlayer> candidates = config.rewindScopeMode == 0 && level.getServer() != null
                    ? level.getServer().getPlayerList().getPlayers()
                    : level.players();
            for (ServerPlayer other : candidates) {
                // While the triggerer's own inventory is rolled back by restorePlayerState their
                // picks are already covered. If that switch is off, applyEntities still resurrects
                // the item entities they consumed, so the picked-up copies must be deducted here
                // or the same items end up both in the inventory and on the ground.
                if (other == trigger && config.rewindPlayerState) continue;
                remaining -= deductItemFromInventory(other.getInventory(), stack, remaining);
                if (remaining <= 0) break;
            }
        }
    }

    private static void deductContainerDeltas(ServerLevel level, YuanGodSwordConfig config,
                                              ServerPlayer trigger, List<Object> after) {
        if (!(config.rewindBlockEntities && config.rewindOtherItemDeduct)) return;
        Map<BlockPos, YuanTimeRewindRecorder.BlockChange> earliest = new HashMap<>();
        for (Object e : after) {
            if (e instanceof YuanTimeRewindRecorder.BlockChange bc) {
                earliest.putIfAbsent(bc.pos(), bc);
            }
        }
        if (earliest.isEmpty()) return;
        List<ServerPlayer> candidates = config.rewindScopeMode == 0 && level.getServer() != null
                ? level.getServer().getPlayerList().getPlayers()
                : level.players();
        for (YuanTimeRewindRecorder.BlockChange bc : earliest.values()) {
            if (bc.oldBlockEntity() == null) continue;
            BlockEntity be = level.getBlockEntity(bc.pos());
            // A missing block entity means the container was destroyed or its chunk is unloaded, not
            // that it was emptied: treating it as an empty "current" state would report every stored
            // item as a delta and strip it from the other players. applyBlockChange restores the
            // recorded contents, and item drops go through deductOtherPlayerItems.
            if (be == null || !be.getBlockState().equals(bc.oldState())) continue;
            CompoundTag current = be.saveWithFullMetadata();
            for (ItemStack delta : computeContainerDeltas(bc.oldBlockEntity(), current)) {
                int remaining = delta.getCount();
                for (ServerPlayer other : candidates) {
                    if (other == trigger) continue;
                    remaining -= deductItemFromInventory(other.getInventory(), delta, remaining);
                    if (remaining <= 0) break;
                }
            }
        }
    }

    public static List<ItemStack> computeContainerDeltas(CompoundTag target, CompoundTag current) {
        List<ItemStack> targetItems = readContainerItems(target);
        List<ItemStack> currentItems = readContainerItems(current);
        List<Integer> remaining = new ArrayList<>();
        for (ItemStack stack : currentItems) {
            remaining.add(stack.getCount());
        }
        List<ItemStack> deltas = new ArrayList<>();
        for (ItemStack targetStack : targetItems) {
            int wanted = targetStack.getCount();
            int matchedCount = 0;
            for (int i = 0; i < currentItems.size() && matchedCount < wanted; i++) {
                if (remaining.get(i) <= 0) continue;
                if (!ItemStack.isSameItemSameTags(currentItems.get(i), targetStack)) continue;
                int take = Math.min(wanted - matchedCount, remaining.get(i));
                remaining.set(i, remaining.get(i) - take);
                matchedCount += take;
            }
            int delta = wanted - matchedCount;
            if (delta > 0) {
                ItemStack copy = targetStack.copy();
                copy.setCount(delta);
                deltas.add(copy);
            }
        }
        return deltas;
    }

    private static List<ItemStack> readContainerItems(CompoundTag tag) {
        List<ItemStack> out = new ArrayList<>();
        if (tag == null) return out;
        readItemsFromList(tag, "Items", out);
        // Modded containers often keep their inventory in ForgeCaps (item stack handler caps).
        if (tag.contains("ForgeCaps", Tag.TAG_COMPOUND)) {
            CompoundTag caps = tag.getCompound("ForgeCaps");
            for (String capKey : caps.getAllKeys()) {
                Tag capTag = caps.get(capKey);
                if (capTag instanceof CompoundTag capNbt) {
                    readItemsFromList(capNbt, "Items", out);
                }
            }
        }
        return out;
    }

    private static void readItemsFromList(CompoundTag tag, String key, List<ItemStack> out) {
        if (tag == null || !tag.contains(key, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) out.add(stack);
        }
    }

    private static int deductItemFromInventory(Inventory inventory, ItemStack stack, int amount) {
        if (amount <= 0) return 0;
        int removed = ContainerHelper.clearOrCountMatchingItems(inventory,
                candidate -> ItemStack.isSameItemSameTags(candidate, stack),
                amount, false);
        if (removed > 0) {
            inventory.setChanged();
        }
        return removed;
    }

    private static ItemStack itemStack(YuanTimeRewindRecorder.EntitySnapshot es) {
        CompoundTag tag = es.nbt();
        if (tag == null || !tag.contains("Item")) return ItemStack.EMPTY;
        return ItemStack.of(tag.getCompound("Item"));
    }

    private static int itemCount(YuanTimeRewindRecorder.EntitySnapshot es) {
        ItemStack stack = itemStack(es);
        return stack.isEmpty() ? 0 : stack.getCount();
    }

    private static YuanTimeRewindRecorder.PlayerSnapshot findPlayerSnapshot(ServerPlayer player, List<Object> before) {
        if (player == null) return null;
        YuanTimeRewindRecorder.PlayerSnapshot latest = null;
        for (Object e : before) {
            if (e instanceof YuanTimeRewindRecorder.PlayerSnapshot ps
                    && ps.uuid().equals(player.getUUID())) {
                latest = ps;
            }
        }
        return latest;
    }

    private static YuanTimeRewindRecorder.PlayerSnapshot findNextPlayerSnapshot(ServerPlayer player, List<Object> after) {
        if (player == null || after == null) return null;
        YuanTimeRewindRecorder.PlayerSnapshot earliest = null;
        for (Object e : after) {
            if (e instanceof YuanTimeRewindRecorder.PlayerSnapshot ps
                    && ps.uuid().equals(player.getUUID())) {
                if (earliest == null || ps.tick() < earliest.tick()) {
                    earliest = ps;
                }
            }
        }
        return earliest;
    }

    private static boolean isPlayerGone(ServerPlayer player) {
        return player == null || player.isRemoved() || player.connection == null
                || player.getServer() == null;
    }

    private static void restorePlayerState(ServerPlayer player,
                                           YuanTimeRewindRecorder.PlayerSnapshot snapshot,
                                           boolean positionRewind) {
        if (snapshot == null) return;
        YuanTimeRewindPlayerSnapshot.restore(player, snapshot.nbt(), positionRewind);
    }

    private static void restorePlayerPosition(ServerPlayer player,
                                              YuanTimeRewindRecorder.PlayerSnapshot snapshot) {
        restorePlayerPosition(player, snapshot, null, 0L);
    }

    private static void restorePlayerPosition(ServerPlayer player,
                                              YuanTimeRewindRecorder.PlayerSnapshot snapshot,
                                              YuanTimeRewindRecorder.PlayerSnapshot next,
                                              long targetTick) {
        if (snapshot == null) return;
        Vec3 pos = snapshotPos(snapshot.nbt());
        if (pos != null) {
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            float[] rotation = snapshotRotation(snapshot.nbt(), yaw, pitch);
            if (next != null && next.tick() > snapshot.tick()) {
                Vec3 nextPos = snapshotPos(next.nbt());
                float[] nextRotation = snapshotRotation(next.nbt(), yaw, pitch);
                if (nextPos != null) {
                    double t = (double) (targetTick - snapshot.tick())
                            / (double) (next.tick() - snapshot.tick());
                    t = Math.max(0.0D, Math.min(1.0D, t));
                    pos = new Vec3(lerp(pos.x, nextPos.x, t),
                            lerp(pos.y, nextPos.y, t), lerp(pos.z, nextPos.z, t));
                    rotation = new float[]{lerpAngle(rotation[0], nextRotation[0], (float) t),
                            lerpAngle(rotation[1], nextRotation[1], (float) t)};
                }
            }
            // TELEPORT INTO THE SNAPSHOT'S OWN DIMENSION, not the player's current one.
            //
            // A player can walk through a portal during playback -- the default camera mode does not
            // freeze them -- and the restore used to apply the old dimension's coordinates to
            // whatever level they were standing in by the time it finished. In the overworld that
            // multiplied the nether's numbers by eight; the reverse dropped them into bedrock.
            var targetLevel = player.server.getLevel(snapshot.dimension());
            if (targetLevel == null) {
                // The dimension no longer exists (a datapack removed). Restoring the position would
                // be a guess, so only the rotation is applied -- the player keeps wherever they are.
                player.setYRot(rotation[0]);
                player.setXRot(rotation[1]);
            } else {
                player.teleportTo(targetLevel, pos.x, pos.y, pos.z,
                        rotation[0], rotation[1]);
            }
        }
    }
}
