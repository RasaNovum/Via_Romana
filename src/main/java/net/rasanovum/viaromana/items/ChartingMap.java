package net.rasanovum.viaromana.items;

import dev.corgitaco.dataanchor.network.broadcast.PacketBroadcaster;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.network.packets.OpenChartingScreenS2C;
//? if <=1.21.1
import net.minecraft.world.InteractionResultHolder;
//? if >1.21.1
/*import net.minecraft.world.InteractionResult;*/

public class ChartingMap extends Item {
    
    public ChartingMap(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    //? if <=1.21.1
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    //? if >1.21.1
    /*public InteractionResult use(Level level, Player player, InteractionHand hand) {*/
        ItemStack itemStack = player.getItemInHand(hand);

        //? if <=1.21.1
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(itemStack);
        //? if >1.21.1
        /*if (player.getCooldowns().isOnCooldown(itemStack)) return InteractionResult.PASS;*/

        if (player instanceof ServerPlayer serverPlayer) {
            PacketBroadcaster.S2C.sendToPlayer(new OpenChartingScreenS2C(), serverPlayer);
        }

        //? if <=1.21.1
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
        //? if >1.21.1
        /*return InteractionResult.SUCCESS;*/
    }

    public boolean isFoil(ItemStack stack) {
        return false;
    }
    
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }
}
