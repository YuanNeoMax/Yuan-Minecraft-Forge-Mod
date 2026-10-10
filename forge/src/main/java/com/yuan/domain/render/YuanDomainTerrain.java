package com.yuan.domain.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Samples the terrain surface for the star-abyss floor.
 *
 * <p>The floor follows the ground rather than sitting on a fixed plane, so the mesh builder needs a
 * height for every vertex. Heights come from {@link Heightmap.Types#MOTION_BLOCKING_NO_LEAVES} --
 * the height the game itself uses for "where would something land" -- so the surface sits on top of
 * grass, snow and water rather than inside them.
 *
 * <p><b>Deliberately uncached.</b> An earlier version cached a window of heights around the camera
 * and interpolated between samples. It was removed for two reasons: the window was smaller than the
 * largest domain radius, so most vertices fell outside it and took the direct path anyway; and having
 * two paths meant they could disagree at the boundary, which shows up as a visible seam. Measured
 * cost of sampling directly is roughly 8,000 O(1) heightmap reads per frame at the largest radius --
 * well under a millisecond, and not worth a second code path that can be wrong.
 */
public final class YuanDomainTerrain {

    private YuanDomainTerrain() {
    }

    /**
     * Surface height at a world position, in blocks.
     *
     * <p>Returns {@link Double#NaN} when no world is loaded, so callers can skip the frame rather
     * than drawing a floor at y=0.
     */
    public static double heightAt(double worldX, double worldZ) {
        Level level = level();
        if (level == null) {
            return Double.NaN;
        }
        int x = (int) Math.floor(worldX);
        int z = (int) Math.floor(worldZ);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** True when a world is loaded and heights can be sampled. */
    public static boolean available() {
        return level() != null;
    }

    private static Level level() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level;
    }
}
