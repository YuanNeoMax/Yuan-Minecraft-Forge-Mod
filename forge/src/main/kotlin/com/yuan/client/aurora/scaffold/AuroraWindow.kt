package com.yuan.client.aurora.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.AuroraLayout
import com.yuan.client.aurora.LocalAuroraLayout
import com.yuan.client.aurora.LocalLayoutRevision
import com.yuan.client.aurora.theme.Star
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The one window-gesture frame every Aurora surface lives in.
 *
 * <p>This file replaces "arrange mode" as the way a window is moved and sized. The mode gated the
 * same gestures behind a persisted flag, and that shape failed twice: the grips existed only inside
 * the mode, so they were invisible and undiscoverable outside it; and a right-click menu that only
 * existed inside a mode is a door behind a door. Here every gesture is resident, and the mode is
 * gone entirely -- what remains of "arranging" is the per-element customization menu, opened by a
 * right-click on whatever is being customized.
 *
 * <p>Both the main window and each floating panel use this frame with the same rules:
 *
 * <p>**Move** -- drag the title bar. The drag band stops short of the title bar's right-hand
 * buttons (they stay clickable) and short of its left edge by the same reserve, so a drag never
 * starts on a control.
 *
 * <p>**Resize** -- eight resident grips: four corners (both axes) and four edge middles (one axis).
 * Resident rather than mode-gated because an invisible affordance is not an affordance: the grips
 * paint a hover band and the stage draws a resize cursor beside the pointer (see
 * [AuroraResizeCursor]), which is the feedback the OS cursor normally provides and this embedded
 * scene cannot.
 *
 * <p>**Maximize / restore** -- double-click the title bar, or its button. Maximized means: fills
 * the stage, square corners, grips hidden. The restore rectangle is saved on the way in.
 *
 * <p>**Edge snap** -- while dragging, holding the window over a screen edge shows a preview of
 * where it will land (left/right half, a quarter at each corner, full at the top); releasing
 * commits it. Switchable off in settings, because a player who wants a freely floating window
 * experiences snapping as the window fighting them.
 *
 * <p>Geometry is stored as fractions of the stage in [AuroraLayout], so everything here converts
 * pointer pixels to fractions and back through the stage's measured size only.
 */

/** Which dimension an edge grip takes from its side and which from the band. */
internal enum class GripKind { Corner, EdgeV, EdgeH }

/**
 * The hit + visual width of an edge grip, in dp.
 *
 * <p>10dp rather than 7: a band narrower than the platform's resize border is a band nobody hits
 * on the first try, and a window that takes two attempts to resize reads as a broken window. The
 * visible affordance stays thin -- the hover plate and the cursor arrow carry the discovery.
 */
internal val GripThickness: Dp = 10.dp

/** The corner grips are square hit areas of this side, in dp. */
internal val GripCorner: Dp = 15.dp

/** How close to a screen edge (fraction of stage width/height) the snap zones sit, while dragging. */
private const val SNAP_ZONE = 0.06f

/** Where a dragged window will land, or null while it is not over a snap zone. */
internal enum class SnapSlot(val label: String) {
    Left("左半屏"), Right("右半屏"),
    TopLeft("左上 1/4"), TopRight("右上 1/4"),
    BottomLeft("左下 1/4"), BottomRight("右下 1/4"),
    Maximize("最大化");

    /** The fraction box this slot lands the window in. */
    fun box(): AuroraLayout.CardBox = when (this) {
        Left -> AuroraLayout.CardBox(0f, 0f, 0.5f, 1f, 0)
        Right -> AuroraLayout.CardBox(0.5f, 0f, 0.5f, 1f, 0)
        TopLeft -> AuroraLayout.CardBox(0f, 0f, 0.5f, 0.5f, 0)
        TopRight -> AuroraLayout.CardBox(0.5f, 0f, 0.5f, 0.5f, 0)
        BottomLeft -> AuroraLayout.CardBox(0f, 0.5f, 0.5f, 0.5f, 0)
        BottomRight -> AuroraLayout.CardBox(0.5f, 0.5f, 0.5f, 0.5f, 0)
        Maximize -> AuroraLayout.CardBox(0f, 0f, 1f, 1f, 0)
    }
}

/**
 * Which resize cursor a grip needs, drawn by [AuroraResizeCursor] beside the pointer.
 *
 * <p>The OS cannot provide one: GLFW 3.3 has no resize cursors, and the scene's pointer-icon
 * callback is not wired in this embedded host. Drawing the arrow beside the pointer is therefore
 * not decoration but the affordance itself -- without it a 7dp invisible strip is undiscoverable,
 * which is exactly how the mode-gated grips read before.
 */
