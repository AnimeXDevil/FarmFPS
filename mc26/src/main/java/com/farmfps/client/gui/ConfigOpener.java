package com.farmfps.client.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Opens the Cloth Config settings screen. Cloth classes are only touched through reflection,
 * so the mod loads fine without Cloth Config; open() then returns null and callers print a hint.
 */
public final class ConfigOpener {
    private ConfigOpener() {
    }

    public static boolean available() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("cloth-config") || loader.isModLoaded("cloth-config2");
    }

    /** The settings screen, or null if Cloth Config is not installed. */
    public static Screen open(Screen parent) {
        if (available()) {
            try {
                Object screen = Class.forName("com.farmfps.compat.ClothScreen")
                    .getMethod("create", Screen.class)
                    .invoke(null, parent);
                if (screen instanceof Screen s) {
                    return s;
                }
            } catch (Throwable ignored) {
                // Cloth screen not compiled into this build.
            }
        }
        return null;
    }
}
