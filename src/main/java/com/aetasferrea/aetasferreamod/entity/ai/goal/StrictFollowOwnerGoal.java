/**
 * @file StrictFollowOwnerGoal.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Custom AI Goal for tamed Wolves following their owner.
 *
 * @description
 * Custom AI Goal for tamed Wolves that alters standard follow-owner behaviors.
 * Introduces sneaking/stealth behaviors, dynamic target clearing when owner sneaks,
 * and anti-stuck teleportation calculations.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.entity.ai.goal;

// ---------- IMPORTS
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

@SuppressWarnings("null")
// ---------- CLASS: STRICT FOLLOW OWNER GOAL
public class StrictFollowOwnerGoal extends Goal {

    // ---------- FIELDS & VARIABLES
    private final Wolf wolf;
    private LivingEntity owner;
    private final Level level;
    private final PathNavigation navigation;
    private int timeToRecalcPath;
    private float oldWaterCost;
    
    // Wolves do not fly, so this is hardcoded to false
    private final boolean flyTarget = false;

    // ---------- CONSTRUCTOR
    public StrictFollowOwnerGoal(Wolf pWolf) {
        this.wolf = pWolf;
        this.level = pWolf.level();
        this.navigation = pWolf.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    // ---------- GOAL EXECUTION CHECKERS
    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.wolf.getOwner();
        if (livingentity == null) {
            return false;
        } else if (livingentity.isSpectator()) {
            return false;
        } else if (this.wolf.isOrderedToSit()) {
            return false;
        } else if (this.wolf.distanceToSqr(livingentity) < (livingentity.isCrouching() ? 4.0D : 256.0D)) {
            // Do not follow if already within threshold range (tighter limit when owner sneaks)
            return false;
        } else {
            this.owner = livingentity;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.navigation.isDone()) {
            return false;
        } else if (this.wolf.isOrderedToSit()) {
            return false;
        } else {
            return this.wolf.distanceToSqr(this.owner) > (this.owner.isCrouching() ? 2.0D : 16.0D);
        }
    }

    // ---------- GOAL STATE LIFECYCLE
    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        
        // Save old water pathfinding cost and allow wolf to cross water when following
        this.oldWaterCost = this.wolf.getPathfindingMalus(BlockPathTypes.WATER);
        this.wolf.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
        
        // Restore old water pathfinding cost
        this.wolf.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
    }

    // ---------- GOAL TICKING LOGIC
    @Override
    public void tick() {
        this.wolf.getLookControl().setLookAt(this.owner, 10.0F, (float)this.wolf.getMaxHeadXRot());
        
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            double dSq = this.wolf.distanceToSqr(this.owner);
            
            // Sneak / Stealth Logic: Keep wolves quiet and close when sneaking
            if (this.owner.isCrouching()) {
                if (dSq > 4.0D) {
                    this.navigation.moveTo(this.owner, 1.0D);
                    this.wolf.setTarget(null); // Force wolves to ignore targets when sneaking to prevent alerting threats
                }
                return;
            }

            // Cross-Chunk / Anti-Stuck Teleportation
            if (dSq > 1024.0D) {
                this.teleportToOwner();
            } else if (dSq > 16.0D) {
                this.navigation.moveTo(this.owner, 1.2D);
            }
        }
    }

    // ---------- TELEPORTATION LOGIC
    private void teleportToOwner() {
        BlockPos blockpos = this.owner.blockPosition();

        // Try up to 10 times to find a safe position near the owner to teleport to
        for(int i = 0; i < 10; ++i) {
            int j = this.randomIntInclusive(-3, 3);
            int k = this.randomIntInclusive(-1, 1);
            int l = this.randomIntInclusive(-3, 3);
            boolean flag = this.maybeTeleportTo(blockpos.getX() + j, blockpos.getY() + k, blockpos.getZ() + l);
            if (flag) {
                return;
            }
        }
    }

    private boolean maybeTeleportTo(int pX, int pY, int pZ) {
        if (Math.abs((double)pX - this.owner.getX()) < 2.0D && Math.abs((double)pZ - this.owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(new BlockPos(pX, pY, pZ))) {
            return false;
        } else {
            this.wolf.moveTo((double)pX + 0.5D, (double)pY, (double)pZ + 0.5D, this.wolf.getYRot(), this.wolf.getXRot());
            this.navigation.stop();
            return true;
        }
    }

    private boolean canTeleportTo(BlockPos pPos) {
        BlockPathTypes blockpathtypes = WalkNodeEvaluator.getBlockPathTypeStatic(this.level, pPos.mutable());
        if (blockpathtypes != BlockPathTypes.WALKABLE) {
            return false;
        } else {
            BlockState blockstate = this.level.getBlockState(pPos.below());
            if (!this.flyTarget && blockstate.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockpos = pPos.subtract(this.wolf.blockPosition());
                return this.level.noCollision(this.wolf, this.wolf.getBoundingBox().move(blockpos));
            }
        }
    }

    // ---------- HELPER METHODS
    private int randomIntInclusive(int pMin, int pMax) {
        return this.wolf.getRandom().nextInt(pMax - pMin + 1) + pMin;
    }
}