package net.rasanovum.viaromana;

import eu.midnightdust.lib.config.MidnightConfig;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicDataPack;
import net.rasanovum.viaromana.init.*;
import net.rasanovum.viaromana.network.PacketRegistration;
import net.rasanovum.viaromana.tags.ServerResourcesGenerator;
import net.rasanovum.viaromana.util.VersionUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViaRomana {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "via_romana";
    @SuppressWarnings("removal")
    public static final DynamicDataPack DYNAMIC_PACK = new DynamicDataPack(VersionUtils.getLocation(MODID, "dynamic_tags"));

    public static void initialize() {
        LOGGER.info("Initializing ViaRomanaMod");

        MidnightConfig.init(MODID, CommonConfig.class);

        new PacketRegistration().init();

        BlockInit.load();
        EffectInit.load();
        ItemInit.load();
        TriggerInit.load();
        DataInit.load();

        ServerResourcesGenerator generator = new ServerResourcesGenerator(DYNAMIC_PACK);
        generator.register();
    }

    public void onJoin() {

    }

    public void onServerTick() {

    }

    public void onServerStart() {

    }

    public void onServerStop() {

    }

    public void onDataPackReload() {

    }

    public void onDimensionChange() {

    }

    public void registerCommands() {

    }
}