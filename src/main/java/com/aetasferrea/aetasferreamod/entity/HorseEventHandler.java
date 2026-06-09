package com.aetasferrea.aetasferreamod.entity;

import javax.annotation.Nonnull;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;

public class HorseEventHandler extends Horse {

    public static final int CLASS_WILD = 0;
    public static final int CLASS_ROUNCEY = 1;
    public static final int CLASS_DESTRIER = 2;
    public static final int CLASS_COURSER = 3;
    public static final int CLASS_PALFREY = 4;

    private static final EntityDataAccessor<Integer> DATA_HORSE_CLASS = SynchedEntityData.defineId(HorseEventHandler.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WALK_MODE = SynchedEntityData.defineId(HorseEventHandler.class, EntityDataSerializers.BOOLEAN);

    private double throttle = 0.0;
    private int jumpCooldown = 0;
    private boolean isReversing = false;

    private int combatXP = 0;
    private int agilityXP = 0;
    private int dailyFood = 0;
    private long lastFoodDay = 0L;
    private int customTemper = 0;
    private boolean isClassInitialized = false;

    private final java.util.Map<java.util.UUID, Long> trampleCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    public HorseEventHandler(EntityType<? extends Horse> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HORSE_CLASS, CLASS_WILD);
        this.entityData.define(DATA_WALK_MODE, false);
    }

    public int getHorseClass() { return this.entityData.get(DATA_HORSE_CLASS); }
    public void setHorseClass(int horseClass) { this.entityData.set(DATA_HORSE_CLASS, horseClass); }
    public boolean isWalkMode() { return this.entityData.get(DATA_WALK_MODE); }
    public void setWalkMode(boolean walkMode) { this.entityData.set(DATA_WALK_MODE, walkMode); }
    public double getThrottle() { return this.throttle; }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(ServerLevel level, net.minecraft.world.entity.AgeableMob otherParent) {
        HorseEventHandler foal = com.aetasferrea.aetasferreamod.init.EntityInit.AETAS_HORSE.get().create(level);
        if (foal != null) {
            // Foals bred from tamed horses still need to be broken and start as Wild
            foal.setHorseClass(CLASS_WILD);
        }
        return foal;
    }

