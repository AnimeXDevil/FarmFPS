package com.farmfps.core;

/**
 * Decides which dropped items / XP orbs are allowed near the camera.
 *
 * Two layers:
 *  1. Spawn cap: once the client already holds {@code maxItems} items in range, new item spawn
 *     packets are dropped, so the extra entities are never created, ticked or tracked at all.
 *  2. Hide threshold: if more than the cap already exist (arrived before the cap, or the cap was
 *     lowered), a stable per-entity hash picks which ones are hidden. They are not drawn and, for
 *     items, not ticked.
 *
 * All values are refreshed a few times per second by the client tick; hot paths only read them.
 */
public final class EntityBudget {
    public static final int FULL = 65536;

    public static int itemKeep = FULL;
    public static int xpKeep = FULL;
    public static int itemsNear;
    public static int xpNear;

    private static double camX = Double.NaN;
    private static double camY;
    private static double camZ;

    private EntityBudget() {
    }

    public static void update(int items, int xp, double x, double y, double z) {
        FarmConfig c = FarmConfig.INSTANCE;
        camX = x;
        camY = y;
        camZ = z;
        itemsNear = items;
        xpNear = xp;
        itemKeep = (c.enabled && c.capItems) ? keep(items, c.maxItems) : FULL;
        xpKeep = (c.enabled && c.capXpOrbs) ? keep(xp, c.maxXpOrbs) : FULL;
    }

    public static void reset() {
        itemKeep = FULL;
        xpKeep = FULL;
        itemsNear = 0;
        xpNear = 0;
        camX = Double.NaN;
    }

    private static int keep(int count, int cap) {
        if (count <= cap) {
            return FULL;
        }
        int k = (int) ((long) cap * FULL / count);
        return Math.max(k, 1);
    }

    /** True if the point is within the configured radius of the player. */
    public static boolean inRange(double x, double y, double z) {
        if (Double.isNaN(camX)) {
            return false;
        }
        double dx = x - camX;
        double dy = y - camY;
        double dz = z - camZ;
        double r = FarmConfig.INSTANCE.cullRadius;
        return dx * dx + dy * dy + dz * dz <= r * r;
    }

    /**
     * Called for every incoming item spawn. Returns true if it should be dropped. Accepted spawns
     * in range are counted immediately so a burst cannot overshoot the cap between scans.
     */
    public static boolean shouldDropItemSpawn(double x, double y, double z) {
        if (!inRange(x, y, z)) {
            return false;
        }
        if (itemsNear >= FarmConfig.INSTANCE.maxItems) {
            return true;
        }
        itemsNear++;
        return false;
    }

    /** True if the entity with this id should NOT be drawn at the given keep threshold. */
    public static boolean isHidden(int entityId, int keep) {
        int h = entityId * 0x9E3779B1;
        h ^= h >>> 16;
        return (h & 0xFFFF) >= keep;
    }
}
