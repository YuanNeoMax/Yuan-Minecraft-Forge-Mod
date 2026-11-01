package com.yuan.client.aurora.scaffold

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.anim.Motion
import com.yuan.client.aurora.AuroraLook
import com.yuan.client.aurora.LocalAuroraLayout
import com.yuan.client.aurora.icons.AuroraIcon
import com.yuan.client.aurora.icons.IconSize
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale
import com.yuan.client.aurora.theme.auroraFont

/**
 * Aurora's controls, in the reference's language.
 *
 * <p>Three rules, each a correction of something the previous version got wrong:
 *
 * <p>1. **State is brightness, not colour.** The toggle's track goes from a dark well to a light
 * track, its knob from dim to bright, and no hue is involved. That is what lets a page of twenty
 * switches be read without reading any of them. The earlier palette made every one of them the same
 * accent colour, which meant the one thing a switch has to say -- on or off -- was the one thing
 * colour could not say.
 * <p>2. **Every control is small.** A 36x20 pill and a 4px slider bar. The reference's density comes
 * from control size, not from row padding: shrinking the controls is what lets a settings list fit in
 * one screen instead of two.
 * <p>3. **No control draws a container.** Rows sit on the surface with no border of their own, and
 * dividers are single hairlines. A bordered row inside a bordered panel is a box in a box, and enough
 * of those is what makes an interface look like a form rather than an application.
 */

/** The control column's width, shared by every settings row so their controls line up exactly. */
internal val CONTROL_WIDTH: Dp = 156.dp

/**
 * A settings row: label on the left, control on the right, hint beneath the label.
 *
 * <p>A fixed control width rather than a fraction. A column of toggles whose x-position answers to
 * each label's length is the single most common way a settings page looks unfinished, and it happens
 * the moment the label is allowed to size the control.
 */
@Composable
internal fun AuroraRow(
    label: String,
    hint: String = "",
    control: @Composable () -> Unit
) {
    // The row's pitch comes from the arrangement layer, so the numbers its menu exposes have something
    // to move. It is read before the layout block for the same reason the rail's offsets are: a
    // composable read inside a non-composable scope has no composition to read from.
    val rowLook = AuroraLook(LocalAuroraLayout.current)
    val rowHeight = rowLook.rowHeight
    val rowPad = rowLook.rowPadding
    Row(
        Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .padding(horizontal = rowPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontSize = TypeScale.row,
                color = Star.textPrimary,
                fontFamily = auroraFont
            )
            if (hint.isNotEmpty()) {
                Text(
                    hint,
                    fontSize = TypeScale.hint,
                    color = Star.textTertiary,
                    fontFamily = auroraFont,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(CONTROL_WIDTH)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                control()
            }
        }
    }
}

/**
 * A small pill toggle.
 *
 * <p>The on state fills the track with the accent and the off state leaves it as a dark groove, which
 * is the reference's own reading and the one thing brightness alone could not say: a track that only
 * got lighter looks like a highlighted row, not like a feature that is running.
 *
 * <p>The knob is white on the accent and grey on the groove, so the pair still separates in greyscale --
 * the accent carries the state, and brightness carries which end of the travel the knob is at.
 *
 * <p>The knob's travel is computed from the track's own size, so changing the token resizes the
 * control without the knob overshooting.
 */
@Composable
internal fun AuroraToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed = isPressed(interaction)
    // KEPT AS State<Float>, NOT UNWRAPPED WITH `by`.
    //
    // `by animateFloatAsState(...)` reads the value during COMPOSITION, so every animation frame
    // recomposes this control and everything under it. Holding the State and reading it only inside
    // `graphicsLayer { }` moves the read into the draw phase: the knob still animates at full frame
    // rate, but the composable is not re-executed for each of those frames.
    //
    // The knob's travel is the only consumer, and travel is a transform -- exactly what a layer is
    // for. Nothing about the toggle's appearance depends on the animated value in a way that needs
    // layout.
    val slide = animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        // Eased, not sprung. A switch is a fact, not a physical object: a bounce on a 16px travel draws
        // the eye twice to a control that has already said what it has to say.
        animationSpec = tween(Motion.dur(Motion.QUICK_MS), easing = Motion.standard),
        label = "toggle"
    )
    val trackW = Star.toggleWidth
    val trackH = Star.toggleHeight
    val inset = 2.dp
    val knob = trackH - inset * 2
    val travel = with(LocalDensity.current) { (trackW - knob - inset * 2).toPx() }

    Box(
        Modifier
            .size(trackW, trackH)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(RoundedCornerShape(trackH / 2))
            // The accent, on a track and nowhere else -- this is one of the two controls the token's
            // contract names, and the only place it fills a surface rather than an indicator.
            .background(if (checked) Star.accent else Star.accentTrack)
            .border(1.dp, if (checked) Color.Transparent else Star.hairline, RoundedCornerShape(trackH / 2))
            .hoverable(interaction, enabled = enabled)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(inset)
                .size(knob)
                .graphicsLayer { translationX = slide.value * travel }
                // The knob brightens when it lifts off the track: pressed feedback that changes no
                // layout, so the row never jitters while being toggled.
                .background(
                    when {
                        !checked -> Star.knobOff
                        pressed -> Color.White
                        hovered -> Color(0xFFF2F2F4)
                        else -> Star.knob
                    },
                    RoundedCornerShape(knob / 2)
                )
        )
    }
}

