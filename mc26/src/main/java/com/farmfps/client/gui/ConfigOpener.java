package com.farmfps.client.gui;

import net.minecraft.client.gui.screens.Screen;

/** The one settings screen, opened by /farmfps and by the Mod Menu config button. */
public final class ConfigOpener {
    private ConfigOpener() {
    }

    public static Screen open(Screen parent) {
        return new ConfigScreen(parent);
    }
}
