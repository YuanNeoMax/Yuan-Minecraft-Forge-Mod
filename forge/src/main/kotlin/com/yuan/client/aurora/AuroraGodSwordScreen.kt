package com.yuan.client.aurora

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.yuan.client.aurora.anim.Motion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.AuroraEditHistory
import com.yuan.client.aurora.AuroraLook
import com.yuan.client.aurora.ProvideLayoutRevision
import com.yuan.client.aurora.panels.ArrangeMenu
import com.yuan.client.aurora.panels.ArrangeTarget
import com.yuan.client.aurora.icons.AuroraIcon
import com.yuan.client.aurora.panels.AuroraSettingsPanel
import com.yuan.client.aurora.panels.GodRow
import com.yuan.client.aurora.panels.GodSwordBinding
import com.yuan.client.aurora.panels.GodSwordModules
import androidx.compose.runtime.rememberCoroutineScope
import com.yuan.client.aurora.panels.GodSwordSearchResults
import com.yuan.client.aurora.panels.GodSwordInlinePanel
import com.yuan.client.aurora.panels.GodSwordPanel
import com.yuan.client.aurora.panels.GodSwordRows
import com.yuan.client.aurora.scaffold.AuroraButton
import com.yuan.client.aurora.scaffold.FocusState
import com.yuan.client.aurora.scaffold.FocusRegion
import com.yuan.client.aurora.scaffold.AuroraKeys
import com.yuan.client.aurora.scaffold.clearFocusOnPointer
import com.yuan.client.aurora.scaffold.AuroraContentHeader
import com.yuan.client.aurora.scaffold.AuroraFloatingPanel
import com.yuan.client.aurora.scaffold.AuroraPanelDrag
import com.yuan.client.aurora.scaffold.AuroraRail
import com.yuan.client.aurora.scaffold.AuroraRailEntry
import com.yuan.client.aurora.scaffold.AuroraRailFooter
import com.yuan.client.aurora.scaffold.AuroraSearchField
import com.yuan.client.aurora.scaffold.RailSection
import com.yuan.client.aurora.scaffold.AuroraShell
import com.yuan.client.aurora.scaffold.ResizeCursorState
import com.yuan.client.aurora.scaffold.trackParallax
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale
import com.yuan.client.aurora.theme.auroraFont
import com.yuan.client.gui.V9Holder
import com.yuan.client.gui.toConfig
import com.yuan.client.gui.v9StateOf
import com.yuan.item.YuanGodSwordConfig
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/**
 * The god sword's configuration window: the real settings, in the v3 shell.
 *
 * <p>Every row comes from the row tables and every value goes through [GodSwordBinding], which
 * dispatches onto the same `V9Holder` the older screen uses. Nothing here re-implements a setting:
 * adding one is a line in a table plus a line in the binding.
 *
 * <p>Three write paths, matching the older screen's semantics exactly, because each answers a
 * different question:
 * <list>
 * <li>**Preview** -- every edit writes the client's copy of the stack at once, so the sword in the
 * player's hand changes while they drag a slider. The server keeps the last saved state, so an
 * abandonable edit never reaches survival.
 * <li>**Save** -- writes the stack for real and syncs the whole config tag to the server, presets
 * included, then clears any pending time-stop cooldown so the new settings apply immediately.
 * <li>**Discard** -- puts the client's copy back to the last saved state on the way out.
 * </list>
 *
 * <p>The unsaved-changes state drives the footer and ESC. ESC opens the confirm rather than closing,
 * because a window with a live preview has a real "these edits are about to be thrown away" moment
 * that a modal has to ask about.
 */
class AuroraGodSwordScreen(stack: ItemStack) : AuroraComposeScreen("星渊 · 神器之刃") {

    override val backdropDim: Float = 0.52f

    init {
        loadConfig(stack)
    }

