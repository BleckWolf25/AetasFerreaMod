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
 * @updated 26/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import static java.util.Objects.requireNonNull;
import com.aetasferrea.aetasferreamod.AetasFerreaConfig;
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
import java.util.List;
import javax.annotation.Nonnull;

// ---------- CLASS: ECONOMYEVENTHANDLER
public class EconomyEventHandler {

    // ---------- FIELDS
    private static final java.util.Map<Item, Item> PROGRESSION_MAP = new java.util.HashMap<>();

    // ---------- STATIC INITIALIZATION
    static {
        // Armor Tiers
        PROGRESSION_MAP.put(Items.CHAINMAIL_HELMET, Items.LEATHER_HELMET);
        PROGRESSION_MAP.put(Items.GOLDEN_HELMET, Items.LEATHER_HELMET);
        PROGRESSION_MAP.put(Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE);
        PROGRESSION_MAP.put(Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE);
        PROGRESSION_MAP.put(Items.CHAINMAIL_LEGGINGS, Items.LEATHER_LEGGINGS);
        PROGRESSION_MAP.put(Items.GOLDEN_LEGGINGS, Items.LEATHER_LEGGINGS);
        PROGRESSION_MAP.put(Items.CHAINMAIL_BOOTS, Items.LEATHER_BOOTS);
        PROGRESSION_MAP.put(Items.GOLDEN_BOOTS, Items.LEATHER_BOOTS);

        PROGRESSION_MAP.put(Items.IRON_HELMET, Items.CHAINMAIL_HELMET);
        PROGRESSION_MAP.put(Items.DIAMOND_HELMET, Items.CHAINMAIL_HELMET);
        PROGRESSION_MAP.put(Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE);
        PROGRESSION_MAP.put(Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE);
        PROGRESSION_MAP.put(Items.IRON_LEGGINGS, Items.CHAINMAIL_LEGGINGS);
        PROGRESSION_MAP.put(Items.DIAMOND_LEGGINGS, Items.CHAINMAIL_LEGGINGS);
        PROGRESSION_MAP.put(Items.IRON_BOOTS, Items.CHAINMAIL_BOOTS);
        PROGRESSION_MAP.put(Items.DIAMOND_BOOTS, Items.CHAINMAIL_BOOTS);

        // Tool / Weapon Tiers
        PROGRESSION_MAP.put(Items.STONE_SWORD, Items.WOODEN_SWORD);
        PROGRESSION_MAP.put(Items.IRON_SWORD, Items.STONE_SWORD);
        PROGRESSION_MAP.put(Items.GOLDEN_SWORD, Items.STONE_SWORD);
        PROGRESSION_MAP.put(Items.DIAMOND_SWORD, Items.IRON_SWORD);

        PROGRESSION_MAP.put(Items.STONE_AXE, Items.WOODEN_AXE);
        PROGRESSION_MAP.put(Items.IRON_AXE, Items.STONE_AXE);
        PROGRESSION_MAP.put(Items.GOLDEN_AXE, Items.STONE_AXE);
        PROGRESSION_MAP.put(Items.DIAMOND_AXE, Items.IRON_AXE);

        PROGRESSION_MAP.put(Items.STONE_PICKAXE, Items.WOODEN_PICKAXE);
        PROGRESSION_MAP.put(Items.IRON_PICKAXE, Items.STONE_PICKAXE);
        PROGRESSION_MAP.put(Items.GOLDEN_PICKAXE, Items.STONE_PICKAXE);
        PROGRESSION_MAP.put(Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE);

        PROGRESSION_MAP.put(Items.STONE_SHOVEL, Items.WOODEN_SHOVEL);
        PROGRESSION_MAP.put(Items.IRON_SHOVEL, Items.STONE_SHOVEL);
        PROGRESSION_MAP.put(Items.GOLDEN_SHOVEL, Items.STONE_SHOVEL);
        PROGRESSION_MAP.put(Items.DIAMOND_SHOVEL, Items.IRON_SHOVEL);

        PROGRESSION_MAP.put(Items.STONE_HOE, Items.WOODEN_HOE);
        PROGRESSION_MAP.put(Items.IRON_HOE, Items.STONE_HOE);
        PROGRESSION_MAP.put(Items.GOLDEN_HOE, Items.STONE_HOE);
        PROGRESSION_MAP.put(Items.DIAMOND_HOE, Items.IRON_HOE);
    }

    // ---------- MATERIAL PROGRESSION LOOKUP
    private static Item getPreviousTierItem(Item item) {
        return PROGRESSION_MAP.get(item);
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
                int minPrice = AetasFerreaConfig.ARMOR_TRADE_MIN_PRICE.get();
                if (costA.is(requireNonNull(finalCurrency))) {
                    costA.setCount(EconomyMath.calculateArmorTradeCost(costA.getCount(), minPrice));
                }
                if (costB.is(requireNonNull(finalCurrency))) {
                    costB.setCount(EconomyMath.calculateArmorTradeCost(costB.getCount(), minPrice));
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
