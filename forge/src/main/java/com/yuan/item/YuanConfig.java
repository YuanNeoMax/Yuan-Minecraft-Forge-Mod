package com.yuan.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class YuanConfig {
    private static final String TAG = "YuanConfig";

    // --- keys ---
    public static final String K_SNIPE = "snipe", K_LIGHTNING = "lightning", K_SWEEP = "sweepAoe";
    public static final String K_RIGHT_AOE = "rightAoe", K_WORLD_KILL = "worldKill";
    public static final String K_RIGHT_LIGHTNING_ENABLED = "rightLightningEnabled";
    public static final String K_RIGHT_LIGHTNING_COUNT_MODE = "rightLightningCountMode";
    public static final String K_RIGHT_LIGHTNING_COUNT = "rightLightningCount";
    public static final String K_RIGHT_LIGHTNING_SPREAD = "rightLightningSpread";
    public static final String K_RIGHT_LIGHTNING_SOUND = "rightLightningSound";
    public static final String K_KILL_STRENGTH = "killStrength", K_HIT11 = "hit11";
    public static final String K_STRIP = "stripAbsorb", K_REFLECT = "reflectWrite";
    public static final String K_TORMENT_PCT = "tormentPct", K_RUIN_POWER = "ruinPower", K_RUIN_EXPLODE = "ruinExplode";
    public static final String K_OBLIVION_DROP = "oblivionDrop", K_OBLIVION_DEATH = "oblivionDeath";
    public static final String K_INVINCIBLE = "invincible", K_COUNTER = "counter", K_FLIGHT = "flight";
    // Inert since the legacy timestop removal (2026-10-11): kept so old saves still parse and
    // normalizeLegacyFloat keeps its migration behaviour. No longer shown in the config screen.
    public static final String K_TIME_STOP = "timeStop", K_TIME_FULL = "timeFull", K_TIME_RANGE = "timeRange";
    public static final String K_BAN_LIST = "banList", K_BAN_PERSIST = "banPersist", K_BAN_MARK = "banMark";
    public static final String K_REACH = "reach", K_SPEED = "speed", K_AOE_RANGE = "aoeRange";
    public static final String K_ABSOLUTE_DROP = "absoluteDrop", K_ABSOLUTE_REENTRY = "absoluteReentry";
    public static final String K_CORRIDOR_DISTANCE = "corridorDistance", K_CORRIDOR_RADIUS = "corridorRadius", K_CORRIDOR_BLOCK_CLIP = "corridorBlockClip";
    public static final String K_RELEASE_MODE = "releaseMode", K_PURGE_RANGE = "purgeRange";
    public static final String K_ATTACK_PLAYERS = "attackPlayers", K_ATTACK_ALLIES = "attackAllies", K_ATTACK_TAMED = "attackTamed";
    public static final String K_ATTACK_VILLAGERS = "attackVillagers", K_ATTACK_BOSSES = "attackBosses", K_MAX_ATTACK_TARGETS = "maxAttackTargets";
    public static final String K_ATTACK_ATTRIBUTE_MODE = "attackAttributeMode";
    public static final String K_DEFENSE_SCOPE = "defenseScope", K_DEFENSE_BLOCKING = "defenseBlocking", K_DEFENSE_ATTACK = "defenseAttack";
    public static final String K_DEFENSE_HURT = "defenseHurt", K_DEFENSE_HEALTH = "defenseHealth", K_DEFENSE_DEATH = "defenseDeath", K_DEFENSE_REMOVAL = "defenseRemoval";
    public static final String K_DEFENSE_KNOCKBACK = "defenseKnockback", K_DEFENSE_PUSH = "defensePush", K_DEFENSE_FIRE = "defenseFire", K_DEFENSE_AIR = "defenseAir";
    public static final String K_DEFENSE_FREEZE = "defenseFreeze", K_DEFENSE_FALL = "defenseFall", K_DEFENSE_HUNGER = "defenseHunger", K_DEFENSE_SUFFOCATION = "defenseSuffocation";
    public static final String K_DEFENSE_CLEANSE = "defenseCleanse", K_DEFENSE_ABSORPTION = "defenseAbsorption", K_DEFENSE_VOID = "defenseVoid";
    public static final String K_BINDING_MODE = "bindingMode", K_ALLOW_MANUAL_DROP = "allowManualDrop", K_ALLOW_CONTAINER = "allowContainer";
    public static final String K_ALLOW_PLAYER_TRANSFER = "allowPlayerTransfer", K_KEEP_ON_DEATH = "keepOnDeath", K_AUTO_RECALL = "autoRecall";
    public static final String K_RESTORE_ON_LOGIN = "restoreOnLogin", K_UNIQUE_WEAPON = "uniqueWeapon", K_RECALL_GRACE_TICKS = "recallGraceTicks";
    public static final String K_BINDING_DEFENSE_GRACE_TICKS = "bindingDefenseGraceTicks", K_BINDING_ADMIN_BYPASS = "bindingAdminBypass";
    public static final String K_DROP_DAMAGE_PROTECTION = "dropDamageProtection", K_DROP_CAN_DESPAWN = "dropCanDespawn", K_DROP_VOID_RESCUE = "dropVoidRescue";
    public static final String K_GLASS_ENABLED = "glassEnabled", K_GLASS_RADIUS = "glassRadius", K_GLASS_BLUR = "glassBlur";
    public static final String K_GLASS_TINT_R = "glassTintR", K_GLASS_TINT_G = "glassTintG", K_GLASS_TINT_B = "glassTintB", K_GLASS_TINT_ALPHA = "glassTintAlpha";
    public static final String K_GLASS_SHADOW_EXPAND = "glassShadowExpand", K_GLASS_SHADOW_FACTOR = "glassShadowFactor";
    public static final String K_GLASS_SHADOW_X = "glassShadowX", K_GLASS_SHADOW_Y = "glassShadowY";
    public static final String K_GLASS_SHADOW_R = "glassShadowR", K_GLASS_SHADOW_G = "glassShadowG", K_GLASS_SHADOW_B = "glassShadowB", K_GLASS_SHADOW_ALPHA = "glassShadowAlpha";
    public static final String K_GLASS_REF_THICKNESS = "glassRefThickness", K_GLASS_REF_FACTOR = "glassRefFactor", K_GLASS_DISPERSION = "glassDispersion";
    public static final String K_GLASS_FRESNEL_RANGE = "glassFresnelRange", K_GLASS_FRESNEL_HARDNESS = "glassFresnelHardness", K_GLASS_FRESNEL_FACTOR = "glassFresnelFactor";
    public static final String K_GLASS_GLARE_RANGE = "glassGlareRange", K_GLASS_GLARE_HARDNESS = "glassGlareHardness";

    public static boolean get(ItemStack stack, String key, boolean def) {
        CompoundTag tag = stack.getTagElement(TAG);
        return tag != null && tag.contains(key) ? tag.getBoolean(key) : def;
    }
    public static float getFloat(ItemStack stack, String key, float def) {
        CompoundTag tag = stack.getTagElement(TAG);
        if (tag == null || !tag.contains(key)) return def;
        float value = normalizeLegacyFloat(key, tag.getFloat(key));
        if (value != tag.getFloat(key)) tag.putFloat(key, value);
        return value;
    }
    public static int getInt(ItemStack stack, String key, int def) {
        CompoundTag tag = stack.getTagElement(TAG);
        return tag != null && tag.contains(key) ? tag.getInt(key) : def;
    }
    public static void set(ItemStack stack, String key, boolean val) { stack.getOrCreateTagElement(TAG).putBoolean(key, val); }
    public static void setFloat(ItemStack stack, String key, float val) { stack.getOrCreateTagElement(TAG).putFloat(key, val); }
    public static void setInt(ItemStack stack, String key, int val) { stack.getOrCreateTagElement(TAG).putInt(key, val); }

    public static float normalizeLegacyFloat(String key, float value) {
        if (K_TORMENT_PCT.equals(key) && value > 0 && value < 1) return value * 100;
        if (K_TIME_RANGE.equals(key) && value > 0 && value <= 1) return value * 100;
        return value;
    }

    public static CompoundTag sanitize(CompoundTag input) {
        CompoundTag clean = new CompoundTag();
        if (input == null) return clean;

        copyBoolean(input, clean, K_SNIPE, K_LIGHTNING, K_SWEEP, K_RIGHT_AOE, K_WORLD_KILL,
                K_RIGHT_LIGHTNING_ENABLED, K_RIGHT_LIGHTNING_SOUND,
                K_HIT11, K_STRIP, K_REFLECT, K_RUIN_EXPLODE, K_OBLIVION_DROP, K_OBLIVION_DEATH,
                K_INVINCIBLE, K_COUNTER, K_FLIGHT, K_TIME_STOP, K_TIME_FULL,
                K_BAN_LIST, K_BAN_PERSIST, K_BAN_MARK, K_GLASS_ENABLED,
                K_ABSOLUTE_DROP, K_CORRIDOR_BLOCK_CLIP, K_ATTACK_PLAYERS, K_ATTACK_ALLIES, K_ATTACK_TAMED,
                K_ATTACK_VILLAGERS, K_ATTACK_BOSSES, K_DEFENSE_BLOCKING, K_DEFENSE_ATTACK, K_DEFENSE_HURT,
                K_DEFENSE_HEALTH, K_DEFENSE_DEATH, K_DEFENSE_REMOVAL, K_DEFENSE_KNOCKBACK, K_DEFENSE_PUSH,
                K_DEFENSE_FIRE, K_DEFENSE_AIR, K_DEFENSE_FREEZE, K_DEFENSE_FALL, K_DEFENSE_HUNGER,
                K_DEFENSE_SUFFOCATION, K_DEFENSE_CLEANSE, K_DEFENSE_ABSORPTION, K_DEFENSE_VOID,
                K_ALLOW_MANUAL_DROP, K_ALLOW_CONTAINER, K_ALLOW_PLAYER_TRANSFER, K_KEEP_ON_DEATH, K_AUTO_RECALL,
                K_RESTORE_ON_LOGIN, K_UNIQUE_WEAPON, K_BINDING_ADMIN_BYPASS, K_DROP_DAMAGE_PROTECTION,
                K_DROP_CAN_DESPAWN, K_DROP_VOID_RESCUE);
        putInt(input, clean, K_KILL_STRENGTH, 0, 3);
        putInt(input, clean, K_RIGHT_LIGHTNING_COUNT_MODE, 0, 1);
        putInt(input, clean, K_RIGHT_LIGHTNING_COUNT, 1, 128);
        putInt(input, clean, K_ABSOLUTE_REENTRY, 0, 2);
        putInt(input, clean, K_RELEASE_MODE, 0, 3);
        putInt(input, clean, K_MAX_ATTACK_TARGETS, 1, 4096);
        putInt(input, clean, K_ATTACK_ATTRIBUTE_MODE, 0, 2);
        putInt(input, clean, K_DEFENSE_SCOPE, 0, 2);
        putInt(input, clean, K_BINDING_MODE, 0, 3);
        putInt(input, clean, K_RECALL_GRACE_TICKS, 0, 1200);
        putInt(input, clean, K_BINDING_DEFENSE_GRACE_TICKS, 0, 200);
        putFloat(input, clean, K_TORMENT_PCT, 1, 99, 50);
        putFloat(input, clean, K_RUIN_POWER, 1, 100, 10);
        putFloat(input, clean, K_TIME_RANGE, 10, 100, 100);
        putFloat(input, clean, K_REACH, 1, 32, 10);
        putFloat(input, clean, K_SPEED, 10, 1000, 100);
        putFloat(input, clean, K_AOE_RANGE, 5, 500, 30);
        putFloat(input, clean, K_RIGHT_LIGHTNING_SPREAD, 0, 128, 0);
        putFloat(input, clean, K_CORRIDOR_DISTANCE, 1, 128, 27);
        putFloat(input, clean, K_CORRIDOR_RADIUS, .5f, 16, 3);
        putFloat(input, clean, K_PURGE_RANGE, 1, 1024, 400);
        putFloat(input, clean, K_GLASS_RADIUS, 0, 30, 12);
        putFloat(input, clean, K_GLASS_BLUR, 0, 32, 12);
        putFloat(input, clean, K_GLASS_TINT_R, 0, 255, 0);
        putFloat(input, clean, K_GLASS_TINT_G, 0, 255, 0);
        putFloat(input, clean, K_GLASS_TINT_B, 0, 255, 0);
        putFloat(input, clean, K_GLASS_TINT_ALPHA, 0, 100, 0);
        putFloat(input, clean, K_GLASS_SHADOW_EXPAND, 0, 60, 30);
        putFloat(input, clean, K_GLASS_SHADOW_FACTOR, 0, 100, 25);
        putFloat(input, clean, K_GLASS_SHADOW_X, -20, 20, 0);
        putFloat(input, clean, K_GLASS_SHADOW_Y, -20, 20, 2);
        putFloat(input, clean, K_GLASS_SHADOW_R, 0, 255, 0);
        putFloat(input, clean, K_GLASS_SHADOW_G, 0, 255, 0);
        putFloat(input, clean, K_GLASS_SHADOW_B, 0, 255, 0);
        putFloat(input, clean, K_GLASS_SHADOW_ALPHA, 0, 100, 100);
        putFloat(input, clean, K_GLASS_REF_THICKNESS, 1, 60, 20);
        putFloat(input, clean, K_GLASS_REF_FACTOR, 1, 2, 1.4f);
        putFloat(input, clean, K_GLASS_DISPERSION, 0, 20, 7);
        putFloat(input, clean, K_GLASS_FRESNEL_RANGE, 1, 100, 30);
        putFloat(input, clean, K_GLASS_FRESNEL_HARDNESS, 0, 100, 20);
        putFloat(input, clean, K_GLASS_FRESNEL_FACTOR, 0, 100, 20);
        putFloat(input, clean, K_GLASS_GLARE_RANGE, 1, 100, 30);
        putFloat(input, clean, K_GLASS_GLARE_HARDNESS, 0, 100, 20);
        return clean;
    }

    private static void copyBoolean(CompoundTag input, CompoundTag clean, String... keys) {
        for (String key : keys) {
            if (input.contains(key, Tag.TAG_BYTE)) clean.putBoolean(key, input.getBoolean(key));
        }
    }

    private static void putInt(CompoundTag input, CompoundTag clean, String key, int min, int max) {
        Tag value = input.get(key);
        if (value == null || value.getId() < Tag.TAG_BYTE || value.getId() > Tag.TAG_LONG) return;
        long integer = ((net.minecraft.nbt.NumericTag)value).getAsLong();
        clean.putInt(key, (int)Math.max(min, Math.min(max, integer)));
    }

    private static void putFloat(CompoundTag input, CompoundTag clean, String key, float min, float max, float def) {
        if (!input.contains(key, Tag.TAG_ANY_NUMERIC)) return;
        float value = normalizeLegacyFloat(key, input.getFloat(key));
        if (Float.isNaN(value)) value = def;
        clean.putFloat(key, Math.max(min, Math.min(max, value)));
    }

    public static void resetDefault(ItemStack stack) {
        resetGameplayDefault(stack);
        resetGlassDefault(stack);
    }

    private static void resetGameplayDefault(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG);
        tag.putBoolean(K_SNIPE, true); tag.putBoolean(K_LIGHTNING, true); tag.putBoolean(K_SWEEP, true);
        tag.putBoolean(K_RIGHT_AOE, true); tag.putBoolean(K_WORLD_KILL, true);
        tag.putBoolean(K_RIGHT_LIGHTNING_ENABLED, true); tag.putInt(K_RIGHT_LIGHTNING_COUNT_MODE, 0);
        tag.putInt(K_RIGHT_LIGHTNING_COUNT, 1); tag.putFloat(K_RIGHT_LIGHTNING_SPREAD, 0);
        tag.putBoolean(K_RIGHT_LIGHTNING_SOUND, true);
        tag.putInt(K_KILL_STRENGTH, 0); tag.putBoolean(K_HIT11, true); tag.putBoolean(K_STRIP, true); tag.putBoolean(K_REFLECT, true);
        tag.putFloat(K_TORMENT_PCT, 50f);
        tag.putFloat(K_RUIN_POWER, 10f); tag.putBoolean(K_RUIN_EXPLODE, true);
        tag.putBoolean(K_OBLIVION_DROP, false); tag.putBoolean(K_OBLIVION_DEATH, false);
        tag.putBoolean(K_INVINCIBLE, true); tag.putBoolean(K_COUNTER, true); tag.putBoolean(K_FLIGHT, true);
        tag.putBoolean(K_TIME_STOP, true); tag.putBoolean(K_TIME_FULL, true); tag.putFloat(K_TIME_RANGE, 100f);
        tag.putBoolean(K_BAN_LIST, true); tag.putBoolean(K_BAN_PERSIST, true); tag.putBoolean(K_BAN_MARK, true);
        tag.putFloat(K_REACH, 10f); tag.putFloat(K_SPEED, 100f); tag.putFloat(K_AOE_RANGE, 30f);
        tag.putBoolean(K_ABSOLUTE_DROP, false); tag.putInt(K_ABSOLUTE_REENTRY, 0);
        tag.putFloat(K_CORRIDOR_DISTANCE, 27); tag.putFloat(K_CORRIDOR_RADIUS, 3); tag.putBoolean(K_CORRIDOR_BLOCK_CLIP, true);
        tag.putInt(K_RELEASE_MODE, 3); tag.putFloat(K_PURGE_RANGE, 400);
        tag.putBoolean(K_ATTACK_PLAYERS, false); tag.putBoolean(K_ATTACK_ALLIES, false); tag.putBoolean(K_ATTACK_TAMED, false);
        tag.putBoolean(K_ATTACK_VILLAGERS, false); tag.putBoolean(K_ATTACK_BOSSES, true); tag.putInt(K_MAX_ATTACK_TARGETS, 512);
        tag.putInt(K_ATTACK_ATTRIBUTE_MODE, 0);
        tag.putInt(K_DEFENSE_SCOPE, 2); tag.putBoolean(K_DEFENSE_BLOCKING, true); tag.putBoolean(K_DEFENSE_ATTACK, true);
        tag.putBoolean(K_DEFENSE_HURT, true); tag.putBoolean(K_DEFENSE_HEALTH, true); tag.putBoolean(K_DEFENSE_DEATH, true);
        tag.putBoolean(K_DEFENSE_REMOVAL, true); tag.putBoolean(K_DEFENSE_KNOCKBACK, true); tag.putBoolean(K_DEFENSE_PUSH, true);
        tag.putBoolean(K_DEFENSE_FIRE, true); tag.putBoolean(K_DEFENSE_AIR, true); tag.putBoolean(K_DEFENSE_FREEZE, true);
        tag.putBoolean(K_DEFENSE_FALL, true); tag.putBoolean(K_DEFENSE_HUNGER, true); tag.putBoolean(K_DEFENSE_SUFFOCATION, true);
        tag.putBoolean(K_DEFENSE_CLEANSE, true); tag.putBoolean(K_DEFENSE_ABSORPTION, true); tag.putBoolean(K_DEFENSE_VOID, true);
        tag.putInt(K_BINDING_MODE, 1); tag.putBoolean(K_ALLOW_MANUAL_DROP, true); tag.putBoolean(K_ALLOW_CONTAINER, true);
        tag.putBoolean(K_ALLOW_PLAYER_TRANSFER, true); tag.putBoolean(K_KEEP_ON_DEATH, true); tag.putBoolean(K_AUTO_RECALL, true);
        tag.putBoolean(K_RESTORE_ON_LOGIN, true); tag.putBoolean(K_UNIQUE_WEAPON, true); tag.putInt(K_RECALL_GRACE_TICKS, 40);
        tag.putInt(K_BINDING_DEFENSE_GRACE_TICKS, 20); tag.putBoolean(K_BINDING_ADMIN_BYPASS, true);
        tag.putBoolean(K_DROP_DAMAGE_PROTECTION, true); tag.putBoolean(K_DROP_CAN_DESPAWN, false); tag.putBoolean(K_DROP_VOID_RESCUE, true);
    }

    public static void resetGlassDefault(ItemStack stack) {
        set(stack, K_GLASS_ENABLED, true);
        setFloat(stack, K_GLASS_RADIUS, 12); setFloat(stack, K_GLASS_BLUR, 12);
        setGlassColors(stack, 0, 0, 0, 0, 0, 0, 0, 100);
        setFloat(stack, K_GLASS_SHADOW_EXPAND, 30); setFloat(stack, K_GLASS_SHADOW_FACTOR, 25);
        setFloat(stack, K_GLASS_SHADOW_X, 0); setFloat(stack, K_GLASS_SHADOW_Y, 2);
        setFloat(stack, K_GLASS_REF_THICKNESS, 20); setFloat(stack, K_GLASS_REF_FACTOR, 1.4f);
        setFloat(stack, K_GLASS_DISPERSION, 7); setFloat(stack, K_GLASS_FRESNEL_RANGE, 30);
        setFloat(stack, K_GLASS_FRESNEL_HARDNESS, 20); setFloat(stack, K_GLASS_FRESNEL_FACTOR, 20);
        setFloat(stack, K_GLASS_GLARE_RANGE, 30); setFloat(stack, K_GLASS_GLARE_HARDNESS, 20);
    }

    public static void presetAttack(ItemStack stack) {
        resetGameplayDefault(stack);
        set(stack, K_INVINCIBLE, false); set(stack, K_COUNTER, false); set(stack, K_FLIGHT, false);
        set(stack, K_TIME_STOP, false); set(stack, K_TIME_FULL, false);
        set(stack, K_BAN_LIST, false);
    }

    public static void presetDefense(ItemStack stack) {
        resetGameplayDefault(stack);
        set(stack, K_WORLD_KILL, false); set(stack, K_SWEEP, false);
        setInt(stack, K_KILL_STRENGTH, 3); // 击退
    }

    public static void presetTimeStop(ItemStack stack) {
        resetGameplayDefault(stack);
        set(stack, K_TIME_FULL, false);
        setFloat(stack, K_TIME_RANGE, 30f);
        set(stack, K_WORLD_KILL, false);
    }

    public static void presetAllOn(ItemStack stack) { resetGameplayDefault(stack); }

    public static void applyGlassColorPreset(ItemStack stack, int preset) {
        switch (preset) {
            case 1 -> setGlassColors(stack, 190, 225, 255, 12, 0, 0, 0, 100); // ice
            case 2 -> setGlassColors(stack, 105, 45, 180, 18, 0, 0, 0, 100); // Yuan purple
            case 3 -> setGlassColors(stack, 255, 185, 55, 14, 0, 0, 0, 100); // gilded
            case 4 -> setGlassColors(stack, 45, 230, 210, 13, 0, 0, 0, 100); // aurora
            case 5 -> setGlassColors(stack, 255, 145, 185, 12, 0, 0, 0, 100); // rose
            case 6 -> setGlassColors(stack, 12, 16, 26, 35, 0, 0, 0, 100); // obsidian
            default -> setGlassColors(stack, 0, 0, 0, 0, 0, 0, 0, 100);
        }
    }

    public static void applyGlassVisualPreset(ItemStack stack, int preset) {
        resetGlassDefault(stack);
        switch (preset) {
            case 1 -> { // clear
                setFloat(stack, K_GLASS_BLUR, 4); setFloat(stack, K_GLASS_REF_THICKNESS, 16);
                setFloat(stack, K_GLASS_FRESNEL_FACTOR, 14); setFloat(stack, K_GLASS_GLARE_HARDNESS, 12);
            }
            case 2 -> { // strong refraction
                setFloat(stack, K_GLASS_BLUR, 8); setFloat(stack, K_GLASS_REF_THICKNESS, 34);
                setFloat(stack, K_GLASS_REF_FACTOR, 1.7f); setFloat(stack, K_GLASS_DISPERSION, 12);
            }
            case 3 -> { // soft frost
                setFloat(stack, K_GLASS_BLUR, 24); setFloat(stack, K_GLASS_TINT_ALPHA, 10);
                setFloat(stack, K_GLASS_FRESNEL_FACTOR, 12); setFloat(stack, K_GLASS_GLARE_HARDNESS, 10);
            }
            case 4 -> { // crystal
                setFloat(stack, K_GLASS_BLUR, 3); setFloat(stack, K_GLASS_DISPERSION, 11);
                setFloat(stack, K_GLASS_FRESNEL_FACTOR, 38); setFloat(stack, K_GLASS_GLARE_HARDNESS, 35);
            }
            case 5 -> { // dark
                applyGlassColorPreset(stack, 6); setFloat(stack, K_GLASS_BLUR, 16);
                setFloat(stack, K_GLASS_SHADOW_FACTOR, 45);
            }
            case 6 -> { // Yuan
                applyGlassColorPreset(stack, 2); setFloat(stack, K_GLASS_REF_FACTOR, 1.55f);
                setFloat(stack, K_GLASS_DISPERSION, 10); setFloat(stack, K_GLASS_FRESNEL_FACTOR, 34);
            }
            default -> { }
        }
    }

    private static void setGlassColors(ItemStack stack, float tintR, float tintG, float tintB, float tintAlpha,
                                       float shadowR, float shadowG, float shadowB, float shadowAlpha) {
        setFloat(stack, K_GLASS_TINT_R, tintR); setFloat(stack, K_GLASS_TINT_G, tintG);
        setFloat(stack, K_GLASS_TINT_B, tintB); setFloat(stack, K_GLASS_TINT_ALPHA, tintAlpha);
        setFloat(stack, K_GLASS_SHADOW_R, shadowR); setFloat(stack, K_GLASS_SHADOW_G, shadowG);
        setFloat(stack, K_GLASS_SHADOW_B, shadowB); setFloat(stack, K_GLASS_SHADOW_ALPHA, shadowAlpha);
    }
}
