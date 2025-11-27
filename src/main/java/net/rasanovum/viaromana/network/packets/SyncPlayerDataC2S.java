package net.rasanovum.viaromana.network.packets;

import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.DataInit;
import net.rasanovum.viaromana.network.AbstractPacket;
import net.rasanovum.viaromana.storage.player.PlayerTrackedData;

/**
 * C2S Packet for syncing player TrackedData changes from client to server.
 */
public record SyncPlayerDataC2S(int playerId, ResourceLocation keyId, CompoundTag dataTag) implements AbstractPacket {

    public SyncPlayerDataC2S(int playerId, ResourceLocation keyId, CompoundTag dataTag) {
        this.playerId = playerId;
        this.keyId = keyId;
        this.dataTag = dataTag != null ? dataTag.copy() : new CompoundTag();
    }

    public SyncPlayerDataC2S(TrackedDataKey<?> key, Player player, CompoundTag dataTag) {
        this(player.getId(), key.getId(), dataTag);
    }

    public SyncPlayerDataC2S(FriendlyByteBuf buf) {
        this(
            buf.readVarInt(),
            buf.readResourceLocation(),
            buf.readNbt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.playerId);
        buf.writeResourceLocation(this.keyId);
        buf.writeNbt(this.dataTag);
    }

    public void handle(Level level, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getId() != this.playerId) {
                ViaRomana.LOGGER.warn("Invalid player ID in sync packet from {}", serverPlayer.getName().getString());
                return;
            }

            if (DataInit.PLAYER_DATA_KEY == null) {
                ViaRomana.LOGGER.warn("Unknown TrackedData key {} in sync packet from {}", this.keyId, serverPlayer.getName().getString());
                return;
            }

            var container = TrackedDataRegistries.ENTITY.getContainer(serverPlayer);
            if (container == null) {
                ViaRomana.LOGGER.warn("No data container for player {} on sync", serverPlayer.getName().getString());
                return;
            }

            container.dataAnchor$getTrackedData(DataInit.PLAYER_DATA_KEY)
                    .filter(data -> data instanceof PlayerTrackedData)
                    .ifPresent(data -> {
                        try {
                            data.readFromNetwork(this.dataTag);
                        } catch (Exception e) {
                            ViaRomana.LOGGER.error("Failed to apply C2S sync for player {}: {}", serverPlayer.getName().getString(), e.getMessage());
                        }
                    });
        }
    }
}