/**
 * A segmented control: a row of labels, the selected one carrying a faint plate.
 *
 * <p>The plate is drawn at the selection's slot rather than cross-faded between slots, so the choice
 * reads as one thing with a position. Two plates brightening past each other reads as two separate
 * events that happened at the same time.
 */
@Composable
internal fun <T> AuroraSegments(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    width: Dp = CONTROL_WIDTH,
    enabled: Boolean = true
) {
    if (options.isEmpty()) return
    val index = options.indexOf(selected).coerceAtLeast(0)
    val count = options.size
    // Held as State and read inside the Canvas draw lambda, for the same reason as the toggle's
    // knob: the plate's position is pure drawing, so animating it must not recompose the row of
    // option labels underneath it.
    val plate = animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = tween(Motion.dur(Motion.BASE_MS), easing = Motion.enter),
        label = "segPlate"
    )

    Box(
        Modifier
            .width(width)
            .height(24.dp)
            .clip(RoundedCornerShape(Star.controlRadius))
            .background(Star.well)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val segW = size.width / count
            val inset = 2f
            val w = segW - inset * 2
            val radius = CornerRadius(Star.controlRadius.toPx() - 2f)
            val x = segW * plate.value + inset
            drawRoundRect(
                color = Star.selected,
                topLeft = Offset(x, inset),
                size = Size(w, size.height - inset * 2),
                cornerRadius = radius
            )
            drawRoundRect(
                color = Star.hairlineStrong,
                topLeft = Offset(x, inset),
                size = Size(w, size.height - inset * 2),
                cornerRadius = radius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
            )
        }
        Row(Modifier.fillMaxSize()) {
            options.forEach { option ->
                val isSelected = option == selected
                val interaction = remember(option) { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (hovered && !isSelected && enabled) Star.hover else Color.Transparent)
                        .hoverable(interaction, enabled = enabled)
                        .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                            onSelect(option)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label(option),
                        fontSize = TypeScale.value,
                        // Selected is simply brighter. No accent colour: on this surface a hue would
                        // make the choice look like an alarm rather than a setting.
                        color = when {
                            !enabled -> Star.textDisabled
                            isSelected -> Star.textPrimary
                            else -> Star.textSecondary
                        },
                        fontFamily = auroraFont,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * A slider: a hairline track, a filled portion, a dot for the handle, and a live readout.
 *
 * <p>The track is 3px and the handle a 6px dot rather than a large circle. At the reference's density
 * a chunky handle forces the row taller, and a row taller than its neighbours is what makes a list
 * look hand-assembled.
 *
 * <p>The whole 24px band is the hit area rather than the track itself: a 3px line is not a target
 * anyone hits reliably, and a slider that takes two attempts is the most common complaint about a
 * custom interface.
 *
 * <p>The readout is a separate cell beside the track, in a fixed-width box, so a value going from
 * `9` to `16` does not shift the track's right edge and start the whole row moving.
 */
@Composable
internal fun AuroraSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    format: (Float) -> String,
    width: Dp = 92.dp,
    enabled: Boolean = true,
    /**
     * The value a double-click returns to, or null when the row has no meaningful default.
     *
     * <p>Resetting one row is already reachable by right-clicking it (see GodSwordBinding.resetRow),
     * but a double-click on the slider is the gesture a player tries FIRST on a control they have
     * just dragged somewhere wrong -- it costs nothing to honour it, and it saves aiming at the
     * label. Null keeps the double-click inert, so a control with no default does not pretend to
     * have one.
     */
    default: Float? = null
) {
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)

    // The readout's width, measured from the values this control can actually show: the current one plus
    // both ends. Measuring only the current value would make the row jitter as it changed; measuring the
    // ends is what keeps the track's right edge still while the text changes between them.
    val measure = rememberTextMeasurer()
    val readoutDensity = LocalDensity.current
    val readoutStyle = remember { TextStyle(fontSize = TypeScale.value, fontFamily = auroraFont) }
    val widestReadout = listOf(range.start, range.endInclusive, value)
        .map { format(it) }
        .maxByOrNull { it.length } ?: "0"
    val readoutWidth = with(readoutDensity) {
        measure.measure(widestReadout, readoutStyle).size.width.toDp() + 6.dp
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(width)
                .height(24.dp)
                // THREE WAYS TO SET ONE VALUE, and they share this one pointerInput.
                //
                // The drag is the obvious one. The WHEEL is the precise one: on a 0..100 row a
                // dragged pixel is worth several units, so nudging a value by one is practically
                // impossible with the pointer alone -- and a player who wants 37 rather than 35
                // should not have to overshoot deliberately. The DOUBLE-CLICK is the forgiving one:
                // it takes a row back to its default without aiming at anything.
                //
                // One handler rather than two chained modifiers: two `pointerInput` blocks on one
                // node is the consume-order trap this file has paid for before -- whichever runs
                // first owns the stream and the other sees consumed events and never starts.
                .pointerInput(enabled, range.start, span, step, default) {
                    if (!enabled) return@pointerInput
                    fun emit(x: Float) {
                        val f = (x / size.width).coerceIn(0f, 1f)
                        val raw = range.start + f * span
                        val next = if (step > 0f) Math.round(raw / step) * step else raw
                        onValueChange(next)
                    }
                    fun nudge(direction: Int) {
                        // One step per notch, or a twentieth of the range when the step is coarse
                        // enough that a single notch would be imperceptible.
                        val delta = if (step > 0f) step else span / 20f
                        val next = (value + delta * direction)
                            .coerceIn(range.start, range.endInclusive)
                        onValueChange(if (step > 0f) Math.round(next / step) * step else next)
                    }
                    awaitPointerEventScope {
                        var lastTapAt = 0L
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            when (event.type) {
                                PointerEventType.Scroll -> {
                                    val dy = change.scrollDelta.y
                                    if (dy != 0f) {
                                        // Wheel UP increases: scrollDelta is positive when the
                                        // content scrolls down, which is the opposite direction.
                                        nudge(if (dy < 0f) 1 else -1)
                                        change.consume()
                                    }
                                }
                                PointerEventType.Press -> {
                                    val now = System.nanoTime()
                                    val isDouble = now - lastTapAt < 400_000_000L
                                    lastTapAt = now
                                    if (isDouble && default != null) {
                                        onValueChange(default)
                                        change.consume()
                                        // Consumed so the pair does not also start a drag that
                                        // would immediately overwrite the reset.
                                        continue
                                    }
                                    emit(change.position.x)
                                    // The drag loop runs from here, as before: a press already
                                    // means "jump to this position", which is what makes the whole
                                    //  24px band feel like the track.
                                }
                                PointerEventType.Move -> if (change.pressed) {
                                    emit(change.position.x)
                                    change.consume()
                                }
                                else -> Unit
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val barH = 3.dp.toPx()
                val trackTop = (size.height - barH) / 2f
                val radius = CornerRadius(barH / 2f)
                drawRoundRect(
                    color = Star.accentTrack,
                    topLeft = Offset(0f, trackTop),
                    size = Size(size.width, barH),
                    cornerRadius = radius
                )
                // The fill is the accent, which is this token's second and last job. A grey fill reads
                // as a progress meter -- something the interface is measuring -- while a blue one reads
                // as this row's value, which is what a setting is.
                drawRoundRect(
                    color = if (enabled) Star.accent else Star.textDisabled,
                    topLeft = Offset(0f, trackTop),
                    size = Size(size.width * fraction, barH),
                    cornerRadius = radius
                )
                val knobX = size.width * fraction
                drawCircle(
                    color = if (enabled) Star.accent else Star.textDisabled,
                    radius = 5f,
                    center = Offset(knobX, size.height / 2f)
                )
                // A darker pip inside the knob, so the handle reads as a ring rather than a dot and the
                // exact value stays findable against the fill it sits on.
                drawCircle(
                    color = Star.panel,
                    radius = 2f,
                    center = Offset(knobX, size.height / 2f)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            format(value),
            fontSize = TypeScale.value,
            color = if (enabled) Star.textSecondary else Star.textDisabled,
            fontFamily = auroraFont,
            maxLines = 1,
            // Sized to the widest value this control can produce, rather than to a fixed 46dp.
            //
            // The fixed width was a guess at "how wide is a number", and it is wrong for anything carrying
            // a unit: `168 dp` was clipped to `1`, `100%` to `1`, `0.05` to `0`. A readout that silently
            // understates its own value is worse than no readout, and the fix is to measure the text the
            // control is already formatting.
            modifier = Modifier.width(readoutWidth)
        )
    }
}


/**
 * A selector: the current option on a line that opens its list.
 *
 * <p>The reference draws a choice as `Value ▸` rather than as a row of options. The difference matters
 * at this density: a segmented control spends the full width of the row on every candidate, so three
 * options make the control as wide as its neighbours' and the eye has to compare plate positions to
 * find the active one. A selector spends one value's width and states the answer in words.
 *
 * <p>The list is a popup, not an inline expansion. Pushing the rows below it down as it opens would make
 * a settings column jump every time a choice was merely considered.
 *
 * <p>The scrim behind the list is what makes outside-click dismiss work, and it also stops a click meant
 * as "dismiss" from landing on the row underneath -- which is the failure a popup without one has.
 */
@Composable
internal fun AuroraSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    width: Dp = 120.dp,
    enabled: Boolean = true,
    placeholder: String = "选择"
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var open by remember { mutableStateOf(false) }
    val current = options.getOrNull(selectedIndex)

    Box {
        Row(
            Modifier
                .width(width)
                .height(24.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
                .clip(RoundedCornerShape(Star.controlRadius))
                .background(if (hovered || open) Star.hover else Color.Transparent)
                .hoverable(interaction, enabled = enabled)
                .handCursor()
                .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                    open = !open
                }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                current ?: placeholder,
                fontSize = TypeScale.value,
                color = Star.textPrimary,
                fontFamily = auroraFont,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.weight(1f))
            AuroraIcon(
                AuroraIcon.ChevronRight,
                tint = Star.textSecondary,
                modifier = Modifier.size(IconSize.inline)
            )
        }

        if (open) {
            // Painted after the closed state so it covers what follows, and full-bleed so a click
            // anywhere dismisses.
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { open = false }
            )
            Column(
                Modifier
                    .padding(top = 26.dp)
                    .width(width)
                    .clip(RoundedCornerShape(Star.controlRadius))
                    .background(Star.panel)
                    .border(1.dp, Star.hairlineStrong, RoundedCornerShape(Star.controlRadius))
            ) {
                options.forEachIndexed { index, option ->
                    val rowInteraction = remember(index) { MutableInteractionSource() }
                    val rowHovered by rowInteraction.collectIsHoveredAsState()
                    val isCurrent = index == selectedIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                when {
                                    rowHovered -> Star.hover
                                    isCurrent -> Star.accentTint
                                    else -> Color.Transparent
                                }
                            )
                            .hoverable(rowInteraction)
                            .handCursor()
                            .clickable(interactionSource = rowInteraction, indication = null) {
                                onSelect(index)
                                open = false
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            option,
                            fontSize = TypeScale.value,
                            // The current option is stated by brightness, and the accent only tints its
                            // plate -- so a list of five options does not read as five blue things.
                            color = if (isCurrent) Star.textPrimary else Star.textSecondary,
                            fontFamily = auroraFont,
                            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * A text button. Quiet by default; a filled variant for the one primary action on a surface.
 *
 * <p>Filled means "raised light", not "coloured": a near-white plate with dark text, which is the
 * highest-contrast legitimate pairing here. The accent is deliberately not used -- it is reserved for
 * stating a control's value, and a button states an action instead, so filling one with it would make a
 * pressable thing look like a reading.
 */
@Composable
internal fun AuroraButton(
    text: String,
    onClick: () -> Unit,
    filled: Boolean = false,
    enabled: Boolean = true,
    icon: AuroraIcon? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed = isPressed(interaction)

    Row(
        Modifier
            .height(28.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .clip(RoundedCornerShape(Star.controlRadius))
            .background(
                when {
                    filled && pressed -> lerp(Star.on, Color(0xFF9A9AA0), 0.30f)
                    filled && hovered -> Color(0xFFF2F2F4)
                    filled -> Star.on
                    pressed -> Star.press
                    hovered -> Star.hover
                    else -> Color.Transparent
                }
            )
            .border(
                1.dp,
                if (filled) Color.Transparent else Star.hairlineStrong,
                RoundedCornerShape(Star.controlRadius)
            )
            .hoverable(interaction, enabled = enabled)
            .handCursor()
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            AuroraIcon(
                icon,
                tint = if (filled) Color(0xFF15151A) else Star.textSecondary,
                modifier = Modifier.size(IconSize.control)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            fontSize = TypeScale.button,
            color = if (filled) Color(0xFF15151A) else Star.textPrimary,
            fontFamily = auroraFont,
            fontWeight = if (filled) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/**
 * A read-only value cell with a label under it, for a status strip.
 *
 * <p>The value takes the label's place in the visual hierarchy: bright number, dim caption. The width
 * is fixed by the caller so a strip of them does not shuffle as the values change.
 */
@Composable
internal fun AuroraStat(label: String, value: String, width: Dp = 64.dp) {
    Column(
        Modifier.width(width),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            value,
            fontSize = TypeScale.value,
            color = Star.textPrimary,
            fontFamily = auroraFont,
            maxLines = 1
        )
        Text(
            label,
            fontSize = TypeScale.hint,
            color = Star.textTertiary,
            fontFamily = auroraFont,
            maxLines = 1
        )
    }
}
