/**
 * @file LivingEntityMixin.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Mixin for LivingEntity to allow the Hollow Monarch to walk on lava.
 *
 * @description
 * Injects into canStandOnFluid to allow entities tagged as the Hollow Monarch
 * to stand on lava as if it were a solid block surface.
 *
 * @since 01/07/2026
 * @updated 01/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.mixin;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.entity.boss.BossCombatHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;

import java.util.Objects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// ---------- CLASS: LIVING ENTITY MIXIN
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    // Lava walking for the Hollow Monarch
    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void onCanStandOnFluid(FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if (((Object) this) instanceof LivingEntity living && BossCombatHandler.isMonarch(living)) {
            if (fluidState.is(Objects.requireNonNull(FluidTags.LAVA))) {
                cir.setReturnValue(true);
            }
        }
    }
}
