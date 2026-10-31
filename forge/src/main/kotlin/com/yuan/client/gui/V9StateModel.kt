package com.yuan.client.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuan.item.YuanGodSwordConfig
import net.minecraft.client.gui.screens.Screen
import org.jetbrains.skia.Surface

internal val renderStyleUiValues = listOf(0, 1, 4, 5, 2, 3)


internal fun renderStyleStored(uiIndex: Int): Int =
    if (uiIndex in renderStyleUiValues.indices) renderStyleUiValues[uiIndex] else 0


internal fun renderStyleUiIndex(stored: Int): Int {
    val idx = renderStyleUiValues.indexOf(stored)
    return if (idx >= 0) idx else 0
}


internal data class VoronoiPreset(
    val name: String,
    val colorCount: Int,
    val steps: Int,
    val color0: Int,
    val color1: Int,
    val color2: Int,
    val color3: Int,
    val color4: Int,
    val glow: Int,
    val gap: Int,
    val distortion: Float,
    val gapSize: Float,
    val glowStrength: Float,
    val scale: Float,
    val speed: Float
)


internal val voronoiPresets = listOf(
    VoronoiPreset("默认", 2, 3, 0xFFFF8247.toInt(), 0xFFFFE53D.toInt(), 0xFFFFE53D.toInt(), 0xFFFFE53D.toInt(), 0xFFFFE53D.toInt(), 0xFFFFFFFF.toInt(), 0xFF2E0000.toInt(), 0.4f, 0.04f, 0f, 0.5f, 0.5f),
    VoronoiPreset("灯光", 3, 2, 0xFFFFFFFC.toInt(), 0xFFBBFF00.toInt(), 0xFF00FFFF.toInt(), 0xFF00FFFF.toInt(), 0xFF00FFFF.toInt(), 0xFFFF00D0.toInt(), 0xFFFF00D0.toInt(), 0.38f, 0f, 1f, 3.3f, 0.5f),
    VoronoiPreset("细胞", 1, 1, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0.5f, 0.03f, 0.8f, 0.5f, 0.5f),
    VoronoiPreset("气泡", 1, 1, 0xFF83C9FB.toInt(), 0xFF83C9FB.toInt(), 0xFF83C9FB.toInt(), 0xFF83C9FB.toInt(), 0xFF83C9FB.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0.4f, 0f, 1f, 0.75f, 0.5f)
)


