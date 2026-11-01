package com.yuan.client.aurora.panels

import com.yuan.client.gui.V9Holder
import java.util.Locale

/**
 * Reads and writes the god sword's real configuration through the existing state holder.
 *
 * <p>Every dispatch here mirrors a table that already exists in the older screen -- its row renderer
 * holds the ranges, labels and formatters, and its composition root holds the read/write dispatch.
 * Mirroring rather than sharing is deliberate: extracting those tables would mean editing code the
 * player's existing interface depends on, and the rule for this project is to change only what was
 * asked for. The duplication is mechanical and bounded to the rows the rail actually shows.
 *
 * <p>The alternative -- a second config model owned by the new interface -- was rejected outright. It
 * would have to re-implement the write, the live preview, the preset list and the server sync, and
 * the first of the four to be subtly wrong would corrupt a saved sword.
 */
internal object GodSwordBinding {

    /** What kind of control a row needs. Matches the older model's own numbering. */
    internal const val TYPE_SWITCH = 0
    internal const val TYPE_SEGMENT = 1
    internal const val TYPE_SLIDER = 2
    internal const val TYPE_SWATCH = 3
    internal const val TYPE_PRESET = 4

    // ---- reads -------------------------------------------------------------------------------

    /** The boolean a switch row shows. Unknown ids answer false rather than throwing. */
    internal fun readBool(id: String, s: V9Holder): Boolean = when (id) {
        "enabled" -> s.enabled
        "invulnerable" -> s.invulnerable
        "grayScreen" -> s.grayScreen
        "grayAnimate" -> s.grayAnimate
        "soundEnabled" -> s.soundEnabled
        "freezeSelf" -> s.freezeSelf
        "freezeEntities" -> s.freezeEntities
        "freezeBlocks" -> s.freezeBlocks
        "freezeFluids" -> s.freezeFluids
        "freezeBossAI" -> s.freezeBossAI
        "showMessage" -> s.showMessage
        "domainStarfield" -> s.domainStarfield
        "domainStarSpikes" -> s.domainStarSpikes
        "slashEnabled" -> s.slashEnabled
        "slashDepthTest" -> s.slashDepthTest
        "slashRandomAngle" -> s.slashRandomAngle
        "slashGlow" -> s.slashGlow
        "slashStarfield" -> s.slashStarfield
        "rewindEnabled" -> s.rewindEnabled
        "rewindBlocks" -> s.rewindBlocks
        "rewindEntities" -> s.rewindEntities
        "rewindItems" -> s.rewindItems
        "rewindTime" -> s.rewindTime
        "rewindPlayerState" -> s.rewindPlayerState
        "rewindDeathEnabled" -> s.rewindDeathEnabled
        "rewindSafetyCheckpoint" -> s.rewindSafetyCheckpoint
        "rewindHostileCheck" -> s.rewindHostileCheck
        "rewindShowStats" -> s.rewindShowStats
        "rewindFreeCamRestorePosition" -> s.rewindFreeCamRestorePosition
        "rewindOtherItemDeduct" -> s.rewindOtherItemDeduct
        "rewindPositionRewind" -> s.rewindPositionRewind
        "rewindRaids" -> s.rewindRaids
        "rewindScoreboard" -> s.rewindScoreboard
        "rewindTimestopStacking" -> s.rewindTimestopStacking
        "rewindWorldBorder" -> s.rewindWorldBorder
        "rewindBlockEntities" -> s.rewindBlockEntities
        "rewindExperience" -> s.rewindExperience
        "rewindWeather" -> s.rewindWeather
        "rewindFreezeOthers" -> s.rewindFreezeOthers
        else -> false
    }

