package com.farmfps.client;

import com.farmfps.core.FarmConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 26.2 port, phase 2: item / mob / storage filters. Redstone-block hiding comes in a later phase. */
public final class HideManager {
    private static volatile Set<EntityType<?>> mobs = Collections.emptySet();
    private static volatile Set<Block> containers = Collections.emptySet();
    private static volatile Set<Item> items = Collections.emptySet();
    public static volatile boolean containersActive;
    public static volatile boolean mobsActive;

    private HideManager() {
    }

    public static boolean isContainerHidden(Block b) {
        return containers.contains(b);
    }

    public static boolean isMobHidden(EntityType<?> t) {
        return mobs.contains(t);
    }

    public static boolean isItemHidden(Item i) {
        return items.contains(i);
    }

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

    public static void rebuild() {
        FarmConfig c = FarmConfig.INSTANCE;
        HashSet<Item> it = new HashSet<>();
        HashSet<EntityType<?>> mb = new HashSet<>();
        HashSet<Block> cont = new HashSet<>();
        if (c.enabled && c.hideFilteredItems && c.hiddenItems != null && !c.hiddenItems.isEmpty()) {
            for (Item i : BuiltInRegistries.ITEM) {
                if (c.hiddenItems.contains(BuiltInRegistries.ITEM.getKey(i).toString())) {
                    it.add(i);
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
                }
            }
        }
        containers = Collections.unmodifiableSet(cont);
        containersActive = !cont.isEmpty();
        mobs = Collections.unmodifiableSet(mb);
        mobsActive = !mb.isEmpty();
        items = Collections.unmodifiableSet(it);
    }
}
