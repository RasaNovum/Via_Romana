package net.rasanovum.viaromana.tags;

import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import pers.solid.brrp.v1.api.RuntimeResourcePack;
import net.minecraft.server.packs.PackType;

/**
 * Generates block, entity, and dimension tag files using BRRP.
 */
public class TagGenerator {

    public static void generateAllTags(RuntimeResourcePack pack) {
        try {
            pack.clearResources(PackType.SERVER_DATA);
            
            TagKey<Block> pathBlockTagKey = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("via_romana", "path_block"));
            TagKey<Block> warpBlockTagKey = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("via_romana", "warp_block"));

            TagBuilder pathTagBuilder = generatePathBlockTagBuilder();
            TagBuilder warpTagBuilder = generateWarpBlockTagBuilder();

            pack.addTag(pathBlockTagKey, pathTagBuilder);
            pack.addTag(warpBlockTagKey, warpTagBuilder);

            ViaRomana.LOGGER.info("Successfully generated dynamic tags to runtime resource pack");
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to generate tag files", e);
        }
    }

    private static TagBuilder generatePathBlockTagBuilder() {
        TagBuilder tagBuilder = generateBlockTagBuilder(CommonConfig.path_block_ids, CommonConfig.path_block_tags, CommonConfig.path_block_strings);
        return tagBuilder;
    }

    private static TagBuilder generateWarpBlockTagBuilder() {
        TagBuilder tagBuilder = generateBlockTagBuilder(CommonConfig.warp_block_ids, CommonConfig.warp_block_tags, null);
        return tagBuilder;
    }

    private static TagBuilder generateBlockTagBuilder(List<String> explicitIds, List<String> tagStrings, List<String> searchStrings) {
        TagBuilder tagBuilder = TagBuilder.create();

        // Add explicit block IDs from config
        for (String blockId : explicitIds) {
            if (!blockId.isEmpty() && !isBlacklistedBlock(blockId)) {
                try {
                    ResourceLocation blockLocation = ResourceLocation.parse(blockId);
                    tagBuilder.addElement(blockLocation);
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Invalid block ID in config: {}", blockId);
                }
            }
        }

        // Add tag references from config
        for (String tagString : tagStrings) {
            if (!tagString.isEmpty() && isModLoadedForTag(tagString) && !isBlacklistedBlock(tagString)) {
                try {
                    ResourceLocation tagLocation = ResourceLocation.parse(tagString);
                    tagBuilder.addTag(tagLocation);
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Invalid tag reference in config: {}", tagString);
                }
            }
        }

        // Add blocks from string matching in config
        if (searchStrings != null && !searchStrings.isEmpty()) {
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                String blockIdString = blockId.toString();

                if (isBlacklistedBlock(blockIdString)) continue;

                for (String searchString : searchStrings) {
                    if (!searchString.isEmpty() && blockIdString.contains(searchString.toLowerCase())) {
                        tagBuilder.addElement(blockId);
                        break;
                    }
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

    private static boolean isBlacklistedBlock(String blockIdString) {
        for (String blacklistString : CommonConfig.block_string_blacklist) {
            if (!blacklistString.isEmpty() && blockIdString.contains(blacklistString.toLowerCase())) {
                ViaRomana.LOGGER.debug("Blacklisted block: '{}' from Via Romana block tag", blockIdString);
                return true;
            }
        }
        return false;
    }
}