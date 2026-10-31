package com.yuan.domain;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Broadcasts one holder's domain state to every client.
 *
 * <p>Carries {@code starfield}: the star-field toggle lives on the holder's sword, and a client can
 * only read its own config. Without this field, other players' clients would have to guess whether
 * to draw the floor -- and guessing wrong means a floor appearing for someone who switched it off,
 * or a missing floor for someone who left it on. The old {@code radius} field was removed with the
 * radius slider: the star field is a full-screen post-process with no cut-off.
 */
public class YuanDomainStatePacket {
    private final UUID holder;
    private final boolean active;
    private final boolean starfield;

    public YuanDomainStatePacket(UUID holder, boolean active, boolean starfield) {
        this.holder = holder;
        this.active = active;
        this.starfield = starfield;
    }

    public static void encode(YuanDomainStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.holder);
        buffer.writeBoolean(message.active);
        buffer.writeBoolean(message.starfield);
    }

    public static YuanDomainStatePacket decode(FriendlyByteBuf buffer) {
        return new YuanDomainStatePacket(buffer.readUUID(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(YuanDomainStatePacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            org.slf4j.LoggerFactory.getLogger("YuanDomainPacket").info(
                    "state packet: holder={} active={} starfield={}",
                    message.holder, message.active, message.starfield);
            YuanDomainClient.apply(message.holder, message.active, message.starfield);
        });
        context.get().setPacketHandled(true);
    }
}
