package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.map.MapInfo;
import net.rasanovum.viaromana.network.AbstractPacket;

/**
 * Response from the server containing the generated map data.
 */
public record MapResponseS2C(MapInfo mapInfo) implements AbstractPacket {

    public MapResponseS2C(FriendlyByteBuf buf) {
        this(MapInfo.readFromBuffer(buf));
    }

    public void write(FriendlyByteBuf buf) {
        this.mapInfo.writeToBuffer(buf);
    }

    public void handle(Level level, Player player) {
        net.rasanovum.viaromana.client.MapClient.handleMapInfo(this);
    }
}