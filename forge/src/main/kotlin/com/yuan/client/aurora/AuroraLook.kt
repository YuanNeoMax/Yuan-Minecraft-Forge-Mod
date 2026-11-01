package com.yuan.client.aurora

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * The arrangement layer's live view over [AuroraLayout.Look].
 *
 * <p>`Look` is a plain Java object outside Compose, so a read of one of its fields would never be
 * re-read when a slider moved it -- the panel would write the value and nothing on screen would change.
 * Every accessor here therefore reads the composition's layout revision first, which is what subscribes
 * the caller; the value itself then comes from the object, which the revision says has changed.
 *
 * <p>Reads are deliberately unconditional. Caching a value in a local would subscribe the reader to
 * nothing, and the first slider drag would then appear to do nothing -- the exact failure this class
 * exists to prevent, and one that would look like a broken control rather than a missed recomposition.
 *
 * <p>Colours and sizes are exposed rather than handed out as raw floats, so a caller cannot mix up a dp
 * and a sp, or write a packed int where a `Color` is expected.
 */

/** The composition's current layout revision, published so [AuroraLook] can subscribe to it. */
internal val LocalLayoutRevision = compositionLocalOf { 0 }

/**
 * The layout the composition is editing.
 *
 * <p>Published alongside the revision because a widget deep in the tree needs both the object (to read
 * its numbers) and a reason to re-read it -- and the revision alone would leave a widget with no way to
 * find the object, which is what would push the layout back through every signature as a parameter.
 */
internal val LocalAuroraLayout = compositionLocalOf<AuroraLayout> {
    error("no Aurora layout provided; wrap the window in ProvideLayoutRevision(layout, revision)")
}

/**
 * Publishes both the layout and its revision for the subtree.
 *
 * <p>Called once at a window's root. A separate local rather than threading the revision as a parameter
 * through every widget: the alternative puts an argument on thirty function signatures to serve the
 * handful that read the arrangement.
 */
@Composable
internal fun ProvideLayoutRevision(
    layout: AuroraLayout,
    revision: Int,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAuroraLayout provides layout,
        LocalLayoutRevision provides revision
    ) { content() }
}

/** Reads of [AuroraLayout.Look] that stay live as the arrangement panel edits them. */
internal class AuroraLook(private val layout: AuroraLayout) {

    // The read that subscribes this caller. Its value is unused: what matters is that a recomposition
    // is requested when it changes -- so it has to be called from a composable context, which is why
    // every accessor below is a `@Composable` getter rather than a property of a plain class.
    @Composable
    private fun subscribe() {
        @Suppress("UNUSED_EXPRESSION")
        LocalLayoutRevision.current
    }

    @Composable
    private fun look(): AuroraLayout.Look {
        subscribe()
        return layout.look
    }

    // ---- rail ----
    val railWidth: Dp @Composable get() = look().railWidth.dp
    val railItemHeight: Dp @Composable get() = look().railItemHeight.dp
    val railItemCorner: Dp @Composable get() = look().railItemCorner.dp
    val railItemPadding: Dp @Composable get() = look().railItemPadding.dp
    val railMarkerWidth: Dp @Composable get() = look().railMarkerWidth.dp
    val railFontSize: TextUnit @Composable get() = look().railFontSize.sp
    val railMarkerColor: Color @Composable get() = Color(look().railMarkerColor)
    val railTextColor: Color @Composable get() = Color(look().railTextColor)
    val railTextSelected: Color @Composable get() = Color(look().railTextSelected)
    val railSelectedPlate: Color @Composable get() = Color(look().railSelectedPlate)
    val railGroupColor: Color @Composable get() = Color(look().railGroupColor)
    val railOffsetX: Dp @Composable get() = look().railOffsetX.dp
    val railOffsetY: Dp @Composable get() = look().railOffsetY.dp

