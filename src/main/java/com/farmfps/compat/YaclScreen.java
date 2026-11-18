package com.farmfps.compat;

import com.farmfps.FarmFPS;
import com.farmfps.client.HideManager;
import com.farmfps.core.FarmConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.ItemControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings screen built with YetAnotherConfigLib: tabs on top, grouped options with hover
 * descriptions, and item lists with icons plus Add / Remove. Only compiled when yacl_version is set
 * in gradle.properties; loaded by reflection from ConfigOpener.
 */
public final class YaclScreen {
    private YaclScreen() {
    }

    public static Screen create(Screen parent) {
        FarmConfig c = FarmConfig.INSTANCE;
        FarmConfig d = new FarmConfig(); // defaults

        // ---------------------------------------------------------------- General
        ConfigCategory general = ConfigCategory.createBuilder()
            .name(Text.literal("General"))
            .tooltip(Text.literal("Master switch, status text and effect budgets."))
            .option(bool("FarmFPS enabled", "Master switch. Off = the mod does nothing at all.",
                d.enabled, () -> c.enabled, v -> c.enabled = v))
            .option(bool("Status action bar", "Shows farm chunks, hidden items and mod counters above your hotbar. Turn off for a clean screen.",
                d.actionBar, () -> c.actionBar, v -> c.actionBar = v))
            .group(OptionGroup.createBuilder()
                .name(Text.literal("Effects"))
                .description(OptionDescription.of(Text.literal("Particle and sound limits.")))
                .option(bool("Particle budget", "Limits how many particles may spawn each tick outside farms.",
                    d.throttleParticles, () -> c.throttleParticles, v -> c.throttleParticles = v))
                .option(bool("Sound budget", "Limits how many identical sounds may start each tick outside farms.",
                    d.throttleSounds, () -> c.throttleSounds, v -> c.throttleSounds = v))
                .option(bool("Hide farm effects", "Inside detected farm chunks, hide all particles and allow each sound only once per tick.",
                    d.hideFarmEffects, () -> c.hideFarmEffects, v -> c.hideFarmEffects = v))
                .option(slider("Particles per tick", "Particle budget outside farms.",
                    d.maxParticlesPerTick, () -> c.maxParticlesPerTick, v -> c.maxParticlesPerTick = v, 20, 2000))
                .option(slider("Same sound per tick", "How many identical sounds may start per tick outside farms.",
                    d.maxSameSoundPerTick, () -> c.maxSameSoundPerTick, v -> c.maxSameSoundPerTick = v, 1, 10))
                .build())
            .option(slider("Farm sensitivity", "Activity score a chunk needs to count as a farm. Lower = detected sooner, higher = only very busy chunks.",
                d.farmSensitivity, () -> c.farmSensitivity, v -> c.farmSensitivity = v, 5, 200))
            .build();

        // ------------------------------------------------- Redstone and pistons
        ConfigCategory red = ConfigCategory.createBuilder()
            .name(Text.literal("Redstone & Pistons"))
            .tooltip(Text.literal("How redstone blocks, pistons, light and fluids are drawn."))
            .option(choice("Scope", "Farms (auto): only chunks detected as farms are affected, normal redstone builds stay vanilla. Everywhere: every redstone block and piston is affected, even a single one.",
                FarmConfig.SCOPE_NAMES, d.redstoneScope, () -> c.redstoneScope, v -> c.redstoneScope = v))
            .option(choice("Redstone visuals", "Vanilla: normal. Refresh slowly: state changes (observers, dust, lamps...) redraw only every few ticks. Frozen: they do not redraw at all. Logic and collision are never affected.",
                FarmConfig.REDSTONE_NAMES, d.redstoneMode, () -> c.redstoneMode, v -> c.redstoneMode = v))
            .option(slider("Refresh interval (ticks)", "How often skipped redstone redraws are refreshed in 'Refresh slowly' mode.",
                d.redstoneFlushTicks, () -> c.redstoneFlushTicks, v -> c.redstoneFlushTicks = v, 2, 40))
            .option(choice("Pistons", "Vanilla: normal animation. Snap: moving blocks jump to the end position (no ghost blocks). Ghost: moving blocks are not drawn while moving.",
                FarmConfig.PISTON_NAMES, d.pistonMode, () -> c.pistonMode, v -> c.pistonMode = v))
            .option(bool("Hide moving blocks", "Piston moves finish on the first client tick, so pushed blocks go straight to their destination and are never drawn in between. Pistons within 3 blocks of you animate normally.",
                d.hideMovingBlocks, () -> c.hideMovingBlocks, v -> c.hideMovingBlocks = v))
            .option(bool("Skip light updates", "Skips client light recalculation for redstone flips and piston moves. Big saving, but lighting can look stale near farms.",
                d.skipLightUpdates, () -> c.skipLightUpdates, v -> c.skipLightUpdates = v))
            .group(OptionGroup.createBuilder()
                .name(Text.literal("Sounds"))
                .description(OptionDescription.of(Text.literal("Redstone noise.")))
                .option(bool("Mute redstone noise in scope", "Mutes piston, dispenser, note block, comparator and trapdoor sounds inside the scope above.",
                    d.muteRedstoneSounds, () -> c.muteRedstoneSounds, v -> c.muteRedstoneSounds = v))
                .option(bool("Mute ALL redstone sounds", "Mutes every redstone-component sound everywhere, except levers (your farm switch). Other sounds are unaffected.",
                    d.muteAllRedstoneSounds, () -> c.muteAllRedstoneSounds, v -> c.muteAllRedstoneSounds = v))
                .build())
            .group(OptionGroup.createBuilder()
                .name(Text.literal("Fluids"))
                .description(OptionDescription.of(Text.literal("Fast-flowing water and lava.")))
                .option(bool("Freeze fast fluids", "Water or lava that keeps changing in a chunk (like sea pickle farm water) stops re-meshing on every change; it is redrawn every 2 seconds. Still oceans, rivers and pools are untouched.",
                    d.freezeFluids, () -> c.freezeFluids, v -> c.freezeFluids = v))
                .option(slider("Fluid churn threshold", "How many fluid changes (decaying) a chunk needs before its fluid counts as fast-changing. Lower = freezes sooner.",
                    d.fluidChurnThreshold, () -> c.fluidChurnThreshold, v -> c.fluidChurnThreshold = v, 4, 200))
                .build())
            .build();

        // ---------------------------------------------------------- Farm mode
        ConfigCategory farm = ConfigCategory.createBuilder()
            .name(Text.literal("Farm Mode"))
            .tooltip(Text.literal("Invisible redstone blocks, hidden drops, and your own hide lists."))
            .group(OptionGroup.createBuilder()
                .name(Text.literal("Redstone blocks"))
                .description(OptionDescription.of(Text.literal("Draw redstone-related blocks as invisible. Levers stay visible. Everything still works normally.")))
                .option(bool("Hide redstone blocks", "Draws pistons, observers, dispensers, hoppers, dust, repeaters, comparators, torches, lamps, note blocks, buttons, plates, tripwire, rails, crafters, copper bulbs, redstone blocks and iron doors as invisible.",
                    d.hideRedstoneBlocks, () -> c.hideRedstoneBlocks, v -> c.hideRedstoneBlocks = v))
                .option(bool("Neighbour face fix", "Makes invisible blocks behave like redstone dust for face culling, so neighbouring blocks draw their own face next to them instead of leaving holes. Turn off if you see odd faces.",
                    d.neighborFaceFix, () -> c.neighborFaceFix, v -> c.neighborFaceFix = v))
                .option(bool("Solid face fix", "Keeps full-size redstone blocks drawn so the blocks next to them have no see-through holes. Turn off for maximum speed (pair with Neighbour face fix).",
                    d.solidFaceFix, () -> c.solidFaceFix, v -> c.solidFaceFix = v))
                .build())
            .group(OptionGroup.createBuilder()
                .name(Text.literal("Dropped items"))
                .description(OptionDescription.of(Text.literal("Stop drawing drops. The server still has them; hoppers and pickups work.")))
                .option(bool("Hide all dropped items", "No dropped item is drawn or ticked on your client.",
                    d.hideAllItems, () -> c.hideAllItems, v -> c.hideAllItems = v))
                .option(bool("Item filter on", "Items in the list below are not drawn as drops, and their placed block is drawn invisible.",
                    d.hideFilteredItems, () -> c.hideFilteredItems, v -> c.hideFilteredItems = v))
                .build())
            .group(ListOption.<Item>createBuilder()
                .name(Text.literal("Custom Item Blacklist"))
                .description(OptionDescription.of(Text.literal("Items (and their placed blocks) that are never drawn. Press Add Item, then pick or type an item.")))
                .binding(new ArrayList<>(), () -> toItems(c.hiddenItems), v -> c.hiddenItems = fromItems(v))
                .controller(ItemControllerBuilder::create)
                .initial(Items.COBBLESTONE)
                .collapsed(false)
                .build())
            .group(OptionGroup.createBuilder()
                .name(Text.literal("Storage"))
                .description(OptionDescription.of(Text.literal("Chests, barrels, shulker boxes, hoppers and similar.")))
                .option(bool("Storage hiding on", "Storage blocks in the list below are not drawn.",
                    d.hideContainers, () -> c.hideContainers, v -> c.hideContainers = v))
                .build())
            .group(ListOption.<Item>createBuilder()
                .name(Text.literal("Hidden storage blocks"))
                .description(OptionDescription.of(Text.literal("Pick the item of the storage block, e.g. Chest, Barrel, Hopper, Shulker Box. Chests, ender chests and shulker boxes are hidden by not drawing their block entity.")))
                .binding(new ArrayList<>(), () -> toBlockItems(c.hiddenContainers), v -> c.hiddenContainers = fromBlockItems(v))
                .controller(ItemControllerBuilder::create)
                .initial(Items.CHEST)
                .collapsed(false)
                .build())
            .group(OptionGroup.createBuilder()
                .name(Text.literal("Mobs"))
                .description(OptionDescription.of(Text.literal("Stop drawing selected mobs.")))
                .option(bool("Hide fish", "Cod, salmon, pufferfish and tropical fish are not drawn. Other aquatic mobs are unaffected.",
                    d.hideFish, () -> c.hideFish, v -> c.hideFish = v))
                .option(bool("Mob filter on", "Mobs in the list below are not drawn.",
                    d.hideFilteredMobs, () -> c.hideFilteredMobs, v -> c.hideFilteredMobs = v))
                .build())
            .group(ListOption.<String>createBuilder()
                .name(Text.literal("Hidden mobs"))
                .description(OptionDescription.of(Text.literal("Type to search, e.g. cow or zombie, then pick an entity id from the dropdown.")))
                .binding(new ArrayList<>(), () -> new ArrayList<>(c.hiddenMobs), v -> c.hiddenMobs = new ArrayList<>(v))
                .controller(opt -> DropdownStringControllerBuilder.create(opt).values(HideManager.allMobIds()))
                .initial("minecraft:cow")
                .collapsed(false)
                .build())
            .build();

        // ---------------------------------------------------- Items and entities
        ConfigCategory ent = ConfigCategory.createBuilder()
            .name(Text.literal("Items & Entities"))
            .tooltip(Text.literal("Caps and distance limits."))
            .group(OptionGroup.createBuilder()
                .name(Text.literal("Dropped items and XP"))
                .description(OptionDescription.of(Text.literal("Limits how many drops and orbs your client keeps near you.")))
                .option(bool("Cap dropped items", "Limits how many dropped items your client keeps near you.",
                    d.capItems, () -> c.capItems, v -> c.capItems = v))
                .option(bool("Drop extra items", "Refuses new item spawns over the cap, skips ticking hidden items and removes excess items already on your client.",
                    d.aggressiveItems, () -> c.aggressiveItems, v -> c.aggressiveItems = v))
                .option(slider("Max items", "Items kept near you when the cap is on.",
                    d.maxItems, () -> c.maxItems, v -> c.maxItems = v, 10, 2000))
                .option(bool("Cap XP orbs", "Limits how many experience orbs are drawn near you.",
                    d.capXpOrbs, () -> c.capXpOrbs, v -> c.capXpOrbs = v))
                .option(slider("Max XP orbs", "XP orbs kept near you when the cap is on.",
                    d.maxXpOrbs, () -> c.maxXpOrbs, v -> c.maxXpOrbs = v, 10, 1000))
                .option(slider("Cap radius", "Items and orbs are counted inside this many blocks around you.",
                    d.cullRadius, () -> c.cullRadius, v -> c.cullRadius = v, 8, 128))
                .build())
            .group(OptionGroup.createBuilder()
                .name(Text.literal("Distance culling"))
                .description(OptionDescription.of(Text.literal("Far entities and block entities are not drawn (inside the scope).")))
                .option(bool("Cull far mobs", "Mobs and other entities beyond the distance below are not drawn. Players are never hidden.",
                    d.entityCull, () -> c.entityCull, v -> c.entityCull = v))
                .option(slider("Far mob distance", "Entities farther than this many blocks are not drawn.",
                    d.entityRenderDistance, () -> c.entityRenderDistance, v -> c.entityRenderDistance = v, 8, 96))
                .option(bool("Cull far block entities", "Chests, signs, pistons and other block entities beyond the distance below are not drawn.",
                    d.blockEntityCull, () -> c.blockEntityCull, v -> c.blockEntityCull = v))
                .option(slider("Far block distance", "Block entities farther than this many blocks are not drawn.",
                    d.blockEntityDistance, () -> c.blockEntityDistance, v -> c.blockEntityDistance = v, 8, 96))
                .build())
            .build();

        // ------------------------------------------------------------ Rendering
        ConfigCategory rend = ConfigCategory.createBuilder()
            .name(Text.literal("Rendering"))
            .tooltip(Text.literal("Texture animation, lighting and ambient particles."))
            .option(slider("Texture animation skip (farms)", "While a farm is active, animated textures (water, lava, fire) advance 1 tick in N+1. 0 = off. Higher = faster but choppier animation.",
                d.textureAnimSkip, () -> c.textureAnimSkip, v -> c.textureAnimSkip = v, 0, 8))
            .option(slider("Texture animation skip (normal)", "The same, but when no farm is active. Animated texture uploads were 20% of frame time in a profile.",
                d.textureAnimSkipNormal, () -> c.textureAnimSkipNormal, v -> c.textureAnimSkipNormal = v, 0, 8))
            .option(bool("Flat light near farms", "Turns smooth lighting off while a farm is active (and for 30 seconds after). Only newly built chunk meshes are affected.",
                d.flatLightNearFarms, () -> c.flatLightNearFarms, v -> c.flatLightNearFarms = v))
            .option(bool("Halve ambient particles", "Runs the random ambient block particles (torch smoke, drips) every second tick.",
                d.skipDisplayTicks, () -> c.skipDisplayTicks, v -> c.skipDisplayTicks = v))
            .build();

        return YetAnotherConfigLib.createBuilder()
            .title(Text.literal("FarmFPS"))
            .category(general)
            .category(red)
            .category(farm)
            .category(ent)
            .category(rend)
            .save(() -> {
                c.clamp();
                FarmFPS.saveConfig();
                HideManager.reload();
            })
            .build()
            .generateScreen(parent);
    }

