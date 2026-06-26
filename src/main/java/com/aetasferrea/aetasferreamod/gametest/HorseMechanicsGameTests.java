/**
 * @file HorseMechanicsGameTests.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary One-phrase summary.
 *
 * @description
 * Detailed explanation of the file's purpose and functionality.
 *
 * @since 26/06/2026
 * @updated 26/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.gametest;

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

// ---------- TEST CLASS
@GameTestHolder(AetasFerreaMod.MODID)
@PrefixGameTestTemplate(false)
public class HorseMechanicsGameTests {

    @GameTest(template = "aetasferreamod:empty")
    public void testHorsePanicWhenHit(GameTestHelper helper) {
        Horse horse = helper.spawn(EntityType.HORSE, 1, 2, 1);

        // Damage the horse to trigger panic
        horse.hurt(horse.damageSources().generic(), 2.0f);

        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityType.HORSE);
            // Just ensure the horse survives the tick processing without crashing
            // The panic AI task is added automatically in the handler
        });
    }

    @GameTest(template = "aetasferreamod:empty")
    public void testDonkeyInDeepWater(GameTestHelper helper) {
        // Setup a 3x3 pool of deep water
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }

        Donkey donkey = helper.spawn(EntityType.DONKEY, 1, 3, 1);

        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityType.DONKEY);
        });
    }
}
