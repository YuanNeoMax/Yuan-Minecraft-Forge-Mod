package com.yuan.client.aurora.panels

import androidx.compose.foundation.background
import com.yuan.client.aurora.icons.IconSize
import com.yuan.client.aurora.scaffold.handCursor
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.AuroraLayout
import com.yuan.client.aurora.scaffold.panelDragBand
import com.yuan.client.aurora.scaffold.ResizeCursorState
import com.yuan.client.aurora.scaffold.AuroraWindowFrame
import androidx.compose.ui.unit.IntSize
import com.yuan.client.aurora.LocalLayoutRevision
import com.yuan.client.aurora.AuroraLook
import com.yuan.client.aurora.anim.Motion
import com.yuan.client.aurora.scaffold.AuroraButton
import com.yuan.client.aurora.icons.AuroraIcon
import com.yuan.client.aurora.scaffold.AuroraRow
import com.yuan.client.aurora.scaffold.AuroraSelector
import com.yuan.client.aurora.scaffold.AuroraSlider
import com.yuan.client.aurora.scaffold.AuroraToggle
import com.yuan.client.aurora.scaffold.auroraScroll
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale
import com.yuan.client.aurora.theme.auroraFont
import java.util.Locale

/**
 * What the arrangement menu is editing.
 *
 * <p>An element rather than a field: a player points at a thing and asks to change it, so the menu's
 * job is to know which set of numbers belongs to the thing they pointed at. The rail is the one element
 * with a menu today; the enum exists so the next one does not invent a second mechanism.
 */
internal enum class ArrangeTarget(val label: String) {
    /** The whole left column: width, offsets, and the values every entry inherits. */
    Rail("左边栏"),
    /** One rail entry (a module): overrides that apply to that entry alone. */
    Entry("条目"),
    /** One rail category: overrides shared by the entries inside it. */
    Section("分类"),
    Panel("面板"),
    Shell("窗口"),
    Controls("控件")
}

/**
 * The arrangement menu: every adjustable number for one element, over a scrim.
 *
 * <p>Opened by a right-click in arrange mode, which is where the reference puts its own context menu and
 * is the reason the mode exists at all -- these values are reachable without leaving the window and
 * without a settings file.
 *
 * <p>Grouped by what each number dresses rather than listed flat. Thirty-four sliders in one column is a
 * scroll with no landmarks; four short groups let a player who wants "the corner radius" find the group
 * whose name says corners.
 *
 * <p>The scrim closes on a press the panel did not claim. See the comment on the consumption check below
 * for why it is decided that way rather than by comparing coordinates.
 */
