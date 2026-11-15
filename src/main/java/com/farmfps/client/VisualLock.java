package com.farmfps.client;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

/**
 * Redstone visual lock. Observers, dust, repeaters, comparators, lamps, copper bulbs and powered
 * rails flip state many times a second in farms, and every flip dirties a chunk section and
 * forces a mesh rebuild. Inside a chunk detected as an active farm, a flip of one of these blocks
 * to another state of the same block is held back, and only the latest state per position is
 * applied every few ticks. Any other kind of update to that position applies immediately and
 * discards the held one, so the client never shows a block that is wrong for longer than one
 * flush interval and never keeps a stale state.
 *
 * Only blocks whose state is purely visual are included. Trapdoors, doors and gates are not,
 * because their open state changes client-side collision.
 */
public final class VisualLock {
    private static final int SIZE = 2048; // power of two
    private static final int MASK = SIZE - 1;
    private static final long EMPTY = Long.MIN_VALUE;

    private static final long[] KEYS = new long[SIZE];
    private static final BlockState[] STATES = new BlockState[SIZE];
    private static final int[] FLAGS = new int[SIZE];

    private static int pending;
    private static boolean flushing;

    static {
        Arrays.fill(KEYS, EMPTY);
    }

    private VisualLock() {
    }

    private static int slot(long key) {
        int h = (int) (key ^ (key >>> 32)) * 0x9E3779B1;
        return (h >>> 21) & MASK;
    }

    /** Returns true if the update was held back and the vanilla handler must not run now. */
    public static boolean intercept(ClientWorld world, BlockPos pos, BlockState state, int flags) {
        if (flushing) {
            return false;
        }
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            return false;
        }
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        FarmDetector.record(cx, cz, FarmDetector.WEIGHT_BLOCK_UPDATE);

        long key = pos.asLong();
        int slot = slot(key);

        if (c.redstoneLock && isVisualOnly(state.getBlock()) && FarmDetector.isActive(cx, cz)) {
            BlockState current = world.getBlockState(pos);
            if (current.getBlock() == state.getBlock() && current != state
                && (KEYS[slot] == EMPTY || KEYS[slot] == key)) {
                if (KEYS[slot] == EMPTY) {
                    pending++;
                }
                KEYS[slot] = key;
                STATES[slot] = state;
                FLAGS[slot] = flags;
                Stats.redstoneHeld++;
                return true;
            }
        }

        // Applied normally: a held update for this exact position is now out of date.
        if (KEYS[slot] == key) {
            KEYS[slot] = EMPTY;
            STATES[slot] = null;
            pending--;
        }
        return false;
    }

    /** Applies every held update (latest state per position). Call every few ticks. */
    public static void flush(ClientWorld world) {
        if (pending <= 0) {
            return;
        }
        flushing = true;
        try {
            for (int i = 0; i < SIZE; i++) {
                long key = KEYS[i];
                if (key == EMPTY) {
                    continue;
                }
                BlockState state = STATES[i];
                int flags = FLAGS[i];
                KEYS[i] = EMPTY;
                STATES[i] = null;
                BlockPos pos = BlockPos.fromLong(key);
                // Skip if the chunk unloaded or the block was replaced in the meantime.
                if (world.getBlockState(pos).getBlock() == state.getBlock()) {
                    world.handleBlockUpdate(pos, state, flags);
                }
            }
            pending = 0;
        } finally {
            flushing = false;
        }
    }

    public static void clear() {
        Arrays.fill(KEYS, EMPTY);
        Arrays.fill(STATES, null);
        pending = 0;
    }

    public static int pendingCount() {
        return pending;
    }

    private static boolean isVisualOnly(Block b) {
        return b instanceof ObserverBlock
            || b instanceof RedstoneWireBlock
            || b instanceof RepeaterBlock
            || b instanceof ComparatorBlock
            || b instanceof RedstoneLampBlock
            || b instanceof PoweredRailBlock
            || b == Blocks.COPPER_BULB
            || b == Blocks.EXPOSED_COPPER_BULB
            || b == Blocks.WEATHERED_COPPER_BULB
            || b == Blocks.OXIDIZED_COPPER_BULB
            || b == Blocks.WAXED_COPPER_BULB
            || b == Blocks.WAXED_EXPOSED_COPPER_BULB
            || b == Blocks.WAXED_WEATHERED_COPPER_BULB
            || b == Blocks.WAXED_OXIDIZED_COPPER_BULB;
    }
}
