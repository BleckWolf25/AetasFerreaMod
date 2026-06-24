/**
 * @file HorseEventHandler.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Core custom Horse entity implementing the Aetas Ferrea medieval equine system.
 *
 * @description
 * Extends Horse to implement the Aetas Ferrea class-based equine system, including five horse classes
 * (Wild, Rouncey, Destrier, Courser, Palfrey), custom taming via hand-feeding, walk-mode speed capping,
 * throttle-based movement control, combat/agility XP tracking, class specialization via item interaction,
 * a custom saddlebag chest inventory, swimming state management, trample mechanics, and NBT serialization
 * of all custom fields.
 *
 * @since 20/05/2026
 * @updated 24/06/2026
 */

// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity;

// ---------- IMPORTS
import javax.annotation.Nonnull;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.Container;

// ---------- CLASS: HorseEventHandler
@SuppressWarnings({"deprecation", "null"})
public class HorseEventHandler extends Horse {

    public static final int CLASS_WILD = 0;
    public static final int CLASS_ROUNCEY = 1;
    public static final int CLASS_DESTRIER = 2;
    public static final int CLASS_COURSER = 3;
    public static final int CLASS_PALFREY = 4;

    private static final EntityDataAccessor<Integer> DATA_HORSE_CLASS = SynchedEntityData.defineId(HorseEventHandler.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WALK_MODE = SynchedEntityData.defineId(HorseEventHandler.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ID_CHEST = SynchedEntityData.defineId(HorseEventHandler.class, EntityDataSerializers.BOOLEAN);

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

    // Swimming state
    private boolean isSwimming = false;
    private boolean wasSwimming = false;
    private int swimTicks = 0;
    public boolean isHorseSwimming() { return this.isSwimming; }

    public HorseEventHandler(EntityType<? extends Horse> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HORSE_CLASS, CLASS_WILD);
        this.entityData.define(DATA_WALK_MODE, false);
        this.entityData.define(DATA_ID_CHEST, false);
    }

    public int getHorseClass() { return this.entityData.get(DATA_HORSE_CLASS); }
    public void setHorseClass(int horseClass) { this.entityData.set(DATA_HORSE_CLASS, horseClass); }
    public boolean isWalkMode() { return this.entityData.get(DATA_WALK_MODE); }
    public void setWalkMode(boolean walkMode) { this.entityData.set(DATA_WALK_MODE, walkMode); }
    public double getThrottle() { return this.throttle; }
    
    public boolean hasChest() { return this.entityData.get(DATA_ID_CHEST); }
    public void setChest(boolean chested) { this.entityData.set(DATA_ID_CHEST, chested); }

    @Override
    public int getInventorySize() {
        return this.hasChest() ? 11 : super.getInventorySize();
    }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(@Nonnull ServerLevel level, net.minecraft.world.entity.AgeableMob otherParent) {
        HorseEventHandler foal = com.aetasferrea.aetasferreamod.init.EntityInit.AETAS_HORSE.get().create(level);
        if (foal != null) {
            foal.setHorseClass(CLASS_WILD);
        }
        return foal;
    }

    @Override
    public void containerChanged(net.minecraft.world.Container container) {
        super.containerChanged(container);
        if (!this.level().isClientSide) {
            ItemStack armor = this.getArmor();
            if (armor.is(Items.DIAMOND_HORSE_ARMOR) || armor.is(Items.GOLDEN_HORSE_ARMOR)) {
                this.spawnAtLocation(armor.copy());
                // Clear the armor slot (slot 1 in the horse inventory)
                container.setItem(1, ItemStack.EMPTY);
                if (this.getFirstPassenger() instanceof Player rider) {
                    rider.displayClientMessage(Component.translatable("message.aetasferreamod.horse.inaccurate_armor").withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    @Override
    protected void removePassenger(net.minecraft.world.entity.Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player player && !this.level().isClientSide) {
            if (this.throttle >= 0.55 && !this.isWalkMode() && !this.isInWater()) {
                float damage = (float) ((this.throttle - 0.4) * 6.0);
                player.hurt(this.damageSources().fall(), damage);
                
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

        // Swimming State Detection (runs on BOTH client and server)
        double waterHeight = this.getFluidHeight(net.minecraft.tags.FluidTags.WATER);
        
        if (this.isSwimming) {
            // Stop swimming if water is very shallow, or if we touch the ground in shallow water
            if (waterHeight < 0.3 || (this.onGround() && waterHeight < 0.7)) {
                this.isSwimming = false;
            }
        } else {
            // Start swimming if deeply submerged, or in moderate water while not on the ground
            if (waterHeight > 0.8 || (waterHeight > 0.5 && !this.onGround())) {
                this.isSwimming = true;
            }
        }

        // Buoyancy (runs on both sides for smooth rendering)
        if (this.isSwimming) {
            net.minecraft.world.phys.Vec3 mot = this.getDeltaMovement();
            
            if (this.getArmor().is(net.minecraft.world.item.Items.IRON_HORSE_ARMOR)) {
                // Iron Armor causes sinking instead of floating
                if (mot.y > -0.05) {
                    this.setDeltaMovement(mot.x, mot.y - 0.02, mot.z);
                }
            } else {
                // Apply upward force if submerged past the chest (0.6 blocks high)
                if (waterHeight > 0.65) {
                    double targetY = 0.04;
                    if (mot.y < targetY) {
                        this.setDeltaMovement(mot.x, Math.min(mot.y + 0.02, targetY), mot.z);
                    }
                } else if (waterHeight > 0.4 && mot.y < 0.0) {
                    // Soften falling if hovering near the surface
                    this.setDeltaMovement(mot.x, mot.y * 0.5, mot.z);
                }
            }
        }

        if (!this.level().isClientSide) {
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

            // Swimming Transition Messages
            if (this.isSwimming && !this.wasSwimming) {
                if (this.getFirstPassenger() instanceof Player rider) {
                    rider.displayClientMessage(Component.translatable("message.aetasferreamod.horse.begins_swim").withStyle(ChatFormatting.AQUA), true);
                }
            } else if (!this.isSwimming && this.wasSwimming) {
                // Exited water — full throttle reset
                this.throttle = 0.0;
                this.swimTicks = 0;
                if (this.getFirstPassenger() instanceof Player rider) {
                    rider.displayClientMessage(Component.translatable("message.aetasferreamod.horse.finds_footing").withStyle(ChatFormatting.GREEN), true);
                }
            }
            this.wasSwimming = this.isSwimming;

            // Iron Armor Drowning
            if (this.isSwimming && this.getArmor().is(Items.IRON_HORSE_ARMOR)) {
                this.swimTicks++;
                if (this.swimTicks % 40 == 0 && this.getFirstPassenger() instanceof Player rider) {
                    rider.displayClientMessage(Component.translatable("message.aetasferreamod.horse.sinking").withStyle(ChatFormatting.RED), true);
                }
                if (this.swimTicks > 100 && this.swimTicks % 20 == 0) {
                    this.hurt(this.damageSources().drown(), 1.0f);
                }
            } else if (!this.isSwimming) {
                this.swimTicks = 0;
            }

            // Swimming Visual & Audio Effects
            if (this.isSwimming && this.level() instanceof ServerLevel sl) {
                if (this.tickCount % 10 == 0) {
                    sl.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.8, this.getZ(), 5, 0.6, 0.1, 0.6, 0.0);
                }
                if (this.tickCount % 20 == 0) {
                    sl.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 0.3, this.getZ(), 4, 0.4, 0.2, 0.4, 0.0);
                }
                if (this.tickCount % 15 == 0) {
                    this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_SWIM, SoundSource.NEUTRAL, 0.4f, 0.9f + this.random.nextFloat() * 0.2f);
                }
            }

            // Charge / Trample Logic (only on land)
            if (!this.isSwimming && this.isVehicle() && this.tickCount % 2 == 0) {
                if ((this.getHorseClass() == CLASS_DESTRIER || this.getHorseClass() == CLASS_COURSER) && this.throttle >= 0.85 && !this.isReversing) {
                    long gameTime = this.level().getGameTime();
                    net.minecraft.world.phys.AABB box = this.getBoundingBox().inflate(1.2, 0.5, 1.2);
                    java.util.List<net.minecraft.world.entity.LivingEntity> nearby = this.level().getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class, box,
                        e -> e != this && e != this.getFirstPassenger() 
                            && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame())
                    );

                    boolean hitSomething = false;

                    for (net.minecraft.world.entity.LivingEntity target : nearby) {
                        long lastTrample = trampleCooldowns.getOrDefault(target.getUUID(), 0L);
                        if (gameTime - lastTrample >= 40L) {
                            trampleCooldowns.put(target.getUUID(), gameTime);
                            hitSomething = true;

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

                            // Damage logic removed! It now exclusively causes knockback.
                            target.knockback(0.6, -pushX, -pushZ);
                            this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.HORSE_STEP_WOOD, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                        }
                    }

                    if (hitSomething) {
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
                player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.inaccurate_armor").withStyle(ChatFormatting.RED), true);
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
                this.setCustomName(Component.literal(plainName + " (").append(Component.translatable("entity.aetasferreamod.horse." + className.toLowerCase())).append(")"));
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
                        player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.wild_refuses_eat", Component.translatable("entity.aetasferreamod.aetas_horse")).withStyle(ChatFormatting.RED), true);
                        return InteractionResult.CONSUME;
                    }
                    long currentDay = this.level().getDayTime() / 24000L;
                    if (currentDay != this.lastFoodDay) {
                        this.dailyFood = 0;
                        this.lastFoodDay = currentDay;
                    }
                    if (this.dailyFood >= 4) {
                        player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.full_for_today", Component.translatable("entity.aetasferreamod.aetas_horse")).withStyle(ChatFormatting.YELLOW), true);
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
                        
                        this.level().broadcastEntityEvent(this, (byte) 7);
                        
                        if (player instanceof ServerPlayer sp) {
                            displaySubtitle(sp, Component.translatable("message.aetasferreamod.horse.broken", Component.translatable("entity.aetasferreamod.aetas_horse"), Component.translatable("entity.aetasferreamod.horse.rouncey")).withStyle(ChatFormatting.GREEN));
                        }
                    } else {
                        if (this.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.5, this.getZ(), 5, 0.5, 0.5, 0.5,0.0);
                        }
                        player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.accepts_food", Component.translatable("entity.aetasferreamod.aetas_horse"), this.customTemper, this.dailyFood).withStyle(ChatFormatting.GREEN), true);
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
                        displaySubtitle(sp, Component.translatable("message.aetasferreamod.horse.destrier_success", Component.translatable("entity.aetasferreamod.horse.rouncey")).withStyle(ChatFormatting.GOLD));
                    }
                } else {
                    this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.7f, 0.8f);
                    player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.destrier_fail", Component.translatable("entity.aetasferreamod.horse.rouncey"), this.combatXP).withStyle(ChatFormatting.RED), true);
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
                        if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.5,this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                        this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_BREATHE, SoundSource.NEUTRAL, 1.0f, 1.0f);
                        
                        if (player instanceof ServerPlayer sp) {
                            displaySubtitle(sp, Component.translatable("message.aetasferreamod.horse.courser_success", Component.translatable("entity.aetasferreamod.horse.rouncey")).withStyle(ChatFormatting.AQUA));
                        }
                    } else {
                        this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.7f, 0.8f);
                        player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.courser_fail_items").withStyle(ChatFormatting.RED), true);
                    }
                } else {
                    this.level().playSound(null, this.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.7f, 0.8f);
                    player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.courser_fail_xp", Component.translatable("entity.aetasferreamod.horse.rouncey"), this.agilityXP).withStyle(ChatFormatting.RED), true);
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
                    displaySubtitle(sp, Component.translatable("message.aetasferreamod.horse.palfrey_success", Component.translatable("entity.aetasferreamod.horse.rouncey")).withStyle(ChatFormatting.GREEN));
                }
                return InteractionResult.CONSUME;
            }
        }

        if (!this.hasChest() && itemstack.is(Items.CHEST)) {
            if (this.getHorseClass() == CLASS_PALFREY || this.getHorseClass() == CLASS_ROUNCEY) {
                this.setChest(true);
                this.playSound(SoundEvents.DONKEY_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }
                this.createInventory();
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
                if (!this.level().isClientSide) {
                    player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.cannot_carry_saddlebags").withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.CONSUME;
            }
        }

        if (this.hasChest() && player.isSecondaryUseActive() && itemstack.is(Items.SHEARS)) {
            if (!this.level().isClientSide) {
                this.spawnAtLocation(Items.CHEST);
                for (int i = 2; i < this.inventory.getContainerSize(); i++) {
                    ItemStack stack = this.inventory.getItem(i);
                    if (!stack.isEmpty()) {
                        this.spawnAtLocation(stack.copy());
                        this.inventory.setItem(i, ItemStack.EMPTY);
                    }
                }
                this.setChestAndCreateInventory(false);
                this.level().playSound(null, this.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
                if (!player.isCreative()) {
                    itemstack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.hasChest() && player.isSecondaryUseActive() && itemstack.isEmpty()) {
            if (!this.level().isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                    (id, playerInv, p) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, playerInv, new HorseChestContainer(this.inventory), 1),
                    Component.translatable("gui.aetasferreamod.horse.saddlebags")
                ));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private static class HorseChestContainer implements Container {
        private final net.minecraft.world.SimpleContainer horseInv;
        public HorseChestContainer(net.minecraft.world.SimpleContainer horseInv) {
            this.horseInv = horseInv;
        }
        @Override public int getContainerSize() { return 9; }
        @Override public boolean isEmpty() {
            for (int i=2; i<11; i++) if (!horseInv.getItem(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) {
            return slot < 9 ? horseInv.getItem(slot + 2) : ItemStack.EMPTY;
        }
        @Override public ItemStack removeItem(int slot, int amount) {
            return slot < 9 ? horseInv.removeItem(slot + 2, amount) : ItemStack.EMPTY;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            return slot < 9 ? horseInv.removeItemNoUpdate(slot + 2) : ItemStack.EMPTY;
        }
        @Override public void setItem(int slot, ItemStack stack) {
            if (slot < 9) horseInv.setItem(slot + 2, stack);
        }
        @Override public void setChanged() { horseInv.setChanged(); }
        @Override public boolean stillValid(Player player) { return horseInv.stillValid(player); }
        @Override public void clearContent() {}
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot < 9; }
    }

    public void applyClassAttributeCaps(int horseClass) {
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = this.getAttribute(Attributes.JUMP_STRENGTH);

        double healthMin = 15.0, healthMax = 20.0;
        double speedMin = 0.17, speedMax = 0.20;
        double jumpMin = 0.45, jumpMax = 0.50;

        switch (horseClass) {
            case CLASS_WILD -> { healthMin = 15.0; healthMax = 20.0; speedMin = 0.17; speedMax = 0.20; jumpMin = 0.45; jumpMax = 0.50;}
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

    public void initializeWithClass(int horseClass) {
        this.setHorseClass(horseClass);
        this.applyClassAttributeCaps(horseClass);
        this.isClassInitialized = true;
    }

    public void equipSaddleForTesting() {
        this.inventory.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE));
    }

    public void setChestAndCreateInventory(boolean chested) {
        this.setChest(chested);
        this.createInventory();
    }

    @Override
    public boolean canJump() { return super.canJump() && this.jumpCooldown <= 0 && !this.isSwimming && !this.isInWater(); }
    
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
        nbt.putBoolean("ChestedHorse", this.hasChest());
        if (this.hasChest()) {
            ListTag listtag = new ListTag();
            for(int i = 2; i < this.inventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.inventory.getItem(i);
                if (!itemstack.isEmpty()) {
                    CompoundTag compoundtag = new CompoundTag();
                    compoundtag.putByte("Slot", (byte)i);
                    itemstack.save(compoundtag);
                    listtag.add(compoundtag);
                }
            }
            nbt.put("Items", listtag);
        }
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
        this.setChest(nbt.getBoolean("ChestedHorse"));
        if (this.hasChest()) {
            this.createInventory();
            if (nbt.contains("Items", 9)) {
                ListTag listtag = nbt.getList("Items", 10);
                for(int i = 0; i < listtag.size(); ++i) {
                    CompoundTag compoundtag = listtag.getCompound(i);
                    int j = compoundtag.getByte("Slot") & 255;
                    if (j >= 2 && j < this.inventory.getContainerSize()) {
                        this.inventory.setItem(j, ItemStack.of(compoundtag));
                    }
                }
            }
        }
        // Enforce stat caps on every load to fix pre-existing horses with out-of-range stats
        if (this.isClassInitialized) {
            this.enforceClassAttributeCaps(this.getHorseClass());
        }
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (this.hasChest()) {
            if (!this.level().isClientSide) {
                this.spawnAtLocation(Blocks.CHEST);
            }
            this.setChest(false);
        }
    }

    /**
     * Clamps existing attribute base values into valid range for the given class.
     * Unlike applyClassAttributeCaps, this does NOT re-roll random values —
     * it preserves existing stats and only adjusts them if they fall outside bounds.
     */
    public void enforceClassAttributeCaps(int horseClass) {
        double healthMin = 15.0, healthMax = 20.0;
        double speedMin = 0.17, speedMax = 0.20;
        double jumpMin = 0.45, jumpMax = 0.50;

        switch (horseClass) {
            case CLASS_WILD     -> { healthMin = 15.0; healthMax = 20.0; speedMin = 0.17; speedMax = 0.20; jumpMin = 0.45; jumpMax = 0.50; }
            case CLASS_ROUNCEY  -> { healthMin = 19.0; healthMax = 22.0; speedMin = 0.21; speedMax = 0.24; jumpMin = 0.50; jumpMax = 0.55; }
            case CLASS_DESTRIER -> { healthMin = 28.0; healthMax = 32.0; speedMin = 0.19; speedMax = 0.22; jumpMin = 0.40; jumpMax = 0.45; }
            case CLASS_COURSER  -> { healthMin = 16.0; healthMax = 18.0; speedMin = 0.31; speedMax = 0.34; jumpMin = 0.70; jumpMax = 0.75; }
            case CLASS_PALFREY  -> { healthMin = 22.0; healthMax = 24.0; speedMin = 0.26; speedMax = 0.28; jumpMin = 0.57; jumpMax = 0.60; }
        }

        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = this.getAttribute(Attributes.JUMP_STRENGTH);

        if (health != null) {
            double v = health.getBaseValue();
            if (v < healthMin || v > healthMax) {
                health.setBaseValue(Math.max(healthMin, Math.min(healthMax, v)));
                this.setHealth(this.getMaxHealth());
            }
        }
        if (speed != null) {
            double v = speed.getBaseValue();
            if (v < speedMin || v > speedMax) {
                speed.setBaseValue(Math.max(speedMin, Math.min(speedMax, v)));
            }
        }
        if (jump != null) {
            double v = jump.getBaseValue();
            if (v < jumpMin || v > jumpMax) {
                jump.setBaseValue(Math.max(jumpMin, Math.min(jumpMax, v)));
            }
        }
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
    public void travel(@Nonnull net.minecraft.world.phys.Vec3 travelVector) {
        if (!this.isAlive()) {
            super.travel(travelVector);
            return;
        }

        // Buoyancy is now handled in tick() for consistency
        if (!this.isVehicle() && this.throttle > 0.01) {
            this.throttle = Math.max(0.0, this.throttle - 0.02);

            double baseSpeed = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double sigmoid = 1.0 / (1.0 + Math.exp(-(this.throttle * 12.0 - 6.0)));
            float outputSpeed = (float) (baseSpeed * sigmoid);

            if (this.getHorseClass() != CLASS_DESTRIER && this.getArmor().is(Items.IRON_HORSE_ARMOR)) {
                outputSpeed = Math.min(outputSpeed, 0.094f); // Hard cap ~4 b/s for non-Destrier with iron armor
            }
            // Cap swimming speed for riderless horses too
            if (this.isSwimming) {
                outputSpeed = Math.min(outputSpeed, 0.047f);
            }
            if (this.isReversing) {
                outputSpeed *= 0.25f;
            }

            this.setSpeed(outputSpeed);
            float forward = this.isReversing ? -1.0f : 1.0f;
            
            super.travel(new net.minecraft.world.phys.Vec3(0.0, travelVector.y, forward));
            return;
        }

        super.travel(travelVector);
    }

    @Override
    protected void tickRidden(net.minecraft.world.entity.player.Player rider, net.minecraft.world.phys.Vec3 travelVector) {
        super.tickRidden(rider, travelVector);

        // Swimming Mode: auto-paddle at constant pace, ignore forward/backward input
        if (this.isSwimming) {
            this.isReversing = false;
            this.throttle = 0.35;
            return;
        }

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

        if (this.getHorseClass() != CLASS_DESTRIER && this.getArmor().is(Items.IRON_HORSE_ARMOR)) {
            outputSpeed = Math.min(outputSpeed, 0.094f);
        }

        // Swimming speed cap
        if (this.isSwimming) {
            outputSpeed = Math.min(outputSpeed, 0.047f);
        }

        return outputSpeed;
    }
}