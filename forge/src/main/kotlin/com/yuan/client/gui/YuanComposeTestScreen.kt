package com.yuan.client.gui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mojang.blaze3d.systems.RenderSystem
import com.yuan.client.render.YuanComposeGlState
import com.yuan.Yuan
import com.yuan.item.YuanGodSwordConfig
import com.yuan.network.YuanGodSwordConfigPacket
import com.yuan.timestop.YuanTimeStop
import com.yuan.timestop.YuanTimeStopServerState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
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
import java.nio.ByteBuffer
import java.time.LocalDate

/**
 * Compose Multiplatform (Skiko) minimal canvas rendered straight into the MC
 * framebuffer. 神剑专属配置界面（方案 B 直渲 MC FBO）。
 */
@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
class YuanComposeTestScreen(private val stack: ItemStack) : Screen(Component.literal("星渊 · Compose 画布")) {

    private var skiaContext: DirectContext? = null
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var composeScene: ComposeScene? = null
    private var boundFbo = -1
    private var lastWidth = -1
    private var lastHeight = -1

    private val awtComponent = object : AwtComponent() {}

    private val config = YuanGodSwordConfig().also { it.read(stack) }
    private val baseline = YuanGodSwordConfig().also { it.copyFrom(config) }
    private val openId = Companion.nextOpenId++
    private var previewBounds: Rect? = null

    /** Mirrored from the composition so the vanilla ESC path can consult it. */
    private val unsavedChanges = mutableStateOf(false)
    private val confirmingClose = mutableStateOf(false)

    private val scale: Float
        get() = Minecraft.getInstance().window.guiScale.toFloat()

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val mc = Minecraft.getInstance()
        val width = mc.window.width
        val height = mc.window.height
        if (width <= 0 || height <= 0) return

