package net.rasanovum.viaromana.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21 {
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.rasanovum.viaromana.util.VersionUtils;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;

/*
 * Response from the server indicating whether the sign at the given position is valid.
 */
//? if <1.21 {
/*public record SignValidationResponseS2C(BlockPos nodePos, boolean isValid) {
*///?} else {
public record SignValidationResponseS2C(BlockPos nodePos, boolean isValid) implements CustomPacketPayload {
//?}
    //? if <1.21 {
    /*public static final ResourceLocation TYPE = VersionUtils.getLocation("viaromana:sign_validation_s2c");
    public static final Object STREAM_CODEC = null;
    *///?} else {
    public static final CustomPacketPayload.Type<SignValidationResponseS2C> TYPE = new CustomPacketPayload.Type<>(VersionUtils.getLocation("viaromana:sign_validation_s2c"));

    public static final StreamCodec<FriendlyByteBuf, SignValidationResponseS2C> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SignValidationResponseS2C::nodePos,
        ByteBufCodecs.BOOL, SignValidationResponseS2C::isValid,
        SignValidationResponseS2C::new
    );
    //?}

    //? if >=1.21 {
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}

    public static void encode(FriendlyByteBuf buf, SignValidationResponseS2C packet) {
        buf.writeBlockPos(packet.nodePos);
        buf.writeBoolean(packet.isValid);
    }

    public static SignValidationResponseS2C decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean valid = buf.readBoolean();
        return new SignValidationResponseS2C(pos, valid);
    }

    public static void handle(PacketContext<SignValidationResponseS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof net.rasanovum.viaromana.client.gui.TeleportMapScreen screen) {
                screen.handleSignValidation(ctx.message().nodePos(), ctx.message().isValid());
            }
        }
    }
}
