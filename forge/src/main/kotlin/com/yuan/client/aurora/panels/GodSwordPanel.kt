package com.yuan.client.aurora.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.anim.EnterBlock
import com.yuan.client.aurora.anim.staggerDelay
import com.yuan.client.aurora.icons.AuroraIcon
import com.yuan.client.aurora.scaffold.AuroraButton
import com.yuan.client.aurora.scaffold.AuroraRow
import com.yuan.client.aurora.scaffold.AuroraSelector
import com.yuan.client.aurora.scaffold.AuroraSlider
import com.yuan.client.aurora.scaffold.AuroraToggle
import com.yuan.client.aurora.scaffold.CONTROL_WIDTH
import com.yuan.client.aurora.scaffold.auroraScroll
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale
import com.yuan.client.aurora.theme.auroraFont
import com.yuan.client.gui.V9Holder
import java.util.Locale

/**
 * Draws the god sword's settings, one control per row, straight from the row tables.
 *
 * <p>The whole point of the tables: this composable carries no per-setting code. It switches on a
 * row's type and asks [GodSwordBinding] for the value and the bounds, so a setting added to a table is
 * rendered without touching this file.
 *
 * <p>Gated rows arrive already filtered. Filtering here would mean this function reading `renderStyle`
 * to decide what to draw, which is a config read inside something that should be a pure function of
 * the rows it was handed -- and it would put the "which style is active" question in two places.
 */
