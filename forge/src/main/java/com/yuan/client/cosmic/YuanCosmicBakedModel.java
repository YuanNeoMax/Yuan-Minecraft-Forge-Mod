package com.yuan.client.cosmic;

import com.yuan.item.YuanGodSwordConfig;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Transformation;
import com.yuan.client.shader.ModRenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class YuanCosmicBakedModel implements BakedModel {
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();

    private static final ModelState IDENTITY_STATE = new ModelState() {
        @Override
        public Transformation getRotation() {
            return Transformation.identity();
        }

        @Override
        public boolean isUvLocked() {
            return false;
        }
    };

    private final BakedModel wrapped;
    private final List<ResourceLocation> maskSprites;

    public record PendingRender(
            ItemStack stack,
            ItemDisplayContext context,
            boolean leftHand,
            Matrix4f poseMatrix,
            Matrix3f normalMatrix,
            int light,
            int overlay,
            BakedModel wrapped,
            List<ResourceLocation> maskSprites,
            YuanRenderStateSnapshot renderState) {
    }

    private static final List<PendingRender> PENDING_RENDERS = new ArrayList<>();
    private static final List<MaskQuadCacheEntry> MASK_QUAD_CACHE = new ArrayList<>();
    private static final int MASK_QUAD_CACHE_LIMIT = 8;

    public YuanCosmicBakedModel(BakedModel wrapped, List<ResourceLocation> maskSprites) {
        this.wrapped = wrapped;
        this.maskSprites = maskSprites;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return wrapped.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return wrapped.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return wrapped.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return wrapped.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return wrapped.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return wrapped.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return wrapped.getOverrides();
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return wrapped.getRenderTypes(stack, fabulous);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
        wrapped.applyTransform(context, poseStack, leftHand);
        return this;
    }

    public void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
        renderBase(stack, pose, buffers, light, overlay);
        YuanGodSwordConfig cfg = new YuanGodSwordConfig();
        cfg.read(stack);
        if (cfg.renderStyle == 0) {
            renderCosmic(stack, context, pose, buffers, light, overlay);
        } else if (cfg.renderStyle == 1 && !YuanIris.isShaderPackActive()) {
            renderSilk(stack, context, pose, buffers, light, overlay, cfg);
        } else if (cfg.renderStyle == 4 && !YuanIris.isShaderPackActive()) {
            renderTunnel(stack, context, pose, buffers, light, overlay, cfg);
        } else if (cfg.renderStyle == 5 && !YuanIris.isShaderPackActive()) {
            renderVoronoi(stack, context, pose, buffers, light, overlay, cfg);
        }
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
    }

    private void renderSilk(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                            MultiBufferSource buffers, int light, int overlay,
                            YuanGodSwordConfig cfg) {
        if (YuanCosmicShaders.silkShader == null) {
            return;
        }
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
        YuanCosmicShaders.setupSilkUniforms(cfg);
        VertexConsumer consumer = buffers.getBuffer(ModRenderType.silkRenderType);
        renderRegularItemCosmic(stack, pose, consumer, light, overlay);
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
    }

    private void renderTunnel(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay,
                              YuanGodSwordConfig cfg) {
        if (YuanCosmicShaders.tunnelShader == null) {
            return;
        }
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
        YuanCosmicShaders.setupTunnelUniforms(cfg);
        VertexConsumer consumer = buffers.getBuffer(ModRenderType.tunnelRenderType);
        renderRegularItemCosmic(stack, pose, consumer, light, overlay);
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
    }

    private void renderVoronoi(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                               MultiBufferSource buffers, int light, int overlay,
                               YuanGodSwordConfig cfg) {
        if (YuanCosmicShaders.voronoiShader == null) {
            return;
        }
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
        YuanCosmicShaders.setupVoronoiUniforms(cfg);
        VertexConsumer consumer = buffers.getBuffer(ModRenderType.voronoiRenderType);
        renderRegularItemCosmic(stack, pose, consumer, light, overlay);
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
    }

    public void collectData(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose,
                            MultiBufferSource buffers, int light, int overlay) {
        YuanRenderStateSnapshot renderState = YuanRenderStateSnapshot.capture();
        PENDING_RENDERS.add(new PendingRender(
                stack.copy(), context, leftHand,
                new Matrix4f(pose.last().pose()),
                new Matrix3f(pose.last().normal()),
                light, overlay, wrapped, new ArrayList<>(maskSprites), renderState));
    }

    public static void renderAllPendingItems(float partialTicks, PoseStack worldPoseStack) {
        if (PENDING_RENDERS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        List<PendingRender> firstPersonItems = new ArrayList<>();
        List<PendingRender> otherItems = new ArrayList<>();
        for (PendingRender data : PENDING_RENDERS) {
            if (data.context() == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || data.context() == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                firstPersonItems.add(data);
            } else {
                otherItems.add(data);
            }
        }
        for (PendingRender data : otherItems) {
            renderSingleItem(data, bufferSource);
        }
        for (PendingRender data : firstPersonItems) {
            RenderSystem.clear(256, Minecraft.ON_OSX);
            renderSingleItem(data, bufferSource);
        }
        PENDING_RENDERS.clear();
    }

    private static void renderSingleItem(PendingRender data, MultiBufferSource.BufferSource bufferSource) {
        PoseStack pose = new PoseStack();
        try {
            data.renderState().restore();
            pose.pushPose();
            pose.last().pose().set(data.poseMatrix());
            pose.last().normal().set(data.normalMatrix());
            new YuanCosmicBakedModel(data.wrapped(), data.maskSprites())
                    .renderItem(data.stack(), data.context(), pose, bufferSource, data.light(), data.overlay());
        } finally {
            bufferSource.endBatch();
            pose.popPose();
            data.renderState().cleanup();
        }
    }

    private void renderBase(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        for (BakedModel pass : wrapped.getRenderPasses(stack, true)) {
            for (RenderType type : pass.getRenderTypes(stack, true)) {
                itemRenderer.renderModelLists(pass, stack, light, overlay, pose, buffers.getBuffer(type));
            }
        }
    }

    private void renderCosmic(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (YuanCosmicShaders.cosmicShader == null) {
            return;
        }
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
        boolean gui = context == ItemDisplayContext.GUI;
        float scale = gui ? 1.2f * 30.0f : 1.2f;
        // ArcaneVortex VanSh default: deep teal-black background.
        YuanCosmicShaders.setupCosmicUniforms(scale, 1.0f, 1, new Vector4f(0.0f, 0.02f, 0.03f, 1.0f), gui);
        int atlasId = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getId();
        YuanCosmicShaders.cosmicShader.setSampler("Sampler0", atlasId);
        VertexConsumer consumer = buffers.getBuffer(ModRenderType.cosmicRenderType);
        renderRegularItemCosmic(stack, pose, consumer, light, overlay);
        if (buffers instanceof MultiBufferSource.BufferSource) {
            ((MultiBufferSource.BufferSource) buffers).endBatch();
        }
    }

    private void renderRegularItemCosmic(ItemStack stack, PoseStack pose, VertexConsumer consumer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderQuadList(pose, consumer, maskQuads(maskSprites), stack, light, overlay);
    }

    private record MaskQuadCacheEntry(List<TextureAtlasSprite> sprites, List<BakedQuad> quads) {}

    /**
     * Per-pixel mask geometry depends only on the atlas sprites, not on the stack or the render
     * style, and every style path (cosmic/silk/tunnel/voronoi) needs the same quads. Sprites are
     * recreated by each resource reload, which is exactly when the baked UVs go stale, so the cache
     * is keyed on sprite identity and stays small enough to survive reload churn.
     */
    private static List<BakedQuad> maskQuads(List<ResourceLocation> masks) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        List<TextureAtlasSprite> sprites = new ArrayList<>(masks.size());
        for (ResourceLocation mask : masks) {
            sprites.add(atlas.apply(mask));
        }
        for (MaskQuadCacheEntry entry : MASK_QUAD_CACHE) {
            if (sameSprites(entry.sprites(), sprites)) {
                return entry.quads();
            }
        }
        List<BakedQuad> quads = bakeMaskQuads(sprites);
        if (MASK_QUAD_CACHE.size() >= MASK_QUAD_CACHE_LIMIT) {
            MASK_QUAD_CACHE.clear();
        }
        MASK_QUAD_CACHE.add(new MaskQuadCacheEntry(List.copyOf(sprites), List.copyOf(quads)));
        return quads;
    }

    private static boolean sameSprites(List<TextureAtlasSprite> a, List<TextureAtlasSprite> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static List<BakedQuad> bakeMaskQuads(List<TextureAtlasSprite> sprites) {
        List<BakedQuad> quads = new ArrayList<>();
        for (int i = 0; i < sprites.size(); i++) {
            TextureAtlasSprite sprite = sprites.get(i);
            List<BlockElement> refElements = ITEM_MODEL_GENERATOR.processFrames(i, "layer" + i, sprite.contents());
            BlockElement ref = null;
            for (BlockElement element : refElements) {
                if (element.faces.containsKey(Direction.SOUTH)) {
                    ref = element;
                    break;
                }
            }
            if (ref == null) {
                continue;
            }
            NativeImage originalImage = sprite.contents().getOriginalImage();
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            float scaleX = 16.0f / width;
            float scaleY = 16.0f / height;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (((originalImage.getPixelRGBA(x, y) >> 24) & 255) == 0) {
                        continue;
                    }
                    float u0 = x * scaleX;
                    float v0 = y * scaleY;
                    float u1 = (x + 1) * scaleX;
                    float v1 = (y + 1) * scaleY;
                    BlockFaceUV uv = new BlockFaceUV(new float[]{u0, v0, u1, v1}, 0);
                    BlockElementFace face = new BlockElementFace(null, -1, "", uv);
                    Map<Direction, BlockElementFace> faces = new HashMap<>();
                    faces.put(Direction.SOUTH, face);
                    float modelY0 = 16.0f - v1;
                    float modelY1 = 16.0f - v0;
                    BlockElement element = new BlockElement(
                            new Vector3f(u0, modelY0, ref.from.z()),
                            new Vector3f(u1, modelY1, ref.to.z()),
                            faces, ref.rotation, ref.shade);
                    quads.add(FACE_BAKERY.bakeQuad(element.from, element.to, face, sprite, Direction.SOUTH,
                            IDENTITY_STATE, element.rotation, element.shade,
                            ResourceLocation.fromNamespaceAndPath("yuan", "dynamic_cosmic")));
                }
            }
        }
        return quads;
    }
}
