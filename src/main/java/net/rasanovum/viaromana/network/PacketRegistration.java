package net.rasanovum.viaromana.network;

import commonnetwork.api.Network;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.packets.*;

public class PacketRegistration {
    public void init() {
        ViaRomana.LOGGER.info("Registering network packets");
        Network.registerPacket(ViaRomanaModVariables.PlayerVariablesSyncMessage.TYPE, ViaRomanaModVariables.PlayerVariablesSyncMessage.class, ViaRomanaModVariables.PlayerVariablesSyncMessage.STREAM_CODEC, ViaRomanaModVariables.PlayerVariablesSyncMessage::handle);
        Network.registerPacket(PathGraphSyncPacket.TYPE, PathGraphSyncPacket.class, PathGraphSyncPacket.STREAM_CODEC, PathGraphSyncPacket::handle);
        Network.registerPacket(OpenChartingScreenS2C.TYPE, OpenChartingScreenS2C.class, OpenChartingScreenS2C.STREAM_CODEC, OpenChartingScreenS2C::handle);
        Network.registerPacket(ChartedPathC2S.TYPE, ChartedPathC2S.class, ChartedPathC2S.STREAM_CODEC, ChartedPathC2S::handle);
        Network.registerPacket(MapResponseS2C.TYPE, MapResponseS2C.class, MapResponseS2C.STREAM_CODEC, MapResponseS2C::handle);
        Network.registerPacket(RoutedActionC2S.TYPE, RoutedActionC2S.class, RoutedActionC2S.STREAM_CODEC, RoutedActionC2S::handle);
        Network.registerPacket(DestinationRequestC2S.TYPE, DestinationRequestC2S.class, DestinationRequestC2S.STREAM_CODEC, DestinationRequestC2S::handle);
        Network.registerPacket(DestinationResponseS2C.TYPE, DestinationResponseS2C.class, DestinationResponseS2C.STREAM_CODEC, DestinationResponseS2C::handle);
        Network.registerPacket(LinkSignRequestC2S.TYPE, LinkSignRequestC2S.class, LinkSignRequestC2S.STREAM_CODEC, LinkSignRequestC2S::handle);
        Network.registerPacket(MapRequestC2S.TYPE, MapRequestC2S.class, MapRequestC2S.STREAM_CODEC, MapRequestC2S::handle);
        Network.registerPacket(OpenWarpBlockScreenS2C.TYPE, OpenWarpBlockScreenS2C.class, OpenWarpBlockScreenS2C.STREAM_CODEC, OpenWarpBlockScreenS2C::handle);
        Network.registerPacket(SignValidationRequestC2S.TYPE, SignValidationRequestC2S.class, SignValidationRequestC2S.STREAM_CODEC, SignValidationRequestC2S::handle);
        Network.registerPacket(SignValidationResponseS2C.TYPE, SignValidationResponseS2C.class, SignValidationResponseS2C.STREAM_CODEC, SignValidationResponseS2C::handle);
        Network.registerPacket(TeleportRequestC2S.TYPE, TeleportRequestC2S.class, TeleportRequestC2S.STREAM_CODEC, TeleportRequestC2S::handle);
        Network.registerPacket(UnlinkSignRequestC2S.TYPE, UnlinkSignRequestC2S.class, UnlinkSignRequestC2S.STREAM_CODEC, UnlinkSignRequestC2S::handle);
    }
}