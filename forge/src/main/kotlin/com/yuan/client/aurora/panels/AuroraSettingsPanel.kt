package com.yuan.client.aurora.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
