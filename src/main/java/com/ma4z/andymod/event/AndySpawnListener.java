package com.ma4z.andymod.event;

import com.ma4z.andymod.AndyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "andymod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AndySpawnListener {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        handleRitualCheck(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        handleRitualCheck(event.getLevel(), event.getPos());
    }

    private static void handleRitualCheck(net.minecraft.world.level.LevelAccessor worldAccessor, BlockPos pos) {
        if (!(worldAccessor instanceof ServerLevel world)) return;

        Scoreboard scoreboard = world.getServer().getScoreboard();
        Objective objective = scoreboard.getObjective("andy_status");
        if (objective != null && scoreboard.hasPlayerScore("andy_exists", objective)) {
            int score = scoreboard.getOrCreatePlayerScore("andy_exists", objective).getScore();
            if (score > 0) {
                return;
            }
        }

        if (world.getBlockState(pos).is(Blocks.FIRE)) {
            BlockPos centerCobble = pos.below();

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (!world.getBlockState(centerCobble.offset(x, 0, z)).is(Blocks.COBBLESTONE)) {
                        return;
                    }
                }
            }

            BlockPos northPos = centerCobble.offset(0, 1, -1);
            BlockPos southPos = centerCobble.offset(0, 1, 1);
            BlockPos westPos = centerCobble.offset(-1, 1, 0);
            BlockPos eastPos = centerCobble.offset(1, 1, 0);

            if (isTorch(world, northPos) && isTorch(world, southPos) && isTorch(world, westPos) && isTorch(world, eastPos)) {
                
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(northPos, Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(southPos, Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(westPos, Blocks.AIR.defaultBlockState(), 3);
                world.setBlock(eastPos, Blocks.AIR.defaultBlockState(), 3);

                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        world.setBlock(centerCobble.offset(x, 0, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }

                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
                if (lightning != null) {
                    lightning.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                    world.addFreshEntity(lightning);
                }

                @SuppressWarnings("unchecked")
                EntityType<? extends AndyEntity> entityType = (EntityType<? extends AndyEntity>) EntityType.byString("andymod:andy").orElse(null);
                if (entityType != null) {
                    AndyEntity andy = entityType.create(world);
                    if (andy != null) {
                        andy.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, world.getRandom().nextFloat() * 360.0F, 0.0F);
                        world.addFreshEntity(andy);

                        Objective obj = scoreboard.getObjective("andy_status");
                        if (obj == null) {
                            obj = scoreboard.addObjective("andy_status", net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY, Component.literal("Andy Status"), net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
                        }
                        scoreboard.getOrCreatePlayerScore("andy_exists", obj).setScore(1);
                    }
                }
            }
        }
    }

    private static boolean isTorch(Level world, BlockPos pos) {
        return world.getBlockState(pos).is(Blocks.TORCH) || world.getBlockState(pos).is(Blocks.WALL_TORCH);
    }
}