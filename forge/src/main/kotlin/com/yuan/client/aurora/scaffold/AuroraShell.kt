package com.yuan.client.aurora.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.AuroraLayout
import com.yuan.client.aurora.anim.openProgress
import com.yuan.client.aurora.AuroraLook
import com.yuan.client.aurora.LocalAuroraLayout
import com.yuan.client.aurora.icons.AuroraIcon
import com.yuan.client.aurora.icons.IconSize
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale
import com.yuan.client.aurora.theme.auroraFont
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * The Aurora shell: one translucent pane holding a header, a rail and a content area.
 *
 * <p>The pane is inset from the screen edge so the blurred world shows on every side. That inset is
 * also what makes the blur legible: a pane flush to the edges gives the eye no reference to compare
 * the glass against, and the frosting reads as a flat grey wash instead.
 *
 * <p>Moving and sizing the window is [AuroraWindowFrame]'s job now, and the gestures are RESIDENT:
 * there is no arrangement mode gating them. What remains of "arranging" is the per-element
 * customization menu, opened by a right-click on the element being customized -- the shell header's
 * free area opens the window's own numbers, the rail's entries open theirs, and so on.
 *
 * <p>A detail panel is not a child of this layout but a sibling over it -- see [AuroraFloatingPanel]
 * -- which is what lets panels cascade sideways without reflowing the grid underneath.
 */
@Composable
internal fun AuroraShell(
    title: String,
    subtitle: String,
    actions: @Composable () -> Unit,
    rail: @Composable () -> Unit,
    content: @Composable (IntSize) -> Unit,
    layout: AuroraLayout,
    cursor: ResizeCursorState,
    onOpenMenu: () -> Unit = {},
    onMoved: () -> Unit = {}
) {
    // Every dimension and colour the frame uses comes from the arrangement layer, so the numbers its
    // menu exposes have something to move. The tokens in `Star` stay as the defaults those start at.
    val look = AuroraLook(LocalAuroraLayout.current)
    val density = androidx.compose.ui.platform.LocalDensity.current

    // The window's own box: an inset from the screen edge until a player resizes it, and a
    // player-sized rectangle after. Stored as fractions rather than pixels so a box set on one monitor
    // comes back correct on another.
    var available by remember { mutableStateOf(IntSize.Zero) }

    // The live box, created on first sight once the stage is measured (not on the first drag), so
    // the gesture frame has a rectangle from its first frame. Reading `frame` unconditionally
    // before the box check is what keeps the very first drag visible.
    var frame by remember { mutableStateOf(0) }
    @Suppress("UNUSED_EXPRESSION")
    frame

    val insetPx = with(density) { look.shellInset.toPx() }
    val box = if (available == IntSize.Zero) {
        null
    } else {
        layout.windowBox(
            AuroraLayout.CardBox(
                insetPx / available.width,
                insetPx / available.height,
                1f - 2f * insetPx / available.width,
                1f - 2f * insetPx / available.height,
                0
            )
        )
    }

    Box(Modifier
        .fillMaxSize()
        .onSizeChanged { available = it }
        // The cursor tracker rides the ROOT's own modifier: an ancestor observer sees every
        // pointer event along the hit path without competing for any of it. A full-size child
        // Box with a pointerInput wins the hit test everywhere and starves every control under
        // it -- the exact regression the dual-channel diagnosis caught.
        .trackCursor(cursor)
    ) {
        if (box != null) {
            val maximized = layout.windowMaximized
            AuroraWindowFrame(
                box = box,
                stage = available,
                headerHeight = look.headerHeight,
                buttonReserve = WINDOW_BUTTON_RESERVE,
                layout = layout,
                cursor = cursor,
                onMoved = onMoved,
                onOpenMenu = onOpenMenu,
                onMaximizeToggle = { toggleMaximize(layout, box, onMoved) }
            ) {
                ShellBody(
                    title = title,
                    subtitle = subtitle,
                    actions = actions,
                    rail = rail,
                    content = content,
                    stagePx = available,
                    look = look,
                    maximized = maximized,
                    layout = layout,
                    onMaximizeToggle = { toggleMaximize(layout, box, onMoved) }
                )
            }
            // The stage-level resize cursor overlay follows every pointer, over every window.
            AuroraResizeCursor(cursor)
        } else {
            // Only before the first measurement, when there is no pixel size to express fractions in.
            Box(Modifier.fillMaxSize().padding(look.shellInset)) {
                ShellBody(
                    title = title,
                    subtitle = subtitle,
                    actions = actions,
                    rail = rail,
                    content = content,
                    stagePx = available,
                    look = look,
                    maximized = false,
                    layout = layout,
                    onMaximizeToggle = {}
                )
            }
        }
    }
}

