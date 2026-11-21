package com.farmfps.core;

import java.util.HashMap;
import java.util.Map;

/** Hover explanations for the settings screen. Version independent, keyed by the button label. */
public final class Tips {
    private static final Map<String, String> MAP = new HashMap<>();

    static {
        MAP.put("Mod", "Master switch. Off = FarmFPS does nothing and the game behaves like vanilla.");
        MAP.put("Preset", "Click to cycle ready-made profiles: balanced, megafarm (strong) and potato (maximum savings).");
        MAP.put("Cap items", "Limit how many dropped items are drawn and simulated near you.");
        MAP.put("Cap XP orbs", "Limit how many experience orbs are drawn and simulated near you.");
        MAP.put("Drop extra items", "Items beyond the cap are not even spawned on your client. Biggest saving at mob farms.");
        MAP.put("Hide farm fx", "Inside detected farms only: hide particles and play each sound at most once per tick.");
        MAP.put("Fx budgets", "Limit particles and identical sounds per tick everywhere, not only in farms.");
        MAP.put("Action bar", "Show a small FarmFPS status line above the hotbar while it is actively saving performance.");
        MAP.put("Scope", "Farms only: tricks apply only in chunks detected as farms. All: everywhere.");
        MAP.put("Redstone", "Vanilla, refresh (batched redraws of redstone blocks) or freeze (no redraws).");
        MAP.put("Pistons", "Vanilla, snap (moving pistons jump to the end of their move) or ghost (moving block hidden).");
        MAP.put("Skip light", "Skip client light recalculation for redstone and piston changes. Can show brief dark patches.");
        MAP.put("Mute redstone snd", "Mute repeating redstone-style noises (pistons, doors, dispensers) inside farms.");
        MAP.put("Cull far blocks", "Do not draw block entities (chests, signs, pistons...) beyond the distance below, inside farms.");
        MAP.put("Cull far mobs", "Do not draw mobs beyond the distance below, inside farms.");
        MAP.put("Refresh ticks", "How often (in ticks) batched redstone redraws are flushed. Higher = fewer redraws.");
        MAP.put("Hide redstone blk", "Make redstone blocks (pistons, observers, dust, lamps, repeaters...) invisible. Levers stay visible.");
        MAP.put("Solid face fix", "Keeps full solid blocks drawn so hiding redstone blocks does not leave holes.");
        MAP.put("Hide all drops", "Do not draw any dropped items.");
        MAP.put("Hide moving blks", "Do not draw blocks being moved by pistons.");
        MAP.put("Mute redstone all", "Mute all redstone-related sounds everywhere. Lever clicks are kept.");
        MAP.put("Hide fish", "Do not draw fish (cod, salmon, pufferfish, tropical fish).");
        MAP.put("Hide items...", "Pick items and blocks that should never be drawn (as drops and as placed blocks).");
        MAP.put("Hide mobs...", "Pick mobs that should never be drawn, including their hitboxes.");
        MAP.put("Hide storage...", "Pick storage blocks (chests, barrels, hoppers...) that should not be drawn.");
        MAP.put("Storage hidden", "Master switch for the hidden storage list.");
        MAP.put("Flat light farms", "Turn off smooth lighting near farms, which makes chunk rebuilds much cheaper.");
        MAP.put("Neighbour faces", "Blocks next to a hidden block keep their faces, so you do not see through the world.");
        MAP.put("Freeze fast fluids", "Stop redrawing water and lava that changes very often.");
        MAP.put("Anim (normal)", "Skip this many texture-animation updates in normal play. 0 = off.");
        MAP.put("Anim skip", "Skip this many texture-animation updates near farms. Water, lava and fire animate less smoothly.");
        MAP.put("Max items", "Maximum dropped items drawn near you before the cap kicks in.");
        MAP.put("Max XP orbs", "Maximum experience orbs drawn near you before the cap kicks in.");
        MAP.put("Radius", "Distance in blocks around you that the item and XP caps count.");
        MAP.put("Particles/tick", "Maximum particles created per tick.");
        MAP.put("Same sound/tick", "Maximum identical sounds started per tick outside farms.");
        MAP.put("Farm trigger", "How much redstone and piston activity marks a chunk as a farm. Lower = detects sooner.");
        MAP.put("Far block dist", "Distance in blocks beyond which block entities are not drawn (in farms).");
        MAP.put("Far mob dist", "Distance in blocks beyond which mobs are not drawn (in farms).");
        MAP.put("Done", "Save and close.");
        MAP.put("More settings >", "Next page of settings.");
    }

    private Tips() {
    }

    /** The explanation for a label, or null if there is none. */
    public static String get(String label) {
        return MAP.get(label);
    }
}
