/**
 * @file GoldenEquipmentEventHandler.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Implements the Golden Dulling feature which dynamically degrades equipment stats as durability drops.
 *
 * @description
 * Subscribes to ItemAttributeModifierEvent and PlayerEvent.BreakSpeed to apply penalties to Attack Damage,
 * Attack Speed, and Block Break Speed proportional to the missing durability of Golden items.
 *
 * @since 24/06/2026
 * @updated 24/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.Objects;
import java.util.UUID;

// ---------- CLASS: GoldenEquipmentEventHandler

public class GoldenEquipmentEventHandler {

    // ---------- CONSTANTS
    private static final UUID DULLING_DAMAGE_UUID = UUID.fromString("d8a0c242-b0e5-4217-8a47-ea76d21e8e91");
    private static final UUID DULLING_SPEED_UUID = UUID.fromString("e5b7c89f-819a-4c12-b2d9-a359b3c582df");

    // ---------- EVENTS
    @SubscribeEvent
    public static void onAttributeModifier(ItemAttributeModifierEvent event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_DULLING.get()) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        // Check if item is golden
        if (!stack.is(Objects.requireNonNull(CombatEventHandler.GOLDEN))) return;

        EquipmentSlot slot = event.getSlotType();
        if (slot != EquipmentSlot.MAINHAND) return;

        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamageValue();
        
        if (maxDamage > 0) {
            double damageFraction = (double) currentDamage / (double) maxDamage;
            
            if (damageFraction > 0.0) {
                // Apply a maximum penalty of 50% at 0 durability
                double damagePenalty = -0.5 * damageFraction;
                double speedPenalty = -0.3 * damageFraction;
                
                event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(Objects.requireNonNull(DULLING_DAMAGE_UUID), "Golden Dulling Damage", damagePenalty, AttributeModifier.Operation.MULTIPLY_TOTAL));
                event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(Objects.requireNonNull(DULLING_SPEED_UUID), "Golden Dulling Speed", speedPenalty, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_DULLING.get()) return;

        ItemStack stack = event.getEntity().getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        if (!stack.is(Objects.requireNonNull(CombatEventHandler.GOLDEN))) return;

        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamageValue();
        
        if (maxDamage > 0) {
            double damageFraction = (double) currentDamage / (double) maxDamage;
            
            if (damageFraction > 0.0) {
                float originalSpeed = event.getNewSpeed();
                // Reduce mining speed by up to 50%
                float newSpeed = originalSpeed * (1.0f - (0.5f * (float)damageFraction));
                event.setNewSpeed(Math.max(0.1f, newSpeed));
            }
        }
    }
}
