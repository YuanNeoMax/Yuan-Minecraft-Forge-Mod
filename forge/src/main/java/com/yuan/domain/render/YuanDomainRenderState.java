package com.yuan.domain.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

/**
 * GL state for the star-abyss floor.
 *
 * <p>The floor is submitted as an immediate-mode buffer rather than through a render-type layer.
 * That layer applied its own state shards around the draw and routed the geometry through the
 * shared buffer source; on this stack the geometry was submitted and the uniforms landed, and
 * nothing appeared on screen. Setting the state here keeps the draw self-contained and adjacent to
 * the code that issues it, so the state and the draw cannot drift apart.
 *
 * <p>Depth is tested but not written: the floor follows the terrain and must be hidden by anything
 * genuinely in front of it, yet it must not occlude the sky or later passes.
 *
 * <p>Blending is a plain alpha blend, not an additive layer. The project's glow layer is
 * {@code blendFunc(SRC_ALPHA, ONE)}, which can only brighten; the abyss floor needs to be a dark
 * body that covers what is under it.
 *
 * <p>State is applied by {@link #apply()} and undone by {@link #restore()}. The caller always runs
 * both, so an early return between them cannot leave blending or depth writes off for the rest of
 * the frame.
 */
final class YuanDomainRenderState {

    private YuanDomainRenderState() {
    }

    /** Enables the floor's state: alpha blend, no culling, depth-tested but not depth-writing. */
    static void apply() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    /** Restores the world state the floor changed, in the opposite direction to {@link #apply()}. */
    static void restore() {
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
