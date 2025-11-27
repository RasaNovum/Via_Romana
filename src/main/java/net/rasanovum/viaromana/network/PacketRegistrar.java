package net.rasanovum.viaromana.network;

import dev.corgitaco.dataanchor.network.NetworkContainer;
import dev.corgitaco.dataanchor.network.Packet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.BiConsumer;
import java.util.function.Function;

//? if >=1.21
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class PacketRegistrar {
    @FunctionalInterface
    public interface PacketRunner<T> {
        void run(T packet, Level level, Player player);
    }

    public static <T extends AbstractPacket> void register(
            NetworkContainer container,
            String id,
            Class<T> clazz,
            BiConsumer<T, FriendlyByteBuf> writer,
            Function<FriendlyByteBuf, T> reader,
            PacketRunner<T> handler
    ) {
//        net.rasanovum.viaromana.ViaRomana.LOGGER.info("PacketRegistrar: Registering '{}' (Class: {})", id, clazz.getSimpleName());

        PacketRunner<T> safeHandler = (packet, level, player) -> {
            if (level.isClientSide()) {
                ClientHandler.handleClient(handler, packet, level, player);
            } else if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.server.execute(() -> handler.run(packet, level, player));
            }
        };

        //? if >=1.21 {
        var location = net.rasanovum.viaromana.util.VersionUtils.getLocation("via_romana", id);
        var type = new CustomPacketPayload.Type<T>(location);
        var codec = CustomPacketPayload.codec(writer::accept, reader::apply);

        AbstractPacket.registerType(clazz, type);

        container.registerPacketHandler(new Packet.Handler(clazz, type, codec, (packet, level, player) -> safeHandler.run((T) packet, level, player)));
        //?} else {
        /*container.registerPacketHandler(id, new Packet.Handler(clazz,writer, reader, (packet, level, player) -> safeHandler.run((T) packet, level, player)));
        *///?}
    }

    private static class ClientHandler {
        private static <T> void handleClient(PacketRegistrar.PacketRunner<T> handler, T packet, Level level, Player player) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> handler.run(packet, level, player));
        }
    }
}