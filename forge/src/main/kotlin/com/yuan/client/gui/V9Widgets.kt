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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.lerp
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

internal fun switchValue(
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
internal fun hoverBackground(): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    return Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(if (hovered) Ui.overlayRowHover else Color.Transparent)
        .hoverable(interaction)
}


@Composable
internal fun V9Label(label: String, desc: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont)
        Text(desc, fontSize = 10.sp, color = Ui.textSecondary, fontFamily = cnFont, maxLines = 2)
    }
}


@Composable
internal fun V9SwitchRow(label: String, desc: String, value: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9Label(label, desc, Modifier.weight(1f))
        V9Switch(value, onToggle)
    }
}


@Composable
internal fun V9Switch(value: Boolean, onToggle: () -> Unit) {
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
        val pressed = isPressed(interaction)
        Box(
            Modifier.width(trackW).height(switchSize)
                .shadow(2.dp, RoundedCornerShape(switchSize), ambientColor = Ui.shadowSlight, spotColor = Ui.shadowSlight)
                .clip(RoundedCornerShape(switchSize))
                .background(
                    Color(prefs.switchSlideTrack).copy(
                        alpha = when {
                            pressed -> 0.68f
                            hovered -> 0.86f
                            else -> 1f
                        }
                    )
                )
                .border(1.dp, Ui.borderDark, RoundedCornerShape(switchSize))
                .hoverable(interaction)
                .handCursor()
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
        val pressed = isPressed(interaction)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(trackW).height(switchSize)
                    .clip(RoundedCornerShape(switchSize))
                    .background(
                        Color(if (value) prefs.switchTextTrackOn else prefs.switchTextTrackOff).copy(
                            alpha = when {
                                pressed -> 0.68f
                                hovered -> 0.86f
                                else -> 1f
                            }
                        )
                    )
                    .hoverable(interaction)
                    .handCursor()
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
                    .handCursor()
                    .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
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
                .shadow(3.dp, RoundedCornerShape(switchSize), ambientColor = Ui.shadowMedium, spotColor = Ui.shadowMedium)
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
                val dotColor = if (value) Ui.knobTop else Ui.knobOff
                val unit = this.size.minDimension
                val dotY = unit * (if (value) 0.52f else 0.5f)
                drawCircle(dotColor, radius = unit * 0.04f, center = Offset(unit * 0.24f, dotY), alpha = 1f - decorReveal)
                drawCircle(dotColor, radius = unit * 0.04f, center = Offset(unit * 0.36f, dotY), alpha = 1f - decorReveal)
                drawCircle(dotColor, radius = unit * 0.08f, center = Offset(unit * 0.76f, dotY), alpha = decorReveal)
            }
            Box(
                Modifier.offset(x = knobX, y = knobY).width(knobD).height(knobD)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Ui.knobEdge, Ui.knobTop)))
                    .border(1.dp, Ui.overlayPressed, CircleShape)
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
                .background(if (value) Ui.accent else if (hovered) Ui.surfaceHover else Ui.surface4)
                .drawBehind {
                    drawLine(Ui.innerHighlightStrong, Offset(4f, 2f), Offset(size.width - 4f, 2f), 1f)
                }
                .border(1.dp, if (value) Ui.accentHover.copy(alpha = 0.25f) else Ui.borderQuiet, RoundedCornerShape(14.dp))
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("on", fontSize = 9.sp, color = if (value) Ui.knobTop else Ui.textTertiary, fontFamily = cnFont, maxLines = 1)
                Text("off", fontSize = 9.sp, color = if (!value) Ui.textSecondary else Ui.textTertiary, fontFamily = cnFont, maxLines = 1)
            }
            Box(
                Modifier.offset(x = knobX, y = 5.dp).width(18.dp).height(18.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Ui.knobTop, Ui.knobBottom)))
                    .border(1.dp, Ui.innerHighlight, CircleShape)
            )
        }
    }
}


@Composable
internal fun V9SegmentRow(label: String, desc: String, options: List<String>, selected: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9Label(label, desc, Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Ui.surface1).padding(3.dp)) {
            options.forEachIndexed { i, opt ->
                val interaction = remember(opt) { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val pressed = isPressed(interaction)
                val isSelected = i == selected
                val reveal by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label = "segmentSelect"
                )
                val tint = when {
                    pressed -> 0.25f
                    else -> 0.16f * reveal + if (hovered && !isSelected) 0.06f else 0f
                }
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(Ui.accent.copy(alpha = tint))
                        .hoverable(interaction)
                        .handCursor()
                        .clickable(interactionSource = interaction, indication = null) { onChange(i) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(opt, fontSize = 12.sp, color = if (isSelected) Ui.accentHover else Ui.textSecondary, fontFamily = cnFont)
                }
            }
        }
    }
}


