package com.yuan.domain;

import com.yuan.item.YuanGodSwordConfig;
import com.yuan.item.YuanGodSwordItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YuanDomainRequestPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger("YuanDomain");

    public static void encode(YuanDomainRequestPacket message, FriendlyByteBuf buffer) {
    }

    public static YuanDomainRequestPacket decode(FriendlyByteBuf buffer) {
        return new YuanDomainRequestPacket();
    }

    public static void handle(YuanDomainRequestPacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer sender = context.get().getSender();
            if (sender == null) {
                return;
            }
            ItemStack sword = sender.getMainHandItem().getItem() instanceof YuanGodSwordItem
                    ? sender.getMainHandItem() : sender.getOffhandItem();
            if (!(sword.getItem() instanceof YuanGodSwordItem)) {
                return;
            }
            YuanGodSwordConfig config = new YuanGodSwordConfig();
            config.read(sword);
            if (!config.enabled) {
                return;
            }
            boolean active = YuanDomainServerState.toggle(sender.getUUID(), config.domainRadius);
            YuanDomainServerState.broadcast(sender.getUUID(), active, config.domainRadius,
                    config.domainStarfield);
            LOGGER.info("domain {} by {} radius={}", active ? "expanded" : "collapsed",
                    sender.getGameProfile().getName(), String.format(java.util.Locale.ROOT, "%.1f", config.domainRadius));
            sender.displayClientMessage(Component.literal(active ? "§c✦ 领域展开" : "§c✦ 领域收束"), true);
        });
        context.get().setPacketHandled(true);
    }
}
