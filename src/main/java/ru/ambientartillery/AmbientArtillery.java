package ru.ambientartillery;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod(AmbientArtillery.MOD_ID)
public class AmbientArtillery {
    public static final String MOD_ID = "ambient_artillery";
    private static final List<ScheduledAction> QUEUE = new ArrayList<>();

    private static final class ScheduledAction {
        private int ticksLeft;
        private final Runnable action;

        private ScheduledAction(int ticksLeft, Runnable action) {
            this.ticksLeft = ticksLeft;
            this.action = action;
        }
    }

    public AmbientArtillery() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("artillery")
            .then(Commands.literal("test")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    startBarrage(player);
                    context.getSource().sendSuccess(
                        () -> Component.literal("Артобстрел запущен: 3 разрыва."),
                        false
                    );
                    return 1;
                }))
            .then(Commands.literal("stop")
                .executes(context -> {
                    int removed = QUEUE.size();
                    QUEUE.clear();
                    context.getSource().sendSuccess(
                        () -> Component.literal("Очередь очищена: отменено событий — " + removed + "."),
                        false
                    );
                    return 1;
                })));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

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

    private static void startBarrage(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
        Vec3 direction = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        Vec3 perpendicular = new Vec3(-direction.z, 0.0, direction.x);

        double distance = 5.0 + player.getRandom().nextDouble() * 95.0;
        Vec3 center = player.position().add(direction.scale(distance));

        for (int i = 0; i < 3; i++) {
            Vec3 target = center.add(perpendicular.scale((i - 1) * 2.0));
            int x = (int) Math.floor(target.x);
            int z = (int) Math.floor(target.z);
            BlockPos impact = new BlockPos(
                x,
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z),
                z
            );

            int startDelay = 1 + i * 100;
            schedule(startDelay, () -> level.playSound(
                null,
                impact,
                SoundEvents.GHAST_SHOOT,
                SoundSource.HOSTILE,
                6.0F,
                0.65F
            ));

            schedule(startDelay + 60, () -> {
                level.playSound(
                    null,
                    impact,
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    8.0F,
                    0.7F
                );
                makeCrater(level, impact);
            });
        }
    }

    private static void makeCrater(ServerLevel level, BlockPos surface) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos blockPos = surface.offset(dx, -1, dz);
                BlockState state = level.getBlockState(blockPos);
                FluidState fluid = state.getFluidState();

                if (state.isAir()
                    || !fluid.isEmpty()
                    || level.getBlockEntity(blockPos) != null
                    || state.getDestroySpeed(level, blockPos) < 0.0F) {
                    continue;
                }

                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) {
                    continue;
                }

                int x = surface.getX() + dx;
                int z = surface.getZ() + dz;
                BlockPos ground = new BlockPos(
                    x,
                    level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z),
                    z
                );
                BlockPos support = ground.below();
                BlockState supportState = level.getBlockState(support);

                if (level.getBlockState(ground).isAir()
                    && supportState.isSolidRender(level, support)
                    && level.getBlockEntity(support) == null) {
                    level.setBlock(ground, Blocks.BLACK_CARPET.defaultBlockState(), 3);
                }
            }
        }
    }
}
