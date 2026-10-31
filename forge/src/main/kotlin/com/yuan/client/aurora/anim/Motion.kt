package com.yuan.client.aurora.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.yuan.client.aurora.AuroraLayout
import kotlinx.coroutines.delay

/**
 * One timing table for the whole window.
 *
 * <p>Every duration and curve the UI uses is named here rather than typed at the call site. Two
 * reasons, both learned from the V9 screen: the same interaction must feel the same everywhere (a
 * hover that takes 120ms in one row and 220ms in the next reads as a bug), and tuning the whole
 * window's feel must be one edit, not thirty.
 *
 * <p>The table is built around the staging of the opening sequence: 90ms between successive items,
 * short enough that a page finishes composing in under half a second even with six rows, long enough
 * that the eye reads it as a sweep rather than a flash.
 */
internal object Motion {

    /**
     * The level of motion in force, set from the layout file at window open.
     *
     * <p>A mutable global rather than a composition local, deliberately: the durations are used
     * inside `animate*AsState` calls and inside non-composable helpers, and threading a parameter
     * through every one of those would put a motion argument on every function signature in the
     * package. The value is read when an animation starts, and the window is rebuilt on the open
     * that changes it, so a stale read cannot outlive the frame it was set on.
     */
    @Volatile
    var level: AuroraLayout.MotionLevel = AuroraLayout.MotionLevel.FULL

    /** True when any motion at all is allowed. */
    val animated: Boolean get() = level != AuroraLayout.MotionLevel.OFF

    /** True when entrance staging and the ambient pulse are allowed (i.e. FULL). */
    val staged: Boolean get() = level == AuroraLayout.MotionLevel.FULL

    /**
     * Scales a baseline duration by the level.
     *
     * <p>CALM keeps the short feedback but drops everything deliberate: a hover still acknowledges
     * within 110ms, while the 380ms window entrance collapses to nothing. That split is the point --
     * removing *all* feedback makes an interface feel broken rather than calm, and it is what a
     * naive reduce-motion switch gets wrong.
     */
    fun dur(baselineMs: Int): Int = when (level) {
        AuroraLayout.MotionLevel.OFF -> 0
        AuroraLayout.MotionLevel.CALM -> if (baselineMs <= QUICK_MS) baselineMs else 0
        AuroraLayout.MotionLevel.FULL -> baselineMs
    }

    /** Between successive items in a staggered entrance. */
    const val STAGGER_MS = 90

    /**
     * Items past this index stop adding delay; otherwise a long page would take seconds to compose
     * and the last rows would arrive after the user has already started reading.
     */
    const val STAGGER_MAX_STEPS = 5

    // ---- durations, short to long ----
    /** Hover, press, focus. Must be shorter than the eye's "instant" threshold (~130ms). */
    const val INSTANT_MS = 110

    /** Reveal of a control's own state: selected pill, underline, toggle knob. */
    const val QUICK_MS = 170

    /** The workhorse: values, sizes, positions that carry meaning. */
    const val BASE_MS = 240

    /** Deliberate, watched transitions: page changes, panel swaps. */
    const val SLOW_MS = 320

    /** Window open/close. The whole thing should feel like one gesture. */
    const val WINDOW_MS = 380

    /**
     * A gentle overshoot for things that should feel physical: knobs snapping, cards settling.
     * Critically damped would look inert on a 10px travel; this has a bounce just under visible.
     */
    val settle: SpringSpec<Float> = spring(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        visibilityThreshold = 0.01f
    )

    /** No overshoot at all: for values where a bounce would read as an error (slider readouts). */
    val glide: SpringSpec<Float> = spring(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
    )

    /** Content entering: decelerates into place, so it reads as arriving rather than thrown. */
    val enter: Easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)

    /** Content leaving: accelerates away, so attention does not follow it out. */
    val exit: Easing = FastOutLinearInEasing

    /** Sizes and positions that should not call attention to themselves. */
    val standard: Easing = FastOutSlowInEasing

    /** For a number that is being counted up: slow start, long ease-out tail. */
    val count: Easing = LinearOutSlowInEasing
}

/**
 * Delay for the item at [index] in a staggered entrance, in milliseconds.
 *
 * <p>Capped by [Motion.STAGGER_MAX_STEPS] so a page with twenty rows does not spend two seconds
 * assembling: past the cap everything enters together, which still reads as a sweep. Zero at any
 * level below FULL, so a calm window has no staging to wait through.
 */
internal fun staggerDelay(index: Int): Int =
    if (!Motion.staged) 0 else Motion.STAGGER_MS * index.coerceAtMost(Motion.STAGGER_MAX_STEPS)

