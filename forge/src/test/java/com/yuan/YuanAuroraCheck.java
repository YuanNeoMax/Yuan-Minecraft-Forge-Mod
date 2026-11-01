package com.yuan;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards the Aurora backdrop-blur pipeline (Aurora step 1).
 *
 * <p>Source assertions pin the contracts a later edit could silently break: every declared uniform
 * really read in the shader (the GLSL 150 drop lesson), the framebuffer re-bind after every
 * bindWrite, and the state restore around the passes.
 */
public final class YuanAuroraCheck {

    public static void main(String[] args) throws Exception {
        String fsh = Files.readString(Path.of(
                "src/main/resources/assets/yuan/shaders/core/aurora_blur.fsh"));
        String json = Files.readString(Path.of(
                "src/main/resources/assets/yuan/shaders/core/aurora_blur.json"));
        String fx = Files.readString(Path.of(
                "src/main/java/com/yuan/client/aurora/AuroraFx.java"));
        String shaders = Files.readString(Path.of(
                "src/main/java/com/yuan/client/aurora/AuroraShaders.java"));
        String state = Files.readString(Path.of(
                "src/main/java/com/yuan/client/aurora/AuroraRenderState.java"));

        // ---- every declared uniform must be genuinely read: GLSL 150 drops the rest -----------
        // A dropped uniform makes the Java lookup return null, ready() false, and the whole pass
        // silently skipped. The domain shader already paid for this lesson once.
        for (String uniform : new String[]{"Dir", "Tint", "ColorModulator"}) {
            assert fsh.contains("uniform vec2 " + uniform)
                    || fsh.contains("uniform vec4 " + uniform)
                    : "aurora fsh must declare " + uniform;
            assert json.contains("\"" + uniform + "\"")
                    : "aurora json must declare " + uniform;
        }
        assert fsh.contains("vec2 texel = Dir;") && fsh.contains("Tint")
                && fsh.contains("ColorModulator")
                : "aurora fsh must READ every uniform it declares, not just declare it";

        // ---- the identity step really is an identity -------------------------------------------
        // Dir=(0,0) must copy: the weights sum to 1 and every tap lands on the same texel.
        double weightSum = 0.227027 + 2 * 0.316216 + 2 * 0.070270;
        assert Math.abs(weightSum - 1.0) < 1e-5
                : "gaussian weights must sum to 1 so the Dir=(0,0) composite is an exact copy, got "
                        + weightSum;

        // ---- framebuffer discipline -------------------------------------------------------------
        // bindWrite leaves the viewport at the destination's size and every pass re-binds its own
        // destination; a pass that forgot the re-bind would detache the frame from the post chain.
        assert fx.contains("destination.bindWrite(true)")
                : "every aurora pass must bind its destination before drawing";
        assert fx.contains("getColorTextureId()")
                : "aurora samples raw texture ids so targets never need a resource location";
        assert fx.contains("AuroraRenderState.restore()")
                : "the passes must restore state unconditionally via finally";

        // ---- state discipline --------------------------------------------------------------------
        assert state.contains("depthMask(false)") && state.contains("depthMask(true)")
                : "aurora passes must not write depth and must undo it";
        assert state.contains("disableDepthTest()") && state.contains("enableDepthTest()")
                : "aurora passes are flat triangles: depth testing must be off and restored";

        // ---- uniform push path -------------------------------------------------------------------
        assert shaders.contains("onApply")
                : "aurora uniforms must be pushed from onApply: values set before apply are "
                        + "overwritten by the json defaults";

        // ---- the v3 shell, grid and panels -------------------------------------------------------
        String screen = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/AuroraComposeScreen.kt"));
        String shell = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/scaffold/AuroraShell.kt"));
        String grid = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/scaffold/AuroraGrid.kt"));
        String controls = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/scaffold/AuroraControls.kt"));
        String motion = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/anim/Motion.kt"));
        String theme = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/theme/Starlight.kt"));
        String icons = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/icons/AuroraIcons.kt"));
        String godScreenSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/AuroraGodSwordScreen.kt"));
        String windowSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/scaffold/AuroraWindow.kt"));

        // ---- selecting a feature must never be disabled ------------------------------------------
        // The bug this guards: the rail's click was gated on `!layout.editMode`, and edit mode is
        // persisted, so a player who had once turned layout editing on found the rail dead -- only the
        // already-selected feature had any content, and nothing said why. Selecting is the rail's
        // primary action; edit mode may add a grip and a drag, never take the click away.
        String rail = stripComments(Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/scaffold/AuroraGrid.kt")));
        assert !rail.contains("enabled = !layout.arrangeMode")
                : "the rail's click must not be gated on edit mode: that flag is persisted, so it "
                        + "silently disables navigation for anyone who ever enabled layout editing";
        assert rail.contains("clickable(interactionSource = interaction, indication = null) {\n"
                        + "                        onSelect(entry.id)")
                || rail.contains("onSelect(entry.id)")
                : "the rail must select on click unconditionally";
        // The arrangement MODE is gone; reordering is a resident long-press drag. What must hold is
        // that nothing gates it on a persisted flag any more, and that the long-press detector is
        // genuinely CALLED on the entries: two occurrences of the token -- one import, one call --
        // and the call is the one that can commit a move. (The column-level long-press move is
        // retired; only the entries' reorder remains.)
        assert !rail.contains("arrangeMode")
                : "the arrangement mode is gone: a persisted flag gating gestures trapped navigation";
        assert countOccurrences(stripComments(rail), "detectDragGesturesAfterLongPress(") == 1
                : "the entry reorder must be exactly one resident long-press drag call site; found "
                        + countOccurrences(stripComments(rail), "detectDragGesturesAfterLongPress(");

        // The backdrop must go down before Compose draws, or the panels sit on an unblurred world.
        int blurAt = screen.indexOf("blurBackdrop(mc)");
        int renderAt = screen.indexOf("scene.render");
        assert blurAt > 0 && renderAt > blurAt
                : "blur must run before the scene render in the same frame, found blur@" + blurAt
                        + " render@" + renderAt;
        assert screen.contains("YuanComposeGlState.save()")
                && screen.contains("YuanComposeGlState.restore(saved)")
                : "the Compose flush must be bracketed by a GL state save/restore";

        // ---- two coordinate spaces, and the pointer conversion belongs to exactly one ----------
        // This guard previously asserted the opposite of the truth and held the bug in place, so the
        // reasoning is written out rather than the conclusion:
        //
        //   vanilla MouseHandler (bytecode, m_91561_ offsets 62-123) hands a Screen
        //   `rawCursor * getGuiScaledWidth / getScreenWidth`, i.e. GUI-scale space. The physical
        //   cursor therefore sits at `mouseX * gameScale`, and the Compose surface is allocated at the
        //   physical size, so `sendPointerEvent` wants surface pixels. Compose divides what it is
        //   given by the scene density to get dp, and a control authored at D dp occupies
        //   D * density physical pixels, so:
        //
        //       passed / density == mouseX * gameScale / density   ->   passed == mouseX * gameScale
        //
        //   The density cancels. Multiplying by the full density applies Aurora's factor a second
        //   time on top of the one Compose already applies, and the pointer lands at
        //   `mouseX * auroraScale` -- correct only at a scale of exactly 1.0.
        assert screen.contains("private val composeDensity: Float")
                : "the scene density must remain one derived value";
        assert screen.contains("gameScale * effectiveScale(")
                : "the scene density must be the game's GUI scale times Aurora's own factor";
        // Density is for the scene, for layout, and for the scroll step -- a step is a distance.
        // The step also carries a real magnitude (56dp per notch): with delta*density alone a
        // notch moved about two pixels, and every scrollable looked unscrollable.
        assert screen.contains("scrollDelta = Offset(0f, (-delta * 56f * composeDensity).toFloat())")
                : "the scroll step must be a real distance (~56dp) scaled by the density";
        // Pointer positions are the one thing density must not appear in.
        assert screen.contains("private fun toComposeOffset(x: Double, y: Double): Offset {\n        val s = gameScale")
                : "the pointer conversion must use the game scale alone; the density cancels out of it";
        assert screen.contains("(mouseX * gameScale).toInt()")
                : "synthesized AWT events carry physical pixels, so they use the game scale too";
        assert !screen.contains("position = toComposeOffset")
                || !screen.contains("val s = composeDensity\n        return Offset((x * s)")
                : "a pointer position must never be scaled by the Aurora factor";

        // ---- the accent has one job, and this is where that is enforced -------------------------
        // V4 admits blue, which the previous revision refused outright. The refusal was not wrong, it
        // was too broad: with state carried only by brightness, a switch that is "on" looks like a row
        // that is highlighted, and a slider fill looks like a progress meter. The rule that keeps the
        // original point is narrower -- the accent fills STATE, and appears nowhere else. So this
        // asserts both halves: that the token exists, and that it is used only by the two controls
        // allowed to state a value with it.
        assert theme.contains("val accent") && theme.contains("val accentTrack")
                : "the theme must carry the accent and a track for the unfilled state";
        assert !controls.contains("0xFFC8A860") && !grid.contains("gold")
                : "the gold palette this replaced must be gone";
        // The two permitted use sites: the toggle's track and the slider's fill and knob.
        assert controls.contains(".background(if (checked) Star.accent else Star.accentTrack)")
                : "a toggle states its on state by filling the track with the accent";
        assert countOccurrences(controls, "color = if (enabled) Star.accent else Star.textDisabled") == 2
                : "a slider's fill and knob are the accent; found "
                        + countOccurrences(controls, "color = if (enabled) Star.accent else Star.textDisabled");
        // Nowhere else. A border, a label or a button taking the accent would make a blue pixel stop
        // meaning "this control is on", which is the whole point of admitting it.
        assert !controls.contains("Star.accent,") && !controls.contains("border(1.dp, Star.accent")
                || controls.contains("border(\n                1.dp,\n                when {")
                : "the accent must not be used as a border or a second fill in the controls";
        // The rail's rows take their accent as an OUTLINE, never as a fill.
        //
        // A tinted row states nothing -- "which of these is selected" has to be readable at a glance,
        // and a wash of colour over a row answers a different question. The one legitimate accent in
        // the rail is the keyboard FOCUS RING, which is a 2dp border: an outline does not replace the
        // selected plate underneath it, and for a keyboard user it is the only signal there is (they
        // never hovered, so the hover fill is not even set -- that is why the ring cannot simply
        // reuse it).
        //
        // So the assertion forbids the accent as a FILL and permits it as a border. Checking the call
        // form rather than the bare token is what keeps the two apart: a blanket ban would have
        // failed on the focus ring, and a blanket allowance would let a filled row back in.
        assert !grid.contains("background(Star.accent")
                && !grid.contains("Star.starlight,")
                : "the rail's rows must not be filled with the accent; a tinted row states nothing";
        assert grid.contains("Modifier.border(2.dp, Star.accent, RoundedCornerShape(entryCorner))")
                : "the keyboard focus ring is the rail's one legitimate accent outline";

        // ---- keyboard navigation ----------------------------------------------------------
        // The window renders into the game's framebuffer through a scene that NEVER takes focus, so
        // Compose's `Modifier.onKeyEvent` delivers nothing -- a tree of focusRequesters would sit
        // there receiving no keys at all. That is why the key path is a hook on the host screen and
        // why the focus model is hand-built.
        {
            String hostSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/AuroraComposeScreen.kt"));
            String focusSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/scaffold/AuroraFocus.kt"));
            String screenSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/AuroraGodSwordScreen.kt"));

            assert hostSrc.contains("protected var onKeyPressed")
                    : "the host must expose a general key hook, not only the ESC hatch";
            assert hostSrc.contains("if (keyHandler != null && keyHandler.invoke(keyCode, modifiers))")
                    : "a consumed key must stop at the hook, or Escape and the game also see it";
            // The hook takes the modifier bitfield as well: Ctrl+Z and a bare `z` are different
            // intentions, and only the host knows which was held.
            assert hostSrc.contains("protected var onKeyPressed: ((keyCode: Int, modifiers: Int) -> Boolean)?")
                    : "the key hook must receive the modifiers alongside the key code";
            // The model is two numbers -- a region and an index -- rather than a focus tree: focus
            // cannot nest in this window, so a tree would be complexity with nothing to express.
            assert focusSrc.contains("var region: FocusRegion by mutableStateOf")
                    && focusSrc.contains("var index: Int by mutableStateOf")
                    : "the focus model must be a region plus an index";
            assert focusSrc.contains("var keyboardActive: Boolean by mutableStateOf")
                    : "the ring's visibility must be state, so it can follow the input in use";
            // The ring starts HIDDEN, and a KEY press reveals it -- two separate facts, asserted
            // separately. Checking only for the presence of "keyboardActive = true" was not enough:
            // `by mutableStateOf(true)` contains that substring too, so a ring that was always on
            // passed the check. (Found by fault injection: the "ring always visible" injection was
            // the one case this assertion missed.)
            assert focusSrc.contains("var keyboardActive: Boolean by mutableStateOf(false)")
                    : "the ring must start hidden: a mouse user must never see it";
            assert focusSrc.contains("keyboardActive = true")
                    : "a key press must reveal the ring";
            // The ring shows only for the keyboard, and any mouse move takes it away again.
            assert shell.contains("internal fun Modifier.clearFocusOnPointer(focus: FocusState)")
                    && shell.contains("focus.keyboardActive = false")
                    : "a mouse move must hide the ring again";
            // Left/right cross regions; they must NOT enter a floating panel, which is opened and
            // closed deliberately and would be a trap to fall into with no visible way back.
            assert screenSrc.contains("focus.cycle(")
                    : "left/right must move between the rail and the content area";
            assert screenSrc.contains("railEntryIds")
                    : "the rail's walkable entries must come from the list the rail draws";
            // Activating on the rail opens the feature -- the same write a left click makes.
            assert screenSrc.contains("AuroraKeys.isActivate(keyCode)")
                    : "Enter/Space must activate the focused element";
            // The ring is drawn only while the keyboard is in use, and on the FOCUSED entry:
            // a ring that ignored the region would appear on the rail while the keyboard was in the
            // content area.
            assert grid.contains("focus.region == FocusRegion.Rail")
                    && grid.contains("focus.index == index")
                    : "the ring must be bound to the focused entry in the focused region";
            // ESC backs out one layer at a time: menu, then panels, then the window. Skipping the
            // panel layer meant the only way to dismiss one was its own small close control.
            {
                String code = stripComments(screenSrc);
                int at = code.indexOf("onEscapePressed = {");
                int end = code.indexOf("railSections", at);
                assert at >= 0 && end > at;
                String esc = code.substring(at, end);
                assert esc.contains("openPanels.isNotEmpty()")
                        : "ESC must close the topmost panel before it closes the window";
                assert esc.contains("dismissPanel(openPanels.last())")
                        : "ESC must dismiss exactly the topmost panel, through the shared path";
            }

            // ---- Tier A: the polish pass ------------------------------------------------------
            // A module switch CROSSFADES. It is the most frequent action in the window, and it used
            // to swap the content area in one frame -- the old feature vanished and the new one then
            // staged in, which reads as a load rather than as a move.
            assert screenSrc.contains("Crossfade(")
                    && screenSrc.contains("targetState = selectedModule")
                    : "switching features must crossfade";
            // Keyed on the MODULE, never on `revision`: revision changes on every slider drag, and
            // crossfading there would make the page flicker while the player drags.
            assert !screenSrc.contains("targetState = revision")
                    : "the crossfade must not be keyed on revision, or every drag flickers the page";
            // The outgoing copy draws its OWN rows: `groups` describes the module being switched TO,
            // so reusing it would paint the new feature's contents into the frame fading out.
            assert screenSrc.contains("contentGroups")
                    : "the outgoing half must resolve its own module's rows";
            // And it fades without staging, so two animations do not fight over the same pixels.
            assert screenSrc.contains("staging = shownModule == selectedModule")
                    : "only the incoming half may stage its rows";
            assert Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordInlinePanel.kt"))
                    .contains("enabled = staging")
                    : "the panel must honour the staging flag";

            // The wheel GLIDES. `verticalScroll` only flings from a drag -- a notch has no release
            // velocity -- so each notch used to teleport the list and stop.
            String commonSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/scaffold/AuroraCommon.kt"));
            assert commonSrc.contains("internal fun Modifier.smoothWheel(state: ScrollState)")
                    : "the wheel must be eased";
            assert commonSrc.contains("state.animateScrollTo(goal.roundToInt())")
                    : "the glide must animate the scroll rather than jump it";
            assert commonSrc.contains("change.consume()")
                    : "the eased wheel must consume, or the scroller moves the list twice per notch";
            // ORDER: the easing handler sits INSIDE the scroller so the child-first Main pass lets it
            // see the notch first. Reversed, the scroller wins and the glide is inert.
            assert commonSrc.contains(
                    "this.verticalScroll(state).smoothWheel(state).thinScrollbar(state)")
                    : "smoothWheel must sit inside verticalScroll or the scroller handles the wheel";

            // Hover FADES, selection does not.
            assert grid.contains("private fun entryFill(")
                    : "the entry fill must ease its transient states";
            assert grid.contains("label = \"entryHover\"")
                    : "the hover fill must be animated";
            assert !grid.contains("hovered -> Star.hover")
                    : "a snapping hover fill strobes as the pointer crosses the column";
            // Selection stays instant: a click must not appear to wait for an animation.
            assert grid.contains("selected -> selectedPlate")
                    : "the selected plate must switch immediately";

            // The panel's dismissal is the mirror of its entrance, and the ramp now takes its own
            // duration so a small surface can leave faster than the window it sits in.
            assert motion.contains("internal fun openProgress(")
                    && motion.contains("shown: Boolean = true,")
                    && motion.contains("durationMs: Int = Motion.WINDOW_MS")
                    : "the surface ramp must run both ways, with a caller-chosen duration";
            assert motion.contains("if (shown) 1f else 0f")
                    : "the ramp must drive toward the shown/hidden target";
            assert shell.contains("dismissing: Boolean = false")
                    && shell.contains("openProgress(title, shown = !dismissing)")
                    : "a panel must know it is being dismissed so it can fade out";
            // The dismissing panel stays RENDERED until its ramp has run; removing the id at dismiss
            // time could only ever blink it away.
            assert screenSrc.contains("renderedPanelIds")
                    && screenSrc.contains("dismissing = panelId in closingPanels")
                    : "a dismissing panel must stay on screen for its exit ramp";
            // Cleanup captures its ids BEFORE the delay: reading the set after it would let a later
            // dismissal be finished early by an earlier run's timer.
            assert screenSrc.contains("val settled = closingPanels")
                    : "the exit cleanup must capture its own ids before waiting";

            // ---- Tier B: undo/redo -----------------------------------------------------------
            // Each line below is a decision rather than a detail: the granularity, the baseline, and
            // the two things a commit must not do.
            String histSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/AuroraEditHistory.kt"));
            assert histSrc.contains("internal class AuroraEditHistory(initial: V9State)")
                    : "the editor needs its own history: YuanConfigHistory works on a tag, not V9State";
            // A commit that changes nothing is dropped -- that is what V9State's structural equality
            // buys, and without it undo would appear to do nothing after a no-op edit.
            assert histSrc.contains("if (now == settled) return")
                    : "a commit that changes nothing must not enter the stack";
            // One gesture is one step: a drag must not leave hundreds of one-pixel entries behind.
            assert screenSrc.contains("LaunchedEffect(pendingEdit)")
                    && screenSrc.contains("delay(EDIT_SETTLE_MS)")
                    : "the commit must be debounced, or a drag fills the stack";
            // Every write arms it, hooked on the one helper all edits already pass through, so a new
            // control cannot forget to record itself.
            assert screenSrc.contains("pendingEdit = true")
                    : "an edit must arm the commit";
            // An undo step must not itself be recorded, or redo could never be reached.
            assert screenSrc.contains("pendingEdit = false")
                    : "restoring must not be recorded as a new edit";
            assert histSrc.contains("redo.clear()")
                    : "a new edit must invalidate the redo branch";
            // A lambda ASSIGNED to a property has no implicit label, so the shortcut's early returns
            // need a named function. Spelling a label out does not work: the implicit ones are the
            // only names available.
            assert screenSrc.contains("fun handleKey(keyCode: Int, modifiers: Int): Boolean")
                    : "the key path must be a named function for its early returns to compile";
            // A shortcut and its modifier are read from one table, so the two cannot drift apart.
            assert focusSrc.contains("const val MOD_CONTROL = 0x0002")
                    && focusSrc.contains("fun isCtrl(modifiers: Int)")
                    : "the Ctrl bit belongs with the key codes, not inline at the call site";

            // ---- Tier B: the search spans features ---------------------------------------------
            // The content header's own filter only narrowed the feature already on screen, so finding
            // "冷却时间" required already knowing which of the eight features held it -- which is when a
            // search is worth having, and when the old one was useless.
            String modSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordModules.kt"));
            assert modSrc.contains("internal fun search(query: String, rowsOfModule: (Module) -> List<GodRow>)")
                    : "the search must span modules";
            assert modSrc.contains("internal data class SearchHit(")
                    && modSrc.contains("val moduleId: String")
                    : "a hit must name its feature, or choosing it cannot take you there";
            // Gated rows are INCLUDED even when their gate is closed: a row hidden by the current
            // renderStyle is still a real setting, and omitting it makes the search silently
            // incomplete -- the worst failure a search can have.
            assert !modSrc.substring(
                    modSrc.indexOf("internal fun search("),
                    modSrc.indexOf("internal data class Group("))
                    .contains("it.gate")
                    : "the search must not skip gated rows, or it silently misses settings";
            // The results REPLACE the feature's own rows while a query is active, and choosing one
            // switches features; the query stays, because the player is usually comparing hits.
            assert screenSrc.contains("GodSwordSearchResults(")
                    : "a query must show cross-module results";
            assert screenSrc.contains("selectedModule = hit.moduleId")
                    : "choosing a hit must open the feature it lives in";
            assert screenSrc.contains("val searchHits = remember(query, revision)")
                    : "the hits must be keyed on the query, not recomputed every frame";

            // ---- Tier C: one row back to its default ------------------------------------------
            String bindSrcMark = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordBinding.kt"));
            // The default comes from `V9State`'s own field initialisers, so there is exactly one
            // place a default is declared. A second table would drift the first time either moved.
            assert bindSrcMark.contains("V9Holder(com.yuan.client.gui.V9State())")
                    : "the defaults must come from V9State's initialisers, not a parallel table";
            // Shared, and read-only: writing here would make each reset restore the previous reset.
            assert bindSrcMark.contains("private val defaultsHolder: V9Holder by lazy")
                    : "the defaults holder must be built once and shared";
            // The scale is NOT applied: readFloat and writeFloat both speak the stored unit, so
            // applying the display factor on one side is the bug the row's `scale` field prevents.
            assert bindSrcMark.contains("writeFloat(id, target, readFloat(id, defaults))")
                    : "a copied value must not be scaled; both accessors speak the stored unit";
            // Rows with no single default must say so rather than appear to work.
            assert bindSrcMark.contains("else -> false")
                    : "a row with no meaningful default must report that it cannot reset";
            String rowSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordPanel.kt"));
            assert rowSrc.contains("GodSwordBinding.resetRow(godRow, holder)")
                    : "the row must offer the reset";
            // One handler for every kind of row, wrapped once, so no branch can forget it.
            assert rowSrc.contains("event.buttons.isSecondaryPressed")
                    : "the reset is reached by right-clicking the row";

            // ---- two materials, switchable -----------------------------------------------------
            // FROSTED and LIQUID_GLASS are different MATERIALS, not two strengths: one says much of
            // the world comes through, the other says what it comes through. A player can want a
            // strong frost or a barely-there pane of either kind, so they are separate choices.
            String layoutSrc2 = Files.readString(Path.of(
                    "src/main/java/com/yuan/client/aurora/AuroraLayout.java"));
            assert layoutSrc2.contains("public enum MaterialMode {")
                    && layoutSrc2.contains("FROSTED(\"液态模糊\")")
                    && layoutSrc2.contains("LIQUID_GLASS(\"液态玻璃\")")
                    : "the two materials must be a choice the player makes";
            assert layoutSrc2.contains("public MaterialMode material = MaterialMode.FROSTED;")
                    : "frosted must be the default: it is the cheaper pass and what existing configs look like";
            // The refraction is its own shader: the blur smears the whole screen, the glass refracts
            // inside a rectangle, and they share nothing but a tint.
            String glassFsh = Files.readString(Path.of(
                    "src/main/resources/assets/yuan/shaders/core/aurora_glass.fsh"));
            assert glassFsh.contains("roundedBoxSdf")
                    : "the refraction must follow the panel's own rounded shape, not its bounding box";
            assert glassFsh.contains("pow(edge, 2.0)")
                    : "the bend must be confined to the rim; a linear ramp warps the whole pane";
            assert glassFsh.contains("discard")
                    : "outside the rounded shape the quad must discard, or it is a rectangle of glass";
            assert glassFsh.contains("Sampler0")
                    : "the glass must sample the already-blurred backdrop";
            // A shader that declares a uniform it never reads has that uniform dropped by GLSL 150,
            // the Java lookup then returns null and the whole pass silently skips.
            assert glassFsh.contains("ScreenTexelSize")
                    : "the pixel-to-UV divisor must be declared and read";
            String fxSrc = Files.readString(Path.of(
                    "src/main/java/com/yuan/client/aurora/AuroraFx.java"));
            assert fxSrc.contains("public static boolean drawGlass(")
                    : "the renderer must expose a per-panel glass draw";
            assert fxSrc.contains("y0 = 1f - (yPx / screenH) * 2f")
                    : "GL's Y grows upward while the layout's grows down; the flip is required";
            // KEYED AND PERSISTENT, NOT REBUILT PER FRAME. `onGloballyPositioned` fires when a node's
            // position CHANGES, not once per frame, so a registry cleared every frame would be
            // refilled only on frames where something moved -- and the glass would vanish the moment
            // the window settled. Panels register under an id and withdraw on dispose.
            String composeSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/AuroraComposeScreen.kt"));
            String rectSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/GlassRects.kt"));
            assert rectSrc.contains("fun put(") && rectSrc.contains("fun snapshot()")
                    && rectSrc.contains("fun remove(")
                    : "the panel rectangles must be published, read, and withdrawn on dispose";
            assert !rectSrc.contains("fun clear()")
                    : "a per-frame clear cannot work: the publish only fires on layout changes";
            // Withdrawal is what keeps glass from outliving its panel.
            assert shell.contains("GlassRects.remove(title)")
                    : "a panel must withdraw its rectangle when it leaves";
            // In glass mode the panel must NOT paint its own background: the refraction is drawn
            // before the composition, so a flat pane would cover the very thing it is made of.
            String lookSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/AuroraLook.kt"));
            assert lookSrc.contains("Color.Transparent")
                    && lookSrc.contains("MaterialMode.LIQUID_GLASS")
                    : "in glass mode the panel background must be transparent, or it hides the glass";
            // The pass runs after the blur it samples and before the composition it sits under.
            {
                String code = stripComments(composeSrc);
                int blur = code.indexOf("blurBackdrop(mc)");
                int glass = code.indexOf("drawLiquidGlass(mc)");
                assert blur >= 0 && glass > blur
                        : "the glass must sample the blurred backdrop, so it runs after it";
            }
            // A Java enum field can be null after Gson reads a value that no longer exists.
            assert composeSrc.contains("?: AuroraLayout.MaterialMode.FROSTED")
                    && composeSrc.contains("?: AuroraLayout.GlassLevel.MEDIUM")
                    : "reading these from a hand-edited file must not be able to crash";

            // ---- C-7: pinned entries ----------------------------------------------------------
            // Ids rather than positions: the rail can be reordered and a module can be added later,
            // and a stored index would then point at whatever slid into that slot.
            assert layoutSrc2.contains("public java.util.Set<String> pinnedModules")
                    : "pins must be stored by id, not by position";
            // The null guard belongs at the READ, in Kotlin: Gson leaves a field null when the file
            // predates it, and a guard inside the Java model means guessing which of that file's
            // several methods owns the field -- getting it wrong compiles to "cannot find symbol"
            // against the outer class, which is what happened here twice.
            assert screenSrc.contains("layout.look.pinnedModules ?: emptySet()")
                    : "a missing set must degrade to no pins, not throw while drawing the rail";
            // ONE collection answers both "is this pinned" and "which pin is first": a set plus a
            // separate order list would be two places for the pins to live.
            assert grid.contains("val pinnedSet = pinned.toSet()")
                    : "membership and order must come from the same list";
            // Pins move an entry UP within the list the player knows, rather than hoisting it into a
            // separate region -- which would restructure the rail every time one was toggled.
            assert grid.contains("section.entries.sortedBy { entry ->")
                    : "pinning must reorder inside the section, not restructure the rail";
            // What is DRAWN must be what the drag maths measured, or a drag lands at the wrong index.
            assert grid.contains("sortedSections.forEach { section ->")
                    && grid.contains("val flat = sortedSections.flatMap")
                    : "the drawn order and the measured order must be the same list";
            assert screenSrc.contains("layout.look.pinnedModules") || screenSrc.contains("pinnedModules")
                    : "the screen must read and write the pins";
            assert screenSrc.contains("layout.save()")
                    : "a pin is arrangement data and must persist";
            // The pin shares the row's right end with the grip rather than sitting beside it: two
            // glyphs would spend a third of the row on symbols.
            assert !grid.contains("AuroraIcon.Grip,\n                        tint = Star.textTertiary,\n                        modifier = Modifier.size(IconSize.inline)\n                    )\n                    Box")
                    : "the pin must replace the grip, not stack beside it";

            // ---- the window's own entrance and exit -------------------------------------------
            // `requestClose()` is synchronous: Minecraft tears the screen down inside the call, so an
            // exit animation has to finish BEFORE it, not during. Closing therefore takes two steps.
            assert screenSrc.contains("var closing by remember { mutableStateOf(false) }")
                    : "the window needs a closing state to animate out through";
            assert screenSrc.contains("val beginClose = {")
                    : "every close path must go through one wrapper";
            assert screenSrc.contains("LaunchedEffect(closing)")
                    : "the handover to requestClose must wait for the animation";
            // ONE wrapper: the close button, the confirm dialog and the ESC hatch used to call
            // requestClose directly, which left the ESC path as the only one that slammed shut.
            assert !screenSrc.contains("dirty = false\n                            requestClose()")
                    : "the confirm dialog must animate out too";
            // THE ROOT LAYER MUST BE CONDITIONAL, and this assertion is what catches the regression.
            // A `graphicsLayer` on the composition root turns the whole window into one cached layer;
            // on this scene the picture then stops following the model while the controls keep
            // responding -- reported as "设置界面没反应，但是你拖动，点击实际有效果".
            //
            // The read is in composition because the CONDITION depends on it, which is the price of
            // correct repainting; the layer itself is absent whenever the ramp has settled.
            // ---- three ways to set one slider value ---------------------------------------------
            String ctlSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/scaffold/AuroraControls.kt"));
            assert ctlSrc.contains("default: Float? = null")
                    : "a slider needs to know its default before it can offer a reset";
            // ONE pointerInput carries all three gestures. Two chained pointerInput blocks on one
            // node is the consume-order trap this file has paid for before: whichever runs first
            // owns the stream and the other never starts.
            assert ctlSrc.contains("PointerEventType.Scroll")
                    : "the wheel must nudge the slider, or a precise value is unreachable by pointer";
            assert ctlSrc.contains("400_000_000L")
                    : "a double-click must be distinguishable from two presses";

            // ---- search results are ranked -----------------------------------------------------
            String modSrc2 = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordModules.kt"));
            assert modSrc2.contains("matchRank")
                    : "a search must rank its hits, or the best answer looks like all the others";
            assert modSrc2.contains("compareByDescending<SearchHit> { it.rank }")
                    : "the best match must come first";
            assert modSrc2.contains(".thenBy { it.rowName }")
                    : "equal-quality hits need one stable order, or the list reorders while typing";
            assert modSrc2.contains("n.startsWith(q)")
                    : "a name that LEADS with the query outranks one that merely contains it";

            // ---- and Enter takes the answer ----------------------------------------------------
            // ---- section headings fold ---------------------------------------------------------
            String foldSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordInlinePanel.kt"));
            // Folded state is data, passed in, so the crossfade's outgoing copy cannot read a
            // layout that has already moved on.
            assert foldSrc.contains("foldedSections: Set<String>")
                    : "the panel must receive folds as a value, not reach into the layout";
            assert foldSrc.contains("onToggleFold: (String) -> Unit")
                    : "the panel must report folds to its caller, which owns persistence";
            // A search OVERRIDES the fold: asking for these rows must not be answered with a heading.
            assert foldSrc.contains("query.isNotEmpty() || !folded")
                    : "a search must show folded sections, or it answers with the wrong thing";
            // The rows COLLAPSE, they do not vanish: a section that disappeared in one frame would
            // make everything below it jump up the page.
            assert foldSrc.contains("AnimatedVisibility(")
                    && foldSrc.contains("shrinkVertically(")
                    : "folding must animate the height, not cut the rows out";
            // The heading is the target, and it states the state without counting rows.
            assert foldSrc.contains("private fun SectionLabel(name: String, folded: Boolean, onToggle: () -> Unit)")
                    : "the heading must be the fold control";
            // ---- a reorder shows both ends ---------------------------------------------------
            // Only the destination used to be marked, so with a tinted target and an ordinary
            // source it was easy to lose track of the row under the button.
            assert grid.contains("val isSource = dragFrom == index")
                    : "a drag must mark the row being carried, not only its destination";
            // ---- a row says whether it was changed -----------------------------------------
            String markSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordBinding.kt"));
            String panelSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordPanel.kt"));
            assert markSrc.contains("internal fun isModified(")
                    : "the binding must be able to compare a row against its default";
            // Same dispatch as the reset, so the mark can never disagree with the button beside
            // it: a row that shows unmodified is exactly one reset would leave alone.
            assert markSrc.contains("TYPE_SWITCH -> readBool(id, current) != readBool(id, defaults)")
                    : "the mark must compare through the same reader the reset writes through";
            assert markSrc.contains("TYPE_SLIDER -> readFloat(id, current) != readFloat(id, defaults)")
                    : "sliders must compare in the stored unit, like the reset does";
            // Read at the LEAF that already re-reads the holder, beside the control it describes.
            assert panelSrc.contains("val modified = GodSwordBinding.isModified(godRow, holder)")
                    : "the mark must be read where the value is read";
            // The RAIL answers "where did I change something": a dot inside a feature says WHICH
            // setting, and nothing said which of the seven features had any setting at all.
            assert grid.contains("modifiedCount: (String) -> Int")
                    : "the rail must be able to show a per-module change count";
            assert grid.contains("if (changed > 0)")
                    : "a count of zero must not be drawn: seven zeroes train the eye to ignore the "
                    + "position where the information appears";
            assert screenSrc.contains("modifierCount = ")
                    || screenSrc.contains("modifiedCount = { moduleId ->")
                    : "the screen must supply the counts";
            // The count comes from the SAME comparison as the row dots, so rail and page can never
            // disagree about what has been changed.
            // ---- a preset can cross to another sword ---------------------------------------
            // A preset lives in the sword's NBT, so it already travels through a relog, a chest or
            // a trade. The only boundary it did not cross was a DIFFERENT sword -- and the clipboard
            // is the one channel that crosses it without inventing a second store on disk that would
            // drift from the item.
            String presetSrc = Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/AuroraComposeScreen.kt"));
            assert presetSrc.contains("internal fun exportPreset(slot: Int)")
                    : "a preset must be copyable out";
            assert presetSrc.contains("setClipboard(stored.toString())")
                    : "the copy must be the tag the game itself saves, in NBT text";
            assert presetSrc.contains("internal fun importPreset(")
                    : "a preset must be pasteable in";
            // Vanilla's own parser, so a paste is whatever NBT prints.
            assert presetSrc.contains("TagParser.parseTag(text)")
                    : "the paste must be parsed by NBT's own syntax";
            // PRESENCE, not "did read() throw": read() fills only present keys and leaves the rest
            // at defaults, so ANY well-formed compound parses successfully and would then be written
            // into the sword as an all-default preset.
            assert presetSrc.contains("knownKeys.none { parsed.contains(it) }")
                    : "a pasted tag must carry at least one declared key, or it is not a preset";
            assert presetSrc.contains("holder.restore(")
                    : "after pasting, the panel must redraw with the loaded values";

            assert screenSrc.contains("GodSwordBinding.modifiedCount(rowsFor(module, holder), holder)")
                    : "the rail count must use the same comparison as the dots";

            assert panelSrc.contains("modified = modified")
                    : "every row must pass its own mark through";

            assert grid.contains("alpha = 0.55f")
                    : "the carried row must visually lift off the list";

            assert foldSrc.contains("rotationZ = 180f * turn")
                    : "the marker must turn with the fold, or the triangle and the rows read as two events";

            assert screenSrc.contains("searchHits.getOrNull(focus.index)")
                    : "Enter must open the focused search hit";

            assert screenSrc.contains("val appearNow = windowAppear.value")
                    : "the window ramp must be read where the conditional layer can see it";
            assert screenSrc.contains("val fading = appearNow < 0.999f")
                    : "the root layer must exist only while the window is animating";
            assert screenSrc.contains("if (fading)")
                    && screenSrc.contains("Modifier.graphicsLayer {")
                    : "the layer must be attached conditionally, or the window stops repainting";
        }

        // The rail's marker is the exception: a bar rather than a surface. Its COLOUR is now a
        // resolved value (entry -> category -> rail), so the literal accent is the default rather
        // than the only possibility -- the marker must still be a bar and still take its tint from
        // the cascade, not from a hard-coded constant.
        assert grid.contains("if (selected) markerColor else Color.Transparent")
                : "the rail's active marker must be a bar tinted by the resolved cascade";
        assert grid.contains("val markerColor = Color(resolved.markerColor)")
                : "the marker's colour must come from the scope cascade";
        assert controls.contains("Star.on")
                : "text still states ON by brightness, so a filled control is not the only signal";
        // This variable is referenced below; keeping it named makes the intent of the count visible.
        int accentMarkerUses = countOccurrences(grid, "Star.starlight");
        assert accentMarkerUses <= 4
                : "the identity mark may appear in the content header and nowhere repeated; found "
                        + accentMarkerUses + " uses";

        // ---- density: the controls are small, and that is what makes the list fit ---------------
        assert theme.contains("detailRowHeight") && theme.contains("sectionHeight")
                : "row and section pitches must be tokens, so a list can be tightened in one edit";
        assert theme.contains("36.dp") && theme.contains("20.dp")
                : "the toggle must stay the small pill size the reference uses";
        assert theme.contains("duplicateColumnMinWidth")
                : "the two-column threshold must be a named token, not a number at the call site";

        // ---- the gestures are resident; no mode arms them ---------------------------------------
        // The arrangement mode is retired. The failure it kept reintroducing is a persisted flag
        // gating input: a flag that outlived the window silently disabled navigation, and grips that
        // existed only inside the mode were invisible outside it -- reported as "cannot be moved at
        // all". What must hold now is the opposite property, asserted on comment-stripped sources:
        // no gesture reads a mode flag, and the gesture arming reads ONE switch (gesturesEnabled),
        // which the settings page owns and which does not gate selection or menus.
        assert !stripComments(grid).contains("arrangeMode")
                : "the rail's gestures must not read an arrangement-mode flag";
        assert !stripComments(shell).contains("arrangeMode")
                : "the shell's gestures must not read an arrangement-mode flag";
        assert countOccurrences(stripComments(grid), "clickable(interactionSource = interaction, indication = null) {") >= 2
                : "selection and collapse clicks must remain unconditional";
        // The one allowed gate is the player's own gesture switch -- and it may gate only the
        // window-move machinery, never the rail's selection or the right-click menus.
        assert windowSrc.contains("if (gestures && !maximized)")
                : "the gesture frame arms on the player's switch, not on a mode";


        // ---- geometry is resolution-independent ----------------------------------------------------
        // Panels are placed in fractions of the content area. Pixel coordinates would put every panel
        // in the top-left corner of a larger window, with the whole arrangement overlapping.
        // Panels resolve their stored fractions through the shared gesture frame now: the frame takes
        // the measured stage in PIXELS (`stagePx`) and multiplies the fractions itself, which is the
        // same resolution-independence the old direct multiplication carried, stated once for every
        // window instead of per panel. The Dp stage the panel hands in is converted with the density.
        assert shell.contains("* densityValue") && shell.contains("stagePx")
                : "panels must hand the frame a pixel stage converted through the density";
        assert windowSrc.contains("box.x * screenW") && windowSrc.contains("box.w * screenW")
                : "the gesture frame must resolve every fraction against the measured stage";
        String layout = Files.readString(Path.of(
                "src/main/java/com/yuan/client/aurora/AuroraLayout.java"));
        assert layout.contains("config.resolve(\"yuan/aurora.json\")")
                : "the layout must live in its own global config file, not on the sword";
        assert !layout.contains("YuanGodSwordConfig") && !layout.contains("ItemStack")
                : "the layout must not be tied to one weapon: a new sword would start with no layout";
        assert layout.contains("private void repair()")
                : "loading must repair a corrupt layout instead of throwing on open";
        assert countOccurrences(layout, "clampBox()") >= 2
                : "a dragged box must be clamped on every delta, not only at load";
        assert screen.contains("layout.save()") && screen.contains("override fun removed()")
                : "the layout must be persisted when the window closes";

        // ---- layout edits must reach the composition ----------------------------------------------
        // The layout is a plain object outside Compose. Without a subscription point a settings edit
        // would mutate it and the window would never re-read: the toggle would move and nothing else
        // would change.
        assert screen.contains("var layoutRevision by mutableStateOf")
                : "the layout needs one revision counter the composition subscribes to";
        assert godScreenSrc.contains("val revision = layoutRevision")
                : "the window must read the layout revision so edits cause a re-read";
        assert countOccurrences(godScreenSrc, "touchLayout()") >= 5
                : "every layout mutation must record a revision, found "
                        + countOccurrences(godScreenSrc, "touchLayout()");

        // ---- drag state must be Compose state ------------------------------------------------------
        // The parallax offset and the dragged id are read inside graphicsLayer and by row backgrounds.
        // A plain field would change without telling Compose to redraw, so the effect would silently do
        // nothing while the drag still worked -- a bug that errors nowhere and looks like "it just
        // doesn't happen".
        assert shell.contains("var parallaxY: Float by mutableStateOf")
                : "panel parallax must be Compose state or it never redraws";

        // ---- one timing table, one motion switch ---------------------------------------------------
        assert countOccurrences(controls, "Motion.") >= 3
                : "controls must take their timing from the Motion table, not raw durations";
        assert motion.contains("var level") && motion.contains("fun dur(")
                : "the motion level must be one switch that scales the whole table";
        assert motion.contains("baselineMs <= QUICK_MS")
                : "the calm level must keep short feedback while dropping long transitions";

        // ---- entrances are keyed by IDENTITY, not by delay --------------------------------
        // `staggerDelay` is capped, so every row past the cap receives the SAME delay. The effect
        // key used to be the delay alone, and the "have we entered yet" flag was scoped by
        // position: a reorder could leave a row holding a completed neighbour's effect with its
        // own flag still false -- a row that occupies layout space and is fully transparent. That
        // is a missing row, not an animation flaw.
        assert motion.contains("identity: Any,")
                : "EnterBlock must take a stable identity";
        assert motion.contains("remember(identity)")
                : "the entrance flag must be scoped to the identity, not the position";
        assert motion.contains("LaunchedEffect(identity, delayMs)")
                : "the entrance effect must be keyed by identity, not by a shared delay";
        // Every call site must actually pass one.
        {
            String inlineCode = stripComments(Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordInlinePanel.kt")));
            String panelCode = stripComments(Files.readString(Path.of(
                    "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordPanel.kt")));
            assert !inlineCode.contains("EnterBlock(delayMs = staggerDelay(index), fromY = 6)")
                    : "an entrance without identity can collide with a neighbour";
            assert inlineCode.contains("identity = group.id")
                    : "the inline panel must key entrances by the group id";
            assert panelCode.contains("identity = row.id")
                    : "a floating panel must key entrances by the row id";
        }

        // ---- animations that only DRAW must not recompose --------------------------------
        // `by animateFloatAsState(...)` reads during composition, so every animation frame
        // recomposes the control and its children. Held as State and read inside
        // `graphicsLayer {}` / a draw lambda, the read happens in the draw phase: the motion is
        // identical and the composable is not re-executed per frame.
        assert controls.contains("val slide = animateFloatAsState(")
                && controls.contains("translationX = slide.value * travel")
                : "the toggle knob must animate in a layer, not by recomposing";
        assert controls.contains("val plate = animateFloatAsState(")
                && controls.contains("val x = segW * plate.value + inset")
                : "the segment plate must animate in a draw lambda, not by recomposing";
        // Comment-stripped: the fix's own explanation quotes `by animateFloatAsState(...)`, and a
        // raw scan reads that comment as a live violation -- trap #1 in this file's history.
        assert !stripComments(controls).contains("by animateFloatAsState(")
                : "no animation value may be unwrapped with `by` where it only feeds drawing";
        // The cursor overlay positions itself by layer for the same reason.
        assert windowSrc.contains("translationX = state.x + gutterPx")
                && !windowSrc.contains("with(LocalDensity.current) { state.x.toDp() }")
                : "the resize cursor must be positioned by layer, not by a layout offset";

        // ---- the surface entrance -------------------------------------------------------
        // `openProgress` used to be `animateFloatAsState(targetValue = 1f)`, which is created ALREADY
        // AT its target: there is no transition to run, so it read 1f on the first composition and
        // never moved. That is why it had no callers -- not because nobody wanted a panel entrance,
        // but because it did nothing when they tried. A floating panel therefore POPPED into
        // existence, which is most of what "动画不够流畅" was about.
        // The ramp now runs in BOTH directions -- a dismissal is the mirror of its entrance -- so it
        // starts at whichever end it is not at and animates toward the target.
        assert motion.contains("val anim = remember(key) { Animatable(if (shown) 0f else 1f) }")
                : "the ramp must start at the end it is not at; animate*AsState(target) never runs";
        assert motion.contains("targetValue = if (shown) 1f else 0f")
                : "the ramp must drive toward the shown/hidden target, so dismissal mirrors entrance";
        // Gated through `dur` with the DURATION AS A PARAMETER: the window's own exit needs the full
        // window timing, while a small surface reads as slow at that length and passes a shorter one.
        assert motion.contains("animationSpec = tween(Motion.dur(durationMs)")
                : "the surface ramp must be gated through Motion.dur, at the caller's duration";
        assert motion.contains("if (!Motion.animated)")
                : "at OFF the entrance must collapse to complete, not animate";
        // Its one consumer, and the only legal place to read it: a draw phase.
        assert shell.contains("val appear = openProgress(title, shown = !dismissing)")
                : "the floating panel must drive the ramp from its dismissing flag";
        assert shell.contains("val p = appear.value")
                : "the entrance must be read inside graphicsLayer, not during composition";
        assert !stripComments(shell).contains("openProgress(title).value")
                : "reading the ramp during composition would recompose the panel every frame";

        // ---- the ambient pulse, and its one consumer --------------------------------------
        // It changes EVERY frame, so a composition-phase read recomposes the row (and its subtree)
        // sixty times a second for the whole session. Its consumer must read it inside a draw
        // lambda; this assertion is the difference between a cheap glow and a permanent frame tax.
        assert motion.contains("ONLY DRAWING CODE MAY READ THIS")
                : "the pulse's contract must stay stated in the source";
        assert motion.contains("if (!Motion.staged)")
                : "below FULL the pulse must be a constant, not a slow animation";
        assert grid.contains("val pulseSource = if (selected) rememberAmbientPulse() else null")
                : "the selected marker is the pulse's consumer";
        {
            String code = stripComments(grid);
            int at = code.indexOf("val pulseSource =");
            int end = code.indexOf("drawBehind", at);
            assert at >= 0 && end > at;
            String draw = code.substring(end, Math.min(code.length(), end + 700));
            assert draw.contains("pulseSource.value")
                    : "the pulse must be read in a draw lambda (drawBehind), never in composition";
            assert !code.contains("alpha = pulseSource")
                    : "a composition-phase pulse read would tax every frame";
        }
        // The breathing marker stays a MARKER: it must not become a filled plate behind the label.
        assert grid.contains(".drawBehind {")
                && !grid.contains("background(markerTint")
                : "the marker must be drawn, not a filled plate behind the label";

        // ---- the inline / collapsed split -----------------------------------------------------------
        // The grid these assertions used to cover was removed when the rail went back to showing a
        // feature's settings directly. What replaces it is the rule that a section too long to fit the
        // column, or holding a control that is not a single value, collapses to a row that opens its
        // own panel -- and only that long form is ever drawn.
        assert grid.contains("onSelect(entry.id)")
                : "the rail must remain the selector the content area follows";
        String inline = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordInlinePanel.kt"));
        assert inline.contains("groupNeedsPopup(group)")
                : "the panel decision must consult the shared rule, not a local row count";

        // ---- one search field and one count on screen ---------------------------------------------
        // Duplicating either puts two identical controls in front of the player and makes them work
        // out whether they filter the same thing.
        String godScreen = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/AuroraGodSwordScreen.kt"));
        assert !shell.contains("search: @Composable") && shell.contains("actions: @Composable")
                : "the shell header takes window actions, not a second search field";
        assert countOccurrences(godScreen, "AuroraSearchField(") == 0
                : "the search field is built once, inside the content header, not again at the top";

        // ---- every declared row must be readable and writable ------------------------------------
        // The failure this catches is silent, which is why it is a guard rather than a review step: a
        // row whose id is missing from the binding renders as a normal control, and interacting with it
        // changes nothing -- no exception, no log line, just a slider that springs back. Twelve rows
        // were in exactly that state the first time this check ran.
        String rows = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordRows.kt"));
        String binding = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordBinding.kt"));
        java.util.Set<String> rowIds = new java.util.TreeSet<>();
        rowIds.addAll(idsMatching(rows, "V9Row\\(\"(\\w+)\""));
        rowIds.addAll(idsMatching(rows, "\\b(?:switch|segment|slider)\\(\"(\\w+)\""));
        // The 时停 and 领域 rows are taken from the older screen's own section list at runtime, so
        // their ids appear in that file and nowhere in these tables. Scanning only the tables would
        // leave those twenty-nine rows outside the check entirely -- which is exactly the hole a fault
        // injection found: a missing binding for one of them passed this guard cleanly.
        String sections = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/gui/V9Sections.kt"));
        rowIds.addAll(idsMatching(sections, "V9Row\\(\"(\\w+)\""));
        java.util.Set<String> readIds = new java.util.TreeSet<>();
        readIds.addAll(idsInFunction(binding, "readBool"));
        readIds.addAll(idsInFunction(binding, "readFloat"));
        readIds.addAll(idsInFunction(binding, "readIndex"));
        java.util.Set<String> writeIds = new java.util.TreeSet<>();
        writeIds.addAll(idsInFunction(binding, "writeBool"));
        writeIds.addAll(idsInFunction(binding, "writeFloat"));
        writeIds.addAll(idsInFunction(binding, "writeIndex"));
        java.util.List<String> noRead = new java.util.ArrayList<>();
        java.util.List<String> noWrite = new java.util.ArrayList<>();
        // Two rows carry multi-part controls instead of a scalar: the swatch and the preset slots.
        // Each is drawn by a dedicated composable and has no single value for a binding to read, so
        // they are excluded here and asserted separately below.
        java.util.Set<String> multipart = new java.util.TreeSet<>(java.util.Arrays.asList("ballColor", "preset"));
        for (String id : rowIds) {
            if (multipart.contains(id)) continue;
            if (!readIds.contains(id)) noRead.add(id);
            if (!writeIds.contains(id)) noWrite.add(id);
        }
        String special = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordSpecialRows.kt"));
        assert special.contains("fun BallColourRow(") && special.contains("fun PresetRow(")
                : "the two multi-part rows must have their own controls, or they render as blank rows";
        assert special.contains("ballColors") && special.contains("customColor")
                : "the swatch must offer the presets and the custom channels";
        assert rowIds.size() > 80
                : "the row tables must be read whole; only " + rowIds.size() + " ids were found";
        assert noRead.isEmpty()
                : "these rows would render but read nothing: " + String.join(", ", noRead);
        assert noWrite.isEmpty()
                : "these rows would render but never save: " + String.join(", ", noWrite);

        // ---- the real config, not a parallel model ------------------------------------------------
        // The window must edit through the existing holder and round trip. A second config model would
        // have to re-implement the write, the live preview, the preset list and the server sync, and
        // the first of those to be subtly wrong would corrupt a saved sword.
        assert godScreen.contains("V9Holder(v9StateOf(editingConfig))")
                : "the window must edit through the existing config state, not a parallel model";
        assert godScreen.contains("holder.snapshot().toConfig()")
                : "an edit must round trip back through the existing config conversion";
        assert godScreen.contains("previewConfig(") && godScreen.contains("saveConfig()")
                : "both write paths must be wired: the live preview and the real save";
        assert godScreen.contains("discardPreview()")
                : "leaving with unsaved edits must restore the client copy";
        assert screen.contains("protected fun saveConfig()") && screen.contains("protected fun loadConfig(")
                : "the config lifecycle belongs in the base class, so a subclass cannot load without a "
                        + "baseline and report unsaved changes from the moment it opens";

        // ---- three levels, and at most one editor ---------------------------------------------------
        // The screenshot that produced this guard showed ten panels piled up: the rail callback appended
        // to a list of open panels instead of switching the grid, so visiting every rail entry stacked
        // ten editors on top of each other. The reference has three levels -- rail, grid, editor -- and
        // the editor is singular.
        // The single-slot rule is retired: panels cascade (the reference opens a second without losing
        // the first) and the pin button tiers them. What the old guard existed to prevent was an
        // APPEND-per-click pile-up; the replacement holds the same line differently -- opening the
        // same group twice must raise, not duplicate, and the draw order must come from depth.
        assert godScreen.contains("var openPanels by remember { mutableStateOf(listOf<String>()) }")
                : "the open panels are an explicit list the composition owns";
        assert godScreen.contains("openPanels.contains(opened.id)")
                : "opening an open group must raise its panel instead of stacking a copy";
        assert godScreen.contains(".sortedBy { (id, _) ->")
                : "panels must draw in depth order so raise is visually meaningful";
        assert godScreen.contains("layout.setPinned") || shell.contains("setPinned(panelId")
                : "the pin must round-trip through the model's re-tiering";
        // ---- several panels, across modules --------------------------------------------------
        // The list held ids from every module, but rendering resolved them against the CURRENT
        // module's groups only -- so switching the rail made the others vanish, and the stale sweep
        // deleted them for good. "多个浮层" could therefore never actually work.
        assert godScreen.contains("groupOfPanelId(")
                : "a panel id must resolve against its OWN module, not the one on screen";
        assert godScreen.contains("id.indexOf('/')")
                : "the id's module/heading shape is what makes a panel self-describing";
        assert !godScreen.contains("val liveIds = groups.map { it.id } union allGroups.map { it.id }")
                : "stale must not mean 'absent from the current module'; other modules stay open";
        // ---- quick reset per page --------------------------------------------------------------
        String resetLayoutSrc = Files.readString(Path.of(
                "src/main/java/com/yuan/client/aurora/AuroraLayout.java"));
        String resetMenuSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/ArrangeMenu.kt"));
        assert resetLayoutSrc.contains("public void resetLookGroup(")
                : "a page-level reset must exist on the model, so it can reset a named group";
        assert resetMenuSrc.contains("resetLookGroup(resetGroup)")
                : "the menu header must offer the quick reset";
        assert resetMenuSrc.contains("ArrangeTarget.Controls -> \"controls\"")
                : "each page must map to the group it owns";

        // ---- three rail scopes, cascading ------------------------------------------------
        // The rail is configured at three levels: the whole column, one category, one entry. The
        // cascade must be entry -> category -> rail, and "not set at this scope" must be a
        // genuinely distinct state from "set to zero" -- otherwise the inheritance cannot exist.
        //
        // That distinction is carried by a primitive field PLUS an explicit `has` flag, not by a
        // boxed type. Boxed worked, but `resolveRail` runs for every entry of every frame the rail
        // is laid out, and each unbox there is a branch-heavy access on a value that is almost
        // always present; the flag says the same thing with no boxing at all.
        assert resetLayoutSrc.contains("public Map<String, ScopeLook> moduleLooks")
                && resetLayoutSrc.contains("public Map<String, ScopeLook> sectionLooks")
                : "entry- and category-scope overrides must have their own stores";
        assert resetLayoutSrc.contains("public float itemHeight;")
                && resetLayoutSrc.contains("public boolean hasItemHeight;")
                && resetLayoutSrc.contains("public int markerColor;")
                && resetLayoutSrc.contains("public boolean hasMarkerColor;")
                : "each scope field needs a primitive value and a `has` flag: unset is not zero";
        // The flag must be the thing the resolver reads -- a value that is set but whose flag is
        // never consulted would silently override, which is the failure this pairing prevents.
        assert resetLayoutSrc.contains("m.hasItemHeight ? m.itemHeight")
                : "the resolver must test the `has` flag, not the value";
        assert resetLayoutSrc.contains("public Resolved resolveRail(String moduleId, String sectionId)")
                : "the cascade must be resolved in one place";
        // Order: the entry's value wins, then the category's, then the rail's.
        {
            String code = stripComments(resetLayoutSrc);
            int at = code.indexOf("public Resolved resolveRail(");
            int end = code.indexOf("public ScopeLook moduleScopeFor(", at);
            assert at >= 0 && end > at;
            String body = code.substring(at, end);
            assert body.contains("m.itemHeight")
                    && body.contains("s.itemHeight")
                    && body.contains("base.railItemHeight")
                    : "resolveRail must consult entry, then category, then rail";
            assert body.indexOf("m.hasItemHeight") < body.indexOf("s.hasItemHeight")
                    : "the entry scope must be tried before the category scope";
        }
        // A clear returns ONE field to the parent, and an emptied scope is dropped.
        assert resetLayoutSrc.contains("public void clearModuleField(String moduleId, String field)")
                && resetLayoutSrc.contains("if (s.isEmpty()) moduleLooks.remove(moduleId)")
                : "clearing a field must return it to inheritance and drop an emptied scope";
        // Three right-click scopes on the rail: entry, category, rail.
        assert grid.contains("onOpenEntryMenu(entry.id, section.id)")
                : "a right-click on an entry must open that entry's scope";
        assert grid.contains("onOpenSectionMenu(")
                : "a right-click on a category must open the category scope";
        assert resetMenuSrc.contains("ArrangeTarget.Entry -> EntryNumbers(")
                : "the entry scope needs its own page";
        assert resetMenuSrc.contains("private fun ScopeFieldRows(")
                : "entry and category pages must share one field list";
        String inlineSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordInlinePanel.kt"));        // The current shape: selecting a rail entry shows the feature's settings directly, and a
        // section that does not fit the column collapses to a row that opens its own panel.
        //
        // An earlier revision put a grid of sections between the rail and the settings, which made
        // reaching any setting two clicks and told the player nothing the headings underneath it
        // would not have said. This guard used to assert that grid existed; it now asserts the
        // opposite, with the reasoning kept so the next reader does not reinstate it.
        assert godScreen.contains("GodSwordInlinePanel(")
                : "a rail entry must show its feature's settings directly, with no intermediate step";
        assert !godScreen.contains("AuroraGroupGrid(")
                : "no grid between the rail and the settings: it was two clicks for no information";
        assert godScreen.contains("GodSwordModules.groupsOf(")
                : "a feature must be split into groups, or it renders as one undivided column";
        // The emptiness test now reads the CROSSFADE's own group list: while an outgoing frame is on
        // screen, `groups` already belongs to the module being switched to, so testing it would
        // describe the wrong feature.
        assert godScreen.contains("contentGroups.isEmpty()")
                : "an empty feature must say so rather than drawing an empty column";

        // ---- the popup rule: what does not fit a line goes to its own panel -----------------------
        // Two reasons, and the second is the one a row count cannot see: a section longer than the
        // The single reason left is a control that cannot sit on a settings line. Length is what the
        // two-column layout and the scroll solve -- an earlier revision hid long sections behind a
        // disclosure, which was a workaround for having only one column.
        assert inlineSrc.contains("if (query.isEmpty() && GodSwordModules.groupNeedsPopup(group))")
                : "only a section holding a multi-part control may open its own panel";
        assert inlineSrc.contains("BoxWithConstraints") && inlineSrc.contains("duplicateColumnMinWidth")
                : "the content area must lay out its two columns from the measured width in Dp";
        // THE SWITCH IS A MORPH, NOT A BRANCH.
        //
        // It used to compose either a Column or a Row depending on the width, which replaced every
        // child in a single frame -- the "硬切". `groups.take(half)` / `groups.drop(half)` existed
        // only because there were two trees; now one list is placed by one interpolation, and the
        // split lives inside MorphingColumns.
        //
        // The properties worth guarding are therefore: the switch is ANIMATED (a value derived
        // straight from the width arrives in one step, because a resize IS one step), and the
        // children are still ONE list (two trees is the bug).
        assert inlineSrc.contains("animateFloatAsState(")
                && inlineSrc.contains("label = \"columnMorph\"")
                : "the column switch must be animated; a width-derived value arrives in one step";
        assert inlineSrc.contains("MorphingColumns(")
                : "the columns must be laid out by the morphing layout";
        assert !inlineSrc.contains("groups.take(half)")
                : "two separately composed trees cannot be eased between; use one child list";
        String morph = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/MorphingColumns.kt"));
        // Fill DOWN: the split point is the first ceil(n/2), so a section is never cut in two.
        assert morph.contains("val half = (total + 1) / 2")
                : "columns must fill down rather than across, so a section stays whole";
        // The progress is read in COMPOSITION. A State read inside MeasureScope sits outside the
        // snapshot observer, so the layout never learns it changed -- the morph would be present,
        // plausible, and motionless.
        assert morph.contains("val p = progress.value.coerceIn(0f, 1f)")
                : "the morph progress must be read where a change is observed";
        String modules = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordModules.kt"));
        assert modules.contains("internal fun groupNeedsPopup(")
                : "the popup rule must be stated once, where the groups are defined";

        String rowsSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordRows.kt"));
        assert rowsSrc.contains("val wantsPanel: Boolean")
                : "a row must declare for itself whether its control fits a settings line";
        // A search must render inside a normally-collapsed section: answering "it is in a panel
        // somewhere" would make the search useless for exactly the sections it helps most.
        assert inlineSrc.contains("query.isEmpty()") && inlineSrc.contains("staggerDelay(")
                : "a search result must render inline even inside a section that opens its own panel";

        // ---- every rail entry must resolve to rows -------------------------------------------------
        // The bug this guards: 时停's rail id was merged from four entries into one, the reader still
        // dispatched on the old ids, and the whole feature silently rendered as "0 项 / 该模块当前没有
        // 可用设置" while the rail happily listed it. Nothing errored anywhere.
        //
        // Two rules replace it: a feature is backed by one *table* rather than by a list of section
        // names, and every table but the window's own must have rows.
        String modulesSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordModules.kt"));
        assert modulesSrc.contains("val table: GodSwordRows.Table")
                : "a module must be backed by a table, not by section names: two features can label a "
                        + "group the same thing, and naming sections pulls the other's rows in";
        assert !modulesSrc.contains("val sections: List<String>")
                : "the section-name list is what allowed 时停 and 斩击 to share 基础 and cross-contaminate";
        assert godScreen.contains("GodSwordRows.rowsOf(module.table)")
                : "the reader must resolve rows from the module's table";
        assert !godScreen.contains("tableFor(")
                : "the section-name lookup must be gone, or the collision survives in any caller left";
        // The window's own settings are not config rows, so its table is legitimately empty; every
        // other table must produce rows or it is a rail entry that opens nothing.
        assert modulesSrc.contains("GodSwordRows.Table.WINDOW, isWindowSettings = true")
                : "the window settings entry is the only one allowed an empty table";

        // ---- the arrangement layer ---------------------------------------------------------------
        // V5 replaces a toggle that sat inside a settings panel with a mode entered from the window's
        // own header. Four things have to hold, and each is a way the previous shape failed:
        //
        //   1. the mode is entered from the window level, not from a page inside it
        //   2. selection is never gated on the mode -- the old flag persisted, and gating navigation on
        //      a persisted flag disabled the window for anyone who had ever enabled it
        //   3. the mode reaches the whole window: a right-click menu, and resize grips
        //   4. every adjustable value it claims is actually read by something
        String arrange = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/ArrangeMenu.kt"));
        String lookSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/AuroraLook.kt"));
        String layoutSrc = Files.readString(Path.of(
                "src/main/java/com/yuan/client/aurora/AuroraLayout.java"));

        // ---- the rail is sections, not a flat list -------------------------------------------------
        // The requested shape: named slots a player assembles, entries movable between them, each slot
        // with its own settings, and one action that unifies their CONTENT. Several properties, and each
        // is a way a heading-plus-list model fails:
        assert grid.contains("internal data class RailSection")
                : "a rail section must be a model, not a label printed above some entries";
        assert grid.contains("internal fun moveEntry(")
                : "an entry must be movable between sections, not only within one list";
        // Move is expressed in flat indices and converted back: a single column has no angle to read, so
        // the drop target is derived from travel, and travel spans section boundaries without knowing
        // where they are.
        //
        // Counted, not merely present. There are exactly two conversion sites -- the one inside the loop,
        // for a drop that lands mid-list, and the one after it, for a drop past the last section -- and a
        // plain `contains` was satisfied by either alone. Replacing the in-loop site with a bare
        // `return sections` makes every cross-section move silently do nothing while the guard still
        // passes, which is the same "existence is not the contract" failure this file has hit before.
        assert countOccurrences(grid, "working.map { it.toRailSection() }") >= 2
                : "both the in-loop and the trailing move must convert back to sections; found "
                        + countOccurrences(grid, "working.map { it.toRailSection() }");
        assert grid.contains("removeAll { it.id == moved.id }")
                : "a move must remove the entry from wherever it was before inserting it";
        assert grid.contains("SectionHeader(")
                : "each section needs its own heading, which is what makes it a slot rather than a label";
        assert grid.contains("section.collapsed")
                : "a section must be collapsible, which a group label cannot be";
        assert arrange.contains("ArrangeTarget.Section")
                : "a section must have its own settings page";
        assert arrange.contains("layout.unifySections()")
                : "the one-key unify action must exist";
        assert layoutSrc.contains("public void unifySections()")
                : "unify must live on the model, so a preset can apply it too";
        assert layoutSrc.contains("public List<Section> sectionsOrDefault(")
                : "unknown ids must be appended rather than dropped when the arrangement is loaded";
        assert layoutSrc.contains("sections.clear();")
                : "resetting the layout must reset the sections too";

        // ---- an invalid escape must be repaired, and the mode must be escapable --------------------
        assert layoutSrc.contains("sections.removeIf(")
                : "a section without an id or a name cannot be addressed; it must be dropped, not shown";
        // The mode could formerly be left only by a header button, and that button was covered by the
        // window's drag handle -- so entering the mode trapped the window. ESC is the general way out.
        assert godScreenSrc.contains("onEscapePressed = {")
                : "the window must install the escape hook, or the mode cannot be left";
        assert screen.contains("protected var onEscapePressed")
                : "the hook must live on the host class, which owns the key path";
        assert screen.contains("GLFW_KEY_ESCAPE")
                : "the host must consult the hook on ESC before closing the window";

        assert godScreen.contains("ArrangeTarget.Rail") && godScreen.contains("ArrangeMenu(")
                : "a right-click must open a per-element menu, resident";
        // The customization menus must not be gated either: they are how the window is reached at all.
        assert !stripComments(godScreenSrc).contains("arrangeMode")
                : "the window must not read an arrangement-mode flag anywhere";
        // The grips now live in the shared gesture frame, and they are RESIDENT: what must hold is that
        // the frame exposes eight of them and that the shell routes the main window through it.
        assert windowSrc.contains("ResizeGripSet(")
                : "the window must be resizable like a window, not only scalable by a global factor";
        // All eight grips must exist AND be reachable. The sizing bug this guards: an edge grip was
        // built as `size(GripThickness, 0.dp)` then `fillMaxHeight(0.4f)` -- filling a 0-height
        // constraint yields 0, so left/right/top/bottom had a 0x0 hit box. Only the four corners
        // responded, which is why every edge drag felt dead while the corners worked.
        assert windowSrc.contains("Grip(Alignment.TopStart, GripKind.Corner")
                && windowSrc.contains("Grip(Alignment.BottomEnd, GripKind.Corner")
                : "all four corner grips must exist";
        assert windowSrc.contains("Grip(Alignment.CenterStart, GripKind.EdgeV")
                && windowSrc.contains("Grip(Alignment.CenterEnd, GripKind.EdgeV")
                && windowSrc.contains("Grip(Alignment.TopCenter, GripKind.EdgeH")
                && windowSrc.contains("Grip(Alignment.BottomCenter, GripKind.EdgeH")
                : "all four edge grips must exist";
        // An edge takes ONE dimension from the band and FILLS the other; a fixed size in both
        // makes one of them zero. And corners must be declared AFTER the edges so they win the
        // overlap (later children are hit first in a Box) -- with short edges, whole thirds of
        // each side were dead zones.
        assert windowSrc.contains("GripKind.EdgeV -> Modifier.width(GripThickness).fillMaxHeight()")
                && windowSrc.contains("GripKind.EdgeH -> Modifier.height(GripThickness).fillMaxWidth()")
                : "an edge grip must fill its side, not carry a fixed zero on that axis";
        // Comment-stripped: the fix's own explanation quotes the bad pattern (`size(GripThickness,
        // 0.dp)`), and a raw scan reads that comment as a live defect -- trap #1 in this file's
        // history, now guarded explicitly rather than remembered.
        assert !stripComments(windowSrc).contains("GripThickness, 0.dp")
                : "a fixed 0.dp on one axis makes the edge hit box empty";
        {
            int edgeAt = stripComments(windowSrc).indexOf("Grip(Alignment.CenterStart, GripKind.EdgeV");
            int cornerAt = stripComments(windowSrc).indexOf("Grip(Alignment.TopStart, GripKind.Corner");
            assert edgeAt >= 0 && cornerAt > edgeAt
                    : "corners must be listed after the edges so they win the overlap";
        }
        // The move band must not cover the header's buttons. Two failures guarded here:
        //   1. no reserve at all -- the exit button could not be clicked;
        //   2. the reserve AFTER pointerInput -- padding only shrinks content, not the hit area,
        //      so the band still covered the buttons, their clicks read as taps, and two quick
        //      clicks fired the double-tap maximize ("突然全屏"). The padding must PRECEDE the
        //      pointerInput in the modifier chain.
        assert windowSrc.contains("padding(start = 48.dp, end = buttonReserve")
                : "the move band must reserve the header's controls on both ends";
        {
            String band = stripComments(windowSrc);
            int padAt = band.indexOf("padding(start = 48.dp");
            int inputAt = band.indexOf(".pointerInput(screenW, screenH)");
            assert padAt >= 0 && inputAt >= 0 && padAt < inputAt
                    : "the band's padding must come BEFORE its pointerInput, or the hit area covers the buttons";
        }

        // ---- maximize / restore, snap, pin: the resident window behaviours ---------------------
        // Each of these was unreachable before (no button, no double-click, no pin), and each is a
        // bookkeeping pair -- save the rectangle on the way in, hand it back on the way out -- so the
        // guards pin the PAIR, not the presence of one half.
        // Double-click maximize lives in the hand-rolled band detector now: a second tap within
        // 400ms toggles. The first implementation chained detectTapGestures + detectDragGestures,
        // and the tap detector consumed the down so the drag detector never started -- the round-3
        // diagnosis. What must hold is the PAIR in one loop: the double-tap window AND the drag
        // arming in the same pointerInput, with no second detector chained on the node.
        assert windowSrc.contains("lastTapTime < 400_000_000L")
                : "double-clicking the title bar must maximize/restore";
        assert countOccurrences(stripComments(windowSrc), "detectTapGestures(") == 0
                : "no detectTapGestures on the gesture nodes: chained detectors consume each other's events";
        // The bookkeeping now lives in ONE shared helper (toggleMaximize/restoreWindow/maximize in
        // the gesture frame) rather than copied per call site: three copies were three chances to
        // drift. What must hold: both surfaces route through the helper, and the helper owns the
        // save/restore pair.
        assert countOccurrences(shell, "toggleMaximize(layout, box, onMoved)") >= 2
                : "the header button and the double-click must share one bookkeeping path";
        assert windowSrc.contains("internal fun toggleMaximize(")
                && windowSrc.contains("internal fun restoreWindow(")
                && windowSrc.contains("internal fun maximize(")
                : "the maximize trio must live in the gesture frame, shared by every path";
        assert countOccurrences(windowSrc, "windowRestore = box.copy()") >= 1
                && countOccurrences(windowSrc, "windowMaximized = true") >= 1
                : "maximizing must save the restore rectangle before filling the stage";
        assert countOccurrences(windowSrc, "windowMaximized = false") >= 2
                : "restoring must hand the saved rectangle back (button and drag paths)";
        // The maximized trap: with the band gated on !maximized, a persisted maximized flag left
        // the window with NO drag gesture at all -- "无法移动" while filling the screen. The band
        // must be unconditional; only the grips may be gated, and a drag on a maximized window
        // must restore it under the pointer first.
        assert windowSrc.contains("restoreUnderPointer(")
                : "dragging a maximized window must restore it under the pointer";
        assert countOccurrences(stripComments(windowSrc), "if (gestures && !maximized)") == 1
                : "only the grips may be gated on maximized; the move band must not be";
        // A window must never be able to leave the stage: off-screen header controls are the other
        // face of the same "cannot be moved" report.
        assert layoutSrc.contains("x = clamp(x, 0f, 1f - w)")
                && layoutSrc.contains("y = clamp(y, 0f, 1f - h)")
                : "the window must stay fully on stage, or its controls become unreachable";
        assert windowSrc.contains("snapSlotAt(") && windowSrc.contains("SnapSlot.Maximize")
                : "dragging to a screen edge must offer the snap slots, including maximize at the top";
        assert windowSrc.contains("layout.aeroSnapEnabled")
                : "edge snapping must honor its own switch, separate from the gesture switch";
        assert layoutSrc.contains("public boolean gesturesEnabled")
                && layoutSrc.contains("public boolean aeroSnapEnabled")
                : "the two gesture switches must live on the layout, persisted with it";
        assert layoutSrc.contains("public void setPinned(") && layoutSrc.contains("renumberByPin()")
                : "pinning must re-tier depths so a pinned panel outranks every unpinned one";
        // The re-tier must be INSIDE setPinned, not merely present somewhere in the file: raise()
        // also calls renumberByPin, so a bare `contains` passes with a pin that does nothing.
        int setPinnedAt = layoutSrc.indexOf("public void setPinned(");
        int setPinnedEnd = layoutSrc.indexOf("}", layoutSrc.indexOf("box.pinned = pinned;", setPinnedAt));
        assert setPinnedAt >= 0 && setPinnedEnd > setPinnedAt
                && layoutSrc.substring(setPinnedAt, setPinnedEnd).contains("renumberByPin()")
                : "setPinned itself must re-tier, or the pin button changes nothing";
        assert shell.contains("setPinned(panelId")
                : "the panel header must expose the pin through the model";
        // ---- a floating panel drags by its HEADER, not by an overlay band ---------------------
        // The panel used the frame's overlay band like the main window did. That band is drawn
        // ABOVE the panel's content, so a press aimed at a control in the panel's top rows landed
        // on the band -- reported as "浮层只能打开和关闭". Owning the drag on the header Row puts
        // the band and the controls in one tier, and the frame's overlay is switched off for
        // panels so there is exactly one drag surface.
        // Internal, not private: the arrangement menu lives in another package and is a window
        // too, so it installs the same header drag band.
        assert shell.contains("internal fun Modifier.panelDragBand(")
                : "the panel must own its drag on its header";
        // UNITS: the band measures pointer travel in PIXELS, so the stage it divides by must be
        // pixels too. Passing the Dp stage made a 1px drag move the panel ~1/density of the stage
        // (about 2.2x too far at GUI scale 2) -- reported as "实际错位".
        // THE DRAG MEASUREMENT, and FOUR shapes failed before this one. Every earlier attempt mixed
        // two LAYOUT GENERATIONS in one subtraction:
        //   1. `c.position - down.position` -- `down` captured before the drag, `c` after: the
        //      node's own movement subtracted once too often (spring-back);
        //   2. `origin + c.position` -- origin from onGloballyPositioned, a layout behind the event:
        //      overshoot then correction ("四处乱跳");
        //   3. `c.position - lastLocalX` -- lastLocalX stored from the previous event, i.e. the
        //      previous layout: same mismatch as (1);
        //   4. this: `c.position - c.previousPosition` -- TWO FIELDS OF ONE CHANGE, recorded against
        //      ONE layout, so the difference is exactly the pointer's step. It is the quantity
        //      Compose's own detectDragGestures accumulates, and the only one that cannot mismatch.
        {
            String code = stripComments(shell);
            int at = code.indexOf("internal fun Modifier.panelDragBand(");
            int end = code.indexOf("internal fun", at + 1);
            assert at >= 0 && end > at;
            String band = code.substring(at, end);
            assert band.contains("val stepX = c.position.x - c.previousPosition.x")
                    && band.contains("box.x = (box.x + stepX / stageWidthPx)")
                    : "drag travel must be position - previousPosition: same event, same layout";
            // Nothing may come from a layout callback: that is a different generation by definition.
            assert !band.contains("originXState")
                    && !band.contains("positionInRoot()")
                    : "the drag must not depend on a layout-callback origin: that lags the pointer";
            // A stored baseline from a previous event is the same mismatch as (1).
            assert !band.contains("lastLocalX")
                    : "a baseline stored from a previous event belongs to a previous layout";
            // A key that changes during the drag restarts the gesture (the "只能拖动一点" bug).
            int headEnd = code.indexOf("awaitPointerEventScope", at);
            String head = code.substring(at, headEnd);
            assert !head.contains("pointerInput(title, stageWidthPx, stageHeightPx, origin")
                    : "no moving value may be a pointerInput key";
        }
        // The main window and the grips use the same same-event baseline.
        assert stripComments(windowSrc).contains("val stepX = c.position.x - c.previousPosition.x")
                : "the window's own drag and resize must use the same-event baseline too";
        assert !stripComments(windowSrc).contains("box.x = grabX + (rootX - downRootX)")
                : "absolute root travel is the shape that oscillated; it must be gone";
        assert shell.contains("stageWidth * density")
                : "the Dp stage must be converted through the density before the band divides by it";
        assert shell.contains("stepX / stageWidthPx")
                : "the band must divide pixel travel by the pixel stage";
        assert shell.contains("moveBand = false")
                : "a panel must disable the frame's overlay band, or it covers the panel's top rows";

        // ---- every panel subtree must SUBSCRIBE, or an edit never repaints --------------------
        // The same class of failure as the gesture frame: the controls read and write plain Java
        // fields on `Look`, so a drag changes the model while the composable's parameters stay
        // equal -- Compose skips the call and the value is never drawn. Reported as "好像实际有效果
        // 但是不显示". The menu and both panel bodies must read the revision.
        assert countOccurrences(stripComments(arrange), "LocalLayoutRevision.current") >= 1
                : "the arrangement menu must subscribe, or a slider's write never repaints";
        String bodyPanelSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordPanel.kt"));
        assert countOccurrences(stripComments(bodyPanelSrc), "LocalLayoutRevision.current") >= 1
                : "a floating panel's body must subscribe, or its rows never repaint";
        assert countOccurrences(stripComments(inline), "LocalLayoutRevision.current") >= 1
                : "the inline content area must subscribe, or its rows never repaint";
        // THE LEAVES, specifically. A subscription on the menu's root is not enough: it re-runs, but
        // `RailNumbers(layout, look, onChanged)` is called with unchanged parameters, so Compose
        // skips that subtree and the composable that actually reads the value never re-runs --
        // knob frozen, readout frozen, model updated. Diagnosed live: the log showed the value
        // walking 167 -> 143 -> 167 and recomposing the menu, while the user saw nothing move.
        assert countOccurrences(stripComments(arrange), "private fun Num(") >= 1
                : "the numeric leaf must exist";
        {
            String code = stripComments(arrange);
            int numAt = code.indexOf("private fun Num(");
            int numEnd = code.indexOf("private fun", numAt + 1);
            assert numAt >= 0 && numEnd > numAt;
            String numBody = code.substring(numAt, numEnd);
            assert numBody.contains("LocalLayoutRevision.current")
                    : "the numeric leaf must subscribe itself: the menu root's read does not reach it";
            int swAt = code.indexOf("private fun Swatch(");
            int swEnd = code.indexOf("private fun", swAt + 1);
            assert swAt >= 0 && swEnd > swAt
                    && code.substring(swAt, swEnd).contains("LocalLayoutRevision.current")
                    : "the swatch leaf must subscribe itself";
        }

        // ---- the arrangement menu is a WINDOW, not a centred box ----------------------------
        // Two reports in one: "window 手势对这个窗口没有用" and "只有几个配置项". The menu was a
        // centred Column with a hard-coded 400x560dp size -- no drag, no resize, no maximize, and
        // pages taller than 560dp simply cut off. It now goes through the shared gesture frame.
        assert stripComments(arrange).contains("AuroraWindowFrame(")
                : "the menu must use the shared window frame, or it cannot be moved or resized";
        assert stripComments(arrange).contains("panelDragBand(")
                : "the menu header must drag the menu, like a panel's header drags it";
        assert !stripComments(arrange).contains("heightIn(max = 560.dp)")
                : "a hard-coded height cap cuts pages off; the box is the player's now";
        assert stripComments(arrange).contains("fillMaxSize()")
                : "the menu body must fill the frame the player sized";

        // ---- no overlay may starve the window's controls -----------------------------------------
        // The regression the dual-channel diagnosis caught (batch 22): the resize-cursor tracker
        // was a full-size CHILD Box with a pointerInput, composited over the window. Compose's hit
        // test resolves to the topmost sibling, so that box won EVERY position and not one press
        // reached any control -- the window could only be opened (a key) and closed (ESC), which
        // is exactly what was reported. Two rules, both structural:
        //   1. observing overlays must ride an ANCESTOR's own modifier, never a covering child;
        //   2. the drawing overlay must carry no pointer modifier at all.
        assert windowSrc.contains("internal fun Modifier.trackCursor(")
                : "cursor tracking must be an ancestor modifier, not a covering child";
        java.util.regex.Matcher arrowBox = java.util.regex.Pattern.compile(
                "AuroraResizeCursor\\(state: ResizeCursorState\\)([\\s\\S]*?)\\n\\}"
        ).matcher(stripComments(windowSrc));
        assert arrowBox.find()
                && !java.util.regex.Pattern.compile("pointerInput").matcher(arrowBox.group(1)).find()
                : "the cursor overlay must be pure drawing: a pointer modifier on it starves the window";
        assert shell.contains(".trackCursor(cursor)")
                : "the stage root must observe the cursor through its own modifier";

        // ---- hover must be Enter/Exit, and the overlay must sit ON the pointer -----------------
        // Two defects reported together in one screenshot:
        //   * the blue frame around the whole window -- every grip's hover was "on". The test was
        //     `position.x >= 0 && position.y >= 0`, and a local position inside a node is ALWAYS
        //     non-negative, so it never turned off.
        //   * the resize arrow floated away from the cursor -- the tracker recorded one
        //     coordinate space and the overlay offset in another.
        assert countOccurrences(stripComments(windowSrc), "PointerEventType.Enter ->") >= 1
                && countOccurrences(stripComments(windowSrc), "PointerEventType.Exit ->") >= 1
                : "grip hover must be driven by Enter/Exit, not by a coordinate test";
        assert !stripComments(windowSrc).contains("position.x >= 0 && it.position.y >= 0")
                : "a local position is always non-negative; that test never turns hover off";
        // Both writers of the cursor position add their node's root origin, so they land in the
        // same space as the overlay.
        assert windowSrc.contains("state.x = rootOriginX + change.position.x")
                : "the cursor tracker must publish root space, matching the grips";
        assert windowSrc.contains("cursor.x = rootX")
                : "a grip drag must keep the arrow at the pointer";
        // ---- no tint is ever painted on the window's edges -------------------------------------
        // Two revisions tinted the edge on hover: a filled plate (a blue frame around the window)
        // and then a 1.5dp line (a blue line around the window). Both were rejected by the user as
        // ugly, and the line cannot follow the shell's rounded corners anyway. The resize arrow is
        // the whole affordance -- the same call every desktop makes.
        java.util.regex.Matcher gripDraw = java.util.regex.Pattern.compile(
                "private fun BoxScope\\.ResizeGripSet\\(([\\s\\S]*?)\\n\\}"
        ).matcher(stripComments(windowSrc));
        assert gripDraw.find()
                : "the grip set must exist";
        {
            String body = gripDraw.group(1);
            assert !body.contains("drawRect(") && !body.contains("Star.accent")
                    : "no hover tint on a grip: an edge line cannot follow the rounded shell";
        }
        // The glyph itself must be a thin stroked arrow, not a plated one. Bounded by the next
        // top-level declaration rather than the first `}` -- the function contains a rotate block,
        // and a `\n}` bound would cut the body off before the calls that matter.
        {
            String code = stripComments(windowSrc);
            // The DEFINITION, not the call site: `indexOf("drawResizeArrow(")` finds the call in
            // the overlay composable first, and the body between that call and the next
            // `private fun` is the grips, not the glyph.
            int start = code.indexOf("DrawScope.drawResizeArrow(");
            int end = code.indexOf("private fun", start + 1);
            assert start >= 0 && end > start
                    : "the resize arrow glyph must exist";
            String body = code.substring(start, end);
            assert !body.contains("drawRoundRect")
                    : "the resize arrow must not draw a plate behind it";
            assert body.contains("drawArrowShaft(") && body.contains("drawArrowBarbs(")
                    : "the arrow must be a waisted shaft with barbs at both ends";
            // DIRECTION, and this was live-reported: the top edge showed a left/right arrow and a
            // side edge showed an up/down one. The base glyph is a "\" diagonal and `rotate` turns
            // clockwise in screen space (y down), so +45deg maps (1,1) to (0,+) -- VERTICAL -- and
            // -45deg maps it to HORIZONTAL. A vertical edge resizes left/right (EW) and must take
            // the horizontal glyph, so EW is -45 and NS is +45. The two were swapped.
            assert body.contains("ResizeCursorDir.EW -> -45f")
                    && body.contains("ResizeCursorDir.NS -> 45f")
                    : "EW must rotate -45 and NS +45, or every edge shows the other axis's arrow";
            assert !body.contains("ResizeCursorDir.EW -> 45f")
                    && !body.contains("ResizeCursorDir.NS -> -45f")
                    : "the swapped rotations are the reported bug: edges showed the wrong axis";
        }

        // ---- the window has ONE box, and the renderer reads it ------------------------------------
        // The bug this guards: the renderer read `layout.window`, while both drag handlers wrote
        // `layout.boxFor("__window", …)` which stores into `cards`. The two never met, so every drag
        // wrote the rectangle where nothing looked for it and the window could not be moved or resized
        // at all -- reported as "completely unusable".
        //
        // Counted on COMMENT-STRIPPED source. The fix's own explanation names the wrong accessor, so a
        // raw count found it in the comment and passed even with the bug reinstated -- the same trap
        // this file has hit repeatedly, now guarded against explicitly rather than remembered.
        String shellCode = stripComments(shell);
        assert !shellCode.contains("boxFor(\"__window\"")
                : "the window must not store its box in cards under a private id";
        // ONE writer now: the renderer seeds the box, and the gesture frame reads that same field
        // (`layout.window`). What has to hold is that every gesture resolves through THAT accessor.
        assert countOccurrences(shellCode, "layout.windowBox(") >= 1
                : "the renderer must seed the window's box from one accessor";
        // The move band tracks the pointer's ROOT-space travel since the grab and applies it to the
        // box captured at press. Two rejected forms came before it:
        //   1. incremental deltas -- double-counted the window's own movement, rubber-band lag;
        //   2. grabOff = downRoot - box.x * screenW -- subtracted a STAGE-space pixel from a
        //      ROOT-space one, correct only when the frame sits at the stage origin. True for the
        //      main window, false for every floating panel, which sits inside the content area
        //      after the rail and the content header: panels landed in the wrong place, reported
        //      as "浮层只能打开和关闭".
        // The stage origin is still published for the cursor overlay, but the DRAG no longer reads
        // any origin: it uses the same-event baseline (position - previousPosition), which is the
        // only measurement whose two terms share a layout generation. Absolute root travel was the
        // shape that oscillated.
        assert countOccurrences(stripComments(windowSrc), "onGloballyPositioned") >= 2
                : "the cursor overlay still needs the stage origin";
        assert countOccurrences(stripComments(windowSrc), "box.x = (box.x + stepX / screenW)") >= 1
                && countOccurrences(stripComments(windowSrc), "box.y = (box.y + stepY / screenH)") >= 1
                : "the move band must apply the pointer's same-event step to the box";
        assert !stripComments(windowSrc).contains("grabOffX")
                : "a stage-space pixel must never be subtracted from a root-space one";
        assert !stripComments(windowSrc).contains("box.x = grabX +")
                : "grab-time absolute travel mixes layouts; it must not come back";
        assert countOccurrences(stripComments(windowSrc), "box.w = (box.w + fx)") >= 1
                : "the grips must accumulate the same-event step into the edge";
        // The sign bug: fx/fy were multiplied by grip.sx/sy AND the branch applied the direction,
        // so left/top edges moved OPPOSITE to the pointer (right/bottom were fine). It survived
        // every rewrite because it came from the batch-21 resizeBox convention. Raw pointer travel
        // only -- the branch owns the direction.
        assert !stripComments(windowSrc).contains("/ screenW * grip.sx")
                && !stripComments(windowSrc).contains("/ screenH * grip.sy")
                : "resize travel must not be sign-multiplied twice: left/top edges invert";
        assert !stripComments(windowSrc).contains("box.x += dx")
                : "incremental dragging against a moving local origin rubber-bands; go absolute";
        // Both side columns must scroll: the rail and the content would otherwise clip entries
        // with no way to reach them once the window is resized or the UI scale raised.
        assert countOccurrences(stripComments(grid), "auroraScroll(rememberScrollState())") >= 1
                : "the rail must scroll when its sections overflow";
        assert stripComments(inline).contains("auroraScroll(")
                : "the content column must scroll";
        String panelSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/GodSwordPanel.kt"));
        assert stripComments(panelSrc).contains("auroraScroll(")
                : "a floating panel's body must scroll";
        assert layoutSrc.contains("public CardBox windowBox(")
                : "the single window accessor must live on the layout, not be re-derived at each site";
        // A plain Java field changes no Compose state, so an edit is invisible unless something
        // observable moves. The subscription must sit BEFORE the null check: the first drag is the one
        // that creates the box, and a read behind the check would make exactly that drag invisible.
        assert shell.contains("var frame by remember { mutableStateOf(0) }")
                : "an edit must bump an observable value, or the window does not redraw while dragged";
        assert countOccurrences(shell, "onMoved()") + countOccurrences(windowSrc, "onMoved()") >= 6
                : "each gesture must request a redraw after writing the box; found shell="
                        + countOccurrences(shell, "onMoved()") + " window="
                        + countOccurrences(windowSrc, "onMoved()");
        // Order, not shape: the subscription must precede the box's own resolution. Comparing against a
        // remembered one-liner broke the moment the box grew a multi-line initialiser; what the contract
        // actually needs is that the read comes first.
        int frameRead = shellCode.indexOf("frame\n");
        if (frameRead < 0) frameRead = shellCode.indexOf("frame\r\n");
        int boxCheck = shellCode.indexOf("layout.windowBox(");
        assert frameRead >= 0 && boxCheck >= 0 && frameRead < boxCheck
                : "the redraw subscription must be read before the box is resolved, not on its far branch";
        // Rule 4 is the one that makes the panel honest: a slider whose value nothing reads is the
        // failure this whole round was reported for.
        assert lookSrc.contains("class AuroraLook") && lookSrc.contains("LocalLayoutRevision.current")
                : "the adjustable values must be read through the live view, or a slider changes nothing";
        // THE GESTURE-FRAME SUBSCRIPTION. Dragging and resizing write plain fields on a Java
        // CardBox; no Compose state changes, every parameter of AuroraWindowFrame stays equal
        // (same box reference, same stage/layout/cursor), and Compose skips the call -- the values
        // move and the picture does not. That is the "拖动没用 / 缩放没用" report, and it survived
        // every geometry fix because the geometry was always correct. The frame must read the
        // revision, and that read must come BEFORE the stage-size early return.
        {
            String winCode = stripComments(windowSrc);
            int subAt = winCode.indexOf("LocalLayoutRevision.current");
            int guardAt = winCode.indexOf("if (screenW <= 0f || screenH <= 0f) return");
            assert subAt >= 0
                    : "the gesture frame must subscribe to the layout revision or gestures never redraw";
            assert guardAt >= 0 && subAt < guardAt
                    : "the subscription must precede the early return, or it never runs on a zero-size frame";
        }

        assert shell.contains("AuroraLook(LocalAuroraLayout.current)")
                : "the shell must read its chrome from the arrangement layer";
        // Asserting that a Look read exists is not enough: a file can read the layer into a local and
        // then keep using the token, which is exactly the "slider moves and nothing happens" state this
        // round was reported for. What has to hold is that the value is CONSUMED.
        assert controls.contains("val rowHeight = rowLook.rowHeight")
                && controls.contains(".height(rowHeight)")
                : "a settings row's pitch must come from the arrangement layer AND be used by it";
        assert shell.contains(".padding(look.shellInset)")
                && shell.contains("look.shellCorner")
                && shell.contains(".width(look.railWidth)")
                : "the shell's inset, radius and rail width must come from the arrangement layer";
        // Every field the menu writes must exist on the model it writes to.
        java.util.Set<String> menuFields = idsMatching(menuSrcOf(arrange), "layout\\.look\\.(\\w+)");
        java.util.Set<String> modelFields = idsMatching(layoutSrc, "public (?:float|int) (\\w+)");
        java.util.List<String> unknown = new java.util.ArrayList<>();
        for (String f : menuFields) {
            if (!modelFields.contains(f)) unknown.add(f);
        }
        assert unknown.isEmpty()
                : "the menu edits fields the layout does not have: " + String.join(", ", unknown);
        assert menuFields.size() >= 25
                : "the arrangement menu must expose the window's numbers, found " + menuFields.size();
        // A menu that can only rewrite what it already had would be a viewer. Every written field must
        // also be assigned back through the writer in the menu's own lambdas.
        assert countOccurrences(arrange, "onChanged()") >= 25
                : "every arrangement edit must request a recomposition";
        // Defaults must survive an old file: a Look is repaired rather than rejected, because the
        // arrangement panel is reached through the chrome these values dress.
        assert layoutSrc.contains("look.clampLook()") && layoutSrc.contains("internal")
                        || layoutSrc.contains("look.clampLook()")
                : "a corrupt or absent Look must be repaired, not rejected";
        assert layoutSrc.contains("public Look look = new Look()")
                : "the adjustable values must have defaults that a first run starts from";
        assert !layoutSrc.contains("rowOrder")
                : "the retired row order must be gone: it was written but never read";

        // ---- every adjustable value must be reachable, read, and consumed ------------------------
        // This is the check that makes the arrangement layer honest, and it exists because the round
        // before it shipped values that could be written and never read -- the report was "some of these
        // don't work", and the cause was exactly that. Three properties, all required:
        //
        //   1. every field the model exposes has an accessor in the Compose bridge
        //   2. every accessor is actually read by a widget (a bridge nobody reads is dead weight)
        //   3. every field is editable from the menu (a value a player cannot reach is a constant)
        java.util.Set<String> lookFields = fieldNamesOf(layoutSrc, "public static final class Look", "Look copy()");
        java.util.List<String> noAccessor = new java.util.ArrayList<>();
        java.util.List<String> unread = new java.util.ArrayList<>();
        java.util.List<String> uneditable = new java.util.ArrayList<>();
        String consumers = lookSrc + shell + grid + controls;
        // THE ONE DOCUMENTED EXCEPTION, and it needs a reason rather than a special case.
        //
        // Every other Look field is a number the arrangement menu edits with a slider. `accentColor`
        // is a PRESET: choosing it is a click on a colour swatch, which lives in the settings panel
        // beside the glass and motion rows -- the places a player looks for "how the window behaves"
        // rather than "how the window is arranged". The guard's job is to catch a field nobody can
        // reach; this field is reachable, by a different control in a different surface, so the guard
        // names it explicitly instead of growing a hole.
        java.util.Set<String> editedOutsideTheMenu = java.util.Set.of("accentColor");
        String settingsSrc = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/aurora/panels/AuroraSettingsPanel.kt"));

        for (String f : lookFields) {
            if (!lookSrc.contains("val " + f + ":")) noAccessor.add(f);
            if (!consumers.contains("look." + f) && !lookSrc.contains("look()." + f)) unread.add(f);
            if (editedOutsideTheMenu.contains(f)) {
                // Reached from the settings panel instead -- and proving it here is what keeps the
                // exception honest: if that row were deleted, this would fail.
                if (!settingsSrc.contains("look." + f) && !settingsSrc.contains("look()." + f)) {
                    uneditable.add(f);
                }
            } else if (!arrange.contains("layout.look." + f)) {
                uneditable.add(f);
            }
        }
        assert lookFields.size() >= 30
                : "the arrangement layer must expose the window's numbers; found " + lookFields.size();
        assert noAccessor.isEmpty()
                : "these fields have no live accessor, so a slider could never move them: "
                        + String.join(", ", noAccessor);
        assert unread.isEmpty()
                : "these fields are never read by a widget, so writing them changes nothing: "
                        + String.join(", ", unread);
        assert uneditable.isEmpty()
                : "these fields are not in the arrangement menu, so no player can reach them: "
                        + String.join(", ", uneditable);

        // ---- a menu must not swallow the controls it contains -------------------------------------
        // Idiomatic, and wrong: a `clickable { }` on the menu panel to stop a click reaching the scrim.
        // A `clickable` consumes the PRESS, so every slider inside lost the press it needs to begin a
        // drag -- the report was "this whole menu cannot be clicked", and the cause was one empty lambda.
        assert !arrange.contains("indication = null\n                ) { }")
                : "the menu panel must not carry a no-op clickable: it eats the press its sliders need";
        // DISMISSAL BY RECTANGLE IN ONE SPACE. Three shapes came before:
        //   1. press position vs a layout-space rectangle -- two spaces, everything read outside;
        //   2. "consume what the children left" -- the premise (children run first) is true, but a
        //      slider does not consume the DOWN at all: detectHorizontalDragGestures waits for the
        //      slop, so the panel's own handler claimed the press for every control inside it.
        //      That is why "完成" (in the header, outside that Column) worked while the sliders,
        //      swatches and toggles inside did not;
        //   3. this one: the panel PUBLISHES its bounds in root space, the scrim tests against
        //      them, and the panel consumes nothing.
        assert arrange.contains("var bounds by remember")
                && arrange.contains("bounds = androidx.compose.ui.geometry.Rect(")
                : "the panel must publish its bounds for the scrim to test against";
        assert arrange.contains("coordinates.positionInRoot()")
                : "bounds and press must be in the same space: root";
        assert arrange.contains("change.position.x >= panel.left")
                : "dismissal must test the press against the published rectangle";
        // The panel must never consume a press on its children's behalf.
        assert !arrange.contains("event.changes.forEach { if (!it.isConsumed) it.consume() }")
                : "the panel must not consume presses: a slider's down is unconsumed at press time, "
                        + "so claiming it starves every control inside the panel";
        assert !arrange.contains("panelBounds.contains(change.position)")
                : "dismissal must not compare against a layout-space rectangle";

        // ---- a readout must not clip the value it states ------------------------------------------
        // The readout was a fixed 46.dp, sized for a guess at "how wide is a number". With a unit it
        // clipped: `168 dp` rendered as `1`, `100%` as `1`. A value display that understates its own
        // value is worse than none, because it is believed.
        assert !controls.contains("Modifier.width(46.dp)")
                : "a fixed readout width clips values carrying a unit; measure the text instead";
        assert controls.contains("rememberTextMeasurer()") && controls.contains("readoutWidth")
                : "the slider readout must be measured from the values it can show";

        // ---- the rail's reorder is a resident long-press drag on the ENTRIES -------------------
        // The closing paren is what distinguishes the call from the import, and it is the only reliable
        // discriminator here: the import line carries the same identifier, so a bare `contains` was
        // satisfied by the import even after the call site was reverted. A multi-line match is equally
        // unreliable in the other direction -- the sources are CRLF, so a `\n` in the pattern never
        // matches. Assert on `(` and nothing else.
        //
        // The column-level long-press move is GONE by decision: moving the window is the title bar's
        // job, and two ways to move the same thing is one too many. What must hold is that the
        // entries' long-press drag commits through the same section-rewrite the click path uses.
        assert !rail.contains("railOffsetX +=")
                : "the column's own move gesture is retired; the window's title bar owns moving";
        assert rail.contains("onDragStart = { dragFrom = index; dragTarget = index }")
                : "rail entries must arm their reorder on the long-press drag start";

        // ---- icons are drawn, never imported ---------------------------------------------------------
        assert icons.contains("enum class AuroraIcon") && icons.contains("DrawScope.drawIcon")
                : "icons must be drawn on the module's own grid, not pulled from a dependency";
        assert icons.contains("1.5f * unit")
                : "the icon set must share one stroke width, or it stops looking like a set";

        // Not a pause screen: a paused world would freeze the one thing the backdrop shows.
        assert screen.contains("isPauseScreen(): Boolean = false")
                : "an Aurora window must not pause the world behind it";
        assert screen.contains("fun requestClose()") && !screen.contains("fun onClose()")
                : "the in-composition close must not shadow Screen.onClose";

        System.out.println("YuanAuroraCheck OK");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            count++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return count;
    }
    /**
     * Source with line and block comments removed.
     *
     * <p>Needed before any negative assertion. An explanatory comment that names the very pattern being
     * forbidden -- which is how these guards document their reasoning -- otherwise satisfies the
     * assertion and the check passes on code that still contains the bug. The domain check hit this
     * first; this is the same fix.
     */
    private static String stripComments(String source) {
        return source
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "");
    }
    /**
     * Every first capture group matching {@code pattern}, as a set.
     *
     * <p>Only for patterns that are unambiguous in the file they run against. A pattern that has to be
     * anchored to a region must use [idsInFunction]: a leading `name[\\s\\S]{0,n}?` bound does not
     * restrict the scan to that function, it only requires that `name` appeared somewhere earlier --
     * so a scan meant for one dispatch block happily reads ids out of every later one.
     */
    private static java.util.Set<String> idsMatching(String text, String pattern) {
        java.util.Set<String> found = new java.util.TreeSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(text);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }

    /**
     * The `"id" ->` labels inside one function of a file.
     *
     * <p>Sliced by hand from the named function's declaration to the next `internal fun`, then scanned
     * in full. That is the difference from a lazily-bounded regex: the slice is exactly one function's
     * body, so ids from neighbouring dispatch blocks cannot be counted as this one's coverage -- which
     * is the failure that makes a coverage check silently useless.
     */
    private static java.util.Set<String> idsInFunction(String text, String name) {
        java.util.Set<String> found = new java.util.TreeSet<>();
        int start = text.indexOf("fun " + name + "(");
        if (start < 0) return found;
        int end = text.indexOf("internal fun ", start + 1);
        if (end < 0) end = text.length();
        String body = text.substring(start, end);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"([a-zA-Z]\\w*)\"\\s*->")
                .matcher(body);
        while (m.find()) {
            found.add(m.group(1));
        }
        return found;
    }
    /** The arrangement menu's own source, read once for the field-coverage assertions. */
    private static String menuSrcOf(String text) {
        return text;
    }

    /** True when the shell routes its header button through the shared maximize bookkeeping. */
    private static boolean shellSrcOfMaximize(String shell) {
        return shell.contains("maximizeToggle(");
    }
    /**
     * The names of a class's public float/int fields, read from a slice of its source.
     *
     * <p>Sliced rather than scanned whole because the layout has several field groups and the assertions
     * above are about one of them; a whole-file scan would report the other groups' fields as missing
     * from a bridge they were never meant to be in.
     */
    private static java.util.Set<String> fieldNamesOf(String source, String from, String to) {
        int start = source.indexOf(from);
        int end = source.indexOf(to, start);
        if (start < 0 || end < 0) return new java.util.TreeSet<>();
        java.util.Set<String> found = new java.util.TreeSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("public (?:float|int) (\\w+) =")
                .matcher(source.substring(start, end));
        while (m.find()) found.add(m.group(1));
        return found;
    }
}
