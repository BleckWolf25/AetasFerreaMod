/**
 * @file MonarchEntity.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Custom entity for the Hollow Monarch boss.
 *
 * @description
 * Extends Husk with custom AI, attributes, and spawn logic.
 *
 * @since 30/06/2026
 * @updated 02/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity.boss;

// ---------- IMPORTS
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

// ---------- CLASS: MONARCH ENTITY
public class MonarchEntity extends Husk {

    private static final String MONARCH_TAG = "Aetas_IsMonarch";

    public MonarchEntity(EntityType<? extends Husk> type, net.minecraft.world.level.Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new MoveTowardsTargetGoal(this, 0.9D, 32.0F));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // Remove default zombie goals that might interfere
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Husk.createAttributes()
                .add(Objects.requireNonNull(Attributes.MAX_HEALTH), 200.0D)
                .add(Objects.requireNonNull(Attributes.MOVEMENT_SPEED), 0.16D)
                .add(Objects.requireNonNull(Attributes.ATTACK_DAMAGE), 20.0D)
                .add(Objects.requireNonNull(Attributes.ATTACK_SPEED), 0.6D)
                .add(Objects.requireNonNull(Attributes.ATTACK_KNOCKBACK), 1.5D)
                .add(Objects.requireNonNull(Attributes.FOLLOW_RANGE), 32.0D)
                .add(Objects.requireNonNull(Attributes.KNOCKBACK_RESISTANCE), 1.0D);
    }

    @SuppressWarnings("removal")
    @Override
    public SpawnGroupData finalizeSpawn(@Nonnull ServerLevelAccessor level, @Nonnull DifficultyInstance difficulty, @Nonnull MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);

        // Check for safe spawn position (3x3 air space with solid floor)
        if (spawnType != MobSpawnType.COMMAND && spawnType != MobSpawnType.TRIGGERED && !level.getLevel().isClientSide) {
            if (!isSafeSpawnPosition(level, this.blockPosition())) {
                this.discard();
                return data;
            }

            // Check if spawning in a structure
            boolean inStructure = false;
            if (level instanceof ServerLevel serverLevel) {
                inStructure = isInStructure(serverLevel, this.blockPosition());
            }

            // Check region limits for natural and structure spawns
            if (level instanceof ServerLevel serverLevel) {
                String regionKey = getRegionKey(serverLevel, this.blockPosition());
                if (regionKey != null) {
                    if (com.aetasferrea.aetasferreamod.world.MonarchWorldData.get(serverLevel).isGranted(regionKey)) {
                        // Region already has a Monarch, discard this entity
                        this.discard();
                        return data;
                    }
                    // Grant this region
                    com.aetasferrea.aetasferreamod.world.MonarchWorldData.get(serverLevel).grant(regionKey);
                }
            }

            // Spawn 2-4 Vanguards if in a structure
            if (inStructure && level instanceof ServerLevel serverLevel) {
                int vanguardCount = 2 + serverLevel.random.nextInt(3); // 2-4 vanguards
                for (int i = 0; i < vanguardCount; i++) {
                    com.aetasferrea.aetasferreamod.entity.boss.VanguardEntity vanguard =
                        com.aetasferrea.aetasferreamod.entity.ModEntities.VANGUARD.get().create(serverLevel);
                    if (vanguard != null) {
                        // Spawn vanguard near the monarch
                        double angle = serverLevel.random.nextDouble() * 2 * Math.PI;
                        double radius = 3.0D + serverLevel.random.nextDouble() * 3.0D;
                        double x = this.getX() + radius * Math.cos(angle);
                        double z = this.getZ() + radius * Math.sin(angle);
                        int y = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);

                        vanguard.moveTo(x, y, z, serverLevel.random.nextFloat() * 360F, 0.0F);
                        vanguard.finalizeSpawn(serverLevel, Objects.requireNonNull(serverLevel.getCurrentDifficultyAt(Objects.requireNonNull(vanguard.blockPosition()))),
                            MobSpawnType.TRIGGERED, null, null);
                        vanguard.getPersistentData().putBoolean("IsRoyalEscort", true);
                        serverLevel.addFreshEntity(vanguard);
                    }
                }
            }
        }

        // Set tags and persistence
        this.addTag("aetas_monarch");
        this.getPersistentData().putBoolean(MONARCH_TAG, true);
        this.getPersistentData().putBoolean("IsDormant", false);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("The Hollow Monarch").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

        // Apply glowing effect on spawn
        this.setGlowingTag(true);

        // Set health
        this.setHealth(this.getMaxHealth());

        // Equip greatsword
        Item swordItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("spartanweaponry", "diamond_greatsword"));
        if (swordItem == null || swordItem == Items.AIR) {
            swordItem = Items.DIAMOND_SWORD;
        }

        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Objects.requireNonNull(swordItem)));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Objects.requireNonNull(Items.ENDER_EYE)));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Objects.requireNonNull(Items.IRON_HELMET)));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Objects.requireNonNull(Items.IRON_CHESTPLATE)));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Objects.requireNonNull(Items.IRON_LEGGINGS)));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Objects.requireNonNull(Items.IRON_BOOTS)));

        // Prevent drops
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.LEGS, 0.0F);
        this.setDropChance(EquipmentSlot.FEET, 0.0F);

        // Spawn vanguard escorts
        if (!level.getLevel().isClientSide && spawnType != MobSpawnType.COMMAND) {
            spawnVanguardEscorts(level);
        }

        // Strip Enhanced AI
        com.aetasferrea.aetasferreamod.events.GuardianEventHandler.stripEnhancedAI(this);

        // Apply scaling
        com.aetasferrea.aetasferreamod.events.GuardianEventHandler.applyBossScale(this, 2.0F);

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

        // Prevent spawning in bedrock layers (check floor and ceiling)
        if (level.getBlockState(Objects.requireNonNull(pos.below())).is(Objects.requireNonNull(Blocks.BEDROCK)) || level.getBlockState(Objects.requireNonNull(pos.above())).is(Objects.requireNonNull(Blocks.BEDROCK))) {
            return false;
        }

        return true;
    }

    private boolean isInStructure(ServerLevel level, BlockPos pos) {
        var structureRegistry = level.registryAccess().registryOrThrow(Objects.requireNonNull(Registries.STRUCTURE));
        for (Structure structure : level.structureManager().getAllStructuresAt(Objects.requireNonNull(pos)).keySet()) {
            ResourceLocation structureName = structureRegistry.getKey(Objects.requireNonNull(structure));
            if (structureName == null) continue;
            if (structureName.toString().equals("minecraft:fortress") ||
                structureName.toString().equals("medieval_nether:castle") ||
                structureName.toString().equals("betterfortresses:fortress")) {
                var structureStart = level.structureManager().getStructureWithPieceAt(Objects.requireNonNull(pos), structure);
                if (structureStart.isValid()) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getRegionKey(ServerLevel level, BlockPos pos) {
        var structureRegistry = level.registryAccess().registryOrThrow(Objects.requireNonNull(Registries.STRUCTURE));

        // Check structure references first
        for (Structure structure : level.structureManager().getAllStructuresAt(Objects.requireNonNull(pos)).keySet()) {
            ResourceLocation structureName = structureRegistry.getKey(Objects.requireNonNull(structure));
            if (structureName == null) continue;
            if (structureName.toString().equals("minecraft:fortress") ||
                structureName.toString().equals("medieval_nether:castle") ||
                structureName.toString().equals("betterfortresses:fortress")) {
                var structureStart = level.structureManager().getStructureWithPieceAt(Objects.requireNonNull(pos), structure);
                if (structureStart.isValid()) {
                    return structureName.toString() + "_" + structureStart.getBoundingBox().toString();
                }
            }
        }

        // Fallback: check biome references
        ResourceLocation biomeName = level.registryAccess().registryOrThrow(Objects.requireNonNull(Registries.BIOME)).getKey(Objects.requireNonNull(level.getBiome(Objects.requireNonNull(pos)).value()));
        if (biomeName != null) {
            if (biomeName.toString().equals("minecraft:basalt_deltas") ||
                biomeName.toString().equals("minecraft:nether_wastes") ||
                biomeName.toString().equals("minecraft:soul_sand_valley") ||
                biomeName.toString().equals("minecraft:crimson_forest")) {
                int regionX = pos.getX() >> 9; // 512x512 block region
                int regionZ = pos.getZ() >> 9;
                return "biome_monarch_" + regionX + "_" + regionZ;
            }
        }

        return null;
    }

    private void spawnVanguardEscorts(ServerLevelAccessor level) {
        int escortCount = 2 + level.getRandom().nextInt(3); // 2-4 escorts

        for (int i = 0; i < escortCount; i++) {
            VanguardEntity vanguard = com.aetasferrea.aetasferreamod.entity.ModEntities.VANGUARD.get().create(Objects.requireNonNull(this.level()));
            if (vanguard != null) {
                // Spawn offset from monarch
                double offsetX = this.getX() + (level.getRandom().nextDouble() - 0.5D) * 5.0D;
                double offsetZ = this.getZ() + (level.getRandom().nextDouble() - 0.5D) * 5.0D;

                vanguard.moveTo(offsetX, this.getY(), offsetZ, this.getYRot(), 0.0F);
                vanguard.getPersistentData().putBoolean("IsRoyalEscort", true);
                vanguard.finalizeSpawn(level, Objects.requireNonNull(level.getCurrentDifficultyAt(Objects.requireNonNull(vanguard.blockPosition()))), MobSpawnType.TRIGGERED, null, null);
                level.addFreshEntity(vanguard);
            }
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
