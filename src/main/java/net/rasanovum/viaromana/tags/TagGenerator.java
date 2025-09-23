package net.rasanovum.viaromana.tags;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.configuration.ViaRomanaConfig;
import pers.solid.brrp.v1.api.RuntimeResourcePack;
import net.minecraft.server.packs.PackType;

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
        JsonObject tagJson = new JsonObject();
        tagJson.addProperty("replace", false);
        JsonArray values = new JsonArray();

        // Add explicit block IDs from config
        for (String blockId : ViaRomanaConfig.path_block_ids) {
            if (!blockId.isEmpty()) {
                values.add(blockId);
            }
        }

        // Add tag references from config
        for (String tagString : ViaRomanaConfig.path_block_tags) {
            if (!tagString.isEmpty() && isModLoadedForTag(tagString)) {
                values.add("#" + tagString);
            }
        }

        // Add blocks from string matching in config
        if (!ViaRomanaConfig.path_block_strings.isEmpty()) {
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                String blockIdString = blockId.toString();

                for (String searchString : ViaRomanaConfig.path_block_strings) {
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

    private static JsonObject generateWarpBlockTagJson() {
        JsonObject tagJson = new JsonObject();
        tagJson.addProperty("replace", false);
        JsonArray values = new JsonArray();

        // Add explicit block IDs from config
        for (String blockId : ViaRomanaConfig.warp_block_ids) {
            if (!blockId.isEmpty()) {
                values.add(blockId);
            }
        }

        // Add tag references from config
        for (String tagString : ViaRomanaConfig.warp_block_tags) {
            if (!tagString.isEmpty() && isModLoadedForTag(tagString)) {
                values.add("#" + tagString);
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
}