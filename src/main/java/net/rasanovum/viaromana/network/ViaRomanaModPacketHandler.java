package net.rasanovum.viaromana.network;

import net.rasanovum.viaromana.teleport.ServerTeleportHandler;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.rasanovum.viaromana.surveyor.ViaRomanaLandmarkManager;
import net.rasanovum.viaromana.storage.IPathStorage;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.Node.NodeData;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

class FabricNetworkHandler implements NetworkHandler {
    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload message) {
        if (player == null) return;

        ServerPlayNetworking.send(player, message);
    }
    
    @Override
    public void sendToServer(CustomPacketPayload message) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(message);
    }
}

public class ViaRomanaModPacketHandler {
    public static final ResourceLocation PLAYER_VARIABLES_SYNC_C2S = ResourceLocation.parse("via_romana:player_variables_sync_c2s");
    public static final ResourceLocation PLAYER_VARIABLES_SYNC_S2C = ResourceLocation.parse("via_romana:player_variables_sync_s2c");
    public static final ResourceLocation GLOBAL_VARIABLES_SYNC_S2C = ResourceLocation.parse("via_romana:global_variables_sync_s2c");
    public static final ResourceLocation TELEPORT_REQUEST_C2S = ResourceLocation.parse("via_romana:teleport_request_c2s");
    public static final ResourceLocation LINK_SIGN_REQUEST_C2S = ResourceLocation.parse("via_romana:link_sign_request_c2s");
    public static final ResourceLocation UNLINK_SIGN_REQUEST_C2S = ResourceLocation.parse("via_romana:unlink_sign_request_c2s");
    public static final ResourceLocation DESTINATION_RESPONSE_S2C = ResourceLocation.parse("via_romana:destination_response_s2c");
    public static final ResourceLocation OPEN_LINK_SIGN_SCREEN_S2C = ResourceLocation.parse("via_romana:open_link_sign_screen_s2c");
    public static final ResourceLocation OPEN_CHARTING_SCREEN_S2C = ResourceLocation.parse("via_romana:open_charting_screen_s2c");
    public static final ResourceLocation OPEN_EDIT_SCREEN_S2C = ResourceLocation.parse("via_romana:open_edit_screen_s2c");
    public static final ResourceLocation PATH_GRAPH_SYNC_S2C = ResourceLocation.parse("via_romana:path_graph_sync_s2c");
    public static final ResourceLocation MAP_REQUEST_C2S = ResourceLocation.parse("via_romana:map_request_c2s");
    public static final ResourceLocation MAP_RESPONSE_S2C = ResourceLocation.parse("via_romana:map_response_s2c");
    public static final ResourceLocation DESTINATION_REQUEST_C2S = ResourceLocation.parse("via_romana:destination_request_c2s");
    public static final ResourceLocation SIGN_VALIDATION_C2S = ResourceLocation.parse("via_romana:sign_validation_c2s");
    public static final ResourceLocation SIGN_VALIDATION_S2C = ResourceLocation.parse("via_romana:sign_validation_s2c");
    public static final ResourceLocation ACTION_REQUEST_C2S = ResourceLocation.parse("via_romana:action_request_c2s");
    public static final ResourceLocation CHARTED_PATH_C2S = ResourceLocation.parse("via_romana:charted_path_c2s");
    public static final ResourceLocation CONFIG_SYNC_C2S = ResourceLocation.parse("via_romana:config_sync_c2s");
    public static final ResourceLocation CONFIG_SYNC_S2C = ResourceLocation.parse("via_romana:config_sync_s2c");
    public static final ResourceLocation OPEN_WARP_BLOCK_SCREEN_S2C = ResourceLocation.parse("via_romana:open_warp_block_screen_s2c");

