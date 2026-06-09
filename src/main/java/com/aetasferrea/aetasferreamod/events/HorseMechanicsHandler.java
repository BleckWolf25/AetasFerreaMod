package com.aetasferrea.aetasferreamod.events;

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
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.aetasferrea.aetasferreamod.AetasFerreaMod.MODID)
public class HorseMechanicsHandler {

    private static void displaySubtitle(Player player, Component text) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 80, 20));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(text));
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.empty()));
        }
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof Player player && event.getEntityBeingMounted() instanceof HorseEventHandler horse) {
            if (!horse.isTamed()) {
                event.setCanceled(true);
                if (!horse.level().isClientSide) {
                    player.stopRiding();
                    player.hurt(player.damageSources().generic(), 2.0f);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    double dx = player.getX() - horse.getX();
                    double dz = player.getZ() - horse.getZ();
                    player.knockback(1.0, -dx, -dz);
                    horse.makeMad();
                    horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    player.displayClientMessage(Component.literal("The wild horse kicks you away! It must be broken first.").withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        HorseEventHandler horse = null;
        if (event.getEntity() instanceof HorseEventHandler h) horse = h;
        else if (event.getEntity() instanceof Player p && p.getVehicle() instanceof HorseEventHandler h) horse = h;

        if (horse != null) {
            int horseClass = horse.getHorseClass();
            if (horseClass == HorseEventHandler.CLASS_ROUNCEY) {
                int combatXP = horse.getCombatXP();
                if (combatXP < 125 && combatXP != -1) {
                    long lastDamageXP = horse.getPersistentData().getLong("AetasDamageXPCooldown");
                    long gameTime = horse.tickCount;
                    if (gameTime - lastDamageXP >= 100L) {
                        horse.getPersistentData().putLong("AetasDamageXPCooldown", gameTime);
                        int previousXP = combatXP;
                        combatXP = Math.min(125, combatXP + 10);
                        horse.setCombatXP(combatXP);
                        if (previousXP < 65 && combatXP >= 65 && horse.getFirstPassenger() instanceof Player rider) {
                            horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
                            rider.displayClientMessage(Component.literal("Your mount is growing more accustomed to the rigors of battle...").withStyle(ChatFormatting.GRAY), true);
                        }
                        if (previousXP < 125 && combatXP >= 125) {
                            horse.level().playSound(null, horse.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                            if (horse.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                            if (horse.getFirstPassenger() instanceof Player rider) {
                                displaySubtitle(rider, Component.literal("Your mount is now ready to become a Destrier!").withStyle(ChatFormatting.GOLD));
                            }
                        }
                    }
                }
            }

            if (horseClass != HorseEventHandler.CLASS_DESTRIER && horse.isVehicle() && horse.level() instanceof ServerLevel sl) {
                if (horse.getRandom().nextFloat() < 0.30f) {
                    horse.ejectPassengers();
                    horse.makeMad();
                    horse.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 2));
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                    horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.getVehicle() instanceof HorseEventHandler horse && !horse.level().isClientSide) {
                if (event.getEntity() == horse) return;
                if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) return;

                double throttle = horse.getThrottle();

                if (horse.getHorseClass() == HorseEventHandler.CLASS_ROUNCEY) {
                    int combatXP = horse.getCombatXP();
                    if (combatXP < 125 && combatXP != -1) {
                        long lastCombatXP = horse.getPersistentData().getLong("AetasCombatXPCooldown");
                        long gameTime = horse.tickCount;
                        if (gameTime - lastCombatXP >= 60L) {
                            horse.getPersistentData().putLong("AetasCombatXPCooldown", gameTime);
                            int previousXP = combatXP;
                            combatXP = Math.min(125, combatXP + 5);
                            horse.setCombatXP(combatXP);
                            if (previousXP < 65 && combatXP >= 65) {
                                horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
                                player.displayClientMessage(Component.literal("Your mount is growing more accustomed to the rigors of battle...").withStyle(ChatFormatting.GRAY), true);
                            }
                            if (previousXP < 125 && combatXP >= 125) {
                                horse.level().playSound(null, horse.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                                if (horse.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                                displaySubtitle(player, Component.literal("Your mount is now ready to become a Destrier!").withStyle(ChatFormatting.GOLD));
                            }
                        }
                    }
                }

                if (throttle >= 0.5) {
                    float momentumBonus = (float) ((throttle - 0.5) * 8.0);
                    event.setAmount(event.getAmount() + momentumBonus);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof HorseEventHandler horse && !horse.level().isClientSide) {
            if (horse.getHorseClass() == HorseEventHandler.CLASS_ROUNCEY) {
                int agilityXP = horse.getAgilityXP();
                if (agilityXP < 150 && agilityXP != -1) {
                    int previousXP = agilityXP;
                    agilityXP = Math.min(150, agilityXP + 5);
                    horse.setAgilityXP(agilityXP);
                    if (previousXP < 75 && agilityXP >= 75 && horse.getFirstPassenger() instanceof Player rider) {
                        horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
                        rider.displayClientMessage(Component.literal("Your mount is growing more accustomed to nimble footwork...").withStyle(ChatFormatting.GRAY), true);
                    }
                    if (previousXP < 150 && agilityXP >= 150) {
                        horse.level().playSound(null, horse.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                        if (horse.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                        if (horse.getFirstPassenger() instanceof Player rider) {
                            displaySubtitle(rider, Component.literal("Your mount is now ready to become a Courser!").withStyle(ChatFormatting.AQUA));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            if (event.player.getVehicle() instanceof HorseEventHandler horse) {
                double throttle = horse.getThrottle();
                if (horse.getHorseClass() == HorseEventHandler.CLASS_ROUNCEY && throttle > 0.6) {
                    if (event.player.tickCount % 20 == 0) {
                        int agilityXP = horse.getAgilityXP();
                        if (agilityXP < 150 && agilityXP != -1) {
                            int previousXP = agilityXP;
                            agilityXP = Math.min(150, agilityXP + 2);
                            horse.setAgilityXP(agilityXP);
                            if (previousXP < 75 && agilityXP >= 75) {
                                horse.level().playSound(null, horse.blockPosition(), SoundEvents.HORSE_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.0f);
                                event.player.displayClientMessage(Component.literal("Your mount is growing more accustomed to nimble footwork...").withStyle(ChatFormatting.GRAY), true);
                            }
                            if (previousXP < 150 && agilityXP >= 150) {
                                horse.level().playSound(null, horse.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                                if (horse.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, horse.getX(), horse.getY() + 1.5, horse.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                                displaySubtitle(event.player, Component.literal("Your mount is now ready to become a Courser!").withStyle(ChatFormatting.AQUA));
                            }
                        }
                    }
                }

                if (horse.getHorseClass() == HorseEventHandler.CLASS_ROUNCEY) {
                    int combatXP = horse.getCombatXP();
                    int agilityXP = horse.getAgilityXP();
                    if ((combatXP >= 125 && combatXP != -1) || (agilityXP >= 150 && agilityXP != -1)) {
                        // Repeating every 60 ticks ensures seamless action bar duration without blinking/fading
                        if (event.player.tickCount % 60 == 0) {
                            if (combatXP >= 125 && combatXP != -1 && agilityXP >= 150 && agilityXP != -1) {
                                event.player.displayClientMessage(Component.literal("Ready for Specialization: Bring Iron Ingot (Destrier) or Leather & Feather (Courser).").withStyle(ChatFormatting.GREEN), true);
                            } else if (combatXP >= 125 && combatXP != -1) {
                                event.player.displayClientMessage(Component.literal("Ready for Specialization: Bring an Iron Ingot to upgrade to a Destrier.").withStyle(ChatFormatting.GREEN), true);
                            } else if (agilityXP >= 150 && agilityXP != -1) {
                                event.player.displayClientMessage(Component.literal("Ready for Specialization: Bring Leather & Feathers to upgrade to a Courser.").withStyle(ChatFormatting.GREEN), true);
                            }
                        }
                    }
                }
            }
        }
    }
}