/** The header's right-hand control area the move band must not cover: maximize + save + close. */
internal val WINDOW_BUTTON_RESERVE: Dp = 300.dp



/** The shell's chrome and body, drawn inside the gesture frame's rectangle. */
@Composable
private fun ShellBody(
    title: String,
    subtitle: String,
    actions: @Composable () -> Unit,
    rail: @Composable () -> Unit,
    content: @Composable (IntSize) -> Unit,
    stagePx: IntSize,
    look: AuroraLook,
    maximized: Boolean,
    layout: AuroraLayout,
    onMaximizeToggle: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer { shadowElevation = 24f }
            .clip(RoundedCornerShape(if (maximized) 0.dp else look.shellCorner))
            .background(
                Brush.verticalGradient(
                    listOf(look.shellBgTop, look.shellBgBottom)
                )
            )
            .border(
                look.shellBorderWidth, look.shellBorderColor,
                RoundedCornerShape(if (maximized) 0.dp else look.shellCorner)
            )
    ) {
        ShellHeader(title, subtitle, actions, look, layout, maximized, onMaximizeToggle)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(
                Modifier
                    .width(look.railWidth)
                    .fillMaxHeight()
                    .rightHairline()
            ) {
                rail()
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                content(stagePx)
            }
        }
    }
}

/**
 * The shell header: the module's mark and name, the window-level actions on the right, and the
 * maximize/restore button among them.
 *
 * <p>The header is the one place the interface states what it is. Everything else on screen is
 * content, and a content area with no title reads as a page that failed to load.
 *
 * <p>The maximize button lives here rather than behind a double-click alone: the double-click is
 * the fast path for players who know it, and the button is the discoverable one for everyone else.
 * Windows has both, and so does this window.
 */
@Composable
private fun ShellHeader(
    title: String,
    subtitle: String,
    actions: @Composable () -> Unit,
    look: AuroraLook,
    layout: AuroraLayout,
    maximized: Boolean,
    onMaximizeToggle: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(look.headerHeight)
            .bottomHairline()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuroraIcon(
            AuroraIcon.Starlight,
            tint = Star.starlight,
            modifier = Modifier.size(IconSize.panelHeader)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                title,
                fontSize = TypeScale.shellTitle,
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
        HeaderIconButton(
            icon = if (maximized) AuroraIcon.Restore else AuroraIcon.Maximize,
            contentDescription = if (maximized) "还原" else "最大化",
            onClick = onMaximizeToggle
        )
        Spacer(Modifier.width(8.dp))
        actions()
    }
}

/** A small square header button: icon only, hover plate, no text. */
@Composable
internal fun HeaderIconButton(
    icon: AuroraIcon,
    contentDescription: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = Star.textSecondary,
    filled: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    filled -> Star.on
                    hovered -> Star.hover
                    else -> androidx.compose.ui.graphics.Color.Transparent
                }
            )
            .hoverable(interaction)
            .handCursor()
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AuroraIcon(
            icon,
            tint = if (filled) androidx.compose.ui.graphics.Color(0xFF15151A) else tint,
            modifier = Modifier.size(IconSize.control)
        )
    }
}

/**
 * A floating detail panel: a cascaded settings surface over the grid, in the same gesture frame as
 * the main window.
 *
 * <p>Cascaded rather than docked. The reference opens a module's settings *over* the list, offset
 * to the right, and keeps the list visible behind it -- which is what lets a player see which
 * module they came from, and open a second one without losing the first. Several may be open at
 * once; clicking one raises it, and the pin button keeps a panel above every unpinned one.
 *
 * <p>Moving and sizing are RESIDENT, like the main window's: drag the header, pull the edges, and
 * the panel remembers its box. There is no mode that arms them, because the panel having grips
 * only inside a mode is what made it read as "cannot be moved at all".
 *
 * <p>[stageWidth] and [stageHeight] are in **Dp**: the box stores fractions, and every delta
 * feeding those fractions is in dp too, so keeping the stage in the same unit makes the arithmetic
 * unitless.
 */
