package com.yuan.timerewind;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.scores.ScoreboardSaveData;
import net.minecraft.world.level.storage.ServerLevelData;

public final class YuanTimeRewindRecorder {
    /**
     * Extra ticks of history kept beyond the configured rewind window so the
     * "before-target" baseline (entity / world / player snapshots at or before
     * the target tick) is always available. Without this margin the pruning
     * window starts exactly at the target tick, so the baseline is frequently
     * empty and entity/world restore silently falls back to the current state.
     */
    private static final long BASELINE_MARGIN_TICKS = 40L;

    public record BlockChange(long tick, BlockPos pos, BlockState oldState, CompoundTag oldBlockEntity) {}
    public record EntitySnapshot(long tick, UUID uuid, EntityType<?> type, double x, double y, double z,
                                 float yRot, float xRot, double dx, double dy, double dz,
                                 float health, CompoundTag nbt) {}
    public record WorldSnapshot(long tick, long dayTime, boolean raining, int rainTime,
                                boolean thundering, int thunderTime, int clearWeatherTime,
                                double borderX, double borderZ, double borderSize,
                                CompoundTag scoreboard, CompoundTag raids) {}
    public record PlayerSnapshot(long tick, UUID uuid, CompoundTag nbt) {}

    private final ServerLevel level;
    private final long windowTicks;
    private final RewindHistory history;
    private final Map<BlockPos, CompoundTag> lastBlockEntity = new HashMap<>();
    private final Map<UUID, CompoundTag> lastEntityNbt = new HashMap<>();
    /**
     * Earliest tick each entity was seen by this recorder. Snapshots are only written when an
     * entity's NBT changes, so a standing-still cow or an armor stand has a single snapshot that
     * the pruning window quickly drops; without this registry the restorer cannot tell "created
     * during the window" from "idle since before it" and deletes idle entities on every rewind.
     * Entries are never dropped: removing one would make a returning entity look newly born.
     */
    private final Map<UUID, Long> entityFirstSeenTick = new HashMap<>();

    public YuanTimeRewindRecorder(ServerLevel level, long windowTicks) {
        this.level = level;
        this.windowTicks = Math.max(1L, windowTicks);
        this.history = new RewindHistory(this.windowTicks + BASELINE_MARGIN_TICKS);
    }

    public RewindHistory history() {
        return history;
    }

    public long windowTicks() {
        return windowTicks;
    }

    public Long entityFirstSeenTick(UUID uuid) {
        return entityFirstSeenTick.get(uuid);
    }

    public void recordBlockChange(long tick, BlockPos pos, BlockState oldState, CompoundTag oldBlockEntity) {
        history.add(tick, new BlockChange(tick, pos.immutable(), oldState, oldBlockEntity));
        lastBlockEntity.remove(pos.immutable());
    }

    public void tick(long tick) {
        int entityInterval = Math.max(1, YuanTimeRewindConfig.entitySnapshotIntervalTicks);
        if (tick % entityInterval == 0) {
            snapshotEntities(tick);
            snapshotPlayers(tick);
        }
        int worldInterval = Math.max(1, YuanTimeRewindConfig.worldSnapshotIntervalTicks);
        if (tick % worldInterval == 0) snapshotWorld(tick);
        int containerInterval = Math.max(1, YuanTimeRewindConfig.containerSnapshotIntervalTicks);
        if (tick % containerInterval == 0) snapshotBlockEntities(tick, true);
        if (tick % 20 == 0) snapshotBlockEntities(tick, false);
    }

    private void snapshotEntities(long tick) {
        Set<UUID> seen = new HashSet<>();
        for (Entity e : level.getEntities().getAll()) {
            if (e == null || e instanceof net.minecraft.world.entity.player.Player) continue;
            CompoundTag tag = new CompoundTag();
            e.saveWithoutId(tag);
            UUID uuid = e.getUUID();
            seen.add(uuid);
            entityFirstSeenTick.putIfAbsent(uuid, tick);
            CompoundTag previous = lastEntityNbt.get(uuid);
            if (previous != null && previous.equals(tag)) {
                continue;
            }
            lastEntityNbt.put(uuid, tag);
            float health = e instanceof LivingEntity le ? le.getHealth() : 1.0f;
            history.add(tick, new EntitySnapshot(tick, uuid, e.getType(),
                    e.getX(), e.getY(), e.getZ(), e.getYRot(), e.getXRot(),
                    e.getDeltaMovement().x, e.getDeltaMovement().y, e.getDeltaMovement().z,
                    health, tag));
        }
        lastEntityNbt.keySet().retainAll(seen);
    }

    private void snapshotPlayers(long tick) {
        for (ServerPlayer p : level.players()) {
            CompoundTag tag = new CompoundTag();
            p.saveWithoutId(tag);
            history.add(tick, new PlayerSnapshot(tick, p.getUUID(), tag));
        }
    }

    private void snapshotBlockEntities(long tick, boolean containersOnly) {
        for (net.minecraft.server.level.ChunkHolder holder : level.getChunkSource().chunkMap.getChunks()) {
            LevelChunk chunk = holder.getFullChunk();
            if (chunk == null) continue;
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if ((be instanceof net.minecraft.world.Container) != containersOnly) {
                    continue;
                }
                CompoundTag current = be.saveWithFullMetadata();
                CompoundTag previous = lastBlockEntity.get(be.getBlockPos());
                if (previous == null) {
                    lastBlockEntity.put(be.getBlockPos(), current);
                } else if (!previous.equals(current)) {
                    history.add(tick, new BlockChange(tick, be.getBlockPos(), be.getBlockState(), previous));
                    lastBlockEntity.put(be.getBlockPos(), current);
                }
            }
        }
    }

    private void snapshotWorld(long tick) {
        ServerLevelData levelData = (ServerLevelData) level.getLevelData();
        CompoundTag scoreboard = new ScoreboardSaveData(level.getScoreboard()).save(new CompoundTag());
        CompoundTag raids = level.getRaids().save(new CompoundTag());
        history.add(tick, new WorldSnapshot(tick,
                level.getDayTime(),
                level.isRaining(), levelData.getRainTime(),
                level.isThundering(), levelData.getThunderTime(), levelData.getClearWeatherTime(),
                level.getWorldBorder().getCenterX(), level.getWorldBorder().getCenterZ(),
                level.getWorldBorder().getSize(), scoreboard, raids));
    }

    public List<Object> since(long targetTick) {
        return history.since(targetTick);
    }
}
