package net.rasanovum.viaromana.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.network.packets.MapRequestC2S;
import net.rasanovum.viaromana.network.packets.MapResponseS2C;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import commonnetwork.api.Dispatcher;

import net.minecraft.client.gui.MapRenderer;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

/**
 * Converts server MapInfo into GPU textures on demand.
 */
public class MapClient {

    /**
     * Represents a GPU texture created from MapInfo.
     */
    public static class MapTexture implements AutoCloseable {
        public final MapInfo mapInfo;
        public final ResourceLocation textureLocation;
        public final DynamicTexture dynamicTexture;
        public final NativeImage nativeImage;

        public MapTexture(MapInfo mapInfo, ResourceLocation textureLocation, DynamicTexture dynamicTexture, NativeImage nativeImage) {
            this.mapInfo = mapInfo;
            this.textureLocation = textureLocation;
            this.dynamicTexture = dynamicTexture;
            this.nativeImage = nativeImage;
        }

        @Override
        public void close() {
            if (dynamicTexture != null) dynamicTexture.close();
        }
    }

    /**
     * Requests a map from the server. Returns a CompletableFuture that will complete
     * with the MapInfo when received, or null if the request fails.
     */
    public static CompletableFuture<MapInfo> requestMap(UUID networkId, BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {        
        CompletableFuture<MapInfo> future = new CompletableFuture<>();
        
        pendingRequest = future;
        
        MapRequestC2S packet = MapRequestC2S.create(networkId, minBounds, maxBounds, networkNodes);
        Dispatcher.sendToServer(packet);
        
        return future;
    }
    
    private static CompletableFuture<MapInfo> pendingRequest = null;

    public static void handleMapResponse(MapResponseS2C packet) {
        handleMapInfo(packet.getMapInfo());
    }
    
    public static void handleMapInfo(MapInfo mapInfo) {        
        if (pendingRequest != null) {
            pendingRequest.complete(mapInfo);
            pendingRequest = null;
        }
    }

    public static MapTexture createTexture(MapInfo mapInfo) {
        if (mapInfo == null || !mapInfo.hasImageData()) {
            return null;
        }

        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(mapInfo.pngData()));
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = Minecraft.getInstance().getTextureManager().register("map_network_" + mapInfo.networkId(), texture);

            ViaRomana.LOGGER.debug("Created GPU texture for network {} map", mapInfo.networkId());
            return new MapTexture(mapInfo, location, texture, image);
            
        } catch (IOException e) {
            ViaRomana.LOGGER.error("MapClient: Failed to create texture for network {}: {}", mapInfo.networkId(), e.getMessage());
            return null;
        }
    }
}