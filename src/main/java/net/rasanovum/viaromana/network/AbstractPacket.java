package net.rasanovum.viaromana.network;

import dev.corgitaco.dataanchor.network.Packet;
//? if >=1.21 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.IdentityHashMap;
import java.util.Map;
//? }

public interface AbstractPacket extends Packet {
    //? if >=1.21 {
    Map<Class<?>, Type<?>> TYPE_CACHE = new IdentityHashMap<>();

    @Override
    default CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE_CACHE.get(this.getClass());
    }

    static <T extends CustomPacketPayload> void registerType(Class<T> clazz, CustomPacketPayload.Type<T> type) {
        TYPE_CACHE.put(clazz, type);
    }
    //?}
}