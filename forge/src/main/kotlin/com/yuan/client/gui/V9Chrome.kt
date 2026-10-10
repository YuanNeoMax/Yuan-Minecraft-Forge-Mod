package com.yuan.client.gui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.item.Items

@Composable
internal fun V9SearchBox(
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
            .shadow(3.dp, RoundedCornerShape(radius), ambientColor = Ui.shadowAmbientSoft, spotColor = Ui.shadowSpotWarm)
            .clip(RoundedCornerShape(radius))
            .background(Ui.surface3)
            .border(1.dp, Ui.borderQuiet, RoundedCornerShape(radius))
            .clickable { focusRequester.requestFocus() }
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(prefs.searchHeight).height(prefs.searchHeight).padding(8.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 2.dp.toPx()
                    val c = if (iconActive) Ui.accent else Ui.textSecondary
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
                    textStyle = TextStyle(fontSize = 12.sp, color = Ui.textBody, fontFamily = cnFont),
                    cursorBrush = SolidColor(Ui.accent),
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
                                    color = Ui.textTertiary,
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
internal fun V9Header(search: String, onSearch: (String) -> Unit, dirty: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(52.dp)
            .drawBehind {
                drawLine(Ui.hairline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("星渊", fontSize = 17.sp, color = Ui.textPrimary, fontFamily = cnFont, fontWeight = FontWeight.Bold)
        Text("/", fontSize = 17.sp, color = Ui.accent, fontFamily = cnFont, fontWeight = FontWeight.Bold)
        Text("神剑", fontSize = 17.sp, color = Ui.textPrimary, fontFamily = cnFont, fontWeight = FontWeight.Bold)
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
                color = if (dirty) Ui.accent else Ui.textSecondary,
                fontFamily = cnFont
            )
            Text("· 神剑专属", fontSize = 10.sp, color = Ui.textTertiary, fontFamily = cnFont)
        }
    }
}


@Composable
internal fun V9LobeTabs(
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    state: V9Holder,
    modifier: Modifier = Modifier
) {
    // Read the tab preferences here rather than at the call site: the caller then stops subscribing
    // to them and only the tab strip recomposes when a preference changes.
    val variant = state.uiTabVariant
    val size = state.uiTabSize
    val compact = state.uiTabCompact
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
                    .background(Ui.accent.copy(alpha = 0.15f * reveal), RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        Ui.hairlineStrong.copy(alpha = 0.08f * reveal),
                        RoundedCornerShape(12.dp)
                    )
            }
            val pressed = isPressed(interaction)
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            pressed -> Ui.overlayPressed
                            hovered && !isSelected -> Ui.overlayHover
                            else -> Color.Transparent
                        }
                    )
                    .then(selectedModifier)
                    .hoverable(interaction)
                    .handCursor()
                    .clickable(interactionSource = interaction, indication = null) { onSelect(tab) }
                    .padding(horizontal = hPad, vertical = vPad)
            ) {
                Text(
                    tab,
                    fontSize = font,
                    color = if (isSelected) Ui.accentHover else Ui.textSecondary,
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
                                .background(Ui.accent, RoundedCornerShape(1.dp))
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}


@Composable
internal fun V9Sidebar(modifier: Modifier, selectedNav: String, onSelect: (String) -> Unit) {
    val items = listOf("时停", "武器渲染", "斩击", "回溯", "设置")
    Column(
        modifier.background(Brush.verticalGradient(listOf(Ui.surface1, Ui.surface1))).drawBehind {
            drawLine(Ui.hairline, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
        }.padding(top = 10.dp, bottom = 10.dp)
    ) {
        Text("导航", fontSize = 11.sp, color = Ui.textTertiary, fontFamily = cnFont, modifier = Modifier.padding(horizontal = 14.dp))
        Spacer(Modifier.height(12.dp))
        items.forEach { item ->
            val interaction = remember { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            val pressed = isPressed(interaction)
            val selected = item == selectedNav
            Row(
                Modifier.fillMaxWidth().height(38.dp)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            pressed -> Ui.accentTintStrong
                            selected -> Ui.accentTintStrong
                            hovered -> Ui.overlayHover
                            else -> Color.Transparent
                        }
                    )
                    .hoverable(interaction)
                    .handCursor()
                    .clickable(interactionSource = interaction, indication = null) { onSelect(item) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.width(3.dp).height(24.dp)
                        .background(if (selected) Ui.accent else Color.Transparent, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(10.dp))
                Text(item, fontSize = 13.sp, color = if (selected) Ui.textPrimary else Ui.textSecondary, fontFamily = cnFont)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("设置跟随这把神剑保存", fontSize = 10.sp, color = Ui.textTertiary, fontFamily = cnFont, modifier = Modifier.padding(horizontal = 14.dp))
    }
}


@Composable
internal fun V9Center(
    modifier: Modifier,
    selectedNav: String,
    sections: List<Pair<V9Section, List<V9Row>>>,
    presetSlot: Int, onPresetLoad: (Int) -> Unit, onPresetSave: () -> Unit,
    onToggle: (String) -> Unit,
    onSegment: (String, Int) -> Unit,
    onText: (String, String) -> Unit,
    onSlider: (String, Float) -> Unit,
    onBall: (Int) -> Unit,
    onCustomColor: (Int) -> Unit,
    state: V9Holder,
    sectionTab: String,
    onSectionTab: (String) -> Unit,
    onResetPage: (String) -> Unit
) {
    if (selectedNav == "设置") {
        V9SettingsPanel(
            modifier = modifier,
            state = state,
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
        modifier.background(Brush.verticalGradient(listOf(Ui.surface2, Ui.surface1))).drawBehind {
            drawLine(Ui.hairline, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
        }.panelScroll("时停").padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(if (selectedNav == "时停") "时停设置" else "${selectedNav}设置", fontSize = 14.sp, color = Ui.textPrimary, fontFamily = cnFont)
        V9TimestopHint(selectedNav, state)
        if (selectedNav == "时停") {
            V9LobeTabs(
                items = timestopTabs,
                selected = sectionTab,
                onSelect = onSectionTab,
            state = state,
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
                Text(section.name, fontSize = 12.sp, color = Ui.textSecondary, fontFamily = cnFont)
                Text(
                    section.hint,
                    fontSize = 10.sp,
                    color = Ui.textTertiary,
                    fontFamily = cnFont,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    maxLines = 2
                )
            }
            rows.forEach { row ->
                V9TimestopRow(row, state, presetSlot, onToggle, onSegment, onSlider,
                    onBall, onCustomColor, onPresetLoad, onPresetSave)
            }
        }
    }
}


@Composable
internal fun V9Preview(
    modifier: Modifier,
    state: V9Holder,
    onStageBounds: (Rect) -> Unit
) {
    // Read here rather than in the composition root: only this panel follows the timestop look.
    val grayScreen = state.grayScreen
    val startDuration = state.startDuration
    val endDuration = state.endDuration
    val ballColor: Long = if (state.ballColorCustom) {
        state.customColor.toLong() and 0xFFFFFFFFL
    } else {
        ballColors[state.ballColor]
    }
    Column(modifier.padding(12.dp)) {
        Text("预览", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont)
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
                .background(Brush.verticalGradient(listOf(Ui.surface1, Ui.surface0)), RoundedCornerShape(16.dp))
                .border(1.dp, Ui.hairlineSoft, RoundedCornerShape(16.dp)),
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
                        .background(Brush.horizontalGradient(listOf(Ui.accentBright, Ui.accentPressed)))
                )
                Box(Modifier.weight(0.48f).fillMaxHeight().background(Ui.surface4))
                Box(
                    Modifier.weight(0.22f).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(Ui.accentPressed, Ui.accentBright)))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            ValueCell(String.format(java.util.Locale.ROOT, "%.1fs", startDuration), Ui.textBody)
            ValueCell("持续", Ui.textSecondary)
            ValueCell(String.format(java.util.Locale.ROOT, "%.1fs", endDuration), Ui.textBody)
        }
    }
}


@Composable
internal fun RowScope.PhaseLabel(text: String) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 10.sp, color = Ui.textSecondary, fontFamily = cnFont, maxLines = 1)
    }
}


