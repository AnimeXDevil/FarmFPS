package com.farmfps.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Blocks whose Java class name changed between 26.x releases (RedStoneWireBlock became RedstoneWireBlock
 * in 26.3) are found by registry id, so the same source compiles for every 26.x version.
 */
public final class BlockIds {
    private static volatile Set<Block> wires;
    private static volatile Set<Block> crafters;
    private static volatile Set<Block> bulbs;

    private BlockIds() {
    }

    private static void init() {
        HashSet<Block> w = new HashSet<>();
        HashSet<Block> c = new HashSet<>();
        HashSet<Block> b = new HashSet<>();
        for (Block blk : BuiltInRegistries.BLOCK) {
            String path = BuiltInRegistries.BLOCK.getKey(blk).getPath();
            if (path.equals("redstone_wire")) {
                w.add(blk);
            } else if (path.equals("crafter")) {
                c.add(blk);
            } else if (path.endsWith("copper_bulb")) {
                b.add(blk);
            }
        }
        crafters = Collections.unmodifiableSet(c);
        bulbs = Collections.unmodifiableSet(b);
        wires = Collections.unmodifiableSet(w);
    }

    public static boolean isRedstoneWire(Block block) {
        if (wires == null) {
            init();
        }
        return wires.contains(block);
    }

    public static boolean isCrafter(Block block) {
        if (crafters == null) {
            init();
        }
        return crafters.contains(block);
    }

    public static boolean isCopperBulb(Block block) {
        if (bulbs == null) {
            init();
        }
        return bulbs.contains(block);
    }
}
