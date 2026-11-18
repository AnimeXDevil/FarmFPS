package com.farmfps.compat;

import com.farmfps.FarmFPS;
import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Settings screen built with Cloth Config: categories, hover explanations for every option and
 * editable lists for hidden items, mobs and storage blocks. Only compiled when cloth_version is
 * set in gradle.properties; loaded by reflection from ConfigOpener.
 */
public final class ClothScreen {
    private ClothScreen() {
    }

    public static Screen create(Screen parent) {
        FarmConfig c = FarmConfig.INSTANCE;
        FarmConfig d = new FarmConfig(); // defaults

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("FarmFPS Settings"))
            .setSavingRunnable(() -> {
                c.clamp();
                FarmFPS.saveConfig();
                HideManager.reload();
            });
        ConfigEntryBuilder eb = builder.entryBuilder();

        // ---------------------------------------------------------------- General
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        bool(general, eb, "FarmFPS enabled", "Master switch. Off = the mod does nothing at all.",
            c.enabled, d.enabled, v -> c.enabled = v);
        bool(general, eb, "Status action bar", "Shows farm chunks, hidden items and mod counters above your hotbar. Turn off for a clean screen.",
            c.actionBar, d.actionBar, v -> c.actionBar = v);
        bool(general, eb, "Particle budget", "Limits how many particles may spawn each tick outside farms.",
            c.throttleParticles, d.throttleParticles, v -> c.throttleParticles = v);
        bool(general, eb, "Sound budget", "Limits how many identical sounds may start each tick outside farms.",
            c.throttleSounds, d.throttleSounds, v -> c.throttleSounds = v);
        bool(general, eb, "Hide farm effects", "Inside detected farm chunks, hide all particles and allow each sound only once per tick.",
            c.hideFarmEffects, d.hideFarmEffects, v -> c.hideFarmEffects = v);
        slider(general, eb, "Farm sensitivity", "Activity score a chunk needs to count as a farm. Lower = detected sooner, higher = only very busy chunks.",
            c.farmSensitivity, d.farmSensitivity, 5, 200, v -> c.farmSensitivity = v);

        // ------------------------------------------------- Redstone, pistons, light
        ConfigCategory red = builder.getOrCreateCategory(Text.literal("Redstone & Pistons"));
        choice(red, eb, "Scope", "Farms (auto): only chunks detected as farms are affected, normal redstone builds stay vanilla. Everywhere: every redstone block and piston is affected, even a single one.",
            FarmConfig.SCOPE_NAMES, c.redstoneScope, d.redstoneScope, v -> c.redstoneScope = v);
        choice(red, eb, "Redstone visuals", "Vanilla: normal. Refresh slowly: state changes (observers, dust, lamps...) redraw only every few ticks. Frozen: they do not redraw at all. Logic and collision are never affected.",
            FarmConfig.REDSTONE_NAMES, c.redstoneMode, d.redstoneMode, v -> c.redstoneMode = v);
        choice(red, eb, "Pistons", "Vanilla: normal animation. Snap: moving blocks jump to the end position (no ghost blocks). Ghost: moving blocks are not drawn while moving.",
            FarmConfig.PISTON_NAMES, c.pistonMode, d.pistonMode, v -> c.pistonMode = v);
        bool(red, eb, "Hide moving blocks", "Piston moves finish on the first client tick, so pushed blocks go straight to their destination and are never drawn in between. Pistons within 3 blocks of you animate normally.",
            c.hideMovingBlocks, d.hideMovingBlocks, v -> c.hideMovingBlocks = v);
        bool(red, eb, "Skip light updates", "Skips client light recalculation for redstone flips and piston moves. Big saving, but lighting can look stale near farms.",
            c.skipLightUpdates, d.skipLightUpdates, v -> c.skipLightUpdates = v);
        slider(red, eb, "Refresh interval (ticks)", "How often skipped redstone redraws are refreshed in 'Refresh slowly' mode.",
            c.redstoneFlushTicks, d.redstoneFlushTicks, 2, 40, v -> c.redstoneFlushTicks = v);
        bool(red, eb, "Mute redstone noise in scope", "Mutes piston, dispenser, note block, comparator and trapdoor sounds inside the scope above.",
            c.muteRedstoneSounds, d.muteRedstoneSounds, v -> c.muteRedstoneSounds = v);
        bool(red, eb, "Mute ALL redstone sounds", "Mutes every redstone-component sound everywhere, except levers (your farm switch). Other sounds are unaffected.",
            c.muteAllRedstoneSounds, d.muteAllRedstoneSounds, v -> c.muteAllRedstoneSounds = v);
        bool(red, eb, "Freeze fast fluids", "Water or lava that keeps changing in a chunk (like sea pickle farm water) stops re-meshing on every change; it is redrawn every 2 seconds. Still oceans, rivers and pools are untouched.",
            c.freezeFluids, d.freezeFluids, v -> c.freezeFluids = v);
        slider(red, eb, "Fluid churn threshold", "How many fluid changes (decaying) a chunk needs before its fluid counts as fast-changing. Lower = freezes sooner.",
            c.fluidChurnThreshold, d.fluidChurnThreshold, 4, 200, v -> c.fluidChurnThreshold = v);

