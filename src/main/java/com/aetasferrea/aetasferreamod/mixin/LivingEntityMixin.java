/**
 * @file LivingEntityMixin.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Mixin for living entities to override default fall damage mechanics.
 *
 * @description
 * Modifies the fall damage calculation for LivingEntity instances, specifically bypassing
 * immunity to enforce custom calculated fall damage on chickens when they fall from high heights.
 *
 * @since 25/06/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.mixin;

// ---------- IMPORTS
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Objects;

// ---------- CLASS: LIVINGENTITYMIXIN
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    // ---------- FALL DAMAGE INJECTION
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void enforceChickenFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // Proceed only if the entity is a chicken
        if (!(entity instanceof Chicken)) {
            return;
        }

        // Bypass the fall_damage_immune tag completely for chickens
        if (distance <= 8.0f) {
            cir.setReturnValue(false);
            return;
        }

        // ---------- FALL DAMAGE CALCULATION (Apply fatigue-modified fall damage)
        float effectiveDistance = distance * 0.5f;
        int damage = net.minecraft.util.Mth.ceil((effectiveDistance - 3.0F) * multiplier);
        if (damage > 0) {
            entity.hurt(Objects.requireNonNull(source), (float) damage);
            cir.setReturnValue(true);
        }
    }
}
