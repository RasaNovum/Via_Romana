package net.rasanovum.viaromana.forge.network;

import net.rasanovum.viaromana.forge.ViaRomanaMod;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.network.NetworkHandler;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ViaRomanaModPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    
    @SuppressWarnings("removal")
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(ViaRomanaMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    public static int getNextId() {
        return packetId++;
    }
    
    private static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer) {
        INSTANCE.registerMessage(getNextId(), messageType, encoder, decoder, messageConsumer);
    }
    
    public static void initialize() {
        ViaRomanaMod.LOGGER.info("Initializing network handler");
        
        registerMessage(ViaRomanaModVariables.PlayerVariablesSyncMessage.class,
                ViaRomanaModVariables.PlayerVariablesSyncMessage::write,
                ViaRomanaModVariables.PlayerVariablesSyncMessage::new,
                (message, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        if (context.getDirection().getReceptionSide().isServer()) {
                            ServerPlayer player = context.getSender();
                            if (player != null) {
                                ViaRomanaModVariables.PlayerVariablesSyncMessage.handleServer(message, player);
                            }
                        } else {
                            ViaRomanaModVariables.PlayerVariablesSyncMessage.handleClient(message);
                        }
                    });
                    context.setPacketHandled(true);
                });

        registerMessage(ViaRomanaModVariables.SavedDataSyncMessage.class,
                ViaRomanaModVariables.SavedDataSyncMessage::write,
                ViaRomanaModVariables.SavedDataSyncMessage::new,
                (message, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        if (context.getDirection().getReceptionSide().isClient()) {
                            ViaRomanaModVariables.SavedDataSyncMessage.handleClient(message);
                        }
                    });
                    context.setPacketHandled(true);
                });
        
        ViaRomanaModVariables.networkHandler = new ForgeNetworkHandler();
    }
    

    static class ForgeNetworkHandler implements NetworkHandler {
        @Override
        public void sendToPlayer(ServerPlayer player, Object message) {
            if (player == null) return;
            
            if (message instanceof ViaRomanaModVariables.PlayerVariablesSyncMessage syncMessage) {
                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), syncMessage);
            } else if (message instanceof ViaRomanaModVariables.SavedDataSyncMessage syncMessage) {
                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), syncMessage);
            } else {
                ViaRomanaMod.LOGGER.error("Attempted to send unknown message type via ForgeNetworkHandler: {}", 
                        message.getClass().getName());
            }
        }
    }
}