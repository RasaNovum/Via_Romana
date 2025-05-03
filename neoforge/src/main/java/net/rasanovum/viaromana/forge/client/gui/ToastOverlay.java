package net.rasanovum.viaromana.forge.client.gui;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.rasanovum.viaromana.PlatformUtils;
import net.rasanovum.viaromana.client.toasts.ToastNormalToast;
import net.rasanovum.viaromana.client.toasts.ToastPatchouliToast;
import net.rasanovum.viaromana.forge.ViaRomanaMod;
import net.rasanovum.viaromana.forge.network.ViaRomanaModClientPacketHandler;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;

@EventBusSubscriber(modid = ViaRomanaMod.MODID, value = Dist.CLIENT)
public class ToastOverlay {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player entity = mc.player;
        if (entity == null)
            return;

        if (VariableAccess.playerVariables.isAwaitingToast(entity)) {
            if (PlatformUtils.isModLoaded("patchouli"))
                mc.getToasts().addToast(new ToastPatchouliToast());
            else
                mc.getToasts().addToast(new ToastNormalToast());

            VariableAccess.playerVariables.setAwaitingToast(entity, false);
            VariableAccess.playerVariables.syncAndSave(entity);
            ViaRomanaModClientPacketHandler.sendPlayerVariablesToServer(ViaRomanaModVariables.getPlayerVariables(entity));
        }
    }
}
