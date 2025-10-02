package net.rasanovum.viaromana.terrain;

import net.minecraft.world.level.block.Block;

//? if neoforge {
public interface ValueView<T> {
    T byId(int id);
}
//?}