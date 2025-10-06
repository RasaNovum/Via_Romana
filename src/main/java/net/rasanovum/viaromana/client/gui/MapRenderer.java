package net.rasanovum.viaromana.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.MapClient;
import net.rasanovum.viaromana.util.VersionUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class MapRenderer implements AutoCloseable {
    // region Constants
    private static final int SCREEN_MARGIN = 40;
    private static final int BACKGROUND_TILE_SIZE = 32;
    private static final float MIN_VISIBLE_TILE_FRACTION = 0.4f;
    private static final float GRADIENT_RANDOMNESS = 0.3f;
    private static final float MAP_OPACITY = 0.75f;
    private static final float BIOME_LAYER_OPACITY = 0.35f;
    private static final int BIOME_LAYER_BLUR_RADIUS = 2;

    private static final ResourceLocation[] CORNER_TILES = createTileLocations("corner-", 4);
    private static final ResourceLocation[] EDGE_TILES = createTileLocations("edge-", 24);
    private static final ResourceLocation[] CENTER_TILES = createTileLocations("center-", 4);
    private static final Map<ResourceLocation, NativeImage> tileImageCache = new ConcurrentHashMap<>();

    // region State
    private final long tileSeed;
    private MapClient.MapTexture mapTexture;
    private DynamicTexture bakedDynamicTexture;
    private ResourceLocation bakedTextureLocation;
    private MapCoordinateSystem coordinateSystem;
    private float lastRenderScale = -1.0f;
    private int bakedTextureWidth, bakedTextureHeight;

    public MapRenderer(BlockPos minBounds, BlockPos maxBounds) {
        this.tileSeed = (long) minBounds.hashCode() * 31L + maxBounds.hashCode();
    }

    // region Public API

    public void setMapTexture(MapClient.MapTexture mapTexture) {
        this.mapTexture = mapTexture;
    }

    /**
     * Renders the map and its background onto the screen.
     */
    public void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (mapTexture == null) return;

        int availableWidth = screenWidth - SCREEN_MARGIN;
        int availableHeight = screenHeight - SCREEN_MARGIN;
        float scale = Math.min((float) availableWidth / mapTexture.biomeImage().getWidth(), (float) availableHeight / mapTexture.biomeImage().getHeight());

        // Rebuild texture if it's the first render or if the scale has changed significantly
        if (bakedDynamicTexture == null || Math.abs(scale - lastRenderScale) > 0.1f) {
            rebuildBakedTexture(scale);
        }

        if (bakedDynamicTexture == null || coordinateSystem == null) return;

        MapCoordinateSystem.MapRenderInfo renderInfo = coordinateSystem.updateAndGetRenderInfo(screenWidth, screenHeight);
        guiGraphics.blit(bakedTextureLocation, renderInfo.x, renderInfo.y, renderInfo.width, renderInfo.height, 0, 0, bakedTextureWidth, bakedTextureHeight, bakedTextureWidth, bakedTextureHeight);
    }

    /**
     * Converts world coordinates to screen coordinates.
     */
    public java.awt.Point worldToScreen(BlockPos worldPos, int screenWidth, int screenHeight) {
        if (coordinateSystem == null) return null;
        coordinateSystem.updateAndGetRenderInfo(screenWidth, screenHeight);
        return coordinateSystem.worldToScreen(worldPos);
    }

    @Override
    public void close() {
        if (bakedDynamicTexture != null) {
            bakedDynamicTexture.close();
            bakedDynamicTexture = null;
            bakedTextureLocation = null;
        }
    }

    // region Texture Baking

    /**
     * Rebuilds the combined map and background texture when the scale changes.
     */
    private void rebuildBakedTexture(float scale) {
        lastRenderScale = scale;
        if (bakedDynamicTexture != null) {
            bakedDynamicTexture.close();
        }

        try (NativeImage newBakedImage = bakeLayersOnBackground(mapTexture.biomeImage(), mapTexture.chunkImage())) {
            this.bakedTextureWidth = newBakedImage.getWidth();
            this.bakedTextureHeight = newBakedImage.getHeight();
            bakedDynamicTexture = new DynamicTexture(newBakedImage);
            bakedTextureLocation = Minecraft.getInstance().getTextureManager().register("baked_map", bakedDynamicTexture);
        }

        BlockPos imageMin = new BlockPos(mapTexture.mapInfo().worldMinX(), 0, mapTexture.mapInfo().worldMinZ());
        BlockPos imageMax = new BlockPos(mapTexture.mapInfo().worldMaxX(), 0, mapTexture.mapInfo().worldMaxZ());

        coordinateSystem = new MapCoordinateSystem(
            imageMin, imageMax,
            mapTexture.biomeImage().getWidth(), mapTexture.biomeImage().getHeight(),
            bakedTextureWidth, bakedTextureHeight,
            mapTexture.mapInfo().scaleFactor()
        );
    }

    /**
     * Creates the final texture by stacking chunk onto biome with edge gradient,
     * then stacking that result onto a tiled background with another edge gradient.
     */
    private NativeImage bakeLayersOnBackground(NativeImage biomeImage, NativeImage chunkImage) {
        int mapWidthPx = biomeImage.getWidth();
        int mapHeightPx = biomeImage.getHeight();
        int padding = (int) Math.ceil(MIN_VISIBLE_TILE_FRACTION * BACKGROUND_TILE_SIZE);

        int requiredWidth = mapWidthPx + 2 * padding;
        int requiredHeight = mapHeightPx + 2 * padding;
        int tilesWide = Math.max(3, (requiredWidth + BACKGROUND_TILE_SIZE - 1) / BACKGROUND_TILE_SIZE);
        int tilesHigh = Math.max(3, (requiredHeight + BACKGROUND_TILE_SIZE - 1) / BACKGROUND_TILE_SIZE);

        NativeImage stackedMap = new NativeImage(biomeImage.format(), mapWidthPx, mapHeightPx, false);
        
        // Apply biome layer with reduced opacity to allow background to show through more
        try (NativeImage biomeWithOpacity = applyImageOpacity(biomeImage, BIOME_LAYER_OPACITY)) {
            stackedMap.copyFrom(biomeWithOpacity);
        }

        // Apply blur to biome layer to smooth out pixelation
        try (NativeImage biomeWithBlur = applyImageBlur(stackedMap, BIOME_LAYER_BLUR_RADIUS)) {
            stackedMap.copyFrom(biomeWithBlur);
        }
        
        // Apply edge gradient to chunk layer before blending at full opacity
        try (NativeImage chunkGradient = applyEdgeGradient(chunkImage)) {
            blendImage(stackedMap, chunkGradient, 0, 0, 1.0f);
        }

        // Create tiled background
        NativeImage combined = createTiledBackground(tilesWide, tilesHigh);

        // Apply stacked map onto background with edge gradient and REDUCED opacity
        try (NativeImage mapGradient = applyEdgeGradient(stackedMap)) {
            int mapX = (combined.getWidth() - mapWidthPx) / 2;
            int mapY = (combined.getHeight() - mapHeightPx) / 2;
            blendImage(combined, mapGradient, mapX, mapY, MAP_OPACITY);
        }
        
        stackedMap.close();

        return combined;
    }

    // region Image Manipulation

    private NativeImage createTiledBackground(int tilesWide, int tilesHigh) {
        int bakedWidth = tilesWide * BACKGROUND_TILE_SIZE;
        int bakedHeight = tilesHigh * BACKGROUND_TILE_SIZE;
        NativeImage background = new NativeImage(NativeImage.Format.RGBA, bakedWidth, bakedHeight, true);

        for (int tileY = 0; tileY < tilesHigh; tileY++) {
            for (int tileX = 0; tileX < tilesWide; tileX++) {
                TileInfo tileInfo = getTileInfo(tileX, tileY, tilesWide, tilesHigh);
                NativeImage source = getCachedTileImage(tileInfo.texture());
                if (source != null) {
                    NativeImage rotated = rotateImage(source, tileInfo.rotation());
                    blitScaled(background, rotated, tileX * BACKGROUND_TILE_SIZE, tileY * BACKGROUND_TILE_SIZE, BACKGROUND_TILE_SIZE, BACKGROUND_TILE_SIZE);
                    if (rotated != source) rotated.close();
                }
            }
        }
        return background;
    }

    /**
     * Applies a smooth, randomized gradient to the edges of the map image.
     */
    private NativeImage applyEdgeGradient(NativeImage mapImage) {
        int width = mapImage.getWidth();
        int height = mapImage.getHeight();
        float[][] distanceMap = calculateDistanceMap(mapImage);
        int gradientDist = Math.max(2, Math.min(20, Math.min(width, height) / 20));

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
                    int adjustedPixel = applyOpacity(pixel, gradient);
                    result.setPixelRGBA(x, y, adjustedPixel);
                }
            }
        }
        return result;
    }

    /**
     * Calculates a distance field from the transparent/border pixels of an image.
     */
    private float[][] calculateDistanceMap(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        float[][] distanceMap = new float[width][height];
        final float maxDist = (float) width + height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean isTransparent = ((image.getPixelRGBA(x, y) >>> 24) & 0xFF) < 10;
                boolean isBorder = x == 0 || y == 0 || x == width - 1 || y == height - 1;
                distanceMap[x][y] = (isTransparent || isBorder) ? 0 : maxDist;
            }
        }

        // Two-pass distance transform
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
        return distanceMap;
    }

    /**
     * Blends foreground onto background with the specified opacity.
     * @param opacity 1.0f for full opacity, lower values for transparency
     */
    private void blendImage(NativeImage background, NativeImage foreground, int startX, int startY, float opacity) {
        for (int px = 0; px < foreground.getWidth(); px++) {
            for (int py = 0; py < foreground.getHeight(); py++) {
                int targetX = startX + px;
                int targetY = startY + py;

                if (targetX >= 0 && targetX < background.getWidth() && targetY >= 0 && targetY < background.getHeight()) {
                    int bgPixel = background.getPixelRGBA(targetX, targetY);
                    int fgPixel = applyOpacity(foreground.getPixelRGBA(px, py), opacity);
                    background.setPixelRGBA(targetX, targetY, blendOver(bgPixel, fgPixel));
                }
            }
        }
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
                int sx = (int) (dx * (float) src.getWidth() / tw);
                int sy = (int) (dy * (float) src.getHeight() / th);
                if (dx + tx < target.getWidth() && dy + ty < target.getHeight()) {
                    target.setPixelRGBA(dx + tx, dy + ty, src.getPixelRGBA(sx, sy));
                }
            }
        }
    }

    // region Helpers

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

    private static int applyOpacity(int color, float opacity) {
        int alpha = (int) (((color >>> 24) & 0xFF) * opacity);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Creates a new image with opacity applied.
     */
    private NativeImage applyImageOpacity(NativeImage source, float opacity) {
        int width = source.getWidth();
        int height = source.getHeight();
        NativeImage result = new NativeImage(source.format(), width, height, false);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getPixelRGBA(x, y);
                int adjustedPixel = applyOpacity(pixel, opacity);
                result.setPixelRGBA(x, y, adjustedPixel);
            }
        }
        
        return result;
    }

    /**
     * Creates a new image with a box blur applied.
     */
    private NativeImage applyImageBlur(NativeImage source, int blurRadius) {
        if (blurRadius < 1) return source;
        
        NativeImage temp = applyBlurPass(source, blurRadius, true);
        NativeImage result = applyBlurPass(temp, blurRadius, false);
        
        temp.close();
        return result;
    }

    private NativeImage applyBlurPass(NativeImage source, int blurRadius, boolean horizontal) {
        int width = source.getWidth();
        int height = source.getHeight();
        NativeImage result = new NativeImage(source.format(), width, height, false);
        int kernelSize = 2 * blurRadius + 1;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sumA = 0, sumR = 0, sumG = 0, sumB = 0;
                
                for (int k = -blurRadius; k <= blurRadius; k++) {
                    int sampleX, sampleY;
                    if (horizontal) {
                        sampleX = Math.min(width - 1, Math.max(0, x + k));
                        sampleY = y;
                    } else {
                        sampleX = x;
                        sampleY = Math.min(height - 1, Math.max(0, y + k));
                    }
                    
                    int pixel = source.getPixelRGBA(sampleX, sampleY);
                    sumA += (pixel >> 24) & 0xFF;
                    sumR += (pixel >> 16) & 0xFF;
                    sumG += (pixel >> 8) & 0xFF;
                    sumB += pixel & 0xFF;
                }
                
                int avgA = sumA / kernelSize;
                int avgR = sumR / kernelSize;
                int avgG = sumG / kernelSize;
                int avgB = sumB / kernelSize;
                result.setPixelRGBA(x, y, (avgA << 24) | (avgR << 16) | (avgG << 8) | avgB);
            }
        }
        
        return result;
    }

    private int blendOver(int bg, int fg) {
        int fgA = (fg >> 24) & 0xFF;
        if (fgA == 0) return bg;
        int bgA = (bg >> 24) & 0xFF;
        int invA = 255 - fgA;
        int outR = (((fg >> 16) & 0xFF) * fgA + ((bg >> 16) & 0xFF) * invA) / 255;
        int outG = (((fg >> 8) & 0xFF) * fgA + ((bg >> 8) & 0xFF) * invA) / 255;
        int outB = ((fg & 0xFF) * fgA + (bg & 0xFF) * invA) / 255;
        int outA = fgA + (bgA * invA) / 255;
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    private static ResourceLocation[] createTileLocations(String prefix, int count) {
        String basePath = "via_romana:textures/screens/background_tile/";
        return IntStream.range(1, count + 1)
            .mapToObj(i -> VersionUtils.getLocation(basePath + prefix + i + ".png"))
            .toArray(ResourceLocation[]::new);
    }

    // region Cache Management

    public static void clearCache() {
        ViaRomana.LOGGER.info("Clearing MapRenderer tile image cache ({} entries)", tileImageCache.size());
        tileImageCache.values().forEach(NativeImage::close);
        tileImageCache.clear();
    }

    // region Records

    private record TileInfo(ResourceLocation texture, int rotation) {}
    private record MapKey(BlockPos min, BlockPos max) {}

    /**
     * A holder class to manage the lifecycle of a single MapRenderer instance.
     */
    public static class Holder {
        private MapRenderer current;
        private MapKey lastKey;

        public MapRenderer getOrCreate(BlockPos minBounds, BlockPos maxBounds) {
            MapKey currentKey = new MapKey(minBounds, maxBounds);
            if (current == null || !currentKey.equals(lastKey)) {
                if (current != null) current.close();
                current = new MapRenderer(minBounds, maxBounds);
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