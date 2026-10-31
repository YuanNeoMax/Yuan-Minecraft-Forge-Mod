package com.yuan.client.aurora

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.Key as ComposeKey
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.mojang.blaze3d.systems.RenderSystem
import com.yuan.client.aurora.anim.Motion
import com.yuan.client.gui.toConfig
import com.yuan.client.render.YuanComposeGlState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL30C
import java.awt.Component as AwtComponent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent as AwtKeyEvent
import java.awt.event.MouseEvent as AwtMouseEvent
import java.awt.event.MouseWheelEvent as AwtMouseWheelEvent

/**
 * The Aurora windows' host: owns Skia, the Compose scene, the Skiko surface wrapper and the AWT
 * event bridge, and composes the backdrop blur under whatever [content] draws.
 *
 * <p>Read this as the second of the two layers that make an Aurora window. [AuroraFx] is the
 * backdrop; this is the frame it is drawn into; the Compose trees in `aurora/scaffold` and
 * `aurora/panels` are the contents. A new window is a subclass or a [content] lambda, never a new
 * copy of this plumbing.
 *
 * <p>The draw order is the whole point and is why this layer exists at all: blur the world that the
 * level renderer already put in the main target, then draw the Compose scene straight into that same
 * target on top of it. The panels are translucent, so the blurred world reads through them as
 * frosted glass and the pipeline costs one extra set of passes rather than a whole offscreen UI
 * framebuffer and a composite step to go with it.
 *
 * <p>Everything Skia touches is wrapped: [YuanComposeGlState] captures the GL state this needs to
 * disturb and puts it back, because a Compose flush changes blending, the bound program, the
 * texture bindings and the pixel-store unpack settings, and vanilla draws after this screen on the
 * same frame.
 */
@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
abstract class AuroraComposeScreen(title: String) : Screen(Component.literal(title)) {

    private var skiaContext: DirectContext? = null
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var composeScene: ComposeScene? = null
    private var boundFbo = -1
    private var lastWidth = -1
    private var lastHeight = -1

    /**
     * Scales the Compose stage, and therefore every layout unit inside it.
     *
     * <p>Separate from the game's GUI scale because the two answer different questions: the game's
     * scale is the player's global accessibility choice, and this one is how large *this* interface
     * wants its own text and controls. Multiplying them lets a player who runs the game at GUI scale
     * 4 still shrink Aurora's cards if they consider them too large.
     *
     * <p>Auto mode keys on the window area and is recomputed every frame, so dragging the window
     * between monitors of different sizes rescales live rather than needing a reopen.
     */
    private fun effectiveScale(pixelWidth: Int, pixelHeight: Int): Float {
        if (!layout.autoScale) return layout.scale
        return layout.autoScaleFor(pixelWidth, pixelHeight)
    }

    /** AWT needs a Component to source events from; Compose only reads its identity. */
    private val awtComponent = object : AwtComponent() {}



    /**
     * The sword this window edits, and its config as loaded at open.
     *
     * <p>Assigned by the subclass that knows which item it was opened for. Held as a supplier rather
     * than passed in, because the base class is constructed before the subclass has a stack to give it
     * -- and a window with no sword is a window with nothing to configure, so the content is simply
     * absent rather than rendered against a default config.
     */
    protected var swordStack: ItemStack = ItemStack.EMPTY

    /**
     * Consulted on ESC before the screen closes; true means the key was consumed.
     *
     * <p>A hook rather than handling in the subclass's `keyPressed`, because the subclass does not own
     * the key path -- this class does. The arrangement mode uses it to swallow the first ESC, leaving
     * the mode rather than the window: a player who entered the mode by a header button and found it
     * covered can still get out, which is what "trap" would otherwise mean.
     *
     * <p>Superseded by [onKeyPressed] for everything except the ESC fallback, which is kept separate
     * because ESC has a second meaning in this screen: with no handler taking it, the screen closes.
     * Folding that into the general hook would make "did anyone consume ESC" and "should the screen
     * close" the same question, and they are not.
     */
    protected var onEscapePressed: (() -> Boolean)? = null

