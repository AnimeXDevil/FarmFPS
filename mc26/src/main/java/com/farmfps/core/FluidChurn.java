package com.farmfps.core;

import java.util.Arrays;

/**
 * Counts fluid-to-fluid block changes (water / lava levels flowing) per chunk. A chunk whose score
 * stays high is "churning": rebuilding its mesh on every change is what costs the frames, while
 * still pools and oceans never score. Same fixed-size direct-mapped design as FarmDetector.
 */
public final class FluidChurn {
    private static final int SIZE = 512;
    private static final int SHIFT = 32 - 9;
    private static final int MAX_SCORE = 4096;
    private static final long EMPTY = Long.MIN_VALUE;
    private static final long[] KEYS = new long[SIZE];
    private static final int[] SCORES = new int[SIZE];

    static {
        Arrays.fill(KEYS, EMPTY);
    }

    private FluidChurn() {
    }

    private static long key(int cx, int cz) {
        return ((long) cx & 0xFFFFFFFFL) | (((long) cz & 0xFFFFFFFFL) << 32);
    }

    private static int slot(long key) {
        return ((int) (key ^ (key >>> 32)) * 0x9E3779B1) >>> SHIFT;
    }

    /** Records one fluid change and returns the chunk's current score. */
    public static int record(int cx, int cz) {
        long k = key(cx, cz);
        int i = slot(k);
        if (KEYS[i] != k) {
            KEYS[i] = k;
            SCORES[i] = 0;
        }
        int s = Math.min(SCORES[i] + 1, MAX_SCORE);
        SCORES[i] = s;
        return s;
    }

    /** Once per client tick: slow decay so a chunk calms down a few seconds after the flow stops. */
    public static void tick() {
        for (int i = 0; i < SIZE; i++) {
            int s = SCORES[i];
            if (s > 0) {
                s -= (s >> 5) + 1;
                SCORES[i] = Math.max(s, 0);
            }
        }
    }

    public static void clear() {
        Arrays.fill(KEYS, EMPTY);
        Arrays.fill(SCORES, 0);
    }
}
