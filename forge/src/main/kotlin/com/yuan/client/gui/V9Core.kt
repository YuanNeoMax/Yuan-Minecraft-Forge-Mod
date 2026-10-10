package com.yuan.client.gui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key as ComposeKey
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mojang.blaze3d.systems.RenderSystem
import com.yuan.client.render.YuanComposeGlState
import com.yuan.Yuan
import com.yuan.item.YuanGodSwordConfig
import com.yuan.network.YuanGodSwordConfigPacket
import com.yuan.timestop.YuanTimeStop
import com.yuan.timestop.YuanTimeStopServerState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL21C
import org.lwjgl.opengl.GL30C
import java.awt.Component as AwtComponent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent as AwtKeyEvent
import java.awt.event.MouseEvent as AwtMouseEvent
import java.awt.event.MouseWheelEvent as AwtMouseWheelEvent
import java.nio.ByteBuffer
import java.time.LocalDate

/**
 * Design tokens for this screen. Colors used to be 215 inline literals over 66 distinct values - nine
 * near-duplicate dark surfaces and four near-duplicate greys - so nothing could be tuned globally.
 * The surface scale collapses values that differ by at most a few units per channel.
 *
 * Text roles are checked against the surfaces they sit on: tertiary is #8A94A2 rather than the old
 * #646D7B, which only reached 2.8:1 on inputs and 3.5:1 on panels (AA body text needs 4.5:1).
 */
internal object Ui {
    // surfaces, darkest to most raised
    val surface0 = Color(0xFF0B0D11)      // window
    val surface1 = Color(0xFF0F1217)      // panel base, sidebar bottom
    val surface2 = Color(0xFF12151B)      // material surface
    val surface3 = Color(0xFF1A1E26)      // cards, fields
    val surface4 = Color(0xFF242A33)      // slider track / input idle
    val surfaceHover = Color(0xFF2A2F38)
    val surfaceActive = Color(0xFF323842)
    val sunken = Color(0xFF0F1116)        // segmented container

    // text
    val textPrimary = Color(0xFFF2F4F8)
    val textBody = Color(0xFFD7DCE5)
    val textSecondary = Color(0xFF9AA2B0)
    val textTertiary = Color(0xFF8A94A2)
    val textOnAccent = Color(0xFF1B100C)

    // accent (coral)
    val accent = Color(0xFFFF7A5C)
    val accentHover = Color(0xFFFFA08A)
    val accentPressed = Color(0xFFE86848)
    val accentBright = Color(0xFFFF8A6E)
    val accentTint = Color(0x26FF7A5C)
    val accentTintStrong = Color(0x2BFF7A5C)

    // lines, overlays, shadows
    val hairlineFaint = Color(0x14E9ECF2)
    val hairlineSoft = Color(0x1FE9ECF2)
    val hairline = Color(0x24E9ECF2)
    val hairlineStrong = Color(0x38E9ECF2)
    val borderQuiet = Color(0x14FFFFFF)
    val borderDark = Color(0x1A000000)
    val overlayRowHover = Color(0x04FFFFFF)
    val overlayHover = Color(0x0FFFFFFF)
    val overlayPressed = Color(0x1FFFFFFF)
    val overlayFade = Color(0x0DFFFFFF)
    val innerHighlight = Color(0x2EFFFFFF)
    val innerHighlightStrong = Color(0x30FFFFFF)
    val innerLine = Color(0x24FFFFFF)
    val shadowSlight = Color(0x26000000)
    val shadowMedium = Color(0x33000000)
    val shadowWindow = Color(0x40000000)
    val shadowStrong = Color(0x66000000)
    val shadowAmbientSoft = Color(0xFF0E0E0E)
    val shadowSpotWarm = Color(0x405F5E5E)
    val scrim = Color(0x8C000000)
    val scrollbar = Color(0x33E9ECF2)

    // control hardware
    val thumb = Color(0xFFF4F6FA)
    val thumbEdge = Color(0xFF3A3F49)
    val knobTop = Color(0xFFFFFFFF)
    val knobBottom = Color(0xFFE6E9EF)
    val knobEdge = Color(0xFFDEDEDE)
    val knobOff = Color(0xFF9B9B9B)

    // decorative spectrum used by the preview glow
    val spectrum = listOf(Color(0xFFFF7A5C), Color(0xFF7C5CFF), Color(0xFF3DD6E8))
}


@OptIn(ExperimentalTextApi::class)
internal val cnFont: FontFamily = FontFamily("Microsoft YaHei")


/**
 * Live view over the holder's UI-preference fields. Reading a property subscribes only the row that
 * asked for it; this used to be a data class built in the composition root, so changing any of the 22
 * preferences recomposed the whole screen.
 */
