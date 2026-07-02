/**
 * @file NetherTargetingHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Targeting immunity for the Hollow Monarch and its Aetas Vanguards.
 *
 * @description
 * Prevents Nether-native hostile mobs (Piglins, Piglin Brutes, Hoglins, Zoglins,
 * Blazes, Wither Skeletons, Magma Cubes and Ghasts) from selecting the Hollow
 * Monarch or any Aetas Vanguard as an attack target, since they are meant to be
 * allied set-pieces rather than additional threats the player must fend off.
 * Also prevents the Monarch and its Vanguards from targeting one another, so
 * the boss encounter never turns into infighting.
 *
 * @since 30/06/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ---------- CLASS: NETHER TARGETING HANDLER
@Mod.EventBusSubscriber(modid = "aetasferreamod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NetherTargetingHandler {

    private static final String MONARCH_TAG = "Aetas_IsMonarch";
    private static final String VANGUARD_TAG = "Aetas_IsVanguard";

    // Targeting - Cancel any attempt by a Nether mob (or the boss/guardians themselves) to target the Monarch or its Vanguards
    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity attacker = event.getEntity();
        LivingEntity newTarget = event.getNewTarget();

        if (attacker == null || newTarget == null || attacker.level().isClientSide) {
            return;
        }

        boolean targetIsMonarch = newTarget.getPersistentData().getBoolean(MONARCH_TAG);
        boolean targetIsVanguard = newTarget.getPersistentData().getBoolean(VANGUARD_TAG);

        if (!targetIsMonarch && !targetIsVanguard) {
            return;
        }

        // Block all Nether-native hostile mobs from targeting the Monarch or any Vanguard
        if (isNetherMob(attacker)) {
            event.setCanceled(true);
            return;
        }

        // Block the Monarch from targeting a Vanguard, and a Vanguard from targeting the Monarch
        boolean attackerIsMonarch = attacker.getPersistentData().getBoolean(MONARCH_TAG);
        boolean attackerIsVanguard = attacker.getPersistentData().getBoolean(VANGUARD_TAG);

        if ((attackerIsMonarch && targetIsVanguard) || (attackerIsVanguard && targetIsMonarch)) {
            event.setCanceled(true);
        }
    }

    /**
     * Determines whether the given entity is one of the Nether-native hostile mobs
     * that should be prevented from targeting the Monarch or its Vanguards.
     *
     * @param entity The entity to check.
     * @return true if the entity is a recognized Nether-native hostile mob.
     */
    private static boolean isNetherMob(LivingEntity entity) {
        return entity instanceof AbstractPiglin
                || entity instanceof Hoglin
                || entity instanceof Blaze
                || entity instanceof WitherSkeleton
                || entity instanceof MagmaCube
                || entity instanceof Ghast;
    }
}
