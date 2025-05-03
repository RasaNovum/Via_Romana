package net.rasanovum.viaromana.forge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.forge.network.ViaRomanaModPacketHandler;
import net.rasanovum.viaromana.forge.init.ViaRomanaModMobEffects;
import net.rasanovum.viaromana.forge.init.ViaRomanaModConfigs;
import net.rasanovum.viaromana.forge.capabilities.CustomDataCapability;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;

@Mod(ViaRomanaMod.MODID)
public class ViaRomanaMod {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "via_romana";

    @SuppressWarnings("removal")
    public ViaRomanaMod() {
        LOGGER.info("Initializing ViaRomanaMod");
        
        ViaRomanaModMobEffects.register(NeoForge.EVENT_BUS);
        
        NeoForge.EVENT_BUS.addListener(this::setup);

        ViaRomanaModConfigs.register();

        NeoForge.EVENT_BUS.addListener(BlockEntity.class, CustomDataCapability::onAttachCapabilitiesToBlockEntity);

        registerServerLifecycleEvents();
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ViaRomanaModPacketHandler.initialize();
        });
    }

    private void registerServerLifecycleEvents() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ViaRomanaModVariables.playerLoggedIn(player);
            }
        });

        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                ViaRomanaModVariables.playerLoggedOut(player);
            }
        });

        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer newPlayer) {
                boolean keepInventory = event.isEndConquered();
                ViaRomanaModVariables.playerRespawned(null, newPlayer, keepInventory);
            }
        });
    }
}