@Composable
internal fun GodSwordPanel(
    rows: List<GodRow>,
    holder: V9Holder,
    onChanged: () -> Unit,
    onReset: () -> Unit,
    presetSlot: Int,
    onPresetSlot: (Int) -> Unit,
    onPresetLoad: (Int) -> Unit,
    onPresetSave: (Int) -> Unit
) {
    // Subscribe: every row below reads plain fields through the binding, and a value written
    // without a subscription changes the model and never repaints ("好像实际有效果，但是不显示").
    @Suppress("UNUSED_EXPRESSION")
    com.yuan.client.aurora.LocalLayoutRevision.current

    Column(
        Modifier
            .fillMaxSize()
            .auroraScroll(rememberScrollState())
            .padding(bottom = 8.dp)
    ) {
        var lastSection: String? = null
        var first = true
        rows.forEachIndexed { index, godRow ->
            val row = godRow.row
            if (row.section != lastSection) {
                SectionHeading(row.section, first)
                lastSection = row.section
                first = false
            }
            EnterBlock(delayMs = staggerDelay(index), identity = row.id, fromY = 6) {
                // The two multi-part rows are handled before the type switch: neither has a single
                // value to read, so both would fall through it as a no-op row.
                when (row.id) {
                    "ballColor" -> BallColourRow(holder, onChanged)
                    "preset" -> PresetRow(holder, presetSlot, onPresetSlot, onPresetLoad, onPresetSave)
                    else -> GodSwordRowLine(godRow, holder, onChanged)
                }
            }
        }

        Box(
            Modifier.fillMaxWidth().padding(12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            AuroraButton("恢复默认", onClick = onReset, icon = AuroraIcon.Reset)
        }
    }
}

/**
 * One row's control, chosen by the row's declared type.
 *
 * <p>A channel row reads its byte out of a packed colour and writes one byte back. The alternative --
 * giving each colour its own RGB triple of sliders -- is what the older screen does, and its cost is
 * eight nearly identical colour blocks for the silk palette alone.
 */
@Composable
internal fun GodSwordRowLine(
    godRow: GodRow,
    holder: V9Holder,
    onChanged: () -> Unit
) {
    // SUBSCRIBE AT THE LEAF: this composable reads the value through the binding (a plain field on
    // the holder's state) and its own parameters never change when that value does, so without this
    // read Compose skips it and the toggle/slider keeps painting the old value.
    @Suppress("UNUSED_EXPRESSION")
    com.yuan.client.aurora.LocalLayoutRevision.current
    val row = godRow.row

    // RIGHT-CLICK THE ROW TO PUT IT BACK TO ITS DEFAULT.
    //
    // On the row rather than a small control beside the value: a control per row would add eighteen
    //-odd buttons to a panel that is already dense, and the action is rare enough that it does not
    //earn permanent space. A row that has no meaningful single default (a colour swatch, a preset
    //slot) declares so by returning false, and then does nothing at all -- which is better than a
    //control that appears and silently fails.
    Box(
        Modifier.fillMaxWidth().pointerInput(row.id) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: continue
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        if (GodSwordBinding.resetRow(godRow, holder)) {
                            change.consume()
                            onChanged()
                        }
                    }
                }
            }
        }
    ) {
    when (row.type) {
        GodSwordRows.TYPE_SWITCH -> AuroraRow(row.name, row.desc) {
            AuroraToggle(
                checked = GodSwordBinding.readBool(row.id, holder),
                onCheckedChange = {
                    GodSwordBinding.writeBool(row.id, holder, it)
                    onChanged()
                }
            )
        }

        GodSwordRows.TYPE_SEGMENT -> AuroraRow(row.name, row.desc) {
            val labels = GodSwordBinding.labelsOf(row.id)
            // A selector rather than a segmented control: at this density a row of options spends the
            // whole control column on every candidate, and the answer is one word. The list opens over
            // the row instead of pushing the column down.
            AuroraSelector(
                options = labels,
                selectedIndex = GodSwordBinding.readIndex(row.id, holder),
                onSelect = {
                    GodSwordBinding.writeIndex(row.id, holder, it)
                    onChanged()
                }
            )
        }

        GodSwordRows.TYPE_SLIDER -> {
            // A scaled row shows and edits a value in a display unit; the factor lives on the row so
            // it cannot be applied on read and forgotten on write, which is exactly what the older
            // screen's two tick-based cooldowns did.
            val scale = godRow.scale
            val display = if (scale > 0f) {
                GodSwordBinding.readFloat(row.id, holder) / scale
            } else {
                GodSwordBinding.readFloat(row.id, holder)
            }
            val baseRange = GodSwordBinding.rangeOf(row.id)
            val range = if (scale > 0f) {
                (baseRange.start / scale)..(baseRange.endInclusive / scale)
            } else {
                baseRange
            }
            val format = if (scale > 0f) {
                { v: Float -> String.format(Locale.ROOT, "%.1fs", v) }
            } else {
                GodSwordBinding.formatOf(row.id)
            }
            AuroraRow(row.name, row.desc) {
                AuroraSlider(
                    value = display,
                    range = range,
                    step = if (scale > 0f) 1f else GodSwordBinding.stepOf(row.id),
                    onValueChange = { v ->
                        GodSwordBinding.writeFloat(row.id, holder, if (scale > 0f) v * scale else v)
                        onChanged()
                    },
                    format = format
                )
            }
        }

        GodSwordRows.TYPE_CHANNEL -> {
            val field = godRow.colourField
            val channel = godRow.channel
            if (field != null && channel != null) {
                AuroraRow(row.name, "") {
                    AuroraSlider(
                        value = GodSwordBinding.readChannel(field, channel, holder),
                        // A channel is a byte: its bounds are fixed at 0..255 rather than derived
                        // from the packed colour's own range.
                        range = 0f..255f,
                        step = 1f,
                        onValueChange = { v ->
                            GodSwordBinding.writeChannel(field, channel, holder, v)
                            onChanged()
                        },
                        format = { v -> "${Math.round(v)}" }
                    )
                }
            }
        }

        else -> Unit
    }
    }
}

/**
 * A dim heading with a hairline above it, for a row's section.
 *
 * <p>The rule is omitted for the first section: a line across the top of a panel that already has a
 * header rule directly above it reads as a double border.
 */
@Composable
internal fun SectionHeading(name: String, first: Boolean = false) {
    if (!first) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(1.dp)
                .background(Star.hairline)
        )
    }
    Text(
        name,
        fontSize = TypeScale.hint,
        color = Star.textTertiary,
        fontFamily = auroraFont,
        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp)
    )
}
