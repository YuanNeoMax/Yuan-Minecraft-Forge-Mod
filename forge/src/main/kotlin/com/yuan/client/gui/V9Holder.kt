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

/**
 * The live values behind the settings screen. Field names mirror {@link V9State} exactly so the
 * panels keep reading `state.x`; the difference is that a read now subscribes only the row that
 * made it, instead of the whole screen through a snapshot built during composition.
 */
internal class V9Holder(initial: V9State) {
    var enabled: Boolean by mutableStateOf(initial.enabled)
    var invulnerable: Boolean by mutableStateOf(initial.invulnerable)
    var grayScreen: Boolean by mutableStateOf(initial.grayScreen)
    var grayStrength: Float by mutableStateOf(initial.grayStrength)
    var grayAnimate: Boolean by mutableStateOf(initial.grayAnimate)
    var startAnim: Int by mutableStateOf(initial.startAnim)
    var endAnim: Int by mutableStateOf(initial.endAnim)
    var startDuration: Float by mutableStateOf(initial.startDuration)
    var endDuration: Float by mutableStateOf(initial.endDuration)
    var ballColor: Int by mutableStateOf(initial.ballColor)
    var ballColorCustom: Boolean by mutableStateOf(initial.ballColorCustom)
    var customColor: Int by mutableStateOf(initial.customColor)
    var particleSize: Float by mutableStateOf(initial.particleSize)
    var stopDuration: Float by mutableStateOf(initial.stopDuration)
    var triggerMode: Int by mutableStateOf(initial.triggerMode)
    var soundEnabled: Boolean by mutableStateOf(initial.soundEnabled)
    var cooldown: Int by mutableStateOf(initial.cooldown)
    var soundVolume: Float by mutableStateOf(initial.soundVolume)
    var particleAlpha: Float by mutableStateOf(initial.particleAlpha)
    var particleCount: Int by mutableStateOf(initial.particleCount)
    var particleSpin: Float by mutableStateOf(initial.particleSpin)
    var freezeSelf: Boolean by mutableStateOf(initial.freezeSelf)
    var stopRadius: Float by mutableStateOf(initial.stopRadius)
    var freezeEntities: Boolean by mutableStateOf(initial.freezeEntities)
    var freezeBlocks: Boolean by mutableStateOf(initial.freezeBlocks)
    var freezeFluids: Boolean by mutableStateOf(initial.freezeFluids)
    var freezeBossAI: Boolean by mutableStateOf(initial.freezeBossAI)
    var showMessage: Boolean by mutableStateOf(initial.showMessage)
    var grayStyle: Int by mutableStateOf(initial.grayStyle)
    var slashEnabled: Boolean by mutableStateOf(initial.slashEnabled)
    var slashDepthTest: Boolean by mutableStateOf(initial.slashDepthTest)
    var slashRandomAngle: Boolean by mutableStateOf(initial.slashRandomAngle)
    var slashGlow: Boolean by mutableStateOf(initial.slashGlow)
    var slashDuration: Float by mutableStateOf(initial.slashDuration)
    var slashLengthMult: Float by mutableStateOf(initial.slashLengthMult)
    var slashWidthRatio: Float by mutableStateOf(initial.slashWidthRatio)
    var slashThicknessRatio: Float by mutableStateOf(initial.slashThicknessRatio)
    var slashTipFade: Float by mutableStateOf(initial.slashTipFade)
    var slashStartScale: Float by mutableStateOf(initial.slashStartScale)
    var slashEndScale: Float by mutableStateOf(initial.slashEndScale)
    var slashCoreWidth: Float by mutableStateOf(initial.slashCoreWidth)
    var slashCoreShade: Float by mutableStateOf(initial.slashCoreShade)
    var slashEdgeWidth: Float by mutableStateOf(initial.slashEdgeWidth)
    var slashEdgeBrightness: Float by mutableStateOf(initial.slashEdgeBrightness)
    var slashGlowWidth: Float by mutableStateOf(initial.slashGlowWidth)
    var slashGlowStrength: Float by mutableStateOf(initial.slashGlowStrength)
    var slashNoiseStrength: Float by mutableStateOf(initial.slashNoiseStrength)
    var slashSweepSpeed: Float by mutableStateOf(initial.slashSweepSpeed)
    var slashSweepSoftness: Float by mutableStateOf(initial.slashSweepSoftness)
    var slashHoldFraction: Float by mutableStateOf(initial.slashHoldFraction)
    var slashFadeStart: Float by mutableStateOf(initial.slashFadeStart)
    var slashFadeDuration: Float by mutableStateOf(initial.slashFadeDuration)
    var slashSurfaceOffset: Float by mutableStateOf(initial.slashSurfaceOffset)
    var slashRollRange: Float by mutableStateOf(initial.slashRollRange)
    var slashCoreColor: Int by mutableStateOf(initial.slashCoreColor)
    var slashEdgeColor: Int by mutableStateOf(initial.slashEdgeColor)
    var slashGlowColor: Int by mutableStateOf(initial.slashGlowColor)
    var renderStyle: Int by mutableStateOf(initial.renderStyle)
    var silkColor0: Int by mutableStateOf(initial.silkColor0)
    var silkColor1: Int by mutableStateOf(initial.silkColor1)
    var silkColor2: Int by mutableStateOf(initial.silkColor2)
    var silkColor3: Int by mutableStateOf(initial.silkColor3)
    var silkColor4: Int by mutableStateOf(initial.silkColor4)
    var silkColor5: Int by mutableStateOf(initial.silkColor5)
    var silkColor6: Int by mutableStateOf(initial.silkColor6)
    var silkColor7: Int by mutableStateOf(initial.silkColor7)
    var silkBrightness: Float by mutableStateOf(initial.silkBrightness)
    var silkContrast: Float by mutableStateOf(initial.silkContrast)
    var silkSaturation: Float by mutableStateOf(initial.silkSaturation)
    var silkScale: Float by mutableStateOf(initial.silkScale)
    var silkIntensity: Float by mutableStateOf(initial.silkIntensity)
    var silkWarp: Float by mutableStateOf(initial.silkWarp)
    var silkDetail: Float by mutableStateOf(initial.silkDetail)
    var silkHue: Float by mutableStateOf(initial.silkHue)
    var silkSeed: Float by mutableStateOf(initial.silkSeed)
    var silkRotation: Float by mutableStateOf(initial.silkRotation)
    var silkDrift: Float by mutableStateOf(initial.silkDrift)
    var silkVignette: Float by mutableStateOf(initial.silkVignette)
    var silkBlur: Float by mutableStateOf(initial.silkBlur)
    var silkGrain: Float by mutableStateOf(initial.silkGrain)
    var tunnelSpeed: Float by mutableStateOf(initial.tunnelSpeed)
    var tunnelBrightness: Float by mutableStateOf(initial.tunnelBrightness)
    var tunnelDensity: Float by mutableStateOf(initial.tunnelDensity)
    var tunnelFov: Float by mutableStateOf(initial.tunnelFov)
    var voronoiColorCount: Int by mutableStateOf(initial.voronoiColorCount)
    var voronoiStepsPerColor: Int by mutableStateOf(initial.voronoiStepsPerColor)
    var voronoiColor0: Int by mutableStateOf(initial.voronoiColor0)
    var voronoiColor1: Int by mutableStateOf(initial.voronoiColor1)
    var voronoiColor2: Int by mutableStateOf(initial.voronoiColor2)
    var voronoiColor3: Int by mutableStateOf(initial.voronoiColor3)
    var voronoiColor4: Int by mutableStateOf(initial.voronoiColor4)
    var voronoiColorGlow: Int by mutableStateOf(initial.voronoiColorGlow)
    var voronoiColorGap: Int by mutableStateOf(initial.voronoiColorGap)
    var voronoiDistortion: Float by mutableStateOf(initial.voronoiDistortion)
    var voronoiGap: Float by mutableStateOf(initial.voronoiGap)
    var voronoiGlow: Float by mutableStateOf(initial.voronoiGlow)
    var voronoiScale: Float by mutableStateOf(initial.voronoiScale)
    var voronoiFov: Float by mutableStateOf(initial.voronoiFov)
    var voronoiSpeed: Float by mutableStateOf(initial.voronoiSpeed)
    var voronoiRotation: Float by mutableStateOf(initial.voronoiRotation)
    var voronoiOffsetX: Float by mutableStateOf(initial.voronoiOffsetX)
    var voronoiOffsetY: Float by mutableStateOf(initial.voronoiOffsetY)
    var slashStarfield: Boolean by mutableStateOf(initial.slashStarfield)
    var slashStarDensity: Float by mutableStateOf(initial.slashStarDensity)
    var slashStarBrightness: Float by mutableStateOf(initial.slashStarBrightness)
    var slashStarSize: Float by mutableStateOf(initial.slashStarSize)
    var slashStarColorMode: Int by mutableStateOf(initial.slashStarColorMode)
    var uiTabVariant: Int by mutableStateOf(initial.uiTabVariant)
    var uiTabSize: Int by mutableStateOf(initial.uiTabSize)
    var uiTabCompact: Boolean by mutableStateOf(initial.uiTabCompact)
    var uiSliderHeight: Int by mutableStateOf(initial.uiSliderHeight)
    var uiSliderThumb: Int by mutableStateOf(initial.uiSliderThumb)
    var uiSearchStyle: Int by mutableStateOf(initial.uiSearchStyle)
    var uiSearchWidth: Float by mutableStateOf(initial.uiSearchWidth)
    var uiSearchHeight: Float by mutableStateOf(initial.uiSearchHeight)
    var uiSearchRadius: Float by mutableStateOf(initial.uiSearchRadius)
    var uiSwitchStyle: Int by mutableStateOf(initial.uiSwitchStyle)
    var uiSwitchWidth: Float by mutableStateOf(initial.uiSwitchWidth)
    var uiSwitchHeight: Float by mutableStateOf(initial.uiSwitchHeight)
    var uiSwitchKnob: Float by mutableStateOf(initial.uiSwitchKnob)
    var uiSwitchGradientA: Int by mutableStateOf(initial.uiSwitchGradientA)
    var uiSwitchGradientB: Int by mutableStateOf(initial.uiSwitchGradientB)
    var uiSwitchTrackOff: Int by mutableStateOf(initial.uiSwitchTrackOff)
    var uiSwitchSlideTrack: Int by mutableStateOf(initial.uiSwitchSlideTrack)
    var uiSwitchSlideOff: Int by mutableStateOf(initial.uiSwitchSlideOff)
    var uiSwitchSlideOn: Int by mutableStateOf(initial.uiSwitchSlideOn)
    var uiSwitchTextTrackOff: Int by mutableStateOf(initial.uiSwitchTextTrackOff)
    var uiSwitchTextTrackOn: Int by mutableStateOf(initial.uiSwitchTextTrackOn)
    var uiSwitchTextKnob: Int by mutableStateOf(initial.uiSwitchTextKnob)
    var uiSwitchTextColor: Int by mutableStateOf(initial.uiSwitchTextColor)
    var uiSwitchTextOn: String by mutableStateOf(initial.uiSwitchTextOn)
    var uiSwitchTextOff: String by mutableStateOf(initial.uiSwitchTextOff)
    var domainRadius: Float by mutableStateOf(initial.domainRadius)
    var rewindEnabled: Boolean by mutableStateOf(initial.rewindEnabled)
    var rewindWindowSeconds: Int by mutableStateOf(initial.rewindWindowSeconds)
    var rewindScopeMode: Int by mutableStateOf(initial.rewindScopeMode)
    var rewindScope: Int by mutableStateOf(initial.rewindScope)
    var rewindRadius: Float by mutableStateOf(initial.rewindRadius)
    var rewindCooldownTicks: Int by mutableStateOf(initial.rewindCooldownTicks)
    var rewindPlaybackMode: Int by mutableStateOf(initial.rewindPlaybackMode)
    var rewindPlaybackSeconds: Float by mutableStateOf(initial.rewindPlaybackSeconds)
    var rewindRestoreOrder: Int by mutableStateOf(initial.rewindRestoreOrder)
    var rewindCameraMode: Int by mutableStateOf(initial.rewindCameraMode)
    var rewindPositionRewind: Boolean by mutableStateOf(initial.rewindPositionRewind)
    var rewindPositionMode: Int by mutableStateOf(initial.rewindPositionMode)
    var rewindPlayerState: Boolean by mutableStateOf(initial.rewindPlayerState)
    var rewindDeathEnabled: Boolean by mutableStateOf(initial.rewindDeathEnabled)
    var rewindDeathCooldownTicks: Int by mutableStateOf(initial.rewindDeathCooldownTicks)
    var rewindDeathMaxRetries: Int by mutableStateOf(initial.rewindDeathMaxRetries)
    var rewindSafetyCheckpoint: Boolean by mutableStateOf(initial.rewindSafetyCheckpoint)
    var rewindHostileCheck: Boolean by mutableStateOf(initial.rewindHostileCheck)
    var rewindOtherItemDeduct: Boolean by mutableStateOf(initial.rewindOtherItemDeduct)
    var rewindFreezeOthers: Boolean by mutableStateOf(initial.rewindFreezeOthers)
    var rewindTimestopStacking: Boolean by mutableStateOf(initial.rewindTimestopStacking)
    var rewindBlocks: Boolean by mutableStateOf(initial.rewindBlocks)
    var rewindBlockEntities: Boolean by mutableStateOf(initial.rewindBlockEntities)
    var rewindEntities: Boolean by mutableStateOf(initial.rewindEntities)
    var rewindItems: Boolean by mutableStateOf(initial.rewindItems)
    var rewindExperience: Boolean by mutableStateOf(initial.rewindExperience)
    var rewindTime: Boolean by mutableStateOf(initial.rewindTime)
    var rewindWeather: Boolean by mutableStateOf(initial.rewindWeather)
    var rewindRaids: Boolean by mutableStateOf(initial.rewindRaids)
    var rewindScoreboard: Boolean by mutableStateOf(initial.rewindScoreboard)
    var rewindWorldBorder: Boolean by mutableStateOf(initial.rewindWorldBorder)
    var rewindFreeCamRestorePosition: Boolean by mutableStateOf(initial.rewindFreeCamRestorePosition)
    var rewindShowStats: Boolean by mutableStateOf(initial.rewindShowStats)

