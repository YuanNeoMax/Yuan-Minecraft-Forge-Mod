package com.yuan.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Per-sword NBT configuration for yuan_god_sword.
 * Kept separate from YuanConfig so the two weapons never share settings.
 */
public final class YuanGodSwordConfig {
    public static final String TAG = "YuanGodSwordConfig";
    /** Preset slots ride inside this item's own {@link #TAG} child tag. */
    public static final String PRESETS_KEY = "presets";

    public boolean enabled = true;
    public boolean invulnerable = true;
    public boolean grayScreen = true;
    public float grayStrength = 1.0f;
    public boolean grayAnimate = false;
    public int startAnim = 0;   // 0 burst, 1 shockwave, 2 none
    public int endAnim = 0;     // 0 collapse, 1 none
    public float startDuration = 0.4f;
    public float endDuration = 0.3f;
    public int ballColor = 0;   // 0..3 coral family swatches
    public boolean ballColorCustom = false;
    public int customColor = 0xFFFF7A5C;
    public float particleSize = 0.62f;
    public float stopDuration = 0.0f;
    public int triggerMode = 0;
    public boolean soundEnabled = true;
    public int cooldown = 30;
    public float soundVolume = 1.0f;
    public float particleAlpha = 0.5f;
    public int particleCount = 1;
    public float particleSpin = 0.3f;
    public boolean freezeSelf = false;
    public float stopRadius = 0.0f;
    public boolean freezeEntities = true;
    public boolean freezeBlocks = true;
    public boolean freezeFluids = true;
    public boolean freezeBossAI = true;
    public boolean showMessage = false;
    public int grayStyle = 0;
    public boolean rewindEnabled = true;
    public int rewindWindowSeconds = 10;
    public int rewindScopeMode = 0;          // 0 全维度, 1 当前维度
    public int rewindScope = 0;              // 0 全区块, 1 半径
    public float rewindRadius = 64f;
    public int rewindCooldownTicks = 600;    // 30 秒
    public int rewindPlaybackMode = 0;       // 0 渐进(动画), 1 瞬间
    public float rewindPlaybackSeconds = 2.5f;
    public int rewindRestoreOrder = 0;       // 0 方块->实体->世界, 1 实体->方块
    public int rewindCameraMode = 0;         // 0 跟随, 1 自由飞行
    public boolean rewindPositionRewind = false;
    public int rewindPositionMode = 0;       // 0 瞬间, 1 平滑
    public boolean rewindPlayerState = true;
    public boolean rewindDeathEnabled = true;
    public int rewindDeathCooldownTicks = 1200; // 60 秒
    public int rewindDeathMaxRetries = 0;    // 0 = 不限, 到窗口开头为止
    public boolean rewindSafetyCheckpoint = false;
    public boolean rewindHostileCheck = false;
    public boolean rewindOtherItemDeduct = true;
    public boolean rewindFreezeOthers = true;
    public boolean rewindTimestopStacking = false;
    public boolean rewindBlocks = true;
    public boolean rewindBlockEntities = true;
    public boolean rewindEntities = true;
    public boolean rewindItems = true;
    public boolean rewindExperience = true;
    public boolean rewindTime = true;
    public boolean rewindWeather = true;
    public boolean rewindRaids = true;
    public boolean rewindScoreboard = true;
    public boolean rewindWorldBorder = true;
    public boolean rewindFreeCamRestorePosition = true;
    public boolean rewindShowStats = true;

    // ---- 神剑空间斩 ----
    public boolean slashEnabled = true;
    public boolean slashDepthTest = true;
    public boolean slashRandomAngle = true;
    public boolean slashGlow = true;
    public float slashDuration = 0.26f;
    public float slashLengthMult = 2.8f;
    public float slashWidthRatio = 0.09f;
    public float slashThicknessRatio = 0.02f;
    public float slashTipFade = 0.08f;
    public float slashStartScale = 1.0f;
    public float slashEndScale = 1.0f;
    public float slashCoreWidth = 0.62f;
    public float slashCoreShade = 1.0f;
    public float slashEdgeWidth = 0.80f;
    public float slashEdgeBrightness = 1.0f;
    public float slashGlowWidth = 0.90f;
    public float slashGlowStrength = 1.0f;
    public float slashNoiseStrength = 1.0f;
    public float slashSweepSpeed = 0.35f;
    public float slashSweepSoftness = 0.15f;
    public float slashHoldFraction = 0.0f;
    public float slashFadeStart = 0.72f;
    public float slashFadeDuration = 0.28f;
    public float slashSurfaceOffset = 0.06f;
    public float slashRollRange = 180f;
    public int slashCoreColor = 0xFF161616;
    public int slashEdgeColor = 0xFFFFFFFF;
    public int slashGlowColor = 0xFFFFFFFF;

