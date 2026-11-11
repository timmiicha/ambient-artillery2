package ru.ambientartillery;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AmbientArtillery implements ModInitializer {
    private static final List<ScheduledAction> QUEUE = new ArrayList<>();

    private static final class ScheduledAction {
        private int ticksLeft;
        private final Runnable action;

        private ScheduledAction(int ticksLeft, Runnable action) {
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(net.minecraft.server.command.CommandManager.literal("artillery")
                .then(net.minecraft.server.command.CommandManager.literal("test")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        startBarrage(player);
                        context.getSource().sendFeedback(
                            () -> Text.literal("Артобстрел запущен: 3 разрыва."),
                            false
                        );
                        return 1;
                    }))
                .then(net.minecraft.server.command.CommandManager.literal("stop")
                    .executes(context -> {
                        int removed = QUEUE.size();
                        QUEUE.clear();
                        context.getSource().sendFeedback(
                            () -> Text.literal("Очередь очищена: отменено событий — " + removed + "."),
                            false
                        );
                        return 1;
                    }))));
    }

    private static void tick() {
        List<Runnable> ready = new ArrayList<>();
        Iterator<ScheduledAction> iterator = QUEUE.iterator();

        while (iterator.hasNext()) {
            ScheduledAction scheduled = iterator.next();
            scheduled.ticksLeft--;
            if (scheduled.ticksLeft <= 0) {
                ready.add(scheduled.action);
                iterator.remove();
            }
        }

        for (Runnable action : ready) {
            action.run();
        }
    }

    private static void schedule(int delayTicks, Runnable action) {
        QUEUE.add(new ScheduledAction(delayTicks, action));
    }

    private static void startBarrage(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();

        // Pick one random direction and a random distance from 5 to 100 blocks.
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
        Vec3d direction = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
        Vec3d perpendicular = new Vec3d(-direction.z, 0.0, direction.x);

        double distance = 5.0 + player.getRandom().nextDouble() * 95.0;
        Vec3d center = player.getPos().add(direction.multiply(distance));

        // Three impact centers, exactly two blocks apart along a short line.
        for (int i = 0; i < 3; i++) {
            Vec3d target = center.add(perpendicular.multiply((i - 1) * 2.0));
            BlockPos column = new BlockPos(
                (int) Math.floor(target.x),
                0,
                (int) Math.floor(target.z)
            );
            BlockPos impact = world.getTopPosition(
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                column
            ).toImmutable();

            int startDelay = 1 + i * 100; // 5 seconds between incoming whistles.
            schedule(startDelay, () ->
                world.playSound(
                    null, impact,
                    SoundEvents.ENTITY_GHAST_SHOOT,
                    SoundCategory.HOSTILE,
                    6.0F, 0.65F
                )
            );

            schedule(startDelay + 60, () -> {
                world.playSound(
                    null, impact,
                    SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS,
                    8.0F, 0.7F
                );
                makeCrater(world, impact);
            });
        }
    }

    private static void makeCrater(ServerWorld world, BlockPos surface) {
        // Remove a 3x3 patch, one block deep. Never delete block entities,
        // fluids, or unbreakable blocks such as bedrock.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos blockPos = surface.add(dx, -1, dz);
                BlockState state = world.getBlockState(blockPos);

                if (state.isAir()
                    || !state.getFluidState().isEmpty()
                    || state.hasBlockEntity()
                    || state.getHardness(world, blockPos) < 0.0F) {
                    continue;
                }

                world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 3);
            }
        }

        // Black carpet forms a scorched square ring around the crater.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) {
                    continue;
                }

                BlockPos ground = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    surface.add(dx, 0, dz)
                );
                BlockPos support = ground.down();

                if (world.getBlockState(ground).isAir()
                    && world.getBlockState(support).isSolidBlock(world, support)
                    && !world.getBlockState(support).hasBlockEntity()) {
                    world.setBlockState(
                        ground,
                        Blocks.BLACK_CARPET.getDefaultState(),
                        3
                    );
                }
            }
        }
    }
}
