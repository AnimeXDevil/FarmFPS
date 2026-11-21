package com.farmfps.client;

import com.farmfps.core.FarmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

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
            return c.hideAllItems || (c.hideFilteredItems && isItemHidden(item.getItem().getItem()));
        }
        return mobsActive && !(e instanceof Player) && isMobHidden(e.getType());
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
    public static boolean isInvisible(BlockState state) {
        Block block = state.getBlock();
        if (filterBlocks.contains(block)) {
            return true;
        }
        return redBlocks.contains(block) && !(FarmConfig.INSTANCE.solidFaceFix && state.isSolidRender());
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
            for (Block blk : BuiltInRegistries.BLOCK) {
                if (isRedstoneRelated(blk)) {
                    b.add(blk);
                    red.add(blk);
                }
            }
        }
        if (c.enabled && c.hideFilteredItems && c.hiddenItems != null && !c.hiddenItems.isEmpty()) {
            for (Item i : BuiltInRegistries.ITEM) {
                String id = BuiltInRegistries.ITEM.getKey(i).toString();
                if (c.hiddenItems.contains(id)) {
                    it.add(i);
                    if (i instanceof BlockItem bi) {
                        Block placed = bi.getBlock();
                        b.add(placed);
                        fil.add(placed);
                        if (BuiltInRegistries.BLOCK.getKey(placed).toString().equals("minecraft:kelp")) {
                            for (Block k : BuiltInRegistries.BLOCK) {
                                if (BuiltInRegistries.BLOCK.getKey(k).toString().equals("minecraft:kelp_plant")) {
                                    b.add(k);
                                    fil.add(k);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (c.enabled && c.hideFish) {
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
                if (id.equals("minecraft:cod") || id.equals("minecraft:salmon")
                    || id.equals("minecraft:pufferfish") || id.equals("minecraft:tropical_fish")) {
                    mb.add(t);
                }
            }
        }
        if (c.enabled && c.hideFilteredMobs && c.hiddenMobs != null && !c.hiddenMobs.isEmpty()) {
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                if (c.hiddenMobs.contains(BuiltInRegistries.ENTITY_TYPE.getKey(t).toString())) {
                    mb.add(t);
                }
            }
        }
        if (c.enabled && c.hideContainers && c.hiddenContainers != null && !c.hiddenContainers.isEmpty()) {
            for (Block blk : BuiltInRegistries.BLOCK) {
                if (c.hiddenContainers.contains(BuiltInRegistries.BLOCK.getKey(blk).toString())) {
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
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.level != null && mc.player != null) {
                int sx = mc.player.blockPosition().getX() >> 4;
                int sy = mc.player.blockPosition().getY() >> 4;
                int sz = mc.player.blockPosition().getZ() >> 4;
                int r = 24;
                mc.level.setSectionRangeDirty(sx - r, sy - 8, sz - r, sx + r, sy + 8, sz + r);
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
        for (Item i : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(i).toString().equals(s)) {
                return s;
            }
        }
        return null;
    }

    /** Full id of a valid entity type, or null. */
    public static String resolveMob(String text) {
        String s = normalize(text);
        if (s == null) {
            return null;
        }
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(t).toString().equals(s)) {
                return s;
            }
        }
        return null;
    }

    /** Full id of a valid block, or null. */
    public static String resolveBlock(String text) {
        String s = normalize(text);
        if (s == null) {
            return null;
        }
        for (Block b : BuiltInRegistries.BLOCK) {
            if (BuiltInRegistries.BLOCK.getKey(b).toString().equals(s)) {
                return s;
            }
        }
        return null;
    }

    private static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim().toLowerCase(java.util.Locale.ROOT);
        if (s.isEmpty()) {
            return null;
        }
        return s.indexOf(':') < 0 ? "minecraft:" + s : s;
    }

    /** Item with this full id, or null. */
    public static Item itemFor(String id) {
        String s = normalize(id);
        if (s == null) {
            return null;
        }
        for (Item i : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(i).toString().equals(s)) {
                return i;
            }
        }
        return null;
    }

    /** The item form of the block with this id, or null if it has none. */
    public static Item itemForBlock(String blockId) {
        String s = normalize(blockId);
        if (s == null) {
            return null;
        }
        for (Block b : BuiltInRegistries.BLOCK) {
            if (BuiltInRegistries.BLOCK.getKey(b).toString().equals(s)) {
                Item i = b.asItem();
                return BuiltInRegistries.ITEM.getKey(i).toString().equals("minecraft:air") ? null : i;
            }
        }
        return null;
    }

    /** Id of the block a block item places, or null for non-block items. */
    public static String blockIdOfItem(Item item) {
        if (item instanceof BlockItem bi) {
            return BuiltInRegistries.BLOCK.getKey(bi.getBlock()).toString();
        }
        return null;
    }

    private static volatile java.util.List<String> mobIds;

    /** All entity type ids except the player, sorted; cached. */
    public static java.util.List<String> allMobIds() {
        java.util.List<String> out = mobIds;
        if (out == null) {
            out = new java.util.ArrayList<>();
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
                if (!id.equals("minecraft:player")) {
                    out.add(id);
                }
            }
            java.util.Collections.sort(out);
            mobIds = out;
        }
        return out;
    }

    public static String idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static List<String> list() {
        return FarmConfig.INSTANCE.hiddenItems;
    }

    /** Blocks identified by registry id so no Blocks constant names are needed. */
    private static boolean isIdBlock(Block b) {
        String id = BuiltInRegistries.BLOCK.getKey(b).toString();
        return id.equals("minecraft:iron_door") || id.equals("minecraft:iron_trapdoor")
            || id.equals("minecraft:redstone_block") || id.endsWith("copper_bulb");
    }

    private static boolean isRedstoneRelated(Block b) {
        return b instanceof PistonBaseBlock
            || b instanceof PistonHeadBlock
            || b instanceof MovingPistonBlock
            || b instanceof ObserverBlock
            || b instanceof DispenserBlock
            || b instanceof HopperBlock
            || b instanceof RedStoneWireBlock
            || b instanceof RepeaterBlock
            || b instanceof ComparatorBlock
            || b instanceof RedstoneTorchBlock
            || b instanceof RedstoneWallTorchBlock
            || b instanceof RedstoneLampBlock
            || b instanceof NoteBlock
            // Levers are deliberately not hidden: they are the on/off switch of a farm.
            || b instanceof ButtonBlock
            || b instanceof BasePressurePlateBlock
            || b instanceof TripWireBlock
            || b instanceof TripWireHookBlock
            || b instanceof TargetBlock
            || b instanceof DaylightDetectorBlock
            || b instanceof PoweredRailBlock
            || b instanceof DetectorRailBlock
            || b instanceof CrafterBlock
            || isIdBlock(b);
    }
}
