package com.farmfps.client.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

/**
 * Opens the best available settings screen: the YetAnotherConfigLib one (tabs, hover descriptions, item lists)
 * when YACL is installed and this build includes it, otherwise the built-in screen.
 * YACL classes are only touched through reflection, so the mod loads fine without YACL.
 */
public final class ConfigOpener {
    private ConfigOpener() {
    }

    public static Screen open(Screen parent) {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("yet_another_config_lib_v3")) {
            try {
                Object screen = Class.forName("com.farmfps.compat.YaclScreen")
                    .getMethod("create", Screen.class)
                    .invoke(null, parent);
                if (screen instanceof Screen s) {
                    return s;
                }
            } catch (Throwable ignored) {
                // Fall through to the built-in screen.
            }
        }
        return new ConfigScreen(parent);
    }
}
