package net.rasanovum.viaromana.network;

import net.rasanovum.viaromana.network.packets.*;
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
import net.rasanovum.viaromana.CommonConfig;
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

public class ViaRomanaModPacketHandler {
    public static void initialize() {
        ViaRomanaModVariables.networkHandler = new FabricNetworkHandler();
        registerC2SPackets();
    }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(ViaRomanaModVariables.PlayerVariablesSyncMessage.TYPE, ViaRomanaModPacketHandler::handlePlayerVariablesSyncC2S);
        ServerPlayNetworking.registerGlobalReceiver(TeleportRequestC2S.TYPE, ViaRomanaModPacketHandler::handleTeleportRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(LinkSignRequestC2S.TYPE, ViaRomanaModPacketHandler::handleLinkSignRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(UnlinkSignRequestC2S.TYPE, ViaRomanaModPacketHandler::handleUnlinkSignRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(DestinationRequestC2S.TYPE, ViaRomanaModPacketHandler::handleDestinationRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(SignValidationRequestC2S.TYPE, ViaRomanaModPacketHandler::handleSignValidationRequestC2S);
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

            Optional<Node> nearestOpt = graph.getNearestNode(player.blockPosition(), CommonConfig.node_utility_distance, node -> true);

            if (nearestOpt.isEmpty()) {
                ViaRomana.LOGGER.warn("No nearby node found for action {} by player {}", packet.op(), player.getName().getString());
                return;
            }

            Node nearestNode = nearestOpt.get();

            switch (packet.op()) {
                case RoutedActionC2S.Operation.SEVER_NEAREST_NODE -> {
                    graph.removeNode(nearestNode);
                }
                case RoutedActionC2S.Operation.REMOVE_BRANCH -> {
                    graph.removeBranch(nearestNode);
                }
            }

            storage.setDirty();
            PathSyncUtils.syncPathGraphToAllPlayers(level);
        });
    }
    
    private static void handleTeleportRequestC2S(TeleportRequestC2S packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> ServerTeleportHandler.handleTeleportRequest(packet, context.player()));
    }
    
    private static void handleLinkSignRequestC2S(LinkSignRequestC2S packet, ServerPlayNetworking.Context context) {
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
    
    private static void handleUnlinkSignRequestC2S(UnlinkSignRequestC2S packet, ServerPlayNetworking.Context context) {
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
    
    private static void handleDestinationRequestC2S(DestinationRequestC2S packet, ServerPlayNetworking.Context context) {
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
                DestinationResponseS2C resp = new DestinationResponseS2C(
                    new ArrayList<>(), sourceSignPos, BlockPos.ZERO, new ArrayList<>(), java.util.UUID.randomUUID());
                ViaRomanaModVariables.networkHandler.sendToPlayer(player, resp);
                return;
            }
            
            Node sourceNode = sourceNodeOpt.get();
            BlockPos sourceNodePos = BlockPos.of(sourceNode.getPos());
            
            List<Node> destinationNodes = graph.getCachedTeleportDestinationsFor(playerUUID, sourceNode);
            List<DestinationResponseS2C.DestinationInfo> infos = new ArrayList<>();
            
            for (Node node : destinationNodes) {
                BlockPos nodePos = node.getBlockPos();
                String name = node.getDestinationName().orElse("Unnamed Destination");
                double distance = Math.sqrt(sourceNodePos.distSqr(nodePos));
                Node.Icon icon = node.getDestinationIcon().orElse(Node.Icon.SIGNPOST);
                
                infos.add(new DestinationResponseS2C.DestinationInfo(nodePos, name, distance, icon));
            }
            
            List<DestinationResponseS2C.NodeNetworkInfo> networkNodes = new ArrayList<>();
            
            sourceNodeOpt.ifPresent(startNode -> {
                var network = graph.getNetwork(startNode);
                for (net.rasanovum.viaromana.path.Node node : network) {
                    List<BlockPos> connections = new ArrayList<>();
                    for (long connectedPos : node.getConnectedNodes()) {
                        connections.add(BlockPos.of(connectedPos));
                    }
                    networkNodes.add(new DestinationResponseS2C.NodeNetworkInfo(
                        node.getBlockPos(),
                        node.getClearance(),
                        connections
                    ));
                }
            });

            DestinationResponseS2C resp;
            {
                java.util.UUID networkUuid = null;
                var networkCache = graph.getNetworkCache(sourceNode);
                if (networkCache != null) {
                    networkUuid = networkCache.id();
                }
                resp = new DestinationResponseS2C(infos, sourceSignPos, sourceNodePos, networkNodes, networkUuid != null ? networkUuid : java.util.UUID.randomUUID());
            }
            ViaRomanaModVariables.networkHandler.sendToPlayer(player, resp);
        });
    }

    private static void handleSignValidationRequestC2S(SignValidationRequestC2S packet, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            ServerLevel level = player.serverLevel();
            BlockPos nodePos = packet.nodePos();
            
            IPathStorage storage = IPathStorage.get(level);
            PathGraph graph = storage.graph();
            Optional<Node> nodeOpt = graph.getNodeAt(nodePos);

            if (nodeOpt.isEmpty()) {
                SignValidationResponseS2C response = new SignValidationResponseS2C(nodePos, false);
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

            SignValidationResponseS2C response = new SignValidationResponseS2C(nodePos, isSignValid);
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

                awardAdvancementIfNeeded(context.player(), "via_romana:story/a_strand_type_game");

                ViaRomana.LOGGER.debug("Created charted path with {} nodes for player {}", chartingNodes.size(), context.player().getName().getString());
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to create charted path for player {}: {}", context.player().getName().getString(), e.getMessage());
            }
        });
    }

    private static void awardAdvancementIfNeeded(ServerPlayer player, String id) {
        try {
            ResourceLocation advancementId = ResourceLocation.parse(id);
            AdvancementHolder advancement = player.server.getAdvancements().get(advancementId);
            if (advancement != null) {
                AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancement);
                if (!advancementProgress.isDone()) {
                    boolean grantedAny = false;
                    for (String criterion : advancementProgress.getRemainingCriteria()) {
                        boolean granted = player.getAdvancements().award(advancement, criterion);
                        if (granted) grantedAny = true;
                    }

                    if (grantedAny) player.getAdvancements().flushDirty(player);
                }
            }
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to award advancement {} to player {}: {}", id, player.getName().getString(), e.getMessage());
            e.printStackTrace();
        }
    }
}