package net.rasanovum.viaromana.loaders.neoforge;

//? if neoforge {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.StatInit;

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
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
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
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            boolean result = ViaRomana.onBlockBreak(event.getLevel(), event.getPos(), serverPlayer);

            if (!result) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        ViaRomana.registerCommands(dispatcher);
    }
}
*///?}