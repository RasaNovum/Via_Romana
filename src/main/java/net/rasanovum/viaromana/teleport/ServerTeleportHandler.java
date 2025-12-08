package net.rasanovum.viaromana.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.corgitaco.dataanchor.network.broadcast.PacketBroadcaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.network.packets.TeleportFadeS2C;
import net.rasanovum.viaromana.network.packets.TeleportRequestC2S;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.speed.SpeedHandler;
import net.rasanovum.viaromana.util.EffectUtils;

public class ServerTeleportHandler {
    private static final Map<UUID, Node> activeTeleports = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> teleportStartTimes = new ConcurrentHashMap<>();
    
    // Teleport timing constants
    private static final int FADE_UP_TICKS = 20;
    private static final int HOLD_TICKS = 10;
    private static final int FADE_DOWN_TICKS = 20;
    private static final int FOOTSTEP_INTERVAL = 7;

    private static final RandomSource RANDOM = RandomSource.create();

    private static final double RANGE_WIDTH = 0.4;
    private static final double RANGE_MIN = 0.3;
    
    /**
     * Check if a player has an active teleport in progress
     */
    public static boolean isTeleporting(ServerPlayer player) {
        return activeTeleports.containsKey(player.getUUID());
    }
    
    /**
     * Called every server tick to process active teleports
     */
    public static void tick(ServerLevel level) {
        long currentTime = level.getGameTime();
        
        activeTeleports.forEach((playerUUID, targetNode) -> {
            Long startTime = teleportStartTimes.get(playerUUID);
            if (startTime == null) return;
            
            long elapsedTicks = currentTime - startTime;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
            
            if (elapsedTicks == FADE_UP_TICKS) {
                if (player != null && player.isAlive()) {
                    executeTeleportation(player, targetNode);
                    EffectUtils.applyEffect(player, "travellers_fatigue");
                }
            }
            else if (elapsedTicks == FADE_UP_TICKS + HOLD_TICKS + FADE_DOWN_TICKS) {
                activeTeleports.remove(playerUUID);
                teleportStartTimes.remove(playerUUID);
            }
        });
    }

    public static void handleTeleportRequest(TeleportRequestC2S packet, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        PathGraph graph = PathGraph.getInstance(level);

        if (graph == null || (packet.originSignPos() != null && !validateOriginSign(level, packet.originSignPos()))) {
            // ViaRomanaLandmarkManager.removeDestinationLandmark(level, packet.originSignPos().toLongPos()); //TODO: Add back
            return;
        }

        graph.getNodeAt(packet.destinationPos()).ifPresent(targetNode -> {
            activeTeleports.put(player.getUUID(), targetNode);
            teleportStartTimes.put(player.getUUID(), level.getGameTime());
            
            PacketBroadcaster.S2C.sendToPlayer(new TeleportFadeS2C(
                FADE_UP_TICKS,
                HOLD_TICKS,
                FADE_DOWN_TICKS,
                FOOTSTEP_INTERVAL
            ), player);
        });
    }

    private static void executeTeleportation(ServerPlayer player, Node targetNode) {
        if (!player.isAlive()) return;
        
        ServerLevel level = player.serverLevel();

        BlockPos centerPos = BlockPos.of(targetNode.getPos());
        BlockPos randomizedCenter = centerPos.offset(RANDOM.nextInt(3) - 1, 0, RANDOM.nextInt(3) - 1);
        BlockPos safePos = findSafePosition(level, randomizedCenter);

        if (safePos == null) {
            player.displayClientMessage(Component.translatable("message.via_romana.unsafe"), true);
            activeTeleports.remove(player.getUUID());
            return;
        }

        double x = safePos.getX() + (RANDOM.nextDouble() * RANGE_WIDTH) + RANGE_MIN;
        double y = getAccurateYPosition(level, safePos);
        double z = safePos.getZ() + (RANDOM.nextDouble() * RANGE_WIDTH) + RANGE_MIN;

        Entity rootVehicle = player.getRootVehicle();
        if (rootVehicle != player && !isValidTeleportEntity(rootVehicle)) {
            player.stopRiding();
            rootVehicle = player;
        }

        SpeedHandler.resetState(player);

        teleportStack(rootVehicle, level, x, y, z);
    }