    /**
     * Consulted for EVERY key press, before [onEscapePressed] and before the superclass.
     *
     * <p>The keyboard-navigation path. It is a hook on the SCREEN rather than a Compose `onKeyEvent`
     * because Compose key handling needs a focused node, and this scene has none: the window is a
     * click-to-drag surface that never requests focus, so a `Modifier.onKeyEvent` anywhere in the
     * tree would receive nothing. The key code arrives as the raw GLFW value, which is what the
     * host has; [AuroraKeys] maps it to something the composition understands.
     *
     * <p>[modifiers] is the GLFW modifier bitfield, passed through because a shortcut needs it:
     * Ctrl+Z and a bare `z` are different intentions, and only the host knows which was held.
     *
     * <p>Returning true consumes the key, so a handled arrow does not also scroll or move the player.
     */
    protected var onKeyPressed: ((keyCode: Int, modifiers: Int) -> Boolean)? = null

    /** The config as it was when the window opened, for the unsaved-change comparison. */
    protected val baselineConfig: com.yuan.item.YuanGodSwordConfig =
        com.yuan.item.YuanGodSwordConfig()

    /** The config being edited. Written on save, previewed live against the client's stack copy. */
    protected val editingConfig: com.yuan.item.YuanGodSwordConfig =
        com.yuan.item.YuanGodSwordConfig()

    /** Unique per open, so a re-opened window rebuilds its composition instead of reusing a stale one. */
    private val openId = nextOpenId++

    /**
     * The player's persisted layout, loaded once per open.
     *
     * <p>Loaded here rather than per card: every card reads the same object, and a card that loaded
     * its own copy would drift from the others the moment one of them was dragged.
     */
    val layout: AuroraLayout = AuroraLayout.load()

    /** Strength of the darkening laid over the blur. Subclasses tune this to taste. */
    protected open val backdropDim: Float = 0.52f

    /**
     * Bumped every time [layout] is mutated, so the composition re-reads it.
     *
     * <p>The layout is a plain object held outside Compose, so a settings edit would otherwise change
     * values that composition had already read without telling Compose to read them again -- the
     * toggles would move their own knob in the slider and then appear to do nothing to the window.
     * One counter is enough because every one of these edits is a "re-render the whole window" event
     * and they are all user-driven, so the cost is a handful of recompositions per second at most.
     */
    var layoutRevision by mutableStateOf(0)
        private set

    /** Records that the layout changed and the composition must re-read it. */
    protected fun touchLayout() {
        layoutRevision++
    }

    /** The window's contents. Called once per open, inside the shared scene. */
    @Composable
    protected abstract fun WindowContent()

    /**
     * The holder for the composition. Assigned on first render and reused for the rest of the
     * window's life: Compose scenes are not cheap to build and the content must not be re-set
     * mid-session, or every animating value would restart.
     *
     * <p>Typed as a composable lambda, not a plain one -- a plain `() -> Unit` cannot be invoked
     * from inside `setContent`, and wrapping it later would build a new lambda per composition.
     */
    private var contentHolder: (@Composable () -> Unit)? = null

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val mc = Minecraft.getInstance()
        val width = mc.window.width
        val height = mc.window.height
        if (width <= 0 || height <= 0) return

        // The motion level is read from the layout each frame and published to the animation table.
        // It is a mutable global rather than a composition local because the durations are consumed
        // inside animate*AsState calls and non-composable helpers; re-reading it here means a change
        // from the settings card takes effect on the very next frame, with no rebuild.
        Motion.level = layout.motion

