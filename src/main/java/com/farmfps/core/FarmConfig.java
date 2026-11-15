package com.farmfps.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * User settings. Pure Java (no Minecraft imports) so it survives version ports.
 * Fields are plain public primitives so hot-path reads are a single field load.
 */
public final class FarmConfig {
    public static final FarmConfig INSTANCE = new FarmConfig();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Master switches
    public boolean enabled = true;
    public boolean pistonSnap = true;
    public boolean capItems = true;
    public boolean capXpOrbs = true;
    public boolean throttleParticles = true;
    public boolean throttleSounds = true;

    // Limits
    public int maxItems = 150;            // rendered dropped items inside cullRadius
    public int maxXpOrbs = 60;            // rendered XP orbs inside cullRadius
    public int cullRadius = 48;           // blocks around the camera
    public int maxParticlesPerTick = 250; // particles accepted per client tick
    public int maxSameSoundPerTick = 3;   // identical sounds allowed per client tick
    public int farmSensitivity = 20;      // activity score at which a chunk counts as an active farm

    public FarmConfig() {
    }

    public void clamp() {
        maxItems = clampInt(maxItems, 10, 2000);
        maxXpOrbs = clampInt(maxXpOrbs, 10, 1000);
        cullRadius = clampInt(cullRadius, 8, 128);
        maxParticlesPerTick = clampInt(maxParticlesPerTick, 20, 2000);
        maxSameSoundPerTick = clampInt(maxSameSoundPerTick, 1, 10);
        farmSensitivity = clampInt(farmSensitivity, 5, 200);
    }

    private static int clampInt(int v, int min, int max) {
        return v < min ? min : Math.min(v, max);
    }

    private void copyFrom(FarmConfig o) {
        enabled = o.enabled;
        pistonSnap = o.pistonSnap;
        capItems = o.capItems;
        capXpOrbs = o.capXpOrbs;
        throttleParticles = o.throttleParticles;
        throttleSounds = o.throttleSounds;
        maxItems = o.maxItems;
        maxXpOrbs = o.maxXpOrbs;
        cullRadius = o.cullRadius;
        maxParticlesPerTick = o.maxParticlesPerTick;
        maxSameSoundPerTick = o.maxSameSoundPerTick;
        farmSensitivity = o.farmSensitivity;
        clamp();
    }

    /** Loads the file if it exists; otherwise writes defaults. Never throws. */
    public static void load(Path file) {
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    FarmConfig loaded = GSON.fromJson(r, FarmConfig.class);
                    if (loaded != null) {
                        INSTANCE.copyFrom(loaded);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            // Corrupt file: keep defaults, overwrite below.
        }
        save(file);
    }

    /** Never throws. */
    public static void save(Path file) {
        try {
            INSTANCE.clamp();
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (IOException | RuntimeException ignored) {
        }
    }
}
