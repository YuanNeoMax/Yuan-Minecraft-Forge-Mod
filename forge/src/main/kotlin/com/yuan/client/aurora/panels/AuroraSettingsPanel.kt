package com.yuan.client.aurora.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.AuroraLayout
import com.yuan.client.aurora.icons.AuroraIcon
import com.yuan.client.aurora.scaffold.AuroraButton
import com.yuan.client.aurora.scaffold.AuroraRow
import com.yuan.client.aurora.scaffold.AuroraSegments
import com.yuan.client.aurora.scaffold.AuroraSlider
import com.yuan.client.aurora.scaffold.AuroraToggle
import com.yuan.client.aurora.scaffold.CONTROL_WIDTH
import com.yuan.client.aurora.scaffold.auroraScroll
import com.yuan.client.aurora.scaffold.handCursor
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale
import com.yuan.client.aurora.theme.auroraFont
import java.util.Locale

/**
 * The window's own settings: scale, motion, backdrop, parallax and the editing switch.
 *
 * <p>These live in a panel rather than a page. The scale and the motion level are things a player
 * adjusts once and then forgets; putting them behind a navigation entry would spend one of the rail's
 * slots on a page nobody visits. A panel opens over whatever they were already looking at, which is
 * also correct: changing the scale while watching the grid is the only way to judge the change.
 *
 * <p>Every change writes to the layout and calls [onChanged], which records a revision and persists on
 * close. There is no save button: a scale whose effect only appeared after saving would make the player
 * commit to a value they cannot see, and every field here is reversible by dragging back.
 */
