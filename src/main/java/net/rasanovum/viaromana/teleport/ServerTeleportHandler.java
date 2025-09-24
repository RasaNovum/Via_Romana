package net.rasanovum.viaromana.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.network.TeleportRequestPacket;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.storage.IPathStorage;
import net.rasanovum.viaromana.variables.VariableAccess;

public class ServerTeleportHandler {

    public static void handleTeleportRequest(TeleportRequestPacket packet, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        IPathStorage storage = IPathStorage.get(level);

        if (storage == null || !validateOriginSign(level, packet.originSignPos())) {
            // ViaRomanaLandmarkManager.removeDestinationLandmark(level, packet.getDestinationPos());
            return;
        }

        storage.graph().getNodeAt(packet.destinationPos()).ifPresent(targetNode -> {
            VariableAccess.playerVariables.setLastNodePos(player, targetNode.getBlockPos());
            VariableAccess.playerVariables.setFadeAmount(player, 0);
            VariableAccess.playerVariables.setFadeIncrease(player, true);
            VariableAccess.playerVariables.syncAndSave(player);
        });
    }

    public static void executeTeleportation(ServerPlayer player) {
        BlockPos targetPos = VariableAccess.playerVariables.getLastNodePos(player);
        if (targetPos == null || targetPos.equals(BlockPos.ZERO)) return;

        ServerLevel level = player.serverLevel();
        IPathStorage storage = IPathStorage.get(level);
        if (storage == null) return;

        storage.graph().getNodeAt(targetPos).ifPresent(targetNode -> {
            performTeleportation(player, targetNode);
        });
    }

    private static void performTeleportation(ServerPlayer player, Node targetNode) {
        ServerLevel level = player.serverLevel();
        BlockPos safePos = findSafePosition(level, BlockPos.of(targetNode.getPos()));

        if (safePos == null) {
            player.displayClientMessage(Component.translatable("message.via_romana.unsafe"), true);
            VariableAccess.playerVariables.setFadeAmount(player, 0);
            VariableAccess.playerVariables.setFadeIncrease(player, false);
            VariableAccess.playerVariables.setLastNodePos(player, BlockPos.ZERO);
            VariableAccess.playerVariables.syncAndSave(player);
            return;
        }

        double x = safePos.getX() + 0.5;
        double y = getAccurateYPosition(level, safePos);
        double z = safePos.getZ() + 0.5;

        Entity rootVehicle = player.getRootVehicle();
        if (rootVehicle != player && !isValidTeleportEntity(rootVehicle)) {
            player.stopRiding();
            rootVehicle = player;
        }

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
        return !ViaRomanaConfig.invalid_entities.contains(entityType);
    }
}