    /** The numeric value a slider row shows. */
    internal fun readFloat(id: String, s: V9Holder): Float = when (id) {
        "domainStarDensity" -> s.domainStarDensity
        "domainSpinScale" -> s.domainSpinScale
        "startDuration" -> s.startDuration
        "endDuration" -> s.endDuration
        "grayStrength" -> s.grayStrength
        "particleSize" -> s.particleSize
        "particleAlpha" -> s.particleAlpha
        "particleSpin" -> s.particleSpin
        "particleCount" -> s.particleCount.toFloat()
        "stopDuration" -> s.stopDuration
        "cooldown" -> s.cooldown.toFloat()
        "soundVolume" -> s.soundVolume
        "stopRadius" -> s.stopRadius
        "slashDuration" -> s.slashDuration
        "slashLengthMult" -> s.slashLengthMult
        "slashWidthRatio" -> s.slashWidthRatio
        "slashThicknessRatio" -> s.slashThicknessRatio
        "slashTipFade" -> s.slashTipFade
        "slashCoreWidth" -> s.slashCoreWidth
        "slashCoreShade" -> s.slashCoreShade
        "slashEdgeWidth" -> s.slashEdgeWidth
        "slashEdgeBrightness" -> s.slashEdgeBrightness
        "slashGlowWidth" -> s.slashGlowWidth
        "slashGlowStrength" -> s.slashGlowStrength
        "slashNoiseStrength" -> s.slashNoiseStrength
        "slashSweepSpeed" -> s.slashSweepSpeed
        "slashSweepSoftness" -> s.slashSweepSoftness
        "slashHoldFraction" -> s.slashHoldFraction
        "slashFadeStart" -> s.slashFadeStart
        "slashFadeDuration" -> s.slashFadeDuration
        "slashSurfaceOffset" -> s.slashSurfaceOffset
        "slashRollRange" -> s.slashRollRange
        "slashStartScale" -> s.slashStartScale
        "slashEndScale" -> s.slashEndScale
        "rewindDeathCooldownTicks" -> s.rewindDeathCooldownTicks.toFloat()
        "voronoiColorCount" -> s.voronoiColorCount.toFloat()
        "voronoiStepsPerColor" -> s.voronoiStepsPerColor.toFloat()
        "slashStarDensity" -> s.slashStarDensity
        "slashStarBrightness" -> s.slashStarBrightness
        "slashStarSize" -> s.slashStarSize
        "silkBrightness" -> s.silkBrightness
        "silkContrast" -> s.silkContrast
        "silkSaturation" -> s.silkSaturation
        "silkScale" -> s.silkScale
        "silkIntensity" -> s.silkIntensity
        "silkWarp" -> s.silkWarp
        "silkDetail" -> s.silkDetail
        "silkHue" -> s.silkHue
        "silkSeed" -> s.silkSeed
        "silkRotation" -> s.silkRotation
        "silkDrift" -> s.silkDrift
        "silkVignette" -> s.silkVignette
        "silkBlur" -> s.silkBlur
        "silkGrain" -> s.silkGrain
        "tunnelSpeed" -> s.tunnelSpeed
        "tunnelBrightness" -> s.tunnelBrightness
        "tunnelDensity" -> s.tunnelDensity
        "tunnelFov" -> s.tunnelFov
        "voronoiDistortion" -> s.voronoiDistortion
        "voronoiGap" -> s.voronoiGap
        "voronoiGlow" -> s.voronoiGlow
        "voronoiScale" -> s.voronoiScale
        "voronoiFov" -> s.voronoiFov
        "voronoiSpeed" -> s.voronoiSpeed
        "voronoiRotation" -> s.voronoiRotation
        "voronoiOffsetX" -> s.voronoiOffsetX
        "voronoiOffsetY" -> s.voronoiOffsetY
        "rewindWindowSeconds" -> s.rewindWindowSeconds.toFloat()
        "rewindRadius" -> s.rewindRadius
        "rewindCooldownTicks" -> s.rewindCooldownTicks.toFloat()
        "rewindPlaybackSeconds" -> s.rewindPlaybackSeconds
        "rewindDeathMaxRetries" -> s.rewindDeathMaxRetries.toFloat()
        else -> 0f
    }

    /** The option index a segment row shows. */
    internal fun readIndex(id: String, s: V9Holder): Int = when (id) {
        "startAnim" -> s.startAnim
        "endAnim" -> s.endAnim
        "triggerMode" -> s.triggerMode
        "grayStyle" -> s.grayStyle
        "slashStarColorMode" -> s.slashStarColorMode
        "renderStyle" -> com.yuan.client.gui.renderStyleUiIndex(s.renderStyle)
        "rewindScopeMode" -> s.rewindScopeMode
        "rewindScope" -> s.rewindScope
        "rewindPlaybackMode" -> s.rewindPlaybackMode
        "rewindRestoreOrder" -> s.rewindRestoreOrder
        "rewindCameraMode" -> s.rewindCameraMode
        "rewindPositionMode" -> s.rewindPositionMode
        else -> 0
    }

