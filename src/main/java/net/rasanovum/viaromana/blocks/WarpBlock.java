package net.rasanovum.viaromana.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.rasanovum.viaromana.network.packets.OpenWarpBlockScreenS2C;
import commonnetwork.api.Dispatcher;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;

/**
 * Custom warp block that opens a screen when right-clicked, made for modpack developers
 */
public class WarpBlock extends Block {
    public WarpBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            OpenWarpBlockScreenS2C packet = new OpenWarpBlockScreenS2C(pos);
            Dispatcher.sendToClient(packet, serverPlayer);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }
}