@Composable
internal fun BoxScope.AuroraFloatingPanel(
    box: AuroraLayout.CardBox,
    stageWidth: Float,
    stageHeight: Float,
    layout: AuroraLayout,
    drag: AuroraPanelDrag,
    cursor: ResizeCursorState,
    title: String,
    subtitle: String,
    onRaise: () -> Unit,
    onGeometryChange: () -> Unit,
    onClose: () -> Unit,
    onOpenMenu: () -> Unit = {},
    /**
     * True while this panel is playing its dismissal.
     *
     * <p>The panel stays in the composition for the length of that animation, so the screen marks it
     * rather than removing it: a surface that fades in must also fade out, and a list that dropped
     * the id immediately could only ever blink it away.
     */
    dismissing: Boolean = false,
    content: @Composable () -> Unit
) {
    if (stageWidth <= 0f || stageHeight <= 0f) return
    val panelLook = AuroraLook(LocalAuroraLayout.current)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val densityValue = density.density

    // THE PANEL'S ENTRANCE, and the reason it exists at all: without it a floating panel was laid
    // out at full size and full opacity in its first frame -- it POPPED into being. A surface that
    // appears should arrive: a short fade with a small scale-up, on the window's own open timing.
    //
    // Keyed by the panel's identity, so two panels opened in the same frame each ramp separately
    // rather than sharing one animation. It is already collapsed to 1f below FULL and at OFF (see
    // openProgress), so a motion-sensitive player gets the panel immediately and unconditioned.
    //
    // Read in the layer below, never during composition: the value moves every frame of the ramp.
    val appear = openProgress(title, shown = !dismissing)

    // The gesture frame resolves the box in Dp space, so the stage is handed over in the same
    // unit it measures fractions against.
    val stagePx = IntSize(
        (stageWidth * densityValue).toInt().coerceAtLeast(1),
        (stageHeight * densityValue).toInt().coerceAtLeast(1)
    )

    AuroraWindowFrame(
        box = box,
        stage = stagePx,
        headerHeight = panelLook.panelHeaderHeight,
        buttonReserve = PANEL_BUTTON_RESERVE,
        layout = layout,
        cursor = cursor,
        // The panel's header owns its drag (see PanelHeader.panelDragBand), so the frame's
        // overlay band is switched off here. Two drag surfaces for one window would overlap in
        // the same z tier, and an overlay band above the panel is also above its top rows -- a
        // press meant for a control there would land on the band.
        moveBand = false,
        onMoved = onGeometryChange,
        onOpenMenu = onOpenMenu
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    shadowElevation = 18f
                    translationY = drag.parallaxY * layout.parallax * 4f
                    // The entrance, in the same layer as the parallax: both are pure transforms, so
                    // both belong in the draw phase. Scaling from just under 1 rather than from 0 --
                    // a panel growing from nothing reads as a zoom, while this reads as a surface
                    // settling forward into place.
                    val p = appear.value
                    alpha = p
                    scaleX = 0.96f + 0.04f * p
                    scaleY = 0.96f + 0.04f * p
                }
                .clip(RoundedCornerShape(panelLook.panelCorner))
                .background(panelLook.panelBg)
                .border(
                    panelLook.panelBorderWidth, panelLook.panelBorderColor,
                    RoundedCornerShape(panelLook.panelCorner)
                )
        ) {
            PanelHeader(
                title = title,
                subtitle = subtitle,
                box = box,
                layout = layout,
                drag = drag,
                stageWidth = stageWidth,
                stageHeight = stageHeight,
                onRaise = onRaise,
                onGeometryChange = onGeometryChange,
                onClose = onClose
            )
            content()
        }
    }
}

/** Width at the panel header's right for the pin and close buttons. */
private val PANEL_BUTTON_RESERVE: Dp = 84.dp

/**
 * The panel's header: its name (centred), the pin toggle and the close control.
 *
 * <p>The close control is always present and always in the same place. A cascaded panel that cannot
 * be closed without going back to the list it came from is a modal wearing a panel's clothes.
 *
 * <p>The pin button states its panel's tier: filled icon when pinned. Pinned panels stay above
 * every unpinned one no matter what is raised -- that is the whole point of the button, and the
 * depth renumbering in [AuroraLayout.renumberByPin] is what makes it hold.
 */
