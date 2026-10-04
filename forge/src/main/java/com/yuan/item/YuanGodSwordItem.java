package com.yuan.item;

import com.yuan.Yuan;
import com.yuan.space_slash.YuanSpaceSlashPacket;
import com.yuan.timestop.YuanTimeStop;
import com.yuan.timestop.YuanTimeStopRequestPacket;
import com.yuan.timestop.YuanTimeStopServerState;
import com.yuan.timestop.YuanTimeStopShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Consumer;

public class YuanGodSwordItem extends SwordItem {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("YuanGodSwordItem");
    public YuanGodSwordItem(Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            YuanGodSwordConfig config = new YuanGodSwordConfig();
            config.read(stack);
            if (!config.enabled || config.triggerMode == 2) {
                return super.use(level, player, hand);
            }
            if (config.triggerMode == 1 && !player.isShiftKeyDown()) {
                return super.use(level, player, hand);
            }
            trigger(Minecraft.getInstance(), player, stack, hand);
        }
        return super.use(level, player, hand);
    }

    public static void trigger(Minecraft mc, Player player, ItemStack stack, InteractionHand hand) {
        if (mc == null || player == null || stack == null || stack.isEmpty()) {
            return;
        }
        YuanGodSwordConfig config = new YuanGodSwordConfig();
        config.read(stack);
        if (!config.enabled) {
            return;
        }
        if (!YuanTimeStop.cooldownReady(config.cooldown)) {
            player.displayClientMessage(
                    Component.literal(
                            "§7冷却中 " + String.format(java.util.Locale.ROOT, "%.1fs",
                                    YuanTimeStop.cooldownRemainingMillis() / 1000.0)),
                    true);
            return;
        }
        YuanTimeStop.setActiveConfig(config);
        YuanTimeStopServerState.setFreezeSelf(config.freezeSelf);
        YuanTimeStopServerState.setFreezeEntities(config.freezeEntities);
        YuanTimeStopServerState.setFreezeBlocks(config.freezeBlocks);
        YuanTimeStopServerState.setFreezeFluids(config.freezeFluids);
        YuanTimeStopServerState.setFreezeBossAI(config.freezeBossAI);
        YuanTimeStopServerState.setStopRadius(config.stopRadius);
        YuanTimeStopServerState.setWielderPosition(player.getX(), player.getY(), player.getZ());
        YuanTimeStop.abs(player);
        boolean next = !YuanTimeStop.get();
        if (next) {
            YuanTimeStop.markLocalStart();
            YuanTimeStop.spawnStartEffect(player);
        } else {
            YuanTimeStop.consumeLocalEnd();
            YuanTimeStop.playEndSound(player);
            YuanTimeStop.spawnEndEffect(player);
            YuanTimeStopServerState.resetFreezeDefaults();
        }
        YuanTimeStop.setIsTimeStop(next);
        if (next && config.soundEnabled) {
            YuanTimeStop.playStartSound(player);
        }
        YuanTimeStopShaders.post();
        if (config.showMessage) {
            player.displayClientMessage(
                    Component.literal(next ? "§e✦ 时停开始" : "§7✦ 时停结束"),
                    true);
        }
        Yuan.CHANNEL.sendToServer(new YuanTimeStopRequestPacket(next));
        YuanTimeStop.startCooldown(config.cooldown);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hurt = super.hurtEnemy(stack, target, attacker);
        if (hurt && attacker instanceof Player && !attacker.level().isClientSide) {
              YuanGodSwordConfig cfg = new YuanGodSwordConfig();
              cfg.read(stack);
              if (!cfg.slashEnabled) return hurt;
              int seed = attacker.level().random.nextInt();
              float roll = attacker.level().random.nextFloat() * cfg.slashRollRange;
              Vec3 eye = attacker.getEyePosition(1.0F);
              Vec3 look = attacker.getLookAngle().normalize();
              Vec3 end = eye.add(look.scale(6.0D));
              Vec3 hit = target.getBoundingBox().clip(eye, end).orElse(
                      target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
              Yuan.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                    new YuanSpaceSlashPacket(target.getId(), seed,
                            attacker.getYRot(), attacker.getXRot(), roll,
                            hit.x, hit.y, hit.z));
        }
        return hurt;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        LOGGER.info("[YuanGodSwordItem] initializeClient");
    }
}