    public static void initialize() {
        ViaRomanaModVariables.networkHandler = new FabricNetworkHandler();
        registerC2SPackets();
    }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(ViaRomanaModVariables.PlayerVariablesSyncMessage.TYPE, ViaRomanaModPacketHandler::handlePlayerVariablesSyncC2S);
        ServerPlayNetworking.registerGlobalReceiver(TeleportRequestPacket.TYPE, ViaRomanaModPacketHandler::handleTeleportRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(LinkSignRequestPacket.TYPE, ViaRomanaModPacketHandler::handleLinkSignRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(UnlinkSignRequestPacket.TYPE, ViaRomanaModPacketHandler::handleUnlinkSignRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(DestinationRequestPacket.TYPE, ViaRomanaModPacketHandler::handleDestinationRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(SignValidationC2S.TYPE, ViaRomanaModPacketHandler::handleSignValidationC2S);
        ServerPlayNetworking.registerGlobalReceiver(MapRequestC2S.TYPE, ViaRomanaModPacketHandler::handleMapRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(ChartedPathC2S.TYPE, ViaRomanaModPacketHandler::handleChartedPathC2S);
        ServerPlayNetworking.registerGlobalReceiver(RoutedActionC2S.TYPE, ViaRomanaModPacketHandler::handleActionRequestC2S);
    }

    private static void handlePlayerVariablesSyncC2S(ViaRomanaModVariables.PlayerVariablesSyncMessage packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> ViaRomanaModVariables.PlayerVariablesSyncMessage.handleServer(packet, context.player()));
    }

    private static void handleActionRequestC2S(RoutedActionC2S packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ServerLevel level = player.serverLevel();
            var storage = IPathStorage.get(level);
            var graph = storage.graph();

            Optional<Node> nearestOpt = graph.getNearestNode(player.blockPosition(), ViaRomanaConfig.node_utility_distance, node -> true);

            if (nearestOpt.isEmpty()) {
                ViaRomana.LOGGER.warn("No nearby node found for action {} by player {}", packet.op(), player.getName().getString());
                return;
            }

            Node nearestNode = nearestOpt.get();

            switch (packet.op()) {
                case SEVER_NEAREST_NODE -> {
                    graph.removeNode(nearestNode);
                }
                case REMOVE_BRANCH -> {
                    graph.removeBranch(nearestNode);
                }
            }

            // Always persist and sync after graph mutation, regardless of cache state
            storage.setDirty();
            PathSyncUtils.syncPathGraphToAllPlayers(level);
        });
    }
    
    private static void handleTeleportRequestC2S(TeleportRequestPacket packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> ServerTeleportHandler.handleTeleportRequest(packet, context.player()));
    }
    
