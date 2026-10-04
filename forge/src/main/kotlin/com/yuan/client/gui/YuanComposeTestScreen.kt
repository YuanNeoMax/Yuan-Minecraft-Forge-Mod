package com.yuan.client.gui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key as ComposeKey
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.lwjgl.opengl.GL21C
import org.lwjgl.opengl.GL30C
import java.awt.Component as AwtComponent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent as AwtKeyEvent
import java.awt.event.MouseEvent as AwtMouseEvent
import java.awt.event.MouseWheelEvent as AwtMouseWheelEvent
import java.nio.ByteBuffer
import java.time.LocalDate

@OptIn(ExperimentalTextApi::class)
private val cnFont: FontFamily = FontFamily("Microsoft YaHei")

private data class V9UiPrefs(
    val sliderHeight: Dp = 24.dp,
    val sliderThumb: Dp = 18.dp,
    val searchStyle: Int = 0,
    val searchWidth: Dp = 200.dp,
    val searchHeight: Dp = 40.dp,
    val searchRadius: Dp = 20.dp,
    val switchStyle: Int = 0,
    val switchWidth: Dp = 62.dp,
    val switchHeight: Dp = 28.dp,
    val switchKnob: Dp = 22.dp,
    val switchGradientA: Int = 0xFFF19AF3.toInt(),
    val switchGradientB: Int = 0xFFF099B5.toInt(),
    val switchTrackOff: Int = 0xFFD7D7D7.toInt(),
    val switchSlideTrack: Int = 0xFFFFFFFF.toInt(),
    val switchSlideOff: Int = 0xFFCCCCCC.toInt(),
    val switchSlideOn: Int = 0xFF59D102.toInt(),
    val switchTextTrackOff: Int = 0xFF05012C.toInt(),
    val switchTextTrackOn: Int = 0xFFFFB500.toInt(),
    val switchTextKnob: Int = 0xFFFFFFFF.toInt(),
    val switchTextColor: Int = 0xFF78768D.toInt(),
    val switchTextOn: String = "On",
    val switchTextOff: String = "Off"
)

