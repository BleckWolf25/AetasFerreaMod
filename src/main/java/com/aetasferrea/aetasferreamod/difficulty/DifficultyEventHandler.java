/**
 * @file DifficultyEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Mob spawning, equipment, and conversion limits based on world age.
 *
 * @description
 * Event subscriber that modifies mob spawning rules, equipment, and conversion limits based on the current age (days) of the world.
 * Banishment rules and surface caps throttle mob levels in the early days.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.difficulty;

// ---------- IMPORTS
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ---------- CLASS: DIFFICULTY EVENT HANDLER
@Mod.EventBusSubscriber(modid = "aetasferreamod")
public class DifficultyEventHandler {

    // Cache the weapon array globally to avoid repetitive heap allocation drops during spawn waves
    private static final Item[] STONE_WEAPONS = {
        Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_PICKAXE, Items.STONE_SHOVEL
    };

    // ---------- MOB SPAWNING & EQUIPMENT PIPELINE
    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevelAccessor serverLevel)) return;
        
        Level level = serverLevel.getLevel();
        LivingEntity entity = event.getEntity();
        long days = WorldAgeTracker.getWorldDays(level);
        
        // 1. Permanent Restrictions
        if (entity instanceof Creeper || entity instanceof Witch) {
            event.setSpawnCancelled(true);
            return;
        }

        // 2. Day 0 Banishment Rules
        if (days < 1 && (entity instanceof Skeleton || entity instanceof Vindicator || entity instanceof Drowned)) {
            event.setSpawnCancelled(true);
            return;
        }

        // All subsequent rules apply strictly to active Hostile Monsters
        if (entity instanceof Monster) {
            
            // 3. Global Surface Mob Cap Throttling
            BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
            if (pos.getY() >= level.getSeaLevel()) {
                int cap = WorldAgeTracker.getHostileMobCap(days);
                
                // Inflating 128 blocks creates a huge bounding box
                int currentSurfaceMonsters = level.getEntitiesOfClass(
                    Monster.class, 
                    entity.getBoundingBox().inflate(128.0), 
                    m -> m.getY() >= level.getSeaLevel()
                ).size();
                    
                if (currentSurfaceMonsters >= cap) {
                    event.setSpawnCancelled(true);
                    return; // Gracefully drop out before executing gear alterations
                }
            }

            // 4. Equipment Progression Engine
            // Days 0-3: Strip all equipment completely
            if (days <= 3) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                entity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                entity.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                entity.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                entity.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
                entity.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            }
            
            if (entity instanceof Zombie) {
                // Day 6+: Shield generation pass
                if (days >= 6 && level.random.nextFloat() < 0.15f) {
                    entity.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                }

                // Day 7+: Stone tool/weapon variant pass
                if (days >= 7 && level.random.nextFloat() < 0.25f) {
                    entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(STONE_WEAPONS[level.random.nextInt(STONE_WEAPONS.length)]));
                }
            }
            
            // Day 11+: Armor scaling & Scout generation pass
            if (days >= 11 && (entity instanceof Zombie || entity instanceof Skeleton)) {
                if (level.random.nextFloat() < 0.10f) { // Scout Variant
                    entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                    entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
                    entity.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
                    entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
                    entity.getPersistentData().putBoolean("IsScout", true);
                } else {
                    if (level.random.nextFloat() < 0.3f) entity.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                    if (level.random.nextFloat() < 0.3f) entity.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                }
            }
        }
    }

    // ---------- MOB CONVERSION EVENTS
    @SubscribeEvent
    public static void onLivingConversion(LivingConversionEvent.Pre event) {
        if (event.getEntity() instanceof Zombie && event.getOutcome() == EntityType.DROWNED) {
            Level level = event.getEntity().level();
            if (WorldAgeTracker.getWorldDays(level) < 5) {
                event.setConversionTimer(0);
                event.setCanceled(true);
            }
        }
    }

    // ---------- LIVING TICK EVENTS
    @SubscribeEvent
    @SuppressWarnings("null")
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Zombie zombie && !entity.level().isClientSide) {
            
            // Pre-Day 5: Zombies drown out instead of morphing to Drowned
            if (WorldAgeTracker.getWorldDays(entity.level()) < 5) {
                if (zombie.isUnderWater()) {
                    int air = zombie.getAirSupply();
                    if (air <= -20) {
                        zombie.setAirSupply(0);
                        zombie.hurt(zombie.damageSources().drown(), 2.0F);
                    }
                }
            }
        }
    }
}