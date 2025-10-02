package net.rasanovum.viaromana.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.MapClient;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C.NodeNetworkInfo;
import net.rasanovum.viaromana.util.VersionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MapRenderer implements AutoCloseable {
    // UI constants
    private static final int SCREEN_MARGIN = 40;
    private static final int BACKGROUND_TILE_SIZE = 32;
    private static final float MIN_VISIBLE_TILE_FRACTION = 0.4f;
    private static final int EDGE_GRADIENT_DISTANCE_SCREEN = 14;
    private static final float GRADIENT_RANDOMNESS = 0.3f;
    private static final float MAP_OPACITY = 0.75f;

    private record TileInfo(ResourceLocation texture, int rotation) {}
    private record MapKey(BlockPos min, BlockPos max) {}

    private static final ResourceLocation[] CORNER_TILES = createTileLocations("corner-", 4);
    private static final ResourceLocation[] EDGE_TILES = createTileLocations("edge-", 24);
    private static final ResourceLocation[] CENTER_TILES = createTileLocations("center-", 4);
    
    private static final Map<ResourceLocation, NativeImage> tileImageCache = new ConcurrentHashMap<>();

    // The requested world-space bounds for this map (no extra padding applied here)
    private final BlockPos requestedMinBounds;
    private final BlockPos requestedMaxBounds;
    private final long tileSeed;
    
    private MapClient.MapTexture mapTexture;
    private DynamicTexture bakedDynamicTexture;
    private ResourceLocation bakedTextureLocation;
    private MapCoordinateSystem coordinateSystem;
    private float lastRenderScale = -1.0f;
    private int bakedTextureWidth, bakedTextureHeight;

    public MapRenderer(BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
        this.requestedMinBounds = minBounds;
        this.requestedMaxBounds = maxBounds;
        this.tileSeed = (long) this.requestedMinBounds.hashCode() * 31L + this.requestedMaxBounds.hashCode();
    }

    private static ResourceLocation[] createTileLocations(String prefix, int count) {
        var locations = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            locations[i] = VersionUtils.getLocation("via_romana:textures/screens/background_tile/" + prefix + (i + 1) + ".png");
        }
        return locations;
    }
    
    public void setMapTexture(MapClient.MapTexture mapTexture) {
        this.mapTexture = mapTexture;
    }
    
    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, Player player) {
        if (mapTexture == null) return;

        int availableWidth = screenWidth - SCREEN_MARGIN;
        int availableHeight = screenHeight - SCREEN_MARGIN;
        
        float scale = Math.min((float) availableWidth / mapTexture.nativeImage.getWidth(), (float) availableHeight / mapTexture.nativeImage.getHeight());

        if (bakedDynamicTexture == null || Math.abs(scale - lastRenderScale) > 0.1f) rebuildBakedTexture(scale);
        if (bakedDynamicTexture == null || coordinateSystem == null) return;

        MapCoordinateSystem.MapRenderInfo renderInfo = coordinateSystem.updateAndGetRenderInfo(availableWidth + SCREEN_MARGIN, availableHeight + SCREEN_MARGIN);
        guiGraphics.blit(bakedTextureLocation, renderInfo.x, renderInfo.y, renderInfo.width, renderInfo.height, 0, 0, bakedTextureWidth, bakedTextureHeight, bakedTextureWidth, bakedTextureHeight);
    }
    
    private void rebuildBakedTexture(float scale) {
        lastRenderScale = scale;
        
        if (bakedDynamicTexture != null) {
            bakedDynamicTexture.close();
        }

        try (NativeImage newBakedImage = bakeBackgroundAndMap(mapTexture.nativeImage, scale)) {
            this.bakedTextureWidth = newBakedImage.getWidth();
            this.bakedTextureHeight = newBakedImage.getHeight();
            
            bakedDynamicTexture = new DynamicTexture(newBakedImage);
            bakedTextureLocation = Minecraft.getInstance().getTextureManager().register("baked_map", bakedDynamicTexture);
        }
        
        BlockPos imageMin = mapTexture.mapInfo.minBounds();
        BlockPos imageMax = mapTexture.mapInfo.maxBounds();
        
        coordinateSystem = new MapCoordinateSystem(
            imageMin, imageMax,
            mapTexture.nativeImage.getWidth(), mapTexture.nativeImage.getHeight(),
            bakedTextureWidth, bakedTextureHeight,
            mapTexture.mapInfo.bakeScaleFactor()
        );
    }
    
    private NativeImage bakeBackgroundAndMap(NativeImage mapImage, float renderScale) {
        ViaRomana.LOGGER.debug("Baking new texture with render scale: {}", renderScale);

        int mapWidthPx = mapImage.getWidth();
        int mapHeightPx = mapImage.getHeight();
        int tileSize = BACKGROUND_TILE_SIZE;

        int requiredWidth = mapWidthPx + (int) Math.ceil(2.0f * MIN_VISIBLE_TILE_FRACTION * tileSize);
        int requiredHeight = mapHeightPx + (int) Math.ceil(2.0f * MIN_VISIBLE_TILE_FRACTION * tileSize);
        int tilesWide = Math.max(3, (requiredWidth + tileSize - 1) / tileSize);
        int tilesHigh = Math.max(3, (requiredHeight + tileSize - 1) / tileSize);
        
        NativeImage combined = createTiledBackground(tilesWide, tilesHigh, tileSize);

        try (NativeImage gradientMap = applyEdgeGradient(mapImage, renderScale)) {
            int mapX = (combined.getWidth() - mapWidthPx) / 2;
            int mapY = (combined.getHeight() - mapHeightPx) / 2;
            blendImage(combined, gradientMap, mapX, mapY);
        }

        return combined;
    }

    private NativeImage createTiledBackground(int tilesWide, int tilesHigh, int tileSize) {
        int bakedWidth = tilesWide * tileSize;
        int bakedHeight = tilesHigh * tileSize;
        NativeImage background = new NativeImage(NativeImage.Format.RGBA, bakedWidth, bakedHeight, true);

        for (int tileY = 0; tileY < tilesHigh; tileY++) {
            for (int tileX = 0; tileX < tilesWide; tileX++) {
                TileInfo tileInfo = getTileInfo(tileX, tileY, tilesWide, tilesHigh);
                NativeImage source = getCachedTileImage(tileInfo.texture());
                if (source != null) {
                    NativeImage rotated = rotateImage(source, tileInfo.rotation());
                    blitScaled(background, rotated, tileX * tileSize, tileY * tileSize, tileSize, tileSize);
                    if (rotated != source) rotated.close();
                }
            }
        }
        return background;
    }
    
    private void blendImage(NativeImage background, NativeImage foreground, int startX, int startY) {
        for (int px = 0; px < foreground.getWidth(); px++) {
            for (int py = 0; py < foreground.getHeight(); py++) {
                int targetX = startX + px;
                int targetY = startY + py;

                if (targetX >= 0 && targetX < background.getWidth() && targetY >= 0 && targetY < background.getHeight()) {
                    int bgPixel = background.getPixelRGBA(targetX, targetY);
                    int fgPixelRaw = foreground.getPixelRGBA(px, py);
                    
                    int fgAlpha = (int) (((fgPixelRaw >>> 24) & 0xFF) * MAP_OPACITY);
                    int fgPixel = (fgAlpha << 24) | (fgPixelRaw & 0x00FFFFFF);

                    background.setPixelRGBA(targetX, targetY, blendOver(bgPixel, fgPixel));
                }
            }
        }
    }

    private NativeImage applyEdgeGradient(NativeImage mapImage, float renderScale) {
        int width = mapImage.getWidth();
        int height = mapImage.getHeight();
        int gradientDist = Math.max(1, (int) Math.ceil(EDGE_GRADIENT_DISTANCE_SCREEN / renderScale));
        
        float[][] distanceMap = new float[width][height];
        final float maxDist = (float) width + height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isTransparent = ((mapImage.getPixelRGBA(x, y) >>> 24) & 0xFF) < 10;
                boolean isBorderPixel = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                
                distanceMap[x][y] = (isTransparent || isBorderPixel) ? 0 : maxDist;
            }
        }

        for (int y = 1; y < height; y++) {
            for (int x = 1; x < width; x++) {
                float minNeighbor = Math.min(distanceMap[x - 1][y], distanceMap[x][y - 1]);
                distanceMap[x][y] = Math.min(distanceMap[x][y], minNeighbor + 1);
            }
        }
        for (int y = height - 2; y >= 0; y--) {
            for (int x = width - 2; x >= 0; x--) {
                float minNeighbor = Math.min(distanceMap[x + 1][y], distanceMap[x][y + 1]);
                distanceMap[x][y] = Math.min(distanceMap[x][y], minNeighbor + 1);
            }
        }

        NativeImage result = new NativeImage(mapImage.format(), width, height, false);
        result.copyFrom(mapImage);
        Random gradientRandom = new Random(tileSeed);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float distance = distanceMap[x][y];

                if (distance < gradientDist) {
                    float randomFactor = 1.0f + (gradientRandom.nextFloat() - 0.5f) * GRADIENT_RANDOMNESS;
                    float gradient = Math.min(1.0f, (distance * randomFactor) / gradientDist);
                    gradient = gradient * gradient * (3.0f - 2.0f * gradient);
                    
                    int pixel = result.getPixelRGBA(x, y);
                    int newAlpha = (int) (((pixel >>> 24) & 0xFF) * gradient);
                    result.setPixelRGBA(x, y, (newAlpha << 24) | (pixel & 0x00FFFFFF));
                }
            }
        }
        return result;
    }

    private TileInfo getTileInfo(int tileX, int tileY, int tilesWide, int tilesHigh) {
        Random tileRandom = new Random(tileSeed + (long) tileY * tilesWide + tileX);

        boolean isTop = tileY == 0, isBottom = tileY == tilesHigh - 1;
        boolean isLeft = tileX == 0, isRight = tileX == tilesWide - 1;

        if (isTop && isLeft) return new TileInfo(CORNER_TILES[0], 0);
        if (isTop && isRight) return new TileInfo(CORNER_TILES[1], 0);
        if (isBottom && isLeft) return new TileInfo(CORNER_TILES[2], 0);
        if (isBottom && isRight) return new TileInfo(CORNER_TILES[3], 0);

        ResourceLocation edgeTexture = EDGE_TILES[tileRandom.nextInt(EDGE_TILES.length)];
        if (isTop) return new TileInfo(edgeTexture, 0);
        if (isRight) return new TileInfo(edgeTexture, 90);
        if (isBottom) return new TileInfo(edgeTexture, 180);
        if (isLeft) return new TileInfo(edgeTexture, 270);
        
        return new TileInfo(CENTER_TILES[tileRandom.nextInt(CENTER_TILES.length)], 0);
    }

    private NativeImage getCachedTileImage(ResourceLocation location) {
        return tileImageCache.computeIfAbsent(location, loc -> {
            try {
                var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(loc);
                if (resourceOpt.isPresent()) {
                    try (var inputStream = resourceOpt.get().open()) {
                        return NativeImage.read(inputStream);
                    }
                }
            } catch (IOException e) {
                ViaRomana.LOGGER.error("Failed to load background tile texture: {}", loc, e);
            }
            return null;
        });
    }

    private NativeImage rotateImage(NativeImage src, int degrees) {
        if (degrees == 0) return src;
        int sw = src.getWidth(), sh = src.getHeight();
        int dw = (degrees == 90 || degrees == 270) ? sh : sw;
        int dh = (degrees == 90 || degrees == 270) ? sw : sh;
        NativeImage dest = new NativeImage(src.format(), dw, dh, false);
        for (int sx = 0; sx < sw; sx++) {
            for (int sy = 0; sy < sh; sy++) {
                int dx, dy;
                switch (degrees) {
                    case 90  -> { dx = sh - 1 - sy; dy = sx; }
                    case 180 -> { dx = sw - 1 - sx; dy = sh - 1 - sy; }
                    case 270 -> { dx = sy; dy = sw - 1 - sx; }
                    default  -> { continue; }
                }
                dest.setPixelRGBA(dx, dy, src.getPixelRGBA(sx, sy));
            }
        }
        return dest;
    }

    private void blitScaled(NativeImage target, NativeImage src, int tx, int ty, int tw, int th) {
        for (int dy = 0; dy < th; dy++) {
            for (int dx = 0; dx < tw; dx++) {
                int sx = (int) (dx * (float)src.getWidth() / tw);
                int sy = (int) (dy * (float)src.getHeight() / th);
                if (dx + tx < target.getWidth() && dy + ty < target.getHeight()) {
                     target.setPixelRGBA(dx + tx, dy + ty, src.getPixelRGBA(sx, sy));
                }
            }
        }
    }

    private int blendOver(int bg, int fg) {
        int fgA = (fg >> 24) & 0xFF;
        if (fgA == 0) return bg;
        int bgA = (bg >> 24) & 0xFF;
        int invA = 255 - fgA;
        int outR = (((fg >> 16) & 0xFF) * fgA + ((bg >> 16) & 0xFF) * invA) / 255;
        int outG = (((fg >> 8)  & 0xFF) * fgA + ((bg >> 8)  & 0xFF) * invA) / 255;
        int outB = ((fg & 0xFF) * fgA + (bg & 0xFF) * invA) / 255;
        int outA = fgA + (bgA * invA) / 255;
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    @Override
    public void close() {
        if (bakedDynamicTexture != null) {
            bakedDynamicTexture.close();
            bakedDynamicTexture = null;
            bakedTextureLocation = null;
        }
    }

    public static void clearCache() {
        ViaRomana.LOGGER.info("Clearing MapRenderer tile image cache ({} entries)", tileImageCache.size());
        tileImageCache.values().forEach(NativeImage::close);
        tileImageCache.clear();
    }
    
    public java.awt.Point worldToScreen(BlockPos worldPos, int screenWidth, int screenHeight) {
        if (coordinateSystem == null) return null;
        int availableWidth = screenWidth - SCREEN_MARGIN;
        int availableHeight = screenHeight - SCREEN_MARGIN;
        coordinateSystem.updateAndGetRenderInfo(availableWidth + SCREEN_MARGIN, availableHeight + SCREEN_MARGIN);
        return coordinateSystem.worldToScreen(worldPos);
    }
    
    public static class Holder {
        private MapRenderer current;
        private MapKey lastKey;
        
        public MapRenderer getOrCreate(BlockPos minBounds, BlockPos maxBounds, List<NodeNetworkInfo> networkNodes) {
            MapKey currentKey = new MapKey(minBounds, maxBounds);
            if (current == null || !currentKey.equals(lastKey)) {
                if (current != null) current.close();
                current = new MapRenderer(minBounds, maxBounds, networkNodes);
                lastKey = currentKey;
            }
            return current;
        }
        
        public void close() {
            if (current != null) {
                current.close();
                current = null;
            }
        }
    }
}