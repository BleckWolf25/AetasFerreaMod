/*
 * @file AetasFerreaConfig.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Aetas Ferrea - Forge Configuration System
 *
 * @description BEHAVIOR:
 * - Centralizes all tunable numbers, caps, and toggles for Aetas Ferrea.
 * - Auto-generates the 'aetasferreamod-common.toml' config file.
 * - Accessible from any class to check enabled modules (e.g., Dulling, Enchants).
 *
 * @since 07/06/2026
 * @updated 07/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod;

// ---------- IMPORTS
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AetasFerreaConfig {
    
    // ---------- CONFIG BUILDER
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // ---------- CONFIGURATION VARIABLES
    public static final ForgeConfigSpec.BooleanValue ENABLE_GOLDEN_DULLING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GOLDEN_ENCHANTS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ARMOR_REALISM;

    public static final ForgeConfigSpec.DoubleValue ARROW_DEFLECTION_CAP_IRON;
    public static final ForgeConfigSpec.DoubleValue ARROW_DEFLECTION_CAP_DIAMOND;
    public static final ForgeConfigSpec.DoubleValue ARROW_DEFLECTION_PER_PIECE_IRON;
    public static final ForgeConfigSpec.DoubleValue ARROW_DEFLECTION_PER_PIECE_DIAMOND;

    public static final ForgeConfigSpec.DoubleValue LEATHER_SPEED_CAP;
    public static final ForgeConfigSpec.DoubleValue LEATHER_SPEED_PER_PIECE;

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
        ARROW_DEFLECTION_CAP_IRON = BUILDER.comment("Max arrow deflection chance for full Iron Armor (e.g., 0.20 for 20%)")
                .defineInRange("arrowDeflectionCapIron", 0.20, 0.0, 1.0);
        ARROW_DEFLECTION_PER_PIECE_IRON = BUILDER.comment("Arrow deflection chance per Iron Armor piece")
                .defineInRange("arrowDeflectionPerPieceIron", 0.05, 0.0, 1.0);
                
        ARROW_DEFLECTION_CAP_DIAMOND = BUILDER.comment("Max arrow deflection chance for full Diamond Armor (e.g., 0.60 for 60%)")
                .defineInRange("arrowDeflectionCapDiamond", 0.60, 0.0, 1.0);
        ARROW_DEFLECTION_PER_PIECE_DIAMOND = BUILDER.comment("Arrow deflection chance per Diamond Armor piece")
                .defineInRange("arrowDeflectionPerPieceDiamond", 0.15, 0.0, 1.0);
                
        LEATHER_SPEED_CAP = BUILDER.comment("Max movement speed bonus for full Leather Armor (e.g., 0.05 for +5%)")
                .defineInRange("leatherSpeedCap", 0.05, 0.0, 1.0);
        LEATHER_SPEED_PER_PIECE = BUILDER.comment("Movement speed bonus per Leather Armor piece")
                .defineInRange("leatherSpeedPerPiece", 0.0125, 0.0, 1.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
