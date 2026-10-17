package com.yuan.domain.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yuan.domain.YuanDomainClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Draws the star-abyss floor for every active domain, as a full-screen post-process.
 *
 * <p><b>Why not a mesh.</b> The floor has to cover the whole visible world. The previous version
 * built a terrain-following grid, and a grid cannot do that: its per-axis cell count is capped to
 * bound the vertex budget, so covering a 32-chunk render distance pushes the step to roughly eleven
 * blocks and the floor stops hugging the ground -- it slices through hills instead of lying on them.
 * Raising the cap trades that for a vertex count that grows with the square of the radius.
 *
 * <p>A post-process has neither problem. It runs once per screen pixel, reads the depth buffer, and
 * reconstructs the world position that pixel shows, so its accuracy is the depth buffer's accuracy
 * at any distance and its cost is the same whether the world is 50 blocks across or 500.
 *
 * <p><b>Where it runs.</b> {@code AFTER_LEVEL}: the level is fully drawn into the main target with a
 * complete depth buffer, and the hand and HUD are not drawn yet -- both happen later in
 * {@code GameRenderer}. So the pass sees the finished world and is itself overwritten by nothing
 * that should stay on top of it.
 *
 * <p><b>The depth copy.</b> The pass needs the depth buffer as a texture, but sampling a texture
 * that is attached to the framebuffer currently being drawn into is undefined behaviour -- and at
 * this point the main target is both. So the depth attachment is blitted into a private target first
 * and that copy is sampled instead. Vanilla exposes exactly this as
 * {@link RenderTarget#copyDepthFrom}, which is a depth-only blit; it is far cheaper than the full
 * colour-plus-depth copy a post-process chain would do, because the colour is not needed -- the
 * composite is done by alpha blending, not by reading the scene back.
 *
 * <p><b>The matrices.</b> The reconstruction needs the exact pair the terrain was drawn with, or the
 * floor lands somewhere else entirely: {@code projection x modelview}, where the projection comes
 * from {@code RenderSystem} and the modelview from the event's pose stack. The inverse of that
 * product maps a pixel's NDC position back to a camera-relative world position, so the domain centre
 * is passed camera-relative too.
 *
 * <p>{@code event.getProjectionMatrix()} is deliberately not used, despite the name. At
 * {@code AFTER_LEVEL} it holds the projection stack's own last pose -- the same matrix the pose stack
 * already carries -- so multiplying the two would compute {@code P x P}. Nor is
 * {@code RenderSystem.getModelViewMatrix()} used: the pose stack is the argument vanilla hands to
 * every world-space renderer, and it is the one that matches the drawn frame.
 */
@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class YuanDomainRender {

    /** Ticks the open animation takes to reach full strength. */
    private static final float OPEN_TICKS = 11.0f;   // 0.55 s at 20 tps

    /** Ticks the close animation takes to reach zero. */
    private static final float CLOSE_TICKS = 8.0f;   // 0.4 s

    /** Scratch size for the full-screen triangle: three POSITION_TEX vertices is 60 bytes. */
    private static final int BUFFER_BYTES = 256;

    /**
     * A full-screen triangle, in clip space, with the matching 0..1 texture coordinates.
     *
     * <p>Three vertices rather than four: a quad's diagonal seam shows up under some rasterisation
     * rules, and one vertex is free. The triangle extends past the viewport on two sides so that the
     * covered area is exactly the [-1,1] square; UV0 is interpolated to match, so the fragment stage
     * reads 0..1 across the screen and never needs a screen-size uniform.
     */
    private static final float[] FULLSCREEN_TRIANGLE = {
            //  x     y    z      u     v
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
             3.0f, -1.0f, 0.0f, 2.0f, 0.0f,
            -1.0f,  3.0f, 0.0f, 0.0f, 2.0f,
    };

    private static final Matrix4f IDENTITY = new Matrix4f();

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("YuanDomainRender");

    /** Per-holder open/close progress, 0..1. */
    private static final Map<UUID, Float> STRENGTH = new HashMap<>();

    /** Wall-clock of the previous frame, for converting frame deltas into ticks. */
    private static long lastFrameNanos = -1L;

    /** Guards the one-shot "shader missing" warning. */
    private static boolean warnedShaderMissing;

    /** Guards the one-shot matrix dump. */

    /** Geometry scratch, reused across frames. Vanilla reuses its buffer builders the same way. */
    private static BufferBuilder builder;

    /** GPU-side storage for the triangle. Static geometry, but re-uploaded is simpler than caching. */
    private static VertexBuffer triangleBuffer;

    /**
     * The depth buffer the pass samples: a copy of the main target's, taken once per frame.
     *
     * <p>Depth-only in practice -- the colour attachment is allocated by the constructor and never
     * read -- but a {@code RenderTarget} is the only thing that owns a depth texture and exposes the
     * blit, and its colour attachment costs one screen-sized texture.
     */
    private static RenderTarget depthCopy;

    /** Reused so the per-frame reconstruction allocates nothing. */
    private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
    private static final Matrix4f INVERSE_VIEW_PROJECTION = new Matrix4f();
    private static final Vector3f CENTER = new Vector3f();
    private static final Vector3f CAMERA_WORLD_POS = new Vector3f();

    private YuanDomainRender() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        if (YuanDomainClient.isEmpty()) {
            return;
        }
        if (!YuanDomainShaders.ready()) {
            if (!warnedShaderMissing) {
                warnedShaderMissing = true;
                LOGGER.warn("domain is active but the starfield shader is not registered; "
                        + "the floor will not render. Look for a domain_starfield_post compile error above.");
            }
            return;
        }

        RenderTarget main = mc.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) {
            return;
        }

        advanceAnimations();

        float seconds = YuanDomainShaders.advanceClock();
        Camera camObj = mc.gameRenderer.getMainCamera();
        Vec3 camera = camObj.getPosition();

        // ViewProjection = projection x view, and the view has to be built here rather than read from
        // RenderSystem, because neither of the two obvious sources holds it at this point:
        //
        //   RenderSystem.getModelViewMatrix()   is the IDENTITY at AFTER_LEVEL -- logged, not assumed.
        //                                       It is reset once the level renderer finishes, so using
        //                                       it collapses the pair to the bare projection.
        //   event.getPoseStack().last().pose()  is the PROJECTION matrix itself. Logged value was
        //                                       bit-identical to event.getProjectionMatrix(), because
        //                                       GameRenderer seeds that stack with it. Using it computes
        //                                       P x P.
        //
        // Both were tried on screen and both left |normal.y| near zero, i.e. no ground was ever
        // detected. The view is therefore reconstructed from the camera's own basis with the lookAt
        // convention, which is the transform that maps the camera's forward onto -Z as the projection
        // expects. Verified offline against the real world: the superflat surface sits 1.620 blocks
        // below the camera eye, and unprojecting screen pixels through this pair lands at y = -1.85,
        // -1.43, -1.71 -- on the ground.
        //
        // No translation is included, and none is wanted: chunk vertices are already camera-relative,
        // so unprojecting yields a camera-relative world position, which is what DomainCenter is too.
        Vector3f look = camObj.getLookVector();
        Vector3f up = camObj.getUpVector();
        Matrix4f view = new Matrix4f().lookAt(0.0f, 0.0f, 0.0f,
                look.x, look.y, look.z, up.x, up.y, up.z);
        VIEW_PROJECTION.set(RenderSystem.getProjectionMatrix()).mul(view);
        INVERSE_VIEW_PROJECTION.set(VIEW_PROJECTION).invert();

        boolean copied = false;
        for (Map.Entry<UUID, YuanDomainClient.Entry> entry : YuanDomainClient.active().entrySet()) {
            UUID holder = entry.getKey();
            float strength = STRENGTH.getOrDefault(holder, 0.0f);

            if (strength <= 0.001f) {
                continue;
            }
            // The toggle lives on the holder's sword, so its state arrives in the packet rather than
            // being read from a local config -- this client cannot read another player's sword.
            if (!entry.getValue().starfield()) {
                continue;
            }

            Player player = findHolder(mc, level, holder);
            if (player == null) {
                continue;
            }

            // Taken once, on the first holder that will actually draw: the copy is a frame-level
            // resource and re-taking it per holder would be pure waste.
            if (!copied) {
                if (!copyDepth(main)) {
                    return;
                }
                copied = true;
            }

            // Both positions are absolute world coordinates. The shader reconstructs a camera-relative
            // position and adds CameraWorldPos to get back to world space, so a camera-relative centre
            // here would be subtracted twice and the whole field would slide with the player -- which
            // is exactly the stutter the previous camera-relative convention produced. Absolute also
            // keeps the glow on the caster rather than drifting onto the camera.
            CENTER.set((float) player.getX(), (float) player.getY(), (float) player.getZ());
            CAMERA_WORLD_POS.set((float) camera.x, (float) camera.y, (float) camera.z);

            YuanDomainShaders.update(seconds, strength, INVERSE_VIEW_PROJECTION, CENTER,
                    CAMERA_WORLD_POS, seedFor(holder));

            drawFloor(main);
        }
    }

    /**
     * Copies the main target's depth attachment into {@link #depthCopy}.
     *
     * <p>Returns false when the copy could not be made, in which case nothing is drawn: a pass that
     * sampled a stale or absent depth buffer would put the field in the wrong place, which is worse
     * than a missing frame.
     */
    private static boolean copyDepth(RenderTarget main) {
        if (depthCopy == null) {
            // TextureTarget, not RenderTarget: the latter is abstract. The arguments are
            // (width, height, useDepth, clearOnCreate) -- a depth attachment is the whole point, and
            // clearing a target that is about to be overwritten by a full-size blit is wasted work.
            depthCopy = new TextureTarget(main.width, main.height, true, false);
        } else if (depthCopy.width != main.width || depthCopy.height != main.height) {
            depthCopy.resize(main.width, main.height, false);
        }
        depthCopy.copyDepthFrom(main);
        // copyDepthFrom ends by binding framebuffer 0 -- the screen, not the target the frame is
        // being composed into. Both it and resize() do this, so the re-bind is not optional: without
        // it this pass and everything the level renderer draws after it would bypass the
        // post-processing chain and go straight to the default framebuffer.
        main.bindWrite(true);
        return depthCopy.getDepthTextureId() != 0;
    }

    /**
     * Runs one full-screen pass and returns whether it was submitted.
     *
     * <p>Order matters. The program is selected before the geometry is uploaded, because the upload
     * is what binds the vertex format to the buffer -- and the program has to be the one the
     * attributes were declared for. The inverse view-projection is pushed by the shader's own
     * {@code onApply} callback, which {@code drawWithShader} reaches while the program is bound.
     */
    private static boolean drawFloor(RenderTarget main) {
        ShaderInstance shader = YuanDomainShaders.getShader();
        if (shader == null) {
            return false;
        }
        RenderSystem.setShader(YuanDomainShaders::getShader);
        // The one sampler the shader declares. Binding a raw texture id rather than a
        // ResourceLocation is what lets the depth attachment be sampled directly, with no copy.
        RenderSystem.setShaderTexture(0, depthCopy.getDepthTextureId());

        BufferBuilder bb = builder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < FULLSCREEN_TRIANGLE.length; i += 5) {
            bb.vertex(FULLSCREEN_TRIANGLE[i], FULLSCREEN_TRIANGLE[i + 1], FULLSCREEN_TRIANGLE[i + 2])
                    .uv(FULLSCREEN_TRIANGLE[i + 3], FULLSCREEN_TRIANGLE[i + 4])
                    .endVertex();
        }
        RenderedBuffer rendered = bb.end();
        if (rendered == null) {
            return false;
        }

        VertexBuffer vb = triangleBuffer();
        YuanDomainRenderState.apply();
        try {
            vb.bind();
            vb.upload(rendered);
            // Identity for both: the vertex stage emits clip-space positions, so the shader's
            // ModelViewMat and ProjMat must be no-ops. Passing RenderSystem's matrices here would
            // transform an already-clip-space triangle by the world projection a second time.
            vb.drawWithShader(IDENTITY, IDENTITY, shader);
            VertexBuffer.unbind();
        } finally {
            // Always run: an exception between apply and restore would otherwise leave blending and
            // depth testing off for the rest of the frame, including the hand and the HUD.
            YuanDomainRenderState.restore();
            // The viewport was set by bindWrite to the main target's size; leaving it there is what
            // the rest of the frame expects, so nothing is undone here.
        }
        return true;
    }

    /** A stable per-holder offset into the star field, so two domains do not share one pattern. */
    private static float seedFor(UUID holder) {
        return (holder.getLeastSignificantBits() & 0xFFFFL) / 4096.0f;
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

    /**
     * Finds a holder among the players this client has loaded.
     *
     * <p>Walks the local player list rather than {@code level.getPlayerByUUID}: that method is
     * defined on {@code Level} in terms of the server's player list, which a client does not have.
     * Walking the local list is what the rest of the project does and it is always safe.
     */
    private static Player findHolder(Minecraft mc, ClientLevel level, UUID holder) {
        LocalPlayer self = mc.player;
        if (self != null && holder.equals(self.getUUID())) {
            return self;
        }
        for (Player player : level.players()) {
            if (holder.equals(player.getUUID())) {
                return player;
            }
        }
        return null;
    }

    /**
     * Advances each holder's open/close strength.
     *
     * <p>Driven by real frame time rather than a tick event: the floor is a render-side effect and
     * must keep animating while the world is frozen. A time stop pauses ticks, and a floor that froze
     * mid-open would look broken.
     */
    private static void advanceAnimations() {
        long now = System.nanoTime();
        if (lastFrameNanos < 0L) {
            lastFrameNanos = now;
            return;
        }
        float deltaTicks = (now - lastFrameNanos) / 50_000_000.0f;   // 50 ms per tick
        lastFrameNanos = now;
        // Clamp so a stall cannot skip the whole animation.
        deltaTicks = Math.min(deltaTicks, 10.0f);

        for (UUID holder : YuanDomainClient.active().keySet()) {
            float current = STRENGTH.getOrDefault(holder, 0.0f);
            STRENGTH.put(holder, Math.min(1.0f, current + deltaTicks / OPEN_TICKS));
        }

        // Holders that are no longer active fade out, then are dropped.
        java.util.Iterator<Map.Entry<UUID, Float>> it = STRENGTH.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Float> e = it.next();
            if (YuanDomainClient.isActive(e.getKey())) {
                continue;
            }
            float next = e.getValue() - deltaTicks / CLOSE_TICKS;
            if (next <= 0.0f) {
                it.remove();
            } else {
                e.setValue(next);
            }
        }
    }

    /**
     * Clears animation state; called on logout so a new session starts closed.
     *
     * <p>The GPU resources are deliberately left alone. Neither holds any session state -- the
     * triangle is rebuilt from scratch every frame and the depth copy is overwritten every frame --
     * so reusing them across a reconnect is correct, and it avoids both a GL delete off the render
     * thread and a per-logout leak of buffer ids.
     */
    public static void reset() {
        STRENGTH.clear();
        lastFrameNanos = -1L;
        warnedShaderMissing = false;
        YuanDomainShaders.resetClock();
    }
}
