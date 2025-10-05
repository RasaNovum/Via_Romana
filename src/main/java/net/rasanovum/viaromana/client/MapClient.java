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
        if (mapInfo == null || mapInfo.biomePixels() == null || mapInfo.pixelWidth() == 0 || mapInfo.pixelHeight() == 0) {
            return null;
        }

        try {
            long startTime = System.nanoTime();
            
            int width = mapInfo.pixelWidth();
            int height = mapInfo.pixelHeight();
            byte[] biomePixels = mapInfo.biomePixels();
            byte[] chunkPixels = mapInfo.chunkPixels() != null ? mapInfo.chunkPixels() : new byte[biomePixels.length];
            
            // Combine pixels: use chunk pixels where available, biome as base
            byte[] combinedPixels = new byte[biomePixels.length];
            for (int i = 0; i < combinedPixels.length; i++) {
                combinedPixels[i] = (chunkPixels[i] != 0) ? chunkPixels[i] : biomePixels[i];
            }
            
            // Create NativeImage and convert raw pixels to ARGB
            long createImageStart = System.nanoTime();
            NativeImage image = new NativeImage(width, height, false);
            long createImageTime = System.nanoTime() - createImageStart;
            
            long convertStart = System.nanoTime();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = x + y * width;
                    int packedId = combinedPixels[idx] & 0xFF;
                    int argb = net.minecraft.world.level.material.MapColor.getColorFromPackedId(packedId);
                    
                    image.setPixelRGBA(x, y, argb);
                }
            }
            long convertTime = System.nanoTime() - convertStart;
            
            long uploadStart = System.nanoTime();
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = Minecraft.getInstance().getTextureManager().register("map_network_" + mapInfo.networkId(), texture);
            long uploadTime = System.nanoTime() - uploadStart;
            
            long totalTime = System.nanoTime() - startTime;

            ViaRomana.LOGGER.info("[PERF-CLIENT] Created GPU texture for network {}: total={}ms, create={}ms, convert={}ms, upload={}ms, dimensions={}x{}, pixels={}", 
                mapInfo.networkId(), totalTime / 1_000_000.0, createImageTime / 1_000_000.0, 
                convertTime / 1_000_000.0, uploadTime / 1_000_000.0, width, height, combinedPixels.length);
            return new MapTexture(mapInfo, location, texture, image);
            
        } catch (Exception e) {
            ViaRomana.LOGGER.error("MapClient: Failed to create texture for network {}: {}", mapInfo.networkId(), e.getMessage());
            return null;
        }
    }
}