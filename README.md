# FarmFPS

Client-side farm lag fix for Minecraft 1.21.11 (Fabric). Install it on any server; it only changes what your own game creates and draws, never server-side block updates, farm mechanics or item collection.

| Feature | What it does |
|---|---|
| Pistons | Three modes: Vanilla; Snap (moving blocks jump to the end position, no ghosts); Ghost (moving blocks are not drawn at all until they land). Real block state and server logic are unchanged. |
| Item cap (packet level) | Once your client holds N dropped items near you, further item spawn packets are ignored, so those entities are never created, ticked or drawn. The server still has them: hoppers collect them and you can pick them up. |
| Hidden item skip | Items that are over the cap but already exist are not drawn and not simulated. |
| XP orb cap | Excess XP orbs near you are not drawn. |
| Farm particles and sounds | Inside a detected farm chunk, particles are hidden and the same sound plays at most once per tick. Elsewhere, per-tick budgets apply. |
| Redstone visuals | Vanilla / Refresh slowly / Frozen. Visual-only flips (observers, dust, repeaters, comparators, lamps, torches, bulbs, rails, piston bases, dispensers, hoppers, note blocks) skip the chunk redraw; Refresh redraws them every N ticks. Logic and collision are unaffected. |
| Light updates | Optional: client light recalculation is skipped for those flips and for piston moves. Lighting near farms may look stale. |
| Scope | "Farms (auto)" affects only chunks the detector marks as active; "Everywhere" applies to every redstone block and piston, even a single one. |
| Far culling | Block entities and (non-player) entities beyond a set distance in the scope are not drawn. Pistons/observers baked into chunk meshes cannot be culled per block. |
| Redstone sounds | In farm chunks, note block, dispenser, piston, comparator, gate and trapdoor sounds are muted. |
| Far block culling | In farm chunks, block entities (chests, hoppers, signs, moving pistons) beyond a set distance are not drawn. |
| Farm detector | Scores each chunk from piston movement, block-entity updates and particles. A single piston door or lamp never reaches the threshold. |

## Commands

| Command | Does |
|---|---|
| `/farmfps` | Opens the settings screen |
| `/farmfps on` / `off` | Turns FarmFPS on or off for you |
| `/farmfps particles [hide\|show]` | Hides or shows farm particles and sounds (no argument toggles) |
| `/farmfps items <10-2000>` | How many dropped items to keep near you |
| `/farmfps preset balanced\|megafarm\|potato` | Applies a set of limits |
| `/farmfps stats` | Live counters. A counter stuck at 0 during a farm means that hook did not apply |
| `/farmfps reload` | Reloads `config/farmfps.json` |

## Releases

Every push to `main` builds a jar and adds it to the single rolling pre-release **dev-builds** as `farmfps-<version>-build-<n>.jar`. The highest number is the newest.


## New commands
`/farmfps redstone off|refresh|freeze`, `/farmfps scope farms|all`, `/farmfps pistons vanilla|snap|ghost`, `/farmfps light` (toggle light skipping). `/farmfps stats` shows which hooks fire; a counter stuck at 0 during a farm means that hook did not apply.

## Farm mode
`/farmfps farmmode` toggles everything below. `/farmfps hide redstone|drops|filter` toggle the parts. `/farmfps hide add <item>`, `hide hand` (item in your hand), `hide remove <item>`, `hide list`, `hide clear` manage the item filter: filtered items are not drawn as drops, and their placed block is drawn invisible. Hidden redstone types: pistons, observers, dispensers/droppers, hoppers, dust, repeaters, comparators, torches, lamps, note blocks, buttons, plates, tripwire, targets, daylight sensors, powered/detector rails, crafters, copper bulbs, redstone blocks, iron doors and iron trapdoors. Levers stay visible on purpose (they switch the farm). Turn `/farmfps hide drops` off to see drops again.

## Update 11 additions
Settings page 4 has: Hide redstone blocks, Solid face fix (keeps full-size redstone blocks drawn so neighbouring faces have no holes; turn off for maximum speed), Hide all drops, Hide moving blocks, Mute all redstone sounds (levers excepted), Hide fish, and two search pickers (Hide items..., Hide mobs...). "Skip light" is now OFF by default: it can leave stale shadows. Commands: `/farmfps hide moving|sounds|fish`.

## Update 14 additions
- Animated textures are throttled everywhere now (`/farmfps anim` = near farms, "Anim (normal)" slider = everywhere else). Profiles showed texture uploads at 20-23% of the render thread.
- "Freeze fast fluids": water/lava that changes constantly in a chunk stops re-meshing every change (redrawn every 2 s); still pools and oceans are unaffected.
- "Hide moving blocks" now finishes piston moves on the first client tick, so pushed blocks go straight to their destination and are never drawn in between (pistons within 3 blocks of you are left alone).
- Ambient block particles (torch smoke etc.) run every second tick.

## Update 15 additions
- Invisible blocks no longer darken their neighbours (ambient-occlusion shading reports full brightness for them).
- "Hide storage...": pick chests, trapped chests, ender chests, shulker boxes, barrels, hoppers, dispensers/droppers, furnaces, brewing stands, decorated pots, chiseled bookshelves and crafters to stop drawing. "Storage hidden" switches the list on and off.

## Update 16
Invisible blocks now also report an empty side culling face, so neighbouring blocks draw their own face next to them (like they do next to redstone dust) instead of leaving see-through holes. With this, "Solid face fix" can usually be turned off for more speed.

## Settings screen (Cloth Config)
With Cloth Config and Mod Menu installed, `/farmfps` and the Mod Menu config button open a categorised screen with a hover explanation for every option and editable lists for hidden items, mobs and storage blocks. Without Cloth Config the built-in screens are used. Build setup: `cloth_version` and `modmenu_version` in gradle.properties (delete a line to build without that integration).