@Composable
private fun PanelHeader(
    title: String,
    subtitle: String,
    box: AuroraLayout.CardBox,
    layout: AuroraLayout,
    drag: AuroraPanelDrag,
    stageWidth: Float,
    stageHeight: Float,
    onRaise: () -> Unit,
    onGeometryChange: () -> Unit,
    onClose: () -> Unit
) {
    val panelLook = AuroraLook(LocalAuroraLayout.current)
    val panelId = "panel-$title"
    // The drag band measures pointer travel in PIXELS, so the stage must arrive in pixels. The
    // panel computes its stage in Dp for layout; this is the one conversion.
    val stagePxW = with(androidx.compose.ui.platform.LocalDensity.current) {
        stageWidth * density
    }
    val stagePxH = with(androidx.compose.ui.platform.LocalDensity.current) {
        stageHeight * density
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(panelLook.panelHeaderHeight)
            .bottomHairline()
            // Raising on press rather than on a click: a player reaching THROUGH one panel to
            // another expects the one they touched to come forward, whether or not the press lands
            // on a control. The press is observed without consuming it, so the control underneath
            // still receives it.
            .pointerObservingPress { onRaise() }
            // The panel's own move band. It lives HERE, on the header row, and not as an overlay
            // above the whole panel: an overlay covering the panel would sit in the same z tier as
            // the panel's controls and eat the presses that make its sliders and toggles work.
            .panelDragBand(
                box, layout, title, stagePxW, stagePxH,
                onGeometryChange
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The title is centred: a panel is a surface without a parent column, so a left-aligned
        // title has nothing to align with. The two flanks are equal-width boxes so the title stays
        // centred whatever is in them.
        Box(Modifier.width(40.dp))
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = TypeScale.panelTitle,
                color = Star.textPrimary,
                fontFamily = auroraFont,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                subtitle,
                fontSize = TypeScale.hint,
                color = Star.textTertiary,
                fontFamily = auroraFont,
                maxLines = 1
            )
        }
        Row(
            Modifier.width(40.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIconButton(
                icon = AuroraIcon.Pin,
                contentDescription = if (box.pinned) "取消置顶" else "置顶",
                onClick = {
                    layout.setPinned(panelId, !box.pinned)
                    onGeometryChange()
                },
                tint = if (box.pinned) Star.textPrimary else Star.textTertiary,
                filled = box.pinned
            )
            Spacer(Modifier.width(2.dp))
            HeaderIconButton(
                icon = AuroraIcon.Close,
                contentDescription = "关闭",
                onClick = onClose
            )
        }
    }
}

/**
 * Drags a floating panel by its header.
 *
 * <p>UNITS. `stageWidthPx` / `stageHeightPx` are PIXELS, and so are the pointer positions this
 * reads. The caller converts the Dp stage once. An earlier revision passed the Dp values here
 * while measuring travel in pixels, which made a 1px drag move the panel by 1/density of the stage
 * -- about 2.2x too far at GUI scale 2 -- reported as "实际错位" (the panel overshooting the
 * pointer).
 *
 * <p>Travel is measured against the position captured at press, i.e. the header's own coordinate
 * system, which moves with the panel: that is why the grab-time origin is what the delta is taken
 * from.
 */
