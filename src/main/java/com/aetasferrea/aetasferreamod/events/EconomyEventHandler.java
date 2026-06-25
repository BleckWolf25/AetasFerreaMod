/**
 * @file EconomyEventHandler.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Subscribes to trade events to reform villager and wandering trader transactions.
 *
 * @description
 * Listens to trade-related Forge events to dynamically replace emeralds with copper, raw iron,
 * or raw gold depending on villager profession and level, overhauls wandering trader inventories,
 * and enforces material progression requirements on weapon, tool, and armor purchases.
 *
 * @since 23/06/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import static java.util.Objects.requireNonNull;
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
import javax.annotation.Nonnull;

// ---------- CLASS: ECONOMYEVENTHANDLER
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID)
public class EconomyEventHandler {

    // ---------- MATERIAL PROGRESSION LOOKUP
    private static Item getPreviousTierItem(Item item) {
        // Armor Tiers
        if (item == Items.CHAINMAIL_HELMET || item == Items.GOLDEN_HELMET) return Items.LEATHER_HELMET;
        if (item == Items.CHAINMAIL_CHESTPLATE || item == Items.GOLDEN_CHESTPLATE) return Items.LEATHER_CHESTPLATE;
        if (item == Items.CHAINMAIL_LEGGINGS || item == Items.GOLDEN_LEGGINGS) return Items.LEATHER_LEGGINGS;
        if (item == Items.CHAINMAIL_BOOTS || item == Items.GOLDEN_BOOTS) return Items.LEATHER_BOOTS;

        if (item == Items.IRON_HELMET || item == Items.DIAMOND_HELMET) return Items.CHAINMAIL_HELMET;
        if (item == Items.IRON_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE) return Items.CHAINMAIL_CHESTPLATE;
        if (item == Items.IRON_LEGGINGS || item == Items.DIAMOND_LEGGINGS) return Items.CHAINMAIL_LEGGINGS;
        if (item == Items.IRON_BOOTS || item == Items.DIAMOND_BOOTS) return Items.CHAINMAIL_BOOTS;

        // Tool / Weapon Tiers
        if (item == Items.STONE_SWORD) return Items.WOODEN_SWORD;
        if (item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD) return Items.STONE_SWORD;
        if (item == Items.DIAMOND_SWORD) return Items.IRON_SWORD;

        if (item == Items.STONE_AXE) return Items.WOODEN_AXE;
        if (item == Items.IRON_AXE || item == Items.GOLDEN_AXE) return Items.STONE_AXE;
        if (item == Items.DIAMOND_AXE) return Items.IRON_AXE;

        if (item == Items.STONE_PICKAXE) return Items.WOODEN_PICKAXE;
        if (item == Items.IRON_PICKAXE || item == Items.GOLDEN_PICKAXE) return Items.STONE_PICKAXE;
        if (item == Items.DIAMOND_PICKAXE) return Items.IRON_PICKAXE;

        if (item == Items.STONE_SHOVEL) return Items.WOODEN_SHOVEL;
        if (item == Items.IRON_SHOVEL || item == Items.GOLDEN_SHOVEL) return Items.STONE_SHOVEL;
        if (item == Items.DIAMOND_SHOVEL) return Items.IRON_SHOVEL;

        if (item == Items.STONE_HOE) return Items.WOODEN_HOE;
        if (item == Items.IRON_HOE || item == Items.GOLDEN_HOE) return Items.STONE_HOE;
        if (item == Items.DIAMOND_HOE) return Items.IRON_HOE;

        return null;
    }

    // ---------- CURRENCY TIER MAPPING
    private static Item getCurrencyForProfessionAndLevel(VillagerProfession profession, int level) {
        // Peasant Tier
        if (profession == VillagerProfession.FARMER || profession == VillagerProfession.FISHERMAN ||
            profession == VillagerProfession.SHEPHERD || profession == VillagerProfession.BUTCHER) {
            return Items.COPPER_INGOT;
        }

        // Merchant Tier
        if (profession == VillagerProfession.FLETCHER || profession == VillagerProfession.LEATHERWORKER ||
            profession == VillagerProfession.MASON || profession == VillagerProfession.CARTOGRAPHER) {
            return Items.RAW_IRON;
        }

        // Mystical Tier
        if (profession == VillagerProfession.CLERIC || profession == VillagerProfession.LIBRARIAN) {
            return Items.RAW_GOLD;
        }

        // Knightly / Royal Tier
        if (profession == VillagerProfession.ARMORER || profession == VillagerProfession.WEAPONSMITH ||
            profession == VillagerProfession.TOOLSMITH) {
            return level >= 4 ? Items.EMERALD : Items.RAW_GOLD;
        }

        // Default Fallback
        return Items.EMERALD;
    }

    // ---------- VILLAGER TRADES INTERCEPTION
    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        VillagerProfession profession = event.getType();
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        if (trades == null || trades.isEmpty()) {
            return;
        }

        for (Int2ObjectMap.Entry<List<VillagerTrades.ItemListing>> entry : trades.int2ObjectEntrySet()) {
            int level = entry.getIntKey();
            List<VillagerTrades.ItemListing> levelTrades = entry.getValue();
            if (levelTrades == null) {
                continue;
            }

            Item newCurrency = getCurrencyForProfessionAndLevel(profession, level);
            // Skip mapping if vanilla Emerald is returned
            if (newCurrency == Items.EMERALD) {
                continue;
            }

            for (int i = 0; i < levelTrades.size(); i++) {
                levelTrades.set(i, new CurrencyConversionTrade(levelTrades.get(i), newCurrency));
            }
        }
    }

    // ---------- WANDERING TRADER INVENTORY OVERHAUL
    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        List<VillagerTrades.ItemListing> genericTrades = event.getGenericTrades();
        List<VillagerTrades.ItemListing> rareTrades = event.getRareTrades();
        if (genericTrades == null || rareTrades == null) {
            return;
        }

        genericTrades.clear();
        rareTrades.clear();

        // ---------- COMMON EXOTIC TRADES (Populate common wandering trader transactions)
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 12), new ItemStack(requireNonNull(Items.SADDLE)), 3, 5, 0.05f));
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 12), new ItemStack(requireNonNull(Items.ENDER_PEARL), 2), 5, 5, 0.05f));
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 16), new ItemStack(requireNonNull(Items.BLAZE_ROD)), 4, 5, 0.05f));
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 8), new ItemStack(requireNonNull(Items.SLIME_BALL), 4), 8, 2, 0.05f));
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 10), new ItemStack(requireNonNull(Items.NAUTILUS_SHELL)), 5, 5, 0.05f));
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 5), new ItemStack(requireNonNull(Items.AMETHYST_SHARD), 3), 6, 2, 0.05f));
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 3), new ItemStack(requireNonNull(Items.GLISTERING_MELON_SLICE), 3), 8, 2, 0.05f));
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 4), new ItemStack(requireNonNull(Items.GOLDEN_CARROT), 4), 8, 2, 0.05f));
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 15), new ItemStack(requireNonNull(Items.GHAST_TEAR)), 2, 10, 0.05f));
        genericTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 4), new ItemStack(requireNonNull(Items.LAPIS_LAZULI), 8), 12, 2, 0.05f));

        // ---------- RARE EXOTIC TRADES (Populate rare wandering trader transactions)
        rareTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 24), new ItemStack(requireNonNull(Items.DIAMOND)), 2, 15, 0.1f));
        rareTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 64), new ItemStack(requireNonNull(Items.TOTEM_OF_UNDYING)), 1, 30, 0.1f));
        rareTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 40), new ItemStack(requireNonNull(Items.HEART_OF_THE_SEA)), 1, 25, 0.1f));
        rareTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 16), new ItemStack(requireNonNull(Items.GOLDEN_APPLE)), 3, 10, 0.05f));
        rareTrades.add((trader, rand) -> new MerchantOffer(new ItemStack(requireNonNull(Items.EMERALD), 64), new ItemStack(requireNonNull(Items.ENCHANTED_GOLDEN_APPLE)), 1, 50, 0.1f));
    }

    // ---------- INNER CLASS: CURRENCYCONVERSIONTRADE
    public static class CurrencyConversionTrade implements VillagerTrades.ItemListing {

        // ---------- FIELDS
        private final VillagerTrades.ItemListing originalTrade;
        private final Item newCurrency;

        // ---------- CONSTRUCTOR
        public CurrencyConversionTrade(VillagerTrades.ItemListing originalTrade, Item newCurrency) {
            this.originalTrade = originalTrade;
            this.newCurrency = newCurrency;
        }

        // ---------- TRANSACTION MODIFICATION
        @Override
        public MerchantOffer getOffer(@Nonnull Entity trader, @Nonnull RandomSource rand) {
            MerchantOffer originalOffer = originalTrade.getOffer(trader, rand);
            if (originalOffer == null) {
                return null;
            }

            ItemStack costA = originalOffer.getBaseCostA().copy();
            ItemStack costB = originalOffer.getCostB().copy();
            ItemStack result = originalOffer.getResult().copy();

            // ---------- CLAY REDIRECT (Redirect clay trades to Copper)
            Item finalCurrency = this.newCurrency;
            if (costA.is(requireNonNull(Items.CLAY_BALL)) || costA.is(requireNonNull(Items.CLAY))) {
                finalCurrency = Items.COPPER_INGOT;
            }

            // ---------- CURRENCY REPLACEMENT (Swap emeralds for tier currency)
            if (costA.is(requireNonNull(Items.EMERALD))) {
                costA = new ItemStack(requireNonNull(finalCurrency), costA.getCount());
            }
            if (costB.is(requireNonNull(Items.EMERALD))) {
                costB = new ItemStack(requireNonNull(finalCurrency), costB.getCount());
            }
            if (result.is(requireNonNull(Items.EMERALD))) {
                result = new ItemStack(requireNonNull(finalCurrency), result.getCount());
            }

            // ---------- PRICE ADJUSTMENT (Enforce minimum currency cost on armor pieces)
            Item resultItem = result.getItem();
            if (resultItem instanceof ArmorItem) {
                if (costA.is(requireNonNull(finalCurrency)) && costA.getCount() < 4) {
                    costA.setCount(4);
                }
                if (costB.is(requireNonNull(finalCurrency)) && costB.getCount() < 4) {
                    costB.setCount(4);
                }
            }

            // ---------- PROGRESSION REQUIREMENT (Insert previous material tier as a required cost)
            Item prevTierItem = getPreviousTierItem(resultItem);
            if (prevTierItem != null && costB.isEmpty()) {
                costB = new ItemStack(prevTierItem, 1);
            }

            return new MerchantOffer(costA, costB, result, originalOffer.getUses(), originalOffer.getMaxUses(), originalOffer.getXp(), originalOffer.getPriceMultiplier(), originalOffer.getDemand());
        }
    }
}
