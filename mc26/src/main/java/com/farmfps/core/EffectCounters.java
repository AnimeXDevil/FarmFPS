package com.farmfps.core;

import java.util.Arrays;

/**
 * Per-tick budgets for cosmetic effects. Reset once at the start of every client tick.
 * Primitive counters only, so the hooks never allocate.
 */
public final class EffectCounters {
    private static final int SOUND_SLOTS = 256; // power of two
    private static final int SOUND_SHIFT = 32 - 8;

    private static int particlesThisTick;
    private static final int[] SOUNDS = new int[SOUND_SLOTS];

    private EffectCounters() {
    }

    public static void resetTick() {
        particlesThisTick = 0;
        Arrays.fill(SOUNDS, 0);
    }

    /** Returns true while the tick's particle budget is not yet exhausted. */
    public static boolean allowParticle() {
        return ++particlesThisTick <= FarmConfig.INSTANCE.maxParticlesPerTick;
    }

    /**
     * Returns true while fewer than the allowed number of identical sounds played this tick.
     * Different sound ids can share a slot; that only makes throttling slightly stricter.
     */
    public static boolean allowSound(int soundIdHash, int limit) {
        int slot = (soundIdHash * 0x9E3779B1) >>> SOUND_SHIFT;
        return ++SOUNDS[slot] <= limit;
    }

    /** Every redstone-component sound except the lever (which is the farm's on/off switch). */
    public static boolean isRedstoneSound(String path) {
        if (path.contains("lever")) {
            return false;
        }
        return path.contains("piston.")
            || path.contains("dispenser.")
            || path.contains("note_block.")
            || path.contains("comparator.")
            || path.contains("repeater.")
            || path.contains("button.")
            || path.contains("pressure_plate.")
            || path.contains("tripwire.")
            || path.contains("iron_door.")
            || path.contains("iron_trapdoor.")
            || path.contains("copper_bulb.")
            || path.contains("crafter.")
            || path.contains("redstone_torch.")
            || path.contains("target.")
            || path.contains("daylight_detector.");
    }

    /** Sound families that redstone farms spam. Muted entirely inside detected farm chunks. */
    public static boolean isRedstoneNoise(String path) {
        return path.startsWith("block.note_block.")
            || path.startsWith("block.dispenser.")
            || path.startsWith("block.piston.")
            || path.startsWith("block.comparator.")
            || path.startsWith("block.fence_gate.")
            || path.contains("trapdoor.");
    }
}
