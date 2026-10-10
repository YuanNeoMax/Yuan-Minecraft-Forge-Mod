package com.yuan.domain.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Render types for the domain's world-space layers.
 *
 * <p>Uses standard alpha blending rather than an additive layer. The project's
 * {@code GlowRenderLayer} is {@code blendFunc(SRC_ALPHA, ONE)} with depth writes off, which can
 * only brighten -- it cannot darken anything, and it draws over geometry regardless of depth. The
 * abyss floor needs to be a dark body that occludes what is under it, so it uses a normal
 * translucent blend with depth test on.
 */
public final class YuanDomainRenderTypes extends RenderType {

    private YuanDomainRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                                  int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                                  Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    /**
     * The star-abyss floor.
     *
     * <p>Depth test is LEQUAL with depth writes disabled: the floor follows the terrain and must be
     * hidden by anything genuinely in front of it, but it must not write depth itself or it would
     * occlude the sky and later passes.
     *
     * <p>{@code NO_CULL} because the mesh is generated as a strip whose winding is not guaranteed
     * when the surface folds on steep terrain.
     */
    public static final RenderType STARFIELD = create(
            "yuan:domain_starfield",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.TRIANGLES,
            // Vertex budget, not a byte size: the mesh is capped at MAX_CELLS per axis, which at a
            // 128-block radius works out to roughly 36,500 vertices (six per quad). Too small and
            // the buffer flushes repeatedly mid-mesh, so this is sized above the worst case rather
            // than at a typical one. The space-slash layers use 4096; this needs more because it is
            // an entire terrain surface rather than a single blade.
            65536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(YuanDomainShaders::getShader))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .createCompositeState(false)
    );
}
