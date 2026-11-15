package com.farmfps.core;

/**
 * Decides which dropped items / XP orbs stay visible.
 *
 * Every few client ticks the entity counts near the camera are fed to {@link #update}.
 * That produces a "keep threshold" in 1..65536. During rendering, an entity is hidden when a
 * stable hash of its id is at or above the threshold. This is O(1), allocation-free, needs no
 * per-frame reset, and the same entities stay hidden from frame to frame (no flicker).
 */
public final class EntityBudget {
    public static final int FULL = 65536;

    public static int itemKeep = FULL;
    public static int xpKeep = FULL;

    private EntityBudget() {
    }

    public static void update(int itemsNearby, int xpNearby) {
        FarmConfig c = FarmConfig.INSTANCE;
        itemKeep = (c.enabled && c.capItems) ? keep(itemsNearby, c.maxItems) : FULL;
        xpKeep = (c.enabled && c.capXpOrbs) ? keep(xpNearby, c.maxXpOrbs) : FULL;
    }

    public static void reset() {
        itemKeep = FULL;
        xpKeep = FULL;
    }

    private static int keep(int count, int cap) {
        if (count <= cap) {
            return FULL;
        }
        int k = (int) ((long) cap * FULL / count);
        return Math.max(k, 1);
    }

    /** True if the entity with this id should NOT be drawn at the given keep threshold. */
    public static boolean isHidden(int entityId, int keep) {
        int h = entityId * 0x9E3779B1;
        h ^= h >>> 16;
        return (h & 0xFFFF) >= keep;
    }
}