internal class V9UiPrefs(private val h: V9Holder = V9Holder(V9State())) {
    val sliderHeight: Dp get() = when (h.uiSliderHeight) { 0 -> 20.dp; 2 -> 28.dp; else -> 24.dp }
    val sliderThumb: Dp get() = when (h.uiSliderThumb) { 0 -> 16.dp; 2 -> 20.dp; else -> 18.dp }
    val searchStyle: Int get() = h.uiSearchStyle
    val searchWidth: Dp get() = h.uiSearchWidth.dp
    val searchHeight: Dp get() = h.uiSearchHeight.dp
    val searchRadius: Dp get() = h.uiSearchRadius.dp
    val switchStyle: Int get() = h.uiSwitchStyle
    val switchWidth: Dp get() = h.uiSwitchWidth.dp
    val switchHeight: Dp get() = h.uiSwitchHeight.dp
    val switchKnob: Dp get() = h.uiSwitchKnob.dp
    val switchGradientA: Int get() = h.uiSwitchGradientA
    val switchGradientB: Int get() = h.uiSwitchGradientB
    val switchTrackOff: Int get() = h.uiSwitchTrackOff
    val switchSlideTrack: Int get() = h.uiSwitchSlideTrack
    val switchSlideOff: Int get() = h.uiSwitchSlideOff
    val switchSlideOn: Int get() = h.uiSwitchSlideOn
    val switchTextTrackOff: Int get() = h.uiSwitchTextTrackOff
    val switchTextTrackOn: Int get() = h.uiSwitchTextTrackOn
    val switchTextKnob: Int get() = h.uiSwitchTextKnob
    val switchTextColor: Int get() = h.uiSwitchTextColor
    val switchTextOn: String get() = h.uiSwitchTextOn
    val switchTextOff: String get() = h.uiSwitchTextOff
}


internal val LocalV9UiPrefs = compositionLocalOf { V9UiPrefs() }


/**
 * Every clickable in this screen passes indication = null, which silently threw away all press
 * acknowledgement. These helpers put it back as colour and cursor changes only, so no layout bounds
 * move and neighbouring rows never jitter.
 */
@Composable
internal fun isPressed(interaction: MutableInteractionSource): Boolean =
    interaction.collectIsPressedAsState().value


@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.handCursor(): Modifier = pointerHoverIcon(PointerIcon.Hand)


/**
 * Scroll positions live above the panels so leaving a page and coming back restores where you were.
 * A panel that is switched away simply stops composing; the registry keyed by name keeps its state.
 */
internal val LocalPanelScrolls = compositionLocalOf<SnapshotStateMap<String, ScrollState>> {
    mutableStateMapOf()
}


@Composable
internal fun rememberPanelScroll(key: String): ScrollState {
    val registry = LocalPanelScrolls.current
    return remember(key) { registry.getOrPut(key) { ScrollState(0) } }
}


@Composable
internal fun Modifier.panelScroll(key: String): Modifier {
    val state = rememberPanelScroll(key)
    return this.verticalScroll(state).thinScrollbar(state)
}


/**
 * A 3px bar that only appears while the panel can actually scroll and is being moved, so long pages
 * get a position cue without a permanent gutter.
 */
internal fun Modifier.thinScrollbar(state: ScrollState): Modifier = drawBehind {
    if (state.maxValue <= 0) return@drawBehind
    val trackH = size.height
    val thumbH = (trackH * trackH / (trackH + state.maxValue)).coerceAtLeast(24f)
    val y = (state.value.toFloat() / state.maxValue) * (trackH - thumbH)
    drawRoundRect(
        color = Ui.scrollbar,
        topLeft = Offset(size.width - 4f, y),
        size = Size(3f, thumbH),
        cornerRadius = CornerRadius(1.5f)
    )
}


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
    val domainRadius: Float = 20f,
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
    domainRadius = config.domainRadius,
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
    it.domainRadius = domainRadius
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


internal data class V9Row(
    val id: String,
    val name: String,
    val desc: String,
    val type: Int,
    val section: String,
    val hint: String
)


internal data class V9Section(val name: String, val hint: String, val rows: List<V9Row>)


internal val ballColors = listOf(
    0xFFFF7A5C, 0xFFE85D3D, 0xFFFFC9BA, 0xFF49322C,
    0xFF7C5CFF, 0xFF3DD6E8, 0xFF7AE85D, 0xFFFFD93D
)


internal val quotes = listOf(
    "星河长明，剑心不改。",
    "剑锋所指，即是我心。",
    "星云为骨，时间作刃。",
    "一瞬千年，一剑永恒。",
    "沉默的星，不灭的剑。"
)


