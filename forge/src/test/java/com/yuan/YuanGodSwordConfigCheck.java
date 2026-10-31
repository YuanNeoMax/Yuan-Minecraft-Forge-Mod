package com.yuan;

import com.yuan.item.YuanGodSwordConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Equivalence guard for {@link YuanGodSwordConfig}.
 *
 * <p>Five methods (reset/copyFrom/sameAs/read/write) each spelled out all 166 fields by hand, ~884
 * lines of pure mirroring. Collapsing them onto one field table is a large mechanical change to NBT
 * persistence, so this check pins down the behaviour that must not move.
 *
 * <p>It is written against reflection, not against the field list in the source, so it keeps
 * working after the refactor and fails loudly if the field table ever drops or renames a field.
 *
 * <p>What it asserts:
 * <ol>
 *   <li>{@code reset()} leaves every field at the value its own declaration initialises it to, so
 *       collapsing reset onto a fresh instance is safe;</li>
 *   <li>{@code copyFrom} copies every field, and {@code sameAs} agrees with a reflective compare;</li>
 *   <li>a write/read round trip preserves every field;</li>
 *   <li>every field is actually persisted -- a field the writer forgets is a silent save loss;</li>
 *   <li>the clamp ranges survive the round trip for the fields that have them.</li>
 * </ol>
 */
public final class YuanGodSwordConfigCheck {

    public static void main(String[] args) throws Exception {
        allowRegistryInitialization();

        List<Field> fields = instanceFields();

        checkResetMatchesInitializers(fields);
        checkCopyAndSameAs(fields);
        checkRoundTrip(fields);
        checkEveryFieldIsPersisted(fields);
        checkClampsSurviveRoundTrip();
        checkLegacyGrayIntensityFallback();
        checkPartialTagKeepsDefaults(fields);

        System.out.println("YuanGodSwordConfigCheck OK (" + fields.size() + " fields)");
    }

