/**
 * @file WolfEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Subscriber for custom wolf behaviors and pack mechanics.
 *
 * @description
 * Manages customized wolf behaviors, including AI goal injection, friendly fire protection,
 * pack defense/retreat mechanics, dynamic health adjustments, pack size limits, and threat targeting.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity.ai;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.entity.ai.goal.StrictFollowOwnerGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("null")
// ---------- CLASS: WOLF EVENT HANDLER
@Mod.EventBusSubscriber(modid = "aetasferreamod")
public class WolfEventHandler {

    // ---------- CONSTANTS & CONFIGURATION
    private static final double PACK_ALERT_RADIUS = 16.0;
    private static final double FLEE_HEALTH_RATIO = 0.3; // Retreat when health drops below 30%

    // ---------- ENTITY JOIN EVENT HANDLER
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof Wolf wolf) {
            // Lazy initialization tag setup
            if (!wolf.getPersistentData().getBoolean("aetas_initialized")) {
                wolf.getPersistentData().putBoolean("aetas_initialized", true);

                // 25% chance for a wild wolf to spawn as a predator hostile to players
                if (!wolf.isTame() && wolf.getRandom().nextFloat() < 0.25f) {
                    wolf.getPersistentData().putBoolean("aggressive_predator", true);
                }
            }

            // Replace standard goals (remove vanilla follow owner and leap target)
            List<WrappedGoal> toRemove = new ArrayList<>();
            for (WrappedGoal priorityGoal : wolf.goalSelector.getAvailableGoals()) {
                if (priorityGoal.getGoal() instanceof FollowOwnerGoal || priorityGoal.getGoal() instanceof LeapAtTargetGoal) {
                    toRemove.add(priorityGoal);
                }
            }
            toRemove.forEach(g -> wolf.goalSelector.removeGoal(g.getGoal()));

            // Register custom strict follow goal
            wolf.goalSelector.addGoal(2, new StrictFollowOwnerGoal(wolf));
        }
    }

    // ---------- LIVING DAMAGE EVENT HANDLER
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide) return;

        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity == null) return;
        LivingEntity attacker = sourceEntity instanceof LivingEntity ? (LivingEntity) sourceEntity : null;
        if (attacker == null) return;

        // CASE A: The Wolf is the Target
        if (victim instanceof Wolf wolfTarget) {
            
            // Friendly Fire Protection: Owner player attacking their own tamed wolf (unless crouching)
            if (wolfTarget.isTame() && attacker instanceof Player playerAttacker && !playerAttacker.isCrouching()) {
                UUID ownerUUID = wolfTarget.getOwnerUUID();
                if (ownerUUID != null && ownerUUID.equals(playerAttacker.getUUID())) {
                    event.setCanceled(true);
                    return;
                }
            }

            // Friendly Fire Protection: Tamed wolves owned by the same player attacking each other
            if (wolfTarget.isTame() && attacker instanceof Wolf wolfAttacker && wolfAttacker.isTame()) {
                UUID ownerA = wolfTarget.getOwnerUUID();
                UUID ownerB = wolfAttacker.getOwnerUUID();
                if (ownerA != null && ownerA.equals(ownerB)) {
                    event.setCanceled(true);
                    return;
                }
            }

            // Pack Rally Logic: Alert nearby wolves to attack the aggressor
            AABB alertBox = wolfTarget.getBoundingBox().inflate(PACK_ALERT_RADIUS);
            List<Wolf> nearbyWolves = level.getEntitiesOfClass(Wolf.class, alertBox, w -> 
                !w.getUUID().equals(wolfTarget.getUUID()) && 
                w.getTarget() == null && 
                (w.getHealth() / w.getMaxHealth()) >= FLEE_HEALTH_RATIO
            );

            for (Wolf packMember : nearbyWolves) {
                boolean bothWild = !packMember.isTame() && !wolfTarget.isTame();
                boolean samePack = false;

                if (packMember.isTame() && wolfTarget.isTame()) {
                    UUID oA = packMember.getOwnerUUID();
                    UUID oB = wolfTarget.getOwnerUUID();
                    if (oA != null && oA.equals(oB)) {
                        samePack = true;
                    }
                }

                if ((bothWild || samePack) && !packMember.isOrderedToSit()) {
                    // Do not rally neutral wild wolves to defend a man-eating predator
                    if (bothWild && wolfTarget.getPersistentData().getBoolean("aggressive_predator") && attacker instanceof Player) {
                        continue;
                    }
                    packMember.setTarget(attacker);
                }
            }

            // Tactical Retreat: Run away and gain Speed II if health falls below 30%
            if ((wolfTarget.getHealth() / wolfTarget.getMaxHealth()) < FLEE_HEALTH_RATIO) {
                wolfTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                wolfTarget.setTarget(null);
                if (wolfTarget.isOrderedToSit()) {
                    wolfTarget.setOrderedToSit(false);
                }

                if (wolfTarget.isTame()) {
                    LivingEntity owner = wolfTarget.getOwner();
                    if (owner != null && attacker instanceof net.minecraft.world.entity.Mob mobAttacker) {
                        mobAttacker.setTarget(owner);
                    }
                } else if (attacker instanceof net.minecraft.world.entity.Mob mobAttacker && mobAttacker.getTarget() == wolfTarget) {
                    mobAttacker.setTarget(null);
                }

                double dx = wolfTarget.getX() - attacker.getX();
                double dz = wolfTarget.getZ() - attacker.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0.1) {
                    wolfTarget.getNavigation().moveTo(wolfTarget.getX() + (dx / dist) * 24.0, wolfTarget.getY(), wolfTarget.getZ() + (dz / dist) * 24.0, 1.6D);
                }
            }
        }

        // CASE B: The Wolf is the Attacker
        if (attacker instanceof Wolf wolfAttacker) {

            // Creative / Spectator immunity
            if (victim instanceof Player playerVictim && (playerVictim.isCreative() || playerVictim.isSpectator())) {
                return;
            }

            // Slow down the wolf briefly after landing an attack
            wolfAttacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));

            // Apply Stun effect on alternate attacks (stun cooldown toggle)
            boolean canStun = true;
            if (wolfAttacker.getPersistentData().contains("can_stun")) {
                canStun = wolfAttacker.getPersistentData().getBoolean("can_stun");
            }

            if (canStun) {
                MobEffect stunEffect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("shield_overhaul", "stun"));
                if (stunEffect != null) {
                    victim.addEffect(new MobEffectInstance(stunEffect, 20, 0));
                }
                wolfAttacker.getPersistentData().putBoolean("can_stun", false);
            } else {
                wolfAttacker.getPersistentData().putBoolean("can_stun", true);
            }

            // Tamed Wolf: Prevent attacking passive animals unless the owner has targeted/attacked them first
            if (wolfAttacker.isTame() && victim instanceof Animal animalVictim && !animalVictim.getPersistentData().getBoolean("attacked_by_owner")) {
                wolfAttacker.setTarget(null);
                event.setCanceled(true);
            }
        }

        // CASE C: OWNER PROTECTION COMMANDS
        // If owner is attacked, make tamed wolves target the attacker
        if (victim instanceof Player playerVictim && !(attacker instanceof Wolf)) {
            AABB alertBox = playerVictim.getBoundingBox().inflate(PACK_ALERT_RADIUS);
            List<Wolf> nearbyTamed = level.getEntitiesOfClass(Wolf.class, alertBox, w -> 
                w.isTame() && 
                playerVictim.getUUID().equals(w.getOwnerUUID()) &&
                !w.isOrderedToSit() && w.getTarget() == null && 
                (w.getHealth() / w.getMaxHealth()) >= FLEE_HEALTH_RATIO
            );
            nearbyTamed.forEach(w -> w.setTarget(attacker));
        }

        // If owner attacks a passive entity, command the wolves to help
        if (victim instanceof Animal animalVictim && attacker instanceof Player playerAttacker) {
            animalVictim.getPersistentData().putBoolean("attacked_by_owner", true);
            AABB alertBox = animalVictim.getBoundingBox().inflate(PACK_ALERT_RADIUS);
            List<Wolf> nearbyTamed = level.getEntitiesOfClass(Wolf.class, alertBox, w -> 
                w.isTame() && 
                playerAttacker.getUUID().equals(w.getOwnerUUID())
            );
            nearbyTamed.forEach(w -> w.setTarget(animalVictim));
        }
    }

    // ---------- LIVING DEATH EVENT HANDLER
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide) return;

        Entity sourceEntity = event.getSource().getEntity();
        // Give wolves Regeneration I and show heart particles when they kill passive animals
        if (sourceEntity instanceof Wolf killer && victim instanceof Animal) {
            killer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, victim.getX(), victim.getY() + 0.5, victim.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    // ---------- PLAYER TICK EVENT HANDLER
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        Level level = player.level();

        // Perform checks once per second (every 20 ticks) for efficiency
        if (player.tickCount % 20 != 0 || !player.isAlive()) return;

        AABB sweepBox = player.getBoundingBox().inflate(64.0);
        List<Wolf> allWolves = level.getEntitiesOfClass(Wolf.class, sweepBox);

        List<Wolf> ownedWolves = new ArrayList<>();
        List<Wolf> hounds = new ArrayList<>();

        for (Wolf wolf : allWolves) {
            // Lazy init setup if not initialized
            if (!wolf.getPersistentData().getBoolean("aetas_initialized")) {
                wolf.getPersistentData().putBoolean("aetas_initialized", true);
                if (!wolf.isTame() && level.random.nextFloat() < 0.25f) {
                    wolf.getPersistentData().putBoolean("aggressive_predator", true);
                }
            }

            boolean isTame = wolf.isTame();

            // Dynamic Max Health Enforcer (Tamed: 20 max health, Wild: 14 max health)
            double expectedMaxHealth = isTame ? 20.0 : 14.0;
            AttributeInstance healthAttr = wolf.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttr != null && healthAttr.getBaseValue() != expectedMaxHealth) {
                healthAttr.setBaseValue(expectedMaxHealth);
                if (wolf.getHealth() > expectedMaxHealth) {
                    wolf.setHealth((float) expectedMaxHealth);
                }
            }

            double dSq = wolf.distanceToSqr(player);
            UUID ownerUUID = wolf.getOwnerUUID();
            boolean ownsThis = isTame && ownerUUID != null && ownerUUID.equals(player.getUUID());

            if (ownsThis) {
                ownedWolves.add(wolf);
                // Keep track of healthy, standing owned wolves within 24 blocks
                if (!wolf.isOrderedToSit() && (wolf.getHealth() / wolf.getMaxHealth()) >= FLEE_HEALTH_RATIO && dSq <= 576.0) {
                    hounds.add(wolf);
                }
            } else if (!isTame && wolf.getPersistentData().getBoolean("aggressive_predator") && wolf.getTarget() == null && dSq <= 256.0) {
                // Wild predator aggressive targeting
                if (!player.isCreative() && !player.isSpectator()) {
                    wolf.setTarget(player);
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.wolf.spots_you").withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            }
        }

        // Pack Limit Enforcer: Limit owned wolves to 2. Excess wolves are untamed and turned hostile.
        if (ownedWolves.size() > 2) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.wolf.pack_rejects").withStyle(net.minecraft.ChatFormatting.RED), true);

            for (int i = 2; i < ownedWolves.size(); i++) {
                Wolf extraWolf = ownedWolves.get(i);
                extraWolf.setTame(false);
                extraWolf.setOwnerUUID(null);
                if (extraWolf.isOrderedToSit()) {
                    extraWolf.setOrderedToSit(false);
                }

                extraWolf.getPersistentData().putBoolean("aggressive_predator", true);
                if (!player.isCreative() && !player.isSpectator()) {
                    extraWolf.setTarget(player);
                }
            }
        }

        // Hound threat targeting: Tamed standing wolves auto-target nearby hostile threats (excluding Creepers)
        if (!hounds.isEmpty()) {
            AABB threatBox = player.getBoundingBox().inflate(10.0);
            List<Monster> threats = level.getEntitiesOfClass(Monster.class, threatBox, m -> 
                m.isAlive() && !(m instanceof Creeper)
            );

            if (!threats.isEmpty()) {
                threats.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
                Monster primaryThreat = threats.get(0);

                for (Wolf wolf : hounds) {
                    LivingEntity currentTarget = wolf.getTarget();

                    // Clear target if dead or too far
                    if (currentTarget != null) {
                        if (!currentTarget.isAlive() || wolf.distanceToSqr(currentTarget) > 1024.0 || player.distanceToSqr(currentTarget) > 1024.0) {
                            wolf.setTarget(null);
                            currentTarget = null;
                        }
                    }

                    // Pivot target if a closer threat exists
                    if (currentTarget == null || wolf.distanceToSqr(primaryThreat) < wolf.distanceToSqr(currentTarget) * 0.5) {
                        wolf.setTarget(primaryThreat);
                    }
                }
            }
        }
    }
}