    // ---- panel ----
    val panelCorner: Dp @Composable get() = look().panelCorner.dp
    /**
     * The panel surface.
     *
     * <p>IN LIQUID-GLASS MODE THIS IS FULLY TRANSPARENT, and that is not a shortcut for the glass --
     * it is the only way the glass can be seen. The refraction is drawn into the framebuffer BEFORE
     * the composition (see the screen's drawLiquidGlass), so a panel that also painted its own
     * background would cover it with a flat pane and the entire pass would be invisible work.
     *
     * <p>The panel is still a surface in that mode: its border, its header and every row paint over
     * the glass, which together with the refraction and its lit rim is what makes it read as a pane
     * of glass rather than as a hole in the window.
     *
     * <p>FROSTED keeps the translucent fill, because there the flat pane IS the material.
     */
    val panelBg: Color @Composable get() =
        if ((look().material ?: AuroraLayout.MaterialMode.FROSTED) == AuroraLayout.MaterialMode.LIQUID_GLASS) {
            Color.Transparent
        } else {
            glassAlpha(look().panelBg, (look().glass ?: AuroraLayout.GlassLevel.MEDIUM).panelAlpha)
        }
    val panelBorderColor: Color @Composable get() = Color(look().panelBorderColor)
    val panelBorderWidth: Dp @Composable get() = look().panelBorderWidth.dp
    val panelHeaderHeight: Dp @Composable get() = look().panelHeaderHeight.dp
    val rowPadding: Dp @Composable get() = look().rowPadding.dp
    val rowHeight: Dp @Composable get() = look().rowHeight.dp
    val rowFontSize: TextUnit @Composable get() = look().rowFontSize.sp
    val rowLabelColor: Color @Composable get() = Color(look().rowLabelColor)
    val hintColor: Color @Composable get() = Color(look().hintColor)

    // ---- shell ----
    val shellInset: Dp @Composable get() = look().shellInset.dp
    val shellCorner: Dp @Composable get() = look().shellCorner.dp
    val shellBgTop: Color @Composable get() = glassAlpha(look().shellBgTop, (look().glass ?: AuroraLayout.GlassLevel.MEDIUM).shellAlpha)
    val shellBgBottom: Color @Composable get() = glassAlpha(look().shellBgBottom, (look().glass ?: AuroraLayout.GlassLevel.MEDIUM).shellAlpha)
    val shellBorderColor: Color @Composable get() = Color(look().shellBorderColor)
    val shellBorderWidth: Dp @Composable get() = look().shellBorderWidth.dp
    val headerHeight: Dp @Composable get() = look().headerHeight.dp

    // ---- controls ----
    val controlCorner: Dp @Composable get() = look().controlCorner.dp
    val toggleWidth: Dp @Composable get() = look().toggleWidth.dp
    val toggleHeight: Dp @Composable get() = look().toggleHeight.dp
    val accent: Color @Composable get() = Color(look().accent)

    /**
     * The stored accent preset colour, packed. Zero means "the shipped one".
     *
     * <p>Read through here rather than off `Look` directly, because every other value in the palette
     * has an accessor and this one is the exception only in WHERE it ends up: a preset writes it
     * into [Star] rather than into a composable parameter. The accessor still exists so the palette
     * has one shape, and so the guard that checks every field is reachable keeps working.
     */
    val accentColor: Int @Composable get() = look().accentColor
    val trackColor: Color @Composable get() = Color(look().trackColor)
    val accentDim: Color @Composable get() = Color(look().accentDim)
}

/**
 * A packed surface colour with its alpha REPLACED by the glass preset's.
 *
 * <p>The preset owns opacity; the stored colour owns hue. Keeping them apart is what lets one choice
 * ("how glassy") be a single control while the palette stays a separate one -- and it is why the
 * stored `shellBgTop`/`panelBg` values keep their old, near-opaque alphas: they are read as RGB here
 * and their alpha is discarded. An existing `aurora.json` therefore renders correctly under every
 * preset without being migrated.
 *
 * <p>Only these surfaces are affected. Rows, wells, borders and text keep the alphas in the palette:
 * those are not glass, they are marks ON the glass, and fading them would take away the contrast the
 * palette was tuned to provide.
 */
private fun glassAlpha(packed: Int, alpha: Float): Color {
    val a = (alpha.coerceIn(0f, 1f) * 255f).roundToInt() and 0xFF
    return Color((packed and 0x00FFFFFF) or (a shl 24))
}
