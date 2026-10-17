package com.example.diamondflash;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DiamondFlash implements ModInitializer {
    private static final int DURATION_TICKS = 12 * 20; // 12 seconds
    private static final int DISTANCE_LEFT = 4;
    private static final int WIDTH = 3;  // along the direction you face
    private static final int HEIGHT = 4; // 3 x 4 = 12 ores

    private record Pending(ServerLevel level, BlockPos pos, BlockState original, long restoreAt) {}

    private final List<Pending> pending = new ArrayList<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("diamondflash").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                int placed = spawn(player);
                ctx.getSource().sendSuccess(
                    () -> Component.literal("Spawned " + placed + " diamond ores for 12 seconds!"), false);
                return placed;
            })));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Pending> it = pending.iterator();
            while (it.hasNext()) {
                Pending p = it.next();
                if (p.level().getGameTime() >= p.restoreAt()) {
                    restore(p);
                    it.remove();
                }
            }
        });

        // Make sure nothing stays permanently replaced if the world closes early.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            pending.forEach(this::restore);
            pending.clear();
        });
    }

    private int spawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Direction facing = player.getDirection();          // horizontal direction you look
        Direction left = facing.getCounterClockWise();     // your left
        BlockPos center = player.blockPosition().relative(left, DISTANCE_LEFT);
        long restoreAt = level.getGameTime() + DURATION_TICKS;

        int count = 0;
        for (int w = -(WIDTH / 2); w <= WIDTH / 2; w++) {
            for (int h = 0; h < HEIGHT; h++) {
                BlockPos pos = center.relative(facing, w).above(h);
                BlockState old = level.getBlockState(pos);

                // Don't destroy chests/furnaces etc. or unbreakable blocks like bedrock.
                if (level.getBlockEntity(pos) != null) continue;
                if (old.getDestroySpeed(level, pos) < 0) continue;
                if (old.is(Blocks.DIAMOND_ORE)) continue;

                pending.add(new Pending(level, pos.immutable(), old, restoreAt));
                level.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);
                count++;
            }
        }
        return count;
    }

    private void restore(Pending p) {
        p.level().setBlock(p.pos(), p.original(), Block.UPDATE_ALL);
    }
}
