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
import net.minecraft.world.entity.player.Player;
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
        // CANCEL MEANS "SKIP THE ANIMATION", NOT "ABANDON THE RESTORE".
        //
        // The old cancel dropped the pending restore and released the lock, which left three things
        // behind: the trigger of a death rewind stuck at 1 health (their recovery runs in
        // finishProgressive), a queued self-rescue nobody would ever serve, and a world frozen
        // halfway through its own rewind. Finishing the remaining batches now applies the whole
        // rewind in one tick -- no animation, but a CONSISTENT world and a healthy player.
        PendingPlayback pending = pendingPlayback;
        boolean finished = false;
        if (pending != null) {
            try {
                finished = YuanTimeRewindRestorer.applyProgressiveBatch(pending, true);
            } catch (RuntimeException e) {
                LOGGER.warn("[YuanTimeRewind] cancel failed to finish restore", e);
            }
            if (finished) {
                YuanTimeRewindRestorer.finishProgressive(pending);
            }
        }
        clearPendingPlayback();
        YuanTimeRewindServerState.clearPlayback();
        YuanTimeRewindServerState.endPlayback();
        YuanTimeRewindServerState.release();
        serveQueuedRescue();
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

    /**
     * The active player died during their own playback. Playback rewinds TO a snapshot taken before
     * their first death, so re-running it after the current one completes undoes this second death
     * as well -- which is the promise a death-protection feature makes. Cleared once served.
     */
    private static volatile UUID queuedSelfRescue;

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
                serveQueuedRescue();
            }
            return;
        }
        int maxWindowSeconds = cachedEnabledRewindWindowSeconds(event.getServer());
        if (maxWindowSeconds <= 0) return;
        long windowTicks = 20L * maxWindowSeconds;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            YuanTimeRewindRecorder recorder = RECORDERS.get(level);
            if (recorder == null) {
                recorder = new YuanTimeRewindRecorder(level, windowTicks);
                RECORDERS.put(level, recorder);
            } else if (recorder.windowTicks() < windowTicks) {
                // WIDEN IN PLACE, never replace. Rebuilding threw away every recorded tick,
                // the first-seen registry and the container/entity baselines: the first scan
                // after a swap compared against nothing, and the next rewind deleted
                // standing-still entities as newly created. A larger window extends the
                // recording, it does not start a new one.
                recorder.widenWindow(windowTicks);
            }
            recorder.tick(level.getGameTime());
        }
    }

    /**
     * Runs the active player's queued self-rescue once the lock is free.
     *
     * <p>Called only after {@code release()}: restore() takes the lock itself, so invoking it while
     * still rewinding would just fail the CAS. A player reference is deliberately NOT held -- the
     * player may have disconnected during playback -- so the rescue is looked up from the UUID, and
     * a gone player simply drops the queue.
     */
    private static void serveQueuedRescue() {
        UUID queued = queuedSelfRescue;
        if (queued == null) return;
        queuedSelfRescue = null;
        ServerPlayer player = null;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Player found = server.getPlayerList().getPlayer(queued);
        if (!(found instanceof ServerPlayer sp)) return;
        player = sp;
        if (player == null || player.isRemoved()) return;
        ItemStack stack = findSword(player);
        if (stack.isEmpty()) return;
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.read(stack);
        if (!config.rewindDeathEnabled) return;
        YuanGodSwordConfig restoreConfig =
                YuanTimeRewindRestorer.withEffectivePositionRewind(config, true);
        // No one-health pre-load here, deliberately. The guard in YuanTimeRewindCheck forbids writing a
// health value after the restore call: the snapshot already carries the health the player had at
// the target tick, and overwriting it afterwards would replace the rewound value with the live
// one. The one-health trick belongs only to the primary path, where the death event is CANCELLED
// and the player must survive until playback catches up; a queued rescue runs with the lock free,
// so restore() itself owns the health from the first frame.
if (!YuanTimeRewindRestorer.restore(
player.serverLevel(), restoreConfig, player, true, 1)) {
player.displayClientMessage(
Component.literal("\u00a7c[\u56de\u6eaf] \u81ea\u6551\u5931\u8d25\uff0c\u8bf7\u4f7f\u7528\u666e\u901a\u65b9\u5f0f\u590d\u6d3b"), true);
}
    }

    /**
     * Sends the rewind Start packet to the players who can actually see it.
     *
     * <p>NOT to everyone. The packet carries the glide targets of every affected dimension, so
     * broadcasting it to the whole server charged unrelated players for a packet they could not use
     * -- and a dense world made that packet large enough to disconnect them. This sends it to the
     * trigger's dimension plus the dimensions the restore touches, which is the set of players whose
     * screen the effect appears on.
     *
     * <p>The trigger is included explicitly rather than relied on through their dimension: a player
     * standing in a portal during the swap would otherwise miss their own playback start.
     */
    static void sendRewindStart(YuanTimeRewindStartPacket packet) {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        java.util.Set<java.util.UUID> sent = new java.util.HashSet<>();
        for (Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>,
                List<YuanTimeRewindRestorer.RetreatTarget>> e
                : YuanTimeRewindServerState.getActiveRetreatTargets().entrySet()) {
            var level = server.getLevel(e.getKey());
            if (level == null) continue;
            for (ServerPlayer p : level.players()) {
                if (sent.add(p.getUUID())) {
                    Yuan.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
                }
            }
        }
        // The trigger last: their own dimension may not appear in the target map at all (an instant
        // restore publishes no targets), and they must still get the playback.
        ServerPlayer trigger = server.getPlayerList().getPlayer(
                YuanTimeRewindServerState.getActivePlayerUuid());
        if (trigger != null && sent.add(trigger.getUUID())) {
            Yuan.CHANNEL.send(PacketDistributor.PLAYER.with(() -> trigger), packet);
        }
        // Fallback: no targets and no resolvable trigger means the effect still has to appear
        // somewhere, so keep the old behaviour rather than silently dropping the playback.
        if (sent.isEmpty()) {
            Yuan.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
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
            // A BATCH THREW MIDWAY, which leaves the world in the worst state there is: blocks
            // partly reverted, entities not yet snapped, the player's own state not yet restored --
            // AND the items deducted before playback began already gone. Items are taken up front
            // (restore()'s deductOtherPlayerItems) precisely because the rewind may not be able to
            // complete; abandoning the restore here would make that deduction a permanent theft.
            //
            // So the remaining work is attempted ONCE more, all at once. If it lands, the world and
            // the player are whole and the deduction is accounted for. If it also throws, nothing
            // safer is left to try: the state is released and the message says what happened rather
            // than leaving a silent half-rewind on screen.
            LOGGER.warn("[YuanTimeRewind] playback batch failed, attempting to finish", e);
            boolean recovered = false;
            try {
                recovered = YuanTimeRewindRestorer.applyProgressiveBatch(pending, true);
                if (recovered) {
                    YuanTimeRewindRestorer.finishProgressive(pending);
                }
            } catch (RuntimeException second) {
                LOGGER.warn("[YuanTimeRewind] could not finish the interrupted restore", second);
            }
            clearPendingPlayback();
            YuanTimeRewindServerState.clearPlayback();
            YuanTimeRewindServerState.endPlayback();
            YuanTimeRewindServerState.release();
            // Playback state is cleaned either way, so the server keeps running rather than
            // rethrowing; only the message differs.
            if (pending.player != null && !pending.player.isRemoved()) {
                pending.player.displayClientMessage(
                        Component.literal(recovered
                                ? "\u00a7e[\u56de\u6eaf] \u64ad\u653e\u4e2d\u65ad\uff0c\u5df2\u76f4\u63a5\u5b8c\u6210\u8fd8\u539f"
                                : "\u00a7c[\u56de\u6eaf] \u64ad\u653e\u4e2d\u65ad\uff0c\u90e8\u5206\u8fd8\u539f\u65e0\u6cd5\u5b8c\u6210"),
                        true);
            }
            if (recovered) {
                serveQueuedRescue();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // A DEATH DURING ANOTHER PLAYER'S (OR ONE'S OWN) PLAYBACK.
        //
        // The old handler returned silently on isRewinding(), and that silence was the bug: the
        // default camera mode does NOT freeze the trigger, so the trigger of a death rewind --
        // sitting at 1 health for the whole playback -- could be hit again during those seconds and
        // die with no second protection, no message, and the rewind lock still held. Now the ACTIVE
        // player's death re-queues a rewind for themselves once playback ends; everyone else gets an
        // explicit message instead of a silent death, because a protection that fails silently is
        // indistinguishable from not having it.
        if (YuanTimeRewindServerState.isRewinding()) {
            UUID activeId = YuanTimeRewindServerState.getActivePlayerUuid();
            if (player.getUUID().equals(activeId)) {
                queuedSelfRescue = player.getUUID();
                return;
            }
            player.displayClientMessage(
                    Component.literal("\u00a77[回溯] 正在回溯中，死亡保护暂不可用"), true);
            return;
        }
        ItemStack stack = findSword(player);
        if (stack.isEmpty()) return;
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.read(stack);
        if (!config.rewindDeathEnabled) return;
        if (!config.rewindTimestopStacking && YuanTimeStopServerState.isStopped()) return;
        UUID uuid = player.getUUID();
        int retry = YuanTimeRewindServerState.getDeathRetry(uuid);
        if (YuanTimeRewindServerState.cooldownReady(uuid, config.rewindDeathCooldownTicks, YuanTimeRewindServerState.CooldownKind.DEATH)) {
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
            YuanTimeRewindEvents.sendRewindStart(new YuanTimeRewindStartPacket(
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
            YuanTimeRewindServerState.startCooldown(uuid, restoreConfig.rewindDeathCooldownTicks, YuanTimeRewindServerState.CooldownKind.DEATH);
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

    /**
     * Folds a stack into the running maximum, reading its config ONCE.
     *
     * <p>The old pair of helpers read the sword's NBT TWICE for every qualifying stack: once in
     * {@code isEnabledRecordingSword} to ask whether it counts at all, then again here to ask what
     * window it wants. On a hundred-player server that is ~3,800 full NBT deserialisations per
     * second on the main thread, and half of them were asking a question the first read had already
     * answered.
     *
     * <p>The cheap test comes first and returns immediately: {@code isGodSword} is an item
     * comparison, while the deserialisation is the expensive half -- and the overwhelming majority
     * of the 3,800 stacks are not god swords.
     */
    private static int enabledRewindWindowSeconds(int currentMax, ItemStack stack) {
        if (!isGodSword(stack)) return currentMax;
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.read(stack);
        if (!config.rewindEnabled && !config.rewindDeathEnabled) return currentMax;
        return Math.max(currentMax, Math.max(1, Math.min(600, config.rewindWindowSeconds)));
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
