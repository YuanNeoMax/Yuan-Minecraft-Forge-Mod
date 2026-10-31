package com.yuan.client.aurora.panels

import com.yuan.client.gui.V9Row
import com.yuan.client.gui.v9Sections

/**
 * A row plus the two things the Aurora renderer needs and the older model has nowhere to put: which
 * render style reveals it, and whether its value is stored scaled.
 *
 * <p>Wrapping rather than extending `V9Row`: that type belongs to the older screen, and adding fields
 * to it would change a class the existing interface is constructed from. The wrap also keeps the
 * three ids in [GodSwordRows] usable by the binding, which dispatches on `V9Row.id` and does not care
 * what carries it.
 */
internal data class GodRow(
    val row: V9Row,
    /** The `renderStyle` index that reveals this row, or -1 when always shown. */
    val gate: Int = -1,
    /**
     * A factor between the stored value and the slider's.
     *
     * <p>Two rewind cooldowns are stored in ticks and edited in seconds; the older screen divided and
     * multiplied at the call site, which is where a factor gets applied twice or forgotten.
     */
    val scale: Float = 0f,
    /** For a channel slider: which hex byte this row edits, or null for an ordinary row. */
    val channel: Char? = null,
    /** For a channel slider: the packed colour field this channel belongs to. */
    val colourField: String? = null
) {
    /**
     * Whether this row's control needs a panel rather than a settings line.
     *
     * <p>True for the two multi-part rows and for every colour channel. A line carries one label and
     * one control between its two edges; eight swatches, three channel sliders or three preset slots do
     * not fit that shape, and squeezing them in is what pushed the rest of a section off screen.
     *
     * <p>Deliberately a property of the row rather than a list of ids kept elsewhere: a row that grows
     * a multi-part control would otherwise have to be remembered in a second place, and the first sign
     * it was forgotten would be a panel that rendered wrong.
     */
    val wantsPanel: Boolean
        get() = row.type == GodSwordRows.TYPE_CHANNEL ||
            row.id == "ballColor" ||
            row.id == "preset"
}

/**
 * Every god-sword setting, described as rows the Aurora renderer can draw.
 *
 * <p>The older interface built these inline inside three large composables, which is why they had no
 * data structure to reuse: a row existed only as a call with its range, its formatter and its write
 * baked into the argument list. Extracting them here gives the new interface one table to render and
 * gives the project one place where a setting's bounds are stated once.
 *
 * <p>Only the *description* moves. Read and write stay in [GodSwordBinding], dispatching on the same
 * id, so a row that appears here and not in the binding renders without effect rather than corrupting
 * a value.
 *
 * <p>Colour channels and the silk palette are generated rather than listed. Writing out twenty-four
 * palette rows and eight colour blocks by hand is exactly how a channel or a ninth colour gets
 * dropped -- the older screen enumerates both, and an enumeration that is generated cannot fall out
 * of step with the number of slots.
 */
internal object GodSwordRows {

    const val TYPE_SWITCH = 0
    const val TYPE_SEGMENT = 1
    const val TYPE_SLIDER = 2
    const val TYPE_SWATCH = 3
    const val TYPE_PRESET = 4
    /** One colour channel: a 0..255 slider bound to a byte of a packed colour. */
    const val TYPE_CHANNEL = 5

    /**
     * 预设's row: three slots that save or load the whole config.
     *
     * <p>Read from the same source as 时停 and 领域 rather than declared here, so its label is written
     * once and the two interfaces cannot disagree about what it is called.
     */
    val PRESET: List<GodRow> = sections("预设")

    /**
     * The row tables, named.     *
     * <p>A feature is backed by one of these rather than by a list of section names. Two features can
     * legitimately label a group the same thing -- 时停 and 斩击 both group rows under 基础 -- so naming
     * sections would pull the other feature's rows in with them, which is the bug this replaced: 时停
     * rendered with 斩击's ten 基础 rows mixed into it.
     */
    enum class Table { TIME_STOP, DOMAIN, SLASH, RENDER, REWIND, PRESET, WINDOW }

    /** The rows of one table, in declaration order. */
    internal fun rowsOf(table: Table): List<GodRow> = when (table) {
        Table.TIME_STOP -> TIME_STOP
        Table.DOMAIN -> DOMAIN
        Table.SLASH -> SLASH
        Table.RENDER -> RENDER
        Table.REWIND -> REWIND
        Table.PRESET -> PRESET
        Table.WINDOW -> emptyList()
    }

