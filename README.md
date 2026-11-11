# Ambient Artillery — Fabric 1.20.1

A small server-side Fabric mod that creates a three-impact artillery barrage.

## Requirements
- Minecraft 1.20.1
- Fabric Loader 0.15.0 or newer
- Fabric API for Minecraft 1.20.1
- Java 17

## Commands
- `/artillery test` — launches a barrage.
- `/artillery stop` — cancels all pending barrage events.

## Behaviour
- Three impacts are placed in a random direction, 5–100 blocks from the player.
- Impact centers are spaced two blocks apart.
- Each impact starts five seconds after the previous one.
- A ghast-shot whistle is followed by an explosion sound three seconds later.
- Each impact removes a 3×3 patch of surface blocks (one block deep) and places black carpet as a scorched border.
- Block entities, fluids, and unbreakable blocks are not removed.
- No projectile entities or fire are created.

## Build
Push to the `main` branch or run the **Build Fabric mod** workflow from the Actions tab. When the workflow succeeds, download the artifact named `ambient-artillery-1.20.1-fabric`. Extract the downloaded ZIP and put the JAR in your Minecraft `mods` folder.
