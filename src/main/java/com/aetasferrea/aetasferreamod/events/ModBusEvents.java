/**
 * @file ModBusEvents.java
 *
 * @version 1.0.0
 * @author Bleckwolf25
 * @license MIT
 *
 * @summary Mod event bus subscriber for registering custom entity attribute maps.
 *
 * @description
 * Listens on the Mod event bus for the EntityAttributeCreationEvent and registers
 * the vanilla horse attribute map for AETAS_HORSE, AETAS_DONKEY, and AETAS_MULE
 * so that Forge can initialise them correctly before they ever spawn.
 *
 * @since 20/05/2026
 * @updated 24/06/2026
 */

// ---------- PACKAGE
package com.aetasferrea.aetasferreamod.events;

// ---------- IMPORTS
import com.aetasferrea.aetasferreamod.AetasFerreaMod;
import com.aetasferrea.aetasferreamod.init.EntityInit;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// ---------- CLASS: ModBusEvents
@Mod.EventBusSubscriber(modid = AetasFerreaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBusEvents {

    // ---------- ATTRIBUTE REGISTRATION
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        // Tells Forge to give our custom entities their attribute maps before they ever spawn
        event.put(EntityInit.AETAS_HORSE.get(), Horse.createBaseHorseAttributes().build());
        event.put(EntityInit.AETAS_DONKEY.get(), AbstractHorse.createBaseHorseAttributes().build());
        event.put(EntityInit.AETAS_MULE.get(), AbstractHorse.createBaseHorseAttributes().build());
    }
}