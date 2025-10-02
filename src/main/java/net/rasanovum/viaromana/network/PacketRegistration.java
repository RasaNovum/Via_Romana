package net.rasanovum.viaromana.network;

import commonnetwork.api.Network;
import commonnetwork.networking.data.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.ViaRomanaModVariables.PlayerVariablesSyncMessage;
import net.rasanovum.viaromana.network.packets.*;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketRegistration {
    
    //? if <1.21 {
    /*private static <T> void registerPacket(
        Object type,
        Class<T> packetClass,
        BiConsumer<FriendlyByteBuf, T> encoder,
        Function<FriendlyByteBuf, T> decoder,
        Object streamCodec,
        java.util.function.Consumer<PacketContext<T>> handler
    ) {
        Network.registerPacket((net.minecraft.resources.ResourceLocation) type, packetClass, (buf, pkt) -> encoder.accept(pkt, buf), decoder, handler);
    }
    *///?} else {
    private static <T extends CustomPacketPayload> void registerPacket(
        Object type,
        Class<T> packetClass,
        BiConsumer<FriendlyByteBuf, T> encoder,
        Function<FriendlyByteBuf, T> decoder,
        Object streamCodec,
        java.util.function.Consumer<PacketContext<T>> handler
    ) {
        Network.registerPacket((CustomPacketPayload.Type<T>) type, packetClass, (StreamCodec<FriendlyByteBuf, T>) streamCodec, handler);
    }
    //?}
    
    public void init() {
        ViaRomana.LOGGER.info("Registering network packets");
        
        registerPacket(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::encode, PlayerVariablesSyncMessage::decode, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handle);
        registerPacket(PathGraphSyncPacket.TYPE, PathGraphSyncPacket.class, PathGraphSyncPacket::encode, PathGraphSyncPacket::decode, PathGraphSyncPacket.STREAM_CODEC, PathGraphSyncPacket::handle);
        registerPacket(OpenChartingScreenS2C.TYPE, OpenChartingScreenS2C.class, OpenChartingScreenS2C::encode, OpenChartingScreenS2C::decode, OpenChartingScreenS2C.STREAM_CODEC, OpenChartingScreenS2C::handle);
        registerPacket(ChartedPathC2S.TYPE, ChartedPathC2S.class, ChartedPathC2S::encode, ChartedPathC2S::decode, ChartedPathC2S.STREAM_CODEC, ChartedPathC2S::handle);
        registerPacket(MapResponseS2C.TYPE, MapResponseS2C.class, MapResponseS2C::encode, MapResponseS2C::decode, MapResponseS2C.STREAM_CODEC, MapResponseS2C::handle);
        registerPacket(RoutedActionC2S.TYPE, RoutedActionC2S.class, RoutedActionC2S::encode, RoutedActionC2S::decode, RoutedActionC2S.STREAM_CODEC, RoutedActionC2S::handle);
        registerPacket(DestinationRequestC2S.TYPE, DestinationRequestC2S.class, DestinationRequestC2S::encode, DestinationRequestC2S::decode, DestinationRequestC2S.STREAM_CODEC, DestinationRequestC2S::handle);
        registerPacket(DestinationResponseS2C.TYPE, DestinationResponseS2C.class, DestinationResponseS2C::encode, DestinationResponseS2C::decode, DestinationResponseS2C.STREAM_CODEC, DestinationResponseS2C::handle);
        registerPacket(SignLinkRequestC2S.TYPE, SignLinkRequestC2S.class, SignLinkRequestC2S::encode, SignLinkRequestC2S::decode, SignLinkRequestC2S.STREAM_CODEC, SignLinkRequestC2S::handle);
        registerPacket(MapRequestC2S.TYPE, MapRequestC2S.class, MapRequestC2S::encode, MapRequestC2S::decode, MapRequestC2S.STREAM_CODEC, MapRequestC2S::handle);
        registerPacket(OpenWarpBlockScreenS2C.TYPE, OpenWarpBlockScreenS2C.class, OpenWarpBlockScreenS2C::encode, OpenWarpBlockScreenS2C::decode, OpenWarpBlockScreenS2C.STREAM_CODEC, OpenWarpBlockScreenS2C::handle);
        registerPacket(SignValidationRequestC2S.TYPE, SignValidationRequestC2S.class, SignValidationRequestC2S::encode, SignValidationRequestC2S::decode, SignValidationRequestC2S.STREAM_CODEC, SignValidationRequestC2S::handle);
        registerPacket(SignValidationResponseS2C.TYPE, SignValidationResponseS2C.class, SignValidationResponseS2C::encode, SignValidationResponseS2C::decode, SignValidationResponseS2C.STREAM_CODEC, SignValidationResponseS2C::handle);
        registerPacket(TeleportRequestC2S.TYPE, TeleportRequestC2S.class, TeleportRequestC2S::encode, TeleportRequestC2S::decode, TeleportRequestC2S.STREAM_CODEC, TeleportRequestC2S::handle);
        registerPacket(SignUnlinkRequestC2S.TYPE, SignUnlinkRequestC2S.class, SignUnlinkRequestC2S::encode, SignUnlinkRequestC2S::decode, SignUnlinkRequestC2S.STREAM_CODEC, SignUnlinkRequestC2S::handle);
        registerPacket(OpenLinkSignScreenS2C.TYPE, OpenLinkSignScreenS2C.class, OpenLinkSignScreenS2C::encode, OpenLinkSignScreenS2C::decode, OpenLinkSignScreenS2C.STREAM_CODEC, OpenLinkSignScreenS2C::handle);
    }
}