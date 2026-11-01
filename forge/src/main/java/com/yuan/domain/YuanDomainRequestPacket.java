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

    /** Whether the packet means "toggle the domain" or "re-send its appearance". */
    private final boolean refresh;

    public YuanDomainRequestPacket(boolean refresh) {
        this.refresh = refresh;
    }

    public static void encode(YuanDomainRequestPacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.refresh);
    }

    public static YuanDomainRequestPacket decode(FriendlyByteBuf buffer) {
        return new YuanDomainRequestPacket(buffer.readBoolean());
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
            // A REFRESH RE-SENDS, IT DOES NOT TOGGLE. The appearance settings live on the sword and
            // reach other clients only through this broadcast, so moving a density slider while
            // the domain is already open changed NOTHING for anyone else -- they kept drawing the
            // sky as it was when the domain was first expanded. Re-broadcasting on every tweak
            // keeps every client's view of the same domain identical, which is the whole point of
            // the state being on the wire at all.
            boolean active;
            if (message.refresh) {
                active = YuanDomainServerState.isActive(sender.getUUID());
                if (!active) {
                    // Nothing to refresh: a closed domain has no appearance to publish.
                    return;
                }
            } else {
                active = YuanDomainServerState.toggle(sender.getUUID());
            }
            YuanDomainServerState.broadcast(sender.getUUID(), active, config.domainStarfield,
                    config.domainStarDensity, config.domainSpinScale, config.domainStarSpikes);
            if (message.refresh) {
                // A refresh is a settings tweak, not an event: no message, and no log line that
                // would bury the expansions in the log while a slider is being dragged.
                return;
            }
            LOGGER.info("domain {} by {}", active ? "expanded" : "collapsed",
                    sender.getGameProfile().getName());
            sender.displayClientMessage(Component.literal(active ? "§c✦ 领域展开" : "§c✦ 领域收束"), true);
        });
        context.get().setPacketHandled(true);
    }
}