        // -------------------------------------------------------------- Farm mode
        ConfigCategory farm = builder.getOrCreateCategory(Text.literal("Farm Mode"));
        bool(farm, eb, "Hide redstone blocks", "Draws pistons, observers, dispensers, hoppers, dust, repeaters, comparators, torches, lamps, note blocks, buttons, plates, tripwire, rails, crafters, copper bulbs, redstone blocks and iron doors as invisible. Levers stay visible. They still work normally.",
            c.hideRedstoneBlocks, d.hideRedstoneBlocks, v -> c.hideRedstoneBlocks = v);
        bool(farm, eb, "Solid face fix", "Keeps full-size redstone blocks drawn so the blocks next to them have no see-through holes. Turn off for maximum speed (pair with Neighbour face fix).",
            c.solidFaceFix, d.solidFaceFix, v -> c.solidFaceFix = v);
        bool(farm, eb, "Neighbour face fix", "Makes invisible blocks behave like redstone dust for face culling, so neighbouring blocks draw their own face next to them instead of leaving holes. Turn off if you see odd faces.",
            c.neighborFaceFix, d.neighborFaceFix, v -> c.neighborFaceFix = v);
        bool(farm, eb, "Hide all dropped items", "No dropped item is drawn or ticked on your client. The server still has them; hoppers and pickups work normally.",
            c.hideAllItems, d.hideAllItems, v -> c.hideAllItems = v);
        bool(farm, eb, "Item filter on", "Items in the list below are not drawn as drops, and their placed block is drawn invisible.",
            c.hideFilteredItems, d.hideFilteredItems, v -> c.hideFilteredItems = v);
        list(farm, eb, "Hidden items / blocks", "One id per line, e.g. minecraft:kelp or minecraft:sea_pickle. Unknown ids are dropped when you save.",
            c.hiddenItems, v -> c.hiddenItems = clean(v, 0));
        bool(farm, eb, "Storage hiding on", "Storage blocks in the list below are not drawn.",
            c.hideContainers, d.hideContainers, v -> c.hideContainers = v);
        list(farm, eb, "Hidden storage blocks", "Block ids, e.g. minecraft:chest, minecraft:barrel, minecraft:hopper, minecraft:white_shulker_box.",
            c.hiddenContainers, v -> c.hiddenContainers = clean(v, 2));
        bool(farm, eb, "Hide fish", "Cod, salmon, pufferfish and tropical fish are not drawn. Other aquatic mobs are unaffected.",
            c.hideFish, d.hideFish, v -> c.hideFish = v);
        bool(farm, eb, "Mob filter on", "Mobs in the list below are not drawn.",
            c.hideFilteredMobs, d.hideFilteredMobs, v -> c.hideFilteredMobs = v);
        list(farm, eb, "Hidden mobs", "Entity ids, e.g. minecraft:cow, minecraft:zombie.",
            c.hiddenMobs, v -> c.hiddenMobs = clean(v, 1));

        // ---------------------------------------------------- Items and entities
        ConfigCategory ent = builder.getOrCreateCategory(Text.literal("Items & Entities"));
        bool(ent, eb, "Cap dropped items", "Limits how many dropped items your client keeps near you.",
            c.capItems, d.capItems, v -> c.capItems = v);
        bool(ent, eb, "Drop extra items", "Refuses new item spawns over the cap, skips ticking hidden items and removes excess items already on your client.",
            c.aggressiveItems, d.aggressiveItems, v -> c.aggressiveItems = v);
        slider(ent, eb, "Max items", "Items kept near you when the cap is on.",
            c.maxItems, d.maxItems, 10, 2000, v -> c.maxItems = v);
        bool(ent, eb, "Cap XP orbs", "Limits how many experience orbs are drawn near you.",
            c.capXpOrbs, d.capXpOrbs, v -> c.capXpOrbs = v);
        slider(ent, eb, "Max XP orbs", "XP orbs kept near you when the cap is on.",
            c.maxXpOrbs, d.maxXpOrbs, 10, 1000, v -> c.maxXpOrbs = v);
        slider(ent, eb, "Cap radius", "Items and orbs are counted inside this many blocks around you.",
            c.cullRadius, d.cullRadius, 8, 128, v -> c.cullRadius = v);
        bool(ent, eb, "Cull far mobs", "Mobs and other entities beyond the distance below, inside the scope, are not drawn. Players are never hidden.",
            c.entityCull, d.entityCull, v -> c.entityCull = v);
        slider(ent, eb, "Far mob distance", "Entities farther than this many blocks are not drawn.",
            c.entityRenderDistance, d.entityRenderDistance, 8, 96, v -> c.entityRenderDistance = v);
        bool(ent, eb, "Cull far block entities", "Chests, signs, pistons and other block entities beyond the distance below, inside the scope, are not drawn.",
            c.blockEntityCull, d.blockEntityCull, v -> c.blockEntityCull = v);
        slider(ent, eb, "Far block distance", "Block entities farther than this many blocks are not drawn.",
            c.blockEntityDistance, d.blockEntityDistance, 8, 96, v -> c.blockEntityDistance = v);
        slider(ent, eb, "Particles per tick", "Particle budget outside farms.",
            c.maxParticlesPerTick, d.maxParticlesPerTick, 20, 2000, v -> c.maxParticlesPerTick = v);
        slider(ent, eb, "Same sound per tick", "How many identical sounds may start per tick outside farms.",
            c.maxSameSoundPerTick, d.maxSameSoundPerTick, 1, 10, v -> c.maxSameSoundPerTick = v);

