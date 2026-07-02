/**
 * @file HorseMechanicsGameTests.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Automated integration tests for horse and equine behavioral mechanics.
 *
 * @description
 * Contains GameTest cases validating specific behaviors of equines, such as horse panic reactions
 * upon receiving damage and donkey interactions in deep water environments.
 *
 * @since 26/06/2026
 * @updated 26/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.gametest;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

// ---------- CLASS: HORSEMECHANICSGAMETESTS
@SuppressWarnings("null")
@GameTestHolder(AetasFerreaMod.MODID)
@PrefixGameTestTemplate(false)
public class HorseMechanicsGameTests {

    // ---------- METHOD: TEST HORSE PANIC WHEN HIT
    @GameTest(template = "aetasferreamod:empty")
    public void testHorsePanicWhenHit(GameTestHelper helper) {
        // Spawn a horse entity to serve as the subject for testing trauma reactions
        Horse horse = helper.spawn(EntityType.HORSE, 1, 2, 1);

        // Inflict generic damage to prompt the horse panic event subscriber logic
        horse.hurt(horse.damageSources().generic(), 2.0f);

        // Verify the horse registers the hit and ticks its state without crashing
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityType.HORSE);
        });
    }

    // ---------- METHOD: TEST DONKEY IN DEEP WATER
    @GameTest(template = "aetasferreamod:empty")
    public void testDonkeyInDeepWater(GameTestHelper helper) {
        // ---------- PREPARE ENVIRONMENT (Water Pool)
        // Construct a deep water pool in the test bounds to simulate a river or lake crossing
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }

        // Spawn a donkey within the water block bounds to trigger the immersion check
        @SuppressWarnings("unused")
        Donkey donkey = helper.spawn(EntityType.DONKEY, 1, 3, 1);

        // Verify the donkey entity does not cause server failures upon drowning or water ticking
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityType.DONKEY);
        });
    }
}
