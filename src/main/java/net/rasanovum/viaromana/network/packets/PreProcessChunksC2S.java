package net.rasanovum.viaromana.network.packets;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.map.ServerMapUtils;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.path.Node.NodeData;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.storage.path.PathDataManager;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.rasanovum.viaromana.util.VersionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
//?} else {
//?}

/**
 * Packet sent from client to server while charting a path.
 * Contains all the temporary nodes of the path so far
 * for the server to render chunk data.
 */
//? if <1.21 {
/*public record ChartedPathC2S(List<NodeData> tempNodes) {
*///?} else {
public record PreProcessChunksC2S(List<NodeData> tempNodes) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:charted_path_c2s");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final Type<PreProcessChunksC2S> TYPE = new Type<>(VersionUtils.getLocation("via_romana:pre_process_chunks_c2s"));

    public static final StreamCodec<FriendlyByteBuf, PreProcessChunksC2S> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buffer, PreProcessChunksC2S packet) { PreProcessChunksC2S.encode(buffer, packet); }

        @Override
        public PreProcessChunksC2S decode(FriendlyByteBuf buffer) {
            return PreProcessChunksC2S.decode(buffer);
        }
    };
    //?}

    public PreProcessChunksC2S(List<NodeData> tempNodes) {
        this.tempNodes = tempNodes != null ? List.copyOf(tempNodes) : List.of();
    }

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buffer, PreProcessChunksC2S packet) {
        buffer.writeInt(packet.tempNodes.size());
        for (NodeData nodeData : packet.tempNodes) {
            buffer.writeBlockPos(nodeData.pos());
            buffer.writeFloat(nodeData.quality());
            buffer.writeFloat(nodeData.clearance());
        }
    }

    public static PreProcessChunksC2S decode(FriendlyByteBuf buffer) {
        int nodeCount = buffer.readInt();
        List<NodeData> nodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            BlockPos pos = buffer.readBlockPos();
            float quality = buffer.readFloat();
            float clearance = buffer.readFloat();
            nodes.add(new NodeData(pos, quality, clearance));
        }
        return new PreProcessChunksC2S(nodes);
    }

    public static void handle(PacketContext<PreProcessChunksC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            ServerLevel level = ctx.sender().serverLevel();
            List<NodeData> tempNodes = ctx.message().tempNodes();
            
            if (tempNodes.isEmpty() || tempNodes.size() < 2) {
                return;
            }
            
            // Convert NodeData to NodeNetworkInfo for FoW calculation
            List<NodeNetworkInfo> nodeInfos = tempNodes.stream()
                .map(nd -> new NodeNetworkInfo(nd.pos(), nd.clearance(), List.of()))
                .toList();
            
            // Calculate FoW data without creating a network cache
            // ServerMapUtils.FoWData fowData = ServerMapUtils.calculateFoWData(nodeInfos);
            
            // if (fowData != null) {
            //     // Pre-process chunks asynchronously
            //     CompletableFuture.runAsync(() -> {
            //         ChunkPreProcessor.processChunks(level, fowData);
            //     }, ServerMapCache.getExecutor());
            // }
        }
    }
}