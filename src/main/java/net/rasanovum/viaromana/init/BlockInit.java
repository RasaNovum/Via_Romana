package net.rasanovum.viaromana.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.rasanovum.viaromana.blocks.WarpBlock;
import net.rasanovum.viaromana.util.VersionUtils;

public class BlockInit {
    public static final Block WARP_BLOCK = new WarpBlock(BlockBehaviour.Properties.of().strength(1.0f));

    public static void load() {
        registerBlock("warp_block", WARP_BLOCK);
    }

    private static void registerBlock(String name, Block block) {
        ResourceLocation id = VersionUtils.getLocation("via_romana:" + name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
    }
}