package com.yuan.client.aurora

/**
 * The rectangles of the panels currently on screen, in root-space pixels.
 *
 * <p>WHY A BRIDGE EXISTS AT ALL. The liquid-glass pass refracts inside a panel, so it needs that
 * panel's rectangle in SCREEN pixels -- and only the composition knows it. A panel's box is a
 * fraction of the content stage, which the shell then positions, so recovering an absolute rectangle
 * from the fractions would mean re-deriving the whole layout outside the layout. Compose can report
 * the answer directly, so it does, and the renderer reads it.
 *
 * <p>REBUILT EVERY FRAME, not accumulated. Each composition clears the list and every live panel
 * registers itself again; an entry that outlived its panel would leave a piece of glass on the
 * screen with nothing behind it, and a stale rectangle from a resized panel would refract the wrong
 * part of the world.
 *
 * <p>Plain mutable state rather than Compose state: nothing in the composition reads it, only the
 * render pass does, so making it observable would buy a recomposition nobody wants.
 */
internal object GlassRects {

    private val current = ArrayList<FloatArray>(4)

    /** Entries are {x, y, w, h, cornerRadiusPx}. */
    @Synchronized
    fun snapshot(): List<FloatArray> = current.toList()

    @Synchronized
    fun clear() {
        current.clear()
    }

    @Synchronized
    fun add(x: Float, y: Float, w: Float, h: Float, radiusPx: Float) {
        if (w < 2f || h < 2f) return
        current.add(floatArrayOf(x, y, w, h, radiusPx))
    }
}
