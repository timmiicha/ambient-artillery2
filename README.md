# Ambient Artillery — Forge 1.20.1

A server-side Forge mod that creates a three-impact artillery barrage.

## Requirements
- Minecraft 1.20.1
- Minecraft Forge 47.2.0 or newer in the 47.x line
- Java 17

## Commands
- `/artillery test` — launches a barrage.
- `/artillery stop` — cancels pending barrage events.

## Behaviour
- Three impacts are placed in a random direction, 5–100 blocks from the player.
- Impact centers are spaced two blocks apart.
- Each impact starts five seconds after the previous one.
- A ghast-shot whistle is followed by an explosion sound three seconds later.
- Each impact removes a 3×3 patch of surface blocks (one block deep) and places black carpet as a scorched border.
- Block entities, fluids, and unbreakable blocks are not removed.
- No projectile entities or fire are created.

## Build
Push to `main` or run the **Build Forge mod** workflow in GitHub Actions. Download the artifact named `ambient-artillery-1.20.1-forge`, extract it, and put the regular JAR (not the sources JAR) in your Minecraft `mods` folder.