    @Composable
    override fun WindowContent() {
        val revision = layoutRevision
        val drag = remember { AuroraPanelDrag() }

        // The stage, measured at this composition's root. The menu is a window-level surface, and
        // the shell only hands its own INTERIOR size to `content` -- so the menu measures the same
        // thing the shell does, once, here.
        var stageSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

        // The holder mirrors the older screen's: one mutable state object per open, seeded from the
        // sword's config and written back on save. Building it once per open is what makes an edit
        // survive a scroll or a module switch.
        val holder = remember { V9Holder(v9StateOf(editingConfig)) }

        // UNDO/REDO, and the debounce that gives it the right granularity.
        //
        // A slider drag rewrites `holder` on every frame it moves. Committing per frame would fill
        // the stack with hundreds of one-pixel steps, so Ctrl+Z would appear to do nothing -- it
        // would undo a single pixel of the drag. `pendingEdit` marks "something changed" and the
        // effect below commits only once the values have gone quiet, which makes one gesture land as
        // one step. That is what a player means by undo: put back what I just did.
        //
        // Keyed on the holder, so the history is per-open: reopening the window starts a fresh
        // session rather than continuing a stack that refers to a configuration no longer loaded.
        val history = remember(holder) { AuroraEditHistory(holder.snapshot()) }
        var pendingEdit by remember { mutableStateOf(false) }

        var query by remember { mutableStateOf("") }
        var selectedModule by remember { mutableStateOf(GodSwordModules.MODULES.first().id) }
        // The open panels: a group's editors, keyed by group id. Several may be open at once --
        // the reference cascades them and the pin button tiers them -- and opening the same group
        // twice raises its existing panel rather than stacking a copy. (An earlier single-slot
        // version existed only because a list was once appended to per rail click; the list is
        // written through a set-like replace, so that failure cannot return.)
        var openPanels by remember { mutableStateOf(listOf<String>()) }
        // Panels playing their dismissal. They stay rendered (and are skipped by the stale sweep)
        // until the ramp finishes, then the effect below removes them for good.
        var closingPanels by remember { mutableStateOf(listOf<String>()) }
        // A local helper rather than four copies of "start closing": dismiss is reached from the
        // panel's own close control and from ESC, and the two must behave identically.
        val dismissPanel = { id: String ->
            if (id in openPanels && id !in closingPanels) {
                closingPanels = closingPanels + id
                // The opening list loses it immediately so it cannot be raised or focused, while the
                // rendering list (below) keeps it until the animation has run.
                openPanels = openPanels - id
            }
        }
        // The shared resize-cursor state: one per stage, feeding the cursor overlay at the root.
        val cursor = remember { ResizeCursorState() }
        // Which element the arrangement menu is editing, or null when none is open.
        var menuTarget by remember { mutableStateOf<ArrangeTarget?>(null) }
        // The live view over the adjustable values. Built once per open, like the layout itself.
        val look = remember { AuroraLook(layout) }
        var presetSlot by remember { mutableStateOf(0) }
        var dirty by remember { mutableStateOf(false) }
        var confirming by remember { mutableStateOf(false) }

        // Any edit through the binding marks the window dirty. The flag is read by the footer and by
        // the ESC path; it is deliberately not a per-row comparison, which would cost a full config
        // diff on every frame.
        //
        // It also arms the undo history's debounce. Every write in this window goes through here, so
        // this is the one place that knows a setting changed -- hooking it here means a new control
        // cannot forget to record itself.
        val markDirty = {
            dirty = true
            pendingEdit = true
        }

        // The section being edited by the arrangement menu, or null.
        var menuSection by remember { mutableStateOf<String?>(null) }
        // The entry an Entry-scope menu is editing. Null for every other scope, which is what keeps
        // a stale id from leaking into a rail or category menu.
        var menuModule by remember { mutableStateOf<String?>(null) }
        // The keyboard focus model. One per open: focus resets when the window is reopened, which is
        // what a player expects -- they are not returning to a half-navigated window they left hours
        // ago, and a remembered index could point at a module that no longer exists.
        val focus = remember { FocusState() }

        // ESC leaves the arrangement mode -- and the menu first, if one is open -- before it closes
        // the window. A plain callback rather than CompositionLocal because the key path lives in the
        // host class, which is not a composable and can only hold a callback.
        onEscapePressed = {
            when {
                // The menu is the topmost surface, so it goes first.
                menuTarget != null || menuSection != null -> {
                    menuTarget = null
                    menuSection = null
                    true
                }
                // Then the panels, topmost first. Previously ESC went straight from "close the menu"
                // to "close the whole window", so the only way out of a panel was to aim at its own
                // close control -- and a player several panels deep had to dismiss each one, or lose
                // the window and every unsaved edit with it. ESC is the general way out; a surface
                // that can only be left by hitting a small target is the same trap the ESC hatch
                // exists to prevent.
                openPanels.isNotEmpty() -> {
                    dismissPanel(openPanels.last())
                    // Focus cannot stay in a panel that is no longer open.
                    if (focus.region == FocusRegion.Panel) {
                        focus.region = FocusRegion.Rail
                        focus.index = 0
                    }
                    true
                }
                // Nothing left to back out of: hand it back so the screen can ask about unsaved
                // changes and close the window.
                else -> false
            }
        }

        // The rail's sections, resolved against the player's stored arrangement. The declared sections
        // are handed in on every pass rather than remembered: a module added in a later version must
        // appear, and `sectionsOrDefault` is what appends it without disturbing what is already placed.
        val railSections = remember(revision) {
            layout.sectionsOrDefault(GodSwordModules.declaredSections()).map { section ->
                RailSection(
                    id = section.id,
                    name = section.name,
                    collapsed = section.collapsed,
                    entries = section.modules.mapNotNull { id ->
                        GodSwordModules.byId(id)?.let { AuroraRailEntry(it.id, it.label) }
                    }
                )
            }
        }

        val module = GodSwordModules.byId(selectedModule) ?: GodSwordModules.MODULES.first()
        val allRows = remember(selectedModule, revision) { rowsFor(module, holder) }

        // Hoisted out of the content area, where it used to live, because the KEYBOARD path needs the
        // same list: the focus model must know how many rows it can walk, and deriving that count
        // separately would let the two drift -- the count would say six while the area drew five.
        // It depends on nothing the content area owns (module, holder, selectedModule, query,
        // revision), so lifting it changes no behaviour.
        val groups = remember(selectedModule, revision, query) {
            val all = GodSwordModules.groupsOf(module, rowsFor(module, holder))
            if (query.isEmpty()) {
                all
            } else {
                // A search keeps only the rows that match, so a hit inside a long section shows the
                // matched rows rather than the whole section.
                all.mapNotNull { group ->
                    val matching = group.rows.filter {
                        it.row.name.contains(query, true)
                    }
                    if (matching.isEmpty()) null else group.copy(rows = matching)
                }
            }
        }

        // THE CROSS-MODULE SEARCH. Computed for every module, not just the selected one -- finding
        // "冷却时间" should not require already knowing which of the eight features holds it.
        //
        // Keyed on `query` and the revision, so it is recomputed when the query changes or a value
        // moves (a rename or a gate can add or remove a hit) and not on every recomposition.
        val searchHits = remember(query, revision) {
            if (query.isBlank()) {
                emptyList()
            } else {
                GodSwordModules.search(query) { m -> rowsFor(m, holder) }
            }
        }

        // The rail's live entry ids, in draw order. Derived from the same `railSections` the rail
        // renders, so the keyboard cannot walk to an entry that is not on screen.
        val railEntryIds = remember(railSections) {
            railSections.flatMap { section -> section.entries.map { it.id } }
        }

        // The debounced commit. `pendingEdit` is set by every edit; this waits for the values to
        // settle and records one step. The delay is longer than a frame but far shorter than a
        // person's sense of "that was one action", so a drag lands as a single undo while two
        // deliberate clicks stay two.
        LaunchedEffect(pendingEdit) {
            if (pendingEdit) {
                delay(EDIT_SETTLE_MS)
                history.commit(holder.snapshot())
                pendingEdit = false
            }
        }

        // THE KEY PATH, installed after the lists it walks. It closes over this frame's state, so it
        // is reassigned on every composition; the host hook holds the latest.
        //
        // A NAMED LOCAL FUNCTION rather than a lambda assigned to the hook. A lambda has no implicit
        // label when it is assigned (only when it is passed as an argument), so the shortcut's early
        // returns had nothing to return to and would not compile -- and spelling the label out does
        // not help, because the implicit ones are the only names available.
        fun handleKey(keyCode: Int, modifiers: Int): Boolean {
            // Shortcuts first: Ctrl+Z/Y are unambiguous and must work wherever focus happens to be.
            if (AuroraKeys.isCtrl(modifiers)) {
                val restored = when (keyCode) {
                    AuroraKeys.Z -> history.undo()
                    AuroraKeys.Y -> history.redo()
                    else -> null
                }
                if (restored != null) {
                    holder.restore(restored)
                    markDirty()
                    previewConfig(holder.snapshot().toConfig())
                    touchLayout()
                    // The undo step must not itself be recorded as an edit: the debounce would then
                    // commit the restored state as a new change, and redo would never be reachable.
                    pendingEdit = false
                    return true
                }
                // Consumed either way: a Ctrl+Z at the bottom of the history must not fall through
                // and be typed into whatever happens to be listening.
                if (keyCode == AuroraKeys.Z || keyCode == AuroraKeys.Y) return true
            }
            val regionSize = when (focus.region) {
                FocusRegion.Rail -> railEntryIds.size
                FocusRegion.Content -> groups.size
                // Panels and the menu are opened and closed deliberately rather than walked into;
                // they keep whatever index they had so reopening one returns you where you were.
                FocusRegion.Panel, FocusRegion.Menu -> 0
            }
            return when {
                keyCode == AuroraKeys.UP || keyCode == AuroraKeys.DOWN -> {
                    focus.step(if (keyCode == AuroraKeys.DOWN) 1 else -1, regionSize)
                    true
                }
                keyCode == AuroraKeys.TAB -> {
                    focus.step(1, regionSize)
                    true
                }
                // Left/right cross between the rail and the content area. They deliberately do NOT
                // enter a floating panel: a panel is opened and closed by its own controls, and an
                // arrow key that jumped into one would be a trap with no visible way back.
                keyCode == AuroraKeys.LEFT || keyCode == AuroraKeys.RIGHT -> {
                    focus.cycle(
                        if (keyCode == AuroraKeys.RIGHT) 1 else -1,
                        railEntryIds.size,
                        groups.size
                    )
                    true
                }
                // Enter/Space activate what is focused. On the rail that is "open this feature",
                // exactly what a left click does -- the two paths call the same write, so they
                // cannot drift apart.
                AuroraKeys.isActivate(keyCode) -> {
                    if (focus.region == FocusRegion.Rail) {
                        railEntryIds.getOrNull(focus.index)?.let { id ->
                            selectedModule = id
                            touchLayout()
                        }
                    }
                    true
                }
                else -> false
            }
        }
        onKeyPressed = { keyCode, modifiers -> handleKey(keyCode, modifiers) }

        ProvideLayoutRevision(layout, revision) {
        // The composition root, measured once: the stage every window-level surface resolves its
        // fractions against. The menu needs it, and the shell only hands its interior down.
        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { stageSize = it }
                // A mouse move puts the focus ring away: the keyboard is no longer the input in use.
                // Applied at the root so it rides the hit path rather than competing for it.
                .clearFocusOnPointer(focus)
        ) {
        AuroraShell(
            title = "星渊",
            subtitle = "神器之刃 · 配置",
            layout = layout,
            cursor = cursor,
            onMoved = { touchLayout() },
            actions = {
                // The arrangement mode button is gone: the window gestures are resident and the
                // customization menus open by right-click. A mode button that arms what is already
                // armed would be a third way to say the same thing.
                AuroraButton(
                    text = if (dirty) "保存更改" else "已保存",
                    onClick = {
                        saveConfig()
                        dirty = false
                    },
                    filled = dirty
                )
                Spacer(Modifier.width(8.dp))
                AuroraButton(
                    text = "关闭",
                    onClick = {
                        if (dirty && !confirming) {
                            confirming = true
                        } else {
                            requestClose()
                        }
                    }
                )
            },
            rail = {
                Column(Modifier.fillMaxHeight()) {
                    Box(Modifier.weight(1f)) {
                        AuroraRail(
                            sections = railSections,
                            selectedId = selectedModule,
                            onSelect = {
                                selectedModule = it
                                query = ""
                            },
                            layout = layout,
                            onSectionsChange = { next ->
                                // Written back through the layout, which is what persists it: the
                                // composed list is a projection of the stored sections, so keeping the
                                // edit only in composition would lose it on the next open.
                                layout.setSections(
                                    next.map { section ->
                                        AuroraLayout.Section(
                                            section.id,
                                            section.name,
                                            section.entries.map { it.id }
                                        ).also { it.collapsed = section.collapsed }
                                    }
                                )
                                touchLayout()
                            },
                            look = look,
                            onOpenMenu = { menuTarget = ArrangeTarget.Rail },
                            onOpenSectionMenu = { menuSection = it },
                            // The ENTRY's own scope. The right-click already knows which entry and
                            // which category it landed on, so the menu opens on exactly that entry
                            // instead of on the whole rail.
                            onOpenEntryMenu = { moduleId, sectionId ->
                                menuModule = moduleId
                                menuSection = sectionId
                                menuTarget = ArrangeTarget.Entry
                            },
                            // Committed on release rather than per frame: a drag writes dozens of times
                            // a second and a file write per frame would turn the drag into a stutter.
                            onMoved = { touchLayout() },
                            focus = focus
                        )
                    }
                    // The footer names whose sword this is. Fetched here rather than passed in, because
                    // the window is opened by a key press with no reference to the player.
                    AuroraRailFooter(
                        playerName = net.minecraft.client.Minecraft.getInstance().player?.gameProfile?.name,
                        itemName = "神器之刃"
                    )
                }
            },
            onOpenMenu = { menuTarget = ArrangeTarget.Shell },
            content = { stage ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .trackParallax(drag)
                        // A right-click on the content area (not claimed by a row or a panel) opens
                        // the controls' own numbers: the Controls page had no entry point after the
                        // mode button was removed, and a page nothing opens is a dead page.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: continue
                                    if (event.type == PointerEventType.Press &&
                                        event.buttons.isSecondaryPressed &&
                                        !change.isConsumed
                                    ) {
                                        change.consume()
                                        menuTarget = ArrangeTarget.Controls
                                    }
                                }
                            }
                        }
                ) {
                    Column(Modifier.fillMaxSize()) {
                        AuroraContentHeader(
                            icon = iconFor(module.id),
                            title = module.label,
                            subtitle = subtitleFor(module, allRows, holder),
                            query = query,
                            onQuery = { query = it }
                        )
                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                            // Selecting a rail entry shows that feature's settings directly. There is no
                            // intermediate grid: a grid listed a feature's sections, so reaching a
                            // setting took two clicks and the grid told the player nothing the content
                            // area could not.
                            //
                            // A feature is often long -- 时停 is seven sections -- so the sections render
                            // in sequence and the long ones collapse to a row that opens their panel.
                            // See `GodSwordModules.INLINE_ROW_LIMIT`.
                            // (`groups` is hoisted above the shell: the keyboard focus model walks the
                            // same list, so the count it moves by and the rows drawn here are one value.)

                            // A MODULE SWITCH CROSSFADES.
                            //
                            // Switching features is the most frequent action in this window -- it is
                            // what the rail is for -- and it used to swap the whole content area in a
                            // single frame. The incoming rows then staged in one after another, so the
                            // eye saw the previous feature vanish instantly and the next one assemble
                            // afterwards. That reads as a load, not as a move.
                            //
                            // Keyed on the module id, so it fires on a real switch and not on an edit:
                            // `revision` changes on every slider drag, and crossfading there would make
                            // the whole page flicker while dragging.
                            Crossfade(
                                targetState = selectedModule,
                                animationSpec = tween(Motion.dur(Motion.SLOW_MS), easing = Motion.enter),
                                label = "moduleSwap"
                            ) { shownModule ->
                            // The outgoing copy must still draw ITS OWN rows. `groups` and `module`
                            // describe the newly selected one, so while the old frame is fading out
                            // they would be the wrong feature's contents -- and would describe a module
                            // whose rows are not the ones the outgoing frame was laid out with.
                            val contentModule = GodSwordModules.byId(shownModule)
                                ?: GodSwordModules.MODULES.first()
                            val contentGroups = if (shownModule == selectedModule) {
                                groups
                            } else {
                                GodSwordModules.groupsOf(contentModule, rowsFor(contentModule, holder))
                            }

                            // While a query is active the content area shows the cross-module results
                            // rather than the current feature filtered down. The filter was the old
                            // behaviour and it could only ever narrow what was already on screen.
                            //
                            // It does NOT clear the query when a hit is chosen: the player is usually
                            // comparing several, and the row they came for is easier to find with the
                            // filter still applied. Switching the feature is the whole effect.
                            if (query.isNotBlank()) {
                                GodSwordSearchResults(
                                    query = query,
                                    hits = searchHits,
                                    onOpen = { hit ->
                                        selectedModule = hit.moduleId
                                        touchLayout()
                                    }
                                )
                                return@Crossfade
                            }

                            if (contentGroups.isEmpty() && !contentModule.isWindowSettings) {
                                Text(
                                    if (query.isEmpty()) "该模块当前没有可用设置" else "没有匹配「$query」的设置",
                                    fontSize = TypeScale.row,
                                    color = Star.textTertiary,
                                    fontFamily = auroraFont,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            if (contentModule.isWindowSettings) {
                                AuroraSettingsPanel(
                                    layout = layout,
                                    onChanged = { touchLayout() },
                                    onResetLayout = { layout.resetLayout(); touchLayout() }
                                )
                            } else {
                                GodSwordInlinePanel(
                                    groups = contentGroups,
                                    holder = holder,
                                    query = query,
                                    presetSlot = presetSlot,
                                    onPresetSlot = { presetSlot = it },
                                    onPresetLoad = { slot ->
                                        loadPreset(slot, holder)
                                        markDirty()
                                        previewConfig(holder.snapshot().toConfig())
                                    },
                                    onPresetSave = { slot -> savePreset(slot, holder.snapshot()) },
                                    onOpenGroup = { opened ->
                                        if (openPanels.contains(opened.id)) {
                                            layout.raise("panel-${opened.id}")
                                        } else {
                                            openPanels = openPanels + opened.id
                                            layout.raise("panel-${opened.id}")
                                        }
                                        touchLayout()
                                    },
                                    onChanged = {
                                        markDirty()
                                        previewConfig(holder.snapshot().toConfig())
                                    },
                                    // The outgoing half fades without staging: two animations on the
                                    // same pixels read as a stutter, not as a move.
                                    staging = shownModule == selectedModule
                                )
                            }
                            }

                            // The open panels, drawn in depth order (pinned tier first, then the
                            // rest, by ascending depth -- the layout's renumbering keeps the tiers
                            // apart). Drawing in order is what makes `raise` visually meaningful.
                            //
                            // PANELS SURVIVE A MODULE SWITCH. The list holds ids from every module,
                            // and resolving only against the current module's groups used to drop
                            // the rest: they vanished on a rail click, and the stale sweep then
                            // deleted them for good -- which is why "多个浮层" never actually
                            // worked. Resolution now falls back to the panel's OWN module, found
                            // from the id's "module/heading" shape.
                            val allGroups = GodSwordModules.groupsOf(module, rowsFor(module, holder))
                            // Open panels PLUS the ones still playing their dismissal. The exit
                            // animation needs its panel to still exist, so the dismissing ids are
                            // rendered here and removed by the effect further down once the ramp has
                            // run -- dropping them at dismiss time could only ever blink them out.
                            val renderedPanelIds = openPanels + closingPanels.filterNot { it in openPanels }
                            val openOrdered = renderedPanelIds
                                .mapNotNull { id ->
                                    val g = groups.firstOrNull { it.id == id }
                                        // A searched-for group is not in the filtered list, so the
                                        // panel would lose its rows the moment a search started.
                                        ?: allGroups.firstOrNull { it.id == id }
                                        ?: groupOfPanelId(id, holder)
                                    g?.let { id to it }
                                }
                                .sortedBy { (id, _) ->
                                    layout.boxFor("panel-$id", AuroraLayout.CardBox(0.1f, 0.05f, 0.78f, 0.88f, 40)).depth
                                }
                            openOrdered.forEach { (panelId, group) ->
                                val fallback = AuroraLayout.CardBox(0.10f, 0.05f, 0.78f, 0.88f, 40)
                                val panelBox = layout.boxFor("panel-$panelId", fallback)
                                AuroraFloatingPanel(
                                    box = panelBox,
                                    stageWidth = maxWidth.value,
                                    stageHeight = maxHeight.value,
                                    layout = layout,
                                    drag = drag,
                                    cursor = cursor,
                                    title = group.heading,
                                    subtitle = module.label,
                                    onRaise = {
                                        layout.raise("panel-$panelId")
                                        touchLayout()
                                    },
                                    onGeometryChange = { touchLayout() },
                                    onClose = { dismissPanel(panelId) },
                                    onOpenMenu = { menuTarget = ArrangeTarget.Panel },
                                    dismissing = panelId in closingPanels
                                ) {
                                    GodSwordPanel(
                                        rows = group.rows,
                                        holder = holder,
                                        presetSlot = presetSlot,
                                        onPresetSlot = { presetSlot = it },
                                        onPresetLoad = { slot ->
                                            loadPreset(slot, holder)
                                            markDirty()
                                            previewConfig(holder.snapshot().toConfig())
                                        },
                                        onPresetSave = { slot -> savePreset(slot, holder.snapshot()) },
                                        onChanged = {
                                            markDirty()
                                            previewConfig(holder.snapshot().toConfig())
                                        },
                                        onReset = {
                                            // Reset reads the defaults through the same binding the
                                            // rows use, rather than storing per-row constants that
                                            // would drift from the config class.
                                            resetRowsToDefaults(holder, group.rows)
                                            markDirty()
                                            previewConfig(holder.snapshot().toConfig())
                                        }
                                    )
                                }
                            }
                            // A group can vanish under an open panel: a search narrows, or the render
                            // style gates its rows off. The panel is then stale, and is dropped from
                            // the open list rather than drawn against a group that no longer exists.
                            //
                            // STALE MEANS GONE FROM ITS OWN MODULE, not "not in the current one":
                            // a panel belonging to another module is still perfectly valid, and
                            // sweeping it here is what made a second panel impossible to keep open.
                            // A panel whose group no longer exists is dropped without an animation:
                            // there is nothing left to animate, and waiting would leave a ghost on
                            // screen for a feature the player has deleted.
                            val stale = openPanels.filterNot { id ->
                                id in allGroups.map { it.id } || groupOfPanelId(id, holder) != null
                            }
                            if (stale.isNotEmpty()) {
                                openPanels = openPanels - stale
                            }

                            // The exit ramp's own cleanup.
                            //
                            // CAPTURED BEFORE THE DELAY, not read after it. The effect is keyed on
                            // the dismissing set, so a second panel starting to close cancels and
                            // restarts this block -- and a body that removed "everything currently
                            // dismissing" would then finish the NEW panel early, cutting its animation
                            // short. Holding the ids this run is responsible for means a later
                            // dismissal waits its own full ramp.
                            LaunchedEffect(closingPanels) {
                                val settled = closingPanels
                                if (settled.isNotEmpty()) {
                                    delay((Motion.dur(Motion.WINDOW_MS) + 30).toLong())
                                    closingPanels = closingPanels - settled
                                }
                            }
                        }
                    }
                }
            }
        )

        // The menu is drawn over the shell so it can sit beside whichever element opened it, and
        // after it in the composition so it paints on top.
        // A section menu and an element menu are the same surface with a different subject, so they
        // share one slot rather than each owning a flag: two flags could both be set and the window
        // would draw two menus over each other.
        val openSection = menuSection
        val openMenu = menuTarget
        if (openMenu != null || openSection != null) {
            // The menu is a WINDOW: it takes a box from the layout (remembered across opens, like
            // every other surface) and the measured stage, so it can be moved, resized and
            // maximized with the same gestures. Its box is seeded to a comfortable default that
            // leaves room for the longest page -- the old hard-coded 400x560dp cut pages off.
            val menuFallback = AuroraLayout.CardBox(0.10f, 0.08f, 0.46f, 0.80f, 90)
            ArrangeMenu(
                target = if (openSection != null) ArrangeTarget.Section else openMenu!!,
                layout = layout,
                look = look,
                box = layout.boxFor("menu", menuFallback),
                stage = stageSize,
                cursor = cursor,
                sectionId = openSection,
                moduleId = menuModule,
                onChanged = { touchLayout() },
                onClose = { menuTarget = null; menuSection = null; menuModule = null }
            )
        }
        }
        }

        if (confirming) {
            AuroraConfirmDialog(
                onSaveClose = {
                    saveConfig()
                    dirty = false
                    requestClose()
                },
                onDiscardClose = {
                    discardPreview()
                    requestClose()
                },
                onKeepEditing = { confirming = false }
            )
        }
    }

    /**
     * The rows a module shows, with gated rows filtered by the current style.
     *
     * <p>The filter reads `renderStyle` here rather than inside the panel, so the panel stays a pure
     * function of the rows it is given -- and so the "which style is active" question lives in one
     * place instead of being answered again by every gated control.
     */
    /**
     * The rows a module shows, with gated rows filtered by the current style.
     *
     * <p>Each feature's rows are selected by which **table** they live in, and the dispatch is on the
     * table's own key rather than on the module's rail id. Keying on the rail id is what produced the
     * empty window this replaced: merging 时停's four rail entries into one changed the id, and nothing
     * told this function, so every one of 时停's rows fell through to the empty default while the rail
     * still listed the feature.
     *
     * <p>Keying on the table removes that whole class of failure -- a rail id can be renamed for a
     * presentation reason at any time, and rows must not depend on what the rail happens to call them.
     *
     * <p>The filter reads `renderStyle` here rather than inside a panel, so a panel stays a pure
     * function of the rows it is given, and the "which style is active" question is answered once.
     */
    private fun rowsFor(module: GodSwordModules.Module, holder: V9Holder): List<GodRow> {
        if (module.isWindowSettings) return emptyList()
        val rows = GodSwordRows.rowsOf(module.table)
        val style = GodSwordBinding.readIndex("renderStyle", holder)
        return rows.filter { it.gate < 0 || it.gate == style }
    }

    /**
     * The group a panel id belongs to, found from the id's own `module/heading` shape.
     *
     * <p>A panel id is built by [GodSwordModules.Group.id], which is `"$moduleId/$heading"`. That
     * makes a panel self-describing: an open panel can be re-resolved after a rail click has
     * switched the content area to a different module, without keeping a parallel map of "which
     * module opened this". Panels belonging to other modules then stay open -- which is the point
     * of allowing several at once.
     *
     * <p>Returns null when the module no longer exists or the heading is gone from it, which is the
     * only case that legitimately makes a panel stale.
     */
    private fun groupOfPanelId(id: String, holder: V9Holder): GodSwordModules.Group? {
        val slash = id.indexOf('/')
        if (slash <= 0) return null
        val module = GodSwordModules.byId(id.substring(0, slash)) ?: return null
        val heading = id.substring(slash + 1)
        return GodSwordModules.groupsOf(module, rowsFor(module, holder))
            .firstOrNull { it.heading == heading }
    }

    /**
     * Restores a module's rows to the defaults in a fresh config.
     *
     * <p>`v9StateOf(YuanGodSwordConfig())` is the default state by construction, so the reset is a
     * copy of one field per row from that state rather than a table of per-row constants. A table
     * would be a second statement of every default and the first thing to go stale when one changes.
     */
    private fun resetRowsToDefaults(holder: V9Holder, rows: List<GodRow>) {
        // A holder over the default state, so the reset reads its defaults through the *same* binding
        // the rows use. Comparing field names by hand would be a second dispatch table that could
        // disagree with the first.
        val defaultHolder = V9Holder(v9StateOf(YuanGodSwordConfig()))
        rows.forEach { godRow ->
            val id = godRow.row.id
            when (godRow.row.type) {
                GodSwordRows.TYPE_SWITCH ->
                    GodSwordBinding.writeBool(id, holder, GodSwordBinding.readBool(id, defaultHolder))
                GodSwordRows.TYPE_SEGMENT ->
                    GodSwordBinding.writeIndex(id, holder, GodSwordBinding.readIndex(id, defaultHolder))
                GodSwordRows.TYPE_SLIDER ->
                    GodSwordBinding.writeFloat(id, holder, GodSwordBinding.readFloat(id, defaultHolder))
                GodSwordRows.TYPE_CHANNEL -> {
                    val field = godRow.colourField
                    val channel = godRow.channel
                    if (field != null && channel != null) {
                        // Reset through the binding rather than to a constant: a black slash core is a
                        // valid default, but so is a white one, and only the config knows which.
                        GodSwordBinding.writeChannel(
                            field,
                            channel,
                            holder,
                            GodSwordBinding.readChannel(field, channel, defaultHolder)
                        )
                    }
                }
            }
        }
    }

    private fun subtitleFor(module: GodSwordModules.Module, rows: List<GodRow>, holder: V9Holder): String {
        if (module.isWindowSettings) return "缩放 · 动效 · 布局"
        val enabled = rows.count {
            it.row.type == GodSwordRows.TYPE_SWITCH && GodSwordBinding.readBool(it.row.id, holder)
        }
        return "$enabled 已启用 / ${rows.size} 项"
    }

    private fun iconFor(id: String): AuroraIcon = when (id) {
        "time-basic", "time-effect", "time-sound", "time-freeze" -> AuroraIcon.Combat
        "domain" -> AuroraIcon.Starlight
        "slash" -> AuroraIcon.Movement
        "render" -> AuroraIcon.Render
        "rewind" -> AuroraIcon.Player
        "preset" -> AuroraIcon.Profile
        "window" -> AuroraIcon.Config
        else -> AuroraIcon.Misc
    }
}

