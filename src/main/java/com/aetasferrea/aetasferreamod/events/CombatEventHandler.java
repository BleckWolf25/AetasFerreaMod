/*
 * @file CombatEventHandler.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Aetas Ferrea - Armor Realism Matrix
 *
 * @description BEHAVIOR:
 * - Manages all combat logic including the physical Armor Realism Matrix.
 * - Pre-Armor (Hurt Event): Deflects arrows and nullifies slashing damage against heavy armor.
 * - Post-Armor (Damage Event): Forces blunt/heavy weapons to bypass chainmail and iron.
 * - Attribute Modifiers: Grants speed per piece of Leather Armor.
 * - Stores Durability Snapshots to prevent armor from breaking against immune weapons.
 * - Grants 'Hero of the Village' for wearing full Golden Armor.
 *
 * @since 07/06/2026
 * @updated 07/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ---------- CLASS
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEventHandler {

    // ---------- CACHED TAGS & SNAPSHOTS
    private static final java.util.Map<LivingEntity, java.util.Map<EquipmentSlot, Integer>> armorDurabilitySnapshot = new java.util.WeakHashMap<>();
    private static final java.util.UUID LEATHER_SPEED_UUID = java.util.UUID.fromString("6a3b2c1d-4e5f-6a7b-8c9d-0e1f2a3b4c5d");

    public static final TagKey<Item> SLASHING = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "slashing_weapons"));
    public static final TagKey<Item> HIGH_MASS = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "high_mass_weapons"));
    public static final TagKey<Item> BLUNT = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "blunt_weapons"));

    public static final TagKey<Item> WOODEN = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "wooden_weapons"));
    public static final TagKey<Item> STONE = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "stone_weapons"));
    public static final TagKey<Item> IRON = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "iron_weapons"));
    public static final TagKey<Item> GOLDEN = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "golden_weapons"));
    public static final TagKey<Item> DIAMOND = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "diamond_weapons"));
    public static final TagKey<Item> NETHERITE = ItemTags.create(new ResourceLocation(AetasFerreaMod.MODID, "netherite_weapons"));

    // ---------- PRE-ARMOR DAMAGE EVENT
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!AetasFerreaConfig.ENABLE_ARMOR_REALISM.get()) return;

        LivingEntity victim = event.getEntity();
        int chainmailPieces = countArmorPieces(victim, ArmorMaterials.CHAIN);
        int ironPieces = countArmorPieces(victim, ArmorMaterials.IRON);
        int diamondPieces = countArmorPieces(victim, ArmorMaterials.DIAMOND);

        // Arrow deflection
        if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
            double deflectChance = 0.0;
            deflectChance += ironPieces * AetasFerreaConfig.ARROW_DEFLECTION_PER_PIECE_IRON.get();
            deflectChance += diamondPieces * AetasFerreaConfig.ARROW_DEFLECTION_PER_PIECE_DIAMOND.get();

            double ironCap = AetasFerreaConfig.ARROW_DEFLECTION_CAP_IRON.get();
            double diamondCap = AetasFerreaConfig.ARROW_DEFLECTION_CAP_DIAMOND.get();
            
            // If they wear full iron, it maxes at iron cap. Full diamond, maxes at diamond cap.
            double maxCap = Math.max(ironPieces > 0 ? ironCap : 0, diamondPieces > 0 ? diamondCap : 0);
            
            if (deflectChance > maxCap) {
                deflectChance = maxCap;
            }

            if (victim.getRandom().nextDouble() < deflectChance) {
                event.setCanceled(true);
                // Play sound or particles if needed
                return;
            }
        }

        boolean isDirectMelee = event.getSource().getDirectEntity() == event.getSource().getEntity() && event.getSource().getEntity() instanceof LivingEntity;
        if (isDirectMelee) {
            LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.isEmpty()) return;

            boolean isSlashing = weapon.is(SLASHING);
            boolean isHighMass = weapon.is(HIGH_MASS);
            boolean isBlunt = weapon.is(BLUNT);

            boolean isWooden = weapon.is(WOODEN);
            boolean isStone = weapon.is(STONE);
            boolean isIron = weapon.is(IRON);
            boolean isGolden = weapon.is(GOLDEN);
            boolean isDiamond = weapon.is(DIAMOND);
            boolean isNetherite = weapon.is(NETHERITE);

            // Snapshot armor durability to prevent loss (Micro-optimization: Only if wearing relevant armor)
            if (chainmailPieces > 0 || ironPieces > 0 || diamondPieces > 0) {
                java.util.Map<EquipmentSlot, Integer> snapshots = new java.util.EnumMap<>(EquipmentSlot.class);
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                        ItemStack armor = victim.getItemBySlot(slot);
                        if (!armor.isEmpty() && armor.getItem() instanceof ArmorItem armorItem && armor.isDamageableItem()) {
                            boolean preventDamage = false;
                            
                            if (armorItem.getMaterial() == ArmorMaterials.CHAIN) {
                                if (isWooden || isGolden) preventDamage = true;
                            } else if (armorItem.getMaterial() == ArmorMaterials.IRON) {
                                if (isWooden || isStone || isGolden) preventDamage = true;
                            } else if (armorItem.getMaterial() == ArmorMaterials.DIAMOND) {
                                if (isWooden || isStone || isGolden || isIron) preventDamage = true;
                            }
                            
                            if (preventDamage) {
                                snapshots.put(slot, armor.getDamageValue());
                            }
                        }
                    }
                }
                if (!snapshots.isEmpty()) {
                    armorDurabilitySnapshot.put(victim, snapshots);
                }
            }

            // Chainmail Matrix
            if (chainmailPieces > 0) {
                double effectRatio = chainmailPieces / 4.0;
                if (isWooden) {
                    // wooden weapons bounce off
                    event.setAmount((float) (event.getAmount() * (1.0 - effectRatio)));
                } else if (isSlashing && (isIron || isGolden)) {
                    // suffer up to 50% damage reduction
                    event.setAmount((float) (event.getAmount() * (1.0 - (0.5 * effectRatio))));
                }
            }

            // Iron Matrix
            if (ironPieces > 0) {
                double effectRatio = ironPieces / 4.0;
                if (isSlashing) {
                    // eliminates vulnerability to slashing sword blades (wooden, stone, golden, iron)
                    if (isWooden || isStone || isGolden || isIron) {
                        event.setAmount((float) (event.getAmount() * (1.0 - effectRatio)));
                    }
                }
                if (isHighMass && isStone) {
                    // stone axe deals 50% damage
                    event.setAmount((float) (event.getAmount() * (1.0 - (0.5 * effectRatio))));
                }
            }

            // Diamond Matrix
            if (diamondPieces > 0) {
                double effectRatio = diamondPieces / 4.0;
                if (isSlashing || isHighMass) {
                    // immune to cutting/slashing of all sword and axe tiers (vanilla)
                    event.setAmount((float) (event.getAmount() * (1.0 - effectRatio)));
                }
            }
        }
    }

    // ---------- POST-ARMOR DAMAGE EVENT
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!AetasFerreaConfig.ENABLE_ARMOR_REALISM.get()) return;

        LivingEntity victim = event.getEntity();
        
        // Restore armor durability if snapshotted
        java.util.Map<EquipmentSlot, Integer> snapshots = armorDurabilitySnapshot.remove(victim);
        if (snapshots != null) {
            for (java.util.Map.Entry<EquipmentSlot, Integer> entry : snapshots.entrySet()) {
                ItemStack armor = victim.getItemBySlot(entry.getKey());
                if (!armor.isEmpty()) {
                    armor.setDamageValue(entry.getValue());
                }
            }
        }

        int chainmailPieces = countArmorPieces(victim, ArmorMaterials.CHAIN);
        int ironPieces = countArmorPieces(victim, ArmorMaterials.IRON);

        boolean isDirectMelee = event.getSource().getDirectEntity() == event.getSource().getEntity() && event.getSource().getEntity() instanceof LivingEntity;
        if (isDirectMelee) {
            LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.isEmpty()) return;

            boolean isHighMass = weapon.is(HIGH_MASS);
            boolean isBlunt = weapon.is(BLUNT);
            
            // Bypass logic for chainmail and iron
            if ((chainmailPieces > 0 || ironPieces > 0) && (isHighMass || isBlunt)) {
                // If wearing full armor, restore 50% of the armor's damage reduction or just boost it.
                // We'll boost the final damage by 30% per piece to simulate armor penetration.
                double effectRatio = Math.max(chainmailPieces, ironPieces) / 4.0;
                event.setAmount((float) (event.getAmount() * (1.0 + (0.5 * effectRatio))));
            }
        }
    }

    // ---------- PASSIVE TICK & EQUIPMENT EVENTS
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!AetasFerreaConfig.ENABLE_ARMOR_REALISM.get()) return;

        Player player = event.player;
        if (event.phase == TickEvent.Phase.END || player.level().isClientSide()) return;

        // Refresh the effect every 20 ticks (1 second)
        if (player.tickCount % 20 == 0) {
            int goldPieces = countArmorPieces(player, ArmorMaterials.GOLD);
            if (goldPieces == 4) {
                // Apply for slightly longer than 1 second (220 ticks = 11 seconds) so it doesn't flicker
                player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 220, 0, false, false, true));
            } else {
                if (player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
                    player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!AetasFerreaConfig.ENABLE_ARMOR_REALISM.get()) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        // Leather Armor Speed Boost
        int leatherPieces = countArmorPieces(entity, ArmorMaterials.LEATHER);
        AttributeInstance speedAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(LEATHER_SPEED_UUID);
            if (leatherPieces > 0) {
                double speedBoost = Math.min(leatherPieces * AetasFerreaConfig.LEATHER_SPEED_PER_PIECE.get(), AetasFerreaConfig.LEATHER_SPEED_CAP.get());
                if (speedBoost > 0) {
                    speedAttribute.addTransientModifier(new AttributeModifier(LEATHER_SPEED_UUID, "Leather Armor Speed", speedBoost, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }
    }

    // ---------- UTILITY METHODS
    private static int countArmorPieces(LivingEntity entity, net.minecraft.world.item.ArmorMaterial material) {
        int count = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armorItem && armorItem.getMaterial() == material) {
                count++;
            }
        }
        return count;
    }
}
