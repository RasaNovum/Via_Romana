package net.rasanovum.viaromana.speed;

import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FieldOfViewHelper {
    public static boolean shouldIgnoreProximitySpeed(Player player) {
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() && player.isScoping()) return false;

        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        return attribute != null && attribute.getModifier(SpeedHandler.PROXIMITY_SPEED_ID) != null;
    }

    public static float getProximityFovCorrection(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return 1.0F;

        double walkSpeed = player.getAbilities().getWalkingSpeed();
        if (walkSpeed == 0) return 1.0F;

        double currentSpeed = attribute.getValue();
        double cleanSpeed = calculateAttributeValueSkipping(attribute);

        float currentFactor = (float) ((currentSpeed / walkSpeed + 1.0) / 2.0);
        float cleanFactor = (float) ((cleanSpeed / walkSpeed + 1.0) / 2.0);

        if (currentFactor == 0) return 1.0F;

        return cleanFactor / currentFactor;
    }

    private static double calculateAttributeValueSkipping(AttributeInstance attribute) {
        double baseValue = attribute.getBaseValue();

        Map<AttributeModifier.Operation, Set<AttributeModifier>> operationToModifiers = Stream.of(AttributeModifier.Operation.values())
                .collect(Collectors.toMap(
                        op -> op,
                        op -> Sets.newHashSet()
                ));

        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (!modifier.id().equals(SpeedHandler.PROXIMITY_SPEED_ID)) {
                operationToModifiers.get(modifier.operation()).add(modifier);
            }
        }

        for (AttributeModifier mod : operationToModifiers.get(AttributeModifier.Operation.ADD_VALUE)) {
            baseValue += mod.amount();
        }

        double value = baseValue;

        for (AttributeModifier mod : operationToModifiers.get(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
            value += baseValue * mod.amount();
        }

        for (AttributeModifier mod : operationToModifiers.get(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
            value *= (1.0D + mod.amount());
        }

        return attribute.getAttribute().value().sanitizeValue(value);
    }
}