@Composable
internal fun V9SliderRow(
    label: String,
    desc: String,
    value: () -> Float,
    min: Float,
    max: Float,
    format: (Float) -> String,
    onChange: (Float) -> Unit
) {
    val value = value()   // read here, so only this row subscribes
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
internal fun V9TextInputRow(label: String, desc: String, value: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9Label(label, desc, Modifier.weight(1f))
        Box(
            Modifier.width(120.dp).height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Ui.surface2)
                .border(1.dp, Ui.borderQuiet, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = Ui.textBody, fontFamily = cnFont),
                cursorBrush = SolidColor(Ui.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Composable
internal fun V9SliderTrack(
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = LocalV9UiPrefs.current
    val density = LocalDensity.current
    val baseThumbR = with(density) { prefs.sliderThumb.toPx() / 2f }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed = isPressed(interaction)
    val emphasis by animateFloatAsState(
        targetValue = when {
            pressed -> 1f
            hovered -> 0.55f
            else -> 0f
        },
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "sliderEmphasis"
    )

    fun pick(x: Float, widthPx: Int): Float {
        val usable = (widthPx - baseThumbR * 2).coerceAtLeast(1f)
        val f = ((x - baseThumbR) / usable).coerceIn(0f, 1f)
        return min + f * (max - min)
    }

    Canvas(
        modifier.height(prefs.sliderHeight)
            .handCursor()
            .hoverable(interaction)
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
        val trackH = (6.dp.toPx() * (1f + 0.25f * emphasis))
        val trackY = h / 2 - trackH / 2
        val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
        val corner = CornerRadius(trackH / 2)

        drawRoundRect(
            lerp(Ui.surface4, Ui.surfaceHover, emphasis),
            topLeft = Offset(0f, trackY),
            size = Size(w, trackH),
            cornerRadius = corner
        )
        val fillW = w * fraction
        if (fillW > 0) {
            drawRoundRect(
                lerp(Ui.accent, Ui.accentHover, emphasis * 0.6f),
                topLeft = Offset(0f, trackY),
                size = Size(fillW, trackH),
                cornerRadius = corner
            )
        }
        val thumbR = baseThumbR * (1f + 0.18f * emphasis)
        val thumbX = baseThumbR + (w - baseThumbR * 2) * fraction
        drawCircle(Ui.thumb, radius = thumbR, center = Offset(thumbX, h / 2))
        drawCircle(
            Ui.thumbEdge, radius = thumbR,
            center = Offset(thumbX, h / 2), style = Stroke(width = 1.dp.toPx())
        )
    }
}


@Composable
internal fun V9EditableValue(
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
                        .background(Ui.surface2)
                        .border(1.dp, Ui.accent, RoundedCornerShape(11.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 11.sp, color = Ui.textBody, fontFamily = cnFont),
                        cursorBrush = SolidColor(Ui.accent),
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
                        .background(Ui.surface3)
                        .border(1.dp, Ui.hairlineFaint, RoundedCornerShape(11.dp))
                        .padding(horizontal = 8.dp)
                        .clickable {
                            text = value.toString()
                            hadFocus = false
                            editing = true
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(display, fontSize = 11.sp, color = Ui.textBody, fontFamily = cnFont, maxLines = 1)
                }
            }
        }
    }
}


@Composable
internal fun FaceIcon(active: Boolean, smile: Boolean) {
    Canvas(Modifier.width(14.dp).height(14.dp)) {
        val c = if (active) Ui.accent else Ui.textTertiary
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
internal fun V9SwatchRow(
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
                            if (!custom && i == selected) Ui.textPrimary else Ui.hairlineStrong,
                            CircleShape
                        )
                        .background(Color(c), CircleShape)
                        .clickable { onChange(i) }
                )
                Spacer(Modifier.width(4.dp))
            }
            Box(
                Modifier.width(18.dp).height(18.dp)
                    .border(if (custom) 2.dp else 1.dp, if (custom) Ui.textPrimary else Ui.hairlineStrong, CircleShape)
                    .background(
                        Brush.linearGradient(Ui.spectrum),
                        CircleShape
                    )
                    .clickable { onCustomColor(customColor) }
            )
        }
        if (custom) {
            Spacer(Modifier.height(2.dp))
            V9ColorSlider("R", { (customColor shr 16) and 0xFF } ) { v ->
                onCustomColor((customColor and 0xFF00FFFF.toInt()) or (v shl 16))
            }
            V9ColorSlider("G", { (customColor shr 8) and 0xFF } ) { v ->
                onCustomColor((customColor and 0xFFFF00FF.toInt()) or (v shl 8))
            }
            V9ColorSlider("B", { customColor and 0xFF } ) { v ->
                onCustomColor((customColor and 0xFFFFFF00.toInt()) or v)
            }
        }
    }
}


@Composable
internal fun V9ColorSlider(label: String, value: () -> Int, onChange: (Int) -> Unit) {
    val value = value()   // read here, so only this slider subscribes
    Row(
        Modifier.fillMaxWidth().heightIn(min = 28.dp).padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Ui.textSecondary, fontFamily = cnFont, modifier = Modifier.width(14.dp))
        V9SliderTrack(value.toFloat(), 0f, 255f, { onChange(it.toInt()) }, Modifier.weight(1f))
        Spacer(Modifier.width(6.dp))
        V9EditableValue(value.toString(), value.toFloat(), 0f, 255f) { onChange(it.toInt().coerceIn(0, 255)) }
    }
}


@Composable
internal fun V9PresetRow(presetSlot: Int, onLoad: (Int) -> Unit, onSave: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp).then(hoverBackground()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V9Label("配置预设", "槽 1-3 保存/载入", Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Ui.surface1).padding(3.dp)) {
            (0..2).forEach { i ->
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp))
                        .background(if (i == presetSlot) Ui.accentTint else Color.Transparent)
                        .clickable { onLoad(i) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(
                        "槽${i + 1}",
                        fontSize = 12.sp,
                        color = if (i == presetSlot) Ui.accentHover else Ui.textSecondary,
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
internal fun V9PillButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed = isPressed(interaction)
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    primary && pressed -> Ui.accentPressed
                    primary && hovered -> Ui.accentHover
                    primary -> Ui.accent
                    pressed -> Ui.surfaceActive
                    hovered -> Ui.surfaceHover
                    else -> Ui.surfaceActive
                }
            )
            .hoverable(interaction)
            .handCursor()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .drawBehind {
                if (primary) {
                    drawLine(Ui.innerHighlight, Offset(6f, 4f), Offset(size.width - 6f, 4f), 1f)
                }
            }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = if (primary) Ui.textOnAccent else Ui.textSecondary, fontFamily = cnFont)
    }
}