        // ------------------------------------------------------------- Rendering
        ConfigCategory rend = builder.getOrCreateCategory(Text.literal("Rendering"));
        slider(rend, eb, "Texture animation skip (farms)", "While a farm is active, animated textures (water, lava, fire) advance 1 tick in N+1. 0 = off. Higher = faster but choppier animation.",
            c.textureAnimSkip, d.textureAnimSkip, 0, 8, v -> c.textureAnimSkip = v);
        slider(rend, eb, "Texture animation skip (normal)", "The same, but when no farm is active. Animated texture uploads were 20% of frame time in a profile.",
            c.textureAnimSkipNormal, d.textureAnimSkipNormal, 0, 8, v -> c.textureAnimSkipNormal = v);
        bool(rend, eb, "Flat light near farms", "Turns smooth lighting off while a farm is active (and for 30 seconds after). Only newly built chunk meshes are affected.",
            c.flatLightNearFarms, d.flatLightNearFarms, v -> c.flatLightNearFarms = v);
        bool(rend, eb, "Halve ambient particles", "Runs the random ambient block particles (torch smoke, drips) every second tick.",
            c.skipDisplayTicks, d.skipDisplayTicks, v -> c.skipDisplayTicks = v);

        return builder.build();
    }

    // ------------------------------------------------------------------ helpers

    private static void bool(ConfigCategory cat, ConfigEntryBuilder eb, String name, String tip,
                             boolean cur, boolean def, Consumer<Boolean> set) {
        cat.addEntry(eb.startBooleanToggle(Text.literal(name), cur)
            .setDefaultValue(def)
            .setTooltip(Text.literal(tip))
            .setSaveConsumer(set)
            .build());
    }

    private static void slider(ConfigCategory cat, ConfigEntryBuilder eb, String name, String tip,
                               int cur, int def, int min, int max, IntConsumer set) {
        cat.addEntry(eb.startIntSlider(Text.literal(name), cur, min, max)
            .setDefaultValue(def)
            .setTooltip(Text.literal(tip))
            .setSaveConsumer(set::accept)
            .build());
    }

    private static void choice(ConfigCategory cat, ConfigEntryBuilder eb, String name, String tip,
                               String[] names, int cur, int def, IntConsumer set) {
        cat.addEntry(eb.startSelector(Text.literal(name), names, names[Math.max(0, Math.min(cur, names.length - 1))])
            .setDefaultValue(names[def])
            .setTooltip(Text.literal(tip))
            .setSaveConsumer(s -> {
                for (int i = 0; i < names.length; i++) {
                    if (names[i].equals(s)) {
                        set.accept(i);
                    }
                }
            })
            .build());
    }

    private static void list(ConfigCategory cat, ConfigEntryBuilder eb, String name, String tip,
                             List<String> cur, Consumer<List<String>> set) {
        cat.addEntry(eb.startStrList(Text.literal(name), cur == null ? new ArrayList<>() : new ArrayList<>(cur))
            .setDefaultValue(new ArrayList<>())
            .setTooltip(Text.literal(tip))
            .setSaveConsumer(set)
            .build());
    }

    /** kind: 0 item, 1 mob, 2 block. Keeps only valid ids, normalised to "namespace:name". */
    private static List<String> clean(List<String> in, int kind) {
        List<String> out = new ArrayList<>();
        for (String s : in) {
            String id = kind == 0 ? HideManager.resolveItem(s)
                : kind == 1 ? HideManager.resolveMob(s) : HideManager.resolveBlock(s);
            if (id != null && !out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }
}
