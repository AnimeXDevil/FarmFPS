# FarmFPS

Client-side farm lag fix for Minecraft 1.21.11 (Fabric). Install it on any server; it only changes what your own game draws, never server-side block updates, farm mechanics or item collection.

| Feature | What it does |
|---|---|
| Piston snap | In chunks detected as active farms, moving piston blocks are drawn at their final position instead of animating. |
| Item / XP cap | Draws at most N dropped items and XP orbs within a radius of the camera. The rest still exist and are collected normally. |
| Particle throttle | Drops particles once a per-tick budget is spent. |
| Sound throttle | Mutes the same sound repeating more than N times in one tick. |
| Farm detector | Scores each chunk by piston movement, block-entity updates and particles; piston snapping only runs where the score is high. |

## Settings

Edit in game with `/farmfps`, or from Mod Menu once enabled (see `gradle.properties`). Saved to `config/farmfps.json`.

## Building

Push to `main`. GitHub Actions builds the jar and publishes a GitHub Release with it attached.
