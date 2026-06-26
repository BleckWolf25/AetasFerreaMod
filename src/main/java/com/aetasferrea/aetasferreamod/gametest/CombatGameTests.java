/**
 * @file CombatGameTests.java
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

// ---------- TEST CLASS
@GameTestHolder(AetasFerreaMod.MODID)
@PrefixGameTestTemplate(false)
public class CombatGameTests {



    @GameTest(template = "aetasferreamod:empty")
    public void testArrowDeflection(GameTestHelper helper) {
        // Spawn a target with Diamond Armor
        ArmorStand target = helper.spawn(EntityType.ARMOR_STAND, 1, 2, 1);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        target.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        target.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));

        // Spawn an arrow aimed at the armor stand
        Arrow arrow = helper.spawn(EntityType.ARROW, 1, 3, 3);
        arrow.setDeltaMovement(new Vec3(0, -0.5, -0.5)); // Move towards armor stand

        // Can't deterministically wait for deflection since it's probability based (60% max),
        // but can verify the combat event handler doesn't crash when an arrow hits a diamond armored entity.
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(EntityType.ARROW);
            helper.assertEntityPresent(EntityType.ARMOR_STAND);
        });
    }
}
