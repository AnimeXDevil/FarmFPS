package com.farmfps.client;

import com.farmfps.core.EffectCounters;
import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;

/**
 * Per-tick glue between Minecraft and the version-independent core.
 * This runs 20 times a second, not per frame, so the entity scan may use a normal iterator.
 * The render-path mixins only read the primitive results.
 */
public final class ClientTicker {
    private static final int SCAN_INTERVAL_TICKS = 5; // 4 scans per second
    private static int ticks;

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
            ticks = 0;
            return;
        }

        FarmConfig c = FarmConfig.INSTANCE;
        if (!c.enabled) {
            EntityBudget.reset();
            return;
        }

        if (++ticks < SCAN_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

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
        EntityBudget.update(items, xp);
    }
}
