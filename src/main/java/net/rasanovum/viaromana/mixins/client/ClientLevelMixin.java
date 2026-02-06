package net.rasanovum.viaromana.mixins.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.rasanovum.viaromana.client.data.ClientPathData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(
            method = "sendBlockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V",
            at = @At("HEAD")
    )
    private void onViaRomanaBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        if (oldState == newState) return;

        ClientLevel level = (ClientLevel)(Object)this;

        if (oldState.getCollisionShape(level, pos).isEmpty() && newState.getCollisionShape(level, pos).isEmpty()) return;

        ClientPathData.getInstance().invalidateColumn(pos.getX(), pos.getY(), pos.getZ());
    }
}