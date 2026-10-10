package com.yuan.client.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.minecraft.world.item.Items

/** Owns the trigger-mode read so the centre column no longer subscribes to it. */
@Composable
internal fun V9TimestopHint(selectedNav: String, state: V9Holder) {
    val triggerMode = state.triggerMode
    Text(
        when {
            selectedNav == "领域" -> "按 V 展开 / 收束领域"
            selectedNav != "时停" -> "设置跟随这把神剑保存"
            triggerMode == 1 -> "Shift + 右键触发"
            triggerMode == 2 -> "自定义键触发"
            else -> "右键触发"
        },
        fontSize = 11.sp,
        color = Ui.textSecondary,
        fontFamily = cnFont,
        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
    )
}


/**
 * One row of the time-stop table. It reads its own values from the holder, so editing a
 * single setting recomposes this row instead of the whole centre column.
 */
@Composable
internal fun V9TimestopRow(
    row: V9Row,
    state: V9Holder,
    presetSlot: Int,
    onToggle: (String) -> Unit,
    onSegment: (String, Int) -> Unit,
    onSlider: (String, Float) -> Unit,
    onBall: (Int) -> Unit,
    onCustomColor: (Int) -> Unit,
    onPresetLoad: (Int) -> Unit,
    onPresetSave: () -> Unit
) {
    when (row.type) {
    0 -> V9SwitchRow(
        row.name, row.desc,
        switchValue(
            row.id, state.enabled, state.invulnerable, state.grayScreen, state.grayAnimate,
            state.soundEnabled, state.freezeSelf, state.freezeEntities,
            state.freezeBlocks, state.freezeFluids, state.freezeBossAI, state.showMessage,
            state.domainStarfield
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
            "startAnim" -> state.startAnim
            "endAnim" -> state.endAnim
            "triggerMode" -> state.triggerMode
            else -> state.grayStyle
        }
    ) { onSegment(row.id, it) }
    2 -> V9SliderRow(
        row.name, row.desc, {
        when (row.id) {
            "startDuration" -> state.startDuration
            "endDuration" -> state.endDuration
            "grayStrength" -> state.grayStrength
            "particleSize" -> state.particleSize
            "stopDuration" -> state.stopDuration
            "cooldown" -> state.cooldown.toFloat()
            "soundVolume" -> state.soundVolume
            "particleAlpha" -> state.particleAlpha
            "particleCount" -> state.particleCount.toFloat()
            "particleSpin" -> state.particleSpin
            "domainRadius" -> state.domainRadius
            else -> state.stopRadius
        } },
        when (row.id) {
            "grayStrength" -> 0f
            "particleSize" -> 0.4f
            "stopDuration" -> 0f
            "cooldown" -> 0f
            "soundVolume" -> 0f
            "particleAlpha" -> 0.1f
            "particleCount" -> 1f
            "particleSpin" -> 0f
            "domainRadius" -> 4f
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
            "domainRadius" -> 128f
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
            "domainRadius" -> { v -> String.format(java.util.Locale.ROOT, "%.0f 格", v) }
            else -> { v -> String.format(java.util.Locale.ROOT, "%.1fs", v) }
        }
    ) { onSlider(row.id, it) }
    3 -> V9SwatchRow(row.name, row.desc, state.ballColor, state.ballColorCustom, state.customColor, onBall, onCustomColor)
    else -> V9PresetRow(presetSlot, onPresetLoad, onPresetSave)
}
}