@Composable
internal fun AuroraSettingsPanel(
    layout: AuroraLayout,
    onChanged: () -> Unit,
    onResetLayout: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .auroraScroll(rememberScrollState())
            .padding(bottom = 8.dp)
    ) {
        GroupLabel("显示", first = true)
        // THE ACCENT, as a row of presets rather than a picker.
        //
        // A full RGB picker needs three sliders and a preview, inside a panel that is already
        // dense -- and the two shades derived from the accent (the dim hover and the faint wash)
        // would then have to be re-derived by hand at every drag, which is how a tint ends up not
        // matching its accent. Five well-chosen hues, one click each, shades computed once.
        //
        // The swatches are shown AS THE COLOUR, which is the only row in this panel where a control
        // is not a slider or a toggle: the question is "which colour", and answering it with words
        // would make the player read five names to pick a hue they could have recognised instantly.
        AuroraRow("强调色", "开关、滑条与焦点的颜色") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AuroraLayout.AccentPreset.values().forEach { preset ->
                    val chosen = currentAccentPreset(layout)
                    val isCurrent = chosen == preset
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(Star.controlRadius))
                            .background(Color(preset.color))
                            .border(
                                if (isCurrent) 2.dp else 1.dp,
                                if (isCurrent) Star.textPrimary else Star.hairline,
                                RoundedCornerShape(Star.controlRadius)
                            )
                            .handCursor()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                layout.look.accentColor = preset.color
                                Star.setAccent(Color(preset.color))
                                onChanged()
                            }
                    )
                }
            }
        }
        AuroraRow("界面缩放", "在游戏缩放之上再乘一层") {
            Row {
                AuroraSlider(
                    value = layout.scale,
                    range = AuroraLayout.MIN_SCALE..AuroraLayout.MAX_SCALE,
                    step = 0.05f,
                    onValueChange = { layout.scale = it; onChanged() },
                    format = { String.format(Locale.ROOT, "%.2fx", it) },
                    width = 72.dp,
                    enabled = !layout.autoScale
                )
                AuroraToggle(layout.autoScale, { layout.autoScale = it; onChanged() })
            }
        }
        // GLASS BEFORE BLUR, in the reading order of cause and effect: the preset decides how much
        // of the world shows through, and the blur slider decides how that world looks. Presenting
        // the blur first would invite tuning a value whose visible effect is set by the control
        // underneath it.
        // MATERIAL BEFORE AMOUNT: what the surfaces are made of is the bigger question than how
        // much of the world comes through them, and the second only means something once the first
        // has been answered.
        AuroraRow("液态材质", "磨砂全屏背景，或逐面板折射") {
            AuroraSegments(
                options = AuroraLayout.MaterialMode.values().map { it.ordinal },
                selected = layout.look.material.ordinal,
                onSelect = {
                    layout.look.material = AuroraLayout.MaterialMode.values()[it]
                    onChanged()
                },
                label = { AuroraLayout.MaterialMode.values()[it].label },
                width = CONTROL_WIDTH
            )
        }
        AuroraRow("玻璃材质", "面板的通透程度；与背景模糊配合生效") {
            AuroraSegments(
                options = AuroraLayout.GlassLevel.values().map { it.ordinal },
                selected = layout.look.glass.ordinal,
                onSelect = {
                    layout.look.glass = AuroraLayout.GlassLevel.values()[it]
                    onChanged()
                },
                label = { AuroraLayout.GlassLevel.values()[it].label },
                width = CONTROL_WIDTH
            )
        }
        AuroraRow("背景模糊", "为 0 时世界保持清晰") {
            AuroraSlider(
                value = layout.backdropBlur,
                range = 0f..1f,
                step = 0.05f,
                onValueChange = { layout.backdropBlur = it; onChanged() },
                format = { String.format(Locale.ROOT, "%.0f%%", it * 100f) }
            )
        }
        AuroraRow("深度视差", "各层随鼠标位移不同") {
            AuroraSlider(
                value = layout.parallax,
                range = 0f..1f,
                step = 0.05f,
                onValueChange = { layout.parallax = it; onChanged() },
                format = { String.format(Locale.ROOT, "%.0f%%", it * 100f) }
            )
        }

        GroupLabel("动效")
        AuroraRow("动效强度", "克制档仅保留状态反馈") {
            AuroraSegments(
                options = listOf(0, 1, 2),
                selected = layout.motion.ordinal,
                onSelect = {
                    layout.motion = AuroraLayout.MotionLevel.from(it)
                    onChanged()
                },
                label = { listOf("完整", "克制", "关闭")[it] },
                width = CONTROL_WIDTH
            )
        }

        GroupLabel("布局")
        AuroraRow("窗口手势", "标题栏拖动 / 边缘缩放 / 双击最大化") {
            AuroraToggle(
                checked = layout.gesturesEnabled,
                onCheckedChange = { layout.gesturesEnabled = it; onChanged() }
            )
        }
        AuroraRow("边缘吸附", "拖到屏幕边缘时半屏 / 1/4 屏 / 最大化") {
            AuroraToggle(
                checked = layout.aeroSnapEnabled,
                onCheckedChange = { layout.aeroSnapEnabled = it; onChanged() }
            )
        }
        AuroraRow("网格吸附", "拖动时的对齐步长") {
            AuroraSlider(
                value = layout.snap,
                range = 0f..0.1f,
                step = 0.005f,
                onValueChange = { layout.snap = it; onChanged() },
                format = { if (it <= 0f) "关闭" else String.format(Locale.ROOT, "%.1f%%", it * 100f) }
            )
        }
        AuroraRow("自定义外观", "右键任意元素打开它的数值面板") {
            Text(
                "右键元素",
                fontSize = TypeScale.value,
                color = Star.textSecondary,
                fontFamily = auroraFont
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            AuroraButton("重置布局", onClick = onResetLayout, icon = AuroraIcon.Reset)
        }
    }
}

/** A dim group label with a rule above it, except for the first in a panel. */
@Composable
private fun GroupLabel(text: String, first: Boolean = false) {
    if (!first) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Star.hairline))
    }
    Text(
        text,
        fontSize = TypeScale.hint,
        color = Star.textTertiary,
        fontFamily = auroraFont,
        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp)
    )
}

/**
 * Which preset the layout is currently showing.
 *
 * <p>A stored zero means "the shipped accent", so it resolves to STARLIGHT rather than to "no
 * swatch is selected" -- a row of five swatches with none lit reads as an unset value, which is the
 * opposite of what the default is.
 */
private fun currentAccentPreset(layout: AuroraLayout): AuroraLayout.AccentPreset {
    val stored = layout.look.accentColor
    val packed = if (stored == 0) Star.defaultAccent.value.toLong() else stored.toLong()
    val rgb = packed and 0xFFFFFFL
    return AuroraLayout.AccentPreset.values().firstOrNull {
        (it.color.toLong() and 0xFFFFFFL) == rgb
    } ?: AuroraLayout.AccentPreset.STARLIGHT
}
