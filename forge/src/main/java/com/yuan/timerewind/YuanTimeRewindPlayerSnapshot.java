package com.yuan.timerewind;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class YuanTimeRewindPlayerSnapshot {
    private YuanTimeRewindPlayerSnapshot() {}

    public static CompoundTag capture(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        player.saveWithoutId(tag);
        return tag;
    }

    public static void restore(ServerPlayer player, CompoundTag tag) {
        restore(player, tag, true);
    }

    /**
     * Restores a player from a snapshot -- AFTER taking them out of whatever they were inside.
     *
     * <p>`load` replaces the inventory, the experience and the whole entity state at once. That is
     * exactly what a rewind wants, and it is also why the caller cannot simply call it: a player
     * standing in a chest GUI has an OPEN CONTAINER MENU holding slot references into the item
     * stacks that are about to be replaced. The server would then be serving a menu whose slots
     * point at objects that no longer exist, while the client keeps drawing them -- items appear to
     * duplicate or vanish, and clicking a slot writes into the discarded copy.
     *
     * <p>Riding is the same problem in a different shape: the vehicle keeps a passenger reference to
     * an entity whose position and passengers `load` has just rewritten, so the player is left
     * attached to a mount that no longer agrees with them.
     *
     * <p>So both are closed FIRST, and the menu is re-sent afterwards. Closing a menu is not a
     * cosmetic act -- `doCloseContainer` is what tells the client to stop treating those slots as
     * live -- and there is no way to keep it open across a load that would leave it consistent.
     */
    public static void restore(ServerPlayer player, CompoundTag tag, boolean restorePosition) {
        CompoundTag effective = effectiveRestoreTag(tag, restorePosition);
        if (effective == null) return;

        // Close whatever the player is looking at before its slots are replaced.
        if (player.containerMenu != player.inventoryMenu) {
            player.doCloseContainer();
        }
        // And get off the mount before its passenger list is rewritten.
        if (player.isPassenger()) {
            player.stopRiding();
        }

        player.load(effective);

        // The inventory menu mirrors the player's own inventory, so it has to be rebuilt against
        // the stacks `load` just installed -- otherwise the player's own GUI keeps showing the
        // pre-rewind contents until something else touches it.
        player.initInventoryMenu();
        player.inventoryMenu.broadcastChanges();
    }

    static CompoundTag effectiveRestoreTag(CompoundTag tag, boolean restorePosition) {
        if (tag == null) return null;
        CompoundTag effective = tag;
        if (!restorePosition) {
            effective = tag.copy();
            effective.remove("Pos");
            effective.remove("Motion");
            effective.remove("Rotation");
        }
        return effective;
    }
}