internal enum class ResizeCursorDir { NWSE, NESW, EW, NS }

/**
 * Shared cursor state for the stage: which resize arrow (if any) floats beside the pointer, and
 * where the pointer is.
 *
 * <p>Compose state, not plain fields: the cursor overlay reads it in draw phases and must
 * re-render as the pointer crosses a grip; a plain field would move nothing on screen.
 */
internal class ResizeCursorState {
    var dir: ResizeCursorDir? by mutableStateOf(null)
    var x: Float by mutableStateOf(0f)
    var y: Float by mutableStateOf(0f)
}

/**
 * The resize arrow beside the pointer while a grip is hovered.
 *
 * <p>NO PLATE, NO WINDOW EDGE TINT. The previous glyph was a dark rounded plate with a heavy
 * white arrow on it: at 18dp it read as a sticker dropped next to the cursor, and the plate had to
 * fight the window's own edges. A deskop resize cursor is a thin double-headed arrow and nothing
 * more, so this is that -- a hairline shaft with two barbs, drawn twice (a dark halo under a pale
 * stroke) so it stays legible over the near-black shell, a bright slider fill and the game world
 * alike, with no background shape at all.
 *
 * <p>The window's edges are deliberately left un-tinted: a line along an edge cannot follow the
 * shell's rounded corners, so it always read as a rectangle drawn over a rounded window. The
 * arrow is the whole affordance, which is exactly how every desktop does it.
 */
@Composable
internal fun BoxScope.AuroraResizeCursor(state: ResizeCursorState) {
    val dir = state.dir
    if (dir != null) {
        // Sits WITH the pointer: `state.x/y` are root-space (both writers agree), the overlay is a
        // stage-level child, and the only offset is a 2dp gutter that keeps the glyph off the
        // cursor hotspot itself.
        val gutterPx = with(LocalDensity.current) { 2.dp.toPx() }
        Box(
            Modifier
                // Anchored at the stage origin, then translated by the pointer: the layer's
                // translation is measured from the box's own laid-out position, so the box must
                // start at (0,0) for the translation to be the pointer's coordinates.
                .align(Alignment.TopStart)
                // POSITION BY LAYER, NOT BY OFFSET.
                //
                // `offset(...)` is a layout modifier: reading the pointer position there means the
                // pointer's every movement recomposes this overlay. The position is a pure
                // translation, so it belongs in a graphics layer, whose lambda runs in the draw
                // phase -- the arrow still tracks the cursor at full frame rate without the
                // composable being re-executed for each step.
                .graphicsLayer {
                    translationX = state.x + gutterPx
                    translationY = state.y + gutterPx
                }
                .size(16.dp)
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawResizeArrow(dir, Star.textPrimary)
            }
        }
    }
}

/**
 * Publishes the pointer's STAGE position without claiming anything.
 *
 * <p>Applied to the stage ROOT (an ancestor of every window), so the observer rides the hit
 * path rather than competing for it: wherever a control is hit, the event still travels through
 * this modifier on its way up, and wherever nothing is hit, the root itself is the target. Either
 * way the position is seen and nothing is consumed.
 *
 * <p>TWO WRITERS, ONE SPACE. The grips write `nodeOrigin + local` into the same fields; a node's
 * root-space position is its parent's root-space position plus its own local position, so adding
 * this node's own offset makes both writers agree exactly. Without that, the arrow jumped between
 * two coordinate systems every frame.
 */
internal fun Modifier.trackCursor(state: ResizeCursorState): Modifier = this.then(
    Modifier
        .onGloballyPositioned { coordinates ->
            rootOriginX = coordinates.positionInRoot().x
            rootOriginY = coordinates.positionInRoot().y
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: continue
                    if (event.type == PointerEventType.Move) {
                        state.x = rootOriginX + change.position.x
                        state.y = rootOriginY + change.position.y
                    }
                }
            }
        }
)

// The stage root's own root-space origin, published for `trackCursor`. Module-level because the
// modifier chain is not a composable and cannot hold state of its own.
private var rootOriginX = 0f
private var rootOriginY = 0f

