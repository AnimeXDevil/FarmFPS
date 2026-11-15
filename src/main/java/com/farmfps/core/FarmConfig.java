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
    public boolean aggressiveItems = true;   // drop item spawns over the cap and skip ticking hidden items
    public boolean hideFarmEffects = true;   // hide particles and matching sounds in detected farm chunks
    public boolean actionBar = true;         // live status message while optimizing
    public boolean redstoneLock = true;      // hold back rapid visual-only redstone state flips in farm chunks
    public boolean muteRedstoneSounds = true; // mute note block / dispenser / piston / trapdoor sounds in farm chunks
    public boolean blockEntityCull = true;   // stop drawing far block entities in farm chunks

    // Limits
    public int maxItems = 200;            // rendered dropped items inside cullRadius
    public int maxXpOrbs = 60;            // rendered XP orbs inside cullRadius
    public int cullRadius = 48;           // blocks around the camera
    public int maxParticlesPerTick = 250; // particles accepted per client tick
    public int maxSameSoundPerTick = 3;   // identical sounds allowed per client tick
    public int farmSensitivity = 20;      // activity score at which a chunk counts as an active farm
    public int redstoneFlushTicks = 5;    // how often held-back redstone states are applied
    public int blockEntityDistance = 32;  // blocks; block entities farther than this are not drawn in farm chunks
    public int preset = 0;                // 0 Balanced, 1 Mega-Farm, 2 Potato PC (display only)

    public static final String[] PRESET_NAMES = {"Balanced", "Mega-Farm", "Potato PC"};

    public FarmConfig() {
    }

    public void clamp() {
        maxItems = clampInt(maxItems, 10, 2000);
        maxXpOrbs = clampInt(maxXpOrbs, 10, 1000);
        cullRadius = clampInt(cullRadius, 8, 128);
        maxParticlesPerTick = clampInt(maxParticlesPerTick, 20, 2000);
        maxSameSoundPerTick = clampInt(maxSameSoundPerTick, 1, 10);
        farmSensitivity = clampInt(farmSensitivity, 5, 200);
        redstoneFlushTicks = clampInt(redstoneFlushTicks, 2, 20);
        blockEntityDistance = clampInt(blockEntityDistance, 8, 96);
        preset = clampInt(preset, 0, PRESET_NAMES.length - 1);
    }

    /** Sets the limits for a preset. Toggles are left as the player set them. */
    public void applyPreset(int p) {
        switch (p) {
            case 1: // Mega-Farm
                maxItems = 100; maxXpOrbs = 30; cullRadius = 64; maxParticlesPerTick = 120;
                maxSameSoundPerTick = 2; farmSensitivity = 12; redstoneFlushTicks = 8; blockEntityDistance = 24;
                break;
            case 2: // Potato PC
                maxItems = 40; maxXpOrbs = 15; cullRadius = 48; maxParticlesPerTick = 60;
                maxSameSoundPerTick = 1; farmSensitivity = 8; redstoneFlushTicks = 10; blockEntityDistance = 16;
                break;
            default: // Balanced
                p = 0;
                maxItems = 200; maxXpOrbs = 60; cullRadius = 48; maxParticlesPerTick = 250;
                maxSameSoundPerTick = 3; farmSensitivity = 20; redstoneFlushTicks = 5; blockEntityDistance = 32;
                break;
        }
        preset = p;
        clamp();
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
        aggressiveItems = o.aggressiveItems;
        hideFarmEffects = o.hideFarmEffects;
        actionBar = o.actionBar;
        maxItems = o.maxItems;
        maxXpOrbs = o.maxXpOrbs;
        cullRadius = o.cullRadius;
        maxParticlesPerTick = o.maxParticlesPerTick;
        maxSameSoundPerTick = o.maxSameSoundPerTick;
        farmSensitivity = o.farmSensitivity;
        redstoneLock = o.redstoneLock;
        muteRedstoneSounds = o.muteRedstoneSounds;
        blockEntityCull = o.blockEntityCull;
        redstoneFlushTicks = o.redstoneFlushTicks;
        blockEntityDistance = o.blockEntityDistance;
        preset = o.preset;
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
