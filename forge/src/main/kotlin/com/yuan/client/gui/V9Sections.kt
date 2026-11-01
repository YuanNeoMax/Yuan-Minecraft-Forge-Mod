package com.yuan.client.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yuan.item.YuanGodSwordConfig
import net.minecraft.client.gui.screens.Screen
import org.jetbrains.skia.Surface

internal data class V9Row(
    val id: String,
    val name: String,
    val desc: String,
    val type: Int,
    val section: String,
    val hint: String
)


internal data class V9Section(val name: String, val hint: String, val rows: List<V9Row>)


internal val ballColors = listOf(
    0xFFFF7A5C, 0xFFE85D3D, 0xFFFFC9BA, 0xFF49322C,
    0xFF7C5CFF, 0xFF3DD6E8, 0xFF7AE85D, 0xFFFFD93D
)


internal val quotes = listOf(
    "星河长明，剑心不改。",
    "剑锋所指，即是我心。",
    "星云为骨，时间作刃。",
    "一瞬千年，一剑永恒。",
    "沉默的星，不灭的剑。"
)


internal val v9Sections = listOf(
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
    V9Section("领域", "领域展开的范围。", listOf(
        V9Row("domainStarfield", "星渊地面", "地形表面被星海替换；关闭后领域只剩其它层", 0, "领域", "领域展开的范围。"),
        // THE FIELD'S OWN APPEARANCE. It had no controls at all: the stars, the nebulae, the rings
        // and the colour spread were all constants, so "more of this" and "less of that" were not
        // available. These three are the ones a player actually reaches for -- how busy the sky
        // looks, whether it moves, and whether bright stars throw a cross.
        V9Row("domainStarDensity", "星场密度", "星点的疏密；1.0 为默认", 2, "领域", "领域展开的范围。"),
        V9Row("domainSpinScale", "星场转速", "0 可完全静止；1.0 为默认速度", 2, "领域", "领域展开的范围。"),
        V9Row("domainStarSpikes", "恒星十字光芒", "亮星的四芒衍射；密度高时可关掉以免过花", 0, "领域", "领域展开的范围。"),
    )),
    V9Section("预设", "快速保存与载入整套配置。", listOf(
        V9Row("preset", "配置预设", "槽 1-3 保存/载入", 4, "预设", "快速保存与载入整套配置。")
    ))
)


internal val navSections = mapOf(
    "时停" to setOf("基础", "动画", "画面", "音效", "光球", "时长", "冻结", "预设"),
    "领域" to setOf("领域"),
    "斩击" to setOf("斩击"),
    "武器渲染" to setOf("武器渲染"),
    "回溯" to setOf("回溯"),
    "设置" to setOf("设置")
)


internal val timestopTabs = listOf("基础", "效果", "音效", "冻结", "预设")


internal val timestopTabSections = mapOf(
    "基础" to setOf("基础", "时长"),
    "效果" to setOf("动画", "画面", "光球"),
    "音效" to setOf("音效"),
    "冻结" to setOf("冻结"),
    "预设" to setOf("预设")
)

