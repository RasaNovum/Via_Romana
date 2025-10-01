package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/*
 * Instruct the client to open the charting screen.
 */
public record OpenChartingScreenS2C() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenChartingScreenS2C> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.parse("via_romana:open_charting_screen_s2c"));

    public static final StreamCodec<FriendlyByteBuf, OpenChartingScreenS2C> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenChartingScreenS2C decode(FriendlyByteBuf buffer) {
            return new OpenChartingScreenS2C();
        }

        @Override
        public void encode(FriendlyByteBuf buffer, OpenChartingScreenS2C packet) {
            // nothing
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(commonnetwork.networking.data.PacketContext<OpenChartingScreenS2C> ctx) {
        if (commonnetwork.networking.data.Side.CLIENT.equals(ctx.side())) {
            net.minecraft.network.chat.Component title = net.minecraft.network.chat.Component.translatable("gui.viaromana.charting_screen.title");
            net.rasanovum.viaromana.client.gui.ChartingScreen screen = new net.rasanovum.viaromana.client.gui.ChartingScreen(title);
            net.minecraft.client.Minecraft.getInstance().setScreen(screen);
        }
    }
}