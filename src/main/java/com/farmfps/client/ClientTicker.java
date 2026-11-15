package com.farmfps.client;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.text.Text;

/**
 * Per-tick glue between Minecraft and the version-independent core.
 * This runs 20 times a second, not per frame, so the entity scan may use a normal iterator.
 * The render-path mixins only read the primitive results.
 */
public final class ClientTicker {
    private static final int SCAN_INTERVAL_TICKS = 5;      // 4 scans per second
    private static final int STATUS_INTERVAL_TICKS = 10;   // action bar refresh
    private static int scanTicks;
    private static int statusTicks;
    private static int lockTicks;
    private static long lastStatsTotal;

    private ClientTicker() {
    }

    public static void onStartTick(MinecraftClient client) {
        EffectCounters.resetTick();
    }

    public static void onEndTick(MinecraftClient client) {
        FarmDetector.tick();

        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) {
            EntityBudget.reset();
            FarmDetector.clear();
            VisualLock.clear();
            lockTicks = 0;
            scanTicks = 0;
            statusTicks = 0;
            return;
        }

        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled || !c.redstoneLock) {
            VisualLock.flush(world); // never leave held-back states behind
        }
        if (!c.enabled) {
            EntityBudget.reset();
            return;
        }

        if (++lockTicks >= c.redstoneFlushTicks) {
            lockTicks = 0;
            VisualLock.flush(world);
        }

        if (++scanTicks >= SCAN_INTERVAL_TICKS) {
            scanTicks = 0;
            scan(world, player, c);
        }

        if (c.actionBar && ++statusTicks >= STATUS_INTERVAL_TICKS) {
            statusTicks = 0;
            showStatus(player, c);
        }
    }

    private static void scan(ClientWorld world, ClientPlayerEntity player, FarmConfig c) {
        double r = c.cullRadius;
        double r2 = r * r;
        int items = 0;
        int xp = 0;
        for (Entity e : world.getEntities()) {
            if (e instanceof ItemEntity) {
                if (e.squaredDistanceTo(player) <= r2) {
                    items++;
                }
            } else if (e instanceof ExperienceOrbEntity) {
                if (e.squaredDistanceTo(player) <= r2) {
                    xp++;
                }
            }
        }
        EntityBudget.update(items, xp, player.getX(), player.getY(), player.getZ());
    }

    private static void showStatus(ClientPlayerEntity player, FarmConfig c) {
        long total = Stats.total();
        boolean busy = total != lastStatsTotal
            || EntityBudget.itemsNear >= c.maxItems
            || FarmDetector.activeCount() > 0;
        lastStatsTotal = total;
        if (busy) {
            player.sendMessage(Text.literal("FarmFPS | items " + EntityBudget.itemsNear + "/" + c.maxItems
                + " | blocked: " + Stats.spawnsDropped + " items, "
                + Stats.particlesHidden + " particles, " + Stats.soundsMuted + " sounds"
                + " | redstone held " + Stats.redstoneHeld), true);
        }
    }
}
