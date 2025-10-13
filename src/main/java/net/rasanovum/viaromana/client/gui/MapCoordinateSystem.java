package net.rasanovum.viaromana.client.gui;

import net.minecraft.core.BlockPos;
import java.awt.Point;

//? if fabric
@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
//? if neoforge
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)*/
public class MapCoordinateSystem {
    // World bounds
    private final int worldStartX;
    private final int worldStartZ;
    
    // Texture properties
    private final int bakedTextureWidth;
    private final int bakedTextureHeight;
    private final int bakeScaleFactor;
    private final int mapOffsetX;
    private final int mapOffsetY;
    
    // Screen rendering properties
    private int screenX;
    private int screenY;
    private int screenWidth;
    private int screenHeight;
    private float renderScale;
    
    public MapCoordinateSystem(BlockPos worldMinBounds, BlockPos worldMaxBounds, 
                              int originalTextureWidth, int originalTextureHeight,
                              int bakedTextureWidth, int bakedTextureHeight,
                              int bakeScaleFactor) {
        this.worldStartX = worldMinBounds.getX();
        this.worldStartZ = worldMinBounds.getZ();
        
        this.bakedTextureWidth = bakedTextureWidth;
        this.bakedTextureHeight = bakedTextureHeight;
        this.bakeScaleFactor = bakeScaleFactor;
        
        this.mapOffsetX = (bakedTextureWidth - originalTextureWidth) / 2;
        this.mapOffsetY = (bakedTextureHeight - originalTextureHeight) / 2;
    }
    
    public MapRenderInfo updateAndGetRenderInfo(int availableScreenWidth, int availableScreenHeight) {
        float scaleX = (float) availableScreenWidth / bakedTextureWidth;
        float scaleY = (float) availableScreenHeight / bakedTextureHeight;
        this.renderScale = Math.min(scaleX, scaleY);
        
        this.screenWidth = (int) (bakedTextureWidth * renderScale);
        this.screenHeight = (int) (bakedTextureHeight * renderScale);
        
        // Center within the available area
        this.screenX = (availableScreenWidth - screenWidth) / 2;
        this.screenY = (availableScreenHeight - screenHeight) / 2;

        return new MapRenderInfo(screenX, screenY, screenWidth, screenHeight, renderScale);
    }
    
    public Point worldToScreen(BlockPos worldPos) {
        float scaledTextureX = (float) (worldPos.getX() - worldStartX) / bakeScaleFactor;
        float scaledTextureZ = (float) (worldPos.getZ() - worldStartZ) / bakeScaleFactor;
        
        float bakedTextureX = scaledTextureX + mapOffsetX;
        float bakedTextureZ = scaledTextureZ + mapOffsetY;
        
        int screenPosX = screenX + (int) (bakedTextureX * renderScale);
        int screenPosY = screenY + (int) (bakedTextureZ * renderScale);
        
        return new Point(screenPosX, screenPosY);
    }
    
    public BlockPos screenToWorld(int screenPosX, int screenPosY) {
        float bakedTextureX = (screenPosX - screenX) / renderScale;
        float bakedTextureZ = (screenPosY - screenY) / renderScale;
        
        float scaledTextureX = bakedTextureX - mapOffsetX;
        float scaledTextureZ = bakedTextureZ - mapOffsetY;
        
        int textureX = (int) (scaledTextureX * bakeScaleFactor);
        int textureZ = (int) (scaledTextureZ * bakeScaleFactor);
        
        int worldX = textureX + worldStartX;
        int worldZ = textureZ + worldStartZ;
        
        return new BlockPos(worldX, 0, worldZ);
    }
    
    public boolean isWithinMapBounds(int screenPosX, int screenPosY) {
        return screenPosX >= screenX && screenPosX < screenX + screenWidth &&
               screenPosY >= screenY && screenPosY < screenY + screenHeight;
    }
    
    public float getRenderScale() { return renderScale; }
    
    public static class MapRenderInfo {
        public final int x, y, width, height;
        public final float scale;
        
        public MapRenderInfo(int x, int y, int width, int height, float scale) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.scale = scale;
        }
    }
}
