package net.rasanovum.viaromana.network;

import commonnetwork.api.Network;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.ViaRomanaModVariables.PlayerVariablesSyncMessage;
import net.rasanovum.viaromana.network.packets.*;

public class PacketRegistration {
    public void init() {
        ViaRomana.LOGGER.info("Registering network packets");
        Network
            .registerPacket(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handle)
            .registerPacket(PathGraphSyncPacket.TYPE, PathGraphSyncPacket.class, PathGraphSyncPacket.STREAM_CODEC, PathGraphSyncPacket::handle)
            .registerPacket(OpenChartingScreenS2C.TYPE, OpenChartingScreenS2C.class, OpenChartingScreenS2C.STREAM_CODEC, OpenChartingScreenS2C::handle)
            .registerPacket(ChartedPathC2S.type(), ChartedPathC2S.class, /*? <=1.21 {*/ /*ChartedPathC2S::encode, ChartedPathC2S::decode *//*?} else {*/ ChartedPathC2S.STREAM_CODEC /*?}*/, ChartedPathC2S::handle)
            .registerPacket(MapResponseS2C.TYPE, MapResponseS2C.class, MapResponseS2C.STREAM_CODEC, MapResponseS2C::handle)
            .registerPacket(RoutedActionC2S.TYPE, RoutedActionC2S.class, RoutedActionC2S.STREAM_CODEC, RoutedActionC2S::handle)
            .registerPacket(DestinationRequestC2S.TYPE, DestinationRequestC2S.class, DestinationRequestC2S.STREAM_CODEC, DestinationRequestC2S::handle)
            .registerPacket(DestinationResponseS2C.TYPE, DestinationResponseS2C.class, DestinationResponseS2C.STREAM_CODEC, DestinationResponseS2C::handle)
            .registerPacket(LinkSignRequestC2S.TYPE, LinkSignRequestC2S.class, LinkSignRequestC2S.STREAM_CODEC, LinkSignRequestC2S::handle)
            .registerPacket(MapRequestC2S.TYPE, MapRequestC2S.class, MapRequestC2S.STREAM_CODEC, MapRequestC2S::handle)
            .registerPacket(OpenWarpBlockScreenS2C.TYPE, OpenWarpBlockScreenS2C.class, OpenWarpBlockScreenS2C.STREAM_CODEC, OpenWarpBlockScreenS2C::handle)
            .registerPacket(SignValidationRequestC2S.TYPE, SignValidationRequestC2S.class, SignValidationRequestC2S.STREAM_CODEC, SignValidationRequestC2S::handle)
            .registerPacket(SignValidationResponseS2C.TYPE, SignValidationResponseS2C.class, SignValidationResponseS2C.STREAM_CODEC, SignValidationResponseS2C::handle)
            .registerPacket(TeleportRequestC2S.TYPE, TeleportRequestC2S.class, TeleportRequestC2S.STREAM_CODEC, TeleportRequestC2S::handle)
            .registerPacket(UnlinkSignRequestC2S.TYPE, UnlinkSignRequestC2S.class, UnlinkSignRequestC2S.STREAM_CODEC, UnlinkSignRequestC2S::handle);
    }
}