package com.ma4z.andymod.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class VisualContext {

    public static String getWhatAndySees(Entity andy, double maxDistance) {
        Vec3 eyePosition = andy.getEyePosition(1.0F);
        Vec3 lookVector = andy.getViewVector(1.0F);
        Vec3 reachVector = eyePosition.add(lookVector.x * maxDistance, lookVector.y * maxDistance, lookVector.z * maxDistance);

        BlockHitResult blockHit = andy.level().clip(new ClipContext(
                eyePosition, 
                reachVector, 
                ClipContext.Block.COLLIDER, 
                ClipContext.Fluid.NONE, 
                andy
        ));

        Vec3 endPoint = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : reachVector;
        
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                andy.level(), 
                andy, 
                eyePosition, 
                endPoint, 
                andy.getBoundingBox().expandTowards(lookVector.scale(maxDistance)).inflate(1.0D), 
                target -> !target.isSpectator() && target.isAlive()
        );

        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            String entityName = target.getType().getDescription().getString();
            String customName = target.getCustomName() != null ? " named " + target.getCustomName().getString() : "";
            
            return "Entity: a " + entityName + customName + " located exactly at coordinates [x: " 
                    + (int)target.getX() + ", y: " + (int)target.getY() + ", z: " + (int)target.getZ() + "]";
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = andy.level().getBlockState(blockHit.getBlockPos());
            String blockName = state.getBlock().getName().getString();
            
            return "Block: looking at " + blockName + " at position [x: " 
                    + blockHit.getBlockPos().getX() + ", y: " + blockHit.getBlockPos().getY() + ", z: " + blockHit.getBlockPos().getZ() + "]";
        }

        return "Nothing clear (just open space or sky)";
    }
}