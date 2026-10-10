package com.yuan.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.yuan.client.particle.YuanParticleTypes;
import com.yuan.client.render.YuanColorHelper;
import com.yuan.event.YuanDroppedWeaponProtection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.entity.PartEntity;

import java.util.*;

public class YuanSwordItem extends SwordItem {
    private static final float AOE_RANGE = 30.0F;
    private static final int SPARK_COUNT = 20;
    private static final float SPARK_VELOCITY = 0.3F;
    private static final float HIT_SOUND_PITCH_MIN = 0.9F;
    private static final float HIT_SOUND_PITCH_MAX = 1.1F;
    private static final int FULL_CHARGE_TICKS = 20;
    private static final int WORLD_CHARGE_TICKS = 80;
    private static final int AOE_INTERVAL_TICKS = 5;
    private static final int MAX_AOE_EFFECTS = 8;
    private static final float SNIPE_RANGE = 64.0F;
    private static final UUID REACH_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID BLOCK_REACH_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    private static final String MODE_TAG = "YuanSwordMode";

    // ==================== Attack Mode ====================

    public enum AttackMode {
        ANNIHILATE(0, "§4§l诛灭", "一击必杀"),
        TORMENT(1, "§c凌迟", "血量减半"),
        RUIN(2, "§e崩坏", "极大击退"),
        OBLIVION(3, "§5§l寂灭", "直接移除"),
        ABSOLUTE(4, "§0§l绝对", "绝对抹除");

        final int id;
        final String displayName;
        final String description;

        AttackMode(int id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }

        private static final AttackMode[] VALUES = values();

        static AttackMode fromId(int id) { return VALUES[Math.floorMod(id, VALUES.length)]; }

        AttackMode next() { return VALUES[(id + 1) % VALUES.length]; }
        AttackMode prev() { return VALUES[(id + VALUES.length - 1) % VALUES.length]; }
    }

