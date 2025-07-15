package net.rasanovum.viaromana.core;

import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.PlatformUtils;
import net.rasanovum.viaromana.util.PathUtils;
import net.rasanovum.viaromana.util.TimerUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.ArrayList;

public class SignCheck {
	public static boolean isSignBlock(LevelAccessor world, double x, double y, double z) {
		BlockState TargetBlock = Blocks.AIR.defaultBlockState();
		TargetBlock = (world.getBlockState(BlockPos.containing(x, y, z)));
		if ((new ItemStack(TargetBlock.getBlock())).is(TagKey.create(Registries.ITEM, new ResourceLocation("via_romana:warp_sign")))) {
			return true;
		}

		for (int index0 = 0; index0 < (int) VariableAccess.mapVariables.getValidSignList(world).size(); index0++) {
			if ((BuiltInRegistries.BLOCK.getKey(TargetBlock.getBlock()).toString()).contains(VariableAccess.mapVariables.getValidSignList(world).get((int) index0) instanceof String _s ? _s : "")
					&& !(VariableAccess.mapVariables.getValidSignList(world).get((int) index0) instanceof String _s ? _s : "").isEmpty()) {
				return true;
			}
		}

		return false;
	}

	public static boolean isSignFound(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return false;
		List<Object> TeleportData = new ArrayList<>();
		String TeleportDataEncoded = "";
		double TargetX = 0;
		double TargetY = 0;
		double TargetZ = 0;
		TeleportDataEncoded = VariableAccess.playerVariables.getPathData(entity);
		TeleportData = PathUtils.decodePathData(TeleportDataEncoded);
		try {
			TargetX = (Double) TeleportData.get(3);
			TargetY = (Double) TeleportData.get(4);
			TargetZ = (Double) TeleportData.get(5);
		} catch (IndexOutOfBoundsException | ClassCastException e) {
			if (entity instanceof Player _player && !_player.getLevel().isClientSide())
				_player.displayClientMessage(Component.literal("Error: Invalid path data format."), true);
			ResetVariables.execute(world, entity);
			return false;
		}

		if (VariableAccess.playerVariables.isChartingPath(entity)) {
			if (!isSignBlock(world, TargetX, TargetY, TargetZ)) {
				if (entity instanceof Player _player && !_player.getLevel().isClientSide())
					_player.displayClientMessage(Component.literal("Starting sign not found, charting cancelled"), true);
				TimerUtils.startLeftClickTimer(entity);
				ResetVariables.execute(world, entity);
				return false;
			}
		} else {
			String signNbtKey = "";
			if (TeleportData.size() > 6 && TeleportData.get(6) instanceof String) {
				signNbtKey = (String) TeleportData.get(6);
			} else {
				if (entity instanceof Player _player && !_player.getLevel().isClientSide())
					_player.displayClientMessage(Component.literal("Error: Missing sign data."), true);
				ResetVariables.execute(world, entity);
				return false;
			}

			if ((new Object() {
				public String getValue(LevelAccessor world, BlockPos pos, String tag) {
					return PlatformUtils.getString(world, pos, tag);
				}
			}.getValue(world, BlockPos.containing(TargetX, TargetY, TargetZ), signNbtKey)).equals("")) {
				if (!world.isClientSide()) {
					BlockPos _bp = BlockPos.containing(x, y, z);
					BlockEntity _blockEntity = world.getBlockEntity(_bp);
					if (_blockEntity != null)
						PlatformUtils.setString(world, BlockPos.containing(x, y, z), SignCheck.getTarget(world, x, y, z, entity), "");
				}
				if (entity instanceof Player _player && !_player.getLevel().isClientSide())
					_player.displayClientMessage(Component.literal(("Linked sign not found, " + Component.translatable("sign_unlinked_message").getString())), true);
				ResetVariables.execute(world, entity);
				return false;
			}
		}
		return true;
	}

	public static boolean isSameSign(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return false;

		String PathStoredData = "";
		List<Object> PathStoredArrayList = new ArrayList<>();
		PathStoredData = VariableAccess.playerVariables.getPathData(entity);
		PathStoredArrayList = PathUtils.decodePathData(PathStoredData);
		
		try {
			Double startX = (Double) PathStoredArrayList.get(3);
			Double startY = (Double) PathStoredArrayList.get(4);
			Double startZ = (Double) PathStoredArrayList.get(5);
			
			if (startX == x && startY == y && startZ == z) {
				if (PathStoredArrayList.size() >= 7) {
					if (PathStoredArrayList.get(6) instanceof String signTag && 
					    signTag.equals(SignCheck.getTarget(world, x, y, z, entity))) {
						return true;
					}
				}
			}
		} catch (IndexOutOfBoundsException | ClassCastException e) {
			return false;
		}
		
		return false;
	}