    fun snapshot(): V9State = V9State(
        enabled = enabled,
        invulnerable = invulnerable,
        grayScreen = grayScreen,
        grayStrength = grayStrength,
        grayAnimate = grayAnimate,
        startAnim = startAnim,
        endAnim = endAnim,
        startDuration = startDuration,
        endDuration = endDuration,
        ballColor = ballColor,
        ballColorCustom = ballColorCustom,
        customColor = customColor,
        particleSize = particleSize,
        stopDuration = stopDuration,
        triggerMode = triggerMode,
        soundEnabled = soundEnabled,
        cooldown = cooldown,
        soundVolume = soundVolume,
        particleAlpha = particleAlpha,
        particleCount = particleCount,
        particleSpin = particleSpin,
        freezeSelf = freezeSelf,
        stopRadius = stopRadius,
        freezeEntities = freezeEntities,
        freezeBlocks = freezeBlocks,
        freezeFluids = freezeFluids,
        freezeBossAI = freezeBossAI,
        showMessage = showMessage,
        grayStyle = grayStyle,
        slashEnabled = slashEnabled,
        slashDepthTest = slashDepthTest,
        slashRandomAngle = slashRandomAngle,
        slashGlow = slashGlow,
        slashDuration = slashDuration,
        slashLengthMult = slashLengthMult,
        slashWidthRatio = slashWidthRatio,
        slashThicknessRatio = slashThicknessRatio,
        slashTipFade = slashTipFade,
        slashStartScale = slashStartScale,
        slashEndScale = slashEndScale,
        slashCoreWidth = slashCoreWidth,
        slashCoreShade = slashCoreShade,
        slashEdgeWidth = slashEdgeWidth,
        slashEdgeBrightness = slashEdgeBrightness,
        slashGlowWidth = slashGlowWidth,
        slashGlowStrength = slashGlowStrength,
        slashNoiseStrength = slashNoiseStrength,
        slashSweepSpeed = slashSweepSpeed,
        slashSweepSoftness = slashSweepSoftness,
        slashHoldFraction = slashHoldFraction,
        slashFadeStart = slashFadeStart,
        slashFadeDuration = slashFadeDuration,
        slashSurfaceOffset = slashSurfaceOffset,
        slashRollRange = slashRollRange,
        slashCoreColor = slashCoreColor,
        slashEdgeColor = slashEdgeColor,
        slashGlowColor = slashGlowColor,
        renderStyle = renderStyle,
        silkColor0 = silkColor0,
        silkColor1 = silkColor1,
        silkColor2 = silkColor2,
        silkColor3 = silkColor3,
        silkColor4 = silkColor4,
        silkColor5 = silkColor5,
        silkColor6 = silkColor6,
        silkColor7 = silkColor7,
        silkBrightness = silkBrightness,
        silkContrast = silkContrast,
        silkSaturation = silkSaturation,
        silkScale = silkScale,
        silkIntensity = silkIntensity,
        silkWarp = silkWarp,
        silkDetail = silkDetail,
        silkHue = silkHue,
        silkSeed = silkSeed,
        silkRotation = silkRotation,
        silkDrift = silkDrift,
        silkVignette = silkVignette,
        silkBlur = silkBlur,
        silkGrain = silkGrain,
        tunnelSpeed = tunnelSpeed,
        tunnelBrightness = tunnelBrightness,
        tunnelDensity = tunnelDensity,
        tunnelFov = tunnelFov,
        voronoiColorCount = voronoiColorCount,
        voronoiStepsPerColor = voronoiStepsPerColor,
        voronoiColor0 = voronoiColor0,
        voronoiColor1 = voronoiColor1,
        voronoiColor2 = voronoiColor2,
        voronoiColor3 = voronoiColor3,
        voronoiColor4 = voronoiColor4,
        voronoiColorGlow = voronoiColorGlow,
        voronoiColorGap = voronoiColorGap,
        voronoiDistortion = voronoiDistortion,
        voronoiGap = voronoiGap,
        voronoiGlow = voronoiGlow,
        voronoiScale = voronoiScale,
        voronoiFov = voronoiFov,
        voronoiSpeed = voronoiSpeed,
        voronoiRotation = voronoiRotation,
        voronoiOffsetX = voronoiOffsetX,
        voronoiOffsetY = voronoiOffsetY,
        slashStarfield = slashStarfield,
        slashStarDensity = slashStarDensity,
        slashStarBrightness = slashStarBrightness,
        slashStarSize = slashStarSize,
        slashStarColorMode = slashStarColorMode,
        uiTabVariant = uiTabVariant,
        uiTabSize = uiTabSize,
        uiTabCompact = uiTabCompact,
        uiSliderHeight = uiSliderHeight,
        uiSliderThumb = uiSliderThumb,
        uiSearchStyle = uiSearchStyle,
        uiSearchWidth = uiSearchWidth,
        uiSearchHeight = uiSearchHeight,
        uiSearchRadius = uiSearchRadius,
        uiSwitchStyle = uiSwitchStyle,
        uiSwitchWidth = uiSwitchWidth,
        uiSwitchHeight = uiSwitchHeight,
        uiSwitchKnob = uiSwitchKnob,
        uiSwitchGradientA = uiSwitchGradientA,
        uiSwitchGradientB = uiSwitchGradientB,
        uiSwitchTrackOff = uiSwitchTrackOff,
        uiSwitchSlideTrack = uiSwitchSlideTrack,
        uiSwitchSlideOff = uiSwitchSlideOff,
        uiSwitchSlideOn = uiSwitchSlideOn,
        uiSwitchTextTrackOff = uiSwitchTextTrackOff,
        uiSwitchTextTrackOn = uiSwitchTextTrackOn,
        uiSwitchTextKnob = uiSwitchTextKnob,
        uiSwitchTextColor = uiSwitchTextColor,
        uiSwitchTextOn = uiSwitchTextOn,
        uiSwitchTextOff = uiSwitchTextOff,
        domainRadius = domainRadius,
        rewindEnabled = rewindEnabled,
        rewindWindowSeconds = rewindWindowSeconds,
        rewindScopeMode = rewindScopeMode,
        rewindScope = rewindScope,
        rewindRadius = rewindRadius,
        rewindCooldownTicks = rewindCooldownTicks,
        rewindPlaybackMode = rewindPlaybackMode,
        rewindPlaybackSeconds = rewindPlaybackSeconds,
        rewindRestoreOrder = rewindRestoreOrder,
        rewindCameraMode = rewindCameraMode,
        rewindPositionRewind = rewindPositionRewind,
        rewindPositionMode = rewindPositionMode,
        rewindPlayerState = rewindPlayerState,
        rewindDeathEnabled = rewindDeathEnabled,
        rewindDeathCooldownTicks = rewindDeathCooldownTicks,
        rewindDeathMaxRetries = rewindDeathMaxRetries,
        rewindSafetyCheckpoint = rewindSafetyCheckpoint,
        rewindHostileCheck = rewindHostileCheck,
        rewindOtherItemDeduct = rewindOtherItemDeduct,
        rewindFreezeOthers = rewindFreezeOthers,
        rewindTimestopStacking = rewindTimestopStacking,
        rewindBlocks = rewindBlocks,
        rewindBlockEntities = rewindBlockEntities,
        rewindEntities = rewindEntities,
        rewindItems = rewindItems,
        rewindExperience = rewindExperience,
        rewindTime = rewindTime,
        rewindWeather = rewindWeather,
        rewindRaids = rewindRaids,
        rewindScoreboard = rewindScoreboard,
        rewindWorldBorder = rewindWorldBorder,
        rewindFreeCamRestorePosition = rewindFreeCamRestorePosition,
        rewindShowStats = rewindShowStats
    )