        val fbo = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING)
        ensureResources(width, height, fbo)

        // The backdrop goes down first: it reads the main target and writes it back blurred and
        // dimmed, so it must run before anything else touches that target this frame.
        // Rebuilt every frame: each live panel registers itself again during the composition, so an
        // entry that outlived its panel cannot leave a piece of glass behind with nothing in it.
        GlassRects.clear()
        blurBackdrop(mc)
        drawLiquidGlass(mc)

        val ctx = skiaContext ?: return
        val surf = surface ?: return
        val scene = composeScene ?: return

        val saved = YuanComposeGlState.save()
        try {
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo)
            YuanComposeGlState.resetPixelStore()
            RenderSystem.enableBlend()
            ctx.resetAll()
            surf.canvas.asComposeCanvas().let { scene.render(it, System.nanoTime()) }
            surf.flush()
        } finally {
            RenderSystem.disableBlend()
            YuanComposeGlState.restore(saved)
        }
    }

    /**
     * Blurs and dims the world the level renderer already drew.
     *
     * <p>The blur strength is the player's, not a constant: at zero the pipeline is skipped entirely
     * and only the dim is drawn, which is what a player who wants the world legible behind the
     * interface asks for. A pipeline run at zero strength would still cost five passes and produce
     * the same picture.
     *
     * <p>Falls back to a flat fill when the shader is unavailable -- the first seconds of a launch,
     * or a compile error. Text over the raw world would be unreadable, and the card fills are
     * translucent, so a fill is the honest degradation rather than glass over an unblurred world.
     */
    /**
     * Draws the liquid-glass pass, one quad per panel, over the blurred backdrop.
     *
     * <p>RUNS AFTER [blurBackdrop] because it samples what that pass left behind: the blurred world
     * is already in the main target, so the glass refracts a soft image. Refracting the sharp world
     * would double its edges, and doubled edges read as a rendering fault rather than as glass.
     *
     * <p>RUNS BEFORE the composition, so the panels' contents land ON the glass. In this mode a panel
     * stops painting its own background (see the material branch in AuroraLook), or the flat pane
     * would simply cover the refraction it is supposed to be made of.
     *
     * <p>ONLY PANELS ARE GLASSED. The shell is the window itself; refracting it would put the effect
     * under the rail and the rows too, and there would be nothing for a panel to be a separate
     * surface against.
     */
    private fun drawLiquidGlass(mc: Minecraft) {
        // Null-safe reads: these are Java fields, and Gson leaves an enum it cannot resolve as null
        // rather than failing the load. A hand-edited aurora.json naming a value that no longer
        // exists would otherwise crash on the first frame the window opened.
        val material = layout.look.material ?: AuroraLayout.MaterialMode.FROSTED
        if (material != AuroraLayout.MaterialMode.LIQUID_GLASS) return
        val main = mc.mainRenderTarget ?: return
        val level = layout.look.glass ?: AuroraLayout.GlassLevel.MEDIUM
        // Nothing to show through: an opaque pane refracts nothing, so the pass is skipped rather
        // than run and then multiplied away.
        if (level.panelAlpha >= 0.999f) return

        // Strength scales with the preset, as the blur does: a barely-there pane must not get the
        // deep displacement of a thick slab, or it stops reading as a thinner material.
        val strengthPx = GLASS_REFRACT_PX * level.blur
        val rim = GLASS_RIM * level.blur
        val tintAlpha = 1f - level.panelAlpha

        GlassRects.snapshot().forEach { r ->
            AuroraFx.drawGlass(
                main,
                r[0], r[1], r[2], r[3],
                r[4],
                strengthPx,
                rim,
                tintAlpha
            )
        }
    }

    private fun blurBackdrop(mc: Minecraft) {
        val main = mc.mainRenderTarget ?: return
        // THE GLASS PRESET SCALES THE BLUR, and that coupling is the point of having a preset at all.
        //
        // Transparency and blur only read as glass together: a translucent surface over a sharp world
        // is a legibility problem, and a heavy blur behind an opaque surface is invisible work -- the
        // player pays for a full-screen pass and sees a dark panel. The slider remains the player's
        // strength; the preset is the multiplier that makes it meaningful at that level of opacity.
        val strength = layout.backdropBlur * layout.look.glass.blur
        if (strength <= 0.01f) {
            AuroraFx.fillFallback(main, backdropDim)
            return
        }
        if (!AuroraFx.apply(main, backdropDim * strength)) {
            AuroraFx.fillFallback(main, backdropDim)
        }
    }

    private fun ensureResources(width: Int, height: Int, fbo: Int) {
        if (composeScene == null) {
            composeScene = sharedScene
        }
        if (composeScene == null) {
            composeScene = CanvasLayersComposeScene(density = Density(gameScale), invalidate = {})
            sharedScene = composeScene
        }
        val scene = composeScene ?: return
        if (contentHolder == null) {
            contentHolder = { WindowContent() }
        }
        if (contentSeed != openId) {
            val holder = contentHolder
            if (holder != null) {
                scene.setContent { holder() }
            }
            contentSeed = openId
        }

        // Density is the game's GUI scale times Aurora's own factor -- a second layer on top, not a
        // replacement. Overriding the game's scale would fight the player's own accessibility choice
        // and make every other screen inconsistent with this one.
        val designed = gameScale * effectiveScale(width, height)
        scene.density = Density(designed)
        // size is nullable: the scene has no size until setContent has laid it out once.
        if (scene.size?.width != width || scene.size?.height != height) {
            scene.size = IntSize(width, height)
        }

        if (skiaContext == null) {
            skiaContext = sharedContext
        }
        if (skiaContext == null) {
            skiaContext = DirectContext.makeGL()
            sharedContext = skiaContext
        }
        val context = skiaContext ?: return

        if (renderTarget == null || surface == null ||
            boundFbo != fbo || lastWidth != width || lastHeight != height
        ) {
            closeSurfaceResources()
            val target = BackendRenderTarget.makeGL(width, height, 0, 8, fbo, FramebufferFormat.GR_GL_RGBA8)
            renderTarget = target
            surface = Surface.makeFromBackendRenderTarget(
                context, target, SurfaceOrigin.BOTTOM_LEFT,
                SurfaceColorFormat.BGRA_8888, ColorSpace.sRGB
            )
            boundFbo = fbo
            lastWidth = width
            lastHeight = height
        }
    }

    /**
     * The Skia surface and Compose scene are shared across opens; the surface wrapper on top of the
     * main framebuffer is not, because it names that framebuffer and is invalid once the window is
     * resized or the screen closes.
     */
    private fun closeSurfaceResources() {
        surface?.close()
        renderTarget?.close()
        surface = null
        renderTarget = null
        boundFbo = -1
    }

    override fun removed() {
        // Persist on close, not on every edit: a drag writes dozens of times a second, and a file
        // write per frame would turn a drag into a stutter. The layout is only authoritative once
        // the player has stopped moving things, which is exactly when the window closes.
        layout.save()
        closeSurfaceResources()
        AuroraFx.close()
        super.removed()
    }

    /**
     * Not a pause screen: the world keeps running behind the glass. A paused world would freeze the
     * one thing the backdrop exists to show.
     */
    override fun isPauseScreen(): Boolean = false

    /**
     * Closes the window from inside the composition, for footer buttons and cancel paths.
     *
     * <p>Named `requestClose` rather than `onClose`: `Screen` already declares an `onClose()` and a
     * same-named member here would silently be an override of that, with vanilla's removal hookup
     * deciding when it runs instead of the button.
     */
    protected fun requestClose() {
        Minecraft.getInstance().setScreen(null)
    }

    /**
     * Loads the sword's config into [editingConfig] and copies it to the baseline.
     *
     * <p>Called by a subclass once it has a stack. Kept in the base class because the read, the
     * baseline and the live preview are one mechanism: a subclass that loaded the config but forgot the
     * baseline would make the window report unsaved changes from the moment it opened.
     */
    protected fun loadConfig(stack: ItemStack) {
        swordStack = stack
        editingConfig.read(stack)
        baselineConfig.copyFrom(editingConfig)
    }

    /**
     * Writes an edited config into the client's copy of the stack, without telling the server.
     *
     * <p>The world renderers read that same stack -- the cosmic model, the slash renderer, the domain's
     * pass -- so an edit shows immediately while the server keeps the last saved state until the player
     * saves. `write` only puts its own keys, so a preset list sharing this tag element is untouched.
     */
    protected fun previewConfig(config: com.yuan.item.YuanGodSwordConfig) {
        if (swordStack.isEmpty) return
        config.write(swordStack)
        editingConfig.copyFrom(config)
    }

    /**
     * Persists the edited config: writes it to the stack, refreshes the baseline, and syncs to the
     * server.
     *
     * <p>The sync sends the whole config child tag rather than the freshly built config. The server
     * owns the ItemStack in survival, so everything stored under that tag has to travel together --
     * sending only the config used to drop the preset list, and presets then never survived a relog.
     */
    protected fun saveConfig() {
        if (swordStack.isEmpty) return
        editingConfig.write(swordStack)
        baselineConfig.copyFrom(editingConfig)
        swordStack.getTagElement(com.yuan.item.YuanGodSwordConfig.TAG)?.let {
            com.yuan.Yuan.CHANNEL.sendToServer(
                com.yuan.network.YuanGodSwordConfigPacket(it.copy())
            )
        }
        // A pending time-stop cooldown is cleared so the edited settings take effect at once rather
        // than after whatever the player triggered before opening the window.
        com.yuan.timestop.YuanTimeStop.startCooldown(0)
        com.yuan.timestop.YuanTimeStopServerState.startCooldown(0)
    }

    /** True when the edited config differs from the one the window opened with. */
    protected fun hasUnsavedChanges(): Boolean = !editingConfig.sameAs(baselineConfig)

    /**
     * Discards live previews, putting the client's stack copy back to the last saved state.
     *
     * <p>Unconditional rather than gated on a dirty flag: the baseline is refreshed on every save, so
     * restoring after a save rewrites the same values, while gating on "dirty" would let a cancel --
     * which clears that flag -- leave previewed values sitting in the client copy.
     */
    protected fun discardPreview() {
        if (swordStack.isEmpty) return
        baselineConfig.write(swordStack)
        editingConfig.copyFrom(baselineConfig)
    }

    /**
     * Saves one preset slot: a snapshot of the config into the sword's preset list.
     *
     * <p>The snapshot is written through a scratch stack and then copied into the list, which is how
     * the older screen does it and is the reason the shape is worth keeping: `write` puts the config
     * under the same tag the presets live in, so writing straight into the list would nest a config
     * inside itself.
     */
    internal fun savePreset(slot: Int, state: com.yuan.client.gui.V9State) {
        if (swordStack.isEmpty) return
        val root = swordStack.getOrCreateTagElement(com.yuan.item.YuanGodSwordConfig.TAG)
        val presets = root.getList(
            com.yuan.item.YuanGodSwordConfig.PRESETS_KEY,
            net.minecraft.nbt.Tag.TAG_COMPOUND.toInt()
        )
        while (presets.size <= slot) {
            presets.add(net.minecraft.nbt.CompoundTag())
        }
        val scratch = ItemStack(net.minecraft.world.item.Items.AIR)
        state.toConfig().write(scratch)
        scratch.getTagElement(com.yuan.item.YuanGodSwordConfig.TAG)?.let { presets.set(slot, it) }
        root.put(com.yuan.item.YuanGodSwordConfig.PRESETS_KEY, presets)
        // The whole tag travels, presets included: sending only the config used to drop the list, and
        // presets then never survived a relog.
        swordStack.getTagElement(com.yuan.item.YuanGodSwordConfig.TAG)?.let {
            com.yuan.Yuan.CHANNEL.sendToServer(
                com.yuan.network.YuanGodSwordConfigPacket(it.copy())
            )
        }
    }

    /**
     * Loads one preset slot into the holder, so the panel redraws with the loaded values.
     *
     * <p>The stored preset is read through a scratch stack rather than assigned field by field: the
     * preset is a config-shaped tag, and the read already knows how to interpret it. Mirroring that
     * interpretation here would be a second reader of the same format.
     */
    internal fun loadPreset(slot: Int, holder: com.yuan.client.gui.V9Holder): Boolean {
        if (swordStack.isEmpty) return false
        val root = swordStack.getTagElement(com.yuan.item.YuanGodSwordConfig.TAG) ?: return false
        val presets = root.getList(
            com.yuan.item.YuanGodSwordConfig.PRESETS_KEY,
            net.minecraft.nbt.Tag.TAG_COMPOUND.toInt()
        )
        if (slot < 0 || slot >= presets.size) return false
        val stored = presets.getCompound(slot)
        if (stored.isEmpty) return false
        val scratch = ItemStack(net.minecraft.world.item.Items.AIR)
        scratch.getOrCreateTag().put(com.yuan.item.YuanGodSwordConfig.TAG, stored.copy())
        val loaded = com.yuan.item.YuanGodSwordConfig()
        loaded.read(scratch)
        holder.restore(com.yuan.client.gui.v9StateOf(loaded))
        return true
    }

    // ---- input bridging -------------------------------------------------------------------
    // Compose expects AWT events, and GLFW hands vanilla a mouse coordinate in GUI-scale space.
    // Everything below converts that to the Compose surface's own pixel space and to AWT's
    // vocabulary.

    /** The game's GUI scale. */
    private val gameScale: Float
        get() = Minecraft.getInstance().window.guiScale.toFloat()

    /**
     * The density the Compose scene is laid out at: the game's GUI scale times Aurora's own factor.
     *
     * <p>Used for the scene's density and for the scroll step, and deliberately **not** for pointer
     * positions -- see {@link #toComposeOffset}.
     */
    private val composeDensity: Float
        get() {
            val mc = Minecraft.getInstance()
            return gameScale * effectiveScale(mc.window.width, mc.window.height)
        }

    /**
     * Converts a coordinate vanilla handed to a Screen into the Compose surface's pixel space.
     *
     * <p>Scaled by the game's GUI scale and nothing else, and the arithmetic says why the Aurora
     * factor must not appear here:
     *
     * <p>Vanilla's `MouseHandler` (verified in bytecode, `m_91561_` offsets 62-123) computes
     * `rawCursor * Window.getGuiScaledWidth() / Window.getScreenWidth()`, so a Screen receives
     * `mouseX` in GUI-scale space, where the physical cursor sits at `mouseX * gameScale`. The
     * scene's surface is allocated at the **physical** size, so a pointer position passed to
     * `sendPointerEvent` is in surface pixels. Compose converts it to `dp` by dividing by the scene
     * density, and a control authored at `D` dp occupies `D * density` physical pixels. Setting the
     * two equal:
     *
     * <pre>passed / density == mouseX * gameScale / density   =>   passed == mouseX * gameScale</pre>
     *
     * <p>The density cancels, so the pointer conversion is independent of it -- which is why this
     * getter does not consult {@link #composeDensity}. An earlier version multiplied by the full
     * density, applying Aurora's scale a second time on top of the one Compose already applies: the
     * pointer landed at `mouseX * auroraScale`, so at an auto scale of 1.10 a click a thousand pixels
     * out missed by a hundred, and at a manual 2.0 it missed by a full screen. It only became visible
     * once the interface gained a second scale factor, since the older screen's density was the game
     * scale itself and the two agreed by construction.
     */
    private fun toComposeOffset(x: Double, y: Double): Offset {
        val s = gameScale
        return Offset((x * s).toFloat(), (y * s).toFloat())
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        composeScene?.sendPointerEvent(
            PointerEventType.Move,
            position = toComposeOffset(mouseX, mouseY),
            type = PointerType.Mouse,
            button = PointerButton(0),
            nativeEvent = awtMouse(mouseX, mouseY, 0, AwtMouseEvent.MOUSE_MOVED)
        )
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        composeScene?.sendPointerEvent(
            PointerEventType.Press,
            position = toComposeOffset(mouseX, mouseY),
            type = PointerType.Mouse,
            button = PointerButton(button),
            nativeEvent = awtMouse(mouseX, mouseY, toAwtButton(button), AwtMouseEvent.MOUSE_PRESSED)
        )
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        composeScene?.sendPointerEvent(
            eventType = PointerEventType.Release,
            position = toComposeOffset(mouseX, mouseY),
            type = PointerType.Mouse,
            button = PointerButton(button),
            nativeEvent = awtMouse(mouseX, mouseY, toAwtButton(button), AwtMouseEvent.MOUSE_RELEASED)
        )
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        composeScene?.sendPointerEvent(
            PointerEventType.Move,
            position = toComposeOffset(mouseX, mouseY),
            type = PointerType.Mouse,
            button = PointerButton(button),
            nativeEvent = awtMouse(mouseX, mouseY, toAwtButton(button), AwtMouseEvent.MOUSE_DRAGGED)
        )
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        val event = AwtMouseWheelEvent(
            awtComponent, AwtMouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), awtMods(),
            (mouseX * gameScale).toInt(), (mouseY * gameScale).toInt(), 0, false,
            AwtMouseWheelEvent.WHEEL_UNIT_SCROLL, 1, (-delta).toInt()
        )
        composeScene?.sendPointerEvent(
            position = toComposeOffset(mouseX, mouseY),
            eventType = PointerEventType.Scroll,
            // One notch reveals ~56 dp of content, scaled by the density so the reveal is
            // consistent at any UI scale. The previous step was delta * density pixels -- about
            // TWO pixels per notch at guiScale 2 -- so every list looked unscrollable.
            scrollDelta = Offset(0f, (-delta * 56f * composeDensity).toFloat()),
            nativeEvent = event
        )
        return super.mouseScrolled(mouseX, mouseY, delta)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // The general key path first, so navigation keys reach the window before anything else.
        // A consumed key stops here: an arrow that moved the focus must not also move the player.
        val keyHandler = onKeyPressed
        if (keyHandler != null && keyHandler.invoke(keyCode, modifiers)) {
            return true
        }
        // ESC leaves the arrangement mode before it does anything else, and it does not reach the rest
        // of the screen in that case.
        //
        // This exists because the mode could otherwise be left only by a header button, and that button
        // was covered by the window's own drag handle -- so a player who entered the mode had no way out.
        // A mode that can trap the window is not a mode; the key is the general escape hatch, and it is
        // checked here rather than in the composition because a Compose key handler needs focus, which a
        // click-to-drag surface never takes.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            val handler = onEscapePressed
            if (handler != null && handler.invoke()) {
                return true
            }
        }
        composeScene?.sendKeyEvent(
            composeKeyEvent(AwtKeyEvent.KEY_PRESSED, glfwToAwtKeyCode(keyCode), AwtKeyEvent.CHAR_UNDEFINED)
        )
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        composeScene?.sendKeyEvent(
            composeKeyEvent(AwtKeyEvent.KEY_RELEASED, glfwToAwtKeyCode(keyCode), 0.toChar())
        )
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        composeScene?.sendKeyEvent(
            composeKeyEvent(AwtKeyEvent.KEY_TYPED, AwtKeyEvent.VK_UNDEFINED, codePoint)
        )
        return super.charTyped(codePoint, modifiers)
    }

    /**
     * An AWT mouse event carrying the cursor in **physical** pixels.
     *
     * <p>AWT is a desktop toolkit: its coordinates are device pixels, not the GUI-scaled space a
     * Minecraft Screen is handed. Converting vanilla's coordinate with the game scale alone is
     * therefore correct here, and adding Aurora's UI factor would push every synthesized event further
     * from the cursor than the pointer it accompanies.
     */
    private fun awtMouse(x: Double, y: Double, button: Int, eventType: Int): AwtMouseEvent {
        val s = gameScale
        return AwtMouseEvent(
            awtComponent, eventType, System.currentTimeMillis(), awtMods(),
            (x * s).toInt(), (y * s).toInt(), 1, false, button
        )
    }

    private fun composeKeyEvent(id: Int, keyCode: Int, keyChar: Char): ComposeKeyEvent {
        val location = if (id == AwtKeyEvent.KEY_TYPED) {
            AwtKeyEvent.KEY_LOCATION_UNKNOWN
        } else {
            AwtKeyEvent.KEY_LOCATION_STANDARD
        }
        val mods = awtMods()
        val native = AwtKeyEvent(
            awtComponent, id, System.currentTimeMillis(), mods, keyCode, keyChar, location
        )
        return ComposeKeyEvent(
            key = ComposeKey(keyCode, location),
            type = when (id) {
                AwtKeyEvent.KEY_PRESSED -> KeyEventType.KeyDown
                AwtKeyEvent.KEY_RELEASED -> KeyEventType.KeyUp
                else -> KeyEventType.Unknown
            },
            codePoint = keyChar.code,
            nativeEvent = native,
            isCtrlPressed = mods and InputEvent.CTRL_DOWN_MASK != 0,
            isAltPressed = mods and InputEvent.ALT_DOWN_MASK != 0,
            isShiftPressed = mods and InputEvent.SHIFT_DOWN_MASK != 0
        )
    }

    private fun toAwtButton(button: Int): Int = when (button) {
        1 -> AwtMouseEvent.BUTTON2
        2 -> AwtMouseEvent.BUTTON3
        else -> AwtMouseEvent.BUTTON1
    }

    /** Modifier state is read from GLFW rather than copied from the event, which carries none. */
    private fun awtMods(): Int {
        val handle = Minecraft.getInstance().window.window
        var mods = 0
        if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS) {
            mods = mods or InputEvent.BUTTON1_DOWN_MASK
        }
        if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS) {
            mods = mods or InputEvent.BUTTON2_DOWN_MASK
        }
        if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_3) == GLFW.GLFW_PRESS) {
            mods = mods or InputEvent.BUTTON3_DOWN_MASK
        }
        if (GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS
        ) {
            mods = mods or InputEvent.CTRL_DOWN_MASK
        }
        if (GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
        ) {
            mods = mods or InputEvent.SHIFT_DOWN_MASK
        }
        if (GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS
        ) {
            mods = mods or InputEvent.ALT_DOWN_MASK
        }
        return mods
    }

    /** GLFW key codes, which are USB HID, to AWT's VK_* constants. */
    private fun glfwToAwtKeyCode(code: Int): Int = when (code) {
        GLFW.GLFW_KEY_SPACE -> AwtKeyEvent.VK_SPACE
        GLFW.GLFW_KEY_ENTER -> AwtKeyEvent.VK_ENTER
        GLFW.GLFW_KEY_BACKSPACE -> AwtKeyEvent.VK_BACK_SPACE
        GLFW.GLFW_KEY_ESCAPE -> AwtKeyEvent.VK_ESCAPE
        GLFW.GLFW_KEY_TAB -> AwtKeyEvent.VK_TAB
        GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> AwtKeyEvent.VK_SHIFT
        GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> AwtKeyEvent.VK_CONTROL
        GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> AwtKeyEvent.VK_ALT
        GLFW.GLFW_KEY_LEFT -> AwtKeyEvent.VK_LEFT
        GLFW.GLFW_KEY_RIGHT -> AwtKeyEvent.VK_RIGHT
        GLFW.GLFW_KEY_UP -> AwtKeyEvent.VK_UP
        GLFW.GLFW_KEY_DOWN -> AwtKeyEvent.VK_DOWN
        GLFW.GLFW_KEY_HOME -> AwtKeyEvent.VK_HOME
        GLFW.GLFW_KEY_END -> AwtKeyEvent.VK_END
        GLFW.GLFW_KEY_PAGE_UP -> AwtKeyEvent.VK_PAGE_UP
        GLFW.GLFW_KEY_PAGE_DOWN -> AwtKeyEvent.VK_PAGE_DOWN
        GLFW.GLFW_KEY_DELETE -> AwtKeyEvent.VK_DELETE
        in GLFW.GLFW_KEY_A..GLFW.GLFW_KEY_Z -> code - GLFW.GLFW_KEY_A + AwtKeyEvent.VK_A
        in GLFW.GLFW_KEY_0..GLFW.GLFW_KEY_9 -> code - GLFW.GLFW_KEY_0 + AwtKeyEvent.VK_0
        in GLFW.GLFW_KEY_F1..GLFW.GLFW_KEY_F12 -> code - GLFW.GLFW_KEY_F1 + AwtKeyEvent.VK_F1
        else -> AwtKeyEvent.VK_UNDEFINED
    }

    companion object {
        private var sharedContext: DirectContext? = null
        private var sharedScene: ComposeScene? = null
        private var nextOpenId = 0
        private var contentSeed = -1

        /**
         * Builds the Skia context and an empty scene ahead of the first open.
         *
         * <p>DirectContext.makeGL and the first Compose composition together cost about a second on
         * a cold JVM, and that second lands on whichever frame the player first pressed the key.
         * Warming it once when the world loads moves the cost off that frame.
         */
        @JvmStatic
        fun warmup() {
            try {
                if (sharedContext == null) sharedContext = DirectContext.makeGL()
                if (sharedScene == null) {
                    val scene = CanvasLayersComposeScene(density = Density(1f), invalidate = {})
                    scene.setContent { }
                    scene.size = IntSize(64, 64)
                    sharedScene = scene
                }
                renderScratch()
            } catch (t: Throwable) {
                // A warmup failure is not fatal: the first real open retries the same path.
            }
        }

        /** Forces one composition + flush into a throwaway target, so the first real frame is warm. */
        private fun renderScratch() {
            val ctx = sharedContext ?: return
            val scene = sharedScene ?: return
            val fbo = GL30C.glGenFramebuffers()
            val tex = GL11C.glGenTextures()
            try {
                GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo)
                GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, tex)
                GL11C.glTexImage2D(
                    GL11C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA8,
                    64, 64, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, null as java.nio.ByteBuffer?
                )
                GL30C.glFramebufferTexture2D(
                    GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                    GL11C.GL_TEXTURE_2D, tex, 0
                )
                val rt = BackendRenderTarget.makeGL(64, 64, 0, 8, fbo, FramebufferFormat.GR_GL_RGBA8)
                val surf = Surface.makeFromBackendRenderTarget(
                    ctx, rt, SurfaceOrigin.BOTTOM_LEFT,
                    SurfaceColorFormat.BGRA_8888, ColorSpace.sRGB
                ) ?: return
                try {
                    val saved = YuanComposeGlState.save()
                    try {
                        YuanComposeGlState.resetPixelStore()
                        ctx.resetAll()
                        surf.canvas.asComposeCanvas().let { scene.render(it, System.nanoTime()) }
                        surf.flush()
                    } finally {
                        YuanComposeGlState.restore(saved)
                    }
                } finally {
                    surf.close()
                    rt.close()
                }
            } finally {
                GL30C.glDeleteFramebuffers(fbo)
                GL11C.glDeleteTextures(tex)
            }
        }
    }
}

/** How far the image is displaced at a panel's rim, in pixels, at full glass strength.
 *
 * <p>Named rather than inlined because it is the whole difference between glass and a wobbling
 * picture: at this displacement the rim visibly magnifies the backdrop while the middle stays
 * optically flat, which is what a thick pane does. */
private const val GLASS_REFRACT_PX = 14f

/** Rim highlight strength at full glass strength. Deliberately low: a bright line reads as a border. */
private const val GLASS_RIM = 0.13f
