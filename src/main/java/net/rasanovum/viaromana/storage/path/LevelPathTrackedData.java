package net.rasanovum.viaromana.storage.path;

import dev.corgitaco.dataanchor.data.registry.TrackedDataKey;
import dev.corgitaco.dataanchor.data.type.level.ServerLevelTrackedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.rasanovum.viaromana.path.Node;
import net.rasanovum.viaromana.path.PathGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side tracked data for path networks per level.
 */
public class LevelPathTrackedData extends ServerLevelTrackedData {
    private final PathGraph graph = new PathGraph();

    public LevelPathTrackedData(TrackedDataKey<? extends ServerLevelTrackedData> trackedDataKey, ServerLevel level) {
        super((TrackedDataKey<ServerLevelTrackedData>) trackedDataKey, level);
    }

    @Override
    public CompoundTag save() {
        if (graph.size() == 0) return null;
        CompoundTag tag = new CompoundTag();
        graph.serialize(tag);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        graph.deserialize(tag);
    }

    public PathGraph getGraph() {
        return graph;
    }

    public void clearAll() {
        List<Node> nodesToRemove = new ArrayList<>(graph.nodesView());
        for (Node node : nodesToRemove) {
            graph.removeNode(node);
        }
        markDirty();
    }
}
