package com.yuan.client.aurora;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

/**
 * The Aurora backdrop pipeline: the world already in the main target is blurred at half
 * resolution and composited back under a dim, so a screen draws its panels over frosted glass.
 *
 * <p>Five passes, one shader. Horizontal and vertical Gaussian passes through half-resolution
 * ping-pong targets, run twice for a wider radius, then an identity-step composite onto the main
 * target under a dark tint. Every pass is a full-screen triangle drawn with the same shader; only
 * the source texture, destination target, {@code Dir} step and {@code Tint} change per pass.
 *
 * <p>The main target is only ever sampled while a different target is bound for drawing, and only
 * drawn into while the blur result is the bound texture -- a texture attached to the bound
 * framebuffer must never be sampled, which is the same rule the domain depth copy follows.
 * Every {@code bindWrite} ends by re-binding whatever the frame expects next, because a pass that
 * leaves the wrong framebuffer bound silently detaches the rest of the frame from the post chain.
 */
public final class AuroraFx {

    /** Scratch for the full-screen triangle: three POSITION_TEX vertices is 60 bytes. */
    private static final int BUFFER_BYTES = 256;

    private static final float[] FULLSCREEN_TRIANGLE = {
            //  x     y    z      u     v
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
             3.0f, -1.0f, 0.0f, 2.0f, 0.0f,
            -1.0f,  3.0f, 0.0f, 0.0f, 2.0f,
    };

    private static final Matrix4f IDENTITY = new Matrix4f();

    /** Composite tint: deep-space blue-black, alpha is the dim strength the caller picks. */
    private static final float TINT_R = 0.03f;
    private static final float TINT_G = 0.045f;
    private static final float TINT_B = 0.09f;

    /** Half-resolution ping-pong targets. No depth: these hold flat colour, nothing tests. */
    private static TextureTarget blurA;
    private static TextureTarget blurB;

    /** Geometry scratch, reused across frames the way vanilla reuses its builders. */
    private static BufferBuilder builder;

    /** Static geometry, but re-uploaded each pass is simpler than caching and costs nothing. */
    private static VertexBuffer triangleBuffer;

    private AuroraFx() {
    }

    /**
     * Runs the pipeline and leaves the main target holding the blurred, dimmed backdrop.
     *
     * @param dimAlpha composite alpha; 0 leaves the world untouched behind the tint, 1 is opaque
     * @return false when the shader or targets are unavailable, in which case the caller should
     *         fall back to a plain fill rather than draw panels over an unblurred world
     */
    public static boolean apply(RenderTarget main, float dimAlpha) {
        ShaderInstance shader = AuroraShaders.getShader();
        if (shader == null || !AuroraShaders.ready()) {
            return false;
        }
        int width = main.width;
        int height = main.height;
        if (width <= 0 || height <= 0) {
            return false;
        }
        if (main.getColorTextureId() == 0) {
            return false;
        }
        int halfWidth = Math.max(1, width / 2);
        int halfHeight = Math.max(1, height / 2);
        ensureTargets(halfWidth, halfHeight);
        if (blurA.getColorTextureId() == 0 || blurB.getColorTextureId() == 0) {
            return false;
        }

        RenderSystem.setShader(AuroraShaders::getShader);
        AuroraRenderState.apply();
        try {
            // Two full H+V iterations: the second pass re-blurs an already-blurred image, which
            // widens the radius without widening the kernel. At half resolution all of this is
            // a quarter of the screen's pixels, four times over.
            pass(main.getColorTextureId(), blurA, width, height, 1.0f / width, 0.0f, 1f, 1f, 1f, 1f);
            pass(blurA.getColorTextureId(), blurB, halfWidth, halfHeight, 0.0f, 1.0f / halfHeight, 1f, 1f, 1f, 1f);
            pass(blurB.getColorTextureId(), blurA, halfWidth, halfHeight, 1.0f / halfWidth, 0.0f, 1f, 1f, 1f, 1f);
            pass(blurA.getColorTextureId(), blurB, halfWidth, halfHeight, 0.0f, 1.0f / halfHeight, 1f, 1f, 1f, 1f);
            // Identity step: every tap lands on the same texel, weights sum to 1 -- an exact copy
            // of the blurred source, dimmed by Tint's alpha as it lands on the main target.
            pass(blurB.getColorTextureId(), main, width, height, 0.0f, 0.0f,
                    TINT_R, TINT_G, TINT_B, dimAlpha);
        } finally {
            AuroraRenderState.restore();
        }
        return true;
    }

