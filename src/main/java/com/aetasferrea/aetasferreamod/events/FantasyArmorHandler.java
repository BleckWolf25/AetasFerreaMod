/**
 * @file FantasyArmorHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Attributes and enchantment enforcement for custom fantasy armor.
 *
 * @description
 * Registers handlers for custom fantasy armor attributes and enforcements, including custom
 * auto-enchantments on pickup/crafting, blocking grindstone disenchanting, and passive attribute
 * updates (e.g. movement speed penalties and knockback boosts).
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("null")
// ---------- CLASS: FANTASY ARMOR HANDLER
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FantasyArmorHandler {

    // ---------- ATTRIBUTE MODIFIER UUIDS
    private static final UUID TWINNED_SLOW_UUID = UUID.fromString("1b4c3d5e-6f7a-8b9c-0d1e-2f3a4b5c6d7e");
    private static final UUID KNOCKBACK_ATTACK_UUID = UUID.fromString("f1e2d3c4-b5a6-9c8d-7e6f-5a4b3c2d1e0f");

    // ---------- ENCHANTMENT ENFORCEMENT & UTILITIES
    /**
     * Checks if the armor piece has its minimum baseline enchants, and applies them if missing.
     */
    private static void enforceEnchantments(ItemStack stack) {
        if (stack.isEmpty()) return;
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        
        if (id.startsWith("fantasy_armor:")) {
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            boolean changed = false;
            
            // Forgotten Tace always has at least Thorns I
            if (id.contains("forgotten_tace")) {
                if (enchants.getOrDefault(Enchantments.THORNS, 0) < 1) {
                    enchants.put(Enchantments.THORNS, 1);
                    changed = true;
                }
            } 
            // Gilded Hunt always has at least Protection I
            else if (id.contains("gilded_hunt")) {
                if (enchants.getOrDefault(Enchantments.ALL_DAMAGE_PROTECTION, 0) < 1) {
                    enchants.put(Enchantments.ALL_DAMAGE_PROTECTION, 1);
                    changed = true;
                }
            } 
            // Dragonslayer always has at least Protection III
            else if (id.contains("dragonslayer")) {
                if (enchants.getOrDefault(Enchantments.ALL_DAMAGE_PROTECTION, 0) < 3) {
                    enchants.put(Enchantments.ALL_DAMAGE_PROTECTION, 3);
                    changed = true;
                }
            }
            
            if (changed) {
                EnchantmentHelper.setEnchantments(enchants, stack);
            }
        }
    }

    private static boolean isProtectedArmor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        return id.startsWith("fantasy_armor:") && (id.contains("forgotten_tace") || id.contains("gilded_hunt") || id.contains("dragonslayer") || id.contains("ornstein"));
    }

    // ---------- EVENTS: ACQUISITION & WORKBENCHES
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        enforceEnchantments(event.getItem().getItem());
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        enforceEnchantments(event.getCrafting());
    }

    @SubscribeEvent
    public static void onGrindstone(GrindstoneEvent.OnPlaceItem event) {
        ItemStack top = event.getTopItem();
        ItemStack bottom = event.getBottomItem();
        
        // Block protected custom fantasy armors from being disenchanted via Grindstone
        if (isProtectedArmor(top) || isProtectedArmor(bottom)) {
            event.setCanceled(true);
        }
    }

    // ---------- TICK EVENT (PASSIVE ARMOR MODIFIERS)
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;

        // Failsafe: Enforce enchantments whenever an item is equipped
        enforceEnchantments(event.getTo());

        int gildedPieces = 0;
        int dragonPieces = 0;
        int twinnedPieces = 0;

        // Recalculate set pieces currently worn
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty()) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            
            if (id != null && id.getNamespace().equals("fantasy_armor")) {
                String path = id.getPath();
                if (path.contains("gilded_hunt")) gildedPieces++;
                else if (path.contains("dragonslayer")) dragonPieces++;
                else if (path.contains("twinned")) twinnedPieces++;
            }
        }

        // Twinned Armor: Slows down the player due to golden weight simulation
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(TWINNED_SLOW_UUID);
            if (twinnedPieces > 0) {
                speedAttr.addTransientModifier(new AttributeModifier(TWINNED_SLOW_UUID, "Twinned Slowness", -0.05 * twinnedPieces, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // Knockback bonus attributes
        AttributeInstance knockbackAttr = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (knockbackAttr != null) {
            knockbackAttr.removeModifier(KNOCKBACK_ATTACK_UUID);
            double totalKbBonus = (twinnedPieces * 0.1) + (gildedPieces * 0.15) + (dragonPieces * 0.2);
            
            if (totalKbBonus > 0) {
                knockbackAttr.addTransientModifier(new AttributeModifier(KNOCKBACK_ATTACK_UUID, "Fantasy Knockback Bonus", totalKbBonus, AttributeModifier.Operation.ADDITION));
            }
        }
    }
}