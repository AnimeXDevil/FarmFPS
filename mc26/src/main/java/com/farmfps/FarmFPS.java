package com.farmfps;

import com.farmfps.client.ClientTicker;
import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** 26.2 port, phase 2: config + tick loop + item/entity/particle/block-entity hooks. */
public class FarmFPS implements ClientModInitializer {
    public static Path configPath;

    @Override
    public void onInitializeClient() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("farmfps.json");
        FarmConfig.load(configPath);
        HideManager.rebuild();
        ClientTickEvents.START_CLIENT_TICK.register(ClientTicker::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTicker::onEndTick);
    }

    public static void saveConfig() {
        FarmConfig.INSTANCE.clamp();
        FarmConfig.save(configPath);
    }
}
