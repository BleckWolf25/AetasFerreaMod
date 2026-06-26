/**
 * @file EconomyMathTest.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Unit tests for verifying the isolated mathematical calculations of EconomyMath.
 *
 * @description
 * Tests the minimum price adjustment math logic under different boundaries to ensure transaction cost
 * logic behaves correctly when costs are below, above, or equal to the minimum required price.
 *
 * @since 26/06/2026
 * @updated 26/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

// ---------- CLASS: ECONOMYMATHTEST
public class EconomyMathTest {

    // ---------- METHOD: TEST CALCULATE ARMOR TRADE COST BELOW MIN PRICE
    @Test
    public void testCalculateArmorTradeCost_BelowMinPrice() {
        int originalCost = 2;
        int minPrice = 5;

        // Execute math function to enforce minimum bound adjustment
        int adjustedCost = EconomyMath.calculateArmorTradeCost(originalCost, minPrice);

        // Assert that the cost is successfully bumped to the minimum configuration floor
        assertEquals(5, adjustedCost, "Cost should be adjusted up to the minimum price");
    }

    // ---------- METHOD: TEST CALCULATE ARMOR TRADE COST ABOVE MIN PRICE
    @Test
    public void testCalculateArmorTradeCost_AboveMinPrice() {
        int originalCost = 8;
        int minPrice = 5;

        // Execute math function with values exceeding the minimum constraint
        int adjustedCost = EconomyMath.calculateArmorTradeCost(originalCost, minPrice);

        // Assert that the original cost remains unaffected when it is already higher than the floor
        assertEquals(8, adjustedCost, "Cost should remain the original cost if it is above the minimum price");
    }

    // ---------- METHOD: TEST CALCULATE ARMOR TRADE COST EQUALS MIN PRICE
    @Test
    public void testCalculateArmorTradeCost_EqualsMinPrice() {
        int originalCost = 5;
        int minPrice = 5;

        // Execute math function with values exactly matching the boundary
        int adjustedCost = EconomyMath.calculateArmorTradeCost(originalCost, minPrice);

        // Assert that the value returns the boundary amount when they match
        assertEquals(5, adjustedCost, "Cost should remain the same if it equals the minimum price");
    }
}