    // ---- writes ------------------------------------------------------------------------------

    /** Flips a switch row's boolean. */
    internal fun writeBool(id: String, s: V9Holder, value: Boolean) {
        when (id) {
            "enabled" -> s.enabled = value
            "invulnerable" -> s.invulnerable = value
            "grayScreen" -> s.grayScreen = value
            "grayAnimate" -> s.grayAnimate = value
            "soundEnabled" -> s.soundEnabled = value
            "freezeSelf" -> s.freezeSelf = value
            "freezeEntities" -> s.freezeEntities = value
            "freezeBlocks" -> s.freezeBlocks = value
            "freezeFluids" -> s.freezeFluids = value
            "freezeBossAI" -> s.freezeBossAI = value
            "showMessage" -> s.showMessage = value
            "domainStarfield" -> s.domainStarfield = value
            "domainStarSpikes" -> s.domainStarSpikes = value
            "slashEnabled" -> s.slashEnabled = value
            "slashDepthTest" -> s.slashDepthTest = value
            "slashRandomAngle" -> s.slashRandomAngle = value
            "slashGlow" -> s.slashGlow = value
            "slashStarfield" -> s.slashStarfield = value
            "rewindEnabled" -> s.rewindEnabled = value
            "rewindBlocks" -> s.rewindBlocks = value
            "rewindEntities" -> s.rewindEntities = value
            "rewindItems" -> s.rewindItems = value
            "rewindTime" -> s.rewindTime = value
            "rewindPlayerState" -> s.rewindPlayerState = value
            "rewindDeathEnabled" -> s.rewindDeathEnabled = value
            "rewindSafetyCheckpoint" -> s.rewindSafetyCheckpoint = value
            "rewindHostileCheck" -> s.rewindHostileCheck = value
            "rewindShowStats" -> s.rewindShowStats = value
            "rewindFreeCamRestorePosition" -> s.rewindFreeCamRestorePosition = value
            "rewindOtherItemDeduct" -> s.rewindOtherItemDeduct = value
            "rewindPositionRewind" -> s.rewindPositionRewind = value
            "rewindRaids" -> s.rewindRaids = value
            "rewindScoreboard" -> s.rewindScoreboard = value
            "rewindTimestopStacking" -> s.rewindTimestopStacking = value
            "rewindWorldBorder" -> s.rewindWorldBorder = value
            "rewindBlockEntities" -> s.rewindBlockEntities = value
            "rewindExperience" -> s.rewindExperience = value
            "rewindWeather" -> s.rewindWeather = value
            "rewindFreezeOthers" -> s.rewindFreezeOthers = value
        }
    }

