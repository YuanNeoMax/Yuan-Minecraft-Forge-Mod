package com.yuan.space_slash;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class YuanSpaceSlashEntityRenderer extends EntityRenderer<YuanSpaceSlashEntity> {
    private static final float PARALLEL_EPSILON = 0.08f;

    public YuanSpaceSlashEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(YuanSpaceSlashEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Override
    public void render(YuanSpaceSlashEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        float progress = entity.progress();
        if (progress <= 0.0f || progress >= 1.0f) {
            return;
        }
        if (!entity.isOrientationReady()) {
            Vector3f[] orientation = computeOrientation(entity);
            entity.setOrientation(orientation[0], orientation[1], orientation[2]);
        }
        YuanSpaceSlashParams params = entity.getParams();
        float scale = Mth.lerp(progress, params.startScale, params.endScale);
        float length = entity.getBaseLength() * params.lengthMult * scale;
        float width = Math.max(0.02f, length * params.widthRatio);
        float thickness = Math.max(0.005f, length * params.thicknessRatio);
        float alpha = 1.0f - progress * progress;
        RenderType bodyType = params.depthTest
                ? YuanSpaceSlashRenderType.SLASH_FALLBACK_DEPTH
                : YuanSpaceSlashRenderType.SLASH_FALLBACK;
        RenderType glowType = params.depthTest
                ? YuanSpaceSlashRenderType.SLASH_GLOW_FALLBACK_DEPTH
                : YuanSpaceSlashRenderType.SLASH_GLOW_FALLBACK;

        pose.pushPose();
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 toCamera = new Vec3(
                camera.x - entity.getX(), camera.y - entity.getY(), camera.z - entity.getZ());
        if (toCamera.lengthSqr() > 1.0e-8D) {
            toCamera = toCamera.normalize();
            pose.translate(
                    toCamera.x * params.surfaceOffset,
                    toCamera.y * params.surfaceOffset,
                    toCamera.z * params.surfaceOffset);
        }
        YuanSpaceSlashMesh.drawSlashBladeFallback(pose, buffer, bodyType,
                length, width, thickness, alpha,
                entity.getLongAxis(), entity.getSide(), entity.getThick(), progress, params);
        if (params.glow) {
            YuanSpaceSlashMesh.drawSlashGlowFallback(pose, buffer, glowType,
                    length * 1.08f, width * 1.14f, alpha * 0.55f,
                    entity.getLongAxis(), entity.getSide(), progress, params);
        }
        pose.popPose();
    }

    private static Vector3f[] computeOrientation(YuanSpaceSlashEntity entity) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f right = YuanSpaceSlashEffect.normalizeOrElse(new Vector3f(look).cross(up, new Vector3f()),
                new Vector3f(1.0f, 0.0f, 0.0f));

        Vector3f attack = YuanSpaceSlashEffect.direction(entity.getYaw(), entity.getPitch()).toVector3f();
        Vector3f axis = YuanSpaceSlashEffect.screenProjection(attack, right, up);
        if (axis.lengthSquared() < PARALLEL_EPSILON * PARALLEL_EPSILON) {
            float angle = entity.getParams().randomAngle
                    ? ((entity.getSeed() & 0xFFFF) / 65535.0f) * (float) Math.PI
                    : (float) (Math.PI / 4.0D);
            axis = new Vector3f(right).mul((float) Math.cos(angle))
                    .add(new Vector3f(up).mul((float) Math.sin(angle)));
        }
        axis.normalize();

        Vector3f side = YuanSpaceSlashEffect.normalizeOrElse(new Vector3f(axis).cross(look, new Vector3f()), up);
        Vector3f thick = YuanSpaceSlashEffect.normalizeOrElse(new Vector3f(axis).cross(side, new Vector3f()), look);
        return new Vector3f[]{axis, side, thick};
    }
}