    /**
     * Degradation path: fills the main target with the composite tint and no blur, for when the
     * shader is not ready (the first seconds of a launch, or a compile error).
     *
     * <p>A clear rather than a blended draw: the tint is opaque and the panels drawn on top are
     * translucent, so washing the world out completely is the honest degradation. Leaving it
     * visible under a translucent panel would produce light text on a bright, busy background,
     * which is worse than losing the effect.
     *
     * <p>Kept separate from {@link #apply} rather than folded into it so the two outcomes stay
     * distinguishable: the caller wants to know whether the backdrop was actually blurred, and a
     * silent fallback inside apply would erase that answer.
     */
    public static void fillFallback(RenderTarget main, float dimAlpha) {
        if (main == null || main.width <= 0 || main.height <= 0) {
            return;
        }
        RenderSystem.clearColor(TINT_R, TINT_G, TINT_B, Math.max(0.85f, dimAlpha));
        main.clear(true);
        RenderSystem.clearColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** Releases the GPU targets; call when the screen closes so they do not idle across sessions. */
    public static void close() {        if (blurA != null) {
            blurA.destroyBuffers();
            blurA = null;
        }
        if (blurB != null) {
            blurB.destroyBuffers();
            blurB = null;
        }
    }

    private static void ensureTargets(int width, int height) {
        if (blurA == null) {
            // TextureTarget, not RenderTarget: the latter is abstract. No depth attachment -- the
            // passes are flat colour, and a clear on a target about to be fully overwritten is
            // wasted work.
            blurA = new TextureTarget(width, height, false, false);
        } else if (blurA.width != width || blurA.height != height) {
            blurA.resize(width, height, false);
        }
        if (blurB == null) {
            blurB = new TextureTarget(width, height, false, false);
        } else if (blurB.width != width || blurB.height != height) {
            blurB.resize(width, height, false);
        }
    }

    /**
     * One full-screen pass: sample {@code sourceTexture}, write {@code destination}.
     *
     * <p>The matrices are identity for both stages: the vertex stage emits clip-space positions,
     * so the shader's ModelViewMat and ProjMat must be no-ops. Passing RenderSystem's matrices
     * here would transform an already-clip-space triangle by whatever projection the caller had.
     */
    private static void pass(int sourceTexture, RenderTarget destination,
                             int dstWidth, int dstHeight,
                             float dirX, float dirY,
                             float tintR, float tintG, float tintB, float tintA) {
        AuroraShaders.setPass(dirX, dirY, tintR, tintG, tintB, tintA);
        destination.bindWrite(true);
        RenderSystem.setShaderTexture(0, sourceTexture);

        BufferBuilder bb = builder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < FULLSCREEN_TRIANGLE.length; i += 5) {
            bb.vertex(FULLSCREEN_TRIANGLE[i], FULLSCREEN_TRIANGLE[i + 1], FULLSCREEN_TRIANGLE[i + 2])
                    .uv(FULLSCREEN_TRIANGLE[i + 3], FULLSCREEN_TRIANGLE[i + 4])
                    .endVertex();
        }

        VertexBuffer vb = triangleBuffer();
        vb.bind();
        vb.upload(bb.end());
        vb.drawWithShader(IDENTITY, IDENTITY, AuroraShaders.getShader());
        VertexBuffer.unbind();
    }

    private static BufferBuilder builder() {
        if (builder == null) {
            builder = new BufferBuilder(BUFFER_BYTES);
        }
        return builder;
    }

    private static VertexBuffer triangleBuffer() {
        if (triangleBuffer == null) {
            triangleBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        return triangleBuffer;
    }
}
