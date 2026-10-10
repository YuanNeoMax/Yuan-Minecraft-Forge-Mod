package com.yuan.domain.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * Builds the star-abyss floor mesh: a terrain-following surface covering the domain.
 *
 * <p>The mesh is a Cartesian grid clipped to the domain circle. Each vertex takes its height from
 * {@link YuanDomainTerrain}, so the surface sits on the ground rather than on a fixed plane -- a hill
 * rises out of the abyss and a valley fills with it. That is what "the world's surface has been
 * replaced" means here.
 *
 * <p>Two deliberate choices:
 *
 * <ul>
 *   <li><b>The grid step grows with the radius.</b> A 128-block domain at one vertex per block would
 *       be 65,536 quads. The step is chosen so the grid stays within {@link #MAX_CELLS} per axis,
 *       bounding the per-frame vertex count whatever radius the player sets. Fidelity loss is mild
 *       because the star field is computed per fragment, not per vertex: only the terrain silhouette
 *       gets coarser, and {@code heightAt} interpolates between samples so it stays smooth rather
 *       than stepped.
 *   <li><b>UV0 carries the offset from the domain centre, in blocks.</b> The fragment stage uses it
 *       both to place a stable world-space star field and to measure radial distance, so no separate
 *       centre uniform is needed. Offsets rather than absolute world coordinates also keep float
 *       precision usable far from the world origin.
 * </ul>
 */
public final class YuanDomainFloorMesh {

    /** Cap on grid cells per axis. Bounds the vertex count for any radius. */
    private static final int MAX_CELLS = 96;

    /** Vertices per quad: two triangles, six vertices. */
    private static final int VERTS_PER_QUAD = 6;

    private YuanDomainFloorMesh() {
    }

    /** Grid step in blocks, chosen so the grid stays within {@link #MAX_CELLS} per axis. */
    public static double stepFor(float radius) {
        double span = radius * 2.0;
        return Math.max(1.0, Math.ceil(span / MAX_CELLS));
    }

    /** Number of grid cells from the centre to the rim, for a given step. */
    private static int halfCells(float radius, double step) {
        return (int) Math.ceil(radius / step);
    }

    /**
     * Emits the floor for one domain into {@code consumer}.
     *
     * @param pose     camera-relative pose stack from the render event
     * @param consumer vertex sink for the starfield render type
     * @param camX     camera world X; subtracted to reach the camera-relative space
     * @param camY     camera world Y
     * @param camZ     camera world Z
     * @param centerX  domain centre world X
     * @param centerZ  domain centre world Z
     * @param radius   domain radius in blocks
     * @return quads emitted, for the caller's diagnostics
     */
    public static int build(PoseStack pose, VertexConsumer consumer,
                            double camX, double camY, double camZ,
                            double centerX, double centerZ, float radius) {
        if (!YuanDomainTerrain.available()) {
            return 0;
        }

        double step = stepFor(radius);
        int half = halfCells(radius, step);
        // One cell of slack beyond the rim, so the shader's fade has geometry to fade out over.
        int span = half + 1;
        int rowLen = span * 2 + 1;

        Matrix4f matrix = pose.last().pose();

        // Two rows of heights, sampled once each and shared between neighbouring quads.
        double[] lower = new double[rowLen];
        double[] upper = new double[rowLen];

        sampleRow(lower, centerX, centerZ, -span, step, span);
        int quads = 0;

        for (int iz = -span; iz < span; iz++) {
            sampleRow(upper, centerX, centerZ, iz + 1, step, span);

            for (int ix = -span; ix < span; ix++) {
                // Skip quads wholly outside the circle. The shader would discard them anyway; not
                // emitting them saves the vertex traffic. The margin keeps the rim covered.
                if (quadOutsideCircle(ix, iz, step, radius)) {
                    continue;
                }

                int i0 = ix + span;
                int i1 = i0 + 1;

                double y00 = lower[i0];
                double y10 = lower[i1];
                double y01 = upper[i0];
                double y11 = upper[i1];
                if (Double.isNaN(y00) || Double.isNaN(y10) || Double.isNaN(y01) || Double.isNaN(y11)) {
                    continue;
                }

                double localX0 = ix * step;
                double localX1 = (ix + 1) * step;
                double localZ0 = iz * step;
                double localZ1 = (iz + 1) * step;

                // Camera-relative positions.
                float px0 = (float) (centerX + localX0 - camX);
                float px1 = (float) (centerX + localX1 - camX);
                float pz0 = (float) (centerZ + localZ0 - camZ);
                float pz1 = (float) (centerZ + localZ1 - camZ);

                // Sink the surface a hair into the ground so it reads as replacing the terrain
                // rather than hovering just above it.
                float py00 = (float) (y00 - camY) - 0.02f;
                float py10 = (float) (y10 - camY) - 0.02f;
                float py01 = (float) (y01 - camY) - 0.02f;
                float py11 = (float) (y11 - camY) - 0.02f;

                // UV0 = offset from the domain centre, in blocks.
                float u0 = (float) localX0;
                float u1 = (float) localX1;
                float v0 = (float) localZ0;
                float v1 = (float) localZ1;

                // Two triangles: (00, 10, 11) and (00, 11, 01).
                vertex(consumer, matrix, px0, py00, pz0, u0, v0);
                vertex(consumer, matrix, px1, py10, pz0, u1, v0);
                vertex(consumer, matrix, px1, py11, pz1, u1, v1);

                vertex(consumer, matrix, px0, py00, pz0, u0, v0);
                vertex(consumer, matrix, px1, py11, pz1, u1, v1);
                vertex(consumer, matrix, px0, py01, pz1, u0, v1);

                quads++;
            }

            double[] swap = lower;
            lower = upper;
            upper = swap;
        }

        return quads;
    }

    /**
     * True when a quad lies entirely beyond the domain rim.
     *
     * <p>Tests the quad's nearest point to the centre rather than its centre, so quads straddling
     * the rim are kept and the circle edge stays smooth.
     */
    private static boolean quadOutsideCircle(int ix, int iz, double step, float radius) {
        double x0 = ix * step;
        double x1 = (ix + 1) * step;
        double z0 = iz * step;
        double z1 = (iz + 1) * step;

        double nearestX = x0 > 0.0 ? x0 : (x1 < 0.0 ? x1 : 0.0);
        double nearestZ = z0 > 0.0 ? z0 : (z1 < 0.0 ? z1 : 0.0);
        double nearest = Math.sqrt(nearestX * nearestX + nearestZ * nearestZ);

        // One step of slack so the fade has geometry past the nominal rim.
        return nearest > radius + step;
    }

    /** Samples one row of terrain heights into {@code out}, indexed by {@code ix + span}. */
    private static void sampleRow(double[] out, double centerX, double centerZ,
                                  int iz, double step, int span) {
        double worldZ = centerZ + iz * step;
        for (int ix = -span; ix <= span; ix++) {
            out[ix + span] = YuanDomainTerrain.heightAt(centerX + ix * step, worldZ);
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                               float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .endVertex();
    }

    /** Vertices emitted per quad; kept for callers that pre-size buffers. */
    public static int vertsPerQuad() {
        return VERTS_PER_QUAD;
    }
}
