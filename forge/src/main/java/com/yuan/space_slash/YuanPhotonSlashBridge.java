package com.yuan.space_slash;

import com.lowdragmc.photon.client.fx.BlockEffect;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays the authored Photon streak-rain FX at a space-slash hit point. The FX itself
 * ({@code yuan:space_slash_rain}) is authored in the in-game Photon editor; until it exists this
 * bridge is a silent no-op. Photon is bundled as a nested mod, and a failure here must never take
 * down the base slash rendering, so every call is guarded and failures log at most once per 30s.
 */
public final class YuanPhotonSlashBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("YuanSpaceSlash");
    private static final ResourceLocation SLASH_RAIN_FX =
            ResourceLocation.fromNamespaceAndPath("yuan", "space_slash_rain");
    private static long lastFailureLogMillis;

    private YuanPhotonSlashBridge() {
    }

    public static void playRain(ClientLevel level, Vec3 hit) {
        try {
            FX fx = FXHelper.getFX(SLASH_RAIN_FX);
            if (fx == null) {
                return;
            }
            BlockEffect effect = new BlockEffect(fx, level, BlockPos.containing(hit.x, hit.y, hit.z));
            // The FX is a momentary visual, not a block attachment: do not let a block change kill it.
            effect.setCheckState(false);
            effect.setAllowMulti(true);
            effect.setOffset(new Vector3f(
                    (float) (hit.x - effect.pos.getX()),
                    (float) (hit.y - effect.pos.getY()),
                    (float) (hit.z - effect.pos.getZ())));
            effect.start();
        } catch (Throwable t) {
            long now = System.currentTimeMillis();
            if (now - lastFailureLogMillis > TimeUnit.SECONDS.toMillis(30)) {
                lastFailureLogMillis = now;
                LOGGER.warn("[YuanSpaceSlash] photon streak fx 'yuan:space_slash_rain' unavailable", t);
            }
        }
    }
}