    /** 时停's sections, taken from the source the older interface uses. */
    val TIME_STOP: List<GodRow> = sections("基础", "时长", "动画", "画面", "光球", "音效", "冻结")

    /** 领域's row, from the same source. */
    val DOMAIN: List<GodRow> = sections("领域")

    /**
     * 斩击, grouped by what a setting affects rather than by the editor it used to live in.
     *
     * <p>The older screen split these across four sub-tabs; the group is a section heading inside one
     * module here, which is one click shallower and keeps a change of mind about grouping to an edit
     * of this list.
     */
    val SLASH: List<GodRow> = listOf(
        switch("slashEnabled", "斩击开关", "左键命中时播放空间斩", "基础"),
        switch("slashDepthTest", "深度遮挡", "开启后被怪物/世界遮挡", "基础"),
        switch("slashRandomAngle", "随机偏角", "正对视线时随机角度", "基础"),
        switch("slashGlow", "加法发光", "白边外发光层", "基础"),
        slider("slashDuration", "时长", "0.1~1.0 秒", "基础"),
        slider("slashLengthMult", "长度倍率", "0.5~5.0", "基础"),
        slider("slashWidthRatio", "刀宽比例", "0.02~0.50", "基础"),
        slider("slashThicknessRatio", "厚度比例", "0.002~0.20", "基础"),
        slider("slashTipFade", "尖端淡出", "0.02~0.50", "基础"),
        slider("slashStartScale", "起始缩放", "0.2~3.0", "基础"),
        slider("slashEndScale", "结束缩放", "0.2~3.0", "基础"),
        slider("slashCoreWidth", "黑芯宽度", "0.30~0.98", "外观"),
        slider("slashCoreShade", "黑芯亮度", "0~100%", "外观"),
        slider("slashEdgeWidth", "白边起始", "0.50~0.99", "外观"),
        slider("slashEdgeBrightness", "白边亮度", "0.1~2.0", "外观"),
        slider("slashGlowWidth", "发光宽度", "0.60~0.99", "外观"),
        slider("slashGlowStrength", "发光强度", "0~3.0", "外观"),
        slider("slashNoiseStrength", "裂纹噪点", "0~2.0", "外观"),
        slider("slashSweepSpeed", "扫开速度", "0.05~0.95", "动画"),
        slider("slashSweepSoftness", "扫开柔和度", "0.02~0.60", "动画"),
        slider("slashHoldFraction", "保持时长", "0~0.8", "动画"),
        slider("slashFadeStart", "淡出起始", "0.30~0.95", "动画"),
        slider("slashFadeDuration", "淡出时长", "0.02~0.70", "动画"),
        slider("slashSurfaceOffset", "命中点外移", "0~0.5", "动画"),
        slider("slashRollRange", "随机倾角", "0~360°", "动画"),
        switch("slashStarfield", "星空开关", "刀身黑芯内显示星点", "星空"),
        slider("slashStarDensity", "星点密度", "0.05~3.0", "星空"),
        slider("slashStarBrightness", "星点亮度", "0~3.0", "星空"),
        slider("slashStarSize", "星点大小", "0.2~5.0", "星空"),
        segment("slashStarColorMode", "颜色模式", "黑白或彩虹", "星空")
    ) + channels("slashCoreColor", "黑芯颜色", "外观") +
        channels("slashEdgeColor", "白边颜色", "外观") +
        channels("slashGlowColor", "发光颜色", "外观")

