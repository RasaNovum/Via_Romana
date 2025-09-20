package net.rasanovum.viaromana.variables;

import net.minecraft.world.level.LevelAccessor;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class MapVariableAccessor {
    
    // Getters
    public String getAcceptedBlockIDs(LevelAccessor world) { return getVars(world).AcceptedBlockIDs; }
    public String getAcceptedBlockStrings(LevelAccessor world) { return getVars(world).AcceptedBlockStrings; }
    public String getAcceptedBlockTags(LevelAccessor world) { return getVars(world).AcceptedBlockTags; }
    public String getAcceptedDimensions(LevelAccessor world) { return getVars(world).AcceptedDimensions; }
    public String getAcceptedEntities(LevelAccessor world) { return getVars(world).AcceptedEntities; }
    public double getInfrastructureCheckQuality(LevelAccessor world) { return getVars(world).InfrastructureCheckQuality; }
    public double getInfrastructureCheckRadius(LevelAccessor world) { return getVars(world).InfrastructureCheckRadius; }
    public double getNodeDistanceMaximum(LevelAccessor world) { return getVars(world).NodeDistanceMaximum; }
    public double getNodeDistanceMinimum(LevelAccessor world) { return getVars(world).NodeDistanceMinimum; }
    public double getPathDistanceMaximum(LevelAccessor world) { return getVars(world).PathDistanceMaximum; }
    public double getPathDistanceMinimum(LevelAccessor world) { return getVars(world).PathDistanceMinimum; }
    public double getTravelFatigueCooldown(LevelAccessor world) { return getVars(world).TravelFatigueCooldown; }
    public List<Object> getValidBlockList(LevelAccessor world) { return getVars(world).ValidBlockList; }
    public List<Object> getValidStringList(LevelAccessor world) { return getVars(world).ValidStringList; }
    public List<Object> getValidEntityList(LevelAccessor world) { return getVars(world).ValidEntityList; }
    public List<Object> getValidDimensionList(LevelAccessor world) { return getVars(world).ValidDimensionList; }
    public List<Object> getValidSignList(LevelAccessor world) { return getVars(world).ValidSignList; }
    public List<Object> getValidTagList() { return ViaRomanaModVariables.ValidTagList; }


    // Setters
    public void setAcceptedBlockIDs(LevelAccessor world, String value) { getVars(world).AcceptedBlockIDs = value; }
    public void setAcceptedBlockStrings(LevelAccessor world, String value) { getVars(world).AcceptedBlockStrings = value; }
    public void setAcceptedBlockTags(LevelAccessor world, String value) { getVars(world).AcceptedBlockTags = value; }
    public void setAcceptedDimensions(LevelAccessor world, String value) { getVars(world).AcceptedDimensions = value; }
    public void setAcceptedEntities(LevelAccessor world, String value) { getVars(world).AcceptedEntities = value; }
    public void setInfrastructureCheckQuality(LevelAccessor world, double value) { getVars(world).InfrastructureCheckQuality = value; }
    public void setInfrastructureCheckRadius(LevelAccessor world, double value) { getVars(world).InfrastructureCheckRadius = value; }
    public void setNodeDistanceMaximum(LevelAccessor world, double value) { getVars(world).NodeDistanceMaximum = value; }
    public void setNodeDistanceMinimum(LevelAccessor world, double value) { getVars(world).NodeDistanceMinimum = value; }
    public void setPathDistanceMaximum(LevelAccessor world, double value) { getVars(world).PathDistanceMaximum = value; }
    public void setPathDistanceMinimum(LevelAccessor world, double value) { getVars(world).PathDistanceMinimum = value; }
    public void setTravelFatigueCooldown(LevelAccessor world, double value) { getVars(world).TravelFatigueCooldown = value; }
    public void setValidBlockList(LevelAccessor world, List<Object> value) { getVars(world).ValidBlockList = value; }
    public void setValidStringList(LevelAccessor world, List<Object> value) { getVars(world).ValidStringList = value; }
    public void setValidEntityList(LevelAccessor world, List<Object> value) { getVars(world).ValidEntityList = value; }
    public void setValidDimensionList(LevelAccessor world, List<Object> value) { getVars(world).ValidDimensionList = value; }
    public void setValidSignList(LevelAccessor world, List<Object> value) { getVars(world).ValidSignList = value; }
    public void setValidTagList(List<Object> value) { ViaRomanaModVariables.ValidTagList = value; }

    public void markMapDirty(LevelAccessor world) {
        getVars(world).setDirty();
    }

    public void syncMap(LevelAccessor world) {
        if (!world.isClientSide() && world.getServer() != null) {
            ViaRomanaModVariables.MapVariables vars = getVars(world);
            MinecraftServer server = world.getServer();
            if (server != null) {
                 server.getPlayerList().getPlayers().forEach(vars::syncToPlayer);
            }
        }
    }

    public void markAndSync(LevelAccessor world) {
        markMapDirty(world);
        syncMap(world);
    }

    private ViaRomanaModVariables.MapVariables getVars(LevelAccessor world) {
        return ViaRomanaModVariables.MapVariables.get(world);
    }
}