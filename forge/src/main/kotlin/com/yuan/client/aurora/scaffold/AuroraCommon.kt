package com.yuan.client.aurora.scaffold

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.yuan.client.aurora.theme.Star

/**
 * Modifiers the Aurora shell shares.
 *
 * <p>One file so no screen re-derives them. Each of these is a behaviour that was missing somewhere
 * in the older screens and had to be added per call site: press acknowledgement, a hand cursor, and a
 * scrollbar that appears only when there is something to scroll.
 */

/**
 * Whether the interaction source is pressed.
 *
 * <p>Every clickable here passes `indication = null`, because Material's ripple needs a colour from
 * an ambient theme this interface does not use. With nothing in its place a click has no
 * acknowledgement at all until its result appears, which on a slow frame reads as the click having
 * been dropped.
 */
@Composable
internal fun isPressed(interaction: MutableInteractionSource): Boolean =
    interaction.collectIsPressedAsState().value

/**
 * A hand cursor over a clickable.
 *
 * <p>The default arrow is what a non-interactive surface shows, and a module row looks the same
 * whether or not it does anything. The cursor is the only affordance that is unambiguous before a
 * click.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.handCursor(): Modifier = pointerHoverIcon(PointerIcon.Hand)

/**
 * A 3px bar that appears only while the content can scroll and is being moved.
 *
 * <p>A reserved gutter would eat width from every list and show an empty track most of the time;
 * overlaying it keeps the full column width and shows the cue exactly when it is informative. The
 * thumb is sized from the visible-to-total ratio, so it never implies more content than there is.
 */
internal fun Modifier.thinScrollbar(state: ScrollState): Modifier = drawBehind {
    if (state.maxValue <= 0) return@drawBehind
    val trackH = size.height
    val thumbH = (trackH * trackH / (trackH + state.maxValue)).coerceAtLeast(24f)
    val y = (state.value.toFloat() / state.maxValue) * (trackH - thumbH)
    drawRoundRect(
        color = Star.scrollbar,
        topLeft = Offset(size.width - 4f, y),
        size = Size(3f, thumbH),
        cornerRadius = CornerRadius(1.5f)
    )
}

/** Scrolling with the thin bar attached, so the two are never applied separately. */
internal fun Modifier.auroraScroll(state: ScrollState): Modifier =
    this.verticalScroll(state).thinScrollbar(state)

/**
 * The hairline under a header or above a footer.
 *
 * <p>A one-pixel line at half-pixel offset, which is what keeps it crisp instead of blurring across
 * two device pixels when the GUI scale is not an integer.
 */
internal fun Modifier.bottomHairline(): Modifier = drawBehind {
    drawLine(
        Star.hairline,
        Offset(0f, size.height - 0.5f),
        Offset(size.width, size.height - 0.5f),
        1f
    )
}

/** The same hairline along the left edge, for a rail that must not merge with the content. */
internal fun Modifier.rightHairline(): Modifier = drawBehind {
    drawLine(
        Star.hairline,
        Offset(size.width - 0.5f, 0f),
        Offset(size.width - 0.5f, size.height),
        1f
    )
}