internal data class V9State(
    val enabled: Boolean = true,
    val invulnerable: Boolean = true,
    val grayScreen: Boolean = true,
    val grayStrength: Float = 1.0f,
    val grayAnimate: Boolean = false,
    val startAnim: Int = 0,
    val endAnim: Int = 0,
    val startDuration: Float = 0.4f,
    val endDuration: Float = 0.3f,
    val ballColor: Int = 0,
    val ballColorCustom: Boolean = false,
    val customColor: Int = 0xFFFF7A5C.toInt(),
    val particleSize: Float = 0.62f,
    val stopDuration: Float = 0.0f,
    val triggerMode: Int = 0,
    val soundEnabled: Boolean = true,
    val cooldown: Int = 30,
    val soundVolume: Float = 1.0f,
    val particleAlpha: Float = 0.5f,
    val particleCount: Int = 1,
    val particleSpin: Float = 0.3f,
    val freezeSelf: Boolean = false,
    val stopRadius: Float = 0.0f,
    val freezeEntities: Boolean = true,
    val freezeBlocks: Boolean = true,
    val freezeFluids: Boolean = true,
    val freezeBossAI: Boolean = true,
    val showMessage: Boolean = false,
    val grayStyle: Int = 0,
    val slashEnabled: Boolean = true,
    val slashDepthTest: Boolean = true,
    val slashRandomAngle: Boolean = true,
    val slashGlow: Boolean = true,
    val slashDuration: Float = 0.26f,
    val slashLengthMult: Float = 2.8f,
    val slashWidthRatio: Float = 0.09f,
    val slashThicknessRatio: Float = 0.02f,
    val slashTipFade: Float = 0.08f,
    val slashStartScale: Float = 1.0f,
    val slashEndScale: Float = 1.0f,
    val slashCoreWidth: Float = 0.62f,
    val slashCoreShade: Float = 1.0f,
    val slashEdgeWidth: Float = 0.80f,
    val slashEdgeBrightness: Float = 1.0f,
    val slashGlowWidth: Float = 0.90f,
    val slashGlowStrength: Float = 1.0f,
    val slashNoiseStrength: Float = 1.0f,
    val slashSweepSpeed: Float = 0.35f,
    val slashSweepSoftness: Float = 0.15f,
    val slashHoldFraction: Float = 0.0f,
    val slashFadeStart: Float = 0.72f,
    val slashFadeDuration: Float = 0.28f,
    val slashSurfaceOffset: Float = 0.06f,
    val slashRollRange: Float = 180f,
    val slashCoreColor: Int = 0xFF161616.toInt(),
    val slashEdgeColor: Int = 0xFFFFFFFF.toInt(),
    val slashGlowColor: Int = 0xFFFFFFFF.toInt(),
    val renderStyle: Int = 0,
    val silkColor0: Int = 0xFF1A1423.toInt(),
    val silkColor1: Int = 0xFFB75D69.toInt(),
    val silkColor2: Int = 0xFFEACDC2.toInt(),
    val silkColor3: Int = 0xFFFFF5EB.toInt(),
    val silkColor4: Int = 0xFFFFF5EB.toInt(),
    val silkColor5: Int = 0xFFFFF5EB.toInt(),
    val silkColor6: Int = 0xFFFFF5EB.toInt(),
    val silkColor7: Int = 0xFFFFF5EB.toInt(),
    val silkBrightness: Float = 0.03f,
    val silkContrast: Float = 1.19f,
    val silkSaturation: Float = 1.34f,
    val silkScale: Float = 1.74f,
    val silkIntensity: Float = 0.69f,
    val silkWarp: Float = 0.07f,
    val silkDetail: Float = 2.66f,
    val silkHue: Float = 0.0f,
    val silkSeed: Float = 9199f,
    val silkRotation: Float = 5.06f,
    val silkDrift: Float = 0.02f,
    val silkVignette: Float = 0.12f,
    val silkBlur: Float = 0.004f,
    val silkGrain: Float = 0.0f,
    val tunnelSpeed: Float = 1.0f,
    val tunnelBrightness: Float = 1.0f,
    val tunnelDensity: Float = 1.0f,
    val tunnelFov: Float = 6.0f,
    val voronoiColorCount: Int = 1,
    val voronoiStepsPerColor: Int = 3,
    val voronoiColor0: Int = 0xFFFF8247.toInt(),
    val voronoiColor1: Int = 0xFFFFE53D.toInt(),
    val voronoiColor2: Int = 0xFFFFE53D.toInt(),
    val voronoiColor3: Int = 0xFFFFE53D.toInt(),
    val voronoiColor4: Int = 0xFFFFE53D.toInt(),
    val voronoiColorGlow: Int = 0xFFFFFFFF.toInt(),
    val voronoiColorGap: Int = 0xFF2E0000.toInt(),
    val voronoiDistortion: Float = 0.4f,
    val voronoiGap: Float = 0.04f,
    val voronoiGlow: Float = 0.0f,
    val voronoiScale: Float = 0.5f,
    val voronoiFov: Float = 6.0f,
    val voronoiSpeed: Float = 0.5f,
    val voronoiRotation: Float = 0.0f,
    val voronoiOffsetX: Float = 0.0f,
    val voronoiOffsetY: Float = 0.0f,
    val slashStarfield: Boolean = false,
    val slashStarDensity: Float = 0.6f,
    val slashStarBrightness: Float = 0.8f,
    val slashStarSize: Float = 1.0f,
    val slashStarColorMode: Int = 0,
    val uiTabVariant: Int = 0,
    val uiTabSize: Int = 1,
    val uiTabCompact: Boolean = false,
    val uiSliderHeight: Int = 1,
    val uiSliderThumb: Int = 1,
    val uiSearchStyle: Int = 0,
    val uiSearchWidth: Float = 200f,
    val uiSearchHeight: Float = 40f,
    val uiSearchRadius: Float = 20f,
    val uiSwitchStyle: Int = 0,
    val uiSwitchWidth: Float = 62f,
    val uiSwitchHeight: Float = 28f,
    val uiSwitchKnob: Float = 22f,
    val uiSwitchGradientA: Int = 0xFFF19AF3.toInt(),
    val uiSwitchGradientB: Int = 0xFFF099B5.toInt(),
    val uiSwitchTrackOff: Int = 0xFFD7D7D7.toInt(),
    val uiSwitchSlideTrack: Int = 0xFFFFFFFF.toInt(),
    val uiSwitchSlideOff: Int = 0xFFCCCCCC.toInt(),
    val uiSwitchSlideOn: Int = 0xFF59D102.toInt(),
    val uiSwitchTextTrackOff: Int = 0xFF05012C.toInt(),
    val uiSwitchTextTrackOn: Int = 0xFFFFB500.toInt(),
    val uiSwitchTextKnob: Int = 0xFFFFFFFF.toInt(),
    val uiSwitchTextColor: Int = 0xFF78768D.toInt(),
    val uiSwitchTextOn: String = "On",
    val uiSwitchTextOff: String = "Off",
    val domainStarfield: Boolean = true,
    val rewindEnabled: Boolean = true,
    val rewindWindowSeconds: Int = 10,
    val rewindScopeMode: Int = 0,
    val rewindScope: Int = 0,
    val rewindRadius: Float = 64f,
    val rewindCooldownTicks: Int = 600,
    val rewindPlaybackMode: Int = 0,
    val rewindPlaybackSeconds: Float = 2.5f,
    val rewindRestoreOrder: Int = 0,
    val rewindCameraMode: Int = 0,
    val rewindPositionRewind: Boolean = false,
    val rewindPositionMode: Int = 0,
    val rewindPlayerState: Boolean = true,
    val rewindDeathEnabled: Boolean = true,
    val rewindDeathCooldownTicks: Int = 1200,
    val rewindDeathMaxRetries: Int = 0,
    val rewindSafetyCheckpoint: Boolean = false,
    val rewindHostileCheck: Boolean = false,
    val rewindOtherItemDeduct: Boolean = true,
    val rewindFreezeOthers: Boolean = true,
    val rewindTimestopStacking: Boolean = false,
    val rewindBlocks: Boolean = true,
    val rewindBlockEntities: Boolean = true,
    val rewindEntities: Boolean = true,
    val rewindItems: Boolean = true,
    val rewindExperience: Boolean = true,
    val rewindTime: Boolean = true,
    val rewindWeather: Boolean = true,
    val rewindRaids: Boolean = true,
    val rewindScoreboard: Boolean = true,
    val rewindWorldBorder: Boolean = true,
    val rewindFreeCamRestorePosition: Boolean = true,
    val rewindShowStats: Boolean = true
)


