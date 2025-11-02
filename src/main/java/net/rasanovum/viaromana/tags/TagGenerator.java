package net.rasanovum.viaromana.tags;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
    public static final TagKey<Block> PATH_BLOCK_TAG = TagKey.create(Registries.BLOCK, VersionUtils.getLocation("via_romana:path_block"));
    public static final TagKey<Block> WARP_BLOCK_TAG = TagKey.create(Registries.BLOCK, VersionUtils.getLocation("via_romana:warp_block"));
    public static final TagKey<Block> LEAVES_BLOCK_TAG = TagKey.create(Registries.BLOCK, VersionUtils.getLocation("minecraft:leaves"));

    @SuppressWarnings("removal")
    public static void generateAllTags(DynamicDataPack pack, ResourceManager resourceManager) {
        try {
            pack.setClearOnReload(true);

            SimpleTagBuilder pathTagBuilder = generatePathBlockTagBuilder(resourceManager);
            SimpleTagBuilder warpTagBuilder = generateWarpBlockTagBuilder(resourceManager);

            pack.addTag(pathTagBuilder, Registries.BLOCK);
            pack.addTag(warpTagBuilder, Registries.BLOCK);

            ViaRomana.LOGGER.info("Successfully generated dynamic tags");
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to generate tag files", e);
        }
    }

    private static SimpleTagBuilder generatePathBlockTagBuilder(ResourceManager resourceManager) {
        return generateBlockTagBuilder(TagGenerator.PATH_BLOCK_TAG, CommonConfig.path_block_ids, CommonConfig.path_block_tags, CommonConfig.path_block_strings, resourceManager);
    }

    private static SimpleTagBuilder generateWarpBlockTagBuilder(ResourceManager resourceManager) {
        return generateBlockTagBuilder(TagGenerator.WARP_BLOCK_TAG, CommonConfig.warp_block_ids, CommonConfig.warp_block_tags, null, resourceManager);
    }

    private static SimpleTagBuilder generateBlockTagBuilder(TagKey<Block> tagKey, List<String> explicitIds, List<String> tagStrings, List<String> searchStrings, ResourceManager resourceManager) {
        SimpleTagBuilder tagBuilder = SimpleTagBuilder.of(tagKey);

        for (String blockId : explicitIds) {
            if (!blockId.isEmpty() && !isBlacklistedBlock(blockId)) {
                try {
                    tagBuilder.addElement(VersionUtils.getLocation(blockId));
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Invalid block ID in config: {}", blockId);
                }
            }
        }

        for (String tagString : tagStrings) {
            if (!tagString.isEmpty() && isModLoadedForTag(tagString) && !isBlacklistedBlock(tagString)) {
                try {
                    ResourceLocation tagLocation = VersionUtils.getLocation(tagString);
                    if (tagExists(resourceManager, tagLocation)) {
                        tagBuilder.addTag(tagLocation);
                    }
                } catch (Exception e) {
                    ViaRomana.LOGGER.warn("Invalid tag reference in config: {}", tagString);
                }
            }
        }

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
            ResourceLocation tagLocation = VersionUtils.getLocation(tagString);
            String modId = tagLocation.getNamespace();
            return "minecraft".equals(modId) || Platform.INSTANCE.isModLoaded(modId);
        } catch (Exception e) {
            ViaRomana.LOGGER.warn("Invalid tag format: '{}'", tagString);
            return false;
        }
    }

    private static boolean tagExists(ResourceManager resourceManager, ResourceLocation tagLocation) {
        return true;

//        if (resourceManager == null) {
//            return true;
//        }
//
//        try {
//            ResourceLocation tagFile = VersionUtils.getLocation(tagLocation.getNamespace(), "tags/block/" + tagLocation.getPath() + ".json");
//            return resourceManager.getResource(tagFile).isPresent();
//        } catch (Exception e) {
//            return false;
//        }
    }

    private static boolean isBlacklistedBlock(String blockIdString) {
        for (String blacklistString : CommonConfig.block_string_blacklist) {
            if (!blacklistString.isEmpty() && blockIdString.contains(blacklistString.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}