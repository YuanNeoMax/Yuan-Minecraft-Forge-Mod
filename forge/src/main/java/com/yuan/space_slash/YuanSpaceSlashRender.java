package com.yuan.space_slash;

import com.yuan.Yuan;
import com.yuan.client.cosmic.YuanIris;
import com.yuan.client.cosmic.YuanRenderStateSnapshot;
import com.yuan.item.YuanGodSwordConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class YuanSpaceSlashRender {
    private static final List<YuanSpaceSlashEffect> effects = new ArrayList<>();
    private static YuanRenderStateSnapshot capturedState;
    private static int clientEntityCounter;

    private YuanSpaceSlashRender() {
    }

    public static void spawn(Entity target, int seed, float yaw, float pitch, float roll,
                             double hitX, double hitY, double hitZ) {
        if (target == null) {
            return;
        }
        ItemStack held = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getMainHandItem()
                : ItemStack.EMPTY;
        YuanGodSwordConfig swordConfig = new YuanGodSwordConfig();
        swordConfig.read(held);
        YuanSpaceSlashParams params = YuanSpaceSlashParams.from(swordConfig);
        if (!params.enabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            YuanPhotonSlashBridge.playRain(mc.level, new Vec3(hitX, hitY, hitZ));
        }
        if ((YuanIris.isShaderPackActive() || ModList.get().isLoaded("oculus")) && mc.level != null) {
            YuanSpaceSlashEntity slashEntity =
                    new YuanSpaceSlashEntity(Yuan.SPACE_SLASH_ENTITY.get(), mc.level);
            slashEntity.setId(-200000 - (++clientEntityCounter));
            slashEntity.setPos(hitX, hitY, hitZ);
            slashEntity.initSlash(target, seed, yaw, pitch, roll, hitX, hitY, hitZ, params);
            mc.level.addEntity(slashEntity.getId(), slashEntity);
            return;
        }
        Vec3 offset = new Vec3(hitX, hitY, hitZ).subtract(target.position());
        effects.add(new YuanSpaceSlashEffect(target, seed, yaw, pitch, roll, offset, params));
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent event) {
        boolean useFallback = YuanIris.isShaderPackActive() || ModList.get().isLoaded("oculus");
        if (useFallback) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                capturedState = YuanRenderStateSnapshot.capture();
                return;
            }
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                return;
            }
            if (effects.isEmpty()) {
                return;
            }
            if (capturedState != null) {
                capturedState.restore();
            }
            try {
                renderEffects(event);
            } finally {
                if (capturedState != null) {
                    capturedState.cleanup();
                }
                capturedState = null;
            }
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (effects.isEmpty()) {
            return;
        }
        renderEffects(event);
    }

    private static void renderEffects(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        Iterator<YuanSpaceSlashEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            YuanSpaceSlashEffect effect = iterator.next();
            if (effect.isFinished()) {
                iterator.remove();
                continue;
            }
            effect.render(event.getPoseStack(), buffer, camera.x, camera.y, camera.z);
        }
    }
}
