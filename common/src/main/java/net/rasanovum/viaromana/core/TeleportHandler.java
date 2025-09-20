package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.PlatformUtils;
import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.util.PathUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class TeleportHandler {
	public static void start(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		
		String pathData = PlatformUtils.getString(world, BlockPos.containing(x, y, z), SignCheck.getTarget(world, x, y, z, entity));
		VariableAccess.playerVariables.setPathData(entity, pathData);
		
		if (SignCheck.isSignFound(world, x, y, z, entity)) {
			VariableAccess.playerVariables.setFadeAmount(entity, 0);
			
			VariableAccess.playerVariables.setLastNodeX(entity, x);
			VariableAccess.playerVariables.setLastNodeY(entity, y);
			VariableAccess.playerVariables.setLastNodeZ(entity, z);
			
			VariableAccess.playerVariables.setLastSignPosition(entity, SignCheck.getTarget(world, x, y, z, entity));
			VariableAccess.playerVariables.setFadeIncrease(entity, true);
		}
		VariableAccess.playerVariables.syncAndSave(entity);
	}

	public static void cycle(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !entity.isAlive())
			return;
		
		handleFadeEffect(world, x, y, z, entity);
		
		if (VariableAccess.playerVariables.getFadeAmount(entity) == 10) {
			performTeleport(world, entity);
		}
		
		if (VariableAccess.playerVariables.getFadeAmount(entity) >= 15) {
			VariableAccess.playerVariables.setFadeIncrease(entity, false);
			VariableAccess.playerVariables.syncAndSave(entity);
		}
	}

	public static void effect(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		
		double fadeAmount = VariableAccess.playerVariables.getFadeAmount(entity);
		
		if (fadeAmount > 0) {
			double particleRadius = 4;
			if (world instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(
					ParticleTypes.ENCHANT, 
					(x + Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * particleRadius), 
					(y + fadeAmount * 0.15),
					(z + Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * particleRadius), 
					32, 
					(Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * particleRadius), 
					(fadeAmount * 0.02),
					(Mth.nextDouble(RandomSource.create(), -0.1, 0.1) * particleRadius), 
					0.5
				);
			}
		}
	}

	private static void playFootstepSound(LevelAccessor world, double x, double y, double z) {
		if (!(world instanceof Level level))
			return;
		
		BlockPos pos = BlockPos.containing(x, Math.ceil(y - 1), z);
		BlockState blockState = world.getBlockState(pos);
		ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(blockState.getSoundType().getStepSound());
		
		if (soundId == null)
			soundId = new ResourceLocation("block.grass.step");

		if (!level.isClientSide()) {
			level.playSound(null, pos, BuiltInRegistries.SOUND_EVENT.get(soundId), SoundSource.BLOCKS, 0.5f, 1f);
		} else {
			level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(soundId), SoundSource.BLOCKS, 0.5f, 1f, false);
		}
	}
	
	private static void handleFadeEffect(LevelAccessor world, double x, double y, double z, Entity entity) {
		double fadeAmount = VariableAccess.playerVariables.getFadeAmount(entity);
		
		if (fadeAmount >= 0 && fadeAmount <= 15) {
			if (fadeAmount % 7 == 0) {
				TeleportHandler.playFootstepSound(world, x, y, z);
			}
			
			boolean isIncreasing = VariableAccess.playerVariables.isFadeIncrease(entity);
			double newFadeAmount = isIncreasing ? fadeAmount + 1 : fadeAmount - 1;
			VariableAccess.playerVariables.setFadeAmount(entity, newFadeAmount);
			VariableAccess.playerVariables.syncAndSave(entity);
			
			if (fadeAmount == 1) {
				PlatformUtils.applyEffect(entity, "travellers_fatigue", world);
			}
		}
	}
	
	private static void performTeleport(LevelAccessor world, Entity entity) {
		String pathData = VariableAccess.playerVariables.getPathData(entity);
		if (pathData.isEmpty()) {
			return;
		}
		
		try {
			List<Object> teleportData = PathUtils.decodePathData(pathData);
			double targetX = (Double) teleportData.get(0);
			double targetY = (Double) teleportData.get(1);
			double targetZ = (Double) teleportData.get(2);
			
			final Entity mountEntity;
			if (entity.isPassenger()) {
				mountEntity = entity.getVehicle();
				entity.stopRiding();
				
				if (mountEntity != null && VariableAccess.mapVariables.getValidEntityList(world).contains(BuiltInRegistries.ENTITY_TYPE.getKey(mountEntity.getType()).toString())) {
					teleportEntity(mountEntity, targetX, targetY, targetZ);
				}
			} else {
				mountEntity = null;
			}
			
			teleportEntity(entity, targetX, targetY, targetZ);
			
			if (mountEntity != null) {
				if (world instanceof ServerLevel serverLevel) {
					serverLevel.getServer().tell(new net.minecraft.server.TickTask(1, () -> {
						entity.startRiding(mountEntity, true);
					}));
				} else {
					entity.startRiding(mountEntity, true);
				}
			}
			
			ResetVariables.execute(world, entity);
			
		} catch (IndexOutOfBoundsException | ClassCastException e) {
			if (entity instanceof Player player && !player.level().isClientSide()) {
				player.displayClientMessage(
					Component.literal("Error: Invalid path data format during teleport."), 
					true
				);
			}
			ResetVariables.execute(world, entity);
		}
	}
	
	private static void teleportEntity(Entity entity, double x, double y, double z) {
		if (entity instanceof ServerPlayer serverPlayer) {
			serverPlayer.absMoveTo(x, Math.ceil(y), z, entity.getYRot(), entity.getXRot());
			serverPlayer.fallDistance = 0.0F;
			serverPlayer.connection.teleport(x, Math.ceil(y), z, entity.getYRot(), entity.getXRot());
			serverPlayer.setDeltaMovement(0, 0, 0);
			serverPlayer.setOnGround(true);
		} else {
			entity.teleportTo(x, Math.ceil(y), z);
			entity.setDeltaMovement(0, 0, 0);
			
			if (entity instanceof LivingEntity living) {
				living.fallDistance = 0.0F;
				living.setOnGround(true);
			}
		}
	}
}
