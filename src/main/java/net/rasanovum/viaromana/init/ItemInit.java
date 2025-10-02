package net.rasanovum.viaromana.init;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.rasanovum.viaromana.items.ChartingMap;
import net.rasanovum.viaromana.util.VersionUtils;

public class ItemInit {
    public static Item CHARTING_MAP;
    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, VersionUtils.getLocation("minecraft:tools_and_utilities"));
    
    public static void load() {
        CHARTING_MAP = Registry.register(BuiltInRegistries.ITEM, 
            VersionUtils.getLocation("via_romana:charting_map"), 
            new ChartingMap(new Item.Properties()));
        
        ItemGroupEvents.modifyEntriesEvent(TOOLS_AND_UTILITIES_KEY).register(content -> {
            content.addAfter(Items.MAP, CHARTING_MAP);
        });
    }
}