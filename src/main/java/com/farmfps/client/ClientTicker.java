package com.farmfps.client;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.FluidChurn;
import com.farmfps.core.Stats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;

/**
 * Per-tick glue between Minecraft and the version-independent core.
 * This runs 20 times a second, not per frame, so the entity scan may use a normal iterator.
 * The render-path mixins only read the primitive results.
 */
public final class ClientTicker {
    private static final int SCAN_INTERVAL_TICKS = 10;     // 2 scans per second
    private static final int STATUS_INTERVAL_TICKS = 10;   // action bar refresh
    private static int scanTicks;
    private static int statusTicks;
    private static int lockTicks;
    private static int flatHold;
    /** True while a farm is active (plus a 30 s hold so meshes are not built with mixed lighting). */
    public static boolean flatLight;
    private static long lastStatsTotal;
    private static final ArrayList<Entity> DISCARD = new ArrayList<>(512);
    private static final int MAX_DISCARDS_PER_SCAN = 300;

    private ClientTicker() {
    }

    public static void onStartTick(MinecraftClient client) {
        EffectCounters.resetTick();
    }

    public static void onEndTick(MinecraftClient client) {
        FarmDetector.tick();
        FluidChurn.tick();
        if (FarmDetector.activeCount() > 0) {
            flatHold = 600;
        } else if (flatHold > 0) {
            flatHold--;
        }
        flatLight = flatHold > 0;

        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) {
            EntityBudget.reset();
            FarmDetector.clear();
            VisualLock.clear();
            FluidChurn.clear();
            lockTicks = 0;
            scanTicks = 0;
            statusTicks = 0;
            return;
        }

        FarmConfig c = FarmConfig.INSTANCE;
        boolean slowRefresh = c.enabled && (c.redstoneMode == 1 || c.freezeFluids);
        if (!slowRefresh) {
            VisualLock.refresh(world); // never leave skipped redraws behind
        }
        if (!c.enabled) {
            EntityBudget.reset();
            return;
        }

        if (slowRefresh && ++lockTicks >= (c.redstoneMode == 1 ? c.redstoneFlushTicks : 40)) {
            lockTicks = 0;
            VisualLock.refresh(world);
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
        if (c.enabled && c.capItems && c.aggressiveItems && items > c.maxItems) {
            discardExcessItems(world, r2, player);
        }
    }

    /**
     * Items that already exist on the client beyond the cap (they arrived before it was reached)
     * are removed from the client world. The server still has them: hoppers and pickups work, and
     * packets about an item the client no longer has are ignored. Runs a few times a second, so
     * allocation here is fine; the batch size is limited to avoid a hitch.
     */
    private static void discardExcessItems(ClientWorld world, double r2, ClientPlayerEntity player) {
        int keep = EntityBudget.itemKeep;
        if (keep >= EntityBudget.FULL) {
            return;
        }
        DISCARD.clear();
        for (Entity e : world.getEntities()) {
            if (e instanceof ItemEntity && e.squaredDistanceTo(player) <= r2
                && EntityBudget.isHidden(e.getId(), keep)) {
                DISCARD.add(e);
                if (DISCARD.size() >= MAX_DISCARDS_PER_SCAN) {
                    break;
                }
            }
        }
        for (int i = 0; i < DISCARD.size(); i++) {
            DISCARD.get(i).discard();
        }
        Stats.itemsDiscarded += DISCARD.size();
        DISCARD.clear();
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
                + " | redraws skipped " + Stats.redstoneHeld + ", light skipped " + Stats.lightSkipped), true);
        }
    }
}
