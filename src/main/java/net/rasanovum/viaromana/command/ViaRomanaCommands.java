package net.rasanovum.viaromana.command;

import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.storage.IPathStorage;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.client.gui.MapRenderer;
import net.rasanovum.viaromana.map.ServerMapCache;

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
                .then(Commands.literal("maps")
                        .then(Commands.literal("clear")
                                .executes(ViaRomanaCommands::clearMaps))
                        .then(Commands.literal("regenerate")
                                .executes(ViaRomanaCommands::regenerateMaps))
                        .then(Commands.literal("save")
                                .executes(ViaRomanaCommands::saveMaps))));
    }

    /**
    * Removes all nodes from Client & Server Path Graphs
     */
    private static int clearAllNodes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        for (Player player : source.getLevel().players()) {
            VariableAccess.playerVariables.setChartingPath(player, false);
            VariableAccess.playerVariables.syncAndSave(player);
        }

        IPathStorage storage = IPathStorage.get(source.getLevel());
        PathGraph graph = storage.graph();
        
        int nodeCount = graph.size();
        
        graph.removeAllNodes();

        ClientPathData.getInstance().clearData();
        PathSyncUtils.syncPathGraphToAllPlayers(source.getLevel());

        storage.setDirty();
        
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

        // VariableAccess.playerVariables.setReceivedTutorial(source.getPlayerOrException(), false);

        source.sendSuccess(() -> Component.literal("Cleared all Via Romana caches"), true);
        return 1;
    }

    /**
     * Clears map caches (server, client, and chunk PNG data)
     */
    private static int clearMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        // Clear chunk PNG data
        source.sendSuccess(() -> Component.literal("Clearing chunk PNG data..."), false);
        ServerMapCache.clearAllChunkPngData();
        
        // Clear map caches and disk data
        ServerMapCache.clear();
        ServerMapCache.deleteAllMapsFromDisk();
        if (source.getLevel().isClientSide()) MapRenderer.clearCache();
        
        source.sendSuccess(() -> Component.literal("Cleared Via Romana maps (cache, disk, and chunk PNG data)"), true);
        return 1;
    }

    /**
     * Immediately regenerates chunk PNG data and processes dirty networks
     */
    private static int regenerateMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        // Regenerate chunk PNG data
        source.sendSuccess(() -> Component.literal("Regenerating chunk PNG data..."), false);
        ServerMapCache.regenerateAllChunkPngData();
        
        // Process dirty networks to regenerate maps
        ServerMapCache.processAllDirtyNetworks();
        
        source.sendSuccess(() -> Component.literal("Regenerated Via Romana maps (chunk PNG data and map images)"), true);
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
