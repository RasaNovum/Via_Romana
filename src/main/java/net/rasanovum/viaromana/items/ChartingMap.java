package net.rasanovum.viaromana.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.network.packets.OpenChartingScreenS2C;
import commonnetwork.api.Dispatcher;

public class ChartingMap extends Item {
    
    public ChartingMap(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(itemStack);
        
        if (!level.isClientSide) {
            Dispatcher.sendToClient(new OpenChartingScreenS2C(), (ServerPlayer) player);
        }
        
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }

    public boolean isFoil(ItemStack stack) {
        return false;
    }
    
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }
}
