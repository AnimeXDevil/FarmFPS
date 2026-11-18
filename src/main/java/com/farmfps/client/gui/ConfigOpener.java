package com.farmfps.client.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

/**
 * Opens the best available settings screen: the Cloth Config one (with tooltips and list editors)
 * when Cloth Config is installed and this build includes it, otherwise the built-in screen.
 * Cloth classes are only touched through reflection, so the mod loads fine without Cloth Config.
 */
public final class ConfigOpener {
    private ConfigOpener() {
    }

    public static Screen open(Screen parent) {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("cloth-config") || loader.isModLoaded("cloth-config2")) {
            try {
                Object screen = Class.forName("com.farmfps.compat.ClothScreen")
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
