package net.rasanovum.viaromana.command;

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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;


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
                                .executes(ViaRomanaCommands::saveMaps))));
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
        
        source.sendSuccess(() -> Component.literal("Cleared " + nodeCount + " nodes"), true);
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

        source.sendSuccess(() -> Component.literal("Cleared all Via Romana caches"), true);
        return 1;
    }

    /**
     * Converts paths from the legacy (level-based) storage system to the new (dimension-based) system.
     * This imports all paths from the old save data into the current dimension.
     */
    private static int convertLegacyPaths(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel currentLevel = source.getLevel();

        PathGraph legacyGraph = IPathStorage.get(currentLevel).graph();
        int legacyNodeCount = legacyGraph.size();

        if (legacyNodeCount == 0) {
            source.sendFailure(Component.literal("No legacy paths found to convert"));
            return 0;
        }

        // Get current dimension's path graph and check if empty
        PathGraph currentGraph = PathDataManager.getOrCreatePathGraph(currentLevel);
        if (currentGraph.size() > 0) {
            source.sendFailure(Component.literal(
                "Current dimension already has " + currentGraph.size() + " nodes. Use /viaromana nodes clear first."
            ));
            return 0;
        }

        // Copy legacy data to current dimension
        currentGraph.deserialize(legacyGraph.serialize(new CompoundTag()));
        PathDataManager.markDirty(currentLevel);
        PathSyncUtils.syncPathGraphToAllPlayers(currentLevel);

        source.sendSuccess(() -> Component.literal(
            "Converted " + legacyNodeCount + " nodes to " + currentLevel.dimension().location()
        ), true);
        
        return legacyNodeCount;
    }

    /**
     * Clears map caches (server, client) and chunk image data
     */
    private static int clearMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("Clearing level chunk data..."), false);
        LevelDataManager.clearAllPixelBytes(source.getLevel());
        LevelDataManager.clearAllCornerBytes(source.getLevel());

        ServerMapCache.clear();
        ServerMapCache.deleteAllMapsFromDisk();
        if (source.getLevel().isClientSide()) MapRenderer.clearCache();
        
        source.sendSuccess(() -> Component.literal("Cleared Via Romana maps and chunk images"), true);
        return 1;
    }

    /**
     * Clears map caches (server, client) and chunk image data
     */
    private static int clearBiomePixels(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("Clearing level biome pixel data..."), false);
        LevelDataManager.clearAllCornerBytes(source.getLevel());

        ServerMapCache.clear();
        ServerMapCache.deleteAllMapsFromDisk();
        if (source.getLevel().isClientSide()) MapRenderer.clearCache();

        source.sendSuccess(() -> Component.literal("Cleared Via Romana maps and biome images"), true);
        return 1;
    }

    /**
     * Clears map caches (server, client) and chunk image data
     */
    private static int clearChunkPixels(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.literal("Clearing level chunk pixel data..."), false);
        LevelDataManager.clearAllPixelBytes(source.getLevel());

        ServerMapCache.clear();
        ServerMapCache.deleteAllMapsFromDisk();
        if (source.getLevel().isClientSide()) MapRenderer.clearCache();

        source.sendSuccess(() -> Component.literal("Cleared Via Romana maps and biome images"), true);
        return 1;
    }

    /**
     * Immediately regenerates chunk pixel data and processes dirty networks
     */
    private static int regenerateMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        // Regenerate chunk image data
        source.sendSuccess(() -> Component.literal("Regenerating chunk image data..."), false);
        ServerMapCache.regenerateAllChunkPixelData();
        
        // Process dirty networks to regenerate maps
        ServerMapCache.processAllDirtyNetworks();
        
        source.sendSuccess(() -> Component.literal("Regenerated Via Romana maps"), true);
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

        source.sendSuccess(() -> Component.literal("Cleared Via Romana maps"), true);
        return 1;
    }

    /**
     * Saves all cached maps to disk
     */
    private static int saveMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerMapCache.saveAllToDisk(true);
        source.sendSuccess(() -> Component.literal("Saved Via Romana maps to disk"), true);
        return 1;
    }
}
