package net.rasanovum.viaromana.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.rasanovum.viaromana.client.render.NodeRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLightEngine.class)
public class BlockLightEngineMixin {

    @Inject(method = "getEmission", at = @At("HEAD"), cancellable = true)
    private void via_romana_getDynamicLightEmission(long worldPos, BlockState blockState, CallbackInfoReturnable<Integer> cir) {
        if (Minecraft.getInstance().level == null) return;

        BlockPos pos = BlockPos.of(worldPos);
        int dynamicLight = NodeRenderer.getLightLevel(pos);

        if (dynamicLight > 0) {
            cir.setReturnValue(dynamicLight);
        }
    }
}
