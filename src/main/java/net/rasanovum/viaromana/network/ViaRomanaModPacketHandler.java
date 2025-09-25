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
import net.rasanovum.viaromana.CommonConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Optional;

class FabricNetworkHandler implements NetworkHandler {
    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        if (player == null) return;

        ResourceLocation packetId = null;
        FriendlyByteBuf buf = PacketByteBufs.create();
        if (message instanceof ViaRomanaModVariables.PlayerVariablesSyncMessage syncMessage) {
            packetId = ViaRomanaModPacketHandler.PLAYER_VARIABLES_SYNC_S2C;
            syncMessage.write(buf);
        } else if (message instanceof DestinationResponsePacket destinationMessage) {
            packetId = ViaRomanaModPacketHandler.DESTINATION_RESPONSE_S2C;
            destinationMessage.write(buf);
        } else if (message instanceof OpenLinkScreenPacket linkSignScreenMessage) {
            packetId = ViaRomanaModPacketHandler.OPEN_LINK_SIGN_SCREEN_S2C;
            linkSignScreenMessage.write(buf);
        } else if (message instanceof OpenChartingScreenPacket chartingScreenMessage) {
            packetId = ViaRomanaModPacketHandler.OPEN_CHARTING_SCREEN_S2C;
            chartingScreenMessage.write(buf);
        } else if (message instanceof PathGraphSyncPacket pathGraphMessage) {
            packetId = ViaRomanaModPacketHandler.PATH_GRAPH_SYNC_S2C;
            pathGraphMessage.write(buf);
        } else if (message instanceof MapResponseS2C mapResponseMessage) {
            packetId = ViaRomanaModPacketHandler.MAP_RESPONSE_S2C;
            mapResponseMessage.write(buf);
        } else if (message instanceof SignValidationS2C signValidationMessage) {
            packetId = ViaRomanaModPacketHandler.SIGN_VALIDATION_S2C;
            signValidationMessage.write(buf);
        } else if (message instanceof OpenWarpBlockScreenS2CPacket warpBlockScreenMessage) {
            packetId = ViaRomanaModPacketHandler.OPEN_WARP_BLOCK_SCREEN_S2C;
            warpBlockScreenMessage.write(buf);
        } else {
            ViaRomana.LOGGER.error("Attempted to send unknown message type via FabricNetworkHandler.sendToPlayer: {}", message.getClass().getName());
            return;
        }

        if (packetId != null) {
            ServerPlayNetworking.send(player, packetId, buf);
        } else {
             ViaRomana.LOGGER.error("Packet ID was null when trying to send message of type {} to {}", message.getClass().getName(), player.getName().getString());
        }
    }
    
    @Override
    public void sendToServer(Object message) {
        ResourceLocation packetId = null;
        FriendlyByteBuf buf = PacketByteBufs.create();
        
        if (message instanceof DestinationRequestPacket destReq) {
            packetId = ViaRomanaModPacketHandler.DESTINATION_REQUEST_C2S;
            destReq.write(buf);
        } else if (message instanceof TeleportRequestPacket teleportMessage) {
            packetId = ViaRomanaModPacketHandler.TELEPORT_REQUEST_C2S;
            teleportMessage.write(buf);
        } else if (message instanceof LinkSignRequestPacket linkSignMessage) {
            packetId = ViaRomanaModPacketHandler.LINK_SIGN_REQUEST_C2S;
            linkSignMessage.write(buf);
        } else if (message instanceof MapRequestC2S mapRequestMessage) {
            packetId = ViaRomanaModPacketHandler.MAP_REQUEST_C2S;
            mapRequestMessage.write(buf);
        } else if (message instanceof RoutedActionC2S action) {
            packetId = ViaRomanaModPacketHandler.ACTION_REQUEST_C2S;
            action.write(buf);
        } else if (message instanceof ChartedPathC2S chartedPathMessage) {
            packetId = ViaRomanaModPacketHandler.CHARTED_PATH_C2S;
            chartedPathMessage.write(buf);
        } else if (message instanceof SignValidationC2S signValidationMessage) {
            packetId = ViaRomanaModPacketHandler.SIGN_VALIDATION_C2S;
            signValidationMessage.write(buf);
        } else {
            ViaRomana.LOGGER.error("Attempted to send unknown message type via FabricNetworkHandler.sendToServer: {}", message.getClass().getName());
            return;
        }
        
        if (packetId != null) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(packetId, buf);
        }
    }
}

