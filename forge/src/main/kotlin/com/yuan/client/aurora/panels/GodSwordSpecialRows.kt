package com.yuan.client.aurora.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.scaffold.AuroraButton
import com.yuan.client.aurora.scaffold.AuroraSlider
import com.yuan.client.aurora.scaffold.handCursor
import com.yuan.client.aurora.scaffold.isPressed
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale
import com.yuan.client.aurora.theme.auroraFont
import com.yuan.client.gui.V9Holder
import com.yuan.client.gui.ballColors

/**
 * The two rows that are not a single scalar: the ball colour swatch and the config presets.
 *
 * <p>Split from [GodSwordPanel] because neither fits the row/control switch that everything else goes
 * through. A swatch is eight colours plus a custom option plus three channel sliders; a preset row is
 * three slots that save or load the whole config. Both are one setting each with a multi-part control,
 * and forcing them into the generic path would mean the generic path growing two special cases that
 * nothing else uses.
 */

/** The ball colour: eight presets and a custom option that reveals three channel sliders. */
@Composable
internal fun BallColourRow(
    holder: V9Holder,
    onChanged: () -> Unit
) {
    val custom = holder.ballColorCustom
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "光球颜色",
                    fontSize = TypeScale.row,
                    color = Star.textPrimary,
                    fontFamily = auroraFont
                )
                Text(
                    "八种预设或自定义 RGB",
                    fontSize = TypeScale.hint,
                    color = Star.textTertiary,
                    fontFamily = auroraFont
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ballColors.forEachIndexed { index, argb ->
                    val selected = !custom && index == holder.ballColor
                    SwatchDot(
                        colour = Color(argb),
                        selected = selected,
                        onClick = {
                            holder.ballColor = index
                            holder.ballColorCustom = false
                            onChanged()
                        }
                    )
                }
                // The custom option is drawn as a spectrum rather than a colour: it is the one swatch
                // whose appearance is not fixed, and a swatch that shows a colour would imply it had
                // one already.
                SwatchDot(
                    colour = Color(0xFF7C5CFF),
                    selected = custom,
                    gradient = true,
                    onClick = {
                        holder.ballColorCustom = true
                        onChanged()
                    }
                )
            }
        }
        // The channels only appear once custom is chosen. Showing them always would put three extra
        // sliders under a row whose preset is already selected, which is noise for the common case.
        if (custom) {
            Spacer(Modifier.height(4.dp))
            ChannelSlider("R", holder.customColor, 'R') { holder.customColor = it; onChanged() }
            ChannelSlider("G", holder.customColor, 'G') { holder.customColor = it; onChanged() }
            ChannelSlider("B", holder.customColor, 'B') { holder.customColor = it; onChanged() }
        }
    }
}

/** One swatch: a filled circle with a selected ring, at the size the reference's dots use. */
@Composable
private fun SwatchDot(
    colour: Color,
    selected: Boolean,
    onClick: () -> Unit,
    gradient: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed = isPressed(interaction)
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(
                if (gradient) {
                    Brush.linearGradient(listOf(Color(0xFFFF7A5C), Color(0xFF7C5CFF), Color(0xFF3DD6E8)))
                } else {
                    Brush.linearGradient(listOf(colour, colour))
                }
            )
            .border(
                if (selected) 2.dp else 1.dp,
                when {
                    selected -> Star.textPrimary
                    hovered -> Star.hairlineStrong
                    else -> Star.hairline
                },
                CircleShape
            )
            .hoverable(interaction)
            .handCursor()
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(2.dp)
    ) {
        // A pressed swatch darkens rather than scaling: eight of them in a row, and a scale on one
        // would shift its neighbours by a pixel as the layout re-measures.
        if (pressed) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(Color(0x33000000)))
        }
    }
}

/** One channel of the custom colour, as a 0..255 slider with the byte patched in place. */
@Composable
private fun ChannelSlider(label: String, colour: Int, channel: Char, onValue: (Int) -> Unit) {
    val value = when (channel) {
        'R' -> (colour shr 16) and 0xFF
        'G' -> (colour shr 8) and 0xFF
        else -> colour and 0xFF
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = TypeScale.hint,
            color = Star.textSecondary,
            fontFamily = auroraFont,
            modifier = Modifier.width(12.dp)
        )
        AuroraSlider(
            value = value.toFloat(),
            range = 0f..255f,
            step = 1f,
            onValueChange = { v ->
                val byte = Math.round(v).coerceIn(0, 255)
                val next = when (channel) {
                    'R' -> (colour and 0xFF00FFFF.toInt()) or (byte shl 16)
                    'G' -> (colour and 0xFFFF00FF.toInt()) or (byte shl 8)
                    else -> (colour and 0xFFFFFF00.toInt()) or byte
                }
                onValue(byte.let { next })
            },
            format = { "${Math.round(it)}" }
        )
    }
}