internal fun v9StateOf(config: YuanGodSwordConfig): V9State = V9State(
    enabled = config.enabled,
    invulnerable = config.invulnerable,
    grayScreen = config.grayScreen,
    grayStrength = config.grayStrength,
    grayAnimate = config.grayAnimate,
    startAnim = config.startAnim,
    endAnim = config.endAnim,
    startDuration = config.startDuration,
    endDuration = config.endDuration,
    ballColor = config.ballColor,
    ballColorCustom = config.ballColorCustom,
    customColor = config.customColor,
    particleSize = config.particleSize,
    stopDuration = config.stopDuration,
    triggerMode = config.triggerMode,
    soundEnabled = config.soundEnabled,
    cooldown = config.cooldown,
    soundVolume = config.soundVolume,
    particleAlpha = config.particleAlpha,
    particleCount = config.particleCount,
    particleSpin = config.particleSpin,
    freezeSelf = config.freezeSelf,
    stopRadius = config.stopRadius,
    freezeEntities = config.freezeEntities,
    freezeBlocks = config.freezeBlocks,
    freezeFluids = config.freezeFluids,
    freezeBossAI = config.freezeBossAI,
    showMessage = config.showMessage,
    grayStyle = config.grayStyle,
    slashEnabled = config.slashEnabled,
    slashDepthTest = config.slashDepthTest,
    slashRandomAngle = config.slashRandomAngle,
    slashGlow = config.slashGlow,
    slashDuration = config.slashDuration,
    slashLengthMult = config.slashLengthMult,
    slashWidthRatio = config.slashWidthRatio,
    slashThicknessRatio = config.slashThicknessRatio,
    slashTipFade = config.slashTipFade,
    slashStartScale = config.slashStartScale,
    slashEndScale = config.slashEndScale,
    slashCoreWidth = config.slashCoreWidth,
    slashCoreShade = config.slashCoreShade,
    slashEdgeWidth = config.slashEdgeWidth,
    slashEdgeBrightness = config.slashEdgeBrightness,
    slashGlowWidth = config.slashGlowWidth,
    slashGlowStrength = config.slashGlowStrength,
    slashNoiseStrength = config.slashNoiseStrength,
    slashSweepSpeed = config.slashSweepSpeed,
    slashSweepSoftness = config.slashSweepSoftness,
    slashHoldFraction = config.slashHoldFraction,
    slashFadeStart = config.slashFadeStart,
    slashFadeDuration = config.slashFadeDuration,
    slashSurfaceOffset = config.slashSurfaceOffset,
    slashRollRange = config.slashRollRange,
    slashCoreColor = config.slashCoreColor,
    slashEdgeColor = config.slashEdgeColor,
    slashGlowColor = config.slashGlowColor,
    renderStyle = config.renderStyle,
    silkColor0 = config.silkColor0,
    silkColor1 = config.silkColor1,
    silkColor2 = config.silkColor2,
    silkColor3 = config.silkColor3,
    silkColor4 = config.silkColor4,
    silkColor5 = config.silkColor5,
    silkColor6 = config.silkColor6,
    silkColor7 = config.silkColor7,
    silkBrightness = config.silkBrightness,
    silkContrast = config.silkContrast,
    silkSaturation = config.silkSaturation,
    silkScale = config.silkScale,
    silkIntensity = config.silkIntensity,
    silkWarp = config.silkWarp,
    silkDetail = config.silkDetail,
    silkHue = config.silkHue,
    silkSeed = config.silkSeed,
    silkRotation = config.silkRotation,
    silkDrift = config.silkDrift,
    silkVignette = config.silkVignette,
    silkBlur = config.silkBlur,
    silkGrain = config.silkGrain,
    tunnelSpeed = config.tunnelSpeed,
    tunnelBrightness = config.tunnelBrightness,
    tunnelDensity = config.tunnelDensity,
    tunnelFov = config.tunnelFov,
    voronoiColorCount = config.voronoiColorCount,
    voronoiStepsPerColor = config.voronoiStepsPerColor,
    voronoiColor0 = config.voronoiColor0,
    voronoiColor1 = config.voronoiColor1,
    voronoiColor2 = config.voronoiColor2,
    voronoiColor3 = config.voronoiColor3,
    voronoiColor4 = config.voronoiColor4,
    voronoiColorGlow = config.voronoiColorGlow,
    voronoiColorGap = config.voronoiColorGap,
    voronoiDistortion = config.voronoiDistortion,
    voronoiGap = config.voronoiGap,
    voronoiGlow = config.voronoiGlow,
    voronoiScale = config.voronoiScale,
    voronoiFov = config.voronoiFov,
    voronoiSpeed = config.voronoiSpeed,
    voronoiRotation = config.voronoiRotation,
    voronoiOffsetX = config.voronoiOffsetX,
    voronoiOffsetY = config.voronoiOffsetY,
    slashStarfield = config.slashStarfield,
    slashStarDensity = config.slashStarDensity,
    slashStarBrightness = config.slashStarBrightness,
    slashStarSize = config.slashStarSize,
    slashStarColorMode = config.slashStarColorMode,
    uiTabVariant = config.uiTabVariant,
    uiTabSize = config.uiTabSize,
    uiTabCompact = config.uiTabCompact,
    uiSliderHeight = config.uiSliderHeight,
    uiSliderThumb = config.uiSliderThumb,
    uiSearchStyle = config.uiSearchStyle,
    uiSearchWidth = config.uiSearchWidth,
    uiSearchHeight = config.uiSearchHeight,
    uiSearchRadius = config.uiSearchRadius,
    uiSwitchStyle = config.uiSwitchStyle,
    uiSwitchWidth = config.uiSwitchWidth,
    uiSwitchHeight = config.uiSwitchHeight,
    uiSwitchKnob = config.uiSwitchKnob,
    uiSwitchGradientA = config.uiSwitchGradientA,
    uiSwitchGradientB = config.uiSwitchGradientB,
    uiSwitchTrackOff = config.uiSwitchTrackOff,
    uiSwitchSlideTrack = config.uiSwitchSlideTrack,
    uiSwitchSlideOff = config.uiSwitchSlideOff,
    uiSwitchSlideOn = config.uiSwitchSlideOn,
    uiSwitchTextTrackOff = config.uiSwitchTextTrackOff,
    uiSwitchTextTrackOn = config.uiSwitchTextTrackOn,
    uiSwitchTextKnob = config.uiSwitchTextKnob,
    uiSwitchTextColor = config.uiSwitchTextColor,
    uiSwitchTextOn = config.uiSwitchTextOn,
    uiSwitchTextOff = config.uiSwitchTextOff,
    domainStarfield = config.domainStarfield,
    rewindEnabled = config.rewindEnabled,
    rewindWindowSeconds = config.rewindWindowSeconds,
    rewindScopeMode = config.rewindScopeMode,
    rewindScope = config.rewindScope,
    rewindRadius = config.rewindRadius,
    rewindCooldownTicks = config.rewindCooldownTicks,
    rewindPlaybackMode = config.rewindPlaybackMode,
    rewindPlaybackSeconds = config.rewindPlaybackSeconds,
    rewindRestoreOrder = config.rewindRestoreOrder,
    rewindCameraMode = config.rewindCameraMode,
    rewindPositionRewind = config.rewindPositionRewind,
    rewindPositionMode = config.rewindPositionMode,
    rewindPlayerState = config.rewindPlayerState,
    rewindDeathEnabled = config.rewindDeathEnabled,
    rewindDeathCooldownTicks = config.rewindDeathCooldownTicks,
    rewindDeathMaxRetries = config.rewindDeathMaxRetries,
    rewindSafetyCheckpoint = config.rewindSafetyCheckpoint,
    rewindHostileCheck = config.rewindHostileCheck,
    rewindOtherItemDeduct = config.rewindOtherItemDeduct,
    rewindFreezeOthers = config.rewindFreezeOthers,
    rewindTimestopStacking = config.rewindTimestopStacking,
    rewindBlocks = config.rewindBlocks,
    rewindBlockEntities = config.rewindBlockEntities,
    rewindEntities = config.rewindEntities,
    rewindItems = config.rewindItems,
    rewindExperience = config.rewindExperience,
    rewindTime = config.rewindTime,
    rewindWeather = config.rewindWeather,
    rewindRaids = config.rewindRaids,
    rewindScoreboard = config.rewindScoreboard,
    rewindWorldBorder = config.rewindWorldBorder,
    rewindFreeCamRestorePosition = config.rewindFreeCamRestorePosition,
    rewindShowStats = config.rewindShowStats
)


