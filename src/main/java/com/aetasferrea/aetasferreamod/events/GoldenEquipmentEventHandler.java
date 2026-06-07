/*
 * @file GoldenEquipmentEventHandler.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Aetas Ferrea - Golden Dulling Mechanics
 *
 * @description BEHAVIOR:
 * - Degrades the efficiency of Golden equipment as durability falls.
 * - Reduces Attack Damage and Attack Speed on Golden Weapons dynamically.
 * - Reduces Block Break Speed for Golden Tools linearly based on missing durability.
 *
 * @since 07/06/2026
 * @updated 07/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.google.common.collect.Multimap;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ---------- PACKAGE
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GoldenEquipmentEventHandler {

    // ---------- ATTRIBUTE MODIFICATION
    @SubscribeEvent
    public static void onAttributeModification(ItemAttributeModifierEvent event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_DULLING.get()) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        boolean isGold = false;
        if (stack.getItem() instanceof TieredItem tieredItem && tieredItem.getTier() == Tiers.GOLD) {
            isGold = true;
        } else if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getMaterial() == ArmorMaterials.GOLD) {
            isGold = true;
        }

        if (!isGold) return;

        // Calculate durability percentage (1.0 = full, 0.0 = broken)
        float damage = stack.getDamageValue();
        float maxDamage = stack.getMaxDamage();
        float durabilityPct = 1.0f - (damage / maxDamage);

        // At 50% durability, effectiveness is 50%. The function is just effectiveness = durabilityPct.
        // Wait, the user said "at 50% durability, the weapon loses 50% of its base damage". This means linear scale from 100% to 0%.
        // So factor is exactly durabilityPct.
        double factor = durabilityPct;

        // We need to modify existing modifiers in the event to be multiplied by 'factor'.
        Multimap<Attribute, AttributeModifier> originalModifiers = event.getOriginalModifiers();
        if (originalModifiers.isEmpty()) return;

        event.clearModifiers();

        for (Attribute attribute : originalModifiers.keySet()) {
            for (AttributeModifier modifier : originalModifiers.get(attribute)) {
                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                    double newAmount = modifier.getAmount() * factor;
                    event.addModifier(attribute, new AttributeModifier(modifier.getId(), modifier.getName(), newAmount, modifier.getOperation()));
                } else {
                    event.addModifier(attribute, modifier);
                }
            }
        }
    }

    // ---------- MINING SPEED MODIFICATION
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_DULLING.get()) return;

        ItemStack stack = event.getEntity().getMainHandItem();
        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        if (stack.getItem() instanceof TieredItem tieredItem && tieredItem.getTier() == Tiers.GOLD) {
            float damage = stack.getDamageValue();
            float maxDamage = stack.getMaxDamage();
            float durabilityPct = 1.0f - (damage / maxDamage);
            
            // Linear reduction of mining speed.
            float currentSpeed = event.getNewSpeed();
            event.setNewSpeed(currentSpeed * durabilityPct);
        }
    }
}
