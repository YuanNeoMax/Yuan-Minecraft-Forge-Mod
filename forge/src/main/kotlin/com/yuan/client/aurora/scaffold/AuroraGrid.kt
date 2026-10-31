package com.yuan.client.aurora.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.yuan.client.aurora.AuroraLook
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yuan.client.aurora.AuroraLayout
import com.yuan.client.aurora.anim.Motion
import com.yuan.client.aurora.anim.rememberAmbientPulse
import com.yuan.client.aurora.icons.AuroraIcon
import com.yuan.client.aurora.icons.IconSize
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale
import com.yuan.client.aurora.theme.auroraFont
import kotlin.math.roundToInt

/** One rail entry: a label, and nothing else. */
internal data class AuroraRailEntry(
    val id: String,
    val label: String
)

/**
 * One rail section: a named slot holding entries.
 *
 * <p>A section rather than a group heading, and the difference is this shape's whole point: a heading is
 * a label the code prints above some entries, while a section is something the player owns -- it can be
 * collapsed, reordered against its siblings, and entries can be moved into and out of it. The heading it
 * replaces could do none of those, because nothing in the data model represented "the entries under this
 * label".
 *
 * <p>`id` is stable and separate from `name` so a rename does not orphan the section's stored values.
 */
internal data class RailSection(
    val id: String,
    val name: String,
    val entries: List<AuroraRailEntry>,
    val collapsed: Boolean = false
)

/**
 * Moves one entry to another slot in the flat draw order, possibly into a different section.
 *
 * <p>The move is expressed in flat indices because that is the only coordinate the drag can produce: a
 * single column has no angle to read, so the drop target is derived from travel over a fixed pitch, and
 * that pitch spans sections without knowing where the boundaries are. Translating back into
 * (section, position) here is what lets an entry cross a boundary at all -- without it, a drag could
 * only ever permute within one section.
 *
 * <p>An empty section keeps its name and stays on screen: a player emptying a slot has not asked to
 * delete it, and a section that vanished on its last removal could not be refilled by dragging into it.
 */
internal fun moveEntry(
    sections: List<RailSection>,
    from: Int,
    to: Int,
    flat: List<AuroraRailEntry>
): List<RailSection> {
    if (from == to || from !in flat.indices || to !in flat.indices) return sections
    val moved = flat[from]
    // Mutable copies: `RailSection.entries` is an immutable `List`, so removing and inserting needs a
    // separate working type. Copying keeps the caller's list untouched until the result is stored.
    val working: MutableList<MutableRailSection> = sections.map { section ->
        MutableRailSection(section.id, section.name, section.entries.toMutableList(), section.collapsed)
    }.toMutableList()

    // Remove first, then insert: removing shifts every later index, so an insert before the removal
    // would land one slot away from where the pointer is.
    working.forEach { section ->
        section.entries.removeAll { it.id == moved.id }
    }
    var remaining = to
    for (section in working) {
        if (remaining <= section.entries.size) {
            section.entries.add(remaining.coerceIn(0, section.entries.size), moved)
            return working.map { it.toRailSection() }
        }
        remaining -= section.entries.size
    }
    // Past every section: the last one takes it, which is what a drag released at the very bottom means.
    if (working.isEmpty()) return sections
    working.last().entries.add(moved)
    return working.map { it.toRailSection() }
}

/** A section mid-edit, where the entry list has to be mutable. */
private data class MutableRailSection(
    val id: String,
    val name: String,
    val entries: MutableList<AuroraRailEntry>,
    val collapsed: Boolean
) {
    fun toRailSection(): RailSection = RailSection(id, name, entries.toList(), collapsed)
}

/**
 * The left rail: named sections, each holding entries, each with its own settings.
 *
 * <p>Selection is always available. The arrangement mode that used to gate reordering is gone; the
 * gestures are resident and separated by button and by distance.
 *
 * <p>Three gestures share one entry: select is a left click, reorder is a LEFT LONG-PRESS followed by
 * a drag (the long press keeps an accidental press-and-move from silently rewriting the stored
 * sections), and the entry's own settings are a right click. Long-pressing the rail's blank surface
 * no longer moves the column -- moving the window is the title bar's job, and two ways to move the
 * same thing is one too many.
 */