    public static AttackMode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(MODE_TAG) ? AttackMode.fromId(tag.getInt(MODE_TAG)) : AttackMode.ANNIHILATE;
    }

    static void setMode(ItemStack stack, AttackMode mode) {
        stack.getOrCreateTag().putInt(MODE_TAG, mode.id);
    }

    public static void nextMode(ItemStack stack) {
        setMode(stack, getMode(stack).next());
    }

    public static void prevMode(ItemStack stack) {
        setMode(stack, getMode(stack).prev());
    }

    // ==================== Constructor ====================

    public YuanSwordItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    // ==================== God‑weapon traits ====================

    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public int getMaxDamage(ItemStack stack) { return 0; }
    @Override public int getEnchantmentValue() { return Integer.MAX_VALUE; }
    @Override public boolean isFoil(ItemStack stack) { return false; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BLOCK; }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return Float.MAX_VALUE; }
    @Override public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) { return true; }
    @Override public boolean hasCustomEntity(ItemStack stack) { return true; }
    @Override public Entity createEntity(Level level, Entity location, ItemStack stack) {
        return YuanDroppedWeaponProtection.create(level, location, stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        HashMultimap<Attribute, AttributeModifier> map = HashMultimap.create(super.getDefaultAttributeModifiers(slot));
        if (slot == EquipmentSlot.MAINHAND) {
            map.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_UUID, "Yuan reach", 10.0, AttributeModifier.Operation.ADDITION));
            map.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(BLOCK_REACH_UUID, "Yuan block reach", 10.0, AttributeModifier.Operation.ADDITION));
        }
        return map;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        HashMultimap<Attribute, AttributeModifier> map = HashMultimap.create(super.getAttributeModifiers(slot, stack));
        if (slot == EquipmentSlot.MAINHAND) {
            float reach = YuanConfig.getFloat(stack, YuanConfig.K_REACH, 10f);
            AttributeModifier damage = attackDamageModifier(
                    YuanConfig.getInt(stack, YuanConfig.K_ATTACK_ATTRIBUTE_MODE, 0));
            if (damage != null) {
                map.removeAll(Attributes.ATTACK_DAMAGE);
                map.put(Attributes.ATTACK_DAMAGE, damage);
            }
            map.removeAll(Attributes.ATTACK_SPEED);
            map.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                    "Weapon modifier", attackSpeedModifier(YuanConfig.getFloat(stack, YuanConfig.K_SPEED, 100f)),
                    AttributeModifier.Operation.ADDITION));
            map.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_UUID, "Yuan reach", reach, AttributeModifier.Operation.ADDITION));
            map.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(BLOCK_REACH_UUID, "Yuan block reach", reach, AttributeModifier.Operation.ADDITION));
        }
        return map;
    }

    // ==================== Client renderer ====================



    // ==================== Left‑click — mode dispatch ====================

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide && (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || !YuanWeaponBinding.canUseWeapon(serverPlayer, stack))) return true;
        AttackMode mode = getMode(stack);
        if (player.isShiftKeyDown() && YuanConfig.get(stack, YuanConfig.K_SNIPE, true)) {
            Entity target = snipe(player, stack);
            if (target instanceof LivingEntity le) {
                boolean attacked = !player.level().isClientSide && applyModeToEntity(mode, le, player, stack);
                if (player.level().isClientSide) {
                    spawnSnipeBeam(player, le);
                }
                if (attacked && YuanConfig.get(stack, YuanConfig.K_LIGHTNING, true))
                    YuanKillHelper.chainLightning(player.level(), le);
            }
        } else {
            Entity directTarget = parent(entity);
            boolean attacked = !player.level().isClientSide && applyModeToEntity(mode, directTarget, player, stack);
            if (attacked && YuanConfig.get(stack, YuanConfig.K_LIGHTNING, true))
                YuanKillHelper.chainLightning(player.level(), directTarget);
            // Sweep AOE around target (诛灭 only)
            if (!player.level().isClientSide && mode == AttackMode.ANNIHILATE && YuanConfig.get(stack, YuanConfig.K_SWEEP, true)) {
                AABB sweep = new AABB(
                    directTarget.getX() - 5, directTarget.getY() - 5, directTarget.getZ() - 5,
                    directTarget.getX() + 5, directTarget.getY() + 5, directTarget.getZ() + 5);
                Set<Entity> targets = new LinkedHashSet<>();
                int maxTargets = maxAttackTargets(stack);
                for (Entity e : player.level().getEntitiesOfClass(Entity.class, sweep,
                        e -> isTarget(e, player, stack)))
                    if (targets.size() < maxTargets) addDistinctTarget(targets, directTarget, parent(e));
                for (Entity target : targets) applyModeToEntity(mode, target, player, stack);
            }
        }
        return true;
    }

    // ==================== Ray‑trace snipe — dot‑product beam, no AABB clip ====================

    private Entity snipe(Player player, ItemStack stack) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();

        Entity best = null;
        double bestDist = SNIPE_RANGE;

        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(SNIPE_RANGE), e -> isTarget(e, player, stack))) {
            Vec3 rel = e.getBoundingBox().getCenter().subtract(eye);
            double dot = look.dot(rel);
            if (dot <= 0) continue;                 // behind player
            double crossSq = rel.subtract(look.scale(dot)).lengthSqr(); // lateral offset squared
            if (crossSq < 4.0 && dot < bestDist) {   // within 2‑block‑wide beam
                bestDist = dot;
                best = e;
            }
        }
        return best;
    }

    // ==================== Swing particles ====================

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity.level().isClientSide) {
            Vec3 pos = entity.position().add(0, 1, 0);
            for (int i = 0; i < 10; i++) {
                double vx = (entity.getRandom().nextDouble() - 0.5) * 0.2;
                double vy = entity.getRandom().nextDouble() * 0.2;
                double vz = (entity.getRandom().nextDouble() - 0.5) * 0.2;
                entity.level().addParticle(YuanParticleTypes.GILDED_SPARK.get(), pos.x, pos.y, pos.z, vx, vy, vz);
            }
        }
        return super.onEntitySwing(stack, entity);
    }

    // ==================== Right‑click — instant AOE + start hold ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || !YuanWeaponBinding.canUseWeapon(serverPlayer, stack)))
            return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);

        if (!level.isClientSide && YuanConfig.get(stack, YuanConfig.K_RIGHT_AOE, true))
            aoePulse(level, player, stack, getMode(stack));

        return InteractionResultHolder.consume(stack);
    }

    // ==================== Right‑click hold — continuous AOE ====================

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || !YuanWeaponBinding.canUseWeapon(serverPlayer, stack)) return;
        if (!YuanConfig.get(stack, YuanConfig.K_RIGHT_AOE, true)) return;
        if (!shouldPulse(chargeTicks(remaining))) return;
        int hit = attackLoadedEntities(level, player, stack, getMode(stack),
                YuanConfig.getFloat(stack, YuanConfig.K_AOE_RANGE, AOE_RANGE), maxAttackTargets(stack));
        if (hit > 0) player.swing(InteractionHand.MAIN_HAND);
    }

    // ==================== Right‑click release — charged massive AOE ====================

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || !YuanWeaponBinding.canUseWeapon(serverPlayer, stack)) return;
        int chargeTicks = chargeTicks(timeLeft);
        int releaseMode = YuanConfig.getInt(stack, YuanConfig.K_RELEASE_MODE, 3);
        int maxTargets = maxAttackTargets(stack);
        Set<Entity> attacked = new HashSet<>();

        if (shouldReleaseImmediate(releaseMode)) {
            if (shouldPurgeOnRelease(player.isCrouching(), releaseMode))
                attackLoadedEntities(level, player, stack, getMode(stack),
                        YuanConfig.getFloat(stack, YuanConfig.K_PURGE_RANGE, 400), maxTargets, attacked);
            else if (level instanceof ServerLevel serverLevel)
                attackCorridor(serverLevel, player, stack, getMode(stack), maxTargets, attacked);
        }

        if (!shouldReleaseCharged(releaseMode) || chargeTicks < FULL_CHARGE_TICKS) return;

        if (!YuanConfig.get(stack, YuanConfig.K_WORLD_KILL, true)) return;

        AttackMode mode = getMode(stack);
        float range = YuanConfig.getFloat(stack, YuanConfig.K_AOE_RANGE, AOE_RANGE);
        if (isWorldCharge(chargeTicks) && player.getServer() != null) {
            for (ServerLevel serverLevel : player.getServer().getAllLevels())
                attackLoadedEntities(serverLevel, player, stack, mode, Double.POSITIVE_INFINITY, maxTargets, attacked);
        } else {
            attackLoadedEntities(level, player, stack, mode, range + chargeTicks * 1.5, maxTargets, attacked);
        }
    }

    static int chargeTicks(int timeLeft) {
        return Math.max(0, 72000 - timeLeft);
    }

    static boolean shouldPulse(int chargeTicks) {
        return chargeTicks > 0 && chargeTicks % AOE_INTERVAL_TICKS == 0;
    }

    static boolean isWorldCharge(int chargeTicks) {
        return chargeTicks >= WORLD_CHARGE_TICKS;
    }

    static boolean isInRange(double x1, double y1, double z1, double x2, double y2, double z2, double range) {
        if (!Double.isFinite(range)) return true;
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    static boolean isInCorridor(double sx, double sy, double sz, double lx, double ly, double lz,
                                double tx, double ty, double tz, double distance, double radius) {
        Vec3 look = new Vec3(lx, ly, lz).normalize();
        Vec3 relative = new Vec3(tx - sx, ty - sy, tz - sz);
        double ahead = look.dot(relative);
        return ahead >= 0 && ahead <= distance
                && relative.subtract(look.scale(ahead)).lengthSqr() <= radius * radius;
    }

    static boolean shouldReleaseImmediate(int mode) { return (mode & 1) != 0; }
    static boolean shouldReleaseCharged(int mode) { return (mode & 2) != 0; }
    static boolean shouldPurgeOnRelease(boolean crouching, int mode) {
        return crouching && shouldReleaseImmediate(mode);
    }

    static int compareTargetDistance(double left, double right) { return Double.compare(left, right); }
    static boolean hasTargetCapacity(int candidates, int attacked, int maxTargets) {
        return candidates + attacked < maxTargets;
    }
    static boolean isBlockPathAccepted(boolean clipEnabled, boolean obstructed) {
        return !clipEnabled || !obstructed;
    }
    static <T> boolean addEligibleTarget(Set<T> targets, T target, boolean alreadyAttacked,
                                         boolean targetAllowed, boolean spatiallyAllowed,
                                         int attacked, int maxTargets) {
        return !alreadyAttacked && targetAllowed && spatiallyAllowed
                && hasTargetCapacity(targets.size(), attacked, maxTargets) && targets.add(target);
    }

    private void attackCorridor(ServerLevel level, Player player, ItemStack stack, AttackMode mode,
                                int maxTargets, Set<Entity> attacked) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();
        double distance = YuanConfig.getFloat(stack, YuanConfig.K_CORRIDOR_DISTANCE, 27);
        double radius = YuanConfig.getFloat(stack, YuanConfig.K_CORRIDOR_RADIUS, 3);
        boolean blockClip = YuanConfig.get(stack, YuanConfig.K_CORRIDOR_BLOCK_CLIP, true);
        AABB bounds = new AABB(eye, eye.add(look.scale(distance))).inflate(radius);
        List<Entity> candidates = level.getEntities(player, bounds, e -> isTarget(parent(e), player, stack));
        candidates.sort((left, right) -> compareTargetDistance(
                parent(left).getBoundingBox().getCenter().distanceToSqr(eye),
                parent(right).getBoundingBox().getCenter().distanceToSqr(eye)));
        Set<Entity> selected = new LinkedHashSet<>();
        for (Entity candidate : candidates) {
            Entity target = parent(candidate);
            Vec3 center = target.getBoundingBox().getCenter();
            boolean obstructed = blockClip && level.clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS;
            if (!addEligibleTarget(selected, target, attacked.contains(target), isTarget(target, player, stack),
                    isInCorridor(eye.x, eye.y, eye.z, look.x, look.y, look.z,
                            center.x, center.y, center.z, distance, radius)
                            && isBlockPathAccepted(blockClip, obstructed), attacked.size(), maxTargets))
                continue;
        }
        List<Entity> hitTargets = new java.util.ArrayList<>();
        for (Entity target : selected) {
            if (!applyModeToEntity(mode, target, player, stack)) continue;
            attacked.add(target);
            hitTargets.add(target);
        }
        renderRightClickLightning(level, stack, hitTargets);
    }

    static boolean shouldRenderRuinExplosion(boolean enabled) { return enabled; }
    static double attackSpeedModifier(float configuredSpeed) { return configuredSpeed; }
    static int maxAttackTargets(ItemStack stack) {
        return YuanConfig.getInt(stack, YuanConfig.K_MAX_ATTACK_TARGETS, 512);
    }
    static int rightLightningCount(int mode, int fixed, int hitCount) {
        return mode == 1 ? hitCount : fixed;
    }
    static AttributeModifier attackDamageModifier(int mode) {
        if (mode == 0) return null;
        return new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                mode == 1 ? 1.0E30D : Double.POSITIVE_INFINITY, AttributeModifier.Operation.ADDITION);
    }

    // ==================== AOE helper ====================

    private void aoePulse(Level level, Player player, ItemStack stack, AttackMode mode) {
        float r = YuanConfig.getFloat(stack, YuanConfig.K_AOE_RANGE, AOE_RANGE);
        attackLoadedEntities(level, player, stack, mode, r, maxAttackTargets(stack));
    }

    private int attackLoadedEntities(Level level, Player player, ItemStack stack, AttackMode mode,
                                     double range, int maxTargets) {
        return attackLoadedEntities(level, player, stack, mode, range, maxTargets, new HashSet<>());
    }

    private int attackLoadedEntities(Level level, Player player, ItemStack stack, AttackMode mode,
                                     double range, int maxTargets, Set<Entity> attacked) {
        if (!(level instanceof ServerLevel serverLevel)) return 0;
        Set<Entity> targets = new LinkedHashSet<>();
        for (Entity e : serverLevel.getAllEntities()) {
            Entity target = parent(e);
            addEligibleTarget(targets, target, attacked.contains(target), isTarget(target, player, stack),
                    isInRange(player.getX(), player.getY(), player.getZ(), target.getX(), target.getY(), target.getZ(), range),
                    attacked.size(), maxTargets);
            if (!hasTargetCapacity(targets.size(), attacked.size(), maxTargets)) break;
        }
        List<Entity> hitTargets = new java.util.ArrayList<>();
        for (Entity e : targets) {
            if (!applyModeToEntity(mode, e, player, stack)) continue;
            attacked.add(e);
            if (hitTargets.size() < MAX_AOE_EFFECTS) spawnHitEffects(e, player, mode, stack);
            hitTargets.add(e);
        }
        renderRightClickLightning(level, stack, hitTargets);
        return hitTargets.size();
    }

    private void renderRightClickLightning(Level level, ItemStack stack, List<Entity> targets) {
        if (targets.isEmpty() || !YuanConfig.get(stack, YuanConfig.K_RIGHT_LIGHTNING_ENABLED, true)) return;
        int count = rightLightningCount(YuanConfig.getInt(stack, YuanConfig.K_RIGHT_LIGHTNING_COUNT_MODE, 0),
                YuanConfig.getInt(stack, YuanConfig.K_RIGHT_LIGHTNING_COUNT, 1), targets.size());
        float spread = YuanConfig.getFloat(stack, YuanConfig.K_RIGHT_LIGHTNING_SPREAD, 0);
        boolean sound = YuanConfig.get(stack, YuanConfig.K_RIGHT_LIGHTNING_SOUND, true);
        for (Entity target : targets) YuanKillHelper.visualLightning(level, target, count, spread, sound);
    }

    private boolean applyModeToEntity(AttackMode mode, Entity entity, Player player, ItemStack stack) {
        entity = parent(entity);
        if (player.level().isClientSide || !isTarget(entity, player, stack)) return false;
        switch (mode) {
            case ANNIHILATE -> YuanKillHelper.kill(entity, player, stack);
            case TORMENT    -> YuanKillHelper.halfHealth(entity, player, stack);
            case RUIN       -> YuanKillHelper.obliterate(entity, player, stack);
            case OBLIVION   -> YuanKillHelper.erase(entity, player, stack);
            case ABSOLUTE -> {
                try (YuanAbsoluteAttack.Scope ignored = YuanAbsoluteAttack.enter(entity)) {
                    YuanKillHelper.absoluteErase(entity, player, stack);
                }
            }
        }
        return true;
    }

    private static Entity parent(Entity entity) {
        return entity instanceof PartEntity<?> part ? part.getParent() : entity;
    }

    public static boolean isTarget(Entity entity, Player player, ItemStack stack) {
        entity = parent(entity);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return false;
        return targetPolicy(entity == player, entity instanceof Player, entity.isAlliedTo(player),
                entity instanceof TamableAnimal tame && tame.isTame(), entity instanceof Villager,
                entity.getType().is(Tags.EntityTypes.BOSSES),
                YuanConfig.get(stack, YuanConfig.K_ATTACK_PLAYERS, false),
                YuanConfig.get(stack, YuanConfig.K_ATTACK_ALLIES, false),
                YuanConfig.get(stack, YuanConfig.K_ATTACK_TAMED, false),
                YuanConfig.get(stack, YuanConfig.K_ATTACK_VILLAGERS, false),
                YuanConfig.get(stack, YuanConfig.K_ATTACK_BOSSES, true));
    }

    static boolean targetPolicy(boolean self, boolean player, boolean ally, boolean tamed, boolean villager, boolean boss,
                                boolean attackPlayers, boolean attackAllies, boolean attackTamed,
                                boolean attackVillagers, boolean attackBosses) {
        return !self && (!player || attackPlayers) && (!ally || attackAllies) && (!tamed || attackTamed)
                && (!villager || attackVillagers) && (!boss || attackBosses);
    }

    static <T> boolean addDistinctTarget(Set<T> targets, T directTarget, T candidate) {
        return candidate != directTarget && targets.add(candidate);
    }

    // ==================== Mode‑aware hit effects ====================

    private void spawnHitEffects(Entity target, LivingEntity attacker, AttackMode mode) {
        spawnHitEffects(target, attacker, mode, attacker.getMainHandItem());
    }

    private void spawnHitEffects(Entity target, LivingEntity attacker, AttackMode mode, ItemStack stack) {
        Level level = target.level();
        double hx = target.getX();
        double hy = target.getY();
        double hz = target.getZ();

        // Common: ring of light (all modes)
        level.addParticle(YuanParticleTypes.GILDED_RING.get(), hx, hy + 0.1, hz, 0, 0, 0);

        // Common: server smoke
        if (!level.isClientSide && target instanceof LivingEntity le) {
            Vec3 atkPos = attacker.position().add(0, 1, 0);
            Vec3 tgtCenter = le.getBoundingBox().getCenter();
            Vec3 dir = tgtCenter.subtract(atkPos).normalize();
            double w = le.getBbWidth() * 0.75;
            double h = le.getBbHeight() * 0.75;
            for (int i = 0; i < 50; i++) {
                double ox = (Math.random() - 0.5) * w;
                double oy = (Math.random() - 0.5) * h;
                double oz = (Math.random() - 0.5) * w;
                double s = 0.3 + Math.random() * 0.2;
                level.addParticle(ParticleTypes.SMOKE,
                    tgtCenter.x + ox, tgtCenter.y + oy, tgtCenter.z + oz,
                    dir.x * s + (Math.random() - 0.5) * 0.2,
                    dir.y * s + (Math.random() - 0.5) * 0.2,
                    dir.z * s + (Math.random() - 0.5) * 0.2);
            }
        }

        // ‑‑‑‑‑ Mode‑specific VFX ‑‑‑‑‑

        switch (mode) {
            case ANNIHILATE -> {
                // Gilded explosion: sparks + sweep sound
                for (int i = 0; i < SPARK_COUNT; i++) {
                    double vx = (attacker.getRandom().nextDouble() - 0.5) * SPARK_VELOCITY * 2;
                    double vy = attacker.getRandom().nextDouble() * SPARK_VELOCITY;
                    double vz = (attacker.getRandom().nextDouble() - 0.5) * SPARK_VELOCITY * 2;
                    level.addParticle(YuanParticleTypes.GILDED_SPARK.get(), hx, hy, hz, vx, vy, vz);
                }
                float pitch = HIT_SOUND_PITCH_MIN + attacker.getRandom().nextFloat() * (HIT_SOUND_PITCH_MAX - HIT_SOUND_PITCH_MIN);
                level.playSound(null, hx, hy, hz, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, pitch);
            }
            case TORMENT -> {
                // Blood burst: damage indicators + soul fire
                for (int i = 0; i < 20; i++) {
                    double vx = (attacker.getRandom().nextDouble() - 0.5) * 0.4;
                    double vy = attacker.getRandom().nextDouble() * 0.3;
                    double vz = (attacker.getRandom().nextDouble() - 0.5) * 0.4;
                    level.addParticle(ParticleTypes.DAMAGE_INDICATOR, hx, hy, hz, vx, vy, vz);
                }
                for (int i = 0; i < 15; i++) {
                    double vx = (attacker.getRandom().nextDouble() - 0.5) * 0.2;
                    double vy = attacker.getRandom().nextDouble() * 0.15;
                    double vz = (attacker.getRandom().nextDouble() - 0.5) * 0.2;
                    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, hx, hy, hz, vx, vy, vz);
                }
                level.playSound(null, hx, hy, hz, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 0.9F, 0.8F);
            }
            case RUIN -> {
                if (shouldRenderRuinExplosion(YuanConfig.get(stack, YuanConfig.K_RUIN_EXPLODE, true))) {
                    level.addParticle(ParticleTypes.EXPLOSION_EMITTER, hx, hy, hz, 0, 0, 0);
                    for (int i = 0; i < 40; i++) {
                        double vx = (attacker.getRandom().nextDouble() - 0.5) * 3.0;
                        double vy = attacker.getRandom().nextDouble() * 2.0;
                        double vz = (attacker.getRandom().nextDouble() - 0.5) * 3.0;
                        level.addParticle(ParticleTypes.POOF, hx, hy, hz, vx, vy, vz);
                    }
                    level.playSound(null, hx, hy, hz, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.7F);
                }
            }
            case OBLIVION -> {
                // Void consume: portal + dragon breath inward
                level.addParticle(ParticleTypes.PORTAL, hx, hy + 0.5, hz, 0, 0, 0);
                for (int i = 0; i < 25; i++) {
                    double vx = (attacker.getRandom().nextDouble() - 0.5) * 0.15;
                    double vy = (attacker.getRandom().nextDouble() - 0.5) * 0.15;
                    double vz = (attacker.getRandom().nextDouble() - 0.5) * 0.15;
                    level.addParticle(ParticleTypes.DRAGON_BREATH, hx, hy, hz, vx, vy, vz);
                }
                for (int i = 0; i < 10; i++) {
                    double vx = (attacker.getRandom().nextDouble() - 0.5) * 0.1;
                    double vy = attacker.getRandom().nextDouble() * 0.2;
                    double vz = (attacker.getRandom().nextDouble() - 0.5) * 0.1;
                    level.addParticle(YuanParticleTypes.GILDED_SPARK.get(), hx, hy, hz, vx, vy, vz);
                }
                level.playSound(null, hx, hy, hz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
            }
            case ABSOLUTE -> level.addParticle(ParticleTypes.REVERSE_PORTAL, hx, hy, hz, 0, 0, 0);
        }
    }

    // ==================== Snipe beam VFX ====================

    private void spawnSnipeBeam(Player player, Entity target) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 tgt = target.getBoundingBox().getCenter();
        Vec3 dir = tgt.subtract(eye);
        double len = dir.length();
        dir = dir.normalize();

        for (double d = 0; d < len; d += 0.3) {
            player.level().addParticle(ParticleTypes.END_ROD,
                eye.x + dir.x * d,
                eye.y + dir.y * d,
                eye.z + dir.z * d,
                0, 0, 0);
        }
    }

    // ==================== Rainbow name ====================

    @Override
    public Component getName(ItemStack stack) {
        Component original = super.getName(stack);
        if (stack.hasCustomHoverName()) return original;
        return rainbow(original.getString());
    }

    // ==================== Rainbow tooltip + mode info + usage ====================

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(rainbow(Component.translatable("tooltip.yuan.sword.1").getString()));
        tooltip.add(rainbow(Component.translatable("tooltip.yuan.sword.2").getString()));
        tooltip.add(rainbow(Component.translatable("tooltip.yuan.sword.3").getString()));

        AttackMode mode = getMode(stack);
        tooltip.add(Component.empty());
        tooltip.add(rainbow("✦ 当前模式: " + mode.displayName + " §8— " + mode.description));
        tooltip.add(Component.literal("§8Shift+滚轮 切换模式 | Shift+左键 射线狙击 | 右键 AOE").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));

        super.appendHoverText(stack, level, tooltip, flag);
    }

    // ==================== Rainbow utility ====================

    private static MutableComponent rainbow(String text) {
        MutableComponent result = Component.empty();
        long tick = System.currentTimeMillis() / 50;
        List<Integer> palette = YuanColorHelper.FoxBlade;
        int n = palette.size();
        for (int i = 0; i < text.length(); i++) {
            int color = palette.get((int)((i + tick) % n));
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
        }
        return result;
    }
}