@Composable
internal fun ArrangeMenu(
    target: ArrangeTarget,
    layout: AuroraLayout,
    look: AuroraLook,
    /** The menu's own box in stage fractions -- it is a window like any other. */
    box: AuroraLayout.CardBox,
    /** The measured stage, in pixels. */
    stage: IntSize,
    /** Shared resize-cursor state: the menu shows the same arrows as the main window. */
    cursor: ResizeCursorState,
    sectionId: String? = null,
    /** The module an [ArrangeTarget.Entry] menu is editing. Null for every other scope. */
    moduleId: String? = null,
    onChanged: () -> Unit = {},
    onClose: () -> Unit
) {
    // THE SUBSCRIPTION THAT MAKES EVERY EDIT VISIBLE.
    //
    // Every control here reads and writes plain fields on `AuroraLayout.Look` -- a Java object with
    // no Compose state in it. A drag writes the value, `onChanged()` bumps the revision, and this
    // menu would still not re-run: its parameters (the same `layout`, the same `look`, the same
    // `target`) do not change, so Compose skips the call. The value is applied in the model and
    // simply never painted -- reported as "好像实际有效果，但是不显示".
    //
    // Reading the revision here subscribes the whole menu subtree, which is what makes the slider
    // knob, the swatch ring and every readout follow the value they are bound to.
    //
    // Unconditional and BEFORE anything that can return: the menu must subscribe on its very
    // first composition, not on the branch that happens to exist later.
    @Suppress("UNUSED_EXPRESSION")
    LocalLayoutRevision.current

    // Dismissal by RECTANGLE, measured in the scrim's own coordinate space.
    //
    // Three shapes came before this one, and each failed for a different reason:
    //   1. comparing `change.position` against a layout-callback rectangle -- two different
    //      spaces, so every press inside the panel read as outside;
    //   2. "consume what the children left": ONE pointerInput on the panel Column that consumed
    //      every unconsumed Press. The premise was that children run first (Main is bottom-up),
    //      but a slider does not consume the DOWN at all -- `detectHorizontalDragGestures` waits
    //      for the touch slop, so at press time nothing is consumed and the Column claimed it.
    //      Every slider, swatch and toggle inside then received an already-consumed press, which
    //      is exactly why "完成" (in the header, OUTSIDE that Column) worked while nothing inside
    //      did;
    //   3. (this one) the panel publishes its bounds in ROOT space via onGloballyPositioned, the
    //      scrim tests the press against them, and NOTHING consumes on the panel's behalf. A press
    //      inside is left to whatever control is under it; a press outside closes.
    //
    // Both positions are root-space and both rectangles come from the same layout generation, so
    // there is no space mismatch -- and no consumption to starve a control.
    var bounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (event.type != PointerEventType.Press) continue
                        if (change.isConsumed) continue
                        val panel = bounds
                        val inside = panel != null &&
                            change.position.x >= panel.left && change.position.x <= panel.right &&
                            change.position.y >= panel.top && change.position.y <= panel.bottom
                        if (!inside) {
                            change.consume()
                            onClose()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // THE MENU IS A WINDOW. It used to be a centred Column with a hard-coded 400x560dp size:
        // it could not be moved, resized or maximized ("window 手势对这个窗口没有用"), and its fixed
        // height cut pages off with no indication ("只有几个配置项"). Routing it through the same
        // [AuroraWindowFrame] the main window and the floating panels use gives it identical
        // gestures and a remembered geometry, and the height becomes the player's to set.
        AuroraWindowFrame(
            box = box,
            stage = stage,
            headerHeight = MENU_HEADER_HEIGHT,
            buttonReserve = MENU_BUTTON_RESERVE,
            layout = layout,
            cursor = cursor,
            // The header row below owns the menu's drag, so the frame's overlay band stays off --
            // an overlay would sit above the first rows' controls.
            moveBand = false,
            onMoved = onChanged,
            onOpenMenu = {}
        ) {
        Column(
            Modifier
                .fillMaxSize()
                // Publishes the panel's rectangle in ROOT space. This is the ONLY thing the panel
                // does about dismissal -- it never consumes a press, so every control inside keeps
                // the press it needs to start.
                .onGloballyPositioned { coordinates ->
                    val topLeft = coordinates.positionInRoot()
                    val s = coordinates.size
                    bounds = androidx.compose.ui.geometry.Rect(
                        offset = topLeft,
                        size = androidx.compose.ui.geometry.Size(
                            s.width.toFloat(),
                            s.height.toFloat()
                        )
                    )
                }
                .clip(RoundedCornerShape(Star.panelRadius))
                .background(Star.panel)
                .border(1.dp, Star.hairlineStrong, RoundedCornerShape(Star.panelRadius))
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(MENU_HEADER_HEIGHT)
                    // The header drags the menu, exactly like a panel's header drags it.
                    .panelDragBand(
                        box, layout, target.label,
                        stage.width.toFloat(), stage.height.toFloat(),
                        onChanged
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    target.label,
                    fontSize = TypeScale.panelTitle,
                    color = Star.textPrimary,
                    fontFamily = auroraFont,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "右键展开 · 改动即时生效",
                    fontSize = TypeScale.hint,
                    color = Star.textTertiary,
                    fontFamily = auroraFont
                )
                Spacer(Modifier.weight(1f))
                // QUICK RESET for THIS page only. A player who has been dragging the rail's
                // numbers wants those back without also losing the panel colours they tuned; the
                // group key names exactly the page's fields (see AuroraLayout.resetLookGroup).
                val resetGroup = when (target) {
                    ArrangeTarget.Rail -> "rail"
                    ArrangeTarget.Panel -> "panel"
                    ArrangeTarget.Shell -> "shell"
                    ArrangeTarget.Controls -> "controls"
                    // A section's or entry's own values live in their scope object, not in Look;
                    // their pages reset that scope directly, so there is no Look group here.
                    ArrangeTarget.Section -> null
                    ArrangeTarget.Entry -> null
                }
                if (resetGroup != null) {
                    AuroraButton(
                        "重置本页",
                        onClick = {
                            layout.resetLookGroup(resetGroup)
                            onChanged()
                        },
                        icon = AuroraIcon.Reset
                    )
                    Spacer(Modifier.width(8.dp))
                }
                AuroraButton("完成", onClick = onClose, filled = true)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Star.hairline))
            Column(
                Modifier
                    .fillMaxWidth()
                    .auroraScroll(rememberScrollState())
                    .padding(bottom = 10.dp)
            ) {
                when (target) {
                    ArrangeTarget.Rail -> RailNumbers(layout, look, onChanged)
                    ArrangeTarget.Panel -> PanelNumbers(layout, look, onChanged)
                    ArrangeTarget.Shell -> ShellNumbers(layout, look, onChanged)
                    ArrangeTarget.Controls -> ControlNumbers(layout, look, onChanged)
                    ArrangeTarget.Section -> SectionNumbers(layout, sectionId, onChanged, onClose)
                    ArrangeTarget.Entry -> EntryNumbers(layout, moduleId, sectionId, onChanged)
                }
            }
        }
        }
    }
}

