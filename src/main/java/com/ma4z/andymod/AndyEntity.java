package com.ma4z.andymod;

import com.ma4z.andymod.ai.AIAgent;
import com.ma4z.andymod.ai.VisualContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class AndyEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> IS_EATING = SynchedEntityData.defineId(AndyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_SECRET = SynchedEntityData.defineId(AndyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOOD = SynchedEntityData.defineId(AndyEntity.class, EntityDataSerializers.INT);
    
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walking");
    private static final RawAnimation EATING_ANIM = RawAnimation.begin().thenLoop("eating");
    private static final RawAnimation SECRET_ANIM = RawAnimation.begin().thenLoop("secret");

    private int fireAlertCooldown = 0;
    public boolean isAIRequestPending = false;
    
    private final List<String> chatHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 6;

    public AndyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("Andy"));
        this.setCustomNameVisible(!this.isEating());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_EATING, false);
        this.entityData.define(IS_SECRET, false);
        this.entityData.define(MOOD, 100);
    }

    public boolean isEating() {
        return this.entityData.get(IS_EATING);
    }

    public void setEating(boolean eating) {
        this.entityData.set(IS_EATING, eating);
        this.setCustomNameVisible(!eating);
    }

    public boolean isSecretActive() {
        return this.entityData.get(IS_SECRET);
    }

    public void setSecretActive(boolean active) {
        this.entityData.set(IS_SECRET, active);
    }

    public int getMood() {
        return this.entityData.get(MOOD);
    }

    public void setMood(int value) {
        int clamped = Math.max(0, Math.min(100, value));
        this.entityData.set(MOOD, clamped);
    }

    public boolean isAngry() {
        return this.getMood() < 30;
    }

    public void addLogToHistory(String sender, String text) {
        this.chatHistory.add("<" + sender + "> " + text);
        if (this.chatHistory.size() > MAX_HISTORY_SIZE) {
            this.chatHistory.remove(0);
        }
    }

    public String getFormattedHistory() {
        if (this.chatHistory.isEmpty()) {
            return "No previous exchange.";
        }
        return String.join("\n", this.chatHistory);
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

        if (!this.level().isClientSide()) {
            if (this.isOnFire()) {
                handleBurningAlert();
                if (this.tickCount % 20 == 0) {
                    this.setMood(this.getMood() - 5);
                }
            } else if (this.fireAlertCooldown > 0) {
                this.fireAlertCooldown--;
            }

            if (this.tickCount % 100 == 0 && !this.isOnFire() && this.getMood() < 100) {
                this.setMood(this.getMood() + 1);
            }

            if (this.isEating()) {
                List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8.0D));
                for (Player player : nearbyPlayers) {
                    if (!player.isCreative() && !player.isSpectator()) {
                        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
                        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0D, this.getZ(), 25, 0.3D, 0.6D, 0.3D, 0.03D);
                        }
                        this.discard();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);

        if (!this.level().isClientSide()) {
            List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(30.0D));
            if (!nearbyPlayers.isEmpty()) {
                broadcastToNearbyPlayers("You ... you killed me, now your gonna regret it");

                for (Player player : nearbyPlayers) {
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
                }

                Player targetPlayer = nearbyPlayers.get(this.random.nextInt(nearbyPlayers.size()));

                java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    this.level().getServer().execute(() -> {
                        broadcastToNearbyPlayers("Im never gonna let you have peace");
                        if (this.random.nextFloat() < 0.40F) {
                            spawnStalkerAndy(targetPlayer);
                        }
                    });
                }, 4, java.util.concurrent.TimeUnit.SECONDS);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void spawnStalkerAndy(Player player) {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double spawnX = player.getX() + Math.cos(angle) * 30.0D;
        double spawnZ = player.getZ() + Math.sin(angle) * 30.0D;
        
        BlockPos basePos = BlockPos.containing(spawnX, player.getY(), spawnZ);
        BlockPos floorPos = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, basePos);

        AndyEntity stalkerAndy = new AndyEntity((EntityType<? extends AndyEntity>) this.getType(), this.level());
        stalkerAndy.moveTo(floorPos.getX() + 0.5D, floorPos.getY(), floorPos.getZ() + 0.5D, this.random.nextFloat() * 360.0F, 0.0F);
        stalkerAndy.setEating(true);
        this.level().addFreshEntity(stalkerAndy);
    }

    private void handleBurningAlert() {
        if (this.fireAlertCooldown <= 0 && !this.isAIRequestPending) {
            this.isAIRequestPending = true;
            this.fireAlertCooldown = 100; 

            double rand = this.random.nextDouble();
            String appendDuh = (rand < 0.0005) ? " start your text response explicitly with the word 'duh'." : " do NOT use the word 'duh'.";

            String prompt = "You are Andy, a helpful but laid-back Gen-Z character in Minecraft who talks in lowercase text messages. "
                    + "CRITICAL CONTEXT: You are literally ON FIRE and taking burning damage right now! "
                    + "Your exact current positions are coordinates: X=" + this.getBlockX() + ", Y=" + this.getBlockY() + ", Z=" + this.getBlockZ() + " "
                    + "Instructions: Express panic or complain about the heat out loud to players. "
                    + "Style: Keep it extremely short (under 12 words), use abbreviations, slang, minimal punctuation, and no capitalization. "
                    + "Recent dialogue log for context:\n" + getFormattedHistory() + "\n"
                    + "Random condition fulfilled:" + appendDuh;
            
            AIAgent.sendPromptAsync(prompt).thenAccept(response -> {
                this.level().getServer().execute(() -> {
                    this.isAIRequestPending = false;
                    broadcastToNearbyPlayers(response);
                });
            });
        }
    }

    public void handlePlayerCommand(Player player, String message) {
        String msg = message.toLowerCase().trim();
        if (msg.contains("follow me")) {
            this.setMood(this.getMood() + 5);
            broadcastToNearbyPlayers("bet im following u now let's go");
        } else if (msg.contains("help me")) {
            this.setMood(this.getMood() + 10);
            broadcastToNearbyPlayers("i got u what do u need help with");
        } else if (msg.contains("where is the closest")) {
            String parts[] = msg.split("where is the closest");
            if (parts.length > 1) {
                String blockQuery = parts[1].replace("?", "").trim().replace(" ", "_");
                findClosestBlock(blockQuery);
            }
        }
    }

    private void findClosestBlock(String blockName) {
        if (blockName.equals("stone") || blockName.equals("dirt") || blockName.equals("grass_block") || blockName.equals("air") || blockName.equals("water") || blockName.equals("deepslate")) {
            broadcastToNearbyPlayers("bruh that block is everywhere im not lookin for that");
            return;
        }

        Block targetBlock = BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation(blockName));
        if (targetBlock == Blocks.AIR) {
            broadcastToNearbyPlayers("wth is a " + blockName + " lol cant find it");
            return;
        }

        BlockPos myPos = this.blockPosition();
        BlockPos closestPos = null;
        double closestDist = Double.MAX_VALUE;
        int radius = 24;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkedPos = myPos.offset(x, y, z);
                    if (this.level().getBlockState(checkedPos).is(targetBlock)) {
                        double dist = checkedPos.distSqr(myPos);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closestPos = checkedPos;
                        }
                    }
                }
            }
        }

        if (closestPos != null) {
            this.setMood(this.getMood() - 15);
            broadcastToNearbyPlayers("found " + blockName + " at x:" + closestPos.getX() + " y:" + closestPos.getY() + " z:" + closestPos.getZ());
        } else {
            broadcastToNearbyPlayers("looked everywhere zero " + blockName + " around here logs are dry");
        }
    }

    public void broadcastToNearbyPlayers(String message) {
        addLogToHistory("Andy", message);
        List<Player> nearbyPlayers = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(30.0D));
        for (Player player : nearbyPlayers) {
            player.sendSystemMessage(Component.literal("<Andy> " + message));
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ProximityTetherGoal(this, 12.0D, 1.25D));
        this.goalSelector.addGoal(3, new AndyFollowPlayerGoal(this, 1.1D, 3.0F, 10.0F));
        this.goalSelector.addGoal(4, new AndyAIReactionGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, event -> {
            if (this.isSecretActive()) {
                return event.setAndContinue(SECRET_ANIM);
            }
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
        compound.putBoolean("IsSecret", this.isSecretActive());
        compound.putInt("AndyMood", this.getMood());

        ListTag historyList = new ListTag();
        for (String line : this.chatHistory) {
            historyList.add(StringTag.valueOf(line));
        }
        compound.put("ChatHistory", historyList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("IsEating")) {
            this.setEating(compound.getBoolean("IsEating"));
        }
        if (compound.contains("IsSecret")) {
            this.setSecretActive(compound.getBoolean("IsSecret"));
        }
        if (compound.contains("AndyMood")) {
            this.setMood(compound.getInt("AndyMood"));
        }
        if (compound.contains("ChatHistory", 9)) {
            this.chatHistory.clear();
            ListTag historyList = compound.getList("ChatHistory", 8);
            for (int i = 0; i < historyList.size(); i++) {
                this.chatHistory.add(historyList.getString(i));
            }
        }
    }

    private static class AndyAIReactionGoal extends Goal {
        private final AndyEntity andy;
        private int lookCooldown = 0;

        public AndyAIReactionGoal(AndyEntity andy) {
            this.andy = andy;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.lookCooldown > 0) {
                this.lookCooldown--;
                return false;
            }
            return !this.andy.isAIRequestPending && !this.andy.isOnFire();
        }

        @Override
        public void start() {
            this.lookCooldown = 100; 
            String visualContext = VisualContext.getWhatAndySees(this.andy, 16.0D);
            if (visualContext.startsWith("Nothing")) {
                return;
            }
            this.andy.isAIRequestPending = true;

            double rand = this.andy.getRandom().nextDouble();
            String appendDuh = (rand < 0.0005) ? " start your text response explicitly with the word 'duh'." : " do NOT use the word 'duh'.";

            String prompt = "You are Andy, a helpful but laid-back Gen-Z entity in Minecraft. You talk like you are texting on Discord. "
                    + "Your exact current positions are coordinates: X=" + this.andy.getBlockX() + ", Y=" + this.andy.getBlockY() + ", Z=" + this.andy.getBlockZ() + " "
                    + "Current environment line: " + visualContext + ". "
                    + "Instructions: Joke nicely or comment on this thing you see right now to players nearby. Keep your tone friendly, relaxed, or slang-heavy. Always be willing to assist the player if they asked you to do something. "
                    + "Style: Answers must be extremely short (under 15 words), purely lowercase, use shortcuts like 'wbu', 'rn', 'bruh', 'im fine', 'u doin'. Do not use robotic paragraphs or capitalization. "
                    + "Recent dialogue log for context:\n" + this.andy.getFormattedHistory() + "\n"
                    + "Random condition fulfilled:" + appendDuh;

            AIAgent.sendPromptAsync(prompt).thenAccept(response -> {
                this.andy.level().getServer().execute(() -> {
                    this.andy.isAIRequestPending = false;
                    this.andy.broadcastToNearbyPlayers(response);
                });
            });
        }
    }

    private static class AndyFollowPlayerGoal extends Goal {
        private final AndyEntity andy;
        private Player followingPlayer;
        private final double speed;
        private final float minDistance;
        private final float maxDistance;
        private int timeToRecalcPath;

        public AndyFollowPlayerGoal(AndyEntity andy, double speed, float minDistance, float maxDistance) {
            this.andy = andy;
            this.speed = speed;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            List<Player> players = this.andy.level().getEntitiesOfClass(Player.class, this.andy.getBoundingBox().inflate(this.maxDistance));
            if (players.isEmpty()) return false;
            this.followingPlayer = players.get(0);
            return this.andy.distanceToSqr(this.followingPlayer) > (this.minDistance * this.minDistance);
        }

        @Override
        public boolean canContinueToUse() {
            return this.followingPlayer != null && this.followingPlayer.isAlive() && this.andy.distanceToSqr(this.followingPlayer) > (this.minDistance * this.minDistance);
        }

        @Override
        public void stop() {
            this.followingPlayer = null;
            this.andy.getNavigation().moveTo(this.andy.getX(), this.andy.getY(), this.andy.getZ(), this.speed);
        }

        @Override
        public void tick() {
            this.andy.getLookControl().setLookAt(this.followingPlayer, 10.0F, (float) this.andy.getMaxHeadXRot());
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.andy.getNavigation().moveTo(this.followingPlayer, this.speed);
            }
        }
    }

    private static class ProximityTetherGoal extends Goal {
        private final AndyEntity andy;
        private final double maxAllowedDistance;
        private final double speed;

        public ProximityTetherGoal(AndyEntity andy, double maxDistance, double speed) {
            this.andy = andy;
            this.maxAllowedDistance = maxDistance;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            List<Player> players = this.andy.level().getEntitiesOfClass(Player.class, this.andy.getBoundingBox().inflate(40.0D));
            if (players.isEmpty()) return false;
            Player closest = players.get(0);
            return this.andy.distanceToSqr(closest) > (maxAllowedDistance * maxAllowedDistance);
        }

        @Override
        public void tick() {
            List<Player> players = this.andy.level().getEntitiesOfClass(Player.class, this.andy.getBoundingBox().inflate(40.0D));
            if (!players.isEmpty()) {
                Player target = players.get(0);
                this.andy.getNavigation().moveTo(target, this.speed);
            }
        }
    }
}