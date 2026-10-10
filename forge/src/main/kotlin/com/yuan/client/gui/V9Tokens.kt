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