public class ViaRomanaModPacketHandler {
    public static final ResourceLocation PLAYER_VARIABLES_SYNC_C2S = new ResourceLocation(ViaRomana.MODID, "player_variables_sync_c2s");
    public static final ResourceLocation PLAYER_VARIABLES_SYNC_S2C = new ResourceLocation(ViaRomana.MODID, "player_variables_sync_s2c");
    public static final ResourceLocation GLOBAL_VARIABLES_SYNC_S2C = new ResourceLocation(ViaRomana.MODID, "global_variables_sync_s2c");
    public static final ResourceLocation TELEPORT_REQUEST_C2S = new ResourceLocation(ViaRomana.MODID, "teleport_request_c2s");
    public static final ResourceLocation LINK_SIGN_REQUEST_C2S = new ResourceLocation(ViaRomana.MODID, "link_sign_request_c2s");
    public static final ResourceLocation UNLINK_SIGN_REQUEST_C2S = new ResourceLocation(ViaRomana.MODID, "unlink_sign_request_c2s");
    public static final ResourceLocation DESTINATION_RESPONSE_S2C = new ResourceLocation(ViaRomana.MODID, "destination_response_s2c");
    public static final ResourceLocation OPEN_LINK_SIGN_SCREEN_S2C = new ResourceLocation(ViaRomana.MODID, "open_link_sign_screen_s2c");
    public static final ResourceLocation OPEN_CHARTING_SCREEN_S2C = new ResourceLocation(ViaRomana.MODID, "open_charting_screen_s2c");
    public static final ResourceLocation OPEN_EDIT_SCREEN_S2C = new ResourceLocation(ViaRomana.MODID, "open_edit_screen_s2c");
    public static final ResourceLocation PATH_GRAPH_SYNC_S2C = new ResourceLocation(ViaRomana.MODID, "path_graph_sync_s2c");
    public static final ResourceLocation MAP_REQUEST_C2S = new ResourceLocation(ViaRomana.MODID, "map_request_c2s");
    public static final ResourceLocation MAP_RESPONSE_S2C = new ResourceLocation(ViaRomana.MODID, "map_response_s2c");
    public static final ResourceLocation DESTINATION_REQUEST_C2S = new ResourceLocation(ViaRomana.MODID, "destination_request_c2s");
    public static final ResourceLocation SIGN_VALIDATION_C2S = new ResourceLocation(ViaRomana.MODID, "sign_validation_c2s");
    public static final ResourceLocation SIGN_VALIDATION_S2C = new ResourceLocation(ViaRomana.MODID, "sign_validation_s2c");
    public static final ResourceLocation ACTION_REQUEST_C2S = new ResourceLocation(ViaRomana.MODID, "action_request_c2s");
    public static final ResourceLocation CHARTED_PATH_C2S = new ResourceLocation(ViaRomana.MODID, "charted_path_c2s");
    public static final ResourceLocation CONFIG_SYNC_C2S = new ResourceLocation(ViaRomana.MODID, "config_sync_c2s");
    public static final ResourceLocation CONFIG_SYNC_S2C = new ResourceLocation(ViaRomana.MODID, "config_sync_s2c");
    public static final ResourceLocation OPEN_WARP_BLOCK_SCREEN_S2C = new ResourceLocation(ViaRomana.MODID, "open_warp_block_screen_s2c");