/**
 * One rail section's own settings.
 *
 * <p>This is what makes a section a slot rather than a heading: it has a name the player sets, a
 * collapsed state, and the unify action. The name is edited through a row of presets rather than a text
 * field -- a text field in a game overlay needs a caret, an IME and a focus model, and the names a
 * player wants here are the feature names this mod already has.
 *
 * <p>"统一模块内容" copies the first section's module list to every other section. Content only: the
 * sections keep their own names and their own layout values, so a player can make every slot offer the
 * same features while still dressing them differently.
 */
@Composable
private fun EntryNumbers(
    layout: AuroraLayout,
    moduleId: String?,
    sectionId: String?,
    onChanged: () -> Unit
) {
    val module = moduleId?.let { GodSwordModules.byId(it) }
    if (module == null) {
        Text(
            "该条目已不存在",
            fontSize = TypeScale.row,
            color = Star.textTertiary,
            fontFamily = auroraFont,
            modifier = Modifier.padding(14.dp)
        )
        return
    }
    Group("条目")
    AuroraRow("名称", "右键左栏条目打开此页") {
        Text(
            module.label,
            fontSize = TypeScale.value,
            color = Star.textSecondary,
            fontFamily = auroraFont
        )
    }
    Group("外观覆盖")
    ScopeFieldRows(layout, { layout.moduleScopeFor(module.id) }, module.id, sectionId, onChanged)
    Group("整条恢复")
    AuroraRow("清除全部覆盖", "这个条目的所有值都回到分类与整栏") {
        AuroraButton("恢复继承", onClick = {
            layout.clearModuleScope(module.id)
            onChanged()
        })
    }
}

/**
 * The rows a scope offers, shared by the entry page and the category page.
 *
 * <p>One implementation for both scopes because they expose the same nine numbers -- a category
 * sets what its entries share, an entry sets what only it uses -- and two copies would be two
 * places for a field to be forgotten when one is added.
 *
 * <p>Each row reads the EFFECTIVE value (post-cascade) and labels where it comes from. Writing
 * sets an override at THIS scope; the per-row clear control removes just that field, so a player
 * who set four numbers and wants one back on the cascade keeps the other three.
 */
