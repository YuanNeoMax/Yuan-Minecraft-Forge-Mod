package com.yuan.domain;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class YuanDomainStatePacket {
    private final UUID holder;
    private final boolean active;
    private final float radius;

    public YuanDomainStatePacket(UUID holder, boolean active, float radius) {
        this.holder = holder;
        this.active = active;
        this.radius = radius;
    }

    public static void encode(YuanDomainStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.holder);
        buffer.writeBoolean(message.active);
        buffer.writeFloat(message.radius);
    }

    public static YuanDomainStatePacket decode(FriendlyByteBuf buffer) {
        return new YuanDomainStatePacket(buffer.readUUID(), buffer.readBoolean(), buffer.readFloat());
    }

    public static void handle(YuanDomainStatePacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> YuanDomainClient.apply(message.holder, message.active, message.radius));
        context.get().setPacketHandled(true);
    }
}
