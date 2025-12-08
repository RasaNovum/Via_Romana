package net.rasanovum.viaromana.speed;

import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.init.StatInit;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.util.VersionUtils;
//? if >=1.21 {
import net.minecraft.advancements.AdvancementHolder;
//?} else {
/*import net.minecraft.advancements.Advancement;
*///?}

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SpeedHandler {
    //? if >=1.21 {
    public static final ResourceLocation PROXIMITY_SPEED_ID = VersionUtils.getLocation("viaromana", "node_proximity_speed");
    //?} else {
    /*public static final UUID PROXIMITY_SPEED_ID = UUID.nameUUIDFromBytes("viaromana:node_proximity_speed".getBytes(StandardCharsets.UTF_8));
    public static final String PROXIMITY_SPEED_NAME = "node_proximity_speed";
    *///?}
    
    private static final double DISTANCE_THRESHOLD = 5000.0 * 100; // Units in cm

    private static final Map<UUID, BlockPos> lastPathWalkPositions = new HashMap<>();

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

            lastPathWalkPositions.put(player.getUUID(), player.blockPosition());
        } else if (!nearNode && hasModifier) {
            speedAttribute.removeModifier(PROXIMITY_SPEED_ID);

            lastPathWalkPositions.remove(player.getUUID());
        } else if (nearNode && hasModifier) {
            BlockPos lastPos = lastPathWalkPositions.get(player.getUUID());
            BlockPos currentPos = player.blockPosition();
            
            if (lastPos == null) {
                lastPathWalkPositions.put(player.getUUID(), currentPos);
            } else if (!lastPos.equals(currentPos)) {
                double distance = Math.sqrt(lastPos.distSqr(currentPos));
                player.awardStat(Stats.CUSTOM.get(StatInit.DISTANCE_WALKED), (int) (distance * 100));
                lastPathWalkPositions.put(player.getUUID(), currentPos);

                int totalDistance = player.getStats().getValue(Stats.CUSTOM.get(StatInit.DISTANCE_WALKED));
                if (totalDistance >= DISTANCE_THRESHOLD) {
                    awardRunningAdvancement(player);
                }
            }
        }
    }
    
    private static void awardRunningAdvancement(ServerPlayer player) {
        try {
            //? if <1.21 {
            /*Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("via_romana:story/i_just_felt_like_running"));
            if (advancement != null) {
                AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancement);
                if (!advancementProgress.isDone()) {
                    for (String c : advancementProgress.getRemainingCriteria()) {
                        player.getAdvancements().award(advancement, c);
                    }
                }
            }
            *///?} else {
            ResourceLocation advancementId = VersionUtils.getLocation("via_romana:story/i_just_felt_like_running");
            AdvancementHolder advancement = player.server.getAdvancements().get(advancementId);
            if (advancement != null) {
                AdvancementProgress advancementProgress = player.getAdvancements().getOrStartProgress(advancement);
                if (!advancementProgress.isDone()) {
                    boolean grantedAny = false;
                    for (String criterion : advancementProgress.getRemainingCriteria()) {
                        boolean granted = player.getAdvancements().award(advancement, criterion);
                        if (granted) grantedAny = true;
                    }

                    if (grantedAny) player.getAdvancements().flushDirty(player);
                }
            }
            //?}
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Failed to award advancement {} to player {}: {}", "via_romana:story/i_just_felt_like_running", player.getName().getString(), e.getMessage());
        }
    }
}