@Composable
private fun ScopeFieldRows(
    layout: AuroraLayout,
    scope: () -> AuroraLayout.ScopeLook,
    moduleId: String?,
    sectionId: String?,
    onChanged: () -> Unit
) {
    val effective = layout.resolveRail(moduleId, sectionId)
    val own = scope()
    val inheritedFrom = if (moduleId != null) "继承自分类" else "继承自整栏"

    fun source(isSet: Boolean): String = if (isSet) "已覆盖" else inheritedFrom
    fun clear(field: String) {
        if (moduleId != null) layout.clearModuleField(moduleId, field)
        else if (sectionId != null) layout.clearSectionField(sectionId, field)
        onChanged()
    }

    ScopeNum("条目高度", 18f, 80f, 1f, effective.itemHeight, own.hasItemHeight,
        source(own.hasItemHeight), { clear("itemHeight") }) {
        own.itemHeight = it; own.hasItemHeight = true; onChanged()
    }
    ScopeNum("条目圆角", 0f, 24f, 1f, effective.itemCorner, own.hasItemCorner,
        source(own.hasItemCorner), { clear("itemCorner") }) {
        own.itemCorner = it; own.hasItemCorner = true; onChanged()
    }
    ScopeNum("条目内缩进", 0f, 48f, 1f, effective.itemPadding, own.hasItemPadding,
        source(own.hasItemPadding), { clear("itemPadding") }) {
        own.itemPadding = it; own.hasItemPadding = true; onChanged()
    }
    ScopeNum("标记条宽度", 0f, 10f, 0.5f, effective.markerWidth, own.hasMarkerWidth,
        source(own.hasMarkerWidth), { clear("markerWidth") }) {
        own.markerWidth = it; own.hasMarkerWidth = true; onChanged()
    }
    ScopeNum("标签字号", 8f, 24f, 1f, effective.fontSize, own.hasFontSize,
        source(own.hasFontSize), { clear("fontSize") }) {
        own.fontSize = it; own.hasFontSize = true; onChanged()
    }

    Group("配色")
    ScopeSwatch("标记颜色", effective.markerColor, own.hasMarkerColor,
        source(own.hasMarkerColor), { clear("markerColor") }) {
        own.markerColor = it; own.hasMarkerColor = true; onChanged()
    }
    ScopeSwatch("未选中文字", effective.textColor, own.hasTextColor,
        source(own.hasTextColor), { clear("textColor") }) {
        own.textColor = it; own.hasTextColor = true; onChanged()
    }
    ScopeSwatch("选中文字", effective.textSelected, own.hasTextSelected,
        source(own.hasTextSelected), { clear("textSelected") }) {
        own.textSelected = it; own.hasTextSelected = true; onChanged()
    }
    ScopeSwatch("选中底板", effective.selectedPlate, own.hasSelectedPlate,
        source(own.hasSelectedPlate), { clear("selectedPlate") }) {
        own.selectedPlate = it; own.hasSelectedPlate = true; onChanged()
    }
}

/**
 * A numeric row for a scope.
 *
 * <p>The clear control appears ONLY while the value is actually overridden: a control that does
 * nothing is worse than no control, because it suggests the value is inherited when it is not.
 */
@Composable
private fun ScopeNum(
    label: String,
    min: Float,
    max: Float,
    step: Float,
    effective: Float,
    isOverridden: Boolean,
    source: String,
    onClear: () -> Unit,
    onWrite: (Float) -> Unit
) {
    AuroraRow(label, source) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isOverridden) {
                ClearFieldButton(onClear)
                Spacer(Modifier.width(6.dp))
            }
            AuroraSlider(
                value = effective,
                range = min..max,
                step = step,
                onValueChange = onWrite,
                format = { "${it.toInt()}" },
                width = 110.dp
            )
        }
    }
}

/** A colour row for a scope, with the same "clear returns to inheritance" control. */
@Composable
private fun ScopeSwatch(
    label: String,
    effective: Int,
    isOverridden: Boolean,
    source: String,
    onClear: () -> Unit,
    onWrite: (Int) -> Unit
) {
    AuroraRow(label, source) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isOverridden) {
                ClearFieldButton(onClear)
                Spacer(Modifier.width(6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                PALETTE.forEach { colour ->
                    val isCurrent = (colour and 0xFFFFFF) == (effective and 0xFFFFFF)
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(Star.controlRadius))
                            .background(Color(colour))
                            .border(
                                if (isCurrent) 2.dp else 1.dp,
                                if (isCurrent) Star.textPrimary else Star.hairline,
                                RoundedCornerShape(Star.controlRadius)
                            )
                            .handCursor()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onWrite((colour and 0xFFFFFF) or (effective and 0xFF000000.toInt()))
                            }
                    )
                }
            }
        }
    }
}

