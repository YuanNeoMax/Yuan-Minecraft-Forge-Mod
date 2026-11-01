package com.yuan.client.aurora.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.anim.EnterBlock
import com.yuan.client.aurora.anim.Motion
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
    onChanged: () -> Unit,
    /**
     * The sections the player has folded shut, keyed by `module/heading`.
     *
     * <p>Passed in rather than read from the layout: this panel is a pure function of its
     * arguments, which is what lets the crossfade render an outgoing copy without it reaching back
     * into state that has already moved on.
     */
    foldedSections: Set<String> = emptySet(),
    /** Toggles one section's fold. The caller owns persistence. */
    onToggleFold: (String) -> Unit = {},
    /**
     * Whether the rows play their staggered entrance.
     *
     * <p>False for the OUTGOING half of a module crossfade: a copy on its way out must not also
     * stage its rows in one after another, or the two animations fight over the same pixels.
     */
    staging: Boolean = true
) {
    // Subscribe: the rows below read plain fields through the binding, so a value written without
    // a subscription changes the model and never repaints.
    @Suppress("UNUSED_EXPRESSION")
    com.yuan.client.aurora.LocalLayoutRevision.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val inner = Modifier.fillMaxSize().auroraScroll(rememberScrollState())

        // THE ONE-COLUMN / TWO-COLUMN SWITCH IS A MORPH, NOT A BRANCH.
        //
        // It used to be `if (maxWidth >= 560.dp)` composing either a Column or a Row: crossing that
        // line replaced every child in a single frame, which is the "硬切" that reads as a jump cut.
        // Here the same children are placed by one interpolation, so the rearrangement is something
        // the eye can follow.
        //
        // `morph` is animated rather than taken from the width directly, and that is the whole point:
        // a window resize delivers its new width in one step, so a value derived from the width would
        // also arrive in one step. The animation is what turns that step into a movement.
        val target = if (maxWidth >= Star.duplicateColumnMinWidth) 1f else 0f
        val morph = animateFloatAsState(
            targetValue = target,
            animationSpec = tween(Motion.dur(Motion.SLOW_MS), easing = Motion.enter),
            label = "columnMorph"
        )

        MorphingColumns(
            groupCount = groups.size,
            progress = morph,
            modifier = inner.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ONE list of children, in declaration order. The layout decides where each one sits;
            // composing two different trees for the two arrangements is exactly what made the switch
            // unfollowable.
            groups.forEachIndexed { index, group ->
                EnterBlock(
                    delayMs = staggerDelay(index),
                    identity = group.id,
                    enabled = staging,
                    fromY = 6
                ) {
                    SectionBlock(group, holder, query, presetSlot, onPresetSlot, onPresetLoad,
                        onPresetSave, onOpenGroup, onChanged,
                        folded = group.id in foldedSections,
                        onToggleFold = { onToggleFold(group.id) })
                }
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
    onChanged: () -> Unit,
    /** Whether this section is folded shut. See AuroraLayout.foldedSections. */
    folded: Boolean,
    onToggleFold: () -> Unit
) {
    // A SEARCH OVERRIDES THE FOLD. The player asked for these rows; hiding them behind a heading
    // they had folded earlier would answer the question with "it is somewhere in here".
    val showRows = query.isNotEmpty() || !folded

    if (query.isEmpty() && GodSwordModules.groupNeedsPopup(group)) {
        SectionLabel(group.heading, folded = folded, onToggle = onToggleFold)
        if (showRows) {
            CollapsedSectionRow(group, onOpen = { onOpenGroup(group) })
        }
        return
    }

    SectionLabel(group.heading, folded = folded, onToggle = onToggleFold)
    // The rows collapse to zero height rather than disappearing in one frame: a section that simply
    // vanished would make everything below it jump up the page, and the player would lose the place
    // they were reading. Animated height keeps the movement legible.
    AnimatedVisibility(
        visible = showRows,
        enter = expandVertically(
            animationSpec = tween(Motion.dur(Motion.BASE_MS), easing = Motion.enter)
        ) + fadeIn(tween(Motion.dur(Motion.QUICK_MS))),
        exit = shrinkVertically(
            animationSpec = tween(Motion.dur(Motion.BASE_MS), easing = Motion.standard)
        ) + fadeOut(tween(Motion.dur(Motion.QUICK_MS)))
    ) {
        Column {
            group.rows.forEach { godRow ->
                when (godRow.row.id) {
                    "ballColor" -> BallColourRow(holder, onChanged)
                    "preset" -> PresetRow(holder, presetSlot, onPresetSlot, onPresetLoad, onPresetSave)
                    else -> GodSwordRowLine(godRow, holder, onChanged)
                }
            }
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
/**
 * A section's heading, which is also the fold control.
 *
 * <p>THE HEADING IS THE TARGET, not a chevron beside it. It is already the row a reader looks at
 * when deciding whether they care about the section below, and it is the full width of the column --
 * a small chevron would be a second thing to aim at, on a row whose meaning is unambiguous.
 *
 * <p>A folded heading dims its text and flips the marker, so the state is readable without counting
 * the rows underneath. The marker is a drawn triangle rather than an icon: three lines of canvas
 * cost less than an asset lookup for a shape this simple, and it can rotate with the animation
 * instead of swapping glyphs.
 */
@Composable
private fun SectionLabel(name: String, folded: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    // The marker turns as the section folds, over the same duration the rows use, so the two read as
    // one movement. An instantly-flipping triangle beside rows that are still collapsing would look
    // like two separate events.
    val turn by animateFloatAsState(
        targetValue = if (folded) 1f else 0f,
        animationSpec = tween(Motion.dur(Motion.QUICK_MS), easing = Motion.standard),
        label = "foldMarker"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .hoverable(interaction)
            .handCursor()
            .clickable(interactionSource = interaction, indication = null) { onToggle() }
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (hovered) Star.hairlineStrong else Star.hairline)
            )
            Row(
                Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .graphicsLayer { rotationZ = 180f * turn },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(8.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.15f, h * 0.30f)
                            lineTo(w * 0.85f, h * 0.30f)
                            lineTo(w * 0.50f, h * 0.78f)
                            close()
                        }
                        drawPath(path, if (folded) Star.textTertiary else Star.textDisabled)
                    }
                }
                Spacer(Modifier.width(5.dp))
                Text(
                    name,
                    fontSize = TypeScale.section,
                    // A folded heading is dimmer, not hidden: it still says what is down there.
                    color = if (folded) Star.textTertiary else Star.textSecondary,
                    fontFamily = auroraFont
                )
            }
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
