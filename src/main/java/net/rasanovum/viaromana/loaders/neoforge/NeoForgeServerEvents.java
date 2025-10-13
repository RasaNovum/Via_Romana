package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.rasanovum.viaromana.ViaRomana;

@EventBusSubscriber(modid = ViaRomana.MODID)
public final class NeoForgeServerEvents {
    private static MinecraftServer serverInstance;

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
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        ViaRomana.registerCommands(dispatcher);
    }
}
*///?}