    private static void handleLinkSignRequestC2S(LinkSignRequestPacket packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerLevel level = context.player().serverLevel();
            LinkHandler.LinkData linkData = packet.linkData();
            boolean isTempNode = packet.isTempNode();

            if (!LinkHandler.isSignBlock(level, linkData.signPos())) {
                ViaRomana.LOGGER.warn("Link request for non-existent sign at {}", linkData.signPos());
                return;
            }

            if (!isTempNode) {
                boolean success = LinkHandler.linkSignToNode(level, linkData);
                if (!success) {
                    ViaRomana.LOGGER.warn("Failed to link sign at {} to node at {}", linkData.signPos(), linkData.nodePos());
                }
            }
        });
    }
    
    private static void handleUnlinkSignRequestC2S(UnlinkSignRequestPacket packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerLevel level = context.player().serverLevel();
            boolean success = LinkHandler.unlinkSignFromNode(level, packet.signPos()); // TODO: port to PathGraph?
            if (!success) {
                ViaRomana.LOGGER.warn("Failed to unlink sign at {}", packet.signPos());
            }
        });
    }

    /**
     * Handles a map request from a client by baking the map asynchronously and sending it back.
     */
    private static void handleMapRequestC2S(MapRequestC2S packet, ServerPlayNetworking.Context context) {
        java.util.UUID networkId = packet.getNetworkId();

        context.server().execute(() -> {
            // Get the network cache to access bounds
            PathGraph graph = IPathStorage.get(context.player().level()).graph();
            PathGraph.NetworkCache network = graph != null ? graph.getNetworkCache(networkId) : null;

            if (network == null) {
                ViaRomana.LOGGER.warn("Could not find network with ID {} for map request", networkId);
                return;
            }

            ServerMapCache.getMapData(networkId).ifPresentOrElse(result -> {
                MapResponseS2C response = MapResponseS2C.create(result.networkId(), result.minBounds(), result.maxBounds(), result.networkNodes(), result.pngData(), result.bakeScaleFactor());
                if (ViaRomanaModVariables.networkHandler != null) {
                    ViaRomanaModVariables.networkHandler.sendToPlayer(context.player(), response);
                }
            }, () -> {
                ServerLevel level = context.player().serverLevel();
                ServerMapCache.generateMapIfNeeded(networkId, level).thenAcceptAsync(produced -> {
                    if (produced != null && ViaRomanaModVariables.networkHandler != null) {
                        MapResponseS2C response = MapResponseS2C.create(produced.networkId(), produced.minBounds(), produced.maxBounds(), produced.networkNodes(), produced.pngData(), produced.bakeScaleFactor());
                        ViaRomanaModVariables.networkHandler.sendToPlayer(context.player(), response);
                    }
                }, context.server());
            });
        });
    }
    
    private static void handleDestinationRequestC2S(DestinationRequestPacket packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            BlockPos sourceSignPos = packet.sourceSignPos();
            Level level = player.level();
            UUID playerUUID = player.getUUID();
            
            PathGraph graph = IPathStorage.get(level).graph();
            Optional<Node> sourceNodeOpt = Optional.empty();
            try {
                sourceNodeOpt = graph.getNodeBySignPos(sourceSignPos);
            } catch (IndexOutOfBoundsException e) {
                ViaRomana.LOGGER.warn("getNodeBySignPos out of bounds for sign {}: {}", sourceSignPos, e.getMessage());
            }
            
            if (sourceNodeOpt.isEmpty()) {
                DestinationResponsePacket resp = new DestinationResponsePacket(
                    new ArrayList<>(), sourceSignPos, BlockPos.ZERO, new ArrayList<>(), java.util.UUID.randomUUID());
                ViaRomanaModVariables.networkHandler.sendToPlayer(player, resp);
                return;
            }
            
            Node sourceNode = sourceNodeOpt.get();
            BlockPos sourceNodePos = BlockPos.of(sourceNode.getPos());
            
            List<Node> destinationNodes = graph.getCachedTeleportDestinationsFor(playerUUID, sourceNode);
            List<DestinationResponsePacket.DestinationInfo> infos = new ArrayList<>();
            
            for (Node node : destinationNodes) {
                BlockPos nodePos = node.getBlockPos();
                String name = node.getDestinationName().orElse("Unnamed Destination");
                double distance = Math.sqrt(sourceNodePos.distSqr(nodePos));
                Node.Icon icon = node.getDestinationIcon().orElse(Node.Icon.SIGNPOST);
                
                infos.add(new DestinationResponsePacket.DestinationInfo(nodePos, name, distance, icon));
            }
            
            List<DestinationResponsePacket.NodeNetworkInfo> networkNodes = new ArrayList<>();
            
            sourceNodeOpt.ifPresent(startNode -> {
                var network = graph.getNetwork(startNode);
                for (net.rasanovum.viaromana.path.Node node : network) {
                    List<BlockPos> connections = new ArrayList<>();
                    for (long connectedPos : node.getConnectedNodes()) {
                        connections.add(BlockPos.of(connectedPos));
                    }
                    networkNodes.add(new DestinationResponsePacket.NodeNetworkInfo(
                        node.getBlockPos(),
                        connections
                    ));
                }
            });

            DestinationResponsePacket resp;
            {
                java.util.UUID networkUuid = null;
                var networkCache = graph.getNetworkCache(sourceNode);
                if (networkCache != null) {
                    networkUuid = networkCache.id();
                }
                resp = new DestinationResponsePacket(infos, sourceSignPos, sourceNodePos, networkNodes, networkUuid != null ? networkUuid : java.util.UUID.randomUUID());
            }
            ViaRomanaModVariables.networkHandler.sendToPlayer(player, resp);
        });
    }

    private static void handleSignValidationC2S(SignValidationC2S packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ServerLevel level = player.serverLevel();
            BlockPos nodePos = packet.nodePos();
            
            IPathStorage storage = IPathStorage.get(level);
            PathGraph graph = storage.graph();
            Optional<Node> nodeOpt = graph.getNodeAt(nodePos);

            if (nodeOpt.isEmpty()) {
                SignValidationS2C response = new SignValidationS2C(nodePos, false);
                ViaRomanaModVariables.networkHandler.sendToPlayer(player, response);
                return;
            }

            Node node = nodeOpt.get();

            boolean isSignValid = node.getSignPos()
                .map(signPosLong -> LinkHandler.isSignBlock(level, BlockPos.of(signPosLong)))
                .orElse(true);

            if (!isSignValid) {
                node.getSignPos().ifPresent(signPosLong -> {
                    graph.removeSignLink(BlockPos.of(signPosLong));
                });
                storage.setDirty();
                ViaRomanaLandmarkManager.removeDestinationLandmark(level, node);
                PathSyncUtils.syncPathGraphToAllPlayers(level);
            }

            SignValidationS2C response = new SignValidationS2C(nodePos, isSignValid);
            ViaRomanaModVariables.networkHandler.sendToPlayer(player, response);
        });
    }

    private static void handleChartedPathC2S(ChartedPathC2S packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerLevel level = context.player().serverLevel();
            IPathStorage storage = IPathStorage.get(level);

            List<NodeData> chartingNodes = packet.getChartedNodes();

            if (chartingNodes.isEmpty()) {
                ViaRomana.LOGGER.warn("Received empty charted path from player {}", context.player().getName().getString());
                return;
            }

            try {
                storage.graph().createConnectedPath(chartingNodes);
                storage.setDirty();

                PathSyncUtils.syncPathGraphToAllPlayers(level);

                awardAdvancementIfNeeded(context.player(), "via_romana:a_strand_type_game");

                ViaRomana.LOGGER.debug("Created charted path with {} nodes for player {}", chartingNodes.size(), context.player().getName().getString());
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to create charted path for player {}: {}", context.player().getName().getString(), e.getMessage());
            }
        });
    }

    private static void awardAdvancementIfNeeded(ServerPlayer player, String id) {
        try {
            AdvancementHolder advancement = player.server.getAdvancements().get(ResourceLocation.parse(id));
            if (advancement != null) {
                AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancement);
                if (!advancementProgress.isDone()) {
                    for (String c : advancementProgress.getRemainingCriteria()) {
                        player.getAdvancements().award(advancement, c);
                    }
                }
            }
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to award advancement {} to player {}: {}", id, player.getName().getString(), e.getMessage());
        }
    }
}
