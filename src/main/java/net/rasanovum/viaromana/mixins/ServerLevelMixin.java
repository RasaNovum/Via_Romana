package net.rasanovum.viaromana.mixins;

import dev.corgitaco.dataanchor.data.registry.TrackedDataRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.core.LinkHandler;
import net.rasanovum.viaromana.init.DataInit;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.path.PathGraph;

import net.rasanovum.viaromana.storage.level.LevelDataManager;
import net.rasanovum.viaromana.storage.level.LevelPixelTrackedData;
import net.rasanovum.viaromana.tags.TagGenerator;
import net.rasanovum.viaromana.util.VersionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(
            method = "onBlockStateChange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD")
    )
    private void onBlockStateChange(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        if (oldState == newState) return;

        if (oldState.is(TagGenerator.WARP_BLOCK_TAG)) {
            LinkHandler.handleSignDestruction((ServerLevel) (Object) this, pos);
        }

        ServerLevel world = (ServerLevel) (Object) this;
        ChunkPos chunkPos = new ChunkPos(pos);

        if (!LevelDataManager.isPixelChunkTracked(world, chunkPos)) return;

        LevelChunk levelChunk = world.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (levelChunk == null) return;

        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        int surfaceY = levelChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, localX, localZ);

        if (pos.getY() < surfaceY - 1) return; // TODO: Figure out a method to dirty chunk when changing blocks below a transparent motion blocking block (e.g. block below glass roof)

        if (oldState.getMapColor(world, pos).equals(newState.getMapColor(world, pos))) return;

        if (newState.getCollisionShape(world, pos).isEmpty() && oldState.getCollisionShape(world, pos).isEmpty()) return; // TODO: Figure out how to correctly handle snowfall (and similar) in a way that doesn't destroy TPS

        PathGraph graph = PathGraph.getInstance(world);
        if (graph == null) return;

        List<PathGraph.NetworkCache> networks = graph.findNetworksForChunk(chunkPos);
        if (networks.isEmpty()) return;

//        ViaRomana.LOGGER.info("Marking block at {} dirty.", pos);

        ServerMapCache.markChunkDirty(world, chunkPos, networks);
    }
}