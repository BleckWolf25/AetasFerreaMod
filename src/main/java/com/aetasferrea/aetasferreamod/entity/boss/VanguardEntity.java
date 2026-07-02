/**
 * @file VanguardEntity.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Custom entity for the Aetas Vanguard.
 *
 * @description
 * Extends WitherSkeleton with custom AI and attributes.
 *
 * @since 30/06/2026
 * @updated 02/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity.boss;

// ---------- IMPORTS
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

// ---------- CLASS: VANGUARD ENTITY
public class VanguardEntity extends WitherSkeleton {

    private static final String VANGUARD_TAG = "Aetas_IsVanguard";

    public VanguardEntity(EntityType<? extends WitherSkeleton> type, net.minecraft.world.level.Level level) {
        super(type, level);
        this.xpReward = 50;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WitherSkeleton.createAttributes()
                .add(Objects.requireNonNull(Attributes.MAX_HEALTH), 80.0D)
                .add(Objects.requireNonNull(Attributes.MOVEMENT_SPEED), 0.18D)
                .add(Objects.requireNonNull(Attributes.ATTACK_DAMAGE), 9.5D)
                .add(Objects.requireNonNull(Attributes.ATTACK_SPEED), 1.4D)
                .add(Objects.requireNonNull(Attributes.ATTACK_KNOCKBACK), 0.5D)
                .add(Objects.requireNonNull(Attributes.FOLLOW_RANGE), 32.0D)
                .add(Objects.requireNonNull(Attributes.KNOCKBACK_RESISTANCE), 0.5D);
    }

    @SuppressWarnings({ "removal", "unused" })
    @Override
    public SpawnGroupData finalizeSpawn(@Nonnull ServerLevelAccessor level, @Nonnull DifficultyInstance difficulty, @Nonnull MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);

        // Check for safe spawn position (3x3 air space with solid floor)
        if (spawnType != MobSpawnType.COMMAND && spawnType != MobSpawnType.TRIGGERED && !level.getLevel().isClientSide) {
            if (!isSafeSpawnPosition(level, this.blockPosition())) {
                this.discard();
                return data;
            }

            // Check region limits for non-royal escort spawns
            if (!this.getPersistentData().getBoolean("IsRoyalEscort") && level instanceof ServerLevel serverLevel) {
                // Check for nearby Vanguards to prevent overcrowding
                var nearbyVanguards = level.getLevel().getEntitiesOfClass(VanguardEntity.class,
                    Objects.requireNonNull(this.getBoundingBox().inflate(64.0D)),
                    e -> !e.getPersistentData().getBoolean("IsRoyalEscort"));
                if (nearbyVanguards.size() >= 1) {
                    this.discard();
                    return data;
                }
            }
        }

        // Set tags
        this.addTag("aetas_vanguard");
        this.getPersistentData().putBoolean(VANGUARD_TAG, true);
        this.setCustomName(Component.literal("Vanguard").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        this.setCustomNameVisible(false);

        // Set health
        this.setHealth(this.getMaxHealth());

        // Equip spear
        Item spearItem = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("spartanweaponry", "iron_spear"));
        if (spearItem == null || spearItem == Items.AIR) {
            spearItem = Items.IRON_SWORD;
        }

        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Objects.requireNonNull(spearItem)));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Objects.requireNonNull(Items.ENDER_EYE)));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Objects.requireNonNull(Items.IRON_CHESTPLATE)));

        // Prevent drops
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.LEGS, 0.0F);
        this.setDropChance(EquipmentSlot.FEET, 0.0F);

        // Strip Enhanced AI
        com.aetasferrea.aetasferreamod.events.GuardianEventHandler.stripEnhancedAI(this);

        // Apply scaling
        com.aetasferrea.aetasferreamod.events.GuardianEventHandler.applyBossScale(this, 1.2F);

        return data;
    }

    private boolean isSafeSpawnPosition(ServerLevelAccessor level, BlockPos pos) {
        // Check for solid floor
        if (!level.getBlockState(Objects.requireNonNull(pos.below())).isSolidRender(level, Objects.requireNonNull(pos.below()))) {
            return false;
        }

        // Check for air at spawn position and one block above (relaxed from 3x3 to single block)
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(Objects.requireNonNull(pos.above())).isAir()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
