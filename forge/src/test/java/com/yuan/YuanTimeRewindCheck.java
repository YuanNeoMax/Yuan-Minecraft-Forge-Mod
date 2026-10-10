package com.yuan;

import com.yuan.item.YuanGodSwordConfig;
import com.yuan.timerewind.YuanTimeRewindClient;
import com.yuan.timerewind.YuanTimeRewindEvents;
import com.yuan.timerewind.YuanTimeRewindRecorder;
import com.yuan.timerewind.YuanTimeRewindPlayerSnapshot;
import com.yuan.timerewind.YuanTimeRewindRestorer;
import com.yuan.timerewind.YuanTimeRewindServerState;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class YuanTimeRewindCheck {
    public static void main(String[] args) throws Exception {
        allowRegistryInitialization();
        String yuan = Files.readString(Path.of("src/main/java/com/yuan/Yuan.java"));
        assert yuan.contains("registerMessage(12") : "rewind request packet id 12 must be registered";
        assert yuan.contains("registerMessage(13") : "rewind start packet id 13 must be registered";
        assert yuan.contains("registerMessage(14") : "rewind end packet id 14 must be registered";
        assert yuan.contains("registerMessage(15") : "rewind cancel packet id 15 must be registered";

        String keyBindings = Files.readString(Path.of("src/main/java/com/yuan/client/YuanKeyBindings.java"));
        assert keyBindings.contains("TRIGGER_REWIND") : "rewind hotkey must be defined";
        assert keyBindings.contains("new YuanTimeRewindCancelPacket()")
                : "rewind hotkey must cancel an active playback";

        String composeScreen = ComposePackage.read();
        assert composeScreen.contains("V9RewindPanel") : "rewind compose tab must define V9RewindPanel";

        String mixins = Files.readString(Path.of("src/main/resources/yuan.mixins.json"));
        assert mixins.contains("TimerewindServerLevelMixin") : "rewind server level mixin must be registered";
        assert mixins.contains("TimerewindCameraMixin") : "rewind camera mixin must be registered";
        assert !mixins.contains("RewindRetreatMoveMixin") : "obsolete retreat move mixin must be removed";
        String chunkCacheMixin = Files.readString(Path.of(
                "src/main/java/com/yuan/mixin/TimestopServerChunkCacheMixin.java"));
        assert chunkCacheMixin.contains("if (YuanTimeStopServerState.isStopped())")
                && !chunkCacheMixin.contains("isStopped() || isRewinding()")
                : "chunk-cache tick must stay live during rewind playback so block updates flow";
        String cameraMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimerewindCameraMixin.java"));
        assert cameraMixin.contains("@Shadow(remap = false) private Vec3 f_90552_;")
                : "rewind camera fields must use SRG names with remap=false";
        assert !cameraMixin.contains("@Shadow private Vec3 position")
                : "rewind camera must not rely on refmap field remapping";
        assert !cameraMixin.contains("= null;")
                : "rewind camera @Shadow fields must not carry initializers";

        String zhCn = Files.readString(Path.of("src/main/resources/assets/yuan/lang/zh_cn.json"));
        assert zhCn.contains("key.yuan.rewind_trigger") : "zh_cn rewind hotkey translation missing";

        String rewindEvents = Files.readString(Path.of("src/main/java/com/yuan/timerewind/YuanTimeRewindEvents.java"));
        assert rewindEvents.contains("LivingDeathEvent") : "death rewind must use LivingDeathEvent";
        assert rewindEvents.contains("event.setCanceled(true)") : "death rewind must cancel the death event";
        assert rewindEvents.contains("YuanTimeRewindRestorer.restore") : "death rewind must call YuanTimeRewindRestorer.restore";
        assert rewindEvents.contains("if (isEnabledDeathSword(stack)) return stack;")
                : "death rewind must work with any enabled sword on the body (hand or inventory)";
        assert !rewindEvents.contains("rewindDeathRequireHeld")
                : "death rewind must not require the found sword to be held";
        assert rewindEvents.contains("new YuanTimeRewindStartPacket") : "death rewind must broadcast start";
        assert rewindEvents.contains("withEffectivePositionRewind") : "death rewind must use effective position rewind config";
        assert rewindEvents.contains("new YuanTimeRewindEndPacket(-1)") : "server tick must send delayed end packet";
        assert rewindEvents.contains("YuanTimeRewindServerState.clearPlayback()") : "server tick must clear playback state";
        assert rewindEvents.contains("PendingPlayback") : "progressive playback pending holder missing";
        assert rewindEvents.contains("applyProgressiveBatch") : "progressive playback batch apply missing";
        assert rewindEvents.contains("ServerStoppedEvent") : "recorder lifecycle must clear on server stop";
        assert rewindEvents.contains("hasEnabledGodSword") : "recorder lifecycle must require an enabled sword";
        assert rewindEvents.contains("cachedEnabledRewindWindowSeconds(event.getServer())")
                : "recorder window must use the configured rewind window via the cache";
        assert rewindEvents.contains("recorder.windowTicks() < windowTicks")
                : "recorder window must be resized when a larger configured window appears";
        assert !rewindEvents.contains("20L * 60L") : "recorder window must not be hardcoded to 60 seconds";
        assert rewindEvents.contains("cooldownReady(uuid") : "death cooldown must be per player UUID";
        assert rewindEvents.indexOf("YuanTimeRewindRestorer.restore")
                < rewindEvents.indexOf("event.setCanceled(true)")
                : "death must only be canceled after restore succeeds";
        assert rewindEvents.contains("shouldAttemptDeathRewind") : "death retry cap helper missing";
        assert rewindEvents.contains("shouldAttemptDeathRewind(retry")
                : "death retry cap must be enforced";
        assert rewindEvents.contains("float deathHealth = player.getHealth()")
                : "death restore must preserve health for failure rollback";
        assert !rewindEvents.substring(rewindEvents.indexOf("YuanTimeRewindRestorer.restore"))
                .contains("player.setHealth(1f)")
                : "death restore must not set health after snapshot restore";
        assert rewindEvents.contains("setPendingSword(stack.copy())")
                : "progressive death playback must retain the sword stack";
        assert !rewindEvents.contains("removeDuplicateGodSwords")
                : "sword exemption must not bulk-remove god swords";
        assert rewindEvents.contains("isGodSword(inventory.getItem(selectedSlot))")
                : "sword exemption must check whether the main hand already holds a god sword";
        assert rewindEvents.contains("findDisplacedStackSlot")
                : "sword exemption must preserve the displaced selected stack";
        assert rewindEvents.contains("inventory.setItem(displacedSlot, selectedStack)")
                : "sword exemption must move the displaced stack to the original, empty, or offhand slot";
        String ensureSwordHeldBlock = rewindEvents.substring(
                rewindEvents.indexOf("static void ensureSwordHeld"),
                rewindEvents.indexOf("private static int findInventorySlot"));
        assert ensureSwordHeldBlock.indexOf("isGodSword(inventory.getItem(selectedSlot))") >= 0
                : "sword exemption must keep the main-hand slot untouched when it already holds a god sword";
        assert ensureSwordHeldBlock.indexOf("if (swordSlot == selectedSlot) return;") >= 0
                : "sword exemption must not reshuffle when the found sword is already in the main hand";
        assert ensureSwordHeldBlock.indexOf("inventory.setItem(selectedSlot, swordInInv)") >= 0
                : "sword exemption must place the found inventory sword (never a copy) into the selected main-hand slot";
        assert ensureSwordHeldBlock.contains("if (swordSlot < 0) return;")
                : "sword exemption must never insert a copy when no god sword is in the inventory";
        assert ensureSwordHeldBlock.contains("findAnyGodSwordSlot(inventory)")
                : "sword exemption must fall back to any god sword when exact NBT does not match";
        assert !ensureSwordHeldBlock.contains("setItem(offhandSlot, sword")
                : "sword exemption must never place the sword itself into the offhand";
        assert rewindEvents.contains("private static int findAnyGodSwordSlot")
                : "god-sword-by-type slot search helper missing";
        assert rewindEvents.contains("shouldRestoreSword")
                : "sword cleanup must be conditional on player state restore";
        assert rewindEvents.contains("config.rewindPlayerState || death")
                : "sword cleanup must run for player-state restore or death only";

        String rewindRequest = Files.readString(Path.of("src/main/java/com/yuan/timerewind/YuanTimeRewindRequestPacket.java"));
        assert rewindRequest.contains("new YuanTimeRewindStartPacket") : "rewind request must broadcast start";
        assert !rewindRequest.contains("new YuanTimeRewindEndPacket") : "rewind request must not send end immediately";
        assert rewindRequest.contains("withEffectivePositionRewind") : "rewind request must use effective position rewind config";
        assert rewindRequest.contains("rewindFreezeOthers") : "rewind request must carry freezeOthers";
        assert rewindRequest.contains("cooldownReady(player.getUUID()") : "active cooldown must be per player UUID";
        assert rewindRequest.contains("setPendingSword(stack.copy())")
                : "progressive active rewind must retain the sword exemption stack";
        assert rewindRequest.contains("ensureSwordHeld(player, stack)")
                : "instant active rewind must reapply the sword exemption";
        assert rewindRequest.contains("shouldRestoreSword(restoreConfig, false)")
                : "active sword exemption must be gated by player state restore";
        assert rewindRequest.indexOf("YuanTimeRewindRestorer.restore")
                < rewindRequest.indexOf("setPendingSword(stack.copy())")
                : "active sword exemption must only run after a successful restore";
        assert rewindRequest.indexOf("shouldRestoreSword(restoreConfig, false)")
                < rewindRequest.indexOf("setPendingSword(stack.copy())")
                : "active sword exemption must be gated before the progressive sword is retained";
        assert rewindRequest.indexOf("shouldRestoreSword(restoreConfig, false)")
                < rewindRequest.indexOf("ensureSwordHeld(player, stack)")
                : "active sword exemption must be gated before the instant sword is placed";
        assert rewindRequest.contains("isEnabledRewindSword")
                : "active sword selection must validate enabled swords";
        assert rewindRequest.contains("for (ItemStack stack : player.getInventory().items) {\n            if (isEnabledRewindSword(stack)) return stack;")
                : "any enabled sword in the inventory must trigger rewind";
        assert !rewindRequest.contains("rewindRequireHeld")
                : "active rewind must not require the found sword to be held";

        String rewindClient = Files.readString(Path.of("src/main/java/com/yuan/timerewind/YuanTimeRewindClient.java"));
        assert rewindClient.contains("Math.pow(0.6D * sensitivity + 0.2D, 3.0D) * 8.0D")
                : "B-camera mouse sensitivity formula must match vanilla MouseHandler";
        assert rewindClient.contains("mc.options.invertYMouse().get()")
                : "B-camera vertical mouse must honor invertYMouse";
        assert rewindClient.contains("verticalDelta = -verticalDelta")
                : "B-camera invertYMouse must negate vertical delta";
        assert rewindClient.contains("activePlayerId != mc.player.getId()")
                : "B-camera flight must be restricted to the trigger player";
        assert rewindClient.contains("shouldFreezeEntity") : "client entity freeze must be configurable per entity";
        assert rewindClient.contains("interpolationStart.lerp(targetPosition, progress)")
                : "smooth position return interpolation missing";
        assert rewindClient.contains("animateRetreat")
                : "client-driven retreat animation must exist";
        assert rewindClient.contains("retreatStartPos")
                : "client retreat animation must track per-entity start positions";
        assert rewindClient.contains("e.setPos(nx, ny, nz)")
                : "client retreat animation must move entities directly";
        assert rewindClient.indexOf("if (isDone())") < 0
                : "client auto-end must not fire before the server end packet";
        assert rewindClient.contains("isFreeCameraEnabled(cameraMode, playerId, localPlayerId)")
                : "free camera must be restricted to the trigger player";
        assert rewindClient.contains("triggerPlayer && positionRewind && positionMode == 1")
                : "smooth position return must be restricted to the trigger player";

        String playerSnapshot = Files.readString(Path.of(
                "src/main/java/com/yuan/timerewind/YuanTimeRewindPlayerSnapshot.java"));
        assert playerSnapshot.contains("effectiveRestoreTag")
                : "player snapshot restore helper missing";

        String rewindServerState = Files.readString(Path.of("src/main/java/com/yuan/timerewind/YuanTimeRewindServerState.java"));
        assert rewindServerState.contains("setPlaybackUntilMillis") : "server playback deadline setter missing";
        assert rewindServerState.contains("getPlaybackUntilMillis") : "server playback deadline getter missing";
        assert rewindServerState.contains("isPlaybackActive") : "server playback active check missing";
        assert rewindServerState.contains("clearPlayback") : "server playback clear missing";
        assert rewindServerState.contains("Map<UUID, Long> COOLDOWNS") : "cooldown state must be per player UUID";
        assert rewindServerState.contains("Map<UUID, Integer> DEATH_RETRIES") : "death retry state must be per player UUID";
        assert rewindServerState.contains("setActiveRetreatTargets")
                && rewindServerState.contains("getActiveRetreatTargets")
                : "server must expose the client-driven retreat targets";

        String rewindStartPacket = Files.readString(Path.of(
                "src/main/java/com/yuan/timerewind/YuanTimeRewindStartPacket.java"));
        assert rewindStartPacket.contains("retreatTargets")
                : "start packet must carry the retreat targets";
        assert rewindStartPacket.contains("buffer.writeResourceLocation(entry.getKey().location())")
                : "start packet must encode retreat targets per dimension";
        assert rewindStartPacket.contains("readResourceLocation()")
                : "start packet must decode retreat targets per dimension";

        String rewindRestorer = Files.readString(Path.of("src/main/java/com/yuan/timerewind/YuanTimeRewindRestorer.java"));
        assert rewindRestorer.contains("setPlaybackUntilMillis") : "restorer must start playback timing";
        assert rewindRestorer.contains("effectivePositionRewind") : "effective position rewind helper missing";
        assert rewindRestorer.contains("withEffectivePositionRewind") : "effective position rewind config copy missing";
        assert rewindRestorer.contains("teleportTo(player.serverLevel()") : "restorer must use mapped ServerPlayer teleport";
        assert rewindRestorer.contains("applyProgressiveBatch") : "restorer progressive batch apply missing";
        assert rewindRestorer.contains("finishProgressive") : "restorer progressive finish missing";
        assert rewindRestorer.contains("collectRetreatTargets")
                : "restorer must collect client-driven retreat targets";
        assert rewindRestorer.contains("record RetreatTarget")
                : "restorer retreat target record missing";
        assert !rewindRestorer.contains("retreatEntities")
                : "server-side per-tick retreat movement must be removed (client drives it)";
        assert !rewindRestorer.contains("TRACKING_ENTITY.with")
                : "server must not broadcast per-tick retreat teleport packets anymore";
        assert rewindRestorer.contains("restorePlayerState") : "full player-state restore helper missing";
        assert rewindRestorer.contains("restorePlayerPosition") : "position-only player restore helper missing";
        assert rewindRestorer.contains("if (config.rewindPlayerState || death)")
                : "instant player-state restore must be conditional";
        assert rewindRestorer.contains("if (config.rewindPositionRewind || death)")
                : "instant position-only restore must be conditional";
        assert rewindRestorer.contains("if (pending.config.rewindPlayerState || pending.death)")
                : "progressive player-state restore must be conditional";
        assert rewindRestorer.contains("if (pending.config.rewindPositionRewind || pending.death)")
                : "progressive position-only restore must be conditional";
        assert rewindRestorer.contains("removeCreatedEntities") : "created entity removal missing";
        assert rewindRestorer.contains("getAllLevels") : "multi-level restore missing";
        assert rewindRestorer.contains("Raids.load") : "raid save data restore missing";
        assert rewindRestorer.contains("deductOtherPlayerItems") : "multiplayer item deduction missing";
        assert rewindRestorer.contains("beforeCount - currentCount") : "item deduction must use stack count delta";
        assert rewindRestorer.contains("deductOtherPlayerItems(data.level(), config, player, data.before())")
                : "item deduction must run before entity restoration in both playback paths";
        assert rewindRestorer.indexOf("deductOtherPlayerItems(data.level()")
                < rewindRestorer.indexOf("if (config.rewindPlaybackMode == 0)")
                : "item deduction must be wired before playback branch selection";
        String deductOtherPlayersBlock = rewindRestorer.substring(
                rewindRestorer.indexOf("private static void deductOtherPlayerItems"),
                rewindRestorer.indexOf("private static int deductItemFromInventory"));
        assert deductOtherPlayersBlock.contains("level.getServer().getPlayerList().getPlayers()")
                : "all-dimension item deduction must scan all online players";
        assert deductOtherPlayersBlock.contains("config.rewindScopeMode == 0")
                : "all-dimension item deduction must branch on rewindScopeMode";
        assert deductOtherPlayersBlock.contains("other == trigger")
                : "all-dimension item deduction must skip the trigger player";
        String finishProgressiveBlock = rewindRestorer.substring(
                rewindRestorer.indexOf("static void finishProgressive"),
                rewindRestorer.indexOf("static boolean isEntitiesFirst"));
        assert !finishProgressiveBlock.contains("deductOtherPlayerItems")
                : "progressive item deduction must not run after entity restoration";
        assert rewindRestorer.contains("hasReliableEntityBaseline")
                : "entity removal must require a reliable baseline";
        assert rewindRestorer.contains("isWithinRadiusSqr")
                : "entity removal must use radius scoping";
        assert rewindRestorer.contains("radiusCenterFor(level, level.dimension()")
                : "radius entity removal must use the cross-dimension radius center";
        assert !rewindRestorer.contains("level != player.serverLevel()")
                : "radius entity removal must not skip non-trigger dimensions";
        assert rewindRestorer.contains("pending.sword")
                : "progressive playback must retain the sword exemption stack";
        assert rewindRestorer.contains("ensureSwordHeld(pending.player, pending.sword)")
                : "progressive playback must reapply sword exemption after player restore";
        assert rewindRestorer.contains("shouldRestoreSword(pending.config, pending.death)")
                : "progressive sword exemption must be gated by player state restore or death";
        assert rewindRestorer.indexOf("shouldRestoreSword(pending.config, pending.death)")
                < rewindRestorer.indexOf("ensureSwordHeld(pending.player, pending.sword)")
                : "progressive sword exemption gate must precede the reapply call";

        String serverLevelMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimerewindServerLevelMixin.java"));
        assert serverLevelMixin.contains("old == state") : "no-op setBlock recording must be skipped";

        assert yuan.contains("YuanTimeRewindConfig")
                : "rewind tuning config must be registered in Yuan";
        assert yuan.contains("registerConfig(ModConfig.Type.COMMON, YuanTimeRewindConfig.SPEC")
                : "rewind tuning config must be a COMMON config";
        assert yuan.contains("\"yuan-rewind-common.toml\"")
                : "rewind COMMON config must use a distinct file name to avoid config conflict";
        String rewindConfig = Files.readString(Path.of("src/main/java/com/yuan/timerewind/YuanTimeRewindConfig.java"));
        assert rewindConfig.contains("recordScanIntervalTicks") : "rewind config must expose record scan interval";
        assert rewindConfig.contains("containerSnapshotIntervalTicks") : "rewind config must expose container snapshot interval";
        assert rewindEvents.contains("cachedEnabledRewindWindowSeconds")
                : "record window scan must be cached";
        assert rewindEvents.contains("YuanTimeRewindConfig.recordScanIntervalTicks")
                : "record window scan interval must be configurable";
        assert rewindEvents.contains("invalidateWindowCache")
                : "record window cache must be invalidatable";
        String configPacket = Files.readString(Path.of("src/main/java/com/yuan/network/YuanGodSwordConfigPacket.java"));
        assert configPacket.contains("invalidateWindowCache")
                : "sword config save must invalidate the record window cache";
        assert configPacket.contains("instanceof YuanGodSwordItem")
                : "the config packet must reject a main hand that is not the god sword";
        assert !configPacket.contains("put(YuanGodSwordConfig.TAG, message.tag.copy())")
                : "the config packet must not stamp the raw client tag onto the stack";
        assert configPacket.contains("config.read(probe)") && configPacket.contains("config.write(stack)")
                : "the config packet must normalize client values through the config round-trip";
        assert configPacket.contains("copyPresets(message.tag, stack)")
                : "the config packet must carry preset slots to the server side stack";
        assert configPacket.contains("i < MAX_PRESETS")
                : "the preset list must be bounded before it reaches the stack";
        String recorderSrc = Files.readString(Path.of("src/main/java/com/yuan/timerewind/YuanTimeRewindRecorder.java"));
        assert recorderSrc.contains("YuanTimeRewindConfig.containerSnapshotIntervalTicks")
                : "container snapshot interval must be configurable";
        assert recorderSrc.contains("instanceof net.minecraft.world.Container")
                : "container block entities must be snapshotted at finer granularity";
        assert recorderSrc.contains("BASELINE_MARGIN_TICKS")
                : "recorder must retain a baseline margin beyond the rewind window";
        assert recorderSrc.contains("new RewindHistory(this.windowTicks + BASELINE_MARGIN_TICKS)")
                : "recorder history must include the baseline margin";
        assert rewindRestorer.contains("be.setChanged()")
                : "restored block entities must be marked changed for client sync";
        assert rewindClient.contains("mc.player.setPos(targetPosition.x, targetPosition.y, targetPosition.z)")
                : "client playback end must snap the visual position to the target";
        assert rewindClient.contains("mc.options.keyLeft.isDown() ? 1.0D : 0.0D)\n"
                + "                - (mc.options.keyRight.isDown() ? 1.0D : 0.0D)")
                : "free camera strafe must match vanilla xxa sign (left positive)";
        assert rewindClient.contains("onRenderTick") && rewindClient.contains("TickEvent.RenderTickEvent")
                : "free camera flight must run per frame for smooth movement";
        assert rewindClient.contains("frameScale")
                : "free camera flight must scale by frame time";
        assert rewindClient.contains("if (trigger) return freeCamera;")
                : "free camera must freeze the trigger player's body";
        String rewindServerStateSrc = Files.readString(Path.of("src/main/java/com/yuan/timerewind/YuanTimeRewindServerState.java"));
        assert rewindServerStateSrc.contains("startPlayback(UUID playerUuid, boolean freezeOtherPlayers, int cameraMode)")
                : "server playback must track the camera mode";
        assert rewindServerStateSrc.contains("activeCameraMode")
                : "server playback must store the camera mode";
        assert rewindRestorer.contains("startPlayback(player.getUUID(),\n                    config.rewindFreezeOthers, config.rewindCameraMode)")
                : "restore must pass the camera mode to server playback";
        assert rewindRestorer.contains("entity == null")
                : "created-entity removal must guard against null entities";
        assert rewindRestorer.contains("toDiscard")
                : "created-entity removal must collect before discarding";
        assert rewindRestorer.contains("\"f_37951_\"") && rewindRestorer.contains("\"f_37954_\"")
                && rewindRestorer.contains("\"f_37953_\"")
                : "raid restore must use SRG field names";
        assert !rewindRestorer.contains("\"raidMap\"")
                : "raid restore must not use official field names at runtime";
        assert rewindRestorer.contains("isInLava") && rewindRestorer.contains("FluidTags.LAVA")
                : "death rewind safety must avoid lava";
        assert rewindRestorer.contains("showRestoreStats") && rewindRestorer.contains("resetRestoreStats")
                && rewindRestorer.contains("restoredBlockCount")
                : "rewind must track and report restored stats";
        assert rewindRestorer.contains("findPortalAround") && rewindRestorer.contains("PortalForcer")
                && rewindRestorer.contains("radiusCenterFor(ServerLevel toLevel")
                : "cross-dimension radius center must refine to the portal pair";
        assert rewindRestorer.contains("readItemsFromList") && rewindRestorer.contains("ForgeCaps")
                : "container delta tracking must read ForgeCaps inventories";
        assert rewindConfig.contains("entitySnapshotIntervalTicks")
                : "rewind config must expose the entity snapshot interval";
        assert recorderSrc.contains("entitySnapshotIntervalTicks")
                && recorderSrc.contains("lastEntityNbt")
                && recorderSrc.contains("previous.equals(tag)")
                : "entity snapshots must be configurable and deduplicated";
        assert rewindEvents.contains("distSqr")
                && rewindEvents.contains("radiusCenterFor(level, level.dimension()")
                : "progressive playback must restore blocks in spatial wave order";
        assert rewindConfig.contains("worldSnapshotIntervalTicks")
                : "rewind config must expose the world snapshot interval";
        assert recorderSrc.contains("worldSnapshotIntervalTicks")
                : "recorder must use the configured world snapshot interval";
        assert rewindRestorer.contains("interpolatedSnapshot") && rewindRestorer.contains("lerpAngle")
                : "entity restore must interpolate positions between snapshots";
        assert rewindRestorer.contains("latest.dayTime() + Math.max(0L, targetTick - latest.tick())")
                : "world time restore must compute the exact target dayTime";
        assert rewindRestorer.contains("findNextPlayerSnapshot")
                && rewindRestorer.contains("next.tick() > snapshot.tick()")
                : "player position restore must interpolate between snapshots";
        assert rewindEvents.contains("nextAfter") && rewindEvents.contains("interpolatedSnapshot(")
                : "progressive playback must interpolate entity positions";
        assert rewindEvents.contains("buildBlockList") && rewindEvents.contains("buildEntityTargets")
                && rewindEvents.contains("entityTargets") && rewindEvents.contains("blocksPerTick")
                : "animated playback must split blocks and entity targets";
        assert rewindRestorer.contains("retreatTime") && rewindRestorer.contains("collectRetreatTargets")
                : "animated playback must retreat time and hand targets to the client";
        assert rewindRestorer.contains("failedRestoreCount")
                : "restore must tolerate single-item failures";
        assert rewindRestorer.contains("isPlayerGone")
                : "restore must guard against a disconnected player";
        assert rewindRestorer.contains("正在回溯中")
                : "failed rewind acquire must notify the player";
        assert rewindEvents.contains("cancelPendingPlayback")
                : "active playback must be cancellable";
        assert rewindClient.contains("client-driven retreat")
                : "client must not visually freeze non-player entities during animated playback";

        YuanGodSwordConfig c = new YuanGodSwordConfig();
        assert c.rewindEnabled;
        assert c.rewindWindowSeconds == 10;
        assert c.rewindCooldownTicks == 600;
        assert c.rewindDeathCooldownTicks == 1200;
        assert c.rewindPlaybackSeconds == 2.5f;
        assert c.rewindShowStats;

        c.rewindEnabled = false;
        c.rewindWindowSeconds = 5;
        c.rewindDeathMaxRetries = 3;
        c.rewindFreeCamRestorePosition = false;
        c.rewindShowStats = false;

        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        c.write(stack);
        YuanGodSwordConfig back = new YuanGodSwordConfig();
        back.read(stack);

        assert !back.rewindEnabled;
        assert back.rewindWindowSeconds == 5;
        assert back.rewindDeathMaxRetries == 3;
        assert !back.rewindFreeCamRestorePosition;
        assert !back.rewindShowStats;
        assert back.sameAs(c);

        YuanGodSwordConfig bad = new YuanGodSwordConfig();
        bad.rewindScopeMode = 99;
        bad.rewindScope = -1;
        bad.rewindPlaybackMode = 99;
        bad.rewindRestoreOrder = -1;
        bad.rewindCameraMode = 99;
        bad.rewindPositionMode = 99;
        bad.rewindDeathMaxRetries = -1;
        ItemStack badStack = new ItemStack(Items.DIAMOND_SWORD);
        bad.write(badStack);
        YuanGodSwordConfig badBack = new YuanGodSwordConfig();
        badBack.read(badStack);
        assert badBack.rewindScopeMode == 1;
        assert badBack.rewindScope == 0;
        assert badBack.rewindPlaybackMode == 1;
        assert badBack.rewindRestoreOrder == 0;
        assert badBack.rewindCameraMode == 1;
        assert badBack.rewindPositionMode == 1;
        assert badBack.rewindDeathMaxRetries == 0;

        c.rewindPositionRewind = false;
        c.rewindCameraMode = 1;
        c.rewindFreeCamRestorePosition = true;
        assert !YuanTimeRewindRestorer.effectivePositionRewind(c)
                : "B free-camera mode must not force an active-rewind position return";
        c.rewindCameraMode = 0;
        assert !YuanTimeRewindRestorer.effectivePositionRewind(c);
        c.rewindPositionRewind = true;
        assert YuanTimeRewindRestorer.effectivePositionRewind(c);
        YuanGodSwordConfig effective = YuanTimeRewindRestorer.withEffectivePositionRewind(c, false);
        assert effective.rewindPositionRewind;
        effective = YuanTimeRewindRestorer.withEffectivePositionRewind(c, true);
        assert effective.rewindPositionRewind;
        c.rewindPositionRewind = false;

        UUID stateUuid = UUID.randomUUID();
        YuanTimeRewindServerState.startCooldown(stateUuid, 20);
        assert !YuanTimeRewindServerState.cooldownReady(stateUuid, 20);
        assert YuanTimeRewindServerState.cooldownRemainingMillis(stateUuid) > 0L;
        assert YuanTimeRewindServerState.tryAcquire();
        assert !YuanTimeRewindServerState.tryAcquire();
        YuanTimeRewindServerState.startPlayback(stateUuid, true, 0);
        YuanTimeRewindServerState.release();
        YuanTimeRewindServerState.endPlayback();
        assert YuanTimeRewindServerState.isRewinding() == false;

        YuanTimeRewindServerState.setPlaybackUntilMillis(System.currentTimeMillis() + 1000L);
        assert YuanTimeRewindServerState.isPlaybackActive();
        assert YuanTimeRewindServerState.getPlaybackUntilMillis() > System.currentTimeMillis();
        YuanTimeRewindServerState.clearPlayback();
        assert !YuanTimeRewindServerState.isPlaybackActive();
        assert YuanTimeRewindServerState.getPlaybackUntilMillis() == 0L;

        YuanTimeRewindServerState.setDeathRetry(stateUuid, 2);
        assert YuanTimeRewindServerState.getDeathRetry(stateUuid) == 2;
        YuanTimeRewindServerState.resetDeathRetry(stateUuid);
        assert YuanTimeRewindServerState.getDeathRetry(stateUuid) == 0;

        com.yuan.timerewind.RewindHistory h = new com.yuan.timerewind.RewindHistory(10);
        h.add(100, "a");
        h.add(105, "b");
        assert h.since(104).size() == 1;
        assert h.since(95).size() == 2;
        assert h.atOrBefore(104).size() == 1;
        assert h.atOrBefore(104).get(0).equals("a");

        CompoundTag oldBlockEntity = new CompoundTag();
        oldBlockEntity.putString("value", "old");
        YuanTimeRewindRecorder recorder = new YuanTimeRewindRecorder(null, 100);
        assert recorder.windowTicks() == 100 : "recorder must expose its configured history window";
        recorder.recordBlockChange(1, new BlockPos(1, 2, 3), Blocks.STONE.defaultBlockState(), oldBlockEntity);
        Object blockChange = recorder.history().since(1).get(0);
        assert blockChange instanceof YuanTimeRewindRecorder.BlockChange;
        assert ((YuanTimeRewindRecorder.BlockChange) blockChange).oldBlockEntity().equals(oldBlockEntity);
        Field lastBlockEntity = YuanTimeRewindRecorder.class.getDeclaredField("lastBlockEntity");
        lastBlockEntity.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<BlockPos, CompoundTag> baselines = (Map<BlockPos, CompoundTag>) lastBlockEntity.get(recorder);
        assert !baselines.containsKey(new BlockPos(1, 2, 3));

        CompoundTag safePlayer = safePlayerTag();
        assert YuanTimeRewindRestorer.isSafe(safePlayer);
        safePlayer.putFloat("Health", 1.0f);
        assert !YuanTimeRewindRestorer.isSafe(safePlayer);
        safePlayer = safePlayerTag();
        safePlayer.putInt("Fire", 5);
        assert !YuanTimeRewindRestorer.isSafe(safePlayer);
        safePlayer = safePlayerTag();
        safePlayer.putInt("Air", 0);
        assert !YuanTimeRewindRestorer.isSafe(safePlayer);
        safePlayer = safePlayerTag();
        safePlayer.putInt("foodLevel", 2);
        assert !YuanTimeRewindRestorer.isSafe(safePlayer);
        safePlayer = safePlayerTag();
        safePlayer.put("Pos", posTag(0.0D, -100.0D, 0.0D));
        assert !YuanTimeRewindRestorer.isSafe(safePlayer);

        Method findSafeTick = YuanTimeRewindRestorer.class.getDeclaredMethod(
                "findSafeTick", List.class, long.class, int.class, ServerLevel.class,
                YuanGodSwordConfig.class, ServerPlayer.class);
        findSafeTick.setAccessible(true);
        UUID playerId = UUID.randomUUID();
        c.rewindHostileCheck = false;
        List<YuanTimeRewindRecorder.PlayerSnapshot> unsafeSnapshots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CompoundTag tag = safePlayerTag();
            tag.putFloat("Health", 1.0f);
            unsafeSnapshots.add(new YuanTimeRewindRecorder.PlayerSnapshot(
                    40L + i * 10L, playerId, tag));
        }
        c.rewindDeathMaxRetries = 2;
        assert ((Long) findSafeTick.invoke(null, unsafeSnapshots, 100L, 0, null, c, null)).longValue() == 70L;
        c.rewindDeathMaxRetries = 0;
        assert ((Long) findSafeTick.invoke(null, unsafeSnapshots, 100L, 0, null, c, null)).longValue() == 40L;
        c.rewindDeathMaxRetries = 1;
        assert ((Long) findSafeTick.invoke(null, unsafeSnapshots, 100L, 0, null, c, null)).longValue() == 80L;

        List<YuanTimeRewindRecorder.PlayerSnapshot> minHeightSnapshots = new ArrayList<>();
        minHeightSnapshots.add(new YuanTimeRewindRecorder.PlayerSnapshot(90L, playerId, safePlayerTag()));
        CompoundTag belowNetherFloor = safePlayerTag();
        belowNetherFloor.put("Pos", posTag(0.0D, -1.0D, 0.0D));
        minHeightSnapshots.add(new YuanTimeRewindRecorder.PlayerSnapshot(100L, playerId, belowNetherFloor));
        c.rewindDeathMaxRetries = 0;
        assert ((Long) findSafeTick.invoke(null, minHeightSnapshots, 100L, 0, null, c, null)).longValue() == 90L;

        Method entitiesFirst = YuanTimeRewindRestorer.class.getDeclaredMethod(
                "isEntitiesFirst", YuanGodSwordConfig.class);
        entitiesFirst.setAccessible(true);
        c.rewindRestoreOrder = 1;
        assert Boolean.TRUE.equals(entitiesFirst.invoke(null, c));
        c.rewindRestoreOrder = 0;
        assert Boolean.FALSE.equals(entitiesFirst.invoke(null, c));

        Method categoryEnabled = YuanTimeRewindRestorer.class.getDeclaredMethod(
                "entityCategoryEnabled", YuanGodSwordConfig.class, EntityType.class);
        categoryEnabled.setAccessible(true);
        c.rewindItems = false;
        c.rewindExperience = true;
        c.rewindEntities = true;
        assert Boolean.FALSE.equals(categoryEnabled.invoke(null, c, EntityType.ITEM));
        assert Boolean.TRUE.equals(categoryEnabled.invoke(null, c, EntityType.EXPERIENCE_ORB));
        assert Boolean.TRUE.equals(categoryEnabled.invoke(null, c, EntityType.PIG));
        c.rewindItems = true;
        c.rewindExperience = false;
        assert Boolean.TRUE.equals(categoryEnabled.invoke(null, c, EntityType.ITEM));
        assert Boolean.FALSE.equals(categoryEnabled.invoke(null, c, EntityType.EXPERIENCE_ORB));

        Method shouldAttemptDeathRewind = YuanTimeRewindEvents.class.getDeclaredMethod(
                "shouldAttemptDeathRewind", int.class, int.class);
        shouldAttemptDeathRewind.setAccessible(true);
        assert Boolean.TRUE.equals(shouldAttemptDeathRewind.invoke(null, 2, 0));
        assert Boolean.TRUE.equals(shouldAttemptDeathRewind.invoke(null, 2, 3));
        assert Boolean.FALSE.equals(shouldAttemptDeathRewind.invoke(null, 3, 3));

        Method shouldRestoreSword = YuanTimeRewindEvents.class.getDeclaredMethod(
                "shouldRestoreSword", YuanGodSwordConfig.class, boolean.class);
        shouldRestoreSword.setAccessible(true);
        YuanGodSwordConfig noPlayerState = new YuanGodSwordConfig();
        noPlayerState.rewindPlayerState = false;
        assert Boolean.FALSE.equals(shouldRestoreSword.invoke(null, noPlayerState, false));
        assert Boolean.TRUE.equals(shouldRestoreSword.invoke(null, noPlayerState, true));
        noPlayerState.rewindPlayerState = true;
        assert Boolean.TRUE.equals(shouldRestoreSword.invoke(null, noPlayerState, false));

        Method hasReliableEntityBaseline = YuanTimeRewindRestorer.class.getDeclaredMethod(
                "hasReliableEntityBaseline", List.class, long.class);
        hasReliableEntityBaseline.setAccessible(true);
        assert Boolean.FALSE.equals(hasReliableEntityBaseline.invoke(null, List.of(), 100L));
        assert Boolean.FALSE.equals(hasReliableEntityBaseline.invoke(null,
                List.of(entitySnapshot(101L)), 100L));
        assert Boolean.TRUE.equals(hasReliableEntityBaseline.invoke(null,
                List.of(entitySnapshot(99L)), 100L));

        Method isWithinRadiusSqr = YuanTimeRewindRestorer.class.getDeclaredMethod(
                "isWithinRadiusSqr", double.class, double.class, double.class,
                double.class, double.class, double.class, double.class);
        isWithinRadiusSqr.setAccessible(true);
        assert Boolean.TRUE.equals(isWithinRadiusSqr.invoke(null,
                0.0D, 0.0D, 0.0D, 5.0D, 0.0D, 0.0D, 25.0D));
        assert Boolean.FALSE.equals(isWithinRadiusSqr.invoke(null,
                0.0D, 0.0D, 0.0D, 6.0D, 0.0D, 0.0D, 25.0D));

        Method isFreeCameraEnabled = YuanTimeRewindClient.class.getDeclaredMethod(
                "isFreeCameraEnabled", int.class, int.class, int.class);
        isFreeCameraEnabled.setAccessible(true);
        assert Boolean.TRUE.equals(isFreeCameraEnabled.invoke(null, 1, 7, 7));
        assert Boolean.FALSE.equals(isFreeCameraEnabled.invoke(null, 1, 8, 7));
        assert Boolean.FALSE.equals(isFreeCameraEnabled.invoke(null, 0, 7, 7));

        Method effectiveRestoreTag = YuanTimeRewindPlayerSnapshot.class.getDeclaredMethod(
                "effectiveRestoreTag", CompoundTag.class, boolean.class);
        effectiveRestoreTag.setAccessible(true);
        CompoundTag healthSnapshot = safePlayerTag();
        healthSnapshot.putFloat("Health", 7.5f);
        CompoundTag healthRestore =
                (CompoundTag) effectiveRestoreTag.invoke(null, healthSnapshot, true);
        assert healthRestore.getFloat("Health") == 7.5f;
        CompoundTag positionlessRestore =
                (CompoundTag) effectiveRestoreTag.invoke(null, healthSnapshot, false);
        assert positionlessRestore.getFloat("Health") == 7.5f;
        assert !positionlessRestore.contains("Pos");

        Method findPlayerSnapshot = YuanTimeRewindRestorer.class.getDeclaredMethod(
                "findPlayerSnapshot", ServerPlayer.class, List.class);
        findPlayerSnapshot.setAccessible(true);
        assert findPlayerSnapshot.invoke(null, null, List.of()) == null;
        assert findPlayerSnapshot.invoke(null, null, List.of("not-a-snapshot")) == null;

        List<ItemStack> containerDeltas =
                YuanTimeRewindRestorer.computeContainerDeltas(containerTag(5), containerTag(2));
        assert containerDeltas.size() == 1
                && containerDeltas.get(0).getItem() == Items.DIAMOND
                && containerDeltas.get(0).getCount() == 3
                : "container delta must remove only the missing stack count";
        assert YuanTimeRewindRestorer.computeContainerDeltas(containerTag(2), containerTag(5)).isEmpty()
                : "container delta must be empty when current has more items";
        CompoundTag multiTarget = containerTag(5);
        CompoundTag slot1 = new CompoundTag();
        slot1.putByte("Slot", (byte) 1);
        new ItemStack(Items.DIAMOND, 5).save(slot1);
        multiTarget.getList("Items", 10).add(slot1);
        assert YuanTimeRewindRestorer.computeContainerDeltas(multiTarget, containerTag(10)).isEmpty()
                : "one larger current stack must satisfy multiple target stacks";

        Vec3 netherToOverworld =
                YuanTimeRewindRestorer.radiusCenterFor(Level.NETHER, Level.OVERWORLD, 80.0D, 64.0D, 160.0D);
        assert netherToOverworld.x == 10.0D && netherToOverworld.z == 20.0D
                : "nether radius center must scale down by 8";
        Vec3 overworldToNether =
                YuanTimeRewindRestorer.radiusCenterFor(Level.OVERWORLD, Level.NETHER, 10.0D, 64.0D, 20.0D);
        assert overworldToNether.x == 80.0D && overworldToNether.z == 160.0D
                : "overworld radius center must scale up by 8";
        assert YuanTimeRewindRestorer.radiusScaleFor(Level.NETHER, Level.OVERWORLD) == 1.0D / 8.0D
                : "nether radius must scale down by 8";
        assert YuanTimeRewindRestorer.radiusScaleFor(Level.OVERWORLD, Level.NETHER) == 8.0D
                : "overworld radius must scale up by 8";

        assert rewindRestorer.contains("deductContainerDeltas")
                && rewindRestorer.contains("computeContainerDeltas")
                && rewindRestorer.contains("radiusCenterFor")
                && rewindRestorer.contains("radiusScaleFor")
                : "container delta and cross-dimension radius helpers missing";
        assert rewindRestorer.indexOf("deductContainerDeltas(data.level(), config, player, data.after())")
                >= 0
                : "container delta deduction must use the after-target history";
        assert rewindRestorer.contains("earliest.putIfAbsent(bc.pos(), bc)")
                : "container delta deduction must select the earliest after-target block entity event";

        assert !YuanTimeRewindRestorer.isProvablyCreatedAfter(900L, 1000L)
                : "an entity first seen before the target tick must never be discarded";
        assert !YuanTimeRewindRestorer.isProvablyCreatedAfter(1000L, 1000L)
                : "an entity first seen exactly on the target tick must never be discarded";
        assert YuanTimeRewindRestorer.isProvablyCreatedAfter(1001L, 1000L)
                : "an entity first seen after the target tick must be discarded";
        assert YuanTimeRewindRestorer.isProvablyCreatedAfter(null, 1000L)
                : "an entity the recorder has never seen must count as newly created";
        assert rewindRestorer.contains("YuanTimeRewindEvents.recorder(level)")
                && rewindRestorer.contains("entityFirstSeenTick")
                : "created-entity removal must key off the recorder first-sighting registry";
        assert !rewindRestorer.contains("retained.contains(entity.getUUID())")
                : "created-entity removal must not treat a missing snapshot as proof of creation";
        assert recorderSrc.contains("entityFirstSeenTick.putIfAbsent(uuid, tick)")
                : "the recorder must register the first tick every entity is seen";
        assert deductOtherPlayersBlock.contains("other == trigger && config.rewindPlayerState")
                : "the triggerer must be deducted when their player state is not rewound";
        assert !rewindRestorer.contains("be == null ? new CompoundTag()")
                : "a missing container must not be read as emptied when computing deltas";
        assert !rewindEvents.contains("throw e")
                : "a failed playback must be logged and cleaned up, not rethrown into the tick loop";
        assert rewindEvents.contains("playback failed, partial restore left applied")
                : "a failed playback has to report what it left half applied";
        assert rewindRestorer.contains("YuanTimeRewindEvents.clearPendingPlayback()")
                : "an aborted restore start must drop the pending playback it already published";

        System.out.println("YuanTimeRewindCheck OK");
    }

    private static void allowRegistryInitialization() throws Exception {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError forgeNetworkBootstrapFailure) {
            // Forge 47.4.20 initializes registries before its standalone NetworkEvent bootstrap fails.
        }
    }

    private static CompoundTag safePlayerTag() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Health", 20.0f);
        tag.putInt("Fire", 0);
        tag.putInt("Air", 300);
        tag.putInt("foodLevel", 20);
        tag.put("Pos", posTag(0.0D, 80.0D, 0.0D));
        return tag;
    }

    private static ListTag posTag(double x, double y, double z) {
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(x));
        pos.add(DoubleTag.valueOf(y));
        pos.add(DoubleTag.valueOf(z));
        return pos;
    }

    private static YuanTimeRewindRecorder.EntitySnapshot entitySnapshot(long tick) {
        return new YuanTimeRewindRecorder.EntitySnapshot(
                tick, UUID.randomUUID(), EntityType.PIG,
                0.0D, 0.0D, 0.0D,
                0.0f, 0.0f,
                0.0D, 0.0D, 0.0D,
                1.0f, new CompoundTag());
    }

    private static CompoundTag containerTag(int count) {
        CompoundTag tag = new CompoundTag();
        ListTag items = new ListTag();
        CompoundTag itemTag = new CompoundTag();
        itemTag.putByte("Slot", (byte) 0);
        new ItemStack(Items.DIAMOND, count).save(itemTag);
        items.add(itemTag);
        tag.put("Items", items);
        return tag;
    }
}
