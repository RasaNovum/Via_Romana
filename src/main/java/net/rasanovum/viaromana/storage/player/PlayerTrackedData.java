package net.rasanovum.viaromana.storage.player;

import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import dev.corgitaco.dataanchor.data.type.entity.EntityTrackedData;
import dev.corgitaco.dataanchor.data.type.entity.SyncedPlayerTrackedData;
import dev.corgitaco.dataanchor.network.broadcast.PacketBroadcaster;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.network.packets.SyncPlayerDataC2S;

/**
 * Player-specific tracked data for Via Romana path charting.
 */
public class PlayerTrackedData extends SyncedPlayerTrackedData {
    private boolean chartingPath = false;
    private BlockPos lastNodePos = BlockPos.ZERO;
    private boolean receivedTutorial = false;

    public PlayerTrackedData(TrackedDataKey<? extends EntityTrackedData> key, Player player) {
        super((TrackedDataKey<? extends SyncedPlayerTrackedData>) key, player);
    }

    @Override
    public CompoundTag save() {
        // ViaRomana.LOGGER.info("Saving PlayerTrackedData for entity ID {}: chartingPath={}, lastNodePos={}, receivedTutorial={}", this.entity.getId(), this.chartingPath, this.lastNodePos, this.receivedTutorial);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ChartingPath", this.chartingPath);
        tag.putLong("LastNodePos", this.lastNodePos.asLong());
        tag.putBoolean("ReceivedTutorial", this.receivedTutorial);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        // ViaRomana.LOGGER.info("Loading PlayerTrackedData for entity ID {}: {}", this.entity.getId(), tag);
        if (tag.contains("ChartingPath")) {
            this.chartingPath = tag.getBoolean("ChartingPath");
        }
        if (tag.contains("LastNodePos")) {
            this.lastNodePos = BlockPos.of(tag.getLong("LastNodePos"));
        }
        if (tag.contains("ReceivedTutorial")) {
            this.receivedTutorial = tag.getBoolean("ReceivedTutorial");
        }
    }

    @Override
    public CompoundTag writeToNetwork() {
        return this.save();
    }

    @Override
    public void readFromNetwork(CompoundTag tag) {
        this.load(tag);
    }

    @Override
    public void sync() {
        if (this.entity.level() instanceof ServerLevel) {
            super.sync();
        }
        else if (this.entity.level() instanceof ClientLevel) {
            // ViaRomana.LOGGER.info("Sending SyncPlayerDataC2S packet to server for entity ID {}: {}", this.entity.getId(), this.writeToNetwork());
            PacketBroadcaster.C2S.sendToServer(new SyncPlayerDataC2S(this.entity.getId(), this.trackedDataKey.getId(), this.writeToNetwork()));
        }
    }

    // Getters
    public boolean isChartingPath() {
        return chartingPath; 
    }
    
    public BlockPos getLastNodePos() { 
        return lastNodePos; 
    }
    
    public boolean hasReceivedTutorial() { 
        return receivedTutorial; 
    }

    // Setters
    public void setChartingPath(boolean value) {
        if (this.chartingPath != value) {
            this.chartingPath = value;
            sync();
        }
    }

    public void setLastNodePos(BlockPos value, boolean sync) {
        if (!this.lastNodePos.equals(value)) {
            this.lastNodePos = value;
            if (sync) sync();
        }
    }

    public void setReceivedTutorial(boolean value) {
        if (this.receivedTutorial != value) {
            this.receivedTutorial = value;
            sync();
        }
    }

    public void resetVariables() {
        boolean changed = false;
        if (this.chartingPath) {
            this.chartingPath = false;
            changed = true;
        }
        if (!this.lastNodePos.equals(BlockPos.ZERO)) {
            this.lastNodePos = BlockPos.ZERO;
            changed = true;
        }
        if (changed) {
            sync();
        }
    }
}