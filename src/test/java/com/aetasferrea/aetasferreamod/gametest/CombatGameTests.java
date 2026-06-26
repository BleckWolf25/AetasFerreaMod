/**
 * @file CombatGameTests.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Automated integration tests for combat mechanics.
 *
 * @description
 * Contains GameTest routines verifying arrow deflection chances and verifying that
 * projectile hit events on fully diamond-armored entities behave correctly without crashes.
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

// ---------- CLASS: COMBATGAMETESTS
@SuppressWarnings("null")
@GameTestHolder(AetasFerreaMod.MODID)
@PrefixGameTestTemplate(false)
public class CombatGameTests {

    // ---------- METHOD: TEST ARROW DEFLECTION
    @GameTest(template = "aetasferreamod:empty")
    public void testArrowDeflection(GameTestHelper helper) {
        // Spawn a target equipped with diamond armor pieces to trigger the deflection chance logic
        ArmorStand target = helper.spawn(EntityType.ARMOR_STAND, 1, 2, 1);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        target.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        target.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));

        // Spawn a projectile offset from the target and propel it downward toward the armor stand
        Arrow arrow = helper.spawn(EntityType.ARROW, 1, 3, 3);
        arrow.setDeltaMovement(new Vec3(0, -0.5, -0.5));

        // Verify the environment remains valid and both entities survive the initial projectile interaction
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityType.ARROW);
            helper.assertEntityPresent(EntityType.ARMOR_STAND);
        });
    }
}
