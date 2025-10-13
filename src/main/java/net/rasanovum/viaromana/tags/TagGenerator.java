package net.rasanovum.viaromana.tags;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.mehvahdjukaar.moonlight.api.resources.SimpleTagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicDataPack;
import net.rasanovum.viaromana.loaders.Platform;
import net.rasanovum.viaromana.util.VersionUtils;

/**
 * Generates block tags to Moonlight's DynamicDataPack.
 * Called on init and on data pack reload.
 */
public class TagGenerator {
    @SuppressWarnings("removal")
    public static void generateAllTags(DynamicDataPack pack) {
        try {
            pack.setClearOnReload(true);
            
            TagKey<Block> pathBlockTagKey = TagKey.create(Registries.BLOCK, VersionUtils.getLocation("via_romana:path_block"));
            TagKey<Block> warpBlockTagKey = TagKey.create(Registries.BLOCK, VersionUtils.getLocation("via_romana:warp_block"));

            SimpleTagBuilder pathTagBuilder = generatePathBlockTagBuilder(pathBlockTagKey);
            SimpleTagBuilder warpTagBuilder = generateWarpBlockTagBuilder(warpBlockTagKey);

            pack.addTag(pathTagBuilder, Registries.BLOCK);
            pack.addTag(warpTagBuilder, Registries.BLOCK);

            ViaRomana.LOGGER.info("Successfully generated dynamic tags to Moonlight DynamicResourcePack");
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to generate tag files", e);
        }
    }

    private static SimpleTagBuilder generatePathBlockTagBuilder(TagKey<Block> tagKey) {
        return generateBlockTagBuilder(tagKey, CommonConfig.path_block_ids, CommonConfig.path_block_tags, CommonConfig.path_block_strings);
    }

    private static SimpleTagBuilder generateWarpBlockTagBuilder(TagKey<Block> tagKey) {
        return generateBlockTagBuilder(tagKey, CommonConfig.warp_block_ids, CommonConfig.warp_block_tags, null);
    }

    private static SimpleTagBuilder generateBlockTagBuilder(TagKey<Block> tagKey, List<String> explicitIds, List<String> tagStrings, List<String> searchStrings) {
        SimpleTagBuilder tagBuilder = SimpleTagBuilder.of(tagKey);

        // Add explicit block IDs from config
        for (String blockId : explicitIds) {
            if (!blockId.isEmpty() && !isBlacklistedBlock(blockId)) {
                try {
                    ResourceLocation blockLocation = VersionUtils.getLocation(blockId);
                    tagBuilder.addElement(blockLocation);
                    ViaRomana.LOGGER.debug("Added explicit block {} to tag {}", blockId, tagKey.location());
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Invalid block ID in config: {}", blockId);
                }
            }
        }

        // Add tag references from config
        for (String tagString : tagStrings) {
            if (!tagString.isEmpty() && isModLoadedForTag(tagString) && !isBlacklistedBlock(tagString)) {
                try {
                    ResourceLocation tagLocation = VersionUtils.getLocation(tagString);
                    tagBuilder.addTag(tagLocation);
                    ViaRomana.LOGGER.debug("Added tag reference {} to tag {}", tagString, tagKey.location());
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
                        ViaRomana.LOGGER.debug("Added block {} via string match '{}' to tag {}", blockIdString, searchString, tagKey.location());
                        break;
                    }
                }
            }
        }

        ViaRomana.LOGGER.info("Generated tag {}", tagKey.location());
        return tagBuilder;
    }

    private static boolean isModLoadedForTag(String tagString) {
        try {
            ResourceLocation tagLocation = VersionUtils.getLocation(tagString);
            String modId = tagLocation.getNamespace();
            return "minecraft".equals(modId) || Platform.INSTANCE.isModLoaded(modId);
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