    fun restore(s: V9State) {
        enabled = s.enabled
        invulnerable = s.invulnerable
        grayScreen = s.grayScreen
        grayStrength = s.grayStrength
        grayAnimate = s.grayAnimate
        startAnim = s.startAnim
        endAnim = s.endAnim
        startDuration = s.startDuration
        endDuration = s.endDuration
        ballColor = s.ballColor
        ballColorCustom = s.ballColorCustom
        customColor = s.customColor
        particleSize = s.particleSize
        stopDuration = s.stopDuration
        triggerMode = s.triggerMode
        soundEnabled = s.soundEnabled
        cooldown = s.cooldown
        soundVolume = s.soundVolume
        particleAlpha = s.particleAlpha
        particleCount = s.particleCount
        particleSpin = s.particleSpin
        freezeSelf = s.freezeSelf
        stopRadius = s.stopRadius
        freezeEntities = s.freezeEntities
        freezeBlocks = s.freezeBlocks
        freezeFluids = s.freezeFluids
        freezeBossAI = s.freezeBossAI
        showMessage = s.showMessage
        grayStyle = s.grayStyle
        slashEnabled = s.slashEnabled
        slashDepthTest = s.slashDepthTest
        slashRandomAngle = s.slashRandomAngle
        slashGlow = s.slashGlow
        slashDuration = s.slashDuration
        slashLengthMult = s.slashLengthMult
        slashWidthRatio = s.slashWidthRatio
        slashThicknessRatio = s.slashThicknessRatio
        slashTipFade = s.slashTipFade
        slashStartScale = s.slashStartScale
        slashEndScale = s.slashEndScale
        slashCoreWidth = s.slashCoreWidth
        slashCoreShade = s.slashCoreShade
        slashEdgeWidth = s.slashEdgeWidth
        slashEdgeBrightness = s.slashEdgeBrightness
        slashGlowWidth = s.slashGlowWidth
        slashGlowStrength = s.slashGlowStrength
        slashNoiseStrength = s.slashNoiseStrength
        slashSweepSpeed = s.slashSweepSpeed
        slashSweepSoftness = s.slashSweepSoftness
        slashHoldFraction = s.slashHoldFraction
        slashFadeStart = s.slashFadeStart
        slashFadeDuration = s.slashFadeDuration
        slashSurfaceOffset = s.slashSurfaceOffset
        slashRollRange = s.slashRollRange
        slashCoreColor = s.slashCoreColor
        slashEdgeColor = s.slashEdgeColor
        slashGlowColor = s.slashGlowColor
        renderStyle = s.renderStyle
        silkColor0 = s.silkColor0
        silkColor1 = s.silkColor1
        silkColor2 = s.silkColor2
        silkColor3 = s.silkColor3
        silkColor4 = s.silkColor4
        silkColor5 = s.silkColor5
        silkColor6 = s.silkColor6
        silkColor7 = s.silkColor7
        silkBrightness = s.silkBrightness
        silkContrast = s.silkContrast
        silkSaturation = s.silkSaturation
        silkScale = s.silkScale
        silkIntensity = s.silkIntensity
        silkWarp = s.silkWarp
        silkDetail = s.silkDetail
        silkHue = s.silkHue
        silkSeed = s.silkSeed
        silkRotation = s.silkRotation
        silkDrift = s.silkDrift
        silkVignette = s.silkVignette
        silkBlur = s.silkBlur
        silkGrain = s.silkGrain
        tunnelSpeed = s.tunnelSpeed
        tunnelBrightness = s.tunnelBrightness
        tunnelDensity = s.tunnelDensity
        tunnelFov = s.tunnelFov
        voronoiColorCount = s.voronoiColorCount
        voronoiStepsPerColor = s.voronoiStepsPerColor
        voronoiColor0 = s.voronoiColor0
        voronoiColor1 = s.voronoiColor1
        voronoiColor2 = s.voronoiColor2
        voronoiColor3 = s.voronoiColor3
        voronoiColor4 = s.voronoiColor4
        voronoiColorGlow = s.voronoiColorGlow
        voronoiColorGap = s.voronoiColorGap
        voronoiDistortion = s.voronoiDistortion
        voronoiGap = s.voronoiGap
        voronoiGlow = s.voronoiGlow
        voronoiScale = s.voronoiScale
        voronoiFov = s.voronoiFov
        voronoiSpeed = s.voronoiSpeed
        voronoiRotation = s.voronoiRotation
        voronoiOffsetX = s.voronoiOffsetX
        voronoiOffsetY = s.voronoiOffsetY
        slashStarfield = s.slashStarfield
        slashStarDensity = s.slashStarDensity
        slashStarBrightness = s.slashStarBrightness
        slashStarSize = s.slashStarSize
        slashStarColorMode = s.slashStarColorMode
        uiTabVariant = s.uiTabVariant
        uiTabSize = s.uiTabSize
        uiTabCompact = s.uiTabCompact
        uiSliderHeight = s.uiSliderHeight
        uiSliderThumb = s.uiSliderThumb
        uiSearchStyle = s.uiSearchStyle
        uiSearchWidth = s.uiSearchWidth
        uiSearchHeight = s.uiSearchHeight
        uiSearchRadius = s.uiSearchRadius
        uiSwitchStyle = s.uiSwitchStyle
        uiSwitchWidth = s.uiSwitchWidth
        uiSwitchHeight = s.uiSwitchHeight
        uiSwitchKnob = s.uiSwitchKnob
        uiSwitchGradientA = s.uiSwitchGradientA
        uiSwitchGradientB = s.uiSwitchGradientB
        uiSwitchTrackOff = s.uiSwitchTrackOff
        uiSwitchSlideTrack = s.uiSwitchSlideTrack
        uiSwitchSlideOff = s.uiSwitchSlideOff
        uiSwitchSlideOn = s.uiSwitchSlideOn
        uiSwitchTextTrackOff = s.uiSwitchTextTrackOff
        uiSwitchTextTrackOn = s.uiSwitchTextTrackOn
        uiSwitchTextKnob = s.uiSwitchTextKnob
        uiSwitchTextColor = s.uiSwitchTextColor
        uiSwitchTextOn = s.uiSwitchTextOn
        uiSwitchTextOff = s.uiSwitchTextOff
        domainRadius = s.domainRadius
        rewindEnabled = s.rewindEnabled
        rewindWindowSeconds = s.rewindWindowSeconds
        rewindScopeMode = s.rewindScopeMode
        rewindScope = s.rewindScope
        rewindRadius = s.rewindRadius
        rewindCooldownTicks = s.rewindCooldownTicks
        rewindPlaybackMode = s.rewindPlaybackMode
        rewindPlaybackSeconds = s.rewindPlaybackSeconds
        rewindRestoreOrder = s.rewindRestoreOrder
        rewindCameraMode = s.rewindCameraMode
        rewindPositionRewind = s.rewindPositionRewind
        rewindPositionMode = s.rewindPositionMode
        rewindPlayerState = s.rewindPlayerState
        rewindDeathEnabled = s.rewindDeathEnabled
        rewindDeathCooldownTicks = s.rewindDeathCooldownTicks
        rewindDeathMaxRetries = s.rewindDeathMaxRetries
        rewindSafetyCheckpoint = s.rewindSafetyCheckpoint
        rewindHostileCheck = s.rewindHostileCheck
        rewindOtherItemDeduct = s.rewindOtherItemDeduct
        rewindFreezeOthers = s.rewindFreezeOthers
        rewindTimestopStacking = s.rewindTimestopStacking
        rewindBlocks = s.rewindBlocks
        rewindBlockEntities = s.rewindBlockEntities
        rewindEntities = s.rewindEntities
        rewindItems = s.rewindItems
        rewindExperience = s.rewindExperience
        rewindTime = s.rewindTime
        rewindWeather = s.rewindWeather
        rewindRaids = s.rewindRaids
        rewindScoreboard = s.rewindScoreboard
        rewindWorldBorder = s.rewindWorldBorder
        rewindFreeCamRestorePosition = s.rewindFreeCamRestorePosition
        rewindShowStats = s.rewindShowStats
    }
}