    /**
     * Manually teleports an entity and all its passengers
     */
    private static void teleportStack(Entity rootEntity, ServerLevel level, double x, double y, double z) {
        // Deconstruct the entire passenger tree.
        Map<Entity, List<Entity>> passengerMap = new HashMap<>();
        List<Entity> allEntities = new ArrayList<>();
        
        List<Entity> toProcess = new ArrayList<>();
        toProcess.add(rootEntity);
        while (!toProcess.isEmpty()) {
            Entity current = toProcess.remove(0);
            allEntities.add(current);
            List<Entity> passengers = List.copyOf(current.getPassengers());
            if (!passengers.isEmpty()) {
                passengerMap.put(current, passengers);
                toProcess.addAll(passengers);
            }
        }
        rootEntity.ejectPassengers();

        // Teleport or recreate each entity
        Map<Entity, Entity> newEntityMap = new HashMap<>();
        for (Entity oldEntity : allEntities) {
            Entity newEntity = recreateAndTeleportEntity(oldEntity, level, x, y, z);
            if (newEntity != null) {
                newEntityMap.put(oldEntity, newEntity);
            }
        }

        // Schedule a task to reassemble the stack.
        if (!passengerMap.isEmpty()) {
            level.getServer().tell(new TickTask(3, () -> {
                passengerMap.forEach((oldVehicle, oldPassengers) -> {
                    Entity newVehicle = newEntityMap.get(oldVehicle);
                    if (newVehicle != null && newVehicle.isAlive()) {
                        oldPassengers.forEach(oldPassenger -> {
                            Entity newPassenger = newEntityMap.get(oldPassenger);
                            if (newPassenger != null && newPassenger.isAlive()) {
                                newPassenger.startRiding(newVehicle, true);
                            }
                        });
                    }
                });
            }));
        }
    }

    /**
     * Recreates a single entity at the target location using a safer, two-step process.
     */
    private static Entity recreateAndTeleportEntity(Entity oldEntity, ServerLevel level, double x, double y, double z) {
        if (oldEntity.isRemoved()) return null;

        if (oldEntity instanceof ServerPlayer player) {
            player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
            player.fallDistance = 0.0F;
            return player;
        }

        // Create a new entity
        Entity newEntity = oldEntity.getType().create(level);
        if (newEntity == null) return null;

        // Clone NBT data
        CompoundTag nbt = oldEntity.saveWithoutId(new CompoundTag());
        nbt.remove("Passengers");
        nbt.remove("UUID");
        newEntity.load(nbt);

        // Add the new entity to the world at target position
        newEntity.moveTo(x, y, z, oldEntity.getYRot(), oldEntity.getXRot());
        level.addFreshEntity(newEntity);

        // Remove the old entity
        oldEntity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        
        return newEntity;
    }

    private static boolean validateOriginSign(ServerLevel level, BlockPos signPos) {
        boolean isValid = LinkHandler.isSignLinked(level, signPos);
        return isValid;
    }
    
    private static BlockPos findSafePosition(ServerLevel level, BlockPos center) {
        for (int dx = 0; dx <= 1; dx = (dx > 0) ? -dx : -dx + 1) {
            for (int dz = 0; dz <= 1; dz = (dz > 0) ? -dz : -dz + 1) {
                BlockPos checkPos = center.offset(dx, 0, dz);
                if (!isHole(level, checkPos)) {
                    return checkPos;
                }
            }
        }
        return null;
    }

    private static boolean isHole(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 5; i++) {
            BlockPos checkPos = pos.below(i);
            BlockState state = level.getBlockState(checkPos);
            if (!state.isAir() && !state.getCollisionShape(level, checkPos, CollisionContext.empty()).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static double getAccurateYPosition(ServerLevel level, BlockPos pos) {
        for (int i = 0; i < 10; i++) {
            BlockPos groundPos = pos.below(i);
            BlockState groundState = level.getBlockState(groundPos);
            VoxelShape shape = groundState.getCollisionShape(level, groundPos, CollisionContext.empty());
            if (!shape.isEmpty()) {
                return groundPos.getY() + shape.max(Direction.Axis.Y);
            }
        }
        return pos.getY() + 1.0;
    }

    private static boolean isValidTeleportEntity(Entity entity) {
        String entityType = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return !CommonConfig.invalid_entities.contains(entityType);
    }
}