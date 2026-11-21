package com.farmfps;

import com.farmfps.core.FarmConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/** Phase 1 of the 26.2 port: toolchain check + config load. Hooks are ported in later phases. */
public class FarmFPS implements ClientModInitializer {
    public static Path configPath;

    @Override
    public void onInitializeClient() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("farmfps.json");
        FarmConfig.load(configPath);
    }

    public static void saveConfig() {
        FarmConfig.INSTANCE.clamp();
        FarmConfig.save(configPath);
    }
}
