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
    public static long redstoneHeld;      // visual-only block changes whose redraw was skipped
    public static long lightSkipped;      // client light updates skipped
    public static long entitiesCulled;    // far entities not drawn
    public static long pistonPushSkipped;  // far piston entity-push searches skipped
    public static long textureTicksSkipped; // animated texture ticks skipped
    public static long itemsDiscarded;     // existing excess items removed from the client world
    public static long blockEntitiesCulled; // block entity draw checks answered "too far"

    // Diagnostics: how often each hook was reached at all (separate from the "did something" counters).
    public static long seenBlockChanges;   // client World.setBlockState calls
    public static long seenFarmChanges;    // ...of which inside the active scope
    public static long seenLightChecks;    // LightingProvider.checkBlock calls on the client thread
    public static long seenItemPackets;    // item spawn packets seen on the client thread
    public static long seenPistonTicks;    // client piston block-entity ticks

    private Stats() {
    }

    public static long total() {
        return spawnsDropped + itemTicksSkipped + rendersHidden + pistonSnaps + particlesHidden + soundsMuted + redstoneHeld + lightSkipped + entitiesCulled + blockEntitiesCulled + pistonPushSkipped + textureTicksSkipped + itemsDiscarded;
    }

    public static void reset() {
        spawnsDropped = 0;
        itemTicksSkipped = 0;
        rendersHidden = 0;
        pistonSnaps = 0;
        particlesHidden = 0;
        soundsMuted = 0;
        redstoneHeld = 0;
        lightSkipped = 0;
        entitiesCulled = 0;
        blockEntitiesCulled = 0;
        pistonPushSkipped = 0;
        textureTicksSkipped = 0;
        itemsDiscarded = 0;
        seenBlockChanges = 0;
        seenFarmChanges = 0;
        seenLightChecks = 0;
        seenItemPackets = 0;
        seenPistonTicks = 0;
    }
}
