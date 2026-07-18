package com.ma4z.andymod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class AndyEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> IS_EATING = SynchedEntityData.defineId(AndyEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walking");
    private static final RawAnimation EATING_ANIM = RawAnimation.begin().thenLoop("eating");

    public AndyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("Andy"));
        this.setCustomNameVisible(!this.isEating());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_EATING, false);
    }

    public boolean isEating() {
        return this.entityData.get(IS_EATING);
    }

    public void setEating(boolean eating) {
        this.entityData.set(IS_EATING, eating);
        this.setCustomNameVisible(!eating);
    }

    @Override
    public boolean isImmobile() {
        return this.isEating() || super.isImmobile();
    }

    @Override
    public boolean shouldShowName() {
        return !this.isEating() && super.shouldShowName();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.isEating()) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.isEating()) {
            List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(8.0D));

            for (Player player : nearbyPlayers) {
                if (!player.isCreative() && !player.isSpectator()) {

                    this.level().playSound(
                            null,
                            this.getX(), this.getY(), this.getZ(),
                            net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                            net.minecraft.sounds.SoundSource.HOSTILE,
                            1.0F,
                            1.0F);

                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                this.getX(), this.getY() + 1.0D, this.getZ(),
                                25,
                                0.3D, 0.6D, 0.3D,
                                0.03D);
                    }

                    this.discard();
                    break;
                }
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            if (this.isEating()) {
                return event.setAndContinue(EATING_ANIM);
            }

            if (event.isMoving()) {
                return event.setAndContinue(WALK_ANIM);
            }

            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("IsEating", this.isEating());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("IsEating")) {
            this.setEating(compound.getBoolean("IsEating"));
        }
    }
}