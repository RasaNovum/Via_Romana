package net.rasanovum.viaromana.forge.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.forge.ViaRomanaMod;

@OnlyIn(Dist.CLIENT)
public class ViaRomanaModClientPacketHandler {

    public static void sendPlayerVariablesToServer(ViaRomanaModVariables.PlayerVariables data) {
        ViaRomanaModVariables.PlayerVariablesSyncMessage message = new ViaRomanaModVariables.PlayerVariablesSyncMessage(data);
        ViaRomanaModPacketHandler.INSTANCE.sendToServer(message);
        ViaRomanaMod.LOGGER.debug("Sent PlayerVariables to server");
    }
}
