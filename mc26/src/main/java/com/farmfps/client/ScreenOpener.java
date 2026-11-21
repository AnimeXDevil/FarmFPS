package com.farmfps.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;

/**
 * Shows a screen on every 26.x release: 26.2 and newer have Gui.setScreen(Screen), 26.1.x has
 * Minecraft.setScreen(Screen). Looked up by reflection so one source compiles for all of them.
 */
public final class ScreenOpener {
    private ScreenOpener() {
    }

    public static void show(Minecraft mc, Screen screen) {
        try {
            Object gui = Minecraft.class.getField("gui").get(mc);
            Method m = gui.getClass().getMethod("setScreen", Screen.class);
            m.invoke(gui, screen);
            return;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // fall through to the 26.1 location
        }
        try {
            Method m = Minecraft.class.getMethod("setScreen", Screen.class);
            m.invoke(mc, screen);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // no screen API found; nothing to show
        }
    }
}
