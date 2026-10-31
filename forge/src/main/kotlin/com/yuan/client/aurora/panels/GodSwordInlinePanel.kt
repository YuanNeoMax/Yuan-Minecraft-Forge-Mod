package com.yuan.client.aurora.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.anim.EnterBlock
import com.yuan.client.aurora.anim.staggerDelay
import com.yuan.client.aurora.icons.AuroraIcon
import com.yuan.client.aurora.icons.IconSize
import com.yuan.client.aurora.scaffold.auroraScroll
import com.yuan.client.aurora.scaffold.handCursor
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale
import com.yuan.client.aurora.theme.auroraFont
import com.yuan.client.gui.V9Holder

/**
 * A feature's settings, drawn directly in the content area, in two columns.
 *
 * <p>Selecting a rail entry shows the feature's content immediately, with no intermediate step. Two
 * earlier revisions both had an extra level here -- first a grid of sections, then a rule that collapsed
 * long sections into a row that opened a panel -- and both were solving a problem the column layout
 * itself solves. A feature is long; long content in one column is a scroll, and a scroll is what the
 * reference answers with two columns rather than with a disclosure.
 *
 * <p>So sections render in full, side by side, and the content area scrolls. What still opens a panel is
 * a row whose control genuinely cannot sit on a settings line -- the ball colour's eight swatches and
 * three channel sliders, the preset slots -- which is a limit of the control, not of the section's
 * length. See [GodSwordModules.groupNeedsPopup].
 *
 * <p>Columns come from `BoxWithConstraints` in **Dp**. A pixel threshold would answer a different
 * question at every scale setting: the scene's density is the game's GUI scale times Aurora's.
 */
@Composable
internal fun GodSwordInlinePanel(
    groups: List<GodSwordModules.Group>,
    holder: V9Holder,
    query: String,
    presetSlot: Int,
    onPresetSlot: (Int) -> Unit,
    onPresetLoad: (Int) -> Unit,
    onPresetSave: (Int) -> Unit,
    onOpenGroup: (GodSwordModules.Group) -> Unit,
    onChanged: () -> Unit
) {
    // Subscribe: the rows below read plain fields through the binding, so a value written without
    // a subscription changes the model and never repaints.
    @Suppress("UNUSED_EXPRESSION")
    com.yuan.client.aurora.LocalLayoutRevision.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoColumns = maxWidth >= Star.duplicateColumnMinWidth
        val inner = Modifier.fillMaxSize().auroraScroll(rememberScrollState())

        if (!twoColumns) {
            Column(inner.padding(horizontal = 12.dp, vertical = 8.dp)) {
                groups.forEachIndexed { index, group ->
                    EnterBlock(delayMs = staggerDelay(index), identity = group.id, fromY = 6) {
                        SectionBlock(group, holder, query, presetSlot, onPresetSlot, onPresetLoad,
                            onPresetSave, onOpenGroup, onChanged)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            return@BoxWithConstraints
        }

        // Two columns, filled in declaration order down the first before the second. Filling down rather
        // than across keeps a section whole and keeps a reader's expectation -- the left column is the
        // first half of the feature -- which filling across would break at the column boundary.
        val half = (groups.size + 1) / 2
        val columns = listOf(groups.take(half), groups.drop(half))
        Row(inner.padding(horizontal = 12.dp, vertical = 8.dp)) {
            columns.forEachIndexed { columnIndex, columnGroups ->
                Column(Modifier.weight(1f)) {
                    columnGroups.forEachIndexed { index, group ->
                        EnterBlock(delayMs = staggerDelay(index + columnIndex * half), identity = group.id, fromY = 6) {
                            SectionBlock(group, holder, query, presetSlot, onPresetSlot, onPresetLoad,
                                onPresetSave, onOpenGroup, onChanged)
                        }
                    }
                }
                if (columnIndex == 0) Spacer(Modifier.width(20.dp))
            }
        }
    }
}

/**
 * One section as the content area draws it: a label, then either its rows or the row that opens its panel.
 *
 * <p>A search always renders inline, even inside a section that would otherwise open a panel: the player
 * asked for these rows, and answering "it is in a panel somewhere" would make the search useless for
 * exactly the sections it helps most.
 */
@Composable
private fun SectionBlock(
    group: GodSwordModules.Group,
    holder: V9Holder,
    query: String,
    presetSlot: Int,
    onPresetSlot: (Int) -> Unit,
    onPresetLoad: (Int) -> Unit,
    onPresetSave: (Int) -> Unit,
    onOpenGroup: (GodSwordModules.Group) -> Unit,
    onChanged: () -> Unit
) {
    if (query.isEmpty() && GodSwordModules.groupNeedsPopup(group)) {
        SectionLabel(group.heading)
        CollapsedSectionRow(group, onOpen = { onOpenGroup(group) })
        return
    }
    SectionLabel(group.heading)
    group.rows.forEach { godRow ->
        when (godRow.row.id) {
            "ballColor" -> BallColourRow(holder, onChanged)
            "preset" -> PresetRow(holder, presetSlot, onPresetSlot, onPresetLoad, onPresetSave)
            else -> GodSwordRowLine(godRow, holder, onChanged)
        }
    }
}

/**
 * A section's label: small, dim, with a hairline above it.
 *
 * <p>Dim and small because its job is to divide a column, not to be read as content. Making it the same
 * weight as the rows under it would give twenty labels and sixty settings equal standing, and the page
 * becomes a wall.
 */
@Composable
private fun SectionLabel(name: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Star.hairline)
            )
            Text(
                name,
                fontSize = TypeScale.section,
                color = Star.textTertiary,
                fontFamily = auroraFont,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * The collapsed form of a section whose control cannot fit a settings line: a label and a row saying
 * what is inside.
 *
 * <p>The row states the section name and its row count, so the column reads as a table of contents rather
 * than as a list of buttons -- a player scanning for a setting can see where it lives before opening
 * anything.
 */
@Composable
private fun CollapsedSectionRow(
    group: GodSwordModules.Group,
    onOpen: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(Star.detailRowHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(if (hovered) Star.hover else Color.Transparent)
            .hoverable(interaction)
            .handCursor()
            .clickable(interactionSource = interaction, indication = null) { onOpen() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(IconSize.inline)) {
            AuroraIcon(AuroraIcon.ChevronRight, tint = Star.textSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            group.heading,
            fontSize = TypeScale.row,
            color = Star.textPrimary,
            fontFamily = auroraFont,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        Text(
            "${group.rows.size} 项",
            fontSize = TypeScale.state,
            color = Star.textTertiary,
            fontFamily = auroraFont
        )
    }
}