    /** Stores a slider row's value. Integer fields are rounded rather than truncated. */
    internal fun writeFloat(id: String, s: V9Holder, value: Float) {
        val asInt = Math.round(value)
        when (id) {
            "domainStarDensity" -> s.domainStarDensity = value
            "domainSpinScale" -> s.domainSpinScale = value
            "startDuration" -> s.startDuration = value
            "endDuration" -> s.endDuration = value
            "grayStrength" -> s.grayStrength = value
            "particleSize" -> s.particleSize = value
            "particleAlpha" -> s.particleAlpha = value
            "particleSpin" -> s.particleSpin = value
            "particleCount" -> s.particleCount = asInt
            "stopDuration" -> s.stopDuration = value
            "cooldown" -> s.cooldown = asInt
            "soundVolume" -> s.soundVolume = value
            "stopRadius" -> s.stopRadius = value
            "slashDuration" -> s.slashDuration = value
            "slashLengthMult" -> s.slashLengthMult = value
            "slashWidthRatio" -> s.slashWidthRatio = value
            "slashThicknessRatio" -> s.slashThicknessRatio = value
            "slashTipFade" -> s.slashTipFade = value
            "slashCoreWidth" -> s.slashCoreWidth = value
            "slashCoreShade" -> s.slashCoreShade = value
            "slashEdgeWidth" -> s.slashEdgeWidth = value
            "slashEdgeBrightness" -> s.slashEdgeBrightness = value
            "slashGlowWidth" -> s.slashGlowWidth = value
            "slashGlowStrength" -> s.slashGlowStrength = value
            "slashNoiseStrength" -> s.slashNoiseStrength = value
            "slashSweepSpeed" -> s.slashSweepSpeed = value
            "slashSweepSoftness" -> s.slashSweepSoftness = value
            "slashHoldFraction" -> s.slashHoldFraction = value
            "slashFadeStart" -> s.slashFadeStart = value
            "slashFadeDuration" -> s.slashFadeDuration = value
            "slashSurfaceOffset" -> s.slashSurfaceOffset = value
            "slashRollRange" -> s.slashRollRange = value
            "slashStartScale" -> s.slashStartScale
        "slashEndScale" -> s.slashEndScale
        "rewindDeathCooldownTicks" -> s.rewindDeathCooldownTicks.toFloat()
        "voronoiColorCount" -> s.voronoiColorCount.toFloat()
        "voronoiStepsPerColor" -> s.voronoiStepsPerColor.toFloat()
        "slashStarDensity" -> s.slashStarDensity = value
            "slashStarBrightness" -> s.slashStarBrightness = value
            "slashStarSize" -> s.slashStarSize = value
            "silkBrightness" -> s.silkBrightness = value
            "silkContrast" -> s.silkContrast = value
            "silkSaturation" -> s.silkSaturation = value
            "silkScale" -> s.silkScale = value
            "silkIntensity" -> s.silkIntensity = value
            "silkWarp" -> s.silkWarp = value
            "silkDetail" -> s.silkDetail = value
            "silkHue" -> s.silkHue = value
            "silkSeed" -> s.silkSeed = value
            "silkRotation" -> s.silkRotation = value
            "silkDrift" -> s.silkDrift = value
            "silkVignette" -> s.silkVignette = value
            "silkBlur" -> s.silkBlur = value
            "silkGrain" -> s.silkGrain = value
            "tunnelSpeed" -> s.tunnelSpeed = value
            "tunnelBrightness" -> s.tunnelBrightness = value
            "tunnelDensity" -> s.tunnelDensity = value
            "tunnelFov" -> s.tunnelFov = value
            "voronoiDistortion" -> s.voronoiDistortion = value
            "voronoiGap" -> s.voronoiGap = value
            "voronoiGlow" -> s.voronoiGlow = value
            "voronoiScale" -> s.voronoiScale = value
            "voronoiFov" -> s.voronoiFov = value
            "voronoiSpeed" -> s.voronoiSpeed = value
            "voronoiRotation" -> s.voronoiRotation = value
            "voronoiOffsetX" -> s.voronoiOffsetX = value
            "voronoiOffsetY" -> s.voronoiOffsetY = value
            "rewindWindowSeconds" -> s.rewindWindowSeconds = asInt
            "rewindRadius" -> s.rewindRadius = value
            "rewindCooldownTicks" -> s.rewindCooldownTicks = asInt
            "rewindPlaybackSeconds" -> s.rewindPlaybackSeconds = value
            "rewindDeathMaxRetries" -> s.rewindDeathMaxRetries = asInt
        }
    }

    /** Stores a segment row's option index. */
    internal fun writeIndex(id: String, s: V9Holder, value: Int) {
        when (id) {
            "startAnim" -> s.startAnim = value
            "endAnim" -> s.endAnim = value
            "triggerMode" -> s.triggerMode = value
            "grayStyle" -> s.grayStyle = value
            "slashStarColorMode" -> s.slashStarColorMode = value
            "renderStyle" -> s.renderStyle = com.yuan.client.gui.renderStyleStored(value)
            "rewindScopeMode" -> s.rewindScopeMode = value
            "rewindScope" -> s.rewindScope = value
            "rewindPlaybackMode" -> s.rewindPlaybackMode = value
            "rewindRestoreOrder" -> s.rewindRestoreOrder = value
            "rewindCameraMode" -> s.rewindCameraMode = value
            "rewindPositionMode" -> s.rewindPositionMode = value
        }
    }

