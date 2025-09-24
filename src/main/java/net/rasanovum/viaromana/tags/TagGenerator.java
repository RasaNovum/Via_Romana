package net.rasanovum.viaromana.tags;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import pers.solid.brrp.v1.api.RuntimeResourcePack;
import net.minecraft.server.packs.PackType;

/**
 * Generates block, entity, and dimension tag files using BRRP.
 */
public class TagGenerator {

    public static void generateAllTags(RuntimeResourcePack pack) {
        try {
            pack.clearResources(PackType.SERVER_DATA);
            
            // Create TagKey objects for the tags
            TagKey<Block> pathBlockTagKey = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("via_romana", "path_block"));
            TagKey<Block> warpBlockTagKey = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("via_romana", "warp_block"));

            // Generate TagBuilders
            TagBuilder pathTagBuilder = generatePathBlockTagBuilder();
            TagBuilder warpTagBuilder = generateWarpBlockTagBuilder();

            // Add tags using the proper BRRP method
            pack.addTag(pathBlockTagKey, pathTagBuilder);
            pack.addTag(warpBlockTagKey, warpTagBuilder);

            ViaRomana.LOGGER.info("Successfully generated dynamic tags to runtime resource pack");
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to generate tag files", e);
        }
    }

    private static TagBuilder generatePathBlockTagBuilder() {
        TagBuilder tagBuilder = TagBuilder.create();

        // Add explicit block IDs from config
        for (String blockId : ViaRomanaConfig.path_block_ids) {
            if (!blockId.isEmpty()) {
                try {
                    ResourceLocation blockLocation = ResourceLocation.parse(blockId);
                    tagBuilder.addElement(blockLocation);
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Invalid block ID in config: {}", blockId);
                }
            }
        }

        // Add tag references from config
        for (String tagString : ViaRomanaConfig.path_block_tags) {
            if (!tagString.isEmpty() && isModLoadedForTag(tagString)) {
                try {
                    ResourceLocation tagLocation = ResourceLocation.parse(tagString);
                    tagBuilder.addTag(tagLocation);
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Invalid tag reference in config: {}", tagString);
                }
            }
        }

        // Add blocks from string matching in config
        if (!ViaRomanaConfig.path_block_strings.isEmpty()) {
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                String blockIdString = blockId.toString();

                for (String searchString : ViaRomanaConfig.path_block_strings) {
                    if (!searchString.isEmpty() && blockIdString.contains(searchString.toLowerCase())) {
                        tagBuilder.addElement(blockId);
                        break;
                    }
                }
            }
        }

        return tagBuilder;
    }



    private static TagBuilder generateWarpBlockTagBuilder() {
        TagBuilder tagBuilder = TagBuilder.create();

        // Add explicit block IDs from config
        for (String blockId : ViaRomanaConfig.warp_block_ids) {
            if (!blockId.isEmpty()) {
                try {
                    ResourceLocation blockLocation = ResourceLocation.parse(blockId);
                    tagBuilder.addElement(blockLocation);
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Invalid warp block ID in config: {}", blockId);
                }
            }
        }

        // Add tag references from config
        for (String tagString : ViaRomanaConfig.warp_block_tags) {
            if (!tagString.isEmpty() && isModLoadedForTag(tagString)) {
                try {
                    ResourceLocation tagLocation = ResourceLocation.parse(tagString);
                    tagBuilder.addTag(tagLocation);
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Invalid warp tag reference in config: {}", tagString);
                }
            }
        }

        return tagBuilder;
    }



    private static boolean isModLoadedForTag(String tagString) {
        try {
            ResourceLocation tagLocation = ResourceLocation.parse(tagString);
            String modId = tagLocation.getNamespace();
            return "minecraft".equals(modId) || FabricLoader.getInstance().isModLoaded(modId);
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Invalid tag format: '{}'", tagString);
            return false;
        }
    }
}