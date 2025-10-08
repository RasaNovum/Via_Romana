package net.rasanovum.viaromana.network.packets;

import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
//? if >=1.21 {
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?} else {
/*import net.minecraft.advancements.Advancement;
 *///?}

import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.DataInit;
import net.rasanovum.viaromana.storage.player.PlayerTrackedData;
import net.rasanovum.viaromana.util.VersionUtils;

/**
 * C2S Packet for syncing player TrackedData changes from client to server.
 * Carries the TrackedData key ID and the NBT data diff for application and subsequent S2C broadcast.
 */
//? if <1.21 {
/*public record SyncPlayerDataC2S(int playerId, ResourceLocation keyId, CompoundTag dataTag) {
 *///?} else {
public record SyncPlayerDataC2S(int playerId, ResourceLocation keyId, CompoundTag dataTag) implements CustomPacketPayload {
    //?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("via_romana:sync_player_data_c2s");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<SyncPlayerDataC2S> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("via_romana:sync_player_data_c2s"));

    public static final StreamCodec<FriendlyByteBuf, SyncPlayerDataC2S> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncPlayerDataC2S decode(FriendlyByteBuf buffer) {
            return SyncPlayerDataC2S.decode(buffer);
        }

        @Override
        public void encode(FriendlyByteBuf buffer, SyncPlayerDataC2S packet) {
            SyncPlayerDataC2S.encode(buffer, packet);
        }
    };
    //?}

    public SyncPlayerDataC2S(int playerId, ResourceLocation keyId, CompoundTag dataTag) {
        this.playerId = playerId;
        this.keyId = keyId;
        this.dataTag = dataTag != null ? dataTag.copy() : new CompoundTag();
    }

    public SyncPlayerDataC2S(TrackedDataKey<?> key, net.minecraft.world.entity.player.Player player, CompoundTag dataTag) {
        this(player.getId(), key.getId(), dataTag);
    }

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buffer, SyncPlayerDataC2S packet) {
        buffer.writeVarInt(packet.playerId);
        buffer.writeResourceLocation(packet.keyId);
        buffer.writeNbt(packet.dataTag);
    }

    public static SyncPlayerDataC2S decode(FriendlyByteBuf buffer) {
        int playerId = buffer.readVarInt();
        ResourceLocation keyId = buffer.readResourceLocation();
        CompoundTag dataTag = buffer.readNbt();
        return new SyncPlayerDataC2S(playerId, keyId, dataTag);
    }

    public static void handle(PacketContext<SyncPlayerDataC2S> ctx) {
        if (Side.SERVER.equals(ctx.side())) {
            ServerPlayer player = ctx.sender();
            if (player == null || player.getId() != ctx.message().playerId) {
                ViaRomana.LOGGER.warn("Invalid player ID in sync packet from {}", player != null ? player.getName().getString() : "unknown");
                return;
            }

            SyncPlayerDataC2S packet = ctx.message();
            if (DataInit.PLAYER_DATA_KEY == null) {
                ViaRomana.LOGGER.warn("Unknown TrackedData key {} in sync packet from {}", packet.keyId, player.getName().getString());
                return;
            }

            var container = TrackedDataRegistries.ENTITY.getContainer(player);
            if (container == null) {
                ViaRomana.LOGGER.warn("No data container for player {} on sync", player.getName().getString());
                return;
            }

            container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                    .filter(data -> data instanceof PlayerTrackedData)
                    .ifPresent(data -> {
                        try {
                            data.readFromNetwork(packet.dataTag);
                            data.sync();
                        } catch (Exception e) {
                            ViaRomana.LOGGER.error("Failed to apply C2S sync for player {}: {}", player.getName().getString(), e.getMessage());
                        }
                    });
        }
    }
}