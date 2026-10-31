package com.yuan.client.aurora;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * The GL state the Aurora blur and composite passes need, applied and restored around the runs.
 *
 * <p>Same discipline as the domain star-field pass: the state is written out explicitly rather than
 * arranged by a render type, and it is restored unconditionally -- a pass that leaves blending or
 * depth testing off would corrupt every draw after it, including the screen's own text.
 *
 * <p>Blending on, so the composite pass can lay the dimmed backdrop over the world with an alpha
 * rather than overwriting it. Depth test and writes off: these passes are flat full-screen
 * triangles with nothing to test against and nothing allowed to occlude later draws. Culling off:
 * the triangle runs off the viewport edge and which way that winds depends on nothing we control.
 */
final class AuroraRenderState {

    private AuroraRenderState() {
    }

    static void apply() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    /** Restores the state the passes changed, in the opposite direction to {@link #apply()}. */
    static void restore() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
