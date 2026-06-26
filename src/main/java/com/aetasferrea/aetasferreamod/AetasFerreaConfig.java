/**
 * @file AetasFerreaConfig.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Central Forge configuration system for Aetas Ferrea.
 *
 * @description
 * Centralizes all tunable numbers, caps, and toggles, and auto-generates the 'aetasferreamod-common.toml' file.
 *
 * @since 20/05/2026
 * @updated 25/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod;

// ---------- IMPORTS
import net.minecraftforge.common.ForgeConfigSpec;

// ---------- CONFIGURATION CLASS
public class AetasFerreaConfig {
    
    // ---------- CONFIG SPEC & BUILDER
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ---------- CONFIGURATION VARIABLES
    // Golden Equipment toggles
    public static final ForgeConfigSpec.BooleanValue ENABLE_GOLDEN_DULLING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GOLDEN_ENCHANTS;
    
    // Armor Realism toggles and values
    public static final ForgeConfigSpec.BooleanValue ENABLE_ARMOR_REALISM;

    public static final ForgeConfigSpec.DoubleValue ARROW_DEFLECTION_CAP_IRON;
    public static final ForgeConfigSpec.DoubleValue ARROW_DEFLECTION_CAP_DIAMOND;
    public static final ForgeConfigSpec.DoubleValue ARROW_DEFLECTION_PER_PIECE_IRON;
    public static final ForgeConfigSpec.DoubleValue ARROW_DEFLECTION_PER_PIECE_DIAMOND;

    public static final ForgeConfigSpec.DoubleValue LEATHER_SPEED_CAP;
    public static final ForgeConfigSpec.DoubleValue LEATHER_SPEED_PER_PIECE;

    // Game Balance Tweaks
    public static final ForgeConfigSpec.IntValue PUFFERFISH_INFLATION_TICKS;
    public static final ForgeConfigSpec.IntValue PUFFERFISH_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue SPIDER_SURFACE_Y;
    public static final ForgeConfigSpec.IntValue ARMOR_TRADE_MIN_PRICE;

    // Combat Tweaks
    public static final ForgeConfigSpec.DoubleValue BLUNT_ARMOR_CRUSH_BOOST;
    public static final ForgeConfigSpec.IntValue WOOD_ON_ARMOR_DURABILITY_DMG;
    public static final ForgeConfigSpec.DoubleValue WOOD_ON_ARMOR_DMG_CHANCE;
    public static final ForgeConfigSpec.IntValue WOOD_ON_NO_ARMOR_DURABILITY_DMG;
    public static final ForgeConfigSpec.DoubleValue WOOD_ON_NO_ARMOR_DMG_CHANCE;
    public static final ForgeConfigSpec.IntValue STONE_ON_ARMOR_DURABILITY_DMG;
    public static final ForgeConfigSpec.DoubleValue STONE_ON_ARMOR_DMG_CHANCE;
    public static final ForgeConfigSpec.DoubleValue HEAVY_ARMOR_ROLL_PENALTY;
    public static final ForgeConfigSpec.DoubleValue LEATHER_ROLL_BONUS;
    public static final ForgeConfigSpec.DoubleValue LEATHER_RECHARGE_MULT;

    // Horse Tweaks
    public static final ForgeConfigSpec.IntValue ROUNCEY_COMBAT_XP_CAP;
    public static final ForgeConfigSpec.IntValue ROUNCEY_AGILITY_XP_CAP;
    public static final ForgeConfigSpec.IntValue ROUNCEY_COMBAT_HIT_XP;
    public static final ForgeConfigSpec.IntValue ROUNCEY_COMBAT_ATTACK_XP;
    public static final ForgeConfigSpec.IntValue ROUNCEY_JUMP_XP;
    public static final ForgeConfigSpec.IntValue ROUNCEY_GALLOP_XP;
    public static final ForgeConfigSpec.DoubleValue HORSE_PANIC_EJECT_CHANCE;

    // Harvest Tweaks
    public static final ForgeConfigSpec.DoubleValue PUNCH_WRONG_BLOCK_DMG_MIN;
    public static final ForgeConfigSpec.DoubleValue PUNCH_WRONG_BLOCK_DMG_MAX;
    public static final ForgeConfigSpec.DoubleValue MINE_WRONG_TOOL_DMG_MIN;
    public static final ForgeConfigSpec.DoubleValue MINE_WRONG_TOOL_DMG_MAX;
    public static final ForgeConfigSpec.IntValue WRONG_TOOL_DURABILITY_DMG;
    public static final ForgeConfigSpec.IntValue KNIFE_CHOP_DURABILITY_DMG;
    // ---------- CONFIGURATION REGISTRATION
    static {
        BUILDER.push("Realism Mechanics");
        
        ENABLE_GOLDEN_DULLING = BUILDER.comment("Whether golden equipment degrades its stats as durability drops.")
                .define("enableGoldenDulling", true);
                
        ENABLE_GOLDEN_ENCHANTS = BUILDER.comment("Whether golden equipment automatically receives enchants when obtained.")
                .define("enableGoldenEnchants", true);
                
        ENABLE_ARMOR_REALISM = BUILDER.comment("Whether the Armor Realism Matrix (Slash immunity, blunt bypass) is enabled.")
                .define("enableArmorRealism", true);
                
        BUILDER.pop();
        
        BUILDER.push("Armor Realism Values");
        
        // Deflection stats for Iron armor
        ARROW_DEFLECTION_CAP_IRON = BUILDER.comment("Max arrow deflection chance for full Iron Armor (e.g., 0.20 for 20%)")
                .defineInRange("arrowDeflectionCapIron", 0.20, 0.0, 1.0);
        ARROW_DEFLECTION_PER_PIECE_IRON = BUILDER.comment("Arrow deflection chance per Iron Armor piece")
                .defineInRange("arrowDeflectionPerPieceIron", 0.05, 0.0, 1.0);
                
        // Deflection stats for Diamond armor
        ARROW_DEFLECTION_CAP_DIAMOND = BUILDER.comment("Max arrow deflection chance for full Diamond Armor (e.g., 0.60 for 60%)")
                .defineInRange("arrowDeflectionCapDiamond", 0.60, 0.0, 1.0);
        ARROW_DEFLECTION_PER_PIECE_DIAMOND = BUILDER.comment("Arrow deflection chance per Diamond Armor piece")
                .defineInRange("arrowDeflectionPerPieceDiamond", 0.15, 0.0, 1.0);
                
        // Movement speed stats for Leather armor
        LEATHER_SPEED_CAP = BUILDER.comment("Max movement speed bonus for full Leather Armor (e.g., 0.05 for +5%)")
                .defineInRange("leatherSpeedCap", 0.05, 0.0, 1.0);
        LEATHER_SPEED_PER_PIECE = BUILDER.comment("Movement speed bonus per Leather Armor piece")
                .defineInRange("leatherSpeedPerPiece", 0.0125, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("Mob Tweaks");
        PUFFERFISH_INFLATION_TICKS = BUILDER.comment("Ticks before pufferfish gets exhausted from inflation")
                .defineInRange("pufferfishInflationTicks", 200, 20, 1200);
        PUFFERFISH_COOLDOWN_TICKS = BUILDER.comment("Ticks pufferfish needs to rest after exhaustion")
                .defineInRange("pufferfishCooldownTicks", 600, 60, 2400);
        SPIDER_SURFACE_Y = BUILDER.comment("Y-level above which spiders are restricted to certain biomes")
                .defineInRange("spiderSurfaceYLevel", 60, -64, 320);
        BUILDER.pop();

        BUILDER.push("Economy Tweaks");
        ARMOR_TRADE_MIN_PRICE = BUILDER.comment("Minimum price for armor trades")
                .defineInRange("armorTradeMinPrice", 4, 1, 64);
        BUILDER.pop();

        BUILDER.push("Combat Tweaks");
        BLUNT_ARMOR_CRUSH_BOOST = BUILDER.comment("Extra damage boost ratio dealt by blunt weapons against heavy armor")
                .defineInRange("bluntArmorCrushBoost", 0.4, 0.0, 10.0);
        WOOD_ON_ARMOR_DURABILITY_DMG = BUILDER.comment("Durability damage taken when a wooden tool hits armor")
                .defineInRange("woodOnArmorDurabilityDmg", 30, 0, 1000);
        WOOD_ON_ARMOR_DMG_CHANCE = BUILDER.comment("Chance of a wooden tool taking durability damage hitting armor")
                .defineInRange("woodOnArmorDmgChance", 0.70, 0.0, 1.0);
        WOOD_ON_NO_ARMOR_DURABILITY_DMG = BUILDER.comment("Durability damage taken when a wooden tool hits unarmored")
                .defineInRange("woodOnNoArmorDurabilityDmg", 1, 0, 1000);
        WOOD_ON_NO_ARMOR_DMG_CHANCE = BUILDER.comment("Chance of a wooden tool taking durability damage hitting unarmored")
                .defineInRange("woodOnNoArmorDmgChance", 0.10, 0.0, 1.0);
        STONE_ON_ARMOR_DURABILITY_DMG = BUILDER.comment("Durability damage taken when a stone tool hits armor")
                .defineInRange("stoneOnArmorDurabilityDmg", 1, 0, 1000);
        STONE_ON_ARMOR_DMG_CHANCE = BUILDER.comment("Chance of a stone tool taking durability damage hitting armor")
                .defineInRange("stoneOnArmorDmgChance", 0.20, 0.0, 1.0);
        HEAVY_ARMOR_ROLL_PENALTY = BUILDER.comment("Combat roll weight penalty for wearing heavy armor")
                .defineInRange("heavyArmorRollPenalty", -10.0, -100.0, 0.0);
        LEATHER_ROLL_BONUS = BUILDER.comment("Combat roll weight bonus for wearing leather armor")
                .defineInRange("leatherRollBonus", 1.0, 0.0, 10.0);
        LEATHER_RECHARGE_MULT = BUILDER.comment("Combat roll recharge multiplier when wearing leather armor")
                .defineInRange("leatherRechargeMult", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("Horse Tweaks");
        ROUNCEY_COMBAT_XP_CAP = BUILDER.comment("Max combat XP a Rouncey can achieve")
                .defineInRange("rounceyCombatXpCap", 125, 1, 10000);
        ROUNCEY_AGILITY_XP_CAP = BUILDER.comment("Max agility XP a Rouncey can achieve")
                .defineInRange("rounceyAgilityXpCap", 150, 1, 10000);
        ROUNCEY_COMBAT_HIT_XP = BUILDER.comment("XP gained by horse when taking combat hits")
                .defineInRange("rounceyCombatHitXp", 10, 0, 1000);
        ROUNCEY_COMBAT_ATTACK_XP = BUILDER.comment("XP gained by horse when rider attacks")
                .defineInRange("rounceyCombatAttackXp", 5, 0, 1000);
        ROUNCEY_JUMP_XP = BUILDER.comment("XP gained by horse when jumping")
                .defineInRange("rounceyJumpXp", 5, 0, 1000);
        ROUNCEY_GALLOP_XP = BUILDER.comment("XP gained by horse when galloping")
                .defineInRange("rounceyGallopXp", 2, 0, 1000);
        HORSE_PANIC_EJECT_CHANCE = BUILDER.comment("Chance a panicking horse ejects its rider when hit")
                .defineInRange("horsePanicEjectChance", 0.30, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.push("Harvest Tweaks");
        PUNCH_WRONG_BLOCK_DMG_MIN = BUILDER.comment("Minimum hand trauma damage when punching wrong block")
                .defineInRange("punchWrongBlockDmgMin", 0.2, 0.0, 20.0);
        PUNCH_WRONG_BLOCK_DMG_MAX = BUILDER.comment("Maximum hand trauma damage when punching wrong block")
                .defineInRange("punchWrongBlockDmgMax", 0.7, 0.0, 20.0);
        MINE_WRONG_TOOL_DMG_MIN = BUILDER.comment("Minimum hand trauma damage when mining with wrong tool")
                .defineInRange("mineWrongToolDmgMin", 1.0, 0.0, 20.0);
        MINE_WRONG_TOOL_DMG_MAX = BUILDER.comment("Maximum hand trauma damage when mining with wrong tool")
                .defineInRange("mineWrongToolDmgMax", 2.5, 0.0, 20.0);
        WRONG_TOOL_DURABILITY_DMG = BUILDER.comment("Durability damage taken when mining with wrong tool")
                .defineInRange("wrongToolDurabilityDmg", 24, 0, 1000);
        KNIFE_CHOP_DURABILITY_DMG = BUILDER.comment("Durability damage taken when chopping logs with a knife")
                .defineInRange("knifeChopDurabilityDmg", 20, 0, 1000);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

