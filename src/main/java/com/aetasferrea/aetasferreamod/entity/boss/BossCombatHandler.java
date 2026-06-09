/**
 * @file BossCombatHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Combat mechanics and behaviors for mini-bosses.
 *
 * @description
 * Event subscriber that manages combat mechanics for mini-bosses, including custom debuffs,
 * knockback, armor shedding, loot drops, AI pathfinding adjustments, guard summoning, and despawn rules.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity.boss;

// ---------- IMPORTS
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("null")
// ---------- CLASS: BOSS COMBAT HANDLER
@Mod.EventBusSubscriber(modid = "aetasferreamod")
public class BossCombatHandler {

    // ---------- LIVING DAMAGE LOGIC (CUSTOM MECHANICS)
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide) return;

        // CASE A: Defiled Castellan Attacking
        if (event.getSource().getEntity() instanceof Zombie attacker && attacker.getPersistentData().getBoolean("IsDefiledCastellan")) {
            // Apply debuffs: Mining Fatigue III (amplifier 2) and Weakness II (amplifier 1)
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 2));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            
            // Apply massive horizontal knockback
            Vec3 vec3 = attacker.position().subtract(victim.position()).normalize();
            victim.knockback(2.0D, vec3.x, vec3.z);
            
            // Call near zombies in 48 blocks to help target the player
            if (victim instanceof Player) {
                AABB aabb = attacker.getBoundingBox().inflate(48.0D);
                List<Zombie> nearbyZombies = level.getEntitiesOfClass(Zombie.class, aabb);
                for (Zombie z : nearbyZombies) {
                    if (z.getTarget() == null) {
                        z.setTarget((Player) victim);
                    }
                }
            }
        }

        // CASE B: Diamond Knight Armor Shedding
        if (victim instanceof Zombie husk && husk.getPersistentData().getBoolean("IsDiamondKnight")) {
            float health = husk.getHealth() - event.getAmount();
            float maxHealth = husk.getMaxHealth();
            float healthPct = health / maxHealth;

            // Shed armor slots sequentially as health drops (Shield -> Helmet -> Boots -> Leggings)
            breakArmor(husk, EquipmentSlot.OFFHAND, healthPct, 0.8f, "diamond_shield_broken");
            breakArmor(husk, EquipmentSlot.HEAD, healthPct, 0.6f, "diamond_helmet_broken");
            breakArmor(husk, EquipmentSlot.FEET, healthPct, 0.4f, "diamond_boots_broken");
            breakArmor(husk, EquipmentSlot.LEGS, healthPct, 0.2f, "diamond_legs_broken");
        }
    }

    /**
     * Internal helper to break and remove a mini-boss armor piece when health drops below a percentage,
     * playing sounds, emitting particle bursts, and increasing the entity's speed.
     */
    private static void breakArmor(Mob mob, EquipmentSlot slot, float healthPct, float threshold, String flagName) {
        if (healthPct <= threshold && !mob.getPersistentData().getBoolean(flagName)) {
            ItemStack item = mob.getItemBySlot(slot);
            if (!item.isEmpty()) {
                mob.getPersistentData().putBoolean(flagName, true);
                mob.setItemSlot(slot, ItemStack.EMPTY);
                
                mob.level().playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
                mob.level().playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.5f);
                
                // Spawn item break particles
                if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, item), 
                        mob.getX(), mob.getY() + 1.0, mob.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                }

                // Increase movement speed permanent modifier to represent armor weight shedding
                net.minecraft.world.entity.ai.attributes.AttributeInstance speed = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(UUID.randomUUID(), "Armor Break Speed", 0.10, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }
    }

    // ---------- SHIELD BLOCK OVERRIDE
    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        // Defiled Castellan's attacks completely bypass shield blocking
        if (event.getDamageSource().getEntity() instanceof Zombie attacker && attacker.getPersistentData().getBoolean("IsDefiledCastellan")) {
            event.setCanceled(true);
        }
    }

    // ---------- DEATH & SPECIAL LOOT DROPS
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide) return;

        // Catena-Mail Vigil Death drops
        if (entity instanceof Skeleton skeleton && skeleton.getPersistentData().getBoolean("IsCatenaVigil")) {
            // Drop steaks scaled by online player count
            int players = level.players().size();
            level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(Items.COOKED_BEEF, 8 * players)));
            
            // 40% chance for associated Vigil Guards to die on boss death
            if (skeleton.getPersistentData().hasUUID("VigilUUID")) {
                UUID vigilId = skeleton.getPersistentData().getUUID("VigilUUID");
                List<Skeleton> guards = level.getEntitiesOfClass(Skeleton.class, skeleton.getBoundingBox().inflate(64.0D), 
                    s -> s.getPersistentData().getBoolean("IsVigilGuard") && s.getPersistentData().hasUUID("VigilOwner") && vigilId.equals(s.getPersistentData().getUUID("VigilOwner")));
                for (Skeleton guard : guards) {
                    if (level.random.nextFloat() < 0.40f) {
                        guard.hurt(guard.damageSources().magic(), 1000.0F);
                    }
                }
            }
        }
        // Zombie mini-boss death drops
        else if (entity instanceof Zombie zombie) {
            if (zombie.getPersistentData().getBoolean("IsDefiledCastellan")) {
                // Drop 4 Golden Apples
                level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(Items.GOLDEN_APPLE, 4)));
            } else if (zombie.getPersistentData().getBoolean("IsDeadIronKnight")) {
                // Drop random Gunpowder (4 to 8)
                int gunpowder = 4 + level.random.nextInt(5);
                level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(Items.GUNPOWDER, gunpowder)));
            }
        }
    }

    // ---------- BOSS TICKING LOGIC (AI & SQUADDING)
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide || !(entity instanceof Mob mob)) return;

        // Catena-Mail Vigil logic: Stationary Archer & Guard Summoner
        if (mob.getPersistentData().getBoolean("IsCatenaVigil")) {
            LivingEntity target = mob.getTarget();
            
            // Stationary Firing: Immobilize boss using Slowness 7 if target is within 15 blocks
            if (target != null && mob.distanceToSqr(target) < 225.0D) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
            }

            // Periodic Guard Spawning (Every 3 seconds)
            if (level.getGameTime() % 60 == 0) {
                UUID vigilId;
                if (!mob.getPersistentData().hasUUID("VigilUUID")) {
                    vigilId = UUID.randomUUID();
                    mob.getPersistentData().putUUID("VigilUUID", vigilId);
                } else {
                    vigilId = mob.getPersistentData().getUUID("VigilUUID");
                }
                
                final UUID vId = vigilId;
                List<Skeleton> guards = level.getEntitiesOfClass(Skeleton.class, mob.getBoundingBox().inflate(32.0D), 
                    s -> s.getPersistentData().getBoolean("IsVigilGuard") && s.getPersistentData().hasUUID("VigilOwner") && vId.equals(s.getPersistentData().getUUID("VigilOwner")));
                
                // Maximum 5 active guards
                if (guards.size() < 5) {
                    long nextSpawn = mob.getPersistentData().getLong("NextGuardSpawnTime");
                    if (level.getGameTime() >= nextSpawn) {
                        // Spawn a guard 3 to 6 blocks away
                        double angle = level.random.nextDouble() * 2 * Math.PI;
                        double radius = 3.0D + level.random.nextDouble() * 3.0D;
                        double x = mob.getX() + radius * Math.cos(angle);
                        double z = mob.getZ() + radius * Math.sin(angle);
                        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
                        
                        Skeleton guard = EntityType.SKELETON.create(level);
                        if (guard != null) {
                            guard.setPos(x, y, z);
                            guard.setHealth(6.0F); // 3 hearts
                            guard.getPersistentData().putBoolean("IsVigilGuard", true);
                            guard.getPersistentData().putUUID("VigilOwner", vigilId);

                            guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                            guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                            guard.setDropChance(EquipmentSlot.HEAD, 0.0F);
                            guard.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                            guard.setPersistenceRequired();
                            
                            level.addFreshEntity(guard);
                            
                            // Set a 15-second cooldown (300 ticks) for the next guard spawn
                            mob.getPersistentData().putLong("NextGuardSpawnTime", level.getGameTime() + 300);
                        }
                    }
                }
            }
        }

        // Sapper Squad & Iron Knight: Anti-Fortification logic (Bypasses walls by digging)
        if (mob.getPersistentData().getBoolean("IsSapperSquad") || mob.getPersistentData().getBoolean("IsDeadIronKnight")) {
            if (mob.horizontalCollision && level.getGameTime() % 20 == 0) {
                // Break blocks directly in front of the entity's path
                Vec3 look = mob.getLookAngle();
                BlockPos pos = BlockPos.containing(mob.getX() + look.x, mob.getY() + 1, mob.getZ() + look.z);
                BlockPos.MutableBlockPos mutablePos = pos.mutable();
                BlockState state = level.getBlockState(mutablePos);
                if (!state.isAir() && state.getDestroySpeed(level, mutablePos) >= 0 && state.getDestroySpeed(level, mutablePos) <= 5.0f) {
                    level.destroyBlock(mutablePos, true);
                }
                
                pos = BlockPos.containing(mob.getX() + look.x, mob.getY(), mob.getZ() + look.z);
                mutablePos = pos.mutable();
                state = level.getBlockState(mutablePos);
                if (!state.isAir() && state.getDestroySpeed(level, mutablePos) >= 0 && state.getDestroySpeed(level, mutablePos) <= 5.0f) {
                    level.destroyBlock(mutablePos, true);
                }
            }
        }
    }

    // ---------- DESPAWN LOGIC (PREVENT DESPAWN AT NIGHT)
    @SubscribeEvent
    public static void onDespawn(MobSpawnEvent.AllowDespawn event) {
        LivingEntity entity = event.getEntity();
        if (entity.getPersistentData().getBoolean("IsCatenaVigil") ||
            entity.getPersistentData().getBoolean("IsDefiledCastellan") ||
            entity.getPersistentData().getBoolean("IsDeadIronKnight")) {
            
            // Force bosses to persist during nighttime (deny despawn)
            if (!entity.level().isDay()) {
                event.setResult(Event.Result.DENY);
            } else {
                event.setResult(Event.Result.DEFAULT);
            }
        }
    }
}
