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
    /**
     * Whether the legacy mesh slash (entity path under Iris/Oculus, plus the fallback effect list)
     * is drawn. Currently off: since 2026-10-04 the space slash plays through
     * {@link YuanPhotonSlashBridge} instead, and the user asked for the photon streak only.
     *
     * <p>The legacy path is kept compiled and wired on purpose — it is the only slash visual proven
     * to work <em>inside a shader pack</em>, whereas the photon streak's behaviour under Iris/Oculus
     * has not been verified. Flip this to {@code true} to restore it when:
     * <ul>
     *   <li>the photon streak misbehaves or disappears while a shader pack is active, or</li>
     *   <li>the photon route is abandoned and the mesh slash is wanted back.</li>
     * </ul>
     *
     * <p>Everything the legacy path needs is still live: the entity type and its renderer stay
     * registered in {@code YuanClient}, and {@link YuanSpaceSlashEffect}'s static helpers are shared
     * with the photon bridge. {@code YuanSpaceSlashCheck} asserts those wiring points, so deleting
     * any of them will fail the build rather than silently break the fallback.
     */
    private static final boolean LEGACY_SLASH_VISIBLE = false;
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
            // Same span the legacy blade uses, so the photon streak covers the same cut.
            float slashLength = YuanSpaceSlashEffect.lengthFor(target) * params.lengthMult;
            YuanPhotonSlashBridge.playRain(mc.level, target, new Vec3(hitX, hitY, hitZ), yaw, pitch, slashLength);
        }
        if (LEGACY_SLASH_VISIBLE
                && (YuanIris.isShaderPackActive() || ModList.get().isLoaded("oculus")) && mc.level != null) {
            YuanSpaceSlashEntity slashEntity =
                    new YuanSpaceSlashEntity(Yuan.SPACE_SLASH_ENTITY.get(), mc.level);
            slashEntity.setId(-200000 - (++clientEntityCounter));
            slashEntity.setPos(hitX, hitY, hitZ);
            slashEntity.initSlash(target, seed, yaw, pitch, roll, hitX, hitY, hitZ, params);
            mc.level.addEntity(slashEntity.getId(), slashEntity);
            return;
        }
        if (!LEGACY_SLASH_VISIBLE) {
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
