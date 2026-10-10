package com.yuan;

import com.yuan.client.particle.YuanParticleTypes;
import com.yuan.domain.YuanDomainRequestPacket;
import com.yuan.domain.YuanDomainStatePacket;
import com.yuan.network.ConfigSyncPacket;
import com.yuan.network.YuanGodSwordConfigPacket;
import com.yuan.network.ModeSwitchMessage;
import com.yuan.network.TimeStopPacket;
import com.yuan.network.TimeStopStatePacket;
import com.yuan.network.BanListRequestPacket;
import com.yuan.network.BanListStatePacket;
import com.yuan.network.BanRemovePacket;
import com.yuan.network.ConfigSyncAckPacket;
import com.yuan.registry.YuanItems;
import com.yuan.space_slash.YuanSpaceSlashEntity;
import com.yuan.space_slash.YuanSpaceSlashPacket;
import com.yuan.timestop.YuanTimeStopConfig;
import com.yuan.timestop.YuanTimeStopSounds;
import com.yuan.timestop.YuanTimeStopPacket;
import com.yuan.timestop.YuanTimeStopRequestPacket;
import com.yuan.timerewind.YuanTimeRewindCancelPacket;
import com.yuan.timerewind.YuanTimeRewindConfig;
import com.yuan.timerewind.YuanTimeRewindEndPacket;
import com.yuan.timerewind.YuanTimeRewindRequestPacket;
import com.yuan.timerewind.YuanTimeRewindStartPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

@Mod("yuan")
public class Yuan {
    public static final String MOD_ID = "yuan";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final RegistryObject<EntityType<YuanSpaceSlashEntity>> SPACE_SLASH_ENTITY =
            ENTITY_TYPES.register("space_slash",
                    () -> EntityType.Builder.<YuanSpaceSlashEntity>of(YuanSpaceSlashEntity::new, MobCategory.MISC)
                            .sized(0.1f, 0.1f).noSummon().noSave().fireImmune()
                            .build("yuan:space_slash"));
    
