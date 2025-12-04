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
import net.rasanovum.viaromana.util.VersionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class SpeedHandler {
    //? if >=1.21 {
    public static final ResourceLocation PROXIMITY_SPEED_ID = VersionUtils.getLocation("viaromana", "node_proximity_speed");
    //?} else {
    /*public static final UUID PROXIMITY_SPEED_ID = UUID.nameUUIDFromBytes("viaromana:node_proximity_speed".getBytes(StandardCharsets.UTF_8));
    public static final String PROXIMITY_SPEED_NAME = "node_proximity_speed";
    *///?}

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
            //? if >=1.21 {
            AttributeModifier modifier = new AttributeModifier(
                    PROXIMITY_SPEED_ID,
                    CommonConfig.fast_movement_speed,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            //?} else {
            /*AttributeModifier modifier = new AttributeModifier(
                    PROXIMITY_SPEED_ID,
                    PROXIMITY_SPEED_NAME,
                    CommonConfig.fast_movement_speed,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            *///?}
            speedAttribute.addPermanentModifier(modifier);
        } else if (!nearNode && hasModifier) {
            speedAttribute.removeModifier(PROXIMITY_SPEED_ID);
        }
    }
}