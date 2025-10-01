package net.rasanovum.viaromana.tags;

import net.mehvahdjukaar.moonlight.api.resources.pack.DynServerResourcesGenerator;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicDataPack;
import net.mehvahdjukaar.moonlight.api.resources.pack.ResourceGenTask;
import eu.midnightdust.lib.config.MidnightConfig;
import net.rasanovum.viaromana.ViaRomana;

import org.apache.logging.log4j.Logger;
import java.util.function.Consumer;

@SuppressWarnings("removal")
public class ServerResourcesGenerator extends DynServerResourcesGenerator {
    public ServerResourcesGenerator(DynamicDataPack pack) {
        super(pack);
    }

    @Override
    public Logger getLogger() {
        return ViaRomana.LOGGER;
    }

    @Override
    public void regenerateDynamicAssets(Consumer<ResourceGenTask> executor) {
        ResourceGenTask task = (manager, sink) -> {
            try {
                MidnightConfig.loadValuesFromJson(ViaRomana.MODID);
                TagGenerator.generateAllTags(this.getPack());
                ViaRomana.LOGGER.info("Successfully regenerated dynamic server resources.");

            } catch (Exception e) {
                ViaRomana.LOGGER.error("Failed to generate dynamic server resources.", e);
            }
        };

        executor.accept(task);
    }
}