package net.rasanovum.viaromana.mixins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.rasanovum.viaromana.storage.player.PlayerData;
import net.rasanovum.viaromana.teleport.ServerTeleportHandler;
import net.rasanovum.viaromana.teleport.TeleportHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Level level = player.level();

        if (!player.isAlive() || player.connection == null || !player.connection.isAcceptingMessages()) return;

        TeleportHelper.effect(level, player);
        TeleportHelper.cycle(level, player);

        double fadeAmount = PlayerData.getFadeAmount(player);

        // Doesn't seem super robust but hasn't failed yet lol
        if (fadeAmount == 10 && PlayerData.isFadeIncrease(player)) {
            ServerTeleportHandler.executeTeleportation(player);
        }
    }
}