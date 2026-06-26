/**
 * @file LSOCompat.java
 *
 * @version 1.0.0
 * @author BleckWolf25
 * @license MIT
 *
 * @summary Reflection-based compatibility helper for Legendary Survival Overhaul (LSO).
 *
 * @description
 * Handles reflection access to LSO capabilities to apply damage to specific body parts.
 * This class isolates the dependency on LSO, allowing the mod to function even if LSO is not installed.
 *
 * @since 20/05/2026
 * @updated 08/06/2026
 */
// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import java.lang.reflect.Method;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.entity.player.Player;

// ---------- CLASS
public class LSOCompat {
    // Initialize a proper Logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // Reflection cache variables for LSO integration
    private static boolean initialized = false;
    private static Method getBodyDamageCapability;
    private static Method hurtBodyPart;
    private static Method setManualDirty;
    private static Class<?> bodyPartEnumClass;
    private static Method valueOfMethod; // Cached to prevent lookup on every hit

    // ---------- INITIALIZATION
    /**
     * Initializes reflection handlers for Legendary Survival Overhaul.
     * Fails silently if LSO is not loaded.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> capabilityUtilClass = Class.forName("sfiomn.legendarysurvivaloverhaul.util.CapabilityUtil");
            bodyPartEnumClass = Class.forName("sfiomn.legendarysurvivaloverhaul.api.bodydamage.BodyPartEnum");
            getBodyDamageCapability = capabilityUtilClass.getMethod("getBodyDamageCapability", Player.class);
            
            // Cache the valueOf method here, not in the hurt method
            valueOfMethod = bodyPartEnumClass.getMethod("valueOf", String.class);
            
        // 2. Catch the specific reflection exceptions
        } catch (ReflectiveOperationException e) {
            // LSO is not installed or version differs; fail silently.
        }
    }

    // ---------- PUBLIC API
    /**
     * Inflicts damage on a specific body part of a player using LSO.
     * * @param player   The target player
     * @param partName Name of the body part (e.g. "head", "chest")
     * @param amount   Amount of damage to inflict
     */
    public static void hurt(Player player, String partName, float amount) {
        if (!initialized) init();
        if (getBodyDamageCapability == null || bodyPartEnumClass == null) return;

        try {
            Object cap = getBodyDamageCapability.invoke(null, player);
            if (cap != null) {
                // Use the pre-cached valueOf method
                Object bodyPart = valueOfMethod.invoke(null, partName.toUpperCase());

                // Retrieve and cache the hurt method from LSO capability class
                if (hurtBodyPart == null) {
                    hurtBodyPart = cap.getClass().getMethod("hurt", bodyPartEnumClass, float.class);
                }
                hurtBodyPart.invoke(cap, bodyPart, amount);

                // Retrieve and cache the setManualDirty method to ensure data sync
                if (setManualDirty == null) {
                    setManualDirty = cap.getClass().getMethod("setManualDirty");
                }
                setManualDirty.invoke(cap);
            }
        // Catch specific exceptions to satisfy the IDE
        } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            // 3. Log the error properly instead of printing stack trace
            LOGGER.error("Aetas Ferrea failed to invoke LSO hurt method via reflection.", e);
        }
    }
}
