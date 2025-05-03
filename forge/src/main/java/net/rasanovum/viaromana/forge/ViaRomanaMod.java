package net.rasanovum.viaromana.forge;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.forge.network.ViaRomanaModPacketHandler;
import net.rasanovum.viaromana.forge.init.ViaRomanaModMobEffects;
import net.rasanovum.viaromana.forge.init.ViaRomanaModConfigs;
import net.rasanovum.viaromana.forge.capabilities.CustomDataCapability;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ViaRomanaMod.MODID)
public class ViaRomanaMod {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "via_romana";

    @SuppressWarnings("removal")
    public ViaRomanaMod() {
        LOGGER.info("Initializing ViaRomanaMod");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        ViaRomanaModMobEffects.register(modEventBus);
        
        modEventBus.addListener(this::setup);

        ViaRomanaModConfigs.register();

        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, CustomDataCapability::onAttachCapabilitiesToBlockEntity);

        registerServerLifecycleEvents();
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ViaRomanaModPacketHandler.initialize();
        });
    }

    private void registerServerLifecycleEvents() {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ViaRomanaModVariables.playerLoggedIn(player);
            }
        });

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ViaRomanaModVariables.playerLoggedOut(player);
            }
        });

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer newPlayer) {
                boolean keepInventory = event.isEndConquered();
                ViaRomanaModVariables.playerRespawned(null, newPlayer, keepInventory);
            }
        });
    }
}
