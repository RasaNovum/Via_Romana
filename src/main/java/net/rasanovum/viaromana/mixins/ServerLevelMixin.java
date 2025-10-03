package net.rasanovum.viaromana.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rasanovum.viaromana.map.ServerMapCache;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    /**
     * Injects into all block state changes to update terrain PNGs in real-time.
     * This keeps the terrain cache accurate as the world is modified.
     */
    @Inject(
        method = "onBlockStateChange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At("HEAD")
    )
    private void onBlockStateChange(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        ServerLevel world = (ServerLevel) (Object) this;
        ChunkPos chunkPos = new ChunkPos(pos);

        LevelChunk levelChunk = world.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (levelChunk == null) return;

        ServerMapCache.markChunkDirty(world, chunkPos);
    }
}