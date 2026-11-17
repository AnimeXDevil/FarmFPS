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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
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
    private static volatile Set<Block> redBlocks = Collections.emptySet();
    private static volatile Set<Block> filterBlocks = Collections.emptySet();
    private static volatile Set<EntityType<?>> mobs = Collections.emptySet();
    private static volatile Set<Block> containers = Collections.emptySet();
    /** Fast pre-check for the block-entity hook. */
    public static volatile boolean containersActive;
    /** Fast pre-check for the mob render hook. */
    public static volatile boolean mobsActive;
    private static volatile Set<Item> items = Collections.emptySet();
    /** Fast pre-check for the render-type hook. */
    public static volatile boolean blocksActive;

    private HideManager() {
    }

    public static boolean isBlockHidden(Block b) {
        return blocks.contains(b);
    }

    /** True if FarmFPS hides this entity (so its hitbox should not be drawn either). */
    public static boolean hidesEntity(Entity e) {
        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            return false;
        }
        if (e instanceof ItemEntity item) {
            return c.hideAllItems || (c.hideFilteredItems && isItemHidden(item.getStack().getItem()));
        }
        return mobsActive && !(e instanceof PlayerEntity) && isMobHidden(e.getType());
    }

    /** Redstone block types hidden by farm mode (subject to the solid-face fix). */
    public static boolean isRedstoneHidden(Block b) {
        return redBlocks.contains(b);
    }

    /** Block types hidden because the user filtered their item; always invisible. */
    public static boolean isFilterHidden(Block b) {
        return filterBlocks.contains(b);
    }

    public static boolean isContainerHidden(Block b) {
        return containers.contains(b);
    }

    /** True if this exact state is currently drawn as invisible (render-type and ambient-occlusion hooks). */
    public static boolean isInvisible(net.minecraft.block.BlockState state) {
        Block block = state.getBlock();
        if (filterBlocks.contains(block)) {
            return true;
        }
        return redBlocks.contains(block) && !(FarmConfig.INSTANCE.solidFaceFix && state.isOpaque());
    }

    public static boolean isMobHidden(EntityType<?> t) {
        return mobs.contains(t);
    }

    public static boolean isItemHidden(Item i) {
        return items.contains(i);
    }

    /** Recomputes the sets from the config. Cheap enough to call on every settings change. */
    public static void rebuild() {
        FarmConfig c = FarmConfig.INSTANCE;
        HashSet<Block> b = new HashSet<>();
        HashSet<Block> red = new HashSet<>();
        HashSet<Block> fil = new HashSet<>();
        HashSet<Item> it = new HashSet<>();
        HashSet<EntityType<?>> mb = new HashSet<>();
        HashSet<Block> cont = new HashSet<>();
        if (c.enabled && c.hideRedstoneBlocks) {
            for (Block blk : Registries.BLOCK) {
                if (isRedstoneRelated(blk)) {
                    b.add(blk);
                    red.add(blk);
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
                        fil.add(placed);
                        if (placed == Blocks.KELP) {
                            b.add(Blocks.KELP_PLANT);
                            fil.add(Blocks.KELP_PLANT);
                        }
                    }
                }
            }
        }
        if (c.enabled && c.hideFish) {
            mb.add(EntityType.COD);
            mb.add(EntityType.SALMON);
            mb.add(EntityType.PUFFERFISH);
            mb.add(EntityType.TROPICAL_FISH);
        }
        if (c.enabled && c.hideFilteredMobs && c.hiddenMobs != null && !c.hiddenMobs.isEmpty()) {
            for (EntityType<?> t : Registries.ENTITY_TYPE) {
                if (c.hiddenMobs.contains(Registries.ENTITY_TYPE.getId(t).toString())) {
                    mb.add(t);
                }
            }
        }
        if (c.enabled && c.hideContainers && c.hiddenContainers != null && !c.hiddenContainers.isEmpty()) {
            for (Block blk : Registries.BLOCK) {
                if (c.hiddenContainers.contains(Registries.BLOCK.getId(blk).toString())) {
                    cont.add(blk);
                    b.add(blk);
                    fil.add(blk);
                }
            }
        }
        containers = Collections.unmodifiableSet(cont);
        containersActive = !cont.isEmpty();
        redBlocks = Collections.unmodifiableSet(red);
        filterBlocks = Collections.unmodifiableSet(fil);
        mobs = Collections.unmodifiableSet(mb);
        mobsActive = !mb.isEmpty();
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
            // Levers are deliberately not hidden: they are the on/off switch of a farm.
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
