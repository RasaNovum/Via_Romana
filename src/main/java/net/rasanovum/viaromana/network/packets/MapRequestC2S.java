package net.rasanovum.viaromana.network.packets;

import dev.corgitaco.dataanchor.network.broadcast.PacketBroadcaster;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;

import java.util.List;
import java.util.UUID;

/**
 * Request the server to generate or update a map for the specified network and area.
 * The server will respond with a MapResponseS2C packet containing the map data.
 */
public record MapRequestC2S(MapInfo mapInfo) implements AbstractPacket {

    public MapRequestC2S(FriendlyByteBuf buf) {
        this(MapInfo.readFromBuffer(buf));
    }

    public void write(FriendlyByteBuf buf) {
        this.mapInfo.writeToBuffer(buf);
    }

    public static MapRequestC2S create(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        return new MapRequestC2S(MapInfo.request(networkId, minBounds, maxBounds, networkNodes));
    }

    public void handle(Level level, Player player) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            UUID networkId = this.mapInfo.networkId();
            
            ServerMapCache.generateMapIfNeeded(networkId, serverLevel)
                .thenAccept(mapInfo -> {
                    if (mapInfo != null) {
                        PacketBroadcaster.S2C.sendToPlayer(new MapResponseS2C(mapInfo), serverPlayer);
                    }
                });
        }
    }
}
