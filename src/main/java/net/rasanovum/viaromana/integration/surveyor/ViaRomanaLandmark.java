package net.rasanovum.viaromana.integration.surveyor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.surveyor.landmark.Landmark;
import folk.sisby.surveyor.landmark.LandmarkType;
import folk.sisby.surveyor.landmark.SimpleLandmarkType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.rasanovum.viaromana.CommonConfig;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;
import net.rasanovum.viaromana.util.VersionUtils;

import java.util.Optional;


/**
 * Landmark for displaying destinations on surveyor-compatible maps.
 */
public record ViaRomanaLandmark(BlockPos pos, Component name, DyeColor color) implements Landmark<ViaRomanaLandmark> {
    public static final LandmarkType<ViaRomanaLandmark> TYPE = new SimpleLandmarkType<>(
        VersionUtils.getLocation("via_romana:destination_landmark"),
        pos -> RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("name").forGetter(landmark ->
                Optional.ofNullable(landmark.name()).map(Component::getString)),
            Codec.STRING.optionalFieldOf("color").forGetter(landmark -> 
                Optional.ofNullable(landmark.color()).map(DyeColor::getName))
        ).apply(instance, (nameStr, colorStr) -> 
            new ViaRomanaLandmark(
                pos,
                nameStr.map(Component::literal).orElse(null),
                colorStr.map(name -> DyeColor.byName(name, DyeColor.BROWN)).orElse(DyeColor.BROWN)
            )
        ))
    );

    /**
     * Creates a landmark for a linked destination node
     */
    public static ViaRomanaLandmark createDestination(ServerLevel level, Node node, BlockPos pos) {
        Component name = node.getDestinationName()
            .map(Component::literal)
            .orElse(Component.literal("Via Romana Destination"));

        DyeColor color;
        if (CommonConfig.enable_surveyor_landmark_coloring) {
            PathGraph graph = PathGraph.getInstance(level);
            color = graph.getNetworkColor(node);
        }
        else {
            color = DyeColor.BROWN;
        }

        return new ViaRomanaLandmark(
            pos,
            name,
            color
        );
    }

    @Override
    public LandmarkType<ViaRomanaLandmark> type() {
        return TYPE;
    }

    @Override
    public BlockPos pos() {
        return pos;
    }

    @Override
    public Component name() {
        return name;
    }

    @Override
    public DyeColor color() {
        return color;
    }
}
