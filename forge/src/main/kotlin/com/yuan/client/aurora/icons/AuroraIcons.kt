package com.yuan.client.aurora.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

/**
 * A hand-drawn linear icon set.
 *
 * <p>Drawn rather than imported. An icon font or an SVG library would add a dependency and a
 * megabyte to a jar that is downloaded per player, to draw fourteen glyphs that are collectively
 * about sixty lines of primitives. Drawing them also means every icon shares one stroke width and one
 * grid by construction, which is the thing that makes a set look like a set rather than a collection.
 *
 * <p>Everything is authored on a 16x16 grid in logical units and scaled to the canvas, so the same
 * definition renders crisply at 14dp in the rail and at 20dp in a panel header with no second
 * variant. Strokes are 1.5 units, which at the smallest use is under two physical pixels: the
 * reference's icons are thin, and a heavier stroke is what makes an icon set look dated.
 *
 * <p>Linear throughout, never filled. A filled glyph at 12dp turns into a blob and, worse, a filled
 * and an outlined icon for the same concept make a set look assembled from two sources.
 */
internal enum class AuroraIcon {
    Combat, Player, Movement, Render, World, Misc, Starlight, Config, Hud, Scripts, Profile, Cosmetics,
    Search, Close, Chevron, ChevronRight, Grip, Reset, Pin, Maximize, Restore
}

/**
 * Draws one icon, tinted, at the canvas's own size.
 *
 * <p>The grid is the canvas: callers give this a box of the right dp and the drawing fills it, so no
 * icon site has to know about a viewport or an aspect ratio.
 */
@Composable
internal fun AuroraIcon(
    icon: AuroraIcon,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val unit = size.minDimension / 16f
        val stroke = Stroke(width = 1.5f * unit, cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round)
        translate(left = (size.width - unit * 16f) / 2f, top = (size.height - unit * 16f) / 2f) {
            drawIcon(icon, tint, unit, stroke)
        }
    }
}

/** One stroke path from a list of grid points, so a glyph reads as a shape rather than a list. */
private fun DrawScope.stroke(unit: Float, stroke: Stroke, color: Color, vararg points: Pair<Float, Float>) {
    val path = Path()
    points.forEachIndexed { index, (x, y) ->
        if (index == 0) path.moveTo(x * unit, y * unit) else path.lineTo(x * unit, y * unit)
    }
    drawPath(path, color, style = stroke)
}

