package net.rasanovum.viaromana.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.rasanovum.viaromana.network.packets.*;

public class PacketTypeRegistry {
    public static void register() {
        // C2S
        PayloadTypeRegistry.playC2S().register(ViaRomanaModVariables.PlayerVariablesSyncMessage.TYPE, ViaRomanaModVariables.PlayerVariablesSyncMessage.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TeleportRequestC2S.TYPE, TeleportRequestC2S.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(LinkSignRequestC2S.TYPE, LinkSignRequestC2S.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UnlinkSignRequestC2S.TYPE, UnlinkSignRequestC2S.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DestinationRequestC2S.TYPE, DestinationRequestC2S.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SignValidationRequestC2S.TYPE, SignValidationRequestC2S.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MapRequestC2S.TYPE, MapRequestC2S.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ChartedPathC2S.TYPE, ChartedPathC2S.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RoutedActionC2S.TYPE, RoutedActionC2S.STREAM_CODEC);

        // S2C
        PayloadTypeRegistry.playS2C().register(ViaRomanaModVariables.PlayerVariablesSyncMessage.TYPE, ViaRomanaModVariables.PlayerVariablesSyncMessage.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DestinationResponseS2C.TYPE, DestinationResponseS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MapResponseS2C.TYPE, MapResponseS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SignValidationResponseS2C.TYPE, SignValidationResponseS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenChartingScreenS2C.TYPE, OpenChartingScreenS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PathGraphSyncPacket.TYPE, PathGraphSyncPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenWarpBlockScreenS2C.TYPE, OpenWarpBlockScreenS2C.STREAM_CODEC);
    }
}