internal fun V9State.toConfig(): YuanGodSwordConfig = YuanGodSwordConfig().also {
    it.enabled = enabled
    it.invulnerable = invulnerable
    it.grayScreen = grayScreen
    it.grayStrength = grayStrength
    it.grayAnimate = grayAnimate
    it.startAnim = startAnim
    it.endAnim = endAnim
    it.startDuration = startDuration
    it.endDuration = endDuration
    it.ballColor = ballColor
    it.ballColorCustom = ballColorCustom
    it.customColor = customColor
    it.particleSize = particleSize
    it.stopDuration = stopDuration
    it.triggerMode = triggerMode
    it.soundEnabled = soundEnabled
    it.cooldown = cooldown
    it.soundVolume = soundVolume
    it.particleAlpha = particleAlpha
    it.particleCount = particleCount
    it.particleSpin = particleSpin
    it.freezeSelf = freezeSelf
    it.stopRadius = stopRadius
    it.freezeEntities = freezeEntities
    it.freezeBlocks = freezeBlocks
    it.freezeFluids = freezeFluids
    it.freezeBossAI = freezeBossAI
    it.showMessage = showMessage
    it.grayStyle = grayStyle
    it.slashEnabled = slashEnabled
    it.slashDepthTest = slashDepthTest
    it.slashRandomAngle = slashRandomAngle
    it.slashGlow = slashGlow
    it.slashDuration = slashDuration
    it.slashLengthMult = slashLengthMult
    it.slashWidthRatio = slashWidthRatio
    it.slashThicknessRatio = slashThicknessRatio
    it.slashTipFade = slashTipFade
    it.slashStartScale = slashStartScale
    it.slashEndScale = slashEndScale
    it.slashCoreWidth = slashCoreWidth
    it.slashCoreShade = slashCoreShade
    it.slashEdgeWidth = slashEdgeWidth
    it.slashEdgeBrightness = slashEdgeBrightness
    it.slashGlowWidth = slashGlowWidth
    it.slashGlowStrength = slashGlowStrength
    it.slashNoiseStrength = slashNoiseStrength
    it.slashSweepSpeed = slashSweepSpeed
    it.slashSweepSoftness = slashSweepSoftness
    it.slashHoldFraction = slashHoldFraction
    it.slashFadeStart = slashFadeStart
    it.slashFadeDuration = slashFadeDuration
    it.slashSurfaceOffset = slashSurfaceOffset
    it.slashRollRange = slashRollRange
    it.slashCoreColor = slashCoreColor
    it.slashEdgeColor = slashEdgeColor
    it.slashGlowColor = slashGlowColor
    it.renderStyle = renderStyle
    it.silkColor0 = silkColor0
    it.silkColor1 = silkColor1
    it.silkColor2 = silkColor2
    it.silkColor3 = silkColor3
    it.silkColor4 = silkColor4
    it.silkColor5 = silkColor5
    it.silkColor6 = silkColor6
    it.silkColor7 = silkColor7
    it.silkBrightness = silkBrightness
    it.silkContrast = silkContrast
    it.silkSaturation = silkSaturation
    it.silkScale = silkScale
    it.silkIntensity = silkIntensity
    it.silkWarp = silkWarp
    it.silkDetail = silkDetail
    it.silkHue = silkHue
    it.silkSeed = silkSeed
    it.silkRotation = silkRotation
    it.silkDrift = silkDrift
    it.silkVignette = silkVignette
    it.silkBlur = silkBlur
    it.silkGrain = silkGrain
    it.tunnelSpeed = tunnelSpeed
    it.tunnelBrightness = tunnelBrightness
    it.tunnelDensity = tunnelDensity
    it.tunnelFov = tunnelFov
    it.voronoiColorCount = voronoiColorCount
    it.voronoiStepsPerColor = voronoiStepsPerColor
    it.voronoiColor0 = voronoiColor0
    it.voronoiColor1 = voronoiColor1
    it.voronoiColor2 = voronoiColor2
    it.voronoiColor3 = voronoiColor3
    it.voronoiColor4 = voronoiColor4
    it.voronoiColorGlow = voronoiColorGlow
    it.voronoiColorGap = voronoiColorGap
    it.voronoiDistortion = voronoiDistortion
    it.voronoiGap = voronoiGap
    it.voronoiGlow = voronoiGlow
    it.voronoiScale = voronoiScale
    it.voronoiFov = voronoiFov
    it.voronoiSpeed = voronoiSpeed
    it.voronoiRotation = voronoiRotation
    it.voronoiOffsetX = voronoiOffsetX
    it.voronoiOffsetY = voronoiOffsetY
    it.slashStarfield = slashStarfield
    it.slashStarDensity = slashStarDensity
    it.slashStarBrightness = slashStarBrightness
    it.slashStarSize = slashStarSize
    it.slashStarColorMode = slashStarColorMode
    it.uiTabVariant = uiTabVariant
    it.uiTabSize = uiTabSize
    it.uiTabCompact = uiTabCompact
    it.uiSliderHeight = uiSliderHeight
    it.uiSliderThumb = uiSliderThumb
    it.uiSearchStyle = uiSearchStyle
    it.uiSearchWidth = uiSearchWidth
    it.uiSearchHeight = uiSearchHeight
    it.uiSearchRadius = uiSearchRadius
    it.uiSwitchStyle = uiSwitchStyle
    it.uiSwitchWidth = uiSwitchWidth
    it.uiSwitchHeight = uiSwitchHeight
    it.uiSwitchKnob = uiSwitchKnob
    it.uiSwitchGradientA = uiSwitchGradientA
    it.uiSwitchGradientB = uiSwitchGradientB
    it.uiSwitchTrackOff = uiSwitchTrackOff
    it.uiSwitchSlideTrack = uiSwitchSlideTrack
    it.uiSwitchSlideOff = uiSwitchSlideOff
    it.uiSwitchSlideOn = uiSwitchSlideOn
    it.uiSwitchTextTrackOff = uiSwitchTextTrackOff
    it.uiSwitchTextTrackOn = uiSwitchTextTrackOn
    it.uiSwitchTextKnob = uiSwitchTextKnob
    it.uiSwitchTextColor = uiSwitchTextColor
    it.uiSwitchTextOn = uiSwitchTextOn
    it.uiSwitchTextOff = uiSwitchTextOff
    it.domainStarfield = domainStarfield
    it.rewindEnabled = rewindEnabled
    it.rewindWindowSeconds = rewindWindowSeconds
    it.rewindScopeMode = rewindScopeMode
    it.rewindScope = rewindScope
    it.rewindRadius = rewindRadius
    it.rewindCooldownTicks = rewindCooldownTicks
    it.rewindPlaybackMode = rewindPlaybackMode
    it.rewindPlaybackSeconds = rewindPlaybackSeconds
    it.rewindRestoreOrder = rewindRestoreOrder
    it.rewindCameraMode = rewindCameraMode
    it.rewindPositionRewind = rewindPositionRewind
    it.rewindPositionMode = rewindPositionMode
    it.rewindPlayerState = rewindPlayerState
    it.rewindDeathEnabled = rewindDeathEnabled
    it.rewindDeathCooldownTicks = rewindDeathCooldownTicks
    it.rewindDeathMaxRetries = rewindDeathMaxRetries
    it.rewindSafetyCheckpoint = rewindSafetyCheckpoint
    it.rewindHostileCheck = rewindHostileCheck
    it.rewindOtherItemDeduct = rewindOtherItemDeduct
    it.rewindFreezeOthers = rewindFreezeOthers
    it.rewindTimestopStacking = rewindTimestopStacking
    it.rewindBlocks = rewindBlocks
    it.rewindBlockEntities = rewindBlockEntities
    it.rewindEntities = rewindEntities
    it.rewindItems = rewindItems
    it.rewindExperience = rewindExperience
    it.rewindTime = rewindTime
    it.rewindWeather = rewindWeather
    it.rewindRaids = rewindRaids
    it.rewindScoreboard = rewindScoreboard
    it.rewindWorldBorder = rewindWorldBorder
    it.rewindFreeCamRestorePosition = rewindFreeCamRestorePosition
    it.rewindShowStats = rewindShowStats
}

