package net.rasanovum.viaromana.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.core.BlockPos;
import net.rasanovum.viaromana.CommonConfig;
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
     * Represents raw map layer images created from MapInfo.
     * The actual texture baking and stacking is handled by MapRenderer.
     */
    public record MapTexture(MapInfo mapInfo, NativeImage biomeImage, NativeImage chunkImage) implements AutoCloseable {
        @Override
        public void close() {
            if (biomeImage != null) biomeImage.close(); //TODO: Look into caching biomeImage client-side and re-using for stack if network isn't invalidated
            if (chunkImage != null) chunkImage.close();
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

            long createImageStart = System.nanoTime();

            NativeImage biomeImage = new NativeImage(width, height, false);
            NativeImage chunkImage = new NativeImage(width, height, false);

            long createImageTime = System.nanoTime() - createImageStart;
            long convertStart = System.nanoTime();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = x + y * width;
                    int packedId = biomePixels[idx] & 0xFF;
                    int argb = net.minecraft.world.level.material.MapColor.getColorFromPackedId(packedId);
                    biomeImage.setPixelRGBA(x, y, argb);
                }
            }

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int idx = x + y * width;
                    int packedId = chunkPixels[idx] & 0xFF;
                    if (packedId != 0) {
                        int argb = net.minecraft.world.level.material.MapColor.getColorFromPackedId(packedId);
                        chunkImage.setPixelRGBA(x, y, argb);
                    } else {
                        chunkImage.setPixelRGBA(x, y, 0);
                    }
                }
            }
            long convertTime = System.nanoTime() - convertStart;

            long totalTime = System.nanoTime() - startTime;

            if (CommonConfig.logging_enum.ordinal() > 1) ViaRomana.LOGGER.info("MapClient: Created raw map images for network {}: total={}ms, create={}ms, convert={}ms, dimensions={}x{}, pixels={}",
                    mapInfo.networkId(), totalTime / 1_000_000.0, createImageTime / 1_000_000.0,
                    convertTime / 1_000_000.0, width, height, biomePixels.length);

            return new MapTexture(mapInfo, biomeImage, chunkImage);

        } catch (Exception e) {
            ViaRomana.LOGGER.error("MapClient: Failed to create texture for network {}: {}", mapInfo.networkId(), e.getMessage());
            return null;
        }
    }
}