/** A closed polygon, for the handful of glyphs that are outlines. */
private fun DrawScope.polygon(unit: Float, stroke: Stroke, color: Color, vararg points: Pair<Float, Float>) {
    val path = Path()
    points.forEachIndexed { index, (x, y) ->
        if (index == 0) path.moveTo(x * unit, y * unit) else path.lineTo(x * unit, y * unit)
    }
    path.close()
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawIcon(icon: AuroraIcon, c: Color, unit: Float, stroke: Stroke) {
    when (icon) {
        // A sword: blade, crossguard, pommel. The rail's first entry.
        AuroraIcon.Combat -> {
            stroke(unit, stroke, c, 8f to 2f, 8f to 12f)
            stroke(unit, stroke, c, 4.5f to 9.5f, 11.5f to 9.5f)
            stroke(unit, stroke, c, 8f to 12f, 6.5f to 14f)
        }
        // A head and shoulders.
        AuroraIcon.Player -> {
            drawCircle(c, radius = 2.6f * unit, center = Offset(8f * unit, 5.4f * unit), style = stroke)
            stroke(unit, stroke, c, 3f to 14f, 3f to 11.5f, 13f to 11.5f, 13f to 14f)
        }
        // Four arrows out from a centre: movement.
        AuroraIcon.Movement -> {
            stroke(unit, stroke, c, 8f to 3f, 8f to 13f)
            stroke(unit, stroke, c, 3f to 8f, 13f to 8f)
            stroke(unit, stroke, c, 8f to 3f, 6f to 5f)
            stroke(unit, stroke, c, 8f to 3f, 10f to 5f)
            stroke(unit, stroke, c, 8f to 13f, 6f to 11f)
            stroke(unit, stroke, c, 8f to 13f, 10f to 11f)
        }
        // A frame with a highlight: rendering.
        AuroraIcon.Render -> {
            polygon(unit, stroke, c, 8f to 2f, 14f to 5.5f, 14f to 10.5f, 8f to 14f, 2f to 10.5f, 2f to 5.5f)
            drawCircle(c, radius = 1.8f * unit, center = Offset(8f * unit, 8f * unit), style = stroke)
        }
        // A globe: latitude and longitude.
        AuroraIcon.World -> {
            drawCircle(c, radius = 6f * unit, center = Offset(8f * unit, 8f * unit), style = stroke)
            drawOval(
                c,
                topLeft = Offset(4.4f * unit, 2f * unit),
                size = Size(7.2f * unit, 12f * unit),
                style = stroke
            )
            stroke(unit, stroke, c, 2f to 8f, 14f to 8f)
        }
        // A diamond, four points.
        AuroraIcon.Misc -> polygon(unit, stroke, c, 8f to 2f, 14f to 8f, 8f to 14f, 2f to 8f)
        // The module's own mark: a four-point star with a long vertical.
        AuroraIcon.Starlight -> {
            stroke(unit, stroke, c, 8f to 1.5f, 8f to 14.5f)
            stroke(unit, stroke, c, 1.5f to 8f, 14.5f to 8f)
            stroke(unit, stroke, c, 4.5f to 4.5f, 11.5f to 11.5f)
            stroke(unit, stroke, c, 11.5f to 4.5f, 4.5f to 11.5f)
        }
        // Sliders: configuration.
        AuroraIcon.Config -> {
            stroke(unit, stroke, c, 2f to 5f, 14f to 5f)
            stroke(unit, stroke, c, 2f to 11f, 14f to 11f)
            drawCircle(c, radius = 2f * unit, center = Offset(10f * unit, 5f * unit), style = stroke)
            drawCircle(c, radius = 2f * unit, center = Offset(6f * unit, 11f * unit), style = stroke)
        }
        // A display with a stand: HUD.
        AuroraIcon.Hud -> {
            polygon(unit, stroke, c, 2f to 3f, 14f to 3f, 14f to 11f, 2f to 11f)
            stroke(unit, stroke, c, 8f to 11f, 8f to 14f)
            stroke(unit, stroke, c, 5f to 14f, 11f to 14f)
        }
        // A page with a fold: scripts.
        AuroraIcon.Scripts -> {
            stroke(unit, stroke, c, 3f to 2f, 10f to 2f, 13f to 5f, 13f to 14f, 3f to 14f, 3f to 2f)
            stroke(unit, stroke, c, 10f to 2f, 10f to 5f, 13f to 5f)
            stroke(unit, stroke, c, 5.5f to 8f, 10.5f to 8f)
            stroke(unit, stroke, c, 5.5f to 11f, 10.5f to 11f)
        }
        // A card with a tab: profile.
        AuroraIcon.Profile -> {
            polygon(unit, stroke, c, 2f to 4f, 14f to 4f, 14f to 13f, 2f to 13f)
            stroke(unit, stroke, c, 5f to 2f, 11f to 2f)
            drawCircle(c, radius = 2f * unit, center = Offset(8f * unit, 8f * unit), style = stroke)
        }
        // A sparkle pair: cosmetics.
        AuroraIcon.Cosmetics -> {
            stroke(unit, stroke, c, 6f to 2f, 6f to 10f)
            stroke(unit, stroke, c, 2f to 6f, 10f to 6f)
            stroke(unit, stroke, c, 12f to 10f, 12f to 14f)
            stroke(unit, stroke, c, 10f to 12f, 14f to 12f)
        }
        // A magnifier.
        AuroraIcon.Search -> {
            drawCircle(c, radius = 4.4f * unit, center = Offset(6.8f * unit, 6.8f * unit), style = stroke)
            stroke(unit, stroke, c, 10f to 10f, 13.5f to 13.5f)
        }
        // An X.
        AuroraIcon.Close -> {
            stroke(unit, stroke, c, 4.5f to 4.5f, 11.5f to 11.5f)
            stroke(unit, stroke, c, 11.5f to 4.5f, 4.5f to 11.5f)
        }
        // A downward chevron.
        AuroraIcon.Chevron -> stroke(unit, stroke, c, 4.5f to 6.5f, 8f to 10f, 11.5f to 6.5f)
        // A right chevron, for a collapsed section.
        AuroraIcon.ChevronRight -> stroke(unit, stroke, c, 6.5f to 4.5f, 10f to 8f, 6.5f to 11.5f)
        // Six dots: a drag handle.
        AuroraIcon.Grip -> {
            listOf(5.5f to 4.5f, 5.5f to 8f, 5.5f to 11.5f, 10.5f to 4.5f, 10.5f to 8f, 10.5f to 11.5f)
                .forEach { (x, y) -> drawCircle(c, radius = 1.1f * unit, center = Offset(x * unit, y * unit)) }
        }
        // A circular arrow: reset.
        AuroraIcon.Reset -> {
            drawArc(
                c,
                startAngle = 40f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = Offset(3f * unit, 3f * unit),
                size = Size(10f * unit, 10f * unit),
                style = stroke
            )
            stroke(unit, stroke, c, 10.5f to 2.5f, 12.5f to 4.5f, 10f to 5.5f)
        }
        // A pushpin, diagonal: head, shaft, point. Filled head would break the set's line rule, so
        // the head is an outline diamond and the shaft crosses it.
        AuroraIcon.Pin -> {
            polygon(unit, stroke, c, 9.5f to 2.5f, 13.5f to 6.5f, 8f to 8f)
            stroke(unit, stroke, c, 8f to 8f, 3.5f to 12.5f)
            stroke(unit, stroke, c, 3.5f to 12.5f, 3f to 13f)
        }
        // A square with a bold top edge: maximize. The top bar is what distinguishes it from a
        // plain frame at 14dp.
        AuroraIcon.Maximize -> {
            polygon(unit, stroke, c, 3f to 4f, 13f to 4f, 13f to 13f, 3f to 13f)
            stroke(unit, stroke, c, 3f to 4f, 13f to 4f)
        }
        // Two overlapping squares: restore. The front square is the window, the back square's
        // offset corner is the "there is a previous size" cue.
        AuroraIcon.Restore -> {
            polygon(unit, stroke, c, 5.5f to 5.5f, 13.5f to 5.5f, 13.5f to 13.5f, 5.5f to 13.5f)
            stroke(unit, stroke, c, 5.5f to 3f, 11f to 3f)
            stroke(unit, stroke, c, 3f to 3f, 3f to 11f)
            stroke(unit, stroke, c, 3f to 11f, 5.5f to 11f)
        }
    }
}

/** The standard icon box sizes, so no call site invents an in-between value. */
internal object IconSize {
    val rail = 16.dp
    val panelHeader = 20.dp
    val inline = 12.dp
    val control = 14.dp
}