@Composable
internal fun AuroraRail(
    sections: List<RailSection>,
    selectedId: String,
    onSelect: (String) -> Unit,
    layout: AuroraLayout,
    onSectionsChange: (List<RailSection>) -> Unit,
    look: AuroraLook,
    onOpenMenu: () -> Unit,
    onOpenSectionMenu: (String) -> Unit,
    /**
     * A right-click on one ENTRY: the entry's own scope, carrying the module id and the category
     * it currently sits in.
     *
     * <p>Three scopes share the rail -- entry, category, whole rail -- and the scope is chosen by
     * WHERE the right-click lands. A single `onOpenMenu` could not express that: it would have to
     * guess, and the guess would be wrong for two of the three surfaces.
     */
    onOpenEntryMenu: (moduleId: String, sectionId: String) -> Unit,
    onMoved: () -> Unit,
    /**
     * The keyboard focus model.
     *
     * <p>The rail draws a focus ring around the entry the keyboard is on, so a player who never
     * touches the mouse can still see where they are. Null when the window has no keyboard path
     * (the test harness composes the rail on its own), in which case no ring is drawn.
     */
    focus: FocusState? = null
) {
    val density = LocalDensity.current
    // The drag pitch uses the RAIL's own item height: it is one number for the whole column, and a
    // per-entry height would make a single "rows travelled" delta mean different distances at
    // different points of the list. Per-entry heights are honoured when drawing (below), not here.
    val itemHeight = look.railItemHeight
    val pitch = with(density) { itemHeight.toPx() }
    var dragFrom by remember { mutableStateOf(-1) }
    var dragTarget by remember { mutableStateOf(-1) }

    // Resolved before the layout block: the Look accessors are composable, and `graphicsLayer`'s lambda
    // runs in GraphicsLayerScope, which is not -- reading one inside it has no composition to read from.
    val railOffsetX = look.railOffsetX
    val railOffsetY = look.railOffsetY
    val offsetXPx = with(density) { railOffsetX.toPx() }
    val offsetYPx = with(density) { railOffsetY.toPx() }

    // The flat draw order. The drag maths needs one index space rather than one per section, or a drag
    // crossing a boundary would compute its target inside the wrong list and land nowhere.
    val flat = sections.flatMap { it.entries }

    Column(
        Modifier
            .fillMaxHeight()
            // The rail scrolls: a player with many sections (or a large UI scale) otherwise has
            // entries clipped at the bottom with no way to reach them. The thin bar appears only
            // while there is something to scroll, and long-press reordering still wins over the
            // scroll once a drag is armed, because a scrollable gives way to a consuming gesture.
            .auroraScroll(rememberScrollState())
            .padding(top = 10.dp, bottom = 10.dp)
            .graphicsLayer {
                translationX = offsetXPx
                translationY = offsetYPx
            }
    ) {
        Text(
            "MODULE",
            fontSize = TypeScale.railTag,
            color = look.railGroupColor,
            fontFamily = auroraFont,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 6.dp)
        )

        var flatIndex = -1
        sections.forEach { section ->
            SectionHeader(
                section = section,
                look = look,
                onToggleCollapse = {
                    onSectionsChange(
                        sections.map {
                            if (it.id == section.id) it.copy(collapsed = !it.collapsed) else it
                        }
                    )
                },
                onOpenMenu = { onOpenSectionMenu(section.id) }
            )
            if (section.collapsed) return@forEach

            section.entries.forEach { entry ->
                flatIndex += 1
                val index = flatIndex
                val selected = entry.id == selectedId
                val interaction = remember(entry.id) { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val isTarget = dragFrom >= 0 && dragTarget == index

                // THE ENTRY'S OWN RESOLVED APPEARANCE: entry override -> category -> rail.
                // Reading `look.rail*` directly here is what made per-entry and per-category
                // configuration impossible; the cascade is stated once in AuroraLayout.resolveRail.
                val resolved = layout.resolveRail(entry.id, section.id)
                val entryHeight = resolved.itemHeight.dp
                val entryCorner = resolved.itemCorner.dp
                val markerWidth = resolved.markerWidth.dp
                val entryPadding = resolved.itemPadding.dp
                val entryFont = resolved.fontSize.sp
                val selectedPlate = Color(resolved.selectedPlate)
                val textSelected = Color(resolved.textSelected)
                val textRest = Color(resolved.textColor)
                val markerColor = Color(resolved.markerColor)

                // WHETHER THE KEYBOARD IS HERE. Read with `by` on purpose: the ring has to appear
                // the moment focus moves, and this recomposes only when the focused index actually
                // changes -- not per frame, since nothing here animates.
                val focused = focus != null &&
                    focus.keyboardActive &&
                    focus.region == FocusRegion.Rail &&
                    focus.index == index

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(entryHeight)
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(entryCorner))
                        .background(
                            when {
                                isTarget -> Star.starlightTint
                                selected -> selectedPlate
                                hovered -> Star.hover
                                else -> Color.Transparent
                            }
                        )
                        // THE FOCUS RING, and why it is a ring rather than the hover fill.
                        //
                        // A keyboard user cannot see a mouse-hover highlight -- they never hovered,
                        // and the hover state is not even set. Reusing the hover background would
                        // have made "focused" and "hovered" the same picture, so the one signal a
                        // keyboard player needs would be indistinguishable from a mouse artefact.
                        // An outline in the accent colour is unambiguous and, unlike a fill, does not
                        // replace the selected-entry plate underneath it.
                        //
                        // Drawn as a border on the same clipped shape, so it follows the entry's own
                        // resolved corner radius; it appears only while the keyboard is the input
                        // in use (see FocusState.keyboardActive).
                        .then(
                            if (focused) {
                                Modifier.border(2.dp, Star.accent, RoundedCornerShape(entryCorner))
                            } else {
                                Modifier
                            }
                        )
                        // A right-click opens THIS ENTRY's settings, scoped to the entry. Right
                        // rather than left because left is select and long-press-drag, and a menu
                        // sharing the primary button turns every mis-aimed drag into a popup.
                        .pointerInput(entry.id, section.id) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: continue
                                    if (event.type == PointerEventType.Press &&
                                        event.buttons.isSecondaryPressed
                                    ) {
                                        change.consume()
                                        onOpenEntryMenu(entry.id, section.id)
                                    }
                                }
                            }
                        }
                        // Reorder is a LEFT LONG-PRESS drag, resident. Long press because the
                        // entry also owns the plain click for selecting: two gestures on one surface
                        // need a difference the player can feel, and the long press is it. A plain
                        // drag past the slop would make every imprecise click a potential rewrite
                        // of the stored sections.
                        .pointerInput(entry.id, sections) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { dragFrom = index; dragTarget = index },
                                onDragEnd = {
                                    if (dragFrom >= 0 && dragTarget >= 0 && dragTarget != dragFrom) {
                                        onSectionsChange(moveEntry(sections, dragFrom, dragTarget, flat))
                                    }
                                    dragFrom = -1
                                    dragTarget = -1
                                },
                                onDragCancel = { dragFrom = -1; dragTarget = -1 }
                            ) { _, delta ->
                                // The drop slot from accumulated travel over the entry pitch: in
                                // a single column there is no angle to read, so travel is the
                                // only honest coordinate, and it is exact because the pitch is
                                // fixed.
                                dragTarget = (index + (delta.y / pitch).roundToInt())
                                    .coerceIn(0, flat.size - 1)
                            }
                        }
                        .hoverable(interaction)
                        .handCursor()
                        // Selecting is the rail's primary action, not something the arrangement mode may
                        // take away. Clicking and dragging coexist without a mode: the detector claims
                        // the pointer only past the touch slop, so a press-and-release stays a click.
                        .clickable(interactionSource = interaction, indication = null) {
                            onSelect(entry.id)
                        }
                        .padding(start = 6.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val markerTint = if (selected) markerColor else Color.Transparent
                    // THE SELECTED MARKER BREATHES, very slightly.
                    //
                    // This is the ambient pulse's one legitimate consumer, and it is chosen
                    // carefully: two DP wide, redrawn at the frame rate, on a surface that already
                    // repaints -- the whole cost is a couple of extra draw ops, in exchange for the
                    // one element on screen that benefits from saying "I am the active one".
                    //
                    // The pulse is read inside `drawBehind`, NEVER in composition. It is a State
                    // that changes every frame; a composition-phase read would recompose this row
                    // sixty times a second for the entire session. Below FULL the pulse returns a
                    // constant, so this collapses to the plain static bar with no animation at all.
                    //
                    // The wobble is 1..0.86 of full alpha rather than 1..0: a marker that fades to
                    // nothing reads as a fault, one that brightens and settles reads as alive.
                    val pulseSource = if (selected) rememberAmbientPulse() else null
                    Box(
                        Modifier
                            .width(markerWidth)
                            .fillMaxHeight()
                            .padding(vertical = 7.dp)
                            .drawBehind {
                                // `toPx()` is a DrawScope member -- it needs the density -- so the
                                // radius is computed here rather than hoisted above the modifier.
                                val radius = CornerRadius(1.dp.toPx())
                                if (pulseSource != null) {
                                    val p = pulseSource.value
                                    drawRoundRect(
                                        color = markerTint,
                                        alpha = 0.86f + 0.14f * p,
                                        cornerRadius = radius
                                    )
                                } else {
                                    drawRoundRect(color = markerTint, cornerRadius = radius)
                                }
                            }
                    )
                    Spacer(Modifier.width(entryPadding))
                    // Text only. The reference's rail has no icons, and it is right: an icon beside every
                    // entry is a second label that says less than the word next to it, and eight of them
                    // down a column is the busiest thing on screen. The icons stay for the content
                    // header, where the selected feature is stated once in a place that has room.
                    Text(
                        entry.label,
                        fontSize = entryFont,
                        color = if (selected) textSelected else textRest,
                        fontFamily = auroraFont,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1
                    )
                    Spacer(Modifier.weight(1f))
                    // The grip states that the entry is movable. Its presence is not the
                    // affordance -- the long-press drag is -- but a row that reorders without
                    // saying so reads as a list that changed on its own.
                    AuroraIcon(
                        AuroraIcon.Grip,
                        tint = Star.textTertiary,
                        modifier = Modifier.size(IconSize.inline)
                    )
                }
            }
        }
    }
}

