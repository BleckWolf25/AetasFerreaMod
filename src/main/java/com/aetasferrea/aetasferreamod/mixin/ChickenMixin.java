/**
 * @file ChickenMixin.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Mixin for chickens to manage flight drag and wing fatigue.
 *
 * @description
 * Redirects the drag velocity calculation in the Chicken entity's step tick to negate
 * slow-falling drag when a chicken has fallen past a threshold height, representing fatigue.
 *
 * @since 25/06/2026
 * @updated 26/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.mixin;

// ---------- IMPORTS
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// ---------- CLASS: CHICKENMIXIN
@Mixin(Chicken.class)
public class ChickenMixin {

    // ---------- FLIGHT DRAG REDIRECT
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 redirectDrag(Vec3 instance, double x, double y, double z) {
        Chicken chicken = (Chicken) (Object) this;
        // Return original velocity unaffected by drag due to wing fatigue
        if (chicken.fallDistance > 8.0f) {
            return instance;
        }

        // Apply normal slow falling drag multipliers
        return instance.multiply(x, y, z);
    }

}
