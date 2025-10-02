package net.rasanovum.viaromana.init;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.world.item.CreativeModeTabs;
import net.rasanovum.viaromana.items.ChartingMap;
import net.rasanovum.viaromana.util.VersionUtils;

public class ItemInit {
    public static Item CHARTING_MAP;
    
    public static void load() {
        CHARTING_MAP = Registry.register(BuiltInRegistries.ITEM, 
            VersionUtils.getLocation("via_romana:charting_map"), 
            new ChartingMap(new Item.Properties()));
        
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.addAfter(Items.MAP, CHARTING_MAP);
        });
    }
}
