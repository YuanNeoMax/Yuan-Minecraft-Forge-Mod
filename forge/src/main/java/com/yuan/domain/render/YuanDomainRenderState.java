package com.yuan.domain.render;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * The GL state the abyss pass needs, applied and restored around the draw.
 *
 * <p>Written out explicitly rather than arranged by a render type's state shards. The pass replaces
 * ground colour across the whole frame, so the state it leaves behind would affect everything drawn
 * afterwards -- the hand and the HUD -- if it were not undone.
 */
final class YuanDomainRenderState {

    private YuanDomainRenderState() {
    }

    /**
     * Blending on, depth off.
     *
     * <p>Blending, because the pass composites over the ground rather than overwriting it: the
     * fragment stage outputs an alpha that ramps off at the mask edge, and a hard cut there aliases
     * into a visible circle.
     *
     * <p>Depth testing off, because the pass IS the ground: it is a full-screen triangle at the near
     * plane, and testing it against the buffer it was reconstructed from would compare it against
     * itself. Depth writes are off for the same reason -- the pass must not occlude the sky, the
     * hand, or anything drawn after it.
     *
     * <p>Culling off: the full-screen triangle runs off the edge of the viewport, and which way that
     * reads depends on the projection's handedness. Turning culling off removes the question.
     */
    static void apply() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    /** Restores the world state the pass changed, in the opposite direction to {@link #apply()}. */
    static void restore() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
