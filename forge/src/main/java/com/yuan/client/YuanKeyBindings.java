package com.yuan.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.yuan.Yuan;
import com.yuan.client.gui.YuanComposeTestScreen;
import com.yuan.client.gui.YuanConfigScreen;
import com.yuan.domain.YuanDomainRequestPacket;
import com.yuan.item.YuanConfig;
import com.yuan.item.YuanGodSwordConfig;
import com.yuan.item.YuanGodSwordItem;
import com.yuan.item.YuanSwordItem;
import com.yuan.network.ConfigSyncPacket;
import com.yuan.timerewind.YuanTimeRewindCancelPacket;
import com.yuan.timerewind.YuanTimeRewindClient;
import com.yuan.timerewind.YuanTimeRewindRequestPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class YuanKeyBindings {
    enum KeyRequest { NONE, CONFIG }

    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
        "key.yuan.config", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.yuan");

    public static final KeyMapping TRIGGER_TIMESTOP = new KeyMapping(
        "key.yuan.timestop_trigger", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.yuan");

    public static final KeyMapping TRIGGER_REWIND = new KeyMapping(
        "key.yuan.rewind_trigger", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.yuan");

    public static final KeyMapping TRIGGER_DOMAIN = new KeyMapping(
        "key.yuan.domain_trigger", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.yuan");

    /**
     * Opens the god sword's Aurora configuration window.
     *
     * <p>Was a temporary blur-pipeline test binding during the interface's build-out; now it is the
     * real entry point, which is why the name survives rather than being renamed: the lang entry that
     * labels it in the controls screen is keyed on it, and a rename would silently drop the label.
     */
    public static final KeyMapping AURORA_POC = new KeyMapping(
        "key.yuan.aurora_poc", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.yuan");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
        event.register(TRIGGER_TIMESTOP);
        event.register(TRIGGER_REWIND);
        event.register(TRIGGER_DOMAIN);
        event.register(AURORA_POC);
    }

    @Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class KeyHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            int configClicks = 0;
            int triggerClicks = 0;
            int rewindClicks = 0;
            int domainClicks = 0;
            int auroraClicks = 0;
            while (OPEN_CONFIG.consumeClick()) configClicks++;
            while (TRIGGER_TIMESTOP.consumeClick()) triggerClicks++;
            while (TRIGGER_REWIND.consumeClick()) rewindClicks++;
            while (TRIGGER_DOMAIN.consumeClick()) domainClicks++;
            while (AURORA_POC.consumeClick()) auroraClicks++;
            if (mc.player == null) return;
            // 进世界后预热 Compose/Skiko，避免首次打开界面卡 1 秒
            YuanComposeTestScreen.warmupIfNeeded();
            ItemStack stack = mc.player.getMainHandItem();
            ItemStack offhand = mc.player.getOffhandItem();
            // B opens the sword's Aurora configuration window. It needs the god sword in hand, because
            // the window edits that sword's own config -- opening against nothing would present a
            // settings page whose save has nowhere to go.
            if (mc.screen == null && auroraClicks > 0) {
                if (stack.getItem() instanceof YuanGodSwordItem) {
                    mc.setScreen(new com.yuan.client.aurora.AuroraGodSwordScreen(stack));
                } else {
                    mc.player.displayClientMessage(Component.literal("§c✦ 需要手持神器之刃"), true);
                }
                return;
            }
            if (mc.screen == null && rewindClicks > 0 && hasGodSwordInInventory(mc.player)) {
                if (YuanTimeRewindClient.isActive()) {
                    Yuan.CHANNEL.sendToServer(new YuanTimeRewindCancelPacket());
                } else {
                    Yuan.CHANNEL.sendToServer(new YuanTimeRewindRequestPacket());
                }
                return;
            }
            if (mc.screen == null && domainClicks > 0) {
                if (hasGodSwordInHand(mc.player)) {
                    Yuan.CHANNEL.sendToServer(new YuanDomainRequestPacket(false));
                } else {
                    mc.player.displayClientMessage(Component.literal("§c✦ 展开领域需手持神剑"), true);
                }
                return;
            }
            if (mc.screen == null && triggerClicks > 0
                    && stack.getItem() instanceof YuanGodSwordItem) {
                YuanGodSwordConfig cfg = new YuanGodSwordConfig();
                cfg.read(stack);
                if (cfg.enabled && cfg.triggerMode == 2) {
                    YuanGodSwordItem.trigger(mc, mc.player, stack, InteractionHand.MAIN_HAND);
                }
                return;
            }
            if (request(configClicks, mc.screen != null) == KeyRequest.NONE) return;
            if (stack.getItem() instanceof YuanGodSwordItem) {
                if (configClicks > 0 && !hasCtrl() && !isShift()) {
                    mc.setScreen(new YuanComposeTestScreen(stack));
                }
                return;
            }
            if (!(stack.getItem() instanceof YuanSwordItem)) return;
            if (hasCtrl()) {
                YuanConfig.presetAllOn(stack);
                syncConfig(stack);
                mc.player.displayClientMessage(Component.literal("§a✦ 预设: 全开"), true);
            } else if (isShift()) {
                String cur = stack.getOrCreateTagElement("YuanConfig").getString("preset");
                switch (cur) {case"":case"all":YuanConfig.presetAttack(stack);store(stack,"attack");break;case"attack":YuanConfig.presetDefense(stack);store(stack,"defense");break;case"defense":YuanConfig.presetTimeStop(stack);store(stack,"timestop");break;default:YuanConfig.presetAllOn(stack);store(stack,"all");}
                syncConfig(stack);
                mc.player.displayClientMessage(Component.literal("§a✦ 预设: "+stack.getOrCreateTagElement("YuanConfig").getString("preset")), true);
            } else {
                mc.setScreen(new YuanConfigScreen(stack));
            }
        }
        static void store(ItemStack s, String p) { s.getOrCreateTagElement("YuanConfig").putString("preset", p); }
        static void syncConfig(ItemStack stack) {
            Yuan.CHANNEL.sendToServer(new ConfigSyncPacket(stack.getOrCreateTagElement("YuanConfig").copy()));
        }

        private static boolean hasGodSwordInInventory(Player player) {
            if (player.getMainHandItem().getItem() instanceof YuanGodSwordItem) return true;
            if (player.getOffhandItem().getItem() instanceof YuanGodSwordItem) return true;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof YuanGodSwordItem) return true;
            }
            return false;
        }

        private static boolean hasGodSwordInHand(Player player) {
            return player.getMainHandItem().getItem() instanceof YuanGodSwordItem
                    || player.getOffhandItem().getItem() instanceof YuanGodSwordItem;
        }
    }

    static KeyRequest request(int configClicks, boolean screenOpen) {
        if (screenOpen) return KeyRequest.NONE;
        return configClicks > 0 ? KeyRequest.CONFIG : KeyRequest.NONE;
    }

    @Mod.EventBusSubscriber(modid = "yuan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class MouseHandler {
        private static boolean godSwordRightHeld = false;

        @SubscribeEvent
        public static void onMouse(InputEvent.MouseButton.Pre event) {
            if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
            if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_RELEASE) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof YuanGodSwordItem) {
                if (event.getAction() == GLFW.GLFW_PRESS) {
                    if (godSwordRightHeld) {
                        event.setCanceled(true);
                        return;
                    }
                    YuanGodSwordConfig cfg = new YuanGodSwordConfig();
                    cfg.read(stack);
                    if (!cfg.enabled || cfg.triggerMode == 2) {
                        event.setCanceled(true);
                        return;
                    }
                    if (cfg.triggerMode == 1 && !isShift()) {
                        return;
                    }
                    godSwordRightHeld = true;
                    event.setCanceled(true);
                    YuanGodSwordItem.trigger(mc, mc.player, stack, InteractionHand.MAIN_HAND);
                } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                    godSwordRightHeld = false;
                }
                return;
            }
        }
    }

    static boolean hasCtrl() {
        long w = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }
    static boolean isShift() {
        long w = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