private val LocalV9UiPrefs = compositionLocalOf { V9UiPrefs() }

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
        closeSurfaceResources()
        super.removed()
    }

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
private fun ComposeTestContent(
    initial: V9State,
    defaults: V9State,
    onSave: (V9State) -> Unit,
    onStageBounds: (Rect) -> Unit,
    onPresetSave: (Int, V9State) -> Unit,
    onPresetLoad: (Int) -> V9State?
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF7A5C),
            background = Color(0xFF0B0D11),
            surface = Color(0xFF12151B),
            onPrimary = Color(0xFF1B100C),
            onBackground = Color(0xFFF2F4F8),
            onSurface = Color(0xFFF2F4F8)
        )
    ) {
        var selectedNav by remember { mutableStateOf("时停") }
        var search by remember { mutableStateOf("") }
        var enabled by remember { mutableStateOf(initial.enabled) }
        var invulnerable by remember { mutableStateOf(initial.invulnerable) }
        var grayScreen by remember { mutableStateOf(initial.grayScreen) }
        var grayStrength by remember { mutableStateOf(initial.grayStrength) }
        var grayAnimate by remember { mutableStateOf(initial.grayAnimate) }
        var startAnim by remember { mutableStateOf(initial.startAnim) }
        var endAnim by remember { mutableStateOf(initial.endAnim) }
        var startDuration by remember { mutableStateOf(initial.startDuration) }
        var endDuration by remember { mutableStateOf(initial.endDuration) }
        var ballColor by remember { mutableStateOf(initial.ballColor) }
        var ballColorCustom by remember { mutableStateOf(initial.ballColorCustom) }
        var customColor by remember { mutableStateOf(initial.customColor) }
        var particleSize by remember { mutableStateOf(initial.particleSize) }
        var stopDuration by remember { mutableStateOf(initial.stopDuration) }
        var triggerMode by remember { mutableStateOf(initial.triggerMode) }
        var soundEnabled by remember { mutableStateOf(initial.soundEnabled) }
        var cooldown by remember { mutableStateOf(initial.cooldown) }
        var soundVolume by remember { mutableStateOf(initial.soundVolume) }
        var particleAlpha by remember { mutableStateOf(initial.particleAlpha) }
        var particleCount by remember { mutableStateOf(initial.particleCount) }
        var particleSpin by remember { mutableStateOf(initial.particleSpin) }
        var freezeSelf by remember { mutableStateOf(initial.freezeSelf) }
        var stopRadius by remember { mutableStateOf(initial.stopRadius) }
        var freezeEntities by remember { mutableStateOf(initial.freezeEntities) }
        var freezeBlocks by remember { mutableStateOf(initial.freezeBlocks) }
        var freezeFluids by remember { mutableStateOf(initial.freezeFluids) }
        var freezeBossAI by remember { mutableStateOf(initial.freezeBossAI) }
        var showMessage by remember { mutableStateOf(initial.showMessage) }
        var grayStyle by remember { mutableStateOf(initial.grayStyle) }
        var slashEnabled by remember { mutableStateOf(initial.slashEnabled) }
        var slashDepthTest by remember { mutableStateOf(initial.slashDepthTest) }
        var slashRandomAngle by remember { mutableStateOf(initial.slashRandomAngle) }
        var slashGlow by remember { mutableStateOf(initial.slashGlow) }
        var slashDuration by remember { mutableStateOf(initial.slashDuration) }
        var slashLengthMult by remember { mutableStateOf(initial.slashLengthMult) }
        var slashWidthRatio by remember { mutableStateOf(initial.slashWidthRatio) }
        var slashThicknessRatio by remember { mutableStateOf(initial.slashThicknessRatio) }
        var slashTipFade by remember { mutableStateOf(initial.slashTipFade) }
        var slashStartScale by remember { mutableStateOf(initial.slashStartScale) }
        var slashEndScale by remember { mutableStateOf(initial.slashEndScale) }
        var slashCoreWidth by remember { mutableStateOf(initial.slashCoreWidth) }
        var slashCoreShade by remember { mutableStateOf(initial.slashCoreShade) }
        var slashEdgeWidth by remember { mutableStateOf(initial.slashEdgeWidth) }
        var slashEdgeBrightness by remember { mutableStateOf(initial.slashEdgeBrightness) }
        var slashGlowWidth by remember { mutableStateOf(initial.slashGlowWidth) }
        var slashGlowStrength by remember { mutableStateOf(initial.slashGlowStrength) }
        var slashNoiseStrength by remember { mutableStateOf(initial.slashNoiseStrength) }
        var slashSweepSpeed by remember { mutableStateOf(initial.slashSweepSpeed) }
        var slashSweepSoftness by remember { mutableStateOf(initial.slashSweepSoftness) }
        var slashHoldFraction by remember { mutableStateOf(initial.slashHoldFraction) }
        var slashFadeStart by remember { mutableStateOf(initial.slashFadeStart) }
        var slashFadeDuration by remember { mutableStateOf(initial.slashFadeDuration) }
        var slashSurfaceOffset by remember { mutableStateOf(initial.slashSurfaceOffset) }
        var slashRollRange by remember { mutableStateOf(initial.slashRollRange) }
        var slashCoreColor by remember { mutableStateOf(initial.slashCoreColor) }
        var slashEdgeColor by remember { mutableStateOf(initial.slashEdgeColor) }
        var slashGlowColor by remember { mutableStateOf(initial.slashGlowColor) }
        var renderStyle by remember { mutableStateOf(initial.renderStyle) }
        var silkColor0 by remember { mutableStateOf(initial.silkColor0) }
        var silkColor1 by remember { mutableStateOf(initial.silkColor1) }
        var silkColor2 by remember { mutableStateOf(initial.silkColor2) }
        var silkColor3 by remember { mutableStateOf(initial.silkColor3) }
        var silkColor4 by remember { mutableStateOf(initial.silkColor4) }
        var silkColor5 by remember { mutableStateOf(initial.silkColor5) }
        var silkColor6 by remember { mutableStateOf(initial.silkColor6) }
        var silkColor7 by remember { mutableStateOf(initial.silkColor7) }
        var silkBrightness by remember { mutableStateOf(initial.silkBrightness) }
        var silkContrast by remember { mutableStateOf(initial.silkContrast) }
        var silkSaturation by remember { mutableStateOf(initial.silkSaturation) }
        var silkScale by remember { mutableStateOf(initial.silkScale) }
        var silkIntensity by remember { mutableStateOf(initial.silkIntensity) }
        var silkWarp by remember { mutableStateOf(initial.silkWarp) }
        var silkDetail by remember { mutableStateOf(initial.silkDetail) }
        var silkHue by remember { mutableStateOf(initial.silkHue) }
        var silkSeed by remember { mutableStateOf(initial.silkSeed) }
        var silkRotation by remember { mutableStateOf(initial.silkRotation) }
        var silkDrift by remember { mutableStateOf(initial.silkDrift) }
        var silkVignette by remember { mutableStateOf(initial.silkVignette) }
        var silkBlur by remember { mutableStateOf(initial.silkBlur) }
        var silkGrain by remember { mutableStateOf(initial.silkGrain) }
        var tunnelSpeed by remember { mutableStateOf(initial.tunnelSpeed) }
        var tunnelBrightness by remember { mutableStateOf(initial.tunnelBrightness) }
        var tunnelDensity by remember { mutableStateOf(initial.tunnelDensity) }
        var tunnelFov by remember { mutableStateOf(initial.tunnelFov) }
        var voronoiColorCount by remember { mutableStateOf(initial.voronoiColorCount) }
        var voronoiStepsPerColor by remember { mutableStateOf(initial.voronoiStepsPerColor) }
        var voronoiColor0 by remember { mutableStateOf(initial.voronoiColor0) }
        var voronoiColor1 by remember { mutableStateOf(initial.voronoiColor1) }
        var voronoiColor2 by remember { mutableStateOf(initial.voronoiColor2) }
        var voronoiColor3 by remember { mutableStateOf(initial.voronoiColor3) }
        var voronoiColor4 by remember { mutableStateOf(initial.voronoiColor4) }
        var voronoiColorGlow by remember { mutableStateOf(initial.voronoiColorGlow) }
        var voronoiColorGap by remember { mutableStateOf(initial.voronoiColorGap) }
        var voronoiDistortion by remember { mutableStateOf(initial.voronoiDistortion) }
        var voronoiGap by remember { mutableStateOf(initial.voronoiGap) }
        var voronoiGlow by remember { mutableStateOf(initial.voronoiGlow) }
        var voronoiScale by remember { mutableStateOf(initial.voronoiScale) }
        var voronoiFov by remember { mutableStateOf(initial.voronoiFov) }
        var voronoiSpeed by remember { mutableStateOf(initial.voronoiSpeed) }
        var voronoiRotation by remember { mutableStateOf(initial.voronoiRotation) }
        var voronoiOffsetX by remember { mutableStateOf(initial.voronoiOffsetX) }
        var voronoiOffsetY by remember { mutableStateOf(initial.voronoiOffsetY) }
        var voronoiPreset by remember { mutableStateOf(0) }
        var slashStarfield by remember { mutableStateOf(initial.slashStarfield) }
        var slashStarDensity by remember { mutableStateOf(initial.slashStarDensity) }
        var slashStarBrightness by remember { mutableStateOf(initial.slashStarBrightness) }
        var slashStarSize by remember { mutableStateOf(initial.slashStarSize) }
        var slashStarColorMode by remember { mutableStateOf(initial.slashStarColorMode) }
        var uiTabVariant by remember { mutableStateOf(initial.uiTabVariant) }
        var uiTabSize by remember { mutableStateOf(initial.uiTabSize) }
        var uiTabCompact by remember { mutableStateOf(initial.uiTabCompact) }
        var uiSliderHeight by remember { mutableStateOf(initial.uiSliderHeight) }
        var uiSliderThumb by remember { mutableStateOf(initial.uiSliderThumb) }
        var uiSearchStyle by remember { mutableStateOf(initial.uiSearchStyle) }
        var uiSearchWidth by remember { mutableStateOf(initial.uiSearchWidth) }
        var uiSearchHeight by remember { mutableStateOf(initial.uiSearchHeight) }
        var uiSearchRadius by remember { mutableStateOf(initial.uiSearchRadius) }
        var uiSwitchStyle by remember { mutableStateOf(initial.uiSwitchStyle) }
        var uiSwitchWidth by remember { mutableStateOf(initial.uiSwitchWidth) }
        var uiSwitchHeight by remember { mutableStateOf(initial.uiSwitchHeight) }
        var uiSwitchKnob by remember { mutableStateOf(initial.uiSwitchKnob) }
        var uiSwitchGradientA by remember { mutableStateOf(initial.uiSwitchGradientA) }
        var uiSwitchGradientB by remember { mutableStateOf(initial.uiSwitchGradientB) }
        var uiSwitchTrackOff by remember { mutableStateOf(initial.uiSwitchTrackOff) }
        var uiSwitchSlideTrack by remember { mutableStateOf(initial.uiSwitchSlideTrack) }
        var uiSwitchSlideOff by remember { mutableStateOf(initial.uiSwitchSlideOff) }
        var uiSwitchSlideOn by remember { mutableStateOf(initial.uiSwitchSlideOn) }
        var uiSwitchTextTrackOff by remember { mutableStateOf(initial.uiSwitchTextTrackOff) }
        var uiSwitchTextTrackOn by remember { mutableStateOf(initial.uiSwitchTextTrackOn) }
        var uiSwitchTextKnob by remember { mutableStateOf(initial.uiSwitchTextKnob) }
        var uiSwitchTextColor by remember { mutableStateOf(initial.uiSwitchTextColor) }
        var uiSwitchTextOn by remember { mutableStateOf(initial.uiSwitchTextOn) }
        var uiSwitchTextOff by remember { mutableStateOf(initial.uiSwitchTextOff) }
        var rewindEnabled by remember { mutableStateOf(initial.rewindEnabled) }
        var rewindWindowSeconds by remember { mutableStateOf(initial.rewindWindowSeconds) }
        var rewindScopeMode by remember { mutableStateOf(initial.rewindScopeMode) }
        var rewindScope by remember { mutableStateOf(initial.rewindScope) }
        var rewindRadius by remember { mutableStateOf(initial.rewindRadius) }
        var rewindCooldownTicks by remember { mutableStateOf(initial.rewindCooldownTicks) }
        var rewindPlaybackMode by remember { mutableStateOf(initial.rewindPlaybackMode) }
        var rewindPlaybackSeconds by remember { mutableStateOf(initial.rewindPlaybackSeconds) }
        var rewindRestoreOrder by remember { mutableStateOf(initial.rewindRestoreOrder) }
        var rewindCameraMode by remember { mutableStateOf(initial.rewindCameraMode) }
        var rewindPositionRewind by remember { mutableStateOf(initial.rewindPositionRewind) }
        var rewindPositionMode by remember { mutableStateOf(initial.rewindPositionMode) }
        var rewindPlayerState by remember { mutableStateOf(initial.rewindPlayerState) }
        var rewindDeathEnabled by remember { mutableStateOf(initial.rewindDeathEnabled) }
        var rewindDeathCooldownTicks by remember { mutableStateOf(initial.rewindDeathCooldownTicks) }
        var rewindDeathMaxRetries by remember { mutableStateOf(initial.rewindDeathMaxRetries) }
        var rewindSafetyCheckpoint by remember { mutableStateOf(initial.rewindSafetyCheckpoint) }
        var rewindHostileCheck by remember { mutableStateOf(initial.rewindHostileCheck) }
        var rewindOtherItemDeduct by remember { mutableStateOf(initial.rewindOtherItemDeduct) }
        var rewindFreezeOthers by remember { mutableStateOf(initial.rewindFreezeOthers) }
        var rewindTimestopStacking by remember { mutableStateOf(initial.rewindTimestopStacking) }
        var rewindBlocks by remember { mutableStateOf(initial.rewindBlocks) }
        var rewindBlockEntities by remember { mutableStateOf(initial.rewindBlockEntities) }
        var rewindEntities by remember { mutableStateOf(initial.rewindEntities) }
        var rewindItems by remember { mutableStateOf(initial.rewindItems) }
        var rewindExperience by remember { mutableStateOf(initial.rewindExperience) }
        var rewindTime by remember { mutableStateOf(initial.rewindTime) }
        var rewindWeather by remember { mutableStateOf(initial.rewindWeather) }
        var rewindRaids by remember { mutableStateOf(initial.rewindRaids) }
        var rewindScoreboard by remember { mutableStateOf(initial.rewindScoreboard) }
        var rewindWorldBorder by remember { mutableStateOf(initial.rewindWorldBorder) }
        var rewindFreeCamRestorePosition by remember { mutableStateOf(initial.rewindFreeCamRestorePosition) }
        var rewindShowStats by remember { mutableStateOf(initial.rewindShowStats) }
        var presetSlot by remember { mutableStateOf(0) }
        var sectionTab by remember { mutableStateOf("基础") }
        var dirty by remember { mutableStateOf(false) }
        val baseline = remember { initial }

        fun toggle(id: String) {
            when (id) {
                "enabled" -> enabled = !enabled
                "invulnerable" -> invulnerable = !invulnerable
                "grayScreen" -> grayScreen = !grayScreen
                "grayAnimate" -> grayAnimate = !grayAnimate
                "soundEnabled" -> soundEnabled = !soundEnabled
                "freezeSelf" -> freezeSelf = !freezeSelf
                "freezeEntities" -> freezeEntities = !freezeEntities
                "freezeBlocks" -> freezeBlocks = !freezeBlocks
                "freezeFluids" -> freezeFluids = !freezeFluids
                "freezeBossAI" -> freezeBossAI = !freezeBossAI
                "slashEnabled" -> slashEnabled = !slashEnabled
                "slashDepthTest" -> slashDepthTest = !slashDepthTest
                "slashRandomAngle" -> slashRandomAngle = !slashRandomAngle
                "slashGlow" -> slashGlow = !slashGlow
                "slashStarfield" -> slashStarfield = !slashStarfield
                "uiTabCompact" -> uiTabCompact = !uiTabCompact
                "rewindEnabled" -> rewindEnabled = !rewindEnabled
                "rewindPositionRewind" -> rewindPositionRewind = !rewindPositionRewind
                "rewindPlayerState" -> rewindPlayerState = !rewindPlayerState
                "rewindDeathEnabled" -> rewindDeathEnabled = !rewindDeathEnabled
                "rewindSafetyCheckpoint" -> rewindSafetyCheckpoint = !rewindSafetyCheckpoint
                "rewindHostileCheck" -> rewindHostileCheck = !rewindHostileCheck
                "rewindOtherItemDeduct" -> rewindOtherItemDeduct = !rewindOtherItemDeduct
                "rewindFreezeOthers" -> rewindFreezeOthers = !rewindFreezeOthers
                "rewindTimestopStacking" -> rewindTimestopStacking = !rewindTimestopStacking
                "rewindBlocks" -> rewindBlocks = !rewindBlocks
                "rewindBlockEntities" -> rewindBlockEntities = !rewindBlockEntities
                "rewindEntities" -> rewindEntities = !rewindEntities
                "rewindItems" -> rewindItems = !rewindItems
                "rewindExperience" -> rewindExperience = !rewindExperience
                "rewindTime" -> rewindTime = !rewindTime
                "rewindWeather" -> rewindWeather = !rewindWeather
                "rewindRaids" -> rewindRaids = !rewindRaids
                "rewindScoreboard" -> rewindScoreboard = !rewindScoreboard
                "rewindWorldBorder" -> rewindWorldBorder = !rewindWorldBorder
                "rewindFreeCamRestorePosition" -> rewindFreeCamRestorePosition = !rewindFreeCamRestorePosition
                "rewindShowStats" -> rewindShowStats = !rewindShowStats
                else -> showMessage = !showMessage
            }
            dirty = true
        }

        fun setSegment(id: String, v: Int) {
            if (id == "startAnim") startAnim = v
            else if (id == "endAnim") endAnim = v
            else if (id == "triggerMode") triggerMode = v
            else if (id == "renderStyle") renderStyle = renderStyleStored(v)
            else if (id == "slashStarColorMode") slashStarColorMode = v
            else if (id == "uiTabVariant") uiTabVariant = v
            else if (id == "uiTabSize") uiTabSize = v
            else if (id == "uiSliderHeight") uiSliderHeight = v
            else if (id == "uiSliderThumb") uiSliderThumb = v
            else if (id == "uiSearchStyle") uiSearchStyle = v
            else if (id == "uiSwitchStyle") uiSwitchStyle = v
            else if (id == "voronoiPreset") voronoiPreset = v
            else if (id == "voronoiColorCount") voronoiColorCount = v
            else if (id == "voronoiStepsPerColor") voronoiStepsPerColor = v
            else if (id == "rewindScopeMode") rewindScopeMode = v
            else if (id == "rewindScope") rewindScope = v
            else if (id == "rewindPlaybackMode") rewindPlaybackMode = v
            else if (id == "rewindRestoreOrder") rewindRestoreOrder = v
            else if (id == "rewindCameraMode") rewindCameraMode = v
            else if (id == "rewindPositionMode") rewindPositionMode = v
            else grayStyle = v
            dirty = true
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
                0 -> voronoiColor0 = patch(voronoiColor0)
                1 -> voronoiColor1 = patch(voronoiColor1)
                2 -> voronoiColor2 = patch(voronoiColor2)
                3 -> voronoiColor3 = patch(voronoiColor3)
                4 -> voronoiColor4 = patch(voronoiColor4)
                5 -> voronoiColorGlow = patch(voronoiColorGlow)
                else -> voronoiColorGap = patch(voronoiColorGap)
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
                0 -> silkColor0 = patch(silkColor0)
                1 -> silkColor1 = patch(silkColor1)
                2 -> silkColor2 = patch(silkColor2)
                3 -> silkColor3 = patch(silkColor3)
                4 -> silkColor4 = patch(silkColor4)
                5 -> silkColor5 = patch(silkColor5)
                6 -> silkColor6 = patch(silkColor6)
                else -> silkColor7 = patch(silkColor7)
            }
        }

        fun setSlider(id: String, v: Float) {
            when (id) {
                "startDuration" -> startDuration = v
                "endDuration" -> endDuration = v
                "grayStrength" -> grayStrength = v
                "particleSize" -> particleSize = v
                "stopDuration" -> stopDuration = v
                "cooldown" -> cooldown = v.toInt()
                "soundVolume" -> soundVolume = v
                "particleAlpha" -> particleAlpha = v
                "particleCount" -> particleCount = v.toInt()
                "particleSpin" -> particleSpin = v
                "stopRadius" -> stopRadius = v
                "slashDuration" -> slashDuration = v
                "slashLengthMult" -> slashLengthMult = v
                "slashWidthRatio" -> slashWidthRatio = v
                "slashThicknessRatio" -> slashThicknessRatio = v
                "slashTipFade" -> slashTipFade = v
                "slashStartScale" -> slashStartScale = v
                "slashEndScale" -> slashEndScale = v
                "slashCoreWidth" -> slashCoreWidth = v
                "slashCoreShade" -> slashCoreShade = v
                "slashEdgeWidth" -> slashEdgeWidth = v
                "slashEdgeBrightness" -> slashEdgeBrightness = v
                "slashGlowWidth" -> slashGlowWidth = v
                "slashGlowStrength" -> slashGlowStrength = v
                "slashNoiseStrength" -> slashNoiseStrength = v
                "slashSweepSpeed" -> slashSweepSpeed = v
                "slashSweepSoftness" -> slashSweepSoftness = v
                "slashHoldFraction" -> slashHoldFraction = v
                "slashFadeStart" -> slashFadeStart = v
                "slashFadeDuration" -> slashFadeDuration = v
                "slashSurfaceOffset" -> slashSurfaceOffset = v
                "slashRollRange" -> slashRollRange = v
                "silkBrightness" -> silkBrightness = v
                "silkContrast" -> silkContrast = v
                "silkSaturation" -> silkSaturation = v
                "silkScale" -> silkScale = v
                "silkIntensity" -> silkIntensity = v
                "silkWarp" -> silkWarp = v
                "silkDetail" -> silkDetail = v
                "silkHue" -> silkHue = v
                "silkSeed" -> silkSeed = v
                "silkRotation" -> silkRotation = v
                "silkDrift" -> silkDrift = v
                "silkVignette" -> silkVignette = v
                "silkBlur" -> silkBlur = v
                "silkGrain" -> silkGrain = v
                "tunnelSpeed" -> tunnelSpeed = v
                "tunnelBrightness" -> tunnelBrightness = v
                "tunnelDensity" -> tunnelDensity = v
                "tunnelFov" -> tunnelFov = v
                "voronoiDistortion" -> voronoiDistortion = v
                "voronoiGap" -> voronoiGap = v
                "voronoiGlow" -> voronoiGlow = v
                "voronoiScale" -> voronoiScale = v
                "voronoiFov" -> voronoiFov = v
                "voronoiSpeed" -> voronoiSpeed = v
                "voronoiRotation" -> voronoiRotation = v
                "voronoiOffsetX" -> voronoiOffsetX = v
                "voronoiOffsetY" -> voronoiOffsetY = v
                "slashStarDensity" -> slashStarDensity = v
                "slashStarBrightness" -> slashStarBrightness = v
                "slashStarSize" -> slashStarSize = v
                "slashCoreColorR" -> slashCoreColor = (slashCoreColor and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "slashCoreColorG" -> slashCoreColor = (slashCoreColor and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "slashCoreColorB" -> slashCoreColor = (slashCoreColor and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "slashEdgeColorR" -> slashEdgeColor = (slashEdgeColor and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "slashEdgeColorG" -> slashEdgeColor = (slashEdgeColor and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "slashEdgeColorB" -> slashEdgeColor = (slashEdgeColor and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "slashGlowColorR" -> slashGlowColor = (slashGlowColor and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "slashGlowColorG" -> slashGlowColor = (slashGlowColor and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "slashGlowColorB" -> slashGlowColor = (slashGlowColor and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "rewindWindowSeconds" -> rewindWindowSeconds = v.toInt()
                "rewindRadius" -> rewindRadius = v
                "rewindCooldownTicks" -> rewindCooldownTicks = v.toInt()
                "rewindPlaybackSeconds" -> rewindPlaybackSeconds = v
                "rewindDeathCooldownTicks" -> rewindDeathCooldownTicks = v.toInt()
                "rewindDeathMaxRetries" -> rewindDeathMaxRetries = v.toInt()
                "uiSearchWidth" -> uiSearchWidth = v
                "uiSearchHeight" -> uiSearchHeight = v
                "uiSearchRadius" -> uiSearchRadius = v
                "uiSwitchWidth" -> uiSwitchWidth = v
                "uiSwitchHeight" -> uiSwitchHeight = v
                "uiSwitchKnob" -> uiSwitchKnob = v
                "uiSwitchGradientAR" -> uiSwitchGradientA = (uiSwitchGradientA and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchGradientAG" -> uiSwitchGradientA = (uiSwitchGradientA and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchGradientAB" -> uiSwitchGradientA = (uiSwitchGradientA and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchGradientBR" -> uiSwitchGradientB = (uiSwitchGradientB and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchGradientBG" -> uiSwitchGradientB = (uiSwitchGradientB and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchGradientBB" -> uiSwitchGradientB = (uiSwitchGradientB and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTrackOffR" -> uiSwitchTrackOff = (uiSwitchTrackOff and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTrackOffG" -> uiSwitchTrackOff = (uiSwitchTrackOff and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTrackOffB" -> uiSwitchTrackOff = (uiSwitchTrackOff and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchSlideTrackR" -> uiSwitchSlideTrack = (uiSwitchSlideTrack and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchSlideTrackG" -> uiSwitchSlideTrack = (uiSwitchSlideTrack and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchSlideTrackB" -> uiSwitchSlideTrack = (uiSwitchSlideTrack and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchSlideOffR" -> uiSwitchSlideOff = (uiSwitchSlideOff and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchSlideOffG" -> uiSwitchSlideOff = (uiSwitchSlideOff and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchSlideOffB" -> uiSwitchSlideOff = (uiSwitchSlideOff and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchSlideOnR" -> uiSwitchSlideOn = (uiSwitchSlideOn and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchSlideOnG" -> uiSwitchSlideOn = (uiSwitchSlideOn and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchSlideOnB" -> uiSwitchSlideOn = (uiSwitchSlideOn and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTextTrackOffR" -> uiSwitchTextTrackOff = (uiSwitchTextTrackOff and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTextTrackOffG" -> uiSwitchTextTrackOff = (uiSwitchTextTrackOff and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTextTrackOffB" -> uiSwitchTextTrackOff = (uiSwitchTextTrackOff and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTextTrackOnR" -> uiSwitchTextTrackOn = (uiSwitchTextTrackOn and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTextTrackOnG" -> uiSwitchTextTrackOn = (uiSwitchTextTrackOn and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTextTrackOnB" -> uiSwitchTextTrackOn = (uiSwitchTextTrackOn and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTextKnobR" -> uiSwitchTextKnob = (uiSwitchTextKnob and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTextKnobG" -> uiSwitchTextKnob = (uiSwitchTextKnob and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTextKnobB" -> uiSwitchTextKnob = (uiSwitchTextKnob and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
                "uiSwitchTextColorR" -> uiSwitchTextColor = (uiSwitchTextColor and 0xFF00FFFF.toInt()) or ((v.toInt() and 0xFF) shl 16)
                "uiSwitchTextColorG" -> uiSwitchTextColor = (uiSwitchTextColor and 0xFFFF00FF.toInt()) or ((v.toInt() and 0xFF) shl 8)
                "uiSwitchTextColorB" -> uiSwitchTextColor = (uiSwitchTextColor and 0xFFFFFF00.toInt()) or (v.toInt() and 0xFF)
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
            dirty = true
        }

        fun setText(id: String, v: String) {
            if (id == "uiSwitchTextOn") uiSwitchTextOn = v
            else if (id == "uiSwitchTextOff") uiSwitchTextOff = v
            dirty = true
        }

        fun setBall(v: Int) {
            ballColor = v
            ballColorCustom = false
            dirty = true
        }

        fun setCustomColor(v: Int) {
            customColor = v
            ballColorCustom = true
            dirty = true
        }

        fun currentState(): V9State = V9State(
            enabled = enabled,
            invulnerable = invulnerable,
            grayScreen = grayScreen,
            grayStrength = grayStrength,
            grayAnimate = grayAnimate,
            startAnim = startAnim,
            endAnim = endAnim,
            startDuration = startDuration,
            endDuration = endDuration,
            ballColor = ballColor,
            ballColorCustom = ballColorCustom,
            customColor = customColor,
            particleSize = particleSize,
            stopDuration = stopDuration,
            triggerMode = triggerMode,
            soundEnabled = soundEnabled,
            cooldown = cooldown,
            soundVolume = soundVolume,
            particleAlpha = particleAlpha,
            particleCount = particleCount,
            particleSpin = particleSpin,
            freezeSelf = freezeSelf,
            stopRadius = stopRadius,
            freezeEntities = freezeEntities,
            freezeBlocks = freezeBlocks,
            freezeFluids = freezeFluids,
            freezeBossAI = freezeBossAI,
            showMessage = showMessage,
            grayStyle = grayStyle,
            slashEnabled = slashEnabled,
            slashDepthTest = slashDepthTest,
            slashRandomAngle = slashRandomAngle,
            slashGlow = slashGlow,
            slashDuration = slashDuration,
            slashLengthMult = slashLengthMult,
            slashWidthRatio = slashWidthRatio,
            slashThicknessRatio = slashThicknessRatio,
            slashTipFade = slashTipFade,
            slashStartScale = slashStartScale,
            slashEndScale = slashEndScale,
            slashCoreWidth = slashCoreWidth,
            slashCoreShade = slashCoreShade,
            slashEdgeWidth = slashEdgeWidth,
            slashEdgeBrightness = slashEdgeBrightness,
            slashGlowWidth = slashGlowWidth,
            slashGlowStrength = slashGlowStrength,
            slashNoiseStrength = slashNoiseStrength,
            slashSweepSpeed = slashSweepSpeed,
            slashSweepSoftness = slashSweepSoftness,
            slashHoldFraction = slashHoldFraction,
            slashFadeStart = slashFadeStart,
            slashFadeDuration = slashFadeDuration,
            slashSurfaceOffset = slashSurfaceOffset,
            slashRollRange = slashRollRange,
            slashCoreColor = slashCoreColor,
            slashEdgeColor = slashEdgeColor,
            slashGlowColor = slashGlowColor,
            renderStyle = renderStyle,
            silkColor0 = silkColor0,
            silkColor1 = silkColor1,
            silkColor2 = silkColor2,
            silkColor3 = silkColor3,
            silkColor4 = silkColor4,
            silkColor5 = silkColor5,
            silkColor6 = silkColor6,
            silkColor7 = silkColor7,
            silkBrightness = silkBrightness,
            silkContrast = silkContrast,
            silkSaturation = silkSaturation,
            silkScale = silkScale,
            silkIntensity = silkIntensity,
            silkWarp = silkWarp,
            silkDetail = silkDetail,
            silkHue = silkHue,
            silkSeed = silkSeed,
            silkRotation = silkRotation,
            silkDrift = silkDrift,
            silkVignette = silkVignette,
            silkBlur = silkBlur,
            silkGrain = silkGrain,
            tunnelSpeed = tunnelSpeed,
            tunnelBrightness = tunnelBrightness,
            tunnelDensity = tunnelDensity,
            tunnelFov = tunnelFov,
            voronoiColorCount = voronoiColorCount,
            voronoiStepsPerColor = voronoiStepsPerColor,
            voronoiColor0 = voronoiColor0,
            voronoiColor1 = voronoiColor1,
            voronoiColor2 = voronoiColor2,
            voronoiColor3 = voronoiColor3,
            voronoiColor4 = voronoiColor4,
            voronoiColorGlow = voronoiColorGlow,
            voronoiColorGap = voronoiColorGap,
            voronoiDistortion = voronoiDistortion,
            voronoiGap = voronoiGap,
            voronoiGlow = voronoiGlow,
            voronoiScale = voronoiScale,
            voronoiFov = voronoiFov,
            voronoiSpeed = voronoiSpeed,
            voronoiRotation = voronoiRotation,
            voronoiOffsetX = voronoiOffsetX,
            voronoiOffsetY = voronoiOffsetY,
            slashStarfield = slashStarfield,
            slashStarDensity = slashStarDensity,
            slashStarBrightness = slashStarBrightness,
            slashStarSize = slashStarSize,
            slashStarColorMode = slashStarColorMode,
            uiTabVariant = uiTabVariant,
            uiTabSize = uiTabSize,
            uiTabCompact = uiTabCompact,
            uiSliderHeight = uiSliderHeight,
            uiSliderThumb = uiSliderThumb,
            uiSearchStyle = uiSearchStyle,
            uiSearchWidth = uiSearchWidth,
            uiSearchHeight = uiSearchHeight,
            uiSearchRadius = uiSearchRadius,
            uiSwitchStyle = uiSwitchStyle,
            uiSwitchWidth = uiSwitchWidth,
            uiSwitchHeight = uiSwitchHeight,
            uiSwitchKnob = uiSwitchKnob,
            uiSwitchGradientA = uiSwitchGradientA,
            uiSwitchGradientB = uiSwitchGradientB,
            uiSwitchTrackOff = uiSwitchTrackOff,
            uiSwitchSlideTrack = uiSwitchSlideTrack,
            uiSwitchSlideOff = uiSwitchSlideOff,
            uiSwitchSlideOn = uiSwitchSlideOn,
            uiSwitchTextTrackOff = uiSwitchTextTrackOff,
            uiSwitchTextTrackOn = uiSwitchTextTrackOn,
            uiSwitchTextKnob = uiSwitchTextKnob,
            uiSwitchTextColor = uiSwitchTextColor,
            uiSwitchTextOn = uiSwitchTextOn,
            uiSwitchTextOff = uiSwitchTextOff,
            rewindEnabled = rewindEnabled,
            rewindWindowSeconds = rewindWindowSeconds,
            rewindScopeMode = rewindScopeMode,
            rewindScope = rewindScope,
            rewindRadius = rewindRadius,
            rewindCooldownTicks = rewindCooldownTicks,
            rewindPlaybackMode = rewindPlaybackMode,
            rewindPlaybackSeconds = rewindPlaybackSeconds,
            rewindRestoreOrder = rewindRestoreOrder,
            rewindCameraMode = rewindCameraMode,
            rewindPositionRewind = rewindPositionRewind,
            rewindPositionMode = rewindPositionMode,
            rewindPlayerState = rewindPlayerState,
            rewindDeathEnabled = rewindDeathEnabled,
            rewindDeathCooldownTicks = rewindDeathCooldownTicks,
            rewindDeathMaxRetries = rewindDeathMaxRetries,
            rewindSafetyCheckpoint = rewindSafetyCheckpoint,
            rewindHostileCheck = rewindHostileCheck,
            rewindOtherItemDeduct = rewindOtherItemDeduct,
            rewindFreezeOthers = rewindFreezeOthers,
            rewindTimestopStacking = rewindTimestopStacking,
            rewindBlocks = rewindBlocks,
            rewindBlockEntities = rewindBlockEntities,
            rewindEntities = rewindEntities,
            rewindItems = rewindItems,
            rewindExperience = rewindExperience,
            rewindTime = rewindTime,
            rewindWeather = rewindWeather,
            rewindRaids = rewindRaids,
            rewindScoreboard = rewindScoreboard,
            rewindWorldBorder = rewindWorldBorder,
            rewindFreeCamRestorePosition = rewindFreeCamRestorePosition,
            rewindShowStats = rewindShowStats
        )

        fun savePreset() {
            onPresetSave(presetSlot, currentState())
        }

        fun apply(s: V9State) {
            enabled = s.enabled
            invulnerable = s.invulnerable
            grayScreen = s.grayScreen
            grayStrength = s.grayStrength
            grayAnimate = s.grayAnimate
            startAnim = s.startAnim
            endAnim = s.endAnim
            startDuration = s.startDuration
            endDuration = s.endDuration
            ballColor = s.ballColor
            ballColorCustom = s.ballColorCustom
            customColor = s.customColor
            particleSize = s.particleSize
            stopDuration = s.stopDuration
            triggerMode = s.triggerMode
            soundEnabled = s.soundEnabled
            cooldown = s.cooldown
            soundVolume = s.soundVolume
            particleAlpha = s.particleAlpha
            particleCount = s.particleCount
            particleSpin = s.particleSpin
            freezeSelf = s.freezeSelf
            stopRadius = s.stopRadius
            freezeEntities = s.freezeEntities
            freezeBlocks = s.freezeBlocks
            freezeFluids = s.freezeFluids
            freezeBossAI = s.freezeBossAI
            showMessage = s.showMessage
            grayStyle = s.grayStyle
            slashEnabled = s.slashEnabled
            slashDepthTest = s.slashDepthTest
            slashRandomAngle = s.slashRandomAngle
            slashGlow = s.slashGlow
            slashDuration = s.slashDuration
            slashLengthMult = s.slashLengthMult
            slashWidthRatio = s.slashWidthRatio
            slashThicknessRatio = s.slashThicknessRatio
            slashTipFade = s.slashTipFade
            slashStartScale = s.slashStartScale
            slashEndScale = s.slashEndScale
            slashCoreWidth = s.slashCoreWidth
            slashCoreShade = s.slashCoreShade
            slashEdgeWidth = s.slashEdgeWidth
            slashEdgeBrightness = s.slashEdgeBrightness
            slashGlowWidth = s.slashGlowWidth
            slashGlowStrength = s.slashGlowStrength
            slashNoiseStrength = s.slashNoiseStrength
            slashSweepSpeed = s.slashSweepSpeed
            slashSweepSoftness = s.slashSweepSoftness
            slashHoldFraction = s.slashHoldFraction
            slashFadeStart = s.slashFadeStart
            slashFadeDuration = s.slashFadeDuration
            slashSurfaceOffset = s.slashSurfaceOffset
            slashRollRange = s.slashRollRange
            slashCoreColor = s.slashCoreColor
            slashEdgeColor = s.slashEdgeColor
            slashGlowColor = s.slashGlowColor
            renderStyle = s.renderStyle
            silkColor0 = s.silkColor0
            silkColor1 = s.silkColor1
            silkColor2 = s.silkColor2
            silkColor3 = s.silkColor3
            silkColor4 = s.silkColor4
            silkColor5 = s.silkColor5
            silkColor6 = s.silkColor6
            silkColor7 = s.silkColor7
            silkBrightness = s.silkBrightness
            silkContrast = s.silkContrast
            silkSaturation = s.silkSaturation
            silkScale = s.silkScale
            silkIntensity = s.silkIntensity
            silkWarp = s.silkWarp
            silkDetail = s.silkDetail
            silkHue = s.silkHue
            silkSeed = s.silkSeed
            silkRotation = s.silkRotation
            silkDrift = s.silkDrift
            silkVignette = s.silkVignette
            silkBlur = s.silkBlur
            silkGrain = s.silkGrain
            tunnelSpeed = s.tunnelSpeed
            tunnelBrightness = s.tunnelBrightness
            tunnelDensity = s.tunnelDensity
            tunnelFov = s.tunnelFov
            voronoiColorCount = s.voronoiColorCount
            voronoiStepsPerColor = s.voronoiStepsPerColor
            voronoiColor0 = s.voronoiColor0
            voronoiColor1 = s.voronoiColor1
            voronoiColor2 = s.voronoiColor2
            voronoiColor3 = s.voronoiColor3
            voronoiColor4 = s.voronoiColor4
            voronoiColorGlow = s.voronoiColorGlow
            voronoiColorGap = s.voronoiColorGap
            voronoiDistortion = s.voronoiDistortion
            voronoiGap = s.voronoiGap
            voronoiGlow = s.voronoiGlow
            voronoiScale = s.voronoiScale
            voronoiFov = s.voronoiFov
            voronoiSpeed = s.voronoiSpeed
            voronoiRotation = s.voronoiRotation
            voronoiOffsetX = s.voronoiOffsetX
            voronoiOffsetY = s.voronoiOffsetY
            slashStarfield = s.slashStarfield
            slashStarDensity = s.slashStarDensity
            slashStarBrightness = s.slashStarBrightness
            slashStarSize = s.slashStarSize
            slashStarColorMode = s.slashStarColorMode
            uiTabVariant = s.uiTabVariant
            uiTabSize = s.uiTabSize
            uiTabCompact = s.uiTabCompact
            uiSliderHeight = s.uiSliderHeight
            uiSliderThumb = s.uiSliderThumb
            uiSearchStyle = s.uiSearchStyle
            uiSearchWidth = s.uiSearchWidth
            uiSearchHeight = s.uiSearchHeight
            uiSearchRadius = s.uiSearchRadius
            uiSwitchStyle = s.uiSwitchStyle
            uiSwitchWidth = s.uiSwitchWidth
            uiSwitchHeight = s.uiSwitchHeight
            uiSwitchKnob = s.uiSwitchKnob
            uiSwitchGradientA = s.uiSwitchGradientA
            uiSwitchGradientB = s.uiSwitchGradientB
            uiSwitchTrackOff = s.uiSwitchTrackOff
            uiSwitchSlideTrack = s.uiSwitchSlideTrack
            uiSwitchSlideOff = s.uiSwitchSlideOff
            uiSwitchSlideOn = s.uiSwitchSlideOn
            uiSwitchTextTrackOff = s.uiSwitchTextTrackOff
            uiSwitchTextTrackOn = s.uiSwitchTextTrackOn
            uiSwitchTextKnob = s.uiSwitchTextKnob
            uiSwitchTextColor = s.uiSwitchTextColor
            uiSwitchTextOn = s.uiSwitchTextOn
            uiSwitchTextOff = s.uiSwitchTextOff
            rewindEnabled = s.rewindEnabled
            rewindWindowSeconds = s.rewindWindowSeconds
            rewindScopeMode = s.rewindScopeMode
            rewindScope = s.rewindScope
            rewindRadius = s.rewindRadius
            rewindCooldownTicks = s.rewindCooldownTicks
            rewindPlaybackMode = s.rewindPlaybackMode
            rewindPlaybackSeconds = s.rewindPlaybackSeconds
            rewindRestoreOrder = s.rewindRestoreOrder
            rewindCameraMode = s.rewindCameraMode
            rewindPositionRewind = s.rewindPositionRewind
            rewindPositionMode = s.rewindPositionMode
            rewindPlayerState = s.rewindPlayerState
            rewindDeathEnabled = s.rewindDeathEnabled
            rewindDeathCooldownTicks = s.rewindDeathCooldownTicks
            rewindDeathMaxRetries = s.rewindDeathMaxRetries
            rewindSafetyCheckpoint = s.rewindSafetyCheckpoint
            rewindHostileCheck = s.rewindHostileCheck
            rewindOtherItemDeduct = s.rewindOtherItemDeduct
            rewindFreezeOthers = s.rewindFreezeOthers
            rewindTimestopStacking = s.rewindTimestopStacking
            rewindBlocks = s.rewindBlocks
            rewindBlockEntities = s.rewindBlockEntities
            rewindEntities = s.rewindEntities
            rewindItems = s.rewindItems
            rewindExperience = s.rewindExperience
            rewindTime = s.rewindTime
            rewindWeather = s.rewindWeather
            rewindRaids = s.rewindRaids
            rewindScoreboard = s.rewindScoreboard
            rewindWorldBorder = s.rewindWorldBorder
            rewindFreeCamRestorePosition = s.rewindFreeCamRestorePosition
            rewindShowStats = s.rewindShowStats
        }

        fun loadPreset(slot: Int) {
            onPresetLoad(slot)?.let {
                presetSlot = slot
                apply(it)
                dirty = true
            }
        }

        fun cancel() {
            apply(baseline)
            dirty = false
            Minecraft.getInstance().setScreen(null)
        }

        fun resetDefaults() {
            apply(defaults)
            dirty = true
        }

        fun resetWeaponDefaults() {
            renderStyle = defaults.renderStyle
            silkColor0 = defaults.silkColor0
            silkColor1 = defaults.silkColor1
            silkColor2 = defaults.silkColor2
            silkColor3 = defaults.silkColor3
            silkColor4 = defaults.silkColor4
            silkColor5 = defaults.silkColor5
            silkColor6 = defaults.silkColor6
            silkColor7 = defaults.silkColor7
            silkBrightness = defaults.silkBrightness
            silkContrast = defaults.silkContrast
            silkSaturation = defaults.silkSaturation
            silkScale = defaults.silkScale
            silkIntensity = defaults.silkIntensity
            silkWarp = defaults.silkWarp
            silkDetail = defaults.silkDetail
            silkHue = defaults.silkHue
            silkSeed = defaults.silkSeed
            silkRotation = defaults.silkRotation
            silkDrift = defaults.silkDrift
            silkVignette = defaults.silkVignette
            silkBlur = defaults.silkBlur
            silkGrain = defaults.silkGrain
            tunnelSpeed = defaults.tunnelSpeed
            tunnelBrightness = defaults.tunnelBrightness
            tunnelDensity = defaults.tunnelDensity
            tunnelFov = defaults.tunnelFov
            voronoiColorCount = defaults.voronoiColorCount
            voronoiStepsPerColor = defaults.voronoiStepsPerColor
            voronoiColor0 = defaults.voronoiColor0
            voronoiColor1 = defaults.voronoiColor1
            voronoiColor2 = defaults.voronoiColor2
            voronoiColor3 = defaults.voronoiColor3
            voronoiColor4 = defaults.voronoiColor4
            voronoiColorGlow = defaults.voronoiColorGlow
            voronoiColorGap = defaults.voronoiColorGap
            voronoiDistortion = defaults.voronoiDistortion
            voronoiGap = defaults.voronoiGap
            voronoiGlow = defaults.voronoiGlow
            voronoiScale = defaults.voronoiScale
            voronoiFov = defaults.voronoiFov
            voronoiSpeed = defaults.voronoiSpeed
            voronoiRotation = defaults.voronoiRotation
            voronoiOffsetX = defaults.voronoiOffsetX
            voronoiOffsetY = defaults.voronoiOffsetY
            dirty = true
        }

        fun resetSilkDefaults() {
            silkColor0 = defaults.silkColor0
            silkColor1 = defaults.silkColor1
            silkColor2 = defaults.silkColor2
            silkColor3 = defaults.silkColor3
            silkColor4 = defaults.silkColor4
            silkColor5 = defaults.silkColor5
            silkColor6 = defaults.silkColor6
            silkColor7 = defaults.silkColor7
            silkBrightness = defaults.silkBrightness
            silkContrast = defaults.silkContrast
            silkSaturation = defaults.silkSaturation
            silkScale = defaults.silkScale
            silkIntensity = defaults.silkIntensity
            silkWarp = defaults.silkWarp
            silkDetail = defaults.silkDetail
            silkHue = defaults.silkHue
            silkSeed = defaults.silkSeed
            silkRotation = defaults.silkRotation
            silkDrift = defaults.silkDrift
            silkVignette = defaults.silkVignette
            silkBlur = defaults.silkBlur
            silkGrain = defaults.silkGrain
            dirty = true
        }

        fun resetTunnelDefaults() {
            tunnelSpeed = defaults.tunnelSpeed
            tunnelBrightness = defaults.tunnelBrightness
            tunnelDensity = defaults.tunnelDensity
            tunnelFov = defaults.tunnelFov
            dirty = true
        }

        fun resetVoronoiDefaults() {
            voronoiColorCount = defaults.voronoiColorCount
            voronoiStepsPerColor = defaults.voronoiStepsPerColor
            voronoiColor0 = defaults.voronoiColor0
            voronoiColor1 = defaults.voronoiColor1
            voronoiColor2 = defaults.voronoiColor2
            voronoiColor3 = defaults.voronoiColor3
            voronoiColor4 = defaults.voronoiColor4
            voronoiColorGlow = defaults.voronoiColorGlow
            voronoiColorGap = defaults.voronoiColorGap
            voronoiDistortion = defaults.voronoiDistortion
            voronoiGap = defaults.voronoiGap
            voronoiGlow = defaults.voronoiGlow
            voronoiScale = defaults.voronoiScale
            voronoiFov = defaults.voronoiFov
            voronoiSpeed = defaults.voronoiSpeed
            voronoiRotation = defaults.voronoiRotation
            voronoiOffsetX = defaults.voronoiOffsetX
            voronoiOffsetY = defaults.voronoiOffsetY
            dirty = true
        }

        fun resetSlashDefaults() {
            slashEnabled = defaults.slashEnabled
            slashDepthTest = defaults.slashDepthTest
            slashRandomAngle = defaults.slashRandomAngle
            slashGlow = defaults.slashGlow
            slashDuration = defaults.slashDuration
            slashLengthMult = defaults.slashLengthMult
            slashWidthRatio = defaults.slashWidthRatio
            slashThicknessRatio = defaults.slashThicknessRatio
            slashTipFade = defaults.slashTipFade
            slashStartScale = defaults.slashStartScale
            slashEndScale = defaults.slashEndScale
            slashCoreWidth = defaults.slashCoreWidth
            slashCoreShade = defaults.slashCoreShade
            slashEdgeWidth = defaults.slashEdgeWidth
            slashEdgeBrightness = defaults.slashEdgeBrightness
            slashGlowWidth = defaults.slashGlowWidth
            slashGlowStrength = defaults.slashGlowStrength
            slashNoiseStrength = defaults.slashNoiseStrength
            slashSweepSpeed = defaults.slashSweepSpeed
            slashSweepSoftness = defaults.slashSweepSoftness
            slashHoldFraction = defaults.slashHoldFraction
            slashFadeStart = defaults.slashFadeStart
            slashFadeDuration = defaults.slashFadeDuration
            slashSurfaceOffset = defaults.slashSurfaceOffset
            slashRollRange = defaults.slashRollRange
            slashCoreColor = defaults.slashCoreColor
            slashEdgeColor = defaults.slashEdgeColor
            slashGlowColor = defaults.slashGlowColor
            slashStarfield = defaults.slashStarfield
            slashStarDensity = defaults.slashStarDensity
            slashStarBrightness = defaults.slashStarBrightness
            slashStarSize = defaults.slashStarSize
            slashStarColorMode = defaults.slashStarColorMode
            dirty = true
        }

        fun resetSlashBaseDefaults() {
            slashEnabled = defaults.slashEnabled
            slashDepthTest = defaults.slashDepthTest
            slashRandomAngle = defaults.slashRandomAngle
            slashGlow = defaults.slashGlow
            slashDuration = defaults.slashDuration
            slashLengthMult = defaults.slashLengthMult
            slashWidthRatio = defaults.slashWidthRatio
            slashThicknessRatio = defaults.slashThicknessRatio
            slashTipFade = defaults.slashTipFade
            slashStartScale = defaults.slashStartScale
            slashEndScale = defaults.slashEndScale
            dirty = true
        }

        fun resetSlashVisualDefaults() {
            slashCoreWidth = defaults.slashCoreWidth
            slashCoreShade = defaults.slashCoreShade
            slashEdgeWidth = defaults.slashEdgeWidth
            slashEdgeBrightness = defaults.slashEdgeBrightness
            slashGlowWidth = defaults.slashGlowWidth
            slashGlowStrength = defaults.slashGlowStrength
            slashNoiseStrength = defaults.slashNoiseStrength
            slashCoreColor = defaults.slashCoreColor
            slashEdgeColor = defaults.slashEdgeColor
            slashGlowColor = defaults.slashGlowColor
            dirty = true
        }

        fun resetSlashAnimDefaults() {
            slashSweepSpeed = defaults.slashSweepSpeed
            slashSweepSoftness = defaults.slashSweepSoftness
            slashHoldFraction = defaults.slashHoldFraction
            slashFadeStart = defaults.slashFadeStart
            slashFadeDuration = defaults.slashFadeDuration
            slashSurfaceOffset = defaults.slashSurfaceOffset
            slashRollRange = defaults.slashRollRange
            dirty = true
        }

        fun resetSlashStarDefaults() {
            slashStarfield = defaults.slashStarfield
            slashStarDensity = defaults.slashStarDensity
            slashStarBrightness = defaults.slashStarBrightness
            slashStarSize = defaults.slashStarSize
            slashStarColorMode = defaults.slashStarColorMode
            dirty = true
        }

        fun resetUiTabDefaults() {
            uiTabVariant = defaults.uiTabVariant
            uiTabSize = defaults.uiTabSize
            uiTabCompact = defaults.uiTabCompact
            dirty = true
        }

        fun resetUiSliderDefaults() {
            uiSliderHeight = defaults.uiSliderHeight
            uiSliderThumb = defaults.uiSliderThumb
            dirty = true
        }

        fun resetUiSearchDefaults() {
            uiSearchStyle = defaults.uiSearchStyle
            uiSearchWidth = defaults.uiSearchWidth
            uiSearchHeight = defaults.uiSearchHeight
            uiSearchRadius = defaults.uiSearchRadius
            dirty = true
        }

        fun resetUiSwitchDefaults() {
            uiSwitchStyle = defaults.uiSwitchStyle
            uiSwitchWidth = defaults.uiSwitchWidth
            uiSwitchHeight = defaults.uiSwitchHeight
            uiSwitchKnob = defaults.uiSwitchKnob
            uiSwitchGradientA = defaults.uiSwitchGradientA
            uiSwitchGradientB = defaults.uiSwitchGradientB
            uiSwitchTrackOff = defaults.uiSwitchTrackOff
            uiSwitchSlideTrack = defaults.uiSwitchSlideTrack
            uiSwitchSlideOff = defaults.uiSwitchSlideOff
            uiSwitchSlideOn = defaults.uiSwitchSlideOn
            uiSwitchTextTrackOff = defaults.uiSwitchTextTrackOff
            uiSwitchTextTrackOn = defaults.uiSwitchTextTrackOn
            uiSwitchTextKnob = defaults.uiSwitchTextKnob
            uiSwitchTextColor = defaults.uiSwitchTextColor
            uiSwitchTextOn = defaults.uiSwitchTextOn
            uiSwitchTextOff = defaults.uiSwitchTextOff
            dirty = true
        }

        fun resetTimestopDefaults() {
            enabled = defaults.enabled
            invulnerable = defaults.invulnerable
            grayScreen = defaults.grayScreen
            grayStrength = defaults.grayStrength
            grayAnimate = defaults.grayAnimate
            startAnim = defaults.startAnim
            endAnim = defaults.endAnim
            startDuration = defaults.startDuration
            endDuration = defaults.endDuration
            ballColor = defaults.ballColor
            ballColorCustom = defaults.ballColorCustom
            customColor = defaults.customColor
            particleSize = defaults.particleSize
            stopDuration = defaults.stopDuration
            triggerMode = defaults.triggerMode
            soundEnabled = defaults.soundEnabled
            cooldown = defaults.cooldown
            soundVolume = defaults.soundVolume
            particleAlpha = defaults.particleAlpha
            particleCount = defaults.particleCount
            particleSpin = defaults.particleSpin
            freezeSelf = defaults.freezeSelf
            stopRadius = defaults.stopRadius
            freezeEntities = defaults.freezeEntities
            freezeBlocks = defaults.freezeBlocks
            freezeFluids = defaults.freezeFluids
            freezeBossAI = defaults.freezeBossAI
            showMessage = defaults.showMessage
            grayStyle = defaults.grayStyle
            dirty = true
        }

        fun resetRewindDefaults() {
            rewindEnabled = defaults.rewindEnabled
            rewindWindowSeconds = defaults.rewindWindowSeconds
            rewindScopeMode = defaults.rewindScopeMode
            rewindScope = defaults.rewindScope
            rewindRadius = defaults.rewindRadius
            rewindCooldownTicks = defaults.rewindCooldownTicks
            rewindPlaybackMode = defaults.rewindPlaybackMode
            rewindPlaybackSeconds = defaults.rewindPlaybackSeconds
            rewindRestoreOrder = defaults.rewindRestoreOrder
            rewindCameraMode = defaults.rewindCameraMode
            rewindPositionRewind = defaults.rewindPositionRewind
            rewindPositionMode = defaults.rewindPositionMode
            rewindPlayerState = defaults.rewindPlayerState
            rewindDeathEnabled = defaults.rewindDeathEnabled
            rewindDeathCooldownTicks = defaults.rewindDeathCooldownTicks
            rewindDeathMaxRetries = defaults.rewindDeathMaxRetries
            rewindSafetyCheckpoint = defaults.rewindSafetyCheckpoint
            rewindHostileCheck = defaults.rewindHostileCheck
            rewindOtherItemDeduct = defaults.rewindOtherItemDeduct
            rewindFreezeOthers = defaults.rewindFreezeOthers
            rewindTimestopStacking = defaults.rewindTimestopStacking
            rewindBlocks = defaults.rewindBlocks
            rewindBlockEntities = defaults.rewindBlockEntities
            rewindEntities = defaults.rewindEntities
            rewindItems = defaults.rewindItems
            rewindExperience = defaults.rewindExperience
            rewindTime = defaults.rewindTime
            rewindWeather = defaults.rewindWeather
            rewindRaids = defaults.rewindRaids
            rewindScoreboard = defaults.rewindScoreboard
            rewindWorldBorder = defaults.rewindWorldBorder
            rewindFreeCamRestorePosition = defaults.rewindFreeCamRestorePosition
            rewindShowStats = defaults.rewindShowStats
            dirty = true
        }

        fun save() {
            onSave(currentState())
            dirty = false
        }

        fun quote(): String = quotes[LocalDate.now().dayOfYear % quotes.size]

        val q = search.trim().lowercase()
        val visible = v9Sections.map { sec ->
            sec to sec.rows.filter {
                q.isEmpty() || it.name.lowercase().contains(q) || it.desc.lowercase().contains(q)
            }
        }.filter { it.second.isNotEmpty() && navSections[selectedNav]?.contains(it.first.name) == true }
        val previewColor: Long = if (ballColorCustom) {
            customColor.toLong() and 0xFFFFFFFFL
        } else {
            ballColors[ballColor]
        }
        val uiPrefs = V9UiPrefs(
            sliderHeight = when (uiSliderHeight) { 0 -> 20.dp; 2 -> 28.dp; else -> 24.dp },
            sliderThumb = when (uiSliderThumb) { 0 -> 16.dp; 2 -> 20.dp; else -> 18.dp },
            searchStyle = uiSearchStyle,
            searchWidth = uiSearchWidth.dp,
            searchHeight = uiSearchHeight.dp,
            searchRadius = uiSearchRadius.dp,
            switchStyle = uiSwitchStyle,
            switchWidth = uiSwitchWidth.dp,
            switchHeight = uiSwitchHeight.dp,
            switchKnob = uiSwitchKnob.dp,
            switchGradientA = uiSwitchGradientA,
            switchGradientB = uiSwitchGradientB,
            switchTrackOff = uiSwitchTrackOff,
            switchSlideTrack = uiSwitchSlideTrack,
            switchSlideOff = uiSwitchSlideOff,
            switchSlideOn = uiSwitchSlideOn,
            switchTextTrackOff = uiSwitchTextTrackOff,
            switchTextTrackOn = uiSwitchTextTrackOn,
            switchTextKnob = uiSwitchTextKnob,
            switchTextColor = uiSwitchTextColor,
            switchTextOn = uiSwitchTextOn,
            switchTextOff = uiSwitchTextOff
        )

        CompositionLocalProvider(LocalV9UiPrefs provides uiPrefs) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x990B0D11))) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight
            val winW = if (availableWidth >= 200.dp) (availableWidth - 16.dp).coerceAtMost(840.dp) else availableWidth
            val winH = if (availableHeight >= 200.dp) (availableHeight - 16.dp).coerceAtMost(480.dp) else availableHeight
            Surface(
                modifier = Modifier.align(Alignment.Center).width(winW).height(winH)
                    .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x40000000), spotColor = Color(0x33FF7A5C)),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0B0D11),
                border = BorderStroke(1.dp, Color(0x38E9ECF2))
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
                            enabled, invulnerable, grayScreen, grayStrength, grayAnimate,
                            startAnim, endAnim, startDuration, endDuration,
                            ballColor, ballColorCustom, customColor,
                            particleSize, stopDuration, triggerMode, soundEnabled, cooldown,
                            soundVolume, particleAlpha, particleCount, particleSpin,
                            freezeSelf, stopRadius, freezeEntities, freezeBlocks, freezeFluids,
                            freezeBossAI, showMessage, grayStyle,
                            presetSlot, ::loadPreset, ::savePreset,
                            ::toggle, ::setSegment, ::setText, ::setSlider, ::setBall, ::setCustomColor,
                            currentState(),
                            uiTabVariant, uiTabSize, uiTabCompact,
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
                                grayScreen, startDuration, endDuration, previewColor,
                                onStageBounds
                            )
                        }
                    }
                    V9Footer(quote(), ::resetDefaults, ::cancel, ::save)
                }
            }
            }
        }
    }
}

private val renderStyleUiValues = listOf(0, 1, 4, 5, 2, 3)

private fun renderStyleStored(uiIndex: Int): Int =
    if (uiIndex in renderStyleUiValues.indices) renderStyleUiValues[uiIndex] else 0

private fun renderStyleUiIndex(stored: Int): Int {
    val idx = renderStyleUiValues.indexOf(stored)
    return if (idx >= 0) idx else 0
}

private data class VoronoiPreset(
    val name: String,
    val colorCount: Int,
    val steps: Int,
    val color0: Int,
    val color1: Int,
    val color2: Int,
    val color3: Int,
    val color4: Int,
    val glow: Int,
    val gap: Int,
    val distortion: Float,
    val gapSize: Float,
    val glowStrength: Float,
    val scale: Float,
    val speed: Float
)

private val voronoiPresets = listOf(
    VoronoiPreset("默认", 2, 3, 0xFFFF8247.toInt(), 0xFFFFE53D.toInt(), 0xFFFFE53D.toInt(), 0xFFFFE53D.toInt(), 0xFFFFE53D.toInt(), 0xFFFFFFFF.toInt(), 0xFF2E0000.toInt(), 0.4f, 0.04f, 0f, 0.5f, 0.5f),
    VoronoiPreset("灯光", 3, 2, 0xFFFFFFFC.toInt(), 0xFFBBFF00.toInt(), 0xFF00FFFF.toInt(), 0xFF00FFFF.toInt(), 0xFF00FFFF.toInt(), 0xFFFF00D0.toInt(), 0xFFFF00D0.toInt(), 0.38f, 0f, 1f, 3.3f, 0.5f),
    VoronoiPreset("细胞", 1, 1, 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0.5f, 0.03f, 0.8f, 0.5f, 0.5f),
    VoronoiPreset("气泡", 1, 1, 0xFF83C9FB.toInt(), 0xFF83C9FB.toInt(), 0xFF83C9FB.toInt(), 0xFF83C9FB.toInt(), 0xFF83C9FB.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0.4f, 0f, 1f, 0.75f, 0.5f)
)

private data class V9State(
    val enabled: Boolean = true,
    val invulnerable: Boolean = true,
    val grayScreen: Boolean = true,
    val grayStrength: Float = 1.0f,
    val grayAnimate: Boolean = false,
    val startAnim: Int = 0,
    val endAnim: Int = 0,
    val startDuration: Float = 0.4f,
    val endDuration: Float = 0.3f,
    val ballColor: Int = 0,
    val ballColorCustom: Boolean = false,
    val customColor: Int = 0xFFFF7A5C.toInt(),
    val particleSize: Float = 0.62f,
    val stopDuration: Float = 0.0f,
    val triggerMode: Int = 0,
    val soundEnabled: Boolean = true,
    val cooldown: Int = 30,
    val soundVolume: Float = 1.0f,
    val particleAlpha: Float = 0.5f,
    val particleCount: Int = 1,
    val particleSpin: Float = 0.3f,
    val freezeSelf: Boolean = false,
    val stopRadius: Float = 0.0f,
    val freezeEntities: Boolean = true,
    val freezeBlocks: Boolean = true,
    val freezeFluids: Boolean = true,
    val freezeBossAI: Boolean = true,
    val showMessage: Boolean = false,
    val grayStyle: Int = 0,
    val slashEnabled: Boolean = true,
    val slashDepthTest: Boolean = true,
    val slashRandomAngle: Boolean = true,
    val slashGlow: Boolean = true,
    val slashDuration: Float = 0.26f,
    val slashLengthMult: Float = 2.8f,
    val slashWidthRatio: Float = 0.09f,
    val slashThicknessRatio: Float = 0.02f,
    val slashTipFade: Float = 0.08f,
    val slashStartScale: Float = 1.0f,
    val slashEndScale: Float = 1.0f,
    val slashCoreWidth: Float = 0.62f,
    val slashCoreShade: Float = 1.0f,
    val slashEdgeWidth: Float = 0.80f,
    val slashEdgeBrightness: Float = 1.0f,
    val slashGlowWidth: Float = 0.90f,
    val slashGlowStrength: Float = 1.0f,
    val slashNoiseStrength: Float = 1.0f,
    val slashSweepSpeed: Float = 0.35f,
    val slashSweepSoftness: Float = 0.15f,
    val slashHoldFraction: Float = 0.0f,
    val slashFadeStart: Float = 0.72f,
    val slashFadeDuration: Float = 0.28f,
    val slashSurfaceOffset: Float = 0.06f,
    val slashRollRange: Float = 180f,
    val slashCoreColor: Int = 0xFF161616.toInt(),
    val slashEdgeColor: Int = 0xFFFFFFFF.toInt(),
    val slashGlowColor: Int = 0xFFFFFFFF.toInt(),
    val renderStyle: Int = 0,
    val silkColor0: Int = 0xFF1A1423.toInt(),
    val silkColor1: Int = 0xFFB75D69.toInt(),
    val silkColor2: Int = 0xFFEACDC2.toInt(),
    val silkColor3: Int = 0xFFFFF5EB.toInt(),
    val silkColor4: Int = 0xFFFFF5EB.toInt(),
    val silkColor5: Int = 0xFFFFF5EB.toInt(),
    val silkColor6: Int = 0xFFFFF5EB.toInt(),
    val silkColor7: Int = 0xFFFFF5EB.toInt(),
    val silkBrightness: Float = 0.03f,
    val silkContrast: Float = 1.19f,
    val silkSaturation: Float = 1.34f,
    val silkScale: Float = 1.74f,
    val silkIntensity: Float = 0.69f,
    val silkWarp: Float = 0.07f,
    val silkDetail: Float = 2.66f,
    val silkHue: Float = 0.0f,
    val silkSeed: Float = 9199f,
    val silkRotation: Float = 5.06f,
    val silkDrift: Float = 0.02f,
    val silkVignette: Float = 0.12f,
    val silkBlur: Float = 0.004f,
    val silkGrain: Float = 0.0f,
    val tunnelSpeed: Float = 1.0f,
    val tunnelBrightness: Float = 1.0f,
    val tunnelDensity: Float = 1.0f,
    val tunnelFov: Float = 6.0f,
    val voronoiColorCount: Int = 1,
    val voronoiStepsPerColor: Int = 3,
    val voronoiColor0: Int = 0xFFFF8247.toInt(),
    val voronoiColor1: Int = 0xFFFFE53D.toInt(),
    val voronoiColor2: Int = 0xFFFFE53D.toInt(),
    val voronoiColor3: Int = 0xFFFFE53D.toInt(),
    val voronoiColor4: Int = 0xFFFFE53D.toInt(),
    val voronoiColorGlow: Int = 0xFFFFFFFF.toInt(),
    val voronoiColorGap: Int = 0xFF2E0000.toInt(),
    val voronoiDistortion: Float = 0.4f,
    val voronoiGap: Float = 0.04f,
    val voronoiGlow: Float = 0.0f,
    val voronoiScale: Float = 0.5f,
    val voronoiFov: Float = 6.0f,
    val voronoiSpeed: Float = 0.5f,
    val voronoiRotation: Float = 0.0f,
    val voronoiOffsetX: Float = 0.0f,
    val voronoiOffsetY: Float = 0.0f,
    val slashStarfield: Boolean = false,
    val slashStarDensity: Float = 0.6f,
    val slashStarBrightness: Float = 0.8f,
    val slashStarSize: Float = 1.0f,
    val slashStarColorMode: Int = 0,
    val uiTabVariant: Int = 0,
    val uiTabSize: Int = 1,
    val uiTabCompact: Boolean = false,
    val uiSliderHeight: Int = 1,
    val uiSliderThumb: Int = 1,
    val uiSearchStyle: Int = 0,
    val uiSearchWidth: Float = 200f,
    val uiSearchHeight: Float = 40f,
    val uiSearchRadius: Float = 20f,
    val uiSwitchStyle: Int = 0,
    val uiSwitchWidth: Float = 62f,
    val uiSwitchHeight: Float = 28f,
    val uiSwitchKnob: Float = 22f,
    val uiSwitchGradientA: Int = 0xFFF19AF3.toInt(),
    val uiSwitchGradientB: Int = 0xFFF099B5.toInt(),
    val uiSwitchTrackOff: Int = 0xFFD7D7D7.toInt(),
    val uiSwitchSlideTrack: Int = 0xFFFFFFFF.toInt(),
    val uiSwitchSlideOff: Int = 0xFFCCCCCC.toInt(),
    val uiSwitchSlideOn: Int = 0xFF59D102.toInt(),
    val uiSwitchTextTrackOff: Int = 0xFF05012C.toInt(),
    val uiSwitchTextTrackOn: Int = 0xFFFFB500.toInt(),
    val uiSwitchTextKnob: Int = 0xFFFFFFFF.toInt(),
    val uiSwitchTextColor: Int = 0xFF78768D.toInt(),
    val uiSwitchTextOn: String = "On",
    val uiSwitchTextOff: String = "Off",
    val rewindEnabled: Boolean = true,
    val rewindWindowSeconds: Int = 10,
    val rewindScopeMode: Int = 0,
    val rewindScope: Int = 0,
    val rewindRadius: Float = 64f,
    val rewindCooldownTicks: Int = 600,
    val rewindPlaybackMode: Int = 0,
    val rewindPlaybackSeconds: Float = 2.5f,
    val rewindRestoreOrder: Int = 0,
    val rewindCameraMode: Int = 0,
    val rewindPositionRewind: Boolean = false,
    val rewindPositionMode: Int = 0,
    val rewindPlayerState: Boolean = true,
    val rewindDeathEnabled: Boolean = true,
    val rewindDeathCooldownTicks: Int = 1200,
    val rewindDeathMaxRetries: Int = 0,
    val rewindSafetyCheckpoint: Boolean = false,
    val rewindHostileCheck: Boolean = false,
    val rewindOtherItemDeduct: Boolean = true,
    val rewindFreezeOthers: Boolean = true,
    val rewindTimestopStacking: Boolean = false,
    val rewindBlocks: Boolean = true,
    val rewindBlockEntities: Boolean = true,
    val rewindEntities: Boolean = true,
    val rewindItems: Boolean = true,
    val rewindExperience: Boolean = true,
    val rewindTime: Boolean = true,
    val rewindWeather: Boolean = true,
    val rewindRaids: Boolean = true,
    val rewindScoreboard: Boolean = true,
    val rewindWorldBorder: Boolean = true,
    val rewindFreeCamRestorePosition: Boolean = true,
    val rewindShowStats: Boolean = true
)

private fun v9StateOf(config: YuanGodSwordConfig): V9State = V9State(
    enabled = config.enabled,
    invulnerable = config.invulnerable,
    grayScreen = config.grayScreen,
    grayStrength = config.grayStrength,
    grayAnimate = config.grayAnimate,
    startAnim = config.startAnim,
    endAnim = config.endAnim,
    startDuration = config.startDuration,
    endDuration = config.endDuration,
    ballColor = config.ballColor,
    ballColorCustom = config.ballColorCustom,
    customColor = config.customColor,
    particleSize = config.particleSize,
    stopDuration = config.stopDuration,
    triggerMode = config.triggerMode,
    soundEnabled = config.soundEnabled,
    cooldown = config.cooldown,
    soundVolume = config.soundVolume,
    particleAlpha = config.particleAlpha,
    particleCount = config.particleCount,
    particleSpin = config.particleSpin,
    freezeSelf = config.freezeSelf,
    stopRadius = config.stopRadius,
    freezeEntities = config.freezeEntities,
    freezeBlocks = config.freezeBlocks,
    freezeFluids = config.freezeFluids,
    freezeBossAI = config.freezeBossAI,
    showMessage = config.showMessage,
    grayStyle = config.grayStyle,
    slashEnabled = config.slashEnabled,
    slashDepthTest = config.slashDepthTest,
    slashRandomAngle = config.slashRandomAngle,
    slashGlow = config.slashGlow,
    slashDuration = config.slashDuration,
    slashLengthMult = config.slashLengthMult,
    slashWidthRatio = config.slashWidthRatio,
    slashThicknessRatio = config.slashThicknessRatio,
    slashTipFade = config.slashTipFade,
    slashStartScale = config.slashStartScale,
    slashEndScale = config.slashEndScale,
    slashCoreWidth = config.slashCoreWidth,
    slashCoreShade = config.slashCoreShade,
    slashEdgeWidth = config.slashEdgeWidth,
    slashEdgeBrightness = config.slashEdgeBrightness,
    slashGlowWidth = config.slashGlowWidth,
    slashGlowStrength = config.slashGlowStrength,
    slashNoiseStrength = config.slashNoiseStrength,
    slashSweepSpeed = config.slashSweepSpeed,
    slashSweepSoftness = config.slashSweepSoftness,
    slashHoldFraction = config.slashHoldFraction,
    slashFadeStart = config.slashFadeStart,
    slashFadeDuration = config.slashFadeDuration,
    slashSurfaceOffset = config.slashSurfaceOffset,
    slashRollRange = config.slashRollRange,
    slashCoreColor = config.slashCoreColor,
    slashEdgeColor = config.slashEdgeColor,
    slashGlowColor = config.slashGlowColor,
    renderStyle = config.renderStyle,
    silkColor0 = config.silkColor0,
    silkColor1 = config.silkColor1,
    silkColor2 = config.silkColor2,
    silkColor3 = config.silkColor3,
    silkColor4 = config.silkColor4,
    silkColor5 = config.silkColor5,
    silkColor6 = config.silkColor6,
    silkColor7 = config.silkColor7,
    silkBrightness = config.silkBrightness,
    silkContrast = config.silkContrast,
    silkSaturation = config.silkSaturation,
    silkScale = config.silkScale,
    silkIntensity = config.silkIntensity,
    silkWarp = config.silkWarp,
    silkDetail = config.silkDetail,
    silkHue = config.silkHue,
    silkSeed = config.silkSeed,
    silkRotation = config.silkRotation,
    silkDrift = config.silkDrift,
    silkVignette = config.silkVignette,
    silkBlur = config.silkBlur,
    silkGrain = config.silkGrain,
    tunnelSpeed = config.tunnelSpeed,
    tunnelBrightness = config.tunnelBrightness,
    tunnelDensity = config.tunnelDensity,
    tunnelFov = config.tunnelFov,
    voronoiColorCount = config.voronoiColorCount,
    voronoiStepsPerColor = config.voronoiStepsPerColor,
    voronoiColor0 = config.voronoiColor0,
    voronoiColor1 = config.voronoiColor1,
    voronoiColor2 = config.voronoiColor2,
    voronoiColor3 = config.voronoiColor3,
    voronoiColor4 = config.voronoiColor4,
    voronoiColorGlow = config.voronoiColorGlow,
    voronoiColorGap = config.voronoiColorGap,
    voronoiDistortion = config.voronoiDistortion,
    voronoiGap = config.voronoiGap,
    voronoiGlow = config.voronoiGlow,
    voronoiScale = config.voronoiScale,
    voronoiFov = config.voronoiFov,
    voronoiSpeed = config.voronoiSpeed,
    voronoiRotation = config.voronoiRotation,
    voronoiOffsetX = config.voronoiOffsetX,
    voronoiOffsetY = config.voronoiOffsetY,
    slashStarfield = config.slashStarfield,
    slashStarDensity = config.slashStarDensity,
    slashStarBrightness = config.slashStarBrightness,
    slashStarSize = config.slashStarSize,
    slashStarColorMode = config.slashStarColorMode,
    uiTabVariant = config.uiTabVariant,
    uiTabSize = config.uiTabSize,
    uiTabCompact = config.uiTabCompact,
    uiSliderHeight = config.uiSliderHeight,
    uiSliderThumb = config.uiSliderThumb,
    uiSearchStyle = config.uiSearchStyle,
    uiSearchWidth = config.uiSearchWidth,
    uiSearchHeight = config.uiSearchHeight,
    uiSearchRadius = config.uiSearchRadius,
    uiSwitchStyle = config.uiSwitchStyle,
    uiSwitchWidth = config.uiSwitchWidth,
    uiSwitchHeight = config.uiSwitchHeight,
    uiSwitchKnob = config.uiSwitchKnob,
    uiSwitchGradientA = config.uiSwitchGradientA,
    uiSwitchGradientB = config.uiSwitchGradientB,
    uiSwitchTrackOff = config.uiSwitchTrackOff,
    uiSwitchSlideTrack = config.uiSwitchSlideTrack,
    uiSwitchSlideOff = config.uiSwitchSlideOff,
    uiSwitchSlideOn = config.uiSwitchSlideOn,
    uiSwitchTextTrackOff = config.uiSwitchTextTrackOff,
    uiSwitchTextTrackOn = config.uiSwitchTextTrackOn,
    uiSwitchTextKnob = config.uiSwitchTextKnob,
    uiSwitchTextColor = config.uiSwitchTextColor,
    uiSwitchTextOn = config.uiSwitchTextOn,
    uiSwitchTextOff = config.uiSwitchTextOff,
    rewindEnabled = config.rewindEnabled,
    rewindWindowSeconds = config.rewindWindowSeconds,
    rewindScopeMode = config.rewindScopeMode,
    rewindScope = config.rewindScope,
    rewindRadius = config.rewindRadius,
    rewindCooldownTicks = config.rewindCooldownTicks,
    rewindPlaybackMode = config.rewindPlaybackMode,
    rewindPlaybackSeconds = config.rewindPlaybackSeconds,
    rewindRestoreOrder = config.rewindRestoreOrder,
    rewindCameraMode = config.rewindCameraMode,
    rewindPositionRewind = config.rewindPositionRewind,
    rewindPositionMode = config.rewindPositionMode,
    rewindPlayerState = config.rewindPlayerState,
    rewindDeathEnabled = config.rewindDeathEnabled,
    rewindDeathCooldownTicks = config.rewindDeathCooldownTicks,
    rewindDeathMaxRetries = config.rewindDeathMaxRetries,
    rewindSafetyCheckpoint = config.rewindSafetyCheckpoint,
    rewindHostileCheck = config.rewindHostileCheck,
    rewindOtherItemDeduct = config.rewindOtherItemDeduct,
    rewindFreezeOthers = config.rewindFreezeOthers,
    rewindTimestopStacking = config.rewindTimestopStacking,
    rewindBlocks = config.rewindBlocks,
    rewindBlockEntities = config.rewindBlockEntities,
    rewindEntities = config.rewindEntities,
    rewindItems = config.rewindItems,
    rewindExperience = config.rewindExperience,
    rewindTime = config.rewindTime,
    rewindWeather = config.rewindWeather,
    rewindRaids = config.rewindRaids,
    rewindScoreboard = config.rewindScoreboard,
    rewindWorldBorder = config.rewindWorldBorder,
    rewindFreeCamRestorePosition = config.rewindFreeCamRestorePosition,
    rewindShowStats = config.rewindShowStats
)

private fun V9State.toConfig(): YuanGodSwordConfig = YuanGodSwordConfig().also {
    it.enabled = enabled
    it.invulnerable = invulnerable
    it.grayScreen = grayScreen
    it.grayStrength = grayStrength
    it.grayAnimate = grayAnimate
    it.startAnim = startAnim
    it.endAnim = endAnim
    it.startDuration = startDuration
    it.endDuration = endDuration
    it.ballColor = ballColor
    it.ballColorCustom = ballColorCustom
    it.customColor = customColor
    it.particleSize = particleSize
    it.stopDuration = stopDuration
    it.triggerMode = triggerMode
    it.soundEnabled = soundEnabled
    it.cooldown = cooldown
    it.soundVolume = soundVolume
    it.particleAlpha = particleAlpha
    it.particleCount = particleCount
    it.particleSpin = particleSpin
    it.freezeSelf = freezeSelf
    it.stopRadius = stopRadius
    it.freezeEntities = freezeEntities
    it.freezeBlocks = freezeBlocks
    it.freezeFluids = freezeFluids
    it.freezeBossAI = freezeBossAI
    it.showMessage = showMessage
    it.grayStyle = grayStyle
    it.slashEnabled = slashEnabled
    it.slashDepthTest = slashDepthTest
    it.slashRandomAngle = slashRandomAngle
    it.slashGlow = slashGlow
    it.slashDuration = slashDuration
    it.slashLengthMult = slashLengthMult
    it.slashWidthRatio = slashWidthRatio
    it.slashThicknessRatio = slashThicknessRatio
    it.slashTipFade = slashTipFade
    it.slashStartScale = slashStartScale
    it.slashEndScale = slashEndScale
    it.slashCoreWidth = slashCoreWidth
    it.slashCoreShade = slashCoreShade
    it.slashEdgeWidth = slashEdgeWidth
    it.slashEdgeBrightness = slashEdgeBrightness
    it.slashGlowWidth = slashGlowWidth
    it.slashGlowStrength = slashGlowStrength
    it.slashNoiseStrength = slashNoiseStrength
    it.slashSweepSpeed = slashSweepSpeed
    it.slashSweepSoftness = slashSweepSoftness
    it.slashHoldFraction = slashHoldFraction
    it.slashFadeStart = slashFadeStart
    it.slashFadeDuration = slashFadeDuration
    it.slashSurfaceOffset = slashSurfaceOffset
    it.slashRollRange = slashRollRange
    it.slashCoreColor = slashCoreColor
    it.slashEdgeColor = slashEdgeColor
    it.slashGlowColor = slashGlowColor
    it.renderStyle = renderStyle
    it.silkColor0 = silkColor0
    it.silkColor1 = silkColor1
    it.silkColor2 = silkColor2
    it.silkColor3 = silkColor3
    it.silkColor4 = silkColor4
    it.silkColor5 = silkColor5
    it.silkColor6 = silkColor6
    it.silkColor7 = silkColor7
    it.silkBrightness = silkBrightness
    it.silkContrast = silkContrast
    it.silkSaturation = silkSaturation
    it.silkScale = silkScale
    it.silkIntensity = silkIntensity
    it.silkWarp = silkWarp
    it.silkDetail = silkDetail
    it.silkHue = silkHue
    it.silkSeed = silkSeed
    it.silkRotation = silkRotation
    it.silkDrift = silkDrift
    it.silkVignette = silkVignette
    it.silkBlur = silkBlur
    it.silkGrain = silkGrain
    it.tunnelSpeed = tunnelSpeed
    it.tunnelBrightness = tunnelBrightness
    it.tunnelDensity = tunnelDensity
    it.tunnelFov = tunnelFov
    it.voronoiColorCount = voronoiColorCount
    it.voronoiStepsPerColor = voronoiStepsPerColor
    it.voronoiColor0 = voronoiColor0
    it.voronoiColor1 = voronoiColor1
    it.voronoiColor2 = voronoiColor2
    it.voronoiColor3 = voronoiColor3
    it.voronoiColor4 = voronoiColor4
    it.voronoiColorGlow = voronoiColorGlow
    it.voronoiColorGap = voronoiColorGap
    it.voronoiDistortion = voronoiDistortion
    it.voronoiGap = voronoiGap
    it.voronoiGlow = voronoiGlow
    it.voronoiScale = voronoiScale
    it.voronoiFov = voronoiFov
    it.voronoiSpeed = voronoiSpeed
    it.voronoiRotation = voronoiRotation
    it.voronoiOffsetX = voronoiOffsetX
    it.voronoiOffsetY = voronoiOffsetY
    it.slashStarfield = slashStarfield
    it.slashStarDensity = slashStarDensity
    it.slashStarBrightness = slashStarBrightness
    it.slashStarSize = slashStarSize
    it.slashStarColorMode = slashStarColorMode
    it.uiTabVariant = uiTabVariant
    it.uiTabSize = uiTabSize
    it.uiTabCompact = uiTabCompact
    it.uiSliderHeight = uiSliderHeight
    it.uiSliderThumb = uiSliderThumb
    it.uiSearchStyle = uiSearchStyle
    it.uiSearchWidth = uiSearchWidth
    it.uiSearchHeight = uiSearchHeight
    it.uiSearchRadius = uiSearchRadius
    it.uiSwitchStyle = uiSwitchStyle
    it.uiSwitchWidth = uiSwitchWidth
    it.uiSwitchHeight = uiSwitchHeight
    it.uiSwitchKnob = uiSwitchKnob
    it.uiSwitchGradientA = uiSwitchGradientA
    it.uiSwitchGradientB = uiSwitchGradientB
    it.uiSwitchTrackOff = uiSwitchTrackOff
    it.uiSwitchSlideTrack = uiSwitchSlideTrack
    it.uiSwitchSlideOff = uiSwitchSlideOff
    it.uiSwitchSlideOn = uiSwitchSlideOn
    it.uiSwitchTextTrackOff = uiSwitchTextTrackOff
    it.uiSwitchTextTrackOn = uiSwitchTextTrackOn
    it.uiSwitchTextKnob = uiSwitchTextKnob
    it.uiSwitchTextColor = uiSwitchTextColor
    it.uiSwitchTextOn = uiSwitchTextOn
    it.uiSwitchTextOff = uiSwitchTextOff
    it.rewindEnabled = rewindEnabled
    it.rewindWindowSeconds = rewindWindowSeconds
    it.rewindScopeMode = rewindScopeMode
    it.rewindScope = rewindScope
    it.rewindRadius = rewindRadius
    it.rewindCooldownTicks = rewindCooldownTicks
    it.rewindPlaybackMode = rewindPlaybackMode
    it.rewindPlaybackSeconds = rewindPlaybackSeconds
    it.rewindRestoreOrder = rewindRestoreOrder
    it.rewindCameraMode = rewindCameraMode
    it.rewindPositionRewind = rewindPositionRewind
    it.rewindPositionMode = rewindPositionMode
    it.rewindPlayerState = rewindPlayerState
    it.rewindDeathEnabled = rewindDeathEnabled
    it.rewindDeathCooldownTicks = rewindDeathCooldownTicks
    it.rewindDeathMaxRetries = rewindDeathMaxRetries
    it.rewindSafetyCheckpoint = rewindSafetyCheckpoint
    it.rewindHostileCheck = rewindHostileCheck
    it.rewindOtherItemDeduct = rewindOtherItemDeduct
    it.rewindFreezeOthers = rewindFreezeOthers
    it.rewindTimestopStacking = rewindTimestopStacking
    it.rewindBlocks = rewindBlocks
    it.rewindBlockEntities = rewindBlockEntities
    it.rewindEntities = rewindEntities
    it.rewindItems = rewindItems
    it.rewindExperience = rewindExperience
    it.rewindTime = rewindTime
    it.rewindWeather = rewindWeather
    it.rewindRaids = rewindRaids
    it.rewindScoreboard = rewindScoreboard
    it.rewindWorldBorder = rewindWorldBorder
    it.rewindFreeCamRestorePosition = rewindFreeCamRestorePosition
    it.rewindShowStats = rewindShowStats
}

private data class V9Row(
    val id: String,
    val name: String,
    val desc: String,
    val type: Int,
    val section: String,
    val hint: String
)

private data class V9Section(val name: String, val hint: String, val rows: List<V9Row>)

private val ballColors = listOf(
    0xFFFF7A5C, 0xFFE85D3D, 0xFFFFC9BA, 0xFF49322C,
    0xFF7C5CFF, 0xFF3DD6E8, 0xFF7AE85D, 0xFFFFD93D
)
private val quotes = listOf(
    "星河长明，剑心不改。",
    "剑锋所指，即是我心。",
    "星云为骨，时间作刃。",
    "一瞬千年，一剑永恒。",
    "沉默的星，不灭的剑。"
)

private val v9Sections = listOf(
    V9Section("基础", "时停是否可用，以及服务端规则。", listOf(
        V9Row("enabled", "启用时停", "允许触发时间停止", 0, "基础", "时停是否可用，以及服务端规则。"),
        V9Row("invulnerable", "时停无敌", "服务端权威生效", 0, "基础", "时停是否可用，以及服务端规则。"),
        V9Row("triggerMode", "触发方式", "右键 / Shift+右键 / 自定义键", 1, "基础", "时停是否可用，以及服务端规则。"),
        V9Row("showMessage", "触发提示", "动作栏显示开始/结束", 0, "基础", "时停是否可用，以及服务端规则。"),
        V9Row("cooldown", "冷却时间", "0 至 100 tick", 2, "基础", "时停是否可用，以及服务端规则。")
    )),
    V9Section("动画", "启动与关闭的视觉节奏。", listOf(
        V9Row("startAnim", "启动动画", "球体从玩家向外爆开", 1, "动画", "启动与关闭的视觉节奏。"),
        V9Row("endAnim", "关闭动画", "光球向内收束", 1, "动画", "启动与关闭的视觉节奏。"),
        V9Row("startDuration", "启动时长", "0.1 至 2 秒", 2, "动画", "启动与关闭的视觉节奏。"),
        V9Row("endDuration", "关闭时长", "0.1 至 2 秒", 2, "动画", "启动与关闭的视觉节奏。")
    )),
    V9Section("画面", "后处理滤镜强度。", listOf(
        V9Row("grayScreen", "黑白画面", "时停期间灰白后处理", 0, "画面", "后处理滤镜强度。"),
        V9Row("grayStrength", "黑白强度", "0 彩色 / 1 全黑白", 2, "画面", "后处理滤镜强度。"),
        V9Row("grayAnimate", "黑白随时间变化", "开启后饱和度脉动", 0, "画面", "后处理滤镜强度。"),
        V9Row("grayStyle", "黑白滤镜", "纯黑白 / 复古 / 暗角", 1, "画面", "后处理滤镜强度。")
    )),
    V9Section("音效", "时停音效。", listOf(
        V9Row("soundEnabled", "时停音效", "启动时播放时停音效", 0, "音效", "时停音效。"),
        V9Row("soundVolume", "音效音量", "0 至 100%", 2, "音效", "时停音效。")
    )),
    V9Section("光球", "光球外观与粒子效果。", listOf(
        V9Row("particleSize", "光球大小", "0.4 至 3.0", 2, "光球", "光球外观与粒子效果。"),
        V9Row("particleAlpha", "光球透明度", "0.1 至 1.0", 2, "光球", "光球外观与粒子效果。"),
        V9Row("particleCount", "粒子数量", "1 至 16 个", 2, "光球", "光球外观与粒子效果。"),
        V9Row("particleSpin", "粒子旋转", "0 至 3.0", 2, "光球", "光球外观与粒子效果。"),
        V9Row("ballColor", "光球颜色", "八种预设或自定义 RGB", 3, "光球", "光球外观与粒子效果。")
    )),
    V9Section("时长", "时停持续与自动结束。", listOf(
        V9Row("stopDuration", "时停持续时间", "0 为手动开关，最长 60 秒", 2, "时长", "时停持续与自动结束。")
    )),
    V9Section("冻结", "时停影响范围与对象。", listOf(
        V9Row("freezeSelf", "冻结自己", "开启后玩家自身也定身", 0, "冻结", "时停影响范围与对象。"),
        V9Row("stopRadius", "时停范围", "0 为全场，最大 128 格", 2, "冻结", "时停影响范围与对象。"),
        V9Row("freezeEntities", "冻结实体", "普通实体停止行动", 0, "冻结", "时停影响范围与对象。"),
        V9Row("freezeBlocks", "冻结方块", "方块刻与更新停止", 0, "冻结", "时停影响范围与对象。"),
        V9Row("freezeFluids", "冻结流体", "液体不再流动", 0, "冻结", "时停影响范围与对象。"),
        V9Row("freezeBossAI", "冻结 Boss AI", "Boss 也随实体冻结", 0, "冻结", "时停影响范围与对象。")
    )),
    V9Section("预设", "快速保存与载入整套配置。", listOf(
        V9Row("preset", "配置预设", "槽 1-3 保存/载入", 4, "预设", "快速保存与载入整套配置。")
    ))
)

private val navSections = mapOf(
    "时停" to setOf("基础", "动画", "画面", "音效", "光球", "时长", "冻结", "预设"),
    "斩击" to setOf("斩击"),
    "武器渲染" to setOf("武器渲染"),
    "回溯" to setOf("回溯"),
    "设置" to setOf("设置")
)

private val timestopTabs = listOf("基础", "效果", "音效", "冻结", "预设")
private val timestopTabSections = mapOf(
    "基础" to setOf("基础", "时长"),
    "效果" to setOf("动画", "画面", "光球"),
    "音效" to setOf("音效"),
    "冻结" to setOf("冻结"),
    "预设" to setOf("预设")
)

@Composable
private fun V9SearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = LocalV9UiPrefs.current
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val collapsed = prefs.searchStyle == 1 && !focused && value.isEmpty()
    val targetWidth = if (collapsed) prefs.searchHeight else prefs.searchWidth
    val animWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "searchWidth"
    )
    val radius = prefs.searchRadius.coerceAtMost(prefs.searchHeight / 2f)
    val iconActive = focused || value.isNotEmpty()

    Box(
        modifier
            .width(animWidth)
            .height(prefs.searchHeight)
            .shadow(3.dp, RoundedCornerShape(radius), ambientColor = Color(0xFF0E0E0E), spotColor = Color(0x405F5E5E))
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFF191A1E))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(radius))
            .clickable { focusRequester.requestFocus() }
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(prefs.searchHeight).height(prefs.searchHeight).padding(8.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 2.dp.toPx()
                    val c = if (iconActive) Color(0xFFFF7A5C) else Color(0xFF9AA2B0)
                    val r = size.minDimension / 2f - stroke
                    drawCircle(
                        c,
                        radius = r,
                        center = Offset(size.minDimension / 2f - 1.dp.toPx(), size.minDimension / 2f - 1.dp.toPx()),
                        style = Stroke(width = stroke)
                    )
                    drawLine(
                        c,
                        start = Offset(size.minDimension / 2f + r * 0.7f, size.minDimension / 2f + r * 0.7f),
                        end = Offset(size.minDimension - 1.dp.toPx(), size.minDimension - 1.dp.toPx()),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
            Box(Modifier.weight(1f).height(prefs.searchHeight)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFD7DCE5), fontFamily = cnFont),
                    cursorBrush = SolidColor(Color(0xFFFF7A5C)),
                    modifier = Modifier.fillMaxSize()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused }
                        .padding(end = 12.dp),
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    "搜索设置",
                                    fontSize = 12.sp,
                                    color = Color(0xFF646D7B),
                                    fontFamily = cnFont,
                                    maxLines = 1
                                )
                            }
                            inner()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun V9Header(search: String, onSearch: (String) -> Unit, dirty: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(52.dp)
            .drawBehind {
                drawLine(Color(0x24E9ECF2), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("星渊", fontSize = 17.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont, fontWeight = FontWeight.Bold)
        Text("/", fontSize = 17.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont, fontWeight = FontWeight.Bold)
        Text("神剑", fontSize = 17.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(18.dp))
        V9SearchBox(
            value = search,
            onValueChange = onSearch
        )
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (dirty) "有未保存的更改" else "已保存",
                fontSize = 12.sp,
                color = if (dirty) Color(0xFFFF7A5C) else Color(0xFF9AA2B0),
                fontFamily = cnFont
            )
            Text("· 神剑专属", fontSize = 10.sp, color = Color(0xFF646D7B), fontFamily = cnFont)
        }
    }
}

@Composable
private fun V9LobeTabs(
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    variant: Int,
    size: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = if (compact) 4.dp else 6.dp
    val hPad = when (size) {
        0 -> if (compact) 8.dp else 10.dp
        2 -> if (compact) 12.dp else 14.dp
        else -> if (compact) 10.dp else 12.dp
    }
    val vPad = when (size) {
        0 -> if (compact) 4.dp else 5.dp
        2 -> if (compact) 6.dp else 8.dp
        else -> if (compact) 5.dp else 6.dp
    }
    val font = when (size) {
        0 -> 11.sp
        2 -> 13.sp
        else -> 12.sp
    }
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items.forEach { tab ->
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val isSelected = tab == selected
            val reveal by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "tabReveal"
            )
            val selectedModifier = when (variant) {
                1 -> Modifier
                else -> Modifier
                    .background(Color(0xFFFF7A5C).copy(alpha = 0.15f * reveal), RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        Color(0xFFE9ECF2).copy(alpha = 0.08f * reveal),
                        RoundedCornerShape(12.dp)
                    )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hovered && !isSelected) Color(0x0FFFFFFF) else Color.Transparent)
                    .then(selectedModifier)
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(tab) }
                    .padding(horizontal = hPad, vertical = vPad)
            ) {
                Text(
                    tab,
                    fontSize = font,
                    color = if (isSelected) Color(0xFFFFA08A) else Color(0xFF9AA2B0),
                    fontFamily = cnFont
                )
                when (variant) {
                    1 -> {
                        val lineReveal by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            label = "tabLine"
                        )
                        Box(
                            Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth(lineReveal)
                                .height(2.dp)
                                .background(Color(0xFFFF7A5C), RoundedCornerShape(1.dp))
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun V9Sidebar(modifier: Modifier, selectedNav: String, onSelect: (String) -> Unit) {
    val items = listOf("时停", "武器渲染", "斩击", "回溯", "设置")
    Column(
        modifier.background(Brush.verticalGradient(listOf(Color(0xFF101319), Color(0xFF0D0F14)))).drawBehind {
            drawLine(Color(0x24E9ECF2), Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
        }.padding(top = 10.dp, bottom = 10.dp)
    ) {
        Text("导航", fontSize = 11.sp, color = Color(0xFF646D7B), fontFamily = cnFont, modifier = Modifier.padding(horizontal = 14.dp))
        Spacer(Modifier.height(12.dp))
        items.forEach { item ->
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val selected = item == selectedNav
            Row(
                Modifier.fillMaxWidth().height(38.dp)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color(0x21FF7A5C) else if (hovered) Color(0x08FFFFFF) else Color.Transparent)
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(item) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.width(3.dp).height(24.dp)
                        .background(if (selected) Color(0xFFFF7A5C) else Color.Transparent, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(10.dp))
                Text(item, fontSize = 13.sp, color = if (selected) Color(0xFFF2F4F8) else Color(0xFF9AA2B0), fontFamily = cnFont)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("设置跟随这把神剑保存", fontSize = 10.sp, color = Color(0xFF646D7B), fontFamily = cnFont, modifier = Modifier.padding(horizontal = 14.dp))
    }
}

@Composable
private fun V9Center(
    modifier: Modifier,
    selectedNav: String,
    sections: List<Pair<V9Section, List<V9Row>>>,
    enabled: Boolean, invulnerable: Boolean, grayScreen: Boolean, grayStrength: Float, grayAnimate: Boolean,
    startAnim: Int, endAnim: Int, startDuration: Float, endDuration: Float,
    ballColor: Int, ballColorCustom: Boolean, customColor: Int,
    particleSize: Float, stopDuration: Float, triggerMode: Int, soundEnabled: Boolean, cooldown: Int,
    soundVolume: Float, particleAlpha: Float, particleCount: Int, particleSpin: Float,
    freezeSelf: Boolean, stopRadius: Float, freezeEntities: Boolean, freezeBlocks: Boolean,
    freezeFluids: Boolean, freezeBossAI: Boolean, showMessage: Boolean, grayStyle: Int,
    presetSlot: Int, onPresetLoad: (Int) -> Unit, onPresetSave: () -> Unit,
    onToggle: (String) -> Unit,
    onSegment: (String, Int) -> Unit,
    onText: (String, String) -> Unit,
    onSlider: (String, Float) -> Unit,
    onBall: (Int) -> Unit,
    onCustomColor: (Int) -> Unit,
    state: V9State,
    uiTabVariant: Int,
    uiTabSize: Int,
    uiTabCompact: Boolean,
    sectionTab: String,
    onSectionTab: (String) -> Unit,
    onResetPage: (String) -> Unit
) {
    if (selectedNav == "设置") {
        V9SettingsPanel(
            modifier = modifier,
            state = state,
            uiTabVariant = uiTabVariant,
            uiTabSize = uiTabSize,
            uiTabCompact = uiTabCompact,
            onToggle = onToggle,
            onSegment = onSegment,
            onText = onText,
            onSlider = onSlider,
            onReset = onResetPage
        )
        return
    }
    if (selectedNav == "武器渲染") {
        V9WeaponPanel(
            modifier = modifier,
            state = state,
            onToggle = onToggle,
            onSegment = onSegment,
            onSlider = onSlider,
            onReset = onResetPage
        )
        return
    }
    if (selectedNav == "斩击") {
        V9SlashPanel(
            modifier = modifier,
            state = state,
            uiTabVariant = uiTabVariant,
            uiTabSize = uiTabSize,
            uiTabCompact = uiTabCompact,
            onToggle = onToggle,
            onSlider = onSlider,
            onSegment = onSegment,
            onReset = onResetPage
        )
        return
    }
    if (selectedNav == "回溯") {
        V9RewindPanel(
            modifier = modifier,
            state = state,
            onToggle = onToggle,
            onSegment = onSegment,
            onSlider = onSlider,
            onReset = onResetPage
        )
        return
    }
    Column(
        modifier.background(Brush.verticalGradient(listOf(Color(0xFF13161D), Color(0xFF0F1217)))).drawBehind {
            drawLine(Color(0x24E9ECF2), Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
        }.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(if (selectedNav == "时停") "时停设置" else "${selectedNav}设置", fontSize = 14.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont)
        Text(
            when {
                selectedNav != "时停" -> "设置跟随这把神剑保存"
                triggerMode == 1 -> "Shift + 右键触发"
                triggerMode == 2 -> "自定义键触发"
                else -> "右键触发"
            },
            fontSize = 11.sp,
            color = Color(0xFF9AA2B0),
            fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )
        if (selectedNav == "时停") {
            V9LobeTabs(
                items = timestopTabs,
                selected = sectionTab,
                onSelect = onSectionTab,
                variant = uiTabVariant,
                size = uiTabSize,
                compact = uiTabCompact,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        val shown = if (selectedNav == "时停") {
            sections.filter { timestopTabSections[sectionTab]?.contains(it.first.name) == true }
        } else {
            sections
        }
        shown.forEach { (section, rows) ->
            Row(Modifier.padding(top = 8.dp, bottom = 2.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(section.name, fontSize = 12.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont)
                Text(
                    section.hint,
                    fontSize = 10.sp,
                    color = Color(0xFF646D7B),
                    fontFamily = cnFont,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    maxLines = 2
                )
            }
            rows.forEach { row ->
                when (row.type) {
                    0 -> V9SwitchRow(
                        row.name, row.desc,
                        switchValue(
                            row.id, enabled, invulnerable, grayScreen, grayAnimate,
                            soundEnabled, freezeSelf, freezeEntities,
                            freezeBlocks, freezeFluids, freezeBossAI, showMessage
                        )
                    ) {
                        onToggle(row.id)
                    }
                    1 -> V9SegmentRow(
                        row.name, row.desc,
                        when (row.id) {
                            "startAnim" -> listOf("爆开", "冲击波", "无")
                            "endAnim" -> listOf("收束", "无")
                            "triggerMode" -> listOf("右键", "Shift + 右键", "自定义键")
                            else -> listOf("纯黑白", "复古", "暗角")
                        },
                        when (row.id) {
                            "startAnim" -> startAnim
                            "endAnim" -> endAnim
                            "triggerMode" -> triggerMode
                            else -> grayStyle
                        }
                    ) { onSegment(row.id, it) }
                    2 -> V9SliderRow(
                        row.name, row.desc,
                        when (row.id) {
                            "startDuration" -> startDuration
                            "endDuration" -> endDuration
                            "grayStrength" -> grayStrength
                            "particleSize" -> particleSize
                            "stopDuration" -> stopDuration
                            "cooldown" -> cooldown.toFloat()
                            "soundVolume" -> soundVolume
                            "particleAlpha" -> particleAlpha
                            "particleCount" -> particleCount.toFloat()
                            "particleSpin" -> particleSpin
                            else -> stopRadius
                        },
                        when (row.id) {
                            "grayStrength" -> 0f
                            "particleSize" -> 0.4f
                            "stopDuration" -> 0f
                            "cooldown" -> 0f
                            "soundVolume" -> 0f
                            "particleAlpha" -> 0.1f
                            "particleCount" -> 1f
                            "particleSpin" -> 0f
                            else -> 0.1f
                        },
                        when (row.id) {
                            "grayStrength" -> 1f
                            "particleSize" -> 3f
                            "stopDuration" -> 60f
                            "cooldown" -> 100f
                            "soundVolume" -> 1f
                            "particleAlpha" -> 1f
                            "particleCount" -> 16f
                            "particleSpin" -> 3f
                            else -> 2f
                        },
                        when (row.id) {
                            "grayStrength" -> { v -> String.format(java.util.Locale.ROOT, "%.0f%%", v * 100f) }
                            "particleSize" -> { v -> String.format(java.util.Locale.ROOT, "%.2f", v) }
                            "stopDuration" -> { v -> if (v <= 0f) "手动" else String.format(java.util.Locale.ROOT, "%.1fs", v) }
                            "cooldown" -> { v -> "${v.toInt()} tick" }
                            "soundVolume" -> { v -> String.format(java.util.Locale.ROOT, "%.0f%%", v * 100f) }
                            "particleAlpha" -> { v -> String.format(java.util.Locale.ROOT, "%.2f", v) }
                            "particleCount" -> { v -> "${v.toInt()} 个" }
                            "particleSpin" -> { v -> String.format(java.util.Locale.ROOT, "%.2f", v) }
                            "stopRadius" -> { v -> if (v <= 0f) "全场" else String.format(java.util.Locale.ROOT, "%.0f 格", v) }
                            else -> { v -> String.format(java.util.Locale.ROOT, "%.1fs", v) }
                        }
                    ) { onSlider(row.id, it) }
                    3 -> V9SwatchRow(row.name, row.desc, ballColor, ballColorCustom, customColor, onBall, onCustomColor)
                    else -> V9PresetRow(presetSlot, onPresetLoad, onPresetSave)
                }
            }
        }
    }
}

private fun switchValue(
    id: String,
    enabled: Boolean,
    invulnerable: Boolean,
    grayScreen: Boolean,
    grayAnimate: Boolean,
    soundEnabled: Boolean,
    freezeSelf: Boolean,
    freezeEntities: Boolean,
    freezeBlocks: Boolean,
    freezeFluids: Boolean,
    freezeBossAI: Boolean,
    showMessage: Boolean
): Boolean = when (id) {
    "enabled" -> enabled
    "invulnerable" -> invulnerable
    "grayScreen" -> grayScreen
    "grayAnimate" -> grayAnimate
    "soundEnabled" -> soundEnabled
    "freezeSelf" -> freezeSelf
    "freezeEntities" -> freezeEntities
    "freezeBlocks" -> freezeBlocks
    "freezeFluids" -> freezeFluids
    "freezeBossAI" -> freezeBossAI
    else -> showMessage
}

@Composable
private fun hoverBackground(): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    return Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(if (hovered) Color(0x04FFFFFF) else Color.Transparent)
        .hoverable(interaction)
}

@Composable
private fun V9SettingsPanel(
    modifier: Modifier,
    state: V9State,
    uiTabVariant: Int,
    uiTabSize: Int,
    uiTabCompact: Boolean,
    onToggle: (String) -> Unit,
    onSegment: (String, Int) -> Unit,
    onText: (String, String) -> Unit,
    onSlider: (String, Float) -> Unit,
    onReset: (String) -> Unit
) {
    var settingsTab by remember { mutableStateOf("标签栏") }
    var previewSearch by remember { mutableStateOf("") }
    var previewSwitch by remember { mutableStateOf(true) }
    Column(
        modifier.background(Brush.verticalGradient(listOf(Color(0xFF13161D), Color(0xFF0F1217))))
            .drawBehind {
                drawLine(Color(0x24E9ECF2), Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("设置", fontSize = 14.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont)
        Text("界面显示与交互样式", fontSize = 11.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        V9LobeTabs(
            items = listOf("标签栏", "滑块", "搜索框", "开关"),
            selected = settingsTab,
            onSelect = { settingsTab = it },
            variant = uiTabVariant,
            size = uiTabSize,
            compact = uiTabCompact,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        when (settingsTab) {
            "滑块" -> {
                Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("设置滑块") }.padding(bottom = 4.dp))
                V9SegmentRow("滑块高度", "矮 / 标准 / 高", listOf("矮", "标准", "高"), state.uiSliderHeight) {
                    onSegment("uiSliderHeight", it)
                }
                V9SegmentRow("滑块大小", "小 / 标准 / 大", listOf("小", "标准", "大"), state.uiSliderThumb) {
                    onSegment("uiSliderThumb", it)
                }
                Text("实时预览", fontSize = 12.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                V9SliderRow("示例", "调整上方参数即时生效", 0.62f, 0f, 1f, { "%.2f".format(it) }) { }
            }
            "搜索框" -> {
                Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("设置搜索框") }.padding(bottom = 4.dp))
                V9SegmentRow("搜索框样式", "经典 / 展开", listOf("经典", "展开"), state.uiSearchStyle) {
                    onSegment("uiSearchStyle", it)
                }
                V9SliderRow("宽度", "120~280", state.uiSearchWidth, 120f, 280f, { "%.0f".format(it) }) {
                    onSlider("uiSearchWidth", it)
                }
                V9SliderRow("高度", "28~48", state.uiSearchHeight, 28f, 48f, { "%.0f".format(it) }) {
                    onSlider("uiSearchHeight", it)
                }
                V9SliderRow("圆角", "0~24", state.uiSearchRadius, 0f, 24f, { "%.0f".format(it) }) {
                    onSlider("uiSearchRadius", it)
                }
                Text("实时预览", fontSize = 12.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                V9SearchBox(
                    value = previewSearch,
                    onValueChange = { previewSearch = it },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            "开关" -> {
                Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("设置开关") }.padding(bottom = 4.dp))
                V9SegmentRow("开关样式", "经典 / 渐变翻转 / 白底滑动 / 文字翻转", listOf("经典", "渐变翻转", "白底滑动", "文字翻转"), state.uiSwitchStyle) {
                    onSegment("uiSwitchStyle", it)
                }
                V9SliderRow("轨道宽度", "54~80", state.uiSwitchWidth, 54f, 80f, { "%.0f".format(it) }) {
                    onSlider("uiSwitchWidth", it)
                }
                V9SliderRow("轨道高度", "24~32", state.uiSwitchHeight, 24f, 32f, { "%.0f".format(it) }) {
                    onSlider("uiSwitchHeight", it)
                }
                V9SliderRow("圆钮大小", "16~24", state.uiSwitchKnob, 16f, 24f, { "%.0f".format(it) }) {
                    onSlider("uiSwitchKnob", it)
                }
                if (state.uiSwitchStyle == 1) {
                    Text("渐变颜色 1", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchGradientA shr 16) and 0xFF) { onSlider("uiSwitchGradientAR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchGradientA shr 8) and 0xFF) { onSlider("uiSwitchGradientAG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchGradientA and 0xFF) { onSlider("uiSwitchGradientAB", it.toFloat()) }
                    Text("渐变颜色 2", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchGradientB shr 16) and 0xFF) { onSlider("uiSwitchGradientBR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchGradientB shr 8) and 0xFF) { onSlider("uiSwitchGradientBG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchGradientB and 0xFF) { onSlider("uiSwitchGradientBB", it.toFloat()) }
                    Text("未选中轨道色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchTrackOff shr 16) and 0xFF) { onSlider("uiSwitchTrackOffR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchTrackOff shr 8) and 0xFF) { onSlider("uiSwitchTrackOffG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchTrackOff and 0xFF) { onSlider("uiSwitchTrackOffB", it.toFloat()) }
                } else if (state.uiSwitchStyle == 2) {
                    Text("轨道底色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchSlideTrack shr 16) and 0xFF) { onSlider("uiSwitchSlideTrackR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchSlideTrack shr 8) and 0xFF) { onSlider("uiSwitchSlideTrackG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchSlideTrack and 0xFF) { onSlider("uiSwitchSlideTrackB", it.toFloat()) }
                    Text("未选中圆钮色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchSlideOff shr 16) and 0xFF) { onSlider("uiSwitchSlideOffR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchSlideOff shr 8) and 0xFF) { onSlider("uiSwitchSlideOffG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchSlideOff and 0xFF) { onSlider("uiSwitchSlideOffB", it.toFloat()) }
                    Text("选中圆钮色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchSlideOn shr 16) and 0xFF) { onSlider("uiSwitchSlideOnR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchSlideOn shr 8) and 0xFF) { onSlider("uiSwitchSlideOnG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchSlideOn and 0xFF) { onSlider("uiSwitchSlideOnB", it.toFloat()) }
                } else if (state.uiSwitchStyle == 3) {
                    Text("未选中轨道色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchTextTrackOff shr 16) and 0xFF) { onSlider("uiSwitchTextTrackOffR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchTextTrackOff shr 8) and 0xFF) { onSlider("uiSwitchTextTrackOffG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchTextTrackOff and 0xFF) { onSlider("uiSwitchTextTrackOffB", it.toFloat()) }
                    Text("选中轨道色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchTextTrackOn shr 16) and 0xFF) { onSlider("uiSwitchTextTrackOnR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchTextTrackOn shr 8) and 0xFF) { onSlider("uiSwitchTextTrackOnG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchTextTrackOn and 0xFF) { onSlider("uiSwitchTextTrackOnB", it.toFloat()) }
                    Text("圆钮色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchTextKnob shr 16) and 0xFF) { onSlider("uiSwitchTextKnobR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchTextKnob shr 8) and 0xFF) { onSlider("uiSwitchTextKnobG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchTextKnob and 0xFF) { onSlider("uiSwitchTextKnobB", it.toFloat()) }
                    Text("文字颜色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", (state.uiSwitchTextColor shr 16) and 0xFF) { onSlider("uiSwitchTextColorR", it.toFloat()) }
                    V9ColorSlider("G", (state.uiSwitchTextColor shr 8) and 0xFF) { onSlider("uiSwitchTextColorG", it.toFloat()) }
                    V9ColorSlider("B", state.uiSwitchTextColor and 0xFF) { onSlider("uiSwitchTextColorB", it.toFloat()) }
                    V9TextInputRow("开启文字", "选中时显示的文字", state.uiSwitchTextOn) { onText("uiSwitchTextOn", it) }
                    V9TextInputRow("关闭文字", "未选中时显示的文字", state.uiSwitchTextOff) { onText("uiSwitchTextOff", it) }
                }
                Text("实时预览", fontSize = 12.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                V9Switch(previewSwitch) { previewSwitch = !previewSwitch }
            }
            else -> {
                Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("设置标签栏") }.padding(bottom = 4.dp))
                V9SegmentRow("标签样式", "圆角 / 方形", listOf("圆角", "方形"), uiTabVariant) {
                    onSegment("uiTabVariant", it)
                }
                V9SegmentRow("标签尺寸", "小 / 中 / 大", listOf("小", "中", "大"), uiTabSize) {
                    onSegment("uiTabSize", it)
                }
                V9SwitchRow("紧凑模式", "缩小标签间距与内边距", uiTabCompact) { onToggle("uiTabCompact") }
            }
        }
    }
}

@Composable
private fun V9WeaponPanel(
    modifier: Modifier,
    state: V9State,
    onToggle: (String) -> Unit,
    onSegment: (String, Int) -> Unit,
    onSlider: (String, Float) -> Unit,
    onReset: (String) -> Unit
) {
    Column(
        modifier.background(Brush.verticalGradient(listOf(Color(0xFF13161D), Color(0xFF0F1217))))
            .drawBehind {
                drawLine(Color(0x24E9ECF2), Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("武器渲染", fontSize = 14.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont)
        Text("按感知度排序：先选渲染风格，再调该风格的参数", fontSize = 11.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        V9SegmentRow("渲染风格", "星空 / 丝绸 / 隧道 / 晶格 / 原版 / 关闭",
            listOf("星空", "丝绸", "隧道", "晶格", "原版", "关闭"), renderStyleUiIndex(state.renderStyle)) { onSegment("renderStyle", it) }

        if (state.renderStyle == 1) {
        Text("丝绸", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
            modifier = Modifier.align(Alignment.End).clickable { onReset("丝绸") }.padding(bottom = 4.dp))
        Text("调色板颜色 1-4", fontSize = 12.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
        V9ColorSlider("1R", (state.silkColor0 shr 16) and 0xFF) { onSlider("silkColor0R", it.toFloat()) }
        V9ColorSlider("1G", (state.silkColor0 shr 8) and 0xFF) { onSlider("silkColor0G", it.toFloat()) }
        V9ColorSlider("1B", state.silkColor0 and 0xFF) { onSlider("silkColor0B", it.toFloat()) }
        V9ColorSlider("2R", (state.silkColor1 shr 16) and 0xFF) { onSlider("silkColor1R", it.toFloat()) }
        V9ColorSlider("2G", (state.silkColor1 shr 8) and 0xFF) { onSlider("silkColor1G", it.toFloat()) }
        V9ColorSlider("2B", state.silkColor1 and 0xFF) { onSlider("silkColor1B", it.toFloat()) }
        V9ColorSlider("3R", (state.silkColor2 shr 16) and 0xFF) { onSlider("silkColor2R", it.toFloat()) }
        V9ColorSlider("3G", (state.silkColor2 shr 8) and 0xFF) { onSlider("silkColor2G", it.toFloat()) }
        V9ColorSlider("3B", state.silkColor2 and 0xFF) { onSlider("silkColor2B", it.toFloat()) }
        V9ColorSlider("4R", (state.silkColor3 shr 16) and 0xFF) { onSlider("silkColor3R", it.toFloat()) }
        V9ColorSlider("4G", (state.silkColor3 shr 8) and 0xFF) { onSlider("silkColor3G", it.toFloat()) }
        V9ColorSlider("4B", state.silkColor3 and 0xFF) { onSlider("silkColor3B", it.toFloat()) }
        Text("调色板颜色 5-8", fontSize = 12.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
        V9ColorSlider("5R", (state.silkColor4 shr 16) and 0xFF) { onSlider("silkColor4R", it.toFloat()) }
        V9ColorSlider("5G", (state.silkColor4 shr 8) and 0xFF) { onSlider("silkColor4G", it.toFloat()) }
        V9ColorSlider("5B", state.silkColor4 and 0xFF) { onSlider("silkColor4B", it.toFloat()) }
        V9ColorSlider("6R", (state.silkColor5 shr 16) and 0xFF) { onSlider("silkColor5R", it.toFloat()) }
        V9ColorSlider("6G", (state.silkColor5 shr 8) and 0xFF) { onSlider("silkColor5G", it.toFloat()) }
        V9ColorSlider("6B", state.silkColor5 and 0xFF) { onSlider("silkColor5B", it.toFloat()) }
        V9ColorSlider("7R", (state.silkColor6 shr 16) and 0xFF) { onSlider("silkColor6R", it.toFloat()) }
        V9ColorSlider("7G", (state.silkColor6 shr 8) and 0xFF) { onSlider("silkColor6G", it.toFloat()) }
        V9ColorSlider("7B", state.silkColor6 and 0xFF) { onSlider("silkColor6B", it.toFloat()) }
        V9ColorSlider("8R", (state.silkColor7 shr 16) and 0xFF) { onSlider("silkColor7R", it.toFloat()) }
        V9ColorSlider("8G", (state.silkColor7 shr 8) and 0xFF) { onSlider("silkColor7G", it.toFloat()) }
        V9ColorSlider("8B", state.silkColor7 and 0xFF) { onSlider("silkColor7B", it.toFloat()) }
        V9SliderRow("亮度", "-1~1", state.silkBrightness, -1f, 1f, { "%.2f".format(it) }) { onSlider("silkBrightness", it) }
        V9SliderRow("对比度", "0.1~3.0", state.silkContrast, 0.1f, 3f, { "%.2f".format(it) }) { onSlider("silkContrast", it) }
        V9SliderRow("饱和度", "0~3.0", state.silkSaturation, 0f, 3f, { "%.2f".format(it) }) { onSlider("silkSaturation", it) }
        V9SliderRow("图案大小", "0.2~8.0", state.silkScale, 0.2f, 8f, { "%.2f".format(it) }) { onSlider("silkScale", it) }
        V9SliderRow("流动强度", "0~3.0", state.silkIntensity, 0f, 3f, { "%.2f".format(it) }) { onSlider("silkIntensity", it) }
        V9SliderRow("扭曲", "0~2.0", state.silkWarp, 0f, 2f, { "%.2f".format(it) }) { onSlider("silkWarp", it) }
        V9SliderRow("细节", "0.5~8.0", state.silkDetail, 0.5f, 8f, { "%.2f".format(it) }) { onSlider("silkDetail", it) }
        V9SliderRow("色相", "0~360°", state.silkHue, 0f, 6.283f, { "%.0f".format(it * 57.29578f) }) { onSlider("silkHue", it) }
        V9SliderRow("种子", "0~20000", state.silkSeed, 0f, 20000f, { "%.0f".format(it) }) { onSlider("silkSeed", it) }
        V9SliderRow("旋转", "-360~360°", state.silkRotation, -6.283f, 6.283f, { "%.0f".format(it * 57.29578f) }) { onSlider("silkRotation", it) }
        V9SliderRow("漂移", "0~3.0", state.silkDrift, 0f, 3f, { "%.2f".format(it) }) { onSlider("silkDrift", it) }
        V9SliderRow("暗角", "0~1.0", state.silkVignette, 0f, 1f, { "%.2f".format(it) }) { onSlider("silkVignette", it) }
        V9SliderRow("柔化", "0~0.05", state.silkBlur, 0f, 0.05f, { "%.3f".format(it) }) { onSlider("silkBlur", it) }
        V9SliderRow("颗粒", "0~1.0", state.silkGrain, 0f, 1f, { "%.2f".format(it) }) { onSlider("silkGrain", it) }
        }
        if (state.renderStyle == 4) {
            Text("隧道", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                modifier = Modifier.align(Alignment.End).clickable { onReset("隧道") }.padding(bottom = 4.dp))
            V9SliderRow("速度", "0.1~3.0", state.tunnelSpeed, 0.1f, 3f, { "%.2f".format(it) }) {
                onSlider("tunnelSpeed", it)
            }
            V9SliderRow("亮度", "0.1~3.0", state.tunnelBrightness, 0.1f, 3f, { "%.2f".format(it) }) {
                onSlider("tunnelBrightness", it)
            }
            V9SliderRow("密度", "0.2~3.0", state.tunnelDensity, 0.2f, 3f, { "%.2f".format(it) }) {
                onSlider("tunnelDensity", it)
            }
            V9SliderRow("视野", "1.0~16.0", state.tunnelFov, 1f, 16f, { "%.1f".format(it) }) {
                onSlider("tunnelFov", it)
            }
        }
        if (state.renderStyle == 5) {
            var voronoiPreset by remember { mutableStateOf(0) }
            Text("晶格", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                modifier = Modifier.align(Alignment.End).clickable { onReset("晶格") }.padding(bottom = 4.dp))
            V9SegmentRow("预设", "默认 / 灯光 / 细胞 / 气泡", listOf("自定义", "默认", "灯光", "细胞", "气泡"), voronoiPreset) {
                voronoiPreset = it
                if (it > 0 && it - 1 < voronoiPresets.size) {
                    val p = voronoiPresets[it - 1]
                    onSegment("voronoiColorCount", p.colorCount)
                    onSegment("voronoiStepsPerColor", p.steps)
                    onSlider("voronoiColor0A", ((p.color0 ushr 24) and 0xFF).toFloat())
                    onSlider("voronoiColor0R", ((p.color0 shr 16) and 0xFF).toFloat())
                    onSlider("voronoiColor0G", ((p.color0 shr 8) and 0xFF).toFloat())
                    onSlider("voronoiColor0B", (p.color0 and 0xFF).toFloat())
                    onSlider("voronoiColor1A", ((p.color1 ushr 24) and 0xFF).toFloat())
                    onSlider("voronoiColor1R", ((p.color1 shr 16) and 0xFF).toFloat())
                    onSlider("voronoiColor1G", ((p.color1 shr 8) and 0xFF).toFloat())
                    onSlider("voronoiColor1B", (p.color1 and 0xFF).toFloat())
                    onSlider("voronoiColor2A", ((p.color2 ushr 24) and 0xFF).toFloat())
                    onSlider("voronoiColor2R", ((p.color2 shr 16) and 0xFF).toFloat())
                    onSlider("voronoiColor2G", ((p.color2 shr 8) and 0xFF).toFloat())
                    onSlider("voronoiColor2B", (p.color2 and 0xFF).toFloat())
                    onSlider("voronoiColor3A", ((p.color3 ushr 24) and 0xFF).toFloat())
                    onSlider("voronoiColor3R", ((p.color3 shr 16) and 0xFF).toFloat())
                    onSlider("voronoiColor3G", ((p.color3 shr 8) and 0xFF).toFloat())
                    onSlider("voronoiColor3B", (p.color3 and 0xFF).toFloat())
                    onSlider("voronoiColor4A", ((p.color4 ushr 24) and 0xFF).toFloat())
                    onSlider("voronoiColor4R", ((p.color4 shr 16) and 0xFF).toFloat())
                    onSlider("voronoiColor4G", ((p.color4 shr 8) and 0xFF).toFloat())
                    onSlider("voronoiColor4B", (p.color4 and 0xFF).toFloat())
                    onSlider("voronoiColorGlowA", ((p.glow ushr 24) and 0xFF).toFloat())
                    onSlider("voronoiColorGlowR", ((p.glow shr 16) and 0xFF).toFloat())
                    onSlider("voronoiColorGlowG", ((p.glow shr 8) and 0xFF).toFloat())
                    onSlider("voronoiColorGlowB", (p.glow and 0xFF).toFloat())
                    onSlider("voronoiColorGapA", ((p.gap ushr 24) and 0xFF).toFloat())
                    onSlider("voronoiColorGapR", ((p.gap shr 16) and 0xFF).toFloat())
                    onSlider("voronoiColorGapG", ((p.gap shr 8) and 0xFF).toFloat())
                    onSlider("voronoiColorGapB", (p.gap and 0xFF).toFloat())
                    onSlider("voronoiDistortion", p.distortion)
                    onSlider("voronoiGap", p.gapSize)
                    onSlider("voronoiGlow", p.glowStrength)
                    onSlider("voronoiScale", p.scale)
                    onSlider("voronoiSpeed", p.speed)
                    onSlider("voronoiRotation", 0f)
                    onSlider("voronoiOffsetX", 0f)
                    onSlider("voronoiOffsetY", 0f)
                }
            }
            V9SliderRow("颜色数量", "1~5", state.voronoiColorCount.toFloat(), 1f, 5f, { "${it.toInt()}" }) {
                onSegment("voronoiColorCount", it.toInt())
            }
            V9SliderRow("颜色步进", "1~3", state.voronoiStepsPerColor.toFloat(), 1f, 3f, { "${it.toInt()}" }) {
                onSegment("voronoiStepsPerColor", it.toInt())
            }
            V9SliderRow("扭曲", "0~0.5", state.voronoiDistortion, 0f, 0.5f, { "%.2f".format(it) }) {
                onSlider("voronoiDistortion", it)
            }
            V9SliderRow("间隙", "0~0.1", state.voronoiGap, 0f, 0.1f, { "%.3f".format(it) }) {
                onSlider("voronoiGap", it)
            }
            V9SliderRow("内部辉光", "0~1.0", state.voronoiGlow, 0f, 1f, { "%.2f".format(it) }) {
                onSlider("voronoiGlow", it)
            }
            V9SliderRow("缩放", "0.05~4.0", state.voronoiScale, 0.05f, 4f, { "%.2f".format(it) }) {
                onSlider("voronoiScale", it)
            }
            V9SliderRow("视野", "1.0~16.0", state.voronoiFov, 1f, 16f, { "%.1f".format(it) }) {
                onSlider("voronoiFov", it)
            }
            V9SliderRow("动画速度", "0~3.0", state.voronoiSpeed, 0f, 3f, { "%.2f".format(it) }) {
                onSlider("voronoiSpeed", it)
            }
            V9SliderRow("旋转", "-360~360°", state.voronoiRotation, -6.283f, 6.283f, { "%.0f°".format(it * 57.29578f) }) {
                onSlider("voronoiRotation", it)
            }
            V9SliderRow("横向偏移", "-1~1", state.voronoiOffsetX, -1f, 1f, { "%.2f".format(it) }) {
                onSlider("voronoiOffsetX", it)
            }
            V9SliderRow("纵向偏移", "-1~1", state.voronoiOffsetY, -1f, 1f, { "%.2f".format(it) }) {
                onSlider("voronoiOffsetY", it)
            }
            val colorSlots = listOf(
                Triple("颜色 1", state.voronoiColor0, "voronoiColor0"),
                Triple("颜色 2", state.voronoiColor1, "voronoiColor1"),
                Triple("颜色 3", state.voronoiColor2, "voronoiColor2"),
                Triple("颜色 4", state.voronoiColor3, "voronoiColor3"),
                Triple("颜色 5", state.voronoiColor4, "voronoiColor4")
            )
            colorSlots.forEach { (label, color, prefix) ->
                Text(label, fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                V9ColorSlider("R", (color shr 16) and 0xFF) { onSlider("${prefix}R", it.toFloat()) }
                V9ColorSlider("G", (color shr 8) and 0xFF) { onSlider("${prefix}G", it.toFloat()) }
                V9ColorSlider("B", color and 0xFF) { onSlider("${prefix}B", it.toFloat()) }
                V9ColorSlider("A", (color ushr 24) and 0xFF) { onSlider("${prefix}A", it.toFloat()) }
            }
            Text("内部辉光色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            V9ColorSlider("R", (state.voronoiColorGlow shr 16) and 0xFF) { onSlider("voronoiColorGlowR", it.toFloat()) }
            V9ColorSlider("G", (state.voronoiColorGlow shr 8) and 0xFF) { onSlider("voronoiColorGlowG", it.toFloat()) }
            V9ColorSlider("B", state.voronoiColorGlow and 0xFF) { onSlider("voronoiColorGlowB", it.toFloat()) }
            V9ColorSlider("A", (state.voronoiColorGlow ushr 24) and 0xFF) { onSlider("voronoiColorGlowA", it.toFloat()) }
            Text("间隙颜色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            V9ColorSlider("R", (state.voronoiColorGap shr 16) and 0xFF) { onSlider("voronoiColorGapR", it.toFloat()) }
            V9ColorSlider("G", (state.voronoiColorGap shr 8) and 0xFF) { onSlider("voronoiColorGapG", it.toFloat()) }
            V9ColorSlider("B", state.voronoiColorGap and 0xFF) { onSlider("voronoiColorGapB", it.toFloat()) }
            V9ColorSlider("A", (state.voronoiColorGap ushr 24) and 0xFF) { onSlider("voronoiColorGapA", it.toFloat()) }
        }
    }
}

@Composable
private fun V9SlashPanel(
    modifier: Modifier,
    state: V9State,
    uiTabVariant: Int,
    uiTabSize: Int,
    uiTabCompact: Boolean,
    onToggle: (String) -> Unit,
    onSlider: (String, Float) -> Unit,
    onSegment: (String, Int) -> Unit,
    onReset: (String) -> Unit
) {
    var slashTab by remember { mutableStateOf("基础") }
    Column(
        modifier.background(Brush.verticalGradient(listOf(Color(0xFF13161D), Color(0xFF0F1217))))
            .drawBehind {
                drawLine(Color(0x24E9ECF2), Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("斩击设置", fontSize = 14.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont)
        Text("左键命中目标时在命中点播放空间斩", fontSize = 11.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        V9LobeTabs(
            items = listOf("基础", "外观", "动画", "星空"),
            selected = slashTab,
            onSelect = { slashTab = it },
            variant = uiTabVariant,
            size = uiTabSize,
            compact = uiTabCompact,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        when (slashTab) {
            "外观" -> {
                Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("斩击外观") }.padding(bottom = 4.dp))
                V9SliderRow("黑芯宽度", "0.30~0.98", state.slashCoreWidth, 0.3f, 0.98f, { "%.2f".format(it) }) { onSlider("slashCoreWidth", it) }
                V9SliderRow("黑芯亮度", "0~100%", state.slashCoreShade, 0f, 1f, { "%.0f%%".format(it * 100f) }) { onSlider("slashCoreShade", it) }
                V9SliderRow("白边起始", "0.50~0.99", state.slashEdgeWidth, 0.5f, 0.99f, { "%.2f".format(it) }) { onSlider("slashEdgeWidth", it) }
                V9SliderRow("白边亮度", "0.1~2.0", state.slashEdgeBrightness, 0.1f, 2f, { "%.2f".format(it) }) { onSlider("slashEdgeBrightness", it) }
                V9SliderRow("发光宽度", "0.60~0.99", state.slashGlowWidth, 0.6f, 0.99f, { "%.2f".format(it) }) { onSlider("slashGlowWidth", it) }
                V9SliderRow("发光强度", "0~3.0", state.slashGlowStrength, 0f, 3f, { "%.2f".format(it) }) { onSlider("slashGlowStrength", it) }
                V9SliderRow("裂纹噪点", "0~2.0", state.slashNoiseStrength, 0f, 2f, { "%.2f".format(it) }) { onSlider("slashNoiseStrength", it) }
                Text("黑芯颜色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                V9ColorSlider("R", (state.slashCoreColor shr 16) and 0xFF) { onSlider("slashCoreColorR", it.toFloat()) }
                V9ColorSlider("G", (state.slashCoreColor shr 8) and 0xFF) { onSlider("slashCoreColorG", it.toFloat()) }
                V9ColorSlider("B", state.slashCoreColor and 0xFF) { onSlider("slashCoreColorB", it.toFloat()) }
                Text("白边颜色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                V9ColorSlider("R", (state.slashEdgeColor shr 16) and 0xFF) { onSlider("slashEdgeColorR", it.toFloat()) }
                V9ColorSlider("G", (state.slashEdgeColor shr 8) and 0xFF) { onSlider("slashEdgeColorG", it.toFloat()) }
                V9ColorSlider("B", state.slashEdgeColor and 0xFF) { onSlider("slashEdgeColorB", it.toFloat()) }
                Text("发光颜色", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                V9ColorSlider("R", (state.slashGlowColor shr 16) and 0xFF) { onSlider("slashGlowColorR", it.toFloat()) }
                V9ColorSlider("G", (state.slashGlowColor shr 8) and 0xFF) { onSlider("slashGlowColorG", it.toFloat()) }
                V9ColorSlider("B", state.slashGlowColor and 0xFF) { onSlider("slashGlowColorB", it.toFloat()) }
            }
            "动画" -> {
                Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("斩击动画") }.padding(bottom = 4.dp))
                V9SliderRow("扫开速度", "0.05~0.95", state.slashSweepSpeed, 0.05f, 0.95f, { "%.2f".format(it) }) { onSlider("slashSweepSpeed", it) }
                V9SliderRow("扫开柔和度", "0.02~0.60", state.slashSweepSoftness, 0.02f, 0.6f, { "%.2f".format(it) }) { onSlider("slashSweepSoftness", it) }
                V9SliderRow("保持时长", "0~0.8", state.slashHoldFraction, 0f, 0.8f, { "%.2f".format(it) }) { onSlider("slashHoldFraction", it) }
                V9SliderRow("淡出起始", "0.30~0.95", state.slashFadeStart, 0.3f, 0.95f, { "%.2f".format(it) }) { onSlider("slashFadeStart", it) }
                V9SliderRow("淡出时长", "0.02~0.70", state.slashFadeDuration, 0.02f, 0.7f, { "%.2f".format(it) }) { onSlider("slashFadeDuration", it) }
                V9SliderRow("命中点外移", "0~0.5", state.slashSurfaceOffset, 0f, 0.5f, { "%.2f".format(it) }) { onSlider("slashSurfaceOffset", it) }
                V9SliderRow("随机倾角", "0~360°", state.slashRollRange, 0f, 360f, { "%.0f".format(it) }) { onSlider("slashRollRange", it) }
            }
            "星空" -> {
                Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("斩击星空") }.padding(bottom = 4.dp))
                V9SwitchRow("星空开关", "刀身黑芯内显示星点", state.slashStarfield) { onToggle("slashStarfield") }
                V9SliderRow("星点密度", "0.05~3.0", state.slashStarDensity, 0.05f, 3f, { "%.2f".format(it) }) { onSlider("slashStarDensity", it) }
                V9SliderRow("星点亮度", "0~3.0", state.slashStarBrightness, 0f, 3f, { "%.2f".format(it) }) { onSlider("slashStarBrightness", it) }
                V9SliderRow("星点大小", "0.2~5.0", state.slashStarSize, 0.2f, 5f, { "%.2f".format(it) }) { onSlider("slashStarSize", it) }
                V9SegmentRow("颜色模式", "黑白或彩虹", listOf("黑白", "彩虹"), state.slashStarColorMode) { onSegment("slashStarColorMode", it) }
            }
            else -> {
                Text("恢复默认", fontSize = 11.sp, color = Color(0xFFFF7A5C), fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("斩击基础") }.padding(bottom = 4.dp))
                V9SwitchRow("斩击开关", "左键命中时播放空间斩", state.slashEnabled) { onToggle("slashEnabled") }
                V9SwitchRow("深度遮挡", "开启后被怪物/世界遮挡", state.slashDepthTest) { onToggle("slashDepthTest") }
                V9SwitchRow("随机偏角", "正对视线时随机角度", state.slashRandomAngle) { onToggle("slashRandomAngle") }
                V9SwitchRow("加法发光", "白边外发光层", state.slashGlow) { onToggle("slashGlow") }
                V9SliderRow("时长", "0.1~1.0 秒", state.slashDuration, 0.1f, 1f, { "%.2f".format(it) }) { onSlider("slashDuration", it) }
                V9SliderRow("长度倍率", "0.5~5.0", state.slashLengthMult, 0.5f, 5f, { "%.2f".format(it) }) { onSlider("slashLengthMult", it) }
                V9SliderRow("刀宽比例", "0.02~0.50", state.slashWidthRatio, 0.02f, 0.5f, { "%.3f".format(it) }) { onSlider("slashWidthRatio", it) }
                V9SliderRow("厚度比例", "0.002~0.20", state.slashThicknessRatio, 0.002f, 0.2f, { "%.3f".format(it) }) { onSlider("slashThicknessRatio", it) }
                V9SliderRow("尖端淡出", "0.02~0.50", state.slashTipFade, 0.02f, 0.5f, { "%.2f".format(it) }) { onSlider("slashTipFade", it) }
                V9SliderRow("起始缩放", "0.2~3.0", state.slashStartScale, 0.2f, 3f, { "%.2f".format(it) }) { onSlider("slashStartScale", it) }
                V9SliderRow("结束缩放", "0.2~3.0", state.slashEndScale, 0.2f, 3f, { "%.2f".format(it) }) { onSlider("slashEndScale", it) }
            }
        }
    }
}

@Composable
private fun V9Label(label: String, desc: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont)
        Text(desc, fontSize = 10.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont, maxLines = 2)
    }
}

@Composable
private fun V9SwitchRow(label: String, desc: String, value: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9Label(label, desc, Modifier.weight(1f))
        V9Switch(value, onToggle)
    }
}

@Composable
private fun V9Switch(value: Boolean, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val prefs = LocalV9UiPrefs.current
    if (prefs.switchStyle == 2) {
        val switchSize = prefs.switchHeight
        val trackW = prefs.switchWidth
        val knobD = prefs.switchKnob
        val knobY = (switchSize - knobD) / 2f
        val offKnobX by animateDpAsState(
            targetValue = if (value) -knobD - 4.dp else 3.dp,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "slideOff"
        )
        val onKnobX by animateDpAsState(
            targetValue = if (value) trackW - knobD - 3.dp else trackW + 4.dp,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "slideOn"
        )
        Box(
            Modifier.width(trackW).height(switchSize)
                .shadow(2.dp, RoundedCornerShape(switchSize), ambientColor = Color(0x26000000), spotColor = Color(0x26000000))
                .clip(RoundedCornerShape(switchSize))
                .background(Color(prefs.switchSlideTrack))
                .border(1.dp, Color(0x1A000000), RoundedCornerShape(switchSize))
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
        ) {
            Box(
                Modifier.offset(x = offKnobX, y = knobY).width(knobD).height(knobD)
                    .clip(CircleShape)
                    .background(Color(prefs.switchSlideOff))
            )
            Box(
                Modifier.offset(x = onKnobX, y = knobY).width(knobD).height(knobD)
                    .clip(CircleShape)
                    .background(Color(prefs.switchSlideOn))
            )
        }
    } else if (prefs.switchStyle == 3) {
        val switchSize = prefs.switchHeight
        val trackW = prefs.switchWidth
        val knobD = prefs.switchKnob
        val knobY = (switchSize - knobD) / 2f
        val knobX by animateDpAsState(
            targetValue = if (value) trackW - knobD - 1.dp else 1.dp,
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            label = "textKnob"
        )
        val offY by animateDpAsState(
            targetValue = if (value) -switchSize else 0.dp,
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            label = "textOffY"
        )
        val onY by animateDpAsState(
            targetValue = if (value) 0.dp else switchSize,
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            label = "textOnY"
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(trackW).height(switchSize)
                    .clip(RoundedCornerShape(switchSize))
                    .background(if (value) Color(prefs.switchTextTrackOn) else Color(prefs.switchTextTrackOff))
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            ) {
                Box(
                    Modifier.offset(x = knobX, y = knobY).width(knobD).height(knobD)
                        .clip(CircleShape)
                        .background(Color(prefs.switchTextKnob))
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.width(40.dp).height(switchSize)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onToggle)
            ) {
                Text(
                    prefs.switchTextOff,
                    fontSize = 11.sp,
                    color = Color(prefs.switchTextColor),
                    fontFamily = cnFont,
                    maxLines = 1,
                    modifier = Modifier.offset(y = offY)
                )
                Text(
                    prefs.switchTextOn,
                    fontSize = 11.sp,
                    color = Color(prefs.switchTextColor),
                    fontFamily = cnFont,
                    maxLines = 1,
                    modifier = Modifier.offset(y = onY)
                )
            }
        }
    } else if (prefs.switchStyle == 1) {
        val switchSize = prefs.switchHeight
        val trackW = prefs.switchWidth
        val knobD = prefs.switchKnob
        val knobY = (switchSize - knobD) / 2f
        val knobX by animateDpAsState(
            targetValue = if (value) trackW - knobD - 3.dp else 3.dp,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            label = "gradientKnob"
        )
        val decorReveal by animateFloatAsState(
            targetValue = if (value) 1f else 0f,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            label = "decorReveal"
        )
        Box(
            Modifier.width(trackW).height(switchSize)
                .shadow(3.dp, RoundedCornerShape(switchSize), ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
                .clip(RoundedCornerShape(switchSize))
                .then(
                    if (value) {
                        Modifier.background(Brush.horizontalGradient(listOf(Color(prefs.switchGradientA), Color(prefs.switchGradientB))))
                    } else {
                        Modifier.background(if (hovered) Color(prefs.switchTrackOff).copy(alpha = 0.85f) else Color(prefs.switchTrackOff))
                    }
                )
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val dotColor = if (value) Color(0xFFFFFFFF) else Color(0xFF9B9B9B)
                val unit = this.size.minDimension
                val dotY = unit * (if (value) 0.52f else 0.5f)
                drawCircle(dotColor, radius = unit * 0.04f, center = Offset(unit * 0.24f, dotY), alpha = 1f - decorReveal)
                drawCircle(dotColor, radius = unit * 0.04f, center = Offset(unit * 0.36f, dotY), alpha = 1f - decorReveal)
                drawCircle(dotColor, radius = unit * 0.08f, center = Offset(unit * 0.76f, dotY), alpha = decorReveal)
            }
            Box(
                Modifier.offset(x = knobX, y = knobY).width(knobD).height(knobD)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFDEDEDE), Color(0xFFFFFFFF))))
                    .border(1.dp, Color(0x1FFFFFFF), CircleShape)
            )
        }
    } else {
        val knobX by animateDpAsState(
            targetValue = if (value) 32.dp else 4.dp,
            animationSpec = tween(180, easing = FastOutSlowInEasing),
            label = "knob"
        )
        Box(
            Modifier.width(54.dp).height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (value) Color(0xFFFF7A5C) else if (hovered) Color(0xFF2A2F38) else Color(0xFF242A33))
                .drawBehind {
                    drawLine(Color(0x30FFFFFF), Offset(4f, 2f), Offset(size.width - 4f, 2f), 1f)
                }
                .border(1.dp, if (value) Color(0x40FFA08A) else Color(0x14FFFFFF), RoundedCornerShape(14.dp))
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("on", fontSize = 9.sp, color = if (value) Color(0xFFFFFFFF) else Color(0xFF646D7B), fontFamily = cnFont, maxLines = 1)
                Text("off", fontSize = 9.sp, color = if (!value) Color(0xFF9AA2B0) else Color(0xFF646D7B), fontFamily = cnFont, maxLines = 1)
            }
            Box(
                Modifier.offset(x = knobX, y = 5.dp).width(18.dp).height(18.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE6E9EF))))
                    .border(1.dp, Color(0x2EFFFFFF), CircleShape)
            )
        }
    }
}

@Composable
private fun V9SegmentRow(label: String, desc: String, options: List<String>, selected: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9Label(label, desc, Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF0F1116)).padding(3.dp)) {
            options.forEachIndexed { i, opt ->
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(if (i == selected) Color(0x29FF7A5C) else Color.Transparent)
                        .clickable { onChange(i) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(opt, fontSize = 12.sp, color = if (i == selected) Color(0xFFFFA08A) else Color(0xFF9AA2B0), fontFamily = cnFont)
                }
            }
        }
    }
}

@Composable
private fun V9SliderRow(
    label: String,
    desc: String,
    value: Float,
    min: Float,
    max: Float,
    format: (Float) -> String,
    onChange: (Float) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9Label(label, desc, Modifier.weight(1f))
        val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
        FaceIcon(active = fraction < 0.5f, smile = false)
        Spacer(Modifier.width(6.dp))
        V9SliderTrack(value, min, max, onChange, Modifier.weight(1f))
        Spacer(Modifier.width(6.dp))
        FaceIcon(active = fraction >= 0.5f, smile = true)
        Spacer(Modifier.width(8.dp))
        V9EditableValue(format(value), value, min, max, onChange)
    }
}

@Composable
private fun V9TextInputRow(label: String, desc: String, value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9Label(label, desc, Modifier.weight(1f))
        Box(
            Modifier.width(120.dp).height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF12151B))
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFFD7DCE5), fontFamily = cnFont),
                cursorBrush = SolidColor(Color(0xFFFF7A5C)),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun V9SliderTrack(
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = LocalV9UiPrefs.current
    val density = LocalDensity.current
    val thumbR = with(density) { prefs.sliderThumb.toPx() / 2f }

    fun pick(x: Float, widthPx: Int): Float {
        val usable = (widthPx - thumbR * 2).coerceAtLeast(1f)
        val f = ((x - thumbR) / usable).coerceIn(0f, 1f)
        return min + f * (max - min)
    }

    Canvas(
        modifier.height(prefs.sliderHeight)
            .pointerInput(min, max) {
                detectTapGestures { offset ->
                    onChange(pick(offset.x, size.width))
                }
            }
            .pointerInput(min, max) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange(pick(change.position.x, size.width))
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val trackH = 6.dp.toPx()
        val trackY = h / 2 - trackH / 2
        val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
        val corner = CornerRadius(trackH / 2)

        drawRoundRect(
            Color(0xFF242A33),
            topLeft = Offset(0f, trackY),
            size = Size(w, trackH),
            cornerRadius = corner
        )
        val fillW = w * fraction
        if (fillW > 0) {
            drawRoundRect(
                Color(0xFFFF7A5C),
                topLeft = Offset(0f, trackY),
                size = Size(fillW, trackH),
                cornerRadius = corner
            )
        }
        val thumbX = thumbR + (w - thumbR * 2) * fraction
        drawCircle(Color(0xFFF4F6FA), radius = thumbR, center = Offset(thumbX, h / 2))
        drawCircle(
            Color(0xFF3A3F49), radius = thumbR,
            center = Offset(thumbX, h / 2), style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun V9EditableValue(
    display: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var hadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    fun commit() {
        val parsed = text.trim().toFloatOrNull()
        if (parsed != null && parsed.isFinite()) {
            onChange(parsed)
        }
        hadFocus = false
        editing = false
    }
    Box(
        Modifier.animateContentSize(tween(180, easing = FastOutSlowInEasing))
    ) {
        AnimatedContent(
            targetState = editing,
            transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(140)) },
            label = "valueEdit"
        ) { isEditing ->
            if (isEditing) {
                Box(
                    Modifier.width(88.dp).height(22.dp).clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFF12151B))
                        .border(1.dp, Color(0xFFFF7A5C), RoundedCornerShape(11.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.sp, color = Color(0xFFD7DCE5), fontFamily = cnFont),
                        cursorBrush = SolidColor(Color(0xFFFF7A5C)),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        keyboardActions = KeyboardActions(onDone = { commit() }),
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) hadFocus = true
                                else if (hadFocus) commit()
                            }
                    )
                }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                Row(
                    Modifier.height(22.dp).clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFF1A1E26))
                        .border(1.dp, Color(0x14E9ECF2), RoundedCornerShape(11.dp))
                        .padding(horizontal = 8.dp)
                        .clickable {
                            text = value.toString()
                            hadFocus = false
                            editing = true
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(display, fontSize = 11.sp, color = Color(0xFFD7DCE5), fontFamily = cnFont, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FaceIcon(active: Boolean, smile: Boolean) {
    Canvas(Modifier.width(14.dp).height(14.dp)) {
        val c = if (active) Color(0xFFFF7A5C) else Color(0xFF646D7B)
        val r = size.minDimension / 2f
        drawCircle(c, radius = r, center = center, style = Stroke(width = 1.2f))
        drawCircle(c, radius = r * 0.12f, center = Offset(center.x - r * 0.4f, center.y - r * 0.25f))
        drawCircle(c, radius = r * 0.12f, center = Offset(center.x + r * 0.4f, center.y - r * 0.25f))
        val mouth = Path().apply {
            if (smile) {
                moveTo(center.x - r * 0.45f, center.y + r * 0.1f)
                quadraticTo(center.x, center.y + r * 0.55f, center.x + r * 0.45f, center.y + r * 0.1f)
            } else {
                moveTo(center.x - r * 0.45f, center.y + r * 0.45f)
                quadraticTo(center.x, center.y + r * 0.1f, center.x + r * 0.45f, center.y + r * 0.45f)
            }
        }
        drawPath(mouth, c, style = Stroke(width = 1.2f))
    }
}

@Composable
private fun V9SwatchRow(
    label: String,
    desc: String,
    selected: Int,
    custom: Boolean,
    customColor: Int,
    onChange: (Int) -> Unit,
    onCustomColor: (Int) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            V9Label(label, desc, Modifier.weight(1f))
            ballColors.forEachIndexed { i, c ->
                Box(
                    Modifier.width(18.dp).height(18.dp)
                        .border(
                            if (!custom && i == selected) 2.dp else 1.dp,
                            if (!custom && i == selected) Color(0xFFF2F4F8) else Color(0x38E9ECF2),
                            CircleShape
                        )
                        .background(Color(c), CircleShape)
                        .clickable { onChange(i) }
                )
                Spacer(Modifier.width(4.dp))
            }
            Box(
                Modifier.width(18.dp).height(18.dp)
                    .border(if (custom) 2.dp else 1.dp, if (custom) Color(0xFFF2F4F8) else Color(0x38E9ECF2), CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFF7A5C), Color(0xFF7C5CFF), Color(0xFF3DD6E8))),
                        CircleShape
                    )
                    .clickable { onCustomColor(customColor) }
            )
        }
        if (custom) {
            Spacer(Modifier.height(2.dp))
            V9ColorSlider("R", (customColor shr 16) and 0xFF) { v ->
                onCustomColor((customColor and 0xFF00FFFF.toInt()) or (v shl 16))
            }
            V9ColorSlider("G", (customColor shr 8) and 0xFF) { v ->
                onCustomColor((customColor and 0xFFFF00FF.toInt()) or (v shl 8))
            }
            V9ColorSlider("B", customColor and 0xFF) { v ->
                onCustomColor((customColor and 0xFFFFFF00.toInt()) or v)
            }
        }
    }
}

@Composable
private fun V9ColorSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 28.dp).padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont, modifier = Modifier.width(14.dp))
        V9SliderTrack(value.toFloat(), 0f, 255f, { onChange(it.toInt()) }, Modifier.weight(1f))
        Spacer(Modifier.width(6.dp))
        V9EditableValue(value.toString(), value.toFloat(), 0f, 255f) { onChange(it.toInt().coerceIn(0, 255)) }
    }
}

