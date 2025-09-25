package net.rasanovum.viaromana.client;

import eu.midnightdust.lib.config.MidnightConfig;

public class ClientConfig extends MidnightConfig {

    public static final String CLIENT = "client";

    @Entry(category = CLIENT, min = 0f, max = 1f) public static float invalid_block_overlay_opacity = 0.4f;
}