    /**
     * 武器渲染: the style selector, then only the chosen style's parameters.
     *
     * <p>Every style's rows carry the index that reveals them and the renderer honours it. The older
     * screen did the same with an `if` per block, but stated as data here it is one list the panel can
     * filter -- and a style whose rows were left ungated would offer a tunnel's field of view while
     * the sword was drawn in silk.
     */
    val RENDER: List<GodRow> = listOf(
        segment("renderStyle", "渲染风格", "星空 / 丝绸 / 隧道 / 晶格 / 原版 / 关闭", "风格")
    ) + silkPalette() + listOf(
        slider("silkBrightness", "亮度", "-1~1", "丝绸", gate = 1),
        slider("silkContrast", "对比度", "0.1~3.0", "丝绸", gate = 1),
        slider("silkSaturation", "饱和度", "0~3.0", "丝绸", gate = 1),
        slider("silkScale", "图案大小", "0.2~8.0", "丝绸", gate = 1),
        slider("silkIntensity", "流动强度", "0~3.0", "丝绸", gate = 1),
        slider("silkWarp", "扭曲", "0~2.0", "丝绸", gate = 1),
        slider("silkDetail", "细节", "0.5~8.0", "丝绸", gate = 1),
        slider("silkHue", "色相", "0~360°", "丝绸", gate = 1),
        slider("silkSeed", "种子", "0~20000", "丝绸", gate = 1),
        slider("silkRotation", "旋转", "-360~360°", "丝绸", gate = 1),
        slider("silkDrift", "漂移", "0~3.0", "丝绸", gate = 1),
        slider("silkVignette", "暗角", "0~1.0", "丝绸", gate = 1),
        slider("silkBlur", "柔化", "0~0.05", "丝绸", gate = 1),
        slider("silkGrain", "颗粒", "0~1.0", "丝绸", gate = 1),
        slider("tunnelSpeed", "速度", "0.1~3.0", "隧道", gate = 2),
        slider("tunnelBrightness", "亮度", "0.1~3.0", "隧道", gate = 2),
        slider("tunnelDensity", "密度", "0.2~3.0", "隧道", gate = 2),
        slider("tunnelFov", "视野", "1.0~16.0", "隧道", gate = 2),
        slider("voronoiColorCount", "颜色数量", "1~5", "晶格", gate = 3),
        slider("voronoiStepsPerColor", "颜色步进", "1~3", "晶格", gate = 3),
        slider("voronoiDistortion", "扭曲", "0~0.5", "晶格", gate = 3),
        slider("voronoiGap", "间隙", "0~0.1", "晶格", gate = 3),
        slider("voronoiGlow", "内部辉光", "0~1.0", "晶格", gate = 3),
        slider("voronoiScale", "缩放", "0.05~4.0", "晶格", gate = 3),
        slider("voronoiFov", "视野", "1.0~16.0", "晶格", gate = 3),
        slider("voronoiSpeed", "动画速度", "0~3.0", "晶格", gate = 3),
        slider("voronoiRotation", "旋转", "-360~360°", "晶格", gate = 3),
        slider("voronoiOffsetX", "横向偏移", "-1~1", "晶格", gate = 3),
        slider("voronoiOffsetY", "纵向偏移", "-1~1", "晶格", gate = 3)
    ) + voronoiColours()

