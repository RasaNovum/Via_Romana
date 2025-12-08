package net.rasanovum.viaromana.command;

import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.storage.level.LevelDataManager;
import net.rasanovum.viaromana.storage.path.PathDataManager;
import net.rasanovum.viaromana.storage.path.legacy.IPathStorage;
import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.client.gui.MapRenderer;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.viaromana.init.StatInit;

import java.nio.file.Files;
import java.nio.file.Path;


public class ViaRomanaCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("viaromana")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("nodes")
                        .then(Commands.literal("clear")
                                .executes(ViaRomanaCommands::clearAllNodes)))
                .then(Commands.literal("client")
                        .then(Commands.literal("clear")
                                .executes(ViaRomanaCommands::clearCache)))
                .then(Commands.literal("convert")
                        .then(Commands.literal("legacyPaths")
                                .executes(ViaRomanaCommands::convertLegacyPaths)))
                .then(Commands.literal("maps")
                        .then(Commands.literal("clear")
                                .then(Commands.literal("all")
                                    .executes(ViaRomanaCommands::clearMaps))
                                .then(Commands.literal("biomePixels")
                                        .executes(ViaRomanaCommands::clearBiomePixels))
                                .then(Commands.literal("chunkPixels")
                                        .executes(ViaRomanaCommands::clearChunkPixels)))
                        .then(Commands.literal("regenerate")
                                .executes(ViaRomanaCommands::regenerateMaps))
                        .then(Commands.literal("delete")
                                .executes(ViaRomanaCommands::deleteMaps))
                        .then(Commands.literal("save")
                                .executes(ViaRomanaCommands::saveMaps)))
                .then(Commands.literal("sync")
                        .executes(ViaRomanaCommands::syncDimension))
                .then(Commands.literal("stats")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("distance_charted")
                                                .executes(ctx -> getStat(ctx, "distance_charted")))
                                        .then(Commands.literal("distance_walked")
                                                .executes(ctx -> getStat(ctx, "distance_walked")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("distance_charted")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> setStat(ctx, "distance_charted"))))
                                        .then(Commands.literal("distance_walked")
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> setStat(ctx, "distance_walked"))))))));
    }

    /**
    * Removes all nodes from Client & Server Path Graphs
     */
    private static int clearAllNodes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        for (Player player : source.getLevel().players()) {
            PlayerData.setChartingPath(player, false);
        }

        PathGraph graph = PathGraph.getInstance(source.getLevel());
        
        int nodeCount = graph.size();
        
        graph.removeAllNodes();

        ClientPathData.getInstance().clearData();
        PathSyncUtils.syncPathGraphToAllPlayers(source.getLevel());

        PathDataManager.markDirty(source.getLevel());
        
        source.sendSuccess(() -> Component.translatable("command.via_romana.nodes_cleared"), true);
        return nodeCount;
    }

    /**
    * Clears Client Path Graph data
     */
    private static int clearCache(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (source.getLevel().isClientSide()) ClientPathData.getInstance().clearData();
        
        PathSyncUtils.syncPathGraphToPlayer(source.getPlayerOrException());

        // PlayerData.setReceivedTutorial(source.getPlayerOrException(), false);

        source.sendSuccess(() -> Component.translatable("command.via_romana.cache_cleared"), true);
        return 1;
    }

    /**
     * Converts paths from the legacy (level-based) storage system to the new (dimension-based) system.
     * This imports all paths from the old save data into the current dimension.
     * All converted nodes will have their clearance value set to 0.
     */
    private static int convertLegacyPaths(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel currentLevel = source.getLevel();

        PathGraph legacyGraph = IPathStorage.get(currentLevel).graph();
        int legacyNodeCount = legacyGraph.size();

        if (legacyNodeCount == 0) {
            source.sendFailure(Component.translatable("command.via_romana.no_legacy_paths"));
            return 0;
        }

        // Get current dimension's path graph and check if empty
        PathGraph currentGraph = PathDataManager.getOrCreatePathGraph(currentLevel);
        if (currentGraph.size() > 0) {
            source.sendFailure(Component.translatable("command.via_romana.dimension_has_nodes"));
            return 0;
        }

        CompoundTag serializedData = legacyGraph.serialize(new CompoundTag());

        // Set all node clearance values to 0
        if (serializedData.contains("nodes")) {
            net.minecraft.nbt.ListTag nodeList = serializedData.getList("nodes", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < nodeList.size(); i++) {
                CompoundTag nodeTag = nodeList.getCompound(i);
                nodeTag.putFloat("clearance", 0.0f);
            }
        }

        currentGraph.deserialize(serializedData);
        PathDataManager.markDirty(currentLevel);
        PathSyncUtils.syncPathGraphToAllPlayers(currentLevel);

        source.sendSuccess(() -> Component.translatable("command.via_romana.legacy_converted"), true);

        return legacyNodeCount;
    }

    /**
     * Clears map caches (server, client) and chunk image data
     */
    private static int clearMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.translatable("command.via_romana.clearing_chunk_data"), false);
        LevelDataManager.clearAllPixelBytes(source.getLevel());
        LevelDataManager.clearAllCornerBytes(source.getLevel());

        ServerMapCache.clear();
        ServerMapCache.deleteAllMapsFromDisk();
        if (source.getLevel().isClientSide()) MapRenderer.clearCache();
        
        source.sendSuccess(() -> Component.translatable("command.via_romana.maps_and_chunks_cleared"), true);
        return 1;
    }

    /**
     * Clears map caches (server, client) and chunk image data
     */
    private static int clearBiomePixels(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.translatable("command.via_romana.clearing_biome_data"), false);
        LevelDataManager.clearAllCornerBytes(source.getLevel());

        ServerMapCache.clear();
        ServerMapCache.deleteAllMapsFromDisk();
        if (source.getLevel().isClientSide()) MapRenderer.clearCache();

        source.sendSuccess(() -> Component.translatable("command.via_romana.maps_and_biomes_cleared"), true);
        return 1;
    }

    /**
     * Clears map caches (server, client) and chunk image data
     */
    private static int clearChunkPixels(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.translatable("command.via_romana.clearing_chunk_pixels"), false);
        LevelDataManager.clearAllPixelBytes(source.getLevel());

        ServerMapCache.clear();
        ServerMapCache.deleteAllMapsFromDisk();
        if (source.getLevel().isClientSide()) MapRenderer.clearCache();

        source.sendSuccess(() -> Component.translatable("command.via_romana.maps_and_chunks_cleared"), true);
        return 1;
    }

    /**
     * Immediately regenerates chunk pixel data and processes dirty networks
     */
    private static int regenerateMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        // Regenerate chunk image data
        source.sendSuccess(() -> Component.translatable("command.via_romana.regenerating_chunks"), false);
        ServerMapCache.regenerateAllChunkPixelData();
        
        // Process dirty networks to regenerate maps
        ServerMapCache.processAllDirtyNetworks(true);
        
        source.sendSuccess(() -> Component.translatable("command.via_romana.maps_regenerated"), true);
        return 1;
    }

    /**
     * Clears map caches (server, client) excluding chunk image data
     */
    private static int deleteMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        // Clear map caches and disk data
        ServerMapCache.clear();
        ServerMapCache.deleteAllMapsFromDisk();
        if (source.getLevel().isClientSide()) MapRenderer.clearCache();

        source.sendSuccess(() -> Component.translatable("command.via_romana.maps_cleared"), true);
        return 1;
    }

    /**
     * Saves all cached maps to disk
     */
    private static int saveMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerMapCache.saveAllToDisk(true);
        source.sendSuccess(() -> Component.translatable("command.via_romana.maps_saved"), true);
        return 1;
    }

    /**
     * Forces a PathGraph sync for the current dimension.
     */
    private static int syncDimension(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        PathSyncUtils.syncPathGraphToAllPlayers(level);
        source.sendSuccess(() -> Component.translatable("command.via_romana.sync_triggered"), true);
        return 1;
    }

    /**
     * Gets a stat value for a player
     */
    private static int getStat(CommandContext<CommandSourceStack> context, String statType) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        
        Stat<ResourceLocation> stat = getStatFromType(statType);
        int value = player.getStats().getValue(Stats.CUSTOM.get(stat.getValue()));
        
        String formattedDistance = formatDistance(value);
        source.sendSuccess(() -> Component.literal(String.format("%s's %s: %s", 
            player.getName().getString(), 
            statType.replace("_", " "),
            formattedDistance)), false);
        
        return value;
    }

    /**
     * Sets a stat value for a player
     */
    private static int setStat(CommandContext<CommandSourceStack> context, String statType) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int value = IntegerArgumentType.getInteger(context, "value");
        
        Stat<ResourceLocation> stat = getStatFromType(statType);

        int currentValue = player.getStats().getValue(Stats.CUSTOM.get(stat.getValue()));
        player.resetStat(Stats.CUSTOM.get(stat.getValue()));
        player.awardStat(Stats.CUSTOM.get(stat.getValue()), value);
        
        String formattedDistance = formatDistance(value);
        source.sendSuccess(() -> Component.literal(String.format("Set %s's %s to %s", 
            player.getName().getString(),
            statType.replace("_", " "),
            formattedDistance)), true);
        
        return value;
    }

    /**
     * Helper method to get the stat from the type string
     */
    private static Stat<ResourceLocation> getStatFromType(String statType) {
        return switch (statType) {
            case "distance_charted" -> Stats.CUSTOM.get(StatInit.DISTANCE_CHARTED);
            case "distance_walked" -> Stats.CUSTOM.get(StatInit.DISTANCE_WALKED);
            default -> throw new IllegalArgumentException("Unknown stat type: " + statType);
        };
    }

    /**
     * Formats distance value (in cm) to human-readable string with m/km units
     */
    private static String formatDistance(int centimeters) {
        double meters = centimeters / 100.0;
        
        if (meters > 1000.0) {
            double kilometers = meters / 1000.0;
            return String.format("%.2f km", kilometers);
        } else {
            return String.format("%.2f m", meters);
        }
    }
}
