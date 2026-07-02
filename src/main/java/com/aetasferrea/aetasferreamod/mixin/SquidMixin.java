/**
 * @file SquidMixin.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Mixin for squid entities to prevent beaching behaviors.
 *
 * @description
 * Intercepts the Squid entity's step tick to check if its current momentum will carry
 * it onto land, cancelling its momentum and pushing it back into water if so.
 *
 * @since 25/06/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.mixin;

// ---------- IMPORTS
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Objects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ---------- CLASS: SQUIDMIXIN
@Mixin(Squid.class)
public abstract class SquidMixin {

    // ---------- SHADOW METHODS
    @Shadow public abstract void setMovementVector(float x, float y, float z);

    // ---------- BEACHING PREVENTION TICK
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void preventBeaching(CallbackInfo ci) {
        Squid squid = (Squid) (Object) this;
        Level level = squid.level();
        // Skip client-side processing
        if (level.isClientSide) {
            return;
        }

        Vec3 delta = squid.getDeltaMovement();
        // Skip check if the squid has negligible velocity
        if (delta.lengthSqr() <= 0.0001) {
            return;
        }

        // ---------- BEACHING DETECTION (Assess fluid state in target travel direction)
        // Check a little bit ahead in the direction of movement
        BlockPos targetPos = BlockPos.containing(squid.getX() + delta.x * 3.0D, squid.getY(), squid.getZ() + delta.z * 3.0D);
        if (!level.getFluidState(Objects.requireNonNull(targetPos)).is(Objects.requireNonNull(FluidTags.WATER))) {
            // Cancel forward momentum and push the squid downward and back
            squid.setDeltaMovement(delta.x * -0.1, -0.05, delta.z * -0.1);
        }
    }
}
