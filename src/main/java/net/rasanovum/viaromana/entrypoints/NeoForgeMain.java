package net.rasanovum.viaromana.entrypoints;

//? if neoforge {
/*import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rasanovum.viaromana.ViaRomana;

@Mod(ViaRomana.MODID)
@EventBusSubscriber
public class NeoForgeMain {

    private static MinecraftServer serverInstance;

    public NeoForgeMain(IEventBus modEventBus) {
        ViaRomana.initialize();
    }

    @SubscribeEvent
    public static void onPlayerJoin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ViaRomana.onJoin(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(final LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level instanceof ServerLevel serverLevel) {
            ViaRomana.onServerTick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onServerStarting(final ServerStartingEvent event) {
        serverInstance = event.getServer();
        ViaRomana.onServerStart(serverInstance);
    }

    @SubscribeEvent
    public static void onServerStopping(final ServerStoppingEvent event) {
        ViaRomana.onServerStop();
        serverInstance = null;
    }

    @SubscribeEvent
    public static void onDataPackReload(final TagsUpdatedEvent event) {
        if (serverInstance != null) {
            ViaRomana.onDataPackReload(serverInstance);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel newLevel = player.serverLevel();
            ViaRomana.onDimensionChange(newLevel, player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        ViaRomana.registerCommands(event.getDispatcher());
    }

    @EventBusSubscriber(modid = ViaRomana.MODID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            ViaRomana.LOGGER.info("Initializing {} Client", ViaRomana.MODID);
        }
    }
}
*///?}