/*
 * @file GoldenEnchantmentHandler.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Aetas Ferrea - Golden Equipment Enchanter
 *
 * @description BEHAVIOR:
 * - Dynamically scans and enchants Golden tools, weapons, and armor.
 * - Applies Looting II, Fortune II, and Protection II respectively.
 * - Checks Player inventory periodically to catch crafted or picked-up gear.
 * - Hooks into mob spawning and equipment changes to grant enchantments to non-player entities.
 *
 * @since 07/06/2026
 * @updated 07/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import com.aetasferrea.aetasferreamod.AetasFerreaMod;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ---------- CLASS
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GoldenEnchantmentHandler {

    // ---------- CONSTANTS
    private static final String ENCHANTED_TAG = "AetasGoldEnchanted";

    // ---------- EVENT LISTENERS
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_ENCHANTS.get()) return;
        if (event.phase == TickEvent.Phase.END || event.player.level().isClientSide()) return;

        Player player = event.player;
        // Check only once every 10 ticks to save performance
        if (player.tickCount % 10 != 0) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            processGoldenItem(stack);
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_ENCHANTS.get()) return;
        processGoldenItem(event.getCrafting());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_ENCHANTS.get()) return;
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof Mob mob) {
            for (ItemStack stack : mob.getArmorSlots()) {
                processGoldenItem(stack);
            }
            for (ItemStack stack : mob.getHandSlots()) {
                processGoldenItem(stack);
            }
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_ENCHANTS.get()) return;
        if (event.getEntity().level().isClientSide()) return;
        
        processGoldenItem(event.getTo());
    }

    // ---------- ENCHANTMENT LOGIC
    private static void processGoldenItem(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        // Fast check if already processed
        if (stack.hasTag() && stack.getTag().getBoolean(ENCHANTED_TAG)) return;

        boolean isGoldWeapon = false;
        boolean isGoldTool = false;
        boolean isGoldArmor = false;

        if (stack.getItem() instanceof TieredItem tieredItem && tieredItem.getTier() == Tiers.GOLD) {
            if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) {
                isGoldWeapon = true;
            } else {
                isGoldTool = true;
            }
        } else if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getMaterial() == ArmorMaterials.GOLD) {
            isGoldArmor = true;
        }

        if (isGoldWeapon) {
            stack.enchant(Enchantments.MOB_LOOTING, 2);
            stack.getOrCreateTag().putBoolean(ENCHANTED_TAG, true);
        } else if (isGoldTool) {
            stack.enchant(Enchantments.BLOCK_FORTUNE, 2);
            stack.getOrCreateTag().putBoolean(ENCHANTED_TAG, true);
        } else if (isGoldArmor) {
            stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 2);
            stack.getOrCreateTag().putBoolean(ENCHANTED_TAG, true);
        }
    }
}
