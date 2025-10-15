package net.rasanovum.viaromana.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.Registries;
import net.rasanovum.viaromana.ViaRomana;
import net.rasanovum.viaromana.items.ChartingMap;
import net.rasanovum.viaromana.util.VersionUtils;
import net.minecraft.core.Holder;

//? if neoforge {
/*import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
*///?} else if forge {
/*import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;
*///?} else if fabric
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public class ItemInit {
    //? if fabric {
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
    //?} else if neoforge || forge {
    /*public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ViaRomana.MODID);

    //? if neoforge {
    /^public static final Holder<Item> CHARTING_MAP = ITEMS.register(
            "charting_map",
            () -> new ChartingMap(new Item.Properties())
    );
    ^///?} else if forge {
    /^public static final RegistryObject<ChartingMap> CHARTING_MAP = ITEMS.register(
            "charting_map",
            () -> new ChartingMap(new Item.Properties())
    );
    ^///?}

    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, VersionUtils.getLocation("minecraft:tools_and_utilities"));

    //? if neoforge {
    /^public static void onBuildContents(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == TOOLS_AND_UTILITIES_KEY) {
            event.insertAfter(Items.MAP.getDefaultInstance(), new ItemStack(CHARTING_MAP), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
    ^///?} else if forge {
    /^public static void onBuildContents(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CHARTING_MAP.get());
        }
    }
    ^///?}
    *///?}
}