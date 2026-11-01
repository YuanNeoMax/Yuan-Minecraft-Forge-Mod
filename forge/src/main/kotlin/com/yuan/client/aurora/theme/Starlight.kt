package com.yuan.client.aurora.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Aurora v4 tokens: a near-black surface where blue marks state.
 *
 * <p>An earlier revision carried state by brightness alone, on the argument that a palette which accents
 * everything stops carrying information. That argument holds, and the reference shows where its limit
 * is: a switch whose on state is only *lighter* reads as "this row is highlighted" rather than "this
 * feature is running", and a slider whose fill is grey reads as a progress meter. So blue is admitted,
 * under a rule narrow enough to keep the original point:
 *
 * <p>**`accent` fills state, and nothing else.** It appears on a toggle's on track and on a slider's
 * fill and knob. It never appears on text, borders, buttons or section labels -- so a blue pixel on
 * screen always means "this control is on or has a value", which is the reading the monochrome version
 * could not support. Everything else stays brightness-stepped.
 *
 * <p>Contrast, against the surface each role sits on: primary #D8D8DC on panel 12.6:1, secondary
 * #8A8A90 6.1:1, tertiary #5E5E66 3.6:1 (10sp and above only), and the on-track #4A90E2 against its
 * white knob 3.1:1 -- enough for a shape edge, which is what a knob boundary is.
 */
internal object Star {

    // ---- grounds, darkest to most raised ----
    /** Behind the shell: never drawn, the blurred world is the backdrop. */
    val void = Color(0xFF060607)
    /** The shell's own surface, translucent so the blur reads through it. */
    val shell = Color(0xFF0A0A0C)
    /** A detail panel's surface, a shade above the shell. */
    val panel = Color(0xFF121215)
    /** Row under the cursor. */
    val hover = Color(0x0DFFFFFF)
    /** Row being pressed. */
    val press = Color(0x14FFFFFF)
    /** A selected rail entry, and a row picked inside a module. */
    val selected = Color(0x1AFFFFFF)
    /** Input wells and the toggle track: darker than the surface they sit on. */
    val well = Color(0xFF08080A)

    // ---- text: brightness is the only state signal, so these steps matter ----
    val textPrimary = Color(0xFFD8D8DC)
    val textSecondary = Color(0xFF8A8A90)
    /** Hints and captions. Never below 10sp: at 3.6:1 this is the floor for small text. */
    val textTertiary = Color(0xFF5E5E66)
    /** A disabled control's label: present but clearly inert. */
    val textDisabled = Color(0xFF3C3C42)

    // ---- state, as brightness ----
    /** ON as a label colour. Also what an enabled row's text uses. */
    val on = Color(0xFFE8E8EA)
    /** OFF. Deliberately not grey-on-grey: an off row must still be legible. */
    val off = Color(0xFF54545A)
    /** The toggle knob in its on state. */
    val knob = Color(0xFFE4E4E8)
    /** ...and in its off state, where it must still be visible against the track. */
    val knobOff = Color(0xFF6A6A72)

    // ---- the accent, and its one job ----
    /**
     * Fills a control's active state: a toggle's on track, a slider's fill and knob.
     *
     * <p>Referenced from exactly those two controls. Any other use is a regression -- see this file's
     * header for why limiting it is what lets a blue pixel mean something.
     */
    /**
     * The accent, and the two shades derived from it.
     *
     * <p>MUTABLE, because the palette is a setting now. Every other value in this object is a
     * constant: the greys are a tuned ladder and changing one without the others breaks the
     * contrast the file's header documents. The accent is the opposite case -- its whole job is
     * identity, and the derived shades are simple arithmetic on it -- so it is the one colour a
     * preset may swap.
     *
     * <p>Plain fields rather than Compose state, for the same reason the arrangement layer is: they
     * are written once when a preset is chosen, and the window already re-reads them because the
     * layout revision bumps. Making them observable here would mean every control reading the
     * palette subscribes individually, for a value that changes a few times a session.
     *
     * <p>Derived rather than stored: a preset that set the three shades independently could
     * produce a tint that does not match its accent, and the tint exists precisely to be the
     * accent at low alpha.
     */
    var accent: Color = Color(0xFF4A90E2)
        private set
    /** A dimmer accent, for a hover on an accent-filled control. */
    var accentDim: Color = Color(0xFF3E7BC4)
        private set
    /** A faint accent wash, for a selected plate that must not compete with a filled control. */
    var accentTint: Color = Color(0x264A90E2)
        private set