    // ------------------------------------------------------------------ helpers

    private static Option<Boolean> bool(String name, String tip, boolean def,
                                        Supplier<Boolean> get, Consumer<Boolean> set) {
        return Option.<Boolean>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal(tip)))
            .binding(def, get, set)
            .controller(TickBoxControllerBuilder::create)
            .build();
    }

    private static Option<Integer> slider(String name, String tip, int def,
                                          Supplier<Integer> get, Consumer<Integer> set, int min, int max) {
        return Option.<Integer>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal(tip)))
            .binding(def, get, set)
            .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(1))
            .build();
    }

    private static Option<String> choice(String name, String tip, String[] names, int def,
                                         Supplier<Integer> get, Consumer<Integer> set) {
        return Option.<String>createBuilder()
            .name(Text.literal(name))
            .description(OptionDescription.of(Text.literal(tip)))
            .binding(names[def],
                () -> names[Math.max(0, Math.min(get.get(), names.length - 1))],
                s -> {
                    for (int i = 0; i < names.length; i++) {
                        if (names[i].equals(s)) {
                            set.accept(i);
                        }
                    }
                })
            .controller(opt -> CyclingListControllerBuilder.create(opt).values(List.of(names)))
            .build();
    }

    private static List<Item> toItems(List<String> ids) {
        List<Item> out = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                Item i = HideManager.itemFor(id);
                if (i != null) {
                    out.add(i);
                }
            }
        }
        return out;
    }

    private static List<String> fromItems(List<Item> items) {
        List<String> out = new ArrayList<>();
        for (Item i : items) {
            String id = HideManager.idOf(i);
            if (!id.equals("minecraft:air") && !out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }

    private static List<Item> toBlockItems(List<String> blockIds) {
        List<Item> out = new ArrayList<>();
        if (blockIds != null) {
            for (String id : blockIds) {
                Item i = HideManager.itemForBlock(id);
                if (i != null) {
                    out.add(i);
                }
            }
        }
        return out;
    }

    private static List<String> fromBlockItems(List<Item> items) {
        List<String> out = new ArrayList<>();
        for (Item i : items) {
            String id = HideManager.blockIdOfItem(i);
            if (id != null && !out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }
}