/**
 * One section's heading: its name, a collapse control, and its own settings on a right-click.
 *
 * <p>The heading is a control rather than a label. That is what makes a section a slot: clicking it
 * folds the slot away, right-clicking opens the slot's own numbers, and the entries below are what the
 * slot currently holds.
 */
@Composable
private fun SectionHeader(
    section: RailSection,
    look: AuroraLook,
    onToggleCollapse: () -> Unit,
    onOpenMenu: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(1.dp)
                    .background(Star.hairline)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (hovered) Star.hover else Color.Transparent)
                    .pointerInput(section.id) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                if (event.type == PointerEventType.Press &&
                                    event.buttons.isSecondaryPressed
                                ) {
                                    change.consume()
                                    onOpenMenu()
                                }
                            }
                        }
                    }
                    .hoverable(interaction)
                    .handCursor()
                    .clickable(interactionSource = interaction, indication = null) { onToggleCollapse() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(IconSize.inline)) {
                    AuroraIcon(
                        AuroraIcon.ChevronRight,
                        tint = look.railGroupColor,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = if (section.collapsed) 0f else 90f
                        }
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    section.name,
                    fontSize = TypeScale.railTag,
                    color = look.railGroupColor,
                    fontFamily = auroraFont,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${section.entries.size}",
                    fontSize = TypeScale.railTag,
                    color = Star.textTertiary,
                    fontFamily = auroraFont
                )
            }
        }
    }
}

