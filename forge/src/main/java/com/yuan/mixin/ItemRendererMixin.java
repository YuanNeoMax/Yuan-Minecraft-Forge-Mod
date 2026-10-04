package com.yuan.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuan.client.cosmic.YuanCosmicBakedModel;
import com.yuan.client.cosmic.YuanIris;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void yuanCosmicRender(ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose, MultiBufferSource buffers, int light, int overlay, BakedModel model, CallbackInfo ci) {
        if (model instanceof YuanCosmicBakedModel) {
            ci.cancel();
            pose.pushPose();
            try {
                ForgeHooksClient.handleCameraTransforms(pose, model, context, leftHand);
                pose.translate(-0.5, -0.5, -0.5);
                YuanCosmicBakedModel cosmicModel = (YuanCosmicBakedModel) model;
                // collectData copies the matrices it needs, so popping below is safe for the deferred path.
                if (YuanIris.isShaderPackActive() && context != ItemDisplayContext.GUI) {
                    cosmicModel.collectData(stack, context, leftHand, pose, buffers, light, overlay);
                } else {
                    cosmicModel.renderItem(stack, context, pose, buffers, light, overlay);
                }
            } finally {
                pose.popPose();
            }
        }
    }
}
