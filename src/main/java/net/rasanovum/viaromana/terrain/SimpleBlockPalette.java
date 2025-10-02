package net.rasanovum.viaromana.terrain;

import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//? if neoforge {
public class SimpleBlockPalette implements ValueView<Block> {
    private final ArrayList<Block> idToBlock = new ArrayList<>(); // Growable: index = local ID
    private final Map<Integer, Integer> globalIdToLocal = new HashMap<>(); // Global Block ID -> local ID

    /**
     * Builder-style: Add a global block ID, get/assign local ID.
     * Call during computation to build palette.
     */
    public int addOrGet(int globalBlockId) {
        Integer local = globalIdToLocal.get(globalBlockId);
        if (local != null) {
            return local;
        }
        // Validate ID with MC's reverse lookup
        Block block = Block.BLOCK_BY_ID[globalBlockId];
        if (block == null) {
            throw new IllegalArgumentException("Invalid global block ID: " + globalBlockId);
        }
        local = idToBlock.size();
        globalIdToLocal.put(globalBlockId, local);
        idToBlock.add(block);
        return local;
    }

    /**
     * ValueView.byId: Local ID -> Block (null if invalid).
     */
    @Override
    public Block byId(int localId) {
        return localId >= 0 && localId < idToBlock.size() ? idToBlock.get(localId) : null;
    }

    // For serialization in Phase 2: Get raw global IDs for storage
    public int[] getGlobalIds() {
        int[] globals = new int[idToBlock.size()];
        for (int i = 0; i < idToBlock.size(); i++) {
            globals[i] = Block.getId(idToBlock.get(i));
        }
        return globals;
    }

    // Size for debugging
    public int size() {
        return idToBlock.size();
    }
}
//?}