    // ---- 武器渲染 ----
    public int renderStyle = 0; // 0 星空, 1 丝绸, 2 原版, 3 关闭, 4 隧道, 5 晶格
    public int silkColor0 = 0xFF1A1423;
    public int silkColor1 = 0xFFB75D69;
    public int silkColor2 = 0xFFEACDC2;
    public int silkColor3 = 0xFFFFF5EB;
    public int silkColor4 = 0xFFFFF5EB;
    public int silkColor5 = 0xFFFFF5EB;
    public int silkColor6 = 0xFFFFF5EB;
    public int silkColor7 = 0xFFFFF5EB;
    public float silkBrightness = 0.03f;
    public float silkContrast = 1.19f;
    public float silkSaturation = 1.34f;
    public float silkScale = 1.74f;
    public float silkIntensity = 0.69f;
    public float silkWarp = 0.07f;
    public float silkDetail = 2.66f;
    public float silkHue = 0.0f;
    public float silkSeed = 9199f;
    public float silkRotation = 5.06f;
    public float silkDrift = 0.02f;
    public float silkVignette = 0.12f;
    public float silkBlur = 0.004f;
    public float silkGrain = 0.0f;

    // ---- 隧道渲染 ----
    public float tunnelSpeed = 1.0f;
    public float tunnelBrightness = 1.0f;
    public float tunnelDensity = 1.0f;
    public float tunnelFov = 6.0f;

    // ---- 晶格渲染（paper-design/shaders Voronoi，Apache-2.0）----
    public int voronoiColorCount = 1;
    public int voronoiStepsPerColor = 3;
    public int voronoiColor0 = 0xFFFF8247;
    public int voronoiColor1 = 0xFFFFE53D;
    public int voronoiColor2 = 0xFFFFE53D;
    public int voronoiColor3 = 0xFFFFE53D;
    public int voronoiColor4 = 0xFFFFE53D;
    public int voronoiColorGlow = 0xFFFFFFFF;
    public int voronoiColorGap = 0xFF2E0000;
    public float voronoiDistortion = 0.4f;
    public float voronoiGap = 0.04f;
    public float voronoiGlow = 0.0f;
    public float voronoiScale = 0.5f;
    public float voronoiFov = 6.0f;
    public float voronoiSpeed = 0.5f;
    public float voronoiRotation = 0.0f;
    public float voronoiOffsetX = 0.0f;
    public float voronoiOffsetY = 0.0f;

    // ---- 刀光星空 ----
    public boolean slashStarfield = false;
    public float slashStarDensity = 0.6f;
    public float slashStarBrightness = 0.8f;
    public float slashStarSize = 1.0f;
    public int slashStarColorMode = 0; // 0 黑白, 1 彩虹
    // ---- 界面设置 ----
    public int uiTabVariant = 0;      // 0 圆角, 1 方形
    public int uiTabSize = 1;         // 0 小, 1 中, 2 大
    public boolean uiTabCompact = false;
    public int uiSliderHeight = 1;    // 0 矮, 1 标准, 2 高
    public int uiSliderThumb = 1;     // 0 小, 1 标准, 2 大
    public int uiSearchStyle = 0;     // 0 经典, 1 展开
    public float uiSearchWidth = 200f;
    public float uiSearchHeight = 40f;
    public float uiSearchRadius = 20f;
    public int uiSwitchStyle = 0;     // 0 经典, 1 渐变翻转
    public float uiSwitchWidth = 62f;
    public float uiSwitchHeight = 28f;
    public float uiSwitchKnob = 22f;
    public int uiSwitchGradientA = 0xFFF19AF3;
    public int uiSwitchGradientB = 0xFFF099B5;
    public int uiSwitchTrackOff = 0xFFD7D7D7;
    public int uiSwitchSlideTrack = 0xFFFFFFFF;
    public int uiSwitchSlideOff = 0xFFCCCCCC;
    public int uiSwitchSlideOn = 0xFF59D102;
    public int uiSwitchTextTrackOff = 0xFF05012C;
    public int uiSwitchTextTrackOn = 0xFFFFB500;
    public int uiSwitchTextKnob = 0xFFFFFFFF;
    public int uiSwitchTextColor = 0xFF78768D;
    public String uiSwitchTextOn = "On";
    public String uiSwitchTextOff = "Off";