/** The small "return this field to its parent scope" control. */
@Composable
private fun ClearFieldButton(onClear: () -> Unit) {
    Box(
        Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Star.hover)
            .handCursor()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClear() },
        contentAlignment = Alignment.Center
    ) {
        AuroraIcon(
            AuroraIcon.Reset,
            tint = Star.textSecondary,
            modifier = Modifier.size(IconSize.inline)
        )
    }
}

@Composable
private fun SectionNumbers(
    layout: AuroraLayout,
    sectionId: String?,
    onChanged: () -> Unit,
    onClose: () -> Unit
) {
    val sections = layout.sections
    val section = sections.firstOrNull { it.id == sectionId }
    if (section == null) {
        Text(
            "该分组已不存在",
            fontSize = TypeScale.row,
            color = Star.textTertiary,
            fontFamily = auroraFont,
            modifier = Modifier.padding(14.dp)
        )
        return
    }

    Group("外观覆盖")
    // The category's own overrides. Every row shows the EFFECTIVE value (what the entries
    // currently use, after the cascade) and states where it comes from; editing it writes an
    // override at this scope, and the clear button returns that one field to the parent.
    ScopeFieldRows(
        layout = layout,
        scope = { layout.sectionScopeFor(section.id) },
        moduleId = null,
        sectionId = section.id,
        onChanged = onChanged
    )

    Group("名称")
    AuroraRow("分组名", "") {
        AuroraSelector(
            options = SECTION_NAMES,
            selectedIndex = SECTION_NAMES.indexOf(section.name).coerceAtLeast(0),
            onSelect = {
                section.name = SECTION_NAMES[it]
                onChanged()
            },
            width = 120.dp
        )
    }
    // A read-only count: how many modules a section holds is a consequence of dragging entries, not
    // something to set from a slider. Written as a no-op sink so the row still shows the value.
    Num("组内条目数", 0f, 12f, 1f, { section.modules.size.toFloat() }, { _ -> }, {
        "${it.toInt()} 项（拖动条目增减）"
    })

    Group("行为")
    AuroraRow("折叠本组", "收起条目但保留组名") {
        AuroraToggle(
            checked = section.collapsed,
            onCheckedChange = { section.collapsed = it; onChanged() }
        )
    }
    AuroraRow("一键统一", "把第一个分组的模块复制到所有分组") {
        AuroraButton("统一模块内容", onClick = {
            layout.unifySections()
            onChanged()
            onClose()
        }, filled = true)
    }

    Group("分组管理")
    AuroraRow("新增分组", "在末尾追加一个空分组") {
        AuroraButton("＋ 新增", onClick = {
            val id = "sec-" + (System.currentTimeMillis() % 100000)
            layout.sections.add(AuroraLayout.Section(id, "模块", ArrayList()))
            onChanged()
            onClose()
        })
    }
    AuroraRow("删除本组", "组内模块会移入其后的分组") {
        AuroraButton("删除", onClick = {
            val index = layout.sections.indexOfFirst { it.id == sectionId }
            if (index >= 0) {
                val orphaned = layout.sections[index].modules
                layout.sections.removeAt(index)
                // The modules are moved rather than dropped: deleting a slot is not a request to lose
                // the features that were in it, and a feature with no entry anywhere would be
                // unreachable from inside the window.
                val target = layout.sections.getOrNull(index) ?: layout.sections.lastOrNull()
                if (target != null) target.modules.addAll(orphaned)
                // A last remaining section cannot be deleted; the rail would have nothing to draw.
                if (layout.sections.isEmpty()) layout.sections.add(AuroraLayout.Section("default", "模块", ArrayList(orphaned)))
            }
            onChanged()
            onClose()
        })
    }
}

/** The names a section may take: the feature groups this mod already has, plus the generic ones. */
private val SECTION_NAMES = listOf("时停", "术式", "客户端", "模块", "战斗", "画面", "其他")

