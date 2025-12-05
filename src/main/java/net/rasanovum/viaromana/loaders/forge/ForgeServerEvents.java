package net.rasanovum.viaromana.loaders.forge;

//? if forge {
/*import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;

@Mod.EventBusSubscriber(modid = ViaRomana.MODID)
public final class ForgeServerEvents {
    private static MinecraftServer serverInstance;

    @SubscribeEvent
    public static void onPlayerJoin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ViaRomana.onJoin(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(final LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Level level = event.level;
            if (level instanceof ServerLevel serverLevel) {
                ViaRomana.onServerTick(serverLevel);
            }
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

