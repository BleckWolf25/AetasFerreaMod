/**
 * @file EconomyMath.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Provides isolated mathematical functions for trade economics.
 *
 * @description
 * Contains static functions to compute adjusted trade values and transaction costs, enforcing minimum price caps for villager armor trading.
 *
 * @since 26/06/2026
 * @updated 26/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- CLASS: ECONOMYMATH
public class EconomyMath {

    // ---------- METHOD: CALCULATE ARMOR TRADE COST
    public static int calculateArmorTradeCost(int originalCost, int minPrice) {
        // Enforce the configured minimum price ceiling using a standard max boundary
        return Math.max(originalCost, minPrice);
    }
}