    // ---- colour channels ----------------------------------------------------------------------

    /**
     * Reads one channel of a packed colour field.
     *
     * <p>The field names match the holder's own fields, so this is a lookup rather than a switch per
     * colour. `letters` on the row says which byte to read; the older screen hard-coded the shift at
     * each call site, which is how a red and a blue get swapped in one of eight otherwise identical
     * blocks.
     */
    internal fun readChannel(field: String, channel: Char, s: V9Holder): Float {
        val colour = colourField(field, s) ?: return 0f
        val value = when (channel) {
            'A' -> (colour ushr 24) and 0xFF
            'R' -> (colour shr 16) and 0xFF
            'G' -> (colour shr 8) and 0xFF
            else -> colour and 0xFF
        }
        return value.toFloat()
    }

    /** Writes one channel of a packed colour field, leaving its other bytes alone. */
    internal fun writeChannel(field: String, channel: Char, s: V9Holder, value: Float) {
        val current = colourField(field, s) ?: return
        val byte = (Math.round(value).coerceIn(0, 255)) and 0xFF
        val (mask, shift) = when (channel) {
            'A' -> 0x00FFFFFF to 24
            'R' -> 0xFF00FFFF.toInt() to 16
            'G' -> 0xFFFF00FF.toInt() to 8
            else -> 0xFFFFFF00.toInt() to 0
        }
        setColourField(field, s, (current and mask) or (byte shl shift))
    }

    /**
     * The default configuration, built once and never written to.
     *
     * <p>`V9State`'s field initialisers ARE the defaults, so a holder built from a bare `V9State()`
     * is the default configuration -- no second table to keep in step with the first. It is shared
     * because it is a constant: one per row would be 182 fields' worth of holders for eight rows of
     * a panel, and threading one down through three layers would put a parameter on every panel
     * signature to carry something none of them vary.
     *
     * <p>READ-ONLY BY CONTRACT. Every use is a read on the way to writing the target holder; nothing
     * may write here, or the "default" would drift into whatever was last reset and every later
     * reset would restore that instead.
     */
    private val defaultsHolder: V9Holder by lazy { V9Holder(com.yuan.client.gui.V9State()) }

    /** Puts [godRow] back to its default. See [resetRow] for what is and is not covered. */
    internal fun resetRow(godRow: GodRow, target: V9Holder): Boolean =
        resetRow(godRow, target, defaultsHolder)

    /**
     * Puts ONE row back to its default, reading that default from a freshly built state.
     *
     * <p>WHERE THE DEFAULT COMES FROM. `V9State` carries the values, and its field initialisers are
     * the defaults -- so `V9Holder(V9State())` IS the default configuration, with no second table to
     * keep in step. A separate defaults table would be a second place for a setting to be declared,
     * and the two would drift the first time one was changed.
     *
     * <p>Only the four kinds this dispatch owns are handled. Colour-swatch and preset rows are drawn
     * by the inline panel rather than through here, and their "default" is not a single value (a
     * swatch may have no default at all, and a preset row edits a whole slot). Returning false for
     * those lets the caller decide -- today it offers no reset there rather than a control that
     * silently does nothing.
     *
     * @return true when a value was actually written.
     */
    /**
     * Whether [godRow]'s value differs from its default.
     *
     * <p>THE QUESTION A PLAYER ACTUALLY ASKS. A feature with twenty-seven settings is impossible to
     * audit from a list of identical rows: after a session of tuning, nobody remembers which three
     * they changed, and "back to defaults" means restoring settings they may never have touched.
     * Marking the changed ones turns the page into a summary of that player's own configuration.
     *
     * <p>Reads the SAME field the reset writes, through the same dispatch, so the mark can never
     * disagree with the button beside it -- a row that shows as unmodified is exactly a row that
     * reset would leave alone.
     *
     * <p>Swatch and preset rows answer false deliberately: a swatch may have no meaningful default
     * and a preset row edits a whole slot, so neither has a single comparable value. The row
     * painter shows no mark for them rather than a mark that means nothing.
     */
    internal fun isModified(godRow: GodRow, current: V9Holder, defaults: V9Holder = defaultsHolder): Boolean {
        val id = godRow.row.id
        return when (godRow.row.type) {
            TYPE_SWITCH -> readBool(id, current) != readBool(id, defaults)
            TYPE_SEGMENT -> readIndex(id, current) != readIndex(id, defaults)
            // Compared in the STORED unit, like resetRow: the display factor belongs to the slider,
            // and applying it on one side only would misreport every row that has one.
            TYPE_SLIDER -> readFloat(id, current) != readFloat(id, defaults)
            GodSwordRows.TYPE_CHANNEL -> {
                val field = godRow.colourField
                val channel = godRow.channel
                if (field == null || channel == null) false
                else readChannel(field, channel, current) != readChannel(field, channel, defaults)
            }
            else -> false
        }
    }

