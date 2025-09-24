package net.rasanovum.viaromana.util;

import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import net.rasanovum.viaromana.variables.VariableAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

public class PathUtils {
    
    public static float calculateNodeDistance(Entity entity) {
        if (entity == null) return 0;

        BlockPos lastNodePos = VariableAccess.playerVariables.getLastNodePos(entity);
        
        double dx = entity.getX() - lastNodePos.getX();
        double dy = entity.getY() - lastNodePos.getY();
        double dz = entity.getZ() - lastNodePos.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static boolean isBlockValidPath(LevelAccessor world, BlockPos targetBlock) {
        if (world.isEmptyBlock(targetBlock)) return false;

        BlockState blockState = world.getBlockState(targetBlock);

        if (blockState.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse("via_romana:path_block")))) return true;

        return false;
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
        int checkRadius = ViaRomanaConfig.infrastructure_check_radius;
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
}