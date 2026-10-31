package com.yuan.client.aurora.scaffold

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The window's keyboard focus model.
 *
 * <p>WHY THIS EXISTS AT ALL, and why it is hand-built rather than Compose's focus system. The window
 * renders into the game's own framebuffer through a scene that never requests focus: it is a
 * click-to-drag surface, and `Modifier.onKeyEvent` only delivers to a FOCUSED node. A tree full of
 * `focusRequester` calls would therefore receive nothing, which is the same reason the ESC hatch
 * lives on the screen. Keys are routed from the host instead, and this model is what they address.
 *
 * <p>The model is deliberately two numbers rather than a hierarchy of focusable nodes: a REGION and
 * an INDEX within it. The window has four places a keyboard can be -- the rail, the content area,
 * the floating panels, the arrangement menu -- and each is a flat list. A tree would earn its
 * complexity if focus could nest (a control inside a row inside a panel), and it cannot: a keyboard
 * either walks the rail's entries or walks the content's rows, and the panels are raised as a whole.
 *
 * <p>State is Compose state so a change repaints the focus ring and nothing else.
 */
internal class FocusState {

    /** Where the keyboard is. */
    var region: FocusRegion by mutableStateOf(FocusRegion.Rail)

    /** Position within the current region's flat list. */
    var index: Int by mutableStateOf(0)

    /**
     * True once the player has used the keyboard, and false again the moment the mouse moves.
     *
     * <p>The focus ring is shown ONLY while this is true. A ring that is always on implies the
     * keyboard is in charge when the player is using a mouse; showing it on a key press and hiding
     * it on a mouse move is the convention every desktop uses, and it is what keeps the ring from
     * being noise for the majority who never touch the keyboard.
     */
    var keyboardActive: Boolean by mutableStateOf(false)

    /** Moves within the current region, clamped rather than wrapped. */
    fun step(delta: Int, size: Int) {
        if (size <= 0) {
            index = 0
            return
        }
        index = (index + delta).coerceIn(0, size - 1)
        keyboardActive = true
    }

    /**
     * Moves between regions, preserving each region's own index.
     *
     * <p>The order is the window's own left-to-right reading order: the rail, then the content it
     * describes. Panels and the menu are entered by opening them, not by an arrow key, so they sit
     * outside this cycle -- an arrow key that jumped into a floating window would be surprising, and
     * the window has a close control for exactly that.
     */
    fun cycle(delta: Int, railSize: Int, contentSize: Int) {
        val order = listOf(FocusRegion.Rail, FocusRegion.Content)
        val at = order.indexOf(region).coerceAtLeast(0)
        var next = at
        repeat(order.size) {
            next = (next + delta).mod(order.size)
            val size = if (order[next] == FocusRegion.Rail) railSize else contentSize
            if (size > 0) {
                region = order[next]
                index = index.coerceIn(0, (size - 1).coerceAtLeast(0))
                keyboardActive = true
                return
            }
        }
    }

    /** Clamps [index] after the current region's list shrank, so focus cannot point past the end. */
    fun clamp(size: Int) {
        if (size <= 0) {
            index = 0
        } else if (index !in 0 until size) {
            index = (size - 1).coerceAtLeast(0)
        }
    }
}

/** The window's four keyboard regions, in the order the window reads. */
internal enum class FocusRegion {
    /** The left column of feature entries. */
    Rail,

    /** The content area's setting rows. */
    Content,

    /** A raised floating panel. */
    Panel,

    /** The arrangement menu. */
    Menu
}

/**
 * Maps the host's raw GLFW key codes to what the window's navigation understands.
 *
 * <p>A named table rather than scattered `== 265` comparisons at the call site: the host hands over
 * GLFW values, which are not self-describing, and a navigation bug caused by one wrong constant is
 * invisible in review.
 */
internal object AuroraKeys {
    const val UP = 265
    const val DOWN = 264
    const val LEFT = 263
    const val RIGHT = 262
    const val ENTER = 257
    const val SPACE = 32
    const val TAB = 258
    const val ESCAPE = 256
    const val KP_ENTER = 335

    /** `Z` and `Y`, for the undo/redo shortcut. */
    const val Z = 90
    const val Y = 89

    /**
     * GLFW's Ctrl bit.
     *
     * <p>GLFW reports modifiers as a bitfield, and the host forwards it verbatim. The constant lives
     * here beside the key codes so a shortcut and its modifier are read from one place: a mistyped
     * bit silently disables the shortcut rather than failing, which is the hard kind to notice.
     */
    const val MOD_CONTROL = 0x0002

    /** True when Ctrl is held, per the host's modifier bitfield. */
    fun isCtrl(modifiers: Int): Boolean = (modifiers and MOD_CONTROL) != 0

    /** True when the code is one of the four arrows. */
    fun isArrow(code: Int): Boolean = code == UP || code == DOWN || code == LEFT || code == RIGHT

    /** True when the code activates whatever is focused. */
    fun isActivate(code: Int): Boolean = code == ENTER || code == KP_ENTER || code == SPACE
}
