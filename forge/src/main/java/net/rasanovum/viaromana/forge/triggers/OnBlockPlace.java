package net.rasanovum.viaromana.forge.triggers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rasanovum.viaromana.core.SignInteract;
import net.rasanovum.viaromana.forge.ViaRomanaMod;
import net.minecraft.server.TickTask;

@Mod.EventBusSubscriber(modid = ViaRomanaMod.MODID)
public class OnBlockPlace {
    
    public OnBlockPlace() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer serverPlayer) {
            BlockPos placePos = event.getPos().relative(event.getFace());
            ItemStack heldItem = serverPlayer.getItemInHand(event.getHand());
            
            if (heldItem.getItem() instanceof SignItem) {
                serverPlayer.getServer().tell(new TickTask(5, () -> {
                    BlockState state = event.getLevel().getBlockState(placePos);
                    if (state.getBlock() instanceof SignBlock) {
                        SignInteract.placed(event.getLevel(), placePos.getX(), placePos.getY(), placePos.getZ(), serverPlayer);
                    }
                }));
            }
        }
    }
}
