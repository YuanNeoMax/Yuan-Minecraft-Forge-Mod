package com.yuan.client.cosmic;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class YuanDeferredRender {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // Drained even when the pack was switched off since the items were collected, otherwise the
        // queue would keep entries that nothing ever renders or clears.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            YuanCosmicBakedModel.renderAllPendingItems(event.getPartialTick(), event.getPoseStack());
        }
    }
}
