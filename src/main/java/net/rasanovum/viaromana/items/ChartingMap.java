package net.rasanovum.viaromana.items;

import dev.corgitaco.dataanchor.network.broadcast.PacketBroadcaster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.client.data.ClientPathData;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C;
import net.rasanovum.viaromana.network.packets.MapRequestC2S;
import net.rasanovum.viaromana.network.packets.OpenChartingScreenS2C;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ChartingMap extends Item {
    
    public ChartingMap(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(itemStack);

        if (player instanceof ServerPlayer serverPlayer) {
            if (!player.isCrouching() || !CommonConfig.enable_remote_map_access) {
                PacketBroadcaster.S2C.sendToPlayer(new OpenChartingScreenS2C(), serverPlayer);
            }
            else {
                PathGraph graph = PathGraph.getInstance((ServerLevel) serverPlayer.level());
                if (graph == null) return InteractionResultHolder.pass(itemStack);

                Optional<Node> nearestNode = graph.getNearestNode(player.blockPosition(), CommonConfig.node_utility_distance * 2);

                if (nearestNode.isPresent()) {
                    Node node = nearestNode.get();
                    PathGraph.NetworkCache networkCache = graph.getNetworkCacheForNode(node);

                    List<net.rasanovum.viaromana.path.Node> destinations = graph.getCachedTeleportDestinationsFor(serverPlayer.getUUID(), node);
                    List<DestinationResponseS2C.NodeNetworkInfo> networkInfos = graph.getNodesAsInfo(networkCache);

                    DestinationResponseS2C responsePacket = new DestinationResponseS2C(
                            graph.getNodesAsDestinationInfo(destinations, node.getBlockPos()),
                            null,
                            node.getBlockPos(),
                            networkInfos,
                            networkCache.id()
                    );

                    PacketBroadcaster.S2C.sendToPlayer(responsePacket, serverPlayer);
                }


            }
        }
        
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide);
    }

    public boolean isFoil(ItemStack stack) {
        return false;
    }
    
//    public int getUseDuration(ItemStack stack) {
//        return 72000;
//    }
}