/**
 * The unsaved-changes confirm: save, discard, or keep editing.
 *
 * <p>Three answers rather than two. A live preview means "close" is ambiguous -- the player may have
 * been adjusting a value they liked, or may have been exploring and want none of it -- and guessing
 * wrong either loses work or ships a change the sword keeps.
 */
@Composable
private fun AuroraConfirmDialog(
    onSaveClose: () -> Unit,
    onDiscardClose: () -> Unit,
    onKeepEditing: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize().padding(0.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            Modifier
                .width(320.dp)
                .padding(16.dp)
        ) {
            androidx.compose.material3.Text(
                "有未保存的修改",
                fontSize = TypeScale.panelTitle,
                color = Star.textPrimary,
                fontFamily = auroraFont
            )
            androidx.compose.material3.Text(
                "关闭后这些改动会还原到上次保存的状态。",
                fontSize = TypeScale.hint,
                color = Star.textSecondary,
                fontFamily = auroraFont,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
            ) {
                AuroraButton("继续编辑", onClick = onKeepEditing)
                Spacer(Modifier.width(8.dp))
                AuroraButton("放弃修改", onClick = onDiscardClose)
                Spacer(Modifier.width(8.dp))
                AuroraButton("保存并关闭", onClick = onSaveClose, filled = true)
            }
        }
    }
}

/**
 * How long the editor waits for the values to stop changing before recording an undo step.
 *
 * <p>Long enough that one drag, or one held arrow key, lands as a single step; short enough that a
 * deliberate pause before the next change still reads as two. The same order as the longest entrance
 * animation, so "the interface has settled" means the same thing in both places.
 */
private const val EDIT_SETTLE_MS = 420L
