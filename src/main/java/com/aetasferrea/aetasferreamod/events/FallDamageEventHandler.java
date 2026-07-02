/**
 * @file FallDamageEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Intercepts and overrides vanilla fall damage mechanics for specific entities.
 *
 * @description
 * Hooks into the Forge LivingFallEvent to dynamically recalculate fall damage for
 * creatures that normally have immunity (e.g. chickens). This introduces a fatigue
 * mechanic where falling from extreme heights overpowers their natural deceleration
 * and applies calculated physical damage.
 *
 * @since 25/06/2026
 * @updated 26/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import net.minecraft.world.entity.animal.Chicken;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ---------- CLASS: FallDamageEventHandler
@Mod.EventBusSubscriber(modid = "aetasferreamod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FallDamageEventHandler {

    // ---------- EVENT HANDLER: CHICKEN FALL DAMAGE
    @SuppressWarnings("null")
    @SubscribeEvent
    public static void onChickenFall(LivingFallEvent event) {

        // Ignore entities that are not chickens to preserve vanilla behavior for others
        if (!(event.getEntity() instanceof Chicken chicken)) {
            return;
        }

        float distance = event.getDistance();
        float multiplier = event.getDamageMultiplier();

        // Cancel to bypass vanilla logic and maintain natural immunity for safe heights
        if (distance <= 8.0f) {
            event.setCanceled(true);
            return;
        }

        // Apply fatigue-modified damage because chickens hitting the ground too fast should hurt
        float effectiveDistance = distance * 0.5f;
        int damage = net.minecraft.util.Mth.ceil((effectiveDistance - 3.0F) * multiplier);

        // Execute damage directly. We use generic() because chickens are explicitly tagged as
        // FALL_DAMAGE_IMMUNE, which makes hurt(fall) abort completely.
        if (damage > 0) {
            chicken.hurt(chicken.damageSources().generic(), (float) damage);
        }

        // Suppress vanilla fall event propagation completely to finalize our custom override
        event.setCanceled(true);
    }
}
