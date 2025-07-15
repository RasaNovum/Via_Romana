package net.rasanovum.viaromana.fabric.network;

import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.network.NetworkHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.rasanovum.viaromana.fabric.ViaRomanaMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;

class FabricNetworkHandler implements NetworkHandler {
    @Override
    public void sendToPlayer(ServerPlayer player, Object message) {
        if (player == null) return;

        ResourceLocation packetId = null;
        FriendlyByteBuf buf = PacketByteBufs.create();

        if (message instanceof ViaRomanaModVariables.PlayerVariablesSyncMessage syncMessage) {
            packetId = ViaRomanaModPacketHandler.PLAYER_VARIABLES_SYNC_S2C;
            syncMessage.write(buf);
        } else if (message instanceof ViaRomanaModVariables.SavedDataSyncMessage syncMessage) {
            packetId = ViaRomanaModPacketHandler.GLOBAL_VARIABLES_SYNC_S2C;
            syncMessage.write(buf);
        } else {
            ViaRomanaMod.LOGGER.error("Attempted to send unknown message type via FabricNetworkHandler: {}", message.getClass().getName());
            return;
        }

        if (packetId != null) {
            ServerPlayNetworking.send(player, packetId, buf);
        } else {
             ViaRomanaMod.LOGGER.error("Packet ID was null when trying to send message of type {} to {}", message.getClass().getName(), player.getName().getString());
        }
    }
}

public class ViaRomanaModPacketHandler {
    public static final ResourceLocation PLAYER_VARIABLES_SYNC_C2S = new ResourceLocation(ViaRomanaMod.MODID, "player_variables_sync_c2s");
    public static final ResourceLocation PLAYER_VARIABLES_SYNC_S2C = new ResourceLocation(ViaRomanaMod.MODID, "player_variables_sync_s2c");
    public static final ResourceLocation GLOBAL_VARIABLES_SYNC_S2C = new ResourceLocation(ViaRomanaMod.MODID, "global_variables_sync_s2c");

    public static void initialize() {
        ViaRomanaModVariables.networkHandler = new FabricNetworkHandler();
        registerC2SPackets();
    }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(PLAYER_VARIABLES_SYNC_C2S, ViaRomanaModPacketHandler::handlePlayerVariablesSyncC2S);
    }

    private static void handlePlayerVariablesSyncC2S(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        ViaRomanaModVariables.PlayerVariablesSyncMessage message = new ViaRomanaModVariables.PlayerVariablesSyncMessage(buf);
        server.execute(() -> ViaRomanaModVariables.PlayerVariablesSyncMessage.handleServer(message, player));
    }
}
