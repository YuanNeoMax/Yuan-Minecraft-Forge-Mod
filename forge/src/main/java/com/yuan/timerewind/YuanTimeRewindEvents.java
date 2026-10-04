package com.yuan.timerewind;

import com.yuan.Yuan;
import com.yuan.item.YuanGodSwordConfig;
import com.yuan.item.YuanGodSwordItem;
import com.yuan.timestop.YuanTimeStopServerState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class YuanTimeRewindEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("YuanTimeRewind");
    private static final Map<ServerLevel, YuanTimeRewindRecorder> RECORDERS = new IdentityHashMap<>();
    private static PendingPlayback pendingPlayback;
    private static int cachedMaxWindowSeconds = 0;
    private static long lastWindowScanTick = -1;
    private static boolean windowCacheValid = false;

    private YuanTimeRewindEvents() {}

    /** Force the next server tick to rescan inventories for enabled rewind swords. */
    public static void invalidateWindowCache() {
        windowCacheValid = false;
    }

    public static YuanTimeRewindRecorder recorder(ServerLevel level) {
        return RECORDERS.get(level);
    }

    static final class PendingPlayback {
        final ServerPlayer player;
        final YuanGodSwordConfig config;
        final boolean death;
        final YuanTimeRewindRecorder.PlayerSnapshot playerSnapshot;
        final YuanTimeRewindRecorder.PlayerSnapshot nextPlayerSnapshot;
        final long targetTick;
        final List<LevelPending> levels;
        ItemStack sword = ItemStack.EMPTY;

        PendingPlayback(YuanGodSwordConfig config, ServerPlayer player, boolean death,
                        YuanTimeRewindRecorder.PlayerSnapshot playerSnapshot,
                        List<LevelPending> levels,
                        YuanTimeRewindRecorder.PlayerSnapshot nextPlayerSnapshot,
                        long targetTick) {
            this.config = config;
            this.player = player;
            this.death = death;
            this.playerSnapshot = playerSnapshot;
            this.levels = levels;
            this.nextPlayerSnapshot = nextPlayerSnapshot;
            this.targetTick = targetTick;
        }
    }

    static final class LevelPending {
        final ServerLevel level;
        final List<Object> before;
        final List<Object> blocks;
        final Map<UUID, YuanTimeRewindRecorder.EntitySnapshot> entityTargets;
        final long targetTick;
        final long targetDayTime;
        int blockIndex;
        int blocksPerTick;

        LevelPending(ServerLevel level, YuanGodSwordConfig config,
                     List<Object> after, List<Object> before, long targetTick, ServerPlayer player) {
            this.level = level;
            this.before = before;
            this.targetTick = targetTick;
            this.blocks = buildBlockList(config, after, player, level);
            this.entityTargets = buildEntityTargets(config, before, after, targetTick);
            this.targetDayTime = computeTargetDayTime(config, before, targetTick);
            int playbackTicks = Math.max(1, Math.round(config.rewindPlaybackSeconds * 20.0f));
            this.blocksPerTick = Math.max(1, (int) Math.ceil(blocks.size() / (double) playbackTicks));
        }

        private static List<Object> buildBlockList(YuanGodSwordConfig config,
                                                   List<Object> after,
                                                   ServerPlayer player, ServerLevel level) {
            List<Object> blockChanges = new ArrayList<>();
            if (config.rewindBlocks) {
                for (int i = after.size() - 1; i >= 0; i--) {
                    Object event = after.get(i);
                    if (event instanceof YuanTimeRewindRecorder.BlockChange bc) {
                        blockChanges.add(bc);
                    }
                }
                if (player != null && !blockChanges.isEmpty()) {
                    Vec3 center = YuanTimeRewindRestorer.radiusCenterFor(level, level.dimension(),
                            player.level().dimension(),
                            player.getX(), player.getY(), player.getZ());
                    blockChanges.sort((o1, o2) -> {
                        YuanTimeRewindRecorder.BlockChange a = (YuanTimeRewindRecorder.BlockChange) o1;
                        YuanTimeRewindRecorder.BlockChange b = (YuanTimeRewindRecorder.BlockChange) o2;
                        return Double.compare(distSqr(center, a.pos()), distSqr(center, b.pos()));
                    });
                }
            }
            return blockChanges;
        }

        private static Map<UUID, YuanTimeRewindRecorder.EntitySnapshot> buildEntityTargets(
                YuanGodSwordConfig config, List<Object> before, List<Object> after, long targetTick) {
            Map<UUID, YuanTimeRewindRecorder.EntitySnapshot> latestEntitySnapshots = new LinkedHashMap<>();
            for (Object event : before) {
                if (event instanceof YuanTimeRewindRecorder.EntitySnapshot es
                        && YuanTimeRewindRestorer.entityCategoryEnabled(config, es.type())) {
                    latestEntitySnapshots.put(es.uuid(), es);
                }
            }
            // First snapshot strictly after the target tick per entity, for position interpolation.
            Map<UUID, YuanTimeRewindRecorder.EntitySnapshot> nextAfter = new HashMap<>();
            for (Object event : after) {
                if (event instanceof YuanTimeRewindRecorder.EntitySnapshot es) {
                    nextAfter.putIfAbsent(es.uuid(), es);
                }
            }
            Map<UUID, YuanTimeRewindRecorder.EntitySnapshot> targets = new LinkedHashMap<>();
            for (YuanTimeRewindRecorder.EntitySnapshot es : latestEntitySnapshots.values()) {
                targets.put(es.uuid(), YuanTimeRewindRestorer.interpolatedSnapshot(
                        es, nextAfter.get(es.uuid()), targetTick));
            }
            return targets;
        }

        private static long computeTargetDayTime(YuanGodSwordConfig config, List<Object> before, long targetTick) {
            if (!config.rewindTime) return 0L;
            YuanTimeRewindRecorder.WorldSnapshot latest = null;
            for (Object e : before) {
                if (e instanceof YuanTimeRewindRecorder.WorldSnapshot ws) latest = ws;
            }
            if (latest == null) return 0L;
            return latest.dayTime() + Math.max(0L, targetTick - latest.tick());
        }

        private static double distSqr(Vec3 center, net.minecraft.core.BlockPos pos) {
            double dx = center.x - (pos.getX() + 0.5D);
            double dy = center.y - (pos.getY() + 0.5D);
            double dz = center.z - (pos.getZ() + 0.5D);
            return dx * dx + dy * dy + dz * dz;
        }
    }

    static void setPendingPlayback(PendingPlayback pending) {
        pendingPlayback = pending;
    }

    static PendingPlayback pendingPlayback() {
        return pendingPlayback;
    }

    static void clearPendingPlayback() {
        pendingPlayback = null;
    }

    /** Ends the ongoing playback immediately: pending restore is dropped, lock released, clients notified. */
    static void cancelPendingPlayback() {
        clearPendingPlayback();
        YuanTimeRewindServerState.clearPlayback();
        YuanTimeRewindServerState.endPlayback();
        YuanTimeRewindServerState.release();
        Yuan.CHANNEL.send(PacketDistributor.ALL.noArg(), new YuanTimeRewindEndPacket(-1));
    }

    static void setPendingSword(ItemStack sword) {
        if (pendingPlayback != null && sword != null && !sword.isEmpty()) {
            pendingPlayback.sword = sword.copy();
        }
    }

    static boolean shouldRestoreSword(YuanGodSwordConfig config, boolean death) {
        return config != null && (config.rewindPlayerState || death);
    }

    static boolean shouldAttemptDeathRewind(int retry, int maxRetries) {
        return maxRetries <= 0 || retry < maxRetries;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (YuanTimeRewindServerState.isRewinding()) {
            processPendingPlayback();
            if (YuanTimeRewindServerState.getPlaybackUntilMillis() > 0L
                    && System.currentTimeMillis() >= YuanTimeRewindServerState.getPlaybackUntilMillis()) {
                Yuan.CHANNEL.send(PacketDistributor.ALL.noArg(), new YuanTimeRewindEndPacket(-1));
                YuanTimeRewindServerState.clearPlayback();
                YuanTimeRewindServerState.endPlayback();
                YuanTimeRewindServerState.release();
            }
            return;
        }
        int maxWindowSeconds = cachedEnabledRewindWindowSeconds(event.getServer());
        if (maxWindowSeconds <= 0) return;
        long windowTicks = 20L * maxWindowSeconds;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            YuanTimeRewindRecorder recorder = RECORDERS.get(level);
            if (recorder == null || recorder.windowTicks() < windowTicks) {
                recorder = new YuanTimeRewindRecorder(level, windowTicks);
                RECORDERS.put(level, recorder);
            }
            recorder.tick(level.getGameTime());
        }
    }

    private static void processPendingPlayback() {
        PendingPlayback pending = pendingPlayback;
        if (pending == null) return;
        boolean allRemaining = System.currentTimeMillis() >= YuanTimeRewindServerState.getPlaybackUntilMillis();
        try {
            if (YuanTimeRewindRestorer.applyProgressiveBatch(pending, allRemaining)) {
                YuanTimeRewindRestorer.finishProgressive(pending);
                clearPendingPlayback();
            }
        } catch (RuntimeException e) {
            clearPendingPlayback();
            YuanTimeRewindServerState.clearPlayback();
            YuanTimeRewindServerState.endPlayback();
            YuanTimeRewindServerState.release();
            // Rethrowing here took the whole server down with the world left half reverted; the
            // playback state is already cleaned, so report it and keep running.
            LOGGER.warn("[YuanTimeRewind] playback failed, partial restore left applied", e);
            if (pending.player != null && !pending.player.isRemoved()) {
                pending.player.displayClientMessage(
                        Component.literal("§c[回溯] 播放中断，已保留部分还原"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (YuanTimeRewindServerState.isRewinding()) return;
        ItemStack stack = findSword(player);
        if (stack.isEmpty()) return;
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.read(stack);
        if (!config.rewindDeathEnabled) return;
        if (!config.rewindTimestopStacking && YuanTimeStopServerState.isStopped()) return;
        UUID uuid = player.getUUID();
        int retry = YuanTimeRewindServerState.getDeathRetry(uuid);
        if (YuanTimeRewindServerState.cooldownReady(uuid, config.rewindDeathCooldownTicks)) {
            YuanTimeRewindServerState.resetDeathRetry(uuid);
            retry = 0;
        } else if (retry == 0) {
            return;
        }
        if (!shouldAttemptDeathRewind(retry, config.rewindDeathMaxRetries)) return;
        YuanGodSwordConfig restoreConfig = YuanTimeRewindRestorer.withEffectivePositionRewind(config, true);
        float deathHealth = player.getHealth();
        player.setHealth(1f);
        boolean ok = YuanTimeRewindRestorer.restore(
                player.serverLevel(), restoreConfig, player, true, retry + 1);
        if (!ok) {
            player.setHealth(deathHealth);
            return;
        }
        if (ok) {
            Yuan.CHANNEL.send(PacketDistributor.ALL.noArg(), new YuanTimeRewindStartPacket(
                    player.getId(),
                    restoreConfig.rewindCameraMode,
                    restoreConfig.rewindFreeCamRestorePosition,
                    restoreConfig.rewindPositionRewind,
                    restoreConfig.rewindPositionMode,
                    restoreConfig.rewindPlaybackSeconds,
                    restoreConfig.rewindFreezeOthers,
                    YuanTimeRewindServerState.getActiveTargetPosition().x,
                    YuanTimeRewindServerState.getActiveTargetPosition().y,
                    YuanTimeRewindServerState.getActiveTargetPosition().z,
                    YuanTimeRewindServerState.getActiveRetreatTargets()));
            YuanTimeRewindServerState.startCooldown(uuid, restoreConfig.rewindDeathCooldownTicks);
            event.setCanceled(true);
            if (restoreConfig.rewindPlaybackMode == 0) {
                setPendingSword(stack.copy());
            } else {
                ensureSwordHeld(player, stack);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RECORDERS.clear();
        pendingPlayback = null;
        cachedMaxWindowSeconds = 0;
        lastWindowScanTick = -1;
        windowCacheValid = false;
        YuanTimeRewindServerState.resetAll();
    }

    private static boolean hasEnabledGodSword(MinecraftServer server) {
        return enabledRewindWindowSeconds(server) > 0;
    }

    private static int cachedEnabledRewindWindowSeconds(MinecraftServer server) {
        long now = server.getTickCount();
        if (windowCacheValid
                && now - lastWindowScanTick < YuanTimeRewindConfig.recordScanIntervalTicks) {
            return cachedMaxWindowSeconds;
        }
        cachedMaxWindowSeconds = enabledRewindWindowSeconds(server);
        lastWindowScanTick = now;
        windowCacheValid = true;
        return cachedMaxWindowSeconds;
    }

    private static int enabledRewindWindowSeconds(MinecraftServer server) {
        int maxWindowSeconds = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                maxWindowSeconds = enabledRewindWindowSeconds(maxWindowSeconds, player.getMainHandItem());
                maxWindowSeconds = enabledRewindWindowSeconds(maxWindowSeconds, player.getOffhandItem());
                for (ItemStack stack : player.getInventory().items) {
                    maxWindowSeconds = enabledRewindWindowSeconds(maxWindowSeconds, stack);
                }
            }
        }
        return maxWindowSeconds;
    }

    private static int enabledRewindWindowSeconds(int currentMax, ItemStack stack) {
        if (!isEnabledRecordingSword(stack)) return currentMax;
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.read(stack);
        return Math.max(currentMax, Math.max(1, Math.min(600, config.rewindWindowSeconds)));
    }

    private static boolean isEnabledRecordingSword(ItemStack stack) {
        if (!isGodSword(stack)) return false;
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.read(stack);
        return config.rewindEnabled || config.rewindDeathEnabled;
    }

    private static ItemStack findSword(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (isEnabledDeathSword(main)) return main;
        ItemStack off = player.getOffhandItem();
        if (isEnabledDeathSword(off)) return off;
        for (ItemStack stack : player.getInventory().items) {
            if (isEnabledDeathSword(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isEnabledDeathSword(ItemStack stack) {
        if (!isGodSword(stack)) return false;
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.read(stack);
        return config.rewindDeathEnabled;
    }

    private static boolean isGodSword(ItemStack stack) {
        return stack.getItem() instanceof YuanGodSwordItem;
    }

    static void ensureSwordHeld(ServerPlayer player, ItemStack sword) {
        if (sword == null || sword.isEmpty()) return;
        Inventory inventory = player.getInventory();
        int selectedSlot = inventory.selected;
        // Already holding a god sword in the main hand (the trigger sword or any god sword):
        // nothing to do, never shuffle slots and never duplicate.
        if (isGodSword(inventory.getItem(selectedSlot))) return;
        ItemStack selectedStack = inventory.getItem(selectedSlot);
        // Match the pending sword exactly first, then fall back to any god sword in the
        // inventory (its NBT may have changed since the snapshot was captured). Never insert
        // a copy: if no god sword is present after the restore, leave the inventory untouched.
        int swordSlot = findInventorySlot(inventory, sword);
        if (swordSlot < 0) {
            swordSlot = findAnyGodSwordSlot(inventory);
        }
        if (swordSlot < 0) return;
        if (swordSlot == selectedSlot) return;
        ItemStack swordInInv = inventory.getItem(swordSlot);
        int offhandSlot = 40;
        // Move the found sword to the selected main-hand slot; the displaced stack goes to the
        // sword's old slot (or first empty slot / offhand). The sword itself never goes to the
        // offhand and no copy is ever created.
        int displacedSlot = findDisplacedStackSlot(inventory, swordSlot, offhandSlot);
        if (displacedSlot < 0) return;
        if (swordSlot >= 0 && displacedSlot != swordSlot) {
            inventory.setItem(swordSlot, ItemStack.EMPTY);
        }
        inventory.setItem(displacedSlot, selectedStack);
        inventory.setItem(selectedSlot, swordInInv);
    }

    private static int findAnyGodSwordSlot(Inventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() instanceof YuanGodSwordItem) return i;
        }
        return -1;
    }

    private static int findDisplacedStackSlot(Inventory inventory, int preferredSlot, int offhandSlot) {
        if (preferredSlot >= 0) return preferredSlot;
        int emptySlot = firstEmptyInventorySlot(inventory);
        if (emptySlot >= 0) return emptySlot;
        if (inventory.getItem(offhandSlot).isEmpty()) return offhandSlot;
        return -1;
    }

    private static int findInventorySlot(Inventory inventory, ItemStack sword) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isSameSword(inventory.getItem(i), sword)) return i;
        }
        return -1;
    }

    private static int firstEmptyInventorySlot(Inventory inventory) {
        for (int i = 0; i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private static boolean isSameSword(ItemStack left, ItemStack right) {
        return ItemStack.isSameItemSameTags(left, right);
    }
}
