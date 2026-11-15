package com.farmfps.core;

/**
 * Session counters for /farmfps stats and the action bar. A counter that stays at 0 while a farm
 * is running usually means the matching hook did not apply on this Minecraft build.
 */
public final class Stats {
    public static long spawnsDropped;     // item spawn packets not turned into client entities
    public static long itemTicksSkipped;  // client ticks skipped for hidden items
    public static long rendersHidden;     // item / XP render checks answered "do not draw"
    public static long pistonSnaps;       // piston animation lookups snapped to the end state
    public static long particlesHidden;   // particles dropped
    public static long soundsMuted;       // sounds dropped

    private Stats() {
    }

    public static long total() {
        return spawnsDropped + itemTicksSkipped + rendersHidden + pistonSnaps + particlesHidden + soundsMuted;
    }

    public static void reset() {
        spawnsDropped = 0;
        itemTicksSkipped = 0;
        rendersHidden = 0;
        pistonSnaps = 0;
        particlesHidden = 0;
        soundsMuted = 0;
    }
}