@Composable
private fun V9PresetRow(presetSlot: Int, onLoad: (Int) -> Unit, onSave: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9Label("配置预设", "槽 1-3 保存/载入", Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF0F1116)).padding(3.dp)) {
            (0..2).forEach { i ->
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(if (i == presetSlot) Color(0x29FF7A5C) else Color.Transparent)
                        .clickable { onLoad(i) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(
                        "槽${i + 1}",
                        fontSize = 12.sp,
                        color = if (i == presetSlot) Color(0xFFFFA08A) else Color(0xFF9AA2B0),
                        fontFamily = cnFont
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        V9PillButton("存入", primary = true, onClick = onSave)
    }
}

@Composable
private fun V9Preview(
    modifier: Modifier,
    grayScreen: Boolean,
    startDuration: Float,
    endDuration: Float,
    ballColor: Long,
    onStageBounds: (Rect) -> Unit
) {
    Column(modifier.padding(12.dp)) {
        Text("预览", fontSize = 13.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    onStageBounds(
                        Rect(
                            pos.x,
                            pos.y,
                            pos.x + coords.size.width.toFloat(),
                            pos.y + coords.size.height.toFloat()
                        )
                    )
                }
                .background(Brush.verticalGradient(listOf(Color(0xFF0D1016), Color(0xFF07090D))), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x1FE9ECF2), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            PreviewGlow(ballColor)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            PhaseLabel("启动")
            PhaseLabel("保持")
            PhaseLabel("关闭")
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))) {
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier.weight(0.30f).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFF8A6E), Color(0xFFE86848))))
                )
                Box(Modifier.weight(0.48f).fillMaxHeight().background(Color(0xFF242A33)))
                Box(
                    Modifier.weight(0.22f).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Color(0xFFE86848), Color(0xFFFF8A6E))))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            ValueCell(String.format(java.util.Locale.ROOT, "%.1fs", startDuration), Color(0xFFD7DCE5))
            ValueCell("持续", Color(0xFF9AA2B0))
            ValueCell(String.format(java.util.Locale.ROOT, "%.1fs", endDuration), Color(0xFFD7DCE5))
        }
    }
}

