package net.rasanovum.viaromana.fabric.triggers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rasanovum.viaromana.core.SignInteract;
import net.minecraft.server.TickTask;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
public class OnBlockPlace {
    public OnBlockPlace() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer) {
                BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());
                ItemStack heldItem = player.getItemInHand(hand);
                if (heldItem.getItem() instanceof SignItem) {
                    world.getServer().tell(new TickTask(5, () -> {
                        BlockState state = world.getBlockState(placePos);
                        if (state.getBlock() instanceof SignBlock) {
                            SignInteract.placed(world, placePos.getX(), placePos.getY(), placePos.getZ(), player);
                        }
                    }));
                }
            }
            return InteractionResult.PASS;
        });
    }
}