    /** ItemStack/Items need the registry bootstrapped before they can be constructed. */
    private static void allowRegistryInitialization() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError forgeNetworkBootstrapFailure) {
            // Forge 47.4.20 initializes registries before its standalone NetworkEvent bootstrap fails.
        }
    }

    /** All non-static instance fields, in declaration order. */
    private static List<Field> instanceFields() {
        List<Field> out = new ArrayList<>();
        for (Field f : YuanGodSwordConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (f.isSynthetic()) continue;
            f.setAccessible(true);
            out.add(f);
        }
        return out;
    }

    /**
     * reset() must reproduce the declaration-time initialisers exactly. If this holds, reset() can
     * be expressed as "copy from a fresh instance" and can never drift when a field is added.
     */
    private static void checkResetMatchesInitializers(List<Field> fields) throws Exception {
        YuanGodSwordConfig fresh = new YuanGodSwordConfig();
        YuanGodSwordConfig reset = new YuanGodSwordConfig();
        // Dirty every field first, so a reset that silently does nothing cannot pass.
        dirty(reset, fields);
        reset.reset();

        List<String> drifted = new ArrayList<>();
        for (Field f : fields) {
            Object expected = f.get(fresh);
            Object actual = f.get(reset);
            if (!java.util.Objects.equals(expected, actual)) {
                drifted.add(f.getName() + ": initial=" + expected + " after reset=" + actual);
            }
        }
        assert drifted.isEmpty()
                : "reset() no longer matches the field initialisers, so it cannot be collapsed:\n  "
                        + String.join("\n  ", drifted);
    }

    /** copyFrom must carry every field; sameAs must agree with a reflective comparison. */
    private static void checkCopyAndSameAs(List<Field> fields) throws Exception {
        YuanGodSwordConfig source = new YuanGodSwordConfig();
        dirty(source, fields);

        YuanGodSwordConfig target = new YuanGodSwordConfig();
        target.copyFrom(source);
        for (Field f : fields) {
            assert java.util.Objects.equals(f.get(source), f.get(target))
                    : "copyFrom dropped field " + f.getName();
        }
        assert target.sameAs(source) : "sameAs must report equal after copyFrom";

        // sameAs must notice a change in any single field.
        for (Field f : fields) {
            YuanGodSwordConfig mutated = new YuanGodSwordConfig();
            mutated.copyFrom(source);
            mutate(f, mutated);
            assert !mutated.sameAs(source)
                    : "sameAs ignores field " + f.getName() + ", so a change to it looks like no change";
        }
    }

    /** write -> read must reproduce the whole config. */
    private static void checkRoundTrip(List<Field> fields) throws Exception {
        YuanGodSwordConfig source = new YuanGodSwordConfig();
        dirty(source, fields);
        clampIntoRange(source, fields);

        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        source.write(stack);

        YuanGodSwordConfig back = new YuanGodSwordConfig();
        back.read(stack);

        List<String> lost = new ArrayList<>();
        for (Field f : fields) {
            Object a = f.get(source);
            Object b = f.get(back);
            if (!java.util.Objects.equals(a, b)) lost.add(f.getName() + ": wrote " + a + ", read " + b);
        }
        assert lost.isEmpty() : "write/read round trip lost fields:\n  " + String.join("\n  ", lost);
    }

    /**
     * Every instance field must appear in the serialised tag. This is the check that catches the
     * real failure mode of a table-driven rewrite: a field left out of the table persists as its
     * default forever, and the player's setting silently reverts on relog.
     */
    private static void checkEveryFieldIsPersisted(List<Field> fields) throws Exception {
        YuanGodSwordConfig source = new YuanGodSwordConfig();
        dirty(source, fields);
        clampIntoRange(source, fields);

        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        source.write(stack);
        CompoundTag tag = stack.getTagElement(YuanGodSwordConfig.TAG);
        assert tag != null : "write() produced no tag";

        // A field whose value differs from a fresh instance but that survives a round trip as the
        // default value is a field the writer skipped.
        YuanGodSwordConfig fresh = new YuanGodSwordConfig();
        YuanGodSwordConfig back = new YuanGodSwordConfig();
        back.read(stack);

        List<String> notPersisted = new ArrayList<>();
        for (Field f : fields) {
            Object wrote = f.get(source);
            Object freshValue = f.get(fresh);
            Object readBack = f.get(back);
            // written value differs from default, but came back as the default -> never serialised
            if (!java.util.Objects.equals(wrote, freshValue) && java.util.Objects.equals(readBack, freshValue)) {
                notPersisted.add(f.getName() + ": wrote " + wrote + " but read back default " + readBack);
            }
        }
        assert notPersisted.isEmpty()
                : "these fields are not persisted by write():\n  " + String.join("\n  ", notPersisted);
    }

    /**
     * Values outside a field's clamp range must survive the round trip the way the config intends.
     *
     * <p>{@code clamp()} is deliberately loose: it widens to {@code max*100} (or {@code min*100})
     * rather than snapping to the slider range, because the UI is allowed to accept values outside
     * the slider bounds -- see the note in the handoff doc about {@code V9EditableValue}. So a
     * "clamp" here is a sanity bound, not a slider bound, and asserting the tight range would be
     * asserting something the config never promised.
     *
     * <p>What must hold: non-finite input falls back to {@code min}, and the loose bound really does
     * stop runaway values.
     */
    private static void checkClampsSurviveRoundTrip() throws Exception {
        YuanGodSwordConfig out = new YuanGodSwordConfig();
        out.grayStrength = Float.NaN;        // non-finite falls back to min
        out.particleSize = 1_000_000f;       // far past the loose upper bound
        out.stopRadius = -1_000_000f;        // far past the loose lower bound
        out.rewindScopeMode = 99;            // tail clamp 0..1
        out.rewindDeathMaxRetries = -5;      // tail clamp 0..100

        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        out.write(stack);
        YuanGodSwordConfig back = new YuanGodSwordConfig();
        back.read(stack);

        assert back.grayStrength == 0f
                : "a non-finite float must fall back to the lower bound, got " + back.grayStrength;
        assert Float.isFinite(back.particleSize) && back.particleSize <= 3f * 100f
                : "particleSize must stay inside the loose bound, got " + back.particleSize;
        assert Float.isFinite(back.stopRadius) && back.stopRadius >= 0f
                : "stopRadius must stay inside the loose bound, got " + back.stopRadius;
        assert back.rewindScopeMode == 1
                : "rewindScopeMode must clamp to 0..1, got " + back.rewindScopeMode;
        assert back.rewindDeathMaxRetries == 0
                : "rewindDeathMaxRetries must clamp to 0..100, got " + back.rewindDeathMaxRetries;
    }

    /**
     * A tag written by an older build has {@code grayIntensity} but no {@code grayStrength}.
     * {@code read} must derive {@code grayStrength = 1 - grayIntensity} in that case, and must prefer
     * {@code grayStrength} when both are present.
     *
     * <p>This is the one field with a legacy key fallback, and the one place where a reflective walk
     * could quietly take over from the hand-written branch, so it is pinned explicitly.
     */
    private static void checkLegacyGrayIntensityFallback() throws Exception {
        // Only the legacy key present.
        CompoundTag legacy = new CompoundTag();
        legacy.putFloat("grayIntensity", 0.25f);
        ItemStack legacyStack = new ItemStack(Items.DIAMOND_SWORD);
        legacyStack.getOrCreateTagElement(YuanGodSwordConfig.TAG).merge(legacy);

        YuanGodSwordConfig fromLegacy = new YuanGodSwordConfig();
        fromLegacy.read(legacyStack);
        assert Math.abs(fromLegacy.grayStrength - 0.75f) < 1e-5f
                : "legacy grayIntensity must invert into grayStrength, got " + fromLegacy.grayStrength;

        // Both present: grayStrength wins.
        CompoundTag both = new CompoundTag();
        both.putFloat("grayStrength", 0.4f);
        both.putFloat("grayIntensity", 0.9f);
        ItemStack bothStack = new ItemStack(Items.DIAMOND_SWORD);
        bothStack.getOrCreateTagElement(YuanGodSwordConfig.TAG).merge(both);

        YuanGodSwordConfig fromBoth = new YuanGodSwordConfig();
        fromBoth.read(bothStack);
        assert Math.abs(fromBoth.grayStrength - 0.4f) < 1e-5f
                : "grayStrength must win when both keys are present, got " + fromBoth.grayStrength;

        // write() must still emit the legacy companion key.
        YuanGodSwordConfig out = new YuanGodSwordConfig();
        out.grayStrength = 0.6f;
        ItemStack written = new ItemStack(Items.DIAMOND_SWORD);
        out.write(written);
        CompoundTag tag = written.getTagElement(YuanGodSwordConfig.TAG);
        assert tag.contains("grayIntensity")
                : "write() must keep emitting the legacy grayIntensity companion key";
        assert Math.abs(tag.getFloat("grayIntensity") - 0.4f) < 1e-5f
                : "legacy companion key must hold 1 - grayStrength, got " + tag.getFloat("grayIntensity");
    }

    /**
     * A tag holding only some fields must leave the others at their defaults. The reflective walk
     * guards each field with {@code tag.contains}, so this pins that guard down: without it, a
     * missing key would overwrite a default with the tag's zero value.
     */
    private static void checkPartialTagKeepsDefaults(List<Field> fields) throws Exception {
        CompoundTag partial = new CompoundTag();
        partial.putBoolean("enabled", false);
        partial.putInt("cooldown", 77);
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.getOrCreateTagElement(YuanGodSwordConfig.TAG).merge(partial);

        YuanGodSwordConfig read = new YuanGodSwordConfig();
        read.read(stack);
        YuanGodSwordConfig defaults = new YuanGodSwordConfig();

        assert !read.enabled : "a present key must be applied";
        assert read.cooldown == 77 : "a present key must be applied";

        List<String> clobbered = new ArrayList<>();
        for (Field f : fields) {
            String name = f.getName();
            if (name.equals("enabled") || name.equals("cooldown")) continue;
            if (!java.util.Objects.equals(f.get(read), f.get(defaults))) {
                clobbered.add(name + ": absent key changed it to " + f.get(read));
            }
        }
        assert clobbered.isEmpty()
                : "an absent NBT key must leave the default alone, but these changed:\n  "
                        + String.join("\n  ", clobbered);
    }

    // ---------------------------------------------------------------- helpers

    /** Give every field a value that differs from its initialiser, so nothing passes by accident. */
    private static void dirty(YuanGodSwordConfig config, List<Field> fields) throws Exception {
        for (Field f : fields) {
            Class<?> t = f.getType();
            if (t == boolean.class) f.setBoolean(config, !f.getBoolean(config));
            else if (t == int.class) f.setInt(config, f.getInt(config) + 7);
            else if (t == float.class) f.setFloat(config, f.getFloat(config) + 0.37f);
            else if (t == String.class) f.set(config, "dirty-" + f.getName());
            else throw new AssertionError("unhandled field type " + t + " for " + f.getName());
        }
    }

    /** Change exactly one field, for the sameAs sensitivity check. */
    private static void mutate(Field f, YuanGodSwordConfig config) throws Exception {
        Class<?> t = f.getType();
        if (t == boolean.class) f.setBoolean(config, !f.getBoolean(config));
        else if (t == int.class) f.setInt(config, f.getInt(config) + 1);
        else if (t == float.class) f.setFloat(config, f.getFloat(config) + 1f);
        else if (t == String.class) f.set(config, "mutated");
        else throw new AssertionError("unhandled field type " + t);
    }

    /**
     * Pull fields whose dirty value would exceed a known clamp back inside the range, so the round
     * trip compares like with like. Fields without a clamp keep their dirty value.
     */
    private static void clampIntoRange(YuanGodSwordConfig c, List<Field> fields) throws Exception {
        Map<String, float[]> ranges = new TreeMap<>();
        ranges.put("grayStrength", new float[]{0f, 1f});
        ranges.put("particleSize", new float[]{0.4f, 3f});
        ranges.put("stopRadius", new float[]{0f, 128f});
        ranges.put("rewindRadius", new float[]{0f, 1024f});
        ranges.put("startDuration", new float[]{0.1f, 2f});
        ranges.put("endDuration", new float[]{0.1f, 2f});
        ranges.put("stopDuration", new float[]{0f, 60f});
        ranges.put("soundVolume", new float[]{0f, 1f});
        ranges.put("particleAlpha", new float[]{0.1f, 1f});
        ranges.put("particleSpin", new float[]{0f, 3f});

        Map<String, int[]> intRanges = new TreeMap<>();
        intRanges.put("rewindScopeMode", new int[]{0, 1});
        intRanges.put("rewindScope", new int[]{0, 1});
        intRanges.put("rewindPlaybackMode", new int[]{0, 1});
        intRanges.put("rewindRestoreOrder", new int[]{0, 1});
        intRanges.put("rewindCameraMode", new int[]{0, 1});
        intRanges.put("rewindPositionMode", new int[]{0, 1});
        intRanges.put("rewindDeathMaxRetries", new int[]{0, 100});
        // The tail of read() clamps these after loading; keep the written value inside the range so
        // the round trip compares like with like. (These were found by this check failing, not by
        // reading the source first -- the list below is the behaviour as measured.)
        intRanges.put("startAnim", new int[]{0, 2});
        intRanges.put("endAnim", new int[]{0, 1});
        intRanges.put("ballColor", new int[]{0, 7});
        intRanges.put("triggerMode", new int[]{0, 2});
        intRanges.put("cooldown", new int[]{0, 100});
        intRanges.put("particleCount", new int[]{1, 16});
        intRanges.put("grayStyle", new int[]{0, 2});
        intRanges.put("voronoiColorCount", new int[]{1, 10});
        intRanges.put("voronoiStepsPerColor", new int[]{1, 10});
        intRanges.put("uiTabVariant", new int[]{0, 1});
        intRanges.put("uiTabSize", new int[]{0, 2});
        intRanges.put("uiSliderHeight", new int[]{0, 2});
        intRanges.put("uiSliderThumb", new int[]{0, 2});
        intRanges.put("uiSearchStyle", new int[]{0, 1});
        intRanges.put("uiSwitchStyle", new int[]{0, 3});
        // renderStyle is clamped inline at its read site rather than in the tail.
        intRanges.put("renderStyle", new int[]{0, 5});

        for (Field f : fields) {
            String name = f.getName();
            if (f.getType() == float.class && ranges.containsKey(name)) {
                float[] r = ranges.get(name);
                f.setFloat(c, Math.max(r[0], Math.min(r[1], f.getFloat(c))));
            }
            else if (f.getType() == int.class && intRanges.containsKey(name)) {
                int[] r = intRanges.get(name);
                f.setInt(c, Math.max(r[0], Math.min(r[1], f.getInt(c))));
            }
        }
    }
}
