package net.rasanovum.viaromana.variables;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;

public class PlayerVariableAccessor {

    // Getters
    public boolean isChartingPath(Entity entity) { return getVars(entity).ChartingPath; }
    public double getFadeAmount(Entity entity) { return getVars(entity).FadeAmount; }
    public boolean isFadeIncrease(Entity entity) { return getVars(entity).FadeIncrease; }
    public BlockPos getLastNodePos(Entity entity) { return getVars(entity).lastNodePos; }
    public boolean hasReceivedTutorial(Entity entity) { return getVars(entity).ReceivedTutorial; }

    // Setters
    public void setChartingPath(Entity entity, boolean value) { getVars(entity).ChartingPath = value; }
    public void setFadeAmount(Entity entity, double value) { getVars(entity).FadeAmount = value; }
    public void setFadeIncrease(Entity entity, boolean value) { getVars(entity).FadeIncrease = value; }
    public void setLastNodePos(Entity entity, BlockPos value) { getVars(entity).lastNodePos = value; }
    public void setReceivedTutorial(Entity entity, boolean value) { getVars(entity).ReceivedTutorial = value; }

    public void sync(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            getVars(player).syncToClient(player);
        }
    }

    public void save(Entity entity) {
        if (entity instanceof ServerPlayer player && player.getServer() != null) {
            ViaRomanaModVariables.savePlayerVariablesToFile(player.getServer(), player.getUUID().toString(), getVars(player));
        }
    }

    public void syncAndSave(Entity entity) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT && isLocalPlayer(entity)) {
            // Use Dispatcher for client-to-server sending
            commonnetwork.api.Dispatcher.sendToServer(new ViaRomanaModVariables.PlayerVariablesSyncMessage(getVars(entity)));
        } else {
            sync(entity);
            save(entity);
        }
    }
    
    private boolean isLocalPlayer(Entity entity) {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return false;
        }
        return entity.getClass().getSimpleName().equals("LocalPlayer");
    }

    private ViaRomanaModVariables.PlayerVariables getVars(Entity entity) {
        return ViaRomanaModVariables.getPlayerVariables(entity);
    }

    public void savePlayerVariables(Entity entity) {
        save(entity);
    }

    public void loadPlayerVariables(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            ViaRomanaModVariables.loadPlayerVariablesFromFile(player);
        }
    }
}