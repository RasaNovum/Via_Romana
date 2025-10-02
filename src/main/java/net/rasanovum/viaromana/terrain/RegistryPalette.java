package net.rasanovum.viaromana.terrain;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//? if neoforge {
/*public class RegistryPalette<T> {
    private final Registry<T> registry;
    private final ArrayList<T> idToValue = new ArrayList<>(); // Growable: index = local ID
    private final Map<Integer, Integer> globalIdToLocal = new HashMap<>(); // Global ID -> local ID
    private final ValueView valueView;

    public RegistryPalette(Registry<T> registry) {
        this.registry = registry;
        this.valueView = new ValueView();
    }

    /^*
     * Builder-style: Add a global ID, get/assign local ID.
     * Mirrors Surveyor's findOrAdd(int value).
     ^/
    public int findOrAdd(int globalId) {
        Integer local = globalIdToLocal.get(globalId);
        if (local != null) {
            return local;
        }
        // Validate ID with registry lookup
        T value = registry.byId(globalId);
        if (value == null) {
            throw new IllegalArgumentException("Invalid global ID: " + globalId);
        }
        local = idToValue.size();
        globalIdToLocal.put(globalId, local);
        idToValue.add(value);
        return local;
    }

    /^*
     * Mirrors Surveyor's findOrAdd(T value).
     ^/
    public int findOrAdd(T value) {
        return findOrAdd(registry.getId(value));
    }

    /^*
     * Returns the ValueView (mirrors Surveyor's view() method).
     ^/
    public ValueView view() {
        return valueView;
    }

    // For serialization in Phase 2: Get raw global IDs for storage
    public int[] getGlobalIds() {
        int[] globals = new int[idToValue.size()];
        for (int i = 0; i < idToValue.size(); i++) {
            globals[i] = registry.getId(idToValue.get(i));
        }
        return globals;
    }

    // Size for debugging
    public int size() {
        return idToValue.size();
    }

    /^*
     * Inner class that mirrors Surveyor's ValueView.
     * Provides byId() method for accessing values by local ID.
     ^/
    public class ValueView {
        private final T defaultValue = registry.byId(0);

        public T byId(int localId) {
            if (localId >= idToValue.size()) {
                return defaultValue;
            }
            return localId >= 0 && localId < idToValue.size() ? idToValue.get(localId) : defaultValue;
        }

        public Registry<T> registry() {
            return registry;
        }

        public int size() {
            return idToValue.size();
        }
    }
}
*///?}