/**
 * The rail's footer: who these settings belong to.
 *
 * <p>Present because this window edits one sword, and a player holding two of them needs to know which
 * one is on screen. The block is drawn only when there is a player to name -- an absent one would be a
 * line of placeholder text claiming the window is configured for nobody.
 */
@Composable
internal fun AuroraRailFooter(playerName: String?, itemName: String) {
    if (playerName == null) return
    Column(
        Modifier
            .fillMaxWidth()
            .height(Star.railUserBlockHeight)
            .drawBehind {
                drawLine(
                    Star.hairline,
                    Offset(0f, 0.5f),
                    Offset(size.width, 0.5f),
                    1f
                )
            }
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            playerName,
            fontSize = TypeScale.railUser,
            color = Star.textPrimary,
            fontFamily = auroraFont,
            maxLines = 1
        )
        Text(
            itemName,
            fontSize = TypeScale.hint,
            color = Star.textTertiary,
            fontFamily = auroraFont,
            maxLines = 1
        )
    }
}

/**
 * The content header: the current group's mark and name, its module count, and the search field.
 *
 * <p>Exactly one search field exists on screen, and this is it. An earlier version had one here *and*
 * one in the shell header, which is the same control twice: the player sees two identical fields and
 * has to work out whether they filter the same thing.
 *
 * <p>The count sits under the group's name rather than beside it, so the row's right end belongs to the
 * search field alone.
 */
