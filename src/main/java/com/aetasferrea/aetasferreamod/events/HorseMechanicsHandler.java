/**
 * @file HorseMechanicsHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Forge event handler for custom equine mount interactions, XP tracking, and agility training.
 *
 * @description
 * Handles mount refusal for untamed equines, rider ejection on damage, combat and agility XP accumulation
 * for Rouncey-class horses, throttle-driven agility ticks, and displays subtitle notifications
 * when specialization thresholds are reached.
 *
 * @since 20/05/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import com.aetasferrea.aetasferreamod.entity.AetasDonkey;
import com.aetasferrea.aetasferreamod.entity.HorseEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// ---------- CLASS: HorseMechanicsHandler
@SuppressWarnings({"null", "DataFlowIssue"})
public class HorseMechanicsHandler {

    // ---------- UTILITY METHODS
    /**
     * Sends a full-screen subtitle packet to a server player.
     *
     * @param player the target server player
     * @param text   the subtitle component to display
     */
    private static void displaySubtitle(Player player, Component text) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 80, 20));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(text));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.empty()));
        }
    }

    // ---------- ENTITY MOUNT EVENTS
    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (!event.isMounting() || !(event.getEntityMounting() instanceof Player player)) return;

        net.minecraft.world.entity.Entity mount = event.getEntityBeingMounted();

        // ---------- UNTAMED HORSE KICK ----------
        if (mount instanceof HorseEventHandler horse) {
            if (!horse.isTamed()) {
                event.setCanceled(true);
                if (!horse.level().isClientSide) {
                    player.stopRiding();
                    player.hurt(player.damageSources().generic(), 2.0f);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    // Knock the player away from the horse
                    double dx = player.getX() - horse.getX();
                    double dz = player.getZ() - horse.getZ();
                    player.knockback(1.0, -dx, -dz);
                    horse.makeMad();
                    horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.wild_kicks", Component.translatable("entity.aetasferreamod.aetas_horse")).withStyle(ChatFormatting.RED), true);
                }
            }
        }

        // ---------- UNTAMED DONKEY KICK ----------
        else if (mount instanceof AetasDonkey donkey) {
            if (!donkey.isTamed()) {
                event.setCanceled(true);
                if (!donkey.level().isClientSide) {
                    player.stopRiding();
                    player.hurt(player.damageSources().generic(), 2.0f);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    double dx = player.getX() - donkey.getX();
                    double dz = player.getZ() - donkey.getZ();
                    player.knockback(1.0, -dx, -dz);
                    donkey.makeMad();
                    donkey.level().playSound(null, donkey.blockPosition(), SoundEvents.DONKEY_ANGRY, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.wild_kicks", Component.translatable("entity.aetasferreamod.aetas_donkey")).withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    // ---------- DAMAGE EVENTS
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        // Check if the damaged entity is a horse, or a player riding one
        HorseEventHandler horse = null;
        if (event.getEntity() instanceof HorseEventHandler h) horse = h;
        else if (event.getEntity() instanceof Player p && p.getVehicle() instanceof HorseEventHandler h) horse = h;

        if (horse != null) {
            int horseClass = horse.getHorseClass();

            // ---------- ROUNCEY COMBAT XP (on damage received) ----------
            if (horseClass == HorseEventHandler.CLASS_ROUNCEY) {
                int combatXP = horse.getCombatXP();
                int combatCap = AetasFerreaConfig.ROUNCEY_COMBAT_XP_CAP.get();
                if (combatXP < combatCap && combatXP != -1) {
                    long lastDamageXP = horse.getPersistentData().getLong("AetasDamageXPCooldown");
                    long gameTime = horse.tickCount;
                    // 100-tick cooldown prevents XP spam from multi-hit attacks
                    if (gameTime - lastDamageXP >= 100L) {
                        horse.getPersistentData().putLong("AetasDamageXPCooldown", gameTime);
                        int previousXP = combatXP;
                        combatXP = Math.min(combatCap, combatXP + AetasFerreaConfig.ROUNCEY_COMBAT_HIT_XP.get());
                        horse.setCombatXP(combatXP);

                        if (previousXP < combatCap / 2 && combatXP >= combatCap / 2 && horse.getFirstPassenger() instanceof Player rider) {
                            horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
                            rider.displayClientMessage(Component.translatable("message.aetasferreamod.horse.accustomed_battle").withStyle(ChatFormatting.GRAY), true);
                        }
                        if (previousXP < combatCap && combatXP >= combatCap) {
                            horse.level().playSound(null, horse.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                            if (horse.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                            if (horse.getFirstPassenger() instanceof Player rider) {
                                displaySubtitle(rider, Component.translatable("message.aetasferreamod.horse.ready_destrier").withStyle(ChatFormatting.GOLD));
                            }
                        }
                    }
                }
            }

            // ---------- RIDER EJECTION (Non-Destrier only) ----------
            if (horseClass != HorseEventHandler.CLASS_DESTRIER && horse.isVehicle() && horse.level() instanceof ServerLevel sl) {
                // 30% chance to panic and eject rider when hit
                if (horse.getRandom().nextFloat() < AetasFerreaConfig.HORSE_PANIC_EJECT_CHANCE.get()) {
                    horse.ejectPassengers();
                    horse.makeMad();
                    horse.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 2));
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                    horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
            }
        }
    }

    // ---------- PLAYER ATTACK EVENTS
    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(player.getVehicle() instanceof HorseEventHandler horse)) return;
        if (horse.level().isClientSide) return;
        if (event.getEntity() == horse) return;
        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) return;

        // ---------- ROUNCEY COMBAT XP (on attack dealt) ----------
        if (horse.getHorseClass() == HorseEventHandler.CLASS_ROUNCEY) {
            int combatXP = horse.getCombatXP();
            int combatCap = AetasFerreaConfig.ROUNCEY_COMBAT_XP_CAP.get();
            if (combatXP < combatCap && combatXP != -1) {
                long lastCombatXP = horse.getPersistentData().getLong("AetasCombatXPCooldown");
                long gameTime = horse.tickCount;
                // 60-tick cooldown to prevent rapid XP inflation from quick attacks
                if (gameTime - lastCombatXP >= 60L) {
                    horse.getPersistentData().putLong("AetasCombatXPCooldown", gameTime);
                    int previousXP = combatXP;
                    combatXP = Math.min(combatCap, combatXP + AetasFerreaConfig.ROUNCEY_COMBAT_ATTACK_XP.get());
                    horse.setCombatXP(combatXP);

                    if (previousXP < combatCap / 2 && combatXP >= combatCap / 2) {
                        horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
                        player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.accustomed_battle").withStyle(ChatFormatting.GRAY), true);
                    }
                    if (previousXP < combatCap && combatXP >= combatCap) {
                        horse.level().playSound(null, horse.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                        if (horse.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                        displaySubtitle(player, Component.translatable("message.aetasferreamod.horse.ready_destrier").withStyle(ChatFormatting.GOLD));
                    }
                }
            }
        }
    }

    // ---------- LIVING JUMP EVENTS
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof HorseEventHandler horse) || horse.level().isClientSide) return;

        // ---------- ROUNCEY AGILITY XP (on jump) ----------
        if (horse.getHorseClass() == HorseEventHandler.CLASS_ROUNCEY) {
            int agilityXP = horse.getAgilityXP();
            int agilityCap = AetasFerreaConfig.ROUNCEY_AGILITY_XP_CAP.get();
            if (agilityXP < agilityCap && agilityXP != -1) {
                int previousXP = agilityXP;
                agilityXP = Math.min(agilityCap, agilityXP + AetasFerreaConfig.ROUNCEY_JUMP_XP.get());
                horse.setAgilityXP(agilityXP);

                if (previousXP < agilityCap / 2 && agilityXP >= agilityCap / 2 && horse.getFirstPassenger() instanceof Player rider) {
                    horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
                    rider.displayClientMessage(Component.translatable("message.aetasferreamod.horse.accustomed_footwork").withStyle(ChatFormatting.GRAY), true);
                }
                if (previousXP < agilityCap && agilityXP >= agilityCap) {
                    horse.level().playSound(null, horse.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    if (horse.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                    if (horse.getFirstPassenger() instanceof Player rider) {
                        displaySubtitle(rider, Component.translatable("message.aetasferreamod.horse.ready_courser").withStyle(ChatFormatting.AQUA));
                    }
                }
            }
        }
    }

    // ---------- PLAYER TICK EVENTS
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player.getVehicle() instanceof HorseEventHandler horse)) return;

        // ---------- ROUNCEY AGILITY XP (on sustained high-throttle gallop) ----------
        double throttle = horse.getThrottle();
        if (horse.getHorseClass() == HorseEventHandler.CLASS_ROUNCEY && throttle > 0.6 && !horse.isHorseSwimming()) {
            if (event.player.tickCount % 20 == 0) {
                int agilityXP = horse.getAgilityXP();
                int agilityCap = AetasFerreaConfig.ROUNCEY_AGILITY_XP_CAP.get();
                if (agilityXP < agilityCap && agilityXP != -1) {
                    int previousXP = agilityXP;
                    agilityXP = Math.min(agilityCap, agilityXP + AetasFerreaConfig.ROUNCEY_GALLOP_XP.get());
                    horse.setAgilityXP(agilityXP);

                    if (previousXP < agilityCap / 2 && agilityXP >= agilityCap / 2) {
                        horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
                        event.player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.accustomed_footwork").withStyle(ChatFormatting.GRAY), true);
                    }
                    if (previousXP < agilityCap && agilityXP >= agilityCap) {
                        horse.level().playSound(null, horse.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                        if (horse.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                        displaySubtitle(event.player, Component.translatable("message.aetasferreamod.horse.ready_courser").withStyle(ChatFormatting.AQUA));
                    }
                }
            }
        }

        // ---------- SPECIALIZATION READINESS REMINDER (every 60 ticks) ----------
        if (horse.getHorseClass() == HorseEventHandler.CLASS_ROUNCEY) {
            int combatXP = horse.getCombatXP();
            int agilityXP = horse.getAgilityXP();
            int customClass = horse.getHorseClass();
            int combatCap = AetasFerreaConfig.ROUNCEY_COMBAT_XP_CAP.get();
            int agilityCap = AetasFerreaConfig.ROUNCEY_AGILITY_XP_CAP.get();
            if ((combatXP >= combatCap && combatXP != -1) || (agilityXP >= agilityCap && agilityXP != -1)) {
                if (event.player.tickCount % 60 == 0) {
                    if (combatXP >= combatCap && combatXP != -1 && agilityXP >= agilityCap && agilityXP != -1) {
                        event.player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.ready_spec").withStyle(ChatFormatting.GREEN), true);
                    } else if (customClass == HorseEventHandler.CLASS_ROUNCEY && combatXP >= combatCap) {
                        event.player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.ready_spec_destrier").withStyle(ChatFormatting.GREEN), true);
                    } else if (customClass == HorseEventHandler.CLASS_ROUNCEY && agilityXP >= agilityCap) {
                        event.player.displayClientMessage(Component.translatable("message.aetasferreamod.horse.ready_spec_courser").withStyle(ChatFormatting.GREEN), true);
                    }
                }
            }
        }
    }
}
