package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.PlatformUtils;
import net.rasanovum.viaromana.util.PathUtils;
import net.rasanovum.viaromana.util.TimerUtils;
import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import java.util.List;

public class SignInteract {
	public static void placed(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !SignCheck.isSignBlock(world, x, y, z) || VariableAccess.playerVariables.hasReceivedTutorial(entity)) {
			return;
		}

		if (PlatformUtils.isModLoaded("patchouli")) {
			giveGuideBook(world, entity);
			playPickupSound(world, entity);
		}

		VariableAccess.playerVariables.setReceivedTutorial(entity, true);
		VariableAccess.playerVariables.setAwaitingToast(entity, true);
		VariableAccess.playerVariables.syncAndSave(entity);
	}

	public static void broken(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
			
		if (SignCheck.isSignBlock(world, x, y, z)) {
			String tagValue = PlatformUtils.getString(world, new BlockPos(x, y, z), SignCheck.getTarget(world, x, y, z, entity));
				
			if (!tagValue.isEmpty()) {
				unlinkPath(world, x, y, z, entity);
				ResetVariables.execute(world, entity);
				if (entity instanceof Player player && !player.getLevel().isClientSide()) {
					player.displayClientMessage(Component.literal(Component.translatable("sign_unlinked_message").getString()), true);
				}
			}
		}
	}

	// TODO: Add a proper system for preventing breakage of partially linked signs, maybe a config?
	public static void clicked(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		if (SignCheck.isSignBlock(world, x, y, z)) {
			if (VariableAccess.playerVariables.isChartingPath(entity)) {
				if (TimerUtils.checkLeftClickTimer(entity)) {
					TimerUtils.startLeftClickTimer(entity);
					if (entity.isShiftKeyDown()) { // If charting path and shift-clicking
						if (SignCheck.isSameSign(world, x, y, z, entity)) {
							if (entity instanceof Player _player && !_player.getLevel().isClientSide())
								_player.displayClientMessage(Component.literal((Component.translatable("cancel_path_message").getString())), true);
							ResetVariables.execute(world, entity);
						} else if (SignCheck.isTargettingLinked(world, x, y, z, entity)) {
							if (entity instanceof Player _player && !_player.getLevel().isClientSide())
								_player.displayClientMessage(Component.literal((Component.translatable("sign_already_linked").getString())), true);
						} else {
							mainLogic(world, x, y, z, entity);
						}
					} else { // If charting path, but not shift-clicking
						if (SignCheck.isSameSign(world, x, y, z, entity)) {
							if (entity instanceof Player _player && !_player.getLevel().isClientSide())
								_player.displayClientMessage(Component.literal((Component.translatable("cannot_break_root_sign").getString())), true);
							PlatformUtils.cancelClickEvent(true);
							return;
						}
						if (SignCheck.isTargettingLinked(world, x, y, z, entity)) {
							if (entity instanceof Player _player && !_player.getLevel().isClientSide())
								_player.displayClientMessage(Component.literal((Component.translatable("cannot_warp_when_recording").getString())), true);
						} else {
							return;
						}
					}
				}
				PlatformUtils.cancelClickEvent(true);
				return;
			} else { // If not charting path
				// If targetting linked sign and not shift-clicking or not targetting linked sign and shift-clicking, execute MainLogic for teleport or begin chart
				if (SignCheck.isTargettingLinked(world, x, y, z, entity) && !entity.isShiftKeyDown() || !SignCheck.isTargettingLinked(world, x, y, z, entity) && entity.isShiftKeyDown()) {
					if (TimerUtils.checkLeftClickTimer(entity)) {
						TimerUtils.startLeftClickTimer(entity);
						mainLogic(world, x, y, z, entity);
					}
					PlatformUtils.cancelClickEvent(true);
					return;
				}
				// If not charting path and shift-clicking
				else if (SignCheck.isTargettingLinked(world, x, y, z, entity) && entity.isShiftKeyDown() && !TimerUtils.checkLeftClickTimer(entity)) {
					PlatformUtils.cancelClickEvent(true);
					return;
				}
			}
		}
	}

	private static void giveGuideBook(LevelAccessor world, Entity entity) {
		if (world instanceof ServerLevel serverLevel) {
			String command = "/give " + entity.getDisplayName().getString() + " patchouli:guide_book{\"patchouli:book\":\"via_romana:guide\"}";
			CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, 
				new Vec3(entity.getX(), entity.getY(), entity.getZ()), 
				Vec2.ZERO, serverLevel, 4, "", Component.literal(""), 
				serverLevel.getServer(), null).withSuppressedOutput();
			serverLevel.getServer().getCommands().performPrefixedCommand(source, command);
		}
	}

	private static void playPickupSound(LevelAccessor world, Entity entity) {
		if (world instanceof Level level) {
			ResourceLocation soundLoc = new ResourceLocation("entity.item.pickup");
			BlockPos pos = new BlockPos(entity.getX(), entity.getY(), entity.getZ());
			if (!level.isClientSide()) {
				level.playSound(null, pos, Registry.SOUND_EVENT.get(soundLoc), SoundSource.NEUTRAL, 1, 1);
			} else {
				level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(), 
					Registry.SOUND_EVENT.get(soundLoc), SoundSource.NEUTRAL, 1, 1, false);
			}
		}
	}

	public static void unlinkPath(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		
		String nbtKey = SignCheck.getTarget(world, x, y, z, entity);
		String pathStoredData = PlatformUtils.getString(world, new BlockPos(x, y, z), nbtKey);
		List<Object> linkedSignData = PathUtils.decodePathData(pathStoredData);
		String linkedSignNbtKey = "";

		try {
			double targetX = (Double) linkedSignData.get(3);
			double targetY = (Double) linkedSignData.get(4);
			double targetZ = (Double) linkedSignData.get(5);

			if (linkedSignData.size() > 6 && linkedSignData.get(6) instanceof String) {
				linkedSignNbtKey = (String) linkedSignData.get(6);
			} else {
				throw new ClassCastException("Missing or invalid NBT key index 6");
			}
			
			if (!world.isClientSide()) {
				BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
				BlockEntity blockEntity = world.getBlockEntity(targetPos);
				
				if (blockEntity != null) {
					PlatformUtils.setString(world, targetPos, linkedSignNbtKey, "");
				}
			}
			
			if (entity instanceof Player player && !player.getLevel().isClientSide()) {
				player.displayClientMessage(
					Component.literal(Component.translatable("sign_unlinked_message").getString()),
					true
				);
			}
			
		} catch (IndexOutOfBoundsException | ClassCastException e) {
			if (entity instanceof Player player && !player.getLevel().isClientSide()) {
				player.displayClientMessage(
					Component.literal("Error: Could not unlink sign due to invalid/missing data."),
					true
				);
			}
		}
	}

	public static void mainLogic(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		if (DimensionHandler.isValid(world, entity)) {
			TimerUtils.startMessageTimer(entity);
			if (entity.isShiftKeyDown()) {
				if (!SignCheck.isTargettingLinked(world, x, y, z, entity)) {
					if (PathUtils.calculateInfrastructureQuality(world, entity) >= VariableAccess.mapVariables.getInfrastructureCheckQuality(world)) {
						if (!VariableAccess.playerVariables.isChartingPath(entity)) {
							if (entity instanceof Player _player && !_player.getLevel().isClientSide())
								_player.displayClientMessage(Component.literal((Component.translatable("path_charting").getString())), true);
							PathRecord.start(world, x, y, z, entity);
							ChartingHandler.display(world, entity);
						} else {
							if (PathUtils.calculatePathStartDistance(entity) >= VariableAccess.mapVariables.getPathDistanceMinimum(world)) {
								if (SignCheck.isSignFound(world, x, y, z, entity) && PathUtils.calculateNodeDistance(entity) <= VariableAccess.mapVariables.getNodeDistanceMaximum(world)) {
									PathRecord.end(world, x, y, z, entity);
									ChartingHandler.display(world, entity);
								}
							} else {
								if (entity instanceof Player _player && !_player.getLevel().isClientSide())
									_player.displayClientMessage(Component.literal((Component.translatable("path_invalid_minimum").getString())), true);
							}
						}
					} else {
						if (entity instanceof Player _player && !_player.getLevel().isClientSide())
							_player.displayClientMessage(Component.literal((Component.translatable("low_quality_message").getString())), true);
					}
				} else {
					if (entity instanceof Player _player && !_player.getLevel().isClientSide())
						_player.displayClientMessage(Component.literal((Component.translatable("sign_already_linked").getString())), true);
				}
			} else {
				if (!VariableAccess.playerVariables.isChartingPath(entity)) {
					if (SignCheck.isTargettingLinked(world, x, y, z, entity)) {
						if (PlatformUtils.hasEffect(entity, "travellers_fatigue")) {
							if (entity instanceof Player _player && !_player.getLevel().isClientSide())
								_player.displayClientMessage(Component.literal((Component.translatable("has_fatigue").getString())), true);
						}
						else {
							TeleportHandler.start(world, x, y, z, entity);
						}
					}
				} else {
					if (entity instanceof Player _player && !_player.getLevel().isClientSide())
						_player.displayClientMessage(Component.literal((Component.translatable("cannot_warp_when_recording").getString())), true);
				}
			}
		}
	}
}
