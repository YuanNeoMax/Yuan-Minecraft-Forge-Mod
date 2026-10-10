package com.yuan.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class YuanDomainClient {
    private static final Map<UUID, Float> ACTIVE = new HashMap<>();
    private static final Map<UUID, Float> READ_ONLY = Collections.unmodifiableMap(ACTIVE);

    private YuanDomainClient() {
    }

    public static void apply(UUID holder, boolean active, float radius) {
        if (active) {
            ACTIVE.put(holder, radius);
        } else {
            ACTIVE.remove(holder);
        }
    }

    public static boolean isEmpty() {
        return ACTIVE.isEmpty();
    }

    public static boolean isActive(UUID holder) {
        return ACTIVE.containsKey(holder);
    }

    public static float radiusOf(UUID holder) {
        Float radius = ACTIVE.get(holder);
        return radius == null ? 0.0f : radius;
    }

    public static Map<UUID, Float> active() {
        return READ_ONLY;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }
}