    /**
     * 回溯: the whole feature, in the order the older screen used.
     *
     * <p>Long, but it is a long feature, and the ordering -- switches, scope, playback, the safety
     * group, then what gets restored -- is the older screen's. That order is worth keeping: it is the
     * order the settings were designed in, not an alphabetical sort.
     */
    val REWIND: List<GodRow> = listOf(
        switch("rewindEnabled", "回溯开关", "启用时间回溯能力", "开关"),
        switch("rewindDeathEnabled", "死亡回溯", "死亡时自动回溯", "开关"),
        slider("rewindWindowSeconds", "回溯窗口", "回退到多少秒前", "窗口"),
        // Stored in ticks, shown in seconds: the factor lives here rather than at the call site,
        // which is where the older screen divided on read and multiplied on write.
        slider("rewindCooldownTicks", "主动冷却", "主动回溯冷却秒数", "窗口", scale = 20f),
        slider("rewindDeathCooldownTicks", "死亡冷却", "死亡回溯冷却秒数", "窗口", scale = 20f),
        slider("rewindDeathMaxRetries", "连续回溯上限", "0 表示直到窗口开头", "窗口"),
        segment("rewindScopeMode", "范围模式", "全维度或当前维度", "范围"),
        segment("rewindScope", "区块模式", "全区块或半径", "范围"),
        slider("rewindRadius", "半径", "半径模式的半径", "范围"),
        segment("rewindPlaybackMode", "还原方式", "渐进或瞬间", "播放"),
        slider("rewindPlaybackSeconds", "播放时长", "渐进还原播放秒数", "播放"),
        segment("rewindRestoreOrder", "还原顺序", "先方块或先实体", "播放"),
        segment("rewindCameraMode", "镜头模式", "跟随或自由飞行", "播放"),
        switch("rewindFreeCamRestorePosition", "自由镜头回位置", "B 模式结束回到原位置", "播放"),
        switch("rewindShowStats", "还原统计", "回溯结束显示还原数量提示", "播放"),
        switch("rewindPlayerState", "玩家状态回退", "生命/饥饿/经验/背包等", "玩家"),
        switch("rewindPositionRewind", "主动位置回退", "主动回溯结束时回位置", "玩家"),
        segment("rewindPositionMode", "回位方式", "瞬间或平滑", "玩家"),
        switch("rewindSafetyCheckpoint", "安全检查点", "死亡回溯挑安全时刻", "安全"),
        switch("rewindHostileCheck", "敌对检测", "安全检查点检测附近敌对", "安全"),
        switch("rewindOtherItemDeduct", "扣除队友拿走", "防复制扣除其他玩家拿走物品", "安全"),
        switch("rewindFreezeOthers", "播放冻结队友", "播放期间其他玩家冻结", "安全"),
        switch("rewindTimestopStacking", "时停叠加", "允许回溯与时停叠加", "安全"),
        switch("rewindBlocks", "方块", "恢复方块状态", "还原内容"),
        switch("rewindBlockEntities", "方块实体", "恢复箱子/熔炉等 NBT", "还原内容"),
        switch("rewindEntities", "实体", "恢复实体位置/血量/复活", "还原内容"),
        switch("rewindItems", "掉落物", "恢复掉落物", "还原内容"),
        switch("rewindExperience", "经验球", "恢复经验球", "还原内容"),
        switch("rewindTime", "时间", "恢复时间", "还原内容"),
        switch("rewindWeather", "天气", "恢复天气", "还原内容"),
        switch("rewindRaids", "袭击", "恢复袭击", "还原内容"),
        switch("rewindScoreboard", "记分板", "恢复记分板", "还原内容"),
        switch("rewindWorldBorder", "世界边界", "恢复世界边界", "还原内容")
    )

    // ---- construction helpers -------------------------------------------------------------------

    private fun switch(id: String, name: String, desc: String, section: String, gate: Int = -1) =
        GodRow(V9Row(id, name, desc, TYPE_SWITCH, section, desc), gate)

    private fun segment(id: String, name: String, desc: String, section: String, gate: Int = -1) =
        GodRow(V9Row(id, name, desc, TYPE_SEGMENT, section, desc), gate)

    private fun slider(
        id: String,
        name: String,
        desc: String,
        section: String,
        gate: Int = -1,
        scale: Float = 0f
    ) = GodRow(V9Row(id, name, desc, TYPE_SLIDER, section, desc), gate, scale)

    /** The rows of the named `v9Sections` sections, in the order they are listed. */
    private fun sections(vararg names: String): List<GodRow> {
        val wanted = names.toSet()
        return v9Sections.filter { it.name in wanted }
            .flatMap { it.rows }
            .map { GodRow(it) }
    }

    /** One packed colour's channels, as 0..255 sliders bound to that colour's bytes. */
    private fun channels(field: String, label: String, section: String, gate: Int = -1, letters: String = "RGB") =
        letters.map { ch ->
            GodRow(
                row = V9Row("$field$ch", "$label $ch", "通道 $ch", TYPE_CHANNEL, section, "通道 $ch"),
                gate = gate,
                channel = ch,
                colourField = field
            )
        }

    /**
     * The silk palette's eight colours.
     *
     * <p>Generated because the older screen's eight blocks are identical but for their index, and
     * eight hand-written blocks is exactly where a ninth colour would quietly never appear.
     */
    private fun silkPalette(): List<GodRow> =
        (0..7).flatMap { index -> channels("silkColor$index", "颜色 ${index + 1}", "丝绸", gate = 1) }

    /**
     * 晶格's five colours plus its glow and gap.
     *
     * <p>The glow and gap carry an alpha channel too -- the older screen exposed it -- so they are the
     * only colours here that are not RGB.
     */
    private fun voronoiColours(): List<GodRow> =
        (0..4).flatMap { index -> channels("voronoiColor$index", "颜色 ${index + 1}", "晶格", gate = 3) } +
            channels("voronoiColorGlow", "内部辉光色", "晶格", gate = 3, letters = "RGBA") +
            channels("voronoiColorGap", "间隙颜色", "晶格", gate = 3, letters = "RGBA")
}
