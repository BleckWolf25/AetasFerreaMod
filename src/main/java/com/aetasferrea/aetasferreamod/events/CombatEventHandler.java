/**
 * @file CombatEventHandler.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Handles realistic combat calculations and armor mechanics.
 *
 * @description
 * Handles realistic combat calculations including arrow deflection, durability degradation/immunity,
 * blunt weapon armor bypass, leather speed boosts, golden armor status effects, and combat roll
 * armor weight penalties.
 *
 * @since 20/05/2026
 * @updated 24/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import java.util.Objects;

import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
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

// ---------- CLASS: COMBAT EVENT HANDLER
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatEventHandler {

    // ---------- ATTRIBUTE UUIDS & SNAPSHOT DATA
    private static final java.util.Map<LivingEntity, java.util.Map<EquipmentSlot, Integer>> armorDurabilitySnapshot = new java.util.WeakHashMap<>();
    private static final java.util.UUID LEATHER_SPEED_UUID = java.util.UUID.fromString("6a3b2c1d-4e5f-6a7b-8c9d-0e1f2a3b4c5d");
    private static final java.util.UUID HEAVY_UUID = java.util.UUID.fromString("0d6e6a10-4f5b-11ee-be56-0242ac120002");
    private static final java.util.UUID LEATHER_COUNT_UUID = java.util.UUID.fromString("0d6e6a10-4f5b-11ee-be56-0242ac120003");
    private static final java.util.UUID LEATHER_RECHARGE_UUID = java.util.UUID.fromString("0d6e6a10-4f5b-11ee-be56-0242ac120004");

    // ---------- CUSTOM WEAPON TAGS (METRICS)
    public static final TagKey<Item> SLASHING = ItemTags.create(Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "slashing_weapons")));
    public static final TagKey<Item> HIGH_MASS = ItemTags.create(Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "high_mass_weapons")));
    public static final TagKey<Item> BLUNT = ItemTags.create(Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "blunt_weapons")));

    // Material tiers
    public static final TagKey<Item> WOODEN = ItemTags.create(Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "wooden_weapons")));
    public static final TagKey<Item> STONE = ItemTags.create(Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "stone_weapons")));
    public static final TagKey<Item> IRON = ItemTags.create(Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "iron_weapons")));
    public static final TagKey<Item> GOLDEN = ItemTags.create(Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "golden_weapons")));
    public static final TagKey<Item> DIAMOND = ItemTags.create(Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "diamond_weapons")));
    public static final TagKey<Item> NETHERITE = ItemTags.create(Objects.requireNonNull(ResourceLocation.fromNamespaceAndPath(AetasFerreaMod.MODID, "netherite_weapons")));

    // ---------- FANTASY ARMOR KEYWORDS (FOR TIER CALCULATIONS)
    public static final String[] FANTASY_EARLY = {"lady_maria", "malenia", "wind_worshipper"};
    public static final String[] FANTASY_MIDDLE = {"hero", "golden_horns", "chess_board_knight", "sunset_wings", "forgotten_tace", "redeemer", "twinned", "crucible_knight", "old_knight", "dead_gladiator"};
    public static final String[] FANTASY_HIGH = {"ornstein", "gilded_hunt"};
    public static final String[] FANTASY_ENDGAME = {"dragonslayer"};

    // ---------- HELPER & PARSING METHODS
    
    /**
     * Counts how many pieces of a specific fantasy armor tier is currently worn.
     */
    public static int countFantasyArmorTier(LivingEntity entity, String[] keywords) {
        int count = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            if (!stack.isEmpty()) {
                net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key != null) {
                    String id = key.toString();
                    if (id.startsWith("fantasy_armor:")) {
                        for (String keyword : keywords) {
                            if (id.contains(keyword)) {
                                count++;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Checks if the item belongs to the Spartan Weaponry mod.
     */
    public static boolean isSpartanWeapon(ItemStack weapon) {
        if (weapon.isEmpty()) return false;
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(weapon.getItem());
        if (key == null) return false;
        String id = key.toString();
        return id.startsWith("spartanweaponry:");
    }

    /**
     * Checks if the Spartan Weaponry item is a blunt weapon.
     */
    public static boolean isSpartanBluntWeapon(ItemStack weapon) {
        if (!isSpartanWeapon(weapon)) return false;
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(weapon.getItem());
        if (key == null) return false;
        String id = key.toString();
        return id.contains("warhammer") || id.contains("mace") || id.contains("flail") || id.contains("club") || id.contains("cestus") || id.contains("quarterstaff");
    }

    /**
     * Checks if the Spartan Weaponry item is a high-mass weapon.
     */
    public static boolean isSpartanHighMassWeapon(ItemStack weapon) {
        if (!isSpartanWeapon(weapon)) return false;
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(weapon.getItem());
        if (key == null) return false;
        String id = key.toString();
        return id.contains("battleaxe") || id.contains("halberd") || id.contains("greatsword") || id.contains("lance") || id.contains("pike");
    }

    // ---------- LIVING HURT EVENT (PRE-ARMOR CALCULATIONS)
    @SuppressWarnings("null")
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!AetasFerreaConfig.ENABLE_ARMOR_REALISM.get()) return;

        LivingEntity victim = event.getEntity();
        int chainmailPieces = countArmorPieces(victim, ArmorMaterials.CHAIN);
        int ironPieces = countArmorPieces(victim, ArmorMaterials.IRON);
        int diamondPieces = countArmorPieces(victim, ArmorMaterials.DIAMOND);

        @SuppressWarnings("unused")
        int fantasyEarly = countFantasyArmorTier(victim, FANTASY_EARLY);
        int fantasyMiddle = countFantasyArmorTier(victim, FANTASY_MIDDLE);
        int fantasyHigh = countFantasyArmorTier(victim, FANTASY_HIGH);
        int fantasyEndgame = countFantasyArmorTier(victim, FANTASY_ENDGAME);

        ironPieces += fantasyMiddle;
        diamondPieces += fantasyHigh;

        // Arrow Deflection Mechanics
        if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
            // Dragonslayer (endgame) armor deflection
            if (fantasyEndgame > 0) {
                if (victim.getRandom().nextDouble() < (fantasyEndgame * 0.25)) {
                    event.setCanceled(true);
                    return;
                }
            }

            double deflectChance = 0.0;
            deflectChance += ironPieces * AetasFerreaConfig.ARROW_DEFLECTION_PER_PIECE_IRON.get();
            deflectChance += diamondPieces * AetasFerreaConfig.ARROW_DEFLECTION_PER_PIECE_DIAMOND.get();

            double ironCap = AetasFerreaConfig.ARROW_DEFLECTION_CAP_IRON.get();
            double diamondCap = AetasFerreaConfig.ARROW_DEFLECTION_CAP_DIAMOND.get();
            
            // Set max deflection cap depending on armor worn
            double maxCap = Math.max(ironPieces > 0 ? ironCap : 0, diamondPieces > 0 ? diamondCap : 0);
            
            if (deflectChance > maxCap) {
                deflectChance = maxCap;
            }

            if (victim.getRandom().nextDouble() < deflectChance) {
                event.setCanceled(true);
                return;
            }
        }

        Entity directEntity = event.getSource().getDirectEntity();
        Entity sourceEntity = event.getSource().getEntity();

        if (directEntity == sourceEntity && sourceEntity instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();

            boolean isSlashing = !weapon.isEmpty() && weapon.is(SLASHING);
            boolean isHighMass = !weapon.isEmpty() && weapon.is(HIGH_MASS);
            boolean isBlunt = !weapon.isEmpty() && weapon.is(BLUNT);

            boolean isSpartanBlunt = isSpartanBluntWeapon(weapon);
            boolean isSpartanHighMass = isSpartanHighMassWeapon(weapon);

            isBlunt = isBlunt || isSpartanBlunt;
            isHighMass = isHighMass || isSpartanHighMass;

            boolean isWooden = !weapon.isEmpty() && weapon.is(WOODEN);
            boolean isStone = !weapon.isEmpty() && weapon.is(STONE);
            boolean isIron = !weapon.isEmpty() && weapon.is(IRON);
            boolean isGolden = !weapon.isEmpty() && weapon.is(GOLDEN);
            boolean isDiamond = !weapon.isEmpty() && weapon.is(DIAMOND);
            boolean isNetherite = !weapon.isEmpty() && weapon.is(NETHERITE);

            boolean isWeapon = isSlashing || isHighMass || isBlunt || isWooden || isStone || isIron || isGolden || isDiamond || isNetherite || isSpartanWeapon(weapon);

            // Cancel fist damage against chainmail, iron, or diamond armor
            if (!isWeapon && (attacker instanceof Player || attacker instanceof Zombie || attacker instanceof Skeleton)) {
                if (ironPieces > 0 || diamondPieces > 0 || fantasyEndgame > 0) {
                    // Use 0.01f instead of 0 so Minecraft's hurt pipeline still registers the attacker
                    // and triggers neutral mob anger (Iron Golems, Endermen, etc.)
                    event.setAmount(0.01f);
                    return;
                }
            }
            if (weapon.isEmpty()) return;

            // Durability snapshotting: prevent weak weapons from damaging superior armor
            if (chainmailPieces > 0 || ironPieces > 0 || diamondPieces > 0 || fantasyEndgame > 0) {
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
                            } else if (armorItem.getMaterial() == ArmorMaterials.DIAMOND || countFantasyArmorTier(victim, FANTASY_HIGH) > 0 || countFantasyArmorTier(victim, FANTASY_ENDGAME) > 0) {
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

            // Durability penalties for striking armor with primitive weapons
            if (weapon.isDamageableItem()) {
                int validArmorPieces = 0;
                for (ItemStack armor : victim.getArmorSlots()) {
                    if (!armor.isEmpty() && armor.getItem() instanceof ArmorItem armorItem) {
                        if (armorItem.getMaterial() != ArmorMaterials.LEATHER) {
                            validArmorPieces++;
                        }
                    }
                }

                if (isWooden) {
                    if (validArmorPieces == 0) {
                        if (attacker.getRandom().nextDouble() < 0.10) {
                            weapon.hurtAndBreak(1, attacker, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                        }
                    } else {
                        if (attacker.getRandom().nextDouble() < 0.70) {
                            weapon.hurtAndBreak(30 * validArmorPieces, attacker, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                        }
                    }
                } else if (isStone) {
                    if (validArmorPieces > 0) {
                        if (attacker.getRandom().nextDouble() < 0.20) {
                            weapon.hurtAndBreak(1, attacker, (e) -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                        }
                    }
                }
            }

            // Dragonslayer Armor: Reduces slashing/piercing damage
            if (fantasyEndgame > 0) {
                if (!isBlunt && !isHighMass && !event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                    double effectRatio = fantasyEndgame / 4.0;
                    event.setAmount((float) (event.getAmount() * (1.0 - effectRatio)));
                }
            }

            // Chainmail Reduction Matrix
            if (chainmailPieces > 0) {
                double effectRatio = chainmailPieces / 4.0;
                if (isWooden) {
                    event.setAmount((float) (event.getAmount() * (1.0 - effectRatio)));
                } else if (isSlashing && (isIron || isGolden)) {
                    event.setAmount((float) (event.getAmount() * (1.0 - (0.5 * effectRatio))));
                }
            }

            // Iron Reduction Matrix
            if (ironPieces > 0) {
                double effectRatio = ironPieces / 4.0;
                if (isSlashing) {
                    if (isWooden || isStone || isGolden) {
                        event.setAmount((float) (event.getAmount() * (1.0 - effectRatio)));
                    }
                }
                if (isHighMass && isStone) {
                    event.setAmount((float) (event.getAmount() * (1.0 - (0.5 * effectRatio))));
                }
            }

            // Diamond Reduction Matrix
            if (diamondPieces > 0) {
                double effectRatio = diamondPieces / 4.0;
                if (isSlashing || isHighMass) {
                    float reduction = (float) effectRatio;
                    
                    // Diamond/Netherite/Spartan heavy weapons ignore pre-armor reduction
                    // to prevent double-reducing the damage alongside vanilla armor calculations.
                    if (isDiamond || isNetherite || isSpartanHighMass || isSpartanBlunt) {
                        reduction = 0.0f;
                    }
                    
                    float newAmount = event.getAmount() * (1.0f - reduction);
                    event.setAmount(newAmount);
                }
            }
        }
    }

    // ---------- LIVING DAMAGE EVENT (POST-ARMOR CALCULATIONS)
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!AetasFerreaConfig.ENABLE_ARMOR_REALISM.get()) return;

        LivingEntity victim = event.getEntity();
        
        // Restore armor durability values if snapshots exist
        java.util.Map<EquipmentSlot, Integer> snapshots = armorDurabilitySnapshot.remove(victim);
        if (snapshots != null) {
            for (java.util.Map.Entry<EquipmentSlot, Integer> entry : snapshots.entrySet()) {
                @SuppressWarnings("null")
                ItemStack armor = victim.getItemBySlot(entry.getKey());
                if (!armor.isEmpty()) {
                    armor.setDamageValue(entry.getValue());
                }
            }
        }

        int chainmailPieces = countArmorPieces(victim, ArmorMaterials.CHAIN);
        int ironPieces = countArmorPieces(victim, ArmorMaterials.IRON);
        int diamondPieces = countArmorPieces(victim, ArmorMaterials.DIAMOND);

        int fantasyMiddle = countFantasyArmorTier(victim, FANTASY_MIDDLE);
        int fantasyHigh = countFantasyArmorTier(victim, FANTASY_HIGH);
        int fantasyEndgame = countFantasyArmorTier(victim, FANTASY_ENDGAME);

        ironPieces += fantasyMiddle;
        diamondPieces += fantasyHigh;

        Entity directEntity = event.getSource().getDirectEntity();
        Entity sourceEntity = event.getSource().getEntity();

        if (directEntity == sourceEntity && sourceEntity instanceof LivingEntity attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.isEmpty()) return;

            @SuppressWarnings("null")
            boolean isHighMass = weapon.is(HIGH_MASS) || isSpartanHighMassWeapon(weapon);
            @SuppressWarnings("null")
            boolean isBlunt = weapon.is(BLUNT) || isSpartanBluntWeapon(weapon);
            @SuppressWarnings("null")
            boolean isDiamond = weapon.is(DIAMOND);
            @SuppressWarnings("null")
            boolean isNetherite = weapon.is(NETHERITE);
            @SuppressWarnings("null")
            boolean isSlashing = weapon.is(SLASHING);
            boolean isSpartanBlunt = isSpartanBluntWeapon(weapon);
            @SuppressWarnings("null")
            boolean isWooden = weapon.is(WOODEN);
            
            // Bypass logic: Blunt/High-Mass weapons deal extra damage past heavy armor (armor crushing)
            if ((chainmailPieces > 0 || ironPieces > 0 || diamondPieces > 0 || fantasyEndgame > 0) && (isHighMass || isBlunt)) {
                double maxPieces = Math.max(chainmailPieces, Math.max(ironPieces, Math.max(diamondPieces, fantasyEndgame)));
                double effectRatio = maxPieces / 4.0;
                event.setAmount((float) (event.getAmount() * (1.0 + (0.4 * effectRatio))));
            }

            // Damage Floors against Full Diamond/High-Tier armor
            if (diamondPieces == 4 || fantasyEndgame == 4) {
                float currentDamage = event.getAmount();
                if (isDiamond || isNetherite) {
                    if (isSlashing && currentDamage < 2.0f) event.setAmount(2.0f);
                    if ((isHighMass || isBlunt) && currentDamage < 4.0f) event.setAmount(4.0f);
                }
                if (isSpartanBlunt && !isWooden && currentDamage < 5.0f) {
                    event.setAmount(5.0f);
                }
            }
        }
    }

    // ---------- PLAYER TICK EVENT (ARMOR PASSIVE EFFECTS)
    @SuppressWarnings("null")
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!AetasFerreaConfig.ENABLE_ARMOR_REALISM.get()) return;

        Player player = event.player;
        if (event.phase == TickEvent.Phase.END || player.level().isClientSide()) return;

        // Perform checks every 20 ticks (1 second) for performance
        if (player.tickCount % 20 == 0) {
            int goldPieces = countArmorPieces(player, ArmorMaterials.GOLD);
            // Apply Hero of the Village effect when wearing full Golden Armor
            if (goldPieces == 4) {
                player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 220, 0, false, false, true));
            } else {
                if (player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
                    player.removeEffect(MobEffects.HERO_OF_THE_VILLAGE);
                }
            }

            // Combat Roll Weight Integration
            net.minecraft.world.entity.ai.attributes.Attribute countAttrDef = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("combatroll", "count"));
            net.minecraft.world.entity.ai.attributes.Attribute rechargeAttrDef = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.fromNamespaceAndPath("combatroll", "recharge"));

            if (countAttrDef != null || rechargeAttrDef != null) {
                boolean isHeavy = false;
                boolean isFullLeather = true;

                for (ItemStack stack : player.getArmorSlots()) {
                    if (stack.isEmpty()) {
                        isFullLeather = false;
                        continue;
                    }
                    net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (key == null) {
                        isFullLeather = false;
                        continue;
                    }
                    String id = key.toString();
                    
                    boolean isFantasyEarly = id.startsWith("fantasy_armor:") && (id.contains("lady_maria") || id.contains("malenia") || id.contains("wind_worshipper"));
                    boolean isFantasyHeavy = id.startsWith("fantasy_armor:") && !isFantasyEarly;

                    if (!id.contains("leather_") && !isFantasyEarly) {
                        isFullLeather = false;
                    }

                    if (id.contains("iron_") || id.contains("gold") || id.contains("diamond_") || isFantasyHeavy || (id.contains("plate") && !id.contains("chestplate"))) {
                        isHeavy = true;
                    }
                }

                // Apply rolling counts and recharge times depending on armor weight
                if (countAttrDef != null) {
                    AttributeInstance countAttr = player.getAttribute(countAttrDef);
                    if (countAttr != null) {
                        countAttr.removeModifier(HEAVY_UUID);
                        countAttr.removeModifier(LEATHER_COUNT_UUID);

                        if (isHeavy) {
                            countAttr.addTransientModifier(new AttributeModifier(HEAVY_UUID, "Heavy Armor Penalty", -10.0, AttributeModifier.Operation.ADDITION));
                            if (!player.getPersistentData().getBoolean("too_heavy_to_roll")) {
                                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.aetasferreamod.combat.too_heavy_roll").withStyle(net.minecraft.ChatFormatting.RED), true);
                                player.getPersistentData().putBoolean("too_heavy_to_roll", true);
                            }
                        } else {
                            player.getPersistentData().putBoolean("too_heavy_to_roll", false);
                            if (isFullLeather) {
                                countAttr.addTransientModifier(new AttributeModifier(LEATHER_COUNT_UUID, "Leather Armor Bonus", 1.0, AttributeModifier.Operation.ADDITION));
                            }
                        }
                    }
                }

                if (rechargeAttrDef != null) {
                    AttributeInstance rechargeAttr = player.getAttribute(rechargeAttrDef);
                    if (rechargeAttr != null) {
                        rechargeAttr.removeModifier(LEATHER_RECHARGE_UUID);
                        if (!isHeavy && isFullLeather) {
                            rechargeAttr.addTransientModifier(new AttributeModifier(LEATHER_RECHARGE_UUID, "Leather Recharge Bonus", 0.5, AttributeModifier.Operation.MULTIPLY_BASE));
                        }
                    }
                }
            }
        }
    }

    // ---------- EQUIPMENT CHANGE EVENT (SPEED MODIFIERS)
    @SuppressWarnings("null")
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!AetasFerreaConfig.ENABLE_ARMOR_REALISM.get()) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        // Apply Leather Armor Speed Boost
        int leatherPieces = countArmorPieces(entity, ArmorMaterials.LEATHER);
        leatherPieces += countFantasyArmorTier(entity, FANTASY_EARLY);
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

    // ---------- INTERNAL UTILITY METHODS
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