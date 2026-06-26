/**
 * @file WorldAgeTrackerTest.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Unit tests for verifying the time-based scaling progression of WorldAgeTracker.
 *
 * @description
 * Tests the mathematical hostile mob cap scaling logic across different phases of world age,
 * verifying that progression transitions occur at correct threshold intervals (days 3, 10, 20, 30).
 *
 * @since 26/06/2026
 * @updated 26/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.difficulty;

// ---------- IMPORTS
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

// ---------- CLASS: WORLDAGETRACKERTEST
public class WorldAgeTrackerTest {

    // ---------- METHOD: TEST GET HOSTILE MOB CAP PHASE I
    @Test
    public void testGetHostileMobCap_PhaseI() {
        // Assert hostile mob cap limits for the first progression phase spanning days zero through three
        assertEquals(12, WorldAgeTracker.getHostileMobCap(0));
        assertEquals(12, WorldAgeTracker.getHostileMobCap(2));
        assertEquals(12, WorldAgeTracker.getHostileMobCap(3));
    }

    // ---------- METHOD: TEST GET HOSTILE MOB CAP PHASE II
    @Test
    public void testGetHostileMobCap_PhaseII() {
        // Assert hostile mob cap limits for the second progression phase spanning days four through ten
        assertEquals(20, WorldAgeTracker.getHostileMobCap(4));
        assertEquals(20, WorldAgeTracker.getHostileMobCap(7));
        assertEquals(20, WorldAgeTracker.getHostileMobCap(10));
    }

    // ---------- METHOD: TEST GET HOSTILE MOB CAP PHASE III
    @Test
    public void testGetHostileMobCap_PhaseIII() {
        // Assert hostile mob cap limits for the third progression phase spanning days eleven through twenty
        assertEquals(32, WorldAgeTracker.getHostileMobCap(11));
        assertEquals(32, WorldAgeTracker.getHostileMobCap(15));
        assertEquals(32, WorldAgeTracker.getHostileMobCap(20));
    }

    // ---------- METHOD: TEST GET HOSTILE MOB CAP PHASE IV
    @Test
    public void testGetHostileMobCap_PhaseIV() {
        // Assert hostile mob cap limits for the fourth progression phase spanning days twenty-one through thirty
        assertEquals(45, WorldAgeTracker.getHostileMobCap(21));
        assertEquals(45, WorldAgeTracker.getHostileMobCap(25));
        assertEquals(45, WorldAgeTracker.getHostileMobCap(30));
    }

    // ---------- METHOD: TEST GET HOSTILE MOB CAP PHASE V
    @Test
    public void testGetHostileMobCap_PhaseV() {
        // Assert hostile mob cap limits for the fifth progression phase spanning days thirty-one and beyond
        assertEquals(60, WorldAgeTracker.getHostileMobCap(31));
        assertEquals(60, WorldAgeTracker.getHostileMobCap(100));
        assertEquals(60, WorldAgeTracker.getHostileMobCap(1000));
    }
}
