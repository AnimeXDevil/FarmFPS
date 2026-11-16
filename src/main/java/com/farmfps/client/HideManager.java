package com.farmfps.client;

import com.farmfps.core.FarmConfig;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.DetectorRailBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LeverBlock;
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
import net.minecraft.block.TargetBlock;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Farm mode" visibility rules: which block types are drawn as invisible and which dropped items
 * are not drawn. The sets are rebuilt when settings change and published as immutable snapshots,
 * so the chunk-meshing threads can read them without locks.
 */
public final class HideManager {
    private static volatile Set<Block> blocks = Collections.emptySet();
    private static volatile Set<Item> items = Collections.emptySet();
    /** Fast pre-check for the render-type hook. */
    public static volatile boolean blocksActive;

    private HideManager() {
    }

    public static boolean isBlockHidden(Block b) {
        return blocks.contains(b);
    }

    public static boolean isItemHidden(Item i) {
        return items.contains(i);
    }

    /** Recomputes the sets from the config. Cheap enough to call on every settings change. */
    public static void rebuild() {
        FarmConfig c = FarmConfig.INSTANCE;
        HashSet<Block> b = new HashSet<>();
        HashSet<Item> it = new HashSet<>();
        if (c.enabled && c.hideRedstoneBlocks) {
            for (Block blk : Registries.BLOCK) {
                if (isRedstoneRelated(blk)) {
                    b.add(blk);
                }
            }
        }
        if (c.enabled && c.hideFilteredItems && c.hiddenItems != null && !c.hiddenItems.isEmpty()) {
            for (Item i : Registries.ITEM) {
                String id = Registries.ITEM.getId(i).toString();
                if (c.hiddenItems.contains(id)) {
                    it.add(i);
                    if (i instanceof BlockItem bi) {
                        Block placed = bi.getBlock();
                        b.add(placed);
                        if (placed == Blocks.KELP) {
                            b.add(Blocks.KELP_PLANT);
                        }
                    }
                }
            }
        }
        blocks = Collections.unmodifiableSet(b);
        items = Collections.unmodifiableSet(it);
        blocksActive = !b.isEmpty();
    }

    /** Rebuilds the sets and re-meshes the world so the change is visible immediately. */
    public static void reload() {
        rebuild();
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.world != null && mc.worldRenderer != null) {
                mc.worldRenderer.reload();
            }
        });
    }

    /** Full registry id ("minecraft:kelp") of a valid item, or null. */
    public static String resolveItem(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim().toLowerCase(java.util.Locale.ROOT);
        if (s.isEmpty()) {
            return null;
        }
        if (s.indexOf(':') < 0) {
            s = "minecraft:" + s;
        }
        for (Item i : Registries.ITEM) {
            if (Registries.ITEM.getId(i).toString().equals(s)) {
                return s;
            }
        }
        return null;
    }

    public static String idOf(Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    public static List<String> list() {
        return FarmConfig.INSTANCE.hiddenItems;
    }

    private static boolean isRedstoneRelated(Block b) {
        return b instanceof PistonBlock
            || b instanceof PistonHeadBlock
            || b instanceof PistonExtensionBlock
            || b instanceof ObserverBlock
            || b instanceof DispenserBlock
            || b instanceof HopperBlock
            || b instanceof RedstoneWireBlock
            || b instanceof RepeaterBlock
            || b instanceof ComparatorBlock
            || b instanceof RedstoneTorchBlock
            || b instanceof WallRedstoneTorchBlock
            || b instanceof RedstoneLampBlock
            || b instanceof NoteBlock
            || b instanceof LeverBlock
            || b instanceof ButtonBlock
            || b instanceof AbstractPressurePlateBlock
            || b instanceof TripwireBlock
            || b instanceof TripwireHookBlock
            || b instanceof TargetBlock
            || b instanceof DaylightDetectorBlock
            || b instanceof PoweredRailBlock
            || b instanceof DetectorRailBlock
            || b instanceof CrafterBlock
            || b == Blocks.IRON_DOOR
            || b == Blocks.IRON_TRAPDOOR
            || b == Blocks.REDSTONE_BLOCK
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
