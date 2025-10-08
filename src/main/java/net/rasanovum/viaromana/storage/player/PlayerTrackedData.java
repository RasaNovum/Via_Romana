package net.rasanovum.viaromana.storage.player;

import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import dev.corgitaco.dataanchor.data.type.entity.EntityTrackedData;
import dev.corgitaco.dataanchor.data.type.entity.SyncedPlayerTrackedData;
import commonnetwork.api.Dispatcher;
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
    private double fadeAmount = 0.0;
    private boolean fadeIncrease = false;
    private BlockPos lastNodePos = BlockPos.ZERO;
    private boolean receivedTutorial = false;

    public PlayerTrackedData(TrackedDataKey<? extends EntityTrackedData> key, Player player) {
        super((TrackedDataKey<? extends SyncedPlayerTrackedData>) key, player);
    }

    @Override
    public CompoundTag save() {
        ViaRomana.LOGGER.info("Saving PlayerTrackedData for entity ID {}: chartingPath={}, fadeAmount={}, fadeIncrease={}, lastNodePos={}, receivedTutorial={}", 
                this.entity.getId(), this.chartingPath, this.fadeAmount, this.fadeIncrease, this.lastNodePos, this.receivedTutorial);
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ChartingPath", this.chartingPath);
        tag.putDouble("FadeAmount", this.fadeAmount);
        tag.putBoolean("FadeIncrease", this.fadeIncrease);
        tag.putLong("LastNodePos", this.lastNodePos.asLong());
        tag.putBoolean("ReceivedTutorial", this.receivedTutorial);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        ViaRomana.LOGGER.info("Loading PlayerTrackedData for entity ID {}: {}", this.entity.getId(), tag);
        if (tag.contains("ChartingPath")) {
            this.chartingPath = tag.getBoolean("ChartingPath");
        }
        if (tag.contains("FadeAmount")) {
            this.fadeAmount = tag.getDouble("FadeAmount");
        }
        if (tag.contains("FadeIncrease")) {
            this.fadeIncrease = tag.getBoolean("FadeIncrease");
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
            Dispatcher.sendToServer(new SyncPlayerDataC2S(this.entity.getId(), this.trackedDataKey.getId(), this.writeToNetwork()));
        }
    }

    // Getters
    public boolean isChartingPath() {
        return chartingPath; 
    }
    
    public double getFadeAmount() { 
        return fadeAmount; 
    }
    
    public boolean isFadeIncrease() { 
        return fadeIncrease; 
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

    public void setFadeAmount(double value) {
        if (this.fadeAmount != value) {
            this.fadeAmount = value;
            sync();
        }
    }

    public void setFadeAmount(double value, boolean sync) {
        if (this.fadeAmount != value) {
            this.fadeAmount = value;
            if (sync) sync();
        }
    }

    public void setFadeIncrease(boolean value) {
        if (this.fadeIncrease != value) {
            this.fadeIncrease = value;
            sync();
        }
    }

    public void setLastNodePos(BlockPos value) {
        if (!this.lastNodePos.equals(value)) {
            this.lastNodePos = value;
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