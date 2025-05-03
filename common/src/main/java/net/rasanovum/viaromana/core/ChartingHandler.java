package net.rasanovum.viaromana.core;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Registry;
import net.minecraft.sounds.SoundEvent;
import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.util.PathUtils;
import net.rasanovum.viaromana.util.TimerUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class ChartingHandler {
	public static void chartPath(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		if (VariableAccess.playerVariables.isChartingPath(entity)) {
			if (PathUtils.calculateNodeDistance(entity) > VariableAccess.mapVariables.getNodeDistanceMaximum(world)) {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal((Component.translatable("too_far_from_node_message").getString())), true);
			} else if (PathUtils.calculateInfrastructureQuality(world, entity) < VariableAccess.mapVariables.getInfrastructureCheckQuality(world)) {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal((Component.translatable("low_quality_message").getString())), true);
			} else if (PathUtils.calculateNodeDistance(entity) < VariableAccess.mapVariables.getNodeDistanceMinimum(world)) {
				if (TimerUtils.checkMessageTimer(entity)) {
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal((Component.translatable("path_charting").getString())), true);
					TimerUtils.startMessageTimer(entity);
				}
				return;
			} else if (PathUtils.calculatePathStartDistance(entity) >= VariableAccess.mapVariables.getPathDistanceMaximum(world)) {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal((Component.translatable("path_invalid_maximum").getString())), true);
			} else {
				if (SignCheck.isSignFound(world, x, y, z, entity)) {
					display(world, entity);
					return;
				}
			}
		}
		return;
	}

	// TODO: Add a message queue system to prevent messages from being overwritten
	public static void display(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;

		SoundEvent cartography_result = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("minecraft","ui.cartography_table.take_result"));

		if (VariableAccess.playerVariables.isChartingPath(entity)) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), cartography_result, SoundSource.PLAYERS, 1, 1);
				} else {
					_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), cartography_result, SoundSource.PLAYERS, 1, 1, false);
				}
			}
			updateLastNode(entity);
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal((Component.translatable("path_charted").getString())), true);

			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), cartography_result, SoundSource.PLAYERS, 1, 1);
				} else {
					_level.playLocalSound((entity.getX()), (entity.getY()), (entity.getZ()), cartography_result, SoundSource.PLAYERS, 1, 1, false);
				}
			}
			
			if (entity instanceof ServerPlayer _player) {
				AdvancementHolder _adv = _player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("via_romana","a_strand_type_game"));
				AdvancementProgress _ap = _player.getAdvancements().getOrStartProgress(_adv);
				if (!_ap.isDone()) {
					for (String criteria : _ap.getRemainingCriteria())
						_player.getAdvancements().award(_adv, criteria);
				}
			}
		}
	}

	public static void updateLastNode(Entity entity) {
		if (entity == null)
			return;
			
		VariableAccess.playerVariables.setLastNodeX(entity, entity.getX());
		VariableAccess.playerVariables.setLastNodeZ(entity, entity.getZ());
	}
}