/** The left rail's numbers: geometry, colour, and where the whole column sits. */
@Composable
private fun RailNumbers(layout: AuroraLayout, look: AuroraLook, onChanged: () -> Unit) {    Group("尺寸")
    Num("栏宽", 90f, 320f, 1f, { layout.look.railWidth }, {
        layout.look.railWidth = it; onChanged()
    }, { "${it.toInt()} dp" })
    Num("条目高度", 22f, 60f, 1f, { layout.look.railItemHeight }, {
        layout.look.railItemHeight = it; onChanged()
    }, { "${it.toInt()} dp" })
    Num("条目圆角", 0f, 20f, 1f, { layout.look.railItemCorner }, {
        layout.look.railItemCorner = it; onChanged()
    }, { "${it.toInt()} dp" })
    Num("条目内缩进", 0f, 40f, 1f, { layout.look.railItemPadding }, {
        layout.look.railItemPadding = it; onChanged()
    }, { "${it.toInt()} dp" })
    Num("整体横向偏移", -400f, 400f, 1f, { layout.look.railOffsetX }, {
        layout.look.railOffsetX = it; onChanged()
    }, { "${it.toInt()} dp" })
    Num("整体纵向偏移", -400f, 400f, 1f, { layout.look.railOffsetY }, {
        layout.look.railOffsetY = it; onChanged()
    }, { "${it.toInt()} dp" })
    Num("标记条宽度", 0f, 8f, 0.5f, { layout.look.railMarkerWidth }, {
        layout.look.railMarkerWidth = it; onChanged()
    }, { String.format(Locale.ROOT, "%.1f dp", it) })
    Num("标签字号", 9f, 20f, 1f, { layout.look.railFontSize }, {
        layout.look.railFontSize = it; onChanged()
    }, { "${it.toInt()} sp" })

    Group("配色")
    Swatch("标记颜色", { layout.look.railMarkerColor }, { layout.look.railMarkerColor = it; onChanged() })
    Swatch("未选中文字", { layout.look.railTextColor }, { layout.look.railTextColor = it; onChanged() })
    Swatch("选中文字", { layout.look.railTextSelected }, { layout.look.railTextSelected = it; onChanged() })
    Swatch("分组标签", { layout.look.railGroupColor }, { layout.look.railGroupColor = it; onChanged() })
    Alpha("选中底板不透明度", { layout.look.railSelectedPlate }, { layout.look.railSelectedPlate = it; onChanged() })
}

/** A floating panel's numbers. */
@Composable
private fun PanelNumbers(layout: AuroraLayout, look: AuroraLook, onChanged: () -> Unit) {
    Group("尺寸")
    Num("圆角", 0f, 32f, 1f, { layout.look.panelCorner }, { layout.look.panelCorner = it; onChanged() }, { "${it.toInt()} dp" })
    Num("边框粗细", 0f, 6f, 0.5f, { layout.look.panelBorderWidth }, { layout.look.panelBorderWidth = it; onChanged() }, { String.format(Locale.ROOT, "%.1f dp", it) })
    Num("标题栏高度", 30f, 90f, 1f, { layout.look.panelHeaderHeight }, { layout.look.panelHeaderHeight = it; onChanged() }, { "${it.toInt()} dp" })
    Num("行高", 22f, 60f, 1f, { layout.look.rowHeight }, { layout.look.rowHeight = it; onChanged() }, { "${it.toInt()} dp" })
    Num("行内缩进", 0f, 40f, 1f, { layout.look.rowPadding }, { layout.look.rowPadding = it; onChanged() }, { "${it.toInt()} dp" })
    Num("行字号", 9f, 20f, 1f, { layout.look.rowFontSize }, { layout.look.rowFontSize = it; onChanged() }, { "${it.toInt()} sp" })

    Group("配色")
    Swatch("面板底色", { layout.look.panelBg }, { layout.look.panelBg = it; onChanged() })
    Swatch("边框颜色", { layout.look.panelBorderColor }, { layout.look.panelBorderColor = it; onChanged() })
    Swatch("行标签", { layout.look.rowLabelColor }, { layout.look.rowLabelColor = it; onChanged() })
    Swatch("说明文字", { layout.look.hintColor }, { layout.look.hintColor = it; onChanged() })
}

