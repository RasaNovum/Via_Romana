package net.rasanovum.viaromana.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class HudMessageManager {
    private static final Queue<Component> messageQueue = new ConcurrentLinkedQueue<>();
    private static final List<String> finalMessageKeys = List.of(
        "message.via_romana.cancel_charting",
        "message.via_romana.finish_charting",
        "message.via_romana.cancel_path_dimension",
        "message.via_romana.invalid_dimension"
    );
    private static int ticksRemaining = 0;

    // TODO: Figure out a better way to handle message durations
    private static final int MESSAGE_DURATION_TICKS = 5;

    /**
     * Adds a message to the queue to be displayed on the HUD.
     *
     * @param langKey The translation key for the message.
     */
    public static void queueMessage(String langKey) {
        if (messageQueue.stream().anyMatch(messageItem -> messageItem.getContents() instanceof TranslatableContents translatableContents && finalMessageKeys.contains(translatableContents.getKey()))) return;

        Component newMessage = Component.translatable(langKey);

        if (finalMessageKeys.contains(langKey)) messageQueue.clear();

        if (!messageQueue.contains(newMessage)) {
            messageQueue.add(newMessage);
        }
    }

    /**
     * Adds a message to the queue to be displayed on the HUD.
     *
     * @param message The component message to display.
     */
    public static void queueMessage(Component message) {
        if (messageQueue.stream().anyMatch(messageItem -> messageItem.getContents() instanceof TranslatableContents translatableContents && finalMessageKeys.contains(translatableContents.getKey()))) return;

        if (message.getContents() instanceof TranslatableContents translatableContents) {
            String key = translatableContents.getKey();
            if (finalMessageKeys.contains(key)) messageQueue.clear();
        }

        boolean alreadyExists = false;
        if (message.getContents() instanceof TranslatableContents translatableContents) {
            String key = translatableContents.getKey();
            alreadyExists = messageQueue.stream().anyMatch(existing -> {
                if (existing.getContents() instanceof TranslatableContents existingTrans) {
                    return existingTrans.getKey().equals(key);
                }
                return false;
            });
        } else {
            alreadyExists = messageQueue.contains(message);
        }

        if (!alreadyExists) {
            messageQueue.add(message);
        }
    }

    public static void onClientTick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
            return;
        }

        if (!messageQueue.isEmpty()) {
            Component nextMessage = messageQueue.poll();
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null) {
                mc.player.displayClientMessage(nextMessage, true);
                ticksRemaining = MESSAGE_DURATION_TICKS;
            }
        }
    }
}