package net.rasanovum.viaromana.speed;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;

import java.util.Optional;

public class SpeedHandler {
    public static final ResourceLocation PROXIMITY_SPEED_ID = ResourceLocation.fromNamespaceAndPath("viaromana", "node_proximity_speed");

    public static void onPlayerTick(ServerPlayer player) {
        if (player.level().isClientSide) return;

        AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        boolean nearNode = false;
        PathGraph graph = PathGraph.getInstance((ServerLevel) player.level());

        if (graph != null) {
            Optional<Node> nearestNode = graph.getNearestNode(player.blockPosition(), CommonConfig.node_distance_minimum);
            nearNode = nearestNode.isPresent();
        }

//        net.rasanovum.viaromana.ViaRomana.LOGGER.info("Near Node: {}", nearNode);

        boolean hasModifier = speedAttribute.getModifier(PROXIMITY_SPEED_ID) != null;

        if (nearNode && !hasModifier) {
            AttributeModifier modifier = new AttributeModifier(
                    PROXIMITY_SPEED_ID,
                    CommonConfig.fast_movement_speed,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            speedAttribute.addPermanentModifier(modifier);
        } else if (!nearNode && hasModifier) {
            speedAttribute.removeModifier(PROXIMITY_SPEED_ID);
        }
    }
}