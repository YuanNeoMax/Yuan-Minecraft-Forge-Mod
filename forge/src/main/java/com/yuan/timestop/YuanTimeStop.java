package com.yuan.timestop;

import com.yuan.item.YuanGodSwordConfig;
import com.yuan.timerewind.YuanTimeRewindClient;
import com.yuan.timestop.render.YuanTimeStopEffect;
import com.yuan.timestop.render.YuanTimeStopParticle;
import com.yuan.timestop.render.YuanTimeStopRender;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

public final class YuanTimeStop {
    public static long millis = 0L;
    public static long realMillis = 0L;
    private static volatile YuanGodSwordConfig activeConfig = new YuanGodSwordConfig();
    private static volatile long nextAllowedMillis = 0L;
    private static boolean locallyStarted = false;

    private YuanTimeStop() {
    }

    public static void setActiveConfig(YuanGodSwordConfig config) {
        activeConfig = config;
    }

    public static YuanGodSwordConfig getActiveConfig() {
        return activeConfig;
    }

    public static boolean cooldownReady(int ticks) {
        return ticks <= 0 || System.currentTimeMillis() >= nextAllowedMillis;
    }

    public static void startCooldown(int ticks) {
        nextAllowedMillis = System.currentTimeMillis() + (long) Math.max(0, ticks) * 50L;
    }

    public static long cooldownRemainingMillis() {
        return Math.max(0L, nextAllowedMillis - System.currentTimeMillis());
    }

    public static float livePartialTick() {
        return Minecraft.getInstance().getPartialTick();
    }

    public static boolean shouldFreezeEntity(Entity entity) {
        if (entity instanceof Player) {
            return YuanTimeStopServerState.isFreezeSelf();
        }
        if (entity instanceof Projectile || entity instanceof ItemEntity) {
            return true;
        }
        boolean boss = entity instanceof EnderDragon || entity instanceof WitherBoss;
        boolean freeze = boss
                ? YuanTimeStopServerState.isFreezeBossAI()
                : YuanTimeStopServerState.isFreezeEntities();
        if (!freeze) {
            return false;
        }
        float radius = YuanTimeStopServerState.getStopRadius();
        return radius <= 0.0f || entity.distanceToSqr(
                YuanTimeStopServerState.getWielderX(),
                YuanTimeStopServerState.getWielderY(),
                YuanTimeStopServerState.getWielderZ()) <= radius * radius;
    }

