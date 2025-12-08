package net.rasanovum.viaromana.items;

import dev.corgitaco.dataanchor.network.broadcast.PacketBroadcaster;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.client.core.PathRecord;
import net.rasanovum.viaromana.network.packets.DestinationResponseS2C;
import net.rasanovum.viaromana.network.packets.OpenChartingScreenS2C;
import net.rasanovum.viaromana.network.packets.RoutedActionC2S;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.List;
import java.util.Optional;

public class ChartingMap extends Item {
    static final ResourceLocation CHARTING = VersionUtils.getLocation("minecraft:item.book.page_turn");
    static final ResourceLocation SEVER = VersionUtils.getLocation("minecraft:entity.sheep.shear");
    
    public ChartingMap(Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(itemStack);

        if (player instanceof ServerPlayer serverPlayer && !CommonConfig.no_gui_charting) {
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
        else if (player instanceof LocalPlayer localPlayer && CommonConfig.no_gui_charting) {
            SoundEvent clickSound = SoundEvent.createVariableRangeEvent(CHARTING);
            if (!PlayerData.isChartingPath(player)) {
                if (!player.isCrouching()) PathRecord.start((ClientLevel) localPlayer.level(), localPlayer, localPlayer.blockPosition());
                else {
                    PacketBroadcaster.C2S.sendToServer(new RoutedActionC2S(RoutedActionC2S.Operation.SEVER_NEAREST_NODE));
                    clickSound = SoundEvent.createVariableRangeEvent(SEVER);
                }
            }
            else {
                if (!player.isCrouching()) PathRecord.end((ClientLevel) localPlayer.level(), localPlayer, localPlayer.blockPosition());
                else PathRecord.cancel((ClientLevel) localPlayer.level(), localPlayer, true);
            }

            player.playSound(clickSound, 1.0F, 1.0F);
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