    /**
     * How many of a module's rows differ from their defaults.
     *
     * <p>The rail shows this per feature so a player can see at a glance which of the seven they
     * have actually configured, without opening each one.
     */
    internal fun modifiedCount(rows: List<GodRow>, current: V9Holder, defaults: V9Holder = defaultsHolder): Int =
        rows.count { isModified(it, current, defaults) }

    internal fun resetRow(godRow: GodRow, target: V9Holder, defaults: V9Holder): Boolean {
        val id = godRow.row.id
        return when (godRow.row.type) {
            GodSwordRows.TYPE_SWITCH -> {
                writeBool(id, target, readBool(id, defaults))
                true
            }
            GodSwordRows.TYPE_SEGMENT -> {
                writeIndex(id, target, readIndex(id, defaults))
                true
            }
            GodSwordRows.TYPE_SLIDER -> {
                // The scale is NOT applied here: readFloat and writeFloat both speak the stored unit,
                // so a value copied between two holders needs no conversion. Applying the display
                // factor on one side only is the mistake the row's `scale` field exists to prevent.
                writeFloat(id, target, readFloat(id, defaults))
                true
            }
            GodSwordRows.TYPE_CHANNEL -> {
                val field = godRow.colourField
                val channel = godRow.channel
                if (field == null || channel == null) {
                    false
                } else {
                    writeChannel(field, channel, target, readChannel(field, channel, defaults))
                    true
                }
            }
            else -> false
        }
    }

    /**
     * The packed colour behind a channel row, or null for an unknown field.
     *
     * <p>Null rather than a default: a channel row whose field is misspelled would otherwise write
     * into an unrelated colour, and a wrong colour is far harder to notice than a row that does
     * nothing.
     */
    private fun colourField(field: String, s: V9Holder): Int? = when (field) {
        "slashCoreColor" -> s.slashCoreColor
        "slashEdgeColor" -> s.slashEdgeColor
        "slashGlowColor" -> s.slashGlowColor
        "silkColor0" -> s.silkColor0
        "silkColor1" -> s.silkColor1
        "silkColor2" -> s.silkColor2
        "silkColor3" -> s.silkColor3
        "silkColor4" -> s.silkColor4
        "silkColor5" -> s.silkColor5
        "silkColor6" -> s.silkColor6
        "silkColor7" -> s.silkColor7
        "voronoiColor0" -> s.voronoiColor0
        "voronoiColor1" -> s.voronoiColor1
        "voronoiColor2" -> s.voronoiColor2
        "voronoiColor3" -> s.voronoiColor3
        "voronoiColor4" -> s.voronoiColor4
        "voronoiColorGlow" -> s.voronoiColorGlow
        "voronoiColorGap" -> s.voronoiColorGap
        else -> null
    }

