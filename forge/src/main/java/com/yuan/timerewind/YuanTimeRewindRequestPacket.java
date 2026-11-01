package com.yuan.timerewind;

import com.yuan.Yuan;
import com.yuan.item.YuanGodSwordConfig;
import com.yuan.item.YuanGodSwordItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class YuanTimeRewindRequestPacket {
    public YuanTimeRewindRequestPacket() {}

    public static void encode(YuanTimeRewindRequestPacket message, FriendlyByteBuf buffer) {}

    public static YuanTimeRewindRequestPacket decode(FriendlyByteBuf buffer) {
        return new YuanTimeRewindRequestPacket();
    }

    public static void handle(YuanTimeRewindRequestPacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null || YuanTimeRewindServerState.isRewinding()) return;
            ItemStack stack = findSword(player);
            if (stack.isEmpty()) return;
            YuanGodSwordConfig config = new YuanGodSwordConfig();
            config.read(stack);
            if (!config.rewindEnabled) return;
            if (!config.rewindTimestopStacking && com.yuan.timestop.YuanTimeStopServerState.isStopped()) return;
            if (!YuanTimeRewindServerState.cooldownReady(player.getUUID(),
                    config.rewindCooldownTicks,
                    YuanTimeRewindServerState.CooldownKind.MANUAL)) return;
            YuanGodSwordConfig restoreConfig = YuanTimeRewindRestorer.withEffectivePositionRewind(config, false);
            if (!YuanTimeRewindRestorer.restore(player.serverLevel(), restoreConfig, player, false, 0)) return;
            YuanTimeRewindServerState.startCooldown(
                    player.getUUID(), restoreConfig.rewindCooldownTicks,
                YuanTimeRewindServerState.CooldownKind.MANUAL);
            if (YuanTimeRewindEvents.shouldRestoreSword(restoreConfig, false)) {
                if (restoreConfig.rewindPlaybackMode == 0) {
                    YuanTimeRewindEvents.setPendingSword(stack.copy());
                } else {
                    YuanTimeRewindEvents.ensureSwordHeld(player, stack);
                }
            }
            YuanTimeRewindEvents.sendRewindStart(new YuanTimeRewindStartPacket(
                    player.getId(),
                    restoreConfig.rewindCameraMode,
                    restoreConfig.rewindFreeCamRestorePosition,
                    restoreConfig.rewindPositionRewind,
                    restoreConfig.rewindPositionMode,
                    restoreConfig.rewindPlaybackSeconds,
                    restoreConfig.rewindFreezeOthers,
                    YuanTimeRewindServerState.getActiveTargetPosition().x,
                    YuanTimeRewindServerState.getActiveTargetPosition().y,
                    YuanTimeRewindServerState.getActiveTargetPosition().z,
                    YuanTimeRewindServerState.getActiveRetreatTargets()));
        });
        context.get().setPacketHandled(true);
    }

    private static ItemStack findSword(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (isEnabledRewindSword(main)) return main;
        ItemStack off = player.getOffhandItem();
        if (isEnabledRewindSword(off)) return off;
        for (ItemStack stack : player.getInventory().items) {
            if (isEnabledRewindSword(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isEnabledRewindSword(ItemStack stack) {
        if (!isGodSword(stack)) return false;
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.read(stack);
        return config.rewindEnabled;
    }

    private static boolean isGodSword(ItemStack stack) {
        return stack.getItem() instanceof YuanGodSwordItem;
    }
}
