package com.farmfps.core;

/** Decision logic for snapping piston animations to their end state. */
public final class PistonSnap {
    /** Pre-boxed so returning it from a mixin never allocates. */
    public static final Float DONE = 1.0F;

    private PistonSnap() {
    }

    public static boolean shouldSnap(int chunkX, int chunkZ) {
        FarmConfig c = FarmConfig.INSTANCE;
        return c.enabled && c.pistonSnap && FarmDetector.isActive(chunkX, chunkZ);
    }
}
