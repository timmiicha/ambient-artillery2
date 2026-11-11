package ru.ambientartillery;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AmbientArtillery implements ModInitializer {
    private static final List<Scheduled> QUEUE = new ArrayList<>();
    private static int tickCounter = 0;

    private static class Scheduled {
        int ticks;
        final Runnable action;
        Scheduled(int ticks, Runnable action) { this.ticks = ticks; this.action = action; }
    }

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("artillery")
                .then(CommandManager.literal("test").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    startBarrage(player);
                    ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Артобстрел запущен: 3 разрыва."), false);
                    return 1;
                }))
                .then(CommandManager.literal("stop").executes(ctx -> {
                    int removed = QUEUE.size();
                    QUEUE.clear();
                    ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Очередь артобстрела очищена: " + removed + " событий."), false);
                    return 1;
                }))));
    }

    private static void tick() {
        tickCounter++;
        Iterator<Scheduled> it = QUEUE.iterator();
        List<Runnable> ready = new ArrayList<>();
        while (it.hasNext()) {
            Scheduled s = it.next();
            if (--s.ticks <= 0) { ready.add(s.action); it.remove(); }
        }
        for (Runnable r : ready) r.run();
    }

    private static void schedule(int delay, Runnable action) { QUEUE.add(new Scheduled(delay, action)); }

    private static void startBarrage(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d forward = new Vec3d(look.x, 0, look.z).normalize();
        if (forward.lengthSquared() < 0.001) forward = new Vec3d(0, 0, 1);
        Vec3d side = new Vec3d(-forward.z, 0, forward.x);
        double distance = 5 + player.getRandom().nextDouble() * 95;
        Vec3d center = player.getPos().add(forward.multiply(distance));

        for (int i = 0; i < 3; i++) {
            final int index = i;
            Vec3d target = center.add(side.multiply((i - 1) * 2.0));
            BlockPos surface = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos((int)Math.floor(target.x), 0, (int)Math.floor(target.z)));
            final BlockPos impact = surface.toImmutable();
            schedule(index * 100, () -> world.playSound(null, impact, SoundEvents.ENTITY_GHAST_SHOOT,
                SoundCategory.HOSTILE, 6.0F, 0.65F));
            schedule(index * 100 + 60, () -> {
                world.playSound(null, impact, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS, 8.0F, 0.7F);
                makeCrater(world, impact);
            });
        }
    }

    private static void makeCrater(ServerWorld world, BlockPos surface) {
        // Remove a 3x3 patch of the top layer: approximately 3 blocks across, 1 block deep.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = surface.add(dx, 0, dz);
                BlockState state = world.getBlockState(p);
                if (!state.isAir() && !state.getFluidState().isStill()) {
                    world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
                }
            }
        }
        // Black carpet scorched ring around the impact, without fire or extra block damage.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue;
                BlockPos ground = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    surface.add(dx, 0, dz));
                BlockPos carpet = ground.up();
                if (world.getBlockState(ground).isSolidBlock(world, ground)
                    && world.getBlockState(carpet).isAir()) {
                    world.setBlockState(carpet, Blocks.BLACK_CARPET.getDefaultState(), 3);
                }
            }
        }
    }
}
