/**
 * @file MonarchEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Drop handler for the Aetas Ferrea Hollow Monarch.
 *
 * @description
 * Handles custom drops for the Hollow Monarch upon death.
 *
 * @since 30/06/2026
 * @updated 02/07/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.entity.boss.MonarchEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import javax.annotation.Nonnull;

// ---------- CLASS: MONARCH EVENT HANDLER
@Mod.EventBusSubscriber(modid = "aetasferreamod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MonarchEventHandler {

    private static final String MONARCH_TAG = "Aetas_IsMonarch";

    /**
     * Spawns a fully configured Hollow Monarch at the specified position.
     * Used by MiniBossManager for command-based spawning.
     *
     * @param level The server level to spawn in.
     * @param pos   The position to spawn the Monarch at.
     * @return The created Husk entity, or null if creation failed.
     */
    @SuppressWarnings("removal")
    public static Husk spawnMonarch(@Nonnull ServerLevel level, BlockPos pos) {
        Husk monarch = EntityType.HUSK.create(level);
        if (monarch == null) return null;

        monarch.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);

        // Tags & Persistence
        monarch.addTag("aetas_monarch");
        monarch.getPersistentData().putBoolean(MONARCH_TAG, true);
        monarch.getPersistentData().putBoolean("IsDormant", false);
        monarch.setPersistenceRequired();
        monarch.setCustomName(Component.literal("The Hollow Monarch").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

        // Attributes
        var maxHealth = monarch.getAttribute(Objects.requireNonNull(Attributes.MAX_HEALTH));
        if (maxHealth != null) maxHealth.setBaseValue(200.0D);
        monarch.setHealth(200.0F);

        var attackDamage = monarch.getAttribute(Objects.requireNonNull(Attributes.ATTACK_DAMAGE));
        if (attackDamage != null) attackDamage.setBaseValue(12.0D);

        var knockbackResist = monarch.getAttribute(Objects.requireNonNull(Attributes.KNOCKBACK_RESISTANCE));
        if (knockbackResist != null) knockbackResist.setBaseValue(1.0D);

        var movementSpeed = monarch.getAttribute(Objects.requireNonNull(Attributes.MOVEMENT_SPEED));
        if (movementSpeed != null) movementSpeed.setBaseValue(0.16D);

        // Equipment
        Item swordItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("spartanweaponry", "diamond_greatsword"));
        if (swordItem == null || swordItem == Items.AIR) {
            swordItem = Items.DIAMOND_SWORD;
        }

        monarch.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Objects.requireNonNull(swordItem)));
        monarch.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Objects.requireNonNull(Items.ENDER_EYE)));
        monarch.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Objects.requireNonNull(Items.IRON_HELMET)));
        monarch.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Objects.requireNonNull(Items.IRON_CHESTPLATE)));
        monarch.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Objects.requireNonNull(Items.IRON_LEGGINGS)));
        monarch.setItemSlot(EquipmentSlot.FEET, new ItemStack(Objects.requireNonNull(Items.IRON_BOOTS)));

        // Force drop chances to 0.0F
        monarch.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        monarch.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        monarch.setDropChance(EquipmentSlot.HEAD, 0.0F);
        monarch.setDropChance(EquipmentSlot.CHEST, 0.0F);
        monarch.setDropChance(EquipmentSlot.LEGS, 0.0F);
        monarch.setDropChance(EquipmentSlot.FEET, 0.0F);

        // Remove lava/fire pathfinding penalties
        monarch.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        monarch.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        monarch.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);

        // Scaling
        GuardianEventHandler.applyBossScale(monarch, 2.0F);

        // Strip Enhanced AI
        GuardianEventHandler.stripEnhancedAI(monarch);

        level.addFreshEntity(monarch);

        // Force equipment update after spawning
        monarch.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Objects.requireNonNull(swordItem)));

        return monarch;
    }

    // Drops - Make the Monarch drop Siege Blueprints / Curios
    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void onMonarchDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof MonarchEntity monarch) {
            event.getDrops().clear();

            // Drop 1: Golden Apple (guaranteed)
            event.getDrops().add(new ItemEntity(Objects.requireNonNull(monarch.level()), monarch.getX(), monarch.getY(), monarch.getZ(), new ItemStack(Objects.requireNonNull(Items.GOLDEN_APPLE), 1)));

            // Drop 2: 16x Rotten Flesh (50% chance)
            if (monarch.level().random.nextFloat() < 0.5f) {
                event.getDrops().add(new ItemEntity(Objects.requireNonNull(monarch.level()), monarch.getX(), monarch.getY(), monarch.getZ(), new ItemStack(Objects.requireNonNull(Items.ROTTEN_FLESH), 16)));
            }

            // Drop 3: Sharpness 3 Enchanted Book (50% chance)
            if (monarch.level().random.nextFloat() < 0.5f) {
                ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Objects.requireNonNull(Enchantments.SHARPNESS), 3));
                event.getDrops().add(new ItemEntity(Objects.requireNonNull(monarch.level()), monarch.getX(), monarch.getY(), monarch.getZ(), Objects.requireNonNull(book)));
            }

            // Drop 4: Diamond Greatsword with <20% durability (50% chance)
            if (monarch.level().random.nextFloat() < 0.5f) {
                Item swordItemDrop = ForgeRegistries.ITEMS.getValue(new ResourceLocation("spartanweaponry", "diamond_greatsword"));
                if (swordItemDrop == null || swordItemDrop == Items.AIR) {
                    swordItemDrop = Items.DIAMOND_SWORD;
                }
                ItemStack swordDrop = new ItemStack(Objects.requireNonNull(swordItemDrop));
                int maxDamage = swordDrop.getMaxDamage();
                int minDamage = (int)(maxDamage * 0.8f);
                int randomDamage = minDamage + monarch.level().random.nextInt(Math.max(1, maxDamage - minDamage));
                swordDrop.setDamageValue(randomDamage);
                event.getDrops().add(new ItemEntity(Objects.requireNonNull(monarch.level()), monarch.getX(), monarch.getY(), monarch.getZ(), swordDrop));
            }

            // Drop 5: A substantial physical reward (Diamond Block)
            ItemStack diamondBlock = new ItemStack(Objects.requireNonNull(Items.DIAMOND_BLOCK), 1);
            event.getDrops().add(new ItemEntity(Objects.requireNonNull(monarch.level()), monarch.getX(), monarch.getY(), monarch.getZ(), diamondBlock));

            // Drop 6: 9 Eyes of Ender (guaranteed)
            ItemStack eyes = new ItemStack(Objects.requireNonNull(Items.ENDER_EYE), 9);
            event.getDrops().add(new ItemEntity(Objects.requireNonNull(monarch.level()), monarch.getX(), monarch.getY(), monarch.getZ(), eyes));
        }
    }
}
