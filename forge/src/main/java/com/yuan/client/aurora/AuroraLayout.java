package com.yuan.client.aurora;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The Aurora window's layout, scale and motion preferences, persisted beside the game's config.
 *
 * <p>Deliberately not part of the sword's NBT. Where a card sits on screen and how large the text is
 * are properties of the player, not of one weapon -- storing them on the item would mean every newly
 * crafted sword starts with a scrambled window, and a player who reorganised their layout would lose
 * it on the next one. The file is global on purpose.
 *
 * <p>Geometry is stored in fractions of the stage rather than pixels. A layout written on a 1080p
 * monitor has to come back correct on a 4K one, and pixel coordinates would put every card in the
 * top-left corner of a larger screen with everything overlapping.
 *
 * <p>Loading is defensive throughout. This is a hand-editable JSON file, so a wrong type or a
 * negative size must produce a loadable default, never an exception at screen-open time -- a crash
 * on open is unrecoverable for the user, who cannot reach the settings page to fix it.
 */
public final class AuroraLayout {

    /** Continuous UI scale on top of the game's GUI scale, not instead of it. */
    public static final float MIN_SCALE = 0.75f;
    public static final float MAX_SCALE = 2.0f;

    /** Below this a card cannot be dragged by its own handle, so it is clamped rather than honoured. */
    public static final float MIN_CARD_W = 0.18f;
    public static final float MIN_CARD_H = 0.12f;

    /** One card's box on the stage, in fractions of the stage's width and height. */
    public static final class CardBox {
        /** Top-left corner, 0..1. Clamped so a card can never be dragged entirely off-stage. */
        public float x = 0.05f;
        public float y = 0.10f;
        /** Size, 0..1. */
        public float w = 0.40f;
        public float h = 0.40f;
        /** Collapsed cards show their title bar only; the box keeps its size so unfolding is exact. */
        public boolean collapsed = false;
        /** Draw order; higher is nearer to the viewer, and the nearest takes the mouse. */
        public int depth = 0;
        /**
         * Whether a floating panel stays above every unpinned one.
         *
         * <p>Depth answers "which of two equal citizens is nearer"; pinned answers "which
         * citizen outranks the others". A pinned panel keeps its depth within the pinned
         * tier, and {@link #raise} renumbers so the pinned tier always sits above the
         * unpinned one -- a pin a click could silently undo would not be a pin.
         */
        public boolean pinned = false;

        public CardBox() {
        }

        public CardBox(float x, float y, float w, float h, int depth) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.depth = depth;
        }

        /**
         * A copy of this box, including pinned.
         *
         * <p>Public because the Kotlin scaffold saves a restore rectangle by copying the live box
         * (maximize) and the gesture frame's snap path does the same.
         */
        public CardBox copy() {
            CardBox c = new CardBox(x, y, w, h, depth);
            c.collapsed = collapsed;
            c.pinned = pinned;
            return c;
        }

