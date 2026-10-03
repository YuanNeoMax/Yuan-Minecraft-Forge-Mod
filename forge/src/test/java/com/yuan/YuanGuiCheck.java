package com.yuan;

import com.yuan.client.gui.YuanGuiLogic;
import com.yuan.client.gui.YuanConfigCatalog;
import com.yuan.client.gui.YuanConfigHistory;
import com.yuan.client.gui.YuanClientPreferences;
import com.yuan.client.gui.YuanPresetManager;
import com.yuan.client.gui.YuanSaveState;
import com.yuan.client.gui.YuanUiAnimation;
import com.yuan.client.gui.YuanConfigLayout;
import com.yuan.client.gui.YuanConfigScreen;
import com.yuan.client.YuanKeyBindingsCheck;
import com.yuan.client.gui.YuanScrollState;
import com.yuan.item.YuanConfig;
import com.yuan.item.YuanSwordItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;

import java.util.Map;
import java.util.Set;
import java.nio.file.Files;

public final class YuanGuiCheck {
    public static void main(String[] args) throws Exception {
        allowRegistryInitialization();
        YuanKeyBindingsCheck.check();
        assert YuanGuiLogic.clampScroll(-10, 100) == 0;
        assert YuanGuiLogic.clampScroll(120, 100) == 100;
        assert YuanGuiLogic.clampScroll(40, 100) == 40;
        assert YuanGuiLogic.shouldHandleModeScroll(false, 1);
        assert !YuanGuiLogic.shouldHandleModeScroll(true, 1);
        assert !YuanGuiLogic.shouldHandleModeScroll(false, 0);

        CompoundTag input = new CompoundTag();
        input.putFloat(YuanConfig.K_GLASS_RADIUS, 99.0f);
        input.putFloat(YuanConfig.K_GLASS_REF_FACTOR, Float.NaN);
        CompoundTag clean = YuanConfig.sanitize(input);
        assert clean.getFloat(YuanConfig.K_GLASS_RADIUS) == 30.0f;
        assert clean.getFloat(YuanConfig.K_GLASS_REF_FACTOR) == 1.4f;
        CompoundTag lightningInput = new CompoundTag();
        lightningInput.putInt(YuanConfig.K_RIGHT_LIGHTNING_COUNT, 999);
        lightningInput.putFloat(YuanConfig.K_RIGHT_LIGHTNING_SPREAD, 999);
        CompoundTag cleanLightning = YuanConfig.sanitize(lightningInput);
        assert cleanLightning.getInt(YuanConfig.K_RIGHT_LIGHTNING_COUNT) == 128;
        assert cleanLightning.getFloat(YuanConfig.K_RIGHT_LIGHTNING_SPREAD) == 128;

        ItemStack stack = new ItemStack(Items.STICK);
        YuanConfig.resetDefault(stack);
        YuanConfig.setFloat(stack, YuanConfig.K_GLASS_RADIUS, 18.0f);
        YuanConfig.presetAttack(stack);
        assert YuanConfig.getFloat(stack, YuanConfig.K_GLASS_RADIUS, 12.0f) == 18.0f
                : "combat presets must preserve glass settings";

        YuanConfig.applyGlassColorPreset(stack, 1);
        assert YuanConfig.getFloat(stack, YuanConfig.K_GLASS_TINT_B, 0.0f) > 0.0f;
        YuanConfig.applyGlassVisualPreset(stack, 0);
        assert YuanConfig.getFloat(stack, YuanConfig.K_GLASS_RADIUS, 0.0f) == 12.0f;
        assert YuanConfig.getFloat(stack, YuanConfig.K_GLASS_BLUR, 0.0f) == 12.0f;

        assert YuanConfigCatalog.all().stream().anyMatch(s -> s.key().equals(YuanConfig.K_WORLD_KILL));
        assert YuanConfigCatalog.find("右键").stream().anyMatch(s -> s.key().equals(YuanConfig.K_RIGHT_AOE));
        var worldKill = YuanConfigCatalog.byKey(YuanConfig.K_WORLD_KILL);
        assert worldKill != null && !worldKill.warning().isEmpty();
        assert worldKill.purpose().contains("20 tick") && worldKill.purpose().contains("80 tick")
                && worldKill.purpose().contains("跨维度")
                : "charged attack description must distinguish local expansion and cross-dimension behavior";
        assert worldKill.explain(YuanConfigCatalog.Detail.CONCISE).size()
                < worldKill.explain(YuanConfigCatalog.Detail.REFERENCE).size();

        String[] combatPolicyKeys = {
                YuanConfig.K_ABSOLUTE_DROP, YuanConfig.K_ABSOLUTE_REENTRY, YuanConfig.K_CORRIDOR_DISTANCE,
                YuanConfig.K_CORRIDOR_RADIUS, YuanConfig.K_CORRIDOR_BLOCK_CLIP, YuanConfig.K_RELEASE_MODE,
                YuanConfig.K_PURGE_RANGE, YuanConfig.K_ATTACK_PLAYERS, YuanConfig.K_ATTACK_ALLIES,
                YuanConfig.K_ATTACK_TAMED, YuanConfig.K_ATTACK_VILLAGERS, YuanConfig.K_ATTACK_BOSSES,
                YuanConfig.K_MAX_ATTACK_TARGETS, YuanConfig.K_ATTACK_ATTRIBUTE_MODE, YuanConfig.K_DEFENSE_SCOPE,
                YuanConfig.K_DEFENSE_BLOCKING, YuanConfig.K_DEFENSE_ATTACK, YuanConfig.K_DEFENSE_HURT,
                YuanConfig.K_DEFENSE_HEALTH, YuanConfig.K_DEFENSE_DEATH, YuanConfig.K_DEFENSE_REMOVAL,
                YuanConfig.K_DEFENSE_KNOCKBACK, YuanConfig.K_DEFENSE_PUSH, YuanConfig.K_DEFENSE_FIRE,
                YuanConfig.K_DEFENSE_AIR, YuanConfig.K_DEFENSE_FREEZE, YuanConfig.K_DEFENSE_FALL,
                YuanConfig.K_DEFENSE_HUNGER, YuanConfig.K_DEFENSE_SUFFOCATION, YuanConfig.K_DEFENSE_CLEANSE,
                YuanConfig.K_DEFENSE_ABSORPTION, YuanConfig.K_DEFENSE_VOID, YuanConfig.K_BINDING_MODE,
                YuanConfig.K_ALLOW_MANUAL_DROP, YuanConfig.K_ALLOW_CONTAINER, YuanConfig.K_ALLOW_PLAYER_TRANSFER,
                YuanConfig.K_KEEP_ON_DEATH, YuanConfig.K_AUTO_RECALL, YuanConfig.K_RESTORE_ON_LOGIN,
                YuanConfig.K_UNIQUE_WEAPON, YuanConfig.K_RECALL_GRACE_TICKS, YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS,
                YuanConfig.K_BINDING_ADMIN_BYPASS, YuanConfig.K_DROP_DAMAGE_PROTECTION,
                YuanConfig.K_DROP_CAN_DESPAWN, YuanConfig.K_DROP_VOID_RESCUE
        };
        for (String key : combatPolicyKeys) assert YuanConfigCatalog.byKey(key) != null : "missing catalog key " + key;
        assert YuanConfigCatalog.byKey(YuanConfig.K_CORRIDOR_DISTANCE).defaultValue() == 27;
        assert YuanConfigCatalog.byKey(YuanConfig.K_CORRIDOR_RADIUS).defaultValue() == 3;
        assert YuanConfigCatalog.byKey(YuanConfig.K_PURGE_RANGE).defaultValue() == 400;
        assert YuanConfigCatalog.byKey(YuanConfig.K_ATTACK_PLAYERS).defaultValue() == 0;
        assert YuanConfigCatalog.byKey(YuanConfig.K_DEFENSE_SCOPE).defaultValue() == 2;
        assert YuanConfigCatalog.byKey(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS).defaultValue() == 20;
        assert YuanConfigCatalog.valueLabel(YuanConfig.K_BINDING_MODE, 1).contains("敌对")
                : "mode 1 label must describe hostile-disarm recovery";
        assert YuanConfigCatalog.valueLabel(YuanConfig.K_BINDING_MODE, 2).contains("灵魂")
                : "mode 2 label must remain soul binding";
        assertEnum(YuanConfig.K_ABSOLUTE_REENTRY, 0, 2, 0);
        assertEnum(YuanConfig.K_RIGHT_LIGHTNING_COUNT_MODE, 0, 1, 0);
        assertEnum(YuanConfig.K_RELEASE_MODE, 0, 3, 3);
        assertEnum(YuanConfig.K_ATTACK_ATTRIBUTE_MODE, 0, 2, 0);
        assertEnum(YuanConfig.K_DEFENSE_SCOPE, 0, 2, 2);
        assertEnum(YuanConfig.K_BINDING_MODE, 0, 3, 1);
        assertInteger(YuanConfig.K_MAX_ATTACK_TARGETS, 1, 4096, 512);
        assertInteger(YuanConfig.K_RECALL_GRACE_TICKS, 0, 1200, 40);
        assertInteger(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS, 0, 200, 20);
        assertInteger(YuanConfig.K_RIGHT_LIGHTNING_COUNT, 1, 128, 1);
        assert YuanConfigCatalog.byKey(YuanConfig.K_RIGHT_LIGHTNING_ENABLED) != null;
        assert YuanConfigCatalog.byKey(YuanConfig.K_RIGHT_LIGHTNING_SOUND) != null;
        assert YuanConfigCatalog.byKey(YuanConfig.K_RIGHT_LIGHTNING_SPREAD).max() == 128;
        assert YuanConfigCatalog.byKey(YuanConfig.K_RIGHT_LIGHTNING_COUNT_MODE).warning().contains("262,144");
        assert "按命中数".equals(YuanConfigCatalog.valueLabel(YuanConfig.K_RIGHT_LIGHTNING_COUNT_MODE, 1));
        var attackAttribute = YuanConfigCatalog.byKey(YuanConfig.K_ATTACK_ATTRIBUTE_MODE);
        assert YuanConfigScreen.enumValueAt(attackAttribute, 139, 0, 140) == 2;
        var reentry = YuanConfigCatalog.byKey(YuanConfig.K_ABSOLUTE_REENTRY);
        assert YuanConfigScreen.enumValueAt(reentry, 70, 0, 140) == 1;

        YuanConfigHistory enumHistory = new YuanConfigHistory(new CompoundTag());
        enumHistory.setInt(YuanConfig.K_ABSOLUTE_REENTRY, 2);
        assert enumHistory.draft().contains(YuanConfig.K_ABSOLUTE_REENTRY, Tag.TAG_INT);
        enumHistory.resetCategory("攻击");
        assert enumHistory.draft().contains(YuanConfig.K_ABSOLUTE_REENTRY, Tag.TAG_INT);
        assert enumHistory.draft().getInt(YuanConfig.K_ABSOLUTE_REENTRY) == 0;

        CompoundTag invalidHistoryInput = new CompoundTag();
        invalidHistoryInput.putFloat(YuanConfig.K_RELEASE_MODE, 2);
        invalidHistoryInput.putString("unknownHistoryKey", "discard");
        YuanConfigHistory strictHistory = new YuanConfigHistory(invalidHistoryInput);
        assert !strictHistory.draft().contains(YuanConfig.K_RELEASE_MODE);
        assert !strictHistory.draft().contains("unknownHistoryKey");
        CompoundTag invalidReplacement = new CompoundTag();
        invalidReplacement.putString(YuanConfig.K_DEFENSE_SCOPE, "wrong type");
        invalidReplacement.putInt("unknownReplacementKey", 1);
        strictHistory.replace(invalidReplacement);
        assert !strictHistory.draft().contains(YuanConfig.K_DEFENSE_SCOPE);
        assert !strictHistory.draft().contains("unknownReplacementKey");

        strictHistory.setInt(YuanConfig.K_MAX_ATTACK_TARGETS, 1000);
        strictHistory.setInt(YuanConfig.K_RECALL_GRACE_TICKS, 60);
        strictHistory.setInt(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS, 30);
        assert strictHistory.draft().contains(YuanConfig.K_MAX_ATTACK_TARGETS, Tag.TAG_INT);
        assert strictHistory.draft().contains(YuanConfig.K_RECALL_GRACE_TICKS, Tag.TAG_INT);
        assert strictHistory.draft().contains(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS, Tag.TAG_INT);
        strictHistory.resetCategory("范围能力");
        strictHistory.resetCategory("绑定");
        assert strictHistory.draft().contains(YuanConfig.K_MAX_ATTACK_TARGETS, Tag.TAG_INT);
        assert strictHistory.draft().contains(YuanConfig.K_RECALL_GRACE_TICKS, Tag.TAG_INT);
        assert strictHistory.draft().contains(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS, Tag.TAG_INT);
        assert strictHistory.draft().getInt(YuanConfig.K_MAX_ATTACK_TARGETS) == 512;
        assert strictHistory.draft().getInt(YuanConfig.K_RECALL_GRACE_TICKS) == 40;
        assert strictHistory.draft().getInt(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS) == 20;

        YuanConfigScreen.commitNumericValue(strictHistory,
                YuanConfigCatalog.byKey(YuanConfig.K_MAX_ATTACK_TARGETS), 777.4f);
        assert strictHistory.draft().contains(YuanConfig.K_MAX_ATTACK_TARGETS, Tag.TAG_INT);
        assert strictHistory.draft().getInt(YuanConfig.K_MAX_ATTACK_TARGETS) == 777;

        CompoundTag previewDraft = strictHistory.draft();
        CompoundTag preview = YuanConfigScreen.buildPreviewConfig(previewDraft,
                Map.of(YuanConfig.K_RECALL_GRACE_TICKS, 75f));
        assert preview.contains(YuanConfig.K_MAX_ATTACK_TARGETS, Tag.TAG_INT);
        assert preview.contains(YuanConfig.K_RECALL_GRACE_TICKS, Tag.TAG_INT);
        assert preview.getInt(YuanConfig.K_RECALL_GRACE_TICKS) == 75;

        CompoundTag unsaved = preview.copy();
        unsaved.putString("unknownSaveKey", "discard");
        unsaved.putFloat(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS, 10);
        CompoundTag payload = YuanConfigScreen.buildSavePayload(unsaved);
        assert !payload.contains("unknownSaveKey");
        assert !payload.contains(YuanConfig.K_BINDING_DEFENSE_GRACE_TICKS);
        assert payload.contains(YuanConfig.K_MAX_ATTACK_TARGETS, Tag.TAG_INT);
        assert payload.contains(YuanConfig.K_RECALL_GRACE_TICKS, Tag.TAG_INT);

        CompoundTag historyBase = YuanConfig.sanitize(stack.getOrCreateTagElement("YuanConfig").copy());
        YuanConfigHistory history = new YuanConfigHistory(historyBase);
        history.setFloat(YuanConfig.K_REACH, 16);
        assert history.changedKeys().contains(YuanConfig.K_REACH);
        history.undo();
        assert history.draft().getFloat(YuanConfig.K_REACH) == historyBase.getFloat(YuanConfig.K_REACH);
        history.redo();
        assert history.draft().getFloat(YuanConfig.K_REACH) == 16;
        history.resetCategory("数值");
        assert history.draft().getFloat(YuanConfig.K_REACH) == 10;

        YuanClientPreferences prefs = new YuanClientPreferences();
        prefs.detail = YuanConfigCatalog.Detail.REFERENCE;
        prefs.autoSave = true;
        prefs.favorites.add(YuanConfig.K_REACH);
        YuanClientPreferences restored = YuanClientPreferences.fromJson(prefs.toJson());
        assert restored.detail == YuanConfigCatalog.Detail.REFERENCE && restored.autoSave;
        assert restored.favorites.contains(YuanConfig.K_REACH);
        assert YuanClientPreferences.saveCheck(prefs, true);
        assert !YuanClientPreferences.saveCheck(prefs, false)
                : "preference save must report writer failure";

        String exported = YuanPresetManager.toJson(history.draft());
        YuanPresetManager.ImportResult imported = YuanPresetManager.parse(exported, history.original());
        assert imported.unknownKeys().isEmpty();
        assert imported.config().contains(YuanConfig.K_REACH);

        String partial = "{\"values\":{\"" + YuanConfig.K_REACH + "\":20}}";
        YuanPresetManager.ImportResult partialImport = YuanPresetManager.parse(partial, history.draft());
        assert partialImport.config().getFloat(YuanConfig.K_SPEED) == history.draft().getFloat(YuanConfig.K_SPEED)
                : "partial import must preserve omitted settings";
        history.replace(partialImport.config());
        history.markSaved();
        assert !history.isDirty() : "successful save must update the saved baseline";

        CompoundTag extended = history.draft();
        extended.putString("preset", "attack");
        YuanConfigHistory extendedHistory = new YuanConfigHistory(extended);
        assert !extendedHistory.draft().contains("preset")
                : "unknown config fields must be discarded";

        CompoundTag dependencies = extendedHistory.draft();
        for (int mode = 0; mode < 4; mode++) {
            dependencies.putInt(YuanConfig.K_RELEASE_MODE, mode);
            dependencies.putBoolean(YuanConfig.K_RIGHT_AOE, false);
            dependencies.putBoolean(YuanConfig.K_WORLD_KILL, true);
            assert YuanConfigCatalog.releaseEnabled(dependencies, 2) == ((mode & 2) != 0);
            assert YuanConfigCatalog.enabled(YuanConfig.K_WORLD_KILL, dependencies) == ((mode & 2) != 0);
            assert YuanConfigCatalog.enabled(YuanConfig.K_AOE_RANGE, dependencies) == ((mode & 2) != 0);
            dependencies.putBoolean(YuanConfig.K_WORLD_KILL, false);
            assert !YuanConfigCatalog.enabled(YuanConfig.K_AOE_RANGE, dependencies);
            dependencies.putBoolean(YuanConfig.K_RIGHT_AOE, true);
            assert YuanConfigCatalog.enabled(YuanConfig.K_AOE_RANGE, dependencies);
        }
        dependencies.putBoolean(YuanConfig.K_RIGHT_AOE, false);
        dependencies.putBoolean(YuanConfig.K_WORLD_KILL, true);
        dependencies.putInt(YuanConfig.K_RELEASE_MODE, 1);
        assert YuanConfigCatalog.enabled(YuanConfig.K_CORRIDOR_DISTANCE, dependencies);
        assert YuanConfigCatalog.enabled(YuanConfig.K_CORRIDOR_RADIUS, dependencies);
        assert YuanConfigCatalog.enabled(YuanConfig.K_CORRIDOR_BLOCK_CLIP, dependencies);
        assert YuanConfigCatalog.enabled(YuanConfig.K_PURGE_RANGE, dependencies);
        assert YuanConfigCatalog.byKey(YuanConfig.K_WORLD_KILL).dangerous();
        assert "tint".equals(YuanConfigCatalog.byKey(YuanConfig.K_GLASS_TINT_R).group());

        assert YuanConfigCatalog.categories().contains("范围能力");
        assert YuanConfigCatalog.categories().contains("防御");
        assert YuanConfigCatalog.categories().contains("绑定");
        assert YuanConfigCatalog.find("corridor").stream()
                .anyMatch(s -> s.key().equals(YuanConfig.K_CORRIDOR_DISTANCE));
        assert YuanConfigCatalog.find("attack attribute").stream()
                .anyMatch(s -> s.key().equals(YuanConfig.K_ATTACK_ATTRIBUTE_MODE));
        assert YuanConfigCatalog.find("release mode").stream()
                .anyMatch(s -> s.key().equals(YuanConfig.K_RELEASE_MODE));
        assert YuanConfigCatalog.find("defense scope").stream()
                .anyMatch(s -> s.key().equals(YuanConfig.K_DEFENSE_SCOPE));
        assert YuanConfigCatalog.find("binding mode").stream()
                .anyMatch(s -> s.key().equals(YuanConfig.K_BINDING_MODE));
        assert YuanConfigCatalog.find("reentry").stream()
                .anyMatch(s -> s.key().equals(YuanConfig.K_ABSOLUTE_REENTRY));

        CompoundTag disabled = dependencies.copy();
        disabled.putInt(YuanConfig.K_RELEASE_MODE, 0);
        disabled.putBoolean(YuanConfig.K_RIGHT_AOE, false);
        disabled.putBoolean(YuanConfig.K_INVINCIBLE, false);
        disabled.putInt(YuanConfig.K_DEFENSE_SCOPE, 0);
        disabled.putInt(YuanConfig.K_BINDING_MODE, 0);
        assert !YuanConfigCatalog.enabled(YuanConfig.K_CORRIDOR_DISTANCE, disabled);
        assert !YuanConfigCatalog.enabled(YuanConfig.K_CORRIDOR_RADIUS, disabled);
        assert !YuanConfigCatalog.enabled(YuanConfig.K_CORRIDOR_BLOCK_CLIP, disabled);
        assert !YuanConfigCatalog.enabled(YuanConfig.K_PURGE_RANGE, disabled);
        disabled.putInt(YuanConfig.K_RELEASE_MODE, 2);
        assert !YuanConfigCatalog.enabled(YuanConfig.K_CORRIDOR_DISTANCE, disabled);
        disabled.putInt(YuanConfig.K_RELEASE_MODE, 3);
        assert YuanConfigCatalog.enabled(YuanConfig.K_CORRIDOR_DISTANCE, disabled);
        disabled.putBoolean(YuanConfig.K_DEFENSE_BLOCKING, true);
        assert YuanConfigCatalog.enabled(YuanConfig.K_DEFENSE_BLOCKING, disabled);
        assert YuanConfigCatalog.enabled(YuanConfig.K_DEFENSE_FIRE, disabled);
        disabled.putBoolean(YuanConfig.K_DEFENSE_BLOCKING, false);
        assert !YuanConfigCatalog.enabled(YuanConfig.K_DEFENSE_FIRE, disabled);
        assert !YuanConfigCatalog.enabled(YuanConfig.K_DEFENSE_VOID, disabled);
        assert !YuanConfigCatalog.enabled(YuanConfig.K_ALLOW_MANUAL_DROP, disabled);
        assert !YuanConfigCatalog.enabled(YuanConfig.K_AUTO_RECALL, disabled);
        assert YuanConfigCatalog.enabled(YuanConfig.K_DROP_CAN_DESPAWN, disabled);
        assert YuanConfigCatalog.enabled(YuanConfig.K_DROP_DAMAGE_PROTECTION, disabled);
        assert YuanConfigCatalog.enabled(YuanConfig.K_DROP_VOID_RESCUE, disabled);

        assert YuanConfigCatalog.byKey(YuanConfig.K_ATTACK_PLAYERS).dangerous();
        assert YuanConfigCatalog.byKey(YuanConfig.K_ABSOLUTE_REENTRY).dangerous();
        assert YuanConfigCatalog.byKey(YuanConfig.K_ATTACK_ATTRIBUTE_MODE).dangerous();
        assert YuanConfigCatalog.byKey(YuanConfig.K_WORLD_KILL).dangerous();
        assert YuanConfigCatalog.byKey(YuanConfig.K_BINDING_MODE).dangerous();
        assert YuanConfigCatalog.byKey(YuanConfig.K_BAN_LIST).dangerous();
        assert YuanConfigCatalog.byKey(YuanConfig.K_BAN_PERSIST).dangerous();
        assert !YuanConfigCatalog.byKey(YuanConfig.K_DROP_DAMAGE_PROTECTION).dangerous();
        assert "攻击属性".equals(YuanConfigCatalog.byKey(YuanConfig.K_ATTACK_ATTRIBUTE_MODE).label());
        assert "释放模式".equals(YuanConfigCatalog.byKey(YuanConfig.K_RELEASE_MODE).label());
        assert "最高防御作用域".equals(YuanConfigCatalog.byKey(YuanConfig.K_DEFENSE_SCOPE).label());
        assert "绑定模式".equals(YuanConfigCatalog.byKey(YuanConfig.K_BINDING_MODE).label());
        assert "绝对重入策略".equals(YuanConfigCatalog.byKey(YuanConfig.K_ABSOLUTE_REENTRY).label());
        assert "无限".equals(YuanConfigCatalog.valueLabel(YuanConfig.K_ATTACK_ATTRIBUTE_MODE, 2));
        assert "立即与蓄力".equals(YuanConfigCatalog.valueLabel(YuanConfig.K_RELEASE_MODE, 3));
        assert "原生背包".equals(YuanConfigCatalog.valueLabel(YuanConfig.K_DEFENSE_SCOPE, 2));
        assert "绝对绑定".equals(YuanConfigCatalog.valueLabel(YuanConfig.K_BINDING_MODE, 3));
        assert "永久".equals(YuanConfigCatalog.valueLabel(YuanConfig.K_ABSOLUTE_REENTRY, 2));
        assert YuanConfigCatalog.byKey(YuanConfig.K_MAX_ATTACK_TARGETS).kind() == YuanConfigCatalog.Kind.INTEGER;
        assert YuanConfigCatalog.byKey(YuanConfig.K_RECALL_GRACE_TICKS).kind() == YuanConfigCatalog.Kind.INTEGER;
        assert YuanSwordItem.AttackMode.values().length == 5;
        assert YuanSwordItem.AttackMode.ABSOLUTE.getDisplayName().contains("绝对");

        assert "保留10%".equals(YuanConfigCatalog.valueLabel(YuanConfig.K_KILL_STRENGTH, 2));

        Set<String> changedKeys = Set.of(YuanConfig.K_GLASS_TINT_G);
        assert YuanConfigCatalog.visibleSettings("攻击", "攻击距离", false, Set.of(), Set.of()).stream()
                .anyMatch(s -> s.key().equals(YuanConfig.K_REACH));
        assert YuanConfigCatalog.visibleSettings("收藏", "", false, Set.of(YuanConfig.K_GLASS_TINT_G), Set.of()).stream()
                .anyMatch(s -> s.key().equals(YuanConfig.K_GLASS_TINT_R));
        assert YuanConfigCatalog.visibleSettings("液态玻璃", "", true, Set.of(), changedKeys).stream()
                .anyMatch(s -> s.key().equals(YuanConfig.K_GLASS_TINT_R));
        assert YuanConfigCatalog.visibleSettings("数值", "", true, Set.of(), Set.of(YuanConfig.K_REACH)).stream()
                .allMatch(s -> s.key().equals(YuanConfig.K_REACH));
        assert YuanConfigCatalog.visibleSettings("收藏", "", false, Set.of(YuanConfig.K_GLASS_TINT_G), Set.of()).stream()
                .filter(s -> s.group().equals("tint")).count() == 1;

        var tint = YuanConfigCatalog.byKey(YuanConfig.K_GLASS_TINT_R);
        assert YuanConfigCatalog.groupChanged(tint, Set.of(YuanConfig.K_GLASS_TINT_G));
        java.util.Set<String> groupFavorites = new java.util.HashSet<>();
        YuanConfigCatalog.toggleFavorite(tint, groupFavorites);
        assert YuanConfigCatalog.groupFavorite(tint, groupFavorites);
        assert groupFavorites.containsAll(Set.of(YuanConfig.K_GLASS_TINT_R,
                YuanConfig.K_GLASS_TINT_G, YuanConfig.K_GLASS_TINT_B));
        YuanConfigCatalog.toggleFavorite(tint, groupFavorites);
        assert !YuanConfigCatalog.groupFavorite(tint, groupFavorites) && groupFavorites.isEmpty();

        YuanConfigHistory groupReset = new YuanConfigHistory(new CompoundTag());
        groupReset.edit(tag -> {
            tag.putFloat(YuanConfig.K_GLASS_TINT_R, 10);
            tag.putFloat(YuanConfig.K_GLASS_TINT_G, 20);
            tag.putFloat(YuanConfig.K_GLASS_TINT_B, 30);
        });
        groupReset.resetGroup("tint");
        assert groupReset.draft().getFloat(YuanConfig.K_GLASS_TINT_R) == 0
                && groupReset.draft().getFloat(YuanConfig.K_GLASS_TINT_G) == 0
                && groupReset.draft().getFloat(YuanConfig.K_GLASS_TINT_B) == 0;
        groupReset.undo();
        assert groupReset.draft().getFloat(YuanConfig.K_GLASS_TINT_R) == 10
                && groupReset.draft().getFloat(YuanConfig.K_GLASS_TINT_G) == 20
                && groupReset.draft().getFloat(YuanConfig.K_GLASS_TINT_B) == 30;

        YuanConfigLayout mediumLayout = YuanConfigLayout.of(640, 360);
        assert !mediumLayout.wide() && !mediumLayout.narrow();
        assert mediumLayout.left().right() <= mediumLayout.center().left();
        assert mediumLayout.center().right() <= mediumLayout.footer().right();
        assert mediumLayout.right().width() == 0;

        assert com.yuan.network.ModeSwitchMessage.direction(1) == 1;
        assert com.yuan.network.ModeSwitchMessage.direction(-1) == -1;
        assert com.yuan.network.ModeSwitchMessage.direction(2) == 0;
        ItemStack modeStack = new ItemStack(Items.STICK);
        for (int i = 0; i < 4; i++) YuanSwordItem.nextMode(modeStack);
        assert YuanSwordItem.getMode(modeStack) == YuanSwordItem.AttackMode.ABSOLUTE;
        YuanSwordItem.nextMode(modeStack);
        assert YuanSwordItem.getMode(modeStack) == YuanSwordItem.AttackMode.ANNIHILATE;
        YuanSwordItem.prevMode(modeStack);
        assert YuanSwordItem.getMode(modeStack) == YuanSwordItem.AttackMode.ABSOLUTE;

        YuanConfigHistory grouped = new YuanConfigHistory(dependencies);
        grouped.setFloat(YuanConfig.K_REACH, grouped.draft().getFloat(YuanConfig.K_REACH));
        assert !grouped.canUndo() : "writing the same value must not create history";
        grouped.edit(tag -> {
            tag.putFloat(YuanConfig.K_GLASS_TINT_R, 10);
            tag.putFloat(YuanConfig.K_GLASS_TINT_G, 20);
            tag.putFloat(YuanConfig.K_GLASS_TINT_B, 30);
        });
        grouped.undo();
        assert grouped.draft().getFloat(YuanConfig.K_GLASS_TINT_R) != 10
                && grouped.draft().getFloat(YuanConfig.K_GLASS_TINT_G) != 20;

        boolean unsupported = false;
        try { YuanPresetManager.parse("{\"format\":2,\"values\":{}}", grouped.draft()); }
        catch (IllegalArgumentException expected) { unsupported = true; }
        assert unsupported : "unsupported preset format must be rejected";
        String dangerousJson = "{\"format\":1,\"values\":{\"" + YuanConfig.K_WORLD_KILL + "\":true}}";
        CompoundTag safeBase = grouped.draft();
        safeBase.putBoolean(YuanConfig.K_WORLD_KILL, false);
        YuanPresetManager.ImportResult danger = YuanPresetManager.parse(dangerousJson, safeBase);
        assert danger.dangerousKeys().contains(YuanConfig.K_WORLD_KILL);
        assert danger.preservedCount() > 0;
        boolean wrongType = false;
        try { YuanPresetManager.parse("{\"format\":1,\"values\":{\"" + YuanConfig.K_WORLD_KILL + "\":1}}", safeBase); }
        catch (IllegalArgumentException expected) { wrongType = true; }
        assert wrongType : "wrong JSON value types must be rejected";

        YuanSaveState saveState = new YuanSaveState();
        long firstRequest = saveState.begin(false);
        long secondRequest = saveState.begin(false);
        assert saveState.acknowledge(firstRequest, true, grouped.draft(), "")
                : "an unapplied earlier acknowledgement must be accepted";
        assert saveState.acknowledge(secondRequest, true, grouped.draft(), "");
        assert !saveState.acknowledge(firstRequest, true, grouped.draft(), "")
                : "acknowledgements older than the latest applied one must be ignored";
        assert saveState.status() == YuanSaveState.Status.SAVED;

        YuanConfigHistory reconciled = new YuanConfigHistory(grouped.draft());
        CompoundTag serverApplied = reconciled.draft();
        serverApplied.putFloat(YuanConfig.K_REACH, 12);
        reconciled.reconcile(serverApplied);
        assert !reconciled.isDirty() && reconciled.draft().getFloat(YuanConfig.K_REACH) == 12;
        reconciled.setFloat(YuanConfig.K_SPEED, 200);
        CompoundTag baselineOnly = reconciled.original();
        baselineOnly.putFloat(YuanConfig.K_REACH, 13);
        reconciled.markSaved(baselineOnly);
        assert reconciled.changedKeys().contains(YuanConfig.K_SPEED)
                : "edits made while a save is pending must remain dirty";

        YuanClientPreferences animationPrefs = new YuanClientPreferences();
        assert animationPrefs.animationPreset == YuanClientPreferences.AnimationPreset.SMOOTH;
        assert animationPrefs.animationEnabled(YuanClientPreferences.AnimationCategory.PANELS);
        animationPrefs.reduceMotion = true;
        assert !animationPrefs.animationEnabled(YuanClientPreferences.AnimationCategory.LISTS);
        assert animationPrefs.animationEnabled(YuanClientPreferences.AnimationCategory.SAVE_STATUS);

        YuanUiAnimation.Track track = new YuanUiAnimation.Track(0);
        track.to(10, 0, 100, YuanUiAnimation.Easing.SMOOTH);
        float halfway = track.value(50);
        track.to(20, 50, 100, YuanUiAnimation.Easing.SMOOTH);
        assert Math.abs(track.value(50) - halfway) < 0.001f : "interruption must continue from current value";
        assert track.value(150) == 20 : "animation must end at the exact target";

        YuanConfigLayout wideLayout = YuanConfigLayout.of(960, 540);
        assert wideLayout.wide() && wideLayout.left().right() <= wideLayout.center().left();
        assert wideLayout.center().right() <= wideLayout.right().left();
        YuanConfigLayout narrowLayout = YuanConfigLayout.of(480, 300);
        assert narrowLayout.narrow() && narrowLayout.center().top() >= narrowLayout.categories().bottom();
        assert narrowLayout.footer().top() >= narrowLayout.center().bottom();

        assert YuanConfigScreen.class.getDeclaredFields().length > 0;
        assert YuanConfigScreen.navigation().size() >= 3;
        assert YuanConfigScreen.lazyPageCheck(false, () -> { throw new AssertionError("eager page"); }) == null;
        assert YuanConfigScreen.lazyPageCheck(true, () -> "page").equals("page");

        String screenSource = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/yuan/client/gui/YuanConfigScreen.java"));
        assert screenSource.contains("if (preferencesSaved) return")
                : "normal close plus removed must not save preferences twice";
        assert YuanConfigScreen.worldAdjustWidgetVisible(true, true, false);
        assert !YuanConfigScreen.worldAdjustWidgetVisible(true, false, true);
        assert YuanConfigScreen.worldAdjustWidgetVisible(false, false, true);

        String composeScreen = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/kotlin/com/yuan/client/gui/YuanComposeTestScreen.kt"));
        assert !composeScreen.contains("id.startsWith(\"voronoiColor\") && id.length")
                : "length-gated voronoi dispatch silently drops the 14-character voronoiColor0R ids";
        assert composeScreen.contains("if (id.startsWith(\"voronoiColor\")) {")
                : "voronoi color sliders must dispatch by parsed slot and channel";
        assert composeScreen.contains("rest.startsWith(\"Glow\")")
                : "named voronoi glow/gap colors must map to their slots";
        assert composeScreen.contains("override fun removed()")
                : "the compose screen must release the surface it allocated for this open";
        assert !composeScreen.contains("skiaContext!!")
                : "a missing Skia GL context must degrade instead of crashing the game";
        assert !composeScreen.contains("YuanGodSwordConfigPacket(updated.toTag())")
                : "sending only toTag() drops the preset list, so presets never survived a relog";
        assert composeScreen.contains("savePreset") && composeScreen.contains("syncStackConfig()")
                : "saving a preset must reach the server-side stack, not just the client copy";

        for (String language : java.util.List.of("zh_cn.json", "en_us.json")) {
            String translations = java.nio.file.Files.readString(java.nio.file.Path.of(
                    "src/main/resources/assets/yuan/lang/" + language));
            assert translations.contains("\"key.categories.yuan\"") : language + " missing key category translation";
            assert translations.contains("\"key.yuan.config\"") : language + " missing config key translation";
        }

        YuanConfigLayout.RightControls right = YuanConfigLayout.rightControls(740, 27, 224, 0, 3);
        assert right.undo().top() == right.renderUndoY();
        assert right.localSave().top() == right.renderLocalButtonsY();
        assert right.animation().top() == right.renderAnimationY();
        assert right.maxScroll(300) > 0 : "right controls must expose enough scrolling for presets";

        YuanConfigLayout.Bounds switchBounds = YuanConfigLayout.switchBounds(300, 120);
        assert switchBounds.contains(400, 130);
        assert !switchBounds.contains(299, 130);
        YuanConfigLayout.Bounds favoriteBounds = YuanConfigLayout.favoriteBounds(200, 80);
        YuanConfigLayout.Bounds resetBounds = YuanConfigLayout.resetBounds(200, 80);
        assert !favoriteBounds.contains(210, 110) : "favorite action must not cover the whole row";
        assert !resetBounds.contains(210, 110) : "reset action must not cover the whole row";
        assert favoriteBounds.right() <= switchBounds.left() || favoriteBounds.bottom() <= switchBounds.top();

        YuanConfigLayout.Scrollbar centerBar = YuanConfigLayout.scrollbar(100, 300, 600, 150);
        assert centerBar.visible() && centerBar.thumbHeight() > 0;
        assert centerBar.thumbTop() > centerBar.trackTop();

        YuanScrollState scroll = new YuanScrollState();
        scroll.setMax(300);
        scroll.dragTo(200, 100, 300, 30);
        assert scroll.value() > 100 && scroll.value() < 200 : "thumb drag must map into scroll range";
        scroll.wheel(3, true);
        float beforeTick = scroll.value();
        scroll.update(true, .5f);
        assert scroll.value() != beforeTick : "animated wheel input must add inertia";
        for (int i = 0; i < 400; i++) scroll.update(true, .5f);
        assert scroll.value() >= 0 && scroll.value() <= 300 && Math.abs(scroll.velocity()) < 0.01f;
        scroll.wheel(2, false);
        assert scroll.velocity() == 0 : "reduced motion must disable inertia";

        assert YuanConfigLayout.glassPresetBounds(10, 20, 200, 0).size() == 7;
        assert YuanConfigLayout.glassPresetBounds(10, 20, 200, 1).size() == 7;

        String configPacket = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/yuan/network/ConfigSyncPacket.java"));
        assert !configPacket.contains("CompoundTag merged =")
                : "server sync must replace YuanConfig instead of retaining stale unknown fields";
        assert configPacket.contains("put(\"YuanConfig\", clean")
                : "sanitized payload must replace only the YuanConfig child tag";
    }

    private static void assertEnum(String key, float min, float max, float def) {
        YuanConfigCatalog.Setting setting = YuanConfigCatalog.byKey(key);
        assert setting != null && setting.kind() == YuanConfigCatalog.Kind.ENUM : key;
        assert setting.min() == min && setting.max() == max && setting.defaultValue() == def : key;
    }

    private static void assertInteger(String key, float min, float max, float def) {
        YuanConfigCatalog.Setting setting = YuanConfigCatalog.byKey(key);
        assert setting != null && setting.kind() == YuanConfigCatalog.Kind.INTEGER : key;
        assert setting.min() == min && setting.max() == max && setting.defaultValue() == def : key;
    }

    private static void allowRegistryInitialization() throws Exception {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError forgeNetworkBootstrapFailure) {
            // Forge 47.4.20 initializes registries before its standalone NetworkEvent bootstrap fails.
        }
    }
}
