package net.rasanovum.viaromana.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rasanovum.viaromana.tags.TagGenerator;
import net.rasanovum.viaromana.util.PathUtils;

import java.util.ArrayList;
import java.util.List;

public class RenderUtil {
    @net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    public static double findSuitableYPosition(ClientLevel level, BlockPos pos, float offsetY) {
        return findSuitableYPosition(level, pos, offsetY, 5, 5);
    }

    @net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    public static double findSuitableYPosition(ClientLevel level, BlockPos pos, float offsetY, int maxUp, int maxDown) {
        if (level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, false) == null) {
            return pos.getY() + offsetY;
        }

        for (int yOffset = maxUp + 1; yOffset >= -maxDown; yOffset--) {
            BlockPos checkPos = pos.offset(0, yOffset, 0);
            BlockPos belowPos = checkPos.below();
            BlockState belowState = level.getBlockState(belowPos);

            if ((belowState.isSolid() || PathUtils.isBlockValidPath(level, belowPos)) && !PathUtils.isBlockLeaveBlock(level, belowPos)) {
                VoxelShape shape = belowState.getCollisionShape(level, belowPos, CollisionContext.empty());
                if (!shape.isEmpty()) {
                    double blockTopY = belowPos.getY() + shape.max(Direction.Axis.Y);
                    return blockTopY + offsetY;
                } else {
                    return checkPos.getY() + offsetY;
                }
            }
        }

        return pos.getY() + offsetY;
    }
}
