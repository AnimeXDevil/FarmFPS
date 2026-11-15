# FarmFPS

Client-side farm lag fix for Minecraft 1.21.11 (Fabric). Install it on any server; it only changes what your own game creates and draws, never server-side block updates, farm mechanics or item collection.

| Feature | What it does |
|---|---|
| Piston snap | In chunks detected as active farms, moving piston blocks are drawn at their final position instead of animating. |
| Item cap (packet level) | Once your client holds N dropped items near you, further item spawn packets are ignored, so those entities are never created, ticked or drawn. The server still has them: hoppers collect them and you can pick them up. |
| Hidden item skip | Items that are over the cap but already exist are not drawn and not simulated. |
| XP orb cap | Excess XP orbs near you are not drawn. |
| Farm particles and sounds | Inside a detected farm chunk, particles are hidden and the same sound plays at most once per tick. Elsewhere, per-tick budgets apply. |
| Farm detector | Scores each chunk from piston movement, block-entity updates and particles. A single piston door or lamp never reaches the threshold. |

## Commands

| Command | Does |
|---|---|
| `/farmfps` | Opens the settings screen |
| `/farmfps on` / `off` | Turns FarmFPS on or off for you |
| `/farmfps particles [hide\|show]` | Hides or shows farm particles and sounds (no argument toggles) |
| `/farmfps items <10-2000>` | How many dropped items to keep near you |
| `/farmfps stats` | Live counters. A counter stuck at 0 during a farm means that hook did not apply |
| `/farmfps reload` | Reloads `config/farmfps.json` |

## Releases

Every push to `main` builds a jar and adds it to the single rolling pre-release **dev-builds** as `farmfps-<version>-build-<n>.jar`. The highest number is the newest.
