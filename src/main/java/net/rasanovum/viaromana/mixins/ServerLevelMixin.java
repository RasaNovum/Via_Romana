package net.rasanovum.viaromana.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.rasanovum.viaromana.map.ServerMapCache;
import net.rasanovum.viaromana.path.PathGraph;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(
            method = "onBlockStateChange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD")
    )
    private void onBlockStateChange(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        ServerLevel world = (ServerLevel) (Object) this;
        ChunkPos chunkPos = new ChunkPos(pos);

        LevelChunk levelChunk = world.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (levelChunk == null) return;

        PathGraph graph = PathGraph.getInstance(world);
        if (graph == null || graph.findNetworksForChunk(chunkPos).isEmpty()) return;

        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        int surfaceY = levelChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, localX, localZ);

        if (pos.getY() >= surfaceY - 2) {
            ServerMapCache.markChunkDirty(world, chunkPos);
        }
    }
}