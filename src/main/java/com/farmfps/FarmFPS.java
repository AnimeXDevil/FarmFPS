package com.farmfps;

import com.farmfps.client.ClientTicker;
import com.farmfps.client.HideManager;
import com.farmfps.client.VisualLock;
import com.farmfps.client.gui.ConfigOpener;
import com.farmfps.core.EntityBudget;
import com.farmfps.core.FarmConfig;
import com.farmfps.core.FarmDetector;
import com.farmfps.core.Stats;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
        HideManager.rebuild();

        ClientTickEvents.START_CLIENT_TICK.register(ClientTicker::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(ClientTicker::onEndTick);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("farmfps")
                .executes(ctx -> {
                    MinecraftClient mc = ctx.getSource().getClient();
                    mc.execute(() -> mc.setScreen(ConfigOpener.open(null)));
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
                .then(ClientCommandManager.literal("farmmode").executes(ctx -> {
                    FarmConfig c = FarmConfig.INSTANCE;
                    boolean on = !(c.hideRedstoneBlocks && c.hideAllItems);
                    c.hideRedstoneBlocks = on;
                    c.hideAllItems = on;
                    c.hideMovingBlocks = on;
                    c.muteAllRedstoneSounds = on;
                    saveConfig();
                    HideManager.reload();
                    say(ctx, "Farm mode " + (on ? "ON: redstone blocks invisible, drops hidden; levers stay visible; moving blocks and redstone sounds off too" : "OFF"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("hide")
                    .then(ClientCommandManager.literal("redstone").executes(ctx -> {
                        FarmConfig.INSTANCE.hideRedstoneBlocks = !FarmConfig.INSTANCE.hideRedstoneBlocks;
                        saveConfig();
                        HideManager.reload();
                        say(ctx, "Redstone blocks invisible: " + (FarmConfig.INSTANCE.hideRedstoneBlocks ? "ON" : "OFF"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("drops").executes(ctx -> {
                        FarmConfig.INSTANCE.hideAllItems = !FarmConfig.INSTANCE.hideAllItems;
                        saveConfig();
                        say(ctx, "All dropped items hidden: " + (FarmConfig.INSTANCE.hideAllItems ? "ON" : "OFF"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("faces").executes(ctx -> {
                        FarmConfig.INSTANCE.neighborFaceFix = !FarmConfig.INSTANCE.neighborFaceFix;
                        saveConfig();
                        HideManager.reload();
                        say(ctx, "Neighbour face fix: " + (FarmConfig.INSTANCE.neighborFaceFix ? "ON" : "OFF"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("moving").executes(ctx -> {
                        FarmConfig.INSTANCE.hideMovingBlocks = !FarmConfig.INSTANCE.hideMovingBlocks;
                        saveConfig();
                        say(ctx, "Piston-moved blocks hidden while moving: " + (FarmConfig.INSTANCE.hideMovingBlocks ? "ON" : "OFF"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("sounds").executes(ctx -> {
                        FarmConfig.INSTANCE.muteAllRedstoneSounds = !FarmConfig.INSTANCE.muteAllRedstoneSounds;
                        saveConfig();
                        say(ctx, "Redstone sounds muted (levers excepted): " + (FarmConfig.INSTANCE.muteAllRedstoneSounds ? "ON" : "OFF"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("fish").executes(ctx -> {
                        FarmConfig.INSTANCE.hideFish = !FarmConfig.INSTANCE.hideFish;
                        saveConfig();
                        HideManager.rebuild();
                        say(ctx, "Fish hidden: " + (FarmConfig.INSTANCE.hideFish ? "ON" : "OFF"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("filter").executes(ctx -> {
                        FarmConfig.INSTANCE.hideFilteredItems = !FarmConfig.INSTANCE.hideFilteredItems;
                        saveConfig();
                        HideManager.reload();
                        say(ctx, "Item filter: " + (FarmConfig.INSTANCE.hideFilteredItems ? "ON" : "OFF"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                            .executes(ctx -> hideAdd(ctx, StringArgumentType.getString(ctx, "item")))))
                    .then(ClientCommandManager.literal("hand").executes(ctx -> {
                        var player = ctx.getSource().getPlayer();
                        if (player == null || player.getMainHandStack().isEmpty()) {
                            say(ctx, "Hold the item you want to hide first");
                            return 0;
                        }
                        return hideAdd(ctx, HideManager.idOf(player.getMainHandStack().getItem()));
                    }))
                    .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("item", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String id = HideManager.resolveItem(StringArgumentType.getString(ctx, "item"));
                                if (id == null || !FarmConfig.INSTANCE.hiddenItems.remove(id)) {
                                    say(ctx, "That item is not in the hidden list");
                                    return 0;
                                }
                                saveConfig();
                                HideManager.reload();
                                say(ctx, "Showing " + id + " again");
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("list").executes(ctx -> {
                        say(ctx, FarmConfig.INSTANCE.hiddenItems.isEmpty()
                            ? "Hidden items: none. Use /farmfps hide add <item> or /farmfps hide hand"
                            : "Hidden items: " + String.join(", ", FarmConfig.INSTANCE.hiddenItems));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("clear").executes(ctx -> {
                        FarmConfig.INSTANCE.hiddenItems.clear();
                        saveConfig();
                        HideManager.reload();
                        say(ctx, "Hidden item list cleared");
                        return 1;
                    })))
                .then(ClientCommandManager.literal("anim")
                    .then(ClientCommandManager.argument("skip", IntegerArgumentType.integer(0, 8))
                        .executes(ctx -> {
                            FarmConfig.INSTANCE.textureAnimSkip = IntegerArgumentType.getInteger(ctx, "skip");
                            saveConfig();
                            say(ctx, "Animated textures: update 1 tick in " + (FarmConfig.INSTANCE.textureAnimSkip + 1)
                                + (FarmConfig.INSTANCE.textureAnimSkip == 0 ? " (off)" : " while a farm is active"));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("light")
                    .executes(ctx -> {
                        FarmConfig.INSTANCE.skipLightUpdates = !FarmConfig.INSTANCE.skipLightUpdates;
                        saveConfig();
                        say(ctx, "Skip redstone light updates: " + (FarmConfig.INSTANCE.skipLightUpdates ? "ON" : "OFF"));
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
                    say(ctx, "Item spawns dropped: " + Stats.spawnsDropped + " | excess items removed: " + Stats.itemsDiscarded + " | hidden item ticks skipped: " + Stats.itemTicksSkipped);
                    say(ctx, "Item/XP draws hidden: " + Stats.rendersHidden + " | piston snaps: " + Stats.pistonSnaps);
                    say(ctx, "Particles hidden: " + Stats.particlesHidden + " | sounds muted: " + Stats.soundsMuted);
                    say(ctx, "Redstone redraws skipped: " + Stats.redstoneHeld + " (waiting now: " + VisualLock.pendingCount()
                        + ") | light updates skipped: " + Stats.lightSkipped);
                    say(ctx, "Block entities culled: " + Stats.blockEntitiesCulled + " | far entities culled: " + Stats.entitiesCulled
                        + " | far piston pushes skipped: " + Stats.pistonPushSkipped
                        + " | texture ticks skipped: " + Stats.textureTicksSkipped
                        + " | piston moves finished at once: " + Stats.pistonFinished
                        + " | fluid redraws frozen: " + Stats.fluidFrozen);
                    say(ctx, "Hook reach -> block changes: " + Stats.seenBlockChanges + " (in scope " + Stats.seenFarmChanges
                        + ") | light checks: " + Stats.seenLightChecks + " | item packets: " + Stats.seenItemPackets
                        + " | piston ticks: " + Stats.seenPistonTicks);
                    say(ctx, "Item settings -> cap: " + c.capItems + " | drop extra: " + c.aggressiveItems
                        + " | max: " + c.maxItems + " | radius: " + c.cullRadius);
                    say(ctx, "Scope: " + FarmConfig.SCOPE_NAMES[c.redstoneScope] + " | redstone: " + FarmConfig.REDSTONE_NAMES[c.redstoneMode]
                        + " | pistons: " + FarmConfig.PISTON_NAMES[c.pistonMode]);
                    say(ctx, "A counter stuck at 0 during a farm means that hook did not apply on this version.");
                    return 1;
                }))
                .then(ClientCommandManager.literal("reload").executes(ctx -> {
                    FarmConfig.load(configPath);
                    HideManager.reload();
                    say(ctx, "FarmFPS config reloaded");
                    return 1;
                }))
            )
        );

        LOGGER.info("FarmFPS initialized");
    }

    private static int hideAdd(CommandContext<FabricClientCommandSource> ctx, String text) {
        String id = HideManager.resolveItem(text);
        if (id == null) {
            say(ctx, "Unknown item: " + text + " (example: kelp or minecraft:sea_pickle)");
            return 0;
        }
        if (!FarmConfig.INSTANCE.hiddenItems.contains(id)) {
            FarmConfig.INSTANCE.hiddenItems.add(id);
        }
        saveConfig();
        HideManager.reload();
        say(ctx, "Hiding " + id + " (dropped item and placed block)");
        return 1;
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