/** Draws one resize arrow glyph: a shaft with an arrowhead at each end, rotated by direction. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawResizeArrow(
    dir: ResizeCursorDir,
    color: Color
) {
    val unit = size.minDimension / 16f
    val rot = when (dir) {
        ResizeCursorDir.NWSE -> 0f
        ResizeCursorDir.NESW -> 90f
        // A vertical edge resizes LEFT/RIGHT, so its glyph is the horizontal arrow (—) and its
        // rotation is the one that turns the base `\` diagonal into a line: in screen space (y
        // down) +45deg maps (1,1) to (0,+), i.e. VERTICAL, and -45deg maps it to HORIZONTAL.
        // The two were swapped, which is why the top edge showed <-> and a side showed an up/down
        // arrow -- exactly backwards from what the edge does.
        ResizeCursorDir.EW -> -45f
        ResizeCursorDir.NS -> 45f
    }
    val cx = size.width / 2f
    val cy = size.height / 2f
    val pivot = Offset(cx, cy)
    rotate(rot, pivot = pivot) {
        // The arrow runs along the diagonal of the rotated frame: one line through the centre with
        // a barb at each end and a small gap at the waist, which is the shape every platform uses.
        // No plate behind it -- see the note above the composable.
        val reach = size.minDimension * 0.36f
        val waist = size.minDimension * 0.06f
        val barb = size.minDimension * 0.19f
        val shaft = 1.4f * unit
        val halo = 3.0f * unit

        // The halo first, drawn as a wider stroke in near-black so the pale arrow stays legible
        // over a light slider fill or a bright block of the world behind the glass.
        drawArrowShaft(Color(0xCC050507), reach, waist, shaft + halo)
        drawArrowShaft(color, reach, waist, shaft)
        drawArrowBarbs(Color(0xCC050507), reach, barb, shaft + halo)
        drawArrowBarbs(color, reach, barb, shaft)
    }
}

/** The waisted shaft: two strokes leaving a gap at the centre, so the arrow reads as a pair. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowShaft(
    color: Color,
    reach: Float,
    waist: Float,
    width: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    drawLine(color, Offset(cx - reach, cy - reach), Offset(cx - waist, cy - waist), width)
    drawLine(color, Offset(cx + waist, cy + waist), Offset(cx + reach, cy + reach), width)
}

/** Two barbs per tip, angled back from it along both axes. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowBarbs(
    color: Color,
    reach: Float,
    barb: Float,
    width: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    // Near tip.
    val nx = cx - reach
    val ny = cy - reach
    drawLine(color, Offset(nx, ny), Offset(nx + barb, ny), width)
    drawLine(color, Offset(nx, ny), Offset(nx, ny + barb), width)
    // Far tip.
    val fx = cx + reach
    val fy = cy + reach
    drawLine(color, Offset(fx, fy), Offset(fx - barb, fy), width)
    drawLine(color, Offset(fx, fy), Offset(fx, fy - barb), width)
}

/**
 * One window's gesture frame.
 *
 * <p>Wraps [content] in the geometry read from `box` (a live [AuroraLayout.CardBox] in stage
 * fractions) and arms the gestures on its behalf. `headerHeight` and the button reserves describe
 * where the title bar's draggable band may run. Callers render their own title bar INSIDE
 * [content]; this frame only owns the pointer mechanics, so the chrome stays each surface's own.
 *
 * @param box the live box the window reads and the gestures write.
 * @param stage the measured stage size in pixels, the one coordinate every fraction resolves
 *   against.
 * @param maximized whether this window is currently maximized; maximized windows fill the stage
 *   and hide their grips.
 * @param onMaximizeToggle double-click on the title bar band or the header's button.
 * @param onSnap while dragging, the slot under the pointer or null; the caller decides whether
 *   snapping is enabled and what to do on release.
 * @param onOpenMenu right-click anywhere on the title bar band.
 */
