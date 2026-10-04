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

    public void reset() {
        enabled = true;
        invulnerable = true;
        grayScreen = true;
        grayStrength = 1.0f;
        grayAnimate = false;
        startAnim = 0;
        endAnim = 0;
        startDuration = 0.4f;
        endDuration = 0.3f;
        ballColor = 0;
        ballColorCustom = false;
        customColor = 0xFFFF7A5C;
        particleSize = 0.62f;
        stopDuration = 0.0f;
        triggerMode = 0;
        soundEnabled = true;
        cooldown = 30;
        soundVolume = 1.0f;
        particleAlpha = 0.5f;
        particleCount = 1;
        particleSpin = 0.3f;
        freezeSelf = false;
        stopRadius = 0.0f;
        freezeEntities = true;
        freezeBlocks = true;
        freezeFluids = true;
        freezeBossAI = true;
        showMessage = false;
        grayStyle = 0;
        rewindEnabled = true;
        rewindWindowSeconds = 10;
        rewindScopeMode = 0;
        rewindScope = 0;
        rewindRadius = 64f;
        rewindCooldownTicks = 600;
        rewindPlaybackMode = 0;
        rewindPlaybackSeconds = 2.5f;
        rewindRestoreOrder = 0;
        rewindCameraMode = 0;
        rewindPositionRewind = false;
        rewindPositionMode = 0;
        rewindPlayerState = true;
        rewindDeathEnabled = true;
        rewindDeathCooldownTicks = 1200;
        rewindDeathMaxRetries = 0;
        rewindSafetyCheckpoint = false;
        rewindHostileCheck = false;
        rewindOtherItemDeduct = true;
        rewindFreezeOthers = true;
        rewindTimestopStacking = false;
        rewindBlocks = true;
        rewindBlockEntities = true;
        rewindEntities = true;
        rewindItems = true;
        rewindExperience = true;
        rewindTime = true;
        rewindWeather = true;
        rewindRaids = true;
        rewindScoreboard = true;
        rewindWorldBorder = true;
        rewindFreeCamRestorePosition = true;
        rewindShowStats = true;
        slashEnabled = true;
        slashDepthTest = true;
        slashRandomAngle = true;
        slashGlow = true;
        slashDuration = 0.26f;
        slashLengthMult = 2.8f;
        slashWidthRatio = 0.09f;
        slashThicknessRatio = 0.02f;
        slashTipFade = 0.08f;
        slashStartScale = 1.0f;
        slashEndScale = 1.0f;
        slashCoreWidth = 0.62f;
        slashCoreShade = 1.0f;
        slashEdgeWidth = 0.80f;
        slashEdgeBrightness = 1.0f;
        slashGlowWidth = 0.90f;
        slashGlowStrength = 1.0f;
        slashNoiseStrength = 1.0f;
        slashSweepSpeed = 0.35f;
        slashSweepSoftness = 0.15f;
        slashHoldFraction = 0.0f;
        slashFadeStart = 0.72f;
        slashFadeDuration = 0.28f;
        slashSurfaceOffset = 0.06f;
        slashRollRange = 180f;
        slashCoreColor = 0xFF161616;
        slashEdgeColor = 0xFFFFFFFF;
        slashGlowColor = 0xFFFFFFFF;
        renderStyle = 0;
        silkColor0 = 0xFF1A1423;
        silkColor1 = 0xFFB75D69;
        silkColor2 = 0xFFEACDC2;
        silkColor3 = 0xFFFFF5EB;
        silkColor4 = 0xFFFFF5EB;
        silkColor5 = 0xFFFFF5EB;
        silkColor6 = 0xFFFFF5EB;
        silkColor7 = 0xFFFFF5EB;
        silkBrightness = 0.03f;
        silkContrast = 1.19f;
        silkSaturation = 1.34f;
        silkScale = 1.74f;
        silkIntensity = 0.69f;
        silkWarp = 0.07f;
        silkDetail = 2.66f;
        silkHue = 0.0f;
        silkSeed = 9199f;
        silkRotation = 5.06f;
        silkDrift = 0.02f;
        silkVignette = 0.12f;
        silkBlur = 0.004f;
        silkGrain = 0.0f;
        tunnelSpeed = 1.0f;
        tunnelBrightness = 1.0f;
        tunnelDensity = 1.0f;
        tunnelFov = 6.0f;
        voronoiColorCount = 1;
        voronoiStepsPerColor = 3;
        voronoiColor0 = 0xFFFF8247;
        voronoiColor1 = 0xFFFFE53D;
        voronoiColor2 = 0xFFFFE53D;
        voronoiColor3 = 0xFFFFE53D;
        voronoiColor4 = 0xFFFFE53D;
        voronoiColorGlow = 0xFFFFFFFF;
        voronoiColorGap = 0xFF2E0000;
        voronoiDistortion = 0.4f;
        voronoiGap = 0.04f;
        voronoiGlow = 0.0f;
        voronoiScale = 0.5f;
        voronoiFov = 6.0f;
        voronoiSpeed = 0.5f;
        voronoiRotation = 0.0f;
        voronoiOffsetX = 0.0f;
        voronoiOffsetY = 0.0f;
        slashStarfield = false;
        slashStarDensity = 0.6f;
        slashStarBrightness = 0.8f;
        slashStarSize = 1.0f;
        slashStarColorMode = 0;
        uiTabVariant = 0;
        uiTabSize = 1;
        uiTabCompact = false;
        uiSliderHeight = 1;
        uiSliderThumb = 1;
        uiSearchStyle = 0;
        uiSearchWidth = 200f;
        uiSearchHeight = 40f;
        uiSearchRadius = 20f;
        uiSwitchStyle = 0;
        uiSwitchWidth = 62f;
        uiSwitchHeight = 28f;
        uiSwitchKnob = 22f;
        uiSwitchGradientA = 0xFFF19AF3;
        uiSwitchGradientB = 0xFFF099B5;
        uiSwitchTrackOff = 0xFFD7D7D7;
        uiSwitchSlideTrack = 0xFFFFFFFF;
        uiSwitchSlideOff = 0xFFCCCCCC;
        uiSwitchSlideOn = 0xFF59D102;
        uiSwitchTextTrackOff = 0xFF05012C;
        uiSwitchTextTrackOn = 0xFFFFB500;
        uiSwitchTextKnob = 0xFFFFFFFF;
        uiSwitchTextColor = 0xFF78768D;
        uiSwitchTextOn = "On";
        uiSwitchTextOff = "Off";
    }

    public void copyFrom(YuanGodSwordConfig other) {
        enabled = other.enabled;
        invulnerable = other.invulnerable;
        grayScreen = other.grayScreen;
        grayStrength = other.grayStrength;
        grayAnimate = other.grayAnimate;
        startAnim = other.startAnim;
        endAnim = other.endAnim;
        startDuration = other.startDuration;
        endDuration = other.endDuration;
        ballColor = other.ballColor;
        ballColorCustom = other.ballColorCustom;
        customColor = other.customColor;
        particleSize = other.particleSize;
        stopDuration = other.stopDuration;
        triggerMode = other.triggerMode;
        soundEnabled = other.soundEnabled;
        cooldown = other.cooldown;
        soundVolume = other.soundVolume;
        particleAlpha = other.particleAlpha;
        particleCount = other.particleCount;
        particleSpin = other.particleSpin;
        freezeSelf = other.freezeSelf;
        stopRadius = other.stopRadius;
        freezeEntities = other.freezeEntities;
        freezeBlocks = other.freezeBlocks;
        freezeFluids = other.freezeFluids;
        freezeBossAI = other.freezeBossAI;
        showMessage = other.showMessage;
        grayStyle = other.grayStyle;
        rewindEnabled = other.rewindEnabled;
        rewindWindowSeconds = other.rewindWindowSeconds;
        rewindScopeMode = other.rewindScopeMode;
        rewindScope = other.rewindScope;
        rewindRadius = other.rewindRadius;
        rewindCooldownTicks = other.rewindCooldownTicks;
        rewindPlaybackMode = other.rewindPlaybackMode;
        rewindPlaybackSeconds = other.rewindPlaybackSeconds;
        rewindRestoreOrder = other.rewindRestoreOrder;
        rewindCameraMode = other.rewindCameraMode;
        rewindPositionRewind = other.rewindPositionRewind;
        rewindPositionMode = other.rewindPositionMode;
        rewindPlayerState = other.rewindPlayerState;
        rewindDeathEnabled = other.rewindDeathEnabled;
        rewindDeathCooldownTicks = other.rewindDeathCooldownTicks;
        rewindDeathMaxRetries = other.rewindDeathMaxRetries;
        rewindSafetyCheckpoint = other.rewindSafetyCheckpoint;
        rewindHostileCheck = other.rewindHostileCheck;
        rewindOtherItemDeduct = other.rewindOtherItemDeduct;
        rewindFreezeOthers = other.rewindFreezeOthers;
        rewindTimestopStacking = other.rewindTimestopStacking;
        rewindBlocks = other.rewindBlocks;
        rewindBlockEntities = other.rewindBlockEntities;
        rewindEntities = other.rewindEntities;
        rewindItems = other.rewindItems;
        rewindExperience = other.rewindExperience;
        rewindTime = other.rewindTime;
        rewindWeather = other.rewindWeather;
        rewindRaids = other.rewindRaids;
        rewindScoreboard = other.rewindScoreboard;
        rewindWorldBorder = other.rewindWorldBorder;
        rewindFreeCamRestorePosition = other.rewindFreeCamRestorePosition;
        rewindShowStats = other.rewindShowStats;
        slashEnabled = other.slashEnabled;
        slashDepthTest = other.slashDepthTest;
        slashRandomAngle = other.slashRandomAngle;
        slashGlow = other.slashGlow;
        slashDuration = other.slashDuration;
        slashLengthMult = other.slashLengthMult;
        slashWidthRatio = other.slashWidthRatio;
        slashThicknessRatio = other.slashThicknessRatio;
        slashTipFade = other.slashTipFade;
        slashStartScale = other.slashStartScale;
        slashEndScale = other.slashEndScale;
        slashCoreWidth = other.slashCoreWidth;
        slashCoreShade = other.slashCoreShade;
        slashEdgeWidth = other.slashEdgeWidth;
        slashEdgeBrightness = other.slashEdgeBrightness;
        slashGlowWidth = other.slashGlowWidth;
        slashGlowStrength = other.slashGlowStrength;
        slashNoiseStrength = other.slashNoiseStrength;
        slashSweepSpeed = other.slashSweepSpeed;
        slashSweepSoftness = other.slashSweepSoftness;
        slashHoldFraction = other.slashHoldFraction;
        slashFadeStart = other.slashFadeStart;
        slashFadeDuration = other.slashFadeDuration;
        slashSurfaceOffset = other.slashSurfaceOffset;
        slashRollRange = other.slashRollRange;
        slashCoreColor = other.slashCoreColor;
        slashEdgeColor = other.slashEdgeColor;
        slashGlowColor = other.slashGlowColor;
        renderStyle = other.renderStyle;
        silkColor0 = other.silkColor0;
        silkColor1 = other.silkColor1;
        silkColor2 = other.silkColor2;
        silkColor3 = other.silkColor3;
        silkColor4 = other.silkColor4;
        silkColor5 = other.silkColor5;
        silkColor6 = other.silkColor6;
        silkColor7 = other.silkColor7;
        silkBrightness = other.silkBrightness;
        silkContrast = other.silkContrast;
        silkSaturation = other.silkSaturation;
        silkScale = other.silkScale;
        silkIntensity = other.silkIntensity;
        silkWarp = other.silkWarp;
        silkDetail = other.silkDetail;
        silkHue = other.silkHue;
        silkSeed = other.silkSeed;
        silkRotation = other.silkRotation;
        silkDrift = other.silkDrift;
        silkVignette = other.silkVignette;
        silkBlur = other.silkBlur;
        silkGrain = other.silkGrain;
        tunnelSpeed = other.tunnelSpeed;
        tunnelBrightness = other.tunnelBrightness;
        tunnelDensity = other.tunnelDensity;
        tunnelFov = other.tunnelFov;
        voronoiColorCount = other.voronoiColorCount;
        voronoiStepsPerColor = other.voronoiStepsPerColor;
        voronoiColor0 = other.voronoiColor0;
        voronoiColor1 = other.voronoiColor1;
        voronoiColor2 = other.voronoiColor2;
        voronoiColor3 = other.voronoiColor3;
        voronoiColor4 = other.voronoiColor4;
        voronoiColorGlow = other.voronoiColorGlow;
        voronoiColorGap = other.voronoiColorGap;
        voronoiDistortion = other.voronoiDistortion;
        voronoiGap = other.voronoiGap;
        voronoiGlow = other.voronoiGlow;
        voronoiScale = other.voronoiScale;
        voronoiFov = other.voronoiFov;
        voronoiSpeed = other.voronoiSpeed;
        voronoiRotation = other.voronoiRotation;
        voronoiOffsetX = other.voronoiOffsetX;
        voronoiOffsetY = other.voronoiOffsetY;
        slashStarfield = other.slashStarfield;
        slashStarDensity = other.slashStarDensity;
        slashStarBrightness = other.slashStarBrightness;
        slashStarSize = other.slashStarSize;
        slashStarColorMode = other.slashStarColorMode;
        uiTabVariant = other.uiTabVariant;
        uiTabSize = other.uiTabSize;
        uiTabCompact = other.uiTabCompact;
        uiSliderHeight = other.uiSliderHeight;
        uiSliderThumb = other.uiSliderThumb;
        uiSearchStyle = other.uiSearchStyle;
        uiSearchWidth = other.uiSearchWidth;
        uiSearchHeight = other.uiSearchHeight;
        uiSearchRadius = other.uiSearchRadius;
        uiSwitchStyle = other.uiSwitchStyle;
        uiSwitchWidth = other.uiSwitchWidth;
        uiSwitchHeight = other.uiSwitchHeight;
        uiSwitchKnob = other.uiSwitchKnob;
        uiSwitchGradientA = other.uiSwitchGradientA;
        uiSwitchGradientB = other.uiSwitchGradientB;
        uiSwitchTrackOff = other.uiSwitchTrackOff;
        uiSwitchSlideTrack = other.uiSwitchSlideTrack;
        uiSwitchSlideOff = other.uiSwitchSlideOff;
        uiSwitchSlideOn = other.uiSwitchSlideOn;
        uiSwitchTextTrackOff = other.uiSwitchTextTrackOff;
        uiSwitchTextTrackOn = other.uiSwitchTextTrackOn;
        uiSwitchTextKnob = other.uiSwitchTextKnob;
        uiSwitchTextColor = other.uiSwitchTextColor;
        uiSwitchTextOn = other.uiSwitchTextOn;
        uiSwitchTextOff = other.uiSwitchTextOff;
    }

    public boolean sameAs(YuanGodSwordConfig other) {
        return enabled == other.enabled
                && invulnerable == other.invulnerable
                && grayScreen == other.grayScreen
                && grayStrength == other.grayStrength
                && grayAnimate == other.grayAnimate
                && startAnim == other.startAnim
                && endAnim == other.endAnim
                && startDuration == other.startDuration
                && endDuration == other.endDuration
                && ballColor == other.ballColor
                && ballColorCustom == other.ballColorCustom
                && customColor == other.customColor
                && particleSize == other.particleSize
                && stopDuration == other.stopDuration
                && triggerMode == other.triggerMode
                && soundEnabled == other.soundEnabled
                && cooldown == other.cooldown
                && soundVolume == other.soundVolume
                && particleAlpha == other.particleAlpha
                && particleCount == other.particleCount
                && particleSpin == other.particleSpin
                && freezeSelf == other.freezeSelf
                && stopRadius == other.stopRadius
                && freezeEntities == other.freezeEntities
                && freezeBlocks == other.freezeBlocks
                && freezeFluids == other.freezeFluids
                && freezeBossAI == other.freezeBossAI
                && showMessage == other.showMessage
                && grayStyle == other.grayStyle
                && rewindEnabled == other.rewindEnabled
                && rewindWindowSeconds == other.rewindWindowSeconds
                && rewindScopeMode == other.rewindScopeMode
                && rewindScope == other.rewindScope
                && rewindRadius == other.rewindRadius
                && rewindCooldownTicks == other.rewindCooldownTicks
                && rewindPlaybackMode == other.rewindPlaybackMode
                && rewindPlaybackSeconds == other.rewindPlaybackSeconds
                && rewindRestoreOrder == other.rewindRestoreOrder
                && rewindCameraMode == other.rewindCameraMode
                && rewindPositionRewind == other.rewindPositionRewind
                && rewindPositionMode == other.rewindPositionMode
                && rewindPlayerState == other.rewindPlayerState
                && rewindDeathEnabled == other.rewindDeathEnabled
                && rewindDeathCooldownTicks == other.rewindDeathCooldownTicks
                && rewindDeathMaxRetries == other.rewindDeathMaxRetries
                && rewindSafetyCheckpoint == other.rewindSafetyCheckpoint
                && rewindHostileCheck == other.rewindHostileCheck
                && rewindOtherItemDeduct == other.rewindOtherItemDeduct
                && rewindFreezeOthers == other.rewindFreezeOthers
                && rewindTimestopStacking == other.rewindTimestopStacking
                && rewindBlocks == other.rewindBlocks
                && rewindBlockEntities == other.rewindBlockEntities
                && rewindEntities == other.rewindEntities
                && rewindItems == other.rewindItems
                && rewindExperience == other.rewindExperience
                && rewindTime == other.rewindTime
                && rewindWeather == other.rewindWeather
                && rewindRaids == other.rewindRaids
                && rewindScoreboard == other.rewindScoreboard
                && rewindWorldBorder == other.rewindWorldBorder
                && rewindFreeCamRestorePosition == other.rewindFreeCamRestorePosition
                && rewindShowStats == other.rewindShowStats
                && slashEnabled == other.slashEnabled
                && slashDepthTest == other.slashDepthTest
                && slashRandomAngle == other.slashRandomAngle
                && slashGlow == other.slashGlow
                && slashDuration == other.slashDuration
                && slashLengthMult == other.slashLengthMult
                && slashWidthRatio == other.slashWidthRatio
                && slashThicknessRatio == other.slashThicknessRatio
                && slashTipFade == other.slashTipFade
                && slashStartScale == other.slashStartScale
                && slashEndScale == other.slashEndScale
                && slashCoreWidth == other.slashCoreWidth
                && slashCoreShade == other.slashCoreShade
                && slashEdgeWidth == other.slashEdgeWidth
                && slashEdgeBrightness == other.slashEdgeBrightness
                && slashGlowWidth == other.slashGlowWidth
                && slashGlowStrength == other.slashGlowStrength
                && slashNoiseStrength == other.slashNoiseStrength
                && slashSweepSpeed == other.slashSweepSpeed
                && slashSweepSoftness == other.slashSweepSoftness
                && slashHoldFraction == other.slashHoldFraction
                && slashFadeStart == other.slashFadeStart
                && slashFadeDuration == other.slashFadeDuration
                && slashSurfaceOffset == other.slashSurfaceOffset
                && slashRollRange == other.slashRollRange
                && slashCoreColor == other.slashCoreColor
                && slashEdgeColor == other.slashEdgeColor
                && slashGlowColor == other.slashGlowColor
                && renderStyle == other.renderStyle
                && silkColor0 == other.silkColor0
                && silkColor1 == other.silkColor1
                && silkColor2 == other.silkColor2
                && silkColor3 == other.silkColor3
                && silkColor4 == other.silkColor4
                && silkColor5 == other.silkColor5
                && silkColor6 == other.silkColor6
                && silkColor7 == other.silkColor7
                && silkBrightness == other.silkBrightness
                && silkContrast == other.silkContrast
                && silkSaturation == other.silkSaturation
                && silkScale == other.silkScale
                && silkIntensity == other.silkIntensity
                && silkWarp == other.silkWarp
                && silkDetail == other.silkDetail
                && silkHue == other.silkHue
                && silkSeed == other.silkSeed
                && silkRotation == other.silkRotation
                && silkDrift == other.silkDrift
                && silkVignette == other.silkVignette
                && silkBlur == other.silkBlur
                && silkGrain == other.silkGrain
                && tunnelSpeed == other.tunnelSpeed
                && tunnelBrightness == other.tunnelBrightness
                && tunnelDensity == other.tunnelDensity
                && tunnelFov == other.tunnelFov
                && voronoiColorCount == other.voronoiColorCount
                && voronoiStepsPerColor == other.voronoiStepsPerColor
                && voronoiColor0 == other.voronoiColor0
                && voronoiColor1 == other.voronoiColor1
                && voronoiColor2 == other.voronoiColor2
                && voronoiColor3 == other.voronoiColor3
                && voronoiColor4 == other.voronoiColor4
                && voronoiColorGlow == other.voronoiColorGlow
                && voronoiColorGap == other.voronoiColorGap
                && voronoiDistortion == other.voronoiDistortion
                && voronoiGap == other.voronoiGap
                && voronoiGlow == other.voronoiGlow
                && voronoiScale == other.voronoiScale
                && voronoiFov == other.voronoiFov
                && voronoiSpeed == other.voronoiSpeed
                && voronoiRotation == other.voronoiRotation
                && voronoiOffsetX == other.voronoiOffsetX
                && voronoiOffsetY == other.voronoiOffsetY
                && slashStarfield == other.slashStarfield
                && slashStarDensity == other.slashStarDensity
                && slashStarBrightness == other.slashStarBrightness
                && slashStarSize == other.slashStarSize
                && slashStarColorMode == other.slashStarColorMode
                && uiTabVariant == other.uiTabVariant
                && uiTabSize == other.uiTabSize
                && uiTabCompact == other.uiTabCompact
                && uiSliderHeight == other.uiSliderHeight
                && uiSliderThumb == other.uiSliderThumb
                && uiSearchStyle == other.uiSearchStyle
                && uiSearchWidth == other.uiSearchWidth
                && uiSearchHeight == other.uiSearchHeight
                && uiSearchRadius == other.uiSearchRadius
                && uiSwitchStyle == other.uiSwitchStyle
                && uiSwitchWidth == other.uiSwitchWidth
                && uiSwitchHeight == other.uiSwitchHeight
                && uiSwitchKnob == other.uiSwitchKnob
                && uiSwitchGradientA == other.uiSwitchGradientA
                && uiSwitchGradientB == other.uiSwitchGradientB
                && uiSwitchTrackOff == other.uiSwitchTrackOff
                && uiSwitchSlideTrack == other.uiSwitchSlideTrack
                && uiSwitchSlideOff == other.uiSwitchSlideOff
                && uiSwitchSlideOn == other.uiSwitchSlideOn
                && uiSwitchTextTrackOff == other.uiSwitchTextTrackOff
                && uiSwitchTextTrackOn == other.uiSwitchTextTrackOn
                && uiSwitchTextKnob == other.uiSwitchTextKnob
                && uiSwitchTextColor == other.uiSwitchTextColor
                && uiSwitchTextOn.equals(other.uiSwitchTextOn)
                && uiSwitchTextOff.equals(other.uiSwitchTextOff);
    }

    public void read(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG);
        if (tag == null) return;
        if (tag.contains("enabled")) enabled = tag.getBoolean("enabled");
        if (tag.contains("invulnerable")) invulnerable = tag.getBoolean("invulnerable");
        if (tag.contains("grayScreen")) grayScreen = tag.getBoolean("grayScreen");
        if (tag.contains("grayStrength")) {
            grayStrength = clamp(tag.getFloat("grayStrength"), 0f, 1f);
        } else if (tag.contains("grayIntensity")) {
            grayStrength = 1f - clamp(tag.getFloat("grayIntensity"), 0f, 1f);
        }
        if (tag.contains("grayAnimate")) grayAnimate = tag.getBoolean("grayAnimate");
        if (tag.contains("startAnim")) startAnim = tag.getInt("startAnim");
        if (tag.contains("endAnim")) endAnim = tag.getInt("endAnim");
        if (tag.contains("startDuration")) startDuration = clamp(tag.getFloat("startDuration"), 0.1f, 2f);
        if (tag.contains("endDuration")) endDuration = clamp(tag.getFloat("endDuration"), 0.1f, 2f);
        if (tag.contains("ballColor")) ballColor = tag.getInt("ballColor");
        if (tag.contains("ballColorCustom")) ballColorCustom = tag.getBoolean("ballColorCustom");
        if (tag.contains("customColor")) customColor = tag.getInt("customColor");
        if (tag.contains("particleSize")) particleSize = clamp(tag.getFloat("particleSize"), 0.4f, 3f);
        if (tag.contains("stopDuration")) stopDuration = clamp(tag.getFloat("stopDuration"), 0f, 60f);
        if (tag.contains("triggerMode")) triggerMode = tag.getInt("triggerMode");
        if (tag.contains("soundEnabled")) soundEnabled = tag.getBoolean("soundEnabled");
        if (tag.contains("cooldown")) cooldown = tag.getInt("cooldown");
        if (tag.contains("soundVolume")) soundVolume = clamp(tag.getFloat("soundVolume"), 0f, 1f);
        if (tag.contains("particleAlpha")) particleAlpha = clamp(tag.getFloat("particleAlpha"), 0.1f, 1f);
        if (tag.contains("particleCount")) particleCount = tag.getInt("particleCount");
        if (tag.contains("particleSpin")) particleSpin = clamp(tag.getFloat("particleSpin"), 0f, 3f);
        if (tag.contains("freezeSelf")) freezeSelf = tag.getBoolean("freezeSelf");
        if (tag.contains("stopRadius")) stopRadius = clamp(tag.getFloat("stopRadius"), 0f, 128f);
        if (tag.contains("freezeEntities")) freezeEntities = tag.getBoolean("freezeEntities");
        if (tag.contains("freezeBlocks")) freezeBlocks = tag.getBoolean("freezeBlocks");
        if (tag.contains("freezeFluids")) freezeFluids = tag.getBoolean("freezeFluids");
        if (tag.contains("freezeBossAI")) freezeBossAI = tag.getBoolean("freezeBossAI");
        if (tag.contains("showMessage")) showMessage = tag.getBoolean("showMessage");
        if (tag.contains("grayStyle")) grayStyle = tag.getInt("grayStyle");
        if (tag.contains("rewindEnabled")) rewindEnabled = tag.getBoolean("rewindEnabled");
        if (tag.contains("rewindWindowSeconds")) rewindWindowSeconds = (int) clamp(tag.getFloat("rewindWindowSeconds"), 1f, 600f);
        if (tag.contains("rewindScopeMode")) rewindScopeMode = Math.max(0, Math.min(1, tag.getInt("rewindScopeMode")));
        if (tag.contains("rewindScope")) rewindScope = Math.max(0, Math.min(1, tag.getInt("rewindScope")));
        if (tag.contains("rewindRadius")) rewindRadius = clamp(tag.getFloat("rewindRadius"), 0f, 1024f);
        if (tag.contains("rewindCooldownTicks")) rewindCooldownTicks = (int) clamp(tag.getFloat("rewindCooldownTicks"), 0f, 72000f);
        if (tag.contains("rewindPlaybackMode")) rewindPlaybackMode = Math.max(0, Math.min(1, tag.getInt("rewindPlaybackMode")));
        if (tag.contains("rewindPlaybackSeconds")) rewindPlaybackSeconds = clamp(tag.getFloat("rewindPlaybackSeconds"), 0.05f, 3600f);
        if (tag.contains("rewindRestoreOrder")) rewindRestoreOrder = Math.max(0, Math.min(1, tag.getInt("rewindRestoreOrder")));
        if (tag.contains("rewindCameraMode")) rewindCameraMode = Math.max(0, Math.min(1, tag.getInt("rewindCameraMode")));
        if (tag.contains("rewindPositionRewind")) rewindPositionRewind = tag.getBoolean("rewindPositionRewind");
        if (tag.contains("rewindPositionMode")) rewindPositionMode = Math.max(0, Math.min(1, tag.getInt("rewindPositionMode")));
        if (tag.contains("rewindPlayerState")) rewindPlayerState = tag.getBoolean("rewindPlayerState");
        if (tag.contains("rewindDeathEnabled")) rewindDeathEnabled = tag.getBoolean("rewindDeathEnabled");
        if (tag.contains("rewindDeathCooldownTicks")) rewindDeathCooldownTicks = (int) clamp(tag.getFloat("rewindDeathCooldownTicks"), 0f, 72000f);
        if (tag.contains("rewindDeathMaxRetries")) rewindDeathMaxRetries = Math.max(0, Math.min(100, tag.getInt("rewindDeathMaxRetries")));
        if (tag.contains("rewindSafetyCheckpoint")) rewindSafetyCheckpoint = tag.getBoolean("rewindSafetyCheckpoint");
        if (tag.contains("rewindHostileCheck")) rewindHostileCheck = tag.getBoolean("rewindHostileCheck");
        if (tag.contains("rewindOtherItemDeduct")) rewindOtherItemDeduct = tag.getBoolean("rewindOtherItemDeduct");
        if (tag.contains("rewindFreezeOthers")) rewindFreezeOthers = tag.getBoolean("rewindFreezeOthers");
        if (tag.contains("rewindTimestopStacking")) rewindTimestopStacking = tag.getBoolean("rewindTimestopStacking");
        if (tag.contains("rewindBlocks")) rewindBlocks = tag.getBoolean("rewindBlocks");
        if (tag.contains("rewindBlockEntities")) rewindBlockEntities = tag.getBoolean("rewindBlockEntities");
        if (tag.contains("rewindEntities")) rewindEntities = tag.getBoolean("rewindEntities");
        if (tag.contains("rewindItems")) rewindItems = tag.getBoolean("rewindItems");
        if (tag.contains("rewindExperience")) rewindExperience = tag.getBoolean("rewindExperience");
        if (tag.contains("rewindTime")) rewindTime = tag.getBoolean("rewindTime");
        if (tag.contains("rewindWeather")) rewindWeather = tag.getBoolean("rewindWeather");
        if (tag.contains("rewindRaids")) rewindRaids = tag.getBoolean("rewindRaids");
        if (tag.contains("rewindScoreboard")) rewindScoreboard = tag.getBoolean("rewindScoreboard");
        if (tag.contains("rewindWorldBorder")) rewindWorldBorder = tag.getBoolean("rewindWorldBorder");
        if (tag.contains("rewindFreeCamRestorePosition")) rewindFreeCamRestorePosition = tag.getBoolean("rewindFreeCamRestorePosition");
        if (tag.contains("rewindShowStats")) rewindShowStats = tag.getBoolean("rewindShowStats");
        if (tag.contains("slashEnabled")) slashEnabled = tag.getBoolean("slashEnabled");
        if (tag.contains("slashDepthTest")) slashDepthTest = tag.getBoolean("slashDepthTest");
        if (tag.contains("slashRandomAngle")) slashRandomAngle = tag.getBoolean("slashRandomAngle");
        if (tag.contains("slashGlow")) slashGlow = tag.getBoolean("slashGlow");
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
        if (tag.contains("slashCoreColor")) slashCoreColor = tag.getInt("slashCoreColor");
        if (tag.contains("slashEdgeColor")) slashEdgeColor = tag.getInt("slashEdgeColor");
        if (tag.contains("slashGlowColor")) slashGlowColor = tag.getInt("slashGlowColor");
        if (tag.contains("renderStyle")) renderStyle = Math.max(0, Math.min(5, tag.getInt("renderStyle")));
        if (tag.contains("silkColor0")) silkColor0 = tag.getInt("silkColor0");
        if (tag.contains("silkColor1")) silkColor1 = tag.getInt("silkColor1");
        if (tag.contains("silkColor2")) silkColor2 = tag.getInt("silkColor2");
        if (tag.contains("silkColor3")) silkColor3 = tag.getInt("silkColor3");
        if (tag.contains("silkColor4")) silkColor4 = tag.getInt("silkColor4");
        if (tag.contains("silkColor5")) silkColor5 = tag.getInt("silkColor5");
        if (tag.contains("silkColor6")) silkColor6 = tag.getInt("silkColor6");
        if (tag.contains("silkColor7")) silkColor7 = tag.getInt("silkColor7");
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
        if (tag.contains("voronoiColorCount")) voronoiColorCount = tag.getInt("voronoiColorCount");
        if (tag.contains("voronoiStepsPerColor")) voronoiStepsPerColor = tag.getInt("voronoiStepsPerColor");
        if (tag.contains("voronoiColor0")) voronoiColor0 = tag.getInt("voronoiColor0");
        if (tag.contains("voronoiColor1")) voronoiColor1 = tag.getInt("voronoiColor1");
        if (tag.contains("voronoiColor2")) voronoiColor2 = tag.getInt("voronoiColor2");
        if (tag.contains("voronoiColor3")) voronoiColor3 = tag.getInt("voronoiColor3");
        if (tag.contains("voronoiColor4")) voronoiColor4 = tag.getInt("voronoiColor4");
        if (tag.contains("voronoiColorGlow")) voronoiColorGlow = tag.getInt("voronoiColorGlow");
        if (tag.contains("voronoiColorGap")) voronoiColorGap = tag.getInt("voronoiColorGap");
        if (tag.contains("voronoiDistortion")) voronoiDistortion = clamp(tag.getFloat("voronoiDistortion"), 0f, 0.5f);
        if (tag.contains("voronoiGap")) voronoiGap = clamp(tag.getFloat("voronoiGap"), 0f, 0.1f);
        if (tag.contains("voronoiGlow")) voronoiGlow = clamp(tag.getFloat("voronoiGlow"), 0f, 1f);
        if (tag.contains("voronoiScale")) voronoiScale = clamp(tag.getFloat("voronoiScale"), 0.05f, 4f);
        if (tag.contains("voronoiFov")) voronoiFov = clamp(tag.getFloat("voronoiFov"), 1f, 16f);
        if (tag.contains("voronoiSpeed")) voronoiSpeed = clamp(tag.getFloat("voronoiSpeed"), 0f, 3f);
        if (tag.contains("voronoiRotation")) voronoiRotation = clamp(tag.getFloat("voronoiRotation"), -6.283f, 6.283f);
        if (tag.contains("voronoiOffsetX")) voronoiOffsetX = clamp(tag.getFloat("voronoiOffsetX"), -1f, 1f);
        if (tag.contains("voronoiOffsetY")) voronoiOffsetY = clamp(tag.getFloat("voronoiOffsetY"), -1f, 1f);
        if (tag.contains("slashStarfield")) slashStarfield = tag.getBoolean("slashStarfield");
        if (tag.contains("slashStarDensity")) slashStarDensity = clamp(tag.getFloat("slashStarDensity"), 0.05f, 3f);
        if (tag.contains("slashStarBrightness")) slashStarBrightness = clamp(tag.getFloat("slashStarBrightness"), 0f, 3f);
        if (tag.contains("slashStarSize")) slashStarSize = clamp(tag.getFloat("slashStarSize"), 0.2f, 5f);
        if (tag.contains("slashStarColorMode")) slashStarColorMode = tag.getInt("slashStarColorMode");
        if (tag.contains("uiTabVariant")) uiTabVariant = tag.getInt("uiTabVariant");
        if (tag.contains("uiTabSize")) uiTabSize = tag.getInt("uiTabSize");
        if (tag.contains("uiTabCompact")) uiTabCompact = tag.getBoolean("uiTabCompact");
        if (tag.contains("uiSliderHeight")) uiSliderHeight = tag.getInt("uiSliderHeight");
        if (tag.contains("uiSliderThumb")) uiSliderThumb = tag.getInt("uiSliderThumb");
        if (tag.contains("uiSearchStyle")) uiSearchStyle = tag.getInt("uiSearchStyle");
        if (tag.contains("uiSearchWidth")) uiSearchWidth = clamp(tag.getFloat("uiSearchWidth"), 120f, 280f);
        if (tag.contains("uiSearchHeight")) uiSearchHeight = clamp(tag.getFloat("uiSearchHeight"), 28f, 48f);
        if (tag.contains("uiSearchRadius")) uiSearchRadius = clamp(tag.getFloat("uiSearchRadius"), 0f, 24f);
        if (tag.contains("uiSwitchStyle")) uiSwitchStyle = tag.getInt("uiSwitchStyle");
        if (tag.contains("uiSwitchWidth")) uiSwitchWidth = clamp(tag.getFloat("uiSwitchWidth"), 54f, 80f);
        if (tag.contains("uiSwitchHeight")) uiSwitchHeight = clamp(tag.getFloat("uiSwitchHeight"), 24f, 32f);
        if (tag.contains("uiSwitchKnob")) uiSwitchKnob = clamp(tag.getFloat("uiSwitchKnob"), 16f, 24f);
        if (tag.contains("uiSwitchGradientA")) uiSwitchGradientA = tag.getInt("uiSwitchGradientA");
        if (tag.contains("uiSwitchGradientB")) uiSwitchGradientB = tag.getInt("uiSwitchGradientB");
        if (tag.contains("uiSwitchTrackOff")) uiSwitchTrackOff = tag.getInt("uiSwitchTrackOff");
        if (tag.contains("uiSwitchSlideTrack")) uiSwitchSlideTrack = tag.getInt("uiSwitchSlideTrack");
        if (tag.contains("uiSwitchSlideOff")) uiSwitchSlideOff = tag.getInt("uiSwitchSlideOff");
        if (tag.contains("uiSwitchSlideOn")) uiSwitchSlideOn = tag.getInt("uiSwitchSlideOn");
        if (tag.contains("uiSwitchTextTrackOff")) uiSwitchTextTrackOff = tag.getInt("uiSwitchTextTrackOff");
        if (tag.contains("uiSwitchTextTrackOn")) uiSwitchTextTrackOn = tag.getInt("uiSwitchTextTrackOn");
        if (tag.contains("uiSwitchTextKnob")) uiSwitchTextKnob = tag.getInt("uiSwitchTextKnob");
        if (tag.contains("uiSwitchTextColor")) uiSwitchTextColor = tag.getInt("uiSwitchTextColor");
        if (tag.contains("uiSwitchTextOn")) uiSwitchTextOn = tag.getString("uiSwitchTextOn");
        if (tag.contains("uiSwitchTextOff")) uiSwitchTextOff = tag.getString("uiSwitchTextOff");
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
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("invulnerable", invulnerable);
        tag.putBoolean("grayScreen", grayScreen);
        tag.putFloat("grayStrength", grayStrength);
        tag.putFloat("grayIntensity", 1f - grayStrength);
        tag.putBoolean("grayAnimate", grayAnimate);
        tag.putInt("startAnim", startAnim);
        tag.putInt("endAnim", endAnim);
        tag.putFloat("startDuration", startDuration);
        tag.putFloat("endDuration", endDuration);
        tag.putInt("ballColor", ballColor);
        tag.putBoolean("ballColorCustom", ballColorCustom);
        tag.putInt("customColor", customColor);
        tag.putFloat("particleSize", particleSize);
        tag.putFloat("stopDuration", stopDuration);
        tag.putInt("triggerMode", triggerMode);
        tag.putBoolean("soundEnabled", soundEnabled);
        tag.putInt("cooldown", cooldown);
        tag.putFloat("soundVolume", soundVolume);
        tag.putFloat("particleAlpha", particleAlpha);
        tag.putInt("particleCount", particleCount);
        tag.putFloat("particleSpin", particleSpin);
        tag.putBoolean("freezeSelf", freezeSelf);
        tag.putFloat("stopRadius", stopRadius);
        tag.putBoolean("freezeEntities", freezeEntities);
        tag.putBoolean("freezeBlocks", freezeBlocks);
        tag.putBoolean("freezeFluids", freezeFluids);
        tag.putBoolean("freezeBossAI", freezeBossAI);
        tag.putBoolean("showMessage", showMessage);
        tag.putInt("grayStyle", grayStyle);
        tag.putBoolean("rewindEnabled", rewindEnabled);
        tag.putInt("rewindWindowSeconds", rewindWindowSeconds);
        tag.putInt("rewindScopeMode", rewindScopeMode);
        tag.putInt("rewindScope", rewindScope);
        tag.putFloat("rewindRadius", rewindRadius);
        tag.putInt("rewindCooldownTicks", rewindCooldownTicks);
        tag.putInt("rewindPlaybackMode", rewindPlaybackMode);
        tag.putFloat("rewindPlaybackSeconds", rewindPlaybackSeconds);
        tag.putInt("rewindRestoreOrder", rewindRestoreOrder);
        tag.putInt("rewindCameraMode", rewindCameraMode);
        tag.putBoolean("rewindPositionRewind", rewindPositionRewind);
        tag.putInt("rewindPositionMode", rewindPositionMode);
        tag.putBoolean("rewindPlayerState", rewindPlayerState);
        tag.putBoolean("rewindDeathEnabled", rewindDeathEnabled);
        tag.putInt("rewindDeathCooldownTicks", rewindDeathCooldownTicks);
        tag.putInt("rewindDeathMaxRetries", rewindDeathMaxRetries);
        tag.putBoolean("rewindSafetyCheckpoint", rewindSafetyCheckpoint);
        tag.putBoolean("rewindHostileCheck", rewindHostileCheck);
        tag.putBoolean("rewindOtherItemDeduct", rewindOtherItemDeduct);
        tag.putBoolean("rewindFreezeOthers", rewindFreezeOthers);
        tag.putBoolean("rewindTimestopStacking", rewindTimestopStacking);
        tag.putBoolean("rewindBlocks", rewindBlocks);
        tag.putBoolean("rewindBlockEntities", rewindBlockEntities);
        tag.putBoolean("rewindEntities", rewindEntities);
        tag.putBoolean("rewindItems", rewindItems);
        tag.putBoolean("rewindExperience", rewindExperience);
        tag.putBoolean("rewindTime", rewindTime);
        tag.putBoolean("rewindWeather", rewindWeather);
        tag.putBoolean("rewindRaids", rewindRaids);
        tag.putBoolean("rewindScoreboard", rewindScoreboard);
        tag.putBoolean("rewindWorldBorder", rewindWorldBorder);
        tag.putBoolean("rewindFreeCamRestorePosition", rewindFreeCamRestorePosition);
        tag.putBoolean("rewindShowStats", rewindShowStats);
        tag.putBoolean("slashEnabled", slashEnabled);
        tag.putBoolean("slashDepthTest", slashDepthTest);
        tag.putBoolean("slashRandomAngle", slashRandomAngle);
        tag.putBoolean("slashGlow", slashGlow);
        tag.putFloat("slashDuration", slashDuration);
        tag.putFloat("slashLengthMult", slashLengthMult);
        tag.putFloat("slashWidthRatio", slashWidthRatio);
        tag.putFloat("slashThicknessRatio", slashThicknessRatio);
        tag.putFloat("slashTipFade", slashTipFade);
        tag.putFloat("slashStartScale", slashStartScale);
        tag.putFloat("slashEndScale", slashEndScale);
        tag.putFloat("slashCoreWidth", slashCoreWidth);
        tag.putFloat("slashCoreShade", slashCoreShade);
        tag.putFloat("slashEdgeWidth", slashEdgeWidth);
        tag.putFloat("slashEdgeBrightness", slashEdgeBrightness);
        tag.putFloat("slashGlowWidth", slashGlowWidth);
        tag.putFloat("slashGlowStrength", slashGlowStrength);
        tag.putFloat("slashNoiseStrength", slashNoiseStrength);
        tag.putFloat("slashSweepSpeed", slashSweepSpeed);
        tag.putFloat("slashSweepSoftness", slashSweepSoftness);
        tag.putFloat("slashHoldFraction", slashHoldFraction);
        tag.putFloat("slashFadeStart", slashFadeStart);
        tag.putFloat("slashFadeDuration", slashFadeDuration);
        tag.putFloat("slashSurfaceOffset", slashSurfaceOffset);
        tag.putFloat("slashRollRange", slashRollRange);
        tag.putInt("slashCoreColor", slashCoreColor);
        tag.putInt("slashEdgeColor", slashEdgeColor);
        tag.putInt("slashGlowColor", slashGlowColor);
        tag.putInt("renderStyle", renderStyle);
        tag.putInt("silkColor0", silkColor0);
        tag.putInt("silkColor1", silkColor1);
        tag.putInt("silkColor2", silkColor2);
        tag.putInt("silkColor3", silkColor3);
        tag.putInt("silkColor4", silkColor4);
        tag.putInt("silkColor5", silkColor5);
        tag.putInt("silkColor6", silkColor6);
        tag.putInt("silkColor7", silkColor7);
        tag.putFloat("silkBrightness", silkBrightness);
        tag.putFloat("silkContrast", silkContrast);
        tag.putFloat("silkSaturation", silkSaturation);
        tag.putFloat("silkScale", silkScale);
        tag.putFloat("silkIntensity", silkIntensity);
        tag.putFloat("silkWarp", silkWarp);
        tag.putFloat("silkDetail", silkDetail);
        tag.putFloat("silkHue", silkHue);
        tag.putFloat("silkSeed", silkSeed);
        tag.putFloat("silkRotation", silkRotation);
        tag.putFloat("silkDrift", silkDrift);
        tag.putFloat("silkVignette", silkVignette);
        tag.putFloat("silkBlur", silkBlur);
        tag.putFloat("silkGrain", silkGrain);
        tag.putFloat("tunnelSpeed", tunnelSpeed);
        tag.putFloat("tunnelBrightness", tunnelBrightness);
        tag.putFloat("tunnelDensity", tunnelDensity);
        tag.putFloat("tunnelFov", tunnelFov);
        tag.putInt("voronoiColorCount", voronoiColorCount);
        tag.putInt("voronoiStepsPerColor", voronoiStepsPerColor);
        tag.putInt("voronoiColor0", voronoiColor0);
        tag.putInt("voronoiColor1", voronoiColor1);
        tag.putInt("voronoiColor2", voronoiColor2);
        tag.putInt("voronoiColor3", voronoiColor3);
        tag.putInt("voronoiColor4", voronoiColor4);
        tag.putInt("voronoiColorGlow", voronoiColorGlow);
        tag.putInt("voronoiColorGap", voronoiColorGap);
        tag.putFloat("voronoiDistortion", voronoiDistortion);
        tag.putFloat("voronoiGap", voronoiGap);
        tag.putFloat("voronoiGlow", voronoiGlow);
        tag.putFloat("voronoiScale", voronoiScale);
        tag.putFloat("voronoiFov", voronoiFov);
        tag.putFloat("voronoiSpeed", voronoiSpeed);
        tag.putFloat("voronoiRotation", voronoiRotation);
        tag.putFloat("voronoiOffsetX", voronoiOffsetX);
        tag.putFloat("voronoiOffsetY", voronoiOffsetY);
        tag.putBoolean("slashStarfield", slashStarfield);
        tag.putFloat("slashStarDensity", slashStarDensity);
        tag.putFloat("slashStarBrightness", slashStarBrightness);
        tag.putFloat("slashStarSize", slashStarSize);
        tag.putInt("slashStarColorMode", slashStarColorMode);
        tag.putInt("uiTabVariant", uiTabVariant);
        tag.putInt("uiTabSize", uiTabSize);
        tag.putBoolean("uiTabCompact", uiTabCompact);
        tag.putInt("uiSliderHeight", uiSliderHeight);
        tag.putInt("uiSliderThumb", uiSliderThumb);
        tag.putInt("uiSearchStyle", uiSearchStyle);
        tag.putFloat("uiSearchWidth", uiSearchWidth);
        tag.putFloat("uiSearchHeight", uiSearchHeight);
        tag.putFloat("uiSearchRadius", uiSearchRadius);
        tag.putInt("uiSwitchStyle", uiSwitchStyle);
        tag.putFloat("uiSwitchWidth", uiSwitchWidth);
        tag.putFloat("uiSwitchHeight", uiSwitchHeight);
        tag.putFloat("uiSwitchKnob", uiSwitchKnob);
        tag.putInt("uiSwitchGradientA", uiSwitchGradientA);
        tag.putInt("uiSwitchGradientB", uiSwitchGradientB);
        tag.putInt("uiSwitchTrackOff", uiSwitchTrackOff);
        tag.putInt("uiSwitchSlideTrack", uiSwitchSlideTrack);
        tag.putInt("uiSwitchSlideOff", uiSwitchSlideOff);
        tag.putInt("uiSwitchSlideOn", uiSwitchSlideOn);
        tag.putInt("uiSwitchTextTrackOff", uiSwitchTextTrackOff);
        tag.putInt("uiSwitchTextTrackOn", uiSwitchTextTrackOn);
        tag.putInt("uiSwitchTextKnob", uiSwitchTextKnob);
        tag.putInt("uiSwitchTextColor", uiSwitchTextColor);
        tag.putString("uiSwitchTextOn", uiSwitchTextOn);
        tag.putString("uiSwitchTextOff", uiSwitchTextOff);
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
