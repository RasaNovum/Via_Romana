package net.rasanovum.viaromana.tags;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.CommonConfig;
import pers.solid.brrp.v1.api.RuntimeResourcePack;
import net.minecraft.server.packs.PackType;

// import java.util.stream.StreamSupport;
// import net.minecraft.core.registries.Registries;
// import net.minecraft.tags.TagKey;

/**
 * Generates block, entity, and dimension tag files using BRRP.
 */
public class TagGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void generateAllTags(RuntimeResourcePack pack) {
        try {
            pack.clearResources(PackType.SERVER_DATA);
            
            JsonObject pathBlockTag = generatePathBlockTagJson();
            JsonObject warpBlockTag = generateWarpBlockTagJson();

            pack.addData(new ResourceLocation(ViaRomana.MODID, "tags/blocks/path_block.json"), GSON.toJson(pathBlockTag).getBytes());
            pack.addData(new ResourceLocation(ViaRomana.MODID, "tags/blocks/warp_block.json"), GSON.toJson(warpBlockTag).getBytes());

            ViaRomana.LOGGER.info("Successfully generated dynamic tags to runtime resource pack");
        } catch (Exception e) {
            ViaRomana.LOGGER.error("Failed to generate tag files", e);
        }
    }

    private static JsonObject generatePathBlockTagJson() {
        JsonObject tagJson = generateBlockTagJson(CommonConfig.path_block_ids, CommonConfig.path_block_tags, CommonConfig.path_block_strings);
        ViaRomana.LOGGER.debug("Generated path_block tag JSON: {}", GSON.toJson(tagJson));
        return tagJson;
    }

    private static JsonObject generateWarpBlockTagJson() {
        JsonObject tagJson = generateBlockTagJson(CommonConfig.warp_block_ids, CommonConfig.warp_block_tags, null);
        ViaRomana.LOGGER.debug("Generated warp_block tag JSON: {}", GSON.toJson(tagJson));
        return tagJson;
    }

    private static JsonObject generateBlockTagJson(List<String> explicitIds, List<String> tagStrings, List<String> searchStrings) {
        JsonObject tagJson = new JsonObject();
        tagJson.addProperty("replace", false);
        JsonArray values = new JsonArray();

        // Add explicit block IDs from config
        for (String blockId : explicitIds) {
            if (!blockId.isEmpty() && !isBlacklistedBlock(blockId)) {
                values.add(blockId);
            }
        }

        // Add tag references from config
        for (String tagString : tagStrings) {
            if (!tagString.isEmpty() && isModLoadedForTag(tagString) && !isBlacklistedBlock(tagString)) {
                values.add("#" + tagString);
            }
        }

        // Add tag references from config with per-block checking, removed as it runs before tags are built (I think)
        // For now I moved tag searches to string searches in config but not the most robust solution
        // for (String tagString : tagStrings) {
        //     if (!tagString.isEmpty() && isModLoadedForTag(tagString)) {
        //         try {
        //             ViaRomana.LOGGER.info("Adding blocks from tag: '{}'", tagString);
        //             ResourceLocation tagId = new ResourceLocation(tagString);
        //             TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
        //             StreamSupport.stream(BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey).spliterator(), false)
        //                 .map(holder -> BuiltInRegistries.BLOCK.getKey(holder.value()))
        //                 .map(rl -> rl.toString())
        //                 .filter(idStr -> !isBlacklistedBlock(idStr))
        //                 .forEach(idStr -> {
        //                     values.add(idStr);
        //                     ViaRomana.LOGGER.info(" - Added block: '{}'", idStr);
        //                 });
        //         } catch (Exception e) {
        //             ViaRomana.LOGGER.warn("Invalid tag: '{}'", tagString, e);
        //         }
        //     }
        // }

        // Add blocks from string matching in config
        if (searchStrings != null && !searchStrings.isEmpty()) {
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                String blockIdString = blockId.toString();

                if (isBlacklistedBlock(blockIdString)) continue;

                for (String searchString : searchStrings) {
                    if (!searchString.isEmpty() && blockIdString.contains(searchString.toLowerCase())) {
                        values.add(blockIdString);
                        break;
                    }
                }
            }
        }

        tagJson.add("values", values);
        return tagJson;
    }

    private static boolean isModLoadedForTag(String tagString) {
        try {
            ResourceLocation tagLocation = new ResourceLocation(tagString);
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