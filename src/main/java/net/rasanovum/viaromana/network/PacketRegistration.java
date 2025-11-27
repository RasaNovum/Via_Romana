package net.rasanovum.viaromana.network;

import dev.corgitaco.dataanchor.network.C2SNetworkContainer;
import dev.corgitaco.dataanchor.network.S2CNetworkContainer;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.packets.*;

public class PacketRegistration {

    public static final C2SNetworkContainer C2S_CONTAINER = C2SNetworkContainer.of(ViaRomana.MODID);
    public static final S2CNetworkContainer S2C_CONTAINER = S2CNetworkContainer.of(ViaRomana.MODID);
    
    public void initCommon() {
        ViaRomana.LOGGER.info("Registering network packets");

        PacketRegistrar.register(C2S_CONTAINER, "routed_action_c2s", RoutedActionC2S.class, RoutedActionC2S::write, RoutedActionC2S::new, RoutedActionC2S::handle);
        PacketRegistrar.register(C2S_CONTAINER, "pre_process_chunks_c2s", PreProcessChunksC2S.class, PreProcessChunksC2S::write, PreProcessChunksC2S::new, PreProcessChunksC2S::handle);
        PacketRegistrar.register(C2S_CONTAINER, "charted_path_c2s", ChartedPathC2S.class, ChartedPathC2S::write, ChartedPathC2S::new, ChartedPathC2S::handle);
        PacketRegistrar.register(C2S_CONTAINER, "destination_request_c2s", DestinationRequestC2S.class, DestinationRequestC2S::write, DestinationRequestC2S::new, DestinationRequestC2S::handle);
        PacketRegistrar.register(C2S_CONTAINER, "sign_link_request_c2s", SignLinkRequestC2S.class, SignLinkRequestC2S::write, SignLinkRequestC2S::new, SignLinkRequestC2S::handle);
        PacketRegistrar.register(C2S_CONTAINER, "map_request_c2s", MapRequestC2S.class, MapRequestC2S::write, MapRequestC2S::new, MapRequestC2S::handle);
        PacketRegistrar.register(C2S_CONTAINER, "sign_validation_request_c2s", SignValidationRequestC2S.class, SignValidationRequestC2S::write, SignValidationRequestC2S::new, SignValidationRequestC2S::handle);
        PacketRegistrar.register(C2S_CONTAINER, "sync_player_data_c2s", SyncPlayerDataC2S.class, SyncPlayerDataC2S::write, SyncPlayerDataC2S::new, SyncPlayerDataC2S::handle);
        PacketRegistrar.register(C2S_CONTAINER, "teleport_request_c2s", TeleportRequestC2S.class, TeleportRequestC2S::write, TeleportRequestC2S::new, TeleportRequestC2S::handle);
        PacketRegistrar.register(C2S_CONTAINER, "sign_unlink_request_c2s", SignUnlinkRequestC2S.class, SignUnlinkRequestC2S::write, SignUnlinkRequestC2S::new, SignUnlinkRequestC2S::handle);

        PacketRegistrar.register(S2C_CONTAINER, "map_response_s2c", MapResponseS2C.class, MapResponseS2C::write, MapResponseS2C::new, MapResponseS2C::handle);
        PacketRegistrar.register(S2C_CONTAINER, "path_graph_sync_s2c", PathGraphSyncPacket.class, PathGraphSyncPacket::write, PathGraphSyncPacket::new, PathGraphSyncPacket::handle);
        PacketRegistrar.register(S2C_CONTAINER, "config_sync_s2c", ConfigSyncS2C.class, ConfigSyncS2C::write, ConfigSyncS2C::new, ConfigSyncS2C::handle);
        PacketRegistrar.register(S2C_CONTAINER, "open_charting_screen_s2c", OpenChartingScreenS2C.class, OpenChartingScreenS2C::write, OpenChartingScreenS2C::new, OpenChartingScreenS2C::handle);
        PacketRegistrar.register(S2C_CONTAINER, "open_link_sign_screen_s2c", OpenLinkSignScreenS2C.class, OpenLinkSignScreenS2C::write, OpenLinkSignScreenS2C::new, OpenLinkSignScreenS2C::handle);
        PacketRegistrar.register(S2C_CONTAINER, "open_warp_block_screen_s2c", OpenWarpBlockScreenS2C.class, OpenWarpBlockScreenS2C::write, OpenWarpBlockScreenS2C::new, OpenWarpBlockScreenS2C::handle);
        PacketRegistrar.register(S2C_CONTAINER, "teleport_fade_s2c", TeleportFadeS2C.class, TeleportFadeS2C::write, TeleportFadeS2C::new, TeleportFadeS2C::handle);
        PacketRegistrar.register(S2C_CONTAINER, "sign_validation_response_s2c", SignValidationResponseS2C.class, SignValidationResponseS2C::write, SignValidationResponseS2C::new, SignValidationResponseS2C::handle);
        PacketRegistrar.register(S2C_CONTAINER, "destination_response_s2c", DestinationResponseS2C.class, DestinationResponseS2C::write, DestinationResponseS2C::new, DestinationResponseS2C::handle);
    }
}