/** The shell's numbers: the frame around everything. */
@Composable
private fun ShellNumbers(layout: AuroraLayout, look: AuroraLook, onChanged: () -> Unit) {
    Group("尺寸")
    Num("外边距", 0f, 200f, 1f, { layout.look.shellInset }, { layout.look.shellInset = it; onChanged() }, { "${it.toInt()} dp" })
    Num("圆角", 0f, 40f, 1f, { layout.look.shellCorner }, { layout.look.shellCorner = it; onChanged() }, { "${it.toInt()} dp" })
    Num("边框粗细", 0f, 6f, 0.5f, { layout.look.shellBorderWidth }, { layout.look.shellBorderWidth = it; onChanged() }, { String.format(Locale.ROOT, "%.1f dp", it) })
    Num("标题栏高度", 36f, 120f, 1f, { layout.look.headerHeight }, { layout.look.headerHeight = it; onChanged() }, { "${it.toInt()} dp" })

    Group("配色")
    Swatch("上方底色", { layout.look.shellBgTop }, { layout.look.shellBgTop = it; onChanged() })
    Swatch("下方底色", { layout.look.shellBgBottom }, { layout.look.shellBgBottom = it; onChanged() })
    Swatch("边框颜色", { layout.look.shellBorderColor }, { layout.look.shellBorderColor = it; onChanged() })
}

/** The shared control numbers: what a toggle, a slider and every border are made of. */
@Composable
private fun ControlNumbers(layout: AuroraLayout, look: AuroraLook, onChanged: () -> Unit) {
    Group("尺寸")
    Num("控件圆角", 0f, 20f, 0.5f, { layout.look.controlCorner }, { layout.look.controlCorner = it; onChanged() }, { String.format(Locale.ROOT, "%.1f dp", it) })
    Num("开关宽度", 24f, 72f, 1f, { layout.look.toggleWidth }, { layout.look.toggleWidth = it; onChanged() }, { "${it.toInt()} dp" })
    Num("开关高度", 14f, 40f, 1f, { layout.look.toggleHeight }, { layout.look.toggleHeight = it; onChanged() }, { "${it.toInt()} dp" })

    Group("配色")
    Swatch("强调色", { layout.look.accent }, { layout.look.accent = it; onChanged() })
    Swatch("强调色暗部", { layout.look.accentDim }, { layout.look.accentDim = it; onChanged() })
    Swatch("轨道未填充", { layout.look.trackColor }, { layout.look.trackColor = it; onChanged() })
}

/** The menu header's height in dp: title, hint and the 完成 button share one row. */
private val MENU_HEADER_HEIGHT: androidx.compose.ui.unit.Dp = 44.dp

/** Width the header's right end reserves so the drag band cannot cover 完成. */
private val MENU_BUTTON_RESERVE: androidx.compose.ui.unit.Dp = 90.dp

/** A group heading inside the menu. */
@Composable
private fun Group(name: String) {
    Text(
        name,
        fontSize = TypeScale.hint,
        color = Star.textTertiary,
        fontFamily = auroraFont,
        modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 2.dp)
    )
}

/** One numeric row: a label, a slider, and a live readout in the value's own unit. */
@Composable
private fun Num(
    label: String,
    min: Float,
    max: Float,
    step: Float,
    read: () -> Float,
    write: (Float) -> Unit,
    format: (Float) -> String
) {
    // SUBSCRIBE AT THE LEAF. A subscription higher up does not help these: the menu's root reads
    // the revision and re-runs, but `RailNumbers(layout, look, onChanged)` is called with unchanged
    // parameters, so Compose skips that whole subtree -- and the leaf that actually reads a Java
    // field never re-runs. The value changes in the model, the menu recomposes, and the knob stays
    // put: reported as "圆钮不动、读数不变". The subscription therefore belongs on the composable
    // that reads the value, not on its ancestor.

    AuroraRow(label, "") {    @Suppress("UNUSED_EXPRESSION")
    com.yuan.client.aurora.LocalLayoutRevision.current

        AuroraSlider(
            value = read(),
            range = min..max,
            step = step,
            onValueChange = write,
            format = format,
            width = 150.dp
        )
    }
}

/**
 * A colour row.
 *
 * <p>Cycled through a fixed set of readable hues rather than opened as a picker. A full RGB picker needs
 * three sliders and a preview, which at this density is a panel inside a panel; the palette covers what
 * a player actually asks for here -- "make the rail dimmer", "make the accent green" -- and the file
 * keeps the exact packed value for anyone who wants a specific one.
 */
