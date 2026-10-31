package com.yuan.client.aurora.panels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The inline content area's layout, which MORPHS between one column and two.
 *
 * <p>WHY A CUSTOM LAYOUT. The area used to pick a branch off `maxWidth >= 560.dp` and compose either
 * a single Column or a Row of two Columns. Crossing that threshold replaced every child in one frame:
 * the same settings at the same order simply appeared somewhere else. That reads as a jump cut, and
 * no amount of easing on the replacement fixes it -- the two layouts were never on screen together to
 * be eased between.
 *
 * <p>Here every child is placed by ONE function of [progress], so a window resize (or the animation
 * that follows it) is a slide rather than a swap. At 0 every child sits in the single column; at 1 the
 * list is split down the middle into two. In between, each child's width, height and position are
 * interpolated -- which is what makes it read as the content rearranging itself instead of reloading.
 *
 * <p>HEIGHT IS DERIVED, NOT DECLARED. Each child is measured at the interpolated width first, because
 * its height depends on how the rows wrap at that width. The container is then the taller of the two
 * arrangements, interpolated -- so the scrollable content does not jump in height halfway through.
 *
 * <p>[progress] IS READ IN COMPOSITION, NOT IN THE MEASURE PASS. That was the first thing tried and it
 * silently does nothing: a `State` read inside `MeasureScope` is outside the snapshot observer, so the
 * layout never learns the value changed and the columns simply never move. Reading it here recomposes
 * this layout on each animation frame, which is the cost that buys correctness -- and the children are
 * a handful of sections, each keeping its own identity so none of them recomposes.
 */
@Composable
internal fun MorphingColumns(
    groupCount: Int,
    progress: State<Float>,
    modifier: Modifier = Modifier,
    gap: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable () -> Unit
) {
    // Read here so the layout is rebuilt when the morph advances. See the note above.
    val p = progress.value.coerceIn(0f, 1f)
    val gapPx = with(androidx.compose.ui.platform.LocalDensity.current) { gap.toPx().roundToInt() }
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val total = measurables.size
        if (total == 0) {
            return@Layout layout(constraints.minWidth, constraints.minHeight) {}
        }

        // The split point: the first ceil(n/2) sections go in the left column, the rest right. Filling
        // down rather than across keeps a section whole, so the boundary never cuts one in two.
        val half = (total + 1) / 2

        val fullWidth = constraints.maxWidth
        // Widths at each end. The single column has the whole width; each of two columns loses half
        // the gap. Interpolating BETWEEN them is what the eye follows.
        val singleW = fullWidth
        val columnW = ((fullWidth - gapPx) / 2).coerceAtLeast(1)

        // Measure every child at the width it has AT THIS PROGRESS, then place it.
        //
        // Measuring width-first is not optional: a section's height depends on how its rows wrap at
        // the width it is being given, and that width is exactly what is changing. Measuring at the
        // final width and then placing at an intermediate one would let the text re-wrap a frame late.
        val placed = ArrayList<Placed>(total)
        var singleY = 0      // where the child sits in the single column
        var leftY = 0        // ...and in the left column of two
        var rightY = 0       // ...and in the right column

        measurables.forEachIndexed { index, measurable ->
            val inRightColumn = index >= half
            val w = lerpWidth(singleW, columnW, p)
            val placeable = measurable.measure(Constraints.fixedWidth(w))

            // x: at one column everything starts at the left edge; at two, the right half shifts by
            // a column plus the gap. The shift is interpolated with the width, so a child slides
            // across while it narrows -- one movement instead of a jump and a resize.
            val x = if (inRightColumn) lerpX(0, columnW + gapPx, p) else 0
            // y: the single column stacks everything; two columns stack each half separately, so a
            // child in the right half travels UP as the split takes effect.
            val y = if (inRightColumn) lerpY(singleY, rightY, p) else lerpY(singleY, leftY, p)

            singleY += placeable.height
            if (inRightColumn) rightY += placeable.height else leftY += placeable.height

            placed += Placed(placeable, x, y)
        }

        // The scrollable height at each end, interpolated. Taking the two-column height outright
        // would make the scroll range collapse the moment the morph started, and the bar would jump.
        val singleHeight = singleY
        val twoHeight = maxOf(leftY, rightY, singleY).coerceAtLeast(1)
        val height = (singleHeight + (twoHeight - singleHeight) * p).roundToInt()

        layout(fullWidth, height.coerceIn(constraints.minHeight, constraints.maxHeight.coerceAtLeast(height))) {
            placed.forEach { it.placeable.place(it.x, it.y) }
        }
    }
}

/** One measured child and the position it holds at the current progress. */
private class Placed(
    val placeable: androidx.compose.ui.layout.Placeable,
    val x: Int,
    val y: Int
)

/**
 * A child's width at [p], snapped to whole pixels.
 *
 * <p>Rounded rather than truncated: truncation would bias every interpolated width downward, and a
 * column measured one pixel narrow re-wraps its text one row earlier than the final layout does --
 * a visible flicker at the end of the morph.
 */
private fun lerpWidth(a: Int, b: Int, p: Float): Int =
    (a + (b - a) * p).roundToInt().coerceAtLeast(1)

private fun lerpX(a: Int, b: Int, p: Float): Int = (a + (b - a) * p).roundToInt()

private fun lerpY(a: Int, b: Int, p: Float): Int = (a + (b - a) * p).roundToInt()