package net.rasanovum.viaromana.fabric.client.gui;

import net.rasanovum.viaromana.PlatformUtils;
import net.rasanovum.viaromana.variables.VariableAccess;
import net.rasanovum.viaromana.client.toasts.ToastNormalToast;
import net.rasanovum.viaromana.client.toasts.ToastPatchouliToast;
import net.rasanovum.viaromana.fabric.network.ViaRomanaModClientPacketHandler;
import net.rasanovum.viaromana.network.ViaRomanaModVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class ToastOverlay {    
    public static void render(GuiGraphics guiGraphics, float tickDelta) {
        Player entity = Minecraft.getInstance().player;
        if (entity == null)
            return;
        
		if (VariableAccess.playerVariables.isAwaitingToast(entity)) {
            if (PlatformUtils.isModLoaded("patchouli"))
                Minecraft.getInstance().getToasts().addToast(new ToastPatchouliToast());
            else
                Minecraft.getInstance().getToasts().addToast(new ToastNormalToast());

            VariableAccess.playerVariables.setAwaitingToast(entity, false);
            VariableAccess.playerVariables.syncAndSave(entity);
            ViaRomanaModClientPacketHandler.sendPlayerVariablesToServer(ViaRomanaModVariables.getPlayerVariables(entity));
        }
    }
}