@Composable
private fun RowScope.PhaseLabel(text: String) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 10.sp, color = Color(0xFF9AA2B0), fontFamily = cnFont, maxLines = 1)
    }
}

@Composable
private fun RowScope.ValueCell(text: String, color: Color) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 10.sp, color = color, fontFamily = cnFont, maxLines = 1)
    }
}

@Composable
private fun PreviewGlow(ballColor: Long) {
    Canvas(Modifier.fillMaxSize().padding(12.dp)) {
        val accent = Color(ballColor)
        drawCircle(
            Brush.radialGradient(listOf(accent.copy(alpha = 0.26f), Color(0x00FF7A5C))),
            radius = size.minDimension * 0.45f,
            center = center
        )
    }
}

@Composable
private fun V9Footer(quote: String, onReset: () -> Unit, onCancel: () -> Unit, onSave: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(66.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF101319), Color(0xFF0D0F14))))
            .drawBehind { drawLine(Color(0x24E9ECF2), Offset(0f, 0f), Offset(size.width, 0f), 1f) }
    ) {
        Row(
            Modifier.align(Alignment.CenterStart).padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(quote, fontSize = 12.sp, color = Color(0xFFD7DCE5), fontFamily = cnFont, maxLines = 1)
        }
        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(72.dp).height(2.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color(0x0DFFFFFF), Color(0x26FF7A5C)))
                )
            )
            Spacer(Modifier.width(12.dp))
            Row(
                Modifier.height(36.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF161A22), Color(0xFF101319))))
                    .border(1.dp, Color(0x24E9ECF2), RoundedCornerShape(14.dp))
                    .drawBehind {
                        drawLine(Color(0x24FFFFFF), Offset(8f, 3f), Offset(size.width - 8f, 3f), 1f)
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(9.dp).height(9.dp).rotate(45f).background(Color(0xFFFF7A5C), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text("星渊", fontSize = 12.sp, color = Color(0xFFD7DCE5), fontFamily = cnFont)
                Spacer(Modifier.width(6.dp))
                Text("THESEUS", fontSize = 11.sp, color = Color(0xFF646D7B), fontFamily = cnFont)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.width(72.dp).height(2.dp).background(
                    Brush.horizontalGradient(listOf(Color(0x26FF7A5C), Color(0x0DFFFFFF), Color.Transparent))
                )
            )
        }
        Row(
            Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V9PillButton("恢复默认", primary = false, onClick = onReset)
            Spacer(Modifier.width(6.dp))
            V9PillButton("取消", primary = false, onClick = onCancel)
            Spacer(Modifier.width(6.dp))
            V9PillButton("保存更改", primary = true, onClick = onSave)
        }
    }
}

