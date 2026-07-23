package com.ma4z.andymod.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class VisualContext {

    private static final Set<String> INTERESTING_KEYWORDS = Set.of(
            "Ore", "Chest", "Door", "Torch", "Bed", "Spawner",
            "Lava", "Water", "Furnace", "Crafting", "Anvil",
            "Portal", "Ladder", "Log", "Leaves", "Sapling",
            "Crop", "Farmland", "Rail", "Barrel", "Table", "Stone", "Cobblestone"
    );

    private static final Set<Block> INTERESTING_BLOCKS = new HashSet<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        for (Block block : ForgeRegistries.BLOCKS) {
            String name = block.getName().getString();
            for (String keyword : INTERESTING_KEYWORDS) {
                if (name.toLowerCase().contains(keyword.toLowerCase())) {
                    INTERESTING_BLOCKS.add(block);
                    break;
                }
            }
        }
        initialized = true;
    }

    public static String getWhatAndySees(Entity andy) {
        return getWhatAndySees(andy, 128.0D);
    }

    public static String getWhatAndySees(Entity andy, double maxDistance) {
        if (!initialized) {
            init();
        }

        int chunkRadius = (int) Math.ceil(maxDistance / 16.0);

        BlockPos currentPos = andy.blockPosition();
        int centerChunkX = currentPos.getX() >> 4;
        int centerChunkZ = currentPos.getZ() >> 4;

        List<Entity> nearbyEntities = andy.level().getEntitiesOfClass(
                Entity.class,
                andy.getBoundingBox().inflate(maxDistance),
                e -> e != andy && e.isAlive() && !e.isSpectator()
        );

        nearbyEntities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(andy)));

        List<String> entityDescriptions = new ArrayList<>();
        for (Entity e : nearbyEntities) {
            String name = e.getType().getDescription().getString();
            if (e.getCustomName() != null) {
                name += " named " + e.getCustomName().getString();
            }

            int dx = (int) (e.getX() - andy.getX());
            int dy = (int) (e.getY() - andy.getY());
            int dz = (int) (e.getZ() - andy.getZ());
            int distance = (int) Math.sqrt((double) dx * dx + dy * dy + dz * dz);

            StringBuilder entityDetails = new StringBuilder();
            entityDetails.append(name);

            if (e instanceof LivingEntity living) {
                entityDetails.append(" (HP: ").append((int) living.getHealth())
                             .append("/").append((int) living.getMaxHealth()).append(")");

                ItemStack mainHand = living.getItemBySlot(EquipmentSlot.MAINHAND);
                if (!mainHand.isEmpty()) {
                    entityDetails.append(" [Holding: ").append(mainHand.getHoverName().getString()).append("]");
                }

                List<String> armorList = new ArrayList<>();
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.isArmor()) {
                        ItemStack armorPiece = living.getItemBySlot(slot);
                        if (!armorPiece.isEmpty()) {
                            armorList.add(armorPiece.getHoverName().getString());
                        }
                    }
                }
                if (!armorList.isEmpty()) {
                    entityDetails.append(" [Armor: ").append(String.join(", ", armorList)).append("]");
                }

                if (living instanceof Mob mob && mob.getTarget() != null) {
                    entityDetails.append(" [Targeting: ").append(mob.getTarget().getType().getDescription().getString()).append("]");
                }
            }

            List<String> states = new ArrayList<>();
            if (e.isCrouching()) states.add("sneaking");
            if (e.isSprinting()) states.add("sprinting");
            if (e.isSwimming()) states.add("swimming");
            if (e.isOnFire()) states.add("on fire");
            if (!e.onGround()) states.add("in air");

            if (!states.isEmpty()) {
                entityDetails.append(" (State: ").append(String.join(", ", states)).append(")");
            }

            String direction = getCompassDirection(dx, dz);
            entityDetails.append(" - ").append(distance).append("m ").append(direction)
                         .append(" [offset x:").append(dx).append(", y:").append(dy).append(", z:").append(dz).append("]");

            entityDescriptions.add(entityDetails.toString());
        }

        Map<String, Integer> keyBlocks = new HashMap<>();
        Map<String, BlockPos> nearestPositionOf = new HashMap<>();
        Map<String, Double> nearestDistanceOf = new HashMap<>();
        Map<String, BlockState> nearestStateOf = new HashMap<>();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        int minY = andy.level().getMinBuildHeight();
        int maxY = andy.level().getMaxBuildHeight();

        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                int chunkDist = Math.max(Math.abs(cx), Math.abs(cz));
                if (chunkDist > chunkRadius) continue;

                ChunkAccess chunk = andy.level().getChunk(centerChunkX + cx, centerChunkZ + cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;

                int chunkBaseX = (centerChunkX + cx) * 16;
                int chunkBaseZ = (centerChunkZ + cz) * 16;

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minY; y < maxY; y++) {
                            mutablePos.set(chunkBaseX + x, y, chunkBaseZ + z);
                            BlockState state = chunk.getBlockState(mutablePos);

                            if (state.isAir()) continue;

                            Block block = state.getBlock();
                            if (!INTERESTING_BLOCKS.contains(block)) continue;

                            String name = block.getName().getString();
                            keyBlocks.merge(name, 1, Integer::sum);

                            double dist = mutablePos.distSqr(currentPos);
                            if (!nearestDistanceOf.containsKey(name) || dist < nearestDistanceOf.get(name)) {
                                nearestDistanceOf.put(name, dist);
                                nearestPositionOf.put(name, mutablePos.immutable());
                                nearestStateOf.put(name, state);
                            }
                        }
                    }
                }
            }
        }

        List<String> blockSummary = new ArrayList<>();
        keyBlocks.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(entry -> {
                    String name = entry.getKey();
                    BlockPos nearest = nearestPositionOf.get(name);
                    BlockState state = nearestStateOf.get(name);

                    int dx = nearest.getX() - currentPos.getX();
                    int dy = nearest.getY() - currentPos.getY();
                    int dz = nearest.getZ() - currentPos.getZ();
                    int dist = (int) Math.sqrt((double) dx * dx + dy * dy + dz * dz);
                    String direction = getCompassDirection(dx, dz);

                    String relativeY = dy > 0 ? " (" + dy + "m above)" : dy < 0 ? " (" + Math.abs(dy) + "m below)" : " (same level)";

                    StringBuilder blockDetail = new StringBuilder();
                    blockDetail.append(entry.getValue()).append("x ").append(name)
                               .append(" (nearest ").append(dist).append("m ").append(direction).append(relativeY)
                               .append(" at x:").append(nearest.getX())
                               .append(", y:").append(nearest.getY())
                               .append(", z:").append(nearest.getZ()).append(")");

                    List<String> properties = new ArrayList<>();
                    for (Property<?> prop : state.getProperties()) {
                        properties.add(prop.getName() + "=" + state.getValue(prop));
                    }
                    if (!properties.isEmpty()) {
                        blockDetail.append(" [State: ").append(String.join(", ", properties)).append("]");
                    }

                    blockSummary.add(blockDetail.toString());
                });

        BlockPos feetPos = currentPos.below();
        BlockState standingState = andy.level().getBlockState(feetPos);
        String standingBlockName = standingState.isAir() ? "Air" : standingState.getBlock().getName().getString();

        String groundBeneath = "Solid Ground";
        if (standingState.isAir()) {
            BlockPos.MutableBlockPos searchBelow = feetPos.mutable();
            int depth = 0;
            while (searchBelow.getY() > minY) {
                searchBelow.move(0, -1, 0);
                depth++;
                BlockState stateBelow = andy.level().getBlockState(searchBelow);
                if (!stateBelow.isAir()) {
                    groundBeneath = stateBelow.getBlock().getName().getString() + " (" + depth + "m down at y:" + searchBelow.getY() + ")";
                    break;
                }
            }
        } else {
            groundBeneath = standingBlockName + " (at feet)";
        }

        StringBuilder result = new StringBuilder();
        result.append("Position: [x: ").append(currentPos.getX())
              .append(", y: ").append(currentPos.getY())
              .append(", z: ").append(currentPos.getZ()).append("]\n");

        result.append("Footing: Standing on ").append(standingBlockName)
              .append(" | Ground Beneath: ").append(groundBeneath).append("\n");

        result.append("Entities Detected (").append(entityDescriptions.size()).append("): ");
        if (entityDescriptions.isEmpty()) {
            result.append("None\n");
        } else {
            result.append("\n - ").append(String.join("\n - ", entityDescriptions)).append("\n");
        }

        result.append("Notable Blocks:");
        if (blockSummary.isEmpty()) {
            result.append(" None detected nearby");
        } else {
            result.append("\n - ").append(String.join("\n - ", blockSummary));
        }

        return result.toString();
    }

    private static String getCompassDirection(int dx, int dz) {
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        if (angle < 0) angle += 360;

        String[] directions = {"East", "Southeast", "South", "Southwest", "West", "Northwest", "North", "Northeast"};
        int index = (int) Math.round(angle / 45.0) % 8;
        return directions[index];
    }
}