/**
 * A one-shot entrance for a block: fade up and slide in from [fromY], after [delayMs].
 *
 * <p>Implemented as a local flag flipped by a [LaunchedEffect] rather than by animating on first
 * composition, because the animation has to start *after* the delay: animating immediately and
 * offsetting the whole thing by a delay would make the block appear in its final position first,
 * then jump back and travel -- which is exactly the flicker this exists to avoid.
 *
 * <p>Below FULL the block is simply present: no delay, no travel, no fade. An entrance that still
 * animated at reduced strength would be worse than none, because the body would still be asked to
 * track movement it did not want.
 *
 * @param identity a STABLE value for this block within its list.
 *
 * <p>THE BUG THIS PARAMETER EXISTS TO PREVENT. [staggerDelay] is capped at
 * [Motion.STAGGER_MAX_STEPS], so every row past the cap receives the SAME `delayMs`. The effect key
 * used to be `delayMs` alone, and `remember { mutableStateOf(false) }` is scoped by position: when
 * a list reordered, rows that happened to share a delay reused each other's effect AND each other's
 * flag, so a row could inherit `shown = true` from a neighbour (no animation, harmless) or -- after
 * a reorder -- keep a flag whose effect had already completed elsewhere, leaving it permanently at
 * `shown = false`, i.e. fully transparent. That is a row that exists, takes layout space, and
 * cannot be seen or clicked.
 *
 * <p>Callers pass the row's own key (an id, not an index) so the effect and the flag belong to
 * that row and survive reordering.
 */
@Composable
internal fun EnterBlock(
    delayMs: Int,
    identity: Any,
    enabled: Boolean = true,
    fromY: Int = 10,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!Motion.staged || !enabled) {
        Column(modifier) { content() }
        return
    }
    // Keyed by identity, NOT by delay: a shared delay must not share an entrance.
    var shown by remember(identity) { mutableStateOf(false) }
    LaunchedEffect(identity, delayMs) {
        delay(delayMs.toLong())
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        // Gated through `dur`, not a raw baseline. The early return above already makes this block
        // inert below FULL, so the two are equivalent TODAY -- but a raw baseline means the gating
        // lives only in that one condition, and relaxing it (a "calm entrance" that still fades,
        // say) would silently start animating at full speed for a motion-sensitive player.
        animationSpec = tween(Motion.dur(Motion.BASE_MS), easing = Motion.enter),
        label = "enterBlock"
    )
    Column(
        modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * fromY
        }
    ) {
        content()
    }
}

/**
 * A one-shot 0..1 ramp for a surface that is being opened, e.g. a floating panel appearing.
 *
 * <p>WHY THIS IS AN [Animatable] AND NOT `animateFloatAsState(targetValue = 1f)`. That is what this
 * used to be, and it never animated: an `animate*AsState` is created already AT its target, with no
 * transition to run, so the value was `1f` on the first composition and stayed there. The function
 * therefore had no callers -- not because nobody wanted a panel entrance, but because it did
 * nothing when they tried. The regression it caused was the real complaint behind "动画极致流畅":
 * a floating panel appeared INSTANTLY, with no fade and no scale, which reads as a pop rather than
 * as a surface arriving.
 *
 * <p>Reading it must happen in a DRAW phase (`graphicsLayer { }` / `drawBehind { }`). The value
 * changes every frame while the ramp runs, and composition-phase reads would recompose the whole
 * subtree for each of those frames.
 *
 * @param key identifies the surface, so two panels opening at once each get their own ramp.
 */
@Composable
internal fun openProgress(key: Any): State<Float> {
    // At OFF there is nothing to animate and the surface must simply be present.
    if (!Motion.animated) {
        return remember(key) { mutableStateOf(1f) }
    }
    val anim = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        anim.animateTo(1f, tween(Motion.dur(Motion.WINDOW_MS), easing = Motion.enter))
    }
    return anim.asState()
}

/**
 * A repeating slow pulse, 0..1..0, for decorative glow only.
 *
 * <p>ONLY DRAWING CODE MAY READ THIS. It is a State that changes every single frame; reading it
 * during composition recomposes whatever read it, and everything under that, sixty times a second
 * for as long as the window is open. The one legal consumer is a draw lambda
 * (`drawBehind { }`, `graphicsLayer { }`, `Canvas`), where the read costs a repaint and nothing
 * more. Its one caller does exactly that -- see the panel's ambient edge in AuroraShell.
 *
 * <p>Returns a constant at rest below FULL rather than a slow animation, so a motion-sensitive
 * player does not get a glow breathing at them for the whole session.
 */
@Composable
internal fun rememberAmbientPulse(periodMs: Int = 4200): State<Float> {
    if (!Motion.staged) {
        return remember { mutableStateOf(0.45f) }
    }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(periodMs) {
        while (true) {
            anim.animateTo(1f, tween(periodMs / 2, easing = Motion.standard))
            anim.animateTo(0f, tween(periodMs / 2, easing = Motion.standard))
        }
    }
    return anim.asState()
}