        val fbo = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING)
        ensureResources(width, height, fbo)

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
        drawRealPreview(g)
    }

    private fun drawRealPreview(g: GuiGraphics) {
        val bounds = previewBounds ?: return
        if (stack.isEmpty) return
        val s = scale
        val cx = ((bounds.left + bounds.right) / 2f) / s
        val cy = ((bounds.top + bounds.bottom) / 2f) / s
        val itemScale = (minOf(bounds.width, bounds.height) / s / 24f).coerceIn(3f, 6f)
        g.pose().pushPose()
        g.pose().translate(cx - 8f * itemScale, cy - 8f * itemScale, 0f)
        g.pose().scale(itemScale, itemScale, 1f)
        g.renderItem(stack, 0, 0)
        g.pose().popPose()
    }

    private fun setPreviewBounds(rect: Rect) {
        previewBounds = rect
    }

    private fun ensureResources(width: Int, height: Int, fbo: Int) {
        if (composeScene == null) {
            composeScene = Companion.sharedScene
        }
        if (composeScene == null) {
            composeScene = createScene()
            Companion.sharedScene = composeScene
        }
        val scene = composeScene
        if (scene != null && Companion.contentSeed != openId) {
            scene.setContent {
                ComposeTestContent(
                    initial = v9StateOf(config),
                    defaults = v9StateOf(YuanGodSwordConfig()),
                    onSave = ::save,
                    onPreview = ::applyPreview,
                    unsaved = unsavedChanges,
                    confirmingClose = confirmingClose,
                    onStageBounds = ::setPreviewBounds,
                    onPresetSave = ::savePreset,
                    onPresetLoad = ::loadPreset
                )
            }
            Companion.contentSeed = openId
        }
        composeScene?.density = Density(scale)
        if (composeScene?.size?.width != width || composeScene?.size?.height != height) {
            composeScene?.size = IntSize(width, height)
        }
        if (skiaContext == null) {
            skiaContext = Companion.sharedContext
        }
        if (skiaContext == null) {
            skiaContext = DirectContext.makeGL()
            Companion.sharedContext = skiaContext
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
     * The surface and render target wrap this screen's framebuffer of the moment, while the Skia
     * context and Compose scene are shared on purpose. Without this the full-window Surface leaked
     * on every G press.
     */
    override fun removed() {
        restorePreview()
        unsavedChanges.value = false
        closeSurfaceResources()
        super.removed()
    }

    /** ESC must not silently discard edits: while something is un-saved it opens a confirm instead. */
    override fun shouldCloseOnEsc(): Boolean = !unsavedChanges.value || confirmingClose.value

    private fun createScene(): ComposeScene =
        CanvasLayersComposeScene(
            density = Density(scale),
            invalidate = {}
        )

    private fun save(state: V9State) {
        val updated = state.toConfig()
        updated.write(stack)
        config.copyFrom(updated)
        baseline.copyFrom(updated)
        syncStackConfig()
        YuanTimeStop.startCooldown(0)
        YuanTimeStopServerState.startCooldown(0)
    }

    /**
     * Live preview writes the config into the client's copy of the stack only. The world renderers read
     * that same stack (YuanCosmicBakedModel, YuanSpaceSlashRender), so edits show up immediately while
     * the server keeps the last saved state until 保存. write() only puts its own keys, so the preset
     * list sharing this tag element is never touched.
     */
    private fun applyPreview(state: V9State) {
        val updated = state.toConfig()
        updated.write(stack)
        config.copyFrom(updated)
    }

    /**
     * Undo every un-saved preview. Unconditional on purpose: baseline is refreshed on each save, so
     * restoring after a save writes the same values, while gating on the dirty flag would let a
     * "cancel" (which clears it) leave previewed values sitting in the client copy.
     */
    private fun restorePreview() {
        baseline.write(stack)
        config.copyFrom(baseline)
    }

    /**
     * The server owns the ItemStack in survival, so everything stored in the config child tag has
     * to travel together: sending only toConfig().toTag() used to drop the preset list, and presets
     * therefore never survived a relog.
     */
    private fun syncStackConfig() {
        stack.getTagElement(YuanGodSwordConfig.TAG)?.let {
            Yuan.CHANNEL.sendToServer(YuanGodSwordConfigPacket(it.copy()))
        }
    }

    private fun savePreset(slot: Int, state: V9State) {
        val root = stack.getOrCreateTagElement(YuanGodSwordConfig.TAG)
        val presets = root.getList(YuanGodSwordConfig.PRESETS_KEY, Tag.TAG_COMPOUND.toInt())
        while (presets.size <= slot) {
            presets.add(CompoundTag())
        }
        val temp = ItemStack(Items.AIR)
        state.toConfig().write(temp)
        temp.getTagElement(YuanGodSwordConfig.TAG)?.let { presets.set(slot, it) }
        root.put(YuanGodSwordConfig.PRESETS_KEY, presets)
        syncStackConfig()
    }

    private fun loadPreset(slot: Int): V9State? {
        val root = stack.getTagElement(YuanGodSwordConfig.TAG) ?: return null
        val presets = root.getList(YuanGodSwordConfig.PRESETS_KEY, Tag.TAG_COMPOUND.toInt())
        if (slot < 0 || slot >= presets.size) return null
        val tag = presets.getCompound(slot)
        if (tag.isEmpty) return null
        val temp = ItemStack(Items.AIR)
        temp.addTagElement(YuanGodSwordConfig.TAG, tag.copy())
        return v9StateOf(YuanGodSwordConfig().also { it.read(temp) })
    }

    private fun closeSurfaceResources() {
        surface?.close()
        renderTarget?.close()
        surface = null
        renderTarget = null
        boundFbo = -1
    }

    companion object {
        private var sharedContext: DirectContext? = null
        private var sharedScene: ComposeScene? = null
        private var warmed = false
        private var warmupFailed = false

        private var nextOpenId = 0
        private var contentSeed = -1

        @JvmStatic
        fun warmupIfNeeded() {
            if (warmed || warmupFailed) return
            try {
                if (sharedContext == null) sharedContext = DirectContext.makeGL()
                if (sharedScene == null) {
                    sharedScene = CanvasLayersComposeScene(
                        density = Density(1f),
                        invalidate = {}
                    ).apply {
                        setContent { ComposeTestContent(v9StateOf(YuanGodSwordConfig()), v9StateOf(YuanGodSwordConfig()), {}, {}, { _, _ -> }, { null }) }
                        size = IntSize(64, 64)
                    }
                }
                renderScratch()
                warmed = true
            } catch (t: Throwable) {
                warmupFailed = true
            }
        }

        private fun renderScratch() {
            val ctx = sharedContext ?: return
            val scene = sharedScene ?: return
            val w = 64
            val h = 64
            val fbo = GL30C.glGenFramebuffers()
            val tex = GL11C.glGenTextures()
            try {
                GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fbo)
                GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, tex)
                val pixels: ByteBuffer? = null
                GL11C.glTexImage2D(
                    GL11C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA8,
                    w, h, 0, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, pixels
                )
                GL30C.glFramebufferTexture2D(
                    GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                    GL11C.GL_TEXTURE_2D, tex, 0
                )
                val rt = BackendRenderTarget.makeGL(w, h, 0, 8, fbo, FramebufferFormat.GR_GL_RGBA8)
                val surf = Surface.makeFromBackendRenderTarget(
                    ctx, rt, SurfaceOrigin.BOTTOM_LEFT,
                    SurfaceColorFormat.BGRA_8888, ColorSpace.sRGB
                )!!
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

    private fun toComposeOffset(x: Double, y: Double): Offset {
        val s = scale
        return Offset((x * s).toFloat(), (y * s).toFloat())
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val event = awtMouse(mouseX, mouseY, 0, AwtMouseEvent.MOUSE_MOVED)
        composeScene?.sendPointerEvent(
            PointerEventType.Move,
            position = toComposeOffset(mouseX, mouseY),
            type = PointerType.Mouse,
            button = PointerButton(0),
            nativeEvent = event
        )
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val event = awtMouse(mouseX, mouseY, toAwtButton(button), AwtMouseEvent.MOUSE_PRESSED)
        composeScene?.sendPointerEvent(
            PointerEventType.Press,
            toComposeOffset(mouseX, mouseY),
            type = PointerType.Mouse,
            button = PointerButton(button),
            nativeEvent = event
        )
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val event = awtMouse(mouseX, mouseY, toAwtButton(button), AwtMouseEvent.MOUSE_RELEASED)
        composeScene?.sendPointerEvent(
            eventType = PointerEventType.Release,
            position = toComposeOffset(mouseX, mouseY),
            type = PointerType.Mouse,
            button = PointerButton(button),
            nativeEvent = event
        )
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        val event = awtMouse(mouseX, mouseY, toAwtButton(button), AwtMouseEvent.MOUSE_DRAGGED)
        composeScene?.sendPointerEvent(
            PointerEventType.Move,
            position = toComposeOffset(mouseX, mouseY),
            type = PointerType.Mouse,
            button = PointerButton(button),
            nativeEvent = event
        )
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        val event = AwtMouseWheelEvent(
            awtComponent, AwtMouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), awtMods(),
            (mouseX * scale).toInt(), (mouseY * scale).toInt(), 0, false,
            AwtMouseWheelEvent.WHEEL_UNIT_SCROLL, 1, (-delta).toInt()
        )
        composeScene?.sendPointerEvent(
            position = toComposeOffset(mouseX, mouseY),
            eventType = PointerEventType.Scroll,
            scrollDelta = Offset(0f, (-delta * scale).toFloat()),
            nativeEvent = event
        )
        return super.mouseScrolled(mouseX, mouseY, delta)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && unsavedChanges.value && !confirmingClose.value) {
            // Consume it: vanilla would close through shouldCloseOnEsc and drop the edits silently.
            confirmingClose.value = true
            return true
        }
        composeScene?.sendKeyEvent(composeKeyEvent(AwtKeyEvent.KEY_PRESSED, glfwToAwtKeyCode(keyCode), AwtKeyEvent.CHAR_UNDEFINED))
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        composeScene?.sendKeyEvent(composeKeyEvent(AwtKeyEvent.KEY_RELEASED, glfwToAwtKeyCode(keyCode), 0.toChar()))
        return super.keyReleased(keyCode, scanCode, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        composeScene?.sendKeyEvent(composeKeyEvent(AwtKeyEvent.KEY_TYPED, AwtKeyEvent.VK_UNDEFINED, codePoint))
        return super.charTyped(codePoint, modifiers)
    }

    private fun awtMouse(x: Double, y: Double, button: Int, eventType: Int): AwtMouseEvent {
        val s = scale
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
        val native = AwtKeyEvent(
            awtComponent, id, System.currentTimeMillis(), awtMods(),
            keyCode, keyChar, location
        )
        val mods = awtMods()
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
}


@Composable
internal fun ComposeTestContent(
    initial: V9State,
    defaults: V9State,
    onSave: (V9State) -> Unit,
    onStageBounds: (Rect) -> Unit,
    onPresetSave: (Int, V9State) -> Unit,
    onPresetLoad: (Int) -> V9State?,
    onPreview: (V9State) -> Unit = {},
    unsaved: MutableState<Boolean> = mutableStateOf(false),
    confirmingClose: MutableState<Boolean> = mutableStateOf(false)
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ui.accent,
            background = Ui.surface0,
            surface = Ui.surface2,
            onPrimary = Ui.textOnAccent,
            onBackground = Ui.textPrimary,
            onSurface = Ui.textPrimary
        )
    ) {
        var selectedNav by remember { mutableStateOf("时停") }
        var search by remember { mutableStateOf("") }
        val holder = remember { V9Holder(initial) }
        var voronoiPreset by remember { mutableStateOf(0) }
        var presetSlot by remember { mutableStateOf(0) }
        var sectionTab by remember { mutableStateOf("基础") }
        var dirty by remember { mutableStateOf(false) }
        var revision by remember { mutableStateOf(0) }
        fun markDirty() { dirty = true; revision++ }
        val baseline = remember { initial }

        fun toggle(id: String) {
            when (id) {
                "enabled" -> holder.enabled = !holder.enabled
                "invulnerable" -> holder.invulnerable = !holder.invulnerable
                "grayScreen" -> holder.grayScreen = !holder.grayScreen
                "grayAnimate" -> holder.grayAnimate = !holder.grayAnimate
                "soundEnabled" -> holder.soundEnabled = !holder.soundEnabled
                "freezeSelf" -> holder.freezeSelf = !holder.freezeSelf
                "freezeEntities" -> holder.freezeEntities = !holder.freezeEntities
                "freezeBlocks" -> holder.freezeBlocks = !holder.freezeBlocks
                "freezeFluids" -> holder.freezeFluids = !holder.freezeFluids
                "freezeBossAI" -> holder.freezeBossAI = !holder.freezeBossAI
                "slashEnabled" -> holder.slashEnabled = !holder.slashEnabled
                "slashDepthTest" -> holder.slashDepthTest = !holder.slashDepthTest
                "slashRandomAngle" -> holder.slashRandomAngle = !holder.slashRandomAngle
                "slashGlow" -> holder.slashGlow = !holder.slashGlow
                "slashStarfield" -> holder.slashStarfield = !holder.slashStarfield
                "uiTabCompact" -> holder.uiTabCompact = !holder.uiTabCompact
                "rewindEnabled" -> holder.rewindEnabled = !holder.rewindEnabled
                "rewindPositionRewind" -> holder.rewindPositionRewind = !holder.rewindPositionRewind
                "rewindPlayerState" -> holder.rewindPlayerState = !holder.rewindPlayerState
                "rewindDeathEnabled" -> holder.rewindDeathEnabled = !holder.rewindDeathEnabled
                "rewindSafetyCheckpoint" -> holder.rewindSafetyCheckpoint = !holder.rewindSafetyCheckpoint
                "rewindHostileCheck" -> holder.rewindHostileCheck = !holder.rewindHostileCheck
                "rewindOtherItemDeduct" -> holder.rewindOtherItemDeduct = !holder.rewindOtherItemDeduct
                "rewindFreezeOthers" -> holder.rewindFreezeOthers = !holder.rewindFreezeOthers
                "rewindTimestopStacking" -> holder.rewindTimestopStacking = !holder.rewindTimestopStacking
                "rewindBlocks" -> holder.rewindBlocks = !holder.rewindBlocks
                "rewindBlockEntities" -> holder.rewindBlockEntities = !holder.rewindBlockEntities
                "rewindEntities" -> holder.rewindEntities = !holder.rewindEntities
                "rewindItems" -> holder.rewindItems = !holder.rewindItems
                "rewindExperience" -> holder.rewindExperience = !holder.rewindExperience
                "rewindTime" -> holder.rewindTime = !holder.rewindTime
                "rewindWeather" -> holder.rewindWeather = !holder.rewindWeather
                "rewindRaids" -> holder.rewindRaids = !holder.rewindRaids
                "rewindScoreboard" -> holder.rewindScoreboard = !holder.rewindScoreboard
                "rewindWorldBorder" -> holder.rewindWorldBorder = !holder.rewindWorldBorder
                "rewindFreeCamRestorePosition" -> holder.rewindFreeCamRestorePosition = !holder.rewindFreeCamRestorePosition
                "rewindShowStats" -> holder.rewindShowStats = !holder.rewindShowStats
                "domainStarfield" -> holder.domainStarfield = !holder.domainStarfield
                else -> holder.showMessage = !holder.showMessage
            }
            markDirty()
        }

        fun setSegment(id: String, v: Int) {
            if (id == "startAnim") holder.startAnim = v
            else if (id == "endAnim") holder.endAnim = v
            else if (id == "triggerMode") holder.triggerMode = v
            else if (id == "renderStyle") holder.renderStyle = renderStyleStored(v)
            else if (id == "slashStarColorMode") holder.slashStarColorMode = v
            else if (id == "uiTabVariant") holder.uiTabVariant = v
            else if (id == "uiTabSize") holder.uiTabSize = v
            else if (id == "uiSliderHeight") holder.uiSliderHeight = v
            else if (id == "uiSliderThumb") holder.uiSliderThumb = v
            else if (id == "uiSearchStyle") holder.uiSearchStyle = v
            else if (id == "uiSwitchStyle") holder.uiSwitchStyle = v
            else if (id == "voronoiPreset") voronoiPreset = v
            else if (id == "voronoiColorCount") holder.voronoiColorCount = v
            else if (id == "voronoiStepsPerColor") holder.voronoiStepsPerColor = v
            else if (id == "rewindScopeMode") holder.rewindScopeMode = v
            else if (id == "rewindScope") holder.rewindScope = v
            else if (id == "rewindPlaybackMode") holder.rewindPlaybackMode = v
            else if (id == "rewindRestoreOrder") holder.rewindRestoreOrder = v
            else if (id == "rewindCameraMode") holder.rewindCameraMode = v
            else if (id == "rewindPositionMode") holder.rewindPositionMode = v
            else holder.grayStyle = v
            markDirty()
        }

        fun setVoronoiColor(idx: Int, ch: Char, v: Float) {
            val value = (v.toInt() and 0xFF)
            val mask = when (ch) {
                'R' -> 0xFF00FFFF.toInt()
                'G' -> 0xFFFF00FF.toInt()
                'B' -> 0xFFFFFF00.toInt()
                else -> 0x00FFFFFF
            }
            val shift = when (ch) {
                'R' -> 16
                'G' -> 8
                'B' -> 0
                else -> 24
            }
            fun patch(current: Int): Int = (current and mask) or (value shl shift)
            when (idx) {
                0 -> holder.voronoiColor0 = patch(holder.voronoiColor0)
                1 -> holder.voronoiColor1 = patch(holder.voronoiColor1)
                2 -> holder.voronoiColor2 = patch(holder.voronoiColor2)
                3 -> holder.voronoiColor3 = patch(holder.voronoiColor3)
                4 -> holder.voronoiColor4 = patch(holder.voronoiColor4)
                5 -> holder.voronoiColorGlow = patch(holder.voronoiColorGlow)
                else -> holder.voronoiColorGap = patch(holder.voronoiColorGap)
            }
        }

        fun setSilkColor(idx: Int, ch: Char, v: Float) {
            val value = (v.toInt() and 0xFF)
            val mask = when (ch) {
                'R' -> 0xFF00FFFF.toInt()
                'G' -> 0xFFFF00FF.toInt()
                else -> 0xFFFFFF00.toInt()
            }
            val shift = when (ch) {
                'R' -> 16
                'G' -> 8
                else -> 0
            }
            fun patch(current: Int): Int = (current and mask) or (value shl shift)
            when (idx) {
                0 -> holder.silkColor0 = patch(holder.silkColor0)
                1 -> holder.silkColor1 = patch(holder.silkColor1)
                2 -> holder.silkColor2 = patch(holder.silkColor2)
                3 -> holder.silkColor3 = patch(holder.silkColor3)
                4 -> holder.silkColor4 = patch(holder.silkColor4)
                5 -> holder.silkColor5 = patch(holder.silkColor5)
                6 -> holder.silkColor6 = patch(holder.silkColor6)
                else -> holder.silkColor7 = patch(holder.silkColor7)
            }
        }

        fun setSlider(id: String, v: Float) {
            when (id) {
                "startDuration" -> holder.startDuration = v
                "endDuration" -> holder.endDuration = v
                "grayStrength" -> holder.grayStrength = v
                "particleSize" -> holder.particleSize = v
                "stopDuration" -> holder.stopDuration = v
                "cooldown" -> holder.cooldown = v.toInt()
                "soundVolume" -> holder.soundVolume = v
                "particleAlpha" -> holder.particleAlpha = v
                "particleCount" -> holder.particleCount = v.toInt()
                "particleSpin" -> holder.particleSpin = v
                "stopRadius" -> holder.stopRadius = v
                "slashDuration" -> holder.slashDuration = v
                "slashLengthMult" -> holder.slashLengthMult = v
                "slashWidthRatio" -> holder.slashWidthRatio = v
                "slashThicknessRatio" -> holder.slashThicknessRatio = v
                "slashTipFade" -> holder.slashTipFade = v
                "slashStartScale" -> holder.slashStartScale = v
                "slashEndScale" -> holder.slashEndScale = v
                "slashCoreWidth" -> holder.slashCoreWidth = v
                "slashCoreShade" -> holder.slashCoreShade = v
                "slashEdgeWidth" -> holder.slashEdgeWidth = v
                "slashEdgeBrightness" -> holder.slashEdgeBrightness = v
                "slashGlowWidth" -> holder.slashGlowWidth = v
                "slashGlowStrength" -> holder.slashGlowStrength = v
                "slashNoiseStrength" -> holder.slashNoiseStrength = v
                "slashSweepSpeed" -> holder.slashSweepSpeed = v
                "slashSweepSoftness" -> holder.slashSweepSoftness = v
                "slashHoldFraction" -> holder.slashHoldFraction = v
                "slashFadeStart" -> holder.slashFadeStart = v
                "slashFadeDuration" -> holder.slashFadeDuration = v
                "slashSurfaceOffset" -> holder.slashSurfaceOffset = v
                "slashRollRange" -> holder.slashRollRange = v
                "silkBrightness" -> holder.silkBrightness = v
                "silkContrast" -> holder.silkContrast = v
                "silkSaturation" -> holder.silkSaturation = v
                "silkScale" -> holder.silkScale = v
                "silkIntensity" -> holder.silkIntensity = v
                "silkWarp" -> holder.silkWarp = v
                "silkDetail" -> holder.silkDetail = v
                "silkHue" -> holder.silkHue = v
                "silkSeed" -> holder.silkSeed = v
                "silkRotation" -> holder.silkRotation = v
                "silkDrift" -> holder.silkDrift = v
                "silkVignette" -> holder.silkVignette = v
                "silkBlur" -> holder.silkBlur = v
                "silkGrain" -> holder.silkGrain = v
                "tunnelSpeed" -> holder.tunnelSpeed = v
                "tunnelBrightness" -> holder.tunnelBrightness = v
                "tunnelDensity" -> holder.tunnelDensity = v
                "tunnelFov" -> holder.tunnelFov = v
                "voronoiDistortion" -> holder.voronoiDistortion = v
                "voronoiGap" -> holder.voronoiGap = v
                "voronoiGlow" -> holder.voronoiGlow = v
                "voronoiScale" -> holder.voronoiScale = v
                "voronoiFov" -> holder.voronoiFov = v
                "voronoiSpeed" -> holder.voronoiSpeed = v
                "voronoiRotation" -> holder.voronoiRotation = v
                "voronoiOffsetX" -> holder.voronoiOffsetX = v
                "voronoiOffsetY" -> holder.voronoiOffsetY = v
                "slashStarDensity" -> holder.slashStarDensity = v
                "slashStarBrightness" -> holder.slashStarBrightness = v
                "slashStarSize" -> holder.slashStarSize = v
                "slashCoreColorR" -> holder.slashCoreColor = (holder.slashCoreColor and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "slashCoreColorG" -> holder.slashCoreColor = (holder.slashCoreColor and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "slashCoreColorB" -> holder.slashCoreColor = (holder.slashCoreColor and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "slashEdgeColorR" -> holder.slashEdgeColor = (holder.slashEdgeColor and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "slashEdgeColorG" -> holder.slashEdgeColor = (holder.slashEdgeColor and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "slashEdgeColorB" -> holder.slashEdgeColor = (holder.slashEdgeColor and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "slashGlowColorR" -> holder.slashGlowColor = (holder.slashGlowColor and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "slashGlowColorG" -> holder.slashGlowColor = (holder.slashGlowColor and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "slashGlowColorB" -> holder.slashGlowColor = (holder.slashGlowColor and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "rewindWindowSeconds" -> holder.rewindWindowSeconds = v.toInt()
                "rewindRadius" -> holder.rewindRadius = v
                "rewindCooldownTicks" -> holder.rewindCooldownTicks = v.toInt()
                "rewindPlaybackSeconds" -> holder.rewindPlaybackSeconds = v
                "rewindDeathCooldownTicks" -> holder.rewindDeathCooldownTicks = v.toInt()
                "rewindDeathMaxRetries" -> holder.rewindDeathMaxRetries = v.toInt()
                "uiSearchWidth" -> holder.uiSearchWidth = v
                "uiSearchHeight" -> holder.uiSearchHeight = v
                "uiSearchRadius" -> holder.uiSearchRadius = v
                "uiSwitchWidth" -> holder.uiSwitchWidth = v
                "uiSwitchHeight" -> holder.uiSwitchHeight = v
                "uiSwitchKnob" -> holder.uiSwitchKnob = v
                "uiSwitchGradientAR" -> holder.uiSwitchGradientA = (holder.uiSwitchGradientA and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchGradientAG" -> holder.uiSwitchGradientA = (holder.uiSwitchGradientA and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchGradientAB" -> holder.uiSwitchGradientA = (holder.uiSwitchGradientA and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchGradientBR" -> holder.uiSwitchGradientB = (holder.uiSwitchGradientB and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchGradientBG" -> holder.uiSwitchGradientB = (holder.uiSwitchGradientB and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchGradientBB" -> holder.uiSwitchGradientB = (holder.uiSwitchGradientB and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTrackOffR" -> holder.uiSwitchTrackOff = (holder.uiSwitchTrackOff and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTrackOffG" -> holder.uiSwitchTrackOff = (holder.uiSwitchTrackOff and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTrackOffB" -> holder.uiSwitchTrackOff = (holder.uiSwitchTrackOff and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchSlideTrackR" -> holder.uiSwitchSlideTrack = (holder.uiSwitchSlideTrack and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchSlideTrackG" -> holder.uiSwitchSlideTrack = (holder.uiSwitchSlideTrack and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchSlideTrackB" -> holder.uiSwitchSlideTrack = (holder.uiSwitchSlideTrack and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchSlideOffR" -> holder.uiSwitchSlideOff = (holder.uiSwitchSlideOff and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchSlideOffG" -> holder.uiSwitchSlideOff = (holder.uiSwitchSlideOff and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchSlideOffB" -> holder.uiSwitchSlideOff = (holder.uiSwitchSlideOff and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchSlideOnR" -> holder.uiSwitchSlideOn = (holder.uiSwitchSlideOn and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchSlideOnG" -> holder.uiSwitchSlideOn = (holder.uiSwitchSlideOn and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchSlideOnB" -> holder.uiSwitchSlideOn = (holder.uiSwitchSlideOn and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTextTrackOffR" -> holder.uiSwitchTextTrackOff = (holder.uiSwitchTextTrackOff and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTextTrackOffG" -> holder.uiSwitchTextTrackOff = (holder.uiSwitchTextTrackOff and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTextTrackOffB" -> holder.uiSwitchTextTrackOff = (holder.uiSwitchTextTrackOff and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTextTrackOnR" -> holder.uiSwitchTextTrackOn = (holder.uiSwitchTextTrackOn and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTextTrackOnG" -> holder.uiSwitchTextTrackOn = (holder.uiSwitchTextTrackOn and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTextTrackOnB" -> holder.uiSwitchTextTrackOn = (holder.uiSwitchTextTrackOn and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTextKnobR" -> holder.uiSwitchTextKnob = (holder.uiSwitchTextKnob and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTextKnobG" -> holder.uiSwitchTextKnob = (holder.uiSwitchTextKnob and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTextKnobB" -> holder.uiSwitchTextKnob = (holder.uiSwitchTextKnob and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTextColorR" -> holder.uiSwitchTextColor = (holder.uiSwitchTextColor and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTextColorG" -> holder.uiSwitchTextColor = (holder.uiSwitchTextColor and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTextColorB" -> holder.uiSwitchTextColor = (holder.uiSwitchTextColor and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
            }
            if (id.startsWith("silkColor") && id.length == 11) {
                val idx = id[9].digitToIntOrNull()
                if (idx != null) {
                    setSilkColor(idx, id[10], v)
                }
            }
            if (id.startsWith("voronoiColor")) {
                // ids are voronoiColor<0-4><RGB A> and voronoiColor<Glow|Gap><RGBA>; the channel is
                // always the last character, and slot names have no leading digits at all.
                val tail = id.removePrefix("voronoiColor")
                val digits = tail.takeWhile { it.isDigit() }
                val rest = tail.drop(digits.length)
                val channel = rest.lastOrNull()
                val slot = digits.toIntOrNull()
                    ?: if (rest.startsWith("Glow")) 5 else if (rest.startsWith("Gap")) 6 else null
                if (slot != null && channel != null) {
                    setVoronoiColor(slot, channel, v)
                }
            }
            markDirty()
        }

        fun setText(id: String, v: String) {
            if (id == "uiSwitchTextOn") holder.uiSwitchTextOn = v
            else if (id == "uiSwitchTextOff") holder.uiSwitchTextOff = v
            markDirty()
        }

        fun setBall(v: Int) {
            holder.ballColor = v
            holder.ballColorCustom = false
            markDirty()
        }

        fun setCustomColor(v: Int) {
            holder.customColor = v
            holder.ballColorCustom = true
            markDirty()
        }

        fun currentState(): V9State = holder.snapshot()

        fun savePreset() {
            onPresetSave(presetSlot, currentState())
        }

        fun apply(s: V9State) {
            holder.restore(s)
        }

        fun loadPreset(slot: Int) {
            onPresetLoad(slot)?.let {
                presetSlot = slot
                apply(it)
                markDirty()
            }
        }

        fun cancel() {
            apply(baseline)
            dirty = false
            Minecraft.getInstance().setScreen(null)
        }

        fun resetDefaults() {
            apply(defaults)
            markDirty()
        }

        fun resetWeaponDefaults() {
            holder.renderStyle = defaults.renderStyle
            holder.silkColor0 = defaults.silkColor0
            holder.silkColor1 = defaults.silkColor1
            holder.silkColor2 = defaults.silkColor2
            holder.silkColor3 = defaults.silkColor3
            holder.silkColor4 = defaults.silkColor4
            holder.silkColor5 = defaults.silkColor5
            holder.silkColor6 = defaults.silkColor6
            holder.silkColor7 = defaults.silkColor7
            holder.silkBrightness = defaults.silkBrightness
            holder.silkContrast = defaults.silkContrast
            holder.silkSaturation = defaults.silkSaturation
            holder.silkScale = defaults.silkScale
            holder.silkIntensity = defaults.silkIntensity
            holder.silkWarp = defaults.silkWarp
            holder.silkDetail = defaults.silkDetail
            holder.silkHue = defaults.silkHue
            holder.silkSeed = defaults.silkSeed
            holder.silkRotation = defaults.silkRotation
            holder.silkDrift = defaults.silkDrift
            holder.silkVignette = defaults.silkVignette
            holder.silkBlur = defaults.silkBlur
            holder.silkGrain = defaults.silkGrain
            holder.tunnelSpeed = defaults.tunnelSpeed
            holder.tunnelBrightness = defaults.tunnelBrightness
            holder.tunnelDensity = defaults.tunnelDensity
            holder.tunnelFov = defaults.tunnelFov
            holder.voronoiColorCount = defaults.voronoiColorCount
            holder.voronoiStepsPerColor = defaults.voronoiStepsPerColor
            holder.voronoiColor0 = defaults.voronoiColor0
            holder.voronoiColor1 = defaults.voronoiColor1
            holder.voronoiColor2 = defaults.voronoiColor2
            holder.voronoiColor3 = defaults.voronoiColor3
            holder.voronoiColor4 = defaults.voronoiColor4
            holder.voronoiColorGlow = defaults.voronoiColorGlow
            holder.voronoiColorGap = defaults.voronoiColorGap
            holder.voronoiDistortion = defaults.voronoiDistortion
            holder.voronoiGap = defaults.voronoiGap
            holder.voronoiGlow = defaults.voronoiGlow
            holder.voronoiScale = defaults.voronoiScale
            holder.voronoiFov = defaults.voronoiFov
            holder.voronoiSpeed = defaults.voronoiSpeed
            holder.voronoiRotation = defaults.voronoiRotation
            holder.voronoiOffsetX = defaults.voronoiOffsetX
            holder.voronoiOffsetY = defaults.voronoiOffsetY
            markDirty()
        }

        fun resetSilkDefaults() {
            holder.silkColor0 = defaults.silkColor0
            holder.silkColor1 = defaults.silkColor1
            holder.silkColor2 = defaults.silkColor2
            holder.silkColor3 = defaults.silkColor3
            holder.silkColor4 = defaults.silkColor4
            holder.silkColor5 = defaults.silkColor5
            holder.silkColor6 = defaults.silkColor6
            holder.silkColor7 = defaults.silkColor7
            holder.silkBrightness = defaults.silkBrightness
            holder.silkContrast = defaults.silkContrast
            holder.silkSaturation = defaults.silkSaturation
            holder.silkScale = defaults.silkScale
            holder.silkIntensity = defaults.silkIntensity
            holder.silkWarp = defaults.silkWarp
            holder.silkDetail = defaults.silkDetail
            holder.silkHue = defaults.silkHue
            holder.silkSeed = defaults.silkSeed
            holder.silkRotation = defaults.silkRotation
            holder.silkDrift = defaults.silkDrift
            holder.silkVignette = defaults.silkVignette
            holder.silkBlur = defaults.silkBlur
            holder.silkGrain = defaults.silkGrain
            markDirty()
        }

        fun resetTunnelDefaults() {
            holder.tunnelSpeed = defaults.tunnelSpeed
            holder.tunnelBrightness = defaults.tunnelBrightness
            holder.tunnelDensity = defaults.tunnelDensity
            holder.tunnelFov = defaults.tunnelFov
            markDirty()
        }

        fun resetVoronoiDefaults() {
            holder.voronoiColorCount = defaults.voronoiColorCount
            holder.voronoiStepsPerColor = defaults.voronoiStepsPerColor
            holder.voronoiColor0 = defaults.voronoiColor0
            holder.voronoiColor1 = defaults.voronoiColor1
            holder.voronoiColor2 = defaults.voronoiColor2
            holder.voronoiColor3 = defaults.voronoiColor3
            holder.voronoiColor4 = defaults.voronoiColor4
            holder.voronoiColorGlow = defaults.voronoiColorGlow
            holder.voronoiColorGap = defaults.voronoiColorGap
            holder.voronoiDistortion = defaults.voronoiDistortion
            holder.voronoiGap = defaults.voronoiGap
            holder.voronoiGlow = defaults.voronoiGlow
            holder.voronoiScale = defaults.voronoiScale
            holder.voronoiFov = defaults.voronoiFov
            holder.voronoiSpeed = defaults.voronoiSpeed
            holder.voronoiRotation = defaults.voronoiRotation
            holder.voronoiOffsetX = defaults.voronoiOffsetX
            holder.voronoiOffsetY = defaults.voronoiOffsetY
            markDirty()
        }

        fun resetSlashDefaults() {
            holder.slashEnabled = defaults.slashEnabled
            holder.slashDepthTest = defaults.slashDepthTest
            holder.slashRandomAngle = defaults.slashRandomAngle
            holder.slashGlow = defaults.slashGlow
            holder.slashDuration = defaults.slashDuration
            holder.slashLengthMult = defaults.slashLengthMult
            holder.slashWidthRatio = defaults.slashWidthRatio
            holder.slashThicknessRatio = defaults.slashThicknessRatio
            holder.slashTipFade = defaults.slashTipFade
            holder.slashStartScale = defaults.slashStartScale
            holder.slashEndScale = defaults.slashEndScale
            holder.slashCoreWidth = defaults.slashCoreWidth
            holder.slashCoreShade = defaults.slashCoreShade
            holder.slashEdgeWidth = defaults.slashEdgeWidth
            holder.slashEdgeBrightness = defaults.slashEdgeBrightness
            holder.slashGlowWidth = defaults.slashGlowWidth
            holder.slashGlowStrength = defaults.slashGlowStrength
            holder.slashNoiseStrength = defaults.slashNoiseStrength
            holder.slashSweepSpeed = defaults.slashSweepSpeed
            holder.slashSweepSoftness = defaults.slashSweepSoftness
            holder.slashHoldFraction = defaults.slashHoldFraction
            holder.slashFadeStart = defaults.slashFadeStart
            holder.slashFadeDuration = defaults.slashFadeDuration
            holder.slashSurfaceOffset = defaults.slashSurfaceOffset
            holder.slashRollRange = defaults.slashRollRange
            holder.slashCoreColor = defaults.slashCoreColor
            holder.slashEdgeColor = defaults.slashEdgeColor
            holder.slashGlowColor = defaults.slashGlowColor
            holder.slashStarfield = defaults.slashStarfield
            holder.slashStarDensity = defaults.slashStarDensity
            holder.slashStarBrightness = defaults.slashStarBrightness
            holder.slashStarSize = defaults.slashStarSize
            holder.slashStarColorMode = defaults.slashStarColorMode
            markDirty()
        }

        fun resetSlashBaseDefaults() {
            holder.slashEnabled = defaults.slashEnabled
            holder.slashDepthTest = defaults.slashDepthTest
            holder.slashRandomAngle = defaults.slashRandomAngle
            holder.slashGlow = defaults.slashGlow
            holder.slashDuration = defaults.slashDuration
            holder.slashLengthMult = defaults.slashLengthMult
            holder.slashWidthRatio = defaults.slashWidthRatio
            holder.slashThicknessRatio = defaults.slashThicknessRatio
            holder.slashTipFade = defaults.slashTipFade
            holder.slashStartScale = defaults.slashStartScale
            holder.slashEndScale = defaults.slashEndScale
            markDirty()
        }

        fun resetSlashVisualDefaults() {
            holder.slashCoreWidth = defaults.slashCoreWidth
            holder.slashCoreShade = defaults.slashCoreShade
            holder.slashEdgeWidth = defaults.slashEdgeWidth
            holder.slashEdgeBrightness = defaults.slashEdgeBrightness
            holder.slashGlowWidth = defaults.slashGlowWidth
            holder.slashGlowStrength = defaults.slashGlowStrength
            holder.slashNoiseStrength = defaults.slashNoiseStrength
            holder.slashCoreColor = defaults.slashCoreColor
            holder.slashEdgeColor = defaults.slashEdgeColor
            holder.slashGlowColor = defaults.slashGlowColor
            markDirty()
        }

        fun resetSlashAnimDefaults() {
            holder.slashSweepSpeed = defaults.slashSweepSpeed
            holder.slashSweepSoftness = defaults.slashSweepSoftness
            holder.slashHoldFraction = defaults.slashHoldFraction
            holder.slashFadeStart = defaults.slashFadeStart
            holder.slashFadeDuration = defaults.slashFadeDuration
            holder.slashSurfaceOffset = defaults.slashSurfaceOffset
            holder.slashRollRange = defaults.slashRollRange
            markDirty()
        }

        fun resetSlashStarDefaults() {
            holder.slashStarfield = defaults.slashStarfield
            holder.slashStarDensity = defaults.slashStarDensity
            holder.slashStarBrightness = defaults.slashStarBrightness
            holder.slashStarSize = defaults.slashStarSize
            holder.slashStarColorMode = defaults.slashStarColorMode
            markDirty()
        }

        fun resetUiTabDefaults() {
            holder.uiTabVariant = defaults.uiTabVariant
            holder.uiTabSize = defaults.uiTabSize
            holder.uiTabCompact = defaults.uiTabCompact
            markDirty()
        }

        fun resetUiSliderDefaults() {
            holder.uiSliderHeight = defaults.uiSliderHeight
            holder.uiSliderThumb = defaults.uiSliderThumb
            markDirty()
        }

        fun resetUiSearchDefaults() {
            holder.uiSearchStyle = defaults.uiSearchStyle
            holder.uiSearchWidth = defaults.uiSearchWidth
            holder.uiSearchHeight = defaults.uiSearchHeight
            holder.uiSearchRadius = defaults.uiSearchRadius
            markDirty()
        }

        fun resetUiSwitchDefaults() {
            holder.uiSwitchStyle = defaults.uiSwitchStyle
            holder.uiSwitchWidth = defaults.uiSwitchWidth
            holder.uiSwitchHeight = defaults.uiSwitchHeight
            holder.uiSwitchKnob = defaults.uiSwitchKnob
            holder.uiSwitchGradientA = defaults.uiSwitchGradientA
            holder.uiSwitchGradientB = defaults.uiSwitchGradientB
            holder.uiSwitchTrackOff = defaults.uiSwitchTrackOff
            holder.uiSwitchSlideTrack = defaults.uiSwitchSlideTrack
            holder.uiSwitchSlideOff = defaults.uiSwitchSlideOff
            holder.uiSwitchSlideOn = defaults.uiSwitchSlideOn
            holder.uiSwitchTextTrackOff = defaults.uiSwitchTextTrackOff
            holder.uiSwitchTextTrackOn = defaults.uiSwitchTextTrackOn
            holder.uiSwitchTextKnob = defaults.uiSwitchTextKnob
            holder.uiSwitchTextColor = defaults.uiSwitchTextColor
            holder.uiSwitchTextOn = defaults.uiSwitchTextOn
            holder.uiSwitchTextOff = defaults.uiSwitchTextOff
            holder.domainStarfield = defaults.domainStarfield
            markDirty()
        }

        fun resetTimestopDefaults() {
            holder.enabled = defaults.enabled
            holder.invulnerable = defaults.invulnerable
            holder.grayScreen = defaults.grayScreen
            holder.grayStrength = defaults.grayStrength
            holder.grayAnimate = defaults.grayAnimate
            holder.startAnim = defaults.startAnim
            holder.endAnim = defaults.endAnim
            holder.startDuration = defaults.startDuration
            holder.endDuration = defaults.endDuration
            holder.ballColor = defaults.ballColor
            holder.ballColorCustom = defaults.ballColorCustom
            holder.customColor = defaults.customColor
            holder.particleSize = defaults.particleSize
            holder.stopDuration = defaults.stopDuration
            holder.triggerMode = defaults.triggerMode
            holder.soundEnabled = defaults.soundEnabled
            holder.cooldown = defaults.cooldown
            holder.soundVolume = defaults.soundVolume
            holder.particleAlpha = defaults.particleAlpha
            holder.particleCount = defaults.particleCount
            holder.particleSpin = defaults.particleSpin
            holder.freezeSelf = defaults.freezeSelf
            holder.stopRadius = defaults.stopRadius
            holder.freezeEntities = defaults.freezeEntities
            holder.freezeBlocks = defaults.freezeBlocks
            holder.freezeFluids = defaults.freezeFluids
            holder.freezeBossAI = defaults.freezeBossAI
            holder.showMessage = defaults.showMessage
            holder.grayStyle = defaults.grayStyle
            markDirty()
        }

        fun resetRewindDefaults() {
            holder.rewindEnabled = defaults.rewindEnabled
            holder.rewindWindowSeconds = defaults.rewindWindowSeconds
            holder.rewindScopeMode = defaults.rewindScopeMode
            holder.rewindScope = defaults.rewindScope
            holder.rewindRadius = defaults.rewindRadius
            holder.rewindCooldownTicks = defaults.rewindCooldownTicks
            holder.rewindPlaybackMode = defaults.rewindPlaybackMode
            holder.rewindPlaybackSeconds = defaults.rewindPlaybackSeconds
            holder.rewindRestoreOrder = defaults.rewindRestoreOrder
            holder.rewindCameraMode = defaults.rewindCameraMode
            holder.rewindPositionRewind = defaults.rewindPositionRewind
            holder.rewindPositionMode = defaults.rewindPositionMode
            holder.rewindPlayerState = defaults.rewindPlayerState
            holder.rewindDeathEnabled = defaults.rewindDeathEnabled
            holder.rewindDeathCooldownTicks = defaults.rewindDeathCooldownTicks
            holder.rewindDeathMaxRetries = defaults.rewindDeathMaxRetries
            holder.rewindSafetyCheckpoint = defaults.rewindSafetyCheckpoint
            holder.rewindHostileCheck = defaults.rewindHostileCheck
            holder.rewindOtherItemDeduct = defaults.rewindOtherItemDeduct
            holder.rewindFreezeOthers = defaults.rewindFreezeOthers
            holder.rewindTimestopStacking = defaults.rewindTimestopStacking
            holder.rewindBlocks = defaults.rewindBlocks
            holder.rewindBlockEntities = defaults.rewindBlockEntities
            holder.rewindEntities = defaults.rewindEntities
            holder.rewindItems = defaults.rewindItems
            holder.rewindExperience = defaults.rewindExperience
            holder.rewindTime = defaults.rewindTime
            holder.rewindWeather = defaults.rewindWeather
            holder.rewindRaids = defaults.rewindRaids
            holder.rewindScoreboard = defaults.rewindScoreboard
            holder.rewindWorldBorder = defaults.rewindWorldBorder
            holder.rewindFreeCamRestorePosition = defaults.rewindFreeCamRestorePosition
            holder.rewindShowStats = defaults.rewindShowStats
            markDirty()
        }

        fun save() {
            onSave(currentState())
            dirty = false
        }

        fun saveAndClose() {
            save()
            Minecraft.getInstance().setScreen(null)
        }

        /** Closing without saving is enough: removed() rolls the client copy back to the baseline. */
        fun discardAndClose() {
            Minecraft.getInstance().setScreen(null)
        }

        LaunchedEffect(dirty) { unsaved.value = dirty }

        // Live preview. The world renderers read this same stack, so a debounced client-side write
        // shows edits immediately without spamming the server with a packet per drag event.
        LaunchedEffect(revision) {
            if (revision == 0) return@LaunchedEffect
            delay(140)
            onPreview(currentState())
        }

        fun quote(): String = quotes[LocalDate.now().dayOfYear % quotes.size]

        val q = search.trim().lowercase()
        val visible = v9Sections.map { sec ->
            sec to sec.rows.filter {
                q.isEmpty() || it.name.lowercase().contains(q) || it.desc.lowercase().contains(q)
            }
        }.filter { it.second.isNotEmpty() && navSections[selectedNav]?.contains(it.first.name) == true }
        val panelScrolls = remember { mutableStateMapOf<String, ScrollState>() }
        CompositionLocalProvider(
            LocalV9UiPrefs provides V9UiPrefs(holder),
            LocalPanelScrolls provides panelScrolls
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Ui.surface0.copy(alpha = 0.60f))) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight
            val winW = if (availableWidth >= 200.dp) (availableWidth - 16.dp).coerceAtMost(840.dp) else availableWidth
            val winH = if (availableHeight >= 200.dp) (availableHeight - 16.dp).coerceAtMost(480.dp) else availableHeight
            Surface(
                modifier = Modifier.align(Alignment.Center).width(winW).height(winH)
                    .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Ui.shadowWindow, spotColor = Ui.accentTintStrong),
                shape = RoundedCornerShape(20.dp),
                color = Ui.surface0,
                border = BorderStroke(1.dp, Ui.hairlineStrong)
            ) {
                Column(Modifier.fillMaxSize()) {
                    V9Header(search = search, onSearch = { search = it }, dirty = dirty)
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        val compact = availableWidth < 560.dp
                        V9Sidebar(Modifier.weight(if (compact) 0.20f else 0.16f).fillMaxHeight(), selectedNav) {
                            selectedNav = it
                            sectionTab = "基础"
                        }
                        V9Center(
                            Modifier.weight(if (compact) 0.80f else 0.54f).fillMaxHeight(),
                            selectedNav, visible,
                            presetSlot, ::loadPreset, ::savePreset,
                            ::toggle, ::setSegment, ::setText, ::setSlider, ::setBall, ::setCustomColor,
                            holder,
                            sectionTab, { sectionTab = it },
                            { page ->
                                when (page) {
                                    "丝绸" -> resetSilkDefaults()
                                    "隧道" -> resetTunnelDefaults()
                                    "晶格" -> resetVoronoiDefaults()
                                    "斩击基础" -> resetSlashBaseDefaults()
                                    "斩击外观" -> resetSlashVisualDefaults()
                                    "斩击动画" -> resetSlashAnimDefaults()
                                    "斩击星空" -> resetSlashStarDefaults()
                                    "设置标签栏" -> resetUiTabDefaults()
                                    "设置滑块" -> resetUiSliderDefaults()
                                    "设置搜索框" -> resetUiSearchDefaults()
                                    "设置开关" -> resetUiSwitchDefaults()
                                    "回溯" -> resetRewindDefaults()
                                    else -> resetTimestopDefaults()
                                }
                            }
                        )
                        if (!compact) {
                            V9Preview(
                                Modifier.weight(0.30f).fillMaxHeight(),
                                holder,
                                onStageBounds
                            )
                        }
                    }
                    V9Footer(quote(), ::resetDefaults, ::cancel, ::save)
                }
            }
            if (confirmingClose.value) {
                V9CloseConfirmDialog(
                    onSaveClose = ::saveAndClose,
                    onDiscardClose = ::discardAndClose,
                    onKeepEditing = { confirmingClose.value = false }
                )
            }
            }
        }
    }
}

