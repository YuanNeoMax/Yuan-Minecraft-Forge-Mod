package com.yuan.client.cosmic;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class YuanRenderStateSnapshot {
    private final boolean depthTest;
    private final boolean depthMask;
    private final int depthFunc;
    private final boolean blend;
    private final int blendSrcRgb;
    private final int blendDstRgb;
    private final int blendSrcAlpha;
    private final int blendDstAlpha;
    private final boolean cull;
    private final int cullFace;
    private final float[] colorMaskValues;
    private final int polygonMode;
    private final Matrix4f modelViewMatrix;
    private final Matrix4f projectionMatrix;
    private final VertexSorting vertexSorting;
    private boolean modelViewPushed;

    private YuanRenderStateSnapshot(boolean depthTest, boolean depthMask, int depthFunc, boolean blend,
                                    int blendSrcRgb, int blendDstRgb, int blendSrcAlpha, int blendDstAlpha,
                                    boolean cull, int cullFace, float[] colorMaskValues, int polygonMode,
                                    Matrix4f modelViewMatrix, Matrix4f projectionMatrix, VertexSorting vertexSorting) {
        this.depthTest = depthTest;
        this.depthMask = depthMask;
        this.depthFunc = depthFunc;
        this.blend = blend;
        this.blendSrcRgb = blendSrcRgb;
        this.blendDstRgb = blendDstRgb;
        this.blendSrcAlpha = blendSrcAlpha;
        this.blendDstAlpha = blendDstAlpha;
        this.cull = cull;
        this.cullFace = cullFace;
        this.colorMaskValues = colorMaskValues;
        this.polygonMode = polygonMode;
        this.modelViewMatrix = new Matrix4f(modelViewMatrix);
        this.projectionMatrix = new Matrix4f(projectionMatrix);
        this.vertexSorting = vertexSorting;
    }

    public static YuanRenderStateSnapshot capture() {
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        int blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        int blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        int blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
        float[] colorMaskValues = new float[4];
        GL11.glGetFloatv(GL11.GL_COLOR_WRITEMASK, colorMaskValues);
        int polygonMode = GL11.glGetInteger(GL11.GL_POLYGON_MODE);
        return new YuanRenderStateSnapshot(
                depthTest, depthMask, depthFunc, blend,
                blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                cull, cullFace, colorMaskValues, polygonMode,
                new Matrix4f(RenderSystem.getModelViewMatrix()),
                new Matrix4f(RenderSystem.getProjectionMatrix()),
                RenderSystem.getVertexSorting());
    }

    public void restore() {
        if (depthTest) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask(depthMask);
        RenderSystem.depthFunc(depthFunc);
        if (blend) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        } else {
            RenderSystem.disableBlend();
        }
        if (cull) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }
        RenderSystem.colorMask(
                colorMaskValues[0] > 0.5f, colorMaskValues[1] > 0.5f,
                colorMaskValues[2] > 0.5f, colorMaskValues[3] > 0.5f);
        RenderSystem.polygonMode(GL11.GL_FRONT_AND_BACK, polygonMode);
        RenderSystem.getModelViewStack().pushPose();
        modelViewPushed = true;
        RenderSystem.getModelViewStack().setIdentity();
        RenderSystem.getModelViewStack().mulPoseMatrix(modelViewMatrix);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projectionMatrix, vertexSorting);
    }

    public void cleanup() {
        // Only pop when restore() actually pushed: it can throw part-way, and an unmatched pop
        // underflows the shared model view stack for the rest of the game.
        if (modelViewPushed) {
            modelViewPushed = false;
            RenderSystem.getModelViewStack().popPose();
            RenderSystem.applyModelViewMatrix();
        }
        RenderSystem.setProjectionMatrix(projectionMatrix, vertexSorting);
    }
}