    // 领域半径同时决定：地面网格的真实截止、术式架尺寸、后处理强度衰减范围
    public float domainRadius = 20f;

    // 星渊地面：地形表面被星海替换。关闭时领域只剩其它层（天穹/术式架）。
    public boolean domainStarfield = true;

    /**
     * Every declared instance field, in declaration order. Used by the three pure in-memory
     * mirrors below so none of them has to name all 166 fields by hand -- a hand-written list is
     * exactly how a field gets forgotten when one is added.
     *
     * <p>read/write are deliberately NOT driven from here: they carry NBT persistence with
     * per-field clamp ranges and one legacy key fallback, which is data a reflective walk cannot
     * infer. See {@link #read} and {@link #write}.
     */
    private static final java.lang.reflect.Field[] FIELDS = instanceFields();

    private static java.lang.reflect.Field[] instanceFields() {
        java.util.List<java.lang.reflect.Field> out = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : YuanGodSwordConfig.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            if (field.isSynthetic()) continue;
            field.setAccessible(true);
            out.add(field);
        }
        return out.toArray(new java.lang.reflect.Field[0]);
    }

    /**
     * Restores every field to the value its own declaration initialises it to.
     *
     * <p>Expressed as "copy from a fresh instance" because that is literally what the previous
     * hand-written body did: its 166 assignments matched the field initialisers exactly (verified
     * by YuanGodSwordConfigCheck). Keeping one source of truth means a new field is covered by
     * both its declaration and this method, instead of needing a third edit.
     */
    public void reset() {
        copyFrom(new YuanGodSwordConfig());
    }

    public void copyFrom(YuanGodSwordConfig other) {
        for (java.lang.reflect.Field field : FIELDS) {
            try {
                field.set(this, field.get(other));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot copy " + field.getName(), e);
            }
        }
    }

    public boolean sameAs(YuanGodSwordConfig other) {
        for (java.lang.reflect.Field field : FIELDS) {
            try {
                if (!java.util.Objects.equals(field.get(this), field.get(other))) return false;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot compare " + field.getName(), e);
            }
        }
        return true;
    }

    public void read(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG);
        if (tag == null) return;

        // Fields with no special handling load by their declared type. read() is not fully
        // table-driven like write(): a clamp range is data that cannot be inferred from a
        // field, so the clamped fields stay written out below.
        for (java.lang.reflect.Field field : FIELDS) {
            String key = field.getName();
            if (!tag.contains(key)) continue;
            Class<?> type = field.getType();
            try {
                if (type == boolean.class) field.setBoolean(this, tag.getBoolean(key));
                else if (type == int.class) field.setInt(this, tag.getInt(key));
                else if (type == float.class) field.setFloat(this, tag.getFloat(key));
                else if (type == String.class) field.set(this, tag.getString(key));
                else throw new IllegalStateException("unhandled field type " + type + " for " + key);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot read " + key, e);
            }
        }

        if (tag.contains("grayStrength")) {
            grayStrength = clamp(tag.getFloat("grayStrength"), 0f, 1f);
        } else if (tag.contains("grayIntensity")) {
            grayStrength = 1f - clamp(tag.getFloat("grayIntensity"), 0f, 1f);
        }
        if (tag.contains("startDuration")) startDuration = clamp(tag.getFloat("startDuration"), 0.1f, 2f);
        if (tag.contains("endDuration")) endDuration = clamp(tag.getFloat("endDuration"), 0.1f, 2f);
        if (tag.contains("particleSize")) particleSize = clamp(tag.getFloat("particleSize"), 0.4f, 3f);
        if (tag.contains("stopDuration")) stopDuration = clamp(tag.getFloat("stopDuration"), 0f, 60f);
        if (tag.contains("soundVolume")) soundVolume = clamp(tag.getFloat("soundVolume"), 0f, 1f);
        if (tag.contains("particleAlpha")) particleAlpha = clamp(tag.getFloat("particleAlpha"), 0.1f, 1f);
        if (tag.contains("particleSpin")) particleSpin = clamp(tag.getFloat("particleSpin"), 0f, 3f);
        if (tag.contains("stopRadius")) stopRadius = clamp(tag.getFloat("stopRadius"), 0f, 128f);
        if (tag.contains("rewindWindowSeconds")) rewindWindowSeconds = (int) clamp(tag.getFloat("rewindWindowSeconds"), 1f, 600f);
        if (tag.contains("rewindScopeMode")) rewindScopeMode = Math.max(0, Math.min(1, tag.getInt("rewindScopeMode")));
        if (tag.contains("rewindScope")) rewindScope = Math.max(0, Math.min(1, tag.getInt("rewindScope")));
        if (tag.contains("rewindRadius")) rewindRadius = clamp(tag.getFloat("rewindRadius"), 0f, 1024f);
        if (tag.contains("rewindCooldownTicks")) rewindCooldownTicks = (int) clamp(tag.getFloat("rewindCooldownTicks"), 0f, 72000f);
        if (tag.contains("rewindPlaybackMode")) rewindPlaybackMode = Math.max(0, Math.min(1, tag.getInt("rewindPlaybackMode")));
        if (tag.contains("rewindPlaybackSeconds")) rewindPlaybackSeconds = clamp(tag.getFloat("rewindPlaybackSeconds"), 0.05f, 3600f);
        if (tag.contains("rewindRestoreOrder")) rewindRestoreOrder = Math.max(0, Math.min(1, tag.getInt("rewindRestoreOrder")));
        if (tag.contains("rewindCameraMode")) rewindCameraMode = Math.max(0, Math.min(1, tag.getInt("rewindCameraMode")));
        if (tag.contains("rewindPositionMode")) rewindPositionMode = Math.max(0, Math.min(1, tag.getInt("rewindPositionMode")));
        if (tag.contains("rewindDeathCooldownTicks")) rewindDeathCooldownTicks = (int) clamp(tag.getFloat("rewindDeathCooldownTicks"), 0f, 72000f);
        if (tag.contains("rewindDeathMaxRetries")) rewindDeathMaxRetries = Math.max(0, Math.min(100, tag.getInt("rewindDeathMaxRetries")));
        if (tag.contains("slashDuration")) slashDuration = clamp(tag.getFloat("slashDuration"), 0.1f, 1f);
        if (tag.contains("slashLengthMult")) slashLengthMult = clamp(tag.getFloat("slashLengthMult"), 0.5f, 5f);
        if (tag.contains("slashWidthRatio")) slashWidthRatio = clamp(tag.getFloat("slashWidthRatio"), 0.02f, 0.5f);
        if (tag.contains("slashThicknessRatio")) slashThicknessRatio = clamp(tag.getFloat("slashThicknessRatio"), 0.002f, 0.2f);
        if (tag.contains("slashTipFade")) slashTipFade = clamp(tag.getFloat("slashTipFade"), 0.02f, 0.5f);
        if (tag.contains("slashStartScale")) slashStartScale = clamp(tag.getFloat("slashStartScale"), 0.2f, 3f);
        if (tag.contains("slashEndScale")) slashEndScale = clamp(tag.getFloat("slashEndScale"), 0.2f, 3f);
        if (tag.contains("slashCoreWidth")) slashCoreWidth = clamp(tag.getFloat("slashCoreWidth"), 0.3f, 0.98f);
        if (tag.contains("slashCoreShade")) slashCoreShade = clamp(tag.getFloat("slashCoreShade"), 0f, 1f);
        if (tag.contains("slashEdgeWidth")) slashEdgeWidth = clamp(tag.getFloat("slashEdgeWidth"), 0.5f, 0.99f);
        if (tag.contains("slashEdgeBrightness")) slashEdgeBrightness = clamp(tag.getFloat("slashEdgeBrightness"), 0.1f, 2f);
        if (tag.contains("slashGlowWidth")) slashGlowWidth = clamp(tag.getFloat("slashGlowWidth"), 0.6f, 0.99f);
        if (tag.contains("slashGlowStrength")) slashGlowStrength = clamp(tag.getFloat("slashGlowStrength"), 0f, 3f);
        if (tag.contains("slashNoiseStrength")) slashNoiseStrength = clamp(tag.getFloat("slashNoiseStrength"), 0f, 2f);
        if (tag.contains("slashSweepSpeed")) slashSweepSpeed = clamp(tag.getFloat("slashSweepSpeed"), 0.05f, 0.95f);
        if (tag.contains("slashSweepSoftness")) slashSweepSoftness = clamp(tag.getFloat("slashSweepSoftness"), 0.02f, 0.6f);
        if (tag.contains("slashHoldFraction")) slashHoldFraction = clamp(tag.getFloat("slashHoldFraction"), 0f, 0.8f);
        if (tag.contains("slashFadeStart")) slashFadeStart = clamp(tag.getFloat("slashFadeStart"), 0.3f, 0.95f);
        if (tag.contains("slashFadeDuration")) slashFadeDuration = clamp(tag.getFloat("slashFadeDuration"), 0.02f, 0.7f);
        if (tag.contains("slashSurfaceOffset")) slashSurfaceOffset = clamp(tag.getFloat("slashSurfaceOffset"), 0f, 0.5f);
        if (tag.contains("slashRollRange")) slashRollRange = clamp(tag.getFloat("slashRollRange"), 0f, 360f);
        if (tag.contains("renderStyle")) renderStyle = Math.max(0, Math.min(5, tag.getInt("renderStyle")));
        if (tag.contains("silkBrightness")) silkBrightness = clamp(tag.getFloat("silkBrightness"), -1f, 1f);
        if (tag.contains("silkContrast")) silkContrast = clamp(tag.getFloat("silkContrast"), 0.1f, 3f);
        if (tag.contains("silkSaturation")) silkSaturation = clamp(tag.getFloat("silkSaturation"), 0f, 3f);
        if (tag.contains("silkScale")) silkScale = clamp(tag.getFloat("silkScale"), 0.2f, 8f);
        if (tag.contains("silkIntensity")) silkIntensity = clamp(tag.getFloat("silkIntensity"), 0f, 3f);
        if (tag.contains("silkWarp")) silkWarp = clamp(tag.getFloat("silkWarp"), 0f, 2f);
        if (tag.contains("silkDetail")) silkDetail = clamp(tag.getFloat("silkDetail"), 0.5f, 8f);
        if (tag.contains("silkHue")) silkHue = clamp(tag.getFloat("silkHue"), 0f, 12.566f);
        if (tag.contains("silkSeed")) silkSeed = clamp(tag.getFloat("silkSeed"), 0f, 20000f);
        if (tag.contains("silkRotation")) silkRotation = clamp(tag.getFloat("silkRotation"), -12.566f, 12.566f);
        if (tag.contains("silkDrift")) silkDrift = clamp(tag.getFloat("silkDrift"), 0f, 3f);
        if (tag.contains("silkVignette")) silkVignette = clamp(tag.getFloat("silkVignette"), 0f, 1f);
        if (tag.contains("silkBlur")) silkBlur = clamp(tag.getFloat("silkBlur"), 0f, 0.05f);
        if (tag.contains("silkGrain")) silkGrain = clamp(tag.getFloat("silkGrain"), 0f, 1f);
        if (tag.contains("tunnelSpeed")) tunnelSpeed = clamp(tag.getFloat("tunnelSpeed"), 0.1f, 3f);
        if (tag.contains("tunnelBrightness")) tunnelBrightness = clamp(tag.getFloat("tunnelBrightness"), 0.1f, 3f);
        if (tag.contains("tunnelDensity")) tunnelDensity = clamp(tag.getFloat("tunnelDensity"), 0.2f, 3f);
        if (tag.contains("tunnelFov")) tunnelFov = clamp(tag.getFloat("tunnelFov"), 1f, 16f);
        if (tag.contains("voronoiDistortion")) voronoiDistortion = clamp(tag.getFloat("voronoiDistortion"), 0f, 0.5f);
        if (tag.contains("voronoiGap")) voronoiGap = clamp(tag.getFloat("voronoiGap"), 0f, 0.1f);
        if (tag.contains("voronoiGlow")) voronoiGlow = clamp(tag.getFloat("voronoiGlow"), 0f, 1f);
        if (tag.contains("voronoiScale")) voronoiScale = clamp(tag.getFloat("voronoiScale"), 0.05f, 4f);
        if (tag.contains("voronoiFov")) voronoiFov = clamp(tag.getFloat("voronoiFov"), 1f, 16f);
        if (tag.contains("voronoiSpeed")) voronoiSpeed = clamp(tag.getFloat("voronoiSpeed"), 0f, 3f);
        if (tag.contains("voronoiRotation")) voronoiRotation = clamp(tag.getFloat("voronoiRotation"), -6.283f, 6.283f);
        if (tag.contains("voronoiOffsetX")) voronoiOffsetX = clamp(tag.getFloat("voronoiOffsetX"), -1f, 1f);
        if (tag.contains("voronoiOffsetY")) voronoiOffsetY = clamp(tag.getFloat("voronoiOffsetY"), -1f, 1f);
        if (tag.contains("slashStarDensity")) slashStarDensity = clamp(tag.getFloat("slashStarDensity"), 0.05f, 3f);
        if (tag.contains("slashStarBrightness")) slashStarBrightness = clamp(tag.getFloat("slashStarBrightness"), 0f, 3f);
        if (tag.contains("slashStarSize")) slashStarSize = clamp(tag.getFloat("slashStarSize"), 0.2f, 5f);
        if (tag.contains("uiSearchWidth")) uiSearchWidth = clamp(tag.getFloat("uiSearchWidth"), 120f, 280f);
        if (tag.contains("uiSearchHeight")) uiSearchHeight = clamp(tag.getFloat("uiSearchHeight"), 28f, 48f);
        if (tag.contains("uiSearchRadius")) uiSearchRadius = clamp(tag.getFloat("uiSearchRadius"), 0f, 24f);
        if (tag.contains("uiSwitchWidth")) uiSwitchWidth = clamp(tag.getFloat("uiSwitchWidth"), 54f, 80f);
        if (tag.contains("uiSwitchHeight")) uiSwitchHeight = clamp(tag.getFloat("uiSwitchHeight"), 24f, 32f);
        if (tag.contains("uiSwitchKnob")) uiSwitchKnob = clamp(tag.getFloat("uiSwitchKnob"), 16f, 24f);
        if (tag.contains("domainRadius")) domainRadius = clamp(tag.getFloat("domainRadius"), 4f, 128f);
        startAnim = Math.max(0, Math.min(2, startAnim));
        endAnim = Math.max(0, Math.min(1, endAnim));
        ballColor = Math.max(0, Math.min(7, ballColor));
        triggerMode = Math.max(0, Math.min(2, triggerMode));
        cooldown = Math.max(0, Math.min(100, cooldown));
        particleCount = Math.max(1, Math.min(16, particleCount));
        grayStyle = Math.max(0, Math.min(2, grayStyle));
        voronoiColorCount = Math.max(1, Math.min(10, voronoiColorCount));
        voronoiStepsPerColor = Math.max(1, Math.min(10, voronoiStepsPerColor));
        uiTabVariant = Math.max(0, Math.min(1, uiTabVariant));
        uiTabSize = Math.max(0, Math.min(2, uiTabSize));
        uiSliderHeight = Math.max(0, Math.min(2, uiSliderHeight));
        uiSliderThumb = Math.max(0, Math.min(2, uiSliderThumb));
        uiSearchStyle = Math.max(0, Math.min(1, uiSearchStyle));
        uiSwitchStyle = Math.max(0, Math.min(3, uiSwitchStyle));
    }

    public void write(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG);
        for (java.lang.reflect.Field field : FIELDS) {
            String key = field.getName();
            Class<?> type = field.getType();
            try {
                if (type == boolean.class) tag.putBoolean(key, field.getBoolean(this));
                else if (type == int.class) tag.putInt(key, field.getInt(this));
                else if (type == float.class) tag.putFloat(key, field.getFloat(this));
                else if (type == String.class) tag.putString(key, (String) field.get(this));
                else throw new IllegalStateException("unhandled field type " + type + " for " + key);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("cannot write " + key, e);
            }
        }
        // Legacy companion key: older builds stored the inverse. Kept so downgrading still reads
        // a sensible value; read() prefers grayStrength and falls back to this.
        tag.putFloat("grayIntensity", 1f - grayStrength);
    }

    public CompoundTag toTag() {
        ItemStack temp = new ItemStack(Items.AIR);
        write(temp);
        CompoundTag root = temp.getTagElement(TAG);
        return root == null ? new CompoundTag() : root.copy();
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min;
        }
        float wideMin = min >= 0f ? 0f : Math.min(min * 100f, min - 100f);
        float wideMax = Math.max(max * 100f, max + 100f);
        return Math.max(wideMin, Math.min(wideMax, value));
    }
}