@Composable
internal fun androidx.compose.foundation.layout.BoxScope.AuroraWindowFrame(
    box: AuroraLayout.CardBox,
    stage: IntSize,
    headerHeight: Dp,
    buttonReserve: Dp,
    layout: AuroraLayout,
    cursor: ResizeCursorState,
    maximized: Boolean = false,
    /**
     * Whether the frame draws its own title-bar move band.
     *
     * <p>Off for a floating panel: the panel owns its drag on its header Row, so the overlay band
     * would be a second drag surface in the same z tier -- and an overlay above the panel is also
     * above its top rows, where presses belong to the panel's own controls.
     */
    moveBand: Boolean = true,
    onMoved: () -> Unit,
    onOpenMenu: () -> Unit,
    onMaximizeToggle: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current

    // THE SUBSCRIPTION THAT MAKES EVERY GESTURE VISIBLE.
    //
    // `box` is a plain Java object. Dragging and resizing write `box.x/y/w/h`, which changes no
    // Compose state at all, and every parameter of this composable (the same `box` reference, the
    // same `stage`, `layout`, `cursor`) stays equal -- so Compose treats the call as unchanged and
    // skips it. The values move, the picture does not: that is exactly what "拖动没用 / 缩放没用"
    // was. Reading the revision here subscribes this frame, and the writer (`onMoved` ->
    // `touchLayout`) therefore forces the re-read on every delta.
    //
    // The read is UNCONDITIONAL and sits before any early return: the first drag is the one that
    // creates the box, and a read placed after the null-size guard would subscribe nothing on the
    // frames that matter most.
    @Suppress("UNUSED_EXPRESSION")
    LocalLayoutRevision.current

    val screenW = stage.width.toFloat()
    val screenH = stage.height.toFloat()
    if (screenW <= 0f || screenH <= 0f) return

    val gestures = layout.gesturesEnabled
    val headerPx = with(density) { headerHeight.toPx() }
    val reservePx = with(density) { buttonReserve.toPx() }

    // The pointer's stage position during a title-bar drag, kept so the snap zone can be tested
    // without re-deriving it from the box (which has already moved).
    var snapHint by remember { mutableStateOf<SnapSlot?>(null) }

    Box(
        Modifier
            .align(Alignment.TopStart)
            .offset(
                with(density) { (box.x * screenW).toDp() },
                with(density) { (box.y * screenH).toDp() }
            )
            .size(
                with(density) { (box.w * screenW).toDp() },
                with(density) { (box.h * screenH).toDp() }
            )
    ) {
        content()

        // ---- the move band over the title bar's free area -------------------------------
        // Left button drag moves; double-click maximizes/restores; right-click opens the window's
        // own customization menu. The band stops short of BOTH ends' controls (the module mark on
        // the left, the buttons on the right) so it never covers a clickable thing.
        //
        // ONE pointerInput, ONE hand-rolled detector. The first version chained three -- right-click
        // observer, detectTapGestures for the double-click, detectDragGestures for the move -- and
        // that is the "first one consumes, the rest starve" trap this project has hit before:
        // detectTapGestures consumes the down while deciding whether it is a tap, and the drag
        // detector's slop awaiter treats a consumed down as "someone else owns this pointer" and
        // cancels. Diagnosed by round-3 probes: every Press arrived consumed=true, the drag
        // detector never fired. A single awaitEachGesture loop routes by button and distance
        // instead, so no detector can eat another's events.
        // The band stays composed even while maximized: a drag on a maximized window must restore
        // it and keep dragging (desktop semantics). Gating it on `!maximized` removed the only
        // gesture that could un-maximize by dragging, so a persisted maximized flag trapped the
        // window -- reported as "无法移动" with the window looking fullscreen.
        if (gestures && moveBand) {
            // ABSOLUTE positioning, not incremental deltas. The band lives INSIDE the window's box,
            // so its local coordinate origin moves with every applied frame; pointer events can
            // arrive faster than layout refresh, and a delta computed against a stale origin
            // double-counts the window's own movement -- the window chases the pointer with a
            // rubber-band lag (reported as "拖不动"), and the grips fail the same way ("缩放一样
            // 问题"). Instead the band's ROOT-space origin is tracked (same layout generation as
            // the event coordinates, so the sum is exact), the grab offset is captured at down,
            // and every move OVERWRITES the position absolutely: no accumulation, self-correcting.
            var bandOriginX by remember { mutableStateOf(0f) }
            var bandOriginY by remember { mutableStateOf(0f) }
            var dragPointerX = 0f
            var dragPointerY = 0f
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(headerHeight)
                    // The padding comes BEFORE the pointerInput so it shrinks the HIT AREA, not
                    // just the content: with the order reversed, the band's pointer region covered
                    // the whole header including the maximize/save/close buttons -- every click on
                    // a button landed on the band instead, registered as a tap, and two quick
                    // "why didn't that work" clicks fired the double-tap maximize.
                    .padding(start = 48.dp, end = buttonReserve, top = 0.dp, bottom = 0.dp)
                    .onGloballyPositioned { coordinates ->
                        val p = coordinates.positionInRoot()
                        bandOriginX = p.x
                        bandOriginY = p.y
                    }
                    .pointerInput(screenW, screenH) {
                        awaitPointerEventScope {
                            // Double-click window, in nanoseconds; zero means "no recent tap".
                            var lastTapTime = 0L
                            while (true) {
                                var pressEvent = awaitPointerEvent()
                                while (pressEvent.type != PointerEventType.Press) {
                                    pressEvent = awaitPointerEvent()
                                }
                                val down = pressEvent.changes.firstOrNull() ?: continue
                                if (pressEvent.buttons.isSecondaryPressed) {
                                    // Right press: the window's own menu.
                                    down.consume()
                                    onOpenMenu()
                                    while (true) {
                                        val e = awaitPointerEvent()
                                        val c = e.changes.firstOrNull() ?: continue
                                        if (e.type == PointerEventType.Release) { c.consume(); break }
                                        if (e.type == PointerEventType.Move) c.consume()
                                    }
                                    continue
                                }
                                if (!pressEvent.buttons.isPrimaryPressed) continue

                                // Left press: tap, double-tap, or drag.
                                val downTime = System.nanoTime()
                                val downRootX = bandOriginX + down.position.x
                                val downRootY = bandOriginY + down.position.y
                                // A press on a maximized window restores it and keeps dragging:
                                // the drag IS the restore gesture, the way every desktop behaves.
                                // The window lands under the pointer proportionally, so the grab
                                // point stays under the cursor across the size change.
                                if (layout.windowMaximized) {
                                    restoreUnderPointer(layout, box, downRootX, downRootY, screenW, screenH)
                                    onMoved()
                                }
                                // PURS ROOT-SPACE TRAVEL, and the box captured at grab.
                                //
                                // The previous form was `grabOff = downRoot - box.x * screenW`,
                                // which subtracts a STAGE-space pixel from a ROOT-space pixel. That
                                // is only valid when the frame sits at the stage origin -- true for
                                // the main window, false for every floating panel, which sits
                                // inside the content area after the rail and the content header.
                                // For a panel the offset was off by the whole rail+header, so a
                                // drag landed the panel somewhere else and a resize computed from
                                // a wrong origin: reported as "浮层只能打开和关闭".
                                //
                                // Taking the travel as (rootNow - rootAtGrab) instead is
                                // independent of where the frame's parent puts it, so the same code
                                // is correct for the stage-level window and for a nested panel.
                                var dragging = false
                                while (true) {
                                    val e = awaitPointerEvent()
                                    val c = e.changes.firstOrNull() ?: continue
                                    if (e.type == PointerEventType.Release || !c.pressed) {
                                        if (!dragging) {
                                            if (downTime - lastTapTime < 400_000_000L) {
                                                onMaximizeToggle?.invoke()
                                                lastTapTime = 0L
                                            } else {
                                                lastTapTime = downTime
                                            }
                                        } else {
                                            onMoved()
                                            onSnapCommit(snapHint, box, layout, screenW, screenH)
                                        }
                                        snapHint = null
                                        c.consume()
                                        break
                                    }
                                    if (e.type == PointerEventType.Move) {
                                        // SAME-EVENT BASELINE, for the same reason as the panel
                                        // band: `position - previousPosition` are two fields of ONE
                                        // change, recorded against ONE layout, so the difference is
                                        // the pointer's real step regardless of where the node sits
                                        // or whether our own move has been laid out yet. The root
                                        // origin from onGloballyPositioned lags a layout behind and
                                        // made the window overshoot and correct ("四处乱跳").
                                        val stepX = c.position.x - c.previousPosition.x
                                        val stepY = c.position.y - c.previousPosition.y
                                        val rootX = bandOriginX + c.position.x
                                        val rootY = bandOriginY + c.position.y
                                        if (!dragging &&
                                            Math.hypot(
                                                (rootX - downRootX).toDouble(),
                                                (rootY - downRootY).toDouble()
                                            ) > 8.0
                                        ) {
                                            dragging = true
                                        }
                                        if (dragging) {
                                            dragPointerX = rootX
                                            dragPointerY = rootY
                                            box.x = (box.x + stepX / screenW).coerceIn(0f, 1f - box.w)
                                            box.y = (box.y + stepY / screenH).coerceIn(0f, 1f - box.h)
                                            box.clampBox()
                                            onMoved()
                                            snapHint = if (layout.aeroSnapEnabled) snapSlotAt(dragPointerX, dragPointerY, screenW, screenH) else null
                                            c.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // ---- eight resident resize grips ------------------------------------------------
        if (gestures && !maximized) {
            ResizeGripSet(box, layout, cursor, stage, onMoved)
        }
    }

    // ---- the snap preview, drawn at stage level over every window --------------------------
    snapHint?.let { hint ->
        val target = hint.box()
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(
                    with(density) { (target.x * screenW).toDp() },
                    with(density) { (target.y * screenH).toDp() }
                )
                .size(
                    with(density) { (target.w * screenW).toDp() },
                    with(density) { (target.h * screenH).toDp() }
                )
                .background(Color(0x264A90E2), RoundedCornerShape(6.dp))
                .drawBehind {
                    drawRoundRect(
                        Star.accent.copy(alpha = 0.6f),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        style = Stroke(1.5f)
                    )
                }
        )
    }
}

/**
 * Commits a snap on release: the window's box takes the slot's rectangle.
 *
 * <p>Restore rectangles are NOT touched here -- snapping is a move, not a maximize, and a window
 * that was dragged into a half and then dragged out again was never asked to remember anything.
 */
private fun onSnapCommit(
    hint: SnapSlot?,
    box: AuroraLayout.CardBox,
    layout: AuroraLayout,
    screenW: Float,
    screenH: Float
) {
    if (hint == null) return
    if (hint == SnapSlot.Maximize) {
        maximize(layout, box)
        return
    }
    val target = hint.box()
    box.x = target.x
    box.y = target.y
    box.w = target.w
    box.h = target.h
    box.clampBox()
}

/**
 * Fills the stage and remembers what to give back.
 *
 * <p>ONE implementation for every maximize path -- the header button, the double-click, and the
 * snap to the top edge. Three copies of the same bookkeeping were three chances to drift.
 */
internal fun maximize(layout: AuroraLayout, box: AuroraLayout.CardBox) {
    layout.windowRestore = box.copy()
    layout.windowMaximized = true
    box.x = 0f; box.y = 0f; box.w = 1f; box.h = 1f
}

/**
 * Hands the saved rectangle back, or a centred default when there is nothing saved.
 */
internal fun restoreWindow(layout: AuroraLayout, box: AuroraLayout.CardBox) {
    val restore = layout.windowRestore
    if (restore != null) {
        box.x = restore.x; box.y = restore.y
        box.w = restore.w; box.h = restore.h
    } else {
        box.x = 0.1f; box.y = 0.1f; box.w = 0.8f; box.h = 0.8f
    }
    layout.windowMaximized = false
    box.clampBox()
}

/**
 * The maximize/restore toggle shared by the header button and the title-bar double-click.
 */
internal fun toggleMaximize(layout: AuroraLayout, box: AuroraLayout.CardBox, onMoved: () -> Unit) {
    if (layout.windowMaximized) {
        restoreWindow(layout, box)
    } else {
        maximize(layout, box)
    }
    layout.save()
    onMoved()
}

/**
 * Un-maximizes under the pointer and returns to normal dragging.
 *
 * <p>Places the window so the pointer keeps roughly the same relative position along its title
 * bar, which is what makes "drag a maximized window" feel like one continuous gesture instead of
 * a jump. Only meaningful while [AuroraLayout.windowMaximized] is set.
 */
internal fun restoreUnderPointer(
    layout: AuroraLayout,
    box: AuroraLayout.CardBox,
    pointerX: Float,
    pointerY: Float,
    stageW: Float,
    stageH: Float
) {
    val restore = layout.windowRestore
    val w = restore?.w ?: 0.8f
    val h = restore?.h ?: 0.8f
    val relX = (pointerX / stageW).coerceIn(0f, 1f)
    box.w = w
    box.h = h
    box.x = (relX * (1f - w)).coerceIn(0f, 1f - w)
    box.y = 0f
    layout.windowMaximized = false
    box.clampBox()
}

/** Which snap zone a stage-space pointer sits in, or null when it is in open space. */
internal fun snapSlotAt(x: Float, y: Float, w: Float, h: Float): SnapSlot? {
    val nearLeft = x < w * SNAP_ZONE
    val nearRight = x > w * (1f - SNAP_ZONE)
    val nearTop = y < h * SNAP_ZONE
    val nearBottom = y > h * (1f - SNAP_ZONE)
    val cornerX = w * 0.18f
    val cornerY = h * 0.18f
    return when {
        nearTop && x < cornerX -> SnapSlot.TopLeft
        nearTop && x > w - cornerX -> SnapSlot.TopRight
        nearBottom && x < cornerX -> SnapSlot.BottomLeft
        nearBottom && x > w - cornerX -> SnapSlot.BottomRight
        nearTop -> SnapSlot.Maximize
        nearLeft -> SnapSlot.Left
        nearRight -> SnapSlot.Right
        else -> null
    }
}

/**
 * Snaps a fraction to the grid, or leaves it when the grid is off.
 *
 * <p>Used by the snap COMMIT path only, never per drag frame: snapping each incremental delta
 * rounds a sub-cell move back to where it started and a gentle drag produces zero net movement --
 * the "无法移动" this batch was reported for.
 */
private fun snap(value: Float, step: Float): Float =
    if (step <= 0f) value else Math.round(value / step) * step

/**
 * The eight resident grips, positioned against the window's own edges.
 *
 * <p>Each grip: hit area (corner 15dp square, edge 7dp band), hover band drawn on the window's
 * border, pointer-cursor direction published to the shared [ResizeCursorState] on enter/exit, and
 * the resize drag itself. Corners take both axes; edge middles span 40% of their side so a corner
 * grip keeps its own area.
 */
@Composable
private fun BoxScope.ResizeGripSet(
    box: AuroraLayout.CardBox,
    layout: AuroraLayout,
    cursor: ResizeCursorState,
    stage: IntSize,
    onMoved: () -> Unit
) {
    val density = LocalDensity.current
    val screenW = stage.width.toFloat()
    val screenH = stage.height.toFloat()

    data class Grip(
        val align: Alignment,
        val kind: GripKind,
        val sx: Float,
        val sy: Float,
        val dir: ResizeCursorDir
    )

    // ORDER MATTERS: in a Box, later children are hit first. The edges therefore come first and
    // the corners LAST, so a corner wins in the overlap and the edges can span the whole side --
    // with the old 40%-length edges, the top and bottom thirds of each side were dead zones that
    // no grip answered to.
    val grips = listOf(
        // Vertical sides (left/right): full height, a thin band.
        Grip(Alignment.CenterStart, GripKind.EdgeV, -1f, 0f, ResizeCursorDir.EW),
        Grip(Alignment.CenterEnd, GripKind.EdgeV, 1f, 0f, ResizeCursorDir.EW),
        // Horizontal sides (top/bottom): full width, a thin band.
        Grip(Alignment.TopCenter, GripKind.EdgeH, 0f, -1f, ResizeCursorDir.NS),
        Grip(Alignment.BottomCenter, GripKind.EdgeH, 0f, 1f, ResizeCursorDir.NS),
        // Corners last: they sit above the edge bands wherever the two overlap.
        Grip(Alignment.TopStart, GripKind.Corner, -1f, -1f, ResizeCursorDir.NWSE),
        Grip(Alignment.TopEnd, GripKind.Corner, 1f, -1f, ResizeCursorDir.NESW),
        Grip(Alignment.BottomStart, GripKind.Corner, -1f, 1f, ResizeCursorDir.NESW),
        Grip(Alignment.BottomEnd, GripKind.Corner, 1f, 1f, ResizeCursorDir.NWSE)
    )

    grips.forEach { grip ->
        // ABSOLUTE sizing, same reasoning as the move band: the grip's local origin moves with
        // every applied frame, so incremental deltas double-count. The root-space origin is
        // tracked, the box is captured at down, and every move recomputes the edge ABSOLUTELY
        // from the pointer's root position.
        var gripOriginX by remember { mutableStateOf(0f) }
        var gripOriginY by remember { mutableStateOf(0f) }
        // SIZING, and this was the reason only the four corners ever worked: an edge grip was
        // built with `size(GripThickness, 0.dp)` and then `fillMaxHeight(0.4f)` -- filling a
        // 0-height constraint leaves 0, so left/right/top/bottom had a 0x0 hit box and no pointer
        // ever reached them. Each edge now takes ONE dimension from the band thickness and lets
        // the other FILL its side; corners stay square.
        val sizeMod = when (grip.kind) {
            GripKind.Corner -> Modifier.size(GripCorner)
            GripKind.EdgeV -> Modifier.width(GripThickness).fillMaxHeight()
            GripKind.EdgeH -> Modifier.height(GripThickness).fillMaxWidth()
        }
        Box(
            Modifier
                .align(grip.align)
                .then(sizeMod)
                .onGloballyPositioned { coordinates ->
                    val p = coordinates.positionInRoot()
                    gripOriginX = p.x
                    gripOriginY = p.y
                }
                // ONE pointerInput for hover-tracking and the resize drag alike. Two chained ones
                // are the consume-order trap: whichever runs first owns the event stream, and the
                // drag detector behind it sees consumed downs and never starts.
                .pointerInput(grip.sx, grip.sy, screenW, screenH) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            // HOVER IS DRIVEN BY ENTER/EXIT, not by a coordinate test.
                            //
                            // HOVER IS DRIVEN BY ENTER/EXIT, not by a coordinate test.
                            //
                            // The previous test was `position.x >= 0 && position.y >= 0`: a local
                            // position inside the node is ALWAYS non-negative, so it read "inside"
                            // for every event and the grip never turned off -- and with eight grips
                            // that painted a blue frame around the whole window. Enter/Exit are the
                            // events that actually say where the pointer is relative to the node.
                            when (event.type) {
                                PointerEventType.Enter -> {
                                    cursor.dir = grip.dir
                                }
                                PointerEventType.Exit -> {
                                    if (cursor.dir == grip.dir) cursor.dir = null
                                }
                                PointerEventType.Press -> {
                                    val pressed = event.changes.firstOrNull() ?: continue
                                    if (!pressed.pressed) continue
                                    pressed.consume()
                                    // Snapshot the box at grab; every move recomputes ABSOLUTELY from
                                    // the pointer's root position (incremental deltas would
                                    // double-count, because the grip's own origin moves with each
                                    // frame the resize applies).
                                    val downRootX = gripOriginX + pressed.position.x
                                    val downRootY = gripOriginY + pressed.position.y
                                    while (true) {
                                        val e = awaitPointerEvent()
                                        val c = e.changes.firstOrNull() ?: continue
                                        if (e.type == PointerEventType.Release || !c.pressed) {
                                            c.consume()
                                            layout.save()
                                            onMoved()
                                            break
                                        }
                                        if (e.type == PointerEventType.Move) {
                                            // SAME-EVENT BASELINE, as in the move band: two fields
                                            // of one change, one layout, so the difference is the
                                            // pointer's real step. `gripOrigin + position` read a
                                            // layout-callback origin that lags our own resize by a
                                            // frame, which made the edge overshoot and snap back.
                                            val stepX = c.position.x - c.previousPosition.x
                                            val stepY = c.position.y - c.previousPosition.y
                                            val rootX = gripOriginX + c.position.x
                                            val rootY = gripOriginY + c.position.y
                                            // The cursor sits AT the pointer: root position, no
                                            // re-offset. See the note on the overlay.
                                            cursor.x = rootX
                                            cursor.y = rootY
                                            // Raw travel only: the branches below own the direction.
                                            // Multiplying by grip.sx/sy as well inverted the
                                            // left/top edges.
                                            val fx = stepX / screenW
                                            val fy = stepY / screenH
                                            if (grip.sx < 0f) {
                                                // The opposite edge stays put, so moving this edge
                                                // left must grow the width by the same amount.
                                                val nextX = (box.x + fx).coerceIn(0f, 1f)
                                                val applied = nextX - box.x
                                                box.x = nextX
                                                box.w = (box.w - applied).coerceAtLeast(0.25f)
                                            } else if (grip.sx > 0f) {
                                                box.w = (box.w + fx).coerceIn(0.25f, 1f - box.x)
                                            }
                                            if (grip.sy < 0f) {
                                                val nextY = (box.y + fy).coerceIn(0f, 1f)
                                                val applied = nextY - box.y
                                                box.y = nextY
                                                box.h = (box.h - applied).coerceAtLeast(0.2f)
                                            } else if (grip.sy > 0f) {
                                                box.h = (box.h + fy).coerceIn(0.2f, 1f - box.y)
                                            }
                                            box.clampBox()
                                            c.consume()
                                            onMoved()
                                        }
                                    }
                                    // The pointer usually leaves the grip mid-drag and that Exit is
                                    // consumed by the drag loop; clear here so the arrow cannot
                                    // outlive the gesture.
                                    if (cursor.dir == grip.dir) cursor.dir = null
                                }
                                else -> {}
                            }
                        }
                    }
                }
                .drawBehind {
                    // NO EDGE TINT AT ALL, on purpose.
                    //
                    // Two revisions painted the window's edge on hover: first a filled plate (a
                    // blue frame around the whole window), then a 1.5dp line (a blue line around
                    // the whole window). Both read as "the window is selected", and the line
                    // cannot follow the shell's rounded corners, so it always looked like a
                    // rectangle drawn over a rounded window.
                    //
                    // The resize arrow beside the pointer is the affordance, which is exactly how
                    // every desktop communicates a resize and what the user asked for when the line
                    // was called ugly. Nothing is drawn here -- the hook is kept so the intent is
                    // recorded where the next reader will look for the tint that used to be here.
                }
        )
    }
}

/**
 * Applies one resize delta to a box, moving the dragged edge and holding the opposite one.
 *
 * <p>Minima are enforced here rather than by the layout's clamp: a box narrower than its header
 * is a window whose close button has nowhere to be.
 */
internal fun resizeBox(
    box: AuroraLayout.CardBox,
    dx: Float,
    dy: Float,
    sx: Float,
    sy: Float
) {
    if (sx < 0f) {
        val nextX = (box.x + dx).coerceIn(-0.2f, 1f)
        val applied = nextX - box.x
        box.x = nextX
        box.w = (box.w - applied).coerceAtLeast(0.25f)
    } else if (sx > 0f) {
        box.w = (box.w + dx).coerceIn(0.25f, 1f - box.x)
    }
    if (sy < 0f) {
        val nextY = (box.y + dy).coerceIn(-0.2f, 1f)
        val applied = nextY - box.y
        box.y = nextY
        box.h = (box.h - applied).coerceAtLeast(0.2f)
    } else if (sy > 0f) {
        box.h = (box.h + dy).coerceIn(0.2f, 1f - box.y)
    }
    box.clampBox()
}
