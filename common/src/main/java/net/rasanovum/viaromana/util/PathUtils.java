package net.rasanovum.viaromana.util;

import net.rasanovum.viaromana.core.SignCheck;
import net.rasanovum.viaromana.variables.VariableAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.ArrayList;
import java.util.Base64;

public class PathUtils {
    public static ArrayList<Object> decodePathData(String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }
        String decodedData = "";
        ArrayList<Object> outputArrayList = new ArrayList<>();
        try {
            decodedData = new String(Base64.getDecoder().decode(input));
            String[] parts = decodedData.split("%");

            for (int i = 0; i < parts.length; i++) { // Convert string coordinates to Double objects
                if (i <= 5) {
                    try {
                        outputArrayList.add(Double.parseDouble(parts[i]));
                    } catch (NumberFormatException e) {
                        outputArrayList.add(parts[i]);
                    }
                } else {
                    outputArrayList.add(parts[i]);
                }
            }
        } catch (IllegalArgumentException e) {
            return new ArrayList<>();
        }

        if (outputArrayList.size() == 6) {
            outputArrayList.add("linkedSign");
        }
        
        return outputArrayList;
    }

    // Encodes path data into a string
    public static String encodePathData(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null)
            return "";
        String recordedData = "";
        String encodedData = "";
        recordedData = (Math.floor(entity.getX()) + 0.5) + "%" + entity.getY() + "%" + (Math.floor(entity.getZ()) + 0.5) + "%" + x + "%" + y + "%" + z + "%" + SignCheck.getTarget(world, x, y, z, entity);
        encodedData = Base64.getEncoder().encodeToString(recordedData.getBytes());
        return encodedData;
    }

    // Calculates the distance between the entity's current position and the last recorded node
    public static double calculateNodeDistance(Entity entity) {
        if (entity == null)
            return 0;
        double lastNodeDistance = 0;
        lastNodeDistance = Math.sqrt(
            Math.pow(entity.getX() - VariableAccess.playerVariables.getLastNodeX(entity), 2) + 
            Math.pow(entity.getZ() - VariableAccess.playerVariables.getLastNodeZ(entity), 2)
        );
        return lastNodeDistance;
    }

    // Calculates the distance between the entity's current position and the starting position stored in the path data
    public static double calculatePathStartDistance(Entity entity) {
        if (entity == null)
            return 0;
        String pathStoredData = VariableAccess.playerVariables.getPathData(entity);
        List<Object> pathStoredArrayList = decodePathData(pathStoredData);
        
        if (pathStoredArrayList.isEmpty() || pathStoredArrayList.size() < 3) {
            return 0;
        }
        
        double startX = 0;
        double startZ = 0;
        
        try {
            if (pathStoredArrayList.get(0) instanceof Double) {
                startX = (Double) pathStoredArrayList.get(0);
            }
            if (pathStoredArrayList.get(2) instanceof Double) {
                startZ = (Double) pathStoredArrayList.get(2);
            }
        } catch (Exception e) {
            return 0;
        }
        
        return Math.sqrt(Math.pow(startX - entity.getX(), 2) + Math.pow(startZ - entity.getZ(), 2));
    }

    // Checks if a block at the given coordinates is a valid path block
    public static boolean isBlockValidPath(LevelAccessor world, double x, double y, double z) {
        BlockState targetBlock = Blocks.AIR.defaultBlockState();
        if (world.isEmptyBlock(BlockPos.containing(x, y, z))) {
            return false;
        }
        targetBlock = (world.getBlockState(BlockPos.containing(x, y, z)));
        
        // Check if block matches any valid tag
        List<Object> validTagList = VariableAccess.mapVariables.getValidTagList();
        for (int index0 = 0; index0 < validTagList.size(); index0++) {
            String tagString = validTagList.get(index0) instanceof String s ? s : "";
            if (targetBlock.is(TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace(tagString.toLowerCase(java.util.Locale.ENGLISH))))) {
                return true;
            }
        }
        
        // Check if block ID matches any valid block ID
        List<Object> validBlockList = VariableAccess.mapVariables.getValidBlockList(world);
        for (int index1 = 0; index1 < validBlockList.size(); index1++) {
            String blockString = validBlockList.get(index1) instanceof String s ? s : "";
            if ((BuiltInRegistries.BLOCK.getKey(targetBlock.getBlock()).toString()).equals(blockString)) {
                return true;
            }
        }
        
        // Check if block ID contains any valid string
        List<Object> validStringList = VariableAccess.mapVariables.getValidStringList(world);
        for (int index2 = 0; index2 < validStringList.size(); index2++) {
            String validString = validStringList.get(index2) instanceof String s ? s : "";
            if (!validString.isEmpty() && 
                (BuiltInRegistries.BLOCK.getKey(targetBlock.getBlock()).toString()).contains(validString)) {
                return true;
            }
        }
        return false;
    }

    // Calculates the infrastructure quality around an entity based on the density of valid path blocks
    public static double calculateInfrastructureQuality(LevelAccessor world, Entity entity) {
        if (entity == null)
            return 0;
        double surfaceLevel = 0;
        double sx = 0;
        double sz = 0;
        double pathQuality = 0;
        double loopAmount = 0;
        double sy = 0;
        double checkX = 0;
        double checkY = 0;
        double checkZ = 0;
        
        surfaceLevel = entity.getY();
        if (!entity.onGround()) {
            while (world.isEmptyBlock(BlockPos.containing(entity.getX(), surfaceLevel, entity.getZ()))) {
                surfaceLevel = surfaceLevel - 1;
                if (entity.getY() - surfaceLevel >= 10) {
                    return 0;
                }
            }
        }
        
        // Ensure check radius is odd
        double checkRadius = VariableAccess.mapVariables.getInfrastructureCheckRadius(world);
        if (checkRadius % 2 == 0) {
            checkRadius = checkRadius + 1;
            VariableAccess.mapVariables.setInfrastructureCheckRadius(world, checkRadius);
            VariableAccess.mapVariables.markAndSync(world);
        }
        
        // Calculate loop parameters
        loopAmount = checkRadius * 2 + 1;
        sx = 0 - checkRadius;
        
        // Loop through all blocks in check radius
        for (int index1 = 0; index1 < (int) loopAmount; index1++) {
            sy = 0 - checkRadius;
            for (int index2 = 0; index2 < (int) loopAmount; index2++) {
                sz = 0 - checkRadius;
                for (int index3 = 0; index3 < (int) loopAmount; index3++) {
                    checkX = Math.floor(entity.getX() + sx);
                    checkY = Math.ceil(surfaceLevel) - 1 + sy;
                    checkZ = Math.floor(entity.getZ() + sz);
                    if (isBlockValidPath(world, checkX, checkY, checkZ)) {
                        pathQuality = pathQuality + 1;
                    }
                    sz = sz + 1;
                }
                sy = sy + 1;
            }
            sx = sx + 1;
        }
        
        // Calculate and return quality percentage
        return pathQuality / Math.pow(checkRadius * 2 + 1, 2);
    }
}
