/**
 * @file PufferfishMixin.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Mixin for pufferfish entities to manage custom exhaustion mechanics.
 *
 * @description
 * Implements a puff exhaustion cooldown on the Pufferfish entity, limiting how long
 * it can remain fully inflated, and forcing it to deflate and stay deflated during
 * the exhaustion period.
 *
 * @since 25/06/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.mixin;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import net.minecraft.world.entity.animal.Pufferfish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ---------- CLASS: PUFFERFISHMIXIN
@Mixin(Pufferfish.class)
public abstract class PufferfishMixin {

    // ---------- SHADOW FIELDS
    @Shadow public abstract int getPuffState();
    @Shadow public abstract void setPuffState(int state);
    @Shadow int inflateCounter;
    @Shadow int deflateTimer;

    // ---------- UNIQUE FIELDS
    @Unique private int puffExhaustionTimer = 0;
    @Unique private int timeFullyPuffed = 0;

    // ---------- TICK TIMERS AND EXHAUSTION HANDLING
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Pufferfish fish = (Pufferfish) (Object) this;
        // Terminate early if this is executing on the client side
        if (fish.level().isClientSide) {
            return;
        }

        // ---------- EXHAUSTION COOLDOWN (Force deflation and decrement timer)
        if (this.puffExhaustionTimer > 0) {
            this.puffExhaustionTimer--;
            // Force deflated state and reset internal timers to prevent inflation AI from working
            if (this.getPuffState() > 0) {
                this.setPuffState(0);
            }
            this.inflateCounter = 0;
            this.deflateTimer = 0;
            return;
        }

        // ---------- STAMINA MONITORING (Increment inflation time or recover stamina)
        if (this.getPuffState() == Pufferfish.STATE_FULL) {
            this.timeFullyPuffed++;
            // Apply exhaustion cooldown after configured time of continuous full inflation
            if (this.timeFullyPuffed >= AetasFerreaConfig.PUFFERFISH_INFLATION_TICKS.get()) {
                this.puffExhaustionTimer = AetasFerreaConfig.PUFFERFISH_COOLDOWN_TICKS.get();
                this.timeFullyPuffed = 0;
            }
        } else {
            // Slowly recover stamina if not fully puffed
            this.timeFullyPuffed = Math.max(0, this.timeFullyPuffed - 1);
        }
    }
}
