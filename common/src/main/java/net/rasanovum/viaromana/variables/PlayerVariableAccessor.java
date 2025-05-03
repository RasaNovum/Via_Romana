package net.rasanovum.viaromana.variables;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;

public class PlayerVariableAccessor {

    // Getters
    public boolean isChartingPath(Entity entity) { return getVars(entity).ChartingPath; }
    public double getFadeAmount(Entity entity) { return getVars(entity).FadeAmount; }
    public boolean isFadeIncrease(Entity entity) { return getVars(entity).FadeIncrease; }
    public String getLastMessage(Entity entity) { return getVars(entity).lastMessage; }
    public double getLastNodeX(Entity entity) { return getVars(entity).LastNodeX; }
    public double getLastNodeY(Entity entity) { return getVars(entity).LastNodeY; }
    public double getLastNodeZ(Entity entity) { return getVars(entity).LastNodeZ; }
    public String getLastSignPosition(Entity entity) { return getVars(entity).LastSignPosition; }
    public double getLeftClickTimer(Entity entity) { return getVars(entity).LeftClickTimer; }
    public double getMessageCooldown(Entity entity) { return getVars(entity).MessageCooldown; }
    public String getPathData(Entity entity) { return getVars(entity).PathData; }
    public boolean hasReceivedTutorial(Entity entity) { return getVars(entity).ReceivedTutorial; }
    public double getTitleCooldown(Entity entity) { return getVars(entity).TitleCooldown; }
    public String getTitleString(Entity entity) { return getVars(entity).TitleString; }
    public double getToastTimer(Entity entity) { return getVars(entity).ToastTimer; }
    public boolean isAwaitingToast(Entity entity) { return getVars(entity).AwaitingToast; }

    // Setters
    public void setChartingPath(Entity entity, boolean value) { getVars(entity).ChartingPath = value; }
    public void setFadeAmount(Entity entity, double value) { getVars(entity).FadeAmount = value; }
    public void setFadeIncrease(Entity entity, boolean value) { getVars(entity).FadeIncrease = value; }
    public void setLastMessage(Entity entity, String value) { getVars(entity).lastMessage = value; }
    public void setLastNodeX(Entity entity, double value) { getVars(entity).LastNodeX = value; }
    public void setLastNodeY(Entity entity, double value) { getVars(entity).LastNodeY = value; }
    public void setLastNodeZ(Entity entity, double value) { getVars(entity).LastNodeZ = value; }
    public void setLastSignPosition(Entity entity, String value) { getVars(entity).LastSignPosition = value; }
    public void setLeftClickTimer(Entity entity, double value) { getVars(entity).LeftClickTimer = value; }
    public void setMessageCooldown(Entity entity, double value) { getVars(entity).MessageCooldown = value; }
    public void setPathData(Entity entity, String value) { getVars(entity).PathData = value; }
    public void setReceivedTutorial(Entity entity, boolean value) { getVars(entity).ReceivedTutorial = value; }
    public void setTitleCooldown(Entity entity, double value) { getVars(entity).TitleCooldown = value; }
    public void setTitleString(Entity entity, String value) { getVars(entity).TitleString = value; }
    public void setToastTimer(Entity entity, double value) { getVars(entity).ToastTimer = value; }
    public void setAwaitingToast(Entity entity, boolean value) { getVars(entity).AwaitingToast = value; }

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
        sync(entity);
        save(entity);
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