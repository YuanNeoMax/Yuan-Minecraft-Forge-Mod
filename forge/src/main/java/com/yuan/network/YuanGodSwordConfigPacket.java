package com.yuan.network;

import com.yuan.item.YuanGodSwordConfig;
import com.yuan.item.YuanGodSwordItem;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class YuanGodSwordConfigPacket {
    private static final int MAX_PRESETS = 8;

    private final CompoundTag tag;

    public YuanGodSwordConfigPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public static void encode(YuanGodSwordConfigPacket message, FriendlyByteBuf buffer) {
        buffer.writeNbt(message.tag);
    }

    public static YuanGodSwordConfigPacket decode(FriendlyByteBuf buffer) {
        return new YuanGodSwordConfigPacket(buffer.readNbt());
    }

    public static void handle(YuanGodSwordConfigPacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null || message.tag == null) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            // This configuration belongs to the god sword alone. A swapped, dropped or emptied main
            // hand must discard the packet instead of stamping client NBT onto whatever item is held.
            if (!(stack.getItem() instanceof YuanGodSwordItem)) {
                return;
            }
            // Round-tripping through the config makes the server own the key set and the value
            // ranges, so arbitrary client-supplied tags never land on the stack, and write() merges
            // into the existing child tag instead of replacing it (the preset list survives).
            ItemStack probe = new ItemStack(stack.getItem());
            probe.addTagElement(YuanGodSwordConfig.TAG, message.tag.copy());
            YuanGodSwordConfig config = new YuanGodSwordConfig();
            config.read(probe);
            config.write(stack);
            copyPresets(message.tag, stack);
            com.yuan.timerewind.YuanTimeRewindEvents.invalidateWindowCache();
        });
        context.get().setPacketHandled(true);
    }

    /**
     * Preset slots live inside the same child tag, and the config screen is the only place they are
     * edited, so they travel with the config save. Each incoming preset is pushed back through the
     * config itself: a hand-edited list cannot smuggle arbitrary keys onto the stack, and the slot
     * count is bounded so a client cannot grow the item's NBT without limit.
     */
    private static void copyPresets(CompoundTag incoming, ItemStack stack) {
        if (!incoming.contains(YuanGodSwordConfig.PRESETS_KEY)) {
            return;
        }
        ListTag incomingPresets = incoming.getList(YuanGodSwordConfig.PRESETS_KEY, Tag.TAG_COMPOUND);
        CompoundTag target = stack.getOrCreateTagElement(YuanGodSwordConfig.TAG);
        if (incomingPresets.isEmpty()) {
            target.remove(YuanGodSwordConfig.PRESETS_KEY);
            return;
        }
        ListTag normalized = new ListTag();
        for (int i = 0; i < incomingPresets.size() && i < MAX_PRESETS; i++) {
            CompoundTag preset = incomingPresets.getCompound(i);
            if (preset.isEmpty()) {
                normalized.add(new CompoundTag());
                continue;
            }
            ItemStack probe = new ItemStack(stack.getItem());
            probe.addTagElement(YuanGodSwordConfig.TAG, preset.copy());
            YuanGodSwordConfig presetConfig = new YuanGodSwordConfig();
            presetConfig.read(probe);
            normalized.add(presetConfig.toTag());
        }
        target.put(YuanGodSwordConfig.PRESETS_KEY, normalized);
    }
}