    @Override
    protected void removePassenger(net.minecraft.world.entity.Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player player && !this.level().isClientSide) {
            // Apply severe momentum damage if jumping off a horse at high speed
            if (this.throttle >= 0.55 && !this.isWalkMode() && !this.isInWater()) {
                float damage = (float) ((this.throttle - 0.4) * 6.0); // Scaled damage: up to ~3.6 damage
                player.hurt(this.damageSources().fall(), damage);
                
                // Throw the player slightly forward based on horse momentum
                net.minecraft.world.phys.Vec3 horseVec = this.getDeltaMovement();
                player.setDeltaMovement(player.getDeltaMovement().add(horseVec.x * 1.5, 0.2, horseVec.z * 1.5));
                player.hurtMarked = true;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.jumpCooldown > 0) this.jumpCooldown--;

        if (!this.level().isClientSide) {
            
            // Unridden Coasting Momentum
            if (!this.isVehicle() && this.throttle > 0.01) {
                this.throttle = Math.max(0.0, this.throttle - 0.02); // Gradual coast to a stop
                
                double baseSpeed = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
                double sigmoid = 1.0 / (1.0 + Math.exp(-(this.throttle * 12.0 - 6.0)));
                double outputSpeed = baseSpeed * sigmoid;
                
                if (this.getHorseClass() != CLASS_DESTRIER && this.getArmor().is(Items.IRON_HORSE_ARMOR)) {
                    outputSpeed *= 0.70;
                }
                if (this.isReversing) {
                    outputSpeed *= -0.25;
                }
                
                float rot = this.getYRot() * ((float)Math.PI / 180F);
                double dx = -Math.sin(rot) * outputSpeed;
                double dz = Math.cos(rot) * outputSpeed;
                
                this.setDeltaMovement(new net.minecraft.world.phys.Vec3(dx, this.getDeltaMovement().y, dz));
                this.yBodyRot = this.getYRot(); // Keep body facing the moving direction
                this.hasImpulse = true;
            }

            if (!this.isClassInitialized) {
                if (this.random.nextFloat() < 0.05f) {
                    this.setTamed(true);
                    this.setHorseClass(CLASS_ROUNCEY);
                } else {
                    this.setHorseClass(CLASS_WILD);
                }
                this.applyClassAttributeCaps(this.getHorseClass());
                this.isClassInitialized = true;
            }

            if (this.getHorseClass() == CLASS_DESTRIER && this.tickCount % 100 == 0) {
                this.removePanicGoal();
            }

            if (this.isVehicle() && this.tickCount % 2 == 0) {
                if ((this.getHorseClass() == CLASS_DESTRIER || this.getHorseClass() == CLASS_COURSER) && this.throttle >= 0.85 && !this.isReversing) {
                    long gameTime = this.level().getGameTime();
                    net.minecraft.world.phys.AABB box = this.getBoundingBox().inflate(1.2, 0.5, 1.2);
                    java.util.List<net.minecraft.world.entity.LivingEntity> nearby = this.level().getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class, box,
                        e -> e != this && e != this.getFirstPassenger() 
                            && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame())
                            && !(e instanceof net.minecraft.world.entity.npc.AbstractVillager)
                            && !(e instanceof net.minecraft.world.entity.animal.AbstractGolem)
                    );

                    boolean hitSomething = false;

                    for (net.minecraft.world.entity.LivingEntity target : nearby) {
                        long lastTrample = trampleCooldowns.getOrDefault(target.getUUID(), 0L);
                        // 40L ticks = 2 seconds cooldown per entity to prevent infinite juggling
                        if (gameTime - lastTrample >= 40L) {
                            trampleCooldowns.put(target.getUUID(), gameTime);
                            hitSomething = true;

                            float damage = 0.5f; // 0.25 hearts
                            boolean armored = false;
                            
                            for (ItemStack slotArmor : target.getArmorSlots()) {
                                if (!slotArmor.isEmpty()) {
                                    String name = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(slotArmor.getItem()).getPath().toLowerCase();
                                    if (name.contains("iron") || name.contains("diamond") || name.contains("netherite")) {
                                        armored = true;
                                        break;
                                    }
                                }
                            }
                            
                            if (armored) damage = 0.2f; // Deal highly reduced damage to heavily armored units
                            
                            // Prevent lethality
                            if (target.getHealth() - damage < 1.0f) {
                                damage = Math.max(0.0f, target.getHealth() - 1.0f);
                            }

                            if (damage > 0.0f) {
                                target.hurt(this.damageSources().mobAttack(this), damage);
                            }

                            net.minecraft.world.phys.Vec3 look = this.getLookAngle();
                            double sideX = -look.z;
                            double sideZ = look.x;
                            double tx = target.getX() - this.getX();
                            double tz = target.getZ() - this.getZ();
                            double dot = tx * sideX + tz * sideZ;
                            double pushX = dot > 0 ? sideX : -sideX;
                            double pushZ = dot > 0 ? sideZ : -sideZ;
                            double len = Math.sqrt(pushX * pushX + pushZ * pushZ);
                            if (len > 0) { pushX /= len; pushZ /= len; }

                            target.knockback(0.6, -pushX, -pushZ);
                            this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.HORSE_STEP_WOOD, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                        }
                    }

                    if (hitSomething) {
                        // Realistically slow down the horse upon impacting a target
                        this.throttle = Math.max(0.1, this.throttle - 0.25);
                    }
                }
                if (this.tickCount % 400 == 0) {
                    long gt = this.level().getGameTime();
                    trampleCooldowns.entrySet().removeIf(e -> gt - e.getValue() > 400L);
                }
            }
        }
    }

    public void removePanicGoal() {
        this.goalSelector.getAvailableGoals().removeIf(wrappedGoal -> wrappedGoal.getGoal() instanceof net.minecraft.world.entity.ai.goal.PanicGoal);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.SUGAR) || stack.is(Items.HAY_BLOCK) || stack.is(Items.APPLE) || stack.is(Items.GOLDEN_CARROT) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    private void consumeItemFromPlayer(Player player, net.minecraft.world.item.Item item) {
        if (player.getMainHandItem().is(item)) {
            player.getMainHandItem().shrink(1);
        } else if (player.getOffhandItem().is(item)) {
            player.getOffhandItem().shrink(1);
        } else {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(item)) {
                    stack.shrink(1);
                    break;
                }
            }
        }
    }

    private void displaySubtitle(ServerPlayer player, Component text) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 80, 20));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(text));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.empty()));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (itemstack.is(Items.DIAMOND_HORSE_ARMOR) || itemstack.is(Items.GOLDEN_HORSE_ARMOR)) {
            if (!this.level().isClientSide) {
                player.displayClientMessage(Component.literal("This armor type is historically inaccurate and cannot be equipped.").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        if (itemstack.is(Items.NAME_TAG) && itemstack.hasCustomHoverName()) {
            if (!this.level().isClientSide) {
                String plainName = itemstack.getHoverName().getString();
                String className = switch(this.getHorseClass()) {
                    case CLASS_ROUNCEY -> "Rouncey";
                    case CLASS_DESTRIER -> "Destrier";
                    case CLASS_COURSER -> "Courser";
                    case CLASS_PALFREY -> "Palfrey";
                    default -> "Wild";
                };
                this.setCustomName(Component.literal(plainName + " (" + className + ")"));
                this.setCustomNameVisible(true);
                this.setPersistenceRequired();
                if (!player.isCreative()) itemstack.shrink(1);
                this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!this.isTamed()) {
            if (isFood(itemstack)) {
                if (!this.level().isClientSide) {
                    if (!this.isLeashed()) {
                        player.displayClientMessage(Component.literal("The wild horse refuses to eat unless tied down.").withStyle(ChatFormatting.RED), true);
                        return InteractionResult.CONSUME;
                    }
                    long currentDay = this.level().getDayTime() / 24000L;
                    if (currentDay != this.lastFoodDay) {
                        this.dailyFood = 0;
                        this.lastFoodDay = currentDay;
                    }
                    if (this.dailyFood >= 4) {
                        player.displayClientMessage(Component.literal("The horse is full for today. Give it time to rest.").withStyle(ChatFormatting.YELLOW), true);
                        return InteractionResult.CONSUME;
                    }
                    if (!player.isCreative()) itemstack.shrink(1);
                    this.dailyFood++;
                    this.customTemper++;
                    this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_EAT, SoundSource.NEUTRAL, 1.0f, 1.0f);

                    if (this.customTemper >= 16 && this.dailyFood >= 4) {
                        this.setTamed(true);
                        this.tameWithName(player);
                        this.setHorseClass(CLASS_ROUNCEY);
                        this.applyClassAttributeCaps(CLASS_ROUNCEY);
                        this.heal(this.getMaxHealth());
                        
                        // Spawns vanilla standard tame hearts
                        this.level().broadcastEntityEvent(this, (byte) 7);
                        
                        if (player instanceof ServerPlayer sp) {
                            displaySubtitle(sp, Component.literal("The horse has been broken! It is now a rideable Rouncey.").withStyle(ChatFormatting.GREEN));
                        }
                    } else {
                        if (this.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.5, this.getZ(), 5, 0.5, 0.5, 0.5,0.0);
                        }
                        player.displayClientMessage(Component.literal("The horse accepts the food. (" + this.customTemper + "/16, Daily: " + this.dailyFood + "/4)").withStyle(ChatFormatting.GREEN), true);
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (!itemstack.isEmpty() && !itemstack.is(Items.LEAD)) {
                return InteractionResult.PASS;
            }
        }

        if (!this.level().isClientSide && this.getHorseClass() == CLASS_ROUNCEY) {
            if (itemstack.is(Items.IRON_INGOT)) {
                if (this.combatXP >= 125) {
                    if (!player.isCreative()) itemstack.shrink(1);
                    this.setHorseClass(CLASS_DESTRIER);
                    this.removePanicGoal();
                    this.applyClassAttributeCaps(CLASS_DESTRIER);
                    this.agilityXP = -1;
                    this.heal(this.getMaxHealth());
                    if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.5, this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    
                    if (player instanceof ServerPlayer sp) {
                        displaySubtitle(sp, Component.literal("Your Rouncey has become a fearless Destrier!").withStyle(ChatFormatting.GOLD));
                    }
                } else {
                    this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.7f, 0.8f);
                    player.displayClientMessage(Component.literal("This Rouncey hasn't seen enough battle to become a Destrier. (CombatXP: " + this.combatXP + "/125)").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.CONSUME;
            } else if (itemstack.is(Items.LEATHER) || itemstack.is(Items.FEATHER)) {
                if (this.agilityXP >= 150) {
                    boolean hasLeather = false;
                    boolean hasFeather = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack s = player.getInventory().getItem(i);
                        if (s.is(Items.LEATHER)) hasLeather = true;
                        if (s.is(Items.FEATHER)) hasFeather = true;
                    }
                    if (hasLeather && hasFeather) {
                        if (!player.isCreative()) {
                            consumeItemFromPlayer(player, Items.LEATHER);
                            consumeItemFromPlayer(player, Items.FEATHER);
                        }
                        this.setHorseClass(CLASS_COURSER);
                        this.applyClassAttributeCaps(CLASS_COURSER);
                        this.combatXP = -1;
                        this.heal(this.getMaxHealth());
                        if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.5, this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                        this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_BREATHE, SoundSource.NEUTRAL, 1.0f, 1.0f);
                        
                        if (player instanceof ServerPlayer sp) {
                            displaySubtitle(sp, Component.literal("Your Rouncey has become a swift Courser!").withStyle(ChatFormatting.AQUA));
                        }
                    } else {
                        this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.7f, 0.8f);
                        player.displayClientMessage(Component.literal("You need BOTH Leather and a Feather in your inventory to upgrade to a Courser.").withStyle(ChatFormatting.RED), true);
                    }
                } else {
                    this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.7f, 0.8f);
                    player.displayClientMessage(Component.literal("This Rouncey lacks the nimble footwork to become a Courser. (AgilityXP: " + this.agilityXP + "/150)").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.CONSUME;
            } else if (itemstack.is(Items.EMERALD)) {
                if (!player.isCreative()) itemstack.shrink(1);
                this.setHorseClass(CLASS_PALFREY);
                this.applyClassAttributeCaps(CLASS_PALFREY);
                this.combatXP = -1;
                this.agilityXP = -1;
                this.heal(this.getMaxHealth());
                if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.5, this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                this.level().playSound(null, this.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                
                if (player instanceof ServerPlayer sp) {
                    displaySubtitle(sp, Component.literal("Your Rouncey has been refined into a luxurious Palfrey!").withStyle(ChatFormatting.GREEN));
                }
                return InteractionResult.CONSUME;
            }
        }

        return super.mobInteract(player, hand);
    }

    public void applyClassAttributeCaps(int horseClass) {
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = this.getAttribute(Attributes.JUMP_STRENGTH);

        double healthMin = 15.0, healthMax = 20.0;
        double speedMin = 0.17, speedMax = 0.20;
        double jumpMin = 0.45, jumpMax = 0.50;

        switch (horseClass) {
            case CLASS_ROUNCEY -> { healthMin = 19.0; healthMax = 22.0; speedMin = 0.21; speedMax = 0.24; jumpMin = 0.50; jumpMax = 0.55;}
            case CLASS_DESTRIER -> { healthMin = 28.0; healthMax = 32.0; speedMin = 0.19; speedMax = 0.22; jumpMin = 0.40; jumpMax = 0.45; }
            case CLASS_COURSER -> { healthMin = 16.0; healthMax = 18.0; speedMin = 0.31; speedMax = 0.34; jumpMin = 0.70; jumpMax = 0.75;}
            case CLASS_PALFREY -> { healthMin = 22.0; healthMax = 24.0; speedMin = 0.26; speedMax = 0.28; jumpMin = 0.57; jumpMax = 0.60;}
        }

        if (health != null) health.setBaseValue(healthMin + this.random.nextDouble() * (healthMax - healthMin));
        if (speed != null) speed.setBaseValue(speedMin + this.random.nextDouble() * (speedMax - speedMin));
        if (jump != null) jump.setBaseValue(jumpMin + this.random.nextDouble() * (jumpMax - jumpMin));
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public boolean canJump() { return super.canJump() && this.jumpCooldown <= 0; }
    
    @Override
    public void onPlayerJump(int pJumpPower) {
        if (this.jumpCooldown > 0) return;
        super.onPlayerJump(pJumpPower);
        this.jumpCooldown = 20; 
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("AetasClass", this.getHorseClass());
        nbt.putInt("AetasCombatXP", this.combatXP);
        nbt.putInt("AetasAgilityXP", this.agilityXP);
        nbt.putInt("AetasDailyFood", this.dailyFood);
        nbt.putLong("AetasLastFoodDay", this.lastFoodDay);
        nbt.putInt("AetasCustomTemper", this.customTemper);
        nbt.putBoolean("AetasInitialized", this.isClassInitialized);
        nbt.putBoolean("AetasWalkModeState", this.isWalkMode());
        nbt.putDouble("AetasThrottle", this.throttle);
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("AetasClass")) this.setHorseClass(nbt.getInt("AetasClass"));
        this.combatXP = nbt.getInt("AetasCombatXP");
        this.agilityXP = nbt.getInt("AetasAgilityXP");
        this.dailyFood = nbt.getInt("AetasDailyFood");
        this.lastFoodDay = nbt.getLong("AetasLastFoodDay");
        this.customTemper = nbt.getInt("AetasCustomTemper");
        this.isClassInitialized = nbt.getBoolean("AetasInitialized");
        this.setWalkMode(nbt.getBoolean("AetasWalkModeState"));
        if (nbt.contains("AetasThrottle")) this.throttle = nbt.getDouble("AetasThrottle");
    }

    public int getCombatXP() { return combatXP; }
    public void setCombatXP(int xp) { this.combatXP = xp; }
    public int getAgilityXP() { return agilityXP; }
    public void setAgilityXP(int xp) { this.agilityXP = xp; }
    public int getDailyFood() { return dailyFood; }
    public void setDailyFood(int food) { this.dailyFood = food; }
    public long getLastFoodDay() { return lastFoodDay; }
    public void setLastFoodDay(long day) { this.lastFoodDay = day; }
    public int getCustomTemper() { return customTemper; }
    public void setCustomTemper(int temper) { this.customTemper = temper; }

    @Override
    protected void tickRidden(net.minecraft.world.entity.player.Player rider, net.minecraft.world.phys.Vec3 travelVector) {
        super.tickRidden(rider, travelVector);

        int horseClass = this.getHorseClass();
        net.minecraft.world.item.ItemStack armor = this.getArmor();

        double riseTimeTicks = switch (horseClass) {
            case CLASS_COURSER  -> 40.0;
            case CLASS_PALFREY  -> 70.0;
            case CLASS_DESTRIER -> 80.0;
            default             -> 60.0;
        };
        if (armor.is(net.minecraft.world.item.Items.IRON_HORSE_ARMOR)) riseTimeTicks += 60.0;

        double throttleRise = 1.0 / riseTimeTicks;
        double coastDecay = throttleRise * 0.4;
        double brakeDecay = (armor.is(net.minecraft.world.item.Items.IRON_HORSE_ARMOR) && horseClass == CLASS_DESTRIER) 
            ? throttleRise * 1.2 
            : throttleRise * 3.0;

        float forwardInput = rider.zza;
        boolean isForward = forwardInput > 0.0f;
        boolean isBackward = forwardInput < 0.0f;
        
        if (this.throttle <= 0.01) {
            if (isBackward) {
                this.isReversing = true;
            } else if (isForward) {
                this.isReversing = false;
            }
        }

        double maxThrottle = this.isWalkMode() ? 0.35 : 1.0;

        if (this.throttle > maxThrottle) {
            this.throttle = Math.max(maxThrottle, this.throttle - brakeDecay); 
        } else if ((isForward && !this.isReversing) || (isBackward && this.isReversing)) {
            this.throttle = Math.min(maxThrottle, this.throttle + throttleRise);
        } else if ((isBackward && !this.isReversing) || (isForward && this.isReversing)) {
            this.throttle = Math.max(0.0, this.throttle - brakeDecay);
        } else {
            this.throttle = Math.max(0.0, this.throttle - coastDecay);
        }

        // Hard cap exploit prevention
        if (this.throttle > 1.0) this.throttle = 1.0;
    }

    @Override
    protected net.minecraft.world.phys.Vec3 getRiddenInput(net.minecraft.world.entity.player.Player rider, net.minecraft.world.phys.Vec3 travelVector) {
        float strafe = rider.xxa * 0.5f;
        float forward = 0.0f;

        if (this.throttle > 0.01) {
            forward = this.isReversing ? -1.0f : 1.0f;
        }

        float length = (float) Math.sqrt(forward * forward + strafe * strafe);
        if (length > 1.0f) {
            forward /= length;
            strafe /= length;
        }

        return new net.minecraft.world.phys.Vec3(strafe, 0.0f, forward);
    }

    @Override
    protected float getRiddenSpeed(net.minecraft.world.entity.player.Player rider) {
        double baseSpeed = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        
        double sigmoid = 1.0 / (1.0 + Math.exp(-(this.throttle * 12.0 - 6.0)));
        if (this.throttle <= 0.01) sigmoid = 0.0;

        float outputSpeed = (float) (baseSpeed * sigmoid);
        if (this.isReversing) {
            outputSpeed *= 0.25f;
        }

        // Apply Speed Penalty if non-Warhorse wears Iron Armor
        if (this.getHorseClass() != CLASS_DESTRIER && this.getArmor().is(Items.IRON_HORSE_ARMOR)) {
            outputSpeed *= 0.70f;
        }

        return outputSpeed;
    }
}