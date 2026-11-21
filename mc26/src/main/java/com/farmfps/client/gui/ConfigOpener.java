package com.farmfps.client.gui;

import net.minecraft.client.gui.screens.Screen;

/**
 * The settings screen for 26.x. The built-in screen is not ported yet, so this returns null and
 * callers fall back to the /farmfps commands.
 */
public final class ConfigOpener {
    private ConfigOpener() {
    }

    public static Screen open(Screen parent) {
        return null;
    }
}