/**
 * The preset slots: three numbered cells above a pair of actions.
 *
 * <p>Drawn as a row of numbered squares rather than as three rows of load/save buttons. That is the
 * reference's own arrangement for its hotbar, and it fits what a preset is: three interchangeable
 * places to put a configuration, not three named commands. The number identifies the slot, and which
 * slot the buttons act on is the selection -- stated once, by the filled cell.
 */
@Composable
internal fun PresetRow(
    holder: V9Holder,
    slot: Int,
    onSlot: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onSave: (Int) -> Unit,
    onCopy: (Int) -> Unit,
    onPaste: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            "配置预设",
            fontSize = TypeScale.row,
            color = Star.textPrimary,
            fontFamily = auroraFont
        )
        Text(
            "选择一个槽位后保存或载入整套配置",
            fontSize = TypeScale.hint,
            color = Star.textTertiary,
            fontFamily = auroraFont
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..2).forEach { index ->
                PresetCell(index, selected = index == slot, onClick = { onSlot(index) })
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AuroraButton("载入槽 ${slot + 1}", onClick = { onLoad(slot) })
            AuroraButton("保存到槽 ${slot + 1}", onClick = { onSave(slot) }, filled = true)
        }
        Spacer(Modifier.height(6.dp))
        // SHARING, on its own row and stated as a clipboard rather than as "import/export".
        //
        // A preset lives in the sword's NBT, so it already travels with the item through a relog,
        // a chest or a trade -- the only thing that did not travel was a preset going to a DIFFERENT
        // sword. The clipboard is the one channel that crosses that boundary without inventing a
        // second store on disk that would drift from the item.
        //
        // Named after what the player will actually do with it: copy the text, paste the text.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AuroraButton("复制槽 ${slot + 1}", onClick = { onCopy(slot) })
            AuroraButton("从剪贴板粘贴", onClick = { onPaste(slot) })
        }
    }
}

/**
 * One preset cell: a square with the slot's number and a corner tick when occupied.
 *
 * <p>The occupied tick is drawn from the sword's own preset list rather than kept in UI state. A preset
 * exists in the item's NBT, so a copy in the composition would be right until the first save and wrong
 * after it -- and "is there anything in slot 2" is exactly the question the cell exists to answer.
 */
@Composable
private fun PresetCell(index: Int, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember(index) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val occupied = remember(index, selected) { isPresetOccupied(index) }

    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(Star.controlRadius))
            .background(
                when {
                    selected -> Star.accentTint
                    hovered -> Star.hover
                    else -> Star.well
                }
            )
            .border(
                1.dp,
                when {
                    selected -> Star.accent
                    else -> Star.hairline
                },
                RoundedCornerShape(Star.controlRadius)
            )
            .hoverable(interaction)
            .handCursor()
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${index + 1}",
            fontSize = TypeScale.row,
            // The number stays legible rather than turning accent-coloured: the border already states
            // the selection, and a filled cell plus a coloured glyph would state it twice.
            color = if (selected) Star.textPrimary else Star.textSecondary,
            fontFamily = auroraFont,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
        if (occupied) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(4.dp)
                    .background(Star.textSecondary, RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Whether one preset slot holds anything.
 *
 * <p>Answered by reading the sword's own tag, because that is where presets live -- they travel with the
 * item, so a client-side flag would disagree with the server the moment the two had different views.
 */
private fun isPresetOccupied(slot: Int): Boolean {
    val mc = net.minecraft.client.Minecraft.getInstance()
    val stack = mc.player?.mainHandItem?.takeIf {
        it.item is com.yuan.item.YuanGodSwordItem
    } ?: mc.player?.offhandItem?.takeIf { it.item is com.yuan.item.YuanGodSwordItem } ?: return false
    val root = stack.getTagElement(com.yuan.item.YuanGodSwordConfig.TAG) ?: return false
    val presets = root.getList(
        com.yuan.item.YuanGodSwordConfig.PRESETS_KEY,
        net.minecraft.nbt.Tag.TAG_COMPOUND.toInt()
    )
    if (slot !in 0 until presets.size) return false
    return !presets.getCompound(slot).isEmpty
}
