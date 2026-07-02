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
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity.boss;

// ---------- IMPORTS
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@SuppressWarnings("null")
// ---------- CLASS: BOSS COMBAT HANDLER

public class BossCombatHandler {

    public record ScorchedBlockData(ResourceKey<Level> dimension, long restoreTime, BlockState originalState) {}
    public static final ConcurrentHashMap<BlockPos, ScorchedBlockData> SCORCHED_BLOCKS = new ConcurrentHashMap<>();

    public static boolean isMonarch(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        return entity instanceof MonarchEntity ||
               entity.getTags().contains("aetas_monarch") || entity.getPersistentData().getBoolean("Aetas_IsMonarch") ||
               (entity.getCustomName() != null && entity.getCustomName().getString().contains("Hollow Monarch"));
    }

    public static boolean isVanguard(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        return entity instanceof VanguardEntity ||
               entity.getTags().contains("aetas_vanguard") || entity.getPersistentData().getBoolean("Aetas_IsVanguard") ||
               (entity.getCustomName() != null && entity.getCustomName().getString().contains("Nether Guardian"));
    }

    // ---------- LIVING ATTACK LOGIC (FIRE/LAVA/MAGMA/FALL/MAGIC IMMUNITY)
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide) return;

        if (isMonarch(victim)) {
            if (victim.getPersistentData().getBoolean("IsDormant")) {
                event.setCanceled(true);
                triggerAwakening(victim);
                return;
            }
        }

        if (isMonarch(victim) || isVanguard(victim)) {
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.LAVA) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) ||
                event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)) {
                event.setCanceled(true);
            }
        }
    }

    public static void triggerAwakening(LivingEntity monarch) {
        if (monarch == null || monarch.level().isClientSide || !monarch.getPersistentData().getBoolean("IsDormant")) return;
        monarch.getPersistentData().putBoolean("IsDormant", false);
        monarch.getPersistentData().putBoolean("IsAwakening", true);
        monarch.getPersistentData().putLong("AwakeningTimer", monarch.level().getGameTime() + 40); // 2 seconds
        // Sound effects for the awakening
        monarch.level().playSound(null, monarch.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 0.7f);
        monarch.level().playSound(null, monarch.blockPosition(), net.minecraft.sounds.SoundEvents.IRON_GOLEM_DAMAGE, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.5f);
        // Chat message telegraph to all nearby players
        broadcastToNearby(monarch, net.minecraft.network.chat.Component.literal("\u2694 The Hollow Monarch stirs... it draws its greatsword!").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD), 48.0D);
        if (monarch.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, monarch.getX(), monarch.getY() + 1.0D, monarch.getZ(), 15, 0.5, 0.5, 0.5, 0.05);
        }
    }

    /**
     * Broadcasts a styled chat message to all players within the specified radius of a living entity.
     */
    private static void broadcastToNearby(LivingEntity entity, net.minecraft.network.chat.Component message, double radius) {
        if (entity == null || entity.level().isClientSide) return;
        List<Player> nearbyPlayers = entity.level().getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(radius));
        for (Player player : nearbyPlayers) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(message);
            }
        }
    }

    // ---------- SPARTAN WEAPONRY TWO-HANDED PENALTY BYPASS (MONARCH ATTACK OVERRIDE & SHIELD BREAKING)
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMonarchAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isMonarch(attacker)) {
            if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player && player.isBlocking()) {
                net.minecraft.world.item.ItemStack shield = player.getUseItem();
                player.stopUsingItem();
                if (!shield.isEmpty()) {
                    player.getCooldowns().addCooldown(shield.getItem(), 100);
                    player.level().broadcastEntityEvent(player, (byte) 30);
                    player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.SHIELD_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
                }
            }
            if (event.isCanceled()) {
                event.setCanceled(false);
            }
            if (!attacker.level().isClientSide) {
                attacker.level().playSound(null, attacker.blockPosition(), net.minecraft.sounds.SoundEvents.BLAZE_SHOOT, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.5f);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMonarchHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isMonarch(attacker)) {
            if (event.isCanceled() || event.getAmount() <= 0.0F) {
                event.setCanceled(false);
                if (event.getAmount() <= 0.0F) {
                    var dmgAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
                    float dmg = dmgAttr != null ? (float) dmgAttr.getValue() : 12.0F;
                    event.setAmount(dmg > 0 ? dmg : 12.0F);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMonarchDamageOverride(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && isMonarch(attacker)) {
            if (event.isCanceled() || event.getAmount() <= 0.0F) {
                event.setCanceled(false);
                if (event.getAmount() <= 0.0F) {
                    var dmgAttr = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
                    float dmg = dmgAttr != null ? (float) dmgAttr.getValue() : 12.0F;
                    event.setAmount(dmg > 0 ? dmg : 12.0F);
                }
            }
            if (attacker.getPersistentData().getBoolean("MonarchPhase2")) {
                event.getEntity().setSecondsOnFire(6);
            }
        }
    }

    // ---------- LIVING DAMAGE LOGIC (CUSTOM MECHANICS)
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide) return;

        if (isMonarch(victim) || isVanguard(victim)) {
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.LAVA) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) ||
                event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FALL) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MAGIC) ||
                event.getSource().is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)) {
                event.setCanceled(true);
                return;
            }
        }

    // ---------- KNOCKBACK & POTION IMMUNITY MOVED TO END OF FILE

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

        // CASE C: Hollow Monarch Armor Shedding & Phase 2 Enrage
        if (isMonarch(victim) && victim instanceof Mob mob) {
            float health = mob.getHealth() - event.getAmount();
            float maxHealth = mob.getMaxHealth();
            float healthPct = health / maxHealth;

            breakArmor(mob, EquipmentSlot.HEAD, healthPct, 0.75f, "monarch_helmet_broken");
            if (healthPct <= 0.50f && !mob.getPersistentData().getBoolean("MonarchPhase2")) {
                mob.getPersistentData().putBoolean("MonarchPhase2", true);
                mob.level().playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.8f);
                mob.level().explode(mob, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 3.0F, Level.ExplosionInteraction.BLOCK);
                if (mob.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                }
                var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.addPermanentModifier(new AttributeModifier(UUID.randomUUID(), "Phase 2 Speed", 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
            breakArmor(mob, EquipmentSlot.FEET, healthPct, 0.50f, "monarch_boots_broken");
            breakArmor(mob, EquipmentSlot.LEGS, healthPct, 0.25f, "monarch_legs_broken");
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

        if (isMonarch(entity) || isVanguard(entity)) {
            if (isMonarch(entity)) {
                level.playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_DEATH, net.minecraft.sounds.SoundSource.HOSTILE, 3.0f, 0.8f);
                level.playSound(null, entity.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_DEATH, net.minecraft.sounds.SoundSource.HOSTILE, 3.0f, 0.5f);
            }
            removeBossBar(entity);
        }

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
    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide || !(entity instanceof Mob mob)) return;

        // Hollow Monarch and Nether Guardian: Fire/Lava/Magma/Fall Immunity, Buoyancy & Boss Bars
        if (isMonarch(entity) || isVanguard(entity)) {
            if (isMonarch(entity)) {
                com.aetasferrea.aetasferreamod.events.GuardianEventHandler.stripEnhancedAI(entity);
                if (entity.hasEffect(MobEffects.DIG_SLOWDOWN)) entity.removeEffect(MobEffects.DIG_SLOWDOWN);
                if (entity.hasEffect(MobEffects.WEAKNESS)) entity.removeEffect(MobEffects.WEAKNESS);
                if (entity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                var attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
                if (attackSpeed != null) {
                    attackSpeed.removeModifiers();
                    attackSpeed.setBaseValue(4.0D);
                }
            } else if (isVanguard(entity)) {
                com.aetasferrea.aetasferreamod.events.GuardianEventHandler.allowEnhancedAI(entity);
            }
            if (entity.isOnFire()) {
                entity.clearFire();
            }
            if (isMonarch(entity)) {
                if (mob.getPersistentData().getBoolean("IsDormant")) {
                    mob.getNavigation().stop();
                    mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
                    boolean hasCloseEnemy = false;
                    var closeEnemies = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(10.0D), e -> e != null && e.isAlive() && e != mob && !isMonarch(e) && !isVanguard(e));
                    if (!closeEnemies.isEmpty() || (mob.getLastHurtByMob() != null && mob.getLastHurtByMob().isAlive())) {
                        hasCloseEnemy = true;
                    }
                    if (hasCloseEnemy) {
                        triggerAwakening(mob);
                    }
                    return; // Skip normal combat ticking while dormant
                } else if (mob.getPersistentData().getBoolean("IsAwakening")) {
                    mob.getNavigation().stop();
                    mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
                    if (level.getGameTime() >= mob.getPersistentData().getLong("AwakeningTimer")) {
                        mob.getPersistentData().remove("IsAwakening");
                        mob.getPersistentData().remove("AwakeningTimer");
                        // Chat message: Monarch has fully awakened
                        broadcastToNearby(mob, net.minecraft.network.chat.Component.literal("\u2694 The Hollow Monarch has awakened!").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD), 48.0D);
                        level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.8f);
                    }
                    return; // Skip normal combat ticking while drawing sword
                }

                // Remove lava pathfinding penalty so Monarch does not try to avoid lava
                mob.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
                mob.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
                mob.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);

                // Only apply gentle upward float if submerged in deep lava (> 0.4 blocks), without horizontal jitter or punching.
                // When at or near the surface, canStandOnFluid lets it stand and walk smoothly as if lava is a solid block.
                if (entity.getFluidHeight(FluidTags.LAVA) > 0.4D) {
                    Vec3 motion = entity.getDeltaMovement();
                    if (motion.y < 0.1D) {
                        entity.setDeltaMovement(motion.x, 0.1D, motion.z);
                    }
                }

                // Glowing effect: visible from 100 blocks, removed when player within 20 blocks
                if (level.getGameTime() % 20 == 0) {
                    boolean shouldGlow = true;
                    for (net.minecraft.server.level.ServerPlayer player : ((net.minecraft.server.level.ServerLevel) level).players()) {
                        double dist = mob.distanceToSqr(player);
                        if (dist < 400.0D) { // Within 20 blocks (400 sq dist)
                            shouldGlow = false;
                            break;
                        }
                    }
                    mob.setGlowingTag(shouldGlow);
                }

                // Sentinel Stance: Stand still unless target within 20 blocks or attacked
                if ((mob.getTarget() == null || !mob.getTarget().isAlive() || mob.distanceToSqr(mob.getTarget()) > 400.0D) && (mob.getLastHurtByMob() == null || !mob.getLastHurtByMob().isAlive())) {
                    mob.getNavigation().stop();
                    mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
                }

                // 1. Heavy Metallic Footsteps
                if (mob.getDeltaMovement().horizontalDistanceSqr() > 0.001D && level.getGameTime() % 15 == 0) {
                    level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_STEP, net.minecraft.sounds.SoundSource.HOSTILE, 0.4f, 0.6f);
                }

                // 2. Scorched Earth Lava Trail (temporarily turning solid blocks under feet into Magma Blocks across a 2x2 footprint)
                if (level.getGameTime() % 10 == 0 && mob.onGround() && !mob.isInWater() && !mob.isInLava()) {
                    net.minecraft.world.phys.AABB bb = mob.getBoundingBox().inflate(0.3D, 0, 0.3D);
                    int minX = net.minecraft.util.Mth.floor(bb.minX);
                    int maxX = net.minecraft.util.Mth.floor(bb.maxX);
                    int minZ = net.minecraft.util.Mth.floor(bb.minZ);
                    int maxZ = net.minecraft.util.Mth.floor(bb.maxZ);
                    int yPos = net.minecraft.util.Mth.floor(mob.getY() - 0.2D);
                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            BlockPos underPos = new BlockPos(x, yPos, z);
                            BlockState underState = level.getBlockState(underPos);
                            if (!underState.isAir() && !underState.is(Blocks.MAGMA_BLOCK) && !underState.is(Blocks.BEDROCK) && !underState.is(Blocks.OBSIDIAN) && !underState.is(Blocks.CHEST) && !underState.is(Blocks.ENDER_CHEST) && !underState.is(Blocks.TRAPPED_CHEST) && !underState.hasBlockEntity() && underState.getDestroySpeed(level, underPos) >= 0 && underState.getDestroySpeed(level, underPos) <= 50.0F) {
                                if (!SCORCHED_BLOCKS.containsKey(underPos)) {
                                    SCORCHED_BLOCKS.put(underPos.immutable(), new ScorchedBlockData(level.dimension(), level.getGameTime() + 200, underState));
                                    level.setBlock(underPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }

                // 3. Volcanic Ground Slam (with 2.5s telegraphed windup delay + chat warning)
                long slamWindup = mob.getPersistentData().getLong("SlamWindup");
                if (slamWindup > 0) {
                    mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0); // pause movement during windup
                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 3, 0.5D, 0.5D, 0.5D, 0.1D);
                    }
                    if (level.getGameTime() >= slamWindup) {
                        mob.getPersistentData().remove("SlamWindup");
                        level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 0.6f);
                        level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.5f);
                        level.explode(mob, mob.getX(), mob.getY() + 0.5D, mob.getZ(), 1.5F, Level.ExplosionInteraction.BLOCK);
                        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, mob.getX(), mob.getY() + 0.5D, mob.getZ(), 2, 0.5D, 0.2D, 0.5D, 0.0D);
                            for (int i = 0; i < 24; i++) {
                                double angle = i * (2 * Math.PI / 24);
                                double rx = mob.getX() + 3.5D * Math.cos(angle);
                                double rz = mob.getZ() + 3.5D * Math.sin(angle);
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, rx, mob.getY() + 0.2D, rz, 3, 0.2D, 0.2D, 0.2D, 0.05D);
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, rx, mob.getY() + 0.2D, rz, 1, 0.1D, 0.1D, 0.1D, 0.0D);
                            }
                        }
                        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(8.0D), e -> e != mob && !isMonarch(e) && !isVanguard(e));
                        for (LivingEntity v : victims) {
                            v.setDeltaMovement(v.getDeltaMovement().add(0, 0.7D, 0));
                            v.hurt(level.damageSources().onFire(), 10.0F);
                            v.setSecondsOnFire(5);
                        }
                    }
                } else if (mob.getTarget() != null && mob.getTarget().isAlive() && mob.distanceToSqr(mob.getTarget()) < 100.0D) {
                    long nextSlamTime = mob.getPersistentData().getLong("NextSlamTime");
                    if (level.getGameTime() >= nextSlamTime) {
                        mob.getPersistentData().putLong("NextSlamTime", level.getGameTime() + 300); // 15s cooldown
                        mob.getPersistentData().putLong("SlamWindup", level.getGameTime() + 50); // 2.5s windup delay
                        // Chat message warning + sound telegraph
                        broadcastToNearby(mob, net.minecraft.network.chat.Component.literal("\u26A0 The Hollow Monarch raises its greatsword overhead!").withStyle(net.minecraft.ChatFormatting.RED, net.minecraft.ChatFormatting.BOLD), 32.0D);
                        level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, 1.5f);
                    }
                }
            } else if (isVanguard(entity)) {
                // 1. Phalanx Bodyguard Synergy
                if (level.getGameTime() % 20 == 0) {
                    List<Mob> monarchs = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(24.0D), BossCombatHandler::isMonarch);
                    if (!monarchs.isEmpty()) {
                        for (Mob monarch : monarchs) {
                            monarch.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
                        }
                        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
                        mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 0, false, false));
                    }
                }

                // 2. Tactical Small Jump Forward towards target
                if (mob.getTarget() != null && mob.getTarget().isAlive() && mob.onGround()) {
                    double distSqr = mob.distanceToSqr(mob.getTarget());
                    if (distSqr >= 9.0D && distSqr <= 100.0D) { // between 3 and 10 blocks away
                        long nextHopTime = mob.getPersistentData().getLong("NextHopTime");
                        if (level.getGameTime() >= nextHopTime) {
                            mob.getPersistentData().putLong("NextHopTime", level.getGameTime() + 120); // 6s cooldown
                            Vec3 jumpVec = mob.getTarget().position().subtract(mob.position()).normalize().scale(0.8D).add(0, 0.35D, 0);
                            mob.setDeltaMovement(jumpVec);
                            level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 0.6f);
                        }
                    }
                }

                // 3. Sentinel Stance: Stand still unless any target is within 20 blocks (400 sq dist) or attacked
                if ((mob.getTarget() == null || !mob.getTarget().isAlive() || mob.distanceToSqr(mob.getTarget()) > 400.0D) && (mob.getLastHurtByMob() == null || !mob.getLastHurtByMob().isAlive())) {
                    mob.getNavigation().stop();
                    mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
                }
            }

            // ServerBossEvent tracking for Enhanced Boss Bars resource pack compatibility and UI display
            updateBossBar(entity);
        }

        // Restore expired Scorched Earth blocks
        if (entity.tickCount % 20 == 0 && !SCORCHED_BLOCKS.isEmpty()) {
            SCORCHED_BLOCKS.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                ScorchedBlockData data = entry.getValue();
                if (level.getGameTime() >= data.restoreTime() && level.dimension().equals(data.dimension())) {
                    if (level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)) {
                        level.setBlock(pos, data.originalState(), 3);
                    }
                    return true;
                }
                return false;
            });
        }

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

        // Sapper Squad, Iron Knight & Vanguard: Anti-Fortification logic (Bypasses walls by digging)
        if (mob.getPersistentData().getBoolean("IsSapperSquad") || mob.getPersistentData().getBoolean("IsDeadIronKnight") || isVanguard(mob)) {
            if (mob.horizontalCollision && level.getGameTime() % 20 == 0) {
                // Break blocks directly in front of the entity's path
                Vec3 look = mob.getLookAngle();
                BlockPos pos = BlockPos.containing(mob.getX() + look.x, mob.getY() + 1, mob.getZ() + look.z);
                BlockPos.MutableBlockPos mutablePos = pos.mutable();
                BlockState state = level.getBlockState(mutablePos);
                float maxSpeed = isVanguard(mob) ? 10.0f : 5.0f;
                if (!state.isAir() && state.getDestroySpeed(level, mutablePos) >= 0 && state.getDestroySpeed(level, mutablePos) <= maxSpeed) {
                    level.destroyBlock(mutablePos, true);
                }

                pos = BlockPos.containing(mob.getX() + look.x, mob.getY(), mob.getZ() + look.z);
                mutablePos = pos.mutable();
                state = level.getBlockState(mutablePos);
                if (!state.isAir() && state.getDestroySpeed(level, mutablePos) >= 0 && state.getDestroySpeed(level, mutablePos) <= maxSpeed) {
                    level.destroyBlock(mutablePos, true);
                }
            }
        }
    }

    // ---------- DESPAWN LOGIC (PREVENT DESPAWN AT NIGHT & NEVER DESPAWN MONARCH)
    @SubscribeEvent
    public static void onDespawn(MobSpawnEvent.AllowDespawn event) {
        LivingEntity entity = event.getEntity();
        if (isMonarch(entity)) {
            event.setResult(Event.Result.DENY);
            return;
        }
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

    // ---------- BOSS BAR TRACKING (ENHANCED BOSS BARS COMPATIBILITY)
    private static final Map<UUID, ServerBossEvent> BOSS_BARS = new ConcurrentHashMap<>();

    private static void updateBossBar(LivingEntity entity) {
        // Mini-bosses (like Vanguards) should NEVER have a boss bar. Only track the Hollow Monarch.
        if (entity == null || !isMonarch(entity) || !entity.isAlive() || entity.isRemoved()) {
            removeBossBar(entity);
            return;
        }
        UUID uuid = entity.getUUID();
        ServerBossEvent bossEvent = BOSS_BARS.computeIfAbsent(uuid, id -> {
            ServerBossEvent eventBar = new ServerBossEvent(entity.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            eventBar.setDarkenScreen(true);
            eventBar.setPlayBossMusic(true);
            return eventBar;
        });

        bossEvent.setProgress(entity.getHealth() / entity.getMaxHealth());
        bossEvent.setName(entity.getDisplayName());

        boolean isDormant = entity.getPersistentData().getBoolean("IsDormant");

        if (entity.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (player == null || !player.isAlive()) {
                    bossEvent.removePlayer(player);
                    continue;
                }
                // Only show boss bar if Monarch is awake AND player is within 120 blocks (14400 sq dist) OR actively engaged in combat
                boolean engaged = (entity instanceof Mob mob && mob.getTarget() == player) || entity.getLastHurtByMob() == player;
                if (!isDormant && (entity.distanceToSqr(player) <= 14400.0D || engaged)) {
                    bossEvent.addPlayer(player);
                } else {
                    bossEvent.removePlayer(player);
                }
            }
        }
    }

    private static void removeBossBar(LivingEntity entity) {
        if (entity == null) return;
        ServerBossEvent bossEvent = BOSS_BARS.remove(entity.getUUID());
        if (bossEvent != null) {
            bossEvent.removeAllPlayers();
        }
    }

    // ---------- KNOCKBACK & POTION IMMUNITY
    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (isMonarch(event.getEntity()) || isVanguard(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (isMonarch(event.getEntity()) || isVanguard(event.getEntity())) {
            if (!event.getEffectInstance().getEffect().isBeneficial()) {
                event.setResult(Event.Result.DENY);
            }
        }
    }
}
