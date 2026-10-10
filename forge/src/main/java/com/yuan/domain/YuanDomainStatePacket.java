package com.yuan.domain;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Broadcasts one holder's domain state to every client.
 *
 * <p>Carries {@code starfield} as well as the radius: the star-field toggle lives on the holder's
 * sword, and a client can only read its own config. Without this field, other players' clients would
 * have to guess whether to draw the floor -- and guessing wrong means a floor appearing for someone
 * who switched it off, or a missing floor for someone who left it on.
 */
public class YuanDomainStatePacket {
    private final UUID holder;
    private final boolean active;
    private final float radius;
    private final boolean starfield;

    public YuanDomainStatePacket(UUID holder, boolean active, float radius, boolean starfield) {
        this.holder = holder;
        this.active = active;
        this.radius = radius;
        this.starfield = starfield;
    }

    public static void encode(YuanDomainStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.holder);
        buffer.writeBoolean(message.active);
        buffer.writeFloat(message.radius);
        buffer.writeBoolean(message.starfield);
    }

    public static YuanDomainStatePacket decode(FriendlyByteBuf buffer) {
        return new YuanDomainStatePacket(buffer.readUUID(), buffer.readBoolean(),
                buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(YuanDomainStatePacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            org.slf4j.LoggerFactory.getLogger("YuanDomainPacket").info(
                    "state packet: holder={} active={} radius={} starfield={}",
                    message.holder, message.active, message.radius, message.starfield);
            YuanDomainClient.apply(message.holder, message.active, message.radius, message.starfield);
        });
        context.get().setPacketHandled(true);
    }
}
