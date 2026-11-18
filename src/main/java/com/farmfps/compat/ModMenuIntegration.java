package com.farmfps.compat;

import com.farmfps.client.gui.ConfigOpener;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Only compiled when modmenu_version is set in gradle.properties (see build.gradle).
 * Without it this folder is excluded from the build and the modmenu entrypoint is left empty.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigOpener::open;
    }
}
