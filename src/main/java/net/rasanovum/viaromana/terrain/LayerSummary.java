package net.rasanovum.viaromana.terrain;

import java.util.BitSet;

//? if neoforge {
/*public class LayerSummary {
    public static record Raw(BitSet exists, int[] depths, int[] blocks, int[] waterDepths) {
        public Raw {
            if (waterDepths == null) {
                waterDepths = new int[256];
            }
            if (depths.length != 256 || blocks.length != 256 || waterDepths.length != 256) {
                throw new IllegalArgumentException("Arrays must be 256 elements for 16x16 grid");
            }
        }
    }
}
*///?}