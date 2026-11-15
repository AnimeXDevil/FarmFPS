package com.farmfps;

import com.farmfps.client.ClientTicker;
import com.farmfps.client.gui.ConfigScreen;
import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class FarmFPS implements ClientModInitializer {
    public static final String MOD_ID = "farmfps";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static Path configPath;

    @Override
    public void onInitializeClient() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("farmfps.json");
        FarmConfig.load(configPath);

        ClientTickEvents.START_CLIENT_TICK.register(ClientTicker::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTicker::onEndTick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("farmfps")
                .executes(ctx -> {
                    MinecraftClient mc = ctx.getSource().getClient();
                    mc.execute(() -> mc.setScreen(new ConfigScreen(null)));
                    return 1;
                })
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    FarmConfig.INSTANCE.enabled = true;
                    saveConfig();
                    say(ctx, "FarmFPS enabled");
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    FarmConfig.INSTANCE.enabled = false;
                    EntityBudget.reset();
                    saveConfig();
                    say(ctx, "FarmFPS disabled (full vanilla rendering)");
                    return 1;
                }))
                .then(ClientCommandManager.literal("particles")
                    .executes(ctx -> {
                        FarmConfig.INSTANCE.hideFarmEffects = !FarmConfig.INSTANCE.hideFarmEffects;
                        saveConfig();
                        say(ctx, "Farm particles and sounds: " + (FarmConfig.INSTANCE.hideFarmEffects ? "hidden" : "shown"));
                        return 1;
                    })
                    .then(ClientCommandManager.literal("hide").executes(ctx -> {
                        FarmConfig.INSTANCE.hideFarmEffects = true;
                        saveConfig();
                        say(ctx, "Farm particles and sounds: hidden");
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("show").executes(ctx -> {
                        FarmConfig.INSTANCE.hideFarmEffects = false;
                        saveConfig();
                        say(ctx, "Farm particles and sounds: shown");
                        return 1;
                    })))
                .then(ClientCommandManager.literal("items")
                    .then(ClientCommandManager.argument("count", IntegerArgumentType.integer(10, 2000))
                        .executes(ctx -> {
                            FarmConfig.INSTANCE.maxItems = IntegerArgumentType.getInteger(ctx, "count");
                            saveConfig();
                            say(ctx, "Rendering up to " + FarmConfig.INSTANCE.maxItems + " dropped items");
                            return 1;
                        })))
                .then(ClientCommandManager.literal("stats").executes(ctx -> {
                    FarmConfig c = FarmConfig.INSTANCE;
                    say(ctx, "FarmFPS " + (c.enabled ? "ON" : "OFF") + " | active farm chunks: " + FarmDetector.activeCount());
                    say(ctx, "Items near: " + EntityBudget.itemsNear + "/" + c.maxItems
                        + " | XP orbs near: " + EntityBudget.xpNear + "/" + c.maxXpOrbs);
                    say(ctx, "Item spawns dropped: " + Stats.spawnsDropped + " | hidden item ticks skipped: " + Stats.itemTicksSkipped);
                    say(ctx, "Item/XP draws hidden: " + Stats.rendersHidden + " | piston snaps: " + Stats.pistonSnaps);
                    say(ctx, "Particles hidden: " + Stats.particlesHidden + " | sounds muted: " + Stats.soundsMuted);
                    say(ctx, "A counter stuck at 0 during a farm means that hook did not apply on this version.");
                    return 1;
                }))
                .then(ClientCommandManager.literal("reload").executes(ctx -> {
                    FarmConfig.load(configPath);
                    say(ctx, "FarmFPS config reloaded");
                    return 1;
                }))
            )
        );

        LOGGER.info("FarmFPS initialized");
    }

    private static void say(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Text.literal(message));
    }

    public static void saveConfig() {
        if (configPath != null) {
            FarmConfig.save(configPath);
        }
    }
}
