package net.rasanovum.viaromana.network.packets;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.advancements.AdvancementHolder;
//?} else {
/*import net.minecraft.advancements.Advancement;
*///?}

import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.path.Node.NodeData;
import net.rasanovum.viaromana.storage.IPathStorage;
import net.rasanovum.viaromana.util.PathSyncUtils;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from client to server when a player finishes charting a path.
 * Contains all the temporary nodes/links that should be made permanent
 * and connected as a path on the server-side PathGraph.
 */
public class ChartedPathC2S {
    public static final ResourceLocation CHANNEL = VersionUtils.getLocation("via_romana:charted_path_c2s");
    //? if >1.21
    public static final StreamCodec<FriendlyByteBuf, ChartedPathC2S> STREAM_CODEC = StreamCodec.ofMember(ChartedPathC2S::encode, ChartedPathC2S::decode);
    private final List<NodeData> chartedNodes;

    public ChartedPathC2S(List<NodeData> chartedNodes) {
        this.chartedNodes = chartedNodes != null ? List.copyOf(chartedNodes) : List.of();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.chartedNodes.size());
        for (NodeData nodeData : this.chartedNodes) {
            buffer.writeBlockPos(nodeData.pos());
            buffer.writeFloat(nodeData.quality());
            buffer.writeFloat(nodeData.clearance());
        }
    }

    public static ChartedPathC2S decode(FriendlyByteBuf buffer) {
        int nodeCount = buffer.readInt();
        List<NodeData> nodes = new ArrayList<>(nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            BlockPos pos = buffer.readBlockPos();
            float quality = buffer.readFloat();
            float clearance = buffer.readFloat();
            nodes.add(new NodeData(pos, quality, clearance));
        }
        return new ChartedPathC2S(nodes);
    }

    //? if <1.21 {
    /*public static ResourceLocation type() {
        return CHANNEL;
    }
    *///?} else {
    public static CustomPacketPayload.Type<CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(CHANNEL);
    }
    //?}

    public List<NodeData> getChartedNodes() {
        return chartedNodes;
    }

    public static void handle(PacketContext<ChartedPathC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            ServerLevel level = ctx.sender().serverLevel();
            IPathStorage storage = IPathStorage.get(level);

            List<NodeData> chartingNodes = ctx.message().getChartedNodes();

            if (chartingNodes.isEmpty()) {
                ViaRomana.LOGGER.warn("Received empty charted path from player {}", ctx.sender().getName().getString());
                return;
            }

            try {
                storage.graph().createConnectedPath(chartingNodes);
                storage.setDirty();

                PathSyncUtils.syncPathGraphToAllPlayers(level);

                awardAdvancementIfNeeded(ctx.sender());

                ViaRomana.LOGGER.debug("Created charted path with {} nodes for player {}", chartingNodes.size(), ctx.sender().getName().getString());
            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to create charted path for player {}: {}", ctx.sender().getName().getString(), e.getMessage());
            }
        }
    }

    private static void awardAdvancementIfNeeded(ServerPlayer player) {
        try {
            //? if <1.21 {
            /*Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("via_romana:story/a_strand_type_game"));
            if (advancement != null) {
                AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancement);
                if (!advancementProgress.isDone()) {
                    for (String c : advancementProgress.getRemainingCriteria()) {
                        player.getAdvancements().award(advancement, c);
                    }
                }
            }
            *///?} else {
            ResourceLocation advancementId = ResourceLocation.parse("via_romana:a_strand_type_game");
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
            //?}
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to award advancement {} to player {}: {}", "via_romana:story/a_strand_type_game", player.getName().getString(), e.getMessage());
        }
    }
}