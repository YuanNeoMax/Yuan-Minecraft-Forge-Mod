package com.yuan.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side mirror of every active domain.
 *
 * <p>Keyed by holder rather than being one global: domains are per-player and several can be open at
 * once. The star-field flag travels in its own packet because the toggle lives on the holder's
 * sword, which this client cannot read for anyone but itself.
 */
@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class YuanDomainClient {

    /** One holder's domain: whether the star-field floor is on. */
    public record Entry(boolean starfield) {
    }

    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();
    private static final Map<UUID, Entry> READ_ONLY = Collections.unmodifiableMap(ACTIVE);

    private YuanDomainClient() {
    }

    public static void apply(UUID holder, boolean active, boolean starfield) {
        if (active) {
            ACTIVE.put(holder, new Entry(starfield));
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

    /** Whether this holder has the star-field floor switched on. */
    public static boolean starfieldOf(UUID holder) {
        Entry entry = ACTIVE.get(holder);
        return entry != null && entry.starfield();
    }

    public static Map<UUID, Entry> active() {
        return READ_ONLY;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
        // Drop render-side animation state too, so the next session starts with the floor closed
        // rather than resuming a half-open fade from the previous one.
        com.yuan.domain.render.YuanDomainRender.reset();
    }
}
