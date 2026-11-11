# Ambient Artillery (Fabric 1.20.1)

This is a source project. GitHub Actions compiles the mod and publishes the JAR as a workflow artifact.

## Build on GitHub
1. Create a new empty GitHub repository.
2. Upload the contents of this folder to the repository root (including `.github`).
3. Open **Actions** → **Build Fabric mod** → **Run workflow**.
4. Open the completed run and download the artifact `ambient-artillery-1.20.1-fabric`.
5. Extract the artifact ZIP. Put the `.jar` inside your Minecraft `mods` folder. You need Fabric Loader and Fabric API for Minecraft 1.20.1.

## Commands
- `/artillery test` — starts three incoming impacts immediately.
- `/artillery stop` — clears queued sounds and explosions.

The three impact centers are 2 blocks apart in a short line, 5–100 blocks in front of the player. Each whistle is followed by an explosion 3 seconds later; impacts start 5 seconds apart. The crater removes a 3×3 top-layer patch and puts black carpet around it. No fire or projectile entities are used.
