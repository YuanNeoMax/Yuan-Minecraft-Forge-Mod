package com.yuan;

import java.nio.file.Files;
import java.nio.file.Path;

public final class YuanTimeStopCheck {
    public static void main(String[] args) throws Exception {
        String yuan = Files.readString(Path.of("src/main/java/com/yuan/Yuan.java"));
        assert yuan.contains("YuanTimeStopSounds.SOUNDS.register(bus)")
                : "sounds must be registered on the mod event bus";
        assert yuan.contains("YuanTimeStopConfig.SPEC")
                : "client config must be registered";
        assert yuan.contains("ModConfig.Type.COMMON")
                : "common config must be registered";
        assert yuan.contains("YuanTimeStopConfig.SERVER_SPEC")
                : "server config spec must be registered";

        String config = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopConfig.java"));
        assert config.contains("SERVER_SPEC")
                : "server config spec must be defined";

        String sounds = Files.readString(Path.of("src/main/resources/assets/yuan/sounds.json"));
        assert sounds.contains("\"stop\"") && sounds.contains("yuan:stop")
                : "stop sound entry missing";
        assert Files.exists(Path.of("src/main/resources/assets/yuan/sounds/stop.ogg"))
                : "stop.ogg missing";

        assert yuan.contains("registerMessage(8, YuanTimeStopPacket.class")
                : "timestop packet id 8 must be registered";
        assert yuan.contains("registerMessage(9, YuanTimeStopRequestPacket.class")
                : "serverbound timestop request packet id 9 must be registered";
        String state = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStop.java"));
        assert state.contains("setIsTimeStop")
                && state.contains("public static boolean get()")
                : "YuanTimeStop state methods missing";
        assert state.contains("YuanTimeStopServerState")
                : "YuanTimeStop must delegate state to the server-safe holder";
        assert state.contains("livePartialTick")
                : "YuanTimeStop must expose a live client partial tick for the exempt player";
        assert Files.exists(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopServerState.java"))
                : "YuanTimeStopServerState missing";
        String serverState = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopServerState.java"));
        assert serverState.contains("isStopped()") && serverState.contains("setStopped(boolean value)")
                && serverState.contains("isInvulnerable()")
                && !serverState.contains("net.minecraft.client")
                : "server-safe timestop state holder must not depend on client classes";
        assert serverState.contains("isFreezeEntities()") && serverState.contains("isFreezeBlocks()")
                && serverState.contains("isFreezeFluids()") && serverState.contains("isFreezeBossAI()")
                && serverState.contains("getStopRadius()") && serverState.contains("resetFreezeDefaults()")
                && serverState.contains("cooldownReady") && serverState.contains("startCooldown")
                : "server-safe timestop state holder must carry freeze toggles and radius";
        String request = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopRequestPacket.java"));
        assert request.contains("YuanTimeStopServerState")
                : "serverbound timestop request handler must use the server-safe state holder";
        assert request.contains("getSender()")
                : "serverbound timestop request handler must validate the sender";
        assert request.contains("YuanGodSwordItem")
                : "serverbound timestop request handler must require the Yuan God Sword";
        assert request.contains("YuanGodSwordConfig") && request.contains("config.enabled")
                : "serverbound timestop request must honor the per-sword enabled flag";
        assert request.contains("setInvulnerable(config.invulnerable)")
                : "serverbound timestop request must apply per-sword invulnerability";
        assert request.contains("setStopUntilMillis")
                : "serverbound timestop request must schedule automatic stop duration";
        assert request.contains("startCooldown") && !request.contains("getCooldowns().addCooldown")
                : "serverbound timestop request must use real-time cooldown only";
        assert request.contains("config.cooldown")
                : "serverbound timestop request must use the per-sword cooldown";
        String events = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopServerEvents.java"));
        assert events.contains("PlayerLoggedInEvent")
                : "server login sync must send timestop state on player login";
        assert events.contains("TickEvent.ServerTickEvent") && events.contains("getStopUntilMillis()")
                : "server tick must auto-stop timed timestop";
        String shaders = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopShaders.java"));
        assert shaders.contains("loadEffect") && shaders.contains("shutdownEffect")
                : "shader load/shutdown missing";
        assert shaders.contains("YuanTimeStopConfig.specialShader")
                : "shader post must be gated by specialShader";
        assert shaders.contains("TickEvent.RenderTickEvent")
                : "shader counters must advance from render tick";
        assert shaders.contains("Util.getMillis()")
                : "shader render tick must advance counters from real elapsed millis";
        assert shaders.contains("getActiveConfig()") && shaders.contains("config.grayStrength")
                && shaders.contains("grayAnimate")
                : "saturation must derive from per-sword grayScreen/grayStrength/grayAnimate";
        assert shaders.contains("grayStyle") && shaders.contains("effectLocation")
                : "shader loading must honor the per-sword gray style";
        assert shaders.contains("value = Dist.CLIENT")
                : "shader subscriber must be restricted to Dist.CLIENT";

        String packet = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopPacket.java"));
        assert packet.contains("YuanTimeStopConfig.specialShader")
                : "timestop packet must gate shader loading on specialShader";

        String item = Files.readString(Path.of("src/main/java/com/yuan/item/YuanGodSwordItem.java"));
        assert item.contains("YuanTimeStop.abs(player)")
                : "right-click must trigger timestop visuals";
        assert item.contains("YuanGodSwordConfig") && item.contains("config.enabled")
                : "god sword right-click must honor the per-sword enabled flag";
        assert item.contains("YuanTimeStop.setActiveConfig(config)")
                : "god sword right-click must publish the per-sword config to timestop";
        assert item.contains("triggerMode") && item.contains("config.cooldown")
                : "god sword right-click must honor trigger mode and per-sword cooldown";
        assert item.contains("YuanTimeStop.cooldownReady(config.cooldown)")
                && item.contains("YuanTimeStop.startCooldown")
                : "god sword right-click must respect the real-time cooldown";
        assert item.contains("public static void trigger")
                : "god sword must expose a centralized trigger entry point";
        assert !item.contains("getCooldowns().addCooldown")
                : "god sword must not use the vanilla item cooldown table";
        assert item.contains("config.showMessage")
                : "god sword right-click must honor the show-message toggle";
        assert item.contains("YuanTimeStop.setIsTimeStop(next)")
                : "right-click must toggle timestop state";
        assert item.contains("YuanTimeStopShaders.post()")
                : "right-click must load/shutdown the_world shader";
        assert item.contains("YuanTimeStopRequestPacket") && item.contains("sendToServer")
                : "right-click must send the serverbound timestop request";
        assert !item.contains("Yuan.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with")
                : "old server-side timestop broadcast must be removed";
        assert Files.exists(Path.of("src/main/resources/assets/yuan/textures/item/white.png"))
                : "white particle texture missing";
        String particle = Files.readString(Path.of("src/main/java/com/yuan/timestop/render/YuanTimeStopParticle.java"));
        assert particle.contains("extends Particle") && particle.contains("renderSphere")
                : "particle/render classes incomplete";
        assert particle.contains("rotationSpeed")
                : "particle must support per-particle rotation speed";
        assert !particle.contains("public static int MAX_LIFE")
                : "MAX_LIFE must be a per-instance field";
        assert state.contains("YuanTimeStopParticle") && state.contains("YuanTimeStopRender.beam")
                : "YuanTimeStop must spawn the sphere particle";
        assert state.contains("setActiveConfig") && state.contains("getActiveConfig")
                && state.contains("ballColorRgb")
                : "YuanTimeStop must carry per-sword config and map ball color";
        assert state.contains("particleSize") && state.contains("customColor")
                && state.contains("soundEnabled")
                : "YuanTimeStop must honor particle size, custom color and sound toggle";
        assert state.contains("particleCount") && state.contains("particleAlpha")
                && state.contains("particleSpin") && state.contains("soundVolume")
                && state.contains("soundLoop")
                : "YuanTimeStop must honor particle count/alpha/spin and sound volume/loop";
        assert state.contains("freezeLerp") && state.contains("shouldFreezeEntity")
                && state.contains("entity.xOld = entity.getX()")
                : "YuanTimeStop must align frozen entity interpolation state";
        assert state.contains("shouldBlockPlayerInput") && state.contains("shouldCancelCameraTick")
                && state.contains("shouldCancelTickFov") && state.contains("shouldCancelItemInHandTick")
                : "YuanTimeStop must expose frozen-player input/camera gating";
        assert state.contains("playEndSound") && state.contains("scheduleLightRecheck")
                : "YuanTimeStop must play an end sound and recheck light after stop";
        assert state.contains("timer.lastMs")
                : "YuanTimeStop must reset the vanilla timer on stop to avoid tick catch-up";
        assert state.contains("entity instanceof Projectile") && state.contains("entity instanceof ItemEntity")
                : "YuanTimeStop must unconditionally freeze projectiles and dropped items";

        String swordConfig = Files.readString(Path.of("src/main/java/com/yuan/item/YuanGodSwordConfig.java"));
        for (String name : new String[]{
                "grayStrength", "grayAnimate", "ballColorCustom", "customColor",
                "particleSize", "stopDuration", "triggerMode", "soundEnabled", "cooldown",
                "soundVolume", "soundLoop", "particleAlpha", "particleCount", "particleSpin",
                "freezeSelf", "stopRadius", "freezeEntities", "freezeBlocks", "freezeFluids",
                "freezeBossAI", "showMessage", "grayStyle"}) {
            assert swordConfig.contains(name) : name + " must be a per-sword config field";
        }
        String render = Files.readString(Path.of("src/main/java/com/yuan/timestop/render/YuanTimeStopRender.java"));
        assert render.contains("isAlive()")
                : "timestop rendering must skip expired particles";
        assert render.contains("value = Dist.CLIENT")
                : "timestop render subscriber must be restricted to Dist.CLIENT";
        assert render.contains("renderRing") && render.contains("spawnEffect")
                && render.contains("YuanTimeStopEffect")
                : "timestop render must support burst/shockwave/collapse effects";
        assert Files.exists(Path.of("src/main/java/com/yuan/timestop/render/YuanTimeStopEffect.java"))
                : "YuanTimeStopEffect missing";
        assert state.contains("spawnStartEffect") && state.contains("spawnEndEffect")
                && state.contains("markLocalStart") && state.contains("consumeLocalEnd")
                && state.contains("activeConfig.startAnim") && state.contains("activeConfig.endAnim")
                && state.contains("activeConfig.startDuration") && state.contains("activeConfig.endDuration")
                : "YuanTimeStop must wire start/end animation types and real durations";
        String timeStopPacket = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopPacket.java"));
        assert timeStopPacket.contains("consumeLocalEnd()") && timeStopPacket.contains("spawnEndEffect(player)")
                && timeStopPacket.contains("playEndSound(player)")
                : "auto-stop packet must play the end sound and collapse animation on the initiating client";
        assert timeStopPacket.contains("freezeSelf") && timeStopPacket.contains("stopRadius")
                && timeStopPacket.contains("wielderX") && timeStopPacket.contains("setWielderPosition")
                : "timestop packet must carry the full freeze state for LAN clients";
        String soundEvents = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopSounds.java"));
        assert soundEvents.contains("START") && soundEvents.contains("register(\n            \"start\"")
                : "timestop must register a separate start/end sound";
        String soundsJson = Files.readString(Path.of("src/main/resources/assets/yuan/sounds.json"));
        assert soundsJson.contains("\"start\"")
                : "sounds.json must declare the start sound";
        assert Files.exists(Path.of("src/main/resources/assets/yuan/sounds/start.ogg"))
                : "start.ogg missing";
        String godSwordItem = Files.readString(Path.of("src/main/java/com/yuan/item/YuanGodSwordItem.java"));
        assert godSwordItem.contains("spawnStartEffect(player)") && godSwordItem.contains("spawnEndEffect(player)")
                : "god sword trigger must spawn start/end animations";

        String mixinConfig = Files.readString(Path.of("src/main/resources/yuan.mixins.json"));
        for (String name : new String[]{
                "TimestopLevelMixin",
                "TimestopServerLevelMixin", "TimestopMinecraftServerMixin",
                "TimestopBlockStateMixin", "TimestopRailMixin", "TimestopFluidMixin",
                "TimestopPlayerMixin",
                "TimestopLevelExtraMixin", "TimestopServerLevelExtraMixin",
                "TimestopLevelChunkMixin", "TimestopLevelTicksMixin",
                "TimestopServerChunkCacheMixin", "TimestopFunctionManagerMixin",
                "TimestopEndDragonFightMixin", "TimestopRaidsMixin", "TimestopWorldBorderMixin",
                "TimestopExperienceOrbMixin", "TimestopItemEntityMixin",
                "TimestopBlockStateLightMixin", "TimestopInventoryMixin"}) {
            assert mixinConfig.contains(name) : name + " must be registered";
        }

        for (String name : new String[]{
                "TimestopMinecraftMixin", "TimestopGameRendererMixin",
                "TimestopCameraMixin", "TimestopIntegratedServerMixin",
                "TimestopRenderStateShardMixin", "TimestopClientPacketListenerMixin",
                "TimestopEntityRenderMixin", "TimestopParticleRenderMixin",
                "TimestopParticleSpawnMixin",
                "TimestopBlockEntityRenderMixin", "TimestopWeatherRenderMixin",
                "TimestopTextureAtlasMixin",
                "TimestopKeyboardInputMixin", "TimestopInputConstantsMixin",
                "TimestopKeyboardHandlerMixin", "TimestopMouseHandlerMixin",
                "TimestopKeyMappingMixin", "TimestopContainerScreenMixin",
                "TimestopCreativeScreenMixin",
                "TimestopItemInHandMixin", "TimestopGuiMixin",
                "TimestopMusicManagerMixin", "TimestopSoundManagerMixin",
                "TimestopChatListenerMixin", "TimestopTutorialMixin",
                "TimestopAdvancementToastMixin", "TimestopLightTextureMixin",
                "TimestopLevelRendererMixin", "TimestopClientLevelMixin",
                "TimestopIrisTimerMixin", "TimestopIrisFrameCounterMixin",
                "TimestopIrisCapturedStateMixin", "TimestopIrisCommonUniformsMixin"}) {
            assert mixinConfig.contains(name) : name + " must be registered";
        }
        for (String irisMixin : new String[]{"TimestopIrisTimerMixin", "TimestopIrisFrameCounterMixin",
                "TimestopIrisCapturedStateMixin", "TimestopIrisCommonUniformsMixin"}) {
            String irisSrc = Files.readString(Path.of("src/main/java/com/yuan/mixin/" + irisMixin + ".java"));
            assert irisSrc.contains("remap = false") && irisSrc.contains("require = 0")
                    : irisMixin + " must degrade instead of crashing at boot when Iris renames a method";
        }

        String serverMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopMinecraftServerMixin.java"));
        assert serverMixin.contains("method = \"tickServer\"")
                && serverMixin.contains("System.currentTimeMillis()")
                : "tickServer injection missing";
        assert !serverMixin.contains("ci.cancel()") && !serverMixin.contains("cancellable = true")
                : "tickServer must not be cancelled: that also skips saveEverything, the server status and every mod's ServerTickEvent";
        assert !serverMixin.contains("method = \"runServer\"")
                : "runServer injection must be removed";
        assert !serverMixin.contains("getWorldArray")
                : "unused getWorldArray shadow must be removed";
        String serverLevelMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopServerLevelMixin.java"));
        assert !serverLevelMixin.contains("chunkSource.tick")
                : "reentrant chunk ticking must be removed";
        assert !serverLevelMixin.contains("guardEntityTick")
                : "guardEntityTick must not be merged as a ServerLevel override";
        String levelMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopLevelMixin.java"));
        assert levelMixin.contains("@Inject(method = \"guardEntityTick\"")
                && levelMixin.contains("ci.cancel()")
                : "guardEntityTick must be intercepted in Level without overwriting";
        String blockStateMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopBlockStateMixin.java"));
        assert blockStateMixin.contains("YuanTimeStopServerState.isStopped()")
                && blockStateMixin.indexOf("YuanTimeStopServerState.isStopped()")
                < blockStateMixin.indexOf("getCollisionShape")
                : "canBeReplaced must check timestop state before shape computation";
        assert !blockStateMixin.contains("method = \"onPlace\"")
                : "onPlace must stay live so falling blocks scheduled during timestop can resume";

        String minecraftMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopMinecraftMixin.java"));
        assert minecraftMixin.contains("YuanTimeStop.freezeLerp(entity)")
                : "client timestop must finalize frozen entity lerp state";
        assert !minecraftMixin.contains("consumePlayerTicks")
                && !minecraftMixin.contains("timer.msPerTick =")
                && !minecraftMixin.contains("ci.cancel()")
                && !minecraftMixin.contains("screen.tick()")
                && !minecraftMixin.contains("mc.pause = true")
                && !minecraftMixin.contains("mc.setScreen(null)")
                : "client timestop must keep the vanilla Minecraft.tick loop so the exempt player moves smoothly";
        assert !minecraftMixin.contains("@Shadow")
                : "minecraft mixin must not use shadow members";
        assert minecraftMixin.contains("timestopRun") && minecraftMixin.contains("setIsTimeStop(false)")
                : "client timestop must keep the auto-stop check";
        String clientLevelMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopClientLevelMixin.java"));
        assert clientLevelMixin.contains("method = \"tick\"") && clientLevelMixin.contains("ci.cancel()")
                : "client level tick must be cancelled so the world freezes while the player stays exempt";
        String particleRenderMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopParticleRenderMixin.java"));
        assert particleRenderMixin.contains("method = \"tick\"") && particleRenderMixin.contains("ci.cancel()")
                : "particle engine tick must be cancelled while the world is frozen";
        String particleSpawnMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopParticleSpawnMixin.java"));
        assert particleSpawnMixin.contains("method = \"add\"") && particleSpawnMixin.contains("ci.cancel()")
                : "particle spawn must be cancelled while the world is frozen";
        String timeStop = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStop.java"));
        assert timeStop.contains("getSoundManager().pause()") && timeStop.contains("getSoundManager().resume()")
                : "sound engine must pause on start and resume on end";
        String timeStopState = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopServerState.java"));
        assert timeStopState.contains("stopUntilMillis")
                : "auto-stop deadline must use real millis so it advances while tickServer is frozen";
        String requestPacket = Files.readString(Path.of("src/main/java/com/yuan/timestop/YuanTimeStopRequestPacket.java"));
        assert requestPacket.contains("setStopUntilMillis")
                : "request packet must store the real-time auto-stop deadline";

        String gameRendererMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopGameRendererMixin.java"));
        assert gameRendererMixin.contains("@ModifyVariable(method = \"loadEffect\"")
                && !gameRendererMixin.contains("@Inject(method = \"loadEffect\"")
                : "loadEffect must force the_world through @ModifyVariable";
        assert gameRendererMixin.contains("YuanTimeStop.get() && YuanTimeStopConfig.specialShader")
                : "game renderer mixin must gate shader forcing on specialShader";
        assert !gameRendererMixin.contains("@Shadow")
                : "game renderer mixin must not use shadow members";
        assert gameRendererMixin.contains("YuanTimeStop.livePartialTick()")
                && gameRendererMixin.contains("method = \"getFov\"")
                && gameRendererMixin.contains("method = \"bobHurt\"")
                && gameRendererMixin.contains("method = \"bobView\"")
                : "game renderer view/hand partialTick must stay live for the exempt player";
        String cameraMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopCameraMixin.java"));
        assert cameraMixin.contains("YuanTimeStop.livePartialTick()")
                && cameraMixin.contains("!YuanTimeStop.shouldFreezeEntity(entity)")
                : "camera setup partialTick must stay live for the exempt player";
        String entityRenderMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopEntityRenderMixin.java"));
        assert entityRenderMixin.contains("YuanTimeStop.shouldFreezeEntity(entity)")
                && entityRenderMixin.contains("YuanTimeStop.livePartialTick()")
                : "entity render partialTick must freeze only frozen entities and stay live for the exempt player";

        assert !serverMixin.contains("tickChildren")
                : "the freeze must stay in the fine-grained mixins; cancelling tickServer also skips autosave, status and ServerTickEvent";
        assert state.contains("BlockPos.MutableBlockPos cursor")
                && !state.contains("center.offset(x, y, z)")
                : "the light recheck must reuse one cursor instead of allocating a BlockPos per voxel";
        String integratedServerMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopIntegratedServerMixin.java"));
        assert integratedServerMixin.contains("server.paused = false")
                : "paused must be set directly";

        String refmap = Files.readString(Path.of("src/main/resources/yuan.refmap.json"));
        for (String name : new String[]{
                "TimestopLevelMixin", "TimestopServerLevelMixin", "TimestopMinecraftServerMixin",
                "TimestopBlockStateMixin", "TimestopRailMixin", "TimestopFluidMixin",
                "TimestopMinecraftMixin", "TimestopGameRendererMixin",
                "TimestopCameraMixin", "TimestopIntegratedServerMixin",
                "TimestopRenderStateShardMixin", "TimestopPlayerMixin",
                "TimestopClientPacketListenerMixin", "TimestopEntityRenderMixin",
                "TimestopParticleRenderMixin", "TimestopBlockEntityRenderMixin",
                "TimestopParticleSpawnMixin",
                "TimestopWeatherRenderMixin", "TimestopTextureAtlasMixin",
                "TimestopLevelExtraMixin", "TimestopServerLevelExtraMixin",
                "TimestopLevelChunkMixin", "TimestopLevelTicksMixin",
                "TimestopServerChunkCacheMixin", "TimestopFunctionManagerMixin",
                "TimestopEndDragonFightMixin", "TimestopRaidsMixin", "TimestopWorldBorderMixin",
                "TimestopExperienceOrbMixin", "TimestopItemEntityMixin",
                "TimestopBlockStateLightMixin", "TimestopInventoryMixin",
                "TimestopKeyboardInputMixin", "TimestopInputConstantsMixin",
                "TimestopKeyboardHandlerMixin", "TimestopMouseHandlerMixin",
                "TimestopKeyMappingMixin", "TimestopContainerScreenMixin",
                "TimestopCreativeScreenMixin",
                "TimestopItemInHandMixin", "TimestopGuiMixin",
                "TimestopMusicManagerMixin", "TimestopSoundManagerMixin",
                "TimestopChatListenerMixin", "TimestopTutorialMixin",
                "TimestopAdvancementToastMixin", "TimestopLightTextureMixin",
                "TimestopLevelRendererMixin", "TimestopClientLevelMixin"}) {
            assert refmap.contains(name) : "refmap missing " + name;
        }
        assert refmap.contains("ItemRendererMixin") : "refmap must keep cosmic item renderer mixin";
        assert refmap.contains("cycleAnimationFrames")
                : "refmap must map TextureAtlas.cycleAnimationFrames to SRG";
        assert refmap.contains("m_104726_") && refmap.contains("m_107388_")
                : "refmap must map ClientLevel.tick and ParticleEngine.tick to SRG";
        assert refmap.contains("tickFov") && refmap.contains("slotClicked")
                && refmap.contains("doAnimateTick") && refmap.contains("getLightEmission")
                && refmap.contains("tickPassenger") && refmap.contains("isTicking")
                && refmap.contains("playerTouch")
                : "refmap must cover the new input/world-system freeze mixins";

        String composeScreen = Files.readString(Path.of("src/main/kotlin/com/yuan/client/gui/YuanComposeTestScreen.kt"));
        assert composeScreen.contains("navSections")
                : "compose config screen must group settings by sidebar tabs";
        assert !composeScreen.contains("暂未开放")
                : "compose config screen must not keep placeholder tabs";
        assert !composeScreen.contains("LegacyHeader") && !composeScreen.contains("LegacyCenter")
                && !composeScreen.contains("LegacyFooter") && !composeScreen.contains("LegacyPreview")
                && !composeScreen.contains("LegacySidebar")
                : "legacy compose screen code must be removed";

        for (String path : new String[]{
                "src/main/resources/assets/minecraft/shaders/post/the_world.json",
                "src/main/resources/assets/minecraft/shaders/program/motion_blur.fsh",
                "src/main/resources/assets/minecraft/shaders/program/motion_blur.json",
                "src/main/resources/assets/minecraft/shaders/program/rewind.fsh",
                "src/main/resources/assets/minecraft/shaders/program/rewind.json",
                "src/main/resources/assets/minecraft/shaders/program/rewind.vsh"}) {
            assert Files.exists(Path.of(path)) : path + " missing";
        }
        for (String path : new String[]{
                "src/main/resources/assets/minecraft/shaders/post/yuan_world_style.json",
                "src/main/resources/assets/minecraft/shaders/program/yuan_style.json",
                "src/main/resources/assets/minecraft/shaders/program/yuan_style.fsh"}) {
            assert Files.exists(Path.of(path)) : path + " missing";
        }
        String styleJson = Files.readString(Path.of("src/main/resources/assets/minecraft/shaders/program/yuan_style.json"));
        assert styleJson.contains("\"vertex\": \"blit\"")
                : "yuan_style shader must use the normal blit vertex mapping";
        String theWorldPost = Files.readString(Path.of("src/main/resources/assets/minecraft/shaders/post/the_world.json"));
        String stylePost = Files.readString(Path.of("src/main/resources/assets/minecraft/shaders/post/yuan_world_style.json"));
        assert !theWorldPost.contains("rewind") && !stylePost.contains("rewind")
                : "timestop post chains must not contain rewind wave/distortion passes";

        String keyBindings = Files.readString(Path.of("src/main/java/com/yuan/client/YuanKeyBindings.java"));
        assert keyBindings.contains("TRIGGER_TIMESTOP")
                && keyBindings.contains("godSwordRightHeld") && keyBindings.contains("YuanGodSwordItem.trigger")
                : "custom timestop trigger key binding missing";

        String packetMixin = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopClientPacketListenerMixin.java"));
        assert packetMixin.contains("handleRotateMob") && packetMixin.contains("handleMoveEntity")
                && packetMixin.contains("handleSetEntityData")
                : "client packet mixin must cover movement, head rotation and entity data packets";
        String timestopMinecraftMixinText = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopMinecraftMixin.java"));
        assert timestopMinecraftMixinText.contains("entitiesForRendering()")
                : "client timestop must freeze lerp state for all renderable entities";
        String levelMixinText = Files.readString(Path.of("src/main/java/com/yuan/mixin/TimestopLevelMixin.java"));
        assert !levelMixinText.contains("tickCount <= 0")
                : "server timestop must freeze newly spawned entities too";
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
