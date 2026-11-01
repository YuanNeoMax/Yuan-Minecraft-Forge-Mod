package com.yuan.domain;

import com.yuan.Yuan;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class YuanDomainServerState {
    private static final Map<UUID, Boolean> ACTIVE = new HashMap<>();

    private YuanDomainServerState() {
    }

    public static synchronized boolean toggle(UUID holder) {
        if (ACTIVE.remove(holder) != null) {
            return false;
        }
        ACTIVE.put(holder, Boolean.TRUE);
        return true;
    }

    public static synchronized boolean isActive(UUID holder) {
        return ACTIVE.containsKey(holder);
    }

    public static void broadcast(UUID holder, boolean active, boolean starfield,
                                 float starDensity, float spinScale, boolean starSpikes) {
        Yuan.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new YuanDomainStatePacket(holder, active, starfield, starDensity, spinScale, starSpikes));
    }

    @SubscribeEvent
    public static synchronized void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (ACTIVE.remove(player.getUUID()) != null) {
            // The star-field flag is irrelevant on a deactivation, but the packet shape is fixed.
            broadcast(player.getUUID(), false, false, 1.0f, 1.0f, true);
        }
    }

    @SubscribeEvent
    public static synchronized void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }
}
