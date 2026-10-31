package com.yuan.client.aurora

/**
 * The rectangles of the panels currently on screen, in root-space pixels, keyed by panel.
 *
 * <p>WHY A BRIDGE EXISTS AT ALL. The liquid-glass pass refracts inside a panel, so it needs that
 * panel's rectangle in SCREEN pixels -- and only the composition knows it. A panel's box is a
 * fraction of the content stage, which the shell then positions, so recovering an absolute rectangle
 * from the fractions would mean re-deriving the whole layout outside the layout. Compose can report
 * the answer directly, so it does, and the renderer reads it.
 *
 * <p>KEYED AND PERSISTENT, NOT CLEARED PER FRAME. `onGloballyPositioned` fires when a node's
 * position CHANGES, not once per frame, so a list that was emptied every frame would be refilled
 * only on the frames where something moved -- and the glass would vanish the moment the window
 * settled. A panel therefore registers under its own id and withdraws with a `DisposableEffect`, so
 * the set describes what is ON SCREEN rather than what happened to be measured this frame.
 *
 * <p>Plain mutable state rather than Compose state: nothing in the composition reads it, only the
 * render pass does, so making it observable would buy a recomposition nobody wants.
 */
internal object GlassRects {

    private val byId = LinkedHashMap<String, FloatArray>()

    /** Entries are {x, y, w, h, cornerRadiusPx}, in root-space pixels. */
    @Synchronized
    fun snapshot(): List<FloatArray> = byId.values.toList()

    @Synchronized
    fun put(id: String, x: Float, y: Float, w: Float, h: Float, radiusPx: Float) {
        // A collapsed panel measures near zero; drawing glass for it would flash a sliver.
        if (w < 2f || h < 2f) {
            byId.remove(id)
            return
        }
        byId[id] = floatArrayOf(x, y, w, h, radiusPx)
    }

    /** Called when a panel leaves the composition, so no glass outlives it. */
    @Synchronized
    fun remove(id: String) {
        byId.remove(id)
    }
}
