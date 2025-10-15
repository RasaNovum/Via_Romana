package net.rasanovum.viaromana.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RenderUtil {
    /**
     * Finds a suitable Y position for rendering an effect near a given BlockPos,
     * accounting for the actual height of blocks based on their collision shapes.
     * It searches a small vertical range around the position.
     *
     * @param level The client level.
     * @param pos   The position to check around.
     * @param offsetY Additional Y offset to apply.
     * @return A suitable Y-coordinate.
     */
    //? if fabric
    @net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    //? if neoforge || forge
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)*/
    @SuppressWarnings("deprecation")
    public static double findSuitableYPosition(ClientLevel level, BlockPos pos, float offsetY) {
        if (level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false) == null) {
            return pos.getY();
        }

        for (int yOffset = 1; yOffset >= -3; yOffset--) {
            BlockPos checkPos = pos.offset(0, yOffset, 0);
            BlockPos belowPos = checkPos.below();
            BlockState belowState = level.getBlockState(belowPos);
            
            if (belowState.isSolid()) {
                VoxelShape shape = belowState.getCollisionShape(level, belowPos, CollisionContext.empty());
                if (!shape.isEmpty()) {
                    double blockTopY = belowPos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                    return blockTopY + offsetY;
                } else {
                    return checkPos.getY() + offsetY;
                }
            }
        }
        return pos.getY() + offsetY;
    }
}
