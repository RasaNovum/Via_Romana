package net.rasanovum.viaromana.variables;

import net.minecraft.world.level.LevelAccessor;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
// import net.minecraft.server.MinecraftServer;

public class WorldVariableAccessor {
    private ViaRomanaModVariables.WorldVariables getVars(LevelAccessor world) {
        return ViaRomanaModVariables.WorldVariables.get(world);
    }

    // private void syncWorld(LevelAccessor world) {
    //     if (!world.isClientSide() && world.getServer() != null) {
    //         ViaRomanaModVariables.WorldVariables vars = getVars(world);
    //         MinecraftServer server = world.getServer();
    //         if (server != null) {
    //              server.getPlayerList().getPlayers().forEach(vars::syncToPlayer);
    //         }
    //     }
    // }

    public void markWorldDirty(LevelAccessor world) {
        getVars(world).setDirty();
    }
}