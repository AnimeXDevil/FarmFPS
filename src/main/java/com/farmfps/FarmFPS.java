package com.farmfps;

import com.farmfps.client.ClientTicker;
import com.farmfps.client.VisualLock;
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
                .then(ClientCommandManager.literal("redstone")
                    .then(ClientCommandManager.literal("off").executes(ctx -> setRedstone(ctx, 0)))
                    .then(ClientCommandManager.literal("refresh").executes(ctx -> setRedstone(ctx, 1)))
                    .then(ClientCommandManager.literal("freeze").executes(ctx -> setRedstone(ctx, 2))))
                .then(ClientCommandManager.literal("scope")
                    .then(ClientCommandManager.literal("farms").executes(ctx -> setScope(ctx, 0)))
                    .then(ClientCommandManager.literal("all").executes(ctx -> setScope(ctx, 1))))
                .then(ClientCommandManager.literal("pistons")
                    .then(ClientCommandManager.literal("vanilla").executes(ctx -> setPistons(ctx, 0)))
                    .then(ClientCommandManager.literal("snap").executes(ctx -> setPistons(ctx, 1)))
                    .then(ClientCommandManager.literal("ghost").executes(ctx -> setPistons(ctx, 2))))
                .then(ClientCommandManager.literal("light")
                    .executes(ctx -> {
                        FarmConfig.INSTANCE.skipRedstoneLight = !FarmConfig.INSTANCE.skipRedstoneLight;
                        saveConfig();
                        say(ctx, "Skip redstone light updates: " + (FarmConfig.INSTANCE.skipRedstoneLight ? "ON" : "OFF"));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("preset")
                    .then(ClientCommandManager.literal("balanced").executes(ctx -> applyPreset(ctx, 0)))
                    .then(ClientCommandManager.literal("megafarm").executes(ctx -> applyPreset(ctx, 1)))
                    .then(ClientCommandManager.literal("potato").executes(ctx -> applyPreset(ctx, 2))))
                .then(ClientCommandManager.literal("stats").executes(ctx -> {
                    FarmConfig c = FarmConfig.INSTANCE;
                    say(ctx, "FarmFPS " + (c.enabled ? "ON" : "OFF") + " | active farm chunks: " + FarmDetector.activeCount());
                    say(ctx, "Items near: " + EntityBudget.itemsNear + "/" + c.maxItems
                        + " | XP orbs near: " + EntityBudget.xpNear + "/" + c.maxXpOrbs);
                    say(ctx, "Item spawns dropped: " + Stats.spawnsDropped + " | hidden item ticks skipped: " + Stats.itemTicksSkipped);
                    say(ctx, "Item/XP draws hidden: " + Stats.rendersHidden + " | piston snaps: " + Stats.pistonSnaps);
                    say(ctx, "Particles hidden: " + Stats.particlesHidden + " | sounds muted: " + Stats.soundsMuted);
                    say(ctx, "Redstone redraws skipped: " + Stats.redstoneHeld + " (waiting now: " + VisualLock.pendingCount()
                        + ") | light updates skipped: " + Stats.lightSkipped);
                    say(ctx, "Block entities culled: " + Stats.blockEntitiesCulled + " | far entities culled: " + Stats.entitiesCulled);
                    say(ctx, "Scope: " + FarmConfig.SCOPE_NAMES[c.redstoneScope] + " | redstone: " + FarmConfig.REDSTONE_NAMES[c.redstoneMode]
                        + " | pistons: " + FarmConfig.PISTON_NAMES[c.pistonMode]);
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

    private static int setRedstone(CommandContext<FabricClientCommandSource> ctx, int mode) {
        FarmConfig.INSTANCE.redstoneMode = mode;
        saveConfig();
        say(ctx, "Redstone visuals: " + FarmConfig.REDSTONE_NAMES[mode]
            + " (scope: " + FarmConfig.SCOPE_NAMES[FarmConfig.INSTANCE.redstoneScope] + ")");
        return 1;
    }

    private static int setScope(CommandContext<FabricClientCommandSource> ctx, int scope) {
        FarmConfig.INSTANCE.redstoneScope = scope;
        saveConfig();
        say(ctx, "Redstone/piston scope: " + FarmConfig.SCOPE_NAMES[scope]);
        return 1;
    }

    private static int setPistons(CommandContext<FabricClientCommandSource> ctx, int mode) {
        FarmConfig.INSTANCE.pistonMode = mode;
        saveConfig();
        say(ctx, "Pistons: " + FarmConfig.PISTON_NAMES[mode]
            + " (scope: " + FarmConfig.SCOPE_NAMES[FarmConfig.INSTANCE.redstoneScope] + ")");
        return 1;
    }

    private static int applyPreset(CommandContext<FabricClientCommandSource> ctx, int preset) {
        FarmConfig.INSTANCE.applyPreset(preset);
        saveConfig();
        say(ctx, "Preset applied: " + FarmConfig.PRESET_NAMES[FarmConfig.INSTANCE.preset]);
        return 1;
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