    public static final RegistryObject<Item> YUAN_SWORD = ITEMS.register("yuan_sword", () -> YuanItems.YUAN_SWORD);
    public static final RegistryObject<Item> YUAN_GOD_SWORD = ITEMS.register("yuan_god_sword", () -> YuanItems.YUAN_GOD_SWORD);
    public static final RegistryObject<Item> YUAN_ORIGIN_BLADE = ITEMS.register("yuan_origin_blade", () -> YuanItems.YUAN_ORIGIN_BLADE);
    public static final RegistryObject<Item> YUAN_AXE = ITEMS.register("yuan_axe", () -> YuanItems.YUAN_AXE);
    public static final RegistryObject<Item> YUAN_PICKAXE = ITEMS.register("yuan_pickaxe", () -> YuanItems.YUAN_PICKAXE);
    public static final RegistryObject<Item> YUAN_BOW = ITEMS.register("yuan_bow", () -> YuanItems.YUAN_BOW);
    public static final RegistryObject<Item> YUAN_HELMET = ITEMS.register("yuan_helmet", () -> YuanItems.YUAN_HELMET);
    public static final RegistryObject<Item> YUAN_CHESTPLATE = ITEMS.register("yuan_chestplate", () -> YuanItems.YUAN_CHESTPLATE);
    public static final RegistryObject<Item> YUAN_LEGGINGS = ITEMS.register("yuan_leggings", () -> YuanItems.YUAN_LEGGINGS);
    public static final RegistryObject<Item> YUAN_BOOTS = ITEMS.register("yuan_boots", () -> YuanItems.YUAN_BOOTS);
    public static final RegistryObject<CreativeModeTab> YUAN_TAB = CREATIVE_TABS.register("yuan", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.yuan"))
            .icon(() -> YUAN_SWORD.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(YUAN_SWORD.get());
                output.accept(YUAN_GOD_SWORD.get());
                output.accept(YUAN_ORIGIN_BLADE.get());
                output.accept(YUAN_AXE.get());
                output.accept(YUAN_PICKAXE.get());
                output.accept(YUAN_BOW.get());
                output.accept(YUAN_HELMET.get());
                output.accept(YUAN_CHESTPLATE.get());
                output.accept(YUAN_LEGGINGS.get());
                output.accept(YUAN_BOOTS.get());
            })
            .build());

    private static final String NET_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> NET_VERSION,
            NET_VERSION::equals,
            NET_VERSION::equals);

    public Yuan() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        CREATIVE_TABS.register(bus);
        ENTITY_TYPES.register(bus);
        YuanTimeStopSounds.SOUNDS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, YuanTimeStopConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, YuanTimeStopConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, YuanTimeRewindConfig.SPEC,
                "yuan-rewind-common.toml");
        YuanParticleTypes.PARTICLE_TYPES.register(bus);
        bus.addListener(this::onBuildCreativeModeTabContents);

        CHANNEL.registerMessage(0, ModeSwitchMessage.class,
                ModeSwitchMessage::encode,
                ModeSwitchMessage::new,
                ModeSwitchMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(1, ConfigSyncPacket.class,
                ConfigSyncPacket::encode,
                ConfigSyncPacket::new,
                ConfigSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(2, TimeStopPacket.class,
                TimeStopPacket::encode,
                TimeStopPacket::new,
                TimeStopPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(3, TimeStopStatePacket.class,
                TimeStopStatePacket::encode,
                TimeStopStatePacket::new,
                TimeStopStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(4, BanListRequestPacket.class,
                BanListRequestPacket::encode, BanListRequestPacket::new, BanListRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5, BanListStatePacket.class,
                BanListStatePacket::encode, BanListStatePacket::new, BanListStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6, BanRemovePacket.class,
                BanRemovePacket::encode, BanRemovePacket::new, BanRemovePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(7, ConfigSyncAckPacket.class,
                ConfigSyncAckPacket::encode, ConfigSyncAckPacket::new, ConfigSyncAckPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(8, YuanTimeStopPacket.class,
                YuanTimeStopPacket::encode,
                YuanTimeStopPacket::decode,
                YuanTimeStopPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(9, YuanTimeStopRequestPacket.class,
                YuanTimeStopRequestPacket::encode,
                YuanTimeStopRequestPacket::decode,
                YuanTimeStopRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(10, YuanSpaceSlashPacket.class,
                YuanSpaceSlashPacket::encode,
                YuanSpaceSlashPacket::decode,
                YuanSpaceSlashPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(11, YuanGodSwordConfigPacket.class,
                YuanGodSwordConfigPacket::encode,
                YuanGodSwordConfigPacket::decode,
                YuanGodSwordConfigPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(12, YuanTimeRewindRequestPacket.class,
                YuanTimeRewindRequestPacket::encode,
                YuanTimeRewindRequestPacket::decode,
                YuanTimeRewindRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(13, YuanTimeRewindStartPacket.class,
                YuanTimeRewindStartPacket::encode,
                YuanTimeRewindStartPacket::decode,
                YuanTimeRewindStartPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(14, YuanTimeRewindEndPacket.class,
                YuanTimeRewindEndPacket::encode,
                YuanTimeRewindEndPacket::decode,
                YuanTimeRewindEndPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        CHANNEL.registerMessage(15, YuanTimeRewindCancelPacket.class,
                YuanTimeRewindCancelPacket::encode,
                YuanTimeRewindCancelPacket::decode,
                YuanTimeRewindCancelPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(16, YuanDomainRequestPacket.class,
                YuanDomainRequestPacket::encode,
                YuanDomainRequestPacket::decode,
                YuanDomainRequestPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(17, YuanDomainStatePacket.class,
                YuanDomainStatePacket::encode,
                YuanDomainStatePacket::decode,
                YuanDomainStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(YUAN_SWORD.get());
            event.accept(YUAN_GOD_SWORD.get());
            event.accept(YUAN_ORIGIN_BLADE.get());
            event.accept(YUAN_AXE.get());
            event.accept(YUAN_BOW.get());
            event.accept(YUAN_HELMET.get());
            event.accept(YUAN_CHESTPLATE.get());
            event.accept(YUAN_LEGGINGS.get());
            event.accept(YUAN_BOOTS.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(YUAN_AXE.get());
            event.accept(YUAN_PICKAXE.get());
        }
    }
}
