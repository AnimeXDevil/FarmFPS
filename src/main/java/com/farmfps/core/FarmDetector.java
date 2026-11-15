package com.farmfps.core;

import java.util.Arrays;

/**
 * Tracks recent activity per chunk (piston movement, block-entity updates, particles)
 * so optimizations only engage around busy farms.
 *
 * Fixed-size, direct-mapped table: no allocation, no boxing, O(1) lookups.
 * A hash collision simply evicts the older chunk, which is harmless for a heuristic.
 * Only the client thread should call this.
 */
public final class FarmDetector {
    public static final int WEIGHT_PISTON = 3;
    public static final int WEIGHT_BLOCK_ENTITY = 2;
    public static final int WEIGHT_PARTICLE = 1;

    private static final int SIZE = 512; // power of two
    private static final int SHIFT = 32 - 9; // top 9 bits of the mixed hash
    private static final int MAX_SCORE = 4096;
    private static final long EMPTY = Long.MIN_VALUE;

    private static final long[] KEYS = new long[SIZE];
    private static final int[] SCORES = new int[SIZE];

    static {
        Arrays.fill(KEYS, EMPTY);
    }

    private FarmDetector() {
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    private static int slot(long key) {
        int h = (int) (key ^ (key >>> 32)) * 0x9E3779B1;
        return h >>> SHIFT;
    }

    public static void record(int chunkX, int chunkZ, int weight) {
        long k = key(chunkX, chunkZ);
        int i = slot(k);
        if (KEYS[i] != k) {
            KEYS[i] = k;
            SCORES[i] = 0;
        }
        int s = SCORES[i] + weight;
        SCORES[i] = Math.min(s, MAX_SCORE);
    }

    public static boolean isActive(int chunkX, int chunkZ) {
        long k = key(chunkX, chunkZ);
        int i = slot(k);
        return KEYS[i] == k && SCORES[i] >= FarmConfig.INSTANCE.farmSensitivity;
    }

    /** Call once per client tick. Exponential decay, roughly 12% per tick plus 1. */
    public static void tick() {
        for (int i = 0; i < SIZE; i++) {
            int s = SCORES[i];
            if (s > 0) {
                s -= (s >> 3) + 1;
                SCORES[i] = Math.max(s, 0);
            }
        }
    }

    public static void clear() {
        Arrays.fill(KEYS, EMPTY);
        Arrays.fill(SCORES, 0);
    }
}
