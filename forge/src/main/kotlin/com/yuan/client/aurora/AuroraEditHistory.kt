package com.yuan.client.aurora

import com.yuan.client.gui.V9State

/**
 * Undo/redo for the configuration editor.
 *
 * <p>WHY A NEW CLASS RATHER THAN [com.yuan.client.gui.YuanConfigHistory]. That one is a history of a
 * `CompoundTag` against `YuanConfigCatalog`, and it is the right tool for the older screen: it can
 * clamp a key because the catalogue knows every setting's range. This editor reads and writes a
 * [V9State] directly through [com.yuan.client.gui.V9Holder], and a snapshot of that state is already
 * a value -- a data class with structural equality -- so the whole mechanism reduces to a stack of
 * snapshots. Routing the values through a tag and back would add a serialisation step that can only
 * lose information.
 *
 * <p>THE GRANULARITY IS ONE GESTURE, and that is what [commit]'s caller must arrange. A slider
 * drag changes the state on every frame; committing per frame would fill the stack with hundreds of
 * one-pixel steps and make Ctrl+Z useless. The editor therefore reports the state only after the
 * edits have gone quiet, so a drag lands as a single step -- "put that back" is one press, which is
 * what a player means by undo.
 *
 * <p>A commit that does not change anything is dropped. That is what the structural equality buys:
 * clicking into a field and out again, or dragging a slider back to where it started, leaves the
 * stack untouched instead of adding an entry that appears to do nothing when undone.
 */
internal class AuroraEditHistory(initial: V9State) {

    private val undo = ArrayDeque<V9State>()
    private val redo = ArrayDeque<V9State>()

    /** The last state that was recorded. The baseline a new edit is measured against. */
    private var settled: V9State = initial

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()

    /**
     * Records [now] as a step if it differs from the last recorded state.
     *
     * <p>Called after a quiet period rather than per change -- see the class note on granularity.
     */
    fun commit(now: V9State) {
        if (now == settled) return
        undo.addFirst(settled)
        // Older steps fall off the end: an editor session is not a log, and a stack deep enough to
        // hold a whole session is one nobody will walk back through.
        while (undo.size > LIMIT) undo.removeLast()
        // A fresh edit invalidates the redo branch -- the usual linear-history rule.
        redo.clear()
        settled = now
    }

    /** Steps back, returning the state to restore, or null when there is nothing to undo. */
    fun undo(): V9State? {
        val previous = undo.removeFirstOrNull() ?: return null
        redo.addFirst(settled)
        settled = previous
        return previous
    }

    /** Steps forward again, returning the state to restore, or null when there is nothing to redo. */
    fun redo(): V9State? {
        val next = redo.removeFirstOrNull() ?: return null
        undo.addFirst(settled)
        settled = next
        return next
    }

    /**
     * Forgets the history and re-bases it on [now].
     *
     * <p>For operations that are not edits: loading a preset or applying a reset replaces the whole
     * configuration at once, and the states before it are no longer reachable by stepping backwards
     * one field at a time. Re-basing also serves "save", where the current state becomes the thing
     * later edits are compared to.
     */
    fun rebase(now: V9State) {
        undo.clear()
        redo.clear()
        settled = now
    }

    private companion object {
        /** How many steps are remembered. Deep enough to cover a session's exploration. */
        const val LIMIT = 80
    }
}