@Composable
internal fun V9SettingsPanel(
    modifier: Modifier,
    state: V9Holder,
    onToggle: (String) -> Unit,
    onSegment: (String, Int) -> Unit,
    onText: (String, String) -> Unit,
    onSlider: (String, Float) -> Unit,
    onReset: (String) -> Unit
) {
    // Read here so V9Center no longer subscribes to the tab preferences.
    val uiTabVariant = state.uiTabVariant
    val uiTabSize = state.uiTabSize
    val uiTabCompact = state.uiTabCompact
    var settingsTab by remember { mutableStateOf("标签栏") }
    var previewSearch by remember { mutableStateOf("") }
    var previewSwitch by remember { mutableStateOf(true) }
    Column(
        modifier.background(Brush.verticalGradient(listOf(Ui.surface2, Ui.surface1)))
            .drawBehind {
                drawLine(Ui.hairline, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
            }
            .panelScroll("设置")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("设置", fontSize = 14.sp, color = Ui.textPrimary, fontFamily = cnFont)
        Text("界面显示与交互样式", fontSize = 11.sp, color = Ui.textSecondary, fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        V9LobeTabs(
            items = listOf("标签栏", "滑块", "搜索框", "开关"),
            selected = settingsTab,
            onSelect = { settingsTab = it },
            state = state,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        when (settingsTab) {
            "滑块" -> {
                Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("设置滑块") }.padding(bottom = 4.dp))
                V9SegmentRow("滑块高度", "矮 / 标准 / 高", listOf("矮", "标准", "高"), state.uiSliderHeight) {
                    onSegment("uiSliderHeight", it)
                }
                V9SegmentRow("滑块大小", "小 / 标准 / 大", listOf("小", "标准", "大"), state.uiSliderThumb) {
                    onSegment("uiSliderThumb", it)
                }
                Text("实时预览", fontSize = 12.sp, color = Ui.textSecondary, fontFamily = cnFont,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                V9SliderRow("示例", "调整上方参数即时生效", { 0.62f }, 0f, 1f, { "%.2f".format(it) }) { }
            }
            "搜索框" -> {
                Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("设置搜索框") }.padding(bottom = 4.dp))
                V9SegmentRow("搜索框样式", "经典 / 展开", listOf("经典", "展开"), state.uiSearchStyle) {
                    onSegment("uiSearchStyle", it)
                }
                V9SliderRow("宽度", "120~280", { state.uiSearchWidth }, 120f, 280f, { "%.0f".format(it) }) {
                    onSlider("uiSearchWidth", it)
                }
                V9SliderRow("高度", "28~48", { state.uiSearchHeight }, 28f, 48f, { "%.0f".format(it) }) {
                    onSlider("uiSearchHeight", it)
                }
                V9SliderRow("圆角", "0~24", { state.uiSearchRadius }, 0f, 24f, { "%.0f".format(it) }) {
                    onSlider("uiSearchRadius", it)
                }
                Text("实时预览", fontSize = 12.sp, color = Ui.textSecondary, fontFamily = cnFont,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                V9SearchBox(
                    value = previewSearch,
                    onValueChange = { previewSearch = it },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            "开关" -> {
                Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("设置开关") }.padding(bottom = 4.dp))
                V9SegmentRow("开关样式", "经典 / 渐变翻转 / 白底滑动 / 文字翻转", listOf("经典", "渐变翻转", "白底滑动", "文字翻转"), state.uiSwitchStyle) {
                    onSegment("uiSwitchStyle", it)
                }
                V9SliderRow("轨道宽度", "54~80", { state.uiSwitchWidth }, 54f, 80f, { "%.0f".format(it) }) {
                    onSlider("uiSwitchWidth", it)
                }
                V9SliderRow("轨道高度", "24~32", { state.uiSwitchHeight }, 24f, 32f, { "%.0f".format(it) }) {
                    onSlider("uiSwitchHeight", it)
                }
                V9SliderRow("圆钮大小", "16~24", { state.uiSwitchKnob }, 16f, 24f, { "%.0f".format(it) }) {
                    onSlider("uiSwitchKnob", it)
                }
                if (state.uiSwitchStyle == 1) {
                    Text("渐变颜色 1", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchGradientA shr 16) and 0xFF }) { onSlider("uiSwitchGradientAR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchGradientA shr 8) and 0xFF }) { onSlider("uiSwitchGradientAG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchGradientA and 0xFF }) { onSlider("uiSwitchGradientAB", it.toFloat()) }
                    Text("渐变颜色 2", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchGradientB shr 16) and 0xFF }) { onSlider("uiSwitchGradientBR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchGradientB shr 8) and 0xFF }) { onSlider("uiSwitchGradientBG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchGradientB and 0xFF }) { onSlider("uiSwitchGradientBB", it.toFloat()) }
                    Text("未选中轨道色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchTrackOff shr 16) and 0xFF }) { onSlider("uiSwitchTrackOffR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchTrackOff shr 8) and 0xFF }) { onSlider("uiSwitchTrackOffG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchTrackOff and 0xFF }) { onSlider("uiSwitchTrackOffB", it.toFloat()) }
                } else if (state.uiSwitchStyle == 2) {
                    Text("轨道底色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchSlideTrack shr 16) and 0xFF }) { onSlider("uiSwitchSlideTrackR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchSlideTrack shr 8) and 0xFF }) { onSlider("uiSwitchSlideTrackG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchSlideTrack and 0xFF }) { onSlider("uiSwitchSlideTrackB", it.toFloat()) }
                    Text("未选中圆钮色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchSlideOff shr 16) and 0xFF }) { onSlider("uiSwitchSlideOffR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchSlideOff shr 8) and 0xFF }) { onSlider("uiSwitchSlideOffG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchSlideOff and 0xFF }) { onSlider("uiSwitchSlideOffB", it.toFloat()) }
                    Text("选中圆钮色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchSlideOn shr 16) and 0xFF }) { onSlider("uiSwitchSlideOnR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchSlideOn shr 8) and 0xFF }) { onSlider("uiSwitchSlideOnG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchSlideOn and 0xFF }) { onSlider("uiSwitchSlideOnB", it.toFloat()) }
                } else if (state.uiSwitchStyle == 3) {
                    Text("未选中轨道色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchTextTrackOff shr 16) and 0xFF }) { onSlider("uiSwitchTextTrackOffR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchTextTrackOff shr 8) and 0xFF }) { onSlider("uiSwitchTextTrackOffG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchTextTrackOff and 0xFF }) { onSlider("uiSwitchTextTrackOffB", it.toFloat()) }
                    Text("选中轨道色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchTextTrackOn shr 16) and 0xFF }) { onSlider("uiSwitchTextTrackOnR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchTextTrackOn shr 8) and 0xFF }) { onSlider("uiSwitchTextTrackOnG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchTextTrackOn and 0xFF }) { onSlider("uiSwitchTextTrackOnB", it.toFloat()) }
                    Text("圆钮色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchTextKnob shr 16) and 0xFF }) { onSlider("uiSwitchTextKnobR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchTextKnob shr 8) and 0xFF }) { onSlider("uiSwitchTextKnobG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchTextKnob and 0xFF }) { onSlider("uiSwitchTextKnobB", it.toFloat()) }
                    Text("文字颜色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    V9ColorSlider("R", { (state.uiSwitchTextColor shr 16) and 0xFF }) { onSlider("uiSwitchTextColorR", it.toFloat()) }
                    V9ColorSlider("G", { (state.uiSwitchTextColor shr 8) and 0xFF }) { onSlider("uiSwitchTextColorG", it.toFloat()) }
                    V9ColorSlider("B", { state.uiSwitchTextColor and 0xFF }) { onSlider("uiSwitchTextColorB", it.toFloat()) }
                    V9TextInputRow("开启文字", "选中时显示的文字", state.uiSwitchTextOn) { onText("uiSwitchTextOn", it) }
                    V9TextInputRow("关闭文字", "未选中时显示的文字", state.uiSwitchTextOff) { onText("uiSwitchTextOff", it) }
                }
                Text("实时预览", fontSize = 12.sp, color = Ui.textSecondary, fontFamily = cnFont,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                V9Switch(previewSwitch) { previewSwitch = !previewSwitch }
            }
            else -> {
                Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
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
internal fun V9WeaponPanel(
    modifier: Modifier,
    state: V9Holder,
    onToggle: (String) -> Unit,
    onSegment: (String, Int) -> Unit,
    onSlider: (String, Float) -> Unit,
    onReset: (String) -> Unit
) {
    Column(
        modifier.background(Brush.verticalGradient(listOf(Ui.surface2, Ui.surface1)))
            .drawBehind {
                drawLine(Ui.hairline, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
            }
            .panelScroll("武器渲染")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("武器渲染", fontSize = 14.sp, color = Ui.textPrimary, fontFamily = cnFont)
        Text("按感知度排序：先选渲染风格，再调该风格的参数", fontSize = 11.sp, color = Ui.textSecondary, fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        V9SegmentRow("渲染风格", "星空 / 丝绸 / 隧道 / 晶格 / 原版 / 关闭",
            listOf("星空", "丝绸", "隧道", "晶格", "原版", "关闭"), renderStyleUiIndex(state.renderStyle)) { onSegment("renderStyle", it) }

        if (state.renderStyle == 1) {
        Text("丝绸", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
            modifier = Modifier.align(Alignment.End).clickable { onReset("丝绸") }.padding(bottom = 4.dp))
        Text("调色板颜色 1-4", fontSize = 12.sp, color = Ui.textSecondary, fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
        V9ColorSlider("1R", { (state.silkColor0 shr 16) and 0xFF }) { onSlider("silkColor0R", it.toFloat()) }
        V9ColorSlider("1G", { (state.silkColor0 shr 8) and 0xFF }) { onSlider("silkColor0G", it.toFloat()) }
        V9ColorSlider("1B", { state.silkColor0 and 0xFF }) { onSlider("silkColor0B", it.toFloat()) }
        V9ColorSlider("2R", { (state.silkColor1 shr 16) and 0xFF }) { onSlider("silkColor1R", it.toFloat()) }
        V9ColorSlider("2G", { (state.silkColor1 shr 8) and 0xFF }) { onSlider("silkColor1G", it.toFloat()) }
        V9ColorSlider("2B", { state.silkColor1 and 0xFF }) { onSlider("silkColor1B", it.toFloat()) }
        V9ColorSlider("3R", { (state.silkColor2 shr 16) and 0xFF }) { onSlider("silkColor2R", it.toFloat()) }
        V9ColorSlider("3G", { (state.silkColor2 shr 8) and 0xFF }) { onSlider("silkColor2G", it.toFloat()) }
        V9ColorSlider("3B", { state.silkColor2 and 0xFF }) { onSlider("silkColor2B", it.toFloat()) }
        V9ColorSlider("4R", { (state.silkColor3 shr 16) and 0xFF }) { onSlider("silkColor3R", it.toFloat()) }
        V9ColorSlider("4G", { (state.silkColor3 shr 8) and 0xFF }) { onSlider("silkColor3G", it.toFloat()) }
        V9ColorSlider("4B", { state.silkColor3 and 0xFF }) { onSlider("silkColor3B", it.toFloat()) }
        Text("调色板颜色 5-8", fontSize = 12.sp, color = Ui.textSecondary, fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp))
        V9ColorSlider("5R", { (state.silkColor4 shr 16) and 0xFF }) { onSlider("silkColor4R", it.toFloat()) }
        V9ColorSlider("5G", { (state.silkColor4 shr 8) and 0xFF }) { onSlider("silkColor4G", it.toFloat()) }
        V9ColorSlider("5B", { state.silkColor4 and 0xFF }) { onSlider("silkColor4B", it.toFloat()) }
        V9ColorSlider("6R", { (state.silkColor5 shr 16) and 0xFF }) { onSlider("silkColor5R", it.toFloat()) }
        V9ColorSlider("6G", { (state.silkColor5 shr 8) and 0xFF }) { onSlider("silkColor5G", it.toFloat()) }
        V9ColorSlider("6B", { state.silkColor5 and 0xFF }) { onSlider("silkColor5B", it.toFloat()) }
        V9ColorSlider("7R", { (state.silkColor6 shr 16) and 0xFF }) { onSlider("silkColor6R", it.toFloat()) }
        V9ColorSlider("7G", { (state.silkColor6 shr 8) and 0xFF }) { onSlider("silkColor6G", it.toFloat()) }
        V9ColorSlider("7B", { state.silkColor6 and 0xFF }) { onSlider("silkColor6B", it.toFloat()) }
        V9ColorSlider("8R", { (state.silkColor7 shr 16) and 0xFF }) { onSlider("silkColor7R", it.toFloat()) }
        V9ColorSlider("8G", { (state.silkColor7 shr 8) and 0xFF }) { onSlider("silkColor7G", it.toFloat()) }
        V9ColorSlider("8B", { state.silkColor7 and 0xFF }) { onSlider("silkColor7B", it.toFloat()) }
        V9SliderRow("亮度", "-1~1", { state.silkBrightness }, -1f, 1f, { "%.2f".format(it) }) { onSlider("silkBrightness", it) }
        V9SliderRow("对比度", "0.1~3.0", { state.silkContrast }, 0.1f, 3f, { "%.2f".format(it) }) { onSlider("silkContrast", it) }
        V9SliderRow("饱和度", "0~3.0", { state.silkSaturation }, 0f, 3f, { "%.2f".format(it) }) { onSlider("silkSaturation", it) }
        V9SliderRow("图案大小", "0.2~8.0", { state.silkScale }, 0.2f, 8f, { "%.2f".format(it) }) { onSlider("silkScale", it) }
        V9SliderRow("流动强度", "0~3.0", { state.silkIntensity }, 0f, 3f, { "%.2f".format(it) }) { onSlider("silkIntensity", it) }
        V9SliderRow("扭曲", "0~2.0", { state.silkWarp }, 0f, 2f, { "%.2f".format(it) }) { onSlider("silkWarp", it) }
        V9SliderRow("细节", "0.5~8.0", { state.silkDetail }, 0.5f, 8f, { "%.2f".format(it) }) { onSlider("silkDetail", it) }
        V9SliderRow("色相", "0~360°", { state.silkHue }, 0f, 6.283f, { "%.0f".format(it * 57.29578f) }) { onSlider("silkHue", it) }
        V9SliderRow("种子", "0~20000", { state.silkSeed }, 0f, 20000f, { "%.0f".format(it) }) { onSlider("silkSeed", it) }
        V9SliderRow("旋转", "-360~360°", { state.silkRotation }, -6.283f, 6.283f, { "%.0f".format(it * 57.29578f) }) { onSlider("silkRotation", it) }
        V9SliderRow("漂移", "0~3.0", { state.silkDrift }, 0f, 3f, { "%.2f".format(it) }) { onSlider("silkDrift", it) }
        V9SliderRow("暗角", "0~1.0", { state.silkVignette }, 0f, 1f, { "%.2f".format(it) }) { onSlider("silkVignette", it) }
        V9SliderRow("柔化", "0~0.05", { state.silkBlur }, 0f, 0.05f, { "%.3f".format(it) }) { onSlider("silkBlur", it) }
        V9SliderRow("颗粒", "0~1.0", { state.silkGrain }, 0f, 1f, { "%.2f".format(it) }) { onSlider("silkGrain", it) }
        }
        if (state.renderStyle == 4) {
            Text("隧道", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
                modifier = Modifier.align(Alignment.End).clickable { onReset("隧道") }.padding(bottom = 4.dp))
            V9SliderRow("速度", "0.1~3.0", { state.tunnelSpeed }, 0.1f, 3f, { "%.2f".format(it) }) {
                onSlider("tunnelSpeed", it)
            }
            V9SliderRow("亮度", "0.1~3.0", { state.tunnelBrightness }, 0.1f, 3f, { "%.2f".format(it) }) {
                onSlider("tunnelBrightness", it)
            }
            V9SliderRow("密度", "0.2~3.0", { state.tunnelDensity }, 0.2f, 3f, { "%.2f".format(it) }) {
                onSlider("tunnelDensity", it)
            }
            V9SliderRow("视野", "1.0~16.0", { state.tunnelFov }, 1f, 16f, { "%.1f".format(it) }) {
                onSlider("tunnelFov", it)
            }
        }
        if (state.renderStyle == 5) {
            var voronoiPreset by remember { mutableStateOf(0) }
            Text("晶格", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
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
            V9SliderRow("颜色数量", "1~5", { state.voronoiColorCount.toFloat() }, 1f, 5f, { "${it.toInt()}" }) {
                onSegment("voronoiColorCount", it.toInt())
            }
            V9SliderRow("颜色步进", "1~3", { state.voronoiStepsPerColor.toFloat() }, 1f, 3f, { "${it.toInt()}" }) {
                onSegment("voronoiStepsPerColor", it.toInt())
            }
            V9SliderRow("扭曲", "0~0.5", { state.voronoiDistortion }, 0f, 0.5f, { "%.2f".format(it) }) {
                onSlider("voronoiDistortion", it)
            }
            V9SliderRow("间隙", "0~0.1", { state.voronoiGap }, 0f, 0.1f, { "%.3f".format(it) }) {
                onSlider("voronoiGap", it)
            }
            V9SliderRow("内部辉光", "0~1.0", { state.voronoiGlow }, 0f, 1f, { "%.2f".format(it) }) {
                onSlider("voronoiGlow", it)
            }
            V9SliderRow("缩放", "0.05~4.0", { state.voronoiScale }, 0.05f, 4f, { "%.2f".format(it) }) {
                onSlider("voronoiScale", it)
            }
            V9SliderRow("视野", "1.0~16.0", { state.voronoiFov }, 1f, 16f, { "%.1f".format(it) }) {
                onSlider("voronoiFov", it)
            }
            V9SliderRow("动画速度", "0~3.0", { state.voronoiSpeed }, 0f, 3f, { "%.2f".format(it) }) {
                onSlider("voronoiSpeed", it)
            }
            V9SliderRow("旋转", "-360~360°", { state.voronoiRotation }, -6.283f, 6.283f, { "%.0f°".format(it * 57.29578f) }) {
                onSlider("voronoiRotation", it)
            }
            V9SliderRow("横向偏移", "-1~1", { state.voronoiOffsetX }, -1f, 1f, { "%.2f".format(it) }) {
                onSlider("voronoiOffsetX", it)
            }
            V9SliderRow("纵向偏移", "-1~1", { state.voronoiOffsetY }, -1f, 1f, { "%.2f".format(it) }) {
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
                Text(label, fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                V9ColorSlider("R", { (color shr 16) and 0xFF } ) { onSlider("${prefix}R", it.toFloat()) }
                V9ColorSlider("G", { (color shr 8) and 0xFF } ) { onSlider("${prefix}G", it.toFloat()) }
                V9ColorSlider("B", { color and 0xFF } ) { onSlider("${prefix}B", it.toFloat()) }
                V9ColorSlider("A", { (color ushr 24) and 0xFF } ) { onSlider("${prefix}A", it.toFloat()) }
            }
            Text("内部辉光色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            V9ColorSlider("R", { (state.voronoiColorGlow shr 16) and 0xFF }) { onSlider("voronoiColorGlowR", it.toFloat()) }
            V9ColorSlider("G", { (state.voronoiColorGlow shr 8) and 0xFF }) { onSlider("voronoiColorGlowG", it.toFloat()) }
            V9ColorSlider("B", { state.voronoiColorGlow and 0xFF }) { onSlider("voronoiColorGlowB", it.toFloat()) }
            V9ColorSlider("A", { (state.voronoiColorGlow ushr 24) and 0xFF }) { onSlider("voronoiColorGlowA", it.toFloat()) }
            Text("间隙颜色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            V9ColorSlider("R", { (state.voronoiColorGap shr 16) and 0xFF }) { onSlider("voronoiColorGapR", it.toFloat()) }
            V9ColorSlider("G", { (state.voronoiColorGap shr 8) and 0xFF }) { onSlider("voronoiColorGapG", it.toFloat()) }
            V9ColorSlider("B", { state.voronoiColorGap and 0xFF }) { onSlider("voronoiColorGapB", it.toFloat()) }
            V9ColorSlider("A", { (state.voronoiColorGap ushr 24) and 0xFF }) { onSlider("voronoiColorGapA", it.toFloat()) }
        }
    }
}


@Composable
internal fun V9SlashPanel(
    modifier: Modifier,
    state: V9Holder,
    onToggle: (String) -> Unit,
    onSlider: (String, Float) -> Unit,
    onSegment: (String, Int) -> Unit,
    onReset: (String) -> Unit
) {
    // Read here so V9Center no longer subscribes to the tab preferences.
    val uiTabVariant = state.uiTabVariant
    val uiTabSize = state.uiTabSize
    val uiTabCompact = state.uiTabCompact
    var slashTab by remember { mutableStateOf("基础") }
    Column(
        modifier.background(Brush.verticalGradient(listOf(Ui.surface2, Ui.surface1)))
            .drawBehind {
                drawLine(Ui.hairline, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
            }
            .panelScroll("斩击")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("斩击设置", fontSize = 14.sp, color = Ui.textPrimary, fontFamily = cnFont)
        Text("左键命中目标时在命中点播放空间斩", fontSize = 11.sp, color = Ui.textSecondary, fontFamily = cnFont,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        V9LobeTabs(
            items = listOf("基础", "外观", "动画", "星空"),
            selected = slashTab,
            onSelect = { slashTab = it },
            state = state,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        when (slashTab) {
            "外观" -> {
                Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("斩击外观") }.padding(bottom = 4.dp))
                V9SliderRow("黑芯宽度", "0.30~0.98", { state.slashCoreWidth }, 0.3f, 0.98f, { "%.2f".format(it) }) { onSlider("slashCoreWidth", it) }
                V9SliderRow("黑芯亮度", "0~100%", { state.slashCoreShade }, 0f, 1f, { "%.0f%%".format(it * 100f) }) { onSlider("slashCoreShade", it) }
                V9SliderRow("白边起始", "0.50~0.99", { state.slashEdgeWidth }, 0.5f, 0.99f, { "%.2f".format(it) }) { onSlider("slashEdgeWidth", it) }
                V9SliderRow("白边亮度", "0.1~2.0", { state.slashEdgeBrightness }, 0.1f, 2f, { "%.2f".format(it) }) { onSlider("slashEdgeBrightness", it) }
                V9SliderRow("发光宽度", "0.60~0.99", { state.slashGlowWidth }, 0.6f, 0.99f, { "%.2f".format(it) }) { onSlider("slashGlowWidth", it) }
                V9SliderRow("发光强度", "0~3.0", { state.slashGlowStrength }, 0f, 3f, { "%.2f".format(it) }) { onSlider("slashGlowStrength", it) }
                V9SliderRow("裂纹噪点", "0~2.0", { state.slashNoiseStrength }, 0f, 2f, { "%.2f".format(it) }) { onSlider("slashNoiseStrength", it) }
                Text("黑芯颜色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                V9ColorSlider("R", { (state.slashCoreColor shr 16) and 0xFF }) { onSlider("slashCoreColorR", it.toFloat()) }
                V9ColorSlider("G", { (state.slashCoreColor shr 8) and 0xFF }) { onSlider("slashCoreColorG", it.toFloat()) }
                V9ColorSlider("B", { state.slashCoreColor and 0xFF }) { onSlider("slashCoreColorB", it.toFloat()) }
                Text("白边颜色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                V9ColorSlider("R", { (state.slashEdgeColor shr 16) and 0xFF }) { onSlider("slashEdgeColorR", it.toFloat()) }
                V9ColorSlider("G", { (state.slashEdgeColor shr 8) and 0xFF }) { onSlider("slashEdgeColorG", it.toFloat()) }
                V9ColorSlider("B", { state.slashEdgeColor and 0xFF }) { onSlider("slashEdgeColorB", it.toFloat()) }
                Text("发光颜色", fontSize = 13.sp, color = Ui.textPrimary, fontFamily = cnFont,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                V9ColorSlider("R", { (state.slashGlowColor shr 16) and 0xFF }) { onSlider("slashGlowColorR", it.toFloat()) }
                V9ColorSlider("G", { (state.slashGlowColor shr 8) and 0xFF }) { onSlider("slashGlowColorG", it.toFloat()) }
                V9ColorSlider("B", { state.slashGlowColor and 0xFF }) { onSlider("slashGlowColorB", it.toFloat()) }
            }
            "动画" -> {
                Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("斩击动画") }.padding(bottom = 4.dp))
                V9SliderRow("扫开速度", "0.05~0.95", { state.slashSweepSpeed }, 0.05f, 0.95f, { "%.2f".format(it) }) { onSlider("slashSweepSpeed", it) }
                V9SliderRow("扫开柔和度", "0.02~0.60", { state.slashSweepSoftness }, 0.02f, 0.6f, { "%.2f".format(it) }) { onSlider("slashSweepSoftness", it) }
                V9SliderRow("保持时长", "0~0.8", { state.slashHoldFraction }, 0f, 0.8f, { "%.2f".format(it) }) { onSlider("slashHoldFraction", it) }
                V9SliderRow("淡出起始", "0.30~0.95", { state.slashFadeStart }, 0.3f, 0.95f, { "%.2f".format(it) }) { onSlider("slashFadeStart", it) }
                V9SliderRow("淡出时长", "0.02~0.70", { state.slashFadeDuration }, 0.02f, 0.7f, { "%.2f".format(it) }) { onSlider("slashFadeDuration", it) }
                V9SliderRow("命中点外移", "0~0.5", { state.slashSurfaceOffset }, 0f, 0.5f, { "%.2f".format(it) }) { onSlider("slashSurfaceOffset", it) }
                V9SliderRow("随机倾角", "0~360°", { state.slashRollRange }, 0f, 360f, { "%.0f".format(it) }) { onSlider("slashRollRange", it) }
            }
            "星空" -> {
                Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("斩击星空") }.padding(bottom = 4.dp))
                V9SwitchRow("星空开关", "刀身黑芯内显示星点", state.slashStarfield) { onToggle("slashStarfield") }
                V9SliderRow("星点密度", "0.05~3.0", { state.slashStarDensity }, 0.05f, 3f, { "%.2f".format(it) }) { onSlider("slashStarDensity", it) }
                V9SliderRow("星点亮度", "0~3.0", { state.slashStarBrightness }, 0f, 3f, { "%.2f".format(it) }) { onSlider("slashStarBrightness", it) }
                V9SliderRow("星点大小", "0.2~5.0", { state.slashStarSize }, 0.2f, 5f, { "%.2f".format(it) }) { onSlider("slashStarSize", it) }
                V9SegmentRow("颜色模式", "黑白或彩虹", listOf("黑白", "彩虹"), state.slashStarColorMode) { onSegment("slashStarColorMode", it) }
            }
            else -> {
                Text("恢复默认", fontSize = 11.sp, color = Ui.accent, fontFamily = cnFont,
                    modifier = Modifier.align(Alignment.End).clickable { onReset("斩击基础") }.padding(bottom = 4.dp))
                V9SwitchRow("斩击开关", "左键命中时播放空间斩", state.slashEnabled) { onToggle("slashEnabled") }
                V9SwitchRow("深度遮挡", "开启后被怪物/世界遮挡", state.slashDepthTest) { onToggle("slashDepthTest") }
                V9SwitchRow("随机偏角", "正对视线时随机角度", state.slashRandomAngle) { onToggle("slashRandomAngle") }
                V9SwitchRow("加法发光", "白边外发光层", state.slashGlow) { onToggle("slashGlow") }
                V9SliderRow("时长", "0.1~1.0 秒", { state.slashDuration }, 0.1f, 1f, { "%.2f".format(it) }) { onSlider("slashDuration", it) }
                V9SliderRow("长度倍率", "0.5~5.0", { state.slashLengthMult }, 0.5f, 5f, { "%.2f".format(it) }) { onSlider("slashLengthMult", it) }
                V9SliderRow("刀宽比例", "0.02~0.50", { state.slashWidthRatio }, 0.02f, 0.5f, { "%.3f".format(it) }) { onSlider("slashWidthRatio", it) }
                V9SliderRow("厚度比例", "0.002~0.20", { state.slashThicknessRatio }, 0.002f, 0.2f, { "%.3f".format(it) }) { onSlider("slashThicknessRatio", it) }
                V9SliderRow("尖端淡出", "0.02~0.50", { state.slashTipFade }, 0.02f, 0.5f, { "%.2f".format(it) }) { onSlider("slashTipFade", it) }
                V9SliderRow("起始缩放", "0.2~3.0", { state.slashStartScale }, 0.2f, 3f, { "%.2f".format(it) }) { onSlider("slashStartScale", it) }
                V9SliderRow("结束缩放", "0.2~3.0", { state.slashEndScale }, 0.2f, 3f, { "%.2f".format(it) }) { onSlider("slashEndScale", it) }
            }
        }
    }
}


@Composable
internal fun V9RewindPanel(
    modifier: Modifier,
    state: V9Holder,
    onToggle: (String) -> Unit,
    onSegment: (String, Int) -> Unit,
    onSlider: (String, Float) -> Unit,
    onReset: (String) -> Unit
) {
    Column(
        modifier.background(Brush.verticalGradient(listOf(Ui.surface2, Ui.surface1))).drawBehind {
            drawLine(Ui.hairline, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 1f)
        }.panelScroll("回溯").padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("回溯设置", fontSize = 14.sp, color = Ui.textPrimary, fontFamily = cnFont)
        Text("跟随这把神剑保存", fontSize = 11.sp, color = Ui.textTertiary, fontFamily = cnFont)

        V9SwitchRow("回溯开关", "启用时间回溯能力", state.rewindEnabled) { onToggle("rewindEnabled") }
        V9SwitchRow("死亡回溯", "死亡时自动回溯", state.rewindDeathEnabled) { onToggle("rewindDeathEnabled") }

        V9SliderRow("回溯窗口", "回退到多少秒前", { state.rewindWindowSeconds.toFloat() }, 1f, 60f, { "${it.toInt()}s" }) { onSlider("rewindWindowSeconds", it) }
        V9SliderRow("主动冷却", "主动回溯冷却秒数", { state.rewindCooldownTicks / 20f }, 0f, 300f, { String.format("%.1fs", it) }) { onSlider("rewindCooldownTicks", it * 20f) }
        V9SliderRow("死亡冷却", "死亡回溯冷却秒数", { state.rewindDeathCooldownTicks / 20f }, 0f, 300f, { String.format("%.1fs", it) }) { onSlider("rewindDeathCooldownTicks", it * 20f) }
        V9SliderRow("连续回溯上限", "0 表示直到窗口开头", { state.rewindDeathMaxRetries.toFloat() }, 0f, 10f, { it.toInt().toString() }) { onSlider("rewindDeathMaxRetries", it) }

        V9SegmentRow("范围模式", "全维度或当前维度", listOf("全维度", "当前维度"), state.rewindScopeMode) { onSegment("rewindScopeMode", it) }
        V9SegmentRow("区块模式", "全区块或半径", listOf("全区块", "半径"), state.rewindScope) { onSegment("rewindScope", it) }
        V9SliderRow("半径", "半径模式的半径", { state.rewindRadius }, 8f, 256f, { "${it.toInt()} 格" }) { onSlider("rewindRadius", it) }

        V9SegmentRow("还原方式", "渐进或瞬间", listOf("渐进", "瞬间"), state.rewindPlaybackMode) { onSegment("rewindPlaybackMode", it) }
        V9SliderRow("播放时长", "渐进还原播放秒数", { state.rewindPlaybackSeconds }, 0.5f, 5f, { String.format("%.1fs", it) }) { onSlider("rewindPlaybackSeconds", it) }
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

        Text("恢复默认", fontSize = 12.sp, color = Ui.accentHover, fontFamily = cnFont,
            modifier = Modifier.align(Alignment.End).clickable { onReset("回溯") }.padding(8.dp))
    }
}

