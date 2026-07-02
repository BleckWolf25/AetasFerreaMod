/**
 * @file AetasMule.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Custom Mule entity with Aetas Ferrea equine mechanics.
 *
 * @description
 * Extends Mule to implement the Aetas Ferrea custom breaking/taming system, walk-mode speed capping,
 * stat rolling (speed, jump, health), a custom 15-slot chest inventory, saddle/equipment support,
 * and NBT serialization of all custom fields.
 *
 * @since 20/05/2026
 * @updated 24/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity;

// ---------- IMPORTS
import java.util.Objects;
import javax.annotation.Nonnull;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.Container;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

// ---------- CLASS: AetasMule
@SuppressWarnings({"deprecation"})
public class AetasMule extends Mule {

    @Nonnull
    private static final EntityDataAccessor<Boolean> DATA_WALK_MODE = Objects.requireNonNull(
            SynchedEntityData.defineId(AetasMule.class, Objects.requireNonNull(EntityDataSerializers.BOOLEAN))
    );

    private double throttle = 0.0;
    private int jumpCooldown = 0;
    private boolean isReversing = false;

    private boolean isSwimming = false;
    private boolean wasSwimming = false;
    private int swimTicks = 0;
    private boolean isStatsChecked = false;

    public AetasMule(EntityType<? extends Mule> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_WALK_MODE, false);
    }

    public boolean isWalkMode() { return this.entityData.get(DATA_WALK_MODE); }
    public void setWalkMode(boolean walkMode) { this.entityData.set(DATA_WALK_MODE, walkMode); }
    public double getThrottle() { return this.throttle; }

    public void rerollStats(@Nonnull net.minecraft.util.RandomSource random) {
        AttributeInstance speed = this.getAttribute(Objects.requireNonNull(Attributes.MOVEMENT_SPEED));
        if (speed != null) speed.setBaseValue(0.19 + random.nextDouble() * (0.20 - 0.19));
        AttributeInstance jump = this.getAttribute(Objects.requireNonNull(Attributes.JUMP_STRENGTH));
        if (jump != null) jump.setBaseValue(0.75 + random.nextDouble() * (0.90 - 0.75));
        AttributeInstance health = this.getAttribute(Objects.requireNonNull(Attributes.MAX_HEALTH));
        if (health != null) health.setBaseValue(30.0 + random.nextInt(11));
        this.setHealth(this.getMaxHealth());
    }

    public void enforceStats() {
        AttributeInstance speed = this.getAttribute(Objects.requireNonNull(Attributes.MOVEMENT_SPEED));
        if (speed != null) {
            double v = speed.getBaseValue();
            if (v < 0.19 || v > 0.20) {
                speed.setBaseValue(Math.max(0.19, Math.min(0.20, v)));
            }
        }
        AttributeInstance jump = this.getAttribute(Objects.requireNonNull(Attributes.JUMP_STRENGTH));
        if (jump != null) {
            double v = jump.getBaseValue();
            if (v < 0.75 || v > 0.90) {
                jump.setBaseValue(Math.max(0.75, Math.min(0.90, v)));
            }
        }
        AttributeInstance health = this.getAttribute(Objects.requireNonNull(Attributes.MAX_HEALTH));
        if (health != null) {
            double v = health.getBaseValue();
            if (v < 30.0 || v > 40.0) {
                health.setBaseValue(30.0 + this.random.nextInt(11));
                if (this.getHealth() > this.getMaxHealth()) {
                    this.setHealth(this.getMaxHealth());
                }
            }
        }
    }

    public void equipSaddleForTesting() {
        this.inventory.setItem(0, new net.minecraft.world.item.ItemStack(Objects.requireNonNull(net.minecraft.world.item.Items.SADDLE)));
    }

    public void setChestAndCreateInventory(boolean chested) {
        this.setChest(chested);
        this.createInventory();
    }

    @Override
    protected net.minecraft.network.chat.Component getTypeName() {
        return net.minecraft.network.chat.Component.translatable("entity.minecraft.mule");
    }

    @Override
    protected void randomizeAttributes(@Nonnull net.minecraft.util.RandomSource random) {
        super.randomizeAttributes(random);
        rerollStats(random);
    }

    @Override
    protected int getInventorySize() {
        return this.hasChest() ? 56 : super.getInventorySize();
    }

    @Override
    public int getInventoryColumns() {
        return 0; // Forces the riding GUI to create and render 0 chest slots
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("AetasWalkModeState", this.isWalkMode());
        nbt.putDouble("AetasThrottle", this.throttle);
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setWalkMode(nbt.getBoolean("AetasWalkModeState"));
        if (nbt.contains("AetasThrottle")) this.throttle = nbt.getDouble("AetasThrottle");
        this.enforceStats();
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && !this.isStatsChecked) {
            this.enforceStats();
            this.isStatsChecked = true;
        }

        // Auto-taming
        if (!this.level().isClientSide && !this.isTamed()) {
            this.setTamed(true);
        }

        if (this.jumpCooldown > 0) this.jumpCooldown--;

        double waterHeight = this.getFluidHeight(Objects.requireNonNull(net.minecraft.tags.FluidTags.WATER));
        if (this.isSwimming) {
            if (waterHeight < 0.3 || (this.onGround() && waterHeight < 0.7)) this.isSwimming = false;
        } else {
            if (waterHeight > 0.8 || (waterHeight > 0.5 && !this.onGround())) this.isSwimming = true;
        }

        if (this.isSwimming) {
            net.minecraft.world.phys.Vec3 mot = this.getDeltaMovement();
            if (this.hasChest()) {
                if (mot.y > -0.05) this.setDeltaMovement(mot.x, mot.y - 0.04, mot.z); // Sink completely
            } else {
                if (waterHeight > 0.65) {
                    double targetY = 0.04;
                    if (mot.y < targetY) this.setDeltaMovement(mot.x, Math.min(mot.y + 0.02, targetY), mot.z);
                } else if (waterHeight > 0.4 && mot.y < 0.0) {
                    this.setDeltaMovement(mot.x, mot.y * 0.5, mot.z);
                }
            }
        }

        if (!this.level().isClientSide) {
            if (!this.isSwimming && this.wasSwimming) {
                this.throttle = 0.0;
                this.swimTicks = 0;
            }
            this.wasSwimming = this.isSwimming;

            if (this.isSwimming) {
                this.swimTicks++;
                if (this.swimTicks > 100) {
                    // Calm submergence: takes damage but doesn't eject
                    if (this.swimTicks % 20 == 0) {
                        this.hurt(Objects.requireNonNull(this.damageSources().drown()), 1.0f);
                    }
                }
            } else {
                this.swimTicks = 0;
            }

            if (this.isSwimming && this.level() instanceof ServerLevel sl) {
                if (this.tickCount % 10 == 0) sl.sendParticles(Objects.requireNonNull(ParticleTypes.SPLASH), this.getX(), this.getY() + 0.8, this.getZ(), 5, 0.6, 0.1, 0.6, 0.0);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(@Nonnull Player player, @Nonnull InteractionHand hand) {
        @Nonnull ItemStack itemstack = Objects.requireNonNull(player.getItemInHand(hand));

        // Let vanilla handle baby feeding and growth
        if (this.isBaby() && this.isFood(itemstack)) {
            return super.mobInteract(player, hand);
        }

        if (this.hasChest() && player.isSecondaryUseActive() && itemstack.is(Objects.requireNonNull(Items.SHEARS))) {
            if (!this.level().isClientSide) {
                this.spawnAtLocation(Objects.requireNonNull(Items.CHEST));
                for (int i = 2; i < this.inventory.getContainerSize(); i++) {
                    @Nonnull ItemStack stack = Objects.requireNonNull(this.inventory.getItem(i));
                    if (!stack.isEmpty()) {
                        this.spawnAtLocation(Objects.requireNonNull(stack.copy()));
                        this.inventory.setItem(i, Objects.requireNonNull(ItemStack.EMPTY));
                    }
                }
                this.setChestAndCreateInventory(false);
                this.level().playSound(null, Objects.requireNonNull(this.blockPosition()), Objects.requireNonNull(SoundEvents.SHEEP_SHEAR), SoundSource.NEUTRAL, 1.0F, 1.0F);
                if (!player.isCreative()) {
                    itemstack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.hasChest() && player.isSecondaryUseActive() && itemstack.isEmpty()) {
            if (!this.level().isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                    (id, playerInv, p) -> new ChestMenu(Objects.requireNonNull(MenuType.GENERIC_9x6), id, playerInv, new MuleChestContainer(this.inventory), 6),
                    Objects.requireNonNull(Component.translatable("gui.aetasferreamod.mule.saddlebags"))
                ));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void openCustomInventoryScreen(@Nonnull Player player) {
        if (!this.level().isClientSide) {
            player.openHorseInventory(this, Objects.requireNonNull(this.inventory));
        }
    }

    @Override
    protected void removePassenger(@Nonnull net.minecraft.world.entity.Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player player && !this.level().isClientSide) {
            if (this.throttle > 0.65 && !this.isWalkMode() && !this.isInWater()) {
                // Fling forward with moderate damage
                player.hurt(Objects.requireNonNull(this.damageSources().fall()), 2.0f);
                net.minecraft.world.phys.Vec3 horseVec = this.getDeltaMovement();
                player.setDeltaMovement(Objects.requireNonNull(player.getDeltaMovement().add(horseVec.x * 1.0, 0.2, horseVec.z * 1.0)));
                player.hurtMarked = true;
            }
        }
    }

    @Override
    public boolean canJump() { return super.canJump() && this.jumpCooldown <= 0 && !this.isSwimming && !this.isInWater(); }

    @Override
    public void onPlayerJump(int pJumpPower) {
        if (this.jumpCooldown > 0) return;
        super.onPlayerJump(pJumpPower);
        this.jumpCooldown = 30; // 1.5 seconds
    }

    @Override
    public void travel(@Nonnull net.minecraft.world.phys.Vec3 travelVector) {
        if (!this.isAlive()) {
            super.travel(travelVector);
            return;
        }

        if (!this.isVehicle() && this.throttle > 0.01) {
            this.throttle = Math.max(0.0, this.throttle - 0.02);
            double baseSpeed = this.getAttributeValue(Objects.requireNonNull(Attributes.MOVEMENT_SPEED));
            double sigmoid = 1.0 / (1.0 + Math.exp(-(this.throttle * 12.0 - 6.0)));
            float outputSpeed = (float) (baseSpeed * sigmoid);
            if (this.isSwimming) outputSpeed = Math.min(outputSpeed, 0.035f); // ~0.8 b/s
            if (this.isReversing) outputSpeed *= 0.25f;
            this.setSpeed(outputSpeed);
            float forward = this.isReversing ? -1.0f : 1.0f;
            super.travel(Objects.requireNonNull(new net.minecraft.world.phys.Vec3(0.0, travelVector.y, forward)));
            return;
        }
        super.travel(travelVector);
    }

    @Override
    protected void tickRidden(@Nonnull net.minecraft.world.entity.player.Player rider, @Nonnull net.minecraft.world.phys.Vec3 travelVector) {
        super.tickRidden(rider, travelVector);

        if (this.isSwimming) {
            this.isReversing = false;
            this.throttle = 0.35;
            return;
        }

        double riseTimeTicks = 60.0; // Responsive acceleration like horses
        double throttleRise = 1.0 / riseTimeTicks;
        double coastDecay = throttleRise * 0.4;
        double brakeDecay = throttleRise * 3.0;

        float forwardInput = rider.zza;
        boolean isForward = forwardInput > 0.0f;
        boolean isBackward = forwardInput < 0.0f;

        if (this.throttle <= 0.01) {
            if (isBackward) this.isReversing = true;
            else if (isForward) this.isReversing = false;
        }

        double maxThrottle = this.isWalkMode() ? 0.40 : 1.0;

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
    protected net.minecraft.world.phys.Vec3 getRiddenInput(@Nonnull net.minecraft.world.entity.player.Player rider, @Nonnull net.minecraft.world.phys.Vec3 travelVector) {
        float strafe = rider.xxa * 0.5f;
        float forward = 0.0f;
        if (this.throttle > 0.01) forward = this.isReversing ? -1.0f : 1.0f;
        float length = (float) Math.sqrt(forward * forward + strafe * strafe);
        if (length > 1.0f) { forward /= length; strafe /= length; }
        return Objects.requireNonNull(new net.minecraft.world.phys.Vec3(strafe, 0.0f, forward));
    }

    @Override
    protected float getRiddenSpeed(@Nonnull net.minecraft.world.entity.player.Player rider) {
        double baseSpeed = this.getAttributeValue(Objects.requireNonNull(Attributes.MOVEMENT_SPEED));
        double sigmoid = 1.0 / (1.0 + Math.exp(-(this.throttle * 12.0 - 6.0)));
        if (this.throttle <= 0.01) sigmoid = 0.0;

        float outputSpeed = (float) (baseSpeed * sigmoid);
        if (this.isReversing) outputSpeed *= 0.25f;
        if (this.isSwimming) outputSpeed = Math.min(outputSpeed, 0.035f); // ~0.8 b/s
        return outputSpeed;
    }

    private static class MuleChestContainer implements Container {
        private final net.minecraft.world.SimpleContainer horseInv;
        public MuleChestContainer(net.minecraft.world.SimpleContainer horseInv) {
            this.horseInv = horseInv;
        }
        @Override public int getContainerSize() { return 54; }
        @Override public boolean isEmpty() {
            for (int i=2; i<56; i++) if (!horseInv.getItem(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot) {
            return slot < 54 ? horseInv.getItem(slot + 2) : ItemStack.EMPTY;
        }
        @Override public ItemStack removeItem(int slot, int amount) {
            return slot < 54 ? horseInv.removeItem(slot + 2, amount) : ItemStack.EMPTY;
        }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            return slot < 54 ? horseInv.removeItemNoUpdate(slot + 2) : ItemStack.EMPTY;
        }
        @Override public void setItem(int slot, @Nonnull ItemStack stack) {
            if (slot < 54) horseInv.setItem(slot + 2, stack);
        }
        @Override public void setChanged() { horseInv.setChanged(); }
        @Override public boolean stillValid(@Nonnull Player player) { return horseInv.stillValid(player); }
        @Override public void clearContent() {}
        @Override public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) { return slot < 54; }
    }
}
