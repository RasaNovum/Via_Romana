package net.rasanovum.viaromana.forge.client.gui;

import net.rasanovum.viaromana.PlatformUtils;
import net.rasanovum.viaromana.client.toasts.ToastNormalToast;
import net.rasanovum.viaromana.client.toasts.ToastPatchouliToast;
import net.rasanovum.viaromana.forge.ViaRomanaMod;
import net.rasanovum.viaromana.forge.network.ViaRomanaModClientPacketHandler;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.rasanovum.viaromana.variables.VariableAccess;

import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ViaRomanaMod.MODID, value = Dist.CLIENT)
public class ToastOverlay {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

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
