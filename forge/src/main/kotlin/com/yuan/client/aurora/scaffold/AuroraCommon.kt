package com.yuan.client.aurora.scaffold

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.verticalScroll
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
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

/**
 * Scrolling with the thin bar attached, and the wheel eased rather than cut.
 *
 * <p>ORDER IS LOAD-BEARING. [smoothWheel] sits INSIDE [verticalScroll], and Compose dispatches the
 * Main pass child-first, so the inner handler sees a wheel event before the scroller does. It
 * consumes the notch and drives the glide; the scroller underneath therefore never sees the wheel
 * and keeps owning what it should -- drag, fling, and the scroll range. Putting the two in the other
 * order makes the scroller win, the notch jumps as before, and the glide never runs: the modifier
 * would be present, correct-looking, and inert.
 */
@Composable
internal fun Modifier.auroraScroll(state: ScrollState): Modifier =
    this.verticalScroll(state).smoothWheel(state).thinScrollbar(state)

/**
 * Eases the WHEEL's discrete notches into a short glide.
 *
 * <p>`verticalScroll` already flings, but only from a DRAG: a fling needs a release velocity, and a
 * wheel notch arrives as a jump with none. So every notch landed as an instant offset -- the list
 * teleported 56dp and stopped, which is the "hard" part of the scrolling rather than its speed.
 *
 * <p>This does not replace the scroll machinery. It eats the wheel event, measures where the notch
 * would put the list, and animates the remaining distance as a short ease-out, so a notch reads as a
 * push that settles. Several notches in quick succession RETARGET the one animation instead of
 * queueing, which is what makes a fast wheel spin feel like a single movement rather than a stutter.
 *
 * <p>Consuming is the point, not a side effect: if the scroller also handled the wheel, the list
 * would move twice per notch.
 */
@Composable
internal fun Modifier.smoothWheel(state: ScrollState): Modifier {
    // The animation is driven from composition, not from the pointer lambda: a pointer handler
    // cannot anchor an animation to the composition, and one launched ad hoc could outlive a
    // composable that left the tree mid-glide.
    var target by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(target) {
        val goal = target ?: return@LaunchedEffect
        // animateScrollTo steps ScrollState frame by frame and clamps each step to the live range,
        // so a notch arriving while the content is still measuring cannot overshoot the end.
        state.animateScrollTo(goal.roundToInt())
        // Clear only if no newer notch retargeted us while this one was running.
        if (target == goal) target = null
    }
    return this.pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull() ?: continue
                if (change.isConsumed) continue
                val dy = change.scrollDelta.y
                if (dy == 0f) continue
                // Retarget from wherever the glide currently is, so successive notches accumulate
                // into one movement instead of restarting from the resting position.
                val from = target ?: state.value.toFloat()
                target = (from + dy).coerceIn(0f, state.maxValue.toFloat())
                change.consume()
            }
        }
    }
}

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
