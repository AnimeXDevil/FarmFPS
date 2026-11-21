package com.farmfps.client;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Blocks that do not exist (or have another class/constant name) in every Minecraft version are found by
 * registry id instead of by Java class, so the same source compiles for all versions.
 */
public final class BlockIds {
    private static volatile Set<Block> crafters;
    private static volatile Set<Block> bulbs;

    private BlockIds() {
    }

    private static void init() {
        HashSet<Block> c = new HashSet<>();
        HashSet<Block> b = new HashSet<>();
        for (Block blk : Registries.BLOCK) {
            String path = Registries.BLOCK.getId(blk).getPath();
            if (path.equals("crafter")) {
                c.add(blk);
            } else if (path.endsWith("copper_bulb")) {
                b.add(blk);
            }
        }
        bulbs = Collections.unmodifiableSet(b);
        crafters = Collections.unmodifiableSet(c);
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
