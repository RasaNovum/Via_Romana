package net.rasanovum.viaromana.client.network;

import net.minecraft.client.Minecraft;
import net.rasanovum.viaromana.client.FadeManager;
import net.rasanovum.viaromana.client.gui.ChartingScreen;
import net.rasanovum.viaromana.client.gui.TeleportMapScreen;
import net.rasanovum.viaromana.client.gui.WarpBlockScreen;
import net.rasanovum.viaromana.client.MapClient;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.rasanovum.viaromana.network.packets.*;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class ClientPacketHandler {
    public static void handleOpenChartingScreen(PacketContext<OpenChartingScreenS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            net.minecraft.network.chat.Component title =
            net.minecraft.network.chat.Component.translatable("gui.viaromana.charting_screen.title");
            ChartingScreen screen = new ChartingScreen(title);
            Minecraft.getInstance().setScreen(screen);
        }
    }

    public static void handleOpenLinkSignScreen(PacketContext<OpenLinkSignScreenS2C> ctx) {
//        if (Side.CLIENT.equals(ctx.side())) {
//            Minecraft.getInstance().setScreen(new WarpBlockScreen(ctx.message().blockPos()));
//        }
    }

    public static void handleOpenWarpBlockScreen(PacketContext<OpenWarpBlockScreenS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            Minecraft.getInstance().setScreen(new WarpBlockScreen(ctx.message().blockPos()));
        }
    }

    public static void handleTeleportFade(PacketContext<TeleportFadeS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            FadeManager.startFade(
                    ctx.message().fadeUpTicks(),
                    ctx.message().holdTicks(),
                    ctx.message().fadeDownTicks(),
                    ctx.message().footstepInterval()
            );
        }
    }

    public static void handleSignValidationResponse(PacketContext<SignValidationResponseS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof TeleportMapScreen screen) {
                screen.handleSignValidation(ctx.message().nodePos(), ctx.message().isValid());
            }
        }
    }

    public static void handleDestinationResponse(PacketContext<DestinationResponseS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                TeleportMapScreen screen = new TeleportMapScreen(ctx.message());
                mc.setScreen(screen);
            });
        }
    }

    public static void handleMapResponse(PacketContext<MapResponseS2C> ctx) {
        if (Side.CLIENT.equals(ctx.side())) {
            MapClient.handleMapResponse(ctx.message());
        }
    }
}