    private fun setColourField(field: String, s: V9Holder, value: Int) {
        when (field) {
            "slashCoreColor" -> s.slashCoreColor = value
            "slashEdgeColor" -> s.slashEdgeColor = value
            "slashGlowColor" -> s.slashGlowColor = value
            "silkColor0" -> s.silkColor0 = value
            "silkColor1" -> s.silkColor1 = value
            "silkColor2" -> s.silkColor2 = value
            "silkColor3" -> s.silkColor3 = value
            "silkColor4" -> s.silkColor4 = value
            "silkColor5" -> s.silkColor5 = value
            "silkColor6" -> s.silkColor6 = value
            "silkColor7" -> s.silkColor7 = value
            "voronoiColor0" -> s.voronoiColor0 = value
            "voronoiColor1" -> s.voronoiColor1 = value
            "voronoiColor2" -> s.voronoiColor2 = value
            "voronoiColor3" -> s.voronoiColor3 = value
            "voronoiColor4" -> s.voronoiColor4 = value
            "voronoiColorGlow" -> s.voronoiColorGlow = value
            "voronoiColorGap" -> s.voronoiColorGap = value
        }
    }

    // ---- control metadata ---------------------------------------------------------------------

    /** A slider's bounds. Symmetric bounds where the older screen used them, so a knob starts centred. */
    internal fun rangeOf(id: String): ClosedFloatingPointRange<Float> = when (id) {
        // A colour channel is a byte, so its bounds do not come from the packed colour's own range.
        "channel" -> 0f..255f
        "grayStrength", "soundVolume", "particleAlpha" -> 0f..1f
        "particleSize" -> 0.4f..3f
        "stopDuration" -> 0f..60f
        "cooldown" -> 0f..100f
        "particleCount" -> 1f..16f
        "particleSpin" -> 0f..3f
    // The field's own appearance. Density below 0.2 is an almost empty floor and spin above 3 is
    // nausea at the horizon, so the sliders cannot reach past what the shader was tuned for.
    "domainStarDensity" -> 0.2f..1f
    "domainSpinScale" -> 0f..3f
        "stopRadius" -> 0f..128f
        "startDuration", "endDuration" -> 0.1f..2f
        "slashDuration" -> 0.05f..1f
        "slashLengthMult" -> 1f..5f
        "slashWidthRatio" -> 0.02f..0.3f
        "slashThicknessRatio" -> 0.005f..0.1f
        "slashTipFade" -> 0f..0.5f
        "slashCoreWidth" -> 0.3f..0.98f
        "slashCoreShade" -> 0f..1f
        "slashEdgeWidth" -> 0.5f..0.99f
        "slashEdgeBrightness" -> 0.1f..2f
        "slashGlowWidth" -> 0.6f..0.99f
        "slashGlowStrength" -> 0f..3f
        "slashNoiseStrength" -> 0f..2f
        "slashSweepSpeed" -> 0.05f..0.95f
        "slashSweepSoftness" -> 0.02f..0.6f
        "slashHoldFraction" -> 0f..0.8f
        "slashFadeStart" -> 0f..1f
        "slashFadeDuration" -> 0f..1f
        "slashSurfaceOffset" -> 0f..1f
        "slashRollRange" -> 0f..360f
        "slashStarDensity" -> 0.05f..3f
        "slashStarBrightness" -> 0f..3f
        "slashStarSize" -> 0.2f..5f
        "silkBrightness" -> -1f..1f
        "silkContrast" -> 0.1f..3f
        "silkSaturation" -> 0f..3f
        "silkScale" -> 0.2f..8f
        "silkIntensity" -> 0f..3f
        "silkWarp" -> 0f..2f
        "silkDetail" -> 0.5f..8f
        "silkHue" -> 0f..6.283f
        "silkSeed" -> 0f..20000f
        "silkRotation" -> -6.283f..6.283f
        "silkDrift" -> 0f..3f
        "silkVignette" -> 0f..1f
        "silkBlur" -> 0f..0.05f
        "silkGrain" -> 0f..1f
        "tunnelSpeed" -> 0.1f..3f
        "tunnelBrightness" -> 0.1f..3f
        "tunnelDensity" -> 0.2f..3f
        "tunnelFov" -> 1f..16f
        "voronoiDistortion" -> 0f..0.5f
        "voronoiGap" -> 0f..0.1f
        "voronoiGlow" -> 0f..1f
        "voronoiScale" -> 0.05f..4f
        "voronoiFov" -> 1f..16f
        "voronoiSpeed" -> 0f..3f
        "voronoiRotation" -> -6.283f..6.283f
        "voronoiOffsetX", "voronoiOffsetY" -> -1f..1f
        "rewindWindowSeconds" -> 1f..60f
        "rewindRadius" -> 0f..1024f
        "rewindCooldownTicks" -> 0f..600f
        "rewindPlaybackSeconds" -> 1f..60f
        "rewindDeathMaxRetries" -> 0f..100f
        else -> 0f..1f
    }

