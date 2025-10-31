package net.rasanovum.viaromana.util;

import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.tags.TagGenerator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

public class PathUtils {
    public static int CLEARANCE_CHECK = 32;

    public static float calculateNodeDistance(Entity entity) {
        if (!(entity instanceof Player player)) return 0;

        BlockPos lastNodePos = PlayerData.getLastNodePos(player);
        
        double dx = player.getX() - lastNodePos.getX();
        double dy = player.getY() - lastNodePos.getY();
        double dz = player.getZ() - lastNodePos.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static boolean isBlockValidPath(LevelAccessor world, BlockPos targetBlock) {
        if (world.isEmptyBlock(targetBlock)) return false;

        BlockState blockState = world.getBlockState(targetBlock);

        return blockState.is(TagGenerator.PATH_BLOCK_TAG);
    }

    public static boolean isBlockLeaveBlock(LevelAccessor world, BlockPos targetBlock) {
        if (world.isEmptyBlock(targetBlock)) return false;

        BlockState blockState = world.getBlockState(targetBlock);

        return blockState.is(TagGenerator.LEAVES_BLOCK_TAG);
    }

    public static float calculateInfrastructureQuality(LevelAccessor world, Entity entity) {
        if (entity == null) return 0;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(entity.getX(), entity.getY(), entity.getZ());
        if (!entity.onGround()) {
            int startY = mutablePos.getY();
            while (world.isEmptyBlock(mutablePos)) {
                mutablePos.setY(mutablePos.getY() - 1);
                if (startY - mutablePos.getY() >= 4) return 0;
            }
        }

        int surfaceY = mutablePos.getY();
        int checkRadius = CommonConfig.infrastructure_check_radius;
        int pathQuality = 0;
        int entityX = (int) Math.floor(entity.getX());
        int entityZ = (int) Math.floor(entity.getZ());

        for (int dx = -checkRadius; dx <= checkRadius; dx++) {
            for (int dy = -checkRadius; dy <= checkRadius; dy++) {
                for (int dz = -checkRadius; dz <= checkRadius; dz++) {
                    mutablePos.set(entityX + dx, surfaceY + dy, entityZ + dz);
                    if (isBlockValidPath(world, mutablePos)) {
                        pathQuality++;
                    }
                }
            }
        }

        float totalBlocksChecked = (float) Math.pow(checkRadius * 2 + 1, 2);

        return totalBlocksChecked == 0 ? 0 : pathQuality / totalBlocksChecked;
    }

    public static float calculateClearance(LevelAccessor world, Entity entity) {
        if (entity == null) return 0;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(entity.getX(), entity.getY(), entity.getZ());
        if (!entity.onGround()) {
            int startY = mutablePos.getY();
            while (world.isEmptyBlock(mutablePos)) {
                mutablePos.setY(mutablePos.getY() - 1);
                if (startY - mutablePos.getY() >= 4) return 0;
            }
        }

        int surfaceY = mutablePos.getY();
        int clearance = 0;
        int entityX = (int) Math.floor(entity.getX());
        int entityZ = (int) Math.floor(entity.getZ());

        for (int y = surfaceY + 1; y <= surfaceY + CLEARANCE_CHECK; y++) {
            mutablePos.set(entityX, y, entityZ);
            BlockState blockState = world.getBlockState(mutablePos);
            
            if (!world.isEmptyBlock(mutablePos) && !blockState.is(TagGenerator.LEAVES_BLOCK_TAG)) {
                return clearance;
            }
            
            clearance++;
        }

        return 0;
    }
}