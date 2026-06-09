/**
 * @file GoldenEnchantmentHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Auto-enchants golden items and locks them from grindstones.
 *
 * @description
 * Automatically applies custom enchants to Golden equipment when obtained (Looting II for weapons,
 * Fortune II for tools, Protection II for armor), and prevents players from removing these
 * enchants using the Grindstone.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
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
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ---------- CLASS: GOLDEN ENCHANTMENT HANDLER
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GoldenEnchantmentHandler {

    // ---------- CONSTANTS & NBT TAG KEYS
    private static final String ENCHANTED_TAG = "AetasGoldEnchanted";

    // ---------- EVENT LISTENERS
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_ENCHANTS.get()) return;
        if (event.phase == TickEvent.Phase.START || event.player.level().isClientSide()) return;

        Player player = event.player;

        // Perform scan once every 10 ticks for performance
        if (player.tickCount % 10 != 0) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            // Skip passing known empty stacks to the processing method
            if (!stack.isEmpty()) {
                processGoldenItem(stack);
            }
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

        // Auto-enchant gold equipment on newly spawned mobs
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

    @SubscribeEvent
    public static void onGrindstonePlace(GrindstoneEvent.OnPlaceItem event) {
        if (!AetasFerreaConfig.ENABLE_GOLDEN_ENCHANTS.get()) return;

        ItemStack top = event.getTopItem();
        ItemStack bottom = event.getBottomItem();

        // Pull the tags into local variables to satisfy the null checker
        net.minecraft.nbt.CompoundTag topTag = top.getTag();
        net.minecraft.nbt.CompoundTag bottomTag = bottom.getTag();

        // Prevent golden items from being processed in a grindstone (no xp grinding or disenchanting)
        if ((topTag != null && topTag.getBoolean(ENCHANTED_TAG)) || 
            (bottomTag != null && bottomTag.getBoolean(ENCHANTED_TAG))) {
            
            event.setCanceled(true);
        }
    }

    // ---------- ENCHANTMENT LOGIC & CRITERIA
    /**
     * Checks if the stack is golden and applies custom enchantment and NBT metadata tagging.
     */
    @SuppressWarnings("null")
    private static void processGoldenItem(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        // Skip if this item has already been processed and tagged
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(ENCHANTED_TAG)) return;

        boolean isGoldWeapon = false;
        boolean isGoldTool = false;
        boolean isGoldArmor = false;

        // Determine golden category
        if (stack.getItem() instanceof TieredItem tieredItem && tieredItem.getTier() == Tiers.GOLD) {
            if (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) {
                isGoldWeapon = true;
            } else {
                isGoldTool = true;
            }
        } else if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getMaterial() == ArmorMaterials.GOLD) {
            isGoldArmor = true;
        }

        // Apply corresponding enchants
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
