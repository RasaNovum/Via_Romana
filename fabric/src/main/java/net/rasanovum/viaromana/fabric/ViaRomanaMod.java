package net.rasanovum.viaromana.fabric;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.fabric.network.ViaRomanaModPacketHandler;
import net.rasanovum.viaromana.fabric.init.ViaRomanaModTriggers;
import net.rasanovum.viaromana.fabric.init.ViaRomanaModMobEffects;
import net.rasanovum.viaromana.fabric.init.ViaRomanaModConfigs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class ViaRomanaMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final String MODID = "via_romana";

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing ViaRomanaMod");

		ViaRomanaModMobEffects.load();
		ViaRomanaModTriggers.load();
		ViaRomanaModConfigs.register();

		ViaRomanaModPacketHandler.initialize();

		registerServerLifecycleEvents();
	}

	// Register server lifecycle events to handle player login, logout, and respawn
	private void registerServerLifecycleEvents() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ViaRomanaModVariables.playerLoggedIn(handler.player);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ViaRomanaModVariables.playerLoggedOut(handler.player);
		});

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			boolean keepInventory = oldPlayer.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
			ViaRomanaModVariables.playerRespawned(oldPlayer, newPlayer, keepInventory || !alive);
		});
	}
}