        /**
         * Brings the box back inside the legal range.
         *
         * <p>Public because the drag handlers live in the Kotlin scaffold and call it after every
         * delta: a card dragged past the stage edge has to be pulled back on the frame it happens,
         * not at the next layout pass, or the card visibly leaves the screen before returning.
         */
        public void clampBox() {
            if (!finite(x, y, w, h)) {
                x = 0.05f;
                y = 0.10f;
                w = 0.40f;
                h = 0.40f;
            }
            w = clamp(w, MIN_CARD_W, 1f);
            h = clamp(h, MIN_CARD_H, 1f);
            // Fully on stage, not "a sliver grabbable". An overlay whose header can sit half off
            // screen has controls that cannot be reached, and that reads as "cannot be moved".
            // Nothing here may leave the stage edge, which is also what makes every position the
            // gesture frame can produce reachable by the gesture frame again.
            x = clamp(x, 0f, 1f - w);
            y = clamp(y, 0f, 1f - h);
            if (depth < 0) depth = 0;
        }
    }

    /** How much of the animation table to use. 0 disables motion for motion-sensitivity reasons. */
    public enum MotionLevel {
        /** No transitions at all. Values change on the frame they are set. */
        OFF,
        /** Only state feedback (hover, press, selection). No entrances, no parallax, no pulse. */
        CALM,
        /** The whole table, including staggered entrances and the star core's breathing. */
        FULL;

        public static MotionLevel from(int ordinal) {
            MotionLevel[] values = values();
            return values[clamp(ordinal, 0, values.length - 1)];
        }
    }

    // ---- scale and motion ----
    /** Continuous scale on top of the game's GUI scale. Ignored while {@link #autoScale} is on. */
    public float scale = 1.0f;
    /** Pick a scale from the window's aspect ratio each open instead of using {@link #scale}. */
    public boolean autoScale = true;
    public MotionLevel motion = MotionLevel.FULL;
    /** Background blur strength, 0..1. Zero leaves the world sharp behind the panels. */
    public float backdropBlur = 1.0f;

    /** Depth parallax strength, 0..1. Zero pins every layer. */
    public float parallax = 1.0f;

    // ---- the stage ----
    /**
     * The star core's position.
     *
     * <p>Retired with the stage, and kept only so an existing `aurora.json` still loads -- the star
     * field moved into the content area's identity mark and has no position of its own. Nothing reads
     * these, which is why they are documented as retired rather than left looking live.
     */
    public float coreX = 0.5f;
    public float coreY = 0.42f;

    // ---- window gestures ----
    /**
     * Whether the window gestures (drag by title bar, edge resize, edge snapping) are armed.
     *
     * <p>The arrangement MODE this field replaces gated the same gestures behind a persisted flag,
     * and that shape failed twice: the grips existed only inside the mode so they were invisible
     * and undiscoverable, and a flag that outlived the window trapped navigation. The gestures are
     * therefore resident, and what this switch answers is the different question a player actually
     * asks: "stop my window from moving when I drag". The right-click customization menus are NOT
     * gated -- they are how the window is reached when the gestures are off.
     */
    public boolean gesturesEnabled = true;

    /**
     * Whether dragging the window against a screen edge snaps it (half, quarter, maximize).
     *
     * <p>Separate from {@link #gesturesEnabled} because the two annoy differently: a player who
     * wants the window draggable but never half-snapped has no use for a switch that removes both.
     */
    public boolean aeroSnapEnabled = true;

    /**
     * Whether the main window is maximized (fills the stage, square corners, no resize grips).
     *
     * <p>Stored rather than derived because it must survive a reopen, and derived-from-what has no
     * answer: a window dragged to exactly full size is not maximized, and treating it as such would
     * lose its restore rectangle.
     */
    public boolean windowMaximized = false;

    /**
     * The rectangle the window returns to when un-maximized, or null while it is not maximized.
     *
     * <p>Saved at the moment of maximizing rather than remembered from the last drag: a player who
     * maximizes, drags (which restores under the pointer), and re-maximizes expects the second
     * maximize to remember the window as it just was, not as it was two steps ago.
     */
    public CardBox windowRestore = null;

    /** Snap dragged elements to this fraction of the stage. Zero disables snapping. */
    public float snap = 0.02f;

    // ---- content arrangement ----
    /** Card id to its box. Absent ids get the default from the panel's own declaration. */
    public Map<String, CardBox> cards = new LinkedHashMap<>();
    /** Nav entry order. Ids the file does not mention are appended in declaration order. */
    public List<String> navOrder = new ArrayList<>();
    /**
     * The whole window's box, in fractions of the screen. Null until the player resizes it.
     *
     * <p>Null rather than a default because "never resized" and "resized to the default" must be
     * distinguishable: the window's inset is a screen-relative value, and a stored default would stop
     * tracking a change to that inset.
     */
    public CardBox window = null;

    /**
     * The rail's sections: the groups the navigation is divided into.
     *
     * <p>Replaces a flat id list, and the reason is the requested shape: the rail is not one column of
     * entries, it is several groups a player assembles. `时停` alone, then `领域` and `斩击` together,
     * then whatever is added later -- each group is a slot with its own name, its own contents and its
     * own collapsed state, and entries move between groups rather than only up and down one list.
     *
     * <p>An empty list means "not yet arranged", and {@link #sectionsOrDefault} then hands back the
     * declaration order. Distinguishing empty from "arranged to nothing" matters because a player who
     * deletes every module would otherwise lose the groups themselves.
     */
    public List<Section> sections = new ArrayList<>();

    /** One rail section: a named slot holding module ids. */
    public static final class Section {
        /** Stable identity for the stored order and for the config menu. */
        public String id = "";
        /** What the rail prints above the entries. */
        public String name = "";
        /** Module ids in this section, in the player's order. */
        public List<String> modules = new ArrayList<>();
        /** Whether the entries are hidden while the name stays. */
        public boolean collapsed = false;

        public Section() {
        }

        public Section(String id, String name, List<String> modules) {
            this.id = id;
            this.name = name;
            this.modules = new ArrayList<>(modules);
        }

        Section copy() {
            return new Section(id, name, new ArrayList<>(modules));
        }
    }

    /**
     * The sections to draw: the player's arrangement, or the declared one when they have not arranged.
     *
     * <p>Ids the arrangement does not mention are appended to the last section rather than dropped: a
     * module added in a later version must appear, and appearing at the end of the rail is the only place
     * that is obviously right without moving everything the player already placed.
     */
    public List<Section> sectionsOrDefault(List<Section> declared) {
        if (sections == null || sections.isEmpty()) {
            List<Section> fresh = new ArrayList<>();
            for (Section s : declared) fresh.add(s.copy());
            return fresh;
        }
        List<Section> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Section s : sections) {
            if (s == null || s.id == null || s.id.isEmpty()) continue;
            Section copy = s.copy();
            copy.modules.removeIf(id -> !seen.add(id));
            result.add(copy);
        }
        for (Section declaredSection : declared) {
            for (String id : declaredSection.modules) {
                if (seen.contains(id)) continue;
                seen.add(id);
                if (result.isEmpty()) result.add(new Section("default", "模块", new ArrayList<>()));
                result.get(result.size() - 1).modules.add(id);
            }
        }
        return result;
    }

    public void setSections(List<Section> order) {
        sections = new ArrayList<>();
        for (Section s : order) sections.add(s.copy());
    }

    /**
     * Makes every section hold the same modules as the first one.
     *
     * <p>The "一键统一模块内容" action. Uniformity of CONTENT, not of appearance: each section keeps its
     * own name and its own layout values, so a player can make every slot offer the same features while
     * still dressing them differently.
     */
    public void unifySections() {
        if (sections == null || sections.size() < 2) return;
        List<String> shared = new ArrayList<>(sections.get(0).modules);
        for (int i = 1; i < sections.size(); i++) {
            sections.get(i).modules = new ArrayList<>(shared);
        }
    }

    /**
     * The adjustable appearance of the window's parts, grouped by what each field dresses.
     *
     * <p>One object rather than thirty loose fields, so a preset can copy it whole and the arrangement
     * panel can iterate it. Every field is a number or a packed colour: the point of the arrangement
     * layer is that a player can move and repaint the window without a code change, and a field that
     * cannot be expressed as one of those two does not belong here.
     */
    public Look look = new Look();

    /**
     * Per-entry overrides, keyed by module id.
     *
     * <p>The rail is configured at three scopes -- the whole rail, one category, one entry -- and
     * the scopes CASCADE: an entry's field falls back to its category's, which falls back to the
     * rail's. That is why every field here is a boxed type: {@code null} means "not set at this
     * scope, inherit the next one up", which is a genuinely different state from "set to 0". A
     * primitive would make "I never touched this" and "I set it to zero" indistinguishable, and
     * the inheritance chain could not exist.
     *
     * <p>Stored sparsely: an entry with no overrides has no map entry at all, so a layout file
     * stays small and a reset is a removal rather than a rewrite of defaults.
     */
    public Map<String, ScopeLook> moduleLooks = new LinkedHashMap<>();

    /** Per-category overrides, keyed by section id. Same nullable-means-inherit contract. */
    public Map<String, ScopeLook> sectionLooks = new LinkedHashMap<>();

    /**
     * One scope's overrides. Every field is nullable; {@code null} inherits from the scope above.
     *
     * <p>One class for both the category and the entry scopes: the two scopes offer the same
     * numbers (a category sets what its entries share, an entry sets what only it uses), and two
     * identical classes would be two places for a field to be forgotten.
     */
    public static final class ScopeLook {
        // PRIMITIVES PLUS A `has` FLAG, not boxed types.
        //
        // "Not set at this scope" must be a distinct state from "set to zero" -- that is what makes
        // the cascade possible. Boxed `Float`/`Integer` expressed it with `null`, which is correct
        // but pays for it on every read: `resolveRail` runs for every rail entry on every frame the
        // rail is laid out, and each unboxing there is an allocation-free but branch-heavy access on
        // a value that is almost always present. A `float` beside a `boolean` says the same thing
        // with no boxing at all, and the intent stays explicit at every call site.
        public float itemHeight;
        public boolean hasItemHeight;
        public float itemCorner;
        public boolean hasItemCorner;
        public float itemPadding;
        public boolean hasItemPadding;
        public float markerWidth;
        public boolean hasMarkerWidth;
        public float fontSize;
        public boolean hasFontSize;
        public int markerColor;
        public boolean hasMarkerColor;
        public int textColor;
        public boolean hasTextColor;
        public int textSelected;
        public boolean hasTextSelected;
        public int selectedPlate;
        public boolean hasSelectedPlate;

        public ScopeLook() {
        }

        ScopeLook copy() {
            ScopeLook c = new ScopeLook();
            c.itemHeight = itemHeight; c.hasItemHeight = hasItemHeight;
            c.itemCorner = itemCorner; c.hasItemCorner = hasItemCorner;
            c.itemPadding = itemPadding; c.hasItemPadding = hasItemPadding;
            c.markerWidth = markerWidth; c.hasMarkerWidth = hasMarkerWidth;
            c.fontSize = fontSize; c.hasFontSize = hasFontSize;
            c.markerColor = markerColor; c.hasMarkerColor = hasMarkerColor;
            c.textColor = textColor; c.hasTextColor = hasTextColor;
            c.textSelected = textSelected; c.hasTextSelected = hasTextSelected;
            c.selectedPlate = selectedPlate; c.hasSelectedPlate = hasSelectedPlate;
            return c;
        }

        /** True when nothing is overridden, i.e. this scope is purely inherited. */
        public boolean isEmpty() {
            return !hasItemHeight && !hasItemCorner && !hasItemPadding && !hasMarkerWidth
                    && !hasFontSize && !hasMarkerColor && !hasTextColor && !hasTextSelected
                    && !hasSelectedPlate;
        }

        /** Brings every set field back into a range the interface can survive. */
        void clampScope() {
            if (hasItemHeight) itemHeight = clamp(itemHeight, 18f, 80f);
            if (hasItemCorner) itemCorner = clamp(itemCorner, 0f, 24f);
            if (hasItemPadding) itemPadding = clamp(itemPadding, 0f, 48f);
            if (hasMarkerWidth) markerWidth = clamp(markerWidth, 0f, 10f);
            if (hasFontSize) fontSize = clamp(fontSize, 8f, 24f);
        }
    }

    /**
     * The value an entry actually uses, after walking entry -> category -> rail.
     *
     * <p>One resolver per field, all built on [resolve] so the cascade is stated once. Callers pass
     * the two scopes and the rail's own value as the last resort; nothing else in the codebase
     * should re-implement the order.
     */
    public static final class Resolved {
        public float itemHeight;
        public float itemCorner;
        public float itemPadding;
        public float markerWidth;
        public float fontSize;
        public int markerColor;
        public int textColor;
        public int textSelected;
        public int selectedPlate;
    }

    /**
     * Resolves the cascade for one entry.
     *
     * @param moduleId   the entry being drawn, or null for a category heading's own use
     * @param sectionId  the category the entry belongs to, or null
     */
    public Resolved resolveRail(String moduleId, String sectionId) {
        ScopeLook m = moduleId == null ? null : moduleLooks.get(moduleId);
        ScopeLook s = sectionId == null ? null : sectionLooks.get(sectionId);
        Look base = look;
        Resolved r = new Resolved();
        // Entry first, then category, then the rail. Primitive reads only -- no boxing on a path
        // that runs for every entry of every frame the rail is laid out.
        r.itemHeight = m != null && m.hasItemHeight ? m.itemHeight
                : s != null && s.hasItemHeight ? s.itemHeight : base.railItemHeight;
        r.itemCorner = m != null && m.hasItemCorner ? m.itemCorner
                : s != null && s.hasItemCorner ? s.itemCorner : base.railItemCorner;
        r.itemPadding = m != null && m.hasItemPadding ? m.itemPadding
                : s != null && s.hasItemPadding ? s.itemPadding : base.railItemPadding;
        r.markerWidth = m != null && m.hasMarkerWidth ? m.markerWidth
                : s != null && s.hasMarkerWidth ? s.markerWidth : base.railMarkerWidth;
        r.fontSize = m != null && m.hasFontSize ? m.fontSize
                : s != null && s.hasFontSize ? s.fontSize : base.railFontSize;
        r.markerColor = m != null && m.hasMarkerColor ? m.markerColor
                : s != null && s.hasMarkerColor ? s.markerColor : base.railMarkerColor;
        r.textColor = m != null && m.hasTextColor ? m.textColor
                : s != null && s.hasTextColor ? s.textColor : base.railTextColor;
        r.textSelected = m != null && m.hasTextSelected ? m.textSelected
                : s != null && s.hasTextSelected ? s.textSelected : base.railTextSelected;
        r.selectedPlate = m != null && m.hasSelectedPlate ? m.selectedPlate
                : s != null && s.hasSelectedPlate ? s.selectedPlate : base.railSelectedPlate;
        return r;
    }

    /** The scope object for an entry, created on first write so an untouched entry stores nothing. */
    public ScopeLook moduleScopeFor(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) return new ScopeLook();
        return moduleLooks.computeIfAbsent(moduleId, k -> new ScopeLook());
    }

    /** The scope object for a category, created on first write. */
    public ScopeLook sectionScopeFor(String sectionId) {
        if (sectionId == null || sectionId.isEmpty()) return new ScopeLook();
        return sectionLooks.computeIfAbsent(sectionId, k -> new ScopeLook());
    }

    /**
     * Clears one field of one scope, returning it to inheritance.
     *
     * <p>A field-by-field clear rather than a whole-scope reset: a player who set four numbers and
     * wants one of them back on the cascade should not lose the other three.
     */
    public void clearModuleField(String moduleId, String field) {
        ScopeLook s = moduleLooks.get(moduleId);
        if (s == null) return;
        applyClear(s, field);
        if (s.isEmpty()) moduleLooks.remove(moduleId);
    }

    public void clearSectionField(String sectionId, String field) {
        ScopeLook s = sectionLooks.get(sectionId);
        if (s == null) return;
        applyClear(s, field);
        if (s.isEmpty()) sectionLooks.remove(sectionId);
    }

    /** Drops every override of one scope, so the whole scope returns to its parent. */
    public void clearModuleScope(String moduleId) {
        moduleLooks.remove(moduleId);
    }

    public void clearSectionScope(String sectionId) {
        sectionLooks.remove(sectionId);
    }

    private static void applyClear(ScopeLook s, String field) {
        switch (field == null ? "" : field) {
            case "itemHeight": s.hasItemHeight = false; break;
            case "itemCorner": s.hasItemCorner = false; break;
            case "itemPadding": s.hasItemPadding = false; break;
            case "markerWidth": s.hasMarkerWidth = false; break;
            case "fontSize": s.hasFontSize = false; break;
            case "markerColor": s.hasMarkerColor = false; break;
            case "textColor": s.hasTextColor = false; break;
            case "textSelected": s.hasTextSelected = false; break;
            case "selectedPlate": s.hasSelectedPlate = false; break;
            default: break;
        }
    }

    /**
     * Which material the window's surfaces are made of.
     *
     * <p>TWO MATERIALS, NOT TWO STRENGTHS. {@link GlassLevel} says HOW MUCH of the world shows
     * through; this says WHAT is showing it through. They are independent questions, and a player
     * can reasonably want a strong frost or a barely-there pane of either kind.
     *
     * <p>FROSTED is one full-screen blur composited flat across the backdrop: every pixel behind the
     * window is smeared by the same amount, and the surfaces are translucent panes in front of it.
     * It reads as fog, and it is cheap -- one shader, four passes, no per-panel work.
     *
     * <p>LIQUID_GLASS refracts INSIDE each panel: the image is displaced by an amount that grows
     * toward the panel's rim, following its corner radius, plus a lit highlight along the bevel.
     * That is what makes a pane look thick, and it is per-panel work -- one draw per panel, each with
     * its own uniforms.
     */
    public enum MaterialMode {
        /** One screen-wide blur behind translucent surfaces. */
        FROSTED("液态模糊"),
        /** Per-panel refraction with a lit rim. */
        LIQUID_GLASS("液态玻璃");

        public final String label;

        MaterialMode(String label) {
            this.label = label;
        }
    }

    /** The glass presets. {@link #shellAlpha} and {@link #panelAlpha} are the numbers each one sets. */
    public enum GlassLevel {
        /** Opaque: the world is fully hidden. The pre-glass look, kept for readability. */
        OFF("不透明", 0.95f, 1.0f, 0.00f),
        /** A hint of the world through the edges. */
        LIGHT("轻", 0.82f, 0.90f, 0.45f),
        /** The default: the blur is clearly visible but text keeps its ground. */
        MEDIUM("中", 0.66f, 0.78f, 0.75f),
        /** Strong: the reference look. Contrast must be re-checked at this level. */
        STRONG("强", 0.52f, 0.66f, 1.00f);

        public final String label;
        /** Alpha for the shell, the window's own surface. */
        public final float shellAlpha;
        /** Alpha for a floating panel, which sits above the shell and must stay distinct from it. */
        public final float panelAlpha;
        /** Blur strength this level asks for; scaled by the player's own blur slider. */
        public final float blur;

        GlassLevel(String label, float shellAlpha, float panelAlpha, float blur) {
            this.label = label;
            this.shellAlpha = shellAlpha;
            this.panelAlpha = panelAlpha;
            this.blur = blur;
        }
    }

    /** Movable and repaintable values: the rail, a panel, and the shell around them. */
    public static final class Look {
        // ---- rail ----
        /** Width in dp. */
        public float railWidth = 168f;

    /**
     * How much of the blurred world shows through the window's surfaces.
     *
     * <p>WHY THIS IS A PRESET AND NOT A SEPARATE OPACITY SLIDER. Glassiness is the product of two
     * numbers -- how transparent a surface is, and how strongly what is behind it is blurred -- and
     * they only read as glass together. Transparent over a sharp background looks like a bug (text
     * fights the world); opaque over a heavy blur is just a dark panel. Exposing them separately
     * invites exactly those two failures, so one choice sets both and the pairs are tuned here.
     *
     * <p>The blur is applied by {@code AuroraFx} to the whole backdrop, so it cannot follow a panel's
     * own rectangle -- this is frosted-at-the-screen rather than per-panel refraction. What makes it
     * read as material instead of as a grey wash is that the surfaces are translucent enough to show
     * the blur, and their borders are lit (see {@code Star.glassEdge}).
     */
    public GlassLevel glass = GlassLevel.MEDIUM;

    /**
     * Which material the surfaces are made of.
     *
     * <p>INDEPENDENT OF {@link #glass}. That one says HOW MUCH of the world shows through; this says
     * WHAT is showing it through. A player can reasonably want a strong frost or a barely-there pane
     * of either kind, so the two are separate choices rather than one list of combinations.
     *
     * <p>FROSTED is one full-screen blur composited flat across the backdrop, with translucent panes
     * in front of it: it reads as fog, and it is cheap -- a shader, four passes, no per-panel work.
     *
     * <p>LIQUID_GLASS refracts INSIDE each panel: the image behind is displaced by an amount that
     * grows toward the rim, following the panel's own corner radius, with a lit highlight along the
     * bevel. That is what makes a pane look thick, and it costs one draw per panel.
     */
    public MaterialMode material = MaterialMode.FROSTED;

    /**
     * The rail entries the player has pinned to the top.
     *
     * <p>IDS, not positions: the rail can be reordered, and a module can be added in a later version,
     * so a stored INDEX would end up pointing at whatever slid into that slot. Ids survive both.
     *
     * <p>ONE LinkedHashSet, used for two questions -- "is this pinned" and "which pinned one comes
     * first". A set for membership plus a separate list for order would be two places for the pins to
     * live, and the rail would rearrange itself the first time one was edited without the other.
     */
    public java.util.Set<String> pinnedModules = new java.util.LinkedHashSet<>();

    /**
     * Where each floating panel sits on screen, in ROOT-space pixels.
     *
     * <p>PUBLISHED BY THE COMPOSITION, READ BY THE RENDERER. The glass pass needs a panel's rectangle
     * in SCREEN pixels, and only Compose knows it: a panel's box is a fraction of the content stage,
     * which is itself positioned by the shell, so recovering an absolute rectangle from the
     * fractions would mean re-deriving the whole layout outside the layout. Compose can report the
     * answer directly with positionInRoot, so it does, and the renderer reads it.
     *
     * <p>Each entry is {x, y, w, h, radiusPx}. Rebuilt every composition; never persisted -- a stale
     * rectangle from a previous session would refract the wrong part of the screen.
     */
    public transient java.util.List<float[]> glassRects = new java.util.ArrayList<>();

        /** Height of one rail entry, in dp. */
        public float railItemHeight = 36f;
        /** Rail entry corner radius, in dp. */
        public float railItemCorner = 8f;
        /** Left padding inside a rail entry, in dp. */
        public float railItemPadding = 11f;
        /** The active marker's width, in dp. */
        public float railMarkerWidth = 2f;
        /** Rail label size, in sp. */
        public float railFontSize = 12f;
        /** The active marker's colour. */
        public int railMarkerColor = 0xFF4A90E2;
        /** A rail entry at rest. */
        public int railTextColor = 0xFF8A8A90;
        /** The selected rail entry's label. */
        public int railTextSelected = 0xFFD8D8DC;
        /** The selected entry's plate. */
        public int railSelectedPlate = 0x1AFFFFFF;
        /** Group label colour. */
        public int railGroupColor = 0xFF5E5E66;
        /** Horizontal offset of the whole rail, in dp. */
        public float railOffsetX = 0f;
        /** Vertical offset of the whole rail, in dp. */
        public float railOffsetY = 0f;

        // ---- panel ----
        /** Panel corner radius, in dp. */
        public float panelCorner = 12f;
        /** Panel surface. */
        public int panelBg = 0xFF121215;
        /** Panel border. */
        public int panelBorderColor = 0x24FFFFFF;
        /** Panel border width, in dp. */
        public float panelBorderWidth = 1f;
        /** Panel header height, in dp. */
        public float panelHeaderHeight = 48f;
        /** Padding inside a settings row, in dp. */
        public float rowPadding = 12f;
        /** A settings row's height, in dp. */
        public float rowHeight = 34f;
        /** Row label size, in sp. */
        public float rowFontSize = 12f;
        /** Row label colour. */
        public int rowLabelColor = 0xFFD8D8DC;
        /** Hint colour under a label. */
        public int hintColor = 0xFF5E5E66;

        // ---- shell ----
        /** Inset from the screen edge, in dp. */
        public float shellInset = 26f;
        /** Shell corner radius, in dp. */
        public float shellCorner = 14f;
        /** Shell top surface. */
        public int shellBgTop = 0xEB0A0A0C;
        /** Shell bottom surface. */
        public int shellBgBottom = 0xF2060607;
        /** Shell border. */
        public int shellBorderColor = 0x24FFFFFF;
        /** Shell border width, in dp. */
        public float shellBorderWidth = 1f;
        /** Header height, in dp. */
        public float headerHeight = 60f;

        // ---- controls ----
        /** Control corner radius, in dp. */
        public float controlCorner = 8f;
        /** Toggle track width, in dp. */
        public float toggleWidth = 36f;
        /** Toggle track height, in dp. */
        public float toggleHeight = 20f;
        /** The accent that fills a control's active state. */
        public int accent = 0xFF4A90E2;
        /** An unfilled track. */
        public int trackColor = 0xFF2A2A30;
        /** The accent's dimmer step, for a hover or a focus ring. */
        public int accentDim = 0xFF3E7BC4;

        Look copy() {
            Look l = new Look();
            l.railWidth = railWidth;
            l.railItemHeight = railItemHeight;
            l.railItemCorner = railItemCorner;
            l.railItemPadding = railItemPadding;
            l.railMarkerWidth = railMarkerWidth;
            l.railFontSize = railFontSize;
            l.railMarkerColor = railMarkerColor;
            l.railTextColor = railTextColor;
            l.railTextSelected = railTextSelected;
            l.railSelectedPlate = railSelectedPlate;
            l.railGroupColor = railGroupColor;
            l.railOffsetX = railOffsetX;
            l.railOffsetY = railOffsetY;
            l.panelCorner = panelCorner;
            l.panelBg = panelBg;
            l.panelBorderColor = panelBorderColor;
            l.panelBorderWidth = panelBorderWidth;
            l.panelHeaderHeight = panelHeaderHeight;
            l.rowPadding = rowPadding;
            l.rowHeight = rowHeight;
            l.rowFontSize = rowFontSize;
            l.rowLabelColor = rowLabelColor;
            l.hintColor = hintColor;
            l.shellInset = shellInset;
            l.shellCorner = shellCorner;
            l.shellBgTop = shellBgTop;
            l.shellBgBottom = shellBgBottom;
            l.shellBorderColor = shellBorderColor;
            l.shellBorderWidth = shellBorderWidth;
            l.headerHeight = headerHeight;
            l.controlCorner = controlCorner;
            l.toggleWidth = toggleWidth;
            l.toggleHeight = toggleHeight;
            l.accent = accent;
            l.trackColor = trackColor;
            l.accentDim = accentDim;
            return l;
        }

        /**
         * Clamps every field into a range the interface can survive.
         *
         * <p>A zero-width rail or a negative corner radius produces a window that cannot be fixed from
         * inside it -- the arrangement panel is reached through the very chrome being mis-sized -- so
         * the bounds are enforced here rather than trusted from the file.
         */
        void clampLook() {
            railWidth = clamp(railWidth, 90f, 320f);
            railItemHeight = clamp(railItemHeight, 22f, 60f);
            railItemCorner = clamp(railItemCorner, 0f, 20f);
            railItemPadding = clamp(railItemPadding, 0f, 40f);
            railMarkerWidth = clamp(railMarkerWidth, 0f, 8f);
            railFontSize = clamp(railFontSize, 9f, 20f);
            railOffsetX = clamp(railOffsetX, -400f, 400f);
            railOffsetY = clamp(railOffsetY, -400f, 400f);
            panelCorner = clamp(panelCorner, 0f, 32f);
            panelBorderWidth = clamp(panelBorderWidth, 0f, 6f);
            panelHeaderHeight = clamp(panelHeaderHeight, 30f, 90f);
            rowPadding = clamp(rowPadding, 0f, 40f);
            rowHeight = clamp(rowHeight, 22f, 60f);
            rowFontSize = clamp(rowFontSize, 9f, 20f);
            shellInset = clamp(shellInset, 0f, 200f);
            shellCorner = clamp(shellCorner, 0f, 40f);
            shellBorderWidth = clamp(shellBorderWidth, 0f, 6f);
            headerHeight = clamp(headerHeight, 36f, 120f);
            controlCorner = clamp(controlCorner, 0f, 20f);
            toggleWidth = clamp(toggleWidth, 24f, 72f);
            toggleHeight = clamp(toggleHeight, 14f, 40f);
        }
    }

    public static AuroraLayout load() {
        try {
            Path path = path();
            return Files.exists(path) ? fromJson(Files.readString(path)) : new AuroraLayout();
        } catch (Exception ignored) {
            return new AuroraLayout();
        }
    }

    public boolean save() {
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Files.writeString(path, toJson());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }

    /**
     * Parses a layout, repairing anything the file got wrong rather than failing.
     *
     * <p>Every field is checked because the return value here is the value the screen then runs with:
     * a null map or a NaN scale would not surface until the first drag or the first layout pass, by
     * which point the stack trace points at the composition instead of at the file that caused it.
     */
    public static AuroraLayout fromJson(String json) {
        try {
            AuroraLayout v = new Gson().fromJson(json, AuroraLayout.class);
            if (v == null) return new AuroraLayout();
            v.repair();
            return v;
        } catch (Exception ignored) {
            return new AuroraLayout();
        }
    }

    private void repair() {
        if (!Float.isFinite(scale)) scale = 1f;
        scale = clamp(scale, MIN_SCALE, MAX_SCALE);
        if (motion == null) motion = MotionLevel.FULL;
        backdropBlur = clamp01(backdropBlur);
        parallax = clamp01(parallax);
        coreX = clamp01(coreX);
        coreY = clamp01(coreY);
        if (!Float.isFinite(snap)) snap = 0.02f;
        snap = clamp(snap, 0f, 0.2f);
        if (cards == null) cards = new LinkedHashMap<>();
        if (navOrder == null) navOrder = new ArrayList<>();
        // A null box, or a corrupt one, would throw on the first clamp during layout.
        cards.entrySet().removeIf(e -> e.getKey() == null);
        for (CardBox box : cards.values()) {
            if (box != null) box.clampBox();
        }
        // A missing Look, or one whose fields a hand-edited file drove out of range, is repaired
        // rather than rejected: the arrangement panel is reached through the chrome these values dress,
        // so refusing to load would leave the player with a window they cannot navigate to fix.
        if (look == null) look = new Look();
        look.clampLook();
        // The scope maps are hand-editable too: a null map, a null key or a value outside the
        // survivable range must be repaired here rather than throwing on the frame that draws the
        // rail. An empty scope is dropped, so a file that recorded an override and then had it
        // cleared does not keep a dead entry around.
        if (moduleLooks == null) moduleLooks = new LinkedHashMap<>();
        if (sectionLooks == null) sectionLooks = new LinkedHashMap<>();
        moduleLooks.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        sectionLooks.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        moduleLooks.entrySet().removeIf(e -> {
            e.getValue().clampScope();
            return e.getValue().isEmpty();
        });
        sectionLooks.entrySet().removeIf(e -> {
            e.getValue().clampScope();
            return e.getValue().isEmpty();
        });
        if (sections == null) sections = new ArrayList<>();
        // A section with no id cannot be addressed by the arrangement or by its menu, and one with no
        // name prints an empty label. Both are the file's fault rather than the player's, so they are
        // dropped rather than shown.
        sections.removeIf(s -> s == null || s.id == null || s.id.isEmpty() || s.name == null);
        for (Section s : sections) {
            if (s.modules == null) s.modules = new ArrayList<>();
            s.modules.removeIf(id -> id == null || id.isEmpty());
        }
        if (window != null) window.clampBox();
        if (windowRestore != null) windowRestore.clampBox();
        // A persisted maximized flag must match a full-stage rectangle. A file written by an older
        // build can carry maximized=true with an arbitrary rectangle: it then renders full height
        // but off to one side, and -- because the move band used to be gated on that same flag --
        // nothing could move it at all. Repairing the rectangle here makes the state coherent.
        if (windowMaximized) {
            window = new CardBox(0f, 0f, 1f, 1f, 0);
        }
    }

    /**
     * The window's box, created from `fallback` on first write and stored from then on.
     *
     * <p>The ONE storage for the whole window. There used to be two: the renderer read {@link #window}
     * while the drag and resize handlers wrote a separate entry into {@link #cards} under a private id, so
     * the two never met -- the box was rewritten on every drag into a place nothing read, and the window
     * could not be moved or resized at all. A single accessor is what makes that impossible to
     * reintroduce.
     *
     * <p>Creation is lazy and happens on the first WRITE, which is what keeps `window == null` meaning
     * "derive the rectangle from the inset". Creating it on read would freeze the inset at whatever it
     * was on the first frame, so changing the inset would then have no effect.
     */
    public CardBox windowBox(CardBox fallback) {
        if (window == null) {
            window = fallback.copy();
            window.clampBox();
        }
        return window;
    }

    /**
     * The box for a card, filled in from the panel's own default on first sight and stored back.
     *
     * <p>Returns the stored instance rather than a copy: the drag handlers write through it, and a
     * copy would silently discard every drag that did not also reach {@link #save()}.
     */
    public CardBox boxFor(String id, CardBox fallback) {
        CardBox existing = cards.get(id);
        if (existing == null) {
            existing = fallback.copy();
            existing.clampBox();
            cards.put(id, existing);
        }
        return existing;
    }

    /** Brings a card to the front, renumbering only when two cards would otherwise tie. */
    public void raise(String id) {
        CardBox box = cards.get(id);
        if (box == null) return;
        int highest = 0;
        for (CardBox other : cards.values()) {
            if (other != null && other.depth > highest) highest = other.depth;
        }
        if (box.depth == highest) return;
        box.depth = highest + 1;
        renumberByPin();
    }

    /**
     * Renumbers depths so the pinned tier sits wholly above the unpinned tier.
     *
     * <p>Called after any depth or pin change. Sorting by (pinned, depth) and assigning dense
     * indices keeps both orderings stable -- pinned panels keep their relative order, unpinned
     * keep theirs, and no unpinned depth can exceed a pinned one, which is the whole contract of
     * a pin. Dense numbering also keeps the persisted values from drifting upward every session.
     */
    private void renumberByPin() {
        List<CardBox> ordered = new ArrayList<>(cards.values());
        ordered.sort((a, b) -> {
            int byPin = Boolean.compare(a.pinned, b.pinned);
            return byPin != 0 ? byPin : Integer.compare(a.depth, b.depth);
        });
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).depth = i;
        }
    }

    /**
     * Sets or clears a panel's pin and re-tiers the depths.
     *
     * <p>Public because the pin control lives in the Kotlin scaffold. The renumber is what makes
     * the pin meaningful: without it a pinned panel could sit at a lower depth than an unpinned
     * one, and the button would say one thing while the window drew another.
     */
    public void setPinned(String id, boolean pinned) {
        CardBox box = cards.get(id);
        if (box == null || box.pinned == pinned) return;
        box.pinned = pinned;
        renumberByPin();
    }

    /** Nav ids in the player's order, with ids the file did not know about appended in code order. */
    public List<String> orderNav(List<String> declared) {
        List<String> result = new ArrayList<>();
        for (String id : navOrder) {
            if (declared.contains(id) && !result.contains(id)) result.add(id);
        }
        for (String id : declared) {
            if (!result.contains(id)) result.add(id);
        }
        return result;
    }

    public void setNav(List<String> order) {
        navOrder = new ArrayList<>(order);
    }

    /**
     * A scale that suits the window, for auto mode.
     *
     * <p>Keyed on the area rather than the diagonal: a 4K window has four times the pixels of a
     * 1080p one but only twice the diagonal, and scaling on the diagonal would leave every label
     * half the physical size it is on the smaller screen.
     */
    public float autoScaleFor(int pixelWidth, int pixelHeight) {
        if (pixelWidth <= 0 || pixelHeight <= 0) return 1f;
        float area = (float) pixelWidth * pixelHeight;
        float reference = 1920f * 1080f;
        float factor = (float) Math.sqrt(area / reference);
        // A gentler slope than the raw square root: at 4K, sqrt gives 2.0, which on a typical desk
        // would make a settings row the size of a hand. 1.35 at 4K reads as comfortable.
        float scaleValue = 0.75f + 0.35f * factor;
        return clamp(scaleValue, MIN_SCALE, MAX_SCALE);
    }

    /**
     * Restores the arrangement to its defaults.
     *
     * <p>Resets the whole {@link Look} rather than the handful of fields that happened to exist first:
     * the panel that invokes this is labelled "重置布局", and a reset that left half a dozen colours
     * and sizes untouched would be a lie.
     */
    public void resetLayout() {
        cards.clear();
        navOrder.clear();
        sections.clear();
        window = null;
        // The gesture switches survive a reset: they are the player's input preferences, not part
        // of the arrangement. Resetting them would turn "put my window back" into "and also forget
        // I disabled snapping", which nobody asked for.
        windowMaximized = false;
        windowRestore = null;
        // The per-scope overrides go with it: they are part of the arrangement, and leaving them
        // behind would make "重置布局" restore the rail's numbers while each entry kept its own.
        moduleLooks.clear();
        sectionLooks.clear();
        look = new Look();
    }

    /**
     * Restores ONE group of {@link Look} values to its defaults, leaving the rest alone.
     *
     * <p>The quick reset a page offers: a player who has been fiddling with the rail's sizes wants
     * those back, and does not want their panel colours, the shell's surface or the control accent
     * reset with them. Groups are named by the page that edits them, which is also how the menu is
     * organised, so "reset" on a page means exactly "reset what this page shows".
     *
     * <p>A field belongs to exactly one group. The assignment below is the single statement of
     * that split -- the menu's pages must agree with it, and a guard asserts that every field is
     * covered by some group so nothing can be silently un-resettable.
     */
    public void resetLookGroup(String group) {
        Look d = new Look();
        Look l = look;
        switch (group == null ? "" : group) {
            case "rail":
                l.railWidth = d.railWidth;
                l.railItemHeight = d.railItemHeight;
                l.railItemCorner = d.railItemCorner;
                l.railItemPadding = d.railItemPadding;
                l.railMarkerWidth = d.railMarkerWidth;
                l.railFontSize = d.railFontSize;
                l.railMarkerColor = d.railMarkerColor;
                l.railTextColor = d.railTextColor;
                l.railTextSelected = d.railTextSelected;
                l.railSelectedPlate = d.railSelectedPlate;
                l.railGroupColor = d.railGroupColor;
                l.railOffsetX = d.railOffsetX;
                l.railOffsetY = d.railOffsetY;
                break;
            case "panel":
                l.panelCorner = d.panelCorner;
                l.panelBg = d.panelBg;
                l.panelBorderColor = d.panelBorderColor;
                l.panelBorderWidth = d.panelBorderWidth;
                l.panelHeaderHeight = d.panelHeaderHeight;
                l.rowPadding = d.rowPadding;
                l.rowHeight = d.rowHeight;
                l.rowFontSize = d.rowFontSize;
                l.rowLabelColor = d.rowLabelColor;
                l.hintColor = d.hintColor;
                break;
            case "shell":
                l.shellInset = d.shellInset;
                l.shellCorner = d.shellCorner;
                l.shellBgTop = d.shellBgTop;
                l.shellBgBottom = d.shellBgBottom;
                l.shellBorderColor = d.shellBorderColor;
                l.shellBorderWidth = d.shellBorderWidth;
                l.headerHeight = d.headerHeight;
                break;
            case "controls":
                l.controlCorner = d.controlCorner;
                l.toggleWidth = d.toggleWidth;
                l.toggleHeight = d.toggleHeight;
                l.accent = d.accent;
                l.trackColor = d.trackColor;
                l.accentDim = d.accentDim;
                break;
            default:
                break;
        }
        l.clampLook();
    }

    public static Path path() {
        Path config = FMLPaths.CONFIGDIR.get();
        if (config == null) config = Path.of("config");
        return config.resolve("yuan/aurora.json");
    }

    private static float clamp01(float v) {
        return clamp(v, 0f, 1f);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static boolean finite(float... values) {
        for (float v : values) {
            if (!Float.isFinite(v)) return false;
        }
        return true;
    }
}