@Composable
internal fun RowScope.ValueCell(text: String, color: Color) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 10.sp, color = color, fontFamily = cnFont, maxLines = 1)
    }
}


@Composable
internal fun PreviewGlow(ballColor: Long) {
    Canvas(Modifier.fillMaxSize().padding(12.dp)) {
        val accent = Color(ballColor)
        drawCircle(
            Brush.radialGradient(listOf(accent.copy(alpha = 0.26f), Ui.accent.copy(alpha = 0f))),
            radius = size.minDimension * 0.45f,
            center = center
        )
    }
}


@Composable
internal fun V9Footer(quote: String, onReset: () -> Unit, onCancel: () -> Unit, onSave: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(66.dp)
            .background(Brush.verticalGradient(listOf(Ui.surface1, Ui.surface1)))
            .drawBehind { drawLine(Ui.hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f) }
    ) {
        Row(
            Modifier.align(Alignment.CenterStart).padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(quote, fontSize = 12.sp, color = Ui.textBody, fontFamily = cnFont, maxLines = 1)
        }
        Row(
            Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(72.dp).height(2.dp).background(
                    Brush.horizontalGradient(listOf(Color.Transparent, Ui.overlayFade, Ui.accentTint))
                )
            )
            Spacer(Modifier.width(12.dp))
            Row(
                Modifier.height(36.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(Ui.surface3, Ui.surface1)))
                    .border(1.dp, Ui.hairline, RoundedCornerShape(14.dp))
                    .drawBehind {
                        drawLine(Ui.innerLine, Offset(8f, 3f), Offset(size.width - 8f, 3f), 1f)
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(9.dp).height(9.dp).rotate(45f).background(Ui.accent, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text("星渊", fontSize = 12.sp, color = Ui.textBody, fontFamily = cnFont)
                Spacer(Modifier.width(6.dp))
                Text("THESEUS", fontSize = 11.sp, color = Ui.textTertiary, fontFamily = cnFont)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.width(72.dp).height(2.dp).background(
                    Brush.horizontalGradient(listOf(Ui.accentTint, Ui.overlayFade, Color.Transparent))
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


/**
 * ESC used to drop the screen and every un-saved edit with it. This asks instead: a modal scrim over
 * the window, primary action on the right, and the card is elevated above the 20dp window radius.
 */
@Composable
internal fun V9CloseConfirmDialog(
    onSaveClose: () -> Unit,
    onDiscardClose: () -> Unit,
    onKeepEditing: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Ui.scrim),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(320.dp)
                .shadow(20.dp, RoundedCornerShape(16.dp), ambientColor = Ui.shadowStrong, spotColor = Ui.shadowStrong)
                .clip(RoundedCornerShape(16.dp))
                .background(Ui.surface2)
                .border(1.dp, Ui.hairlineStrong, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text("有未保存的修改", fontSize = 14.sp, color = Ui.textPrimary, fontWeight = FontWeight.Bold, fontFamily = cnFont)
            Spacer(Modifier.height(8.dp))
            Text(
                "关闭后这些改动会还原到上次保存的状态。",
                fontSize = 12.sp,
                color = Ui.textBody,
                fontFamily = cnFont
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                V9PillButton("继续编辑", primary = false, onClick = onKeepEditing)
                Spacer(Modifier.width(8.dp))
                V9PillButton("放弃修改", primary = false, onClick = onDiscardClose)
                Spacer(Modifier.width(8.dp))
                V9PillButton("保存并关闭", primary = true, onClick = onSaveClose)
            }
        }
    }
}

