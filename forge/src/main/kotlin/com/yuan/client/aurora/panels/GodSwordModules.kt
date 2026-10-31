package com.yuan.client.aurora.panels

import com.yuan.client.aurora.AuroraLayout
import com.yuan.client.gui.V9Row
import com.yuan.client.gui.v9Sections
import java.util.LinkedHashMap

/**
 * The god sword's real configuration, described as Aurora modules.
 *
 * <p>A thin descriptor over the existing config model rather than a parallel one. The old screen
 * already owns the round trip -- `v9StateOf(config)` builds a `V9State` from the sword's NBT and
 * `V9State.toConfig()` turns it back -- and reusing both means the new interface inherits the write,
 * the live preview, the preset list and the server sync for free. A second model would have to
 * re-implement all four, and the first one it got subtly wrong would corrupt somebody's saved sword.
 *
 * <p>The table below is the one new thing: it says which of the config's sections become rail entries
 * and which rows live under each. It is declarative on purpose. A hand-written panel per section would
 * mean thirty composables that each read their own field, and adding a field would then mean touching
 * four files -- here it is one line.
 *
 * <p>Rows are taken from `v9Sections` by id rather than re-declared, so a label or a hint is written
 * once and the two interfaces cannot disagree about what a setting is called.
 */
internal object GodSwordModules {

    /**
     * A rail entry: one feature, backed by exactly one row table.
     *
     * <p>Backed by a **table** rather than by a list of section names. Two features can legitimately
     * label a group the same thing -- 时停 and 斩击 both group rows under 基础 -- so a module that named
     * its sections would pull the other feature's rows in with them. A table is the unit a feature's
     * rows actually live in, and naming it makes that collision impossible rather than something to
     * watch for.
     */
    internal data class Module(
        val id: String,
        val label: String,
        /** The row table this feature renders. */
        val table: GodSwordRows.Table,
        /** Whether this module's content is the window's own layout rather than the sword's config. */
        val isWindowSettings: Boolean = false
    )

    /**
     * The rail, in declaration order.
     *
     * <p>One entry per feature, not per section. An earlier revision split 时停 across four entries
     * (`时停 · 基础`, `时停 · 效果`, ...) because the older screen had done the same with its sub-tabs;
     * that made the rail a list of *sections* pretending to be a list of features, and a player
     * looking for 时停 had to guess which quarter of it a setting lived in. A feature is one entry, and
     * its length is handled inside the content area -- see [GodSwordRows.needsPopup].
     *
     * <p>界面设置 is not a config module at all: it is the window's own scale and motion controls. It
     * rides in the same rail because it needs to be reachable from every module, and a settings window
     * whose own settings live somewhere else is a settings window with a hidden page.
     */
    internal val MODULES: List<Module> = listOf(
        Module("timestop", "时停", GodSwordRows.Table.TIME_STOP),
        Module("domain", "领域", GodSwordRows.Table.DOMAIN),
        Module("slash", "斩击", GodSwordRows.Table.SLASH),
        Module("render", "武器渲染", GodSwordRows.Table.RENDER),
        Module("rewind", "回溯", GodSwordRows.Table.REWIND),
        Module("preset", "预设", GodSwordRows.Table.PRESET),
        Module("window", "界面设置", GodSwordRows.Table.WINDOW, isWindowSettings = true)
    )

    /**
     * The rail's groups: the sword's own features first, then the housekeeping.
     *
     * <p>Splitting them keeps the settings a player changes often away from the ones they visit once.
     */
    internal val GROUPS: Map<String, String> = mapOf(
        "timestop" to "时停",
        "domain" to "术式",
        "slash" to "术式",
        "render" to "术式",
        "rewind" to "术式",
        "preset" to "客户端",
        "window" to "客户端"
    )

