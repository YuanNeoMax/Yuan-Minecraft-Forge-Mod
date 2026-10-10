package com.yuan.space_slash;

import com.yuan.client.cosmic.YuanIris;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;

public final class YuanSpaceSlashEffect {
    private static final float PARALLEL_EPSILON = 0.08f;

    private final YuanSpaceSlashParams params;
    private final int entityId;
    private final int seed;
    private final float yaw;
    private final float pitch;
    private final float roll;
    private final Vec3 offset;
    private final long startMillis;
    private Vec3 lastPos;
    private Vector3f longAxis;
    private Vector3f side;
    private Vector3f thick;

    public YuanSpaceSlashEffect(Entity target, int seed, float yaw, float pitch, float roll,
                                Vec3 offset, YuanSpaceSlashParams params) {
        this.params = params;
        this.entityId = target.getId();
        this.seed = seed;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.offset = offset;
        this.startMillis = System.currentTimeMillis();
        this.lastPos = target.position().add(offset);
    }

    public boolean isFinished() {
        return progress() >= 1.0f;
    }

    public void render(PoseStack matrix, MultiBufferSource.BufferSource buffer,
                       double cameraX, double cameraY, double cameraZ) {
        float progress = progress();
        if (progress <= 0.0f || progress >= 1.0f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        Vec3 center = entity != null && !entity.isRemoved()
                ? entity.position().add(offset)
                : lastPos;
        if (entity != null && !entity.isRemoved()) {
            lastPos = center;
        }

        float scale = Mth.lerp(progress, params.startScale, params.endScale);
        float length = lengthFor(entity) * params.lengthMult * scale;
        float width = Math.max(0.02f, length * params.widthRatio);
        float thickness = Math.max(0.005f, length * params.thicknessRatio);
        float alpha = 1.0f - progress * progress;

        Camera camera = mc.gameRenderer.getMainCamera();
        if (longAxis == null) {
            initOrientation(camera);
        }

        matrix.pushPose();
        matrix.translate(center.x - cameraX, center.y - cameraY, center.z - cameraZ);

        if (YuanIris.isShaderPackActive() || ModList.get().isLoaded("oculus")) {
            RenderType bodyType = params.depthTest
                    ? YuanSpaceSlashRenderType.SLASH_FALLBACK_DEPTH
                    : YuanSpaceSlashRenderType.SLASH_FALLBACK;
            RenderType glowType = params.depthTest
                    ? YuanSpaceSlashRenderType.SLASH_GLOW_FALLBACK_DEPTH
                    : YuanSpaceSlashRenderType.SLASH_GLOW_FALLBACK;
            YuanSpaceSlashMesh.drawSlashBladeFallback(matrix, buffer,
                    bodyType,
                    length, width, thickness, alpha, longAxis, side, thick, progress, params);
            buffer.endBatch(bodyType);
            if (params.glow) {
                YuanSpaceSlashMesh.drawSlashGlowFallback(matrix, buffer, glowType,
                        length * 1.08f, width * 1.14f, alpha * 0.55f,
                        longAxis, side, progress, params);
                buffer.endBatch(glowType);
            }
            matrix.popPose();
            return;
        }

        RenderType bodyType = params.depthTest
                ? YuanSpaceSlashRenderType.SLASH_BODY_DEPTH
                : YuanSpaceSlashRenderType.SLASH_BODY;
        RenderType glowType = params.depthTest
                ? YuanSpaceSlashRenderType.SLASH_GLOW_DEPTH
                : YuanSpaceSlashRenderType.SLASH_GLOW;
        YuanSpaceSlashShaders.configureBody(params, progress, seed);
        YuanSpaceSlashMesh.drawSlashBlade(matrix, buffer,
                bodyType,
                length, width, thickness, alpha, longAxis, side, thick);
        buffer.endBatch(bodyType);

        if (params.glow) {
            YuanSpaceSlashShaders.configureGlow(params, progress, seed);
            YuanSpaceSlashMesh.drawSlashGlow(matrix, buffer,
                    glowType,
                    length * 1.08f, width * 1.14f, alpha * 0.55f,
                    longAxis, side);
            buffer.endBatch(glowType);
        }

        matrix.popPose();
    }

    private void initOrientation(Camera camera) {
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f right = normalizeOrElse(new Vector3f(look).cross(up, new Vector3f()),
                new Vector3f(1.0f, 0.0f, 0.0f));

        Vector3f attack = direction(yaw, pitch).toVector3f();
        Vector3f axis = screenProjection(attack, right, up);
        if (axis.lengthSquared() < PARALLEL_EPSILON * PARALLEL_EPSILON) {
            float angle = params.randomAngle
                    ? ((seed & 0xFFFF) / 65535.0f) * (float) Math.PI
                    : (float) (Math.PI / 4.0D);
            axis = new Vector3f(right).mul((float) Math.cos(angle))
                    .add(new Vector3f(up).mul((float) Math.sin(angle)));
        }
        axis.normalize();
        this.longAxis = axis;
        this.side = normalizeOrElse(new Vector3f(axis).cross(look, new Vector3f()), up);
        this.thick = normalizeOrElse(new Vector3f(axis).cross(this.side, new Vector3f()), look);
    }

    /**
     * Normalizes only when the vector is not degenerate. A zero-length cross product normalizes to
     * NaN, and every NaN comparison is false, so a check placed after normalize() can never fire.
     */
    public static Vector3f normalizeOrElse(Vector3f vector, Vector3f fallback) {
        if (vector.lengthSquared() < 1.0e-6f) {
            return new Vector3f(fallback).normalize();
        }
        return vector.normalize();
    }

    public static Vector3f screenProjection(Vector3f attack, Vector3f right, Vector3f up) {
        return new Vector3f(right).mul(attack.dot(right))
                .add(new Vector3f(up).mul(attack.dot(up)));
    }

    private float progress() {
        long elapsed = System.currentTimeMillis() - startMillis;
        float durationMs = Math.max(0.05f, params.durationSeconds) * 1000.0f;
        return (float) Mth.clamp((double) elapsed / durationMs, 0.0D, 1.0D);
    }

    public static float lengthFor(Entity entity) {
        if (entity != null && !entity.isRemoved()) {
            return Math.max(1.1f, entity.getBbWidth() * 0.9f + entity.getBbHeight() * 0.7f);
        }
        return 1.6f;
    }

    public static Vec3 direction(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        Vec3 dir = new Vec3(
                -Math.cos(pitchRad) * Math.sin(yawRad),
                -Math.sin(pitchRad),
                Math.cos(pitchRad) * Math.cos(yawRad));
        if (dir.lengthSqr() < 1.0e-6D) {
            dir = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return dir.normalize();
    }

}