internal fun Modifier.panelDragBand(
    box: AuroraLayout.CardBox,
    layout: AuroraLayout,
    title: String,
    stageWidthPx: Float,
    stageHeightPx: Float,
    onGeometryChange: () -> Unit
): Modifier = this.then(
    Modifier
        .pointerInput(title, stageWidthPx, stageHeightPx) {
            awaitPointerEventScope {
                var lastTapTime = 0L
                while (true) {
                    var pressEvent = awaitPointerEvent()
                    while (pressEvent.type != PointerEventType.Press) {
                        pressEvent = awaitPointerEvent()
                    }
                    val down = pressEvent.changes.firstOrNull() ?: continue
                    // Only the primary button drags; the right button belongs to the panel's menu.
                    if (!pressEvent.buttons.isPrimaryPressed) continue
                    val downTime = System.nanoTime()
                    // SAME-EVENT BASELINE. This is the fourth shape, and the only one whose two
                    // coordinates come from the SAME layout generation.
                    //
                    // Every earlier attempt mixed generations, which is why each failed:
                    //   1. `c.position - down.position` -- `down` was captured under the layout
                    //      before the drag, `c` under the current one. Different baselines, so the
                    //      node's own movement was subtracted once too often: spring-back;
                    //   2. `origin + c.position` -- the origin came from onGloballyPositioned, a
                    //      LAYOUT BEHIND the pointer event that follows our own move: overshoot,
                    //      then correction: "四处乱跳";
                    //   3. `c.position - lastLocalX` -- `lastLocalX` was stored from the PREVIOUS
                    //      event, i.e. the previous layout. Same mismatch as (1).
                    //
                    // `position - previousPosition` is ONE change object: both fields are recorded
                    // by the same pointer event against the same layout, so their difference is
                    // exactly how far the pointer moved -- independent of where the node sits, how
                    // far it has been dragged, and whether a relayout has happened yet. It is the
                    // quantity Compose's own detectDragGestures accumulates.
                    var dragging = false
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull() ?: continue
                        if (e.type == PointerEventType.Release || !c.pressed) {
                            if (!dragging) {
                                lastTapTime = downTime
                            } else {
                                layout.save()
                                onGeometryChange()
                            }
                            c.consume()
                            break
                        }
                        if (e.type == PointerEventType.Move) {
                            val stepX = c.position.x - c.previousPosition.x
                            val stepY = c.position.y - c.previousPosition.y
                            if (!dragging && Math.hypot(stepX.toDouble(), stepY.toDouble()) > 6.0) {
                                dragging = true
                            }
                            if (dragging) {
                                box.x = (box.x + stepX / stageWidthPx).coerceIn(0f, 1f - box.w)
                                box.y = (box.y + stepY / stageHeightPx).coerceIn(0f, 1f - box.h)
                                box.clampBox()
                                onGeometryChange()
                                c.consume()
                            }
                        }
                    }
                }
            }
        }
)

/**
 * Observes a left press without claiming it, so a control underneath still receives the press.
 *
 * <p>The raise-on-press needs this because a `clickable` consumes the press: the earlier panel
 * header had one for raising, and its consumption is what made the panel's own sliders and the
 * header's buttons unreliable inside it.
 */
private fun Modifier.pointerObservingPress(onPress: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && !event.buttons.isSecondaryPressed) {
                    onPress()
                }
            }
        }
    }
)

/**
 * What the panel layer needs to know about the pointer and about an in-flight drag.
 *
 * <p>Compose state, not plain fields: the parallax offset is read inside `graphicsLayer`, and a
 * plain field would change without ever telling Compose to redraw.
 */
internal class AuroraPanelDrag {
    /** Title of the panel being dragged, or null. */
    var active: String? by mutableStateOf(null)

    /** Pointer offset from the centre, -1..1 on each axis. */
    var parallaxX: Float by mutableStateOf(0f)
    var parallaxY: Float by mutableStateOf(0f)
}

/**
 * Publishes the pointer's offset from the centre of this box, without consuming any event.
 *
 * <p>`awaitPointerEventScope` observes the pointer stream while still letting every event through to
 * the controls underneath -- a `pointerInput` that consumed motion would silently disable the sliders
 * and toggles it was measured over.
 */
internal fun Modifier.trackParallax(drag: AuroraPanelDrag): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                val p = change.position
                if (size.width > 0 && size.height > 0) {
                    drag.parallaxX = (p.x / size.width - 0.5f) * 2f
                    drag.parallaxY = (p.y / size.height - 0.5f) * 2f
                }
            }
        }
    }
)

/** The header icon size, re-exported so a caller need not import the icons package for it. */
internal val panelHeaderIcon = IconSize.panelHeader

/**
 * Hides the keyboard focus ring as soon as the pointer is used.
 *
 * <p>The other half of [FocusState.keyboardActive]: a key press turns the ring on, and any mouse
 * movement turns it back off. Without this the ring would persist after the player went back to the
 * mouse, which implies the keyboard is still in charge and makes the ring look like a stuck artefact.
 *
 * <p>Applied at the window root, so it is an ANCESTOR of every focusable row: the event travels up
 * through this modifier wherever it lands, and nothing is consumed -- a press must still reach the
 * slider or button under it. Only Move is watched: a press is a deliberate act on a control, while
 * a moving pointer is unambiguously the mouse taking over.
 */
internal fun Modifier.clearFocusOnPointer(focus: FocusState): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Move && focus.keyboardActive) {
                    focus.keyboardActive = false
                }
            }
        }
    }
)

/**
 * The smallest a panel may be sized, enforced by the frame's resize clamps (0.25f of the stage's
 * width, 0.2f of its height -- the frame's own constants, since panels and the main window share
 * it).
 */
internal val MIN_PANEL_W = 230f
internal val MIN_PANEL_H = 170f