@Composable
private fun V9PillButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    primary && hovered -> Color(0xFFFFA08A)
                    primary -> Color(0xFFFF7A5C)
                    hovered -> Color(0xFF2A2F38)
                    else -> Color(0xFF20242C)
                }
            )
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .drawBehind {
                if (primary) {
                    drawLine(Color(0x2EFFFFFF), Offset(6f, 4f), Offset(size.width - 6f, 4f), 1f)
                }
            }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = if (primary) Color(0xFF1B100C) else Color(0xFF9AA2B0), fontFamily = cnFont)
    }
}

@Composable
private fun V9RewindPanel(
    modifier: Modifier,
    state: V9State,
    onToggle: (String) -> Unit,
    onSegment: (String, Int) -> Unit,
    onSlider: (String, Float) -> Unit,
    onReset: (String) -> Unit
) {
    Column(
        modifier.background(Brush.verticalGradient(listOf(Color(0xFF13161D), Color(0xFF0F1217)))).drawBehind {
            drawLine(Color(0x24E9ECF2), Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
        }.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("回溯设置", fontSize = 14.sp, color = Color(0xFFF2F4F8), fontFamily = cnFont)
        Text("跟随这把神剑保存", fontSize = 11.sp, color = Color(0xFF646D7B), fontFamily = cnFont)

        V9SwitchRow("回溯开关", "启用时间回溯能力", state.rewindEnabled) { onToggle("rewindEnabled") }
        V9SwitchRow("死亡回溯", "死亡时自动回溯", state.rewindDeathEnabled) { onToggle("rewindDeathEnabled") }

        V9SliderRow("回溯窗口", "回退到多少秒前", state.rewindWindowSeconds.toFloat(), 1f, 60f, { "${it.toInt()}s" }) { onSlider("rewindWindowSeconds", it) }
        V9SliderRow("主动冷却", "主动回溯冷却秒数", state.rewindCooldownTicks / 20f, 0f, 300f, { String.format("%.1fs", it) }) { onSlider("rewindCooldownTicks", it * 20f) }
        V9SliderRow("死亡冷却", "死亡回溯冷却秒数", state.rewindDeathCooldownTicks / 20f, 0f, 300f, { String.format("%.1fs", it) }) { onSlider("rewindDeathCooldownTicks", it * 20f) }
        V9SliderRow("连续回溯上限", "0 表示直到窗口开头", state.rewindDeathMaxRetries.toFloat(), 0f, 10f, { it.toInt().toString() }) { onSlider("rewindDeathMaxRetries", it) }

        V9SegmentRow("范围模式", "全维度或当前维度", listOf("全维度", "当前维度"), state.rewindScopeMode) { onSegment("rewindScopeMode", it) }
        V9SegmentRow("区块模式", "全区块或半径", listOf("全区块", "半径"), state.rewindScope) { onSegment("rewindScope", it) }
        V9SliderRow("半径", "半径模式的半径", state.rewindRadius, 8f, 256f, { "${it.toInt()} 格" }) { onSlider("rewindRadius", it) }

        V9SegmentRow("还原方式", "渐进或瞬间", listOf("渐进", "瞬间"), state.rewindPlaybackMode) { onSegment("rewindPlaybackMode", it) }
        V9SliderRow("播放时长", "渐进还原播放秒数", state.rewindPlaybackSeconds, 0.5f, 5f, { String.format("%.1fs", it) }) { onSlider("rewindPlaybackSeconds", it) }
        V9SegmentRow("还原顺序", "先方块或先实体", listOf("方块→实体→世界", "实体→方块"), state.rewindRestoreOrder) { onSegment("rewindRestoreOrder", it) }
        V9SegmentRow("镜头模式", "跟随或自由飞行", listOf("跟随角色", "自由飞行"), state.rewindCameraMode) { onSegment("rewindCameraMode", it) }
        V9SwitchRow("自由镜头回位置", "B 模式结束回到原位置", state.rewindFreeCamRestorePosition) { onToggle("rewindFreeCamRestorePosition") }
        V9SwitchRow("还原统计", "回溯结束显示还原数量提示", state.rewindShowStats) { onToggle("rewindShowStats") }

        V9SwitchRow("玩家状态回退", "生命/饥饿/经验/背包等", state.rewindPlayerState) { onToggle("rewindPlayerState") }
        V9SwitchRow("主动位置回退", "主动回溯结束时回位置", state.rewindPositionRewind) { onToggle("rewindPositionRewind") }
        V9SegmentRow("回位方式", "瞬间或平滑", listOf("瞬间", "平滑"), state.rewindPositionMode) { onSegment("rewindPositionMode", it) }

        V9SwitchRow("安全检查点", "死亡回溯挑安全时刻", state.rewindSafetyCheckpoint) { onToggle("rewindSafetyCheckpoint") }
        V9SwitchRow("敌对检测", "安全检查点检测附近敌对", state.rewindHostileCheck) { onToggle("rewindHostileCheck") }
        V9SwitchRow("扣除队友拿走", "防复制扣除其他玩家拿走物品", state.rewindOtherItemDeduct) { onToggle("rewindOtherItemDeduct") }
        V9SwitchRow("播放冻结队友", "播放期间其他玩家冻结", state.rewindFreezeOthers) { onToggle("rewindFreezeOthers") }
        V9SwitchRow("时停叠加", "允许回溯与时停叠加", state.rewindTimestopStacking) { onToggle("rewindTimestopStacking") }

        V9SwitchRow("方块", "恢复方块状态", state.rewindBlocks) { onToggle("rewindBlocks") }
        V9SwitchRow("方块实体", "恢复箱子/熔炉等 NBT", state.rewindBlockEntities) { onToggle("rewindBlockEntities") }
        V9SwitchRow("实体", "恢复实体位置/血量/复活", state.rewindEntities) { onToggle("rewindEntities") }
        V9SwitchRow("掉落物", "恢复掉落物", state.rewindItems) { onToggle("rewindItems") }
        V9SwitchRow("经验球", "恢复经验球", state.rewindExperience) { onToggle("rewindExperience") }
        V9SwitchRow("时间", "恢复时间", state.rewindTime) { onToggle("rewindTime") }
        V9SwitchRow("天气", "恢复天气", state.rewindWeather) { onToggle("rewindWeather") }
        V9SwitchRow("袭击", "恢复袭击", state.rewindRaids) { onToggle("rewindRaids") }
        V9SwitchRow("记分板", "恢复记分板", state.rewindScoreboard) { onToggle("rewindScoreboard") }
        V9SwitchRow("世界边界", "恢复世界边界", state.rewindWorldBorder) { onToggle("rewindWorldBorder") }

        Text("恢复默认", fontSize = 12.sp, color = Color(0xFFFFA08A), fontFamily = cnFont,
            modifier = Modifier.align(Alignment.End).clickable { onReset("回溯") }.padding(8.dp))
    }
}
