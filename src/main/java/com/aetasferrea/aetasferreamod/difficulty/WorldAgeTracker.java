/**
 * @file WorldAgeTracker.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Utility class to calculate the current age of the world in days.
 *
 * @description
 * Utility class to calculate the current age of the world in days and retrieve
 * corresponding difficulty mob caps for specific days.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.difficulty;

// ---------- IMPORTS
import net.minecraft.world.level.Level;

// ---------- CLASS: WORLD AGE TRACKER
public class WorldAgeTracker {
    
    // ---------- UTILITY METHODS
    /**
     * Retrieves the current "World Age" in days.
     * Calculated directly from the level's total daytime.
     */
    public static long getWorldDays(Level level) {
        return level.getDayTime() / 24000L;
    }
    
    /**
     * Calculates the maximum hostile surface mob cap for the current world day.
     * Phase I (0-3): Max 12 monsters on surface.
     * Phase II (4-10): Max 20 monsters on surface.
     * Phase III (11-20): Max 32 monsters on surface.
     * Phase IV (21-30): Max 45 monsters on surface.
     * Phase V (31+): Max 60 monsters on surface.
     */
    public static int getHostileMobCap(long days) {
        if (days <= 3) return 12;
        if (days <= 10) return 20;
        if (days <= 20) return 32;
        if (days <= 30) return 45;
        return 60;
    }
}
