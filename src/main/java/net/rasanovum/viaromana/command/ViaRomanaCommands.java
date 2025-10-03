package net.rasanovum.viaromana.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.rasanovum.viaromana.map.ChunkPngUtil;
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

import java.util.Optional;


public class ViaRomanaCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("viaromana")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("nodes")
                        .then(Commands.literal("clear")
                                .executes(ViaRomanaCommands::clearAllNodes)))
                .then(Commands.literal("cache")
                        .then(Commands.literal("clear")
                                .executes(ViaRomanaCommands::clearCache)))
                .then(Commands.literal("maps")
                        .then(Commands.literal("clear")
                                .executes(ViaRomanaCommands::clearMaps))
                        .then(Commands.literal("regenerate")
                                .executes(ViaRomanaCommands::regenerateMaps))
                        .then(Commands.literal("testpng")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx ->
                                                        testPngAttachment(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                IntegerArgumentType.getInteger(ctx, "z"))))))
                        .then(Commands.literal("save")
                                .executes(ViaRomanaCommands::saveMaps))));
    }
    
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

    private static int clearCache(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (source.getLevel().isClientSide()) {
            ClientPathData.getInstance().clearData();
            MapRenderer.clearCache();
        }
        
        PathSyncUtils.syncPathGraphToPlayer(source.getPlayerOrException());
        ServerMapCache.clear();

        // VariableAccess.playerVariables.setReceivedTutorial(source.getPlayerOrException(), false);

        source.sendSuccess(() -> Component.literal("Cleared all Via Romana caches"), true);
        return 1;
    }

    /**
     * Clears only the map caches (server and client map renderers)
     */
    private static int clearMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerMapCache.clear();
        ServerMapCache.deleteAllMapsFromDisk();
        if (source.getLevel().isClientSide()) MapRenderer.clearCache();
        source.sendSuccess(() -> Component.literal("Cleared Via Romana map cache"), true);
        return 1;
    }

    /**
     * Immediately processes any queued/dirty networks to regenerate their maps
     */
    private static int regenerateMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerMapCache.processAllDirtyNetworks();
        source.sendSuccess(() -> Component.literal("Triggered regeneration for dirty Via Romana maps"), true);
        return 1;
    }

    /**
     * Saves all in-memory maps to disk
     */
    private static int saveMaps(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerMapCache.saveAllToDisk(true);
        source.sendSuccess(() -> Component.literal("Saved Via Romana maps to disk"), true);
        return 1;
    }

    /*
    * Tests PNG attachment to a given chunk
     */
    private static int testPngAttachment(CommandSourceStack source, int cx, int cz) {
        ServerLevel level = source.getLevel();
        ChunkPos pos = new ChunkPos(cx, cz);

        // Step 1: Render & set
        source.sendSuccess(() -> Component.literal("Rendering PNG for chunk " + pos + "..."), false);
        byte[] bytes = ChunkPngUtil.renderChunkPngBytes(level, pos);
        ChunkPngUtil.setPngBytes(level, pos, bytes);
        source.sendSuccess(() -> Component.literal("Set PNG (size: " + bytes.length + " bytes)"), false);

        // Step 2: Immediate get (should match)
        Optional<byte[]> immediate = ChunkPngUtil.getPngBytes(level, pos);
        if (immediate.isPresent() && java.util.Arrays.equals(immediate.get(), bytes)) {
            source.sendSuccess(() -> Component.literal("Immediate get: SUCCESS (matches)"), false);
        } else {
            source.sendFailure(Component.literal("Immediate get: FAIL"));
        }

        // Step 3: Reload chunk (unload/load to test persistence)
        // Force unload (walk away or use /tp), then return and run:
        source.sendSuccess(() -> Component.literal("Reload chunk and re-run /testpng to check persistence"), false);

        return 1;
    }
}
