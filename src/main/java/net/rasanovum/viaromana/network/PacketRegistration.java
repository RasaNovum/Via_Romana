package net.rasanovum.viaromana.network;

import commonnetwork.api.Network;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.client.network.ClientPacketHandler;
import net.rasanovum.viaromana.loaders.Platform;
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
        // Network.registerPacket expects BiConsumer<T, FriendlyByteBuf> (packet, buffer)
        // but our encode methods are (buffer, packet), so we need to swap the parameters
        Network.registerPacket((net.minecraft.resources.ResourceLocation) type, packetClass, (pkt, buf) -> encoder.accept(buf, pkt), decoder, handler);
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
    
    public void initCommon() {
        ViaRomana.LOGGER.info("Registering common network packets");

        registerPacket(PathGraphSyncPacket.TYPE, PathGraphSyncPacket.class, PathGraphSyncPacket::encode, PathGraphSyncPacket::decode, PathGraphSyncPacket.STREAM_CODEC, PathGraphSyncPacket::handle);
        registerPacket(PreProcessChunksC2S.TYPE, PreProcessChunksC2S.class, PreProcessChunksC2S::encode, PreProcessChunksC2S::decode, PreProcessChunksC2S.STREAM_CODEC, PreProcessChunksC2S::handle);
        registerPacket(ChartedPathC2S.TYPE, ChartedPathC2S.class, ChartedPathC2S::encode, ChartedPathC2S::decode, ChartedPathC2S.STREAM_CODEC, ChartedPathC2S::handle);
        registerPacket(RoutedActionC2S.TYPE, RoutedActionC2S.class, RoutedActionC2S::encode, RoutedActionC2S::decode, RoutedActionC2S.STREAM_CODEC, RoutedActionC2S::handle);
        registerPacket(DestinationRequestC2S.TYPE, DestinationRequestC2S.class, DestinationRequestC2S::encode, DestinationRequestC2S::decode, DestinationRequestC2S.STREAM_CODEC, DestinationRequestC2S::handle);
        registerPacket(SignLinkRequestC2S.TYPE, SignLinkRequestC2S.class, SignLinkRequestC2S::encode, SignLinkRequestC2S::decode, SignLinkRequestC2S.STREAM_CODEC, SignLinkRequestC2S::handle);
        registerPacket(MapRequestC2S.TYPE, MapRequestC2S.class, MapRequestC2S::encode, MapRequestC2S::decode, MapRequestC2S.STREAM_CODEC, MapRequestC2S::handle);
        registerPacket(SignValidationRequestC2S.TYPE, SignValidationRequestC2S.class, SignValidationRequestC2S::encode, SignValidationRequestC2S::decode, SignValidationRequestC2S.STREAM_CODEC, SignValidationRequestC2S::handle);
        registerPacket(SyncPlayerDataC2S.TYPE, SyncPlayerDataC2S.class, SyncPlayerDataC2S::encode, SyncPlayerDataC2S::decode, SyncPlayerDataC2S.STREAM_CODEC, SyncPlayerDataC2S::handle);
        registerPacket(TeleportRequestC2S.TYPE, TeleportRequestC2S.class, TeleportRequestC2S::encode, TeleportRequestC2S::decode, TeleportRequestC2S.STREAM_CODEC, TeleportRequestC2S::handle);
        registerPacket(SignUnlinkRequestC2S.TYPE, SignUnlinkRequestC2S.class, SignUnlinkRequestC2S::encode, SignUnlinkRequestC2S::decode, SignUnlinkRequestC2S.STREAM_CODEC, SignUnlinkRequestC2S::handle);

        //? if >=1.21 {
        if (Platform.INSTANCE.isClientSide()) return;

        registerPacket(OpenChartingScreenS2C.TYPE, OpenChartingScreenS2C.class, OpenChartingScreenS2C::encode, OpenChartingScreenS2C::decode, OpenChartingScreenS2C.STREAM_CODEC, ctx -> {});
        registerPacket(OpenLinkSignScreenS2C.TYPE, OpenLinkSignScreenS2C.class, OpenLinkSignScreenS2C::encode, OpenLinkSignScreenS2C::decode, OpenLinkSignScreenS2C.STREAM_CODEC, ctx -> {});
        registerPacket(OpenWarpBlockScreenS2C.TYPE, OpenWarpBlockScreenS2C.class, OpenWarpBlockScreenS2C::encode, OpenWarpBlockScreenS2C::decode, OpenWarpBlockScreenS2C.STREAM_CODEC, ctx -> {});
        registerPacket(TeleportFadeS2C.TYPE, TeleportFadeS2C.class, TeleportFadeS2C::encode, TeleportFadeS2C::decode, TeleportFadeS2C.STREAM_CODEC, ctx -> {});
        registerPacket(SignValidationResponseS2C.TYPE, SignValidationResponseS2C.class, SignValidationResponseS2C::encode, SignValidationResponseS2C::decode, SignValidationResponseS2C.STREAM_CODEC, ctx -> {});
        registerPacket(DestinationResponseS2C.TYPE, DestinationResponseS2C.class, DestinationResponseS2C::encode, DestinationResponseS2C::decode, DestinationResponseS2C.STREAM_CODEC, ctx -> {});
        registerPacket(MapResponseS2C.TYPE, MapResponseS2C.class, MapResponseS2C::encode, MapResponseS2C::decode, MapResponseS2C.STREAM_CODEC, ctx -> {});
        //?} else {
        /*if (Platform.INSTANCE.isClientSide()) {
            registerPacket(OpenChartingScreenS2C.TYPE, OpenChartingScreenS2C.class, OpenChartingScreenS2C::encode, OpenChartingScreenS2C::decode, OpenChartingScreenS2C.STREAM_CODEC, ClientPacketHandler::handleOpenChartingScreen);
            registerPacket(OpenLinkSignScreenS2C.TYPE, OpenLinkSignScreenS2C.class, OpenLinkSignScreenS2C::encode, OpenLinkSignScreenS2C::decode, OpenLinkSignScreenS2C.STREAM_CODEC, ClientPacketHandler::handleOpenLinkSignScreen);
            registerPacket(OpenWarpBlockScreenS2C.TYPE, OpenWarpBlockScreenS2C.class, OpenWarpBlockScreenS2C::encode, OpenWarpBlockScreenS2C::decode, OpenWarpBlockScreenS2C.STREAM_CODEC, ClientPacketHandler::handleOpenWarpBlockScreen);
            registerPacket(TeleportFadeS2C.TYPE, TeleportFadeS2C.class, TeleportFadeS2C::encode, TeleportFadeS2C::decode, TeleportFadeS2C.STREAM_CODEC, ClientPacketHandler::handleTeleportFade);
            registerPacket(SignValidationResponseS2C.TYPE, SignValidationResponseS2C.class, SignValidationResponseS2C::encode, SignValidationResponseS2C::decode, SignValidationResponseS2C.STREAM_CODEC, ClientPacketHandler::handleSignValidationResponse);
            registerPacket(DestinationResponseS2C.TYPE, DestinationResponseS2C.class, DestinationResponseS2C::encode, DestinationResponseS2C::decode, DestinationResponseS2C.STREAM_CODEC, ClientPacketHandler::handleDestinationResponse);
            registerPacket(MapResponseS2C.TYPE, MapResponseS2C.class, MapResponseS2C::encode, MapResponseS2C::decode, MapResponseS2C.STREAM_CODEC, ClientPacketHandler::handleMapResponse);
        } else {
            registerPacket(OpenChartingScreenS2C.TYPE, OpenChartingScreenS2C.class, OpenChartingScreenS2C::encode, OpenChartingScreenS2C::decode, OpenChartingScreenS2C.STREAM_CODEC, ctx -> {});
            registerPacket(OpenLinkSignScreenS2C.TYPE, OpenLinkSignScreenS2C.class, OpenLinkSignScreenS2C::encode, OpenLinkSignScreenS2C::decode, OpenLinkSignScreenS2C.STREAM_CODEC, ctx -> {});
            registerPacket(OpenWarpBlockScreenS2C.TYPE, OpenWarpBlockScreenS2C.class, OpenWarpBlockScreenS2C::encode, OpenWarpBlockScreenS2C::decode, OpenWarpBlockScreenS2C.STREAM_CODEC, ctx -> {});
            registerPacket(TeleportFadeS2C.TYPE, TeleportFadeS2C.class, TeleportFadeS2C::encode, TeleportFadeS2C::decode, TeleportFadeS2C.STREAM_CODEC, ctx -> {});
            registerPacket(SignValidationResponseS2C.TYPE, SignValidationResponseS2C.class, SignValidationResponseS2C::encode, SignValidationResponseS2C::decode, SignValidationResponseS2C.STREAM_CODEC, ctx -> {});
            registerPacket(DestinationResponseS2C.TYPE, DestinationResponseS2C.class, DestinationResponseS2C::encode, DestinationResponseS2C::decode, DestinationResponseS2C.STREAM_CODEC, ctx -> {});
            registerPacket(MapResponseS2C.TYPE, MapResponseS2C.class, MapResponseS2C::encode, MapResponseS2C::decode, MapResponseS2C.STREAM_CODEC, ctx -> {});
        }
        *///?}
    }

    public void initClient() {
        ViaRomana.LOGGER.info("Registering client network packets");

        //? if >=1.21 {
        registerPacket(OpenChartingScreenS2C.TYPE, OpenChartingScreenS2C.class, OpenChartingScreenS2C::encode, OpenChartingScreenS2C::decode, OpenChartingScreenS2C.STREAM_CODEC, ClientPacketHandler::handleOpenChartingScreen);
        registerPacket(OpenLinkSignScreenS2C.TYPE, OpenLinkSignScreenS2C.class, OpenLinkSignScreenS2C::encode, OpenLinkSignScreenS2C::decode, OpenLinkSignScreenS2C.STREAM_CODEC, ClientPacketHandler::handleOpenLinkSignScreen);
        registerPacket(OpenWarpBlockScreenS2C.TYPE, OpenWarpBlockScreenS2C.class, OpenWarpBlockScreenS2C::encode, OpenWarpBlockScreenS2C::decode, OpenWarpBlockScreenS2C.STREAM_CODEC, ClientPacketHandler::handleOpenWarpBlockScreen);
        registerPacket(TeleportFadeS2C.TYPE, TeleportFadeS2C.class, TeleportFadeS2C::encode, TeleportFadeS2C::decode, TeleportFadeS2C.STREAM_CODEC, ClientPacketHandler::handleTeleportFade);
        registerPacket(SignValidationResponseS2C.TYPE, SignValidationResponseS2C.class, SignValidationResponseS2C::encode, SignValidationResponseS2C::decode, SignValidationResponseS2C.STREAM_CODEC, ClientPacketHandler::handleSignValidationResponse);
        registerPacket(DestinationResponseS2C.TYPE, DestinationResponseS2C.class, DestinationResponseS2C::encode, DestinationResponseS2C::decode, DestinationResponseS2C.STREAM_CODEC, ClientPacketHandler::handleDestinationResponse);
        registerPacket(MapResponseS2C.TYPE, MapResponseS2C.class, MapResponseS2C::encode, MapResponseS2C::decode, MapResponseS2C.STREAM_CODEC, ClientPacketHandler::handleMapResponse);
        //?}
    }
}