    /**
     * The rail's declared sections, used until a player arranges their own.
     *
     * <p>Matches the group map, and derived from it rather than written twice: the groups a player sees on
     * a first run are the declaration's, and a second list would let the two drift apart.
     *
     * <p>Ids are the group names, which makes the stored arrangement readable in the config file -- a
     * section called `术式` in the file is the section labelled `术式` on screen.
     */
    internal fun declaredSections(): List<AuroraLayout.Section> {
        val order = LinkedHashMap<String, MutableList<String>>()
        for (module in MODULES) {
            val group = GROUPS[module.id] ?: "模块"
            order.getOrPut(group) { mutableListOf() }.add(module.id)
        }
        return order.map { (name, ids) ->
            AuroraLayout.Section(name, name, ids)
        }
    }

    /**
     * The rail entry a row id belongs to, or null for an unknown id.
     *
     * <p>Answered from the tables rather than from a section list: a feature's rows are exactly
     * [GodSwordRows.rowsOf] of its table, so this and the renderer cannot disagree.
     */
    internal fun moduleFor(rowId: String): Module? =
        MODULES.firstOrNull { module -> GodSwordRows.rowsOf(module.table).any { it.row.id == rowId } }

    internal fun byId(id: String): Module? = MODULES.firstOrNull { it.id == id }

    /**
     * One cell of the content grid: a named group of rows inside a module.
     *
     * <p>This is the third level the reference uses and the one the previous design was missing. A rail
     * entry selected a module, and then the whole module's settings opened as one long panel -- so 回溯
     * put thirty-odd rows in a single scroll with no way to reach the group you wanted directly. Here a
     * module is a set of groups, the grid lists them, and a group is what opens a panel.
     *
     * <p>Both fields come from the row tables, so a group cannot name rows that do not exist.
     */
    internal data class Group(
        val id: String,
        val heading: String,
        val rows: List<GodRow>
    )

    /**
     * The groups a module's rows fall into, in the order the rows declare them.
     *
     * <p>Derived by walking the rows and starting a new group whenever the section changes, rather than
     * from a second hand-written list. A separate table would be the second place a row's group is
     * stated, and the first thing to disagree with the rows themselves.
     *
     * <p>An empty group is dropped: a module whose rows are all gated off by the current render style
     * would otherwise offer cells that open empty panels.
     */
    internal fun groupsOf(module: Module, rows: List<GodRow>): List<Group> {
        if (module.isWindowSettings || rows.isEmpty()) return emptyList()
        val result = ArrayList<Group>()
        var currentSection: String? = null
        var current = ArrayList<GodRow>()
        rows.forEach { row ->
            if (row.row.section != currentSection) {
                if (current.isNotEmpty() && currentSection != null) {
                    result.add(Group(groupId(module.id, currentSection), currentSection, current))
                }
                current = ArrayList()
                currentSection = row.row.section
            }
            current.add(row)
        }
        if (current.isNotEmpty() && currentSection != null) {
            result.add(Group(groupId(module.id, currentSection), currentSection, current))
        }
        return result
    }

    /**
     * Whether a section opens its own panel rather than rendering in the column.
     *
     * <p>One reason, and it is about the control rather than the length: a section holding a row whose
     * control is not a single value -- the ball colour's eight swatches and three channel sliders, the
     * preset slots. Those cannot sit on a settings line, so they get a surface that can hold them.
     *
     * <p>A long section does **not** qualify. Length is what the two-column layout and the scroll solve,
     * and an earlier revision's row-count threshold was a workaround for a single column: it hid rows
     * behind a disclosure instead of giving them somewhere to be. See [Group] and GodSwordInlinePanel.
     */
    internal fun groupNeedsPopup(group: Group): Boolean = group.rows.any { it.wantsPanel }

    /**
     * A stable id for a group, used to key its panel's saved geometry.
     *
     * <p>Built from the module and the heading rather than an index, so reordering or adding a group
     * does not shift every later panel to a different saved position.
     */
    private fun groupId(moduleId: String, heading: String): String = "$moduleId/$heading"
}