    public static boolean shouldBlockPlayerInput() {
        if (!get() || !YuanTimeStopServerState.isFreezeSelf()) {
            return false;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        float radius = YuanTimeStopServerState.getStopRadius();
        if (radius <= 0.0f) {
            return true;
        }
        double dx = player.getX() - YuanTimeStopServerState.getWielderX();
        double dy = player.getY() - YuanTimeStopServerState.getWielderY();
        double dz = player.getZ() - YuanTimeStopServerState.getWielderZ();
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    public static boolean shouldCancelCameraTick(Camera camera) {
        if (!get()) {
            return false;
        }
        Entity entity = camera.getEntity();
        return entity == null || shouldFreezeEntity(entity);
    }

    public static boolean shouldCancelItemInHandTick() {
        if (!get()) {
            return false;
        }
        Player player = Minecraft.getInstance().player;
        return player != null && shouldFreezeEntity(player);
    }

    public static boolean shouldCancelTickFov() {
        if (!get()) {
            return false;
        }
        return shouldCancelCameraTick(Minecraft.getInstance().gameRenderer.getMainCamera());
    }

    public static void freezeLerp(Entity entity) {
        if (YuanTimeRewindClient.isActive()) {
            if (!YuanTimeRewindClient.shouldFreezeEntity(entity)) {
                return;
            }
        } else if (!shouldFreezeEntity(entity)) {
            return;
        }
        entity.xOld = entity.getX();
        entity.yOld = entity.getY();
        entity.zOld = entity.getZ();
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.yRotO = entity.getYRot();
        entity.xRotO = entity.getXRot();
    }

    public static void abs(final Player player) {
        if (!get()) {
            if (YuanTimeStopConfig.specialShader) {
                float[] rgb = activeConfig.ballColorCustom
                        ? rgbFromInt(activeConfig.customColor)
                        : ballColorRgb(activeConfig.ballColor);
                ClientLevel level = Minecraft.getInstance().level;
                for (int i = 0; i < activeConfig.particleCount; i++) {
                    double ox = (level.random.nextDouble() - 0.5) * 0.6;
                    double oy = (level.random.nextDouble() - 0.5) * 0.6;
                    double oz = (level.random.nextDouble() - 0.5) * 0.6;
                    YuanTimeStopParticle particle = new YuanTimeStopParticle(
                            level, player.getX() + ox, player.getY() + oy, player.getZ() + oz,
                            0.4f, 0.0, 0.0, 0.0, YuanTimeStopRender.beam.toString(),
                            activeConfig.particleSize, rgb[0], rgb[1], rgb[2],
                            activeConfig.particleAlpha, false, 1.2) {
                        @Override
                        public Vec3 getPos() {
                            return player.position();
                        }
                    };
                    particle.rotationSpeed = activeConfig.particleSpin;
                    particle.setLifetime(80);
                    Minecraft.getInstance().particleEngine.add(particle);
                }
            }
        }
        if (!get()) {
            Minecraft.getInstance().gameRenderer.shutdownEffect();
        }
        Minecraft.getInstance().particleEngine.tick();
    }

    public static void setIsTimeStop(boolean value) {
        boolean wasStopped = get();
        Minecraft mc = Minecraft.getInstance();
        if (!value) {
            mc.timer.lastMs = Util.getMillis();
            mc.getSoundManager().resume();
        } else {
            mc.getSoundManager().pause();
        }
        YuanTimeStopServerState.setStopped(value);
        if (wasStopped && !value) {
            scheduleLightRecheck();
        }
    }

    /** Marks that this client started the stop, so only its owner gets the local end flourish. */
    public static void markLocalStart() {
        locallyStarted = true;
    }

    public static boolean consumeLocalEnd() {
        boolean was = locallyStarted;
        locallyStarted = false;
        return was;
    }

    public static void spawnStartEffect(Player player) {
        if (activeConfig.startAnim == 2) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player == null) {
            return;
        }
        float[] rgb = activeConfig.ballColorCustom
                ? rgbFromInt(activeConfig.customColor)
                : ballColorRgb(activeConfig.ballColor);
        float[] rgba = new float[]{rgb[0], rgb[1], rgb[2], activeConfig.particleAlpha};
        float duration = activeConfig.startDuration;
        if (activeConfig.startAnim == 0) {
            int count = Math.max(1, activeConfig.particleCount);
            Vec3 eye = player.position().add(0.0, 0.9, 0.0);
            for (int i = 0; i < count; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0;
                double pitch = (level.random.nextDouble() - 0.5) * 1.4;
                double dx = Math.cos(angle) * Math.cos(pitch);
                double dy = Math.sin(pitch) * 0.6;
                double dz = Math.sin(angle) * Math.cos(pitch);
                float radius = (1.0f + activeConfig.particleSize)
                        * (0.7f + level.random.nextFloat() * 0.6f);
                YuanTimeStopRender.spawnEffect(new YuanTimeStopEffect(
                        YuanTimeStopEffect.Kind.BURST,
                        eye.add(dx * 0.35, dy * 0.35, dz * 0.35),
                        duration, radius, rgba));
            }
        } else {
            Vec3 origin = new Vec3(player.getX(), player.getY() + 0.05, player.getZ());
            float radius = 5.0f + activeConfig.particleSize * 2.0f;
            YuanTimeStopRender.spawnEffect(new YuanTimeStopEffect(
                    YuanTimeStopEffect.Kind.SHOCKWAVE,
                    origin, duration, radius, rgba));
        }
    }

    public static void spawnEndEffect(Player player) {
        if (activeConfig.endAnim == 1) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player == null) {
            return;
        }
        float[] rgb = activeConfig.ballColorCustom
                ? rgbFromInt(activeConfig.customColor)
                : ballColorRgb(activeConfig.ballColor);
        float[] rgba = new float[]{rgb[0], rgb[1], rgb[2], activeConfig.particleAlpha};
        float duration = activeConfig.endDuration;
        Vec3 eye = player.position().add(0.0, 0.9, 0.0);
        int count = Math.max(1, activeConfig.particleCount);
        for (int i = 0; i < count; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 0.8;
            double oy = (level.random.nextDouble() - 0.5) * 0.6;
            double oz = (level.random.nextDouble() - 0.5) * 0.8;
            float radius = 1.0f + activeConfig.particleSize;
            YuanTimeStopRender.spawnEffect(new YuanTimeStopEffect(
                    YuanTimeStopEffect.Kind.COLLAPSE,
                    eye.add(ox, oy, oz), duration, radius, rgba));
        }
    }

    /**
     * Must be called after setIsTimeStop(true): SoundEngine.pause() only pauses the channels that
     * already exist, so a sting started afterwards plays over the frozen world instead of being
     * suspended until the stop ends.
     */
    public static void playStartSound(Player player) {
        player.playSound(YuanTimeStopSounds.STOP.get(), activeConfig.soundVolume, 1.0f);
    }

    public static void playEndSound(Player player) {
        if (!activeConfig.soundEnabled || player == null) {
            return;
        }
        player.playSound(YuanTimeStopSounds.START.get(), activeConfig.soundVolume, 1.0f);
    }

    private static void scheduleLightRecheck() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        mc.execute(() -> {
            ClientLevel level = mc.level;
            Player player = mc.player;
            if (level == null || player == null) {
                return;
            }
            BlockPos center = player.blockPosition();
            int radius = 32;
            // One reused cursor instead of 274,625 BlockPos allocations from center.offset(...); the
            // engine only needs an immutable coordinate for the few positions that actually emit light.
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                        if (level.getBlockState(cursor).getLightEmission() > 0) {
                            level.getChunkSource().getLightEngine().checkBlock(cursor.immutable());
                        }
                    }
                }
            }
        });
    }

    public static boolean get() {
        return YuanTimeStopServerState.isStopped();
    }

    private static float[] ballColorRgb(int index) {
        return switch (index) {
            case 1 -> new float[]{0.91f, 0.365f, 0.239f};
            case 2 -> new float[]{1.0f, 0.788f, 0.729f};
            case 3 -> new float[]{0.286f, 0.196f, 0.173f};
            case 4 -> new float[]{0.486f, 0.361f, 1.0f};
            case 5 -> new float[]{0.239f, 0.839f, 0.91f};
            case 6 -> new float[]{0.478f, 0.91f, 0.365f};
            case 7 -> new float[]{1.0f, 0.851f, 0.239f};
            default -> new float[]{1.0f, 0.478f, 0.361f};
        };
    }

    private static float[] rgbFromInt(int color) {
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f
        };
    }
}