	public static boolean isIncludedInSign(double x, double y, double z, Entity entity) {
		if (entity == null)
			return false;
			
		String PathStoredData = "";
		List<Object> PathStoredArrayList = new ArrayList<>();
		PathStoredData = VariableAccess.playerVariables.getPathData(entity);
		PathStoredArrayList = PathUtils.decodePathData(PathStoredData);
		
		try {
			Double startX = (Double) PathStoredArrayList.get(3);
			Double startY = (Double) PathStoredArrayList.get(4);
			Double startZ = (Double) PathStoredArrayList.get(5);
			
			if (startX == x && startY == y && startZ == z) {
				return true;
			}
		} catch (IndexOutOfBoundsException | ClassCastException e) {
			return false;
		}
		
		return false;
	}

	public static boolean isSupplementariesSign(LevelAccessor world, double x, double y, double z) {
		if ((BuiltInRegistries.BLOCK.getKey((world.getBlockState(BlockPos.containing(x, y, z))).getBlock()).toString()).contains("supplementaries")) {
			return true;
		}
		return false;
	}

	public static boolean isTopSign(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return false;

		double hitY = 0;
		Vec3 eyePosition = entity.getEyePosition(1f);
		Vec3 viewVector = entity.getViewVector(1f).scale(5);
		Vec3 targetPosition = eyePosition.add(viewVector);
		ClipContext clipContext = new ClipContext(eyePosition, targetPosition, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity);
		BlockHitResult hitResult = entity.getLevel().clip(clipContext);
		Direction hitDirection = hitResult.getDirection();

		if (hitDirection == Direction.UP)
			return true;
		else if (hitDirection == Direction.DOWN)
			return false;

		hitY = hitResult.getLocation().y;
		
		return hitY - y >= 0.5;
	}

	public static String getTarget(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return "";

		if (SignCheck.isSupplementariesSign(world, x, y, z)) {
			if (SignCheck.isTopSign(world, x, y, z, entity)) {
				return "linkedSignTop";
			}
			return "linkedSignBottom";
		}
		return "linkedSign";
	}

	public static boolean isSignLinked(LevelAccessor world, double x, double y, double z) {
		if (!(new Object() {
			public String getValue(LevelAccessor world, BlockPos pos, String tag) {
				return PlatformUtils.getString(world, pos, tag);
			}
		}.getValue(world, BlockPos.containing(x, y, z), "linkedSignTop")).equals("") || !(new Object() {
			public String getValue(LevelAccessor world, BlockPos pos, String tag) {
				return PlatformUtils.getString(world, pos, tag);
			}
		}.getValue(world, BlockPos.containing(x, y, z), "linkedSignBottom")).equals("") || !(new Object() {
			public String getValue(LevelAccessor world, BlockPos pos, String tag) {
				return PlatformUtils.getString(world, pos, tag);
			}
		}.getValue(world, BlockPos.containing(x, y, z), "linkedSign")).equals("")) {
			return true;
		}
		return false;
	}

	public static boolean isFullyLinked(LevelAccessor world, double x, double y, double z) {
		if (!isSignLinked(world, x, y, z)) {
			return false;
		}
		if (!isSupplementariesSign(world, x, y, z) && !(new Object() {
			public String getValue(LevelAccessor world, BlockPos pos, String tag) {
				return PlatformUtils.getString(world, pos, tag);
			}
		}.getValue(world, BlockPos.containing(x, y, z), "linkedSign")).equals("")) {
			return true;
		}
		if (!(new Object() {
			public String getValue(LevelAccessor world, BlockPos pos, String tag) {
				return PlatformUtils.getString(world, pos, tag);
			}
		}.getValue(world, BlockPos.containing(x, y, z), "linkedSignTop")).equals("") && !(new Object() {
			public String getValue(LevelAccessor world, BlockPos pos, String tag) {
				return PlatformUtils.getString(world, pos, tag);
			}
		}.getValue(world, BlockPos.containing(x, y, z), "linkedSignBottom")).equals("")) {
			return true;
		}
		return false;
	}

	public static boolean isTargettingLinked(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return false;
		if (!(new Object() {
			public String getValue(LevelAccessor world, BlockPos pos, String tag) {
				return PlatformUtils.getString(world, pos, tag);
			}
		}.getValue(world, BlockPos.containing(x, y, z), SignCheck.getTarget(world, x, y, z, entity))).equals("")) {
			return true;
		}
		return false;
	}
}
