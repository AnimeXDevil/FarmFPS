package com.farmfps.client;

import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.FluidChurn;
import com.farmfps.core.Stats;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonExtensionBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;

/**
 * Decides, for every block change on the client world, whether to skip its redraw and/or its light
 * update. The real block state is always applied, so collision and logic stay correct; only the
 * visual work is skipped.
 *
 * Redraw skip: the vanilla NO_REDRAW flag is added to visual-only changes (an observer flipping,
 * dust changing power, a lamp toggling, a piston base extending...). The chunk mesh keeps showing
 * the old look until something else redraws that section. In "refresh" mode the affected sections
 * are redrawn a few times per second instead of never.
 *
 * Light skip: the client light engine is not run for redstone flips and piston moves, which is
 * the most expensive part of those changes. Lighting near a farm can look stale as a result.
 */
public final class VisualLock {
    public static final int NO_REDRAW = 4;
    public static final int MOVED = 64;
    public static final int WANT_NO_REDRAW = 1;
    public static final int WANT_NO_LIGHT = 2;

    /** True while MixinWorld re-issues a change with adjusted flags; stops recursion. */
    public static boolean reentry;
    /** True only while a change we flagged is being applied; read by the light hook. */
    public static boolean suppressLight;

    private static final int SIZE = 512; // power of two
    private static final int MASK = SIZE - 1;
    private static final long EMPTY = Long.MIN_VALUE;
    private static final long[] SECTIONS = new long[SIZE];
    private static final long[] REPS = new long[SIZE];
    private static int dirty;

    static {
        Arrays.fill(SECTIONS, EMPTY);
    }

    private VisualLock() {
    }

    /** Returns a bit mask of WANT_* flags, or 0 to leave the change alone. */
    public static int decide(World world, BlockPos pos, BlockState state, int flags) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            return 0;
        }
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        FarmDetector.record(cx, cz, FarmDetector.WEIGHT_BLOCK_UPDATE);
        Block newBlock = state.getBlock();
        BlockState old = world.getBlockState(pos);
        Block oldBlock = old.getBlock();
        // Farm mode: a change between two hidden block types draws nothing, so skip redraw and light.
        if (HideManager.blocksActive && old != state && (flags & NO_REDRAW) == 0
            && HideManager.isBlockHidden(newBlock) && HideManager.isBlockHidden(oldBlock)) {
            Stats.redstoneHeld++;
            return c.skipLightUpdates ? (WANT_NO_REDRAW | WANT_NO_LIGHT) : WANT_NO_REDRAW;
        }
        // Fast-flowing water / lava: skip the redraw while the chunk keeps churning; the section is
        // redrawn every couple of seconds (see ClientTicker) so it never stays stale for long.
        if (c.freezeFluids && old != state && !old.getFluidState().isEmpty() && !state.getFluidState().isEmpty()) {
            int churn = FluidChurn.record(cx, cz);
            if (churn >= c.fluidChurnThreshold && (flags & NO_REDRAW) == 0) {
                markDirty(pos);
                Stats.fluidFrozen++;
                return c.skipLightUpdates ? (WANT_NO_REDRAW | WANT_NO_LIGHT) : WANT_NO_REDRAW;
            }
        }
        if (!FarmDetector.shouldAffect(cx, cz)) {
            return 0;
        }
        Stats.seenFarmChanges++;

        boolean flip = oldBlock == newBlock && old != state && isVisualOnly(newBlock);

        int mask = 0;
        if (flip && c.redstoneMode != 0 && (flags & NO_REDRAW) == 0) {
            mask |= WANT_NO_REDRAW;
            Stats.redstoneHeld++;
            if (c.redstoneMode == 1) {
                markDirty(pos);
            }
        }
        if (c.skipLightUpdates
            && (flip || (flags & MOVED) != 0 || isPistonPart(newBlock) || isPistonPart(oldBlock))) {
            mask |= WANT_NO_LIGHT;
        }
        return mask;
    }

    private static void markDirty(BlockPos pos) {
        int x = pos.getX() >> 4;
        int y = pos.getY() >> 4;
        int z = pos.getZ() >> 4;
        long section = (((long) x & 0x3FFFFFL) << 42) | (((long) z & 0x3FFFFFL) << 20) | ((long) y & 0xFFFFFL);
        int h = (int) (section ^ (section >>> 32)) * 0x9E3779B1;
        int slot = (h >>> 23) & MASK;
        for (int i = 0; i < 4; i++) {
            int s = (slot + i) & MASK;
            if (SECTIONS[s] == section) {
                return;
            }
            if (SECTIONS[s] == EMPTY) {
                SECTIONS[s] = section;
                REPS[s] = pos.asLong();
                dirty++;
                return;
            }
        }
        // Table region full: skip; the section redraws on its next natural update.
    }

    /** Redraws every section that had skipped changes. Safe to call when nothing is pending. */
    public static void refresh(World world) {
        if (dirty <= 0) {
            return;
        }
        BlockState air = Blocks.AIR.getDefaultState();
        for (int i = 0; i < SIZE; i++) {
            if (SECTIONS[i] == EMPTY) {
                continue;
            }
            long rep = REPS[i];
            SECTIONS[i] = EMPTY;
            BlockPos pos = BlockPos.fromLong(rep);
            BlockState current = world.getBlockState(pos);
            if (!current.isAir()) {
                world.updateListeners(pos, air, current, 2);
            }
        }
        dirty = 0;
    }

    public static void clear() {
        Arrays.fill(SECTIONS, EMPTY);
        dirty = 0;
    }

    public static int pendingCount() {
        return dirty;
    }

    private static boolean isPistonPart(Block b) {
        return b instanceof PistonBlock || b instanceof PistonHeadBlock || b instanceof PistonExtensionBlock;
    }

    /**
     * Blocks whose state flips are purely visual on the client. Trapdoors, doors, gates, levers and
     * buttons are deliberately excluded: their state affects collision or player interaction.
     */
    private static boolean isVisualOnly(Block b) {
        return b instanceof ObserverBlock
            || b instanceof RedstoneWireBlock
            || b instanceof RepeaterBlock
            || b instanceof ComparatorBlock
            || b instanceof RedstoneLampBlock
            || b instanceof RedstoneTorchBlock
            || b instanceof PoweredRailBlock
            || b instanceof PistonBlock
            || b instanceof PistonHeadBlock
            || b instanceof DispenserBlock
            || b instanceof HopperBlock
            || b instanceof NoteBlock
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