internal val v9Sections = listOf(
    V9Section("基础", "时停是否可用，以及服务端规则。", listOf(
        V9Row("enabled", "启用时停", "允许触发时间停止", 0, "基础", "时停是否可用，以及服务端规则。"),
        V9Row("invulnerable", "时停无敌", "服务端权威生效", 0, "基础", "时停是否可用，以及服务端规则。"),
        V9Row("triggerMode", "触发方式", "右键 / Shift+右键 / 自定义键", 1, "基础", "时停是否可用，以及服务端规则。"),
        V9Row("showMessage", "触发提示", "动作栏显示开始/结束", 0, "基础", "时停是否可用，以及服务端规则。"),
        V9Row("cooldown", "冷却时间", "0 至 100 tick", 2, "基础", "时停是否可用，以及服务端规则。")
    )),
    V9Section("动画", "启动与关闭的视觉节奏。", listOf(
        V9Row("startAnim", "启动动画", "球体从玩家向外爆开", 1, "动画", "启动与关闭的视觉节奏。"),
        V9Row("endAnim", "关闭动画", "光球向内收束", 1, "动画", "启动与关闭的视觉节奏。"),
        V9Row("startDuration", "启动时长", "0.1 至 2 秒", 2, "动画", "启动与关闭的视觉节奏。"),
        V9Row("endDuration", "关闭时长", "0.1 至 2 秒", 2, "动画", "启动与关闭的视觉节奏。")
    )),
    V9Section("画面", "后处理滤镜强度。", listOf(
        V9Row("grayScreen", "黑白画面", "时停期间灰白后处理", 0, "画面", "后处理滤镜强度。"),
        V9Row("grayStrength", "黑白强度", "0 彩色 / 1 全黑白", 2, "画面", "后处理滤镜强度。"),
        V9Row("grayAnimate", "黑白随时间变化", "开启后饱和度脉动", 0, "画面", "后处理滤镜强度。"),
        V9Row("grayStyle", "黑白滤镜", "纯黑白 / 复古 / 暗角", 1, "画面", "后处理滤镜强度。")
    )),
    V9Section("音效", "时停音效。", listOf(
        V9Row("soundEnabled", "时停音效", "启动时播放时停音效", 0, "音效", "时停音效。"),
        V9Row("soundVolume", "音效音量", "0 至 100%", 2, "音效", "时停音效。")
    )),
    V9Section("光球", "光球外观与粒子效果。", listOf(
        V9Row("particleSize", "光球大小", "0.4 至 3.0", 2, "光球", "光球外观与粒子效果。"),
        V9Row("particleAlpha", "光球透明度", "0.1 至 1.0", 2, "光球", "光球外观与粒子效果。"),
        V9Row("particleCount", "粒子数量", "1 至 16 个", 2, "光球", "光球外观与粒子效果。"),
        V9Row("particleSpin", "粒子旋转", "0 至 3.0", 2, "光球", "光球外观与粒子效果。"),
        V9Row("ballColor", "光球颜色", "八种预设或自定义 RGB", 3, "光球", "光球外观与粒子效果。")
    )),
    V9Section("时长", "时停持续与自动结束。", listOf(
        V9Row("stopDuration", "时停持续时间", "0 为手动开关，最长 60 秒", 2, "时长", "时停持续与自动结束。")
    )),
    V9Section("冻结", "时停影响范围与对象。", listOf(
        V9Row("freezeSelf", "冻结自己", "开启后玩家自身也定身", 0, "冻结", "时停影响范围与对象。"),
        V9Row("stopRadius", "时停范围", "0 为全场，最大 128 格", 2, "冻结", "时停影响范围与对象。"),
        V9Row("freezeEntities", "冻结实体", "普通实体停止行动", 0, "冻结", "时停影响范围与对象。"),
        V9Row("freezeBlocks", "冻结方块", "方块刻与更新停止", 0, "冻结", "时停影响范围与对象。"),
        V9Row("freezeFluids", "冻结流体", "液体不再流动", 0, "冻结", "时停影响范围与对象。"),
        V9Row("freezeBossAI", "冻结 Boss AI", "Boss 也随实体冻结", 0, "冻结", "时停影响范围与对象。")
    )),
    V9Section("领域", "领域展开的范围。", listOf(
        V9Row("domainRadius", "领域半径", "同时决定地面网格截止、术式架尺寸与滤镜范围", 2, "领域", "领域展开的范围。"),
    )),
    V9Section("预设", "快速保存与载入整套配置。", listOf(
        V9Row("preset", "配置预设", "槽 1-3 保存/载入", 4, "预设", "快速保存与载入整套配置。")
    ))
)


internal val navSections = mapOf(
    "时停" to setOf("基础", "动画", "画面", "音效", "光球", "时长", "冻结", "领域", "预设"),
    "斩击" to setOf("斩击"),
    "武器渲染" to setOf("武器渲染"),
    "回溯" to setOf("回溯"),
    "设置" to setOf("设置")
)


internal val timestopTabs = listOf("基础", "效果", "音效", "冻结", "领域", "预设")


internal val timestopTabSections = mapOf(
    "基础" to setOf("基础", "时长"),
    "效果" to setOf("动画", "画面", "光球"),
    "音效" to setOf("音效"),
    "冻结" to setOf("冻结"),
    "领域" to setOf("领域"),
    "预设" to setOf("预设")
)

