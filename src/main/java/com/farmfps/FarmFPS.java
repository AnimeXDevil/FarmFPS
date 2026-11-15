package com.farmfps;

import com.farmfps.client.ClientTicker;
import com.farmfps.client.gui.ConfigScreen;
import com.farmfps.core.FarmConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class FarmFPS implements ClientModInitializer {
    public static final String MOD_ID = "farmfps";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Path configPath;

    @Override
    public void onInitializeClient() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("farmfps.json");
        FarmConfig.load(configPath);

        ClientTickEvents.START_CLIENT_TICK.register(ClientTicker::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTicker::onEndTick);

        // "/farmfps" opens the settings screen. Useful when Mod Menu is not installed,
        // and easy to type on a phone.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("farmfps").executes(ctx -> {
                MinecraftClient mc = ctx.getSource().getClient();
                mc.execute(() -> mc.setScreen(new ConfigScreen(null)));
                return 1;
            }))
        );

        LOGGER.info("FarmFPS initialized");
    }

    public static void saveConfig() {
        if (configPath != null) {
            FarmConfig.save(configPath);
        }
    }
}
