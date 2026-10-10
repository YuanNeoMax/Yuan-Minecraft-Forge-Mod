package com.yuan.event;

import com.yuan.item.YuanConfig;
import com.yuan.item.YuanAbsoluteAttack;
import com.yuan.item.YuanKillHelper;
import com.yuan.item.YuanSwordItem;
import com.yuan.item.YuanWeaponBinding;
import com.yuan.data.YuanBanData;
import com.yuan.Yuan;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class YuanSwordEvents {
    private static final String YUAN_FLIGHT = "YuanFlightGranted";
    private static final String YUAN_VOID_HOLD = "YuanVoidHold";
    private static final String YUAN_VOID_RETRY = "YuanVoidRetry";
    private static final String YUAN_VOID_PHASE = "YuanVoidPhase";
    private static final ThreadLocal<Boolean> COUNTERING = ThreadLocal.withInitial(() -> false);

    /** Outcome of a void rescue attempt; used by the defense chain, not by time stop. */
    public enum RescueResult { RESCUED, HOLD_RETRY, PHASE_HOLD }

    public static boolean shouldCounter(boolean enabled, boolean attackerHoldingYuan, boolean countering,
                                        boolean targetAllowed) {
        return enabled && !attackerHoldingYuan && !countering && targetAllowed;
    }

    public static boolean shouldGrantFlight(boolean enabled, boolean mayfly) {
        return enabled && !mayfly;
    }

    public static boolean shouldRevokeFlight(boolean enabled, boolean grantedByYuan,
                                             boolean creative, boolean spectator) {
        return !enabled && grantedByYuan && !creative && !spectator;
    }

    public static boolean capabilityEnabled(boolean configured, boolean authorized) {
        return configured && authorized;
    }

    public static RescueResult rescueResult(boolean supportedDestination) {
        return supportedDestination ? RescueResult.RESCUED : RescueResult.HOLD_RETRY;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        revokeFlight(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) {
            YuanWeaponBinding.logout(player);
            YuanDefenseState.clear(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            YuanWeaponBinding.login(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        YuanDefenseState.reset(event.getOriginal().getUUID());
        YuanDefenseState.reset(event.getEntity().getUUID());
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement)
            YuanWeaponBinding.clonePlayer(original, replacement, event.isWasDeath());
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && YuanWeaponBinding.blockToss(player,
                event.getEntity().getItem(), YuanDefenseState.administrativeCommand())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem().getItem();
        if (YuanWeaponBinding.blockPickup(player, stack, YuanDefenseState.administrativeCommand())) {
            event.setCanceled(true);
        } else {
            YuanWeaponBinding.transferOwner(player, stack);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player)
            YuanWeaponBinding.containerClosed(player, event.getContainer());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && protects(player, YuanConfig.K_DEFENSE_DEATH)) {
            event.setCanceled(true);
            restoreHealth(player);
            return;
        }
        if (event.getEntity() instanceof Player player) {
            revokeFlight(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        YuanDefenseState.clearAll();
        YuanWeaponBinding.clearAll();
        YuanBanData.clearSession();
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack cfg = configStack(player);
        Defense defense = resolveDefense(player);
        if (cfg.isEmpty() && defense.isEmpty()) return;
        boolean counter = !cfg.isEmpty() && YuanConfig.get(cfg, YuanConfig.K_COUNTER, true);
        Entity attacker = event.getSource().getEntity();
        boolean countered = false;
        if (attacker instanceof LivingEntity le
                && shouldCounter(counter, isHoldingYuanSword(le), COUNTERING.get(),
                YuanSwordItem.isTarget(attacker, player, cfg))
                && player instanceof ServerPlayer serverPlayer
                && YuanWeaponBinding.canUseWeapon(serverPlayer, cfg)) {
            COUNTERING.set(true);
            try {
                YuanKillHelper.kill(attacker, player, cfg);
                countered = true;
            } finally {
                COUNTERING.set(false);
            }
        }
        if (countered || shouldBlockDamage(defense, event.getSource(), YuanConfig.K_DEFENSE_ATTACK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity le)) return;
        if (YuanBanData.isBanned(event.getLevel().getServer(), le.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        YuanKillHelper.tickPendingDragonFallbacks(event.getServer());
        if (event.getServer().getTickCount() % 20 == 0) YuanKillHelper.cleanupNoDrops();
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            YuanWeaponBinding.keepDeathDrops(player, event.getDrops());
        if (event.getEntity() instanceof ServerPlayer player && protects(player, YuanConfig.K_DEFENSE_DEATH)) {
            event.getDrops().clear();
            return;
        }
        if (YuanKillHelper.consumeNoDrops(event.getEntity().getUUID())) event.getDrops().clear();
    }

    @SubscribeEvent
    public static void onLivingExperience(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && protects(player, YuanConfig.K_DEFENSE_DEATH)) {
            event.setDroppedExperience(0);
        }
    }

    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && protects(player, YuanConfig.K_DEFENSE_KNOCKBACK)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (player instanceof ServerPlayer serverPlayer) YuanWeaponBinding.tick(serverPlayer);
        ItemStack cfg = configStack(player);
        Defense defense = resolveDefense(player);
        if (player instanceof ServerPlayer serverPlayer) repairDefense(serverPlayer, defense);
        boolean flightEnabled = !cfg.isEmpty() && YuanConfig.get(cfg, YuanConfig.K_FLIGHT, true);
        if (shouldGrantFlight(flightEnabled, player.getAbilities().mayfly)) {
            player.getAbilities().mayfly = true;
            player.getPersistentData().putBoolean(YUAN_FLIGHT, true);
            player.onUpdateAbilities();
        } else if (shouldRevokeFlight(flightEnabled,
                player.getPersistentData().getBoolean(YUAN_FLIGHT),
                player.isCreative(), player.isSpectator())) {
            revokeFlight(player);
        }
    }

    private static void revokeFlight(Player player) {
        if (!player.getPersistentData().getBoolean(YUAN_FLIGHT)) return;
        player.getPersistentData().remove(YUAN_FLIGHT);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static ItemStack defenseStack(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return ItemStack.EMPTY;
        if (player.getInventory() == null) return ItemStack.EMPTY;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof YuanSwordItem && YuanWeaponBinding.canUseWeapon(serverPlayer, main)) return main;
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof YuanSwordItem
                && YuanWeaponBinding.canUseWeapon(serverPlayer, offhand)
                && YuanConfig.getInt(offhand, YuanConfig.K_DEFENSE_SCOPE, 2) >= 1) return offhand;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof YuanSwordItem
                    && YuanWeaponBinding.canUseWeapon(serverPlayer, stack)
                    && YuanConfig.getInt(stack, YuanConfig.K_DEFENSE_SCOPE, 2) >= 2) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static boolean hasAbsoluteDefense(Player player) {
        return !YuanAbsoluteAttack.isActive(player) && !resolveDefense(player).isEmpty();
    }

    public static boolean protects(Player player, String key) {
        Defense defense = resolveDefense(player);
        return !defense.isEmpty() && YuanConfig.get(defense.stack(), key, true);
    }

    public static boolean shouldBlockDamage(Player player, net.minecraft.world.damagesource.DamageSource source,
                                            String genericKey) {
        return !player.level().isClientSide && shouldBlockDamage(resolveDefense(player), source, genericKey);
    }

    public static boolean shouldBlockHealthSet(Player player, float requested) {
        return shouldBlockHealthSet(player, requested, false);
    }

    public static boolean shouldBlockHealthSet(Player player, float requested, boolean allowedDecrease) {
        Defense defense = resolveDefense(player);
        return YuanDefenseState.shouldBlockHealthSet(!player.level().isClientSide,
                !defense.isEmpty() && YuanConfig.get(defense.stack(), YuanConfig.K_DEFENSE_HEALTH, true),
                YuanAbsoluteAttack.isActive(player), allowedDecrease,
                player.getHealth(), requested);
    }

    public static boolean shouldProtectRemoval(Player player, Entity.RemovalReason reason) {
        Defense defense = resolveDefense(player);
        return YuanDefenseState.protectRemoval(!player.level().isClientSide, !defense.isEmpty()
                        && YuanConfig.get(defense.stack(), YuanConfig.K_DEFENSE_REMOVAL, true),
                !defense.isEmpty() && YuanConfig.get(defense.stack(), YuanConfig.K_BINDING_ADMIN_BYPASS, true),
                YuanAbsoluteAttack.isActive(player), YuanDefenseState.administrativeCommand(),
                YuanDefenseState.lifecycleRemoval(), reason);
    }

    private static ItemStack configStack(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return ItemStack.EMPTY;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof YuanSwordItem && YuanWeaponBinding.canUseWeapon(serverPlayer, stack)) return stack;
        stack = player.getOffhandItem();
        return stack.getItem() instanceof YuanSwordItem && YuanWeaponBinding.canUseWeapon(serverPlayer, stack)
                ? stack : ItemStack.EMPTY;
    }

    private static void restoreHealth(ServerPlayer player) {
        player.setHealth(YuanDefenseState.recoveryHealth(player.getHealth(),
                YuanDefenseState.trustedHealth(player), player.getMaxHealth()));
        player.deathTime = 0;
    }

    private static void repairDefense(ServerPlayer player, Defense defense) {
        if (defense.isEmpty()) {
            clearVoidHold(player);
            YuanDefenseState.reset(player.getUUID());
            return;
        }
        ItemStack cfg = defense.stack();
        YuanDefenseState.updateSession(player.getUUID(), true, player.getHealth(), player.getMaxHealth(),
                player.getAbsorptionAmount());
        if (YuanConfig.get(cfg, YuanConfig.K_DEFENSE_HEALTH, true)) {
            float health = player.getHealth();
            float baseline = YuanDefenseState.trustedHealth(player);
            if (!YuanDefenseState.validHealth(health) || health < baseline) restoreHealth(player);
        }
        if (YuanConfig.get(cfg, YuanConfig.K_DEFENSE_FIRE, true)) player.clearFire();
        if (YuanConfig.get(cfg, YuanConfig.K_DEFENSE_AIR, true)) player.setAirSupply(player.getMaxAirSupply());
        if (YuanConfig.get(cfg, YuanConfig.K_DEFENSE_FREEZE, true)) player.setTicksFrozen(0);
        if (YuanConfig.get(cfg, YuanConfig.K_DEFENSE_FALL, true)) player.fallDistance = 0;
        if (YuanConfig.get(cfg, YuanConfig.K_DEFENSE_CLEANSE, true)) {
            player.getActiveEffects().stream()
                    .filter(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL)
                    .map(effect -> effect.getEffect()).toList().forEach(player::removeEffect);
        }
        if (YuanConfig.get(cfg, YuanConfig.K_DEFENSE_ABSORPTION, true)) {
            float absorption = player.getAbsorptionAmount();
            float baseline = YuanDefenseState.trustedAbsorption(player);
            if (!Float.isFinite(absorption) || absorption < baseline) player.setAbsorptionAmount(baseline);
        }
        if (!YuanConfig.get(cfg, YuanConfig.K_DEFENSE_VOID, true)) {
            clearVoidHold(player);
            return;
        }
        int minimum = player.serverLevel().getMinBuildHeight();
        if (player.getPersistentData().getBoolean(YUAN_VOID_RETRY)) {
            rescueFromVoid(player);
            return;
        }
        if (player.getY() >= minimum) {
            if (safePosition(player, player.position())) {
                YuanDefenseState.recordSafePosition(player);
                clearVoidHold(player);
            }
            return;
        }
        rescueFromVoid(player);
    }

    private static RescueResult rescueFromVoid(ServerPlayer player) {
        Vec3 safe = YuanDefenseState.safePosition(player);
        if (safe != null && !safePosition(player, safe)) {
            YuanDefenseState.forgetSafePosition(player);
            safe = null;
        }
        ResourceKey<Level> respawnDimension = player.getRespawnDimension();
        BlockPos respawn = player.getRespawnPosition();
        ServerLevel respawnLevel = respawn == null ? null : player.getServer().getLevel(respawnDimension);
        Optional<Vec3> resolvedRespawn = respawnLevel == null ? Optional.empty()
                : ServerPlayer.findRespawnPositionAndUseSpawnBlock(respawnLevel, respawn,
                player.getRespawnAngle(), player.isRespawnForced(), true);
        ServerLevel destination = safe != null ? player.serverLevel()
                : resolvedRespawn.isPresent() ? respawnLevel : player.getServer().overworld();
        Vec3 target = safe != null ? safe : resolvedRespawn.orElse(null);
        target = findHoldingPosition(player, destination, target != null ? BlockPos.containing(target)
                : destination.getSharedSpawnPos(), 4, 4);
        if (target != null) {
            YuanDefenseState.terminalRescueDecision(true, true, player.isNoGravity(), player.noPhysics,
                    BlockPos.containing(target).getY(), destination.getMinBuildHeight());
            moveRescued(player, destination, target);
            return RescueResult.RESCUED;
        }

        ServerLevel current = player.serverLevel();
        target = findSuspensionPosition(player, current, player.blockPosition(), 4);
        destination = current;
        if (target == null) {
            destination = player.getServer().overworld();
            target = findSuspensionPosition(player, destination, destination.getSharedSpawnPos(), 0);
        }
        if (target == null) {
            destination = current;
            target = findSuspensionPosition(player, current, player.blockPosition(), 24);
        }
        if (target == null) {
            destination = player.getServer().overworld();
            target = findSuspensionPosition(player, destination, destination.getSharedSpawnPos(), 32);
        }
        if (target == null) {
            destination = current;
            target = findSuspensionInColumn(player, current, player.blockPosition());
        }
        if (target == null) target = findSuspensionInPlayerChunk(player, current);
        if (target != null) {
            YuanDefenseState.RescueDecision decision = YuanDefenseState.terminalRescueDecision(false,
                    true, player.isNoGravity(), player.noPhysics, BlockPos.containing(target).getY(),
                    destination.getMinBuildHeight());
            moveHolding(player, destination, target, decision);
            return RescueResult.HOLD_RETRY;
        }
        net.minecraft.world.level.border.WorldBorder border = current.getWorldBorder();
        BlockPos spawn = current.getSharedSpawnPos();
        YuanDefenseState.EmergencyTarget emergency = YuanDefenseState.emergencyTarget(
                player.getX(), player.getZ(), spawn.getX() + 0.5, spawn.getZ() + 0.5,
                border.getMinX(), border.getMaxX(), border.getMinZ(), border.getMaxZ(),
                current.getMinBuildHeight(), current.getMaxBuildHeight());
        YuanDefenseState.RescueDecision decision = YuanDefenseState.terminalRescueDecision(false,
                false, player.isNoGravity(), player.noPhysics, emergency.y(), current.getMinBuildHeight());
        movePhaseHolding(player, emergency, decision);
        return RescueResult.PHASE_HOLD;
    }

    private static void moveRescued(ServerPlayer player, ServerLevel destination, Vec3 target) {
        player.teleportTo(destination, target.x, target.y, target.z, player.getYRot(), player.getXRot());
        clearVoidHold(player);
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void moveHolding(ServerPlayer player, ServerLevel destination, Vec3 target,
                                    YuanDefenseState.RescueDecision decision) {
        player.teleportTo(destination, target.x, target.y, target.z, player.getYRot(), player.getXRot());
        player.getPersistentData().putBoolean(YUAN_VOID_RETRY, true);
        if (decision.enableNoGravity()) {
            player.setNoGravity(true);
            player.getPersistentData().putBoolean(YUAN_VOID_HOLD, true);
        }
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void movePhaseHolding(ServerPlayer player, YuanDefenseState.EmergencyTarget target,
                                         YuanDefenseState.RescueDecision decision) {
        player.setPos(target.x(), target.y(), target.z());
        player.getPersistentData().putBoolean(YUAN_VOID_RETRY, true);
        if (decision.enableNoGravity()) {
            player.setNoGravity(true);
            player.getPersistentData().putBoolean(YUAN_VOID_HOLD, true);
        }
        if (decision.enableNoPhysics()) {
            player.noPhysics = true;
            player.getPersistentData().putBoolean(YUAN_VOID_PHASE, true);
        }
        player.fallDistance = 0;
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void clearVoidHold(Player player) {
        boolean retry = player.getPersistentData().getBoolean(YUAN_VOID_RETRY);
        boolean owned = player.getPersistentData().getBoolean(YUAN_VOID_HOLD);
        boolean phaseOwned = player.getPersistentData().getBoolean(YUAN_VOID_PHASE);
        YuanDefenseState.VoidCleanup cleanup = YuanDefenseState.cleanupVoidState(retry, owned, phaseOwned);
        if (cleanup.clearRetry()) player.getPersistentData().remove(YUAN_VOID_RETRY);
        if (cleanup.clearNoGravity()) {
            player.getPersistentData().remove(YUAN_VOID_HOLD);
            player.setNoGravity(false);
        }
        if (cleanup.clearNoPhysics()) {
            player.getPersistentData().remove(YUAN_VOID_PHASE);
            player.noPhysics = false;
        }
    }

    private static boolean shouldBlockDamage(Defense defense, net.minecraft.world.damagesource.DamageSource source,
                                             String genericKey) {
        if (defense.isEmpty()) return false;
        ItemStack stack = defense.stack();
        int type = source.is(net.minecraft.world.damagesource.DamageTypes.STARVE) ? 1
                : source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL) ? 2 : 0;
        return YuanDefenseState.shouldBlockDamage(true, true, YuanAbsoluteAttack.isActive(defense.player()),
                YuanConfig.get(stack, genericKey, true),
                YuanConfig.get(stack, YuanConfig.K_DEFENSE_HUNGER, true),
                YuanConfig.get(stack, YuanConfig.K_DEFENSE_SUFFOCATION, true), type);
    }

    private static Defense resolveDefense(Player player) {
        if (YuanAbsoluteAttack.isActive(player)) return Defense.NONE;
        ItemStack permanent = defenseStack(player);
        if (!permanent.isEmpty() && YuanConfig.get(permanent, YuanConfig.K_INVINCIBLE, true))
            return new Defense(player, permanent);
        if (!player.isUsingItem()) return Defense.NONE;
        ItemStack used = player.getUseItem();
        if (!(used.getItem() instanceof YuanSwordItem)) return Defense.NONE;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !YuanWeaponBinding.canUseWeapon(serverPlayer, used)) return Defense.NONE;
        int slot = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 0 : 1;
        int scope = YuanConfig.getInt(used, YuanConfig.K_DEFENSE_SCOPE, 2);
        return YuanDefenseState.activeDefense(false,
                YuanConfig.get(used, YuanConfig.K_DEFENSE_BLOCKING, true), true,
                YuanDefenseState.scopeAllows(scope, slot), false) ? new Defense(player, used) : Defense.NONE;
    }

    private static boolean safePosition(ServerPlayer player, Vec3 position) {
        return safePosition(player, player.serverLevel(), position);
    }

    private static Vec3 findSafePosition(ServerPlayer player, ServerLevel level, BlockPos center,
                                         int radius, int vertical) {
        for (int dy = 0; dy <= vertical; dy++) for (int r = 0; r <= radius; r++)
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (r > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                Vec3 candidate = Vec3.atBottomCenterOf(center.offset(dx, dy, dz));
                if (safePosition(player, level, candidate)) return candidate;
            }
        return null;
    }

    private static Vec3 findHoldingPosition(ServerPlayer player, ServerLevel level, BlockPos center,
                                            int radius, int vertical) {
        return findSafePosition(player, level, center, radius, vertical);
    }

    private static Vec3 findSuspensionPosition(ServerPlayer player, ServerLevel level, BlockPos center, int radius) {
        int startY = YuanDefenseState.rescueY(center.getY(), level.getMinBuildHeight());
        for (int y = startY; y < level.getMaxBuildHeight() - 1; y++)
            for (int r = 0; r <= radius; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (r > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                Vec3 candidate = Vec3.atBottomCenterOf(new BlockPos(center.getX() + dx, y, center.getZ() + dz));
                if (suspensionPosition(player, level, candidate)) return candidate;
            }
        return null;
    }

    private static Vec3 findSuspensionInColumn(ServerPlayer player, ServerLevel level, BlockPos column) {
        for (int y = level.getMinBuildHeight() + 1; y < level.getMaxBuildHeight() - 1; y++) {
            Vec3 candidate = Vec3.atBottomCenterOf(new BlockPos(column.getX(), y, column.getZ()));
            if (suspensionPosition(player, level, candidate)) return candidate;
        }
        return null;
    }

    private static Vec3 findSuspensionInPlayerChunk(ServerPlayer player, ServerLevel level) {
        BlockPos position = player.blockPosition();
        int minX = (position.getX() >> 4) << 4;
        int minZ = (position.getZ() >> 4) << 4;
        for (int y = level.getMinBuildHeight() + 1; y < level.getMaxBuildHeight() - 1; y++)
            for (int x = minX; x < minX + 16; x++) for (int z = minZ; z < minZ + 16; z++) {
                Vec3 candidate = Vec3.atBottomCenterOf(new BlockPos(x, y, z));
                if (suspensionPosition(player, level, candidate)) return candidate;
            }
        return null;
    }

    private static boolean suspensionPosition(ServerPlayer player, ServerLevel level, Vec3 position) {
        BlockPos feet = BlockPos.containing(position);
        BlockPos head = feet.above();
        AABB bounds = player.getDimensions(Pose.STANDING).makeBoundingBox(position);
        boolean buildBounds = feet.getY() >= level.getMinBuildHeight() + 1
                && head.getY() < level.getMaxBuildHeight();
        return YuanDefenseState.suspensionPosition(level.noCollision(player, bounds),
                level.getBlockState(head).getCollisionShape(level, head).isEmpty(),
                level.getFluidState(feet).isEmpty() && level.getFluidState(head).isEmpty(),
                level.getWorldBorder().isWithinBounds(bounds), buildBounds, level.hasChunkAt(feet));
    }

    private static boolean safePosition(ServerPlayer player, ServerLevel level, Vec3 position) {
        BlockPos feet = BlockPos.containing(position);
        BlockPos head = feet.above();
        BlockPos below = feet.below();
        AABB bounds = player.getDimensions(Pose.STANDING).makeBoundingBox(position);
        boolean buildBounds = feet.getY() >= level.getMinBuildHeight()
                && head.getY() < level.getMaxBuildHeight();
        boolean loaded = level.hasChunkAt(feet) && level.hasChunkAt(below);
        if (!loaded) return false;
        BlockState support = level.getBlockState(below);
        return YuanDefenseState.supportedRescuePosition(
                support.isFaceSturdy(level, below, Direction.UP),
                level.noCollision(player, bounds),
                level.getBlockState(head).getCollisionShape(level, head).isEmpty(),
                level.getFluidState(feet).isEmpty() && level.getFluidState(head).isEmpty(),
                level.getWorldBorder().isWithinBounds(bounds), buildBounds, loaded);
    }

    private record Defense(Player player, ItemStack stack) {
        private static final Defense NONE = new Defense(null, ItemStack.EMPTY);
        private boolean isEmpty() { return stack.isEmpty(); }
    }

    private static boolean isHoldingYuanSword(LivingEntity entity) {
        return entity.getMainHandItem().getItem() instanceof YuanSwordItem
            || entity.getOffhandItem().getItem() instanceof YuanSwordItem;
    }
}