    /**
     * A slider's step.
     *
     * <p>Derived from the range rather than declared per row: a step is only ever chosen so a value
     * reads cleanly, and a row whose range spans 0..1 wants hundredths while one spanning 0..20000
     * wants units. Deriving it keeps a new row from silently landing on a step that makes its readout
     * show three decimals for a seed.
     */
    internal fun stepOf(id: String): Float {
        val range = rangeOf(id)
        val span = range.endInclusive - range.start
        return when {
            id == "particleCount" || id == "cooldown" || id == "rewindWindowSeconds" -> 1f
            id == "rewindCooldownTicks" || id == "rewindPlaybackSeconds" || id == "rewindDeathMaxRetries" -> 1f
            id == "silkSeed" -> 10f
            span <= 0.5f -> 0.005f
            span <= 2f -> 0.01f
            span <= 10f -> 0.05f
            span <= 100f -> 1f
            else -> 10f
        }
    }

    /** A segment row's labels, matching the older screen's wording exactly. */
    internal fun labelsOf(id: String): List<String> = when (id) {
        "startAnim" -> listOf("爆开", "冲击波", "无")
        "endAnim" -> listOf("收束", "无")
        "triggerMode" -> listOf("右键", "Shift + 右键", "自定义键")
        "grayStyle" -> listOf("纯黑白", "复古", "暗角")
        "slashStarColorMode" -> listOf("单色", "彩虹", "星色")
        "renderStyle" -> listOf("星空", "丝绸", "隧道", "沃罗诺伊")
        "rewindScopeMode" -> listOf("全场", "范围")
        "rewindScope" -> listOf("自身", "队伍")
        "rewindPlaybackMode" -> listOf("瞬时", "回放")
        "rewindRestoreOrder" -> listOf("先方块", "先实体")
        "rewindCameraMode" -> listOf("保持", "跟随")
        "rewindPositionMode" -> listOf("原地", "记录点")
        else -> listOf("开", "关")
    }

    /** A slider's readout, matching the older screen's units. */
    internal fun formatOf(id: String): (Float) -> String = when (id) {
        "grayStrength", "soundVolume", "particleAlpha", "slashCoreShade" ->
            { v -> String.format(Locale.ROOT, "%.0f%%", v * 100f) }
        "particleSize", "particleAlpha", "particleSpin", "slashLengthMult", "slashTipFade" ->
            { v -> String.format(Locale.ROOT, "%.2f", v) }
        "stopDuration" -> { v -> if (v <= 0f) "手动" else String.format(Locale.ROOT, "%.1fs", v) }
        "cooldown" -> { v -> "${v.toInt()} tick" }
        "particleCount" -> { v -> "${v.toInt()} 个" }
        "stopRadius" -> { v -> if (v <= 0f) "全场" else String.format(Locale.ROOT, "%.0f 格", v) }
        "startDuration", "endDuration" -> { v -> String.format(Locale.ROOT, "%.2fs", v) }
        "silkHue" -> { v -> String.format(Locale.ROOT, "%.0f°", v * 57.29578f) }
        "silkSeed" -> { v -> String.format(Locale.ROOT, "%.0f", v) }
        "silkRotation", "voronoiRotation", "slashRollRange" ->
            { v -> String.format(Locale.ROOT, "%.0f°", v * 57.29578f) }
        "rewindWindowSeconds", "rewindPlaybackSeconds" -> { v -> "${v.toInt()}s" }
        "rewindCooldownTicks" -> { v -> "${v.toInt()} tick" }
        "rewindRadius" -> { v -> if (v <= 0f) "全场" else String.format(Locale.ROOT, "%.0f 格", v) }
        "rewindDeathMaxRetries" -> { v -> "${v.toInt()} 次" }
        else -> { v -> String.format(Locale.ROOT, "%.2f", v) }
    }
}