@Composable
private fun Swatch(label: String, read: () -> Int, write: (Int) -> Unit) {
    // SUBSCRIBE AT THE LEAF. A subscription higher up does not help these: the menu's root reads
    // the revision and re-runs, but `RailNumbers(layout, look, onChanged)` is called with unchanged
    // parameters, so Compose skips that whole subtree -- and the leaf that actually reads a Java
    // field never re-runs. The value changes in the model, the menu recomposes, and the knob stays
    // put: reported as "圆钮不动、读数不变". The subscription therefore belongs on the composable
    // that reads the value, not on its ancestor.
    @Suppress("UNUSED_EXPRESSION")
    com.yuan.client.aurora.LocalLayoutRevision.current

    val current = read()
    AuroraRow(label, "") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PALETTE.forEach { colour ->
                val isCurrent = (colour and 0xFFFFFF) == (current and 0xFFFFFF)
                val interaction = remember(colour) { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                Box(
                    Modifier
                        .width(20.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(Star.controlRadius))
                        .background(Color(colour))
                        .border(
                            if (isCurrent) 2.dp else 1.dp,
                            when {
                                isCurrent -> Star.textPrimary
                                hovered -> Star.hairlineStrong
                                else -> Star.hairline
                            },
                            RoundedCornerShape(Star.controlRadius)
                        )
                        .hoverable(interaction)
                        .clickable(interactionSource = interaction, indication = null) {
                            // The alpha travels with the swap: a player adjusting a plate's opacity wants
                            // the new hue at the opacity they already chose.
                            write((colour and 0xFFFFFF) or (current and 0xFF000000.toInt()))
                        }
                )
            }
        }
    }
}

/**
 * An alpha row: the same colour, made more or less present.
 *
 * <p>Separate from [Swatch] because opacity and hue are different questions, and the values that read
 * well for one are not the values for the other -- a plate wants to be faint, a border wants to be
 * visible, and neither cares which hue it is.
 */
@Composable
private fun Alpha(label: String, read: () -> Int, write: (Int) -> Unit) {
    // SUBSCRIBE AT THE LEAF. A subscription higher up does not help these: the menu's root reads
    // the revision and re-runs, but `RailNumbers(layout, look, onChanged)` is called with unchanged
    // parameters, so Compose skips that whole subtree -- and the leaf that actually reads a Java
    // field never re-runs. The value changes in the model, the menu recomposes, and the knob stays
    // put: reported as "圆钮不动、读数不变". The subscription therefore belongs on the composable
    // that reads the value, not on its ancestor.
    @Suppress("UNUSED_EXPRESSION")
    com.yuan.client.aurora.LocalLayoutRevision.current

    val current = read()
    val alpha = ((current ushr 24) and 0xFF) / 255f
    AuroraRow(label, "") {
        AuroraSlider(
            value = alpha,
            range = 0f..1f,
            step = 0.05f,
            onValueChange = { write((current and 0xFFFFFF) or ((Math.round(it * 255f) and 0xFF) shl 24)) },
            format = { String.format(Locale.ROOT, "%.0f%%", it * 100f) },
            width = 150.dp
        )
    }
}

/**
 * The hues a swatch cycles through.
 *
 * <p>Chosen to be legible on this near-black ground and mutually distinguishable: four greys for the
 * structural values, a blue that matches the accent, and three saturated hues for a player who wants the
 * window to say something. A full spectrum would be a picker, and a picker is the panel-inside-a-panel
 * this deliberately is not.
 */
private val PALETTE = listOf(
    0xFFF2F2F4.toInt(),  // near-white
    0xFFD8D8DC.toInt(),  // primary text
    0xFF8A8A90.toInt(),  // secondary text
    0xFF5E5E66.toInt(),  // tertiary
    0xFF2A2A30.toInt(),  // dark groove
    0xFF121215.toInt(),  // panel
    0xFF0A0A0C.toInt(),  // shell
    0xFF4A90E2.toInt(),  // accent blue
    0xFF3DD6E8.toInt(),  // cyan
    0xFF7C5CFF.toInt(),  // violet
    0xFFFF7A5C.toInt(),  // coral
    0xFF7AE85D.toInt()   // green
)