    public static void initialize() {
        ViaRomanaModVariables.networkHandler = new FabricNetworkHandler();
        registerC2SPackets();
    }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(PLAYER_VARIABLES_SYNC_C2S, ViaRomanaModPacketHandler::handlePlayerVariablesSyncC2S);
        ServerPlayNetworking.registerGlobalReceiver(TELEPORT_REQUEST_C2S, ViaRomanaModPacketHandler::handleTeleportRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(LINK_SIGN_REQUEST_C2S, ViaRomanaModPacketHandler::handleLinkSignRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(UNLINK_SIGN_REQUEST_C2S, ViaRomanaModPacketHandler::handleUnlinkSignRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(DESTINATION_REQUEST_C2S, ViaRomanaModPacketHandler::handleDestinationRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(SIGN_VALIDATION_C2S, ViaRomanaModPacketHandler::handleSignValidationC2S);
        ServerPlayNetworking.registerGlobalReceiver(MAP_REQUEST_C2S, ViaRomanaModPacketHandler::handleMapRequestC2S);
        ServerPlayNetworking.registerGlobalReceiver(CHARTED_PATH_C2S, ViaRomanaModPacketHandler::handleChartedPathC2S);
        ServerPlayNetworking.registerGlobalReceiver(ACTION_REQUEST_C2S, ViaRomanaModPacketHandler::handleActionRequestC2S);
    }

    private static void handlePlayerVariablesSyncC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        ViaRomanaModVariables.PlayerVariablesSyncMessage message = new ViaRomanaModVariables.PlayerVariablesSyncMessage(buf);
        server.execute(() -> ViaRomanaModVariables.PlayerVariablesSyncMessage.handleServer(message, player));
    }

    private static void handleActionRequestC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        RoutedActionC2S message = new RoutedActionC2S(buf);
        server.execute(() -> {
            ServerLevel level = player.serverLevel();
            var storage = IPathStorage.get(level);
            var graph = storage.graph();

            Optional<Node> nearestOpt = graph.getNearestNode(player.blockPosition(), CommonConfig.node_utility_distance, node -> true);

            if (nearestOpt.isEmpty()) {
                ViaRomana.LOGGER.warn("No nearby node found for action {} by player {}", message.op(), player.getName().getString());
                return;
            }

            Node nearestNode = nearestOpt.get();

            switch (message.op()) {
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
    
    private static void handleTeleportRequestC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        TeleportRequestPacket message = new TeleportRequestPacket(buf);
        server.execute(() -> ServerTeleportHandler.handleTeleportRequest(message, player));
    }
    
    private static void handleLinkSignRequestC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        LinkSignRequestPacket message = new LinkSignRequestPacket(buf);
        server.execute(() -> {
            ServerLevel level = player.serverLevel();
            LinkHandler.LinkData linkData = message.getLinkData();
            Boolean isTempNode = message.isTempNode();

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
    
    private static void handleUnlinkSignRequestC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        final BlockPos signPos = buf.readBlockPos();
        server.execute(() -> {
            ServerLevel level = player.serverLevel();
            boolean success = LinkHandler.unlinkSignFromNode(level, signPos); // TODO: port to PathGraph?
            if (!success) {
                ViaRomana.LOGGER.warn("Failed to unlink sign at {}", signPos);
            }
        });
    }

    /**
     * Handles a map request from a client by baking the map asynchronously and sending it back.
     */
    private static void handleMapRequestC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        MapRequestC2S message = new MapRequestC2S(buf);
        java.util.UUID networkId = message.getNetworkId();

        server.execute(() -> {
            // Get the network cache to access bounds
            PathGraph graph = IPathStorage.get(player.level()).graph();
            PathGraph.NetworkCache network = graph != null ? graph.getNetworkCache(networkId) : null;
            
            if (network == null) {
                ViaRomana.LOGGER.warn("Could not find network with ID {} for map request", networkId);
                return;
            }
            
            ServerMapCache.getMapData(networkId).ifPresentOrElse(result -> {
                MapResponseS2C response = new MapResponseS2C(result.networkId(), result.minBounds(), result.maxBounds(), result.networkNodes(), result.pngData(), result.bakeScaleFactor());
                if (ViaRomanaModVariables.networkHandler != null) {
                    ViaRomanaModVariables.networkHandler.sendToPlayer(player, response);
                }
            }, () -> {
                ServerLevel level = player.serverLevel();
                ServerMapCache.generateMapIfNeeded(networkId, level).thenAcceptAsync(produced -> {
                    if (produced != null && ViaRomanaModVariables.networkHandler != null) {
                        MapResponseS2C response = new MapResponseS2C(produced.networkId(), produced.minBounds(), produced.maxBounds(), produced.networkNodes(), produced.pngData(), produced.bakeScaleFactor());
                        ViaRomanaModVariables.networkHandler.sendToPlayer(player, response);
                    }
                }, server);
            });
        });
    }
    
    private static void handleDestinationRequestC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        DestinationRequestPacket message = new DestinationRequestPacket(buf);
        server.execute(() -> {
            BlockPos sourceSignPos = message.getSourceSignPos();
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
                var networkCache = graph.getNetworkCache(sourceNode); // compute via start node
                if (networkCache != null) {
                    networkUuid = networkCache.id();
                }
                resp = new DestinationResponsePacket(infos, sourceSignPos, sourceNodePos, networkNodes, networkUuid != null ? networkUuid : java.util.UUID.randomUUID());
            }
            ViaRomanaModVariables.networkHandler.sendToPlayer(player, resp);
        });
    }

    private static void handleSignValidationC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        SignValidationC2S message = new SignValidationC2S(buf);
        server.execute(() -> {
            ServerLevel level = player.serverLevel();
            BlockPos nodePos = message.getNodePos();
            
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

    private static void handleChartedPathC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        ChartedPathC2S message = new ChartedPathC2S(buf);
        server.execute(() -> {
            ServerLevel level = player.serverLevel();
            IPathStorage storage = IPathStorage.get(level);
            
            List<NodeData> chartingNodes = message.getChartedNodes();
            
            if (chartingNodes.isEmpty()) {
                ViaRomana.LOGGER.warn("Received empty charted path from player {}", player.getName().getString());
                return;
            }

            try {
                storage.graph().createConnectedPath(chartingNodes);
                storage.setDirty();
                
                PathSyncUtils.syncPathGraphToAllPlayers(level);

                awardAdvancementIfNeeded(player, "via_romana:a_strand_type_game");

                ViaRomana.LOGGER.debug("Created charted path with {} nodes for player {}", chartingNodes.size(), player.getName().getString());
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to create charted path for player {}: {}", player.getName().getString(), e.getMessage());
            }
        });
    }

    private static void awardAdvancementIfNeeded(ServerPlayer player, String id) {
        try {
            Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation(id));
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