    /** The shipped accent, so a preset can name "the default" rather than duplicating its hex. */
    val defaultAccent: Color = Color(0xFF4A90E2)

    /**
     * Installs a new accent, deriving its dim and tint shades.
     *
     * <p>The derivation is fixed here rather than in each preset: dim is the accent at 78 percent
     * brightness (the ratio the shipped pair already implies) and tint is the accent at 15 percent
     * alpha. A preset that dropped to the keyboard-shortcut grey would otherwise also need a
     * matching tint by hand, and the two would drift the first time one was edited.
     */
    fun setAccent(colour: Color) {
        accent = colour
        accentDim = Color(
            red = colour.red * 0.78f,
            green = colour.green * 0.78f,
            blue = colour.blue * 0.78f,
            alpha = 1.0f
        )
        accentTint = Color(
            red = colour.red,
            green = colour.green,
            blue = colour.blue,
            alpha = 0.15f
        )
    }

    /** Restores the shipped accent. */
    fun resetAccent() = setAccent(defaultAccent)
    /** A toggle's off track and a slider's unfilled track. Reads as a groove, not as a state. */
    val accentTrack = Color(0xFF2A2A30)

    // ---- the mark, for identity rather than state ----
    /** The module's own mark in the content header. Not a state colour; never on a control. */
    val starlight = Color(0xFF9FB8E8)
    val starlightDim = Color(0xFF6E82A8)
    val starlightTint = Color(0x1A9FB8E8)

    // ---- lines ----
    /** The standard hairline: group separators, panel edges, row dividers. */
    val hairline = Color(0x14FFFFFF)
    /** A slightly stronger edge, for a panel that must not merge with the one behind it. */
    val hairlineStrong = Color(0x24FFFFFF)
    /** The top light on a raised surface. */
    val topLight = Color(0x0FFFFFFF)

    // ---- overlays and shadows ----
    val scrim = Color(0xB3000000)
    val shadowPanel = Color(0x99000000)
    val scrollbar = Color(0x33FFFFFF)

    // ---- metrics: measured off the reference, not chosen ----
    /** The shell is inset from the screen edge so the world frames it on every side. */
    val shellInset: Dp = 26.dp
    val shellRadius: Dp = 14.dp
    val panelRadius: Dp = 12.dp
    val controlRadius: Dp = 8.dp
    val headerHeight: Dp = 60.dp
    val railWidth: Dp = 168.dp
    val railItemHeight: Dp = 36.dp
    val railGroupGap: Dp = 14.dp
    /** The rail's own top tag and its footer player block. */
    val railLabelHeight: Dp = 30.dp
    val railUserBlockHeight: Dp = 56.dp
    /** A settings row inside a panel. */
    val detailRowHeight: Dp = 34.dp
    /** A section label inside the content area. */
    val sectionHeight: Dp = 26.dp
    /** The small pill toggle: this size is why a settings list stays short. */
    val toggleWidth: Dp = 36.dp
    val toggleHeight: Dp = 20.dp
    /** How narrow the content area may be before its sections drop to a single column. */
    val duplicateColumnMinWidth: Dp = 560.dp
}

/**
 * Type scale. A step smaller than the previous set throughout: the reference's labels are 12sp and
 * its hints 10sp, and matching that is what makes a settings list scannable rather than a scroll.
 */
internal object TypeScale {
    val shellTitle: TextUnit = 14.sp
    val panelTitle: TextUnit = 13.sp
    val state: TextUnit = 11.sp
    /** A section label. Small and dim, because it labels a block rather than being one. */
    val section: TextUnit = 11.sp
    val row: TextUnit = 12.sp
    val hint: TextUnit = 10.sp
    val value: TextUnit = 11.sp
    val button: TextUnit = 11.sp
    val rail: TextUnit = 12.sp
    /** The rail's own top tag, in uppercase: `MODULE`. */
    val railTag: TextUnit = 10.sp
    /** A player's name in the rail's footer block. */
    val railUser: TextUnit = 11.sp
}

/**
 * The CJK family the module uses.
 *
 * <p>Set as a string name rather than a bundled font: Skia resolves it through the platform, which
 * keeps the jar's size unchanged and means the interface follows the player's own font settings.
 */
@OptIn(ExperimentalTextApi::class)
internal val auroraFont: FontFamily = FontFamily("Microsoft YaHei")