@Composable
internal fun AuroraContentHeader(
    icon: AuroraIcon,
    title: String,
    subtitle: String,
    query: String,
    onQuery: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(Star.headerHeight)
            .bottomHairline()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuroraIcon(icon, tint = Star.textPrimary, modifier = Modifier.size(IconSize.panelHeader))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                title,
                fontSize = TypeScale.panelTitle,
                color = Star.textPrimary,
                fontFamily = auroraFont,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                fontSize = TypeScale.hint,
                color = Star.textTertiary,
                fontFamily = auroraFont,
                maxLines = 1
            )
        }
        Spacer(Modifier.weight(1f))
        AuroraSearchField(query, onQuery)
    }
}

/**
 * The search field: an icon, a placeholder and a caret.
 *
 * <p>Focus is drawn as a hairline that brightens, not a border that appears. A border appearing on
 * focus changes the control's box, which on a 60px header shifts the whole row by a pixel -- visible,
 * and exactly the kind of thing that makes an interface feel unsettled.
 */
@Composable
internal fun AuroraSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp = 200.dp
) {
    val interaction = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val expanded = focused || value.isNotEmpty()
    val animWidth by animateDpAsState(
        targetValue = if (expanded) width else 30.dp,
        animationSpec = tween(Motion.dur(Motion.BASE_MS), easing = Motion.enter),
        label = "searchWidth"
    )

    Row(
        Modifier
            .width(animWidth)
            .height(30.dp)
            .clip(RoundedCornerShape(Star.controlRadius))
            .background(Star.well)
            .border(
                1.dp,
                if (focused) Star.accentDim else Star.hairline,
                RoundedCornerShape(Star.controlRadius)
            )
            .hoverable(interaction)
            .handCursor()
            .clickable(interactionSource = interaction, indication = null) { focused = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(9.dp))
        AuroraIcon(
            AuroraIcon.Search,
            tint = if (expanded) Star.textSecondary else Star.textTertiary,
            modifier = Modifier.size(IconSize.control)
        )
        if (expanded) {
            Spacer(Modifier.width(7.dp))
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = TypeScale.value,
                        color = Star.textPrimary,
                        fontFamily = auroraFont
                    ),
                    cursorBrush = SolidColor(Star.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    "搜索设置",
                                    fontSize = TypeScale.value,
                                    color = Star.textTertiary,
                                    fontFamily = auroraFont,
                                    maxLines = 1
                                )
                            }
                            inner()
                        }
                    }
                )
            }
            Spacer(Modifier.width(9.dp))
        }
    }
}

