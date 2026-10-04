package com.yuan.mixin;

import com.yuan.Yuan;
import com.yuan.timestop.YuanTimeStopConfig;
import com.yuan.timestop.YuanTimeStopPacket;
import com.yuan.timestop.YuanTimeStopServerState;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class TimestopMinecraftServerMixin {
    /**
     * Ends a manual-expiry time stop. This deliberately does not cancel tickServer any more: the
     * world freeze is enforced by the per-level, per-entity and chunk-cache mixins, while cancelling
     * here also skipped saveEverything (no autosave at all during a stop, so a crash lost the work),
     * the server status refresh, and Forge's ServerTickEvent for every other mod.
     */
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void timestopTick(CallbackInfo ci) {
        if (!YuanTimeStopServerState.isStopped()) {
            return;
        }
        long until = YuanTimeStopServerState.getStopUntilMillis();
        if (until > 0L && System.currentTimeMillis() >= until) {
            YuanTimeStopServerState.setStopped(false);
            YuanTimeStopServerState.setInvulnerable(YuanTimeStopConfig.invulnerable);
            YuanTimeStopServerState.setStopUntilMillis(0L);
            YuanTimeStopServerState.resetFreezeDefaults();
            Yuan.CHANNEL.send(PacketDistributor.ALL.noArg(), new YuanTimeStopPacket(
                    false, -1,
                    YuanTimeStopServerState.isFreezeSelf(),
                    YuanTimeStopServerState.isFreezeEntities(),
                    YuanTimeStopServerState.isFreezeBlocks(),
                    YuanTimeStopServerState.isFreezeFluids(),
                    YuanTimeStopServerState.isFreezeBossAI(),
                    YuanTimeStopServerState.getStopRadius(),
                    YuanTimeStopServerState.getWielderX(),
                    YuanTimeStopServerState.getWielderY(),
                    YuanTimeStopServerState.getWielderZ()));
        }
    }
}
